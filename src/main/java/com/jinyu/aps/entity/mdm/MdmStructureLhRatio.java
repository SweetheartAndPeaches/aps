package com.jinyu.aps.entity.mdm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jinyu.aps.entity.base.ApsBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
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
@TableName("T_MDM_STRUCTURE_LH_RATIO")
@Schema(description = "成型结构硫化配比对象")
public class MdmStructureLhRatio extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 机型
     */
    @Schema(description = "机型")
    @TableField("CX_MACHINE_TYPE_CODE")
    private String cxMachineTypeCode;

    /**
     * 结构
     */
    @Schema(description = "结构")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    /**
     * 最大硫化机台数
     */
    @Schema(description = "最大硫化机台数")
    @TableField("LH_MACHINE_MAX_QTY")
    private Integer lhMachineMaxQty;

    /**
     * 最大胎胚数
     */
    @Schema(description = "最大胎胚数")
    @TableField("MAX_EMBRYO_QTY")
    private Integer maxEmbryoQty;
}
