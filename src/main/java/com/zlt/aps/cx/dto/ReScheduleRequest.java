package com.zlt.aps.cx.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 重排程请求DTO
 *
 * @author APS Team
 */
@Data
@ApiModel(description = "重排程请求参数")
public class ReScheduleRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "排程日期", example = "2024-01-01", required = true)
    private LocalDate scheduleDate;

    @ApiModelProperty(value = "重排程原因")
    private String reason;

    @ApiModelProperty(value = "紧急订单编号（可选）")
    private String urgentOrderId;

    @ApiModelProperty(value = "故障机台编码（可选）")
    private String breakdownMachineCode;

    @ApiModelProperty(value = "预计恢复时间（可选）")
    private LocalDate estimatedRecoveryDate;
}
