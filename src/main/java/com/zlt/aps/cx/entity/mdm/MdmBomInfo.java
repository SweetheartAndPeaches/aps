package com.zlt.aps.cx.entity.mdm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.aps.cx.entity.base.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * BOM示方书对象
 * 对应表：T_MDM_BOM_INFO
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_BOM_INFO")
@ApiModel(value = "BOM示方书对象", description = "BOM示方书对象")
public class MdmBomInfo extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "子物料品号")
    @TableField("CHILD_MATERIAL_CODE")
    private String childMaterialCode;

    @ApiModelProperty(value = "子物料名称")
    @TableField("CHILD_MATERIAL_NAME")
    private String childMaterialName;

    @ApiModelProperty(value = "子物料名称编码")
    @TableField("CHILD_MATERIAL_NAME_CODE")
    private String childMaterialNameCode;

    @ApiModelProperty(value = "子物料代码")
    @TableField("CHILD_CODE")
    private String childCode;

    @ApiModelProperty(value = "单位描述")
    @TableField("UNIT")
    private String unit;

    @ApiModelProperty(value = "用量，单胎消耗量")
    @TableField("DOSAGE")
    private BigDecimal dosage;

    @ApiModelProperty(value = "组成用量")
    @TableField("DOSAGE_FORM")
    private BigDecimal dosageForm;

    @ApiModelProperty(value = "父物料品号")
    @TableField("PARENT_MATERIAL_CODE")
    private String parentMaterialCode;

    @ApiModelProperty(value = "父物料名称")
    @TableField("PARENT_MATERIAL_NAME")
    private String parentMaterialName;

    @ApiModelProperty(value = "父物料代码")
    @TableField("PARENT_CODE")
    private String parentCode;

    @ApiModelProperty(value = "父物料版本")
    @TableField("PARENT_VERSION")
    private String parentVersion;

    @ApiModelProperty(value = "生产阶段")
    @TableField("PRODUCTION_STAGE")
    private String productionStage;

    @ApiModelProperty(value = "生产阶段中文映射")
    @TableField("PRODUCTION_STAGE_CODE")
    private String productionStageCode;

    @ApiModelProperty(value = "BOM信息版本")
    @TableField("BOM_VERSION")
    private String bomVersion;

    @ApiModelProperty(value = "子物料版本")
    @TableField("CHILD_MATERIAL_VERSION")
    private String childMaterialVersion;

    @ApiModelProperty(value = "BOM类型")
    @TableField("BOM_TYPE")
    private String bomType;

    @ApiModelProperty(value = "状态(1正常3废止)")
    @TableField("STATUS")
    private String status;

    @ApiModelProperty(value = "MES系统创建时间")
    @TableField("MES_CREATE_DATE")
    private Date mesCreateDate;

    @ApiModelProperty(value = "MES更新时间")
    @TableField("MES_UPDATE_DATE")
    private Date mesUpdateDate;

    @TableField(exist = false)
    private MdmBomInfo parent;

    @TableField(exist = false)
    private Boolean isLeaf;

    @TableField(exist = false)
    private List<MdmBomInfo> children;
}
