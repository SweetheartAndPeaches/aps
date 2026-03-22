package com.jinyu.aps.service;

import com.jinyu.aps.entity.*;
import com.jinyu.aps.service.AlgorithmService.AllocationResult;
import com.jinyu.aps.service.AlgorithmServiceOptimized;
import com.jinyu.aps.service.AlgorithmServiceOptimized.AllocationDetail;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 性能压力测试
 * 
 * 测试目标：
 * 1. 大规模任务分配性能
 * 2. 缓存策略效果验证
 * 3. 递归深度优化效果
 * 4. 原版vs优化版对比
 * 
 * @author APS Team
 */
@DisplayName("性能压力测试")
public class PerformanceTest {

    private AlgorithmService originalService;
    private AlgorithmServiceOptimized optimizedService;

    @BeforeEach
    void setUp() {
        originalService = new AlgorithmService();
        optimizedService = new AlgorithmServiceOptimized();
    }

    // ==================== 规模测试 ====================

    @Nested
    @DisplayName("规模测试")
    public class ScaleTests {

        @Test
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        @DisplayName("小规模-10任务5机台")
        void testSmallScale() {
            List<DailyEmbryoTask> tasks = createTasks(10, 60);
            List<Machine> machines = createMachines(5, 200);
            ScheduleMain scheduleMain = createScheduleMain();

            // 原版测试
            long originalStart = System.currentTimeMillis();
            AllocationResult originalResult = originalService.allocateTasks(tasks, machines, scheduleMain);
            long originalTime = System.currentTimeMillis() - originalStart;

            // 优化版测试
            long optimizedStart = System.currentTimeMillis();
            AlgorithmServiceOptimized.AllocationResult optimizedResult = optimizedService.allocateTasks(tasks, machines, scheduleMain);
            long optimizedTime = System.currentTimeMillis() - optimizedStart;

            printComparison("小规模测试", originalTime, optimizedTime, 
                originalResult.isSuccess(), optimizedResult.isSuccess());
        }

        @Test
        @Timeout(value = 60, unit = TimeUnit.SECONDS)
        @DisplayName("中规模-30任务10机台")
        void testMediumScale() {
            List<DailyEmbryoTask> tasks = createTasks(30, 60);
            List<Machine> machines = createMachines(10, 200);
            ScheduleMain scheduleMain = createScheduleMain();

            // 原版测试
            long originalStart = System.currentTimeMillis();
            AllocationResult originalResult = originalService.allocateTasks(tasks, machines, scheduleMain);
            long originalTime = System.currentTimeMillis() - originalStart;

            // 优化版测试
            long optimizedStart = System.currentTimeMillis();
            AlgorithmServiceOptimized.AllocationResult optimizedResult = optimizedService.allocateTasks(tasks, machines, scheduleMain);
            long optimizedTime = System.currentTimeMillis() - optimizedStart;

            printComparison("中规模测试", originalTime, optimizedTime,
                originalResult.isSuccess(), optimizedResult.isSuccess());
        }

        @Test
        @Timeout(value = 120, unit = TimeUnit.SECONDS)
        @DisplayName("大规模-50任务15机台")
        void testLargeScale() {
            List<DailyEmbryoTask> tasks = createTasks(50, 48);
            List<Machine> machines = createMachines(15, 200);
            ScheduleMain scheduleMain = createScheduleMain();

            // 优化版测试（原版可能超时，仅测试优化版）
            long optimizedStart = System.currentTimeMillis();
            AlgorithmServiceOptimized.AllocationResult optimizedResult = optimizedService.allocateTasks(tasks, machines, scheduleMain);
            long optimizedTime = System.currentTimeMillis() - optimizedStart;

            System.out.println("=== 大规模测试结果 ===");
            System.out.println("优化版耗时: " + optimizedTime + "ms");
            System.out.println("分配成功: " + optimizedResult.isSuccess());
            System.out.println("分配明细数: " + optimizedResult.getDetails().size());
            
            assertTrue(optimizedTime < 30000, "大规模分配应在30秒内完成");
        }
    }

    // ==================== 压力测试 ====================

    @Nested
    @DisplayName("压力测试")
    public class StressTests {

