package com.jinyu.aps.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 班次配置实体类
 * 
 * 用于管理班次的动态调整配置。
 * 支持班次时间调整、产能比例配置等。
 *
 * @author APS Team
 */
@Data
@TableName(value = "t_cx_shift_config", keepGlobalPrefix = false)
@Schema(description = "班次配置")
public class ShiftConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "配置日期")
    @TableField("config_date")
    private LocalDate configDate;

    @Schema(description = "班次编码：NIGHT(夜班)/DAY(白班)/AFTERNOON(中班)")
    @TableField("shift_code")
    private String shiftCode;

    @Schema(description = "班次名称")
    @TableField("shift_name")
    private String shiftName;

    @Schema(description = "班次开始时间")
    @TableField("start_time")
    private LocalTime startTime;

    @Schema(description = "班次结束时间")
    @TableField("end_time")
    private LocalTime endTime;

    @Schema(description = "标准时长（小时）")
    @TableField("standard_hours")
    private Integer standardHours;

    @Schema(description = "调整后时长（小时）")
    @TableField("adjusted_hours")
    private Integer adjustedHours;

    @Schema(description = "产能比例（0-100）")
    @TableField("capacity_ratio")
    private Integer capacityRatio;

    @Schema(description = "是否生效")
    @TableField("is_active")
    private Integer isActive;

    @Schema(description = "调整原因")
    @TableField("adjust_reason")
    private String adjustReason;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
