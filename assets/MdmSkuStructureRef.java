package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * SKU与结构关系对象 t_mdm_sku_structure_ref
 */
@ApiModel(value = "SKU与结构关系对象", description = "SKU与结构关系对象")
@Data
@TableName(value = "T_MDM_SKU_STRUCTURE_REF")
public class MdmSkuStructureRef extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 物料编码 */
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 主物料(胎胚描述) */
    @ApiModelProperty(value = "主物料(胎胚描述)", name = "mainMaterialDesc")
    @TableField(value = "MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    /** 结构 */
    @ApiModelProperty(value = "结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /** 物料描述（非数据库字段） */
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(exist = false)
    private String materialDesc;
}
