package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 硫化排程结果表 t_lh_schedule_result
 */
@ApiModel(value = "硫化排程结果对象", description = "硫化排程结果表")
@Data
@TableName(value = "t_lh_schedule_result")
public class LhScheduleResult extends BaseEntity {

    /** 主键ID */
    @ApiModelProperty(value = "主键ID", name = "id")
    @TableField("ID")
    private Long id;

    /** 分厂编号 */
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    /** 每次生成1个批次号 */
    @ApiModelProperty(value = "批次号", name = "batchNo")
    @TableField("BATCH_NO")
    private String batchNo;

    /** 唯一工单号 */
    @ApiModelProperty(value = "工单号", name = "orderNo")
    @TableField("ORDER_NO")
    private String orderNo;

    /** 硫化机台编号 */
    @ApiModelProperty(value = "硫化机台编号", name = "lhMachineCode")
    @TableField("LH_MACHINE_CODE")
    private String lhMachineCode;

    /** 左右模 L/R/LR */
    @ApiModelProperty(value = "左右模", name = "leftRightMould")
    @TableField("LEFT_RIGHT_MOULD")
    private String leftRightMould;

    /** 硫化机台名称 */
    @ApiModelProperty(value = "硫化机台名称", name = "lhMachineName")
    @TableField("LH_MACHINE_NAME")
    private String lhMachineName;

    /** 物料编号 */
    @ApiModelProperty(value = "物料编号", name = "materialCode")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    /** 规格代码 */
    @ApiModelProperty(value = "规格代码", name = "specCode")
    @TableField("SPEC_CODE")
    private String specCode;

    /** 胎胚代码 */
    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    /** 产品结构 */
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    /** 物料描述 */
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField("MATERIAL_DESC")
    private String materialDesc;

