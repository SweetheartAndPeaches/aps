package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 结构信息对象 t_mdm_structure_name
 */
@ApiModel(value = "结构信息对象", description = "结构信息对象")
@Data
@TableName(value = "T_MDM_STRUCTURE_NAME")
public class MdmStructureName extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 结构 */
    @ApiModelProperty(value = "结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;
}
