package com.zlt.aps.cx.service.impl;

import com.zlt.aps.cx.entity.CxMaterial;
import com.zlt.aps.cx.entity.CxStock;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.entity.mdm.MdmMoldingMachine;
import com.zlt.aps.cx.service.AlgorithmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 排程算法服务实现类
 *
 * @author APS Team
 */
@Slf4j
@Service
public class AlgorithmServiceImpl implements AlgorithmService {

    @Override
    public List<CxScheduleResult> executeScheduleAlgorithm(
            LocalDate scheduleDate,
            List<MdmMoldingMachine> machines,
            List<CxMaterial> materials,
            List<CxStock> stocks) {
        
        log.info("执行排程算法，日期: {}, 机台数: {}, 物料数: {}", 
                scheduleDate, machines.size(), materials.size());

        List<CxScheduleResult> results = new ArrayList<>();

        // 1. 按优先级排序SKU
        List<String> sortedSkuCodes = sortSkuPriority(materials, stocks);
        log.debug("SKU优先级排序完成，数量: {}", sortedSkuCodes.size());

        // 2. 构建物料和库存的映射
        Map<String, CxMaterial> materialMap = materials.stream()
                .collect(Collectors.toMap(CxMaterial::getMaterialCode, m -> m));
        Map<String, CxStock> stockMap = stocks.stream()
                .collect(Collectors.toMap(CxStock::getMaterialCode, s -> s));

        // 3. 可用机台列表（按状态过滤）
        List<MdmMoldingMachine> availableMachines = machines.stream()
                .filter(m -> m.getIsActive() != null && m.getIsActive() == 1)
                .filter(m -> "RUNNING".equals(m.getMaintainStatus()) || m.getMaintainStatus() == null)
                .collect(Collectors.toList());

        // 4. 为每个SKU分配机台并生成排程
        Set<String> usedMachines = new HashSet<>();
        
        for (String skuCode : sortedSkuCodes) {
            CxMaterial material = materialMap.get(skuCode);
            CxStock stock = stockMap.get(skuCode);
            
            if (material == null) {
                log.debug("物料 {} 不存在，跳过", skuCode);
                continue;
            }

            // 分配机台
            MdmMoldingMachine assignedMachine = allocateMachine(material, availableMachines, stock);
            if (assignedMachine == null) {
                log.debug("物料 {} 无可用机台，跳过", skuCode);
                continue;
            }

            // 检查结构约束
            if (!checkStructureConstraint(assignedMachine, material.getProductStructure())) {
                log.debug("物料 {} 结构约束不满足，跳过", skuCode);
                continue;
            }

            // 计算计划量
            BigDecimal totalPlanQty = calculateTotalPlanQty(material, stock, assignedMachine);
            if (totalPlanQty.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("物料 {} 计划量为零，跳过", skuCode);
                continue;
            }

            // 计算班次计划量
            List<String> shifts = Arrays.asList("SHIFT1", "SHIFT2", "SHIFT3", "SHIFT4", 
                    "SHIFT5", "SHIFT6", "SHIFT7", "SHIFT8");
            Map<String, BigDecimal> shiftPlanQty = calculateShiftPlanQty(
                    totalPlanQty, shifts, assignedMachine.getProductionCapacity());

            // 创建排程结果
            CxScheduleResult result = createScheduleResult(
                    scheduleDate, assignedMachine, material, stock, totalPlanQty, shiftPlanQty);
            
            results.add(result);
            usedMachines.add(assignedMachine.getCxMachineCode());
        }

        log.info("排程算法执行完成，生成 {} 条排程记录", results.size());
        return results;
    }

    @Override
    public MdmMoldingMachine allocateMachine(CxMaterial material, List<MdmMoldingMachine> machines, CxStock stock) {
        if (machines == null || machines.isEmpty() || material == null) {
            return null;
        }

        String structure = material.getProductStructure();
        
        // 优先选择固定生产该结构的机台
        for (MdmMoldingMachine machine : machines) {
            if (checkStructureConstraint(machine, structure)) {
                // 检查是否为固定机台
                if (isFixedMachine(machine, structure)) {
                    return machine;
                }
            }
        }

        // 其次选择可生产该结构的机台（按产能排序）
        return machines.stream()
                .filter(m -> checkStructureConstraint(m, structure))
                .max(Comparator.comparing(MdmMoldingMachine::getProductionCapacity))
                .orElse(null);
    }

