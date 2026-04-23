package com.zlt.aps.cx.service.impl;

import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.mapper.MdmSkuConstructionRefMapper;
import com.zlt.aps.cx.service.engine.*;
import com.zlt.aps.cx.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.mp.api.domain.entity.MdmSkuConstructionRef;
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
    private final MdmSkuConstructionRefMapper skuConstructionRefMapper;

    /** 构造函数注入 */
    @Autowired
    public CoreScheduleAlgorithmServiceImpl(
            @Lazy ContinueTaskProcessor continueTaskProcessor,
            @Lazy TrialTaskProcessor trialTaskProcessor,
            @Lazy NewTaskProcessor newTaskProcessor,
            @Lazy ShiftScheduleService shiftScheduleService,
            @Lazy ProductionCalculator productionCalculator,
            ScheduleDayTypeHelper scheduleDayTypeHelper,
            @Lazy BalancingService balancingService,
            MdmSkuConstructionRefMapper skuConstructionRefMapper) {
        this.continueTaskProcessor = continueTaskProcessor;
        this.trialTaskProcessor = trialTaskProcessor;
        this.newTaskProcessor = newTaskProcessor;
        this.shiftScheduleService = shiftScheduleService;
        this.productionCalculator = productionCalculator;
        this.scheduleDayTypeHelper = scheduleDayTypeHelper;
        this.balancingService = balancingService;
        this.skuConstructionRefMapper = skuConstructionRefMapper;
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

        // 按排程天数和班次序号排序，确保按 班次1→班次2→...→班次8 顺序处理
        List<CxShiftConfig> sortedShiftConfigs = allShiftConfigs.stream()
                .filter(c -> c.getScheduleDay() != null)
                .sorted(Comparator.comparingInt(CxShiftConfig::getScheduleDay)
                        .thenComparingInt(c -> c.getDayShiftOrder() != null ? c.getDayShiftOrder() : 0))
                .collect(Collectors.toList());

        // 收集每个班次的排产结果
        List<ShiftScheduleResult> shiftResults = new ArrayList<>();

        // 记录机台在产状态（跨班次持续更新）
        Map<String, Set<String>> machineOnlineEmbryoMap = context.getMachineOnlineEmbryoMap();
        if (machineOnlineEmbryoMap == null) {
            machineOnlineEmbryoMap = new HashMap<>();
        }

        // 已处理的天的集合（用于判断是否需要做停产日检查）
        Set<Integer> processedDays = new HashSet<>();
        // 记录上一个班次的天数，用于判断是否跨天
        int lastDay = 0;

        // 按班次逐个执行排程
        int shiftIndex = 0;
        int totalShifts = sortedShiftConfigs.size();
        for (CxShiftConfig shiftConfig : sortedShiftConfigs) {
            int day = shiftConfig.getScheduleDay();
            LocalDate currentScheduleDate = context.getScheduleDate()
                    .minusDays(SCHEDULE_START_OFFSET_DAYS).plusDays(day - 1);

            // 检查当前天是否是整天停产
            if (scheduleDayTypeHelper.isFullDayStopped(currentScheduleDate)) {
                log.info("第 {} 天日期 {} 整天停产，跳过班次 {} 的排程", day, currentScheduleDate, shiftConfig.getShiftCode());
                continue;
            }

            // 检查当前班次是否停产
            Integer dayShiftOrder = shiftConfig.getDayShiftOrder();
            if (dayShiftOrder != null && scheduleDayTypeHelper.isShiftStopped(currentScheduleDate, dayShiftOrder)) {
                log.info("第 {} 天日期 {} 班次 {} 停产，跳过该班次排程", day, currentScheduleDate, shiftConfig.getShiftCode());
                continue;
            }

            shiftIndex++;
            // 获取历史胎胚数量用于日志
            int historyCount = machineOnlineEmbryoMap != null ? machineOnlineEmbryoMap.values().stream().mapToInt(Collection::size).sum() : 0;
            log.info("【班次开始】#{}/{} | 日期:{} | 班次:{} | 历史胎胚数量:{}",
                    shiftIndex, totalShifts, currentScheduleDate, shiftConfig.getShiftCode(), historyCount);

            // 设置当前班次的上下文
            List<CxShiftConfig> singleShiftList = Collections.singletonList(shiftConfig);
            context.setCurrentScheduleDay(day);
            context.setCurrentScheduleDate(currentScheduleDate);
            context.setCurrentShiftConfigs(singleShiftList);


            // 执行该班次的排程
            ShiftScheduleResult shiftResult = executeShiftSchedule(
                    context, day, shiftConfig, currentScheduleDate, machineOnlineEmbryoMap);
            shiftResults.add(shiftResult);

            // 更新机台在产状态
            machineOnlineEmbryoMap = updateMachineOnlineStatus(
                    shiftResult.getAllAllocations(), machineOnlineEmbryoMap);

            // 将更新后的机台在产状态存回 context，供下一个班次使用
            context.setMachineOnlineEmbryoMap(new HashMap<>(machineOnlineEmbryoMap));

            // 更新库存和硫化余量，供下一个班次排程使用
            updateContextForNextShift(context, shiftResult.getAllAllocations(), singleShiftList, shiftConfig, shiftResult.getShiftProductionResults());

            lastDay = day;
            processedDays.add(day);
        }

        // ==================== 合并多班次结果：每个机台一条记录，8个班次映射到CLASS1~8 ====================
        List<CxScheduleResult> allResults = buildFinalScheduleResultsFromShifts(context, shiftResults, allShiftConfigs);

        // ==================== 构建子表：按"胎胚+整车"维度拆分车次，计算库存可供硫化时长和顺序 ====================
        List<CxScheduleDetail> allDetails = buildScheduleDetailsFromShifts(context, shiftResults, allShiftConfigs);
        log.info("子表记录构建完成，共 {} 条", allDetails.size());

        // ==================== 将子表明细关联到主表 ====================
        associateDetailsToResults(allResults, allDetails);

        log.info("排程算法执行完成，共 {} 个班次，总机台数: {}", shiftIndex, allResults.size());
        return allResults;
    }

    /**
     * 将子表明细关联到主表结果
     * <p>匹配规则：机台编码 + 胎胚代码 一致
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

    /**
     * 执行单天排程
     *
     * <p>排程流程：
     * <ol>
     *   <li>S5.2 任务分组：续作/试制/新增三类</li>
     *   <li>S5.3 处理续作任务</li>
     *   <li>S5.3 处理试制任务（独立处理，特殊约束）</li>
     *   <li>S5.3 处理新增任务（合并续作+新增，重新均衡）</li>
     *   <li>S5.3.7 班次排产</li>
     * </ol>
     *
     * @return 班次排产结果列表 + 机台分配结果列表
     */
    private ShiftScheduleResult executeShiftSchedule(
            ScheduleContextVo context,
            int day,
            CxShiftConfig shiftConfig,
            LocalDate scheduleDate,
            Map<String, Set<String>> machineOnlineEmbryoMap) {

        List<CxShiftConfig> singleShiftList = Collections.singletonList(shiftConfig);

        log.info("========== 开始执行班次排程，天={}, 日期={}, 班次={} ==========",
                day, scheduleDate, shiftConfig.getShiftCode());

        // ==================== 第一步：S5.2 任务分组（单班次） ====================
        TaskGroupService.TaskGroupResult taskGroup = taskGroupService.groupTasks(
                context, machineOnlineEmbryoMap, scheduleDate, singleShiftList);
        log.info("任务分组完成：续作 {} 个，试制 {} 个，新增 {} 个",
                taskGroup.getContinueTasks().size(),
                taskGroup.getTrialTasks().size(),
                taskGroup.getNewTasks().size());

        // ==================== 第二步：S5.3 处理续作任务 ====================
        List<MachineAllocationResult> continueAllocations = continueTaskProcessor.processContinueTasks(
                taskGroup.getContinueTasks(), context, scheduleDate, singleShiftList, day);
        log.info("续作任务处理完成，机台分配数: {}", continueAllocations.size());

        // ==================== 第三步：S5.3 处理试制任务（独立处理） ====================
        List<MachineAllocationResult> trialAllocations = trialTaskProcessor.processTrialTasks(
                taskGroup.getTrialTasks(), context, scheduleDate, singleShiftList, context.getAvailableMachines());
        log.info("试制任务处理完成，机台分配数: {}", trialAllocations.size());

        // ==================== 第四步：S5.3 处理新增任务（续作剩余需求+新增统一均衡） ====================
        List<MachineAllocationResult> newAllocations = newTaskProcessor.processNewTasks(
                taskGroup.getNewTasks(),
                context,
                scheduleDate,
                singleShiftList,
                taskGroup.getContinueTasks(),
                continueAllocations,
                trialAllocations);
        log.info("新增任务处理完成，机台分配数: {}", newAllocations.size());

        // ==================== 第五步：合并分配结果 ====================
        List<MachineAllocationResult> allAllocations = new ArrayList<>();
        allAllocations.addAll(continueAllocations);
        allAllocations.addAll(newAllocations);
        allAllocations.addAll(trialAllocations);

        log.info("班次分配前检查: 总分配数={}", allAllocations.size());

        // ==================== 第六步：S5.3.7 班次排产（单个班次，无需跨班次均衡） ====================
        List<ShiftScheduleService.ShiftProductionResult> shiftProductionResults = new ArrayList<>();
        LocalDate scheduleDateForShift = scheduleDate;

        for (MachineAllocationResult allocation : allAllocations) {
            String machineCode = allocation.getMachineCode();
            log.info("========== 对{}机台进行班次排量 ==========", machineCode);
            for (TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                CoreScheduleAlgorithmService.DailyEmbryoTask task = new CoreScheduleAlgorithmService.DailyEmbryoTask();
                task.setEmbryoCode(taskAlloc.getEmbryoCode());
                task.setMaterialCode(taskAlloc.getMaterialCode());
                task.setMaterialDesc(taskAlloc.getMaterialDesc());
                task.setMainMaterialDesc(taskAlloc.getMainMaterialDesc());
                task.setStructureName(taskAlloc.getStructureName());
                task.setPlannedProduction(taskAlloc.getQuantity());
                // 优先使用 endingExtraInventory（实际需生产量），如果没有则用 quantity
                task.setEndingExtraInventory(taskAlloc.getEndingExtraInventory() != null 
                        ? taskAlloc.getEndingExtraInventory() : taskAlloc.getQuantity());
                task.setIsTrialTask(taskAlloc.getIsTrialTask());
                task.setIsEndingTask(taskAlloc.getIsEndingTask());
                task.setIsContinueTask(taskAlloc.getIsContinueTask());
                task.setIsLastEndingBatch(taskAlloc.getIsLastEndingBatch());  // 设置是否收尾最后一批
                task.setIsOpeningDayTask(context.getIsOpeningDay());
                task.setStockHours(taskAlloc.getStockHours());
                task.setPriority(taskAlloc.getPriority());
                task.setLhId(taskAlloc.getLhId());

                // 计算需要的车数（使用实际待排产量）
                int tripCapacity = productionCalculator.getTripCapacity(taskAlloc.getStructureName(), context);
                int actualQty = taskAlloc.getEndingExtraInventory() != null ? taskAlloc.getEndingExtraInventory() : taskAlloc.getQuantity();
                int cars = tripCapacity > 0 ? (int) Math.ceil((double) actualQty / tripCapacity) : 0;
                task.setRequiredCars(cars);

                // 打印精排任务日志
                String taskType;
                if (Boolean.TRUE.equals(taskAlloc.getIsContinueTask())) {
                    taskType = "续作任务";
                } else if (Boolean.TRUE.equals(taskAlloc.getIsTrialTask())) {
                    taskType = "试制任务";
                } else {
                    taskType = "新增任务";
                }
                log.info("  【{}】物料编码:{} | 物料描述:{} | 胎胚:{} | 主物料:{} | 规格:{} | 待排产量:{}条 | 需{}车(每车{}条) | 库存可撑:{}h | 硫化机:{}台",
                        taskType,
                        taskAlloc.getMaterialCode(),
                        taskAlloc.getMaterialDesc(),
                        taskAlloc.getEmbryoCode(),
                        taskAlloc.getMainMaterialDesc(),
                        taskAlloc.getStructureName(),
                        actualQty,
                        cars,
                        tripCapacity,
                        String.format("%.1f", taskAlloc.getStockHours()),
                        task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0);

                List<ShiftScheduleService.ShiftProductionResult> taskShiftResults =
                        shiftScheduleService.scheduleTaskToShifts(task, machineCode, context, singleShiftList, scheduleDateForShift);
                shiftProductionResults.addAll(taskShiftResults);
            }
        }
        log.info("【班次完成】共分配 {} 条排产记录", shiftProductionResults.size());

        // 注意：按班次排程时不需要跨班次均衡（balanceShiftQuantities），
        // 因为每个班次独立 DFS 均衡，量已经按单班次需求分配

        // 封装该班次排产结果
        ShiftScheduleResult shiftResult = new ShiftScheduleResult();
        shiftResult.setDay(day);
        shiftResult.setScheduleDate(scheduleDate);
        shiftResult.setShiftConfig(shiftConfig);
        shiftResult.setAllAllocations(allAllocations);
        shiftResult.setShiftProductionResults(shiftProductionResults);

        log.info("========== 班次排程完成，天={}, 班次={} ==========\n", day, shiftConfig.getShiftCode());
        return shiftResult;
    }

    /**
     * 更新机台在产状态
     */
    private Map<String, Set<String>> updateMachineOnlineStatus(
            List<MachineAllocationResult> allocations,
            Map<String, Set<String>> currentMachineOnlineMap) {

        // 首先复制当前状态
        Map<String, Set<String>> newMap = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : currentMachineOnlineMap.entrySet()) {
            newMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        // 遍历 allocations，将每个机台的所有胚胎添加到 newMap 中（合并续作+新增分配）
        // 注意：这里不是覆盖，而是合并（取并集）
        for (MachineAllocationResult allocation : allocations) {
            String machineCode = allocation.getMachineCode();
            Set<String> existingEmbryos = newMap.get(machineCode);
            if (existingEmbryos == null) {
                existingEmbryos = new HashSet<>();
                newMap.put(machineCode, existingEmbryos);
            }
            for (TaskAllocation taskAlloc : allocation.getTaskAllocations()) {
                if (taskAlloc.getEmbryoCode() != null) {
                    existingEmbryos.add(taskAlloc.getEmbryoCode());
                }
            }
        }

        log.debug("更新机台在产状态完成，共 {} 台机台: {}", newMap.size(), formatMachineEmbryoMap(newMap));
        return newMap;
    }
    
    /**
     * 格式化机台胚胎映射用于日志输出
     */
    private String formatMachineEmbryoMap(Map<String, Set<String>> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append("=[");
            boolean firstEmbryo = true;
            for (String embryo : entry.getValue()) {
                if (!firstEmbryo) sb.append(",");
                sb.append(embryo);
                firstEmbryo = false;
            }
            sb.append("]");
            first = false;
        }
        sb.append("}");
        return sb.toString();
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
                            merged.setSourceTask(existing.getSourceTask());
                            merged.setPlanStartTime(existing.getPlanStartTime());
                            merged.setPlanEndTime(existing.getPlanEndTime());
                            // 注意：isLastEndingBatch 不在此处合并，每个班次保持独立状态
                            // merged 对象不会被使用（因为 existing == null 时直接返回 spr）
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
                    result.setTotalStock(stock != null ? new BigDecimal(stock) : BigDecimal.ZERO);
                } else {
                    result.setTotalStock(BigDecimal.ZERO);
                }
            } else {
                result.setTotalStock(BigDecimal.ZERO);
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

            // ---- 成型批次号 & 工单号 ----
            String dateStr = startDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            String cxBatchNo = "CXPC" + dateStr + String.format("%03d", results.size() + 1);
            String orderNo = "CXGD" + dateStr + String.format("%03d", results.size() + 1);
            result.setCxBatchNo(cxBatchNo);
            result.setOrderNo(orderNo);

            // ---- 收尾提示 ----
            if (result.getCxRemainQty() != null && result.getCxRemainQty().compareTo(BigDecimal.ZERO) <= 0) {
                result.setMarkCloseOutTip("0");
            } else {
                result.setMarkCloseOutTip("1");
            }

            // ---- 映射班次排量到 CLASS1~8 ----
            for (Map.Entry<String, ShiftScheduleService.ShiftProductionResult> classEntry : classSprMap.entrySet()) {
                setClassFieldValue(result, classEntry.getKey(), classEntry.getValue(), primaryLh, materialCode);
            }

            // ---- 班次未排量的栏位补零 ----
            fillDefaultClassValues(result, classSprMap.keySet());

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
        // 打印前5条验证数据
        for (int i = 0; i < Math.min(5, allDetails.size()); i++) {
            CxScheduleDetail d = allDetails.get(i);
            log.info("子表明细[{}]: 机台={}, 胎胚={}, CLASS1=[TRIP={},PLAN={},HOURS={}], CLASS2=[TRIP={},PLAN={}], CLASS3=[TRIP={},PLAN={}], CLASS4=[TRIP={},PLAN={}], CLASS5=[TRIP={},PLAN={}], CLASS6=[TRIP={},PLAN={}], CLASS7=[TRIP={},PLAN={}], CLASS8=[TRIP={},PLAN={}]",
                    i, d.getCxMachineCode(), d.getEmbryoCode(),
                    d.getClass1TripNo(), d.getClass1PlanQty(), d.getClass1StockHours(),
                    d.getClass2TripNo(), d.getClass2PlanQty(),
                    d.getClass3TripNo(), d.getClass3PlanQty(),
                    d.getClass4TripNo(), d.getClass4PlanQty(),
                    d.getClass5TripNo(), d.getClass5PlanQty(),
                    d.getClass6TripNo(), d.getClass6PlanQty(),
                    d.getClass7TripNo(), d.getClass7PlanQty(),
                    d.getClass8TripNo(), d.getClass8PlanQty());
        }
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
                detail.setClass1PlanQty(BigDecimal.valueOf(trip.getPlanQty()));
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
                detail.setClass2PlanQty(BigDecimal.valueOf(trip.getPlanQty()));
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
                detail.setClass3PlanQty(BigDecimal.valueOf(trip.getPlanQty()));
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
                detail.setClass4PlanQty(BigDecimal.valueOf(trip.getPlanQty()));
                detail.setClass4TripNo(String.valueOf(trip.getTripNo()));
                detail.setClass4TripCapacity(BigDecimal.valueOf(trip.getTripCapacity()));
                detail.setClass4StockHours(trip.getStockHours());
                detail.setClass4Sequence(trip.getSequence());
                detail.setClass4PlanStartTime(trip.getPlanStartTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanStartTime()) : null);
                detail.setClass4PlanEndTime(trip.getPlanEndTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanEndTime()) : null);
                break;
            case "CLASS5":
                detail.setClass5PlanQty(BigDecimal.valueOf(trip.getPlanQty()));
                detail.setClass5TripNo(String.valueOf(trip.getTripNo()));
                detail.setClass5TripCapacity(BigDecimal.valueOf(trip.getTripCapacity()));
                detail.setClass5StockHours(trip.getStockHours());
                detail.setClass5Sequence(trip.getSequence());
                detail.setClass5PlanStartTime(trip.getPlanStartTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanStartTime()) : null);
                detail.setClass5PlanEndTime(trip.getPlanEndTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanEndTime()) : null);
                break;
            case "CLASS6":
                detail.setClass6PlanQty(BigDecimal.valueOf(trip.getPlanQty()));
                detail.setClass6TripNo(String.valueOf(trip.getTripNo()));
                detail.setClass6TripCapacity(BigDecimal.valueOf(trip.getTripCapacity()));
                detail.setClass6StockHours(trip.getStockHours());
                detail.setClass6Sequence(trip.getSequence());
                detail.setClass6PlanStartTime(trip.getPlanStartTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanStartTime()) : null);
                detail.setClass6PlanEndTime(trip.getPlanEndTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanEndTime()) : null);
                break;
            case "CLASS7":
                detail.setClass7PlanQty(BigDecimal.valueOf(trip.getPlanQty()));
                detail.setClass7TripNo(String.valueOf(trip.getTripNo()));
                detail.setClass7TripCapacity(BigDecimal.valueOf(trip.getTripCapacity()));
                detail.setClass7StockHours(trip.getStockHours());
                detail.setClass7Sequence(trip.getSequence());
                detail.setClass7PlanStartTime(trip.getPlanStartTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanStartTime()) : null);
                detail.setClass7PlanEndTime(trip.getPlanEndTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanEndTime()) : null);
                break;
            case "CLASS8":
                detail.setClass8PlanQty(BigDecimal.valueOf(trip.getPlanQty()));
                detail.setClass8TripNo(String.valueOf(trip.getTripNo()));
                detail.setClass8TripCapacity(BigDecimal.valueOf(trip.getTripCapacity()));
                detail.setClass8StockHours(trip.getStockHours());
                detail.setClass8Sequence(trip.getSequence());
                detail.setClass8PlanStartTime(trip.getPlanStartTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanStartTime()) : null);
                detail.setClass8PlanEndTime(trip.getPlanEndTime() != null
                        ? java.sql.Timestamp.valueOf(trip.getPlanEndTime()) : null);
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
    private void setClassFieldValue(CxScheduleResult result, String classField, ShiftScheduleService.ShiftProductionResult spr,
                                       LhScheduleResult primaryLh, String materialCode) {
        if (classField == null || spr == null) {
            return;
        }

        // 构建原因分析字符串
        String analysis = buildTaskAnalysis(spr);

        // 计划量（无值给0）
        BigDecimal planQty = spr.getQuantity() != null ? new BigDecimal(spr.getQuantity()) : BigDecimal.ZERO;
        // 完成量默认给0
        BigDecimal finishQty = BigDecimal.ZERO;
        // 示方书编号：取硫化任务的lhNo
        String recipeNo = (primaryLh != null) ? primaryLh.getLhNo() : null;
        // 示方书类型：通过基础表 MdmSkuConstructionRef 查询 lhType
        String recipeType = null;
        if (materialCode != null && recipeNo != null) {
            try {
                MdmSkuConstructionRef ref = skuConstructionRefMapper.selectByMaterialCodeAndLhNo(materialCode, recipeNo);
                if (ref != null) {
                    recipeType = ref.getLhType();
                }
            } catch (Exception e) {
                log.debug("查询示方书类型失败: materialCode={}, lhNo={}", materialCode, recipeNo);
            }
        }

        switch (classField) {
            case "CLASS1":
                result.setClass1PlanQty(planQty);
                result.setClass1FinishQty(finishQty);
                result.setClass1RecipeNo(recipeNo);
                result.setClass1RecipeType(recipeType);
                if (analysis != null) { result.setClass1Analysis(analysis); }
                break;
            case "CLASS2":
                result.setClass2PlanQty(planQty);
                result.setClass2FinishQty(finishQty);
                result.setClass2RecipeNo(recipeNo);
                result.setClass2RecipeType(recipeType);
                if (analysis != null) { result.setClass2Analysis(analysis); }
                break;
            case "CLASS3":
                result.setClass3PlanQty(planQty);
                result.setClass3FinishQty(finishQty);
                result.setClass3RecipeNo(recipeNo);
                result.setClass3RecipeType(recipeType);
                if (analysis != null) { result.setClass3Analysis(analysis); }
                break;
            case "CLASS4":
                result.setClass4PlanQty(planQty);
                result.setClass4FinishQty(finishQty);
                result.setClass4RecipeNo(recipeNo);
                result.setClass4RecipeType(recipeType);
                if (analysis != null) { result.setClass4Analysis(analysis); }
                break;
            case "CLASS5":
                result.setClass5PlanQty(planQty);
                result.setClass5FinishQty(finishQty);
                result.setClass5RecipeNo(recipeNo);
                result.setClass5RecipeType(recipeType);
                if (analysis != null) { result.setClass5Analysis(analysis); }
                break;
            case "CLASS6":
                result.setClass6PlanQty(planQty);
                result.setClass6FinishQty(finishQty);
                result.setClass6RecipeNo(recipeNo);
                result.setClass6RecipeType(recipeType);
                if (analysis != null) { result.setClass6Analysis(analysis); }
                break;
            case "CLASS7":
                result.setClass7PlanQty(planQty);
                result.setClass7FinishQty(finishQty);
                result.setClass7RecipeNo(recipeNo);
                result.setClass7RecipeType(recipeType);
                if (analysis != null) { result.setClass7Analysis(analysis); }
                break;
            case "CLASS8":
                result.setClass8PlanQty(planQty);
                result.setClass8FinishQty(finishQty);
                result.setClass8RecipeNo(recipeNo);
                result.setClass8RecipeType(recipeType);
                if (analysis != null) { result.setClass8Analysis(analysis); }
                break;
            default:
                log.warn("未知的 CLASS_FIELD: {}", classField);
        }
    }

    /**
     * 填充未排产班次的默认值（PLAN_QTY=0, FINISH_QTY=0）
     */
    private void fillDefaultClassValues(CxScheduleResult result, Set<String> filledClasses) {
        BigDecimal zero = BigDecimal.ZERO;
        if (!filledClasses.contains("CLASS1")) { result.setClass1PlanQty(zero); result.setClass1FinishQty(zero); }
        if (!filledClasses.contains("CLASS2")) { result.setClass2PlanQty(zero); result.setClass2FinishQty(zero); }
        if (!filledClasses.contains("CLASS3")) { result.setClass3PlanQty(zero); result.setClass3FinishQty(zero); }
        if (!filledClasses.contains("CLASS4")) { result.setClass4PlanQty(zero); result.setClass4FinishQty(zero); }
        if (!filledClasses.contains("CLASS5")) { result.setClass5PlanQty(zero); result.setClass5FinishQty(zero); }
        if (!filledClasses.contains("CLASS6")) { result.setClass6PlanQty(zero); result.setClass6FinishQty(zero); }
        if (!filledClasses.contains("CLASS7")) { result.setClass7PlanQty(zero); result.setClass7FinishQty(zero); }
        if (!filledClasses.contains("CLASS8")) { result.setClass8PlanQty(zero); result.setClass8FinishQty(zero); }
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
            if (Boolean.TRUE.equals(spr.getIsLastEndingBatch())) {
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
     * @param context                排程上下文
     * @param shiftAllocations       该班次的机台分配结果
     * @param shiftConfigs           该班次的配置
     * @param currentShiftConfig     当前班次配置（用于确定取哪个 CLASS 字段）
     * @param shiftProductionResults 当前班次的成型排产结果（用于计算成型产出）
     */
    private void updateContextForNextShift(
            ScheduleContextVo context,
            List<MachineAllocationResult> shiftAllocations,
            List<CxShiftConfig> shiftConfigs,
            CxShiftConfig currentShiftConfig,
            List<ShiftScheduleService.ShiftProductionResult> shiftProductionResults) {
        // 直接复用 updateContextForNextDay 逻辑，它已经按班次配置计算硫化消耗
        updateContextForNextDay(context, shiftAllocations, shiftConfigs, currentShiftConfig, shiftProductionResults);
    }

    /**
     * 每天/每班次排程后更新上下文中的库存和硫化余量，供下一天/下一班次排程使用
     *
     * <p>更新逻辑：
     * <ol>
     *   <li>计算当天成型产出（按胎胚编码汇总 ShiftProductionResult.quantity）</li>
     *   <li>计算当天硫化消耗（按胎胚编码汇总，根据当天班次CLASS字段获取硫化计划量）</li>
     *   <li>更新materialStockMap：每条硫化任务的库存 = 原库存 - 硫化消耗 + 比例分配的成型产出</li>
     *   <li>更新monthSurplusMap：硫化余量 -= 当天硫化消耗</li>
     *   <li>重算formingRemainderMap：成型余量 = 硫化余量 - 库存</li>
     * </ol>
     *
     * @param context                排程上下文
     * @param dayAllocations         当天排程结果
     * @param dayShifts              当天班次配置
     * @param currentShiftConfig     当前班次配置（用于确定取哪个 CLASS 字段）
     * @param shiftProductionResults 当前班次的成型排产结果（用于计算成型产出）
     */
    private void updateContextForNextDay(
            ScheduleContextVo context,
            List<MachineAllocationResult> dayAllocations,
            List<CxShiftConfig> dayShifts,
            CxShiftConfig currentShiftConfig,
            List<ShiftScheduleService.ShiftProductionResult> shiftProductionResults) {

        LocalDate scheduleDate = context.getCurrentScheduleDate();
        int currentDay = context.getCurrentScheduleDay();
        
        // 提取班次名称（如 DAY_D1, NIGHT_N1 等）
        String shiftName = "未知";
        if (dayShifts != null && !dayShifts.isEmpty()) {
            CxShiftConfig firstShift = dayShifts.get(0);
            if (firstShift.getShiftCode() != null) {
                shiftName = firstShift.getShiftCode();
            } else if (firstShift.getShiftName() != null) {
                shiftName = firstShift.getShiftName();
            }
        }
        
        log.info("\n========== 第 {} 天 - {} 班排程后上下文更新 (日期: {}) ==========",
                currentDay, shiftName, scheduleDate);

        // 1. 计算当天成型产出（按胎胚编码汇总，从 ShiftProductionResult.quantity 获取）
        Map<String, Integer> formingOutputMap = calculateFormingOutputByEmbryo(dayAllocations, context, currentShiftConfig, shiftProductionResults);
        log.info("【步骤1】成型产出汇总（胎胚 → 产出量，来自 ShiftProductionResult）:");
        for (Map.Entry<String, Integer> entry : formingOutputMap.entrySet()) {
            log.info("  - {}: {} 条", entry.getKey(), entry.getValue());
        }

        // 2. 计算当天硫化消耗（按胎胚编码汇总，根据当天班次CLASS字段获取计划量）
        Map<String, Integer> vulcanizingConsumptionByEmbryo = new HashMap<>();
        Map<Long, Integer> vulcanizingConsumptionByLhId = new HashMap<>();
        calculateVulcanizingConsumption(context.getLhScheduleResults(), dayShifts,
                vulcanizingConsumptionByEmbryo, vulcanizingConsumptionByLhId);
        log.info("【步骤2】硫化消耗汇总（胎胚 → 消耗量）:");
        for (Map.Entry<String, Integer> entry : vulcanizingConsumptionByEmbryo.entrySet()) {
            log.info("  - {}: {} 条", entry.getKey(), entry.getValue());
        }

        // 2.5. 先更新 CxStock 实体中的 stockNum（计算新库存 = 原库存 + 成型产出 - 硫化消耗）
        log.info("【步骤2.5】更新胎胚库存表（CxStock），计算新库存...");
        updateCxStockEntities(context, formingOutputMap, vulcanizingConsumptionByEmbryo);

        // 3. 重新按日硫化量比例分配库存给硫化任务（使用更新后的库存）
        log.info("【步骤3】按日硫化量比例重新分配库存（materialStockMap）...");
        reallocateStockByDayVulcanizationCapacity(context, dayShifts, scheduleDate);

        // 5. 更新 monthSurplusMap（硫化余量 -= 当天硫化消耗）
        log.info("【步骤4】更新硫化余量（monthSurplusMap）...");
        updateMonthSurplus(context, vulcanizingConsumptionByEmbryo);

        // 6. 重算 formingRemainderMap（成型余量 = 硫化余量 - 库存）
        log.info("【步骤5】重算成型余量（formingRemainderMap）...");
        recalculateFormingRemainder(context);

        log.info("========== 第 {} 天 - {} 班上下文更新完成 ==========\n",
                currentDay, shiftName);
    }

    /**
     * 计算当天成型产出，按胎胚编码汇总
     *
     * <p>成型产出 = 从 ShiftProductionResult.quantity 汇总（这是成型机台实际生产的数量）
     *
     * @param dayAllocations         当天机台分配结果（未使用，保留参数兼容性）
     * @param context                排程上下文
     * @param currentShiftConfig     当前班次配置
     * @param shiftProductionResults 当前班次的成型排产结果
     * @return 胎胚编码 → 成型产出量
     */
    private Map<String, Integer> calculateFormingOutputByEmbryo(List<MachineAllocationResult> dayAllocations,
                                                                 ScheduleContextVo context,
                                                                 CxShiftConfig currentShiftConfig,
                                                                 List<ShiftScheduleService.ShiftProductionResult> shiftProductionResults) {
        Map<String, Integer> outputMap = new HashMap<>();
        
        if (shiftProductionResults == null || shiftProductionResults.isEmpty()) {
            log.warn("【调试】shiftProductionResults 为空，无法计算成型产出");
            return outputMap;
        }
        
        log.debug("【调试】计算成型产出 - 当前班次={}, shiftProductionResults 数={}",
                currentShiftConfig != null ? currentShiftConfig.getShiftCode() : "未知",
                shiftProductionResults.size());
        
        // 从 ShiftProductionResult 中汇总成型产出
        for (ShiftScheduleService.ShiftProductionResult spr : shiftProductionResults) {
            String embryoCode = spr.getEmbryoCode();
            Integer qty = spr.getQuantity();
            
            if (embryoCode != null && qty != null && qty > 0) {
                log.debug("  - 胎胚={}, 物料={}, quantity={}, machineCode={}",
                        embryoCode, spr.getMaterialCode(), qty, spr.getMachineCode());
                outputMap.merge(embryoCode, qty, Integer::sum);
            }
        }
        
        // 打印汇总统计
        log.info("【调试】成型产出汇总详情（来自 ShiftProductionResult，班次={}）:",
                currentShiftConfig != null ? currentShiftConfig.getShiftCode() : "未知");
        for (Map.Entry<String, Integer> entry : outputMap.entrySet()) {
            log.info("  - {}: {} 条", entry.getKey(), entry.getValue());
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
     * 按日硫化量比例重新分配库存给硫化任务
     *
     * <p>流程：
     * <ol>
     *   <li>使用更新后的 CxStock（新库存 = 原库存 + 成型产出 - 硫化消耗）</li>
     *   <li>调用 ScheduleServiceImpl.allocateStockByMaterialRatio 按日硫化量比例分配</li>
     *   <li>更新 context.materialStockMap</li>
     * </ol>
     *
     * @param context        排程上下文
     * @param dayShifts      当天班次配置
     * @param scheduleDate   排程日期
     */
    private void reallocateStockByDayVulcanizationCapacity(
            ScheduleContextVo context,
            List<CxShiftConfig> dayShifts,
            LocalDate scheduleDate) {

        // 获取更新后的库存列表
        List<CxStock> stocks = context.getStocks();
        if (stocks == null || stocks.isEmpty()) {
            log.warn("【步骤3】CxStock 为空，无法重新分配库存");
            return;
        }

        // 获取硫化排程结果
        List<LhScheduleResult> lhScheduleResults = context.getLhScheduleResults();
        if (lhScheduleResults == null || lhScheduleResults.isEmpty()) {
            log.warn("【步骤3】LhScheduleResults 为空，无法重新分配库存");
            return;
        }

        // 获取物料日硫化产能映射
        Map<String, MonthPlanProductLhCapacityVo> materialLhCapacityMap = context.getMaterialLhCapacityMap();
        if (materialLhCapacityMap == null || materialLhCapacityMap.isEmpty()) {
            log.warn("【步骤3】materialLhCapacityMap 为空，无法按日硫化量比例分配");
            return;
        }

        // 调用 ScheduleServiceImpl 的 allocateStockByMaterialRatio 方法
        // 注意：这里需要通过反射或者将逻辑提取到工具类中
        // 暂时先创建一个简化版本
        Map<String, Integer> newMaterialStockMap = allocateStockByMaterialRatioSimple(
                stocks, lhScheduleResults, dayShifts, scheduleDate, materialLhCapacityMap);

        // 更新 context
        context.setMaterialStockMap(newMaterialStockMap);
        log.info("【步骤3】materialStockMap 重新分配完成，共 {} 条记录", newMaterialStockMap.size());
    }

    /**
     * 简化的按日硫化量比例分配库存方法
     * （从 ScheduleServiceImpl.allocateStockByMaterialRatio 复制而来）
     */
    private Map<String, Integer> allocateStockByMaterialRatioSimple(
            List<CxStock> stocks,
            List<LhScheduleResult> lhScheduleResults,
            List<CxShiftConfig> dayShifts,
            LocalDate scheduleDate,
            Map<String, MonthPlanProductLhCapacityVo> materialLhCapacityMap) {

        Map<String, Integer> materialStockMap = new HashMap<>();

        for (CxStock stock : stocks) {
            String embryoCode = stock.getEmbryoCode();
            if (embryoCode == null) {
                continue;
            }

            int totalStock = stock.getStockNum() != null ? stock.getStockNum() : 0;
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
                // 胎胚对应多个硫化任务，按物料的日硫化量比例分配
                int totalDemand = 0;
                List<TaskDemandSimple> taskDemands = new ArrayList<>();

                for (LhScheduleResult lh : relatedTasks) {
                    String materialCode = lh.getMaterialCode();
                    int dayVulcanizationQty = 0;

                    // 从 materialLhCapacityMap 获取日硫化量
                    if (materialLhCapacityMap != null && materialCode != null) {
                        MonthPlanProductLhCapacityVo capacityVo = materialLhCapacityMap.get(materialCode);
                        if (capacityVo != null) {
                            // 使用默认日硫化量（优先标准产能，其次MES产能）
                            dayVulcanizationQty = capacityVo.getDefaultDayVulcanizationQty() != null
                                    ? capacityVo.getDefaultDayVulcanizationQty() : 0;
                        }
                    }

                    // 如果日硫化量为0，使用班次计划量作为后备
                    if (dayVulcanizationQty <= 0) {
                        ShiftPlanResultSimple shiftResult = getShiftPlanQtyWithShiftNameSimple(lh, dayShifts, scheduleDate);
                        dayVulcanizationQty = shiftResult.planQty;
                    }

                    taskDemands.add(new TaskDemandSimple(lh.getId(), dayVulcanizationQty, materialCode));
                    totalDemand += dayVulcanizationQty;
                }

                if (totalDemand == 0) {
                    // 总需求为0，平均分配
                    int avgStock = totalStock / taskDemands.size();
                    for (TaskDemandSimple td : taskDemands) {
                        materialStockMap.merge(td.taskKey, avgStock, Integer::sum);
                    }
                    log.debug("胎胚 {} 对应多个硫化任务但总日硫化量为0，平均分配库存 {}", embryoCode, avgStock);
                } else {
                    // 按日硫化量比例分配，最后一条用倒扣
                    int allocatedTotal = 0;

                    for (int i = 0; i < taskDemands.size(); i++) {
                        TaskDemandSimple td = taskDemands.get(i);
                        int currentStock;

                        if (i == taskDemands.size() - 1) {
                            // 最后一个硫化任务分配剩余库存（倒扣）
                            currentStock = totalStock - allocatedTotal;
                        } else {
                            // 按日硫化量比例分配
                            currentStock = (int) ((long) totalStock * td.demand / totalDemand);
                        }

                        materialStockMap.merge(td.taskKey, currentStock, Integer::sum);
                        allocatedTotal += currentStock;

                        log.debug("物料编码 {}，胎胚 {} 共用分配：硫化任务 {} 日硫化量 {}，分配库存 {}",
                                td.materialCode, embryoCode, td.taskKey, td.demand, currentStock);
                    }
                }
            }
        }

        return materialStockMap;
    }

    /**
     * 硫化任务需求（内部类）
     */
    private static class TaskDemandSimple {
        String taskKey;
        int demand;
        String materialCode;

        TaskDemandSimple(Long lhId, int demand, String materialCode) {
            this.taskKey = String.valueOf(lhId);
            this.demand = demand;
            this.materialCode = materialCode;
        }
    }

    /**
     * 班次计划量查询结果（内部类）
     */
    private static class ShiftPlanResultSimple {
        int planQty;

        ShiftPlanResultSimple(int planQty) {
            this.planQty = planQty;
        }
    }

    /**
     * 获取硫化任务的班次计划量（简化版）
     */
    private ShiftPlanResultSimple getShiftPlanQtyWithShiftNameSimple(
            LhScheduleResult lhResult, List<CxShiftConfig> dayShifts, LocalDate scheduleDate) {
        int defaultQty = lhResult.getDailyPlanQty() != null ? lhResult.getDailyPlanQty() : 0;
        if (dayShifts == null || dayShifts.isEmpty()) {
            return new ShiftPlanResultSimple(defaultQty);
        }

        // 简单返回第一个班次的计划量
        for (CxShiftConfig shiftConfig : dayShifts) {
            String classField = shiftConfig.getClassField();
            if (classField != null && classField.startsWith("CLASS")) {
                try {
                    int classIndex = Integer.parseInt(classField.substring(5));
                    Integer planQty = getClassPlanQtyByIndex(lhResult, classIndex);
                    if (planQty != null && planQty > 0) {
                        return new ShiftPlanResultSimple(planQty);
                    }
                } catch (NumberFormatException e) {
                    log.warn("无法解析班次字段: {}", classField);
                }
            }
        }

        return new ShiftPlanResultSimple(defaultQty);
    }
     /*
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
        log.info("  3.1 扣减硫化消耗（按硫化任务lhId）:");
        for (LhScheduleResult lhResult : lhResults) {
            if (lhResult.getId() == null) {
                continue;
            }
            String taskKey = String.valueOf(lhResult.getId());
            Integer consumption = vulcanizingConsumptionByLhId.get(lhResult.getId());
            if (consumption != null && consumption > 0) {
                int currentStock = materialStockMap.getOrDefault(taskKey, 0);
                int newStock = Math.max(0, currentStock - consumption);
                materialStockMap.put(taskKey, newStock);
                log.debug("    - lhId={}, 胎胚={}, 原库存={}, 消耗={}, 新库存={}",
                        taskKey, lhResult.getEmbryoCode(), currentStock, consumption, newStock);
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

        log.info("  3.2 分配成型产出（按胎胚 → 硫化任务）:");
        for (Map.Entry<String, Integer> entry : formingOutputMap.entrySet()) {
            String embryoCode = entry.getKey();
            int formingOutput = entry.getValue();
            if (formingOutput <= 0) {
                continue;
            }

            List<LhScheduleResult> relatedTasks = embryoToLhMap.get(embryoCode);
            if (relatedTasks == null || relatedTasks.isEmpty()) {
                log.warn("    - {}: 成型产出={} 条，但未找到对应硫化任务", embryoCode, formingOutput);
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
                log.info("    - {}: 产出={} 条，平均分配到 {} 个任务（每任务 {} 条）",
                        embryoCode, formingOutput, relatedTasks.size(), avgOutput);
                for (int i = 0; i < relatedTasks.size(); i++) {
                    String taskKey = String.valueOf(relatedTasks.get(i).getId());
                    int alloc = avgOutput + (i == 0 ? remaining : 0);
                    materialStockMap.merge(taskKey, alloc, Integer::sum);
                    log.debug("      * lhId={}, 分配={}", taskKey, alloc);
                }
            } else {
                // 按库存比例分配成型产出，最后一个任务用倒扣
                log.info("    - {}: 产出={} 条，按库存比例分配到 {} 个任务（总库存={}）",
                        embryoCode, formingOutput, relatedTasks.size(), totalAllocated);
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
                    log.debug("      * lhId={}, 当前库存={}, 分配={}", taskKey, currentAlloc, outputShare);
                }
            }
        }

        log.info("  materialStockMap 更新完成，共 {} 条记录", materialStockMap.size());
    }

    /**
     * 更新 CxStock 实体中的 stockNum
     *
     * <p>有效库存 = stockNum - overTimeStock - badNum + modifyNum
     * 所以调整库存时直接修改 stockNum 即可
     *
     * <p>如果某个胎胚在 CxStock 中没有记录但有成型产出，会补充创建新的库存记录
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
            stocks = new ArrayList<>();
            context.setStocks(stocks);
        }

        // 收集所有涉及的胎胚编码
        Set<String> allEmbryoCodes = new HashSet<>();
        allEmbryoCodes.addAll(formingOutputMap.keySet());
        allEmbryoCodes.addAll(vulcanizingConsumptionByEmbryo.keySet());

        // 构建现有库存映射（胎胚编码 → CxStock）
        Map<String, CxStock> existingStockMap = new HashMap<>();
        for (CxStock stock : stocks) {
            if (stock.getEmbryoCode() != null) {
                existingStockMap.put(stock.getEmbryoCode(), stock);
            }
        }

        // 遍历所有涉及的胎胚编码
        for (String embryoCode : allEmbryoCodes) {
            int formingOutput = formingOutputMap.getOrDefault(embryoCode, 0);
            int vulcanizingConsumption = vulcanizingConsumptionByEmbryo.getOrDefault(embryoCode, 0);
            int delta = formingOutput - vulcanizingConsumption;

            if (delta == 0) {
                continue;
            }

            CxStock stock = existingStockMap.get(embryoCode);
            if (stock != null) {
                // 已有库存记录，直接更新
                int currentStockNum = stock.getStockNum() != null ? stock.getStockNum() : 0;
                int newStockNum = Math.max(0, currentStockNum + delta);
                stock.setStockNum(newStockNum);
                log.info("  - {}: 原库存={}, 成型产出={}, 硫化消耗={}, 净变化={}, 新库存={}",
                        embryoCode, currentStockNum, formingOutput, vulcanizingConsumption, delta, newStockNum);
            } else {
                // 没有库存记录，但有成型产出或硫化消耗，需要补充创建
                int newStockNum = Math.max(0, delta);  // 新库存 = 成型产出 - 硫化消耗（不能为负）
                
                CxStock newStock = new CxStock();
                newStock.setEmbryoCode(embryoCode);
                newStock.setStockNum(newStockNum);
                newStock.setOverTimeStock(0);
                newStock.setBadNum(0);
                newStock.setModifyNum(0);
                newStock.setIsDelete("0");
                
                // 设置库存日期（使用当前排程日期）
                LocalDate scheduleDate = context.getCurrentScheduleDate();
                if (scheduleDate != null) {
                    newStock.setStockDate(java.sql.Date.valueOf(scheduleDate));
                }
                
                stocks.add(newStock);
                existingStockMap.put(embryoCode, newStock);
                
                log.info("  - {}: 【新增库存记录】成型产出={}, 硫化消耗={}, 净变化={}, 新库存={}",
                        embryoCode, formingOutput, vulcanizingConsumption, delta, newStockNum);
            }
        }
    }

    /**
     * 更新 monthSurplusMap（硫化余量 -= 当天硫化消耗）
     *
     * <p>注意：vulcanizingConsumptionByEmbryo 的 key 是胎胚编码，需要转换为物料编码
     *
     * @param context                        排程上下文
     * @param vulcanizingConsumptionByEmbryo 胎胚编码 → 硫化消耗量
     */
    private void updateMonthSurplus(
            ScheduleContextVo context,
            Map<String, Integer> vulcanizingConsumptionByEmbryo) {

        Map<String, MdmMonthSurplus> monthSurplusMap = context.getMonthSurplusMap();
        if (monthSurplusMap == null || monthSurplusMap.isEmpty()) {
            log.debug("【步骤4】monthSurplusMap 为空，跳过更新");
            return;
        }

        if (vulcanizingConsumptionByEmbryo == null || vulcanizingConsumptionByEmbryo.isEmpty()) {
            log.debug("【步骤4】vulcanizingConsumptionByEmbryo 为空，跳过更新");
            return;
        }

        // 构建胎胚编码 → 物料编码的映射
        List<LhScheduleResult> lhResults = context.getLhScheduleResults();
        Map<String, String> embryoToMaterialMap = new HashMap<>();
        if (lhResults != null) {
            for (LhScheduleResult lh : lhResults) {
                if (lh.getEmbryoCode() != null && lh.getMaterialCode() != null) {
                    embryoToMaterialMap.put(lh.getEmbryoCode(), lh.getMaterialCode());
                }
            }
        }

        // 按物料编码汇总硫化消耗
        Map<String, Integer> consumptionByMaterial = new HashMap<>();
        for (Map.Entry<String, Integer> entry : vulcanizingConsumptionByEmbryo.entrySet()) {
            String embryoCode = entry.getKey();
            int consumption = entry.getValue();
            
            String materialCode = embryoToMaterialMap.get(embryoCode);
            if (materialCode != null) {
                consumptionByMaterial.merge(materialCode, consumption, Integer::sum);
            } else {
                log.warn("【步骤4】胎胚 {} 未找到对应的物料编码", embryoCode);
            }
        }

        // 更新硫化余量
        log.info("【步骤4】硫化消耗按物料汇总详情:");
        for (Map.Entry<String, Integer> entry : consumptionByMaterial.entrySet()) {
            String materialCode = entry.getKey();
            int consumption = entry.getValue();
            
            MdmMonthSurplus surplus = monthSurplusMap.get(materialCode);
            if (surplus != null && surplus.getPlanSurplusQty() != null && consumption > 0) {
                BigDecimal oldSurplus = surplus.getPlanSurplusQty();
                BigDecimal newSurplus = oldSurplus.subtract(BigDecimal.valueOf(consumption));
                surplus.setPlanSurplusQty(newSurplus);
                log.info("  - {}: 原余量={}, 硫化消耗={}, 新余量={}",
                        materialCode, oldSurplus, consumption, newSurplus);
            } else {
                log.warn("  - {}: 未找到硫化余量记录或余量为空，消耗={}", materialCode, consumption);
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

        // 按物料编码汇总库存（从 materialStockMap 按硫化任务汇总）
        Map<String, Integer> stockByMaterial = new HashMap<>();
        if (lhResults != null && materialStockMap != null) {
            for (LhScheduleResult lh : lhResults) {
                if (lh.getMaterialCode() != null && lh.getId() != null) {
                    String taskKey = String.valueOf(lh.getId());
                    int stock = materialStockMap.getOrDefault(taskKey, 0);
                    stockByMaterial.merge(lh.getMaterialCode(), stock, Integer::sum);
                }
            }
        }

        // 重算成型余量
        Map<String, Integer> newFormingRemainderMap = new HashMap<>();
        log.info("【步骤5】重算成型余量（物料 → 硫化余量 - 库存 = 成型余量）:");
        for (Map.Entry<String, MdmMonthSurplus> entry : monthSurplusMap.entrySet()) {
            String materialCode = entry.getKey();
            MdmMonthSurplus surplus = entry.getValue();
            int vulcanizingRemainder = surplus.getPlanSurplusQty() != null
                    ? surplus.getPlanSurplusQty().intValue() : 0;
            int materialStock = stockByMaterial.getOrDefault(materialCode, 0);
            int formingRemainder = Math.max(0, vulcanizingRemainder - materialStock);
            newFormingRemainderMap.put(materialCode, formingRemainder);
            log.info("  - {}: 硫化余量={}, 库存={}, 成型余量={}",
                    materialCode, vulcanizingRemainder, materialStock, formingRemainder);
        }

        context.setFormingRemainderMap(newFormingRemainderMap);
        log.info("  formingRemainderMap 重算完成，共 {} 条记录", newFormingRemainderMap.size());
    }
}
