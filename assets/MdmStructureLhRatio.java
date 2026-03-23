package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 成型结构硫化配比对象 t_mdm_structure_lh_ratio
 */
@ApiModel(value = "成型结构硫化配比对象", description = "成型结构硫化配比对象")
@Data
@TableName(value = "T_MDM_STRUCTURE_LH_RATIO")
public class MdmStructureLhRatio extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 机型 */
    @ApiModelProperty(value = "机型", name = "cxMachineTypeCode")
    @TableField(value = "CX_MACHINE_TYPE_CODE")
    private String cxMachineTypeCode;

    /** 结构 */
    @ApiModelProperty(value = "结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /** 最大硫化机台数 */
    @ApiModelProperty(value = "最大硫化机台数", name = "lhMachineMaxQty")
    @TableField(value = "LH_MACHINE_MAX_QTY")
    private Integer lhMachineMaxQty;

    /** 最大胎胚数 */
    @ApiModelProperty(value = "最大胎胚数", name = "maxEmbryoQty")
    @TableField(value = "MAX_EMBRYO_QTY")
    private Integer maxEmbryoQty;

    /** 备注 */
    @ApiModelProperty("备注")
    @TableField(value = "REMARK")
    private String remark;
}
