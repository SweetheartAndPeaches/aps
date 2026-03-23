package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 节假日配置实体类
 * 
 * 用于管理节假日开产/停产配置。
 * 支持设置节假日的生产状态、调整产能比例等。
 *
 * @author APS Team
 */
@Data
@TableName(value = "t_cx_holiday_config", keepGlobalPrefix = false)
@Schema(description = "节假日配置")
public class HolidayConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "假期日期")
    @TableField("holiday_date")
    private LocalDate holidayDate;

    @Schema(description = "假期名称（如：春节、国庆等）")
    @TableField("holiday_name")
    private String holidayName;

    @Schema(description = "假期类型：HOLIDAY(法定假日)/ADJUSTED(调休)/SPECIAL(特殊安排)")
    @TableField("holiday_type")
    private String holidayType;

    @Schema(description = "生产状态：PRODUCTION(正常生产)/REDUCED(减产)/STOPPED(停产)")
    @TableField("production_status")
    private String productionStatus;

    @Schema(description = "产能比例（0-100，0表示停产，50表示减半，100表示正常）")
    @TableField("capacity_ratio")
    private Integer capacityRatio;

    @Schema(description = "生效班次（多个班次用逗号分隔，如：DAY,AFTERNOON）")
    @TableField("effective_shifts")
    private String effectiveShifts;

    @Schema(description = "适用机台（多个机台用逗号分隔，为空表示全部）")
    @TableField("applicable_machines")
    private String applicableMachines;

    @Schema(description = "备注说明")
    @TableField("remark")
    private String remark;

    @Schema(description = "状态（ENABLED/DISABLED）")
    @TableField("status")
    private String status;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
