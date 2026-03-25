package com.zlt.aps.cx.service.impl;

import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.entity.*;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxStructurePriority;
import com.zlt.aps.cx.entity.config.CxStructureShiftCapacity;
import com.zlt.aps.cx.entity.mdm.*;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.mapper.CxStructureShiftCapacityMapper;
import com.zlt.aps.cx.service.CoreScheduleAlgorithmService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 实现试错分配、班次均衡、顺位排序等核心算法
 * 
 * 算法参数从 ScheduleContextDTO 获取，支持动态配置：
 * - 班次数量：context.getScheduleShiftCount()，默认8个班次
 * - 波浪比例：context.getWaveRatio()，默认 {1,2,1}
 * - 机台种类上限：context.getMaxTypesPerMachine()，默认4
 * - 默认整车容量：context.getDefaultTripCapacity()，默认12
 *
 * @author APS Team
 */
@Slf4j
@Service
public class CoreScheduleAlgorithmServiceImpl implements CoreScheduleAlgorithmService {

    /** 默认整车容量（当配置中没有时使用） */
    private static final int DEFAULT_TRIP_CAPACITY = 12;

    /** 默认机台种类上限 */
    private static final int DEFAULT_MAX_TYPES_PER_MACHINE = 4;

    /** 默认波浪比例：夜班:早班:中班 = 1:2:1 */
    private static final int[] DEFAULT_WAVE_RATIO = {1, 2, 1};

    /** 默认班次编码 */
    private static final String[] DEFAULT_SHIFT_CODES = {"SHIFT_NIGHT", "SHIFT_DAY", "SHIFT_AFTERNOON"};

    @Autowired
    private CxStructureShiftCapacityMapper structureShiftCapacityMapper;

    @Override
    public List<CxScheduleResult> executeSchedule(ScheduleContextDTO context) {
        log.info("开始执行排程算法，日期: {}", context.getScheduleDate());

        // 第一步：任务分组并计算日胎胚任务
        List<DailyEmbryoTask> tasks = calculateDailyEmbryoTasks(context);
        log.info("第一步完成，日胎胚任务数: {}", tasks.size());

        // 第二步：试错分配任务到机台
        List<MachineAllocationResult> allocations = allocateTasksToMachines(tasks, context);
        log.info("第二步完成，机台分配数: {}", allocations.size());

        // 第三步：班次均衡分配
        List<ShiftAllocationResult> shiftAllocations = balanceShiftAllocation(allocations, context);
        log.info("第三步完成，班次分配完成");

        // 第四步：排生产顺位
        List<CxScheduleDetail> details = calculateSequence(shiftAllocations, context);
        log.info("第四步完成，排程明细数: {}", details.size());

        // 构建排程结果
        return buildScheduleResults(context, allocations, shiftAllocations, details);
    }

