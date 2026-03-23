package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.entity.*;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxStructureShiftCapacity;
import com.zlt.aps.cx.entity.mdm.MdmMoldingMachine;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.CxTrialPlan;
import com.zlt.aps.cx.mapper.*;
import com.zlt.aps.cx.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 排程服务实现类
 * 
 * 整合所有核心服务，实现完整的排程流程
 *
 * @author APS Team
 */
@Slf4j
@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private CoreScheduleAlgorithmService coreAlgorithmService;

    @Autowired
    private ConstraintCheckService constraintCheckService;

    @Autowired
    private DynamicAdjustService dynamicAdjustService;

    @Autowired
    private HolidayScheduleService holidayScheduleService;

    @Autowired
    private TrialScheduleService trialScheduleService;

    @Autowired
    private MdmMoldingMachineMapper moldingMachineMapper;

    @Autowired
    private CxMaterialMapper materialMapper;

    @Autowired
    private CxStockMapper stockMapper;

    @Autowired
    private CxLhPlanMapper lhPlanMapper;

    @Autowired
    private CxScheduleResultMapper scheduleResultMapper;

    @Autowired
    private CxScheduleDetailMapper scheduleDetailMapper;

    @Autowired
    private CxParamConfigMapper paramConfigMapper;

    @Autowired
    private CxTrialPlanMapper trialPlanMapper;

    @Autowired
    private CxStructureShiftCapacityMapper structureShiftCapacityMapper;

    @Override
    public ScheduleResult executeSchedule(ScheduleRequest request) {
        ScheduleResult result = new ScheduleResult();
        result.setSuccess(false);
        result.setScheduleDate(request.getScheduleDate());

        try {
            log.info("开始执行排程，日期：{}，排程模式：{}", 
                    request.getScheduleDate(), request.getScheduleMode());

            // 1. 检查节假日
            if (holidayScheduleService.isStopProductionDay(request.getScheduleDate())) {
                result.setMessage("停产日，不执行排程");
                log.info("停产日 {}，跳过排程", request.getScheduleDate());
                return result;
            }

            // 2. 构建排程上下文
            ScheduleContextDTO context = buildScheduleContext(request);
            if (context == null) {
                result.setMessage("构建排程上下文失败");
                return result;
            }

            // 3. 处理节假日特殊逻辑
            if (holidayScheduleService.isBeforeHoliday(request.getScheduleDate())) {
                HolidayScheduleService.HolidayScheduleResult holidayResult = 
                        holidayScheduleService.handleBeforeHoliday(request.getScheduleDate());
                log.info("停产前一天处理结果：{}", holidayResult.getMessage());
            }

            // 4. 执行试制排程
            executeTrialScheduleInternal(request.getScheduleDate());

            // 5. 执行核心排程算法
            List<CxScheduleResult> scheduleResults = coreAlgorithmService.executeSchedule(context);

            // 6. 应用节假日调整
            scheduleResults = holidayScheduleService.adjustHolidaySchedule(
                    request.getScheduleDate(), scheduleResults);

            // 7. 保存排程结果
            saveScheduleResults(scheduleResults);

            // 8. 验证排程结果
            boolean validated = validateScheduleResults(scheduleResults);

            result.setSuccess(validated);
            result.setMessage(validated ? "排程成功" : "排程完成，但存在约束冲突");
            result.setResults(scheduleResults);

            log.info("排程执行完成，日期：{}，结果数量：{}", 
                    request.getScheduleDate(), scheduleResults.size());

        } catch (Exception e) {
            log.error("排程执行失败", e);
            result.setMessage("排程失败：" + e.getMessage());
        }

        return result;
    }

    @Override
    public ScheduleContextDTO buildScheduleContext(ScheduleRequest request) {
        try {
            ScheduleContextDTO context = new ScheduleContextDTO();

            // 1. 获取机台信息
            List<MdmMoldingMachine> machines = moldingMachineMapper.selectList(
                    new LambdaQueryWrapper<MdmMoldingMachine>()
                            .eq(MdmMoldingMachine::getIsActive, 1)
                            .ne(MdmMoldingMachine::getMaintainStatus, "FAULT"));
            context.setMachines(machines);

            // 2. 获取物料信息
            List<CxMaterial> materials = materialMapper.selectList(
                    new LambdaQueryWrapper<CxMaterial>()
                            .eq(CxMaterial::getIsActive, 1));
            context.setMaterials(materials);

            // 3. 获取库存信息
            List<CxStock> stocks = stockMapper.selectList(
                    new LambdaQueryWrapper<CxStock>()
                            .gt(CxStock::getCurrentStock, 0));
            context.setStocks(stocks);

            // 4. 获取硫化计划（需求）
            List<CxLhPlan> lhPlans = lhPlanMapper.selectList(
                    new LambdaQueryWrapper<CxLhPlan>()
                            .eq(CxLhPlan::getPlanDate, request.getScheduleDate())
                            .eq(CxLhPlan::getStatus, "PENDING"));
            context.setLhPlans(lhPlans);

            // 5. 获取参数配置
            List<CxParamConfig> paramConfigs = paramConfigMapper.selectList(null);
            context.setParamConfigs(paramConfigs);

            // 6. 获取结构班产配置（整车条数）
            List<CxStructureShiftCapacity> structureShiftCapacities = structureShiftCapacityMapper.selectList(
                    new LambdaQueryWrapper<CxStructureShiftCapacity>()
                            .eq(CxStructureShiftCapacity::getIsActive, 1));
            context.setStructureShiftCapacities(structureShiftCapacities);

            // 7. 设置排程参数
            context.setScheduleDate(request.getScheduleDate());
            context.setScheduleMode(request.getScheduleMode());
            context.setReScheduleType(request.getReScheduleType());

            return context;

        } catch (Exception e) {
            log.error("构建排程上下文失败", e);
            return null;
        }
    }

    @Override
    public boolean executeDynamicAdjust(String shiftCode) {
        try {
            log.info("执行动态调整，班次：{}", shiftCode);

            // 获取当前日期
            LocalDate today = LocalDate.now();

            // 执行交班前检查和调整
            DynamicAdjustService.ShiftAdjustResult adjustResult = 
                    dynamicAdjustService.checkAndAdjustBeforeShiftEnd(
                            today.atStartOfDay(), shiftCode);

            if (adjustResult.isAdjusted()) {
                log.info("动态调整完成：{}", adjustResult.getMessage());
            } else {
                log.info("无需动态调整：{}", adjustResult.getMessage());
            }

            return true;

        } catch (Exception e) {
            log.error("动态调整失败", e);
            return false;
        }
    }

    @Override
    public ScheduleResult executeTrialSchedule(LocalDate scheduleDate) {
        ScheduleResult result = new ScheduleResult();
        result.setScheduleDate(scheduleDate);

        try {
            // 获取待排程的试制计划
            List<CxTrialPlan> trialPlans = trialScheduleService.getPendingTrialPlans();

            // 执行试制排程
            TrialScheduleService.TrialScheduleResult trialResult = 
                    trialScheduleService.executeTrialSchedule(scheduleDate, trialPlans);

            result.setSuccess(trialResult.isSuccess());
            result.setMessage(trialResult.getMessage());

            // 转换为通用排程结果格式
            if (!CollectionUtils.isEmpty(trialResult.getScheduledDetails())) {
                List<CxScheduleResult> results = new ArrayList<>();
                for (CxScheduleDetail detail : trialResult.getScheduledDetails()) {
                    CxScheduleResult sr = new CxScheduleResult();
                    sr.setCxMachineCode(detail.getMachineCode());
                    sr.setEmbryoCode(detail.getMaterialCode());
                    sr.setProductNum(BigDecimal.valueOf(detail.getPlanQty()));
                    sr.setScheduleDate(detail.getScheduleDate());
                    sr.setShiftCode(detail.getShiftCode());
                    sr.setIsTrial(1);
                    results.add(sr);
                }
                result.setResults(results);
            }

            log.info("试制排程完成，日期：{}，结果：{}", scheduleDate, trialResult.getMessage());

        } catch (Exception e) {
            log.error("试制排程失败", e);
            result.setSuccess(false);
            result.setMessage("试制排程失败：" + e.getMessage());
        }

        return result;
    }

    @Override
    public boolean executeReSchedule(ReScheduleRequest request) {
        try {
            log.info("执行重排程，类型：{}，原因：{}", 
                    request.getReScheduleType(), request.getReason());

            // 构建排程请求
            ScheduleRequest scheduleRequest = new ScheduleRequest();
            scheduleRequest.setScheduleDate(request.getScheduleDate());
            scheduleRequest.setScheduleMode("RE_SCHEDULE");
            scheduleRequest.setReScheduleType(request.getReScheduleType());
            scheduleRequest.setAffectedMachineCodes(request.getAffectedMachineCodes());

            // 执行排程
            ScheduleResult result = executeSchedule(scheduleRequest);

            return result.isSuccess();

        } catch (Exception e) {
            log.error("重排程失败", e);
            return false;
        }
    }

    @Override
    public ScheduleValidationResult validateSchedule(LocalDate scheduleDate) {
        ScheduleValidationResult result = new ScheduleValidationResult();
        result.setValid(true);
        result.setErrors(new ArrayList<>());
        result.setWarnings(new ArrayList<>());

        try {
            // 获取该日期的所有排程结果
            List<CxScheduleResult> results = scheduleResultMapper.selectList(
                    new LambdaQueryWrapper<CxScheduleResult>()
                            .eq(CxScheduleResult::getScheduleDate, scheduleDate));

            if (CollectionUtils.isEmpty(results)) {
                result.setValid(false);
                result.getErrors().add("无排程结果");
                return result;
            }

            // 逐个校验
            for (CxScheduleResult scheduleResult : results) {
                ConstraintCheckService.ConstraintCheckResult checkResult = 
                        constraintCheckService.checkAllConstraints(scheduleResult);

                if (!checkResult.isPassed()) {
                    result.setValid(false);
                    result.getErrors().addAll(checkResult.getViolations());
                }

                if (!CollectionUtils.isEmpty(checkResult.getWarnings())) {
                    result.getWarnings().addAll(checkResult.getWarnings());
                }
            }

        } catch (Exception e) {
            log.error("验证排程失败", e);
            result.setValid(false);
            result.getErrors().add("验证异常：" + e.getMessage());
        }

        return result;
    }

    /**
     * 执行内部试制排程
     */
    private void executeTrialScheduleInternal(LocalDate scheduleDate) {
        try {
            // 获取待排程的试制计划
            List<CxTrialPlan> trialPlans = trialScheduleService.getPendingTrialPlans();

            if (!CollectionUtils.isEmpty(trialPlans)) {
                // 执行试制排程
                TrialScheduleService.TrialScheduleResult trialResult = 
                        trialScheduleService.executeTrialSchedule(scheduleDate, trialPlans);

                log.info("试制排程完成：{}", trialResult.getMessage());
            }

        } catch (Exception e) {
            log.error("试制排程失败", e);
        }
    }

    /**
     * 保存排程结果
     */
    @Transactional(rollbackFor = Exception.class)
    private void saveScheduleResults(List<CxScheduleResult> results) {
        if (CollectionUtils.isEmpty(results)) {
            return;
        }

        for (CxScheduleResult result : results) {
            result.setCreateTime(LocalDateTime.now());
            result.setStatus("PLANNED");
            scheduleResultMapper.insert(result);

            // 保存明细
            if (!CollectionUtils.isEmpty(result.getDetails())) {
                for (CxScheduleDetail detail : result.getDetails()) {
                    detail.setResultId(result.getId());
                    detail.setCreateTime(LocalDateTime.now());
                    scheduleDetailMapper.insert(detail);
                }
            }
        }

        log.info("保存排程结果 {} 条", results.size());
    }

    /**
     * 验证排程结果
     */
    private boolean validateScheduleResults(List<CxScheduleResult> results) {
        if (CollectionUtils.isEmpty(results)) {
            return false;
        }

        int validCount = 0;
        for (CxScheduleResult result : results) {
            ConstraintCheckService.ConstraintCheckResult checkResult = 
                    constraintCheckService.checkAllConstraints(result);
            if (checkResult.isPassed()) {
                validCount++;
            } else {
                log.warn("排程结果存在约束冲突，机台：{}，物料：{}，冲突：{}",
                        result.getCxMachineCode(), result.getEmbryoCode(), 
                        checkResult.getViolations());
            }
        }

        return validCount == results.size();
    }
}
