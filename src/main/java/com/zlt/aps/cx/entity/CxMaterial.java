package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物料主数据实体类
 * 对应数据库表：T_CX_MATERIAL
 *
 * @author APS Team
 * @deprecated 已弃用，请使用 {@link com.zlt.aps.cx.entity.mdm.MdmMaterialInfo}
 */
@Deprecated
@Data
@TableName(value = "T_CX_MATERIAL", keepGlobalPrefix = false)
@Schema(description = "物料主数据")
public class CxMaterial implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @Schema(description = "胎胚物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    @Schema(description = "物料名称")
    @TableField("MATERIAL_NAME")
    private String materialName;

    @Schema(description = "规格型号")
    @TableField("SPECIFICATION")
    private String specification;

    @Schema(description = "产品结构")
    @TableField("PRODUCT_STRUCTURE")
    private String productStructure;

    @Schema(description = "主花纹")
    @TableField("MAIN_PATTERN")
    private String mainPattern;

    @Schema(description = "花纹")
    @TableField("PATTERN")
    private String pattern;

    @Schema(description = "物料分类")
    @TableField("CATEGORY")
    private String category;

    @Schema(description = "单位")
    @TableField("UNIT")
    private String unit;

    @Schema(description = "硫化时间(分钟)")
    @TableField("VULCANIZE_TIME_MINUTES")
    private BigDecimal vulcanizeTimeMinutes;

    @Schema(description = "是否主销产品")
    @TableField("IS_MAIN_PRODUCT")
    private Integer isMainProduct;

    @Schema(description = "是否启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;

    @Schema(description = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
