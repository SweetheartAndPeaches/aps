package com.zlt.aps.cx.entity.config;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 关键产品配置实体
 * 
 * 定义关键产品列表，用于开产首班排除等场景判断
 *
 * @author APS Team
 */
@Data
@TableName("T_CX_KEY_PRODUCT")
@ApiModel(value = "关键产品配置")
public class CxKeyProduct extends BaseEntity {

    private static final long serialVersionUID = 1L;

  @ApiModelProperty(value = "胎胚编码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    @ApiModelProperty(value = "结构名称")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    @ApiModelProperty(value = "是否启用：0-禁用 1-启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;
}
