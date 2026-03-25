package com.zlt.aps.cx.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 排程结果DTO
 *
 * @author APS Team
 */
@Data
@ApiModel(description = "排程结果响应")
public class ScheduleResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "排程主表ID")
    private Long id;

    @ApiModelProperty(value = "成型批次号")
    private String cxBatchNo;

    @ApiModelProperty(value = "排程日期")
    private LocalDate scheduleDate;

    @ApiModelProperty(value = "成型机台编号")
    private String cxMachineCode;

    @ApiModelProperty(value = "成型机台名称")
    private String cxMachineName;

    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    @ApiModelProperty(value = "产品结构")
    private String structureName;

    @ApiModelProperty(value = "规格描述")
    private String specDesc;

    @ApiModelProperty(value = "胎胚总计划量")
    private BigDecimal productNum;

    @ApiModelProperty(value = "胎胚库存")
    private BigDecimal totalStock;

    @ApiModelProperty(value = "生产状态：0-未生产；1-生产中；2-已收尾")
    private String productionStatus;

    @ApiModelProperty(value = "是否发布：0-未发布；1-已发布")
    private String isRelease;

    @ApiModelProperty(value = "数据来源：0-自动排程；1-插单；2-导入")
    private String dataSource;

    // ========== 班次计划量 ==========
    @ApiModelProperty(value = "一班计划数")
    private BigDecimal class1PlanQty;

    @ApiModelProperty(value = "二班计划数")
    private BigDecimal class2PlanQty;

    @ApiModelProperty(value = "三班计划数")
    private BigDecimal class3PlanQty;

    @ApiModelProperty(value = "四班计划数")
    private BigDecimal class4PlanQty;

    @ApiModelProperty(value = "五班计划数")
    private BigDecimal class5PlanQty;

    @ApiModelProperty(value = "六班计划数")
    private BigDecimal class6PlanQty;

    @ApiModelProperty(value = "七班计划数")
    private BigDecimal class7PlanQty;

    @ApiModelProperty(value = "八班计划数")
    private BigDecimal class8PlanQty;

    // ========== 班次完成量 ==========
    @ApiModelProperty(value = "一班完成量")
    private BigDecimal class1FinishQty;

    @ApiModelProperty(value = "二班完成量")
    private BigDecimal class2FinishQty;

    @ApiModelProperty(value = "三班完成量")
    private BigDecimal class3FinishQty;

    @ApiModelProperty(value = "四班完成量")
    private BigDecimal class4FinishQty;

    @ApiModelProperty(value = "五班完成量")
    private BigDecimal class5FinishQty;

    @ApiModelProperty(value = "六班完成量")
    private BigDecimal class6FinishQty;

    @ApiModelProperty(value = "七班完成量")
    private BigDecimal class7FinishQty;

    @ApiModelProperty(value = "八班完成量")
    private BigDecimal class8FinishQty;

    @ApiModelProperty(value = "排程明细列表")
    private List<ScheduleDetailDTO> details;

    /**
     * 排程明细DTO
     */
    @Data
    @ApiModel(value = "排程明细")
    public static class ScheduleDetailDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "明细ID")
        private Long id;

        @ApiModelProperty(value = "班次编码")
        private String shiftCode;

        @ApiModelProperty(value = "车次号")
        private Integer tripNo;

        @ApiModelProperty(value = "本车次容量（条）")
        private Integer tripCapacity;

        @ApiModelProperty(value = "本车次实际完成数量")
        private Integer tripActualQty;

        @ApiModelProperty(value = "顺位（全局排序号）")
        private Integer sequence;

        @ApiModelProperty(value = "库存可供硫化时长(小时)")
        private BigDecimal stockHoursAtCalc;

        @ApiModelProperty(value = "是否收尾：0-否 1-是")
        private Integer isEnding;

        @ApiModelProperty(value = "是否试制：0-否 1-是")
        private Integer isTrial;

        @ApiModelProperty(value = "计划开始时间")
        private LocalDateTime planStartTime;

        @ApiModelProperty(value = "计划结束时间")
        private LocalDateTime planEndTime;
    }
}
