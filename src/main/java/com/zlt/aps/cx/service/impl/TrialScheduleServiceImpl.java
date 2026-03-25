package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zlt.aps.cx.entity.mdm.MdmMaterialInfo;
import com.zlt.aps.cx.entity.mdm.MdmMoldingMachine;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxTrialPlan;
import com.zlt.aps.cx.mapper.*;
import com.zlt.aps.cx.service.ConstraintCheckService;
import com.zlt.aps.cx.service.TrialScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 试制排程服务实现类
 *
 * @author APS Team
 */
@Slf4j
@Service
public class TrialScheduleServiceImpl implements TrialScheduleService {

    @Autowired
    private CxTrialPlanMapper trialPlanMapper;

    @Autowired
    private CxScheduleDetailMapper scheduleDetailMapper;

    @Autowired
    private MdmMaterialInfoMapper materialInfoMapper;

    @Autowired
    private MdmMoldingMachineMapper moldingMachineMapper;

    @Autowired
    private ConstraintCheckService constraintCheckService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrialPlanResult createTrialPlan(CxTrialPlan trialPlan) {
        TrialPlanResult result = new TrialPlanResult();

        try {
            // 验证物料是否存在
            MdmMaterialInfo material = materialInfoMapper.selectOne(
                    new LambdaQueryWrapper<MdmMaterialInfo>()
                            .eq(MdmMaterialInfo::getMaterialCode, trialPlan.getMaterialCode()));

            if (material == null) {
                result.setSuccess(false);
                result.setMessage("物料不存在：" + trialPlan.getMaterialCode());
                return result;
            }

            // 设置初始状态
            trialPlan.setStatus("PENDING");
            trialPlan.setCreateTime(LocalDateTime.now());

            // 保存试制计划
            trialPlanMapper.insert(trialPlan);

            result.setSuccess(true);
            result.setTrialPlanId(trialPlan.getId());
            result.setMessage("试制计划创建成功");

            log.info("创建试制计划成功，物料：{}，计划日期：{}", 
                    trialPlan.getMaterialCode(), trialPlan.getPlanDate());

        } catch (Exception e) {
            log.error("创建试制计划失败", e);
            result.setSuccess(false);
            result.setMessage("创建失败：" + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrialScheduleResult executeTrialSchedule(LocalDate scheduleDate, List<CxTrialPlan> trialPlans) {
        TrialScheduleResult result = new TrialScheduleResult();
        result.setScheduledDetails(new ArrayList<>());
        result.setSkippedPlans(new ArrayList<>());

        if (CollectionUtils.isEmpty(trialPlans)) {
            result.setSuccess(true);
            result.setMessage("无待排程的试制计划");
            return result;
        }

        // 检查是否为周日
        if (scheduleDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            result.setSuccess(false);
            result.setMessage("周日不安排试制");
            return result;
        }

        // 获取当天已安排的试制任务数量
        int todayTrialCount = getTodayTrialTaskCount(scheduleDate);

        // 获取可用机台
        List<MdmMoldingMachine> availableMachines = moldingMachineMapper.selectList(
                new LambdaQueryWrapper<MdmMoldingMachine>()
                        .eq(MdmMoldingMachine::getIsActive, 1)
                        .ne(MdmMoldingMachine::getMaintainStatus, "FAULT"));

        List<String> machineCodes = availableMachines.stream()
                .map(MdmMoldingMachine::getCxMachineCode)
                .collect(Collectors.toList());

        int scheduledCount = 0;

        for (CxTrialPlan trialPlan : trialPlans) {
            // 检查当天是否已达到上限
            if (todayTrialCount + scheduledCount >= 2) {
                result.getSkippedPlans().add(trialPlan.getMaterialCode() + "：当天已安排2个试制任务");
                continue;
            }

            // 检查约束
            TrialConstraintResult constraintResult = checkTrialConstraints(scheduleDate, trialPlan);
            if (!constraintResult.isPassed()) {
                result.getSkippedPlans().add(trialPlan.getMaterialCode() + "：" + constraintResult.getMessage());
                continue;
            }

            // 分配机台
            String machineCode = allocateTrialMachine(trialPlan, machineCodes);
            if (machineCode == null) {
                result.getSkippedPlans().add(trialPlan.getMaterialCode() + "：无可用机台");
                continue;
            }

            // 确定班次
            List<String> availableShifts = determineTrialShifts(scheduleDate, machineCode);
            if (CollectionUtils.isEmpty(availableShifts)) {
                result.getSkippedPlans().add(trialPlan.getMaterialCode() + "：无可用班次");
                continue;
            }

            // 计算试制数量
            int trialQuantity = calculateTrialQuantity(trialPlan);

            // 创建排程明细
            CxScheduleDetail detail = createTrialScheduleDetail(
                    trialPlan, scheduleDate, machineCode, 
                    availableShifts.get(0), trialQuantity);

            if (detail != null) {
                result.getScheduledDetails().add(detail);
                scheduledCount++;

                // 更新试制计划状态
                updateTrialPlanStatus(trialPlan.getId(), "SCHEDULED");

                log.info("试制排程成功，物料：{}，机台：{}，班次：{}，数量：{}",
                        trialPlan.getMaterialCode(), machineCode, 
                        availableShifts.get(0), trialQuantity);
            }
        }

        result.setSuccess(!CollectionUtils.isEmpty(result.getScheduledDetails()));
        result.setMessage(String.format("成功安排 %d 个试制任务，跳过 %d 个",
                result.getScheduledDetails().size(), result.getSkippedPlans().size()));

        return result;
    }

    @Override
    public TrialConstraintResult checkTrialConstraints(LocalDate scheduleDate, CxTrialPlan trialPlan) {
        TrialConstraintResult result = new TrialConstraintResult();
        result.setViolations(new ArrayList<>());

        // 1. 检查是否为周日
        if (scheduleDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            result.getViolations().add("周日不安排试制");
        }

        // 2. 检查当天是否已达到上限
        int todayTrialCount = getTodayTrialTaskCount(scheduleDate);
        if (todayTrialCount >= 2) {
            result.getViolations().add("当天已安排2个试制任务");
        }

        // 3. 检查数量是否为双数
        if (trialPlan.getPlanQuantity() != null && trialPlan.getPlanQuantity() % 2 != 0) {
            result.getViolations().add("试制数量必须是双数");
        }

        // 4. 使用约束校验服务检查
        ConstraintCheckService.ConstraintCheckResult constraintResult = 
                constraintCheckService.checkTrialConstraint(
                        scheduleDate.atStartOfDay(),
                        todayTrialCount,
                        null,
                        trialPlan.getPlanQuantity() != null ? trialPlan.getPlanQuantity() : 0);

        if (!constraintResult.isPassed()) {
            result.getViolations().addAll(constraintResult.getViolations());
        }

        result.setPassed(result.getViolations().isEmpty());
        result.setMessage(result.isPassed() ? "约束检查通过" : 
                String.join("；", result.getViolations()));

        return result;
    }

    @Override
    public String allocateTrialMachine(CxTrialPlan trialPlan, List<String> availableMachines) {
        if (CollectionUtils.isEmpty(availableMachines)) {
            return null;
        }

        // TODO: 实现更复杂的机台选择逻辑
        // 优先选择：
        // 1. 有该物料试制经验的机台
        // 2. 当前负载较低的机台
        // 3. 符合结构约束的机台

        // 简化处理：选择第一个可用机台
        return availableMachines.get(0);
    }

    @Override
    public List<String> determineTrialShifts(LocalDate scheduleDate, String machineCode) {
        // 试制只能在早班或中班排
        List<String> availableShifts = new ArrayList<>();
        availableShifts.add("SHIFT_MORNING");
        availableShifts.add("SHIFT_AFTERNOON");

        // 检查每个班次是否可用
        List<String> shiftsToRemove = new ArrayList<>();
        for (String shift : availableShifts) {
            // 检查该班次是否已有其他任务
            Long taskCount = scheduleDetailMapper.selectCount(
                    new LambdaQueryWrapper<CxScheduleDetail>()
                            .eq(CxScheduleDetail::getScheduleDate, scheduleDate)
                            .eq(CxScheduleDetail::getCxMachineCode, machineCode)
                            .eq(CxScheduleDetail::getShiftCode, shift));

            if (taskCount != null && taskCount > 0) {
                shiftsToRemove.add(shift);
            }
        }

        availableShifts.removeAll(shiftsToRemove);
        return availableShifts;
    }

    @Override
    public int calculateTrialQuantity(CxTrialPlan trialPlan) {
        // 如果已指定数量，直接返回（确保是双数）
        if (trialPlan.getPlanQuantity() != null && trialPlan.getPlanQuantity() > 0) {
            int quantity = trialPlan.getPlanQuantity();
            if (quantity % 2 != 0) {
                quantity = quantity + 1; // 向上取整到双数
            }
            return quantity;
        }

        // 根据试制目的确定数量
        String trialPurpose = trialPlan.getTrialPurpose();
        if ("NEW_PRODUCT".equals(trialPurpose)) {
            return 24; // 新产品试制：24条
        } else if ("PROCESS_TEST".equals(trialPurpose)) {
            return 12; // 工艺试验：12条
        } else if ("MATERIAL_TEST".equals(trialPurpose)) {
            return 12; // 材料试验：12条
        } else {
            return 24; // 默认：24条
        }
    }

    @Override
    public TrialEvaluationResult evaluateTrialResult(Long scheduleDetailId, int actualQuantity, BigDecimal qualityRate) {
        TrialEvaluationResult result = new TrialEvaluationResult();

        // 获取排程明细
        CxScheduleDetail detail = scheduleDetailMapper.selectById(scheduleDetailId);
        if (detail == null) {
            result.setSuccess(false);
            result.setEvaluation("未找到排程明细");
            return result;
        }

        // 计算完成率
        BigDecimal completionRate = BigDecimal.ZERO;
        if (detail.getPlanQty() != null && detail.getPlanQty() > 0) {
            completionRate = BigDecimal.valueOf(actualQuantity)
                    .divide(BigDecimal.valueOf(detail.getPlanQty()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // 计算综合得分
        BigDecimal score = completionRate.multiply(new BigDecimal("0.6"))
                .add(qualityRate.multiply(new BigDecimal("0.4")));

        // 评价
        if (score.compareTo(new BigDecimal("90")) >= 0) {
            result.setSuccess(true);
            result.setEvaluation("优秀");
            result.setSuggestion("可以转为量产");
        } else if (score.compareTo(new BigDecimal("70")) >= 0) {
            result.setSuccess(true);
            result.setEvaluation("良好");
            result.setSuggestion("建议小批量试产后再转量产");
        } else if (score.compareTo(new BigDecimal("50")) >= 0) {
            result.setSuccess(false);
            result.setEvaluation("一般");
            result.setSuggestion("需要改进后重新试制");
        } else {
            result.setSuccess(false);
            result.setEvaluation("不合格");
            result.setSuggestion("需要全面改进");
        }

        result.setScore(score);

        // 更新排程明细
        detail.setActualQty(actualQuantity);
        detail.setCompletionRate(completionRate);
        detail.setStatus("COMPLETED");
        scheduleDetailMapper.updateById(detail);

        log.info("试制评价完成，明细ID：{}，完成率：{}%，合格率：{}%，得分：{}，评价：{}",
                scheduleDetailId, completionRate, qualityRate, score, result.getEvaluation());

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean convertTrialToMassProduction(String materialCode) {
        // TODO: MdmMaterialInfo 没有 isTrial 字段，需要确认如何实现试制转量产的业务逻辑
        // 可能需要：
        // 1. 在其他表中维护试制状态
        // 2. 或者使用 MdmMaterialInfo 的其他字段来标记
        log.warn("试制转量产功能待确认实现方式，物料编码: {}", materialCode);
        return true;
    }

    @Override
    public int getTodayTrialTaskCount(LocalDate scheduleDate) {
        // 查询当天已安排的试制任务数量
        Long count = trialPlanMapper.selectCount(
                new LambdaQueryWrapper<CxTrialPlan>()
                        .eq(CxTrialPlan::getPlanDate, scheduleDate)
                        .in(CxTrialPlan::getStatus, Arrays.asList("SCHEDULED", "IN_PROGRESS")));
        return count != null ? count.intValue() : 0;
    }

    @Override
    public List<CxTrialPlan> getPendingTrialPlans() {
        return trialPlanMapper.selectList(
                new LambdaQueryWrapper<CxTrialPlan>()
                        .eq(CxTrialPlan::getStatus, "PENDING")
                        .orderByAsc(CxTrialPlan::getPriority)
                        .orderByAsc(CxTrialPlan::getPlanDate));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTrialPlanStatus(Long trialPlanId, String status) {
        try {
            trialPlanMapper.update(null,
                    new LambdaUpdateWrapper<CxTrialPlan>()
                            .eq(CxTrialPlan::getId, trialPlanId)
                            .set(CxTrialPlan::getStatus, status)
                            .set(CxTrialPlan::getUpdateTime, LocalDateTime.now()));

            return true;

        } catch (Exception e) {
            log.error("更新试制计划状态失败", e);
            return false;
        }
    }

    /**
     * 创建试制排程明细
     */
    private CxScheduleDetail createTrialScheduleDetail(CxTrialPlan trialPlan, LocalDate scheduleDate,
                                                        String machineCode, String shiftCode, int quantity) {
        try {
            CxScheduleDetail detail = new CxScheduleDetail();
            detail.setScheduleDate(scheduleDate);
            detail.setMachineCode(machineCode);
            detail.setMaterialCode(trialPlan.getMaterialCode());
            detail.setShiftCode(shiftCode);
            detail.setPlanQty(quantity);
            detail.setStatus("PLANNED");
            detail.setIsTrial(1);
            detail.setTrialPlanId(trialPlan.getId());
            detail.setSequence(1); // 试制任务优先
            detail.setCreateTime(LocalDateTime.now());

            scheduleDetailMapper.insert(detail);
            return detail;

        } catch (Exception e) {
            log.error("创建试制排程明细失败", e);
            return null;
        }
    }
}
