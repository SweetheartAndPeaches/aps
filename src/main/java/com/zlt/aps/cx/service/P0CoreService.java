package com.zlt.aps.cx.service;

import com.zlt.aps.cx.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * P0核心功能服务
 * 
 * 实现三个核心业务功能：
 * 1. 收尾管理（紧急收尾标签优先级）
 * 2. 关键产品开产首班不排
 * 3. 精度计划机台停机处理
 *
 * @author APS Team
 */
@Service
public class P0CoreService {

    private static final Logger logger = LoggerFactory.getLogger(P0CoreService.class);

    // ==================== 配置常量 ====================
    
    // 紧急收尾阈值（天）- 3天内收尾为紧急
    private static final int URGENT_ENDING_THRESHOLD_DAYS = 3;
    
    // 近期收尾阈值（天）- 10天内收尾
    private static final int NEAR_ENDING_THRESHOLD_DAYS = 10;
    
    // 延误平摊天数
    private static final int DELAY_DISTRIBUTION_DAYS = 3;
    
    // 精度校验默认时长（小时）
    private static final int PRECISION_DURATION_HOURS = 4;
    
    // 库存安全阈值（小时）- 库存够吃4小时以上才安全
    private static final double STOCK_SAFETY_HOURS = 4.0;
    
    // 库存预警阈值（小时）- 库存超过18小时预警
    private static final double STOCK_WARNING_HOURS = 18.0;
    
    // 开产首班时长（小时）
    private static final int STARTING_FIRST_SHIFT_HOURS = 6;
    
    // 正常班次时长（小时）
    private static final int NORMAL_SHIFT_HOURS = 8;

    // ==================== 1. 收尾管理功能 ====================

    /**
     * 计算结构收尾状态
     * 
     * @param vulcanizingRemainder 硫化余量（条）
     * @param embryoStock 胎胚库存（条）
     * @param dailyCapacity 当前日产能（条/天）
     * @return 收尾状态信息
     */
    public StructureEnding calculateEndingStatus(int vulcanizingRemainder, int embryoStock, int dailyCapacity) {
        logger.debug("计算收尾状态: 硫化余量={}, 胎胚库存={}, 日产能={}", 
            vulcanizingRemainder, embryoStock, dailyCapacity);
        
        StructureEnding ending = new StructureEnding();
        ending.setVulcanizingRemainder(vulcanizingRemainder);
        ending.setEmbryoStock(embryoStock);
        ending.setDailyCapacity(dailyCapacity);
        ending.setStatDate(LocalDate.now());
        
        // 计算成型余量 = 硫化余量 - 胎胚库存
        int formingRemainder = Math.max(0, vulcanizingRemainder - embryoStock);
        ending.setFormingRemainder(formingRemainder);
        
        // 计算预计收尾天数
        if (dailyCapacity > 0 && formingRemainder > 0) {
            BigDecimal endingDays = BigDecimal.valueOf(formingRemainder)
                .divide(BigDecimal.valueOf(dailyCapacity), 2, RoundingMode.HALF_UP);
            ending.setEstimatedEndingDays(endingDays);
            
            LocalDate plannedEndDate = LocalDate.now().plusDays(endingDays.intValue());
            ending.setPlannedEndingDate(plannedEndDate);
            
            // 判断是否紧急收尾（3天内）
            if (endingDays.compareTo(BigDecimal.valueOf(URGENT_ENDING_THRESHOLD_DAYS)) <= 0) {
                ending.setIsUrgentEnding(1);
                logger.info("结构进入紧急收尾状态，预计{}天收尾", endingDays);
            } else {
                ending.setIsUrgentEnding(0);
            }
            
            // 判断是否近期收尾（10天内）
            if (endingDays.compareTo(BigDecimal.valueOf(NEAR_ENDING_THRESHOLD_DAYS)) <= 0) {
                ending.setIsNearEnding(1);
                
                // 计算延误量：如果10天内做不完，计算延误
                int tenDayCapacity = dailyCapacity * NEAR_ENDING_THRESHOLD_DAYS;
                if (formingRemainder > tenDayCapacity) {
                    int delayQuantity = formingRemainder - tenDayCapacity;
                    ending.setDelayQuantity(delayQuantity);
                    
                    // 计算平摊到未来3天的量
                    int distributedQty = (int) Math.ceil((double) delayQuantity / DELAY_DISTRIBUTION_DAYS);
                    ending.setDistributedQuantity(distributedQty);
                    
                    // 判断是否需要调整月计划
                    int threeDayFullCapacity = dailyCapacity * DELAY_DISTRIBUTION_DAYS;
                    if (delayQuantity > threeDayFullCapacity) {
                        ending.setNeedMonthPlanAdjust(1);
                        logger.warn("延误量{}超过3天产能{}，需要调整月计划", delayQuantity, threeDayFullCapacity);
                    }
                }
            } else {
                ending.setIsNearEnding(0);
            }
        } else {
            // 无需生产
            ending.setEstimatedEndingDays(BigDecimal.ZERO);
            ending.setIsUrgentEnding(0);
            ending.setIsNearEnding(0);
        }
        
        return ending;
    }

