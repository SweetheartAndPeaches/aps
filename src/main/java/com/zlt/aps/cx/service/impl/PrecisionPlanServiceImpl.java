package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cx.api.domain.entity.CxPrecisionPlan;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.cx.mapper.CxPrecisionPlanMapper;
import com.zlt.aps.cx.mapper.CxStockMapper;
import com.zlt.aps.cx.mapper.MdmMoldingMachineMapper;
import com.zlt.aps.cx.service.PrecisionPlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 精度计划服务实现类
 * 
 * 精度计划业务规则：
 * 1. 每个机台每两个月做一次精度校验，每次4小时
 * 2. 正常提前3天安排
 * 3. 一天最多做2台
 * 4. 精度期间成型机停机，影响排程产能
 * 5. 硫化机影响判断：
 *    - 胎胚库存够硫化机吃4小时以上 → 硫化机继续生产
 *    - 不够 → 硫化机减产一半
 *
 * 实体字段说明：
 * - precisionType: 精度类型
 * - precisionCycle: 周期（15/60分钟）
 * - completionStatus: 完成情况（0-未完成，1-已完成）
 * - planDate: 计划日期
 * - scheduleDate: 排程日期（硫化排程回填）
 * - actualDate: 实际执行日期
 * - dueDate: 到期日期
 *
 * @author APS Team
 */
