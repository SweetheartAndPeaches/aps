package com.zlt.aps.cx.service;

import com.zlt.aps.cx.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * P2核心功能服务
 * 
 * 实现：
 * 7. 操作工请假管理
 * 8. 胎面停放时间约束
 * 9. 材料异常处理
 *
 * @author APS Team
 */
@Slf4j
@Service
public class P2CoreService {

    // ==================== 7. 操作工请假管理功能 ====================

    /**
     * 获取指定日期和班次的请假记录
     * 
     * @param leaveRecords 请假记录列表
     * @param date 日期
     * @param shiftCode 班次编码
     * @return 请假记录列表
     */
    public List<OperatorLeave> getLeaveRecords(
            List<OperatorLeave> leaveRecords, 
            LocalDate date, 
            String shiftCode) {
        
        return leaveRecords.stream()
            .filter(l -> "APPROVED".equals(l.getApprovalStatus()))
            .filter(l -> !date.isBefore(l.getStartDate()) && !date.isAfter(l.getEndDate()))
            .filter(l -> {
                if (l.getAffectedShifts() == null || l.getAffectedShifts().isEmpty()) {
                    return true;
                }
                return Arrays.asList(l.getAffectedShifts().split(",")).contains(shiftCode);
            })
            .collect(Collectors.toList());
    }

    /**
     * 计算机台的请假影响比例
     * 
     * @param leaveRecords 请假记录列表
     * @param machineCode 机台编码
     * @param date 日期
     * @param shiftCode 班次编码
     * @return 影响比例（0-100）
     */
    public int calculateLeaveImpactRatio(
            List<OperatorLeave> leaveRecords,
            String machineCode,
            LocalDate date,
            String shiftCode) {
        
        List<OperatorLeave> machineLeaves = getLeaveRecords(leaveRecords, date, shiftCode).stream()
            .filter(l -> machineCode.equals(l.getMachineCode()))
            .filter(l -> l.getAffectCapacity() != null && l.getAffectCapacity() == 1)
            .collect(Collectors.toList());

        if (machineLeaves.isEmpty()) {
            return 0;
        }

        // 累加影响比例
        int totalImpact = machineLeaves.stream()
            .mapToInt(l -> l.getCapacityImpactRatio() != null ? l.getCapacityImpactRatio() : 0)
            .sum();

        // 上限100%
        return Math.min(totalImpact, 100);
    }

    /**
     * 根据请假情况调整机台产能
     * 
     * @param machine 机台
     * @param leaveRecords 请假记录列表
     * @param date 日期
     * @param shiftCode 班次编码
     * @return 调整后的班次产能
     */
    public int adjustCapacityForLeave(
            Machine machine,
            List<OperatorLeave> leaveRecords,
            LocalDate date,
            String shiftCode) {
        
        int dailyCapacity = machine.getMaxDailyCapacity() != null ? machine.getMaxDailyCapacity() : 0;
        int shiftCapacity = dailyCapacity / 3;

        int impactRatio = calculateLeaveImpactRatio(leaveRecords, machine.getMachineCode(), date, shiftCode);
        
        if (impactRatio <= 0) {
            return shiftCapacity;
        }

        int adjustedCapacity = shiftCapacity * (100 - impactRatio) / 100;

        if (impactRatio >= 100) {
            log.warn("机台 {} 因请假人数过多，产能降为0", machine.getMachineCode());
        } else {
            log.info("机台 {} 因请假影响产能下降 {}%，从 {} 降为 {}", 
                machine.getMachineCode(), impactRatio, shiftCapacity, adjustedCapacity);
        }

        return adjustedCapacity;
    }

    /**
     * 批量计算机台产能（考虑请假影响）
     * 
     * @param machines 机台列表
     * @param leaveRecords 请假记录列表
     * @param date 日期
     * @param shiftCode 班次编码
     * @return 机台产能映射
     */
    public Map<String, Integer> calculateCapacitiesWithLeave(
            List<Machine> machines,
            List<OperatorLeave> leaveRecords,
            LocalDate date,
            String shiftCode) {
        
        Map<String, Integer> capacityMap = new HashMap<>();
        for (Machine machine : machines) {
            int capacity = adjustCapacityForLeave(machine, leaveRecords, date, shiftCode);
            capacityMap.put(machine.getMachineCode(), capacity);
        }
        return capacityMap;
    }

