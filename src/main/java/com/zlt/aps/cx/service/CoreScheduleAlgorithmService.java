package com.zlt.aps.cx.service;

import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.entity.CxMaterial;
import com.zlt.aps.cx.entity.CxStock;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.mdm.MdmMoldingMachine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 核心排程算法服务接口
 * 
 * 基于需求文档实现以下核心算法：
 * - 试错分配算法（递归回溯）
 * - 班次均衡调整（波浪交替）
 * - 约束校验算法
 * - 顺位排序算法
 * - 库存时长计算
 *
 * @author APS Team
 */
public interface CoreScheduleAlgorithmService {

    // ==================== 主流程算法 ====================

    /**
     * 执行完整排程算法
     *
     * @param context 排程上下文
     * @return 排程结果列表
     */
    List<CxScheduleResult> executeSchedule(ScheduleContextDTO context);

    /**
     * 第一步：计算日胎胚任务
     * 算需求量、检查收尾、处理节假日
     *
     * @param context 排程上下文
     * @return 日胎胚任务列表
     */
    List<DailyEmbryoTask> calculateDailyEmbryoTasks(ScheduleContextDTO context);

    /**
     * 第二步：试错分配任务到机台
     * 递归回溯算法，找最优分配方案
     *
     * @param tasks   日胎胚任务列表
     * @param context 排程上下文
     * @return 机台分配结果
     */
    List<MachineAllocationResult> allocateTasksToMachines(
            List<DailyEmbryoTask> tasks, 
            ScheduleContextDTO context);

    /**
     * 第三步：班次均衡分配
     * 波浪交替算法：夜班:早班:中班 = 1:2:1
     *
     * @param allocations 机台分配结果
     * @param context     排程上下文
     * @return 班次分配结果
     */
    List<ShiftAllocationResult> balanceShiftAllocation(
            List<MachineAllocationResult> allocations,
            ScheduleContextDTO context);

    /**
     * 第四步：排生产顺位
     * 按紧急程度和库存时长排序
     *
     * @param shiftAllocations 班次分配结果
     * @param context          排程上下文
     * @return 排程明细列表
     */
    List<CxScheduleDetail> calculateSequence(
            List<ShiftAllocationResult> shiftAllocations,
            ScheduleContextDTO context);

    // ==================== 辅助算法 ====================

    /**
     * 计算库存可供硫化时长
     * 公式：库存时长 = 胎胚库存 / (硫化机数 × 单台模数)
     *
     * @param stock              库存信息
     * @param vulcanizeMachineCount 硫化机台数
     * @param vulcanizeMoldCount    总模数
     * @return 可供硫化时长（小时）
     */
    BigDecimal calculateStockHours(
            CxStock stock,
            Integer vulcanizeMachineCount,
            Integer vulcanizeMoldCount);

    /**
     * 计算日需求量
     * 公式：日胎胚计划量 = (硫化消耗量 - 库存分配量) × (1 + 损耗率)
     *
     * @param material       物料信息
     * @param stock          库存信息
     * @param context        排程上下文
     * @return 日需求量
     */
    BigDecimal calculateDailyDemand(
            CxMaterial material,
            CxStock stock,
            ScheduleContextDTO context);

    /**
     * 检查结构约束
     * 包括：固定机台约束、不可作业约束、种类上限约束
     *
     * @param machine   机台信息
     * @param material  物料信息
     * @param context   排程上下文
     * @return 是否通过约束检查
     */
    boolean checkStructureConstraint(
            MdmMoldingMachine machine,
            CxMaterial material,
            ScheduleContextDTO context);

    /**
     * 检查机台种类上限
     * 每台成型机最多做4种不同的胎胚
     *
     * @param machine       机台信息
     * @param currentTypes  当前已分配的种类数
     * @param newMaterial   新物料
     * @param context       排程上下文
     * @return 是否可以分配
     */
    boolean checkTypeLimit(
            MdmMoldingMachine machine,
            int currentTypes,
            CxMaterial newMaterial,
            ScheduleContextDTO context);

    /**
     * 计算优先级分数
     * 综合考虑：紧急收尾、库存预警、主销产品、结构优先级
     *
     * @param material 物料信息
     * @param stock    库存信息
     * @param context  排程上下文
     * @return 优先级分数（越大越优先）
     */
    int calculatePriorityScore(
            CxMaterial material,
            CxStock stock,
            ScheduleContextDTO context);

    /**
     * 整车取整
     * 按12条一车取整
     *
     * @param quantity 原始数量
     * @param mode     取整模式（CEILING向上/FLOOR向下/ROUND四舍五入）
     * @return 取整后的数量
     */
    int roundToTrip(int quantity, String mode);

    // ==================== 数据结构定义 ====================

    /**
     * 日胎胚任务
     */
    @lombok.Data
    class DailyEmbryoTask {
        private String materialCode;
        private String materialName;
        private String structureCode;
        private String structureName;
        private Integer demandQuantity;
        private Integer assignedQuantity;
        private Integer remainingQuantity;
        private Integer priority;
        private Boolean isUrgentEnding;
        private Boolean isMainProduct;
        private Boolean isTrialTask;
        private String trialNo;
        private BigDecimal stockHours;
        private Integer vulcanizeMachineCount;
        private Integer vulcanizeMoldCount;
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
        private String materialCode;
        private String materialName;
        private String structureName;
        private Integer quantity;
        private Integer priority;
        private BigDecimal stockHours;
        private Boolean isUrgentEnding;
        private Boolean isTrialTask;
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
