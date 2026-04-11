package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.entity.CxTreadParkingConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.mapper.*;
import com.zlt.aps.cx.service.ConstraintCheckService;
import com.zlt.aps.mp.api.domain.entity.MdmCxMachineFixed;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 约束校验服务实现类
 *
 * @author APS Team
 */
@Slf4j
@Service
public class ConstraintCheckServiceImpl implements ConstraintCheckService {

    @Autowired
    private MdmCxMachineFixedMapper machineFixedMapper;

    @Autowired
    private MdmStructureLhRatioMapper structureLhRatioMapper;

    @Autowired
    private CxTreadParkingConfigMapper treadParkingConfigMapper;

    @Autowired
    private CxPrecisionPlanMapper precisionPlanMapper;

    @Autowired
    private MdmMoldingMachineMapper moldingMachineMapper;

    @Autowired
    private MdmMaterialInfoMapper materialInfoMapper;

    @Override
    public ConstraintCheckResult checkAllConstraints(CxScheduleResult scheduleResult) {
        List<String> violations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 获取机台信息
        MdmMoldingMachine machine = moldingMachineMapper.selectOne(
                new LambdaQueryWrapper<MdmMoldingMachine>()
                        .eq(MdmMoldingMachine::getCxMachineCode, scheduleResult.getCxMachineCode()));

        // 获取物料信息
        MdmMaterialInfo material = materialInfoMapper.selectOne(
                new LambdaQueryWrapper<MdmMaterialInfo>()
                        .eq(MdmMaterialInfo::getMaterialCode, scheduleResult.getEmbryoCode()));

        // 1. 检查结构约束
        if (machine != null && material != null) {
            ConstraintCheckResult structureResult = checkStructureConstraint(machine, material);
            if (!structureResult.isPassed()) {
                violations.addAll(structureResult.getViolations());
            }
        }

        // 2. 检查产能约束（使用机台-结构维度产能）
        if (machine != null) {
            String structureCode = material != null ? material.getStructureName() : null;
            ConstraintCheckResult capacityResult = checkCapacityConstraint(
                    machine, structureCode, scheduleResult.getProductNum(), null);
            if (!capacityResult.isPassed()) {
                violations.addAll(capacityResult.getViolations());
            }
        }

        // 构建结果
        ConstraintCheckResult result = new ConstraintCheckResult();
        result.setPassed(violations.isEmpty());
        result.setViolations(violations);
        result.setWarnings(warnings);
        return result;
    }

    @Override
    public ConstraintCheckResult checkStructureConstraint(MdmMoldingMachine machine, MdmMaterialInfo material) {
        if (machine == null || material == null) {
            return ConstraintCheckResult.fail("机台或物料信息为空");
        }

        List<String> violations = new ArrayList<>();
        String structure = material.getStructureName();

        // 检查固定机台配置
        List<MdmCxMachineFixed> fixedConfigs = machineFixedMapper.selectList(
                new LambdaQueryWrapper<MdmCxMachineFixed>()
                        .eq(MdmCxMachineFixed::getCxMachineCode, machine.getCxMachineCode()));

        for (MdmCxMachineFixed fixed : fixedConfigs) {
            // 检查不可作业结构
            if (fixed.getDisableStructure() != null &&
                    containsValue(fixed.getDisableStructure(), structure)) {
                violations.add(String.format("机台 %s 不可作业结构 %s",
                        machine.getCxMachineCode(), structure));
            }

            // 检查不可作业SKU
            if (fixed.getDisableMaterialCode() != null &&
                    containsValue(fixed.getDisableMaterialCode(), material.getMaterialCode())) {
                violations.add(String.format("机台 %s 不可作业SKU %s",
                        machine.getCxMachineCode(), material.getMaterialCode()));
            }
        }

        // 检查硫化配比
        List<MdmStructureLhRatio> ratios = structureLhRatioMapper.selectList(
                new LambdaQueryWrapper<MdmStructureLhRatio>()
                        .eq(MdmStructureLhRatio::getStructureName, structure));

        for (MdmStructureLhRatio ratio : ratios) {
            // 检查机型是否匹配
            if (ratio.getCxMachineTypeCode() != null &&
                    !ratio.getCxMachineTypeCode().equals(machine.getCxMachineTypeCode())) {
                violations.add(String.format("结构 %s 不匹配机型 %s",
                        structure, machine.getCxMachineTypeCode()));
            }
        }

        if (violations.isEmpty()) {
            return ConstraintCheckResult.pass();
        } else {
            return ConstraintCheckResult.fail(violations);
        }
    }

