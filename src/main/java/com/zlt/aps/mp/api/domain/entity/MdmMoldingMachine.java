package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMoldingMachine.java
 * 描    述：基础数据-成型机档案对象 t_mdm_molding_machine
 *@author zlt
 *@date 2025-12-14
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "基础数据-成型机档案对象", description = "基础数据-成型机档案对象 ")
@Data
@TableName(value = "T_MDM_MOLDING_MACHINE")
public class MdmMoldingMachine extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂编号 */
    @Excel(name = "ui.data.column.mdmMoldingMachine.factoryCode", dictType = "biz_factory_name", sort = 1)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 成型机编码 */
    @Excel(name = "ui.data.column.mdmMoldingMachine.cxMachineCode", sort = 2)
    @ApiModelProperty(value = "成型机编码", name = "cxMachineCode")
    @TableField(value = "CX_MACHINE_CODE")
    private String cxMachineCode;

    /** 成型机名称 */
    @Excel(name = "ui.data.column.mdmMoldingMachine.cxMachineName", sort = 3)
    @ApiModelProperty(value = "成型机名称", name = "cxMachineName")
    @TableField(value = "CX_MACHINE_NAME")
    private String cxMachineName;

    /** 成型机类型 */
    @Excel(name = "ui.data.column.mdmMoldingMachine.cxMachineBrandCode",dictType = "biz_machine_brand", sort = 4)
    @ApiModelProperty(value = "成型机类型", name = "cxMachineBrandCode")
    @TableField(value = "CX_MACHINE_BRAND_CODE")
    private String cxMachineBrandCode;

    /** 机型 */
    @Excel(name = "ui.data.column.mdmMoldingMachine.cxMachineTypeCode", dictType = "cx_machine_type_code", sort = 3)
    @ApiModelProperty(value = "机型", name = "cxMachineTypeCode")
    @TableField(value = "CX_MACHINE_TYPE_CODE")
    private String cxMachineTypeCode;

    /** 反包方式 */
    @Excel(name = "ui.data.column.mdmMoldingMachine.rollOverType", dictType = "ROLL_OVER_TYPE", sort = 5)
    @ApiModelProperty(value = "反包方式", name = "rollOverType")
    @TableField(value = "ROLL_OVER_TYPE")
    private String rollOverType;

    /** 是否有零度供料架 数据字典 biz_yes_no 1 是 0 否 */
    @Excel(name = "ui.data.column.mdmMoldingMachine.isZeroRack", dictType = "biz_yes_no", sort = 6)
    @ApiModelProperty(value = "是否有零度供料架 数据字典 biz_yes_no 1 是 0 否", name = "isZeroRack")
    @TableField(value = "IS_ZERO_RACK")
    private String isZeroRack;

    /** 硫化机上限 */
    @Excel(name = "ui.data.column.mdmMoldingMachine.lhMachineMaxQty", sort = 7)
    @ApiModelProperty(value = "硫化机上限", name = "lhMachineMaxQty")
    @TableField(value = "LH_MACHINE_MAX_QTY")
    private Integer lhMachineMaxQty;

    /** 设备最大日产量 */
    @Excel(name = "ui.data.column.mdmMoldingMachine.maxDayCapacity", sort = 8)
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

    @ImportExcelValidated(maxLength = 500)
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", name = "remark")
    @TableField(value = "REMARK")
    private String remark;


    /**
     * 是否启用：0-禁用 1-启用
     */
    @ApiModelProperty(value = "是否启用：0-禁用 1-启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;

    /**
     * 删除标志：0-未删除 1-已删除
     */
    @ApiModelProperty(value = "删除标志：0-未删除 1-已删除")
    @TableField("IS_DELETE")
    @TableLogic(value = "0", delval = "1")
    private String isDelete;

}
