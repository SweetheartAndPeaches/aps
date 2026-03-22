package com.jinyu.aps.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排程主表实体类
 *
 * @author APS Team
 */
@Data
@TableName(value = "t_cx_schedule_main", keepGlobalPrefix = false)
@Schema(description = "排程主表")
public class ScheduleMain implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "排程单号")
    @TableField("schedule_code")
    private String scheduleCode;

    @Schema(description = "计划日期")
    @TableField("schedule_date")
    private LocalDate scheduleDate;

    @Schema(description = "排程类型")
    @TableField("schedule_type")
    private String scheduleType;

    @Schema(description = "状态")
    @TableField("status")
    private String status;

    @Schema(description = "参与排程机台数")
    @TableField("total_machines")
    private Integer totalMachines;

    @Schema(description = "总计划量")
    @TableField("total_quantity")
    private Integer totalQuantity;

    @Schema(description = "总车次数")
    @TableField("total_vehicles")
    private Integer totalVehicles;

    @Schema(description = "版本号")
    @TableField("version")
    private Integer version;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    @TableField("create_by")
    private String createBy;

    @Schema(description = "更新人")
    @TableField("update_by")
    private String updateBy;

    @Schema(description = "确认时间")
    @TableField("confirm_time")
    private LocalDateTime confirmTime;

    @Schema(description = "确认人")
    @TableField("confirm_by")
    private String confirmBy;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
