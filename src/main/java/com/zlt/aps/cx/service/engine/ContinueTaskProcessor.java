package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;

import com.zlt.aps.cx.service.engine.ScheduleDayTypeHelper.DayFlagInfo;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.sql.Date;
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
    private final ScheduleDayTypeHelper scheduleDayTypeHelper;

    /** 胎胚库容上限比例 */
    private static final double EMBRYO_STORAGE_RATIO = 0.9;

    /** 默认整车容量（条/车） */
    private static final int DEFAULT_TRIP_CAPACITY = 12;

    /** 默认最大硫化机台数 */
    private static final int DEFAULT_MAX_LH_MACHINE_COUNT = 10;

    /** 默认日产能（条/天），机台未配置时使用 */
    private static final int DEFAULT_DAILY_CAPACITY = 1200;

    /** 默认机台小时产能（条/小时） */
    private static final int DEFAULT_HOURLY_CAPACITY = 50;

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

        // 标记续作任务
        for (CoreScheduleAlgorithmService.DailyEmbryoTask task : continueTasks) {
            task.setIsContinueTask(true);
        }

        // 检查是否强制保留历史任务
        boolean forceKeepHistory = getForceKeepHistoryConfig(context);
        log.info("强制保留历史任务配置: {}", forceKeepHistory);

        if (!forceKeepHistory) {
            // 不做保底预留，续作任务全部由 NewTaskProcessor 统一均衡
            log.info("强制保留历史任务未开启，续作任务不保底预留，全部交给新增均衡处理");
            log.info("========== 续作任务处理完成（仅标记），共 {} 个任务 ==========", continueTasks.size());
            return results;
        }

        // 构建历史任务映射
        Map<String, Set<String>> machineHistoryMap = buildMachineHistoryMap(context);
        log.info("构建历史任务映射完成，共 {} 台机台有历史记录", machineHistoryMap.size());

        // 保底预留：每个机台的每个历史胎胚至少预留1个在原机台
        // 使用 Map<机台编码, MachineAllocationResult> 收集预留结果
        Map<String, CoreScheduleAlgorithmService.MachineAllocationResult> allocationMap = new LinkedHashMap<>();

        for (Map.Entry<String, Set<String>> historyEntry : machineHistoryMap.entrySet()) {
            String machineCode = historyEntry.getKey();
            Set<String> historyEmbryos = historyEntry.getValue();

            for (String embryoCode : historyEmbryos) {
                // 在续作任务列表中找到 demand > 0 的任务
                CoreScheduleAlgorithmService.DailyEmbryoTask matchedTask = null;
                for (CoreScheduleAlgorithmService.DailyEmbryoTask task : continueTasks) {
                    if (embryoCode.equals(task.getEmbryoCode())) {
                        int demand = task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0;
                        if (demand > 0) {
                            matchedTask = task;
                            break;
                        }
                    }
                }

                if (matchedTask == null) {
                    log.debug("机台 {} 的历史胎胚 {} 在续作任务中无剩余需求，跳过保底预留", machineCode, embryoCode);
                    continue;
                }

                int demand = matchedTask.getVulcanizeMachineCount() != null ? matchedTask.getVulcanizeMachineCount() : 0;

                // 保底预留1个硫化机
                int reservedCount = 1;
                matchedTask.setVulcanizeMachineCount(demand - reservedCount);

                // 构建分配结果
                CoreScheduleAlgorithmService.MachineAllocationResult allocation =
                        allocationMap.computeIfAbsent(machineCode, code -> createMachineAllocation(code, context));

                allocateContinueReservation(allocation, matchedTask, reservedCount, context);

                log.info("机台 {} 保底预留胎胚 {} 共 {} 个硫化机", machineCode, embryoCode, reservedCount);
            }
        }

        results.addAll(allocationMap.values());
        log.info("========== 续作任务保底预留完成，共 {} 台机台预留任务 ==========", results.size());
        return results;
    }

    /**
     * 保底预留分配到机台（续作保底预留场景，只分配预留的硫化机数，不是全量）
     */
    private void allocateContinueReservation(
            CoreScheduleAlgorithmService.MachineAllocationResult allocation,
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            int reservedVulcanizeCount,
            ScheduleContextVo context) {

        // quantity 应该始终是胎胚数量，不是硫化机数
        // 使用 endingExtraInventory（最终需要生产的量，经过收尾处理）
        int quantity = task.getEndingExtraInventory() != null && task.getEndingExtraInventory() > 0
                ? task.getEndingExtraInventory() : task.getDemandQuantity();

        CoreScheduleAlgorithmService.TaskAllocation taskAllocation = new CoreScheduleAlgorithmService.TaskAllocation();
        taskAllocation.setEmbryoCode(task.getEmbryoCode());
        taskAllocation.setMaterialCode(task.getMaterialCode());
        taskAllocation.setMaterialDesc(task.getMaterialDesc());
        taskAllocation.setMainMaterialDesc(task.getMainMaterialDesc());
        taskAllocation.setStructureName(task.getStructureName());
        taskAllocation.setQuantity(quantity);  // 设置为胎胚数量
        taskAllocation.setVulcanizeMachineCount(reservedVulcanizeCount);  // 硫化机数单独存储
        taskAllocation.setPriority(task.getPriority());
        taskAllocation.setStockHours(task.getStockHours());
        taskAllocation.setIsTrialTask(task.getIsTrialTask());
        taskAllocation.setIsProductionTrial(task.getIsProductionTrial());
        taskAllocation.setIsEndingTask(task.getIsEndingTask());
        taskAllocation.setEndingSurplusQty(task.getEndingSurplusQty());
        taskAllocation.setEndingExtraInventory(task.getEndingExtraInventory());  // 设置收尾额外库存
        taskAllocation.setIsLastEndingBatch(task.getIsLastEndingBatch());  // 设置是否收尾最后一批
        taskAllocation.setIsMainProduct(task.getIsMainProduct());
        taskAllocation.setIsContinueTask(true);  // 标记为续作预留
        taskAllocation.setLhId(task.getLhId());

        allocation.getTaskAllocations().add(taskAllocation);
        // 注意：这里占用的是硫化机数，不是胎胚数量
        allocation.setUsedCapacity(allocation.getUsedCapacity() + reservedVulcanizeCount);
        allocation.setRemainingCapacity(allocation.getRemainingCapacity() - reservedVulcanizeCount);
    }

    // ==================== 辅助方法 ====================

    /**
     * 按结构分组
     */
    private Map<String, List<CoreScheduleAlgorithmService.DailyEmbryoTask>> groupTasksByStructure(
            List<CoreScheduleAlgorithmService.DailyEmbryoTask> tasks) {

        // 先分组
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
                boolean result = "1".equals(config.getParamValue()) || "true".equalsIgnoreCase(config.getParamValue());
                log.info("FORCE_KEEP_HISTORY_TASK 数据库配置值: {}, 解析结果: {}", config.getParamValue(), result);
                return result;
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
            String structureName, LocalDate scheduleDate, ScheduleContextVo context,
            String productionVersion) {
        if (context.getStructureAllocationMap() != null) {
            List<MpCxCapacityConfiguration> configs = context.getStructureAllocationMap().get(structureName);
            if (configs != null && !configs.isEmpty()) {
                int day = scheduleDate.getDayOfMonth();
                // 过滤日期范围 + PRODUCTION_VERSION
                return configs.stream()
                        .filter(c -> c.getBeginDay() != null && c.getEndDay() != null)
                        .filter(c -> c.getBeginDay() <= day && c.getEndDay() >= day)
                        .filter(c -> productionVersion == null
                                || productionVersion.equals(c.getProductionVersion()))
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
                maxLh = DEFAULT_MAX_LH_MACHINE_COUNT;
                log.debug("机台 {} 机型 {} 结构 {} 未找到配比配置，使用默认值 {}",
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

        // H15开头机台：如果有专用配置则优先使用
        Integer h15MaxEmbryoTypes = context.getH15MaxEmbryoTypes();

        // 为每台机台获取对应的最大胎胚种类数
        for (MpCxCapacityConfiguration config : machineConfigs) {
            String machineCode = config.getCxMachineCode();
            String machineType = machineTypeMap.get(machineCode);

            // H15开头机台：如果有专用配置则优先使用，否则走配比逻辑
            if (h15MaxEmbryoTypes != null && machineCode != null && machineCode.startsWith("H15")) {
                log.info("  机台 {} (机型={}): 使用H15专用最大胎胚种类数={}", machineCode, machineType, h15MaxEmbryoTypes);
                result.put(machineCode, h15MaxEmbryoTypes);
                continue;
            }

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
                maxTypes = context.getMaxTypesPerMachine() != null ? context.getMaxTypesPerMachine() : BalancingService.DEFAULT_MAX_TYPES_PER_MACHINE;
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
                    return machine.getMaxDayCapacity() != null ? machine.getMaxDayCapacity() : DEFAULT_DAILY_CAPACITY;
                }
            }
        }
        return DEFAULT_DAILY_CAPACITY;
    }

    /**
     * 从任务对象中读取当前库存，同步到 currentStock 字段
     *
     * <p>materialStockMap 的库存分配在 TaskGroupService.buildSingleTask 中已完成
     * （按各硫化任务的需求比例预分配）。此处仅读取并同步到
     * currentStock 字段，供后续 calculatePlannedProduction 使用。
     *
     * @param task         任务
     * @param context      排程上下文
     * @param scheduleDate 排程日期（未使用，为扩展预留）
     */
    public void allocateEmbryoStock(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextVo context,
            LocalDate scheduleDate) {
        
        // 库存已在 TaskGroupService.buildSingleTask 中按物料需求比例分配好
        // 这里直接使用当前库存
        int currentStock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;
        task.setCurrentStock(currentStock);
        log.debug("胎胚 {} 当前库存：{}", task.getEmbryoCode(), currentStock);
    }
    
    /**
     * 计算任务的计划量（条）
     *
     * <p>硫化需求 − 库存抵扣后，调用 ProductionCalculator 整车取整。
     * 仅在正常生产日执行，开停产日已在 handleOpeningClosingDay 中处理。
     *
     * @param task         任务
     * @param context      排程上下文
     * @param scheduleDate 排程日期
     */
    public void calculatePlannedProduction(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextVo context,
            LocalDate scheduleDate,
            boolean isOpeningDay) {

        // 停产日已在 handleOpeningClosingDay 中设置 plannedProduction=0，跳过此处
        if (scheduleDayTypeHelper.isStopDay(scheduleDate, context.getFactoryCode())) {
            return;
        }

        // 硫化需求 − 库存抵扣 = 待排条数
        int vulcanizeDemand = task.getVulcanizeDemand() != null ? task.getVulcanizeDemand() : 0;
        int currentStock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;
        int requiredProduction = Math.max(0, vulcanizeDemand - currentStock);

        // 整车取整（由 ProductionCalculator 统一管理）
        int tripCapacity = getTripCapacity(task.getStructureName(), context);
        int plannedProduction = productionCalculator.roundToVehicle(requiredProduction, tripCapacity);
        task.setPlannedProduction(plannedProduction);

        log.debug("任务 {} 待排产量：需求={}，库存={}，待排={}，胎面整车={}，计划量={}",
                task.getEmbryoCode(), vulcanizeDemand, currentStock,
                requiredProduction, tripCapacity, plannedProduction);
    }

    
    /**
     * 处理开产日和停产日对任务计划量的影响
     *
     * <p>判断逻辑：
     * <ol>
     *   <li>从当前排产日期往前找最近一个有 dayFlag 标识的日期（MdmWorkCalendar.dayFlag 不为 null）</li>
     *   <li>若该日期是「停」（dayFlag="0"）→ 往后都是停产</li>
     *   <li>若该日期是「开」（dayFlag="1"）→ 正常按硫化计划安排</li>
     * </ol>
     *
     * <p>停产日处理：
     * <ul>
     *   <li>停产是今天：有量（库存必须为0），安排 = 硫化需要的量（不做整车取整）</li>
     *   <li>停产不是今天（已停产）：plannedProduction = 0</li>
     * </ul>
     *
     * <p>开产日处理：
     * <ul>
     *   <li>开产日有量但不多，严格按硫化计划安排，取整到整车</li>
     * </ul>
     *
     * @param task         任务
     * @param context      排程上下文
     * @param dayShifts    当天班次配置（未使用，开产停产处理不需要分班次）
     */
    public void handleOpeningClosingDay(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextVo context,
            List<CxShiftConfig> dayShifts) {
        
        LocalDate scheduleDate = context.getCurrentScheduleDate();
        
        // Step 1: 从当前日期往前找最近一个有 dayFlag 标识的日期
        DayFlagInfo flagInfo = scheduleDayTypeHelper.findNearestDayFlag(scheduleDate, context.getFactoryCode());
        
        if (flagInfo == null || flagInfo.dayFlag == null) {
            // 没有找到任何标识，按正常日期处理
            return;
        }
        
        if ("0".equals(flagInfo.dayFlag)) {
            // 最近标识是「停」→ 停产标识日之后都是停产日
            // 停产标识日当天有量（最后一天生产），只有停产日之后才算停产
            task.setIsClosingDayTask(true);
            
            if (scheduleDate.isAfter(flagInfo.nearestDate)) {
                // 停产日之后：无法安排
                task.setPlannedProduction(0);
                log.debug("停产日（已停产），不安排：materialCode={}", task.getEmbryoCode());
            }
            // 停产标识日当天：有量，plannedProduction 保持原值
            // 整车取整由 calculatePlannedProduction 完成
        } else if ("1".equals(flagInfo.dayFlag)) {
            // 最近标识是「开」→ 正常按硫化计划安排，取整到整车
            task.setIsOpeningDayTask(true);
            // 开产日有量但不多，整车取整由 calculatePlannedProduction 完成
            log.debug("开产日，正常按硫化计划安排：materialCode={}", task.getEmbryoCode());
        }
        // 其他情况（dayFlag 未知）按正常处理，不做干预
    }
    /**
     * 处理收尾任务的余量约束
     *
     * <p>前提：仅对 isEndingTask=true 或 isNearEnding=true 的任务生效
     * <p>逻辑：
     * <ol>
     *   <li>计算剩余需生产量 = 收尾余量 - 已分配库存</li>
     *   <li>调用 ProductionCalculator 计算收尾计划量（整车取整 + 非主销产品余量≤2条则舍弃）</li>
     *   <li>若余量被舍弃 → 计划量=0，标记 abandoned</li>
     *   <li>若主销产品多做了 → 记录 extraInventory</li>
     *   <li>若本批完成全部收尾 → 标记 isLastEndingBatch</li>
     * </ol>
     *
     * @param task         任务
     * @param context      排程上下文
     * @param isOpeningDay 是否开产日（开产日不触发收尾处理）
     */
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
        int currentStock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;
        int totalPlanned = plannedProduction + currentStock;

        // 使用 ProductionCalculator 计算收尾计划量
        boolean isMainProduct = Boolean.TRUE.equals(task.getIsMainProduct());
        int remainingToProduce = Math.max(0, endingSurplus - currentStock);
        
        ProductionCalculator.PlanQuantityResult endingResult = productionCalculator.calculateEndingQuantity(
                remainingToProduce,
                getTripCapacity(task.getStructureName(), context),
                isMainProduct,
                task.getEmbryoCode()
        );
        
        // 更新任务状态
        if (endingResult.isAbandoned()) {
            // 非主销产品余量≤2条，舍弃
            task.setPlannedProduction(0);
            task.setEndingAbandoned(true);
            task.setEndingAbandonedQty(endingResult.getAbandonedQuantity());
            log.info("收尾任务 {} 余量 {} 条被舍弃", task.getEmbryoCode(), endingResult.getAbandonedQuantity());
        } else {
            // 更新计划量
            int newPlanQuantity = endingResult.getPlanQuantity();
            task.setPlannedProduction(newPlanQuantity);
            
            // 记录额外库存（主销产品多做的部分）
            if (endingResult.getExtraInventory() > 0) {
                task.setEndingExtraInventory(endingResult.getExtraInventory());
                log.info("收尾任务 {} 主销产品，多做 {} 条当库存", 
                        task.getEmbryoCode(), endingResult.getExtraInventory());
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


    private int getTripCapacity(String structureCode, ScheduleContextVo context) {
        return productionCalculator.getTripCapacity(structureCode, context);
    }

    private void allocateTaskToMachine(
            CoreScheduleAlgorithmService.MachineAllocationResult allocation,
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextVo context) {

        // 使用 endingExtraInventory（最终需要生产的量，经过收尾处理）
        int quantity = task.getEndingExtraInventory() != null && task.getEndingExtraInventory() > 0
                ? task.getEndingExtraInventory() : task.getDemandQuantity();

        CoreScheduleAlgorithmService.TaskAllocation taskAllocation = new CoreScheduleAlgorithmService.TaskAllocation();
        taskAllocation.setEmbryoCode(task.getEmbryoCode());
        taskAllocation.setMaterialCode(task.getMaterialCode());
        taskAllocation.setMaterialDesc(task.getMaterialDesc());
        taskAllocation.setMainMaterialDesc(task.getMainMaterialDesc());
        taskAllocation.setStructureName(task.getStructureName());
        taskAllocation.setQuantity(quantity);
        taskAllocation.setVulcanizeMachineCount(task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 1);
        taskAllocation.setPriority(task.getPriority());
        taskAllocation.setStockHours(task.getStockHours());
        taskAllocation.setIsTrialTask(task.getIsTrialTask());
        taskAllocation.setIsProductionTrial(task.getIsProductionTrial());
        taskAllocation.setIsEndingTask(task.getIsEndingTask());
        taskAllocation.setEndingSurplusQty(task.getEndingSurplusQty());
        taskAllocation.setEndingExtraInventory(task.getEndingExtraInventory());  // 设置收尾额外库存
        taskAllocation.setIsLastEndingBatch(task.getIsLastEndingBatch());  // 设置是否收尾最后一批
        taskAllocation.setIsMainProduct(task.getIsMainProduct());
        taskAllocation.setLhId(task.getLhId());

        allocation.getTaskAllocations().add(taskAllocation);
        allocation.setUsedCapacity(allocation.getUsedCapacity() + quantity);
        allocation.setRemainingCapacity(allocation.getRemainingCapacity() - quantity);
    }
}
