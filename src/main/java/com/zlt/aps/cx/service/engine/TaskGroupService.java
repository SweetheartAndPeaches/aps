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
        log.info("【任务分组】收到 {} 条硫化记录", lhScheduleResults.size());

        // 构建基础映射
        Map<String, MdmMaterialInfo> materialMap = buildMaterialMap(context);
        Map<String, CxStock> stockMap = buildStockMap(context);

        if (machineOnlineEmbryoMap == null) {
            machineOnlineEmbryoMap = new HashMap<>();
        }

        // 获取当前班次的排量（每个班次只处理自己班次有排量的任务）
        final int currentClassIndex = getCurrentClassIndex(dayShifts);
        
        // 直接遍历每条硫化记录，为每条记录创建独立的任务
        int skippedNullEmbryo = 0;
        int skippedNullTask = 0;
        int skippedVulcanizeSurplusZero = 0;  // 硫化余量<=0跳过的任务数
        int skippedFormingRemainderZero = 0;  // 成型余量<=0跳过的任务数
        
        // 跟踪每个物料已使用的成型余量（用于多任务共享同一物料的场景）
        Map<String, Integer> materialUsedFormingRemainder = new HashMap<>();
        
        for (LhScheduleResult lhResult : lhScheduleResults) {
            if (lhResult.getEmbryoCode() == null) {
                skippedNullEmbryo++;
                continue;
            }
            
            // 每个班次只处理自己班次有排量的任务
            // 如果当前班次没有排量（为null或<=0），则跳过该任务（不创建任务）
            if (currentClassIndex > 0) {
                Integer classPlanQty = getClassPlanQtyByIndex(lhResult, currentClassIndex);
                if (classPlanQty == null || classPlanQty <= 0) {
                    skippedNullTask++;
                    continue;
                }
            }

            // 检查1：硫化余量 <= 0，说明该物料已超产，不再需要生产
            String materialCode = lhResult.getMaterialCode();
            if (context.getMonthSurplusMap() != null && materialCode != null) {
                MdmMonthSurplus monthSurplus = context.getMonthSurplusMap().get(materialCode);
                if (monthSurplus != null && monthSurplus.getPlanSurplusQty() != null) {
                    int vulcanizeSurplus = monthSurplus.getPlanSurplusQty().intValue();
                    if (vulcanizeSurplus <= 0) {
                        log.debug("物料 {} 硫化余量={} <= 0，已超产，跳过该任务", materialCode, vulcanizeSurplus);
                        skippedVulcanizeSurplusZero++;
                        continue;
                    }
                }
            }

            // 检查2：成型余量 <= 0，说明胎胚库存已满足硫化需求，不再需要成型生产
            Integer formingRemainder = getFormingRemainder(materialCode, context);
            if (formingRemainder != null && formingRemainder <= 0) {
                log.debug("物料 {} 成型余量={} <= 0，胎胚已满足，跳过该任务", materialCode, formingRemainder);
                skippedFormingRemainderZero++;
                continue;
            }

            CoreScheduleAlgorithmService.DailyEmbryoTask task = buildSingleTask(
                    lhResult, materialMap, stockMap, context, dayShifts);
            if (task == null) {
                skippedNullTask++;
                continue;
            }

            String embryoCode = lhResult.getEmbryoCode();

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

            // S5.2.4 计算收尾属性（传入已使用的成型余量）
            int usedRemainder = materialUsedFormingRemainder.getOrDefault(materialCode, 0);
            calculateEndingInfo(task, context, scheduleDate, usedRemainder);

            // S5.2.5 计算待排产量
            calculatePlannedProduction(task, context, scheduleDate);
            // S5.2.6 收尾余量处理
            handleEndingRemainder(task, context);

            // 打印收尾任务完整信息（所有字段已填充完毕）
            // 条件：成型余量低于阈值 或 紧急收尾
            Integer endingSurplus = task.getEndingSurplusQty();
            if ((endingSurplus != null && endingSurplus < ENDING_URGENT_FORMING_REMAINDER)
                    || Boolean.TRUE.equals(task.getIsUrgentEnding())) {
                log.info("成型余量低于阈值的收尾任务：物料={}, 剩余成型余量={}, 阈值={} | 收尾任务={}, 收尾余量={}, 硫化余量={}, 收尾日={}, 距收尾天={}, 紧急收尾={}, 近期收尾={} | 待排产量={}, 需车数={}",
                        embryoCode, task.getEndingSurplusQty(), ENDING_URGENT_FORMING_REMAINDER,
                        task.getIsEndingTask(), task.getEndingSurplusQty(), task.getVulcanizeSurplusQty(),
                        task.getEndingDate(), task.getDaysToEnding(), task.getIsUrgentEnding(), task.getIsNearEnding(),
                        task.getPlannedProduction(), task.getRequiredCars());
            }
            
            // 更新已使用的成型余量（累加当前任务的 endingExtraInventory）
            if (task.getEndingExtraInventory() != null && task.getEndingExtraInventory() > 0) {
                materialUsedFormingRemainder.merge(materialCode, task.getEndingExtraInventory(), Integer::sum);
                log.debug("物料 {} 已使用成型余量累计: {}", materialCode, materialUsedFormingRemainder.get(materialCode));
            }
            
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

        log.info("【任务分组结果】续作:{}个 | 试制:{}个 | 新增:{}个 | 跳过无效胚胎:{}个 | 跳过空任务:{}个 | 跳过硫化余量<=0:{}个 | 跳过成型余量<=0:{}个",
                result.getContinueTasks().size(),
                result.getTrialTasks().size(),
                result.getNewTasks().size(),
                skippedNullEmbryo, skippedNullTask, skippedVulcanizeSurplusZero, skippedFormingRemainderZero);
        return result;
    }

    /**
     * S5.2.4 计算收尾属性
     *
     * <p>包括：成型余量、是否收尾任务、是否10天内收尾、是否3天内收尾（紧急）、收尾日
     *
     * @param task           胎胚任务
     * @param context        排程上下文
     * @param scheduleDate   排程日期
     * @param usedRemainder  该物料已使用的成型余量（前面任务已排产的数量）
     */
    public void calculateEndingInfo(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextVo context,
            LocalDate scheduleDate,
            int usedRemainder) {

        String embryoCode = task.getEmbryoCode();
        String materialCode = task.getMaterialCode();

        // 获取成型余量（从预计算的映射中获取）
        Map<String, Integer> formingRemainderMap = context.getFormingRemainderMap();
        Integer totalFormingRemainder = null;
        Integer vulcanizeSurplusQty = null;

        // 从月计划余量获取硫化余量
        if (context.getMonthSurplusMap() != null) {
            MdmMonthSurplus monthSurplus = context.getMonthSurplusMap().get(materialCode);
            if (monthSurplus != null && monthSurplus.getPlanSurplusQty() != null) {
                vulcanizeSurplusQty = monthSurplus.getPlanSurplusQty().intValue();
            }
        }

        // 获取总成型余量
        if (formingRemainderMap != null && formingRemainderMap.containsKey(materialCode)) {
            totalFormingRemainder = formingRemainderMap.get(materialCode);
        }

        // 获取当前任务分配的库存
        int currentTaskStock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;

        // 计算当前任务的剩余成型余量 = 总成型余量 - 已使用成型余量 - 当前任务库存
        Integer remainingFormingRemainder = null;
        if (totalFormingRemainder != null) {
            remainingFormingRemainder = Math.max(0, totalFormingRemainder - usedRemainder - currentTaskStock);
            log.debug("物料 {} 总成型余量={}, 已使用={}, 当前任务库存={}, 剩余={}", 
                    materialCode, totalFormingRemainder, usedRemainder, currentTaskStock, remainingFormingRemainder);
        }

        task.setVulcanizeSurplusQty(vulcanizeSurplusQty);
        task.setEndingSurplusQty(remainingFormingRemainder);  // 使用剩余成型余量

        // 判断是否收尾任务（剩余成型余量 <= 0）
        boolean isEndingTask = remainingFormingRemainder != null && remainingFormingRemainder <= 0;
        task.setIsEndingTask(isEndingTask);

        // 获取收尾日（从物料收尾管理表，该表以物料编码为键）
        LocalDate endingDate = findEndingDate(task.getMaterialCode(), context);
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
                        task.getMaterialCode(), endingDate, daysToEnding);
            }
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
     * 获取成型余量
     *
     * <p>从 context 的 formingRemainderMap 中获取（key 是物料编码），如果没有则根据硫化余量和库存计算。
     *
     * @param materialCode 物料编码
     * @param context      排程上下文
     * @return 成型余量，无法计算时返回 null
     */
    private Integer getFormingRemainder(String materialCode, ScheduleContextVo context) {
        if (materialCode == null) {
            return null;
        }

        // 从context.getFormingRemainderMap映射中获取（key 是物料编码）
        Map<String, Integer> formingRemainderMap = context.getFormingRemainderMap();
        if (formingRemainderMap != null && formingRemainderMap.containsKey(materialCode)) {
            return formingRemainderMap.get(materialCode);
        }

        return 0;
    }

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
        String combinedKey = embryoCode;
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
        // 重要：优先使用 lhResult 中的 materialCode，因为同一个 embryoCode 可能对应多个不同的物料
        String materialCodeFromLh = lhResult.getMaterialCode();
        MdmMaterialInfo material = materialMap.get(embryoCode);
        
        // 如果 lhResult 中有 materialCode，优先使用；否则从 materialMap 中获取
        String finalMaterialCode = materialCodeFromLh;
        String materialDesc = null;
        String mainMaterialDesc = null;
        String structureNameFromMaterial = null;
        
        if (finalMaterialCode == null && material != null) {
            // lhResult 中没有 materialCode，从 materialMap 中获取
            finalMaterialCode = material.getMaterialCode();
            materialDesc = material.getMaterialDesc();
            mainMaterialDesc = material.getEmbryoDesc();
            structureNameFromMaterial = material.getStructureName();
        } else if (material != null) {
            // lhResult 中有 materialCode，但保留 material 的其他信息（如描述）
            materialDesc = material.getMaterialDesc();
            mainMaterialDesc = material.getEmbryoDesc();
            structureNameFromMaterial = material.getStructureName();
        }
        
        String structureName = structureNameFromMaterial != null ? structureNameFromMaterial : lhResult.getStructureName();

        // 构建任务
        CoreScheduleAlgorithmService.DailyEmbryoTask task = new CoreScheduleAlgorithmService.DailyEmbryoTask();
        task.setLhId(lhResult.getId());
        task.setEmbryoCode(embryoCode);
        task.setVulcanizeDemand(vulcanizeDemand);
        task.setCurrentStock(currentStock);
        task.setProductionVersion(lhResult.getProductionVersion());
        task.setMaterialCode(finalMaterialCode);  // 直接使用 lhResult 或 materialMap 中的 materialCode
        
        if (materialDesc != null) {
            task.setMaterialDesc(materialDesc);
        } else {
            task.setMaterialDesc(finalMaterialCode != null ? finalMaterialCode : embryoCode);
        }
        
        if (mainMaterialDesc != null) {
            task.setMainMaterialDesc(mainMaterialDesc);
        } else {
            task.setMainMaterialDesc(embryoCode);
        }
        
        task.setStructureName(structureName);

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
     * 获取当前班次对应的硫化班次索引
     *
     * <p>从 dayShifts 中获取当前班次的 classField，然后提取班次索引。
     * 例如：CLASS1 -> 1, CLASS2 -> 2, CLASS3 -> 3
     *
     * @param dayShifts 当前班次配置列表
     * @return 班次索引 (1-8)，如果没有有效的班次配置则返回 0
     */
    private int getCurrentClassIndex(List<CxShiftConfig> dayShifts) {
        if (dayShifts == null || dayShifts.isEmpty()) {
            return 0;
        }
        // 获取第一个班次的 classField
        CxShiftConfig shiftConfig = dayShifts.get(0);
        String classField = shiftConfig.getClassField();
        if (classField != null && classField.startsWith("CLASS")) {
            try {
                return Integer.parseInt(classField.replace("CLASS", ""));
            } catch (NumberFormatException e) {
                log.warn("无法解析班次字段: {}", classField);
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
    /**
     * 从物料收尾管理表获取计划收尾日期
     * @param materialCode 物料编码（CxMaterialEnding.materialCode 存的是物料编码）
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

        // Step 2: 乘以(1 + 损耗率)，但试制任务不考虑损耗率
        int requiredProductionValue;
        if (Boolean.TRUE.equals(task.getIsTrialTask()) || Boolean.TRUE.equals(task.getIsProductionTrial())) {
            // 试制任务不计算损耗率
            requiredProductionValue = netDemand;
        } else {
            BigDecimal lossRate = context.getLossRate() != null ? context.getLossRate() : BigDecimal.ZERO;
            BigDecimal requiredProduction = new BigDecimal(netDemand)
                    .multiply(BigDecimal.ONE.add(lossRate))
                    .setScale(0, BigDecimal.ROUND_UP);
            requiredProductionValue = requiredProduction.intValue();
        }
        task.setPlannedProduction(requiredProductionValue);

        // Step 3: 整车取整
        int tripCapacity = getTripCapacity(task.getStructureName(), context);
        int plannedProduction = productionCalculator.roundToVehicle(requiredProductionValue, tripCapacity);
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
            // endingExtraInventory 设置为实际余量（不取整），用于后续均衡分配时扣除
            task.setEndingExtraInventory(endingSurplusQty);
            
            // requiredCars 按实际余量计算，不足一车的部分也算1车
            task.setRequiredCars((endingSurplusQty + tripCapacity - 1) / Math.max(tripCapacity, 1));
            
            // plannedProduction 保持取整后的值（用于显示），但实际生产按 endingExtraInventory
            task.setIsLastEndingBatch(true);
            log.info("收尾任务 {} 今天最后一批（非主销），余量={}，计划={}，实际生产={}",
                    task.getEmbryoCode(), endingSurplusQty, task.getPlannedProduction(), endingSurplusQty);
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

        // 停产日（已停产）：当天产量设为0
        if (scheduleDayTypeHelper.isStopDay(scheduleDate)) {
            task.setPlannedProduction(0);
            task.setRequiredCars(0);
            task.setEndingExtraInventory(0);
            return;
        }

        // 获取当前班次信息
        CxShiftConfig currentShift = dayShifts != null && dayShifts.size() == 1 ? dayShifts.get(0) : null;
        int currentDayShiftOrder = currentShift != null && currentShift.getDayShiftOrder() != null
                ? currentShift.getDayShiftOrder() : 0;

        // ==================== 停产标识日处理 ====================
        if (scheduleDayTypeHelper.isStopFlagDay(scheduleDate)) {
            handleClosingDayTask(task, context, scheduleDate, currentDayShiftOrder);
            return;
        }

        // ==================== 开产日处理 ====================
        if (scheduleDayTypeHelper.isOpeningDay(scheduleDate)) {
            handleOpeningDayTask(task, context, scheduleDate, currentDayShiftOrder);
            return;
        }
    }

    /**
     * 停产标识日任务处理：反推封顶
     *
     * <p>核心逻辑：
     * <ol>
     *   <li>根据硫化机停锅时间和班次配置，确定停锅班次 closingShiftOrder</li>
     *   <li>计算成型停机时间 = 硫化停锅时间 - 预留消化时间</li>
     *   <li>反推总量 = 从成型停机到硫化停锅期间硫化消耗的胎胚数</li>
     *   <li>当前班次需生产量 = min(normalDemand, max(0, 反推总量 - currentStock))</li>
     *   <li>如果当前班次 = 停锅班次，不补整车</li>
     * </ol>
     *
     * @param task               胎胚任务
     * @param context            排程上下文
     * @param scheduleDate       排程日期
     * @param currentDayShiftOrder 当前班次序号
     */
    private void handleClosingDayTask(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                       ScheduleContextVo context,
                                       LocalDate scheduleDate,
                                       int currentDayShiftOrder) {
        // 标记为停产日任务
        task.setIsClosingDayTask(true);

        // 确定停锅班次
        Integer closingShiftOrder = determineClosingShiftOrder(context);
        task.setClosingShiftOrder(closingShiftOrder);

        if (closingShiftOrder == null) {
            log.warn("停产日 {} 无法确定停锅班次，保持原计划量", scheduleDate);
            return;
        }

        // 计算反推总量
        int closingRequiredStock = calculateClosingRequiredStock(task, context, scheduleDate);
        task.setClosingRequiredStock(closingRequiredStock);

        int currentStock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;
        int normalDemand = task.getPlannedProduction() != null ? task.getPlannedProduction() : 0;

        // 当前班次到停机时间还需的量
        int thisShiftNeeded = Math.max(0, closingRequiredStock - currentStock);

        // 封顶：取正常需求和反推需求中的较小值
        int cappedProduction = Math.min(normalDemand, thisShiftNeeded);

        log.info("停产反推封顶: embryoCode={}, closingShiftOrder={}, closingRequiredStock={}, " +
                        "currentStock={}, normalDemand={}, thisShiftNeeded={}, cappedProduction={}, " +
                        "currentDayShiftOrder={}",
                task.getEmbryoCode(), closingShiftOrder, closingRequiredStock,
                currentStock, normalDemand, thisShiftNeeded, cappedProduction, currentDayShiftOrder);

        // 如果当前班次是停锅班次，不补整车（按实量下）
        if (currentDayShiftOrder == closingShiftOrder) {
            // 不补整车：用封顶量直接作为 endingExtraInventory
            int tripCapacity = getTripCapacity(task.getStructureName(), context);
            if (tripCapacity > 0 && cappedProduction > 0 && cappedProduction % tripCapacity != 0) {
                // 向下取整到整车
                int roundedDown = (cappedProduction / tripCapacity) * tripCapacity;
                // 但停产最后班次可以不整车，保持封顶量
                log.info("停锅班次不补整车: embryoCode={}, cappedProduction={}, 向下整车={}, 保持不整车={}",
                        task.getEmbryoCode(), cappedProduction, roundedDown, cappedProduction);
            }
            task.setPlannedProduction(cappedProduction);
            task.setEndingExtraInventory(cappedProduction);
            task.setRequiredCars(cappedProduction > 0 ? 1 : 0);
        } else if (currentDayShiftOrder < closingShiftOrder) {
            // 停锅班次之前的班次：按封顶量正常排产，整车取整
            int tripCapacity = getTripCapacity(task.getStructureName(), context);
            int roundedProduction = productionCalculator.roundToVehicle(cappedProduction, tripCapacity);
            task.setPlannedProduction(roundedProduction);
            task.setEndingExtraInventory(roundedProduction);
            task.setRequiredCars(tripCapacity > 0
                    ? (roundedProduction + tripCapacity - 1) / tripCapacity : 0);
        }
        // currentDayShiftOrder > closingShiftOrder 不应出现（已被班次停产跳过）
    }

    /**
     * 开产日任务处理：提前一班备货
     *
     * <p>核心逻辑：
     * <ol>
     *   <li>根据硫化开模时间和班次配置，确定硫化开产班次 lhOpeningShiftOrder</li>
     *   <li>成型开产班次 = 硫化开产班次 - 1</li>
     *   <li>当成型班次早于硫化开产班次时，用硫化开产班次的需求作为成型需求</li>
     *   <li>成型开产首班：6小时产能封顶，不补整车</li>
     * </ol>
     *
     * @param task               胎胚任务
     * @param context            排程上下文
     * @param scheduleDate       排程日期
     * @param currentDayShiftOrder 当前班次序号
     */
    private void handleOpeningDayTask(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                       ScheduleContextVo context,
                                       LocalDate scheduleDate,
                                       int currentDayShiftOrder) {
        // 确定硫化开产班次和成型开产班次
        Integer lhOpeningShiftOrder = determineLhOpeningShiftOrder(context);
        if (lhOpeningShiftOrder == null) {
            log.warn("开产日 {} 无法确定硫化开产班次，保持原计划量", scheduleDate);
            return;
        }
        int formingOpeningShiftOrder = Math.max(1, lhOpeningShiftOrder - 1);
        task.setLhOpeningShiftOrder(lhOpeningShiftOrder);
        task.setFormingOpeningShiftOrder(formingOpeningShiftOrder);

        log.info("开产日班次确定: embryoCode={}, lhOpeningShiftOrder={}, formingOpeningShiftOrder={}, currentDayShiftOrder={}",
                task.getEmbryoCode(), lhOpeningShiftOrder, formingOpeningShiftOrder, currentDayShiftOrder);

        // 当前班次 = 成型开产首班（早于硫化开产一个班次）
        if (currentDayShiftOrder == formingOpeningShiftOrder && currentDayShiftOrder < lhOpeningShiftOrder) {
            // 当前班次硫化需求=0（硫化还没开产），用硫化开产班次的需求
            int nextShiftDemand = getNextShiftDemand(task, context, lhOpeningShiftOrder);
            if (nextShiftDemand <= 0) {
                // 硫化开产班次也没有需求，不排产
                task.setPlannedProduction(0);
                task.setEndingExtraInventory(0);
                task.setRequiredCars(0);
                log.info("开产首班硫化开产班次无需求: embryoCode={}, 不排产", task.getEmbryoCode());
                return;
            }

            // 开产首班6小时产能封顶
            int openingShiftCapacity = calculateOpeningShiftCapacity(task, context);
            int demand = Math.min(nextShiftDemand, openingShiftCapacity);

            // 开产首班不补整车
            task.setPlannedProduction(demand);
            task.setEndingExtraInventory(demand);
            task.setRequiredCars(1);
            task.setIsOpeningDayTask(true);
            task.setOpeningShiftCapacity(openingShiftCapacity);

            log.info("开产首班备货: embryoCode={}, 硫化开产班次需求={}, 首班6h产能={}, 实际排产={}, 不补整车",
                    task.getEmbryoCode(), nextShiftDemand, openingShiftCapacity, demand);
        } else if (currentDayShiftOrder >= lhOpeningShiftOrder) {
            // 硫化已开产的班次：正常排产（demand已在buildSingleTask中正确计算）
            task.setIsOpeningDayTask(true);
            log.info("开产非首班正常排产: embryoCode={}, currentDayShiftOrder={}, demand={}",
                    task.getEmbryoCode(), currentDayShiftOrder, task.getVulcanizeDemand());
        }
    }

    /**
     * 确定停锅班次序号
     *
     * <p>根据硫化机停锅时间（参数配置）和班次时间表，确定停锅时间落在哪个班次。
     *
     * @param context 排程上下文
     * @return 停锅班次的dayShiftOrder，找不到返回null
     */
    private Integer determineClosingShiftOrder(ScheduleContextVo context) {
        String vulcanizingStopTimeStr = context.getVulcanizingStopTimeStr();
        if (vulcanizingStopTimeStr == null || vulcanizingStopTimeStr.isEmpty()) {
            log.warn("未配置硫化机停锅时间(VULCANIZING_STOP_TIME)，无法确定停锅班次");
            return null;
        }
        // 获取班次配置（按dayShiftOrder排序）
        List<CxShiftConfig> shiftConfigs = getSortedShiftConfigs(context);
        return scheduleDayTypeHelper.getShiftOrderByTime(vulcanizingStopTimeStr, shiftConfigs);
    }

    /**
     * 确定硫化开产班次序号
     *
     * <p>根据硫化开模时间（参数配置）和班次时间表，确定开模时间落在哪个班次。
     *
     * @param context 排程上下文
     * @return 硫化开产班次的dayShiftOrder，找不到返回null
     */
    private Integer determineLhOpeningShiftOrder(ScheduleContextVo context) {
        String vulcanizingOpenTimeStr = context.getVulcanizingOpenTimeStr();
        if (vulcanizingOpenTimeStr == null || vulcanizingOpenTimeStr.isEmpty()) {
            log.warn("未配置硫化开模时间(VULCANIZING_OPEN_TIME)，无法确定硫化开产班次");
            return null;
        }
        List<CxShiftConfig> shiftConfigs = getSortedShiftConfigs(context);
        return scheduleDayTypeHelper.getShiftOrderByTime(vulcanizingOpenTimeStr, shiftConfigs);
    }

    /**
     * 获取按dayShiftOrder排序的班次配置列表（去重，只取第1天的3个班次）
     */
    private List<CxShiftConfig> getSortedShiftConfigs(ScheduleContextVo context) {
        List<CxShiftConfig> allShifts = context.getShiftConfigList();
        if (allShifts == null || allShifts.isEmpty()) {
            return new ArrayList<>();
        }
        // 只取第1天的班次配置（排程天数不同但班次时间相同）
        return allShifts.stream()
                .filter(c -> c.getScheduleDay() != null && c.getScheduleDay() == 1)
                .sorted(java.util.Comparator.comparingInt(c -> c.getDayShiftOrder() != null ? c.getDayShiftOrder() : 0))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 计算停产反推总量
     *
     * <p>从成型停机时间到硫化停锅时间，硫化需要消耗的胎胚总量。
     * <pre>
     *   成型停机时间 = 硫化停锅时间 - 预留消化时间
     *   反推总量 = 时长(秒) / 单胎单模硫化时长(秒) × 模数
     * </pre>
     *
     * @param task         胎胚任务
     * @param context      排程上下文
     * @param scheduleDate 排程日期
     * @return 反推总量（条数）
     */
    private int calculateClosingRequiredStock(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                               ScheduleContextVo context,
                                               LocalDate scheduleDate) {
        // 从硫化排程结果反推
        LhScheduleResult lhResult = findLhResultByTask(task, context);
        if (lhResult == null) {
            log.warn("停产反推：无法找到胎胚 {} 对应的硫化排程结果，使用默认0", task.getEmbryoCode());
            return 0;
        }

        // 计算单胎单模硫化时长(秒)
        int dailyLhCapacity = getDailyLhCapacity(lhResult, context);
        int moldQty = task.getVulcanizeMoldCount() != null ? task.getVulcanizeMoldCount() : 1;
        int ratio = getStructureLhRatio(task, context);
        if (dailyLhCapacity <= 0 || ratio <= 0) {
            return 0;
        }
        double singleTireMoldSeconds = (double) 24 * 3600 / ((long) ratio * dailyLhCapacity);

        // 预留消化时间
        int reservedDigestHours = context.getReservedDigestHours() != null ? context.getReservedDigestHours() : 1;
        // 从成型停机到硫化停锅的时长 = 预留消化时间（小时）
        double durationSeconds = reservedDigestHours * 3600.0;

        // 反推总量 = 时长 / 单胎单模时长 × 模数
        int requiredStock = (int) Math.ceil(durationSeconds / singleTireMoldSeconds * moldQty);

        log.info("停产反推总量计算: embryoCode={}, dailyLhCapacity={}, moldQty={}, ratio={}, " +
                        "singleTireMoldSeconds={}, reservedDigestHours={}h, requiredStock={}",
                task.getEmbryoCode(), dailyLhCapacity, moldQty, ratio,
                String.format("%.1f", singleTireMoldSeconds), reservedDigestHours, requiredStock);

        return requiredStock;
    }

    /**
     * 获取硫化开产班次的需求量
     *
     * <p>成型开产首班（早于硫化开产一个班次）需要用硫化开产班次的CLASS需求量。
     *
     * @param task                胎胚任务
     * @param context             排程上下文
     * @param lhOpeningShiftOrder 硫化开产班次序号
     * @return 硫化开产班次的需求量
     */
    private int getNextShiftDemand(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                    ScheduleContextVo context,
                                    int lhOpeningShiftOrder) {
        LhScheduleResult lhResult = findLhResultByTask(task, context);
        if (lhResult == null) {
            return 0;
        }

        // 硫化开产班次对应的classField
        // dayShiftOrder -> classField 映射
        List<CxShiftConfig> shiftConfigs = getSortedShiftConfigs(context);
        String targetClassField = null;
        for (CxShiftConfig shiftConfig : shiftConfigs) {
            if (shiftConfig.getDayShiftOrder() != null && shiftConfig.getDayShiftOrder() == lhOpeningShiftOrder) {
                targetClassField = shiftConfig.getClassField();
                break;
            }
        }

        if (targetClassField == null || !targetClassField.startsWith("CLASS")) {
            log.warn("无法找到硫化开产班次 {} 对应的classField", lhOpeningShiftOrder);
            return 0;
        }

        try {
            int classIndex = Integer.parseInt(targetClassField.substring(5));
            Integer planQty = getClassPlanQtyByIndex(lhResult, classIndex);
            return planQty != null ? planQty : 0;
        } catch (NumberFormatException e) {
            log.warn("无法解析classField: {}", targetClassField);
            return 0;
        }
    }

    /**
     * 计算开产首班6小时产能
     */
    private int calculateOpeningShiftCapacity(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                               ScheduleContextVo context) {
        int dailyLhCapacity = getDailyLhCapacityByTask(task, context);
        int ratio = getStructureLhRatio(task, context);
        if (dailyLhCapacity <= 0 || ratio <= 0) {
            return 300; // 默认6h × 50条/h
        }
        double singleTireMoldSeconds = (double) 24 * 3600 / ((long) ratio * dailyLhCapacity);
        int capacity = (int) Math.floor(6 * 3600.0 / singleTireMoldSeconds);
        return Math.max(capacity, 0);
    }

    /**
     * 根据任务查找对应的LhScheduleResult
     */
    private LhScheduleResult findLhResultByTask(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                                 ScheduleContextVo context) {
        if (task.getLhId() != null) {
            List<LhScheduleResult> lhResults = context.getLhScheduleResults();
            if (lhResults != null) {
                for (LhScheduleResult lh : lhResults) {
                    if (task.getLhId().equals(lh.getId())) {
                        return lh;
                    }
                }
            }
        }
        // 兜底：按embryoCode匹配
        if (task.getEmbryoCode() != null) {
            List<LhScheduleResult> lhResults = context.getLhScheduleResults();
            if (lhResults != null) {
                for (LhScheduleResult lh : lhResults) {
                    if (task.getEmbryoCode().equals(lh.getEmbryoCode())) {
                        return lh;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取日硫化量
     */
    private int getDailyLhCapacity(LhScheduleResult lhResult, ScheduleContextVo context) {
        if (context.getMaterialLhCapacityMap() != null && lhResult.getMaterialCode() != null) {
            MonthPlanProductLhCapacityVo vo = context.getMaterialLhCapacityMap().get(lhResult.getMaterialCode());
            if (vo != null && vo.getDayVulcanizationQty() != null) {
                return vo.getDayVulcanizationQty();
            }
        }
        return 0;
    }

    /**
     * 通过任务获取日硫化量
     */
    private int getDailyLhCapacityByTask(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                          ScheduleContextVo context) {
        if (context.getMaterialLhCapacityMap() != null && task.getMaterialCode() != null) {
            MonthPlanProductLhCapacityVo vo = context.getMaterialLhCapacityMap().get(task.getMaterialCode());
            if (vo != null && vo.getDayVulcanizationQty() != null) {
                return vo.getDayVulcanizationQty();
            }
        }
        return 0;
    }

    /**
     * 获取结构硫化配比
     */
    private int getStructureLhRatio(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                     ScheduleContextVo context) {
        if (context.getStructureLhRatioMap() != null && task.getStructureName() != null) {
            com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio lhRatio =
                    context.getStructureLhRatioMap().get(task.getStructureName());
            if (lhRatio != null && lhRatio.getLhMachineMaxQty() != null && lhRatio.getLhMachineMaxQty() > 0) {
                return lhRatio.getLhMachineMaxQty();
            }
        }
        return 1;
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
