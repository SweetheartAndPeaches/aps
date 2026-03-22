package com.jinyu.aps.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 试制任务实体类
 * 
 * 用于管理试制量试插单任务。
 * 试制任务具有较高优先级，可以插单处理。
 *
 * @author APS Team
 */
@Data
@TableName(value = "t_cx_trial_task", keepGlobalPrefix = false)
@Schema(description = "试制任务")
public class TrialTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "试制单号")
    @TableField("trial_no")
    private String trialNo;

    @Schema(description = "胎胚物料编码")
    @TableField("material_code")
    private String materialCode;

    @Schema(description = "物料名称")
    @TableField("material_name")
    private String materialName;

    @Schema(description = "结构编码")
    @TableField("structure_code")
    private String structureCode;

    @Schema(description = "试制数量")
    @TableField("trial_quantity")
    private Integer trialQuantity;

    @Schema(description = "已生产数量")
    @TableField("produced_quantity")
    private Integer producedQuantity;

    @Schema(description = "期望开始日期")
    @TableField("expected_start_date")
    private LocalDate expectedStartDate;

    @Schema(description = "期望完成日期")
    @TableField("expected_end_date")
    private LocalDate expectedEndDate;

    @Schema(description = "优先级（1-10，数字越大优先级越高）")
    @TableField("priority")
    private Integer priority;

    @Schema(description = "试制类型：NEW_PRODUCT(新产品)/PROCESS_TEST(工艺试验)/MATERIAL_TEST(材料试验)")
    @TableField("trial_type")
    private String trialType;

    @Schema(description = "插单类型：NORMAL(正常排程)/URGENT(紧急插单)/FORCE(强制插单)")
    @TableField("insert_type")
    private String insertType;

    @Schema(description = "指定机台编码（可空，系统自动分配）")
    @TableField("assigned_machine")
    private String assignedMachine;

    @Schema(description = "试制原因/备注")
    @TableField("reason")
    private String reason;

    @Schema(description = "状态：PENDING(待排程)/SCHEDULED(已排程)/IN_PROGRESS(生产中)/COMPLETED(已完成)/CANCELLED(已取消)")
    @TableField("status")
    private String status;

    @Schema(description = "申请人")
    @TableField("applicant")
    private String applicant;

    @Schema(description = "审批人")
    @TableField("approver")
    private String approver;

    @Schema(description = "审批时间")
    @TableField("approve_time")
    private LocalDateTime approveTime;

    @Schema(description = "实际开始时间")
    @TableField("actual_start_time")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际完成时间")
    @TableField("actual_end_time")
    private LocalDateTime actualEndTime;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
