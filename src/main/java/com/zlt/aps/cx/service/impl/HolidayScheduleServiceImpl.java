package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.entity.config.CxKeyProduct;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.mapper.*;
import com.zlt.aps.cx.service.HolidayScheduleService;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmWorkCalendar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 节假日处理服务实现类
 * 
 * 按班次级别判断开产/停产逻辑：
 * - 停产班：本班次 = 0(停产)，不做处理
 * - 开产班（首个）：本班次 = 1(开产) 且 上个班次 = 0(停产)，走开产逻辑
 * - 停产前一天班（末个）：本班次 = 1(开产) 且 下个班次 = 0(停产)，走停产前一天逻辑
 *
 * @author APS Team
 */
@Slf4j
@Service
public class HolidayScheduleServiceImpl implements HolidayScheduleService {

    @Autowired
    private MdmWorkCalendarMapper workCalendarMapper;

    @Autowired
    private CxStockMapper stockMapper;

    @Autowired
    private LhScheduleResultMapper lhScheduleResultMapper;

    @Autowired
    private CxKeyProductMapper keyProductMapper;

    @Autowired
    private CxShiftConfigMapper shiftConfigMapper;

    @Autowired
    private CxParamConfigMapper paramConfigMapper;

    @Autowired
    private MdmMoldingMachineMapper moldingMachineMapper;

    /** 预留消化时间默认值（小时） */
    private static final int DEFAULT_RESERVED_DIGEST_HOURS = 1;

    /** 胎胚最长停放时间默认值（小时） */
    private static final int DEFAULT_MAX_PARKING_HOURS = 24;

    /** 成型工序编码 */
    private static final String PROC_CODE_CX = "03";

    /** 班次停产标志：0-停 */
    private static final String SHIFT_FLAG_STOP = "0";
    
    /** 班次开产标志：1-开 */
    private static final String SHIFT_FLAG_START = "1";

    /**
     * 班次编码枚举（按顺序）
     */
    public enum ShiftOrder {
        ONE(1, "一班"),
        TWO(2, "二班"),
        THREE(3, "三班");
        
        private final int order;
        private final String name;
        
        ShiftOrder(int order, String name) {
            this.order = order;
            this.name = name;
        }
        
        public int getOrder() {
            return order;
        }
        
        public String getName() {
            return name;
        }
        
        /**
         * 根据序号获取班次枚举
         */
        public static ShiftOrder fromOrder(int order) {
            for (ShiftOrder shift : values()) {
                if (shift.order == order) {
                    return shift;
                }
            }
            return ONE;
        }
    }

    /**
     * 获取指定班次的开停产标志
     *
     * @param date       日期
     * @param shiftOrder 班次序号（1,2,3）
     * @return 开产标志（0-停, 1-开），默认返回"1"（开产）
     */
    private String getShiftFlag(LocalDate date, int shiftOrder) {
        Date queryDate = Date.valueOf(date);
        MdmWorkCalendar workCalendar = workCalendarMapper.selectOne(
                new LambdaQueryWrapper<MdmWorkCalendar>()
                        .eq(MdmWorkCalendar::getProcCode, PROC_CODE_CX)
                        .eq(MdmWorkCalendar::getProductionDate, queryDate));

        if (workCalendar == null) {
            log.warn("未找到工作日历配置，日期: {}，班次: {}，默认视为开产", date, shiftOrder);
            return SHIFT_FLAG_START;
        }

        switch (shiftOrder) {
            case 1:
                return workCalendar.getOneShiftFlag();
            case 2:
                return workCalendar.getTwoShiftFlag();
            case 3:
                return workCalendar.getThreeShiftFlag();
            default:
                log.warn("未知的班次序号: {}，默认视为开产", shiftOrder);
                return SHIFT_FLAG_START;
        }
    }

