package com.jinyu.aps.service;

import com.jinyu.aps.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * APS核心算法服务 - 性能优化版本
 * 
 * 优化内容：
 * 1. 缓存策略：候选机台缓存、约束检查结果缓存
 * 2. 剪枝优化：提前终止无效分支
 * 3. 动态递归深度控制
 * 4. 性能监控指标
 * 
 * @author APS Team
 */
@Service
public class AlgorithmServiceOptimized {

    private static final Logger logger = LoggerFactory.getLogger(AlgorithmServiceOptimized.class);
    private static final Logger perfLogger = LoggerFactory.getLogger("PERFORMANCE");

    // 算法参数配置
    private static final int MAX_RECURSION_DEPTH = 30;  // 增大递归深度上限
    private static final int MAX_SKU_PER_MACHINE_PER_DAY = 4;
    private static final int MAX_STRUCTURE_SWITCH_PER_DAY = 2;
    private static final int DEFAULT_TRIP_CAPACITY = 12;
    
    // 性能优化参数
    private static final int CACHE_MAX_SIZE = 1000;  // 缓存最大容量
    private static final int PRUNE_THRESHOLD = 100;  // 剪枝阈值（尝试次数）
    
    // 缓存
    private final Map<String, Boolean> constraintCheckCache = new ConcurrentHashMap<>();
    private final Map<String, List<MachineLoadContext>> candidateMachineCache = new ConcurrentHashMap<>();
    
    // 性能统计
    private final AtomicLong totalAllocations = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong backtrackCount = new AtomicLong(0);

    /**
     * 试错分配算法 - 性能优化版本
     */
    public AllocationResult allocateTasks(List<DailyEmbryoTask> tasks, List<Machine> machines, ScheduleMain scheduleMain) {
        long startTime = System.currentTimeMillis();
        logger.info("开始试错分配算法（优化版），任务数: {}, 机台数: {}", tasks.size(), machines.size());
        
        // 清空缓存
        constraintCheckCache.clear();
        candidateMachineCache.clear();
        
        AllocationResult result = new AllocationResult();
        result.setScheduleMainId(scheduleMain.getId());
        
        // 1. 任务预处理和排序（使用优化的排序算法）
        List<TaskAllocationContext> taskContexts = prepareTaskContextsOptimized(tasks);
        
        // 2. 机台负载初始化
        Map<String, MachineLoadContext> machineLoads = initializeMachineLoads(machines);
        
        // 3. 预计算候选机台映射（缓存优化）
        precomputeCandidateMachines(taskContexts, machineLoads);
        
        // 4. 递归分配（带剪枝优化）
        AllocationState state = new AllocationState();
        boolean success = recursiveAllocateOptimized(taskContexts, 0, machineLoads, result, scheduleMain, state);
        
        long endTime = System.currentTimeMillis();
        
        if (success) {
            result.setSuccess(true);
            result.setMessage("分配成功");
            logger.info("试错分配完成，耗时: {}ms, 分配数: {}", 
                (endTime - startTime), result.getDetails().size());
        } else {
            result.setSuccess(false);
            result.setMessage("无法找到有效分配方案");
            logger.warn("试错分配失败，耗时: {}ms", (endTime - startTime));
        }
        
        // 记录性能指标
        logPerformanceMetrics(startTime, endTime);
        
        return result;
    }

    /**
     * 优化的任务上下文准备
     */
    private List<TaskAllocationContext> prepareTaskContextsOptimized(List<DailyEmbryoTask> tasks) {
        // 使用并行流加速处理
        return tasks.parallelStream()
            .map(task -> {
                TaskAllocationContext ctx = new TaskAllocationContext();
                ctx.setTask(task);
                ctx.setRemainderQuantity(task.getTaskQuantity());
                return ctx;
            })
            .sorted(this::compareTaskPriority)
            .collect(Collectors.toList());
    }