    /** 主物料(胎胚描述) */
    @ApiModelProperty(value = "主物料", name = "mainMaterialDesc")
    @TableField("MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    /** 胎胚库存 */
    @ApiModelProperty(value = "胎胚库存", name = "embryoStock")
    @TableField("EMBRYO_STOCK")
    private Integer embryoStock;

    /** 规格描述信息 */
    @ApiModelProperty(value = "规格描述", name = "specDesc")
    @TableField("SPEC_DESC")
    private String specDesc;

    /** 硫化时长 单位：秒 */
    @ApiModelProperty(value = "硫化时长(秒)", name = "lhTime")
    @TableField("LH_TIME")
    private Integer lhTime;

    /** 日计划数量 */
    @ApiModelProperty(value = "日计划数量", name = "dailyPlanQty")
    @TableField("DAILY_PLAN_QTY")
    private Integer dailyPlanQty;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField("SCHEDULE_DATE")
    private LocalDate scheduleDate;

    /** 规格结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "规格结束时间", name = "specEndTime")
    @TableField("SPEC_END_TIME")
    private LocalDateTime specEndTime;

    /** 生产状态:0-未生产；1-生产中；2-生产完成 */
    @ApiModelProperty(value = "生产状态", name = "productionStatus")
    @TableField("PRODUCTION_STATUS")
    private String productionStatus;

    /** 1班计划量 */
    @ApiModelProperty(value = "1班计划量", name = "class1PlanQty")
    @TableField("CLASS1_PLAN_QTY")
    private Integer class1PlanQty;

    /** 1班计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "1班开始时间", name = "class1StartTime")
    @TableField("CLASS1_START_TIME")
    private LocalDateTime class1StartTime;

    /** 1班计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "1班结束时间", name = "class1EndTime")
    @TableField("CLASS1_END_TIME")
    private LocalDateTime class1EndTime;

    /** 1班原因分析 */
    @ApiModelProperty(value = "1班原因分析", name = "class1Analysis")
    @TableField("CLASS1_ANALYSIS")
    private String class1Analysis;

    /** 1班完成量 */
    @ApiModelProperty(value = "1班完成量", name = "class1FinishQty")
    @TableField("CLASS1_FINISH_QTY")
    private Integer class1FinishQty;

    /** 2班计划量 */
    @ApiModelProperty(value = "2班计划量", name = "class2PlanQty")
    @TableField("CLASS2_PLAN_QTY")
    private Integer class2PlanQty;

    /** 2班计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "2班开始时间", name = "class2StartTime")
    @TableField("CLASS2_START_TIME")
    private LocalDateTime class2StartTime;

    /** 2班计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "2班结束时间", name = "class2EndTime")
    @TableField("CLASS2_END_TIME")
    private LocalDateTime class2EndTime;

    /** 2班原因分析 */
    @ApiModelProperty(value = "2班原因分析", name = "class2Analysis")
    @TableField("CLASS2_ANALYSIS")
    private String class2Analysis;

    /** 2班完成量 */
    @ApiModelProperty(value = "2班完成量", name = "class2FinishQty")
    @TableField("CLASS2_FINISH_QTY")
    private Integer class2FinishQty;

    /** 3班计划量 */
    @ApiModelProperty(value = "3班计划量", name = "class3PlanQty")
    @TableField("CLASS3_PLAN_QTY")
    private Integer class3PlanQty;

    /** 3班计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "3班开始时间", name = "class3StartTime")
    @TableField("CLASS3_START_TIME")
    private LocalDateTime class3StartTime;

    /** 3班计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "3班结束时间", name = "class3EndTime")
    @TableField("CLASS3_END_TIME")
    private LocalDateTime class3EndTime;

    /** 3班原因分析 */
    @ApiModelProperty(value = "3班原因分析", name = "class3Analysis")
    @TableField("CLASS3_ANALYSIS")
    private String class3Analysis;

    /** 3班完成量 */
    @ApiModelProperty(value = "3班完成量", name = "class3FinishQty")
    @TableField("CLASS3_FINISH_QTY")
    private Integer class3FinishQty;

    /** 4班计划量 */
    @ApiModelProperty(value = "4班计划量", name = "class4PlanQty")
    @TableField("CLASS4_PLAN_QTY")
    private Integer class4PlanQty;

    /** 4班计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "4班开始时间", name = "class4StartTime")
    @TableField("CLASS4_START_TIME")
    private LocalDateTime class4StartTime;

    /** 4班计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "4班结束时间", name = "class4EndTime")
    @TableField("CLASS4_END_TIME")
    private LocalDateTime class4EndTime;

    /** 4班原因分析 */
    @ApiModelProperty(value = "4班原因分析", name = "class4Analysis")
    @TableField("CLASS4_ANALYSIS")
    private String class4Analysis;

    /** 4班完成量 */
    @ApiModelProperty(value = "4班完成量", name = "class4FinishQty")
    @TableField("CLASS4_FINISH_QTY")
    private Integer class4FinishQty;

    /** 5班计划量 */
    @ApiModelProperty(value = "5班计划量", name = "class5PlanQty")
    @TableField("CLASS5_PLAN_QTY")
    private Integer class5PlanQty;

    /** 5班计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "5班开始时间", name = "class5StartTime")
    @TableField("CLASS5_START_TIME")
    private LocalDateTime class5StartTime;

    /** 5班计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "5班结束时间", name = "class5EndTime")
    @TableField("CLASS5_END_TIME")
    private LocalDateTime class5EndTime;

    /** 5班原因分析 */
    @ApiModelProperty(value = "5班原因分析", name = "class5Analysis")
    @TableField("CLASS5_ANALYSIS")
    private String class5Analysis;

    /** 5班完成量 */
    @ApiModelProperty(value = "5班完成量", name = "class5FinishQty")
    @TableField("CLASS5_FINISH_QTY")
    private Integer class5FinishQty;

    /** 6班计划量 */
    @ApiModelProperty(value = "6班计划量", name = "class6PlanQty")
    @TableField("CLASS6_PLAN_QTY")
    private Integer class6PlanQty;

    /** 6班计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "6班开始时间", name = "class6StartTime")
    @TableField("CLASS6_START_TIME")
    private LocalDateTime class6StartTime;

    /** 6班计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "6班结束时间", name = "class6EndTime")
    @TableField("CLASS6_END_TIME")
    private LocalDateTime class6EndTime;

    /** 6班原因分析 */
    @ApiModelProperty(value = "6班原因分析", name = "class6Analysis")
    @TableField("CLASS6_ANALYSIS")
    private String class6Analysis;

    /** 6班完成量 */
    @ApiModelProperty(value = "6班完成量", name = "class6FinishQty")
    @TableField("CLASS6_FINISH_QTY")
    private Integer class6FinishQty;

    /** 7班计划量 */
    @ApiModelProperty(value = "7班计划量", name = "class7PlanQty")
    @TableField("CLASS7_PLAN_QTY")
    private Integer class7PlanQty;

    /** 7班计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "7班开始时间", name = "class7StartTime")
    @TableField("CLASS7_START_TIME")
    private LocalDateTime class7StartTime;

    /** 7班计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "7班结束时间", name = "class7EndTime")
    @TableField("CLASS7_END_TIME")
    private LocalDateTime class7EndTime;

    /** 7班原因分析 */
    @ApiModelProperty(value = "7班原因分析", name = "class7Analysis")
    @TableField("CLASS7_ANALYSIS")
    private String class7Analysis;

    /** 7班完成量 */
    @ApiModelProperty(value = "7班完成量", name = "class7FinishQty")
    @TableField("CLASS7_FINISH_QTY")
    private Integer class7FinishQty;

    /** 8班计划量 */
    @ApiModelProperty(value = "8班计划量", name = "class8PlanQty")
    @TableField("CLASS8_PLAN_QTY")
    private Integer class8PlanQty;

    /** 8班计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "8班开始时间", name = "class8StartTime")
    @TableField("CLASS8_START_TIME")
    private LocalDateTime class8StartTime;

    /** 8班计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "8班结束时间", name = "class8EndTime")
    @TableField("CLASS8_END_TIME")
    private LocalDateTime class8EndTime;

    /** 8班原因分析 */
    @ApiModelProperty(value = "8班原因分析", name = "class8Analysis")
    @TableField("CLASS8_ANALYSIS")
    private String class8Analysis;

    /** 8班完成量 */
    @ApiModelProperty(value = "8班完成量", name = "class8FinishQty")
    @TableField("CLASS8_FINISH_QTY")
    private Integer class8FinishQty;

    /** 是否交期，0--否，1--是 */
    @ApiModelProperty(value = "是否交期", name = "isDelivery")
    @TableField("IS_DELIVERY")
    private String isDelivery;

    /** 是否发布 */
    @ApiModelProperty(value = "是否发布", name = "isRelease")
    @TableField("IS_RELEASE")
    private String isRelease;

    /** 发布成功计数器 */
    @ApiModelProperty(value = "发布成功计数", name = "publishSuccessCount")
    @TableField("PUBLISH_SUCCESS_COUNT")
    private BigDecimal publishSuccessCount;

    /** 最新发布成功时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最新发布时间", name = "newestPublishTime")
    @TableField("NEWEST_PUBLISH_TIME")
    private LocalDateTime newestPublishTime;

    /** 数据来源：0 自动排程；1 插单；2 导入 */
    @ApiModelProperty(value = "数据来源", name = "dataSource")
    @TableField("DATA_SOURCE")
    private String dataSource;

    /** 使用模数 */
    @ApiModelProperty(value = "使用模数", name = "mouldQty")
    @TableField("MOULD_QTY")
    private Integer mouldQty;

    /** 单班硫化量 */
    @ApiModelProperty(value = "单班硫化量", name = "singleMouldShiftQty")
    @TableField("SINGLE_MOULD_SHIFT_QTY")
    private Integer singleMouldShiftQty;

    /** 模具信息 JSON字符串 */
    @ApiModelProperty(value = "模具信息", name = "mouldInfo")
    @TableField("MOULD_INFO")
    private String mouldInfo;

    /** 硫化方式 */
    @ApiModelProperty(value = "硫化方式", name = "mouldMethod")
    @TableField("MOULD_METHOD")
    private String mouldMethod;

    /** 施工阶段 */
    @ApiModelProperty(value = "施工阶段", name = "constructionStage")
    @TableField("CONSTRUCTION_STAGE")
    private String constructionStage;

    /** 制造示方书号 */
    @ApiModelProperty(value = "制造示方书号", name = "embryoNo")
    @TableField("EMBRYO_NO")
    private String embryoNo;

    /** 文字示方书号 */
    @ApiModelProperty(value = "文字示方书号", name = "textNo")
    @TableField("TEXT_NO")
    private String textNo;

    /** 硫化示方书号 */
    @ApiModelProperty(value = "硫化示方书号", name = "lhNo")
    @TableField("LH_NO")
    private String lhNo;

    /** 月计划需求版本 */
    @ApiModelProperty(value = "月计划版本", name = "monthPlanVersion")
    @TableField("MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /** 机台排序号 */
    @ApiModelProperty(value = "机台排序号", name = "machineOrder")
    @TableField("MACHINE_ORDER")
    private Integer machineOrder;

    /** 是否试制量试 */
    @ApiModelProperty(value = "是否试制量试", name = "isTrial")
    @TableField("IS_TRIAL")
    private String isTrial;

    /** 实际排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "实际排程日期", name = "realScheduleDate")
    @TableField("REAL_SCHEDULE_DATE")
    private LocalDate realScheduleDate;

    /** T日规格结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "T日规格结束时间", name = "tdaySpecEndTime")
    @TableField("TDAY_SPEC_END_TIME")
    private LocalDateTime tdaySpecEndTime;

    /** 是否首排 */
    @ApiModelProperty(value = "是否首排", name = "isFirst")
    @TableField("IS_FIRST")
    private String isFirst;

    /** 硫化余量 */
    @ApiModelProperty(value = "硫化余量", name = "mouldSurplusQty")
    @TableField("MOULD_SURPLUS_QTY")
    private Integer mouldSurplusQty;

    /** 是否收尾 */
    @ApiModelProperty(value = "是否收尾", name = "isEnd")
    @TableField("IS_END")
    private String isEnd;

    /** 月计划排产版本 */
    @ApiModelProperty(value = "排产版本", name = "productionVersion")
    @TableField("PRODUCTION_VERSION")
    private String productionVersion;

    /** 模具号 多个以逗号分隔 */
    @ApiModelProperty(value = "模具号", name = "mouldCode")
    @TableField("MOULD_CODE")
    private String mouldCode;

    /** 是否拆分 */
    @ApiModelProperty(value = "是否拆分", name = "isSplit")
    @TableField("IS_SPLIT")
    private String isSplit;

    /** 排程顺序 */
    @ApiModelProperty(value = "排程顺序", name = "scheduleOrder")
    @TableField("SCHEDULE_ORDER")
    private String scheduleOrder;

    /** 排程类型 01-续作 02-新增 */
    @ApiModelProperty(value = "排程类型", name = "scheduleType")
    @TableField("SCHEDULE_TYPE")
    private String scheduleType;

    /** 是否换模 */
    @ApiModelProperty(value = "是否换模", name = "isChangeMould")
    @TableField("IS_CHANGE_MOULD")
    private String isChangeMould;

    /** 总计划数量 */
    @ApiModelProperty(value = "总计划数量", name = "totalDailyPlanQty")
    @TableField("TOTAL_DAILY_PLAN_QTY")
    private Integer totalDailyPlanQty;

    /** 备注说明字段 */
    @ApiModelProperty(value = "备注", name = "remark")
    @TableField("REMARK")
    private String remark;
}
