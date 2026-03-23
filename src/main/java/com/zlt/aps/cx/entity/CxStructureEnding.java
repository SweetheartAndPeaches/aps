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
 * 对应数据库表：T_CX_STRUCTURE_ENDING
 * 
 * 用于跟踪每个结构（菜系）的收尾进度，支持紧急收尾判断。
 *
 * @author APS Team
 */
@Data
@TableName(value = "T_CX_STRUCTURE_ENDING", keepGlobalPrefix = false)
@Schema(description = "结构收尾管理")
public class CxStructureEnding implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @Schema(description = "产品结构编码")
    @TableField("STRUCTURE_CODE")
    private String structureCode;

    @Schema(description = "结构名称")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    @Schema(description = "硫化余量（条）")
    @TableField("VULCANIZING_REMAINDER")
    private Integer vulcanizingRemainder;

    @Schema(description = "胎胚库存（条）")
    @TableField("EMBRYO_STOCK")
    private Integer embryoStock;

    @Schema(description = "成型余量 = 硫化余量 - 胎胚库存")
    @TableField("FORMING_REMAINDER")
    private Integer formingRemainder;

    @Schema(description = "当前日产能")
    @TableField("DAILY_CAPACITY")
    private Integer dailyCapacity;

    @Schema(description = "预计收尾天数")
    @TableField("ESTIMATED_ENDING_DAYS")
    private BigDecimal estimatedEndingDays;

    @Schema(description = "计划收尾日期")
    @TableField("PLANNED_ENDING_DATE")
    private LocalDate plannedEndingDate;

    @Schema(description = "是否紧急收尾（3天内）")
    @TableField("IS_URGENT_ENDING")
    private Integer isUrgentEnding;

    @Schema(description = "是否10天内收尾")
    @TableField("IS_NEAR_ENDING")
    private Integer isNearEnding;

    @Schema(description = "延误量（条）")
    @TableField("DELAY_QUANTITY")
    private Integer delayQuantity;

    @Schema(description = "平摊到未来3天的量")
    @TableField("DISTRIBUTED_QUANTITY")
    private Integer distributedQuantity;

    @Schema(description = "是否需要调整月计划")
    @TableField("NEED_MONTH_PLAN_ADJUST")
    private Integer needMonthPlanAdjust;

    @Schema(description = "统计日期")
    @TableField("STAT_DATE")
    private LocalDate statDate;

    @Schema(description = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
