package com.jinyu.aps.service;

import com.jinyu.aps.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * APS核心算法服务
 * 实现技术文档V5.0.0定义的核心算法
 * 
 * 算法包括：
 * 1. 试错分配算法（递归回溯）
 * 2. 班次均衡调整算法
 * 3. 顺位排序算法
 *
 * @author APS Team
 */
@Service
public class AlgorithmService {

    private static final Logger logger = LoggerFactory.getLogger(AlgorithmService.class);

    // 算法参数配置
    private static final int MAX_RECURSION_DEPTH = 20;  // 最大递归深度
    private static final int MAX_SKU_PER_MACHINE_PER_DAY = 4;  // 单台机每日最大SKU种类
    private static final int MAX_STRUCTURE_SWITCH_PER_DAY = 2;  // 每日最大结构切换次数
    private static final int DEFAULT_TRIP_CAPACITY = 12;  // 默认车次容量

    /**
     * ==================== 1. 试错分配算法 ====================
     * 
     * 核心逻辑：
     * 1. 对任务列表按照优先级和库存时长排序
     * 2. 递归地为每个任务选择最优机台分配
     * 3. 约束检查：机台产能、SKU种类限制、结构切换限制
     * 4. 回溯机制：当无有效方案时回退并重试
     * 
     * @param tasks 日胎胚任务列表
     * @param machines 可用机台列表
     * @param scheduleMain 排程主表
     * @return 分配结果
     */
    public AllocationResult allocateTasks(List<DailyEmbryoTask> tasks, List<Machine> machines, ScheduleMain scheduleMain) {
        logger.info("开始试错分配算法，任务数: {}, 机台数: {}", tasks.size(), machines.size());
        
        AllocationResult result = new AllocationResult();
        result.setScheduleMainId(scheduleMain.getId());
        
        // 1. 任务预处理和排序
        List<TaskAllocationContext> taskContexts = prepareTaskContexts(tasks);
        
        // 2. 机台负载初始化
        Map<String, MachineLoadContext> machineLoads = initializeMachineLoads(machines);
        
        // 3. 递归分配
        boolean success = recursiveAllocate(taskContexts, 0, machineLoads, result, scheduleMain);
        
        if (success) {
            result.setSuccess(true);
            result.setMessage("分配成功");
            logger.info("试错分配完成，分配成功");
        } else {
            result.setSuccess(false);
            result.setMessage("无法找到有效分配方案，请检查约束条件");
            logger.warn("试错分配完成，未找到有效方案");
        }
        
        return result;
    }

    /**
     * 准备任务上下文
     * 按照优先级、库存时长、主销产品排序
     */
    private List<TaskAllocationContext> prepareTaskContexts(List<DailyEmbryoTask> tasks) {
        return tasks.stream()
            .map(task -> {
                TaskAllocationContext ctx = new TaskAllocationContext();
                ctx.setTask(task);
                ctx.setRemainderQuantity(task.getTaskQuantity());
                return ctx;
            })
            .sorted((a, b) -> {
                // 1. 优先级排序（升序）
                int priorityCompare = a.getTask().getPriority().compareTo(b.getTask().getPriority());
                if (priorityCompare != 0) return priorityCompare;
                
                // 2. 主销产品优先
                int isMainA = a.getTask().getIsMainProduct() != null ? a.getTask().getIsMainProduct() : 0;
                int isMainB = b.getTask().getIsMainProduct() != null ? b.getTask().getIsMainProduct() : 0;
                if (isMainA != isMainB) {
                    return isMainB - isMainA;  // 主销产品优先
                }
                
                // 3. 任务量排序（降序，大任务优先）
                return b.getTask().getTaskQuantity().compareTo(a.getTask().getTaskQuantity());
            })
            .collect(Collectors.toList());
    }

