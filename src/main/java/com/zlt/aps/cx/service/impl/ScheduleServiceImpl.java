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
import com.zlt.aps.cx.entity.mdm.MdmMonthSurplus;
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
import java.math.RoundingMode;
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

    @Autowired
    private FactoryMonthPlanProductionFinalResultMapper monthPlanMapper;

    @Override
    public CxScheduleResult executeSchedule(ScheduleRequest request) {
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

            // 10. 获取结构收尾管理列表（从FactoryMonthPlanProductionFinalResult计算生成）
            int year = scheduleDate.getYear();
            int month = scheduleDate.getMonthValue();
            List<CxStructureEnding> structureEndings = calculateStructureEndings(scheduleDate, year, month);
            context.setStructureEndings(structureEndings);
            log.info("从月计划计算生成结构收尾信息 {} 条", structureEndings.size());

            // 11. 获取月度计划余量（用于收尾计算）
            List<MdmMonthSurplus> monthSurplusList = 
                    monthSurplusMapper.selectByYearMonth(year, month);
            context.setMonthSurplusList(monthSurplusList);
            // 构建物料编码映射
            Map<String, MdmMonthSurplus> monthSurplusMap = monthSurplusList.stream()
                    .collect(Collectors.toMap(
                            MdmMonthSurplus::getMaterialCode, 
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

    /**
     * 从FactoryMonthPlanProductionFinalResult计算生成结构收尾管理列表
     * 
     * 收尾管理规则（严格依据月计划收尾日）：
     * 1. 收尾日判断：从月计划day_1到day_31找到最后一个有排产的日期
     * 2. 收尾前10天检查：
     *    - 计算：成型余量 = 硫化余量 - 胎胚库存
     *    - 判断：能否在收尾日前完成？
     * 3. 延误量追赶：
     *    - 如果做不完，计算延误量，平摊到未来3天
     *    - 检查未来3天满产是否能追上
     * 4. 满产判断：
     *    - 如果未来3天满产仍追不上，通知月计划调整（调用硫化接口）
     *
     * @param scheduleDate 排程日期
     * @param year 年份
     * @param month 月份
     * @return 结构收尾管理列表
     */
    private List<CxStructureEnding> calculateStructureEndings(LocalDate scheduleDate, int year, int month) {
        List<CxStructureEnding> resultList = new ArrayList<>();
        
        try {
            // 1. 获取当月月计划数据
            Integer yearMonth = year * 100 + month;
            List<FactoryMonthPlanProductionFinalResult> monthPlans = monthPlanMapper.selectByYearMonth(yearMonth);
            
            if (CollectionUtils.isEmpty(monthPlans)) {
                log.warn("未找到 {} 年 {} 月的月计划数据", year, month);
                return resultList;
            }
            
            // 2. 获取月度计划余量（硫化余量）
            List<MdmMonthSurplus> monthSurplusList = monthSurplusMapper.selectByYearMonth(year, month);
            Map<String, MdmMonthSurplus> surplusMap = monthSurplusList.stream()
                    .collect(Collectors.toMap(MdmMonthSurplus::getMaterialCode, s -> s, (a, b) -> a));
            
            // 3. 获取胎胚库存
            List<CxStock> stocks = stockMapper.selectList(
                    new LambdaQueryWrapper<CxStock>().gt(CxStock::getCurrentStock, 0));
            Map<String, CxStock> stockMap = stocks.stream()
                    .collect(Collectors.toMap(CxStock::getMaterialCode, s -> s, (a, b) -> a));
            
            // 4. 获取成型机台列表（用于计算满产能力）
            List<MdmMoldingMachine> machines = moldingMachineMapper.selectList(
                    new LambdaQueryWrapper<MdmMoldingMachine>()
                            .eq(MdmMoldingMachine::getIsActive, 1)
                            .ne(MdmMoldingMachine::getMaintainStatus, "FAULT"));
            
            // 5. 按结构分组汇总
            Map<String, List<FactoryMonthPlanProductionFinalResult>> structurePlanMap = monthPlans.stream()
                    .filter(p -> p.getStructureName() != null)
                    .collect(Collectors.groupingBy(FactoryMonthPlanProductionFinalResult::getStructureName));
            
            // 6. 当前日期信息
            int currentDay = scheduleDate.getDayOfMonth();
            int lastDayOfMonth = scheduleDate.lengthOfMonth();
            
            // 7. 遍历每个结构，计算收尾信息
            for (Map.Entry<String, List<FactoryMonthPlanProductionFinalResult>> entry : structurePlanMap.entrySet()) {
                String structureName = entry.getKey();
                List<FactoryMonthPlanProductionFinalResult> plans = entry.getValue();
                
                // ========== Step 1: 确定收尾日 ==========
                // 从月计划中找到该结构最后一个有排产的日期
                int endingDay = findEndingDay(plans, lastDayOfMonth);
                LocalDate endingDate = scheduleDate.withDayOfMonth(endingDay);
                
                // 如果收尾日已经过了，跳过
                if (endingDay < currentDay) {
                    log.debug("结构 {} 收尾日 {} 已过，跳过", structureName, endingDate);
                    continue;
                }
                
                // 创建收尾记录
                CxStructureEnding ending = new CxStructureEnding();
                ending.setStructureName(structureName);
                ending.setStructureCode(structureName);
                ending.setStatDate(scheduleDate);
                ending.setPlannedEndingDate(endingDate);
                
                // ========== Step 2: 计算成型余量 ==========
                // 获取该结构对应的物料信息
                FactoryMonthPlanProductionFinalResult firstPlan = plans.get(0);
                String materialCode = firstPlan.getMaterialCode();
                
                // 硫化余量（从月度计划余量表获取）
                MdmMonthSurplus surplus = surplusMap.get(materialCode);
                
                // 计算剩余排产量（从当前日期到收尾日）
                int remainingPlanQty = 0;
                for (FactoryMonthPlanProductionFinalResult plan : plans) {
                    for (int day = currentDay; day <= endingDay; day++) {
                        Integer dayQty = plan.getDayQty(day);
                        if (dayQty != null && dayQty > 0) {
                            remainingPlanQty += dayQty;
                        }
                    }
                }
                
                int vulcanizingRemainder = surplus != null && surplus.getPlanSurplusQty() != null 
                        ? surplus.getPlanSurplusQty() : remainingPlanQty;
                ending.setVulcanizingRemainder(vulcanizingRemainder);
                
                // 胎胚库存
                CxStock stock = stockMap.get(materialCode);
                int embryoStock = stock != null && stock.getCurrentStock() != null 
                        ? stock.getCurrentStock() : 0;
                ending.setEmbryoStock(embryoStock);
                
                // 成型余量 = 硫化余量 - 胎胚库存（需要生产的量）
                int formingRemainder = Math.max(0, vulcanizingRemainder - embryoStock);
                ending.setFormingRemainder(formingRemainder);
                
                // 日产能（取日硫化量，或从结构班产配置获取）
                Integer dailyCapacity = firstPlan.getDayVulcanizationQty();
                if (dailyCapacity == null || dailyCapacity <= 0) {
                    dailyCapacity = calculateStructureDailyCapacity(structureName, machines);
                }
                ending.setDailyCapacity(dailyCapacity);
                
                // ========== Step 3: 计算距离收尾日的天数 ==========
                int daysToEnding = endingDay - currentDay + 1;
                
                // 预计收尾天数 = 成型余量 / 日产能
                BigDecimal estimatedDays = BigDecimal.ZERO;
                if (dailyCapacity > 0 && formingRemainder > 0) {
                    estimatedDays = BigDecimal.valueOf(formingRemainder)
                            .divide(BigDecimal.valueOf(dailyCapacity), 2, RoundingMode.HALF_UP);
                }
                ending.setEstimatedEndingDays(estimatedDays);
                
                // ========== Step 4: 判断是否紧急收尾（3天内） ==========
                boolean isUrgentEnding = daysToEnding <= 3 && formingRemainder > 0;
                ending.setIsUrgentEnding(isUrgentEnding ? 1 : 0);
                
                // ========== Step 5: 判断是否10天内收尾 ==========
                boolean isNearEnding = daysToEnding <= 10 && formingRemainder > 0;
                ending.setIsNearEnding(isNearEnding ? 1 : 0);
                
                // ========== Step 6: 收尾前10天检查 - 核心逻辑 ==========
                if (isNearEnding && formingRemainder > 0) {
                    // 收尾日前能生产的量
                    int canProduceBeforeEnding = daysToEnding * dailyCapacity;
                    
                    // 判断能否按计划收尾
                    if (formingRemainder <= canProduceBeforeEnding) {
                        // 能按计划收尾，无需追赶
                        ending.setDelayQuantity(0);
                        ending.setDistributedQuantity(0);
                        ending.setNeedMonthPlanAdjust(0);
                        log.info("结构 {} 可以按计划收尾，成型余量 {}，收尾日前产能 {}", 
                                structureName, formingRemainder, canProduceBeforeEnding);
                    } else {
                        // ========== 会延误，计算延误量 ==========
                        int delayQty = formingRemainder - canProduceBeforeEnding;
                        ending.setDelayQuantity(delayQty);
                        
                        // ========== 计算未来3天追赶能力 ==========
                        // 未来3天满产能力 = 3天 × 日产能
                        int next3DaysFullCapacity = 3 * dailyCapacity;
                        
                        // 未来3天计划产量（从月计划获取）
                        int next3DaysPlanQty = calculateNext3DaysPlanQty(plans, currentDay);
                        
                        // 未来3天可追加产能 = 满产能力 - 计划产量
                        int next3DaysAvailableCapacity = Math.max(0, next3DaysFullCapacity - next3DaysPlanQty);
                        
                        // 判断能否追赶
                        if (delayQty <= next3DaysAvailableCapacity) {
                            // 可以追赶上，平摊到未来3天
                            int distributedQty = (int) Math.ceil(delayQty / 3.0);
                            ending.setDistributedQuantity(distributedQty);
                            ending.setNeedMonthPlanAdjust(0);
                            log.info("结构 {} 延误量 {} 可追赶上，平摊到未来3天每天增加 {}", 
                                    structureName, delayQty, distributedQty);
                        } else {
                            // ========== 满产也追不上，需要通知月计划调整 ==========
                            ending.setDistributedQuantity(next3DaysAvailableCapacity / 3);
                            ending.setNeedMonthPlanAdjust(1);
                            log.warn("结构 {} 延误量 {} 超过未来3天满产能力 {}，需要调整月计划！", 
                                    structureName, delayQty, next3DaysAvailableCapacity);
                            
                            // TODO: 调用硫化调整接口通知月计划调整
                            // notifyMonthPlanAdjustment(structureName, delayQty, endingDate);
                        }
                    }
                } else {
                    // 10天以外，正常安排
                    ending.setDelayQuantity(0);
                    ending.setDistributedQuantity(0);
                    ending.setNeedMonthPlanAdjust(0);
                }
                
                ending.setCreateTime(LocalDateTime.now());
                ending.setUpdateTime(LocalDateTime.now());
                
                resultList.add(ending);
            }
            
            // 按紧急程度和收尾天数排序
            resultList.sort((a, b) -> {
                // 紧急收尾的排最前面
                if (a.getIsUrgentEnding() != b.getIsUrgentEnding()) {
                    return b.getIsUrgentEnding() - a.getIsUrgentEnding();
                }
                // 需要月计划调整的排前面
                if (a.getNeedMonthPlanAdjust() != b.getNeedMonthPlanAdjust()) {
                    return b.getNeedMonthPlanAdjust() - a.getNeedMonthPlanAdjust();
                }
                // 按预计收尾天数排序
                if (a.getEstimatedEndingDays() == null) return 1;
                if (b.getEstimatedEndingDays() == null) return -1;
                return a.getEstimatedEndingDays().compareTo(b.getEstimatedEndingDays());
            });
            
            log.info("计算结构收尾信息完成，共 {} 个结构，其中紧急收尾 {} 个，需调整月计划 {} 个", 
                    resultList.size(),
                    resultList.stream().filterToInt(e -> e.getIsUrgentEnding()).sum(),
                    resultList.stream().filterToInt(e -> e.getNeedMonthPlanAdjust()).sum());
            
        } catch (Exception e) {
            log.error("计算结构收尾信息失败", e);
        }
        
        return resultList;
    }
    
    /**
     * 找到该结构的收尾日（最后一个有排产的日期）
     */
    private int findEndingDay(List<FactoryMonthPlanProductionFinalResult> plans, int lastDayOfMonth) {
        int endingDay = 0;
        for (FactoryMonthPlanProductionFinalResult plan : plans) {
            for (int day = 1; day <= lastDayOfMonth; day++) {
                Integer dayQty = plan.getDayQty(day);
                if (dayQty != null && dayQty > 0 && day > endingDay) {
                    endingDay = day;
                }
            }
        }
        // 如果没找到，默认月底
        return endingDay > 0 ? endingDay : lastDayOfMonth;
    }
    
    /**
     * 计算结构日产能（从可用机台汇总）
     */
    private int calculateStructureDailyCapacity(String structureName, List<MdmMoldingMachine> machines) {
        // 简化处理：默认每个机台日产200条
        // TODO: 从结构班产配置获取实际产能
        int machineCount = machines.size();
        return machineCount * 200;
    }
    
    /**
     * 计算未来3天的计划产量
     */
    private int calculateNext3DaysPlanQty(List<FactoryMonthPlanProductionFinalResult> plans, int currentDay) {
        int totalQty = 0;
        for (FactoryMonthPlanProductionFinalResult plan : plans) {
            for (int day = currentDay; day <= Math.min(currentDay + 2, 31); day++) {
                Integer dayQty = plan.getDayQty(day);
                if (dayQty != null && dayQty > 0) {
                    totalQty += dayQty;
                }
            }
        }
        return totalQty;
    }
}
