package com.zlt.aps.cx.vo;

import com.zlt.aps.cx.entity.*;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.entity.config.CxKeyProduct;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.config.CxStructurePriority;
import com.zlt.aps.cx.api.domain.entity.CxPrecisionPlan;
import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import com.zlt.aps.mp.api.domain.entity.MdmDevicePlanShut;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 排程上下文VO
 * 用于承载排程过程中需要的所有数据
 *
 * @author APS Team
 */
@Data
public class ScheduleContextVo {

    /**
     * 排程日期
     */
    private LocalDate scheduleDate;

    /**
     * 班次序号（1=一班, 2=二班, 3=三班）
     * 用于按班次级别判断开产/停产
     */
    private Integer shiftOrder;

    /**
     * 工厂编号
     * 用于加载工厂特定的班次配置
     */
    private String factoryCode;

    /**
     * 当前排程天数（循环执行时使用）
     * 1-第一天 2-第二天 3-第三天
     */
    private Integer currentScheduleDay;

    /**
     * 当前排程日期（循环执行时使用）
     * scheduleDate + (currentScheduleDay - 1)
     */
    private LocalDate currentScheduleDate;

    /**
     * 当前天的班次配置列表（循环执行时使用）
     */
    private List<CxShiftConfig> currentShiftConfigs;

    /**
     * 所有班次配置列表（按排程天数分组）
     * 用于核心算法按天获取班次配置
     */
    private List<CxShiftConfig> shiftConfigList;

    public List<CxShiftConfig> getShiftConfigList() {
        return shiftConfigList;
    }

    public void setShiftConfigList(List<CxShiftConfig> shiftConfigList) {
        this.shiftConfigList = shiftConfigList;
    }

    /**
     * 参数配置映射（按参数编码索引）
     * 用于校验关键参数是否配置
     */
    private Map<String, CxParamConfig> paramConfigMap;

    /**
     * 排程模式：NORMAL-正常排程，RE_SCHEDULE-重排程，STRUCTURE_RE_SCHEDULE-结构重排
     */
    private String scheduleMode;

    /**
     * 可用成型机台列表
     */
    private List<MdmMoldingMachine> availableMachines;

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
    private List<CxMachineOnlineInfo> onlineInfos;

    /**
     * 机台在机胎胚映射（快速查询用）
     * Key: 成型机台编码
     * Value: 该机台正在做的胎胚编码集合
     */
    private Map<String, Set<String>> machineOnlineEmbryoMap;

    /**
     * 机台机型映射（快速查询用）
     * Key: 机台编码
     * Value: 机型编码（cxMachineTypeCode）
     */
    private Map<String, String> machineTypeCodeMap;

    /**
     * 固定机台配置列表
     */
    private List<MdmCxMachineFixed> machineFixedConfigs;

    /**
     * 设备计划停机列表
     * 用于排除排程日期范围内的停机机台
     */
    private List<MdmDevicePlanShut> devicePlanShuts;

    /**
     * 物料主数据列表
     */
    private List<MdmMaterialInfo> materials;

    /**
     * 库存列表
     */
    private List<CxStock> stocks;

    /**
     * 物料库存映射（按物料编码分配库存，共用胎胚按需求比例分配）
     * Key: 物料编码 (materialCode)
     * Value: 分配给该物料的库存数量
     */
    private Map<String, Integer> materialStockMap;

    /**
     * 结构硫化配比列表
     */
    private List<MdmStructureLhRatio> structureLhRatios;

    /**
     * 结构整车配置列表
     * 用于获取每个结构的整车胎面条数(TREAD_COUNT)
     */
    private List<CxStructureTreadConfig> structureTreadConfigs;

    /**
     * 结构整车配置映射（快速查询用）
     * Key: 结构编码 (structureCode)
     * Value: 整车胎面条数 (treadCount)
     */
    private Map<String, Integer> structureTreadCountMap;    /**
     * 结构班产配置列表（整车条数）
     * 按结构+班次定义的标准产能
     */
    private List<CxStructureTreadConfig> structureShiftCapacities;

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
     * 预警配置
     */
    private Map<String, CxAlertConfig> alertConfigMap;

    /**
     * 精度计划列表
     */
    private List<CxPrecisionPlan> precisionPlans;

