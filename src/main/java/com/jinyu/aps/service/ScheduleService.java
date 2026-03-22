package com.jinyu.aps.service;

import com.jinyu.aps.entity.*;
import com.jinyu.aps.mapper.*;
import com.jinyu.aps.model.entity.MaterialGroup;
import com.jinyu.aps.service.AlgorithmService.AllocationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 排程服务
 * 实现完整的排程生成流程
 * 
 * 流程步骤（按技术文档V5.0.0）：
 * 1. 数据准备与校验
 * 2. 物料分组处理
 * 3. 试错分配算法
 * 4. 班次均衡调整
 * 5. 顺位排序
 * 6. 约束检查与预警
 * 7. 结果保存
 *
 * @author APS Team
 */
@Service
public class ScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

    @Autowired
    private MachineMapper machineMapper;

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private VulcanizingPlanMapper vulcanizingPlanMapper;

    @Autowired
    private ScheduleMainMapper scheduleMainMapper;

    @Autowired
    private ScheduleDetailMapper scheduleDetailMapper;

    @Autowired
    private DailyEmbryoTaskMapper dailyEmbryoTaskMapper;

    @Autowired
    private AlgorithmService algorithmService;

    @Autowired
    private AlertConfigMapper alertConfigMapper;

    /**
     * 生成排程
     * 
     * @param scheduleDate 排程日期
     * @return 排程结果
     */
    @Transactional(rollbackFor = Exception.class)
    public ScheduleGenerateResult generateSchedule(LocalDate scheduleDate) {
        logger.info("========== 开始生成排程，日期: {} ==========", scheduleDate);
        
        ScheduleGenerateResult result = new ScheduleGenerateResult();
        result.setScheduleDate(scheduleDate);
        
        try {
            // 1. 数据准备与校验
            logger.info("步骤1: 数据准备与校验...");
            ScheduleContext context = prepareScheduleData(scheduleDate);
            if (context.getErrors().size() > 0) {
                result.setSuccess(false);
                result.setErrors(context.getErrors());
                result.setMessage(String.join("; ", context.getErrors()));
                return result;
            }
            
            // 2. 创建排程主表
            logger.info("步骤2: 创建排程主表...");
            ScheduleMain scheduleMain = createScheduleMain(scheduleDate);
            context.setScheduleMain(scheduleMain);
            
            // 3. 生成日胎胚任务
            logger.info("步骤3: 生成日胎胚任务...");
            List<DailyEmbryoTask> tasks = generateDailyTasks(context);
            context.setTasks(tasks);
            
            // 4. 物料分组处理
            logger.info("步骤4: 物料分组处理...");
            List<MaterialGroup> materialGroups = groupMaterials(context);
            context.setMaterialGroups(materialGroups);
            
            // 5. S5.3.1 续作排产 - 优先处理续作任务
            logger.info("步骤5: S5.3.1 续作排产...");
            List<ScheduleDetail> continueDetails = processContinueProduction(context);
            logger.info("续作排产完成，生成续作明细数: {}", continueDetails.size());
            
            // 6. S5.3.2 新增规格排产 - 试错分配算法
            logger.info("步骤6: S5.3.2 新增规格排产（试错分配算法）...");
            AllocationResult allocationResult = algorithmService.allocateTasks(
                tasks, context.getMachines(), scheduleMain);
            
            if (!allocationResult.isSuccess()) {
                result.setSuccess(false);
                result.setMessage(allocationResult.getMessage());
                return result;
            }
            
            // 7. 转换分配结果为排程明细
            logger.info("步骤7: 转换分配结果...");
            List<ScheduleDetail> newDetails = convertAllocationToDetails(allocationResult, context);
            
            // 8. 合并续作明细和新增明细
            List<ScheduleDetail> allDetails = new ArrayList<>();
            allDetails.addAll(continueDetails);  // 续作明细在前
            allDetails.addAll(newDetails);       // 新增明细在后
            
            // 9. 班次均衡调整
            logger.info("步骤9: 班次均衡调整...");
            String shiftRatio = getAlertConfigValue("SHIFT_BALANCE_RATIO", "1:2:1");
            allDetails = algorithmService.balanceShiftDistribution(allDetails, shiftRatio);
            
            // 10. 顺位排序（续作已在前面，排序时会保持续作优先）
            logger.info("步骤10: 顺位排序...");
            Map<String, Double> stockHours = calculateStockHours(context);
            allDetails = algorithmService.sortScheduleSequence(allDetails, stockHours);
            
            // 11. 约束检查与预警
            logger.info("步骤11: 约束检查与预警...");
            List<String> warnings = checkConstraintsAndGenerateWarnings(allDetails, context);
            result.setWarnings(warnings);
            
            // 12. 保存结果
            logger.info("步骤12: 保存排程结果...");
            saveScheduleResult(scheduleMain, allDetails, tasks);
            
            // 13. 更新排程主表统计信息
            updateScheduleMainStatistics(scheduleMain, allDetails);
            
            result.setSuccess(true);
            result.setMessage("排程生成成功");
            result.setScheduleMain(scheduleMain);
            result.setDetails(allDetails);
            result.setTotalMachines(context.getMachines().size());
            result.setTotalQuantity(allDetails.stream().mapToInt(ScheduleDetail::getPlanQuantity).sum());
            result.setTotalVehicles((int) allDetails.stream().filter(d -> d.getTripNo() != null).count());
            
            logger.info("========== 排程生成完成 ==========");
            
        } catch (Exception e) {
            logger.error("排程生成失败", e);
            result.setSuccess(false);
            result.setMessage("排程生成失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * S5.3.1 续作排产
     * 核心逻辑：
     * 1. 遍历所有机台
     * 2. 检查机台是否有在产结构（structure字段）
     * 3. 如果有在产结构，查找对应的任务
     * 4. 创建续作排程明细，标记 is_continue = true
     * 
     * @param context 排程上下文
     * @return 续作排程明细列表
     */
    private List<ScheduleDetail> processContinueProduction(ScheduleContext context) {
        List<ScheduleDetail> continueDetails = new ArrayList<>();
        
        // 构建物料编码到产品结构的映射
        Map<String, String> materialToStructure = new HashMap<>();
        for (Material material : context.getMaterialMap().values()) {
            if (material.getProductStructure() != null) {
                materialToStructure.put(material.getMaterialCode(), material.getProductStructure());
            }
        }
        
        // 构建产品结构到任务列表的映射
        Map<String, List<DailyEmbryoTask>> structureToTasks = new HashMap<>();
        for (DailyEmbryoTask task : context.getTasks()) {
            String structure = task.getProductStructure();
            if (structure != null) {
                structureToTasks.computeIfAbsent(structure, k -> new ArrayList<>()).add(task);
            }
        }
        
        // 遍历机台，处理续作
        for (Machine machine : context.getMachines()) {
            String inProductionStructure = machine.getStructure();
            
            // 跳过没有在产结构的机台
            if (inProductionStructure == null || inProductionStructure.trim().isEmpty()) {
                logger.debug("机台 {} 无在产结构，跳过续作处理", machine.getMachineCode());
                continue;
            }
            
            logger.info("机台 {} 在产结构: {}，处理续作...", machine.getMachineCode(), inProductionStructure);
            
            // 查找该结构对应的任务
            List<DailyEmbryoTask> matchedTasks = structureToTasks.get(inProductionStructure);
            if (matchedTasks == null || matchedTasks.isEmpty()) {
                logger.info("机台 {} 在产结构 {} 无对应任务，跳过", machine.getMachineCode(), inProductionStructure);
                continue;
            }
            
            // 找到有剩余量的任务
            for (DailyEmbryoTask task : matchedTasks) {
                if (task.getRemainderQuantity() != null && task.getRemainderQuantity() > 0) {
                    // 计算续作计划量（取机台日产能或任务剩余量的较小值）
                    int continueQty = Math.min(
                        machine.getMaxDailyCapacity() != null ? machine.getMaxDailyCapacity() : 120,
                        task.getRemainderQuantity()
                    );
                    
                    // 创建续作排程明细
                    ScheduleDetail detail = new ScheduleDetail();
                    detail.setMainId(context.getScheduleMain().getId());
                    detail.setScheduleDate(context.getScheduleDate());
                    detail.setMachineCode(machine.getMachineCode());
                    detail.setMaterialCode(task.getMaterialCode());
                    detail.setPlanQuantity(continueQty);
                    detail.setCompletedQuantity(0);
                    
                    // 设置续作标识
                    detail.setIsContinue(1);
                    detail.setProductionMode("CONTINUE");
                    
                    // 设置物料属性
                    Material material = context.getMaterialMap().get(task.getMaterialCode());
                    if (material != null) {
                        detail.setProductStructure(material.getProductStructure());
                        detail.setIsMainProduct(material.getIsMainProduct() == 1);
                    }
                    
                    // 分配班次（续作优先分配到夜班，符合波浪交替）
                    detail.setShiftCode("NIGHT");
                    
                    detail.setStatus("PLANNED");
                    detail.setCreateTime(LocalDateTime.now());
                    
                    continueDetails.add(detail);
                    
                    // 更新任务剩余量
                    task.setAssignedQuantity(task.getAssignedQuantity() + continueQty);
                    task.setRemainderQuantity(task.getRemainderQuantity() - continueQty);
                    if (task.getRemainderQuantity() <= 0) {
                        task.setIsFullyAssigned(1);
                    }
                    
                    logger.info("机台 {} 续作排产: 物料={}, 计划量={}, 剩余量={}",
                        machine.getMachineCode(), task.getMaterialCode(), continueQty, task.getRemainderQuantity());
                    
                    // 每个机台只处理一个续作任务（同一结构）
                    break;
                }
            }
        }
        
        return continueDetails;
    }

    /**
     * 准备排程数据
     */
    private ScheduleContext prepareScheduleData(LocalDate scheduleDate) {
        ScheduleContext context = new ScheduleContext();
        context.setScheduleDate(scheduleDate);
        
        // 获取可用机台
        List<Machine> machines = machineMapper.selectAll();
        if (machines == null || machines.isEmpty()) {
            context.addError("没有可用的成型机台");
            return context;
        }
        context.setMachines(machines);
        
        // 获取硫化计划
        List<VulcanizingPlan> plans = vulcanizingPlanMapper.selectByDate(scheduleDate);
        if (plans == null || plans.isEmpty()) {
            context.addError("没有当日的硫化计划");
            return context;
        }
        context.setVulcanizingPlans(plans);
        
        // 获取物料信息
        List<Material> materials = materialMapper.selectAll();
        Map<String, Material> materialMap = materials.stream()
            .collect(Collectors.toMap(Material::getMaterialCode, m -> m));
        context.setMaterialMap(materialMap);
        
        // 获取库存信息
        List<Stock> stocks = stockMapper.selectAll();
        Map<String, Stock> stockMap = stocks.stream()
            .collect(Collectors.toMap(Stock::getMaterialCode, s -> s));
        context.setStockMap(stockMap);
        
        return context;
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
     * 生成日胎胚任务
     */
    private List<DailyEmbryoTask> generateDailyTasks(ScheduleContext context) {
        List<DailyEmbryoTask> tasks = new ArrayList<>();
        int sortOrder = 1;
        
        for (VulcanizingPlan plan : context.getVulcanizingPlans()) {
            DailyEmbryoTask task = new DailyEmbryoTask();
            task.setScheduleMainId(context.getScheduleMain().getId());
            task.setTaskGroupId("TG_" + plan.getMaterialCode());
            task.setMaterialCode(plan.getMaterialCode());
            task.setTaskQuantity(plan.getPlanQuantity());
            
            // 设置物料属性
            Material material = context.getMaterialMap().get(plan.getMaterialCode());
            if (material != null) {
                task.setProductStructure(material.getProductStructure());
                task.setIsMainProduct(material.getIsMainProduct());
                task.setMaterialName(material.getMaterialName());
            }
            
            // 设置库存信息
            Stock stock = context.getStockMap().get(plan.getMaterialCode());
            if (stock != null) {
                task.setStockQuantity(stock.getCurrentStock());
                task.setVulcanizeMachineCount(stock.getVulcanizeMachineCount());
            }
            
            task.setPriority(plan.getPriority());
            task.setSortOrder(sortOrder++);
            task.setAssignedQuantity(0);
            task.setRemainderQuantity(plan.getPlanQuantity());
            task.setIsFullyAssigned(0);
            task.setCreateTime(LocalDateTime.now());
            
            tasks.add(task);
            dailyEmbryoTaskMapper.insert(task);
        }
        
        return tasks;
    }

    /**
     * 物料分组处理
     */
    private List<MaterialGroup> groupMaterials(ScheduleContext context) {
        Map<String, MaterialGroup> groupMap = new HashMap<>();
        
        for (DailyEmbryoTask task : context.getTasks()) {
            String structure = task.getProductStructure();
            MaterialGroup group = groupMap.computeIfAbsent(
                structure, MaterialGroup::new);
            
            group.addMaterial(
                task.getMaterialCode(),
                task.getTaskQuantity(),
                task.getStockQuantity() != null ? task.getStockQuantity() : 0,
                task.getVulcanizeMachineCount() != null ? task.getVulcanizeMachineCount() : 0,
                task.getIsMainProduct() != null && task.getIsMainProduct() == 1
            );
        }
        
        return new ArrayList<>(groupMap.values());
    }

    /**
     * 转换分配结果为排程明细
     */
    private List<ScheduleDetail> convertAllocationToDetails(AllocationResult allocationResult, 
                                                           ScheduleContext context) {
        List<ScheduleDetail> details = new ArrayList<>();
        
        for (AlgorithmService.AllocationDetail allocDetail : allocationResult.getDetails()) {
            ScheduleDetail detail = new ScheduleDetail();
            detail.setMainId(allocDetail.getMainId());
            detail.setScheduleDate(allocDetail.getScheduleDate());
            detail.setMachineCode(allocDetail.getMachineCode());
            detail.setMaterialCode(allocDetail.getMaterialCode());
            detail.setPlanQuantity(allocDetail.getPlanQuantity());
            detail.setCompletedQuantity(0);
            
            // 分配班次（简化处理，按顺序轮换）
            String shiftCode = assignShiftCode(details.size());
            detail.setShiftCode(shiftCode);
            
            // 设置物料属性
            Material material = context.getMaterialMap().get(allocDetail.getMaterialCode());
            if (material != null) {
                detail.setProductStructure(material.getProductStructure());
                detail.setIsMainProduct(material.getIsMainProduct() == 1);
            }
            
            detail.setStatus("PLANNED");
            detail.setCreateTime(LocalDateTime.now());
            
            details.add(detail);
        }
        
        return details;
    }

    /**
     * 分配班次（简化处理）
     */
    private String assignShiftCode(int index) {
        String[] shifts = {"NIGHT", "DAY", "AFTERNOON"};
        // 按1:2:1的比例分配
        int[] ratios = {1, 3, 4};  // 累计比例
        int total = 4;
        
        int mod = index % total;
        for (int i = 0; i < ratios.length; i++) {
            if (mod < ratios[i]) {
                return shifts[i];
            }
        }
        return shifts[0];
    }

    /**
     * 计算库存可供硫化时长
     */
    private Map<String, Double> calculateStockHours(ScheduleContext context) {
        Map<String, Double> stockHours = new HashMap<>();
        
        for (DailyEmbryoTask task : context.getTasks()) {
            String materialCode = task.getMaterialCode();
            Stock stock = context.getStockMap().get(materialCode);
            
            if (stock != null && stock.getVulcanizeMachineCount() != null 
                && stock.getVulcanizeMachineCount() > 0) {
                // 库存可供硫化时长 = 库存量 / (硫化机台数 × 4车/小时 × 12条/车)
                double hours = stock.getCurrentStock() / 
                    (stock.getVulcanizeMachineCount() * 48.0);
                stockHours.put(materialCode, hours);
            } else {
                stockHours.put(materialCode, 0.0);
            }
        }
        
        return stockHours;
    }

    /**
     * 约束检查与预警生成
     */
    private List<String> checkConstraintsAndGenerateWarnings(List<ScheduleDetail> details, 
                                                             ScheduleContext context) {
        List<String> warnings = new ArrayList<>();
        
        // 1. 检查单机台SKU种类数
        Map<String, Set<String>> machineMaterials = details.stream()
            .collect(Collectors.groupingBy(
                ScheduleDetail::getMachineCode,
                Collectors.mapping(ScheduleDetail::getMaterialCode, Collectors.toSet())
            ));
        
        int maxSkuPerMachine = Integer.parseInt(
            getAlertConfigValue("MAX_SKU_PER_MACHINE_PER_DAY", "4"));
        
        for (Map.Entry<String, Set<String>> entry : machineMaterials.entrySet()) {
            if (entry.getValue().size() > maxSkuPerMachine) {
                warnings.add(String.format("机台%s的SKU种类数(%d)超过限制(%d)",
                    entry.getKey(), entry.getValue().size(), maxSkuPerMachine));
            }
        }
        
        // 2. 检查库存预警
        Map<String, Double> stockHours = calculateStockHours(context);
        double lowThreshold = Double.parseDouble(
            getAlertConfigValue("INVENTORY_LOW_HOURS", "4"));
        double highThreshold = Double.parseDouble(
            getAlertConfigValue("INVENTORY_HIGH_HOURS", "18"));
        
        for (Map.Entry<String, Double> entry : stockHours.entrySet()) {
            if (entry.getValue() < lowThreshold) {
                warnings.add(String.format("物料%s库存不足(%.1f小时)，建议优先排产",
                    entry.getKey(), entry.getValue()));
            } else if (entry.getValue() > highThreshold) {
                warnings.add(String.format("物料%s库存偏高(%.1f小时)，建议减少排产",
                    entry.getKey(), entry.getValue()));
            }
        }
        
        // 3. 检查任务是否全部分配
        for (DailyEmbryoTask task : context.getTasks()) {
            if (task.getRemainderQuantity() != null && task.getRemainderQuantity() > 0) {
                warnings.add(String.format("物料%s仍有%d条未分配",
                    task.getMaterialCode(), task.getRemainderQuantity()));
            }
        }
        
        return warnings;
    }

    /**
     * 保存排程结果
     */
    private void saveScheduleResult(ScheduleMain main, List<ScheduleDetail> details, 
                                    List<DailyEmbryoTask> tasks) {
        // 保存明细
        for (ScheduleDetail detail : details) {
            detail.setMainId(main.getId());
            scheduleDetailMapper.insert(detail);
        }
        
        // 更新任务状态
        for (DailyEmbryoTask task : tasks) {
            dailyEmbryoTaskMapper.updateById(task);
        }
    }

    /**
     * 更新排程主表统计信息
     */
    private void updateScheduleMainStatistics(ScheduleMain main, List<ScheduleDetail> details) {
        main.setTotalMachines((int) details.stream()
            .map(ScheduleDetail::getMachineCode)
            .distinct()
            .count());
        main.setTotalQuantity(details.stream()
            .mapToInt(ScheduleDetail::getPlanQuantity)
            .sum());
        main.setTotalVehicles((int) details.stream()
            .filter(d -> d.getTripNo() != null)
            .map(ScheduleDetail::getTripGroupId)
            .distinct()
            .count());
        
        scheduleMainMapper.updateById(main);
    }

    /**
     * 获取预警配置值
     */
    private String getAlertConfigValue(String configCode, String defaultValue) {
        try {
            AlertConfig config = alertConfigMapper.selectByCode(configCode);
            if (config != null) {
                return config.getConfigValue();
            }
        } catch (Exception e) {
            logger.warn("获取预警配置失败: {}", configCode);
        }
        return defaultValue;
    }

    /**
     * 确认排程
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmSchedule(Long scheduleId, String operator) {
        ScheduleMain main = scheduleMainMapper.selectById(scheduleId);
        if (main == null) {
            throw new RuntimeException("排程不存在");
        }
        
        if (!"DRAFT".equals(main.getStatus())) {
            throw new RuntimeException("只有草稿状态的排程才能确认");
        }
        
        main.setStatus("CONFIRMED");
        main.setConfirmTime(LocalDateTime.now());
        main.setConfirmBy(operator);
        
        return scheduleMainMapper.updateById(main) > 0;
    }

    /**
     * 查询排程明细
     */
    public List<ScheduleDetail> getScheduleDetails(Long scheduleId) {
        return scheduleDetailMapper.selectByMainId(scheduleId);
    }

    /**
     * 查询排程列表
     */
    public List<ScheduleMain> getScheduleList(LocalDate startDate, LocalDate endDate) {
        return scheduleMainMapper.selectByDateRange(startDate, endDate);
    }

    // ==================== 内部类定义 ====================

    /**
     * 排程上下文
     */
    private static class ScheduleContext {
        private LocalDate scheduleDate;
        private ScheduleMain scheduleMain;
        private List<Machine> machines;
        private List<VulcanizingPlan> vulcanizingPlans;
        private List<DailyEmbryoTask> tasks;
        private List<MaterialGroup> materialGroups;
        private Map<String, Material> materialMap;
        private Map<String, Stock> stockMap;
        private List<String> errors = new ArrayList<>();

        // Getters and Setters
        public LocalDate getScheduleDate() { return scheduleDate; }
        public void setScheduleDate(LocalDate scheduleDate) { this.scheduleDate = scheduleDate; }
        public ScheduleMain getScheduleMain() { return scheduleMain; }
        public void setScheduleMain(ScheduleMain scheduleMain) { this.scheduleMain = scheduleMain; }
        public List<Machine> getMachines() { return machines; }
        public void setMachines(List<Machine> machines) { this.machines = machines; }
        public List<VulcanizingPlan> getVulcanizingPlans() { return vulcanizingPlans; }
        public void setVulcanizingPlans(List<VulcanizingPlan> vulcanizingPlans) { this.vulcanizingPlans = vulcanizingPlans; }
        public List<DailyEmbryoTask> getTasks() { return tasks; }
        public void setTasks(List<DailyEmbryoTask> tasks) { this.tasks = tasks; }
        public List<MaterialGroup> getMaterialGroups() { return materialGroups; }
        public void setMaterialGroups(List<MaterialGroup> materialGroups) { this.materialGroups = materialGroups; }
        public Map<String, Material> getMaterialMap() { return materialMap; }
        public void setMaterialMap(Map<String, Material> materialMap) { this.materialMap = materialMap; }
        public Map<String, Stock> getStockMap() { return stockMap; }
        public void setStockMap(Map<String, Stock> stockMap) { this.stockMap = stockMap; }
        public List<String> getErrors() { return errors; }
        public void addError(String error) { this.errors.add(error); }
    }

    /**
     * 排程生成结果
     */
    public static class ScheduleGenerateResult {
        private boolean success;
        private String message;
        private LocalDate scheduleDate;
        private ScheduleMain scheduleMain;
        private List<ScheduleDetail> details;
        private List<String> warnings;
        private List<String> errors;
        private int totalMachines;
        private int totalQuantity;
        private int totalVehicles;

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public LocalDate getScheduleDate() { return scheduleDate; }
        public void setScheduleDate(LocalDate scheduleDate) { this.scheduleDate = scheduleDate; }
        public ScheduleMain getScheduleMain() { return scheduleMain; }
        public void setScheduleMain(ScheduleMain scheduleMain) { this.scheduleMain = scheduleMain; }
        public List<ScheduleDetail> getDetails() { return details; }
        public void setDetails(List<ScheduleDetail> details) { this.details = details; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        public int getTotalMachines() { return totalMachines; }
        public void setTotalMachines(int totalMachines) { this.totalMachines = totalMachines; }
        public int getTotalQuantity() { return totalQuantity; }
        public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }
        public int getTotalVehicles() { return totalVehicles; }
        public void setTotalVehicles(int totalVehicles) { this.totalVehicles = totalVehicles; }
    }
}
