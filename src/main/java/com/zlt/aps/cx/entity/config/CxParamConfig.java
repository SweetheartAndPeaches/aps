package com.zlt.aps.cx.entity.config;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 排程参数配置表
 * 对应表：T_CX_PARAM_CONFIG
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_CX_PARAM_CONFIG")
@ApiModel(value = "排程参数配置对象", description = "排程参数配置表")
public class CxParamConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "参数编码")
    @TableField("PARAM_CODE")
    private String paramCode;

    @ApiModelProperty(value = "参数名称")
    @TableField("PARAM_NAME")
    private String paramName;

    @ApiModelProperty(value = "参数值")
    @TableField("PARAM_VALUE")
    private String paramValue;

    @ApiModelProperty(value = "是否启用：0-禁用 1-启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;

    @ApiModelProperty(value = "正则表达式校验")
    @TableField("REGULAR_EXPRESSION")
    private String regularExpression;

    @ApiModelProperty(value = "校验错误提示")
    @TableField("ERROR_TIPS")
    private String errorTips;
}
