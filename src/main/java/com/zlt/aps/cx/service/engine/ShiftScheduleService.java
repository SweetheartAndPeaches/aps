package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.CxMachineStructureCapacity;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmDevicePlanShut;
import com.zlt.aps.mdm.api.domain.entity.CxPrecisionPlan;
import com.zlt.aps.mdm.api.domain.entity.MdmStructureTreadConfig;
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
 * <p>负责将待排产量分配到具体的班次和时间段，支持5种任务类型的班次分配：
 * <ul>
 *   <li>普通任务：波浪放置，每个班次车数相差不超过1</li>
 *   <li>收尾任务：只能在硫化的收尾班次或之前安排</li>
 *   <li>开产任务：成型提前一班开始，首班6小时，关键产品从第二班开始</li>
 *   <li>停产任务：库存全部消耗，反推计划量</li>
 *   <li>试制任务：只在早班/中班，双数，不补整车</li>
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

    /** 默认整车容量（条/车） */
    private static final int DEFAULT_TRIP_CAPACITY = 12;

    /** 默认机台小时产能（条/小时） */
    private static final int DEFAULT_HOURLY_CAPACITY = 50;

    /** 开产首班时长上限（小时） */
    private static final int OPENING_FIRST_SHIFT_MAX_HOURS = 6;

    /** 默认班次时长（小时） */
    private static final int DEFAULT_SHIFT_HOURS = 8;

    /** 默认机台准备时间（分钟） */
    private static final int DEFAULT_MACHINE_PREPARE_MINUTES = 30;

    /** 默认精度计划时长（小时） */
    private static final int DEFAULT_PRECISION_HOURS = 4;

    /** 整车取整模式：向上取整 */
    private static final String ROUND_MODE_CEILING = "CEILING";
    /** 整车取整模式：向下取整 */
    private static final String ROUND_MODE_FLOOR = "FLOOR";
    /** 整车取整模式：四舍五入 */
    private static final String ROUND_MODE_ROUND = "ROUND";

    /** 班次编码：夜班 */
    public static final String SHIFT_NIGHT = "SHIFT_NIGHT";
    /** 班次编码：早班 */
    public static final String SHIFT_DAY = "SHIFT_DAY";
    /** 班次编码：中班 */
    public static final String SHIFT_AFTERNOON = "SHIFT_AFTERNOON";

    // ==================== S5.3.7 班次精排 ====================

    /**
     * S5.3.7 按班次排产（任务级精排）
     *
     * <p>将单个任务的待排产量分配到具体的班次和时间段，支持5种任务类型：
     * <ul>
     *   <li>普通任务：波浪放置，每个班次车数相差不超过1</li>
     *   <li>收尾任务：只能在硫化的收尾班次或之前安排</li>
     *   <li>开产任务：首班6小时，关键产品从第二班开始</li>
     *   <li>停产任务：库存全部消耗，反推计划量</li>
     *   <li>试制任务：只在早班/中班，双数，不补整车</li>
     * </ul>
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

        int requiredCars = task.getRequiredCars() != null ? task.getRequiredCars() : 0;
        if (requiredCars <= 0) {
            return results;
        }

        // 获取胎面整车条数
        int treadCount = getTripCapacity(task.getStructureName(), context);

        // 班次波浪分配
        int[] shiftCars = calculateWaveCars(requiredCars, dayShifts, context, task);

        // 处理试制任务：必须是双数，不补整车
        boolean isTrial = Boolean.TRUE.equals(task.getIsTrialTask());
        if (isTrial) {
            shiftCars = adjustTrialShiftCars(shiftCars, dayShifts);
        }

        // 处理收尾任务：只能在硫化收尾班次或之前安排
        boolean isEnding = Boolean.TRUE.equals(task.getIsEndingTask());
        if (isEnding) {
            shiftCars = adjustEndingShiftCars(shiftCars, dayShifts, task);
        }

        // 处理开产任务：首班6小时，关键产品从第二班开始
        boolean isOpening = Boolean.TRUE.equals(task.getIsOpeningDayTask());
        if (isOpening && context.getCurrentScheduleDay() != null && context.getCurrentScheduleDay() == 1) {
            shiftCars = adjustOpeningShiftCars(shiftCars, dayShifts, task, context);
        }

        // 生成班次排产结果
        int remainingCars = requiredCars;
        for (int i = 0; i < dayShifts.size() && remainingCars > 0; i++) {
            CxShiftConfig shiftConfig = dayShifts.get(i);
            int carsForShift = shiftCars[i];

            if (carsForShift <= 0) {
                continue;
            }

            // 试制任务：必须是双数
            if (isTrial && carsForShift % 2 != 0) {
                carsForShift = carsForShift - 1;
                if (carsForShift <= 0) {
                    continue;
                }
            }

            // 实际车数不能超过剩余车数
            carsForShift = Math.min(carsForShift, remainingCars);

            int batchQty = carsForShift * treadCount;

            // 计算生产时间
            int hourlyCapacity = getMachineHourlyCapacity(machineCode, task.getStructureName(), context);
            double productionHours = (double) batchQty / hourlyCapacity;

            LocalDateTime startTime = calculateStartTime(machineCode, shiftConfig, scheduleDate, context);
            LocalDateTime endTime = startTime.plusMinutes((long) (productionHours * 60));

            // 班次结束时间检查
            LocalDateTime shiftEndTime = calculateShiftEndTime(shiftConfig, scheduleDate);
            if (endTime.isAfter(shiftEndTime)) {
                long availableMinutes = java.time.Duration.between(startTime, shiftEndTime).toMinutes();
                availableMinutes -= getMachinePrepareMinutes(machineCode, context);
                int availableQty = (int) (availableMinutes * hourlyCapacity / 60);
                availableQty = roundToTrip(availableQty, ROUND_MODE_FLOOR, treadCount);
                batchQty = Math.max(0, availableQty);
                carsForShift = treadCount > 0 ? batchQty / treadCount : 0;
                endTime = shiftEndTime;
            }

            if (batchQty <= 0) {
                continue;
            }

            ShiftProductionResult result = new ShiftProductionResult();
            result.setMachineCode(machineCode);
            result.setShiftCode(shiftConfig.getShiftCode());
            result.setShiftName(shiftConfig.getShiftName());
            result.setEmbryoCode(task.getMaterialCode());
            result.setMaterialCode(task.getRelatedMaterialCode());
            result.setMaterialDesc(task.getMaterialDesc());
            result.setMainMaterialDesc(task.getMainMaterialDesc());
            result.setStructureName(task.getStructureName());
            result.setQuantity(batchQty);
            result.setPlanStartTime(startTime);
            result.setPlanEndTime(endTime);
            result.setIsTrialTask(task.getIsTrialTask());
            result.setIsEndingTask(task.getIsEndingTask());
            result.setIsContinueTask(task.getIsContinueTask());
            result.setCarsForShift(carsForShift);

            results.add(result);
            remainingCars -= carsForShift;

            log.debug("班次排产：机台={}, 班次={}, 胎胚={}, 物料编号={}, 车数={}, 数量={}, 时间={}-{}",
                    machineCode, shiftConfig.getShiftName(), task.getMaterialCode(),
                    task.getRelatedMaterialCode(), carsForShift, batchQty, startTime, endTime);
        }

        if (remainingCars > 0) {
            log.warn("任务 {} 还有 {} 车未排产，产能不足", task.getMaterialCode(), remainingCars);
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
     * @param context      排程上下文
     * @param task         任务
     * @return 各班次车数数组
     */
    private int[] calculateWaveCars(
            int requiredCars,
            List<CxShiftConfig> dayShifts,
            ScheduleContextVo context,
            CoreScheduleAlgorithmService.DailyEmbryoTask task) {

        int shiftCount = dayShifts.size();
        int[] shiftCars = new int[shiftCount];

        if (requiredCars <= 0) {
            return shiftCars;
        }

        int[] waveRatio = context.getWaveRatio();
        if (waveRatio == null || waveRatio.length < shiftCount) {
            waveRatio = DEFAULT_WAVE_RATIO;
        }

        // 按班次顺序映射波浪比例：夜班、早班、中班
        int[] adjustedRatio = mapShiftRatio(waveRatio, dayShifts);

        int totalRatio = 0;
        for (int ratio : adjustedRatio) {
            totalRatio += ratio;
        }

        // 按比例分配车数
        int remainingCars = requiredCars;
        for (int i = 0; i < shiftCount; i++) {
            shiftCars[i] = requiredCars * adjustedRatio[i] / totalRatio;
            remainingCars -= shiftCars[i];
        }

        // 将剩余车数分配到靠前的班次（实现波浪效果）
        for (int i = 0; i < shiftCount && remainingCars > 0; i++) {
            shiftCars[i]++;
            remainingCars--;
        }

        // 波浪均衡：确保相邻班次车数相差不超过1
        shiftCars = balanceWaveDistribution(shiftCars);

        log.debug("波浪分配：需要{}车，分配结果：{}", requiredCars, Arrays.toString(shiftCars));

        return shiftCars;
    }

    /**
     * 将波浪比例映射到具体班次
     *
     * @param waveRatio 波浪比例数组 [夜班, 早班, 中班]
     * @param dayShifts 班次配置
     * @return 各班次对应的比例值
     */
    private int[] mapShiftRatio(int[] waveRatio, List<CxShiftConfig> dayShifts) {
        int shiftCount = dayShifts.size();
        int[] adjustedRatio = new int[shiftCount];

        for (int i = 0; i < shiftCount; i++) {
            String shiftCode = dayShifts.get(i).getShiftCode();
            if (SHIFT_NIGHT.equals(shiftCode)) {
                adjustedRatio[i] = waveRatio.length > 0 ? waveRatio[0] : 1;
            } else if (SHIFT_DAY.equals(shiftCode)) {
                adjustedRatio[i] = waveRatio.length > 1 ? waveRatio[1] : 2;
            } else if (SHIFT_AFTERNOON.equals(shiftCode)) {
                adjustedRatio[i] = waveRatio.length > 2 ? waveRatio[2] : 1;
            } else {
                adjustedRatio[i] = 1;
            }
        }

        return adjustedRatio;
    }

    /**
     * 波浪均衡：确保相邻班次车数相差不超过1
     *
     * @param shiftCars 各班次车数
     * @return 均衡后的车数
     */
    private int[] balanceWaveDistribution(int[] shiftCars) {
        if (shiftCars == null || shiftCars.length <= 1) {
            return shiftCars;
        }

        int n = shiftCars.length;
        boolean changed;

        do {
            changed = false;
            for (int i = 1; i < n; i++) {
                if (shiftCars[i] < shiftCars[i - 1] - 1) {
                    shiftCars[i]++;
                    changed = true;
                } else if (shiftCars[i] > shiftCars[i - 1] + 1) {
                    shiftCars[i]--;
                    changed = true;
                }
            }
            for (int i = n - 2; i >= 0; i--) {
                if (shiftCars[i] < shiftCars[i + 1] - 1) {
                    shiftCars[i]++;
                    changed = true;
                } else if (shiftCars[i] > shiftCars[i + 1] + 1) {
                    shiftCars[i]--;
                    changed = true;
                }
            }
        } while (changed);

        return shiftCars;
    }

    // ==================== 特殊任务调整 ====================

    /**
     * 调整试制任务车数：必须是双数，夜班不排
     *
     * @param shiftCars 各班次车数
     * @param dayShifts 班次配置
     * @return 调整后的车数
     */
    private int[] adjustTrialShiftCars(int[] shiftCars, List<CxShiftConfig> dayShifts) {
        if (shiftCars == null) {
            return shiftCars;
        }

        for (int i = 0; i < shiftCars.length && i < dayShifts.size(); i++) {
            String shiftCode = dayShifts.get(i).getShiftCode();
            if (SHIFT_NIGHT.equals(shiftCode)) {
                shiftCars[i] = 0;
            } else {
                if (shiftCars[i] % 2 != 0) {
                    shiftCars[i] = shiftCars[i] - 1;
                }
            }
        }

        return shiftCars;
    }

    /**
     * 调整收尾任务车数：只能在硫化收尾班次或之前安排
     *
     * <p>如果硫化的收尾班次是夜班，则收尾任务只能在夜班安排；
     * 如果是早班，则可以在夜班和早班安排；依此类推。
     *
     * @param shiftCars 各班次车数
     * @param dayShifts 班次配置
     * @param task      任务
     * @return 调整后的车数
     */
    private int[] adjustEndingShiftCars(
            int[] shiftCars,
            List<CxShiftConfig> dayShifts,
            CoreScheduleAlgorithmService.DailyEmbryoTask task) {

        if (shiftCars == null) {
            return shiftCars;
        }

        String vulcanizeEndingShift = getVulcanizeEndingShift(task);
        int maxShiftIndex = getShiftIndex(vulcanizeEndingShift, dayShifts);

        if (maxShiftIndex < 0) {
            maxShiftIndex = dayShifts.size() - 1;
        }

        for (int i = 0; i < shiftCars.length; i++) {
            if (i > maxShiftIndex) {
                shiftCars[maxShiftIndex] += shiftCars[i];
                shiftCars[i] = 0;
            }
        }

        log.debug("收尾任务调整：硫化收尾班次={}，分配结果：{}", vulcanizeEndingShift, Arrays.toString(shiftCars));

        return shiftCars;
    }

    /**
     * 获取硫化的收尾班次
     *
     * @param task 任务
     * @return 班次编码
     */
    private String getVulcanizeEndingShift(CoreScheduleAlgorithmService.DailyEmbryoTask task) {
        return SHIFT_DAY;
    }

    /**
     * 获取班次索引
     *
     * @param shiftCode 班次编码
     * @param dayShifts 班次配置
     * @return 班次索引，未找到返回-1
     */
    private int getShiftIndex(String shiftCode, List<CxShiftConfig> dayShifts) {
        for (int i = 0; i < dayShifts.size(); i++) {
            if (dayShifts.get(i).getShiftCode().equals(shiftCode)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 调整开产任务车数：首班6小时，关键产品从第二班开始
     *
     * @param shiftCars 各班次车数
     * @param dayShifts 班次配置
     * @param task      任务
     * @param context   排程上下文
     * @return 调整后的车数
     */
    private int[] adjustOpeningShiftCars(
            int[] shiftCars,
            List<CxShiftConfig> dayShifts,
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextVo context) {

        if (shiftCars == null || shiftCars.length == 0) {
            return shiftCars;
        }

        int firstShiftCars = shiftCars[0];

        // 首班只排6小时
        int shiftHours = calculateShiftHours(dayShifts.get(0));
        if (shiftHours > OPENING_FIRST_SHIFT_MAX_HOURS) {
            int reducedCars = (int) Math.round(
                    (double) firstShiftCars * (shiftHours - OPENING_FIRST_SHIFT_MAX_HOURS) / shiftHours);
            shiftCars[0] = firstShiftCars - reducedCars;
            log.debug("开产首班{}小时，原始{}车，减少{}车，实际{}车",
                    shiftHours, firstShiftCars, reducedCars, shiftCars[0]);
        }

        // 关键产品从第二班开始
        if (isKeyProduct(task, context) && shiftCars.length > 1) {
            shiftCars[0] = 0;
            shiftCars[1] = firstShiftCars;
            log.debug("开产关键产品从第二班开始安排，{}车", firstShiftCars);
        }

        return shiftCars;
    }

    /**
     * 判断是否关键产品
     *
     * @param task    任务
     * @param context 排程上下文
     * @return 是否关键产品
     */
    private boolean isKeyProduct(CoreScheduleAlgorithmService.DailyEmbryoTask task, ScheduleContextVo context) {
        if (task == null || context == null) {
            return false;
        }
        Set<String> keyProductCodes = context.getKeyProductCodes();
        if (keyProductCodes == null || keyProductCodes.isEmpty()) {
            return false;
        }
        return keyProductCodes.contains(task.getMaterialCode())
                || keyProductCodes.contains(task.getRelatedMaterialCode());
    }

    // ==================== 产能与时间计算 ====================

    /**
     * 计算班次可用产能
     *
     * @param machineCode    机台编码
     * @param shiftConfig    班次配置
     * @param hourlyCapacity 小时产能
     * @param context        排程上下文
     * @return 可用产能（条）
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

        return Math.max(0, baseCapacity - shutdownDeduction - precisionDeduction);
    }

    /**
     * 计算班次时长（小时）
     *
     * @param shiftConfig 班次配置
     * @return 班次时长
     */
    private int calculateShiftHours(CxShiftConfig shiftConfig) {
        Integer startHour = shiftConfig.getStartHour();
        Integer endHour = shiftConfig.getEndHour();

        if (startHour == null || endHour == null) {
            return DEFAULT_SHIFT_HOURS;
        }

        if (endHour < startHour) {
            return (24 - startHour) + endHour;
        }
        return endHour - startHour;
    }

    /**
     * 计算班次停机扣减产能
     *
     * @param machineCode    机台编码
     * @param shiftConfig    班次配置
     * @param hourlyCapacity 小时产能
     * @param context        排程上下文
     * @return 扣减产能（条）
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
     * @param machineCode    机台编码
     * @param shiftConfig    班次配置
     * @param hourlyCapacity 小时产能
     * @param context        排程上下文
     * @return 扣减产能（条）
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
                    int precisionHours = plan.getEstimatedHours() != null
                            ? plan.getEstimatedHours().intValue() : DEFAULT_PRECISION_HOURS;
                    return precisionHours * hourlyCapacity;
                }
            }
        }

        return 0;
    }

    /**
     * 计算生产开始时间
     *
     * @param machineCode   机台编码
     * @param shiftConfig   班次配置
     * @param scheduleDate  排程日期
     * @param context       排程上下文
     * @return 开始时间
     */
    private LocalDateTime calculateStartTime(
            String machineCode,
            CxShiftConfig shiftConfig,
            LocalDate scheduleDate,
            ScheduleContextVo context) {

        int startHour = shiftConfig.getStartHour() != null ? shiftConfig.getStartHour() : 0;
        int startMinute = shiftConfig.getStartMinute() != null ? shiftConfig.getStartMinute() : 0;

        LocalDateTime startTime = LocalDateTime.of(scheduleDate, LocalTime.of(startHour, startMinute));
        startTime = startTime.plusMinutes(getMachinePrepareMinutes(machineCode, context));

        return startTime;
    }

    /**
     * 计算班次结束时间
     *
     * @param shiftConfig   班次配置
     * @param scheduleDate  排程日期
     * @return 结束时间
     */
    private LocalDateTime calculateShiftEndTime(CxShiftConfig shiftConfig, LocalDate scheduleDate) {
        int endHour = shiftConfig.getEndHour() != null ? shiftConfig.getEndHour() : DEFAULT_SHIFT_HOURS;
        int endMinute = shiftConfig.getEndMinute() != null ? shiftConfig.getEndMinute() : 0;

        LocalDateTime endTime = LocalDateTime.of(scheduleDate, LocalTime.of(endHour, endMinute));

        if (shiftConfig.getShiftCode().equals(SHIFT_NIGHT) && endHour <= OPENING_FIRST_SHIFT_MAX_HOURS) {
            endTime = endTime.plusDays(1);
        }

        return endTime;
    }

    /**
     * 获取机台小时产能
     *
     * @param machineCode   机台编码
     * @param structureName 结构名称
     * @param context       排程上下文
     * @return 小时产能（条/小时）
     */
    private int getMachineHourlyCapacity(String machineCode, String structureName, ScheduleContextVo context) {
        if (context.getMachineStructureCapacities() != null && machineCode != null && structureName != null) {
            for (CxMachineStructureCapacity capacity : context.getMachineStructureCapacities()) {
                if (machineCode.equals(capacity.getCxMachineCode())
                        && structureName.equals(capacity.getStructureCode())) {
                    return capacity.getHourlyCapacity() != null ? capacity.getHourlyCapacity() : DEFAULT_HOURLY_CAPACITY;
                }
            }
        }
        return context.getMachineHourlyCapacity() != null ? context.getMachineHourlyCapacity() : DEFAULT_HOURLY_CAPACITY;
    }

    /**
     * 获取结构的整车容量
     *
     * @param structureCode 结构编码
     * @param context       排程上下文
     * @return 整车容量（条/车）
     */
    private int getTripCapacity(String structureCode, ScheduleContextVo context) {
        if (context.getStructureShiftCapacities() != null) {
            for (MdmStructureTreadConfig capacity : context.getStructureShiftCapacities()) {
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
     *
     * @param machineCode 机台编码
     * @param context     排程上下文
     * @return 准备时间（分钟）
     */
    private int getMachinePrepareMinutes(String machineCode, ScheduleContextVo context) {
        return DEFAULT_MACHINE_PREPARE_MINUTES;
    }

    // ==================== 工具方法 ====================

    /**
     * 整车取整
     *
     * @param quantity     数量
     * @param mode         取整模式：CEILING/FLOOR/ROUND
     * @param tripCapacity 整车容量
     * @return 取整后的数量
     */
    private int roundToTrip(int quantity, String mode, int tripCapacity) {
        if (quantity <= 0) {
            return 0;
        }

        int trips;
        switch (mode) {
            case ROUND_MODE_CEILING:
                trips = (int) Math.ceil((double) quantity / tripCapacity);
                break;
            case ROUND_MODE_FLOOR:
                trips = (int) Math.floor((double) quantity / tripCapacity);
                break;
            case ROUND_MODE_ROUND:
            default:
                trips = (int) Math.round((double) quantity / tripCapacity);
                break;
        }

        return trips * tripCapacity;
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
        private int quantity;
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
    }
}
