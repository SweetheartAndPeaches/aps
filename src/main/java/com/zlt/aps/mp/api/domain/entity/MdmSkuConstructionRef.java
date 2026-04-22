package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * SKU与施工（示方书）关系对象 t_mdm_sku_construction_ref
 * 用于查询硫化示方书类型
 *
 * @author APS Team
 */
@ApiModel(value = "SKU与施工（示方书）关系对象", description = "SKU与施工（示方书）关系对象")
@Data
@TableName(value = "T_MDM_SKU_CONSTRUCTION_REF")
public class MdmSkuConstructionRef extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    @ApiModelProperty(value = "产品状态", name = "trialStatus")
    @TableField(value = "TRIAL_STATUS")
    private String trialStatus;

    @ApiModelProperty(value = "规格代号", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    @ApiModelProperty(value = "施工代号", name = "constructionCode")
    @TableField(value = "CONSTRUCTION_CODE")
    private String constructionCode;

    @ApiModelProperty(value = "胎胚号", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    @ApiModelProperty(value = "生产版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    @ApiModelProperty(value = "成型法", name = "mouldMethod")
    @TableField(value = "MOULD_METHOD")
    private String mouldMethod;

    @ApiModelProperty(value = "BOM版本", name = "bomVersion")
    @TableField(value = "BOM_VERSION")
    private String bomVersion;

    @ApiModelProperty(value = "合模压力", name = "mouldClampingPressure")
    @TableField(value = "MOULD_CLAMPING_PRESSURE")
    private java.math.BigDecimal mouldClampingPressure;

    @ApiModelProperty(value = "模具型腔", name = "mouldCavity")
    @TableField(value = "MOULD_CAVITY")
    private String mouldCavity;

    @ApiModelProperty(value = "是否零度材料", name = "isZeroRack")
    @TableField(value = "IS_ZERO_RACK")
    private String isZeroRack;

    @ApiModelProperty(value = "夏季机械硫化时间(分)", name = "curingTime")
    @TableField(value = "CURING_TIME")
    private Integer curingTime;

    @ApiModelProperty(value = "夏季液压硫化时间(分)", name = "hydraulicPressureCuringTime")
    @TableField(value = "HY_PRESSURE_CURING_TIME")
    private Integer hydraulicPressureCuringTime;

    @ApiModelProperty(value = "冬季机械硫化时间(分)", name = "curingTime2")
    @TableField(value = "CURING_TIME2")
    private Integer curingTime2;

    @ApiModelProperty(value = "冬季液压硫化时间(分)", name = "hydraulicPressureCuringTime2")
    @TableField(value = "HY_PRESSURE_CURING_TIME2")
    private Integer hydraulicPressureCuringTime2;

    @ApiModelProperty(value = "制造示方书号", name = "embryoNo")
    @TableField(value = "EMBRYO_NO")
    private String embryoNo;

    @ApiModelProperty(value = "制造示方书类型", name = "embryoType")
    @TableField(value = "EMBRYO_TYPE")
    private String embryoType;

    @ApiModelProperty(value = "制造示方书发行时间", name = "embryoReleaseDate")
    @TableField(value = "EMBRYO_RELEASE_DATE")
    private String embryoReleaseDate;

    @ApiModelProperty(value = "文字示方书号", name = "textNo")
    @TableField(value = "TEXT_NO")
    private String textNo;

    @ApiModelProperty(value = "文字示方书类型", name = "textType")
    @TableField(value = "TEXT_TYPE")
    private String textType;

    @ApiModelProperty(value = "文字示方书发行时间", name = "textReleaseDate")
    @TableField(value = "TEXT_RELEASE_DATE")
    private String textReleaseDate;

    @ApiModelProperty(value = "硫化示方书号", name = "lhNo")
    @TableField(value = "LH_NO")
    private String lhNo;

    @ApiModelProperty(value = "硫化示方书类型", name = "lhType")
    @TableField(value = "LH_TYPE")
    private String lhType;

    @ApiModelProperty(value = "硫化示方书发行时间", name = "lhReleaseDate")
    @TableField(value = "LH_RELEASE_DATE")
    private String lhReleaseDate;

    @ApiModelProperty(value = "主物料(胎胚号)", name = "mainMaterialDesc")
    @TableField(value = "MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;
}
