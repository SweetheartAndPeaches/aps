package com.zlt.aps.cx.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 排程查询VO
 *
 * @author APS Team
 */
@Data
@ApiModel(description = "排程查询参数")
public class ScheduleQueryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "开始日期")
    private LocalDate startDate;

    @ApiModelProperty(value = "结束日期")
    private LocalDate endDate;

    @ApiModelProperty(value = "成型机台编号")
    private String cxMachineCode;

    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    @ApiModelProperty(value = "产品结构")
    private String structureName;

    @ApiModelProperty(value = "生产状态：0-未生产；1-生产中；2-已收尾")
    private String productionStatus;

    @ApiModelProperty(value = "是否发布：0-未发布；1-已发布")
    private String isRelease;

    @ApiModelProperty(value = "页码", example = "1")
    private Integer pageNum = 1;

    @ApiModelProperty(value = "每页数量", example = "10")
    private Integer pageSize = 10;
}