    @Override
    public Map<String, BigDecimal> calculateShiftPlanQty(
            BigDecimal totalPlanQty,
            List<String> shifts,
            BigDecimal machineCapacity) {
        
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        
        if (totalPlanQty == null || totalPlanQty.compareTo(BigDecimal.ZERO) <= 0) {
            shifts.forEach(shift -> result.put(shift, BigDecimal.ZERO));
            return result;
        }

        // 每班次时长（假设3小时）
        int shiftHours = 3;
        
        // 每班产能 = 机台产能 * 班次时长
        BigDecimal shiftCapacity = machineCapacity != null 
                ? machineCapacity.multiply(BigDecimal.valueOf(shiftHours))
                : BigDecimal.valueOf(50); // 默认每班50条

        // 平均分配到各班次
        BigDecimal avgPerShift = totalPlanQty.divide(
                BigDecimal.valueOf(shifts.size()), 0, RoundingMode.DOWN);
        
        BigDecimal remaining = totalPlanQty;
        for (int i = 0; i < shifts.size(); i++) {
            String shift = shifts.get(i);
            BigDecimal planQty;
            
            if (i == shifts.size() - 1) {
                // 最后一班取剩余量
                planQty = remaining;
            } else {
                planQty = avgPerShift.min(shiftCapacity).min(remaining);
            }
            
            result.put(shift, planQty);
            remaining = remaining.subtract(planQty);
            
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }

        // 填充剩余班次为0
        for (String shift : shifts) {
            if (!result.containsKey(shift)) {
                result.put(shift, BigDecimal.ZERO);
            }
        }

        return result;
    }

