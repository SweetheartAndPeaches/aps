package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.aps.cx.entity.base.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * SKU与施工（示方书）关系对象
 * 对应表：T_MDM_SKU_CONSTRUCTION_REF
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_SKU_CONSTRUCTION_REF")
@ApiModel(value = "SKU与施工关系对象", description = "SKU与施工（示方书）关系对象")
public class MdmSkuConstructionRef extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    @ApiModelProperty(value = "MES物料编码")
    @TableField("MES_MATERIAL_CODE")
    private String mesMaterialCode;

    @ApiModelProperty(value = "产品状态")
    @TableField("TRIAL_STATUS")
    private String trialStatus;

    @ApiModelProperty(value = "规格代号")
    @TableField("SPEC_CODE")
    private String specCode;

    @ApiModelProperty(value = "施工代号")
    @TableField("CONSTRUCTION_CODE")
    private String constructionCode;

    @ApiModelProperty(value = "胎胚号")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    @ApiModelProperty(value = "生产版本")
    @TableField("PRODUCTION_VERSION")
    private String productionVersion;

    @ApiModelProperty(value = "成型法")
    @TableField("MOULD_METHOD")
    private String mouldMethod;

    @ApiModelProperty(value = "BOM版本")
    @TableField("BOM_VERSION")
    private String bomVersion;

    @ApiModelProperty(value = "合模压力")
    @TableField("MOULD_CLAMPING_PRESSURE")
    private BigDecimal mouldClampingPressure;

    @ApiModelProperty(value = "模具型腔")
    @TableField("MOULD_CAVITY")
    private String mouldCavity;

    @ApiModelProperty(value = "是否零度材料")
    @TableField("IS_ZERO_RACK")
    private String isZeroRack;

    @ApiModelProperty(value = "夏季机械硫化时间(分)")
    @TableField("CURING_TIME")
    private Integer curingTime;

    @ApiModelProperty(value = "夏季液压硫化时间(分)")
    @TableField("HY_PRESSURE_CURING_TIME")
    private Integer hydraulicPressureCuringTime;

    @ApiModelProperty(value = "冬季机械硫化时间(分)")
    @TableField("CURING_TIME2")
    private Integer curingTime2;

    @ApiModelProperty(value = "冬季液压硫化时间(分)")
    @TableField("HY_PRESSURE_CURING_TIME2")
    private Integer hydraulicPressureCuringTime2;

    @ApiModelProperty(value = "制造示方书号")
    @TableField("EMBRYO_NO")
    private String embryoNo;

    @ApiModelProperty(value = "制造示方书类型")
    @TableField("EMBRYO_TYPE")
    private String embryoType;

    @ApiModelProperty(value = "制造示方书发行时间")
    @TableField("EMBRYO_RELEASE_DATE")
    private String embryoReleaseDate;

    @ApiModelProperty(value = "文字示方书号")
    @TableField("TEXT_NO")
    private String textNo;

    @ApiModelProperty(value = "文字示方书类型")
    @TableField("TEXT_TYPE")
    private String textType;

    @ApiModelProperty(value = "文字示方书发行时间")
    @TableField("TEXT_RELEASE_DATE")
    private String textReleaseDate;

    @ApiModelProperty(value = "硫化示方书号")
    @TableField("LH_NO")
    private String lhNo;

    @ApiModelProperty(value = "硫化示方书类型")
    @TableField("LH_TYPE")
    private String lhType;

    @ApiModelProperty(value = "硫化示方书发行时间")
    @TableField("LH_RELEASE_DATE")
    private String lhReleaseDate;

    @ApiModelProperty(value = "主物料(胎胚号)")
    @TableField("MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;
}