    /**
     * 获取上一个班次的开停产标志
     * 
     * 班次顺序：一班 → 二班 → 三班 → 下一天一班
     * 一班的"上一个班次" = 前一天的三班
     *
     * @param date       日期
     * @param shiftOrder 班次序号（1,2,3）
     * @return 上一个班次的开产标志（0-停, 1-开），默认返回"1"（开产）
     */
    private String getPreviousShiftFlag(LocalDate date, int shiftOrder) {
        LocalDate prevDate = date;
        int prevShiftOrder;
        
        switch (shiftOrder) {
            case 1:
                // 一班的"上一个班次" = 前一天的三班
                prevDate = date.minusDays(1);
                prevShiftOrder = 3;
                break;
            case 2:
                // 二班的"上一个班次" = 当天的一班
                prevShiftOrder = 1;
                break;
            case 3:
                // 三班的"上一个班次" = 当天的二班
                prevShiftOrder = 2;
                break;
            default:
                log.warn("未知的班次序号: {}", shiftOrder);
                return SHIFT_FLAG_START;
        }
        
        String flag = getShiftFlag(prevDate, prevShiftOrder);
        log.debug("获取上一个班次标志，日期: {}，班次: {} -> 日期: {}，班次: {}，标志: {}",
                date, shiftOrder, prevDate, prevShiftOrder, flag);
        return flag;
    }

    /**
     * 获取下一个班次的开停产标志
     * 
     * 班次顺序：一班 → 二班 → 三班 → 下一天一班
     * 三班的"下一个班次" = 下一天的一班
     *
     * @param date       日期
     * @param shiftOrder 班次序号（1,2,3）
     * @return 下一个班次的开产标志（0-停, 1-开），默认返回"1"（开产）
     */
    private String getNextShiftFlag(LocalDate date, int shiftOrder) {
        LocalDate nextDate = date;
        int nextShiftOrder;
        
        switch (shiftOrder) {
            case 1:
                // 一班的"下一个班次" = 当天的二班
                nextShiftOrder = 2;
                break;
            case 2:
                // 二班的"下一个班次" = 当天的三班
                nextShiftOrder = 3;
                break;
            case 3:
                // 三班的"下一个班次" = 下一天的一班
                nextDate = date.plusDays(1);
                nextShiftOrder = 1;
                break;
            default:
                log.warn("未知的班次序号: {}", shiftOrder);
                return SHIFT_FLAG_START;
        }
        
        String flag = getShiftFlag(nextDate, nextShiftOrder);
        log.debug("获取下一个班次标志，日期: {}，班次: {} -> 日期: {}，班次: {}，标志: {}",
                date, shiftOrder, nextDate, nextShiftOrder, flag);
        return flag;
    }

    /**
     * 按班次级别判断班次类型
     * 
     * 判断逻辑：
     * - 停产班：本班次 = 0(停产)，不做处理
     * - 开产班（首个）：本班次 = 1(开产) 且 上个班次 = 0(停产)，走开产逻辑
     * - 停产前一天班（末个）：本班次 = 1(开产) 且 下个班次 = 0(停产)，走停产前一天逻辑
     *
     * @param date       日期
     * @param shiftOrder 班次序号（1,2,3）
     * @return 班次类型
     */
    public ShiftType determineShiftType(LocalDate date, int shiftOrder) {
        String currentFlag = getShiftFlag(date, shiftOrder);
        
        // 1. 判断是否为停产班
        if (SHIFT_FLAG_STOP.equals(currentFlag)) {
            log.info("班次类型判定：日期={}，班次={}，结果=停产班", 
                    date, ShiftOrder.fromOrder(shiftOrder).getName());
            return ShiftType.CLOSED;
        }
        
        // 2. 本班次是开产，判断是开产首个班还是停产前一天班
        String prevFlag = getPreviousShiftFlag(date, shiftOrder);
        String nextFlag = getNextShiftFlag(date, shiftOrder);
        
        // 上个班次是停产 -> 开产首个班次
        if (SHIFT_FLAG_STOP.equals(prevFlag)) {
            log.info("班次类型判定：日期={}，班次={}，上个班次=停产，结果=开产首个班次", 
                    date, ShiftOrder.fromOrder(shiftOrder).getName());
            return ShiftType.OPEN_START;
        }
        
        // 下个班次是停产 -> 停产前一天班次
        if (SHIFT_FLAG_STOP.equals(nextFlag)) {
            log.info("班次类型判定：日期={}，班次={}，下个班次=停产，结果=停产前一天班次", 
                    date, ShiftOrder.fromOrder(shiftOrder).getName());
            return ShiftType.BEFORE_CLOSE;
        }
        
        // 正常班
        log.info("班次类型判定：日期={}，班次={}，结果=正常班", 
                date, ShiftOrder.fromOrder(shiftOrder).getName());
        return ShiftType.NORMAL;
    }

    // ==================== 以下为废弃方法（按天判断，已改用按班次判断） ====================

