package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.entity.*;
import com.zlt.aps.cx.entity.config.CxKeyProduct;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxStructureShiftCapacity;
import com.zlt.aps.cx.entity.mdm.MdmCxMachineOnlineInfo;
import com.zlt.aps.cx.entity.mdm.MdmMaterialInfo;
import com.zlt.aps.cx.entity.mdm.MdmMoldingMachine;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.CxTrialPlan;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private MdmMaterialInfoMapper materialInfoMapper;

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

    @Autowired
    private CxKeyProductMapper keyProductMapper;

    @Autowired
    private MdmMonthSurplusMapper monthSurplusMapper;

    @Autowired
    private MdmSkuScheduleCategoryMapper skuScheduleCategoryMapper;

    @Autowired
    private LhScheduleResultMapper lhScheduleResultMapper;

    @Autowired
    private MdmCxMachineOnlineInfoMapper onlineInfoMapper;

    @Autowired
    private CxStructureEndingMapper structureEndingMapper;

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

            // 4. 执行核心排程算法（包含续作、试制、正常任务的统一处理）
            // 任务优先级：续作 > 新增任务（试制在有空出产能时优先，但不挤掉实单）
            List<CxScheduleResult> scheduleResults = coreAlgorithmService.executeSchedule(context);

            // 5. 应用节假日调整
            scheduleResults = holidayScheduleService.adjustHolidaySchedule(
                    request.getScheduleDate(), scheduleResults);

            // 6. 保存排程结果
            saveScheduleResults(scheduleResults);

            // 7. 验证排程结果
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
            LocalDate scheduleDate = request.getScheduleDate();

            // 1. 获取机台信息
            List<MdmMoldingMachine> machines = moldingMachineMapper.selectList(
                    new LambdaQueryWrapper<MdmMoldingMachine>()
                            .eq(MdmMoldingMachine::getIsActive, 1)
                            .ne(MdmMoldingMachine::getMaintainStatus, "FAULT"));
            context.setMachines(machines);
            context.setAvailableMachines(machines);

            // 2. 获取物料信息
            List<MdmMaterialInfo> materials = materialInfoMapper.selectList(
                    new LambdaQueryWrapper<MdmMaterialInfo>());
            context.setMaterials(materials);

            // 3. 获取库存信息
            List<CxStock> stocks = stockMapper.selectList(
                    new LambdaQueryWrapper<CxStock>()
                            .gt(CxStock::getCurrentStock, 0));
            context.setStocks(stocks);

            // 4. 【主要任务来源】获取硫化排程结果
            // 从T_LH_SCHEDULE_RESULT获取今日硫化计划
            List<LhScheduleResult> lhScheduleResults = lhScheduleResultMapper.selectByDate(scheduleDate);
            context.setLhScheduleResults(lhScheduleResults);
            log.info("加载硫化排程结果 {} 条", lhScheduleResults.size());

            // 5. 【续作判断】获取成型在机信息
            // 从T_MDM_CX_MACHINE_ONLINE_INFO获取当前机台正在做的胎胚
            // 查询今天和昨天在机的信息（可能跨班次生产）
            List<MdmCxMachineOnlineInfo> onlineInfos = onlineInfoMapper.selectByDateRange(
                    scheduleDate, scheduleDate.minusDays(1));
            context.setOnlineInfos(onlineInfos);
            log.info("加载成型在机信息 {} 条", onlineInfos.size());

            // 6. 构建机台在机胎胚映射（快速查询用）
            // Key: 成型机台编码, Value: 该机台正在做的胎胚编码集合
            Map<String, Set<String>> machineOnlineEmbryoMap = new HashMap<>();
            for (MdmCxMachineOnlineInfo onlineInfo : onlineInfos) {
                String cxCode = onlineInfo.getCxCode();
                String embryoCode = onlineInfo.getMesMaterialCode();
                if (cxCode != null && embryoCode != null) {
                    machineOnlineEmbryoMap.computeIfAbsent(cxCode, k -> new HashSet<>())
                            .add(embryoCode);
                }
            }
            context.setMachineOnlineEmbryoMap(machineOnlineEmbryoMap);
            log.info("构建机台在机胎胚映射，共 {} 个机台有在机任务", machineOnlineEmbryoMap.size());

            // 7. 获取参数配置
            List<CxParamConfig> paramConfigs = paramConfigMapper.selectList(null);
            // 转换为Map方便查询
            Map<String, CxParamConfig> paramConfigMap = paramConfigs.stream()
                    .collect(Collectors.toMap(CxParamConfig::getParamCode, p -> p, (a, b) -> a));
            context.setParamConfigMap(paramConfigMap);
            
            // 加载损耗率
            CxParamConfig lossRateConfig = paramConfigMap.get("LOSS_RATE");
            java.math.BigDecimal lossRate = lossRateConfig != null 
                    ? new java.math.BigDecimal(lossRateConfig.getParamValue()) 
                    : new java.math.BigDecimal("0.02");
            context.setLossRate(lossRate);
            
            // 加载预留消化时间
            CxParamConfig reservedHoursConfig = paramConfigMap.get("RESERVED_DIGEST_HOURS");
            Integer reservedDigestHours = reservedHoursConfig != null 
                    ? Integer.parseInt(reservedHoursConfig.getParamValue()) 
                    : 1;
            context.setReservedDigestHours(reservedDigestHours);
            
            // 加载胎胚最长停放时间
            CxParamConfig maxParkingConfig = paramConfigMap.get("MAX_PARKING_HOURS");
            Integer maxParkingHours = maxParkingConfig != null 
                    ? Integer.parseInt(maxParkingConfig.getParamValue()) 
                    : 24;
            context.setMaxParkingHours(maxParkingHours);

            // 8. 获取结构班产配置（整车条数）
            List<CxStructureShiftCapacity> structureShiftCapacities = structureShiftCapacityMapper.selectList(
                    new LambdaQueryWrapper<CxStructureShiftCapacity>()
                            .eq(CxStructureShiftCapacity::getIsActive, 1));
            context.setStructureShiftCapacities(structureShiftCapacities);

            // 9. 获取关键产品配置
            List<CxKeyProduct> keyProducts = keyProductMapper.selectList(
                    new LambdaQueryWrapper<CxKeyProduct>()
                            .eq(CxKeyProduct::getIsActive, 1));
            context.setKeyProducts(keyProducts);
            
            // 构建关键产品编码集合（快速查询用）
            Set<String> keyProductCodes = new HashSet<>();
            for (CxKeyProduct product : keyProducts) {
                keyProductCodes.add(product.getEmbryoCode());
            }
            context.setKeyProductCodes(keyProductCodes);

            // 10. 获取结构收尾管理列表
            List<CxStructureEnding> structureEndings = structureEndingMapper.selectList(
                    new LambdaQueryWrapper<CxStructureEnding>()
                            .eq(CxStructureEnding::getIsActive, 1));
            context.setStructureEndings(structureEndings);

            // 11. 获取月度计划余量（用于收尾计算）
            int year = scheduleDate.getYear();
            int month = scheduleDate.getMonthValue();
            List<com.zlt.aps.cx.entity.mdm.MdmMonthSurplus> monthSurplusList = 
                    monthSurplusMapper.selectByYearMonth(year, month);
            context.setMonthSurplusList(monthSurplusList);
            // 构建物料编码映射
            Map<String, com.zlt.aps.cx.entity.mdm.MdmMonthSurplus> monthSurplusMap = monthSurplusList.stream()
                    .collect(Collectors.toMap(
                            com.zlt.aps.cx.entity.mdm.MdmMonthSurplus::getMaterialCode, 
                            s -> s, (a, b) -> a));
            context.setMonthSurplusMap(monthSurplusMap);
            log.info("加载月度计划余量 {} 条", monthSurplusList.size());

            // 12. 获取SKU排产分类（用于判断主销产品）
            List<com.zlt.aps.cx.entity.mdm.MdmSkuScheduleCategory> skuCategories = 
                    skuScheduleCategoryMapper.selectAllCategories();
            context.setSkuScheduleCategories(skuCategories);
            // 构建主销产品编码集合（SCHEDULE_TYPE='01'）
            Set<String> mainProductCodes = skuCategories.stream()
                    .filter(c -> "01".equals(c.getScheduleType()))
                    .map(com.zlt.aps.cx.entity.mdm.MdmSkuScheduleCategory::getMaterialCode)
                    .collect(Collectors.toSet());
            context.setMainProductCodes(mainProductCodes);
            log.info("加载SKU排产分类 {} 条，其中主销产品 {} 个", skuCategories.size(), mainProductCodes.size());

            // 13. 设置节假日相关标记
            context.setIsOpeningDay(holidayScheduleService.isStartProductionDay(scheduleDate));
            context.setIsClosingDay(holidayScheduleService.isStopProductionDay(scheduleDate));
            context.setIsBeforeClosingDay(holidayScheduleService.isBeforeHoliday(scheduleDate));

            // 14. 设置排程参数
            context.setScheduleDate(scheduleDate);
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
