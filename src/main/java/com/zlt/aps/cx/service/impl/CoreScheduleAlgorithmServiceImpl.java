package com.zlt.aps.cx.service.impl;

import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.service.engine.*;
import com.zlt.aps.cx.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 核心排程算法服务实现类
 *
 * <p>负责排程主流程编排，具体业务逻辑委托给各专门服务：
 * <ul>
 *   <li>{@link TaskGroupService} - 任务分组与属性计算</li>
 *   <li>{@link ContinueTaskProcessor} - 续作任务处理</li>
 *   <li>{@link TrialTaskProcessor} - 试制任务处理</li>
 *   <li>{@link NewTaskProcessor} - 新增任务处理（含量试约束）</li>
 *   <li>{@link ShiftScheduleService} - 班次精排</li>
 *   <li>{@link BalancingService} - 班次间生产量均衡</li>
 * </ul>
 *
 * <p>排程主流程：
 * <ol>
 *   <li>按天循环排程（共排8个班次，约3天）</li>
 *   <li>每天：任务分组 → 续作处理 → 试制处理 → 新增处理 → 班次精排</li>
 *   <li>每天排完后更新上下文（库存/余量/在机信息）</li>
 *   <li>汇总多天结果，按 机台+胎胚+物料编号 维度生成单表排程数据</li>
 * </ol>
 *
 * @author APS Team
 */
@Slf4j
@Service
public class CoreScheduleAlgorithmServiceImpl implements CoreScheduleAlgorithmService {

    /** taskGroupService 使用 @Lazy 延迟注入，打破循环依赖 */
    @Autowired
    @Lazy
    private TaskGroupService taskGroupService;
    private final ContinueTaskProcessor continueTaskProcessor;
    private final TrialTaskProcessor trialTaskProcessor;
    private final NewTaskProcessor newTaskProcessor;
    private final ShiftScheduleService shiftScheduleService;
    private final ProductionCalculator productionCalculator;
    private final ScheduleDayTypeHelper scheduleDayTypeHelper;
    private final BalancingService balancingService;

    /** 构造函数注入 */
    @Autowired
    public CoreScheduleAlgorithmServiceImpl(
            @Lazy ContinueTaskProcessor continueTaskProcessor,
            @Lazy TrialTaskProcessor trialTaskProcessor,
            @Lazy NewTaskProcessor newTaskProcessor,
            @Lazy ShiftScheduleService shiftScheduleService,
            @Lazy ProductionCalculator productionCalculator,
            ScheduleDayTypeHelper scheduleDayTypeHelper,
            @Lazy BalancingService balancingService) {
        this.continueTaskProcessor = continueTaskProcessor;
        this.trialTaskProcessor = trialTaskProcessor;
        this.newTaskProcessor = newTaskProcessor;
        this.shiftScheduleService = shiftScheduleService;
        this.productionCalculator = productionCalculator;
        this.scheduleDayTypeHelper = scheduleDayTypeHelper;
        this.balancingService = balancingService;
    }

    /** 默认排程天数 */
    private static final int DEFAULT_SCHEDULE_DAYS = 3;

    /** 排程起始偏移天数：前端传入最后一天，需要往前推2天开始排产 */
    private static final int SCHEDULE_START_OFFSET_DAYS = 2;

    @Override
    public List<CxScheduleResult> executeSchedule(ScheduleContextVo context) {
        log.info("开始执行排程算法，日期: {}", context.getScheduleDate());

        // 预加载工作日历缓存，避免后续频繁数据库查询
        LocalDate scheduleDate = context.getScheduleDate();
        int scheduleDays = context.getScheduleDays() != null ? context.getScheduleDays() : DEFAULT_SCHEDULE_DAYS;
        if (scheduleDate != null) {
            scheduleDayTypeHelper.preloadCache(scheduleDate, scheduleDate.plusDays(scheduleDays - 1));
        }

        // 使用 ScheduleServiceImpl.buildScheduleContext 中已加载的班次配置
        List<CxShiftConfig> allShiftConfigs = context.getShiftConfigList();
        if (allShiftConfigs == null || allShiftConfigs.isEmpty()) {
            log.error("班次配置为空，请先调用 buildScheduleContext 加载班次配置");
            return new ArrayList<>();
        }

        // 按排程天数和班次序号排序
        List<CxShiftConfig> sortedShiftConfigs = allShiftConfigs.stream()
                .filter(c -> c.getScheduleDay() != null)
                .sorted(Comparator.comparingInt(CxShiftConfig::getScheduleDay)
                        .thenComparingInt(c -> c.getDayShiftOrder() != null ? c.getDayShiftOrder() : 0))
                .collect(Collectors.toList());

        // 按天分组班次
        Map<Integer, List<CxShiftConfig>> dayShiftMap = sortedShiftConfigs.stream()
                .collect(Collectors.groupingBy(CxShiftConfig::getScheduleDay, LinkedHashMap::new, Collectors.toList()));

        // 收集每个班次的排产结果
        List<ShiftScheduleResult> shiftResults = new ArrayList<>();

        // 记录机台在产状态（跨班次持续更新）
        Map<String, Set<String>> machineOnlineEmbryoMap = context.getMachineOnlineEmbryoMap();
        if (machineOnlineEmbryoMap == null) {
            machineOnlineEmbryoMap = new HashMap<>();
        }

        // 按天执行排程
        int dayIndex = 0;
        for (Map.Entry<Integer, List<CxShiftConfig>> dayEntry : dayShiftMap.entrySet()) {
            int day = dayEntry.getKey();
            List<CxShiftConfig> dayShifts = dayEntry.getValue();
            LocalDate currentScheduleDate = context.getScheduleDate()
                    .minusDays(SCHEDULE_START_OFFSET_DAYS).plusDays(day - 1);

            // 检查当前天是否是整天停产
            if (scheduleDayTypeHelper.isFullDayStopped(currentScheduleDate)) {
                log.info("第 {} 天日期 {} 整天停产，跳过该天排程", day, currentScheduleDate);
                continue;
            }

            dayIndex++;
            log.info("====== 开始执行第 {} 天排程，天={}, 日期={}, 可用班次数={} ======",
                    dayIndex, day, currentScheduleDate, dayShifts.size());

            // 过滤掉停产的班次
            List<CxShiftConfig> activeShifts = dayShifts.stream()
                    .filter(s -> !scheduleDayTypeHelper.isShiftStopped(currentScheduleDate, s.getDayShiftOrder()))
                    .collect(Collectors.toList());

            if (activeShifts.isEmpty()) {
                log.info("第 {} 天日期 {} 所有班次均停产，跳过", day, currentScheduleDate);
                continue;
            }

            // 设置当前天的上下文
            context.setCurrentScheduleDay(day);
            context.setCurrentScheduleDate(currentScheduleDate);
            context.setCurrentShiftConfigs(activeShifts);

            // 执行该天的排程（天维度分组 + DFS均衡 + 班次均衡分配 + 逐班次精排）
            List<ShiftScheduleResult> dayResults = executeDaySchedule(
                    context, day, activeShifts, currentScheduleDate, machineOnlineEmbryoMap);
            shiftResults.addAll(dayResults);

            // 更新机台在产状态（使用最后一个班次的结果）
            if (!dayResults.isEmpty()) {
                ShiftScheduleResult lastShiftResult = dayResults.get(dayResults.size() - 1);
                machineOnlineEmbryoMap = updateMachineOnlineStatus(
                        lastShiftResult.getAllAllocations(), machineOnlineEmbryoMap);
            }

            log.info("====== 第 {} 天排程完成，天={}, 共 {} 个班次 ======\n", dayIndex, day, dayResults.size());
        }

        // ==================== 合并多班次结果 ====================
        List<CxScheduleResult> allResults = buildFinalScheduleResultsFromShifts(context, shiftResults, allShiftConfigs);

        // ==================== 构建子表 ====================
        List<CxScheduleDetail> allDetails = buildScheduleDetailsFromShifts(context, shiftResults, allShiftConfigs);
        log.info("子表记录构建完成，共 {} 条", allDetails.size());

        // ==================== 将子表明细关联到主表 ====================
        associateDetailsToResults(allResults, allDetails);

    /**
     * 将子表明细关联到主表
     */
    private void associateDetailsToResults(List<CxScheduleResult> allResults, List<CxScheduleDetail> allDetails) {
        if (allDetails.isEmpty()) {
            return;
        }

        // 按 机台+胎胚 分组子表
        Map<String, List<CxScheduleDetail>> detailGroupMap = allDetails.stream()
                .collect(Collectors.groupingBy(d -> d.getCxMachineCode() + "|" + d.getEmbryoCode()));

        int matched = 0;
        for (CxScheduleResult result : allResults) {
            String key = result.getCxMachineCode() + "|" + result.getEmbryoCode();
            List<CxScheduleDetail> details = detailGroupMap.get(key);
            if (details != null) {
                result.setDetails(details);
                matched += details.size();
            }
        }
        log.info("子表关联主表完成：子表 {} 条，成功关联 {} 条", allDetails.size(), matched);
    }


        log.info("排程算法执行完成，共 {} 天，总机台数: {}", dayIndex, allResults.size());
        return allResults;
    }

