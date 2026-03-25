package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排程主表实体
 * 对应数据库表：T_CX_SCHEDULE_MAIN
 *
 * @author APS Team
 */
@Data
@TableName("T_CX_SCHEDULE_MAIN")
@ApiModel(value = "排程主表", description = "排程主表")
public class CxScheduleMain implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "排程日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("SCHEDULE_DATE")
    private LocalDate scheduleDate;

    @ApiModelProperty(value = "排程类型：NORMAL-正常排程，PRECISION-精准排程，TRIAL-试制排程")
    @TableField("SCHEDULE_TYPE")
    private String scheduleType;

    @ApiModelProperty(value = "状态：PLANNED-已计划，CONFIRMED-已确认，RELEASED-已发布，CANCELED-已取消")
    @TableField("STATUS")
    private String status;

    @ApiModelProperty(value = "总任务数")
    @TableField("TOTAL_TASKS")
    private Integer totalTasks;

    @ApiModelProperty(value = "已完成任务数")
    @TableField("COMPLETED_TASKS")
    private Integer completedTasks;

    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;

    @ApiModelProperty(value = "创建人")
    @TableField("CREATE_BY")
    private String createBy;

    @ApiModelProperty(value = "创建时间")
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新人")
    @TableField("UPDATE_BY")
    private String updateBy;

    @ApiModelProperty(value = "更新时间")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