    @Override
    public BigDecimal calculateStockHours(
            CxStock stock,
            Integer vulcanizeMachineCount,
            Integer vulcanizeMoldCount,
            BigDecimal vulcanizeTimeMinutes) {
        
        if (stock == null || stock.getCurrentStock() == null || stock.getCurrentStock() <= 0) {
            return BigDecimal.ZERO;
        }

        if (vulcanizeMachineCount == null || vulcanizeMachineCount == 0 ||
            vulcanizeMoldCount == null || vulcanizeMoldCount == 0 ||
            vulcanizeTimeMinutes == null || vulcanizeTimeMinutes.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 硫化时间转换为小时
        BigDecimal vulcanizeTimeHours = vulcanizeTimeMinutes.divide(
                BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

        // 单班产能 = 硫化机台数 * 总模数 * 单班硫化量
        // 这里简化计算：假设单班硫化量 = 8小时 / 硫化时间(小时)
        BigDecimal shiftCapacity = BigDecimal.valueOf(vulcanizeMachineCount)
                .multiply(BigDecimal.valueOf(vulcanizeMoldCount))
                .multiply(BigDecimal.valueOf(8).divide(vulcanizeTimeHours, 2, RoundingMode.HALF_UP));

        // 库存时长 = 库存 / 单班产能
        return BigDecimal.valueOf(stock.getCurrentStock())
                .divide(shiftCapacity, 2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateMachineCapacity(MdmMoldingMachine machine, CxMaterial material, Integer shiftHours) {
        if (machine == null || machine.getProductionCapacity() == null) {
            return BigDecimal.valueOf(50 * (shiftHours != null ? shiftHours : 3)); // 默认每班50条
        }
        return machine.getProductionCapacity().multiply(BigDecimal.valueOf(shiftHours != null ? shiftHours : 3));
    }

    @Override
    public List<String> sortSkuPriority(List<CxMaterial> materials, List<CxStock> stocks) {
        if (materials == null || materials.isEmpty()) {
            return new ArrayList<>();
        }

        // 构建库存映射
        Map<String, CxStock> stockMap = stocks != null 
                ? stocks.stream().collect(Collectors.toMap(CxStock::getMaterialCode, s -> s, (a, b) -> a))
                : new HashMap<>();

        // 排序规则：
        // 1. 库存预警优先（库存时长最短的优先）
        // 2. 主销产品优先
        // 3. 其他产品
        return materials.stream()
                .sorted((m1, m2) -> {
                    CxStock s1 = stockMap.get(m1.getMaterialCode());
                    CxStock s2 = stockMap.get(m2.getMaterialCode());

                    // 库存预警优先
                    int alertCompare = compareStockAlert(s1, s2);
                    if (alertCompare != 0) {
                        return alertCompare;
                    }

                    // 主销产品优先
                    int mainProductCompare = compareMainProduct(m1, m2);
                    if (mainProductCompare != 0) {
                        return mainProductCompare;
                    }

                    // 按编码排序
                    return m1.getMaterialCode().compareTo(m2.getMaterialCode());
                })
                .map(CxMaterial::getMaterialCode)
                .collect(Collectors.toList());
    }

    @Override
    public boolean checkStructureConstraint(MdmMoldingMachine machine, String structure) {
        if (machine == null || structure == null) {
            return false;
        }

        // 检查机台是否有限制结构
        // 如果机台有固定结构配置，需要匹配
        // 这里简化处理，实际应根据业务规则判断
        return true;
    }

    @Override
    public StockConstraintResult checkStockConstraint(CxStock stock, BigDecimal planQty, BigDecimal alertThreshold) {
        StockConstraintResult result = new StockConstraintResult();
        
        if (stock == null) {
            result.setPassed(false);
            result.setReason("库存信息不存在");
            return result;
        }

        if (stock.getCurrentStock() == null || stock.getCurrentStock() <= 0) {
            result.setPassed(false);
            result.setReason("库存为零");
            return result;
        }

        // 计算库存可供硫化时长
        BigDecimal stockHours = stock.getStockHours();
        result.setAvailableHours(stockHours != null ? stockHours : BigDecimal.ZERO);

        // 检查是否低于预警阈值
        if (stockHours != null && alertThreshold != null && 
            stockHours.compareTo(alertThreshold) < 0) {
            result.setPassed(false);
            result.setReason("库存可供硫化时长低于预警阈值");
        } else {
            result.setPassed(true);
        }

        return result;
    }

    @Override
    public List<CxScheduleDetail> generateScheduleDetails(
            CxScheduleResult scheduleResult,
            Map<String, ShiftConfig> shifts) {
        
        List<CxScheduleDetail> details = new ArrayList<>();
        
        if (scheduleResult == null || shifts == null || shifts.isEmpty()) {
            return details;
        }

        int globalSequence = 1;
        LocalDate scheduleDate = scheduleResult.getScheduleDate() != null 
                ? scheduleResult.getScheduleDate().toLocalDate() 
                : LocalDate.now();

        // 为每个班次生成明细
        for (Map.Entry<String, ShiftConfig> entry : shifts.entrySet()) {
            String shiftCode = entry.getKey();
            ShiftConfig shift = entry.getValue();

            BigDecimal planQty = getShiftPlanQty(scheduleResult, shiftCode);
            if (planQty == null || planQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 计算车次数（假设每车50条）
            int tripCapacity = 50;
            int tripCount = planQty.divide(BigDecimal.valueOf(tripCapacity), 0, RoundingMode.CEILING).intValue();

            for (int tripNo = 1; tripNo <= tripCount; tripNo++) {
                CxScheduleDetail detail = new CxScheduleDetail();
                detail.setMainId(scheduleResult.getId());
                detail.setScheduleDate(scheduleDate);
                detail.setShiftCode(shiftCode);
                detail.setCxMachineCode(scheduleResult.getCxMachineCode());
                detail.setCxMachineName(scheduleResult.getCxMachineName());
                detail.setEmbryoCode(scheduleResult.getEmbryoCode());
                detail.setTripNo(tripNo);
                detail.setTripCapacity(tripCapacity);
                detail.setTripActualQty(0);
                detail.setSequence(globalSequence++);
                detail.setSequenceInGroup(tripNo);
                detail.setIsEnding(0);
                detail.setIsTrial(0);
                detail.setIsPrecision(0);
                detail.setIsContinue(0);

                // 计算计划时间
                LocalDateTime planStartTime = calculatePlanStartTime(scheduleDate, shift, tripNo);
                LocalDateTime planEndTime = planStartTime.plusMinutes(30); // 假设每车30分钟
                detail.setPlanStartTime(planStartTime);
                detail.setPlanEndTime(planEndTime);

                details.add(detail);
            }
        }

        return details;
    }

    @Override
    public boolean linkLhSchedule(CxScheduleResult cxScheduleResult, List<LhScheduleResult> lhSchedules) {
        if (cxScheduleResult == null || lhSchedules == null || lhSchedules.isEmpty()) {
            return false;
        }

        // 关联硫化排程ID
        String lhScheduleIds = lhSchedules.stream()
                .map(lh -> String.valueOf(lh.getId()))
                .collect(Collectors.joining(","));
        
        cxScheduleResult.setLhScheduleIds(lhScheduleIds);
        
        return true;
    }

    @Override
    public boolean calculateEndingTip(CxScheduleResult scheduleResult, BigDecimal remainingQty, CxStock stock) {
        if (scheduleResult == null || stock == null) {
            return false;
        }

        // 如果剩余量小于库存，且库存预警状态为收尾，则需要收尾提示
        if (remainingQty != null && remainingQty.compareTo(BigDecimal.ZERO) > 0) {
            if (stock.getIsEndingSku() != null && stock.getIsEndingSku() == 1) {
                return true;
            }
            
            // 检查库存是否低于某个阈值
            if (stock.getCurrentStock() != null && stock.getCurrentStock() < 100) {
                return true;
            }
        }

        return false;
    }

    // ==================== 私有方法 ====================

    /**
     * 计算总计划量
     */
    private BigDecimal calculateTotalPlanQty(CxMaterial material, CxStock stock, MdmMoldingMachine machine) {
        if (stock == null || stock.getCurrentStock() == null || stock.getCurrentStock() <= 0) {
            return BigDecimal.ZERO;
        }

        // 根据库存和机台产能计算计划量
        BigDecimal capacity = machine.getProductionCapacity() != null 
                ? machine.getProductionCapacity().multiply(BigDecimal.valueOf(24)) // 日产能
                : BigDecimal.valueOf(1200); // 默认1200条/天

        // 取库存和产能的较小值
        return BigDecimal.valueOf(stock.getCurrentStock()).min(capacity);
    }

    /**
     * 创建排程结果
     */
    private CxScheduleResult createScheduleResult(
            LocalDate scheduleDate,
            MdmMoldingMachine machine,
            CxMaterial material,
            CxStock stock,
            BigDecimal totalPlanQty,
            Map<String, BigDecimal> shiftPlanQty) {
        
        CxScheduleResult result = new CxScheduleResult();
        result.setScheduleDate(scheduleDate.atStartOfDay());
        result.setCxMachineCode(machine.getCxMachineCode());
        result.setCxMachineName(machine.getCxMachineName());
        result.setCxMachineType(machine.getCxMachineTypeName());
        result.setEmbryoCode(material.getMaterialCode());
        result.setStructureName(material.getProductStructure());
        result.setSpecDesc(material.getSpecification());
        result.setProductNum(totalPlanQty);
        result.setTotalStock(stock != null ? BigDecimal.valueOf(stock.getCurrentStock()) : BigDecimal.ZERO);
        result.setProductionStatus("0");
        result.setIsRelease("0");
        result.setDataSource("0");

        // 设置班次计划量
        shiftPlanQty.forEach((shift, qty) -> {
            switch (shift) {
                case "SHIFT1": result.setClass1PlanQty(qty); break;
                case "SHIFT2": result.setClass2PlanQty(qty); break;
                case "SHIFT3": result.setClass3PlanQty(qty); break;
                case "SHIFT4": result.setClass4PlanQty(qty); break;
                case "SHIFT5": result.setClass5PlanQty(qty); break;
                case "SHIFT6": result.setClass6PlanQty(qty); break;
                case "SHIFT7": result.setClass7PlanQty(qty); break;
                case "SHIFT8": result.setClass8PlanQty(qty); break;
            }
        });

        return result;
    }

    /**
     * 检查是否为固定机台
     */
    private boolean isFixedMachine(MdmMoldingMachine machine, String structure) {
        // 简化处理，实际应根据固定配置判断
        return false;
    }

    /**
     * 比较库存预警级别
     */
    private int compareStockAlert(CxStock s1, CxStock s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return 1;
        if (s2 == null) return -1;

        // 预警状态优先级: LOW > NORMAL > HIGH
        String status1 = s1.getAlertStatus() != null ? s1.getAlertStatus() : "NORMAL";
        String status2 = s2.getAlertStatus() != null ? s2.getAlertStatus() : "NORMAL";

        if ("LOW".equals(status1) && !"LOW".equals(status2)) return -1;
        if (!"LOW".equals(status1) && "LOW".equals(status2)) return 1;

        // 按库存时长排序
        BigDecimal hours1 = s1.getStockHours() != null ? s1.getStockHours() : BigDecimal.valueOf(999);
        BigDecimal hours2 = s2.getStockHours() != null ? s2.getStockHours() : BigDecimal.valueOf(999);

        return hours1.compareTo(hours2);
    }

    /**
     * 比较主销产品优先级
     */
    private int compareMainProduct(CxMaterial m1, CxMaterial m2) {
        int isMain1 = m1.getIsMainProduct() != null ? m1.getIsMainProduct() : 0;
        int isMain2 = m2.getIsMainProduct() != null ? m2.getIsMainProduct() : 0;
        return Integer.compare(isMain2, isMain1); // 主销产品优先
    }

    /**
     * 获取班次计划量
     */
    private BigDecimal getShiftPlanQty(CxScheduleResult result, String shiftCode) {
        switch (shiftCode) {
            case "SHIFT1": return result.getClass1PlanQty();
            case "SHIFT2": return result.getClass2PlanQty();
            case "SHIFT3": return result.getClass3PlanQty();
            case "SHIFT4": return result.getClass4PlanQty();
            case "SHIFT5": return result.getClass5PlanQty();
            case "SHIFT6": return result.getClass6PlanQty();
            case "SHIFT7": return result.getClass7PlanQty();
            case "SHIFT8": return result.getClass8PlanQty();
            default: return BigDecimal.ZERO;
        }
    }

    /**
     * 计算计划开始时间
     */
    private LocalDateTime calculatePlanStartTime(LocalDate scheduleDate, ShiftConfig shift, int tripNo) {
        LocalTime startTime = LocalTime.of(shift.getStartHour(), 0);
        // 每车约30分钟
        return LocalDateTime.of(scheduleDate, startTime.plusMinutes((tripNo - 1) * 30L));
    }
}
