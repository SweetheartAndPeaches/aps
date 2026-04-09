package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmCxMachineFixed;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 试制任务处理器
 *
 * <p>负责试制任务的机台分配，仅处理试制任务（量试由其他模块处理）。
 *
 * <p>处理流程：
 * <ol>
 *   <li>按结构分组任务</li>
 *   <li>每个结构内按任务优先级（priority）排序</li>
 *   <li>为空机台优先，其次选择最不均衡的机台</li>
 *   <li>直接分配到机台，无整车换算、无胎胚库存分配、无收尾处理</li>
 * </ol>
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrialTaskProcessor {

    private static final int DEFAULT_MAX_TYPES_PER_MACHINE = 4;

    /**
     * 处理试制任务
     *
     * @param trialTasks       试制任务列表
     * @param context         排程上下文
     * @param scheduleDate    排程日期
     * @param dayShifts       当天班次配置
     * @param availableMachines 可用机台列表
     * @return 机台分配结果列表
     */
    public List<CoreScheduleAlgorithmService.MachineAllocationResult> processTrialTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> trialTasks,
            ScheduleContextVo context,
            LocalDate scheduleDate,
            List<CxShiftConfig> dayShifts,
            List<MdmMoldingMachine> availableMachines) {

        List<CoreScheduleAlgorithmService.MachineAllocationResult> results = new ArrayList<>();

        if (CollectionUtils.isEmpty(trialTasks)) {
            return results;
        }

        log.info("========== 开始处理试制任务，共 {} 个任务 ==========", trialTasks.size());

        // Step 1: 按结构分组
        Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> structureTaskMap =
                trialTasks.stream()
                        .filter(t -> t.getStructureName() != null)
                        .collect(Collectors.groupingBy(
                                CoreScheduleAlgorithmService.DailyEmbryoTask::getStructureName,
                                LinkedHashMap::new,
                                Collectors.toList()));

        // Step 2: 记录已分配的机台任务映射（用于计算负载差异）
        Map<String, CoreScheduleAlgorithmService.MachineAllocationResult> machineAllocationMap =
                new HashMap<>();

        // Step 3: 按结构处理
        for (Map.Entry<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> entry : structureTaskMap.entrySet()) {
            String structureName = entry.getKey();
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks = entry.getValue();

            log.info("--- 处理结构 {}，共 {} 个试制胎胚 ---", structureName, tasks.size());

            // Step 3.1: 获取该结构可安排的机台
            List<MpCxCapacityConfiguration> structMachines = getAvailableMachinesForStructure(
                    structureName, scheduleDate, context);
            if (structMachines.isEmpty()) {
                log.warn("结构 {} 没有可安排的机台，跳过", structureName);
                continue;
            }

            // Step 3.2: 按任务优先级排序
            tasks.sort((a, b) -> {
                Integer priA = a.getPriority();
                Integer priB = b.getPriority();
                return Integer.compare(
                        priA != null ? priA : Integer.MAX_VALUE,
                        priB != null ? priB : Integer.MAX_VALUE);
            });

            // Step 3.3: 逐个分配到机台
            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
                allocateTrialTask(task, structMachines, machineAllocationMap, context);
            }
        }

        results.addAll(machineAllocationMap.values());
        log.info("========== 试制任务处理完成，共 {} 台机台分配任务 ==========", results.size());
        return results;
    }

    /**
     * 分配单个试制任务到机台
     *
     * <p>机台选择策略：
     * <ol>
     *   <li>空机台优先（没有任何任务分配）</li>
     *   <li>其次选择最不均衡的机台（负载差异最大）</li>
     * </ol>
     */
    private void allocateTrialTask(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            List<MpCxCapacityConfiguration> structMachines,
            Map<String, CoreScheduleAlgorithmService.MachineAllocationResult> machineAllocationMap,
            ScheduleContextVo context) {

        String structureName = task.getStructureName();
        String materialCode = task.getMaterialCode();

        MdmMoldingMachine selectedMachine = selectMachineForTrial(
                materialCode, structureName, structMachines, machineAllocationMap, context);
        if (selectedMachine == null) {
            log.warn("试制任务 {} 无法找到合适的机台，跳过", materialCode);
            return;
        }

        String machineCode = selectedMachine.getCxMachineCode();

        // 获取或创建机台分配结果
        CoreScheduleAlgorithmService.MachineAllocationResult allocation =
                machineAllocationMap.computeIfAbsent(machineCode, k -> createMachineAllocation(k, context));

        // 设置试制任务计划量（直接取需求，不整车换算）
        int demandQty = task.getDemandQuantity() != null ? task.getDemandQuantity() : 0;
        task.setPlannedProduction(demandQty);
        task.setIsTrialTask(true);

        // 分配到机台
        allocateTaskToMachine(allocation, task);
        log.debug("试制任务 {} 分配到机台 {}，计划量={}", materialCode, machineCode, demandQty);
    }

    /**
     * 为试制任务选择机台
     *
     * <p>选择顺序：
     * <ol>
     *   <li>空机台优先（无任何任务）</li>
     *   <li>最不均衡的机台（负载差异最大）</li>
     * </ol>
     */
    private MdmMoldingMachine selectMachineForTrial(
            String materialCode,
            String structureName,
            List<MpCxCapacityConfiguration> structMachines,
            Map<String, CoreScheduleAlgorithmService.MachineAllocationResult> machineAllocationMap,
            ScheduleContextVo context) {

        MdmMoldingMachine emptyMachine = null;
        MdmMoldingMachine bestImbalancedMachine = null;
        int maxImbalance = -1;

        for (MpCxCapacityConfiguration config : structMachines) {
            String machineCode = config.getCxMachineCode();
            MdmMoldingMachine machine = findMachine(machineCode, context.getAvailableMachines());
            if (machine == null) {
                continue;
            }

            // 检查结构约束
            if (!checkStructureConstraint(machine, structureName, context)) {
                continue;
            }

            CoreScheduleAlgorithmService.MachineAllocationResult allocation = machineAllocationMap.get(machineCode);

            if (allocation == null || allocation.getTaskAllocations().isEmpty()) {
                // 空机台优先
                if (emptyMachine == null) {
                    emptyMachine = machine;
                }
            } else {
                // 计算负载差异（越不均衡越好）
                int imbalance = calculateImbalance(machineCode, machineAllocationMap);
                if (imbalance > maxImbalance) {
                    maxImbalance = imbalance;
                    bestImbalancedMachine = machine;
                }
            }
        }

        // 空机台优先，其次最不均衡
        if (emptyMachine != null) {
            return emptyMachine;
        }
        return bestImbalancedMachine;
    }

    /**
     * 计算指定机台的负载不均衡度
     *
     * <p>不均衡度 = |usedCapacity - avgUsedCapacity|
     * 即当前机台与所有非空机台平均负载的偏差，偏差越大越不均衡。
     */
    private int calculateImbalance(
            String machineCode,
            Map<String, CoreScheduleAlgorithmService.MachineAllocationResult> machineAllocationMap) {
        // 计算所有非空机台的平均负载
        int totalUsed = 0;
        int count = 0;
        for (CoreScheduleAlgorithmService.MachineAllocationResult alloc : machineAllocationMap.values()) {
            if (!alloc.getTaskAllocations().isEmpty()) {
                totalUsed += alloc.getUsedCapacity();
                count++;
            }
        }
        if (count == 0) {
            return 0;
        }
        int avgUsed = totalUsed / count;
        CoreScheduleAlgorithmService.MachineAllocationResult current = machineAllocationMap.get(machineCode);
        int currentUsed = current != null ? current.getUsedCapacity() : 0;
        return Math.abs(currentUsed - avgUsed);
    }

    /**
     * 从机台列表中查找指定编码的机台
     */
    private MdmMoldingMachine findMachine(String machineCode, List<MdmMoldingMachine> machines) {
        if (machines == null) {
            return null;
        }
        for (MdmMoldingMachine m : machines) {
            if (m.getCxMachineCode().equals(machineCode)) {
                return m;
            }
        }
        return null;
    }

    /**
     * 获取指定结构在当前日期可安排的机台配置
     */
    private List<MpCxCapacityConfiguration> getAvailableMachinesForStructure(
            String structureName, LocalDate scheduleDate, ScheduleContextVo context) {
        if (context.getStructureAllocationMap() != null) {
            List<MpCxCapacityConfiguration> configs =
                    context.getStructureAllocationMap().get(structureName);
            if (configs != null && !configs.isEmpty()) {
                int day = scheduleDate.getDayOfMonth();
                return configs.stream()
                        .filter(c -> c.getBeginDay() != null && c.getEndDay() != null)
                        .filter(c -> c.getBeginDay() <= day && c.getEndDay() >= day)
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }

    /**
     * 检查机台是否禁止生产指定结构
     */
    private boolean checkStructureConstraint(
            MdmMoldingMachine machine, String structureName, ScheduleContextVo context) {
        if (structureName == null || context.getMachineFixedConfigs() == null) {
            return true;
        }
        for (MdmCxMachineFixed fixed : context.getMachineFixedConfigs()) {
            if (fixed.getCxMachineCode().equals(machine.getCxMachineCode())) {
                if (fixed.getDisableStructure() != null &&
                        fixed.getDisableStructure().contains(structureName)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 创建机台分配结果
     */
    private CoreScheduleAlgorithmService.MachineAllocationResult createMachineAllocation(
            String machineCode, ScheduleContextVo context) {
        CoreScheduleAlgorithmService.MachineAllocationResult allocation =
                new CoreScheduleAlgorithmService.MachineAllocationResult();
        allocation.setMachineCode(machineCode);
        allocation.setTaskAllocations(new ArrayList<>());
        allocation.setUsedCapacity(0);
        allocation.setRemainingCapacity(getMachineDailyCapacity(machineCode, context));
        return allocation;
    }

    /**
     * 获取机台日产能
     */
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

    /**
     * 分配任务到机台
     */
    private void allocateTaskToMachine(
            CoreScheduleAlgorithmService.MachineAllocationResult allocation,
            CoreScheduleAlgorithmService.DailyEmbryoTask task) {

        CoreScheduleAlgorithmService.TaskAllocation taskAllocation =
                new CoreScheduleAlgorithmService.TaskAllocation();
        taskAllocation.setEmbryoCode(task.getMaterialCode());
        taskAllocation.setSapCode(task.getRelatedMaterialCode());
        taskAllocation.setMaterialName(task.getMaterialName());
        taskAllocation.setStructureName(task.getStructureName());
        taskAllocation.setQuantity(task.getPlannedProduction());
        taskAllocation.setPriority(task.getPriority());
        taskAllocation.setStockHours(task.getStockHours());
        taskAllocation.setIsTrialTask(task.getIsTrialTask());
        taskAllocation.setIsEndingTask(task.getIsEndingTask());
        taskAllocation.setEndingSurplusQty(task.getEndingSurplusQty());
        taskAllocation.setIsMainProduct(task.getIsMainProduct());
        taskAllocation.setLhId(task.getLhId());

        allocation.getTaskAllocations().add(taskAllocation);
        allocation.setUsedCapacity(allocation.getUsedCapacity() + task.getPlannedProduction());
        allocation.setRemainingCapacity(allocation.getRemainingCapacity() - task.getPlannedProduction());
    }
}
