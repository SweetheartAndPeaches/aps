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
 * 精度计划表 T_CX_PRECISION_PLAN
 */
@ApiModel(value = "精度计划对象", description = "精度计划表")
@Data
@TableName(value = "T_CX_PRECISION_PLAN")
public class CxPrecisionPlan extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "主键ID", name = "id")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /** 机台编号 */
    @ApiModelProperty(value = "机台编号", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    /** 精度计划日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "精度计划日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private LocalDate scheduleDate;

    /** 班次编码 */
    @ApiModelProperty(value = "班次编码", name = "shiftCode")
    @TableField(value = "SHIFT_CODE")
    private String shiftCode;

    /** 精度时长（分钟） */
    @ApiModelProperty(value = "精度时长(分钟)", name = "accuracyDuration")
    @TableField(value = "ACCURACY_DURATION")
    private Integer accuracyDuration;

    /** 备注 */
    @ApiModelProperty(value = "备注", name = "remark")
    @TableField(value = "REMARK")
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间", name = "createTime")
    @TableField(value = "CREATE_TIME")
    private LocalDateTime createTime;

    /** 创建人 */
    @ApiModelProperty(value = "创建人", name = "createBy")
    @TableField(value = "CREATE_BY")
    private String createBy;
}
