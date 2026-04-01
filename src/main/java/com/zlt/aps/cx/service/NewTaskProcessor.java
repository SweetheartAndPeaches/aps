package com.zlt.aps.cx.service;

import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.config.CxStructurePriority;
import com.zlt.aps.cx.entity.config.CxStructureShiftCapacity;
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
 *   <li>机台选择：均衡分配（与续作任务类似）</li>
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

    /** 默认整车容量 */
    private static final int DEFAULT_TRIP_CAPACITY = 12;
    
    /** 默认机台种类上限 */
    private static final int DEFAULT_MAX_TYPES_PER_MACHINE = 4;
    
    /** 种类数允许差额（默认1） */
    private static final int DEFAULT_TYPE_DIFF_THRESHOLD = 1;
    
    /** 负荷允许差额（默认3硫化机台数） */
    private static final int DEFAULT_LOAD_DIFF_THRESHOLD = 3;

    /**
     * 处理普通新增任务
     *
     * <p>S5.3 新增任务排产流程：
     * <ol>
     *   <li>按排序规则对任务排序</li>
     *   <li>按结构分组</li>
     *   <li>合并续作分配结果 + 新增任务</li>
     *   <li>使用均衡算法重新分配</li>
     * </ol>
     *
     * <p>排序规则：
     * <ol>
     *   <li>按照月计划优先级排序</li>
     *   <li>收尾任务优先</li>
     *   <li>紧急收尾优先</li>
     *   <li>新胎胚的优先级高于普通的新增胎胚</li>
     * </ol>
     *
     * @param newTasks          新增任务列表
     * @param context           排程上下文
     * @param scheduleDate      排程日期
     * @param dayShifts         班次配置
     * @param day               排程天数
     * @param existAllocations  已有的分配结果（续作任务）
     * @return 机台分配结果列表
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

        // 判断是否停产日
        boolean isClosingDay = Boolean.TRUE.equals(context.getIsClosingDay());
        if (isClosingDay) {
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

        // Step 4: 合并续作分配结果 + 新增任务，重新均衡
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
     * 合并续作分配结果 + 新增任务，重新均衡
     *
     * <p>方案A：合并后重新均衡
     * <ol>
     *   <li>提取续作分配的胎胚信息</li>
     *   <li>合并新增任务</li>
     *   <li>使用DFS算法重新均衡分配</li>
     *   <li>保留续作任务的核心约束（机台不变）</li>
     * </ol>
     */
    private List<CoreScheduleAlgorithmService.MachineAllocationResult> rebalanceWithNewTasks(
            List<CoreScheduleAlgorithmService.MachineAllocationResult> existAllocations,
            Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> structureTaskMap,
            List<MdmMoldingMachine> availableMachines,
            ScheduleContextDTO context) {

        log.info("----- 合并续作+新增任务，重新均衡 -----");

        // 获取机台配置参数
        int maxTypes = context.getMaxTypesPerMachine() != null
                ? context.getMaxTypesPerMachine()
                : DEFAULT_MAX_TYPES_PER_MACHINE;
        int typeDiffThreshold = context.getTypeDiffThreshold() != null
                ? context.getTypeDiffThreshold()
                : DEFAULT_TYPE_DIFF_THRESHOLD;
        int loadDiffThreshold = context.getLoadDiffThreshold() != null
                ? context.getLoadDiffThreshold()
                : DEFAULT_LOAD_DIFF_THRESHOLD;

        // Step 1: 提取续作分配信息
        Map<String, Set<String>> machineEmbryoMap = new HashMap<>(); // 机台 -> 胎胚集合
        Map<String, Integer> machineLoadMap = new HashMap<>(); // 机台 -> 硫化机台数

        for (CoreScheduleAlgorithmService.MachineAllocationResult allocation : existAllocations) {
            String machineCode = allocation.getMachineCode();
            Set<String> embryos = new HashSet<>();
            int totalLoad = 0;

            for (CoreScheduleAlgorithmService.TaskAllocation task : allocation.getTaskAllocations()) {
                embryos.add(task.getMaterialCode());
                // 计算硫化机台数（整车容量=12）
                int tripCapacity = DEFAULT_TRIP_CAPACITY;
                int load = (int) Math.ceil((double) task.getQuantity() / tripCapacity);
                totalLoad += load;
            }

            machineEmbryoMap.put(machineCode, embryos);
            machineLoadMap.put(machineCode, totalLoad);
        }

        // Step 2: 收集所有待分配的胎胚（续作 + 新增）
        Map<String, CoreScheduleAlgorithmService.DailyEmbryoTask> allEmbryoTaskMap = new LinkedHashMap<>();

        // 添加续作胎胚
        for (CoreScheduleAlgorithmService.MachineAllocationResult allocation : existAllocations) {
            for (CoreScheduleAlgorithmService.TaskAllocation task : allocation.getTaskAllocations()) {
                if (!allEmbryoTaskMap.containsKey(task.getMaterialCode())) {
                    CoreScheduleAlgorithmService.DailyEmbryoTask embryoTask = new CoreScheduleAlgorithmService.DailyEmbryoTask();
                    embryoTask.setMaterialCode(task.getMaterialCode());
                    embryoTask.setMaterialName(task.getMaterialName());
                    embryoTask.setStructureName(task.getStructureName());
                    embryoTask.setIsContinueTask(true); // 标记为续作
                    embryoTask.setIsEndingTask(task.getIsEndingTask());
                    embryoTask.setIsMainProduct(task.getIsMainProduct());
                    allEmbryoTaskMap.put(task.getMaterialCode(), embryoTask);
                }
            }
        }

        // 添加新增胎胚
        for (Map.Entry<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> entry : structureTaskMap.entrySet()) {
            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : entry.getValue()) {
                if (!allEmbryoTaskMap.containsKey(task.getMaterialCode())) {
                    task.setIsContinueTask(false); // 标记为新增
                    allEmbryoTaskMap.put(task.getMaterialCode(), task);
                }
            }
        }

        // Step 3: 按结构分组所有胎胚
        Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> allStructureTaskMap = allEmbryoTaskMap.values().stream()
                .filter(t -> t.getStructureName() != null)
                .collect(Collectors.groupingBy(
                        CoreScheduleAlgorithmService.DailyEmbryoTask::getStructureName,
                        LinkedHashMap::new,
                        Collectors.toList()));

        // Step 4: 对每个结构进行均衡分配
        List<CoreScheduleAlgorithmService.MachineAllocationResult> results = new ArrayList<>();
        
        for (Map.Entry<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> entry : allStructureTaskMap.entrySet()) {
            String structureName = entry.getKey();
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks = entry.getValue();

            // 获取该结构可用的机台
            List<MdmMoldingMachine> structureMachines = getMachinesForStructure(structureName, availableMachines, context);
            if (structureMachines.isEmpty()) {
                log.warn("结构 {} 没有可用机台", structureName);
                continue;
            }

            // 分离续作胎胚和新增胎胚
            List<String> continueEmbryos = tasks.stream()
                    .filter(t -> Boolean.TRUE.equals(t.getIsContinueTask()))
                    .map(CoreScheduleAlgorithmService.DailyEmbryoTask::getMaterialCode)
                    .collect(Collectors.toList());

            List<String> newEmbryos = tasks.stream()
                    .filter(t -> !Boolean.TRUE.equals(t.getIsContinueTask()))
                    .map(CoreScheduleAlgorithmService.DailyEmbryoTask::getMaterialCode)
                    .collect(Collectors.toList());

            // 计算硫化机台数（需求）
            Map<String, Integer> embryoLoadMap = new HashMap<>();
            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
                int demand = task.getPlannedProduction() != null && task.getPlannedProduction() > 0
                        ? task.getPlannedProduction() : task.getDemandQuantity();
                int load = (int) Math.ceil((double) demand / DEFAULT_TRIP_CAPACITY);
                embryoLoadMap.put(task.getMaterialCode(), load);
            }

            // 使用DFS算法均衡分配
            Map<String, Set<String>> allocation = balanceEmbryosToMachines(
                    continueEmbryos, newEmbryos, embryoLoadMap,
                    structureMachines, maxTypes, typeDiffThreshold, loadDiffThreshold, context);

            // 构建分配结果
            for (Map.Entry<String, Set<String>> allocEntry : allocation.entrySet()) {
                String machineCode = allocEntry.getKey();
                Set<String> embryoCodes = allocEntry.getValue();

                CoreScheduleAlgorithmService.MachineAllocationResult machineResult = createMachineAllocation(machineCode, context);

                for (String embryoCode : embryoCodes) {
                    CoreScheduleAlgorithmService.DailyEmbryoTask task = allEmbryoTaskMap.get(embryoCode);
                    if (task != null) {
                        allocateTaskToMachine(machineResult, task, context);
                    }
                }

                if (!machineResult.getTaskAllocations().isEmpty()) {
                    results.add(machineResult);
                }
            }
        }

        return results;
    }

    /**
     * 仅新增任务均衡分配（无续作）
     */
    private List<CoreScheduleAlgorithmService.MachineAllocationResult> balanceNewTasks(
            Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> structureTaskMap,
            List<MdmMoldingMachine> availableMachines,
            ScheduleContextDTO context) {

        log.info("----- 仅新增任务均衡分配 -----");

        int maxTypes = context.getMaxTypesPerMachine() != null
                ? context.getMaxTypesPerMachine()
                : DEFAULT_MAX_TYPES_PER_MACHINE;
        int typeDiffThreshold = context.getTypeDiffThreshold() != null
                ? context.getTypeDiffThreshold()
                : DEFAULT_TYPE_DIFF_THRESHOLD;
        int loadDiffThreshold = context.getLoadDiffThreshold() != null
                ? context.getLoadDiffThreshold()
                : DEFAULT_LOAD_DIFF_THRESHOLD;

        List<CoreScheduleAlgorithmService.MachineAllocationResult> results = new ArrayList<>();

        for (Map.Entry<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> entry : structureTaskMap.entrySet()) {
            String structureName = entry.getKey();
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks = entry.getValue();

            List<MdmMoldingMachine> structureMachines = getMachinesForStructure(structureName, availableMachines, context);
            if (structureMachines.isEmpty()) {
                log.warn("结构 {} 没有可用机台", structureName);
                continue;
            }

            List<String> embryoCodes = tasks.stream()
                    .map(CoreScheduleAlgorithmService.DailyEmbryoTask::getMaterialCode)
                    .collect(Collectors.toList());

            Map<String, Integer> embryoLoadMap = new HashMap<>();
            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
                int demand = task.getPlannedProduction() != null && task.getPlannedProduction() > 0
                        ? task.getPlannedProduction() : task.getDemandQuantity();
                int load = (int) Math.ceil((double) demand / DEFAULT_TRIP_CAPACITY);
                embryoLoadMap.put(task.getMaterialCode(), load);
            }

            Map<String, Set<String>> allocation = balanceEmbryosToMachines(
                    Collections.emptyList(), embryoCodes, embryoLoadMap,
                    structureMachines, maxTypes, typeDiffThreshold, loadDiffThreshold, context);

            Map<String, CoreScheduleAlgorithmService.DailyEmbryoTask> taskMap = tasks.stream()
                    .collect(Collectors.toMap(
                            CoreScheduleAlgorithmService.DailyEmbryoTask::getMaterialCode,
                            t -> t,
                            (a, b) -> a));

            for (Map.Entry<String, Set<String>> allocEntry : allocation.entrySet()) {
                String machineCode = allocEntry.getKey();
                Set<String> embryos = allocEntry.getValue();

                CoreScheduleAlgorithmService.MachineAllocationResult machineResult = createMachineAllocation(machineCode, context);

                for (String embryoCode : embryos) {
                    CoreScheduleAlgorithmService.DailyEmbryoTask task = taskMap.get(embryoCode);
                    if (task != null) {
                        allocateTaskToMachine(machineResult, task, context);
                    }
                }

                if (!machineResult.getTaskAllocations().isEmpty()) {
                    results.add(machineResult);
                }
            }
        }

        return results;
    }

    /**
     * 均衡分配胎胚到机台
     *
     * <p>使用 DFS + 剪枝算法，目标：
     * <ul>
     *   <li>胎胚种类数均衡</li>
     *   <li>硫化机台数均衡</li>
     *   <li>续作胎胚尽量保持原机台</li>
     * </ul>
     *
     * @param continueEmbryos    续作胎胚列表
     * @param newEmbryos         新增胎胚列表
     * @param embryoLoadMap      胎胚 -> 硫化机台数
     * @param machines           可用机台列表
     * @param maxTypes           机台种类上限
     * @param typeDiffThreshold  种类数允许差额
     * @param loadDiffThreshold  负荷允许差额
     * @param context            排程上下文
     * @return 机台 -> 胎胚集合
     */
    private Map<String, Set<String>> balanceEmbryosToMachines(
            List<String> continueEmbryos,
            List<String> newEmbryos,
            Map<String, Integer> embryoLoadMap,
            List<MdmMoldingMachine> machines,
            int maxTypes,
            int typeDiffThreshold,
            int loadDiffThreshold,
            ScheduleContextDTO context) {

        Map<String, Set<String>> bestAllocation = new HashMap<>();
        int[] bestScore = {Integer.MAX_VALUE};

        // 初始化机台状态
        Map<String, Set<String>> currentAllocation = new HashMap<>();
        Map<String, Integer> currentLoad = new HashMap<>();
        for (MdmMoldingMachine machine : machines) {
            currentAllocation.put(machine.getCxMachineCode(), new HashSet<>());
            currentLoad.put(machine.getCxMachineCode(), 0);
        }

        // 合并所有胎胚，续作在前
        List<String> allEmbryos = new ArrayList<>();
        allEmbryos.addAll(continueEmbryos);
        allEmbryos.addAll(newEmbryos);

        // 记录续作胎胚的推荐机台（尽量不换机台）
        Map<String, String> continueRecommendMachine = getContinueRecommendMachines(continueEmbryos, context);

        // DFS分配
        dfsAssign(allEmbryos, 0, embryoLoadMap, machines, currentAllocation, currentLoad,
                maxTypes, typeDiffThreshold, loadDiffThreshold,
                continueRecommendMachine, bestAllocation, bestScore);

        return bestAllocation;
    }

    /**
     * DFS + 剪枝分配
     */
    private void dfsAssign(
            List<String> embryos,
            int index,
            Map<String, Integer> embryoLoadMap,
            List<MdmMoldingMachine> machines,
            Map<String, Set<String>> currentAllocation,
            Map<String, Integer> currentLoad,
            int maxTypes,
            int typeDiffThreshold,
            int loadDiffThreshold,
            Map<String, String> continueRecommendMachine,
            Map<String, Set<String>> bestAllocation,
            int[] bestScore) {

        // 所有胎胚已分配完成
        if (index == embryos.size()) {
            int score = calculateBalanceScore(currentAllocation, currentLoad, typeDiffThreshold, loadDiffThreshold);
            if (score < bestScore[0]) {
                bestScore[0] = score;
                bestAllocation.clear();
                for (Map.Entry<String, Set<String>> entry : currentAllocation.entrySet()) {
                    bestAllocation.put(entry.getKey(), new HashSet<>(entry.getValue()));
                }
            }
            return;
        }

        String embryo = embryos.get(index);
        int load = embryoLoadMap.getOrDefault(embryo, 1);
        String recommendMachine = continueRecommendMachine.get(embryo);

        // 尝试分配到每个机台
        for (MdmMoldingMachine machine : machines) {
            String machineCode = machine.getCxMachineCode();
            Set<String> currentTypes = currentAllocation.get(machineCode);
            int currentMachineLoad = currentLoad.get(machineCode);

            // 剪枝：检查种类上限
            if (!currentTypes.contains(embryo) && currentTypes.size() >= maxTypes) {
                continue;
            }

            // 尝试分配
            currentTypes.add(embryo);
            currentLoad.put(machineCode, currentMachineLoad + load);

            // 剪枝：检查均衡性
            if (isBalanced(currentLoad, loadDiffThreshold)) {
                dfsAssign(embryos, index + 1, embryoLoadMap, machines, currentAllocation, currentLoad,
                        maxTypes, typeDiffThreshold, loadDiffThreshold, continueRecommendMachine,
                        bestAllocation, bestScore);
            }

            // 回溯
            currentTypes.remove(embryo);
            currentLoad.put(machineCode, currentMachineLoad);
        }
    }

    /**
     * 计算均衡得分（越小越好）
     */
    private int calculateBalanceScore(
            Map<String, Set<String>> allocation,
            Map<String, Integer> load,
            int typeDiffThreshold,
            int loadDiffThreshold) {

        // 计算种类数差异
        List<Integer> typeCounts = allocation.values().stream()
                .map(Set::size)
                .collect(Collectors.toList());
        int typeDiff = Collections.max(typeCounts) - Collections.min(typeCounts);

        // 计算负荷差异
        List<Integer> loads = new ArrayList<>(load.values());
        int loadDiff = Collections.max(loads) - Collections.min(loads);

        // 综合得分（加权）
        return typeDiff * 10 + loadDiff;
    }

    /**
     * 检查是否均衡
     */
    private boolean isBalanced(Map<String, Integer> load, int loadDiffThreshold) {
        List<Integer> loads = new ArrayList<>(load.values());
        if (loads.isEmpty()) {
            return true;
        }
        int diff = Collections.max(loads) - Collections.min(loads);
        return diff <= loadDiffThreshold * 3; // 放宽阈值，允许更多探索
    }

    /**
     * 获取续作胎胚的推荐机台
     */
    private Map<String, String> getContinueRecommendMachines(
            List<String> continueEmbryos, ScheduleContextDTO context) {
        // TODO: 从上下文获取续作胎胚的已分配机台
        return new HashMap<>();
    }

    /**
     * 获取可用于某结构的机台列表
     */
    private List<MdmMoldingMachine> getMachinesForStructure(
            String structureName,
            List<MdmMoldingMachine> availableMachines,
            ScheduleContextDTO context) {

        List<MdmMoldingMachine> result = new ArrayList<>();

        // 先从结构排产配置获取
        if (context.getStructureAllocationMap() != null) {
            List<MpCxCapacityConfiguration> configs = context.getStructureAllocationMap().get(structureName);
            if (configs != null && !configs.isEmpty()) {
                Set<String> machineCodes = configs.stream()
                        .map(MpCxCapacityConfiguration::getCxMachineCode)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                for (MdmMoldingMachine machine : availableMachines) {
                    if (machineCodes.contains(machine.getCxMachineCode())) {
                        result.add(machine);
                    }
                }

                if (!result.isEmpty()) {
                    return result;
                }
            }
        }

        // 检查机台固定配置
        for (MdmMoldingMachine machine : availableMachines) {
            if (checkStructureConstraint(machine, structureName, context)) {
                result.add(machine);
            }
        }

        return result;
    }

    /**
     * 排序新增任务
     * 
     * <p>排序规则：
     * <ol>
     *   <li>按月计划优先级排序</li>
     *   <li>收尾任务优先</li>
     *   <li>紧急收尾优先</li>
     *   <li>10天内收尾优先</li>
     *   <li>新胎胚优先（但不能挤掉已排好的实单）</li>
     *   <li>按需求量排序（大的优先）</li>
     * </ol>
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
        taskAllocation.setIsContinueTask(task.getIsContinueTask());

        allocation.getTaskAllocations().add(taskAllocation);
        allocation.setUsedCapacity(allocation.getUsedCapacity() + quantity);
        allocation.setRemainingCapacity(allocation.getRemainingCapacity() - quantity);
    }
}
