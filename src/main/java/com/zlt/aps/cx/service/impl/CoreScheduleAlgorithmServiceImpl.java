package com.zlt.aps.cx.service.impl;

import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.entity.*;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxStructurePriority;
import com.zlt.aps.cx.entity.config.CxStructureShiftCapacity;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.service.CoreScheduleAlgorithmService;
import com.zlt.aps.mp.api.domain.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 核心排程算法服务实现类
 *
 * <p>实现试错分配、班次均衡、顺位排序等核心算法
 *
 * <p>算法参数从 ScheduleContextDTO 获取，支持动态配置：
 * <ul>
 *   <li>班次数量：context.getScheduleShiftCount()，默认8个班次</li>
 *   <li>波浪比例：context.getWaveRatio()，默认 {1,2,1}</li>
 *   <li>机台种类上限：context.getMaxTypesPerMachine()，默认4</li>
 *   <li>默认整车容量：context.getDefaultTripCapacity()，默认12</li>
 * </ul>
 *
 * <p>所有数据从 ScheduleContextDTO 上下文获取，不直接查询数据库
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoreScheduleAlgorithmServiceImpl implements CoreScheduleAlgorithmService {

    // ==================== 常量定义 ====================

    /** 默认整车容量（当配置中没有时使用） */
    private static final int DEFAULT_TRIP_CAPACITY = 12;

    /** 默认机台种类上限 */
    private static final int DEFAULT_MAX_TYPES_PER_MACHINE = 4;

    /** 默认波浪比例：夜班:早班:中班 = 1:2:1 */
    private static final int[] DEFAULT_WAVE_RATIO = {1, 2, 1};

    /** 默认排程天数 */
    private static final int DEFAULT_SCHEDULE_DAYS = 3;

    /** 默认机台小时产能（条/小时） */
    private static final int DEFAULT_HOURLY_CAPACITY = 50;

    /** 班次编码：夜班 */
    private static final String SHIFT_NIGHT = "SHIFT_NIGHT";

    /** 班次编码：早班 */
    private static final String SHIFT_DAY = "SHIFT_DAY";

    /** 班次编码：中班 */
    private static final String SHIFT_AFTERNOON = "SHIFT_AFTERNOON";

    /** 默认班次编码数组 */
    private static final String[] DEFAULT_SHIFT_CODES = {SHIFT_NIGHT, SHIFT_DAY, SHIFT_AFTERNOON};

    @Override
    public List<CxScheduleResult> executeSchedule(ScheduleContextDTO context) {
        log.info("开始执行排程算法，日期: {}", context.getScheduleDate());

        // 使用 ScheduleServiceImpl.buildScheduleContext 中已加载的班次配置
        List<com.zlt.aps.cx.entity.config.CxShiftConfig> allShiftConfigs = context.getShiftConfigList();
        if (allShiftConfigs == null || allShiftConfigs.isEmpty()) {
            log.error("班次配置为空，请先调用 buildScheduleContext 加载班次配置");
            return new ArrayList<>();
        }

        // 按排程天数分组
        Map<Integer, List<com.zlt.aps.cx.entity.config.CxShiftConfig>> dayShiftMap = allShiftConfigs.stream()
                .filter(c -> c.getScheduleDay() != null)
                .collect(Collectors.groupingBy(com.zlt.aps.cx.entity.config.CxShiftConfig::getScheduleDay));

        // 获取排程天数（从上下文获取，已由 buildScheduleContext 根据班次配置计算）
        int scheduleDays = context.getScheduleDays() != null ? context.getScheduleDays() : DEFAULT_SCHEDULE_DAYS;

        List<CxScheduleResult> allResults = new ArrayList<>();
        
        // 记录每天的机台在产状态（第一天使用初始状态，后续天使用上一天的排程结果）
        Map<String, Set<String>> dailyMachineOnlineEmbryoMap = null;

        // 连续执行多天排程
        for (int day = 1; day <= scheduleDays; day++) {
            List<com.zlt.aps.cx.entity.config.CxShiftConfig> dayShifts = dayShiftMap.get(day);
            if (CollectionUtils.isEmpty(dayShifts)) {
                log.warn("第 {} 天没有配置班次，跳过", day);
                continue;
            }

            // 设置当前天的上下文
            LocalDate currentScheduleDate = context.getScheduleDate().plusDays(day - 1);
            context.setCurrentScheduleDay(day);
            context.setCurrentScheduleDate(currentScheduleDate);
            context.setCurrentShiftConfigs(dayShifts);

            // 检查当前天是否是停产日（节假日）
            if (isStopProductionDay(context, currentScheduleDate)) {
                log.info("第 {} 天日期 {} 是停产日，跳过排程", day, currentScheduleDate);
                continue;
            }

            log.info("执行第 {} 天排程，日期: {}，班次数: {}", day, currentScheduleDate, dayShifts.size());

            // 第一天使用初始机台在产状态，后续天使用上一天的排程结果更新
            if (day == 1) {
                dailyMachineOnlineEmbryoMap = context.getMachineOnlineEmbryoMap();
                if (dailyMachineOnlineEmbryoMap == null) {
                    dailyMachineOnlineEmbryoMap = new HashMap<>();
                }
            }

            // 执行该天的排程，传入当前天的机台在产状态
            List<CxScheduleResult> dayResults = executeDaySchedule(
                    context, day, dayShifts, dailyMachineOnlineEmbryoMap);
            allResults.addAll(dayResults);

            // 更新下一天的机台在产状态（基于当天的排程结果）
            dailyMachineOnlineEmbryoMap = updateMachineOnlineStatus(dayResults, dailyMachineOnlineEmbryoMap);
        }

        log.info("排程算法执行完成，共 {} 天，总结果数: {}", scheduleDays, allResults.size());
        return allResults;
    }

    /**
     * 更新机台在产状态
     *
     * <p>根据当天的排程结果，更新机台在产胎胚映射
     * 用于下一天的续作判断
     *
     * @param dayResults               当天的排程结果
     * @param currentMachineOnlineMap  当前机台在产映射
     * @return 更新后的机台在产映射
     */
    private Map<String, Set<String>> updateMachineOnlineStatus(
            List<CxScheduleResult> dayResults,
            Map<String, Set<String>> currentMachineOnlineMap) {

        // 复制当前映射（避免修改原映射）
        Map<String, Set<String>> newMap = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : currentMachineOnlineMap.entrySet()) {
            newMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        // 根据排程结果更新：每个机台最后一个排产的胎胚即为次日在产胎胚
        // 按机台分组，取最后一个胎胚
        Map<String, CxScheduleResult> lastResultPerMachine = new LinkedHashMap<>();
        for (CxScheduleResult result : dayResults) {
            String machineCode = result.getCxMachineCode();
            if (machineCode != null) {
                lastResultPerMachine.put(machineCode, result);
            }
        }

        for (Map.Entry<String, CxScheduleResult> entry : lastResultPerMachine.entrySet()) {
            String machineCode = entry.getKey();
            String embryoCode = entry.getValue().getEmbryoCode();
            if (embryoCode != null) {
                newMap.computeIfAbsent(machineCode, k -> new HashSet<>()).add(embryoCode);
            }
        }

        log.debug("更新机台在产状态完成，共 {} 台机台", newMap.size());
        return newMap;
    }

    /**
     * 执行单天排程
     *
     * @param context                   排程上下文
     * @param day                       排程天数
     * @param dayShifts                 该天的班次配置
     * @param machineOnlineEmbryoMap    当前的机台在产胎胚映射
     * @return 排程结果列表
     */
    /**
     * 执行单天排程
     *
     * <p>排程流程：
     * <ol>
     *   <li>获取当前机台在产状态</li>
     *   <li>任务分组：续作/试制/新增三类</li>
     *   <li>处理续作任务：按机台分组 → 收尾检测 → 延误检测 → 补做逻辑</li>
     *   <li>处理试制/新增任务：优先级排序 → 成型机选择</li>
     *   <li>班次排产</li>
     *   <li>生成排程结果</li>
     * </ol>
     *
     * @param context                   排程上下文
     * @param day                       排程天数
     * @param dayShifts                 该天的班次配置
     * @param machineOnlineEmbryoMap    当前的机台在产胎胚映射
     * @return 排程结果列表
     */
    private List<CxScheduleResult> executeDaySchedule(
            ScheduleContextDTO context, 
            int day,
            List<com.zlt.aps.cx.entity.config.CxShiftConfig> dayShifts,
            Map<String, Set<String>> machineOnlineEmbryoMap) {

        LocalDate scheduleDate = context.getCurrentScheduleDate() != null 
                ? context.getCurrentScheduleDate() 
                : context.getScheduleDate();
        
        log.info("========== 开始执行第 {} 天排程，日期: {} ==========", day, scheduleDate);

        // ==================== 第一步：任务分组 ====================
        TaskGroupResult taskGroup = groupTasks(context, machineOnlineEmbryoMap, scheduleDate);
        log.info("任务分组完成：续作 {} 个，试制 {} 个，新增 {} 个",
                taskGroup.getContinueTasks().size(),
                taskGroup.getTrialTasks().size(),
                taskGroup.getNewTasks().size());

        // ==================== 第二步：处理续作任务 ====================
        // 续作任务按机台分组，检测收尾和延误，计算补做
        List<MachineAllocationResult> continueAllocations = processContinueTasks(
                taskGroup.getContinueTasks(), context, scheduleDate, dayShifts, day);
        log.info("续作任务处理完成，机台分配数: {}", continueAllocations.size());

        // ==================== 第三步：处理试制/新增任务 ====================
        // 试制和新增任务需要排序，选择成型机
        List<MachineAllocationResult> newAllocations = processTrialAndNewTasks(
                taskGroup.getTrialTasks(), 
                taskGroup.getNewTasks(),
                context, 
                scheduleDate, 
                dayShifts, 
                day,
                continueAllocations);
        log.info("试制/新增任务处理完成，机台分配数: {}", newAllocations.size());

        // ==================== 第四步：合并分配结果 ====================
        List<MachineAllocationResult> allAllocations = new ArrayList<>();
        allAllocations.addAll(continueAllocations);
        allAllocations.addAll(newAllocations);

        // ==================== 第五步：班次排产 ====================
        List<ShiftAllocationResult> shiftAllocations = balanceShiftAllocation(allAllocations, dayShifts, context);
        log.info("班次排产完成");

        // ==================== 第六步：生成排程结果 ====================
        List<CxScheduleResult> results = buildScheduleResults(context, allAllocations, shiftAllocations, null, dayShifts);
        
        log.info("========== 第 {} 天排程完成，排程结果数: {} ==========\n", day, results.size());
        return results;
    }

    // ==================== 任务分组 ====================

    /**
     * 任务分组结果
     */
    @lombok.Data
    private static class TaskGroupResult {
        /** 续作任务：当前机台在产的胎胚 */
        private List<DailyEmbryoTask> continueTasks = new ArrayList<>();
        /** 试制任务：试制/量试任务 */
        private List<DailyEmbryoTask> trialTasks = new ArrayList<>();
        /** 新增任务：非续作、非试制的常规任务 */
        private List<DailyEmbryoTask> newTasks = new ArrayList<>();
    }

    /**
     * 任务分组
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
     * @return 任务分组结果
     */
    private TaskGroupResult groupTasks(
            ScheduleContextDTO context,
            Map<String, Set<String>> machineOnlineEmbryoMap,
            LocalDate scheduleDate) {

        TaskGroupResult result = new TaskGroupResult();

        // 构建基础映射
        Map<String, MdmMaterialInfo> materialMap = buildMaterialMap(context);
        Map<String, CxStock> stockMap = buildStockMap(context);

        // 获取硫化排程结果
        List<LhScheduleResult> lhScheduleResults = context.getLhScheduleResults();
        if (lhScheduleResults == null || lhScheduleResults.isEmpty()) {
            log.warn("硫化排程结果为空，无法分组任务");
            return result;
        }

        // 按胎胚编码分组
        Map<String, List<LhScheduleResult>> embryoTaskMap = lhScheduleResults.stream()
                .filter(r -> r.getEmbryoCode() != null)
                .collect(Collectors.groupingBy(LhScheduleResult::getEmbryoCode));

        // 确保机台在产映射非空
        if (machineOnlineEmbryoMap == null) {
            machineOnlineEmbryoMap = new HashMap<>();
        }

        // 遍历每个胎胚任务
        for (Map.Entry<String, List<LhScheduleResult>> entry : embryoTaskMap.entrySet()) {
            String embryoCode = entry.getKey();
            List<LhScheduleResult> lhResults = entry.getValue();

            // 构建基础任务
            DailyEmbryoTask task = buildBaseTask(embryoCode, lhResults, materialMap, stockMap, context);
            if (task == null) {
                continue;
            }

            // 判断任务类型
            // 1. 续作任务：当前机台在产的胎胚
            List<String> continueMachineCodes = findContinueMachines(embryoCode, machineOnlineEmbryoMap);
            boolean isContinueTask = !continueMachineCodes.isEmpty();

            // 2. 试制任务
            boolean isTrialTask = lhResults.stream()
                    .anyMatch(r -> "1".equals(r.getIsTrial()));

            // 设置任务属性
            task.setIsContinueTask(isContinueTask);
            task.setContinueMachineCodes(continueMachineCodes);
            task.setIsTrialTask(isTrialTask);
            task.setIsFirstTask(!isContinueTask && !isTrialTask);

            // 计算收尾相关属性
            calculateEndingInfo(task, context, scheduleDate);

            // 分组
            if (isContinueTask) {
                result.getContinueTasks().add(task);
            } else if (isTrialTask) {
                result.getTrialTasks().add(task);
            } else {
                result.getNewTasks().add(task);
            }
        }

        // 处理试制任务表中的任务
        processTrialTaskTable(result, materialMap, stockMap, context, scheduleDate);

        return result;
    }

    /**
     * 构建基础任务
     */
    private DailyEmbryoTask buildBaseTask(
            String embryoCode,
            List<LhScheduleResult> lhResults,
            Map<String, MdmMaterialInfo> materialMap,
            Map<String, CxStock> stockMap,
            ScheduleContextDTO context) {

        // 计算硫化需求量
        int totalVulcanizeDemand = lhResults.stream()
                .mapToInt(r -> r.getDailyPlanQty() != null ? r.getDailyPlanQty() : 0)
                .sum();

        // 获取当前库存
        int currentStock = getCurrentStock(lhResults.get(0), stockMap, embryoCode);

        // 获取结构名称
        String structureName = lhResults.get(0).getStructureName();

        // 计算日需求量
        int dailyDemand = calculateDailyDemand(totalVulcanizeDemand, currentStock, structureName, context);

        // 构建任务
        DailyEmbryoTask task = new DailyEmbryoTask();
        task.setMaterialCode(embryoCode);
        task.setVulcanizeDemand(totalVulcanizeDemand);
        task.setCurrentStock(currentStock);

        // 获取物料信息
        MdmMaterialInfo material = materialMap.get(embryoCode);
        if (material != null) {
            task.setMaterialName(material.getMaterialDesc());
            task.setStructureName(material.getStructureName());
        } else {
            task.setMaterialName(embryoCode);
            task.setStructureName(structureName);
        }

        task.setDemandQuantity(dailyDemand);
        task.setAssignedQuantity(0);
        task.setRemainingQuantity(dailyDemand);

        // 是否主销产品
        task.setIsMainProduct(context.getMainProductCodes() != null 
                && context.getMainProductCodes().contains(embryoCode));

        // 计算库存时长
        calculateStockHours(task, lhResults, currentStock);

        // 硫化机台数和模数
        CxStock stock = stockMap.get(embryoCode);
        if (stock != null) {
            task.setVulcanizeMachineCount(stock.getVulcanizeMachineCount());
            task.setVulcanizeMoldCount(stock.getVulcanizeMoldCount());
        }

        return task;
    }

    /**
     * 获取当前库存
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
     * 处理试制任务表中的任务
     */
    private void processTrialTaskTable(
            TaskGroupResult result,
            Map<String, MdmMaterialInfo> materialMap,
            Map<String, CxStock> stockMap,
            ScheduleContextDTO context,
            LocalDate scheduleDate) {

        if (CollectionUtils.isEmpty(context.getTrialTasks())) {
            return;
        }

        for (CxTrialTask trialTask : context.getTrialTasks()) {
            if (!"PENDING".equals(trialTask.getStatus()) && !"SCHEDULED".equals(trialTask.getStatus())) {
                continue;
            }

            String materialCode = trialTask.getMaterialCode();
            MdmMaterialInfo material = materialMap.get(materialCode);
            if (material == null) {
                continue;
            }

            DailyEmbryoTask task = new DailyEmbryoTask();
            task.setMaterialCode(materialCode);
            task.setMaterialName(material.getMaterialDesc());
            task.setStructureName(material.getStructureName());
            task.setDemandQuantity(trialTask.getTrialQuantity() - trialTask.getProducedQuantity());
            task.setAssignedQuantity(0);
            task.setRemainingQuantity(task.getDemandQuantity());
            task.setIsTrialTask(true);
            task.setTrialNo(trialTask.getTrialNo());
            task.setIsContinueTask(false);
            task.setIsFirstTask(true);

            // 计算收尾信息
            calculateEndingInfo(task, context, scheduleDate);

            result.getTrialTasks().add(task);
        }
    }

    // ==================== 续作任务处理 ====================

    /**
     * 处理续作任务
     *
     * <p>流程：
     * <ol>
     *   <li>按机台分组</li>
     *   <li>对每个胎胚检测：10天内收尾？成型余量<400？</li>
     *   <li>10天内要收尾的：检测延误，计算补做</li>
     *   <li>补做逻辑：换算成车，主要产品补到一车</li>
     * </ol>
     *
     * @param continueTasks    续作任务列表
     * @param context          排程上下文
     * @param scheduleDate     排程日期
     * @param dayShifts        班次配置
     * @param day              排程天数
     * @return 机台分配结果列表
     */
    private List<MachineAllocationResult> processContinueTasks(
            List<DailyEmbryoTask> continueTasks,
            ScheduleContextDTO context,
            LocalDate scheduleDate,
            List<com.zlt.aps.cx.entity.config.CxShiftConfig> dayShifts,
            int day) {

        List<MachineAllocationResult> results = new ArrayList<>();

        if (continueTasks.isEmpty()) {
            return results;
        }

        // 按机台分组
        Map<String, List<DailyEmbryoTask>> machineTaskMap = groupTasksByMachine(continueTasks);

        // 获取参数配置
        int endingThreshold = getEndingThreshold(context);
        boolean isOpeningDay = Boolean.TRUE.equals(context.getIsOpeningDay()) && day == 1;

        // 处理每个机台的任务
        for (Map.Entry<String, List<DailyEmbryoTask>> entry : machineTaskMap.entrySet()) {
            String machineCode = entry.getKey();
            List<DailyEmbryoTask> tasks = entry.getValue();

            // 创建机台分配结果
            MachineAllocationResult allocation = createMachineAllocation(machineCode, context);

            // 处理机台上的每个任务
            for (DailyEmbryoTask task : tasks) {
                // 检测是否需要补做（10天内收尾的续作任务）
                if (Boolean.TRUE.equals(task.getIsNearEnding())) {
                    // 计算延误量和补做
                    int catchUpQty = calculateCatchUpQuantity(task, context, scheduleDate);
                    if (catchUpQty > 0) {
                        // 开产日不补做
                        if (!isOpeningDay) {
                            // 换算成车，安排补做
                            int tripCapacity = getTripCapacity(task.getStructureName(), context);
                            int catchUpTrips = convertToTrips(catchUpQty, tripCapacity, task.getIsMainProduct());
                            task.setCatchUpQuantity(catchUpTrips * tripCapacity);
                            log.info("续作任务补做：胎胚={}, 原需求={}, 补做量={}, 车数={}",
                                    task.getMaterialCode(), task.getDemandQuantity(), 
                                    task.getCatchUpQuantity(), catchUpTrips);
                        }
                    }
                }

                // 更新任务需求量（加上补做量）
                int totalDemand = task.getDemandQuantity();
                if (task.getCatchUpQuantity() != null && task.getCatchUpQuantity() > 0) {
                    totalDemand += task.getCatchUpQuantity();
                    task.setDemandQuantity(totalDemand);
                    task.setRemainingQuantity(totalDemand);
                }

                // 分配任务到机台
                allocateTaskToMachine(allocation, task, context);
            }

            // 如果机台有任务收尾了，需要考虑均衡
            boolean hasEndingTask = tasks.stream()
                    .anyMatch(t -> Boolean.TRUE.equals(t.getIsEndingTask()));
            if (hasEndingTask) {
                // TODO: 均衡逻辑待补充
                log.debug("机台 {} 有收尾任务，需要考虑均衡", machineCode);
            }

            if (!allocation.getTaskAllocations().isEmpty()) {
                results.add(allocation);
            }
        }

        return results;
    }

    /**
     * 按机台分组续作任务
     */
    private Map<String, List<DailyEmbryoTask>> groupTasksByMachine(List<DailyEmbryoTask> continueTasks) {
        Map<String, List<DailyEmbryoTask>> machineTaskMap = new LinkedHashMap<>();
        
        for (DailyEmbryoTask task : continueTasks) {
            List<String> machineCodes = task.getContinueMachineCodes();
            if (machineCodes != null && !machineCodes.isEmpty()) {
                // 取第一个机台（续作任务应该在当前机台继续）
                String machineCode = machineCodes.get(0);
                machineTaskMap.computeIfAbsent(machineCode, k -> new ArrayList<>()).add(task);
            }
        }
        
        return machineTaskMap;
    }

    /**
     * 获取收尾阈值参数
     */
    private int getEndingThreshold(ScheduleContextDTO context) {
        Map<String, CxParamConfig> paramConfigMap = context.getParamConfigMap();
        if (paramConfigMap != null) {
            CxParamConfig config = paramConfigMap.get("ENDING_THRESHOLD");
            if (config != null && config.getParamValue() != null) {
                try {
                    return Integer.parseInt(config.getParamValue());
                } catch (NumberFormatException e) {
                    log.warn("收尾阈值参数配置错误: {}", config.getParamValue());
                }
            }
        }
        return 400; // 默认400条
    }

    /**
     * 计算延误量和补做量
     *
     * <p>比较成型余量与月计划当前日期到收尾日的计划量汇总
     *
     * @param task         任务
     * @param context      排程上下文
     * @param scheduleDate 排程日期
     * @return 需要补做的量（条）
     */
    private int calculateCatchUpQuantity(
            DailyEmbryoTask task,
            ScheduleContextDTO context,
            LocalDate scheduleDate) {

        // 成型余量
        Integer formingRemainder = task.getEndingSurplusQty();
        if (formingRemainder == null || formingRemainder <= 0) {
            return 0;
        }

        // 获取收尾日
        LocalDate endingDate = task.getEndingDate();
        if (endingDate == null) {
            return 0;
        }

        // 计算当前日期到收尾日的月计划量汇总
        int plannedQty = calculatePlannedQuantityToDate(
                task.getMaterialCode(), 
                scheduleDate, 
                endingDate, 
                context);

        // 比较成型余量和汇总量
        int gap = formingRemainder - plannedQty;
        if (gap > 0) {
            log.info("任务 {} 有延误，成型余量={}, 计划量={}, 差值={}",
                    task.getMaterialCode(), formingRemainder, plannedQty, gap);
            return gap;
        }

        return 0;
    }

    /**
     * 计算从当前日期到收尾日的月计划量汇总
     */
    private int calculatePlannedQuantityToDate(
            String materialCode,
            LocalDate startDate,
            LocalDate endDate,
            ScheduleContextDTO context) {

        int totalQty = 0;

        // 从 context 获取月计划（需要按物料过滤）
        // TODO: 需要从 context 获取月计划数据
        // 这里使用简化逻辑，实际需要查询 FactoryMonthPlanProductionFinalResult

        return totalQty;
    }

    /**
     * 将数量换算成车
     *
     * @param quantity      数量（条）
     * @param tripCapacity  每车条数
     * @param isMainProduct 是否主要产品
     * @return 车数
     */
    private int convertToTrips(int quantity, int tripCapacity, Boolean isMainProduct) {
        if (quantity <= 0) {
            return 0;
        }

        // 主要产品补到一整车，非主要产品按实际
        if (Boolean.TRUE.equals(isMainProduct)) {
            return (int) Math.ceil((double) quantity / tripCapacity);
        } else {
            return quantity / tripCapacity; // 向下取整
        }
    }

    // ==================== 试制/新增任务处理 ====================

    /**
     * 处理试制和新增任务
     *
     * <p>排序规则：
     * <ol>
     *   <li>按照月计划优先级排序</li>
     *   <li>试制/量试一定在月计划优先</li>
     *   <li>同一个胎胚的试制和量试要在同一台成型机做</li>
     *   <li>新胎胚的优先级高于普通的新增胎胚</li>
     *   <li>试制/量试只能安排在早班或中班（7:30-15:00），数量必须是双数</li>
     * </ol>
     *
     * @param trialTasks        试制任务列表
     * @param newTasks          新增任务列表
     * @param context           排程上下文
     * @param scheduleDate      排程日期
     * @param dayShifts         班次配置
     * @param day               排程天数
     * @param existAllocations  已有的分配结果（续作任务）
     * @return 机台分配结果列表
     */
    private List<MachineAllocationResult> processTrialAndNewTasks(
            List<DailyEmbryoTask> trialTasks,
            List<DailyEmbryoTask> newTasks,
            ScheduleContextDTO context,
            LocalDate scheduleDate,
            List<com.zlt.aps.cx.entity.config.CxShiftConfig> dayShifts,
            int day,
            List<MachineAllocationResult> existAllocations) {

        List<MachineAllocationResult> results = new ArrayList<>();

        // 合并试制和新增任务
        List<DailyEmbryoTask> allTasks = new ArrayList<>();
        allTasks.addAll(trialTasks);
        allTasks.addAll(newTasks);

        if (allTasks.isEmpty()) {
            return results;
        }

        // 排序
        sortTrialAndNewTasks(allTasks, context);

        // 获取已有机台的分配情况（用于均衡）
        Map<String, Integer> machineUsedCapacity = calculateMachineUsedCapacity(existAllocations);

        // 获取可用机台列表
        List<MdmMoldingMachine> availableMachines = context.getAvailableMachines();
        if (availableMachines == null || availableMachines.isEmpty()) {
            log.warn("没有可用的成型机台");
            return results;
        }

        // 为每个任务分配机台
        for (DailyEmbryoTask task : allTasks) {
            // 选择最佳机台
            MdmMoldingMachine bestMachine = selectBestMachineForNewTask(
                    task, availableMachines, machineUsedCapacity, context);

            if (bestMachine == null) {
                log.warn("任务 {} 无可用机台", task.getMaterialCode());
                continue;
            }

            // 获取或创建机台分配结果
            MachineAllocationResult allocation = findOrCreateAllocation(
                    bestMachine.getCxMachineCode(), results, context);

            // 分配任务
            allocateTaskToMachine(allocation, task, context);

            // 更新机台已用产能
            machineUsedCapacity.merge(bestMachine.getCxMachineCode(), 
                    task.getDemandQuantity(), Integer::sum);
        }

        return results;
    }

    /**
     * 排序试制和新增任务
     */
    private void sortTrialAndNewTasks(List<DailyEmbryoTask> tasks, ScheduleContextDTO context) {
        tasks.sort((a, b) -> {
            // 1. 试制/量试优先
            if (Boolean.TRUE.equals(a.getIsTrialTask()) && !Boolean.TRUE.equals(b.getIsTrialTask())) {
                return -1;
            }
            if (!Boolean.TRUE.equals(a.getIsTrialTask()) && Boolean.TRUE.equals(b.getIsTrialTask())) {
                return 1;
            }

            // 2. 按月计划优先级排序（需要从 context 获取）
            int priorityA = getMonthPlanPriority(a.getMaterialCode(), context);
            int priorityB = getMonthPlanPriority(b.getMaterialCode(), context);
            if (priorityA != priorityB) {
                return Integer.compare(priorityA, priorityB);
            }

            // 3. 收尾任务优先
            if (Boolean.TRUE.equals(a.getIsEndingTask()) && !Boolean.TRUE.equals(b.getIsEndingTask())) {
                return -1;
            }
            if (!Boolean.TRUE.equals(a.getIsEndingTask()) && Boolean.TRUE.equals(b.getIsEndingTask())) {
                return 1;
            }

            // 4. 紧急收尾优先
            if (Boolean.TRUE.equals(a.getIsUrgentEnding()) && !Boolean.TRUE.equals(b.getIsUrgentEnding())) {
                return -1;
            }
            if (!Boolean.TRUE.equals(a.getIsUrgentEnding()) && Boolean.TRUE.equals(b.getIsUrgentEnding())) {
                return 1;
            }

            // 5. 按需求量排序（大的优先）
            return Integer.compare(b.getDemandQuantity(), a.getDemandQuantity());
        });
    }

    /**
     * 获取月计划优先级
     */
    private int getMonthPlanPriority(String materialCode, ScheduleContextDTO context) {
        List<CxStructurePriority> priorities = context.getStructurePriorities();
        if (priorities != null) {
            for (CxStructurePriority priority : priorities) {
                // TODO: 需要根据物料编码匹配结构
            }
        }
        return 999; // 默认优先级
    }

    /**
     * 计算机台已用产能
     */
    private Map<String, Integer> calculateMachineUsedCapacity(List<MachineAllocationResult> allocations) {
        Map<String, Integer> usedCapacity = new HashMap<>();
        if (allocations != null) {
            for (MachineAllocationResult allocation : allocations) {
                usedCapacity.put(allocation.getMachineCode(), allocation.getUsedCapacity());
            }
        }
        return usedCapacity;
    }

    /**
     * 为新任务选择最佳机台
     */
    private MdmMoldingMachine selectBestMachineForNewTask(
            DailyEmbryoTask task,
            List<MdmMoldingMachine> availableMachines,
            Map<String, Integer> machineUsedCapacity,
            ScheduleContextDTO context) {

        MdmMoldingMachine bestMachine = null;
        int bestScore = -1;

        for (MdmMoldingMachine machine : availableMachines) {
            // 检查结构约束
            if (!checkStructureConstraint(machine, task.getStructureName(), context)) {
                continue;
            }

            // 计算机台得分（考虑均衡）
            int usedCapacity = machineUsedCapacity.getOrDefault(machine.getCxMachineCode(), 0);
            int remainingCapacity = (machine.getMaxDayCapacity() != null ? machine.getMaxDayCapacity() : 1200) - usedCapacity;

            if (remainingCapacity < task.getDemandQuantity()) {
                continue; // 产能不足
            }

            int score = calculateMachineScoreForNewTask(machine, task, remainingCapacity, context);
            if (score > bestScore) {
                bestScore = score;
                bestMachine = machine;
            }
        }

        return bestMachine;
    }

    /**
     * 检查结构约束
     */
    private boolean checkStructureConstraint(MdmMoldingMachine machine, String structureName, ScheduleContextDTO context) {
        // TODO: 检查机台是否支持该结构
        return true;
    }

    /**
     * 计算新任务机台得分
     */
    private int calculateMachineScoreForNewTask(
            MdmMoldingMachine machine, 
            DailyEmbryoTask task, 
            int remainingCapacity,
            ScheduleContextDTO context) {

        int score = 0;

        // 1. 剩余产能越多得分越高（均衡）
        score += remainingCapacity / 10;

        // 2. 固定生产该结构的机台加分
        if (context.getMachineFixedConfigs() != null) {
            for (MdmCxMachineFixed fixed : context.getMachineFixedConfigs()) {
                if (fixed.getCxMachineCode().equals(machine.getCxMachineCode())) {
                    if (fixed.getFixedStructure1() != null && 
                            fixed.getFixedStructure1().contains(task.getStructureName())) {
                        score += 200;
                    }
                }
            }
        }

        return score;
    }

    /**
     * 查找或创建机台分配结果
     */
    private MachineAllocationResult findOrCreateAllocation(
            String machineCode,
            List<MachineAllocationResult> results,
            ScheduleContextDTO context) {

        for (MachineAllocationResult result : results) {
            if (result.getMachineCode().equals(machineCode)) {
                return result;
            }
        }

        MachineAllocationResult allocation = createMachineAllocation(machineCode, context);
        results.add(allocation);
        return allocation;
    }

    /**
     * 创建机台分配结果
     */
    private MachineAllocationResult createMachineAllocation(String machineCode, ScheduleContextDTO context) {
        MachineAllocationResult allocation = new MachineAllocationResult();
        allocation.setMachineCode(machineCode);
        allocation.setTaskAllocations(new ArrayList<>());
        allocation.setUsedCapacity(0);
        allocation.setRemainingCapacity(getMachineDailyCapacity(machineCode, context));
        return allocation;
    }

    /**
     * 获取机台日产能
     */
    private int getMachineDailyCapacity(String machineCode, ScheduleContextDTO context) {
        if (context.getAvailableMachines() != null) {
            for (MdmMoldingMachine machine : context.getAvailableMachines()) {
                if (machine.getCxMachineCode().equals(machineCode)) {
                    return machine.getMaxDayCapacity() != null ? machine.getMaxDayCapacity() : 1200;
                }
            }
        }
        return 1200;
    }

    /**
     * 分配任务到机台
     */
    private void allocateTaskToMachine(
            MachineAllocationResult allocation,
            DailyEmbryoTask task,
            ScheduleContextDTO context) {

        TaskAllocation taskAllocation = new TaskAllocation();
        taskAllocation.setMaterialCode(task.getMaterialCode());
        taskAllocation.setMaterialName(task.getMaterialName());
        taskAllocation.setStructureName(task.getStructureName());
        taskAllocation.setQuantity(task.getDemandQuantity());
        taskAllocation.setPriority(task.getPriority());
        taskAllocation.setStockHours(task.getStockHours());
        taskAllocation.setIsTrialTask(task.getIsTrialTask());
        taskAllocation.setIsEndingTask(task.getIsEndingTask());
        taskAllocation.setEndingSurplusQty(task.getEndingSurplusQty());
        taskAllocation.setIsMainProduct(task.getIsMainProduct());

        allocation.getTaskAllocations().add(taskAllocation);
        allocation.setUsedCapacity(allocation.getUsedCapacity() + task.getDemandQuantity());
        allocation.setRemainingCapacity(allocation.getRemainingCapacity() - task.getDemandQuantity());
    }

    // ==================== 收尾信息计算 ====================

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
     *
     * @param task         任务
     * @param context      排程上下文
     * @param scheduleDate 排程日期
     */
    private void calculateEndingInfo(
            DailyEmbryoTask task,
            ScheduleContextDTO context,
            LocalDate scheduleDate) {

        String materialCode = task.getMaterialCode();

        // 获取成型余量（从预计算的映射中获取）
        Map<String, Integer> formingRemainderMap = context.getFormingRemainderMap();
        Integer formingRemainder = null;
        Integer vulcanizeSurplusQty = null;

        // 从月计划余量获取硫化余量
        if (context.getMonthSurplusMap() != null) {
            MdmMonthSurplus monthSurplus = context.getMonthSurplusMap().get(materialCode);
            if (monthSurplus != null && monthSurplus.getPlanSurplusQty() != null) {
                vulcanizeSurplusQty = monthSurplus.getPlanSurplusQty().intValue();
            }
        }

        // 获取成型余量
        if (formingRemainderMap != null && formingRemainderMap.containsKey(materialCode)) {
            formingRemainder = formingRemainderMap.get(materialCode);
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
        LocalDate endingDate = findEndingDate(materialCode, context);
        task.setEndingDate(endingDate);

        if (endingDate != null) {
            int daysToEnding = (int) java.time.temporal.ChronoUnit.DAYS.between(scheduleDate, endingDate);
            task.setDaysToEnding(daysToEnding);

            // 判断是否10天内收尾
            boolean isNearEnding = daysToEnding >= 0 && daysToEnding <= 10;
            task.setIsNearEnding(isNearEnding);

            // 判断是否3天内收尾（紧急）
            boolean isUrgentEnding = daysToEnding >= 0 && daysToEnding <= 3;
            task.setIsUrgentEnding(isUrgentEnding);

            if (isUrgentEnding) {
                log.info("紧急收尾任务：物料={}, 收尾日={}, 距收尾{}天", 
                        materialCode, endingDate, daysToEnding);
            }
        }

        // 成型余量小于阈值也标记为紧急收尾
        int threshold = getEndingThreshold(context);
        if (formingRemainder != null && formingRemainder < threshold && formingRemainder > 0) {
            task.setIsUrgentEnding(true);
            log.info("成型余量低于阈值的收尾任务：物料={}, 成型余量={}, 阈值={}",
                    materialCode, formingRemainder, threshold);
        }

        // 计算优先级
        task.setPriority(calculateTaskPriority(task, context));
    }

    /**
     * 查找物料收尾日
     */
    private LocalDate findEndingDate(String materialCode, ScheduleContextDTO context) {
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
    private int calculateTaskPriority(DailyEmbryoTask task, ScheduleContextDTO context) {
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

        // 库存紧张
        if (task.getStockHours() != null) {
            if (task.getStockHours().compareTo(new BigDecimal("4")) < 0) {
                score += 800;
            } else if (task.getStockHours().compareTo(new BigDecimal("6")) < 0) {
                score += 400;
            }
        }

        // 主销产品
        if (Boolean.TRUE.equals(task.getIsMainProduct())) {
            score += 200;
        }

        return score;
    }

    @Override
    public List<DailyEmbryoTask> calculateDailyEmbryoTasks(
            ScheduleContextDTO context,
            Map<String, Set<String>> machineOnlineEmbryoMap) {
        
        LocalDate scheduleDate = context.getCurrentScheduleDate() != null 
                ? context.getCurrentScheduleDate() 
                : context.getScheduleDate();
        
        TaskGroupResult groupResult = groupTasks(context, machineOnlineEmbryoMap, scheduleDate);
        
        List<DailyEmbryoTask> allTasks = new ArrayList<>();
        allTasks.addAll(groupResult.getContinueTasks());
        allTasks.addAll(groupResult.getTrialTasks());
        allTasks.addAll(groupResult.getNewTasks());
        
        return allTasks;
    }

    @Override
    public List<MachineAllocationResult> allocateTasksToMachines(
            List<DailyEmbryoTask> tasks,
            ScheduleContextDTO context) {

        List<MachineAllocationResult> results = new ArrayList<>();

        // 获取当前排程日期
        LocalDate currentDate = context.getCurrentScheduleDate();
        if (currentDate == null) {
            currentDate = context.getScheduleDate();
        }

        // 初始化机台状态（传入当前日期，用于计算停机扣减）
        Map<String, MachineAllocationResult> machineStatusMap = initMachineStatus(context, currentDate);

        // 记录每个机台已分配的物料编码（用于种类上限检查）
        Map<String, Set<String>> machineMaterialMap = new HashMap<>();

        // 尝试分配每个任务
        for (DailyEmbryoTask task : tasks) {
            boolean allocated = false;
            int remainingQty = task.getDemandQuantity();
            int retryCount = 0;
            int maxRetry = 100; // 最大重试次数

            while (remainingQty > 0 && retryCount < maxRetry) {
                // 找最佳机台
                MdmMoldingMachine bestMachine = findBestMachine(
                        task, machineStatusMap, machineMaterialMap, context);

                if (bestMachine == null) {
                    log.debug("任务 {} 无可用机台，剩余量: {}", task.getMaterialCode(), remainingQty);
                    break;
                }

                MachineAllocationResult machineResult = machineStatusMap.get(bestMachine.getCxMachineCode());

                // 计算可分配量
                int assignQty = Math.min(remainingQty, machineResult.getRemainingCapacity());

                // 检查种类上限
                Set<String> materials = machineMaterialMap.computeIfAbsent(
                        bestMachine.getCxMachineCode(), k -> new HashSet<>());
                int maxTypes = context.getMaxTypesPerMachine() != null
                        ? context.getMaxTypesPerMachine()
                        : DEFAULT_MAX_TYPES_PER_MACHINE;
                if (!materials.contains(task.getMaterialCode()) && materials.size() >= maxTypes) {
                    // 种类已满，跳过此机台
                    machineResult = null;
                    retryCount++;
                    continue;
                }

                // 执行分配
                if (assignQty > 0) {
                    TaskAllocation allocation = new TaskAllocation();
                    allocation.setMaterialCode(task.getMaterialCode());
                    allocation.setMaterialName(task.getMaterialName());
                    allocation.setStructureName(task.getStructureName());
                    allocation.setQuantity(assignQty);
                    allocation.setPriority(task.getPriority());
                    allocation.setStockHours(task.getStockHours());
                    allocation.setIsEndingTask(task.getIsEndingTask());
                    allocation.setEndingSurplusQty(task.getEndingSurplusQty());
                    allocation.setIsMainProduct(task.getIsMainProduct());
                    allocation.setIsTrialTask(task.getIsTrialTask());

                    machineResult.getTaskAllocations().add(allocation);
                    machineResult.setUsedCapacity(machineResult.getUsedCapacity() + assignQty);
                    machineResult.setRemainingCapacity(machineResult.getRemainingCapacity() - assignQty);
                    materials.add(task.getMaterialCode());
                    machineResult.setAssignedTypes(materials.size());

                    remainingQty -= assignQty;
                    allocated = true;
                }

                retryCount++;
            }

            if (allocated) {
                task.setAssignedQuantity(task.getDemandQuantity() - remainingQty);
                task.setRemainingQuantity(remainingQty);
            }
        }

        // 收集有任务分配的机台
        for (MachineAllocationResult result : machineStatusMap.values()) {
            if (!CollectionUtils.isEmpty(result.getTaskAllocations())) {
                results.add(result);
            }
        }

        return results;
    }

    @Override
    public List<ShiftAllocationResult> balanceShiftAllocation(
            List<MachineAllocationResult> allocations,
            ScheduleContextDTO context) {

        List<ShiftAllocationResult> results = new ArrayList<>();

        // 从上下文获取班次配置
        String[] shiftCodes = context.getShiftCodes();
        if (shiftCodes == null || shiftCodes.length == 0) {
            shiftCodes = DEFAULT_SHIFT_CODES;
        }

        // 从上下文构建结构班产配置映射
        Map<String, Map<String, CxStructureShiftCapacity>> structureCapacityMap = buildStructureShiftCapacityMap(context);

        for (MachineAllocationResult allocation : allocations) {
            ShiftAllocationResult shiftResult = new ShiftAllocationResult();
            shiftResult.setMachineCode(allocation.getMachineCode());
            shiftResult.setTasks(allocation.getTaskAllocations());

            Map<String, Integer> shiftPlanQty = new LinkedHashMap<>();
            int totalQty = allocation.getUsedCapacity();

            // 获取机台最大产能
            Integer maxDailyCapacity = allocation.getDailyCapacity();

            // 按任务结构获取班产整车数，计算波浪分配
            Map<String, Integer> structureWaveAllocation = calculateStructureWaveAllocation(
                    allocation.getTaskAllocations(),
                    structureCapacityMap,
                    maxDailyCapacity,
                    shiftCodes,
                    context);

            // 汇总各班次分配量
            for (String shiftCode : shiftCodes) {
                int shiftQty = structureWaveAllocation.getOrDefault(shiftCode, 0);
                shiftPlanQty.put(shiftCode, shiftQty);
            }

            // 处理特殊情况：开产首班不排关键产品
            if (Boolean.TRUE.equals(context.getIsOpeningDay())) {
                String firstShift = context.getFormingStartShift();
                if (firstShift == null) {
                    firstShift = SHIFT_DAY; // 默认早班为开产首班
                }

                Set<String> keyProductCodes = context.getKeyProductCodes();
                if (keyProductCodes != null && !keyProductCodes.isEmpty()) {
                    // 计算首班中关键产品的量，移到下一班次
                    int keyProductQty = 0;
                    for (TaskAllocation task : allocation.getTaskAllocations()) {
                        if (keyProductCodes.contains(task.getMaterialCode())) {
                            // 关键产品，从首班移出
                            keyProductQty += task.getQuantity();
                        }
                    }

                    if (keyProductQty > 0) {
                        // 首班减去关键产品量
                        int firstShiftQty = shiftPlanQty.getOrDefault(firstShift, 0);
                        int adjustedQty = Math.max(firstShiftQty - keyProductQty, 0);
                        shiftPlanQty.put(firstShift, roundToTrip(adjustedQty, "FLOOR"));

                        // 关键产品量加到下一班次
                        String secondShift = getNextShift(firstShift);
                        int secondShiftQty = shiftPlanQty.getOrDefault(secondShift, 0);
                        shiftPlanQty.put(secondShift, secondShiftQty + roundToTrip(keyProductQty, "CEILING"));

                        log.debug("开产首班 {} 移出关键产品 {} 条到 {}",
                                firstShift, keyProductQty, secondShift);
                    }
                }
            }

            shiftResult.setShiftPlanQty(shiftPlanQty);
            results.add(shiftResult);
        }

        return results;
    }

    /**
     * 第三步：班次均衡分配（接收动态班次配置）
     * 用于多天排程场景，每天可能有不同的班次配置
     *
     * @param allocations 机台分配结果
     * @param dayShifts   该天的班次配置列表
     * @param context     排程上下文
     * @return 班次分配结果
     */
    public List<ShiftAllocationResult> balanceShiftAllocation(
            List<MachineAllocationResult> allocations,
            List<com.zlt.aps.cx.entity.config.CxShiftConfig> dayShifts,
            ScheduleContextDTO context) {

        List<ShiftAllocationResult> results = new ArrayList<>();

        if (CollectionUtils.isEmpty(dayShifts)) {
            log.warn("班次配置为空，使用默认配置");
            return balanceShiftAllocation(allocations, context);
        }

        // 从班次配置提取班次编码数组
        String[] shiftCodes = dayShifts.stream()
                .map(com.zlt.aps.cx.entity.config.CxShiftConfig::getShiftCode)
                .toArray(String[]::new);

        // 加载结构班产配置
        // 从上下文构建结构班产配置映射
        Map<String, Map<String, CxStructureShiftCapacity>> structureCapacityMap = buildStructureShiftCapacityMap(context);

        for (MachineAllocationResult allocation : allocations) {
            ShiftAllocationResult shiftResult = new ShiftAllocationResult();
            shiftResult.setMachineCode(allocation.getMachineCode());
            shiftResult.setTasks(allocation.getTaskAllocations());

            Map<String, Integer> shiftPlanQty = new LinkedHashMap<>();
            int totalQty = allocation.getUsedCapacity();

            // 获取机台最大产能
            Integer maxDailyCapacity = allocation.getDailyCapacity();

            // 按任务结构获取班产整车数，计算波浪分配
            Map<String, Integer> structureWaveAllocation = calculateStructureWaveAllocation(
                    allocation.getTaskAllocations(),
                    structureCapacityMap,
                    maxDailyCapacity,
                    shiftCodes,
                    context);

            // 汇总各班次分配量
            for (com.zlt.aps.cx.entity.config.CxShiftConfig shiftConfig : dayShifts) {
                String shiftCode = shiftConfig.getShiftCode();
                int shiftQty = structureWaveAllocation.getOrDefault(shiftCode, 0);
                shiftPlanQty.put(shiftCode, shiftQty);
            }

            // 处理特殊情况：开产首班不排关键产品
            if (Boolean.TRUE.equals(context.getIsOpeningDay()) && context.getCurrentScheduleDay() == 1) {
                String firstShift = dayShifts.get(0).getShiftCode(); // 当天第一个班次

                Set<String> keyProductCodes = context.getKeyProductCodes();
                if (keyProductCodes != null && !keyProductCodes.isEmpty() && dayShifts.size() > 1) {
                    // 计算首班中关键产品的量，移到下一班次
                    int keyProductQty = 0;
                    for (TaskAllocation task : allocation.getTaskAllocations()) {
                        if (keyProductCodes.contains(task.getMaterialCode())) {
                            keyProductQty += task.getQuantity();
                        }
                    }

                    if (keyProductQty > 0) {
                        // 首班减去关键产品量
                        int firstShiftQty = shiftPlanQty.getOrDefault(firstShift, 0);
                        int adjustedQty = Math.max(firstShiftQty - keyProductQty, 0);
                        shiftPlanQty.put(firstShift, roundToTrip(adjustedQty, "FLOOR"));

                        // 关键产品量加到下一班次
                        String secondShift = dayShifts.get(1).getShiftCode();
                        int secondShiftQty = shiftPlanQty.getOrDefault(secondShift, 0);
                        shiftPlanQty.put(secondShift, secondShiftQty + roundToTrip(keyProductQty, "CEILING"));

                        log.debug("开产首班 {} 移出关键产品 {} 条到 {}",
                                firstShift, keyProductQty, secondShift);
                    }
                }
            }

            shiftResult.setShiftPlanQty(shiftPlanQty);
            results.add(shiftResult);
        }

        return results;
    }

    /**
     * 获取下一个班次
     *
     * @param currentShift 当前班次编码
     * @return 下一个班次编码
     */
    private String getNextShift(String currentShift) {
        if (SHIFT_NIGHT.equals(currentShift)) {
            return SHIFT_DAY;
        } else if (SHIFT_DAY.equals(currentShift)) {
            return SHIFT_AFTERNOON;
        } else {
            return SHIFT_NIGHT;
        }
    }

    /**
     * 从上下文构建结构班产配置映射
     *
     * <p>数据已在 ScheduleServiceImpl.buildScheduleContext 中预加载
     *
     * @param context 排程上下文
     * @return Map<结构编码, Map<班次编码, 班产配置>>
     */
    private Map<String, Map<String, CxStructureShiftCapacity>> buildStructureShiftCapacityMap(ScheduleContextDTO context) {
        Map<String, Map<String, CxStructureShiftCapacity>> result = new HashMap<>();

        List<CxStructureShiftCapacity> capacities = context.getStructureShiftCapacities();
        if (capacities == null || capacities.isEmpty()) {
            log.warn("上下文中结构班产配置为空");
            return result;
        }

        for (CxStructureShiftCapacity capacity : capacities) {
            String structureCode = capacity.getStructureCode();
            String shiftCode = capacity.getShiftCode();
            if (structureCode != null && shiftCode != null) {
                result.computeIfAbsent(structureCode, k -> new HashMap<>())
                        .put(shiftCode, capacity);
            }
        }

        log.debug("构建结构班产配置映射完成，共 {} 个结构", result.size());
        return result;
    }

    /**
     * 按结构计算波浪分配
     * 从结构班产表获取整车条数，按波浪方式生成硫化需求量
     */
    private Map<String, Integer> calculateStructureWaveAllocation(
            List<TaskAllocation> tasks,
            Map<String, Map<String, CxStructureShiftCapacity>> structureCapacityMap,
            Integer maxDailyCapacity,
            String[] shiftCodes,
            ScheduleContextDTO context) {

        Map<String, Integer> shiftTotalQty = new LinkedHashMap<>();
        for (String shiftCode : shiftCodes) {
            shiftTotalQty.put(shiftCode, 0);
        }

        int totalAssigned = 0;

        for (TaskAllocation task : tasks) {
            String structureCode = task.getStructureName();
            int taskQty = task.getQuantity();

            // 获取该结构的班产配置
            Map<String, CxStructureShiftCapacity> shiftCapacityMap = structureCapacityMap.get(structureCode);

            if (shiftCapacityMap != null && !shiftCapacityMap.isEmpty()) {
                // 按班产配置计算各班次分配量
                int[] shiftQty = calculateShiftQtyByCapacity(taskQty, structureCode, shiftCapacityMap, shiftCodes, context);

                for (int i = 0; i < shiftCodes.length; i++) {
                    int qty = shiftQty[i];
                    shiftTotalQty.merge(shiftCodes[i], qty, Integer::sum);
                    totalAssigned += qty;
                }
            } else {
                // 无班产配置，使用默认波浪比例
                int[] waveQty = calculateWaveAllocation(taskQty, shiftCodes, context);

                for (int i = 0; i < shiftCodes.length; i++) {
                    shiftTotalQty.merge(shiftCodes[i], waveQty[i], Integer::sum);
                    totalAssigned += waveQty[i];
                }
            }
        }

        // 检查是否超过机台最大产能
        if (maxDailyCapacity != null && totalAssigned > maxDailyCapacity) {
            // 按比例缩减
            double ratio = (double) maxDailyCapacity / totalAssigned;
            int newTotal = 0;

            for (String shiftCode : shiftCodes) {
                int originalQty = shiftTotalQty.get(shiftCode);
                int adjustedQty = roundToTrip((int) (originalQty * ratio), "FLOOR");
                shiftTotalQty.put(shiftCode, adjustedQty);
                newTotal += adjustedQty;
            }

            log.debug("班次分配量超过机台最大产能，已按比例缩减：{} -> {}", totalAssigned, newTotal);
        }

        return shiftTotalQty;
    }

    /**
     * 根据结构班产配置计算各班次分配量
     * 按波浪方式分配
     */
    private int[] calculateShiftQtyByCapacity(
            int taskQty,
            String structureCode,
            Map<String, CxStructureShiftCapacity> shiftCapacityMap,
            String[] shiftCodes,
            ScheduleContextDTO context) {

        int[] result = new int[shiftCodes.length];

        // 计算总班产整车数
        int totalTripQty = 0;
        int[] tripQtyPerShift = new int[shiftCodes.length];

        for (int i = 0; i < shiftCodes.length; i++) {
            CxStructureShiftCapacity capacity = shiftCapacityMap.get(shiftCodes[i]);
            if (capacity != null && capacity.getTripQty() != null) {
                tripQtyPerShift[i] = capacity.getTripQty();
                totalTripQty += capacity.getTripQty();
            }
        }

        // 如果没有班产配置，使用默认波浪比例
        if (totalTripQty == 0) {
            return calculateWaveAllocation(taskQty, shiftCodes, context);
        }

        // 获取波浪比例
        int[] waveRatio = context.getWaveRatio();
        if (waveRatio == null) {
            waveRatio = DEFAULT_WAVE_RATIO;
        }

        // 根据班次编码调整波浪比例（映射夜早中比例到正确的班次位置）
        int[] adjustedRatio = adjustWaveRatio(waveRatio, shiftCodes);

        // 计算总比例
        int totalRatio = 0;
        for (int ratio : adjustedRatio) {
            totalRatio += ratio;
        }

        int remainingQty = taskQty;

        // 获取整车容量（用于取整）
        int tripCapacity = getTripCapacity(structureCode, shiftCapacityMap);

        for (int i = 0; i < shiftCodes.length; i++) {
            // 按波浪比例计算该班次分配量
            int shiftQty = taskQty * adjustedRatio[i] / totalRatio;

            // 限制不超过该班次的班产整车条数
            int maxShiftQty = tripQtyPerShift[i];  // tripQtyPerShift已经是条数，不需要再乘整车容量
            shiftQty = Math.min(shiftQty, maxShiftQty);

            // 整车取整
            shiftQty = roundToTripQty(shiftQty, tripCapacity, "ROUND");

            result[i] = shiftQty;
            remainingQty -= shiftQty;
        }

        // 分配余量（优先分配给早班）
        if (remainingQty > 0) {
            for (int i = 1; i < shiftCodes.length && remainingQty >= tripCapacity; i++) {
                int idx = (i + 1) % shiftCodes.length; // 早班优先
                CxStructureShiftCapacity capacity = shiftCapacityMap.get(shiftCodes[idx]);
                int maxQty = capacity != null && capacity.getTripQty() != null
                        ? capacity.getTripQty()  // tripQty已经是条数
                        : Integer.MAX_VALUE;

                if (result[idx] + tripCapacity <= maxQty) {
                    result[idx] += tripCapacity;
                    remainingQty -= tripCapacity;
                }
            }
        }

        return result;
    }

    @Override
    public List<CxScheduleDetail> calculateSequence(
            List<ShiftAllocationResult> shiftAllocations,
            ScheduleContextDTO context) {

        List<CxScheduleDetail> allDetails = new ArrayList<>();
        LocalDate scheduleDate = context.getScheduleDate();

        // 班次时间配置（使用常量）
        Map<String, int[]> shiftTimeMap = new HashMap<>();
        shiftTimeMap.put(SHIFT_NIGHT, new int[]{0, 8});
        shiftTimeMap.put(SHIFT_DAY, new int[]{8, 16});
        shiftTimeMap.put(SHIFT_AFTERNOON, new int[]{16, 24});

        int globalSequence = 1;

        for (ShiftAllocationResult shiftAllocation : shiftAllocations) {
            // 对每个班次的任务按优先级排序
            // 排序顺序：续作 > 试制 > 收尾 > 按库存时长
            List<TaskAllocation> sortedTasks = shiftAllocation.getTasks().stream()
                    .sorted((a, b) -> {
                        // 收尾任务优先（在班次内排序）
                        if (Boolean.TRUE.equals(a.getIsEndingTask()) && !Boolean.TRUE.equals(b.getIsEndingTask())) {
                            return -1;
                        }
                        if (!Boolean.TRUE.equals(a.getIsEndingTask()) && Boolean.TRUE.equals(b.getIsEndingTask())) {
                            return 1;
                        }
                        // 按库存时长排序（越短越急）
                        if (a.getStockHours() != null && b.getStockHours() != null) {
                            return a.getStockHours().compareTo(b.getStockHours());
                        }
                        return 0;
                    })
                    .collect(Collectors.toList());

            int tripNo = 1;
            for (TaskAllocation task : sortedTasks) {
                int qty = task.getQuantity();

                // 获取该结构的整车容量
                int tripCapacity = getTripCapacity(task.getStructureName(), shiftAllocation.getMachineCode(), context);

                int tripCount = (int) Math.ceil((double) qty / tripCapacity);

                for (int t = 1; t <= tripCount; t++) {
                    CxScheduleDetail detail = new CxScheduleDetail();
                    detail.setScheduleDate(scheduleDate);
                    detail.setCxMachineCode(shiftAllocation.getMachineCode());
                    detail.setCxMachineName(shiftAllocation.getMachineName());
                    detail.setEmbryoCode(task.getMaterialCode());
                    detail.setTripNo(tripNo++);
                    detail.setTripCapacity(tripCapacity);  // 使用结构对应的整车容量
                    detail.setTripActualQty(0);
                    detail.setSequence(globalSequence++);
                    detail.setSequenceInGroup(t);
                    detail.setIsEnding(Boolean.TRUE.equals(task.getIsEndingTask()) ? 1 : 0);
                    detail.setIsTrial(Boolean.TRUE.equals(task.getIsTrialTask()) ? 1 : 0);
                    detail.setIsPrecision(0);
                    detail.setIsContinue(0);

                    // 计算计划时间
                    int[] shiftTime = shiftTimeMap.values().iterator().next();
                    LocalDateTime planStartTime = LocalDateTime.of(scheduleDate, LocalTime.of(shiftTime[0], 0));
                    planStartTime = planStartTime.plusMinutes((long) (tripNo - 1) * 30);
                    detail.setPlanStartTime(planStartTime);
                    detail.setPlanEndTime(planStartTime.plusMinutes(30));

                    allDetails.add(detail);
                }
            }
        }

        return allDetails;
    }

    @Override
    public BigDecimal calculateStockHours(
            CxStock stock,
            Integer vulcanizeMachineCount,
            Integer vulcanizeMoldCount) {

        if (stock == null) {
            return BigDecimal.ZERO;
        }

        Integer effectiveStock = stock.getEffectiveStock();
        if (effectiveStock <= 0) {
            return BigDecimal.ZERO;
        }

        if (vulcanizeMachineCount == null || vulcanizeMachineCount == 0 ||
                vulcanizeMoldCount == null || vulcanizeMoldCount == 0) {
            return BigDecimal.ZERO;
        }

        // 库存时长 = 胎胚库存 / (硫化机数 × 单台模数)
        // 假设每小时每模硫化量
        BigDecimal hourlyOutput = BigDecimal.valueOf(vulcanizeMachineCount)
                .multiply(BigDecimal.valueOf(vulcanizeMoldCount))
                .multiply(BigDecimal.valueOf(0.5)); // 假设每模每小时0.5条

        return BigDecimal.valueOf(effectiveStock)
                .divide(hourlyOutput, 2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateDailyDemand(
            MdmMaterialInfo material,
            CxStock stock,
            ScheduleContextDTO context) {

        // 获取参数配置
        CxParamConfig lossRateConfig = context.getParamConfigMap().get("LOSS_RATE");
        BigDecimal lossRate = lossRateConfig != null
                ? new BigDecimal(lossRateConfig.getParamValue())
                : new BigDecimal("0.02"); // 默认2%损耗率

        // 获取硫化消耗量（从结构硫化配比中获取）
        BigDecimal vulcanizeDemand = getVulcanizeDemand(material, context);

        // 获取库存分配量
        BigDecimal stockAllocation = BigDecimal.ZERO;
        if (stock != null) {
            Integer effectiveStock = stock.getEffectiveStock();
            if (effectiveStock > 0) {
                stockAllocation = BigDecimal.valueOf(effectiveStock);
            }
        }

        // 日胎胚计划量 = (硫化消耗量 - 库存分配量) × (1 + 损耗率)
        BigDecimal demand = vulcanizeDemand.subtract(stockAllocation);
        if (demand.compareTo(BigDecimal.ZERO) < 0) {
            demand = BigDecimal.ZERO;
        }
        demand = demand.multiply(BigDecimal.ONE.add(lossRate));

        // 整车取整
        return new BigDecimal(roundToTrip(demand.intValue(), "CEILING"));
    }

    @Override
    public boolean checkStructureConstraint(
            MdmMoldingMachine machine,
            MdmMaterialInfo material,
            ScheduleContextDTO context) {

        if (machine == null || material == null) {
            return false;
        }

        String structure = material.getStructureName();

        // 检查固定机台配置
        for (MdmCxMachineFixed fixed : context.getMachineFixedConfigs()) {
            if (fixed.getCxMachineCode().equals(machine.getCxMachineCode())) {
                // 检查不可作业结构
                if (fixed.getDisableStructure() != null &&
                        fixed.getDisableStructure().contains(structure)) {
                    return false;
                }
                // 检查不可作业SKU
                if (fixed.getDisableMaterialCode() != null &&
                        fixed.getDisableMaterialCode().contains(material.getMaterialCode())) {
                    return false;
                }
            }
        }

        // 检查硫化配比
        for (MdmStructureLhRatio ratio : context.getStructureLhRatios()) {
            if (ratio.getStructureName().equals(structure)) {
                // 检查机型是否匹配
                if (ratio.getCxMachineTypeCode() != null &&
                        !ratio.getCxMachineTypeCode().equals(machine.getCxMachineTypeCode())) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean checkTypeLimit(
            MdmMoldingMachine machine,
            int currentTypes,
            MdmMaterialInfo newMaterial,
            ScheduleContextDTO context) {

        int maxTypes = context.getMaxTypesPerMachine() != null
                ? context.getMaxTypesPerMachine()
                : DEFAULT_MAX_TYPES_PER_MACHINE;

        // 检查固定机台配置（强制保留的情况）
        for (MdmCxMachineFixed fixed : context.getMachineFixedConfigs()) {
            if (fixed.getCxMachineCode().equals(machine.getCxMachineCode())) {
                // 如果是固定SKU，不算新种类
                if (fixed.getFixedMaterialCode() != null &&
                        fixed.getFixedMaterialCode().contains(newMaterial.getMaterialCode())) {
                    return true;
                }
            }
        }

        return currentTypes < maxTypes;
    }

    @Override
    public int calculatePriorityScore(
            MdmMaterialInfo material,
            CxStock stock,
            ScheduleContextDTO context) {

        int score = 0;

        // 收尾任务通过月度计划余量计算
        // 收尾余量 = 硫化余量(PLAN_SURPLUS_QTY) - 胎胚库存
        // 硫化余量来自 t_mdm_month_surplus.PLAN_SURPLUS_QTY（已由系统计算）
        if (context.getMonthSurplusMap() != null) {
            com.zlt.aps.mp.api.domain.entity.MdmMonthSurplus monthSurplus =
                    context.getMonthSurplusMap().get(material.getMaterialCode());
            if (monthSurplus != null && monthSurplus.getPlanSurplusQty() != null) {
                int stockQty = stock != null ? stock.getEffectiveStock() : 0;
                // 收尾余量 = 硫化余量 - 胎胚库存
                int endingSurplusQty = monthSurplus.getPlanSurplusQty().intValue() - stockQty;
                if (endingSurplusQty <= 0) {
                    score += 2000; // 收尾任务加分
                    score += Math.max(0, 500 - endingSurplusQty * 10); // 余量越小越紧急
                }
            }
        }

        // 检查库存预警
        if (stock != null && stock.getStockHours() != null) {
            if (stock.getStockHours().compareTo(new BigDecimal("4")) < 0) {
                score += 800; // 库存预警加分
            } else if (stock.getStockHours().compareTo(new BigDecimal("6")) < 0) {
                score += 400;
            }
        }

        // 关键产品加分（从关键产品配置表判断）
        Set<String> keyProductCodes = context.getKeyProductCodes();
        if (keyProductCodes != null && keyProductCodes.contains(material.getMaterialCode())) {
            score += 200;
        }

        // 结构优先级加分
        if (context.getStructurePriorities() != null) {
            for (CxStructurePriority priority : context.getStructurePriorities()) {
                if (priority.getStructureName().equals(material.getStructureName())) {
                    score += priority.getPriorityLevel() * 10;
                    break;
                }
            }
        }

        return score;
    }

    /**
     * 计算优先级分数（新方法，支持任务对象）
     *
     * 优先级规则：
     * 1. 紧急收尾任务（3天内收尾）> 普通收尾任务 > 试制任务 > 续作任务 > 首排任务 > 其他
     * 2. 同级别内按库存紧张程度排序
     * 3. 库存 < 4小时：最紧急
     * 4. 库存 < 6小时：次紧急
     */
    private int calculatePriorityScoreNew(
            DailyEmbryoTask task,
            MdmMaterialInfo material,
            CxStock stock,
            ScheduleContextDTO context) {

        int score = 0;

        // 1. 紧急收尾任务（3天内收尾，最高优先级）
        if (Boolean.TRUE.equals(task.getIsUrgentEnding())) {
            score += 3000; // 最高优先级
            log.debug("紧急收尾任务: {} 获得3000优先级加分", task.getMaterialCode());
        }
        // 2. 普通收尾任务（收尾余量 <= 0 表示需要收尾）
        else if (Boolean.TRUE.equals(task.getIsEndingTask())) {
            score += 2000;
            // 收尾余量越小越紧急
            if (task.getEndingSurplusQty() != null) {
                score += Math.max(0, 500 - task.getEndingSurplusQty() * 10);
            }
        }

        // 3. 试制任务（从硫化排程结果的IS_TRIAL字段判断）
        if (Boolean.TRUE.equals(task.getIsTrialTask())) {
            score += 1500;
        }

        // 4. 续作任务
        if (Boolean.TRUE.equals(task.getIsContinueTask())) {
            score += 800;
        }

        // 5. 首排任务
        if (Boolean.TRUE.equals(task.getIsFirstTask())) {
            score += 500;
        }

        // 6. 库存紧张（断料风险）
        if (task.getStockHours() != null) {
            if (task.getStockHours().compareTo(new BigDecimal("4")) < 0) {
                score += 800;
            } else if (task.getStockHours().compareTo(new BigDecimal("6")) < 0) {
                score += 400;
            }
        } else if (stock != null && stock.getStockHours() != null) {
            if (stock.getStockHours().compareTo(new BigDecimal("4")) < 0) {
                score += 800;
            } else if (stock.getStockHours().compareTo(new BigDecimal("6")) < 0) {
                score += 400;
            }
        }

        // 7. 关键产品（从关键产品配置表判断）
        Set<String> keyProductCodes = context.getKeyProductCodes();
        if (keyProductCodes != null && keyProductCodes.contains(task.getMaterialCode())) {
            score += 200;
        }

        // 8. 结构优先级
        if (material != null && context.getStructurePriorities() != null) {
            for (CxStructurePriority priority : context.getStructurePriorities()) {
                if (priority.getStructureName().equals(material.getStructureName())) {
                    score += priority.getPriorityLevel() * 10;
                    break;
                }
            }
        }

        // 9. 需要月计划调整的任务，额外加分以确保优先排产
        if (Boolean.TRUE.equals(task.getNeedMonthPlanAdjust())) {
            score += 300;
        }

        return score;
    }

    @Override
    public int roundToTrip(int quantity, String mode) {
        // 默认使用12条/车
        return roundToTrip(quantity, mode, DEFAULT_TRIP_CAPACITY);
    }

    /**
     * 整车取整（支持不同整车容量）
     *
     * @param quantity 原始数量
     * @param mode 取整模式（CEILING向上/FLOOR向下/ROUND四舍五入）
     * @param tripCapacity 整车容量（每车多少条）
     * @return 取整后的数量
     */
    public int roundToTrip(int quantity, String mode, int tripCapacity) {
        if (quantity <= 0) {
            return 0;
        }

        int trips;
        switch (mode) {
            case "CEILING":
                trips = (int) Math.ceil((double) quantity / tripCapacity);
                break;
            case "FLOOR":
                trips = (int) Math.floor((double) quantity / tripCapacity);
                break;
            case "ROUND":
            default:
                trips = (int) Math.round((double) quantity / tripCapacity);
                break;
        }

        return trips * tripCapacity;
    }

    /**
     * 获取结构的整车容量
     * 从结构班产配置表获取，如果没有配置则返回默认值12
     *
     * @param structureCode 结构编码
     * @param machineCode 机台编码
     * @param context 排程上下文
     * @return 整车容量
     */
    private int getTripCapacity(String structureCode, String machineCode, ScheduleContextDTO context) {
        if (context.getStructureShiftCapacities() != null) {
            for (CxStructureShiftCapacity capacity : context.getStructureShiftCapacities()) {
                if (capacity.getStructureCode() != null &&
                        capacity.getStructureCode().equals(structureCode)) {
                    // 如果指定了机台，需要匹配机台
                    if (capacity.getCxMachineCode() == null ||
                            capacity.getCxMachineCode().isEmpty() ||
                            capacity.getCxMachineCode().equals(machineCode)) {
                        if (capacity.getTripQty() != null && capacity.getTripQty() > 0) {
                            return capacity.getTripQty();
                        }
                    }
                }
            }
        }
        // 没有配置则返回默认值
        return context.getDefaultTripCapacity() != null
                ? context.getDefaultTripCapacity()
                : DEFAULT_TRIP_CAPACITY;
    }

    /**
     * 获取结构的整车容量（不指定机台）
     */
    private int getTripCapacity(String structureCode, ScheduleContextDTO context) {
        return getTripCapacity(structureCode, null, context);
    }

    /**
     * 从班产配置Map获取整车容量
     * 取第一个有效配置的整车容量
     *
     * @param structureCode 结构编码
     * @param shiftCapacityMap 班次班产配置Map
     * @return 整车容量
     */
    private int getTripCapacity(String structureCode, Map<String, CxStructureShiftCapacity> shiftCapacityMap) {
        if (shiftCapacityMap != null) {
            for (CxStructureShiftCapacity capacity : shiftCapacityMap.values()) {
                if (capacity.getTripQty() != null && capacity.getTripQty() > 0) {
                    return capacity.getTripQty();
                }
            }
        }
        return DEFAULT_TRIP_CAPACITY;
    }

    /**
     * 按整车容量取整（别名方法）
     */
    private int roundToTripQty(int quantity, int tripCapacity, String mode) {
        return roundToTrip(quantity, mode, tripCapacity);
    }

    /**
     * 计算机台在指定日期的停机扣减产能
     *
     * @param machineCode  机台编码
     * @param currentDate  当前排程日期
     * @param context      排程上下文
     * @return 停机扣减产能（条）
     */
    private int calculateShutdownDeduction(String machineCode, LocalDate currentDate, ScheduleContextDTO context) {
        if (context.getDevicePlanShuts() == null || context.getDevicePlanShuts().isEmpty()) {
            return 0;
        }

        int totalDeduction = 0;

        for (MdmDevicePlanShut shutdown : context.getDevicePlanShuts()) {
            // 检查是否匹配该机台
            if (!machineCode.equals(shutdown.getMachineCode())) {
                continue;
            }

            // 检查停机时间是否覆盖当前日期
            if (shutdown.getBeginDate() == null || shutdown.getEndDate() == null) {
                continue;
            }

            // 将Date转换为LocalDate进行比较
            LocalDate beginDate = shutdown.getBeginDate().toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
            LocalDate endDate = shutdown.getEndDate().toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();

            // 检查当前日期是否在停机时间范围内
            if (!currentDate.isBefore(beginDate) && currentDate.isBefore(endDate)) {
                // 计算停机时长（小时）
                long hours = java.time.Duration.between(
                        shutdown.getBeginDate().toInstant().atZone(java.time.ZoneId.systemDefault()),
                        shutdown.getEndDate().toInstant().atZone(java.time.ZoneId.systemDefault())
                ).toHours();

                // 获取机台小时产能
                int hourlyCapacity = getMachineHourlyCapacity(machineCode, null, context);

                // 计算扣减产能
                int deduction = (int) hours * hourlyCapacity;
                totalDeduction += deduction;

                log.debug("机台 {} 在 {} 有停机计划 {} 小时，扣减产能 {} 条",
                        machineCode, currentDate, hours, deduction);
            }
        }

        return totalDeduction;
    }

    // ==================== 私有方法 ====================

    /**
     * 判断是否为停产日
     *
     * <p>使用 context 中预计算的工作日历信息判断
     *
     * @param context 排程上下文
     * @param date    待判断日期
     * @return true-停产日，false-生产日
     */
    private boolean isStopProductionDay(ScheduleContextDTO context, LocalDate date) {
        // 优先使用 context 中的工作日历判断
        MdmWorkCalendar workCalendar = context.getWorkCalendar();
        if (workCalendar != null) {
            // 检查是否在停产日期范围内
            Date startDate = workCalendar.getStopStartDate();
            Date endDate = workCalendar.getStopEndDate();
            if (startDate != null && endDate != null) {
                LocalDate stopStart = startDate.toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
                LocalDate stopEnd = endDate.toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
                if (!date.isBefore(stopStart) && !date.isAfter(stopEnd)) {
                    return true;
                }
            }
        }

        // 检查当前排程日是否标记为停产日
        if (context.getCurrentScheduleDate() != null 
                && date.equals(context.getCurrentScheduleDate())
                && Boolean.TRUE.equals(context.getIsClosingDay())) {
            return true;
        }

        return false;
    }

    /**
     * 初始化机台状态
     *
     * 处理逻辑：
     * 1. 跳过禁用、维护中、故障的机台
     * 2. 处理精度计划：
     *    - 精度期间的机台在对应班次不可用（扣减该班次全部产能）
     *    - 根据胎胚库存判断是否影响硫化
     * 3. 从机台当前状态表获取当前在产结构
     * 4. 从机台结构产能表获取小时产能
     * 5. 根据设备计划停机表扣减对应日期的产能
     */
    private Map<String, MachineAllocationResult> initMachineStatus(ScheduleContextDTO context, LocalDate currentDate) {
        Map<String, MachineAllocationResult> map = new LinkedHashMap<>();

        // 构建精度计划映射（机台编码 -> 精度计划）
        Map<String, CxPrecisionPlan> precisionPlanMap = new HashMap<>();
        if (context.getPrecisionPlans() != null) {
            for (CxPrecisionPlan plan : context.getPrecisionPlans()) {
                precisionPlanMap.put(plan.getMachineCode(), plan);
            }
        }

        // 构建机台在线信息映射
        Map<String, MdmCxMachineOnlineInfo> machineOnlineInfoMap = new HashMap<>();
        if (context.getOnlineInfos() != null) {
            for (MdmCxMachineOnlineInfo info : context.getOnlineInfos()) {
                if (info.getCxCode() != null) {
                    machineOnlineInfoMap.put(info.getCxCode(), info);
                }
            }
        }

        for (MdmMoldingMachine machine : context.getAvailableMachines()) {
            if (machine.getIsActive() == null || machine.getIsActive() != 1) {
                log.debug("机台 {} 未启用，跳过", machine.getCxMachineCode());
                continue;
            }

            MachineAllocationResult result = new MachineAllocationResult();
            result.setMachineCode(machine.getCxMachineCode());
            result.setMachineType(machine.getCxMachineTypeCode());
            result.setDailyCapacity(machine.getMaxDayCapacity() != null
                    ? machine.getMaxDayCapacity() : 1200);
            result.setUsedCapacity(0);
            result.setRemainingCapacity(result.getDailyCapacity());
            result.setAssignedTypes(0);
            result.setTaskAllocations(new ArrayList<>());

            // 从机台在线信息表获取当前在产胎胚描述（用于续作判断）
            MdmCxMachineOnlineInfo onlineInfo = machineOnlineInfoMap.get(machine.getCxMachineCode());
            if (onlineInfo != null && onlineInfo.getEmbryoSpec() != null) {
                result.setCurrentStructure(onlineInfo.getEmbryoSpec());
            }

            // 检查精度计划
            CxPrecisionPlan precisionPlan = precisionPlanMap.get(machine.getCxMachineCode());
            if (precisionPlan != null &&
                    ("PLANNED".equals(precisionPlan.getStatus()) ||
                            "IN_PROGRESS".equals(precisionPlan.getStatus()))) {

                // 精度时长（小时）
                int precisionHours = precisionPlan.getEstimatedHours() != null
                        ? precisionPlan.getEstimatedHours() : 4;

                // 机台小时产能（条/小时）- 从机台结构产能表获取，或使用默认值
                int hourlyCapacity = getMachineHourlyCapacity(
                        machine.getCxMachineCode(),
                        precisionPlan.getEmbryoCode(),
                        context);

                // 扣减产能 = 精度时长 × 小时产能
                int precisionDeduction = precisionHours * hourlyCapacity;

                // 根据班次扣减
                String planShift = precisionPlan.getPlanShift();
                if (SHIFT_DAY.equals(planShift)) {
                    // 早班精度，扣减早班产能
                    result.setRemainingCapacity(result.getRemainingCapacity() - precisionDeduction);
                    log.info("机台 {} 在早班有精度计划，扣减产能 {} 条",
                            machine.getCxMachineCode(), precisionDeduction);
                } else if (SHIFT_AFTERNOON.equals(planShift)) {
                    // 中班精度，扣减中班产能
                    result.setRemainingCapacity(result.getRemainingCapacity() - precisionDeduction);
                    log.info("机台 {} 在中班有精度计划，扣减产能 {} 条",
                            machine.getCxMachineCode(), precisionDeduction);
                } else {
                    // 未指定班次，默认扣减全天产能的1/3
                    int deduction = result.getDailyCapacity() / 3;
                    result.setRemainingCapacity(result.getRemainingCapacity() - deduction);
                    log.info("机台 {} 有精度计划（未指定班次），扣减产能 {} 条",
                            machine.getCxMachineCode(), deduction);
                }

                // 标记精度计划信息
                result.setPrecisionPlan(precisionPlan);
            }

            // 检查设备计划停机，根据停机时长扣减产能
            int shutdownDeduction = calculateShutdownDeduction(machine.getCxMachineCode(), currentDate, context);
            if (shutdownDeduction > 0) {
                result.setRemainingCapacity(result.getRemainingCapacity() - shutdownDeduction);
                log.info("机台 {} 在 {} 有停机计划，扣减产能 {} 条，剩余产能 {} 条",
                        machine.getCxMachineCode(), currentDate, shutdownDeduction, result.getRemainingCapacity());
            }

            // 如果剩余产能为负，设为0
            if (result.getRemainingCapacity() < 0) {
                log.warn("机台 {} 因停机/精度计划导致剩余产能为负，设为0", machine.getCxMachineCode());
                result.setRemainingCapacity(0);
            }

            map.put(machine.getCxMachineCode(), result);
        }

        log.info("初始化机台状态完成，可用机台 {} 台，当前日期: {}", map.size(), currentDate);
        return map;
    }

    /**
     * 查找最佳机台
     */
    private MdmMoldingMachine findBestMachine(
            DailyEmbryoTask task,
            Map<String, MachineAllocationResult> machineStatusMap,
            Map<String, Set<String>> machineMaterialMap,
            ScheduleContextDTO context) {

        MdmMoldingMachine bestMachine = null;
        int bestScore = -1;

        for (MdmMoldingMachine machine : context.getAvailableMachines()) {
            MachineAllocationResult status = machineStatusMap.get(machine.getCxMachineCode());
            if (status == null || status.getRemainingCapacity() <= 0) {
                continue;
            }

            // 检查结构约束
            if (!checkStructureConstraint(machine,
                    context.getMaterials().stream()
                            .filter(m -> m.getMaterialCode().equals(task.getMaterialCode()))
                            .findFirst()
                            .orElse(null),
                    context)) {
                continue;
            }

            // 检查种类上限
            Set<String> materials = machineMaterialMap.get(machine.getCxMachineCode());
            int currentTypes = materials != null ? materials.size() : 0;
            if (!checkTypeLimit(machine, currentTypes,
                    context.getMaterials().stream()
                            .filter(m -> m.getMaterialCode().equals(task.getMaterialCode()))
                            .findFirst()
                            .orElse(null),
                    context)) {
                continue;
            }

            // 计算机台得分
            int score = calculateMachineScore(machine, task, status, context);
            if (score > bestScore) {
                bestScore = score;
                bestMachine = machine;
            }
        }

        return bestMachine;
    }

    /**
     * 计算机台得分
     * 续作机台获得最高加分（+1000分）
     */
    private int calculateMachineScore(
            MdmMoldingMachine machine,
            DailyEmbryoTask task,
            MachineAllocationResult status,
            ScheduleContextDTO context) {

        int score = 0;

        // 【最高优先】续作任务：该机台正在做这个胎胚
        if (Boolean.TRUE.equals(task.getIsContinueTask()) &&
                task.getContinueMachineCodes() != null &&
                task.getContinueMachineCodes().contains(machine.getCxMachineCode())) {
            score += 1000; // 续作最高加分，无需换产
            log.debug("机台 {} 是胎胚 {} 的续作机台，加分1000",
                    machine.getCxMachineCode(), task.getMaterialCode());
        }

        // 昨日做过该胎胚（但有换产）
        if (context.getYesterdayResults() != null) {
            for (CxScheduleResult yesterday : context.getYesterdayResults()) {
                if (yesterday.getCxMachineCode().equals(machine.getCxMachineCode()) &&
                        yesterday.getEmbryoCode() != null &&
                        yesterday.getEmbryoCode().equals(task.getMaterialCode())) {
                    score += 500;
                    break;
                }
            }
        }

        // 优先选剩余产能最多的（均衡）
        score += status.getRemainingCapacity() / 10;

        // 优先选种类最少的（均衡）
        int maxTypes = context.getMaxTypesPerMachine() != null
                ? context.getMaxTypesPerMachine()
                : DEFAULT_MAX_TYPES_PER_MACHINE;
        score += (maxTypes - status.getAssignedTypes()) * 50;

        // 优先选固定生产该结构的机台
        if (context.getMachineFixedConfigs() != null) {
            for (MdmCxMachineFixed fixed : context.getMachineFixedConfigs()) {
                if (fixed.getCxMachineCode().equals(machine.getCxMachineCode())) {
                    if (fixed.getFixedStructure1() != null &&
                            fixed.getFixedStructure1().contains(task.getStructureName())) {
                        score += 200;
                    }
                    if (fixed.getFixedMaterialCode() != null &&
                            fixed.getFixedMaterialCode().contains(task.getMaterialCode())) {
                        score += 300;
                    }
                }
            }
        }

        return score;
    }

    /**
     * 获取机台小时产能
     * 从机台结构产能表查询，如果找不到则返回默认值50条/小时
     *
     * @param machineCode 机台编码
     * @param structureCode 结构编码（可选）
     * @param context 排程上下文
     * @return 小时产能（条/小时）
     */
    private int getMachineHourlyCapacity(String machineCode, String structureCode, ScheduleContextDTO context) {
        // 构建机台结构产能映射（懒加载）
        Map<String, CxMachineStructureCapacity> capacityMap = context.getMachineCapacityMap();
        if (capacityMap == null && context.getMachineStructureCapacities() != null) {
            capacityMap = new HashMap<>();
            for (CxMachineStructureCapacity capacity : context.getMachineStructureCapacities()) {
                // Key: 机台编码_结构编码
                String key = capacity.getCxMachineCode() + "_" + capacity.getStructureCode();
                capacityMap.put(key, capacity);
                // 同时添加只有机台编码的key作为默认
                if (!capacityMap.containsKey(capacity.getCxMachineCode())) {
                    capacityMap.put(capacity.getCxMachineCode(), capacity);
                }
            }
            context.setMachineCapacityMap(capacityMap);
        }

        if (capacityMap != null) {
            // 先尝试精确匹配（机台+结构）
            if (structureCode != null) {
                String key = machineCode + "_" + structureCode;
                CxMachineStructureCapacity capacity = capacityMap.get(key);
                if (capacity != null && capacity.getHourlyCapacity() != null) {
                    return capacity.getHourlyCapacity();
                }
            }
            // 再尝试只匹配机台
            CxMachineStructureCapacity capacity = capacityMap.get(machineCode);
            if (capacity != null && capacity.getHourlyCapacity() != null) {
                return capacity.getHourlyCapacity();
            }
        }

        // 默认机台小时产能
        return DEFAULT_HOURLY_CAPACITY;
    }

    /**
     * 计算波浪分配（无班产配置时使用默认整车容量）
     *
     * @param totalQty 总数量
     * @param shiftCodes 班次编码数组
     * @param context 排程上下文
     */
    private int[] calculateWaveAllocation(int totalQty, String[] shiftCodes, ScheduleContextDTO context) {
        int shiftCount = shiftCodes.length;
        int[] result = new int[shiftCount];

        // 获取波浪比例
        int[] waveRatio = context.getWaveRatio();
        if (waveRatio == null) {
            waveRatio = DEFAULT_WAVE_RATIO;
        }

        // 获取默认整车容量
        int tripCapacity = context.getDefaultTripCapacity() != null
                ? context.getDefaultTripCapacity()
                : DEFAULT_TRIP_CAPACITY;

        // 根据班次编码调整波浪比例（映射夜早中比例到正确的班次位置）
        int[] adjustedRatio = adjustWaveRatio(waveRatio, shiftCodes);

        // 计算总比例
        int totalRatio = 0;
        for (int ratio : adjustedRatio) {
            totalRatio += ratio;
        }

        int remaining = totalQty;

        for (int i = 0; i < shiftCount; i++) {
            int qty = totalQty * adjustedRatio[i] / totalRatio;
            // 整车取整
            qty = roundToTripQty(qty, tripCapacity, "ROUND");
            result[i] = qty;
            remaining -= qty;
        }

        // 分配余量
        for (int i = 0; i < remaining / tripCapacity && i < shiftCount; i++) {
            result[i % shiftCount] += tripCapacity;
            remaining -= tripCapacity;
        }

        return result;
    }

    /**
     * 调整波浪比例以匹配班次序列
     *
     * 波浪比例配置顺序：夜班、早班、中班（如 {1, 2, 1} 表示夜:早:中 = 1:2:1）
     * 班次序列顺序：早中、夜早中、夜早中...（第一天夜班跳过）
     *
     * 需要根据班次编码将波浪比例映射到正确的位置
     *
     * @param baseRatio 基础波浪比例 [夜, 早, 中]
     * @param shiftCodes 班次编码数组
     * @return 调整后的波浪比例
     */
    private int[] adjustWaveRatio(int[] baseRatio, String[] shiftCodes) {
        int shiftCount = shiftCodes.length;
        int[] result = new int[shiftCount];

        // 波浪比例索引映射
        // SHIFT_NIGHT -> 0, SHIFT_DAY -> 1, SHIFT_AFTERNOON -> 2
        for (int i = 0; i < shiftCount; i++) {
            String shiftCode = shiftCodes[i];
            int ratioIndex;

            if (shiftCode.contains("NIGHT")) {
                ratioIndex = 0;  // 夜班对应第一个比例
            } else if (shiftCode.contains("DAY")) {
                ratioIndex = 1;  // 早班对应第二个比例
            } else if (shiftCode.contains("AFTERNOON")) {
                ratioIndex = 2;  // 中班对应第三个比例
            } else {
                ratioIndex = i % baseRatio.length;  // 兜底
            }

            result[i] = baseRatio[ratioIndex];
        }

        return result;
    }

    /**
     * 创建日胎胚任务
     */
    private DailyEmbryoTask createDailyEmbryoTask(
            String materialCode,
            Integer demandQuantity,
            Map<String, MdmMaterialInfo> materialMap,
            Map<String, CxStock> stockMap,
            Map<String, CxStructureEnding> endingMap,
            ScheduleContextDTO context) {

        MdmMaterialInfo material = materialMap.get(materialCode);
        if (material == null) {
            return null;
        }

        DailyEmbryoTask task = new DailyEmbryoTask();
        task.setMaterialCode(materialCode);
        task.setMaterialName(material.getMaterialDesc());
        task.setStructureName(material.getStructureName());
        task.setDemandQuantity(demandQuantity);
        task.setAssignedQuantity(0);
        task.setRemainingQuantity(demandQuantity);
        task.setIsTrialTask(false);

        // 计算库存时长
        CxStock stock = stockMap.get(materialCode);
        if (stock != null) {
            task.setStockHours(stock.getStockHours());
            task.setVulcanizeMachineCount(stock.getVulcanizeMachineCount());
            task.setVulcanizeMoldCount(stock.getVulcanizeMoldCount());
        }

        // 计算收尾余量
        Integer vulcanizeSurplusQty = null;
        Integer endingSurplusQty = null;
        boolean isEndingTask = false;

        if (context.getMonthSurplusMap() != null) {
            com.zlt.aps.mp.api.domain.entity.MdmMonthSurplus monthSurplus =
                    context.getMonthSurplusMap().get(materialCode);
            if (monthSurplus != null && monthSurplus.getPlanSurplusQty() != null) {
                vulcanizeSurplusQty = monthSurplus.getPlanSurplusQty().intValue();
                int stockQty = stock != null ? stock.getEffectiveStock() : 0;
                endingSurplusQty = vulcanizeSurplusQty - stockQty;
                isEndingTask = endingSurplusQty <= 0;
            }
        }

        task.setIsEndingTask(isEndingTask);
        task.setEndingSurplusQty(endingSurplusQty);
        task.setVulcanizeSurplusQty(vulcanizeSurplusQty);

        // 是否主销产品（从SKU排产分类表判断）
        boolean isMainProduct = context.getMainProductCodes() != null
                && context.getMainProductCodes().contains(materialCode);
        task.setIsMainProduct(isMainProduct);

        // ========== 收尾管理：应用追赶量和优先级 ==========
        String structureName = material.getStructureName();
        CxStructureEnding structureEnding = endingMap.get(structureName);

        if (structureEnding != null) {
            // 设置紧急收尾标记
            boolean isUrgentEnding = structureEnding.getIsUrgentEnding() != null
                    && structureEnding.getIsUrgentEnding() == 1;
            task.setIsUrgentEnding(isUrgentEnding);

            // 如果需要追赶，增加需求量（平摊量）
            if (structureEnding.getDistributedQuantity() != null
                    && structureEnding.getDistributedQuantity() > 0) {
                int originalDemand = task.getDemandQuantity();
                int catchUpQty = structureEnding.getDistributedQuantity();
                task.setDemandQuantity(originalDemand + catchUpQty);
                task.setRemainingQuantity(originalDemand + catchUpQty);
                log.info("收尾追赶：物料 {} 原需求 {}，增加追赶量 {}，新需求 {}",
                        materialCode, originalDemand, catchUpQty, task.getDemandQuantity());
            }

            // 标记是否需要月计划调整
            task.setNeedMonthPlanAdjust(structureEnding.getNeedMonthPlanAdjust() != null
                    && structureEnding.getNeedMonthPlanAdjust() == 1);
        }

        // 计算优先级（紧急收尾会获得更高优先级）
        task.setPriority(calculatePriorityScoreNew(task, material, stock, context));

        return task;
    }

    /**
     * 获取硫化消耗量
     */
    private BigDecimal getVulcanizeDemand(MdmMaterialInfo material, ScheduleContextDTO context) {
        // 从结构硫化配比中获取硫化需求
        for (MdmStructureLhRatio ratio : context.getStructureLhRatios()) {
            if (ratio.getStructureName().equals(material.getStructureName())) {
                // 简化处理：假设每日硫化需求为最大胎胚数
                return ratio.getMaxEmbryoQty() != null
                        ? new BigDecimal(ratio.getMaxEmbryoQty())
                        : BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * 构建排程结果
     */
    private List<CxScheduleResult> buildScheduleResults(
            ScheduleContextDTO context,
            List<MachineAllocationResult> allocations,
            List<ShiftAllocationResult> shiftAllocations,
            List<CxScheduleDetail> details) {

        // 使用当前天的班次配置
        List<com.zlt.aps.cx.entity.config.CxShiftConfig> dayShifts = context.getCurrentShiftConfigs();
        if (!CollectionUtils.isEmpty(dayShifts)) {
            return buildScheduleResults(context, allocations, shiftAllocations, details, dayShifts);
        }

        // 兼容旧逻辑：默认班次配置
        List<CxScheduleResult> results = new ArrayList<>();
        LocalDate scheduleDate = context.getScheduleDate();

        // 按机台构建排程结果
        Map<String, ShiftAllocationResult> shiftMap = shiftAllocations.stream()
                .collect(Collectors.toMap(ShiftAllocationResult::getMachineCode, s -> s));

        for (MachineAllocationResult allocation : allocations) {
            CxScheduleResult result = new CxScheduleResult();
            result.setScheduleDate(scheduleDate.atStartOfDay());
            result.setCxMachineCode(allocation.getMachineCode());
            result.setCxMachineType(allocation.getMachineType());
            result.setProductNum(new BigDecimal(allocation.getUsedCapacity()));
            result.setProductionStatus("0");
            result.setIsRelease("0");
            result.setDataSource("0");
            result.setCreateTime(new Date());

            // 设置班次计划量（旧逻辑：固定映射）
            ShiftAllocationResult shiftResult = shiftMap.get(allocation.getMachineCode());
            if (shiftResult != null) {
                Map<String, Integer> shiftPlanQty = shiftResult.getShiftPlanQty();
                result.setClass1PlanQty(new BigDecimal(shiftPlanQty.getOrDefault(SHIFT_NIGHT, 0)));
                result.setClass2PlanQty(new BigDecimal(shiftPlanQty.getOrDefault(SHIFT_DAY, 0)));
                result.setClass3PlanQty(new BigDecimal(shiftPlanQty.getOrDefault(SHIFT_AFTERNOON, 0)));
            }

            // 设置第一个任务的胎胚信息
            if (!allocation.getTaskAllocations().isEmpty()) {
                TaskAllocation firstTask = allocation.getTaskAllocations().get(0);
                result.setEmbryoCode(firstTask.getMaterialCode());
                result.setStructureName(firstTask.getStructureName());
            }

            results.add(result);
        }

        return results;
    }

    /**
     * 构建排程结果（接收动态班次配置）
     * 按 CLASS_FIELD 映射到结果表的对应字段
     *
     * @param context        排程上下文
     * @param allocations    机台分配结果
     * @param shiftAllocations 班次分配结果
     * @param details        排程明细
     * @param dayShifts      该天的班次配置列表
     * @return 排程结果列表
     */
    private List<CxScheduleResult> buildScheduleResults(
            ScheduleContextDTO context,
            List<MachineAllocationResult> allocations,
            List<ShiftAllocationResult> shiftAllocations,
            List<CxScheduleDetail> details,
            List<com.zlt.aps.cx.entity.config.CxShiftConfig> dayShifts) {

        List<CxScheduleResult> results = new ArrayList<>();
        LocalDate scheduleDate = context.getCurrentScheduleDate() != null 
                ? context.getCurrentScheduleDate() 
                : context.getScheduleDate();

        // 按机台构建排程结果
        Map<String, ShiftAllocationResult> shiftMap = shiftAllocations.stream()
                .collect(Collectors.toMap(ShiftAllocationResult::getMachineCode, s -> s));

        for (MachineAllocationResult allocation : allocations) {
            CxScheduleResult result = new CxScheduleResult();
            result.setScheduleDate(scheduleDate.atStartOfDay());
            result.setCxMachineCode(allocation.getMachineCode());
            result.setCxMachineType(allocation.getMachineType());
            result.setProductNum(new BigDecimal(allocation.getUsedCapacity()));
            result.setProductionStatus("0");
            result.setIsRelease("0");
            result.setDataSource("0");
            result.setCreateTime(new Date());

            // 按 CLASS_FIELD 映射班次计划量
            ShiftAllocationResult shiftResult = shiftMap.get(allocation.getMachineCode());
            if (shiftResult != null) {
                Map<String, Integer> shiftPlanQty = shiftResult.getShiftPlanQty();
                
                // 遍历该天的班次配置，按 CLASS_FIELD 设置对应字段
                for (com.zlt.aps.cx.entity.config.CxShiftConfig shiftConfig : dayShifts) {
                    String classField = shiftConfig.getClassField();
                    String shiftCode = shiftConfig.getShiftCode();
                    Integer shiftQty = shiftPlanQty.getOrDefault(shiftCode, 0);
                    
                    setClassFieldValue(result, classField, shiftQty);
                }
            }

            // 设置第一个任务的胎胚信息
            if (!allocation.getTaskAllocations().isEmpty()) {
                TaskAllocation firstTask = allocation.getTaskAllocations().get(0);
                result.setEmbryoCode(firstTask.getMaterialCode());
                result.setStructureName(firstTask.getStructureName());
            }

            results.add(result);
        }

        return results;
    }

    /**
     * 按 CLASS_FIELD 设置对应的班次计划量
     *
     * @param result     排程结果
     * @param classField 字段名：CLASS1~CLASS8
     * @param qty        计划量
     */
    private void setClassFieldValue(CxScheduleResult result, String classField, Integer qty) {
        if (classField == null || qty == null) {
            return;
        }
        BigDecimal qtyDecimal = new BigDecimal(qty);
        switch (classField) {
            case "CLASS1":
                result.setClass1PlanQty(qtyDecimal);
                break;
            case "CLASS2":
                result.setClass2PlanQty(qtyDecimal);
                break;
            case "CLASS3":
                result.setClass3PlanQty(qtyDecimal);
                break;
            case "CLASS4":
                result.setClass4PlanQty(qtyDecimal);
                break;
            case "CLASS5":
                result.setClass5PlanQty(qtyDecimal);
                break;
            case "CLASS6":
                result.setClass6PlanQty(qtyDecimal);
                break;
            case "CLASS7":
                result.setClass7PlanQty(qtyDecimal);
                break;
            case "CLASS8":
                result.setClass8PlanQty(qtyDecimal);
                break;
            default:
                log.warn("未知的 CLASS_FIELD: {}", classField);
        }
    }
}
