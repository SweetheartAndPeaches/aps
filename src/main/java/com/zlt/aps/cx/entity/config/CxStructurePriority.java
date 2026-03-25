package com.zlt.aps.cx.entity.config;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.cx.entity.base.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 结构优先级配置表
 * 对应表：T_CX_STRUCTURE_PRIORITY
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_CX_STRUCTURE_PRIORITY")
@ApiModel(value = "结构优先级配置对象", description = "结构优先级配置表")
public class CxStructurePriority extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "结构名称")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    @ApiModelProperty(value = "优先级等级：数值越大优先级越高")
    @TableField("PRIORITY_LEVEL")
    private Integer priorityLevel;

    @ApiModelProperty(value = "机型编码")
    @TableField("CX_MACHINE_TYPE_CODE")
    private String cxMachineTypeCode;

    @ApiModelProperty(value = "是否启用：0-禁用 1-启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;

    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;
}
