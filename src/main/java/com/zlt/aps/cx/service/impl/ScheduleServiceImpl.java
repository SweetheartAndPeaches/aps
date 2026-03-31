package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.dto.ScheduleRequest;
import com.zlt.aps.cx.entity.*;
import com.zlt.aps.cx.entity.config.CxKeyProduct;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxStructureShiftCapacity;

import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.CxTrialPlan;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.enums.DayVulcanizationModeEnum;
import com.zlt.aps.cx.mapper.*;
import com.zlt.aps.cx.service.*;
import com.zlt.aps.mp.api.domain.entity.*;
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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    /** 默认工厂编号（当请求中未指定时使用） */
    private static final String DEFAULT_FACTORY_CODE = "DEFAULT";

    /** 默认排程天数（当班次配置为空时使用） */
    private static final int DEFAULT_SCHEDULE_DAYS = 3;

    /** 机台类型：成型 */
    private static final String MACHINE_TYPE_MOLDING = "成型";

    /** 日硫化量计算模式参数编码 */
    private static final String PARAM_CODE_DAY_VULCANIZATION_MODE = "DAY_VULCANIZATION_MODE";

    @Autowired
    private CoreScheduleAlgorithmService coreScheduleAlgorithmService;

    @Autowired
    private ConstraintCheckService constraintCheckService;

    @Autowired
    private HolidayScheduleService holidayScheduleService;

    @Autowired
    private MdmMoldingMachineMapper moldingMachineMapper;

    @Autowired
    private MdmMaterialInfoMapper materialInfoMapper;

    @Autowired
    private CxStockMapper stockMapper;

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

    @Autowired
    private com.zlt.aps.mp.api.mapper.MdmDevicePlanShutMapper devicePlanShutMapper;

    @Autowired
    private com.zlt.aps.mp.api.mapper.MdmMonthPlanProductLhCapacityMapper monthPlanProductLhCapacityMapper;

    @Autowired
    private com.zlt.aps.cx.mapper.CxShiftConfigMapper shiftConfigMapper;

    @Autowired
    private MdmStructureLhRatioMapper structureLhRatioMapper;

    @Autowired
    private ScheduleDataValidator scheduleDataValidator;

    @Override
    public ScheduleResult executeSchedule(ScheduleRequest request) {
        ScheduleResult result = new ScheduleResult();
        result.setSuccess(false);
        result.setScheduleDate(request.getScheduleDate());

        try {
            log.info("开始执行排程，日期：{}，排程模式：{}",
                    request.getScheduleDate(), request.getScheduleMode());

            // 1. 构建排程上下文(对应流程图S5.1.6详细初始化)
            ScheduleContextDTO context = buildScheduleContext(request);
            if (context == null) {
                result.setMessage("构建排程上下文失败");
                return result;
            }

            // 2. 执行核心排程算法（包含续作、试制、新增任务的统一处理）
            // 任务优先级：续作 > 新增任务（试制在有空出产能时优先，但不挤掉实单）
            List<CxScheduleResult> scheduleResults = coreScheduleAlgorithmService.executeSchedule(context);

            // 3. 保存排程结果
            saveScheduleResults(scheduleResults);

            // 4. 验证排程结果
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

            // 1. 加载班次配置（按工厂）
            // 从班次配置表获取排程天数和班次顺序
            String factoryCode = request.getFactoryCode() != null ? request.getFactoryCode() : DEFAULT_FACTORY_CODE;
            context.setFactoryCode(factoryCode);

            List<com.zlt.aps.cx.entity.config.CxShiftConfig> allShiftConfigs = loadShiftConfigs(factoryCode);
            context.setShiftConfigList(allShiftConfigs);

            // 按排程天数分组
            Map<Integer, List<com.zlt.aps.cx.entity.config.CxShiftConfig>> dayShiftMap = allShiftConfigs.stream()
                    .filter(c -> c.getScheduleDay() != null)
                    .collect(Collectors.groupingBy(com.zlt.aps.cx.entity.config.CxShiftConfig::getScheduleDay));

            // 获取排程天数（根据班次配置计算，取最大的scheduleDay）
            int scheduleDays = dayShiftMap.isEmpty() ? DEFAULT_SCHEDULE_DAYS : dayShiftMap.keySet().stream()
                    .max(Integer::compareTo).orElse(DEFAULT_SCHEDULE_DAYS);
            context.setScheduleDays(scheduleDays);
            log.info("根据班次配置计算排程天数: {}", scheduleDays);

            // 计算排程日期范围
            LocalDate endDate = scheduleDate.plusDays(scheduleDays - 1);

            // 2. 获取设备计划停机信息（成型机台）
            // 查询排程日期范围内的停机计划，用于排程时扣减产能
            List<MdmDevicePlanShut> devicePlanShuts = devicePlanShutMapper.selectByMachineTypeAndDateRange(
                    MACHINE_TYPE_MOLDING, scheduleDate, endDate);
            context.setDevicePlanShuts(devicePlanShuts);
            log.info("加载成型机台停机计划 {} 条", devicePlanShuts.size());

            // 3. 获取所有机台（停机机台在排程时根据停机计划扣减产能）
            List<MdmMoldingMachine> machines = moldingMachineMapper.selectList(null);
            context.setAvailableMachines(machines);
            log.info("加载成型机台 {} 台", machines.size());

            // 4. 【主要任务来源】获取硫化排程结果
            // 从T_LH_SCHEDULE_RESULT获取今日硫化计划
            List<LhScheduleResult> lhScheduleResults = lhScheduleResultMapper.selectByDate(scheduleDate);
            context.setLhScheduleResults(lhScheduleResults);
            log.info("加载硫化排程结果 {} 条", lhScheduleResults.size());

            // 5. 根据硫化排程结果获取需要的物料信息
            // 从硫化排程结果中提取胎胚编码
            Set<String> embryoCodes = lhScheduleResults.stream()
                    .map(LhScheduleResult::getEmbryoCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // 根据胎胚编码查询物料信息
            List<MdmMaterialInfo> materials;
            if (!embryoCodes.isEmpty()) {
                materials = materialInfoMapper.selectList(
                        new LambdaQueryWrapper<MdmMaterialInfo>()
                                .in(MdmMaterialInfo::getMaterialCode, embryoCodes));
                log.info("根据硫化排程结果加载物料信息 {} 条", materials.size());
            } else {
                materials = new ArrayList<>();
                log.info("硫化排程结果为空，加载物料信息 0 条");
            }
            context.setMaterials(materials);

            // 6. 获取胎胚库存信息（只获取有库存的）
            List<CxStock> stocks = stockMapper.selectList(
                    new LambdaQueryWrapper<CxStock>()
                            .gt(CxStock::getStockNum, 0));
            context.setStocks(stocks);
            log.info("加载胎胚库存 {} 条", stocks.size());

            // 7. 【续作判断】获取成型在机信息
            // 从T_MDM_CX_MACHINE_ONLINE_INFO获取当前机台正在做的胎胚
            // 查询今天和昨天在机的信息（可能跨班次生产）
            List<MdmCxMachineOnlineInfo> onlineInfos = onlineInfoMapper.selectByDateRange(
                    scheduleDate, scheduleDate.minusDays(1));
            context.setOnlineInfos(onlineInfos);
            log.info("加载成型在机信息 {} 条", onlineInfos.size());

            // 8. 构建机台在机胎胚映射（快速查询用）
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

            // 9. 获取参数配置
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

            // 10. 获取结构整车配置
            List<CxStructureShiftCapacity> structureShiftCapacities = structureShiftCapacityMapper.selectList(null);
            context.setStructureShiftCapacities(structureShiftCapacities);

            // 11. 获取关键产品配置:开产首班次不安排关键产品
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

            // ========== 12. 构建产能映射（用于满算力计算，仅供收尾计算使用） ==========
            
            // 12.1 物料日硫化最大产能映射（基础表，人工维护的标准/MES/APS产能）
            Map<String, com.zlt.aps.mp.engine.domain.vo.MonthPlanProductLhCapacityVo> materialLhCapacityMap = buildMaterialLhCapacityMap(context);
            context.setMaterialLhCapacityMap(materialLhCapacityMap);
            log.info("构建物料日硫化最大产能映射 {} 条", materialLhCapacityMap.size());

            // 12.2 结构硫化配比映射（每个结构最大可用的硫化机台数）
            Map<String, MdmStructureLhRatio> structureLhRatioMap = buildStructureLhRatioMap();
            context.setStructureLhRatioMap(structureLhRatioMap);
            log.info("构建结构硫化配比映射 {} 条", structureLhRatioMap.size());

            // 13. 获取月度计划余量（用于收尾计算）
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

            // ========== 13.1 计算成型余量映射 ==========
            // 成型余量 = 硫化余量 - 该物料对应的所有胎胚库存
            // 需要通过物料信息表获取胎胚与物料的对应关系
            Map<String, Integer> formingRemainderMap = calculateFormingRemainderMap(
                    materials, monthSurplusMap, stocks);
            context.setFormingRemainderMap(formingRemainderMap);
            log.info("计算成型余量映射 {} 条", formingRemainderMap.size());

            // 14. 获取SKU排产分类（用于判断主销产品）
            List<MdmSkuScheduleCategory> skuCategories =
                    skuScheduleCategoryMapper.selectAllCategories();
            context.setSkuScheduleCategories(skuCategories);
            // 构建主销产品编码集合（SCHEDULE_TYPE='01'）
            Set<String> mainProductCodes = skuCategories.stream()
                    .filter(c -> "01".equals(c.getScheduleType()))
                    .map(MdmSkuScheduleCategory::getMaterialCode)
                    .collect(Collectors.toSet());
            context.setMainProductCodes(mainProductCodes);
            log.info("加载SKU排产分类 {} 条，其中主销产品 {} 个", skuCategories.size(), mainProductCodes.size());

            // 15. 设置节假日相关标记
            context.setIsOpeningDay(holidayScheduleService.isStartProductionDay(scheduleDate));
            context.setIsClosingDay(holidayScheduleService.isStopProductionDay(scheduleDate));
            context.setIsBeforeClosingDay(holidayScheduleService.isBeforeHoliday(scheduleDate));

            // 16. 设置排程参数
            context.setScheduleDate(scheduleDate);
            context.setScheduleMode(request.getScheduleMode());

            // 17. 数据完整性校验(对应流程图S5.1.7数据完整性校验)
            // 在返回之前进行数据完整性校验，提前发现问题
            ScheduleDataValidationResult validationResult = scheduleDataValidator.validate(
                    context, scheduleDate, factoryCode);

            // 如果存在阻断级错误（ERROR），终止排程
            if (!validationResult.isPassed()) {
                String errorMsg = "数据完整性校验不通过，无法进行排程：" + validationResult.generateSummary();
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            // 如果存在警告，记录但继续执行
            if (validationResult.getWarnCount() > 0) {
                log.warn("数据完整性校验存在警告，请检查日志：{}", validationResult.generateSummary());
            }

            return context;

        } catch (Exception e) {
            log.error("构建排程上下文失败", e);
            return null;
        }
    }

    /**
     * 保存排程结果
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveScheduleResults(List<CxScheduleResult> results) {
        if (CollectionUtils.isEmpty(results)) {
            return;
        }

        for (CxScheduleResult result : results) {
            result.setCreateTime(new Date());
            scheduleResultMapper.insert(result);

            // 保存明细
            if (!CollectionUtils.isEmpty(result.getDetails())) {
                for (CxScheduleDetail detail : result.getDetails()) {
                    detail.setResultId(result.getId());
                    detail.setCreateTime(new Date());
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
     * 计算物料的日硫化产能（满算力）
     *
     * 计算逻辑：
     * 1. 配比塞满时：使用当前硫化机台的实际产能
     * 2. 配比未塞满时：对于未塞满的配比，使用当前机台的最小日硫化量来预测
     *
     * 满算力 = Σ(各硫化机台日硫化量)
     * - 已塞满的硫化机：使用实际日硫化量
     * - 未塞满的硫化机：使用当前机台的最小日硫化量预测
     *
     * @param materialCode 物料编码
     * @param structureName 结构名称（用于查找配比）
     * @param materialLhCapacityMap 物料日硫化产能映射
     * @param structureLhRatioMap 结构硫化配比映射
     * @param lhMachineCapacityMap 硫化机台产能映射
     * @return 日硫化产能（条/天）
     */
    private int calculateMaterialDailyLhCapacity(
            String materialCode,
            String structureName,
            Map<String, com.zlt.aps.mp.engine.domain.vo.MonthPlanProductLhCapacityVo> materialLhCapacityMap,
            Map<String, com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio> structureLhRatioMap,
            Map<String, List<LhMachineCapacityInfo>> lhMachineCapacityMap) {

        // 1. 获取该物料对应的硫化机台列表
        List<LhMachineCapacityInfo> machineList = lhMachineCapacityMap.get(materialCode);
        if (machineList == null || machineList.isEmpty()) {
            log.debug("未找到物料 {} 的硫化机台信息，尝试使用基础产能", materialCode);
            // 兜底：使用基础产能
            com.zlt.aps.mp.engine.domain.vo.MonthPlanProductLhCapacityVo capacityVo = materialLhCapacityMap.get(materialCode);
            if (capacityVo != null) {
                int baseCapacity = capacityVo.getDefaultDayVulcanizationQty();
                log.debug("物料 {} 使用基础产能 {}", materialCode, baseCapacity);
                return baseCapacity;
            }
            return 0;
        }

        // 2. 获取该结构的硫化配比（最大硫化机台数）
        com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio ratio = structureLhRatioMap.get(structureName);
        int maxLhMachineQty = (ratio != null && ratio.getLhMachineMaxQty() != null)
                ? ratio.getLhMachineMaxQty() : machineList.size();

        // 3. 获取当前硫化机台的数量
        int currentMachineCount = machineList.size();

        // 4. 获取当前硫化机台的最小日硫化量（用于预测未塞满的配比）
        int minCapacity = Integer.MAX_VALUE;
        int totalCurrentCapacity = 0;
        for (LhMachineCapacityInfo machine : machineList) {
            if (machine.getDailyCapacity() != null && machine.getDailyCapacity() > 0) {
                minCapacity = Math.min(minCapacity, machine.getDailyCapacity());
                totalCurrentCapacity += machine.getDailyCapacity();
            }
        }

        // 如果没有找到有效产能，尝试使用基础产能
        if (minCapacity == Integer.MAX_VALUE) {
            com.zlt.aps.mp.engine.domain.vo.MonthPlanProductLhCapacityVo capacityVo = materialLhCapacityMap.get(materialCode);
            if (capacityVo != null) {
                int baseCapacity = capacityVo.getDefaultDayVulcanizationQty();
                log.debug("物料 {} 硫化机台产能为空，使用基础产能 {}", materialCode, baseCapacity);
                return baseCapacity;
            }
            return 0;
        }

        // 5. 计算满算力
        int fullCapacity;
        if (currentMachineCount >= maxLhMachineQty) {
            // 配比已塞满，使用当前硫化机台的总产能
            fullCapacity = totalCurrentCapacity;
            log.debug("物料 {} 配比已塞满，硫化机台数量 {} >= 最大配比 {}，总产能 {}",
                    materialCode, currentMachineCount, maxLhMachineQty, fullCapacity);
        } else {
            // 配比未塞满，需要预测未塞满的配比
            // 未塞满的配比数量
            int unfilledRatioCount = maxLhMachineQty - currentMachineCount;

            // 对于未塞满的配比，使用当前机台的最小日硫化量来预测
            // 满算力 = 当前硫化机台总产能 + 未塞满配比数量 × 最小硫化量
            int predictedUnfilledCapacity = unfilledRatioCount * minCapacity;
            fullCapacity = totalCurrentCapacity + predictedUnfilledCapacity;

            log.info("物料 {} 配比未塞满，当前硫化机台 {} 台，最大配比 {}，未塞满 {} 个配比",
                    materialCode, currentMachineCount, maxLhMachineQty, unfilledRatioCount);
            log.info("物料 {} 满算力计算：当前产能 {} + 预测产能({} × {}) = {}",
                    materialCode, totalCurrentCapacity, unfilledRatioCount, minCapacity, fullCapacity);
        }

        return fullCapacity;
    }

    /**
     * 找到该物料的最近一个收尾日
     * 
     * 逻辑说明：
     * 从当前日期开始往后找，找到第一个连续排产区间的最后一天。
     * 如果中间遇到停产，停产结束后可能还会继续生产这个胎胚，
     * 所以应该找最近的一个收尾日，而不是整个月最后一个有排产的日期。
     * 
     * 例如：
     * - 当前日期：10号
     * - 月计划排产：12-15号、20-25号（停产）、28-30号
     * - 返回：15号（最近一个收尾日）
     * 
     * @param plans 月计划列表
     * @param currentDay 当前日期（几号）
     * @param lastDayOfMonth 月末日期
     * @return 最近一个收尾日
     */
    private int findMaterialEndingDay(List<FactoryMonthPlanProductionFinalResult> plans, int currentDay, int lastDayOfMonth) {
        // 收集所有有排产的日期
        Set<Integer> productionDays = new HashSet<>();
        for (FactoryMonthPlanProductionFinalResult plan : plans) {
            for (int day = currentDay; day <= lastDayOfMonth; day++) {
                Integer dayQty = plan.getDayQty(day);
                if (dayQty != null && dayQty > 0) {
                    productionDays.add(day);
                }
            }
        }
        
        if (productionDays.isEmpty()) {
            return lastDayOfMonth;
        }
        
        // 从当前日期开始，找到第一个连续排产区间的最后一天
        int endingDay = currentDay;
        for (int day = currentDay; day <= lastDayOfMonth; day++) {
            if (productionDays.contains(day)) {
                // 有排产，更新收尾日
                endingDay = day;
            } else if (endingDay > currentDay) {
                // 已经进入排产区间，但今天没有排产，说明连续区间结束了
                // 这就是最近一个收尾日
                break;
            }
            // 如果还没进入排产区间（endingDay == currentDay 且当天没排产），继续往后找
        }
        
        return endingDay;
    }

    /**
     * 计算未来3天的计划产量（物料维度）
     */
    private int calculateNext3DaysMaterialPlanQty(List<FactoryMonthPlanProductionFinalResult> plans, int currentDay) {
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

    /**
     * 加载班次配置（按工厂）
     *
     * @param factoryCode 工厂编号
     * @return 排序后的班次配置列表
     */
    private List<com.zlt.aps.cx.entity.config.CxShiftConfig> loadShiftConfigs(String factoryCode) {
        return shiftConfigMapper.selectList(
                new LambdaQueryWrapper<com.zlt.aps.cx.entity.config.CxShiftConfig>()
                        .eq(com.zlt.aps.cx.entity.config.CxShiftConfig::getFactoryCode, factoryCode)
                        .eq(com.zlt.aps.cx.entity.config.CxShiftConfig::getIsActive, 1)
                        .orderByAsc(com.zlt.aps.cx.entity.config.CxShiftConfig::getScheduleDay)
                        .orderByAsc(com.zlt.aps.cx.entity.config.CxShiftConfig::getDayShiftOrder)
        );
    }

    /**
     * 构建物料日硫化产能映射
     *
     * 从月计划数据中提取物料的日硫化产能信息，构建物料编码到产能信息的映射
     * 用于计算成型机台的满算力
     *
     * @return 物料日硫化产能映射
     */
    /**
     * 构建硫化机台日产能信息映射（用于计算满算力）
     *
     * @return 物料编码 -> 硫化机台产能信息列表
     */
    /**
     * 计算成型余量映射
     * 
     * 成型余量 = 硫化余量 - 该物料对应的所有胎胚库存
     * 
     * 计算步骤：
     * 1. 从物料信息表构建胎胚→物料的映射
     * 2. 将胎胚库存按物料汇总
     * 3. 计算成型余量
     * 
     * @param materials 物料信息列表
     * @param monthSurplusMap 月度计划余量映射（物料编码 -> 余量信息）
     * @param stocks 胎胚库存列表
     * @return 成型余量映射（物料编码 -> 成型余量）
     */
    private Map<String, Integer> calculateFormingRemainderMap(
            List<MdmMaterialInfo> materials,
            Map<String, MdmMonthSurplus> monthSurplusMap,
            List<CxStock> stocks) {
        
        Map<String, Integer> resultMap = new HashMap<>();
        
        try {
            // Step 1: 构建胎胚→物料的映射
            Map<String, String> embryoToMaterialMap = new HashMap<>();
            for (MdmMaterialInfo material : materials) {
                String embryoCode = material.getEmbryoCode();
                String materialCode = material.getMaterialCode();
                if (embryoCode != null && materialCode != null) {
                    embryoToMaterialMap.put(embryoCode, materialCode);
                }
            }
            log.debug("构建胎胚→物料映射 {} 条", embryoToMaterialMap.size());
            
            // Step 2: 将胎胚库存按物料汇总
            // Key: 物料编码, Value: 该物料对应的所有胎胚库存总和
            Map<String, Integer> materialStockMap = new HashMap<>();
            for (CxStock stock : stocks) {
                String embryoCode = stock.getEmbryoCode();
                if (embryoCode == null) {
                    continue;
                }
                // 找到胎胚对应的物料编码
                String materialCode = embryoToMaterialMap.get(embryoCode);
                if (materialCode == null) {
                    // 如果找不到对应关系，假设胎胚编码就是物料编码（兼容处理）
                    materialCode = embryoCode;
                }
                // 累加该物料的有效库存
                int effectiveStock = stock.getEffectiveStock();
                materialStockMap.merge(materialCode, effectiveStock, Integer::sum);
            }
            log.debug("按物料汇总胎胚库存 {} 条", materialStockMap.size());
            
            // Step 3: 计算成型余量
            // 遍历所有有硫化余量的物料
            for (Map.Entry<String, MdmMonthSurplus> entry : monthSurplusMap.entrySet()) {
                String materialCode = entry.getKey();
                MdmMonthSurplus surplus = entry.getValue();
                
                // 硫化余量
                int vulcanizingRemainder = surplus.getPlanSurplusQty() != null 
                        ? surplus.getPlanSurplusQty().intValue() : 0;
                
                // 该物料对应的胎胚库存
                int embryoStock = materialStockMap.getOrDefault(materialCode, 0);
                
                // 成型余量 = 硫化余量 - 胎胚库存
                int formingRemainder = Math.max(0, vulcanizingRemainder - embryoStock);
                
                resultMap.put(materialCode, formingRemainder);
                
                if (formingRemainder > 0) {
                    log.debug("物料 {} 成型余量计算: 硫化余量 {} - 胎胚库存 {} = {}",
                            materialCode, vulcanizingRemainder, embryoStock, formingRemainder);
                }
            }
            
            log.info("计算成型余量映射完成，共 {} 条", resultMap.size());
            
        } catch (Exception e) {
            log.error("计算成型余量映射失败", e);
        }
        
        return resultMap;
    }

    private Map<String, List<LhMachineCapacityInfo>> buildLhMachineCapacityMap() {
        Map<String, List<LhMachineCapacityInfo>> resultMap = new HashMap<>();

        try {
            LocalDate today = LocalDate.now();
            List<LhScheduleResult> lhResults = lhScheduleResultMapper.selectByDate(today);

            for (LhScheduleResult lhResult : lhResults) {
                String embryoCode = lhResult.getEmbryoCode();
                if (embryoCode == null) {
                    continue;
                }

                LhMachineCapacityInfo info = new LhMachineCapacityInfo();
                info.setLhMachineCode(lhResult.getLhMachineCode());
                info.setMaterialCode(embryoCode);
                info.setDailyCapacity(lhResult.getDayVulcanizationQty());

                resultMap.computeIfAbsent(embryoCode, k -> new ArrayList<>()).add(info);
            }

            log.info("从硫化排程结果构建硫化机台产能映射，共 {} 个物料", resultMap.size());

        } catch (Exception e) {
            log.error("构建硫化机台产能映射失败", e);
        }

        return resultMap;
    }

    private Map<String, com.zlt.aps.mp.engine.domain.vo.MonthPlanProductLhCapacityVo> buildMaterialLhCapacityMap(ScheduleContextDTO context) {
        Map<String, com.zlt.aps.mp.engine.domain.vo.MonthPlanProductLhCapacityVo> resultMap = new HashMap<>();

        try {
            // 从参数配置中获取日硫化量计算模式
            DayVulcanizationModeEnum mode = DayVulcanizationModeEnum.STANDARD_CAPACITY;
            Map<String, CxParamConfig> paramConfigMap = context.getParamConfigMap();
            if (paramConfigMap != null) {
                CxParamConfig modeConfig = paramConfigMap.get(PARAM_CODE_DAY_VULCANIZATION_MODE);
                if (modeConfig != null && modeConfig.getParamValue() != null) {
                    mode = DayVulcanizationModeEnum.getByCode(modeConfig.getParamValue());
                }
            }
            log.info("日硫化量计算模式: {}", mode.getDesc());

            // 从基础表查询物料日硫化产能（按工厂+物料维度）
            String factoryCode = context.getFactoryCode();
            java.util.List<com.zlt.aps.mp.engine.domain.vo.MonthPlanProductLhCapacityVo> baseCapacities =
                    monthPlanProductLhCapacityMapper.selectByFactoryCode(factoryCode);

            // 根据模式计算日硫化量并放入映射
            for (com.zlt.aps.mp.engine.domain.vo.MonthPlanProductLhCapacityVo vo : baseCapacities) {
                String materialCode = vo.getMaterialCode();
                if (materialCode == null) {
                    continue;
                }
                // 根据计算模式设置日硫化量
                vo.calculateDayVulcanizationQty(mode);
                resultMap.put(materialCode, vo);
            }

            log.info("从基础表构建物料日硫化产能映射（工厂:{}），共 {} 个物料", factoryCode, resultMap.size());

        } catch (Exception e) {
            log.error("构建物料日硫化产能映射失败", e);
        }

        return resultMap;
    }

    /**
     * 构建结构硫化配比映射
     *
     * 从T_MDM_STRUCTURE_LH_RATIO表中获取结构的最大硫化机台数
     * 用于计算成型机台的满算力
     *
     * @return 结构硫化配比映射
     */
    private Map<String, com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio> buildStructureLhRatioMap() {
        Map<String, com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio> resultMap = new HashMap<>();

        try {
            List<com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio> ratios =
                    structureLhRatioMapper.selectList(null);

            for (com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio ratio : ratios) {
                String structureName = ratio.getStructureName();
                if (structureName != null) {
                    resultMap.put(structureName, ratio);
                }
            }

            log.info("从结构硫化配比表构建映射，共 {} 个结构", resultMap.size());

        } catch (Exception e) {
            log.error("构建结构硫化配比映射失败", e);
        }

        return resultMap;
    }
}