    @Override
    @Deprecated
    public boolean isHoliday(LocalDate date) {
        // 废弃：三个班次全部停产视为停产日（已改用 determineShiftType 按班次级别判断）
        return false;
    }

    @Override
    @Deprecated
    public boolean isStopProductionDay(LocalDate date) {
        // 废弃：按天判断停产日（已改用 determineShiftType 按班次级别判断）
        return false;
    }

    @Override
    @Deprecated
    public boolean isStartProductionDay(LocalDate date) {
        // 废弃：按天判断开产日（已改用 determineShiftType 按班次级别判断）
        return false;
    }

    @Override
    @Deprecated
    public boolean isBeforeHoliday(LocalDate date) {
        // 废弃：按天判断停产前一天（已改用 determineShiftType 按班次级别判断）
        return false;
    }

    @Override
    public HolidayInfo getHolidayInfo(LocalDate date) {
        HolidayInfo.HolidayInfoBuilder builder = HolidayInfo.builder();
        builder.isHoliday(isHoliday(date));
        builder.isBeforeHoliday(isBeforeHoliday(date));

        // 获取班次信息
        Date queryDate = Date.valueOf(date);
        MdmWorkCalendar workCalendar = workCalendarMapper.selectOne(
                new LambdaQueryWrapper<MdmWorkCalendar>()
                        .eq(MdmWorkCalendar::getProcCode, PROC_CODE_CX)
                        .eq(MdmWorkCalendar::getProductionDate, queryDate));

        if (workCalendar != null && SHIFT_FLAG_STOP.equals(workCalendar.getDayFlag())) {
            builder.holidayName("停产日")
                    .startDate(date)
                    .endDate(date)
                    .totalDays(1);
        } else if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            builder.holidayName("周日")
                    .startDate(date)
                    .endDate(date)
                    .totalDays(1);
        }

        builder.isStopProductionDay(isStopProductionDay(date))
                .isStartProductionDay(isStartProductionDay(date));

