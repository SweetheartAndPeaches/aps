package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 成型精度计划实体（设备校准）
 * 
 * 品质部每周下发精度计划，指定哪些机台什么时候做精度校验。
 * - 每个机台每两个月做一次，每次4小时
 * - 正常提前3天安排
 * - 一天最多做2台
 * - 安排时段：胎胚库存够吃超过一个班→早班(7:30-11:30)；特殊情况→中班(13:00-17:00)
 * 
 * 精度期间成型机停机，系统需判断硫化机是否减产：
 * - 胎胚库存够硫化机吃4小时以上→硫化机继续生产
 * - 不够→硫化机减产一半，等精度做完恢复
 *
 * @author APS Team
 */
@Data
@TableName("t_cx_precision_plan")
@ApiModel(value = "成型精度计划", description = "成型机台精度校验计划")
public class CxPrecisionPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "机台编码")
    @TableField("MACHINE_CODE")
    private String machineCode;

    @ApiModelProperty(value = "机台名称")
    @TableField("MACHINE_NAME")
    private String machineName;

    @ApiModelProperty(value = "计划日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("PLAN_DATE")
    private LocalDate planDate;

    @ApiModelProperty(value = "计划班次：SHIFT_DAY-早班，SHIFT_AFTERNOON-中班")
    @TableField("PLAN_SHIFT")
    private String planShift;

    @ApiModelProperty(value = "计划开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("PLAN_START_TIME")
    private LocalDateTime planStartTime;

    @ApiModelProperty(value = "计划结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("PLAN_END_TIME")
    private LocalDateTime planEndTime;

    @ApiModelProperty(value = "预计时长（小时），默认4小时")
    @TableField("ESTIMATED_HOURS")
    private Integer estimatedHours;

    @ApiModelProperty(value = "上次精度日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("LAST_PRECISION_DATE")
    private LocalDate lastPrecisionDate;

    @ApiModelProperty(value = "到期日期（下次应做精度日期）")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("DUE_DATE")
    private LocalDate dueDate;

    @ApiModelProperty(value = "状态：PLANNED-已计划，IN_PROGRESS-进行中，COMPLETED-已完成，CANCELLED-已取消")
    @TableField("STATUS")
    private String status;

    @ApiModelProperty(value = "安排原因：SCHEDULED-正常安排，URGENT-紧急安排，RESCHEDULED-重排")
    @TableField("ARRANGE_REASON")
    private String arrangeReason;

    @ApiModelProperty(value = "是否影响硫化：0-否 1-是")
    @TableField("AFFECT_VULCANIZE")
    private Integer affectVulcanize;

    @ApiModelProperty(value = "硫化减产比例（0-1），0表示不减产，0.5表示减半")
    @TableField("VULCANIZE_REDUCE_RATIO")
    private java.math.BigDecimal vulcanizeReduceRatio;

    @ApiModelProperty(value = "关联胎胚编码（主要生产的胎胚）")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "创建人")
    @TableField("CREATE_BY")
    private String createBy;
}
