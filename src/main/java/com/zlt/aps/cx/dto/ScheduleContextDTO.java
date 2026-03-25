package com.zlt.aps.cx.dto;

import com.zlt.aps.cx.entity.*;
import com.zlt.aps.cx.entity.config.CxKeyProduct;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.config.CxStructurePriority;
import com.zlt.aps.cx.entity.config.CxStructureShiftCapacity;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 排程上下文DTO
 * 用于承载排程过程中需要的所有数据
 *
 * @author APS Team
 */
@Data
public class ScheduleContextDTO {

    /**
     * 排程日期
     */
    private LocalDate scheduleDate;

    /**
     * 排程模式：NORMAL-正常排程，RE_SCHEDULE-重排程，STRUCTURE_RE_SCHEDULE-结构重排
     */
    private String scheduleMode;

    /**
     * 可用成型机台列表
     */
    private List<MdmMoldingMachine> availableMachines;

    /**
     * 机台结构产能配置列表
     * 用于获取机台-结构维度的小时产能、班次产能
     */
    private List<CxMachineStructureCapacity> machineStructureCapacities;

    /**
     * 机台结构产能映射（快速查询用）
     * Key: 机台编码_结构编码
     * Value: 产能配置
     */
    private Map<String, CxMachineStructureCapacity> machineCapacityMap;

    // ==================== 任务来源数据 ====================

    /**
     * 硫化排程结果列表（主要任务来源）
     * 从T_LH_SCHEDULE_RESULT获取今日硫化计划
     */
    private List<LhScheduleResult> lhScheduleResults;

    /**
     * 成型在机信息列表（续作判断）
     * 从T_MDM_CX_MACHINE_ONLINE_INFO获取当前机台正在做的胎胚
     */
    private List<MdmCxMachineOnlineInfo> onlineInfos;

    /**
     * 机台在机胎胚映射（快速查询用）
     * Key: 成型机台编码
     * Value: 该机台正在做的胎胚编码集合
     */
    private Map<String, Set<String>> machineOnlineEmbryoMap;

    /**
     * 固定机台配置列表
     */
    private List<MdmCxMachineFixed> machineFixedConfigs;

    /**
     * 物料主数据列表
     */
    private List<MdmMaterialInfo> materials;

    /**
     * 库存列表
     */
    private List<CxStock> stocks;

    /**
     * 结构硫化配比列表
     */
    private List<MdmStructureLhRatio> structureLhRatios;

    /**
     * 结构班产配置列表（整车条数）
     * 按结构+班次定义的标准产能
     */
    private List<CxStructureShiftCapacity> structureShiftCapacities;

    /**
     * 结构优先级配置列表
     */
    private List<CxStructurePriority> structurePriorities;

    /**
     * 关键产品配置列表
     * 用于开产首班排除等场景判断
     */
    private List<CxKeyProduct> keyProducts;

    /**
     * 关键产品编码集合（快速查询用）
     */
    private Set<String> keyProductCodes;

    /**
     * 排程参数配置
     */
    private Map<String, CxParamConfig> paramConfigMap;

    /**
     * 预警配置
     */
    private Map<String, CxAlertConfig> alertConfigMap;

    /**
     * 试制任务列表
     */
    private List<CxTrialTask> trialTasks;

    /**
     * 精度计划列表
     */
    private List<CxPrecisionPlan> precisionPlans;

    /**
     * 操作工请假列表
     */
    private List<CxOperatorLeave> operatorLeaves;

    /**
     * 材料异常列表
     */
    private List<CxMaterialException> materialExceptions;

    /**
     * 胎面停放配置列表
     */
    private List<CxTreadParkingConfig> treadParkingConfigs;

    /**
     * 结构收尾管理列表
     */
    private List<CxStructureEnding> structureEndings;

    /**
     * 工作日历
     */
    private MdmWorkCalendar workCalendar;

    /**
     * 昨日排程结果（用于续作判断）
     */
    private List<CxScheduleResult> yesterdayResults;

    /**
     * 班次配置
     */
    private Map<String, ShiftInfo> shiftConfigs;

