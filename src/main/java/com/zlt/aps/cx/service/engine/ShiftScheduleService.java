package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.cx.entity.CxMachineStructureCapacity;
import com.zlt.aps.cx.entity.CxPrecisionPlan;

import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.mp.api.domain.entity.MdmDevicePlanShut;
import com.zlt.aps.mp.api.domain.entity.MdmStructureTreadConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 班次排产服务
 * 
 * <p>负责 S5.3.7 按班次排产：
 * <ul>
 *   <li>将待排产量分配到具体的班次和时间段</li>
 *   <li>选择排产模式（核心计划计算 vs 成型结构达产）</li>
 *   <li>计算计划量和生产耗时</li>
 *   <li>确定开始时间和结束时间</li>
 *   <li>更新机台剩余产能</li>
 * </ul>
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftScheduleService {

    /** 默认波浪比例：夜班:早班:中班 = 1:2:1 */
    private static final int[] DEFAULT_WAVE_RATIO = {1, 2, 1};

    /** 班次编码：夜班 */
    public static final String SHIFT_NIGHT = "SHIFT_NIGHT";
    /** 班次编码：早班 */
    public static final String SHIFT_DAY = "SHIFT_DAY";
    /** 班次编码：中班 */
    public static final String SHIFT_AFTERNOON = "SHIFT_AFTERNOON";

    /**
     * 班次均衡分配
     *
     * @param allocations 机台分配结果
     * @param dayShifts   该天的班次配置列表
     * @param context     排程上下文
     * @return 班次分配结果
     */
    public List<CoreScheduleAlgorithmService.ShiftAllocationResult> balanceShiftAllocation(
            List<CoreScheduleAlgorithmService.MachineAllocationResult> allocations,
            List<CxShiftConfig> dayShifts,
            ScheduleContextVo context) {

        List<CoreScheduleAlgorithmService.ShiftAllocationResult> results = new ArrayList<>();

        if (dayShifts == null || dayShifts.isEmpty()) {
            log.warn("班次配置为空");
            return results;
        }

        // 从班次配置提取班次编码数组
        String[] shiftCodes = dayShifts.stream()
                .map(CxShiftConfig::getShiftCode)
                .toArray(String[]::new);

        // 加载结构班产配置
        Map<String, Map<String, MdmStructureTreadConfig>> structureCapacityMap = buildStructureShiftCapacityMap(context);

        for (CoreScheduleAlgorithmService.MachineAllocationResult allocation : allocations) {
            CoreScheduleAlgorithmService.ShiftAllocationResult shiftResult = new CoreScheduleAlgorithmService.ShiftAllocationResult();
            shiftResult.setMachineCode(allocation.getMachineCode());
            shiftResult.setTasks(allocation.getTaskAllocations());

            Map<String, Integer> shiftPlanQty = new LinkedHashMap<>();

            // 按任务结构获取班产整车数，计算波浪分配
            Map<String, Integer> structureWaveAllocation = calculateStructureWaveAllocation(
                    allocation.getTaskAllocations(),
                    structureCapacityMap,
                    allocation.getDailyCapacity(),
                    shiftCodes,
                    context);

            // 汇总各班次分配量
            for (CxShiftConfig shiftConfig : dayShifts) {
                String shiftCode = shiftConfig.getShiftCode();
                int shiftQty = structureWaveAllocation.getOrDefault(shiftCode, 0);
                shiftPlanQty.put(shiftCode, shiftQty);
            }

            // 处理特殊情况：开产首班不排关键产品
            if (Boolean.TRUE.equals(context.getIsOpeningDay()) && context.getCurrentScheduleDay() == 1) {
                String firstShift = dayShifts.get(0).getShiftCode();

                Set<String> keyProductCodes = context.getKeyProductCodes();
                if (keyProductCodes != null && !keyProductCodes.isEmpty() && dayShifts.size() > 1) {
                    int keyProductQty = 0;
                    for (CoreScheduleAlgorithmService.TaskAllocation task : allocation.getTaskAllocations()) {
                        if (keyProductCodes.contains(task.getMaterialCode())) {
                            keyProductQty += task.getQuantity();
                        }
                    }

                    if (keyProductQty > 0) {
                        int firstShiftQty = shiftPlanQty.getOrDefault(firstShift, 0);
                        int adjustedQty = Math.max(firstShiftQty - keyProductQty, 0);
                        shiftPlanQty.put(firstShift, roundToTrip(adjustedQty, "FLOOR"));

                        String secondShift = dayShifts.get(1).getShiftCode();
                        int secondShiftQty = shiftPlanQty.getOrDefault(secondShift, 0);
                        shiftPlanQty.put(secondShift, secondShiftQty + roundToTrip(keyProductQty, "CEILING"));

                        log.debug("开产首班 {} 移出关键产品 {} 条到 {}", firstShift, keyProductQty, secondShift);
                    }
                }
            }

            shiftResult.setShiftPlanQty(shiftPlanQty);
            results.add(shiftResult);
        }

        return results;
    }

    /**
     * 按结构计算波浪分配
     */
    private Map<String, Integer> calculateStructureWaveAllocation(
            List<CoreScheduleAlgorithmService.TaskAllocation> tasks,
            Map<String, Map<String, MdmStructureTreadConfig>> structureCapacityMap,
            Integer maxDailyCapacity,
            String[] shiftCodes,
            ScheduleContextVo context) {

        Map<String, Integer> shiftTotalQty = new LinkedHashMap<>();
        for (String shiftCode : shiftCodes) {
            shiftTotalQty.put(shiftCode, 0);
        }

        int totalAssigned = 0;

        for (CoreScheduleAlgorithmService.TaskAllocation task : tasks) {
            String structureCode = task.getStructureName();
            int taskQty = task.getQuantity();

            Map<String, MdmStructureTreadConfig> shiftCapacityMap = structureCapacityMap.get(structureCode);

            if (shiftCapacityMap != null && !shiftCapacityMap.isEmpty()) {
                int[] shiftQty = calculateShiftQtyByCapacity(taskQty, structureCode, shiftCapacityMap, shiftCodes, context);

                for (int i = 0; i < shiftCodes.length; i++) {
                    int qty = shiftQty[i];
                    shiftTotalQty.merge(shiftCodes[i], qty, Integer::sum);
                    totalAssigned += qty;
                }
            } else {
                int[] waveQty = calculateWaveAllocation(taskQty, shiftCodes, context);

                for (int i = 0; i < shiftCodes.length; i++) {
                    shiftTotalQty.merge(shiftCodes[i], waveQty[i], Integer::sum);
                    totalAssigned += waveQty[i];
                }
            }
        }

        // 检查是否超过机台最大产能
        if (maxDailyCapacity != null && totalAssigned > maxDailyCapacity) {
            double ratio = (double) maxDailyCapacity / totalAssigned;

            for (String shiftCode : shiftCodes) {
                int originalQty = shiftTotalQty.get(shiftCode);
                int adjustedQty = roundToTrip((int) (originalQty * ratio), "FLOOR");
                shiftTotalQty.put(shiftCode, adjustedQty);
            }
        }

        return shiftTotalQty;
    }

    /**
     * 根据结构班产配置计算各班次分配量
     */
    private int[] calculateShiftQtyByCapacity(
            int taskQty,
            String structureCode,
            Map<String, MdmStructureTreadConfig> shiftCapacityMap,
            String[] shiftCodes,
            ScheduleContextVo context) {

        int[] result = new int[shiftCodes.length];

        int totalTripQty = 0;
        int[] tripQtyPerShift = new int[shiftCodes.length];

        for (int i = 0; i < shiftCodes.length; i++) {
            MdmStructureTreadConfig capacity = shiftCapacityMap.get(shiftCodes[i]);
            if (capacity != null && capacity.getTreadCount() != null) {
                tripQtyPerShift[i] = capacity.getTreadCount();
                totalTripQty += capacity.getTreadCount();
            }
        }

        if (totalTripQty == 0) {
            return calculateWaveAllocation(taskQty, shiftCodes, context);
        }

        int[] waveRatio = context.getWaveRatio();
        if (waveRatio == null) {
            waveRatio = DEFAULT_WAVE_RATIO;
        }

        int[] adjustedRatio = adjustWaveRatio(waveRatio, shiftCodes);

        int totalRatio = 0;
        for (int ratio : adjustedRatio) {
            totalRatio += ratio;
        }

        int tripCapacity = getTripCapacity(structureCode, shiftCapacityMap);

        for (int i = 0; i < shiftCodes.length; i++) {
            int shiftQty = taskQty * adjustedRatio[i] / totalRatio;
            shiftQty = Math.min(shiftQty, tripQtyPerShift[i]);
            shiftQty = roundToTripQty(shiftQty, tripCapacity, "ROUND");

            result[i] = shiftQty;
        }

        return result;
    }

    /**
     * 按波浪比例分配
     */
    private int[] calculateWaveAllocation(int taskQty, String[] shiftCodes, ScheduleContextVo context) {
        int[] result = new int[shiftCodes.length];

        int[] waveRatio = context.getWaveRatio();
        if (waveRatio == null) {
            waveRatio = DEFAULT_WAVE_RATIO;
        }

        int[] adjustedRatio = adjustWaveRatio(waveRatio, shiftCodes);

        int totalRatio = 0;
        for (int ratio : adjustedRatio) {
            totalRatio += ratio;
        }

        int tripCapacity = context.getDefaultTripCapacity() != null ? context.getDefaultTripCapacity() : 12;

        int remainingQty = taskQty;
        for (int i = 0; i < shiftCodes.length; i++) {
            int shiftQty = taskQty * adjustedRatio[i] / totalRatio;
            shiftQty = roundToTripQty(shiftQty, tripCapacity, "ROUND");
            result[i] = shiftQty;
            remainingQty -= shiftQty;
        }

        // 分配余量
        if (remainingQty > 0) {
            for (int i = 0; i < shiftCodes.length && remainingQty >= tripCapacity; i++) {
                result[i] += tripCapacity;
                remainingQty -= tripCapacity;
            }
        }

        return result;
    }

    /**
     * 调整波浪比例映射
     */
    private int[] adjustWaveRatio(int[] waveRatio, String[] shiftCodes) {
        int[] adjustedRatio = new int[shiftCodes.length];

        for (int i = 0; i < shiftCodes.length; i++) {
            String shiftCode = shiftCodes[i];
            if (SHIFT_NIGHT.equals(shiftCode)) {
                adjustedRatio[i] = waveRatio[0];
            } else if (SHIFT_DAY.equals(shiftCode)) {
                adjustedRatio[i] = waveRatio[1];
            } else if (SHIFT_AFTERNOON.equals(shiftCode)) {
                adjustedRatio[i] = waveRatio[2];
            } else {
                adjustedRatio[i] = 1;
            }
        }

        return adjustedRatio;
    }

    /**
     * 从上下文构建结构班产配置映射
     */
    private Map<String, Map<String, MdmStructureTreadConfig>> buildStructureShiftCapacityMap(ScheduleContextVo context) {
        Map<String, Map<String, MdmStructureTreadConfig>> result = new HashMap<>();

        List<MdmStructureTreadConfig> capacities = context.getStructureShiftCapacities();
        if (capacities == null || capacities.isEmpty()) {
            return result;
        }

        for (MdmStructureTreadConfig capacity : capacities) {
            String structureCode = capacity.getStructureCode();
            String shiftCode = capacity.getShiftCode();
            if (structureCode != null && shiftCode != null) {
                result.computeIfAbsent(structureCode, k -> new HashMap<>())
                        .put(shiftCode, capacity);
            }
        }

        return result;
    }

    /**
     * 获取结构的整车容量
     */
    private int getTripCapacity(String structureCode, Map<String, MdmStructureTreadConfig> shiftCapacityMap) {
        if (shiftCapacityMap != null) {
            for (MdmStructureTreadConfig capacity : shiftCapacityMap.values()) {
                if (capacity.getTreadCount() != null && capacity.getTreadCount() > 0) {
                    return capacity.getTreadCount();
                }
            }
        }
        return 12;
    }

    /**
     * 整车取整
     */
    private int roundToTrip(int quantity, String mode) {
        return roundToTrip(quantity, mode, 12);
    }

    /**
     * 整车取整
     */
    private int roundToTrip(int quantity, String mode, int tripCapacity) {
        if (quantity <= 0) {
            return 0;
        }

        int trips;
        switch (mode) {
            case "CEILING":
                trips = (int) Math.ceil((double) quantity / tripCapacity);
                break;
            case "FLOOR":
                trips = (int) Math.floor((double) quantity / tripCapacity);
                break;
            case "ROUND":
            default:
                trips = (int) Math.round((double) quantity / tripCapacity);
                break;
        }

        return trips * tripCapacity;
    }

    /**
     * 按整车容量取整
     */
    private int roundToTripQty(int quantity, int tripCapacity, String mode) {
        return roundToTrip(quantity, mode, tripCapacity);
    }

    // ==================== S5.3.7 详细排产方法 ====================

    /**
     * S5.3.7 按班次排产详细实现
     *
     * <p>将待排产量分配到具体的班次和时间段
     *
     * @param task          任务
     * @param machineCode   机台编码
     * @param context       排程上下文
     * @param dayShifts     班次配置
     * @param scheduleDate  排程日期
     * @return 班次排产结果列表
     */
    public List<ShiftProductionResult> scheduleTaskToShifts(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            String machineCode,
            ScheduleContextVo context,
            List<CxShiftConfig> dayShifts,
            LocalDate scheduleDate) {

        List<ShiftProductionResult> results = new ArrayList<>();

        int remainingQty = task.getPlannedProduction() != null ? task.getPlannedProduction() : 0;
        if (remainingQty <= 0) {
            return results;
        }

        int hourlyCapacity = getMachineHourlyCapacity(machineCode, task.getStructureName(), context);
        int tripCapacity = getTripCapacity(task.getStructureName(), context);

        for (CxShiftConfig shiftConfig : dayShifts) {
            if (remainingQty <= 0) {
                break;
            }

            // 试制任务只能在早班或中班
            if (Boolean.TRUE.equals(task.getIsTrialTask())) {
                if (!SHIFT_DAY.equals(shiftConfig.getShiftCode()) 
                        && !SHIFT_AFTERNOON.equals(shiftConfig.getShiftCode())) {
                    continue;
                }
            }

            int shiftAvailableCapacity = calculateShiftAvailableCapacity(
                    machineCode, shiftConfig, hourlyCapacity, context);

            if (shiftAvailableCapacity <= 0) {
                continue;
            }

            int currentBatchQty = Math.min(remainingQty, shiftAvailableCapacity);

            double productionHours = (double) currentBatchQty / hourlyCapacity;

            LocalDateTime startTime = calculateStartTime(machineCode, shiftConfig, scheduleDate, context);
            LocalDateTime endTime = startTime.plusMinutes((long) (productionHours * 60));

            LocalDateTime shiftEndTime = calculateShiftEndTime(shiftConfig, scheduleDate);
            if (endTime.isAfter(shiftEndTime)) {
                endTime = shiftEndTime;
                long actualMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
                currentBatchQty = (int) (actualMinutes * hourlyCapacity / 60);
                currentBatchQty = roundToTrip(currentBatchQty, "FLOOR", tripCapacity);
            }

            if (currentBatchQty <= 0) {
                continue;
            }

            ShiftProductionResult result = new ShiftProductionResult();
            result.setMachineCode(machineCode);
            result.setShiftCode(shiftConfig.getShiftCode());
            result.setShiftName(shiftConfig.getShiftName());
            result.setMaterialCode(task.getMaterialCode());
            result.setMaterialName(task.getMaterialName());
            result.setStructureName(task.getStructureName());
            result.setQuantity(currentBatchQty);
            result.setPlanStartTime(startTime);
            result.setPlanEndTime(endTime);
            result.setIsTrialTask(task.getIsTrialTask());
            result.setIsEndingTask(task.getIsEndingTask());
            result.setIsContinueTask(task.getIsContinueTask());

            results.add(result);

            remainingQty -= currentBatchQty;

            log.debug("班次排产：机台={}, 班次={}, 胎胚={}, 数量={}, 时间={}-{}",
                    machineCode, shiftConfig.getShiftName(), task.getMaterialCode(),
                    currentBatchQty, startTime, endTime);
        }

        if (remainingQty > 0) {
            log.warn("任务 {} 还有 {} 条未排产，产能不足", task.getMaterialCode(), remainingQty);
        }

        return results;
    }

    /**
     * 计算班次可用产能
     */
    private int calculateShiftAvailableCapacity(
            String machineCode,
            CxShiftConfig shiftConfig,
            int hourlyCapacity,
            ScheduleContextVo context) {

        int shiftHours = calculateShiftHours(shiftConfig);
        int baseCapacity = hourlyCapacity * shiftHours;

        int shutdownDeduction = calculateShiftShutdownDeduction(machineCode, shiftConfig, hourlyCapacity, context);
        int precisionDeduction = calculateShiftPrecisionDeduction(machineCode, shiftConfig, hourlyCapacity, context);

        int availableCapacity = baseCapacity - shutdownDeduction - precisionDeduction;

        return Math.max(0, availableCapacity);
    }

    /**
     * 计算班次时长（小时）
     */
    private int calculateShiftHours(CxShiftConfig shiftConfig) {
        Integer startHour = shiftConfig.getStartHour();
        Integer endHour = shiftConfig.getEndHour();

        if (startHour == null || endHour == null) {
            return 8;
        }

        if (endHour < startHour) {
            return (24 - startHour) + endHour;
        }
        return endHour - startHour;
    }

    /**
     * 计算班次停机扣减产能
     */
    private int calculateShiftShutdownDeduction(
            String machineCode,
            CxShiftConfig shiftConfig,
            int hourlyCapacity,
            ScheduleContextVo context) {

        if (context.getDevicePlanShuts() == null || context.getDevicePlanShuts().isEmpty()) {
            return 0;
        }

        int totalDeduction = 0;

        for (MdmDevicePlanShut shutdown : context.getDevicePlanShuts()) {
            if (!machineCode.equals(shutdown.getMachineCode())) {
                continue;
            }
            // TODO: 实现详细的班次停机时间扣减计算
        }

        return totalDeduction;
    }

    /**
     * 计算班次精度计划扣减产能
     */
    private int calculateShiftPrecisionDeduction(
            String machineCode,
            CxShiftConfig shiftConfig,
            int hourlyCapacity,
            ScheduleContextVo context) {

        if (context.getPrecisionPlans() == null || context.getPrecisionPlans().isEmpty()) {
            return 0;
        }

        for (CxPrecisionPlan plan : context.getPrecisionPlans()) {
            if (machineCode.equals(plan.getMachineCode())) {
                if (shiftConfig.getShiftCode().equals(plan.getPlanShift())) {
                    int precisionHours = plan.getEstimatedHours() != null ? plan.getEstimatedHours() : 4;
                    return precisionHours * hourlyCapacity;
                }
            }
        }

        return 0;
    }

    /**
     * 计算生产开始时间
     */
    private LocalDateTime calculateStartTime(
            String machineCode,
            CxShiftConfig shiftConfig,
            LocalDate scheduleDate,
            ScheduleContextVo context) {

        int startHour = shiftConfig.getStartHour() != null ? shiftConfig.getStartHour() : 0;
        int startMinute = shiftConfig.getStartMinute() != null ? shiftConfig.getStartMinute() : 0;

        LocalDateTime startTime = LocalDateTime.of(scheduleDate, LocalTime.of(startHour, startMinute));

        int prepareMinutes = getMachinePrepareMinutes(machineCode, context);
        startTime = startTime.plusMinutes(prepareMinutes);

        return startTime;
    }

    /**
     * 计算班次结束时间
     */
    private LocalDateTime calculateShiftEndTime(CxShiftConfig shiftConfig, LocalDate scheduleDate) {
        int endHour = shiftConfig.getEndHour() != null ? shiftConfig.getEndHour() : 8;
        int endMinute = shiftConfig.getEndMinute() != null ? shiftConfig.getEndMinute() : 0;

        LocalDateTime endTime = LocalDateTime.of(scheduleDate, LocalTime.of(endHour, endMinute));

        if (shiftConfig.getShiftCode().equals(SHIFT_NIGHT) && endHour <= 8) {
            endTime = endTime.plusDays(1);
        }

        return endTime;
    }

    /**
     * 获取机台小时产能
     */
    private int getMachineHourlyCapacity(String machineCode, String structureName, ScheduleContextVo context) {
        if (context.getMachineStructureCapacities() != null && machineCode != null && structureName != null) {
            for (CxMachineStructureCapacity capacity : context.getMachineStructureCapacities()) {
                if (machineCode.equals(capacity.getCxMachineCode()) 
                        && structureName.equals(capacity.getStructureCode())) {
                    return capacity.getHourlyCapacity() != null ? capacity.getHourlyCapacity() : 50;
                }
            }
        }
        return context.getMachineHourlyCapacity() != null ? context.getMachineHourlyCapacity() : 50;
    }

    /**
     * 获取结构的整车容量
     */
    private int getTripCapacity(String structureCode, ScheduleContextVo context) {
        if (context.getStructureShiftCapacities() != null) {
            for (MdmStructureTreadConfig capacity : context.getStructureShiftCapacities()) {
                if (capacity.getStructureCode() != null &&
                        capacity.getStructureCode().equals(structureCode)) {
                    if (capacity.getTreadCount() != null && capacity.getTreadCount() > 0) {
                        return capacity.getTreadCount();
                    }
                }
            }
        }
        return context.getDefaultTripCapacity() != null ? context.getDefaultTripCapacity() : 12;
    }

    /**
     * 获取机台准备时间（分钟）
     */
    private int getMachinePrepareMinutes(String machineCode, ScheduleContextVo context) {
        return 30;
    }

    /**
     * 班次排产结果
     */
    @lombok.Data
    public static class ShiftProductionResult {
        private String machineCode;
        private String shiftCode;
        private String shiftName;
        private String materialCode;
        private String materialName;
        private String structureName;
        private int quantity;
        private LocalDateTime planStartTime;
        private LocalDateTime planEndTime;
        private Boolean isTrialTask;
        private Boolean isEndingTask;
        private Boolean isContinueTask;
    }
}
