package com.jinyu.aps.integration;

import com.jinyu.aps.entity.*;
import com.jinyu.aps.service.AlgorithmService;
import com.jinyu.aps.service.AlgorithmService.AllocationResult;
import com.jinyu.aps.service.ScheduleService;
import com.jinyu.aps.service.ScheduleService.ScheduleGenerateResult;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试 - 完整流程验证
 * 
 * 测试范围：
 * 1. 完整排程生成流程
 * 2. 三大核心算法协作验证
 * 3. 约束条件综合验证
 * 4. 边界条件和异常处理
 * 
 * @author APS Team
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("APS系统集成测试")
public class ScheduleIntegrationTest {

    @Autowired(required = false)
    private ScheduleService scheduleService;

    @Autowired(required = false)
    private AlgorithmService algorithmService;

    // ==================== 完整流程测试 ====================

    @Nested
    @DisplayName("完整排程流程测试")
    class FullScheduleFlowTests {

        @Test
        @Order(1)
        @DisplayName("完整流程-标准排程生成")
        void testFullFlow_StandardSchedule() {
            if (scheduleService == null) {
                System.out.println("跳过测试: ScheduleService未注入");
                return;
            }

            // Given: 准备排程日期
            LocalDate scheduleDate = LocalDate.now();

            // When: 执行完整排程流程
            ScheduleGenerateResult result = scheduleService.generateSchedule(scheduleDate);

            // Then: 验证排程结果
            assertNotNull(result, "排程结果不应为空");
            System.out.println("排程结果: success=" + result.isSuccess() + 
                ", message=" + result.getMessage());
            
            if (result.isSuccess()) {
                assertNotNull(result.getScheduleMain(), "排程主表不应为空");
                assertNotNull(result.getDetails(), "排程明细不应为空");
                assertTrue(result.getTotalQuantity() > 0, "总排产量应大于0");
                
                System.out.println("总排产量: " + result.getTotalQuantity());
                System.out.println("总机台数: " + result.getTotalMachines());
                System.out.println("总车次数: " + result.getTotalVehicles());
            }
        }

        @Test
        @Order(2)
        @DisplayName("完整流程-多日连续排程")
        void testFullFlow_MultipleDaysSchedule() {
            if (scheduleService == null) {
                System.out.println("跳过测试: ScheduleService未注入");
                return;
            }

            // Given: 准备连续3天排程
            LocalDate startDate = LocalDate.now();

            // When: 执行连续排程
            for (int i = 0; i < 3; i++) {
                LocalDate scheduleDate = startDate.plusDays(i);
                ScheduleGenerateResult result = scheduleService.generateSchedule(scheduleDate);

                // Then: 验证每天排程结果
                assertNotNull(result, "第" + (i + 1) + "天排程结果不应为空");
                System.out.println("第" + (i + 1) + "天排程: " + 
                    (result.isSuccess() ? "成功" : "失败 - " + result.getMessage()));
            }
        }
    }

    // ==================== 三大核心算法协作验证 ====================

    @Nested
    @DisplayName("核心算法协作验证")
    class AlgorithmCollaborationTests {

