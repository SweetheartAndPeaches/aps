package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 成型结构硫化配比对象
 * 对应表：T_MDM_STRUCTURE_LH_RATIO
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mdm_structure_lh_ratio")
@ApiModel(value = "成型结构硫化配比对象", description = "成型结构硫化配比对象")
public class MdmStructureLhRatio extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "机型")
    @TableField("CX_MACHINE_TYPE_CODE")
    private String cxMachineTypeCode;

    @ApiModelProperty(value = "结构")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    @ApiModelProperty(value = "最大硫化机台数")
    @TableField("LH_MACHINE_MAX_QTY")
    private Integer lhMachineMaxQty;

    @ApiModelProperty(value = "最大胎胚数")
    @TableField("MAX_EMBRYO_QTY")
    private Integer maxEmbryoQty;
}
