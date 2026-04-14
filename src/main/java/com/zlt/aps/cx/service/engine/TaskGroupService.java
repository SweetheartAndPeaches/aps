package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.entity.CxMaterialEnding;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMonthSurplus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 任务分组服务 — 成型排程 S5.2 阶段
 *
 * <p>将硫化需求转化为成型排程任务，按胎胚+物料维度分组，计算各任务的排产属性。
 * 核心流程：
 * <ol>
 *   <li>S5.2.1 遍历硫化排程结果，为每条记录构建 DailyEmbryoTask</li>
 *   <li>S5.2.2 分配库存（按硫化任务需求比例）</li>
 *   <li>S5.2.3 计算库存可支撑时长（stockHours）</li>
 *   <li>S5.2.4 计算收尾属性（余量、紧急度、优先级）</li>
 *   <li>S5.2.5 计算待排产量（库存对冲 × 损耗率 × 整车取整）</li>
 *   <li>S5.2.6 收尾余量处理（舍弃/按实/补车）</li>
 *   <li>S5.2.7 开停产特殊处理</li>
 *   <li>S5.2.8 试制任务双数处理</li>
 *   <li>S5.2.9 按任务类型分组返回（续作/试制/新增）</li>
 * </ol>
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskGroupService {

    // ==================== 业务阈值常量 ====================

    /** 收尾舍弃阈值：非主销产品余量≤此值时舍弃（条） */
    private static final int ENDING_DISCARD_THRESHOLD = 2;

    /** 成型余量紧急阈值：成型余量低于此值标记为紧急收尾（条） */
    private static final int ENDING_URGENT_FORMING_REMAINDER = 400;

    /** 近期收尾天数阈值（10 天内） */
    private static final int ENDING_DAYS_THRESHOLD = 10;

    /** 紧急收尾天数阈值（3 天内） */
    private static final int URGENT_ENDING_DAYS = 3;

    /** 库存低水位阈值（小时）：低于此值提升优先级 */
    private static final BigDecimal STOCK_LOW_HOURS_THRESHOLD = new BigDecimal("4");

    /** 库存中等水位阈值（小时） */
    private static final BigDecimal STOCK_MEDIUM_HOURS_THRESHOLD = new BigDecimal("6");

    /** 库存高水位阈值（小时）：超过此值降低优先级 */
    private static final int STOCK_HIGH_HOURS_THRESHOLD = 18;

    /** 一天总秒数 */
    private static final int SECONDS_PER_DAY = 24 * 60 * 60;

    /** 秒转小时的除数 */
    private static final int SECONDS_PER_HOUR = 3600;

    // ==================== 优先级分值常量 ====================

    /** 紧急收尾优先级加分 */
    private static final int PRIORITY_URGENT_ENDING = 3000;

    /** 普通收尾优先级加分 */
    private static final int PRIORITY_ENDING = 2000;

    /** 试制任务优先级加分 */
    private static final int PRIORITY_TRIAL = 1500;

    /** 近期收尾优先级加分 */
    private static final int PRIORITY_NEAR_ENDING = 1000;

    /** 续作任务优先级加分 */
    private static final int PRIORITY_CONTINUE = 800;

    /** 库存紧张优先级加分 */
    private static final int PRIORITY_STOCK_LOW = 800;

    /** 首排任务优先级加分 */
    private static final int PRIORITY_FIRST_TASK = 500;

    /** 库存高预警优先级减分 */
    private static final int PRIORITY_STOCK_HIGH_PENALTY = 500;

    /** 库存中等紧张优先级加分 */
    private static final int PRIORITY_STOCK_MEDIUM = 400;

    /** 主销产品优先级加分 */
    private static final int PRIORITY_MAIN_PRODUCT = 200;

    // ==================== 施工阶段常量 ====================

    /** 施工阶段：试制 */
    private static final String STAGE_TRIAL = "01";

    /** 施工阶段：量试 */
    private static final String STAGE_PRODUCTION_TRIAL = "02";

    // ==================== 依赖注入 ====================

    private final ProductionCalculator productionCalculator;
    private final ScheduleDayTypeHelper scheduleDayTypeHelper;

    // ==================== 内部类 ====================

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

    // ==================== 公开方法 ====================

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
     * @param context                排程上下文
     * @param machineOnlineEmbryoMap 机台在产胎胚映射
     * @param scheduleDate           排程日期
     * @param dayShifts              当前天的班次配置列表（用于获取对应班次的硫化计划量）
     * @return 任务分组结果
     */
    public TaskGroupResult groupTasks(
            ScheduleContextVo context,
            Map<String, Set<String>> machineOnlineEmbryoMap,
            LocalDate scheduleDate,
            List<CxShiftConfig> dayShifts) {

        TaskGroupResult result = new TaskGroupResult();

        List<LhScheduleResult> lhScheduleResults = context.getLhScheduleResults();
        if (lhScheduleResults == null || lhScheduleResults.isEmpty()) {
            log.warn("硫化排程结果为空，无法分组任务");
            return result;
        }
        log.info("任务分组开始：共 {} 条硫化记录", lhScheduleResults.size());

        // 构建基础映射
        Map<String, MdmMaterialInfo> materialMap = buildMaterialMap(context);
        Map<String, CxStock> stockMap = buildStockMap(context);

        if (machineOnlineEmbryoMap == null) {
            machineOnlineEmbryoMap = new HashMap<>();
        }

        // 直接遍历每条硫化记录，为每条记录创建独立的任务
        int skippedNullEmbryo = 0;
        int skippedNullTask = 0;
        for (LhScheduleResult lhResult : lhScheduleResults) {
            if (lhResult.getEmbryoCode() == null) {
                skippedNullEmbryo++;
                continue;
            }

            CoreScheduleAlgorithmService.DailyEmbryoTask task = buildSingleTask(
                    lhResult, materialMap, stockMap, context, dayShifts);
            if (task == null) {
                skippedNullTask++;
                continue;
            }

            String materialCode = lhResult.getMaterialCode();
              String embryoCode = lhResult.getMainMaterialDesc();

            // 判断任务类型
            List<String> continueMachineCodes = findContinueMachines(materialCode, embryoCode, machineOnlineEmbryoMap);
            boolean isContinueTask = !continueMachineCodes.isEmpty();

            String constructionStage = lhResult.getConstructionStage();
            boolean isTrialTask = STAGE_TRIAL.equals(constructionStage);
            boolean isProductionTrial = STAGE_PRODUCTION_TRIAL.equals(constructionStage);

            // 设置任务属性
            task.setIsContinueTask(isContinueTask);
            task.setIsTrialTask(isTrialTask);
            task.setIsProductionTrial(isProductionTrial);
            task.setContinueMachineCodes(continueMachineCodes);
            task.setIsFirstTask(!isContinueTask && !isTrialTask && !isProductionTrial);

            // S5.2.4 计算收尾属性
            calculateEndingInfo(task, context, scheduleDate);

            // S5.2.5 计算待排产量
            calculatePlannedProduction(task, context, scheduleDate);
            // S5.2.6 收尾余量处理
            handleEndingRemainder(task, context);
            // S5.2.7 停产特殊处理
            handleOpeningClosingDay(task, context, dayShifts);
            // S5.2.8 试制任务：产量必须是双数，不补整车
            if (Boolean.TRUE.equals(isTrialTask)) {
                Integer pp = task.getPlannedProduction();
                if (pp != null && pp % 2 != 0) {
                    task.setPlannedProduction(pp - 1);
                    task.setEndingExtraInventory(pp - 1);
                    log.debug("试制任务 {} 产量{}为奇数，调整为偶数{}", task.getEmbryoCode(), pp, pp - 1);
                }
            }

            // S5.2.9 分组
            if (isContinueTask) {
                result.getContinueTasks().add(task);
            } else if (isTrialTask) {
                result.getTrialTasks().add(task);
            } else {
                result.getNewTasks().add(task);
            }
        }

        log.info("任务分组完成：续作 {} 个，试制 {} 个，新增 {} 个，跳过无效胚胎 {} 个，跳过空任务 {} 个",
                result.getContinueTasks().size(),
                result.getTrialTasks().size(),
                result.getNewTasks().size(),
                skippedNullEmbryo, skippedNullTask);
        return result;
    }

    /**
     * S5.2.4 计算收尾属性
     *
     * <p>包括：成型余量、是否收尾任务、是否10天内收尾、是否3天内收尾（紧急）、收尾日
     *
     * @param task         胎胚任务
     * @param context      排程上下文
     * @param scheduleDate 排程日期
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
        if (formingRemainder != null && formingRemainder < ENDING_URGENT_FORMING_REMAINDER && formingRemainder > 0) {
            task.setIsUrgentEnding(true);
            log.info("成型余量低于阈值的收尾任务：物料={}, 成型余量={}, 阈值={}",
                    embryoCode, formingRemainder, ENDING_URGENT_FORMING_REMAINDER);
        }

        // 计算优先级
        task.setPriority(calculateTaskPriority(task, context));
    }

    /**
     * 计算任务优先级分数
     *
     * <p>优先级评分规则：
     * <ul>
     *   <li>紧急收尾: +3000</li>
     *   <li>普通收尾: +2000</li>
     *   <li>试制任务: +1500</li>
     *   <li>近期收尾(10天内): +1000</li>
     *   <li>续作任务: +800</li>
     *   <li>库存紧张(&lt;4h): +800</li>
     *   <li>首排任务: +500</li>
     *   <li>库存高预警(&gt;18h): -500</li>
     *   <li>库存中等紧张(&lt;6h): +400</li>
     *   <li>主销产品: +200</li>
     * </ul>
     *
     * @param task    胎胚任务
     * @param context 排程上下文
     * @return 优先级分数（越高越优先）
     */
    public int calculateTaskPriority(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextVo context) {

        int score = 0;

        // 紧急收尾最高优先级
        if (Boolean.TRUE.equals(task.getIsUrgentEnding())) {
            score += PRIORITY_URGENT_ENDING;
        } else if (Boolean.TRUE.equals(task.getIsEndingTask())) {
            score += PRIORITY_ENDING;
        }

        // 近期收尾
        if (Boolean.TRUE.equals(task.getIsNearEnding())) {
            score += PRIORITY_NEAR_ENDING;
        }

        // 试制任务
        if (Boolean.TRUE.equals(task.getIsTrialTask())) {
            score += PRIORITY_TRIAL;
        }

        // 续作任务
        if (Boolean.TRUE.equals(task.getIsContinueTask())) {
            score += PRIORITY_CONTINUE;
        }

        // 首排任务
        if (Boolean.TRUE.equals(task.getIsFirstTask())) {
            score += PRIORITY_FIRST_TASK;
        }

        // 库存紧张（低库存时长 = 高优先级）
        if (task.getStockHours() != null) {
            if (task.getStockHours().compareTo(STOCK_LOW_HOURS_THRESHOLD) < 0) {
                score += PRIORITY_STOCK_LOW;
            } else if (task.getStockHours().compareTo(STOCK_MEDIUM_HOURS_THRESHOLD) < 0) {
                score += PRIORITY_STOCK_MEDIUM;
            }
        }

        // 库存高预警（>18小时 = 低优先级）
        if (Boolean.TRUE.equals(task.getIsStockHighWarning())) {
            score -= PRIORITY_STOCK_HIGH_PENALTY;
            log.debug("胎胚 {} 库存水位过高，优先级降低{}分", task.getEmbryoCode(), PRIORITY_STOCK_HIGH_PENALTY);
        }

        // 主销产品
        if (Boolean.TRUE.equals(task.getIsMainProduct())) {
            score += PRIORITY_MAIN_PRODUCT;
        }

        return score;
    }

    // ==================== 私有方法 ====================

    /**
     * 构建物料映射（双索引：materialCode + embryoCode）
     *
     * @param context 排程上下文
     * @return 物料编码/胎胚编码 → 物料信息
     */
    private Map<String, MdmMaterialInfo> buildMaterialMap(ScheduleContextVo context) {
        Map<String, MdmMaterialInfo> map = new HashMap<>();
        if (context.getMaterials() != null) {
            for (MdmMaterialInfo material : context.getMaterials()) {
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
     *
     * @param context 排程上下文
     * @return 胎胚编码 → 库存信息
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
     * 机台在产 key 格式: materialCode|embryoCode
     *
     * @param materialCode          成品物料编码
     * @param embryoCode            胎胚编码
     * @param machineOnlineEmbryoMap 机台在产映射
     * @return 续作机台编码列表
     */
    private List<String> findContinueMachines(String materialCode, String embryoCode,
                                               Map<String, Set<String>> machineOnlineEmbryoMap) {
        List<String> machineCodes = new ArrayList<>();
        String combinedKey = materialCode + "|" + embryoCode;
        for (Map.Entry<String, Set<String>> entry : machineOnlineEmbryoMap.entrySet()) {
            if (entry.getValue().contains(combinedKey)) {
                machineCodes.add(entry.getKey());
            }
        }
        return machineCodes;
    }

    /**
     * 为单条硫化记录构建任务
     *
     * <p>每条硫化记录作为独立任务，不再按胎胚合并。
     * 执行 S5.2.1~S5.2.3 的属性计算。
     *
     * @param lhResult             硫化记录
     * @param materialMap          物料映射
     * @param stockMap             库存映射
     * @param context              排程上下文
     * @param currentShiftConfigs  当前班次配置列表
     * @return 构建好的胎胚任务，无效记录返回 null
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

        // 获取分配给该硫化任务的库存（按硫化任务维度分配，共用胎胚库存已按比例分配）
        int currentStock = getCurrentStock(context, lhResult.getId());
        log.info("硫化任务排量: embryoCode={}, vulcanizeDemand={}, currentStock={}",
                embryoCode, vulcanizeDemand, currentStock);

        // 获取物料信息
        MdmMaterialInfo material = materialMap.get(embryoCode);
        if (material == null) {
            log.debug("buildSingleTask：物料信息为空，embryoCode={}", embryoCode);
        }

        String structureName = material != null ? material.getStructureName() : lhResult.getStructureName();

        // 构建任务
        CoreScheduleAlgorithmService.DailyEmbryoTask task = new CoreScheduleAlgorithmService.DailyEmbryoTask();
        task.setLhId(lhResult.getId());
        task.setEmbryoCode(embryoCode);
        task.setVulcanizeDemand(vulcanizeDemand);
        task.setCurrentStock(currentStock);
        task.setProductionVersion(lhResult.getProductionVersion());

        if (material != null) {
            task.setMaterialDesc(material.getMaterialDesc());
            task.setMainMaterialDesc(material.getEmbryoDesc());
            task.setStructureName(material.getStructureName());
            task.setMaterialCode(material.getMaterialCode());
        } else {
            task.setMaterialDesc(embryoCode);
            task.setMainMaterialDesc(embryoCode);
            task.setStructureName(structureName);
        }

        task.setDemandQuantity(vulcanizeDemand);
        task.setAssignedQuantity(0);

        // 是否主销产品
        String mainProductCode = task.getMaterialCode();
        task.setIsMainProduct(context.getMainProductCodes() != null
                && mainProductCode != null
                && context.getMainProductCodes().contains(mainProductCode));

        // 硫化机台数和模数：一条LhScheduleResult = 一台硫化机
        task.setVulcanizeMachineCount(1);
        task.setVulcanizeMoldCount(lhResult.getMouldQty() != null ? lhResult.getMouldQty() : 1);

        // S5.2.3 计算库存可供硫化时长
        calculateStockHours(task, lhResult, currentStock, context);

        return task;
    }

    /**
     * 根据班次配置获取硫化记录对应班次的计划量
     *
     * <p>硫化有8个班次(CLASS1-CLASS8)，成型分3天排程。
     * 根据当前班次配置的 classField 字段获取对应的硫化班次计划量。
     *
     * @param lhResult            硫化记录
     * @param currentShiftConfigs 当前班次配置列表
     * @return 对应班次的硫化计划量
     */
    private int getShiftPlanQty(LhScheduleResult lhResult, List<CxShiftConfig> currentShiftConfigs) {
        if (currentShiftConfigs == null || currentShiftConfigs.isEmpty()) {
            return 0;
        }

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
     * 获取分配给该硫化任务的库存
     *
     * <p>库存已按硫化任务维度分配，共用胎胚库存按硫化任务需求比例分配。
     * 使用硫化任务的唯一标识 (lhId) 获取当前库存。
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
        return materialStockMap.getOrDefault(String.valueOf(lhId), 0);
    }

    /**
     * S5.2.3 计算库存可供硫化时长（stockHours）
     *
     * <p>任务分组阶段：成型产出未知（本次排程的结果），无法按班次动态推算，
     * 因此只基于当前库存计算初始的库存可供硫化时长。
     *
     * <p>计算公式：
     * <pre>
     *   日硫化量 → 从 MonthPlanProductLhCapacityVo 获取
     *   单胎单模硫化时长(秒) = 24×60×60 / 日硫化量
     *   stockHours = 库存 × 单胎单模硫化时长 / 任务的模数 / 3600 (转为小时)
     * </pre>
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

        // 1. 从 materialLhCapacityMap 获取该物料的日硫化量（key 是 materialCode，不是 embryoCode）
        Map<String, MonthPlanProductLhCapacityVo> lhCapacityMap = context.getMaterialLhCapacityMap();
        Integer dailyLhCapacity = null;
        if (lhCapacityMap != null) {
            String materialCode = task.getMaterialCode();
            MonthPlanProductLhCapacityVo capacityVo = lhCapacityMap.get(materialCode);
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
        BigDecimal stockHours = BigDecimal.valueOf(currentStock)
                .multiply(singleTireMoldSeconds)
                .divide(BigDecimal.valueOf(taskMoldQty), 2, BigDecimal.ROUND_HALF_UP)
                .divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 2, BigDecimal.ROUND_HALF_UP);

        task.setStockHours(stockHours);

        // 库存预警：超过18小时标记为高库存
        boolean isHighStock = stockHours.compareTo(BigDecimal.valueOf(STOCK_HIGH_HOURS_THRESHOLD)) > 0;
        task.setIsStockHighWarning(isHighStock);

        log.debug("物料 {} stockHours计算: 日硫化量={}, 单胎单模时长={}s, 模数={}, 库存={}, 库存可供时长={}h",
                task.getEmbryoCode(), dailyLhCapacity, singleTireMoldSeconds, taskMoldQty, currentStock, stockHours);
    }

    /**
     * 查找物料收尾日
     *
     * @param embryoCode 胎胚编码
     * @param context    排程上下文
     * @return 收尾日期，无则返回 null
     */
    private LocalDate findEndingDate(String embryoCode, ScheduleContextVo context) {
        if (context.getMaterialEndings() != null) {
            for (CxMaterialEnding ending : context.getMaterialEndings()) {
                if (embryoCode.equals(ending.getMaterialCode())) {
                    return ending.getPlannedEndingDate();
                }
            }
        }
        return null;
    }

    /**
     * S5.2.5 计算待排产量
     *
     * <p>与库存对冲后的计划产量：
     * <pre>
     *   plannedProduction = roundToVehicle(max(0, vulcanizeDemand - currentStock) × (1 + lossRate), tripCapacity)
     * </pre>
     *
     * @param task         胎胚任务
     * @param context      排程上下文
     * @param scheduleDate 排程日期
     */
    private void calculatePlannedProduction(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                             ScheduleContextVo context,
                                             LocalDate scheduleDate) {
        // 停产日：当天产量设为0
        if (scheduleDayTypeHelper.isStopDay(scheduleDate)) {
            task.setPlannedProduction(0);
            task.setRequiredCars(0);
            task.setEndingExtraInventory(0);
            return;
        }

        int vulcanizeDemand = task.getVulcanizeDemand() != null ? task.getVulcanizeDemand() : 0;
        int currentStock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;

        // Step 1: 与库存对冲，计算净需求
        int netDemand = Math.max(0, vulcanizeDemand - currentStock);

        // Step 2: 乘以(1 + 损耗率)
        BigDecimal lossRate = context.getLossRate() != null ? context.getLossRate() : BigDecimal.ZERO;
        BigDecimal requiredProduction = new BigDecimal(netDemand)
                .multiply(BigDecimal.ONE.add(lossRate))
                .setScale(0, BigDecimal.ROUND_UP);
        task.setPlannedProduction(requiredProduction.intValue());

        // Step 3: 整车取整
        int tripCapacity = getTripCapacity(task.getStructureName(), context);
        int plannedProduction = productionCalculator.roundToVehicle(requiredProduction.intValue(), tripCapacity);
        task.setRequiredCars((plannedProduction + tripCapacity - 1) / Math.max(tripCapacity, 1));
        task.setEndingExtraInventory(plannedProduction);
    }

    /**
     * S5.2.6 收尾余量处理
     *
     * <p>近3天收尾的任务，判断是否今天收尾（endingExtraInventory >= endingSurplusQty）：
     * <ul>
     *   <li>非主销 + 余量≤2条 → 舍弃（plannedProduction=0）</li>
     *   <li>非主销 + 余量&gt;2条 → 按实际量下（不补车）</li>
     *   <li>主销产品 → 不够一车则补足到一车</li>
     * </ul>
     *
     * @param task    胎胚任务
     * @param context 排程上下文
     */
    private void handleEndingRemainder(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                        ScheduleContextVo context) {
        // 仅处理近3天收尾的紧急任务
        if (!Boolean.TRUE.equals(task.getIsUrgentEnding())) {
            return;
        }

        Integer endingExtraInventory = task.getEndingExtraInventory();
        Integer endingSurplusQty = task.getEndingSurplusQty();

        if (endingExtraInventory == null || endingExtraInventory <= 0
                || endingSurplusQty == null || endingSurplusQty <= 0) {
            return;
        }

        // 今天是否最后一天收尾：当天计划量（含整车取整）>= 剩余成型量
        boolean isLastDay = endingExtraInventory >= endingSurplusQty;
        if (!isLastDay) {
            return;
        }

        // 今天最后一天收尾
        int tripCapacity = getTripCapacity(task.getStructureName(), context);
        if (!Boolean.TRUE.equals(task.getIsMainProduct()) && endingSurplusQty <= ENDING_DISCARD_THRESHOLD) {
            // 非主销产品 + 收尾余量≤2条，舍弃当天排产
            task.setPlannedProduction(0);
            task.setRequiredCars(0);
            task.setEndingExtraInventory(0);
            task.setEndingAbandoned(true);
            task.setEndingAbandonedQty(endingSurplusQty);
            log.info("收尾任务 {} 余量{}条被舍弃（非主销+余量≤2）", task.getEmbryoCode(), endingSurplusQty);
        } else if (!Boolean.TRUE.equals(task.getIsMainProduct())) {
            // 非主销产品 + 收尾余量>2条，按实际量下（不补车）
            // requiredCars 按实际量计算，不足一车的部分也算1车
            int planned = task.getPlannedProduction();
            task.setRequiredCars((planned + tripCapacity - 1) / Math.max(tripCapacity, 1));
            task.setEndingExtraInventory(planned);
            task.setIsLastEndingBatch(true);
            log.info("收尾任务 {} 今天最后一批（非主销），余量={}，计划={}",
                    task.getEmbryoCode(), endingSurplusQty, planned);
        } else {
            // 主销产品最后一批：不够一车则补足到一车
            if (endingExtraInventory > 0 && endingExtraInventory < tripCapacity) {
                task.setPlannedProduction(tripCapacity);
                task.setRequiredCars(1);
                task.setEndingExtraInventory(tripCapacity);
                log.info("收尾任务 {} 主销最后一批不足一车，补足到一车：{}", task.getEmbryoCode(), tripCapacity);
            }
            task.setIsLastEndingBatch(true);
            log.info("收尾任务 {} 今天最后一批（主销），余量={}，计划={}",
                    task.getEmbryoCode(), endingSurplusQty, task.getPlannedProduction());
        }
    }

    /**
     * S5.2.7 开停产特殊处理
     *
     * <p>停产日当天产量设为0（在 calculatePlannedProduction 中已处理）；
     * 停产标识日当天产量按实际量下（不补车），plannedProduction 已在上游计算好。
     *
     * @param task      胎胚任务
     * @param context   排程上下文
     * @param dayShifts 当前班次配置
     */
    private void handleOpeningClosingDay(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                          ScheduleContextVo context,
                                          List<CxShiftConfig> dayShifts) {
        LocalDate scheduleDate = context.getCurrentScheduleDate();

        // 停产日：当天产量设为0
        if (scheduleDayTypeHelper.isStopDay(scheduleDate)) {
            task.setPlannedProduction(0);
            task.setRequiredCars(0);
            task.setEndingExtraInventory(0);
            return;
        }

        // 停产标识日：plannedProduction 已在 calculatePlannedProduction 中计算好（含损耗率+整车取整）
        // 不做额外处理，保持原值即可（不补车）
    }

    /**
     * 获取结构胎面整车配置
     *
     * @param structureName 结构名称
     * @param context       排程上下文
     * @return 整车条数
     */
    private int getTripCapacity(String structureName, ScheduleContextVo context) {
        return productionCalculator.getTripCapacity(structureName, context);
    }
}
