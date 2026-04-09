package com.zlt.aps.cx.entity.schedule;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 成型排程结果表（单表结构）
 *
 * <p>每条记录代表一个成型机台+胎胚在排程周期内的8班次排产计划。
 * 不再使用主子表结构，所有班次排量直接拍平到 CLASS1~8 字段。
 *
 * <p>对应表：T_CX_SCHEDULE_RESULT
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_CX_SCHEDULE_RESULT")
@ApiModel(value = "成型排程结果对象", description = "成型排程结果表（单表）")
public class CxScheduleResult extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "成型批次号")
    @TableField("CX_BATCH_NO")
    private String cxBatchNo;

    @ApiModelProperty(value = "工单号")
    @TableField("ORDER_NO")
    private String orderNo;

    @ApiModelProperty(value = "生产状态：0-未生产；1-生产中；2-已收尾")
    @TableField("PRODUCTION_STATUS")
    private String productionStatus;

    @ApiModelProperty(value = "是否发布：0--未发布，1--已发布")
    @TableField("IS_RELEASE")
    private String isRelease;

    @ApiModelProperty(value = "排程日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @TableField("SCHEDULE_DATE")
    private Date scheduleDate;

    @ApiModelProperty(value = "成型机台编号")
    @TableField("CX_MACHINE_CODE")
    private String cxMachineCode;

    @ApiModelProperty(value = "成型机台名称")
    @TableField("CX_MACHINE_NAME")
    private String cxMachineName;

    @ApiModelProperty(value = "成型机台类型")
    @TableField("CX_MACHINE_TYPE")
    private String cxMachineType;

    @ApiModelProperty(value = "硫化排程任务序号")
    @TableField("LH_SCHEDULE_IDS")
    private String lhScheduleIds;

    @ApiModelProperty(value = "硫化机台编号")
    @TableField("LH_MACHINE_CODE")
    private String lhMachineCode;

    @ApiModelProperty(value = "硫化机台名称")
    @TableField("LH_MACHINE_NAME")
    private String lhMachineName;

    @ApiModelProperty(value = "硫化机使用总模数")
    @TableField("LH_MACHINE_QTY")
    private BigDecimal lhMachineQty;

    /**
     * 物料编号
     */
    @Excel(name = "ui.data.column.lhScheduleResult.materialCode")
    @ApiModelProperty(value = "物料编号", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

   /**
     * 物料描述
     */
    @Excel(name = "ui.data.column.lhScheduleResult.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    @ApiModelProperty(value = "胎胚代码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;
   /**
     * 主物料(胎胚描述)
     */
    @Excel(name = "ui.data.column.lhScheduleResult.mainMaterialDesc")
    @ApiModelProperty(value = "主物料(胎胚描述)", name = "mainMaterialDesc")
    @TableField(value = "MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    @ApiModelProperty(value = "胎胚寸口")
    @TableField("SPEC_DIMENSION")
    private BigDecimal specDimension;

    @ApiModelProperty(value = "结构")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    @ApiModelProperty(value = "胎胚库存")
    @TableField("TOTAL_STOCK")
    private BigDecimal totalStock;

    @ApiModelProperty(value = "施工版本信息")
    @TableField("BOM_DATA_VERSION")
    private String bomDataVersion;

    @ApiModelProperty(value = "胎胚总计划量")
    @TableField("PRODUCT_NUM")
    private BigDecimal productNum;

    // ========== 一班 ==========
    @ApiModelProperty(value = "一班计划数")
    @TableField("CLASS1_PLAN_QTY")
    private BigDecimal class1PlanQty;

    @ApiModelProperty(value = "一班原因分析手工输入")
    @TableField("CLASS1_ANALYSIS_INPUT")
    private String class1AnalysisInput;

    @ApiModelProperty(value = "一班完成量")
    @TableField("CLASS1_FINISH_QTY")
    private BigDecimal class1FinishQty;

    @ApiModelProperty(value = "一班原因分析")
    @TableField("CLASS1_ANALYSIS")
    private String class1Analysis;

    @ApiModelProperty(value = "一班示方书类型")
    @TableField("CLASS1_RECIPE_TYPE")
    private String class1RecipeType;

    @ApiModelProperty(value = "一班示方书编号")
    @TableField("CLASS1_RECIPE_NO")
    private String class1RecipeNo;

    // ========== 二班 ==========
    @ApiModelProperty(value = "二班计划数")
    @TableField("CLASS2_PLAN_QTY")
    private BigDecimal class2PlanQty;

    @ApiModelProperty(value = "二班原因分析手工输入")
    @TableField("CLASS2_ANALYSIS_INPUT")
    private String class2AnalysisInput;

    @ApiModelProperty(value = "二班完成量")
    @TableField("CLASS2_FINISH_QTY")
    private BigDecimal class2FinishQty;

    @ApiModelProperty(value = "二班原因分析")
    @TableField("CLASS2_ANALYSIS")
    private String class2Analysis;

    @ApiModelProperty(value = "二班示方书类型")
    @TableField("CLASS2_RECIPE_TYPE")
    private String class2RecipeType;

    @ApiModelProperty(value = "二班示方书编号")
    @TableField("CLASS2_RECIPE_NO")
    private String class2RecipeNo;

    // ========== 三班 ==========
    @ApiModelProperty(value = "三班计划数")
    @TableField("CLASS3_PLAN_QTY")
    private BigDecimal class3PlanQty;

    @ApiModelProperty(value = "三班原因分析手工输入")
    @TableField("CLASS3_ANALYSIS_INPUT")
    private String class3AnalysisInput;

    @ApiModelProperty(value = "三班完成量")
    @TableField("CLASS3_FINISH_QTY")
    private BigDecimal class3FinishQty;

    @ApiModelProperty(value = "三班原因分析")
    @TableField("CLASS3_ANALYSIS")
    private String class3Analysis;

    @ApiModelProperty(value = "三班示方书类型")
    @TableField("CLASS3_RECIPE_TYPE")
    private String class3RecipeType;

    @ApiModelProperty(value = "三班示方书编号")
    @TableField("CLASS3_RECIPE_NO")
    private String class3RecipeNo;

    // ========== 四班 ==========
    @ApiModelProperty(value = "四班计划数")
    @TableField("CLASS4_PLAN_QTY")
    private BigDecimal class4PlanQty;

    @ApiModelProperty(value = "四班原因分析手工输入")
    @TableField("CLASS4_ANALYSIS_INPUT")
    private String class4AnalysisInput;

    @ApiModelProperty(value = "四班完成量")
    @TableField("CLASS4_FINISH_QTY")
    private BigDecimal class4FinishQty;

    @ApiModelProperty(value = "四班原因分析")
    @TableField("CLASS4_ANALYSIS")
    private String class4Analysis;

    @ApiModelProperty(value = "四班示方书类型")
    @TableField("CLASS4_RECIPE_TYPE")
    private String class4RecipeType;

    @ApiModelProperty(value = "四班示方书编号")
    @TableField("CLASS4_RECIPE_NO")
    private String class4RecipeNo;

    // ========== 五班 ==========
    @ApiModelProperty(value = "五班计划数")
    @TableField("CLASS5_PLAN_QTY")
    private BigDecimal class5PlanQty;

    @ApiModelProperty(value = "五班原因分析手工输入")
    @TableField("CLASS5_ANALYSIS_INPUT")
    private String class5AnalysisInput;

    @ApiModelProperty(value = "五班完成量")
    @TableField("CLASS5_FINISH_QTY")
    private BigDecimal class5FinishQty;

    @ApiModelProperty(value = "五班原因分析")
    @TableField("CLASS5_ANALYSIS")
    private String class5Analysis;

    @ApiModelProperty(value = "五班示方书类型")
    @TableField("CLASS5_RECIPE_TYPE")
    private String class5RecipeType;

    @ApiModelProperty(value = "五班示方书编号")
    @TableField("CLASS5_RECIPE_NO")
    private String class5RecipeNo;

    // ========== 六班 ==========
    @ApiModelProperty(value = "六班计划数")
    @TableField("CLASS6_PLAN_QTY")
    private BigDecimal class6PlanQty;

    @ApiModelProperty(value = "六班原因分析手工输入")
    @TableField("CLASS6_ANALYSIS_INPUT")
    private String class6AnalysisInput;

    @ApiModelProperty(value = "六班完成量")
    @TableField("CLASS6_FINISH_QTY")
    private BigDecimal class6FinishQty;

    @ApiModelProperty(value = "六班原因分析")
    @TableField("CLASS6_ANALYSIS")
    private String class6Analysis;

    @ApiModelProperty(value = "六班示方书类型")
    @TableField("CLASS6_RECIPE_TYPE")
    private String class6RecipeType;

    @ApiModelProperty(value = "六班示方书编号")
    @TableField("CLASS6_RECIPE_NO")
    private String class6RecipeNo;

    // ========== 七班 ==========
    @ApiModelProperty(value = "七班计划数")
    @TableField("CLASS7_PLAN_QTY")
    private BigDecimal class7PlanQty;

    @ApiModelProperty(value = "七班原因分析手工输入")
    @TableField("CLASS7_ANALYSIS_INPUT")
    private String class7AnalysisInput;

    @ApiModelProperty(value = "七班完成量")
    @TableField("CLASS7_FINISH_QTY")
    private BigDecimal class7FinishQty;

    @ApiModelProperty(value = "七班原因分析")
    @TableField("CLASS7_ANALYSIS")
    private String class7Analysis;

    @ApiModelProperty(value = "七班示方书类型")
    @TableField("CLASS7_RECIPE_TYPE")
    private String class7RecipeType;

    @ApiModelProperty(value = "七班示方书编号")
    @TableField("CLASS7_RECIPE_NO")
    private String class7RecipeNo;

    // ========== 八班 ==========
    @ApiModelProperty(value = "八班计划数")
    @TableField("CLASS8_PLAN_QTY")
    private BigDecimal class8PlanQty;

    @ApiModelProperty(value = "八班原因分析手工输入")
    @TableField("CLASS8_ANALYSIS_INPUT")
    private String class8AnalysisInput;

    @ApiModelProperty(value = "八班完成量")
    @TableField("CLASS8_FINISH_QTY")
    private BigDecimal class8FinishQty;

    @ApiModelProperty(value = "八班原因分析")
    @TableField("CLASS8_ANALYSIS")
    private String class8Analysis;

    @ApiModelProperty(value = "八班示方书类型")
    @TableField("CLASS8_RECIPE_TYPE")
    private String class8RecipeType;

    @ApiModelProperty(value = "八班示方书编号")
    @TableField("CLASS8_RECIPE_NO")
    private String class8RecipeNo;

    // ========== 其他字段 ==========
    @ApiModelProperty(value = "收尾提示标识：0-提示收尾；1-不需要提示")
    @TableField("MARK_CLOSE_OUT_TIP")
    private String markCloseOutTip;

    @ApiModelProperty(value = "数据来源：0-自动排程；1-插单；2-导入")
    @TableField("DATA_SOURCE")
    private String dataSource;

    @ApiModelProperty(value = "特殊要求")
    @TableField("SPECIAL_REQUIREMENTS")
    private String specialRequirements;

    @ApiModelProperty(value = "成型余量")
    @TableField("CX_REMAIN_QTY")
    private BigDecimal cxRemainQty;

    @ApiModelProperty(value = "硫化余量")
    @TableField("LH_REMAIN_QTY")
    private BigDecimal lhRemainQty;

    @ApiModelProperty(value = "硫化班产")
    @TableField("LH_CLASS_QTY")
    private BigDecimal lhClassQty;
}
