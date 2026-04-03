package com.zlt.aps.cx.entity.schedule;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 硫化排程结果表
 * 对应表：T_LH_SCHEDULE_RESULT
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_LH_SCHEDULE_RESULT")
@ApiModel(value = "硫化排程结果对象", description = "硫化排程结果表")
public class LhScheduleResult extends ApsBaseEntity {

    private static final long serialVersionUID = 2597208202828961196L;

    /**
     * 分厂编号
     */
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    @Excel(name = "ui.data.column.result.factoryCode", sort = 1)
    private String factoryCode;

    /**
     * 自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号
     */
    @ApiModelProperty(value = "自动排程批次号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /**
     * 工单号，自动生成（工序+日期+三位顺序号001,002）
     */
    @ApiModelProperty(value = "工单号", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /**
     * 月度计划单号
     */
    @ApiModelProperty(value = "月度计划单号", name = "MONTH_PLAN_NO")
    @TableField(value = "MONTH_PLAN_NO")
    private String monthPlanNo;

    /**
     * 月度计划版本号
     */
    @ApiModelProperty(value = "月度计划版本号", name = "MONTH_PLAN_VERSION")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 硫化机台编号
     */
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "硫化机台编号", name = "lhMachineCode")
    @TableField(value = "LH_MACHINE_CODE")
    @ImportValidated(required = true, maxLength = 20)
    @Excel(name = "ui.data.column.scheduleResult.lhMachineCode", sort = 2)
    private String lhMachineCode;

    /**
     * 规格描述信息
     */
    @ApiModelProperty(value = "规格描述信息", name = "specDesc")
    @TableField(value = "SPEC_DESC")
    @Excel(name = "ui.data.column.scheduleResult.specDesc", sort = 3)
    private String specDesc;

    /**
     * 存储当前左右模情况，如果非单模单规格的则可为空，单模单规格则存储对应的模信息，如：存储内容，L/R、L1/R1
     */
    @ApiModelProperty(value = "左右模", name = "leftRightMold")
    @TableField(value = "LEFT_RIGHT_MOLD")
    @Excel(name = "ui.data.column.scheduleResult.leftRightMold", sort = 40)
    private String leftRightMold;

    /**
     * 硫化机台名称
     */
    @ApiModelProperty(value = "硫化机台名称", name = "lhMachineName")
    @TableField(value = "LH_MACHINE_NAME")
    private String lhMachineName;

    /**
     * 物料编号
     */
    @ApiModelProperty(value = "物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    @Excel(name = "ui.data.column.result.productCode", sort = 50)
    private String productCode;

    /**
     * 规格代码
     */
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "规格代码", name = "specCode")
    @TableField(value = "SPEC_CODE")
    @ImportValidated(maxLength = 66)
    @Excel(name = "ui.data.column.scheduleResult.specCode", sort = 5)
    private String specCode;

    /**
     * 胎胚代码
     */
    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    @Excel(name = "ui.data.column.scheduleResult.embryoCode", sort = 7)
    private String embryoCode;

    /**
     * 胎胚库存
     */
    @ApiModelProperty(value = "胎胚库存", name = "embryoStock")
    @TableField(value = "EMBRYO_STOCK")
    @Excel(name = "ui.data.column.scheduleResult.embryoStock", cellType = Excel.ColumnType.NUMERIC, sort = 52)
    private Integer embryoStock;

    /**
     * 硫化时长
     */
    @ApiModelProperty(value = "硫化时长", name = "lhTime")
    @TableField(value = "LH_TIME")
    @Excel(name = "ui.data.column.scheduleResult.lhTime", cellType = Excel.ColumnType.NUMERIC, sort = 53)
    private BigDecimal lhTime;

    /**
     * 月度计划日需求量
     */
    @ApiModelProperty(value = "月度计划日需求量", name = "dailyPlanQty")
    @TableField(value = "DAILY_PLAN_QTY")
    @Excel(name = "ui.data.column.scheduleResult.dailyPlanQty", cellType = Excel.ColumnType.NUMERIC, sort = 40)
    private Integer dailyPlanQty;

    /**
     * 月度计划模数
     */
    @ApiModelProperty(value = "月度计划模数")
    @TableField(value = "MP_MOLD_QTY")
    private Integer mpMoldQty;

    /**
     * 使用模数
     */
    @ApiModelProperty(value = "使用模数")
    @TableField(value = "MOLD_QTY")
    private Integer moldQty;

    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "实际排程日期", name = "realScheduleDate")
    @TableField(value = "REAL_SCHEDULE_DATE")
    private Date realScheduleDate;

