package com.zlt.aps.cx.service.impl;

import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.entity.CxPrecisionPlan;
import com.zlt.aps.cx.entity.CxStock;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
// Engine 包 - 核心算法
import com.zlt.aps.cx.service.engine.ContinueTaskProcessor;
import com.zlt.aps.cx.service.engine.CoreScheduleAlgorithmService;
import com.zlt.aps.cx.service.engine.NewTaskProcessor;
import com.zlt.aps.cx.service.engine.ShiftScheduleService;
import com.zlt.aps.cx.service.engine.TaskGroupService;
import com.zlt.aps.cx.service.engine.TrialTaskProcessor;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    /** 默认排程天数 */
    private static final int DEFAULT_SCHEDULE_DAYS = 3;

    @Override
    public List<CxScheduleResult> executeSchedule(ScheduleContextDTO context) {
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
            LocalDate currentScheduleDate = context.getScheduleDate().plusDays(day - 1);
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
            ScheduleContextDTO context, 
            int day,
            List<CxShiftConfig> dayShifts,
            Map<String, Set<String>> machineOnlineEmbryoMap) {

        LocalDate scheduleDate = context.getCurrentScheduleDate() != null 
                ? context.getCurrentScheduleDate() 
                : context.getScheduleDate();
        
        log.info("========== 开始执行第 {} 天排程，日期: {} ==========", day, scheduleDate);

        // ==================== 第一步：S5.2 任务分组 ====================
        TaskGroupService.TaskGroupResult taskGroup = taskGroupService.groupTasks(
                context, machineOnlineEmbryoMap, scheduleDate);
        log.info("任务分组完成：续作 {} 个，试制 {} 个，新增 {} 个",
                taskGroup.getContinueTasks().size(),
                taskGroup.getTrialTasks().size(),
                taskGroup.getNewTasks().size());

        // ==================== 第二步：S5.3 处理续作任务 ====================
        List<MachineAllocationResult> continueAllocations = continueTaskProcessor.processContinueTasks(
                taskGroup.getContinueTasks(), context, scheduleDate, dayShifts, day);
        log.info("续作任务处理完成，机台分配数: {}", continueAllocations.size());

        // ==================== 第三步：S5.3 处理试制任务（独立处理） ====================
        // 获取未被续作任务占用的机台
        List<MdmMoldingMachine> availableMachinesForTrial = getAvailableMachinesForTrial(
                context.getAvailableMachines(), continueAllocations);
        
        List<MachineAllocationResult> trialAllocations = trialTaskProcessor.processTrialTasks(
                taskGroup.getTrialTasks(), context, scheduleDate, dayShifts, availableMachinesForTrial);
        log.info("试制任务处理完成，机台分配数: {}", trialAllocations.size());

        // ==================== 第四步：S5.3 处理新增任务（合并续作+新增，重新均衡） ====================
        List<MachineAllocationResult> newAllocations = newTaskProcessor.processNewTasks(
                taskGroup.getNewTasks(),
                context, 
                scheduleDate, 
                dayShifts, 
                day,
                continueAllocations);
        log.info("新增任务处理完成，机台分配数: {}", newAllocations.size());

        // ==================== 第五步：合并分配结果 ====================
        // 方案A：newAllocations 已经包含续作+新增的合并结果
        List<MachineAllocationResult> allAllocations = new ArrayList<>();
        allAllocations.addAll(newAllocations);
        allAllocations.addAll(trialAllocations);

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
     * 获取未被续作任务占用的机台列表（用于试制任务）
     */
    private List<MdmMoldingMachine> getAvailableMachinesForTrial(
            List<MdmMoldingMachine> allMachines,
            List<MachineAllocationResult> continueAllocations) {
        
        if (CollectionUtils.isEmpty(allMachines)) {
            return new ArrayList<>();
        }
        
        // 收集已被续作任务占用的机台编码
        Set<String> occupiedMachineCodes = continueAllocations.stream()
                .map(MachineAllocationResult::getMachineCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        // 过滤出未被占用的机台
        return allMachines.stream()
                .filter(m -> !occupiedMachineCodes.contains(m.getCxMachineCode()))
                .collect(Collectors.toList());
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
     */
    private boolean isStopProductionDay(ScheduleContextDTO context, LocalDate date) {
        var workCalendar = context.getWorkCalendar();
        if (workCalendar != null) {
            var startDate = workCalendar.getStopStartDate();
            var endDate = workCalendar.getStopEndDate();
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

        if (context.getCurrentScheduleDate() != null 
                && date.equals(context.getCurrentScheduleDate())
                && Boolean.TRUE.equals(context.getIsClosingDay())) {
            return true;
        }

        return false;
    }

    /**
     * 构建排程结果
     */
    private List<CxScheduleResult> buildScheduleResults(
            ScheduleContextDTO context,
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
            CxScheduleResult result = new CxScheduleResult();
            result.setScheduleDate(scheduleDate.atStartOfDay());
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
                
                for (CxShiftConfig shiftConfig : dayShifts) {
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
            ScheduleContextDTO context,
            Map<String, Set<String>> machineOnlineEmbryoMap) {
        
        LocalDate scheduleDate = context.getCurrentScheduleDate() != null 
                ? context.getCurrentScheduleDate() 
                : context.getScheduleDate();
        
        TaskGroupService.TaskGroupResult groupResult = taskGroupService.groupTasks(
                context, machineOnlineEmbryoMap, scheduleDate);
        
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
        // 此方法由 processContinueTasks 和 processTrialAndNewTasks 实现
        return new ArrayList<>();
    }

    @Override
    public List<ShiftAllocationResult> balanceShiftAllocation(
            List<MachineAllocationResult> allocations,
            ScheduleContextDTO context) {
        return shiftScheduleService.balanceShiftAllocation(
                allocations, context.getCurrentShiftConfigs(), context);
    }

    @Override
    public List<CxScheduleDetail> calculateSequence(
            List<ShiftAllocationResult> shiftAllocations,
            ScheduleContextDTO context) {
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
            ScheduleContextDTO context) {
        // 简化实现
        return BigDecimal.ZERO;
    }

    @Override
    public boolean checkStructureConstraint(
            MdmMoldingMachine machine,
            MdmMaterialInfo material,
            ScheduleContextDTO context) {
        // 委托给 NewTaskProcessor
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
                : 4;
        return currentTypes < maxTypes;
    }

    @Override
    public int calculatePriorityScore(
            MdmMaterialInfo material,
            CxStock stock,
            ScheduleContextDTO context) {
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
}
