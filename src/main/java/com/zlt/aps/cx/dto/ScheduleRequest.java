package com.zlt.aps.cx.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 排程请求DTO
 *
 * @author APS Team
 */
@Data
@ApiModel(description = "排程请求参数")
public class ScheduleRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "排程日期", example = "2024-01-01", required = true)
    private LocalDate scheduleDate;

    @ApiModelProperty(value = "排程模式：NORMAL-正常排程，RE_SCHEDULE-重排程，STRUCTURE_RE_SCHEDULE-结构重排")
    private String scheduleMode;

    @ApiModelProperty(value = "是否覆盖已有排程")
    private Boolean overwrite;

    @ApiModelProperty(value = "排程类型：NORMAL-正常排程，PRECISION-精准排程")
    private String scheduleType;
}
