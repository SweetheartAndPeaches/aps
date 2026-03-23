package com.zlt.aps.cx.entity.schedule;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.cx.entity.base.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排程明细表（含车次标识）
 * 对应表：T_CX_SCHEDULE_DETAIL
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_CX_SCHEDULE_DETAIL")
@ApiModel(value = "排程明细对象", description = "排程明细表（含车次标识）")
public class CxScheduleDetail extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "所属主表ID")
    @TableField("MAIN_ID")
    private Long mainId;

    @ApiModelProperty(value = "计划日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("SCHEDULE_DATE")
    private LocalDate scheduleDate;

    @ApiModelProperty(value = "班次编码")
    @TableField("SHIFT_CODE")
    private String shiftCode;

    @ApiModelProperty(value = "成型机台编号")
    @TableField("CX_MACHINE_CODE")
    private String cxMachineCode;

    @ApiModelProperty(value = "成型机台名称")
    @TableField("CX_MACHINE_NAME")
    private String cxMachineName;

    @ApiModelProperty(value = "胎胚代码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    @ApiModelProperty(value = "车次号")
    @TableField("TRIP_NO")
    private Integer tripNo;

    @ApiModelProperty(value = "本车次容量（条）")
    @TableField("TRIP_CAPACITY")
    private Integer tripCapacity;

    @ApiModelProperty(value = "本车次实际完成数量")
    @TableField("TRIP_ACTUAL_QTY")
    private Integer tripActualQty;

    @ApiModelProperty(value = "顺位（全局排序号）")
    @TableField("SEQUENCE")
    private Integer sequence;

    @ApiModelProperty(value = "组内顺位")
    @TableField("SEQUENCE_IN_GROUP")
    private Integer sequenceInGroup;

    @ApiModelProperty(value = "库存可供硫化时长(小时)")
    @TableField("STOCK_HOURS_AT_CALC")
    private BigDecimal stockHoursAtCalc;

    @ApiModelProperty(value = "是否收尾：0-否 1-是")
    @TableField("IS_ENDING")
    private Integer isEnding;

    @ApiModelProperty(value = "是否试制：0-否 1-是")
    @TableField("IS_TRIAL")
    private Integer isTrial;

    @ApiModelProperty(value = "是否精度计划：0-否 1-是")
    @TableField("IS_PRECISION")
    private Integer isPrecision;

    @ApiModelProperty(value = "是否续作：0-否 1-是")
    @TableField("IS_CONTINUE")
    private Integer isContinue;

    @ApiModelProperty(value = "计划开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("PLAN_START_TIME")
    private LocalDateTime planStartTime;

    @ApiModelProperty(value = "计划结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("PLAN_END_TIME")
    private LocalDateTime planEndTime;
}
