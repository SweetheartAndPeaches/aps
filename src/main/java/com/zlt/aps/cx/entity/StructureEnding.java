package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 结构收尾管理实体类
 * 
 * 用于跟踪每个结构（菜系）的收尾进度，支持紧急收尾判断。
 *
 * @author APS Team
 */
@Data
@TableName(value = "t_cx_structure_ending", keepGlobalPrefix = false)
@Schema(description = "结构收尾管理")
public class StructureEnding implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "产品结构编码")
    @TableField("structure_code")
    private String structureCode;

    @Schema(description = "结构名称")
    @TableField("structure_name")
    private String structureName;

    @Schema(description = "硫化余量（条）")
    @TableField("vulcanizing_remainder")
    private Integer vulcanizingRemainder;

    @Schema(description = "胎胚库存（条）")
    @TableField("embryo_stock")
    private Integer embryoStock;

    @Schema(description = "成型余量 = 硫化余量 - 胎胚库存")
    @TableField("forming_remainder")
    private Integer formingRemainder;

    @Schema(description = "当前日产能")
    @TableField("daily_capacity")
    private Integer dailyCapacity;

    @Schema(description = "预计收尾天数")
    @TableField("estimated_ending_days")
    private BigDecimal estimatedEndingDays;

    @Schema(description = "计划收尾日期")
    @TableField("planned_ending_date")
    private LocalDate plannedEndingDate;

    @Schema(description = "是否紧急收尾（3天内）")
    @TableField("is_urgent_ending")
    private Integer isUrgentEnding;

    @Schema(description = "是否10天内收尾")
    @TableField("is_near_ending")
    private Integer isNearEnding;

    @Schema(description = "延误量（条）")
    @TableField("delay_quantity")
    private Integer delayQuantity;

    @Schema(description = "平摊到未来3天的量")
    @TableField("distributed_quantity")
    private Integer distributedQuantity;

    @Schema(description = "是否需要调整月计划")
    @TableField("need_month_plan_adjust")
    private Integer needMonthPlanAdjust;

    @Schema(description = "统计日期")
    @TableField("stat_date")
    private LocalDate statDate;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
