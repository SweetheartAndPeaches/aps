package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 成型精度计划实体类
 * 对应数据库表：T_CX_PRECISION_PLAN
 * 
 * 用于记录成型机台的定期精度校验计划。
 * 每个机台每两个月需要做一次精度校验，每次4小时。
 *
 * @author APS Team
 */
@Data
@TableName(value = "T_CX_PRECISION_PLAN", keepGlobalPrefix = false)
@Schema(description = "成型精度计划")
public class CxPrecisionPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @Schema(description = "机台编号")
    @TableField("MACHINE_CODE")
    private String machineCode;

    @Schema(description = "机台名称")
    @TableField("MACHINE_NAME")
    private String machineName;

    @Schema(description = "计划日期")
    @TableField("PLAN_DATE")
    private LocalDate planDate;

    @Schema(description = "计划班次（DAY/MIDDLE/NIGHT）")
    @TableField("PLAN_SHIFT")
    private String planShift;

    @Schema(description = "开始时间")
    @TableField("START_TIME")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @TableField("END_TIME")
    private LocalDateTime endTime;

    @Schema(description = "时长（小时）")
    @TableField("DURATION_HOURS")
    private Integer durationHours;

    @Schema(description = "状态（PLANNED/IN_PROGRESS/COMPLETED/CANCELLED）")
    @TableField("STATUS")
    private String status;

    @Schema(description = "实际开始时间")
    @TableField("ACTUAL_START_TIME")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际结束时间")
    @TableField("ACTUAL_END_TIME")
    private LocalDateTime actualEndTime;

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
