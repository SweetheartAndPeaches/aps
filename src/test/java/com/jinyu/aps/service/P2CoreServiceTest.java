package com.jinyu.aps.service;

import com.jinyu.aps.entity.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P2核心功能测试类
 * 
 * 测试：
 * 7. 操作工请假管理
 * 8. 胎面停放时间约束
 * 9. 材料异常处理
 *
 * @author APS Team
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("P2核心功能测试")
class P2CoreServiceTest {

    @Autowired
    private P2CoreService p2CoreService;

    // ==================== 7. 操作工请假管理测试 ====================

    @Nested
    @DisplayName("请假管理测试")
    class LeaveManagementTests {

        @Test
        @DisplayName("获取指定日期的请假记录")
        void testGetLeaveRecords() {
            // Given: 请假记录列表
            List<OperatorLeave> leaves = new ArrayList<>();
            
            OperatorLeave leave = new OperatorLeave();
            leave.setEmployeeNo("EMP001");
            leave.setMachineCode("M01");
            leave.setStartDate(LocalDate.now().minusDays(1));
            leave.setEndDate(LocalDate.now().plusDays(1));
            leave.setApprovalStatus("APPROVED");
            leaves.add(leave);

            // When
            List<OperatorLeave> result = p2CoreService.getLeaveRecords(leaves, LocalDate.now(), "DAY");

            // Then
            assertEquals(1, result.size());
            assertEquals("M01", result.get(0).getMachineCode());
        }

        @Test
        @DisplayName("计算机台请假影响比例-无请假")
        void testCalculateLeaveImpactRatio_NoLeave() {
            // Given: 空请假列表
            List<OperatorLeave> leaves = new ArrayList<>();

            // When
            int ratio = p2CoreService.calculateLeaveImpactRatio(leaves, "M01", LocalDate.now(), "DAY");

            // Then
            assertEquals(0, ratio);
        }

        @Test
        @DisplayName("计算机台请假影响比例-有请假")
        void testCalculateLeaveImpactRatio_WithLeave() {
            // Given: 请假记录
            List<OperatorLeave> leaves = new ArrayList<>();
            
            OperatorLeave leave = new OperatorLeave();
            leave.setMachineCode("M01");
            leave.setStartDate(LocalDate.now());
            leave.setEndDate(LocalDate.now());
            leave.setApprovalStatus("APPROVED");
            leave.setAffectCapacity(1);
            leave.setCapacityImpactRatio(30);
            leaves.add(leave);

            // When
            int ratio = p2CoreService.calculateLeaveImpactRatio(leaves, "M01", LocalDate.now(), "DAY");

            // Then
            assertEquals(30, ratio);
        }

        @Test
        @DisplayName("计算机台请假影响比例-多人请假累计")
        void testCalculateLeaveImpactRatio_MultipleLeaves() {
            // Given: 多人请假
            List<OperatorLeave> leaves = new ArrayList<>();
            
            for (int i = 0; i < 3; i++) {
                OperatorLeave leave = new OperatorLeave();
                leave.setMachineCode("M01");
                leave.setStartDate(LocalDate.now());
                leave.setEndDate(LocalDate.now());
                leave.setApprovalStatus("APPROVED");
                leave.setAffectCapacity(1);
                leave.setCapacityImpactRatio(40);
                leaves.add(leave);
            }

            // When
            int ratio = p2CoreService.calculateLeaveImpactRatio(leaves, "M01", LocalDate.now(), "DAY");

            // Then: 累计120%，上限100%
            assertEquals(100, ratio);
        }

        @Test
        @DisplayName("根据请假调整机台产能")
        void testAdjustCapacityForLeave() {
            // Given: 机台和请假记录
            Machine machine = new Machine();
            machine.setMachineCode("M01");
            machine.setMaxDailyCapacity(240);

            List<OperatorLeave> leaves = new ArrayList<>();
            OperatorLeave leave = new OperatorLeave();
            leave.setMachineCode("M01");
            leave.setStartDate(LocalDate.now());
            leave.setEndDate(LocalDate.now());
            leave.setApprovalStatus("APPROVED");
            leave.setAffectCapacity(1);
            leave.setCapacityImpactRatio(50);
            leaves.add(leave);

            // When
            int capacity = p2CoreService.adjustCapacityForLeave(machine, leaves, LocalDate.now(), "DAY");

            // Then: 班次产能80，影响50%后为40
            assertEquals(40, capacity);
        }

