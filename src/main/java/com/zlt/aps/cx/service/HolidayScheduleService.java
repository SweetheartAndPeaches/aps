package com.zlt.aps.cx.service;

import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 节假日处理服务接口
 * 
 * 实现节假日相关的排程逻辑：
 * - 停产前处理（消耗过剩库存、确定停机时间）
 * - 开产后处理（成型早硫化1班开产、首班不排关键产品）
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
     * 处理停产前一天排程
     * 
     * 核心逻辑：
     * 1. 计算硫化最低需求 = 硫化计划需求 × (1 + 损耗率)
     * 2. 计算过剩库存 = 当前库存 - 最低需求
     * 3. 确定成型停机时间 = 硫化停机时间 - 预留消化时间
     * 4. 计算成型可排产时长和可排产量
     * 5. 检查胎胚停放时间约束
     *
     * @param context 排程上下文
     * @return 处理结果
     */
    HolidayScheduleResult handleBeforeHoliday(ScheduleContextVo context);

    /**
     * 处理开产日排程
     * 
     * 核心逻辑：
     * 1. 成型比硫化早1个班开始生产
     * 2. 成型开产班次（第一个班次）不排关键产品
     * 3. 关键产品从配置表判断
     *
     * @param context 排程上下文
     * @return 处理结果
     */
    HolidayScheduleResult handleOpeningDay(ScheduleContextVo context);

    /**
     * 计算硫化最低需求
     * 最低需求 = 硫化计划需求 × (1 + 损耗率)
     *
     * @param holidayStartDate 节假日开始日期
     * @param holidayDays      节假日天数
     * @return 各物料的最低需求
     */
    Map<String, Integer> calculateMinDemandForHoliday(LocalDate holidayStartDate, int holidayDays);

    /**
     * 计算过剩库存
     * 过剩量 = 当前库存 - 最低需求
     *
     * @param minDemand 各物料最低需求
     * @param currentStock 各物料当前库存
     * @return 各物料的过剩库存量
     */
    Map<String, Integer> calculateExcessStock(Map<String, Integer> minDemand, Map<String, Integer> currentStock);

    /**
     * 确定成型停机时间
     * 成型停机时间 = 硫化停机时间 - 预留消化时间
     *
     * @param vulcanizingStopTime 硫化停机时间
     * @param reservedDigestHours 预留消化时间（小时）
     * @return 成型停机时间
     */
    LocalDateTime determineFormingStopTime(LocalDateTime vulcanizingStopTime, Integer reservedDigestHours);

    /**
     * 计算成型可排产时长
     *
     * @param shiftStartTime    班次开始时间
     * @param formingStopTime   成型停机时间
     * @return 可排产时长（小时）
     */
    Integer calculateFormingAvailableHours(LocalDateTime shiftStartTime, LocalDateTime formingStopTime);

    /**
     * 检查胎胚停放时间约束
     * 胎胚已停放时间 + 预留消化时间 > 胎胚最长停放时间 → 需强制消耗
     *
     * @param scheduleDate      排程日期
     * @param formingStopTime   成型停机时间
     * @return 需要强制消耗的胎胚列表
     */
    List<EmbryoConsumptionSuggestion> checkEmbryoParkingTime(LocalDate scheduleDate, LocalDateTime formingStopTime);

    /**
     * 调整节假日排程策略
     * 根据节假日类型调整排程策略
     *
     * @param scheduleDate 排程日期
     * @param originalResult 原始排程结果
     * @param context 排程上下文
     * @return 调整后的排程结果
     */
    List<CxScheduleResult> adjustHolidaySchedule(LocalDate scheduleDate, List<CxScheduleResult> originalResult, ScheduleContextVo context);

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
        private boolean isBeforeHoliday;
    }

    /**
     * 节假日排程结果
     */
    @lombok.Data
    class HolidayScheduleResult {
        private boolean adjusted;
        private String message;
        private List<String> adjustments;
        private Map<String, Integer> excessStockToConsume;
        private LocalDateTime formingStopTime;
        private Integer formingAvailableHours;
        private String formingStartShift;
        private String vulcanizingStartShift;
    }

    /**
     * 胎胚消耗建议
     */
    @lombok.Data
    class EmbryoConsumptionSuggestion {
        private String embryoCode;
        private String embryoName;
        private Integer currentStock;
        private BigDecimal parkingHours;
        private Integer suggestedConsumption;
        private String reason;
    }
}
