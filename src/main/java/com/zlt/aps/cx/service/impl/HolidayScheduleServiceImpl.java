package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.cx.entity.CxStock;
import com.zlt.aps.mp.api.domain.entity.MdmWorkCalendar;
import com.zlt.aps.cx.entity.config.CxKeyProduct;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.mapper.*;
import com.zlt.aps.cx.service.HolidayScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private static final String PROC_CODE_CX = "CX";

    @Override
    public boolean isHoliday(LocalDate date) {
        // 使用工作日历判断是否停产（按工序CX查询）
        // MdmWorkCalendar.dayFlag: 0-停,1-开
        Date queryDate = Date.valueOf(date);
        MdmWorkCalendar workCalendar = workCalendarMapper.selectOne(
                new LambdaQueryWrapper<MdmWorkCalendar>()
                        .eq(MdmWorkCalendar::getProcCode, PROC_CODE_CX)
                        .eq(MdmWorkCalendar::getProductionDate, queryDate));

        if (workCalendar != null) {
            // dayFlag = '0' 表示停产
            return "0".equals(workCalendar.getDayFlag());
        }

        // 如果工作日历中没有配置，检查是否为周日
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isStopProductionDay(LocalDate date) {
        if (!isHoliday(date)) {
            return false;
        }

        // 检查前一天是否为工作日
        LocalDate previousDay = date.minusDays(1);
        return !isHoliday(previousDay);
    }

    @Override
    public boolean isStartProductionDay(LocalDate date) {
        if (isHoliday(date)) {
            return false;
        }

        // 检查前一天是否为节假日
        LocalDate previousDay = date.minusDays(1);
        return isHoliday(previousDay);
    }

    @Override
    public boolean isBeforeHoliday(LocalDate date) {
        // 检查明天是否为停产日
        LocalDate nextDay = date.plusDays(1);
        return isStopProductionDay(nextDay);
    }

    @Override
    public HolidayInfo getHolidayInfo(LocalDate date) {
        HolidayInfo.HolidayInfoBuilder builder = HolidayInfo.builder();
        builder.isHoliday(isHoliday(date));
        builder.isBeforeHoliday(isBeforeHoliday(date));

        // 使用工作日历获取节假日信息（按工序CX查询）
        Date queryDate = Date.valueOf(date);
        MdmWorkCalendar workCalendar = workCalendarMapper.selectOne(
                new LambdaQueryWrapper<MdmWorkCalendar>()
                        .eq(MdmWorkCalendar::getProcCode, PROC_CODE_CX)
                        .eq(MdmWorkCalendar::getProductionDate, queryDate));

        if (workCalendar != null && "0".equals(workCalendar.getDayFlag())) {
            // 停产日
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

        if (!isBeforeHoliday(scheduleDate)) {
            result.setMessage("非停产前一天，无需特殊处理");
            return result;
        }

        log.info("处理停产前一天排程调整: {}", scheduleDate);

        // 获取节假日信息
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
        // 一个胎胚可能对应多个硫化任务（通过物料编码关联），取所有硫化任务中最大的specEndTime
        LocalDateTime vulcanizingStopTime = determineVulcanizingStopTimeFromSchedule(scheduleDate);
        Integer reservedDigestHours = getReservedDigestHours();
        LocalDateTime formingStopTime = determineFormingStopTime(vulcanizingStopTime, reservedDigestHours);
        result.setFormingStopTime(formingStopTime);

        // 5. 计算成型可排产时长
        // 从班次配置表获取第一个班次的开始时间
        LocalTime firstShiftStartTime = getFirstShiftStartTime();
        LocalDateTime shiftStartTime = LocalDateTime.of(scheduleDate, firstShiftStartTime);
        Integer formingAvailableHours = calculateFormingAvailableHours(shiftStartTime, formingStopTime);
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
        result.setMessage(result.isAdjusted() ? "已完成停产前一天调整" : "无需调整");

        return result;
    }

    @Override
    public HolidayScheduleResult handleOpeningDay(ScheduleContextVo context) {
        HolidayScheduleResult result = new HolidayScheduleResult();
        result.setAdjusted(false);
        result.setAdjustments(new ArrayList<>());

        LocalDate scheduleDate = context.getScheduleDate();

        if (!isStartProductionDay(scheduleDate)) {
            result.setMessage("非开产日，无需特殊处理");
            return result;
        }

        log.info("处理开产日排程调整: {}", scheduleDate);

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
        context.setIsOpeningDay(true);
        context.setFormingStartShift(formingStartShift);
        context.setVulcanizingStartShift(vulcanizingStartShift);

        result.setAdjusted(true);
        result.setMessage("已完成开产日调整：成型早于硫化1个班开产，首班不排关键产品");

        return result;
    }

    /**
     * 从硫化排程结果中获取硫化停机时间
     *
     * 逻辑说明：
     * 1. 一个胎胚(embryoCode)可能对应多个硫化任务（通过物料编码关联）
     * 2. 每个硫化任务有各自的specEndTime（规格结束时间）
     * 3. 按胎胚维度分组，找出每个胎胚对应的所有硫化任务中最大的specEndTime
     * 4. 取所有胎胚中最大的停机时间作为最终的硫化停机时间
     *
     * @param scheduleDate 排程日期
     * @return 硫化停机时间
     */
    private LocalDateTime determineVulcanizingStopTimeFromSchedule(LocalDate scheduleDate) {
        // 获取该日期的硫化排程结果
        List<LhScheduleResult> lhResults = lhScheduleResultMapper.selectByDate(scheduleDate);

        if (CollectionUtils.isEmpty(lhResults)) {
            log.warn("未找到硫化排程结果，使用默认停机时间22:00");
            return LocalDateTime.of(scheduleDate, LocalTime.of(22, 0));
        }

        // 按胎胚编码(embryoCode)分组，找出每个胎胚的最大停机时间
        // 然后取所有胎胚中最大的那个作为最终停机时间
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
            String embryoCode = entry.getKey();
            List<LhScheduleResult> embryoResults = entry.getValue();

            // 找出该胎胚对应的所有硫化任务中最大的specEndTime
            LocalDateTime embryoMaxStopTime = null;
            for (LhScheduleResult result : embryoResults) {
                LocalDateTime specEndTime = result.getSpecEndTime();
                if (specEndTime != null) {
                    if (embryoMaxStopTime == null || specEndTime.isAfter(embryoMaxStopTime)) {
                        embryoMaxStopTime = specEndTime;
                    }
                }
            }

            // 取所有胎胚中最大的停机时间
            if (embryoMaxStopTime != null) {
                if (maxStopTime == null || embryoMaxStopTime.isAfter(maxStopTime)) {
                    maxStopTime = embryoMaxStopTime;
                    log.debug("胎胚 {} 的最大硫化停机时间: {}", embryoCode, embryoMaxStopTime);
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
     * 从班次配置表获取第一个班次的开始时间
     *
     * 逻辑说明：
     * 1. 从 T_CX_SHIFT_CONFIG 表查询班次配置
     * 2. 按班次序号(shiftOrder)排序，取第一个启用的班次
     * 3. 解析其开始时间(startTime)，格式为 HH:mm:ss
     *
     * @return 第一个班次的开始时间
     */
    private LocalTime getFirstShiftStartTime() {
        // 查询启用的班次配置，按序号排序取第一个
        CxShiftConfig firstShift = shiftConfigMapper.selectOne(
                new LambdaQueryWrapper<CxShiftConfig>()
                        .eq(CxShiftConfig::getIsActive, 1)
                        .orderByAsc(CxShiftConfig::getShiftOrder)
                        .last("LIMIT 1"));

        if (firstShift != null && firstShift.getStartTime() != null) {
            String startTimeStr = firstShift.getStartTime();
            log.info("从班次配置获取首个班次开始时间: {}，班次编码: {}", startTimeStr, firstShift.getShiftCode());
            // 解析 HH:mm:ss 格式的时间
            return LocalTime.parse(startTimeStr);
        }

        log.warn("未找到有效的班次配置，使用默认开始时间08:00");
        return LocalTime.of(8, 0);
    }

    @Override
    public Map<String, Integer> calculateMinDemandForHoliday(LocalDate holidayStartDate, int holidayDays) {
        Map<String, Integer> minDemand = new HashMap<>();

        // 获取损耗率配置
        BigDecimal lossRate = getLossRate();

        // 从硫化排程结果表获取节假日期间的硫化计划
        for (int i = 0; i < holidayDays; i++) {
            LocalDate planDate = holidayStartDate.plusDays(i);

            // 从硫化排程结果表获取该日期的计划
            List<LhScheduleResult> lhResults = lhScheduleResultMapper.selectByDate(planDate);
            
            if (lhResults != null && !lhResults.isEmpty()) {
                for (LhScheduleResult result : lhResults) {
                    String embryoCode = result.getEmbryoCode();
                    Integer dailyPlanQty = result.getDailyPlanQty();
                    
                    if (embryoCode != null && dailyPlanQty != null && dailyPlanQty > 0) {
                        // 累加该胎胚在节假日期间的需求
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

        // 获取胎胚停放时间配置
        int maxParkingHours = getMaxParkingHours();
        int reservedDigestHours = getReservedDigestHours();

        // 获取所有胎胚库存
        List<CxStock> stocks = stockMapper.selectList(
                new LambdaQueryWrapper<CxStock>().gt(CxStock::getStockNum, 0));

        for (CxStock stock : stocks) {
            // 获取胎胚已停放时间（从生产时间计算）
            // TODO: 需要从库存记录获取生产时间
            BigDecimal parkingHours = stock.getStockHours();

            if (parkingHours != null) {
                // 预测停放时间 = 已停放时间 + 预留消化时间
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

    @Override
    public List<CxScheduleResult> adjustHolidaySchedule(LocalDate scheduleDate, List<CxScheduleResult> originalResult, ScheduleContextVo context) {
        if (CollectionUtils.isEmpty(originalResult)) {
            return originalResult;
        }

        // 检查是否需要特殊处理
        if (isStopProductionDay(scheduleDate)) {
            log.info("停产日 {} 不排程", scheduleDate);
            return new ArrayList<>();
        }

        if (isBeforeHoliday(scheduleDate)) {
            HolidayScheduleResult adjustResult = handleBeforeHoliday(context);
            if (adjustResult.isAdjusted()) {
                log.info("停产前一天 {} 已调整排程: {}", scheduleDate, adjustResult.getAdjustments());
            }
        }

        if (isStartProductionDay(scheduleDate)) {
            HolidayScheduleResult adjustResult = handleOpeningDay(context);
            if (adjustResult.isAdjusted()) {
                log.info("开产日 {} 已调整排程: {}", scheduleDate, adjustResult.getAdjustments());

                // 首班不排关键产品
                String firstShift = context.getFormingStartShift();
                Set<String> keyProductCodes = context.getKeyProductCodes();

                if (keyProductCodes != null && !keyProductCodes.isEmpty()) {
                    for (CxScheduleResult result : originalResult) {
                        // 一班=夜班，二班=早班，三班=中班
                        // 成型开产班次默认为早班（二班）
                        if ("SHIFT_DAY".equals(firstShift) && 
                            result.getClass2PlanQty() != null && 
                            result.getClass2PlanQty().compareTo(BigDecimal.ZERO) > 0) {
                            // 标记早班需要排除关键产品
                            result.setRemark("开产首班(早班) - 不排关键产品");
                        }
                    }
                }
            }
        }

        return originalResult;
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
