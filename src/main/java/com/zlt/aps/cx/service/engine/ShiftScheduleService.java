package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmDevicePlanShut;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.cx.api.domain.entity.CxPrecisionPlan;
import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 班次排产服务 — 成型排程 S5.3.7 阶段
 *
 * <p>负责将待排产量分配到具体的班次和时间段，支持5种任务类型的班次分配：
 * <ol>
 *   <li><b>普通任务</b>：波浪放置，每个班次车数相差不超过1</li>
 *   <li><b>收尾任务</b>：只能在硫化的收尾班次或之前安排，最后一个班次可以不是整车</li>
 *   <li><b>开产任务</b>：成型提前一班开始，首班6小时产能，关键产品从第二班开始；
 *       首班产量 = 6×3600 / 成型一条胎时间(s)，不补整车</li>
 *   <li><b>停产任务</b>：根据硫化任务的 class*EndTime 反推成型需要在停产前生产多少条，
 *       使得库存刚好够硫化消化到停产时刻</li>
 *   <li><b>试制任务</b>：只在早班/中班，双数，不补整车</li>
 * </ol>
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftScheduleService {

    // ==================== 业务阈值常量 ====================

    /** 默认整车容量（条/车） */
    private static final int DEFAULT_TRIP_CAPACITY = 12;

    /** 默认机台小时产能（条/小时） */
    private static final int DEFAULT_HOURLY_CAPACITY = 50;

    /** 开产首班时长上限（小时） */
    private static final int OPENING_FIRST_SHIFT_HOURS = 6;

    /** 默认班次时长（小时） */
    private static final int DEFAULT_SHIFT_HOURS = 8;

    /** 默认机台准备时间（分钟） */
    private static final int DEFAULT_MACHINE_PREPARE_MINUTES = 30;

    /** 默认精度计划时长（小时） */
    private static final int DEFAULT_PRECISION_HOURS = 4;

    /** 一天的秒数 */
    private static final int SECONDS_PER_DAY = 24 * 60 * 60;

    /** 一小时的秒数 */
    private static final int SECONDS_PER_HOUR = 3600;

    // ==================== 班次编码常量 ====================

    /** 班次编码：夜班 */
    public static final String SHIFT_NIGHT = "SHIFT_NIGHT";
    /** 班次编码：早班 */
    public static final String SHIFT_DAY = "SHIFT_DAY";
    /** 班次编码：中班 */
    public static final String SHIFT_AFTERNOON = "SHIFT_AFTERNOON";

    // ==================== 依赖注入 ====================

    private final ScheduleDayTypeHelper scheduleDayTypeHelper;

    // ==================== 公开方法 ====================

    /**
     * S5.3.7 按班次排产（任务级精排）
     *
     * <p>将单个任务的待排产量分配到具体的班次，分配的数量是 task.endingExtraInventory。
     * 根据5种任务类型采用不同的分配策略：
     * <ol>
     *   <li>普通任务：波浪放置，每个班次车数相差不超过1</li>
     *   <li>收尾任务：只能在硫化的收尾班次或之前班次安排，最后班次可以不是整车</li>
     *   <li>开产任务：首班6小时产能，关键产品从第二班开始</li>
     *   <li>停产任务：根据硫化EndTime反推，库存全部消耗，最后一个班次用反推计划量</li>
     *   <li>试制任务：只在早班/中班，双数，不补整车</li>
     * </ol>
     *
     * @param task          日胎胚任务
     * @param machineCode   机台编码
     * @param context       排程上下文
     * @param dayShifts     当天班次配置
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

        Integer endingExtraInventory = task.getEndingExtraInventory();
        if (endingExtraInventory == null || endingExtraInventory <= 0) {
            return results;
        }

        int tripCapacity = getTripCapacity(task.getStructureName(), context);

        // 调试日志
        log.info("scheduleTaskToShifts: embryoCode={}, materialCode={}, materialDesc={}, structureName={}, " +
                        "endingExtraInventory(待排产量)={}, vulcanizeMachineCount(硫化机台数)={}, " +
                        "tripCapacity(整车容量/每车条数)={}, " +
                        "isTrial={}, isClosingDay={}, isOpeningDay={}, isEnding={}, isContinue={}, " +
                        "stockHours(库存可供时长h)={}, isOpeningDayTask={}, isLastEndingBatch={}, dayShifts.size={}",
                task.getEmbryoCode(), task.getMaterialCode(), task.getMaterialDesc(), task.getStructureName(),
                endingExtraInventory, task.getVulcanizeMachineCount(),
                tripCapacity,
                task.getIsTrialTask(), task.getIsClosingDayTask(), task.getIsOpeningDayTask(),
                task.getIsEndingTask(), task.getIsContinueTask(),
                task.getStockHours(), task.getIsOpeningDayTask(), task.getIsLastEndingBatch(),
                dayShifts != null ? dayShifts.size() : "null");

        // 判断任务类型，按优先级从高到低
        boolean isTrial = Boolean.TRUE.equals(task.getIsTrialTask());
        boolean isClosingDay = Boolean.TRUE.equals(task.getIsClosingDayTask());
        boolean isOpeningDay = Boolean.TRUE.equals(task.getIsOpeningDayTask());
        boolean isEnding = Boolean.TRUE.equals(task.getIsEndingTask()) || Boolean.TRUE.equals(task.getIsUrgentEnding());
        boolean isLastEndingBatch = Boolean.TRUE.equals(task.getIsLastEndingBatch());  // 收尾最后一批

        // ---- 1. 试制任务：只在早班/中班，双数，不补整车 ----
        if (isTrial) {
            return scheduleTrialTask(task, machineCode, context, dayShifts, scheduleDate, tripCapacity);
        }

        // ---- 2. 停产任务：根据硫化EndTime反推计划量 ----
        if (isClosingDay) {
            return scheduleClosingTask(task, machineCode, context, dayShifts, scheduleDate, tripCapacity);
        }

        // ---- 3. 开产任务：首班6小时产能，关键产品从第二班开始 ----
        if (isOpeningDay) {
            // 按班次排程时，根据 formingOpeningShiftOrder 判断是否为开产首班
            if (dayShifts.size() == 1) {
                CxShiftConfig singleShift = dayShifts.get(0);
                int shiftOrder = singleShift.getDayShiftOrder() != null ? singleShift.getDayShiftOrder() : 1;
                Integer formingOpeningShiftOrder = task.getFormingOpeningShiftOrder();
                Integer lhOpeningShiftOrder = task.getLhOpeningShiftOrder();

                // 用 formingOpeningShiftOrder 判断是否为成型开产首班
                boolean isOpeningFirstShift = formingOpeningShiftOrder != null
                        && shiftOrder == formingOpeningShiftOrder
                        && formingOpeningShiftOrder < (lhOpeningShiftOrder != null ? lhOpeningShiftOrder : Integer.MAX_VALUE);

                if (isOpeningFirstShift) {
                    // 成型开产首班：关键产品不排产，非关键产品用6小时产能
                    if (isKeyProduct(task, context)) {
                        log.info("开产首班关键产品 {} 不排产，等待下一班次", task.getEmbryoCode());
                        return results;
                    }
                    return scheduleOpeningTask(task, machineCode, context, dayShifts, scheduleDate, tripCapacity);
                } else {
                    // 开产非首班：按普通任务全产能排产
                    log.info("开产非首班，任务 {} 按普通任务排产（shiftOrder={}）", task.getEmbryoCode(), shiftOrder);
                    return scheduleNormalTask(task, machineCode, context, dayShifts, scheduleDate, tripCapacity);
                }
            }
            // 多班次模式（兼容旧逻辑）：走 scheduleOpeningTask
            return scheduleOpeningTask(task, machineCode, context, dayShifts, scheduleDate, tripCapacity);
        }

        // ---- 4. 收尾任务：只能在硫化收尾班次或之前安排，最后班次可以不是整车 ----
        if (isEnding || isLastEndingBatch) {
            return scheduleEndingTask(task, machineCode, context, dayShifts, scheduleDate, tripCapacity);
        }

        // ---- 5. 普通任务：波浪放置 ----
        return scheduleNormalTask(task, machineCode, context, dayShifts, scheduleDate, tripCapacity);
    }

    // ==================== 1. 试制任务排产 ====================

    /**
     * 试制任务排产：只在早班/中班，双数，不补整车
     */
    private List<ShiftProductionResult> scheduleTrialTask(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            String machineCode,
            ScheduleContextVo context,
            List<CxShiftConfig> dayShifts,
            LocalDate scheduleDate,
            int tripCapacity) {

        List<ShiftProductionResult> results = new ArrayList<>();
        int totalQty = task.getEndingExtraInventory();

        // 确保双数
        if (totalQty % 2 != 0) {
            totalQty = totalQty - 1;
        }
        if (totalQty <= 0) {
            return results;
        }

        // 按均分方式分配到早班和中班（跳过夜班）
        // 先统计可排班次
        List<CxShiftConfig> trialShifts = new ArrayList<>();
        for (CxShiftConfig shiftConfig : dayShifts) {
            if (!SHIFT_NIGHT.equals(shiftConfig.getShiftCode())) {
                trialShifts.add(shiftConfig);
            }
        }
        int trialShiftCount = Math.max(trialShifts.size(), 1);

        // 按班次均分总量
        int[] shiftQuantities = distributeQuantityEvenly(totalQty, trialShiftCount);
        // 确保双数
        for (int i = 0; i < shiftQuantities.length; i++) {
            if (shiftQuantities[i] % 2 != 0) {
                shiftQuantities[i] = shiftQuantities[i] - 1;
            }
        }

        int remainingQty = totalQty;
        int shiftIndex = 0;

        for (CxShiftConfig shiftConfig : dayShifts) {
            String shiftCode = shiftConfig.getShiftCode();

            // 夜班不排试制
            if (SHIFT_NIGHT.equals(shiftCode)) {
                continue;
            }

            int shiftQty = shiftIndex < shiftQuantities.length ? shiftQuantities[shiftIndex] : 0;
            shiftIndex++;

            // 不能超过剩余量
            shiftQty = Math.min(shiftQty, remainingQty);
            if (shiftQty % 2 != 0) {
                shiftQty = shiftQty - 1;
            }
            if (shiftQty <= 0) {
                continue;
            }

            // 计算时间
            int hourlyCapacity = getMachineHourlyCapacity(machineCode, task.getMaterialCode(), task.getStructureName(), context);
            double productionHours = (double) shiftQty / hourlyCapacity;
            LocalDateTime startTime = calculateStartTime(machineCode, shiftConfig, scheduleDate, context);
            LocalDateTime endTime = startTime.plusMinutes((long) (productionHours * 60));

            // 班次结束时间检查
            LocalDateTime shiftEndTime = calculateShiftEndTime(shiftConfig, scheduleDate);
            if (endTime.isAfter(shiftEndTime)) {
                long availableMinutes = Duration.between(startTime, shiftEndTime).toMinutes();
                availableMinutes -= getMachinePrepareMinutes(machineCode, context);
                int availableQty = (int) (availableMinutes * hourlyCapacity / 60);
                // 双数
                if (availableQty % 2 != 0) {
                    availableQty = availableQty - 1;
                }
                shiftQty = Math.max(0, availableQty);
                endTime = shiftEndTime;
            }

            if (shiftQty <= 0) {
                continue;
            }

            // 试制任务不补整车，cars按实际计算（不向上取整）
            int trialCars = tripCapacity > 0 ? shiftQty / tripCapacity : 1;
            ShiftProductionResult result = buildResult(machineCode, shiftConfig, task, shiftQty,
                    tripCapacity, trialCars,
                    startTime, endTime, true, false, task.getIsContinueTask());

            results.add(result);
            remainingQty -= shiftQty;
        }

        log.info("试制任务 {} 班次排产完成：总计划 {}，已排 {}", task.getEmbryoCode(), totalQty,
                totalQty - remainingQty);
        return results;
    }

    // ==================== 2. 停产任务排产 ====================

    /**
     * 停产任务排产：endingExtraInventory已在TaskGroupService中通过反推封顶正确计算
     *
     * <p>改造后逻辑（简化）：
     * <ol>
     *   <li>endingExtraInventory 已经是经过反推封顶后的正确值，不再需要重新反推</li>
     *   <li>判断当前班次是否为停锅班次（task.closingShiftOrder）</li>
     *   <li>停锅班次：不补整车，按实量下</li>
     *   <li>停锅班次之前的班次：按普通任务排产（整车取整）</li>
     * </ol>
     */
    private List<ShiftProductionResult> scheduleClosingTask(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            String machineCode,
            ScheduleContextVo context,
            List<CxShiftConfig> dayShifts,
            LocalDate scheduleDate,
            int tripCapacity) {

        List<ShiftProductionResult> results = new ArrayList<>();
        int totalQty = task.getEndingExtraInventory();
        if (totalQty <= 0) {
            return results;
        }

        Integer closingShiftOrder = task.getClosingShiftOrder();
        int currentDayShiftOrder = dayShifts != null && dayShifts.size() == 1 && dayShifts.get(0).getDayShiftOrder() != null
                ? dayShifts.get(0).getDayShiftOrder() : 0;

        // 判断当前班次是否为停锅班次
        boolean isClosingShift = closingShiftOrder != null && currentDayShiftOrder == closingShiftOrder;

        int hourlyCapacity = getMachineHourlyCapacity(machineCode, task.getMaterialCode(), task.getStructureName(), context);

        for (CxShiftConfig shiftConfig : dayShifts) {
            if (totalQty <= 0) {
                break;
            }

            int shiftQty;
            if (isClosingShift) {
                // 停锅班次：不补整车，按实量下
                shiftQty = totalQty;
            } else {
                // 停锅班次之前的班次：整车取整
                int shiftHours = calculateShiftHours(shiftConfig);
                int shiftCapacity = shiftHours * hourlyCapacity;
                shiftCapacity -= calculateShiftShutdownDeduction(machineCode, shiftConfig, hourlyCapacity, context);
                shiftCapacity -= calculateShiftPrecisionDeduction(machineCode, shiftConfig, hourlyCapacity, context);
                shiftCapacity = Math.max(0, shiftCapacity);
                int cars = shiftCapacity / Math.max(tripCapacity, 1);
                shiftQty = Math.min(cars * tripCapacity, totalQty);
            }

            if (shiftQty <= 0) {
                continue;
            }

            // 计算时间
            LocalDateTime startTime = calculateStartTime(machineCode, shiftConfig, scheduleDate, context);
            double productionHours = (double) shiftQty / hourlyCapacity;
            LocalDateTime endTime = startTime.plusMinutes((long) (productionHours * 60));

            LocalDateTime shiftEndTime = calculateShiftEndTime(shiftConfig, scheduleDate);
            if (endTime.isAfter(shiftEndTime)) {
                long availableMinutes = Duration.between(startTime, shiftEndTime).toMinutes();
                availableMinutes -= getMachinePrepareMinutes(machineCode, context);
                shiftQty = Math.max(0, (int) (availableMinutes * hourlyCapacity / 60));
                // 停锅班次不补整车；非停锅班次整车取整
                if (!isClosingShift && tripCapacity > 0) {
                    shiftQty = (shiftQty / tripCapacity) * tripCapacity;
                }
                endTime = shiftEndTime;
            }

            if (shiftQty <= 0) {
                continue;
            }

            int cars = isClosingShift
                    ? (tripCapacity > 0 ? (shiftQty + tripCapacity - 1) / tripCapacity : 1)
                    : (tripCapacity > 0 ? shiftQty / tripCapacity : 1);

            ShiftProductionResult result = buildResult(machineCode, shiftConfig, task, shiftQty,
                    tripCapacity, cars, startTime, endTime, false, false, task.getIsContinueTask());

            results.add(result);
            totalQty -= shiftQty;
        }

        log.info("停产任务 {} 班次排产完成: closingShiftOrder={}, isClosingShift={}, 已排={}",
                task.getEmbryoCode(), closingShiftOrder, isClosingShift,
                task.getEndingExtraInventory() - totalQty);
        return results;
    }

    // ==================== 3. 开产任务排产 ====================

    /**
     * 开产任务排产：endingExtraInventory已在TaskGroupService中正确计算
     *
     * <p>改造后逻辑（简化）：
     * <ol>
     *   <li>endingExtraInventory 已经是经过开产首班6h封顶后的正确值</li>
     *   <li>判断当前班次是否为成型开产首班（task.formingOpeningShiftOrder）</li>
     *   <li>成型开产首班：6小时产能封顶，不补整车</li>
     *   <li>非首班：按普通任务全产能排产，整车取整</li>
     * </ol>
     */
    private List<ShiftProductionResult> scheduleOpeningTask(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            String machineCode,
            ScheduleContextVo context,
            List<CxShiftConfig> dayShifts,
            LocalDate scheduleDate,
            int tripCapacity) {

        List<ShiftProductionResult> results = new ArrayList<>();
        int totalQty = task.getEndingExtraInventory();
        if (totalQty <= 0) {
            return results;
        }

        Integer formingOpeningShiftOrder = task.getFormingOpeningShiftOrder();
        Integer lhOpeningShiftOrder = task.getLhOpeningShiftOrder();
        int currentDayShiftOrder = dayShifts != null && dayShifts.size() == 1 && dayShifts.get(0).getDayShiftOrder() != null
                ? dayShifts.get(0).getDayShiftOrder() : 0;

        // 判断当前班次是否为成型开产首班
        boolean isOpeningFirstShift = formingOpeningShiftOrder != null && currentDayShiftOrder == formingOpeningShiftOrder
                && formingOpeningShiftOrder < (lhOpeningShiftOrder != null ? lhOpeningShiftOrder : Integer.MAX_VALUE);

        boolean isKeyProduct = isKeyProduct(task, context);

        // 开产首班且关键产品：不排
        if (isOpeningFirstShift && isKeyProduct) {
            log.info("开产首班关键产品 {} 不排产，等待下一班次", task.getEmbryoCode());
            return results;
        }

        int hourlyCapacity = getMachineHourlyCapacity(machineCode, task.getMaterialCode(), task.getStructureName(), context);

        for (CxShiftConfig shiftConfig : dayShifts) {
            if (totalQty <= 0) {
                break;
            }

            int shiftQty;
            if (isOpeningFirstShift) {
                // 成型开产首班：endingExtraInventory已是6h封顶值，不补整车
                shiftQty = totalQty;
            } else {
                // 非首班：按整车分配
                int shiftHours = calculateShiftHours(shiftConfig);
                int shiftCapacity = shiftHours * hourlyCapacity;
                shiftCapacity -= calculateShiftShutdownDeduction(machineCode, shiftConfig, hourlyCapacity, context);
                shiftCapacity -= calculateShiftPrecisionDeduction(machineCode, shiftConfig, hourlyCapacity, context);
                shiftCapacity = Math.max(0, shiftCapacity);
                int cars = shiftCapacity / Math.max(tripCapacity, 1);
                shiftQty = Math.min(cars * tripCapacity, totalQty);
            }

            if (shiftQty <= 0) {
                continue;
            }

            // 计算时间
            LocalDateTime startTime = calculateStartTime(machineCode, shiftConfig, scheduleDate, context);
            double productionHours = (double) shiftQty / hourlyCapacity;
            LocalDateTime endTime = startTime.plusMinutes((long) (productionHours * 60));

            LocalDateTime shiftEndTime = calculateShiftEndTime(shiftConfig, scheduleDate);
            if (endTime.isAfter(shiftEndTime)) {
                long availableMinutes = Duration.between(startTime, shiftEndTime).toMinutes();
                availableMinutes -= getMachinePrepareMinutes(machineCode, context);
                shiftQty = Math.max(0, (int) (availableMinutes * hourlyCapacity / 60));
                if (!isOpeningFirstShift && tripCapacity > 0) {
                    // 非首班整车取整
                    shiftQty = (shiftQty / tripCapacity) * tripCapacity;
                }
                endTime = shiftEndTime;
            }

            if (shiftQty <= 0) {
                continue;
            }

            int cars = isOpeningFirstShift
                    ? (tripCapacity > 0 ? (shiftQty + tripCapacity - 1) / tripCapacity : 1)
                    : (tripCapacity > 0 ? shiftQty / tripCapacity : 1);

            ShiftProductionResult result = buildResult(machineCode, shiftConfig, task, shiftQty,
                    tripCapacity, cars, startTime, endTime, false, false, task.getIsContinueTask());

            results.add(result);
            totalQty -= shiftQty;
        }

        log.info("开产任务 {} 班次排产完成: formingOpeningShiftOrder={}, isOpeningFirstShift={}, 关键产品={}, 已排={}",
                task.getEmbryoCode(), formingOpeningShiftOrder, isOpeningFirstShift, isKeyProduct,
                task.getEndingExtraInventory() - totalQty);
        return results;
    }

    /**
     * 计算开产首班6小时产能
     *
     * <p>计算公式：
     * <pre>
     *   日硫化量 → materialLhCapacityMap
     *   配比 → structureLhRatioMap (机型+结构)
     *   成型一条胎时间(s) = 24×3600 / (配比 × 日硫化量)
     *   首班6小时产量 = 6×3600 / 成型一条胎时间(s)
     * </pre>
     */
    private int calculateOpeningFirstShiftCapacity(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            String machineCode,
            ScheduleContextVo context) {

        // 1. 获取日硫化量
        Integer dailyLhCapacity = null;
        Map<String, MonthPlanProductLhCapacityVo> lhCapacityMap = context.getMaterialLhCapacityMap();
        if (lhCapacityMap != null && task.getMaterialCode() != null) {
            MonthPlanProductLhCapacityVo capacityVo = lhCapacityMap.get(task.getMaterialCode());
            if (capacityVo != null) {
                dailyLhCapacity = capacityVo.getDefaultDayVulcanizationQty();
            }
        }

        if (dailyLhCapacity == null || dailyLhCapacity <= 0) {
            log.warn("开产首班产能计算：无法获取物料 {} 的日硫化量，使用默认值", task.getEmbryoCode());
            return OPENING_FIRST_SHIFT_HOURS * getMachineHourlyCapacity(machineCode, task.getMaterialCode(), task.getStructureName(), context);
        }

        // 2. 获取配比（机型+结构 → 配比）
        int ratio = 1;
        if (context.getStructureLhRatioMap() != null && task.getStructureName() != null) {
            MdmStructureLhRatio lhRatio = context.getStructureLhRatioMap().get(task.getStructureName());
            if (lhRatio != null && lhRatio.getLhMachineMaxQty() != null && lhRatio.getLhMachineMaxQty() > 0) {
                ratio = lhRatio.getLhMachineMaxQty();
            }
        }

        // 3. 成型一条胎时间(s) = 24×3600 / (配比 × 日硫化量)
        BigDecimal formingTimePerTire = BigDecimal.valueOf(SECONDS_PER_DAY)
                .divide(BigDecimal.valueOf((long) ratio * dailyLhCapacity), 2, RoundingMode.HALF_UP);

        // 4. 首班6小时产量 = 6×3600 / 成型一条胎时间(s)
        int firstShiftCapacity = BigDecimal.valueOf(OPENING_FIRST_SHIFT_HOURS * SECONDS_PER_HOUR)
                .divide(formingTimePerTire, 0, RoundingMode.FLOOR)
                .intValue();

        log.debug("开产首班产能计算：日硫化量={}, 配比={}, 成型单条时间={}s, 首班产量={}",
                dailyLhCapacity, ratio, formingTimePerTire, firstShiftCapacity);

        return Math.max(firstShiftCapacity, 0);
    }

    // ==================== 4. 收尾任务排产 ====================

    /**
     * 收尾任务排产：只能在硫化收尾班次或之前安排，最后班次可以不是整车
     *
     * <p>如果硫化的收尾班次是夜班，则收尾任务只能在夜班安排；
     * 如果是早班，则可以在夜班和早班安排；依此类推。
     * 最后一个班次可以不是整车（按实际量下）。
     */
    private List<ShiftProductionResult> scheduleEndingTask(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            String machineCode,
            ScheduleContextVo context,
            List<CxShiftConfig> dayShifts,
            LocalDate scheduleDate,
            int tripCapacity) {

        List<ShiftProductionResult> results = new ArrayList<>();
        int totalQty = task.getEndingExtraInventory();
        if (totalQty <= 0) {
            return results;
        }

        // 获取硫化的收尾班次索引
        String vulcanizeEndingShift = getVulcanizeEndingShift(task, context);
        int maxShiftIndex = getShiftIndex(vulcanizeEndingShift, dayShifts);
        if (maxShiftIndex < 0) {
            maxShiftIndex = dayShifts.size() - 1;
        }

        // 计算波浪分配
        int requiredCars = tripCapacity > 0 ? (totalQty + tripCapacity - 1) / tripCapacity : 1;
        int[] shiftCars = calculateWaveCars(requiredCars, dayShifts);
        log.info("【波浪分配】胎胚={}, 待排={}条, 每车={}条, 需={}车, 各班分配={}", 
                 task.getEmbryoCode(), totalQty, tripCapacity, requiredCars, Arrays.toString(shiftCars));

        // 收尾班次约束：只能在 maxShiftIndex 或之前的班次安排
        for (int i = maxShiftIndex + 1; i < shiftCars.length; i++) {
            if (shiftCars[i] > 0) {
                shiftCars[maxShiftIndex] += shiftCars[i];
                shiftCars[i] = 0;
            }
        }

        int hourlyCapacity = getMachineHourlyCapacity(machineCode, task.getMaterialCode(), task.getStructureName(), context);
        int remainingQty = totalQty;

        for (int i = 0; i < dayShifts.size() && remainingQty > 0; i++) {
            if (shiftCars[i] <= 0) {
                continue;
            }

            CxShiftConfig shiftConfig = dayShifts.get(i);

            // 计算该班次分配的量
            int shiftQty = shiftCars[i] * tripCapacity;
            boolean isLastProductive = isLastShiftWithQty(i, shiftCars);

            if (isLastProductive) {
                // 最后一个有量的班次：使用剩余量，可以不是整车
                shiftQty = remainingQty;
            }

            shiftQty = Math.min(shiftQty, remainingQty);
            if (shiftQty <= 0) {
                continue;
            }

            // 计算时间
            LocalDateTime startTime = calculateStartTime(machineCode, shiftConfig, scheduleDate, context);
            double productionHours = (double) shiftQty / hourlyCapacity;
            LocalDateTime endTime = startTime.plusMinutes((long) (productionHours * 60));

            LocalDateTime shiftEndTime = calculateShiftEndTime(shiftConfig, scheduleDate);
            if (endTime.isAfter(shiftEndTime)) {
                long availableMinutes = Duration.between(startTime, shiftEndTime).toMinutes();
                availableMinutes -= getMachinePrepareMinutes(machineCode, context);
                shiftQty = Math.max(0, (int) (availableMinutes * hourlyCapacity / 60));
                endTime = shiftEndTime;
            }

            if (shiftQty <= 0) {
                continue;
            }

            int cars = tripCapacity > 0 ? (shiftQty + tripCapacity - 1) / tripCapacity : 1;

            ShiftProductionResult result = buildResult(machineCode, shiftConfig, task, shiftQty,
                    tripCapacity, cars, startTime, endTime, false, true, task.getIsContinueTask());

            results.add(result);
            remainingQty -= shiftQty;
        }

        return results;
    }

    // ==================== 5. 普通任务排产 ====================

    /**
     * 普通任务排产：波浪放置，每个班次车数相差不超过1
     */
    private List<ShiftProductionResult> scheduleNormalTask(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            String machineCode,
            ScheduleContextVo context,
            List<CxShiftConfig> dayShifts,
            LocalDate scheduleDate,
            int tripCapacity) {

        List<ShiftProductionResult> results = new ArrayList<>();
        int totalQty = task.getEndingExtraInventory();
        log.info("scheduleNormalTask: embryoCode={}, 待排产量={}, tripCapacity={}, dayShifts.size={}",
                task.getEmbryoCode(), totalQty, tripCapacity, dayShifts != null ? dayShifts.size() : "null");
        if (totalQty <= 0) {
            return results;
        }

        int requiredCars = tripCapacity > 0 ? (totalQty + tripCapacity - 1) / tripCapacity : 1;
        int[] shiftCars = calculateWaveCars(requiredCars, dayShifts);
        log.info("【波浪分配】胎胚={}, 待排={}条, 每车={}条, 需={}车, 各班分配={}",
                 task.getEmbryoCode(), totalQty, tripCapacity, requiredCars, Arrays.toString(shiftCars));

        int hourlyCapacity = getMachineHourlyCapacity(machineCode, task.getMaterialCode(), task.getStructureName(), context);
        int remainingCars = requiredCars;
        int remainingQty = totalQty;

        for (int i = 0; i < dayShifts.size() && remainingCars > 0 && remainingQty > 0; i++) {
            CxShiftConfig shiftConfig = dayShifts.get(i);
            int carsForShift = shiftCars[i];

            if (carsForShift <= 0) {
                continue;
            }

            carsForShift = Math.min(carsForShift, remainingCars);
            int batchQty = Math.min(carsForShift * tripCapacity, remainingQty);

            // 计算时间
            LocalDateTime startTime = calculateStartTime(machineCode, shiftConfig, scheduleDate, context);
            double productionHours = (double) batchQty / hourlyCapacity;
            LocalDateTime endTime = startTime.plusMinutes((long) (productionHours * 60));

            // 班次结束时间检查
            LocalDateTime shiftEndTime = calculateShiftEndTime(shiftConfig, scheduleDate);
            if (shiftEndTime != null && endTime.isAfter(shiftEndTime)) {
                long availableMinutes = Duration.between(startTime, shiftEndTime).toMinutes();
                availableMinutes -= getMachinePrepareMinutes(machineCode, context);
                int availableQty = (int) (availableMinutes * hourlyCapacity / 60);
                // 产能不足时按实际可生产量下，不强制整车取整
                batchQty = Math.min(Math.max(0, availableQty), remainingQty);
                carsForShift = tripCapacity > 0 ? (batchQty + tripCapacity - 1) / tripCapacity : (batchQty > 0 ? 1 : 0);
                endTime = shiftEndTime;
            }

            if (batchQty <= 0) {
                log.info("scheduleNormalTask: 跳过 batchQty=0, carsForShift={}, remainingCars={}", carsForShift, remainingCars);
                continue;
            }

            // 根据 batchQty 重新计算实际车数（不足一车算1车）
            carsForShift = tripCapacity > 0 ? (batchQty + tripCapacity - 1) / tripCapacity : 1;

            log.info("【硫化排产完成】胎胚={}, 班次={}, 产量={}条, 车数={}", 
                     task.getEmbryoCode(), shiftConfig.getShiftCode(), batchQty, carsForShift);
            ShiftProductionResult result = buildResult(machineCode, shiftConfig, task, batchQty,
                    tripCapacity, carsForShift, startTime, endTime, false, false, task.getIsContinueTask());

            results.add(result);
            remainingCars -= carsForShift;
            remainingQty -= batchQty;
        }

        if (remainingQty > 0) {
            log.warn("普通任务 {} 还有 {} 条未排产，产能不足", task.getEmbryoCode(), remainingQty);
        }

        return results;
    }

    // ==================== 波浪分配 ====================

    /**
     * 计算波浪分配车数
     *
     * <p>按比例分配车数到各班次，确保每个班次车数相差不超过1
     *
     * @param requiredCars 需要的车数
     * @param dayShifts    班次配置
     * @param dayShifts    班次配置
     * @return 各班次车数数组
     */
    private int[] calculateWaveCars(int requiredCars, List<CxShiftConfig> dayShifts) {

        int shiftCount = dayShifts.size();
        int[] shiftCars = new int[shiftCount];

        if (requiredCars <= 0) {
            return shiftCars;
        }

        // 均分车数：每个班次车数相差不超过1
        int base = requiredCars / shiftCount;
        int remainder = requiredCars % shiftCount;

        // 全部初始化为基础车数
        for (int i = 0; i < shiftCount; i++) {
            shiftCars[i] = base;
        }

        // 将余数对称分配：从外向内配对，每对两侧各+1；奇数余数给中间班次
        int left = 0;
        int right = shiftCount - 1;
        while (remainder > 0 && left <= right) {
            if (left == right) {
                // 中间班次，+1
                shiftCars[left]++;
                remainder--;
            } else if (remainder >= 2) {
                // 两侧对称各+1
                shiftCars[left]++;
                shiftCars[right]++;
                remainder -= 2;
            } else {
                // remainder == 1，给中间
                shiftCars[shiftCount / 2]++;
                remainder--;
            }
            left++;
            right--;
        }

        log.debug("波浪分配：需要{}车，分配结果：{}", requiredCars, Arrays.toString(shiftCars));

        return shiftCars;
    }

    /**
     * 将总量按条数均分到各班次（与 calculateWaveCars 对称，但分配的是条数而非车数）
     *
     * <p>分配结果对称：两侧多、中间少（余数为1时中间多1）
     *
     * @param totalQuantity 总条数
     * @param shiftCount    班次数
     * @return 各班次分配条数
     */
    private int[] distributeQuantityEvenly(int totalQuantity, int shiftCount) {
        int[] quantities = new int[shiftCount];
        if (totalQuantity <= 0 || shiftCount <= 0) {
            return quantities;
        }

        int base = totalQuantity / shiftCount;
        int remainder = totalQuantity % shiftCount;

        for (int i = 0; i < shiftCount; i++) {
            quantities[i] = base;
        }

        // 对称分配余数
        int left = 0;
        int right = shiftCount - 1;
        while (remainder > 0 && left <= right) {
            if (left == right) {
                quantities[left]++;
                remainder--;
            } else if (remainder >= 2) {
                quantities[left]++;
                quantities[right]++;
                remainder -= 2;
            } else {
                quantities[shiftCount / 2]++;
                remainder--;
            }
            left++;
            right--;
        }

        return quantities;
    }

    // ==================== 停产反推计算 ====================

    /**
     * 查找任务对应的硫化排程结果
     */
    private LhScheduleResult findLhScheduleResult(Long lhId, ScheduleContextVo context) {
        if (lhId == null || context.getLhScheduleResults() == null) {
            return null;
        }
        for (LhScheduleResult result : context.getLhScheduleResults()) {
            if (lhId.equals(result.getId())) {
                return result;
            }
        }
        return null;
    }

    /**
     * 判断是否为最后一个有产量的班次
     */
    private boolean isLastShiftWithQty(int currentIndex, int[] shiftCars) {
        for (int i = currentIndex + 1; i < shiftCars.length; i++) {
            if (shiftCars[i] > 0) {
                return false;
            }
        }
        return true;
    }

    // ==================== 收尾班次判断 ====================

    /**
     * 获取硫化的收尾班次
     *
     * <p>从硫化排程结果中找到最后一个有计划量的班次
     */
    private String getVulcanizeEndingShift(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                            ScheduleContextVo context) {
        LhScheduleResult lhResult = findLhScheduleResult(task.getLhId(), context);
        if (lhResult == null) {
            return SHIFT_DAY; // 默认早班
        }

        // 从后往前找最后一个有计划量的班次
        for (int i = 8; i >= 1; i--) {
            Integer planQty = getClassPlanQtyByIndex(lhResult, i);
            if (planQty != null && planQty > 0) {
                // class index → shift code 映射
                // 一般: 1-2=夜班, 3-4=早班, 5-6=中班, 7-8=次日班次
                if (i <= 2) return SHIFT_NIGHT;
                if (i <= 4) return SHIFT_DAY;
                if (i <= 6) return SHIFT_AFTERNOON;
                return SHIFT_AFTERNOON;
            }
        }

        return SHIFT_DAY;
    }

    /**
     * 根据班次索引获取硫化记录的计划量
     */
    private Integer getClassPlanQtyByIndex(LhScheduleResult lhResult, int classIndex) {
        switch (classIndex) {
            case 1: return lhResult.getClass1PlanQty();
            case 2: return lhResult.getClass2PlanQty();
            case 3: return lhResult.getClass3PlanQty();
            case 4: return lhResult.getClass4PlanQty();
            case 5: return lhResult.getClass5PlanQty();
            case 6: return lhResult.getClass6PlanQty();
            case 7: return lhResult.getClass7PlanQty();
            case 8: return lhResult.getClass8PlanQty();
            default: return null;
        }
    }

    /**
     * 获取班次索引
     */
    private int getShiftIndex(String shiftCode, List<CxShiftConfig> dayShifts) {
        for (int i = 0; i < dayShifts.size(); i++) {
            if (dayShifts.get(i).getShiftCode().equals(shiftCode)) {
                return i;
            }
        }
        return -1;
    }

    // ==================== 产能与时间计算 ====================

    /**
     * 计算班次时长（小时）
     */
    private int calculateShiftHours(CxShiftConfig shiftConfig) {
        Integer shiftHours = shiftConfig.getShiftHours();
        if (shiftHours != null && shiftHours > 0) {
            return shiftHours;
        }
        return DEFAULT_SHIFT_HOURS;
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
     * 
     * 根据精度计划的 precisionCycle（15/60分钟）和 completionStatus 判断：
     * - 未完成（completionStatus=0）的精度计划才扣减产能
     * - 精度计划不区分班次，当天有未完成计划即影响全天产能
     * - 按 precisionCycle 计算扣减时长
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
                // 只扣减未完成的精度计划（completionStatus=0）
                if ("1".equals(plan.getCompletionStatus())) {
                    continue;
                }
                
                // 根据 precisionCycle 计算扣减时长
                // precisionCycle: 15=15分钟, 60=60分钟
                int precisionMinutes = 60; // 默认60分钟
                if ("15".equals(plan.getPrecisionCycle())) {
                    precisionMinutes = 15;
                } else if ("60".equals(plan.getPrecisionCycle())) {
                    precisionMinutes = 60;
                }
                
                // 精度时长(小时) × 机台小时产能
                int precisionHours = (int) Math.ceil(precisionMinutes / 60.0);
                // 至少扣减4小时（精度校验标准时长）
                if (precisionHours < 4) {
                    precisionHours = 4;
                }
                return precisionHours * hourlyCapacity;
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

        LocalTime shiftStart = shiftConfig.getShiftStartTime();
        LocalDateTime startTime = LocalDateTime.of(scheduleDate, shiftStart);
        startTime = startTime.plusMinutes(getMachinePrepareMinutes(machineCode, context));

        return startTime;
    }

    /**
     * 计算班次结束时间
     */
    private LocalDateTime calculateShiftEndTime(CxShiftConfig shiftConfig, LocalDate scheduleDate) {
        LocalTime shiftEnd = shiftConfig.getShiftEndTime();
        if (shiftEnd == null) {
            log.warn("calculateShiftEndTime: shiftEnd is null, shiftCode={}", shiftConfig.getShiftCode());
            return null;
        }
        LocalDateTime endTime = LocalDateTime.of(scheduleDate, shiftEnd);

        // 夜班跨天
        if (shiftConfig.getIsCrossDay() != null && shiftConfig.getIsCrossDay() == 1) {
            endTime = endTime.plusDays(1);
        }

        return endTime;
    }

    /**
     * 计算机台小时产能
     *
     * <p>每个机台生产不同物料成型一条胎的时间不一样，需要动态计算：
     * <ol>
     *   <li>从 materialLhCapacityMap 获取该物料的日硫化量</li>
     *   <li>从 structureLhRatioMap 通过 结构+机型 获取配比 (lhMachineMaxQty)</li>
     *   <li>成型一条胎的时间(s) = 86400 / (配比 × 日硫化量)</li>
     *   <li>小时产能 = 3600 / 成型一条胎的时间(s)</li>
     * </ol>
     *
     * @param machineCode   机台编码
     * @param materialCode  成品物料编码（materialLhCapacityMap 的 key 是 materialCode）
     * @param structureName 结构名称
     * @param context       排程上下文
     * @return 小时产能（条/小时）
     */
    private int getMachineHourlyCapacity(String machineCode, String materialCode,
                                          String structureName, ScheduleContextVo context) {
        // 1. 获取日硫化量（materialLhCapacityMap 的 key 是 materialCode，不是 embryoCode）
        Integer dailyLhCapacity = null;
        Map<String, MonthPlanProductLhCapacityVo> lhCapacityMap = context.getMaterialLhCapacityMap();
        if (lhCapacityMap != null && materialCode != null) {
            MonthPlanProductLhCapacityVo capacityVo = lhCapacityMap.get(materialCode);
            if (capacityVo != null) {
                dailyLhCapacity = capacityVo.getDefaultDayVulcanizationQty();
            }
        }

        // 2. 获取配比（结构+机型 → lhMachineMaxQty）
        int ratio = 1;
        if (context.getStructureLhRatioMap() != null && structureName != null && machineCode != null) {
            // 先通过机台编码查机型，再以 机型+结构 组合 key 查配比
            Map<String, String> machineTypeCodeMap = context.getMachineTypeCodeMap();
            String machineTypeCode = machineTypeCodeMap != null ? machineTypeCodeMap.get(machineCode) : null;
            if (machineTypeCode != null) {
                MdmStructureLhRatio lhRatio = context.getStructureLhRatioMap().get(machineTypeCode + "|" + structureName);
                if (lhRatio != null && lhRatio.getLhMachineMaxQty() != null && lhRatio.getLhMachineMaxQty() > 0) {
                    ratio = lhRatio.getLhMachineMaxQty();
                }
            }
        }

        if (dailyLhCapacity != null && dailyLhCapacity > 0) {
            // 3. 成型一条胎的时间(s) = 86400 / (配比 × 日硫化量)
            BigDecimal timePerTire = BigDecimal.valueOf(SECONDS_PER_DAY)
                    .divide(BigDecimal.valueOf((long) ratio * dailyLhCapacity), 2, RoundingMode.HALF_UP);

            // 4. 小时产能 = 3600 / 成型一条胎的时间(s)
            if (timePerTire.compareTo(BigDecimal.ZERO) > 0) {
                int hourlyCapacity = BigDecimal.valueOf(SECONDS_PER_HOUR)
                        .divide(timePerTire, 0, RoundingMode.FLOOR)
                        .intValue();
                log.info("机台 {} 物料 {} 小时产能计算: 日硫化量={}, 配比={}, 单条耗时={}s, 产能={}条/h",
                        machineCode, materialCode, dailyLhCapacity, ratio, timePerTire, hourlyCapacity);
                return hourlyCapacity;
            }
        }

        log.warn("无法计算机台 {} 物料 {} 的小时产能(日硫化量={}, 配比={})，使用默认值 {}",
                machineCode, materialCode, dailyLhCapacity, ratio, DEFAULT_HOURLY_CAPACITY);
        return DEFAULT_HOURLY_CAPACITY;
    }

    /**
     * 获取结构的整车容量
     */
    private int getTripCapacity(String structureCode, ScheduleContextVo context) {
        if (context.getStructureShiftCapacities() != null) {
            for (CxStructureTreadConfig capacity : context.getStructureShiftCapacities()) {
                if (capacity.getStructureCode() != null
                        && capacity.getStructureCode().equals(structureCode)) {
                    if (capacity.getTreadCount() != null && capacity.getTreadCount() > 0) {
                        return capacity.getTreadCount();
                    }
                }
            }
        }
        return context.getDefaultTripCapacity() != null ? context.getDefaultTripCapacity() : DEFAULT_TRIP_CAPACITY;
    }

    /**
     * 获取机台准备时间（分钟）
     */
    private int getMachinePrepareMinutes(String machineCode, ScheduleContextVo context) {
        return DEFAULT_MACHINE_PREPARE_MINUTES;
    }

    /**
     * 判断是否关键产品
     */
    private boolean isKeyProduct(CoreScheduleAlgorithmService.DailyEmbryoTask task, ScheduleContextVo context) {
        if (task == null || context == null) {
            return false;
        }
        Set<String> keyProductCodes = context.getKeyProductCodes();
        if (keyProductCodes == null || keyProductCodes.isEmpty()) {
            return false;
        }
        return keyProductCodes.contains(task.getEmbryoCode())
                || keyProductCodes.contains(task.getMaterialCode());
    }

    // ==================== 工具方法 ====================

    /**
     * 构建班次排产结果
     */
    private ShiftProductionResult buildResult(
            String machineCode,
            CxShiftConfig shiftConfig,
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            int quantity,
            int tripCapacity,
            int cars,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Boolean isTrialTask,
            Boolean isEndingTask,
            Boolean isContinueTask) {

        // 从班次配置获取班次序号
        int sequence = shiftConfig.getDayShiftOrder() != null ? shiftConfig.getDayShiftOrder() : 1;

        ShiftProductionResult result = new ShiftProductionResult();
        result.setMachineCode(machineCode);
        result.setShiftCode(shiftConfig.getShiftCode());
        result.setShiftName(shiftConfig.getShiftName());
        result.setEmbryoCode(task.getEmbryoCode());
        result.setMaterialCode(task.getMaterialCode());
        result.setMaterialDesc(task.getMaterialDesc());
        result.setMainMaterialDesc(task.getMainMaterialDesc());
        result.setStructureName(task.getStructureName());
        result.setQuantity(quantity);
        result.setTripNo(String.valueOf(sequence));
        result.setTripCapacity(tripCapacity);
        result.setStockHours(task.getStockHours());
        result.setSequence(sequence);
        result.setCarsForShift(cars);
        result.setPlanStartTime(startTime);
        result.setPlanEndTime(endTime);
        result.setIsTrialTask(isTrialTask);
        result.setIsEndingTask(isEndingTask);
        result.setIsContinueTask(isContinueTask);
        // 设置是否收尾最后一批（从任务中获取）
        result.setIsLastEndingBatch(task.getIsLastEndingBatch());
        // 保存来源任务（用于均衡计算时获取 vulcanizeMachineCount）
        result.setSourceTask(task);

        return result;
    }

    // ==================== 数据结构 ====================

    /**
     * 班次排产结果
     */
    @lombok.Data
    public static class ShiftProductionResult {
        /** 机台编码 */
        private String machineCode;
        /** 班次编码 */
        private String shiftCode;
        /** 班次名称 */
        private String shiftName;
        /** 胎胚编码 */
        private String embryoCode;
        /** 物料编号（成品物料编码） */
        private String materialCode;
        /** 物料描述 */
        private String materialDesc;
        /** 主物料描述（胎胚描述） */
        private String mainMaterialDesc;
        /** 结构名称 */
        private String structureName;
        /** 排产数量（条） */
        private Integer quantity;
        /** 车次号（班次内第几车） */
        private String tripNo;
        /** 本车次容量（整车条数） */
        private Integer tripCapacity;
        /** 库存可供硫化时长（小时） */
        private BigDecimal stockHours;
        /** 顺位（班次内排序） */
        private Integer sequence;
        /** 计划开始时间 */
        private LocalDateTime planStartTime;
        /** 计划结束时间 */
        private LocalDateTime planEndTime;
        /** 是否试制任务 */
        private Boolean isTrialTask;
        /** 是否收尾任务 */
        private Boolean isEndingTask;
        /** 是否续作任务 */
        private Boolean isContinueTask;
        /** 该班次分配的车数 */
        private Integer carsForShift;
        /** 机台小时产能（条/小时） */
        private Integer hourCapacity;
        /** 是否收尾最后一批（不补整车） */
        private Boolean isLastEndingBatch;
        /** 来源任务（用于均衡计算：获取硫化机数 vulcanizeMachineCount） */
        private CoreScheduleAlgorithmService.DailyEmbryoTask sourceTask;
    }
}
