package com.jinyu.aps.service;

import com.jinyu.aps.entity.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P1核心功能测试类
 * 
 * 测试：
 * 4. 节假日开产/停产管理
 * 5. 试制量试插单逻辑
 * 6. 班次动态调整
 *
 * @author APS Team
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("P1核心功能测试")
class P1CoreServiceTest {

    @Autowired
    private P1CoreService p1CoreService;

    // ==================== 4. 节假日开产/停产管理测试 ====================

    @Nested
    @DisplayName("节假日管理测试")
    class HolidayManagementTests {

        @Test
        @DisplayName("检查日期是否为节假日")
        void testCheckHoliday() {
            // Given: 节假日配置列表
            List<HolidayConfig> configs = new ArrayList<>();
            
            HolidayConfig holiday = new HolidayConfig();
            holiday.setHolidayDate(LocalDate.of(2024, 1, 1));
            holiday.setHolidayName("元旦");
            holiday.setStatus("ENABLED");
            configs.add(holiday);

            // When: 检查节假日
            HolidayConfig result = p1CoreService.checkHoliday(configs, LocalDate.of(2024, 1, 1));

            // Then: 应返回节假日配置
            assertNotNull(result);
            assertEquals("元旦", result.getHolidayName());
        }

        @Test
        @DisplayName("检查非节假日")
        void testCheckNonHoliday() {
            // Given: 节假日配置
            List<HolidayConfig> configs = new ArrayList<>();
            HolidayConfig holiday = new HolidayConfig();
            holiday.setHolidayDate(LocalDate.of(2024, 1, 1));
            holiday.setStatus("ENABLED");
            configs.add(holiday);

            // When: 检查非节假日
            HolidayConfig result = p1CoreService.checkHoliday(configs, LocalDate.of(2024, 1, 2));

            // Then: 应返回null
            assertNull(result);
        }

        @Test
        @DisplayName("获取节假日产能比例-正常情况")
        void testGetHolidayCapacityRatio_Normal() {
            // Given: 无节假日
            // When
            int ratio = p1CoreService.getHolidayCapacityRatio(null, "DAY");

            // Then: 应返回100%
            assertEquals(100, ratio);
        }

        @Test
        @DisplayName("获取节假日产能比例-减产情况")
        void testGetHolidayCapacityRatio_Reduced() {
            // Given: 节假日减产配置
            HolidayConfig config = new HolidayConfig();
            config.setCapacityRatio(50);
            config.setEffectiveShifts("DAY,NIGHT");

            // When: 检查受影响班次
            int ratioDay = p1CoreService.getHolidayCapacityRatio(config, "DAY");
            int ratioAfternoon = p1CoreService.getHolidayCapacityRatio(config, "AFTERNOON");

            // Then: 白班减产，中班正常
            assertEquals(50, ratioDay);
            assertEquals(100, ratioAfternoon);
        }

        @Test
        @DisplayName("节假日调整机台产能-停产")
        void testAdjustCapacityForHoliday_Stopped() {
            // Given: 停产配置
            List<Machine> machines = createTestMachines();
            
            HolidayConfig config = new HolidayConfig();
            config.setHolidayName("春节");
            config.setProductionStatus("STOPPED");
            config.setCapacityRatio(0);

            // When: 调整产能
            List<Machine> adjusted = p1CoreService.adjustCapacityForHoliday(machines, config, "DAY");

            // Then: 应返回空列表
            assertTrue(adjusted.isEmpty());
        }

        @Test
        @DisplayName("节假日调整机台产能-减产")
        void testAdjustCapacityForHoliday_Reduced() {
            // Given: 减产配置
            List<Machine> machines = createTestMachines();
            
            HolidayConfig config = new HolidayConfig();
            config.setHolidayName("国庆");
            config.setProductionStatus("REDUCED");
            config.setCapacityRatio(50);
            config.setEffectiveShifts("DAY");

            // When: 调整产能
            List<Machine> adjusted = p1CoreService.adjustCapacityForHoliday(machines, config, "DAY");

            // Then: 产能应减半
            assertEquals(2, adjusted.size());
            assertEquals(120, adjusted.get(0).getMaxDailyCapacity()); // 240 * 50%
        }

