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
     * @param day               排程日（day of month）
     * @param existAllocations  续作任务分配结果
     * @param trialAllocations 试制任务分配结果（用于量试约束）
     * @return 均衡分配结果
     */
    public List<CoreScheduleAlgorithmService.MachineAllocationResult> processNewTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> newTasks,
            ScheduleContextVo context,
            LocalDate scheduleDate,
            List<CxShiftConfig> dayShifts,
            int day,
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> continueTasks,
            List<CoreScheduleAlgorithmService.MachineAllocationResult> existAllocations,
            List<CoreScheduleAlgorithmService.MachineAllocationResult> trialAllocations) {

        List<CoreScheduleAlgorithmService.MachineAllocationResult> allResults = new ArrayList<>();

        if (CollectionUtils.isEmpty(newTasks)) {
            return allResults;
        }

        log.info("========== 开始处理新增任务，共 {} 个任务 ==========", newTasks.size());

        // Step 1: 按结构分组新增任务
        Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> structureTaskMap = newTasks.stream()
                .filter(t -> t.getStructureName() != null)
                .collect(Collectors.groupingBy(
                        CoreScheduleAlgorithmService.DailyEmbryoTask::getStructureName,
                        LinkedHashMap::new,
                        Collectors.toList()));

        // Step 2: 获取是否强制保留历史任务
        boolean forceKeepHistory = getForceKeepHistoryConfig(context);

        // Step 3: 按结构处理
        for (Map.Entry<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> entry : structureTaskMap.entrySet()) {
            String structureName = entry.getKey();
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> newTasksForStructure = entry.getValue();

            log.info("--- 处理结构 {}，共 {} 个新增任务 ---", structureName, newTasksForStructure.size());

            // Step 3.1: 获取该结构可安排的机台
            List<MpCxCapacityConfiguration> availableMachines =
                    getAvailableMachinesForStructure(structureName, day, context);
            if (availableMachines.isEmpty()) {
                log.warn("结构 {} 没有可安排的机台，跳过", structureName);
                continue;
            }

            // Step 3.2: 获取该结构的续作任务，构建 machineHistoryMap
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> continueTasksForStructure = new ArrayList<>();
            Map<String, Set<String>> machineHistoryMap = new HashMap<>();

            if (continueTasks != null) {
                for (CoreScheduleAlgorithmService.DailyEmbryoTask task : continueTasks) {
                    if (structureName.equals(task.getStructureName())) {
                        continueTasksForStructure.add(task);
                    }
                }
            }

            // 从 existAllocations（续作第一轮均衡结果）构建 machineHistoryMap
            if (existAllocations != null) {
                for (CoreScheduleAlgorithmService.MachineAllocationResult allocation : existAllocations) {
                    String machineCode = allocation.getMachineCode();
                    Set<String> embryos = new HashSet<>();
                    for (CoreScheduleAlgorithmService.TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                        if (structureName.equals(taskAlloc.getStructureName())) {
                            embryos.add(taskAlloc.getEmbryoCode());
                        }
                    }
                    if (!embryos.isEmpty()) {
                        machineHistoryMap.put(machineCode, embryos);
                    }
                }
            }

            // Step 3.3: 构建试制机台映射 materialCode → machineCode（同结构下）
            Map<String, String> trialMachineMap = buildTrialMachineMap(trialAllocations, structureName);

            // Step 3.4: 分类新增任务 - 固定量试 vs 参与均衡
            // 固定量试：量试任务 + 同胎胚有试制任务 → 固定到试制机台
            // 参与均衡：其余新增任务（含无量试约束的量试任务）
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> fixedVolumeTrials = new ArrayList<>();
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> balancedTasks = new ArrayList<>();

            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : newTasksForStructure) {
                task.setIsContinueTask(false);
                if (isVolumeTrialConstrained(task, trialMachineMap)) {
                    fixedVolumeTrials.add(task);
                } else {
                    balancedTasks.add(task);
                }
            }

            // 固定量试预占机台：加入 machineHistoryMap
            for (CoreScheduleAlgorithmService.DailyEmbryoTask fixedTask : fixedVolumeTrials) {
                String targetMachine = trialMachineMap.get(fixedTask.getEmbryoCode());
                machineHistoryMap.computeIfAbsent(targetMachine, k -> new HashSet<>())
                        .add(fixedTask.getEmbryoCode());
            }

            // Step 3.5: 合并续作任务和参与均衡的新增任务
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> allTasksForStructure = new ArrayList<>();
            allTasksForStructure.addAll(continueTasksForStructure);
            allTasksForStructure.addAll(balancedTasks);

            log.info("结构 {} 合并后：续作={}, 均衡新增={}, 固定量试={}",
                    structureName, continueTasksForStructure.size(),
                    balancedTasks.size(), fixedVolumeTrials.size());

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
                            forceKeepHistory,
                            context);

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
                for (BalancingService.EmbryoAssignment embryoAssignment
                        : assignment.getEmbryoAssignments()) {
                    CoreScheduleAlgorithmService.DailyEmbryoTask task =
                            embryoAssignment.getTask();
                    int assignedQty = embryoAssignment.getAssignedQty();
                    usedCapacity += assignedQty;

                    CoreScheduleAlgorithmService.TaskAllocation taskAlloc =
                            new CoreScheduleAlgorithmService.TaskAllocation();
                    taskAlloc.setEmbryoCode(task.getEmbryoCode());
                    taskAlloc.setMaterialCode(task.getMaterialCode());
                    taskAlloc.setMaterialDesc(task.getMaterialDesc());
                    taskAlloc.setMainMaterialDesc(task.getMainMaterialDesc());
                    taskAlloc.setStructureName(task.getStructureName());
                    taskAlloc.setQuantity(task.getPlannedProduction() != null ? task.getPlannedProduction() : 0);
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

            // Step 3.10: 固定量试任务追加到对应机台结果
            for (CoreScheduleAlgorithmService.DailyEmbryoTask fixedTask : fixedVolumeTrials) {
                String targetMachine = trialMachineMap.get(fixedTask.getEmbryoCode());
                // 找到对应机台的结果
                CoreScheduleAlgorithmService.MachineAllocationResult targetResult = null;
                for (CoreScheduleAlgorithmService.MachineAllocationResult r : allResults) {
                    if (r.getMachineCode().equals(targetMachine)) {
                        targetResult = r;
                        break;
                    }
                }
                // 如果没找到，新建一个
                if (targetResult == null) {
                    targetResult = new CoreScheduleAlgorithmService.MachineAllocationResult();
                    targetResult.setMachineCode(targetMachine);
                    targetResult.setTaskAllocations(new ArrayList<>());
                    targetResult.setUsedCapacity(0);
                    allResults.add(targetResult);
                }

                CoreScheduleAlgorithmService.TaskAllocation taskAlloc =
                        new CoreScheduleAlgorithmService.TaskAllocation();
                taskAlloc.setEmbryoCode(fixedTask.getEmbryoCode());
                taskAlloc.setMaterialCode(fixedTask.getMaterialCode());
                taskAlloc.setMaterialDesc(fixedTask.getMaterialDesc());
                taskAlloc.setMainMaterialDesc(fixedTask.getMainMaterialDesc());
                taskAlloc.setStructureName(fixedTask.getStructureName());
                taskAlloc.setQuantity(fixedTask.getPlannedProduction() != null ? fixedTask.getPlannedProduction() : 0);
                taskAlloc.setPriority(fixedTask.getPriority());
                taskAlloc.setStockHours(fixedTask.getStockHours());
                taskAlloc.setIsTrialTask(fixedTask.getIsTrialTask());
                taskAlloc.setIsContinueTask(false);
                taskAlloc.setIsEndingTask(fixedTask.getIsEndingTask());
                taskAlloc.setEndingSurplusQty(fixedTask.getEndingSurplusQty());
                taskAlloc.setIsMainProduct(fixedTask.getIsMainProduct());
                taskAlloc.setLhId(fixedTask.getLhId());

                targetResult.getTaskAllocations().add(taskAlloc);
                log.info("固定量试任务 {} → 机台 {}", fixedTask.getEmbryoCode(), targetMachine);
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
     * 获取指定结构在当前日期可安排的机台配置
     */
    private List<MpCxCapacityConfiguration> getAvailableMachinesForStructure(
            String structureName, int day, ScheduleContextVo context) {
        if (context.getStructureAllocationMap() != null) {
            List<MpCxCapacityConfiguration> configs =
                    context.getStructureAllocationMap().get(structureName);
            if (configs != null && !configs.isEmpty()) {
                return configs.stream()
                        .filter(c -> c.getBeginDay() != null && c.getEndDay() != null)
                        .filter(c -> c.getBeginDay() <= day && c.getEndDay() >= day)
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

        for (MpCxCapacityConfiguration config : machineConfigs) {
            String machineCode = config.getCxMachineCode();
            String machineType = machineTypeMap.get(machineCode);
            String key = machineType + "_" + structureName;
            Integer maxTypes = typeStructureMap.get(key);
            if (maxTypes == null) {
                maxTypes = context.getMaxTypesPerMachine() != null
                        ? context.getMaxTypesPerMachine() : BalancingService.DEFAULT_MAX_TYPES_PER_MACHINE;
            }
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
