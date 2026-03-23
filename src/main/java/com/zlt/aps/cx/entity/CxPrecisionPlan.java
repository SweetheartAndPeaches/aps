package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 精度计划实体
 *
 * @author APS Team
 */
@Data
@TableName("cx_precision_plan")
public class CxPrecisionPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 机台编码 */
    private String machineCode;

    /** 计划日期 */
    private LocalDate planDate;

    /** 计划班次 */
    private String planShift;

    /** 预计时长（小时） */
    private Integer estimatedHours;

    /** 状态（PLANNED-已计划，IN_PROGRESS-进行中，COMPLETED-已完成） */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
