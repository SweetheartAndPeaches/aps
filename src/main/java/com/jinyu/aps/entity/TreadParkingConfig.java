package com.jinyu.aps.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 胎面停放时间配置实体类
 * 
 * 用于管理胎面停放时间约束配置。
 * 确保胎面停放时间在合理范围内，保证产品质量。
 *
 * @author APS Team
 */
@Data
@TableName(value = "t_cx_tread_parking_config", keepGlobalPrefix = false)
@Schema(description = "胎面停放时间配置")
public class TreadParkingConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "结构编码")
    @TableField("structure_code")
    private String structureCode;

    @Schema(description = "结构名称")
    @TableField("structure_name")
    private String structureName;

    @Schema(description = "最小停放时间（小时）")
    @TableField("min_parking_hours")
    private Integer minParkingHours;

    @Schema(description = "最大停放时间（小时）")
    @TableField("max_parking_hours")
    private Integer maxParkingHours;

    @Schema(description = "最佳停放时间（小时）")
    @TableField("optimal_parking_hours")
    private Integer optimalParkingHours;

    @Schema(description = "停放温度要求（℃）- 最小值")
    @TableField("min_temperature")
    private Integer minTemperature;

    @Schema(description = "停放温度要求（℃）- 最大值")
    @TableField("max_temperature")
    private Integer maxTemperature;

    @Schema(description = "停放湿度要求（%）- 最小值")
    @TableField("min_humidity")
    private Integer minHumidity;

    @Schema(description = "停放湿度要求（%）- 最大值")
    @TableField("max_humidity")
    private Integer maxHumidity;

    @Schema(description = "预警阈值（小时，即将超过最大停放时间前预警）")
    @TableField("warning_threshold_hours")
    private Integer warningThresholdHours;

    @Schema(description = "超时处理方式：ALERT(仅提醒)/FORCE_SCHEDULE(强制排程)/ADJUST_PRIORITY(调整优先级)")
    @TableField("timeout_action")
    private String timeoutAction;

    @Schema(description = "优先级调整幅度（超时后优先级提升值）")
    @TableField("priority_boost")
    private Integer priorityBoost;

    @Schema(description = "备注说明")
    @TableField("remark")
    private String remark;

    @Schema(description = "是否启用：0-禁用，1-启用")
    @TableField("is_enabled")
    private Integer isEnabled;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