    /**
     * 班次配置列表（从T_CX_SHIFT_CONFIG加载）
     * 用于排程时获取班次顺序、产能比例等信息
     */
    private List<CxShiftConfig> shiftConfigList;

    /**
     * 排产天数
     * 默认3天
     * 班次数量 = 天数 * 3 - 1（第一天的夜班跳过）
     * 例如：3天 → 早中 + 夜早中 + 夜早中 = 8个班次
     */
    private Integer scheduleDays;

    /**
     * 班次编码数组（按排产顺序排列）
     * 例如3天排产：[早班, 中班, 夜班, 早班, 中班, 夜班, 早班, 中班]
     * 即：早中、夜早中、夜早中
     */
    private String[] shiftCodes;

    /**
     * 每个班次对应的日期（与shiftCodes一一对应）
     * 用于确定每个班次属于哪一天
     */
    private LocalDate[] shiftDates;

    // ==================== 算法可配置参数 ====================

    /**
     * 波浪比例（班次分配比例）
     * 按夜班:早班:中班顺序配置
     * 默认 {1, 2, 1} 表示夜班:早班:中班 = 1:2:1
     * 实际使用时会根据班次顺序重新映射
     */
    private int[] waveRatio;

    /**
     * 机台种类上限
     * 默认4种
     */
    private Integer maxTypesPerMachine;

    /**
     * 默认整车容量（条）
     * 当结构班产配置中没有该结构时使用
     */
    private Integer defaultTripCapacity;

    /**
     * 损耗率
     */
    private java.math.BigDecimal lossRate;

    /**
     * 预留消化时间（小时）
     * 默认1小时
     */
    private Integer reservedDigestHours;

    /**
     * 胎胚最长停放时间（小时）
     * 默认24小时
     */
    private Integer maxParkingHours;

    /**
     * 机台小时产能（条/小时）
     */
    private Integer machineHourlyCapacity;

    /**
     * 是否开产日
     */
    private Boolean isOpeningDay;

    /**
     * 是否停产日
     */
    private Boolean isClosingDay;

    /**
     * 是否停产前一天
     */
    private Boolean isBeforeClosingDay;

    /**
     * 停产剩余天数
     */
    private Integer daysToClose;

    /**
     * 节假日天数
     */
    private Integer holidayDays;

    /**
     * 成型开产班次（开产日第一个班次）
     */
    private String formingStartShift;

    /**
     * 硫化开模班次（开产日第二个班次）
     */
    private String vulcanizingStartShift;

    /**
     * 硫化停机时间（停产前一天）
     */
    private LocalDateTime vulcanizingStopTime;

    /**
     * 成型停机时间（停产前一天，早于硫化停机时间）
     */
    private LocalDateTime formingStopTime;

    /**
     * 成型可排产时长（小时，停产前一天）
     */
    private Integer formingAvailableHours;

    /**
     * 过剩库存需要消耗的量
     */
    private Map<String, Integer> excessStockToConsume;

    // ==================== 收尾相关数据 ====================

    /**
     * 月度计划余量列表
     * 硫化余量(PLAN_SURPLUS_QTY)已由系统计算好（总计划量 - 硫化真实完成量）
     * 用于计算收尾余量：收尾余量 = 硫化余量 - 胎胚库存
     */
    private List<com.zlt.aps.mp.api.domain.entity.MdmMonthSurplus> monthSurplusList;

    /**
     * 月度计划余量映射（物料编码 -> 余量信息）
     * 快速查询用
     */
    private Map<String, com.zlt.aps.mp.api.domain.entity.MdmMonthSurplus> monthSurplusMap;

    /**
     * SKU排产分类列表
     * 用于判断是否为主销产品（SCHEDULE_TYPE='01'表示主销产品，月均销量>=500条）
     */
    private List<com.zlt.aps.mp.api.domain.entity.MdmSkuScheduleCategory> skuScheduleCategories;

    /**
     * 主销产品编码集合
     * 快速查询用
     */
    private Set<String> mainProductCodes;

    /**
     * 班次信息
     */
    @Data
    public static class ShiftInfo {
        private String shiftCode;
        private String shiftName;
        private Integer startHour;
        private Integer endHour;
        private Integer standardHours;
        private Boolean isActive;
    }
}
