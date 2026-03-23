package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cx.entity.CxMaterial;
import com.zlt.aps.cx.entity.CxStock;
import com.zlt.aps.cx.entity.config.CxHolidayConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.mapper.*;
import com.zlt.aps.cx.service.HolidayScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 节假日处理服务实现类
 *
 * @author APS Team
 */
@Slf4j
@Service
public class HolidayScheduleServiceImpl implements HolidayScheduleService {

    @Autowired
    private CxHolidayConfigMapper holidayConfigMapper;

    @Autowired
    private CxStockMapper stockMapper;

    @Autowired
    private CxMaterialMapper materialMapper;

    @Autowired
    private CxLhPlanMapper lhPlanMapper;

    @Override
    public boolean isHoliday(LocalDate date) {
        // 先检查配置的节假日
        CxHolidayConfig config = holidayConfigMapper.selectOne(
                new LambdaQueryWrapper<CxHolidayConfig>()
                        .eq(CxHolidayConfig::getHolidayDate, date)
                        .eq(CxHolidayConfig::getIsEnabled, 1));

        if (config != null) {
            return true;
        }

        // 检查是否为周末（如果配置了周末休息）
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isStopProductionDay(LocalDate date) {
        // 停产日 = 节假日首日
        if (!isHoliday(date)) {
            return false;
        }

        // 检查前一天是否为工作日
        LocalDate previousDay = date.minusDays(1);
        return !isHoliday(previousDay);
    }

    @Override
    public boolean isStartProductionDay(LocalDate date) {
        // 开产日 = 节假日后首个工作日
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

        // 获取节假日配置
        CxHolidayConfig config = holidayConfigMapper.selectOne(
                new LambdaQueryWrapper<CxHolidayConfig>()
                        .eq(CxHolidayConfig::getHolidayDate, date)
                        .eq(CxHolidayConfig::getIsEnabled, 1));

        if (config != null) {
            builder.holidayName(config.getHolidayName())
                    .startDate(config.getStartDate())
                    .endDate(config.getEndDate())
                    .totalDays((int) ChronoUnit.DAYS.between(config.getStartDate(), config.getEndDate()) + 1);
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
    public HolidayScheduleResult handleBeforeHoliday(LocalDate scheduleDate) {
        HolidayScheduleResult result = new HolidayScheduleResult();
        result.setAdjusted(false);
        result.setAdjustments(new ArrayList<>());

        if (!isBeforeHoliday(scheduleDate)) {
            result.setMessage("非停产前一天，无需特殊处理");
            return result;
        }

        log.info("处理停产前一天排程调整: {}", scheduleDate);

        // 1. 检查胎面库存，避免积压
        List<TreadConsumptionSuggestion> treadSuggestions = checkTreadStockBeforeHoliday(scheduleDate);
        for (TreadConsumptionSuggestion suggestion : treadSuggestions) {
            result.getAdjustments().add(String.format("胎面 %s 需消耗 %d 条，原因：%s",
                    suggestion.getTreadName(), suggestion.getSuggestedConsumption(), suggestion.getReason()));
        }

        // 2. 计算安全库存
        HolidayInfo holidayInfo = getHolidayInfo(scheduleDate.plusDays(1));
        List<SafetyStockSuggestion> stockSuggestions = calculateSafetyStockForHoliday(
                holidayInfo.getStartDate() != null ? holidayInfo.getStartDate() : scheduleDate.plusDays(1),
                holidayInfo.getTotalDays() > 0 ? holidayInfo.getTotalDays() : 1);

        for (SafetyStockSuggestion suggestion : stockSuggestions) {
            if (suggestion.getAdditionalNeeded() != null && suggestion.getAdditionalNeeded() > 0) {
                result.getAdjustments().add(String.format("物料 %s 需增加库存 %d 条，原因：%s",
                        suggestion.getMaterialName(), suggestion.getAdditionalNeeded(), suggestion.getReason()));
            }
        }

        result.setAdjusted(!CollectionUtils.isEmpty(result.getAdjustments()));
        result.setMessage(result.isAdjusted() ? "已完成停产前调整" : "无需调整");

        return result;
    }

    @Override
    public HolidayScheduleResult handleAfterHoliday(LocalDate scheduleDate) {
        HolidayScheduleResult result = new HolidayScheduleResult();
        result.setAdjusted(false);
        result.setAdjustments(new ArrayList<>());

        if (!isStartProductionDay(scheduleDate)) {
            result.setMessage("非开产日，无需特殊处理");
            return result;
        }

        log.info("处理开产后排程调整: {}", scheduleDate);

        // 1. 首班不排关键产品
        result.getAdjustments().add("开产首班不排关键产品");

        // 2. 检查库存水平
        List<CxStock> stocks = stockMapper.selectList(
                new LambdaQueryWrapper<CxStock>()
                        .gt(CxStock::getCurrentStock, 0));

        for (CxStock stock : stocks) {
            BigDecimal stockHours = stock.getStockHours() != null ? stock.getStockHours() : BigDecimal.ZERO;
            
            // 检查库存是否过低
            if (stockHours.compareTo(new BigDecimal("4")) < 0) {
                result.getAdjustments().add(String.format("物料 %s 库存过低，可供时长 %.2f 小时",
                        stock.getMaterialName(), stockHours));
            }
        }

        result.setAdjusted(true);
        result.setMessage("已完成开产后调整");

        return result;
    }

    @Override
    public List<CxScheduleResult> adjustHolidaySchedule(LocalDate scheduleDate, List<CxScheduleResult> originalResult) {
        if (CollectionUtils.isEmpty(originalResult)) {
            return originalResult;
        }

        // 检查是否需要特殊处理
        if (isStopProductionDay(scheduleDate)) {
            // 停产日：暂停排程
            log.info("停产日 {} 不排程", scheduleDate);
            return new ArrayList<>();
        }

        if (isBeforeHoliday(scheduleDate)) {
            // 停产前一天：调整策略
            HolidayScheduleResult adjustResult = handleBeforeHoliday(scheduleDate);
            if (adjustResult.isAdjusted()) {
                log.info("停产前一天 {} 已调整排程: {}", scheduleDate, adjustResult.getAdjustments());
                // TODO: 根据调整建议修改排程结果
            }
        }

        if (isStartProductionDay(scheduleDate)) {
            // 开产日：调整策略
            HolidayScheduleResult adjustResult = handleAfterHoliday(scheduleDate);
            if (adjustResult.isAdjusted()) {
                log.info("开产日 {} 已调整排程: {}", scheduleDate, adjustResult.getAdjustments());
                
                // 首班不排关键产品
                for (CxScheduleResult result : originalResult) {
                    if ("SHIFT_MORNING".equals(result.getShiftCode())) {
                        // 标记首班，后续在分配时会跳过关键产品
                        result.setRemark("开产首班 - 不排关键产品");
                    }
                }
            }
        }

        return originalResult;
    }

    @Override
    public List<SafetyStockSuggestion> calculateSafetyStockForHoliday(LocalDate holidayStartDate, int holidayDays) {
        List<SafetyStockSuggestion> suggestions = new ArrayList<>();

        // 获取所有物料及其当前库存
        List<CxStock> stocks = stockMapper.selectList(
                new LambdaQueryWrapper<CxStock>()
                        .gt(CxStock::getCurrentStock, 0));

        for (CxStock stock : stocks) {
            SafetyStockSuggestion suggestion = new SafetyStockSuggestion();
            suggestion.setMaterialCode(stock.getMaterialCode());
            suggestion.setMaterialName(stock.getMaterialName());
            suggestion.setCurrentStock(stock.getCurrentStock());

            // 计算节假日期间的硫化需求（简化处理）
            int dailyLhDemand = estimateDailyLhDemand(stock.getMaterialCode());
            int holidayDemand = dailyLhDemand * holidayDays;

            // 建议库存 = 节假日需求 + 安全缓冲（20%）
            int suggestedStock = (int) (holidayDemand * 1.2);
            suggestion.setSuggestedStock(suggestedStock);

            // 计算缺口
            int additionalNeeded = suggestedStock - stock.getCurrentStock();
            suggestion.setAdditionalNeeded(Math.max(additionalNeeded, 0));

            if (additionalNeeded > 0) {
                suggestion.setReason(String.format("节假日%d天，预计需求%d条", holidayDays, holidayDemand));
            } else {
                suggestion.setReason("库存充足");
            }

            suggestions.add(suggestion);
        }

        return suggestions;
    }

    @Override
    public List<TreadConsumptionSuggestion> checkTreadStockBeforeHoliday(LocalDate scheduleDate) {
        List<TreadConsumptionSuggestion> suggestions = new ArrayList<>();

        // 获取所有胎面库存（假设胎面编码以"-T"结尾）
        List<CxStock> treadStocks = stockMapper.selectList(
                new LambdaQueryWrapper<CxStock>()
                        .likeRight(CxStock::getMaterialCode, "TREAD") // 简化处理
                        .gt(CxStock::getCurrentStock, 0));

        for (CxStock tread : treadStocks) {
            TreadConsumptionSuggestion suggestion = new TreadConsumptionSuggestion();
            suggestion.setTreadCode(tread.getMaterialCode());
            suggestion.setTreadName(tread.getMaterialName());
            suggestion.setCurrentStock(tread.getCurrentStock());
            suggestion.setStockHours(tread.getStockHours());

            // 胎面停放时间限制（假设最大24小时）
            BigDecimal maxParkingHours = new BigDecimal("24");
            if (tread.getStockHours() != null && tread.getStockHours().compareTo(maxParkingHours) > 0) {
                // 停放时间过长，需要消耗
                int suggestedConsumption = tread.getCurrentStock();
                suggestion.setSuggestedConsumption(suggestedConsumption);
                suggestion.setReason(String.format("停放时间%.2f小时超过限制，建议全部消耗",
                        tread.getStockHours()));
                suggestions.add(suggestion);
            } else if (tread.getStockHours() != null && 
                       tread.getStockHours().compareTo(new BigDecimal("16")) > 0) {
                // 接近停放时间限制，部分消耗
                int suggestedConsumption = tread.getCurrentStock() / 2;
                suggestion.setSuggestedConsumption(suggestedConsumption);
                suggestion.setReason(String.format("停放时间%.2f小时接近限制，建议消耗一半",
                        tread.getStockHours()));
                suggestions.add(suggestion);
            }
        }

        return suggestions;
    }

    /**
     * 估算每日硫化需求
     */
    private int estimateDailyLhDemand(String materialCode) {
        // 简化处理：假设每个物料每天平均需求为100条
        // 实际应该根据历史数据或硫化计划计算
        return 100;
    }
}
