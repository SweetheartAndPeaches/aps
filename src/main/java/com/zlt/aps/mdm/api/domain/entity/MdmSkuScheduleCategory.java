package com.zlt.aps.mdm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * SKU排产分类
 *
 * @author APS Team
 */
@Data
@ApiModel(description = "SKU排产分类")
@TableName("T_MDM_SKU_SCHEDULE_CATEGORY")
public class MdmSkuScheduleCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * SKU编码
     */
    @ApiModelProperty(value = "SKU编码")
    @TableField("SKU_CODE")
    private String skuCode;

    /**
     * SKU名称
     */
    @ApiModelProperty(value = "SKU名称")
    @TableField("SKU_NAME")
    private String skuName;

    /**
     * 物料编码
     */
    @ApiModelProperty(value = "物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    /**
     * 分类编码
     */
    @ApiModelProperty(value = "分类编码")
    @TableField("CATEGORY_CODE")
    private String categoryCode;

    /**
     * 分类名称
     */
    @ApiModelProperty(value = "分类名称")
    @TableField("CATEGORY_NAME")
    private String categoryName;

    /**
     * 优先级
     */
    @ApiModelProperty(value = "优先级")
    @TableField("PRIORITY")
    private Integer priority;

    /**
     * 是否启用
     */
    @ApiModelProperty(value = "是否启用(0-禁用 1-启用)")
    @TableField("IS_ACTIVE")
    private Integer isActive;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;

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
