package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 工厂月度排产-SKU日硫化产能表
 * 人工维护的基础产能表
 * 对应表：T_MDM_MONTH_PLAN_PRODUCT_LH
 *
 * @author APS Team
 */
@Data
@TableName("T_MDM_MONTH_PLAN_PRODUCT_LH")
@ApiModel(value = "SKU日硫化产能对象", description = "工厂月度排产-SKU日硫化产能表")
public class MdmMonthPlanProductLhCapacity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "工厂编码")
    private String factoryCode;

    @ApiModelProperty(value = "物料编码")
    private String materialCode;

    @ApiModelProperty(value = "物料描述")
    private String materialDesc;

    @ApiModelProperty(value = "MES的日硫化量")
    private Integer mesCapacity;

    @ApiModelProperty(value = "标准日硫化量")
    private Integer standardCapacity;

    @ApiModelProperty(value = "APS的日硫化量")
    private Integer apsCapacity;

    @ApiModelProperty(value = "总硫化时间(单位s)")
    private BigDecimal vulcanizationTime;

    @ApiModelProperty(value = "类型 01 模具关系 02 新模具到货计划")
    private String type;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "创建时间")
    private java.util.Date createTime;

    @ApiModelProperty(value = "更新人")
    private String updateBy;

    @ApiModelProperty(value = "更新时间")
    private java.util.Date updateTime;
}
