package com.jinyu.aps.entity.mdm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jinyu.aps.entity.base.ApsBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "SKU与施工（示方书）关系对象")
public class MdmSkuConstructionRef extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 物料编码
     */
    @Schema(description = "物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    /**
     * MES物料编码
     */
    @Schema(description = "MES物料编码")
    @TableField("MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /**
     * 产品状态
     */
    @Schema(description = "产品状态")
    @TableField("TRIAL_STATUS")
    private String trialStatus;

    /**
     * 规格代号
     */
    @Schema(description = "规格代号")
    @TableField("SPEC_CODE")
    private String specCode;

    /**
     * 施工代号
     */
    @Schema(description = "施工代号")
    @TableField("CONSTRUCTION_CODE")
    private String constructionCode;

    /**
     * 胎胚号
     */
    @Schema(description = "胎胚号")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    /**
     * 生产版本
     */
    @Schema(description = "生产版本")
    @TableField("PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 成型法
     */
    @Schema(description = "成型法")
    @TableField("MOULD_METHOD")
    private String mouldMethod;

    /**
     * BOM版本
     */
    @Schema(description = "BOM版本")
    @TableField("BOM_VERSION")
    private String bomVersion;

    /**
     * 合模压力
     */
    @Schema(description = "合模压力")
    @TableField("MOULD_CLAMPING_PRESSURE")
    private BigDecimal mouldClampingPressure;

    /**
     * 模具型腔
     */
    @Schema(description = "模具型腔")
    @TableField("MOULD_CAVITY")
    private String mouldCavity;

    /**
     * 是否零度材料
     */
    @Schema(description = "是否零度材料")
    @TableField("IS_ZERO_RACK")
    private String isZeroRack;

    /**
     * 夏季机械硫化时间(分)
     */
    @Schema(description = "夏季机械硫化时间(分)")
    @TableField("CURING_TIME")
    private Integer curingTime;

    /**
     * 夏季液压硫化时间(分)
     */
    @Schema(description = "夏季液压硫化时间(分)")
    @TableField("HY_PRESSURE_CURING_TIME")
    private Integer hydraulicPressureCuringTime;

    /**
     * 冬季机械硫化时间(分)
     */
    @Schema(description = "冬季机械硫化时间(分)")
    @TableField("CURING_TIME2")
    private Integer curingTime2;

    /**
     * 冬季液压硫化时间(分)
     */
    @Schema(description = "冬季液压硫化时间(分)")
    @TableField("HY_PRESSURE_CURING_TIME2")
    private Integer hydraulicPressureCuringTime2;

    // ========== 示方书信息 ==========

    /**
     * 制造示方书号
     */
    @Schema(description = "制造示方书号")
    @TableField("EMBRYO_NO")
    private String embryoNo;

    /**
     * 制造示方书类型
     */
    @Schema(description = "制造示方书类型")
    @TableField("EMBRYO_TYPE")
    private String embryoType;

    /**
     * 制造示方书发行时间
     */
    @Schema(description = "制造示方书发行时间")
    @TableField("EMBRYO_RELEASE_DATE")
    private String embryoReleaseDate;

    /**
     * 文字示方书号
     */
    @Schema(description = "文字示方书号")
    @TableField("TEXT_NO")
    private String textNo;

    /**
     * 文字示方书类型
     */
    @Schema(description = "文字示方书类型")
    @TableField("TEXT_TYPE")
    private String textType;

    /**
     * 文字示方书发行时间
     */
    @Schema(description = "文字示方书发行时间")
    @TableField("TEXT_RELEASE_DATE")
    private String textReleaseDate;

    /**
     * 硫化示方书号
     */
    @Schema(description = "硫化示方书号")
    @TableField("LH_NO")
    private String lhNo;

    /**
     * 硫化示方书类型
     */
    @Schema(description = "硫化示方书类型")
    @TableField("LH_TYPE")
    private String lhType;

    /**
     * 硫化示方书发行时间
     */
    @Schema(description = "硫化示方书发行时间")
    @TableField("LH_RELEASE_DATE")
    private String lhReleaseDate;

    /**
     * 主物料(胎胚号)
     */
    @Schema(description = "主物料(胎胚号)")
    @TableField("MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;
}
