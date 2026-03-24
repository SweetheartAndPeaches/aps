package com.zlt.aps.cx.entity.mdm;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 月底计划余量实体
 * 
 * 用于计算硫化余量和收尾判断
 *
 * @author APS Team
 */
@Data
@TableName("t_mdm_month_surplus")
@Schema(description = "月底计划余量")
public class MdmMonthSurplus implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @Schema(description = "工厂编号")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    @Schema(description = "产品品类")
    @TableField("PRODUCT_TYPE_CODE")
    private String productTypeCode;

    @Schema(description = "年份")
    @TableField("YEAR")
    private BigDecimal year;

    @Schema(description = "月份")
    @TableField("MONTH")
    private BigDecimal month;

    @Schema(description = "需求版本号")
    @TableField("REQUIRE_VERSION")
    private String requireVersion;

    @Schema(description = "品牌")
    @TableField("BRAND")
    private String brand;

    @Schema(description = "产品结构")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    @Schema(description = "物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    @Schema(description = "物料描述")
    @TableField("MATERIAL_DESC")
    private String materialDesc;

    @Schema(description = "计划余量（硫化余量 = 总计划量 - 硫化真实完成量）")
    @TableField("PLAN_SURPLUS_QTY")
    private Integer planSurplusQty;

    @Schema(description = "库存抓取日")
    @TableField("STOCK_CAPTURE_DATE")
    private LocalDateTime stockCaptureDate;

    @Schema(description = "备注")
    @TableField("REMARK")
    private String remark;

    @Schema(description = "是否删除：0-未删除 1-已删除")
    @TableField("IS_DELETE")
    private Integer isDelete;

    @Schema(description = "创建时间")
    @TableField("CREATE_TIME")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField("UPDATE_TIME")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    @TableField("CREATE_BY")
    private String createBy;

    @Schema(description = "修改人")
    @TableField("UPDATE_BY")
    private String updateBy;
}