    /**
     * 执行单天排程（天维度分组 + DFS均衡 + 班次均衡分配 + 逐班次精排）
     *
     * <p>排程流程：
     * <ol>
     *   <li>S5.2 任务分组：续作/试制/新增三类（天维度，含全天需求）</li>
     *   <li>S5.3 处理续作任务（DFS均衡，天维度总量）</li>
     *   <li>S5.3 处理试制任务</li>
     *   <li>S5.3 处理新增任务（DFS均衡，天维度总量）</li>
     *   <li>S5.3.6 天维度→班次均衡分配（将天总量按班次产能比例切分）</li>
     *   <li>S5.3.7 逐班次精排（每个班次消耗预分配量，处理开产/停产/收尾等特殊逻辑）</li>
     * </ol>
     */
    private List<ShiftScheduleResult> executeDaySchedule(
            ScheduleContextVo context,
            int day,
            List<CxShiftConfig> activeShifts,
            LocalDate scheduleDate,
            Map<String, Set<String>> machineOnlineEmbryoMap) {

        log.info("========== 开始执行天排程，天={}, 日期={}, 班次数={} ==========",
                day, scheduleDate, activeShifts.size());

        // ==================== 第一步：S5.2 任务分组（天维度，使用全天班次配置） ====================
        TaskGroupService.TaskGroupResult taskGroup = taskGroupService.groupTasks(
                context, machineOnlineEmbryoMap, scheduleDate, activeShifts);
        log.info("天维度任务分组完成：续作 {} 个，试制 {} 个，新增 {} 个",
                taskGroup.getContinueTasks().size(),
                taskGroup.getTrialTasks().size(),
                taskGroup.getNewTasks().size());

        // ==================== 第二步：S5.3 处理续作任务（天维度总量） ====================
        List<MachineAllocationResult> continueAllocations = continueTaskProcessor.processContinueTasks(
                taskGroup.getContinueTasks(), context, scheduleDate, activeShifts, day);
        log.info("续作任务处理完成，机台分配数: {}", continueAllocations.size());

        // ==================== 第三步：S5.3 处理试制任务 ====================
        List<MachineAllocationResult> trialAllocations = trialTaskProcessor.processTrialTasks(
                taskGroup.getTrialTasks(), context, scheduleDate, activeShifts, context.getAvailableMachines());
        log.info("试制任务处理完成，机台分配数: {}", trialAllocations.size());

        // ==================== 第四步：S5.3 处理新增任务（天维度总量） ====================
        List<MachineAllocationResult> newAllocations = newTaskProcessor.processNewTasks(
                taskGroup.getNewTasks(),
                context,
                scheduleDate,
                activeShifts,
                taskGroup.getContinueTasks(),
                continueAllocations,
                trialAllocations);
        log.info("新增任务处理完成，机台分配数: {}", newAllocations.size());

        // ==================== 第五步：合并分配结果 ====================
        List<MachineAllocationResult> allAllocations = new ArrayList<>();
        allAllocations.addAll(continueAllocations);
        allAllocations.addAll(newAllocations);
        allAllocations.addAll(trialAllocations);
        log.info("天维度分配结果合并完成，总分配数: {}", allAllocations.size());

        // ==================== 第六步：S5.3.6 天维度→班次均衡分配 ====================
        for (MachineAllocationResult allocation : allAllocations) {
            for (TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                // 从原始任务中复制停产/开产相关字段
                copyTaskTypeFields(taskAlloc, taskGroup);
                // 按班次产能比例均衡分配天总量
                distributeQuantityToShifts(taskAlloc, activeShifts, scheduleDate);
            }
        }

        // ==================== 第七步：S5.3.7 逐班次精排 ====================
        List<ShiftScheduleResult> shiftResults = new ArrayList<>();

        for (CxShiftConfig shiftConfig : activeShifts) {
            List<CxShiftConfig> singleShiftList = Collections.singletonList(shiftConfig);
            Integer dayShiftOrder = shiftConfig.getDayShiftOrder();

            log.info("----- 天={}, 日期={}, 班次={}, classField={} 精排开始 -----",
                    day, scheduleDate, shiftConfig.getShiftCode(), shiftConfig.getClassField());

            // 设置当前班次的上下文
            context.setCurrentShiftConfigs(singleShiftList);

            // 为当前班次构建精排任务列表（使用预分配量作为该班次的需求量）
            List<ShiftScheduleService.ShiftProductionResult> shiftProductionResults = new ArrayList<>();

            for (MachineAllocationResult allocation : allAllocations) {
                String machineCode = allocation.getMachineCode();
                for (TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                    // 获取该班次的预分配量
                    Integer shiftQty = taskAlloc.getShiftPreAllocatedQty() != null
                            ? taskAlloc.getShiftPreAllocatedQty().get(dayShiftOrder) : null;
                    if (shiftQty == null || shiftQty <= 0) {
                        continue; // 该班次没有分配到量，跳过
                    }

                    // 构建精排任务（用天维度的TaskAllocation信息 + 该班次的预分配量）
                    CoreScheduleAlgorithmService.DailyEmbryoTask task = buildShiftTask(taskAlloc, shiftQty, context);

                    log.info("班次精排: embryoCode={}, materialDesc={}, structureName={}, " +
                                    "dayQty(天总量)={}, shiftQty(本班预分配量)={}, shiftOrder={}, " +
                                    "isContinue={}, isTrial={}, isClosingDay={}, isOpeningDay={}",
                            taskAlloc.getEmbryoCode(), taskAlloc.getMaterialDesc(), taskAlloc.getStructureName(),
                            taskAlloc.getQuantity(), shiftQty, dayShiftOrder,
                            taskAlloc.getIsContinueTask(), taskAlloc.getIsTrialTask(),
                            taskAlloc.getIsClosingDayTask(), taskAlloc.getIsOpeningDayTask());

                    List<ShiftScheduleService.ShiftProductionResult> taskShiftResults =
                            shiftScheduleService.scheduleTaskToShifts(task, machineCode, context, singleShiftList, scheduleDate);
                    shiftProductionResults.addAll(taskShiftResults);
                }
            }
            log.info("班次精排完成，共 {} 条班次排产记录", shiftProductionResults.size());

            // 封装该班次排产结果
            ShiftScheduleResult shiftResult = new ShiftScheduleResult();
            shiftResult.setDay(day);
            shiftResult.setScheduleDate(scheduleDate);
            shiftResult.setShiftConfig(shiftConfig);
            shiftResult.setAllAllocations(allAllocations);
            shiftResult.setShiftProductionResults(shiftProductionResults);
            shiftResults.add(shiftResult);

            // 更新库存和硫化余量，供下一个班次精排使用
            updateContextForNextShift(context, allAllocations, singleShiftList);

            log.info("----- 天={}, 班次={} 精排结束 -----\n", day, shiftConfig.getShiftCode());
        }

        log.info("========== 天排程完成，天={}, 共 {} 个班次 ==========", day, shiftResults.size());
        return shiftResults;
    }

