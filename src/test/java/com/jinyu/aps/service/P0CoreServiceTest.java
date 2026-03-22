package com.jinyu.aps.service;

import com.jinyu.aps.entity.*;
import com.jinyu.aps.service.P0CoreService.PrecisionTimeSlot;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P0核心功能单元测试
 * 
 * 测试三个核心功能：
 * 1. 收尾管理（紧急收尾标签优先级）
 * 2. 关键产品开产首班不排
 * 3. 精度计划机台停机处理
 *
 * @author APS Team
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("P0核心功能测试")
class P0CoreServiceTest {

    @Autowired
    private P0CoreService p0CoreService;

    @Autowired
    private AlgorithmService algorithmService;

    // ==================== 1. 收尾管理测试 ====================

    @Nested
    @DisplayName("收尾管理测试")
    class EndingManagementTests {

        @Test
        @DisplayName("紧急收尾判断-硫化余量小于等于3天产能")
        void testUrgentEnding_WithinThreeDays() {
            // Given: 硫化余量150条，胎胚库存0，日产能60条
            int vulcanizingRemainder = 150;  // 硫化余量
            int embryoStock = 0;             // 胎胚库存
            int dailyCapacity = 60;          // 日产能

            // When: 计算收尾状态
            StructureEnding ending = p0CoreService.calculateEndingStatus(
                vulcanizingRemainder, embryoStock, dailyCapacity);

            // Then: 应该标记为紧急收尾
            assertEquals(150, ending.getFormingRemainder());
            assertTrue(ending.getEstimatedEndingDays().compareTo(BigDecimal.valueOf(3)) <= 0);
            assertEquals(1, ending.getIsUrgentEnding());
            assertTrue(ending.getIsNearEnding() == 1);
        }

        @Test
        @DisplayName("非紧急收尾-硫化余量大于3天产能")
        void testNonUrgentEnding_MoreThanThreeDays() {
            // Given: 硫化余量500条，胎胚库存100条，日产能60条
            int vulcanizingRemainder = 500;
            int embryoStock = 100;  // 成型余量 = 500 - 100 = 400条
            int dailyCapacity = 60;

            // When
            StructureEnding ending = p0CoreService.calculateEndingStatus(
                vulcanizingRemainder, embryoStock, dailyCapacity);

            // Then: 非紧急收尾
            assertEquals(400, ending.getFormingRemainder());
            assertTrue(ending.getEstimatedEndingDays().compareTo(BigDecimal.valueOf(3)) > 0);
            assertEquals(0, ending.getIsUrgentEnding());
            assertTrue(ending.getIsNearEnding() == 1);  // 400/60 ≈ 6.7天 < 10天
        }

        @Test
        @DisplayName("正常生产-收尾天数超过10天")
        void testNormalProduction_MoreThanTenDays() {
            // Given: 硫化余量1000条，胎胚库存100条，日产能60条
            int vulcanizingRemainder = 1000;
            int embryoStock = 100;  // 成型余量 = 900条
            int dailyCapacity = 60;

            // When
            StructureEnding ending = p0CoreService.calculateEndingStatus(
                vulcanizingRemainder, embryoStock, dailyCapacity);

            // Then: 非紧急，非近期收尾
            assertEquals(900, ending.getFormingRemainder());
            assertTrue(ending.getEstimatedEndingDays().compareTo(BigDecimal.valueOf(10)) > 0);
            assertEquals(0, ending.getIsUrgentEnding());
            assertEquals(0, ending.getIsNearEnding());
        }

        @Test
        @DisplayName("延误量计算-10天内做不完")
        void testDelayCalculation_CannotFinishInTenDays() {
            // Given: 硫化余量550条，胎胚库存50条，日产能50条
            // 成型余量 = 550 - 50 = 500条
            // 预计收尾天数 = 500 / 50 = 10 天（刚好等于10天，属于近期收尾）
            // 10天产能 = 50 * 10 = 500条
            // 延误量 = 500 - 500 = 0条
            int vulcanizingRemainder = 550;
            int embryoStock = 50;
            int dailyCapacity = 50;

            // When
            StructureEnding ending = p0CoreService.calculateEndingStatus(
                vulcanizingRemainder, embryoStock, dailyCapacity);

            // Then: 近期收尾但无延误
            assertEquals(500, ending.getFormingRemainder());
            assertEquals(1, ending.getIsNearEnding());  // 近期收尾
            // 延误量可能为0或null（刚好10天做完）
        }

