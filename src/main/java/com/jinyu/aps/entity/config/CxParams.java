package com.jinyu.aps.entity.config;

import com.baomidou.mybatisplus.annotation.*;
import com.jinyu.aps.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 成型参数配置表
 * 对应表：T_CX_PARAMS
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_CX_PARAMS")
@Schema(description = "成型参数配置对象")
public class CxParams extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 配置编码
     */
    @Schema(description = "配置编码")
    @TableField("CONFIG_CODE")
    private String configCode;

    /**
     * 配置名称
     */
    @Schema(description = "配置名称")
    @TableField("CONFIG_NAME")
    private String configName;

    /**
     * 配置值（支持字符串）
     */
    @Schema(description = "配置值")
    @TableField("CONFIG_VALUE")
    private String configValue;

    /**
     * 配置类型：NUMBER/STRING/DATE/BOOLEAN
     */
    @Schema(description = "配置类型：NUMBER/STRING/DATE/BOOLEAN")
    @TableField("CONFIG_TYPE")
    private String configType;

    /**
     * 单位（小时/条/百分比）
     */
    @Schema(description = "单位（小时/条/百分比）")
    @TableField("CONFIG_UNIT")
    private String configUnit;

    /**
     * 配置说明
     */
    @Schema(description = "配置说明")
    @TableField("DESCRIPTION")
    private String description;

    /**
     * 是否启用：0-禁用 1-启用
     */
    @Schema(description = "是否启用：0-禁用 1-启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;
}