    /**
     * 规格结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "规格结束时间", name = "specEndTime")
    @TableField(value = "SPEC_END_TIME")
    private Date specEndTime;

    /**
     * T日规格结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "T日规格结束时间", name = "tDaySpecEndTime")
    @TableField(value = "TDAY_SPEC_END_TIME")
    private Date tDaySpecEndTime;

    /**
     * 生产状态:0-未生产；1-生产中；2-生产完成
     */
    @ApiModelProperty(value = "生产状态", name = "productionStatus")
    @TableField(value = "PRODUCTION_STATUS")
    private String productionStatus;

    /**
     * 一班计划量
     */
    @ApiModelProperty(value = "一班计划量", name = "class1PlanQty")
    @TableField(value = "CLASS1_PLAN_QTY")
    @Excel(name = "ui.data.column.lhScheduleResult.class1PlanQty", cellType = Excel.ColumnType.NUMERIC, sort = 11)
    private Integer class1PlanQty;

    /**
     * 一班计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "一班计划开始时间", name = "class1StartTime")
    @TableField(value = "CLASS1_START_TIME")
    private Date class1StartTime;

    /**
     * 一班计划结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "一班计划结束时间", name = "class1EndTime")
    @TableField(value = "CLASS1_END_TIME")
    private Date class1EndTime;

    /**
     * 一班原因分析
     */
    @ApiModelProperty(value = "一班原因分析", name = "class1Analysis")
    @TableField(value = "CLASS1_ANALYSIS")
    @ImportValidated(maxLength = 66)
    @Excel(name = "ui.data.column.lhScheduleResult.class1Analysis", sort = 15)
    private String class1Analysis;

    /**
     * 一班完成量
     */
    @ApiModelProperty(value = "一班完成量", name = "class1FinishQty")
    @TableField(value = "CLASS1_FINISH_QTY")
    @Excel(name = "ui.data.column.lhScheduleResult.class1FinishQty", cellType = Excel.ColumnType.NUMERIC, sort = 13)
    private Integer class1FinishQty;

    /**
     * 二班计划量
     */
    @ApiModelProperty(value = "二班计划量", name = "class2PlanQty")
    @TableField(value = "CLASS2_PLAN_QTY")
    @Excel(name = "ui.data.column.lhScheduleResult.class2PlanQty", cellType = Excel.ColumnType.NUMERIC, sort = 12)
    private Integer class2PlanQty;

    /**
     * 二班计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "二班计划开始时间", name = "class2StartTime")
    @TableField(value = "CLASS2_START_TIME")
    private Date class2StartTime;

    /**
     * 二班计划结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "二班计划结束时间", name = "class2EndTime")
    @TableField(value = "CLASS2_END_TIME")
    private Date class2EndTime;

    /**
     * 二班原因分析
     */
    @ApiModelProperty(value = "二班原因分析", name = "class2Analysis")
    @TableField(value = "CLASS2_ANALYSIS")
    @ImportValidated(maxLength = 66)
    @Excel(name = "ui.data.column.lhScheduleResult.class2Analysis", sort = 16)
    private String class2Analysis;

    /**
     * 二班完成量
     */
    @ApiModelProperty(value = "二班完成量", name = "class2FinishQty")
    @TableField(value = "CLASS2_FINISH_QTY")
    @Excel(name = "ui.data.column.lhScheduleResult.class2FinishQty", cellType = Excel.ColumnType.NUMERIC, sort = 14)
    private Integer class2FinishQty;

    /**
     * 三班计划量
     */
    @ApiModelProperty(value = "三班计划量", name = "class3PlanQty")
    @TableField(value = "CLASS3_PLAN_QTY")
    @Excel(name = "ui.data.column.lhScheduleResult.class3PlanQty", cellType = Excel.ColumnType.NUMERIC, sort = 17)
    private Integer class3PlanQty;

    /**
     * 三班计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "三班计划开始时间", name = "class3StartTime")
    @TableField(value = "CLASS3_START_TIME")
    private Date class3StartTime;

    /**
     * 三班计划结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "三班计划结束时间", name = "class3EndTime")
    @TableField(value = "CLASS3_END_TIME")
    private Date class3EndTime;

    /**
     * 三班原因分析
     */
    @ApiModelProperty(value = "三班原因分析", name = "class3Analysis")
    @TableField(value = "CLASS3_ANALYSIS")
    @Excel(name = "ui.data.column.lhScheduleResult.class3Analysis", sort = 19)
    private String class3Analysis;

