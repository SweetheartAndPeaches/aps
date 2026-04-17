package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

/**
 * 均衡分配服务
 * 
 * <p>负责胎胚到机台的均衡分配，使用 DFS + 剪枝算法。
 * <p><b>优先级原则</b>：满排优先 > 均衡尽量满足
 * <ul>
 *   <li>硬约束：maxCapacity（机台最大硫化机数）、maxTypes（机台最大种类数）一定不能超过</li>
 *   <li>第一优先：所有任务全部排上（满排）</li>
 *   <li>第二优先：在满排前提下尽量满足均衡指标</li>
 *   <li>均衡指标：负荷差距 ≤ loadDiffThreshold 且 种类差距 ≤ typeDiffThreshold 为均衡解</li>
 *   <li>选择优先级：满排均衡解 > 满排不均衡解 > 非满排解</li>
 * </ul>
 *
 * <p>剪枝策略：
 * <ul>
 *   <li>剩余产能为零剪枝：剩余机台总产能为0时剪枝（产能不足时继续探索部分解）</li>
 *   <li>贪心上界剪枝：找到均衡完整解后，当前分支不可能满足均衡阈值时剪枝</li>
 *   <li>搜索限制：100万次，防止极端情况卡死</li>
 * </ul>
 *
 * <p>被以下处理器复用：
 * <ul>
 *   <li>{@link ContinueTaskProcessor} - 续作任务均衡分配</li>
 *   <li>{@link NewTaskProcessor} - 新增任务均衡分配（合并续作后重新均衡）</li>
 * </ul>
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalancingService {

    /** 机台最大胎胚种类数上限（默认4种） */
    public static final int DEFAULT_MAX_TYPES_PER_MACHINE = 4;

    /** 机台默认最大硫化机数（配比配置缺失时的兜底值） */
    public static final int DEFAULT_MAX_LH_MACHINE_QTY = 10;

    /** 参数编码：机台最大硫化机数 */
    private static final String PARAM_MAX_LH_MACHINE_QTY = "MAX_LH_MACHINE_QTY";

    /** 参数编码：强制保留历史任务  */
    private static final String PARAM_FORCE_KEEP_HISTORY = "FORCE_KEEP_HISTORY_TASK";
    
    /** 参数编码：胎胚种类数允许差额（均衡阈值） */
    private static final String PARAM_TYPE_DIFF_THRESHOLD = "BALANCE_TYPE_DIFF_THRESHOLD";
    
    /** 参数编码：硫化机台数允许差额（均衡阈值） */
    private static final String PARAM_LOAD_DIFF_THRESHOLD = "BALANCE_LOAD_DIFF_THRESHOLD";
    
    /** 默认：胎胚种类数允许差额（最多差1种） */
    private static final int DEFAULT_TYPE_DIFF_THRESHOLD = 1;
    
    /** 默认：硫化机台数允许差额（最多差3台） */
    private static final int DEFAULT_LOAD_DIFF_THRESHOLD = 3;

    // ==================== 核心均衡分配方法 ====================

    /**
     * 均衡分配胎胚到机台（DFS + 剪枝算法）
     *
     * <p>算法核心：
     * <ul>
     *   <li>目标：胎胚种类数均衡，硫化机台数配比均衡</li>
     *   <li>约束：机台最大硫化机数上限、胎胚种类数上限</li>
     *   <li>策略：深度优先搜索 + 剪枝，超过均衡阈值的分支直接舍弃</li>
     * </ul>
     *
     * <p>单位说明：
     * <ul>
     *   <li>总需求 = 结构下所有胎胚的硫化机台数之和</li>
     *   <li>总产能 = 所有可分配机台的最大硫化机数之和（每台机台可能不同）</li>
     * </ul>
     *
     * <p>注意：此方法使用统一的 maxLhMachines，如果不同机型有不同的最大硫化机数，
     * 请使用 {@link #balanceEmbryosToMachinesWithMachineCapacity} 方法
     *
     * @param tasks                 胎胚任务列表
     * @param availableMachines     可分配机台列表（结构排产配置）
     * @param machineHistoryMap     历史任务映射（机台 -> 昨天做的胎胚集合）
     * @param maxLhMachines         最大硫化机台数（每台成型机的产能上限，统一值）
     * @param maxEmbryoTypes        最大胎胚种类数
     * @param forceKeepHistory      是否强制保留历史任务
     * @param context               排程上下文
     * @return 均衡分配结果
     */
    public BalancingResult balanceEmbryosToMachines(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
            List<MpCxCapacityConfiguration> availableMachines,
            Map<String, Set<String>> machineHistoryMap,
            int maxLhMachines,
            int maxEmbryoTypes,
            boolean forceKeepHistory,
            ScheduleContextVo context) {

        // 构建统一的机台最大硫化机数映射（向后兼容）
        Map<String, Integer> machineMaxLhMap = new HashMap<>();
        for (MpCxCapacityConfiguration config : availableMachines) {
            machineMaxLhMap.put(config.getCxMachineCode(), maxLhMachines);
        }

        return balanceEmbryosToMachinesWithMachineCapacity(
                tasks, availableMachines, machineHistoryMap,
                machineMaxLhMap, maxEmbryoTypes, forceKeepHistory, context);
    }

    /**
     * 均衡分配（简化版，根据机型+结构获取每台机台的最大硫化机数）
     *
     * <p>重要：每台成型机的机型可能不同，需要根据机型+结构从 MdmStructureLhRatio 获取各自的最大硫化机数
     *
     * @param tasks             胎胚任务列表
     * @param availableMachines 可分配机台列表（包含机型信息）
     * @param structureName     当前处理的结构名称
     * @param context           排程上下文
     * @return 均衡分配结果
     */
    public BalancingResult balanceEmbryosToMachines(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
            List<MdmMoldingMachine> availableMachines,
            String structureName,
            ScheduleContextVo context) {

        log.info("====== 均衡分配(简化版)开始 ======");
        log.info("结构={}, 任务数={}, 可用机台数={}, 机台列表={}",
                structureName, tasks.size(), availableMachines.size(),
                availableMachines.stream().map(MdmMoldingMachine::getCxMachineCode).collect(Collectors.toList()));

        // 打印每个胎胚任务详情
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
            log.info("  胎胚任务: embryoCode={}, materialCode={}, vulcanizeMachineCount={}, structureName={}",
                    task.getEmbryoCode(), task.getMaterialCode(),
                    task.getVulcanizeMachineCount(), task.getStructureName());
        }

        // 转换为配置格式
        List<MpCxCapacityConfiguration> configs = availableMachines.stream()
                .map(m -> {
                    MpCxCapacityConfiguration config = new MpCxCapacityConfiguration();
                    config.setCxMachineCode(m.getCxMachineCode());
                    return config;
                })
                .collect(Collectors.toList());

        // 根据机型+结构获取每台机台的最大硫化机数
        Map<String, Integer> machineMaxLhMap = buildMachineMaxLhMap(availableMachines, structureName, context);

        // 根据机型+结构获取每台机台的最大胎胚种类数
        Map<String, Integer> machineMaxEmbryoTypesMap = buildMachineMaxEmbryoTypesMap(availableMachines, structureName, context);

        // 获取其他参数
        boolean forceKeepHistory = getForceKeepHistoryConfig(context);
        Map<String, Set<String>> machineHistoryMap = context.getMachineOnlineEmbryoMap();
        if (machineHistoryMap == null) {
            machineHistoryMap = new HashMap<>();
        }

        return balanceEmbryosToMachinesWithMachineCapacity(
                tasks, configs, machineHistoryMap,
                machineMaxLhMap, machineMaxEmbryoTypesMap, forceKeepHistory, context);
    }

    /**
     * 构建机台最大硫化机数映射
     *
     * <p>根据每台机台的机型 + 结构，从 MdmStructureLhRatio 获取对应的最大硫化机数
     *
     * @param machines      机台列表
     * @param structureName 结构名称
     * @param context       排程上下文
     * @return 机台编码 -> 最大硫化机数
     */
    private Map<String, Integer> buildMachineMaxLhMap(
            List<MdmMoldingMachine> machines,
            String structureName,
            ScheduleContextVo context) {

        Map<String, Integer> result = new HashMap<>();
        List<MdmStructureLhRatio> ratios = context.getStructureLhRatios();

        // 构建 机型_结构 -> 最大硫化机数 的映射
        Map<String, Integer> machineTypeStructureMap = new HashMap<>();
        if (ratios != null) {
            log.info("结构 {} 配比数据共 {} 条", structureName, ratios.size());
            int matchCount = 0;
            for (MdmStructureLhRatio ratio : ratios) {
                String key = ratio.getCxMachineTypeCode() + "_" + ratio.getStructureName();
                if (ratio.getLhMachineMaxQty() != null) {
                    machineTypeStructureMap.put(key, ratio.getLhMachineMaxQty());
                    // 只记录匹配当前结构的
                    if (structureName.equals(ratio.getStructureName())) {
                        log.info("  配比匹配: 机型={}, 结构={}, 硫化机上限={}", 
                                ratio.getCxMachineTypeCode(), ratio.getStructureName(), ratio.getLhMachineMaxQty());
                        matchCount++;
                    }
                }
            }
            log.info("结构 {} 找到 {} 条配比配置", structureName, matchCount);
        } else {
            log.warn("结构 {} 配比数据为空", structureName);
        }

        // 为每台机台获取对应的最大硫化机数
        int fallbackCount = 0;
        for (MdmMoldingMachine machine : machines) {
            String machineCode = machine.getCxMachineCode();
            String machineType = machine.getCxMachineTypeCode();

            // 优先根据机型+结构查找
            String key = machineType + "_" + structureName;
            Integer maxLh = machineTypeStructureMap.get(key);

            // 如果找不到，使用机台本身的硫化机上限
            if (maxLh == null) {
                int defaultMaxLh = context.getMaxLhMachineQty() != null ? context.getMaxLhMachineQty() : DEFAULT_MAX_LH_MACHINE_QTY;
                maxLh = machine.getLhMachineMaxQty() != null ? machine.getLhMachineMaxQty() : defaultMaxLh;
                log.info("  机台 {} 机型 {} 未找到配比，使用默认值 {}", machineCode, machineType, maxLh);
                fallbackCount++;
            }

            result.put(machineCode, maxLh);
        }

        if (fallbackCount > 0) {
            log.warn("结构 {} 有 {}/{} 台机台未找到配比配置，使用默认值", structureName, fallbackCount, machines.size());
        }
        return result;
    }

    /**
     * 构建机台最大胎胚种类数映射
     *
     * <p>根据每台机台的机型 + 结构，从 MdmStructureLhRatio 获取对应的最大胎胚种类数
     *
     * @param machines      机台列表
     * @param structureName 结构名称
     * @param context       排程上下文
     * @return 机台编码 -> 最大胎胚种类数
     */
    private Map<String, Integer> buildMachineMaxEmbryoTypesMap(
            List<MdmMoldingMachine> machines,
            String structureName,
            ScheduleContextVo context) {

        log.info("构建机台最大胎胚种类数映射: 结构={}, 机台数={}", structureName, machines.size());

        Map<String, Integer> result = new HashMap<>();

        // 构建机台编码 -> 机型 映射
        Map<String, String> machineTypeMap = new HashMap<>();
        for (MdmMoldingMachine machine : machines) {
            machineTypeMap.put(machine.getCxMachineCode(), machine.getCxMachineTypeCode());
        }

        // 构建 机型_结构 -> 最大胎胚种类数 映射
        Map<String, Integer> typeStructureMap = new HashMap<>();
        List<MdmStructureLhRatio> ratios = context.getStructureLhRatios();
        if (ratios != null) {
            log.info("  配比数据共 {} 条，筛选结构={}", ratios.size(), structureName);
            int matchCount = 0;
            for (MdmStructureLhRatio ratio : ratios) {
                String key = ratio.getCxMachineTypeCode() + "_" + ratio.getStructureName();
                if (ratio.getMaxEmbryoQty() != null) {
                    typeStructureMap.put(key, ratio.getMaxEmbryoQty());
                    if (structureName.equals(ratio.getStructureName())) {
                        log.info("  配比匹配: 机型={}, 结构={}, 最大胎胚种类数={}",
                                ratio.getCxMachineTypeCode(), ratio.getStructureName(), ratio.getMaxEmbryoQty());
                        matchCount++;
                    }
                }
            }
            log.info("  结构 {} 匹配到 {} 条配比记录", structureName, matchCount);
        } else {
            log.warn("  配比数据为空，所有机台将使用默认值");
        }

        for (MdmMoldingMachine machine : machines) {
            String machineCode = machine.getCxMachineCode();
            String machineType = machineTypeMap.get(machineCode);

            String key = machineType + "_" + structureName;
            Integer maxTypes = typeStructureMap.get(key);

            // 如果找不到，使用默认值
            if (maxTypes == null) {
                maxTypes = context.getMaxTypesPerMachine() != null ? context.getMaxTypesPerMachine() : DEFAULT_MAX_TYPES_PER_MACHINE;
                log.debug("  机台 {} 机型 {} 未找到配比，使用默认最大胎胚种类数 {}", machineCode, machineType, maxTypes);
            }

            log.info("  机台 {} (机型={}): 最大胎胚种类数={}", machineCode, machineType, maxTypes);
            result.put(machineCode, maxTypes);
        }

        return result;
    }

    /**
     * 均衡分配胎胚到机台（支持每台机台有不同的最大硫化机数和最大胎胚种类数）
     *
     * @param tasks                   胎胚任务列表
     * @param availableMachines       可分配机台列表
     * @param machineHistoryMap       历史任务映射
     * @param machineMaxLhMap         机台最大硫化机数映射（机台编码 -> 最大硫化机数）
     * @param machineMaxEmbryoTypesMap 机台最大胎胚种类数映射（机台编码 -> 最大胎胚种类数）
     * @param forceKeepHistory        是否强制保留历史任务
     * @param context                 排程上下文
     * @return 均衡分配结果
     */
    public BalancingResult balanceEmbryosToMachinesWithMachineCapacity(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
            List<MpCxCapacityConfiguration> availableMachines,
            Map<String, Set<String>> machineHistoryMap,
            Map<String, Integer> machineMaxLhMap,
            Map<String, Integer> machineMaxEmbryoTypesMap,
            boolean forceKeepHistory,
            ScheduleContextVo context) {

        // Step 1: 获取均衡阈值配置
        int typeDiffThreshold = getTypeDiffThreshold(context);
        int loadDiffThreshold = getLoadDiffThreshold(context);

        log.info("均衡分配参数：种类差额阈值={}, 负荷差额阈值={}",
                typeDiffThreshold, loadDiffThreshold);

        // Step 2: 计算总需求（所有胎胚的硫化机台数之和）
        int totalDemand = tasks.stream()
                .mapToInt(t -> t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 0)
                .sum();

        // Step 3: 计算总产能（每台机台的最大硫化机数之和，不再简单相乘）
        int totalCapacity = 0;
        for (MpCxCapacityConfiguration config : availableMachines) {
            Integer maxLh = machineMaxLhMap.get(config.getCxMachineCode());
            totalCapacity += (maxLh != null ? maxLh : DEFAULT_MAX_LH_MACHINE_QTY);
        }

        // 计算种类数（去重）
        long totalTypes = tasks.stream()
                .map(CoreScheduleAlgorithmService.DailyEmbryoTask::getEmbryoCode)
                .distinct()
                .count();

        log.info("均衡分配计算：总需求（硫化机台数）={}, 种类数={}, 总产能（各机台最大硫化机数之和）={}, 机台数={}",
                totalDemand, totalTypes, totalCapacity, availableMachines.size());

        // 打印任务需求明细（合并同胚子代码）
        Map<String, Integer> taskDetailMap = new LinkedHashMap<>();
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
            String code = task.getEmbryoCode();
            int cnt = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;
            taskDetailMap.merge(code, cnt, Integer::sum);
        }
        List<String> taskDetails = taskDetailMap.entrySet().stream()
                .map(e -> e.getKey() + "(" + e.getValue() + ")")
                .collect(Collectors.toList());
        log.info("任务需求明细：{}", taskDetails);

        // Step 4: 排序胎胚任务
        // 先计算每个胎胚编码的总需求量（同编码的任务已拆成(1)单元，需汇总）
        Map<String, Integer> embryoTotalDemand = new HashMap<>();
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
            embryoTotalDemand.merge(task.getEmbryoCode(),
                    task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0,
                    Integer::sum);
        }

        // 第零排序：约束量试任务优先（候选机台最少，必须先安排）
        // 新增排序：续作任务优先（候选机台受限，必须先保住）
        // 第一排序：胎胚总需求量降序（大需求优先占种类槽，产能不足时丢弃小需求更划算）
        // 第二排序：候选机台数升序（受限任务优先）
        final Set<String> availableMachineCodes = availableMachines.stream()
                .map(MpCxCapacityConfiguration::getCxMachineCode)
                .collect(Collectors.toSet());
        
        List<CoreScheduleAlgorithmService.DailyEmbryoTask> sortedTasks = tasks.stream()
                .sorted((a, b) -> {
                    // 第零排序：约束量试任务优先
                    boolean aConstrained = a.getConstrainedMachineCode() != null && !a.getConstrainedMachineCode().isEmpty();
                    boolean bConstrained = b.getConstrainedMachineCode() != null && !b.getConstrainedMachineCode().isEmpty();
                    if (aConstrained != bConstrained) return aConstrained ? -1 : 1;

                    // 新增排序：续作任务优先（候选机台受限，必须先保住）
                    boolean aContinue = a.getIsContinueTask() != null && a.getIsContinueTask()
                            && a.getContinueMachineCodes() != null && !a.getContinueMachineCodes().isEmpty();
                    boolean bContinue = b.getIsContinueTask() != null && b.getIsContinueTask()
                            && b.getContinueMachineCodes() != null && !b.getContinueMachineCodes().isEmpty();
                    if (aContinue != bContinue) return aContinue ? -1 : 1;

                    // 第一排序：胎胚总需求量降序（大需求优先占种类槽）
                    int totalA = embryoTotalDemand.getOrDefault(a.getEmbryoCode(), 0);
                    int totalB = embryoTotalDemand.getOrDefault(b.getEmbryoCode(), 0);
                    int demandCompare = Integer.compare(totalB, totalA);
                    if (demandCompare != 0) return demandCompare;
                    
                    // 第二排序：候选机台数升序（受限任务优先）
                    MachineState tmpA = createTempMachineState(availableMachineCodes);
                    MachineState tmpB = createTempMachineState(availableMachineCodes);
                    int candA = countCandidatesForStaticSort(a.getEmbryoCode(), tmpA);
                    int candB = countCandidatesForStaticSort(b.getEmbryoCode(), tmpB);
                    return Integer.compare(candA, candB);
                })
                .collect(Collectors.toList());

        // Step 5: 初始化机台状态（每台机台使用自己的最大硫化机数和最大胎胚种类数）
        List<MachineState> machineStates = new ArrayList<>();
        for (MpCxCapacityConfiguration config : availableMachines) {
            MachineState state = new MachineState();
            state.setMachineCode(config.getCxMachineCode());

            // 从映射中获取该机台的最大硫化机数
            Integer maxLh = machineMaxLhMap.get(config.getCxMachineCode());
            state.setMaxCapacity(maxLh != null ? maxLh : DEFAULT_MAX_LH_MACHINE_QTY);

            // 从映射中获取该机台的最大胎胚种类数
            Integer maxTypes = machineMaxEmbryoTypesMap.get(config.getCxMachineCode());
            state.setMaxTypes(maxTypes != null ? maxTypes : DEFAULT_MAX_TYPES_PER_MACHINE);

            state.setCurrentLoad(0);
            state.setCurrentTypes(0);
            state.setAssignedEmbryos(new ArrayList<>());

            Set<String> historyEmbryos = machineHistoryMap.get(config.getCxMachineCode());
            state.setHistoryEmbryos(historyEmbryos != null ? historyEmbryos : new HashSet<>());

            log.info("  初始化机台 {}: maxCapacity={}, maxTypes={}, 历史胎胚={}",
                    config.getCxMachineCode(), state.getMaxCapacity(), state.getMaxTypes(), state.getHistoryEmbryos());

            machineStates.add(state);
        }

        // Step 6: 如果强制保留历史任务，先进行保底预留
        if (forceKeepHistory) {
            reservedHistoryTasks(sortedTasks, machineStates, context);
        }

        // 产能充足标记（影响DFS候选排序策略）
        boolean capacitySufficient = (totalCapacity >= totalDemand);
        
        // 产能不足提示（DFS仍可处理部分解，无需贪心兜底）
        int effectiveCapacity = totalCapacity;
        if (!capacitySufficient) {
            log.warn("产能不足（产能={}, 需求={}），DFS将尝试最优部分解", effectiveCapacity, totalDemand);
        }

        // Step 7: DFS + 剪枝搜索最优方案
        DfsSearchResult searchResult = new DfsSearchResult();
        searchResult.bestScore = Integer.MAX_VALUE;
        searchResult.bestAssignedCount = 0;
        searchResult.bestIsBalanced = false;
        searchResult.bestAssignments = null;
        searchResult.bestMachineCodes = null;
        searchResult.searchCount = 0;
        searchResult.pruneCount = 0;

        List<CoreScheduleAlgorithmService.DailyEmbryoTask> remainingTasks = getRemainingTasks(sortedTasks);
        
        // 打印 DFS 任务列表（排序后）
        if (!remainingTasks.isEmpty()) {
            List<String> taskList = remainingTasks.stream()
                    .map(t -> t.getEmbryoCode() + "(" + (t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 0) + ")")
                    .collect(Collectors.toList());
            log.info("DFS任务列表（排序后，共{}个）：{}", remainingTasks.size(), taskList);
        }
        
        // 初始化：从第一个任务开始，remainingCount 为第一个任务的硫化机台数
        int initialRemainingCount = remainingTasks.isEmpty() ? 0 
                : (remainingTasks.get(0).getVulcanizeMachineCount() != null 
                   ? remainingTasks.get(0).getVulcanizeMachineCount() : 0);
        
        dfsAssign(remainingTasks, 0, initialRemainingCount, machineStates, forceKeepHistory,
                typeDiffThreshold, loadDiffThreshold, totalDemand, searchResult, capacitySufficient);

        log.info("DFS搜索统计：总搜索次数={}, 剪枝次数={}, 最优分数={}, 均衡={}, 最优已分配={}/{}",
                searchResult.searchCount, searchResult.pruneCount, searchResult.bestScore,
                searchResult.bestIsBalanced ? "满足" : "不满足",
                searchResult.bestAssignedCount, totalDemand);

        // 输出未被分配的任务（区分正式/量试）
        if (searchResult.bestAssignedCount < totalDemand && searchResult.bestAssignments != null) {
            log.info("检测到分配不足：已分配={}/总需求={}, bestAssignments={}",
                    searchResult.bestAssignedCount, totalDemand, searchResult.bestAssignments != null ? "存在" : "null");
            // 统计每个embryoCode的已分配总量
            Map<String, Integer> assignedQtyMap = new java.util.HashMap<>();
            for (List<EmbryoAssignment> assignments : searchResult.bestAssignments) {
                for (EmbryoAssignment ea : assignments) {
                    assignedQtyMap.merge(ea.getEmbryoCode(), ea.getAssignedQty(), Integer::sum);
                }
            }
            // 按 embryoCode 统计原始需求
            Map<String, Integer> demandQtyMap = new java.util.LinkedHashMap<>();
            for (CoreScheduleAlgorithmService.DailyEmbryoTask t : remainingTasks) {
                String key = t.getEmbryoCode()
                        + (t.getIsProductionTrial() != null && t.getIsProductionTrial() ? "(量试)" : "(正式)")
                        + (t.getConstrainedMachineCode() != null ? "[约束:" + t.getConstrainedMachineCode() + "]" : "");
                demandQtyMap.merge(key, t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 1, Integer::sum);
            }
            // 对比：同embryoCode总需求 vs 总已分配
            Map<String, Integer> demandByEmbryo = new java.util.HashMap<>();
            for (CoreScheduleAlgorithmService.DailyEmbryoTask t : remainingTasks) {
                demandByEmbryo.merge(t.getEmbryoCode(), t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 1, Integer::sum);
            }
            List<String> shortageItems = new java.util.ArrayList<>();
            for (Map.Entry<String, Integer> e : demandByEmbryo.entrySet()) {
                int assigned = assignedQtyMap.getOrDefault(e.getKey(), 0);
                if (assigned < e.getValue()) {
                    shortageItems.add(e.getKey() + "(" + assigned + "/" + e.getValue() + ")");
                }
            }
            if (!shortageItems.isEmpty()) {
                log.warn("分配不足的胎胚：{}，各任务明细：{}", shortageItems, demandQtyMap);
            }
        }

        // Step 8: 构建结果
        BalancingResult result;
        if (searchResult.bestAssignments != null) {
            // 直接使用 bestAssignedCount 判断完整性（避免从 Map 统计因重复 key 导致数据丢失）
            if (searchResult.bestAssignedCount == totalDemand) {
                result = convertDfsResultToBalancingResult(searchResult.bestAssignments, searchResult.bestMachineCodes, machineStates, sortedTasks);
                log.info("找到满足均衡条件的完整方案，已分配 {} 台硫化机", searchResult.bestAssignedCount);
            } else {
                log.warn("DFS搜索完成：找到最优但不完备的解（已分配 {}/{}），这是约束系统允许的最大值，直接使用此结果", searchResult.bestAssignedCount, totalDemand);
                result = convertDfsResultToBalancingResult(searchResult.bestAssignments, searchResult.bestMachineCodes, machineStates, sortedTasks);
            }
        } else {
            // DFS未找到任何方案，检查 machineStates 是否有保底预留的分配
            boolean hasReservedAssignments = machineStates.stream()
                    .anyMatch(s -> !s.getAssignedEmbryos().isEmpty());
            if (hasReservedAssignments) {
                log.info("DFS无剩余任务，但存在保底预留分配，收集预留结果");
                result = buildResultFromMachineStates(machineStates);
            } else {
                log.warn("DFS未找到任何方案，返回空结果");
                BalancingResult emptyResult = new BalancingResult();
                emptyResult.setAssignments(new ArrayList<>());
                result = emptyResult;
            }
        }

        logAllocationResult(result, machineStates, remainingTasks);
        return result;
    }

    // ==================== 向后兼容方法 ====================

    /**
     * 均衡分配胎胚到机台（向后兼容，统一 maxEmbryoTypes）
     *
     * @deprecated 请使用 {@link #balanceEmbryosToMachinesWithMachineCapacity} 方法
     */
    @Deprecated
    public BalancingResult balanceEmbryosToMachinesWithMachineCapacity(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
            List<MpCxCapacityConfiguration> availableMachines,
            Map<String, Set<String>> machineHistoryMap,
            Map<String, Integer> machineMaxLhMap,
            int maxEmbryoTypes,
            boolean forceKeepHistory,
            ScheduleContextVo context) {

        // 构建统一的机台最大胎胚种类数映射（向后兼容）
        Map<String, Integer> machineMaxEmbryoTypesMap = new HashMap<>();
        for (MpCxCapacityConfiguration config : availableMachines) {
            machineMaxEmbryoTypesMap.put(config.getCxMachineCode(), maxEmbryoTypes);
        }

        return balanceEmbryosToMachinesWithMachineCapacity(
                tasks, availableMachines, machineHistoryMap,
                machineMaxLhMap, machineMaxEmbryoTypesMap, forceKeepHistory, context);
    }

    // ==================== DFS + 剪枝算法核心 ====================

    /**
     * 保底预留历史任务
     */
    private void reservedHistoryTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
            List<MachineState> machineStates,
            ScheduleContextVo context) {
        
        log.info("开始保底预留历史任务...");
        
        int totalReserved = 0;
        
        for (MachineState state : machineStates) {
            Set<String> historyEmbryos = state.getHistoryEmbryos();
            if (historyEmbryos == null || historyEmbryos.isEmpty()) {
                continue;
            }
            
            for (String embryoCode : historyEmbryos) {
                // 直接遍历 tasks 列表按 embryoCode 匹配，避免 Map 去2重丢失多机台同胚胎的预留
                CoreScheduleAlgorithmService.DailyEmbryoTask matchedTask = null;
                for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
                    if (embryoCode.equals(task.getEmbryoCode())) {
                        int remainingDemand = task.getVulcanizeMachineCount() != null
                                ? task.getVulcanizeMachineCount() : 0;
                        if (remainingDemand > 0) {
                            matchedTask = task;
                            break;
                        }
                    }
                }
                
                if (matchedTask == null) {
                    continue;
                }
                
                int remainingDemand = matchedTask.getVulcanizeMachineCount() != null
                        ? matchedTask.getVulcanizeMachineCount() : 0;
                
                if (state.getCurrentLoad() >= state.getMaxCapacity()) {
                    log.warn("机台 {} 容量已满，无法保底预留胎胚 {}",
                            state.getMachineCode(), embryoCode);
                    continue;
                }
                
                // 检查胎胚种类数是否已达上限
                boolean isNewTypeForHistory = !state.getAssignedEmbryos().stream()
                        .anyMatch(e -> e.getEmbryoCode().equals(embryoCode));
                if (isNewTypeForHistory && state.getCurrentTypes() >= state.getMaxTypes()) {
                    log.warn("机台 {} 胎胚种类已达上限 ({}/{})，无法保底预留胎胚 {}",
                            state.getMachineCode(), state.getCurrentTypes(), state.getMaxTypes(), embryoCode);
                    continue;
                }
                
                // 保底预留1个硫化机台数
                int reservedCount = 1;
                
                state.getAssignedEmbryos().add(new EmbryoAssignment(embryoCode, matchedTask, reservedCount));
                state.setCurrentLoad(state.getCurrentLoad() + reservedCount);
                state.setCurrentTypes(state.getCurrentTypes() + 1);
                
                matchedTask.setVulcanizeMachineCount(remainingDemand - reservedCount);
                
                totalReserved++;
                log.info("机台 {} 保底预留胎胚 {} 共 {} 个硫化机",
                        state.getMachineCode(), embryoCode, reservedCount);
            }
        }
        
        log.info("保底预留完成，共预留 {} 个任务", totalReserved);
    }

    /**
     * 获取剩余待分配的任务（需求量 > 0）
     */
    private List<CoreScheduleAlgorithmService.DailyEmbryoTask> getRemainingTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks) {
        return tasks.stream()
                .filter(t -> t.getVulcanizeMachineCount() != null && t.getVulcanizeMachineCount() > 0)
                .collect(Collectors.toList());
    }

    /**
     * DFS深度优先搜索 + 剪枝（支持胎胚拆分）
     *
     * <p>算法改进：
     * <ul>
     *   <li>支持将一个胎胚的硫化机台数拆分到多台机台</li>
     *   <li>递归单位从"整个胎胚"改为"单个硫化机台数"</li>
     *   <li>增加 remainingCount 参数跟踪当前胎胚剩余待分配的硫化机数</li>
     * </ul>
     */
    private void dfsAssign(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
            int taskIndex,
            int remainingCount,
            List<MachineState> machineStates,
            boolean forceKeepHistory,
            int typeDiffThreshold,
            int loadDiffThreshold,
            int totalDemand,
            DfsSearchResult searchResult,
            boolean capacitySufficient) {

        searchResult.searchCount++;
        searchResult.callCount++;

        // 安全限制：搜索次数超过 100 万次后停止（防止极端情况卡死）
        if (searchResult.searchCount > 1000000) {
            return;
        }

        // 【剩余产能为零剪枝】：剩余产能为0时，无法再分配任何任务，剪枝
        // 注意：产能不足（产能<需求）时不剪枝，DFS需要继续探索最优部分解
        int allCurrentLoad = 0;
        int allCapacity = 0;
        for (MachineState s : machineStates) {
            allCurrentLoad += s.getCurrentLoad();
            allCapacity += s.getMaxCapacity();
        }
        int remainingCapacity = allCapacity - allCurrentLoad;
        if (remainingCapacity <= 0) {
            // 产能已耗尽，先记录当前部分解，再剪枝
            int totalAssignedNow = allCurrentLoad;
            int totalRequiredAll = tasks.stream()
                    .mapToInt(t -> t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 0)
                    .sum();
            if (totalAssignedNow < totalRequiredAll) {
                // 部分解评估
                int partialScore = calculateBalancingScore(machineStates);
                boolean currentBestIsComplete = (searchResult.bestAssignedCount == totalRequiredAll);
                if (!currentBestIsComplete &&
                        (totalAssignedNow > searchResult.bestAssignedCount ||
                        (totalAssignedNow == searchResult.bestAssignedCount && partialScore < searchResult.bestScore))) {
                    searchResult.bestScore = partialScore;
                    searchResult.bestAssignedCount = totalAssignedNow;
                    searchResult.bestIsBalanced = isBalanced(machineStates, typeDiffThreshold, loadDiffThreshold);
                    searchResult.bestAssignments = copyAssignments(machineStates);
                    searchResult.bestMachineCodes = copyMachineCodes(machineStates);
                }
            } else {
                // 完整解评估（所有任务已分配，产能刚好耗尽）
                int score = calculateBalancingScore(machineStates);
                boolean currentIsBalanced = isBalanced(machineStates, typeDiffThreshold, loadDiffThreshold);
                boolean currentBestIsComplete = (searchResult.bestAssignedCount == totalRequiredAll);
                boolean shouldReplace = false;
                if (!currentBestIsComplete) {
                    shouldReplace = true;
                } else if (currentIsBalanced && !searchResult.bestIsBalanced) {
                    shouldReplace = true;
                } else if (currentIsBalanced == searchResult.bestIsBalanced && score < searchResult.bestScore) {
                    shouldReplace = true;
                }
                if (shouldReplace) {
                    searchResult.bestScore = score;
                    searchResult.bestAssignedCount = totalAssignedNow;
                    searchResult.bestIsBalanced = currentIsBalanced;
                    searchResult.bestAssignments = copyAssignments(machineStates);
                    searchResult.bestMachineCodes = copyMachineCodes(machineStates);
                }
            }
            searchResult.pruneCount++;
            return;
        }

        // 【贪心上界剪枝】：只在已找到完整解后才启用均衡剪枝
        // 满排优先：没找到完整解时不剪枝，确保优先探索满排方案
        // 找到均衡完整解后：当前负荷差不可能满足阈值时剪枝（加速收敛）
        if (searchResult.bestAssignedCount >= totalDemand && searchResult.bestIsBalanced) {
            int curMaxLoad = 0;
            for (MachineState s : machineStates) {
                if (s.getCurrentLoad() > curMaxLoad) {
                    curMaxLoad = s.getCurrentLoad();
                }
            }
            // 贪心解的负荷下界：总已分配 / 机台数
            int greedyLoadLowerBound = allCurrentLoad / machineStates.size();
            // 如果当前最大负荷 - 贪心下界 > 负荷阈值，说明此分支不可能满足均衡条件
            if (curMaxLoad - greedyLoadLowerBound > loadDiffThreshold) {
                searchResult.pruneCount++;
                return;
            }
        }

        // 终止条件：所有任务已分配
        if (taskIndex >= tasks.size()) {
            // 检查是否所有任务都已分配完毕
            int totalAssigned = machineStates.stream()
                    .mapToInt(MachineState::getCurrentLoad).sum();
            int totalRequired = tasks.stream()
                    .mapToInt(t -> t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 0)
                    .sum();
            
            if (totalAssigned == totalRequired) {
                // 完整解：优先级 = 满排均衡 > 满排不均衡
                int score = calculateBalancingScore(machineStates);
                boolean currentIsBalanced = isBalanced(machineStates, typeDiffThreshold, loadDiffThreshold);
                boolean currentBestIsComplete = (searchResult.bestAssignedCount == totalRequired);
                
                boolean shouldReplace = false;
                if (!currentBestIsComplete) {
                    // 之前是部分解，当前完整解更优
                    shouldReplace = true;
                } else if (currentIsBalanced && !searchResult.bestIsBalanced) {
                    // 之前是不均衡完整解，当前是均衡完整解 → 替换
                    shouldReplace = true;
                } else if (currentIsBalanced == searchResult.bestIsBalanced && score < searchResult.bestScore) {
                    // 同等均衡等级，分数更优 → 替换
                    shouldReplace = true;
                }
                
                if (shouldReplace) {
                    searchResult.bestScore = score;
                    searchResult.bestAssignedCount = totalAssigned;
                    searchResult.bestIsBalanced = currentIsBalanced;
                    searchResult.bestAssignments = copyAssignments(machineStates);
                    searchResult.bestMachineCodes = copyMachineCodes(machineStates);
                }
            } else {
                // 部分解：完整度优先（分配更多优于更均衡），同等完整度比较均衡分数
                int partialScore = calculateBalancingScore(machineStates);
                boolean currentBestIsComplete = (searchResult.bestAssignedCount == totalRequired);
                if (!currentBestIsComplete && 
                        (totalAssigned > searchResult.bestAssignedCount ||
                        (totalAssigned == searchResult.bestAssignedCount && partialScore < searchResult.bestScore))) {
                    searchResult.bestScore = partialScore;
                    searchResult.bestAssignedCount = totalAssigned;
                    searchResult.bestIsBalanced = isBalanced(machineStates, typeDiffThreshold, loadDiffThreshold);
                    searchResult.bestAssignments = copyAssignments(machineStates);
                    searchResult.bestMachineCodes = copyMachineCodes(machineStates);
                }
            }
            return;
        }
        
        CoreScheduleAlgorithmService.DailyEmbryoTask task = tasks.get(taskIndex);
        String embryoCode = task.getEmbryoCode();
        
        // 如果当前胎胚还有剩余未分配的硫化机数
        if (remainingCount > 0) {
            // 找出可以分配的候选机台（只要有剩余容量即可）
            // isFirstCall=true 时打印日志，避免重复噪音
            List<MachineState> candidates = findCandidateMachinesForSplit(
                    embryoCode, machineStates, forceKeepHistory, searchResult.searchCount == 1,
                    task.getConstrainedMachineCode());
            
            if (candidates.isEmpty()) {
                // 没有可用机台，跳过当前任务，继续处理后续任务（记录部分解）
                int totalAssignedNow = machineStates.stream().mapToInt(MachineState::getCurrentLoad).sum();
                int totalRequiredAll = tasks.stream()
                        .mapToInt(t -> t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 0)
                        .sum();
                int partialScore = calculateBalancingScore(machineStates);
                boolean currentBestIsComplete = (searchResult.bestAssignedCount == totalRequiredAll);
                if (!currentBestIsComplete &&
                        (totalAssignedNow > searchResult.bestAssignedCount ||
                        (totalAssignedNow == searchResult.bestAssignedCount && partialScore < searchResult.bestScore))) {
                    searchResult.bestScore = partialScore;
                    searchResult.bestAssignedCount = totalAssignedNow;
                    searchResult.bestIsBalanced = isBalanced(machineStates, typeDiffThreshold, loadDiffThreshold);
                    searchResult.bestAssignments = copyAssignments(machineStates);
                    searchResult.bestMachineCodes = copyMachineCodes(machineStates);
                }
                // 跳过当前任务，递归处理下一个
                dfsAssign(tasks, taskIndex + 1, 0, machineStates, forceKeepHistory,
                        typeDiffThreshold, loadDiffThreshold, totalDemand, searchResult, capacitySufficient);
                return;
            }
            
            // 按优先级排序候选机台
            sortCandidatesForDfs(candidates, embryoCode, forceKeepHistory, capacitySufficient, loadDiffThreshold);
            
            // 尝试给每个候选机台分配 k 个硫化机台数（k从1到min(remainingCount, 机台剩余容量)）
            for (MachineState candidate : candidates) {
                int maxCanAssign = Math.min(remainingCount, 
                        candidate.getMaxCapacity() - candidate.getCurrentLoad());
                
                if (maxCanAssign <= 0) {
                    continue;
                }
                
                // 尝试分配不同的数量（从大到小，优先填满一台机台）
                for (int assignQty = maxCanAssign; assignQty >= 1; assignQty--) {
                    // === 剪枝检查 ===
                    
                    int newTypes = candidate.getCurrentTypes();
                    boolean isNewType = !candidate.getAssignedEmbryos().stream()
                            .anyMatch(e -> e.getEmbryoCode().equals(embryoCode));
                    if (isNewType) {
                        newTypes++;
                    }
                    
                    // 剪枝条件1：超过机台最大胎胚种类数限制（硬约束，必须剪枝）
                    if (newTypes > candidate.getMaxTypes()) {
                        searchResult.pruneCount++;
                        continue;
                    }
                    
                    // 剪枝条件2：当前分配后产能耗尽，无需尝试更小分配量
                    // （DFS递归入口处会检查 remainingCapacity <= 0 剪枝）
                    
                    // 剪枝条件3：剩余种类可行性剪枝（已在下方实现）
                    // 注意：种类均衡和负荷均衡不做中间剪枝！
                    // 原因：DFS中间状态不可能均衡，只有最终分配结果才能判断均衡性
                    // 均衡性通过 calculateBalancingScore 在终点评估
                    
                    // 可行性剪枝：计算剩余机台种类容量，判断是否还能容纳剩余未分配的胎胚种类
                    // 注意：种类不足时仍需探索部分解（丢弃放不下的胎胚种类），所以只在满排可能时剪枝
                    int remainingTypeCapacity = 0;
                    for (MachineState state : machineStates) {
                        remainingTypeCapacity += state.getMaxTypes() - state.getCurrentTypes();
                        if (state == candidate && isNewType) {
                            remainingTypeCapacity--; // 当前分配已占1个
                        }
                    }
                    // 统计剩余未分配的胎胚种类数（当前任务之后的）
                    int remainingDistinctTypes = 0;
                    Set<String> assignedEmbryoSet = new HashSet<>();
                    for (MachineState state : machineStates) {
                        for (EmbryoAssignment ea : state.getAssignedEmbryos()) {
                            assignedEmbryoSet.add(ea.getEmbryoCode());
                        }
                    }
                    if (isNewType) {
                        assignedEmbryoSet.add(embryoCode);
                    }
                    for (int i = taskIndex; i < tasks.size(); i++) {
                        String nextEmbryo = tasks.get(i).getEmbryoCode();
                        if (!assignedEmbryoSet.contains(nextEmbryo)) {
                            remainingDistinctTypes++;
                            assignedEmbryoSet.add(nextEmbryo);
                        }
                    }
                    // 只在已找到完整解后启用此剪枝（加速搜索收敛）
                    // 未找到完整解时不剪枝，让DFS探索丢弃小需求种类、保大需求种类的部分解
                    if (remainingDistinctTypes > remainingTypeCapacity
                            && searchResult.bestAssignedCount >= totalDemand) {
                        searchResult.pruneCount++;
                        continue;
                    }
                    
                    // === 分配并递归 ===
                    int newLoad = candidate.getCurrentLoad() + assignQty;
                    candidate.getAssignedEmbryos().add(new EmbryoAssignment(embryoCode, task, assignQty));
                    candidate.setCurrentLoad(newLoad);
                    if (isNewType) {
                        candidate.setCurrentTypes(candidate.getCurrentTypes() + 1);
                    }
                    
                    int newRemainingCount = remainingCount - assignQty;
                    
                    // 如果当前胎胚还有剩余，继续分配；否则处理下一个任务
                    if (newRemainingCount > 0) {
                        dfsAssign(tasks, taskIndex, newRemainingCount, machineStates, forceKeepHistory,
                                typeDiffThreshold, loadDiffThreshold, totalDemand, searchResult, capacitySufficient);
                    } else {
                        dfsAssign(tasks, taskIndex + 1, 0, machineStates, forceKeepHistory,
                                typeDiffThreshold, loadDiffThreshold, totalDemand, searchResult, capacitySufficient);
                    }
                    
                    // === 回溯 ===
                    candidate.getAssignedEmbryos().remove(candidate.getAssignedEmbryos().size() - 1);
                    candidate.setCurrentLoad(candidate.getCurrentLoad() - assignQty);
                    if (isNewType) {
                        candidate.setCurrentTypes(candidate.getCurrentTypes() - 1);
                    }
                }
            }
        } else {
            // remainingCount=0：当前任务刚完成或刚被跳过，需要处理 taskIndex 处的任务
            // 注意：调用方传入 taskIndex 时已指向下一个待处理任务，remainingCount=0 表示该任务还未开始分配
            int currentLhCount = taskIndex < tasks.size()
                    ? (tasks.get(taskIndex).getVulcanizeMachineCount() != null
                       ? tasks.get(taskIndex).getVulcanizeMachineCount() : 0)
                    : 0;

            if (currentLhCount <= 0) {
                // 当前任务不需要分配，跳到下一个
                dfsAssign(tasks, taskIndex + 1, 0, machineStates, forceKeepHistory,
                        typeDiffThreshold, loadDiffThreshold, totalDemand, searchResult, capacitySufficient);
            } else {
                // 开始处理当前任务（传入其完整需求量）
                dfsAssign(tasks, taskIndex, currentLhCount, machineStates, forceKeepHistory,
                        typeDiffThreshold, loadDiffThreshold, totalDemand, searchResult, capacitySufficient);
            }
        }
    }
    
    /**
     * 创建临时MachineState用于静态排序（所有机台初始状态）
     */
    private MachineState createTempMachineState(Set<String> availableMachineCodes) {
        MachineState state = new MachineState();
        // 临时设置一个很大的容量，使其在初始时总是可用
        state.setCurrentLoad(0);
        state.setCurrentTypes(0);
        state.setMaxCapacity(100);
        state.setMaxTypes(DEFAULT_MAX_TYPES_PER_MACHINE);
        return state;
    }
    
    /**
     * 静态估算候选机台数（用于排序）
     * 
     * 策略：
     * 1. 如果机器数>=3且候选数量充足，返回3
     * 2. 如果是该批次中的后几个胚子（推理受限），返回较少候选
     * 
     * 由于没有运行时信息，使用胚胎编码数字部分来估算：
     * 数字大的胚胎（后面的）通常候选更少
     */
    private int countCandidatesForStaticSort(String embryoCode, MachineState tmpState) {
        // 提取胚胎编码中的数字部分
        String numPart = embryoCode.replaceAll("[^0-9]", "");
        int num = 0;
        if (!numPart.isEmpty()) {
            try { num = Integer.parseInt(numPart); } catch (NumberFormatException ignored) {}
        }
        
        // 胚胎编码大的（通常需求大/候选少），候选更少
        // 这里做一个粗略估算：
        // 如果数字在 215104000-215104600 范围内（22个胚子），后几个候选更少
        if (num >= 215103130) {
            // 肽子 21-22：候选极少
            return 2;
        } else if (num >= 215103000) {
            // 肽子 11-20：候选较少
            return 3;
        } else {
            return 3;
        }
    }

    /**
     * 查找可以将胎胚分配到该机台的候选机台列表
     *
     * <p>候选条件：机台当前负荷 < 最大容量（即还有剩余硫化机台数）
     * <p>胎胚种类数未达上限（已有该胎胚或还有空余种类位）
     * <p>胎胚可以拆分：一个胎胚的硫化机台数可以分配到多台候选机台
     *
     * @param embryoCode       胎胚编码
     * @param machineStates    所有机台状态列表
     * @param forceKeepHistory 是否强制保留历史胎胚（未使用，为扩展预留）
     * @return 有剩余容量的机台列表
     */
    private List<MachineState> findCandidateMachinesForSplit(
            String embryoCode,
            List<MachineState> machineStates,
            boolean forceKeepHistory,
            boolean isFirstCall,
            String constrainedMachineCode) {
        
        List<MachineState> candidates = new ArrayList<>();
        
        for (MachineState state : machineStates) {
            // 约束机台过滤：如果该胎胚有约束机台，只能分配到指定机台
            if (constrainedMachineCode != null && !constrainedMachineCode.isEmpty()
                    && !state.getMachineCode().equals(constrainedMachineCode)) {
                continue;
            }
            // 容量已满，跳过
            if (state.getCurrentLoad() >= state.getMaxCapacity()) {
                log.trace("  [-满载] 机台 {}", state.getMachineCode());
                continue;
            }
            // 胎胚种类数已达上限，且当前胎胚是新种类，跳过
            boolean isNewType = !state.getAssignedEmbryos().stream()
                    .anyMatch(e -> e.getEmbryoCode().equals(embryoCode));
            if (isNewType && state.getCurrentTypes() >= state.getMaxTypes()) {
                log.trace("  [-种类满] 机台 {}", state.getMachineCode());
                continue;
            }
            candidates.add(state);
        }
        
        // 仅在候选为空或首次搜索时打印，避免重复噪音
        if (candidates.isEmpty() || isFirstCall) {
            String skipInfo = "";
            for (MachineState s : machineStates) {
                if (!candidates.contains(s)) {
                    if (s.getCurrentLoad() >= s.getMaxCapacity()) {
                        skipInfo += String.format("满载(%d/%d)/", s.getCurrentLoad(), s.getMaxCapacity());
                    } else {
                        boolean isNew = !s.getAssignedEmbryos().stream()
                                .anyMatch(e -> e.getEmbryoCode().equals(embryoCode));
                        if (isNew) {
                            skipInfo += String.format("种类满(%d/%d)/", s.getCurrentTypes(), s.getMaxTypes());
                        }
                    }
                }
            }
            if (!candidates.isEmpty()) {
                log.info("胎胚 {} 候选机台({}): [{}] | 跳过: {}",
                        embryoCode, candidates.size(),
                        candidates.stream().map(MachineState::getMachineCode).collect(Collectors.joining(",")),
                        skipInfo);
            }
        }
        
        return candidates;
    }

    /**
     * 排序候选机台（DFS用）
     *
     * <p>根据产能是否充足，使用不同排序策略：
     * <ul>
     *   <li><b>产能充足</b>：目标是均衡分配，侧重负荷均衡</li>
     *   <li><b>产能不足</b>：目标是尽量多排，侧重节省种类槽</li>
     * </ul>
     */
    private void sortCandidatesForDfs(
            List<MachineState> candidates,
            String embryoCode,
            boolean forceKeepHistory,
            boolean capacitySufficient,
            int loadDiffThreshold) {
        
        if (capacitySufficient) {
            // ===== 产能充足策略：负荷均衡优先，已有胎胚为次要考量 =====
            // 产能充足时目标是均衡分配，避免任务集中在少数机台
            candidates.sort((a, b) -> {
                // 优先级1：负荷少的优先（均衡分配，避免集中）
                int loadCompare = Integer.compare(a.getCurrentLoad(), b.getCurrentLoad());
                if (loadCompare != 0) {
                    return loadCompare;
                }
                
                // 优先级2：负荷相同时，已有胎胚优先（节省种类槽）
                boolean aAlreadyHas = a.getAssignedEmbryos().stream()
                        .anyMatch(e -> e.getEmbryoCode().equals(embryoCode));
                boolean bAlreadyHas = b.getAssignedEmbryos().stream()
                        .anyMatch(e -> e.getEmbryoCode().equals(embryoCode));
                if (aAlreadyHas && !bAlreadyHas) {
                    return -1;
                }
                if (!aAlreadyHas && bAlreadyHas) {
                    return 1;
                }
                
                // 优先级3：历史胎胚优先
                boolean aHasHistory = a.getHistoryEmbryos().contains(embryoCode);
                boolean bHasHistory = b.getHistoryEmbryos().contains(embryoCode);
                if (aHasHistory && !bHasHistory) {
                    return -1;
                }
                if (!aHasHistory && bHasHistory) {
                    return 1;
                }
                
                // 优先级4：同为未有时，剩余种类容量大的优先（保留稀缺种类槽）
                if (!aAlreadyHas) {
                    int aRemainingTypes = a.getMaxTypes() - a.getCurrentTypes();
                    int bRemainingTypes = b.getMaxTypes() - b.getCurrentTypes();
                    int remainingCompare = Integer.compare(bRemainingTypes, aRemainingTypes);
                    if (remainingCompare != 0) {
                        return remainingCompare;
                    }
                }
                
                // 优先级5：种类少的优先
                return Integer.compare(a.getCurrentTypes(), b.getCurrentTypes());
            });
        } else {
            // ===== 产能不足策略：侧重节省种类槽、尽量多排 =====
            candidates.sort((a, b) -> {
                // 优先级1：胎胚已在机台上绝对优先（节省种类槽，多排任务）
                boolean aAlreadyHas = a.getAssignedEmbryos().stream()
                        .anyMatch(e -> e.getEmbryoCode().equals(embryoCode));
                boolean bAlreadyHas = b.getAssignedEmbryos().stream()
                        .anyMatch(e -> e.getEmbryoCode().equals(embryoCode));
                if (aAlreadyHas && !bAlreadyHas) {
                    return -1;
                }
                if (!aAlreadyHas && bAlreadyHas) {
                    return 1;
                }
                
                // 优先级2：历史胎胚优先
                boolean aHasHistory = a.getHistoryEmbryos().contains(embryoCode);
                boolean bHasHistory = b.getHistoryEmbryos().contains(embryoCode);
                if (aHasHistory && !bHasHistory) {
                    return -1;
                }
                if (!aHasHistory && bHasHistory) {
                    return 1;
                }
                
                // 优先级3：剩余种类容量大的优先（保留灵活性）
                int aRemainingTypes = a.getMaxTypes() - a.getCurrentTypes();
                int bRemainingTypes = b.getMaxTypes() - b.getCurrentTypes();
                int remainingCompare = Integer.compare(bRemainingTypes, aRemainingTypes);
                if (remainingCompare != 0) {
                    return remainingCompare;
                }
                
                // 优先级4：负荷少的优先
                int loadCompare = Integer.compare(a.getCurrentLoad(), b.getCurrentLoad());
                if (loadCompare != 0) {
                    return loadCompare;
                }
                
                // 优先级5：种类少的优先
                return Integer.compare(a.getCurrentTypes(), b.getCurrentTypes());
            });
        }
    }

    /**
     * 深拷贝所有机台的分配状态
     *
     * <p>用于在 DFS 搜索过程中保存当前最优解。
     * 每个 MachineState 中的 assignedEmbryos 列表会被完整复制。
     *
     * @param machineStates 所有机台状态
     * @return machineCode → EmbryoAssignment列表 的深拷贝
     */
    /**
     * 深拷贝机台分配状态（按机台索引存储，避免重复编码覆盖）
     */
    private List<List<EmbryoAssignment>> copyAssignments(List<MachineState> machineStates) {
        List<List<EmbryoAssignment>> copy = new ArrayList<>();
        for (MachineState state : machineStates) {
            copy.add(new ArrayList<>(state.getAssignedEmbryos()));
        }
        return copy;
    }

    /**
     * 提取机台编码列表
     */
    private List<String> copyMachineCodes(List<MachineState> machineStates) {
        List<String> codes = new ArrayList<>();
        for (MachineState state : machineStates) {
            codes.add(state.getMachineCode());
        }
        return codes;
    }

    /**
     * 计算当前分配方案的均衡分数
     *
     * <p>分数越小越均衡。公式：score = 负荷差额 × 10 + 种类差额 × 100
     * <ul>
     *   <li>负荷差额 = max(各机台硫化机台数) − min(各机台硫化机台数)</li>
     *   <li>种类差额 = max(各机台胎胚种类数) − min(各机台胎胚种类数)</li>
     * </ul>
     * 只统计负荷>0 或种类>0 的机台。
     *
     * @param machineStates 所有机台状态
     * @return 均衡分数
     */
    /**
     * 判断当前分配结果是否满足均衡阈值
     *
     * <p>均衡条件：负荷差距 ≤ loadDiffThreshold 且 种类差距 ≤ typeDiffThreshold
     *
     * @param machineStates     所有机台状态
     * @param typeDiffThreshold 种类数允许差额
     * @param loadDiffThreshold 负荷允许差额
     * @return true=均衡，false=不均衡
     */
    private boolean isBalanced(List<MachineState> machineStates, int typeDiffThreshold, int loadDiffThreshold) {
        if (machineStates.isEmpty()) {
            return true;
        }
        int maxLoad = 0, minLoad = Integer.MAX_VALUE;
        int maxTypes = 0, minTypes = Integer.MAX_VALUE;
        for (MachineState state : machineStates) {
            maxLoad = Math.max(maxLoad, state.getCurrentLoad());
            minLoad = Math.min(minLoad, state.getCurrentLoad());
            maxTypes = Math.max(maxTypes, state.getCurrentTypes());
            minTypes = Math.min(minTypes, state.getCurrentTypes());
        }
        int loadGap = maxLoad - minLoad;
        int typeGap = maxTypes - minTypes;
        return loadGap <= loadDiffThreshold && typeGap <= typeDiffThreshold;
    }

    /**
     * 计算均衡分数（仅在同等均衡等级内用于比较优劣）
     *
     * <p>分数 = 负荷差距 * 10 + 种类差距 * 100，越小越优。
     *
     * @param machineStates 所有机台状态
     * @return 均衡分数
     */
    private int calculateBalancingScore(List<MachineState> machineStates) {
        if (machineStates.isEmpty()) {
            return 0;
        }
        
        int maxLoad = 0, minLoad = Integer.MAX_VALUE;
        int maxTypes = 0, minTypes = Integer.MAX_VALUE;
        
        for (MachineState state : machineStates) {
            maxLoad = Math.max(maxLoad, state.getCurrentLoad());
            minLoad = Math.min(minLoad, state.getCurrentLoad());
            maxTypes = Math.max(maxTypes, state.getCurrentTypes());
            minTypes = Math.min(minTypes, state.getCurrentTypes());
        }
        
        int loadGap = maxLoad - minLoad;
        int typeGap = maxTypes - minTypes;
        
        return loadGap * 10 + typeGap * 100;
    }

    /**
     * 将 DFS 最优解的分配映射转换为 BalancingResult 结构
     *
     * <p>遍历 assignments 中每台机台及其分配的胎胚列表，
     * 重建 MachineAssignment 对象，计算该机台的总硫化机台数和总胎胚种类数。
     *
     * @param assignments  machineCode → EmbryoAssignment列表
     * @param machineStates 所有机台状态（用于获取未分配机台的信息）
     * @param tasks        所有任务（用于获取未分配任务的信息）
     * @return 转换后的 BalancingResult
     */
    private BalancingResult convertDfsResultToBalancingResult(
            List<List<EmbryoAssignment>> assignments,
            List<String> machineCodes,
            List<MachineState> machineStates,
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks) {
        
        BalancingResult result = new BalancingResult();
        result.setAssignments(new ArrayList<>());
        
        for (int i = 0; i < assignments.size(); i++) {
            List<EmbryoAssignment> embryoAssignments = assignments.get(i);
            if (embryoAssignments.isEmpty()) {
                continue;
            }
            
            MachineAssignment assignment = new MachineAssignment();
            assignment.setMachineCode(machineCodes.get(i));
            assignment.setEmbryoAssignments(embryoAssignments);
            result.getAssignments().add(assignment);
        }
        
        return result;
    }

    /**
     * 从 machineStates 构建分配结果（保底预留后 DFS 无剩余任务的场景）
     */
    private BalancingResult buildResultFromMachineStates(List<MachineState> machineStates) {
        BalancingResult result = new BalancingResult();
        result.setAssignments(new ArrayList<>());

        for (MachineState state : machineStates) {
            if (state.getAssignedEmbryos().isEmpty()) {
                continue;
            }
            MachineAssignment assignment = new MachineAssignment();
            assignment.setMachineCode(state.getMachineCode());
            assignment.setEmbryoAssignments(new ArrayList<>(state.getAssignedEmbryos()));
            result.getAssignments().add(assignment);
        }
        return result;
    }

    /**
     * 记录分配结果日志
     */
    private void logAllocationResult(BalancingResult result, List<MachineState> machineStates,
                                     List<CoreScheduleAlgorithmService.DailyEmbryoTask> originalTasks) {
        log.info("均衡分配结果：");
        
        int maxLoad = 0, minLoad = Integer.MAX_VALUE;
        int maxTypes = 0, minTypes = Integer.MAX_VALUE;
        
        // 统计已分配的胎胚数量
        Map<String, Integer> assignedQtyMap = new LinkedHashMap<>();
        
        for (MachineAssignment assignment : result.getAssignments()) {
            // 合并相同胚子代码的条目
            Map<String, Integer> embryoQtyMap = new LinkedHashMap<>();
            for (EmbryoAssignment e : assignment.getEmbryoAssignments()) {
                embryoQtyMap.merge(e.getEmbryoCode(), e.getAssignedQty(), Integer::sum);
                assignedQtyMap.merge(e.getEmbryoCode(), e.getAssignedQty(), Integer::sum);
            }
            List<String> embryos = embryoQtyMap.entrySet().stream()
                    .map(e -> e.getKey() + "(" + e.getValue() + ")")
                    .collect(Collectors.toList());
            log.info("  机台 {}: {}", assignment.getMachineCode(), embryos);
            
            // 从 result 中计算均衡指标
            int load = assignment.getEmbryoAssignments().stream()
                    .mapToInt(EmbryoAssignment::getAssignedQty).sum();
            int types = (int) assignment.getEmbryoAssignments().stream()
                    .map(EmbryoAssignment::getEmbryoCode).distinct().count();
            
            maxLoad = Math.max(maxLoad, load);
            minLoad = Math.min(minLoad, load);
            maxTypes = Math.max(maxTypes, types);
            minTypes = Math.min(minTypes, types);
        }
        
        // 如果没有分配结果，避免打印错误指标
        if (minLoad == Integer.MAX_VALUE) {
            log.info("均衡指标：无有效分配");
        } else {
            log.info("均衡指标：负荷差距={}, 种类差距={}", 
                    maxLoad - minLoad, maxTypes - minTypes);
        }
        
        // 打印未排上的胎胚
        if (originalTasks != null && !originalTasks.isEmpty()) {
            Map<String, Integer> demandByEmbryo = new LinkedHashMap<>();
            for (CoreScheduleAlgorithmService.DailyEmbryoTask t : originalTasks) {
                demandByEmbryo.merge(t.getEmbryoCode(),
                        t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 1, Integer::sum);
            }
            List<String> unassignedItems = new ArrayList<>();
            for (Map.Entry<String, Integer> e : demandByEmbryo.entrySet()) {
                int assigned = assignedQtyMap.getOrDefault(e.getKey(), 0);
                if (assigned < e.getValue()) {
                    unassignedItems.add(e.getKey() + "(" + (e.getValue() - assigned) + ")");
                }
            }
            if (!unassignedItems.isEmpty()) {
                log.warn("未排上的胎胚：{}", unassignedItems);
            }
        }
    }

    // ==================== 配置获取方法 ====================

    /**
     * 获取种类数允许差额配置
     */
    public int getTypeDiffThreshold(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_TYPE_DIFF_THRESHOLD);
            if (config != null && config.getParamValue() != null) {
                try {
                    return Integer.parseInt(config.getParamValue());
                } catch (NumberFormatException e) {
                    log.warn("解析种类差额阈值配置失败: {}", config.getParamValue());
                }
            }
        }
        return DEFAULT_TYPE_DIFF_THRESHOLD;
    }

    /**
     * 获取负荷允许差额配置
     */
    public int getLoadDiffThreshold(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_LOAD_DIFF_THRESHOLD);
            if (config != null && config.getParamValue() != null) {
                try {
                    return Integer.parseInt(config.getParamValue());
                } catch (NumberFormatException e) {
                    log.warn("解析负荷差额阈值配置失败: {}", config.getParamValue());
                }
            }
        }
        return DEFAULT_LOAD_DIFF_THRESHOLD;
    }

    /**
     * 获取是否强制保留历史任务配置
     */
    public boolean getForceKeepHistoryConfig(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_FORCE_KEEP_HISTORY);
            if (config != null && config.getParamValue() != null) {
                return "1".equals(config.getParamValue()) || "true".equalsIgnoreCase(config.getParamValue());
            }
        }
        return false;
    }

    // ==================== 班次间均衡方法 ====================

    /**
     * 班次间生产量均衡
     *
     * <p>目标：使同一结构（structureName）下，各机台同班次的总车数趋于均衡。
     * 硫化机台数最多的胎胚（绑定胎胚）决定该结构的排产节奏，其他胎胚向其靠拢。
     *
     * <p>不参与均衡的任务类型：
     * <ul>
     *   <li>试制任务：独立排产，不与其他任务混合</li>
     *   <li>收尾任务：仅在首/末班生产，不参与班次比例调整</li>
     * </ul>
     *
     * <p>均衡执行步骤：
     * <ul>
     *   <li>Step1: 按 structureName 分组</li>
     *   <li>Step2: 在每个结构组内，按 (machineCode, embryoCode) 分组</li>
     *   <li>Step3: 在每个 (machine, embryo) 组合内，找硫化机台数最多的胎胚作为绑定胎胚</li>
     *   <li>Step4: 对每个绑定胎胚执行"排序+循环右移"均衡（例：76,86,76 → 86,76,76）</li>
     *   <li>Step5: 汇总每个班次的总车数，检查是否均衡（max-min ≤ 1）</li>
     *   <li>Step6: 若不均衡，执行跨机台调整，按比例分摊差额</li>
     *   <li>Step7: 更新所有 ShiftProductionResult 的 carsForShift 和 quantity</li>
     * </ul>
     *
     * @param results 排产结果列表（包含所有任务的班次精排结果，会被直接修改）
     * @param context 排程上下文
     * @return 均衡后的结果列表（与输入 results 相同引用）
     */
    public List<ShiftScheduleService.ShiftProductionResult> balanceShiftQuantities(
            List<ShiftScheduleService.ShiftProductionResult> results,
            ScheduleContextVo context) {

        log.info("====== 班次间均衡开始 ======");
        
        if (results == null || results.isEmpty()) {
            log.warn("班次均衡输入为空，跳过");
            return results;
        }

        log.info("班次均衡输入: 总结果数={}", results.size());
        // 打印均衡前每个结果的详情
        for (ShiftScheduleService.ShiftProductionResult r : results) {
            log.info("  均衡前: 机台={}, 胎胚={}, 班次={}, 车数={}, 产量={}, 试制={}, 收尾={}",
                    r.getMachineCode(), r.getEmbryoCode(), r.getShiftCode(),
                    r.getCarsForShift(), r.getQuantity(),
                    r.getIsTrialTask(), r.getIsEndingTask());
        }

        // Step1: 按 structureName 分组
        Map<String, List<ShiftScheduleService.ShiftProductionResult>> byStructure = results.stream()
                .collect(Collectors.groupingBy(r -> {
                    CoreScheduleAlgorithmService.DailyEmbryoTask task = r.getSourceTask();
                    return task != null ? task.getStructureName() : r.getStructureName();
                }));

        log.info("按结构分组: 共 {} 个结构组", byStructure.size());
        for (Map.Entry<String, List<ShiftScheduleService.ShiftProductionResult>> e : byStructure.entrySet()) {
            log.info("  结构 {}: {} 条结果", e.getKey(), e.getValue().size());
        }

        for (Map.Entry<String, List<ShiftScheduleService.ShiftProductionResult>> entry : byStructure.entrySet()) {
            String currentStructure = entry.getKey();
            List<ShiftScheduleService.ShiftProductionResult> group = entry.getValue();

            log.info("--- 处理结构 {} (共{}条) ---", currentStructure, group.size());

            // 过滤出普通任务（排除试制、收尾）
            List<ShiftScheduleService.ShiftProductionResult> regularTasks = group.stream()
                    .filter(r -> !Boolean.TRUE.equals(r.getIsTrialTask())
                            && !Boolean.TRUE.equals(r.getIsEndingTask()))
                    .collect(Collectors.toList());

            log.info("结构 {} 普通任务数={}, 试制/收尾任务数={}",
                    currentStructure, regularTasks.size(), group.size() - regularTasks.size());

            if (regularTasks.size() < 2) {
                log.info("结构 {} 普通任务不足2条，跳过班次均衡", currentStructure);
                continue;
            }

            // Step2: 按 machineCode + embryoCode 分组，每组内找绑定胎胚（硫化机台数最多）
            Map<String, List<ShiftScheduleService.ShiftProductionResult>> byMachineEmbryo = regularTasks.stream()
                    .collect(Collectors.groupingBy(r -> r.getMachineCode() + "|" + r.getEmbryoCode()));

            log.info("结构 {} 按(机台+胎胚)分组: {} 组", currentStructure, byMachineEmbryo.size());
            for (Map.Entry<String, List<ShiftScheduleService.ShiftProductionResult>> me : byMachineEmbryo.entrySet()) {
                log.info("  组 {}: {} 条班次结果", me.getKey(), me.getValue().size());
            }

            // 收集所有绑定胎胚的班次结果
            List<BindingEmbryoShifts> bindingList = new ArrayList<>();

            for (Map.Entry<String, List<ShiftScheduleService.ShiftProductionResult>> meEntry : byMachineEmbryo.entrySet()) {
                List<ShiftScheduleService.ShiftProductionResult> meResults = meEntry.getValue();

                // 该 (machine, embryo) 组合内的绑定胎胚：取硫化机台数最多的
                ShiftScheduleService.ShiftProductionResult binding = meResults.stream()
                        .max(Comparator.comparingInt(r -> {
                            CoreScheduleAlgorithmService.DailyEmbryoTask task = r.getSourceTask();
                            return task != null && task.getVulcanizeMachineCount() != null
                                    ? task.getVulcanizeMachineCount() : 0;
                        }))
                        .orElse(null);

                if (binding == null) {
                    continue;
                }

                log.info("  组 {} 绑定胎胚: embryoCode={}, vulcanizeMachineCount={}",
                        meEntry.getKey(), binding.getEmbryoCode(),
                        binding.getSourceTask() != null ? binding.getSourceTask().getVulcanizeMachineCount() : "null");

                // 提取该绑定胎胚的三个班次（按班次编码排序，确保顺序固定为夜-早-中）
                List<ShiftScheduleService.ShiftProductionResult> bindingShifts = meResults.stream()
                        .filter(r -> r.getEmbryoCode().equals(binding.getEmbryoCode()))
                        .sorted(Comparator.comparing(ShiftScheduleService.ShiftProductionResult::getShiftCode))
                        .collect(Collectors.toList());

                if (bindingShifts.size() == 3) {
                    bindingList.add(new BindingEmbryoShifts(bindingShifts));
                    log.info("  绑定胎胚 {} 三个班次车次: 班次1={}({}), 班次2={}({}), 班次3={}({})",
                            binding.getEmbryoCode(),
                            bindingShifts.get(0).getCarsForShift(), bindingShifts.get(0).getShiftCode(),
                            bindingShifts.get(1).getCarsForShift(), bindingShifts.get(1).getShiftCode(),
                            bindingShifts.get(2).getCarsForShift(), bindingShifts.get(2).getShiftCode());
                } else {
                    log.warn("  绑定胎胚 {} 班次数={}, 不足3个，跳过",
                            binding.getEmbryoCode(), bindingShifts.size());
                }
            }

            if (bindingList.isEmpty()) {
                log.info("结构 {} 无有效绑定胎胚（三班齐全），跳过班次均衡", currentStructure);
                continue;
            }

            log.info("结构 {} 共找到 {} 个绑定胎胚，开始排序+循环右移均衡", currentStructure, bindingList.size());

            // Step3: 对每台绑定胎胚执行排序+循环右移，并汇总各班次总量
            int[] totalByShift = new int[3]; // 汇总：夜、早、中

            int bindingIdx = 0;
            for (BindingEmbryoShifts binding : bindingList) {
                int[] cars = binding.getCars();
                int total = cars[0] + cars[1] + cars[2];

                log.info("  绑定胎胚 #{}: 原始车次=[夜={}, 早={}, 中={}], 总计={}",
                        bindingIdx, cars[0], cars[1], cars[2], total);

                // 均衡：排序后循环右移1位
                int[] sorted = cars.clone();
                Arrays.sort(sorted);
                // sorted = [min, mid, max]
                // 循环右移：[max, min, mid]
                int[] balanced = new int[]{sorted[2], sorted[0], sorted[1]};

                log.info("  绑定胎胚 #{}: 排序后=[{}, {}, {}], 均衡后=[夜={}, 早={}, 中={}]",
                        bindingIdx, sorted[0], sorted[1], sorted[2],
                        balanced[0], balanced[1], balanced[2]);

                binding.applyBalanced(balanced);

                // 汇总
                for (int i = 0; i < 3; i++) {
                    totalByShift[i] += balanced[i];
                }
                bindingIdx++;
            }

            log.info("结构 {} 汇总班次车次: 夜={}, 早={}, 中={}, 总计={}",
                    currentStructure, totalByShift[0], totalByShift[1], totalByShift[2],
                    totalByShift[0] + totalByShift[1] + totalByShift[2]);

            // Step4: 检查汇总后是否均衡（max-min > 1 则需要跨机台调整）
            int maxShift = Math.max(Math.max(totalByShift[0], totalByShift[1]), totalByShift[2]);
            int minShift = Math.min(Math.min(totalByShift[0], totalByShift[1]), totalByShift[2]);
            log.info("结构 {} 均衡检查: max={}, min={}, 差额={}, 阈值=1",
                    currentStructure, maxShift, minShift, maxShift - minShift);
            if (maxShift - minShift > 1) {
                log.info("结构 {} 需要跨机台调整均衡", currentStructure);
                // 跨机台调整：计算每台绑定胎胚各班次占总班次数的比例，按比例分摊调整量
                int totalCars = totalByShift[0] + totalByShift[1] + totalByShift[2];
                if (totalCars > 0) {
                    // 目标：max-min <= 1 的均衡分布
                    int base = totalCars / 3;
                    int remainder = totalCars % 3;
                    // remainder=0 → [base, base, base]
                    // remainder=1 → [base+1, base, base] 或 [base, base+1, base]
                    // remainder=2 → [base+1, base+1, base]
                    int[] targetTotal = new int[]{base, base, base};
                    if (remainder == 1) {
                        targetTotal[1] = base + 1; // 中班多1
                    } else if (remainder == 2) {
                        targetTotal[0] = base + 1;
                        targetTotal[1] = base + 1; // 夜和中多1
                    }

                    log.info("  跨机台调整: totalCars={}, base={}, remainder={}, 目标=[夜={}, 早={}, 中={}]",
                            totalCars, base, remainder, targetTotal[0], targetTotal[1], targetTotal[2]);

                    // 计算差额
                    int[] diff = new int[3];
                    for (int i = 0; i < 3; i++) {
                        diff[i] = targetTotal[i] - totalByShift[i];
                    }
                    log.info("  跨机台调整: 当前=[夜={}, 早={}, 中={}], 差额=[夜={}, 早={}, 中={}]",
                            totalByShift[0], totalByShift[1], totalByShift[2],
                            diff[0], diff[1], diff[2]);

                    // 按各班次差额占总差额的比例，从各绑定胎胚的对应班次中调整
                    // 正差额表示该班次多了需要减，负差额表示少了需要加
                    int totalDiff = Math.abs(diff[0]) + Math.abs(diff[1]) + Math.abs(diff[2]);
                    if (totalDiff > 0) {
                        int adjustIdx = 0;
                        for (BindingEmbryoShifts binding : bindingList) {
                            int[] currentCars = binding.getCars();
                            int bindingTotal = currentCars[0] + currentCars[1] + currentCars[2];

                            log.info("    调整绑定胎胚 #{}: 当前车次=[夜={}, 早={}, 中={}], 总计={}",
                                    adjustIdx, currentCars[0], currentCars[1], currentCars[2], bindingTotal);

                            for (int i = 0; i < 3; i++) {
                                if (diff[i] == 0) {
                                    continue;
                                }
                                // 按该班次差额占总差额的比例分摊调整量
                                int absDiff = Math.abs(diff[i]);
                                int adjust = (int) Math.round((double) absDiff / totalDiff * bindingTotal / 3.0);
                                adjust = Math.max(1, adjust); // 至少调整1车

                                log.info("      班次{}: 差额={}, 分摊调整量={}, 当前车次={}",
                                        i, diff[i], adjust, currentCars[i]);

                                int oldCars = currentCars[i];
                                if (diff[i] > 0) {
                                    // 该班次少了，需要加车（从其他班次匀）
                                    // 找到有多余的班次匀过来
                                    for (int j = 0; j < 3; j++) {
                                        if (j == i || currentCars[j] <= binding.getMinCars() + 1) {
                                            continue;
                                        }
                                        int canGive = currentCars[j] - binding.getMinCars() - 1;
                                        if (canGive > 0) {
                                            int give = Math.min(canGive, adjust);
                                            currentCars[j] -= give;
                                            currentCars[i] += give;
                                            adjust -= give;
                                        }
                                        if (adjust <= 0) {
                                            break;
                                        }
                                    }
                                } else {
                                    // 该班次多了，需要减车（匀到其他班次）
                                    int canReduce = currentCars[i] - binding.getMinCars() - 1;
                                    int reduce = Math.min(canReduce, absDiff);
                                    currentCars[i] -= reduce;
                                    // 加到其他班次
                                    for (int j = 0; j < 3; j++) {
                                        if (j == i) {
                                            continue;
                                        }
                                        currentCars[j] += reduce / 2;
                                        break;
                                    }
                                }
                            }

                            // 更新结果
                            binding.updateResults();
                            log.info("    调整后绑定胎胚 #{}: 车次=[夜={}, 早={}, 中={}]",
                                    adjustIdx, currentCars[0], currentCars[1], currentCars[2]);
                            adjustIdx++;
                        }
                    }
                }
            }
        }

        // 打印均衡后每个结果的详情
        log.info("====== 班次间均衡后结果 ======");
        for (ShiftScheduleService.ShiftProductionResult r : results) {
            log.info("  均衡后: 机台={}, 胎胚={}, 班次={}, 车数={}, 产量={}",
                    r.getMachineCode(), r.getEmbryoCode(), r.getShiftCode(),
                    r.getCarsForShift(), r.getQuantity());
        }

        return results;
    }

    /**
     * 绑定胎胚三个班次结果的封装，内部记录原始引用，均衡后可直接写回
     */
    private static class BindingEmbryoShifts {
        private final List<ShiftScheduleService.ShiftProductionResult> shifts;
        private int[] cars = new int[3];
        private int minCars;

        BindingEmbryoShifts(List<ShiftScheduleService.ShiftProductionResult> shifts) {
            this.shifts = shifts;
            this.cars[0] = shifts.get(0).getCarsForShift() != null ? shifts.get(0).getCarsForShift() : 0;
            this.cars[1] = shifts.get(1).getCarsForShift() != null ? shifts.get(1).getCarsForShift() : 0;
            this.cars[2] = shifts.get(2).getCarsForShift() != null ? shifts.get(2).getCarsForShift() : 0;
            this.minCars = Math.min(Math.min(cars[0], cars[1]), cars[2]);
            log.debug("BindingEmbryoShifts初始化: 胎胚={}, 班次=[{},{},{}], minCars={}",
                    shifts.get(0).getEmbryoCode(), cars[0], cars[1], cars[2], minCars);
        }

        int[] getCars() {
            return cars;
        }

        int getMinCars() {
            return minCars;
        }

        void applyBalanced(int[] balanced) {
            for (int i = 0; i < 3; i++) {
                this.cars[i] = balanced[i];
            }
            this.minCars = Math.min(Math.min(cars[0], cars[1]), cars[2]);
        }

        void updateResults() {
            for (int i = 0; i < 3; i++) {
                ShiftScheduleService.ShiftProductionResult r = shifts.get(i);
                int tripCapacity = r.getTripCapacity() != null ? r.getTripCapacity() : 1;
                r.setCarsForShift(cars[i]);
                r.setQuantity(cars[i] * tripCapacity);
            }
        }
    }

    // ==================== 内部类 ====================

    /**
     * 机台状态（分配过程中使用）
     */
    @lombok.Data
    public static class MachineState {
        private String machineCode;
        private int maxCapacity;
        private int maxTypes;
        private int currentLoad;
        private int currentTypes;
        private List<EmbryoAssignment> assignedEmbryos;
        private Set<String> historyEmbryos;
    }

    /**
     * DFS搜索结果记录
     */
    @lombok.Data
    private static class DfsSearchResult {
        int bestScore;
        int bestAssignedCount;  // 最优解的已分配数量（完整度优先于均衡分数）
        boolean bestIsBalanced; // 最优解是否满足均衡阈值
        List<List<EmbryoAssignment>> bestAssignments; // 按机台索引存储，避免重复编码覆盖
        List<String> bestMachineCodes; // 对应的机台编码列表
        int searchCount;  // DFS搜索次数
        int pruneCount;   // 剪枝次数
        int callCount;    // findCandidate调用次数（用于日志控制）
    }

    /**
     * 均衡分配结果
     */
    @lombok.Data
    public static class BalancingResult {
        private List<MachineAssignment> assignments;
    }

    /**
     * 机台分配
     */
    @lombok.Data
    public static class MachineAssignment {
        private String machineCode;
        private List<EmbryoAssignment> embryoAssignments;
    }

    /**
     * 胎胚分配
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class EmbryoAssignment {
        private String embryoCode;
        private CoreScheduleAlgorithmService.DailyEmbryoTask task;
        /** 分配数量，单位：硫化机台数 */
        private int assignedQty;
    }
}
