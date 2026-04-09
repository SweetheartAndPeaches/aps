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
 * 结构硫化比例配置
 *
 * @author APS Team
 */
@Data
@ApiModel(description = "结构硫化比例配置")
@TableName("T_MDM_STRUCTURE_LH_RATIO")
public class MdmStructureLhRatio implements Serializable {

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
     * 结构名称
     */
    @ApiModelProperty(value = "结构名称")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    /**
     * 硫化机台数量
     */
    @ApiModelProperty(value = "硫化机台数量")
    @TableField("LH_MACHINE_QTY")
    private Integer lhMachineQty;

    /**
     * 硫化比例
     */
    @ApiModelProperty(value = "硫化比例")
    @TableField("LH_RATIO")
    private BigDecimal lhRatio;

    /**
     * 日产能(条/天)
     */
    @ApiModelProperty(value = "日产能(条/天)")
    @TableField("DAILY_CAPACITY")
    private BigDecimal dailyCapacity;

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
