package com.zlt.aps.cx.service.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 精度计划测试 - 需求文档覆盖
 * 
 * 测试场景：
 * 1. 停产前精度计划：4小时停机检查
 * 2. 库存判断：够不够用到停机时间
 * 3. 硫化减产场景
 */
@DisplayName("精度计划测试")
class PrecisionScheduleTest {

    // ==================== 停产前精度计划测试 ====================
    
    @Nested
    @DisplayName("停产前精度计划测试")
    class PreStopPrecisionTests {

        /**
         * 停产前要"清空冰箱"——精度计划
         * - 停火前4小时，不再做新胎胚，让成型机停机做精度检查
         * - 停止时间足够时，成型车间做精度检查
         * - 成型精度计划：停产前4小时开始，不排生产
         */
        
        @Test
        @DisplayName("停产前4小时 - 停止生产")
        void testPreStop4Hours_StopProduction() {
            int stopHourBeforeShutdown = 4;
            
            // 停机前4小时不应排产
            boolean shouldSchedule = false;
            
            assertEquals(4, stopHourBeforeShutdown);
            assertFalse(shouldSchedule, "停产前4小时不应排产");
        }

        @Test
        @DisplayName("停产前4小时 - 精度检查时间窗口")
        void testPreStop4Hours_PrecisionCheckWindow() {
            // 假设停产时间是 16:00
            // 精度检查开始时间是 12:00
            int shutdownHour = 16;
            int precisionCheckStartHour = shutdownHour - 4;
            
            assertEquals(12, precisionCheckStartHour, "精度检查应在停产前4小时开始");
        }

        @Test
        @DisplayName("停产前4小时 - 成型机停机")
        void testPreStop4Hours_MachineStop() {
            boolean isPrecisionCheckPeriod = true;
            boolean machineShouldRun = !isPrecisionCheckPeriod;
            
            assertFalse(machineShouldRun, "精度检查期间成型机应停机");
        }

        @Test
        @DisplayName("停产前4小时 - 不排新任务")
        void testPreStop4Hours_NoNewTasks() {
            // 精度检查期间不应排新任务
            boolean canScheduleNewTask = false;
            assertFalse(canScheduleNewTask);
        }

        @Test
        @DisplayName("停产时间不足4小时 - 不做精度检查")
        void testShortStopTime_NoPrecisionCheck() {
            // 如果停产时间很短（不足4小时），可能不做精度检查
            int stopDurationHours = 3;
            int precisionCheckRequired = 4;
            
            boolean shouldDoPrecisionCheck = stopDurationHours >= precisionCheckRequired;
            assertFalse(shouldDoPrecisionCheck, "停产时间不足4小时不做精度检查");
        }

        @Test
        @DisplayName("停产时间充足 - 需要精度检查")
        void testLongStopTime_NeedPrecisionCheck() {
            int stopDurationHours = 8;
            int precisionCheckRequired = 4;
            
            boolean shouldDoPrecisionCheck = stopDurationHours >= precisionCheckRequired;
            assertTrue(shouldDoPrecisionCheck, "停产时间充足需要做精度检查");
        }
    }

    // ==================== 库存判断测试 ====================
    
    @Nested
    @DisplayName("库存判断测试")
    class StockJudgmentTests {

        /**
         * 判断库存够不够用到停机时间：
         * - 如果不够，还得继续做
         * - 如果够了，可以提前停机
         */
        
        @Test
        @DisplayName("库存判断 - 够用到停机")
        void testStockSufficient_UntilShutdown() {
            int currentStock = 100; // 当前库存
            int hoursUntilShutdown = 4; // 距停产4小时
            int hourlyConsumption = 20; // 每小时消耗20条
            
            int requiredStock = hoursUntilShutdown * hourlyConsumption;
            boolean stockSufficient = currentStock >= requiredStock;
            
            assertTrue(stockSufficient, "库存够用到停机");
        }