    @Override
    public ConstraintCheckResult checkStockConstraint(CxStock stock, BigDecimal planQty, BigDecimal alertThreshold) {
        List<String> violations = new ArrayList<>();

        if (stock == null) {
            return ConstraintCheckResult.fail("库存信息不存在");
        }

        // 使用有效库存
        Integer effectiveStock = stock.getEffectiveStock();
        if (effectiveStock == null || effectiveStock <= 0) {
            return ConstraintCheckResult.fail("有效库存为零");
        }

        // 检查库存是否满足计划量
        if (planQty != null && planQty.compareTo(BigDecimal.valueOf(effectiveStock)) > 0) {
            violations.add(String.format("库存不足，当前有效库存: %d, 计划量: %s",
                    effectiveStock, planQty));
        }

        // 检查库存时长预警
        BigDecimal stockHours = stock.getStockHours();
        if (stockHours != null && alertThreshold != null &&
                stockHours.compareTo(alertThreshold) < 0) {
            violations.add(String.format("库存可供硫化时长低于预警阈值，当前: %.2f小时, 阈值: %.2f小时",
                    stockHours, alertThreshold));
        }

        if (violations.isEmpty()) {
            ConstraintCheckResult result = ConstraintCheckResult.pass();
            result.setDetails(String.format("库存充足，当前有效库存: %d条，可供时长: %.2f小时",
                    effectiveStock, stockHours != null ? stockHours : BigDecimal.ZERO));
            return result;
        } else {
            return ConstraintCheckResult.fail(violations);
        }
    }

    @Override
    public ConstraintCheckResult checkTreadParkingTime(String materialCode, LocalDateTime produceTime, LocalDateTime scheduleTime) {
        List<String> violations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (produceTime == null || scheduleTime == null) {
            return ConstraintCheckResult.pass();
        }

        // 计算停放时间（小时）
        long parkingHours = ChronoUnit.HOURS.between(produceTime, scheduleTime);

        // 获取胎面停放配置
        CxTreadParkingConfig config = getTreadParkingConfig(materialCode);

        if (config != null) {
            // 检查最小停放时间
            if (config.getMinParkingHours() != null && parkingHours < config.getMinParkingHours()) {
                violations.add(String.format("胎面停放时间不足，当前: %d小时，要求最少: %d小时",
                        parkingHours, config.getMinParkingHours()));
            }

            // 检查最大停放时间
            if (config.getMaxParkingHours() != null && parkingHours > config.getMaxParkingHours()) {
                warnings.add(String.format("胎面停放时间过长，当前: %d小时，建议最多: %d小时",
                        parkingHours, config.getMaxParkingHours()));
            }
        } else {
            // 默认规则：停放4小时以上
            if (parkingHours < 4) {
                violations.add(String.format("胎面停放时间不足4小时，当前: %d小时", parkingHours));
            }
        }

        // 检查是否差10分钟以内（预警）
        long parkingMinutes = ChronoUnit.MINUTES.between(produceTime, scheduleTime);
        if (config != null && config.getMinParkingHours() != null) {
            long minMinutes = config.getMinParkingHours() * 60;
            if (parkingMinutes >= minMinutes && parkingMinutes < minMinutes + 10) {
                warnings.add("胎面停放时间刚好满足，建议关注");
            }
        }

        ConstraintCheckResult result = new ConstraintCheckResult();
        result.setPassed(violations.isEmpty());
        result.setViolations(violations);
        result.setWarnings(warnings);
        return result;
    }

