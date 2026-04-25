package com.zlt.aps.cx.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 排程明细VO - 包含主表关联信息
 *
 * @author APS Team
 */
@Data
public class CxScheduleDetailVo {

    // ==================== 子表字段 ====================
    /** 主键ID */
    private Long id;

    /** 所属主表ID */
    private Long mainId;

    // 一班~八班计划数
    private BigDecimal class1PlanQty;
    private BigDecimal class2PlanQty;
    private BigDecimal class3PlanQty;
    private BigDecimal class4PlanQty;
    private BigDecimal class5PlanQty;
    private BigDecimal class6PlanQty;
    private BigDecimal class7PlanQty;
    private BigDecimal class8PlanQty;

    // 一班~八班车次号
    private String class1TripNo;
    private String class2TripNo;
    private String class3TripNo;
    private String class4TripNo;
    private String class5TripNo;
    private String class6TripNo;
    private String class7TripNo;
    private String class8TripNo;

    // 一班~八班车次容量
    private BigDecimal class1TripCapacity;
    private BigDecimal class2TripCapacity;
    private BigDecimal class3TripCapacity;
    private BigDecimal class4TripCapacity;
    private BigDecimal class5TripCapacity;
    private BigDecimal class6TripCapacity;
    private BigDecimal class7TripCapacity;
    private BigDecimal class8TripCapacity;

    // 一班~八班库存可供硫化时长
    private BigDecimal class1StockHours;
    private BigDecimal class2StockHours;
    private BigDecimal class3StockHours;
    private BigDecimal class4StockHours;
    private BigDecimal class5StockHours;
    private BigDecimal class6StockHours;
    private BigDecimal class7StockHours;
    private BigDecimal class8StockHours;

    // 一班~八班顺位
    private Integer class1Sequence;
    private Integer class2Sequence;
    private Integer class3Sequence;
    private Integer class4Sequence;
    private Integer class5Sequence;
    private Integer class6Sequence;
    private Integer class7Sequence;
    private Integer class8Sequence;

    // 一班~八班计划开始时间
    private LocalDateTime class1PlanStartTime;
    private LocalDateTime class2PlanStartTime;
    private LocalDateTime class3PlanStartTime;
    private LocalDateTime class4PlanStartTime;
    private LocalDateTime class5PlanStartTime;
    private LocalDateTime class6PlanStartTime;
    private LocalDateTime class7PlanStartTime;
    private LocalDateTime class8PlanStartTime;

    // 一班~八班计划结束时间
    private LocalDateTime class1PlanEndTime;
    private LocalDateTime class2PlanEndTime;
    private LocalDateTime class3PlanEndTime;
    private LocalDateTime class4PlanEndTime;
    private LocalDateTime class5PlanEndTime;
    private LocalDateTime class6PlanEndTime;
    private LocalDateTime class7PlanEndTime;
    private LocalDateTime class8PlanEndTime;

    // ==================== 主表字段（关联信息） ====================
    /** 成型机台编号 */
    private String cxMachineCode;

    /** 成型机台名称 */
    private String cxMachineName;

    /** 成型机台类型 */
    private String cxMachineType;

    /** 硫化机台编号 */
    private String lhMachineCode;

    /** 硫化机台名称 */
    private String lhMachineName;

    /** 硫化机台数量 */
    private Integer lhMachineQty;

    /** 胎胚编码 */
    private String embryoCode;

    /** 物料编码 */
    private String materialCode;

    /** 物料描述 */
    private String materialDesc;

    /** 主物料描述 */
    private String mainMaterialDesc;

    /** 规格尺寸 */
    private String specDimension;

    /** 结构名称 */
    private String structureName;

    /** 排程日期 */
    private String scheduleDate;

    /** 批次号 */
    private String cxBatchNo;

    /** 订单号 */
    private String orderNo;

    /** 排产状态 */
    private String productionStatus;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
