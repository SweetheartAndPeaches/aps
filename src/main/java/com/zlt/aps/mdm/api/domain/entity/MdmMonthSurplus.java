package com.zlt.aps.mdm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 月度计划余量
 *
 * @author APS Team
 */
@Data
@ApiModel(description = "月度计划余量")
@TableName("T_MDM_MONTH_SURPLUS")
public class MdmMonthSurplus implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 工厂编号
     */
    @ApiModelProperty(value = "工厂编号")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @ApiModelProperty(value = "年份")
    @TableField("YEAR")
    private Integer year;

    /**
     * 月份
     */
    @ApiModelProperty(value = "月份")
    @TableField("MONTH")
    private Integer month;

    /**
     * 物料编码
     */
    @ApiModelProperty(value = "物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    /**
     * 物料描述
     */
    @ApiModelProperty(value = "物料描述")
    @TableField("MATERIAL_DESC")
    private String materialDesc;

    /**
     * 计划量
     */
    @ApiModelProperty(value = "计划量")
    @TableField("PLAN_QTY")
    private BigDecimal planQty;

    /**
     * 已排量
     */
    @ApiModelProperty(value = "已排量")
    @TableField("SCHEDULED_QTY")
    private BigDecimal scheduledQty;

    /**
     * 余量
     */
    @ApiModelProperty(value = "余量")
    @TableField("SURPLUS_QTY")
    private BigDecimal surplusQty;

    /**
     * 创建人
     */
    @ApiModelProperty(value = "创建人")
    @TableField("CREATE_BY")
    private String createBy;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    @TableField("CREATE_TIME")
    private Date createTime;

    /**
     * 更新人
     */
    @ApiModelProperty(value = "更新人")
    @TableField("UPDATE_BY")
    private String updateBy;

    /**
     * 更新时间
     */
    @ApiModelProperty(value = "更新时间")
    @TableField("UPDATE_TIME")
    private Date updateTime;
}
