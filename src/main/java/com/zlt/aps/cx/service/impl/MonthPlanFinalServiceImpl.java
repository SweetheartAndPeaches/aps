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
 * 月度生产计划服务实现类
 * 
 * @author APS Team
 */
@Service
public class MonthPlanFinalServiceImpl extends ServiceImpl<MonthPlanFinalMapper, MonthPlanFinal> 
        implements MonthPlanFinalService {

    private static final Logger logger = LoggerFactory.getLogger(MonthPlanFinalServiceImpl.class);

    @Autowired
    private MonthPlanFinalMapper monthPlanFinalMapper;

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
    public List<MonthPlanFinal> getByYearMonth(Integer yearMonth) {
        return monthPlanFinalMapper.selectByYearMonth(yearMonth);
    }

    @Override
    public List<MonthPlanFinal> getByYearAndMonth(Integer year, Integer month) {
        return monthPlanFinalMapper.selectByYearAndMonth(year, month);
    }

    @Override
    public List<MonthPlanFinal> getByFactoryAndYearMonth(String factoryCode, Integer yearMonth) {
        return monthPlanFinalMapper.selectByFactoryAndYearMonth(factoryCode, yearMonth);
    }

    @Override
    public MonthPlanFinal getByProductionNo(String productionNo) {
        return monthPlanFinalMapper.selectByProductionNo(productionNo);
    }

    @Override
    public Page<MonthPlanFinal> getPage(int pageNum, int pageSize, Integer yearMonth, String factoryCode) {
        Page<MonthPlanFinal> page = new Page<>(pageNum, pageSize);
        return (Page<MonthPlanFinal>) monthPlanFinalMapper.selectPageByYearMonth(page, yearMonth, factoryCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(MonthPlanFinal monthPlan) {
        monthPlan.setCreateTime(LocalDateTime.now());
        monthPlan.setUpdateTime(LocalDateTime.now());
        return monthPlanFinalMapper.insert(monthPlan) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(List<MonthPlanFinal> monthPlans) {
        for (MonthPlanFinal plan : monthPlans) {
            plan.setCreateTime(LocalDateTime.now());
            plan.setUpdateTime(LocalDateTime.now());
        }
        return super.saveBatch(monthPlans);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(MonthPlanFinal monthPlan) {
        monthPlan.setUpdateTime(LocalDateTime.now());
        return monthPlanFinalMapper.updateById(monthPlan) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        return monthPlanFinalMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean release(Integer yearMonth, String productionVersion) {
        List<MonthPlanFinal> plans = monthPlanFinalMapper.selectByVersion(yearMonth, productionVersion);
        for (MonthPlanFinal plan : plans) {
            plan.setIsRelease("1");
            plan.setUpdateTime(LocalDateTime.now());
            monthPlanFinalMapper.updateById(plan);
        }
        logger.info("发布月计划成功，年月: {}, 版本: {}, 数量: {}", yearMonth, productionVersion, plans.size());
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
        List<MonthPlanFinal> monthPlans = monthPlanFinalMapper.selectWithPlanOnDay(yearMonth, day);
        if (monthPlans == null || monthPlans.isEmpty()) {
            logger.info("未找到月计划数据，年月: {}, 日期: {}", yearMonth, day);
            return Collections.emptyList();
        }
        
        // 2. 转换为日硫化排程
        List<LhScheduleResult> dailyPlans = new ArrayList<>();
        int sortIndex = 1;
        LocalDate scheduleDate = convertToDate(yearMonth, day);
        
        for (MonthPlanFinal monthPlan : monthPlans) {
            Integer planQty = monthPlan.getDayQty(day);
            if (planQty == null || planQty <= 0) {
                continue;
            }
            
            LhScheduleResult dailyPlan = new LhScheduleResult();
            dailyPlan.setBatchNo(generateBatchNo(yearMonth, day, sortIndex));
            dailyPlan.setScheduleDate(scheduleDate);
            dailyPlan.setMaterialCode(monthPlan.getMaterialCode());
            dailyPlan.setDailyPlanQty(planQty);
            dailyPlan.setStructureName(monthPlan.getProductStructure());
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
        List<MonthPlanFinal> monthPlans = monthPlanFinalMapper.selectByYearMonth(yearMonth);
        for (MonthPlanFinal plan : monthPlans) {
            String materialCode = plan.getMaterialCode();
            Integer scheduledQty = materialQtyMap.get(materialCode);
            Integer planQty = plan.getDayQty(day);
            
            if (planQty != null && planQty > 0) {
                // 更新实际排产量
                if (scheduledQty != null) {
                    plan.setTotalQty(scheduledQty);
                    plan.setDifferenceQty(planQty - scheduledQty);
                }
                
                // 更新分配的机台
                String machines = materialMachineMap.get(materialCode);
                if (machines != null) {
                    plan.setCxMachineCode(machines);
                }
                
                plan.setUpdateTime(LocalDateTime.now());
                monthPlanFinalMapper.updateById(plan);
            }
        }
        
        logger.info("同步排程结果完成，更新月计划数量: {}", monthPlans.size());
        return true;
    }

    // ==================== 统计与报表 ====================

    @Override
    public int countByYearMonth(Integer yearMonth) {
        return monthPlanFinalMapper.countByYearMonth(yearMonth);
    }

    @Override
    public Long sumTotalQtyByYearMonth(Integer yearMonth) {
        return monthPlanFinalMapper.sumTotalQtyByYearMonth(yearMonth);
    }

    @Override
    public MonthPlanOverview getOverview(Integer yearMonth) {
        MonthPlanOverview overview = new MonthPlanOverview();
        overview.setYearMonth(yearMonth);
        
        List<MonthPlanFinal> plans = monthPlanFinalMapper.selectByYearMonth(yearMonth);
        
        overview.setTotalPlanCount(plans.size());
        overview.setTotalPlanQty(plans.stream()
            .mapToLong(p -> p.getProdReqPlan() != null ? p.getProdReqPlan() : 0)
            .sum());
        overview.setTotalProductionQty(plans.stream()
            .mapToLong(p -> p.getTotalQty() != null ? p.getTotalQty() : 0)
            .sum());
        overview.setTotalDifferenceQty(plans.stream()
            .mapToLong(p -> p.getDifferenceQty() != null ? p.getDifferenceQty() : 0)
            .sum());
        
        long releasedCount = plans.stream()
            .filter(p -> "1".equals(p.getIsRelease()))
            .count();
        overview.setReleasedCount((int) releasedCount);
        overview.setUnreleasedCount(plans.size() - (int) releasedCount);
        
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