    @Override
    public List<DailyEmbryoTask> calculateDailyEmbryoTasks(ScheduleContextDTO context) {
        List<DailyEmbryoTask> tasks = new ArrayList<>();

        // 构建物料和库存映射
        Map<String, MdmMaterialInfo> materialMap = new HashMap<>();
        if (context.getMaterials() != null) {
            materialMap = context.getMaterials().stream()
                    .collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode, m -> m, (a, b) -> a));
        }
        
        Map<String, CxStock> stockMap = new HashMap<>();
        if (context.getStocks() != null) {
            stockMap = context.getStocks().stream()
                    .collect(Collectors.toMap(CxStock::getEmbryoCode, s -> s, (a, b) -> a));
        }

        // 构建结构收尾映射
        Map<String, CxStructureEnding> endingMap = new HashMap<>();
        if (context.getStructureEndings() != null) {
            endingMap = context.getStructureEndings().stream()
                    .collect(Collectors.toMap(CxStructureEnding::getStructureCode, e -> e, (a, b) -> a));
        }

        // 获取机台在机胎胚映射（用于续作判断）
        Map<String, Set<String>> machineOnlineEmbryoMap = context.getMachineOnlineEmbryoMap();
        if (machineOnlineEmbryoMap == null) {
            machineOnlineEmbryoMap = new HashMap<>();
        }

        // ==================== 主要任务来源：硫化排程结果 ====================
        List<LhScheduleResult> lhScheduleResults = context.getLhScheduleResults();
        if (lhScheduleResults != null && !lhScheduleResults.isEmpty()) {
            // 按胎胚编码分组汇总
            Map<String, List<LhScheduleResult>> embryoTaskMap = lhScheduleResults.stream()
                    .filter(r -> r.getEmbryoCode() != null)
                    .collect(Collectors.groupingBy(LhScheduleResult::getEmbryoCode));

            for (Map.Entry<String, List<LhScheduleResult>> entry : embryoTaskMap.entrySet()) {
                String embryoCode = entry.getKey();
                List<LhScheduleResult> lhResults = entry.getValue();

                // 计算硫化需求量（汇总所有硫化机的需求）
                int totalVulcanizeDemand = lhResults.stream()
                        .mapToInt(r -> r.getDailyPlanQty() != null ? r.getDailyPlanQty() : 0)
                        .sum();

                // 获取当前库存
                int currentStock = 0;
                Integer embryoStock = lhResults.get(0).getEmbryoStock();
                if (embryoStock != null) {
                    currentStock = embryoStock;
                } else {
                    CxStock stock = stockMap.get(embryoCode);
                    if (stock != null) {
                        currentStock = stock.getEffectiveStock();
                    }
                }

                // 计算净需求
                int netDemand = totalVulcanizeDemand - currentStock;
                
                // 如果库存充足，跳过
                if (netDemand <= 0) {
                    log.debug("胎胚 {} 库存充足，无需生产，硫化需求: {}, 库存: {}", 
                            embryoCode, totalVulcanizeDemand, currentStock);
                    continue;
                }

                // 考虑损耗率
                BigDecimal lossRate = context.getLossRate();
                if (lossRate == null) {
                    lossRate = new BigDecimal("0.02"); // 默认2%
                }
                int dailyDemand = (int) Math.ceil(netDemand * (1 + lossRate.doubleValue()));

                // 获取结构编码
                String structureCode = lhResults.get(0).getStructureName();
                String structureName = lhResults.get(0).getStructureName();

                // 获取该结构的整车容量（不同结构整车条数可能不同，如12、18等）
                int tripCapacity = getTripCapacity(structureCode, context);
                
                // 整车取整（向上取整到整车容量的倍数）
                dailyDemand = roundToTrip(dailyDemand, "CEILING", tripCapacity);

                // 判断续作：检查是否有在机机台
                List<String> continueMachineCodes = new ArrayList<>();
                for (Map.Entry<String, Set<String>> machineEntry : machineOnlineEmbryoMap.entrySet()) {
                    String machineCode = machineEntry.getKey();
                    Set<String> onlineEmbryos = machineEntry.getValue();
                    if (onlineEmbryos != null && onlineEmbryos.contains(embryoCode)) {
                        continueMachineCodes.add(machineCode);
                    }
                }
                boolean isContinueTask = !continueMachineCodes.isEmpty();

                // 判断是否试制
                boolean isTrialTask = lhResults.stream()
                        .anyMatch(r -> "1".equals(r.getIsTrial()));

                // 判断是否首排：非续作、非试制/量试的任务
                boolean isFirstTask = !isContinueTask && !isTrialTask;

                // 计算收尾余量
                // 收尾余量 = 硫化余量(PLAN_SURPLUS_QTY) - 胎胚库存
                // 硫化余量来自 t_mdm_month_surplus.PLAN_SURPLUS_QTY（已由系统计算好，无需再计算）
                Integer vulcanizeSurplusQty = null;
                Integer endingSurplusQty = null;
                boolean isEndingTask = false;
                
                if (context.getMonthSurplusMap() != null) {
                    com.zlt.aps.cx.entity.mdm.MdmMonthSurplus monthSurplus = 
                            context.getMonthSurplusMap().get(embryoCode);
                    if (monthSurplus != null && monthSurplus.getPlanSurplusQty() != null) {
                        // 硫化余量 = 总计划量 - 硫化真实完成量（已由系统计算）
                        vulcanizeSurplusQty = monthSurplus.getPlanSurplusQty();
                        int stockQty = currentStock;
                        // 收尾余量 = 硫化余量 - 胎胚库存
                        endingSurplusQty = vulcanizeSurplusQty - stockQty;
                        // 收尾余量 <= 0 表示该任务需要收尾
                        isEndingTask = endingSurplusQty != null && endingSurplusQty <= 0;
                    }
                }
                
                // 判断是否主销产品（月均销量 >= 500条）
                boolean isMainProduct = context.getMainProductCodes() != null 
                        && context.getMainProductCodes().contains(embryoCode);

                // 构建任务
                DailyEmbryoTask task = new DailyEmbryoTask();
                task.setMaterialCode(embryoCode);
                
                // 获取物料名称
                MdmMaterialInfo material = materialMap.get(embryoCode);
                if (material != null) {
                    task.setMaterialName(material.getMaterialDesc());
                    task.setStructureCode(material.getStructureName());
                    task.setStructureName(material.getStructureName());
                } else {
                    task.setMaterialName(embryoCode);
                    task.setStructureCode(structureCode);
                    task.setStructureName(structureName);
                }
                
                task.setDemandQuantity(dailyDemand);
                task.setAssignedQuantity(0);
                task.setRemainingQuantity(dailyDemand);
                task.setIsTrialTask(isTrialTask);
                task.setIsFirstTask(isFirstTask);
                task.setIsContinueTask(isContinueTask);
                task.setContinueMachineCodes(continueMachineCodes);
                task.setIsMainProduct(isMainProduct);
                
                // 收尾相关属性
                task.setIsEndingTask(isEndingTask);
                task.setEndingSurplusQty(endingSurplusQty);
                task.setVulcanizeSurplusQty(vulcanizeSurplusQty);
                task.setCurrentStock(currentStock);

                // 计算库存时长
                CxStock stock = stockMap.get(embryoCode);
                if (stock != null) {
                    task.setStockHours(stock.getStockHours());
                    task.setVulcanizeMachineCount(stock.getVulcanizeMachineCount());
                    task.setVulcanizeMoldCount(stock.getVulcanizeMoldCount());
                }

                // 计算优先级分数（收尾任务通过分数体现紧急程度）
                task.setPriority(calculatePriorityScoreNew(task, material, stock, context));

                tasks.add(task);
                
                if (isContinueTask) {
                    log.info("续作任务: 胎胚={}, 需求量={}, 续作机台={}, 收尾余量={}", 
                            embryoCode, dailyDemand, continueMachineCodes, endingSurplusQty);
                }
                if (isEndingTask) {
                    log.info("收尾任务: 胎胚={}, 硫化余量={}, 库存={}, 收尾余量={}", 
                            embryoCode, vulcanizeSurplusQty, currentStock, endingSurplusQty);
                }
            }
        }

        // ==================== 处理试制任务 ====================
        if (!CollectionUtils.isEmpty(context.getTrialTasks())) {
            for (CxTrialTask trialTask : context.getTrialTasks()) {
                if ("PENDING".equals(trialTask.getStatus()) || "SCHEDULED".equals(trialTask.getStatus())) {
                    DailyEmbryoTask task = createDailyEmbryoTask(
                            trialTask.getMaterialCode(),
                            trialTask.getTrialQuantity() - trialTask.getProducedQuantity(),
                            materialMap, stockMap, endingMap, context);
                    if (task != null) {
                        task.setIsTrialTask(true);
                        task.setTrialNo(trialTask.getTrialNo());
                        task.setPriority(1500); // 试制任务高优先级
                        tasks.add(task);
                    }
                }
            }
        }

        // ==================== 按优先级排序 ====================
        // 正确优先级顺序：续作 > 试制 > 首排
        // 收尾是任务属性，不是独立的优先级层级（续作/试制/首排任务都可能需要收尾）
        tasks.sort((a, b) -> {
            // 1. 续作任务最高优先级（必须在原机台继续生产，不可中断）
            if (Boolean.TRUE.equals(a.getIsContinueTask()) && !Boolean.TRUE.equals(b.getIsContinueTask())) {
                return -1;
            }
            if (!Boolean.TRUE.equals(a.getIsContinueTask()) && Boolean.TRUE.equals(b.getIsContinueTask())) {
                return 1;
            }
            // 2. 试制任务优先（试制/量试优先级大于首排任务）
            if (Boolean.TRUE.equals(a.getIsTrialTask()) && !Boolean.TRUE.equals(b.getIsTrialTask())) {
                return -1;
            }
            if (!Boolean.TRUE.equals(a.getIsTrialTask()) && Boolean.TRUE.equals(b.getIsTrialTask())) {
                return 1;
            }
            // 3. 首排任务（非续作、非试制/量试就是首排）
            if (Boolean.TRUE.equals(a.getIsFirstTask()) && !Boolean.TRUE.equals(b.getIsFirstTask())) {
                return -1;
            }
            if (!Boolean.TRUE.equals(a.getIsFirstTask()) && Boolean.TRUE.equals(b.getIsFirstTask())) {
                return 1;
            }
            // 4. 按优先级分数排序（库存时长越短越急，收尾任务通过分数体现）
            return Integer.compare(b.getPriority(), a.getPriority());
        });

        log.info("计算完成，日胎胚任务数: {}, 其中续作任务数: {}, 试制任务数: {}, 收尾任务数: {}", 
                tasks.size(), 
                tasks.stream().filter(t -> Boolean.TRUE.equals(t.getIsContinueTask())).count(),
                tasks.stream().filter(t -> Boolean.TRUE.equals(t.getIsTrialTask())).count(),
                tasks.stream().filter(t -> Boolean.TRUE.equals(t.getIsEndingTask())).count());

        return tasks;
    }

    @Override
    public List<MachineAllocationResult> allocateTasksToMachines(
            List<DailyEmbryoTask> tasks, 
            ScheduleContextDTO context) {
        
        List<MachineAllocationResult> results = new ArrayList<>();
        
        // 初始化机台状态
        Map<String, MachineAllocationResult> machineStatusMap = initMachineStatus(context);
        
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
        
        // 加载结构班产配置
        Map<String, Map<String, CxStructureShiftCapacity>> structureCapacityMap = loadStructureShiftCapacity();
        
        for (MachineAllocationResult allocation : allocations) {
            ShiftAllocationResult shiftResult = new ShiftAllocationResult();
            shiftResult.setMachineCode(allocation.getMachineCode());
            shiftResult.setMachineName(allocation.getMachineName());
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
                    firstShift = "SHIFT_DAY"; // 默认早班为开产首班
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
     * 获取下一个班次
     */
    private String getNextShift(String currentShift) {
        if ("SHIFT_NIGHT".equals(currentShift)) {
            return "SHIFT_DAY";
        } else if ("SHIFT_DAY".equals(currentShift)) {
            return "SHIFT_AFTERNOON";
        } else {
            return "SHIFT_NIGHT";
        }
    }
    
    /**
     * 加载结构班产配置
     * 返回：Map<结构编码, Map<班次编码, 班产配置>>
     */
    private Map<String, Map<String, CxStructureShiftCapacity>> loadStructureShiftCapacity() {
        Map<String, Map<String, CxStructureShiftCapacity>> result = new HashMap<>();
        
        List<CxStructureShiftCapacity> capacities = structureShiftCapacityMapper.selectList(
                new LambdaQueryWrapper<CxStructureShiftCapacity>()
                        .eq(CxStructureShiftCapacity::getIsActive, 1));
        
        for (CxStructureShiftCapacity capacity : capacities) {
            result.computeIfAbsent(capacity.getStructureCode(), k -> new HashMap<>())
                    .put(capacity.getShiftCode(), capacity);
        }
        
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
        
        // 班次时间配置
        Map<String, int[]> shiftTimeMap = new HashMap<>();
        shiftTimeMap.put("SHIFT_NIGHT", new int[]{0, 8});
        shiftTimeMap.put("SHIFT_DAY", new int[]{8, 16});
        shiftTimeMap.put("SHIFT_AFTERNOON", new int[]{16, 24});

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
            com.zlt.aps.cx.entity.mdm.MdmMonthSurplus monthSurplus = 
                    context.getMonthSurplusMap().get(material.getMaterialCode());
            if (monthSurplus != null && monthSurplus.getPlanSurplusQty() != null) {
                int stockQty = stock != null ? stock.getEffectiveStock() : 0;
                // 收尾余量 = 硫化余量 - 胎胚库存
                int endingSurplusQty = monthSurplus.getPlanSurplusQty() - stockQty;
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

    // ==================== 私有方法 ====================

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
     */
    private Map<String, MachineAllocationResult> initMachineStatus(ScheduleContextDTO context) {
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
                continue;
            }
            // 检查维护状态
            if ("MAINTAINING".equals(machine.getMaintainStatus()) || 
                "FAULT".equals(machine.getMaintainStatus())) {
                log.debug("机台 {} 状态异常（{}），跳过", 
                        machine.getCxMachineCode(), machine.getMaintainStatus());
                continue;
            }

            MachineAllocationResult result = new MachineAllocationResult();
            result.setMachineCode(machine.getCxMachineCode());
            result.setMachineName(machine.getCxMachineName());
            result.setMachineType(machine.getCxMachineTypeName());
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
                if ("SHIFT_DAY".equals(planShift)) {
                    // 早班精度，扣减早班产能
                    result.setRemainingCapacity(result.getRemainingCapacity() - precisionDeduction);
                    log.info("机台 {} 在早班有精度计划，扣减产能 {} 条", 
                            machine.getCxMachineCode(), precisionDeduction);
                } else if ("SHIFT_AFTERNOON".equals(planShift)) {
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
                
                // 如果剩余产能为负，设为0
                if (result.getRemainingCapacity() < 0) {
                    log.warn("机台 {} 精度计划导致剩余产能为负，设为0", machine.getCxMachineCode());
                    result.setRemainingCapacity(0);
                }
            }

            map.put(machine.getCxMachineCode(), result);
        }

        log.info("初始化机台状态完成，可用机台 {} 台", map.size());
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
        
        // 默认50条/小时
        return 50;
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
        task.setStructureCode(material.getStructureName());
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
            com.zlt.aps.cx.entity.mdm.MdmMonthSurplus monthSurplus = 
                    context.getMonthSurplusMap().get(materialCode);
            if (monthSurplus != null && monthSurplus.getPlanSurplusQty() != null) {
                vulcanizeSurplusQty = monthSurplus.getPlanSurplusQty();
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
        String structureName = material.getProductStructure();
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
        
        List<CxScheduleResult> results = new ArrayList<>();
        LocalDate scheduleDate = context.getScheduleDate();

        // 按机台构建排程结果
        Map<String, ShiftAllocationResult> shiftMap = shiftAllocations.stream()
                .collect(Collectors.toMap(ShiftAllocationResult::getMachineCode, s -> s));

        for (MachineAllocationResult allocation : allocations) {
            CxScheduleResult result = new CxScheduleResult();
            result.setScheduleDate(scheduleDate.atStartOfDay());
            result.setCxMachineCode(allocation.getMachineCode());
            result.setCxMachineName(allocation.getMachineName());
            result.setCxMachineType(allocation.getMachineType());
            result.setProductNum(new BigDecimal(allocation.getUsedCapacity()));
            result.setProductionStatus("0");
            result.setIsRelease("0");
            result.setDataSource("0");
            result.setCreateTime(new Date());

            // 设置班次计划量
            ShiftAllocationResult shiftResult = shiftMap.get(allocation.getMachineCode());
            if (shiftResult != null) {
                Map<String, Integer> shiftPlanQty = shiftResult.getShiftPlanQty();
                result.setClass1PlanQty(new BigDecimal(shiftPlanQty.getOrDefault("SHIFT_NIGHT", 0)));
                result.setClass2PlanQty(new BigDecimal(shiftPlanQty.getOrDefault("SHIFT_DAY", 0)));
                result.setClass3PlanQty(new BigDecimal(shiftPlanQty.getOrDefault("SHIFT_AFTERNOON", 0)));
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
}
