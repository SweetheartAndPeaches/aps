package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排程明细表 T_CX_SCHEDULE_DETAIL
 */
@ApiModel(value = "排程明细对象", description = "排程明细表（含车次标识）")
@Data
@TableName(value = "T_CX_SCHEDULE_DETAIL")
public class CxScheduleDetail extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "主键ID", name = "id")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /** 所属主表ID */
    @ApiModelProperty(value = "所属主表ID", name = "mainId")
    @TableField(value = "MAIN_ID")
    private Long mainId;

    // ========== 基本信息 ==========
    /** 计划日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private LocalDate scheduleDate;

    /** 班次编码 */
    @ApiModelProperty(value = "班次编码", name = "shiftCode")
    @TableField(value = "SHIFT_CODE")
    private String shiftCode;

    /** 成型机台编号 */
    @ApiModelProperty(value = "成型机台编号", name = "cxMachineCode")
    @TableField(value = "CX_MACHINE_CODE")
    private String cxMachineCode;

    /** 成型机台名称 */
    @ApiModelProperty(value = "成型机台名称", name = "cxMachineName")
    @TableField(value = "CX_MACHINE_NAME")
    private String cxMachineName;

    /** 胎胚代码 */
    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    // ========== 车次标识核心字段 ==========
    /** 车次号（第几车，如1,2,3...） */
    @ApiModelProperty(value = "车次号", name = "tripNo")
    @TableField(value = "TRIP_NO")
    private Integer tripNo;

    /** 本车次容量（条） */
    @ApiModelProperty(value = "本车次容量", name = "tripCapacity")
    @TableField(value = "TRIP_CAPACITY")
    private Integer tripCapacity;

    /** 本车次实际完成数量 */
    @ApiModelProperty(value = "本车次实际完成数量", name = "tripActualQty")
    @TableField(value = "TRIP_ACTUAL_QTY")
    private Integer tripActualQty;

    // ========== 顺位相关 ==========
    /** 顺位（全局排序号） */
    @ApiModelProperty(value = "顺位", name = "sequence")
    @TableField(value = "SEQUENCE")
    private Integer sequence;

    /** 组内顺位（同一物料分组内的排序） */
    @ApiModelProperty(value = "组内顺位", name = "sequenceInGroup")
    @TableField(value = "SEQUENCE_IN_GROUP")
    private Integer sequenceInGroup;

    // ========== 库存可供硫化时长 ==========
    /** 计算顺位时的库存可供硫化时长（小时） */
    @ApiModelProperty(value = "库存可供硫化时长(小时)", name = "stockHoursAtCalc")
    @TableField(value = "STOCK_HOURS_AT_CALC")
    private BigDecimal stockHoursAtCalc;

    // ========== 特殊标记 ==========
    /** 是否收尾：0-否 1-是 */
    @ApiModelProperty(value = "是否收尾", name = "isEnding")
    @TableField(value = "IS_ENDING")
    private Integer isEnding;

    /** 是否试制：0-否 1-是 */
    @ApiModelProperty(value = "是否试制", name = "isTrial")
    @TableField(value = "IS_TRIAL")
    private Integer isTrial;

    /** 是否精度计划：0-否 1-是 */
    @ApiModelProperty(value = "是否精度计划", name = "isPrecision")
    @TableField(value = "IS_PRECISION")
    private Integer isPrecision;

    /** 是否续作：0-否 1-是 */
    @ApiModelProperty(value = "是否续作", name = "isContinue")
    @TableField(value = "IS_CONTINUE")
    private Integer isContinue;

    // ========== 时间戳 ==========
    /** 计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "计划开始时间", name = "planStartTime")
    @TableField(value = "PLAN_START_TIME")
    private LocalDateTime planStartTime;

    /** 计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "计划结束时间", name = "planEndTime")
    @TableField(value = "PLAN_END_TIME")
    private LocalDateTime planEndTime;
}
