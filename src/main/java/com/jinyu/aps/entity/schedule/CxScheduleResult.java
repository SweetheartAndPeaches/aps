package com.jinyu.aps.entity.schedule;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.jinyu.aps.entity.base.ApsBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成型排程结果表
 * 对应表：T_CX_SCHEDULE_RESULT
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_CX_SCHEDULE_RESULT")
@Schema(description = "成型排程结果对象")
public class CxScheduleResult extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 自动排程批次号信息，每重新生成一次排程结果，批次号就递增。
     * 规则：工序+年月日+3位定长自增序号
     */
    @Schema(description = "成型批次号")
    @TableField("CX_BATCH_NO")
    private String cxBatchNo;

    /**
     * 成型排程工单号，自动生成，批次号+4位定长自增序号
     */
    @Schema(description = "工单号")
    @TableField("ORDER_NO")
    private String orderNo;

    /**
     * 生产状态：0-未生产；1-生产中；2-已收尾
     */
    @Schema(description = "生产状态：0-未生产；1-生产中；2-已收尾")
    @TableField("PRODUCTION_STATUS")
    private String productionStatus;

    /**
     * 是否发布：0--未发布，1--已发布
     */
    @Schema(description = "是否发布：0--未发布，1--已发布")
    @TableField("IS_RELEASE")
    private String isRelease;

    /**
     * 排程日期
     */
    @Schema(description = "排程日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("SCHEDULE_DATE")
    private LocalDateTime scheduleDate;

    /**
     * 成型机台编号
     */
    @Schema(description = "成型机台编号")
    @TableField("CX_MACHINE_CODE")
    private String cxMachineCode;

    /**
     * 成型机台名称
     */
    @Schema(description = "成型机台名称")
    @TableField("CX_MACHINE_NAME")
    private String cxMachineName;

    /**
     * 成型机台类型
     */
    @Schema(description = "成型机台类型")
    @TableField("CX_MACHINE_TYPE")
    private String cxMachineType;

    /**
     * 硫化排程任务序号，多个采用" / "分割
     */
    @Schema(description = "硫化排程任务序号")
    @TableField("LH_SCHEDULE_IDS")
    private String lhScheduleIds;

    /**
     * 硫化机台编号，多个采用" / "分割
     */
    @Schema(description = "硫化机台编号")
    @TableField("LH_MACHINE_CODE")
    private String lhMachineCode;

    /**
     * 硫化机台名称，多个采用" / "分割
     */
    @Schema(description = "硫化机台名称")
    @TableField("LH_MACHINE_NAME")
    private String lhMachineName;

    /**
     * 硫化机使用总模数
     */
    @Schema(description = "硫化机使用总模数")
    @TableField("LH_MACHINE_QTY")
    private BigDecimal lhMachineQty;

    /**
     * 外胎代码，多个采用" / "分割
     */
    @Schema(description = "外胎代码")
    @TableField("SAP_CODE")
    private String sapCode;

    /**
     * 外胎规格描述，多个采用" / "分割
     */
    @Schema(description = "外胎规格描述")
    @TableField("SPEC_DESC")
    private String specDesc;

    /**
     * 胎胚代码
     */
    @Schema(description = "胎胚代码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    /**
     * 胎胚寸口
     */
    @Schema(description = "胎胚寸口")
    @TableField("SPEC_DIMENSION")
    private BigDecimal specDimension;

    /**
     * 结构
     */
    @Schema(description = "结构")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    /**
     * 胎胚库存
     */
    @Schema(description = "胎胚库存")
    @TableField("TOTAL_STOCK")
    private BigDecimal totalStock;

    /**
     * 施工版本信息
     */
    @Schema(description = "施工版本信息")
    @TableField("BOM_DATA_VERSION")
    private String bomDataVersion;

    /**
     * 胎胚总计划量
     */
    @Schema(description = "胎胚总计划量")
    @TableField("PRODUCT_NUM")
    private BigDecimal productNum;

    // ========== 一班 ==========

    /**
     * 一班计划数
     */
    @Schema(description = "一班计划数")
    @TableField("CLASS1_PLAN_QTY")
    private BigDecimal class1PlanQty;

    /**
     * 一班原因分析手工输入
     */
    @Schema(description = "一班原因分析手工输入")
    @TableField("CLASS1_ANALYSIS_INPUT")
    private String class1AnalysisInput;

    /**
     * 一班完成量
     */
    @Schema(description = "一班完成量")
    @TableField("CLASS1_FINISH_QTY")
    private BigDecimal class1FinishQty;

    /**
     * 一班原因分析
     */
    @Schema(description = "一班原因分析")
    @TableField("CLASS1_ANALYSIS")
    private String class1Analysis;

    // ========== 二班 ==========

    @Schema(description = "二班计划数")
    @TableField("CLASS2_PLAN_QTY")
    private BigDecimal class2PlanQty;

    @Schema(description = "二班原因分析手工输入")
    @TableField("CLASS2_ANALYSIS_INPUT")
    private String class2AnalysisInput;

    @Schema(description = "二班完成量")
    @TableField("CLASS2_FINISH_QTY")
    private BigDecimal class2FinishQty;

    @Schema(description = "二班原因分析")
    @TableField("CLASS2_ANALYSIS")
    private String class2Analysis;

    // ========== 三班 ==========

    @Schema(description = "三班计划数")
    @TableField("CLASS3_PLAN_QTY")
    private BigDecimal class3PlanQty;

    @Schema(description = "三班原因分析手工输入")
    @TableField("CLASS3_ANALYSIS_INPUT")
    private String class3AnalysisInput;

    @Schema(description = "三班完成量")
    @TableField("CLASS3_FINISH_QTY")
    private BigDecimal class3FinishQty;

    @Schema(description = "三班原因分析")
    @TableField("CLASS3_ANALYSIS")
    private String class3Analysis;

    // ========== 四班 ==========

    @Schema(description = "四班计划数")
    @TableField("CLASS4_PLAN_QTY")
    private BigDecimal class4PlanQty;

    @Schema(description = "四班原因分析手工输入")
    @TableField("CLASS4_ANALYSIS_INPUT")
    private String class4AnalysisInput;

    @Schema(description = "四班完成量")
    @TableField("CLASS4_FINISH_QTY")
    private BigDecimal class4FinishQty;

    @Schema(description = "四班原因分析")
    @TableField("CLASS4_ANALYSIS")
    private String class4Analysis;

    // ========== 五班 ==========

    @Schema(description = "五班计划数")
    @TableField("CLASS5_PLAN_QTY")
    private BigDecimal class5PlanQty;

    @Schema(description = "五班原因分析手工输入")
    @TableField("CLASS5_ANALYSIS_INPUT")
    private String class5AnalysisInput;

    @Schema(description = "五班完成量")
    @TableField("CLASS5_FINISH_QTY")
    private BigDecimal class5FinishQty;

    @Schema(description = "五班原因分析")
    @TableField("CLASS5_ANALYSIS")
    private String class5Analysis;

    // ========== 六班 ==========

    @Schema(description = "六班计划数")
    @TableField("CLASS6_PLAN_QTY")
    private BigDecimal class6PlanQty;

    @Schema(description = "六班原因分析手工输入")
    @TableField("CLASS6_ANALYSIS_INPUT")
    private String class6AnalysisInput;

    @Schema(description = "六班完成量")
    @TableField("CLASS6_FINISH_QTY")
    private BigDecimal class6FinishQty;

    @Schema(description = "六班原因分析")
    @TableField("CLASS6_ANALYSIS")
    private String class6Analysis;

    // ========== 七班 ==========

    @Schema(description = "七班计划数")
    @TableField("CLASS7_PLAN_QTY")
    private BigDecimal class7PlanQty;

    @Schema(description = "七班原因分析手工输入")
    @TableField("CLASS7_ANALYSIS_INPUT")
    private String class7AnalysisInput;

    @Schema(description = "七班完成量")
    @TableField("CLASS7_FINISH_QTY")
    private BigDecimal class7FinishQty;

    @Schema(description = "七班原因分析")
    @TableField("CLASS7_ANALYSIS")
    private String class7Analysis;

    // ========== 八班 ==========

    @Schema(description = "八班计划数")
    @TableField("CLASS8_PLAN_QTY")
    private BigDecimal class8PlanQty;

    @Schema(description = "八班原因分析手工输入")
    @TableField("CLASS8_ANALYSIS_INPUT")
    private String class8AnalysisInput;

    @Schema(description = "八班完成量")
    @TableField("CLASS8_FINISH_QTY")
    private BigDecimal class8FinishQty;

    @Schema(description = "八班原因分析")
    @TableField("CLASS8_ANALYSIS")
    private String class8Analysis;

    // ========== 其他字段 ==========

    /**
     * 收尾提示标识：(0-提示收尾；1-不需要提示)
     */
    @Schema(description = "收尾提示标识：0-提示收尾；1-不需要提示")
    @TableField("MARK_CLOSE_OUT_TIP")
    private String markCloseOutTip;

    /**
     * 数据来源：0>自动排程；1>插单；2>导入。
     * 插单数据可以进行计划调整
     */
    @Schema(description = "数据来源：0-自动排程；1-插单；2-导入")
    @TableField("DATA_SOURCE")
    private String dataSource;

    /**
     * 特殊要求
     */
    @Schema(description = "特殊要求")
    @TableField("SPECIAL_REQUIREMENTS")
    private String specialRequirements;
}
