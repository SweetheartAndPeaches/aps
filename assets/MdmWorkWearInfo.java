package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 成型鼓(工装)台账对象 t_mdm_work_wear_info
 */
@ApiModel(value = "成型鼓(工装)台账对象", description = "成型鼓(工装)台账对象")
@Data
@TableName(value = "T_MDM_WORK_WEAR_INFO")
public class MdmWorkWearInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 工装分类 01 成型鼓 02 胎体鼓 */
    @ApiModelProperty(value = "工装分类", name = "workWearType")
    @TableField(value = "WORK_WEAR_TYPE")
    private String workWearType;

    /** 工装状态 1 可用 0 禁用 */
    @ApiModelProperty(value = "工装状态", name = "workWearStatus")
    @TableField(value = "WORK_WEAR_STATUS")
    private String workWearStatus;

    /** 工装名称 */
    @ApiModelProperty(value = "工装名称", name = "workWearName")
    @TableField(value = "WORK_WEAR_NAME")
    private String workWearName;

    /** 成型鼓类型 01 软控 02 赛象 03 青岛贝帆 */
    @ApiModelProperty(value = "成型鼓类型", name = "cxMachineBrandCode")
    @TableField(value = "CX_MACHINE_BRAND_CODE")
    private String cxMachineBrandCode;

    /** 类型 01 机械 02 宽基 */
    @ApiModelProperty(value = "类型", name = "cxMachineTypeCode")
    @TableField(value = "CX_MACHINE_TYPE_CODE")
    private String cxMachineTypeCode;

    /** 成型鼓周长上限 */
    @ApiModelProperty(value = "成型鼓周长上限", name = "perimeterMax")
    @TableField(value = "PERIMETER_MAX")
    private Integer perimeterMax;

    /** 成型鼓周长下限 */
    @ApiModelProperty(value = "成型鼓周长下限", name = "perimeterMin")
    @TableField(value = "PERIMETER_MIN")
    private Integer perimeterMin;

    /** 规格 */
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /** 数量 */
    @ApiModelProperty(value = "数量", name = "qty")
    @TableField(value = "QTY")
    private Integer qty;

    /** 单位 01 套 */
    @ApiModelProperty(value = "单位", name = "unit")
    @TableField(value = "UNIT")
    private String unit;

    /** 适用机型 */
    @ApiModelProperty(value = "适用机型", name = "usedType")
    @TableField(value = "USED_TYPE")
    private String usedType;

    /** 备注 */
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
}
