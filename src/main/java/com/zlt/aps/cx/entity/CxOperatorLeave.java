package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 操作工请假记录实体类
 * 对应数据库表：T_CX_OPERATOR_LEAVE
 * 
 * 用于管理操作工请假信息，影响机台产能分配。
 *
 * @author APS Team
 */
@Data
@TableName(value = "T_CX_OPERATOR_LEAVE", keepGlobalPrefix = false)
@Schema(description = "操作工请假记录")
public class CxOperatorLeave implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @Schema(description = "员工编号")
    @TableField("EMPLOYEE_NO")
    private String employeeNo;

    @Schema(description = "员工姓名")
    @TableField("EMPLOYEE_NAME")
    private String employeeName;

    @Schema(description = "所属机台编码")
    @TableField("MACHINE_CODE")
    private String machineCode;

    @Schema(description = "请假类型：ANNUAL(年假)/SICK(病假)/PERSONAL(事假)/MATERNITY(产假)/OTHER(其他)")
    @TableField("LEAVE_TYPE")
    private String leaveType;

    @Schema(description = "请假开始日期")
    @TableField("START_DATE")
    private LocalDate startDate;

    @Schema(description = "请假结束日期")
    @TableField("END_DATE")
    private LocalDate endDate;

    @Schema(description = "请假天数")
    @TableField("LEAVE_DAYS")
    private Integer leaveDays;

    @Schema(description = "影响班次（多个班次用逗号分隔）")
    @TableField("AFFECTED_SHIFTS")
    private String affectedShifts;

    @Schema(description = "是否影响产能：0-否，1-是")
    @TableField("AFFECT_CAPACITY")
    private Integer affectCapacity;

    @Schema(description = "产能影响比例（0-100）")
    @TableField("CAPACITY_IMPACT_RATIO")
    private Integer capacityImpactRatio;

    @Schema(description = "请假原因")
    @TableField("REASON")
    private String reason;

    @Schema(description = "审批状态：PENDING(待审批)/APPROVED(已批准)/REJECTED(已拒绝)/CANCELLED(已取消)")
    @TableField("APPROVAL_STATUS")
    private String approvalStatus;

    @Schema(description = "审批人")
    @TableField("APPROVER")
    private String approver;

    @Schema(description = "审批时间")
    @TableField("APPROVAL_TIME")
    private LocalDateTime approvalTime;

    @Schema(description = "审批意见")
    @TableField("APPROVAL_COMMENT")
    private String approvalComment;

    @Schema(description = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
