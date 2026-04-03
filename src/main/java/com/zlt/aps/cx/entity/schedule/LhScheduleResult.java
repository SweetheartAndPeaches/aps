package com.zlt.aps.cx.entity.schedule;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel(value = "硫化排程结果对象", description = "硫化排程结果表")
public class LhScheduleResult extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "批次号")
    @TableField("BATCH_NO")
    private String batchNo;

    @ApiModelProperty(value = "工单号")
    @TableField("ORDER_NO")
    private String orderNo;

    @ApiModelProperty(value = "硫化机台编号")
    @TableField("LH_MACHINE_CODE")
    private String lhMachineCode;

    @ApiModelProperty(value = "左右模")
    @TableField("LEFT_RIGHT_MOULD")
    private String leftRightMould;

    @ApiModelProperty(value = "硫化机台名称")
    @TableField("LH_MACHINE_NAME")
    private String lhMachineName;

    @ApiModelProperty(value = "物料编号")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    @ApiModelProperty(value = "规格代码")
    @TableField("SPEC_CODE")
    private String specCode;

    @ApiModelProperty(value = "胎胚代码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    @ApiModelProperty(value = "产品结构")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    @ApiModelProperty(value = "物料描述")
    @TableField("MATERIAL_DESC")
    private String materialDesc;

    @ApiModelProperty(value = "主物料")
    @TableField("MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    @ApiModelProperty(value = "胎胚库存")
    @TableField("EMBRYO_STOCK")
    private Integer embryoStock;

    @ApiModelProperty(value = "规格描述")
    @TableField("SPEC_DESC")
    private String specDesc;

    @ApiModelProperty(value = "硫化时长(秒)")
    @TableField("LH_TIME")
    private Integer lhTime;

    @ApiModelProperty(value = "日计划数量")
    @TableField("DAILY_PLAN_QTY")
    private Integer dailyPlanQty;

    @ApiModelProperty(value = "排程日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("SCHEDULE_DATE")
    private LocalDate scheduleDate;

    @ApiModelProperty(value = "规格结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("SPEC_END_TIME")
    private LocalDateTime specEndTime;

    @ApiModelProperty(value = "生产状态")
    @TableField("PRODUCTION_STATUS")
    private String productionStatus;

    // ========== 班次信息 ==========
    @ApiModelProperty(value = "1班计划量")
    @TableField("CLASS1_PLAN_QTY")
    private Integer class1PlanQty;

    @ApiModelProperty(value = "1班开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS1_START_TIME")
    private LocalDateTime class1StartTime;

    @ApiModelProperty(value = "1班结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS1_END_TIME")
    private LocalDateTime class1EndTime;

    @ApiModelProperty(value = "1班原因分析")
    @TableField("CLASS1_ANALYSIS")
    private String class1Analysis;

    @ApiModelProperty(value = "1班完成量")
    @TableField("CLASS1_FINISH_QTY")
    private Integer class1FinishQty;

    @ApiModelProperty(value = "2班计划量")
    @TableField("CLASS2_PLAN_QTY")
    private Integer class2PlanQty;

    @ApiModelProperty(value = "2班开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS2_START_TIME")
    private LocalDateTime class2StartTime;

    @ApiModelProperty(value = "2班结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS2_END_TIME")
    private LocalDateTime class2EndTime;

    @ApiModelProperty(value = "2班原因分析")
    @TableField("CLASS2_ANALYSIS")
    private String class2Analysis;

    @ApiModelProperty(value = "2班完成量")
    @TableField("CLASS2_FINISH_QTY")
    private Integer class2FinishQty;

    @ApiModelProperty(value = "3班计划量")
    @TableField("CLASS3_PLAN_QTY")
    private Integer class3PlanQty;

    @ApiModelProperty(value = "3班开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS3_START_TIME")
    private LocalDateTime class3StartTime;

    @ApiModelProperty(value = "3班结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS3_END_TIME")
    private LocalDateTime class3EndTime;

    @ApiModelProperty(value = "3班原因分析")
    @TableField("CLASS3_ANALYSIS")
    private String class3Analysis;

    @ApiModelProperty(value = "3班完成量")
    @TableField("CLASS3_FINISH_QTY")
    private Integer class3FinishQty;

    @ApiModelProperty(value = "4班计划量")
    @TableField("CLASS4_PLAN_QTY")
    private Integer class4PlanQty;

    @ApiModelProperty(value = "4班开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS4_START_TIME")
    private LocalDateTime class4StartTime;

    @ApiModelProperty(value = "4班结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("CLASS4_END_TIME")
    private LocalDateTime class4EndTime;

    @ApiModelProperty(value = "4班原因分析")
    @TableField("CLASS4_ANALYSIS")
    private String class4Analysis;

    @ApiModelProperty(value = "4班完成量")
    @TableField("CLASS4_FINISH_QTY")
    private Integer class4FinishQty;

    // 其他班次省略...与上面类似结构

    @ApiModelProperty(value = "是否交期")
    @TableField("IS_DELIVERY")
    private String isDelivery;

    @ApiModelProperty(value = "是否发布")
    @TableField("IS_RELEASE")
    private String isRelease;

    @ApiModelProperty(value = "发布成功计数")
    @TableField("PUBLISH_SUCCESS_COUNT")
    private BigDecimal publishSuccessCount;

    @ApiModelProperty(value = "最新发布时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("NEWEST_PUBLISH_TIME")
    private LocalDateTime newestPublishTime;

    @ApiModelProperty(value = "数据来源")
    @TableField("DATA_SOURCE")
    private String dataSource;

    @ApiModelProperty(value = "使用模数")
    @TableField("MOULD_QTY")
    private Integer mouldQty;

    @ApiModelProperty(value = "单班硫化量")
    @TableField("SINGLE_MOULD_SHIFT_QTY")
    private Integer singleMouldShiftQty;

    @ApiModelProperty(value = "模具信息")
    @TableField("MOULD_INFO")
    private String mouldInfo;

    @ApiModelProperty(value = "硫化方式")
    @TableField("MOULD_METHOD")
    private String mouldMethod;

    @ApiModelProperty(value = "施工阶段")
    @TableField("CONSTRUCTION_STAGE")
    private String constructionStage;

    @ApiModelProperty(value = "制造示方书号")
    @TableField("EMBRYO_NO")
    private String embryoNo;

    @ApiModelProperty(value = "文字示方书号")
    @TableField("TEXT_NO")
    private String textNo;

    @ApiModelProperty(value = "硫化示方书号")
    @TableField("LH_NO")
    private String lhNo;

    @ApiModelProperty(value = "月计划版本")
    @TableField("MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    @ApiModelProperty(value = "机台排序号")
    @TableField("MACHINE_ORDER")
    private Integer machineOrder;

    @ApiModelProperty(value = "是否试制量试")
    @TableField("IS_TRIAL")
    private String isTrial;

    @ApiModelProperty(value = "实际排程日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("REAL_SCHEDULE_DATE")
    private LocalDate realScheduleDate;

    @ApiModelProperty(value = "是否首排")
    @TableField("IS_FIRST")
    private String isFirst;

    @ApiModelProperty(value = "硫化余量")
    @TableField("MOULD_SURPLUS_QTY")
    private Integer mouldSurplusQty;

    @ApiModelProperty(value = "是否收尾")
    @TableField("IS_END")
    private String isEnd;

    @ApiModelProperty(value = "排产版本")
    @TableField("PRODUCTION_VERSION")
    private String productionVersion;

    @ApiModelProperty(value = "模具号")
    @TableField("MOULD_CODE")
    private String mouldCode;

    @ApiModelProperty(value = "是否拆分")
    @TableField("IS_SPLIT")
    private String isSplit;

    @ApiModelProperty(value = "排程顺序")
    @TableField("SCHEDULE_ORDER")
    private String scheduleOrder;

    @ApiModelProperty(value = "排程类型")
    @TableField("SCHEDULE_TYPE")
    private String scheduleType;

    @ApiModelProperty(value = "是否换模")
    @TableField("IS_CHANGE_MOULD")
    private String isChangeMould;

    @ApiModelProperty(value = "总计划数量")
    @TableField("TOTAL_DAILY_PLAN_QTY")
    private Integer totalDailyPlanQty;
}
