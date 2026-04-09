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

    private static final int DEFAULT_TRIP_CAPACITY = 60;

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
     * @param trialAllocations 试制任务分配结果（暂不使用）
     * @return 均衡分配结果
     */
    public List<CoreScheduleAlgorithmService.MachineAllocationResult> processNewTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> newTasks,
            ScheduleContextVo context,
            LocalDate scheduleDate,
            List<CxShiftConfig> dayShifts,
            int day,
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> continueTasks,
            List<CoreScheduleAlgorithmService.MachineAllocationResult> existAllocations) {

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
                            embryos.add(taskAlloc.getMaterialCode());
                        }
                    }
                    if (!embryos.isEmpty()) {
                        machineHistoryMap.put(machineCode, embryos);
                    }
                }
            }

            // Step 3.3: 合并续作任务和新增任务
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> allTasksForStructure = new ArrayList<>();
            allTasksForStructure.addAll(continueTasksForStructure);

            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : newTasksForStructure) {
                task.setIsContinueTask(false);
                allTasksForStructure.add(task);
            }

            log.info("结构 {} 合并后：续作={}, 新增={}",
                    structureName, continueTasksForStructure.size(), newTasksForStructure.size());

            // Step 3.4: 构建机台最大硫化机数映射
            Map<String, Integer> machineMaxLhMap = buildMachineMaxLhMap(
                    availableMachines, structureName, context);

            // Step 3.5: 构建机台最大胎胚种类数映射
            Map<String, Integer> machineMaxEmbryoTypesMap = buildMachineMaxEmbryoTypesMap(
                    availableMachines, structureName, context);

            // Step 3.6: 均衡分配
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

            // Step 3.7: 构建分配结果
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
                    taskAlloc.setMaterialCode(task.getMaterialCode());
                    taskAlloc.setMaterialName(task.getMaterialName());
                    taskAlloc.setStructureName(task.getStructureName());
                    taskAlloc.setQuantity(task.getPlannedProduction() != null ? task.getPlannedProduction() : 0);
                    taskAlloc.setPriority(task.getPriority());
                    taskAlloc.setStockHours(task.getStockHours());
                    taskAlloc.setIsTrialTask(task.getIsTrialTask());
                    taskAlloc.setIsContinueTask(task.getIsContinueTask());
                    taskAlloc.setIsEndingTask(task.getIsEndingTask());
                    taskAlloc.setEndingSurplusQty(task.getEndingSurplusQty());
                    taskAlloc.setIsMainProduct(task.getIsMainProduct());

                    result.getTaskAllocations().add(taskAlloc);
                }

                result.setUsedCapacity(usedCapacity);
                allResults.add(result);
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
                maxLh = 10;
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
                        ? context.getMaxTypesPerMachine() : 4;
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
}