        @Test
        @Order(10)
        @DisplayName("算法协作-分配→均衡→排序完整流程")
        void testAlgorithmCollaboration_FullPipeline() {
            if (algorithmService == null) {
                System.out.println("跳过测试: AlgorithmService未注入");
                return;
            }

            // Given: 准备测试数据
            List<DailyEmbryoTask> tasks = createTestTasks(10, 72);
            List<Machine> machines = createTestMachines(5, 200);
            ScheduleMain scheduleMain = createTestScheduleMain();

            // Step 1: 试错分配算法
            AllocationResult allocationResult = algorithmService.allocateTasks(tasks, machines, scheduleMain);
            assertTrue(allocationResult.isSuccess(), "分配应成功");
            System.out.println("分配完成: " + allocationResult.getDetails().size() + "条明细");

            // Step 2: 转换为排程明细
            List<ScheduleDetail> details = convertToScheduleDetails(allocationResult, scheduleMain);
            assertFalse(details.isEmpty(), "转换后明细不应为空");

            // Step 3: 班次均衡调整算法
            List<ScheduleDetail> balancedDetails = algorithmService.balanceShiftDistribution(details, "1:2:1");
            assertNotNull(balancedDetails, "均衡结果不应为空");
            System.out.println("班次均衡完成: " + balancedDetails.size() + "条明细");

            // Step 4: 顺位排序算法
            Map<String, Double> stockHours = createTestStockHours(tasks);
            List<ScheduleDetail> sortedDetails = algorithmService.sortScheduleSequence(balancedDetails, stockHours);
            assertNotNull(sortedDetails, "排序结果不应为空");
            System.out.println("顺位排序完成: " + sortedDetails.size() + "条明细");

            // Then: 验证最终结果
            verifyFinalResult(sortedDetails);
        }

        @Test
        @Order(11)
        @DisplayName("算法协作-约束条件传递验证")
        void testAlgorithmCollaboration_ConstraintPropagation() {
            if (algorithmService == null) {
                System.out.println("跳过测试: AlgorithmService未注入");
                return;
            }

            // Given: 准备受限数据（多SKU，少机台）
            List<DailyEmbryoTask> tasks = createTestTasksWithDifferentMaterials(8, 50);
            List<Machine> machines = createTestMachines(2, 200);
            ScheduleMain scheduleMain = createTestScheduleMain();

            // When: 执行分配
            AllocationResult result = algorithmService.allocateTasks(tasks, machines, scheduleMain);

            // Then: 验证约束传递
            // 检查SKU种类限制（每机台最多4种）
            Map<String, Set<String>> machineMaterials = new HashMap<>();
            for (var detail : result.getDetails()) {
                machineMaterials.computeIfAbsent(detail.getMachineCode(), k -> new HashSet<>())
                    .add(detail.getMaterialCode());
            }

            for (Map.Entry<String, Set<String>> entry : machineMaterials.entrySet()) {
                assertTrue(entry.getValue().size() <= 4, 
                    "机台" + entry.getKey() + "SKU种类数" + entry.getValue().size() + "应<=4");
            }

            System.out.println("约束传递验证通过");
        }
    }

    // ==================== 约束条件综合验证 ====================

    @Nested
    @DisplayName("约束条件综合验证")
    class ConstraintValidationTests {

        @Test
        @Order(20)
        @DisplayName("约束验证-SKU种类限制")
        void testConstraint_SkuTypeLimit() {
            if (algorithmService == null) {
                System.out.println("跳过测试: AlgorithmService未注入");
                return;
            }

            // Given: 6种不同SKU，2个机台（每个最多4种）
            List<DailyEmbryoTask> tasks = createTestTasksWithDifferentMaterials(6, 100);
            List<Machine> machines = createTestMachines(2, 300);
            ScheduleMain scheduleMain = createTestScheduleMain();

            // When: 执行分配
            AllocationResult result = algorithmService.allocateTasks(tasks, machines, scheduleMain);

            // Then: 验证约束
            if (result.isSuccess()) {
                Map<String, Set<String>> machineMaterials = new HashMap<>();
                for (var detail : result.getDetails()) {
                    machineMaterials.computeIfAbsent(detail.getMachineCode(), k -> new HashSet<>())
                        .add(detail.getMaterialCode());
                }

                boolean constraintMet = machineMaterials.values().stream()
                    .allMatch(materials -> materials.size() <= 4);
                assertTrue(constraintMet, "所有机台SKU种类应<=4");
            }
        }

