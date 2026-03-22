package com.jinyu.aps.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预警配置实体类
 *
 * @author APS Team
 */
@Data
@TableName(value = "t_cx_alert_config", keepGlobalPrefix = false)
@Schema(description = "预警配置")
public class AlertConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "配置编码")
    @TableField("config_code")
    private String configCode;

    @Schema(description = "配置名称")
    @TableField("config_name")
    private String configName;

    @Schema(description = "配置值")
    @TableField("config_value")
    private String configValue;

    @Schema(description = "配置类型")
    @TableField("config_type")
    private String configType;

    @Schema(description = "单位")
    @TableField("config_unit")
    private String configUnit;

    @Schema(description = "配置说明")
    @TableField("description")
    private String description;

    @Schema(description = "是否启用")
    @TableField("is_active")
    private Integer isActive;

    @Schema(description = "生效日期")
    @TableField("effective_date")
    private LocalDateTime effectiveDate;

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
}
