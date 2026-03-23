package com.zlt.aps.cx.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 排程查询DTO
 *
 * @author APS Team
 */
@Data
@Schema(description = "排程查询参数")
public class ScheduleQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "成型机台编号")
    private String cxMachineCode;

    @Schema(description = "胎胚代码")
    private String embryoCode;

    @Schema(description = "产品结构")
    private String structureName;

    @Schema(description = "生产状态：0-未生产；1-生产中；2-已收尾")
    private String productionStatus;

    @Schema(description = "是否发布：0-未发布；1-已发布")
    private String isRelease;

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页数量", example = "10")
    private Integer pageSize = 10;
}
