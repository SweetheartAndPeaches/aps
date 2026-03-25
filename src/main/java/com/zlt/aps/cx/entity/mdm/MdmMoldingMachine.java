package com.zlt.aps.cx.entity.mdm;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.cx.entity.base.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 成型机档案对象
 * 对应表：T_MDM_MOLDING_MACHINE
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_MOLDING_MACHINE")
@ApiModel(value = "成型机档案对象", description = "基础数据-成型机档案对象")
public class MdmMoldingMachine extends ApsBaseEntity {

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
     * 成型机编码
     */
    @ApiModelProperty(value = "成型机编码")
    @TableField("CX_MACHINE_CODE")
    private String cxMachineCode;

    /**
     * 成型机类型
     */
    @ApiModelProperty(value = "成型机类型")
    @TableField("CX_MACHINE_BRAND_CODE")
    private String cxMachineBrandCode;

    /**
     * 机型
     */
    @ApiModelProperty(value = "机型")
    @TableField("CX_MACHINE_TYPE_CODE")
    private String cxMachineTypeCode;

    /**
     * 反包方式
     */
    @ApiModelProperty(value = "反包方式")
    @TableField("ROLL_OVER_TYPE")
    private String rollOverType;

    /**
     * 是否有零度供料架 数据字典 biz_yes_no 1 是 0 否
     */
    @ApiModelProperty(value = "是否有零度供料架 数据字典 biz_yes_no 1 是 0 否")
    @TableField("IS_ZERO_RACK")
    private String isZeroRack;

    /**
     * 硫化机上限
     */
    @ApiModelProperty(value = "硫化机上限")
    @TableField("LH_MACHINE_MAX_QTY")
    private Integer lhMachineMaxQty;

    /**
     * 设备最大日产量
     */
    @ApiModelProperty(value = "设备最大日产量")
    @TableField("MAX_DAY_CAPACITY")
    private Integer maxDayCapacity;

    /**
     * 产线编号
     */
    @ApiModelProperty(value = "产线编号")
    @TableField("LINE_NUMBER")
    private Integer lineNumber;

    /**
     * 机台状态：RUNNING-正常运行 MAINTAIN-维护中 FAULT-故障 STOP-停用
     */
    @ApiModelProperty(value = "机台状态：RUNNING-正常运行 MAINTAIN-维护中 FAULT-故障 STOP-停用")
    @TableField("MAINTAIN_STATUS")
    private String maintainStatus;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;

    /**
     * 是否启用：0-禁用 1-启用
     */
    @ApiModelProperty(value = "是否启用：0-禁用 1-启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;

    // ==================== 非数据库字段 ====================

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
     * 成型机名称（用于展示）
     */
    @ApiModelProperty(value = "成型机名称")
    @TableField(exist = false)
    private String cxMachineName;
    
    /**
     * 成型机类型名称（用于展示）
     */
    @ApiModelProperty(value = "成型机类型名称")
    @TableField(exist = false)
    private String cxMachineTypeName;
}