    /**
     * 初始化机台负载上下文
     */
    private Map<String, MachineLoadContext> initializeMachineLoads(List<Machine> machines) {
        Map<String, MachineLoadContext> loads = new HashMap<>();
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

    /**
     * 递归分配核心逻辑
     */
    private boolean recursiveAllocate(List<TaskAllocationContext> tasks, int taskIndex, 
                                     Map<String, MachineLoadContext> machineLoads,
                                     AllocationResult result, ScheduleMain scheduleMain) {
        // 基准情况：所有任务已分配完成
        if (taskIndex >= tasks.size()) {
            return true;
        }
        
        TaskAllocationContext currentTask = tasks.get(taskIndex);
        
        // 获取候选机台列表（按负载升序）
        List<MachineLoadContext> candidateMachines = machineLoads.values().stream()
            .filter(load -> canAllocateToMachine(currentTask, load))
            .sorted(Comparator.comparingInt(MachineLoadContext::getCurrentLoad))
            .collect(Collectors.toList());
        
        // 尝试每个候选机台
        for (MachineLoadContext machineLoad : candidateMachines) {
            // 计算本机台可分配量
            int allocatableQty = calculateAllocatableQuantity(currentTask, machineLoad);
            
            if (allocatableQty <= 0) {
                continue;
            }
            
            // 记录状态用于回溯
            AllocationSnapshot snapshot = saveSnapshot(machineLoad, currentTask);
            
            // 执行分配
            applyAllocation(machineLoad, currentTask, allocatableQty);
            
            // 记录分配结果
            AllocationDetail detail = createAllocationDetail(
                currentTask.getTask(), machineLoad.getMachine(), allocatableQty, scheduleMain);
            result.addDetail(detail);
            
            // 记录日志
            logAllocationAttempt(currentTask, machineLoad, allocatableQty, true);
            
            // 递归处理下一个任务
            if (recursiveAllocate(tasks, taskIndex + 1, machineLoads, result, scheduleMain)) {
                return true;
            }
            
            // 回溯
            restoreSnapshot(machineLoad, currentTask, snapshot);
            result.removeDetail(detail);
            logAllocationAttempt(currentTask, machineLoad, allocatableQty, false);
        }
        
        // 当前任务无法分配，检查是否可以跳过（部分分配场景）
        if (currentTask.getRemainderQuantity() < currentTask.getTask().getTaskQuantity()) {
            // 部分已分配，继续下一个任务
            return recursiveAllocate(tasks, taskIndex + 1, machineLoads, result, scheduleMain);
        }
        
        return false;
    }

    /**
     * 检查任务是否可以分配到指定机台
     */
    private boolean canAllocateToMachine(TaskAllocationContext task, MachineLoadContext machineLoad) {
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
        
        return true;
    }

    /**
     * 计算机台可分配量
     */
    private int calculateAllocatableQuantity(TaskAllocationContext task, MachineLoadContext machineLoad) {
        Machine machine = machineLoad.getMachine();
        int remainingCapacity = machine.getMaxDailyCapacity() - machineLoad.getCurrentLoad();
        int taskRemainder = task.getRemainderQuantity();
        
        return Math.min(remainingCapacity, taskRemainder);
    }

    /**
     * 应用分配
     */
    private void applyAllocation(MachineLoadContext machineLoad, TaskAllocationContext task, int quantity) {
        machineLoad.setCurrentLoad(machineLoad.getCurrentLoad() + quantity);
        machineLoad.getAllocatedMaterials().add(task.getTask().getMaterialCode());
        
        // 更新结构切换计数
        String taskStructure = task.getTask().getProductStructure();
        String lastStructure = machineLoad.getLastStructure();
        if (lastStructure != null && !lastStructure.equals(taskStructure)) {
            machineLoad.setStructureSwitchCount(machineLoad.getStructureSwitchCount() + 1);
        }
        machineLoad.setLastStructure(taskStructure);
        
        // 更新任务剩余量
        task.setRemainderQuantity(task.getRemainderQuantity() - quantity);
    }

    /**
     * 保存快照用于回溯
     */
    private AllocationSnapshot saveSnapshot(MachineLoadContext machineLoad, TaskAllocationContext task) {
        AllocationSnapshot snapshot = new AllocationSnapshot();
        snapshot.setMachineLoad(machineLoad.getCurrentLoad());
        snapshot.setAllocatedMaterials(new HashSet<>(machineLoad.getAllocatedMaterials()));
        snapshot.setStructureSwitchCount(machineLoad.getStructureSwitchCount());
        snapshot.setLastStructure(machineLoad.getLastStructure());
        snapshot.setTaskRemainder(task.getRemainderQuantity());
        return snapshot;
    }

    /**
     * 恢复快照
     */
    private void restoreSnapshot(MachineLoadContext machineLoad, TaskAllocationContext task, AllocationSnapshot snapshot) {
        machineLoad.setCurrentLoad(snapshot.getMachineLoad());
        machineLoad.setAllocatedMaterials(snapshot.getAllocatedMaterials());
        machineLoad.setStructureSwitchCount(snapshot.getStructureSwitchCount());
        machineLoad.setLastStructure(snapshot.getLastStructure());
        task.setRemainderQuantity(snapshot.getTaskRemainder());
    }

    /**
     * 创建分配明细
     */
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

    /**
     * 记录分配尝试日志
     */
    private void logAllocationAttempt(TaskAllocationContext task, MachineLoadContext machineLoad, 
                                     int quantity, boolean success) {
        logger.debug("分配尝试 - 任务: {}, 机台: {}, 数量: {}, 结果: {}",
            task.getTask().getMaterialCode(),
            machineLoad.getMachine().getMachineCode(),
            quantity,
            success ? "成功" : "回溯");
    }

    /**
     * ==================== 2. 班次均衡调整算法 ====================
     * 
     * 核心逻辑：
     * 1. 计算当前各班次产量
     * 2. 检查偏差是否超过阈值
     * 3. 通过调整达到均衡（向偏差大的班次移动任务）
     * 
     * @param details 已分配的排程明细
     * @param shiftRatio 班次分配比例（如"1:2:1"）
     * @return 调整后的排程明细
     */
    public List<ScheduleDetail> balanceShiftDistribution(List<ScheduleDetail> details, String shiftRatio) {
        logger.info("开始班次均衡调整算法，明细数: {}", details.size());
        
        // 1. 解析班次比例
        Map<String, Double> ratioMap = parseShiftRatio(shiftRatio);
        
        // 2. 计算当前各班次产量
        Map<String, Integer> currentDistribution = calculateShiftDistribution(details);
        
        // 3. 计算目标产量
        int totalQuantity = details.stream().mapToInt(ScheduleDetail::getPlanQuantity).sum();
        Map<String, Integer> targetDistribution = calculateTargetDistribution(totalQuantity, ratioMap);
        
        // 4. 检查偏差
        Map<String, Integer> deviations = calculateDeviations(currentDistribution, targetDistribution);
        
        // 5. 执行调整
        boolean needsAdjustment = deviations.values().stream()
            .anyMatch(d -> Math.abs(d) > totalQuantity * 0.05);  // 偏差超过5%
        
        if (needsAdjustment) {
            details = performShiftAdjustment(details, currentDistribution, targetDistribution, deviations);
            logger.info("班次均衡调整完成");
        } else {
            logger.info("班次分布已均衡，无需调整");
        }
        
        return details;
    }

    /**
     * 解析班次比例
     */
    private Map<String, Double> parseShiftRatio(String shiftRatio) {
        Map<String, Double> ratioMap = new LinkedHashMap<>();
        String[] parts = shiftRatio.split(":");
        
        String[] shiftCodes = {"NIGHT", "DAY", "AFTERNOON"};
        for (int i = 0; i < Math.min(parts.length, shiftCodes.length); i++) {
            ratioMap.put(shiftCodes[i], Double.parseDouble(parts[i]));
        }
        
        return ratioMap;
    }

    /**
     * 计算当前班次分布
     */
    private Map<String, Integer> calculateShiftDistribution(List<ScheduleDetail> details) {
        Map<String, Integer> distribution = new HashMap<>();
        for (ScheduleDetail detail : details) {
            String shiftCode = detail.getShiftCode();
            distribution.merge(shiftCode, detail.getPlanQuantity(), Integer::sum);
        }
        return distribution;
    }

    /**
     * 计算目标班次分布
     */
    private Map<String, Integer> calculateTargetDistribution(int totalQuantity, Map<String, Double> ratioMap) {
        Map<String, Integer> target = new HashMap<>();
        double totalRatio = ratioMap.values().stream().mapToDouble(Double::doubleValue).sum();
        
        for (Map.Entry<String, Double> entry : ratioMap.entrySet()) {
            int targetQty = (int) (totalQuantity * entry.getValue() / totalRatio);
            target.put(entry.getKey(), targetQty);
        }
        
        return target;
    }

    /**
     * 计算偏差
     */
    private Map<String, Integer> calculateDeviations(Map<String, Integer> current, Map<String, Integer> target) {
        Map<String, Integer> deviations = new HashMap<>();
        for (String shiftCode : target.keySet()) {
            int currentQty = current.getOrDefault(shiftCode, 0);
            int targetQty = target.get(shiftCode);
            deviations.put(shiftCode, currentQty - targetQty);
        }
        return deviations;
    }

    /**
     * 执行班次调整
     */
    private List<ScheduleDetail> performShiftAdjustment(List<ScheduleDetail> details,
                                                       Map<String, Integer> current,
                                                       Map<String, Integer> target,
                                                       Map<String, Integer> deviations) {
        // 找出过剩和不足的班次
        List<String> surplusShifts = deviations.entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        List<String> deficitShifts = deviations.entrySet().stream()
            .filter(e -> e.getValue() < 0)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // 从过剩班次向不足班次转移任务
        for (String surplusShift : surplusShifts) {
            for (String deficitShift : deficitShifts) {
                int transferQty = Math.min(
                    deviations.get(surplusShift),
                    -deviations.get(deficitShift)
                );
                
                if (transferQty > 0) {
                    details = transferBetweenShifts(details, surplusShift, deficitShift, transferQty);
                    
                    // 更新偏差
                    deviations.put(surplusShift, deviations.get(surplusShift) - transferQty);
                    deviations.put(deficitShift, deviations.get(deficitShift) + transferQty);
                }
            }
        }
        
        return details;
    }

    /**
     * 在班次间转移任务
     */
    private List<ScheduleDetail> transferBetweenShifts(List<ScheduleDetail> details,
                                                       String fromShift, String toShift, int quantity) {
        // 找出可转移的明细（优先选择可合并到目标班次的）
        List<ScheduleDetail> transferable = details.stream()
            .filter(d -> d.getShiftCode().equals(fromShift))
            .filter(d -> d.getPlanQuantity() <= quantity)
            .sorted(Comparator.comparingInt(ScheduleDetail::getPlanQuantity).reversed())
            .collect(Collectors.toList());
        
        int remaining = quantity;
        for (ScheduleDetail detail : transferable) {
            if (remaining <= 0) break;
            
            int transferAmount = Math.min(detail.getPlanQuantity(), remaining);
            detail.setShiftCode(toShift);
            remaining -= transferAmount;
            
            logger.debug("转移任务: 物料{}从{}班转移到{}班，数量{}",
                detail.getMaterialCode(), fromShift, toShift, transferAmount);
        }
        
        return details;
    }

    /**
     * ==================== 3. 顺位排序算法 ====================
     * 
     * 核心逻辑：
     * 1. 基于库存可供硫化时长确定优先级
     * 2. 低库存优先生产（避免硫化停工）
     * 3. 同组物料按车次齐套排序
     * 4. 考虑续作相同结构优先
     * 
     * @param details 待排序的排程明细
     * @param stockInfo 库存信息
     * @return 排序后的明细
     */
    public List<ScheduleDetail> sortScheduleSequence(List<ScheduleDetail> details, Map<String, Double> stockHours) {
        logger.info("开始顺位排序算法，明细数: {}", details.size());
        
        // 1. 为每个明细计算排序分数
        for (ScheduleDetail detail : details) {
            // 如果stockHours中没有该物料，使用默认值999.0（排在后面）
            double stockHour = stockHours.containsKey(detail.getMaterialCode()) 
                ? stockHours.get(detail.getMaterialCode()) 
                : 999.0;
            detail.setStockHoursAtCalc(BigDecimal.valueOf(stockHour));
        }
        
        // 2. 按机台分组
        Map<String, List<ScheduleDetail>> byMachine = details.stream()
            .collect(Collectors.groupingBy(ScheduleDetail::getMachineCode));
        
        // 3. 对每个机台的明细进行排序
        List<ScheduleDetail> sortedDetails = new ArrayList<>();
        int globalSequence = 1;
        
        for (Map.Entry<String, List<ScheduleDetail>> entry : byMachine.entrySet()) {
            List<ScheduleDetail> machineDetails = entry.getValue();
            
            // 排序规则：
            // 1. 库存时长升序（低库存优先）
            // 2. 续作相同结构优先
            // 3. 主销产品优先
            machineDetails.sort((a, b) -> {
                // 库存时长比较（升序）
                double aStock = a.getStockHoursAtCalc() != null ? a.getStockHoursAtCalc().doubleValue() : 999.0;
                double bStock = b.getStockHoursAtCalc() != null ? b.getStockHoursAtCalc().doubleValue() : 999.0;
                int stockCompare = Double.compare(aStock, bStock);
                if (stockCompare != 0) return stockCompare;
                
                // 是否续作（续作优先）
                int aContinue = a.getIsContinue() != null ? a.getIsContinue() : 0;
                int bContinue = b.getIsContinue() != null ? b.getIsContinue() : 0;
                if (aContinue != bContinue) {
                    return bContinue - aContinue;  // 续作优先
                }
                
                // 计划量（大任务优先）
                return Integer.compare(b.getPlanQuantity(), a.getPlanQuantity());
            });
            
            // 分配顺位
            for (int i = 0; i < machineDetails.size(); i++) {
                ScheduleDetail detail = machineDetails.get(i);
                detail.setSequence(globalSequence++);
                detail.setSequenceInGroup(i + 1);
            }
            
            sortedDetails.addAll(machineDetails);
        }
        
        // 4. 车次齐套处理
        sortedDetails = arrangeTrips(sortedDetails);
        
        logger.info("顺位排序完成，总顺位数: {}", globalSequence - 1);
        return sortedDetails;
    }

    /**
     * 安排车次
     * 将每12条作为一个车次，确保齐套
     */
    private List<ScheduleDetail> arrangeTrips(List<ScheduleDetail> details) {
        // 按机台和班次分组，使用LinkedHashMap保持插入顺序
        Map<String, List<ScheduleDetail>> groups = details.stream()
            .collect(Collectors.groupingBy(d -> 
                d.getMachineCode() + "_" + d.getShiftCode(),
                LinkedHashMap::new,
                Collectors.toList()));
        
        List<ScheduleDetail> result = new ArrayList<>();
        int tripCounter = 1;
        int sequenceCounter = 1; // 全局顺位计数器
        
        for (List<ScheduleDetail> group : groups.values()) {
            // 按之前的sequence排序，保持续作优先等排序结果
            group.sort(Comparator.comparingInt(d -> 
                d.getSequence() != null ? d.getSequence() : Integer.MAX_VALUE));
            
            int tripNo = 1;
            
            for (ScheduleDetail detail : group) {
                int qty = detail.getPlanQuantity();
                int tripsNeeded = (int) Math.ceil((double) qty / DEFAULT_TRIP_CAPACITY);
                
                for (int t = 0; t < tripsNeeded; t++) {
                    int tripQty = Math.min(DEFAULT_TRIP_CAPACITY, qty - t * DEFAULT_TRIP_CAPACITY);
                    
                    // 如果是第一个车次，更新原明细
                    if (t == 0) {
                        detail.setTripNo(tripNo);
                        detail.setTripCapacity(DEFAULT_TRIP_CAPACITY);
                        detail.setTripActualQty(tripQty);
                        detail.setTripGroupId("TRIP_" + tripCounter++);
                        detail.setSequence(sequenceCounter++); // 重新分配顺位
                        result.add(detail);  // 直接添加到result，保持顺序
                    } else {
                        // 创建新的明细用于后续车次
                        ScheduleDetail newDetail = cloneDetail(detail);
                        newDetail.setTripNo(tripNo);
                        newDetail.setTripCapacity(DEFAULT_TRIP_CAPACITY);
                        newDetail.setTripActualQty(tripQty);
                        newDetail.setTripGroupId("TRIP_" + tripCounter++);
                        newDetail.setPlanQuantity(tripQty);
                        newDetail.setSequence(sequenceCounter++); // 分配唯一顺位
                        result.add(newDetail);
                    }
                }
                
                tripNo++;
            }
        }
        
        return result;
    }

    /**
     * 克隆明细
     */
    private ScheduleDetail cloneDetail(ScheduleDetail source) {
        ScheduleDetail target = new ScheduleDetail();
        target.setMainId(source.getMainId());
        target.setScheduleDate(source.getScheduleDate());
        target.setShiftCode(source.getShiftCode());
        target.setMachineCode(source.getMachineCode());
        target.setMaterialCode(source.getMaterialCode());
        target.setSequence(source.getSequence());
        target.setStatus(source.getStatus());
        target.setIsContinue(source.getIsContinue());  // 复制续作标记
        target.setStockHoursAtCalc(source.getStockHoursAtCalc());  // 复制库存时长
        return target;
    }

    // ==================== 内部类定义 ====================

    /**
     * 任务分配上下文
     */
    private static class TaskAllocationContext {
        private DailyEmbryoTask task;
        private int remainderQuantity;

        public DailyEmbryoTask getTask() { return task; }
        public void setTask(DailyEmbryoTask task) { this.task = task; }
        public int getRemainderQuantity() { return remainderQuantity; }
        public void setRemainderQuantity(int remainderQuantity) { this.remainderQuantity = remainderQuantity; }
    }

    /**
     * 机台负载上下文
     */
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

    /**
     * 分配快照（用于回溯）
     */
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

    /**
     * 分配结果
     */
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

    /**
     * 分配明细
     */
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
