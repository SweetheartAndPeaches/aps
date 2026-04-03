package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.cx.vo.ScheduleRequestVo;
import com.zlt.aps.cx.entity.CxMaterialEnding;
import com.zlt.aps.cx.entity.CxStock;
import com.zlt.aps.cx.entity.config.CxKeyProduct;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.enums.DayVulcanizationModeEnum;
import com.zlt.aps.cx.mapper.CxKeyProductMapper;
import com.zlt.aps.cx.mapper.CxMaterialEndingMapper;
import com.zlt.aps.cx.mapper.CxParamConfigMapper;
import com.zlt.aps.cx.mapper.CxScheduleDetailMapper;
import com.zlt.aps.cx.mapper.CxScheduleResultMapper;
import com.zlt.aps.cx.mapper.CxShiftConfigMapper;
import com.zlt.aps.cx.mapper.CxStockMapper;
import com.zlt.aps.cx.mapper.MdmStructureTreadConfigMapper;
import com.zlt.aps.cx.mapper.FactoryMonthPlanProductionFinalResultMapper;
import com.zlt.aps.cx.mapper.LhScheduleResultMapper;
import com.zlt.aps.cx.mapper.MdmCxMachineOnlineInfoMapper;
import com.zlt.aps.cx.mapper.MdmDevicePlanShutMapper;
import com.zlt.aps.cx.mapper.MdmMaterialInfoMapper;
import com.zlt.aps.cx.mapper.MdmMoldingMachineMapper;
import com.zlt.aps.cx.mapper.MdmMonthPlanProductLhCapacityMapper;
import com.zlt.aps.cx.mapper.MdmMonthSurplusMapper;
import com.zlt.aps.cx.mapper.MdmSkuScheduleCategoryMapper;
import com.zlt.aps.cx.mapper.MdmStructureLhRatioMapper;
import com.zlt.aps.cx.mapper.MpCxCapacityConfigurationMapper;
import com.zlt.aps.cx.service.ConstraintCheckService;
import com.zlt.aps.cx.service.engine.CoreScheduleAlgorithmService;
import com.zlt.aps.cx.service.HolidayScheduleService;
import com.zlt.aps.cx.service.ScheduleService;
import com.zlt.aps.cx.service.impl.validation.ScheduleDataValidator;
import com.zlt.aps.cx.service.impl.validation.ScheduleDataValidationResult;
import com.zlt.aps.cx.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.mdm.api.domain.entity.MdmStructureTreadConfig;
import com.zlt.aps.mp.api.domain.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private static final String DEFAULT_FACTORY_CODE = "F001";

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
    private final MdmStructureTreadConfigMapper structureShiftCapacityMapper;
    private final CxKeyProductMapper keyProductMapper;
    private final LhScheduleResultMapper lhScheduleResultMapper;
    private final MdmCxMachineOnlineInfoMapper onlineInfoMapper;
    private final CxShiftConfigMapper shiftConfigMapper;
    private final FactoryMonthPlanProductionFinalResultMapper monthPlanMapper;
    private final CxMaterialEndingMapper materialEndingMapper;
    private final MpCxCapacityConfigurationMapper capacityConfigurationMapper;

    // ==================== 公共方法 ====================

    @Override
    public ScheduleResult executeSchedule(ScheduleRequestVo request) {
        ScheduleResult result = new ScheduleResult();
        result.setSuccess(false);
        result.setScheduleDate(request.getScheduleDate());

        try {
            log.info("开始执行排程，日期：{}，排程模式：{}", request.getScheduleDate(), request.getScheduleMode());

            // 1. 构建排程上下文(流程图S5.1.6初始化)
            ScheduleContextVo context = buildScheduleContext(request);
            if (context == null) {
                result.setMessage("构建排程上下文失败");
                return result;
            }

            // 2. 执行核心排程算法(流程图S5.2-S5.5)
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
    public boolean reSchedule(ScheduleRequestVo request) {
        try {
            log.info("开始执行重排程，日期：{}", request.getScheduleDate());

            // 1. 构建排程上下文
            ScheduleContextVo context = buildScheduleContext(request);

            // 2. 执行重排程算法
            List<CxScheduleResult> scheduleResults = coreScheduleAlgorithmService.executeSchedule(context);

            // 3. 删除原有排程结果
            deleteExistingScheduleResults(request.getScheduleDate());

            // 4. 保存新的排程结果
            saveScheduleResults(scheduleResults);

            log.info("重排程完成，日期：{}，结果数量：{}", request.getScheduleDate(), scheduleResults.size());
            return true;

        } catch (Exception e) {
            log.error("重排程失败", e);
            return false;
        }
    }

    /**
     * 删除指定日期的排程结果
     */
    private void deleteExistingScheduleResults(LocalDate scheduleDate) {
        scheduleResultMapper.delete(
                new LambdaQueryWrapper<CxScheduleResult>()
                        .eq(CxScheduleResult::getScheduleDate, scheduleDate)
        );
    }

    /**
     * 构建排程上下文
     */
    private ScheduleContextVo buildScheduleContext(ScheduleRequestVo request) {
        try {
            ScheduleContextVo context = new ScheduleContextVo();
            LocalDate scheduleDate = request.getScheduleDate();
            log.info("开始构建排程上下文，日期：{}，工厂：{}", scheduleDate, request.getFactoryCode());

            // 1. 加载班次配置
            String factoryCode = request.getFactoryCode() != null ? request.getFactoryCode() : DEFAULT_FACTORY_CODE;
            context.setFactoryCode(factoryCode);
            loadShiftConfigs(context, factoryCode);
            log.info("班次配置加载完成，班次数：{}", context.getShiftConfigList() != null ? context.getShiftConfigList().size() : 0);

            // 2. 获取设备计划停机信息
            try {
                loadDevicePlanShuts(context, scheduleDate);
                log.info("设备计划停机信息加载完成");
            } catch (Exception e) {
                log.warn("加载设备计划停机信息失败，继续执行：{}", e.getMessage());
            }

            // 3. 获取所有机台
            loadMoldingMachines(context);
            log.info("机台信息加载完成，机台数：{}", context.getAvailableMachines() != null ? context.getAvailableMachines().size() : 0);

            // 4. 获取硫化排程结果（后续会根据成型余量过滤）
            try {
                loadLhScheduleResults(context, scheduleDate);
                log.info("硫化排程结果加载完成");
            } catch (Exception e) {
                log.warn("加载硫化排程结果失败，继续执行：{}", e.getMessage());
            }

            // 5. 根据硫化排程结果获取物料信息
            try {
                loadMaterials(context);
                log.info("物料信息加载完成");
            } catch (Exception e) {
                log.warn("加载物料信息失败，继续执行：{}", e.getMessage());
            }

            // 6. 获取胎胚库存信息
            try {
                loadStocks(context);
                log.info("胎胚库存信息加载完成");
            } catch (Exception e) {
                log.warn("加载胎胚库存信息失败，继续执行：{}", e.getMessage());
            }

            // 7. 获取成型在机信息
            try {
                loadOnlineInfos(context, scheduleDate);
                log.info("成型在机信息加载完成");
            } catch (Exception e) {
                log.warn("加载成型在机信息失败，继续执行：{}", e.getMessage());
            }

            // 8. 构建机台在机胎胚映射（后续会根据成型余量过滤）
            try {
                buildMachineOnlineEmbryoMap(context);
            } catch (Exception e) {
                log.warn("构建机台在机胎胚映射失败，继续执行：{}", e.getMessage());
            }

            // 9. 获取参数配置
            try {
                loadParamConfigs(context);
            } catch (Exception e) {
                log.warn("加载参数配置失败，继续执行：{}", e.getMessage());
            }

            // 10. 获取结构整车配置
            try {
                loadStructureShiftCapacities(context);
            } catch (Exception e) {
                log.warn("加载结构整车配置失败，继续执行：{}", e.getMessage());
            }

            // 11. 获取关键产品配置
            try {
                loadKeyProducts(context);
            } catch (Exception e) {
                log.warn("加载关键产品配置失败，继续执行：{}", e.getMessage());
            }

            // 12. 构建物料日产能映射/成型硫化配比
            try {
                buildCapacityMaps(context);
            } catch (Exception e) {
                log.warn("构建产能映射失败，继续执行：{}", e.getMessage());
            }

            // 13. 获取月度计划余量并计算成型余量（考虑共用胎胚）
            try {
                loadMonthSurplusAndCalculateFormingRemainder(context, scheduleDate);
            } catch (Exception e) {
                log.warn("加载月度计划余量失败，继续执行：{}", e.getMessage());
            }

            // 14. 获取SKU排产分类
            try {
                loadSkuCategories(context);
            } catch (Exception e) {
                log.warn("加载SKU排产分类失败，继续执行：{}", e.getMessage());
            }

            // 15. 设置节假日相关标记
            try {
                setHolidayFlags(context, scheduleDate);
            } catch (Exception e) {
                log.warn("设置节假日标记失败，继续执行：{}", e.getMessage());
            }

            // 16. 加载物料收尾信息并计算收尾日
            try {
                loadMaterialEndings(context, scheduleDate);
            } catch (Exception e) {
                log.warn("加载物料收尾信息失败，继续执行：{}", e.getMessage());
            }

            // 17. 过滤已收尾物料（成型余量<=0的物料不参与排程）
            try {
                filterCompletedMaterials(context);
            } catch (Exception e) {
                log.warn("过滤已收尾物料失败，继续执行：{}", e.getMessage());
            }

            // 18. 加载结构排产配置（用于均衡分配）
            try {
                loadStructureAllocations(context, scheduleDate);
            } catch (Exception e) {
                log.warn("加载结构排产配置失败，继续执行：{}", e.getMessage());
            }

            // 19. 设置排程参数
            context.setScheduleDate(scheduleDate);
            context.setScheduleMode(request.getScheduleMode());

            log.info("排程上下文构建完成");
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
    private void loadShiftConfigs(ScheduleContextVo context, String factoryCode) {
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
    private void loadDevicePlanShuts(ScheduleContextVo context, LocalDate scheduleDate) {
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
    private void loadMoldingMachines(ScheduleContextVo context) {
        List<MdmMoldingMachine> machines = moldingMachineMapper.selectList(null);
        context.setAvailableMachines(machines);
        log.info("加载成型机台 {} 台", machines.size());
    }

    /**
     * 加载硫化排程结果
     */
    private void loadLhScheduleResults(ScheduleContextVo context, LocalDate scheduleDate) {
        List<LhScheduleResult> lhScheduleResults = lhScheduleResultMapper.selectByDate(scheduleDate);
        context.setLhScheduleResults(lhScheduleResults);
        log.info("加载硫化排程结果 {} 条", lhScheduleResults.size());
    }

    /**
     * 加载物料信息
     */
    private void loadMaterials(ScheduleContextVo context) {
        List<LhScheduleResult> lhScheduleResults = context.getLhScheduleResults();

        if (lhScheduleResults == null || lhScheduleResults.isEmpty()) {
            log.info("硫化排程结果为空，加载物料信息 0 条");
            context.setMaterials(new ArrayList<>());
            return;
        }

        Set<String> materialCodes = lhScheduleResults.stream()
                .map(LhScheduleResult::getMaterialCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        log.debug("从硫化排程结果提取到 {} 个不重复的外胎代码", materialCodes.size());

        List<MdmMaterialInfo> materials;
        if (!materialCodes.isEmpty()) {
            // 使用 embryoCode 查询物料信息（一个物料对应一个胎胚）
            materials = materialInfoMapper.selectList(
                    new LambdaQueryWrapper<MdmMaterialInfo>()
                            .in(MdmMaterialInfo::getMaterialCode, materialCodes));
            log.info("根据硫化排程结果加载物料信息 {} 条，涉及 {} 个外胎", materials.size(), materialCodes.size());
        } else {
            materials = new ArrayList<>();
            log.warn("硫化排程结果中包含 {} 条记录，但没有有效的外胎代码 (materialCodes 均为 null)，加载物料信息 0 条",
                    lhScheduleResults.size());
        }
        context.setMaterials(materials);
    }

    /**
     * 加载胎胚库存
     */
    private void loadStocks(ScheduleContextVo context) {
        List<CxStock> stocks = stockMapper.selectList(
                new LambdaQueryWrapper<CxStock>()
                        .gt(CxStock::getStockNum, 0));
        context.setStocks(stocks);
        log.info("加载胎胚库存 {} 条", stocks.size());
    }

    /**
     * 加载成型在机信息
     */
    private void loadOnlineInfos(ScheduleContextVo context, LocalDate scheduleDate) {
        List<MdmCxMachineOnlineInfo> onlineInfos = onlineInfoMapper.selectByDateRange(
                scheduleDate, scheduleDate.minusDays(1));
        context.setOnlineInfos(onlineInfos);
        log.info("加载成型在机信息 {} 条", onlineInfos.size());
    }

    /**
     * 构建机台在机胎胚映射
     */
    private void buildMachineOnlineEmbryoMap(ScheduleContextVo context) {
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
    private void loadParamConfigs(ScheduleContextVo context) {
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
    private void loadStructureShiftCapacities(ScheduleContextVo context) {
        List<MdmStructureTreadConfig> structureShiftCapacities = structureShiftCapacityMapper.selectList(null);
        context.setStructureShiftCapacities(structureShiftCapacities);
    }

    /**
     * 加载关键产品配置
     */
    private void loadKeyProducts(ScheduleContextVo context) {
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
     * 加载结构排产配置
     *
     * <p>从 T_MP_STRUCTURE_ALLOCATION 表获取每个结构可分配的机台列表
     * <p>用于续作任务的均衡分配
     */
    private void loadStructureAllocations(ScheduleContextVo context, LocalDate scheduleDate) {
        int year = scheduleDate.getYear();
        int month = scheduleDate.getMonthValue();

        // 查询当月的结构排产配置
        List<MpCxCapacityConfiguration> allocations = capacityConfigurationMapper.selectByYearAndMonth(year, month);
        context.setStructureAllocations(allocations);

        // 按结构分组
        Map<String, List<MpCxCapacityConfiguration>> structureAllocationMap = allocations.stream()
                .filter(a -> a.getStructureName() != null)
                .collect(Collectors.groupingBy(
                        MpCxCapacityConfiguration::getStructureName,
                        () -> new LinkedHashMap<>(),
                        Collectors.toList()));

        context.setStructureAllocationMap(structureAllocationMap);
        log.info("加载结构排产配置 {} 条，共 {} 个结构", allocations.size(), structureAllocationMap.size());
    }

    /**
     * 构建产能映射
     */
    private void buildCapacityMaps(ScheduleContextVo context) {
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
    private void loadMonthSurplusAndCalculateFormingRemainder(ScheduleContextVo context, LocalDate scheduleDate) {
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
    private void loadSkuCategories(ScheduleContextVo context) {
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
    private void setHolidayFlags(ScheduleContextVo context, LocalDate scheduleDate) {
        context.setIsOpeningDay(holidayScheduleService.isStartProductionDay(scheduleDate));
        context.setIsClosingDay(holidayScheduleService.isStopProductionDay(scheduleDate));
        context.setIsBeforeClosingDay(holidayScheduleService.isBeforeHoliday(scheduleDate));
    }

    /**
     * 数据完整性校验
     */
    private void validateScheduleData(ScheduleContextVo context, LocalDate scheduleDate, String factoryCode) {
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
                    detail.setMainId(result.getId());
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
    private Map<String, MonthPlanProductLhCapacityVo> buildMaterialLhCapacityMap(ScheduleContextVo context) {
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
    private DayVulcanizationModeEnum getDayVulcanizationMode(ScheduleContextVo context) {
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
     * <p>处理胎胚共用场景：
     * <ul>
     *   <li>一个胎胚可能被多个物料共用</li>
     *   <li>库存按硫化需求比例分配给各物料</li>
     *   <li>比例来源：硫化排程结果的日计划量</li>
     * </ul>
     *
     * @param materials       物料信息列表
     * @param monthSurplusMap 月度计划硫化余量映射
     * @param stocks          胎胚库存列表
     * @return 成型余量映射
     */
    private Map<String, Integer> calculateFormingRemainderMap(
            List<MdmMaterialInfo> materials,
            Map<String, MdmMonthSurplus> monthSurplusMap,
            List<CxStock> stocks) {

        Map<String, Integer> resultMap = new HashMap<>();

        try {
            // Step 1: 构建胎胚→物料列表的映射（支持一个胎胚对应多个物料）
            Map<String, List<String>> embryoToMaterialsMap = buildEmbryoToMaterialsMap(materials);
            log.debug("构建胎胚→物料列表映射 {} 条", embryoToMaterialsMap.size());

            // Step 2: 构建物料→胎胚的映射
            Map<String, String> materialToEmbryoMap = new HashMap<>();
            for (MdmMaterialInfo material : materials) {
                if (material.getEmbryoCode() != null && material.getMaterialCode() != null) {
                    materialToEmbryoMap.put(material.getMaterialCode(), material.getEmbryoCode());
                }
            }

            // Step 3: 计算各物料的硫化需求比例（用于分配共用胎胚库存）
            Map<String, Integer> materialDemandMap = calculateMaterialDemandRatio(materials, monthSurplusMap);
            log.debug("计算物料硫化需求比例 {} 条", materialDemandMap.size());

            // Step 4: 按比例分配胎胚库存到物料
            Map<String, Integer> materialStockMap = allocateStockByMaterialRatio(stocks, embryoToMaterialsMap, materialDemandMap);
            log.debug("按比例分配胎胚库存到物料 {} 条", materialStockMap.size());

            // Step 5: 计算成型余量
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
     * 构建胎胚→物料列表的映射（支持一个胎胚对应多个物料）
     */
    private Map<String, List<String>> buildEmbryoToMaterialsMap(List<MdmMaterialInfo> materials) {
        Map<String, List<String>> embryoToMaterialsMap = new HashMap<>();
        for (MdmMaterialInfo material : materials) {
            String embryoCode = material.getEmbryoCode();
            String materialCode = material.getMaterialCode();
            if (embryoCode != null && materialCode != null) {
                embryoToMaterialsMap.computeIfAbsent(embryoCode, k -> new ArrayList<>()).add(materialCode);
            }
        }
        return embryoToMaterialsMap;
    }

    /**
     * 计算各物料的硫化需求比例
     *
     * <p>使用月计划余量作为需求比例的参考值
     */
    private Map<String, Integer> calculateMaterialDemandRatio(
            List<MdmMaterialInfo> materials,
            Map<String, MdmMonthSurplus> monthSurplusMap) {

        Map<String, Integer> demandMap = new HashMap<>();

        for (MdmMaterialInfo material : materials) {
            String materialCode = material.getMaterialCode();
            if (materialCode == null) {
                continue;
            }

            // 优先使用月计划余量作为需求比例
            MdmMonthSurplus surplus = monthSurplusMap.get(materialCode);
            if (surplus != null && surplus.getPlanSurplusQty() != null) {
                demandMap.put(materialCode, surplus.getPlanSurplusQty().intValue());
            } else {
                // 默认需求为1，避免除零
                demandMap.put(materialCode, 1);
            }
        }

        return demandMap;
    }

    /**
     * 按比例分配胎胚库存到物料
     *
     * <p>当一个胎胚被多个物料共用时，按各物料的硫化需求比例分配库存
     * <p>例如：胎胚1被物料A和物料B共用，A的需求=300，B的需求=500，库存=800
     * <p>则A分配: 800 * 300/800 = 300，B分配: 800 * 500/800 = 500
     */
    private Map<String, Integer> allocateStockByMaterialRatio(
            List<CxStock> stocks,
            Map<String, List<String>> embryoToMaterialsMap,
            Map<String, Integer> materialDemandMap) {

        Map<String, Integer> materialStockMap = new HashMap<>();

        for (CxStock stock : stocks) {
            String embryoCode = stock.getEmbryoCode();
            if (embryoCode == null) {
                continue;
            }

            int totalStock = stock.getEffectiveStock();
            List<String> materialCodes = embryoToMaterialsMap.get(embryoCode);

            if (materialCodes == null || materialCodes.isEmpty()) {
                // 胎胚没有对应的物料，跳过
                log.debug("胎胚 {} 没有对应的物料，跳过", embryoCode);
                continue;
            }

            if (materialCodes.size() == 1) {
                // 胎胚只对应一个物料，直接分配全部库存
                String materialCode = materialCodes.get(0);
                materialStockMap.merge(materialCode, totalStock, Integer::sum);
                log.debug("胎胚 {} 只对应物料 {}，分配库存 {}", embryoCode, materialCode, totalStock);
            } else {
                // 胎胚对应多个物料，按需求比例分配
                int totalDemand = 0;
                Map<String, Integer> demands = new HashMap<>();

                for (String materialCode : materialCodes) {
                    int demand = materialDemandMap.getOrDefault(materialCode, 1);
                    demands.put(materialCode, demand);
                    totalDemand += demand;
                }

                if (totalDemand == 0) {
                    // 总需求为0，平均分配
                    int avgStock = totalStock / materialCodes.size();
                    for (String materialCode : materialCodes) {
                        materialStockMap.merge(materialCode, avgStock, Integer::sum);
                    }
                    log.debug("胎胚 {} 对应多个物料但总需求为0，平均分配库存 {}", embryoCode, avgStock);
                } else {
                    // 按比例分配
                    int allocatedTotal = 0;
                    String lastMaterial = null;

                    for (int i = 0; i < materialCodes.size(); i++) {
                        String materialCode = materialCodes.get(i);
                        int demand = demands.get(materialCode);
                        int allocatedStock;

                        if (i == materialCodes.size() - 1) {
                            // 最后一个物料分配剩余库存，避免四舍五入误差
                            allocatedStock = totalStock - allocatedTotal;
                        } else {
                            // 按比例分配
                            allocatedStock = (int) ((double) totalStock * demand / totalDemand);
                        }

                        materialStockMap.merge(materialCode, allocatedStock, Integer::sum);
                        allocatedTotal += allocatedStock;
                        lastMaterial = materialCode;

                        log.debug("胎胚 {} 共用分配：物料 {} 需求占比 {}/{}，分配库存 {}",
                                embryoCode, materialCode, demand, totalDemand, allocatedStock);
                    }
                }
            }
        }

        return materialStockMap;
    }

    /**
     * 加载物料收尾信息并计算收尾日
     *
     * <p>流程：
     * <ol>
     *   <li>从 T_CX_MATERIAL_ENDING 表加载已存在的收尾信息</li>
     *   <li>对于没有收尾信息的物料，从月计划计算收尾日</li>
     *   <li>计算成型余量、预计收尾天数、紧急收尾标记等</li>
     * </ol>
     *
     * @param context      排程上下文
     * @param scheduleDate 排程日期
     */
    private void loadMaterialEndings(ScheduleContextVo context, LocalDate scheduleDate) {
        int year = scheduleDate.getYear();
        int month = scheduleDate.getMonthValue();
        int currentDay = scheduleDate.getDayOfMonth();
        int lastDayOfMonth = scheduleDate.withDayOfMonth(scheduleDate.lengthOfMonth()).getDayOfMonth();
        Integer yearMonth = year * 100 + month;

        // 1. 尝试从数据库加载已存在的收尾信息
        List<CxMaterialEnding> existingEndings = materialEndingMapper.selectByStatDate(scheduleDate);

        // 2. 获取月计划数据
        List<FactoryMonthPlanProductionFinalResult> monthPlans = monthPlanMapper.selectByYearAndMonth(year, month);
        Map<String, List<FactoryMonthPlanProductionFinalResult>> materialPlanMap = monthPlans.stream()
                .filter(p -> p.getMaterialCode() != null)
                .collect(Collectors.groupingBy(FactoryMonthPlanProductionFinalResult::getMaterialCode));

        // 3. 获取物料信息和库存
        Map<String, MdmMaterialInfo> materialMap = context.getMaterials() != null
                ? context.getMaterials().stream()
                .collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode, m -> m, (a, b) -> a))
                : new HashMap<>();

        Map<String, Integer> formingRemainderMap = context.getFormingRemainderMap() != null
                ? context.getFormingRemainderMap()
                : new HashMap<>();

        Map<String, MdmMonthSurplus> monthSurplusMap = context.getMonthSurplusMap() != null
                ? context.getMonthSurplusMap()
                : new HashMap<>();

        // 4. 如果已有收尾信息，直接使用
        if (!existingEndings.isEmpty()) {
            context.setMaterialEndings(existingEndings);
            log.info("从数据库加载物料收尾信息 {} 条", existingEndings.size());
            return;
        }

        // 5. 计算每个物料的收尾信息
        List<CxMaterialEnding> materialEndings = new ArrayList<>();

        // 获取所有需要处理的物料编码（硫化排程中的物料）
        Set<String> materialCodes = new HashSet<>();
        if (context.getLhScheduleResults() != null) {
            materialCodes.addAll(context.getLhScheduleResults().stream()
                    .filter(r -> r.getMaterialCode() != null)
                    .map(LhScheduleResult::getMaterialCode)
                    .collect(Collectors.toSet()));
        }
        // 也包含月计划中的物料
        materialCodes.addAll(materialPlanMap.keySet());

        for (String materialCode : materialCodes) {
            CxMaterialEnding ending = new CxMaterialEnding();
            ending.setMaterialCode(materialCode);
            ending.setStatDate(scheduleDate);

            // 获取物料信息
            MdmMaterialInfo material = materialMap.get(materialCode);
            if (material != null) {
                ending.setMaterialDesc(material.getMaterialDesc());
                ending.setStructureName(material.getStructureName());
            }

            // 获取硫化余量
            MdmMonthSurplus surplus = monthSurplusMap.get(materialCode);
            if (surplus != null && surplus.getPlanSurplusQty() != null) {
                ending.setVulcanizingRemainder(surplus.getPlanSurplusQty().intValue());
            }

            // 获取成型余量
            Integer formingRemainder = formingRemainderMap.get(materialCode);
            if (formingRemainder != null) {
                ending.setFormingRemainder(formingRemainder);
            } else if (ending.getVulcanizingRemainder() != null) {
                // 成型余量 = 硫化余量 - 胎胚库存
                ending.setFormingRemainder(ending.getVulcanizingRemainder());
            }

            // 计算收尾日
            List<FactoryMonthPlanProductionFinalResult> plans = materialPlanMap.get(materialCode);
            if (plans != null && !plans.isEmpty()) {
                int endingDay = findMaterialEndingDay(plans, currentDay, lastDayOfMonth);
                LocalDate plannedEndingDate = scheduleDate.withDayOfMonth(endingDay);
                ending.setPlannedEndingDate(plannedEndingDate);

                // 计算距收尾日的天数
                int daysToEnding = (int) java.time.temporal.ChronoUnit.DAYS.between(scheduleDate, plannedEndingDate);
                ending.setEstimatedEndingDays(BigDecimal.valueOf(daysToEnding));

                // 设置收尾标记
                if (daysToEnding >= 0 && daysToEnding <= URGENT_ENDING_DAYS) {
                    ending.setIsUrgentEnding(1);
                }
                if (daysToEnding >= 0 && daysToEnding <= NEAR_ENDING_DAYS) {
                    ending.setIsNearEnding(1);
                }

                // 计算延误量（如果成型余量 > 0 且接近收尾日）
                if (formingRemainder != null && formingRemainder > 0 && daysToEnding >= 0 && daysToEnding <= NEAR_ENDING_DAYS) {
                    // 日产能估算（简化：假设每天能做100条）
                    int dailyCapacity = 100;
                    int remainingDays = Math.max(daysToEnding, 1);
                    int producibleQty = dailyCapacity * remainingDays;

                    if (formingRemainder > producibleQty) {
                        int delayQty = formingRemainder - producibleQty;
                        ending.setDelayQuantity(delayQty);
                        ending.setDistributedQuantity(delayQty / CATCH_UP_DAYS);

                        // 如果未来3天满产仍追不上，需要调整月计划
                        if (delayQty > dailyCapacity * CATCH_UP_DAYS) {
                            ending.setNeedMonthPlanAdjust(1);
                        }
                    }
                }
            } else {
                // 没有月计划，默认月末收尾
                ending.setPlannedEndingDate(scheduleDate.withDayOfMonth(lastDayOfMonth));
                ending.setEstimatedEndingDays(BigDecimal.valueOf(lastDayOfMonth - currentDay));
            }

            materialEndings.add(ending);
        }

        context.setMaterialEndings(materialEndings);
        log.info("计算物料收尾信息 {} 条", materialEndings.size());

        // 统计紧急收尾数量
        long urgentCount = materialEndings.stream()
                .filter(e -> e.getIsUrgentEnding() != null && e.getIsUrgentEnding() == 1)
                .count();
        if (urgentCount > 0) {
            log.warn("发现 {} 个紧急收尾物料", urgentCount);
        }
    }

    /**
     * 过滤已收尾物料
     *
     * <p>成型余量 <= 0 的物料表示已经收尾完成，不参与排程。
     * <p>需要过滤：
     * <ul>
     *   <li>硫化排程结果：移除已收尾物料的任务</li>
     *   <li>在机信息：移除已收尾物料的在机记录</li>
     *   <li>机台在机胎胚映射：移除已收尾物料的映射</li>
     * </ul>
     *
     * @param context 排程上下文
     */
    private void filterCompletedMaterials(ScheduleContextVo context) {
        // 获取成型余量映射
        Map<String, Integer> formingRemainderMap = context.getFormingRemainderMap();
        if (formingRemainderMap == null || formingRemainderMap.isEmpty()) {
            log.debug("成型余量映射为空，跳过过滤");
            return;
        }

        // 构建已收尾物料集合（成型余量 <= 0）
        Set<String> completedMaterialCodes = new HashSet<>();
        for (Map.Entry<String, Integer> entry : formingRemainderMap.entrySet()) {
            if (entry.getValue() != null && entry.getValue() <= 0) {
                completedMaterialCodes.add(entry.getKey());
            }
        }

        if (completedMaterialCodes.isEmpty()) {
            log.debug("没有已收尾的物料，跳过过滤");
            return;
        }

        log.info("发现 {} 个已收尾物料，开始过滤", completedMaterialCodes.size());

        // 1. 过滤硫化排程结果
        int originalLhCount = context.getLhScheduleResults() != null ? context.getLhScheduleResults().size() : 0;
        if (context.getLhScheduleResults() != null) {
            List<LhScheduleResult> filteredLhResults = context.getLhScheduleResults().stream()
                    .filter(r -> {
                        String materialCode = r.getMaterialCode();
                        // 如果物料编码在已收尾集合中，则过滤掉
                        if (materialCode != null && completedMaterialCodes.contains(materialCode)) {
                            log.debug("过滤硫化排程结果：物料={}，成型余量={}",
                                    materialCode, formingRemainderMap.get(materialCode));
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
            context.setLhScheduleResults(filteredLhResults);
            log.info("过滤硫化排程结果：{} -> {} 条（移除 {} 条已收尾物料任务）",
                    originalLhCount, filteredLhResults.size(), originalLhCount - filteredLhResults.size());
        }

        // 2. 过滤在机信息
        int originalOnlineCount = context.getOnlineInfos() != null ? context.getOnlineInfos().size() : 0;
        if (context.getOnlineInfos() != null) {
            // 需要先将物料编码转换为胎胚编码
            Map<String, String> materialToEmbryoMap = new HashMap<>();
            if (context.getMaterials() != null) {
                for (MdmMaterialInfo material : context.getMaterials()) {
                    if (material.getMaterialCode() != null && material.getEmbryoCode() != null) {
                        materialToEmbryoMap.put(material.getMaterialCode(), material.getEmbryoCode());
                    }
                }
            }

            // 构建已收尾的胎胚编码集合
            Set<String> completedEmbryoCodes = new HashSet<>();
            for (String materialCode : completedMaterialCodes) {
                String embryoCode = materialToEmbryoMap.get(materialCode);
                if (embryoCode != null) {
                    completedEmbryoCodes.add(embryoCode);
                }
            }

            List<MdmCxMachineOnlineInfo> filteredOnlineInfos = context.getOnlineInfos().stream()
                    .filter(info -> {
                        String embryoCode = info.getMesMaterialCode();
                        // 如果胎胚编码在已收尾集合中，则过滤掉
                        if (embryoCode != null && completedEmbryoCodes.contains(embryoCode)) {
                            log.debug("过滤在机信息：机台={}，胎胚={}，对应物料已收尾",
                                    info.getCxCode(), embryoCode);
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
            context.setOnlineInfos(filteredOnlineInfos);
            log.info("过滤在机信息：{} -> {} 条（移除 {} 条已收尾物料在机记录）",
                    originalOnlineCount, filteredOnlineInfos.size(), originalOnlineCount - filteredOnlineInfos.size());
        }

        // 3. 重新构建机台在机胎胚映射（使用过滤后的在机信息）
        if (context.getOnlineInfos() != null) {
            Map<String, Set<String>> machineOnlineEmbryoMap = new HashMap<>();
            for (MdmCxMachineOnlineInfo onlineInfo : context.getOnlineInfos()) {
                String cxCode = onlineInfo.getCxCode();
                String embryoCode = onlineInfo.getMesMaterialCode();
                if (cxCode != null && embryoCode != null) {
                    machineOnlineEmbryoMap.computeIfAbsent(cxCode, k -> new HashSet<>()).add(embryoCode);
                }
            }
            context.setMachineOnlineEmbryoMap(machineOnlineEmbryoMap);
            log.info("重新构建机台在机胎胚映射，共 {} 个机台有在机任务", machineOnlineEmbryoMap.size());
        }

        // 4. 记录被过滤的物料信息
        if (!completedMaterialCodes.isEmpty()) {
            StringBuilder sb = new StringBuilder("已收尾物料列表：\n");
            for (String materialCode : completedMaterialCodes) {
                Integer remainder = formingRemainderMap.get(materialCode);
                sb.append(String.format("  - 物料: %s, 成型余量: %d\n", materialCode, remainder));
            }
            log.info(sb.toString());
        }
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
