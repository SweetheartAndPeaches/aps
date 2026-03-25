package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 胎胚库存实体类
 * 对应数据库表：T_CX_STOCK
 *
 * @author APS Team
 */
@Data
@TableName(value = "T_CX_STOCK", keepGlobalPrefix = false)
@ApiModel(value = "胎胚库存")
public class CxStock implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "胎胚物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    @ApiModelProperty(value = "胎胚物料名称")
    @TableField(exist = false)
    private String materialName;

    @ApiModelProperty(value = "实时库存数量")
    @TableField("CURRENT_STOCK")
    private Integer currentStock;

    @ApiModelProperty(value = "计划入库量")
    @TableField("PLANNED_IN_QTY")
    private Integer plannedInQty;

    @ApiModelProperty(value = "计划出库量")
    @TableField("PLANNED_OUT_QTY")
    private Integer plannedOutQty;

    @ApiModelProperty(value = "可用库存")
    @TableField("AVAILABLE_STOCK")
    private Integer availableStock;

    @ApiModelProperty(value = "可用硫化机台数")
    @TableField("VULCANIZE_MACHINE_COUNT")
    private Integer vulcanizeMachineCount;

    @ApiModelProperty(value = "总模数")
    @TableField("VULCANIZE_MOLD_COUNT")
    private Integer vulcanizeMoldCount;

    @ApiModelProperty(value = "库存可供硫化时长(小时)")
    @TableField("STOCK_HOURS")
    private BigDecimal stockHours;

    @ApiModelProperty(value = "计算公式记录")
    @TableField("STOCK_HOURS_FORMULA")
    private String stockHoursFormula;

    @ApiModelProperty(value = "交班剩余可供硫化时长")
    @TableField("SHIFT_END_AVAILABLE_HOURS")
    private BigDecimal shiftEndAvailableHours;

    @ApiModelProperty(value = "预警状态")
    @TableField("ALERT_STATUS")
    private String alertStatus;

    @ApiModelProperty(value = "预警触发时间")
    @TableField("ALERT_TIME")
    private LocalDateTime alertTime;

    @ApiModelProperty(value = "是否收尾SKU")
    @TableField("IS_ENDING_SKU")
    private Integer isEndingSku;

    @ApiModelProperty(value = "预计收尾日期")
    @TableField("ENDING_DATE")
    private LocalDateTime endingDate;

    @ApiModelProperty(value = "计算时间")
    @TableField("CALC_TIME")
    private LocalDateTime calcTime;

    @ApiModelProperty(value = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
