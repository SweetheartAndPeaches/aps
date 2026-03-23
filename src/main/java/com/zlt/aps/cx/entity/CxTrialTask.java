package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 试制任务实体类
 * 对应数据库表：T_CX_TRIAL_TASK
 * 
 * 用于管理试制量试插单任务。
 * 试制任务具有较高优先级，可以插单处理。
 *
 * @author APS Team
 */
@Data
@TableName(value = "T_CX_TRIAL_TASK", keepGlobalPrefix = false)
@Schema(description = "试制任务")
public class CxTrialTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @Schema(description = "试制单号")
    @TableField("TRIAL_NO")
    private String trialNo;

    @Schema(description = "胎胚物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    @Schema(description = "物料名称")
    @TableField("MATERIAL_NAME")
    private String materialName;

    @Schema(description = "结构编码")
    @TableField("STRUCTURE_CODE")
    private String structureCode;

    @Schema(description = "试制数量")
    @TableField("TRIAL_QUANTITY")
    private Integer trialQuantity;

    @Schema(description = "已生产数量")
    @TableField("PRODUCED_QUANTITY")
    private Integer producedQuantity;

    @Schema(description = "期望开始日期")
    @TableField("EXPECTED_START_DATE")
    private LocalDate expectedStartDate;

    @Schema(description = "期望完成日期")
    @TableField("EXPECTED_END_DATE")
    private LocalDate expectedEndDate;

    @Schema(description = "优先级（1-10，数字越大优先级越高）")
    @TableField("PRIORITY")
    private Integer priority;

    @Schema(description = "试制类型：NEW_PRODUCT(新产品)/PROCESS_TEST(工艺试验)/MATERIAL_TEST(材料试验)")
    @TableField("TRIAL_TYPE")
    private String trialType;

    @Schema(description = "插单类型：NORMAL(正常排程)/URGENT(紧急插单)/FORCE(强制插单)")
    @TableField("INSERT_TYPE")
    private String insertType;

    @Schema(description = "指定机台编码（可空，系统自动分配）")
    @TableField("ASSIGNED_MACHINE")
    private String assignedMachine;

    @Schema(description = "试制原因/备注")
    @TableField("REASON")
    private String reason;

    @Schema(description = "状态：PENDING(待排程)/SCHEDULED(已排程)/IN_PROGRESS(生产中)/COMPLETED(已完成)/CANCELLED(已取消)")
    @TableField("STATUS")
    private String status;

    @Schema(description = "申请人")
    @TableField("APPLICANT")
    private String applicant;

    @Schema(description = "审批人")
    @TableField("APPROVER")
    private String approver;

    @Schema(description = "审批时间")
    @TableField("APPROVE_TIME")
    private LocalDateTime approveTime;

    @Schema(description = "实际开始时间")
    @TableField("ACTUAL_START_TIME")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际完成时间")
    @TableField("ACTUAL_END_TIME")
    private LocalDateTime actualEndTime;

    @Schema(description = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
