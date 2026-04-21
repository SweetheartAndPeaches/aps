package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
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
 * 新增任务处理器
 *
 * <p>将新增任务与同结构的续作任务合并，重新进行均衡分配。
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewTaskProcessor {

    private final BalancingService balancingService;

    /** 默认单次行程产能（条/车），用于硫化机数计算 */
    private static final int DEFAULT_TRIP_CAPACITY = 60;

    /** 默认最大硫化机台数 */
    private static final int DEFAULT_MAX_LH_MACHINE_COUNT = 10;

    /**
     * 处理新增任务
     *
     * <p>新增任务 + 续作任务 → 重新均衡。
     *
     * @param newTasks          新增任务列表
     * @param context           排程上下文
     * @param scheduleDate      排程日期
     * @param dayShifts         当天班次配置
     * @param dayShifts         当天班次配置
     * @param existAllocations  续作任务分配结果
     * @param trialAllocations 试制任务分配结果（用于量试约束）
     * @return 均衡分配结果
     */
    public List<CoreScheduleAlgorithmService.MachineAllocationResult> processNewTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> newTasks,
            ScheduleContextVo context,
            LocalDate scheduleDate,
            List<CxShiftConfig> dayShifts,
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> continueTasks,
            List<CoreScheduleAlgorithmService.MachineAllocationResult> existAllocations,
            List<CoreScheduleAlgorithmService.MachineAllocationResult> trialAllocations) {

        List<CoreScheduleAlgorithmService.MachineAllocationResult> allResults = new ArrayList<>();

        // 新增任务为空时，仍需处理续作剩余需求的均衡
        log.info("========== 开始处理新增任务，新增={}，续作={} ==========",
                CollectionUtils.isEmpty(newTasks) ? 0 : newTasks.size(),
                CollectionUtils.isEmpty(continueTasks) ? 0 : continueTasks.size());

        // Step 1: 按结构分组新增任务
        Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> structureTaskMap = new LinkedHashMap<>();
        if (!CollectionUtils.isEmpty(newTasks)) {
            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : newTasks) {
                structureTaskMap.computeIfAbsent(task.getStructureName(), k -> new ArrayList<>()).add(task);
            }
        }

        // Step 1.1: 将续作剩余 demand > 0 的任务也加入结构分组（补上只有续作没有新增的结构）
        if (!CollectionUtils.isEmpty(continueTasks)) {
            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : continueTasks) {
                int demand = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;
                if (demand > 0) {
                    structureTaskMap.computeIfAbsent(task.getStructureName(), k -> new ArrayList<>()).add(task);
                }
            }
        }

        if (structureTaskMap.isEmpty()) {
            log.info("无新增和续作剩余任务，说明续作任务全部都是1并且没有新增胎胚，跳过均衡");
            return allResults;
        }
        // Step 2: 保底预留已在 ContinueTaskProcessor 完成，此处不再做保底预留
        boolean forceKeepHistoryForBalancing = false;

        // Step 3: 按结构处理
        for (Map.Entry<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> entry : structureTaskMap.entrySet()) {
            String structureName = entry.getKey();
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> newTasksForStructure = entry.getValue();

            log.info("--- 处理结构 {}，共 {} 个新增任务 ---", structureName, newTasksForStructure.size());

            // Step 3.1: 获取该结构可安排的机台（按 PRODUCTION_VERSION 过滤）同一结构下所有任务的 productionVersion 应一致，取第一个
            String productionVersion = newTasksForStructure.get(0).getProductionVersion();
            List<MpCxCapacityConfiguration> availableMachines =
                    getAvailableMachinesForStructure(structureName, scheduleDate, context, productionVersion);
            if (availableMachines.isEmpty()) {
                log.warn("结构 {} 没有可安排的机台，跳过", structureName);
                continue;
            }

            // Step 3.2: 从 existAllocations（续作均衡结果）构建 machineHistoryMap 和机台已占容量/种类
            Map<String, Set<String>> machineHistoryMap = new HashMap<>();
            Map<String, Integer> continueLoadMap = new HashMap<>();   // 续作已占容量
            Map<String, Set<String>> continueTypeMap = new HashMap<>(); // 续作已占种类

            if (existAllocations != null) {
                for (CoreScheduleAlgorithmService.MachineAllocationResult allocation : existAllocations) {
                    String machineCode = allocation.getMachineCode();
                    Set<String> embryos = new HashSet<>();
                    int load = 0;
                    Set<String> types = new HashSet<>();
                    for (CoreScheduleAlgorithmService.TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                        if (structureName.equals(taskAlloc.getStructureName())) {
                            embryos.add(taskAlloc.getEmbryoCode());
                            types.add(taskAlloc.getEmbryoCode());
                            load += taskAlloc.getVulcanizeMachineCount() != null ? taskAlloc.getVulcanizeMachineCount() : 0;
                        }
                    }
                    if (!embryos.isEmpty()) {
                        machineHistoryMap.put(machineCode, embryos);
                        continueLoadMap.put(machineCode, load);
                        continueTypeMap.put(machineCode, types);
                    }
                }
            }

            // Step 3.3: 构建试制机台映射 materialCode → machineCode（同结构下）
            Map<String, String> trialMachineMap = buildTrialMachineMap(trialAllocations, structureName);

            // Step 3.4: 分类新增任务 - 固定量试 vs 参与均衡
            // 量试约束任务：量试任务 + 同胎胚有试制任务 → 设置约束机台，参与均衡
            // 参与均衡：所有新增任务（量试约束任务也参与，但限制候选机台）
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> constrainedTrials = new ArrayList<>();
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> balancedTasks = new ArrayList<>();

            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : newTasksForStructure) {
                task.setIsContinueTask(false);
                if (isVolumeTrialConstrained(task, trialMachineMap)) {
                    // 设置约束机台，参与均衡时只能分配到该机台
                    task.setConstrainedMachineCode(trialMachineMap.get(task.getEmbryoCode()));
                    constrainedTrials.add(task);
                }
                balancedTasks.add(task);
            }

            // 约束量试预占机台：加入 machineHistoryMap（保证DFS优先保留历史种类）
            for (CoreScheduleAlgorithmService.DailyEmbryoTask constrainedTask : constrainedTrials) {
                String targetMachine = constrainedTask.getConstrainedMachineCode();
                machineHistoryMap.computeIfAbsent(targetMachine, k -> new HashSet<>())
                        .add(constrainedTask.getEmbryoCode());
            }

            // Step 3.5: 参与均衡的任务 = balancedTasks（已包含新增任务和续作剩余任务）
            // 注意：续作剩余任务已在 Step 1.1 加入 structureTaskMap，此处不再重复添加
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> allTasksForStructure = new ArrayList<>(balancedTasks);
            
            // 统计续作剩余数量（仅用于日志）
            int continueRemaining = 0;
            if (!CollectionUtils.isEmpty(continueTasks)) {
                for (CoreScheduleAlgorithmService.DailyEmbryoTask task : continueTasks) {
                    if (structureName.equals(task.getStructureName())) {
                        int demand = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;
                        if (demand > 0) {
                            continueRemaining++;
                        }
                    }
                }
            }

            log.info("结构 {}：续作已占机台={}, 续作剩余={}, 均衡新增={}, 约束量试={}, 参与均衡总任务数={}",
                    structureName, continueLoadMap.size(), continueRemaining,
                    balancedTasks.size() - constrainedTrials.size() - continueRemaining, constrainedTrials.size(),
                    allTasksForStructure.size());

            // Step 3.6: 构建机台最大硫化机数映射
            Map<String, Integer> machineMaxLhMap = buildMachineMaxLhMap(
                    availableMachines, structureName, context);

            // Step 3.7: 构建机台最大胎胚种类数映射
            Map<String, Integer> machineMaxEmbryoTypesMap = buildMachineMaxEmbryoTypesMap(
                    availableMachines, structureName, context);

            // Step 3.8: 均衡分配
            BalancingService.BalancingResult balancingResult =
                    balancingService.balanceEmbryosToMachinesWithMachineCapacity(
                            allTasksForStructure,
                            availableMachines,
                            machineHistoryMap,
                            machineMaxLhMap,
                            machineMaxEmbryoTypesMap,
                            forceKeepHistoryForBalancing,
                            context,
                            continueLoadMap,
                            continueTypeMap);

            if (balancingResult == null
                    || CollectionUtils.isEmpty(balancingResult.getAssignments())) {
                log.warn("结构 {} 均衡分配失败，跳过", structureName);
                continue;
            }

            log.info("结构 {} 均衡分配完成", structureName);

            // Step 3.9: 构建分配结果
            for (BalancingService.MachineAssignment assignment : balancingResult.getAssignments()) {
                CoreScheduleAlgorithmService.MachineAllocationResult result =
                        new CoreScheduleAlgorithmService.MachineAllocationResult();
                result.setMachineCode(assignment.getMachineCode());
                result.setTaskAllocations(new ArrayList<>());

                int usedCapacity = 0;
                
                // 关键修复：不能简单遍历 embryoAssignments，因为同一个 embryoCode 可能对应多个不同的 task（不同物料）
                // 需要保留每个独立的 EmbryoAssignment，而不是按 embryoCode 合并
                for (BalancingService.EmbryoAssignment embryoAssignment
                        : assignment.getEmbryoAssignments()) {
                    CoreScheduleAlgorithmService.DailyEmbryoTask task =
                            embryoAssignment.getTask();

                    // 跳过续作预扣的条目（task=null，已在 ContinueTaskProcessor 中分配）
                    if (task == null) {
                        continue;
                    }

                    int assignedQty = embryoAssignment.getAssignedQty();
                    usedCapacity += assignedQty;

                    CoreScheduleAlgorithmService.TaskAllocation taskAlloc =
                            new CoreScheduleAlgorithmService.TaskAllocation();
                    // 重要：直接使用当前 embryoAssignment 对应的 task 对象，确保物料信息正确
                    taskAlloc.setEmbryoCode(task.getEmbryoCode());
                    taskAlloc.setMaterialCode(task.getMaterialCode());
                    taskAlloc.setMaterialDesc(task.getMaterialDesc());
                    taskAlloc.setMainMaterialDesc(task.getMainMaterialDesc());
                    taskAlloc.setStructureName(task.getStructureName());
                    taskAlloc.setQuantity(task.getPlannedProduction() != null ? task.getPlannedProduction() : 0);
                    taskAlloc.setVulcanizeMachineCount(assignedQty);
                    taskAlloc.setPriority(task.getPriority());
                    taskAlloc.setStockHours(task.getStockHours());
                    taskAlloc.setIsTrialTask(task.getIsTrialTask());
                    taskAlloc.setIsContinueTask(task.getIsContinueTask());
                    taskAlloc.setIsEndingTask(task.getIsEndingTask());
                    taskAlloc.setEndingSurplusQty(task.getEndingSurplusQty());
                    taskAlloc.setIsMainProduct(task.getIsMainProduct());
                    taskAlloc.setLhId(task.getLhId());

                    result.getTaskAllocations().add(taskAlloc);
                }

                result.setUsedCapacity(usedCapacity);
                allResults.add(result);
            }

            // 输出约束量试任务分配结果，检查是否被成功分配
            if (!constrainedTrials.isEmpty()) {
                for (CoreScheduleAlgorithmService.DailyEmbryoTask ct : constrainedTrials) {
                    boolean trialAssigned = false;
                    for (CoreScheduleAlgorithmService.MachineAllocationResult mr : allResults) {
                        if (mr.getMachineCode().equals(ct.getConstrainedMachineCode())) {
                            for (CoreScheduleAlgorithmService.TaskAllocation ta : mr.getTaskAllocations()) {
                                if (ta.getEmbryoCode().equals(ct.getEmbryoCode())) {
                                    trialAssigned = true;
                                    break;
                                }
                            }
                        }
                        if (trialAssigned) break;
                    }
                    if (trialAssigned) {
                        log.info("约束量试任务 {} → 机台 {} (已分配)", ct.getEmbryoCode(), ct.getConstrainedMachineCode());
                    } else {
                        log.warn("约束量试任务 {} → 机台 {} (未分配，约束冲突)", ct.getEmbryoCode(), ct.getConstrainedMachineCode());
                    }
                }
                log.info("均衡后机台分配结果：");
                for (CoreScheduleAlgorithmService.MachineAllocationResult mr : allResults) {
                    // 改进日志：显示每个任务的物料信息，避免误导
                    List<String> taskDetails = new ArrayList<>();
                    for (CoreScheduleAlgorithmService.TaskAllocation ta : mr.getTaskAllocations()) {
                        String detail = ta.getEmbryoCode() + "[" + ta.getMaterialCode() + "]" + "(" + ta.getVulcanizeMachineCount() + ")";
                        taskDetails.add(detail);
                    }
                    log.info("  机台 {}: {}", mr.getMachineCode(), taskDetails);
                }
            }
        }

        log.info("========== 新增任务处理完成，共 {} 个机台分配 ==========", allResults.size());
        return allResults;
    }

    // ==================== 辅助方法（与 ContinueTaskProcessor 保持一致） ====================

    /**
     * 按任务优先级排序
     */
    public void sortNewTasks(List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
                             ScheduleContextVo context) {
        tasks.sort((a, b) -> {
            Integer priA = a.getPriority();
            Integer priB = b.getPriority();
            return Integer.compare(
                    priA != null ? priA : Integer.MAX_VALUE,
                    priB != null ? priB : Integer.MAX_VALUE);
        });
    }

    /**
     * 获取指定结构在当前日期可安排的机台配置（按 PRODUCTION_VERSION 过滤）
     *
     * <p>beginDay/endDay 是月内天数(1-31)，用 scheduleDate.getDayOfMonth() 过滤
     */
    private List<MpCxCapacityConfiguration> getAvailableMachinesForStructure(
            String structureName, LocalDate scheduleDate, ScheduleContextVo context,
            String productionVersion) {
        if (context.getStructureAllocationMap() != null) {
            List<MpCxCapacityConfiguration> configs =
                    context.getStructureAllocationMap().get(structureName);
            if (configs != null && !configs.isEmpty()) {
                int dayOfMonth = scheduleDate.getDayOfMonth();
                return configs.stream()
                        .filter(c -> c.getBeginDay() != null && c.getEndDay() != null)
                        .filter(c -> c.getBeginDay() <= dayOfMonth && c.getEndDay() >= dayOfMonth)
                        .filter(c -> productionVersion == null
                                || productionVersion.equals(c.getProductionVersion()))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }

    /**
     * 构建机台最大硫化机数映射
     */
    private Map<String, Integer> buildMachineMaxLhMap(
            List<MpCxCapacityConfiguration> machineConfigs,
            String structureName,
            ScheduleContextVo context) {

        Map<String, Integer> result = new HashMap<>();

        // 构建机台编码 -> 机型 映射
        Map<String, String> machineTypeMap = new HashMap<>();
        if (context.getAvailableMachines() != null) {
            for (MdmMoldingMachine machine : context.getAvailableMachines()) {
                machineTypeMap.put(machine.getCxMachineCode(), machine.getCxMachineTypeCode());
            }
        }

        // 构建 机型_结构 -> 最大硫化机数 映射
        Map<String, Integer> typeStructureMap = new HashMap<>();
        List<MdmStructureLhRatio> ratios = context.getStructureLhRatios();
        if (ratios != null) {
            for (MdmStructureLhRatio ratio : ratios) {
                String key = ratio.getCxMachineTypeCode() + "_" + ratio.getStructureName();
                if (ratio.getLhMachineMaxQty() != null) {
                    typeStructureMap.put(key, ratio.getLhMachineMaxQty());
                }
            }
        }

        for (MpCxCapacityConfiguration config : machineConfigs) {
            String machineCode = config.getCxMachineCode();
            String machineType = machineTypeMap.get(machineCode);
            String key = machineType + "_" + structureName;
            Integer maxLh = typeStructureMap.get(key);

            if (maxLh == null && context.getStructureLhRatioMap() != null) {
                MdmStructureLhRatio ratioConfig = context.getStructureLhRatioMap().get(structureName);
                if (ratioConfig != null && ratioConfig.getLhMachineMaxQty() != null) {
                    maxLh = ratioConfig.getLhMachineMaxQty();
                }
            }

            if (maxLh == null) {
                maxLh = DEFAULT_MAX_LH_MACHINE_COUNT;
            }
            result.put(machineCode, maxLh);
        }

        return result;
    }

    /**
     * 构建机台最大胎胚种类数映射
     */
    private Map<String, Integer> buildMachineMaxEmbryoTypesMap(
            List<MpCxCapacityConfiguration> machineConfigs,
            String structureName,
            ScheduleContextVo context) {

        Map<String, Integer> result = new HashMap<>();

        // 构建机台编码 -> 机型 映射
        Map<String, String> machineTypeMap = new HashMap<>();
        if (context.getAvailableMachines() != null) {
            for (MdmMoldingMachine machine : context.getAvailableMachines()) {
                machineTypeMap.put(machine.getCxMachineCode(), machine.getCxMachineTypeCode());
            }
        }

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

        // H15开头机台：如果有专用配置则优先使用
        Integer h15MaxEmbryoTypes = context.getH15MaxEmbryoTypes();

        for (MpCxCapacityConfiguration config : machineConfigs) {
            String machineCode = config.getCxMachineCode();
            String machineType = machineTypeMap.get(machineCode);

            // H15开头机台：如果有专用配置则优先使用，否则走配比逻辑
            if (h15MaxEmbryoTypes != null && machineCode != null && machineCode.startsWith("H15")) {
                log.info("  机台 {} (机型={}): 使用H15专用最大胎胚种类数={}", machineCode, machineType, h15MaxEmbryoTypes);
                result.put(machineCode, h15MaxEmbryoTypes);
                continue;
            }

            String key = machineType + "_" + structureName;
            Integer maxTypes = typeStructureMap.get(key);
            if (maxTypes == null) {
                maxTypes = context.getMaxTypesPerMachine() != null
                        ? context.getMaxTypesPerMachine() : BalancingService.DEFAULT_MAX_TYPES_PER_MACHINE;
            }
            log.info("  机台 {} (机型={}): 最大胎胚种类数={}", machineCode, machineType, maxTypes);
            result.put(machineCode, maxTypes);
        }

        return result;
    }

    /**
     * 获取是否强制保留历史任务配置
     */
    private boolean getForceKeepHistoryConfig(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get("FORCE_KEEP_HISTORY_TASK");
            if (config != null && config.getParamValue() != null) {
                return "1".equals(config.getParamValue()) || "true".equalsIgnoreCase(config.getParamValue());
            }
        }
        return false;
    }

    /**
     * 构建试制机台映射：materialCode → machineCode
     * 仅返回指定结构下的试制任务
     */
    private Map<String, String> buildTrialMachineMap(
            List<CoreScheduleAlgorithmService.MachineAllocationResult> trialAllocations,
            String structureName) {
        Map<String, String> trialMachineMap = new HashMap<>();
        if (trialAllocations == null) {
            return trialMachineMap;
        }
        for (CoreScheduleAlgorithmService.MachineAllocationResult allocation : trialAllocations) {
            String machineCode = allocation.getMachineCode();
            for (CoreScheduleAlgorithmService.TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                if (structureName.equals(taskAlloc.getStructureName())
                        && taskAlloc.getEmbryoCode() != null
                        && Boolean.TRUE.equals(taskAlloc.getIsTrialTask())) {
                    trialMachineMap.put(taskAlloc.getEmbryoCode(), machineCode);
                }
            }
        }
        return trialMachineMap;
    }

    /**
     * 判断量试任务是否受试制约束
     * 条件：是量试任务 + 同胎胚有试制任务
     */
    private boolean isVolumeTrialConstrained(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            Map<String, String> trialMachineMap) {
        if (!Boolean.TRUE.equals(task.getIsProductionTrial())) {
            return false;
        }
        return trialMachineMap.containsKey(task.getEmbryoCode());
    }
}