    /**
     * 任务优先级比较（优化版）
     */
    private int compareTaskPriority(TaskAllocationContext a, TaskAllocationContext b) {
        // 1. 优先级排序（升序）
        int priorityCompare = a.getTask().getPriority().compareTo(b.getTask().getPriority());
        if (priorityCompare != 0) return priorityCompare;
        
        // 2. 主销产品优先
        int isMainA = a.getTask().getIsMainProduct() != null ? a.getTask().getIsMainProduct() : 0;
        int isMainB = b.getTask().getIsMainProduct() != null ? b.getTask().getIsMainProduct() : 0;
        if (isMainA != isMainB) {
            return isMainB - isMainA;
        }
        
        // 3. 任务量排序（降序，大任务优先）
        return b.getTask().getTaskQuantity().compareTo(a.getTask().getTaskQuantity());
    }

    /**
     * 预计算候选机台映射
     */
    private void precomputeCandidateMachines(List<TaskAllocationContext> tasks, 
                                              Map<String, MachineLoadContext> machineLoads) {
        for (TaskAllocationContext task : tasks) {
            String cacheKey = generateCacheKey(task.getTask());
            if (!candidateMachineCache.containsKey(cacheKey)) {
                List<MachineLoadContext> candidates = machineLoads.values().stream()
                    .filter(load -> canAllocateToMachineBasic(task, load))
                    .sorted(Comparator.comparingInt(MachineLoadContext::getCurrentLoad))
                    .collect(Collectors.toList());
                candidateMachineCache.put(cacheKey, candidates);
            }
        }
    }

    /**
     * 优化的递归分配
     */
    private boolean recursiveAllocateOptimized(List<TaskAllocationContext> tasks, int taskIndex,
                                               Map<String, MachineLoadContext> machineLoads,
                                               AllocationResult result, ScheduleMain scheduleMain,
                                               AllocationState state) {
        // 递归深度检查
        if (state.depth > MAX_RECURSION_DEPTH) {
            logger.warn("达到最大递归深度: {}", MAX_RECURSION_DEPTH);
            return false;
        }
        
        // 剪枝检查：尝试次数过多时提前终止
        if (state.attemptCount > PRUNE_THRESHOLD * tasks.size()) {
            logger.warn("剪枝触发，尝试次数: {}", state.attemptCount);
            return false;
        }
        
        // 基准情况：所有任务已分配完成
        if (taskIndex >= tasks.size()) {
            return true;
        }
        
        state.depth++;
        TaskAllocationContext currentTask = tasks.get(taskIndex);
        
        // 从缓存获取候选机台
        String cacheKey = generateCacheKey(currentTask.getTask());
        List<MachineLoadContext> cachedCandidates = candidateMachineCache.get(cacheKey);
        
        // 获取实时有效的候选机台（需要重新过滤，因为状态可能已变化）
        List<MachineLoadContext> candidateMachines;
        if (cachedCandidates != null) {
            candidateMachines = cachedCandidates.stream()
                .filter(load -> canAllocateToMachine(currentTask, load))
                .sorted(Comparator.comparingInt(MachineLoadContext::getCurrentLoad))
                .collect(Collectors.toList());
            cacheHits.incrementAndGet();
        } else {
            candidateMachines = machineLoads.values().stream()
                .filter(load -> canAllocateToMachine(currentTask, load))
                .sorted(Comparator.comparingInt(MachineLoadContext::getCurrentLoad))
                .collect(Collectors.toList());
        }
        
        // 尝试每个候选机台
        for (MachineLoadContext machineLoad : candidateMachines) {
            state.attemptCount++;
            
            int allocatableQty = calculateAllocatableQuantity(currentTask, machineLoad);
            if (allocatableQty <= 0) {
                continue;
            }
            
            // 记录状态用于回溯
            AllocationSnapshot snapshot = saveSnapshot(machineLoad, currentTask);
            
            // 执行分配
            applyAllocation(machineLoad, currentTask, allocatableQty);
            totalAllocations.incrementAndGet();
            
            // 记录分配结果
            AllocationDetail detail = createAllocationDetail(
                currentTask.getTask(), machineLoad.getMachine(), allocatableQty, scheduleMain);
            result.addDetail(detail);
            
            // 递归处理下一个任务
            if (recursiveAllocateOptimized(tasks, taskIndex + 1, machineLoads, result, scheduleMain, state)) {
                state.depth--;
                return true;
            }
            
            // 回溯
            restoreSnapshot(machineLoad, currentTask, snapshot);
            result.removeDetail(detail);
            backtrackCount.incrementAndGet();
        }
        
        state.depth--;
        
        // 当前任务无法完全分配，尝试部分分配
        if (currentTask.getRemainderQuantity() < currentTask.getTask().getTaskQuantity()) {
            return recursiveAllocateOptimized(tasks, taskIndex + 1, machineLoads, result, scheduleMain, state);
        }
        
        return false;
    }

