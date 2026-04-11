package com.zlt.aps.cx.entity.schedule;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

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

     @ApiModelProperty(value = "一班车次号")
    @TableField("CLASS1_TRIP_NO")
    private String class1TripNo;

    @ApiModelProperty(value = "一班车次容量（整车条数）")
    @TableField("CLASS1_TRIP_CAPACITY")
    private BigDecimal class1TripCapacity;

    @ApiModelProperty(value = "一班库存可供硫化时长")
    @TableField("CLASS1_STOCK_HOURS")
    private BigDecimal class1StockHours;

    @ApiModelProperty(value = "一班顺位")
    @TableField("CLASS1_SEQUENCE")
    private Integer class1Sequence;

    @ApiModelProperty(value = "一班计划开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("CLASS1_PLAN_START_TIME")
    private Date class1PlanStartTime;

    @ApiModelProperty(value = "一班计划结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("CLASS1_PLAN_END_TIME")
    private Date class1PlanEndTime;

      @ApiModelProperty(value = "二班车次号")
    @TableField("CLASS2_TRIP_NO")
    private String class2TripNo;

    @ApiModelProperty(value = "二班车次容量（整车条数）")
    @TableField("CLASS2_TRIP_CAPACITY")
    private BigDecimal class2TripCapacity;

    @ApiModelProperty(value = "二班库存可供硫化时长")
    @TableField("CLASS2_STOCK_HOURS")
    private BigDecimal class2StockHours;

    @ApiModelProperty(value = "二班顺位")
    @TableField("CLASS2_SEQUENCE")
    private Integer class2Sequence;

    @ApiModelProperty(value = "二班计划开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("CLASS2_PLAN_START_TIME")
    private Date class2PlanStartTime;

    @ApiModelProperty(value = "二班计划结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("CLASS2_PLAN_END_TIME")
    private Date class2PlanEndTime;

     @ApiModelProperty(value = "三班车次号")
    @TableField("CLASS3_TRIP_NO")
    private String class3TripNo;

    @ApiModelProperty(value = "三班车次容量（整车条数）")
    @TableField("CLASS3_TRIP_CAPACITY")
    private BigDecimal class3TripCapacity;

    @ApiModelProperty(value = "三班库存可供硫化时长")
    @TableField("CLASS3_STOCK_HOURS")
    private BigDecimal class3StockHours;

    @ApiModelProperty(value = "三班顺位")
    @TableField("CLASS3_SEQUENCE")
    private Integer class3Sequence;

    @ApiModelProperty(value = "三班计划开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("CLASS3_PLAN_START_TIME")
    private Date class3PlanStartTime;

    @ApiModelProperty(value = "三班计划结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("CLASS3_PLAN_END_TIME")
    private Date class3PlanEndTime;

    
    @ApiModelProperty(value = "四班车次号")
    @TableField("CLASS4_TRIP_NO")
    private String class4TripNo;

    @ApiModelProperty(value = "四班车次容量（整车条数）")
    @TableField("CLASS4_TRIP_CAPACITY")
    private BigDecimal class4TripCapacity;

    @ApiModelProperty(value = "四班库存可供硫化时长")
    @TableField("CLASS4_STOCK_HOURS")
    private BigDecimal class4StockHours;

    @ApiModelProperty(value = "四班顺位")
    @TableField("CLASS4_SEQUENCE")
    private Integer class4Sequence;

    @ApiModelProperty(value = "四班计划开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("CLASS4_PLAN_START_TIME")
    private Date class4PlanStartTime;

    @ApiModelProperty(value = "四班计划结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("CLASS4_PLAN_END_TIME")
    private Date class4PlanEndTime;

      @ApiModelProperty(value = "五班车次号")
    @TableField("CLASS5_TRIP_NO")
    private String class5TripNo;

    @ApiModelProperty(value = "五班车次容量（整车条数）")
    @TableField("CLASS5_TRIP_CAPACITY")
    private BigDecimal class5TripCapacity;

    @ApiModelProperty(value = "五班库存可供硫化时长")
    @TableField("CLASS5_STOCK_HOURS")
    private BigDecimal class5StockHours;

    @ApiModelProperty(value = "五班顺位")
    @TableField("CLASS5_SEQUENCE")
    private Integer class5Sequence;

    @ApiModelProperty(value = "五班计划开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("CLASS5_PLAN_START_TIME")
    private Date class5PlanStartTime;

    @ApiModelProperty(value = "五班计划结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("CLASS5_PLAN_END_TIME")
    private Date class5PlanEndTime;

    @ApiModelProperty(value = "六班车次号")
    @TableField("CLASS6_TRIP_NO")
    private String class6TripNo;

    @ApiModelProperty(value = "六班车次容量（整车条数）")
    @TableField("CLASS6_TRIP_CAPACITY")
    private BigDecimal class6TripCapacity;

    @ApiModelProperty(value = "六班库存可供硫化时长")
    @TableField("CLASS6_STOCK_HOURS")
    private BigDecimal class6StockHours;

    @ApiModelProperty(value = "六班顺位")
    @TableField("CLASS6_SEQUENCE")
    private Integer class6Sequence;

    @ApiModelProperty(value = "六班计划开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("CLASS6_PLAN_START_TIME")
    private Date class6PlanStartTime;

    @ApiModelProperty(value = "六班计划结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("CLASS6_PLAN_END_TIME")
    private Date class6PlanEndTime;

    
    @ApiModelProperty(value = "七班车次号")
    @TableField("CLASS7_TRIP_NO")
    private String class7TripNo;

    @ApiModelProperty(value = "七班车次容量（整车条数）")
    @TableField("CLASS7_TRIP_CAPACITY")
    private BigDecimal class7TripCapacity;

    @ApiModelProperty(value = "七班库存可供硫化时长")
    @TableField("CLASS7_STOCK_HOURS")
    private BigDecimal class7StockHours;

    @ApiModelProperty(value = "七班顺位")
    @TableField("CLASS7_SEQUENCE")
    private Integer class7Sequence;

    @ApiModelProperty(value = "七班计划开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("CLASS7_PLAN_START_TIME")
    private Date class7PlanStartTime;

    @ApiModelProperty(value = "七班计划结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("CLASS7_PLAN_END_TIME")
    private Date class7PlanEndTime;

    @ApiModelProperty(value = "八班车次号")
    @TableField("CLASS8_TRIP_NO")
    private String class8TripNo;

    @ApiModelProperty(value = "八班车次容量（整车条数）")
    @TableField("CLASS8_TRIP_CAPACITY")
    private BigDecimal class8TripCapacity;

    @ApiModelProperty(value = "八班库存可供硫化时长")
    @TableField("CLASS8_STOCK_HOURS")
    private BigDecimal class8StockHours;

    @ApiModelProperty(value = "八班顺位")
    @TableField("CLASS8_SEQUENCE")
    private Integer class8Sequence;

    @ApiModelProperty(value = "八班计划开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("CLASS8_PLAN_START_TIME")
    private Date class8PlanStartTime;

    @ApiModelProperty(value = "八班计划结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("CLASS8_PLAN_END_TIME")
    private Date class8PlanEndTime;
}
