package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.aps.cx.entity.base.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 结构名称字典对象
 * 对应表：T_MDM_STRUCTURE_NAME
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_STRUCTURE_NAME")
@ApiModel(value = "结构名称字典对象", description = "结构名称字典对象")
public class MdmStructureName extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "结构编码")
    @TableField("STRUCTURE_CODE")
    private String structureCode;

    @ApiModelProperty(value = "结构名称")
    @TableField("STRUCTURE_NAME")
    private String structureName;
}
