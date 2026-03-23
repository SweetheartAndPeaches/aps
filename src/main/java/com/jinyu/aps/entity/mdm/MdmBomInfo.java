package com.jinyu.aps.entity.mdm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.jinyu.aps.entity.base.ApsBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "BOM示方书对象")
public class MdmBomInfo extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 子物料品号
     */
    @Schema(description = "子物料品号")
    @TableField("CHILD_MATERIAL_CODE")
    private String childMaterialCode;

    /**
     * 子物料名称
     */
    @Schema(description = "子物料名称")
    @TableField("CHILD_MATERIAL_NAME")
    private String childMaterialName;

    /**
     * 子物料名称编码(名称中文映射)
     */
    @Schema(description = "子物料名称编码")
    @TableField("CHILD_MATERIAL_NAME_CODE")
    private String childMaterialNameCode;

    /**
     * 子物料代码
     */
    @Schema(description = "子物料代码")
    @TableField("CHILD_CODE")
    private String childCode;

    /**
     * 单位描述
     */
    @Schema(description = "单位描述")
    @TableField("UNIT")
    private String unit;

    /**
     * 用量，单胎消耗量
     */
    @Schema(description = "用量，单胎消耗量")
    @TableField("DOSAGE")
    private BigDecimal dosage;

    /**
     * 组成用量，单胎需要的数量
     */
    @Schema(description = "组成用量")
    @TableField("DOSAGE_FORM")
    private BigDecimal dosageForm;

    /**
     * 父物料品号
     */
    @Schema(description = "父物料品号")
    @TableField("PARENT_MATERIAL_CODE")
    private String parentMaterialCode;

    /**
     * 父物料名称
     */
    @Schema(description = "父物料名称")
    @TableField("PARENT_MATERIAL_NAME")
    private String parentMaterialName;

    /**
     * 父物料代码
     */
    @Schema(description = "父物料代码")
    @TableField("PARENT_CODE")
    private String parentCode;

    /**
     * 父物料版本
     */
    @Schema(description = "父物料版本")
    @TableField("PARENT_VERSION")
    private String parentVersion;

    /**
     * 生产阶段
     */
    @Schema(description = "生产阶段")
    @TableField("PRODUCTION_STAGE")
    private String productionStage;

    /**
     * 生产阶段中文映射
     */
    @Schema(description = "生产阶段中文映射")
    @TableField("PRODUCTION_STAGE_CODE")
    private String productionStageCode;

    /**
     * BOM信息版本
     */
    @Schema(description = "BOM信息版本")
    @TableField("BOM_VERSION")
    private String bomVersion;

    /**
     * 子物料版本
     */
    @Schema(description = "子物料版本")
    @TableField("CHILD_MATERIAL_VERSION")
    private String childMaterialVersion;

    /**
     * BOM类型
     */
    @Schema(description = "BOM类型")
    @TableField("BOM_TYPE")
    private String bomType;

    /**
     * 状态(1正常3废止)
     */
    @Schema(description = "状态(1正常3废止)")
    @TableField("STATUS")
    private String status;

    /**
     * MES系统创建时间
     */
    @Schema(description = "MES系统创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("MES_CREATE_DATE")
    private Date mesCreateDate;

    /**
     * MES更新时间
     */
    @Schema(description = "MES更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("MES_UPDATE_DATE")
    private Date mesUpdateDate;

    // ========== 非数据库字段，用于构建树结构 ==========

    /**
     * 父节点（仅用于构建树）
     */
    @TableField(exist = false)
    private MdmBomInfo parent;

    /**
     * 叶子节点标记（仅用于构建树）
     */
    @TableField(exist = false)
    private Boolean isLeaf;

    /**
     * 子节点（仅用于构建树）
     */
    @TableField(exist = false)
    private List<MdmBomInfo> children;
}
