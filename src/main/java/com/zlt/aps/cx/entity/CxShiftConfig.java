package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 班次配置实体类
 * 对应数据库表：T_CX_SHIFT_CONFIG
 * 
 * 用于管理班次的动态调整配置。
 * 支持班次时间调整、产能比例配置等。
 *
 * @author APS Team
 */
@Data
@TableName(value = "T_CX_SHIFT_CONFIG", keepGlobalPrefix = false)
@Schema(description = "班次配置")
public class CxShiftConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @Schema(description = "配置日期")
    @TableField("CONFIG_DATE")
    private LocalDate configDate;

    @Schema(description = "班次编码：NIGHT(夜班)/DAY(白班)/AFTERNOON(中班)")
    @TableField("SHIFT_CODE")
    private String shiftCode;

    @Schema(description = "班次名称")
    @TableField("SHIFT_NAME")
    private String shiftName;

    @Schema(description = "班次开始时间")
    @TableField("START_TIME")
    private LocalTime startTime;

    @Schema(description = "班次结束时间")
    @TableField("END_TIME")
    private LocalTime endTime;

    @Schema(description = "标准时长（小时）")
    @TableField("STANDARD_HOURS")
    private Integer standardHours;

    @Schema(description = "调整后时长（小时）")
    @TableField("ADJUSTED_HOURS")
    private Integer adjustedHours;

    @Schema(description = "产能比例（0-100）")
    @TableField("CAPACITY_RATIO")
    private Integer capacityRatio;

    @Schema(description = "是否生效")
    @TableField("IS_ACTIVE")
    private Integer isActive;

    @Schema(description = "调整原因")
    @TableField("ADJUST_REASON")
    private String adjustReason;

    @Schema(description = "备注")
    @TableField("REMARK")
    private String remark;

    @Schema(description = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
