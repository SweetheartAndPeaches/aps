package com.zlt.aps.cx.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 重排程请求DTO
 *
 * @author APS Team
 */
@Data
@Schema(description = "重排程请求参数")
public class ReScheduleRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "排程日期", example = "2024-01-01", required = true)
    private LocalDate scheduleDate;

    @Schema(description = "重排程类型：MACHINE_BREAKDOWN-机台故障，URGENT_ORDER-紧急订单，MATERIAL_SHORTAGE-物料短缺", required = true)
    private String reScheduleType;

    @Schema(description = "重排程原因")
    private String reason;

    @Schema(description = "受影响的机台编码列表")
    private List<String> affectedMachineCodes;

    @Schema(description = "紧急订单编号（当类型为URGENT_ORDER时）")
    private String urgentOrderId;

    @Schema(description = "故障机台编码（当类型为MACHINE_BREAKDOWN时）")
    private String breakdownMachineCode;

    @Schema(description = "预计恢复时间（当类型为MACHINE_BREAKDOWN时）")
    private LocalDate estimatedRecoveryDate;
}
