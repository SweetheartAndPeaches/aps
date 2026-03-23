package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalTime;

/**
 * 班次配置表 T_CX_SHIFT_CONFIG
 */
@ApiModel(value = "班次配置对象", description = "班次配置表")
@Data
@TableName(value = "T_CX_SHIFT_CONFIG")
public class CxShiftConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "主键ID", name = "id")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /** 班次编码：NIGHT-夜班/DAY-早班/AFTERNOON-中班 */
    @ApiModelProperty(value = "班次编码", name = "shiftCode")
    @TableField(value = "SHIFT_CODE")
    private String shiftCode;

    /** 班次名称 */
    @ApiModelProperty(value = "班次名称", name = "shiftName")
    @TableField(value = "SHIFT_NAME")
    private String shiftName;

    /** 开始时间 */
    @ApiModelProperty(value = "开始时间", name = "startTime")
    @TableField(value = "START_TIME")
    private LocalTime startTime;

    /** 结束时间 */
    @ApiModelProperty(value = "结束时间", name = "endTime")
    @TableField(value = "END_TIME")
    private LocalTime endTime;

    /** 排序（用于排班顺序） */
    @ApiModelProperty(value = "排序", name = "sortOrder")
    @TableField(value = "SORT_ORDER")
    private Integer sortOrder;

    /** 是否启用 */
    @ApiModelProperty(value = "是否启用", name = "isActive")
    @TableField(value = "IS_ACTIVE")
    private Integer isActive;
}
