package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 物料信息对象 t_mdm_material_info
 */
@ApiModel(value = "物料信息对象", description = "物料信息对象")
@Data
@TableName(value = "T_MDM_MATERIAL_INFO")
public class MdmMaterialInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 分厂编号 */
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** MES物料大类编码 */
    @ApiModelProperty(value = "MES物料大类编码", name = "mesMaterialCategory")
    @TableField(value = "MES_MATERIAL_CATEGORY")
    private String mesMaterialCategory;

    /** MES物料细类编码 */
    @ApiModelProperty(value = "MES物料细类编码", name = "mesMaterialSubcategory")
    @TableField(value = "MES_MATERIAL_SUBCATEGORY")
    private String mesMaterialSubcategory;

    /** MES物料大类名称 */
    @ApiModelProperty(value = "MES物料大类名称", name = "mesMaterialCateName")
    @TableField(value = "MES_MATERIAL_CATE_NAME")
    private String mesMaterialCateName;

    /** MES物料细类名称 */
    @ApiModelProperty(value = "MES物料细类名称", name = "mesMaterialSubcatName")
    @TableField(value = "MES_MATERIAL_SUBCAT_NAME")
    private String mesMaterialSubcatName;

    /** 物料类型 */
    @ApiModelProperty(value = "物料类型", name = "materialCategory")
    @TableField(value = "MATERIAL_CATEGORY")
    private String materialCategory;

    /** 产品分类 */
    @ApiModelProperty(value = "产品分类", name = "productCategory")
    @TableField(value = "PRODUCT_CATEGORY")
    private String productCategory;

    /** 结构 */
    @ApiModelProperty(value = "结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /** 物料编号 */
    @ApiModelProperty(value = "物料编号", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料编号(查询参数，可传多个) */
    @ApiModelProperty(value = "物料编号(查询参数，可传多个)", name = "materialCodes")
    @TableField(exist = false)
    private String materialCodes;

    /** MES物料编号 */
    @ApiModelProperty(value = "MES物料编号", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /** 物料描述 */
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 规格 */
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /** 主花纹 */
    @ApiModelProperty(value = "主花纹", name = "mainPattern")
    @TableField(value = "MAIN_PATTERN")
    private String mainPattern;

    /** 花纹 */
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /** 品牌 */
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /** 速度 */
    @ApiModelProperty(value = "速度", name = "speed")
    @TableField(value = "SPEED")
    private String speed;

    /** 层级 */
    @ApiModelProperty(value = "层级", name = "hierarchy")
    @TableField(value = "HIERARCHY")
    private String hierarchy;

    /** 英寸 */
    @ApiModelProperty(value = "英寸", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private String proSize;

    /** 性能 */
    @ApiModelProperty(value = "性能", name = "ability")
    @TableField(value = "ABILITY")
    private String ability;

    /** 不可生产，0否1是 */
    @ApiModelProperty(value = "不可生产", name = "cantProduce")
    @TableField(value = "CANT_PRODUCE")
    private Integer cantProduce;

    /** 胎胚编码 */
    @ApiModelProperty(value = "胎胚编码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /** 胎胚描述 */
    @ApiModelProperty(value = "胎胚描述", name = "embryoDesc")
    @TableField(value = "EMBRYO_DESC")
    private String embryoDesc;

    /** 断面宽 */
    @ApiModelProperty(value = "断面宽", name = "sectionWidth")
    @TableField(value = "SECTION_WIDTH")
    private String sectionWidth;

    /** 品名编码 */
    @ApiModelProperty(value = "品名编码", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /** 品名 */
    @ApiModelProperty(value = "品名", name = "productTypeName")
    @TableField(value = "PRODUCT_TYPE_NAME")
    private String productTypeName;

    /** 模具大类 */
    @ApiModelProperty(value = "模具大类", name = "mouldCategory")
    @TableField(value = "MOULD_CATEGORY")
    private String mouldCategory;

    /** 轮胎类型 */
    @ApiModelProperty(value = "轮胎类型", name = "tireType")
    @TableField(value = "TIRE_TYPE")
    private String tireType;

    /** 公用类型 */
    @ApiModelProperty(value = "公用类型", name = "commonType")
    @TableField(value = "COMMON_TYPE")
    private String commonType;

    /** 替换品种分组 */
    @ApiModelProperty(value = "替换品种分组", name = "replaceGroup")
    @TableField(value = "REPLACE_GROUP")
    private String replaceGroup;

    /** 不能发货 */
    @ApiModelProperty(value = "不能发货", name = "noDelivery")
    @TableField(value = "NO_DELIVERY")
    private Integer noDelivery;

    /** 环保 */
    @ApiModelProperty(value = "环保", name = "environmentProtection")
    @TableField(value = "ENVIRONMENT_PROTECTION")
    private String environmentProtection;

    /** 认证串 */
    @ApiModelProperty(value = "认证串", name = "authentication")
    @TableField(value = "AUTHENTICATION")
    private String authentication;

    /** 物料组 */
    @ApiModelProperty(value = "物料组", name = "materialGroupCode")
    @TableField(value = "MATERIAL_GROUP_CODE")
    private String materialGroupCode;

    /** 废停标志 */
    @ApiModelProperty(value = "废停标志", name = "forbidTag")
    @TableField(value = "FORBID_TAG")
    private String forbidTag;

    /** 单胎重量 */
    @ApiModelProperty(value = "单胎重量", name = "singleTireWeight")
    @TableField(value = "SINGLE_TIRE_WEIGHT")
    private BigDecimal singleTireWeight;

    /** 合模压力 PA */
    @ApiModelProperty(value = "合模压力 PA", name = "mouldClampingPressure")
    @TableField(value = "MOULD_CLAMPING_PRESSURE")
    private BigDecimal mouldClampingPressure;

    /** 毛利率Json */
    @ApiModelProperty(value = "毛利率Json", name = "grossRateJson")
    @TableField(value = "GROSS_RATE_JSON")
    private String grossRateJson;

    /** 外销毛利率 */
    @ApiModelProperty(value = "外销毛利率", name = "outGrossRate")
    @TableField(exist = false)
    private BigDecimal outGrossRate;

    /** 内销毛利率 */
    @ApiModelProperty(value = "内销毛利率", name = "inGrossRate")
    @TableField(exist = false)
    private BigDecimal inGrossRate;

    /** OE毛利率 */
    @ApiModelProperty(value = "OE毛利率", name = "oeGrossRate")
    @TableField(exist = false)
    private BigDecimal oeGrossRate;

    /** 是否查询轮胎类型空数据，0-否，1-是 */
    @ApiModelProperty(value = "是否查询轮胎类型空数据", name = "isTireTypeNullData")
    @TableField(exist = false)
    private String isTireTypeNullData;

    /** 是否查询共用类型空数据，0-否，1-是 */
    @ApiModelProperty(value = "是否查询共用类型空数据", name = "isCommonTypeNullData")
    @TableField(exist = false)
    private String isCommonTypeNullData;

    /** 是否查询品牌空数据，0-否，1-是 */
    @ApiModelProperty(value = "是否查询品牌空数据", name = "isBrandNullData")
    @TableField(exist = false)
    private String isBrandNullData;

    /** 备注 */
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;

    /** 制造示方书号 */
    @ApiModelProperty(value = "制造示方书号", name = "embryoNo")
    @TableField(exist = false)
    private String embryoNo;

    /** 文字示方书号 */
    @ApiModelProperty(value = "文字示方书号", name = "textNo")
    @TableField(exist = false)
    private String textNo;

    /** 硫化示方书号 */
    @ApiModelProperty(value = "硫化示方书号", name = "lhNo")
    @TableField(exist = false)
    private String lhNo;

    /** 质控状态 */
    @ApiModelProperty(value = "质控状态", name = "qualityStateCode")
    @TableField(value = "QUALITY_STATE_CODE")
    private String qualityStateCode;
}
