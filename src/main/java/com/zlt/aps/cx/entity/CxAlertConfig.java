package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预警配置实体类
 * 对应数据库表：T_CX_ALERT_CONFIG
 *
 * @author APS Team
 */
@Data
@TableName(value = "T_CX_ALERT_CONFIG", keepGlobalPrefix = false)
@Schema(description = "预警配置")
public class CxAlertConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @Schema(description = "配置编码")
    @TableField("CONFIG_CODE")
    private String configCode;

    @Schema(description = "配置名称")
    @TableField("CONFIG_NAME")
    private String configName;

    @Schema(description = "配置值")
    @TableField("CONFIG_VALUE")
    private String configValue;

    @Schema(description = "配置类型")
    @TableField("CONFIG_TYPE")
    private String configType;

    @Schema(description = "单位")
    @TableField("CONFIG_UNIT")
    private String configUnit;

    @Schema(description = "配置说明")
    @TableField("DESCRIPTION")
    private String description;

    @Schema(description = "是否启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;

    @Schema(description = "生效日期")
    @TableField("EFFECTIVE_DATE")
    private LocalDateTime effectiveDate;

    @Schema(description = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    @TableField("CREATE_BY")
    private String createBy;

    @Schema(description = "更新人")
    @TableField("UPDATE_BY")
    private String updateBy;
}
