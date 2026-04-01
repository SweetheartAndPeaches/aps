package com.zlt.aps.cx.service;

import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.config.CxStructureShiftCapacity;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
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
 * 续作任务处理器
 * 
 * <p>负责 S5.3 续作任务排产：
 * <ul>
 *   <li>按结构分组任务</li>
 *   <li>均衡分配胎胚到机台</li>
 *   <li>S5.3.1 分配胎胚库存：按硫化需求占比分配</li>
 *   <li>S5.3.2 计算待排产量：(日硫化量 - 库存) × (1 + 损耗率) + 异常平摊</li>
 *   <li>S5.3.3 开停产特殊处理</li>
 *   <li>S5.3.4 收尾余量处理：主要产品补到一整车</li>
 *   <li>S5.3.5 补做逻辑：延误检测和补做计算</li>
 * </ul>
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContinueTaskProcessor {

    /** 开产首班排产时长（小时） */
    private static final int OPENING_SHIFT_HOURS = 6;
    
    /** 胎胚库容上限比例 */
    private static final double EMBRYO_STORAGE_RATIO = 0.9;
    
    /** 默认整车容量 */
    private static final int DEFAULT_TRIP_CAPACITY = 12;

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

    // ==================== 核心方法：均衡分配 ====================

    /**
     * 处理续作任务
     *
     * <p>S5.3 续作任务排产流程：
     * <ol>
     *   <li>按结构分组任务</li>
     *   <li>获取结构可分配机台</li>
     *   <li>均衡分配胎胚到机台</li>
     *   <li>分配计划量</li>
     * </ol>
     *
     * @param continueTasks    续作任务列表
     * @param context          排程上下文
     * @param scheduleDate     排程日期
     * @param dayShifts        班次配置
     * @param day              排程天数
     * @return 机台分配结果列表
     */
    public List<CoreScheduleAlgorithmService.MachineAllocationResult> processContinueTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> continueTasks,
            ScheduleContextDTO context,
            LocalDate scheduleDate,
            List<CxShiftConfig> dayShifts,
            int day) {

        List<CoreScheduleAlgorithmService.MachineAllocationResult> results = new ArrayList<>();

        if (CollectionUtils.isEmpty(continueTasks)) {
            return results;
        }

        log.info("========== 开始处理续作任务，共 {} 个任务 ==========", continueTasks.size());

        // Step 1: 按结构分组任务
        Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> structureTaskMap = 
                groupTasksByStructure(continueTasks);
        log.info("按结构分组完成，共 {} 个结构", structureTaskMap.size());

        // Step 2: 获取是否强制保留历史任务
        boolean forceKeepHistory = getForceKeepHistoryConfig(context);
        log.info("强制保留历史任务配置: {}", forceKeepHistory);

        // Step 3: 构建历史任务映射（机台 -> 昨天做的胎胚集合）
        Map<String, Set<String>> machineHistoryMap = buildMachineHistoryMap(context);
        log.info("构建历史任务映射完成，共 {} 台机台有历史记录", machineHistoryMap.size());

        // Step 4: 按结构处理每个分组
        for (Map.Entry<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> entry : structureTaskMap.entrySet()) {
            String structureName = entry.getKey();
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks = entry.getValue();

            log.info("--- 处理结构 {}，共 {} 个胎胚 ---", structureName, tasks.size());

            // 获取该结构可分配的机台列表
            List<MpCxCapacityConfiguration> availableMachines = getAvailableMachinesForStructure(
                    structureName, scheduleDate, context);
            
            if (availableMachines.isEmpty()) {
                log.warn("结构 {} 没有可分配的机台，跳过", structureName);
                continue;
            }

            // 获取结构的产能限制配置
            MdmStructureLhRatio ratioConfig = context.getStructureLhRatioMap() != null 
                    ? context.getStructureLhRatioMap().get(structureName) 
                    : null;
            
            int maxLhMachines = ratioConfig != null && ratioConfig.getLhMachineMaxQty() != null 
                    ? ratioConfig.getLhMachineMaxQty() : 10;
            int maxEmbryoTypes = ratioConfig != null && ratioConfig.getMaxEmbryoQty() != null 
                    ? ratioConfig.getMaxEmbryoQty() : 4;

            // Step 5: 均衡分配胎胚到机台
            BalancingResult balancingResult = balanceEmbryosToMachines(
                    tasks, 
                    availableMachines, 
                    machineHistoryMap,
                    maxLhMachines,
                    maxEmbryoTypes,
                    forceKeepHistory,
                    context);

            // Step 6: 为每个机台分配计划量
            for (MachineAssignment assignment : balancingResult.getAssignments()) {
                CoreScheduleAlgorithmService.MachineAllocationResult allocation = createMachineAllocation(
                        assignment.getMachineCode(), context);

                for (EmbryoAssignment embryoAssignment : assignment.getEmbryoAssignments()) {
                    CoreScheduleAlgorithmService.DailyEmbryoTask task = embryoAssignment.getTask();
                    
                    // S5.3.1 分配胎胚库存
                    allocateEmbryoStock(task, context, scheduleDate);
                    
                    // S5.3.2 计算待排产量
                    boolean isOpeningDay = Boolean.TRUE.equals(context.getIsOpeningDay()) && day == 1;
                    calculatePlannedProduction(task, context, scheduleDate, isOpeningDay);
                    
                    // S5.3.3 开停产特殊处理
                    boolean isClosingDay = Boolean.TRUE.equals(context.getIsClosingDay());
                    handleOpeningClosingDay(task, context, dayShifts, isOpeningDay, isClosingDay);
                    
                    // S5.3.4 收尾余量处理
                    handleEndingRemainder(task, context, isOpeningDay);
                    
                    // S5.3.5 计算延误量和补做
                    if (Boolean.TRUE.equals(task.getIsNearEnding()) && !isOpeningDay) {
                        int catchUpQty = calculateCatchUpQuantity(task, context, scheduleDate);
                        if (catchUpQty > 0) {
                            int tripCapacity = getTripCapacity(task.getStructureName(), context);
                            int catchUpTrips = convertToTrips(catchUpQty, tripCapacity, task.getIsMainProduct());
                            task.setCatchUpQuantity(catchUpTrips * tripCapacity);
                            task.setPlannedProduction(task.getPlannedProduction() + task.getCatchUpQuantity());
                        }
                    }

                    // 分配任务到机台
                    if (task.getPlannedProduction() != null && task.getPlannedProduction() > 0) {
                        allocateTaskToMachine(allocation, task, context);
                    }
                }

                if (!allocation.getTaskAllocations().isEmpty()) {
                    results.add(allocation);
                }
            }
        }

        log.info("========== 续作任务处理完成，共 {} 台机台分配任务 ==========", results.size());
        return results;
    }

    // ==================== 均衡分配算法 ====================

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
     * @param tasks             胎胚任务列表
     * @param availableMachines 可分配机台列表
     * @param machineHistoryMap 历史任务映射（机台 -> 昨天做的胎胚集合）
     * @param maxLhMachines     最大硫化机台数（每台成型机的产能上限）
     * @param maxEmbryoTypes    最大胎胚种类数
     * @param forceKeepHistory  是否强制保留历史任务
     * @param context           排程上下文
     * @return 均衡分配结果
     */
    private BalancingResult balanceEmbryosToMachines(
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

        // Step 6: DFS + 剪枝搜索最优方案
        DfsSearchResult searchResult = new DfsSearchResult();
        searchResult.bestScore = Integer.MAX_VALUE;
        searchResult.bestAssignments = null;
        searchResult.searchCount = 0;
        searchResult.pruneCount = 0;
        
        // 开始DFS搜索
        dfsAssign(sortedTasks, 0, machineStates, forceKeepHistory, 
                typeDiffThreshold, loadDiffThreshold, searchResult);
        
        log.info("DFS搜索统计：总搜索次数={}, 剪枝次数={}, 最优分数={}", 
                searchResult.searchCount, searchResult.pruneCount, searchResult.bestScore);

        // Step 7: 构建结果
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
     * DFS深度优先搜索 + 剪枝
     *
     * @param tasks              待分配任务列表（已排序）
     * @param taskIndex          当前处理的任务索引
     * @param machineStates      机台状态列表（会被修改和恢复）
     * @param forceKeepHistory   是否强制保留历史任务
     * @param typeDiffThreshold  种类数允许差额
     * @param loadDiffThreshold  负荷允许差额
     * @param searchResult       搜索结果记录对象
     */
    private void dfsAssign(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
            int taskIndex,
            List<MachineState> machineStates,
            boolean forceKeepHistory,
            int typeDiffThreshold,
            int loadDiffThreshold,
            DfsSearchResult searchResult) {
        
        // 搜索计数
        searchResult.searchCount++;
        
        // 终止条件：所有任务已分配
        if (taskIndex >= tasks.size()) {
            // 计算当前方案的均衡分数
            int score = calculateBalancingScore(machineStates);
            
            // 更新最优方案
            if (score < searchResult.bestScore) {
                searchResult.bestScore = score;
                searchResult.bestAssignments = copyAssignments(machineStates);
            }
            return;
        }
        
        // 获取当前任务
        CoreScheduleAlgorithmService.DailyEmbryoTask task = tasks.get(taskIndex);
        String embryoCode = task.getMaterialCode();
        int lhMachineCount = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;
        
        // 如果硫化机台数为0，跳过
        if (lhMachineCount <= 0) {
            dfsAssign(tasks, taskIndex + 1, machineStates, forceKeepHistory,
                    typeDiffThreshold, loadDiffThreshold, searchResult);
            return;
        }
        
        // 找出可以分配的候选机台（考虑历史优先）
        List<MachineState> candidates = findCandidateMachinesForDfs(
                embryoCode, machineStates, forceKeepHistory, lhMachineCount);
        
        // 按优先级排序候选机台（历史优先 > 负荷少 > 种类少）
        sortCandidatesForDfs(candidates, embryoCode, forceKeepHistory);
        
        // 遍历候选机台
        for (MachineState candidate : candidates) {
            // === 剪枝检查 ===
            
            // 检查1：是否会导致种类数差额超过阈值
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
                minTypes = Math.min(minTypes, types);
                maxTypes = Math.max(maxTypes, types);
            }
            
            // 剪枝条件：种类数差额超过阈值
            if (maxTypes - minTypes > typeDiffThreshold) {
                searchResult.pruneCount++;
                continue; // 剪枝，尝试下一个机台
            }
            
            // 检查2：是否会导致负荷差额超过阈值
            int newLoad = candidate.getCurrentLoad() + lhMachineCount;
            
            // 计算分配后各机台的负荷
            int minLoad = Integer.MAX_VALUE;
            int maxLoad = 0;
            for (MachineState state : machineStates) {
                int load = state.getCurrentLoad();
                if (state == candidate) {
                    load = newLoad;
                }
                minLoad = Math.min(minLoad, load);
                maxLoad = Math.max(maxLoad, load);
            }
            
            // 剪枝条件：负荷差额超过阈值
            if (maxLoad - minLoad > loadDiffThreshold) {
                searchResult.pruneCount++;
                continue; // 剪枝
            }
            
            // === 分配并递归 ===
            
            // 执行分配
            candidate.getAssignedEmbryos().add(new EmbryoAssignment(embryoCode, task, lhMachineCount));
            candidate.setCurrentLoad(newLoad);
            if (isNewType) {
                candidate.setCurrentTypes(candidate.getCurrentTypes() + 1);
            }
            
            // 递归搜索下一个任务
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
            // 检查容量是否足够
            if (state.getCurrentLoad() + lhMachineCount > state.getMaxCapacity()) {
                continue;
            }
            
            // 如果强制保留历史，只选择历史机台或没有历史的机台
            if (forceKeepHistory && !state.getHistoryEmbryos().isEmpty() 
                    && !state.getHistoryEmbryos().contains(embryoCode)) {
                continue;
            }
            
            candidates.add(state);
        }
        
        return candidates;
    }
    
    /**
     * 排序候选机台（DFS用）
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
     * 复制当前分配状态（用于记录最优方案）
     */
    private Map<String, List<EmbryoAssignment>> copyAssignments(List<MachineState> machineStates) {
        Map<String, List<EmbryoAssignment>> copy = new LinkedHashMap<>();
        for (MachineState state : machineStates) {
            copy.put(state.getMachineCode(), new ArrayList<>(state.getAssignedEmbryos()));
        }
        return copy;
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
     * 贪心算法兜底方案（当DFS未找到有效方案时使用）
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
        
        // 使用贪心算法分配，尽量满足均衡条件
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
            String embryoCode = task.getMaterialCode();
            int lhMachineCount = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;
            
            if (lhMachineCount <= 0) {
                continue;
            }
            
            // 找出候选机台
            List<MachineState> candidates = findCandidateMachinesForDfs(
                    embryoCode, machineStates, forceKeepHistory, lhMachineCount);
            
            if (candidates.isEmpty()) {
                log.warn("胎胚 {} 无法分配到任何机台", embryoCode);
                continue;
            }
            
            // 排序候选机台
            sortCandidatesForDfs(candidates, embryoCode, forceKeepHistory);
            
            // 选择第一个候选机台
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
     * 获取种类数允许差额配置
     */
    private int getTypeDiffThreshold(ScheduleContextDTO context) {
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
    private int getLoadDiffThreshold(ScheduleContextDTO context) {
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
     * DFS搜索结果记录
     */
    @lombok.Data
    private static class DfsSearchResult {
        /** 最优均衡分数 */
        int bestScore;
        /** 最优分配方案（机台编码 -> 分配列表） */
        Map<String, List<EmbryoAssignment>> bestAssignments;
        /** 搜索次数统计 */
        int searchCount;
        /** 剪枝次数统计 */
        int pruneCount;
    }

    /**
     * 计算均衡分数（越小越均衡）
     */
    private int calculateBalancingScore(List<MachineState> machineStates) {
        if (machineStates.isEmpty()) {
            return 0;
        }
        
        // 计算负荷差距
        int maxLoad = 0, minLoad = Integer.MAX_VALUE;
        int maxTypes = 0, minTypes = Integer.MAX_VALUE;
        
        for (MachineState state : machineStates) {
            if (state.getCurrentLoad() > 0) { // 只考虑有任务的机台
                maxLoad = Math.max(maxLoad, state.getCurrentLoad());
                minLoad = Math.min(minLoad, state.getCurrentLoad());
                maxTypes = Math.max(maxTypes, state.getCurrentTypes());
                minTypes = Math.min(minTypes, state.getCurrentTypes());
            }
        }
        
        // 分数 = 负荷差距 * 10 + 种类差距
        int loadGap = maxLoad - (minLoad == Integer.MAX_VALUE ? 0 : minLoad);
        int typeGap = maxTypes - (minTypes == Integer.MAX_VALUE ? 0 : minTypes);
        
        return loadGap * 10 + typeGap * 100;
    }

    /**
     * 简单分配（兜底方案）
     *
     * <p>轮流分配胎胚到机台，不考虑均衡
     */
    private BalancingResult simpleAssign(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
            List<MachineState> machineStates) {
        
        BalancingResult result = new BalancingResult();
        result.setAssignments(new ArrayList<>());
        
        int machineIndex = 0;
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
            MachineState state = machineStates.get(machineIndex % machineStates.size());
            
            MachineAssignment assignment = result.getAssignments().stream()
                    .filter(a -> a.getMachineCode().equals(state.getMachineCode()))
                    .findFirst()
                    .orElse(null);
            
            if (assignment == null) {
                assignment = new MachineAssignment();
                assignment.setMachineCode(state.getMachineCode());
                assignment.setEmbryoAssignments(new ArrayList<>());
                result.getAssignments().add(assignment);
            }
            
            // 使用硫化机台数
            int lhMachineCount = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;
            assignment.getEmbryoAssignments().add(new EmbryoAssignment(
                    task.getMaterialCode(), task, lhMachineCount));
            
            machineIndex++;
        }
        
        return result;
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
        
        // 计算均衡指标
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

    // ==================== 辅助方法 ====================

    /**
     * 按结构分组任务
     */
    private Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> groupTasksByStructure(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks) {
        return tasks.stream()
                .filter(t -> t.getStructureName() != null)
                .collect(Collectors.groupingBy(
                        CoreScheduleAlgorithmService.DailyEmbryoTask::getStructureName,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    /**
     * 获取是否强制保留历史任务配置
     */
    private boolean getForceKeepHistoryConfig(ScheduleContextDTO context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_FORCE_KEEP_HISTORY);
            if (config != null && config.getParamValue() != null) {
                return "1".equals(config.getParamValue()) || "true".equalsIgnoreCase(config.getParamValue());
            }
        }
        return false; // 默认不强制保留
    }

    /**
     * 构建历史任务映射
     */
    private Map<String, Set<String>> buildMachineHistoryMap(ScheduleContextDTO context) {
        Map<String, Set<String>> historyMap = new HashMap<>();
        
        if (context.getMachineOnlineEmbryoMap() != null) {
            historyMap.putAll(context.getMachineOnlineEmbryoMap());
        }
        
        return historyMap;
    }

    /**
     * 获取结构可分配的机台列表
     */
    private List<MpCxCapacityConfiguration> getAvailableMachinesForStructure(
            String structureName,
            LocalDate scheduleDate,
            ScheduleContextDTO context) {
        
        // 从预加载的配置中获取
        if (context.getStructureAllocationMap() != null) {
            List<MpCxCapacityConfiguration> configs = context.getStructureAllocationMap().get(structureName);
            if (configs != null && !configs.isEmpty()) {
                // 过滤当天可用的机台
                int day = scheduleDate.getDayOfMonth();
                return configs.stream()
                        .filter(c -> c.getBeginDay() != null && c.getEndDay() != null)
                        .filter(c -> c.getBeginDay() <= day && c.getEndDay() >= day)
                        .collect(Collectors.toList());
            }
        }
        
        return new ArrayList<>();
    }

    // ==================== 内部类 ====================

    /**
     * 机台状态（分配过程中使用）
     */
    @lombok.Data
    private static class MachineState {
        private String machineCode;
        private int maxCapacity;
        private int maxTypes;
        private int currentLoad;
        private int currentTypes;
        private List<EmbryoAssignment> assignedEmbryos;
        private Set<String> historyEmbryos;
    }

    /**
     * 均衡分配结果
     */
    @lombok.Data
    private static class BalancingResult {
        private List<MachineAssignment> assignments;
    }

    /**
     * 机台分配
     */
    @lombok.Data
    private static class MachineAssignment {
        private String machineCode;
        private List<EmbryoAssignment> embryoAssignments;
    }

    /**
     * 胎胚分配
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class EmbryoAssignment {
        private String embryoCode;
        private CoreScheduleAlgorithmService.DailyEmbryoTask task;
        /** 分配数量，单位：硫化机台数 */
        private int assignedQty;
    }

    // ==================== 原有方法（保持不变） ====================

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
     * S5.3.1 分配胎胚库存
     */
    public void allocateEmbryoStock(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextDTO context,
            LocalDate scheduleDate) {
        
        String embryoCode = task.getMaterialCode();
        
        int totalEmbryoStock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;
        if (totalEmbryoStock <= 0) {
            task.setAllocatedStock(0);
            return;
        }
        
        List<LhScheduleResult> lhResults = context.getLhScheduleResults();
        if (lhResults == null || lhResults.isEmpty()) {
            task.setAllocatedStock(totalEmbryoStock);
            return;
        }
        
        Map<String, List<LhScheduleResult>> embryoSkuMap = lhResults.stream()
                .filter(r -> embryoCode.equals(r.getEmbryoCode()))
                .collect(Collectors.groupingBy(LhScheduleResult::getEmbryoCode));
        
        List<LhScheduleResult> sameEmbryoResults = embryoSkuMap.get(embryoCode);
        if (sameEmbryoResults == null || sameEmbryoResults.isEmpty()) {
            task.setAllocatedStock(totalEmbryoStock);
            return;
        }
        
        int totalDailyVulcanize = sameEmbryoResults.stream()
                .mapToInt(r -> r.getDailyPlanQty() != null ? r.getDailyPlanQty() : 0)
                .sum();
        
        if (totalDailyVulcanize <= 0) {
            task.setAllocatedStock(totalEmbryoStock);
            return;
        }
        
        int currentSkuDailyVulcanize = task.getVulcanizeDemand() != null ? task.getVulcanizeDemand() : 0;
        
        int allocatedStock;
        if (sameEmbryoResults.size() == 1) {
            allocatedStock = totalEmbryoStock;
        } else {
            allocatedStock = (int) ((double) currentSkuDailyVulcanize / totalDailyVulcanize * totalEmbryoStock);
        }
        
        task.setAllocatedStock(allocatedStock);
        log.debug("胎胚 {} 库存分配：总库存={}, 分配量={}", embryoCode, totalEmbryoStock, allocatedStock);
    }
    
    /**
     * S5.3.2 计算待排产量
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
        int exceptionAllocation = task.getCatchUpQuantity() != null ? task.getCatchUpQuantity() : 0;
        
        int baseProduction = Math.max(0, dailyVulcanize - allocatedStock);
        int plannedProduction = (int) Math.ceil(baseProduction * (1 + lossRate.doubleValue())) + exceptionAllocation;
        
        int machineMaxCapacity = getMachineDailyCapacity(
                task.getContinueMachineCodes() != null && !task.getContinueMachineCodes().isEmpty() 
                        ? task.getContinueMachineCodes().get(0) : null, context);
        
        int embryoStorageLimit = (int) (getEmbryoStorageLimit(task.getMaterialCode(), context) * EMBRYO_STORAGE_RATIO);
        
        plannedProduction = Math.min(plannedProduction, machineMaxCapacity);
        plannedProduction = Math.min(plannedProduction, embryoStorageLimit);
        plannedProduction = Math.max(0, plannedProduction);
        
        task.setPlannedProduction(plannedProduction);
        
        log.debug("任务 {} 待排产量计算：日硫化量={}, 分配库存={}, 待排产量={}",
                task.getMaterialCode(), dailyVulcanize, allocatedStock, plannedProduction);
    }
    
    /**
     * S5.3.3 开停产特殊处理
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
            log.info("停产日任务 {} 不排产", task.getMaterialCode());
            return;
        }
        
        if (isOpeningDay) {
            int hourlyCapacity = getMachineHourlyCapacity(
                    task.getContinueMachineCodes() != null && !task.getContinueMachineCodes().isEmpty()
                            ? task.getContinueMachineCodes().get(0) : null,
                    task.getStructureName(), context);
            
            int openingShiftCapacity = hourlyCapacity * OPENING_SHIFT_HOURS;
            
            boolean isKeyProduct = context.getKeyProductCodes() != null 
                    && context.getKeyProductCodes().contains(task.getMaterialCode());
            
            if (isKeyProduct) {
                task.setIsKeyProductOnOpening(true);
                task.setOpeningShiftCapacity(0);
                log.info("开产首班关键产品 {} 不排产，从第二班开始", task.getMaterialCode());
            } else {
                task.setOpeningShiftCapacity(openingShiftCapacity);
                int originalPlanned = task.getPlannedProduction() != null ? task.getPlannedProduction() : 0;
                task.setPlannedProduction(Math.min(originalPlanned, openingShiftCapacity));
            }
            
            task.setIsOpeningDayTask(true);
        }
    }
    
    /**
     * S5.3.4 收尾余量处理
     */
    public void handleEndingRemainder(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextDTO context,
            boolean isOpeningDay) {
        
        if (isOpeningDay) {
            return;
        }
        
        if (!Boolean.TRUE.equals(task.getIsEndingTask()) && !Boolean.TRUE.equals(task.getIsNearEnding())) {
            return;
        }
        
        Integer endingSurplus = task.getEndingSurplusQty();
        if (endingSurplus == null || endingSurplus <= 0) {
            return;
        }
        
        int tripCapacity = getTripCapacity(task.getStructureName(), context);
        int plannedProduction = task.getPlannedProduction() != null ? task.getPlannedProduction() : 0;
        
        int allocatedStock = task.getAllocatedStock() != null ? task.getAllocatedStock() : 0;
        boolean isLastBatch = (plannedProduction + allocatedStock) >= endingSurplus;
        
        if (isLastBatch) {
            boolean isMainProduct = Boolean.TRUE.equals(task.getIsMainProduct());
            
            if (isMainProduct) {
                int remainder = plannedProduction % tripCapacity;
                if (remainder > 0) {
                    int addToFullTrip = tripCapacity - remainder;
                    task.setPlannedProduction(plannedProduction + addToFullTrip);
                    log.info("主要产品收尾补整车：胎胚={}, 原计划={}, 补充={}, 最终={}",
                            task.getMaterialCode(), plannedProduction, addToFullTrip, task.getPlannedProduction());
                }
            }
            
            task.setIsLastEndingBatch(true);
        }
    }
    
    /**
     * S5.3.5 计算延误量和补做量
     */
    public int calculateCatchUpQuantity(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextDTO context,
            LocalDate scheduleDate) {

        Integer formingRemainder = task.getEndingSurplusQty();
        if (formingRemainder == null || formingRemainder <= 0) {
            return 0;
        }

        LocalDate endingDate = task.getEndingDate();
        if (endingDate == null) {
            return 0;
        }

        int plannedQty = calculatePlannedQuantityToDate(
                task.getMaterialCode(), 
                scheduleDate, 
                endingDate, 
                context);

        int gap = formingRemainder - plannedQty;
        if (gap > 0) {
            log.info("任务 {} 有延误，成型余量={}, 计划量={}, 差值={}",
                    task.getMaterialCode(), formingRemainder, plannedQty, gap);
            return gap;
        }

        return 0;
    }

    /**
     * 计算从当前日期到收尾日的月计划量汇总
     */
    private int calculatePlannedQuantityToDate(
            String materialCode,
            LocalDate startDate,
            LocalDate endDate,
            ScheduleContextDTO context) {
        // TODO: 需要从 context 获取月计划数据
        return 0;
    }

    /**
     * 将数量换算成车
     */
    private int convertToTrips(int quantity, int tripCapacity, Boolean isMainProduct) {
        if (quantity <= 0) {
            return 0;
        }

        if (Boolean.TRUE.equals(isMainProduct)) {
            return (int) Math.ceil((double) quantity / tripCapacity);
        } else {
            return quantity / tripCapacity;
        }
    }

    /**
     * 获取结构的整车容量
     */
    private int getTripCapacity(String structureCode, ScheduleContextDTO context) {
        if (context.getStructureShiftCapacities() != null) {
            for (CxStructureShiftCapacity capacity : context.getStructureShiftCapacities()) {
                if (capacity.getStructureCode() != null &&
                        capacity.getStructureCode().equals(structureCode)) {
                    if (capacity.getTripQty() != null && capacity.getTripQty() > 0) {
                        return capacity.getTripQty();
                    }
                }
            }
        }
        return context.getDefaultTripCapacity() != null
                ? context.getDefaultTripCapacity()
                : DEFAULT_TRIP_CAPACITY;
    }

    /**
     * 获取机台小时产能
     */
    private int getMachineHourlyCapacity(String machineCode, String structureName, ScheduleContextDTO context) {
        if (context.getMachineStructureCapacities() != null && machineCode != null && structureName != null) {
            for (var capacity : context.getMachineStructureCapacities()) {
                if (machineCode.equals(capacity.getCxMachineCode()) 
                        && structureName.equals(capacity.getStructureCode())) {
                    return capacity.getHourlyCapacity() != null ? capacity.getHourlyCapacity() : 50;
                }
            }
        }
        return context.getMachineHourlyCapacity() != null ? context.getMachineHourlyCapacity() : 50;
    }

    /**
     * 获取胎胚库容限制
     */
    private int getEmbryoStorageLimit(String materialCode, ScheduleContextDTO context) {
        return Integer.MAX_VALUE;
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

        allocation.getTaskAllocations().add(taskAllocation);
        allocation.setUsedCapacity(allocation.getUsedCapacity() + quantity);
        allocation.setRemainingCapacity(allocation.getRemainingCapacity() - quantity);
    }
}