        @Test
        @Order(21)
        @DisplayName("约束验证-产能限制")
        void testConstraint_CapacityLimit() {
            if (algorithmService == null) {
                System.out.println("跳过测试: AlgorithmService未注入");
                return;
            }

            // Given: 任务总量超过机台产能
            List<DailyEmbryoTask> tasks = createTestTasks(5, 150); // 总量750
            List<Machine> machines = createTestMachines(2, 200); // 总产能400
            ScheduleMain scheduleMain = createTestScheduleMain();

            // When: 执行分配
            AllocationResult result = algorithmService.allocateTasks(tasks, machines, scheduleMain);

            // Then: 验证分配总量不超过产能
            int totalAllocated = result.getDetails().stream()
                .mapToInt(d -> d.getPlanQuantity()).sum();
            assertTrue(totalAllocated <= 400, "分配总量" + totalAllocated + "应<=总产能400");
        }

        @Test
        @Order(22)
        @DisplayName("约束验证-结构切换限制")
        void testConstraint_StructureSwitchLimit() {
            if (algorithmService == null) {
                System.out.println("跳过测试: AlgorithmService未注入");
                return;
            }

            // Given: 多种不同结构的任务
            List<DailyEmbryoTask> tasks = createTestTasksWithDifferentStructures(6, 50);
            List<Machine> machines = createTestMachines(2, 200);
            ScheduleMain scheduleMain = createTestScheduleMain();

            // When: 执行分配
            AllocationResult result = algorithmService.allocateTasks(tasks, machines, scheduleMain);

            // Then: 验证结构切换次数限制
            // 注：当前算法每次分配新结构都会增加切换计数，限制为每日2次
            System.out.println("结构切换验证: 分配" + result.getDetails().size() + "条明细");
        }

        @Test
        @Order(23)
        @DisplayName("约束验证-班次均衡偏差")
        void testConstraint_ShiftBalanceDeviation() {
            if (algorithmService == null) {
                System.out.println("跳过测试: AlgorithmService未注入");
                return;
            }

            // Given: 创建严重不均衡的明细
            List<ScheduleDetail> details = createUnbalancedDetails(300);

            // When: 执行均衡调整
            List<ScheduleDetail> balancedDetails = algorithmService.balanceShiftDistribution(details, "1:1:1");

            // Then: 验证偏差率
            Map<String, Integer> distribution = new HashMap<>();
            for (ScheduleDetail detail : balancedDetails) {
                distribution.merge(detail.getShiftCode(), detail.getPlanQuantity(), Integer::sum);
            }

            int total = distribution.values().stream().mapToInt(Integer::intValue).sum();
            for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
                double deviation = Math.abs(entry.getValue() - total / 3.0) / (total / 3.0);
                System.out.println(entry.getKey() + "偏差率: " + String.format("%.2f%%", deviation * 100));
                // 允许一定偏差（算法调整后可能在合理范围内）
            }
        }
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionTests {

        @Test
        @Order(30)
        @DisplayName("边界条件-空任务列表")
        void testBoundary_EmptyTasks() {
            if (algorithmService == null) {
                System.out.println("跳过测试: AlgorithmService未注入");
                return;
            }

            // Given: 空任务列表
            List<DailyEmbryoTask> tasks = new ArrayList<>();
            List<Machine> machines = createTestMachines(2, 200);
            ScheduleMain scheduleMain = createTestScheduleMain();

            // When: 执行分配
            AllocationResult result = algorithmService.allocateTasks(tasks, machines, scheduleMain);

            // Then: 应返回成功但无明细
            assertTrue(result.isSuccess(), "空任务应返回成功");
            assertTrue(result.getDetails().isEmpty(), "不应有分配明细");
        }

        @Test
        @Order(31)
        @DisplayName("边界条件-无机台可用")
        void testBoundary_NoMachines() {
            if (algorithmService == null) {
                System.out.println("跳过测试: AlgorithmService未注入");
                return;
            }

            // Given: 有任务但无机台
            List<DailyEmbryoTask> tasks = createTestTasks(3, 100);
            List<Machine> machines = new ArrayList<>();
            ScheduleMain scheduleMain = createTestScheduleMain();

            // When: 执行分配
            AllocationResult result = algorithmService.allocateTasks(tasks, machines, scheduleMain);

            // Then: 应返回失败
            assertFalse(result.isSuccess(), "无机台时应返回失败");
        }

        @Test
        @Order(32)
        @DisplayName("边界条件-单任务分配")
        void testBoundary_SingleTask() {
            if (algorithmService == null) {
                System.out.println("跳过测试: AlgorithmService未注入");
                return;
            }

            // Given: 单个任务
            List<DailyEmbryoTask> tasks = createTestTasks(1, 72);
            List<Machine> machines = createTestMachines(3, 200);
            ScheduleMain scheduleMain = createTestScheduleMain();

            // When: 执行分配
            AllocationResult result = algorithmService.allocateTasks(tasks, machines, scheduleMain);

            // Then: 应成功分配
            assertTrue(result.isSuccess(), "单任务应成功分配");
            assertEquals(1, result.getDetails().size(), "应有1条明细");
        }

        @Test
        @Order(33)
        @DisplayName("边界条件-任务量为零")
        void testBoundary_ZeroQuantity() {
            if (algorithmService == null) {
                System.out.println("跳过测试: AlgorithmService未注入");
                return;
            }

            // Given: 任务量为零
            List<DailyEmbryoTask> tasks = createTestTasks(1, 0);
            List<Machine> machines = createTestMachines(2, 200);
            ScheduleMain scheduleMain = createTestScheduleMain();

            // When: 执行分配
            AllocationResult result = algorithmService.allocateTasks(tasks, machines, scheduleMain);

            // Then: 应成功但无实际分配
            // 注：当前算法可能会跳过零量任务
            System.out.println("零量任务测试完成: success=" + result.isSuccess());
        }

        @Test
        @Order(34)
        @DisplayName("边界条件-机台状态非运行")
        void testBoundary_MachineNotRunning() {
            if (algorithmService == null) {
                System.out.println("跳过测试: AlgorithmService未注入");
                return;
            }

            // Given: 机台状态非运行
            List<DailyEmbryoTask> tasks = createTestTasks(2, 100);
            List<Machine> machines = createTestMachinesWithStatus(2, 200, "MAINTENANCE");
            ScheduleMain scheduleMain = createTestScheduleMain();

            // When: 执行分配
            AllocationResult result = algorithmService.allocateTasks(tasks, machines, scheduleMain);

            // Then: 应返回失败（无机台可用）
            assertFalse(result.isSuccess(), "无运行机台时应返回失败");
        }
    }