        @Test
        @DisplayName("获取请假统计信息")
        void testGetLeaveStatistics() {
            // Given: 请假记录
            List<OperatorLeave> leaves = new ArrayList<>();
            
            OperatorLeave l1 = new OperatorLeave();
            l1.setLeaveType("ANNUAL");
            l1.setMachineCode("M01");
            l1.setStartDate(LocalDate.now());
            l1.setEndDate(LocalDate.now());
            l1.setLeaveDays(1);
            leaves.add(l1);

            OperatorLeave l2 = new OperatorLeave();
            l2.setLeaveType("SICK");
            l2.setMachineCode("M02");
            l2.setStartDate(LocalDate.now());
            l2.setEndDate(LocalDate.now().plusDays(1));
            l2.setLeaveDays(2);
            leaves.add(l2);

            // When
            Map<String, Object> stats = p2CoreService.getLeaveStatistics(
                leaves, LocalDate.now().minusDays(1), LocalDate.now().plusDays(2));

            // Then
            assertEquals(2, stats.get("totalRecords"));
            assertEquals(3, stats.get("totalLeaveDays"));
            assertNotNull(stats.get("byType"));
            assertNotNull(stats.get("byMachine"));
        }
    }

    // ==================== 8. 胎面停放时间约束测试 ====================

    @Nested
    @DisplayName("停放时间约束测试")
    class ParkingConstraintTests {

        @Test
        @DisplayName("获取停放时间配置")
        void testGetParkingConfig() {
            // Given: 配置列表
            List<TreadParkingConfig> configs = new ArrayList<>();
            
            TreadParkingConfig config = new TreadParkingConfig();
            config.setStructureCode("STR-A");
            config.setMinParkingHours(4);
            config.setMaxParkingHours(48);
            config.setIsEnabled(1);
            configs.add(config);

            // When
            TreadParkingConfig result = p2CoreService.getParkingConfig(configs, "STR-A");

            // Then
            assertNotNull(result);
            assertEquals(4, result.getMinParkingHours());
        }

        @Test
        @DisplayName("计算停放时间")
        void testCalculateParkingHours() {
            // Given: 生产时间和当前时间
            LocalDateTime produceTime = LocalDateTime.now().minusHours(10);
            LocalDateTime currentTime = LocalDateTime.now();

            // When
            double hours = p2CoreService.calculateParkingHours(produceTime, currentTime);

            // Then
            assertEquals(10.0, hours, 0.1);
        }

        @Test
        @DisplayName("检查停放状态-正常")
        void testCheckParkingStatus_Normal() {
            // Given: 正常停放时间
            TreadParkingConfig config = new TreadParkingConfig();
            config.setMinParkingHours(4);
            config.setMaxParkingHours(48);
            config.setWarningThresholdHours(44);

            // When
            String status = p2CoreService.checkParkingStatus(20, config);

            // Then
            assertEquals("NORMAL", status);
        }

        @Test
        @DisplayName("检查停放状态-预警")
        void testCheckParkingStatus_Warning() {
            // Given: 接近上限停放时间
            TreadParkingConfig config = new TreadParkingConfig();
            config.setMinParkingHours(4);
            config.setMaxParkingHours(48);
            config.setWarningThresholdHours(44);

            // When
            String status = p2CoreService.checkParkingStatus(46, config);

            // Then
            assertEquals("WARNING", status);
        }

        @Test
        @DisplayName("检查停放状态-超时")
        void testCheckParkingStatus_Exceeded() {
            // Given: 超时停放时间
            TreadParkingConfig config = new TreadParkingConfig();
            config.setMinParkingHours(4);
            config.setMaxParkingHours(48);
            config.setWarningThresholdHours(44);

            // When
            String status = p2CoreService.checkParkingStatus(50, config);

            // Then
            assertEquals("EXCEEDED", status);
        }

        @Test
        @DisplayName("检查停放状态-时间不足")
        void testCheckParkingStatus_UnderMin() {
            // Given: 停放时间不足
            TreadParkingConfig config = new TreadParkingConfig();
            config.setMinParkingHours(4);
            config.setMaxParkingHours(48);

            // When
            String status = p2CoreService.checkParkingStatus(2, config);

            // Then
            assertEquals("WARNING", status);
        }

        @Test
        @DisplayName("根据停放状态调整优先级")
        void testAdjustPriorityForParking() {
            // Given: 超时任务
            DailyEmbryoTask task = new DailyEmbryoTask();
            task.setMaterialCode("MAT-001");
            task.setPriority(5);

            TreadParkingConfig config = new TreadParkingConfig();
            config.setMaxParkingHours(48);
            config.setTimeoutAction("ADJUST_PRIORITY");
            config.setPriorityBoost(3);

            // When
            int newPriority = p2CoreService.adjustPriorityForParking(task, 50, config);

            // Then: 优先级提升
            assertEquals(8, newPriority);
        }

