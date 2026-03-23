package com.jinyu.aps.entity.mdm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jinyu.aps.entity.base.ApsBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 物料信息对象
 * 对应表：T_MDM_MATERIAL_INFO
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_MATERIAL_INFO")
@Schema(description = "物料信息对象")
public class MdmMaterialInfo extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * MES物料大类编码
     */
    @Schema(description = "MES物料大类编码")
    @TableField("MES_MATERIAL_CATEGORY")
    private String mesMaterialCategory;

    /**
     * MES物料细类编码
     */
    @Schema(description = "MES物料细类编码")
    @TableField("MES_MATERIAL_SUBCATEGORY")
    private String mesMaterialSubcategory;

    /**
     * MES物料大类名称
     */
    @Schema(description = "MES物料大类名称")
    @TableField("MES_MATERIAL_CATE_NAME")
    private String mesMaterialCateName;

    /**
     * MES物料细类名称
     */
    @Schema(description = "MES物料细类名称")
    @TableField("MES_MATERIAL_SUBCAT_NAME")
    private String mesMaterialSubcatName;

    /**
     * 物料类型
     */
    @Schema(description = "物料类型")
    @TableField("MATERIAL_CATEGORY")
    private String materialCategory;

    /**
     * 产品分类
     */
    @Schema(description = "产品分类")
    @TableField("PRODUCT_CATEGORY")
    private String productCategory;

    /**
     * 结构
     */
    @Schema(description = "结构")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    /**
     * 物料编号
     */
    @Schema(description = "物料编号")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    /**
     * 物料编号(查询参数，可传多个，非数据库字段)
     */
    @Schema(description = "物料编号(查询参数)")
    @TableField(exist = false)
    private String materialCodes;

    /**
     * MES物料编号
     */
    @Schema(description = "MES物料编号")
    @TableField("MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /**
     * 物料描述
     */
    @Schema(description = "物料描述")
    @TableField("MATERIAL_DESC")
    private String materialDesc;

    /**
     * 规格
     */
    @Schema(description = "规格")
    @TableField("SPECIFICATIONS")
    private String specifications;

    /**
     * 主花纹
     */
    @Schema(description = "主花纹")
    @TableField("MAIN_PATTERN")
    private String mainPattern;

    /**
     * 花纹
     */
    @Schema(description = "花纹")
    @TableField("PATTERN")
    private String pattern;

    /**
     * 品牌
     */
    @Schema(description = "品牌")
    @TableField("BRAND")
    private String brand;

    /**
     * 速度
     */
    @Schema(description = "速度")
    @TableField("SPEED")
    private String speed;

    /**
     * 层级
     */
    @Schema(description = "层级")
    @TableField("HIERARCHY")
    private String hierarchy;

    /**
     * 英寸
     */
    @Schema(description = "英寸")
    @TableField("PRO_SIZE")
    private String proSize;

    /**
     * 性能
     */
    @Schema(description = "性能")
    @TableField("ABILITY")
    private String ability;

    /**
     * 不可生产，0否1是
     */
    @Schema(description = "不可生产：0-否 1-是")
    @TableField("CANT_PRODUCE")
    private Integer cantProduce;

    /**
     * 胎胚编码
     */
    @Schema(description = "胎胚编码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    /**
     * 胎胚描述
     */
    @Schema(description = "胎胚描述")
    @TableField("EMBRYO_DESC")
    private String embryoDesc;

    /**
     * 断面宽
     */
    @Schema(description = "断面宽")
    @TableField("SECTION_WIDTH")
    private String sectionWidth;

    /**
     * 品名编码
     */
    @Schema(description = "品名编码")
    @TableField("PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 品名
     */
    @Schema(description = "品名")
    @TableField("PRODUCT_TYPE_NAME")
    private String productTypeName;

    /**
     * 模具大类
     */
    @Schema(description = "模具大类")
    @TableField("MOULD_CATEGORY")
    private String mouldCategory;

    /**
     * 轮胎类型
     */
    @Schema(description = "轮胎类型")
    @TableField("TIRE_TYPE")
    private String tireType;

    /**
     * 公用类型
     */
    @Schema(description = "公用类型")
    @TableField("COMMON_TYPE")
    private String commonType;

    /**
     * 替换品种分组
     */
    @Schema(description = "替换品种分组")
    @TableField("REPLACE_GROUP")
    private String replaceGroup;

    /**
     * 不能发货：0-否 1-是
     */
    @Schema(description = "不能发货：0-否 1-是")
    @TableField("NO_DELIVERY")
    private Integer noDelivery;

    /**
     * 环保
     */
    @Schema(description = "环保")
    @TableField("ENVIRONMENT_PROTECTION")
    private String environmentProtection;

    /**
     * 认证串
     */
    @Schema(description = "认证串")
    @TableField("AUTHENTICATION")
    private String authentication;

    /**
     * 物料组
     */
    @Schema(description = "物料组")
    @TableField("MATERIAL_GROUP_CODE")
    private String materialGroupCode;

    /**
     * 废停标志
     */
    @Schema(description = "废停标志")
    @TableField("FORBID_TAG")
    private String forbidTag;

    /**
     * 单胎重量
     */
    @Schema(description = "单胎重量")
    @TableField("SINGLE_TIRE_WEIGHT")
    private BigDecimal singleTireWeight;

    /**
     * 合模压力 PA
     */
    @Schema(description = "合模压力 PA")
    @TableField("MOULD_CLAMPING_PRESSURE")
    private BigDecimal mouldClampingPressure;

    /**
     * 毛利率Json
     */
    @Schema(description = "毛利率Json")
    @TableField("GROSS_RATE_JSON")
    private String grossRateJson;

    /**
     * 外销毛利率（非数据库字段）
     */
    @Schema(description = "外销毛利率")
    @TableField(exist = false)
    private BigDecimal outGrossRate;

    /**
     * 内销毛利率（非数据库字段）
     */
    @Schema(description = "内销毛利率")
    @TableField(exist = false)
    private BigDecimal inGrossRate;

    /**
     * OE毛利率（非数据库字段）
     */
    @Schema(description = "OE毛利率")
    @TableField(exist = false)
    private BigDecimal oeGrossRate;

    /**
     * 是否查询轮胎类型空数据，0-否，1-是（非数据库字段）
     */
    @Schema(description = "是否查询轮胎类型空数据")
    @TableField(exist = false)
    private String isTireTypeNullData;

    /**
     * 是否查询共用类型空数据，0-否，1-是（非数据库字段）
     */
    @Schema(description = "是否查询共用类型空数据")
    @TableField(exist = false)
    private String isCommonTypeNullData;

    /**
     * 是否查询品牌空数据，0-否，1-是（非数据库字段）
     */
    @Schema(description = "是否查询品牌空数据")
    @TableField(exist = false)
    private String isBrandNullData;

    /**
     * 质控状态
     */
    @Schema(description = "质控状态")
    @TableField("QUALITY_STATE_CODE")
    private String qualityStateCode;

    // ========== 非数据库字段，用于关联查询 ==========

    /**
     * 制造示方书号（非数据库字段）
     */
    @Schema(description = "制造示方书号")
    @TableField(exist = false)
    private String embryoNo;

    /**
     * 文字示方书号（非数据库字段）
     */
    @Schema(description = "文字示方书号")
    @TableField(exist = false)
    private String textNo;

    /**
     * 硫化示方书号（非数据库字段）
     */
    @Schema(description = "硫化示方书号")
    @TableField(exist = false)
    private String lhNo;
}
