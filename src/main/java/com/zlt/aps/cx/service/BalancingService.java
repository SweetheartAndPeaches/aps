package com.zlt.aps.cx.service;

import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.entity.config.CxParamConfig;
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
     *   <li>总产能 = 所有可分配机台的最大硫化机数之和</li>
     * </ul>
     *
     * @param tasks                 胎胚任务列表
     * @param availableMachines     可分配机台列表（结构排产配置）
     * @param machineHistoryMap     历史任务映射（机台 -> 昨天做的胎胚集合）
     * @param maxLhMachines         最大硫化机台数（每台成型机的产能上限）
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
            ScheduleContextDTO context) {

        // Step 1: 获取均衡阈值配置
        int typeDiffThreshold = getTypeDiffThreshold(context);      // 种类数允许差额
        int loadDiffThreshold = getLoadDiffThreshold(context);      // 负荷允许差额
        
        log.info("均衡分配参数：种类差额阈值={}, 负荷差额阈值={}", 
                typeDiffThreshold, loadDiffThreshold);
        
        // Step 2: 计算总需求（所有胎胚的硫化机台数之和）
        int totalDemand = tasks.stream()
                .mapToInt(t -> t.getVulcanizeMachineCount() != null ? t.getVulcanizeMachineCount() : 0)
                .sum();
        
        // Step 3: 计算总产能（所有可分配机台的最大硫化机数之和）
        int totalCapacity = availableMachines.size() * maxLhMachines;
        
        log.info("均衡分配计算：总需求（硫化机台数）={}, 总产能（最大硫化机数）={}, 机台数={}", 
                totalDemand, totalCapacity, availableMachines.size());
        
        // Step 4: 按硫化机台数从大到小排序胎胚（硫化机数多的优先分配，减少搜索深度）
        List<CoreScheduleAlgorithmService.DailyEmbryoTask> sortedTasks = tasks.stream()
                .sorted((a, b) -> {
                    int countA = a.getVulcanizeMachineCount() != null ? a.getVulcanizeMachineCount() : 0;
                    int countB = b.getVulcanizeMachineCount() != null ? b.getVulcanizeMachineCount() : 0;
                    return Integer.compare(countB, countA);
                })
                .collect(Collectors.toList());

        // Step 5: 初始化机台状态
        List<MachineState> machineStates = new ArrayList<>();
        for (MpCxCapacityConfiguration config : availableMachines) {
            MachineState state = new MachineState();
            state.setMachineCode(config.getCxMachineCode());
            state.setMaxCapacity(maxLhMachines);  // 最大硫化机数
            state.setMaxTypes(maxEmbryoTypes);    // 最大胎胚种类数
            state.setCurrentLoad(0);              // 当前已分配的硫化机数
            state.setCurrentTypes(0);             // 当前已分配的胎胚种类数
            state.setAssignedEmbryos(new ArrayList<>());
            
            Set<String> historyEmbryos = machineHistoryMap.get(config.getCxMachineCode());
            state.setHistoryEmbryos(historyEmbryos != null ? historyEmbryos : new HashSet<>());
            
            machineStates.add(state);
        }

        // Step 6: 如果强制保留历史任务，先进行保底预留
        if (forceKeepHistory) {
            reservedHistoryTasks(sortedTasks, machineStates, context);
        }

        // Step 7: DFS + 剪枝搜索最优方案
        DfsSearchResult searchResult = new DfsSearchResult();
        searchResult.bestScore = Integer.MAX_VALUE;
        searchResult.bestAssignments = null;
        searchResult.searchCount = 0;
        searchResult.pruneCount = 0;
        
        // 开始DFS搜索（只处理剩余需求）
        List<CoreScheduleAlgorithmService.DailyEmbryoTask> remainingTasks = getRemainingTasks(sortedTasks);
        dfsAssign(remainingTasks, 0, machineStates, forceKeepHistory, 
                typeDiffThreshold, loadDiffThreshold, searchResult);
        
        log.info("DFS搜索统计：总搜索次数={}, 剪枝次数={}, 最优分数={}", 
                searchResult.searchCount, searchResult.pruneCount, searchResult.bestScore);

        // Step 8: 构建结果
        BalancingResult result;
        if (searchResult.bestAssignments != null) {
            result = convertDfsResultToBalancingResult(searchResult.bestAssignments, machineStates, sortedTasks);
            log.info("找到满足均衡条件的最优方案");
        } else {
            log.warn("未找到满足均衡条件的方案，使用贪心算法作为兜底");
            result = greedyAssignFallback(sortedTasks, machineStates, forceKeepHistory, 
                    typeDiffThreshold, loadDiffThreshold);
        }

        logAllocationResult(result, machineStates);
        return result;
    }

    /**
     * 均衡分配（简化版，使用默认参数）
     *
     * @param tasks             胎胚任务列表
     * @param availableMachines 可分配机台列表
     * @param context           排程上下文
     * @return 均衡分配结果
     */
    public BalancingResult balanceEmbryosToMachines(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
            List<MdmMoldingMachine> availableMachines,
            ScheduleContextDTO context) {

        // 转换为配置格式
        List<MpCxCapacityConfiguration> configs = availableMachines.stream()
                .map(m -> {
                    MpCxCapacityConfiguration config = new MpCxCapacityConfiguration();
                    config.setCxMachineCode(m.getCxMachineCode());
                    return config;
                })
                .collect(Collectors.toList());

        // 获取默认参数
        int maxLhMachines = 10;
        int maxEmbryoTypes = context.getMaxTypesPerMachine() != null
                ? context.getMaxTypesPerMachine() : 4;
        boolean forceKeepHistory = getForceKeepHistoryConfig(context);
        Map<String, Set<String>> machineHistoryMap = context.getMachineOnlineEmbryoMap();
        if (machineHistoryMap == null) {
            machineHistoryMap = new HashMap<>();
        }

        return balanceEmbryosToMachines(tasks, configs, machineHistoryMap,
                maxLhMachines, maxEmbryoTypes, forceKeepHistory, context);
    }

    // ==================== DFS + 剪枝算法核心 ====================

    /**
     * 保底预留历史任务
     */
    private void reservedHistoryTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
            List<MachineState> machineStates,
            ScheduleContextDTO context) {
        
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
     * DFS深度优先搜索 + 剪枝
     */
    private void dfsAssign(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
            int taskIndex,
            List<MachineState> machineStates,
            boolean forceKeepHistory,
            int typeDiffThreshold,
            int loadDiffThreshold,
            DfsSearchResult searchResult) {
        
        searchResult.searchCount++;
        
        // 终止条件：所有任务已分配
        if (taskIndex >= tasks.size()) {
            int score = calculateBalancingScore(machineStates);
            
            if (score < searchResult.bestScore) {
                searchResult.bestScore = score;
                searchResult.bestAssignments = copyAssignments(machineStates);
            }
            return;
        }
        
        CoreScheduleAlgorithmService.DailyEmbryoTask task = tasks.get(taskIndex);
        String embryoCode = task.getMaterialCode();
        int lhMachineCount = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;
        
        if (lhMachineCount <= 0) {
            dfsAssign(tasks, taskIndex + 1, machineStates, forceKeepHistory,
                    typeDiffThreshold, loadDiffThreshold, searchResult);
            return;
        }
        
        // 找出可以分配的候选机台
        List<MachineState> candidates = findCandidateMachinesForDfs(
                embryoCode, machineStates, forceKeepHistory, lhMachineCount);
        
        // 按优先级排序候选机台
        sortCandidatesForDfs(candidates, embryoCode, forceKeepHistory);
        
        for (MachineState candidate : candidates) {
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
            int newLoad = candidate.getCurrentLoad() + lhMachineCount;
            
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
            candidate.getAssignedEmbryos().add(new EmbryoAssignment(embryoCode, task, lhMachineCount));
            candidate.setCurrentLoad(newLoad);
            if (isNewType) {
                candidate.setCurrentTypes(candidate.getCurrentTypes() + 1);
            }
            
            dfsAssign(tasks, taskIndex + 1, machineStates, forceKeepHistory,
                    typeDiffThreshold, loadDiffThreshold, searchResult);
            
            // === 回溯 ===
            candidate.getAssignedEmbryos().remove(candidate.getAssignedEmbryos().size() - 1);
            candidate.setCurrentLoad(candidate.getCurrentLoad() - lhMachineCount);
            if (isNewType) {
                candidate.setCurrentTypes(candidate.getCurrentTypes() - 1);
            }
        }
    }

    /**
     * 找出可以分配胎胚的候选机台（DFS用）
     */
    private List<MachineState> findCandidateMachinesForDfs(
            String embryoCode,
            List<MachineState> machineStates,
            boolean forceKeepHistory,
            int lhMachineCount) {
        
        List<MachineState> candidates = new ArrayList<>();
        
        for (MachineState state : machineStates) {
            if (state.getCurrentLoad() + lhMachineCount > state.getMaxCapacity()) {
                continue;
            }
            candidates.add(state);
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
            if (aHasHistory && !bHasHistory) return -1;
            if (!aHasHistory && bHasHistory) return 1;
            
            // 优先级2：负荷少的优先
            int loadCompare = Integer.compare(a.getCurrentLoad(), b.getCurrentLoad());
            if (loadCompare != 0) return loadCompare;
            
            // 优先级3：种类少的优先
            return Integer.compare(a.getCurrentTypes(), b.getCurrentTypes());
        });
    }

    /**
     * 复制当前分配状态
     */
    private Map<String, List<EmbryoAssignment>> copyAssignments(List<MachineState> machineStates) {
        Map<String, List<EmbryoAssignment>> copy = new LinkedHashMap<>();
        for (MachineState state : machineStates) {
            copy.put(state.getMachineCode(), new ArrayList<>(state.getAssignedEmbryos()));
        }
        return copy;
    }

    /**
     * 计算均衡分数（越小越均衡）
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
     * 将DFS搜索结果转换为BalancingResult
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
     * 贪心算法兜底方案
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
            String embryoCode = task.getMaterialCode();
            int lhMachineCount = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;
            
            if (lhMachineCount <= 0) {
                continue;
            }
            
            List<MachineState> candidates = findCandidateMachinesForDfs(
                    embryoCode, machineStates, forceKeepHistory, lhMachineCount);
            
            if (candidates.isEmpty()) {
                log.warn("胎胚 {} 无法分配到任何机台", embryoCode);
                continue;
            }
            
            sortCandidatesForDfs(candidates, embryoCode, forceKeepHistory);
            
            MachineState selected = candidates.get(0);
            
            boolean isNewType = !selected.getAssignedEmbryos().stream()
                    .anyMatch(e -> e.getEmbryoCode().equals(embryoCode));
            
            selected.getAssignedEmbryos().add(new EmbryoAssignment(embryoCode, task, lhMachineCount));
            selected.setCurrentLoad(selected.getCurrentLoad() + lhMachineCount);
            if (isNewType) {
                selected.setCurrentTypes(selected.getCurrentTypes() + 1);
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
        
        for (MachineAssignment assignment : result.getAssignments()) {
            List<String> embryos = assignment.getEmbryoAssignments().stream()
                    .map(e -> e.getEmbryoCode() + "(" + e.getAssignedQty() + ")")
                    .collect(Collectors.toList());
            log.info("  机台 {}: {}", assignment.getMachineCode(), embryos);
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
        
        log.info("均衡指标：负荷差距={}, 种类差距={}", 
                maxLoad - minLoad, maxTypes - minTypes);
    }

    // ==================== 配置获取方法 ====================

    /**
     * 获取种类数允许差额配置
     */
    public int getTypeDiffThreshold(ScheduleContextDTO context) {
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
    public int getLoadDiffThreshold(ScheduleContextDTO context) {
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
    public boolean getForceKeepHistoryConfig(ScheduleContextDTO context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_FORCE_KEEP_HISTORY);
            if (config != null && config.getParamValue() != null) {
                return "1".equals(config.getParamValue()) || "true".equalsIgnoreCase(config.getParamValue());
            }
        }
        return false;
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