        @Test
        @Timeout(value = 60, unit = TimeUnit.SECONDS)
        @DisplayName("压力测试-高约束条件")
        void testStress_HighConstraints() {
            // 高约束条件：多SKU、少机台
            List<DailyEmbryoTask> tasks = createTasksWithDifferentMaterials(20, 50);
            List<Machine> machines = createMachines(3, 300);
            ScheduleMain scheduleMain = createScheduleMain();

            long start = System.currentTimeMillis();
            AlgorithmServiceOptimized.AllocationResult result = optimizedService.allocateTasks(tasks, machines, scheduleMain);
            long time = System.currentTimeMillis() - start;

            System.out.println("=== 高约束压力测试 ===");
            System.out.println("耗时: " + time + "ms");
            System.out.println("成功: " + result.isSuccess());
            System.out.println("分配明细数: " + result.getDetails().size());

            // 验证约束
            if (result.isSuccess()) {
                Map<String, Set<String>> machineMaterials = new HashMap<>();
                for (AllocationDetail detail : result.getDetails()) {
                    machineMaterials.computeIfAbsent(detail.getMachineCode(), k -> new HashSet<>())
                        .add(detail.getMaterialCode());
                }
                
                for (Map.Entry<String, Set<String>> entry : machineMaterials.entrySet()) {
                    assertTrue(entry.getValue().size() <= 4, 
                        "机台" + entry.getKey() + "SKU种类应<=4，实际: " + entry.getValue().size());
                }
            }
        }

        @Test
        @Timeout(value = 60, unit = TimeUnit.SECONDS)
        @DisplayName("压力测试-产能瓶颈")
        void testStress_CapacityBottleneck() {
            // 产能瓶颈：任务总量远超机台产能
            List<DailyEmbryoTask> tasks = createTasks(20, 150); // 总量3000
            List<Machine> machines = createMachines(5, 100); // 总产能500
            ScheduleMain scheduleMain = createScheduleMain();

            long start = System.currentTimeMillis();
            AlgorithmServiceOptimized.AllocationResult result = optimizedService.allocateTasks(tasks, machines, scheduleMain);
            long time = System.currentTimeMillis() - start;

            System.out.println("=== 产能瓶颈压力测试 ===");
            System.out.println("耗时: " + time + "ms");
            System.out.println("成功: " + result.isSuccess());
            
            int totalAllocated = result.getDetails().stream()
                .mapToInt(d -> d.getPlanQuantity()).sum();
            System.out.println("实际分配量: " + totalAllocated);
            
            assertTrue(totalAllocated <= 500, "分配量不应超过总产能");
        }

        @Test
        @Timeout(value = 120, unit = TimeUnit.SECONDS)
        @DisplayName("压力测试-多次迭代稳定性")
        void testStress_MultipleIterations() {
            List<Long> times = new ArrayList<>();
            int iterations = 10;

            for (int i = 0; i < iterations; i++) {
                List<DailyEmbryoTask> tasks = createTasks(20, 60);
                List<Machine> machines = createMachines(8, 200);
                ScheduleMain scheduleMain = createScheduleMain();

                long start = System.currentTimeMillis();
                AlgorithmServiceOptimized.AllocationResult result = optimizedService.allocateTasks(tasks, machines, scheduleMain);
                long time = System.currentTimeMillis() - start;
                times.add(time);

                assertTrue(result.isSuccess(), "迭代" + (i + 1) + "应成功");
            }

            // 分析性能稳定性
            long avgTime = (long) times.stream().mapToLong(Long::longValue).average().orElse(0);
            long maxTime = times.stream().mapToLong(Long::longValue).max().orElse(0);
            long minTime = times.stream().mapToLong(Long::longValue).min().orElse(0);

            System.out.println("=== 多次迭代稳定性测试 ===");
            System.out.println("迭代次数: " + iterations);
            System.out.println("平均耗时: " + avgTime + "ms");
            System.out.println("最大耗时: " + maxTime + "ms");
            System.out.println("最小耗时: " + minTime + "ms");
            System.out.println("耗时波动: " + ((maxTime - minTime) * 100.0 / avgTime) + "%");

            // 验证稳定性：最大耗时不应超过平均耗时的2倍
            assertTrue(maxTime < avgTime * 2, "性能应稳定，最大耗时不应超过平均耗时的2倍");
        }
    }

    // ==================== 班次均衡性能测试 ====================

    @Nested
    @DisplayName("班次均衡性能测试")
    public class ShiftBalancePerformanceTests {

