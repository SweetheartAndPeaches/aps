package com.zlt.aps.cx.service;

import com.zlt.aps.cx.entity.schedule.CxScheduleResult;

import java.time.LocalDate;
import java.util.List;

/**
 * 节假日处理服务接口
 * 
 * 实现节假日相关的排程逻辑：
 * - 停产前处理（拉低库存、避免胎面积压）
 * - 开产后处理（恢复生产、平衡库存）
 * - 节假日排程策略调整
 *
 * @author APS Team
 */
public interface HolidayScheduleService {

    /**
     * 检查是否为节假日
     *
     * @param date 日期
     * @return 是否为节假日
     */
    boolean isHoliday(LocalDate date);

    /**
     * 检查是否为停产日
     * 停产日 = 节假日首日
     *
     * @param date 日期
     * @return 是否为停产日
     */
    boolean isStopProductionDay(LocalDate date);

    /**
     * 检查是否为开产日
     * 开产日 = 节假日后首个工作日
     *
     * @param date 日期
     * @return 是否为开产日
     */
    boolean isStartProductionDay(LocalDate date);

    /**
     * 检查是否为停产前一天
     *
     * @param date 日期
     * @return 是否为停产前一天
     */
    boolean isBeforeHoliday(LocalDate date);

    /**
     * 获取节假日信息
     *
     * @param date 日期
     * @return 节假日信息（名称、天数等）
     */
    HolidayInfo getHolidayInfo(LocalDate date);

    /**
     * 处理停产前排程
     * 停产前一天的排程策略调整：
     * - 拉低胎胚库存
     * - 避免胎面积压
     * - 特殊结构处理
     *
     * @param scheduleDate 排程日期
     * @return 处理结果
     */
    HolidayScheduleResult handleBeforeHoliday(LocalDate scheduleDate);

    /**
     * 处理开产后排程
     * 开产当天的排程策略调整：
     * - 首班不排关键产品（除非该结构只有一个产品）
     * - 恢复正常库存水平
     *
     * @param scheduleDate 排程日期
     * @return 处理结果
     */
    HolidayScheduleResult handleAfterHoliday(LocalDate scheduleDate);

    /**
     * 调整节假日排程策略
     * 根据节假日类型调整排程策略
     *
     * @param scheduleDate 排程日期
     * @param originalResult 原始排程结果
     * @return 调整后的排程结果
     */
    List<CxScheduleResult> adjustHolidaySchedule(LocalDate scheduleDate, List<CxScheduleResult> originalResult);

    /**
     * 计算停产期间的安全库存
     * 确保停产期间硫化需求可以满足
     *
     * @param holidayStartDate 节假日开始日期
     * @param holidayDays      节假日天数
     * @return 各物料的安全库存建议
     */
    List<SafetyStockSuggestion> calculateSafetyStockForHoliday(LocalDate holidayStartDate, int holidayDays);

    /**
     * 检查胎面库存是否会影响停产
     * 胎面停放时间有限，需要提前消耗
     *
     * @param scheduleDate 排程日期
     * @return 需要消耗的胎面列表
     */
    List<TreadConsumptionSuggestion> checkTreadStockBeforeHoliday(LocalDate scheduleDate);

    /**
     * 节假日信息
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class HolidayInfo {
        private boolean isHoliday;
        private String holidayName;
        private LocalDate startDate;
        private LocalDate endDate;
        private int totalDays;
        private boolean isStopProductionDay;
        private boolean isStartProductionDay;
    }

    /**
     * 节假日排程结果
     */
    @lombok.Data
    class HolidayScheduleResult {
        private boolean adjusted;
        private String message;
        private List<String> adjustments;
    }

    /**
     * 安全库存建议
     */
    @lombok.Data
    class SafetyStockSuggestion {
        private String materialCode;
        private String materialName;
        private Integer currentStock;
        private Integer suggestedStock;
        private Integer additionalNeeded;
        private String reason;
    }

    /**
     * 胎面消耗建议
     */
    @lombok.Data
    class TreadConsumptionSuggestion {
        private String treadCode;
        private String treadName;
        private Integer currentStock;
        private BigDecimal stockHours;
        private Integer suggestedConsumption;
        private String reason;
    }
}
