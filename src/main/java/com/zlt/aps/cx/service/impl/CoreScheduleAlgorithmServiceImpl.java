package com.zlt.aps.cx.service.impl;

import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.entity.*;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxStructurePriority;
import com.zlt.aps.cx.entity.mdm.*;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.service.CoreScheduleAlgorithmService;
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
 * 实现试错分配、班次均衡、顺位排序等核心算法
 *
 * @author APS Team
 */
@Slf4j
@Service
public class CoreScheduleAlgorithmServiceImpl implements CoreScheduleAlgorithmService {

    /** 整车容量（12条/车） */
    private static final int TRIP_CAPACITY = 12;

    /** 机台种类上限 */
    private static final int MAX_TYPES_PER_MACHINE = 4;

    /** 波浪比例：夜班:早班:中班 = 1:2:1 */
    private static final int[] WAVE_RATIO = {1, 2, 1};

    @Override
    public List<CxScheduleResult> executeSchedule(ScheduleContextDTO context) {
        log.info("开始执行排程算法，日期: {}", context.getScheduleDate());

        // 第一步：计算日胎胚任务
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
        Map<String, CxMaterial> materialMap = context.getMaterials().stream()
                .collect(Collectors.toMap(CxMaterial::getMaterialCode, m -> m));
        Map<String, CxStock> stockMap = context.getStocks().stream()
                .collect(Collectors.toMap(CxStock::getMaterialCode, s -> s, (a, b) -> a));

        // 构建结构收尾映射
        Map<String, CxStructureEnding> endingMap = context.getStructureEndings().stream()
                .collect(Collectors.toMap(CxStructureEnding::getStructureCode, e -> e, (a, b) -> a));

        // 处理试制任务（优先级最高）
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
                        task.setPriority(1000); // 试制任务最高优先级
                        tasks.add(task);
                    }
                }
            }
        }

        // 计算常规任务
        for (CxMaterial material : context.getMaterials()) {
            if (material.getIsActive() == null || material.getIsActive() != 1) {
                continue;
            }

            CxStock stock = stockMap.get(material.getMaterialCode());

            // 计算日需求量
            BigDecimal dailyDemand = calculateDailyDemand(material, stock, context);
            if (dailyDemand.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            DailyEmbryoTask task = createDailyEmbryoTask(
                    material.getMaterialCode(),
                    dailyDemand.intValue(),
                    materialMap, stockMap, endingMap, context);
            if (task != null) {
                tasks.add(task);
            }
        }

        // 按优先级排序
        tasks.sort((a, b) -> {
            // 紧急收尾优先
            if (Boolean.TRUE.equals(a.getIsUrgentEnding()) && !Boolean.TRUE.equals(b.getIsUrgentEnding())) {
                return -1;
            }
            if (!Boolean.TRUE.equals(a.getIsUrgentEnding()) && Boolean.TRUE.equals(b.getIsUrgentEnding())) {
                return 1;
            }
            // 试制任务优先
            if (Boolean.TRUE.equals(a.getIsTrialTask()) && !Boolean.TRUE.equals(b.getIsTrialTask())) {
                return -1;
            }
            if (!Boolean.TRUE.equals(a.getIsTrialTask()) && Boolean.TRUE.equals(b.getIsTrialTask())) {
                return 1;
            }
            // 按优先级分数排序
            return Integer.compare(b.getPriority(), a.getPriority());
        });

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
                if (!materials.contains(task.getMaterialCode()) && materials.size() >= MAX_TYPES_PER_MACHINE) {
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
                    allocation.setIsUrgentEnding(task.getIsUrgentEnding());
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
        
        // 班次顺序：夜班、早班、中班
        String[] shiftCodes = {"SHIFT_NIGHT", "SHIFT_DAY", "SHIFT_AFTERNOON"};
        
        for (MachineAllocationResult allocation : allocations) {
            ShiftAllocationResult shiftResult = new ShiftAllocationResult();
            shiftResult.setMachineCode(allocation.getMachineCode());
            shiftResult.setMachineName(allocation.getMachineName());
            shiftResult.setTasks(allocation.getTaskAllocations());
            
            Map<String, Integer> shiftPlanQty = new LinkedHashMap<>();
            int totalQty = allocation.getUsedCapacity();

            // 计算波浪分配
            int[] waveQty = calculateWaveAllocation(totalQty);
            
            // 处理特殊情况
            if (Boolean.TRUE.equals(context.getIsOpeningDay())) {
                // 开产首班：只排6小时，计划量减半
                waveQty[1] = waveQty[1] / 2; // 早班减半
            }

            for (int i = 0; i < shiftCodes.length; i++) {
                shiftPlanQty.put(shiftCodes[i], waveQty[i]);
            }
            
            shiftResult.setShiftPlanQty(shiftPlanQty);
            results.add(shiftResult);
        }

        return results;
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
            // 对每个班次的任务按紧急程度排序
            List<TaskAllocation> sortedTasks = shiftAllocation.getTasks().stream()
                    .sorted((a, b) -> {
                        // 紧急收尾优先
                        if (Boolean.TRUE.equals(a.getIsUrgentEnding()) && !Boolean.TRUE.equals(b.getIsUrgentEnding())) {
                            return -1;
                        }
                        if (!Boolean.TRUE.equals(a.getIsUrgentEnding()) && Boolean.TRUE.equals(b.getIsUrgentEnding())) {
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
                int tripCount = (int) Math.ceil((double) qty / TRIP_CAPACITY);

                for (int t = 1; t <= tripCount; t++) {
                    CxScheduleDetail detail = new CxScheduleDetail();
                    detail.setScheduleDate(scheduleDate);
                    detail.setCxMachineCode(shiftAllocation.getMachineCode());
                    detail.setCxMachineName(shiftAllocation.getMachineName());
                    detail.setEmbryoCode(task.getMaterialCode());
                    detail.setTripNo(tripNo++);
                    detail.setTripCapacity(TRIP_CAPACITY);
                    detail.setTripActualQty(0);
                    detail.setSequence(globalSequence++);
                    detail.setSequenceInGroup(t);
                    detail.setIsEnding(Boolean.TRUE.equals(task.getIsUrgentEnding()) ? 1 : 0);
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
        
        if (stock == null || stock.getCurrentStock() == null || stock.getCurrentStock() <= 0) {
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

        return BigDecimal.valueOf(stock.getCurrentStock())
                .divide(hourlyOutput, 2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateDailyDemand(
            CxMaterial material,
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
        if (stock != null && stock.getCurrentStock() != null && stock.getCurrentStock() > 0) {
            stockAllocation = BigDecimal.valueOf(stock.getCurrentStock());
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
            CxMaterial material,
            ScheduleContextDTO context) {
        
        if (machine == null || material == null) {
            return false;
        }

        String structure = material.getProductStructure();

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
            CxMaterial newMaterial,
            ScheduleContextDTO context) {
        
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

        return currentTypes < MAX_TYPES_PER_MACHINE;
    }

    @Override
    public int calculatePriorityScore(
            CxMaterial material,
            CxStock stock,
            ScheduleContextDTO context) {
        
        int score = 0;

        // 检查紧急收尾
        for (CxStructureEnding ending : context.getStructureEndings()) {
            if (ending.getStructureCode().equals(material.getProductStructure())) {
                if (Boolean.TRUE.equals(ending.getIsUrgentEnding())) {
                    score += 1000; // 紧急收尾加分
                }
                if (Boolean.TRUE.equals(ending.getIsNearEnding())) {
                    score += 500; // 10天内收尾加分
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

        // 主销产品加分
        if (material.getIsMainProduct() != null && material.getIsMainProduct() == 1) {
            score += 200;
        }

        // 结构优先级加分
        for (CxStructurePriority priority : context.getStructurePriorities()) {
            if (priority.getStructureName().equals(material.getProductStructure())) {
                score += priority.getPriorityLevel() * 10;
                break;
            }
        }

        return score;
    }

    @Override
    public int roundToTrip(int quantity, String mode) {
        if (quantity <= 0) {
            return 0;
        }

        int trips;
        switch (mode) {
            case "CEILING":
                trips = (int) Math.ceil((double) quantity / TRIP_CAPACITY);
                break;
            case "FLOOR":
                trips = (int) Math.floor((double) quantity / TRIP_CAPACITY);
                break;
            case "ROUND":
            default:
                trips = (int) Math.round((double) quantity / TRIP_CAPACITY);
                break;
        }

        return trips * TRIP_CAPACITY;
    }

    // ==================== 私有方法 ====================

    /**
     * 初始化机台状态
     */
    private Map<String, MachineAllocationResult> initMachineStatus(ScheduleContextDTO context) {
        Map<String, MachineAllocationResult> map = new LinkedHashMap<>();

        for (MdmMoldingMachine machine : context.getAvailableMachines()) {
            if (machine.getIsActive() == null || machine.getIsActive() != 1) {
                continue;
            }
            // 检查维护状态
            if ("MAINTAINING".equals(machine.getMaintainStatus()) || 
                "FAULT".equals(machine.getMaintainStatus())) {
                continue;
            }

            MachineAllocationResult result = new MachineAllocationResult();
            result.setMachineCode(machine.getCxMachineCode());
            result.setMachineName(machine.getCxMachineName());
            result.setMachineType(machine.getCxMachineTypeName());
            result.setDailyCapacity(machine.getMaxDailyCapacity() != null 
                    ? machine.getMaxDailyCapacity() : 1200);
            result.setUsedCapacity(0);
            result.setRemainingCapacity(result.getDailyCapacity());
            result.setAssignedTypes(0);
            result.setTaskAllocations(new ArrayList<>());
            result.setCurrentStructure(machine.getCurrentStructure());

            // 检查精度计划（扣减4小时产能）
            for (CxPrecisionPlan plan : context.getPrecisionPlans()) {
                if (plan.getMachineCode().equals(machine.getCxMachineCode()) &&
                    plan.getPlanDate().equals(context.getScheduleDate())) {
                    // 扣减4小时产能（假设每小时50条）
                    int precisionDeduction = 200;
                    result.setRemainingCapacity(result.getRemainingCapacity() - precisionDeduction);
                }
            }

            map.put(machine.getCxMachineCode(), result);
        }

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
     */
    private int calculateMachineScore(
            MdmMoldingMachine machine,
            DailyEmbryoTask task,
            MachineAllocationResult status,
            ScheduleContextDTO context) {
        
        int score = 0;

        // 优先选昨日做过该胎胚的机台（续作）
        for (CxScheduleResult yesterday : context.getYesterdayResults()) {
            if (yesterday.getCxMachineCode().equals(machine.getCxMachineCode()) &&
                yesterday.getEmbryoCode().equals(task.getMaterialCode())) {
                score += 500;
                break;
            }
        }

        // 优先选剩余产能最多的（均衡）
        score += status.getRemainingCapacity() / 10;

        // 优先选种类最少的（均衡）
        score += (MAX_TYPES_PER_MACHINE - status.getAssignedTypes()) * 50;

        // 优先选固定生产该结构的机台
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

        return score;
    }

    /**
     * 计算波浪分配
     */
    private int[] calculateWaveAllocation(int totalQty) {
        int[] result = new int[3];
        
        // 按比例计算
        int totalRatio = WAVE_RATIO[0] + WAVE_RATIO[1] + WAVE_RATIO[2];
        int remaining = totalQty;

        for (int i = 0; i < 3; i++) {
            int qty = totalQty * WAVE_RATIO[i] / totalRatio;
            // 整车取整
            qty = roundToTrip(qty, "ROUND");
            result[i] = qty;
            remaining -= qty;
        }

        // 分配余量
        for (int i = 0; i < remaining && i < 3; i++) {
            result[i % 3] += TRIP_CAPACITY;
        }

        return result;
    }

    /**
     * 创建日胎胚任务
     */
    private DailyEmbryoTask createDailyEmbryoTask(
            String materialCode,
            Integer demandQuantity,
            Map<String, CxMaterial> materialMap,
            Map<String, CxStock> stockMap,
            Map<String, CxStructureEnding> endingMap,
            ScheduleContextDTO context) {
        
        CxMaterial material = materialMap.get(materialCode);
        if (material == null) {
            return null;
        }

        DailyEmbryoTask task = new DailyEmbryoTask();
        task.setMaterialCode(materialCode);
        task.setMaterialName(material.getMaterialName());
        task.setStructureCode(material.getProductStructure());
        task.setStructureName(material.getProductStructure());
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

        // 检查紧急收尾
        CxStructureEnding ending = endingMap.get(material.getProductStructure());
        if (ending != null) {
            task.setIsUrgentEnding(ending.getIsUrgentEnding() != null && ending.getIsUrgentEnding() == 1);
        } else {
            task.setIsUrgentEnding(false);
        }

        // 是否主销产品
        task.setIsMainProduct(material.getIsMainProduct() != null && material.getIsMainProduct() == 1);

        // 计算优先级
        task.setPriority(calculatePriorityScore(material, stock, context));

        return task;
    }

    /**
     * 获取硫化消耗量
     */
    private BigDecimal getVulcanizeDemand(CxMaterial material, ScheduleContextDTO context) {
        // 从结构硫化配比中获取硫化需求
        for (MdmStructureLhRatio ratio : context.getStructureLhRatios()) {
            if (ratio.getStructureName().equals(material.getProductStructure())) {
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
            result.setCreateTime(LocalDateTime.now());

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
