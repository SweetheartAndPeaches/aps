package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.mdm.api.domain.entity.CxPrecisionPlan;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 精度计划服务实现类
 * 
 * 精度计划业务规则：
 * 1. 每个机台每两个月做一次精度校验，每次4小时
 * 2. 正常提前3天安排
 * 3. 一天最多做2台
 * 4. 安排时段选择：
 *    - 胎胚库存够吃超过一个班 → 早班（7:30-11:30）
 *    - 特殊情况 → 中班（13:00-17:00）
 * 5. 精度期间成型机停机
 * 6. 硫化机影响判断：
 *    - 胎胚库存够硫化机吃4小时以上 → 硫化机继续生产
 *    - 不够 → 硫化机减产一半
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

    /** 早班开始时间 */
    private static final LocalTime MORNING_SHIFT_START = LocalTime.of(7, 30);

    /** 早班结束时间 */
    private static final LocalTime MORNING_SHIFT_END = LocalTime.of(11, 30);

    /** 中班开始时间 */
    private static final LocalTime AFTERNOON_SHIFT_START = LocalTime.of(13, 0);

    /** 中班结束时间 */
    private static final LocalTime AFTERNOON_SHIFT_END = LocalTime.of(17, 0);

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
        wrapper.eq(CxPrecisionPlan::getPlanDate, planDate)
               .ne(CxPrecisionPlan::getStatus, "CANCELLED")
               .orderByAsc(CxPrecisionPlan::getPlanStartTime);
        return list(wrapper);
    }

    @Override
    public CxPrecisionPlan getByMachineAndDate(String machineCode, LocalDate planDate) {
        LambdaQueryWrapper<CxPrecisionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxPrecisionPlan::getMachineCode, machineCode)
               .eq(CxPrecisionPlan::getPlanDate, planDate)
               .ne(CxPrecisionPlan::getStatus, "CANCELLED")
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

                    log.info("自动生成精度计划：机台={}, 日期={}, 班次={}", 
                            machine.getCxMachineCode(), date, plan.getPlanShift());

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

        // 获取该机台主要生产的胎胚库存
        String embryoCode = plan.getEmbryoCode();
        if (embryoCode == null) {
            // 如果没有关联胎胚，默认安排早班
            plan.setPlanShift("SHIFT_DAY");
            plan.setPlanStartTime(LocalDateTime.of(plan.getPlanDate(), MORNING_SHIFT_START));
            plan.setPlanEndTime(LocalDateTime.of(plan.getPlanDate(), MORNING_SHIFT_END));
        } else {
            // 根据胎胚库存判断
            CxStock stock = stockMapper.selectOne(
                    new LambdaQueryWrapper<CxStock>()
                            .eq(CxStock::getEmbryoCode, embryoCode));

            // 计算库存可支持的硫化时长（小时）
            BigDecimal stockHours = calculateStockHours(stock);

            // 一个班次约8小时，如果库存够吃超过一个班，安排早班
            if (stockHours.compareTo(BigDecimal.valueOf(8)) > 0) {
                plan.setPlanShift("SHIFT_DAY");
                plan.setPlanStartTime(LocalDateTime.of(plan.getPlanDate(), MORNING_SHIFT_START));
                plan.setPlanEndTime(LocalDateTime.of(plan.getPlanDate(), MORNING_SHIFT_END));
            } else {
                // 特殊情况安排中班
                plan.setPlanShift("SHIFT_AFTERNOON");
                plan.setPlanStartTime(LocalDateTime.of(plan.getPlanDate(), AFTERNOON_SHIFT_START));
                plan.setPlanEndTime(LocalDateTime.of(plan.getPlanDate(), AFTERNOON_SHIFT_END));
            }

            // 计算对硫化的影响
            BigDecimal reduceRatio = calculateVulcanizeReduceRatio(plan.getMachineCode(), plan.getPlanDate());
            plan.setVulcanizeReduceRatio(reduceRatio);
            plan.setAffectVulcanize(reduceRatio.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0);
        }

        plan.setStatus("PLANNED");
        plan.setArrangeReason("SCHEDULED");
        plan.setUpdateTime(LocalDateTime.now());

        return updateById(plan);
    }

    @Override
    public BigDecimal calculateVulcanizeReduceRatio(String machineCode, LocalDate planDate) {
        CxPrecisionPlan plan = getByMachineAndDate(machineCode, planDate);
        if (plan == null) {
            return BigDecimal.ZERO;
        }

        // 获取关联胎胚库存
        String embryoCode = plan.getEmbryoCode();
        if (embryoCode == null) {
            return BigDecimal.ZERO;
        }

        CxStock stock = stockMapper.selectOne(
                new LambdaQueryWrapper<CxStock>()
                        .eq(CxStock::getEmbryoCode, embryoCode));

        // 计算库存可支持的硫化时长（小时）
        BigDecimal stockHours = calculateStockHours(stock);

        // 如果胎胚库存够硫化机吃4小时以上，硫化机继续生产
        if (stockHours.compareTo(BigDecimal.valueOf(4)) >= 0) {
            return BigDecimal.ZERO;
        }

        // 不够，硫化机减产一半
        return new BigDecimal("0.5");
    }

    @Override
    public List<String> getUnavailableMachines(LocalDate planDate, String shiftCode) {
        List<CxPrecisionPlan> plans = getByDate(planDate);
        
        return plans.stream()
                .filter(p -> shiftCode == null || shiftCode.equals(p.getPlanShift()))
                .filter(p -> "PLANNED".equals(p.getStatus()) || "IN_PROGRESS".equals(p.getStatus()))
                .map(CxPrecisionPlan::getMachineCode)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Integer> getCapacityDeduction(LocalDate planDate) {
        Map<String, Integer> result = new HashMap<>();
        
        List<CxPrecisionPlan> plans = getByDate(planDate);
        for (CxPrecisionPlan plan : plans) {
            if ("PLANNED".equals(plan.getStatus()) || "IN_PROGRESS".equals(plan.getStatus())) {
                // 计算扣减产能 = 精度时长 × 机台小时产能
                int deduction = PRECISION_HOURS * DEFAULT_MACHINE_HOURLY_CAPACITY;
                result.put(plan.getMachineCode(), deduction);
            }
        }
        
        return result;
    }

    @Override
    public boolean isInPrecisionPeriod(String machineCode, LocalDate planDate, String shiftCode) {
        CxPrecisionPlan plan = getByMachineAndDate(machineCode, planDate);
        if (plan == null) {
            return false;
        }

        if (!"PLANNED".equals(plan.getStatus()) && !"IN_PROGRESS".equals(plan.getStatus())) {
            return false;
        }

        if (shiftCode == null) {
            return true;
        }

        return shiftCode.equals(plan.getPlanShift());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateStatus(List<Long> planIds, String status) {
        if (planIds == null || planIds.isEmpty()) {
            return 0;
        }

        boolean success = update(new LambdaUpdateWrapper<CxPrecisionPlan>()
                .in(CxPrecisionPlan::getId, planIds)
                .set(CxPrecisionPlan::getStatus, status)
                .set(CxPrecisionPlan::getUpdateTime, LocalDateTime.now()));
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
     */
    private boolean needsPrecisionPlan(MdmMoldingMachine machine, LocalDate targetDate) {
        // 获取该机台最近一次精度计划
        CxPrecisionPlan lastPlan = getOne(
                new LambdaQueryWrapper<CxPrecisionPlan>()
                        .eq(CxPrecisionPlan::getMachineCode, machine.getCxMachineCode())
                        .in(CxPrecisionPlan::getStatus, "PLANNED", "IN_PROGRESS", "COMPLETED")
                        .orderByDesc(CxPrecisionPlan::getPlanDate)
                        .last("LIMIT 1"));

        if (lastPlan == null) {
            // 从未做过精度，需要安排
            return true;
        }

        // 检查是否已过精度周期
        LocalDate nextDueDate = lastPlan.getPlanDate().plusMonths(PRECISION_CYCLE_MONTHS);
        LocalDate arrangeDate = nextDueDate.minusDays(ADVANCE_DAYS);

        return !targetDate.isBefore(arrangeDate) && targetDate.isBefore(nextDueDate);
    }

    /**
     * 统计指定日期的精度计划数量
     */
    private long countByDate(LocalDate date) {
        return count(new LambdaQueryWrapper<CxPrecisionPlan>()
                .eq(CxPrecisionPlan::getPlanDate, date)
                .ne(CxPrecisionPlan::getStatus, "CANCELLED"));
    }

    /**
     * 创建精度计划
     */
    private CxPrecisionPlan createPrecisionPlan(MdmMoldingMachine machine, LocalDate planDate) {
        CxPrecisionPlan plan = new CxPrecisionPlan();
        plan.setMachineCode(machine.getCxMachineCode());
        plan.setMachineName(machine.getMachineName());
        plan.setPlanDate(planDate);
        plan.setEstimatedHours(PRECISION_HOURS);
        plan.setStatus("PLANNED");
        plan.setArrangeReason("SCHEDULED");

        // 获取机台当前在产结构的胎胚（需要从 CxMachineOnlineInfo 获取）
        // 暂时不设置 embryoCode，由排程时动态获取

        // 设置上次精度日期（需要查询）
        CxPrecisionPlan lastPlan = getOne(
                new LambdaQueryWrapper<CxPrecisionPlan>()
                        .eq(CxPrecisionPlan::getMachineCode, machine.getCxMachineCode())
                        .eq(CxPrecisionPlan::getStatus, "COMPLETED")
                        .orderByDesc(CxPrecisionPlan::getPlanDate)
                        .last("LIMIT 1"));
        if (lastPlan != null) {
            plan.setLastPrecisionDate(lastPlan.getPlanDate());
        }

        // 设置到期日期
        if (lastPlan != null) {
            plan.setDueDate(lastPlan.getPlanDate().plusMonths(PRECISION_CYCLE_MONTHS));
        } else {
            plan.setDueDate(planDate.plusMonths(PRECISION_CYCLE_MONTHS));
        }

        plan.setCreateTime(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());

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
