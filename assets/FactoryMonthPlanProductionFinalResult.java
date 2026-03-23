package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 工厂月生产计划-最终排产计划定稿对象 t_mp_month_plan_prod_final
 */
@Data
@TableName(value = "T_MP_MONTH_PLAN_PROD_FINAL")
@ApiModel(value = "工厂月生产计划-最终排产计划定稿对象", description = "工厂月生产计划-最终排产计划定稿对象")
@EqualsAndHashCode(callSuper = true)
public class FactoryMonthPlanProductionFinalResult extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工单号 */
    @ApiModelProperty(value = "工单号", name = "productionNo")
    @TableField(value = "PRODUCTION_NO")
    private String productionNo;

    /** 工厂编码 */
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 年份 */
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 年月:YYYYMM */
    @ApiModelProperty(value = "年月", name = "yearMonth")
    @TableField(value = "`YEAR_MONTH`")
    private Integer yearMonth;

    /** 销售生产需求计划版本 */
    @ApiModelProperty(value = "需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /** 最新需求计划版本 */
    @ApiModelProperty(value = "最新需求计划版本", name = "lastMonthPlanVersion")
    @TableField(value = "LAST_MONTH_PLAN_VERSION")
    private String lastMonthPlanVersion;

    /** 排产计划版本 */
    @ApiModelProperty(value = "月度生产计划版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /** 产品品类 */
    @ApiModelProperty(value = "产品品类", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /** 物料编码 */
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** MES物料编码 */
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /** 产品结构 */
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /** 英寸 */
    @ApiModelProperty(value = "英寸", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private String proSize;

    /** 产品分类 */
    @ApiModelProperty(value = "产品分类", name = "productCategory")
    @TableField(value = "PRODUCT_CATEGORY")
    private String productCategory;

    /** 产品状态 */
    @ApiModelProperty(value = "产品状态", name = "productStatus")
    @TableField(value = "PRODUCT_STATUS")
    private String productStatus;

    /** 结构类型 */
    @ApiModelProperty(value = "结构类型", name = "structureType")
    @TableField(value = "STRUCTURE_TYPE")
    private String structureType;

    /** 排产分类 */
    @ApiModelProperty(value = "排产分类", name = "productionType")
    @TableField(value = "PRODUCTION_TYPE")
    private String productionType;

    /** 生胎代码 */
    @ApiModelProperty(value = "生胎代码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /** 主物料(胎胚号) */
    @ApiModelProperty(value = "主物料(胎胚号)", name = "mainMaterialDesc")
    @TableField(value = "MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    /** 施工阶段 */
    @ApiModelProperty(value = "施工阶段", name = "constructionStage")
    @TableField(value = "CONSTRUCTION_STAGE")
    private String constructionStage;

    /** 是否零度材料 */
    @ApiModelProperty(value = "是否零度材料", name = "isZeroRack")
    @TableField(value = "IS_ZERO_RACK")
    private String isZeroRack;

    /** 制造示方书号 */
    @ApiModelProperty(value = "制造示方书号", name = "embryoNo")
    @TableField(value = "EMBRYO_NO")
    private String embryoNo;

    /** 文字示方书号 */
    @ApiModelProperty(value = "文字示方书号", name = "textNo")
    @TableField(value = "TEXT_NO")
    private String textNo;

    /** 硫化示方书号 */
    @ApiModelProperty(value = "硫化示方书号", name = "lhNo")
    @TableField(value = "LH_NO")
    private String lhNo;

    /** 品牌 */
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /** 规格 */
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /** 主花纹 */
    @ApiModelProperty(value = "主花纹", name = "mainPattern")
    @TableField(value = "MAIN_PATTERN")
    private String mainPattern;

    /** 花纹 */
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /** 型腔数量 */
    @ApiModelProperty(value = "型腔数量", name = "mouldCavityQty")
    @TableField(value = "MOULD_CAVITY_QTY")
    private Integer mouldCavityQty;

    /** 活块数量 */
    @ApiModelProperty(value = "活块数量", name = "typeBlockQty")
    @TableField(value = "TYPE_BLOCK_QTY")
    private Integer typeBlockQty;

    /** 高优先级数量 */
    @ApiModelProperty(value = "高优先级数量", name = "heightQty")
    @TableField(value = "HEIGHT_QTY")
    private Integer heightQty;

    /** 月均销量 */
    @ApiModelProperty(value = "月均销量", name = "averageSaleQty")
    @TableField(value = "AVERAGE_SALE_QTY")
    private Integer averageSaleQty;

    /** 库销比 */
    @ApiModelProperty(value = "库销比", name = "inventorySalesRatio")
    @TableField(value = "INVENTORY_SALES_RATIO")
    private BigDecimal inventorySalesRatio;

    /** 日硫化量 */
    @ApiModelProperty(value = "日硫化量", name = "dayVulcanizationQty")
    @TableField(value = "DAY_VULCANIZATION_QTY")
    private Integer dayVulcanizationQty;

    /** 成型机台信息 */
    @ApiModelProperty(value = "成型机台信息", name = "cxMachineCode")
    @TableField(value = "CX_MACHINE_CODE")
    private String cxMachineCode;

    /** 模具使用变化信息 */
    @ApiModelProperty(value = "模具使用变化信息", name = "mouldChangeInfo")
    @TableField(value = "MOULD_CHANGE_INFO")
    private String mouldChangeInfo;

    /** 动平衡数量 */
    @ApiModelProperty(value = "动平衡数量", name = "dynamicBalanceQty")
    @TableField(value = "DYNAMIC_BALANCE_QTY")
    private String dynamicBalanceQty;

    /** 均匀性数量 */
    @ApiModelProperty(value = "均匀性数量", name = "uniformityQty")
    @TableField(value = "UNIFORMITY_QTY")
    private Integer uniformityQty;

    /** 是否EXCEL导入 */
    @ApiModelProperty(value = "是否EXCEL导入", name = "isImport")
    @TableField(value = "IS_IMPORT")
    private String isImport;

    /** 排产顺序 */
    @ApiModelProperty(value = "排产顺序", name = "productionSequence")
    @TableField(value = "PRODUCTION_SEQUENCE")
    private Long productionSequence;

    /** 单条硫化时间 */
    @ApiModelProperty(value = "单条硫化时间", name = "curingTime")
    @TableField(value = "CURING_TIME")
    private Integer curingTime;

    /** 生产需求计划 */
    @ApiModelProperty(value = "净需求", name = "prodReqPlan")
    @TableField(value = "PROD_REQ_PLAN")
    private Integer prodReqPlan;

    /** 试制量试计划需求量 */
    @ApiModelProperty(value = "试制量试计划需求量", name = "trialQty")
    @TableField(value = "TRIAL_QTY")
    private Integer trialQty;

    /** 高优先级排产数量 */
    @ApiModelProperty(value = "高优先级排产数量", name = "heightProductionQty")
    @TableField(value = "HEIGHT_PRODUCTION_QTY")
    private Integer heightProductionQty;

    /** 实际生产需求(含损耗) */
    @ApiModelProperty(value = "实际生产需求", name = "factProdReqQty")
    @TableField(value = "FACT_PROD_REQ_QTY")
    private Integer factProdReqQty;

    /** 生产实际排产量 */
    @ApiModelProperty(value = "生产实际排产量", name = "totalQty")
    @TableField(value = "TOTAL_QTY")
    private Integer totalQty;

    /** 中优先级排产数量 */
    @ApiModelProperty(value = "中优先级排产数量", name = "midProductionQty")
    @TableField(value = "MID_PRODUCTION_QTY")
    private Integer midProductionQty;

    /** 周期排产储备排产数量 */
    @ApiModelProperty(value = "周期排产储备排产数量", name = "cycleProductionQty")
    @TableField(value = "CYCLE_PRODUCTION_QTY")
    private Integer cycleProductionQty;

    /** 常规储备排产数量 */
    @ApiModelProperty(value = "常规储备排产数量", name = "conventionProductionQty")
    @TableField(value = "CONVENTION_PRODUCTION_QTY")
    private Integer conventionProductionQty;

    /** 暂缓订单排产数量 */
    @ApiModelProperty(value = "暂缓订单排产数量", name = "postponeProductionQty")
    @TableField(value = "POSTPONE_PRODUCTION_QTY")
    private Integer postponeProductionQty;

    /** 试制量试排产量 */
    @ApiModelProperty(value = "试制量试排产量", name = "trialProductionQty")
    @TableField(value = "TRIAL_PRODUCTION_QTY")
    private Integer trialProductionQty;

    /** 差异量(未排产数量) */
    @ApiModelProperty(value = "差异量", name = "differenceQty")
    @TableField(value = "DIFFERENCE_QTY")
    private Integer differenceQty;

    /** 第1周调整量 */
    @ApiModelProperty(value = "第1周调整量", name = "adjustQty1")
    @TableField(value = "ADJUST_QTY1")
    private Integer adjustQty1;

    /** 第2周调整量 */
    @ApiModelProperty(value = "第2周调整量", name = "adjustQty2")
    @TableField(value = "ADJUST_QTY2")
    private Integer adjustQty2;

    /** 第3周调整量 */
    @ApiModelProperty(value = "第3周调整量", name = "adjustQty3")
    @TableField(value = "ADJUST_QTY3")
    private Integer adjustQty3;

    /** 第4周调整量 */
    @ApiModelProperty(value = "第4周调整量", name = "adjustQty4")
    @TableField(value = "ADJUST_QTY4")
    private Integer adjustQty4;

    /** 未排产原因 */
    @ApiModelProperty(value = "未排产原因", name = "reason")
    @TableField(value = "REASON")
    private String reason;

    /** 开始日期 */
    @ApiModelProperty(value = "开始日期", name = "beginDay")
    @TableField(value = "BEGIN_DAY")
    private Integer beginDay;

    /** 结束日期 */
    @ApiModelProperty(value = "结束日期", name = "endDay")
    @TableField(value = "END_DAY")
    private Integer endDay;

    /** DAY_1 ~ DAY_31 */
    @ApiModelProperty(value = "DAY_1", name = "day1")
    @TableField(value = "DAY_1")
    private Integer day1;

    @ApiModelProperty(value = "DAY_2", name = "day2")
    @TableField(value = "DAY_2")
    private Integer day2;

    @ApiModelProperty(value = "DAY_3", name = "day3")
    @TableField(value = "DAY_3")
    private Integer day3;

    @ApiModelProperty(value = "DAY_4", name = "day4")
    @TableField(value = "DAY_4")
    private Integer day4;

    @ApiModelProperty(value = "DAY_5", name = "day5")
    @TableField(value = "DAY_5")
    private Integer day5;

    @ApiModelProperty(value = "DAY_6", name = "day6")
    @TableField(value = "DAY_6")
    private Integer day6;

    @ApiModelProperty(value = "DAY_7", name = "day7")
    @TableField(value = "DAY_7")
    private Integer day7;

    @ApiModelProperty(value = "DAY_8", name = "day8")
    @TableField(value = "DAY_8")
    private Integer day8;

    @ApiModelProperty(value = "DAY_9", name = "day9")
    @TableField(value = "DAY_9")
    private Integer day9;

    @ApiModelProperty(value = "DAY_10", name = "day10")
    @TableField(value = "DAY_10")
    private Integer day10;

    @ApiModelProperty(value = "DAY_11", name = "day11")
    @TableField(value = "DAY_11")
    private Integer day11;

    @ApiModelProperty(value = "DAY_12", name = "day12")
    @TableField(value = "DAY_12")
    private Integer day12;

    @ApiModelProperty(value = "DAY_13", name = "day13")
    @TableField(value = "DAY_13")
    private Integer day13;

    @ApiModelProperty(value = "DAY_14", name = "day14")
    @TableField(value = "DAY_14")
    private Integer day14;

    @ApiModelProperty(value = "DAY_15", name = "day15")
    @TableField(value = "DAY_15")
    private Integer day15;

    @ApiModelProperty(value = "DAY_16", name = "day16")
    @TableField(value = "DAY_16")
    private Integer day16;

    @ApiModelProperty(value = "DAY_17", name = "day17")
    @TableField(value = "DAY_17")
    private Integer day17;

    @ApiModelProperty(value = "DAY_18", name = "day18")
    @TableField(value = "DAY_18")
    private Integer day18;

    @ApiModelProperty(value = "DAY_19", name = "day19")
    @TableField(value = "DAY_19")
    private Integer day19;

    @ApiModelProperty(value = "DAY_20", name = "day20")
    @TableField(value = "DAY_20")
    private Integer day20;

    @ApiModelProperty(value = "DAY_21", name = "day21")
    @TableField(value = "DAY_21")
    private Integer day21;

    @ApiModelProperty(value = "DAY_22", name = "day22")
    @TableField(value = "DAY_22")
    private Integer day22;

    @ApiModelProperty(value = "DAY_23", name = "day23")
    @TableField(value = "DAY_23")
    private Integer day23;

    @ApiModelProperty(value = "DAY_24", name = "day24")
    @TableField(value = "DAY_24")
    private Integer day24;

    @ApiModelProperty(value = "DAY_25", name = "day25")
    @TableField(value = "DAY_25")
    private Integer day25;

    @ApiModelProperty(value = "DAY_26", name = "day26")
    @TableField(value = "DAY_26")
    private Integer day26;

    @ApiModelProperty(value = "DAY_27", name = "day27")
    @TableField(value = "DAY_27")
    private Integer day27;

    @ApiModelProperty(value = "DAY_28", name = "day28")
    @TableField(value = "DAY_28")
    private Integer day28;

    @ApiModelProperty(value = "DAY_29", name = "day29")
    @TableField(value = "DAY_29")
    private Integer day29;

    @ApiModelProperty(value = "DAY_30", name = "day30")
    @TableField(value = "DAY_30")
    private Integer day30;

    @ApiModelProperty(value = "DAY_31", name = "day31")
    @TableField(value = "DAY_31")
    private Integer day31;

    /** 硫化总工时 */
    @ApiModelProperty(value = "硫化总工时", name = "totalVulcanizationMinutes")
    @TableField(value = "TOTAL_VULCANIZATION_MINUTES")
    private BigDecimal totalVulcanizationMinutes;

    /** 显示顺序 */
    @ApiModelProperty(value = "显示顺序", name = "displaySeq")
    @TableField(value = "DISPLAY_SEQ")
    private Integer displaySeq;

    /** 发布状态 */
    @ApiModelProperty(value = "发布状态", name = "isRelease")
    @TableField(value = "IS_RELEASE")
    private String isRelease;

    public Integer getDayLhQty() {
        if (null == this.dayVulcanizationQty) {
            return 0;
        }
        return dayVulcanizationQty * 2;
    }

    public String getGroupKey() {
        String keyFormat = "%s|*|%s";
        return String.format(keyFormat, factoryCode, materialDesc);
    }
}
