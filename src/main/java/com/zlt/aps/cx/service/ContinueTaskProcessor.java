package com.zlt.aps.cx.service;

import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.config.CxStructureShiftCapacity;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 续作任务处理器
 * 
 * <p>负责 S5.3 续作任务排产：
 * <ul>
 *   <li>S5.3.1 分配胎胚库存：按硫化需求占比分配</li>
 *   <li>S5.3.2 计算待排产量：(日硫化量 - 库存) × (1 + 损耗率) + 异常平摊</li>
 *   <li>S5.3.3 开停产特殊处理</li>
 *   <li>S5.3.4 收尾余量处理：主要产品补到一整车</li>
 *   <li>S5.3.5 补做逻辑：延误检测和补做计算</li>
 * </ul>
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContinueTaskProcessor {

    /** 开产首班排产时长（小时） */
    private static final int OPENING_SHIFT_HOURS = 6;
    
    /** 胎胚库容上限比例 */
    private static final double EMBRYO_STORAGE_RATIO = 0.9;
    
    /** 默认整车容量 */
    private static final int DEFAULT_TRIP_CAPACITY = 12;

    /**
     * 处理续作任务
     *
     * <p>S5.3 续作任务排产流程：
     * <ol>
     *   <li>S5.3.1 分配胎胚库存：按硫化需求占比分配</li>
     *   <li>S5.3.2 计算待排产量：(日硫化量 - 库存) × (1 + 损耗率) + 异常平摊</li>
     *   <li>S5.3.3 开停产特殊处理</li>
     *   <li>S5.3.4 收尾余量处理：主要产品补到一整车</li>
     *   <li>S5.3.5 补做逻辑：延误检测和补做计算</li>
     * </ol>
     *
     * @param continueTasks    续作任务列表
     * @param context          排程上下文
     * @param scheduleDate     排程日期
     * @param dayShifts        班次配置
     * @param day              排程天数
     * @return 机台分配结果列表
     */
    public List<CoreScheduleAlgorithmService.MachineAllocationResult> processContinueTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> continueTasks,
            ScheduleContextDTO context,
            LocalDate scheduleDate,
            List<CxShiftConfig> dayShifts,
            int day) {

        List<CoreScheduleAlgorithmService.MachineAllocationResult> results = new ArrayList<>();

        if (continueTasks.isEmpty()) {
            return results;
        }

        // 按机台分组
        Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> machineTaskMap = groupTasksByMachine(continueTasks);

        // 获取参数配置
        boolean isOpeningDay = Boolean.TRUE.equals(context.getIsOpeningDay()) && day == 1;
        boolean isClosingDay = Boolean.TRUE.equals(context.getIsClosingDay());

        // 处理每个机台的任务
        for (Map.Entry<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> entry : machineTaskMap.entrySet()) {
            String machineCode = entry.getKey();
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks = entry.getValue();

            // 创建机台分配结果
            CoreScheduleAlgorithmService.MachineAllocationResult allocation = createMachineAllocation(machineCode, context);

            // 处理机台上的每个任务
            for (CoreScheduleAlgorithmService.DailyEmbryoTask task : tasks) {
                // S5.3.1 分配胎胚库存
                allocateEmbryoStock(task, context, scheduleDate);
                
                // S5.3.2 计算待排产量
                calculatePlannedProduction(task, context, scheduleDate, isOpeningDay);
                
                // S5.3.3 开停产特殊处理
                handleOpeningClosingDay(task, context, dayShifts, isOpeningDay, isClosingDay);
                
                // S5.3.4 收尾余量处理
                handleEndingRemainder(task, context, isOpeningDay);
                
                // S5.3.5 计算延误量和补做（针对10天内收尾的任务）
                if (Boolean.TRUE.equals(task.getIsNearEnding()) && !isOpeningDay) {
                    int catchUpQty = calculateCatchUpQuantity(task, context, scheduleDate);
                    if (catchUpQty > 0) {
                        int tripCapacity = getTripCapacity(task.getStructureName(), context);
                        int catchUpTrips = convertToTrips(catchUpQty, tripCapacity, task.getIsMainProduct());
                        task.setCatchUpQuantity(catchUpTrips * tripCapacity);
                        log.info("续作任务补做：胎胚={}, 原需求={}, 补做量={}, 车数={}",
                                task.getMaterialCode(), task.getDemandQuantity(), 
                                task.getCatchUpQuantity(), catchUpTrips);
                        // 更新待排产量
                        task.setPlannedProduction(task.getPlannedProduction() + task.getCatchUpQuantity());
                    }
                }

                // 分配任务到机台
                if (task.getPlannedProduction() != null && task.getPlannedProduction() > 0) {
                    allocateTaskToMachine(allocation, task, context);
                }
            }

            // 如果机台有任务收尾了，需要考虑均衡
            boolean hasEndingTask = tasks.stream()
                    .anyMatch(t -> Boolean.TRUE.equals(t.getIsEndingTask()));
            if (hasEndingTask) {
                balanceMachineWithEndingTask(allocation, tasks, context);
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
    private Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> groupTasksByMachine(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> continueTasks) {
        Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> machineTaskMap = new LinkedHashMap<>();
        
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : continueTasks) {
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
     * 创建机台分配结果
     */
    private CoreScheduleAlgorithmService.MachineAllocationResult createMachineAllocation(
            String machineCode, ScheduleContextDTO context) {
        CoreScheduleAlgorithmService.MachineAllocationResult allocation = new CoreScheduleAlgorithmService.MachineAllocationResult();
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
     * S5.3.1 分配胎胚库存
     * 
     * <p>同一个胎胚可能对应多个硫化SKU（规格），按当天硫化需求的占比分配库存
     * <p>公式：（这个SKU的日硫化量 ÷ 同胎胚所有SKU的日硫化量之和） × 胎胚库存总量
     *
     * @param task         任务
     * @param context      排程上下文
     * @param scheduleDate 排程日期
     */
    public void allocateEmbryoStock(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextDTO context,
            LocalDate scheduleDate) {
        
        String embryoCode = task.getMaterialCode();
        
        // 获取当前胎胚库存
        int totalEmbryoStock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;
        if (totalEmbryoStock <= 0) {
            task.setAllocatedStock(0);
            return;
        }
        
        // 获取该胎胚对应的所有硫化SKU需求
        List<LhScheduleResult> lhResults = context.getLhScheduleResults();
        if (lhResults == null || lhResults.isEmpty()) {
            task.setAllocatedStock(totalEmbryoStock);
            return;
        }
        
        // 按胎胚编码分组，获取同胎胚的所有SKU
        Map<String, List<LhScheduleResult>> embryoSkuMap = lhResults.stream()
                .filter(r -> embryoCode.equals(r.getEmbryoCode()))
                .collect(Collectors.groupingBy(LhScheduleResult::getEmbryoCode));
        
        List<LhScheduleResult> sameEmbryoResults = embryoSkuMap.get(embryoCode);
        if (sameEmbryoResults == null || sameEmbryoResults.isEmpty()) {
            task.setAllocatedStock(totalEmbryoStock);
            return;
        }
        
        // 计算同胎胚所有SKU的日硫化量之和
        int totalDailyVulcanize = sameEmbryoResults.stream()
                .mapToInt(r -> r.getDailyPlanQty() != null ? r.getDailyPlanQty() : 0)
                .sum();
        
        if (totalDailyVulcanize <= 0) {
            task.setAllocatedStock(totalEmbryoStock);
            return;
        }
        
        // 计算当前SKU的日硫化量
        int currentSkuDailyVulcanize = task.getVulcanizeDemand() != null ? task.getVulcanizeDemand() : 0;
        
        // 按比例分配库存
        int allocatedStock;
        if (sameEmbryoResults.size() == 1) {
            allocatedStock = totalEmbryoStock;
        } else {
            allocatedStock = (int) ((double) currentSkuDailyVulcanize / totalDailyVulcanize * totalEmbryoStock);
        }
        
        task.setAllocatedStock(allocatedStock);
        log.debug("胎胚 {} 库存分配：总库存={}, 分配量={}", embryoCode, totalEmbryoStock, allocatedStock);
    }
    
    /**
     * S5.3.2 计算待排产量
     * 
     * <p>待排产量 = (SKU日硫化量 - 分到的胎胚库存) × (1 + 损耗率) + 异常平摊的量
     * <p>检查约束：不能超过机台最大产能，也不能超过胎胚库容的90%
     *
     * @param task         任务
     * @param context      排程上下文
     * @param scheduleDate 排程日期
     * @param isOpeningDay 是否开产日
     */
    public void calculatePlannedProduction(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextDTO context,
            LocalDate scheduleDate,
            boolean isOpeningDay) {
        
        // 获取参数
        BigDecimal lossRate = context.getLossRate();
        if (lossRate == null) {
            lossRate = new BigDecimal("0.02"); // 默认2%损耗率
        }
        
        // SKU日硫化量
        int dailyVulcanize = task.getVulcanizeDemand() != null ? task.getVulcanizeDemand() : 0;
        
        // 分到的胎胚库存
        int allocatedStock = task.getAllocatedStock() != null ? task.getAllocatedStock() : 0;
        
        // 异常平摊量（之前计算的补做量）
        int exceptionAllocation = task.getCatchUpQuantity() != null ? task.getCatchUpQuantity() : 0;
        
        // 待排产量 = (日硫化量 - 库存) × (1 + 损耗率) + 异常平摊
        int baseProduction = Math.max(0, dailyVulcanize - allocatedStock);
        int plannedProduction = (int) Math.ceil(baseProduction * (1 + lossRate.doubleValue())) + exceptionAllocation;
        
        // 获取机台最大产能限制
        int machineMaxCapacity = getMachineDailyCapacity(
                task.getContinueMachineCodes() != null && !task.getContinueMachineCodes().isEmpty() 
                        ? task.getContinueMachineCodes().get(0) : null, context);
        
        // 胎胚库容限制（90%）
        int embryoStorageLimit = (int) (getEmbryoStorageLimit(task.getMaterialCode(), context) * EMBRYO_STORAGE_RATIO);
        
        // 取最小值
        plannedProduction = Math.min(plannedProduction, machineMaxCapacity);
        plannedProduction = Math.min(plannedProduction, embryoStorageLimit);
        
        // 确保非负
        plannedProduction = Math.max(0, plannedProduction);
        
        task.setPlannedProduction(plannedProduction);
        
        log.debug("任务 {} 待排产量计算：日硫化量={}, 分配库存={}, 待排产量={}",
                task.getMaterialCode(), dailyVulcanize, allocatedStock, plannedProduction);
    }
    
    /**
     * S5.3.3 开停产特殊处理
     *
     * @param task         任务
     * @param context      排程上下文
     * @param dayShifts    班次配置
     * @param isOpeningDay 是否开产日
     * @param isClosingDay 是否停产日
     */
    public void handleOpeningClosingDay(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextDTO context,
            List<CxShiftConfig> dayShifts,
            boolean isOpeningDay,
            boolean isClosingDay) {
        
        if (isClosingDay) {
            // 停产：库存归0，不排产
            task.setPlannedProduction(0);
            task.setIsClosingDayTask(true);
            log.info("停产日任务 {} 不排产", task.getMaterialCode());
            return;
        }
        
        if (isOpeningDay) {
            // 开产：首班只排6小时
            int hourlyCapacity = getMachineHourlyCapacity(
                    task.getContinueMachineCodes() != null && !task.getContinueMachineCodes().isEmpty()
                            ? task.getContinueMachineCodes().get(0) : null,
                    task.getStructureName(), context);
            
            int openingShiftCapacity = hourlyCapacity * OPENING_SHIFT_HOURS;
            
            // 检查是否是关键产品
            boolean isKeyProduct = context.getKeyProductCodes() != null 
                    && context.getKeyProductCodes().contains(task.getMaterialCode());
            
            if (isKeyProduct) {
                // 关键产品在结构切换开产日首班不排（不做首件），从第二班开始
                task.setIsKeyProductOnOpening(true);
                task.setOpeningShiftCapacity(0);
                log.info("开产首班关键产品 {} 不排产，从第二班开始", task.getMaterialCode());
            } else {
                task.setOpeningShiftCapacity(openingShiftCapacity);
                int originalPlanned = task.getPlannedProduction() != null ? task.getPlannedProduction() : 0;
                task.setPlannedProduction(Math.min(originalPlanned, openingShiftCapacity));
            }
            
            task.setIsOpeningDayTask(true);
        }
    }
    
    /**
     * S5.3.4 收尾余量处理
     * 
     * <p>如果这个任务当天要收尾，且本次排产是收尾的最后一批：
     * <ul>
     *   <li>主要产品：把剩余量补到一整车</li>
     *   <li>非主要产品：按实际剩余量下，不强行凑整</li>
     * </ul>
     *
     * @param task         任务
     * @param context      排程上下文
     * @param isOpeningDay 是否开产日
     */
    public void handleEndingRemainder(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextDTO context,
            boolean isOpeningDay) {
        
        // 开产日不处理收尾余量
        if (isOpeningDay) {
            return;
        }
        
        // 检查是否是收尾任务
        if (!Boolean.TRUE.equals(task.getIsEndingTask()) && !Boolean.TRUE.equals(task.getIsNearEnding())) {
            return;
        }
        
        // 成型余量
        Integer endingSurplus = task.getEndingSurplusQty();
        if (endingSurplus == null || endingSurplus <= 0) {
            return;
        }
        
        // 获取整车容量
        int tripCapacity = getTripCapacity(task.getStructureName(), context);
        
        // 当前待排产量
        int plannedProduction = task.getPlannedProduction() != null ? task.getPlannedProduction() : 0;
        
        // 判断是否是收尾的最后一批
        int allocatedStock = task.getAllocatedStock() != null ? task.getAllocatedStock() : 0;
        boolean isLastBatch = (plannedProduction + allocatedStock) >= endingSurplus;
        
        if (isLastBatch) {
            boolean isMainProduct = Boolean.TRUE.equals(task.getIsMainProduct());
            
            if (isMainProduct) {
                // 主要产品：补到一整车
                int remainder = plannedProduction % tripCapacity;
                if (remainder > 0) {
                    int addToFullTrip = tripCapacity - remainder;
                    task.setPlannedProduction(plannedProduction + addToFullTrip);
                    log.info("主要产品收尾补整车：胎胚={}, 原计划={}, 补充={}, 最终={}",
                            task.getMaterialCode(), plannedProduction, addToFullTrip, task.getPlannedProduction());
                }
            }
            
            task.setIsLastEndingBatch(true);
        }
    }
    
    /**
     * S5.3.5 计算延误量和补做量
     *
     * <p>比较成型余量与月计划当前日期到收尾日的计划量汇总
     *
     * @param task         任务
     * @param context      排程上下文
     * @param scheduleDate 排程日期
     * @return 需要补做的量（条）
     */
    public int calculateCatchUpQuantity(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
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
        // TODO: 需要从 context 获取月计划数据
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
            return quantity / tripCapacity;
        }
    }

    /**
     * 获取结构的整车容量
     */
    private int getTripCapacity(String structureCode, ScheduleContextDTO context) {
        if (context.getStructureShiftCapacities() != null) {
            for (CxStructureShiftCapacity capacity : context.getStructureShiftCapacities()) {
                if (capacity.getStructureCode() != null &&
                        capacity.getStructureCode().equals(structureCode)) {
                    if (capacity.getTripQty() != null && capacity.getTripQty() > 0) {
                        return capacity.getTripQty();
                    }
                }
            }
        }
        return context.getDefaultTripCapacity() != null
                ? context.getDefaultTripCapacity()
                : DEFAULT_TRIP_CAPACITY;
    }

    /**
     * 获取机台小时产能
     */
    private int getMachineHourlyCapacity(String machineCode, String structureName, ScheduleContextDTO context) {
        // 优先从机台结构产能表获取
        if (context.getMachineStructureCapacities() != null && machineCode != null && structureName != null) {
            for (var capacity : context.getMachineStructureCapacities()) {
                if (machineCode.equals(capacity.getCxMachineCode()) 
                        && structureName.equals(capacity.getStructureCode())) {
                    return capacity.getHourlyCapacity() != null ? capacity.getHourlyCapacity() : 50;
                }
            }
        }
        // 使用默认值
        return context.getMachineHourlyCapacity() != null ? context.getMachineHourlyCapacity() : 50;
    }

    /**
     * 获取胎胚库容限制
     */
    private int getEmbryoStorageLimit(String materialCode, ScheduleContextDTO context) {
        // TODO: 从配置或库存表获取胎胚库容限制
        return Integer.MAX_VALUE;
    }

    /**
     * 机台有收尾任务时的均衡处理
     */
    private void balanceMachineWithEndingTask(
            CoreScheduleAlgorithmService.MachineAllocationResult allocation,
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks,
            ScheduleContextDTO context) {
        // TODO: 实现机台均衡逻辑
        log.debug("机台 {} 有收尾任务，执行均衡处理", allocation.getMachineCode());
    }

    /**
     * 分配任务到机台
     */
    private void allocateTaskToMachine(
            CoreScheduleAlgorithmService.MachineAllocationResult allocation,
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextDTO context) {

        int quantity = task.getPlannedProduction() != null && task.getPlannedProduction() > 0 
                ? task.getPlannedProduction() 
                : task.getDemandQuantity();

        CoreScheduleAlgorithmService.TaskAllocation taskAllocation = new CoreScheduleAlgorithmService.TaskAllocation();
        taskAllocation.setMaterialCode(task.getMaterialCode());
        taskAllocation.setMaterialName(task.getMaterialName());
        taskAllocation.setStructureName(task.getStructureName());
        taskAllocation.setQuantity(quantity);
        taskAllocation.setPriority(task.getPriority());
        taskAllocation.setStockHours(task.getStockHours());
        taskAllocation.setIsTrialTask(task.getIsTrialTask());
        taskAllocation.setIsEndingTask(task.getIsEndingTask());
        taskAllocation.setEndingSurplusQty(task.getEndingSurplusQty());
        taskAllocation.setIsMainProduct(task.getIsMainProduct());

        allocation.getTaskAllocations().add(taskAllocation);
        allocation.setUsedCapacity(allocation.getUsedCapacity() + quantity);
        allocation.setRemainingCapacity(allocation.getRemainingCapacity() - quantity);
    }
}
