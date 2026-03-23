package com.zlt.aps.cx.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 生成排程DTO
 *
 * @author APS Team
 */
@Data
@Schema(description = "生成排程请求参数")
public class ScheduleGenerateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "计划日期", example = "2024-01-01", required = true)
    private LocalDate scheduleDate;

    @Schema(description = "计划天数", example = "3", required = true)
    private Integer days;

    @Schema(description = "是否覆盖已有排程", example = "false")
    private Boolean overwrite;

    @Schema(description = "排程类型：NORMAL-正常排程，PRECISION-精准排程")
    private String scheduleType;

    @Schema(description = "机台编号列表（可选，为空则排所有机台）")
    private java.util.List<String> machineCodes;
}
