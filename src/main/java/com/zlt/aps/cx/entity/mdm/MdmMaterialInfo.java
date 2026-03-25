package com.zlt.aps.cx.entity.mdm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.aps.cx.entity.base.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel(value = "物料信息对象", description = "物料信息对象")
public class MdmMaterialInfo extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "MES物料大类编码")
    @TableField("MES_MATERIAL_CATEGORY")
    private String mesMaterialCategory;

    @ApiModelProperty(value = "MES物料细类编码")
    @TableField("MES_MATERIAL_SUBCATEGORY")
    private String mesMaterialSubcategory;

    @ApiModelProperty(value = "MES物料大类名称")
    @TableField("MES_MATERIAL_CATE_NAME")
    private String mesMaterialCateName;

    @ApiModelProperty(value = "MES物料细类名称")
    @TableField("MES_MATERIAL_SUBCAT_NAME")
    private String mesMaterialSubcatName;

    @ApiModelProperty(value = "物料类型")
    @TableField("MATERIAL_CATEGORY")
    private String materialCategory;

    @ApiModelProperty(value = "产品分类")
    @TableField("PRODUCT_CATEGORY")
    private String productCategory;

    @ApiModelProperty(value = "结构")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    @ApiModelProperty(value = "物料编号")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    @ApiModelProperty(value = "物料名称")
    @TableField(exist = false)
    private String materialName;

    @ApiModelProperty(value = "产品结构")
    @TableField(exist = false)
    private String productStructure;

    @ApiModelProperty(value = "物料编号(查询参数)")
    @TableField(exist = false)
    private String materialCodes;

    @ApiModelProperty(value = "MES物料编号")
    @TableField("MES_MATERIAL_CODE")
    private String mesMaterialCode;

    @ApiModelProperty(value = "物料描述")
    @TableField("MATERIAL_DESC")
    private String materialDesc;

    @ApiModelProperty(value = "规格")
    @TableField("SPECIFICATIONS")
    private String specifications;

    @ApiModelProperty(value = "主花纹")
    @TableField("MAIN_PATTERN")
    private String mainPattern;

    @ApiModelProperty(value = "花纹")
    @TableField("PATTERN")
    private String pattern;

    @ApiModelProperty(value = "品牌")
    @TableField("BRAND")
    private String brand;

    @ApiModelProperty(value = "速度")
    @TableField("SPEED")
    private String speed;

    @ApiModelProperty(value = "层级")
    @TableField("HIERARCHY")
    private String hierarchy;

    @ApiModelProperty(value = "英寸")
    @TableField("PRO_SIZE")
    private String proSize;

    @ApiModelProperty(value = "性能")
    @TableField("ABILITY")
    private String ability;

    @ApiModelProperty(value = "不可生产：0-否 1-是")
    @TableField("CANT_PRODUCE")
    private Integer cantProduce;

    @ApiModelProperty(value = "胎胚编码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    @ApiModelProperty(value = "胎胚描述")
    @TableField("EMBRYO_DESC")
    private String embryoDesc;

    @ApiModelProperty(value = "断面宽")
    @TableField("SECTION_WIDTH")
    private String sectionWidth;

    @ApiModelProperty(value = "品名编码")
    @TableField("PRODUCT_TYPE_CODE")
    private String productTypeCode;

    @ApiModelProperty(value = "品名")
    @TableField("PRODUCT_TYPE_NAME")
    private String productTypeName;

    @ApiModelProperty(value = "模具大类")
    @TableField("MOULD_CATEGORY")
    private String mouldCategory;

    @ApiModelProperty(value = "轮胎类型")
    @TableField("TIRE_TYPE")
    private String tireType;

    @ApiModelProperty(value = "公用类型")
    @TableField("COMMON_TYPE")
    private String commonType;

    @ApiModelProperty(value = "替换品种分组")
    @TableField("REPLACE_GROUP")
    private String replaceGroup;

    @ApiModelProperty(value = "不能发货：0-否 1-是")
    @TableField("NO_DELIVERY")
    private Integer noDelivery;

    @ApiModelProperty(value = "环保")
    @TableField("ENVIRONMENT_PROTECTION")
    private String environmentProtection;

    @ApiModelProperty(value = "认证串")
    @TableField("AUTHENTICATION")
    private String authentication;

    @ApiModelProperty(value = "物料组")
    @TableField("MATERIAL_GROUP_CODE")
    private String materialGroupCode;

    @ApiModelProperty(value = "废停标志")
    @TableField("FORBID_TAG")
    private String forbidTag;

    @ApiModelProperty(value = "单胎重量")
    @TableField("SINGLE_TIRE_WEIGHT")
    private BigDecimal singleTireWeight;

    @ApiModelProperty(value = "合模压力 PA")
    @TableField("MOULD_CLAMPING_PRESSURE")
    private BigDecimal mouldClampingPressure;

    @ApiModelProperty(value = "毛利率Json")
    @TableField("GROSS_RATE_JSON")
    private String grossRateJson;

    @ApiModelProperty(value = "外销毛利率")
    @TableField(exist = false)
    private BigDecimal outGrossRate;

    @ApiModelProperty(value = "内销毛利率")
    @TableField(exist = false)
    private BigDecimal inGrossRate;

    @ApiModelProperty(value = "OE毛利率")
    @TableField(exist = false)
    private BigDecimal oeGrossRate;

    @ApiModelProperty(value = "是否查询轮胎类型空数据")
    @TableField(exist = false)
    private String isTireTypeNullData;

    @ApiModelProperty(value = "是否查询共用类型空数据")
    @TableField(exist = false)
    private String isCommonTypeNullData;

    @ApiModelProperty(value = "是否查询品牌空数据")
    @TableField(exist = false)
    private String isBrandNullData;

    @ApiModelProperty(value = "质控状态")
    @TableField("QUALITY_STATE_CODE")
    private String qualityStateCode;

    @ApiModelProperty(value = "制造示方书号")
    @TableField(exist = false)
    private String embryoNo;

    @ApiModelProperty(value = "文字示方书号")
    @TableField(exist = false)
    private String textNo;

    @ApiModelProperty(value = "硫化示方书号")
    @TableField(exist = false)
    private String lhNo;
}
