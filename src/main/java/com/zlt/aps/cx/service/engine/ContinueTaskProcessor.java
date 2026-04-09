package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.entity.CxMachineStructureCapacity;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mdm.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mdm.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.mdm.api.domain.entity.MpCxCapacityConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
    private final ProductionCalculator productionCalculator;

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
            ScheduleContextVo context,
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

            // 构建机台编码 -> 最大硫化机数 映射（根据每台机台的机型+结构获取）
            Map<String, Integer> machineMaxLhMap = buildMachineMaxLhMap(availableMachines, structureName, context);

            // 构建机台编码 -> 最大胎胚种类数 映射（根据每台机台的机型+结构获取）
            Map<String, Integer> machineMaxEmbryoTypesMap = buildMachineMaxEmbryoTypesMap(availableMachines, structureName, context);

            // Step 5: 使用 BalancingService 均衡分配（使用每台机台各自的最大硫化机数和最大胎胚种类数）
            BalancingService.BalancingResult balancingResult = balancingService.balanceEmbryosToMachinesWithMachineCapacity(
                    tasks, availableMachines, machineHistoryMap,
                    machineMaxLhMap, machineMaxEmbryoTypesMap, forceKeepHistory, context);

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

    private boolean getForceKeepHistoryConfig(ScheduleContextVo context) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(PARAM_FORCE_KEEP_HISTORY);
            if (config != null && config.getParamValue() != null) {
                return "1".equals(config.getParamValue()) || "true".equalsIgnoreCase(config.getParamValue());
            }
        }
        return false;
    }

    private Map<String, Set<String>> buildMachineHistoryMap(ScheduleContextVo context) {
        Map<String, Set<String>> historyMap = new HashMap<>();
        if (context.getMachineOnlineEmbryoMap() != null) {
            historyMap.putAll(context.getMachineOnlineEmbryoMap());
        }
        return historyMap;
    }

    private List<MpCxCapacityConfiguration> getAvailableMachinesForStructure(
            String structureName, LocalDate scheduleDate, ScheduleContextVo context) {
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

    /**
     * 构建机台最大硫化机数映射
     *
     * <p>根据每台机台的机型 + 结构，从 MdmStructureLhRatio 获取对应的最大硫化机数
     *
     * @param machineConfigs  机台配置列表
     * @param structureName   结构名称
     * @param context         排程上下文
     * @return 机台编码 -> 最大硫化机数
     */
    private Map<String, Integer> buildMachineMaxLhMap(
            List<MpCxCapacityConfiguration> machineConfigs,
            String structureName,
            ScheduleContextVo context) {

        Map<String, Integer> result = new HashMap<>();

        // 构建机台编码 -> 机型 映射
        Map<String, String> machineTypeMap = new HashMap<>();
        if (context.getAvailableMachines() != null) {
            for (MdmMoldingMachine machine : context.getAvailableMachines()) {
                machineTypeMap.put(machine.getCxMachineCode(), machine.getCxMachineTypeCode());
            }
        }

        // 构建 机型_结构 -> 最大硫化机数 映射
        Map<String, Integer> typeStructureMap = new HashMap<>();
        List<MdmStructureLhRatio> ratios = context.getStructureLhRatios();
        if (ratios != null) {
            for (MdmStructureLhRatio ratio : ratios) {
                String key = ratio.getCxMachineTypeCode() + "_" + ratio.getStructureName();
                if (ratio.getLhMachineMaxQty() != null) {
                    typeStructureMap.put(key, ratio.getLhMachineMaxQty());
                }
            }
        }

        // 为每台机台获取对应的最大硫化机数
        for (MpCxCapacityConfiguration config : machineConfigs) {
            String machineCode = config.getCxMachineCode();
            String machineType = machineTypeMap.get(machineCode);

            // 根据机型+结构查找
            String key = machineType + "_" + structureName;
            Integer maxLh = typeStructureMap.get(key);

            // 如果找不到，尝试只按结构查找（向后兼容）
            if (maxLh == null && context.getStructureLhRatioMap() != null) {
                MdmStructureLhRatio ratioConfig = context.getStructureLhRatioMap().get(structureName);
                if (ratioConfig != null && ratioConfig.getLhMachineMaxQty() != null) {
                    maxLh = ratioConfig.getLhMachineMaxQty();
                }
            }

            // 兜底：使用默认值
            if (maxLh == null) {
                maxLh = 10;
                log.debug("机台 {} 机型 {} 结构 {} 未找到配比配置，使用默认值 10",
                        machineCode, machineType, structureName);
            }

            result.put(machineCode, maxLh);
        }

        log.info("结构 {} 机台最大硫化机数映射：{}", structureName, result);
        return result;
    }

    /**
     * 构建机台最大胎胚种类数映射
     *
     * <p>根据每台机台的机型 + 结构，从 MdmStructureLhRatio 获取对应的最大胎胚种类数
     *
     * @param machineConfigs  机台配置列表
     * @param structureName   结构名称
     * @param context         排程上下文
     * @return 机台编码 -> 最大胎胚种类数
     */
    private Map<String, Integer> buildMachineMaxEmbryoTypesMap(
            List<MpCxCapacityConfiguration> machineConfigs,
            String structureName,
            ScheduleContextVo context) {

        Map<String, Integer> result = new HashMap<>();

        // 构建机台编码 -> 机型 映射
        Map<String, String> machineTypeMap = new HashMap<>();
        if (context.getAvailableMachines() != null) {
            for (MdmMoldingMachine machine : context.getAvailableMachines()) {
                machineTypeMap.put(machine.getCxMachineCode(), machine.getCxMachineTypeCode());
            }
        }

        // 构建 机型_结构 -> 最大胎胚种类数 映射
        Map<String, Integer> typeStructureMap = new HashMap<>();
        List<MdmStructureLhRatio> ratios = context.getStructureLhRatios();
        if (ratios != null) {
            for (MdmStructureLhRatio ratio : ratios) {
                String key = ratio.getCxMachineTypeCode() + "_" + ratio.getStructureName();
                if (ratio.getMaxEmbryoQty() != null) {
                    typeStructureMap.put(key, ratio.getMaxEmbryoQty());
                }
            }
        }

        // 为每台机台获取对应的最大胎胚种类数
        for (MpCxCapacityConfiguration config : machineConfigs) {
            String machineCode = config.getCxMachineCode();
            String machineType = machineTypeMap.get(machineCode);

            // 根据机型+结构查找
            String key = machineType + "_" + structureName;
            Integer maxTypes = typeStructureMap.get(key);

            // 如果找不到，尝试只按结构查找（向后兼容）
            if (maxTypes == null && context.getStructureLhRatioMap() != null) {
                MdmStructureLhRatio ratioConfig = context.getStructureLhRatioMap().get(structureName);
                if (ratioConfig != null && ratioConfig.getMaxEmbryoQty() != null) {
                    maxTypes = ratioConfig.getMaxEmbryoQty();
                }
            }

            // 兜底：使用默认值
            if (maxTypes == null) {
                maxTypes = context.getMaxTypesPerMachine() != null ? context.getMaxTypesPerMachine() : 4;
                log.debug("机台 {} 机型 {} 结构 {} 未找到胎胚种类数配置，使用默认值 {}",
                        machineCode, machineType, structureName, maxTypes);
            }

            result.put(machineCode, maxTypes);
        }

        log.info("结构 {} 机台最大胎胚种类数映射：{}", structureName, result);
        return result;
    }

    // ==================== 原有方法 ====================

    private CoreScheduleAlgorithmService.MachineAllocationResult createMachineAllocation(
            String machineCode, ScheduleContextVo context) {
        CoreScheduleAlgorithmService.MachineAllocationResult allocation = new CoreScheduleAlgorithmService.MachineAllocationResult();
        allocation.setMachineCode(machineCode);
        allocation.setTaskAllocations(new ArrayList<>());
        allocation.setUsedCapacity(0);
        allocation.setRemainingCapacity(getMachineDailyCapacity(machineCode, context));
        return allocation;
    }

    private int getMachineDailyCapacity(String machineCode, ScheduleContextVo context) {
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
            ScheduleContextVo context,
            LocalDate scheduleDate) {
        
        // 库存已在 TaskGroupService.buildSingleTask 中按物料需求比例分配好
        // 这里直接使用已分配的库存
        int allocatedStock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;
        task.setAllocatedStock(allocatedStock);
        log.debug("胎胚 {} 库存分配：分配量={}", task.getMaterialCode(), allocatedStock);
    }
    
    /**
     * 计算待排产量（按车分配）
     *
     * <p>计算逻辑：
     * 1. 获取硫化需求量（vulcanizeDemand）
     * 2. 获取硫化任务分配的库存（allocatedStock）
     * 3. 计算需要的计划量 = vulcanizeDemand - 库存
     * 4. 获取胎面整车条数（treadCount）
     * 5. 计算需要的车数 = 需要的计划量 / treadCount
     *
     * <p>如果是收尾任务，需要考虑成型余量判断是否收尾
     */
    public void calculatePlannedProduction(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextVo context,
            LocalDate scheduleDate,
            boolean isOpeningDay) {

        // Step 1: 获取硫化需求量和分配的库存
        int vulcanizeDemand = task.getVulcanizeDemand() != null ? task.getVulcanizeDemand() : 0;
        int allocatedStock = task.getAllocatedStock() != null ? task.getAllocatedStock() : 0;

        // Step 2: 计算需要的计划量
        int requiredProduction = Math.max(0, vulcanizeDemand - allocatedStock);

        // Step 3: 获取胎面整车条数
        String structureName = task.getStructureName();
        Map<String, Integer> treadCountMap = context.getStructureTreadCountMap();
        int treadCount = treadCountMap != null ? treadCountMap.getOrDefault(structureName, 1) : 1;

        // Step 4: 计算需要的车数
        int requiredCars = 0;
        if (treadCount > 0) {
            requiredCars = (int) Math.ceil((double) requiredProduction / treadCount);
        }

        // Step 5: 如果是收尾任务，判断是否需要收尾
        if (Boolean.TRUE.equals(task.getIsEndingTask())) {
            requiredCars = calculateEndingCars(task, context, requiredProduction, requiredCars);
        }

        // Step 6: 设置任务属性
        task.setPlannedProduction(requiredProduction);
        task.setRequiredCars(requiredCars);

        log.debug("任务 {} 计划量计算：需求={}，库存={}，需要量={}，胎面条数={}，需要车数={}",
                task.getMaterialCode(), vulcanizeDemand, allocatedStock, requiredProduction, treadCount, requiredCars);
    }

    /**
     * 计算收尾任务需要的车数
     *
     * <p>判断逻辑与 ProductionCalculator.handleEndingRemainder 一致：
     * - 如果成型余量充足，不收尾
     * - 如果成型余量不足，需要收尾
     */
    private int calculateEndingCars(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextVo context,
            int requiredProduction,
            int calculatedCars) {

        String materialCode = task.getRelatedMaterialCode();
        Map<String, Integer> formingRemainderMap = context.getFormingRemainderMap();
        Integer formingRemainder = formingRemainderMap != null ? formingRemainderMap.get(materialCode) : 0;

        // 判断是否需要收尾
        if (formingRemainder != null && formingRemainder > 0) {
            // 成型余量充足，不需要收尾
            log.debug("任务 {} 收尾判断：成型余量={}，充足，不收尾", task.getMaterialCode(), formingRemainder);
            return 0;
        }

        // 需要收尾，返回计算的车数
        log.debug("任务 {} 收尾判断：成型余量不足，需要收尾，车数={}", task.getMaterialCode(), calculatedCars);
        return calculatedCars;
    }
    
    public void handleOpeningClosingDay(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextVo context,
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
            
            // 关键产品判断：使用胎胚编码（task.getMaterialCode() 返回的是 embryoCode）
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
            ScheduleContextVo context,
            boolean isOpeningDay) {
        
        if (isOpeningDay) {
            return;
        }
        if (!Boolean.TRUE.equals(task.getIsEndingTask()) && !Boolean.TRUE.equals(task.getIsNearEnding())) {
            return;
        }
        
        Integer endingSurplus = task.getEndingSurplusQty();
        if (endingSurplus == null || endingSurplus <= 0) {
            return;
        }
        
        int plannedProduction = task.getPlannedProduction() != null ? task.getPlannedProduction() : 0;
        int allocatedStock = task.getAllocatedStock() != null ? task.getAllocatedStock() : 0;
        int totalPlanned = plannedProduction + allocatedStock;
        
        // 使用 ProductionCalculator 计算收尾计划量
        boolean isMainProduct = Boolean.TRUE.equals(task.getIsMainProduct());
        int remainingToProduce = Math.max(0, endingSurplus - allocatedStock);
        
        ProductionCalculator.PlanQuantityResult endingResult = productionCalculator.calculateEndingQuantity(
                remainingToProduce,
                getTripCapacity(task.getStructureName(), context),
                isMainProduct,
                task.getMaterialCode()
        );
        
        // 更新任务状态
        if (endingResult.isAbandoned()) {
            // 非主销产品余量≤2条，舍弃
            task.setPlannedProduction(0);
            task.setEndingAbandoned(true);
            task.setEndingAbandonedQty(endingResult.getAbandonedQuantity());
            log.info("收尾任务 {} 余量 {} 条被舍弃", task.getMaterialCode(), endingResult.getAbandonedQuantity());
        } else {
            // 更新计划量
            int newPlanQuantity = endingResult.getPlanQuantity();
            task.setPlannedProduction(newPlanQuantity);
            
            // 记录额外库存（主销产品多做的部分）
            if (endingResult.getExtraInventory() > 0) {
                task.setEndingExtraInventory(endingResult.getExtraInventory());
                log.info("收尾任务 {} 主销产品，多做 {} 条当库存", 
                        task.getMaterialCode(), endingResult.getExtraInventory());
            }
            
            // 标记是否为收尾最后一批
            if (totalPlanned >= endingSurplus || newPlanQuantity >= remainingToProduce) {
                task.setIsLastEndingBatch(true);
            }
        }
        
        // 保存班次分配
        if (endingResult.getShiftAllocation() != null) {
            task.setShiftAllocation(endingResult.getShiftAllocation());
        }
    }
    
    public int calculateCatchUpQuantity(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextVo context,
            LocalDate scheduleDate) {

        Integer formingRemainder = task.getEndingSurplusQty();
        if (formingRemainder == null || formingRemainder <= 0) {
            return 0;
        }

        LocalDate endingDate = task.getEndingDate();
        if (endingDate == null) {
            return 0;
        }

        int plannedQty = calculatePlannedQuantityToDate(task.getMaterialCode(), scheduleDate, endingDate, context);
        int gap = formingRemainder - plannedQty;
        
        return gap > 0 ? gap : 0;
    }

    private int calculatePlannedQuantityToDate(String materialCode, LocalDate startDate, LocalDate endDate, ScheduleContextVo context) {
        return 0; // TODO
    }

    private int convertToTrips(int quantity, int tripCapacity, Boolean isMainProduct) {
        if (quantity <= 0) {
            return 0;
        }
        return Boolean.TRUE.equals(isMainProduct) 
                ? (int) Math.ceil((double) quantity / tripCapacity) 
                : quantity / tripCapacity;
    }

    private int getTripCapacity(String structureCode, ScheduleContextVo context) {
        return productionCalculator.getTripCapacity(structureCode, context);
    }

    private int getMachineHourlyCapacity(String machineCode, String structureName, ScheduleContextVo context) {
        if (context.getMachineStructureCapacities() != null && machineCode != null && structureName != null) {
            for (CxMachineStructureCapacity capacity : context.getMachineStructureCapacities()) {
                if (machineCode.equals(capacity.getCxMachineCode()) && structureName.equals(capacity.getStructureCode())) {
                    return capacity.getHourlyCapacity() != null ? capacity.getHourlyCapacity() : 50;
                }
            }
        }
        return context.getMachineHourlyCapacity() != null ? context.getMachineHourlyCapacity() : 50;
    }

    private void allocateTaskToMachine(
            CoreScheduleAlgorithmService.MachineAllocationResult allocation,
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextVo context) {

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
