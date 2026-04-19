package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.vo.ScheduleContextVo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 核心排程算法服务接口
 *
 * <p>成型排程主流程编排，具体业务逻辑委托给各专门服务：
 * <ul>
 *   <li>{@link TaskGroupService} - 任务分组与属性计算</li>
 *   <li>{@link ContinueTaskProcessor} - 续作任务处理</li>
 *   <li>{@link TrialTaskProcessor} - 试制任务处理</li>
 *   <li>{@link NewTaskProcessor} - 新增任务处理（含量试约束）</li>
 *   <li>{@link ShiftScheduleService} - 班次精排</li>
 * </ul>
 *
 * @author APS Team
 */
public interface CoreScheduleAlgorithmService {

    /**
     * 执行完整排程算法
     *
     * <p>主流程：
     * <ol>
     *   <li>按天循环排程（共排8个班次，约3天）</li>
     *   <li>每天：任务分组 → 续作处理 → 试制处理 → 新增处理 → 班次精排</li>
     *   <li>每天排完后更新上下文（库存/余量/在机信息）</li>
     *   <li>汇总多天结果，按 机台+胎胚+物料编号 维度生成单表排程数据</li>
     * </ol>
     *
     * @param context 排程上下文
     * @return 排程结果列表（每条记录：机台+胎胚+物料编号，CLASS1~8为8班次排量）
     */
    List<CxScheduleResult> executeSchedule(ScheduleContextVo context);

    // ==================== 数据结构定义 ====================

    /**
     * 日胎胚任务
     *
     * <p>排程调度的最小单元，一个胎胚在一个排程日的一条任务。
     * 由 {@link TaskGroupService#groupTasks} 生成，包含完整的任务属性。
     */
    @lombok.Data
    class DailyEmbryoTask {
        /** 胎胚编码 */
        private String embryoCode;
        /** 物料编码（成品物料编码，用于判断主销产品） */
        private String materialCode;
        /** 物料描述 */
        private String materialDesc;
        /** 主物料描述（胎胚描述） */
        private String mainMaterialDesc;
        /** 结构名称 */
        private String structureName;
        /** 日需求量 */
        private Integer demandQuantity;
        /** 已分配量 */
        private Integer assignedQuantity;
        /** 剩余待分配量 */
        private Integer remainingQuantity;
        /** 优先级分数 */
        private Integer priority;
        /** 是否主销产品 */
        private Boolean isMainProduct;
        /** 是否试制任务 */
        private Boolean isTrialTask;
        /** 试制号 */
        private String trialNo;
        /** 库存可供时长（小时） */
        private BigDecimal stockHours;
        /** 库存是否高预警（>18小时） */
        private Boolean isStockHighWarning;
        /** 硫化机台数 */
        private Integer vulcanizeMachineCount;
        /** 硫化模数 */
        private Integer vulcanizeMoldCount;
        /** 是否首排 */
        private Boolean isFirstTask;
        /** 是否续作任务 */
        private Boolean isContinueTask;
        /** 是否量试任务（施工阶段为02-量试） */
        private Boolean isProductionTrial;
        /** 量试约束机台编码（量试任务只能分配到该机台，来自试制分配结果） */
        private String constrainedMachineCode;
        /** 续作机台列表 */
        private List<String> continueMachineCodes;
        /** 硫化需求量（来自硫化排程） */
        private Integer vulcanizeDemand;
        /** 当前库存 */
        private Integer currentStock;

        // ==================== 收尾相关字段 ====================
        /** 是否收尾任务（收尾余量<=0时为true） */
        private Boolean isEndingTask;
        /** 收尾余量 = 硫化余量(PLAN_SURPLUS_QTY) - 胎胚库存 */
        private Integer endingSurplusQty;
        /** 硫化余量（来自t_mdm_month_surplus.PLAN_SURPLUS_QTY） */
        private Integer vulcanizeSurplusQty;
        /** 收尾日（月计划的最后排产日期） */
        private LocalDate endingDate;
        /** 距离收尾日天数 */
        private Integer daysToEnding;
        /** 是否紧急收尾（3天内收尾） */
        private Boolean isUrgentEnding;
        /** 是否10天内收尾 */
        private Boolean isNearEnding;
        /** 是否需要月计划调整（满产追不上时为true） */
        private Boolean needMonthPlanAdjust;
        /** 追赶量（平摊到未来3天的延误量） */
        private Integer catchUpQuantity;