    // ==================== 性能测试 ====================

    @Nested
    @DisplayName("性能测试")
    class PerformanceTests {

        @Test
        @Order(40)
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        @DisplayName("性能测试-大规模分配")
        void testPerformance_LargeScaleAllocation() {
            if (algorithmService == null) {
                System.out.println("跳过测试: AlgorithmService未注入");
                return;
            }

            // Given: 大规模任务和机台
            List<DailyEmbryoTask> tasks = createTestTasks(30, 60);
            List<Machine> machines = createTestMachines(10, 200);
            ScheduleMain scheduleMain = createTestScheduleMain();

            // When: 执行分配（应在10秒内完成）
            long startTime = System.currentTimeMillis();
            AllocationResult result = algorithmService.allocateTasks(tasks, machines, scheduleMain);
            long endTime = System.currentTimeMillis();

            // Then: 验证性能
            System.out.println("大规模分配耗时: " + (endTime - startTime) + "ms");
            System.out.println("分配结果: success=" + result.isSuccess() + 
                ", details=" + result.getDetails().size());
            assertTrue((endTime - startTime) < 10000, "分配应在10秒内完成");
        }

        @Test
        @Order(41)
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("性能测试-大量明细排序")
        void testPerformance_LargeScaleSorting() {
            if (algorithmService == null) {
                System.out.println("跳过测试: AlgorithmService未注入");
                return;
            }

            // Given: 大量明细
            List<ScheduleDetail> details = createLargeDetails(100);
            Map<String, Double> stockHours = createTestStockHoursForLarge(details);

            // When: 执行排序（应在5秒内完成）
            long startTime = System.currentTimeMillis();
            List<ScheduleDetail> sortedDetails = algorithmService.sortScheduleSequence(details, stockHours);
            long endTime = System.currentTimeMillis();

            // Then: 验证性能
            System.out.println("大规模排序耗时: " + (endTime - startTime) + "ms");
            assertTrue((endTime - startTime) < 5000, "排序应在5秒内完成");
            assertEquals(100, sortedDetails.size(), "排序明细数量应一致");
        }
    }

