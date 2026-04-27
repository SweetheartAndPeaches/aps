package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.entity.CxMaterialEnding;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.cx.service.engine.CoreScheduleAlgorithmService;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMonthSurplus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
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

    /** 收尾舍弃阈值默认值：非主销产品余量≤此值时舍弃（条） */
    private static final int DEFAULT_ENDING_DISCARD_THRESHOLD = 2;

    /** 成型余量紧急阈值默认值：成型余量低于此值标记为紧急收尾（条） */
    private static final int DEFAULT_ENDING_URGENT_FORMING_REMAINDER = 400;

    /** 近期收尾天数阈值默认值（10 天内） */
    private static final int DEFAULT_ENDING_DAYS_THRESHOLD = 10;

    /** 紧急收尾天数阈值默认值（3 天内） */
    private static final int DEFAULT_URGENT_ENDING_DAYS = 3;

    // ==================== 参数配置编码 ====================

    /** 参数编码：收尾舍弃阈值 */
    private static final String PARAM_ENDING_DISCARD_THRESHOLD = "ENDING_DISCARD_THRESHOLD";

    /** 参数编码：成型余量紧急阈值 */
    private static final String PARAM_ENDING_URGENT_FORMING_REMAINDER = "ENDING_URGENT_FORMING_REMAINDER";

    /** 参数编码：近期收尾天数阈值 */
    private static final String PARAM_ENDING_DAYS_THRESHOLD = "ENDING_DAYS_THRESHOLD";

    /** 参数编码：紧急收尾天数阈值 */
    private static final String PARAM_URGENT_ENDING_DAYS = "URGENT_ENDING_DAYS";

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

        // 一次性加载所有阈值配置（避免循环中重复打印日志）
        int endingDiscardThreshold = getEndingDiscardThreshold(context);
        int endingUrgentFormingRemainder = getEndingUrgentFormingRemainder(context);
        int endingDaysThreshold = getEndingDaysThreshold(context);
        int urgentEndingDays = getUrgentEndingDays(context);
        log.info("【收尾参数配置】收尾舍弃阈值={}, 成型余量紧急阈值={}, 近期收尾天数={}, 紧急收尾天数={}",
                endingDiscardThreshold, endingUrgentFormingRemainder, endingDaysThreshold, urgentEndingDays);

        // 判断当前班次是否为开产班次（用于提前过滤关键产品）
        boolean isOpeningShift = false;
        if (dayShifts != null && !dayShifts.isEmpty()) {
            CxShiftConfig currentShift = dayShifts.get(0);
            if (currentShift.getDayShiftOrder() != null) {
                LocalDate currentScheduleDate = context.getCurrentScheduleDate();
                String factoryCode = context.getFactoryCode();
                ScheduleDayTypeHelper.ShiftType st = scheduleDayTypeHelper.determineShiftType(
                        currentScheduleDate, currentShift.getDayShiftOrder(), factoryCode);
                isOpeningShift = st == ScheduleDayTypeHelper.ShiftType.OPEN_START;
            }
        }

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
        // 跟踪每个物料已处理的任务列表（用于回溯更新 isLastEndingBatch）
        Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> materialTasksMap = new HashMap<>();
        
        for (LhScheduleResult lhResult : lhScheduleResults) {
            if (lhResult.getEmbryoCode() == null) {
                skippedNullEmbryo++;
                continue;
            }

            log.info("========== 处理任务: 胎胚={}, 物料={} ==========", lhResult.getEmbryoCode(), lhResult.getMaterialCode());
            
            // 库存够硫化当天剩余班次的消耗，跳过该任务
            // 逻辑：
            // - 班次1（第一天）：判断库存够硫化班次1+班次2的计划，够了就跳过
            // - 班次2（第一天）：判断库存够硫化班次2的计划，够了就跳过
            // - 班次1（第二天）：判断库存够硫化班次1+班次2+班次3的计划，够了就跳过
            // - 依次类推
            int currentStock = getCurrentStock(context, lhResult.getId());
            int todayRemainingDemand = calculateTodayRemainingDemand(context, dayShifts, lhResult);
            if (todayRemainingDemand > 0 && currentStock >= todayRemainingDemand) {
                log.debug("库存充足跳过: 胎胚={}, 库存={}, 当日剩余需求={}, 当前班次={}", 
                        lhResult.getEmbryoCode(), currentStock, todayRemainingDemand, dayShifts.get(0).getShiftCode());
                skippedNullTask++;
                continue;
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

            // 开产班次提前过滤关键产品（不在分组中保留，直接在循环中跳过）
            if (isOpeningShift && context.getKeyProductCodes() != null
                    && lhResult.getEmbryoCode() != null
                    && context.getKeyProductCodes().contains(lhResult.getEmbryoCode())) {
                log.info("开产班次关键产品跳过: 胎胚={}", lhResult.getEmbryoCode());
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

            // 将任务添加到物料任务列表（用于回溯更新）
            materialTasksMap.computeIfAbsent(materialCode, k -> new ArrayList<>()).add(task);

            // S5.2.4 计算收尾属性（传入已使用的成型余量）
            int usedRemainder = materialUsedFormingRemainder.getOrDefault(materialCode, 0);
            calculateEndingInfo(task, context, scheduleDate, usedRemainder);

            // S5.2.4.1 开产班次：更新 vulcanizeDemand 为下一个有计划的 CLASS 的量
            if (isOpeningShift) {
                int currentDemand = task.getVulcanizeDemand() != null ? task.getVulcanizeDemand() : 0;
                if (currentDemand <= 0) {
                    if (currentClassIndex > 0) {
                        for (int ci = currentClassIndex + 1; ci <= 8; ci++) {
                            Integer nextPlan = getClassPlanQtyByIndex(lhResult, ci);
                            if (nextPlan != null && nextPlan > 0) {
                                task.setVulcanizeDemand(nextPlan);
                                log.info("开产班次: 胎胚={}, 当前CLASS{}计划=0, 使用CLASS{}计划={}",
                                        embryoCode, currentClassIndex, ci, nextPlan);
                                break;
                            }
                        }
                    }
                }
                // 同时算出开产基准量（6/24 × 双模日硫化，往下取整车），供 handleOpeningDayTaskV2 封顶用
                int doubleMoldDailyCapacity = getDailyLhCapacityByTask(task, context) * 2;
                if (doubleMoldDailyCapacity > 0) {
                    int raw = (int) Math.ceil(6.0 / 24.0 * doubleMoldDailyCapacity);
                    int tripCapacity = getTripCapacity(task.getStructureName(), context);
                    int openingBase = tripCapacity > 0 ? (raw / tripCapacity) * tripCapacity : raw;
                    task.setOpeningShiftCapacity(openingBase);
                }
            }

            // S5.2.5 计算待排产量
            calculatePlannedProduction(task, context, scheduleDate);
            // S5.2.6 收尾余量处理
            handleEndingRemainder(task, context);
            
            // S5.2.6.1 收尾余量处理后再次检查：如果 handleEndingRemainder 标记了 isLastEndingBatch，需要回溯更新
            if (Boolean.TRUE.equals(task.getIsLastEndingBatch())) {
                List<CoreScheduleAlgorithmService.DailyEmbryoTask> allTasksForMaterial = materialTasksMap.get(materialCode);
                if (allTasksForMaterial != null) {
                    for (CoreScheduleAlgorithmService.DailyEmbryoTask prevTask : allTasksForMaterial) {
                        if (prevTask != task && !Boolean.TRUE.equals(prevTask.getIsLastEndingBatch())) {
                            prevTask.setIsLastEndingBatch(true);
                            log.info("回溯更新 isLastEndingBatch（收尾余量处理）: 物料={}, 胎胚={} → true", materialCode, prevTask.getEmbryoCode());
                        }
                    }
                }
            }

            // 打印收尾任务完整信息（所有字段已填充完毕）
            // 条件：成型余量低于阈值 或 紧急收尾
            Integer endingSurplus = task.getEndingSurplusQty();
            if ((endingSurplus != null && endingSurplus < getEndingUrgentFormingRemainder(context))
                    || Boolean.TRUE.equals(task.getIsUrgentEnding())) {
                int vulcanizeDmd = task.getVulcanizeDemand() != null ? task.getVulcanizeDemand() : 0;
                int stock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;
                int netDemand = Math.max(0, vulcanizeDmd - stock);
                BigDecimal lossRate = context.getLossRate() != null ? context.getLossRate() : BigDecimal.ZERO;
                int tripCap = getTripCapacity(task.getStructureName(), context);
                int plannedProd = task.getPlannedProduction() != null ? task.getPlannedProduction() : 0;
                log.info("收尾任务[{}]: 剩余余量={}, 收尾日={}, 距收尾={}天, 紧急={}, 近期={}, 最后一批={}",
                        embryoCode, task.getEndingSurplusQty(), task.getEndingDate(),
                        task.getDaysToEnding(), task.getIsUrgentEnding(), task.getIsNearEnding(), task.getIsLastEndingBatch());
                log.info("  排产计算: (硫化{} - 库存{}) × (1+损耗{}) = {}×{}={}, 整车({})取整→待排={}, 需车={}, 实际={}",
                        vulcanizeDmd, stock, lossRate, netDemand,
                        lossRate.add(BigDecimal.ONE).setScale(4, BigDecimal.ROUND_HALF_UP),
                        plannedProd, tripCap, task.getPlannedProduction(), task.getRequiredCars(), task.getEndingExtraInventory());
            }
            
            // 更新已使用的成型余量（累加当前任务的 endingExtraInventory）
            if (task.getEndingExtraInventory() != null && task.getEndingExtraInventory() > 0) {
                materialUsedFormingRemainder.merge(materialCode, task.getEndingExtraInventory(), Integer::sum);
                log.debug("物料 {} 已使用成型余量累计: {}", materialCode, materialUsedFormingRemainder.get(materialCode));
            }
            
            // S5.2.7 停产特殊处理
            handleOpeningClosingDay(task, context, dayShifts);
            // S5.2.8 试制任务：产量必须是双数，不补整车
            if (Boolean.TRUE.equals(isTrialTask) || Boolean.TRUE.equals(isProductionTrial)) {
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

        // 计算当前任务的剩余成型余量 = 总成型余量 - 已使用成型余量
        Integer remainingFormingRemainder = null;
        if (totalFormingRemainder != null) {
            remainingFormingRemainder = Math.max(0, totalFormingRemainder - usedRemainder);
            log.debug("物料 {} 总成型余量={}, 已使用={}, 剩余={}", 
                    materialCode, totalFormingRemainder, usedRemainder, remainingFormingRemainder);
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
            boolean isNearEnding = daysToEnding >= 0 && daysToEnding <= getEndingDaysThreshold(context);
            task.setIsNearEnding(isNearEnding);

            // 判断是否3天内收尾（紧急），或成型余量>=400（库存积压风险）
            boolean isUrgentEnding = (daysToEnding >= 0 && daysToEnding <= getUrgentEndingDays(context))
                    || (remainingFormingRemainder != null && remainingFormingRemainder <= getEndingUrgentFormingRemainder(context));
            task.setIsUrgentEnding(isUrgentEnding);

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
        log.debug("硫化任务排量: embryoCode={}, vulcanizeDemand={}, currentStock={}",
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
     * 计算当日剩余班次的硫化需求总量
     *
     * <p>按班次维度判断库存是否充足：
     * - 班次1（第一天）：库存 >= 班次1计划量 + 班次2计划量 → 跳过
     * - 班次2（第一天）：库存 >= 班次2计划量 → 跳过
     * - 班次1（第二天）：库存 >= 班次1+2+3计划量 → 跳过
     * - 依次类推
     *
     * @param context   排程上下文
     * @param dayShifts 当前班次配置列表（singleShiftList，只含当前班次）
     * @param lhResult  硫化记录
     * @return 当日剩余班次的硫化需求总量
     */
    private int calculateTodayRemainingDemand(ScheduleContextVo context, List<CxShiftConfig> dayShifts, LhScheduleResult lhResult) {
        if (dayShifts == null || dayShifts.isEmpty()) {
            return 0;
        }
        CxShiftConfig currentShift = dayShifts.get(0);
        int currentScheduleDay = currentShift.getScheduleDay() != null ? currentShift.getScheduleDay() : 1;
        int currentClassIndex = currentShift.getDayShiftOrder() != null ? currentShift.getDayShiftOrder() : 1;

        // 从上下文获取所有班次配置，确定当天有几个班次
        List<CxShiftConfig> allShifts = context.getShiftConfigList();
        if (allShifts == null || allShifts.isEmpty()) {
            return 0;
        }
        // 当天班次数 = scheduleDay 等于当前班的班次数
        int shiftsPerDay = (int) allShifts.stream()
                .filter(c -> c.getScheduleDay() != null && c.getScheduleDay().equals(currentScheduleDay))
                .count();
        if (shiftsPerDay <= 0) {
            shiftsPerDay = 1;
        }

        // 计算当日剩余班次数（含当前班次）
        int remainingShifts = shiftsPerDay - currentClassIndex + 1;
        if (remainingShifts <= 0) {
            return 0;
        }

        // 计算需求：从当前班次到当日最后一个班次的 classPlanQty 总和
        int totalDemand = 0;
        for (int i = 0; i < remainingShifts; i++) {
            int classOffset = (currentScheduleDay - 1) * 3 + currentClassIndex + i - 1; // 0-indexed class field offset
            if (classOffset >= 0 && classOffset < 8) {
                Integer classDemand = getClassPlanQtyByIndex(lhResult, classOffset + 1); // classIndex is 1-based
                if (classDemand != null && classDemand > 0) {
                    totalDemand += classDemand;
                }
            }
        }
        return totalDemand;
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
                    dailyLhCapacity = capacityVo.getDayVulcanizationQty() / 2; // 日硫化量是双模的，需要除以2得到单模产量
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
        if (scheduleDayTypeHelper.isStopDay(scheduleDate, context.getFactoryCode())) {
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

        // Step 3: 整车取整（试制任务不补整车，直接用实际需求量）
        int tripCapacity = getTripCapacity(task.getStructureName(), context);
        int plannedProduction;
        int requiredCars;
        if (Boolean.TRUE.equals(task.getIsTrialTask()) || Boolean.TRUE.equals(task.getIsProductionTrial())) {
            // 试制任务：不补整车，使用实际需求量
            plannedProduction = requiredProductionValue;
            requiredCars = tripCapacity > 0 ? requiredProductionValue / tripCapacity : 0;
        } else {
            // 普通任务：整车取整
            plannedProduction = productionCalculator.roundToVehicle(requiredProductionValue, tripCapacity);
            requiredCars = tripCapacity > 0 ? (plannedProduction + tripCapacity - 1) / tripCapacity : 0;
        }
        task.setPlannedProduction(plannedProduction);
        task.setRequiredCars(requiredCars);
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
        if (!Boolean.TRUE.equals(task.getIsMainProduct()) && endingSurplusQty <= getEndingDiscardThreshold(context)) {
            // 非主销产品 + 收尾余量≤2条，舍弃当天排产
            task.setPlannedProduction(0);
            task.setRequiredCars(0);
            task.setEndingExtraInventory(0);
            task.setEndingAbandoned(true);
            task.setEndingAbandonedQty(endingSurplusQty);
            task.setIsLastEndingBatch(true);
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
            if (endingSurplusQty > 0 && endingSurplusQty < tripCapacity) {
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
     * <p>每个班次处理时调用，顺序判断：
     * <ol>
     *   <li>已停产日（isStopDay）→ 产量=0</li>
     *   <li>当前班次停产/停产前一个班次/停产标识日 → handleClosingDayTaskV2（反推封顶）</li>
     *   <li>开产班次（OPEN_START）→ handleOpeningDayTaskV2（6/24备货）</li>
     *   <li>明天有停产 → 跨天封顶</li>
     * </ol>
     *
     * @param task      胎胚任务
     * @param context   排程上下文
     * @param dayShifts 当前班次配置
     */
    private void handleOpeningClosingDay(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                          ScheduleContextVo context,
                                          List<CxShiftConfig> dayShifts) {
        LocalDate scheduleDate = context.getCurrentScheduleDate();
        String factoryCode = context.getFactoryCode();

        // 获取当前班次信息
        CxShiftConfig currentShift = dayShifts != null && dayShifts.size() == 1 ? dayShifts.get(0) : null;
        int currentDayShiftOrder = currentShift != null && currentShift.getDayShiftOrder() != null
                ? currentShift.getDayShiftOrder() : 0;

        // ==================== 停产日（已停产）：当天产量设为0 ====================
        if (scheduleDayTypeHelper.isStopDay(scheduleDate, factoryCode)) {
            task.setPlannedProduction(0);
            task.setRequiredCars(0);
            task.setEndingExtraInventory(0);
            return;
        }

        // ==================== 停产逻辑调整（v2）====================
        // 每个班次都检查：今天有没有包含停产班次
        // 通过一次 determineShiftType 获取班次类型，避免重复调用
        ScheduleDayTypeHelper.ShiftType shiftType = scheduleDayTypeHelper.determineShiftType(
                scheduleDate, currentDayShiftOrder, factoryCode);
        boolean isCurrentClosingShift = shiftType == ScheduleDayTypeHelper.ShiftType.CLOSED;
        boolean isBeforeClosingShift = shiftType == ScheduleDayTypeHelper.ShiftType.BEFORE_CLOSE;
        // 判断条件3：当前班次本身是否是停产标识日的班次（包含停产班次的当天）
        boolean isStopFlagDayToday = scheduleDayTypeHelper.isStopFlagDay(scheduleDate, factoryCode);

        if (isCurrentClosingShift || isBeforeClosingShift || isStopFlagDayToday) {
            log.info("当前班次事件: 工厂={}, 日期={}, 当天第{}班, 类型={}, 停产标识日={}",
                    factoryCode, scheduleDate, currentDayShiftOrder,
                    isCurrentClosingShift ? "停产班"
                            : isBeforeClosingShift ? "停产前一个班次(下个班次停产)"
                            : "停产标识日",
                    isStopFlagDayToday);
            // 今天包含停产班次，走停产逻辑
            handleClosingDayTaskV2(task, context, scheduleDate, currentDayShiftOrder, dayShifts);
            return;
        }

        // ==================== 停产日前一天封顶 ====================
        // 如果明天有停产班次，需跨天封顶当前班次的产量
        // 避免前一个班次过量生产，导致停产后库存过剩（此检查必须在开产日前，避免被截断）
        LocalDate nextDay = scheduleDate.plusDays(1);
        boolean isNextDayStop = scheduleDayTypeHelper.hasAnyClosingShift(nextDay, factoryCode);

        // ==================== 开产处理（仅 OPEN_START 班次）====================
        boolean isOpening = shiftType == ScheduleDayTypeHelper.ShiftType.OPEN_START;
        if (isOpening) {
            handleOpeningDayTaskV2(task, context, scheduleDate, currentDayShiftOrder, dayShifts);
            if (isNextDayStop) {
                int closingRequiredStock = calculateClosingRequiredStockV2(task, context, scheduleDate, currentDayShiftOrder, dayShifts);
                int currentStock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;
                int thisShiftNeeded = Math.max(0, closingRequiredStock - currentStock);
                int normalDemand = task.getPlannedProduction() != null ? task.getPlannedProduction() : 0;
                int cappedProduction = Math.min(normalDemand, thisShiftNeeded);
                log.info("跨天封顶(明天{}有停产,开产日): 胎胚={}, 反推需求={}, 库存={}, 还需={}, 正常需求={}, 封顶={}",
                        nextDay, task.getEmbryoCode(), closingRequiredStock, currentStock,
                        thisShiftNeeded, normalDemand, cappedProduction);
                int tripCapacity = getTripCapacity(task.getStructureName(), context);
                int roundedProduction = productionCalculator.roundToVehicle(cappedProduction, tripCapacity);
                task.setPlannedProduction(roundedProduction);
                task.setEndingExtraInventory(roundedProduction);
                task.setRequiredCars(tripCapacity > 0 ? (roundedProduction + tripCapacity - 1) / tripCapacity : 0);
            }
            return;
        }

        if (isNextDayStop) {
            int closingRequiredStock = calculateClosingRequiredStockV2(task, context, scheduleDate, currentDayShiftOrder, dayShifts);
            int currentStock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;
            int thisShiftNeeded = Math.max(0, closingRequiredStock - currentStock);
            int normalDemand = task.getPlannedProduction() != null ? task.getPlannedProduction() : 0;
            int cappedProduction = Math.min(normalDemand, thisShiftNeeded);
            log.info("跨天封顶(明天{}有停产): 胎胚={}, 反推需求={}, 库存={}, 还需={}, 正常需求={}, 封顶={}",
                    nextDay, task.getEmbryoCode(), closingRequiredStock, currentStock,
                    thisShiftNeeded, normalDemand, cappedProduction);
            int tripCapacity = getTripCapacity(task.getStructureName(), context);
            int roundedProduction = productionCalculator.roundToVehicle(cappedProduction, tripCapacity);
            task.setPlannedProduction(roundedProduction);
            task.setEndingExtraInventory(roundedProduction);
            task.setRequiredCars(tripCapacity > 0 ? (roundedProduction + tripCapacity - 1) / tripCapacity : 0);
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
    /**
     * 停产任务处理：每个班次检查是否包含停产班次，按反推公式计算
     *
     * <p>调整逻辑：
     * <ul>
     *   <li>每个班次进来都要检查今天有没有包含停产班次</li>
     *   <li>如果包含停产班次，依据硫化停锅时间倒推当前班次到停产班次还需生成的量</li>
     *   <li>反推公式：反推总量 = (停锅时间 - 当前班次开始时间 - 预留消化时间) / 单胎单模时长 × 模数</li>
     *   <li>封顶：取收尾后实需(endingExtraInventory)和反推需求中的较小值</li>
     *   <li>如果任务之前走了收尾余量处理，以停产为优先调整回来</li>
     * </ul>
     *
     * @param task               胎胚任务
     * @param context            排程上下文
     * @param scheduleDate       排程日期
     * @param currentDayShiftOrder 当前班次序号
     * @param dayShifts          当前班次配置
     */
    private void handleClosingDayTaskV2(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                         ScheduleContextVo context,
                                         LocalDate scheduleDate,
                                         int currentDayShiftOrder,
                                         List<CxShiftConfig> dayShifts) {
        // 标记为停产日任务
        task.setIsClosingDayTask(true);

        // ==================== 判断当前班次本身是否为停产班次 ====================
        // 如果当前班次本身是停产班次（day_flag="0"），则不生产，产量为0
        boolean isCurrentClosingShift = scheduleDayTypeHelper.isClosingShift(scheduleDate, currentDayShiftOrder, context.getFactoryCode());
        if (isCurrentClosingShift) {
            log.info("当前班次 {} 是停产班次，产量设为0", currentDayShiftOrder);
            task.setPlannedProduction(0);
            task.setRequiredCars(0);
            task.setEndingExtraInventory(0);
            return;
        }

        // 确定停锅班次
        Integer closingShiftOrder = determineClosingShiftOrder(context);
        task.setClosingShiftOrder(closingShiftOrder);

        if (closingShiftOrder == null) {
            log.warn("停产日 {} 无法确定停锅班次，保持原计划量", scheduleDate);
            return;
        }

        // ==================== 计算反推总量（新公式）====================
        // 反推总量 = (停锅时间 - 当前班次开始时间 - 预留消化时间) / 单胎单模时长 × 模数
        int closingRequiredStock = calculateClosingRequiredStockV2(task, context, scheduleDate, currentDayShiftOrder, dayShifts);
        task.setClosingRequiredStock(closingRequiredStock);

        int currentStock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;
        int endingInventory = task.getEndingExtraInventory() != null ? task.getEndingExtraInventory() : 0;

        // 当前班次到停机时间还需的量
        int thisShiftNeeded = Math.max(0, closingRequiredStock - currentStock);

        // 封顶：取 endingExtraInventory（收尾/试制调整后的量）和反推需求中的较小值
        // 如果 endingExtraInventory < 反推需求，说明收尾已经在限制了，停产不放大
        int cappedProduction = Math.min(endingInventory, thisShiftNeeded);

        log.info("停产反推封顶: 胎胚={}, 停锅班次=当天第{}班, 反推需胎胚={}, 当前库存={}, 收尾后实需={}, 还需生产={}, 封顶={}, 当前班次=当天第{}班",
                task.getEmbryoCode(), closingShiftOrder, closingRequiredStock,
                currentStock, endingInventory, thisShiftNeeded, cappedProduction, currentDayShiftOrder);

        // ==================== 如果之前走了收尾余量处理，以停产为优先调整回来 ====================
        if (Boolean.TRUE.equals(task.getIsLastEndingBatch())) {
            // 收尾任务被停产逻辑覆盖，需要调整回来
            int tripCapacity = getTripCapacity(task.getStructureName(), context);
            log.info("停产优先于收尾：embryoCode={}, 原endingExtraInventory={}, 调整为cappedProduction={}, 原requiredCars={}",
                    task.getEmbryoCode(), task.getEndingExtraInventory(), cappedProduction, task.getRequiredCars());
            task.setEndingExtraInventory(cappedProduction);
            task.setPlannedProduction(cappedProduction);
            task.setRequiredCars(cappedProduction > 0 ? (cappedCapacity(task, cappedProduction, tripCapacity, closingShiftOrder, currentDayShiftOrder)) : 0);
            return;
        }

        // ==================== 正常停产封顶逻辑 ====================
        int tripCapacity = getTripCapacity(task.getStructureName(), context);

        // 如果当前班次是停锅班次，不补整车（按实量下）
        if (currentDayShiftOrder == closingShiftOrder) {
            // 不补整车：用封顶量直接作为 endingExtraInventory
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
            int roundedProduction = productionCalculator.roundToVehicle(cappedProduction, tripCapacity);
            task.setPlannedProduction(roundedProduction);
            task.setEndingExtraInventory(roundedProduction);
            task.setRequiredCars(tripCapacity > 0
                    ? (roundedProduction + tripCapacity - 1) / tripCapacity : 0);
        }
        // currentDayShiftOrder > closingShiftOrder 不应出现（已被班次停产跳过）
    }

    /**
     * 辅助方法：根据班次与停锅班次的关系计算 requiredCars
     */
    private int cappedCapacity(CoreScheduleAlgorithmService.DailyEmbryoTask task, int cappedProduction,
                                int tripCapacity, int closingShiftOrder, int currentDayShiftOrder) {
        if (currentDayShiftOrder == closingShiftOrder) {
            return cappedProduction > 0 ? 1 : 0; // 停锅班次不补整车
        } else {
            return tripCapacity > 0 ? (cappedProduction + tripCapacity - 1) / tripCapacity : 0;
        }
    }

    /**
     * 计算停产反推总量 V2
     *
     * <p>新公式：反推总量 = (停锅时间 - 当前班次开始时间 - 预留消化时间) / 单胎单模时长 × 模数
     *
     * @param task               胎胚任务
     * @param context            排程上下文
     * @param scheduleDate       排程日期
     * @param currentDayShiftOrder 当前班次序号
     * @param dayShifts          当前班次配置
     * @return 反推总量（条数）
     */
    private int calculateClosingRequiredStockV2(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                                 ScheduleContextVo context,
                                                 LocalDate scheduleDate,
                                                 int currentDayShiftOrder,
                                                 List<CxShiftConfig> dayShifts) {
        // 从硫化排程结果反推
        LhScheduleResult lhResult = findLhResultByTask(task, context);
        if (lhResult == null) {
            log.warn("停产反推V2：无法找到胎胚 {} 对应的硫化排程结果，使用默认0", task.getEmbryoCode());
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

        // ==================== 获取停锅时间（优先使用完整日期时间）====================
        LocalDateTime vulcanizingStopDateTime = context.getVulcanizingStopDateTime();
        LocalDateTime stopTime;
        if (vulcanizingStopDateTime != null) {
            stopTime = vulcanizingStopDateTime;
        } else {
            // 回退：用 vulcanizingStopTimeStr(HH:mm) + 排程日期构造
            String vulcanizingStopTimeStr = context.getVulcanizingStopTimeStr();
            if (vulcanizingStopTimeStr == null || vulcanizingStopTimeStr.isEmpty()) {
                log.warn("停产反推V2：未配置硫化停锅时间，无法计算");
                return 0;
            }
            try {
                String timePart = vulcanizingStopTimeStr.length() >= 5
                        ? vulcanizingStopTimeStr.substring(0, 5) : vulcanizingStopTimeStr;
                stopTime = LocalDateTime.of(scheduleDate,
                        java.time.LocalTime.parse(timePart));
            } catch (Exception e) {
                log.warn("停产反推V2：解析停锅时间失败: {}", vulcanizingStopTimeStr);
                return 0;
            }
        }

        // ==================== 获取当前班次开始时间 ====================
        LocalDateTime shiftStartTime = getShiftStartDateTime(scheduleDate, dayShifts);
        if (shiftStartTime == null) {
            log.warn("停产反推V2：无法获取当前班次 {} 的开始时间", currentDayShiftOrder);
            return 0;
        }

        // 预留消化时间
        int reservedDigestHours = context.getReservedDigestHours() != null ? context.getReservedDigestHours() : 1;

        // ==================== 计算反推总量 ====================
        // 反推总量 = (停锅时间 - 当前班次开始时间 - 预留消化时间) / 单胎单模时长 × 模数
        long durationSeconds = java.time.Duration.between(shiftStartTime, stopTime).getSeconds();
        durationSeconds -= (long) reservedDigestHours * 3600;

        if (durationSeconds <= 0) {
            log.info("停产反推V2: embryoCode={}, 停锅时间{}早于当前班次开始时间{}+消化时间{}h，反推总量=0",
                    task.getEmbryoCode(), stopTime, shiftStartTime, reservedDigestHours);
            return 0;
        }

        int requiredStock = (int) ((double) durationSeconds / singleTireMoldSeconds * moldQty);

        log.info("停产反推总量: 胎胚={}, 单模日硫化量={}, 模数={}, 单胎时长={}s, 停锅={}, 当前班次开始={}, 消化={}h, 可用={}s, 需胎胚={}",
                task.getEmbryoCode(), dailyLhCapacity, moldQty,
                String.format("%.1f", singleTireMoldSeconds), stopTime, shiftStartTime, reservedDigestHours,
                durationSeconds, requiredStock);

        return requiredStock;
    }

    /**
     * 获取班次开始时间（LocalDateTime）
     *
     * @param scheduleDate       排程日期
     * @param dayShiftOrder      班次序号
     * @param context            排程上下文
     * @return 班次开始时间
     */
    private LocalDateTime getShiftStartDateTime(LocalDate scheduleDate, List<CxShiftConfig> dayShifts) {
        if (dayShifts == null || dayShifts.isEmpty()) {
            return null;
        }
        CxShiftConfig currentShift = dayShifts.get(0);
        LocalTime startTime = currentShift.getShiftStartTime();
        LocalTime endTime = currentShift.getShiftEndTime();
        if (startTime == null) {
            return null;
        }
        // 跨天班次（endTime < startTime，如 22:00~05:59）的实际开始日期在前一天
        // 例如 NIGHT_D2: scheduleDate=2026-05-19, 22:00~05:59 → 2026-05-18T22:00
        // DAY_D2: scheduleDate=2026-05-19, 06:00~14:00 → 2026-05-19T06:00
        LocalDate startDate = scheduleDate;
        if (endTime != null && !endTime.isAfter(startTime)) {
            startDate = scheduleDate.minusDays(1);
        }
        return LocalDateTime.of(startDate, startTime);
    }

    /**
     * 开产日任务处理（开产基准量已在 groupTasks 中提前算出并存于 openingShiftCapacity）
     *
     * <p>逻辑：
     * <ol>
     *   <li>取 groupTasks 中预存的 openingShiftCapacity（开产基准量，向下取整到整车）</li>
     *   <li>与收尾/试制调整后的 endingExtraInventory 比较，取较小值</li>
     * </ol>
     *
     * <p>注意：关键产品已在 groupTasks 中提前过滤，不会进入此方法
     *
     * @param task               胎胚任务
     * @param context            排程上下文
     * @param scheduleDate       排程日期
     * @param currentDayShiftOrder 当前班次序号
     * @param dayShifts          当前班次配置
     */
    private void handleOpeningDayTaskV2(CoreScheduleAlgorithmService.DailyEmbryoTask task,
                                         ScheduleContextVo context,
                                         LocalDate scheduleDate,
                                         int currentDayShiftOrder,
                                         List<CxShiftConfig> dayShifts) {
        task.setIsOpeningDayTask(true);

        // 收尾/试制已正常算完（vulcanizeDemand 已在循环中被更新为下游 CLASS 计划量）
        // 开产基准(openingShiftCapacity)为 6/24 兜底值，仅在无实需时使用
        int openingBase = task.getOpeningShiftCapacity() != null ? task.getOpeningShiftCapacity() : 0;
        int endingAdjusted = task.getEndingExtraInventory() != null ? task.getEndingExtraInventory() : 0;

        int finalProduction;
        if (endingAdjusted > 0) {
            // 有实需（正常产量或收尾限制），以实需为准，不受开产基准限制
            finalProduction = endingAdjusted;
        } else if (Boolean.TRUE.equals(task.getIsLastEndingBatch())) {
            // 收尾明确舍弃
            finalProduction = 0;
        } else {
            // 无 CLASS 计划量，用开产基准兜底
            finalProduction = openingBase;
        }

        task.setPlannedProduction(finalProduction);
        task.setEndingExtraInventory(finalProduction);

        int tripCapacity = getTripCapacity(task.getStructureName(), context);
        task.setRequiredCars(tripCapacity > 0 ? (finalProduction + tripCapacity - 1) / tripCapacity : 0);

        log.info("开产日排产: 胎胚={}, 开产基准={}, 收尾后实需={}, 最终产量={}, 需车={}",
                task.getEmbryoCode(), openingBase, endingAdjusted, finalProduction,
                tripCapacity > 0 ? (finalProduction + tripCapacity - 1) / tripCapacity : 0);
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
        LocalDateTime stopDateTime = context.getVulcanizingStopDateTime();
        if (stopDateTime == null) {
            // 回退：只有 HH:mm 格式，用旧方法按时分匹配
            String vulcanizingStopTimeStr = context.getVulcanizingStopTimeStr();
            if (vulcanizingStopTimeStr == null || vulcanizingStopTimeStr.isEmpty()) {
                log.warn("未配置硫化机停锅时间(VULCANIZING_STOP_TIME)，无法确定停锅班次");
                return null;
            }
            List<CxShiftConfig> shiftConfigs = getSortedShiftConfigs(context);
            String timePart = extractTimePart(vulcanizingStopTimeStr);
            return scheduleDayTypeHelper.getShiftOrderByTime(timePart, shiftConfigs);
        }

        // 使用完整日期时间匹配：遍历所有班次，结合排程日期算出每个班次的实际起止时间
        return findShiftOrderByDateTime(stopDateTime, context);
    }

    /**
     * 从时间字符串中提取 HH:mm 格式的时间部分
     * 支持格式： "2026-05-19 05:30" -> "05:30" 或 "05:30" -> "05:30"
     */
    private String extractTimePart(String dateTimeStr) {
        if (dateTimeStr == null) {
            return null;
        }
        String trimmed = dateTimeStr.trim();
        // 如果包含空格，取空格后的部分
        if (trimmed.contains(" ")) {
            String[] parts = trimmed.split("\\s+");
            if (parts.length >= 2) {
                trimmed = parts[parts.length - 1]; // 取最后一部分（时间部分）
            }
        }
        // 格式化，确保是 HH:mm 格式（补零）
        try {
            String[] timeParts = trimmed.split(":");
            if (timeParts.length >= 2) {
                String hour = timeParts[0].trim();
                String minute = timeParts[1].trim();
                // 补零
                if (hour.length() == 1) {
                    hour = "0" + hour;
                }
                if (minute.length() == 1) {
                    minute = "0" + minute;
                }
                return hour + ":" + minute;
            }
        } catch (Exception e) {
            log.warn("解析时间字符串失败: {}", dateTimeStr);
        }
        return trimmed;
    }

    /**
     * 根据完整日期时间查找对应的班次序号
     *
     * <p>遍历所有班次配置，结合排程日期算出每个班次的实际起止时间范围，
     * 找到目标时间落在哪个班次内。支持跨天班次。
     *
     * @param dateTime 目标日期时间（如停锅时间 2026-05-19T05:30）
     * @param context  排程上下文
     * @return 班次序号（dayShiftOrder），找不到返回第一个班次序号
     */
    private Integer findShiftOrderByDateTime(LocalDateTime dateTime, ScheduleContextVo context) {
        LocalDate scheduleDate = context.getScheduleDate();
        if (scheduleDate == null) {
            return null;
        }
        // 排程起始日期：前端传入最后一天，往前推2天
        LocalDate scheduleStartDate = scheduleDate.minusDays(2);

        List<CxShiftConfig> allShifts = context.getShiftConfigList();
        if (allShifts == null || allShifts.isEmpty()) {
            return null;
        }

        for (CxShiftConfig shift : allShifts) {
            if (shift.getScheduleDay() == null || shift.getDayShiftOrder() == null) continue;

            LocalDate shiftDate = scheduleStartDate.plusDays(shift.getScheduleDay() - 1);
            LocalTime shiftStartTime = shift.getShiftStartTime();
            LocalTime shiftEndTime = shift.getShiftEndTime();

            LocalDateTime shiftStart = LocalDateTime.of(shiftDate, shiftStartTime);
            LocalDateTime shiftEnd = LocalDateTime.of(shiftDate, shiftEndTime);

            // 跨天班次：endTime <= startTime，结束时间加1天
            if (!shiftEnd.isAfter(shiftStart)) {
                shiftEnd = shiftEnd.plusDays(1);
            }

            if (!dateTime.isBefore(shiftStart) && dateTime.isBefore(shiftEnd)) {
                return shift.getDayShiftOrder();
            }
        }

        // 兜底：取第一个班次
        CxShiftConfig firstShift = allShifts.stream()
                .filter(s -> s.getScheduleDay() != null && s.getDayShiftOrder() != null)
                .min(Comparator.comparingInt(CxShiftConfig::getScheduleDay)
                        .thenComparingInt(CxShiftConfig::getDayShiftOrder))
                .orElse(null);
        return firstShift != null ? firstShift.getDayShiftOrder() : null;
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
                return vo.getDayVulcanizationQty() / 2;
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
                return vo.getDayVulcanizationQty() / 2;
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

    // ==================== 参数配置获取方法 ====================

    /**
     * 获取收尾舍弃阈值：非主销产品余量≤此值时舍弃
     * 优先使用参数配置，否则使用默认值
     */
    private int getEndingDiscardThreshold(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_ENDING_DISCARD_THRESHOLD);
            if (config != null && config.getParamValue() != null) {
                try {
                    return Integer.parseInt(config.getParamValue());
                } catch (NumberFormatException e) {
                    log.warn("解析收尾舍弃阈值配置失败: {}", config.getParamValue());
                }
            }
        }
        return DEFAULT_ENDING_DISCARD_THRESHOLD;
    }

    /**
     * 获取成型余量紧急阈值：成型余量低于此值标记为紧急收尾
     * 优先使用参数配置，否则使用默认值
     */
    private int getEndingUrgentFormingRemainder(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_ENDING_URGENT_FORMING_REMAINDER);
            if (config != null && config.getParamValue() != null) {
                try {
                    return Integer.parseInt(config.getParamValue());
                } catch (NumberFormatException e) {
                    log.warn("解析成型余量紧急阈值配置失败: {}", config.getParamValue());
                }
            }
        }
        return DEFAULT_ENDING_URGENT_FORMING_REMAINDER;
    }

    /**
     * 获取近期收尾天数阈值（10天内）
     * 优先使用参数配置，否则使用默认值
     */
    private int getEndingDaysThreshold(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_ENDING_DAYS_THRESHOLD);
            if (config != null && config.getParamValue() != null) {
                try {
                    return Integer.parseInt(config.getParamValue());
                } catch (NumberFormatException e) {
                    log.warn("解析近期收尾天数阈值配置失败: {}", config.getParamValue());
                }
            }
        }
        return DEFAULT_ENDING_DAYS_THRESHOLD;
    }

    /**
     * 获取紧急收尾天数阈值（3天内）
     * 优先使用参数配置，否则使用默认值
     */
    private int getUrgentEndingDays(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_URGENT_ENDING_DAYS);
            if (config != null && config.getParamValue() != null) {
                try {
                    return Integer.parseInt(config.getParamValue());
                } catch (NumberFormatException e) {
                    log.warn("解析紧急收尾天数阈值配置失败: {}", config.getParamValue());
                }
            }
        }
        return DEFAULT_URGENT_ENDING_DAYS;
    }
}
