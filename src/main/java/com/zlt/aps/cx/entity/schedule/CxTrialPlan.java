package com.zlt.aps.cx.entity.schedule;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 试制计划实体
 *
 * @author APS Team
 */
@Data
@TableName("cx_trial_plan")
public class CxTrialPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 计划日期 */
    private LocalDate planDate;

    /** 计划数量 */
    private Integer planQuantity;

    /** 试制目的（NEW_PRODUCT-新产品试制，PROCESS_TEST-工艺试验，MATERIAL_TEST-材料试验） */
    private String trialPurpose;

    /** 优先级 */
    private Integer priority;

    /** 状态（PENDING-待排程，SCHEDULED-已排程，IN_PROGRESS-进行中，COMPLETED-已完成） */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