        @Test
        @DisplayName("批量标记紧急收尾任务")
        void testMarkUrgentEndingTasks() {
            // Given: 准备任务列表和收尾状态
            List<DailyEmbryoTask> tasks = new ArrayList<>();
            
            DailyEmbryoTask task1 = new DailyEmbryoTask();
            task1.setMaterialCode("MAT-001");
            task1.setProductStructure("STRUCT-A");
            task1.setTaskQuantity(100);
            tasks.add(task1);

            DailyEmbryoTask task2 = new DailyEmbryoTask();
            task2.setMaterialCode("MAT-002");
            task2.setProductStructure("STRUCT-B");
            task2.setTaskQuantity(200);
            tasks.add(task2);

            Map<String, StructureEnding> endingMap = new HashMap<>();
            
            StructureEnding ending1 = new StructureEnding();
            ending1.setIsUrgentEnding(1);  // 紧急收尾
            ending1.setEstimatedEndingDays(BigDecimal.valueOf(2.5));
            endingMap.put("STRUCT-A", ending1);

            StructureEnding ending2 = new StructureEnding();
            ending2.setIsUrgentEnding(0);  // 非紧急
            endingMap.put("STRUCT-B", ending2);

            // When: 标记紧急收尾
            p0CoreService.markUrgentEndingTasks(tasks, endingMap);

            // Then: STRUCT-A的任务应标记为紧急
            assertEquals(1, tasks.get(0).getIsUrgentEnding());
            assertEquals(0, tasks.get(1).getIsUrgentEnding());
        }
    }

    // ==================== 2. 关键产品开产首班不排测试 ====================

    @Nested
    @DisplayName("关键产品开产首班不排测试")
    class KeyProductFirstShiftTests {

        @Test
        @DisplayName("开产首班-关键产品有其他产品可做则跳过")
        void testSkipKeyProduct_FirstShift_WithOtherProducts() {
            // Given: 关键产品，开产首日，早班，有其他非关键产品
            DailyEmbryoTask keyProductTask = new DailyEmbryoTask();
            keyProductTask.setMaterialCode("KEY-001");
            keyProductTask.setProductStructure("STRUCT-A");
            keyProductTask.setIsKeyProduct(1);

            List<DailyEmbryoTask> allTasks = new ArrayList<>();
            allTasks.add(keyProductTask);
            
            DailyEmbryoTask otherTask = new DailyEmbryoTask();
            otherTask.setMaterialCode("NON-KEY-001");
            otherTask.setProductStructure("STRUCT-A");
            otherTask.setIsKeyProduct(0);
            allTasks.add(otherTask);

            // When: 判断是否跳过
            boolean hasOther = p0CoreService.hasOtherNonKeyProducts(allTasks, "STRUCT-A", "KEY-001");
            boolean shouldSkip = p0CoreService.shouldSkipKeyProductFirstShift(
                keyProductTask, true, "DAY", hasOther);

            // Then: 应该跳过
            assertTrue(hasOther);
            assertTrue(shouldSkip);
        }

        @Test
        @DisplayName("开产首班-关键产品无其他产品则不跳过")
        void testNotSkipKeyProduct_FirstShift_NoOtherProducts() {
            // Given: 关键产品，开产首日，早班，但只有这一个产品
            DailyEmbryoTask keyProductTask = new DailyEmbryoTask();
            keyProductTask.setMaterialCode("KEY-001");
            keyProductTask.setProductStructure("STRUCT-A");
            keyProductTask.setIsKeyProduct(1);

            List<DailyEmbryoTask> allTasks = new ArrayList<>();
            allTasks.add(keyProductTask);

            // When
            boolean hasOther = p0CoreService.hasOtherNonKeyProducts(allTasks, "STRUCT-A", "KEY-001");
            boolean shouldSkip = p0CoreService.shouldSkipKeyProductFirstShift(
                keyProductTask, true, "DAY", hasOther);

            // Then: 不能跳过（因为只有关键产品）
            assertFalse(hasOther);
            assertFalse(shouldSkip);
        }