    /**
     * 获取请假统计信息
     * 
     * @param leaveRecords 请假记录列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计信息
     */
    public Map<String, Object> getLeaveStatistics(
            List<OperatorLeave> leaveRecords,
            LocalDate startDate,
            LocalDate endDate) {
        
        List<OperatorLeave> filteredRecords = leaveRecords.stream()
            .filter(l -> !l.getEndDate().isBefore(startDate) && !l.getStartDate().isAfter(endDate))
            .collect(Collectors.toList());

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalRecords", filteredRecords.size());
        
        // 按请假类型统计
        Map<String, Long> byType = filteredRecords.stream()
            .collect(Collectors.groupingBy(OperatorLeave::getLeaveType, Collectors.counting()));
        statistics.put("byType", byType);

        // 按机台统计
        Map<String, Long> byMachine = filteredRecords.stream()
            .collect(Collectors.groupingBy(OperatorLeave::getMachineCode, Collectors.counting()));
        statistics.put("byMachine", byMachine);

        // 总请假天数
        int totalDays = filteredRecords.stream()
            .mapToInt(l -> l.getLeaveDays() != null ? l.getLeaveDays() : 0)
            .sum();
        statistics.put("totalLeaveDays", totalDays);

        return statistics;
    }

    // ==================== 8. 胎面停放时间约束功能 ====================

    /**
     * 获取结构的停放时间配置
     * 
     * @param parkingConfigs 停放配置列表
     * @param structureCode 结构编码
     * @return 停放配置
     */
    public TreadParkingConfig getParkingConfig(
            List<TreadParkingConfig> parkingConfigs, 
            String structureCode) {
        
        return parkingConfigs.stream()
            .filter(c -> structureCode.equals(c.getStructureCode()))
            .filter(c -> c.getIsEnabled() != null && c.getIsEnabled() == 1)
            .findFirst()
            .orElse(null);
    }

