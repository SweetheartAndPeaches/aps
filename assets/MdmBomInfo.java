package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * BOM示方书对象 t_mdm_bom_info
 */
@ApiModel(value = "BOM示方书对象", description = "BOM示方书对象")
@Data
@TableName(value = "T_MDM_BOM_INFO")
public class MdmBomInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 子物料品号 */
    @ApiModelProperty(value = "子物料品号", name = "childMaterialCode")
    @TableField(value = "CHILD_MATERIAL_CODE")
    private String childMaterialCode;

    /** 子物料名称 */
    @ApiModelProperty(value = "子物料名称", name = "childMaterialName")
    @TableField(value = "CHILD_MATERIAL_NAME")
    private String childMaterialName;

    /** 子物料名称编码(名称中文映射) */
    @ApiModelProperty(value = "子物料名称编码", name = "childMaterialNameCode")
    @TableField(value = "CHILD_MATERIAL_NAME_CODE")
    private String childMaterialNameCode;

    /** 子物料代码 */
    @ApiModelProperty(value = "子物料代码", name = "childCode")
    @TableField(value = "CHILD_CODE")
    private String childCode;

    /** 单位描述 */
    @ApiModelProperty(value = "单位描述", name = "unit")
    @TableField(value = "UNIT")
    private String unit;

    /** 用量，单胎消耗量 */
    @ApiModelProperty(value = "用量，单胎消耗量", name = "dosage")
    @TableField(value = "DOSAGE")
    private BigDecimal dosage;

    /** 组成用量，单胎需要的数量 */
    @ApiModelProperty(value = "组成用量", name = "dosageForm")
    @TableField(value = "DOSAGE_FORM")
    private BigDecimal dosageForm;

    /** 父物料品号 */
    @ApiModelProperty(value = "父物料品号", name = "parentMaterialCode")
    @TableField(value = "PARENT_MATERIAL_CODE")
    private String parentMaterialCode;

    /** 父物料名称 */
    @ApiModelProperty(value = "父物料名称", name = "parentMaterialName")
    @TableField(value = "PARENT_MATERIAL_NAME")
    private String parentMaterialName;

    /** 父物料代码 */
    @ApiModelProperty(value = "父物料代码", name = "parentCode")
    @TableField(value = "PARENT_CODE")
    private String parentCode;

    /** 父物料版本 */
    @ApiModelProperty(value = "父物料版本", name = "parentVersion")
    @TableField(value = "PARENT_VERSION")
    private String parentVersion;

    /** 生产阶段 */
    @ApiModelProperty(value = "生产阶段", name = "productionStage")
    @TableField(value = "PRODUCTION_STAGE")
    private String productionStage;

    /** 生产阶段中文映射 */
    @ApiModelProperty(value = "生产阶段中文映射", name = "productionStageCode")
    @TableField(value = "PRODUCTION_STAGE_CODE")
    private String productionStageCode;

    /** BOM信息版本 */
    @ApiModelProperty(value = "BOM信息版本", name = "bomVersion")
    @TableField(value = "BOM_VERSION")
    private String bomVersion;

    /** 子物料版本 */
    @ApiModelProperty(value = "子物料版本", name = "childMaterialVersion")
    @TableField(value = "CHILD_MATERIAL_VERSION")
    private String childMaterialVersion;

    /** BOM类型 */
    @ApiModelProperty(value = "BOM类型", name = "bomType")
    @TableField(value = "BOM_TYPE")
    private String bomType;

    /** 状态(1正常3废止) */
    @ApiModelProperty(value = "状态(1正常3废止)", name = "status")
    @TableField(value = "STATUS")
    private String status;

    /** MES系统创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "MES系统创建时间", name = "mesCreateDate")
    @TableField(value = "MES_CREATE_DATE")
    private Date mesCreateDate;

    /** MES更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "MES更新时间", name = "mesUpdateDate")
    @TableField(value = "MES_UPDATE_DATE")
    private Date mesUpdateDate;

    /** 父节点（仅用于构建树） */
    @TableField(exist = false)
    private MdmBomInfo parent;

    /** 叶子节点标记（仅用于构建树） */
    @TableField(exist = false)
    private Boolean isLeaf;

    /** 子节点（仅用于构建树） */
    @TableField(exist = false)
    private List<MdmBomInfo> children;
}
