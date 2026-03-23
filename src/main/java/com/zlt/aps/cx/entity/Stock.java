package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 胎胚库存实体类
 *
 * @author APS Team
 */
@Data
@TableName(value = "t_cx_stock", keepGlobalPrefix = false)
@Schema(description = "胎胚库存")
public class Stock implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "胎胚物料编码")
    @TableField("material_code")
    private String materialCode;

    @Schema(description = "实时库存数量")
    @TableField("current_stock")
    private Integer currentStock;

    @Schema(description = "计划入库量")
    @TableField("planned_in_qty")
    private Integer plannedInQty;

    @Schema(description = "计划出库量")
    @TableField("planned_out_qty")
    private Integer plannedOutQty;

    @Schema(description = "可用库存")
    @TableField("available_stock")
    private Integer availableStock;

    @Schema(description = "可用硫化机台数")
    @TableField("vulcanize_machine_count")
    private Integer vulcanizeMachineCount;

    @Schema(description = "总模数")
    @TableField("vulcanize_mold_count")
    private Integer vulcanizeMoldCount;

    @Schema(description = "库存可供硫化时长(小时)")
    @TableField("stock_hours")
    private BigDecimal stockHours;

    @Schema(description = "计算公式记录")
    @TableField("stock_hours_formula")
    private String stockHoursFormula;

    @Schema(description = "交班剩余可供硫化时长")
    @TableField("shift_end_available_hours")
    private BigDecimal shiftEndAvailableHours;

    @Schema(description = "预警状态")
    @TableField("alert_status")
    private String alertStatus;

    @Schema(description = "预警触发时间")
    @TableField("alert_time")
    private LocalDateTime alertTime;

    @Schema(description = "是否收尾SKU")
    @TableField("is_ending_sku")
    private Integer isEndingSku;

    @Schema(description = "预计收尾日期")
    @TableField("ending_date")
    private LocalDateTime endingDate;

    @Schema(description = "计算时间")
    @TableField("calc_time")
    private LocalDateTime calcTime;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