@Slf4j
@Service
public class PrecisionPlanServiceImpl extends ServiceImpl<CxPrecisionPlanMapper, CxPrecisionPlan> 
        implements PrecisionPlanService {

    /** 精度周期（月） */
    private static final int PRECISION_CYCLE_MONTHS = 2;

    /** 精度时长（小时） */
    private static final int PRECISION_HOURS = 4;

    /** 提前安排天数 */
    private static final int ADVANCE_DAYS = 3;

    /** 每天最多安排台数 */
    private static final int MAX_PLANS_PER_DAY = 2;

    /** 成型机小时产能（条/小时）默认值 */
    private static final int DEFAULT_MACHINE_HOURLY_CAPACITY = 50;

    /** 硫化机小时产能（条/小时）默认值 */
    private static final int DEFAULT_VULCANIZE_HOURLY_CAPACITY = 30;

    @Autowired
    private MdmMoldingMachineMapper moldingMachineMapper;

    @Autowired
    private CxStockMapper stockMapper;

    @Override
    public List<CxPrecisionPlan> getByDate(LocalDate planDate) {
        LambdaQueryWrapper<CxPrecisionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxPrecisionPlan::getPlanDate, java.sql.Date.valueOf(planDate))
               .ne(CxPrecisionPlan::getCompletionStatus, "1")
               .orderByAsc(CxPrecisionPlan::getPlanDate);
        return list(wrapper);
    }

    @Override
    public CxPrecisionPlan getByMachineAndDate(String machineCode, LocalDate planDate) {
        LambdaQueryWrapper<CxPrecisionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxPrecisionPlan::getMachineCode, machineCode)
               .eq(CxPrecisionPlan::getPlanDate, java.sql.Date.valueOf(planDate))
               .ne(CxPrecisionPlan::getCompletionStatus, "1")
               .last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoGeneratePlans(LocalDate startDate, LocalDate endDate) {
        int count = 0;

        // 获取所有启用的成型机台
        List<MdmMoldingMachine> machines = moldingMachineMapper.selectList(
                new LambdaQueryWrapper<MdmMoldingMachine>()
                        .eq(MdmMoldingMachine::getIsActive, 1));

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            // 检查当天已安排数量
            long todayCount = countByDate(date);
            if (todayCount >= MAX_PLANS_PER_DAY) {
                continue;
            }

            for (MdmMoldingMachine machine : machines) {
                // 检查是否需要安排精度
                if (needsPrecisionPlan(machine, date)) {
                    // 检查是否已存在计划
                    if (getByMachineAndDate(machine.getCxMachineCode(), date) != null) {
                        continue;
                    }

                    // 创建精度计划
                    CxPrecisionPlan plan = createPrecisionPlan(machine, date);
                    save(plan);
                    count++;

                    log.info("自动生成精度计划：机台={}, 日期={}, 精度类型={}, 精度周期={}分钟", 
                            machine.getCxMachineCode(), date, plan.getPrecisionType(), plan.getPrecisionCycle());

                    // 检查当天是否已达上限
                    if (countByDate(date) >= MAX_PLANS_PER_DAY) {
                        break;
                    }
                }
            }
        }

        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean arrangePlanShift(Long planId) {
        CxPrecisionPlan plan = getById(planId);
        if (plan == null) {
            return false;
        }

        // 精度计划不区分班次，只标记为未完成即可
        // 排程时根据 planDate 和 completionStatus 判断是否影响产能
        plan.setCompletionStatus("0");
        plan.setUpdateTime(new Date());

        return updateById(plan);
    }

    @Override
    public BigDecimal calculateVulcanizeReduceRatio(String machineCode, LocalDate planDate) {
        CxPrecisionPlan plan = getByMachineAndDate(machineCode, planDate);
        if (plan == null) {
            return BigDecimal.ZERO;
        }

        // 已完成的精度计划不影响硫化
        if ("1".equals(plan.getCompletionStatus())) {
            return BigDecimal.ZERO;
        }

        // 获取该机台关联胎胚库存来判断硫化影响
        // 精度计划没有 embryoCode 字段，需要通过机台在产信息获取胎胚
        // 简化处理：默认精度期间硫化机减产一半
        return new BigDecimal("0.5");
    }

    @Override
    public List<String> getUnavailableMachines(LocalDate planDate, String shiftCode) {
        List<CxPrecisionPlan> plans = getByDate(planDate);
        
        // 精度计划不区分班次，只要当天有未完成的精度计划，该机台就不可用
        return plans.stream()
                .filter(p -> !"1".equals(p.getCompletionStatus()))
                .map(CxPrecisionPlan::getMachineCode)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Integer> getCapacityDeduction(LocalDate planDate) {
        Map<String, Integer> result = new HashMap<>();
        
        List<CxPrecisionPlan> plans = getByDate(planDate);
        for (CxPrecisionPlan plan : plans) {
            // 已完成的不扣减产能
            if ("1".equals(plan.getCompletionStatus())) {
                continue;
            }
            
            // 根据 precisionCycle 计算扣减产能
            // precisionCycle: 15=15分钟, 60=60分钟
            int precisionMinutes = 60; // 默认60分钟
            if ("15".equals(plan.getPrecisionCycle())) {
                precisionMinutes = 15;
            } else if ("60".equals(plan.getPrecisionCycle())) {
                precisionMinutes = 60;
            }
            
            // 扣减产能 = 精度时长(小时) × 机台小时产能
            int precisionHours = (int) Math.ceil(precisionMinutes / 60.0);
            if (precisionHours < PRECISION_HOURS) {
                precisionHours = PRECISION_HOURS; // 至少扣减4小时（精度校验标准时长）
            }
            int deduction = precisionHours * DEFAULT_MACHINE_HOURLY_CAPACITY;
            result.put(plan.getMachineCode(), deduction);
        }
        
        return result;
    }

    @Override
    public boolean isInPrecisionPeriod(String machineCode, LocalDate planDate, String shiftCode) {
        CxPrecisionPlan plan = getByMachineAndDate(machineCode, planDate);
        if (plan == null) {
            return false;
        }

        // 已完成的不影响
        if ("1".equals(plan.getCompletionStatus())) {
            return false;
        }

        // 精度计划不区分班次，只要当天有未完成的精度计划就影响
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateStatus(List<Long> planIds, String status) {
        if (planIds == null || planIds.isEmpty()) {
            return 0;
        }

        boolean success = update(new LambdaUpdateWrapper<CxPrecisionPlan>()
                .in(CxPrecisionPlan::getId, planIds)
                .set(CxPrecisionPlan::getCompletionStatus, status)
                .set(CxPrecisionPlan::getUpdateTime, new Date()));
        return success ? planIds.size() : 0;
    }

    @Override
    public List<MdmMoldingMachine> getMachinesDueForPrecision(int days) {
        LocalDate targetDate = LocalDate.now().plusDays(days);
        
        List<MdmMoldingMachine> allMachines = moldingMachineMapper.selectList(
                new LambdaQueryWrapper<MdmMoldingMachine>()
                        .eq(MdmMoldingMachine::getIsActive, 1));

        return allMachines.stream()
                .filter(m -> needsPrecisionPlan(m, targetDate))
                .collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    /**
     * 检查机台是否需要安排精度计划
     * 根据上次已完成精度计划的 planDate + 精度周期判断
     */
    private boolean needsPrecisionPlan(MdmMoldingMachine machine, LocalDate targetDate) {
        // 获取该机台最近一次已完成的精度计划
        CxPrecisionPlan lastCompletedPlan = getOne(
                new LambdaQueryWrapper<CxPrecisionPlan>()
                        .eq(CxPrecisionPlan::getMachineCode, machine.getCxMachineCode())
                        .eq(CxPrecisionPlan::getCompletionStatus, "1")
                        .orderByDesc(CxPrecisionPlan::getPlanDate)
                        .last("LIMIT 1"));

        if (lastCompletedPlan == null) {
            // 从未做过精度，需要安排
            return true;
        }

        // 检查是否已过精度周期
        LocalDate lastPlanDate = lastCompletedPlan.getPlanDate() != null
                ? lastCompletedPlan.getPlanDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                : null;
        if (lastPlanDate == null) {
            return true;
        }

        LocalDate nextDueDate = lastPlanDate.plusMonths(PRECISION_CYCLE_MONTHS);
        LocalDate arrangeDate = nextDueDate.minusDays(ADVANCE_DAYS);

        return !targetDate.isBefore(arrangeDate) && targetDate.isBefore(nextDueDate);
    }

    /**
     * 统计指定日期的精度计划数量
     */
    private long countByDate(LocalDate date) {
        return count(new LambdaQueryWrapper<CxPrecisionPlan>()
                .eq(CxPrecisionPlan::getPlanDate, java.sql.Date.valueOf(date))
                .ne(CxPrecisionPlan::getCompletionStatus, "1"));
    }

    /**
     * 创建精度计划
     */
    private CxPrecisionPlan createPrecisionPlan(MdmMoldingMachine machine, LocalDate planDate) {
        CxPrecisionPlan plan = new CxPrecisionPlan();
        plan.setMachineCode(machine.getCxMachineCode());
        plan.setPlanDate(java.sql.Date.valueOf(planDate));
        plan.setPrecisionType("STANDARD"); // 默认标准精度类型
        plan.setPrecisionCycle("60"); // 默认60分钟周期
        plan.setCompletionStatus("0");
        plan.setWarningStatus("0");
        plan.setIsWarningSent("0");
        plan.setDataSource("1"); // 系统自动生成

        // 设置上次保养日期（查询最近一次已完成的精度计划）
        CxPrecisionPlan lastCompletedPlan = getOne(
                new LambdaQueryWrapper<CxPrecisionPlan>()
                        .eq(CxPrecisionPlan::getMachineCode, machine.getCxMachineCode())
                        .eq(CxPrecisionPlan::getCompletionStatus, "1")
                        .orderByDesc(CxPrecisionPlan::getPlanDate)
                        .last("LIMIT 1"));
        if (lastCompletedPlan != null) {
            plan.setLastMaintenanceDate(lastCompletedPlan.getActualDate());
        }

        // 设置到期日期
        if (lastCompletedPlan != null && lastCompletedPlan.getPlanDate() != null) {
            LocalDate lastPlanDate = lastCompletedPlan.getPlanDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            plan.setDueDate(java.sql.Date.valueOf(lastPlanDate.plusMonths(PRECISION_CYCLE_MONTHS)));
        } else {
            plan.setDueDate(java.sql.Date.valueOf(planDate.plusMonths(PRECISION_CYCLE_MONTHS)));
        }

        // 设置计划年度
        plan.setYear(BigDecimal.valueOf(planDate.getYear()));

        plan.setCreateTime(new Date());
        plan.setUpdateTime(new Date());

        return plan;
    }

    /**
     * 计算胎胚库存可支持的硫化时长（小时）
     */
    private BigDecimal calculateStockHours(CxStock stock) {
        if (stock == null || stock.getStockNum() == null || stock.getStockNum() <= 0) {
            return BigDecimal.ZERO;
        }

        // 有效库存 = 库存量 - 超期库存 - 不良数量 + 修正数量
        Integer effectiveStock = stock.getEffectiveStock();
        if (effectiveStock <= 0) {
            return BigDecimal.ZERO;
        }

        // 库存时长 = 有效库存 / 硫化机小时产能
        return new BigDecimal(effectiveStock)
                .divide(new BigDecimal(DEFAULT_VULCANIZE_HOURLY_CAPACITY), 2, RoundingMode.HALF_UP);
    }
}
