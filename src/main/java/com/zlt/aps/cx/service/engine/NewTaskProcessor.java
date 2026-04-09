package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.service.impl.CoreScheduleAlgorithmServiceImpl;
import com.zlt.aps.cx.service.engine.ScheduleDayTypeHelper;
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
    private final CoreScheduleAlgorithmServiceImpl coreScheduleAlgorithmService;
    private final ScheduleDayTypeHelper scheduleDayTypeHelper;

    /** 默认整车容量 */
    private static final int DEFAULT_TRIP_CAPACITY = 12;
    
    /** 默认机台种类上限 */
    private static final int DEFAULT_MAX_TYPES_PER_MACHINE = 4;

    /**
     * 处理普通新增任务
     *
     * <p>方案A：合并续作+新增重新均衡
     *
     * <p><b>量试约束</b>：若新增任务（量试）的物料+结构与试制任务一致，
     * 必须安排在同一机台上（试制优先占机台）。
     */
    public List<CoreScheduleAlgorithmService.MachineAllocationResult> processNewTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> newTasks,
            ScheduleContextVo context,
            LocalDate scheduleDate,
            List<CxShiftConfig> dayShifts,
            int day,
            List<CoreScheduleAlgorithmService.MachineAllocationResult> existAllocations,
            List<CoreScheduleAlgorithmService.MachineAllocationResult> trialAllocations) {

        if (CollectionUtils.isEmpty(newTasks)) {
            return existAllocations != null ? existAllocations : new ArrayList<>();
        }

        log.info("========== 开始处理新增任务，共 {} 个任务 ==========", newTasks.size());

        // 停产日不排新增任务（停产标识日之后才算停产，停产日当天有量）
        CoreScheduleAlgorithmServiceImpl.DayFlagInfo flagInfo =
                scheduleDayTypeHelper.getDayFlagInfo(context.getCurrentScheduleDate());
        if (flagInfo != null && "0".equals(flagInfo.dayFlag)
                && context.getCurrentScheduleDate().isAfter(flagInfo.nearestDate)) {
            log.info("停产日（已停产）不排新增任务，dayFlag={}, 标识日={}",
                    flagInfo.dayFlag, flagInfo.nearestDate);
            return existAllocations != null ? existAllocations : new ArrayList<>();
        }

        // Step 1: 排序新增任务
        sortNewTasks(newTasks, context);

        // Step 2: 构建试制约束映射（物料+结构 → 试制机台）
        // 量试必须与同物料+结构的试制安排在同一机台
        Map<String, String> trialMachineMap = buildTrialMachineMap(trialAllocations);
        log.info("试制约束映射：{} 个物料有试制要求", trialMachineMap.size());

        // Step 3: 按结构分组
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

            // 记录试制机台（用于均衡时标记为已占用，不参与均衡）
            // 量试任务在均衡后单独分配到试制机台
            if (!CollectionUtils.isEmpty(trialAllocations)) {
                for (CoreScheduleAlgorithmService.MachineAllocationResult trialAlloc : trialAllocations) {
                    String machineCode = trialAlloc.getMachineCode();
                    // 试制机台标记为已占用（均衡时不会分配其他任务过去）
                    usedMachineCodes.add(machineCode);
                }
            }

            // 分类任务：有量试约束的 vs 正常的（量试不参与均衡）
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> volumeTrialTasks = new ArrayList<>();
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> normalTasks = new ArrayList<>();

            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
                if (trialMachineMap.containsKey(task.getMaterialCode())) {
                    volumeTrialTasks.add(task);
                } else {
                    normalTasks.add(task);
                }
            }

            if (!volumeTrialTasks.isEmpty()) {
                log.info("结构 {} 有 {} 个量试任务需均衡后分配到试制机台", structureName, volumeTrialTasks.size());
            }

            // 合并续作和正常新增任务（量试不参与均衡，等待均衡后分配到试制机台）
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> allTasksForStructure = new ArrayList<>();
            allTasksForStructure.addAll(continueTasksForStructure);
            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : normalTasks) {
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

            // 量试任务分配到试制机台（均衡后处理）
            if (!volumeTrialTasks.isEmpty()) {
                allocateVolumeTrialsToTrialMachines(structureResults, volumeTrialTasks, trialMachineMap, context);
            }

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
     * 按任务优先级排序
     *
     * <p>与 TrialTaskProcessor 排序逻辑一致：仅按 task.priority 排序（来自月计划）。
     */
    public void sortNewTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
            ScheduleContextVo context) {
        tasks.sort((a, b) -> {
            Integer priA = a.getPriority();
            Integer priB = b.getPriority();
            return Integer.compare(
                    priA != null ? priA : Integer.MAX_VALUE,
                    priB != null ? priB : Integer.MAX_VALUE);
        });
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
            Integer assignedVulcanizeQty,
            ScheduleContextVo context) {

        // 排量使用任务的 demandQuantity（需求排量），这是根据硫化需求计算出来的
        // assignedVulcanizeQty 是均衡分配时分配的硫化机台数，仅用于记录
        int quantity = task.getDemandQuantity() != null ? task.getDemandQuantity() : 0;

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

    /**
     * 构建试制约束映射（物料编码 → 机台编码）
     *
     * <p>量试任务必须与同物料+结构的试制安排在同一机台。
     * 从试制分配结果中提取：materialCode → machineCode。
     */
    private Map<String, String> buildTrialMachineMap(
            List<CoreScheduleAlgorithmService.MachineAllocationResult> trialAllocations) {
        Map<String, String> trialMachineMap = new HashMap<>();
        if (CollectionUtils.isEmpty(trialAllocations)) {
            return trialMachineMap;
        }
        for (CoreScheduleAlgorithmService.MachineAllocationResult alloc : trialAllocations) {
            String machineCode = alloc.getMachineCode();
            if (alloc.getTaskAllocations() == null) {
                continue;
            }
            for (CoreScheduleAlgorithmService.TaskAllocation taskAlloc : alloc.getTaskAllocations()) {
                // key = materialCode，同物料的量试必须与试制同机台
                if (taskAlloc.getMaterialCode() != null) {
                    trialMachineMap.put(taskAlloc.getMaterialCode(), machineCode);
                }
            }
        }
        return trialMachineMap;
    }

    /**

    /**
     * 将量试任务分配到试制机台
     *
     * <p>在均衡完成后执行。根据 trialMachineMap（物料→机台），
     * 将量试任务追加到对应的试制机台上。
     *
     * @param structureResults        当前结构的均衡分配结果
     * @param volumeTrialTasks       量试任务列表
     * @param trialMachineMap       物料→试制机台映射
     * @param context               排程上下文
     */
    private void allocateVolumeTrialsToTrialMachines(
            List<CoreScheduleAlgorithmService.MachineAllocationResult> structureResults,
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> volumeTrialTasks,
            Map<String, String> trialMachineMap,
            ScheduleContextVo context) {

        for (CoreScheduleAlgorithmService.DailyEmbryoTask volTask : volumeTrialTasks) {
            String materialCode = volTask.getMaterialCode();
            String machineCode = trialMachineMap.get(materialCode);
            if (machineCode == null) {
                log.warn("量试任务 {} 无法找到对应的试制机台，跳过", materialCode);
                continue;
            }

            // 在结构结果中查找对应机台
            CoreScheduleAlgorithmService.MachineAllocationResult targetAlloc = null;
            for (CoreScheduleAlgorithmService.MachineAllocationResult alloc : structureResults) {
                if (alloc.getMachineCode().equals(machineCode)) {
                    targetAlloc = alloc;
                    break;
                }
            }

            if (targetAlloc == null) {
                log.warn("量试任务 {} 的试制机台 {} 不在均衡结果中，跳过", materialCode, machineCode);
                continue;
            }

            // 分配量试任务
            int demand = volTask.getDemandQuantity() != null ? volTask.getDemandQuantity() : 0;
            volTask.setPlannedProduction(demand);

            CoreScheduleAlgorithmService.TaskAllocation taskAlloc = new CoreScheduleAlgorithmService.TaskAllocation();
            taskAlloc.setMaterialCode(materialCode);
            taskAlloc.setMaterialName(volTask.getMaterialName());
            taskAlloc.setStructureName(volTask.getStructureName());
            taskAlloc.setQuantity(demand);
            taskAlloc.setPriority(volTask.getPriority());
            taskAlloc.setStockHours(volTask.getStockHours());
            taskAlloc.setIsTrialTask(false);
            taskAlloc.setIsContinueTask(false);
            taskAlloc.setIsMainProduct(volTask.getIsMainProduct());

            targetAlloc.getTaskAllocations().add(taskAlloc);
            targetAlloc.setUsedCapacity(targetAlloc.getUsedCapacity() + demand);
            targetAlloc.setRemainingCapacity(targetAlloc.getRemainingCapacity() - demand);

            log.info("量试任务 {} 分配到试制机台 {}，计划量={}", materialCode, machineCode, demand);
        }
    }
}