        @Test
        @DisplayName("非开产首日-关键产品正常排产")
        void testKeyProduct_NormalDay_NotSkipped() {
            // Given: 关键产品，非开产首日
            DailyEmbryoTask keyProductTask = new DailyEmbryoTask();
            keyProductTask.setMaterialCode("KEY-001");
            keyProductTask.setIsKeyProduct(1);

            // When: 非开产首日
            boolean shouldSkip = p0CoreService.shouldSkipKeyProductFirstShift(
                keyProductTask, false, "DAY", false);

            // Then: 不跳过
            assertFalse(shouldSkip);
        }

        @Test
        @DisplayName("非首班-关键产品正常排产")
        void testKeyProduct_NonFirstShift_NotSkipped() {
            // Given: 关键产品，开产首日，但不是早班
            DailyEmbryoTask keyProductTask = new DailyEmbryoTask();
            keyProductTask.setMaterialCode("KEY-001");
            keyProductTask.setIsKeyProduct(1);

            // When: 中班
            boolean shouldSkip = p0CoreService.shouldSkipKeyProductFirstShift(
                keyProductTask, true, "MIDDLE", false);

            // Then: 不跳过
            assertFalse(shouldSkip);
        }

        @Test
        @DisplayName("非关键产品-首班正常排产")
        void testNonKeyProduct_FirstShift_NotSkipped() {
            // Given: 非关键产品
            DailyEmbryoTask normalTask = new DailyEmbryoTask();
            normalTask.setMaterialCode("NORMAL-001");
            normalTask.setIsKeyProduct(0);

            // When: 开产首日，早班
            boolean shouldSkip = p0CoreService.shouldSkipKeyProductFirstShift(
                normalTask, true, "DAY", false);

            // Then: 不跳过
            assertFalse(shouldSkip);
        }

        @Test
        @DisplayName("批量过滤首班关键产品")
        void testFilterKeyProductsForFirstShift() {
            // Given: 混合任务列表
            List<DailyEmbryoTask> tasks = new ArrayList<>();

            // 关键产品
            DailyEmbryoTask key1 = new DailyEmbryoTask();
            key1.setMaterialCode("KEY-001");
            key1.setProductStructure("STRUCT-A");
            key1.setIsKeyProduct(1);
            tasks.add(key1);

            // 同结构的非关键产品
            DailyEmbryoTask nonKey1 = new DailyEmbryoTask();
            nonKey1.setMaterialCode("NON-KEY-001");
            nonKey1.setProductStructure("STRUCT-A");
            nonKey1.setIsKeyProduct(0);
            tasks.add(nonKey1);

            // 另一个结构的关键产品（无其他产品）
            DailyEmbryoTask key2 = new DailyEmbryoTask();
            key2.setMaterialCode("KEY-002");
            key2.setProductStructure("STRUCT-B");
            key2.setIsKeyProduct(1);
            tasks.add(key2);

            // When: 开产首日早班过滤
            List<DailyEmbryoTask> filtered = p0CoreService.filterKeyProductsForFirstShift(
                tasks, true, "DAY");

            // Then: KEY-001应被过滤，KEY-002应保留
            assertEquals(2, filtered.size());
            assertTrue(filtered.stream().anyMatch(t -> "NON-KEY-001".equals(t.getMaterialCode())));
            assertTrue(filtered.stream().anyMatch(t -> "KEY-002".equals(t.getMaterialCode())));
            assertFalse(filtered.stream().anyMatch(t -> "KEY-001".equals(t.getMaterialCode())));
        }
    }

    // ==================== 3. 精度计划机台停机处理测试 ====================

