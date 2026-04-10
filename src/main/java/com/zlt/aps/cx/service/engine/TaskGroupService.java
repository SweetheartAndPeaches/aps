package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.entity.CxMaterialEnding;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.cx.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.cx.service.engine.ProductionCalculator;
import com.zlt.aps.cx.service.engine.ScheduleDayTypeHelper;
import com.zlt.aps.cx.service.engine.ScheduleDayTypeHelper.DayFlagInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 任务分组服务
 * 
 * <p>负责 S5.2 排程分类与余量计算：
 * <ul>
 *   <li>确定当前排程班次</li>
 *   <li>获取续作任务列表</li>
 *   <li>计算成型余量</li>
 *   <li>判断收尾任务</li>
 *   <li>生成续作任务、试制任务、新增任务列表</li>
 * </ul>
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskGroupService {

    private final ProductionCalculator productionCalculator;

    private final ScheduleDayTypeHelper scheduleDayTypeHelper;

    /** 收尾阈值：成型余量低于此值视为紧急收尾 */
    private static final int ENDING_SURPLUS_THRESHOLD = 400;
    
    /** 收尾判断天数：未来多少天内判断收尾 */
    private static final int ENDING_DAYS_THRESHOLD = 10;
    
    /** 紧急收尾天数：未来多少天内视为紧急收尾 */
    private static final int URGENT_ENDING_DAYS = 3;

    /**
     * 任务分组结果
     */
    @lombok.Data
    public static class TaskGroupResult {
        /** 续作任务：当前机台在产的胎胚 */
        private List<CoreScheduleAlgorithmService.DailyEmbryoTask> continueTasks = new ArrayList<>();
        /** 试制任务：试制/量试任务 */
        private List<CoreScheduleAlgorithmService.DailyEmbryoTask> trialTasks = new ArrayList<>();
        /** 新增任务：非续作、非试制的常规任务 */
        private List<CoreScheduleAlgorithmService.DailyEmbryoTask> newTasks = new ArrayList<>();
    }

    /**
     * S5.2 排程分类与余量计算
     *
     * <p>将硫化任务分为三类：
     * <ul>
     *   <li>续作任务：当前机台在产的胎胚，需要继续生产</li>
     *   <li>试制任务：试制/量试任务</li>
     *   <li>新增任务：非续作、非试制的常规任务</li>
     * </ul>
     *
     * @param context                   排程上下文
     * @param machineOnlineEmbryoMap    机台在产胎胚映射
     * @param scheduleDate              排程日期
     * @param dayShifts                当前天的班次配置列表（用于获取对应班次的硫化计划量）
     * @return 任务分组结果
     */
    public TaskGroupResult groupTasks(
            ScheduleContextVo context,
            Map<String, Set<String>> machineOnlineEmbryoMap,
            LocalDate scheduleDate,
            List<CxShiftConfig> dayShifts) {

        TaskGroupResult result = new TaskGroupResult();

        // 获取硫化排程结果
        List<LhScheduleResult> lhScheduleResults = context.getLhScheduleResults();
        if (lhScheduleResults == null || lhScheduleResults.isEmpty()) {
            log.warn("硫化排程结果为空，无法分组任务");
            return result;
        }
        log.info("任务分组开始：共 {} 条硫化记录", lhScheduleResults.size());

        // 构建基础映射
        Map<String, MdmMaterialInfo> materialMap = buildMaterialMap(context);
        Map<String, CxStock> stockMap = buildStockMap(context);

        // 确保机台在产映射非空
        if (machineOnlineEmbryoMap == null) {
            machineOnlineEmbryoMap = new HashMap<>();
        }

        // 计算开产日标识（分组内所有任务共用）
        boolean isOpeningDay = scheduleDayTypeHelper.isOpeningDay(scheduleDate);

        // 直接遍历每条硫化记录，为每条记录创建独立的任务
        int skippedNullEmbryo = 0;
        int skippedNullTask = 0;
        for (LhScheduleResult lhResult : lhScheduleResults) {
            if (lhResult.getEmbryoCode() == null) {
                skippedNullEmbryo++;
                continue;
            }

            // 为每条硫化记录构建独立任务
            CoreScheduleAlgorithmService.DailyEmbryoTask task = buildSingleTask(
                    lhResult, materialMap, stockMap, context, dayShifts);
            if (task == null) {
                skippedNullTask++;
                continue;
            }

            String materialCode = lhResult.getMaterialCode();
            String embryoCode = lhResult.getEmbryoCode();

            // 判断任务类型
            // 1. 续作任务：当前机台在产的胎胚
            // 使用物料编码 + 胎胚编码组合键匹配
            List<String> continueMachineCodes = findContinueMachines(materialCode, embryoCode, machineOnlineEmbryoMap);
            boolean isContinueTask = !continueMachineCodes.isEmpty();

            // 2. 试制任务：根据施工阶段判断
            // constructionStage: 01-试制, 02-量试, 03-正式
            // 01-试制 → 试制任务
            // 02-量试 → 量试任务（有独立标识，但归入新增任务处理）
            String constructionStage = lhResult.getConstructionStage();
            boolean isTrialTask = "01".equals(constructionStage);
            boolean isProductionTrial = "02".equals(constructionStage);

            // 设置任务属性
            task.setIsContinueTask(isContinueTask);
            task.setIsTrialTask(isTrialTask);
            task.setIsProductionTrial(isProductionTrial);
            task.setContinueMachineCodes(continueMachineCodes);
            task.setIsFirstTask(!isContinueTask && !isTrialTask && !isProductionTrial);

            // 计算收尾相关属性
            calculateEndingInfo(task, context, scheduleDate);

            // S5.3.1 计算待排产量
            calculatePlannedProduction(task, context, scheduleDate, isOpeningDay);
            // S5.3.2 收尾余量处理
            handleEndingRemainder(task, context, isOpeningDay);
            // S5.3.3 开停产特殊处理
            handleOpeningClosingDay(task, context, dayShifts);

            // 分组
            if (isContinueTask) {
                result.getContinueTasks().add(task);
            } else if (isTrialTask) {
                result.getTrialTasks().add(task);
            } else {
                result.getNewTasks().add(task);
            }
        }

        log.info("任务分组完成：硫化记录{}条，跳过(embryoCode为null):{}，跳过(task为null):{}，续作:{}，试制:{}，新增:{}",
                lhScheduleResults.size(), skippedNullEmbryo, skippedNullTask,
                result.getContinueTasks().size(), result.getTrialTasks().size(), result.getNewTasks().size());

        return result;
    }

    /**
     * 构建物料映射
     */
    private Map<String, MdmMaterialInfo> buildMaterialMap(ScheduleContextVo context) {
        Map<String, MdmMaterialInfo> map = new HashMap<>();
        if (context.getMaterials() != null) {
            for (MdmMaterialInfo material : context.getMaterials()) {
                // 同时用 MATERIAL_CODE 和 EMBRYO_CODE 作为索引
                if (material.getMaterialCode() != null) {
                    map.put(material.getMaterialCode(), material);
                }
                if (material.getEmbryoCode() != null) {
                    map.put(material.getEmbryoCode(), material);
                }
            }
        }
        log.debug("物料映射构建完成，共 {} 条物料信息", map.size());
        return map;
    }

    /**
     * 构建库存映射
     */
    private Map<String, CxStock> buildStockMap(ScheduleContextVo context) {
        Map<String, CxStock> map = new HashMap<>();
        if (context.getStocks() != null) {
            for (CxStock stock : context.getStocks()) {
                map.put(stock.getEmbryoCode(), stock);
            }
        }
        return map;
    }

    /**
     * 查找续作机台
     *
     * <p>使用物料编码 + 胎胚编码组合键匹配：
     * - 机台在产: mesMaterialCode + embryoSpec (格式: materialCode|embryoCode)
     * - 硫化任务: materialCode + embryoCode
     */
    private List<String> findContinueMachines(String materialCode, String embryoCode, Map<String, Set<String>> machineOnlineEmbryoMap) {
        List<String> machineCodes = new ArrayList<>();
        // 组合键格式与机台在产映射一致: materialCode|embryoCode
        String combinedKey = materialCode + "|" + embryoCode;
        for (Map.Entry<String, Set<String>> entry : machineOnlineEmbryoMap.entrySet()) {
            if (entry.getValue().contains(combinedKey)) {
                machineCodes.add(entry.getKey());
            }
        }
        return machineCodes;
    }

    // ==================== S5.3 任务属性计算 ====================

    /**
     * S5.3.1 计算待排产量
     */
    private void calculatePlannedProduction(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                            ScheduleContextVo context,
                                            LocalDate scheduleDate,
                                            boolean isOpeningDay) {
        int plannedProduction;
        if (task.getIsEndingTask() != null && task.getIsEndingTask()) {
            // 收尾任务：按胎胚库存计算，剩余库存全部排产
            plannedProduction = task.getEndingSurplusQty() != null ? task.getEndingSurplusQty() : 0;
        } else if (task.getRemainingQuantity() != null) {
            // 正常任务：取月计划剩余量
            plannedProduction = task.getRemainingQuantity();
        } else {
            plannedProduction = 0;
        }

        // 开产日：如果胎胚库存在合理范围内（≥24小时库存），则减产
        if (isOpeningDay) {
            int tripCapacity = getTripCapacity(task.getStructureName(), context);
            if (plannedProduction > tripCapacity) {
                plannedProduction -= tripCapacity;
            }
        }

        task.setPlannedProduction(plannedProduction);

        // 根据计划量计算硫化机台数（向上取整）
        int tripCapacity = getTripCapacity(task.getStructureName(), context);
        int vulcanizeMachineCount = productionCalculator.roundToVehicle(plannedProduction, tripCapacity);
        task.setVulcanizeMachineCount(vulcanizeMachineCount);
    }

    /**
     * S5.3.2 收尾余量处理
     */
    private void handleEndingRemainder(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                        ScheduleContextVo context,
                                        boolean isOpeningDay) {
        if (task.getIsEndingTask() == null || !task.getIsEndingTask()) {
            return;
        }

        Integer surplusQty = task.getEndingSurplusQty();
        if (surplusQty == null || surplusQty <= 0) {
            return;
        }

        Integer currentStock = task.getCurrentStock();
        if (currentStock == null) {
            currentStock = 0;
        }

        // 收尾任务当天可排产量 = min(胎胚库存, 当天计划量)
        int availableQty = Math.min(currentStock, task.getPlannedProduction() != null ? task.getPlannedProduction() : 0);

        if (availableQty > 0) {
            int tripCapacity = getTripCapacity(task.getStructureName(), context);
            int cars = productionCalculator.roundToVehicle(availableQty, tripCapacity);
            task.setPlannedProduction(cars * tripCapacity);
            task.setEndingSurplusQty(surplusQty - task.getPlannedProduction());
        }
    }

    /**
     * S5.3.3 开停产特殊处理
     */
    private void handleOpeningClosingDay(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                         ScheduleContextVo context,
                                         List<CxShiftConfig> dayShifts) {
        LocalDate scheduleDate = context.getCurrentScheduleDate();

        // 停产日：当天产量设为0
        if (scheduleDayTypeHelper.isStopDay(scheduleDate)) {
            task.setPlannedProduction(0);
            task.setVulcanizeMachineCount(0);
            return;
        }

        // 停产标识日：当天产量按胎胚库存取整
        if (scheduleDayTypeHelper.isStopFlagDay(scheduleDate)) {
            Integer currentStock = task.getCurrentStock();
            if (currentStock != null && currentStock > 0) {
                int tripCapacity = getTripCapacity(task.getStructureName(), context);
                int cars = productionCalculator.roundToVehicle(currentStock, tripCapacity);
                int qty = Math.min(cars * tripCapacity, currentStock);
                task.setPlannedProduction(qty);
                task.setVulcanizeMachineCount(cars);
            } else {
                task.setPlannedProduction(0);
                task.setVulcanizeMachineCount(0);
            }
        }
    }

    /**
     * 获取班次换胎产能
     */
    private int getTripCapacity(String structureName, ScheduleContextVo context) {
        return productionCalculator.getTripCapacity(structureName, context);
    }

    /**
     * 为单条硫化记录构建任务
     *
     * <p>每条硫化记录作为独立任务，不再按胎胚合并
     *
     * @param lhResult            硫化记录
     * @param materialMap         物料映射
     * @param stockMap             库存映射
     * @param context              排程上下文
     * @param currentShiftConfigs  当前班次配置列表
     */
    private CoreScheduleAlgorithmService.DailyEmbryoTask buildSingleTask(
            LhScheduleResult lhResult,
            Map<String, MdmMaterialInfo> materialMap,
            Map<String, CxStock> stockMap,
            ScheduleContextVo context,
            List<CxShiftConfig> currentShiftConfigs) {

        String embryoCode = lhResult.getEmbryoCode();
        String materialCode = lhResult.getMaterialCode();
        if (embryoCode == null) {
            log.warn("buildSingleTask跳过：embryoCode为null，materialCode={}", materialCode);
            return null;
        }

        // 获取硫化需求量（根据当前班次配置获取对应的CLASS计划量）
        int vulcanizeDemand = getShiftPlanQty(lhResult, currentShiftConfigs);

        if (vulcanizeDemand <= 0) {
            log.debug("buildSingleTask跳过：硫化需求为0，embryoCode={}, materialCode={}", embryoCode, materialCode);
            // 不返回null，因为即使需求为0也可能需要排产（比如补库存）
        }

        // 获取分配给该硫化任务的库存（按硫化任务维度分配，共用胎胚库存已按比例分配）
        int currentStock = getCurrentStock(context, lhResult.getId());
        log.info("硫化任务排量: embryoCode={}, vulcanizeDemand={}, currentStock={}", 
                embryoCode, vulcanizeDemand, currentStock);

        // 获取物料信息
        MdmMaterialInfo material = materialMap.get(embryoCode);
        if (material == null) {
            log.debug("buildSingleTask跳过：物料信息为空，embryoCode={}", embryoCode);
        }

        String structureName = material != null ? material.getStructureName() : lhResult.getStructureName();

        // 计算日需求量
        int dailyDemand = calculateDailyDemand(vulcanizeDemand, currentStock, structureName, context);

        // 构建任务
        CoreScheduleAlgorithmService.DailyEmbryoTask task = new CoreScheduleAlgorithmService.DailyEmbryoTask();
        task.setLhId(lhResult.getId());  // 设置硫化任务ID，用于关联库存分配
        task.setEmbryoCode(embryoCode);
        task.setVulcanizeDemand(vulcanizeDemand);
        task.setCurrentStock(currentStock);

        if (material != null) {
            task.setMaterialDesc(material.getMaterialDesc());
            task.setMainMaterialDesc(material.getEmbryoDesc());
            task.setStructureName(material.getStructureName());
            // 设置成品物料编码（用于判断主销产品）
            task.setMaterialCode(material.getMaterialCode());
        } else {
            task.setMaterialDesc(embryoCode);
            task.setMainMaterialDesc(embryoCode);
            task.setStructureName(structureName);
        }

        task.setDemandQuantity(dailyDemand);
        task.setAssignedQuantity(0);
        task.setRemainingQuantity(dailyDemand);

        // 是否主销产品
        String mainProductCode = task.getMaterialCode();
        task.setIsMainProduct(context.getMainProductCodes() != null
                && mainProductCode != null
                && context.getMainProductCodes().contains(mainProductCode));

        // 硫化机台数和模数
        // 一条LhScheduleResult = 一台硫化机，机台数直接为1
        // 模数取硫化任务的 mouldQty
        task.setVulcanizeMachineCount(1);
        task.setVulcanizeMoldCount(lhResult.getMouldQty() != null ? lhResult.getMouldQty() : 1);

        // 计算库存可供硫化时长（基于当前库存，成型产出未知不做班次动态推算）
        calculateStockHours(task, lhResult, currentStock, context);

        return task;
    }

    /**
     * 根据班次配置获取硫化记录对应班次的计划量
     *
     * <p>硫化有8个班次(CLASS1-CLASS8)，成型分3天排程
     * 根据当前班次配置的 classField 字段获取对应的硫化班次计划量
     *
     * @param lhResult            硫化记录
     * @param currentShiftConfigs 当前班次配置列表
     * @return 对应班次的硫化计划量
     */
    private int getShiftPlanQty(LhScheduleResult lhResult, List<CxShiftConfig> currentShiftConfigs) {
        if (currentShiftConfigs == null || currentShiftConfigs.isEmpty()) {
            // 没有班次配置，返回0
            return 0;
        }

        // 获取班次配置中的 classField (如 CLASS1, CLASS2, ..., CLASS8)
        for (CxShiftConfig shiftConfig : currentShiftConfigs) {
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

        // 取不到就是0
        return 0;
    }

    /**
     * 根据班次索引获取硫化记录的计划量
     *
     * @param lhResult   硫化记录
     * @param classIndex 班次索引 (1-8)
     * @return 计划量
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
     * 获取任务可用的当前库存
     *
     * <p>优先级：
     * <ol>
     *   <li>硫化记录上记录的库存（lhResult.embryoStock）</li>
     *   <li>CxStock 的有效库存 = stockNum - overTimeStock - badNum + modifyNum</li>
     * </ol>
     *
     * @param lhResult   硫化记录
     * @param stockMap   embryoCode → CxStock 的映射
     * @param embryoCode 胎胚编码
     * @return 当前可用库存数量
     */
    private int getCurrentStock(LhScheduleResult lhResult, Map<String, CxStock> stockMap, String embryoCode) {
        Integer embryoStock = lhResult.getEmbryoStock();
        if (embryoStock != null) {
            return embryoStock;
        }
        CxStock stock = stockMap.get(embryoCode);
        return stock != null ? stock.getEffectiveStock() : 0;
    }

    /**
     * 获取分配给该硫化任务的库存
     *
     * <p>库存已按硫化任务维度分配，共用胎胚库存按硫化任务需求比例分配
     * 使用硫化任务的唯一标识 (lhId) 获取当前库存
     *
     * @param context 排程上下文
     * @param lhId    硫化任务ID
     * @return 当前库存数量
     */
    private int getCurrentStock(ScheduleContextVo context, Long lhId) {
        if (lhId == null) {
            return 0;
        }
        Map<String, Integer> materialStockMap = context.getMaterialStockMap();
        if (materialStockMap == null) {
            log.warn("materialStockMap 为空，无法获取分配给硫化任务 {} 的库存", lhId);
            return 0;
        }
        // 使用硫化任务的唯一标识获取库存
        String taskKey = String.valueOf(lhId);
        return materialStockMap.getOrDefault(taskKey, 0);
    }

    /** 库存高预警阈值（小时），可配置 */
    private static final int STOCK_HIGH_HOURS_THRESHOLD = 18;

    /** 库存低预警阈值（小时），低于此值优先排产 */
    private static final BigDecimal STOCK_LOW_HOURS_THRESHOLD = new BigDecimal("4");

    /** 一天的秒数 */
    private static final int SECONDS_PER_DAY = 24 * 60 * 60;

    /**
     * 计算库存可供硫化时长
     *
     * 任务分组阶段：成型产出未知（本次排程的结果），无法按班次动态推算
     * 因此只基于当前库存计算初始的库存可供硫化时长
     *
     * 公式：
     *   日硫化量 → 从 MonthPlanProductLhCapacityVo 获取
     *   单胎单模硫化时长(秒) = 24 × 60 × 60 / 日硫化量
     *   stockHours = 库存 × 单胎单模硫化时长 / 任务的模数 / 3600 (转为小时)
     *
     * @param task         胎胚任务
     * @param lhResult     硫化排程结果
     * @param currentStock 当前胎胚库存
     * @param context      排程上下文
     */
    private void calculateStockHours(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            LhScheduleResult lhResult,
            int currentStock,
            ScheduleContextVo context) {

        // 1. 从 materialLhCapacityMap 获取该物料的日硫化量
        Map<String, MonthPlanProductLhCapacityVo> lhCapacityMap = context.getMaterialLhCapacityMap();
        Integer dailyLhCapacity = null;
        if (lhCapacityMap != null) {
            String embryoCode = task.getEmbryoCode();
            MonthPlanProductLhCapacityVo capacityVo = lhCapacityMap.get(embryoCode);
            if (capacityVo != null) {
                if (capacityVo.getDayVulcanizationQty() != null && capacityVo.getDayVulcanizationQty() > 0) {
                    dailyLhCapacity = capacityVo.getDayVulcanizationQty();
                } else if (capacityVo.getStandardCapacity() != null && capacityVo.getStandardCapacity() > 0) {
                    dailyLhCapacity = capacityVo.getStandardCapacity();
                }
            }
        }

        // 如果取不到日硫化量，尝试从lhResult的硫化时长和模数计算
        if (dailyLhCapacity == null || dailyLhCapacity <= 0) {
            if (lhResult != null && lhResult.getLhTime() != null && lhResult.getLhTime() > 0
                    && lhResult.getMouldQty() != null && lhResult.getMouldQty() > 0) {
                int lhTimeSeconds = lhResult.getLhTime();
                int mouldQty = lhResult.getMouldQty();
                dailyLhCapacity = (SECONDS_PER_DAY / lhTimeSeconds) * mouldQty;
            }
        }

        // 仍然取不到，无法计算
        if (dailyLhCapacity == null || dailyLhCapacity <= 0) {
            log.warn("无法获取物料 {} 的日硫化量，stockHours 无法计算", task.getEmbryoCode());
            task.setStockHours(BigDecimal.ZERO);
            task.setIsStockHighWarning(false);
            return;
        }

        // 2. 单胎单模硫化时长(秒) = 24×60×60 / 日硫化量
        BigDecimal singleTireMoldSeconds = BigDecimal.valueOf(SECONDS_PER_DAY)
                .divide(BigDecimal.valueOf(dailyLhCapacity), 2, BigDecimal.ROUND_HALF_UP);

        // 3. 任务的模数
        Integer taskMoldQty = task.getVulcanizeMoldCount();
        if (taskMoldQty == null || taskMoldQty <= 0) {
            taskMoldQty = lhResult != null && lhResult.getMouldQty() != null ? lhResult.getMouldQty() : 1;
        }

        // 4. 基于当前库存计算库存可供硫化时长
        // stockHours = 库存 × 单胎单模硫化时长 / 任务模数 / 3600 (转小时)
        BigDecimal stockHours = BigDecimal.valueOf(currentStock)
                .multiply(singleTireMoldSeconds)
                .divide(BigDecimal.valueOf(taskMoldQty), 2, BigDecimal.ROUND_HALF_UP)
                .divide(BigDecimal.valueOf(3600), 2, BigDecimal.ROUND_HALF_UP);

        task.setStockHours(stockHours);

        // 库存预警：超过18小时标记为高库存
        boolean isHighStock = stockHours.compareTo(BigDecimal.valueOf(STOCK_HIGH_HOURS_THRESHOLD)) > 0;
        task.setIsStockHighWarning(isHighStock);

        log.debug("物料 {} stockHours计算: 日硫化量={}, 单胎单模时长={}s, 模数={}, 库存={}, 库存可供时长={}h",
                task.getEmbryoCode(), dailyLhCapacity, singleTireMoldSeconds, taskMoldQty, currentStock, stockHours);
    }

    /**
     * 计算日需求量
     *
     * <p>简化实现：直接返回硫化需求量（排量）。
     * 更精细的需求拆分（考虑历史排产量、班次分配等）在续作/新增任务处理器中进行。
     *
     * @param totalVulcanizeDemand 硫化总需求（排量）
     * @param currentStock         当前库存（未使用，为扩展预留）
     * @param structureName        结构名称（未使用，为扩展预留）
     * @param context              排程上下文（未使用，为扩展预留）
     * @return 日需求量
     */
    private int calculateDailyDemand(
            int totalVulcanizeDemand,
            int currentStock,
            String structureName,
            ScheduleContextVo context) {

        // 简化计算：日需求量 = 硫化需求量
        // 更详细的计算在续作任务处理器中进行
        return totalVulcanizeDemand;
    }



    /**
     * 计算收尾相关信息
     *
     * <p>包括：
     * <ul>
     *   <li>成型余量</li>
     *   <li>是否收尾任务</li>
     *   <li>是否10天内收尾</li>
     *   <li>是否3天内收尾（紧急）</li>
     *   <li>收尾日</li>
     * </ul>
     */
    public void calculateEndingInfo(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextVo context,
            LocalDate scheduleDate) {

        String embryoCode = task.getEmbryoCode();

        // 获取成型余量（从预计算的映射中获取）
        Map<String, Integer> formingRemainderMap = context.getFormingRemainderMap();
        Integer formingRemainder = null;
        Integer vulcanizeSurplusQty = null;

        // 从月计划余量获取硫化余量
        if (context.getMonthSurplusMap() != null) {
            MdmMonthSurplus monthSurplus = context.getMonthSurplusMap().get(embryoCode);
            if (monthSurplus != null && monthSurplus.getPlanSurplusQty() != null) {
                vulcanizeSurplusQty = monthSurplus.getPlanSurplusQty().intValue();
            }
        }

        // 获取成型余量
        if (formingRemainderMap != null && formingRemainderMap.containsKey(embryoCode)) {
            formingRemainder = formingRemainderMap.get(embryoCode);
        } else if (vulcanizeSurplusQty != null) {
            // 成型余量 = 硫化余量 - 胎胚库存
            int currentStock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;
            formingRemainder = vulcanizeSurplusQty - currentStock;
        }

        task.setVulcanizeSurplusQty(vulcanizeSurplusQty);
        task.setEndingSurplusQty(formingRemainder);

        // 判断是否收尾任务（成型余量 <= 0）
        boolean isEndingTask = formingRemainder != null && formingRemainder <= 0;
        task.setIsEndingTask(isEndingTask);

        // 获取收尾日（从物料收尾管理表）
        LocalDate endingDate = findEndingDate(embryoCode, context);
        task.setEndingDate(endingDate);

        if (endingDate != null) {
            int daysToEnding = (int) java.time.temporal.ChronoUnit.DAYS.between(scheduleDate, endingDate);
            task.setDaysToEnding(daysToEnding);

            // 判断是否10天内收尾
            boolean isNearEnding = daysToEnding >= 0 && daysToEnding <= ENDING_DAYS_THRESHOLD;
            task.setIsNearEnding(isNearEnding);

            // 判断是否3天内收尾（紧急）
            boolean isUrgentEnding = daysToEnding >= 0 && daysToEnding <= URGENT_ENDING_DAYS;
            task.setIsUrgentEnding(isUrgentEnding);

            if (isUrgentEnding) {
                log.info("紧急收尾任务：物料={}, 收尾日={}, 距收尾{}天", 
                        embryoCode, endingDate, daysToEnding);
            }
        }

        // 成型余量小于阈值也标记为紧急收尾
        if (formingRemainder != null && formingRemainder < ENDING_SURPLUS_THRESHOLD && formingRemainder > 0) {
            task.setIsUrgentEnding(true);
            log.info("成型余量低于阈值的收尾任务：物料={}, 成型余量={}, 阈值={}",
                    embryoCode, formingRemainder, ENDING_SURPLUS_THRESHOLD);
        }

        // 计算优先级
        task.setPriority(calculateTaskPriority(task, context));
    }

    /**
     * 查找物料收尾日
     */
    private LocalDate findEndingDate(String materialCode, ScheduleContextVo context) {
        if (context.getMaterialEndings() != null) {
            for (CxMaterialEnding ending : context.getMaterialEndings()) {
                if (materialCode.equals(ending.getMaterialCode())) {
                    return ending.getPlannedEndingDate();
                }
            }
        }
        return null;
    }

    /**
     * 计算任务优先级分数
     */
    public int calculateTaskPriority(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextVo context) {

        int score = 0;

        // 紧急收尾最高优先级
        if (Boolean.TRUE.equals(task.getIsUrgentEnding())) {
            score += 3000;
        }
        // 普通收尾
        else if (Boolean.TRUE.equals(task.getIsEndingTask())) {
            score += 2000;
        }

        // 10天内收尾
        if (Boolean.TRUE.equals(task.getIsNearEnding())) {
            score += 1000;
        }

        // 试制任务
        if (Boolean.TRUE.equals(task.getIsTrialTask())) {
            score += 1500;
        }

        // 续作任务
        if (Boolean.TRUE.equals(task.getIsContinueTask())) {
            score += 800;
        }

        // 首排任务
        if (Boolean.TRUE.equals(task.getIsFirstTask())) {
            score += 500;
        }

        // 库存紧张（低库存时长 = 高优先级）
        if (task.getStockHours() != null) {
            if (task.getStockHours().compareTo(STOCK_LOW_HOURS_THRESHOLD) < 0) {
                score += 800;
            } else if (task.getStockHours().compareTo(new BigDecimal("6")) < 0) {
                score += 400;
            }
        }

        // 库存高预警（>18小时 = 低优先级，排后面）
        if (Boolean.TRUE.equals(task.getIsStockHighWarning())) {
            score -= 500;
            log.debug("胎胚 {} 库存水位过高，优先级降低500分", task.getEmbryoCode());
        }

        // 主销产品
        if (Boolean.TRUE.equals(task.getIsMainProduct())) {
            score += 200;
        }

        return score;
    }
}