    /**
     * 三班完成量
     */
    @ApiModelProperty(value = "三班完成量", name = "class3FinishQty")
    @TableField(value = "CLASS3_FINISH_QTY")
    @Excel(name = "ui.data.column.lhScheduleResult.class3FinishQty", cellType = Excel.ColumnType.NUMERIC, sort = 18)
    private Integer class3FinishQty;

    /**
     * 次日一班计划量
     */
    @ApiModelProperty(value = "次日一班计划量", name = "class4PlanQty")
    @TableField(value = "CLASS4_PLAN_QTY")
    @Excel(name = "ui.data.column.lhScheduleResult.class4PlanQty", cellType = Excel.ColumnType.NUMERIC, sort = 21)
    private Integer class4PlanQty;

    /**
     * 次日一班计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "次日一班计划开始时间", name = "class4StartTime")
    @TableField(value = "CLASS4_START_TIME")
    private Date class4StartTime;

    /**
     * 次日一班计划结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "次日一班计划结束时间", name = "class4EndTime")
    @TableField(value = "CLASS4_END_TIME")
    private Date class4EndTime;

    /**
     * 次日一班原因分析
     */
    @ApiModelProperty(value = "次日一班原因分析", name = "class4Analysis")
    @TableField(value = "CLASS4_ANALYSIS")
    @Excel(name = "ui.data.column.lhScheduleResult.class4Analysis", sort = 23)
    private String class4Analysis;

    /**
     * 次日一班完成量
     */
    @ApiModelProperty(value = "次日一班完成量", name = "class4FinishQty")
    @TableField(value = "CLASS4_FINISH_QTY")
    @Excel(name = "ui.data.column.lhScheduleResult.class4FinishQty", cellType = Excel.ColumnType.NUMERIC, sort = 22)
    private Integer class4FinishQty;

    /**
     * 次日二班计划量
     */
    @ApiModelProperty(value = "次日二班计划量", name = "class5PlanQty")
    @TableField(value = "CLASS5_PLAN_QTY")
    @Excel(name = "ui.data.column.lhScheduleResult.class5PlanQty", cellType = Excel.ColumnType.NUMERIC, sort = 24)
    private Integer class5PlanQty;

    /**
     * 次日二班计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "次日二班计划开始时间", name = "class5StartTime")
    @TableField(value = "CLASS5_START_TIME")
    private Date class5StartTime;

    /**
     * 次日二班计划结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "次日二班计划结束时间", name = "class5EndTime")
    @TableField(value = "CLASS5_END_TIME")
    private Date class5EndTime;

    /**
     * 次日二班原因分析
     */
    @ApiModelProperty(value = "次日二班原因分析", name = "class5Analysis")
    @TableField(value = "CLASS5_ANALYSIS")
    @Excel(name = "ui.data.column.lhScheduleResult.class5Analysis", sort = 26)
    private String class5Analysis;

    /**
     * 次日二班完成量
     */
    @ApiModelProperty(value = "次日二班完成量", name = "class5FinishQty")
    @TableField(value = "CLASS5_FINISH_QTY")
    @Excel(name = "ui.data.column.lhScheduleResult.class5FinishQty", cellType = Excel.ColumnType.NUMERIC, sort = 25)
    private Integer class5FinishQty;

    /**
     * 次日三班计划量
     */
    @ApiModelProperty(value = "次日三班计划量", name = "class6PlanQty")
    @TableField(value = "CLASS6_PLAN_QTY")
    @Excel(name = "ui.data.column.lhScheduleResult.class6PlanQty", cellType = Excel.ColumnType.NUMERIC, sort = 27)
    private Integer class6PlanQty;

    /**
     * 次日三班计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "次日三班计划开始时间", name = "class6StartTime")
    @TableField(value = "CLASS6_START_TIME")
    private Date class6StartTime;

    /**
     * 次日三班计划结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "次日三班计划结束时间", name = "class6EndTime")
    @TableField(value = "CLASS6_END_TIME")
    private Date class6EndTime;

    /**
     * 次日三班原因分析
     */
    @ApiModelProperty(value = "次日三班原因分析", name = "class6Analysis")
    @TableField(value = "CLASS6_ANALYSIS")
    @Excel(name = "ui.data.column.lhScheduleResult.class6Analysis", sort = 29)
    private String class6Analysis;