        @Test
        @DisplayName("获取停放预警任务列表")
        void testGetParkingWarningTasks() {
            // Given: 混合任务列表
            List<DailyEmbryoTask> tasks = new ArrayList<>();
            
            DailyEmbryoTask t1 = new DailyEmbryoTask();
            t1.setMaterialCode("MAT-001");
            t1.setStructureCode("STR-A");
            t1.setProduceTime(LocalDateTime.now().minusHours(50)); // 超时
            tasks.add(t1);

            DailyEmbryoTask t2 = new DailyEmbryoTask();
            t2.setMaterialCode("MAT-002");
            t2.setStructureCode("STR-A");
            t2.setProduceTime(LocalDateTime.now().minusHours(20)); // 正常
            tasks.add(t2);

            List<TreadParkingConfig> configs = new ArrayList<>();
            TreadParkingConfig config = new TreadParkingConfig();
            config.setStructureCode("STR-A");
            config.setMaxParkingHours(48);
            config.setWarningThresholdHours(44);
            config.setIsEnabled(1);
            configs.add(config);

            // When
            List<DailyEmbryoTask> warnings = p2CoreService.getParkingWarningTasks(
                tasks, configs, LocalDateTime.now());

            // Then
            assertEquals(1, warnings.size());
            assertEquals("MAT-001", warnings.get(0).getMaterialCode());
        }
    }

    // ==================== 9. 材料异常处理测试 ====================

    @Nested
    @DisplayName("材料异常处理测试")
    class MaterialExceptionTests {

        @Test
        @DisplayName("获取有效材料异常记录")
        void testGetActiveExceptions() {
            // Given: 异常记录列表
            List<MaterialException> exceptions = new ArrayList<>();
            
            MaterialException e1 = new MaterialException();
            e1.setMaterialCode("MAT-001");
            e1.setStatus("PENDING");
            e1.setAffectStartDate(LocalDate.now().minusDays(1));
            e1.setAffectEndDate(LocalDate.now().plusDays(1));
            exceptions.add(e1);

            MaterialException e2 = new MaterialException();
            e2.setMaterialCode("MAT-002");
            e2.setStatus("RESOLVED");
            exceptions.add(e2);

            // When
            List<MaterialException> active = p2CoreService.getActiveExceptions(exceptions, LocalDate.now());

            // Then
            assertEquals(1, active.size());
            assertEquals("MAT-001", active.get(0).getMaterialCode());
        }

        @Test
        @DisplayName("检查材料是否有异常")
        void testHasMaterialException() {
            // Given: 异常记录
            List<MaterialException> exceptions = new ArrayList<>();
            MaterialException e = new MaterialException();
            e.setMaterialCode("MAT-001");
            e.setStatus("PENDING");
            exceptions.add(e);

            // When
            boolean hasException = p2CoreService.hasMaterialException(exceptions, "MAT-001", LocalDate.now());
            boolean noException = p2CoreService.hasMaterialException(exceptions, "MAT-002", LocalDate.now());

            // Then
            assertTrue(hasException);
            assertFalse(noException);
        }

        @Test
        @DisplayName("获取替代材料")
        void testGetSubstituteMaterial() {
            // Given: 材料替换异常
            List<MaterialException> exceptions = new ArrayList<>();
            MaterialException e = new MaterialException();
            e.setMaterialCode("MAT-001");
            e.setStatus("PENDING");
            e.setHandlingMethod("CHANGE_MATERIAL");
            e.setSubstituteMaterial("MAT-001-SUB");
            e.setAffectStartDate(LocalDate.now());
            exceptions.add(e);

            // When
            String substitute = p2CoreService.getSubstituteMaterial(exceptions, "MAT-001", LocalDate.now());

            // Then
            assertEquals("MAT-001-SUB", substitute);
        }

        @Test
        @DisplayName("处理材料异常-正常")
        void testHandleMaterialException_Normal() {
            // Given: 无异常的任务
            DailyEmbryoTask task = new DailyEmbryoTask();
            task.setMaterialCode("MAT-001");

            // When
            String result = p2CoreService.handleMaterialException(
                task, new ArrayList<>(), LocalDate.now());

            // Then
            assertEquals("NORMAL", result);
        }

        @Test
        @DisplayName("处理材料异常-取消计划")
        void testHandleMaterialException_Skipped() {
            // Given: 取消计划的异常
            DailyEmbryoTask task = new DailyEmbryoTask();
            task.setId(1L);
            task.setMaterialCode("MAT-001");

            List<MaterialException> exceptions = new ArrayList<>();
            MaterialException e = new MaterialException();
            e.setMaterialCode("MAT-001");
            e.setStatus("PENDING");
            e.setHandlingMethod("CANCEL_PLAN");
            e.setAffectStartDate(LocalDate.now());
            exceptions.add(e);

            // When
            String result = p2CoreService.handleMaterialException(task, exceptions, LocalDate.now());

            // Then
            assertEquals("SKIPPED", result);
        }

