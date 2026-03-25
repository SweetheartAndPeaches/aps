package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工厂月生产计划-最终排产计划定稿实体类
 * 对应数据库表：T_FACTORY_MONTH_PLAN_PRODUCTION_FINAL_RESULT
 * 
 * 数据来源：ERP/MES系统月度生产计划
 * 用途：作为APS日排程的数据输入源
 *
 * @author APS Team
 */
@Data
@TableName(value = "T_FACTORY_MONTH_PLAN_PRODUCTION_FINAL_RESULT", keepGlobalPrefix = false)
@ApiModel(value = "工厂月生产计划-最终排产计划定稿")
public class FactoryMonthPlanProductionFinalResult implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 主键与基本信息 ====================

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "工单号(MP+年月日+批次号+5位流水)")
    @TableField("PRODUCTION_NO")
    private String productionNo;

    @ApiModelProperty(value = "工厂编码")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "年份")
    @TableField("YEAR")
    private Integer year;

    @ApiModelProperty(value = "月份")
    @TableField("MONTH")
    private Integer month;

    @ApiModelProperty(value = "年月(YYYYMM)")
    @TableField("YEAR_MONTH")
    private Integer yearMonth;

    // ==================== 版本信息 ====================

    @ApiModelProperty(value = "销售生产需求计划版本")
    @TableField("MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    @ApiModelProperty(value = "最新需求计划版本")
    @TableField("LAST_MONTH_PLAN_VERSION")
    private String lastMonthPlanVersion;

    @ApiModelProperty(value = "排产计划版本")
    @TableField("PRODUCTION_VERSION")
    private String productionVersion;

    // ==================== 产品信息 ====================

    @ApiModelProperty(value = "产品品类(TBR全钢/PCR半钢)")
    @TableField("PRODUCT_TYPE_CODE")
    private String productTypeCode;

    @ApiModelProperty(value = "物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    @ApiModelProperty(value = "物料描述")
    @TableField("MATERIAL_DESC")
    private String materialDesc;

    @ApiModelProperty(value = "MES物料编码")
    @TableField("MES_MATERIAL_CODE")
    private String mesMaterialCode;

    @ApiModelProperty(value = "产品结构")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    @ApiModelProperty(value = "英寸")
    @TableField("PRO_SIZE")
    private String proSize;

    @ApiModelProperty(value = "产品分类")
    @TableField("PRODUCT_CATEGORY")
    private String productCategory;

    @ApiModelProperty(value = "产品状态")
    @TableField("PRODUCT_STATUS")
    private String productStatus;

    @ApiModelProperty(value = "结构类型(01周期结构/02常规结构)")
    @TableField("STRUCTURE_TYPE")
    private String structureType;

    @ApiModelProperty(value = "排产分类")
    @TableField("PRODUCTION_TYPE")
    private String productionType;

    // ==================== 胎胚与施工信息 ====================

    @ApiModelProperty(value = "生胎代码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    @ApiModelProperty(value = "主物料(胎胚号)")
    @TableField("MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    @ApiModelProperty(value = "施工阶段(00无工艺/01试制/02量试/03正式)")
    @TableField("CONSTRUCTION_STAGE")
    private String constructionStage;

    @ApiModelProperty(value = "是否零度材料")
    @TableField("IS_ZERO_RACK")
    private String isZeroRack;

    @ApiModelProperty(value = "制造示方书号")
    @TableField("EMBRYO_NO")
    private String embryoNo;

    @ApiModelProperty(value = "文字示方书号")
    @TableField("TEXT_NO")
    private String textNo;

    @ApiModelProperty(value = "硫化示方书号")
    @TableField("LH_NO")
    private String lhNo;

    // ==================== 产品属性 ====================

    @ApiModelProperty(value = "品牌")
    @TableField("BRAND")
    private String brand;

    @ApiModelProperty(value = "规格")
    @TableField("SPECIFICATIONS")
    private String specifications;

    @ApiModelProperty(value = "主花纹")
    @TableField("MAIN_PATTERN")
    private String mainPattern;

    @ApiModelProperty(value = "花纹")
    @TableField("PATTERN")
    private String pattern;

    @ApiModelProperty(value = "型腔数量(同主花纹模具数量)")
    @TableField("MOULD_CAVITY_QTY")
    private Integer mouldCavityQty;

    @ApiModelProperty(value = "活块数量(同主花纹物料模具数量)")
    @TableField("TYPE_BLOCK_QTY")
    private Integer typeBlockQty;

    @ApiModelProperty(value = "高优先级数量")
    @TableField("HEIGHT_QTY")
    private Integer heightQty;

    @ApiModelProperty(value = "月均销量")
    @TableField("AVERAGE_SALE_QTY")
    private Integer averageSaleQty;

    @ApiModelProperty(value = "库销比")
    @TableField("INVENTORY_SALES_RATIO")
    private BigDecimal inventorySalesRatio;

    // ==================== 产能与机台信息 ====================

    @ApiModelProperty(value = "日硫化量")
    @TableField("DAY_VULCANIZATION_QTY")
    private Integer dayVulcanizationQty;

    @ApiModelProperty(value = "成型机台信息(多个逗号分隔)")
    @TableField("CX_MACHINE_CODE")
    private String cxMachineCode;

    @ApiModelProperty(value = "模具使用变化信息(如2-4-2)")
    @TableField("MOULD_CHANGE_INFO")
    private String mouldChangeInfo;

    @ApiModelProperty(value = "动平衡数量")
    @TableField("DYNAMIC_BALANCE_QTY")
    private String dynamicBalanceQty;

    @ApiModelProperty(value = "均匀性数量")
    @TableField("UNIFORMITY_QTY")
    private Integer uniformityQty;

    @ApiModelProperty(value = "单条硫化时间(分钟)")
    @TableField("CURING_TIME")
    private Integer curingTime;

    // ==================== 需求与排产数量 ====================

    @ApiModelProperty(value = "生产需求计划(净需求)")
    @TableField("PROD_REQ_PLAN")
    private Integer prodReqPlan;

    @ApiModelProperty(value = "试制量试计划需求量")
    @TableField("TRIAL_QTY")
    private Integer trialQty;

    @ApiModelProperty(value = "高优先级排产数量")
    @TableField("HEIGHT_PRODUCTION_QTY")
    private Integer heightProductionQty;

    @ApiModelProperty(value = "实际生产需求(含损耗)")
    @TableField("FACT_PROD_REQ_QTY")
    private Integer factProdReqQty;

    @ApiModelProperty(value = "生产实际排产量")
    @TableField("TOTAL_QTY")
    private Integer totalQty;

    @ApiModelProperty(value = "中优先级排产数量")
    @TableField("MID_PRODUCTION_QTY")
    private Integer midProductionQty;

    @ApiModelProperty(value = "周期排产储备排产数量")
    @TableField("CYCLE_PRODUCTION_QTY")
    private Integer cycleProductionQty;

    @ApiModelProperty(value = "常规储备排产数量")
    @TableField("CONVENTION_PRODUCTION_QTY")
    private Integer conventionProductionQty;

    @ApiModelProperty(value = "暂缓订单排产数量")
    @TableField("POSTPONE_PRODUCTION_QTY")
    private Integer postponeProductionQty;

    @ApiModelProperty(value = "试制量试排产量")
    @TableField("TRIAL_PRODUCTION_QTY")
    private Integer trialProductionQty;

    @ApiModelProperty(value = "差异量(未排产数量)")
    @TableField("DIFFERENCE_QTY")
    private Integer differenceQty;

    // ==================== 周调整量 ====================

    @ApiModelProperty(value = "第1周调整量")
    @TableField("ADJUST_QTY1")
    private Integer adjustQty1;

    @ApiModelProperty(value = "第2周调整量")
    @TableField("ADJUST_QTY2")
    private Integer adjustQty2;

    @ApiModelProperty(value = "第3周调整量")
    @TableField("ADJUST_QTY3")
    private Integer adjustQty3;

    @ApiModelProperty(value = "第4周调整量")
    @TableField("ADJUST_QTY4")
    private Integer adjustQty4;

    @ApiModelProperty(value = "未排产原因")
    @TableField("REASON")
    private String reason;

    // ==================== 月度每日排产计划 ====================

    @ApiModelProperty(value = "开始日期(1-31)")
    @TableField("BEGIN_DAY")
    private Integer beginDay;

    @ApiModelProperty(value = "结束日期(1-31)")
    @TableField("END_DAY")
    private Integer endDay;

    @ApiModelProperty(value = "第1天排产量")
    @TableField("DAY_1")
    private Integer day1;

    @ApiModelProperty(value = "第2天排产量")
    @TableField("DAY_2")
    private Integer day2;

    @ApiModelProperty(value = "第3天排产量")
    @TableField("DAY_3")
    private Integer day3;

    @ApiModelProperty(value = "第4天排产量")
    @TableField("DAY_4")
    private Integer day4;

    @ApiModelProperty(value = "第5天排产量")
    @TableField("DAY_5")
    private Integer day5;

    @ApiModelProperty(value = "第6天排产量")
    @TableField("DAY_6")
    private Integer day6;

    @ApiModelProperty(value = "第7天排产量")
    @TableField("DAY_7")
    private Integer day7;

    @ApiModelProperty(value = "第8天排产量")
    @TableField("DAY_8")
    private Integer day8;

    @ApiModelProperty(value = "第9天排产量")
    @TableField("DAY_9")
    private Integer day9;

    @ApiModelProperty(value = "第10天排产量")
    @TableField("DAY_10")
    private Integer day10;

    @ApiModelProperty(value = "第11天排产量")
    @TableField("DAY_11")
    private Integer day11;

    @ApiModelProperty(value = "第12天排产量")
    @TableField("DAY_12")
    private Integer day12;

    @ApiModelProperty(value = "第13天排产量")
    @TableField("DAY_13")
    private Integer day13;

    @ApiModelProperty(value = "第14天排产量")
    @TableField("DAY_14")
    private Integer day14;

    @ApiModelProperty(value = "第15天排产量")
    @TableField("DAY_15")
    private Integer day15;

    @ApiModelProperty(value = "第16天排产量")
    @TableField("DAY_16")
    private Integer day16;

    @ApiModelProperty(value = "第17天排产量")
    @TableField("DAY_17")
    private Integer day17;

    @ApiModelProperty(value = "第18天排产量")
    @TableField("DAY_18")
    private Integer day18;

    @ApiModelProperty(value = "第19天排产量")
    @TableField("DAY_19")
    private Integer day19;

    @ApiModelProperty(value = "第20天排产量")
    @TableField("DAY_20")
    private Integer day20;

    @ApiModelProperty(value = "第21天排产量")
    @TableField("DAY_21")
    private Integer day21;

    @ApiModelProperty(value = "第22天排产量")
    @TableField("DAY_22")
    private Integer day22;

    @ApiModelProperty(value = "第23天排产量")
    @TableField("DAY_23")
    private Integer day23;

    @ApiModelProperty(value = "第24天排产量")
    @TableField("DAY_24")
    private Integer day24;

    @ApiModelProperty(value = "第25天排产量")
    @TableField("DAY_25")
    private Integer day25;

    @ApiModelProperty(value = "第26天排产量")
    @TableField("DAY_26")
    private Integer day26;

    @ApiModelProperty(value = "第27天排产量")
    @TableField("DAY_27")
    private Integer day27;

    @ApiModelProperty(value = "第28天排产量")
    @TableField("DAY_28")
    private Integer day28;

    @ApiModelProperty(value = "第29天排产量")
    @TableField("DAY_29")
    private Integer day29;

    @ApiModelProperty(value = "第30天排产量")
    @TableField("DAY_30")
    private Integer day30;

    @ApiModelProperty(value = "第31天排产量")
    @TableField("DAY_31")
    private Integer day31;

    // ==================== 其他信息 ====================

    @ApiModelProperty(value = "硫化总工时")
    @TableField("TOTAL_VULCANIZATION_MINUTES")
    private BigDecimal totalVulcanizationMinutes;

    @ApiModelProperty(value = "显示顺序")
    @TableField("DISPLAY_SEQ")
    private Integer displaySeq;

    @ApiModelProperty(value = "发布状态(0未发布/1已发布/2失败/3发布中/4超时/5待发布)")
    @TableField("IS_RELEASE")
    private String isRelease;

    @ApiModelProperty(value = "是否EXCEL导入(0否/1是)")
    @TableField("IS_IMPORT")
    private String isImport;

    @ApiModelProperty(value = "排产顺序")
    @TableField("PRODUCTION_SEQUENCE")
    private Long productionSequence;

    // ==================== 系统字段 ====================

    @ApiModelProperty(value = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "创建人")
    @TableField("CREATE_BY")
    private String createBy;

    @ApiModelProperty(value = "更新人")
    @TableField("UPDATE_BY")
    private String updateBy;

    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;

    // ==================== 业务方法 ====================

    /**
     * 获取日硫化量(双班次)
     */
    public Integer getDayLhQty() {
        if (this.dayVulcanizationQty == null) {
            return 0;
        }
        return this.dayVulcanizationQty * 2;
    }

    /**
     * 获取月底计划剩余量key
     */
    public String getGroupKey() {
        return String.format("%s|*|%s", this.factoryCode, this.materialDesc);
    }

    /**
     * 获取指定日期的排产量
     * @param day 日期(1-31)
     * @return 排产量
     */
    public Integer getDayQty(int day) {
        switch (day) {
            case 1: return day1;
            case 2: return day2;
            case 3: return day3;
            case 4: return day4;
            case 5: return day5;
            case 6: return day6;
            case 7: return day7;
            case 8: return day8;
            case 9: return day9;
            case 10: return day10;
            case 11: return day11;
            case 12: return day12;
            case 13: return day13;
            case 14: return day14;
            case 15: return day15;
            case 16: return day16;
            case 17: return day17;
            case 18: return day18;
            case 19: return day19;
            case 20: return day20;
            case 21: return day21;
            case 22: return day22;
            case 23: return day23;
            case 24: return day24;
            case 25: return day25;
            case 26: return day26;
            case 27: return day27;
            case 28: return day28;
            case 29: return day29;
            case 30: return day30;
            case 31: return day31;
            default: return null;
        }
    }

    /**
     * 设置指定日期的排产量
     * @param day 日期(1-31)
     * @param qty 排产量
     */
    public void setDayQty(int day, Integer qty) {
        switch (day) {
            case 1: day1 = qty; break;
            case 2: day2 = qty; break;
            case 3: day3 = qty; break;
            case 4: day4 = qty; break;
            case 5: day5 = qty; break;
            case 6: day6 = qty; break;
            case 7: day7 = qty; break;
            case 8: day8 = qty; break;
            case 9: day9 = qty; break;
            case 10: day10 = qty; break;
            case 11: day11 = qty; break;
            case 12: day12 = qty; break;
            case 13: day13 = qty; break;
            case 14: day14 = qty; break;
            case 15: day15 = qty; break;
            case 16: day16 = qty; break;
            case 17: day17 = qty; break;
            case 18: day18 = qty; break;
            case 19: day19 = qty; break;
            case 20: day20 = qty; break;
            case 21: day21 = qty; break;
            case 22: day22 = qty; break;
            case 23: day23 = qty; break;
            case 24: day24 = qty; break;
            case 25: day25 = qty; break;
            case 26: day26 = qty; break;
            case 27: day27 = qty; break;
            case 28: day28 = qty; break;
            case 29: day29 = qty; break;
            case 30: day30 = qty; break;
            case 31: day31 = qty; break;
        }
    }

    /**
     * 计算总排产量
     */
    public int calculateTotalQty() {
        int total = 0;
        for (int i = 1; i <= 31; i++) {
            Integer qty = getDayQty(i);
            if (qty != null && qty > 0) {
                total += qty;
            }
        }
        return total;
    }
}
