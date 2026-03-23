package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物料主数据实体类
 *
 * @author APS Team
 */
@Data
@TableName(value = "t_cx_material", keepGlobalPrefix = false)
@Schema(description = "物料主数据")
public class Material implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "胎胚物料编码")
    @TableField("material_code")
    private String materialCode;

    @Schema(description = "物料名称")
    @TableField("material_name")
    private String materialName;

    @Schema(description = "规格型号")
    @TableField("specification")
    private String specification;

    @Schema(description = "产品结构")
    @TableField("product_structure")
    private String productStructure;

    @Schema(description = "主花纹")
    @TableField("main_pattern")
    private String mainPattern;

    @Schema(description = "花纹")
    @TableField("pattern")
    private String pattern;

    @Schema(description = "物料分类")
    @TableField("category")
    private String category;

    @Schema(description = "单位")
    @TableField("unit")
    private String unit;

    @Schema(description = "硫化时间(分钟)")
    @TableField("vulcanize_time_minutes")
    private BigDecimal vulcanizeTimeMinutes;

    @Schema(description = "是否主销产品")
    @TableField("is_main_product")
    private Integer isMainProduct;

    @Schema(description = "是否启用")
    @TableField("is_active")
    private Integer isActive;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
