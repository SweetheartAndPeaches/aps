package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * SKU与施工（示方书）关系对象 t_mdm_sku_construction_ref
 */
@ApiModel(value = "SKU与施工（示方书）关系对象", description = "SKU与施工（示方书）关系对象")
@Data
@TableName(value = "T_MDM_SKU_CONSTRUCTION_REF")
public class MdmSkuConstructionRef extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 物料编码 */
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** MES物料编码 */
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /** 产品状态 */
    @ApiModelProperty(value = "产品状态", name = "trialStatus")
    @TableField(value = "TRIAL_STATUS")
    private String trialStatus;

    /** 规格代号 */
    @ApiModelProperty(value = "规格代号", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /** 施工代号 */
    @ApiModelProperty(value = "施工代号", name = "constructionCode")
    @TableField(value = "CONSTRUCTION_CODE")
    private String constructionCode;

    /** 胎胚号 */
    @ApiModelProperty(value = "胎胚号", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /** 生产版本 */
    @ApiModelProperty(value = "生产版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /** 成型法 */
    @ApiModelProperty(value = "成型法", name = "mouldMethod")
    @TableField(value = "MOULD_METHOD")
    private String mouldMethod;

    /** BOM版本 */
    @ApiModelProperty(value = "BOM版本", name = "bomVersion")
    @TableField(value = "BOM_VERSION")
    private String bomVersion;

    /** 合模压力 */
    @ApiModelProperty(value = "合模压力", name = "mouldClampingPressure")
    @TableField(value = "MOULD_CLAMPING_PRESSURE")
    private BigDecimal mouldClampingPressure;

    /** 模具型腔 */
    @ApiModelProperty(value = "模具型腔", name = "mouldCavity")
    @TableField(value = "MOULD_CAVITY")
    private String mouldCavity;

    /** 是否零度材料 */
    @ApiModelProperty(value = "是否零度材料", name = "isZeroRack")
    @TableField(value = "IS_ZERO_RACK")
    private String isZeroRack;

    /** 夏季机械硫化时间(分) */
    @ApiModelProperty(value = "夏季机械硫化时间(分)", name = "curingTime")
    @TableField(value = "CURING_TIME")
    private Integer curingTime;

    /** 夏季液压硫化时间(分) */
    @ApiModelProperty(value = "夏季液压硫化时间(分)", name = "hydraulicPressureCuringTime")
    @TableField(value = "HY_PRESSURE_CURING_TIME")
    private Integer hydraulicPressureCuringTime;

    /** 冬季机械硫化时间(分) */
    @ApiModelProperty(value = "冬季机械硫化时间(分)", name = "curingTime2")
    @TableField(value = "CURING_TIME2")
    private Integer curingTime2;

    /** 冬季液压硫化时间(分) */
    @ApiModelProperty(value = "冬季液压硫化时间(分)", name = "hydraulicPressureCuringTime2")
    @TableField(value = "HY_PRESSURE_CURING_TIME2")
    private Integer hydraulicPressureCuringTime2;

    /** 制造示方书号 */
    @ApiModelProperty(value = "制造示方书号", name = "embryoNo")
    @TableField(value = "EMBRYO_NO")
    private String embryoNo;

    /** 制造示方书类型 */
    @ApiModelProperty(value = "制造示方书类型", name = "embryoType")
    @TableField(value = "EMBRYO_TYPE")
    private String embryoType;

    /** 制造示方书发行时间 */
    @ApiModelProperty(value = "制造示方书发行时间", name = "embryoReleaseDate")
    @TableField(value = "EMBRYO_RELEASE_DATE")
    private String embryoReleaseDate;

    /** 文字示方书号 */
    @ApiModelProperty(value = "文字示方书号", name = "textNo")
    @TableField(value = "TEXT_NO")
    private String textNo;

    /** 文字示方书类型 */
    @ApiModelProperty(value = "文字示方书类型", name = "textType")
    @TableField(value = "TEXT_TYPE")
    private String textType;

    /** 文字示方书发行时间 */
    @ApiModelProperty(value = "文字示方书发行时间", name = "textReleaseDate")
    @TableField(value = "TEXT_RELEASE_DATE")
    private String textReleaseDate;

    /** 硫化示方书号 */
    @ApiModelProperty(value = "硫化示方书号", name = "lhNo")
    @TableField(value = "LH_NO")
    private String lhNo;

    /** 硫化示方书类型 */
    @ApiModelProperty(value = "硫化示方书类型", name = "lhType")
    @TableField(value = "LH_TYPE")
    private String lhType;

    /** 硫化示方书发行时间 */
    @ApiModelProperty(value = "硫化示方书发行时间", name = "lhReleaseDate")
    @TableField(value = "LH_RELEASE_DATE")
    private String lhReleaseDate;

    /** 主物料(胎胚号) */
    @ApiModelProperty(value = "主物料(胎胚号)", name = "mainMaterialDesc")
    @TableField(value = "MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;
}