    /**
     * 次日三班完成量
     */
    @ApiModelProperty(value = "次日三班完成量", name = "class6FinishQty")
    @TableField(value = "CLASS6_FINISH_QTY")
    @Excel(name = "ui.data.column.lhScheduleResult.class6FinishQty", cellType = Excel.ColumnType.NUMERIC, sort = 28)
    private Integer class6FinishQty;

    /**
     * 是否交期，0--否，1--是
     */
    @ApiModelProperty(value = "是否交期")
    @TableField(value = "IS_DELIVERY")
    @Excel(name = "ui.data.column.lhScheduleResult.isDelivery", dictType = "IS_HAVE", sort = 31)
    private String isDelivery;

    /**
     * 交期需要数量
     */
    @ApiModelProperty(value = "交期需要数量")
    @TableField(exist = false)
    private Integer deliveryNum;

    /**
     * 是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE
     */
    @ApiModelProperty(value = "是否发布", name = "isRelease")
    @TableField(value = "IS_RELEASE")
    private String isRelease;

    /**
     * 发布成功计数器，每次发布成功进行累加。如果大于1发，发布状态只能到待发布
     */
    @ApiModelProperty(value = "发布成功计数器", name = "publishSuccessCount")
    @TableField(value = "PUBLISH_SUCCESS_COUNT")
    private Integer publishSuccessCount;

    /**
     * 保留最新的一次发布成功时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "发布成功时间", name = "newestPublishTime")
    @TableField(value = "NEWEST_PUBLISH_TIME")
    private Date newestPublishTime;

    /**
     * 数据来源：0&gt;自动排程；1&gt;插单；2：导入。插单数据可以进行计划调整
     */
    @ApiModelProperty(value = "数据来源", name = "dataSource")
    @TableField(value = "DATA_SOURCE")
    private String dataSource;

    @ApiModelProperty(value = "单班产能", name = "singleMoldShiftLhQty")
    @TableField(value = "SINGLE_MOLD_SHIFT_QTY")
    private Integer singleMoldShiftLhQty;

    /**
     * 模具信息
     */
    @ApiModelProperty(value = "模具信息", name = "moldInfo")
    @TableField(value = "MOLD_INFO")
    private String moldInfo;

    /**
     * BOM版本
     */
    @ApiModelProperty(value = "BOM版本", name = "bomVersion")
    @TableField(value = "BOM_VERSION")
    private String bomVersion;

    /**
     * 成型法
     */
    @ApiModelProperty(value = "成型法", name = "mouldMethod")
    @TableField(value = "MOULD_METHOD")
    private String mouldMethod;

    /**
     * 成型排程使用的虚字段,存合并任务后IDS
     */
    @ApiModelProperty(value = "合并任务的IDS")
    @TableField(exist = false)
    private String mergeIds;

    /**
     * 是否变更机台
     */
    @TableField(exist = false)
    private String changeMachine;

    /**
     * 是否更改一班计划
     */
    @TableField(exist = false)
    private String changeClass1Plan;

    /**
     * 是否更改二班计划
     */
    @TableField(exist = false)
    private String changeClass2Plan;

    /**
     * 是否更改三班计划
     */
    @TableField(exist = false)
    private String changeClass3Plan;

    /**
     * 是否更改四班计划
     */
    @TableField(exist = false)
    private String changeClass4Plan;

    /**
     * 是否更改五班计划
     */
    @TableField(exist = false)
    private String changeClass5Plan;

    /**
     * 是否更改六班计划
     */
    @TableField(exist = false)
    private String changeClass6Plan;

    @ApiModelProperty("排程记录id数组")
    @TableField(exist = false)
    private Long[] ids;

    @ApiModelProperty("机台顺序")
    @TableField(value = "MACHINE_ORDER")
    private Integer machineOrder;

    @ApiModelProperty(value = "是否试产试制")
    @TableField(value = "IS_TRIAL")
    private String isTrial;

    @ApiModelProperty(value = "是否首排")
    @TableField(value = "IS_FIRST")
    private String isFirst;
    /**
     * 导出合并标志
     */
    @TableField(exist = false)
    private String exportCombineFlag;

    /**
     * 总完成率
     */
    @TableField(exist = false)
    private Double finishRate;
}