    /**
     * 从TaskGroupResult中复制任务类型字段到TaskAllocation
     */
    private void copyTaskTypeFields(TaskAllocation taskAlloc, TaskGroupService.TaskGroupResult taskGroup) {
        List<CoreScheduleAlgorithmService.DailyEmbryoTask> allTasks = new ArrayList<>();
        allTasks.addAll(taskGroup.getContinueTasks());
        allTasks.addAll(taskGroup.getNewTasks());
        allTasks.addAll(taskGroup.getTrialTasks());

        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : allTasks) {
            if (task.getEmbryoCode() != null && task.getEmbryoCode().equals(taskAlloc.getEmbryoCode())) {
                if (Boolean.TRUE.equals(task.getIsClosingDayTask())) {
                    taskAlloc.setIsClosingDayTask(true);
                    taskAlloc.setClosingShiftOrder(task.getClosingShiftOrder());
                }
                if (Boolean.TRUE.equals(task.getIsOpeningDayTask())) {
                    taskAlloc.setIsOpeningDayTask(true);
                    taskAlloc.setFormingOpeningShiftOrder(task.getFormingOpeningShiftOrder());
                    taskAlloc.setLhOpeningShiftOrder(task.getLhOpeningShiftOrder());
                }
                break;
            }
        }
    }

    /**
     * 将TaskAllocation的天维度总量按班次产能比例均衡分配到各班次
     *
     * <p>分配规则：
     * <ul>
     *   <li>试制任务：只在早班/中班排产</li>
     *   <li>停产任务：停锅班次及之后不排，需求前移到停锅前的班次</li>
     *   <li>开产任务：首班6h产能封顶</li>
     *   <li>普通/续作/收尾任务：按班次产能比例均衡</li>
     * </ul>
     */
    private void distributeQuantityToShifts(TaskAllocation taskAlloc,
                                            List<CxShiftConfig> activeShifts,
                                            LocalDate scheduleDate) {
        int dayTotalQty = taskAlloc.getQuantity() != null ? taskAlloc.getQuantity() : 0;
        if (dayTotalQty <= 0) {
            taskAlloc.setShiftPreAllocatedQty(Collections.emptyMap());
            return;
        }

        Map<Integer, Integer> shiftQtyMap = new LinkedHashMap<>();
        boolean isTrial = Boolean.TRUE.equals(taskAlloc.getIsTrialTask());
        boolean isClosingDay = Boolean.TRUE.equals(taskAlloc.getIsClosingDayTask());
        boolean isOpeningDay = Boolean.TRUE.equals(taskAlloc.getIsOpeningDayTask());

        // ---- 试制任务：只在早班/中班排产 ----
        if (isTrial) {
            for (CxShiftConfig shift : activeShifts) {
                Integer order = shift.getDayShiftOrder();
                if (order != null && (order == 2 || order == 3)) {
                    shiftQtyMap.put(order, dayTotalQty);
                }
            }
            if (shiftQtyMap.size() > 1) {
                int perShift = dayTotalQty / shiftQtyMap.size();
                int remainder = dayTotalQty % shiftQtyMap.size();
                int idx = 0;
                for (Map.Entry<Integer, Integer> entry : shiftQtyMap.entrySet()) {
                    entry.setValue(perShift + (idx < remainder ? 1 : 0));
                    idx++;
                }
            }
            taskAlloc.setShiftPreAllocatedQty(shiftQtyMap);
            log.info("  试制任务 {} 天总量={} → 班次预分配: {}", taskAlloc.getEmbryoCode(), dayTotalQty, shiftQtyMap);
            return;
        }

        // ---- 计算每个班次的产能比例 ----
        int[] shiftCapacities = new int[activeShifts.size()];
        int totalCapacity = 0;
        for (int i = 0; i < activeShifts.size(); i++) {
            CxShiftConfig shift = activeShifts.get(i);
            Integer order = shift.getDayShiftOrder();
            int hours = 8;
            if (isOpeningDay) {
                Integer formingOpeningShiftOrder = taskAlloc.getFormingOpeningShiftOrder();
                if (formingOpeningShiftOrder != null && order != null && order.equals(formingOpeningShiftOrder)) {
                    hours = 6; // 成型开产首班6小时
                }
            }
            shiftCapacities[i] = hours;
            totalCapacity += hours;
        }

        // ---- 按产能比例分配 ----
        int remaining = dayTotalQty;
        for (int i = 0; i < activeShifts.size(); i++) {
            CxShiftConfig shift = activeShifts.get(i);
            Integer order = shift.getDayShiftOrder();
            if (order == null) continue;

            int allocated;
            if (i == activeShifts.size() - 1) {
                allocated = remaining; // 最后一个班次分到剩余全部
            } else {
                allocated = (int) Math.round((double) dayTotalQty * shiftCapacities[i] / totalCapacity);
                allocated = Math.min(allocated, remaining);
            }
            if (allocated > 0) {
                shiftQtyMap.put(order, allocated);
                remaining -= allocated;
            }
        }

        // ---- 停产任务：停锅班次及之后不再排产，需求前移 ----
        if (isClosingDay) {
            Integer closingShiftOrder = taskAlloc.getClosingShiftOrder();
            if (closingShiftOrder != null) {
                int removedQty = 0;
                List<Integer> ordersToRemove = new ArrayList<>();
                for (Map.Entry<Integer, Integer> entry : shiftQtyMap.entrySet()) {
                    if (entry.getKey() >= closingShiftOrder) {
                        removedQty += entry.getValue();
                        ordersToRemove.add(entry.getKey());
                    }
                }
                for (Integer order : ordersToRemove) {
                    shiftQtyMap.remove(order);
                }
                if (removedQty > 0 && !shiftQtyMap.isEmpty()) {
                    int totalPreQty = shiftQtyMap.values().stream().mapToInt(Integer::intValue).sum();
                    int idx = 0;
                    int size = shiftQtyMap.size();
                    for (Map.Entry<Integer, Integer> entry : shiftQtyMap.entrySet()) {
                        int addQty;
                        if (idx == size - 1) {
                            addQty = removedQty;
                        } else {
                            addQty = (int) Math.round((double) removedQty * entry.getValue() / totalPreQty);
                        }
                        entry.setValue(entry.getValue() + addQty);
                        removedQty -= addQty;
                        idx++;
                    }
                }
            }
        }

        taskAlloc.setShiftPreAllocatedQty(shiftQtyMap);
        log.info("  任务 {} 天总量={} → 班次预分配: {} (停产={}, 开产={}, closingShift={}, formingOpeningShift={})",
                taskAlloc.getEmbryoCode(), dayTotalQty, shiftQtyMap,
                isClosingDay, isOpeningDay,
                taskAlloc.getClosingShiftOrder(), taskAlloc.getFormingOpeningShiftOrder());
    }

    /**
     * 根据TaskAllocation和班次预分配量构建精排用的DailyEmbryoTask
     */
    private CoreScheduleAlgorithmService.DailyEmbryoTask buildShiftTask(
            TaskAllocation taskAlloc, Integer shiftQty, ScheduleContextVo context) {

        CoreScheduleAlgorithmService.DailyEmbryoTask task = new CoreScheduleAlgorithmService.DailyEmbryoTask();
        task.setEmbryoCode(taskAlloc.getEmbryoCode());
        task.setMaterialCode(taskAlloc.getMaterialCode());
        task.setMaterialDesc(taskAlloc.getMaterialDesc());
        task.setMainMaterialDesc(taskAlloc.getMainMaterialDesc());
        task.setStructureName(taskAlloc.getStructureName());
        task.setPlannedProduction(shiftQty);
        task.setEndingExtraInventory(shiftQty);
        task.setIsTrialTask(taskAlloc.getIsTrialTask());
        task.setIsEndingTask(taskAlloc.getIsEndingTask());
        task.setIsContinueTask(taskAlloc.getIsContinueTask());
        task.setIsClosingDayTask(taskAlloc.getIsClosingDayTask());
        task.setIsOpeningDayTask(taskAlloc.getIsOpeningDayTask());
        task.setClosingShiftOrder(taskAlloc.getClosingShiftOrder());
        task.setFormingOpeningShiftOrder(taskAlloc.getFormingOpeningShiftOrder());
        task.setLhOpeningShiftOrder(taskAlloc.getLhOpeningShiftOrder());
        task.setStockHours(taskAlloc.getStockHours());
        task.setPriority(taskAlloc.getPriority());
        task.setLhId(taskAlloc.getLhId());

        int tripCapacity = productionCalculator.getTripCapacity(taskAlloc.getStructureName(), context);
        int cars = tripCapacity > 0 ? (int) Math.ceil((double) shiftQty / tripCapacity) : 0;
        task.setRequiredCars(cars);

        return task;
    }


    /**
     * 更新机台在产状态
     */
    private Map<String, Set<String>> updateMachineOnlineStatus(
            List<MachineAllocationResult> allocations,
            Map<String, Set<String>> currentMachineOnlineMap) {

        Map<String, Set<String>> newMap = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : currentMachineOnlineMap.entrySet()) {
            newMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        for (MachineAllocationResult allocation : allocations) {
            String machineCode = allocation.getMachineCode();
            Set<String> embryos = new HashSet<>();
            for (TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                if (taskAlloc.getEmbryoCode() != null) {
                    embryos.add(taskAlloc.getEmbryoCode());
                }
            }
            if (!embryos.isEmpty()) {
                newMap.put(machineCode, embryos);
            }
        }

        log.debug("更新机台在产状态完成，共 {} 台机台", newMap.size());
        return newMap;
    }

    /**
     * 判断是否为停产日
     */
    private boolean isStopProductionDay(ScheduleContextVo context, LocalDate date) {
        return scheduleDayTypeHelper.isStopDay(date);
    }
    
    /**
     * 单班次排产结果
     */
    public static class ShiftScheduleResult {
        /** 排产日（1-3），与 CxShiftConfig.scheduleDay 对应 */
        private int day;
        /** 排产日期 */
        private LocalDate scheduleDate;
        /** 该班次的班次配置 */
        private CxShiftConfig shiftConfig;
        /** 该班次所有机台的任务分配结果（包含续作/新任务/试制任务分配） */
        private List<MachineAllocationResult> allAllocations;
        /** 该班次的精排结果（包含班次级别的车数/数量） */
        private List<ShiftScheduleService.ShiftProductionResult> shiftProductionResults;

        public int getDay() { return day; }
        public void setDay(int day) { this.day = day; }
        public LocalDate getScheduleDate() { return scheduleDate; }
        public void setScheduleDate(LocalDate scheduleDate) { this.scheduleDate = scheduleDate; }
        public CxShiftConfig getShiftConfig() { return shiftConfig; }
        public void setShiftConfig(CxShiftConfig shiftConfig) { this.shiftConfig = shiftConfig; }
        public List<MachineAllocationResult> getAllAllocations() { return allAllocations; }
        public void setAllAllocations(List<MachineAllocationResult> allAllocations) { this.allAllocations = allAllocations; }
        public List<ShiftScheduleService.ShiftProductionResult> getShiftProductionResults() { return shiftProductionResults; }
        public void setShiftProductionResults(List<ShiftScheduleService.ShiftProductionResult> shiftProductionResults) { this.shiftProductionResults = shiftProductionResults; }
    }

    /**
     * 按班次排程后合并结果（与 buildFinalScheduleResults 逻辑一致，但输入是 ShiftScheduleResult）
     */
    private List<CxScheduleResult> buildFinalScheduleResultsFromShifts(
            ScheduleContextVo context,
            List<ShiftScheduleResult> shiftResults,
            List<CxShiftConfig> allShiftConfigs) {

        // 构建 shiftCode+scheduleDay → classField 的映射
        Map<String, String> shiftClassFieldMap = new HashMap<>();
        for (CxShiftConfig shiftConfig : allShiftConfigs) {
            String key = shiftConfig.getShiftCode() + "_" + shiftConfig.getScheduleDay();
            shiftClassFieldMap.put(key, shiftConfig.getClassField());
        }

        // ==================== 按 机台+胎胚+SAP物料 三维维度汇总班次排量 ====================
        Map<String, Map<String, ShiftScheduleService.ShiftProductionResult>> taskClassSprMap = new LinkedHashMap<>();
        Map<String, Integer> taskTotalQtyMap = new LinkedHashMap<>();
        Map<String, String> taskStructureMap = new LinkedHashMap<>();
        Map<String, Long> taskLhIdMap = new LinkedHashMap<>();

        for (ShiftScheduleResult shiftResult : shiftResults) {
            int day = shiftResult.getDay();
            // 直接从 ShiftScheduleResult 获取 classField，无需查表
            String classField = shiftResult.getShiftConfig() != null
                    ? shiftResult.getShiftConfig().getClassField() : null;
            if (classField == null) {
                // 回退：从映射表查找
                String shiftCode = shiftResult.getShiftConfig() != null
                        ? shiftResult.getShiftConfig().getShiftCode() : null;
                if (shiftCode != null) {
                    classField = shiftClassFieldMap.get(shiftCode + "_" + day);
                }
            }

            for (ShiftScheduleService.ShiftProductionResult spr : shiftResult.getShiftProductionResults()) {
                String machineCode = spr.getMachineCode();
                String embryoCode = spr.getEmbryoCode();
                String materialCode = spr.getMaterialCode() != null ? spr.getMaterialCode() : "";

                // 优先使用从 ShiftScheduleResult 获取的 classField
                String effectiveClassFieldTmp = classField;
                if (effectiveClassFieldTmp == null) {
                    String shiftCode = spr.getShiftCode();
                    String shiftKey = shiftCode + "_" + day;
                    effectiveClassFieldTmp = shiftClassFieldMap.get(shiftKey);
                }

                if (effectiveClassFieldTmp == null) {
                    log.warn("未找到班次映射: shiftCode={}, day={}", spr.getShiftCode(), day);
                    continue;
                }
                final String effectiveClassField = effectiveClassFieldTmp;

                String taskKey = machineCode + "|" + embryoCode + "|" + materialCode;
                taskClassSprMap.computeIfAbsent(taskKey, k -> new LinkedHashMap<>())
                        .compute(effectiveClassField, (k, existing) -> {
                            if (existing == null) {
                                return spr;
                            }
                            ShiftScheduleService.ShiftProductionResult merged = new ShiftScheduleService.ShiftProductionResult();
                            merged.setMachineCode(existing.getMachineCode());
                            merged.setEmbryoCode(existing.getEmbryoCode());
                            merged.setMaterialCode(existing.getMaterialCode());
                            merged.setMaterialDesc(existing.getMaterialDesc());
                            merged.setMainMaterialDesc(existing.getMainMaterialDesc());
                            merged.setStructureName(existing.getStructureName());
                            merged.setShiftCode(effectiveClassField);
                            merged.setQuantity((existing.getQuantity() != null ? existing.getQuantity() : 0)
                                    + (spr.getQuantity() != null ? spr.getQuantity() : 0));
                            merged.setTripNo(existing.getTripNo());
                            merged.setTripCapacity(existing.getTripCapacity());
                            merged.setStockHours(existing.getStockHours());
                            merged.setSequence(existing.getSequence());
                            merged.setPlanStartTime(existing.getPlanStartTime());
                            merged.setPlanEndTime(existing.getPlanEndTime());
                            return merged;
                        });
                taskTotalQtyMap.merge(taskKey, spr.getQuantity() != null ? spr.getQuantity() : 0, Integer::sum);
                if (spr.getStructureName() != null) {
                    taskStructureMap.putIfAbsent(taskKey, spr.getStructureName());
                }
            }
        }

        // 从 ShiftScheduleResult 的 allAllocations 中收集 lhId 信息
        for (ShiftScheduleResult shiftResult : shiftResults) {
            for (MachineAllocationResult allocation : shiftResult.getAllAllocations()) {
                if (allocation.getTaskAllocations() != null) {
                    for (TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                        String embryoCode = taskAlloc.getEmbryoCode();
                        String materialCode = taskAlloc.getMaterialCode() != null ? taskAlloc.getMaterialCode() : "";
                        String taskKey = allocation.getMachineCode() + "|" + embryoCode + "|" + materialCode;
                        if (taskAlloc.getLhId() != null) {
                            taskLhIdMap.putIfAbsent(taskKey, taskAlloc.getLhId());
                        }
                    }
                }
            }
        }

        // ==================== 构建辅助查询映射（复用逻辑） ====================
        Map<String, MdmMoldingMachine> machineMap = new HashMap<>();
        if (context.getAvailableMachines() != null) {
            for (MdmMoldingMachine machine : context.getAvailableMachines()) {
                machineMap.put(machine.getCxMachineCode(), machine);
            }
        }

        Map<String, MdmMaterialInfo> materialByCodeMap = new HashMap<>();
        Map<String, MdmMaterialInfo> materialByEmbryoMap = new HashMap<>();
        if (context.getMaterials() != null) {
            for (MdmMaterialInfo material : context.getMaterials()) {
                if (material.getMaterialCode() != null) {
                    materialByCodeMap.putIfAbsent(material.getMaterialCode(), material);
                }
                if (material.getEmbryoCode() != null) {
                    materialByEmbryoMap.putIfAbsent(material.getEmbryoCode(), material);
                }
            }
        }

        Map<Long, LhScheduleResult> lhByIdMap = new HashMap<>();
        Map<String, List<LhScheduleResult>> materialCodeToLhMap = new HashMap<>();
        if (context.getLhScheduleResults() != null) {
            for (LhScheduleResult lh : context.getLhScheduleResults()) {
                if (lh.getId() != null) {
                    lhByIdMap.put(lh.getId(), lh);
                }
                if (lh.getMaterialCode() != null) {
                    materialCodeToLhMap.computeIfAbsent(lh.getMaterialCode(), k -> new ArrayList<>()).add(lh);
                }
            }
        }

        // ==================== 构建最终的 CxScheduleResult 列表 ====================
        List<CxScheduleResult> results = new ArrayList<>();
        LocalDate startDate = context.getScheduleDate();

        for (Map.Entry<String, Map<String, ShiftScheduleService.ShiftProductionResult>> entry : taskClassSprMap.entrySet()) {
            String taskKey = entry.getKey();
            Map<String, ShiftScheduleService.ShiftProductionResult> classSprMap = entry.getValue();

            String[] parts = taskKey.split("\\|", 3);
            String machineCode = parts[0];
            String embryoCode = parts.length > 1 ? parts[1] : null;
            String materialCode = parts.length > 2 && !parts[2].isEmpty() ? parts[2] : null;
            String structureName = taskStructureMap.get(taskKey);

            CxScheduleResult result = new CxScheduleResult();

            // ---- 排程日期 ----
            result.setScheduleDate(java.sql.Timestamp.valueOf(startDate.atStartOfDay()));

            // ---- 机台信息 ----
            result.setCxMachineCode(machineCode);
            MdmMoldingMachine machine = machineMap.get(machineCode);
            if (machine != null) {
                result.setCxMachineName(machine.getMachineName());
                result.setCxMachineType(machine.getCxMachineBrandCode());
            }

            // ---- 胎胚信息 ----
            if (embryoCode != null) {
                result.setEmbryoCode(embryoCode);
                MdmMaterialInfo materialByEmbryo = materialByEmbryoMap.get(embryoCode);
                if (materialByEmbryo != null) {
                    result.setMainMaterialDesc(materialByEmbryo.getEmbryoDesc());
                    if (materialByEmbryo.getProSize() != null) {
                        try {
                            result.setSpecDimension(new BigDecimal(materialByEmbryo.getProSize()));
                        } catch (NumberFormatException e) {
                            log.debug("无法解析寸口: {}", materialByEmbryo.getProSize());
                        }
                    }
                    result.setStructureName(materialByEmbryo.getStructureName() != null ? materialByEmbryo.getStructureName() : structureName);
                } else {
                    result.setStructureName(structureName);
                }
            }

            // ---- 物料信息 ----
            if (materialCode != null) {
                result.setMaterialCode(materialCode);
                MdmMaterialInfo materialByCode = materialByCodeMap.get(materialCode);
                if (materialByCode != null) {
                    result.setMaterialDesc(materialByCode.getMaterialDesc());
                    result.setBomDataVersion(materialByCode.getEmbryoNo());
                    if (materialByCode.getStructureName() != null) {
                        result.setStructureName(materialByCode.getStructureName());
                    }
                }
            }

            // ---- 库存信息 ----
            Long lhId = taskLhIdMap.get(taskKey);
            if (lhId != null) {
                Map<String, Integer> stockMap = context.getMaterialStockMap();
                if (stockMap != null) {
                    Integer stock = stockMap.get(String.valueOf(lhId));
                    if (stock != null && stock > 0) {
                        result.setTotalStock(new BigDecimal(stock));
                    }
                }
            }

            // ---- 硫化信息 ----
            LhScheduleResult primaryLh = null;
            if (lhId != null) {
                primaryLh = lhByIdMap.get(lhId);
            }
            if (primaryLh == null && materialCode != null) {
                List<LhScheduleResult> relatedLhResults = materialCodeToLhMap.get(materialCode);
                if (relatedLhResults != null && !relatedLhResults.isEmpty()) {
                    primaryLh = relatedLhResults.get(0);
                }
            }

            if (primaryLh != null) {
                result.setLhMachineCode(primaryLh.getLhMachineCode());
                result.setLhMachineName(primaryLh.getLhMachineName());
                result.setLhScheduleIds(primaryLh.getId() != null ? String.valueOf(primaryLh.getId()) : null);
                if (primaryLh.getMouldQty() != null) {
                    result.setLhMachineQty(new BigDecimal(primaryLh.getMouldQty()));
                }
                if (primaryLh.getSingleMouldShiftQty() != null) {
                    result.setLhClassQty(new BigDecimal(primaryLh.getSingleMouldShiftQty()));
                }
                Map<String, MdmMonthSurplus> monthSurplusMap = context.getMonthSurplusMap();
                if (monthSurplusMap != null) {
                    String surplusKey = materialCode != null ? materialCode : embryoCode;
                    MdmMonthSurplus surplus = monthSurplusMap.get(surplusKey);
                    if (surplus != null && surplus.getPlanSurplusQty() != null) {
                        result.setLhRemainQty(surplus.getPlanSurplusQty());
                    }
                }
            }

            // ---- 成型余量 ----
            {
                Map<String, Integer> formingRemainderMap = context.getFormingRemainderMap();
                if (formingRemainderMap != null && embryoCode != null) {
                    Integer cxRemain = formingRemainderMap.get(embryoCode);
                    if (cxRemain != null) {
                        result.setCxRemainQty(new BigDecimal(cxRemain));
                    }
                }
            }

            // ---- 胎胚总计划量 ----
            int totalQty = taskTotalQtyMap.getOrDefault(taskKey, 0);
            result.setProductNum(new BigDecimal(totalQty));

            // ---- 状态字段 ----
            result.setProductionStatus("0");
            result.setIsRelease("0");
            result.setDataSource("0");
            result.setCreateTime(new Date());

            // ---- 收尾提示 ----
            if (result.getCxRemainQty() != null && result.getCxRemainQty().compareTo(BigDecimal.ZERO) <= 0) {
                result.setMarkCloseOutTip("0");
            } else {
                result.setMarkCloseOutTip("1");
            }

            // ---- 映射班次排量到 CLASS1~8 ----
            for (Map.Entry<String, ShiftScheduleService.ShiftProductionResult> classEntry : classSprMap.entrySet()) {
                setClassFieldValue(result, classEntry.getKey(), classEntry.getValue());
            }

            results.add(result);
        }

        log.info("最终排程结果（按班次合并）：共 {} 条记录（机台+胎胚+SAP物料维度）", results.size());
        return results;
    }

    /**
     * 按班次排程后构建子表记录（与 buildScheduleDetails 逻辑一致，但输入是 ShiftScheduleResult）
     */
    private List<CxScheduleDetail> buildScheduleDetailsFromShifts(
            ScheduleContextVo context,
            List<ShiftScheduleResult> shiftResults,
            List<CxShiftConfig> allShiftConfigs) {

        if (shiftResults == null || shiftResults.isEmpty()) {
            return Collections.emptyList();
        }

        // 构建班次配置映射：shiftCode → classField
        Map<String, String> shiftToClassField = new HashMap<>();
        Map<String, Integer> classFieldOrder = new HashMap<>();
        if (allShiftConfigs != null) {
            int order = 1;
            for (CxShiftConfig cfg : allShiftConfigs) {
                shiftToClassField.put(cfg.getShiftCode(), cfg.getClassField());
                classFieldOrder.putIfAbsent(cfg.getClassField(), order++);
            }
        }

        Map<Long, LhScheduleResult> lhResultMap = new HashMap<>();
        if (context.getLhScheduleResults() != null) {
            for (LhScheduleResult lh : context.getLhScheduleResults()) {
                if (lh.getId() != null) {
                    lhResultMap.put(lh.getId(), lh);
                }
            }
        }

        Map<String, EmbryoTripTracker> embryoTrackers = new LinkedHashMap<>();

        for (ShiftScheduleResult shiftResult : shiftResults) {
            int day = shiftResult.getDay();
            String shiftClassField = shiftResult.getShiftConfig() != null
                    ? shiftResult.getShiftConfig().getClassField() : null;

            for (ShiftScheduleService.ShiftProductionResult spr : shiftResult.getShiftProductionResults()) {
                String embryoCode = spr.getEmbryoCode();
                String materialCode = spr.getMaterialCode() != null ? spr.getMaterialCode() : "";
                String embryoKey = embryoCode + "|" + materialCode;

                EmbryoTripTracker tracker = embryoTrackers.computeIfAbsent(embryoKey,
                        k -> new EmbryoTripTracker(embryoCode, materialCode));

                CoreScheduleAlgorithmService.DailyEmbryoTask task = spr.getSourceTask();
                if (task != null) {
                    if (task.getVulcanizeMachineCount() != null) {
                        tracker.setVulcanizeMachineCount(task.getVulcanizeMachineCount());
                    }
                    if (task.getVulcanizeMoldCount() != null) {
                        tracker.setVulcanizeMoldCount(task.getVulcanizeMoldCount());
                    }
                    if (task.getCurrentStock() != null && tracker.getBeginStock() == null) {
                        tracker.setBeginStock(task.getCurrentStock());
                    }
                    if (task.getHourCapacity() != null && task.getHourCapacity() > 0) {
                        tracker.setHourlyCapacity(task.getHourCapacity());
                    } else {
                        int hourlyCapacity = calculateHourlyCapacity(
                                spr.getMachineCode(), materialCode, task.getStructureName(), context);
                        tracker.setHourlyCapacity(hourlyCapacity);
                    }
                }

                int beginStock = tracker.getCurrentStock();
                int tripCapacity = spr.getTripCapacity() != null ? spr.getTripCapacity() : 12;
                int planQty = spr.getQuantity() != null ? spr.getQuantity() : 0;
                int tripCount = (planQty + tripCapacity - 1) / tripCapacity;

                Long lhId = null;
                LhScheduleResult lhResult = null;
                if (spr.getSourceTask() != null) {
                    lhId = spr.getSourceTask().getLhId();
                    if (lhId != null) {
                        lhResult = lhResultMap.get(lhId);
                    }
                }

                // 优先使用 ShiftScheduleResult 的 classField
                String classField = shiftClassField;
                if (classField == null) {
                    classField = shiftToClassField.getOrDefault(spr.getShiftCode(), spr.getShiftCode());
                }
                int vulcanizeClassIndex = getClassIndex(classField);
                int vulcanizeClassConsumption = lhResult != null
                        ? (getClassPlanQtyByIndex(lhResult, vulcanizeClassIndex) != null
                        ? getClassPlanQtyByIndex(lhResult, vulcanizeClassIndex) : 0) : 0;

                int cumulativeTripPlan = 0;
                for (int i = 1; i <= tripCount; i++) {
                    int tripPlanQty = Math.min(tripCapacity, planQty - (i - 1) * tripCapacity);
                    cumulativeTripPlan += tripPlanQty;

                    int currentStock = tracker.getCurrentStock();
                    double stockHours = calculateStockHours(
                            currentStock, tracker.getCumulativeForming(),
                            tracker.getCumulativeVulcanize(),
                            tracker.getVulcanizeMachineCount(), tracker.getVulcanizeMoldCount());

                    LocalDateTime tripStartTime = null;
                    LocalDateTime tripEndTime = null;
                    if (spr.getPlanStartTime() != null && tracker.getHourlyCapacity() > 0) {
                        LocalDateTime shiftStart = spr.getPlanStartTime();
                        int hourlyCapacity = tracker.getHourlyCapacity();

                        int cumulativeBeforeTrip = 0;
                        for (int j = 1; j < i; j++) {
                            cumulativeBeforeTrip += Math.min(tripCapacity, planQty - (j - 1) * tripCapacity);
                        }

                        long minutesBefore = (long) cumulativeBeforeTrip * 60 / hourlyCapacity;
                        long minutesForTrip = (long) tripPlanQty * 60 / hourlyCapacity;

                        tripStartTime = shiftStart.plusMinutes(minutesBefore);
                        tripEndTime = shiftStart.plusMinutes(minutesBefore + minutesForTrip);
                    }

                    TripRecord record = new TripRecord();
                    record.setEmbryoCode(embryoCode);
                    record.setMaterialCode(materialCode);
                    record.setMachineCode(spr.getMachineCode());
                    record.setDay(day);
                    record.setShiftCode(spr.getShiftCode());
                    record.setClassField(classField);
                    record.setTripNo(i);
                    record.setTripCapacity(tripCapacity);
                    record.setPlanQty(tripPlanQty);
                    record.setStockHours(BigDecimal.valueOf(stockHours).setScale(2, RoundingMode.HALF_UP));
                    record.setPlanStartTime(tripStartTime);
                    record.setPlanEndTime(tripEndTime);
                    record.setIsTrialTask(Boolean.TRUE.equals(spr.getIsTrialTask()));
                    record.setIsEndingTask(Boolean.TRUE.equals(spr.getIsEndingTask()));
                    record.setVulcanizeMachineCount(tracker.getVulcanizeMachineCount());

                    tracker.addTrip(record);
                    tracker.addFormingProduction(tripPlanQty);
                    if (i == tripCount && vulcanizeClassConsumption > 0) {
                        tracker.addVulcanizeConsumption(vulcanizeClassConsumption);
                    }
                }
            }
        }

        // 对每个胎胚内的车次记录按顺位规则排序并分配顺序号
        List<CxScheduleDetail> allDetails = new ArrayList<>();
        for (EmbryoTripTracker tracker : embryoTrackers.values()) {
            List<TripRecord> allTrips = tracker.getTrips();

            List<TripRecord> regularTrips = allTrips.stream()
                    .filter(t -> !t.getIsTrialTask() && !t.getIsEndingTask())
                    .collect(Collectors.toList());

            regularTrips.sort((a, b) -> {
                int classA = classFieldOrder.getOrDefault(a.getClassField(), 99);
                int classB = classFieldOrder.getOrDefault(b.getClassField(), 99);
                if (classA != classB) return Integer.compare(classA, classB);
                return Double.compare(a.getStockHours().doubleValue(), b.getStockHours().doubleValue());
            });

            Map<String, Integer> classSeqMap = new HashMap<>();
            for (TripRecord trip : regularTrips) {
                int seq = classSeqMap.merge(trip.getClassField(), 1, Integer::sum);
                trip.setSequence(seq);
            }

            for (TripRecord trip : allTrips) {
                CxScheduleDetail detail = new CxScheduleDetail();
                detail.setEmbryoCode(trip.getEmbryoCode());
                detail.setMaterialCode(trip.getMaterialCode());
                detail.setCxMachineCode(trip.getMachineCode());
                detail.setScheduleDate(context.getScheduleDate().plusDays(trip.getDay()));

                setDetailClassField(detail, trip.getClassField(), trip);
                allDetails.add(detail);
            }
        }

        log.info("子表构建完成（按班次）：共 {} 条车次记录", allDetails.size());
        return allDetails;
    }

    /**
     * 计算库存可供硫化时长（小时）
     *
     * <p>公式：库存可供硫化时长 = (当前库存 + 成型累计计划量) / 硫化机数 / 单台模数
     *
     * <p>其中当前库存 = 期初库存 + 成型累计 - 硫化累计
     *
     * @param currentStock      当前库存（期初库存 + 成型累计 - 硫化累计）
     * @param cumulativeForming 成型累计生产量
     * @param vulcanizeConsumed 硫化累计消耗量
     * @param vulcanizeMachineCount 硫化机台数
     * @param vulcanizeMoldCount   单台模数
     * @return 库存可供硫化时长（小时）
     */
    private double calculateStockHours(int currentStock, int cumulativeForming,
                                       int vulcanizeConsumed,
                                       int vulcanizeMachineCount, int vulcanizeMoldCount) {
        if (vulcanizeMachineCount <= 0 || vulcanizeMoldCount <= 0) {
            return 0;
        }
        // 库存可供硫化时长 = (当前库存 + 成型累计) / 硫化机数 / 单台模数
        return (double) (currentStock + cumulativeForming) / vulcanizeMachineCount / vulcanizeMoldCount;
    }

    /**
     * 内部类：胎胚车次追踪器
     * <p>用于递推计算每个班次开始前的库存
     */
    private static class EmbryoTripTracker {
        private final String embryoCode;
        private final String materialCode;
        private Integer beginStock;  // 期初库存（首次设置后不再变）
        private int currentStock;     // 当前库存（= 期初 + 成型累计 - 硫化累计）
        private int cumulativeForming;     // 成型累计生产
        private int cumulativeVulcanize;   // 硫化累计消耗
        private int vulcanizeMachineCount = 1;
        private int vulcanizeMoldCount = 1;
        private int hourlyCapacity = 12;   // 小时产能（条/小时）
        private final List<TripRecord> trips = new ArrayList<>();

        EmbryoTripTracker(String embryoCode, String materialCode) {
            this.embryoCode = embryoCode;
            this.materialCode = materialCode;
        }

        void setBeginStock(Integer beginStock) {
            this.beginStock = beginStock;
            this.currentStock = beginStock;
        }

        Integer getBeginStock() { return beginStock; }
        int getVulcanizeMachineCount() { return vulcanizeMachineCount; }
        void setVulcanizeMachineCount(int count) { this.vulcanizeMachineCount = count; }
        int getVulcanizeMoldCount() { return vulcanizeMoldCount; }
        void setVulcanizeMoldCount(int count) { this.vulcanizeMoldCount = count; }

        int getCurrentStock() {
            return (beginStock != null ? beginStock : 0) + cumulativeForming - cumulativeVulcanize;
        }

        int getCumulativeForming() { return cumulativeForming; }
        int getCumulativeVulcanize() { return cumulativeVulcanize; }
        void addFormingProduction(int qty) { this.cumulativeForming += qty; }
        void addVulcanizeConsumption(int qty) { this.cumulativeVulcanize += qty; }
        int getHourlyCapacity() { return hourlyCapacity; }
        void setHourlyCapacity(int capacity) { this.hourlyCapacity = capacity > 0 ? capacity : 12; }
        void addTrip(TripRecord trip) { this.trips.add(trip); }
        List<TripRecord> getTrips() { return trips; }
    }

    /**
     * 设置子表记录的车次字段
     */
    private void setDetailClassField(CxScheduleDetail detail, String classField, TripRecord trip) {
        if (classField == null || trip == null) {
            return;
        }
        switch (classField) {
            case "CLASS1":
                detail.setClass1TripNo(String.valueOf(trip.getTripNo()));
                detail.setClass1TripCapacity(BigDecimal.valueOf(trip.getTripCapacity()));
                detail.setClass1StockHours(trip.getStockHours());
                detail.setClass1Sequence(trip.getSequence());
                detail.setClass1PlanStartTime(trip.getPlanStartTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanStartTime()) : null);
                detail.setClass1PlanEndTime(trip.getPlanEndTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanEndTime()) : null);
                break;
            case "CLASS2":
                detail.setClass2TripNo(String.valueOf(trip.getTripNo()));
                detail.setClass2TripCapacity(BigDecimal.valueOf(trip.getTripCapacity()));
                detail.setClass2StockHours(trip.getStockHours());
                detail.setClass2Sequence(trip.getSequence());
                detail.setClass2PlanStartTime(trip.getPlanStartTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanStartTime()) : null);
                detail.setClass2PlanEndTime(trip.getPlanEndTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanEndTime()) : null);
                break;
            case "CLASS3":
                detail.setClass3TripNo(String.valueOf(trip.getTripNo()));
                detail.setClass3TripCapacity(BigDecimal.valueOf(trip.getTripCapacity()));
                detail.setClass3StockHours(trip.getStockHours());
                detail.setClass3Sequence(trip.getSequence());
                detail.setClass3PlanStartTime(trip.getPlanStartTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanStartTime()) : null);
                detail.setClass3PlanEndTime(trip.getPlanEndTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanEndTime()) : null);
                break;
            case "CLASS4":
                detail.setClass4TripNo(String.valueOf(trip.getTripNo()));
                detail.setClass4TripCapacity(BigDecimal.valueOf(trip.getTripCapacity()));
                detail.setClass4StockHours(trip.getStockHours());
                detail.setClass4Sequence(trip.getSequence());
                break;
            case "CLASS5":
                detail.setClass5TripNo(String.valueOf(trip.getTripNo()));
                detail.setClass5TripCapacity(BigDecimal.valueOf(trip.getTripCapacity()));
                detail.setClass5StockHours(trip.getStockHours());
                detail.setClass5Sequence(trip.getSequence());
                break;
            case "CLASS6":
                detail.setClass6TripNo(String.valueOf(trip.getTripNo()));
                detail.setClass6TripCapacity(BigDecimal.valueOf(trip.getTripCapacity()));
                detail.setClass6StockHours(trip.getStockHours());
                detail.setClass6Sequence(trip.getSequence());
                break;
            case "CLASS7":
                detail.setClass7TripNo(String.valueOf(trip.getTripNo()));
                detail.setClass7TripCapacity(BigDecimal.valueOf(trip.getTripCapacity()));
                detail.setClass7StockHours(trip.getStockHours());
                detail.setClass7Sequence(trip.getSequence());
                break;
            case "CLASS8":
                detail.setClass8TripNo(String.valueOf(trip.getTripNo()));
                detail.setClass8TripCapacity(BigDecimal.valueOf(trip.getTripCapacity()));
                detail.setClass8StockHours(trip.getStockHours());
                detail.setClass8Sequence(trip.getSequence());
                break;
            default:
                log.warn("未知的 CLASS_FIELD: {}", classField);
        }
    }

    /**
     * 内部类：车次记录（用于计算顺位）
     */
    private static class TripRecord {
        private String embryoCode;
        private String materialCode;
        private String machineCode;
        private int day;
        private String shiftCode;
        private String classField;
        private int tripNo;
        private int tripCapacity;
        private int planQty;
        private BigDecimal stockHours;
        private LocalDateTime planStartTime;
        private LocalDateTime planEndTime;
        private boolean isTrialTask;
        private boolean isEndingTask;
        private int vulcanizeMachineCount;
        private int sequence;

        // getters and setters
        public String getEmbryoCode() { return embryoCode; }
        public void setEmbryoCode(String embryoCode) { this.embryoCode = embryoCode; }
        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
        public String getMachineCode() { return machineCode; }
        public void setMachineCode(String machineCode) { this.machineCode = machineCode; }
        public int getDay() { return day; }
        public void setDay(int day) { this.day = day; }
        public String getShiftCode() { return shiftCode; }
        public void setShiftCode(String shiftCode) { this.shiftCode = shiftCode; }
        public String getClassField() { return classField; }
        public void setClassField(String classField) { this.classField = classField; }
        public int getTripNo() { return tripNo; }
        public void setTripNo(int tripNo) { this.tripNo = tripNo; }
        public int getTripCapacity() { return tripCapacity; }
        public void setTripCapacity(int tripCapacity) { this.tripCapacity = tripCapacity; }
        public int getPlanQty() { return planQty; }
        public void setPlanQty(int planQty) { this.planQty = planQty; }
        public BigDecimal getStockHours() { return stockHours; }
        public void setStockHours(BigDecimal stockHours) { this.stockHours = stockHours; }
        public LocalDateTime getPlanStartTime() { return planStartTime; }
        public void setPlanStartTime(LocalDateTime planStartTime) { this.planStartTime = planStartTime; }
        public LocalDateTime getPlanEndTime() { return planEndTime; }
        public void setPlanEndTime(LocalDateTime planEndTime) { this.planEndTime = planEndTime; }
        public boolean getIsTrialTask() { return isTrialTask; }
        public void setIsTrialTask(boolean isTrialTask) { this.isTrialTask = isTrialTask; }
        public boolean getIsEndingTask() { return isEndingTask; }
        public void setIsEndingTask(boolean isEndingTask) { this.isEndingTask = isEndingTask; }
        public int getVulcanizeMachineCount() { return vulcanizeMachineCount; }
        public void setVulcanizeMachineCount(int vulcanizeMachineCount) { this.vulcanizeMachineCount = vulcanizeMachineCount; }
        public int getSequence() { return sequence; }
        public void setSequence(int sequence) { this.sequence = sequence; }
    }

    /**
     * 构建任务Key（用于关联主表记录）
     */
    private String buildTaskKey(String machineCode, String embryoCode, String materialCode) {
        return (machineCode != null ? machineCode : "") + "|"
                + (embryoCode != null ? embryoCode : "") + "|"
                + (materialCode != null ? materialCode : "");
    }

    /**
     * 按 CLASS_FIELD 设置对应的班次计划量
     * <p>将 ShiftProductionResult 的计划量字段设置到 CxScheduleResult 的 CLASSn 列：
     * PLAN_QTY、ANALYSIS（原因分析）
     *
     * <p>原因分析标记规则：
     * <ul>
     *   <li>试制任务 → "试制"</li>
     *   <li>收尾任务 → "收尾"</li>
     *   <li>开产任务 → "开产"</li>
     *   <li>停产任务 → "停产"</li>
     *   <li>量试任务 → "量试"</li>
     *   <li>新增任务 → "新增"</li>
     *   <li>多个原因可叠加，如 "试制,收尾"</li>
     * </ul>
     *
     * @param result    排程结果记录
     * @param classField CLASS1~CLASS8 班次字段标识
     * @param spr       班次排产结果
     */
    private void setClassFieldValue(CxScheduleResult result, String classField, ShiftScheduleService.ShiftProductionResult spr) {
        if (classField == null || spr == null) {
            return;
        }

        // 构建原因分析字符串
        String analysis = buildTaskAnalysis(spr);

        switch (classField) {
            case "CLASS1":
                result.setClass1PlanQty(spr.getQuantity() != null ? new BigDecimal(spr.getQuantity()) : null);
                if (analysis != null) {
                    result.setClass1Analysis(analysis);
                }
                break;
            case "CLASS2":
                result.setClass2PlanQty(spr.getQuantity() != null ? new BigDecimal(spr.getQuantity()) : null);
                if (analysis != null) {
                    result.setClass2Analysis(analysis);
                }
                break;
            case "CLASS3":
                result.setClass3PlanQty(spr.getQuantity() != null ? new BigDecimal(spr.getQuantity()) : null);
                if (analysis != null) {
                    result.setClass3Analysis(analysis);
                }
                break;
            case "CLASS4":
                result.setClass4PlanQty(spr.getQuantity() != null ? new BigDecimal(spr.getQuantity()) : null);
                if (analysis != null) {
                    result.setClass4Analysis(analysis);
                }
                break;
            case "CLASS5":
                result.setClass5PlanQty(spr.getQuantity() != null ? new BigDecimal(spr.getQuantity()) : null);
                if (analysis != null) {
                    result.setClass5Analysis(analysis);
                }
                break;
            case "CLASS6":
                result.setClass6PlanQty(spr.getQuantity() != null ? new BigDecimal(spr.getQuantity()) : null);
                if (analysis != null) {
                    result.setClass6Analysis(analysis);
                }
                break;
            case "CLASS7":
                result.setClass7PlanQty(spr.getQuantity() != null ? new BigDecimal(spr.getQuantity()) : null);
                if (analysis != null) {
                    result.setClass7Analysis(analysis);
                }
                break;
            case "CLASS8":
                result.setClass8PlanQty(spr.getQuantity() != null ? new BigDecimal(spr.getQuantity()) : null);
                if (analysis != null) {
                    result.setClass8Analysis(analysis);
                }
                break;
            default:
                log.warn("未知的 CLASS_FIELD: {}", classField);
        }
    }

    /**
     * 构建任务原因分析字符串
     * <p>根据任务类型组合原因标记，多个原因用逗号分隔
     */
    private String buildTaskAnalysis(ShiftScheduleService.ShiftProductionResult spr) {
        if (spr == null) {
            return null;
        }

        List<String> reasons = new ArrayList<>();

        // 从 sourceTask 获取详细任务类型
        CoreScheduleAlgorithmService.DailyEmbryoTask task = spr.getSourceTask();
        if (task != null) {
            if (Boolean.TRUE.equals(task.getIsTrialTask())) {
                reasons.add("试制");
            }
            if (Boolean.TRUE.equals(task.getIsProductionTrial())) {
                reasons.add("量试");
            }
            if (Boolean.TRUE.equals(task.getIsEndingTask())) {
                reasons.add("收尾");
            }
            if (Boolean.TRUE.equals(task.getIsOpeningDayTask())) {
                reasons.add("开产");
            }
            if (Boolean.TRUE.equals(task.getIsClosingDayTask())) {
                reasons.add("停产");
            }
            if (Boolean.TRUE.equals(task.getIsFirstTask()) && !Boolean.TRUE.equals(task.getIsContinueTask())) {
                // 新增任务（非续作的首次任务）
                reasons.add("新增");
            }
        }

        // 如果 sourceTask 为空，回退到 ShiftProductionResult 的标记
        if (task == null) {
            if (Boolean.TRUE.equals(spr.getIsTrialTask())) {
                reasons.add("试制");
            }
            if (Boolean.TRUE.equals(spr.getIsEndingTask())) {
                reasons.add("收尾");
            }
            if (Boolean.TRUE.equals(spr.getIsContinueTask())) {
                // 续作任务不标记
            }
        }

        if (reasons.isEmpty()) {
            return null;
        }

        return String.join(",", reasons);
    }

    /**
     * 更新一个班次排程后的库存和硫化余量，供下一个班次排程使用
     * <p>逻辑与 updateContextForNextDay 一致，只是按单个班次执行
     *
     * @param context        排程上下文
     * @param shiftAllocations 该班次的机台分配结果
     * @param shiftConfigs   该班次的配置
     */
    private void updateContextForNextShift(
            ScheduleContextVo context,
            List<MachineAllocationResult> shiftAllocations,
            List<CxShiftConfig> shiftConfigs) {
        // 直接复用 updateContextForNextDay 逻辑，它已经按班次配置计算硫化消耗
        updateContextForNextDay(context, shiftAllocations, shiftConfigs);
    }

    /**
     * 每天/每班次排程后更新上下文中的库存和硫化余量，供下一天/下一班次排程使用
     *
     * <p>更新逻辑：
     * <ol>
     *   <li>计算当天成型产出（按胎胚编码汇总排程结果的productNum）</li>
     *   <li>计算当天硫化消耗（按胎胚编码汇总，根据当天班次CLASS字段获取硫化计划量）</li>
     *   <li>更新materialStockMap：每条硫化任务的库存 = 原库存 - 硫化消耗 + 比例分配的成型产出</li>
     *   <li>更新monthSurplusMap：硫化余量 -= 当天硫化消耗</li>
     *   <li>重算formingRemainderMap：成型余量 = 硫化余量 - 库存</li>
     * </ol>
     *
     * @param context    排程上下文
     * @param dayResults 当天排程结果
     * @param dayShifts  当天班次配置
     */
    private void updateContextForNextDay(
            ScheduleContextVo context,
            List<MachineAllocationResult> dayAllocations,
            List<CxShiftConfig> dayShifts) {

        LocalDate scheduleDate = context.getCurrentScheduleDate();
        log.info("========== 更新第 {} 天排程后的库存和硫化余量，日期: {} ==========",
                context.getCurrentScheduleDay(), scheduleDate);

        // 1. 计算当天成型产出（按胎胚编码汇总）
        Map<String, Integer> formingOutputMap = calculateFormingOutputByEmbryo(dayAllocations);
        log.info("成型产出汇总: {}", formingOutputMap);

        // 2. 计算当天硫化消耗（按胎胚编码汇总，根据当天班次CLASS字段获取计划量）
        Map<String, Integer> vulcanizingConsumptionByEmbryo = new HashMap<>();
        Map<Long, Integer> vulcanizingConsumptionByLhId = new HashMap<>();
        calculateVulcanizingConsumption(context.getLhScheduleResults(), dayShifts,
                vulcanizingConsumptionByEmbryo, vulcanizingConsumptionByLhId);
        log.info("硫化消耗汇总(按胎胚): {}", vulcanizingConsumptionByEmbryo);

        // 3. 更新 materialStockMap（按硫化任务维度更新库存分配）
        updateMaterialStockMap(context, formingOutputMap, vulcanizingConsumptionByEmbryo, vulcanizingConsumptionByLhId);

        // 4. 更新 CxStock 实体中的 stockNum（供 buildStockMap 使用）
        updateCxStockEntities(context, formingOutputMap, vulcanizingConsumptionByEmbryo);

        // 5. 更新 monthSurplusMap（硫化余量 -= 当天硫化消耗）
        updateMonthSurplus(context, vulcanizingConsumptionByEmbryo);

        // 6. 重算 formingRemainderMap（成型余量 = 硫化余量 - 库存）
        recalculateFormingRemainder(context);

        log.info("========== 库存和硫化余量更新完成 ==========");
    }

    /**
     * 计算当天成型产出，按胎胚编码汇总
     *
     * @param dayAllocations 当天机台分配结果
     * @return 胎胚编码 → 成型产出量
     */
    private Map<String, Integer> calculateFormingOutputByEmbryo(List<MachineAllocationResult> dayAllocations) {
        Map<String, Integer> outputMap = new HashMap<>();
        if (dayAllocations == null) {
            return outputMap;
        }
        for (MachineAllocationResult allocation : dayAllocations) {
            for (TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                String embryoCode = taskAlloc.getEmbryoCode();
                Integer qty = taskAlloc.getQuantity();
                if (embryoCode != null && qty != null && qty > 0) {
                    outputMap.merge(embryoCode, qty, Integer::sum);
                }
            }
        }
        return outputMap;
    }

    /**
     * 计算当天硫化消耗
     *
     * <p>根据当天班次配置的CLASS字段，获取每条硫化记录对应的计划量作为硫化消耗
     *
     * @param lhResults                     硫化排程结果列表
     * @param dayShifts                     当天班次配置
     * @param vulcanizingConsumptionByEmbryo 输出：胎胚编码 → 硫化消耗量
     * @param vulcanizingConsumptionByLhId   输出：硫化任务ID → 硫化消耗量
     */
    private void calculateVulcanizingConsumption(
            List<LhScheduleResult> lhResults,
            List<CxShiftConfig> dayShifts,
            Map<String, Integer> vulcanizingConsumptionByEmbryo,
            Map<Long, Integer> vulcanizingConsumptionByLhId) {

        if (lhResults == null || dayShifts == null || dayShifts.isEmpty()) {
            return;
        }

        for (LhScheduleResult lhResult : lhResults) {
            String embryoCode = lhResult.getEmbryoCode();
            if (embryoCode == null) {
                continue;
            }

            // 获取当天班次对应的硫化计划量
            int consumption = getVulcanizingConsumptionForDay(lhResult, dayShifts);
            if (consumption > 0) {
                vulcanizingConsumptionByEmbryo.merge(embryoCode, consumption, Integer::sum);
                if (lhResult.getId() != null) {
                    vulcanizingConsumptionByLhId.merge(lhResult.getId(), consumption, Integer::sum);
                }
            }
        }
    }

    /**
     * 获取硫化记录在指定班次的计划量（即当天的硫化消耗）
     *
     * @param lhResult  硫化记录
     * @param dayShifts 当天班次配置
     * @return 该硫化记录在当天班次的计划量之和
     */
    private int getVulcanizingConsumptionForDay(LhScheduleResult lhResult, List<CxShiftConfig> dayShifts) {
        int total = 0;
        for (CxShiftConfig shiftConfig : dayShifts) {
            String classField = shiftConfig.getClassField();
            if (classField != null && classField.startsWith("CLASS")) {
                try {
                    int classIndex = Integer.parseInt(classField.substring(5));
                    Integer planQty = getClassPlanQtyByIndex(lhResult, classIndex);
                    if (planQty != null && planQty > 0) {
                        total += planQty;
                    }
                } catch (NumberFormatException e) {
                    log.warn("无法解析班次字段: {}", classField);
                }
            }
        }
        return total;
    }

    /**
     * 根据班次字段获取班次索引
     *
     * @param classField 班次字段（如 "CLASS1"）
     * @return 班次索引 (1-8)，解析失败返回 0
     */
    private int getClassIndex(String classField) {
        if (classField != null && classField.startsWith("CLASS")) {
            try {
                return Integer.parseInt(classField.substring(5));
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
     * 更新 materialStockMap（按硫化任务维度更新库存分配）
     *
     * <p>逻辑：
     * <ol>
     *   <li>对每条硫化任务，减去当天硫化消耗</li>
     *   <li>对每个胎胚编码的成型产出，按各硫化任务当前库存比例分配</li>
     * </ol>
     *
     * @param context                        排程上下文
     * @param formingOutputMap               胎胚编码 → 成型产出量
     * @param vulcanizingConsumptionByEmbryo 胎胚编码 → 硫化消耗量
     * @param vulcanizingConsumptionByLhId   硫化任务ID → 硫化消耗量
     */
    private void updateMaterialStockMap(
            ScheduleContextVo context,
            Map<String, Integer> formingOutputMap,
            Map<String, Integer> vulcanizingConsumptionByEmbryo,
            Map<Long, Integer> vulcanizingConsumptionByLhId) {

        Map<String, Integer> materialStockMap = context.getMaterialStockMap();
        if (materialStockMap == null) {
            materialStockMap = new HashMap<>();
            context.setMaterialStockMap(materialStockMap);
        }

        List<LhScheduleResult> lhResults = context.getLhScheduleResults();

        // Step 1: 减去每条硫化任务的当天硫化消耗
        for (LhScheduleResult lhResult : lhResults) {
            if (lhResult.getId() == null) continue;
            String taskKey = String.valueOf(lhResult.getId());
            Integer consumption = vulcanizingConsumptionByLhId.get(lhResult.getId());
            if (consumption != null && consumption > 0) {
                int currentStock = materialStockMap.getOrDefault(taskKey, 0);
                int newStock = Math.max(0, currentStock - consumption);
                materialStockMap.put(taskKey, newStock);
                log.debug("硫化消耗扣减: lhId={}, 原库存={}, 消耗={}, 新库存={}",
                        taskKey, currentStock, consumption, newStock);
            }
        }

        // Step 2: 按胎胚编码分组，将成型产出按比例分配给各硫化任务
        // 按 embryoCode 分组硫化任务
        Map<String, List<LhScheduleResult>> embryoToLhMap = new HashMap<>();
        for (LhScheduleResult lhResult : lhResults) {
            if (lhResult.getEmbryoCode() != null && lhResult.getId() != null) {
                embryoToLhMap.computeIfAbsent(lhResult.getEmbryoCode(), k -> new ArrayList<>()).add(lhResult);
            }
        }

        for (Map.Entry<String, Integer> entry : formingOutputMap.entrySet()) {
            String embryoCode = entry.getKey();
            int formingOutput = entry.getValue();
            if (formingOutput <= 0) continue;

            List<LhScheduleResult> relatedTasks = embryoToLhMap.get(embryoCode);
            if (relatedTasks == null || relatedTasks.isEmpty()) {
                log.warn("成型产出找不到对应硫化任务: embryoCode={}, 产出={}", embryoCode, formingOutput);
                continue;
            }

            // 计算该胎胚下所有硫化任务的总库存（用于按比例分配）
            int totalAllocated = 0;
            for (LhScheduleResult lh : relatedTasks) {
                String taskKey = String.valueOf(lh.getId());
                totalAllocated += materialStockMap.getOrDefault(taskKey, 0);
            }

            if (totalAllocated <= 0) {
                // 所有任务库存为0，平均分配成型产出
                int avgOutput = formingOutput / relatedTasks.size();
                int remaining = formingOutput - avgOutput * relatedTasks.size();
                for (int i = 0; i < relatedTasks.size(); i++) {
                    String taskKey = String.valueOf(relatedTasks.get(i).getId());
                    int alloc = avgOutput + (i == 0 ? remaining : 0);
                    materialStockMap.merge(taskKey, alloc, Integer::sum);
                }
                log.debug("成型产出平均分配: embryoCode={}, 产出={}, 任务数={}", embryoCode, formingOutput, relatedTasks.size());
            } else {
                // 按库存比例分配成型产出，最后一个任务用倒扣
                int allocatedTotal = 0;
                for (int i = 0; i < relatedTasks.size(); i++) {
                    String taskKey = String.valueOf(relatedTasks.get(i).getId());
                    int currentAlloc = materialStockMap.getOrDefault(taskKey, 0);
                    int outputShare;
                    if (i == relatedTasks.size() - 1) {
                        outputShare = formingOutput - allocatedTotal;
                    } else {
                        outputShare = (int) ((long) formingOutput * currentAlloc / totalAllocated);
                        allocatedTotal += outputShare;
                    }
                    materialStockMap.merge(taskKey, outputShare, Integer::sum);
                    log.debug("成型产出比例分配: lhId={}, 当前库存={}, 分配产出={}", taskKey, currentAlloc, outputShare);
                }
            }
        }

        log.info("materialStockMap 更新完成，共 {} 条", materialStockMap.size());
    }

    /**
     * 更新 CxStock 实体中的 stockNum
     *
     * <p>有效库存 = stockNum - overTimeStock - badNum + modifyNum
     * 所以调整库存时直接修改 stockNum 即可
     *
     * @param context                        排程上下文
     * @param formingOutputMap               胎胚编码 → 成型产出量
     * @param vulcanizingConsumptionByEmbryo 胎胚编码 → 硫化消耗量
     */
    private void updateCxStockEntities(
            ScheduleContextVo context,
            Map<String, Integer> formingOutputMap,
            Map<String, Integer> vulcanizingConsumptionByEmbryo) {

        List<CxStock> stocks = context.getStocks();
        if (stocks == null) {
            return;
        }

        // 收集所有涉及的胎胚编码
        Set<String> allEmbryoCodes = new HashSet<>();
        allEmbryoCodes.addAll(formingOutputMap.keySet());
        allEmbryoCodes.addAll(vulcanizingConsumptionByEmbryo.keySet());

        for (CxStock stock : stocks) {
            String embryoCode = stock.getEmbryoCode();
            if (embryoCode == null || !allEmbryoCodes.contains(embryoCode)) {
                continue;
            }

            int formingOutput = formingOutputMap.getOrDefault(embryoCode, 0);
            int vulcanizingConsumption = vulcanizingConsumptionByEmbryo.getOrDefault(embryoCode, 0);
            int delta = formingOutput - vulcanizingConsumption;

            if (delta != 0) {
                int currentStockNum = stock.getStockNum() != null ? stock.getStockNum() : 0;
                int newStockNum = Math.max(0, currentStockNum + delta);
                stock.setStockNum(newStockNum);
                log.info("库存更新: embryoCode={}, 原stockNum={}, delta=({}-{}={}), 新stockNum={}",
                        embryoCode, currentStockNum, formingOutput, vulcanizingConsumption, delta, newStockNum);
            }
        }
    }

    /**
     * 更新 monthSurplusMap（硫化余量 -= 当天硫化消耗）
     *
     * @param context                        排程上下文
     * @param vulcanizingConsumptionByEmbryo 胎胚编码 → 硫化消耗量
     */
    private void updateMonthSurplus(
            ScheduleContextVo context,
            Map<String, Integer> vulcanizingConsumptionByEmbryo) {

        Map<String, MdmMonthSurplus> monthSurplusMap = context.getMonthSurplusMap();
        if (monthSurplusMap == null) {
            return;
        }

        for (Map.Entry<String, Integer> entry : vulcanizingConsumptionByEmbryo.entrySet()) {
            String materialCode = entry.getKey();
            int consumption = entry.getValue();
            MdmMonthSurplus surplus = monthSurplusMap.get(materialCode);
            if (surplus != null && surplus.getPlanSurplusQty() != null && consumption > 0) {
                BigDecimal oldSurplus = surplus.getPlanSurplusQty();
                BigDecimal newSurplus = oldSurplus.subtract(BigDecimal.valueOf(consumption));
                surplus.setPlanSurplusQty(newSurplus);
                log.info("硫化余量更新: materialCode={}, 原余量={}, 硫化消耗={}, 新余量={}",
                        materialCode, oldSurplus, consumption, newSurplus);
            }
        }
    }

    /**
     * 计算机台小时产能
     *
     * <p>参考 ShiftScheduleService.getMachineHourlyCapacity：
     * <ol>
     *   <li>从 materialLhCapacityMap 获取该物料的日硫化量</li>
     *   <li>从 structureLhRatioMap 通过 结构+机型 获取配比 (lhMachineMaxQty)</li>
     *   <li>成型一条胎的时间(s) = 86400 / (配比 × 日硫化量)</li>
     *   <li>小时产能 = 3600 / 成型一条胎的时间(s)</li>
     * </ol>
     *
     * @param machineCode   机台编码
     * @param materialCode  物料编码
     * @param structureName 结构名称
     * @param context       排程上下文
     * @return 小时产能（条/小时）
     */
    private int calculateHourlyCapacity(String machineCode, String materialCode,
                                        String structureName, ScheduleContextVo context) {
        // 1. 获取日硫化量
        Integer dailyLhCapacity = null;
        Map<String, MonthPlanProductLhCapacityVo> lhCapacityMap = context.getMaterialLhCapacityMap();
        if (lhCapacityMap != null && materialCode != null) {
            MonthPlanProductLhCapacityVo capacityVo = lhCapacityMap.get(materialCode);
            if (capacityVo != null) {
                dailyLhCapacity = capacityVo.getDefaultDayVulcanizationQty();
            }
        }

        // 2. 获取配比
        int ratio = 1;
        if (context.getStructureLhRatioMap() != null && structureName != null && machineCode != null) {
            Map<String, String> machineTypeCodeMap = context.getMachineTypeCodeMap();
            String machineTypeCode = machineTypeCodeMap != null ? machineTypeCodeMap.get(machineCode) : null;
            if (machineTypeCode != null) {
                MdmStructureLhRatio lhRatio = context.getStructureLhRatioMap().get(machineTypeCode + "|" + structureName);
                if (lhRatio != null && lhRatio.getLhMachineMaxQty() != null && lhRatio.getLhMachineMaxQty() > 0) {
                    ratio = lhRatio.getLhMachineMaxQty();
                }
            }
        }

        if (dailyLhCapacity != null && dailyLhCapacity > 0) {
            // 3. 成型一条胎的时间(s) = 86400 / (配比 × 日硫化量)
            BigDecimal timePerTire = BigDecimal.valueOf(86400)
                    .divide(BigDecimal.valueOf((long) ratio * dailyLhCapacity), 2, RoundingMode.HALF_UP);
            // 4. 小时产能 = 3600 / 成型一条胎的时间(s)
            if (timePerTire.compareTo(BigDecimal.ZERO) > 0) {
                return BigDecimal.valueOf(3600)
                        .divide(timePerTire, 0, RoundingMode.FLOOR)
                        .intValue();
            }
        }

        return 12; // 默认值
    }

    /**
     * 重算 formingRemainderMap（成型余量 = 硫化余量 - 库存）
     *
     * @param context 排程上下文
     */
    private void recalculateFormingRemainder(ScheduleContextVo context) {
        Map<String, MdmMonthSurplus> monthSurplusMap = context.getMonthSurplusMap();
        Map<String, Integer> materialStockMap = context.getMaterialStockMap();
        List<LhScheduleResult> lhResults = context.getLhScheduleResults();

        if (monthSurplusMap == null) {
            return;
        }

        // 按胎胚编码汇总库存
        Map<String, Integer> stockByEmbryo = new HashMap<>();
        if (lhResults != null && materialStockMap != null) {
            for (LhScheduleResult lh : lhResults) {
                if (lh.getEmbryoCode() != null && lh.getId() != null) {
                    String taskKey = String.valueOf(lh.getId());
                    int stock = materialStockMap.getOrDefault(taskKey, 0);
                    stockByEmbryo.merge(lh.getEmbryoCode(), stock, Integer::sum);
                }
            }
        }

        // 重算成型余量
        Map<String, Integer> newFormingRemainderMap = new HashMap<>();
        for (Map.Entry<String, MdmMonthSurplus> entry : monthSurplusMap.entrySet()) {
            String materialCode = entry.getKey();
            MdmMonthSurplus surplus = entry.getValue();
            int vulcanizingRemainder = surplus.getPlanSurplusQty() != null
                    ? surplus.getPlanSurplusQty().intValue() : 0;
            int embryoStock = stockByEmbryo.getOrDefault(materialCode, 0);
            int formingRemainder = Math.max(0, vulcanizingRemainder - embryoStock);
            newFormingRemainderMap.put(materialCode, formingRemainder);
            log.debug("成型余量重算: materialCode={}, 硫化余量={}, 库存={}, 成型余量={}",
                    materialCode, vulcanizingRemainder, embryoStock, formingRemainder);
        }

        context.setFormingRemainderMap(newFormingRemainderMap);
        log.info("formingRemainderMap 重算完成，共 {} 条", newFormingRemainderMap.size());
    }
}
