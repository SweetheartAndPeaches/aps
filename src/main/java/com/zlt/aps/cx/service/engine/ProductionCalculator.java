package com.zlt.aps.cx.service.engine;


import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.service.engine.ScheduleDayTypeHelper;
import com.zlt.aps.cx.vo.ScheduleContextVo;

import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 计划量计算服务
 * 
 * <p>负责成型排程的计划量计算：
 * <ul>
 *   <li>计算待排产量（硫化需求 - 成型余量）</li>
 *   <li>整车换算</li>
 *   <li>波浪分配到班次</li>
 *   <li>特殊情况处理（开产、停产、试制、停机）</li>
 * </ul>
 *
 * <h3>正常情况计算流程：</h3>
 * <ol>
 *   <li>已知成型机安排的胎胚及对应的硫化任务</li>
 *   <li>计算今天需求 = 硫化任务需求 - 成型余量（库存）</li>
 *   <li>按整车换算（查询 CxStructureShiftCapacity.treadCount）</li>
 *   <li>波浪分配到3个班次（相邻班次差距不超过1车）</li>
 * </ol>
 *
 * <h3>特殊情况：</h3>
 * <ul>
 *   <li><b>停产</b>：成型停机比硫化停火提前1班，精确收尾，库存归零</li>
 *   <li><b>开产</b>：首班只排6小时，关键产品从第二班开始</li>
 *   <li><b>试制量试</b>：只能安排早班或中班，数量必须是双数</li>
 *   <li><b>设备计划停机</b>：库存够4小时继续生产，不够减半消化</li>
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

    private final ScheduleDayTypeHelper scheduleDayTypeHelper;


    /** 试制量试允许的班次时间范围 */
    public static final String TRIAL_SHIFT_DAY = "SHIFT_DAY";        // 早班
    public static final String TRIAL_SHIFT_AFTERNOON = "SHIFT_AFTERNOON";  // 中班

    // ==================== 核心计算方法 ====================

    /**
     * 计算胎胚的今日计划量
     *
     * <p>计算流程：
     * <ol>
     *   <li>获取硫化需求</li>
     *   <li>计算成型余量（库存）</li>
     *   <li>计算待排产量</li>
     *   <li>整车换算</li>
     * </ol>
     *
     * @param embryoCode    胎胚编码
     * @param structureName 结构名称
     * @param context       排程上下文
     * @param scheduleDate  排程日期
     * @return 计划量结果
     */
    public PlanQuantityResult calculatePlanQuantity(
            String embryoCode,
            String structureName,
            ScheduleContextVo context,
            LocalDate scheduleDate) {

        PlanQuantityResult result = new PlanQuantityResult();
        result.setEmbryoCode(embryoCode);
        result.setStructureName(structureName);

        // Step 1: 获取硫化需求
        int vulcanizeDemand = getVulcanizeDemand(embryoCode, context, scheduleDate);
        result.setVulcanizeDemand(vulcanizeDemand);

        // Step 2: 计算成型余量（库存）
        int formingRemainder = getFormingRemainder(embryoCode, context);
        result.setFormingRemainder(formingRemainder);

        // Step 3: 计算待排产量 = 硫化需求 - 成型余量
        int baseProduction = Math.max(0, vulcanizeDemand - formingRemainder);

        // Step 4: 考虑损耗率
        BigDecimal lossRate = getLossRate(context);
        int productionWithLoss = (int) Math.ceil(baseProduction * (1 + lossRate.doubleValue()));

        // Step 5: 整车换算
        int tripCapacity = getTripCapacity(structureName, context);
        int trips = calculateTrips(productionWithLoss, tripCapacity);
        int planQuantity = trips * tripCapacity;

        result.setTripCapacity(tripCapacity);
        result.setTripCount(trips);
        result.setPlanQuantity(planQuantity);

        log.debug("胎胚 {} 计划量计算：硫化需求={}, 成型余量={}, 待排={}, 整车容量={}, 车次={}, 计划量={}",
                embryoCode, vulcanizeDemand, formingRemainder, productionWithLoss,
                tripCapacity, trips, planQuantity);

        return result;
    }

    /**
     * 获取硫化需求
     *
     * <p>从硫化排程结果中获取该胎胚今日的需求量
     */
    public int getVulcanizeDemand(String embryoCode, ScheduleContextVo context, LocalDate scheduleDate) {
        List<LhScheduleResult> lhResults = context.getLhScheduleResults();
        if (lhResults == null || lhResults.isEmpty()) {
            return 0;
        }

        int totalDemand = 0;
        for (LhScheduleResult result : lhResults) {
            if (embryoCode.equals(result.getEmbryoCode())) {
                // 获取日计划数量
                Integer dailyPlanQty = result.getDailyPlanQty();
                if (dailyPlanQty != null && dailyPlanQty > 0) {
                    totalDemand += dailyPlanQty;
                }
            }
        }

        return totalDemand;
    }

    /**
     * 计算成型余量（胎胚库存）
     *
     * <p>成型余量 = 该胎胚的当前库存
     */
    public int getFormingRemainder(String embryoCode, ScheduleContextVo context) {
        List<CxStock> stocks = context.getStocks();
        if (stocks == null || stocks.isEmpty()) {
            return 0;
        }

        for (CxStock stock : stocks) {
            if (embryoCode.equals(stock.getEmbryoCode())) {
                Integer qty = stock.getStockNum();
                return qty != null && qty > 0 ? qty : 0;
            }
        }

        return 0;
    }

    /**
     * 计算需要的车次
     *
     * <p>向上取整，确保覆盖需求
     */
    public int calculateTrips(int quantity, int tripCapacity) {
        if (quantity <= 0 || tripCapacity <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) quantity / tripCapacity);
    }

    /**
     * 波浪分配车次到班次
     *
     * <p>波浪规则：
     * <ul>
     *   <li>相邻班次差距不超过1车</li>
     *   <li>分配顺序：夜班-早班-中班（按班次配置顺序）</li>
     *   <li>波浪形态：两端多，中间少</li>
     * </ul>
     *
     * <p>示例：5车分配到3班 → 2-1-2 → 30-15-30条
     *
     * @param totalTrips  总车次
     * @param shiftCount  班次数量
     * @param tripCapacity 每车条数
     * @return 各班次分配量（条）
     */
    public Map<String, Integer> waveAllocation(int totalTrips, int shiftCount, int tripCapacity) {
        Map<String, Integer> result = new LinkedHashMap<>();

        if (totalTrips <= 0 || shiftCount <= 0) {
            for (int i = 0; i < shiftCount; i++) {
                result.put("SHIFT_" + (i + 1), 0);
            }
            return result;
        }

        // 计算基础分配
        int baseTripsPerShift = totalTrips / shiftCount;
        int remainder = totalTrips % shiftCount;

        // 波浪分配：将余数分配给两端
        int[] shiftTrips = new int[shiftCount];
        for (int i = 0; i < shiftCount; i++) {
            shiftTrips[i] = baseTripsPerShift;
        }

        // 分配余数：优先分配给两端，形成波浪
        if (remainder > 0) {
            // 先分配给第一个班次
            shiftTrips[0]++;
            remainder--;

            // 如果还有余数，分配给最后一个班次
            if (remainder > 0 && shiftCount > 1) {
                shiftTrips[shiftCount - 1]++;
                remainder--;
            }

            // 如果还有余数，继续从两端分配
            int left = 1;
            int right = shiftCount - 2;
            while (remainder > 0) {
                if (left <= right) {
                    shiftTrips[left]++;
                    remainder--;
                    left++;
                }
                if (remainder > 0 && right >= left) {
                    shiftTrips[right]++;
                    remainder--;
                    right--;
                }
            }
        }

        // 验证波浪规则：相邻班次差距不超过1车
        for (int i = 1; i < shiftCount; i++) {
            int diff = Math.abs(shiftTrips[i] - shiftTrips[i - 1]);
            if (diff > 1) {
                log.warn("波浪分配警告：班次 {} 和 {} 差距 {} 车超过1车", i, i + 1, diff);
            }
        }

        // 转换为条数并生成结果
        for (int i = 0; i < shiftCount; i++) {
            result.put("SHIFT_" + (i + 1), shiftTrips[i] * tripCapacity);
        }

        log.info("波浪分配：总车次={}, 班次数={}, 分配结果={}", totalTrips, shiftCount, result);

        return result;
    }

    /**
     * 波浪分配（使用班次配置）
     *
     * @param totalTrips    总车次
     * @param shiftConfigs  班次配置列表
     * @param tripCapacity  每车条数
     * @return 班次编码 -> 分配量
     */
    public Map<String, Integer> waveAllocationWithShifts(
            int totalTrips,
            List<CxShiftConfig> shiftConfigs,
            int tripCapacity) {

        Map<String, Integer> result = new LinkedHashMap<>();

        if (shiftConfigs == null || shiftConfigs.isEmpty()) {
            return result;
        }

        int shiftCount = shiftConfigs.size();
        int[] shiftTrips = new int[shiftCount];

        if (totalTrips > 0) {
            // 计算基础分配
            int baseTripsPerShift = totalTrips / shiftCount;
            int remainder = totalTrips % shiftCount;

            for (int i = 0; i < shiftCount; i++) {
                shiftTrips[i] = baseTripsPerShift;
            }

            // 波浪分配余数
            if (remainder > 0) {
                // 分配给两端
                shiftTrips[0]++;
                remainder--;

                if (remainder > 0 && shiftCount > 1) {
                    shiftTrips[shiftCount - 1]++;
                    remainder--;
                }

                // 继续分配
                int left = 1;
                int right = shiftCount - 2;
                while (remainder > 0 && left <= right) {
                    shiftTrips[left]++;
                    remainder--;
                    if (remainder > 0) {
                        shiftTrips[right]--;
                        remainder--;
                    }
                    left++;
                    right--;
                }
            }
        }

        // 生成结果
        for (int i = 0; i < shiftCount; i++) {
            String shiftCode = shiftConfigs.get(i).getShiftCode();
            if (shiftCode == null) {
                shiftCode = "SHIFT_" + (i + 1);
            }
            result.put(shiftCode, shiftTrips[i] * tripCapacity);
        }

        return result;
    }

    // ==================== 特殊情况处理 ====================

    /**
     * 停产日计划量计算
     *
     * <p>停产规则：
     * <ul>
     *   <li>成型机停机时间比硫化机停火提前1个班次</li>
     *   <li>胎胚要求全部收尾，库存为0</li>
     *   <li>精确计算，保证做完后库存为0，且正好够硫化吃到停火</li>
     * </ul>
     *
     * @param embryoCode    胎胚编码
     * @param structureName 结构名称
     * @param context       排程上下文
     * @param scheduleDate  排程日期
     * @return 停产计划量结果
     */
    public PlanQuantityResult calculateClosingDayQuantity(
            String embryoCode,
            String structureName,
            ScheduleContextVo context,
            LocalDate scheduleDate) {

        PlanQuantityResult result = calculatePlanQuantity(embryoCode, structureName, context, scheduleDate);
        result.setClosingDay(true);

        // 停产日：精确计算，不留库存
        // 计划量 = 硫化需求 - 成型余量（精确匹配，不整车取整）
        int vulcanizeDemand = result.getVulcanizeDemand();
        int formingRemainder = result.getFormingRemainder();
        int exactQuantity = vulcanizeDemand - formingRemainder;

        if (exactQuantity <= 0) {
            // 库存足够，不需要生产
            result.setPlanQuantity(0);
            result.setTripCount(0);
            result.setExactClosing(true);
            log.info("停产日 {} 胎胚 {} 库存足够，不需要生产", scheduleDate, embryoCode);
        } else {
            // 精确计算到条数（停产日可以不按整车）
            result.setPlanQuantity(exactQuantity);
            result.setExactClosing(true);
            log.info("停产日 {} 胎胚 {} 精确计划量={}", scheduleDate, embryoCode, exactQuantity);
        }

        return result;
    }

    /**
     * 收尾计划量计算
     *
     * <p>收尾规则：
     * <ul>
     *   <li><b>主销产品</b>（月均销量≥500条）：即使收尾剩下的量不够一整车，也按整车下，多做的当库存</li>
     *   <li><b>非主销产品</b>：
     *     <ul>
     *       <li>余量≤2条：不做了，直接舍弃</li>
     *       <li>余量>2条：按实际量下，不凑整车</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <p>示例：
     * <ul>
     *   <li>主销产品，余量8条，整车12条 → 计划12条，多做的4条当库存</li>
     *   <li>非主销产品，余量2条 → 计划0条，舍弃2条</li>
     *   <li>非主销产品，余量5条 → 计划5条，不凑整车</li>
     * </ul>
     *
     * @param endingSurplus 收尾余量
     * @param tripCapacity  整车容量
     * @param isMainProduct 是否主销产品
     * @param embryoCode    胎胚编码（日志用）
     * @return 收尾计划量结果
     */
    public PlanQuantityResult calculateEndingQuantity(
            int endingSurplus,
            int tripCapacity,
            boolean isMainProduct,
            String embryoCode) {

        PlanQuantityResult result = new PlanQuantityResult();
        result.setEndingTask(true);
        result.setTripCapacity(tripCapacity);
        result.setMainProduct(isMainProduct);

        if (endingSurplus <= 0) {
            result.setPlanQuantity(0);
            result.setTripCount(0);
            log.info("收尾任务 {} 余量为 {}，不需要生产", embryoCode, endingSurplus);
            return result;
        }

        if (isMainProduct) {
            // 主销产品：按整车下，多做的当库存
            int trips = calculateTrips(endingSurplus, tripCapacity);
            int planQuantity = trips * tripCapacity;
            int extraInventory = planQuantity - endingSurplus; // 多做的部分

            result.setPlanQuantity(planQuantity);
            result.setTripCount(trips);
            result.setExactClosing(false); // 按整车取整
            result.setExtraInventory(extraInventory);

            log.info("收尾任务 {} 主销产品，余量 {} 条，按整车 {} 条下，多做的 {} 条当库存",
                    embryoCode, endingSurplus, planQuantity, extraInventory);
        } else {
            // 非主销产品
            if (endingSurplus <= 2) {
                // 余量≤2条：舍弃，不做了
                result.setPlanQuantity(0);
                result.setTripCount(0);
                result.setAbandoned(true);
                result.setAbandonedQuantity(endingSurplus);

                log.info("收尾任务 {} 非主销产品，余量 {} 条≤2条，舍弃不生产", embryoCode, endingSurplus);
            } else {
                // 余量>2条：按实际量下，不凑整车
                result.setPlanQuantity(endingSurplus);
                result.setTripCount(0); // 不按车次
                result.setExactClosing(true); // 精确数量

                log.info("收尾任务 {} 非主销产品，余量 {} 条>2条，按实际量下",
                        embryoCode, endingSurplus);
            }
        }

        return result;
    }

    /**
     * 收尾计划量计算（完整版）
     *
     * <p>包含库存计算的综合版本：
     * <ol>
     *   <li>计算待排量 = 收尾余量 - 成型余量（库存）</li>
     *   <li>根据是否主销产品应用收尾规则</li>
     * </ol>
     *
     * @param embryoCode      胎胚编码
     * @param structureName   结构名称
     * @param endingSurplus   收尾余量
     * @param isMainProduct   是否主销产品
     * @param context         排程上下文
     * @param scheduleDate    排程日期
     * @return 收尾计划量结果
     */
    public PlanQuantityResult calculateEndingQuantityWithStock(
            String embryoCode,
            String structureName,
            int endingSurplus,
            boolean isMainProduct,
            ScheduleContextVo context,
            LocalDate scheduleDate) {

        // 获取成型余量（库存）
        int formingRemainder = getFormingRemainder(embryoCode, context);

        // 计算待排量 = 收尾余量 - 库存
        int remainingToProduce = Math.max(0, endingSurplus - formingRemainder);

        // 获取整车容量
        int tripCapacity = getTripCapacity(structureName, context);

        // 调用基础收尾计算
        PlanQuantityResult result = calculateEndingQuantity(
                remainingToProduce, tripCapacity, isMainProduct, embryoCode);

        // 补充信息
        result.setEmbryoCode(embryoCode);
        result.setStructureName(structureName);
        result.setFormingRemainder(formingRemainder);
        result.setVulcanizeDemand(endingSurplus); // 收尾余量作为需求

        log.info("收尾任务 {} 综合计算：收尾余量={}, 成型余量={}, 待排={}, 是否主销={}, 计划量={}",
                embryoCode, endingSurplus, formingRemainder, remainingToProduce,
                isMainProduct, result.getPlanQuantity());

        return result;
    }

    // ==================== 试制量试计算 ====================

    /**
     * 试制量试计划量计算
     *
     * <p>试制规则：
     * <ul>
     *   <li>只能安排在早班或中班（7:30-15:00）</li>
     *   <li>数量必须是双数</li>
     * </ul>
     *
     * @param demand      需求量
     * @param tripCapacity 整车容量
     * @return 试制计划量
     */
    public PlanQuantityResult calculateTrialQuantity(int demand, int tripCapacity) {
        PlanQuantityResult result = new PlanQuantityResult();
        result.setTrialTask(true);

        if (demand <= 0) {
            result.setPlanQuantity(0);
            return result;
        }

        // 数量必须是双数
        int adjustedDemand = demand % 2 == 0 ? demand : demand + 1;
        result.setPlanQuantity(adjustedDemand);

        // 试制只能在早班或中班
        result.setAllowedShifts(Arrays.asList(TRIAL_SHIFT_DAY, TRIAL_SHIFT_AFTERNOON));

        log.info("试制任务需求 {} 调整为双数 {}", demand, adjustedDemand);

        return result;
    }

    /**
     * 正常任务的整车取整
     *
     * <p>将待排条数向上取整到整车（胎面）。
     *
     * @param stripQuantity 待排条数
     * @param tripCapacity 整车条数（胎面每车条数）
     * @return 整车取整后的条数
     */
    public int roundToVehicle(int stripQuantity, int tripCapacity) {
        if (stripQuantity <= 0 || tripCapacity <= 0) {
            return 0;
        }
        int trips = (int) Math.ceil((double) stripQuantity / tripCapacity);
        return trips * tripCapacity;
    }

    /**
     * 设备计划停机处理
     *
     * <p>停机规则：
     * <ul>
     *   <li>如果胎胚库存够硫化机吃4小时以上，硫化机继续生产</li>
     *   <li>如果不够，硫化机要减产一半，慢慢消化库存</li>
     *   <li>安排在早班（7:30-11:30）；特殊情况可以安排中班（13:00-17:00）</li>
     * </ul>
     *
     * @param embryoCode     胎胚编码
     * @param hourlyCapacity 硫化机小时产能
     * @param context        排程上下文
     * @return 停机处理结果
     */
    public ShutdownHandlingResult handleDeviceShutdown(
            String embryoCode,
            int hourlyCapacity,
            ScheduleContextVo context) {

        ShutdownHandlingResult result = new ShutdownHandlingResult();
        result.setEmbryoCode(embryoCode);

        // 获取成型余量
        int formingRemainder = getFormingRemainder(embryoCode, context);
        result.setFormingRemainder(formingRemainder);

        // 计算4小时硫化需求
        int fourHourDemand = hourlyCapacity * 4;
        result.setFourHourDemand(fourHourDemand);

        if (formingRemainder >= fourHourDemand) {
            // 库存够4小时，硫化机继续生产
            result.setVulcanizeContinue(true);
            result.setReductionRatio(1.0); // 不减产
            log.info("胎胚 {} 库存 {} 够4小时需求 {}，硫化机继续生产",
                    embryoCode, formingRemainder, fourHourDemand);
        } else {
            // 库存不够，硫化机减产一半
            result.setVulcanizeContinue(false);
            result.setReductionRatio(0.5); // 减产一半
            log.info("胎胚 {} 库存 {} 不够4小时需求 {}，硫化机减产一半",
                    embryoCode, formingRemainder, fourHourDemand);
        }

        // 停机安排在早班，特殊情况中班
        result.setPreferredShift(TRIAL_SHIFT_DAY);
        result.setAlternativeShift(TRIAL_SHIFT_AFTERNOON);

        return result;
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取整车容量
     */
    public int getTripCapacity(String structureName, ScheduleContextVo context) {
        if (context.getStructureShiftCapacities() != null && structureName != null) {
            for (CxStructureTreadConfig capacity : context.getStructureShiftCapacities()) {
                if (structureName.equals(capacity.getStructureCode())) {
                    if (capacity.getTreadCount() != null && capacity.getTreadCount() > 0) {
                        return capacity.getTreadCount();
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
     */
    public BigDecimal getLossRate(ScheduleContextVo context) {
        return context.getLossRate() != null ? context.getLossRate() : DEFAULT_LOSS_RATE;
    }

    /**
     * 整车取整
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
     * 计算机台日产能
     */
    public int calculateMachineDailyCapacity(MdmMoldingMachine machine, ScheduleContextVo context) {
        if (machine == null) {
            return DEFAULT_DAILY_CAPACITY;
        }

        if (machine.getMaxDayCapacity() != null && machine.getMaxDayCapacity() > 0) {
            return machine.getMaxDayCapacity();
        }

        int hourlyCapacity = getMachineHourlyCapacity(machine, context);
        int shiftHours = getShiftHours(context);
        
        return hourlyCapacity * shiftHours;
    }

    public int getMachineHourlyCapacity(MdmMoldingMachine machine, ScheduleContextVo context) {
        if (machine == null) {
            return DEFAULT_HOURLY_CAPACITY;
        }
        return context.getMachineHourlyCapacity() != null 
                ? context.getMachineHourlyCapacity() 
                : DEFAULT_HOURLY_CAPACITY;
    }

    private int getShiftHours(ScheduleContextVo context) {
        if (context.getCurrentShiftConfigs() != null && !context.getCurrentShiftConfigs().isEmpty()) {
            int totalHours = 0;
            for (CxShiftConfig shift : context.getCurrentShiftConfigs()) {
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
     * 检查是否为关键产品
     *
     * <p>关键产品定义：从 CxKeyProduct 表获取，使用胎胚编码（embryoCode）
     * <p>用于开产首班排除等场景判断
     *
     * @param embryoCode 胎胚编码
     * @param context    排程上下文
     * @return 是否关键产品
     */
    public boolean isKeyProduct(String embryoCode, ScheduleContextVo context) {
        Set<String> keyProductCodes = context.getKeyProductCodes();
        if (keyProductCodes != null && embryoCode != null) {
            return keyProductCodes.contains(embryoCode);
        }
        return false;
    }

    /**
     * 检查是否为主销产品
     *
     * <p>主销产品定义：月均销量≥500条，SKU排产分类SCHEDULE_TYPE='01'
     * <p>从 MdmSkuScheduleCategory 表获取，使用物料编码（materialCode）
     * <p>用于收尾时判断是否按整车下
     *
     * @param materialCode 物料编码
     * @param context      排程上下文
     * @return 是否主销产品
     */
    public boolean isMainProduct(String materialCode, ScheduleContextVo context) {
        Set<String> mainProductCodes = context.getMainProductCodes();
        if (mainProductCodes != null && materialCode != null) {
            return mainProductCodes.contains(materialCode);
        }
        return false;
    }

    // ==================== 综合计算方法 ====================

    /**
     * 综合计划量计算（处理所有场景）
     *
     * <p>处理逻辑：
     * <ol>
     *   <li>判断是否试制任务 → 使用试制规则</li>
     *   <li>判断是否收尾任务 → 使用收尾规则</li>
     *   <li>判断是否停产日 → 使用停产收尾规则</li>
     *   <li>判断是否开产日 → 使用开产规则</li>
     *   <li>正常情况 → 波浪分配</li>
     * </ol>
     *
     * @param embryoCode    胎胚编码（用于判断关键产品，来自 CxKeyProduct.embryoCode）
     * @param structureName 结构名称
     * @param materialCode  物料编码（用于判断主销产品，来自 MdmSkuScheduleCategory.materialCode）
     * @param trialDemand   试制需求量（如果是试制任务）
     * @param context       排程上下文
     * @param scheduleDate  排程日期
     * @return 计算结果（包含班次分配）
     */
    public PlanQuantityResult calculateComprehensive(
            String embryoCode,
            String structureName,
            String materialCode,
            Integer trialDemand,
            ScheduleContextVo context,
            LocalDate scheduleDate) {

        return calculateComprehensive(embryoCode, structureName, materialCode,
                trialDemand, null, context, scheduleDate);
    }

    /**
     * 综合计划量计算（完整版，支持收尾场景）
     *
     * <p>处理逻辑：
     * <ol>
     *   <li>判断是否试制任务 → 使用试制规则</li>
     *   <li>判断是否收尾任务 → 使用收尾规则</li>
     *   <li>判断是否停产日 → 使用停产收尾规则</li>
     *   <li>判断是否开产日 → 使用开产规则</li>
     *   <li>正常情况 → 波浪分配</li>
     * </ol>
     *
     * <p><b>数据来源说明：</b>
     * <ul>
     *   <li><b>关键产品</b>：CxKeyProduct 表，使用胎胚编码（embryoCode）判断</li>
     *   <li><b>主销产品</b>：MdmSkuScheduleCategory 表（scheduleType='01'），使用物料编码（materialCode）判断</li>
     *   <li><b>成型余量</b>：MdmMonthSurplus 表，按物料维度计算</li>
     * </ul>
     *
     * @param embryoCode      胎胚编码（用于判断关键产品）
     * @param structureName   结构名称
     * @param materialCode    物料编码（用于判断主销产品）
     * @param trialDemand     试制需求量（如果是试制任务）
     * @param endingSurplus   收尾余量（如果是收尾任务）
     * @param context         排程上下文
     * @param scheduleDate    排程日期
     * @return 计算结果（包含班次分配）
     */
    public PlanQuantityResult calculateComprehensive(
            String embryoCode,
            String structureName,
            String materialCode,
            Integer trialDemand,
            Integer endingSurplus,
            ScheduleContextVo context,
            LocalDate scheduleDate) {

        // Step 1: 判断是否试制任务
        if (trialDemand != null && trialDemand > 0) {
            int tripCapacity = getTripCapacity(structureName, context);
            PlanQuantityResult trialResult = calculateTrialQuantity(trialDemand, tripCapacity);
            trialResult.setEmbryoCode(embryoCode);
            trialResult.setStructureName(structureName);

            // 试制任务：只安排在早班或中班
            Map<String, Integer> trialShiftAllocation = new LinkedHashMap<>();
            trialShiftAllocation.put(TRIAL_SHIFT_DAY, trialResult.getPlanQuantity());
            trialResult.setShiftAllocation(trialShiftAllocation);

            return trialResult;
        }

        // Step 2: 判断是否收尾任务
        if (endingSurplus != null && endingSurplus > 0) {
            boolean isMainProduct = isMainProduct(materialCode, context);
            PlanQuantityResult endingResult = calculateEndingQuantityWithStock(
                    embryoCode, structureName, endingSurplus, isMainProduct, context, scheduleDate);

            // 收尾任务：安排在早班或中班（不安排夜班）
            int planQuantity = endingResult.getPlanQuantity();
            if (planQuantity > 0) {
                Map<String, Integer> endingShiftAllocation = new LinkedHashMap<>();
                // 收尾任务优先安排在早班
                endingShiftAllocation.put("SHIFT_NIGHT", 0);
                endingShiftAllocation.put(TRIAL_SHIFT_DAY, planQuantity);
                endingShiftAllocation.put(TRIAL_SHIFT_AFTERNOON, 0);
                endingResult.setShiftAllocation(endingShiftAllocation);
            }

            return endingResult;
        }

        // Step 3: 判断是否停产日（根据 dayFlag：停产标识日之后才算停产）
        // 停产标识的那一天本身有量，只有停产日之后才算停产
        ScheduleDayTypeHelper.DayFlagInfo flagInfo =
                scheduleDayTypeHelper.findNearestDayFlag(scheduleDate, context.getFactoryCode());
        if (flagInfo != null && "0".equals(flagInfo.dayFlag) && scheduleDate.isAfter(flagInfo.nearestDate)) {
            // 停产日之后：plannedProduction = 0，使用停产收尾规则
            return calculateClosingDayQuantity(embryoCode, structureName, context, scheduleDate);
        }
        // 停产标识日当天：有量，正常按硫化计划安排

        // Step 4: 判断是否开产日（最近标识为"开"则正常按硫化计划安排，取整到整车）
        // 开产日有量但不多，严格按硫化计划安排，取整到整车
        // 收尾任务的整车取整已在上面完成，此处不再做额外限制
        if (flagInfo != null && "1".equals(flagInfo.dayFlag)) {
            // 开产日按正常硫化计划走，不做班次限制，直接用上面的收尾计算结果
            log.debug("开产日，materialCode={}，按硫化计划正常安排", materialCode);
        }

        // Step 5: 正常情况计算
        PlanQuantityResult result = calculatePlanQuantity(embryoCode, structureName, context, scheduleDate);

        // 波浪分配到班次
        List<CxShiftConfig> shiftConfigs = context.getCurrentShiftConfigs();
        int tripCapacity = result.getTripCapacity();
        int totalTrips = result.getTripCount();

        Map<String, Integer> shiftAllocation;
        if (shiftConfigs != null && !shiftConfigs.isEmpty()) {
            shiftAllocation = waveAllocationWithShifts(totalTrips, shiftConfigs, tripCapacity);
        } else {
            // 默认3班制
            shiftAllocation = waveAllocation(totalTrips, 3, tripCapacity);
        }

        result.setShiftAllocation(shiftAllocation);
        return result;
    }

    /**
     * 处理设备计划停机后的产量调整
     *
     * <p>返回调整后的班次分配
     */
    public Map<String, Integer> adjustForDeviceShutdown(
            String embryoCode,
            int originalPlanQuantity,
            int hourlyCapacity,
            ScheduleContextVo context) {

        ShutdownHandlingResult shutdownResult = handleDeviceShutdown(embryoCode, hourlyCapacity, context);

        Map<String, Integer> adjustedAllocation = new LinkedHashMap<>();

        if (shutdownResult.isVulcanizeContinue()) {
            // 硫化机继续生产，不需要调整
            int normalShiftQty = originalPlanQuantity / 3;
            adjustedAllocation.put("SHIFT_NIGHT", normalShiftQty);
            adjustedAllocation.put(TRIAL_SHIFT_DAY, normalShiftQty);
            adjustedAllocation.put(TRIAL_SHIFT_AFTERNOON, originalPlanQuantity - 2 * normalShiftQty);
        } else {
            // 硫化机减产一半
            int reducedQty = (int) (originalPlanQuantity * shutdownResult.getReductionRatio());
            // 安排在早班
            adjustedAllocation.put("SHIFT_NIGHT", 0);
            adjustedAllocation.put(TRIAL_SHIFT_DAY, reducedQty);
            adjustedAllocation.put(TRIAL_SHIFT_AFTERNOON, 0);
        }

        return adjustedAllocation;
    }

    // ==================== 结果类 ====================

    /**
     * 计划量计算结果
     */
    @lombok.Data
    public static class PlanQuantityResult {
        /** 胎胚编码 */
        private String embryoCode;
        /** 结构名称 */
        private String structureName;
        /** 硫化需求 */
        private int vulcanizeDemand;
        /** 成型余量（库存） */
        private int formingRemainder;
        /** 整车容量 */
        private int tripCapacity;
        /** 车次数 */
        private int tripCount;
        /** 计划量 */
        private int planQuantity;
        /** 班次分配 */
        private Map<String, Integer> shiftAllocation;

        // 特殊情况标记
        private boolean closingDay;
        private boolean trialTask;
        private boolean exactClosing;
        private List<String> allowedShifts;

        // 收尾相关字段
        /** 是否收尾任务 */
        private boolean endingTask;
        /** 是否主销产品 */
        private boolean mainProduct;
        /** 是否被舍弃（非主销产品余量≤2条） */
        private boolean abandoned;
        /** 舍弃数量（被舍弃的余量） */
        private int abandonedQuantity;
        /** 多做的库存量（主销产品按整车下时） */
        private int extraInventory;
    }

    /**
     * 设备停机处理结果
     */
    @lombok.Data
    public static class ShutdownHandlingResult {
        private String embryoCode;
        private int formingRemainder;
        private int fourHourDemand;
        private boolean vulcanizeContinue;
        private double reductionRatio;
        private String preferredShift;
        private String alternativeShift;
    }
}