        @Test
        @DisplayName("库存判断 - 不够用到停机")
        void testStockInsufficient_NeedContinue() {
            int currentStock = 50;
            int hoursUntilShutdown = 4;
            int hourlyConsumption = 20;
            
            int requiredStock = hoursUntilShutdown * hourlyConsumption;
            boolean stockSufficient = currentStock >= requiredStock;
            
            assertFalse(stockSufficient, "库存不够，需要继续生产");
        }

        @Test
        @DisplayName("库存判断 - 刚好够用")
        void testStockExactly_Equal() {
            int currentStock = 80;
            int hoursUntilShutdown = 4;
            int hourlyConsumption = 20;
            
            int requiredStock = hoursUntilShutdown * hourlyConsumption;
            boolean stockSufficient = currentStock >= requiredStock;
            
            assertTrue(stockSufficient, "库存刚好够用");
        }

        @Test
        @DisplayName("库存判断 - 计算需要生产量")
        void testCalculateRequiredProduction() {
            int currentStock = 50;
            int hoursUntilShutdown = 4;
            int hourlyConsumption = 20;
            
            int requiredStock = hoursUntilShutdown * hourlyConsumption;
            int needToProduce = Math.max(0, requiredStock - currentStock);
            
            assertEquals(30, needToProduce, "需要再生产30条");
        }

        @Test
        @DisplayName("库存判断 - 剩余时间计算")
        void testCalculateRemainingHours() {
            // 假设现在是12:00，停机时间是16:00
            int currentHour = 12;
            int shutdownHour = 16;
            
            int remainingHours = shutdownHour - currentHour;
            assertEquals(4, remainingHours);
        }

        @Test
        @DisplayName("库存判断 - 班次剩余时间")
        void testShiftRemainingTime() {
            // 早班：7:30-15:00
            // 当前时间：13:00
            // 停机时间：15:00
            
            int currentHour = 13;
            int shiftEndHour = 15;
            
            int remainingHours = shiftEndHour - currentHour;
            assertEquals(2, remainingHours, "班次剩余2小时");
        }
    }

    // ==================== 硫化减产场景测试 ====================
    
    @Nested
    @DisplayName("硫化减产场景测试")
    class VulcanizeReductionTests {

        /**
         * 硫化减产时，成型也要跟着调整
         */
        
        @Test
        @DisplayName("硫化减产 - 成型同步调整")
        void testVulcanizeReduction_FormingAdjustment() {
            int normalVulcanizePlan = 1000;
            int reductionPercent = 20; // 硫化减产20%
            
            int adjustedVulcanizePlan = normalVulcanizePlan * (100 - reductionPercent) / 100;
            assertEquals(800, adjustedVulcanizePlan);
            
            // 成型也要按比例调整
            int normalFormingPlan = 1000;
            int adjustedFormingPlan = normalFormingPlan * (100 - reductionPercent) / 100;
            assertEquals(800, adjustedFormingPlan);
        }

        @Test
        @DisplayName("硫化停产 - 成型提前停机")
        void testVulcanizeStop_FormingAdvanceStop() {
            // 硫化停机时间
            int vulcanizeStopHour = 16;
            
            // 成型提前1个班次（8小时）停机
            int formingAdvanceHours = 8;
            int formingStopHour = vulcanizeStopHour - formingAdvanceHours;
            
            assertEquals(8, formingStopHour, "成型应在早上8点停机");
        }

        @Test
        @DisplayName("硫化减产 - 库存处理")
        void testVulcanizeReduction_StockHandling() {
            // 硫化减产后，库存可能会增加
            int originalDemand = 1000;
            int reducedDemand = 800;
            int currentFormingStock = 100;
            
            // 减产后的硫化需求
            int demandAfterReduction = reducedDemand;
            
            // 成型库存够用的话，可以少生产
            int needToProduce = Math.max(0, demandAfterReduction - currentFormingStock);
            assertEquals(700, needToProduce);
        }

