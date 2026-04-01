package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.mp.api.domain.entity.MdmCxMachineFixed;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 试制/量试任务处理器
 * 
 * <p>负责试制/量试任务的机台分配，特殊约束如下：
 * <ul>
 *   <li>试制/量试按月计划优先级排序</li>
 *   <li>同一个胎胚的试制和量试要在同一台成型机做</li>
 *   <li>试制/量试只能安排在早班或中班（7:30-15:00）</li>
 *   <li>试制数量必须是双数</li>
 *   <li>试制/量试不参与均衡量少</li>
 * </ul>
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrialTaskProcessor {

    /** 早班开始时间 */
    private static final LocalTime MORNING_SHIFT_START = LocalTime.of(7, 30);
    
    /** 中班结束时间 */
    private static final LocalTime AFTERNOON_SHIFT_END = LocalTime.of(15, 0);
    
    /** 默认机台种类上限 */
    private static final int DEFAULT_MAX_TYPES_PER_MACHINE = 4;

    /**
     * 处理试制/量试任务
     *
     * <p>处理流程：
     * <ol>
     *   <li>按月计划优先级排序</li>
     *   <li>同胎胚的试制和量试分配到同一机台</li>
     *   <li>选择有空闲产能的机台</li>
     *   <li>检查班次约束（早班或中班）</li>
     * </ol>
     *
     * @param trialTasks       试制任务列表
     * @param context          排程上下文
     * @param scheduleDate     排程日期
     * @param dayShifts        班次配置
     * @param availableMachines 可用机台列表（已排除续作和新增任务的机台）
     * @return 机台分配结果列表
     */
    public List<CoreScheduleAlgorithmService.MachineAllocationResult> processTrialTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> trialTasks,
            ScheduleContextDTO context,
            LocalDate scheduleDate,
            List<CxShiftConfig> dayShifts,
            List<MdmMoldingMachine> availableMachines) {

        List<CoreScheduleAlgorithmService.MachineAllocationResult> results = new ArrayList<>();

        if (CollectionUtils.isEmpty(trialTasks)) {
            return results;
        }

        log.info("========== 开始处理试制/量试任务，共 {} 个任务 ==========", trialTasks.size());

        // Step 1: 检查班次约束（必须有早班或中班）
        boolean hasValidShift = checkShiftConstraint(dayShifts);
        if (!hasValidShift) {
            log.warn("没有早班或中班，无法安排试制/量试任务");
            return results;
        }

        // Step 2: 按胎胚编码分组（同胎胚试制和量试要在同一机台）
        Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> embryoTaskMap = trialTasks.stream()
                .filter(t -> t.getMaterialCode() != null)
                .collect(Collectors.groupingBy(
                        CoreScheduleAlgorithmService.DailyEmbryoTask::getMaterialCode,
                        LinkedHashMap::new,
                        Collectors.toList()));

        // Step 3: 按月计划优先级排序胎胚组
        List<String> sortedEmbryoCodes = embryoTaskMap.keySet().stream()
                .sorted((a, b) -> {
                    int priorityA = getMonthPlanPriority(a, context);
                    int priorityB = getMonthPlanPriority(b, context);
                    return Integer.compare(priorityA, priorityB);
                })
                .collect(Collectors.toList());

        // Step 4: 记录已使用的机台（胎胚 -> 机台编码）
        Map<String, String> embryoMachineMap = new HashMap<>();
        
        // 记录机台已分配的任务
        Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> machineTaskMap = new HashMap<>();

        // Step 5: 为每个胎胚组分配机台
        for (String embryoCode : sortedEmbryoCodes) {
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks = embryoTaskMap.get(embryoCode);

            // 检查是否已有该胎胚的机台分配（同胎胚试制和量试在同一机台）
            String assignedMachineCode = embryoMachineMap.get(embryoCode);

            // 选择机台
            MdmMoldingMachine selectedMachine = selectMachineForTrial(
                    embryoCode, tasks, assignedMachineCode, availableMachines, 
                    machineTaskMap, context);

            if (selectedMachine == null) {
                log.warn("试制任务 {} 无法找到合适的机台", embryoCode);
                continue;
            }

            // 记录分配
            embryoMachineMap.put(embryoCode, selectedMachine.getCxMachineCode());
            machineTaskMap.computeIfAbsent(selectedMachine.getCxMachineCode(), k -> new ArrayList<>())
                    .addAll(tasks);
        }

        // Step 6: 构建分配结果
        for (Map.Entry<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> entry : machineTaskMap.entrySet()) {
            String machineCode = entry.getKey();
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks = entry.getValue();

            CoreScheduleAlgorithmService.MachineAllocationResult allocation = createMachineAllocation(machineCode, context);
            
            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
                // 计算排产量（试制数量必须是双数）
                int plannedQty = calculateTrialPlannedProduction(task);
                task.setPlannedProduction(plannedQty);
                
                // 标记为试制任务
                task.setIsTrialTask(true);
                
                // 分配任务
                allocateTaskToMachine(allocation, task, context);
            }

            if (!allocation.getTaskAllocations().isEmpty()) {
                results.add(allocation);
            }
        }

        log.info("========== 试制/量试任务处理完成，共 {} 台机台分配任务 ==========", results.size());
        return results;
    }

    /**
     * 检查班次约束（必须有早班或中班）
     */
    private boolean checkShiftConstraint(List<CxShiftConfig> dayShifts) {
        if (CollectionUtils.isEmpty(dayShifts)) {
            return false;
        }

        for (CxShiftConfig shift : dayShifts) {
            LocalTime shiftStart = shift.getShiftStartTime();
            LocalTime shiftEnd = shift.getShiftEndTime();
            
            if (shiftStart != null && shiftEnd != null) {
                // 早班：开始时间 >= 7:30 且 结束时间 <= 15:00
                // 或者班次与早班/中班有重叠
                if (!shiftEnd.isBefore(MORNING_SHIFT_START) && !shiftStart.isAfter(AFTERNOON_SHIFT_END)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 获取月计划优先级
     */
    private int getMonthPlanPriority(String materialCode, ScheduleContextDTO context) {
        // TODO: 从月计划配置获取优先级
        return 999;
    }

    /**
     * 为试制任务选择机台
     *
     * <p>选择规则：
     * <ul>
     *   <li>如果已分配机台，优先使用</li>
     *   <li>选择有空闲产能的机台</li>
     *   <li>考虑结构约束</li>
     *   <li>优先选择月计划推荐的机台</li>
     * </ul>
     */
    private MdmMoldingMachine selectMachineForTrial(
            String embryoCode,
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
            String assignedMachineCode,
            List<MdmMoldingMachine> availableMachines,
            Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> machineTaskMap,
            ScheduleContextDTO context) {

        // 如果已有分配的机台，优先使用
        if (assignedMachineCode != null) {
            for (MdmMoldingMachine machine : availableMachines) {
                if (machine.getCxMachineCode().equals(assignedMachineCode)) {
                    return machine;
                }
            }
        }

        // 获取结构名称
        String structureName = tasks.isEmpty() ? null : tasks.get(0).getStructureName();

        // 获取月计划推荐的机台
        List<String> recommendedMachines = getRecommendedMachines(embryoCode, context);

        MdmMoldingMachine bestMachine = null;
        int bestScore = -1;

        int maxTypes = context.getMaxTypesPerMachine() != null
                ? context.getMaxTypesPerMachine()
                : DEFAULT_MAX_TYPES_PER_MACHINE;

        for (MdmMoldingMachine machine : availableMachines) {
            // 检查结构约束
            if (!checkStructureConstraint(machine, structureName, context)) {
                continue;
            }

            String machineCode = machine.getCxMachineCode();
            
            // 检查胎胚种类上限
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> existingTasks = machineTaskMap.get(machineCode);
            int currentTypes = existingTasks == null ? 0 : (int) existingTasks.stream()
                    .map(CoreScheduleAlgorithmService.DailyEmbryoTask::getMaterialCode)
                    .distinct()
                    .count();
            
            if (currentTypes >= maxTypes && !containsEmbryo(existingTasks, embryoCode)) {
                continue;
            }

            // 计算得分
            int score = calculateMachineScore(machine, embryoCode, recommendedMachines, context);
            if (score > bestScore) {
                bestScore = score;
                bestMachine = machine;
            }
        }

        return bestMachine;
    }

    /**
     * 检查结构约束
     */
    private boolean checkStructureConstraint(MdmMoldingMachine machine, String structureName, ScheduleContextDTO context) {
        if (structureName == null) {
            return true;
        }
        
        if (context.getMachineFixedConfigs() != null) {
            for (MdmCxMachineFixed fixed : context.getMachineFixedConfigs()) {
                if (fixed.getCxMachineCode().equals(machine.getCxMachineCode())) {
                    if (fixed.getDisableStructure() != null &&
                            fixed.getDisableStructure().contains(structureName)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 检查任务列表是否包含指定胎胚
     */
    private boolean containsEmbryo(List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks, String embryoCode) {
        if (tasks == null) {
            return false;
        }
        return tasks.stream().anyMatch(t -> embryoCode.equals(t.getMaterialCode()));
    }

    /**
     * 获取月计划推荐的机台列表
     */
    private List<String> getRecommendedMachines(String materialCode, ScheduleContextDTO context) {
        // 从结构排产配置获取推荐机台
        List<String> recommendedMachines = new ArrayList<>();
        
        if (context.getMaterials() != null) {
            for (var material : context.getMaterials()) {
                if (materialCode.equals(material.getMaterialCode())) {
                    String structureName = material.getStructureName();
                    if (structureName != null && context.getStructureAllocationMap() != null) {
                        List<MpCxCapacityConfiguration> configs = context.getStructureAllocationMap().get(structureName);
                        if (configs != null) {
                            for (MpCxCapacityConfiguration config : configs) {
                                if (config.getCxMachineCode() != null) {
                                    recommendedMachines.add(config.getCxMachineCode());
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
        
        return recommendedMachines;
    }

    /**
     * 计算机台得分
     */
    private int calculateMachineScore(
            MdmMoldingMachine machine,
            String embryoCode,
            List<String> recommendedMachines,
            ScheduleContextDTO context) {

        int score = 0;

        // 推荐机台加分
        if (recommendedMachines != null && recommendedMachines.contains(machine.getCxMachineCode())) {
            score += 100;
        }

        // 固定生产该结构的机台加分
        if (context.getMachineFixedConfigs() != null) {
            for (MdmCxMachineFixed fixed : context.getMachineFixedConfigs()) {
                if (fixed.getCxMachineCode().equals(machine.getCxMachineCode())) {
                    score += 50;
                    break;
                }
            }
        }

        return score;
    }

    /**
     * 计算试制排产量
     * 
     * <p>试制数量必须是双数
     */
    private int calculateTrialPlannedProduction(CoreScheduleAlgorithmService.DailyEmbryoTask task) {
        int demand = task.getDemandQuantity() != null ? task.getDemandQuantity() : 0;
        
        // 向上取整到双数
        if (demand % 2 != 0) {
            demand = demand + 1;
        }
        
        return demand;
    }

    /**
     * 创建机台分配结果
     */
    private CoreScheduleAlgorithmService.MachineAllocationResult createMachineAllocation(
            String machineCode, ScheduleContextDTO context) {
        CoreScheduleAlgorithmService.MachineAllocationResult allocation = new CoreScheduleAlgorithmService.MachineAllocationResult();
        allocation.setMachineCode(machineCode);
        allocation.setTaskAllocations(new ArrayList<>());
        allocation.setUsedCapacity(0);
        allocation.setRemainingCapacity(getMachineDailyCapacity(machineCode, context));
        return allocation;
    }

    /**
     * 获取机台日产能
     */
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

    /**
     * 分配任务到机台
     */
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
        taskAllocation.setIsTrialTask(true);
        taskAllocation.setIsEndingTask(task.getIsEndingTask());
        taskAllocation.setEndingSurplusQty(task.getEndingSurplusQty());
        taskAllocation.setIsMainProduct(task.getIsMainProduct());

        allocation.getTaskAllocations().add(taskAllocation);
        allocation.setUsedCapacity(allocation.getUsedCapacity() + quantity);
        allocation.setRemainingCapacity(allocation.getRemainingCapacity() - quantity);
    }
}
