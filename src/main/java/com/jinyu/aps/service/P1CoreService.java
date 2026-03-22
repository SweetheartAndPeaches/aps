package com.jinyu.aps.service;

import com.jinyu.aps.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * P1核心功能服务
 * 
 * 实现：
 * 4. 节假日开产/停产管理
 * 5. 试制量试插单逻辑
 * 6. 班次动态调整
 *
 * @author APS Team
 */
@Slf4j
@Service
public class P1CoreService {

    // ==================== 4. 节假日开产/停产管理功能 ====================

    /**
     * 检查指定日期是否为节假日
     * 
     * @param holidayConfigs 节假日配置列表
     * @param date 检查日期
     * @return 节假日配置，如果不是节假日则返回null
     */
    public HolidayConfig checkHoliday(List<HolidayConfig> holidayConfigs, LocalDate date) {
        return holidayConfigs.stream()
            .filter(c -> date.equals(c.getHolidayDate()))
            .filter(c -> "ENABLED".equals(c.getStatus()))
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取节假日的产能比例
     * 
     * @param holidayConfig 节假日配置（可为null）
     * @param shiftCode 班次编码
     * @return 产能比例（0-100）
     */
    public int getHolidayCapacityRatio(HolidayConfig holidayConfig, String shiftCode) {
        if (holidayConfig == null) {
            return 100; // 非节假日，正常产能
        }

        // 检查班次是否受影响
        String effectiveShifts = holidayConfig.getEffectiveShifts();
        if (effectiveShifts != null && !effectiveShifts.isEmpty()) {
            List<String> shifts = Arrays.asList(effectiveShifts.split(","));
            if (!shifts.contains(shiftCode)) {
                return 100; // 该班次不受影响
            }
        }

        // 返回配置的产能比例
        Integer ratio = holidayConfig.getCapacityRatio();
        return ratio != null ? ratio : 0;
    }

    /**
     * 根据节假日调整机台产能
     * 
     * @param machines 机台列表
     * @param holidayConfig 节假日配置
     * @param shiftCode 班次编码
     * @return 调整后的机台列表
     */
    public List<Machine> adjustCapacityForHoliday(
            List<Machine> machines, 
            HolidayConfig holidayConfig, 
            String shiftCode) {
        
        if (holidayConfig == null) {
            return machines;
        }

        int capacityRatio = getHolidayCapacityRatio(holidayConfig, shiftCode);
        
        if (capacityRatio >= 100) {
            return machines; // 无需调整
        }

        if (capacityRatio <= 0) {
            // 停产，返回空列表
            log.info("节假日 {} 停产，所有机台暂停生产", holidayConfig.getHolidayName());
            return Collections.emptyList();
        }

        // 减产模式，按比例调整产能
        List<Machine> adjustedMachines = new ArrayList<>();
        String applicableMachines = holidayConfig.getApplicableMachines();
        Set<String> machineSet = applicableMachines != null && !applicableMachines.isEmpty()
            ? new HashSet<>(Arrays.asList(applicableMachines.split(",")))
            : null;

        for (Machine machine : machines) {
            // 检查机台是否受影响
            if (machineSet != null && !machineSet.contains(machine.getMachineCode())) {
                adjustedMachines.add(machine);
                continue;
            }

            // 调整机台产能
            Machine adjustedMachine = cloneMachine(machine);
            int originalCapacity = machine.getMaxDailyCapacity() != null ? machine.getMaxDailyCapacity() : 0;
            int adjustedCapacity = originalCapacity * capacityRatio / 100;
            adjustedMachine.setMaxDailyCapacity(adjustedCapacity);
            adjustedMachines.add(adjustedMachine);

            log.info("节假日调整：机台 {} 产能从 {} 调整为 {} (比例{}%)", 
                machine.getMachineCode(), originalCapacity, adjustedCapacity, capacityRatio);
        }

        return adjustedMachines;
    }

    /**
     * 判断是否可以在节假日排产
     * 
     * @param holidayConfig 节假日配置
     * @return 是否可以排产
     */
    public boolean canScheduleOnHoliday(HolidayConfig holidayConfig) {
        if (holidayConfig == null) {
            return true;
        }
        return !"STOPPED".equals(holidayConfig.getProductionStatus());
    }

    // ==================== 5. 试制量试插单逻辑功能 ====================

    /**
     * 将试制任务转换为日胚任务
     * 
     * @param trialTask 试制任务
     * @return 日胚任务
     */
    public DailyEmbryoTask convertTrialToDailyTask(TrialTask trialTask) {
        DailyEmbryoTask task = new DailyEmbryoTask();
        task.setMaterialCode(trialTask.getMaterialCode());
        task.setStructureCode(trialTask.getStructureCode());
        task.setPlanQuantity(trialTask.getTrialQuantity() - 
            (trialTask.getProducedQuantity() != null ? trialTask.getProducedQuantity() : 0));
        task.setPlanDate(trialTask.getExpectedStartDate());
        task.setPriority(trialTask.getPriority() != null ? trialTask.getPriority() : 5);
        task.setIsTrialTask(1); // 标记为试制任务
        task.setTrialNo(trialTask.getTrialNo());
        task.setStatus("PENDING");
        return task;
    }

    /**
     * 根据插单类型确定任务优先级
     * 
     * @param insertType 插单类型
     * @param basePriority 基础优先级
     * @return 调整后的优先级
     */
    public int calculateTrialPriority(String insertType, int basePriority) {
        if ("FORCE".equals(insertType)) {
            return 10; // 强制插单，最高优先级
        } else if ("URGENT".equals(insertType)) {
            return Math.min(basePriority + 3, 9); // 紧急插单，提升3级
        } else {
            return basePriority; // 正常排程
        }
    }

    /**
     * 检查试制任务是否可以插单
     * 
     * @param trialTask 试制任务
     * @param existingTasks 现有任务列表
     * @param machines 机台列表
     * @return 是否可以插单
     */
    public boolean canInsertTrialTask(
            TrialTask trialTask, 
            List<DailyEmbryoTask> existingTasks,
            List<Machine> machines) {
        
        // 检查试制任务状态
        if (!"PENDING".equals(trialTask.getStatus()) && !"APPROVED".equals(trialTask.getStatus())) {
            log.warn("试制任务 {} 状态不允许插单: {}", trialTask.getTrialNo(), trialTask.getStatus());
            return false;
        }

        // 检查机台是否可用
        if (trialTask.getAssignedMachine() != null) {
            boolean machineAvailable = machines.stream()
                .anyMatch(m -> m.getMachineCode().equals(trialTask.getAssignedMachine()));
            if (!machineAvailable) {
                log.warn("试制任务 {} 指定机台 {} 不可用", trialTask.getTrialNo(), trialTask.getAssignedMachine());
                return false;
            }
        }

        // 检查日期是否合理
        LocalDate expectedStart = trialTask.getExpectedStartDate();
        LocalDate expectedEnd = trialTask.getExpectedEndDate();
        if (expectedStart != null && expectedStart.isBefore(LocalDate.now())) {
            log.warn("试制任务 {} 期望开始日期已过期", trialTask.getTrialNo());
            return false;
        }

        return true;
    }

    /**
     * 执行试制任务插单
     * 
     * @param trialTask 试制任务
     * @param existingTasks 现有任务列表
     * @param machines 机台列表
     * @return 插单后的任务列表
     */
    public List<DailyEmbryoTask> insertTrialTask(
            TrialTask trialTask,
            List<DailyEmbryoTask> existingTasks,
            List<Machine> machines) {
        
        if (!canInsertTrialTask(trialTask, existingTasks, machines)) {
            return existingTasks;
        }

        // 转换为日胚任务
        DailyEmbryoTask trialDailyTask = convertTrialToDailyTask(trialTask);
        
        // 计算插单优先级
        int priority = calculateTrialPriority(
            trialTask.getInsertType(), 
            trialTask.getPriority() != null ? trialTask.getPriority() : 5
        );
        trialDailyTask.setPriority(priority);

        // 分配机台
        if (trialTask.getAssignedMachine() != null) {
            trialDailyTask.setMachineCode(trialTask.getAssignedMachine());
        } else {
            // 自动分配机台（选择同结构已排产机台或产能最大的机台）
            String assignedMachine = autoAssignMachine(trialTask, existingTasks, machines);
            trialDailyTask.setMachineCode(assignedMachine);
        }

        // 插入任务列表
        List<DailyEmbryoTask> newTasks = new ArrayList<>(existingTasks);
        newTasks.add(trialDailyTask);

        // 按优先级重新排序
        newTasks.sort((a, b) -> {
            int priorityA = a.getPriority() != null ? a.getPriority() : 5;
            int priorityB = b.getPriority() != null ? b.getPriority() : 5;
            return priorityB - priorityA; // 优先级高的在前
        });

        log.info("试制任务 {} 已插单，优先级: {}, 分配机台: {}", 
            trialTask.getTrialNo(), priority, trialDailyTask.getMachineCode());

        return newTasks;
    }

    /**
     * 自动分配机台
     */
    private String autoAssignMachine(TrialTask trialTask, List<DailyEmbryoTask> existingTasks, List<Machine> machines) {
        String targetStructure = trialTask.getStructureCode();
        
        // 优先选择已生产同结构的机台
        Optional<String> sameStructureMachine = existingTasks.stream()
            .filter(t -> targetStructure != null && targetStructure.equals(t.getStructureCode()))
            .map(DailyEmbryoTask::getMachineCode)
            .filter(Objects::nonNull)
            .findFirst();
        
        if (sameStructureMachine.isPresent()) {
            return sameStructureMachine.get();
        }

        // 否则选择产能最大的可用机台
        return machines.stream()
            .filter(m -> m.getIsActive() != null && m.getIsActive() == 1)
            .max(Comparator.comparing(m -> m.getMaxDailyCapacity() != null ? m.getMaxDailyCapacity() : 0))
            .map(Machine::getMachineCode)
            .orElse(null);
    }

    /**
     * 批量处理试制任务插单
     * 
     * @param trialTasks 试制任务列表
     * @param existingTasks 现有任务列表
     * @param machines 机台列表
     * @return 插单后的任务列表
     */
    public List<DailyEmbryoTask> batchInsertTrialTasks(
            List<TrialTask> trialTasks,
            List<DailyEmbryoTask> existingTasks,
            List<Machine> machines) {
        
        // 按优先级排序试制任务
        List<TrialTask> sortedTrialTasks = trialTasks.stream()
            .filter(t -> "PENDING".equals(t.getStatus()) || "APPROVED".equals(t.getStatus()))
            .sorted((a, b) -> {
                int priorityA = calculateTrialPriority(a.getInsertType(), a.getPriority() != null ? a.getPriority() : 5);
                int priorityB = calculateTrialPriority(b.getInsertType(), b.getPriority() != null ? b.getPriority() : 5);
                return priorityB - priorityA;
            })
            .collect(Collectors.toList());

        List<DailyEmbryoTask> result = new ArrayList<>(existingTasks);
        for (TrialTask trialTask : sortedTrialTasks) {
            result = insertTrialTask(trialTask, result, machines);
        }

        return result;
    }

    // ==================== 6. 班次动态调整功能 ====================

    /**
     * 获取指定日期的班次配置
     * 
     * @param shiftConfigs 班次配置列表
     * @param date 日期
     * @param shiftCode 班次编码
     * @return 班次配置，如果没有则返回null
     */
    public ShiftConfig getShiftConfig(List<ShiftConfig> shiftConfigs, LocalDate date, String shiftCode) {
        return shiftConfigs.stream()
            .filter(c -> date.equals(c.getConfigDate()))
            .filter(c -> shiftCode.equals(c.getShiftCode()))
            .filter(c -> c.getIsActive() != null && c.getIsActive() == 1)
            .findFirst()
            .orElse(null);
    }

    /**
     * 计算班次有效工时
     * 
     * @param shiftConfig 班次配置（可为null）
     * @param defaultHours 默认工时
     * @return 有效工时
     */
    public int calculateEffectiveHours(ShiftConfig shiftConfig, int defaultHours) {
        if (shiftConfig == null) {
            return defaultHours;
        }
        return shiftConfig.getAdjustedHours() != null 
            ? shiftConfig.getAdjustedHours() 
            : (shiftConfig.getStandardHours() != null ? shiftConfig.getStandardHours() : defaultHours);
    }

    /**
     * 计算班次产能比例
     * 
     * @param shiftConfig 班次配置
     * @return 产能比例（0-100）
     */
    public int calculateShiftCapacityRatio(ShiftConfig shiftConfig) {
        if (shiftConfig == null) {
            return 100;
        }
        return shiftConfig.getCapacityRatio() != null ? shiftConfig.getCapacityRatio() : 100;
    }

    /**
     * 根据班次配置调整机台产能
     * 
     * @param machine 机台
     * @param shiftConfig 班次配置
     * @param shiftCode 班次编码
     * @return 调整后的班次产能
     */
    public int adjustMachineCapacityForShift(Machine machine, ShiftConfig shiftConfig, String shiftCode) {
        int dailyCapacity = machine.getMaxDailyCapacity() != null ? machine.getMaxDailyCapacity() : 0;
        int baseShiftCapacity = dailyCapacity / 3; // 三个班次均分

        if (shiftConfig == null) {
            return baseShiftCapacity;
        }

        // 调整时长比例
        int standardHours = shiftConfig.getStandardHours() != null ? shiftConfig.getStandardHours() : 8;
        int adjustedHours = calculateEffectiveHours(shiftConfig, standardHours);
        double hoursRatio = (double) adjustedHours / standardHours;

        // 应用产能比例
        int capacityRatio = calculateShiftCapacityRatio(shiftConfig);

        int adjustedCapacity = (int) (baseShiftCapacity * hoursRatio * capacityRatio / 100);

        log.debug("班次调整: 机台 {} 班次 {} 产能从 {} 调整为 {} (时长比={}, 产能比={}%)",
            machine.getMachineCode(), shiftCode, baseShiftCapacity, adjustedCapacity, hoursRatio, capacityRatio);

        return adjustedCapacity;
    }

    /**
     * 批量调整机台班次产能
     * 
     * @param machines 机台列表
     * @param shiftConfigs 班次配置列表
     * @param date 日期
     * @param shiftCode 班次编码
     * @return 班次产能映射（机台编码 -> 产能）
     */
    public Map<String, Integer> calculateShiftCapacities(
            List<Machine> machines,
            List<ShiftConfig> shiftConfigs,
            LocalDate date,
            String shiftCode) {
        
        ShiftConfig shiftConfig = getShiftConfig(shiftConfigs, date, shiftCode);
        Map<String, Integer> capacityMap = new HashMap<>();

        for (Machine machine : machines) {
            int capacity = adjustMachineCapacityForShift(machine, shiftConfig, shiftCode);
            capacityMap.put(machine.getMachineCode(), capacity);
        }

        return capacityMap;
    }

    /**
     * 计算班次时间分布
     * 
     * @param shiftConfigs 班次配置列表
     * @param date 日期
     * @return 班次时间分布（班次编码 -> 工时）
     */
    public Map<String, Integer> calculateShiftDistribution(
            List<ShiftConfig> shiftConfigs, 
            LocalDate date) {
        
        Map<String, Integer> distribution = new LinkedHashMap<>();
        String[] defaultShifts = {"NIGHT", "DAY", "AFTERNOON"};
        
        for (String shiftCode : defaultShifts) {
            ShiftConfig config = getShiftConfig(shiftConfigs, date, shiftCode);
            int hours = calculateEffectiveHours(config, 8);
            distribution.put(shiftCode, hours);
        }

        return distribution;
    }

    /**
     * 验证班次配置是否合理
     * 
     * @param shiftConfig 班次配置
     * @return 验证结果（空表示通过，否则返回错误信息）
     */
    public String validateShiftConfig(ShiftConfig shiftConfig) {
        if (shiftConfig.getStartTime() != null && shiftConfig.getEndTime() != null) {
            long hours = ChronoUnit.HOURS.between(shiftConfig.getStartTime(), shiftConfig.getEndTime());
            if (hours <= 0) {
                hours += 24; // 跨天情况
            }
            if (hours > 12) {
                return "班次时长超过12小时，请检查配置";
            }
        }

        if (shiftConfig.getAdjustedHours() != null && shiftConfig.getAdjustedHours() > 12) {
            return "调整后工时超过12小时";
        }

        if (shiftConfig.getCapacityRatio() != null && 
            (shiftConfig.getCapacityRatio() < 0 || shiftConfig.getCapacityRatio() > 100)) {
            return "产能比例必须在0-100之间";
        }

        return null; // 验证通过
    }

    // ==================== 辅助方法 ====================

    /**
     * 克隆机台（用于产能调整）
     */
    private Machine cloneMachine(Machine source) {
        Machine target = new Machine();
        target.setId(source.getId());
        target.setMachineCode(source.getMachineCode());
        target.setMachineName(source.getMachineName());
        target.setMachineType(source.getMachineType());
        target.setWrappingType(source.getWrappingType());
        target.setHasZeroDegreeFeeder(source.getHasZeroDegreeFeeder());
        target.setStructure(source.getStructure());
        target.setMaxCapacityPerHour(source.getMaxCapacityPerHour());
        target.setMaxDailyCapacity(source.getMaxDailyCapacity());
        target.setMaxCuringMachines(source.getMaxCuringMachines());
        target.setFixedStructure1(source.getFixedStructure1());
        target.setFixedStructure2(source.getFixedStructure2());
        target.setFixedStructure3(source.getFixedStructure3());
        target.setRestrictedStructures(source.getRestrictedStructures());
        target.setProductionRestriction(source.getProductionRestriction());
        target.setLineNumber(source.getLineNumber());
        target.setStatus(source.getStatus());
        target.setIsActive(source.getIsActive());
        return target;
    }
}
