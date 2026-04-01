package com.zlt.aps.cx.service;

import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.config.CxStructurePriority;
import com.zlt.aps.cx.entity.config.CxStructureShiftCapacity;
import com.zlt.aps.mp.api.domain.entity.MdmCxMachineFixed;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 新增任务处理器
 * 
 * <p>负责 S5.3 新增任务（试制/量试/常规新增）排产：
 * <ul>
 *   <li>任务排序：试制优先→月计划优先级→收尾→新胎胚</li>
 *   <li>机台选择：胎胚种类均衡+成型硫化配比均衡</li>
 *   <li>同胎胚试制和量试在同一机台</li>
 *   <li>试制任务限制在早班或中班</li>
 * </ul>
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewTaskProcessor {

    /** 默认整车容量 */
    private static final int DEFAULT_TRIP_CAPACITY = 12;
    
    /** 默认机台种类上限 */
    private static final int DEFAULT_MAX_TYPES_PER_MACHINE = 4;

    /**
     * 处理试制和新增任务
     *
     * <p>S5.3 新增任务排产流程：
     * <ol>
     *   <li>按排序规则对任务排序</li>
     *   <li>获取月计划推荐的机台列表</li>
     *   <li>选择最佳机台（考虑胎胚种类均衡、成型硫化配比均衡）</li>
     *   <li>计算待排产量并分配任务</li>
     * </ol>
     *
     * <p>排序规则：
     * <ol>
     *   <li>按照月计划优先级排序</li>
     *   <li>试制/量试一定在月计划优先</li>
         <li>试制/量试不参与均衡量少</li>
     *   <li>同一个胎胚的试制和量试要在同一台成型机做</li>
     *   <li>新胎胚的优先级高于普通的新增胎胚，但不能挤掉已排好的实单</li>
     *   <li>试制/量试只能安排在早班或中班（7:30-15:00），数量必须是双数</li>
     * </ol>
     *
     * @param trialTasks        试制任务列表
     * @param newTasks          新增任务列表
     * @param context           排程上下文
     * @param scheduleDate      排程日期
     * @param dayShifts         班次配置
     * @param day               排程天数
     * @param existAllocations  已有的分配结果（续作任务）
     * @return 机台分配结果列表
     */
    public List<CoreScheduleAlgorithmService.MachineAllocationResult> processTrialAndNewTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> trialTasks,
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> newTasks,
            ScheduleContextDTO context,
            LocalDate scheduleDate,
            List<CxShiftConfig> dayShifts,
            int day,
            List<CoreScheduleAlgorithmService.MachineAllocationResult> existAllocations) {

        List<CoreScheduleAlgorithmService.MachineAllocationResult> results = new ArrayList<>();

        // 合并试制和新增任务
        List<CoreScheduleAlgorithmService.DailyEmbryoTask> allTasks = new ArrayList<>();
        allTasks.addAll(trialTasks);
        allTasks.addAll(newTasks);

        if (allTasks.isEmpty()) {
            return results;
        }

        // 判断是否开产日或停产日
        boolean isOpeningDay = Boolean.TRUE.equals(context.getIsOpeningDay()) && day == 1;
        boolean isClosingDay = Boolean.TRUE.equals(context.getIsClosingDay());

        // 排序
        sortTrialAndNewTasks(allTasks, context);

        // 获取已有机台的分配情况（用于均衡）
        Map<String, Integer> machineUsedCapacity = calculateMachineUsedCapacity(existAllocations);
        
        // 记录每个机台已分配的胎胚种类（用于均衡）
        Map<String, Set<String>> machineEmbryoTypes = calculateMachineEmbryoTypes(existAllocations);
        
        // 记录试制任务已使用的机台（同胎胚试制和量试要在同一台）
        Map<String, String> trialEmbryoMachineMap = new HashMap<>();

        // 获取可用机台列表
        List<MdmMoldingMachine> availableMachines = context.getAvailableMachines();
        if (availableMachines == null || availableMachines.isEmpty()) {
            log.warn("没有可用的成型机台");
            return results;
        }

        // 为每个任务分配机台
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : allTasks) {
            // 停产日不排新增任务
            if (isClosingDay) {
                log.debug("停产日不排新增任务: {}", task.getMaterialCode());
                continue;
            }
            
            // 获取月计划推荐的机台列表
            List<String> recommendedMachines = getRecommendedMachines(task.getMaterialCode(), context);
            task.setRecommendedMachines(recommendedMachines);

            // 检查是否是同胎胚试制任务（需要在同一机台）
            String trialMachineCode = trialEmbryoMachineMap.get(task.getMaterialCode());
            
            // 选择最佳机台
            MdmMoldingMachine bestMachine = selectBestMachineForNewTask(
                    task, availableMachines, machineUsedCapacity, machineEmbryoTypes, 
                    recommendedMachines, trialMachineCode, context);

            if (bestMachine == null) {
                log.warn("任务 {} 无可用机台", task.getMaterialCode());
                continue;
            }

            // 记录试制任务的机台
            if (Boolean.TRUE.equals(task.getIsTrialTask())) {
                trialEmbryoMachineMap.put(task.getMaterialCode(), bestMachine.getCxMachineCode());
            }

            // 计算待排产量
            calculatePlannedProduction(task, context, scheduleDate, isOpeningDay);
            
            // 开停产处理
            handleOpeningClosingDay(task, context, dayShifts, isOpeningDay, isClosingDay);

            // 获取或创建机台分配结果
            CoreScheduleAlgorithmService.MachineAllocationResult allocation = findOrCreateAllocation(
                    bestMachine.getCxMachineCode(), results, context);

            // 分配任务
            allocateTaskToMachine(allocation, task, context);

            // 更新机台已用产能
            int quantity = task.getPlannedProduction() != null && task.getPlannedProduction() > 0
                    ? task.getPlannedProduction() : task.getDemandQuantity();
            machineUsedCapacity.merge(bestMachine.getCxMachineCode(), quantity, Integer::sum);
            
            // 更新机台胎胚种类
            machineEmbryoTypes.computeIfAbsent(bestMachine.getCxMachineCode(), k -> new HashSet<>())
                    .add(task.getMaterialCode());
        }

        return results;
    }

    /**
     * 排序试制和新增任务
     * 
     * <p>排序规则：
     * <ol>
     *   <li>试制/量试优先</li>
     *   <li>按月计划优先级排序</li>
     *   <li>收尾任务优先</li>
     *   <li>紧急收尾优先</li>
     *   <li>新胎胚优先（但不能挤掉已排好的实单）</li>
     *   <li>按需求量排序（大的优先）</li>
     * </ol>
     */
    public void sortTrialAndNewTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks, 
            ScheduleContextDTO context) {
        tasks.sort((a, b) -> {
            // 1. 试制/量试优先
            if (Boolean.TRUE.equals(a.getIsTrialTask()) && !Boolean.TRUE.equals(b.getIsTrialTask())) {
                return -1;
            }
            if (!Boolean.TRUE.equals(a.getIsTrialTask()) && Boolean.TRUE.equals(b.getIsTrialTask())) {
                return 1;
            }

            // 2. 按月计划优先级排序
            int priorityA = getMonthPlanPriority(a.getMaterialCode(), context);
            int priorityB = getMonthPlanPriority(b.getMaterialCode(), context);
            if (priorityA != priorityB) {
                return Integer.compare(priorityA, priorityB);
            }

            // 3. 收尾任务优先
            if (Boolean.TRUE.equals(a.getIsEndingTask()) && !Boolean.TRUE.equals(b.getIsEndingTask())) {
                return -1;
            }
            if (!Boolean.TRUE.equals(a.getIsEndingTask()) && Boolean.TRUE.equals(b.getIsEndingTask())) {
                return 1;
            }

            // 4. 紧急收尾优先
            if (Boolean.TRUE.equals(a.getIsUrgentEnding()) && !Boolean.TRUE.equals(b.getIsUrgentEnding())) {
                return -1;
            }
            if (!Boolean.TRUE.equals(a.getIsUrgentEnding()) && Boolean.TRUE.equals(b.getIsUrgentEnding())) {
                return 1;
            }
            
            // 5. 10天内收尾优先
            if (Boolean.TRUE.equals(a.getIsNearEnding()) && !Boolean.TRUE.equals(b.getIsNearEnding())) {
                return -1;
            }
            if (!Boolean.TRUE.equals(a.getIsNearEnding()) && Boolean.TRUE.equals(b.getIsNearEnding())) {
                return 1;
            }
            
            // 6. 新胎胚优先
            boolean aIsNew = Boolean.TRUE.equals(a.getIsNewEmbryo());
            boolean bIsNew = Boolean.TRUE.equals(b.getIsNewEmbryo());
            if (aIsNew && !bIsNew) {
                return -1;
            }
            if (!aIsNew && bIsNew) {
                return 1;
            }

            // 7. 按需求量排序（大的优先）
            return Integer.compare(b.getDemandQuantity(), a.getDemandQuantity());
        });
    }

    /**
     * 获取月计划优先级
     */
    private int getMonthPlanPriority(String materialCode, ScheduleContextDTO context) {
        List<CxStructurePriority> priorities = context.getStructurePriorities();
        if (priorities != null) {
            for (CxStructurePriority priority : priorities) {
                // TODO: 需要根据物料编码匹配结构
            }
        }
        return 999;
    }

    /**
     * 获取月计划推荐的机台列表
     */
    private List<String> getRecommendedMachines(String materialCode, ScheduleContextDTO context) {
        // TODO: 从月计划配置获取推荐的机台列表
        return new ArrayList<>();
    }

    /**
     * 为新任务选择最佳机台
     *
     * <p>选择规则：
     * <ul>
     *   <li>优先选择月计划推荐的机台</li>
     *   <li>考虑胎胚种类均衡</li>
     *   <li>考虑成型硫化配比均衡</li>
     *   <li>同胎胚试制任务优先选择之前试制用的机台</li>
     * </ul>
     */
    public MdmMoldingMachine selectBestMachineForNewTask(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            List<MdmMoldingMachine> availableMachines,
            Map<String, Integer> machineUsedCapacity,
            Map<String, Set<String>> machineEmbryoTypes,
            List<String> recommendedMachines,
            String trialMachineCode,
            ScheduleContextDTO context) {

        MdmMoldingMachine bestMachine = null;
        int bestScore = -1;

        int maxTypes = context.getMaxTypesPerMachine() != null
                ? context.getMaxTypesPerMachine()
                : DEFAULT_MAX_TYPES_PER_MACHINE;

        for (MdmMoldingMachine machine : availableMachines) {
            // 检查结构约束
            if (!checkStructureConstraint(machine, task.getStructureName(), context)) {
                continue;
            }

            String machineCode = machine.getCxMachineCode();
            
            // 如果是试制任务且有指定机台，只考虑该机台
            if (Boolean.TRUE.equals(task.getIsTrialTask()) && trialMachineCode != null) {
                if (!machineCode.equals(trialMachineCode)) {
                    continue;
                }
            }

            int usedCapacity = machineUsedCapacity.getOrDefault(machineCode, 0);
            int remainingCapacity = (machine.getMaxDayCapacity() != null ? machine.getMaxDayCapacity() : 1200) - usedCapacity;

            int taskDemand = task.getPlannedProduction() != null && task.getPlannedProduction() > 0
                    ? task.getPlannedProduction() : task.getDemandQuantity();

            if (remainingCapacity < taskDemand) {
                continue;
            }

            // 检查胎胚种类上限
            Set<String> currentTypes = machineEmbryoTypes.getOrDefault(machineCode, new HashSet<>());
            if (!currentTypes.contains(task.getMaterialCode()) && currentTypes.size() >= maxTypes) {
                continue;
            }

            int score = calculateMachineScoreForNewTask(
                    machine, task, remainingCapacity, recommendedMachines, 
                    currentTypes.size(), maxTypes, context);
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
     * 计算新任务机台得分
     */
    private int calculateMachineScoreForNewTask(
            MdmMoldingMachine machine, 
            CoreScheduleAlgorithmService.DailyEmbryoTask task, 
            int remainingCapacity,
            List<String> recommendedMachines,
            int currentTypes,
            int maxTypes,
            ScheduleContextDTO context) {

        int score = 0;

        // 1. 剩余产能越多得分越高（均衡）
        score += remainingCapacity / 10;

        // 2. 推荐机台加分
        if (recommendedMachines != null && recommendedMachines.contains(machine.getCxMachineCode())) {
            score += 300;
        }
        
        // 3. 种类越少得分越高（均衡）
        score += (maxTypes - currentTypes) * 50;

        // 4. 固定生产该结构的机台加分
        if (context.getMachineFixedConfigs() != null) {
            for (MdmCxMachineFixed fixed : context.getMachineFixedConfigs()) {
                if (fixed.getCxMachineCode().equals(machine.getCxMachineCode())) {
                    if (fixed.getFixedStructure1() != null && 
                            fixed.getFixedStructure1().contains(task.getStructureName())) {
                        score += 200;
                    }
                }
            }
        }

        return score;
    }

    /**
     * 计算机台已用产能
     */
    private Map<String, Integer> calculateMachineUsedCapacity(
            List<CoreScheduleAlgorithmService.MachineAllocationResult> allocations) {
        Map<String, Integer> usedCapacity = new HashMap<>();
        if (allocations != null) {
            for (CoreScheduleAlgorithmService.MachineAllocationResult allocation : allocations) {
                usedCapacity.put(allocation.getMachineCode(), allocation.getUsedCapacity());
            }
        }
        return usedCapacity;
    }

    /**
     * 计算机台已分配的胎胚种类
     */
    private Map<String, Set<String>> calculateMachineEmbryoTypes(
            List<CoreScheduleAlgorithmService.MachineAllocationResult> allocations) {
        Map<String, Set<String>> embryoTypes = new HashMap<>();
        if (allocations != null) {
            for (CoreScheduleAlgorithmService.MachineAllocationResult allocation : allocations) {
                Set<String> types = new HashSet<>();
                for (CoreScheduleAlgorithmService.TaskAllocation task : allocation.getTaskAllocations()) {
                    types.add(task.getMaterialCode());
                }
                embryoTypes.put(allocation.getMachineCode(), types);
            }
        }
        return embryoTypes;
    }

    /**
     * 查找或创建机台分配结果
     */
    private CoreScheduleAlgorithmService.MachineAllocationResult findOrCreateAllocation(
            String machineCode,
            List<CoreScheduleAlgorithmService.MachineAllocationResult> results,
            ScheduleContextDTO context) {

        for (CoreScheduleAlgorithmService.MachineAllocationResult result : results) {
            if (result.getMachineCode().equals(machineCode)) {
                return result;
            }
        }

        CoreScheduleAlgorithmService.MachineAllocationResult allocation = createMachineAllocation(machineCode, context);
        results.add(allocation);
        return allocation;
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
     * 计算待排产量
     */
    public void calculatePlannedProduction(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextDTO context,
            LocalDate scheduleDate,
            boolean isOpeningDay) {
        
        BigDecimal lossRate = context.getLossRate();
        if (lossRate == null) {
            lossRate = new BigDecimal("0.02");
        }
        
        int dailyVulcanize = task.getVulcanizeDemand() != null ? task.getVulcanizeDemand() : 0;
        int allocatedStock = task.getAllocatedStock() != null ? task.getAllocatedStock() : 0;
        
        int baseProduction = Math.max(0, dailyVulcanize - allocatedStock);
        int plannedProduction = (int) Math.ceil(baseProduction * (1 + lossRate.doubleValue()));
        
        task.setPlannedProduction(plannedProduction);
    }

    /**
     * 开停产特殊处理
     */
    public void handleOpeningClosingDay(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextDTO context,
            List<CxShiftConfig> dayShifts,
            boolean isOpeningDay,
            boolean isClosingDay) {
        
        if (isClosingDay) {
            task.setPlannedProduction(0);
            task.setIsClosingDayTask(true);
            return;
        }
        
        if (isOpeningDay) {
            boolean isKeyProduct = context.getKeyProductCodes() != null 
                    && context.getKeyProductCodes().contains(task.getMaterialCode());
            
            if (isKeyProduct) {
                task.setIsKeyProductOnOpening(true);
                task.setOpeningShiftCapacity(0);
            }
            
            task.setIsOpeningDayTask(true);
        }
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
        taskAllocation.setIsTrialTask(task.getIsTrialTask());
        taskAllocation.setIsEndingTask(task.getIsEndingTask());
        taskAllocation.setEndingSurplusQty(task.getEndingSurplusQty());
        taskAllocation.setIsMainProduct(task.getIsMainProduct());

        allocation.getTaskAllocations().add(taskAllocation);
        allocation.setUsedCapacity(allocation.getUsedCapacity() + quantity);
        allocation.setRemainingCapacity(allocation.getRemainingCapacity() - quantity);
    }
}
