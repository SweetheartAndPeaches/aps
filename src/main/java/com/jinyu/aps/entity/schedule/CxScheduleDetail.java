package com.jinyu.aps.entity.schedule;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.jinyu.aps.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "排程明细对象")
public class CxScheduleDetail extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 所属主表ID
     */
    @Schema(description = "所属主表ID")
    @TableField("MAIN_ID")
    private Long mainId;

    // ========== 基本信息 ==========

    /**
     * 计划日期
     */
    @Schema(description = "计划日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("SCHEDULE_DATE")
    private LocalDate scheduleDate;

    /**
     * 班次编码
     */
    @Schema(description = "班次编码")
    @TableField("SHIFT_CODE")
    private String shiftCode;

    /**
     * 成型机台编号
     */
    @Schema(description = "成型机台编号")
    @TableField("CX_MACHINE_CODE")
    private String cxMachineCode;

    /**
     * 成型机台名称
     */
    @Schema(description = "成型机台名称")
    @TableField("CX_MACHINE_NAME")
    private String cxMachineName;

    /**
     * 胎胚代码
     */
    @Schema(description = "胎胚代码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    // ========== 车次标识核心字段 ==========

    /**
     * 车次号（第几车，如1,2,3...）
     */
    @Schema(description = "车次号")
    @TableField("TRIP_NO")
    private Integer tripNo;

    /**
     * 本车次容量（条）
     */
    @Schema(description = "本车次容量（条）")
    @TableField("TRIP_CAPACITY")
    private Integer tripCapacity;

    /**
     * 本车次实际完成数量
     */
    @Schema(description = "本车次实际完成数量")
    @TableField("TRIP_ACTUAL_QTY")
    private Integer tripActualQty;

    // ========== 顺位相关 ==========

    /**
     * 顺位（全局排序号）
     */
    @Schema(description = "顺位（全局排序号）")
    @TableField("SEQUENCE")
    private Integer sequence;

    /**
     * 组内顺位（同一物料分组内的排序）
     */
    @Schema(description = "组内顺位")
    @TableField("SEQUENCE_IN_GROUP")
    private Integer sequenceInGroup;

    // ========== 库存可供硫化时长 ==========

    /**
     * 计算顺位时的库存可供硫化时长（小时）
     */
    @Schema(description = "库存可供硫化时长(小时)")
    @TableField("STOCK_HOURS_AT_CALC")
    private BigDecimal stockHoursAtCalc;

    // ========== 特殊标记 ==========

    /**
     * 是否收尾：0-否 1-是
     */
    @Schema(description = "是否收尾：0-否 1-是")
    @TableField("IS_ENDING")
    private Integer isEnding;

    /**
     * 是否试制：0-否 1-是
     */
    @Schema(description = "是否试制：0-否 1-是")
    @TableField("IS_TRIAL")
    private Integer isTrial;

    /**
     * 是否精度计划：0-否 1-是
     */
    @Schema(description = "是否精度计划：0-否 1-是")
    @TableField("IS_PRECISION")
    private Integer isPrecision;

    /**
     * 是否续作：0-否 1-是
     */
    @Schema(description = "是否续作：0-否 1-是")
    @TableField("IS_CONTINUE")
    private Integer isContinue;

    // ========== 时间戳 ==========

    /**
     * 计划开始时间
     */
    @Schema(description = "计划开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("PLAN_START_TIME")
    private LocalDateTime planStartTime;

    /**
     * 计划结束时间
     */
    @Schema(description = "计划结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("PLAN_END_TIME")
    private LocalDateTime planEndTime;
}
