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
 * 成型机台
 *
 * @author APS Team
 */
@Data
@ApiModel(description = "成型机台")
@TableName("T_MDM_MOLDING_MACHINE")
public class MdmMoldingMachine implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 成型机台编码
     */
    @ApiModelProperty(value = "成型机台编码")
    @TableField("CX_MACHINE_CODE")
    private String cxMachineCode;

    /**
     * 成型机台名称
     */
    @ApiModelProperty(value = "成型机台名称")
    @TableField("CX_MACHINE_NAME")
    private String cxMachineName;

    /**
     * 机台类型
     */
    @ApiModelProperty(value = "机台类型")
    @TableField("MACHINE_TYPE")
    private String machineType;

    /**
     * 班次产能(条/班)
     */
    @ApiModelProperty(value = "班次产能(条/班)")
    @TableField("SHIFT_CAPACITY")
    private BigDecimal shiftCapacity;

    /**
     * 小时产能(条/小时)
     */
    @ApiModelProperty(value = "小时产能(条/小时)")
    @TableField("HOURLY_CAPACITY")
    private BigDecimal hourlyCapacity;

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
     * 是否删除
     */
    @ApiModelProperty(value = "是否删除(0-未删除 1-已删除)")
    @TableField("IS_DELETE")
    private String isDelete;

    /**
     * 排序号
     */
    @ApiModelProperty(value = "排序号")
    @TableField("SORT_ORDER")
    private Integer sortOrder;

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
