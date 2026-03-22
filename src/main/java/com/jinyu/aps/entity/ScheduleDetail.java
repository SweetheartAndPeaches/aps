package com.jinyu.aps.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排程明细实体类
 *
 * @author APS Team
 */
@Data
@TableName(value = "t_cx_schedule_detail", keepGlobalPrefix = false)
@Schema(description = "排程明细")
public class ScheduleDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属主表ID")
    @TableField("main_id")
    private Long mainId;

    @Schema(description = "计划日期")
    @TableField("schedule_date")
    private LocalDate scheduleDate;

    @Schema(description = "班次编码")
    @TableField("shift_code")
    private String shiftCode;

    @Schema(description = "成型机台编号")
    @TableField("machine_code")
    private String machineCode;

    @Schema(description = "胎胚物料编码")
    @TableField("material_code")
    private String materialCode;

    @Schema(description = "计划量")
    @TableField("plan_quantity")
    private Integer planQuantity;

    @Schema(description = "完成量")
    @TableField("completed_quantity")
    private Integer completedQuantity;

    @Schema(description = "物料分组ID")
    @TableField("material_group_id")
    private String materialGroupId;

    @Schema(description = "车次号")
    @TableField("trip_no")
    private Integer tripNo;

    @Schema(description = "车内序号")
    @TableField("trip_sequence")
    private Integer tripSequence;

    @Schema(description = "车次分组ID")
    @TableField("trip_group_id")
    private String tripGroupId;

    @Schema(description = "本车次容量")
    @TableField("trip_capacity")
    private Integer tripCapacity;

    @Schema(description = "本车次实际完成数量")
    @TableField("trip_actual_qty")
    private Integer tripActualQty;

    @Schema(description = "车次状态")
    @TableField("trip_status")
    private String tripStatus;

    @Schema(description = "车次创建时间")
    @TableField("trip_create_time")
    private LocalDateTime tripCreateTime;

    @Schema(description = "车次齐套时间")
    @TableField("trip_complete_time")
    private LocalDateTime tripCompleteTime;

    @Schema(description = "顺位")
    @TableField("sequence")
    private Integer sequence;

    @Schema(description = "组内顺位")
    @TableField("sequence_in_group")
    private Integer sequenceInGroup;

    @Schema(description = "计算顺位时的库存可供硫化时长")
    @TableField("stock_hours_at_calc")
    private BigDecimal stockHoursAtCalc;

    @Schema(description = "生产方式")
    @TableField("production_mode")
    private String productionMode;

    @Schema(description = "是否收尾")
    @TableField("is_ending")
    private Integer isEnding;

    @Schema(description = "是否投产")
    @TableField("is_starting")
    private Integer isStarting;

    @Schema(description = "是否试制")
    @TableField("is_trial")
    private Integer isTrial;

    @Schema(description = "是否精度计划")
    @TableField("is_precision")
    private Integer isPrecision;

    @Schema(description = "是否续作")
    @TableField("is_continue")
    private Integer isContinue;

    @Schema(description = "紧急收尾标识(0-否,1-是)")
    @TableField("is_urgent_ending")
    private Integer isUrgentEnding;

    @Schema(description = "关键产品标识(0-否,1-是)")
    @TableField("is_key_product")
    private Integer isKeyProduct;

    @Schema(description = "收尾天数")
    @TableField("ending_days")
    private BigDecimal endingDays;

    @Schema(description = "状态")
    @TableField("status")
    private String status;

    @Schema(description = "计划开始时间")
    @TableField("plan_start_time")
    private LocalDateTime planStartTime;

    @Schema(description = "计划结束时间")
    @TableField("plan_end_time")
    private LocalDateTime planEndTime;

    @Schema(description = "实际开始时间")
    @TableField("actual_start_time")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际结束时间")
    @TableField("actual_end_time")
    private LocalDateTime actualEndTime;

    @Schema(description = "优先级")
    @TableField("priority")
    private Integer priority;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;

    @Schema(description = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    // ==================== 扩展字段（不映射到数据库） ====================

    @TableField(exist = false)
    private String productStructure;

    @TableField(exist = false)
    private Boolean isMainProduct;
}
