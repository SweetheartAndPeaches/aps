package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 基础数据-成型机档案对象 t_mdm_molding_machine
 */
@ApiModel(value = "基础数据-成型机档案对象", description = "基础数据-成型机档案对象")
@Data
@TableName(value = "T_MDM_MOLDING_MACHINE")
public class MdmMoldingMachine extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 成型机编码 */
    @ApiModelProperty(value = "成型机编码", name = "cxMachineCode")
    @TableField(value = "CX_MACHINE_CODE")
    private String cxMachineCode;

    /** 成型机类型 */
    @ApiModelProperty(value = "成型机类型", name = "cxMachineBrandCode")
    @TableField(value = "CX_MACHINE_BRAND_CODE")
    private String cxMachineBrandCode;

    /** 机型 */
    @ApiModelProperty(value = "机型", name = "cxMachineTypeCode")
    @TableField(value = "CX_MACHINE_TYPE_CODE")
    private String cxMachineTypeCode;

    /** 反包方式 */
    @ApiModelProperty(value = "反包方式", name = "rollOverType")
    @TableField(value = "ROLL_OVER_TYPE")
    private String rollOverType;

    /** 是否有零度供料架 数据字典 biz_yes_no 1 是 0 否 */
    @ApiModelProperty(value = "是否有零度供料架 数据字典 biz_yes_no 1 是 0 否", name = "isZeroRack")
    @TableField(value = "IS_ZERO_RACK")
    private String isZeroRack;

    /** 硫化机上限 */
    @ApiModelProperty(value = "硫化机上限", name = "lhMachineMaxQty")
    @TableField(value = "LH_MACHINE_MAX_QTY")
    private Integer lhMachineMaxQty;

    /** 设备最大日产量 */
    @ApiModelProperty(value = "设备最大日产量", name = "maxDayCapacity")
    @TableField(value = "MAX_DAY_CAPACITY")
    private Integer maxDayCapacity;

    /**
     * 展示机台编号
     */
    @ApiModelProperty(value = "展示机台编号", name = "machineCode")
    @TableField(exist = false)
    private String machineCode;

    /**
     * 展示机台名称
     */
    @ApiModelProperty(value = "展示机台名称", name = "machineCode")
    @TableField(exist = false)
    private String machineName;

    /** 备注 */
    @ApiModelProperty(value = "备注", name = "remark")
    @TableField(value = "REMARK")
    private String remark;
}
