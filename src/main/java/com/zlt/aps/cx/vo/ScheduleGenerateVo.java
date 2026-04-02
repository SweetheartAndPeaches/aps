package com.zlt.aps.cx.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 生成排程VO
 *
 * @author APS Team
 */
@Data
@ApiModel(description = "生成排程请求参数")
public class ScheduleGenerateVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "计划日期", example = "2024-01-01", required = true)
    private LocalDate scheduleDate;

    @ApiModelProperty(value = "计划天数", example = "3", required = true)
    private Integer days;

    @ApiModelProperty(value = "是否覆盖已有排程", example = "false")
    private Boolean overwrite;

    @ApiModelProperty(value = "排程类型：NORMAL-正常排程，PRECISION-精准排程")
    private String scheduleType;

    @ApiModelProperty(value = "机台编号列表（可选，为空则排所有机台）")
    private java.util.List<String> machineCodes;
}
