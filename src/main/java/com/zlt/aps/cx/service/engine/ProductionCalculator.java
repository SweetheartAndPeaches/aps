package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.mp.api.domain.entity.MdmCxMachineFixed;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
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
 * 排量计算服务
 * 
 * <p>负责统一处理各种排量计算逻辑：
 * <ul>
 *   <li>计算待排产量</li>
 *   <li>计算开停产特殊处理</li>
 *   <li>整车取整</li>
 *   <li>损耗率计算</li>
 *   <li>机台产能计算</li>
 * </ul>
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionCalculator {

    /** 默认整车容量 */
    public static final int DEFAULT_TRIP_CAPACITY = 12;
    
    /** 默认机台种类上限 */
    public static final int DEFAULT_MAX_TYPES_PER_MACHINE = 4;
    
    /** 默认损耗率 */
    public static final BigDecimal DEFAULT_LOSS_RATE = new BigDecimal("0.02");
    
    /** 默认机台小时产能 */
    public static final int DEFAULT_HOURLY_CAPACITY = 50;
    
    /** 默认日产能 */
    public static final int DEFAULT_DAILY_CAPACITY = 1200;

    /**
     * 计算待排产量
     *
     * <p>计算逻辑：
     * <ol>
     *   <li>获取日硫化需求量</li>
     *   <li>减去已分配库存</li>
     *   <li>考虑损耗率</li>
     *   <li>整车取整</li>
     * </ol>
     *
     * @param dailyVulcanizeDemand 日硫化需求量
     * @param allocatedStock       已分配库存
     * @param lossRate             损耗率
     * @param tripCapacity         整车容量
     * @param roundMode            取整模式（CEILING/FLOOR/ROUND）
     * @return 待排产量
     */
    public int calculatePlannedProduction(
            int dailyVulcanizeDemand,
            int allocatedStock,
            BigDecimal lossRate,
            int tripCapacity,
            String roundMode) {

        if (dailyVulcanizeDemand <= 0) {
            return 0;
        }

        if (lossRate == null) {
            lossRate = DEFAULT_LOSS_RATE;
        }

        // 基础产量 = 硫化需求 - 已分配库存
        int baseProduction = Math.max(0, dailyVulcanizeDemand - allocatedStock);

        // 考虑损耗率
        int productionWithLoss = (int) Math.ceil(baseProduction * (1 + lossRate.doubleValue()));

        // 整车取整
        return roundToTrip(productionWithLoss, tripCapacity, roundMode);
    }

    /**
     * 计算待排产量（简化版）
     */
    public int calculatePlannedProduction(
            Integer dailyVulcanizeDemand,
            Integer allocatedStock,
            BigDecimal lossRate) {

        int demand = dailyVulcanizeDemand != null ? dailyVulcanizeDemand : 0;
        int stock = allocatedStock != null ? allocatedStock : 0;
        
        return calculatePlannedProduction(demand, stock, lossRate, DEFAULT_TRIP_CAPACITY, "CEILING");
    }

    /**
     * 整车取整
     *
     * @param quantity     数量
     * @param tripCapacity 整车容量
     * @param mode         取整模式（CEILING/FLOOR/ROUND）
     * @return 取整后的数量
     */
    public int roundToTrip(int quantity, int tripCapacity, String mode) {
        if (quantity <= 0 || tripCapacity <= 0) {
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
     * 整车取整（默认整车容量12）
     */
    public int roundToTrip(int quantity, String mode) {
        return roundToTrip(quantity, DEFAULT_TRIP_CAPACITY, mode);
    }

    /**
     * 计算硫化机台数（整车数）
     *
     * @param quantity     数量
     * @param tripCapacity 整车容量
     * @return 硫化机台数
     */
    public int calculateVulcanizeMachineCount(int quantity, int tripCapacity) {
        if (quantity <= 0 || tripCapacity <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) quantity / tripCapacity);
    }

    /**
     * 计算硫化机台数（默认整车容量12）
     */
    public int calculateVulcanizeMachineCount(int quantity) {
        return calculateVulcanizeMachineCount(quantity, DEFAULT_TRIP_CAPACITY);
    }

    /**
     * 计算机台日产能
     *
     * @param machine 机台信息
     * @param context 排程上下文
     * @return 机台日产能
     */
    public int calculateMachineDailyCapacity(MdmMoldingMachine machine, ScheduleContextDTO context) {
        if (machine == null) {
            return DEFAULT_DAILY_CAPACITY;
        }

        // 优先使用机台配置的日产能
        if (machine.getMaxDayCapacity() != null && machine.getMaxDayCapacity() > 0) {
            return machine.getMaxDayCapacity();
        }

        // 根据小时产能和班次计算
        int hourlyCapacity = getMachineHourlyCapacity(machine, context);
        int shiftHours = getShiftHours(context);
        
        return hourlyCapacity * shiftHours;
    }

    /**
     * 获取机台小时产能
     */
    public int getMachineHourlyCapacity(MdmMoldingMachine machine, ScheduleContextDTO context) {
        if (machine == null) {
            return DEFAULT_HOURLY_CAPACITY;
        }

        // 从机台结构产能配置获取
        if (context.getMachineStructureCapacities() != null && machine.getCxMachineCode() != null) {
            for (var capacity : context.getMachineStructureCapacities()) {
                if (machine.getCxMachineCode().equals(capacity.getCxMachineCode())) {
                    if (capacity.getHourlyCapacity() != null && capacity.getHourlyCapacity() > 0) {
                        return capacity.getHourlyCapacity();
                    }
                }
            }
        }

        // 从上下文获取默认值
        return context.getMachineHourlyCapacity() != null 
                ? context.getMachineHourlyCapacity() 
                : DEFAULT_HOURLY_CAPACITY;
    }

    /**
     * 获取班次总时长
     */
    private int getShiftHours(ScheduleContextDTO context) {
        if (context.getCurrentShiftConfigs() != null && !context.getCurrentShiftConfigs().isEmpty()) {
            int totalHours = 0;
            for (var shift : context.getCurrentShiftConfigs()) {
                Integer startHour = shift.getStartHour();
                Integer endHour = shift.getEndHour();
                if (startHour != null && endHour != null) {
                    if (endHour < startHour) {
                        totalHours += (24 - startHour) + endHour;
                    } else {
                        totalHours += endHour - startHour;
                    }
                }
            }
            return totalHours > 0 ? totalHours : 24;
        }
        return 24;
    }

    /**
     * 计算机台剩余产能
     *
     * @param machine          机台信息
     * @param usedCapacity     已用产能
     * @param context          排程上下文
     * @param scheduleDate     排程日期
     * @return 剩余产能
     */
    public int calculateMachineRemainingCapacity(
            MdmMoldingMachine machine,
            int usedCapacity,
            ScheduleContextDTO context,
            LocalDate scheduleDate) {

        int dailyCapacity = calculateMachineDailyCapacity(machine, context);

        // 扣除停机产能
        int shutdownDeduction = calculateShutdownDeduction(
                machine.getCxMachineCode(), scheduleDate, context);

        // 扣除精度计划产能
        int precisionDeduction = calculatePrecisionDeduction(
                machine.getCxMachineCode(), scheduleDate, context);

        int availableCapacity = dailyCapacity - shutdownDeduction - precisionDeduction - usedCapacity;

        return Math.max(0, availableCapacity);
    }

    /**
     * 计算停机扣减产能
     */
    private int calculateShutdownDeduction(
            String machineCode,
            LocalDate scheduleDate,
            ScheduleContextDTO context) {

        if (context.getDevicePlanShuts() == null || machineCode == null) {
            return 0;
        }

        int totalDeduction = 0;
        for (var shutdown : context.getDevicePlanShuts()) {
            if (machineCode.equals(shutdown.getMachineCode())) {
                // TODO: 根据停机时间计算扣减产能
            }
        }

        return totalDeduction;
    }

    /**
     * 计算精度计划扣减产能
     */
    private int calculatePrecisionDeduction(
            String machineCode,
            LocalDate scheduleDate,
            ScheduleContextDTO context) {

        if (context.getPrecisionPlans() == null || machineCode == null) {
            return 0;
        }

        int totalDeduction = 0;
        for (var plan : context.getPrecisionPlans()) {
            if (machineCode.equals(plan.getMachineCode())) {
                // TODO: 根据精度计划计算扣减产能
            }
        }

        return totalDeduction;
    }

    /**
     * 计算开停产特殊处理后的排产量
     *
     * @param baseProduction 基础排产量
     * @param isOpeningDay   是否开产日
     * @param isClosingDay   是否停产日
     * @param isKeyProduct   是否关键产品
     * @param context        排程上下文
     * @return 处理后的排产量
     */
    public int handleOpeningClosingDay(
            int baseProduction,
            boolean isOpeningDay,
            boolean isClosingDay,
            boolean isKeyProduct,
            ScheduleContextDTO context) {

        // 停产日不排产
        if (isClosingDay) {
            return 0;
        }

        // 开产日首班不排关键产品
        if (isOpeningDay && isKeyProduct) {
            // 可以延迟到第二班排产
            return baseProduction;
        }

        return baseProduction;
    }

    /**
     * 计算试制任务排产量
     *
     * <p>试制数量必须是双数
     *
     * @param demand 需求量
     * @return 排产量（双数）
     */
    public int calculateTrialProduction(int demand) {
        if (demand <= 0) {
            return 0;
        }
        // 向上取整到双数
        return demand % 2 == 0 ? demand : demand + 1;
    }

    /**
     * 检查机台是否可用于指定结构
     *
     * @param machine       机台信息
     * @param structureName 结构名称
     * @param context       排程上下文
     * @return 是否可用
     */
    public boolean checkStructureConstraint(
            MdmMoldingMachine machine,
            String structureName,
            ScheduleContextDTO context) {

        if (machine == null || structureName == null) {
            return true;
        }

        // 检查禁用结构配置
        if (context.getMachineFixedConfigs() != null) {
            for (MdmCxMachineFixed fixed : context.getMachineFixedConfigs()) {
                if (machine.getCxMachineCode().equals(fixed.getCxMachineCode())) {
                    if (fixed.getDisableStructure() != null &&
                            fixed.getDisableStructure().contains(structureName)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * 获取可用于指定结构的机台列表
     *
     * @param structureName     结构名称
     * @param availableMachines 可用机台列表
     * @param context           排程上下文
     * @return 可用机台列表
     */
    public List<MdmMoldingMachine> getMachinesForStructure(
            String structureName,
            List<MdmMoldingMachine> availableMachines,
            ScheduleContextDTO context) {

        if (CollectionUtils.isEmpty(availableMachines)) {
            return new ArrayList<>();
        }

        List<MdmMoldingMachine> result = new ArrayList<>();

        // 优先从结构排产配置获取
        if (context.getStructureAllocationMap() != null && structureName != null) {
            List<MpCxCapacityConfiguration> configs = context.getStructureAllocationMap().get(structureName);
            if (configs != null && !configs.isEmpty()) {
                Set<String> machineCodes = configs.stream()
                        .map(MpCxCapacityConfiguration::getCxMachineCode)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                for (MdmMoldingMachine machine : availableMachines) {
                    if (machineCodes.contains(machine.getCxMachineCode())) {
                        result.add(machine);
                    }
                }

                if (!result.isEmpty()) {
                    return result;
                }
            }
        }

        // 检查机台固定配置
        for (MdmMoldingMachine machine : availableMachines) {
            if (checkStructureConstraint(machine, structureName, context)) {
                result.add(machine);
            }
        }

        return result;
    }

    /**
     * 获取机台种类上限
     *
     * @param context 排程上下文
     * @return 种类上限
     */
    public int getMaxTypesPerMachine(ScheduleContextDTO context) {
        return context.getMaxTypesPerMachine() != null
                ? context.getMaxTypesPerMachine()
                : DEFAULT_MAX_TYPES_PER_MACHINE;
    }

    /**
     * 获取整车容量
     *
     * @param structureCode 结构编码
     * @param context       排程上下文
     * @return 整车容量
     */
    public int getTripCapacity(String structureCode, ScheduleContextDTO context) {
        if (context.getStructureShiftCapacities() != null && structureCode != null) {
            for (var capacity : context.getStructureShiftCapacities()) {
                if (structureCode.equals(capacity.getStructureCode())) {
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
     * 获取损耗率
     *
     * @param context 排程上下文
     * @return 损耗率
     */
    public BigDecimal getLossRate(ScheduleContextDTO context) {
        return context.getLossRate() != null ? context.getLossRate() : DEFAULT_LOSS_RATE;
    }
}
