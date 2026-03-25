package com.zlt.aps.cx.entity.mdm;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.cx.entity.base.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 成型机台信息对象
 * 对应表：T_MDM_MOLDING_MACHINE
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_MOLDING_MACHINE")
@ApiModel(value = "成型机台信息对象", description = "成型机台信息对象")
public class MdmMoldingMachine extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 成型机编码
     */
    @ApiModelProperty(value = "成型机编码")
    @TableField("CX_MACHINE_CODE")
    private String cxMachineCode;

    /**
     * 成型机名称
     */
    @ApiModelProperty(value = "成型机名称")
    @TableField("CX_MACHINE_NAME")
    private String cxMachineName;

    /**
     * 成型机类型编码
     */
    @ApiModelProperty(value = "成型机类型编码")
    @TableField("CX_MACHINE_TYPE_CODE")
    private String cxMachineTypeCode;

    /**
     * 成型机类型名称
     */
    @ApiModelProperty(value = "成型机类型名称")
    @TableField("CX_MACHINE_TYPE_NAME")
    private String cxMachineTypeName;

    /**
     * 机型型号
     */
    @ApiModelProperty(value = "机型型号")
    @TableField("CX_MACHINE_MODEL")
    private String cxMachineModel;

    /**
     * 反包方式
     */
    @ApiModelProperty(value = "反包方式")
    @TableField("WRAPPING_TYPE")
    private String wrappingType;

    /**
     * 是否有零度供料架：0-否 1-是
     */
    @ApiModelProperty(value = "是否有零度供料架：0-否 1-是")
    @TableField("HAS_ZERO_DEGREE_FEEDER")
    private Integer hasZeroDegreeFeeder;

    /**
     * 设备最大日产能（条）
     * 
     * 注：具体结构的产能请查询 CxMachineStructureCapacity 表
     * 当前在产结构请查询 CxMachineCurrentStatus 表
     */
    @ApiModelProperty(value = "设备最大日产能（条）")
    @TableField("MAX_DAILY_CAPACITY")
    private Integer maxDailyCapacity;

    /**
     * 对应硫化机上限数量
     */
    @ApiModelProperty(value = "对应硫化机上限数量")
    @TableField("MAX_CURING_MACHINES")
    private Integer maxCuringMachines;

    /**
     * 产线编号
     */
    @ApiModelProperty(value = "产线编号")
    @TableField("LINE_NUMBER")
    private Integer lineNumber;

    /**
     * 机台状态：0-正常 1-维护中 2-故障 3-停用
     * 
     * 注：机台维护时间从 CxPrecisionPlan（精度计划）获取
     */
    @ApiModelProperty(value = "机台状态：0-正常 1-维护中 2-故障 3-停用")
    @TableField("MAINTAIN_STATUS")
    private String maintainStatus;

    /**
     * 排产限制说明
     */
    @ApiModelProperty(value = "排产限制说明")
    @TableField("PRODUCTION_RESTRICTION")
    private String productionRestriction;

    /**
     * 是否启用：0-禁用 1-启用
     */
    @ApiModelProperty(value = "是否启用：0-禁用 1-启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;
}