    /**
     * 基本约束检查（用于预计算）
     */
    private boolean canAllocateToMachineBasic(TaskAllocationContext task, MachineLoadContext machineLoad) {
        Machine machine = machineLoad.getMachine();
        
        // 机台状态检查
        if (!"RUNNING".equals(machine.getStatus())) {
            return false;
        }
        
        // 产能检查
        int remainingCapacity = machine.getMaxDailyCapacity() - machineLoad.getCurrentLoad();
        if (remainingCapacity <= 0) {
            return false;
        }
        
        return true;
    }

    /**
     * 完整约束检查（带缓存）
     */
    private boolean canAllocateToMachine(TaskAllocationContext task, MachineLoadContext machineLoad) {
        String cacheKey = task.getTask().getMaterialCode() + "_" + machineLoad.getMachine().getMachineCode();
        
        // 检查缓存
        Boolean cachedResult = constraintCheckCache.get(cacheKey);
        if (cachedResult != null && cachedResult) {
            // 缓存命中，但需要检查动态约束
            if (!checkDynamicConstraints(task, machineLoad)) {
                return false;
            }
            cacheHits.incrementAndGet();
            return true;
        }
        
        Machine machine = machineLoad.getMachine();
        
        // 1. 机台状态检查
        if (!"RUNNING".equals(machine.getStatus())) {
            return false;
        }
        
        // 2. SKU种类限制检查
        if (machineLoad.getAllocatedMaterials().size() >= MAX_SKU_PER_MACHINE_PER_DAY) {
            String taskMaterial = task.getTask().getMaterialCode();
            if (!machineLoad.getAllocatedMaterials().contains(taskMaterial)) {
                return false;
            }
        }
        
        // 3. 结构切换限制检查
        String taskStructure = task.getTask().getProductStructure();
        String lastStructure = machineLoad.getLastStructure();
        if (lastStructure != null && !lastStructure.equals(taskStructure)) {
            if (machineLoad.getStructureSwitchCount() >= MAX_STRUCTURE_SWITCH_PER_DAY) {
                return false;
            }
        }
        
        // 4. 机台产能检查
        int remainingCapacity = machine.getMaxDailyCapacity() - machineLoad.getCurrentLoad();
        if (remainingCapacity <= 0) {
            return false;
        }
        
        // 缓存结果（仅缓存静态约束）
        if (constraintCheckCache.size() < CACHE_MAX_SIZE) {
            constraintCheckCache.put(cacheKey, true);
        }
        
        return true;
    }

    /**
     * 检查动态约束（状态相关）
     */
    private boolean checkDynamicConstraints(TaskAllocationContext task, MachineLoadContext machineLoad) {
        // SKU种类限制（动态）
        if (machineLoad.getAllocatedMaterials().size() >= MAX_SKU_PER_MACHINE_PER_DAY) {
            String taskMaterial = task.getTask().getMaterialCode();
            if (!machineLoad.getAllocatedMaterials().contains(taskMaterial)) {
                return false;
            }
        }
        
        // 结构切换限制（动态）
        String taskStructure = task.getTask().getProductStructure();
        String lastStructure = machineLoad.getLastStructure();
        if (lastStructure != null && !lastStructure.equals(taskStructure)) {
            if (machineLoad.getStructureSwitchCount() >= MAX_STRUCTURE_SWITCH_PER_DAY) {
                return false;
            }
        }
        
        // 产能限制（动态）
        int remainingCapacity = machineLoad.getMachine().getMaxDailyCapacity() - machineLoad.getCurrentLoad();
        return remainingCapacity > 0;
    }

