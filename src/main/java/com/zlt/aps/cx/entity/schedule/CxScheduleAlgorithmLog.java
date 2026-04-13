package com.zlt.aps.cx.entity.schedule;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 成型算法日志表（结构维度）
 * 
 * <p>记录排产过程中的详细逻辑，供测试人员验证算法准确性。
 * 每个结构维度一条记录，每次重排时更新最新数据。
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_CX_SCHEDULE_ALGORITHM_LOG")
@ApiModel(value = "成型算法日志对象", description = "成型算法日志表（结构维度）")
public class CxScheduleAlgorithmLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    // ==================== 基础信息 ====================
    
    @ApiModelProperty(value = "排程批次号")
    @TableField("SCHEDULE_BATCH_NO")
    private String scheduleBatchNo;

    @ApiModelProperty(value = "排程日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("SCHEDULE_DATE")
    private LocalDate scheduleDate;

    @ApiModelProperty(value = "工厂编码")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "排程类型：NORMAL-正常排程，RE_SCHEDULE-重排程")
    @TableField("SCHEDULE_TYPE")
    private String scheduleType;

    @ApiModelProperty(value = "第几天排程（1=第一天，2=第二天，3=第三天）")
    @TableField("SCHEDULE_DAY_INDEX")
    private Integer scheduleDayIndex;

    // ==================== 结构维度信息 ====================

    @ApiModelProperty(value = "结构名称")
    @TableField("STRUCTURE_NAME")
    private String structureName;

    @ApiModelProperty(value = "结构编码")
    @TableField("STRUCTURE_CODE")
    private String structureCode;

    @ApiModelProperty(value = "机型编码（该结构对应的机型）")
    @TableField("MACHINE_TYPE_CODE")
    private String machineTypeCode;

    // ==================== 任务分组信息 ====================

    @ApiModelProperty(value = "任务分组JSON-续作任务列表")
    @TableField("CONTINUE_TASK_JSON")
    private String continueTaskJson;

    @ApiModelProperty(value = "任务分组JSON-试制任务列表")
    @TableField("TRIAL_TASK_JSON")
    private String trialTaskJson;

    @ApiModelProperty(value = "任务分组JSON-新任务列表")
    @TableField("NEW_TASK_JSON")
    private String newTaskJson;

    @ApiModelProperty(value = "续作任务数量")
    @TableField("CONTINUE_TASK_COUNT")
    private Integer continueTaskCount;

    @ApiModelProperty(value = "试制任务数量")
    @TableField("TRIAL_TASK_COUNT")
    private Integer trialTaskCount;

    @ApiModelProperty(value = "新任务数量")
    @TableField("NEW_TASK_COUNT")
    private Integer newTaskCount;

    @ApiModelProperty(value = "任务分组说明")
    @TableField("TASK_GROUP_DESC")
    private String taskGroupDesc;

    // ==================== 机台分配信息 ====================

    @ApiModelProperty(value = "分配机台JSON-详细分配信息")
    @TableField("MACHINE_ALLOCATION_JSON")
    private String machineAllocationJson;

    @ApiModelProperty(value = "分配机台数量")
    @TableField("ALLOCATED_MACHINE_COUNT")
    private Integer allocatedMachineCount;

    @ApiModelProperty(value = "分配机台列表（逗号分隔）")
    @TableField("ALLOCATED_MACHINES")
    private String allocatedMachines;

    // ==================== 库存信息 ====================

    @ApiModelProperty(value = "初始库存JSON-各胎胚库存")
    @TableField("INITIAL_STOCK_JSON")
    private String initialStockJson;

    @ApiModelProperty(value = "初始库存总量")
    @TableField("INITIAL_STOCK_TOTAL")
    private BigDecimal initialStockTotal;

    @ApiModelProperty(value = "硫化消耗库存总量")
    @TableField("CONSUMED_STOCK_TOTAL")
    private BigDecimal consumedStockTotal;

    @ApiModelProperty(value = "剩余库存总量")
    @TableField("REMAINING_STOCK_TOTAL")
    private BigDecimal remainingStockTotal;

    @ApiModelProperty(value = "库存计算说明")
    @TableField("STOCK_CALC_DESC")
    private String stockCalcDesc;

    // ==================== 班次分配信息 ====================

    @ApiModelProperty(value = "班次分配JSON-各班次分配详情")
    @TableField("SHIFT_ALLOCATION_JSON")
    private String shiftAllocationJson;

    @ApiModelProperty(value = "一班计划数")
    @TableField("CLASS1_PLAN_QTY")
    private BigDecimal class1PlanQty;

    @ApiModelProperty(value = "二班计划数")
    @TableField("CLASS2_PLAN_QTY")
    private BigDecimal class2PlanQty;

    @ApiModelProperty(value = "三班计划数")
    @TableField("CLASS3_PLAN_QTY")
    private BigDecimal class3PlanQty;

    @ApiModelProperty(value = "四班计划数")
    @TableField("CLASS4_PLAN_QTY")
    private BigDecimal class4PlanQty;

    @ApiModelProperty(value = "夜班计划数（汇总）")
    @TableField("NIGHT_PLAN_QTY")
    private BigDecimal nightPlanQty;

    @ApiModelProperty(value = "早班计划数（汇总）")
    @TableField("MORNING_PLAN_QTY")
    private BigDecimal morningPlanQty;

    @ApiModelProperty(value = "中班计划数（汇总）")
    @TableField("NOON_PLAN_QTY")
    private BigDecimal noonPlanQty;

    @ApiModelProperty(value = "班次均衡比例（夜:早:中）")
    @TableField("SHIFT_BALANCE_RATIO")
    private String shiftBalanceRatio;

    @ApiModelProperty(value = "班次均衡说明")
    @TableField("SHIFT_BALANCE_DESC")
    private String shiftBalanceDesc;

    // ==================== 产能计算信息 ====================

    @ApiModelProperty(value = "结构整车配置JSON")
    @TableField("TREAD_CONFIG_JSON")
    private String treadConfigJson;

    @ApiModelProperty(value = "整车胎面条数（TREAD_COUNT）")
    @TableField("TREAD_COUNT")
    private Integer treadCount;

    @ApiModelProperty(value = "单班产能（整车条数）")
    @TableField("SHIFT_CAPACITY")
    private BigDecimal shiftCapacity;

    @ApiModelProperty(value = "日产能（整车条数）")
    @TableField("DAY_CAPACITY")
    private BigDecimal dayCapacity;

    @ApiModelProperty(value = "产能计算说明")
    @TableField("CAPACITY_CALC_DESC")
    private String capacityCalcDesc;

    // ==================== 关键产品信息 ====================

    @ApiModelProperty(value = "是否关键产品（0-否，1-是）")
    @TableField("IS_KEY_PRODUCT")
    private String isKeyProduct;

    @ApiModelProperty(value = "关键产品优先级")
    @TableField("KEY_PRIORITY")
    private Integer keyPriority;

    @ApiModelProperty(value = "关键产品处理说明")
    @TableField("KEY_PRODUCT_DESC")
    private String keyProductDesc;

    // ==================== 约束检查信息 ====================

    @ApiModelProperty(value = "约束检查结果JSON")
    @TableField("CONSTRAINT_CHECK_JSON")
    private String constraintCheckJson;

    @ApiModelProperty(value = "约束冲突数量")
    @TableField("CONSTRAINT_VIOLATION_COUNT")
    private Integer constraintViolationCount;

    @ApiModelProperty(value = "约束检查说明")
    @TableField("CONSTRAINT_CHECK_DESC")
    private String constraintCheckDesc;

    // ==================== 时间计算信息 ====================

    @ApiModelProperty(value = "首班开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("FIRST_SHIFT_START_TIME")
    private Date firstShiftStartTime;

    @ApiModelProperty(value = "末班结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("LAST_SHIFT_END_TIME")
    private Date lastShiftEndTime;

    @ApiModelProperty(value = "总排产时长（分钟）")
    @TableField("TOTAL_DURATION_MINUTES")
    private Integer totalDurationMinutes;

    @ApiModelProperty(value = "时间计算说明")
    @TableField("TIME_CALC_DESC")
    private String timeCalcDesc;

    // ==================== 收尾信息 ====================

    @ApiModelProperty(value = "是否收尾（0-否，1-是）")
    @TableField("IS_CLOSE_OUT")
    private String isCloseOut;

    @ApiModelProperty(value = "收尾标识说明")
    @TableField("CLOSE_OUT_DESC")
    private String closeOutDesc;

    // ==================== 结果汇总 ====================

    @ApiModelProperty(value = "该结构总计划数")
    @TableField("TOTAL_PLAN_QTY")
    private BigDecimal totalPlanQty;

    @ApiModelProperty(value = "生成排程结果数量")
    @TableField("SCHEDULE_RESULT_COUNT")
    private Integer scheduleResultCount;

    @ApiModelProperty(value = "结果汇总说明")
    @TableField("RESULT_SUMMARY_DESC")
    private String resultSummaryDesc;

    // ==================== 详细日志（JSON格式存储完整计算过程） ====================

    @ApiModelProperty(value = "完整计算过程JSON（包含所有中间变量和计算步骤）")
    @TableField("CALCULATION_PROCESS_JSON")
    private String calculationProcessJson;

    @ApiModelProperty(value = "算法执行步骤JSON")
    @TableField("ALGORITHM_STEPS_JSON")
    private String algorithmStepsJson;

    @ApiModelProperty(value = "异常信息（如有）")
    @TableField("ERROR_MESSAGE")
    private String errorMessage;

    @ApiModelProperty(value = "执行耗时（毫秒）")
    @TableField("EXECUTION_TIME_MS")
    private Long executionTimeMs;

    @ApiModelProperty(value = "日志版本号（用于版本追踪）")
    @TableField("LOG_VERSION")
    private String logVersion;
}