        return builder.build();
    }

    @Override
    public HolidayScheduleResult handleBeforeHoliday(ScheduleContextVo context) {
        HolidayScheduleResult result = new HolidayScheduleResult();
        result.setAdjusted(false);
        result.setAdjustments(new ArrayList<>());

        LocalDate scheduleDate = context.getScheduleDate();
        Integer shiftOrder = context.getShiftOrder();

        // 班次级别判断：是否为停产前一天班次
        ShiftType shiftType = determineShiftType(scheduleDate, shiftOrder != null ? shiftOrder : 1);
        if (shiftType != ShiftType.BEFORE_CLOSE) {
            result.setMessage("非停产前一天班次，无需特殊处理");
            return result;
        }

        log.info("处理停产前一天班次排程调整: {} {}", scheduleDate, ShiftOrder.fromOrder(shiftOrder).getName());

        // 获取节假日信息（停产日信息）
        HolidayInfo holidayInfo = getHolidayInfo(scheduleDate.plusDays(1));
        int holidayDays = holidayInfo.getTotalDays() > 0 ? holidayInfo.getTotalDays() : 1;

        // 1. 计算硫化最低需求
        Map<String, Integer> minDemand = calculateMinDemandForHoliday(
                holidayInfo.getStartDate() != null ? holidayInfo.getStartDate() : scheduleDate.plusDays(1),
                holidayDays);

        // 2. 获取当前库存
        Map<String, Integer> currentStock = new HashMap<>();
        for (CxStock stock : context.getStocks()) {
            currentStock.put(stock.getEmbryoCode(), stock.getEffectiveStock());
        }

        // 3. 计算过剩库存
        Map<String, Integer> excessStock = calculateExcessStock(minDemand, currentStock);
        result.setExcessStockToConsume(excessStock);

        // 4. 确定停机时间
        // 从硫化排程结果中获取硫化停机时间
        LocalDateTime vulcanizingStopTime = determineVulcanizingStopTimeFromSchedule(scheduleDate);
        Integer reservedDigestHours = getReservedDigestHours();
        LocalDateTime formingStopTime = determineFormingStopTime(vulcanizingStopTime, reservedDigestHours);
        result.setFormingStopTime(formingStopTime);

        // 5. 计算成型可排产时长
        // 从班次配置表获取当前班次的开始时间
        LocalTime shiftStartTime = getShiftStartTime(shiftOrder != null ? shiftOrder : 1);
        LocalDateTime shiftStartDateTime = LocalDateTime.of(scheduleDate, shiftStartTime);
        Integer formingAvailableHours = calculateFormingAvailableHours(shiftStartDateTime, formingStopTime);
        result.setFormingAvailableHours(formingAvailableHours);

        // 6. 检查胎胚停放时间约束
        List<EmbryoConsumptionSuggestion> parkingViolations = checkEmbryoParkingTime(scheduleDate, formingStopTime);

        // 7. 生成调整建议
        for (Map.Entry<String, Integer> entry : excessStock.entrySet()) {
            if (entry.getValue() > 0) {
                result.getAdjustments().add(String.format("物料 %s 过剩库存 %d 条，需停产前消耗",
                        entry.getKey(), entry.getValue()));
            }
        }

        for (EmbryoConsumptionSuggestion suggestion : parkingViolations) {
            result.getAdjustments().add(String.format("胎胚 %s 停放时间 %.2f 小时，需强制消耗",
                    suggestion.getEmbryoCode(), suggestion.getParkingHours()));
        }

        // 设置到上下文
        context.setIsBeforeClosingDay(true);
        context.setHolidayDays(holidayDays);
        context.setVulcanizingStopTime(vulcanizingStopTime);
        context.setFormingStopTime(formingStopTime);
        context.setReservedDigestHours(reservedDigestHours);
        context.setFormingAvailableHours(formingAvailableHours);
        context.setExcessStockToConsume(excessStock);

        result.setAdjusted(!CollectionUtils.isEmpty(result.getAdjustments()));
        result.setMessage(result.isAdjusted() ? "已完成停产前一天班次调整" : "无需调整");

        return result;
    }

    @Override
    public HolidayScheduleResult handleOpeningDay(ScheduleContextVo context) {
        HolidayScheduleResult result = new HolidayScheduleResult();
        result.setAdjusted(false);
        result.setAdjustments(new ArrayList<>());

        LocalDate scheduleDate = context.getScheduleDate();
        Integer shiftOrder = context.getShiftOrder();

        // 班次级别判断：是否为开产首个班次
        ShiftType shiftType = determineShiftType(scheduleDate, shiftOrder != null ? shiftOrder : 1);
        if (shiftType != ShiftType.OPEN_START) {
            result.setMessage("非开产首个班次，无需特殊处理");
            return result;
        }

        log.info("处理开产首个班次排程调整: {} {}", scheduleDate, ShiftOrder.fromOrder(shiftOrder).getName());

        // 1. 确定成型开产班次和硫化开模班次
        // 假设班次顺序：夜班(0-8) -> 早班(8-16) -> 中班(16-24)
        // 成型开产班次 = 早班（第一个班次）
        // 硫化开模班次 = 中班（第二个班次）
        String formingStartShift = "SHIFT_DAY";
        String vulcanizingStartShift = "SHIFT_AFTERNOON";

        result.setFormingStartShift(formingStartShift);
        result.setVulcanizingStartShift(vulcanizingStartShift);

        // 2. 加载关键产品配置
        Set<String> keyProductCodes = context.getKeyProductCodes();
        if (keyProductCodes == null) {
            keyProductCodes = loadKeyProductCodes();
            context.setKeyProductCodes(keyProductCodes);
        }

        // 3. 首班不排关键产品
        if (!CollectionUtils.isEmpty(keyProductCodes)) {
            result.getAdjustments().add(String.format("开产首班(%s)不排关键产品，共 %d 个",
                    formingStartShift, keyProductCodes.size()));
        }

        // 设置到上下文
        context.setFormingStartShift(formingStartShift);
        context.setVulcanizingStartShift(vulcanizingStartShift);

        result.setAdjusted(true);
        result.setMessage("已完成开产首个班次调整：成型早于硫化1个班开产，首班不排关键产品");

        return result;
    }

    /**
     * 从硫化排程结果中获取硫化停机时间
     */
    private LocalDateTime determineVulcanizingStopTimeFromSchedule(LocalDate scheduleDate) {
        List<LhScheduleResult> lhResults = lhScheduleResultMapper.selectByDate(scheduleDate);

        if (CollectionUtils.isEmpty(lhResults)) {
            log.warn("未找到硫化排程结果，使用默认停机时间22:00");
            return LocalDateTime.of(scheduleDate, LocalTime.of(22, 0));
        }

        LocalDateTime maxStopTime = null;

        // 按胎胚分组
        Map<String, List<LhScheduleResult>> embryoGroupMap = new HashMap<>();
        for (LhScheduleResult result : lhResults) {
            String embryoCode = result.getEmbryoCode();
            if (embryoCode != null) {
                embryoGroupMap.computeIfAbsent(embryoCode, k -> new ArrayList<>()).add(result);
            }
        }

        // 遍历每个胎胚组，找出该胎胚对应的所有硫化任务中最大的specEndTime
        for (Map.Entry<String, List<LhScheduleResult>> entry : embryoGroupMap.entrySet()) {
            List<LhScheduleResult> embryoResults = entry.getValue();

            LocalDateTime embryoMaxStopTime = null;
            for (LhScheduleResult result : embryoResults) {
                LocalDateTime specEndTime = result.getSpecEndTime().toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime();

                if (specEndTime != null) {
                    if (embryoMaxStopTime == null || specEndTime.isAfter(embryoMaxStopTime)) {
                        embryoMaxStopTime = specEndTime;
                    }
                }
            }

            if (embryoMaxStopTime != null) {
                if (maxStopTime == null || embryoMaxStopTime.isAfter(maxStopTime)) {
                    maxStopTime = embryoMaxStopTime;
                    log.debug("胎胚 {} 的最大硫化停机时间: {}", entry.getKey(), embryoMaxStopTime);
                }
            }
        }

        if (maxStopTime == null) {
            log.warn("未能从硫化排程结果获取停机时间，使用默认停机时间22:00");
            return LocalDateTime.of(scheduleDate, LocalTime.of(22, 0));
        }

        log.info("从硫化排程结果获取硫化停机时间: {}，涉及胎胚种类: {}", maxStopTime, embryoGroupMap.size());
        return maxStopTime;
    }

    /**
     * 获取指定班次的开始时间
     *
     * @param shiftOrder 班次序号（1,2,3）
     * @return 班次开始时间
     */
    private LocalTime getShiftStartTime(int shiftOrder) {
        CxShiftConfig shiftConfig = shiftConfigMapper.selectOne(
                new LambdaQueryWrapper<CxShiftConfig>()
                        .eq(CxShiftConfig::getIsActive, 1)
                        .eq(CxShiftConfig::getShiftOrder, shiftOrder)
                        .last("LIMIT 1"));

        if (shiftConfig != null && shiftConfig.getStartTime() != null) {
            String startTimeStr = shiftConfig.getStartTime();
            log.info("从班次配置获取班次{}开始时间: {}", shiftOrder, startTimeStr);
            return LocalTime.parse(startTimeStr);
        }

        log.warn("未找到班次{}的配置，使用默认开始时间", shiftOrder);
        // 根据班次序号返回默认时间
        switch (shiftOrder) {
            case 1: return LocalTime.of(0, 0);   // 一班 00:00
            case 2: return LocalTime.of(8, 0);   // 二班 08:00
            case 3: return LocalTime.of(16, 0);  // 三班 16:00
            default: return LocalTime.of(8, 0);
        }
    }

    @Override
    public Map<String, Integer> calculateMinDemandForHoliday(LocalDate holidayStartDate, int holidayDays) {
        Map<String, Integer> minDemand = new HashMap<>();

        BigDecimal lossRate = getLossRate();

        for (int i = 0; i < holidayDays; i++) {
            LocalDate planDate = holidayStartDate.plusDays(i);

            List<LhScheduleResult> lhResults = lhScheduleResultMapper.selectByDate(planDate);

            if (lhResults != null && !lhResults.isEmpty()) {
                for (LhScheduleResult result : lhResults) {
                    String embryoCode = result.getEmbryoCode();
                    Integer dailyPlanQty = result.getDailyPlanQty();

                    if (embryoCode != null && dailyPlanQty != null && dailyPlanQty > 0) {
                        minDemand.merge(embryoCode, dailyPlanQty, Integer::sum);
                    }
                }
            }
        }

        // 应用损耗率
        for (Map.Entry<String, Integer> entry : minDemand.entrySet()) {
            int demand = entry.getValue();
            int demandWithLoss = (int) Math.ceil(demand * (1 + lossRate.doubleValue()));
            entry.setValue(demandWithLoss);
        }

        log.info("计算节假日最低需求完成，共 {} 种胎胚", minDemand.size());

        return minDemand;
    }

    @Override
    public Map<String, Integer> calculateExcessStock(Map<String, Integer> minDemand, Map<String, Integer> currentStock) {
        Map<String, Integer> excessStock = new HashMap<>();

        for (Map.Entry<String, Integer> entry : minDemand.entrySet()) {
            String materialCode = entry.getKey();
            int demand = entry.getValue();
            int stock = currentStock.getOrDefault(materialCode, 0);

            int excess = stock - demand;
            excessStock.put(materialCode, Math.max(excess, 0));
        }

        return excessStock;
    }

    @Override
    public LocalDateTime determineFormingStopTime(LocalDateTime vulcanizingStopTime, Integer reservedDigestHours) {
        if (reservedDigestHours == null) {
            reservedDigestHours = DEFAULT_RESERVED_DIGEST_HOURS;
        }
        return vulcanizingStopTime.minusHours(reservedDigestHours);
    }

    @Override
    public Integer calculateFormingAvailableHours(LocalDateTime shiftStartTime, LocalDateTime formingStopTime) {
        if (shiftStartTime == null || formingStopTime == null) {
            return 8; // 默认8小时
        }

        long hours = ChronoUnit.HOURS.between(shiftStartTime, formingStopTime);
        return Math.max((int) hours, 0);
    }

    @Override
    public List<EmbryoConsumptionSuggestion> checkEmbryoParkingTime(LocalDate scheduleDate, LocalDateTime formingStopTime) {
        List<EmbryoConsumptionSuggestion> suggestions = new ArrayList<>();

        int maxParkingHours = getMaxParkingHours();
        int reservedDigestHours = getReservedDigestHours();

        List<CxStock> stocks = stockMapper.selectList(
                new LambdaQueryWrapper<CxStock>().gt(CxStock::getStockNum, 0));

        for (CxStock stock : stocks) {
            BigDecimal parkingHours = stock.getStockHours();

            if (parkingHours != null) {
                BigDecimal predictedParkingHours = parkingHours.add(BigDecimal.valueOf(reservedDigestHours));

                if (predictedParkingHours.compareTo(BigDecimal.valueOf(maxParkingHours)) > 0) {
                    EmbryoConsumptionSuggestion suggestion = new EmbryoConsumptionSuggestion();
                    suggestion.setEmbryoCode(stock.getEmbryoCode());
                    suggestion.setEmbryoName(stock.getMaterialName());
                    suggestion.setCurrentStock(stock.getStockNum());
                    suggestion.setParkingHours(parkingHours);
                    suggestion.setSuggestedConsumption(stock.getEffectiveStock());
                    suggestion.setReason(String.format("停放时间 %.2f 小时，加预留时间后将超过 %d 小时限制",
                            parkingHours, maxParkingHours));
                    suggestions.add(suggestion);
                }
            }
        }

        return suggestions;
    }

    /**
     * 按班次级别调整排程
     * 
     * 核心判断逻辑（按班次）：
     * - 停产班（本班次=0）：不排程，返回空
     * - 开产首个班（本班次=1 且 上班次=0）：走开产逻辑
     * - 停产前一天班（本班次=1 且 下班次=0）：走停产前一天逻辑
     *
     * @param scheduleDate  排程日期
     * @param shiftOrder    班次序号（1,2,3）
     * @param originalResult 原始排程结果
     * @param context       排程上下文
     * @return 调整后的排程结果
     */
    @Override
    public List<CxScheduleResult> adjustHolidaySchedule(LocalDate scheduleDate, int shiftOrder, 
            List<CxScheduleResult> originalResult, ScheduleContextVo context) {
        
        if (CollectionUtils.isEmpty(originalResult)) {
            return originalResult;
        }
        
        // 设置上下文中的班次信息
        context.setScheduleDate(scheduleDate);
        context.setShiftOrder(shiftOrder);

        // 按班次级别判断类型
        ShiftType shiftType = determineShiftType(scheduleDate, shiftOrder);
        
        log.info("班次级别排程调整，日期: {}，班次: {}，类型: {}",
                scheduleDate, ShiftOrder.fromOrder(shiftOrder).getName(), shiftType.getDesc());

        switch (shiftType) {
            case CLOSED:
                // 停产班，不排程
                log.info("停产班次 {} {} 不排程", scheduleDate, ShiftOrder.fromOrder(shiftOrder).getName());
                return new ArrayList<>();
                
            case OPEN_START:
                // 开产首个班次，走开产逻辑
                HolidayScheduleResult openResult = handleOpeningDay(context);
                if (openResult.isAdjusted()) {
                    log.info("开产首个班次 {} {} 已调整: {}", 
                            scheduleDate, ShiftOrder.fromOrder(shiftOrder).getName(), 
                            openResult.getAdjustments());
                    
                    // 首班不排关键产品
                    String firstShift = context.getFormingStartShift();
                    Set<String> keyProductCodes = context.getKeyProductCodes();
                    
                    if (keyProductCodes != null && !keyProductCodes.isEmpty()) {
                        for (CxScheduleResult result : originalResult) {
                            result.setRemark("开产首个班次(" + firstShift + ") - 不排关键产品");
                        }
                    }
                }
                break;
                
            case BEFORE_CLOSE:
                // 停产前一天班次，走停产前一天逻辑
                HolidayScheduleResult closeResult = handleBeforeHoliday(context);
                if (closeResult.isAdjusted()) {
                    log.info("停产前一天班次 {} {} 已调整: {}", 
                            scheduleDate, ShiftOrder.fromOrder(shiftOrder).getName(),
                            closeResult.getAdjustments());
                }
                break;
                
            case NORMAL:
                // 正常班，不做特殊处理
                log.info("正常班次 {} {} 无需特殊处理", 
                        scheduleDate, ShiftOrder.fromOrder(shiftOrder).getName());
                break;
        }

        return originalResult;
    }

    /**
     * 兼容旧接口：按天调整排程（仅用于兼容保留）
     */
    @Override
    @Deprecated
    public List<CxScheduleResult> adjustHolidaySchedule(LocalDate scheduleDate, 
            List<CxScheduleResult> originalResult, ScheduleContextVo context) {
        // 默认处理当天的第一个班次
        return adjustHolidaySchedule(scheduleDate, 1, originalResult, context);
    }

    /**
     * 加载关键产品编码集合
     */
    private Set<String> loadKeyProductCodes() {
        List<CxKeyProduct> keyProducts = keyProductMapper.selectList(
                new LambdaQueryWrapper<CxKeyProduct>().eq(CxKeyProduct::getIsActive, 1));

        Set<String> codes = new HashSet<>();
        for (CxKeyProduct product : keyProducts) {
            codes.add(product.getEmbryoCode());
        }
        return codes;
    }

    /**
     * 获取预留消化时间（小时）
     */
    private Integer getReservedDigestHours() {
        CxParamConfig config = paramConfigMapper.selectOne(
                new LambdaQueryWrapper<CxParamConfig>()
                        .eq(CxParamConfig::getParamCode, "RESERVED_DIGEST_HOURS")
                        .eq(CxParamConfig::getIsActive, 1));

        if (config != null && config.getParamValue() != null) {
            try {
                return Integer.parseInt(config.getParamValue());
            } catch (NumberFormatException e) {
                log.warn("预留消化时间配置值无效: {}", config.getParamValue());
            }
        }
        return DEFAULT_RESERVED_DIGEST_HOURS;
    }

    /**
     * 获取胎胚最长停放时间（小时）
     */
    private int getMaxParkingHours() {
        CxParamConfig config = paramConfigMapper.selectOne(
                new LambdaQueryWrapper<CxParamConfig>()
                        .eq(CxParamConfig::getParamCode, "MAX_PARKING_HOURS")
                        .eq(CxParamConfig::getIsActive, 1));

        if (config != null && config.getParamValue() != null) {
            try {
                return Integer.parseInt(config.getParamValue());
            } catch (NumberFormatException e) {
                log.warn("胎胚最长停放时间配置值无效: {}", config.getParamValue());
            }
        }
        return DEFAULT_MAX_PARKING_HOURS;
    }

    /**
     * 获取损耗率
     */
    private BigDecimal getLossRate() {
        CxParamConfig config = paramConfigMapper.selectOne(
                new LambdaQueryWrapper<CxParamConfig>()
                        .eq(CxParamConfig::getParamCode, "LOSS_RATE")
                        .eq(CxParamConfig::getIsActive, 1));

        if (config != null && config.getParamValue() != null) {
            try {
                return new BigDecimal(config.getParamValue());
            } catch (NumberFormatException e) {
                log.warn("损耗率配置值无效: {}", config.getParamValue());
            }
        }
        return new BigDecimal("0.02"); // 默认2%
    }
}
