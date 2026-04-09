package com.zlt.aps.cx.service.impl;


import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.mapper.MdmWorkCalendarMapper;
import com.zlt.aps.cx.service.engine.*;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.mp.api.domain.entity.MdmWorkCalendar;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 核心排程算法服务实现类
 *
 * <p>负责排程主流程编排，具体业务逻辑委托给各专门服务：
 * <ul>
 *   <li>{@link TaskGroupService} - 任务分组服务</li>
 *   <li>{@link ContinueTaskProcessor} - 续作任务处理器</li>
 *   <li>{@link TrialTaskProcessor} - 试制任务处理器</li>
 *   <li>{@link NewTaskProcessor} - 新增任务处理器</li>
 *   <li>{@link ShiftScheduleService} - 班次排产服务</li>
 * </ul>
 *
 * <p>任务处理流程（方案A）：
 * <ol>
 *   <li>S5.2 任务分组：续作/试制/新增三类</li>
 *   <li>S5.3 处理续作任务</li>
 *   <li>S5.3 处理试制任务（独立处理，特殊约束）</li>
 *   <li>S5.3 处理新增任务（合并续作+新增，重新均衡）</li>
 *   <li>S5.3.7 班次排产</li>
 *   <li>生成排程结果</li>
 * </ol>
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoreScheduleAlgorithmServiceImpl implements CoreScheduleAlgorithmService {

    private final TaskGroupService taskGroupService;
    private final ContinueTaskProcessor continueTaskProcessor;
    private final TrialTaskProcessor trialTaskProcessor;
    private final NewTaskProcessor newTaskProcessor;
    private final ShiftScheduleService shiftScheduleService;
    private final MdmWorkCalendarMapper workCalendarMapper;

    /** 默认排程天数 */
    private static final int DEFAULT_SCHEDULE_DAYS = 3;

    /** 排程起始偏移天数：前端传入最后一天，需要往前推2天开始排产 */
    private static final int SCHEDULE_START_OFFSET_DAYS = 2;

    @Override
    public List<CxScheduleResult> executeSchedule(ScheduleContextVo context) {
        log.info("开始执行排程算法，日期: {}", context.getScheduleDate());

        // 使用 ScheduleServiceImpl.buildScheduleContext 中已加载的班次配置
        List<CxShiftConfig> allShiftConfigs = context.getShiftConfigList();
        if (allShiftConfigs == null || allShiftConfigs.isEmpty()) {
            log.error("班次配置为空，请先调用 buildScheduleContext 加载班次配置");
            return new ArrayList<>();
        }

        // 按排程天数分组
        Map<Integer, List<CxShiftConfig>> dayShiftMap = allShiftConfigs.stream()
                .filter(c -> c.getScheduleDay() != null)
                .collect(Collectors.groupingBy(CxShiftConfig::getScheduleDay));

        // 获取排程天数
        int scheduleDays = context.getScheduleDays() != null ? context.getScheduleDays() : DEFAULT_SCHEDULE_DAYS;

        List<CxScheduleResult> allResults = new ArrayList<>();

        // 记录每天的机台在产状态
        Map<String, Set<String>> dailyMachineOnlineEmbryoMap = null;

        // 连续执行多天排程
        for (int day = 1; day <= scheduleDays; day++) {
            List<CxShiftConfig> dayShifts = dayShiftMap.get(day);
            if (CollectionUtils.isEmpty(dayShifts)) {
                log.warn("第 {} 天没有配置班次，跳过", day);
                continue;
            }

            // 设置当前天的上下文
            // 前端传入的是最后一天（如2026-03-28），需要往前推2天开始排产（如2026-03-26）
            LocalDate currentScheduleDate = context.getScheduleDate().minusDays(SCHEDULE_START_OFFSET_DAYS).plusDays(day - 1);
            context.setCurrentScheduleDay(day);
            context.setCurrentScheduleDate(currentScheduleDate);
            context.setCurrentShiftConfigs(dayShifts);

            // 检查当前天是否是停产日
            if (isStopProductionDay(context, currentScheduleDate)) {
                log.info("第 {} 天日期 {} 是停产日，跳过排程", day, currentScheduleDate);
                continue;
            }

            log.info("执行第 {} 天排程，日期: {}，班次数: {}", day, currentScheduleDate, dayShifts.size());

            // 第一天使用初始机台在产状态
            if (day == 1) {
                dailyMachineOnlineEmbryoMap = context.getMachineOnlineEmbryoMap();
                if (dailyMachineOnlineEmbryoMap == null) {
                    dailyMachineOnlineEmbryoMap = new HashMap<>();
                }
            }

            // 执行该天的排程
            List<CxScheduleResult> dayResults = executeDaySchedule(
                    context, day, dayShifts, dailyMachineOnlineEmbryoMap);
            allResults.addAll(dayResults);

            // 更新下一天的机台在产状态
            dailyMachineOnlineEmbryoMap = updateMachineOnlineStatus(dayResults, dailyMachineOnlineEmbryoMap);

            // 更新库存和硫化余量，供下一天排程使用
            if (day < scheduleDays) {
                updateContextForNextDay(context, dayResults, dayShifts);
            }
        }

        log.info("排程算法执行完成，共 {} 天，总结果数: {}", scheduleDays, allResults.size());
        return allResults;
    }

    /**
     * 执行单天排程
     *
     * <p>排程流程（方案A：合并续作+新增重新均衡）：
     * <ol>
     *   <li>S5.2 任务分组：续作/试制/新增三类</li>
     *   <li>S5.3 处理续作任务</li>
     *   <li>S5.3 处理试制任务（独立处理，特殊约束）</li>
     *   <li>S5.3 处理新增任务（合并续作+新增，重新均衡）</li>
     *   <li>S5.3.7 班次排产</li>
     *   <li>生成排程结果</li>
     * </ol>
     */
    private List<CxScheduleResult> executeDaySchedule(
            ScheduleContextVo context,
            int day,
            List<CxShiftConfig> dayShifts,
            Map<String, Set<String>> machineOnlineEmbryoMap) {

        LocalDate scheduleDate = context.getCurrentScheduleDate() != null
                ? context.getCurrentScheduleDate()
                : context.getScheduleDate();

        log.info("========== 开始执行第 {} 天排程，日期: {} ==========", day, scheduleDate);

        // ==================== 第一步：S5.2 任务分组 ====================
        // 传入当前天的班次配置，获取对应班次的硫化计划量
        TaskGroupService.TaskGroupResult taskGroup = taskGroupService.groupTasks(
                context, machineOnlineEmbryoMap, scheduleDate, dayShifts);
        log.info("任务分组完成：续作 {} 个，试制 {} 个，新增 {} 个",
                taskGroup.getContinueTasks().size(),
                taskGroup.getTrialTasks().size(),
                taskGroup.getNewTasks().size());

        // ==================== 第二步：S5.3 处理续作任务 ====================
        List<MachineAllocationResult> continueAllocations = continueTaskProcessor.processContinueTasks(
                taskGroup.getContinueTasks(), context, scheduleDate, dayShifts, day);
        log.info("续作任务处理完成，机台分配数: {}", continueAllocations.size());

        // ==================== 第三步：S5.3 处理试制任务（独立处理） ====================
        // 试制任务可在任意机台上执行（包括续作占用的机台）
        List<MachineAllocationResult> trialAllocations = trialTaskProcessor.processTrialTasks(
                taskGroup.getTrialTasks(), context, scheduleDate, dayShifts, context.getAvailableMachines());
        log.info("试制任务处理完成，机台分配数: {}", trialAllocations.size());

        // ==================== 第四步：S5.3 处理新增任务（合并续作+新增，重新均衡） ====================
        // 注意：量试约束（与试制同物料+结构→同机台）在 NewTaskProcessor 中处理
        List<MachineAllocationResult> newAllocations = newTaskProcessor.processNewTasks(
                taskGroup.getNewTasks(),
                context,
                scheduleDate,
                dayShifts,
                day,
                continueAllocations,
                trialAllocations);
        log.info("新增任务处理完成，机台分配数: {}", newAllocations.size());

        // ==================== 第五步：合并分配结果 ====================
        // 方案A：newAllocations 已经包含续作+新增的合并结果
        List<MachineAllocationResult> allAllocations = new ArrayList<>();
        allAllocations.addAll(newAllocations);
        allAllocations.addAll(trialAllocations);

        // 班次分配前检查
        log.info("班次分配前检查: 总分配数={}", allAllocations.size());

        // ==================== 第六步：S5.3.7 班次排产 ====================
        List<ShiftAllocationResult> shiftAllocations = shiftScheduleService.balanceShiftAllocation(
                allAllocations, dayShifts, context);
        log.info("班次排产完成");

        // ==================== 第七步：生成排程结果 ====================
        List<CxScheduleResult> results = buildScheduleResults(context, allAllocations, shiftAllocations, dayShifts);

        log.info("========== 第 {} 天排程完成，排程结果数: {} ==========\n", day, results.size());
        return results;
    }

    /**
     * 更新机台在产状态
     */
    private Map<String, Set<String>> updateMachineOnlineStatus(
            List<CxScheduleResult> dayResults,
            Map<String, Set<String>> currentMachineOnlineMap) {

        Map<String, Set<String>> newMap = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : currentMachineOnlineMap.entrySet()) {
            newMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

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
     * 判断是否为停产日
     * 从当前日期往前找最近一个有 dayFlag 标识的日期。
     * 停产标识的那一天有量，只有停产日之后才算停产。
     */
    private boolean isStopProductionDay(ScheduleContextVo context, LocalDate date) {
        DayFlagInfo flagInfo = findNearestDayFlag(context.getCurrentScheduleDate());
        if (flagInfo == null || flagInfo.dayFlag == null) {
            return false;
        }
        // 最近的标识是「停」→ 停产标识日之后都是停产日（停产日当天有量，不算停产）
        if ("0".equals(flagInfo.dayFlag)) {
            boolean isStopDay = date.isAfter(flagInfo.nearestDate);
            log.info("日期 {} 最近 dayFlag={}（停），标识日={}，判定为{}",
                    date, flagInfo.dayFlag, flagInfo.nearestDate, isStopDay ? "停产日" : "正常");
            return isStopDay;
        }
        return false;
    }
    
    /**
     * 从当前排产日期往前找最近一个有 dayFlag 标识的日期
     *
     * @param date 当前排产日期
     * @return 最近标识信息，包含标识日期和标识值（"0"=停，"1"=开）
     */
    public DayFlagInfo findNearestDayFlag(LocalDate date) {
        // 最多往前查 30 天
        for (int i = 0; i < 30; i++) {
            LocalDate queryDate = date.minusDays(i);
            MdmWorkCalendar calendar = workCalendarMapper.selectOne(
                    new LambdaQueryWrapper<MdmWorkCalendar>()
                            .eq(MdmWorkCalendar::getProcCode, "CX")
                            .eq(MdmWorkCalendar::getProductionDate, java.sql.Date.valueOf(queryDate)));
            if (calendar != null && calendar.getDayFlag() != null) {
                return new DayFlagInfo(queryDate, calendar.getDayFlag());
            }
        }
        return null;
    }
    
    /**
     * 最近工作日历标识信息
     */
    public class DayFlagInfo {
        /** 标识日期 */
        public final LocalDate nearestDate;
        /** 标识值：0=停，1=开 */
        public final String dayFlag;
        
        public DayFlagInfo(LocalDate nearestDate, String dayFlag) {
            this.nearestDate = nearestDate;
            this.dayFlag = dayFlag;
        }
    }

    /**
     * 构建排程结果
     */
    private List<CxScheduleResult> buildScheduleResults(
            ScheduleContextVo context,
            List<MachineAllocationResult> allocations,
            List<ShiftAllocationResult> shiftAllocations,
            List<CxShiftConfig> dayShifts) {

        List<CxScheduleResult> results = new ArrayList<>();
        LocalDate scheduleDate = context.getCurrentScheduleDate() != null
                ? context.getCurrentScheduleDate()
                : context.getScheduleDate();

        Map<String, ShiftAllocationResult> shiftMap = shiftAllocations.stream()
                .collect(Collectors.toMap(ShiftAllocationResult::getMachineCode, s -> s));

        for (MachineAllocationResult allocation : allocations) {
            log.info("【结果构建】机台 {} usedCapacity={}, taskCount={}", 
                    allocation.getMachineCode(), allocation.getUsedCapacity(),
                    allocation.getTaskAllocations() != null ? allocation.getTaskAllocations().size() : 0);
            
            CxScheduleResult result = new CxScheduleResult();
            result.setScheduleDate(java.sql.Timestamp.valueOf(scheduleDate.atStartOfDay()));
            result.setCxMachineCode(allocation.getMachineCode());
            result.setCxMachineType(allocation.getMachineType());
            result.setProductNum(new BigDecimal(allocation.getUsedCapacity()));
            result.setProductionStatus("0");
            result.setIsRelease("0");
            result.setDataSource("0");
            result.setCreateTime(new Date());

            // 按班次配置映射班次计划量
            ShiftAllocationResult shiftResult = shiftMap.get(allocation.getMachineCode());
            if (shiftResult != null) {
                Map<String, Integer> shiftPlanQty = shiftResult.getShiftPlanQty();
                log.info("机台 {} 班次计划量映射: {}", allocation.getMachineCode(), shiftPlanQty);

                for (CxShiftConfig shiftConfig : dayShifts) {
                    String classField = shiftConfig.getClassField();
                    String shiftCode = shiftConfig.getShiftCode();
                    Integer shiftQty = shiftPlanQty.getOrDefault(shiftCode, 0);

                    setClassFieldValue(result, classField, shiftQty);
                }
            } else {
                log.warn("机台 {} 未找到班次分配结果", allocation.getMachineCode());
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

    // ==================== 以下为接口方法实现（委托给专门服务） ====================

    @Override
    public List<DailyEmbryoTask> calculateDailyEmbryoTasks(
            ScheduleContextVo context,
            Map<String, Set<String>> machineOnlineEmbryoMap,
            List<CxShiftConfig> dayShifts) {

        LocalDate scheduleDate = context.getCurrentScheduleDate() != null
                ? context.getCurrentScheduleDate()
                : context.getScheduleDate();

        TaskGroupService.TaskGroupResult groupResult = taskGroupService.groupTasks(
                context, machineOnlineEmbryoMap, scheduleDate, dayShifts);

        List<DailyEmbryoTask> allTasks = new ArrayList<>();
        allTasks.addAll(groupResult.getContinueTasks());
        allTasks.addAll(groupResult.getTrialTasks());
        allTasks.addAll(groupResult.getNewTasks());

        return allTasks;
    }

    @Override
    public List<MachineAllocationResult> allocateTasksToMachines(
            List<DailyEmbryoTask> tasks,
            ScheduleContextVo context) {
        // 此方法由 processContinueTasks 和 processTrialAndNewTasks 实现
        return new ArrayList<>();
    }

    @Override
    public List<ShiftAllocationResult> balanceShiftAllocation(
            List<MachineAllocationResult> allocations,
            ScheduleContextVo context) {
        return shiftScheduleService.balanceShiftAllocation(
                allocations, context.getCurrentShiftConfigs(), context);
    }

    @Override
    public List<CxScheduleDetail> calculateSequence(
            List<ShiftAllocationResult> shiftAllocations,
            ScheduleContextVo context) {
        // TODO: 实现顺位排序
        return new ArrayList<>();
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

        BigDecimal hourlyOutput = BigDecimal.valueOf(vulcanizeMachineCount)
                .multiply(BigDecimal.valueOf(vulcanizeMoldCount))
                .multiply(BigDecimal.valueOf(0.5));

        return BigDecimal.valueOf(effectiveStock)
                .divide(hourlyOutput, 2, BigDecimal.ROUND_HALF_UP);
    }

    @Override
    public BigDecimal calculateDailyDemand(
            MdmMaterialInfo material,
            CxStock stock,
            ScheduleContextVo context) {
        // 简化实现
        return BigDecimal.ZERO;
    }

    @Override
    public boolean checkStructureConstraint(
            MdmMoldingMachine machine,
            MdmMaterialInfo material,
            ScheduleContextVo context) {
        // 委托给 NewTaskProcessor
        return true;
    }

    @Override
    public boolean checkTypeLimit(
            MdmMoldingMachine machine,
            int currentTypes,
            MdmMaterialInfo newMaterial,
            ScheduleContextVo context) {
        int maxTypes = context.getMaxTypesPerMachine() != null
                ? context.getMaxTypesPerMachine()
                : 4;
        return currentTypes < maxTypes;
    }

    @Override
    public int calculatePriorityScore(
            MdmMaterialInfo material,
            CxStock stock,
            ScheduleContextVo context) {
        // 委托给 TaskGroupService
        return 0;
    }

    @Override
    public int roundToTrip(int quantity, String mode) {
        int tripCapacity = 12;
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

    // ==================== 多天排程上下文更新 ====================

    /**
     * 每天排程后更新上下文中的库存和硫化余量，供下一天排程使用
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
            List<CxScheduleResult> dayResults,
            List<CxShiftConfig> dayShifts) {

        LocalDate scheduleDate = context.getCurrentScheduleDate();
        log.info("========== 更新第 {} 天排程后的库存和硫化余量，日期: {} ==========",
                context.getCurrentScheduleDay(), scheduleDate);

        // 1. 计算当天成型产出（按胎胚编码汇总）
        Map<String, Integer> formingOutputMap = calculateFormingOutputByEmbryo(dayResults);
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
     * @param dayResults 当天排程结果
     * @return 胎胚编码 → 成型产出量
     */
    private Map<String, Integer> calculateFormingOutputByEmbryo(List<CxScheduleResult> dayResults) {
        Map<String, Integer> outputMap = new HashMap<>();
        if (dayResults == null) {
            return outputMap;
        }
        for (CxScheduleResult result : dayResults) {
            String embryoCode = result.getEmbryoCode();
            BigDecimal productNum = result.getProductNum();
            if (embryoCode != null && productNum != null && productNum.intValue() > 0) {
                outputMap.merge(embryoCode, productNum.intValue(), Integer::sum);
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
