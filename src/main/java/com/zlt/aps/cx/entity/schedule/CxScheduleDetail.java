package com.zlt.aps.cx.entity.schedule;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
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
@TableName("t_cx_schedule_detail")
@ApiModel(value = "排程明细对象", description = "排程明细表（含车次标识）")
public class CxScheduleDetail extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "所属主表ID")
    @TableField("MAIN_ID")
    private Long mainId;

    @ApiModelProperty(value = "成型机台编码")
    @TableField("CX_MACHINE_CODE")
    private String cxMachineCode;

    @ApiModelProperty(value = "班次编码")
    @TableField("SHIFT_CODE")
    private String shiftCode;

    @ApiModelProperty(value = "计划日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("SCHEDULE_DATE")
    private LocalDate scheduleDate;

    @ApiModelProperty(value = "胎胚代码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    /**
     * 主物料(胎胚描述)
     */
    @ApiModelProperty(value = "主物料(胎胚描述)", name = "mainMaterialDesc")
    @TableField(value = "MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    @ApiModelProperty(value = "物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    @ApiModelProperty(value = "计划数量")
    @TableField("PLAN_QTY")
    private Integer planQty;

    @ApiModelProperty(value = "车次号")
    @TableField("TRIP_NO")
    private Integer tripNo;

    @ApiModelProperty(value = "本车次容量（条）")
    @TableField("TRIP_CAPACITY")
    private Integer tripCapacity;

    @ApiModelProperty(value = "库存可供硫化时长(小时)")
    @TableField("STOCK_HOURS_AT_CALC")
    private BigDecimal stockHoursAtCalc;

    @ApiModelProperty(value = "顺位")
    @TableField("SEQUENCE")
    private Integer sequence;

    @ApiModelProperty(value = "组内顺位")
    @TableField("SEQUENCE_IN_GROUP")
    private Integer sequenceInGroup;

    @ApiModelProperty(value = "车次实际完成数量")
    @TableField("TRIP_ACTUAL_QTY")
    private Integer tripActualQty;

    @ApiModelProperty(value = "计划开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("PLAN_START_TIME")
    private LocalDateTime planStartTime;

    @ApiModelProperty(value = "计划结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("PLAN_END_TIME")
    private LocalDateTime planEndTime;
}