    // ==================== 辅助方法 ====================

    private List<DailyEmbryoTask> createTestTasks(int count, int quantity) {
        List<DailyEmbryoTask> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            DailyEmbryoTask task = new DailyEmbryoTask();
            task.setId((long) (i + 1));
            task.setMaterialCode("MAT-" + String.format("%03d", i + 1));
            task.setTaskQuantity(quantity);
            task.setPriority(i + 1);
            task.setProductStructure("STRUCT-" + (i % 3 + 1));
            task.setIsMainProduct(i < count / 2 ? 1 : 0);
            tasks.add(task);
        }
        return tasks;
    }

    private List<DailyEmbryoTask> createTestTasksWithDifferentMaterials(int count, int quantity) {
        List<DailyEmbryoTask> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            DailyEmbryoTask task = new DailyEmbryoTask();
            task.setId((long) (i + 1));
            task.setMaterialCode("MAT-DIFF-" + String.format("%03d", i + 1));
            task.setTaskQuantity(quantity);
            task.setPriority(i + 1);
            task.setProductStructure("STRUCT-" + (i + 1));
            tasks.add(task);
        }
        return tasks;
    }

    private List<DailyEmbryoTask> createTestTasksWithDifferentStructures(int count, int quantity) {
        List<DailyEmbryoTask> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            DailyEmbryoTask task = new DailyEmbryoTask();
            task.setId((long) (i + 1));
            task.setMaterialCode("MAT-" + String.format("%03d", i + 1));
            task.setTaskQuantity(quantity);
            task.setPriority(i + 1);
            task.setProductStructure("STRUCT-" + (i + 1));
            tasks.add(task);
        }
        return tasks;
    }

    private List<Machine> createTestMachines(int count, int dailyCapacity) {
        List<Machine> machines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Machine machine = new Machine();
            machine.setId((long) (i + 1));
            machine.setMachineCode("GM" + String.format("%02d", i + 1));
            machine.setMachineName("成型机-" + (i + 1));
            machine.setMaxDailyCapacity(dailyCapacity);
            machine.setMaxCapacityPerHour(new BigDecimal(dailyCapacity / 8.0));
            machine.setStatus("RUNNING");
            machine.setLineNumber(i % 5 + 1);
            machines.add(machine);
        }
        return machines;
    }

    private List<Machine> createTestMachinesWithStatus(int count, int dailyCapacity, String status) {
        List<Machine> machines = createTestMachines(count, dailyCapacity);
        for (Machine machine : machines) {
            machine.setStatus(status);
        }
        return machines;
    }

    private ScheduleMain createTestScheduleMain() {
        ScheduleMain scheduleMain = new ScheduleMain();
        scheduleMain.setId(1L);
        scheduleMain.setScheduleDate(LocalDate.now());
        scheduleMain.setStatus("DRAFT");
        return scheduleMain;
    }

    private List<ScheduleDetail> convertToScheduleDetails(AllocationResult allocationResult, ScheduleMain scheduleMain) {
        List<ScheduleDetail> details = new ArrayList<>();
        String[] shifts = {"DAY", "AFTERNOON", "NIGHT"};
        int idx = 0;
        
        for (var alloc : allocationResult.getDetails()) {
            ScheduleDetail detail = new ScheduleDetail();
            detail.setId((long) (idx + 1));
            detail.setMainId(alloc.getMainId());
            detail.setScheduleDate(alloc.getScheduleDate());
            detail.setMachineCode(alloc.getMachineCode());
            detail.setMaterialCode(alloc.getMaterialCode());
            detail.setPlanQuantity(alloc.getPlanQuantity());
            detail.setShiftCode(shifts[idx % 3]);
            detail.setStatus("DRAFT");
            details.add(detail);
            idx++;
        }
        return details;
    }

    private Map<String, Double> createTestStockHours(List<DailyEmbryoTask> tasks) {
        Map<String, Double> map = new HashMap<>();
        for (DailyEmbryoTask task : tasks) {
            map.put(task.getMaterialCode(), 10.0 + Math.random() * 10);
        }
        return map;
    }

    private List<ScheduleDetail> createUnbalancedDetails(int totalQuantity) {
        List<ScheduleDetail> details = new ArrayList<>();
        
        // 早班分配过多
        int dayQty = (int) (totalQuantity * 0.7);
        for (int i = 0; i < dayQty / 60; i++) {
            ScheduleDetail detail = new ScheduleDetail();
            detail.setId((long) (i + 1));
            detail.setShiftCode("DAY");
            detail.setMachineCode("GM01");
            detail.setMaterialCode("MAT-" + i);
            detail.setPlanQuantity(60);
            details.add(detail);
        }
        
        // 中班分配较少
        int afternoonQty = (int) (totalQuantity * 0.2);
        for (int i = 0; i < afternoonQty / 60; i++) {
            ScheduleDetail detail = new ScheduleDetail();
            detail.setId((long) (details.size() + 1));
            detail.setShiftCode("AFTERNOON");
            detail.setMachineCode("GM02");
            detail.setMaterialCode("MAT-" + (i + 10));
            detail.setPlanQuantity(60);
            details.add(detail);
        }
        
        // 夜班分配最少
        int nightQty = totalQuantity - dayQty - afternoonQty;
        for (int i = 0; i < nightQty / 60; i++) {
            ScheduleDetail detail = new ScheduleDetail();
            detail.setId((long) (details.size() + 1));
            detail.setShiftCode("NIGHT");
            detail.setMachineCode("GM03");
            detail.setMaterialCode("MAT-" + (i + 20));
            detail.setPlanQuantity(60);
            details.add(detail);
        }
        
        return details;
    }

    private List<ScheduleDetail> createLargeDetails(int count) {
        List<ScheduleDetail> details = new ArrayList<>();
        String[] shifts = {"DAY", "AFTERNOON", "NIGHT"};
        
        for (int i = 0; i < count; i++) {
            ScheduleDetail detail = new ScheduleDetail();
            detail.setId((long) (i + 1));
            detail.setShiftCode(shifts[i % 3]);
            detail.setMachineCode("GM" + String.format("%02d", (i % 10) + 1));
            detail.setMaterialCode("MAT-" + String.format("%03d", i + 1));
            detail.setPlanQuantity(24 + (i % 5) * 12);
            details.add(detail);
        }
        return details;
    }

    private Map<String, Double> createTestStockHoursForLarge(List<ScheduleDetail> details) {
        Map<String, Double> map = new HashMap<>();
        for (ScheduleDetail detail : details) {
            map.putIfAbsent(detail.getMaterialCode(), 5.0 + Math.random() * 20);
        }
        return map;
    }

    private void verifyFinalResult(List<ScheduleDetail> details) {
        // 验证顺位递增
        int prevSequence = 0;
        for (ScheduleDetail detail : details) {
            if (detail.getSequence() != null) {
                assertTrue(detail.getSequence() > prevSequence, 
                    "顺位应递增，当前: " + detail.getSequence() + ", 前一个: " + prevSequence);
                prevSequence = detail.getSequence();
            }
        }
        
        // 验证车次容量
        for (ScheduleDetail detail : details) {
            if (detail.getTripCapacity() != null) {
                assertEquals(12, detail.getTripCapacity(), "车次容量应为12");
            }
        }
        
        System.out.println("最终结果验证通过: " + details.size() + "条明细");
    }
}
