package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.config.CxStructurePriority;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 普通新增任务处理器
 * 
 * <p>负责 S5.3 普通新增任务排产：
 * <ul>
 *   <li>任务排序：月计划优先级→收尾→新胎胚</li>
 *   <li>按结构分组处理</li>
 *   <li>使用 {@link BalancingService} 均衡分配</li>
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
            ScheduleContextVo context,
            LocalDate scheduleDate,
            List<CxShiftConfig> dayShifts,
            int day,
            List<CoreScheduleAlgorithmService.MachineAllocationResult> existAllocations) {

        if (CollectionUtils.isEmpty(newTasks)) {
            return existAllocations != null ? existAllocations : new ArrayList<>();
        }

        log.info("========== 开始处理新增任务，共 {} 个任务 ==========", newTasks.size());

        // 调试：检查任务排量
        int totalDemandQty = 0;
        int totalVulcanizeCount = 0;
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : newTasks) {
            int demandQty = task.getDemandQuantity() != null ? task.getDemandQuantity() : 0;
            int vulcanizeCount = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;
            totalDemandQty += demandQty;
            totalVulcanizeCount += vulcanizeCount;
        }
        log.info("【DEBUG】新增任务统计: 总任务数={}, 总硫化机台数={}, 总需求排量={}", 
                newTasks.size(), totalVulcanizeCount, totalDemandQty);

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

        // Step 4: 按结构分组处理
        List<CoreScheduleAlgorithmService.MachineAllocationResult> allResults = new ArrayList<>();
        Set<String> usedMachineCodes = new HashSet<>();

        for (Map.Entry<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> entry : structureTaskMap.entrySet()) {
            String structureName = entry.getKey();
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks = entry.getValue();

            log.info("--- 处理结构 {}，共 {} 个新增任务 ---", structureName, tasks.size());

            // 获取该结构可用的机台（从结构分配配置中获取）
            List<MdmMoldingMachine> structureMachines = getMachinesForStructure(
                    structureName, availableMachines, scheduleDate, context);

            // 排除已被其他结构使用的机台
            structureMachines = structureMachines.stream()
                    .filter(m -> !usedMachineCodes.contains(m.getCxMachineCode()))
                    .collect(Collectors.toList());

            if (structureMachines.isEmpty()) {
                log.warn("结构 {} 没有可用机台，跳过", structureName);
                continue;
            }
            log.info("结构 {} 有 {} 台可用机台", structureName, structureMachines.size());

            // 获取续作任务中属于该结构的任务
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> continueTasksForStructure = new ArrayList<>();
            Map<String, Set<String>> machineHistoryMap = new HashMap<>();

            if (existAllocations != null) {
                for (CoreScheduleAlgorithmService.MachineAllocationResult allocation : existAllocations) {
                    String machineCode = allocation.getMachineCode();
                    Set<String> embryos = new HashSet<>();

                    for (CoreScheduleAlgorithmService.TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                        if (structureName.equals(taskAlloc.getStructureName())) {
                            embryos.add(taskAlloc.getMaterialCode());

                            // 重建续作任务
                            CoreScheduleAlgorithmService.DailyEmbryoTask continueTask = new CoreScheduleAlgorithmService.DailyEmbryoTask();
                            continueTask.setMaterialCode(taskAlloc.getMaterialCode());
                            continueTask.setMaterialName(taskAlloc.getMaterialName());
                            continueTask.setStructureName(taskAlloc.getStructureName());
                            continueTask.setIsContinueTask(true);
                            continueTask.setIsEndingTask(taskAlloc.getIsEndingTask());
                            continueTask.setIsMainProduct(taskAlloc.getIsMainProduct());
                            continueTask.setPlannedProduction(taskAlloc.getQuantity());

                            int load = (int) Math.ceil((double) taskAlloc.getQuantity() / DEFAULT_TRIP_CAPACITY);
                            continueTask.setVulcanizeMachineCount(load);

                            continueTasksForStructure.add(continueTask);
                        }
                    }

                    if (!embryos.isEmpty()) {
                        machineHistoryMap.put(machineCode, embryos);
                    }
                }
            }

            // 合并续作和新增任务
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> allTasksForStructure = new ArrayList<>();
            allTasksForStructure.addAll(continueTasksForStructure);

            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
                task.setIsContinueTask(false);
                int demand = task.getPlannedProduction() != null && task.getPlannedProduction() > 0
                        ? task.getPlannedProduction() : task.getDemandQuantity();
                int load = (int) Math.ceil((double) demand / DEFAULT_TRIP_CAPACITY);
                task.setVulcanizeMachineCount(load);
                allTasksForStructure.add(task);

                log.debug("新增任务：materialCode={}, demand={}, load={} (DEFAULT_TRIP_CAPACITY={})",
                        task.getMaterialCode(), demand, load, DEFAULT_TRIP_CAPACITY);
            }

            // 构建机台最大硫化机数映射（根据每台机台的机型+结构获取）
            Map<String, Integer> machineMaxLhMap = buildMachineMaxLhMap(structureMachines, structureName, context);

            // 构建机台最大胎胚种类数映射（根据每台机台的机型+结构获取）
            Map<String, Integer> machineMaxEmbryoTypesMap = buildMachineMaxEmbryoTypesMap(structureMachines, structureName, context);

            // 统计总需求
            int totalDemand = allTasksForStructure.stream()
                    .mapToInt(t -> t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 0)
                    .sum();

            log.info("结构 {} 开始均衡分配：{} 个任务，{} 台机台，总硫化机台数需求={}",
                    structureName, allTasksForStructure.size(), structureMachines.size(), totalDemand);

            // 使用 BalancingService 均衡分配
            BalancingService.BalancingResult balancingResult = balancingService.balanceEmbryosToMachinesWithMachineCapacity(
                    allTasksForStructure,
                    convertToConfigs(structureMachines),
                    machineHistoryMap,
                    machineMaxLhMap,
                    machineMaxEmbryoTypesMap,
                    true,  // 强制保留历史任务
                    context);

            log.info("结构 {} 均衡分配完成", structureName);

            // 构建分配结果
            List<CoreScheduleAlgorithmService.MachineAllocationResult> structureResults = 
                    buildResultsFromBalancingResult(balancingResult, allTasksForStructure, context);

            allResults.addAll(structureResults);

            // 记录已使用的机台
            for (CoreScheduleAlgorithmService.MachineAllocationResult result : structureResults) {
                usedMachineCodes.add(result.getMachineCode());
            }
        }

        log.info("========== 新增任务处理完成，共 {} 台机台分配任务 ==========", allResults.size());
        return allResults;
    }

    /**
     * 获取结构可用的机台列表
     */
    private List<MdmMoldingMachine> getMachinesForStructure(
            String structureName,
            List<MdmMoldingMachine> allMachines,
            LocalDate scheduleDate,
            ScheduleContextVo context) {

        // 从结构分配配置中获取该结构的机台编码
        Set<String> structureMachineCodes = new HashSet<>();
        if (context.getStructureAllocationMap() != null) {
            List<MpCxCapacityConfiguration> configs = context.getStructureAllocationMap().get(structureName);
            if (configs != null) {
                int day = scheduleDate.getDayOfMonth();
                for (MpCxCapacityConfiguration config : configs) {
                    if (config.getBeginDay() != null && config.getEndDay() != null
                            && config.getBeginDay() <= day && config.getEndDay() >= day) {
                        structureMachineCodes.add(config.getCxMachineCode());
                    }
                }
            }
        }

        // 如果没有配置，返回所有机台
        if (structureMachineCodes.isEmpty()) {
            return allMachines;
        }

        // 过滤出该结构的机台
        return allMachines.stream()
                .filter(m -> structureMachineCodes.contains(m.getCxMachineCode()))
                .collect(Collectors.toList());
    }

    /**
     * 构建机台最大硫化机数映射
     */
    private Map<String, Integer> buildMachineMaxLhMap(
            List<MdmMoldingMachine> machines,
            String structureName,
            ScheduleContextVo context) {

        Map<String, Integer> result = new HashMap<>();

        // 构建 机型_结构 -> 最大硫化机数 映射
        Map<String, Integer> typeStructureMap = new HashMap<>();
        List<MdmStructureLhRatio> ratios = context.getStructureLhRatios();
        
        log.info("结构 {} 构建 maxLh 映射，配比数据: {}", structureName, ratios != null ? ratios.size() : "null");
        
        if (ratios != null) {
            int matchCount = 0;
            for (MdmStructureLhRatio ratio : ratios) {
                String key = ratio.getCxMachineTypeCode() + "_" + ratio.getStructureName();
                if (ratio.getLhMachineMaxQty() != null) {
                    typeStructureMap.put(key, ratio.getLhMachineMaxQty());
                    // 只记录匹配当前结构的
                    if (structureName.equals(ratio.getStructureName())) {
                        log.info("  配比匹配: 机型={}, 结构={}, 硫化机上限={}", 
                                ratio.getCxMachineTypeCode(), ratio.getStructureName(), ratio.getLhMachineMaxQty());
                        matchCount++;
                    }
                }
            }
            log.info("结构 {} 从配比表找到 {} 条配置", structureName, matchCount);
        }

        int fallbackCount = 0;
        for (MdmMoldingMachine machine : machines) {
            String machineCode = machine.getCxMachineCode();
            String machineType = machine.getCxMachineTypeCode();

            String key = machineType + "_" + structureName;
            Integer maxLh = typeStructureMap.get(key);

            // 如果找不到，使用机台本身的硫化机上限
            if (maxLh == null) {
                maxLh = machine.getLhMachineMaxQty() != null ? machine.getLhMachineMaxQty() : 10;
                log.info("  机台 {} 机型 {} 未找到配比，使用默认值 {}", machineCode, machineType, maxLh);
                fallbackCount++;
            }

            result.put(machineCode, maxLh);
        }

        if (fallbackCount > 0) {
            log.warn("结构 {} 有 {}/{} 台机台未找到配比配置", structureName, fallbackCount, machines.size());
        }
        return result;
    }

    /**
     * 构建机台最大胎胚种类数映射
     *
     * <p>根据每台机台的机型 + 结构，从 MdmStructureLhRatio 获取对应的最大胎胚种类数
     */
    private Map<String, Integer> buildMachineMaxEmbryoTypesMap(
            List<MdmMoldingMachine> machines,
            String structureName,
            ScheduleContextVo context) {

        Map<String, Integer> result = new HashMap<>();

        // 构建 机型_结构 -> 最大胎胚种类数 映射
        Map<String, Integer> typeStructureMap = new HashMap<>();
        List<MdmStructureLhRatio> ratios = context.getStructureLhRatios();
        if (ratios != null) {
            for (MdmStructureLhRatio ratio : ratios) {
                String key = ratio.getCxMachineTypeCode() + "_" + ratio.getStructureName();
                if (ratio.getMaxEmbryoQty() != null) {
                    typeStructureMap.put(key, ratio.getMaxEmbryoQty());
                }
            }
        }

        for (MdmMoldingMachine machine : machines) {
            String machineCode = machine.getCxMachineCode();
            String machineType = machine.getCxMachineTypeCode();

            String key = machineType + "_" + structureName;
            Integer maxTypes = typeStructureMap.get(key);

            // 如果找不到，使用默认值
            if (maxTypes == null) {
                maxTypes = context.getMaxTypesPerMachine() != null ? context.getMaxTypesPerMachine() : DEFAULT_MAX_TYPES_PER_MACHINE;
            }

            result.put(machineCode, maxTypes);
        }

        return result;
    }

    /**
     * 转换为配置格式
     */
    private List<MpCxCapacityConfiguration> convertToConfigs(List<MdmMoldingMachine> machines) {
        return machines.stream()
                .map(m -> {
                    MpCxCapacityConfiguration config = new MpCxCapacityConfiguration();
                    config.setCxMachineCode(m.getCxMachineCode());
                    return config;
                })
                .collect(Collectors.toList());
    }

    /**
     * 从 BalancingService 结果构建 MachineAllocationResult 列表
     */
    private List<CoreScheduleAlgorithmService.MachineAllocationResult> buildResultsFromBalancingResult(
            BalancingService.BalancingResult balancingResult,
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
            ScheduleContextVo context) {

        List<CoreScheduleAlgorithmService.MachineAllocationResult> results = new ArrayList<>();

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
                    allocateTaskToMachine(allocation, task, embryoAssignment.getAssignedQty(), context);
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
            ScheduleContextVo context) {
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

    private int getMonthPlanPriority(String materialCode, ScheduleContextVo context) {
        List<CxStructurePriority> priorities = context.getStructurePriorities();
        if (priorities != null) {
            for (CxStructurePriority priority : priorities) {
                // TODO: 需要根据物料编码匹配结构
            }
        }
        return 999;
    }

    private CoreScheduleAlgorithmService.MachineAllocationResult createMachineAllocation(
            String machineCode, ScheduleContextVo context) {
        CoreScheduleAlgorithmService.MachineAllocationResult allocation = new CoreScheduleAlgorithmService.MachineAllocationResult();
        allocation.setMachineCode(machineCode);
        allocation.setTaskAllocations(new ArrayList<>());
        allocation.setUsedCapacity(0);
        allocation.setRemainingCapacity(getMachineDailyCapacity(machineCode, context));
        return allocation;
    }

    private int getMachineDailyCapacity(String machineCode, ScheduleContextVo context) {
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
            ScheduleContextVo context) {
        // 默认不指定分配的硫化机台数，使用task的计划排量或需求排量
        allocateTaskToMachine(allocation, task, null, context);
    }

    private void allocateTaskToMachine(
            CoreScheduleAlgorithmService.MachineAllocationResult allocation,
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            Integer assignedVulcanizeQty,
            ScheduleContextVo context) {

        // 排量使用任务的 demandQuantity（需求排量），这是根据硫化需求计算出来的
        // assignedVulcanizeQty 是均衡分配时分配的硫化机台数，仅用于记录
        int quantity = task.getDemandQuantity() != null ? task.getDemandQuantity() : 0;

        // 调试日志：检查排量来源
        System.err.println("[allocateTask] materialCode=" + task.getMaterialCode() 
                + ", demandQuantity=" + quantity 
                + ", assignedVulcanizeQty=" + assignedVulcanizeQty);

        CoreScheduleAlgorithmService.TaskAllocation taskAllocation = new CoreScheduleAlgorithmService.TaskAllocation();
        taskAllocation.setMaterialCode(task.getMaterialCode());
        taskAllocation.setMaterialName(task.getMaterialName());
        taskAllocation.setStructureName(task.getStructureName());
        taskAllocation.setQuantity(quantity);  // 使用 demandQuantity 作为排量
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
