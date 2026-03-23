package com.zlt.aps.cx.entity.config;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.cx.entity.base.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 班次配置表
 * 对应表：T_CX_SHIFT_CONFIG
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_CX_SHIFT_CONFIG")
@ApiModel(value = "班次配置对象", description = "班次配置表")
public class CxShiftConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "班次编码")
    @TableField("SHIFT_CODE")
    private String shiftCode;

    @ApiModelProperty(value = "班次名称")
    @TableField("SHIFT_NAME")
    private String shiftName;

    @ApiModelProperty(value = "班次序号")
    @TableField("SHIFT_ORDER")
    private Integer shiftOrder;

    @ApiModelProperty(value = "开始时间（HH:mm:ss格式）")
    @TableField("START_TIME")
    private String startTime;

    @ApiModelProperty(value = "结束时间（HH:mm:ss格式）")
    @TableField("END_TIME")
    private String endTime;

    @ApiModelProperty(value = "班次时长（小时）")
    @TableField("SHIFT_HOURS")
    private Integer shiftHours;

    @ApiModelProperty(value = "是否跨天：0-否 1-是")
    @TableField("IS_CROSS_DAY")
    private Integer isCrossDay;

    @ApiModelProperty(value = "是否启用：0-禁用 1-启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;

    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;
}
