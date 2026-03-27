package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 设备计划停机对象 t_mdm_device_plan_shut
 * 用于记录机台的计划停机时间（精度校验、润滑、巡检点检、维修等）
 *
 * @author APS Team
 */
@ApiModel(value = "设备计划停机对象", description = "设备计划停机对象")
@Data
@TableName(value = "T_MDM_DEVICE_PLAN_SHUT")
public class MdmDevicePlanShut extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号，字典：biz_factory_name
     */
    @Excel(name = "工厂编号", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 机台类型，字典：device_shut_machine_type；硫化、成型、压出、裁断、压延、密炼；
     */
    @Excel(name = "机台类型", dictType = "device_shut_machine_type")
    @ApiModelProperty(value = "机台类型", name = "machineType")
    @TableField(value = "MACHINE_TYPE")
    private String machineType;

    /**
     * 机台编号
     */
    @Excel(name = "机台编号")
    @ApiModelProperty(value = "机台编号", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    /**
     * 停机类型，字典：machine_stop_type；
     * 00-精度校验、01-润滑、02-巡检点检、03-预见性维护、04-预防性维护、05-计划性维修、06-临时性故障
     */
    @Excel(name = "停机类型", dictType = "machine_stop_type")
    @ApiModelProperty(value = "停机类型", name = "machineStopType")
    @TableField(value = "MACHINE_STOP_TYPE")
    private String machineStopType;

    /**
     * 开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "开始日期", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "开始日期", name = "beginDate")
    @TableField(value = "BEGIN_DATE")
    private Date beginDate;

    /**
     * 结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束日期", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "结束日期", name = "endDate")
    @TableField(value = "END_DATE")
    private Date endDate;

    /**
     * 备注
     */
    @Excel(name = "备注")
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
}