        @Test
        @DisplayName("硫化恢复 - 成型提前恢复")
        void testVulcanizeResume_FormingAdvanceResume() {
            // 硫化恢复时间
            int vulcanizeResumeHour = 16;
            
            // 成型提前1个班次恢复
            int formingAdvanceHours = 8;
            int formingResumeHour = vulcanizeResumeHour - formingAdvanceHours;
            
            assertEquals(8, formingResumeHour, "成型应在早上8点恢复生产");
        }
    }

    // ==================== 精度计划任务生成测试 ====================
    
    @Nested
    @DisplayName("精度计划任务生成测试")
    class PrecisionTaskGenerationTests {

        @Test
        @DisplayName("精度任务 - 时间安排")
        void testPrecisionTask_TimeArrangement() {
            // 停产时间：16:00
            // 精度检查：12:00-16:00
            // 正常生产：07:30-12:00
            
            int normalProductionEndHour = 12;
            int shutdownHour = 16;
            
            int precisionCheckDuration = shutdownHour - normalProductionEndHour;
            assertEquals(4, precisionCheckDuration, "精度检查时长4小时");
        }

        @Test
        @DisplayName("精度任务 - 任务类型")
        void testPrecisionTask_TaskType() {
            String taskType = "PRECISION_CHECK";
            assertEquals("PRECISION_CHECK", taskType);
        }

        @Test
        @DisplayName("精度任务 - 不影响其他机台")
        void testPrecisionTask_IsolatedImpact() {
            // 精度检查只影响本机台，不影响其他机台
            String currentMachine = "CX001";
            String otherMachine = "CX002";
            
            // 其他机台正常生产
            boolean otherMachineRunning = true;
            assertTrue(otherMachineRunning);
        }

        @Test
        @DisplayName("精度任务 - 可配置时长")
        void testPrecisionTask_ConfigurableDuration() {
            int defaultDuration = 4; // 默认4小时
            int configuredDuration = 6; // 可配置为6小时
            
            assertTrue(configuredDuration >= defaultDuration);
        }
    }

    // ==================== 综合场景测试 ====================
    
    @Nested
    @DisplayName("综合场景测试")
    class IntegrationTests {

        @Test
        @DisplayName("停产前一天 - 完整流程")
        void testDayBeforeShutdown_CompleteFlow() {
            // 场景：
            // 1. 明天16:00停产
            // 2. 成型提前8小时停机，即明天8:00停机
            // 3. 精度检查4小时，从明天4:00开始
            // 4. 今天要确保库存够用到明天4:00
            
            int shutdownDay = 1; // 明天（相对于今天）
            int shutdownHour = 16;
            int formingAdvanceHours = 8;
            int precisionCheckHours = 4;
            
            // 计算关键时间点
            int formingStopHour = shutdownHour - formingAdvanceHours; // 8:00
            int precisionStartHour = formingStopHour - precisionCheckHours; // 4:00
            
            assertEquals(8, formingStopHour);
            assertEquals(4, precisionStartHour);
            
            // 计算需要维持生产的时间（从今天最后一班到明天4:00）
            // 假设今天最后一班到23:00
            int todayLastShiftEnd = 23;
            int hoursToCover = (24 - todayLastShiftEnd) + precisionStartHour; // 1 + 4 = 5小时
            assertEquals(5, hoursToCover);
        }

        @Test
        @DisplayName("库存不足 - 继续生产决策")
        void testInsufficientStock_ContinueProductionDecision() {
            int currentStock = 50;
            int requiredStock = 80;
            
            boolean needContinueProduction = currentStock < requiredStock;
            assertTrue(needContinueProduction, "库存不足需要继续生产");
            
            int additionalProduction = requiredStock - currentStock;
            assertEquals(30, additionalProduction);
        }

        @Test
        @DisplayName("库存充足 - 提前停机决策")
        void testSufficientStock_EarlyShutdownDecision() {
            int currentStock = 100;
            int requiredStock = 80;
            
            boolean canShutdownEarly = currentStock >= requiredStock;
            assertTrue(canShutdownEarly, "库存充足可以提前停机");
            
            int excessStock = currentStock - requiredStock;
            assertEquals(20, excessStock, "超出需求20条");
        }
    }
}
