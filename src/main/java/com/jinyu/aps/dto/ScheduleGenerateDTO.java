package com.jinyu.aps.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 生成排程DTO
 *
 * @author APS Team
 */
@Data
@Schema(description = "生成排程请求参数")
public class ScheduleGenerateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "计划日期", example = "2024-01-01T00:00:00", required = true)
    private LocalDateTime scheduleDate;

    @Schema(description = "计划天数", example = "3", required = true)
    private Integer days;
}