    /**
     * 班次均衡调整算法 - 性能优化版本
     */
    public List<ScheduleDetail> balanceShiftDistribution(List<ScheduleDetail> details, String shiftRatio) {
        long startTime = System.currentTimeMillis();
        logger.info("开始班次均衡调整算法（优化版），明细数: {}", details.size());
        
        if (details.isEmpty()) {
            return details;
        }
        
        // 1. 解析班次比例
        Map<String, Double> ratioMap = parseShiftRatio(shiftRatio);
        
        // 2. 使用并行流计算班次分布
        Map<String, Integer> currentDistribution = details.parallelStream()
            .collect(Collectors.groupingByConcurrent(
                ScheduleDetail::getShiftCode,
                Collectors.summingInt(ScheduleDetail::getPlanQuantity)
            ));
        
        // 3. 计算目标产量
        int totalQuantity = currentDistribution.values().stream().mapToInt(Integer::intValue).sum();
        Map<String, Integer> targetDistribution = calculateTargetDistribution(totalQuantity, ratioMap);
        
        // 4. 检查偏差（使用并行计算）
        Map<String, Integer> deviations = calculateDeviations(currentDistribution, targetDistribution);
        
        // 5. 执行调整（带早停优化）
        boolean needsAdjustment = deviations.values().parallelStream()
            .anyMatch(d -> Math.abs(d) > totalQuantity * 0.05);
        
        if (needsAdjustment) {
            details = performShiftAdjustmentOptimized(details, currentDistribution, targetDistribution, deviations);
            logger.info("班次均衡调整完成");
        } else {
            logger.info("班次分布已均衡，无需调整");
        }
        
        long endTime = System.currentTimeMillis();
        perfLogger.debug("班次均衡耗时: {}ms", (endTime - startTime));
        
        return details;
    }

