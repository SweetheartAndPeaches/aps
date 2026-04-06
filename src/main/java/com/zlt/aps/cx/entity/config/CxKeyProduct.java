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
@TableName("cx_key_product")
@ApiModel(value = "关键产品配置")
public class CxKeyProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "胎胚编码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    @ApiModelProperty(value = "胎胚名称")
    @TableField("EMBRYO_NAME")
    private String embryoName;

    @ApiModelProperty(value = "结构编码")
    @TableField("STRUCTURE_CODE")
    private String structureCode;

    @ApiModelProperty(value = "结构名称")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    @ApiModelProperty(value = "优先级")
    @TableField("PRIORITY")
    private Integer priority;

    @ApiModelProperty(value = "是否启用：0-禁用 1-启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;

    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