        @Test
        @DisplayName("处理材料异常-替换材料")
        void testHandleMaterialException_Substituted() {
            // Given: 材料替换异常
            DailyEmbryoTask task = new DailyEmbryoTask();
            task.setId(1L);
            task.setMaterialCode("MAT-001");

            List<MaterialException> exceptions = new ArrayList<>();
            MaterialException e = new MaterialException();
            e.setMaterialCode("MAT-001");
            e.setStatus("PENDING");
            e.setHandlingMethod("CHANGE_MATERIAL");
            e.setSubstituteMaterial("MAT-001-SUB");
            e.setAffectStartDate(LocalDate.now());
            exceptions.add(e);

            // When
            String result = p2CoreService.handleMaterialException(task, exceptions, LocalDate.now());

            // Then
            assertEquals("SUBSTITUTED", result);
            assertEquals("MAT-001-SUB", task.getMaterialCode());
            assertEquals("MAT-001", task.getOriginalMaterialCode());
        }

        @Test
        @DisplayName("处理材料异常-调整计划量")
        void testHandleMaterialException_AdjustPlan() {
            // Given: 调整计划异常
            DailyEmbryoTask task = new DailyEmbryoTask();
            task.setId(1L);
            task.setMaterialCode("MAT-001");
            task.setPlanQuantity(100);

            List<MaterialException> exceptions = new ArrayList<>();
            MaterialException e = new MaterialException();
            e.setMaterialCode("MAT-001");
            e.setStatus("PENDING");
            e.setHandlingMethod("ADJUST_PLAN");
            e.setAffectedQuantity(30);
            e.setAffectStartDate(LocalDate.now());
            exceptions.add(e);

            // When
            String result = p2CoreService.handleMaterialException(task, exceptions, LocalDate.now());

            // Then
            assertEquals("NORMAL", result);
            assertEquals(70, task.getPlanQuantity());
        }

        @Test
        @DisplayName("获取材料异常统计")
        void testGetExceptionStatistics() {
            // Given: 异常记录
            List<MaterialException> exceptions = new ArrayList<>();
            
            MaterialException e1 = new MaterialException();
            e1.setExceptionType("QUALITY");
            e1.setExceptionLevel("HIGH");
            e1.setStatus("PENDING");
            e1.setAffectStartDate(LocalDate.now());
            exceptions.add(e1);

            MaterialException e2 = new MaterialException();
            e2.setExceptionType("SHORTAGE");
            e2.setExceptionLevel("CRITICAL");
            e2.setStatus("PROCESSING");
            e2.setAffectStartDate(LocalDate.now());
            exceptions.add(e2);

            // When
            Map<String, Object> stats = p2CoreService.getExceptionStatistics(
                exceptions, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));

            // Then
            assertEquals(2, stats.get("totalExceptions"));
            assertEquals(1L, stats.get("criticalCount"));
            assertNotNull(stats.get("byType"));
            assertNotNull(stats.get("byLevel"));
        }

        @Test
        @DisplayName("生成异常预警")
        void testGenerateExceptionAlerts() {
            // Given: 紧急异常
            List<MaterialException> exceptions = new ArrayList<>();
            
            MaterialException e1 = new MaterialException();
            e1.setExceptionNo("EX001");
            e1.setMaterialCode("MAT-001");
            e1.setMaterialName("材料A");
            e1.setExceptionType("QUALITY");
            e1.setExceptionLevel("CRITICAL");
            e1.setDescription("质量问题");
            e1.setHandlingMethod("CHANGE_MATERIAL");
            e1.setStatus("PENDING");
            e1.setAffectStartDate(LocalDate.now());
            exceptions.add(e1);

            MaterialException e2 = new MaterialException();
            e2.setExceptionNo("EX002");
            e2.setMaterialCode("MAT-002");
            e2.setExceptionType("SHORTAGE");
            e2.setExceptionLevel("LOW"); // 低等级，不在预警中
            e2.setStatus("PENDING");
            e2.setAffectStartDate(LocalDate.now());
            exceptions.add(e2);

            // When
            List<Map<String, Object>> alerts = p2CoreService.generateExceptionAlerts(
                exceptions, LocalDate.now());

            // Then: 只返回高等级和紧急等级
            assertEquals(1, alerts.size());
            assertEquals("EX001", alerts.get(0).get("exceptionNo"));
            assertEquals("CRITICAL", alerts.get(0).get("exceptionLevel"));
        }
    }
}
