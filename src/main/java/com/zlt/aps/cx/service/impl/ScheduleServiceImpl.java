package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.dto.ScheduleRequest;
import com.zlt.aps.cx.entity.CxStock;
import com.zlt.aps.cx.entity.config.CxKeyProduct;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.config.CxStructureShiftCapacity;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.enums.DayVulcanizationModeEnum;
import com.zlt.aps.cx.mapper.CxKeyProductMapper;
import com.zlt.aps.cx.mapper.CxParamConfigMapper;
import com.zlt.aps.cx.mapper.CxScheduleDetailMapper;
import com.zlt.aps.cx.mapper.CxScheduleResultMapper;
import com.zlt.aps.cx.mapper.CxShiftConfigMapper;
import com.zlt.aps.cx.mapper.CxStockMapper;
import com.zlt.aps.cx.mapper.CxStructureShiftCapacityMapper;
import com.zlt.aps.cx.mapper.FactoryMonthPlanProductionFinalResultMapper;
import com.zlt.aps.cx.mapper.LhScheduleResultMapper;
import com.zlt.aps.cx.mapper.MdmCxMachineOnlineInfoMapper;
import com.zlt.aps.cx.mapper.MdmMaterialInfoMapper;
import com.zlt.aps.cx.mapper.MdmMoldingMachineMapper;
import com.zlt.aps.cx.mapper.MdmMonthSurplusMapper;
import com.zlt.aps.cx.mapper.MdmSkuScheduleCategoryMapper;
import com.zlt.aps.cx.mapper.MdmStructureLhRatioMapper;
import com.zlt.aps.cx.service.ConstraintCheckService;
import com.zlt.aps.cx.service.CoreScheduleAlgorithmService;
import com.zlt.aps.cx.service.HolidayScheduleService;
import com.zlt.aps.cx.service.ScheduleService;
import com.zlt.aps.cx.service.impl.ScheduleDataValidator;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MdmCxMachineOnlineInfo;
import com.zlt.aps.mp.api.domain.entity.MdmDevicePlanShut;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.mp.api.domain.entity.MdmSkuScheduleCategory;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.mp.api.mapper.MdmDevicePlanShutMapper;
import com.zlt.aps.mp.api.mapper.MdmMonthPlanProductLhCapacityMapper;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductLhCapacityVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * <p>整合所有核心服务，实现完整的排程流程
 * 
 * <p>主要功能：
 * <ul>
 *   <li>排程上下文初始化</li>
 *   <li>核心排程算法调用</li>
 *   <li>排程结果保存与验证</li>
 *   <li>物料收尾计算</li>
 * </ul>
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    // ==================== 常量定义 ====================

    /** 默认工厂编号 */
    private static final String DEFAULT_FACTORY_CODE = "DEFAULT";

    /** 默认排程天数 */
    private static final int DEFAULT_SCHEDULE_DAYS = 3;

    /** 机台类型：成型 */
    private static final String MACHINE_TYPE_MOLDING = "成型";

    /** 参数编码：日硫化量计算模式 */
    private static final String PARAM_CODE_DAY_VULCANIZATION_MODE = "DAY_VULCANIZATION_MODE";

    /** 参数编码：损耗率 */
    private static final String PARAM_CODE_LOSS_RATE = "LOSS_RATE";

    /** 默认损耗率 */
    private static final BigDecimal DEFAULT_LOSS_RATE = new BigDecimal("0.02");

    /** 主销产品类型编码 */
    private static final String MAIN_PRODUCT_SCHEDULE_TYPE = "01";

    /** 启用状态 */
    private static final Integer ACTIVE_STATUS = 1;

    /** 收尾预警天数：紧急 */
    private static final int URGENT_ENDING_DAYS = 3;

    /** 收尾预警天数：临近 */
    private static final int NEAR_ENDING_DAYS = 10;

    /** 追赶计划天数 */
    private static final int CATCH_UP_DAYS = 3;

    // ==================== 依赖注入 ====================

    private final CoreScheduleAlgorithmService coreScheduleAlgorithmService;
    private final ConstraintCheckService constraintCheckService;
    private final HolidayScheduleService holidayScheduleService;
    private final ScheduleDataValidator scheduleDataValidator;

    private final MdmMoldingMachineMapper moldingMachineMapper;
    private final MdmMaterialInfoMapper materialInfoMapper;
    private final MdmMonthSurplusMapper monthSurplusMapper;
    private final MdmSkuScheduleCategoryMapper skuScheduleCategoryMapper;
    private final MdmStructureLhRatioMapper structureLhRatioMapper;
    private final MdmDevicePlanShutMapper devicePlanShutMapper;
    private final MdmMonthPlanProductLhCapacityMapper monthPlanProductLhCapacityMapper;

    private final CxStockMapper stockMapper;
    private final CxScheduleResultMapper scheduleResultMapper;
    private final CxScheduleDetailMapper scheduleDetailMapper;
    private final CxParamConfigMapper paramConfigMapper;
    private final CxStructureShiftCapacityMapper structureShiftCapacityMapper;
    private final CxKeyProductMapper keyProductMapper;
    private final LhScheduleResultMapper lhScheduleResultMapper;
    private final MdmCxMachineOnlineInfoMapper onlineInfoMapper;
    private final CxShiftConfigMapper shiftConfigMapper;
    private final FactoryMonthPlanProductionFinalResultMapper monthPlanMapper;

    // ==================== 公共方法 ====================

    @Override
    public ScheduleResult executeSchedule(ScheduleRequest request) {
        ScheduleResult result = new ScheduleResult();
        result.setSuccess(false);
        result.setScheduleDate(request.getScheduleDate());

        try {
            log.info("开始执行排程，日期：{}，排程模式：{}", request.getScheduleDate(), request.getScheduleMode());

            // 1. 构建排程上下文
            ScheduleContextDTO context = buildScheduleContext(request);
            if (context == null) {
                result.setMessage("构建排程上下文失败");
                return result;
            }

            // 2. 执行核心排程算法
            List<CxScheduleResult> scheduleResults = coreScheduleAlgorithmService.executeSchedule(context);

            // 3. 保存排程结果
            saveScheduleResults(scheduleResults);

            // 4. 验证排程结果
            boolean validated = validateScheduleResults(scheduleResults);

            result.setSuccess(validated);
            result.setMessage(validated ? "排程成功" : "排程完成，但存在约束冲突");
            result.setResults(scheduleResults);

            log.info("排程执行完成，日期：{}，结果数量：{}", request.getScheduleDate(), scheduleResults.size());

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

            // 1. 加载班次配置
            String factoryCode = request.getFactoryCode() != null ? request.getFactoryCode() : DEFAULT_FACTORY_CODE;
            context.setFactoryCode(factoryCode);
            loadShiftConfigs(context, factoryCode);

            // 2. 获取设备计划停机信息
            loadDevicePlanShuts(context, scheduleDate);

            // 3. 获取所有机台
            loadMoldingMachines(context);

            // 4. 获取硫化排程结果
            loadLhScheduleResults(context, scheduleDate);

            // 5. 根据硫化排程结果获取物料信息
            loadMaterials(context);

            // 6. 获取胎胚库存信息
            loadStocks(context);

            // 7. 获取成型在机信息
            loadOnlineInfos(context, scheduleDate);

            // 8. 构建机台在机胎胚映射
            buildMachineOnlineEmbryoMap(context);

            // 9. 获取参数配置
            loadParamConfigs(context);

            // 10. 获取结构整车配置
            loadStructureShiftCapacities(context);

            // 11. 获取关键产品配置
            loadKeyProducts(context);

            // 12. 构建产能映射
            buildCapacityMaps(context);

            // 13. 获取月度计划余量并计算成型余量
            loadMonthSurplusAndCalculateFormingRemainder(context, scheduleDate);

            // 14. 获取SKU排产分类
            loadSkuCategories(context);

            // 15. 设置节假日相关标记
            setHolidayFlags(context, scheduleDate);

            // 16. 设置排程参数
            context.setScheduleDate(scheduleDate);
            context.setScheduleMode(request.getScheduleMode());

            // 17. 数据完整性校验
            validateScheduleData(context, scheduleDate, factoryCode);

            return context;

        } catch (Exception e) {
            log.error("构建排程上下文失败", e);
            return null;
        }
    }

    // ==================== 私有方法：初始化相关 ====================

    /**
     * 加载班次配置
     */
    private void loadShiftConfigs(ScheduleContextDTO context, String factoryCode) {
        List<CxShiftConfig> allShiftConfigs = shiftConfigMapper.selectList(
                new LambdaQueryWrapper<CxShiftConfig>()
                        .eq(CxShiftConfig::getFactoryCode, factoryCode)
                        .eq(CxShiftConfig::getIsActive, ACTIVE_STATUS)
                        .orderByAsc(CxShiftConfig::getScheduleDay)
                        .orderByAsc(CxShiftConfig::getDayShiftOrder)
        );
        context.setShiftConfigList(allShiftConfigs);

        // 按排程天数分组
        Map<Integer, List<CxShiftConfig>> dayShiftMap = allShiftConfigs.stream()
                .filter(c -> c.getScheduleDay() != null)
                .collect(Collectors.groupingBy(CxShiftConfig::getScheduleDay));

        int scheduleDays = dayShiftMap.isEmpty() ? DEFAULT_SCHEDULE_DAYS 
                : dayShiftMap.keySet().stream().max(Integer::compareTo).orElse(DEFAULT_SCHEDULE_DAYS);
        context.setScheduleDays(scheduleDays);
        log.info("根据班次配置计算排程天数: {}", scheduleDays);
    }

    /**
     * 加载设备计划停机信息
     */
    private void loadDevicePlanShuts(ScheduleContextDTO context, LocalDate scheduleDate) {
        int scheduleDays = context.getScheduleDays();
        LocalDate endDate = scheduleDate.plusDays(scheduleDays - 1);

        List<MdmDevicePlanShut> devicePlanShuts = devicePlanShutMapper.selectByMachineTypeAndDateRange(
                MACHINE_TYPE_MOLDING, scheduleDate, endDate);
        context.setDevicePlanShuts(devicePlanShuts);
        log.info("加载成型机台停机计划 {} 条", devicePlanShuts.size());
    }

    /**
     * 加载成型机台
     */
    private void loadMoldingMachines(ScheduleContextDTO context) {
        List<MdmMoldingMachine> machines = moldingMachineMapper.selectList(null);
        context.setAvailableMachines(machines);
        log.info("加载成型机台 {} 台", machines.size());
    }

    /**
     * 加载硫化排程结果
     */
    private void loadLhScheduleResults(ScheduleContextDTO context, LocalDate scheduleDate) {
        List<LhScheduleResult> lhScheduleResults = lhScheduleResultMapper.selectByDate(scheduleDate);
        context.setLhScheduleResults(lhScheduleResults);
        log.info("加载硫化排程结果 {} 条", lhScheduleResults.size());
    }

    /**
     * 加载物料信息
     */
    private void loadMaterials(ScheduleContextDTO context) {
        List<LhScheduleResult> lhScheduleResults = context.getLhScheduleResults();

        Set<String> embryoCodes = lhScheduleResults.stream()
                .map(LhScheduleResult::getEmbryoCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

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
    }

    /**
     * 加载胎胚库存
     */
    private void loadStocks(ScheduleContextDTO context) {
        List<CxStock> stocks = stockMapper.selectList(
                new LambdaQueryWrapper<CxStock>()
                        .gt(CxStock::getStockNum, 0));
        context.setStocks(stocks);
        log.info("加载胎胚库存 {} 条", stocks.size());
    }

    /**
     * 加载成型在机信息
     */
    private void loadOnlineInfos(ScheduleContextDTO context, LocalDate scheduleDate) {
        List<MdmCxMachineOnlineInfo> onlineInfos = onlineInfoMapper.selectByDateRange(
                scheduleDate, scheduleDate.minusDays(1));
        context.setOnlineInfos(onlineInfos);
        log.info("加载成型在机信息 {} 条", onlineInfos.size());
    }

    /**
     * 构建机台在机胎胚映射
     */
    private void buildMachineOnlineEmbryoMap(ScheduleContextDTO context) {
        Map<String, Set<String>> machineOnlineEmbryoMap = new HashMap<>();
        for (MdmCxMachineOnlineInfo onlineInfo : context.getOnlineInfos()) {
            String cxCode = onlineInfo.getCxCode();
            String embryoCode = onlineInfo.getMesMaterialCode();
            if (cxCode != null && embryoCode != null) {
                machineOnlineEmbryoMap.computeIfAbsent(cxCode, k -> new HashSet<>()).add(embryoCode);
            }
        }
        context.setMachineOnlineEmbryoMap(machineOnlineEmbryoMap);
        log.info("构建机台在机胎胚映射，共 {} 个机台有在机任务", machineOnlineEmbryoMap.size());
    }

    /**
     * 加载参数配置
     */
    private void loadParamConfigs(ScheduleContextDTO context) {
        List<CxParamConfig> paramConfigs = paramConfigMapper.selectList(null);
        Map<String, CxParamConfig> paramConfigMap = paramConfigs.stream()
                .collect(Collectors.toMap(CxParamConfig::getParamCode, p -> p, (a, b) -> a));
        context.setParamConfigMap(paramConfigMap);

        // 加载损耗率
        CxParamConfig lossRateConfig = paramConfigMap.get(PARAM_CODE_LOSS_RATE);
        BigDecimal lossRate = lossRateConfig != null
                ? new BigDecimal(lossRateConfig.getParamValue())
                : DEFAULT_LOSS_RATE;
        context.setLossRate(lossRate);
    }

    /**
     * 加载结构整车配置
     */
    private void loadStructureShiftCapacities(ScheduleContextDTO context) {
        List<CxStructureShiftCapacity> structureShiftCapacities = structureShiftCapacityMapper.selectList(null);
        context.setStructureShiftCapacities(structureShiftCapacities);
    }

    /**
     * 加载关键产品配置
     */
    private void loadKeyProducts(ScheduleContextDTO context) {
        List<CxKeyProduct> keyProducts = keyProductMapper.selectList(
                new LambdaQueryWrapper<CxKeyProduct>()
                        .eq(CxKeyProduct::getIsActive, ACTIVE_STATUS));
        context.setKeyProducts(keyProducts);

        Set<String> keyProductCodes = new HashSet<>();
        for (CxKeyProduct product : keyProducts) {
            keyProductCodes.add(product.getEmbryoCode());
        }
        context.setKeyProductCodes(keyProductCodes);
    }

    /**
     * 构建产能映射
     */
    private void buildCapacityMaps(ScheduleContextDTO context) {
        // 物料日硫化最大产能映射
        Map<String, MonthPlanProductLhCapacityVo> materialLhCapacityMap = buildMaterialLhCapacityMap(context);
        context.setMaterialLhCapacityMap(materialLhCapacityMap);
        log.info("构建物料日硫化最大产能映射 {} 条", materialLhCapacityMap.size());

        // 结构硫化配比映射
        Map<String, MdmStructureLhRatio> structureLhRatioMap = buildStructureLhRatioMap();
        context.setStructureLhRatioMap(structureLhRatioMap);
        log.info("构建结构硫化配比映射 {} 条", structureLhRatioMap.size());
    }

    /**
     * 加载月度计划余量并计算成型余量
     */
    private void loadMonthSurplusAndCalculateFormingRemainder(ScheduleContextDTO context, LocalDate scheduleDate) {
        int year = scheduleDate.getYear();
        int month = scheduleDate.getMonthValue();

        List<MdmMonthSurplus> monthSurplusList = monthSurplusMapper.selectByYearMonth(year, month);
        context.setMonthSurplusList(monthSurplusList);

        Map<String, MdmMonthSurplus> monthSurplusMap = monthSurplusList.stream()
                .collect(Collectors.toMap(MdmMonthSurplus::getMaterialCode, s -> s, (a, b) -> a));
        context.setMonthSurplusMap(monthSurplusMap);
        log.info("加载月度计划余量 {} 条", monthSurplusList.size());

        // 计算成型余量映射
        Map<String, Integer> formingRemainderMap = calculateFormingRemainderMap(
                context.getMaterials(), monthSurplusMap, context.getStocks());
        context.setFormingRemainderMap(formingRemainderMap);
        log.info("计算成型余量映射 {} 条", formingRemainderMap.size());
    }

    /**
     * 加载SKU排产分类
     */
    private void loadSkuCategories(ScheduleContextDTO context) {
        List<MdmSkuScheduleCategory> skuCategories = skuScheduleCategoryMapper.selectAllCategories();
        context.setSkuScheduleCategories(skuCategories);

        Set<String> mainProductCodes = skuCategories.stream()
                .filter(c -> MAIN_PRODUCT_SCHEDULE_TYPE.equals(c.getScheduleType()))
                .map(MdmSkuScheduleCategory::getMaterialCode)
                .collect(Collectors.toSet());
        context.setMainProductCodes(mainProductCodes);
        log.info("加载SKU排产分类 {} 条，其中主销产品 {} 个", skuCategories.size(), mainProductCodes.size());
    }

    /**
     * 设置节假日相关标记
     */
    private void setHolidayFlags(ScheduleContextDTO context, LocalDate scheduleDate) {
        context.setIsOpeningDay(holidayScheduleService.isStartProductionDay(scheduleDate));
        context.setIsClosingDay(holidayScheduleService.isStopProductionDay(scheduleDate));
        context.setIsBeforeClosingDay(holidayScheduleService.isBeforeHoliday(scheduleDate));
    }

    /**
     * 数据完整性校验
     */
    private void validateScheduleData(ScheduleContextDTO context, LocalDate scheduleDate, String factoryCode) {
        ScheduleDataValidationResult validationResult = scheduleDataValidator.validate(context, scheduleDate, factoryCode);

        if (!validationResult.isPassed()) {
            String errorMsg = "数据完整性校验不通过，无法进行排程：" + validationResult.generateSummary();
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        if (validationResult.getWarnCount() > 0) {
            log.warn("数据完整性校验存在警告，请检查日志：{}", validationResult.generateSummary());
        }
    }

    // ==================== 私有方法：排程结果相关 ====================

    /**
     * 保存排程结果
     *
     * @param results 排程结果列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveScheduleResults(List<CxScheduleResult> results) {
        if (CollectionUtils.isEmpty(results)) {
            return;
        }

        for (CxScheduleResult result : results) {
            result.setCreateTime(new Date());
            scheduleResultMapper.insert(result);

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
     *
     * @param results 排程结果列表
     * @return 是否全部通过验证
     */
    private boolean validateScheduleResults(List<CxScheduleResult> results) {
        if (CollectionUtils.isEmpty(results)) {
            return false;
        }

        int validCount = 0;
        for (CxScheduleResult result : results) {
            ConstraintCheckService.ConstraintCheckResult checkResult = constraintCheckService.checkAllConstraints(result);
            if (checkResult.isPassed()) {
                validCount++;
            } else {
                log.warn("排程结果存在约束冲突，机台：{}，物料：{}，冲突：{}",
                        result.getCxMachineCode(), result.getEmbryoCode(), checkResult.getViolations());
            }
        }

        return validCount == results.size();
    }

    // ==================== 私有方法：产能映射构建 ====================

    /**
     * 构建物料日硫化产能映射
     *
     * @param context 排程上下文
     * @return 物料日硫化产能映射
     */
    private Map<String, MonthPlanProductLhCapacityVo> buildMaterialLhCapacityMap(ScheduleContextDTO context) {
        Map<String, MonthPlanProductLhCapacityVo> resultMap = new HashMap<>();

        try {
            DayVulcanizationModeEnum mode = getDayVulcanizationMode(context);
            log.info("日硫化量计算模式: {}", mode.getDesc());

            String factoryCode = context.getFactoryCode();
            List<MonthPlanProductLhCapacityVo> baseCapacities = monthPlanProductLhCapacityMapper.selectByFactoryCode(factoryCode);

            for (MonthPlanProductLhCapacityVo vo : baseCapacities) {
                String materialCode = vo.getMaterialCode();
                if (materialCode == null) {
                    continue;
                }
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
     * 获取日硫化量计算模式
     */
    private DayVulcanizationModeEnum getDayVulcanizationMode(ScheduleContextDTO context) {
        Map<String, CxParamConfig> paramConfigMap = context.getParamConfigMap();
        if (paramConfigMap == null) {
            return DayVulcanizationModeEnum.STANDARD_CAPACITY;
        }

        CxParamConfig modeConfig = paramConfigMap.get(PARAM_CODE_DAY_VULCANIZATION_MODE);
        if (modeConfig != null && modeConfig.getParamValue() != null) {
            return DayVulcanizationModeEnum.getByCode(modeConfig.getParamValue());
        }

        return DayVulcanizationModeEnum.STANDARD_CAPACITY;
    }

    /**
     * 构建结构硫化配比映射
     *
     * @return 结构硫化配比映射
     */
    private Map<String, MdmStructureLhRatio> buildStructureLhRatioMap() {
        Map<String, MdmStructureLhRatio> resultMap = new HashMap<>();

        try {
            List<MdmStructureLhRatio> ratios = structureLhRatioMapper.selectList(null);
            for (MdmStructureLhRatio ratio : ratios) {
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

    // ==================== 私有方法：成型余量计算 ====================

    /**
     * 计算成型余量映射
     *
     * <p>成型余量 = 硫化余量 - 该物料对应的所有胎胚库存
     *
     * @param materials       物料信息列表
     * @param monthSurplusMap 月度计划余量映射
     * @param stocks          胎胚库存列表
     * @return 成型余量映射
     */
    private Map<String, Integer> calculateFormingRemainderMap(
            List<MdmMaterialInfo> materials,
            Map<String, MdmMonthSurplus> monthSurplusMap,
            List<CxStock> stocks) {

        Map<String, Integer> resultMap = new HashMap<>();

        try {
            // Step 1: 构建胎胚→物料的映射
            Map<String, String> embryoToMaterialMap = buildEmbryoToMaterialMap(materials);
            log.debug("构建胎胚→物料映射 {} 条", embryoToMaterialMap.size());

            // Step 2: 将胎胚库存按物料汇总
            Map<String, Integer> materialStockMap = aggregateStockByMaterial(stocks, embryoToMaterialMap);
            log.debug("按物料汇总胎胚库存 {} 条", materialStockMap.size());

            // Step 3: 计算成型余量
            for (Map.Entry<String, MdmMonthSurplus> entry : monthSurplusMap.entrySet()) {
                String materialCode = entry.getKey();
                MdmMonthSurplus surplus = entry.getValue();

                int vulcanizingRemainder = surplus.getPlanSurplusQty() != null
                        ? surplus.getPlanSurplusQty().intValue() : 0;
                int embryoStock = materialStockMap.getOrDefault(materialCode, 0);
                int formingRemainder = Math.max(0, vulcanizingRemainder - embryoStock);

                resultMap.put(materialCode, formingRemainder);
            }

            log.info("计算成型余量映射完成，共 {} 条", resultMap.size());

        } catch (Exception e) {
            log.error("计算成型余量映射失败", e);
        }

        return resultMap;
    }

    /**
     * 构建胎胚→物料的映射
     */
    private Map<String, String> buildEmbryoToMaterialMap(List<MdmMaterialInfo> materials) {
        Map<String, String> embryoToMaterialMap = new HashMap<>();
        for (MdmMaterialInfo material : materials) {
            String embryoCode = material.getEmbryoCode();
            String materialCode = material.getMaterialCode();
            if (embryoCode != null && materialCode != null) {
                embryoToMaterialMap.put(embryoCode, materialCode);
            }
        }
        return embryoToMaterialMap;
    }

    /**
     * 将胎胚库存按物料汇总
     */
    private Map<String, Integer> aggregateStockByMaterial(List<CxStock> stocks, Map<String, String> embryoToMaterialMap) {
        Map<String, Integer> materialStockMap = new HashMap<>();
        for (CxStock stock : stocks) {
            String embryoCode = stock.getEmbryoCode();
            if (embryoCode == null) {
                continue;
            }

            String materialCode = embryoToMaterialMap.get(embryoCode);
            if (materialCode == null) {
                continue;
            }

            int effectiveStock = stock.getEffectiveStock();
            materialStockMap.merge(materialCode, effectiveStock, Integer::sum);
        }
        return materialStockMap;
    }

    // ==================== 私有方法：收尾计算 ====================

    /**
     * 找到该物料的最近一个收尾日
     *
     * <p>从当前日期开始往后找，找到第一个连续排产区间的最后一天。
     *
     * @param plans         月计划列表
     * @param currentDay    当前日期（几号）
     * @param lastDayOfMonth 月末日期
     * @return 最近一个收尾日
     */
    private int findMaterialEndingDay(List<FactoryMonthPlanProductionFinalResult> plans, int currentDay, int lastDayOfMonth) {
        Set<Integer> productionDays = collectProductionDays(plans, currentDay, lastDayOfMonth);

        if (productionDays.isEmpty()) {
            return lastDayOfMonth;
        }

        int endingDay = currentDay;
        for (int day = currentDay; day <= lastDayOfMonth; day++) {
            if (productionDays.contains(day)) {
                endingDay = day;
            } else if (endingDay > currentDay) {
                break;
            }
        }

        return endingDay;
    }

    /**
     * 收集所有有排产的日期
     */
    private Set<Integer> collectProductionDays(List<FactoryMonthPlanProductionFinalResult> plans, int currentDay, int lastDayOfMonth) {
        Set<Integer> productionDays = new HashSet<>();
        for (FactoryMonthPlanProductionFinalResult plan : plans) {
            for (int day = currentDay; day <= lastDayOfMonth; day++) {
                Integer dayQty = plan.getDayQty(day);
                if (dayQty != null && dayQty > 0) {
                    productionDays.add(day);
                }
            }
        }
        return productionDays;
    }
}
