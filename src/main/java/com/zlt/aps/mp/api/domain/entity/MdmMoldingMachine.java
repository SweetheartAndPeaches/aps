package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 成型机档案对象
 * 对应表：t_cx_machine
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@ApiModel(value = "成型机档案对象", description = "基础数据-成型机档案对象")
@TableName(value = "t_cx_machine", autoResultMap = true)
public class MdmMoldingMachine implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 成型机编码
     */
    @ApiModelProperty(value = "成型机编码")
    @TableField("machine_code")
    private String cxMachineCode;

    /**
     * 成型机名称
     */
    @ApiModelProperty(value = "成型机名称")
    @TableField("machine_name")
    private String cxMachineName;

    /**
     * 成型机类型
     */
    @ApiModelProperty(value = "成型机类型")
    @TableField("machine_type")
    private String cxMachineBrandCode;

    /**
     * 反包方式
     */
    @ApiModelProperty(value = "反包方式")
    @TableField("wrapping_type")
    private String rollOverType;

    /**
     * 是否有零度供料架
     */
    @ApiModelProperty(value = "是否有零度供料架")
    @TableField("has_zero_degree_feeder")
    private String isZeroRack;

    /**
     * 硫化机上限
     */
    @ApiModelProperty(value = "硫化机上限")
    @TableField("max_curing_machines")
    private Integer lhMachineMaxQty;

    /**
     * 设备最大日产量
     */
    @ApiModelProperty(value = "设备最大日产量")
    @TableField("max_daily_capacity")
    private Integer maxDayCapacity;

    /**
     * 每小时最大产能
     */
    @ApiModelProperty(value = "每小时最大产能")
    @TableField("max_capacity_per_hour")
    private Integer maxCapacityPerHour;

    /**
     * 产线编号
     */
    @ApiModelProperty(value = "产线编号")
    @TableField("line_number")
    private Integer lineNumber;

    /**
     * 是否启用：0-禁用 1-启用
     */
    @ApiModelProperty(value = "是否启用：0-禁用 1-启用")
    @TableField("is_active")
    private Integer isActive;

    /**
     * 状态
     */
    @ApiModelProperty(value = "状态")
    @TableField("status")
    private String status;

    /**
     * 固定规格1
     */
    @ApiModelProperty(value = "固定规格1")
    @TableField("fixed_structure1")
    private String fixedStructure1;

    /**
     * 固定规格2
     */
    @ApiModelProperty(value = "固定规格2")
    @TableField("fixed_structure2")
    private String fixedStructure2;

    /**
     * 固定规格3
     */
    @ApiModelProperty(value = "固定规格3")
    @TableField("fixed_structure3")
    private String fixedStructure3;

    // ==================== 非数据库字段 ====================

    /**
     * 工厂编号（兼容字段）
     */
    @ApiModelProperty(value = "工厂编号")
    @TableField(exist = false)
    private String factoryCode;

    /**
     * 展示机台编号
     */
    @ApiModelProperty(value = "展示机台编号")
    @TableField(exist = false)
    private String machineCode;

    /**
     * 展示机台名称
     */
    @ApiModelProperty(value = "展示机台名称")
    @TableField(exist = false)
    private String machineName;
    
    /**
     * 成型机类型名称（用于展示）
     */
    @ApiModelProperty(value = "成型机类型名称")
    @TableField(exist = false)
    private String cxMachineTypeName;

    /**
     * 机型（兼容字段）
     */
    @ApiModelProperty(value = "机型")
    @TableField(exist = false)
    private String cxMachineTypeCode;
}
