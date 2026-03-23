package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成型排程结果表 T_CX_SCHEDULE_RESULT
 */
@ApiModel(value = "成型排程结果对象", description = "成型排程结果表")
@Data
@TableName(value = "T_CX_SCHEDULE_RESULT")
public class CxScheduleResult extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "主键ID", name = "id")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /** 自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号 */
    @ApiModelProperty(value = "成型批次号", name = "cxBatchNo")
    @TableField(value = "CX_BATCH_NO")
    private String cxBatchNo;

    /** 成型排程工单号，自动生成，批次号+4位定长自增序号 */
    @ApiModelProperty(value = "工单号", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /** 生产状态：0-未生产；1-生产中；2-已收尾 */
    @ApiModelProperty(value = "生产状态", name = "productionStatus")
    @TableField(value = "PRODUCTION_STATUS")
    private String productionStatus;

    /** 是否发布：0--未发布，1--已发布 */
    @ApiModelProperty(value = "是否发布", name = "isRelease")
    @TableField(value = "IS_RELEASE")
    private String isRelease;

    /** 分厂编号 */
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private LocalDateTime scheduleDate;

    /** 成型机台编号 */
    @ApiModelProperty(value = "成型机台编号", name = "cxMachineCode")
    @TableField(value = "CX_MACHINE_CODE")
    private String cxMachineCode;

    /** 成型机台名称 */
    @ApiModelProperty(value = "成型机台名称", name = "cxMachineName")
    @TableField(value = "CX_MACHINE_NAME")
    private String cxMachineName;

    /** 成型机台类型 */
    @ApiModelProperty(value = "成型机台类型", name = "cxMachineType")
    @TableField(value = "CX_MACHINE_TYPE")
    private String cxMachineType;

    /** 硫化排程任务序号，多个采用" / "分割 */
    @ApiModelProperty(value = "硫化排程任务序号", name = "lhScheduleIds")
    @TableField(value = "LH_SCHEDULE_IDS")
    private String lhScheduleIds;

    /** 硫化机台编号，多个采用" / "分割 */
    @ApiModelProperty(value = "硫化机台编号", name = "lhMachineCode")
    @TableField(value = "LH_MACHINE_CODE")
    private String lhMachineCode;

    /** 硫化机台名称，多个采用" / "分割 */
    @ApiModelProperty(value = "硫化机台名称", name = "lhMachineName")
    @TableField(value = "LH_MACHINE_NAME")
    private String lhMachineName;

    /** 硫化机使用总模数 */
    @ApiModelProperty(value = "硫化机使用总模数", name = "lhMachineQty")
    @TableField(value = "LH_MACHINE_QTY")
    private BigDecimal lhMachineQty;

    /** 外胎代码，多个采用" / "分割 */
    @ApiModelProperty(value = "外胎代码", name = "sapCode")
    @TableField(value = "SAP_CODE")
    private String sapCode;

    /** 外胎规格描述，多个采用" / "分割 */
    @ApiModelProperty(value = "外胎规格描述", name = "specDesc")
    @TableField(value = "SPEC_DESC")
    private String specDesc;

    /** 胎胚代码 */
    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /** 胎胚寸口 */
    @ApiModelProperty(value = "胎胚寸口", name = "specDimension")
    @TableField(value = "SPEC_DIMENSION")
    private BigDecimal specDimension;

    /** 结构 */
    @ApiModelProperty(value = "结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /** 胎胚库存 */
    @ApiModelProperty(value = "胎胚库存", name = "totalStock")
    @TableField(value = "TOTAL_STOCK")
    private BigDecimal totalStock;

    /** 施工版本信息 */
    @ApiModelProperty(value = "施工版本信息", name = "bomDataVersion")
    @TableField(value = "BOM_DATA_VERSION")
    private String bomDataVersion;

    /** 胎胚总计划量 */
    @ApiModelProperty(value = "胎胚总计划量", name = "productNum")
    @TableField(value = "PRODUCT_NUM")
    private BigDecimal productNum;

    // ========== 一班 ==========
    /** 一班计划数 */
    @ApiModelProperty(value = "一班计划数", name = "class1PlanQty")
    @TableField(value = "CLASS1_PLAN_QTY")
    private BigDecimal class1PlanQty;

    /** 一班原因分析手工输入 */
    @ApiModelProperty(value = "一班原因分析手工输入", name = "class1AnalysisInput")
    @TableField(value = "CLASS1_ANALYSIS_INPUT")
    private String class1AnalysisInput;

    /** 一班完成量 */
    @ApiModelProperty(value = "一班完成量", name = "class1FinishQty")
    @TableField(value = "CLASS1_FINISH_QTY")
    private BigDecimal class1FinishQty;

    /** 一班原因分析 */
    @ApiModelProperty(value = "一班原因分析", name = "class1Analysis")
    @TableField(value = "CLASS1_ANALYSIS")
    private String class1Analysis;

    // ========== 二班 ==========
    /** 二班计划数 */
    @ApiModelProperty(value = "二班计划数", name = "class2PlanQty")
    @TableField(value = "CLASS2_PLAN_QTY")
    private BigDecimal class2PlanQty;

    /** 二班原因分析手工输入 */
    @ApiModelProperty(value = "二班原因分析手工输入", name = "class2AnalysisInput")
    @TableField(value = "CLASS2_ANALYSIS_INPUT")
    private String class2AnalysisInput;

    /** 二班完成量 */
    @ApiModelProperty(value = "二班完成量", name = "class2FinishQty")
    @TableField(value = "CLASS2_FINISH_QTY")
    private BigDecimal class2FinishQty;

    /** 二班原因分析 */
    @ApiModelProperty(value = "二班原因分析", name = "class2Analysis")
    @TableField(value = "CLASS2_ANALYSIS")
    private String class2Analysis;

    // ========== 三班 ==========
    /** 三班计划数 */
    @ApiModelProperty(value = "三班计划数", name = "class3PlanQty")
    @TableField(value = "CLASS3_PLAN_QTY")
    private BigDecimal class3PlanQty;

    /** 三班原因分析手工输入 */
    @ApiModelProperty(value = "三班原因分析手工输入", name = "class3AnalysisInput")
    @TableField(value = "CLASS3_ANALYSIS_INPUT")
    private String class3AnalysisInput;

    /** 三班完成量 */
    @ApiModelProperty(value = "三班完成量", name = "class3FinishQty")
    @TableField(value = "CLASS3_FINISH_QTY")
    private BigDecimal class3FinishQty;

    /** 三班原因分析 */
    @ApiModelProperty(value = "三班原因分析", name = "class3Analysis")
    @TableField(value = "CLASS3_ANALYSIS")
    private String class3Analysis;

    // ========== 四班 ==========
    /** 四班计划数 */
    @ApiModelProperty(value = "四班计划数", name = "class4PlanQty")
    @TableField(value = "CLASS4_PLAN_QTY")
    private BigDecimal class4PlanQty;

    /** 四班原因分析手工输入 */
    @ApiModelProperty(value = "四班原因分析手工输入", name = "class4AnalysisInput")
    @TableField(value = "CLASS4_ANALYSIS_INPUT")
    private String class4AnalysisInput;

    /** 四班完成量 */
    @ApiModelProperty(value = "四班完成量", name = "class4FinishQty")
    @TableField(value = "CLASS4_FINISH_QTY")
    private BigDecimal class4FinishQty;

    /** 四班原因分析 */
    @ApiModelProperty(value = "四班原因分析", name = "class4Analysis")
    @TableField(value = "CLASS4_ANALYSIS")
    private String class4Analysis;

    // ========== 五班 ==========
    /** 五班计划数 */
    @ApiModelProperty(value = "五班计划数", name = "class5PlanQty")
    @TableField(value = "CLASS5_PLAN_QTY")
    private BigDecimal class5PlanQty;

    /** 五班原因分析手工输入 */
    @ApiModelProperty(value = "五班原因分析手工输入", name = "class5AnalysisInput")
    @TableField(value = "CLASS5_ANALYSIS_INPUT")
    private String class5AnalysisInput;

    /** 五班完成量 */
    @ApiModelProperty(value = "五班完成量", name = "class5FinishQty")
    @TableField(value = "CLASS5_FINISH_QTY")
    private String class5FinishQty;

    /** 五班原因分析 */
    @ApiModelProperty(value = "五班原因分析", name = "class5Analysis")
    @TableField(value = "CLASS5_ANALYSIS")
    private String class5Analysis;

    // ========== 六班 ==========
    /** 六班计划数 */
    @ApiModelProperty(value = "六班计划数", name = "class6PlanQty")
    @TableField(value = "CLASS6_PLAN_QTY")
    private BigDecimal class6PlanQty;

    /** 六班原因分析手工输入 */
    @ApiModelProperty(value = "六班原因分析手工输入", name = "class6AnalysisInput")
    @TableField(value = "CLASS6_ANALYSIS_INPUT")
    private String class6AnalysisInput;

    /** 六班完成量 */
    @ApiModelProperty(value = "六班完成量", name = "class6FinishQty")
    @TableField(value = "CLASS6_FINISH_QTY")
    private String class6FinishQty;

    /** 六班原因分析 */
    @ApiModelProperty(value = "六班原因分析", name = "class6Analysis")
    @TableField(value = "CLASS6_ANALYSIS")
    private String class6Analysis;

    // ========== 七班 ==========
    /** 七班计划数 */
    @ApiModelProperty(value = "七班计划数", name = "class7PlanQty")
    @TableField(value = "CLASS7_PLAN_QTY")
    private BigDecimal class7PlanQty;

    /** 七班原因分析手工输入 */
    @ApiModelProperty(value = "七班原因分析手工输入", name = "class7AnalysisInput")
    @TableField(value = "CLASS7_ANALYSIS_INPUT")
    private String class7AnalysisInput;

    /** 七班完成量 */
    @ApiModelProperty(value = "七班完成量", name = "class7FinishQty")
    @TableField(value = "CLASS7_FINISH_QTY")
    private String class7FinishQty;

    /** 七班原因分析 */
    @ApiModelProperty(value = "七班原因分析", name = "class7Analysis")
    @TableField(value = "CLASS7_ANALYSIS")
    private String class7Analysis;

    // ========== 八班 ==========
    /** 八班计划数 */
    @ApiModelProperty(value = "八班计划数", name = "class8PlanQty")
    @TableField(value = "CLASS8_PLAN_QTY")
    private BigDecimal class8PlanQty;

    /** 八班原因分析手工输入 */
    @ApiModelProperty(value = "八班原因分析手工输入", name = "class8AnalysisInput")
    @TableField(value = "CLASS8_ANALYSIS_INPUT")
    private String class8AnalysisInput;

    /** 八班完成量 */
    @ApiModelProperty(value = "八班完成量", name = "class8FinishQty")
    @TableField(value = "CLASS8_FINISH_QTY")
    private String class8FinishQty;

    /** 八班原因分析 */
    @ApiModelProperty(value = "八班原因分析", name = "class8Analysis")
    @TableField(value = "CLASS8_ANALYSIS")
    private String class8Analysis;

    /** 收尾提示标识：(0-提示收尾；1-不需要提示) */
    @ApiModelProperty(value = "收尾提示标识", name = "markCloseOutTip")
    @TableField(value = "MARK_CLOSE_OUT_TIP")
    private String markCloseOutTip;

    /** 数据来源：0>自动排程；1>插单；2>导入。插单数据可以进行计划调整 */
    @ApiModelProperty(value = "数据来源", name = "dataSource")
    @TableField(value = "DATA_SOURCE")
    private String dataSource;

    /** 特殊要求 */
    @ApiModelProperty(value = "特殊要求", name = "specialRequirements")
    @TableField(value = "SPECIAL_REQUIREMENTS")
    private String specialRequirements;
}
