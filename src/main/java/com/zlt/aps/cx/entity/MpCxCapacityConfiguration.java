package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 成型产能分配配置(结构)对象
 * 对应表：T_MP_STRUCTURE_ALLOCATION
 *
 * @author APS Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MP_STRUCTURE_ALLOCATION")
@ApiModel(value = "成型产能分配配置(结构)对象", description = "成型产能分配配置(结构)对象")
public class MpCxCapacityConfiguration extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编码
     */
    @ApiModelProperty(value = "工厂编码")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @ApiModelProperty(value = "年份")
    @TableField("YEAR")
    private Integer year;

    /**
     * 月份
     */
    @ApiModelProperty(value = "月份")
    @TableField("MONTH")
    private Integer month;

    /**
     * 销售生产需求计划版本
     */
    @ApiModelProperty(value = "销售生产需求计划版本")
    @TableField("MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 工厂排产版本
     */
    @ApiModelProperty(value = "工厂排产版本")
    @TableField("PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 计划类型 01 正常 02 订单预测 03 实单模拟
     */
    @ApiModelProperty(value = "计划类型")
    @TableField("PLAN_TYPE")
    private String planType;

    /**
     * 产品结构
     */
    @ApiModelProperty(value = "产品结构")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    /**
     * 排产净需求
     */
    @ApiModelProperty(value = "总需求量")
    @TableField("NET_QTY")
    private Long netQty;

    /**
     * 排产净需求(含损耗)
     */
    @ApiModelProperty(value = "净需求量")
    @TableField("LOSS_QTY")
    private Long lossQty;

    /**
     * 成型机编码
     */
    @ApiModelProperty(value = "成型机编码")
    @TableField("CX_MACHINE_CODE")
    private String cxMachineCode;

    /**
     * 开始日期
     */
    @ApiModelProperty(value = "开始日期")
    @TableField("BEGIN_DAY")
    private Integer beginDay;

    /**
     * 结束日期
     */
    @ApiModelProperty(value = "结束日期")
    @TableField("END_DAY")
    private Integer endDay;

    /**
     * 分配天数
     */
    @ApiModelProperty(value = "分配天数")
    @TableField("ALLOT_DAYS")
    private Integer allotDays;
}