    /**
     * 批量计算所有结构的收尾状态
     * 
     * @param tasks 日胎胚任务列表
     * @param stockMap 库存映射
     * @param vulcanizingRemainderMap 硫化余量映射（按结构）
     * @return 结构收尾状态映射
     */
    public Map<String, StructureEnding> calculateAllEndingStatus(
            List<DailyEmbryoTask> tasks,
            Map<String, Integer> stockMap,
            Map<String, Integer> vulcanizingRemainderMap) {
        
        logger.info("开始计算所有结构收尾状态...");
        
        Map<String, StructureEnding> result = new HashMap<>();
        
        // 按结构分组统计日产能
        Map<String, Integer> structureCapacity = tasks.stream()
            .collect(Collectors.groupingBy(
                DailyEmbryoTask::getProductStructure,
                Collectors.summingInt(DailyEmbryoTask::getTaskQuantity)
            ));
        
        for (Map.Entry<String, Integer> entry : structureCapacity.entrySet()) {
            String structure = entry.getKey();
            int dailyCapacity = entry.getValue();
            int embryoStock = stockMap.getOrDefault(structure, 0);
            int vulcanRemainder = vulcanizingRemainderMap.getOrDefault(structure, 0);
            
            StructureEnding ending = calculateEndingStatus(vulcanRemainder, embryoStock, dailyCapacity);
            ending.setStructureCode(structure);
            result.put(structure, ending);
        }
        
        // 统计紧急收尾数量
        long urgentCount = result.values().stream()
            .filter(e -> e.getIsUrgentEnding() == 1)
            .count();
        
        logger.info("收尾状态计算完成，紧急收尾结构数: {}", urgentCount);
        
        return result;
    }

    /**
     * 为任务标记紧急收尾标签
     * 
     * @param tasks 任务列表
     * @param endingStatusMap 收尾状态映射
     */
    public void markUrgentEndingTasks(List<DailyEmbryoTask> tasks, Map<String, StructureEnding> endingStatusMap) {
        for (DailyEmbryoTask task : tasks) {
            String structure = task.getProductStructure();
            StructureEnding ending = endingStatusMap.get(structure);
            
            if (ending != null && ending.getIsUrgentEnding() == 1) {
                task.setIsUrgentEnding(1);
                task.setEndingDays(ending.getEstimatedEndingDays());
                // 紧急收尾任务提升优先级
                if (task.getPriority() == null || task.getPriority() > 1) {
                    task.setPriority(1);
                }
                logger.debug("任务 {} 标记为紧急收尾，优先级提升为1", task.getMaterialCode());
            } else {
                task.setIsUrgentEnding(0);
            }
        }
    }

    // ==================== 2. 关键产品开产首班不排功能 ====================

