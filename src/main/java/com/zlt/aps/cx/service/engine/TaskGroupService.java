package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.cx.entity.*;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.mp.api.domain.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 任务分组服务
 * 
 * <p>负责 S5.2 排程分类与余量计算：
 * <ul>
 *   <li>确定当前排程班次</li>
 *   <li>获取续作任务列表</li>
 *   <li>计算成型余量</li>
 *   <li>判断收尾任务</li>
 *   <li>生成续作任务、试制任务、新增任务列表</li>
 * </ul>
 *
 * @author APS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskGroupService {

    /** 收尾阈值：成型余量低于此值视为紧急收尾 */
    private static final int ENDING_SURPLUS_THRESHOLD = 400;
    
    /** 收尾判断天数：未来多少天内判断收尾 */
    private static final int ENDING_DAYS_THRESHOLD = 10;
    
    /** 紧急收尾天数：未来多少天内视为紧急收尾 */
    private static final int URGENT_ENDING_DAYS = 3;

    /**
     * 任务分组结果
     */
    @lombok.Data
    public static class TaskGroupResult {
        /** 续作任务：当前机台在产的胎胚 */
        private List<CoreScheduleAlgorithmService.DailyEmbryoTask> continueTasks = new ArrayList<>();
        /** 试制任务：试制/量试任务 */
        private List<CoreScheduleAlgorithmService.DailyEmbryoTask> trialTasks = new ArrayList<>();
        /** 新增任务：非续作、非试制的常规任务 */
        private List<CoreScheduleAlgorithmService.DailyEmbryoTask> newTasks = new ArrayList<>();
    }

    /**
     * S5.2 排程分类与余量计算
     *
     * <p>将硫化任务分为三类：
     * <ul>
     *   <li>续作任务：当前机台在产的胎胚，需要继续生产</li>
     *   <li>试制任务：试制/量试任务</li>
     *   <li>新增任务：非续作、非试制的常规任务</li>
     * </ul>
     *
     * @param context                   排程上下文
     * @param machineOnlineEmbryoMap    机台在产胎胚映射
     * @param scheduleDate              排程日期
     * @return 任务分组结果
     */
    public TaskGroupResult groupTasks(
            ScheduleContextVo context,
            Map<String, Set<String>> machineOnlineEmbryoMap,
            LocalDate scheduleDate) {

        TaskGroupResult result = new TaskGroupResult();

        // 构建基础映射
        Map<String, MdmMaterialInfo> materialMap = buildMaterialMap(context);
        Map<String, CxStock> stockMap = buildStockMap(context);

        // 获取硫化排程结果
        List<LhScheduleResult> lhScheduleResults = context.getLhScheduleResults();
        if (lhScheduleResults == null || lhScheduleResults.isEmpty()) {
            log.warn("硫化排程结果为空，无法分组任务");
            return result;
        }

        // 按胎胚编码分组
        Map<String, List<LhScheduleResult>> embryoTaskMap = lhScheduleResults.stream()
                .filter(r -> r.getEmbryoCode() != null)
                .collect(Collectors.groupingBy(LhScheduleResult::getEmbryoCode));

        // 确保机台在产映射非空
        if (machineOnlineEmbryoMap == null) {
            machineOnlineEmbryoMap = new HashMap<>();
        }

        // 遍历每个胎胚任务
        for (Map.Entry<String, List<LhScheduleResult>> entry : embryoTaskMap.entrySet()) {
            String embryoCode = entry.getKey();
            List<LhScheduleResult> lhResults = entry.getValue();

            // 构建基础任务
            CoreScheduleAlgorithmService.DailyEmbryoTask task = buildBaseTask(
                    embryoCode, lhResults, materialMap, stockMap, context);
            if (task == null) {
                continue;
            }

            // 判断任务类型
            // 1. 续作任务：当前机台在产的胎胚
            List<String> continueMachineCodes = findContinueMachines(embryoCode, machineOnlineEmbryoMap);
            boolean isContinueTask = !continueMachineCodes.isEmpty();

            // 2. 试制任务
            boolean isTrialTask = lhResults.stream()
                    .anyMatch(r -> "1".equals(r.getIsTrial()));

            // 设置任务属性
            task.setIsContinueTask(isContinueTask);
            task.setContinueMachineCodes(continueMachineCodes);
            task.setIsTrialTask(isTrialTask);
            task.setIsFirstTask(!isContinueTask && !isTrialTask);

            // 计算收尾相关属性
            calculateEndingInfo(task, context, scheduleDate);

            // 分组
            if (isContinueTask) {
                result.getContinueTasks().add(task);
            } else if (isTrialTask) {
                result.getTrialTasks().add(task);
            } else {
                result.getNewTasks().add(task);
            }
        }

        return result;
    }

    /**
     * 构建物料映射
     */
    private Map<String, MdmMaterialInfo> buildMaterialMap(ScheduleContextVo context) {
        Map<String, MdmMaterialInfo> map = new HashMap<>();
        if (context.getMaterials() != null) {
            for (MdmMaterialInfo material : context.getMaterials()) {
                map.put(material.getMaterialCode(), material);
            }
        }
        return map;
    }

    /**
     * 构建库存映射
     */
    private Map<String, CxStock> buildStockMap(ScheduleContextVo context) {
        Map<String, CxStock> map = new HashMap<>();
        if (context.getStocks() != null) {
            for (CxStock stock : context.getStocks()) {
                map.put(stock.getEmbryoCode(), stock);
            }
        }
        return map;
    }

    /**
     * 查找续作机台
     */
    private List<String> findContinueMachines(String embryoCode, Map<String, Set<String>> machineOnlineEmbryoMap) {
        //todo 因为这里同胎胚可以被不同硫化任务物料共用，并且不同任务分配的机台不同，这里不能简单的比较在机是那些胎胚就安排
        List<String> machineCodes = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : machineOnlineEmbryoMap.entrySet()) {
            if (entry.getValue().contains(embryoCode)) {
                machineCodes.add(entry.getKey());
            }
        }
        return machineCodes;
    }

    /**
     * 构建基础任务
     */
    private CoreScheduleAlgorithmService.DailyEmbryoTask buildBaseTask(
            String embryoCode,
            List<LhScheduleResult> lhResults,
            Map<String, MdmMaterialInfo> materialMap,
            Map<String, CxStock> stockMap,
            ScheduleContextVo context) {

        // 计算硫化需求量
        int totalVulcanizeDemand = lhResults.stream()
                .mapToInt(r -> r.getDailyPlanQty() != null ? r.getDailyPlanQty() : 0)
                .sum();

        // 获取当前库存
        int currentStock = getCurrentStock(lhResults.get(0), stockMap, embryoCode);

        // 获取结构名称
        String structureName = materialMap.get(lhResults.get(0).getMaterialCode()).getStructureName();

        // 计算日需求量
        int dailyDemand = calculateDailyDemand(totalVulcanizeDemand, currentStock, structureName, context);

        // 构建任务
        CoreScheduleAlgorithmService.DailyEmbryoTask task = new CoreScheduleAlgorithmService.DailyEmbryoTask();
        task.setMaterialCode(embryoCode);
        task.setVulcanizeDemand(totalVulcanizeDemand);
        task.setCurrentStock(currentStock);

        // 获取物料信息
        MdmMaterialInfo material = materialMap.get(embryoCode);
        if (material != null) {
            task.setMaterialName(material.getMaterialDesc());
            task.setStructureName(material.getStructureName());
            // 设置关联的物料编码（用于判断主销产品）
            task.setRelatedMaterialCode(material.getMaterialCode());
        } else {
            task.setMaterialName(embryoCode);
            task.setStructureName(structureName);
        }

        task.setDemandQuantity(dailyDemand);
        task.setAssignedQuantity(0);
        task.setRemainingQuantity(dailyDemand);

        // 是否主销产品（使用物料编码判断，而不是胎胚编码）
        String relatedMaterialCode = task.getRelatedMaterialCode();
        task.setIsMainProduct(context.getMainProductCodes() != null 
                && relatedMaterialCode != null
                && context.getMainProductCodes().contains(relatedMaterialCode));

        // 计算库存时长
        calculateStockHours(task, lhResults, currentStock);

        // 硫化机台数和模数
        CxStock stock = stockMap.get(embryoCode);
        if (stock != null) {
            task.setVulcanizeMachineCount(stock.getVulcanizeMachineCount());
            task.setVulcanizeMoldCount(stock.getVulcanizeMoldCount());
        }

        return task;
    }

    /**
     * 获取当前库存
     */
    private int getCurrentStock(LhScheduleResult lhResult, Map<String, CxStock> stockMap, String embryoCode) {
        Integer embryoStock = lhResult.getEmbryoStock();
        if (embryoStock != null) {
            return embryoStock;
        }
        CxStock stock = stockMap.get(embryoCode);
        return stock != null ? stock.getEffectiveStock() : 0;
    }

    /**
     * 计算库存时长
     */
    private void calculateStockHours(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            List<LhScheduleResult> lhResults,
            int currentStock) {

        if (currentStock <= 0) {
            task.setStockHours(BigDecimal.ZERO);
            return;
        }

        // 获取硫化产能信息
        Integer vulcanizeMachineCount = task.getVulcanizeMachineCount();
        Integer vulcanizeMoldCount = task.getVulcanizeMoldCount();

        if (vulcanizeMachineCount == null || vulcanizeMachineCount == 0 ||
                vulcanizeMoldCount == null || vulcanizeMoldCount == 0) {
            task.setStockHours(BigDecimal.ZERO);
            return;
        }

        // 库存时长 = 胎胚库存 / (硫化机数 × 单台模数 × 每小时每模产量)
        BigDecimal hourlyOutput = BigDecimal.valueOf(vulcanizeMachineCount)
                .multiply(BigDecimal.valueOf(vulcanizeMoldCount))
                .multiply(BigDecimal.valueOf(0.5)); // 假设每模每小时0.5条

        BigDecimal stockHours = BigDecimal.valueOf(currentStock)
                .divide(hourlyOutput, 2, BigDecimal.ROUND_HALF_UP);
        task.setStockHours(stockHours);
    }

    /**
     * 计算日需求量
     */
    private int calculateDailyDemand(
            int totalVulcanizeDemand,
            int currentStock,
            String structureName,
            ScheduleContextVo context) {

        // 简化计算：日需求量 = 硫化需求量
        // 更详细的计算在续作任务处理器中进行
        return totalVulcanizeDemand;
    }



    /**
     * 计算收尾相关信息
     *
     * <p>包括：
     * <ul>
     *   <li>成型余量</li>
     *   <li>是否收尾任务</li>
     *   <li>是否10天内收尾</li>
     *   <li>是否3天内收尾（紧急）</li>
     *   <li>收尾日</li>
     * </ul>
     */
    public void calculateEndingInfo(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextVo context,
            LocalDate scheduleDate) {

        String materialCode = task.getMaterialCode();

        // 获取成型余量（从预计算的映射中获取）
        Map<String, Integer> formingRemainderMap = context.getFormingRemainderMap();
        Integer formingRemainder = null;
        Integer vulcanizeSurplusQty = null;

        // 从月计划余量获取硫化余量
        if (context.getMonthSurplusMap() != null) {
            MdmMonthSurplus monthSurplus = context.getMonthSurplusMap().get(materialCode);
            if (monthSurplus != null && monthSurplus.getPlanSurplusQty() != null) {
                vulcanizeSurplusQty = monthSurplus.getPlanSurplusQty().intValue();
            }
        }

        // 获取成型余量
        if (formingRemainderMap != null && formingRemainderMap.containsKey(materialCode)) {
            formingRemainder = formingRemainderMap.get(materialCode);
        } else if (vulcanizeSurplusQty != null) {
            // 成型余量 = 硫化余量 - 胎胚库存
            int currentStock = task.getCurrentStock() != null ? task.getCurrentStock() : 0;
            formingRemainder = vulcanizeSurplusQty - currentStock;
        }

        task.setVulcanizeSurplusQty(vulcanizeSurplusQty);
        task.setEndingSurplusQty(formingRemainder);

        // 判断是否收尾任务（成型余量 <= 0）
        boolean isEndingTask = formingRemainder != null && formingRemainder <= 0;
        task.setIsEndingTask(isEndingTask);

        // 获取收尾日（从物料收尾管理表）
        LocalDate endingDate = findEndingDate(materialCode, context);
        task.setEndingDate(endingDate);

        if (endingDate != null) {
            int daysToEnding = (int) java.time.temporal.ChronoUnit.DAYS.between(scheduleDate, endingDate);
            task.setDaysToEnding(daysToEnding);

            // 判断是否10天内收尾
            boolean isNearEnding = daysToEnding >= 0 && daysToEnding <= ENDING_DAYS_THRESHOLD;
            task.setIsNearEnding(isNearEnding);

            // 判断是否3天内收尾（紧急）
            boolean isUrgentEnding = daysToEnding >= 0 && daysToEnding <= URGENT_ENDING_DAYS;
            task.setIsUrgentEnding(isUrgentEnding);

            if (isUrgentEnding) {
                log.info("紧急收尾任务：物料={}, 收尾日={}, 距收尾{}天", 
                        materialCode, endingDate, daysToEnding);
            }
        }

        // 成型余量小于阈值也标记为紧急收尾
        if (formingRemainder != null && formingRemainder < ENDING_SURPLUS_THRESHOLD && formingRemainder > 0) {
            task.setIsUrgentEnding(true);
            log.info("成型余量低于阈值的收尾任务：物料={}, 成型余量={}, 阈值={}",
                    materialCode, formingRemainder, ENDING_SURPLUS_THRESHOLD);
        }

        // 计算优先级
        task.setPriority(calculateTaskPriority(task, context));
    }

    /**
     * 查找物料收尾日
     */
    private LocalDate findEndingDate(String materialCode, ScheduleContextVo context) {
        if (context.getMaterialEndings() != null) {
            for (CxMaterialEnding ending : context.getMaterialEndings()) {
                if (materialCode.equals(ending.getMaterialCode())) {
                    return ending.getPlannedEndingDate();
                }
            }
        }
        return null;
    }

    /**
     * 计算任务优先级分数
     */
    public int calculateTaskPriority(
            CoreScheduleAlgorithmService.DailyEmbryoTask task,
            ScheduleContextVo context) {

        int score = 0;

        // 紧急收尾最高优先级
        if (Boolean.TRUE.equals(task.getIsUrgentEnding())) {
            score += 3000;
        }
        // 普通收尾
        else if (Boolean.TRUE.equals(task.getIsEndingTask())) {
            score += 2000;
        }

        // 10天内收尾
        if (Boolean.TRUE.equals(task.getIsNearEnding())) {
            score += 1000;
        }

        // 试制任务
        if (Boolean.TRUE.equals(task.getIsTrialTask())) {
            score += 1500;
        }

        // 续作任务
        if (Boolean.TRUE.equals(task.getIsContinueTask())) {
            score += 800;
        }

        // 首排任务
        if (Boolean.TRUE.equals(task.getIsFirstTask())) {
            score += 500;
        }

        // 库存紧张
        if (task.getStockHours() != null) {
            if (task.getStockHours().compareTo(new BigDecimal("4")) < 0) {
                score += 800;
            } else if (task.getStockHours().compareTo(new BigDecimal("6")) < 0) {
                score += 400;
            }
        }

        // 主销产品
        if (Boolean.TRUE.equals(task.getIsMainProduct())) {
            score += 200;
        }

        return score;
    }
}
