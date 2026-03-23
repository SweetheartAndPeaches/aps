package com.zlt.aps.cx.service;

import com.zlt.aps.cx.entity.CxMaterial;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxTrialPlan;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 试制排程服务接口
 * 
 * 实现试制任务的排程逻辑：
 * - 试制任务创建与管理
 * - 试制排程约束检查
 * - 试制结果评价
 * - 试制转量产处理
 *
 * @author APS Team
 */
public interface TrialScheduleService {

    /**
     * 创建试制计划
     *
     * @param trialPlan 试制计划信息
     * @return 创建结果
     */
    TrialPlanResult createTrialPlan(CxTrialPlan trialPlan);

    /**
     * 执行试制排程
     * 试制任务优先于常规任务，但受以下约束：
     * - 周日不排试制
     * - 一天最多2个新胎胚
     * - 只能在早班或中班排
     * - 数量必须是双数
     *
     * @param scheduleDate 排程日期
     * @param trialPlans   待排程的试制计划列表
     * @return 排程结果
     */
    TrialScheduleResult executeTrialSchedule(LocalDate scheduleDate, List<CxTrialPlan> trialPlans);

    /**
     * 检查试制排程约束
     *
     * @param scheduleDate 排程日期
     * @param trialPlan    试制计划
     * @return 约束检查结果
     */
    TrialConstraintResult checkTrialConstraints(LocalDate scheduleDate, CxTrialPlan trialPlan);

    /**
     * 分配试制任务到机台
     * 优先选择空闲机台或有试制经验的机台
     *
     * @param trialPlan 试制计划
     * @param availableMachines 可用机台列表
     * @return 最佳机台编码
     */
    String allocateTrialMachine(CxTrialPlan trialPlan, List<String> availableMachines);

    /**
     * 确定试制排程的班次
     * 只能在早班或中班排
     *
     * @param scheduleDate 排程日期
     * @param machineCode  机台编码
     * @return 可用班次列表
     */
    List<String> determineTrialShifts(LocalDate scheduleDate, String machineCode);

    /**
     * 计算试制数量
     * 根据试制目的和物料特性确定
     *
     * @param trialPlan 试制计划
     * @return 计算出的试制数量（双数）
     */
    int calculateTrialQuantity(CxTrialPlan trialPlan);

    /**
     * 评价试制结果
     * 根据完成率、质量情况评价试制是否成功
     *
     * @param scheduleDetailId 排程明细ID
     * @param actualQuantity   实际完成数量
     * @param qualityRate      质量合格率
     * @return 评价结果
     */
    TrialEvaluationResult evaluateTrialResult(Long scheduleDetailId, int actualQuantity, BigDecimal qualityRate);

    /**
     * 试制转量产
     * 试制成功后，将物料转为正常量产状态
     *
     * @param materialCode 物料编码
     * @return 转换结果
     */
    boolean convertTrialToMassProduction(String materialCode);

    /**
     * 获取当天已安排的试制任务数量
     *
     * @param scheduleDate 排程日期
     * @return 已安排的试制任务数量
     */
    int getTodayTrialTaskCount(LocalDate scheduleDate);

    /**
     * 获取待排程的试制计划列表
     *
     * @return 待排程的试制计划列表
     */
    List<CxTrialPlan> getPendingTrialPlans();

    /**
     * 更新试制计划状态
     *
     * @param trialPlanId 试制计划ID
     * @param status      新状态
     * @return 是否更新成功
     */
    boolean updateTrialPlanStatus(Long trialPlanId, String status);

    /**
     * 试制计划创建结果
     */
    @lombok.Data
    class TrialPlanResult {
        private boolean success;
        private Long trialPlanId;
        private String message;
    }

    /**
     * 试制排程结果
     */
    @lombok.Data
    class TrialScheduleResult {
        private boolean success;
        private List<CxScheduleDetail> scheduledDetails;
        private List<String> skippedPlans;
        private String message;
    }

    /**
     * 试制约束检查结果
     */
    @lombok.Data
    class TrialConstraintResult {
        private boolean passed;
        private List<String> violations;
        private String message;
    }

    /**
     * 试制评价结果
     */
    @lombok.Data
    class TrialEvaluationResult {
        private boolean success;
        private String evaluation;
        private BigDecimal score;
        private String suggestion;
    }
}
