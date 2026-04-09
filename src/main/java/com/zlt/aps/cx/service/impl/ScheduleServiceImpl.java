package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.zlt.aps.cx.entity.CxMaterialEnding;
import com.zlt.aps.cx.entity.CxStock;
import com.zlt.aps.cx.entity.config.CxKeyProduct;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.enums.DayVulcanizationModeEnum;
import com.zlt.aps.cx.mapper.*;
import com.zlt.aps.cx.service.ConstraintCheckService;
import com.zlt.aps.cx.service.HolidayScheduleService;
import com.zlt.aps.cx.service.ScheduleService;
import com.zlt.aps.cx.service.engine.CoreScheduleAlgorithmService;
import com.zlt.aps.cx.service.impl.validation.ScheduleDataValidationResult;
import com.zlt.aps.cx.service.impl.validation.ScheduleDataValidator;
import com.zlt.aps.cx.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.cx.vo.ScheduleRequestVo;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.mp.api.domain.entity.MdmDevicePlanShut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
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
    private static final String DEFAULT_FACTORY_CODE = "116";

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

            // 2. 数据完整性校验
            validateScheduleData(context, request.getScheduleDate(), request.getFactoryCode());

            // 3. 执行核心排程算法(流程图S5.2-S5.5)
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
            // 前端传入的是最后一天，排产起始日期需要往前推2天
            LocalDate scheduleStartDate = scheduleDate.minusDays(2);
            log.info("开始构建排程上下文，排产起始日期：{}，最后一天：{}，工厂：{}", 
                    scheduleStartDate, scheduleDate, request.getFactoryCode());

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

            // 6. 获取胎胚库存信息（根据排产起始日期获取早上6点的库存）
            try {
                loadStocks(context, scheduleStartDate);
                log.info("胎胚库存信息加载完成");
            } catch (Exception e) {
                log.warn("加载胎胚库存信息失败，继续执行：{}", e.getMessage());
            }

            // 7. 获取成型在机信息
            try {
                loadOnlineInfos(context, scheduleStartDate);
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

            // 19. 加载结构整车配置（用于按车分配计算）
            try {
                loadStructureTreadConfigs(context);
            } catch (Exception e) {
                log.warn("加载结构整车配置失败，继续执行：{}", e.getMessage());
            }

            // 20. 设置排程参数
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
        log.info("班次配置加载完成，班次数：{}，示例：{}", 
                allShiftConfigs != null ? allShiftConfigs.size() : 0,
                allShiftConfigs != null && !allShiftConfigs.isEmpty() 
                        ? allShiftConfigs.get(0).getShiftCode() : "无");

        // 按排程天数分组
        Map<Integer, List<CxShiftConfig>> dayShiftMap = allShiftConfigs.stream()
                .filter(c -> c.getScheduleDay() != null)
                .collect(Collectors.groupingBy(CxShiftConfig::getScheduleDay));

        int scheduleDays = dayShiftMap.isEmpty() ? DEFAULT_SCHEDULE_DAYS
                : dayShiftMap.keySet().stream().max(Integer::compareTo).orElse(DEFAULT_SCHEDULE_DAYS);
        context.setScheduleDays(scheduleDays);
        log.info("根据班次配置计算排程天数: {}, 班次分布: {}", scheduleDays, 
                dayShiftMap.entrySet().stream()
                        .map(e -> e.getKey() + ":" + e.getValue().size() + "个")
                        .collect(Collectors.joining(", ")));
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
     * 加载成型机台（只加载启用且未删除的机台）
     */
    private void loadMoldingMachines(ScheduleContextVo context) {
        List<MdmMoldingMachine> machines = moldingMachineMapper.selectList(
                new LambdaQueryWrapper<MdmMoldingMachine>()
                        .eq(MdmMoldingMachine::getIsActive, 1)
                        .eq(MdmMoldingMachine::getIsDelete, "0"));
        context.setAvailableMachines(machines);
        log.info("加载成型机台 {} 台（已过滤禁用和已删除）", machines.size());
    }

    /**
     * 加载硫化排程结果
     */
    private void loadLhScheduleResults(ScheduleContextVo context, LocalDate scheduleDate) {
        log.info("查询硫化排程结果，日期: {}", scheduleDate);
        List<LhScheduleResult> lhScheduleResults = lhScheduleResultMapper.selectByDate(scheduleDate);
        log.info("硫化排程查询结果: {} 条", lhScheduleResults != null ? lhScheduleResults.size() : 0);
        
        if (lhScheduleResults == null || lhScheduleResults.isEmpty()) {
            // 尝试不带条件查询，看看是否有数据
            List<LhScheduleResult> allResults = lhScheduleResultMapper.selectAll();
            log.info("全表查询硫化排程结果: {} 条", allResults != null ? allResults.size() : 0);
            if (allResults != null && !allResults.isEmpty()) {
                log.info("硫化数据示例 - 日期: {}, 物料: {}, 胚号: {}", 
                    allResults.get(0).getScheduleDate(), 
                    allResults.get(0).getMaterialCode(),
                    allResults.get(0).getEmbryoCode());
            }
        }
        
        context.setLhScheduleResults(lhScheduleResults);
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
            // 使用 embryoCode 查询物料信息（一个物料对应一个胎胚），只查询未删除的数据
            materials = materialInfoMapper.selectList(
                    new LambdaQueryWrapper<MdmMaterialInfo>()
                            .in(MdmMaterialInfo::getMaterialCode, materialCodes)
                            .eq(MdmMaterialInfo::getIsDelete, "0"));
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
     *
     * <p>根据排程日期获取早上6点那一刻的库存
     *
     * @param context       排程上下文
     * @param scheduleDate  排程日期
     */
    private void loadStocks(ScheduleContextVo context, LocalDate scheduleDate) {
        // 将 LocalDate 转换为 java.sql.Date 用于数据库查询
        Date stockDate = Date.from(scheduleDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        List<CxStock> stocks = stockMapper.selectList(
                new LambdaQueryWrapper<CxStock>()
                        .eq(CxStock::getStockDate, stockDate)
                        .gt(CxStock::getStockNum, 0));
        context.setStocks(stocks);
        log.info("加载胎胚库存 {} 条 (库存日期: {})", stocks.size(), scheduleDate);
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
     *
     * <p>使用物料编码 + 胎胚编码组合作为唯一键：
     * - 机台在产: mesMaterialCode + embryoSpec
     * - 硫化任务: materialCode + embryoCode
     */
    private void buildMachineOnlineEmbryoMap(ScheduleContextVo context) {
        Map<String, Set<String>> machineOnlineEmbryoMap = new HashMap<>();
        for (MdmCxMachineOnlineInfo onlineInfo : context.getOnlineInfos()) {
            String cxCode = onlineInfo.getCxCode();
            // 组合物料编码和胎胚编码作为唯一键
            String materialCode = onlineInfo.getMesMaterialCode();
            String embryoSpec = onlineInfo.getEmbryoSpec();
            String combinedKey = materialCode + "|" + embryoSpec;
            if (cxCode != null && combinedKey != null && !combinedKey.equals("|")) {
                machineOnlineEmbryoMap.computeIfAbsent(cxCode, k -> new HashSet<>()).add(combinedKey);
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
        log.info("加载参数配置，共 {} 条记录", paramConfigs != null ? paramConfigs.size() : 0);
        if (paramConfigs != null && !paramConfigs.isEmpty()) {
            for (CxParamConfig config : paramConfigs) {
                log.debug("参数配置：{} = {}", config.getParamCode(), config.getParamValue());
            }
        }
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
        List<MdmStructureTreadConfig> structureShiftCapacities = structureShiftCapacityMapper.selectList(
                new LambdaQueryWrapper<MdmStructureTreadConfig>()
                        .eq(MdmStructureTreadConfig::getIsDelete, "0"));
        context.setStructureShiftCapacities(structureShiftCapacities);
        log.info("加载结构班次产能配置 {} 条", structureShiftCapacities.size());
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
        String factoryCode = context.getFactoryCode() != null ? context.getFactoryCode() : DEFAULT_FACTORY_CODE;

        // 查询当月的结构排产配置（添加is_delete和工厂过滤）
        List<MpCxCapacityConfiguration> allocations = capacityConfigurationMapper.selectByYearAndMonth(factoryCode, year, month);
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
     * 加载结构整车配置
     *
     * <p>从 t_mdm_structure_tread_config 表获取每个结构的整车胎面条数配置
     * <p>用于按车分配的计算：需要的车数 = 待排产量 / 胎面整车条数
     */
    private void loadStructureTreadConfigs(ScheduleContextVo context) {
        List<MdmStructureTreadConfig> treadConfigs = structureShiftCapacityMapper.selectList(
                new LambdaQueryWrapper<MdmStructureTreadConfig>()
                        .eq(MdmStructureTreadConfig::getIsDelete, "0"));
        context.setStructureTreadConfigs(treadConfigs);

        // 构建结构-整车条数映射
        Map<String, Integer> structureTreadCountMap = new HashMap<>();
        for (MdmStructureTreadConfig config : treadConfigs) {
            if (config.getStructureCode() != null && config.getTreadCount() != null) {
                structureTreadCountMap.put(config.getStructureCode(), config.getTreadCount());
            }
        }
        context.setStructureTreadCountMap(structureTreadCountMap);
        log.info("加载结构整车配置 {} 条，共 {} 个结构", treadConfigs.size(), structureTreadCountMap.size());
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
        // 同时设置列表，供 BalancingService 使用
        context.setStructureLhRatios(getStructureLhRatios());
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

        // 获取当前天的班次配置（用于获取硫化任务的班次计划量）
        List<CxShiftConfig> currentDayShifts = getCurrentDayShifts(context);

        // 计算成型余量映射（按硫化任务的班次计划量分配库存）
        Map<String, Integer> formingRemainderMap = new HashMap<>();
        Map<String, Integer> materialStockMap = calculateFormingRemainderMap(
                context.getMaterials(),
                monthSurplusMap,
                context.getStocks(),
                context.getLhScheduleResults(),
                currentDayShifts,
                formingRemainderMap);
        context.setFormingRemainderMap(formingRemainderMap);
        context.setMaterialStockMap(materialStockMap);
        log.info("计算成型余量映射 {} 条，物料库存分配 {} 条", formingRemainderMap.size(), materialStockMap.size());
    }

    /**
     * 获取当前排程日期的班次配置
     */
    private List<CxShiftConfig> getCurrentDayShifts(ScheduleContextVo context) {
        LocalDate scheduleDate = context.getScheduleDate();
        List<CxShiftConfig> allShifts = context.getShiftConfigList();
        if (allShifts == null || scheduleDate == null) {
            return new ArrayList<>();
        }
        // 获取第1天的班次配置
        return allShifts.stream()
                .filter(s -> s.getScheduleDay() != null && s.getScheduleDay() == 1)
                .collect(Collectors.toList());
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

    /**
     * 获取结构硫化配比列表
     */
    private List<MdmStructureLhRatio> getStructureLhRatios() {
        try {
            return structureLhRatioMapper.selectList(null);
        } catch (Exception e) {
            log.error("获取结构硫化配比列表失败", e);
            return Collections.emptyList();
        }
    }

    // ==================== 私有方法：成型余量计算 ====================

    /**
     * 计算成型余量映射
     *
     * <p>功能：
     * <ul>
     *   <li>按硫化任务的班次计划量作为需求比例，分配共用胎胚库存</li>
     *   <li>最后一条物料用倒扣形式（总库存 - 已分配）</li>
     * </ul>
     *
     * @param materials          物料信息列表
     * @param monthSurplusMap    月度计划硫化余量映射
     * @param stocks             胎胚库存列表
     * @param lhScheduleResults  硫化排程结果（用于获取班次计划量作为需求比例）
     * @param dayShifts          当前天班次配置
     * @param formingRemainderMap 成型余量映射（输出参数）
     * @return 物料库存映射（按物料编码分配库存）
     */
    private Map<String, Integer> calculateFormingRemainderMap(
            List<MdmMaterialInfo> materials,
            Map<String, MdmMonthSurplus> monthSurplusMap,
            List<CxStock> stocks,
            List<LhScheduleResult> lhScheduleResults,
            List<CxShiftConfig> dayShifts,
            Map<String, Integer> formingRemainderMap) {

        // 用于返回的物料库存映射
        Map<String, Integer> materialStockMap = new HashMap<>();

        try {
            // 按硫化任务维度分配库存，共用胎胚按硫化任务需求比例分配
            materialStockMap = allocateStockByMaterialRatio(stocks, lhScheduleResults, dayShifts);
            log.debug("按硫化任务维度分配胎胚库存 {} 条", materialStockMap.size());

            // 计算成型余量
            for (Map.Entry<String, MdmMonthSurplus> entry : monthSurplusMap.entrySet()) {
                String materialCode = entry.getKey();
                MdmMonthSurplus surplus = entry.getValue();

                int vulcanizingRemainder = surplus.getPlanSurplusQty() != null
                        ? surplus.getPlanSurplusQty().intValue() : 0;
                int embryoStock = materialStockMap.getOrDefault(materialCode, 0);
                int formingRemainder = Math.max(0, vulcanizingRemainder - embryoStock);

                formingRemainderMap.put(materialCode, formingRemainder);
            }

            log.info("计算成型余量映射完成，共 {} 条", formingRemainderMap.size());

        } catch (Exception e) {
            log.error("计算成型余量映射失败", e);
        }

        return materialStockMap;
    }

    /**
     * 从硫化记录获取对应班次的计划量
     *
     * @param lhResult   硫化记录
     * @param dayShifts  当前天班次配置
     * @return 对应班次的硫化计划量
     */
    private int getShiftPlanQtyFromLhResult(LhScheduleResult lhResult, List<CxShiftConfig> dayShifts) {
        if (dayShifts == null || dayShifts.isEmpty()) {
            return lhResult.getDailyPlanQty() != null ? lhResult.getDailyPlanQty() : 0;
        }

        for (CxShiftConfig shiftConfig : dayShifts) {
            String classField = shiftConfig.getClassField();
            if (classField != null && classField.startsWith("CLASS")) {
                try {
                    int classIndex = Integer.parseInt(classField.substring(5));
                    Integer planQty = getClassPlanQtyByIndex(lhResult, classIndex);
                    if (planQty != null && planQty > 0) {
                        return planQty;
                    }
                } catch (NumberFormatException e) {
                    log.warn("无法解析班次字段: {}", classField);
                }
            }
        }

        return lhResult.getDailyPlanQty() != null ? lhResult.getDailyPlanQty() : 0;
    }

    /**
     * 根据班次索引获取硫化记录的计划量
     */
    private Integer getClassPlanQtyByIndex(LhScheduleResult lhResult, int classIndex) {
        switch (classIndex) {
            case 1: return lhResult.getClass1PlanQty();
            case 2: return lhResult.getClass2PlanQty();
            case 3: return lhResult.getClass3PlanQty();
            case 4: return lhResult.getClass4PlanQty();
            case 5: return lhResult.getClass5PlanQty();
            case 6: return lhResult.getClass6PlanQty();
            case 7: return lhResult.getClass7PlanQty();
            case 8: return lhResult.getClass8PlanQty();
            default: return null;
        }
    }

    /**
     * 按比例分配胎胚库存到硫化任务
     *
     * <p>当一个胎胚被多个物料共用时，按各硫化任务的班次计划量需求比例分配库存
     * <p>最后一条硫化任务用倒扣形式（总库存 - 已分配）
     */
    private Map<String, Integer> allocateStockByMaterialRatio(
            List<CxStock> stocks,
            List<LhScheduleResult> lhScheduleResults,
            List<CxShiftConfig> dayShifts) {

        Map<String, Integer> materialStockMap = new HashMap<>();

        for (CxStock stock : stocks) {
            String embryoCode = stock.getEmbryoCode();
            if (embryoCode == null) {
                continue;
            }

            int totalStock = stock.getEffectiveStock();
            if (totalStock <= 0) {
                continue;
            }

            // 找到该胎胚对应的所有硫化任务
            List<LhScheduleResult> relatedTasks = new ArrayList<>();
            for (LhScheduleResult lh : lhScheduleResults) {
                if (embryoCode.equals(lh.getEmbryoCode())) {
                    relatedTasks.add(lh);
                }
            }

            if (relatedTasks.isEmpty()) {
                // 胎胚没有对应的硫化任务，跳过
                log.debug("胎胚 {} 没有对应的硫化任务，跳过", embryoCode);
                continue;
            }

            if (relatedTasks.size() == 1) {
                // 胎胚只对应一个硫化任务，直接分配全部库存
                LhScheduleResult task = relatedTasks.get(0);
                String taskKey = String.valueOf(task.getId());
                materialStockMap.merge(taskKey, totalStock, Integer::sum);
                log.debug("胎胚 {} 只对应硫化任务 {}，分配库存 {}", embryoCode, taskKey, totalStock);
            } else {
                // 胎胚对应多个硫化任务，按硫化任务需求比例分配
                int totalDemand = 0;
                List<TaskDemand> taskDemands = new ArrayList<>();
                for (LhScheduleResult lh : relatedTasks) {
                    int demand = getShiftPlanQtyFromLhResult(lh, dayShifts);
                    taskDemands.add(new TaskDemand(lh.getId(), demand));
                    totalDemand += demand;
                }

                if (totalDemand == 0) {
                    // 总需求为0，平均分配
                    int avgStock = totalStock / taskDemands.size();
                    for (TaskDemand td : taskDemands) {
                        materialStockMap.merge(td.taskKey, avgStock, Integer::sum);
                    }
                    log.debug("胎胚 {} 对应多个硫化任务但总需求为0，平均分配库存 {}", embryoCode, avgStock);
                } else {
                    // 按比例分配，最后一条用倒扣
                    int allocatedTotal = 0;

                    for (int i = 0; i < taskDemands.size(); i++) {
                        TaskDemand td = taskDemands.get(i);
                        int allocatedStock;

                        if (i == taskDemands.size() - 1) {
                            // 最后一个硫化任务分配剩余库存（倒扣）
                            allocatedStock = totalStock - allocatedTotal;
                        } else {
                            // 按比例分配
                            allocatedStock = (int) ((long) totalStock * td.demand / totalDemand);
                        }

                        materialStockMap.merge(td.taskKey, allocatedStock, Integer::sum);
                        allocatedTotal += allocatedStock;

                        log.debug("胎胚 {} 共用分配：硫化任务 {} 需求 {}，分配库存 {}",
                                embryoCode, td.taskKey, td.demand, allocatedStock);
                    }
                }
            }
        }

        return materialStockMap;
    }

    /**
     * 硫化任务需求（内部类）
     */
    private static class TaskDemand {
        String taskKey;    // 硫化任务唯一键：lhId
        int demand;

        TaskDemand(Long lhId, int demand) {
            this.taskKey = String.valueOf(lhId);
            this.demand = demand;
        }
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

            // 构建已收尾的组合键集合：物料编码 + 胎胚编码
            // 用于过滤在机信息
            Set<String> completedKeys = new HashSet<>();
            for (String materialCode : completedMaterialCodes) {
                String embryoCode = materialToEmbryoMap.get(materialCode);
                if (materialCode != null && embryoCode != null) {
                    completedKeys.add(materialCode + "|" + embryoCode);
                }
            }

            List<MdmCxMachineOnlineInfo> filteredOnlineInfos = context.getOnlineInfos().stream()
                    .filter(info -> {
                        // 使用物料编码 + 胎胚编码组合键
                        String materialCode = info.getMesMaterialCode();
                        String embryoSpec = info.getEmbryoSpec();
                        String combinedKey = materialCode + "|" + embryoSpec;
                        // 如果组合键在已收尾集合中，则过滤掉
                        if (combinedKey != null && !combinedKey.equals("|") && completedKeys.contains(combinedKey)) {
                            log.debug("过滤在机信息：机台={}，组合键={}，对应物料已收尾",
                                    info.getCxCode(), combinedKey);
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
        // 使用物料编码 + 胎胚编码组合键
        if (context.getOnlineInfos() != null) {
            Map<String, Set<String>> machineOnlineEmbryoMap = new HashMap<>();
            for (MdmCxMachineOnlineInfo onlineInfo : context.getOnlineInfos()) {
                String cxCode = onlineInfo.getCxCode();
                String materialCode = onlineInfo.getMesMaterialCode();
                String embryoSpec = onlineInfo.getEmbryoSpec();
                String combinedKey = materialCode + "|" + embryoSpec;
                if (cxCode != null && combinedKey != null && !combinedKey.equals("|")) {
                    machineOnlineEmbryoMap.computeIfAbsent(cxCode, k -> new HashSet<>()).add(combinedKey);
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
