package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 成型精度计划实体类
 * 
 * 用于记录成型机台的定期精度校验计划。
 * 每个机台每两个月需要做一次精度校验，每次4小时。
 *
 * @author APS Team
 */
@Data
@TableName(value = "t_cx_precision_plan", keepGlobalPrefix = false)
@Schema(description = "成型精度计划")
public class PrecisionPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "机台编号")
    @TableField("machine_code")
    private String machineCode;

    @Schema(description = "机台名称")
    @TableField("machine_name")
    private String machineName;

    @Schema(description = "计划日期")
    @TableField("plan_date")
    private LocalDate planDate;

    @Schema(description = "计划班次（DAY/MIDDLE/NIGHT）")
    @TableField("plan_shift")
    private String planShift;

    @Schema(description = "开始时间")
    @TableField("start_time")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @TableField("end_time")
    private LocalDateTime endTime;

    @Schema(description = "时长（小时）")
    @TableField("duration_hours")
    private Integer durationHours;

    @Schema(description = "状态（PLANNED/IN_PROGRESS/COMPLETED/CANCELLED）")
    @TableField("status")
    private String status;

    @Schema(description = "实际开始时间")
    @TableField("actual_start_time")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际结束时间")
    @TableField("actual_end_time")
    private LocalDateTime actualEndTime;

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
