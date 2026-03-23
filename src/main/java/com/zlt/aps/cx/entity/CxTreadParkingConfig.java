package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 胎面停放时间配置实体类
 * 对应数据库表：T_CX_TREAD_PARKING_CONFIG
 * 
 * 用于管理胎面停放时间约束配置。
 * 确保胎面停放时间在合理范围内，保证产品质量。
 *
 * @author APS Team
 */
@Data
@TableName(value = "T_CX_TREAD_PARKING_CONFIG", keepGlobalPrefix = false)
@Schema(description = "胎面停放时间配置")
public class CxTreadParkingConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @Schema(description = "结构编码")
    @TableField("STRUCTURE_CODE")
    private String structureCode;

    @Schema(description = "结构名称")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    @Schema(description = "最小停放时间（小时）")
    @TableField("MIN_PARKING_HOURS")
    private Integer minParkingHours;

    @Schema(description = "最大停放时间（小时）")
    @TableField("MAX_PARKING_HOURS")
    private Integer maxParkingHours;

    @Schema(description = "最佳停放时间（小时）")
    @TableField("OPTIMAL_PARKING_HOURS")
    private Integer optimalParkingHours;

    @Schema(description = "停放温度要求（℃）- 最小值")
    @TableField("MIN_TEMPERATURE")
    private Integer minTemperature;

    @Schema(description = "停放温度要求（℃）- 最大值")
    @TableField("MAX_TEMPERATURE")
    private Integer maxTemperature;

    @Schema(description = "停放湿度要求（%）- 最小值")
    @TableField("MIN_HUMIDITY")
    private Integer minHumidity;

    @Schema(description = "停放湿度要求（%）- 最大值")
    @TableField("MAX_HUMIDITY")
    private Integer maxHumidity;

    @Schema(description = "预警阈值（小时，即将超过最大停放时间前预警）")
    @TableField("WARNING_THRESHOLD_HOURS")
    private Integer warningThresholdHours;

    @Schema(description = "超时处理方式：ALERT(仅提醒)/FORCE_SCHEDULE(强制排程)/ADJUST_PRIORITY(调整优先级)")
    @TableField("TIMEOUT_ACTION")
    private String timeoutAction;

    @Schema(description = "优先级调整幅度（超时后优先级提升值）")
    @TableField("PRIORITY_BOOST")
    private Integer priorityBoost;

    @Schema(description = "备注说明")
    @TableField("REMARK")
    private String remark;

    @Schema(description = "是否启用：0-禁用，1-启用")
    @TableField("IS_ENABLED")
    private Integer isEnabled;

    @Schema(description = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