        @Test
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        @DisplayName("班次均衡-大量明细")
        void testShiftBalance_LargeDetails() {
            List<ScheduleDetail> details = createLargeDetails(500);
            String shiftRatio = "1:2:1";

            long start = System.currentTimeMillis();
            List<ScheduleDetail> result = optimizedService.balanceShiftDistribution(details, shiftRatio);
            long time = System.currentTimeMillis() - start;

            System.out.println("=== 班次均衡大量明细测试 ===");
            System.out.println("明细数: " + details.size());
            System.out.println("耗时: " + time + "ms");
            
            assertTrue(time < 5000, "班次均衡应在5秒内完成");
            assertNotNull(result, "结果不应为空");
        }

        @Test
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        @DisplayName("班次均衡-严重不均衡场景")
        void testShiftBalance_SeverelyUnbalanced() {
            List<ScheduleDetail> details = createSeverelyUnbalancedDetails(600);
            String shiftRatio = "1:1:1";

            long start = System.currentTimeMillis();
            List<ScheduleDetail> result = optimizedService.balanceShiftDistribution(details, shiftRatio);
            long time = System.currentTimeMillis() - start;

            System.out.println("=== 班次均衡严重不均衡测试 ===");
            System.out.println("耗时: " + time + "ms");
            
            // 验证均衡效果
            Map<String, Integer> distribution = new HashMap<>();
            for (ScheduleDetail detail : result) {
                distribution.merge(detail.getShiftCode(), detail.getPlanQuantity(), Integer::sum);
            }
            
            int total = distribution.values().stream().mapToInt(Integer::intValue).sum();
            for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
                double ratio = entry.getValue() / (double) total;
                System.out.println(entry.getKey() + "占比: " + String.format("%.2f%%", ratio * 100));
            }
        }
    }

    // ==================== 顺位排序性能测试 ====================

    @Nested
    @DisplayName("顺位排序性能测试")
    public class SequenceSortPerformanceTests {

        @Test
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        @DisplayName("顺位排序-大量明细")
        void testSequenceSort_LargeDetails() {
            List<ScheduleDetail> details = createLargeDetails(500);
            Map<String, Double> stockHours = createStockHours(details);

            long start = System.currentTimeMillis();
            List<ScheduleDetail> result = optimizedService.sortScheduleSequence(details, stockHours);
            long time = System.currentTimeMillis() - start;

            System.out.println("=== 顺位排序大量明细测试 ===");
            System.out.println("明细数: " + details.size());
            System.out.println("耗时: " + time + "ms");
            
            assertTrue(time < 5000, "顺位排序应在5秒内完成");
            assertEquals(500, result.size(), "明细数量应一致");
        }

        @Test
        @Timeout(value = 60, unit = TimeUnit.SECONDS)
        @DisplayName("顺位排序-超大规模")
        void testSequenceSort_VeryLargeScale() {
            List<ScheduleDetail> details = createLargeDetails(1000);
            Map<String, Double> stockHours = createStockHours(details);

            long start = System.currentTimeMillis();
            List<ScheduleDetail> result = optimizedService.sortScheduleSequence(details, stockHours);
            long time = System.currentTimeMillis() - start;

            System.out.println("=== 顺位排序超大规模测试 ===");
            System.out.println("明细数: " + details.size());
            System.out.println("耗时: " + time + "ms");
            
            assertTrue(time < 10000, "顺位排序应在10秒内完成");
        }
    }

    // ==================== 缓存效果测试 ====================

    @Nested
    @DisplayName("缓存效果测试")
    public class CacheEffectTests {

        @Test
        @DisplayName("缓存效果-重复分配场景")
        void testCacheEffect_RepeatedAllocation() {
            List<DailyEmbryoTask> tasks = createTasks(10, 60);
            List<Machine> machines = createMachines(5, 200);
            ScheduleMain scheduleMain = createScheduleMain();

            // 第一次分配（冷启动）
            long coldStart = System.currentTimeMillis();
            AlgorithmServiceOptimized.AllocationResult result1 = optimizedService.allocateTasks(tasks, machines, scheduleMain);
            long coldTime = System.currentTimeMillis() - coldStart;

            // 第二次分配（热启动，相同数据）
            long hotStart = System.currentTimeMillis();
            AlgorithmServiceOptimized.AllocationResult result2 = optimizedService.allocateTasks(tasks, machines, scheduleMain);
            long hotTime = System.currentTimeMillis() - hotStart;

            System.out.println("=== 缓存效果测试 ===");
            System.out.println("冷启动耗时: " + coldTime + "ms");
            System.out.println("热启动耗时: " + hotTime + "ms");
            System.out.println("性能提升: " + ((coldTime - hotTime) * 100.0 / coldTime) + "%");
        }
    }

    // ==================== 辅助方法 ====================

    private List<DailyEmbryoTask> createTasks(int count, int quantity) {
        List<DailyEmbryoTask> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            DailyEmbryoTask task = new DailyEmbryoTask();
            task.setId((long) (i + 1));
            task.setMaterialCode("MAT-" + String.format("%03d", i + 1));
            task.setTaskQuantity(quantity);
            task.setPriority(i + 1);
            task.setProductStructure("STRUCT-" + (i % 5 + 1));
            task.setIsMainProduct(i < count / 2 ? 1 : 0);
            tasks.add(task);
        }
        return tasks;
    }

    private List<DailyEmbryoTask> createTasksWithDifferentMaterials(int count, int quantity) {
        List<DailyEmbryoTask> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            DailyEmbryoTask task = new DailyEmbryoTask();
            task.setId((long) (i + 1));
            task.setMaterialCode("MAT-DIFF-" + String.format("%03d", i + 1));
            task.setTaskQuantity(quantity);
            task.setPriority(i + 1);
            task.setProductStructure("STRUCT-" + (i % 3 + 1));
            tasks.add(task);
        }
        return tasks;
    }

    private List<Machine> createMachines(int count, int dailyCapacity) {
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

    private ScheduleMain createScheduleMain() {
        ScheduleMain scheduleMain = new ScheduleMain();
        scheduleMain.setId(1L);
        scheduleMain.setScheduleDate(LocalDate.now());
        scheduleMain.setStatus("DRAFT");
        return scheduleMain;
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

    private List<ScheduleDetail> createSeverelyUnbalancedDetails(int totalQuantity) {
        List<ScheduleDetail> details = new ArrayList<>();
        
        int dayQty = (int) (totalQuantity * 0.8);
        for (int i = 0; i < dayQty / 60; i++) {
            ScheduleDetail detail = new ScheduleDetail();
            detail.setId((long) (i + 1));
            detail.setShiftCode("DAY");
            detail.setMachineCode("GM01");
            detail.setMaterialCode("MAT-" + i);
            detail.setPlanQuantity(60);
            details.add(detail);
        }
        
        int afternoonQty = (int) (totalQuantity * 0.15);
        for (int i = 0; i < afternoonQty / 60; i++) {
            ScheduleDetail detail = new ScheduleDetail();
            detail.setId((long) (details.size() + 1));
            detail.setShiftCode("AFTERNOON");
            detail.setMachineCode("GM02");
            detail.setMaterialCode("MAT-" + (i + 100));
            detail.setPlanQuantity(60);
            details.add(detail);
        }
        
        int nightQty = totalQuantity - dayQty - afternoonQty;
        for (int i = 0; i < nightQty / 60; i++) {
            ScheduleDetail detail = new ScheduleDetail();
            detail.setId((long) (details.size() + 1));
            detail.setShiftCode("NIGHT");
            detail.setMachineCode("GM03");
            detail.setMaterialCode("MAT-" + (i + 200));
            detail.setPlanQuantity(60);
            details.add(detail);
        }
        
        return details;
    }

    private Map<String, Double> createStockHours(List<ScheduleDetail> details) {
        Map<String, Double> map = new HashMap<>();
        for (ScheduleDetail detail : details) {
            map.putIfAbsent(detail.getMaterialCode(), 5.0 + Math.random() * 20);
        }
        return map;
    }

    private void printComparison(String testName, long originalTime, long optimizedTime,
                                  boolean originalSuccess, boolean optimizedSuccess) {
        System.out.println("=== " + testName + " 对比结果 ===");
        System.out.println("原版耗时: " + originalTime + "ms");
        System.out.println("优化版耗时: " + optimizedTime + "ms");
        System.out.println("性能提升: " + ((originalTime - optimizedTime) * 100.0 / originalTime) + "%");
        System.out.println("原版成功: " + originalSuccess);
        System.out.println("优化版成功: " + optimizedSuccess);
    }
}