    /**
     * 计算停放时间（小时）
     * 
     * @param produceTime 生产时间
     * @param currentTime 当前时间
     * @return 停放时间（小时）
     */
    public double calculateParkingHours(LocalDateTime produceTime, LocalDateTime currentTime) {
        if (produceTime == null || currentTime == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(produceTime, currentTime) / 60.0;
    }

    /**
     * 检查停放时间是否在合理范围内
     * 
     * @param parkingHours 停放时间（小时）
     * @param config 停放配置
     * @return 状态：NORMAL(正常)/WARNING(预警)/EXCEEDED(超时)
     */
    public String checkParkingStatus(double parkingHours, TreadParkingConfig config) {
        if (config == null) {
            return "NORMAL"; // 无配置，默认正常
        }

        int minHours = config.getMinParkingHours() != null ? config.getMinParkingHours() : 0;
        int maxHours = config.getMaxParkingHours() != null ? config.getMaxParkingHours() : Integer.MAX_VALUE;
        int warningHours = config.getWarningThresholdHours() != null 
            ? config.getWarningThresholdHours() : maxHours - 2;

        if (parkingHours < minHours) {
            log.warn("停放时间不足：{} 小时，最小要求 {} 小时", parkingHours, minHours);
            return "WARNING";
        }

        if (parkingHours > maxHours) {
            log.warn("停放时间超时：{} 小时，最大限制 {} 小时", parkingHours, maxHours);
            return "EXCEEDED";
        }

        if (parkingHours > warningHours) {
            log.info("停放时间接近上限：{} 小时，预警阈值 {} 小时", parkingHours, warningHours);
            return "WARNING";
        }

        return "NORMAL";
    }

    /**
     * 根据停放状态调整任务优先级
     * 
     * @param task 任务
     * @param parkingHours 停放时间
     * @param config 停放配置
     * @return 调整后的优先级
     */
    public int adjustPriorityForParking(
            DailyEmbryoTask task, 
            double parkingHours,
            TreadParkingConfig config) {
        
        String status = checkParkingStatus(parkingHours, config);
        int basePriority = task.getPriority() != null ? task.getPriority() : 5;

        if ("EXCEEDED".equals(status)) {
            // 超时，根据配置处理
            if (config != null && "ADJUST_PRIORITY".equals(config.getTimeoutAction())) {
                int boost = config.getPriorityBoost() != null ? config.getPriorityBoost() : 3;
                int newPriority = Math.min(basePriority + boost, 10);
                log.info("停放超时，任务 {} 优先级从 {} 提升到 {}", 
                    task.getMaterialCode(), basePriority, newPriority);
                return newPriority;
            }
        }

        if ("WARNING".equals(status)) {
            // 预警，轻微提升优先级
            return Math.min(basePriority + 1, 10);
        }

        return basePriority;
    }

    /**
     * 批量检查停放状态并调整优先级
     * 
     * @param tasks 任务列表
     * @param parkingConfigs 停放配置列表
     * @param currentTime 当前时间
     * @return 调整后的任务列表
     */
    public List<DailyEmbryoTask> checkAndAdjustParking(
            List<DailyEmbryoTask> tasks,
            List<TreadParkingConfig> parkingConfigs,
            LocalDateTime currentTime) {
        
        for (DailyEmbryoTask task : tasks) {
            if (task.getProduceTime() != null) {
                double parkingHours = calculateParkingHours(task.getProduceTime(), currentTime);
                TreadParkingConfig config = getParkingConfig(parkingConfigs, task.getStructureCode());
                
                int adjustedPriority = adjustPriorityForParking(task, parkingHours, config);
                task.setPriority(adjustedPriority);
                task.setParkingHours(parkingHours);
            }
        }

        // 按优先级重新排序
        tasks.sort((a, b) -> {
            int priorityA = a.getPriority() != null ? a.getPriority() : 5;
            int priorityB = b.getPriority() != null ? b.getPriority() : 5;
            return priorityB - priorityA;
        });

        return tasks;
    }

    /**
     * 获取停放预警任务列表
     * 
     * @param tasks 任务列表
     * @param parkingConfigs 停放配置列表
     * @param currentTime 当前时间
     * @return 预警任务列表
     */
    public List<DailyEmbryoTask> getParkingWarningTasks(
            List<DailyEmbryoTask> tasks,
            List<TreadParkingConfig> parkingConfigs,
            LocalDateTime currentTime) {
        
        return tasks.stream()
            .filter(t -> t.getProduceTime() != null)
            .filter(t -> {
                double parkingHours = calculateParkingHours(t.getProduceTime(), currentTime);
                TreadParkingConfig config = getParkingConfig(parkingConfigs, t.getStructureCode());
                String status = checkParkingStatus(parkingHours, config);
                return "WARNING".equals(status) || "EXCEEDED".equals(status);
            })
            .collect(Collectors.toList());
    }

    // ==================== 9. 材料异常处理功能 ====================

    /**
     * 获取有效的材料异常记录
     * 
     * @param exceptions 异常记录列表
     * @param date 日期
     * @return 有效异常记录列表
     */
    public List<MaterialException> getActiveExceptions(
            List<MaterialException> exceptions, 
            LocalDate date) {
        
        return exceptions.stream()
            .filter(e -> !"CLOSED".equals(e.getStatus()) && !"RESOLVED".equals(e.getStatus()))
            .filter(e -> {
                boolean afterStart = e.getAffectStartDate() == null || 
                    !date.isBefore(e.getAffectStartDate());
                boolean beforeEnd = e.getAffectEndDate() == null || 
                    !date.isAfter(e.getAffectEndDate());
                return afterStart && beforeEnd;
            })
            .collect(Collectors.toList());
    }

    /**
     * 检查材料是否有异常
     * 
     * @param exceptions 异常记录列表
     * @param materialCode 材料编码
     * @param date 日期
     * @return 是否有异常
     */
    public boolean hasMaterialException(
            List<MaterialException> exceptions, 
            String materialCode, 
            LocalDate date) {
        
        return getActiveExceptions(exceptions, date).stream()
            .anyMatch(e -> materialCode.equals(e.getMaterialCode()));
    }

    /**
     * 获取材料的替代材料
     * 
     * @param exceptions 异常记录列表
     * @param materialCode 原材料编码
     * @param date 日期
     * @return 替代材料编码，如果没有则返回null
     */
    public String getSubstituteMaterial(
            List<MaterialException> exceptions,
            String materialCode,
            LocalDate date) {
        
        return getActiveExceptions(exceptions, date).stream()
            .filter(e -> materialCode.equals(e.getMaterialCode()))
            .filter(e -> "CHANGE_MATERIAL".equals(e.getHandlingMethod()))
            .map(MaterialException::getSubstituteMaterial)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    /**
     * 处理材料异常对任务的影响
     * 
     * @param task 任务
     * @param exceptions 异常记录列表
     * @param date 日期
     * @return 处理结果：NORMAL(正常)/SUBSTITUTED(已替换)/SKIPPED(跳过)/DELAYED(延迟)
     */
    public String handleMaterialException(
            DailyEmbryoTask task,
            List<MaterialException> exceptions,
            LocalDate date) {
        
        String materialCode = task.getMaterialCode();
        
        List<MaterialException> materialExceptions = getActiveExceptions(exceptions, date).stream()
            .filter(e -> materialCode.equals(e.getMaterialCode()))
            .collect(Collectors.toList());

        if (materialExceptions.isEmpty()) {
            return "NORMAL";
        }

        // 检查异常等级和处理方式
        for (MaterialException ex : materialExceptions) {
            String handlingMethod = ex.getHandlingMethod();
            
            if ("CANCEL_PLAN".equals(handlingMethod)) {
                log.warn("任务 {} 因材料异常 {} 被取消", task.getId(), ex.getExceptionNo());
                return "SKIPPED";
            }

            if ("CHANGE_MATERIAL".equals(handlingMethod) && ex.getSubstituteMaterial() != null) {
                log.info("任务 {} 材料从 {} 替换为 {}", task.getId(), materialCode, ex.getSubstituteMaterial());
                task.setMaterialCode(ex.getSubstituteMaterial());
                task.setOriginalMaterialCode(materialCode);
                return "SUBSTITUTED";
            }

            if ("WAIT_SUPPLY".equals(handlingMethod)) {
                log.info("任务 {} 因等待材料供应延迟", task.getId());
                return "DELAYED";
            }

            if ("ADJUST_PLAN".equals(handlingMethod)) {
                // 调整计划量
                if (ex.getAffectedQuantity() != null && task.getPlanQuantity() != null) {
                    int adjustedQty = Math.max(0, task.getPlanQuantity() - ex.getAffectedQuantity());
                    task.setPlanQuantity(adjustedQty);
                    log.info("任务 {} 计划量调整为 {}", task.getId(), adjustedQty);
                }
            }
        }

        return "NORMAL";
    }

    /**
     * 批量处理材料异常
     * 
     * @param tasks 任务列表
     * @param exceptions 异常记录列表
     * @param date 日期
     * @return 处理结果映射
     */
    public Map<String, String> batchHandleExceptions(
            List<DailyEmbryoTask> tasks,
            List<MaterialException> exceptions,
            LocalDate date) {
        
        Map<String, String> results = new HashMap<>();
        List<DailyEmbryoTask> validTasks = new ArrayList<>();

        for (DailyEmbryoTask task : tasks) {
            String result = handleMaterialException(task, exceptions, date);
            results.put(task.getId() != null ? task.getId().toString() : UUID.randomUUID().toString(), result);
            
            if (!"SKIPPED".equals(result)) {
                validTasks.add(task);
            }
        }

        return results;
    }

    /**
     * 获取材料异常统计
     * 
     * @param exceptions 异常记录列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计信息
     */
    public Map<String, Object> getExceptionStatistics(
            List<MaterialException> exceptions,
            LocalDate startDate,
            LocalDate endDate) {
        
        List<MaterialException> filtered = exceptions.stream()
            .filter(e -> {
                boolean afterStart = e.getAffectStartDate() == null || 
                    !e.getAffectStartDate().isAfter(endDate);
                boolean beforeEnd = e.getAffectEndDate() == null || 
                    !e.getAffectEndDate().isBefore(startDate);
                return afterStart && beforeEnd;
            })
            .collect(Collectors.toList());

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalExceptions", filtered.size());

        // 按类型统计
        Map<String, Long> byType = filtered.stream()
            .collect(Collectors.groupingBy(MaterialException::getExceptionType, Collectors.counting()));
        statistics.put("byType", byType);

        // 按等级统计
        Map<String, Long> byLevel = filtered.stream()
            .collect(Collectors.groupingBy(MaterialException::getExceptionLevel, Collectors.counting()));
        statistics.put("byLevel", byLevel);

        // 按状态统计
        Map<String, Long> byStatus = filtered.stream()
            .collect(Collectors.groupingBy(MaterialException::getStatus, Collectors.counting()));
        statistics.put("byStatus", byStatus);

        // 紧急异常数量
        long criticalCount = filtered.stream()
            .filter(e -> "CRITICAL".equals(e.getExceptionLevel()))
            .count();
        statistics.put("criticalCount", criticalCount);

        return statistics;
    }

    /**
     * 生成异常预警
     * 
     * @param exceptions 异常记录列表
     * @param date 日期
     * @return 预警信息列表
     */
    public List<Map<String, Object>> generateExceptionAlerts(
            List<MaterialException> exceptions,
            LocalDate date) {
        
        return getActiveExceptions(exceptions, date).stream()
            .filter(e -> "HIGH".equals(e.getExceptionLevel()) || "CRITICAL".equals(e.getExceptionLevel()))
            .map(e -> {
                Map<String, Object> alert = new HashMap<>();
                alert.put("exceptionNo", e.getExceptionNo());
                alert.put("materialCode", e.getMaterialCode());
                alert.put("materialName", e.getMaterialName());
                alert.put("exceptionType", e.getExceptionType());
                alert.put("exceptionLevel", e.getExceptionLevel());
                alert.put("description", e.getDescription());
                alert.put("handlingMethod", e.getHandlingMethod());
                alert.put("status", e.getStatus());
                return alert;
            })
            .collect(Collectors.toList());
    }
}