        // ==================== S5.2 排程分类与余量计算新增字段 ====================
        /** 硫化任务ID（用于关联 materialStockMap） */
        private Long lhId;
        /** 待排产量 = (日硫化量 - 库存) × (1 + 损耗率) */
        private Integer plannedProduction;
        /** 需要的车数 = 待排产量 / 胎面整车条数 */
        private Integer requiredCars;
        /** 月计划排产版本（来自硫化排程结果） */
        private String productionVersion;
        

        // ==================== S5.3 开停产处理新增字段 ====================
        /** 开产班次产能（首班只排6小时） */
        private Integer openingShiftCapacity;
        /** 是否开产日任务 */
        private Boolean isOpeningDayTask;
        /** 是否停产日任务 */
        private Boolean isClosingDayTask;
        /** 是否关键产品开产（首班不排） */
        private Boolean isKeyProductOnOpening;
        /** 是否收尾最后一批 */
        private Boolean isLastEndingBatch;
        /** 班次分配结果（班次编码 -> 计划量） */
        private Map<String, Integer> shiftAllocation;

        // ==================== 停产反推封顶新增字段 ====================
        /** 停锅班次序号（dayShiftOrder，根据硫化机停锅时间和班次时间计算） */
        private Integer closingShiftOrder;
        /** 停产反推总量（从成型停机到硫化停锅期间需要消耗的胎胚总量） */
        private Integer closingRequiredStock;

        // ==================== 开产提前一班新增字段 ====================
        /** 硫化开产班次序号（dayShiftOrder，根据硫化开模时间和班次时间计算） */
        private Integer lhOpeningShiftOrder;
        /** 成型开产班次序号（= 硫化开产班次 - 1，提前一个班次） */
        private Integer formingOpeningShiftOrder;

        // ==================== 收尾处理新增字段 ====================
        /** 收尾是否被舍弃（非主销产品余量<=2条） */
        private Boolean endingAbandoned;
        /** 舍弃数量 */
        private Integer endingAbandonedQty;
        /** 最终需要生产的量 */
        private Integer endingExtraInventory;
        /** 机台小时产能（条/小时） */
        private Integer hourCapacity;

        // ==================== 新增任务排序相关字段 ====================
        /** 月计划优先级 */
        private Integer monthPlanPriority;
        /** 是否新胎胚（无历史生产记录） */
        private Boolean isNewEmbryo;
        /** 推荐机台列表（从月计划获取） */
        private List<String> recommendedMachines;
    }

    /**
     * 机台分配结果
     */
    @lombok.Data
    class MachineAllocationResult {
        private String machineCode;
        private String machineName;
        private String machineType;
        private Integer dailyCapacity;
        private Integer usedCapacity;
        private Integer remainingCapacity;
        private Integer assignedTypes;
        private List<TaskAllocation> taskAllocations;
        private String currentStructure;
    }

    /**
     * 任务分配
     */
    @lombok.Data
    class TaskAllocation {
        /** 胎胚编码（成型生产的胎胚） */
        private String embryoCode;
        /** 物料编号（成品物料编码，用于关联硫化需求） */
        private String materialCode;
        /** 物料描述 */
        private String materialDesc;
        /** 主物料描述（胎胚描述） */
        private String mainMaterialDesc;
        /** 结构名称 */
        private String structureName;
        /** 计划数量（天维度总量） */
        private Integer quantity;
        /** 硫化机台数 */
        private Integer vulcanizeMachineCount;
        /** 优先级 */
        private Integer priority;
        /** 库存可供时长（小时） */
        private BigDecimal stockHours;
        /** 是否试制任务 */
        private Boolean isTrialTask;
        /** 是否收尾任务 */
        private Boolean isEndingTask;
        /** 收尾余量 */
        private Integer endingSurplusQty;
        /** 是否主销产品 */
        private Boolean isMainProduct;
        /** 是否续作任务 */
        private Boolean isContinueTask;
        /** 硫化任务ID（关联LhScheduleResult） */
        private Long lhId;
        /** 是否停产日任务 */
        private Boolean isClosingDayTask;
        /** 是否开产日任务 */
        private Boolean isOpeningDayTask;
        /** 停产班次序号 */
        private Integer closingShiftOrder;
        /** 成型开产首班序号 */
        private Integer formingOpeningShiftOrder;
        /** 硫化开产班次序号 */
        private Integer lhOpeningShiftOrder;
        /** 各班次预分配数量（班次序号 → 预分配量） */
        private Map<Integer, Integer> shiftPreAllocatedQty;
    }

    /**
     * 班次分配结果
     */
    @lombok.Data
    class ShiftAllocationResult {
        private String machineCode;
        private String machineName;
        private Map<String, Integer> shiftPlanQty;
        private List<TaskAllocation> tasks;
    }
}