    /**
     * 判断是否需要跳过关键产品的首班排产
     * 
     * 规则：
     * 1. 如果是开产首日
     * 2. 且该任务是关键产品
     * 3. 且是第一个班次
     * 4. 且该结构下有其他非关键产品可做
     * 则跳过该关键产品的首班排产
     * 
     * @param task 任务
     * @param isStartingDay 是否开产首日
     * @param shiftCode 班次
     * @param structureHasOtherProducts 该结构下是否有其他非关键产品
     * @return 是否跳过
     */
    public boolean shouldSkipKeyProductFirstShift(
            DailyEmbryoTask task, 
            boolean isStartingDay, 
            String shiftCode,
            boolean structureHasOtherProducts) {
        
        // 非开产首日，不跳过
        if (!isStartingDay) {
            return false;
        }
        
        // 非关键产品，不跳过
        if (task.getIsKeyProduct() == null || task.getIsKeyProduct() != 1) {
            return false;
        }
        
        // 非首班（早班），不跳过
        if (!"DAY".equals(shiftCode)) {
            return false;
        }
        
        // 如果该结构只有这一个关键产品，没有其他产品可做，则不能跳过
        if (!structureHasOtherProducts) {
            logger.info("关键产品 {} 所在结构无其他产品可做，首班必须排产", task.getMaterialCode());
            return false;
        }
        
        logger.info("关键产品 {} 开产首班跳过排产", task.getMaterialCode());
        return true;
    }

    /**
     * 检查结构下是否有其他非关键产品
     * 
     * @param tasks 任务列表
     * @param structure 产品结构
     * @param excludeMaterialCode 排除的物料编码（当前关键产品）
     * @return 是否有其他非关键产品
     */
    public boolean hasOtherNonKeyProducts(List<DailyEmbryoTask> tasks, String structure, String excludeMaterialCode) {
        return tasks.stream()
            .anyMatch(t -> 
                structure.equals(t.getProductStructure()) &&
                !excludeMaterialCode.equals(t.getMaterialCode()) &&
                (t.getIsKeyProduct() == null || t.getIsKeyProduct() != 1)
            );
    }

    /**
     * 过滤首班需跳过的关键产品任务
     * 
     * @param tasks 任务列表
     * @param isStartingDay 是否开产首日
     * @param shiftCode 班次
     * @return 过滤后的任务列表
     */
    public List<DailyEmbryoTask> filterKeyProductsForFirstShift(
            List<DailyEmbryoTask> tasks, 
            boolean isStartingDay, 
            String shiftCode) {
        
        if (!isStartingDay || !"DAY".equals(shiftCode)) {
            return tasks;
        }
        
        // 按结构分组
        Map<String, List<DailyEmbryoTask>> byStructure = tasks.stream()
            .collect(Collectors.groupingBy(DailyEmbryoTask::getProductStructure));
        
        List<DailyEmbryoTask> result = new ArrayList<>();
        
        for (Map.Entry<String, List<DailyEmbryoTask>> entry : byStructure.entrySet()) {
            String structure = entry.getKey();
            List<DailyEmbryoTask> structureTasks = entry.getValue();
            
            for (DailyEmbryoTask task : structureTasks) {
                boolean hasOther = hasOtherNonKeyProducts(tasks, structure, task.getMaterialCode());
                
                if (!shouldSkipKeyProductFirstShift(task, isStartingDay, shiftCode, hasOther)) {
                    result.add(task);
                }
            }
        }
        
        logger.info("开产首班过滤完成，原任务数: {}, 过滤后: {}", tasks.size(), result.size());
        return result;
    }

    // ==================== 3. 精度计划机台停机处理功能 ====================

    /**
     * 获取精度计划影响的机台
     * 
     * @param precisionPlans 精度计划列表
     * @param scheduleDate 排程日期
     * @param shiftCode 班次（可选，null表示不限制班次）
     * @return 受影响的机台编码集合
     */
    public Set<String> getAffectedMachinesByPrecision(
            List<PrecisionPlan> precisionPlans, 
            LocalDate scheduleDate, 
            String shiftCode) {
        
        return precisionPlans.stream()
            .filter(p -> scheduleDate.equals(p.getPlanDate()))
            .filter(p -> shiftCode == null || shiftCode.equals(p.getPlanShift()))
            .filter(p -> "PLANNED".equals(p.getStatus()) || "IN_PROGRESS".equals(p.getStatus()))
            .map(PrecisionPlan::getMachineCode)
            .collect(Collectors.toSet());
    }

