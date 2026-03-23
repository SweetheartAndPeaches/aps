package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工厂月生产计划-最终排产计划定稿实体类
 * 对应数据库表：t_mp_month_plan_prod_final
 * 
 * 数据来源：ERP/MES系统月度生产计划
 * 用途：作为APS日排程的数据输入源
 *
 * @author APS Team
 */
@Data
@TableName(value = "t_mp_month_plan_prod_final", keepGlobalPrefix = false)
@Schema(description = "工厂月生产计划-最终排产计划定稿")
public class MonthPlanFinal implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 主键与基本信息 ====================

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "工单号(MP+年月日+批次号+5位流水)")
    @TableField("production_no")
    private String productionNo;

    @Schema(description = "工厂编码")
    @TableField("factory_code")
    private String factoryCode;

    @Schema(description = "年份")
    @TableField("year")
    private Integer year;

    @Schema(description = "月份")
    @TableField("month")
    private Integer month;

    @Schema(description = "年月(YYYYMM)")
    @TableField("year_month")
    private Integer yearMonth;

    // ==================== 版本信息 ====================

    @Schema(description = "销售生产需求计划版本")
    @TableField("month_plan_version")
    private String monthPlanVersion;

    @Schema(description = "最新需求计划版本")
    @TableField("last_month_plan_version")
    private String lastMonthPlanVersion;

    @Schema(description = "排产计划版本")
    @TableField("production_version")
    private String productionVersion;

    // ==================== 产品信息 ====================

    @Schema(description = "产品品类(TBR全钢/PCR半钢)")
    @TableField("product_type_code")
    private String productTypeCode;

    @Schema(description = "物料编码")
    @TableField("material_code")
    private String materialCode;

    @Schema(description = "物料描述")
    @TableField("material_desc")
    private String materialDesc;

    @Schema(description = "MES物料编码")
    @TableField("mes_material_code")
    private String mesMaterialCode;

    @Schema(description = "产品结构")
    @TableField("structure_name")
    private String structureName;

    @Schema(description = "英寸")
    @TableField("pro_size")
    private String proSize;

    @Schema(description = "产品分类")
    @TableField("product_category")
    private String productCategory;

    @Schema(description = "产品状态")
    @TableField("product_status")
    private String productStatus;

    @Schema(description = "结构类型(01周期结构/02常规结构)")
    @TableField("structure_type")
    private String structureType;

    @Schema(description = "排产分类")
    @TableField("production_type")
    private String productionType;

    // ==================== 胎胚与施工信息 ====================

    @Schema(description = "生胎代码")
    @TableField("embryo_code")
    private String embryoCode;

    @Schema(description = "主物料(胎胚号)")
    @TableField("main_material_desc")
    private String mainMaterialDesc;

    @Schema(description = "施工阶段(00无工艺/01试制/02量试/03正式)")
    @TableField("construction_stage")
    private String constructionStage;

    @Schema(description = "是否零度材料")
    @TableField("is_zero_rack")
    private String isZeroRack;

    @Schema(description = "制造示方书号")
    @TableField("embryo_no")
    private String embryoNo;

    @Schema(description = "文字示方书号")
    @TableField("text_no")
    private String textNo;

    @Schema(description = "硫化示方书号")
    @TableField("lh_no")
    private String lhNo;

    // ==================== 产品属性 ====================

    @Schema(description = "品牌")
    @TableField("brand")
    private String brand;

    @Schema(description = "规格")
    @TableField("specifications")
    private String specifications;

    @Schema(description = "主花纹")
    @TableField("main_pattern")
    private String mainPattern;

    @Schema(description = "花纹")
    @TableField("pattern")
    private String pattern;

    @Schema(description = "型腔数量(同主花纹模具数量)")
    @TableField("mould_cavity_qty")
    private Integer mouldCavityQty;

    @Schema(description = "活块数量(同主花纹物料模具数量)")
    @TableField("type_block_qty")
    private Integer typeBlockQty;

    @Schema(description = "高优先级数量")
    @TableField("height_qty")
    private Integer heightQty;

    @Schema(description = "月均销量")
    @TableField("average_sale_qty")
    private Integer averageSaleQty;

    @Schema(description = "库销比")
    @TableField("inventory_sales_ratio")
    private BigDecimal inventorySalesRatio;

    // ==================== 产能与机台信息 ====================

    @Schema(description = "日硫化量")
    @TableField("day_vulcanization_qty")
    private Integer dayVulcanizationQty;

    @Schema(description = "成型机台信息(多个逗号分隔)")
    @TableField("cx_machine_code")
    private String cxMachineCode;

    @Schema(description = "模具使用变化信息(如2-4-2)")
    @TableField("mould_change_info")
    private String mouldChangeInfo;

    @Schema(description = "动平衡数量")
    @TableField("dynamic_balance_qty")
    private String dynamicBalanceQty;

    @Schema(description = "均匀性数量")
    @TableField("uniformity_qty")
    private Integer uniformityQty;

    @Schema(description = "单条硫化时间(分钟)")
    @TableField("curing_time")
    private Integer curingTime;

    // ==================== 需求与排产数量 ====================

    @Schema(description = "生产需求计划(净需求)")
    @TableField("prod_req_plan")
    private Integer prodReqPlan;

    @Schema(description = "试制量试计划需求量")
    @TableField("trial_qty")
    private Integer trialQty;

    @Schema(description = "高优先级排产数量")
    @TableField("height_production_qty")
    private Integer heightProductionQty;

    @Schema(description = "实际生产需求(含损耗)")
    @TableField("fact_prod_req_qty")
    private Integer factProdReqQty;

    @Schema(description = "生产实际排产量")
    @TableField("total_qty")
    private Integer totalQty;

    @Schema(description = "中优先级排产数量")
    @TableField("mid_production_qty")
    private Integer midProductionQty;

    @Schema(description = "周期排产储备排产数量")
    @TableField("cycle_production_qty")
    private Integer cycleProductionQty;

    @Schema(description = "常规储备排产数量")
    @TableField("convention_production_qty")
    private Integer conventionProductionQty;

    @Schema(description = "暂缓订单排产数量")
    @TableField("postpone_production_qty")
    private Integer postponeProductionQty;

    @Schema(description = "试制量试排产量")
    @TableField("trial_production_qty")
    private Integer trialProductionQty;

    @Schema(description = "差异量(未排产数量)")
    @TableField("difference_qty")
    private Integer differenceQty;

    // ==================== 周调整量 ====================

    @Schema(description = "第1周调整量")
    @TableField("adjust_qty1")
    private Integer adjustQty1;

    @Schema(description = "第2周调整量")
    @TableField("adjust_qty2")
    private Integer adjustQty2;

    @Schema(description = "第3周调整量")
    @TableField("adjust_qty3")
    private Integer adjustQty3;

    @Schema(description = "第4周调整量")
    @TableField("adjust_qty4")
    private Integer adjustQty4;

    @Schema(description = "未排产原因")
    @TableField("reason")
    private String reason;

    // ==================== 月度每日排产计划 ====================

    @Schema(description = "开始日期(1-31)")
    @TableField("begin_day")
    private Integer beginDay;

    @Schema(description = "结束日期(1-31)")
    @TableField("end_day")
    private Integer endDay;

    @Schema(description = "第1天排产量")
    @TableField("day_1")
    private Integer day1;

    @Schema(description = "第2天排产量")
    @TableField("day_2")
    private Integer day2;

    @Schema(description = "第3天排产量")
    @TableField("day_3")
    private Integer day3;

    @Schema(description = "第4天排产量")
    @TableField("day_4")
    private Integer day4;

    @Schema(description = "第5天排产量")
    @TableField("day_5")
    private Integer day5;

    @Schema(description = "第6天排产量")
    @TableField("day_6")
    private Integer day6;

    @Schema(description = "第7天排产量")
    @TableField("day_7")
    private Integer day7;

    @Schema(description = "第8天排产量")
    @TableField("day_8")
    private Integer day8;

    @Schema(description = "第9天排产量")
    @TableField("day_9")
    private Integer day9;

    @Schema(description = "第10天排产量")
    @TableField("day_10")
    private Integer day10;

    @Schema(description = "第11天排产量")
    @TableField("day_11")
    private Integer day11;

    @Schema(description = "第12天排产量")
    @TableField("day_12")
    private Integer day12;

    @Schema(description = "第13天排产量")
    @TableField("day_13")
    private Integer day13;

    @Schema(description = "第14天排产量")
    @TableField("day_14")
    private Integer day14;

    @Schema(description = "第15天排产量")
    @TableField("day_15")
    private Integer day15;

    @Schema(description = "第16天排产量")
    @TableField("day_16")
    private Integer day16;

    @Schema(description = "第17天排产量")
    @TableField("day_17")
    private Integer day17;

    @Schema(description = "第18天排产量")
    @TableField("day_18")
    private Integer day18;

    @Schema(description = "第19天排产量")
    @TableField("day_19")
    private Integer day19;

    @Schema(description = "第20天排产量")
    @TableField("day_20")
    private Integer day20;

    @Schema(description = "第21天排产量")
    @TableField("day_21")
    private Integer day21;

    @Schema(description = "第22天排产量")
    @TableField("day_22")
    private Integer day22;

    @Schema(description = "第23天排产量")
    @TableField("day_23")
    private Integer day23;

    @Schema(description = "第24天排产量")
    @TableField("day_24")
    private Integer day24;

    @Schema(description = "第25天排产量")
    @TableField("day_25")
    private Integer day25;

    @Schema(description = "第26天排产量")
    @TableField("day_26")
    private Integer day26;

    @Schema(description = "第27天排产量")
    @TableField("day_27")
    private Integer day27;

    @Schema(description = "第28天排产量")
    @TableField("day_28")
    private Integer day28;

    @Schema(description = "第29天排产量")
    @TableField("day_29")
    private Integer day29;

    @Schema(description = "第30天排产量")
    @TableField("day_30")
    private Integer day30;

    @Schema(description = "第31天排产量")
    @TableField("day_31")
    private Integer day31;

    // ==================== 其他信息 ====================

    @Schema(description = "硫化总工时")
    @TableField("total_vulcanization_minutes")
    private BigDecimal totalVulcanizationMinutes;

    @Schema(description = "显示顺序")
    @TableField("display_seq")
    private Integer displaySeq;

    @Schema(description = "发布状态(0未发布/1已发布/2失败/3发布中/4超时/5待发布)")
    @TableField("is_release")
    private String isRelease;

    @Schema(description = "是否EXCEL导入(0否/1是)")
    @TableField("is_import")
    private String isImport;

    @Schema(description = "排产顺序")
    @TableField("production_sequence")
    private Long productionSequence;

    // ==================== 系统字段 ====================

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    @TableField("create_by")
    private String createBy;

    @Schema(description = "更新人")
    @TableField("update_by")
    private String updateBy;

    @Schema(description = "备注")
    @TableField("remark")
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
