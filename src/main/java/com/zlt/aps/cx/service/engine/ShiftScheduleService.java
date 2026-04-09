package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.entity.CxMachineStructureCapacity;
import com.zlt.aps.cx.entity.CxPrecisionPlan;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.vo.ScheduleContextVo;

import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
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

        log.info("班次分配开始，机台分配数: {}", allocations != null ? allocations.size() : 0);

        if (dayShifts == null || dayShifts.isEmpty()) {
            log.warn("班次配置为空");
            return results;
        }

        // 从班次配置提取班次编码数组
        String[] shiftCodes = dayShifts.stream()
                .map(CxShiftConfig::getShiftCode)
                .toArray(String[]::new);

        // 加载结构整车配置
        Map<String, MdmStructureTreadConfig> structureTreadConfigMap = buildStructureTreadConfigMap(context);

        for (CoreScheduleAlgorithmService.MachineAllocationResult allocation : allocations) {
            CoreScheduleAlgorithmService.ShiftAllocationResult shiftResult = new CoreScheduleAlgorithmService.ShiftAllocationResult();
            shiftResult.setMachineCode(allocation.getMachineCode());
            shiftResult.setTasks(allocation.getTaskAllocations());

            Map<String, Integer> shiftPlanQty = new LinkedHashMap<>();

            // 按任务结构获取班产整车数，计算波浪分配
            Map<String, Integer> structureWaveAllocation = calculateStructureWaveAllocation(
                    allocation.getTaskAllocations(),
                    structureTreadConfigMap,
                    allocation.getDailyCapacity(),
                    shiftCodes,
                    context);

            // 汇总各班次分配量
            for (CxShiftConfig shiftConfig : dayShifts) {
                String shiftCode = shiftConfig.getShiftCode();
                int shiftQty = structureWaveAllocation.getOrDefault(shiftCode, 0);
                shiftPlanQty.put(shiftCode, shiftQty);
            }
            
            // 调试日志
            log.info("班次分配结果: 机台={}, 总排量={}, 班次计划量={}", 
                    allocation.getMachineCode(), allocation.getUsedCapacity(), shiftPlanQty);

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
            Map<String, MdmStructureTreadConfig> structureTreadConfigMap,
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
            log.info("班次分配-任务: 结构={}, 排量={}", structureCode, taskQty);

            MdmStructureTreadConfig treadConfig = structureTreadConfigMap.get(structureCode);

            // 只在花纹数合理（<=排量）时使用花纹配置分配
            if (treadConfig != null && treadConfig.getTreadCount() != null && treadConfig.getTreadCount() > 0 && treadConfig.getTreadCount() <= taskQty) {
                int treadCount = treadConfig.getTreadCount();
                log.info("花纹配置分配: 结构={}, 排量={}, 花纹数={}", structureCode, taskQty, treadCount);
                int[] shiftQty = calculateShiftQtyByTreadCount(taskQty, treadCount, shiftCodes, context);

                for (int i = 0; i < shiftCodes.length; i++) {
                    int qty = shiftQty[i];
                    shiftTotalQty.merge(shiftCodes[i], qty, Integer::sum);
                    totalAssigned += qty;
                }
            } else {
                log.info("波浪比例分配: 结构={}, 排量={}", structureCode, taskQty);
                int[] waveQty = calculateWaveAllocation(taskQty, shiftCodes, context);

                for (int i = 0; i < shiftCodes.length; i++) {
                    shiftTotalQty.merge(shiftCodes[i], waveQty[i], Integer::sum);
                    totalAssigned += waveQty[i];
                }
            }
        }

        // 检查是否超过机台最大产能
        if (maxDailyCapacity != null && totalAssigned > maxDailyCapacity) {
            log.warn("班次分配超出机台最大产能: 分配量={}, 最大产能={}", 
                    totalAssigned, maxDailyCapacity);
            // 按比例缩减
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
     * 根据整车胎面条数计算各班次分配量
     */
    private int[] calculateShiftQtyByTreadCount(
            int taskQty,
            int treadCount,
            String[] shiftCodes,
            ScheduleContextVo context) {

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

        for (int i = 0; i < shiftCodes.length; i++) {
            int shiftQty = taskQty * adjustedRatio[i] / totalRatio;
            shiftQty = roundToTripQty(shiftQty, treadCount, "ROUND");

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
            // 支持两种格式：1. SHIFT_NIGHT/SHIFT_DAY/SHIFT_AFTERNOON 常量；2. DAY_D1/NIGHT_D2/AFTERNOON_D3 格式
            if (SHIFT_NIGHT.equals(shiftCode) || (shiftCode != null && shiftCode.startsWith("NIGHT_"))) {
                adjustedRatio[i] = waveRatio[0];
            } else if (SHIFT_DAY.equals(shiftCode) || (shiftCode != null && shiftCode.startsWith("DAY_"))) {
                adjustedRatio[i] = waveRatio[1];
            } else if (SHIFT_AFTERNOON.equals(shiftCode) || (shiftCode != null && shiftCode.startsWith("AFTERNOON_"))) {
                adjustedRatio[i] = waveRatio[2];
            } else {
                adjustedRatio[i] = 1;
            }
        }

        return adjustedRatio;
    }

    /**
     * 从上下文构建结构整车配置映射
     */
    private Map<String, MdmStructureTreadConfig> buildStructureTreadConfigMap(ScheduleContextVo context) {
        Map<String, MdmStructureTreadConfig> result = new HashMap<>();

        List<MdmStructureTreadConfig> configs = context.getStructureShiftCapacities();
        if (configs == null || configs.isEmpty()) {
            return result;
        }

        for (MdmStructureTreadConfig config : configs) {
            String structureCode = config.getStructureCode();
            if (structureCode != null) {
                result.put(structureCode, config);
            }
        }

        return result;
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
     * S5.3.7 按班次排产详细实现（按车分配版本）
     *
     * <p>将待排产量分配到具体的班次和时间段
     * <p>支持5种任务类型的班次分配：
     * <ul>
     *   <li>普通任务：波浪放置，每个班次车数相差不超过1</li>
     *   <li>收尾任务：只能在硫化的收尾班次或之前安排</li>
     *   <li>开产任务：成型提前一班开始，首班6小时，关键产品从第二班开始</li>
     *   <li>停产任务：库存全部消耗，反推计划量</li>
     *   <li>试制任务：只在早班/中班，双数，不补整车</li>
     * </ul>
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

        // 获取需要的车数
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
        if (isOpening && context.getCurrentScheduleDay() == 1) {
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
                // 超出班次时间，重新计算车数
                long availableMinutes = java.time.Duration.between(startTime, shiftEndTime).toMinutes();
                availableMinutes -= getMachinePrepareMinutes(machineCode, context);
                int availableQty = (int) (availableMinutes * hourlyCapacity / 60);
                availableQty = roundToTrip(availableQty, "FLOOR", treadCount);
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
            result.setMaterialCode(task.getMaterialCode());
            result.setMaterialName(task.getMaterialName());
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

            log.debug("班次排产：机台={}, 班次={}, 胎胚={}, 车数={}, 数量={}, 时间={}-{}",
                    machineCode, shiftConfig.getShiftName(), task.getMaterialCode(),
                    carsForShift, batchQty, startTime, endTime);
        }

        if (remainingCars > 0) {
            log.warn("任务 {} 还有 {} 车未排产，产能不足", task.getMaterialCode(), remainingCars);
        }

        return results;
    }

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

        // 获取波浪比例
        int[] waveRatio = context.getWaveRatio();
        if (waveRatio == null || waveRatio.length < shiftCount) {
            waveRatio = DEFAULT_WAVE_RATIO;
        }

        // 按班次顺序：夜班、早班、中班（根据班次编码映射）
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

        // 计算总比例
        int totalRatio = 0;
        for (int ratio : adjustedRatio) {
            totalRatio += ratio;
        }

        // 按比例分配车数
        int remainingCars = requiredCars;
        int[] baseCars = new int[shiftCount];

        for (int i = 0; i < shiftCount; i++) {
            baseCars[i] = requiredCars * adjustedRatio[i] / totalRatio;
            remainingCars -= baseCars[i];
        }

        // 波浪均衡：确保每个班次车数相差不超过1
        // 先按基础分配
        for (int i = 0; i < shiftCount; i++) {
            shiftCars[i] = baseCars[i];
        }

        // 将剩余车数分配到靠前的班次（实现波浪效果）
        if (remainingCars > 0) {
            for (int i = 0; i < shiftCount && remainingCars > 0; i++) {
                shiftCars[i]++;
                remainingCars--;
            }
        }

        // 确保波浪均衡：相邻班次车数相差不超过1
        shiftCars = balanceWaveDistribution(shiftCars);

        log.debug("波浪分配：需要{}车，分配结果：{}", requiredCars, Arrays.toString(shiftCars));

        return shiftCars;
    }

    /**
     * 波浪均衡：确保相邻班次车数相差不超过1
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
                // 如果当前班次比前一个班次少超过1，则增加
                if (shiftCars[i] < shiftCars[i - 1] - 1) {
                    shiftCars[i]++;
                    changed = true;
                }
                // 如果当前班次比前一个班次多超过1，则减少
                else if (shiftCars[i] > shiftCars[i - 1] + 1) {
                    shiftCars[i]--;
                    changed = true;
                }
            }
            // 再从后往前检查一次
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

    /**
     * 调整试制任务车数：必须是双数
     */
    private int[] adjustTrialShiftCars(int[] shiftCars, List<CxShiftConfig> dayShifts) {
        if (shiftCars == null) {
            return shiftCars;
        }

        // 试制任务只能在早班或中班，夜班不排
        for (int i = 0; i < shiftCars.length && i < dayShifts.size(); i++) {
            String shiftCode = dayShifts.get(i).getShiftCode();
            if (SHIFT_NIGHT.equals(shiftCode)) {
                // 夜班不排试制任务，将车数转到早班
                shiftCars[i] = 0;
            } else {
                // 确保是双数
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
     */
    private int[] adjustEndingShiftCars(
            int[] shiftCars,
            List<CxShiftConfig> dayShifts,
            CoreScheduleAlgorithmService.DailyEmbryoTask task) {

        if (shiftCars == null) {
            return shiftCars;
        }

        // 获取硫化的收尾班次（从任务中获取，如果没有则默认最后一个班次）
        String vulcanizeEndingShift = getVulcanizeEndingShift(task);
        int maxShiftIndex = getShiftIndex(vulcanizeEndingShift, dayShifts);

        if (maxShiftIndex < 0) {
            maxShiftIndex = dayShifts.size() - 1;
        }

        // 只在收尾班次及之前的班次安排
        for (int i = 0; i < shiftCars.length; i++) {
            if (i > maxShiftIndex) {
                // 将超出部分转到收尾班次
                shiftCars[maxShiftIndex] += shiftCars[i];
                shiftCars[i] = 0;
            }
        }

        log.debug("收尾任务调整：硫化收尾班次={}，分配结果：{}", vulcanizeEndingShift, Arrays.toString(shiftCars));

        return shiftCars;
    }

    /**
     * 获取硫化的收尾班次
     */
    private String getVulcanizeEndingShift(CoreScheduleAlgorithmService.DailyEmbryoTask task) {
        // 如果任务有收尾班次信息，使用它
        // 目前从上下文或配置中获取
        // 默认返回早班（如果硫化早班收尾，则成型也在早班及之前安排）
        return SHIFT_DAY;
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

    /**
     * 调整开产任务车数：首班6小时，关键产品从第二班开始
     */
    private int[] adjustOpeningShiftCars(
            int[] shiftCars,
            List<CxShiftConfig> dayShifts,
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextVo context) {

        if (shiftCars == null || shiftCars.length == 0) {
            return shiftCars;
        }

        // 首班只排6小时（如果是早班）
        String firstShiftCode = dayShifts.get(0).getShiftCode();
        int firstShiftCars = shiftCars[0];

        // 获取班次时长
        int shiftHours = calculateShiftHours(dayShifts.get(0));
        if (shiftHours > 6) {
            // 超出6小时的部分按比例减少
            int reducedCars = (int) Math.round((double) firstShiftCars * (shiftHours - 6) / shiftHours);
            shiftCars[0] = firstShiftCars - reducedCars;
            log.debug("开产首班{}小时，原始{}车，减少{}车，实际{}车",
                    shiftHours, firstShiftCars, reducedCars, shiftCars[0]);
        }

        // 关键产品从第二班开始
        boolean isKeyProduct = isKeyProduct(task, context);
        if (isKeyProduct && shiftCars.length > 1) {
            // 将首班关键产品移到第二班
            shiftCars[0] = 0;
            shiftCars[1] = firstShiftCars;
            log.debug("开产关键产品从第二班开始安排，{}车", firstShiftCars);
        }

        return shiftCars;
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
        return keyProductCodes.contains(task.getMaterialCode())
                || keyProductCodes.contains(task.getRelatedMaterialCode());
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
                    int precisionHours = plan.getEstimatedHours() != null ? plan.getEstimatedHours().intValue() : 4;
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
        /** 该班次分配的车数（按车分配模式） */
        private Integer carsForShift;
    }
}
