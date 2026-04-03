package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 操作工请假记录实体
 *
 * @author APS Team
 */
@Data
@TableName("cx_operator_leave")
public class CxOperatorLeave {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 机台编码 */
    private String machineCode;

    /** 员工编码 */
    private String employeeCode;

    /** 员工姓名 */
    private String employeeName;

    /** 班次编码 */
    private String shiftCode;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 请假类型 */
    private String leaveType;

    /** 是否影响产能（0-否，1-是） */
    private Integer affectCapacity;

    /** 审批状态（PENDING-待审批，APPROVED-已审批，REJECTED-已拒绝） */
    private String approvalStatus;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
