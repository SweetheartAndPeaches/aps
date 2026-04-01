package com.zlt.aps.cx.service;

import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.config.CxStructurePriority;
import com.zlt.aps.mp.api.domain.entity.MdmCxMachineFixed;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 普通新增任务处理器
 * 
 * <p>负责 S5.3 普通新增任务排产：
 * <ul>
 *   <li>任务排序：月计划优先级→收尾→新胎胚</li>
 *   <li>使用 {@link BalancingService} 均衡分配（与续作任务相同逻辑）</li>
 *   <li>合并续作+新增后重新均衡</li>
 * </ul>
 *
 * <p>注意：试制/量试任务由 {@link TrialTaskProcessor} 处理
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewTaskProcessor {

    private final BalancingService balancingService;

    /** 默认整车容量 */
    private static final int DEFAULT_TRIP_CAPACITY = 12;
    
    /** 默认机台种类上限 */
    private static final int DEFAULT_MAX_TYPES_PER_MACHINE = 4;

    /**
     * 处理普通新增任务
     *
     * <p>方案A：合并续作+新增重新均衡
     */
    public List<CoreScheduleAlgorithmService.MachineAllocationResult> processNewTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> newTasks,
            ScheduleContextDTO context,
            LocalDate scheduleDate,
            List<CxShiftConfig> dayShifts,
            int day,
            List<CoreScheduleAlgorithmService.MachineAllocationResult> existAllocations) {

        if (CollectionUtils.isEmpty(newTasks)) {
            return existAllocations != null ? existAllocations : new ArrayList<>();
        }

        log.info("========== 开始处理新增任务，共 {} 个任务 ==========", newTasks.size());

        // 停产日不排新增任务
        if (Boolean.TRUE.equals(context.getIsClosingDay())) {
            log.info("停产日不排新增任务");
            return existAllocations != null ? existAllocations : new ArrayList<>();
        }

        // Step 1: 排序新增任务
        sortNewTasks(newTasks, context);

        // Step 2: 按结构分组
        Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> structureTaskMap = newTasks.stream()
                .filter(t -> t.getStructureName() != null)
                .collect(Collectors.groupingBy(
                        CoreScheduleAlgorithmService.DailyEmbryoTask::getStructureName,
                        LinkedHashMap::new,
                        Collectors.toList()));

        // Step 3: 获取可用机台
        List<MdmMoldingMachine> availableMachines = context.getAvailableMachines();
        if (CollectionUtils.isEmpty(availableMachines)) {
            log.warn("没有可用的成型机台");
            return existAllocations != null ? existAllocations : new ArrayList<>();
        }

        // Step 4: 合并续作+新增，使用 BalancingService 重新均衡
        List<CoreScheduleAlgorithmService.MachineAllocationResult> results;
        if (existAllocations != null && !existAllocations.isEmpty()) {
            results = rebalanceWithNewTasks(existAllocations, structureTaskMap, availableMachines, context);
        } else {
            results = balanceNewTasks(structureTaskMap, availableMachines, context);
        }

        log.info("========== 新增任务处理完成，共 {} 台机台分配任务 ==========", results.size());
        return results;
    }

    /**
     * 合并续作分配结果 + 新增任务，使用 BalancingService 重新均衡
     */
    private List<CoreScheduleAlgorithmService.MachineAllocationResult> rebalanceWithNewTasks(
            List<CoreScheduleAlgorithmService.MachineAllocationResult> existAllocations,
            Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> structureTaskMap,
            List<MdmMoldingMachine> availableMachines,
            ScheduleContextDTO context) {

        log.info("----- 合并续作+新增任务，使用 BalancingService 重新均衡 -----");

        // Step 1: 提取续作任务，标记为续作
        List<CoreScheduleAlgorithmService.DailyEmbryoTask> continueTasks = new ArrayList<>();
        Map<String, Set<String>> machineHistoryMap = new HashMap<>();

        for (CoreScheduleAlgorithmService.MachineAllocationResult allocation : existAllocations) {
            String machineCode = allocation.getMachineCode();
            Set<String> embryos = new HashSet<>();

            for (CoreScheduleAlgorithmService.TaskAllocation task : allocation.getTaskAllocations()) {
                embryos.add(task.getMaterialCode());

                // 从已有分配中重建任务对象
                CoreScheduleAlgorithmService.DailyEmbryoTask embryoTask = new CoreScheduleAlgorithmService.DailyEmbryoTask();
                embryoTask.setMaterialCode(task.getMaterialCode());
                embryoTask.setMaterialName(task.getMaterialName());
                embryoTask.setStructureName(task.getStructureName());
                embryoTask.setIsContinueTask(true);
                embryoTask.setIsEndingTask(task.getIsEndingTask());
                embryoTask.setIsMainProduct(task.getIsMainProduct());
                embryoTask.setPlannedProduction(task.getQuantity());

                // 计算硫化机台数
                int load = (int) Math.ceil((double) task.getQuantity() / DEFAULT_TRIP_CAPACITY);
                embryoTask.setVulcanizeMachineCount(load);

                continueTasks.add(embryoTask);
            }

            machineHistoryMap.put(machineCode, embryos);
        }

        // Step 2: 添加新增任务
        List<CoreScheduleAlgorithmService.DailyEmbryoTask> newTasks = new ArrayList<>();
        for (Map.Entry<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> entry : structureTaskMap.entrySet()) {
            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : entry.getValue()) {
                task.setIsContinueTask(false);
                // 计算硫化机台数
                int demand = task.getPlannedProduction() != null && task.getPlannedProduction() > 0
                        ? task.getPlannedProduction() : task.getDemandQuantity();
                int load = (int) Math.ceil((double) demand / DEFAULT_TRIP_CAPACITY);
                task.setVulcanizeMachineCount(load);
                newTasks.add(task);
            }
        }

        // Step 3: 合并所有任务
        List<CoreScheduleAlgorithmService.DailyEmbryoTask> allTasks = new ArrayList<>();
        allTasks.addAll(continueTasks);
        allTasks.addAll(newTasks);

        // Step 4: 使用 BalancingService 均衡分配
        BalancingService.BalancingResult balancingResult = balancingService.balanceEmbryosToMachines(
                allTasks, availableMachines, context);

        // Step 5: 构建分配结果
        return buildResultsFromBalancingResult(balancingResult, allTasks, context);
    }

    /**
     * 仅新增任务均衡分配（无续作）
     */
    private List<CoreScheduleAlgorithmService.MachineAllocationResult> balanceNewTasks(
            Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> structureTaskMap,
            List<MdmMoldingMachine> availableMachines,
            ScheduleContextDTO context) {

        log.info("----- 仅新增任务均衡分配 -----");

        // 收集所有新增任务
        List<CoreScheduleAlgorithmService.DailyEmbryoTask> allTasks = new ArrayList<>();
        for (Map.Entry<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> entry : structureTaskMap.entrySet()) {
            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : entry.getValue()) {
                task.setIsContinueTask(false);
                // 计算硫化机台数
                int demand = task.getPlannedProduction() != null && task.getPlannedProduction() > 0
                        ? task.getPlannedProduction() : task.getDemandQuantity();
                int load = (int) Math.ceil((double) demand / DEFAULT_TRIP_CAPACITY);
                task.setVulcanizeMachineCount(load);
                allTasks.add(task);
            }
        }

        // 使用 BalancingService 均衡分配
        BalancingService.BalancingResult balancingResult = balancingService.balanceEmbryosToMachines(
                allTasks, availableMachines, context);

        // 构建分配结果
        return buildResultsFromBalancingResult(balancingResult, allTasks, context);
    }

    /**
     * 从 BalancingService 结果构建 MachineAllocationResult 列表
     */
    private List<CoreScheduleAlgorithmService.MachineAllocationResult> buildResultsFromBalancingResult(
            BalancingService.BalancingResult balancingResult,
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
            ScheduleContextDTO context) {

        List<CoreScheduleAlgorithmService.MachineAllocationResult> results = new ArrayList<>();

        // 构建任务映射
        Map<String, CoreScheduleAlgorithmService.DailyEmbryoTask> taskMap = tasks.stream()
                .collect(Collectors.toMap(
                        CoreScheduleAlgorithmService.DailyEmbryoTask::getMaterialCode,
                        t -> t,
                        (a, b) -> a));

        for (BalancingService.MachineAssignment assignment : balancingResult.getAssignments()) {
            CoreScheduleAlgorithmService.MachineAllocationResult allocation = createMachineAllocation(
                    assignment.getMachineCode(), context);

            for (BalancingService.EmbryoAssignment embryoAssignment : assignment.getEmbryoAssignments()) {
                CoreScheduleAlgorithmService.DailyEmbryoTask task = embryoAssignment.getTask();
                if (task != null) {
                    allocateTaskToMachine(allocation, task, context);
                }
            }

            if (!allocation.getTaskAllocations().isEmpty()) {
                results.add(allocation);
            }
        }

        return results;
    }

    /**
     * 排序新增任务
     */
    public void sortNewTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks, 
            ScheduleContextDTO context) {
        tasks.sort((a, b) -> {
            // 1. 按月计划优先级排序
            int priorityA = getMonthPlanPriority(a.getMaterialCode(), context);
            int priorityB = getMonthPlanPriority(b.getMaterialCode(), context);
            if (priorityA != priorityB) {
                return Integer.compare(priorityA, priorityB);
            }

            // 2. 收尾任务优先
            if (Boolean.TRUE.equals(a.getIsEndingTask()) && !Boolean.TRUE.equals(b.getIsEndingTask())) {
                return -1;
            }
            if (!Boolean.TRUE.equals(a.getIsEndingTask()) && Boolean.TRUE.equals(b.getIsEndingTask())) {
                return 1;
            }

            // 3. 紧急收尾优先
            if (Boolean.TRUE.equals(a.getIsUrgentEnding()) && !Boolean.TRUE.equals(b.getIsUrgentEnding())) {
                return -1;
            }
            if (!Boolean.TRUE.equals(a.getIsUrgentEnding()) && Boolean.TRUE.equals(b.getIsUrgentEnding())) {
                return 1;
            }
            
            // 4. 10天内收尾优先
            if (Boolean.TRUE.equals(a.getIsNearEnding()) && !Boolean.TRUE.equals(b.getIsNearEnding())) {
                return -1;
            }
            if (!Boolean.TRUE.equals(a.getIsNearEnding()) && Boolean.TRUE.equals(b.getIsNearEnding())) {
                return 1;
            }
            
            // 5. 新胎胚优先
            boolean aIsNew = Boolean.TRUE.equals(a.getIsNewEmbryo());
            boolean bIsNew = Boolean.TRUE.equals(b.getIsNewEmbryo());
            if (aIsNew && !bIsNew) {
                return -1;
            }
            if (!aIsNew && bIsNew) {
                return 1;
            }

            // 6. 按需求量排序（大的优先）
            return Integer.compare(b.getDemandQuantity(), a.getDemandQuantity());
        });
    }

    private int getMonthPlanPriority(String materialCode, ScheduleContextDTO context) {
        List<CxStructurePriority> priorities = context.getStructurePriorities();
        if (priorities != null) {
            for (CxStructurePriority priority : priorities) {
                // TODO: 需要根据物料编码匹配结构
            }
        }
        return 999;
    }

    private CoreScheduleAlgorithmService.MachineAllocationResult createMachineAllocation(
            String machineCode, ScheduleContextDTO context) {
        CoreScheduleAlgorithmService.MachineAllocationResult allocation = new CoreScheduleAlgorithmService.MachineAllocationResult();
        allocation.setMachineCode(machineCode);
        allocation.setTaskAllocations(new ArrayList<>());
        allocation.setUsedCapacity(0);
        allocation.setRemainingCapacity(getMachineDailyCapacity(machineCode, context));
        return allocation;
    }

    private int getMachineDailyCapacity(String machineCode, ScheduleContextDTO context) {
        if (context.getAvailableMachines() != null) {
            for (MdmMoldingMachine machine : context.getAvailableMachines()) {
                if (machine.getCxMachineCode().equals(machineCode)) {
                    return machine.getMaxDayCapacity() != null ? machine.getMaxDayCapacity() : 1200;
                }
            }
        }
        return 1200;
    }

    private void allocateTaskToMachine(
            CoreScheduleAlgorithmService.MachineAllocationResult allocation,
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextDTO context) {

        int quantity = task.getPlannedProduction() != null && task.getPlannedProduction() > 0 
                ? task.getPlannedProduction() 
                : task.getDemandQuantity();

        CoreScheduleAlgorithmService.TaskAllocation taskAllocation = new CoreScheduleAlgorithmService.TaskAllocation();
        taskAllocation.setMaterialCode(task.getMaterialCode());
        taskAllocation.setMaterialName(task.getMaterialName());
        taskAllocation.setStructureName(task.getStructureName());
        taskAllocation.setQuantity(quantity);
        taskAllocation.setPriority(task.getPriority());
        taskAllocation.setStockHours(task.getStockHours());
        taskAllocation.setIsTrialTask(task.getIsTrialTask());
        taskAllocation.setIsEndingTask(task.getIsEndingTask());
        taskAllocation.setEndingSurplusQty(task.getEndingSurplusQty());
        taskAllocation.setIsMainProduct(task.getIsMainProduct());
        taskAllocation.setIsContinueTask(task.getIsContinueTask());

        allocation.getTaskAllocations().add(taskAllocation);
        allocation.setUsedCapacity(allocation.getUsedCapacity() + quantity);
        allocation.setRemainingCapacity(allocation.getRemainingCapacity() - quantity);
    }
}