    @Override
    public ConstraintCheckResult checkCapacityConstraint(MdmMoldingMachine machine, BigDecimal planQty, Integer shiftHours) {
        // 向后兼容：使用机台最大日产能作为兜底
        return checkCapacityConstraint(machine, null, planQty, null);
    }

    @Override
    public ConstraintCheckResult checkCapacityConstraint(MdmMoldingMachine machine, String structureCode,
                                                         BigDecimal planQty, String shiftCode) {
        if (machine == null) {
            return ConstraintCheckResult.fail("机台信息为空");
        }

        List<String> violations = new ArrayList<>();

        // 计算机台产能：优先从机台结构产能表获取
        BigDecimal capacity;
        String capacitySource;

        if (structureCode != null && !structureCode.isEmpty()) {
            // 使用机台最大日产能兜底（废弃表 CxMachineStructureCapacity 已移除）
            capacity = machine.getMaxDayCapacity() != null
                    ? BigDecimal.valueOf(machine.getMaxDayCapacity())
                    : BigDecimal.valueOf(1200);
            capacitySource = "机台最大日产能";
        } else {
            // 无结构信息，使用机台最大日产能
            capacity = machine.getMaxDayCapacity() != null
                    ? BigDecimal.valueOf(machine.getMaxDayCapacity())
                    : BigDecimal.valueOf(1200);
            capacitySource = "机台最大日产能";
        }

        // 检查产能是否满足
        if (planQty != null && planQty.compareTo(capacity) > 0) {
            violations.add(String.format("机台产能不足，计划量: %s，最大产能: %s（来源:%s）",
                    planQty, capacity, capacitySource));
        }

        // 检查机台是否启用
        if (machine.getIsActive() == null || machine.getIsActive() != 1) {
            violations.add(String.format("机台 %s 未启用", machine.getCxMachineCode()));
        }

        if (violations.isEmpty()) {
            ConstraintCheckResult result = ConstraintCheckResult.pass();
            result.setDetails(String.format("机台产能满足，计划量: %s，最大产能: %s（来源:%s）",
                    planQty, capacity, capacitySource));
            return result;
        } else {
            return ConstraintCheckResult.fail(violations);
        }
    }

    @Override
    public ConstraintCheckResult checkTypeLimitConstraint(String machineCode, int currentTypes, String newMaterial) {
        // 每台成型机最多做4种不同的胎胚
        if (currentTypes >= 4) {
            // 检查是否为固定机台配置的物料
            List<MdmCxMachineFixed> fixedConfigs = machineFixedMapper.selectList(
                    new LambdaQueryWrapper<MdmCxMachineFixed>()
                            .eq(MdmCxMachineFixed::getCxMachineCode, machineCode));

            for (MdmCxMachineFixed fixed : fixedConfigs) {
                if (containsValue(fixed.getFixedMaterialCode(), newMaterial)) {
                    // 固定SKU不算新种类
                    return ConstraintCheckResult.pass();
                }
            }

            return ConstraintCheckResult.fail(String.format(
                    "机台 %s 已达到种类上限（4种），无法分配新物料 %s",
                    machineCode, newMaterial));
        }

        return ConstraintCheckResult.pass();
    }