    /**
     * 材料异常列表
     */
    private List<CxMaterialException> materialExceptions;

    /**
     * 胎面停放配置列表
     */
    private List<CxTreadParkingConfig> treadParkingConfigs;

    /**
     * 物料收尾管理列表（物料维度）
     * 从月计划计算生成，用于跟踪每个物料的收尾进度
     */
    private List<CxMaterialEnding> materialEndings;

    /**
     * 物料日硫化产能映射（物料编码 -> 产能信息）
     * 用于计算成型机台的满算力
     * Key: 物料编码
     * Value: 日硫化产能信息
     */
    private Map<String, MonthPlanProductLhCapacityVo> materialLhCapacityMap;

    /**
     * 结构硫化配比映射（结构编码 -> 配比信息）
     * 用于获取机台的最大配比
     * Key: 结构名称
     * Value: 硫化配比信息
     */
    private Map<String, MdmStructureLhRatio> structureLhRatioMap;

    /**
     * 成型余量映射（物料编码 -> 成型余量）
     * 成型余量 = 硫化余量 - 该物料对应的所有胎胚库存
     * 用于收尾计算
     */
    private Map<String, Integer> formingRemainderMap;

    /**
     * 胎胚到物料映射（胎胚编码 -> 物料编码）
     * 用于将胎胚库存转换为物料维度
     */
    private Map<String, String> embryoToMaterialMap;

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
     * 机台种类上限
     * 默认4种
     */
    private Integer maxTypesPerMachine;

    /**
     * 机台默认最大硫化机数
     * 当机台配比配置缺失时的兜底值，默认10
     */
    private Integer maxLhMachineQty;

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

    /**
     * 硫化机停锅时间（停产日硫化停止时刻，HH:mm格式字符串，如 "08:00"）
     */
    private String vulcanizingStopTimeStr;

    /**
     * 硫化机停锅时间（完整日期时间，格式 yyyy-MM-dd HH:mm，如 "2026-05-06 08:00"）
     * 优先使用此字段，若为空则回退到 vulcanizingStopTimeStr
     */
    private LocalDateTime vulcanizingStopDateTime;

    /**
     * 硫化开模时间（开产日硫化开始时刻，HH:mm格式字符串，如 "08:00"）
     */
    private String vulcanizingOpenTimeStr;

    /**
     * 硫化开模时间（完整日期时间，格式 yyyy-MM-dd HH:mm，如 "2026-05-07 08:00"）
     * 优先使用此字段，若为空则回退到 vulcanizingOpenTimeStr
     */
    private LocalDateTime vulcanizingOpenDateTime;

    /**
     * H15开头机台最大胎胚种类数（未配置则按配比默认值，配置后覆盖配比值）
     */
    private Integer h15MaxEmbryoTypes;

    /**
     * 库存可供硫化时长预警阈值（小时），默认18小时
     * 当胎胚预计库存可供硫化时长超过此值时进行预警
     */
    private Integer stockHoursWarningThreshold;

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

    // ==================== 结构排产配置 ====================

    /**
     * 结构排产配置列表
     * 从 T_MP_STRUCTURE_ALLOCATION 获取，定义了每个结构可分配的机台
     */
    private List<MpCxCapacityConfiguration> structureAllocations;

    /**
     * 结构排产配置映射（结构编码 -> 可分配机台列表）
     * 快速查询用
     */
    private Map<String, List<MpCxCapacityConfiguration>> structureAllocationMap;

    /**
     * 月计划排产版本
     * 从硫化排程结果中提取，用于过滤结构排产配置
     */
    private String productionVersion;

    /**
     * 是否强制保留历史任务
     * 从 CxParamConfig 获取，控制续作任务是否优先保留在原机台
     */
    private Boolean forceKeepHistoryTask;

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

    // ==================== 辅助方法 ====================

    /**
     * 获取结构胎面整车条数映射
     *
     * @return Map<structureName, treadCount>
     */
    public Map<String, Integer> getStructureTreadCountMap() {
        Map<String, Integer> map = new HashMap<>();
        if (structureShiftCapacities != null) {
            for (CxStructureTreadConfig config : structureShiftCapacities) {
                if (config.getStructureCode() != null && config.getTreadCount() != null) {
                    map.put(config.getStructureCode(), config.getTreadCount());
                }
            }
        }
        return map;
    }
}