    /**
     * 获取机台的精度时间段
     * 
     * @param precisionPlans 精度计划列表
     * @param machineCode 机台编码
     * @param scheduleDate 排程日期
     * @return 精度时间段列表
     */
    public List<PrecisionTimeSlot> getPrecisionTimeSlots(
            List<PrecisionPlan> precisionPlans, 
            String machineCode, 
            LocalDate scheduleDate) {
        
        return precisionPlans.stream()
            .filter(p -> machineCode.equals(p.getMachineCode()))
            .filter(p -> scheduleDate.equals(p.getPlanDate()))
            .filter(p -> "PLANNED".equals(p.getStatus()) || "IN_PROGRESS".equals(p.getStatus()))
            .map(p -> new PrecisionTimeSlot(
                p.getPlanShift(),
                p.getStartTime(),
                p.getEndTime(),
                p.getDurationHours()
            ))
            .collect(Collectors.toList());
    }

    /**
     * 计算精度期间的可用产能
     * 
     * @param machine 机台
     * @param precisionTimeSlots 精度时间段列表
     * @param shiftCode 班次
     * @return 可用产能（条/班）
     */
    public int calculateAvailableCapacityDuringPrecision(
            Machine machine, 
            List<PrecisionTimeSlot> precisionTimeSlots,
            String shiftCode) {
        
        int dailyCapacity = machine.getMaxDailyCapacity() != null ? machine.getMaxDailyCapacity() : 0;
        int shiftCapacity = dailyCapacity / 3; // 三个班次均分
        
        // 检查该班次是否有精度计划
        boolean hasPrecisionInShift = precisionTimeSlots.stream()
            .anyMatch(slot -> shiftCode.equals(slot.shiftCode));
        
        if (hasPrecisionInShift) {
            // 精度占用4小时，班次8小时，剩余4小时
            // 产能按比例折算：剩余4小时/班次8小时 = 50%
            int availableCapacity = shiftCapacity / 2;
            logger.info("机台 {} 班次 {} 有精度计划，产能从 {} 降至 {}", 
                machine.getMachineCode(), shiftCode, shiftCapacity, availableCapacity);
            return availableCapacity;
        }
        
        return shiftCapacity;
    }

    /**
     * 判断精度期间库存是否足够
     * 
     * @param embryoStock 胎胚库存（条）
     * @param vulcanizingRate 硫化速率（条/小时）
     * @param precisionDuration 精度时长（小时）
     * @return 库存是否足够支撑精度期间
     */
    public boolean isStockSufficientDuringPrecision(
            int embryoStock, 
            double vulcanizingRate, 
            int precisionDuration) {
        
        // 库存可供硫化时长
        double stockHours = vulcanizingRate > 0 ? embryoStock / vulcanizingRate : Double.MAX_VALUE;
        
        // 如果库存够吃4小时以上，则安全
        boolean sufficient = stockHours >= STOCK_SAFETY_HOURS;
        
        if (!sufficient) {
            logger.warn("精度期间库存不足，库存{}条，可供{}小时，精度时长{}小时", 
                embryoStock, stockHours, precisionDuration);
        }
        
        return sufficient;
    }

    /**
     * 计算精度期间需要的硫化减产比例
     * 
     * @param embryoStock 胎胚库存（条）
     * @param vulcanizingRate 硫化速率（条/小时）
     * @param precisionDuration 精度时长（小时）
     * @return 减产比例（0.0-1.0，0表示不减产，0.5表示减半）
     */
    public double calculateVulcanizingReductionRatio(
            int embryoStock, 
            double vulcanizingRate, 
            int precisionDuration) {
        
        double stockHours = vulcanizingRate > 0 ? embryoStock / vulcanizingRate : Double.MAX_VALUE;
        
        if (stockHours >= precisionDuration) {
            // 库存够吃整个精度时长，不需要减产
            return 0.0;
        }
        
        if (stockHours < STOCK_SAFETY_HOURS) {
            // 库存不够4小时，建议硫化减半
            logger.warn("建议硫化减产50%以配合精度计划");
            return 0.5;
        }
        
        // 库存介于安全线和精度时长之间，部分减产
        double ratio = 1.0 - (stockHours / precisionDuration);
        return Math.min(0.5, ratio); // 最高减半
    }

