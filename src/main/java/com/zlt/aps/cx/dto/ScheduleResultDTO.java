package com.zlt.aps.cx.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "排程结果响应")
public class ScheduleResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "排程主表ID")
    private Long id;

    @Schema(description = "成型批次号")
    private String cxBatchNo;

    @Schema(description = "排程日期")
    private LocalDate scheduleDate;

    @Schema(description = "成型机台编号")
    private String cxMachineCode;

    @Schema(description = "成型机台名称")
    private String cxMachineName;

    @Schema(description = "胎胚代码")
    private String embryoCode;

    @Schema(description = "产品结构")
    private String structureName;

    @Schema(description = "规格描述")
    private String specDesc;

    @Schema(description = "胎胚总计划量")
    private BigDecimal productNum;

    @Schema(description = "胎胚库存")
    private BigDecimal totalStock;

    @Schema(description = "生产状态：0-未生产；1-生产中；2-已收尾")
    private String productionStatus;

    @Schema(description = "是否发布：0-未发布；1-已发布")
    private String isRelease;

    @Schema(description = "数据来源：0-自动排程；1-插单；2-导入")
    private String dataSource;

    // ========== 班次计划量 ==========
    @Schema(description = "一班计划数")
    private BigDecimal class1PlanQty;

    @Schema(description = "二班计划数")
    private BigDecimal class2PlanQty;

    @Schema(description = "三班计划数")
    private BigDecimal class3PlanQty;

    @Schema(description = "四班计划数")
    private BigDecimal class4PlanQty;

    @Schema(description = "五班计划数")
    private BigDecimal class5PlanQty;

    @Schema(description = "六班计划数")
    private BigDecimal class6PlanQty;

    @Schema(description = "七班计划数")
    private BigDecimal class7PlanQty;

    @Schema(description = "八班计划数")
    private BigDecimal class8PlanQty;

    // ========== 班次完成量 ==========
    @Schema(description = "一班完成量")
    private BigDecimal class1FinishQty;

    @Schema(description = "二班完成量")
    private BigDecimal class2FinishQty;

    @Schema(description = "三班完成量")
    private BigDecimal class3FinishQty;

    @Schema(description = "四班完成量")
    private BigDecimal class4FinishQty;

    @Schema(description = "五班完成量")
    private BigDecimal class5FinishQty;

    @Schema(description = "六班完成量")
    private BigDecimal class6FinishQty;

    @Schema(description = "七班完成量")
    private BigDecimal class7FinishQty;

    @Schema(description = "八班完成量")
    private BigDecimal class8FinishQty;

    @Schema(description = "排程明细列表")
    private List<ScheduleDetailDTO> details;

    /**
     * 排程明细DTO
     */
    @Data
    @Schema(description = "排程明细")
    public static class ScheduleDetailDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "明细ID")
        private Long id;

        @Schema(description = "班次编码")
        private String shiftCode;

        @Schema(description = "车次号")
        private Integer tripNo;

        @Schema(description = "本车次容量（条）")
        private Integer tripCapacity;

        @Schema(description = "本车次实际完成数量")
        private Integer tripActualQty;

        @Schema(description = "顺位（全局排序号）")
        private Integer sequence;

        @Schema(description = "库存可供硫化时长(小时)")
        private BigDecimal stockHoursAtCalc;

        @Schema(description = "是否收尾：0-否 1-是")
        private Integer isEnding;

        @Schema(description = "是否试制：0-否 1-是")
        private Integer isTrial;

        @Schema(description = "计划开始时间")
        private LocalDateTime planStartTime;

        @Schema(description = "计划结束时间")
        private LocalDateTime planEndTime;
    }
}