        @Test
        @DisplayName("判断是否可以在节假日排产")
        void testCanScheduleOnHoliday() {
            // Given: 不同配置
            HolidayConfig stopped = new HolidayConfig();
            stopped.setProductionStatus("STOPPED");
            
            HolidayConfig reduced = new HolidayConfig();
            reduced.setProductionStatus("REDUCED");

            // When & Then
            assertFalse(p1CoreService.canScheduleOnHoliday(stopped));
            assertTrue(p1CoreService.canScheduleOnHoliday(reduced));
            assertTrue(p1CoreService.canScheduleOnHoliday(null));
        }
    }

    // ==================== 5. 试制量试插单逻辑测试 ====================

    @Nested
    @DisplayName("试制插单测试")
    class TrialInsertTests {

        @Test
        @DisplayName("试制任务转换为日胚任务")
        void testConvertTrialToDailyTask() {
            // Given: 试制任务
            TrialTask trial = new TrialTask();
            trial.setTrialNo("TR20240101001");
            trial.setMaterialCode("MAT-001");
            trial.setStructureCode("STR-A");
            trial.setTrialQuantity(100);
            trial.setProducedQuantity(20);
            trial.setExpectedStartDate(LocalDate.now());
            trial.setPriority(8);

            // When: 转换
            DailyEmbryoTask task = p1CoreService.convertTrialToDailyTask(trial);

            // Then: 验证转换结果
            assertEquals("MAT-001", task.getMaterialCode());
            assertEquals("STR-A", task.getStructureCode());
            assertEquals(80, task.getPlanQuantity()); // 100 - 20
            assertEquals(8, task.getPriority());
            assertEquals(1, task.getIsTrialTask());
        }

        @Test
        @DisplayName("计算试制优先级-强制插单")
        void testCalculateTrialPriority_Force() {
            // When: 强制插单
            int priority = p1CoreService.calculateTrialPriority("FORCE", 5);

            // Then: 最高优先级
            assertEquals(10, priority);
        }

        @Test
        @DisplayName("计算试制优先级-紧急插单")
        void testCalculateTrialPriority_Urgent() {
            // When: 紧急插单
            int priority = p1CoreService.calculateTrialPriority("URGENT", 5);

            // Then: 提升3级
            assertEquals(8, priority);
        }

        @Test
        @DisplayName("检查试制任务是否可以插单")
        void testCanInsertTrialTask() {
            // Given: 有效试制任务
            TrialTask trial = new TrialTask();
            trial.setTrialNo("TR001");
            trial.setStatus("PENDING");
            trial.setExpectedStartDate(LocalDate.now().plusDays(1));
            
            List<DailyEmbryoTask> existingTasks = new ArrayList<>();
            List<Machine> machines = createTestMachines();

            // When
            boolean canInsert = p1CoreService.canInsertTrialTask(trial, existingTasks, machines);

            // Then: 可以插单
            assertTrue(canInsert);
        }

        @Test
        @DisplayName("检查过期试制任务不可插单")
        void testCannotInsertExpiredTrialTask() {
            // Given: 过期试制任务
            TrialTask trial = new TrialTask();
            trial.setTrialNo("TR001");
            trial.setStatus("PENDING");
            trial.setExpectedStartDate(LocalDate.now().minusDays(1));
            
            List<DailyEmbryoTask> existingTasks = new ArrayList<>();
            List<Machine> machines = createTestMachines();

            // When
            boolean canInsert = p1CoreService.canInsertTrialTask(trial, existingTasks, machines);

            // Then: 不可插单
            assertFalse(canInsert);
        }

