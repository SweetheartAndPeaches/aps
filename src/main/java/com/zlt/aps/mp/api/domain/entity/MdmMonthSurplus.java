package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel(value = "月底计划余量")
public class MdmMonthSurplus implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "工厂编号")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "产品品类")
    @TableField("PRODUCT_TYPE_CODE")
    private String productTypeCode;

    @ApiModelProperty(value = "年份")
    @TableField("YEAR")
    private BigDecimal year;

    @ApiModelProperty(value = "月份")
    @TableField("MONTH")
    private BigDecimal month;

    @ApiModelProperty(value = "需求版本号")
    @TableField("REQUIRE_VERSION")
    private String requireVersion;

    @ApiModelProperty(value = "品牌")
    @TableField("BRAND")
    private String brand;

    @ApiModelProperty(value = "产品结构")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    @ApiModelProperty(value = "物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    @ApiModelProperty(value = "物料描述")
    @TableField("MATERIAL_DESC")
    private String materialDesc;

    @ApiModelProperty(value = "计划余量（硫化余量 = 总计划量 - 硫化真实完成量，已由系统计算好）")
    @TableField("PLAN_SURPLUS_QTY")
    private Integer planSurplusQty;

    @ApiModelProperty(value = "库存抓取日")
    @TableField("STOCK_CAPTURE_DATE")
    private LocalDateTime stockCaptureDate;

    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;

    @ApiModelProperty(value = "是否删除：0-未删除 1-已删除")
    @TableField("IS_DELETE")
    private Integer isDelete;

    @ApiModelProperty(value = "创建时间")
    @TableField("CREATE_TIME")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField("UPDATE_TIME")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "创建人")
    @TableField("CREATE_BY")
    private String createBy;

    @ApiModelProperty(value = "修改人")
    @TableField("UPDATE_BY")
    private String updateBy;
}