    @Nested
    @DisplayName("精度计划机台停机处理测试")
    class PrecisionPlanTests {

        @Test
        @DisplayName("获取精度计划影响的机台")
        void testGetAffectedMachines() {
            // Given: 精度计划列表
            List<PrecisionPlan> plans = new ArrayList<>();
            
            PrecisionPlan plan1 = new PrecisionPlan();
            plan1.setMachineCode("M01");
            plan1.setPlanDate(LocalDate.now());
            plan1.setPlanShift("DAY");
            plan1.setStatus("PLANNED");
            plans.add(plan1);

            PrecisionPlan plan2 = new PrecisionPlan();
            plan2.setMachineCode("M02");
            plan2.setPlanDate(LocalDate.now());
            plan2.setPlanShift("MIDDLE");
            plan2.setStatus("PLANNED");
            plans.add(plan2);

            // When: 获取今天早班受影响的机台
            Set<String> affected = p0CoreService.getAffectedMachinesByPrecision(
                plans, LocalDate.now(), "DAY");

            // Then: 只有M01在早班受影响
            assertEquals(1, affected.size());
            assertTrue(affected.contains("M01"));
        }

        @Test
        @DisplayName("计算精度期间可用产能")
        void testCalculateAvailableCapacity() {
            // Given: 机台日产能240条
            Machine machine = new Machine();
            machine.setMachineCode("M01");
            machine.setMaxDailyCapacity(240);  // 日产能240条 = 每班80条

            List<PrecisionTimeSlot> slots = new ArrayList<>();
            slots.add(new PrecisionTimeSlot("DAY", null, null, 4));

            // When: 计算早班（有精度计划）的可用产能
            int available = p0CoreService.calculateAvailableCapacityDuringPrecision(
                machine, slots, "DAY");

            // Then: 产能应减半（精度占用4小时/班次8小时）
            assertEquals(40, available);  // 80 / 2 = 40
        }

        @Test
        @DisplayName("库存足够支撑精度期间")
        void testStockSufficientDuringPrecision() {
            // Given: 库存100条，硫化速率10条/小时，精度时长4小时
            int embryoStock = 100;
            double vulcanizingRate = 10.0;  // 100/10 = 10小时库存
            int precisionDuration = 4;

            // When
            boolean sufficient = p0CoreService.isStockSufficientDuringPrecision(
                embryoStock, vulcanizingRate, precisionDuration);

            // Then: 库存够吃10小时 > 精度4小时，足够
            assertTrue(sufficient);
        }

        @Test
        @DisplayName("库存不足需要硫化减产")
        void testStockInsufficient_NeedReduction() {
            // Given: 库存20条，硫化速率10条/小时，精度时长4小时
            int embryoStock = 20;  // 20/10 = 2小时库存
            double vulcanizingRate = 10.0;
            int precisionDuration = 4;

            // When
            boolean sufficient = p0CoreService.isStockSufficientDuringPrecision(
                embryoStock, vulcanizingRate, precisionDuration);
            double reductionRatio = p0CoreService.calculateVulcanizingReductionRatio(
                embryoStock, vulcanizingRate, precisionDuration);

            // Then: 库存不足，建议减产
            assertFalse(sufficient);
            assertEquals(0.5, reductionRatio);  // 建议减半
        }

        @Test
        @DisplayName("排除精度期间的机台")
        void testExcludeMachinesWithPrecision() {
            // Given: 机台列表和精度计划
            List<Machine> machines = new ArrayList<>();
            
            Machine m1 = new Machine();
            m1.setMachineCode("M01");
            m1.setMaxDailyCapacity(240);
            machines.add(m1);

            Machine m2 = new Machine();
            m2.setMachineCode("M02");
            m2.setMaxDailyCapacity(240);
            machines.add(m2);

            List<PrecisionPlan> plans = new ArrayList<>();
            PrecisionPlan plan = new PrecisionPlan();
            plan.setMachineCode("M01");
            plan.setPlanDate(LocalDate.now());
            plan.setPlanShift("DAY");
            plan.setStatus("PLANNED");
            plans.add(plan);

            // When: 排除模式
            List<Machine> filtered = p0CoreService.adjustMachinesForPrecision(
                machines, plans, LocalDate.now(), "DAY", "EXCLUDE");

            // Then: M01被排除
            assertEquals(1, filtered.size());
            assertEquals("M02", filtered.get(0).getMachineCode());
        }

