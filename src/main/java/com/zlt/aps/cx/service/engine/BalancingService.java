package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 均衡分配服务
 * 
 * <p>负责胎胚到机台的均衡分配，使用 DFS + 剪枝算法：
 * <ul>
 *   <li>目标：胎胚种类数均衡，硫化机台数配比均衡</li>
 *   <li>约束：机台最大硫化机数上限、胎胚种类数上限</li>
 *   <li>策略：深度优先搜索 + 剪枝，超过均衡阈值的分支直接舍弃</li>
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

    /** 参数编码：强制保留历史任务 */
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
                maxLh = machine.getLhMachineMaxQty() != null ? machine.getLhMachineMaxQty() : 10;
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
            for (MdmStructureLhRatio ratio : ratios) {
                String key = ratio.getCxMachineTypeCode() + "_" + ratio.getStructureName();
                if (ratio.getMaxEmbryoQty() != null) {
                    typeStructureMap.put(key, ratio.getMaxEmbryoQty());
                }
            }
        }

        for (MdmMoldingMachine machine : machines) {
            String machineCode = machine.getCxMachineCode();
            String machineType = machineTypeMap.get(machineCode);

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
            totalCapacity += (maxLh != null ? maxLh : 10);
        }

        log.info("均衡分配计算：总需求（硫化机台数）={}, 总产能（各机台最大硫化机数之和）={}, 机台数={}",
                totalDemand, totalCapacity, availableMachines.size());

        // Step 4: 按硫化机台数从大到小排序胎胚
        List<CoreScheduleAlgorithmService.DailyEmbryoTask> sortedTasks = tasks.stream()
                .sorted((a, b) -> {
                    int countA = a.getVulcanizeMachineCount() != null ? a.getVulcanizeMachineCount() : 0;
                    int countB = b.getVulcanizeMachineCount() != null ? b.getVulcanizeMachineCount() : 0;
                    return Integer.compare(countB, countA);
                })
                .collect(Collectors.toList());

        // Step 5: 初始化机台状态（每台机台使用自己的最大硫化机数和最大胎胚种类数）
        List<MachineState> machineStates = new ArrayList<>();
        for (MpCxCapacityConfiguration config : availableMachines) {
            MachineState state = new MachineState();
            state.setMachineCode(config.getCxMachineCode());

            // 从映射中获取该机台的最大硫化机数
            Integer maxLh = machineMaxLhMap.get(config.getCxMachineCode());
            state.setMaxCapacity(maxLh != null ? maxLh : 10);

            // 从映射中获取该机台的最大胎胚种类数
            Integer maxTypes = machineMaxEmbryoTypesMap.get(config.getCxMachineCode());
            state.setMaxTypes(maxTypes != null ? maxTypes : DEFAULT_MAX_TYPES_PER_MACHINE);

            state.setCurrentLoad(0);
            state.setCurrentTypes(0);
            state.setAssignedEmbryos(new ArrayList<>());

            Set<String> historyEmbryos = machineHistoryMap.get(config.getCxMachineCode());
            state.setHistoryEmbryos(historyEmbryos != null ? historyEmbryos : new HashSet<>());

            machineStates.add(state);
        }

        // Step 6: 如果强制保留历史任务，先进行保底预留
        if (forceKeepHistory) {
            reservedHistoryTasks(sortedTasks, machineStates, context);
        }

        // 产能不足检查：如果总产能 < 总需求，直接使用贪心算法
        int effectiveCapacity = totalCapacity;
        if (effectiveCapacity < totalDemand) {
            log.warn("产能不足（产能={}, 需求={}），跳过DFS直接使用贪心算法", effectiveCapacity, totalDemand);
            BalancingResult result = greedyAssignFallback(sortedTasks, machineStates, forceKeepHistory,
                    typeDiffThreshold, loadDiffThreshold);
            logAllocationResult(result, machineStates);
            return result;
        }

        // Step 7: DFS + 剪枝搜索最优方案
        DfsSearchResult searchResult = new DfsSearchResult();
        searchResult.bestScore = Integer.MAX_VALUE;
        searchResult.bestAssignments = null;
        searchResult.searchCount = 0;
        searchResult.pruneCount = 0;

        List<CoreScheduleAlgorithmService.DailyEmbryoTask> remainingTasks = getRemainingTasks(sortedTasks);
        
        // 初始化：从第一个任务开始，remainingCount 为第一个任务的硫化机台数
        int initialRemainingCount = remainingTasks.isEmpty() ? 0 
                : (remainingTasks.get(0).getVulcanizeMachineCount() != null 
                   ? remainingTasks.get(0).getVulcanizeMachineCount() : 0);
        
        dfsAssign(remainingTasks, 0, initialRemainingCount, machineStates, forceKeepHistory,
                typeDiffThreshold, loadDiffThreshold, searchResult);

        log.info("DFS搜索统计：总搜索次数={}, 剪枝次数={}, 最优分数={}",
                searchResult.searchCount, searchResult.pruneCount, searchResult.bestScore);

        // Step 8: 构建结果
        BalancingResult result;
        if (searchResult.bestAssignments != null) {
            // 验证解是否完整
            int totalAssigned = 0;
            for (Map.Entry<String, List<EmbryoAssignment>> entry : searchResult.bestAssignments.entrySet()) {
                totalAssigned += entry.getValue().stream().mapToInt(EmbryoAssignment::getAssignedQty).sum();
            }
            
            if (totalAssigned == totalDemand) {
                result = convertDfsResultToBalancingResult(searchResult.bestAssignments, machineStates, sortedTasks);
                log.info("找到满足均衡条件的完整方案，已分配 {} 台硫化机", totalAssigned);
            } else {
                log.warn("DFS找到的解不完整（已分配 {}/{}），使用贪心算法作为兜底", totalAssigned, totalDemand);
                result = greedyAssignFallback(sortedTasks, machineStates, forceKeepHistory,
                        typeDiffThreshold, loadDiffThreshold);
            }
        } else {
            log.warn("未找到满足均衡条件的方案，使用贪心算法作为兜底");
            result = greedyAssignFallback(sortedTasks, machineStates, forceKeepHistory,
                    typeDiffThreshold, loadDiffThreshold);
        }

        logAllocationResult(result, machineStates);
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
        
        // 构建胎胚编码 -> 任务 的映射
        Map<String, CoreScheduleAlgorithmService.DailyEmbryoTask> taskMap = tasks.stream()
                .collect(Collectors.toMap(
                        CoreScheduleAlgorithmService.DailyEmbryoTask::getMaterialCode,
                        t -> t,
                        (a, b) -> a));
        
        int totalReserved = 0;
        
        for (MachineState state : machineStates) {
            Set<String> historyEmbryos = state.getHistoryEmbryos();
            if (historyEmbryos == null || historyEmbryos.isEmpty()) {
                continue;
            }
            
            for (String embryoCode : historyEmbryos) {
                CoreScheduleAlgorithmService.DailyEmbryoTask task = taskMap.get(embryoCode);
                if (task == null) {
                    continue;
                }
                
                int remainingDemand = task.getVulcanizeMachineCount() != null 
                        ? task.getVulcanizeMachineCount() : 0;
                
                if (remainingDemand <= 0) {
                    continue;
                }
                
                if (state.getCurrentLoad() >= state.getMaxCapacity()) {
                    log.warn("机台 {} 容量已满，无法保底预留胎胚 {}", 
                            state.getMachineCode(), embryoCode);
                    continue;
                }
                
                // 保底预留1个硫化机台数
                int reservedCount = 1;
                
                state.getAssignedEmbryos().add(new EmbryoAssignment(embryoCode, task, reservedCount));
                state.setCurrentLoad(state.getCurrentLoad() + reservedCount);
                state.setCurrentTypes(state.getCurrentTypes() + 1);
                
                task.setVulcanizeMachineCount(remainingDemand - reservedCount);
                
                totalReserved++;
                log.debug("机台 {} 保底预留胎胚 {} 共 {} 个硫化机", 
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
            DfsSearchResult searchResult) {

        searchResult.searchCount++;

        // 安全限制：搜索次数超过 100 万次后停止（防止极端情况卡死）
        if (searchResult.searchCount > 1000000) {
            return;
        }

        // 终止条件：所有任务已分配
        if (taskIndex >= tasks.size()) {
            // 检查是否所有任务都已分配完毕
            int totalAssigned = machineStates.stream()
                    .mapToInt(MachineState::getCurrentLoad).sum();
            int totalRequired = tasks.stream()
                    .mapToInt(t -> t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 0)
                    .sum();
            
            // 只有完整解才更新最优解
            if (totalAssigned == totalRequired) {
                int score = calculateBalancingScore(machineStates);
                
                if (score < searchResult.bestScore) {
                    searchResult.bestScore = score;
                    searchResult.bestAssignments = copyAssignments(machineStates);
                }
            }
            return;
        }
        
        CoreScheduleAlgorithmService.DailyEmbryoTask task = tasks.get(taskIndex);
        String embryoCode = task.getEmbryoCode();
        
        // 如果当前胎胚还有剩余未分配的硫化机数
        if (remainingCount > 0) {
            // 找出可以分配的候选机台（只要有剩余容量即可）
            List<MachineState> candidates = findCandidateMachinesForSplit(
                    embryoCode, machineStates, forceKeepHistory);
            
            if (candidates.isEmpty()) {
                // 没有可用机台，此分支无效
                return;
            }
            
            // 按优先级排序候选机台
            sortCandidatesForDfs(candidates, embryoCode, forceKeepHistory);
            
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
                    
                    // 计算分配后各机台的种类数
                    int minTypes = Integer.MAX_VALUE;
                    int maxTypes = 0;
                    for (MachineState state : machineStates) {
                        int types = state.getCurrentTypes();
                        if (state == candidate) {
                            types = newTypes;
                        }
                        if (types > 0) {
                            minTypes = Math.min(minTypes, types);
                            maxTypes = Math.max(maxTypes, types);
                        }
                    }
                    
                    // 剪枝条件：种类数差额超过阈值
                    if (minTypes != Integer.MAX_VALUE && maxTypes - minTypes > typeDiffThreshold) {
                        searchResult.pruneCount++;
                        continue;
                    }
                    
                    // 检查2：是否会导致负荷差额超过阈值
                    int newLoad = candidate.getCurrentLoad() + assignQty;
                    
                    int minLoad = Integer.MAX_VALUE;
                    int maxLoad = 0;
                    for (MachineState state : machineStates) {
                        int load = state.getCurrentLoad();
                        if (state == candidate) {
                            load = newLoad;
                        }
                        if (load > 0) {
                            minLoad = Math.min(minLoad, load);
                            maxLoad = Math.max(maxLoad, load);
                        }
                    }
                    
                    // 剪枝条件：负荷差额超过阈值
                    if (minLoad != Integer.MAX_VALUE && maxLoad - minLoad > loadDiffThreshold) {
                        searchResult.pruneCount++;
                        continue;
                    }
                    
                    // === 分配并递归 ===
                    candidate.getAssignedEmbryos().add(new EmbryoAssignment(embryoCode, task, assignQty));
                    candidate.setCurrentLoad(newLoad);
                    if (isNewType) {
                        candidate.setCurrentTypes(candidate.getCurrentTypes() + 1);
                    }
                    
                    int newRemainingCount = remainingCount - assignQty;
                    
                    // 如果当前胎胚还有剩余，继续分配；否则处理下一个任务
                    if (newRemainingCount > 0) {
                        dfsAssign(tasks, taskIndex, newRemainingCount, machineStates, forceKeepHistory,
                                typeDiffThreshold, loadDiffThreshold, searchResult);
                    } else {
                        dfsAssign(tasks, taskIndex + 1, 0, machineStates, forceKeepHistory,
                                typeDiffThreshold, loadDiffThreshold, searchResult);
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
            // 当前胎胚已分配完毕，处理下一个任务
            int nextLhCount = taskIndex + 1 < tasks.size() 
                    ? (tasks.get(taskIndex + 1).getVulcanizeMachineCount() != null 
                       ? tasks.get(taskIndex + 1).getVulcanizeMachineCount() : 0)
                    : 0;
            
            if (nextLhCount <= 0) {
                // 下一个任务不需要分配，直接跳过
                dfsAssign(tasks, taskIndex + 1, 0, machineStates, forceKeepHistory,
                        typeDiffThreshold, loadDiffThreshold, searchResult);
            } else {
                // 开始分配下一个任务
                dfsAssign(tasks, taskIndex + 1, nextLhCount, machineStates, forceKeepHistory,
                        typeDiffThreshold, loadDiffThreshold, searchResult);
            }
        }
    }

    /**
     * 找出可以分配胎胚的候选机台（支持拆分）
    /**
     * 查找可以将胎胚分配到该机台的候选机台列表
     *
     * <p>候选条件：机台当前负荷 < 最大容量（即还有剩余硫化机台数）
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
            boolean forceKeepHistory) {
        
        List<MachineState> candidates = new ArrayList<>();
        
        for (MachineState state : machineStates) {
            // 只要有剩余容量就可以作为候选
            if (state.getCurrentLoad() < state.getMaxCapacity()) {
                candidates.add(state);
            }
        }
        
        return candidates;
    }

    /**
     * 排序候选机台（DFS用）
     *
     * <p>排序优先级：
     * <ol>
     *   <li>历史胎胚优先（工人更熟悉，换品种成本低）</li>
     *   <li>负荷少的优先（均衡分配）</li>
     *   <li>种类少的优先（均衡分配）</li>
     * </ol>
     */
    private void sortCandidatesForDfs(
            List<MachineState> candidates,
            String embryoCode,
            boolean forceKeepHistory) {
        
        candidates.sort((a, b) -> {
            // 优先级1：历史胎胚优先
            boolean aHasHistory = a.getHistoryEmbryos().contains(embryoCode);
            boolean bHasHistory = b.getHistoryEmbryos().contains(embryoCode);
            if (aHasHistory && !bHasHistory) {
                return -1;
            }
            if (!aHasHistory && bHasHistory) {
                return 1;
            }
            
            // 优先级2：负荷少的优先
            int loadCompare = Integer.compare(a.getCurrentLoad(), b.getCurrentLoad());
            if (loadCompare != 0) {
                return loadCompare;
            }
            
            // 优先级3：种类少的优先
            return Integer.compare(a.getCurrentTypes(), b.getCurrentTypes());
        });
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
    private Map<String, List<EmbryoAssignment>> copyAssignments(List<MachineState> machineStates) {
        Map<String, List<EmbryoAssignment>> copy = new LinkedHashMap<>();
        for (MachineState state : machineStates) {
            copy.put(state.getMachineCode(), new ArrayList<>(state.getAssignedEmbryos()));
        }
        return copy;
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
    private int calculateBalancingScore(List<MachineState> machineStates) {
        if (machineStates.isEmpty()) {
            return 0;
        }
        
        int maxLoad = 0, minLoad = Integer.MAX_VALUE;
        int maxTypes = 0, minTypes = Integer.MAX_VALUE;
        
        for (MachineState state : machineStates) {
            if (state.getCurrentLoad() > 0) {
                maxLoad = Math.max(maxLoad, state.getCurrentLoad());
                minLoad = Math.min(minLoad, state.getCurrentLoad());
                maxTypes = Math.max(maxTypes, state.getCurrentTypes());
                minTypes = Math.min(minTypes, state.getCurrentTypes());
            }
        }
        
        int loadGap = maxLoad - (minLoad == Integer.MAX_VALUE ? 0 : minLoad);
        int typeGap = maxTypes - (minTypes == Integer.MAX_VALUE ? 0 : minTypes);
        
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
            Map<String, List<EmbryoAssignment>> assignments,
            List<MachineState> machineStates,
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks) {
        
        BalancingResult result = new BalancingResult();
        result.setAssignments(new ArrayList<>());
        
        for (Map.Entry<String, List<EmbryoAssignment>> entry : assignments.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            
            MachineAssignment assignment = new MachineAssignment();
            assignment.setMachineCode(entry.getKey());
            assignment.setEmbryoAssignments(entry.getValue());
            result.getAssignments().add(assignment);
        }
        
        return result;
    }

    /**
     * 贪心算法兜底方案（支持胎胚拆分）
     */
    private BalancingResult greedyAssignFallback(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
            List<MachineState> machineStates,
            boolean forceKeepHistory,
            int typeDiffThreshold,
            int loadDiffThreshold) {
        
        // 重置机台状态
        for (MachineState state : machineStates) {
            state.setCurrentLoad(0);
            state.setCurrentTypes(0);
            state.getAssignedEmbryos().clear();
        }
        
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
            String embryoCode = task.getEmbryoCode();
            int remainingCount = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;
            
            if (remainingCount <= 0) {
                continue;
            }
            
            // 支持拆分：逐个分配硫化机台数
            while (remainingCount > 0) {
                List<MachineState> candidates = findCandidateMachinesForSplit(
                        embryoCode, machineStates, forceKeepHistory);
                
                if (candidates.isEmpty()) {
                    log.warn("胎胚 {} 剩余 {} 个硫化机无法分配到任何机台", embryoCode, remainingCount);
                    break;
                }
                
                sortCandidatesForDfs(candidates, embryoCode, forceKeepHistory);
                
                MachineState selected = candidates.get(0);
                
                // 计算可以分配的数量（最多分配剩余的，或机台剩余容量）
                int assignQty = Math.min(remainingCount, 
                        selected.getMaxCapacity() - selected.getCurrentLoad());
                
                if (assignQty <= 0) {
                    log.warn("机台 {} 无剩余容量，胎胚 {} 剩余 {} 个硫化机无法分配", 
                            selected.getMachineCode(), embryoCode, remainingCount);
                    break;
                }
                
                boolean isNewType = !selected.getAssignedEmbryos().stream()
                        .anyMatch(e -> e.getEmbryoCode().equals(embryoCode));
                
                selected.getAssignedEmbryos().add(new EmbryoAssignment(embryoCode, task, assignQty));
                selected.setCurrentLoad(selected.getCurrentLoad() + assignQty);
                if (isNewType) {
                    selected.setCurrentTypes(selected.getCurrentTypes() + 1);
                }
                
                remainingCount -= assignQty;
            }
        }
        
        return convertToResult(machineStates, tasks);
    }

    /**
     * 转换为结果对象
     */
    private BalancingResult convertToResult(
            List<MachineState> machineStates,
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks) {
        
        BalancingResult result = new BalancingResult();
        result.setAssignments(new ArrayList<>());
        
        for (MachineState state : machineStates) {
            if (state.getAssignedEmbryos().isEmpty()) {
                continue;
            }
            
            MachineAssignment assignment = new MachineAssignment();
            assignment.setMachineCode(state.getMachineCode());
            assignment.setEmbryoAssignments(state.getAssignedEmbryos());
            result.getAssignments().add(assignment);
        }
        
        return result;
    }

    /**
     * 记录分配结果日志
     */
    private void logAllocationResult(BalancingResult result, List<MachineState> machineStates) {
        log.info("均衡分配结果：");
        
        int maxLoad = 0, minLoad = Integer.MAX_VALUE;
        int maxTypes = 0, minTypes = Integer.MAX_VALUE;
        
        for (MachineAssignment assignment : result.getAssignments()) {
            List<String> embryos = assignment.getEmbryoAssignments().stream()
                    .map(e -> e.getEmbryoCode() + "(" + e.getAssignedQty() + ")")
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
     * <p>在同结构下，以硫化机台数最多的胎胚（绑定胎胚）的班次分配比例为基准，
     * 将同组其他胎胚的班次计划量按该比例重新分配，使同一结构下各班次总产量趋于均衡。
     *
     * <p>仅对普通任务（非试制、非开产、非收尾）进行均衡：
     * <ul>
     *   <li>试制任务独立排产，不参与结构均衡</li>
     *   <li>开产/收尾任务仅在首/末班生产，不参与班次比例调整</li>
     * </ul>
     *
     * <p>均衡策略：
     * <ul>
     *   <li>Step1: 按 structureName 分组</li>
     *   <li>Step2: 在每组内，找 vulcanizeMachineCount 最大的胎胚作为绑定胎胚</li>
     *   <li>Step3: 计算绑定胎胚各班次的车数比例</li>
     *   <li>Step4: 将该比例应用到同组其他胎胚，重新计算各班次的车数</li>
     *   <li>Step5: 更新对应 ShiftProductionResult 的 quantity 和 carsForShift</li>
     * </ul>
     *
     * @param results 排产结果列表（会被直接修改）
     * @param context 排程上下文
     * @return 均衡后的结果列表（与输入 results 相同引用）
     */
    public List<ShiftScheduleService.ShiftProductionResult> balanceShiftQuantities(
            List<ShiftScheduleService.ShiftProductionResult> results,
            ScheduleContextVo context) {

        if (results == null || results.isEmpty()) {
            return results;
        }

        // Step1: 按 structureName 分组
        Map<String, List<ShiftScheduleService.ShiftProductionResult>> byStructure = results.stream()
                .collect(Collectors.groupingBy(r -> {
                    CoreScheduleAlgorithmService.DailyEmbryoTask task = r.getSourceTask();
                    return task != null ? task.getStructureName() : r.getStructureName();
                }));

        for (Map.Entry<String, List<ShiftScheduleService.ShiftProductionResult>> entry : byStructure.entrySet()) {
            String structureName = entry.getKey();
            List<ShiftScheduleService.ShiftProductionResult> group = entry.getValue();

            // 过滤出普通任务（排除试制、收尾），仅普通任务参与均衡
            List<ShiftScheduleService.ShiftProductionResult> regularTasks = group.stream()
                    .filter(r -> !Boolean.TRUE.equals(r.getIsTrialTask())
                            && !Boolean.TRUE.equals(r.getIsEndingTask()))
                    .collect(Collectors.toList());

            if (regularTasks.size() < 2) {
                continue;
            }

            // Step2: 找绑定胎胚 —— 硫化机台数最多的那个
            ShiftScheduleService.ShiftProductionResult bindingResult = regularTasks.stream()
                    .max(Comparator.comparingInt(r -> {
                        CoreScheduleAlgorithmService.DailyEmbryoTask task = r.getSourceTask();
                        return task != null && task.getVulcanizeMachineCount() != null
                                ? task.getVulcanizeMachineCount() : 0;
                    }))
                    .orElse(null);

            if (bindingResult == null) {
                continue;
            }

            // Step3: 提取绑定胎胚的三个班次量，按班次编码排序
            List<ShiftScheduleService.ShiftProductionResult> bindingShifts = regularTasks.stream()
                    .filter(r -> r.getEmbryoCode().equals(bindingResult.getEmbryoCode()))
                    .sorted(Comparator.comparing(ShiftScheduleService.ShiftProductionResult::getShiftCode))
                    .collect(Collectors.toList());

            if (bindingShifts.size() != 3) {
                // 不足3个班次，跳过均衡
                continue;
            }

            // 提取当前三个班次的车数
            int[] currentCars = new int[]{
                    bindingShifts.get(0).getCarsForShift() != null ? bindingShifts.get(0).getCarsForShift() : 0,
                    bindingShifts.get(1).getCarsForShift() != null ? bindingShifts.get(1).getCarsForShift() : 0,
                    bindingShifts.get(2).getCarsForShift() != null ? bindingShifts.get(2).getCarsForShift() : 0
            };

            int totalCars = currentCars[0] + currentCars[1] + currentCars[2];
            if (totalCars == 0) {
                continue;
            }

            // Step4: 均衡策略 —— 先使三个班次量尽可能均衡（最大-最小 ≤ 1），再循环右移1位
            // 4.1 排序得到 [min, mid, max]
            int[] sorted = currentCars.clone();
            Arrays.sort(sorted);

            // 4.2 均分：total=238 → [79, 79, 80]；total=240 → [80, 80, 80]
            int base = totalCars / 3;
            int remainder = totalCars % 3;
            // 分配策略：最大值多拿，余量从大到小分配，使 max-min ≤ 1
            // remainder=0 → [base, base, base]
            // remainder=1 → [base, base, base+1]
            // remainder=2 → [base, base+1, base+1]
            sorted[0] = base + (remainder > 1 ? 1 : 0);
            sorted[1] = base + (remainder > 0 ? 1 : 0);
            sorted[2] = base;

            // 4.3 循环右移1位：sorted=[a,b,c] → [c,a,b]
            // 结果：shift1=max, shift2=min, shift3=mid（中间班次最少）
            int[] balancedCars = new int[]{sorted[2], sorted[0], sorted[1]};

            // Step5: 更新绑定胎胚三个班次的 carsForShift 和 quantity
            for (int i = 0; i < 3; i++) {
                ShiftScheduleService.ShiftProductionResult shift = bindingShifts.get(i);
                int newCars = balancedCars[i];
                int tripCapacity = shift.getTripCapacity() != null ? shift.getTripCapacity() : 1;
                shift.setCarsForShift(newCars);
                shift.setQuantity(newCars * tripCapacity);
            }
        }

        return results;
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
        Map<String, List<EmbryoAssignment>> bestAssignments;
        int searchCount;
        int pruneCount;
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
