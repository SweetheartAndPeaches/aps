package com.jinyu.aps.entity.schedule;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.jinyu.aps.entity.base.ApsBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 硫化排程结果表
 * 对应表：T_LH_SCHEDULE_RESULT
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_LH_SCHEDULE_RESULT")
@Schema(description = "硫化排程结果对象")
public class LhScheduleResult extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 每次生成1个批次号
     */
    @Schema(description = "批次号")
    @TableField("BATCH_NO")
    private String batchNo;

    /**
     * 唯一工单号
     */
    @Schema(description = "工单号")
    @TableField("ORDER_NO")
    private String orderNo;

    /**
     * 硫化机台编号
     */
    @Schema(description = "硫化机台编号")
    @TableField("LH_MACHINE_CODE")
    private String lhMachineCode;

    /**
     * 左右模 L/R/LR
     */
    @Schema(description = "左右模")
    @TableField("LEFT_RIGHT_MOULD")
    private String leftRightMould;

    /**
     * 硫化机台名称
     */
    @Schema(description = "硫化机台名称")
    @TableField("LH_MACHINE_NAME")
    private String lhMachineName;

    /**
     * 物料编号
     */
    @Schema(description = "物料编号")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    /**
     * 规格代码
     */
    @Schema(description = "规格代码")
    @TableField("SPEC_CODE")
    private String specCode;

    /**
     * 胎胚代码
     */
    @Schema(description = "胎胚代码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    /**
     * 产品结构
     */
    @Schema(description = "产品结构")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    /**
     * 物料描述
     */
    @Schema(description = "物料描述")
    @TableField("MATERIAL_DESC")
    private String materialDesc;

    /**
     * 主物料(胎胚描述)
     */
    @Schema(description = "主物料")
    @TableField("MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    /**
     * 胎胚库存
     */
    @Schema(description = "胎胚库存")
    @TableField("EMBRYO_STOCK")
    private Integer embryoStock;

    /**
     * 规格描述信息
     */
    @Schema(description = "规格描述")
    @TableField("SPEC_DESC")
    private String specDesc;

    /**
     * 硫化时长 单位：秒
     */
    @Schema(description = "硫化时长(秒)")
    @TableField("LH_TIME")
    private Integer lhTime;

    /**
     * 日计划数量
     */
    @Schema(description = "日计划数量")
    @TableField("DAILY_PLAN_QTY")
    private Integer dailyPlanQty;

    /**
     * 排程日期
     */
    @Schema(description = "排程日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("SCHEDULE_DATE")
    private LocalDate scheduleDate;

    /**
     * 规格结束时间
     */
    @Schema(description = "规格结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("SPEC_END_TIME")
    private LocalDateTime specEndTime;

    /**
     * 生产状态:0-未生产；1-生产中；2-生产完成
     */
    @Schema(description = "生产状态")
    @TableField("PRODUCTION_STATUS")
    private String productionStatus;

    // ========== 班次信息（8班次） ==========

    @Schema(description = "1班计划量")
    @TableField("CLASS1_PLAN_QTY")
    private Integer class1PlanQty;

    @Schema(description = "1班开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS1_START_TIME")
    private LocalDateTime class1StartTime;

    @Schema(description = "1班结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS1_END_TIME")
    private LocalDateTime class1EndTime;

    @Schema(description = "1班原因分析")
    @TableField("CLASS1_ANALYSIS")
    private String class1Analysis;

    @Schema(description = "1班完成量")
    @TableField("CLASS1_FINISH_QTY")
    private Integer class1FinishQty;

    @Schema(description = "2班计划量")
    @TableField("CLASS2_PLAN_QTY")
    private Integer class2PlanQty;

    @Schema(description = "2班开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS2_START_TIME")
    private LocalDateTime class2StartTime;

    @Schema(description = "2班结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS2_END_TIME")
    private LocalDateTime class2EndTime;

    @Schema(description = "2班原因分析")
    @TableField("CLASS2_ANALYSIS")
    private String class2Analysis;

    @Schema(description = "2班完成量")
    @TableField("CLASS2_FINISH_QTY")
    private Integer class2FinishQty;

    @Schema(description = "3班计划量")
    @TableField("CLASS3_PLAN_QTY")
    private Integer class3PlanQty;

    @Schema(description = "3班开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS3_START_TIME")
    private LocalDateTime class3StartTime;

    @Schema(description = "3班结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS3_END_TIME")
    private LocalDateTime class3EndTime;

    @Schema(description = "3班原因分析")
    @TableField("CLASS3_ANALYSIS")
    private String class3Analysis;

    @Schema(description = "3班完成量")
    @TableField("CLASS3_FINISH_QTY")
    private Integer class3FinishQty;

    @Schema(description = "4班计划量")
    @TableField("CLASS4_PLAN_QTY")
    private Integer class4PlanQty;

    @Schema(description = "4班开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS4_START_TIME")
    private LocalDateTime class4StartTime;

    @Schema(description = "4班结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS4_END_TIME")
    private LocalDateTime class4EndTime;

    @Schema(description = "4班原因分析")
    @TableField("CLASS4_ANALYSIS")
    private String class4Analysis;

    @Schema(description = "4班完成量")
    @TableField("CLASS4_FINISH_QTY")
    private Integer class4FinishQty;

    @Schema(description = "5班计划量")
    @TableField("CLASS5_PLAN_QTY")
    private Integer class5PlanQty;

    @Schema(description = "5班开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS5_START_TIME")
    private LocalDateTime class5StartTime;

    @Schema(description = "5班结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS5_END_TIME")
    private LocalDateTime class5EndTime;

    @Schema(description = "5班原因分析")
    @TableField("CLASS5_ANALYSIS")
    private String class5Analysis;

    @Schema(description = "5班完成量")
    @TableField("CLASS5_FINISH_QTY")
    private Integer class5FinishQty;

    @Schema(description = "6班计划量")
    @TableField("CLASS6_PLAN_QTY")
    private Integer class6PlanQty;

    @Schema(description = "6班开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS6_START_TIME")
    private LocalDateTime class6StartTime;

    @Schema(description = "6班结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS6_END_TIME")
    private LocalDateTime class6EndTime;

    @Schema(description = "6班原因分析")
    @TableField("CLASS6_ANALYSIS")
    private String class6Analysis;

    @Schema(description = "6班完成量")
    @TableField("CLASS6_FINISH_QTY")
    private Integer class6FinishQty;

    @Schema(description = "7班计划量")
    @TableField("CLASS7_PLAN_QTY")
    private Integer class7PlanQty;

    @Schema(description = "7班开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS7_START_TIME")
    private LocalDateTime class7StartTime;

    @Schema(description = "7班结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS7_END_TIME")
    private LocalDateTime class7EndTime;

    @Schema(description = "7班原因分析")
    @TableField("CLASS7_ANALYSIS")
    private String class7Analysis;

    @Schema(description = "7班完成量")
    @TableField("CLASS7_FINISH_QTY")
    private Integer class7FinishQty;

    @Schema(description = "8班计划量")
    @TableField("CLASS8_PLAN_QTY")
    private Integer class8PlanQty;

    @Schema(description = "8班开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS8_START_TIME")
    private LocalDateTime class8StartTime;

    @Schema(description = "8班结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS8_END_TIME")
    private LocalDateTime class8EndTime;

    @Schema(description = "8班原因分析")
    @TableField("CLASS8_ANALYSIS")
    private String class8Analysis;

    @Schema(description = "8班完成量")
    @TableField("CLASS8_FINISH_QTY")
    private Integer class8FinishQty;

    // ========== 其他字段 ==========

    @Schema(description = "是否交期：0-否 1-是")
    @TableField("IS_DELIVERY")
    private String isDelivery;

    @Schema(description = "是否发布")
    @TableField("IS_RELEASE")
    private String isRelease;

    @Schema(description = "发布成功计数")
    @TableField("PUBLISH_SUCCESS_COUNT")
    private BigDecimal publishSuccessCount;

    @Schema(description = "最新发布时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("NEWEST_PUBLISH_TIME")
    private LocalDateTime newestPublishTime;

    @Schema(description = "数据来源：0-自动排程；1-插单；2-导入")
    @TableField("DATA_SOURCE")
    private String dataSource;

    @Schema(description = "使用模数")
    @TableField("MOULD_QTY")
    private Integer mouldQty;

    @Schema(description = "单班硫化量")
    @TableField("SINGLE_MOULD_SHIFT_QTY")
    private Integer singleMouldShiftQty;

    @Schema(description = "模具信息 JSON字符串")
    @TableField("MOULD_INFO")
    private String mouldInfo;

    @Schema(description = "硫化方式")
    @TableField("MOULD_METHOD")
    private String mouldMethod;

    @Schema(description = "施工阶段")
    @TableField("CONSTRUCTION_STAGE")
    private String constructionStage;

    @Schema(description = "制造示方书号")
    @TableField("EMBRYO_NO")
    private String embryoNo;

    @Schema(description = "文字示方书号")
    @TableField("TEXT_NO")
    private String textNo;

    @Schema(description = "硫化示方书号")
    @TableField("LH_NO")
    private String lhNo;

    @Schema(description = "月计划版本")
    @TableField("MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    @Schema(description = "机台排序号")
    @TableField("MACHINE_ORDER")
    private Integer machineOrder;

    @Schema(description = "是否试制量试")
    @TableField("IS_TRIAL")
    private String isTrial;

    @Schema(description = "实际排程日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("REAL_SCHEDULE_DATE")
    private LocalDate realScheduleDate;

    @Schema(description = "T日规格结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("TDAY_SPEC_END_TIME")
    private LocalDateTime tdaySpecEndTime;

    @Schema(description = "是否首排")
    @TableField("IS_FIRST")
    private String isFirst;

    @Schema(description = "硫化余量")
    @TableField("MOULD_SURPLUS_QTY")
    private Integer mouldSurplusQty;

    @Schema(description = "是否收尾")
    @TableField("IS_END")
    private String isEnd;

    @Schema(description = "排产版本")
    @TableField("PRODUCTION_VERSION")
    private String productionVersion;

    @Schema(description = "模具号")
    @TableField("MOULD_CODE")
    private String mouldCode;

    @Schema(description = "是否拆分")
    @TableField("IS_SPLIT")
    private String isSplit;

    @Schema(description = "排程顺序")
    @TableField("SCHEDULE_ORDER")
    private String scheduleOrder;

    @Schema(description = "排程类型 01-续作 02-新增")
    @TableField("SCHEDULE_TYPE")
    private String scheduleType;

    @Schema(description = "是否换模")
    @TableField("IS_CHANGE_MOULD")
    private String isChangeMould;

    @Schema(description = "总计划数量")
    @TableField("TOTAL_DAILY_PLAN_QTY")
    private Integer totalDailyPlanQty;
}