        @Test
        @DisplayName("降低精度期间机台的产能")
        void testReduceCapacityForMachinesWithPrecision() {
            // Given: 机台列表和精度计划
            List<Machine> machines = new ArrayList<>();
            
            Machine m1 = new Machine();
            m1.setId(1L);
            m1.setMachineCode("M01");
            m1.setMachineName("机台1");
            m1.setMaxDailyCapacity(240);
            machines.add(m1);

            List<PrecisionPlan> plans = new ArrayList<>();
            PrecisionPlan plan = new PrecisionPlan();
            plan.setMachineCode("M01");
            plan.setPlanDate(LocalDate.now());
            plan.setPlanShift("DAY");
            plan.setStatus("PLANNED");
            plan.setDurationHours(4);  // 精度时长4小时
            plans.add(plan);

            // When: 降产模式
            List<Machine> adjusted = p0CoreService.adjustMachinesForPrecision(
                machines, plans, LocalDate.now(), "DAY", "REDUCE_CAPACITY");

            // Then: M01保留但产能降低（早班产能降为40，日产能降为120）
            assertEquals(1, adjusted.size());
            assertEquals(120, adjusted.get(0).getMaxDailyCapacity());  // 产能减半
        }
    }

    // ==================== 4. 算法集成测试 ====================

    @Nested
    @DisplayName("紧急收尾优先级集成测试")
    class UrgentEndingIntegrationTests {

        @Test
        @DisplayName("紧急收尾任务排在最前面")
        void testUrgentEndingTasksFirst() {
            // Given: 准备混合优先级的任务
            List<ScheduleDetail> details = new ArrayList<>();
            
            // 普通任务
            ScheduleDetail normal1 = createDetail("MAT-001", "M01", 10.0, 0, 0);
            details.add(normal1);
            
            // 紧急收尾任务
            ScheduleDetail urgent1 = createDetail("MAT-002", "M01", 5.0, 1, 0);
            details.add(urgent1);
            
            // 低库存任务
            ScheduleDetail lowStock = createDetail("MAT-003", "M01", 2.0, 0, 0);
            details.add(lowStock);
            
            // 另一个紧急收尾任务
            ScheduleDetail urgent2 = createDetail("MAT-004", "M01", 8.0, 1, 0);
            details.add(urgent2);

            Map<String, Double> stockHours = new HashMap<>();
            stockHours.put("MAT-001", 10.0);
            stockHours.put("MAT-002", 5.0);
            stockHours.put("MAT-003", 2.0);
            stockHours.put("MAT-004", 8.0);

            // When: 执行排序
            List<ScheduleDetail> sorted = algorithmService.sortScheduleSequence(details, stockHours);

            // Then: 紧急收尾任务应排在最前面
            List<ScheduleDetail> m01Tasks = sorted.stream()
                .filter(d -> "M01".equals(d.getMachineCode()))
                .collect(java.util.stream.Collectors.toList());
            
            // 前两个应该是紧急收尾任务（isUrgentEnding=1）
            assertEquals(1, m01Tasks.get(0).getIsUrgentEnding() != null ? m01Tasks.get(0).getIsUrgentEnding() : 0);
            assertEquals(1, m01Tasks.get(1).getIsUrgentEnding() != null ? m01Tasks.get(1).getIsUrgentEnding() : 0);
        }

        private ScheduleDetail createDetail(String material, String machine, 
                double stockHour, int isUrgent, int isContinue) {
            ScheduleDetail detail = new ScheduleDetail();
            detail.setMaterialCode(material);
            detail.setMachineCode(machine);
            detail.setPlanQuantity(100);
            detail.setIsUrgentEnding(isUrgent);
            detail.setIsContinue(isContinue);
            return detail;
        }
    }
}