        @Test
        @DisplayName("执行试制任务插单")
        void testInsertTrialTask() {
            // Given: 试制任务和现有任务
            TrialTask trial = new TrialTask();
            trial.setTrialNo("TR001");
            trial.setMaterialCode("MAT-NEW");
            trial.setStructureCode("STR-B");
            trial.setTrialQuantity(50);
            trial.setStatus("PENDING");
            trial.setInsertType("URGENT");
            trial.setPriority(5);
            trial.setExpectedStartDate(LocalDate.now());

            List<DailyEmbryoTask> existingTasks = new ArrayList<>();
            DailyEmbryoTask existing = new DailyEmbryoTask();
            existing.setMaterialCode("MAT-001");
            existing.setPriority(5);
            existingTasks.add(existing);

            List<Machine> machines = createTestMachines();

            // When: 插单
            List<DailyEmbryoTask> result = p1CoreService.insertTrialTask(trial, existingTasks, machines);

            // Then: 应包含试制任务
            assertEquals(2, result.size());
            // 试制任务应排在前面（优先级更高）
            assertEquals("MAT-NEW", result.get(0).getMaterialCode());
            assertEquals(8, result.get(0).getPriority()); // 5 + 3
        }

        @Test
        @DisplayName("批量处理试制任务插单")
        void testBatchInsertTrialTasks() {
            // Given: 多个试制任务
            List<TrialTask> trials = new ArrayList<>();
            
            TrialTask t1 = new TrialTask();
            t1.setTrialNo("TR001");
            t1.setMaterialCode("MAT-A");
            t1.setStructureCode("STR-A");
            t1.setTrialQuantity(50);
            t1.setStatus("PENDING");
            t1.setInsertType("NORMAL");
            t1.setPriority(3);
            t1.setExpectedStartDate(LocalDate.now());
            trials.add(t1);

            TrialTask t2 = new TrialTask();
            t2.setTrialNo("TR002");
            t2.setMaterialCode("MAT-B");
            t2.setStructureCode("STR-B");
            t2.setTrialQuantity(30);
            t2.setStatus("PENDING");
            t2.setInsertType("FORCE");
            t2.setPriority(5);
            t2.setExpectedStartDate(LocalDate.now());
            trials.add(t2);

            List<DailyEmbryoTask> existingTasks = new ArrayList<>();
            List<Machine> machines = createTestMachines();

            // When: 批量插单
            List<DailyEmbryoTask> result = p1CoreService.batchInsertTrialTasks(trials, existingTasks, machines);

            // Then: 强制插单应排在前面
            assertEquals(2, result.size());
            assertEquals("MAT-B", result.get(0).getMaterialCode()); // 强制插单优先
        }
    }

    // ==================== 6. 班次动态调整测试 ====================

    @Nested
    @DisplayName("班次调整测试")
    class ShiftAdjustmentTests {

        @Test
        @DisplayName("获取班次配置")
        void testGetShiftConfig() {
            // Given: 班次配置列表
            List<ShiftConfig> configs = new ArrayList<>();
            
            ShiftConfig dayConfig = new ShiftConfig();
            dayConfig.setConfigDate(LocalDate.now());
            dayConfig.setShiftCode("DAY");
            dayConfig.setIsActive(1);
            configs.add(dayConfig);

            // When
            ShiftConfig result = p1CoreService.getShiftConfig(configs, LocalDate.now(), "DAY");

            // Then
            assertNotNull(result);
            assertEquals("DAY", result.getShiftCode());
        }

        @Test
        @DisplayName("计算有效工时-无配置")
        void testCalculateEffectiveHours_NoConfig() {
            // When: 无配置
            int hours = p1CoreService.calculateEffectiveHours(null, 8);

            // Then: 使用默认值
            assertEquals(8, hours);
        }

