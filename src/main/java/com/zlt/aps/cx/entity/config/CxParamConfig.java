package com.zlt.aps.cx.entity.config;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
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
@TableName("cx_param_config")
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

    @ApiModelProperty(value = "参数类型：STRING-字符串 NUMBER-数字 BOOLEAN-布尔")
    @TableField("PARAM_TYPE")
    private String paramType;

    @ApiModelProperty(value = "参数描述")
    @TableField("PARAM_DESC")
    private String paramDesc;

    @ApiModelProperty(value = "分组编码")
    @TableField("GROUP_CODE")
    private String groupCode;

    @ApiModelProperty(value = "分组名称")
    @TableField("GROUP_NAME")
    private String groupName;

    @ApiModelProperty(value = "排序号")
    @TableField("SORT_ORDER")
    private Integer sortOrder;

    @ApiModelProperty(value = "是否启用：0-禁用 1-启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;
}
