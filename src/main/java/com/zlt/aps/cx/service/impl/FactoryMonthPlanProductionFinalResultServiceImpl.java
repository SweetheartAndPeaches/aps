package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cx.entity.*;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.mapper.*;
import com.zlt.aps.cx.mapper.LhScheduleResultMapper;
import com.zlt.aps.cx.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工厂月生产计划服务实现类
 * 
 * @author APS Team
 */
@Service
public class FactoryMonthPlanProductionFinalResultServiceImpl extends ServiceImpl<FactoryMonthPlanProductionFinalResultMapper, FactoryMonthPlanProductionFinalResult> 
        implements FactoryMonthPlanProductionFinalResultService {

    private static final Logger logger = LoggerFactory.getLogger(FactoryMonthPlanProductionFinalResultServiceImpl.class);

    @Autowired
    private FactoryMonthPlanProductionFinalResultMapper factoryMonthPlanProductionFinalResultMapper;

    @Autowired
    private LhScheduleResultMapper lhScheduleResultMapper;

    @Autowired
    private ScheduleMainMapper scheduleMainMapper;

    @Autowired
    private ScheduleDetailMapper scheduleDetailMapper;

    @Autowired
    private MachineMapper machineMapper;

    @Autowired
    private AlgorithmService algorithmService;

    @Override
    public List<FactoryMonthPlanProductionFinalResult> getByYearMonth(Integer yearMonth) {
        return factoryMonthPlanProductionFinalResultMapper.selectByYearMonth(yearMonth);
    }

    @Override
    public List<FactoryMonthPlanProductionFinalResult> getByYearAndMonth(Integer year, Integer month) {
        return factoryMonthPlanProductionFinalResultMapper.selectByYearAndMonth(year, month);
    }

    @Override
    public List<FactoryMonthPlanProductionFinalResult> getByFactoryAndYearMonth(String factoryCode, Integer yearMonth) {
        return factoryMonthPlanProductionFinalResultMapper.selectByFactoryAndYearMonth(factoryCode, yearMonth);
    }

    @Override
    public FactoryMonthPlanProductionFinalResult getByProductionNo(String productionNo) {
        return factoryMonthPlanProductionFinalResultMapper.selectByProductionNo(productionNo);
    }

    @Override
    public Page<FactoryMonthPlanProductionFinalResult> getPage(int pageNum, int pageSize, Integer yearMonth, String factoryCode) {
        Page<FactoryMonthPlanProductionFinalResult> page = new Page<>(pageNum, pageSize);
        return (Page<FactoryMonthPlanProductionFinalResult>) factoryMonthPlanProductionFinalResultMapper.selectPageByYearMonth(page, yearMonth, factoryCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(FactoryMonthPlanProductionFinalResult result) {
        result.setCreateTime(LocalDateTime.now());
        result.setUpdateTime(LocalDateTime.now());
        return factoryMonthPlanProductionFinalResultMapper.insert(result) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(List<FactoryMonthPlanProductionFinalResult> results) {
        for (FactoryMonthPlanProductionFinalResult result : results) {
            result.setCreateTime(LocalDateTime.now());
            result.setUpdateTime(LocalDateTime.now());
        }
        return super.saveBatch(results);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(FactoryMonthPlanProductionFinalResult result) {
        result.setUpdateTime(LocalDateTime.now());
        return factoryMonthPlanProductionFinalResultMapper.updateById(result) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        return factoryMonthPlanProductionFinalResultMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean release(Integer yearMonth, String productionVersion) {
        List<FactoryMonthPlanProductionFinalResult> results = factoryMonthPlanProductionFinalResultMapper.selectByVersion(yearMonth, productionVersion);
        for (FactoryMonthPlanProductionFinalResult result : results) {
            result.setIsRelease("1");
            result.setUpdateTime(LocalDateTime.now());
            factoryMonthPlanProductionFinalResultMapper.updateById(result);
        }
        logger.info("发布月计划成功，年月: {}, 版本: {}, 数量: {}", yearMonth, productionVersion, results.size());
        return true;
    }

    // ==================== 核心功能：月计划拆分为日计划 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<LhScheduleResult> splitToDailyPlan(LocalDate scheduleDate) {
        int yearMonth = Integer.parseInt(scheduleDate.format(DateTimeFormatter.ofPattern("yyyyMM")));
        int day = scheduleDate.getDayOfMonth();
        return splitToDailyPlan(yearMonth, day);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<LhScheduleResult> splitToDailyPlan(Integer yearMonth, Integer day) {
        logger.info("开始拆分月计划到日硫化排程，年月: {}, 日期: {}", yearMonth, day);
        
        // 1. 查询当天有排产的月计划
        List<FactoryMonthPlanProductionFinalResult> results = factoryMonthPlanProductionFinalResultMapper.selectWithPlanOnDay(yearMonth, day);
        if (results == null || results.isEmpty()) {
            logger.info("未找到月计划数据，年月: {}, 日期: {}", yearMonth, day);
            return Collections.emptyList();
        }
        
        // 2. 转换为日硫化排程
        List<LhScheduleResult> dailyPlans = new ArrayList<>();
        int sortIndex = 1;
        LocalDate scheduleDate = convertToDate(yearMonth, day);
        
        for (FactoryMonthPlanProductionFinalResult result : results) {
            Integer planQty = result.getDayQty(day);
            if (planQty == null || planQty <= 0) {
                continue;
            }
            
            LhScheduleResult dailyPlan = new LhScheduleResult();
            dailyPlan.setBatchNo(generateBatchNo(yearMonth, day, sortIndex));
            dailyPlan.setScheduleDate(scheduleDate);
            dailyPlan.setMaterialCode(result.getMaterialCode());
            dailyPlan.setDailyPlanQty(planQty);
            dailyPlan.setStructureName(result.getStructureName());
            dailyPlan.setProductionStatus("PENDING");
            dailyPlan.setMachineOrder(sortIndex);
            dailyPlan.setCreateTime(LocalDateTime.now());
            dailyPlan.setCreateBy("SYSTEM");
            
            dailyPlans.add(dailyPlan);
            sortIndex++;
        }
        
        // 3. 批量保存日硫化排程
        for (LhScheduleResult plan : dailyPlans) {
            lhScheduleResultMapper.insert(plan);
        }
        
        logger.info("月计划拆分完成，生成日硫化排程数量: {}", dailyPlans.size());
        return dailyPlans;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleGenerateResult generateScheduleFromMonthPlan(LocalDate scheduleDate) {
        logger.info("========== 开始从月计划生成排程，日期: {} ==========", scheduleDate);
        
        ScheduleGenerateResult result = new ScheduleGenerateResult();
        result.setScheduleDate(scheduleDate);
        
        try {
            // 1. 拆分月计划到日硫化排程
            List<LhScheduleResult> dailyPlans = splitToDailyPlan(scheduleDate);
            if (dailyPlans.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("未找到当日计划数据");
                return result;
            }
            
            // 2. 转换为日胎胚任务
            List<DailyEmbryoTask> tasks = convertToTasks(dailyPlans, scheduleDate);
            result.setTotalTasks(tasks.size());
            result.setTotalQuantity(tasks.stream().mapToInt(DailyEmbryoTask::getTaskQuantity).sum());
            
            // 3. 获取可用机台
            List<Machine> machines = machineMapper.selectRunningMachines();
            if (machines.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("没有可用的成型机台");
                return result;
            }
            
            // 4. 创建排程主表
            ScheduleMain scheduleMain = createScheduleMain(scheduleDate);
            result.setScheduleMainId(scheduleMain.getId());
            
            // 5. 调用APS核心算法进行排程
            AlgorithmService.AllocationResult allocationResult = algorithmService.allocateTasks(
                tasks, machines, scheduleMain);
            
            if (!allocationResult.isSuccess()) {
                result.setSuccess(false);
                result.setMessage(allocationResult.getMessage());
                return result;
            }
            
            // 6. 保存排程结果
            saveScheduleDetails(allocationResult, scheduleMain, scheduleDate);
            
            // 7. 同步结果到月计划
            syncScheduleResult(scheduleDate);
            
            result.setSuccess(true);
            result.setMessage("排程生成成功");
            
            logger.info("========== 从月计划生成排程完成 ==========");
            
        } catch (Exception e) {
            logger.error("从月计划生成排程失败", e);
            result.setSuccess(false);
            result.setMessage("排程生成失败: " + e.getMessage());
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncScheduleResult(LocalDate scheduleDate) {
        logger.info("开始同步排程结果到月计划，日期: {}", scheduleDate);
        
        int yearMonth = Integer.parseInt(scheduleDate.format(DateTimeFormatter.ofPattern("yyyyMM")));
        int day = scheduleDate.getDayOfMonth();
        
        // 1. 查询当天的排程明细
        List<ScheduleDetail> details = scheduleDetailMapper.selectByMachineAndDate(
            null, scheduleDate.toString());
        
        if (details == null || details.isEmpty()) {
            logger.info("未找到排程明细数据");
            return true;
        }
        
        // 2. 按物料分组统计排产量
        Map<String, Integer> materialQtyMap = details.stream()
            .collect(Collectors.groupingBy(
                ScheduleDetail::getMaterialCode,
                Collectors.summingInt(ScheduleDetail::getPlanQuantity)
            ));
        
        // 3. 按物料分组计算机台分配
        Map<String, String> materialMachineMap = details.stream()
            .collect(Collectors.groupingBy(
                ScheduleDetail::getMaterialCode,
                Collectors.mapping(
                    ScheduleDetail::getMachineCode,
                    Collectors.joining(",")
                )
            ));
        
        // 4. 更新月计划
        List<FactoryMonthPlanProductionFinalResult> results = factoryMonthPlanProductionFinalResultMapper.selectByYearMonth(yearMonth);
        for (FactoryMonthPlanProductionFinalResult planResult : results) {
            String materialCode = planResult.getMaterialCode();
            Integer scheduledQty = materialQtyMap.get(materialCode);
            Integer planQty = planResult.getDayQty(day);
            
            if (planQty != null && planQty > 0) {
                // 更新实际排产量
                if (scheduledQty != null) {
                    planResult.setTotalQty(scheduledQty);
                    planResult.setDifferenceQty(planQty - scheduledQty);
                }
                
                // 更新分配的机台
                String machines = materialMachineMap.get(materialCode);
                if (machines != null) {
                    planResult.setCxMachineCode(machines);
                }
                
                planResult.setUpdateTime(LocalDateTime.now());
                factoryMonthPlanProductionFinalResultMapper.updateById(planResult);
            }
        }
        
        logger.info("同步排程结果完成，更新月计划数量: {}", results.size());
        return true;
    }

    // ==================== 统计与报表 ====================

    @Override
    public int countByYearMonth(Integer yearMonth) {
        return factoryMonthPlanProductionFinalResultMapper.countByYearMonth(yearMonth);
    }

    @Override
    public Long sumTotalQtyByYearMonth(Integer yearMonth) {
        return factoryMonthPlanProductionFinalResultMapper.sumTotalQtyByYearMonth(yearMonth);
    }

    @Override
    public MonthPlanOverview getOverview(Integer yearMonth) {
        MonthPlanOverview overview = new MonthPlanOverview();
        overview.setYearMonth(yearMonth);
        
        List<FactoryMonthPlanProductionFinalResult> results = factoryMonthPlanProductionFinalResultMapper.selectByYearMonth(yearMonth);
        
        overview.setTotalPlanCount(results.size());
        overview.setTotalPlanQty(results.stream()
            .mapToLong(p -> p.getProdReqPlan() != null ? p.getProdReqPlan() : 0)
            .sum());
        overview.setTotalProductionQty(results.stream()
            .mapToLong(p -> p.getTotalQty() != null ? p.getTotalQty() : 0)
            .sum());
        overview.setTotalDifferenceQty(results.stream()
            .mapToLong(p -> p.getDifferenceQty() != null ? p.getDifferenceQty() : 0)
            .sum());
        
        long releasedCount = results.stream()
            .filter(p -> "1".equals(p.getIsRelease()))
            .count();
        overview.setReleasedCount((int) releasedCount);
        overview.setUnreleasedCount(results.size() - (int) releasedCount);
        
        return overview;
    }

    // ==================== 私有方法 ====================

    /**
     * 生成批次号
     */
    private String generateBatchNo(Integer yearMonth, int day, int index) {
        return String.format("LH%d%02d%04d", yearMonth, day, index);
    }

    /**
     * 转换为日期
     */
    private LocalDate convertToDate(Integer yearMonth, int day) {
        int year = yearMonth / 100;
        int month = yearMonth % 100;
        return LocalDate.of(year, month, day);
    }

    /**
     * 转换为日胎胚任务
     */
    private List<DailyEmbryoTask> convertToTasks(List<LhScheduleResult> dailyPlans, LocalDate scheduleDate) {
        return dailyPlans.stream()
            .map(plan -> {
                DailyEmbryoTask task = new DailyEmbryoTask();
                task.setMaterialCode(plan.getMaterialCode());
                task.setTaskQuantity(plan.getDailyPlanQty());
                task.setProductStructure(plan.getStructureName());
                task.setAssignedQuantity(0);
                task.setRemainderQuantity(plan.getDailyPlanQty());
                task.setIsFullyAssigned(0);
                task.setCreateTime(LocalDateTime.now());
                return task;
            })
            .collect(Collectors.toList());
    }

    /**
     * 创建排程主表
     */
    private ScheduleMain createScheduleMain(LocalDate scheduleDate) {
        ScheduleMain main = new ScheduleMain();
        main.setScheduleCode("APS" + scheduleDate.toString().replace("-", "") + 
            String.format("%04d", System.currentTimeMillis() % 10000));
        main.setScheduleDate(scheduleDate);
        main.setScheduleType("NORMAL");
        main.setStatus("DRAFT");
        main.setCreateTime(LocalDateTime.now());
        main.setCreateBy("SYSTEM");
        
        scheduleMainMapper.insert(main);
        return main;
    }

    /**
     * 保存排程明细
     */
    private void saveScheduleDetails(AlgorithmService.AllocationResult allocationResult,
                                    ScheduleMain scheduleMain, LocalDate scheduleDate) {
        for (AlgorithmService.AllocationDetail detail : allocationResult.getDetails()) {
            ScheduleDetail scheduleDetail = new ScheduleDetail();
            scheduleDetail.setMainId(scheduleMain.getId());
            scheduleDetail.setScheduleDate(scheduleDate);
            scheduleDetail.setMachineCode(detail.getMachineCode());
            scheduleDetail.setMaterialCode(detail.getMaterialCode());
            scheduleDetail.setPlanQuantity(detail.getPlanQuantity());
            scheduleDetail.setCompletedQuantity(0);
            scheduleDetail.setStatus("PLANNED");
            
            scheduleDetailMapper.insert(scheduleDetail);
        }
    }
}
