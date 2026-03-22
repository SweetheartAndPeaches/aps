package com.jinyu.aps.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 操作工请假记录实体类
 * 
 * 用于管理操作工请假信息，影响机台产能分配。
 *
 * @author APS Team
 */
@Data
@TableName(value = "t_cx_operator_leave", keepGlobalPrefix = false)
@Schema(description = "操作工请假记录")
public class OperatorLeave implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "员工编号")
    @TableField("employee_no")
    private String employeeNo;

    @Schema(description = "员工姓名")
    @TableField("employee_name")
    private String employeeName;

    @Schema(description = "所属机台编码")
    @TableField("machine_code")
    private String machineCode;

    @Schema(description = "请假类型：ANNUAL(年假)/SICK(病假)/PERSONAL(事假)/MATERNITY(产假)/OTHER(其他)")
    @TableField("leave_type")
    private String leaveType;

    @Schema(description = "请假开始日期")
    @TableField("start_date")
    private LocalDate startDate;

    @Schema(description = "请假结束日期")
    @TableField("end_date")
    private LocalDate endDate;

    @Schema(description = "请假天数")
    @TableField("leave_days")
    private Integer leaveDays;

    @Schema(description = "影响班次（多个班次用逗号分隔）")
    @TableField("affected_shifts")
    private String affectedShifts;

    @Schema(description = "是否影响产能：0-否，1-是")
    @TableField("affect_capacity")
    private Integer affectCapacity;

    @Schema(description = "产能影响比例（0-100）")
    @TableField("capacity_impact_ratio")
    private Integer capacityImpactRatio;

    @Schema(description = "请假原因")
    @TableField("reason")
    private String reason;

    @Schema(description = "审批状态：PENDING(待审批)/APPROVED(已批准)/REJECTED(已拒绝)/CANCELLED(已取消)")
    @TableField("approval_status")
    private String approvalStatus;

    @Schema(description = "审批人")
    @TableField("approver")
    private String approver;

    @Schema(description = "审批时间")
    @TableField("approval_time")
    private LocalDateTime approvalTime;

    @Schema(description = "审批意见")
    @TableField("approval_comment")
    private String approvalComment;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