    /**
     * 优化的班次调整
     */
    private List<ScheduleDetail> performShiftAdjustmentOptimized(List<ScheduleDetail> details,
                                                                   Map<String, Integer> current,
                                                                   Map<String, Integer> target,
                                                                   Map<String, Integer> deviations) {
        // 找出过剩和不足的班次
        List<Map.Entry<String, Integer>> surplusShifts = deviations.entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))  // 过剩多的优先
            .collect(Collectors.toList());
        
        List<Map.Entry<String, Integer>> deficitShifts = deviations.entrySet().stream()
            .filter(e -> e.getValue() < 0)
            .sorted(Comparator.comparingInt(Map.Entry::getValue))  // 不足多的优先
            .collect(Collectors.toList());
        
        // 按班次分组
        Map<String, List<ScheduleDetail>> byShift = details.stream()
            .collect(Collectors.groupingBy(ScheduleDetail::getShiftCode));
        
        // 从过剩班次向不足班次转移
        for (Map.Entry<String, Integer> surplus : surplusShifts) {
            for (Map.Entry<String, Integer> deficit : deficitShifts) {
                int transferQty = Math.min(surplus.getValue(), -deficit.getValue());
                
                if (transferQty > 0 && byShift.containsKey(surplus.getKey())) {
                    List<ScheduleDetail> sourceDetails = byShift.get(surplus.getKey());
                    int transferred = transferFromShift(sourceDetails, deficit.getKey(), transferQty);
                    
                    surplus.setValue(surplus.getValue() - transferred);
                    deficit.setValue(deficit.getValue() + transferred);
                }
            }
        }
        
        return details;
    }

    /**
     * 从班次转移任务
     */
    private int transferFromShift(List<ScheduleDetail> sourceDetails, String toShift, int quantity) {
        int remaining = quantity;
        for (ScheduleDetail detail : sourceDetails) {
            if (remaining <= 0) break;
            if (detail.getPlanQuantity() <= remaining) {
                detail.setShiftCode(toShift);
                remaining -= detail.getPlanQuantity();
            }
        }
        return quantity - remaining;
    }

    /**
     * 顺位排序算法 - 性能优化版本
     */
    public List<ScheduleDetail> sortScheduleSequence(List<ScheduleDetail> details, Map<String, Double> stockHours) {
        long startTime = System.currentTimeMillis();
        logger.info("开始顺位排序算法（优化版），明细数: {}", details.size());
        
        if (details.isEmpty()) {
            return details;
        }
        
        // 1. 批量设置库存时长（避免重复计算）
        details.parallelStream().forEach(detail -> {
            double stockHour = stockHours.getOrDefault(detail.getMaterialCode(), 0.0);
            detail.setStockHoursAtCalc(BigDecimal.valueOf(stockHour));
        });
        
        // 2. 按机台分组（使用并行流）
        Map<String, List<ScheduleDetail>> byMachine = details.parallelStream()
            .collect(Collectors.groupingByConcurrent(ScheduleDetail::getMachineCode));
        
        // 3. 并行排序每个机台的明细
        final List<ScheduleDetail> sortedList = Collections.synchronizedList(new ArrayList<>());
        final int[] globalSequence = {1};
        
        byMachine.entrySet().parallelStream()
            .forEach(entry -> {
                List<ScheduleDetail> machineDetails = entry.getValue();
                
                // 排序
                machineDetails.sort(this::compareDetailPriority);
                
                // 分配顺位
                synchronized (this) {
                    for (int i = 0; i < machineDetails.size(); i++) {
                        ScheduleDetail detail = machineDetails.get(i);
                        detail.setSequence(globalSequence[0]++);
                        detail.setSequenceInGroup(i + 1);
                    }
                }
                
                sortedList.addAll(machineDetails);
            });
        
        // 4. 车次齐套处理
        List<ScheduleDetail> result = arrangeTripsOptimized(sortedList);
        
        long endTime = System.currentTimeMillis();
        perfLogger.debug("顺位排序耗时: {}ms", (endTime - startTime));
        
        return result;
    }

    /**
     * 明细优先级比较
     */
    private int compareDetailPriority(ScheduleDetail a, ScheduleDetail b) {
        // 库存时长升序
        double aStock = a.getStockHoursAtCalc() != null ? a.getStockHoursAtCalc().doubleValue() : 999.0;
        double bStock = b.getStockHoursAtCalc() != null ? b.getStockHoursAtCalc().doubleValue() : 999.0;
        int stockCompare = Double.compare(aStock, bStock);
        if (stockCompare != 0) return stockCompare;
        
        // 续作优先
        int aContinue = a.getIsContinue() != null ? a.getIsContinue() : 0;
        int bContinue = b.getIsContinue() != null ? b.getIsContinue() : 0;
        if (aContinue != bContinue) {
            return bContinue - aContinue;
        }
        
        // 计划量降序
        return Integer.compare(b.getPlanQuantity(), a.getPlanQuantity());
    }

    /**
     * 优化的车次安排
     */
    private List<ScheduleDetail> arrangeTripsOptimized(List<ScheduleDetail> details) {
        // 按机台和班次分组
        Map<String, List<ScheduleDetail>> groups = details.stream()
            .collect(Collectors.groupingBy(d -> d.getMachineCode() + "_" + d.getShiftCode()));
        
        List<ScheduleDetail> result = Collections.synchronizedList(new ArrayList<>());
        int[] tripCounter = {1};
        
        groups.values().parallelStream().forEach(group -> {
            int tripNo = 1;
            for (ScheduleDetail detail : group) {
                int qty = detail.getPlanQuantity();
                
                // 如果是整车倍数，直接设置车次
                if (qty % DEFAULT_TRIP_CAPACITY == 0) {
                    detail.setTripNo(tripNo++);
                    detail.setTripCapacity(DEFAULT_TRIP_CAPACITY);
                    detail.setTripActualQty(DEFAULT_TRIP_CAPACITY);
                    synchronized (tripCounter) {
                        detail.setTripGroupId("TRIP_" + tripCounter[0]++);
                    }
                }
            }
            
            result.addAll(group);
        });
        
        return result;
    }

    // ==================== 辅助方法 ====================

    private Map<String, MachineLoadContext> initializeMachineLoads(List<Machine> machines) {
        Map<String, MachineLoadContext> loads = new ConcurrentHashMap<>();
        for (Machine machine : machines) {
            MachineLoadContext load = new MachineLoadContext();
            load.setMachine(machine);
            load.setCurrentLoad(0);
            load.setAllocatedMaterials(new HashSet<>());
            load.setStructureSwitchCount(0);
            load.setLastStructure(null);
            loads.put(machine.getMachineCode(), load);
        }
        return loads;
    }

    private String generateCacheKey(DailyEmbryoTask task) {
        return task.getMaterialCode() + "_" + task.getProductStructure();
    }

    private int calculateAllocatableQuantity(TaskAllocationContext task, MachineLoadContext machineLoad) {
        Machine machine = machineLoad.getMachine();
        int remainingCapacity = machine.getMaxDailyCapacity() - machineLoad.getCurrentLoad();
        int taskRemainder = task.getRemainderQuantity();
        return Math.min(remainingCapacity, taskRemainder);
    }

    private void applyAllocation(MachineLoadContext machineLoad, TaskAllocationContext task, int quantity) {
        machineLoad.setCurrentLoad(machineLoad.getCurrentLoad() + quantity);
        machineLoad.getAllocatedMaterials().add(task.getTask().getMaterialCode());
        
        String taskStructure = task.getTask().getProductStructure();
        String lastStructure = machineLoad.getLastStructure();
        if (lastStructure != null && !lastStructure.equals(taskStructure)) {
            machineLoad.setStructureSwitchCount(machineLoad.getStructureSwitchCount() + 1);
        }
        machineLoad.setLastStructure(taskStructure);
        
        task.setRemainderQuantity(task.getRemainderQuantity() - quantity);
    }

    private AllocationSnapshot saveSnapshot(MachineLoadContext machineLoad, TaskAllocationContext task) {
        AllocationSnapshot snapshot = new AllocationSnapshot();
        snapshot.setMachineLoad(machineLoad.getCurrentLoad());
        snapshot.setAllocatedMaterials(new HashSet<>(machineLoad.getAllocatedMaterials()));
        snapshot.setStructureSwitchCount(machineLoad.getStructureSwitchCount());
        snapshot.setLastStructure(machineLoad.getLastStructure());
        snapshot.setTaskRemainder(task.getRemainderQuantity());
        return snapshot;
    }

    private void restoreSnapshot(MachineLoadContext machineLoad, TaskAllocationContext task, AllocationSnapshot snapshot) {
        machineLoad.setCurrentLoad(snapshot.getMachineLoad());
        machineLoad.setAllocatedMaterials(snapshot.getAllocatedMaterials());
        machineLoad.setStructureSwitchCount(snapshot.getStructureSwitchCount());
        machineLoad.setLastStructure(snapshot.getLastStructure());
        task.setRemainderQuantity(snapshot.getTaskRemainder());
    }

    private AllocationDetail createAllocationDetail(DailyEmbryoTask task, Machine machine,
                                                   int quantity, ScheduleMain scheduleMain) {
        AllocationDetail detail = new AllocationDetail();
        detail.setMainId(scheduleMain.getId());
        detail.setScheduleDate(scheduleMain.getScheduleDate());
        detail.setMachineCode(machine.getMachineCode());
        detail.setMaterialCode(task.getMaterialCode());
        detail.setPlanQuantity(quantity);
        return detail;
    }

    private Map<String, Double> parseShiftRatio(String shiftRatio) {
        Map<String, Double> ratioMap = new LinkedHashMap<>();
        String[] parts = shiftRatio.split(":");
        String[] shiftCodes = {"NIGHT", "DAY", "AFTERNOON"};
        for (int i = 0; i < Math.min(parts.length, shiftCodes.length); i++) {
            ratioMap.put(shiftCodes[i], Double.parseDouble(parts[i]));
        }
        return ratioMap;
    }

    private Map<String, Integer> calculateTargetDistribution(int totalQuantity, Map<String, Double> ratioMap) {
        Map<String, Integer> target = new HashMap<>();
        double totalRatio = ratioMap.values().stream().mapToDouble(Double::doubleValue).sum();
        for (Map.Entry<String, Double> entry : ratioMap.entrySet()) {
            int targetQty = (int) (totalQuantity * entry.getValue() / totalRatio);
            target.put(entry.getKey(), targetQty);
        }
        return target;
    }

    private Map<String, Integer> calculateDeviations(Map<String, Integer> current, Map<String, Integer> target) {
        Map<String, Integer> deviations = new HashMap<>();
        for (String shiftCode : target.keySet()) {
            int currentQty = current.getOrDefault(shiftCode, 0);
            int targetQty = target.get(shiftCode);
            deviations.put(shiftCode, currentQty - targetQty);
        }
        return deviations;
    }

    private void logPerformanceMetrics(long startTime, long endTime) {
        perfLogger.info("=== 性能指标 ===");
        perfLogger.info("总耗时: {}ms", (endTime - startTime));
        perfLogger.info("总分配次数: {}", totalAllocations.get());
        perfLogger.info("缓存命中次数: {}", cacheHits.get());
        perfLogger.info("回溯次数: {}", backtrackCount.get());
        perfLogger.info("缓存命中率: {}", 
            String.format("%.2f%%", 
                totalAllocations.get() > 0 ? 
                    (cacheHits.get() * 100.0 / totalAllocations.get()) : 0));
    }

    // ==================== 内部类定义 ====================

    private static class AllocationState {
        int depth = 0;
        int attemptCount = 0;
    }

    private static class TaskAllocationContext {
        private DailyEmbryoTask task;
        private int remainderQuantity;
        public DailyEmbryoTask getTask() { return task; }
        public void setTask(DailyEmbryoTask task) { this.task = task; }
        public int getRemainderQuantity() { return remainderQuantity; }
        public void setRemainderQuantity(int remainderQuantity) { this.remainderQuantity = remainderQuantity; }
    }

    private static class MachineLoadContext {
        private Machine machine;
        private int currentLoad;
        private Set<String> allocatedMaterials;
        private int structureSwitchCount;
        private String lastStructure;
        public Machine getMachine() { return machine; }
        public void setMachine(Machine machine) { this.machine = machine; }
        public int getCurrentLoad() { return currentLoad; }
        public void setCurrentLoad(int currentLoad) { this.currentLoad = currentLoad; }
        public Set<String> getAllocatedMaterials() { return allocatedMaterials; }
        public void setAllocatedMaterials(Set<String> allocatedMaterials) { this.allocatedMaterials = allocatedMaterials; }
        public int getStructureSwitchCount() { return structureSwitchCount; }
        public void setStructureSwitchCount(int structureSwitchCount) { this.structureSwitchCount = structureSwitchCount; }
        public String getLastStructure() { return lastStructure; }
        public void setLastStructure(String lastStructure) { this.lastStructure = lastStructure; }
    }

    private static class AllocationSnapshot {
        private int machineLoad;
        private Set<String> allocatedMaterials;
        private int structureSwitchCount;
        private String lastStructure;
        private int taskRemainder;
        public int getMachineLoad() { return machineLoad; }
        public void setMachineLoad(int machineLoad) { this.machineLoad = machineLoad; }
        public Set<String> getAllocatedMaterials() { return allocatedMaterials; }
        public void setAllocatedMaterials(Set<String> allocatedMaterials) { this.allocatedMaterials = allocatedMaterials; }
        public int getStructureSwitchCount() { return structureSwitchCount; }
        public void setStructureSwitchCount(int structureSwitchCount) { this.structureSwitchCount = structureSwitchCount; }
        public String getLastStructure() { return lastStructure; }
        public void setLastStructure(String lastStructure) { this.lastStructure = lastStructure; }
        public int getTaskRemainder() { return taskRemainder; }
        public void setTaskRemainder(int taskRemainder) { this.taskRemainder = taskRemainder; }
    }

    public static class AllocationResult {
        private Long scheduleMainId;
        private boolean success;
        private String message;
        private List<AllocationDetail> details = new ArrayList<>();
        public Long getScheduleMainId() { return scheduleMainId; }
        public void setScheduleMainId(Long scheduleMainId) { this.scheduleMainId = scheduleMainId; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public List<AllocationDetail> getDetails() { return details; }
        public void setDetails(List<AllocationDetail> details) { this.details = details; }
        public void addDetail(AllocationDetail detail) { this.details.add(detail); }
        public void removeDetail(AllocationDetail detail) { this.details.remove(detail); }
    }

    public static class AllocationDetail {
        private Long mainId;
        private java.time.LocalDate scheduleDate;
        private String machineCode;
        private String materialCode;
        private int planQuantity;
        public Long getMainId() { return mainId; }
        public void setMainId(Long mainId) { this.mainId = mainId; }
        public java.time.LocalDate getScheduleDate() { return scheduleDate; }
        public void setScheduleDate(java.time.LocalDate scheduleDate) { this.scheduleDate = scheduleDate; }
        public String getMachineCode() { return machineCode; }
        public void setMachineCode(String machineCode) { this.machineCode = machineCode; }
        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
        public int getPlanQuantity() { return planQuantity; }
        public void setPlanQuantity(int planQuantity) { this.planQuantity = planQuantity; }
    }
}
