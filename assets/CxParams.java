package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 成型参数配置表 T_CX_PARAMS
 */
@ApiModel(value = "成型参数配置对象", description = "成型参数配置表")
@Data
@TableName(value = "T_CX_PARAMS")
public class CxParams extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "主键ID", name = "id")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /** 配置编码 */
    @ApiModelProperty(value = "配置编码", name = "configCode")
    @TableField(value = "CONFIG_CODE")
    private String configCode;

    /** 配置名称 */
    @ApiModelProperty(value = "配置名称", name = "configName")
    @TableField(value = "CONFIG_NAME")
    private String configName;

    /** 配置值（支持字符串） */
    @ApiModelProperty(value = "配置值", name = "configValue")
    @TableField(value = "CONFIG_VALUE")
    private String configValue;

    /** 配置类型：NUMBER/STRING/DATE/BOOLEAN */
    @ApiModelProperty(value = "配置类型", name = "configType")
    @TableField(value = "CONFIG_TYPE")
    private String configType;

    /** 单位（小时/条/百分比） */
    @ApiModelProperty(value = "单位", name = "configUnit")
    @TableField(value = "CONFIG_UNIT")
    private String configUnit;

    /** 配置说明 */
    @ApiModelProperty(value = "配置说明", name = "description")
    @TableField(value = "DESCRIPTION")
    private String description;

    /** 是否启用：0-禁用 1-启用 */
    @ApiModelProperty(value = "是否启用", name = "isActive")
    @TableField(value = "IS_ACTIVE")
    private Integer isActive;
}