    /**
     * 从可用机台列表中排除精度期间的机台（或调整产能）
     * 
     * @param machines 机台列表
     * @param precisionPlans 精度计划列表
     * @param scheduleDate 排程日期
     * @param shiftCode 班次
     * @param adjustmentMode 调整模式：EXCLUDE(完全排除) / REDUCE_CAPACITY(降低产能)
     * @return 调整后的机台列表
     */
    public List<Machine> adjustMachinesForPrecision(
            List<Machine> machines,
            List<PrecisionPlan> precisionPlans,
            LocalDate scheduleDate,
            String shiftCode,
            String adjustmentMode) {
        
        Set<String> affectedMachines = getAffectedMachinesByPrecision(precisionPlans, scheduleDate, shiftCode);
        
        if (affectedMachines.isEmpty()) {
            return machines;
        }
        
        List<Machine> result = new ArrayList<>();
        
        for (Machine machine : machines) {
            if (affectedMachines.contains(machine.getMachineCode())) {
                if ("EXCLUDE".equals(adjustmentMode)) {
                    logger.info("机台 {} 因精度计划被排除", machine.getMachineCode());
                    // 完全排除
                } else {
                    // 降低产能模式
                    List<PrecisionTimeSlot> slots = getPrecisionTimeSlots(
                        precisionPlans, machine.getMachineCode(), scheduleDate);
                    int adjustedCapacity = calculateAvailableCapacityDuringPrecision(
                        machine, slots, shiftCode);
                    
                    // 创建产能调整后的机台副本
                    Machine adjustedMachine = cloneMachine(machine);
                    // 班次产能调整为精度后的可用产能
                    adjustedMachine.setMaxDailyCapacity(adjustedCapacity * 3);
                    result.add(adjustedMachine);
                    
                    logger.info("机台 {} 因精度计划产能调整为 {}", 
                        machine.getMachineCode(), adjustedCapacity);
                }
            } else {
                result.add(machine);
            }
        }
        
        return result;
    }

    /**
     * 克隆机台（用于产能调整）
     */
    private Machine cloneMachine(Machine source) {
        Machine target = new Machine();
        target.setId(source.getId());
        target.setMachineCode(source.getMachineCode());
        target.setMachineName(source.getMachineName());
        target.setMachineType(source.getMachineType());
        target.setWrappingType(source.getWrappingType());
        target.setHasZeroDegreeFeeder(source.getHasZeroDegreeFeeder());
        target.setStructure(source.getStructure());
        target.setMaxCapacityPerHour(source.getMaxCapacityPerHour());
        target.setMaxDailyCapacity(source.getMaxDailyCapacity());
        target.setMaxCuringMachines(source.getMaxCuringMachines());
        target.setFixedStructure1(source.getFixedStructure1());
        target.setFixedStructure2(source.getFixedStructure2());
        target.setFixedStructure3(source.getFixedStructure3());
        target.setRestrictedStructures(source.getRestrictedStructures());
        target.setProductionRestriction(source.getProductionRestriction());
        target.setLineNumber(source.getLineNumber());
        target.setStatus(source.getStatus());
        target.setIsActive(source.getIsActive());
        return target;
    }

    // ==================== 辅助类 ====================

    /**
     * 精度时间段
     */
    public static class PrecisionTimeSlot {
        public final String shiftCode;
        public final LocalDateTime startTime;
        public final LocalDateTime endTime;
        public final int durationHours;

        public PrecisionTimeSlot(String shiftCode, LocalDateTime startTime, LocalDateTime endTime, int durationHours) {
            this.shiftCode = shiftCode;
            this.startTime = startTime;
            this.endTime = endTime;
            this.durationHours = durationHours;
        }
    }
}