    @Override
    public ConstraintCheckResult checkLhRatioConstraint(String structureName, String machineType, int lhMachineCount) {
        List<MdmStructureLhRatio> ratios = structureLhRatioMapper.selectList(
                new LambdaQueryWrapper<MdmStructureLhRatio>()
                        .eq(MdmStructureLhRatio::getStructureName, structureName));

        for (MdmStructureLhRatio ratio : ratios) {
            // 检查机型匹配
            if (ratio.getCxMachineTypeCode() != null &&
                    !ratio.getCxMachineTypeCode().equals(machineType)) {
                continue;
            }

            // 检查硫化机数量是否超过上限
            if (ratio.getLhMachineMaxQty() != null && lhMachineCount > ratio.getLhMachineMaxQty()) {
                return ConstraintCheckResult.fail(String.format(
                        "结构 %s 硫化机数量超过上限，当前: %d，上限: %d",
                        structureName, lhMachineCount, ratio.getLhMachineMaxQty()));
            }
        }

        return ConstraintCheckResult.pass();
    }

    @Override
    public ConstraintCheckResult checkKeyProductConstraint(MdmMaterialInfo material, boolean isOpeningDay, boolean isFirstShift) {
        if (!isOpeningDay || !isFirstShift) {
            return ConstraintCheckResult.pass();
        }

        // 检查是否为关键产品
        // 注意：MdmMaterialInfo 没有直接的关键产品标识
        // 实际应根据关键产品配置表判断（context.getKeyProductCodes()）
        // 这里暂时通过配置判断，需要在调用时传入关键产品编码集合
        return ConstraintCheckResult.pass();
    }

    @Override
    public ConstraintCheckResult checkTrialConstraint(LocalDateTime scheduleDate, int trialTaskCount, String shiftCode, int quantity) {
        List<String> violations = new ArrayList<>();

        // 检查周日
        if (scheduleDate != null && scheduleDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            violations.add("周日不安排试制");
        }

        // 检查一天最多2个新胎胚
        if (trialTaskCount >= 2) {
            violations.add("当天已安排2个试制任务，无法再安排");
        }

        // 检查班次（只能安排在早班或中班）
        if (shiftCode != null && "SHIFT_NIGHT".equals(shiftCode)) {
            violations.add("试制任务只能安排在早班或中班");
        }

        // 检查数量必须是双数
        if (quantity % 2 != 0) {
            violations.add("试制数量必须是双数");
        }

        if (violations.isEmpty()) {
            return ConstraintCheckResult.pass();
        } else {
            return ConstraintCheckResult.fail(violations);
        }
    }


    @Override
    public ConstraintCheckResult checkEndingConstraint(MdmMaterialInfo material, CxStock stock, BigDecimal remainingQty) {
        if (remainingQty == null || remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
            return ConstraintCheckResult.pass();
        }

        List<String> warnings = new ArrayList<>();

        // 检查是否为主销产品
        // 注意：MdmMaterialInfo 没有直接的主销产品标识
        // 实际应根据主销产品配置表判断（context.getMainProductCodes()）
        // 这里暂时不判断主销产品，统一按非主销产品处理

        // 非主销产品：收尾余量≤2条时舍弃，>2条时按实际量下
        if (remainingQty.compareTo(BigDecimal.valueOf(2)) <= 0) {
            warnings.add(String.format("收尾余量 %s ≤2条，建议舍弃", remainingQty));
        }

        if (!warnings.isEmpty()) {
            ConstraintCheckResult result = ConstraintCheckResult.pass();
            result.setWarnings(warnings);
            return result;
        }

        return ConstraintCheckResult.pass();
    }

    @Override
    public CxTreadParkingConfig getTreadParkingConfig(String structureCode) {
        return treadParkingConfigMapper.selectOne(
                new LambdaQueryWrapper<CxTreadParkingConfig>()
                        .eq(CxTreadParkingConfig::getStructureCode, structureCode)
                        .eq(CxTreadParkingConfig::getIsEnabled, 1)
                        .last("LIMIT 1"));
    }

    /**
     * 检查逗号分隔的字符串中是否包含指定值
     */
    private boolean containsValue(String commaSeparated, String value) {
        if (commaSeparated == null || value == null) {
            return false;
        }
        String[] values = commaSeparated.split(",");
        for (String v : values) {
            if (v.trim().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
