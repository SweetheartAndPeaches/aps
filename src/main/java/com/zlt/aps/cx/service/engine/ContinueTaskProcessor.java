package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.config.CxStructureShiftCapacity;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
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
 *   <li>按结构分组任务</li>
 *   <li>使用 {@link BalancingService} 均衡分配胎胚到机台</li>
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

    private final BalancingService balancingService;

    /** 开产首班排产时长（小时） */
    private static final int OPENING_SHIFT_HOURS = 6;
    
    /** 胎胚库容上限比例 */
    private static final double EMBRYO_STORAGE_RATIO = 0.9;
    
    /** 默认整车容量 */
    private static final int DEFAULT_TRIP_CAPACITY = 12;

    /** 参数编码：强制保留历史任务 */
    private static final String PARAM_FORCE_KEEP_HISTORY = "FORCE_KEEP_HISTORY_TASK";

    /**
     * 处理续作任务
     */
    public List<CoreScheduleAlgorithmService.MachineAllocationResult> processContinueTasks(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> continueTasks,
            ScheduleContextDTO context,
            LocalDate scheduleDate,
            List<CxShiftConfig> dayShifts,
            int day) {

        List<CoreScheduleAlgorithmService.MachineAllocationResult> results = new ArrayList<>();

        if (CollectionUtils.isEmpty(continueTasks)) {
            return results;
        }

        log.info("========== 开始处理续作任务，共 {} 个任务 ==========", continueTasks.size());

        // Step 1: 按结构分组任务
        Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> structureTaskMap = 
                groupTasksByStructure(continueTasks);
        log.info("按结构分组完成，共 {} 个结构", structureTaskMap.size());

        // Step 2: 获取是否强制保留历史任务
        boolean forceKeepHistory = getForceKeepHistoryConfig(context);
        log.info("强制保留历史任务配置: {}", forceKeepHistory);

        // Step 3: 构建历史任务映射
        Map<String, Set<String>> machineHistoryMap = buildMachineHistoryMap(context);
        log.info("构建历史任务映射完成，共 {} 台机台有历史记录", machineHistoryMap.size());

        // Step 4: 按结构处理每个分组
        for (Map.Entry<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> entry : structureTaskMap.entrySet()) {
            String structureName = entry.getKey();
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks = entry.getValue();

            log.info("--- 处理结构 {}，共 {} 个胎胚 ---", structureName, tasks.size());

            // 获取该结构可分配的机台列表
            List<MpCxCapacityConfiguration> availableMachines = getAvailableMachinesForStructure(
                    structureName, scheduleDate, context);
            
            if (availableMachines.isEmpty()) {
                log.warn("结构 {} 没有可分配的机台，跳过", structureName);
                continue;
            }

            // 获取结构的产能限制配置
            MdmStructureLhRatio ratioConfig = context.getStructureLhRatioMap() != null 
                    ? context.getStructureLhRatioMap().get(structureName) : null;
            
            int maxLhMachines = ratioConfig != null && ratioConfig.getLhMachineMaxQty() != null 
                    ? ratioConfig.getLhMachineMaxQty() : 10;
            int maxEmbryoTypes = ratioConfig != null && ratioConfig.getMaxEmbryoQty() != null 
                    ? ratioConfig.getMaxEmbryoQty() : 4;

            // Step 5: 使用 BalancingService 均衡分配
            BalancingService.BalancingResult balancingResult = balancingService.balanceEmbryosToMachines(
                    tasks, availableMachines, machineHistoryMap,
                    maxLhMachines, maxEmbryoTypes, forceKeepHistory, context);

            // Step 6: 为每个机台分配计划量
            for (BalancingService.MachineAssignment assignment : balancingResult.getAssignments()) {
                CoreScheduleAlgorithmService.MachineAllocationResult allocation = createMachineAllocation(
                        assignment.getMachineCode(), context);

                for (BalancingService.EmbryoAssignment embryoAssignment : assignment.getEmbryoAssignments()) {
                    CoreScheduleAlgorithmService.DailyEmbryoTask task = embryoAssignment.getTask();
                    
                    // S5.3.1 分配胎胚库存
                    allocateEmbryoStock(task, context, scheduleDate);
                    
                    // S5.3.2 计算待排产量
                    boolean isOpeningDay = Boolean.TRUE.equals(context.getIsOpeningDay()) && day == 1;
                    calculatePlannedProduction(task, context, scheduleDate, isOpeningDay);
                    
                    // S5.3.3 开停产特殊处理
                    boolean isClosingDay = Boolean.TRUE.equals(context.getIsClosingDay());
                    handleOpeningClosingDay(task, context, dayShifts, isOpeningDay, isClosingDay);
                    
                    // S5.3.4 收尾余量处理
                    handleEndingRemainder(task, context, isOpeningDay);
                    
                    // S5.3.5 计算延误量和补做
                    if (Boolean.TRUE.equals(task.getIsNearEnding()) && !isOpeningDay) {
                        int catchUpQty = calculateCatchUpQuantity(task, context, scheduleDate);
                        if (catchUpQty > 0) {
                            int tripCapacity = getTripCapacity(task.getStructureName(), context);
                            int catchUpTrips = convertToTrips(catchUpQty, tripCapacity, task.getIsMainProduct());
                            task.setCatchUpQuantity(catchUpTrips * tripCapacity);
                            task.setPlannedProduction(task.getPlannedProduction() + task.getCatchUpQuantity());
                        }
                    }

                    // 分配任务到机台
                    if (task.getPlannedProduction() != null && task.getPlannedProduction() > 0) {
                        allocateTaskToMachine(allocation, task, context);
                    }
                }

                if (!allocation.getTaskAllocations().isEmpty()) {
                    results.add(allocation);
                }
            }
        }

        log.info("========== 续作任务处理完成，共 {} 台机台分配任务 ==========", results.size());
        return results;
    }

    // ==================== 辅助方法 ====================

    private Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> groupTasksByStructure(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks) {
        return tasks.stream()
                .filter(t -> t.getStructureName() != null)
                .collect(Collectors.groupingBy(
                        CoreScheduleAlgorithmService.DailyEmbryoTask::getStructureName,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private boolean getForceKeepHistoryConfig(ScheduleContextDTO context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_FORCE_KEEP_HISTORY);
            if (config != null && config.getParamValue() != null) {
                return "1".equals(config.getParamValue()) || "true".equalsIgnoreCase(config.getParamValue());
            }
        }
        return false;
    }

    private Map<String, Set<String>> buildMachineHistoryMap(ScheduleContextDTO context) {
        Map<String, Set<String>> historyMap = new HashMap<>();
        if (context.getMachineOnlineEmbryoMap() != null) {
            historyMap.putAll(context.getMachineOnlineEmbryoMap());
        }
        return historyMap;
    }

    private List<MpCxCapacityConfiguration> getAvailableMachinesForStructure(
            String structureName, LocalDate scheduleDate, ScheduleContextDTO context) {
        if (context.getStructureAllocationMap() != null) {
            List<MpCxCapacityConfiguration> configs = context.getStructureAllocationMap().get(structureName);
            if (configs != null && !configs.isEmpty()) {
                int day = scheduleDate.getDayOfMonth();
                return configs.stream()
                        .filter(c -> c.getBeginDay() != null && c.getEndDay() != null)
                        .filter(c -> c.getBeginDay() <= day && c.getEndDay() >= day)
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }

    // ==================== 原有方法 ====================

    private CoreScheduleAlgorithmService.MachineAllocationResult createMachineAllocation(
            String machineCode, ScheduleContextDTO context) {
        CoreScheduleAlgorithmService.MachineAllocationResult allocation = new CoreScheduleAlgorithmService.MachineAllocationResult();
        allocation.setMachineCode(machineCode);
        allocation.setTaskAllocations(new ArrayList<>());
        allocation.setUsedCapacity(0);
        allocation.setRemainingCapacity(getMachineDailyCapacity(machineCode, context));
        return allocation;
    }

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

    public void allocateEmbryoStock(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextDTO context,
            LocalDate scheduleDate) {
        
        String embryoCode = task.getMaterialCode();
        int totalEmbryoStock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;
        if (totalEmbryoStock <= 0) {
            task.setAllocatedStock(0);
            return;
        }
        
        List<LhScheduleResult> lhResults = context.getLhScheduleResults();
        if (lhResults == null || lhResults.isEmpty()) {
            task.setAllocatedStock(totalEmbryoStock);
            return;
        }
        
        List<LhScheduleResult> sameEmbryoResults = lhResults.stream()
                .filter(r -> embryoCode.equals(r.getEmbryoCode()))
                .collect(Collectors.toList());
        
        if (sameEmbryoResults.isEmpty()) {
            task.setAllocatedStock(totalEmbryoStock);
            return;
        }
        
        int totalDailyVulcanize = sameEmbryoResults.stream()
                .mapToInt(r -> r.getDailyPlanQty() != null ? r.getDailyPlanQty() : 0)
                .sum();
        
        if (totalDailyVulcanize <= 0) {
            task.setAllocatedStock(totalEmbryoStock);
            return;
        }
        
        int currentSkuDailyVulcanize = task.getVulcanizeDemand() != null ? task.getVulcanizeDemand() : 0;
        
        int allocatedStock;
        if (sameEmbryoResults.size() == 1) {
            allocatedStock = totalEmbryoStock;
        } else {
            allocatedStock = (int) ((double) currentSkuDailyVulcanize / totalDailyVulcanize * totalEmbryoStock);
        }
        
        task.setAllocatedStock(allocatedStock);
        log.debug("胎胚 {} 库存分配：总库存={}, 分配量={}", embryoCode, totalEmbryoStock, allocatedStock);
    }
    
    public void calculatePlannedProduction(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextDTO context,
            LocalDate scheduleDate,
            boolean isOpeningDay) {
        
        BigDecimal lossRate = context.getLossRate() != null ? context.getLossRate() : new BigDecimal("0.02");
        
        int dailyVulcanize = task.getVulcanizeDemand() != null ? task.getVulcanizeDemand() : 0;
        int allocatedStock = task.getAllocatedStock() != null ? task.getAllocatedStock() : 0;
        int exceptionAllocation = task.getCatchUpQuantity() != null ? task.getCatchUpQuantity() : 0;
        
        int baseProduction = Math.max(0, dailyVulcanize - allocatedStock);
        int plannedProduction = (int) Math.ceil(baseProduction * (1 + lossRate.doubleValue())) + exceptionAllocation;
        
        int machineMaxCapacity = getMachineDailyCapacity(
                task.getContinueMachineCodes() != null && !task.getContinueMachineCodes().isEmpty() 
                        ? task.getContinueMachineCodes().get(0) : null, context);
        
        int embryoStorageLimit = (int) (getEmbryoStorageLimit(task.getMaterialCode(), context) * EMBRYO_STORAGE_RATIO);
        
        plannedProduction = Math.min(plannedProduction, machineMaxCapacity);
        plannedProduction = Math.min(plannedProduction, embryoStorageLimit);
        plannedProduction = Math.max(0, plannedProduction);
        
        task.setPlannedProduction(plannedProduction);
    }
    
    public void handleOpeningClosingDay(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextDTO context,
            List<CxShiftConfig> dayShifts,
            boolean isOpeningDay,
            boolean isClosingDay) {
        
        if (isClosingDay) {
            task.setPlannedProduction(0);
            task.setIsClosingDayTask(true);
            return;
        }
        
        if (isOpeningDay) {
            int hourlyCapacity = getMachineHourlyCapacity(
                    task.getContinueMachineCodes() != null && !task.getContinueMachineCodes().isEmpty()
                            ? task.getContinueMachineCodes().get(0) : null,
                    task.getStructureName(), context);
            
            int openingShiftCapacity = hourlyCapacity * OPENING_SHIFT_HOURS;
            
            boolean isKeyProduct = context.getKeyProductCodes() != null 
                    && context.getKeyProductCodes().contains(task.getMaterialCode());
            
            if (isKeyProduct) {
                task.setIsKeyProductOnOpening(true);
                task.setOpeningShiftCapacity(0);
            } else {
                task.setOpeningShiftCapacity(openingShiftCapacity);
                int originalPlanned = task.getPlannedProduction() != null ? task.getPlannedProduction() : 0;
                task.setPlannedProduction(Math.min(originalPlanned, openingShiftCapacity));
            }
            
            task.setIsOpeningDayTask(true);
        }
    }
    
    public void handleEndingRemainder(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextDTO context,
            boolean isOpeningDay) {
        
        if (isOpeningDay) return;
        if (!Boolean.TRUE.equals(task.getIsEndingTask()) && !Boolean.TRUE.equals(task.getIsNearEnding())) return;
        
        Integer endingSurplus = task.getEndingSurplusQty();
        if (endingSurplus == null || endingSurplus <= 0) return;
        
        int tripCapacity = getTripCapacity(task.getStructureName(), context);
        int plannedProduction = task.getPlannedProduction() != null ? task.getPlannedProduction() : 0;
        int allocatedStock = task.getAllocatedStock() != null ? task.getAllocatedStock() : 0;
        
        if ((plannedProduction + allocatedStock) >= endingSurplus) {
            if (Boolean.TRUE.equals(task.getIsMainProduct())) {
                int remainder = plannedProduction % tripCapacity;
                if (remainder > 0) {
                    task.setPlannedProduction(plannedProduction + (tripCapacity - remainder));
                }
            }
            task.setIsLastEndingBatch(true);
        }
    }
    
    public int calculateCatchUpQuantity(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextDTO context,
            LocalDate scheduleDate) {

        Integer formingRemainder = task.getEndingSurplusQty();
        if (formingRemainder == null || formingRemainder <= 0) return 0;

        LocalDate endingDate = task.getEndingDate();
        if (endingDate == null) return 0;

        int plannedQty = calculatePlannedQuantityToDate(task.getMaterialCode(), scheduleDate, endingDate, context);
        int gap = formingRemainder - plannedQty;
        
        return gap > 0 ? gap : 0;
    }

    private int calculatePlannedQuantityToDate(String materialCode, LocalDate startDate, LocalDate endDate, ScheduleContextDTO context) {
        return 0; // TODO
    }

    private int convertToTrips(int quantity, int tripCapacity, Boolean isMainProduct) {
        if (quantity <= 0) return 0;
        return Boolean.TRUE.equals(isMainProduct) 
                ? (int) Math.ceil((double) quantity / tripCapacity) 
                : quantity / tripCapacity;
    }

    private int getTripCapacity(String structureCode, ScheduleContextDTO context) {
        if (context.getStructureShiftCapacities() != null) {
            for (CxStructureShiftCapacity capacity : context.getStructureShiftCapacities()) {
                if (structureCode.equals(capacity.getStructureCode()) && capacity.getTripQty() != null && capacity.getTripQty() > 0) {
                    return capacity.getTripQty();
                }
            }
        }
        return context.getDefaultTripCapacity() != null ? context.getDefaultTripCapacity() : DEFAULT_TRIP_CAPACITY;
    }

    private int getMachineHourlyCapacity(String machineCode, String structureName, ScheduleContextDTO context) {
        if (context.getMachineStructureCapacities() != null && machineCode != null && structureName != null) {
            for (var capacity : context.getMachineStructureCapacities()) {
                if (machineCode.equals(capacity.getCxMachineCode()) && structureName.equals(capacity.getStructureCode())) {
                    return capacity.getHourlyCapacity() != null ? capacity.getHourlyCapacity() : 50;
                }
            }
        }
        return context.getMachineHourlyCapacity() != null ? context.getMachineHourlyCapacity() : 50;
    }

    private int getEmbryoStorageLimit(String materialCode, ScheduleContextDTO context) {
        return Integer.MAX_VALUE;
    }

    private void allocateTaskToMachine(
            CoreScheduleAlgorithmService.MachineAllocationResult allocation,
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextDTO context) {

        int quantity = task.getPlannedProduction() != null && task.getPlannedProduction() > 0 
                ? task.getPlannedProduction() : task.getDemandQuantity();

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