        @Test
        @DisplayName("计算有效工时-有配置")
        void testCalculateEffectiveHours_WithConfig() {
            // Given: 调整后工时为6
            ShiftConfig config = new ShiftConfig();
            config.setStandardHours(8);
            config.setAdjustedHours(6);

            // When
            int hours = p1CoreService.calculateEffectiveHours(config, 8);

            // Then: 使用调整后值
            assertEquals(6, hours);
        }

        @Test
        @DisplayName("计算机台班次产能")
        void testAdjustMachineCapacityForShift() {
            // Given: 机台和班次配置
            Machine machine = new Machine();
            machine.setMachineCode("M01");
            machine.setMaxDailyCapacity(240);

            ShiftConfig config = new ShiftConfig();
            config.setStandardHours(8);
            config.setAdjustedHours(6);
            config.setCapacityRatio(80);

            // When: 调整产能
            int capacity = p1CoreService.adjustMachineCapacityForShift(machine, config, "DAY");

            // Then: 240/3 * (6/8) * 80% = 80 * 0.75 * 0.8 = 48
            assertEquals(48, capacity);
        }

        @Test
        @DisplayName("批量计算班次产能")
        void testCalculateShiftCapacities() {
            // Given: 多个机台
            List<Machine> machines = createTestMachines();
            
            ShiftConfig config = new ShiftConfig();
            config.setStandardHours(8);
            config.setAdjustedHours(8);
            config.setCapacityRatio(100);
            config.setIsActive(1);

            List<ShiftConfig> configs = new ArrayList<>();
            configs.add(config);

            // When
            Map<String, Integer> capacityMap = p1CoreService.calculateShiftCapacities(
                machines, configs, LocalDate.now(), "DAY");

            // Then
            assertEquals(2, capacityMap.size());
            assertEquals(80, capacityMap.get("M01")); // 240/3
        }

        @Test
        @DisplayName("验证班次配置-正常情况")
        void testValidateShiftConfig_Valid() {
            // Given: 有效配置
            ShiftConfig config = new ShiftConfig();
            config.setStartTime(LocalTime.of(8, 0));
            config.setEndTime(LocalTime.of(16, 0));
            config.setAdjustedHours(8);
            config.setCapacityRatio(100);

            // When
            String error = p1CoreService.validateShiftConfig(config);

            // Then: 应为null（通过验证）
            assertNull(error);
        }

        @Test
        @DisplayName("验证班次配置-时长超限")
        void testValidateShiftConfig_ExceededHours() {
            // Given: 超长工时配置
            ShiftConfig config = new ShiftConfig();
            config.setAdjustedHours(14);
            config.setCapacityRatio(100);

            // When
            String error = p1CoreService.validateShiftConfig(config);

            // Then: 应返回错误信息
            assertNotNull(error);
            assertTrue(error.contains("超过12小时"));
        }

        @Test
        @DisplayName("验证班次配置-产能比例超限")
        void testValidateShiftConfig_InvalidCapacityRatio() {
            // Given: 无效产能比例
            ShiftConfig config = new ShiftConfig();
            config.setAdjustedHours(8);
            config.setCapacityRatio(150);

            // When
            String error = p1CoreService.validateShiftConfig(config);

            // Then: 应返回错误信息
            assertNotNull(error);
            assertTrue(error.contains("产能比例"));
        }
    }

    // ==================== 辅助方法 ====================

    private List<Machine> createTestMachines() {
        List<Machine> machines = new ArrayList<>();
        
        Machine m1 = new Machine();
        m1.setId(1L);
        m1.setMachineCode("M01");
        m1.setMachineName("机台1");
        m1.setMaxDailyCapacity(240);
        m1.setIsActive(1);
        machines.add(m1);

        Machine m2 = new Machine();
        m2.setId(2L);
        m2.setMachineCode("M02");
        m2.setMachineName("机台2");
        m2.setMaxDailyCapacity(180);
        m2.setIsActive(1);
        machines.add(m2);

        return machines;
    }
}
