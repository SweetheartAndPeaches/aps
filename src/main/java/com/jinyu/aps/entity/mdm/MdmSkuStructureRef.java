package com.jinyu.aps.entity.mdm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jinyu.aps.entity.base.ApsBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SKU与结构关系对象
 * 对应表：T_MDM_SKU_STRUCTURE_REF
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_SKU_STRUCTURE_REF")
@Schema(description = "SKU与结构关系对象")
public class MdmSkuStructureRef extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 物料编码
     */
    @Schema(description = "物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    /**
     * 结构名称
     */
    @Schema(description = "结构名称")
    @TableField("STRUCTURE_NAME")
    private String structureName;
}
