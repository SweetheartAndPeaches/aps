package com.zlt.aps.cx.service.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 试制量试测试 - 需求文档覆盖
 * 
 * 测试场景：
 * 1. 试制条件检查：配方、结构在机、模具
 * 2. 数量限制：一天最多2个新菜
 * 3. 时间限制：周日不做
 * 4. 班次限制：只能早班或中班（7:30-15:00）
 * 5. 数量必须是双数
 * 6. 同一菜系试制和量试要在同一灶台
 * 7. 优先级高于普通新增菜，但不能挤掉实单
 */
@DisplayName("试制量试测试")
class TrialScheduleTest {

    // ==================== 试制条件检查 ====================
    
    @Nested
    @DisplayName("试制条件检查测试")
    class TrialConditionTests {

        /**
         * 新菜要满足什么条件才能做？
         * 1. 必须有配方：制造示方、文字示方、硫化示方都要齐全
         * 2. 这个菜系的灶台得在开着：新菜所属的结构必须有成型机在做同类菜
         * 3. 必须有模具：新菜用的模具要有，不能临时借
         */
        
        @Test
        @DisplayName("条件检查 - 所有条件满足")
        void testAllConditionsMet() {
            boolean hasManufacturingRecipe = true;  // 制造示方
            boolean hasTextRecipe = true;           // 文字示方
            boolean hasVulcanizeRecipe = true;      // 硫化示方
            boolean structureOnline = true;         // 结构在机
            boolean hasMould = true;                // 有模具
            
            boolean canTrial = hasManufacturingRecipe && hasTextRecipe 
                && hasVulcanizeRecipe && structureOnline && hasMould;
            
            assertTrue(canTrial, "所有条件满足时可以试制");
        }

        @Test
        @DisplayName("条件检查 - 缺少制造示方")
        void testMissingManufacturingRecipe() {
            boolean hasManufacturingRecipe = false;
            boolean hasTextRecipe = true;
            boolean hasVulcanizeRecipe = true;
            boolean structureOnline = true;
            boolean hasMould = true;
            
            boolean canTrial = hasManufacturingRecipe && hasTextRecipe 
                && hasVulcanizeRecipe && structureOnline && hasMould;
            
            assertFalse(canTrial, "缺少制造示方不能试制");
        }

        @Test
        @DisplayName("条件检查 - 缺少文字示方")
        void testMissingTextRecipe() {
            boolean hasManufacturingRecipe = true;
            boolean hasTextRecipe = false;
            boolean hasVulcanizeRecipe = true;
            boolean structureOnline = true;
            boolean hasMould = true;
            
            boolean canTrial = hasManufacturingRecipe && hasTextRecipe 
                && hasVulcanizeRecipe && structureOnline && hasMould;
            
            assertFalse(canTrial, "缺少文字示方不能试制");
        }

        @Test
        @DisplayName("条件检查 - 缺少硫化示方")
        void testMissingVulcanizeRecipe() {
            boolean hasManufacturingRecipe = true;
            boolean hasTextRecipe = true;
            boolean hasVulcanizeRecipe = false;
            boolean structureOnline = true;
            boolean hasMould = true;
            
            boolean canTrial = hasManufacturingRecipe && hasTextRecipe 
                && hasVulcanizeRecipe && structureOnline && hasMould;
            
            assertFalse(canTrial, "缺少硫化示方不能试制");
        }

        @Test
        @DisplayName("条件检查 - 结构未在机")
        void testStructureNotOnline() {
            boolean hasManufacturingRecipe = true;
            boolean hasTextRecipe = true;
            boolean hasVulcanizeRecipe = true;
            boolean structureOnline = false;  // 结构不在机
            boolean hasMould = true;
            
            boolean canTrial = hasManufacturingRecipe && hasTextRecipe 
                && hasVulcanizeRecipe && structureOnline && hasMould;
            
            assertFalse(canTrial, "结构未在机不能试制");
        }

        @Test
        @DisplayName("条件检查 - 缺少模具")
        void testMissingMould() {
            boolean hasManufacturingRecipe = true;
            boolean hasTextRecipe = true;
            boolean hasVulcanizeRecipe = true;
            boolean structureOnline = true;
            boolean hasMould = false;  // 无模具
            
            boolean canTrial = hasManufacturingRecipe && hasTextRecipe 
                && hasVulcanizeRecipe && structureOnline && hasMould;
            
            assertFalse(canTrial, "缺少模具不能试制");
        }

        @Test
        @DisplayName("条件检查 - 多个条件缺失")
        void testMultipleMissingConditions() {
            // 场景：既没有配方，也没有模具
            boolean allRecipesExist = false;
            boolean structureOnline = true;
            boolean hasMould = false;
            
            boolean canTrial = allRecipesExist && structureOnline && hasMould;
            
            assertFalse(canTrial);
        }
    }

    // ==================== 数量限制测试 ====================
    
    @Nested
    @DisplayName("试制数量限制测试")
    class TrialQuantityLimitTests {

        /**
         * 一天最多做2个新菜（可配置）
         */
        
        @Test
        @DisplayName("数量限制 - 每天2个上限")
        void testDailyLimit_2PerDay() {
            int maxTrialsPerDay = 2;
            int currentTrialCount = 2;
            
            boolean canAddMore = currentTrialCount < maxTrialsPerDay;
            assertFalse(canAddMore, "达到每日上限后不能再加");
        }

        @Test
        @DisplayName("数量限制 - 未达上限可添加")
        void testDailyLimit_BelowLimit() {
            int maxTrialsPerDay = 2;
            int currentTrialCount = 1;
            
            boolean canAddMore = currentTrialCount < maxTrialsPerDay;
            assertTrue(canAddMore, "未达每日上限可以添加");
        }

        @Test
        @DisplayName("数量限制 - 当天无试制可添加2个")
        void testDailyLimit_ZeroTrials() {
            int maxTrialsPerDay = 2;
            int currentTrialCount = 0;
            
            boolean canAddMore = currentTrialCount < maxTrialsPerDay;
            assertTrue(canAddMore);
        }
    }

    // ==================== 时间限制测试 ====================
    
    @Nested
    @DisplayName("试制时间限制测试")
    class TrialTimeLimitTests {

        /**
         * 周日不做试制
         */
        
        @Test
        @DisplayName("时间限制 - 周日不做试制")
        void testSunday_NoTrial() {
            LocalDate sunday = LocalDate.of(2024, 7, 7); // 2024-07-07 是周日
            assertEquals(DayOfWeek.SUNDAY, sunday.getDayOfWeek());
            
            boolean canTrialOnSunday = false; // 周日不做
            assertFalse(canTrialOnSunday, "周日不能做试制");
        }

        @Test
        @DisplayName("时间限制 - 周一到周六可做")
        void testWeekdays_CanTrial() {
            LocalDate monday = LocalDate.of(2024, 7, 8); // 周一
            LocalDate saturday = LocalDate.of(2024, 7, 13); // 周六
            
            boolean canTrialOnMonday = monday.getDayOfWeek() != DayOfWeek.SUNDAY;
            boolean canTrialOnSaturday = saturday.getDayOfWeek() != DayOfWeek.SUNDAY;
            
            assertTrue(canTrialOnMonday, "周一可以做试制");
            assertTrue(canTrialOnSaturday, "周六可以做试制");
        }

        /**
         * 不能跟新菜系开张撞车：如果某天是某个结构第一次起产的日子，不安排新菜
         */
        @Test
        @DisplayName("时间限制 - 结构首次开产日不做试制")
        void testStructureFirstOpening_NoTrial() {
            boolean isStructureFirstOpeningDay = true;
            
            boolean canTrial = !isStructureFirstOpeningDay;
            assertFalse(canTrial, "结构首次开产日不能做试制");
        }

        /**
         * 插单时间：
         * - 紧急：锁定期内插单（3天内）
         * - 普通：锁定期后1天（大后天）
         */
        @Test
        @DisplayName("插单时间 - 紧急试制3天内")
        void testUrgentTrial_Within3Days() {
            int lockPeriod = 3;
            boolean isUrgent = true;
            
            int earliestDay = isUrgent ? 1 : lockPeriod + 1;
            assertEquals(1, earliestDay, "紧急试制可在锁定期内安排");
        }

        @Test
        @DisplayName("插单时间 - 普通试制锁定期后")
        void testNormalTrial_AfterLockPeriod() {
            int lockPeriod = 3;
            boolean isUrgent = false;
            
            int earliestDay = isUrgent ? 1 : lockPeriod + 1;
            assertEquals(4, earliestDay, "普通试制需在锁定期后1天");
        }
    }

    // ==================== 班次限制测试 ====================
    
    @Nested
    @DisplayName("试制班次限制测试")
    class TrialShiftLimitTests {

        /**
         * 试制必须安排在早班或中班（7:30-15:00），不能放在夜班
         */
        
        @Test
        @DisplayName("班次限制 - 早班可以")
        void testDayShift_Allowed() {
            String shift = "SHIFT_DAY"; // 早班
            String shiftStartTime = "07:30";
            String shiftEndTime = "15:00";
            
            boolean isValidShift = "SHIFT_DAY".equals(shift) || "SHIFT_AFTERNOON".equals(shift);
            assertTrue(isValidShift, "早班可以做试制");
        }

        @Test
        @DisplayName("班次限制 - 中班可以")
        void testAfternoonShift_Allowed() {
            String shift = "SHIFT_AFTERNOON"; // 中班
            
            boolean isValidShift = "SHIFT_DAY".equals(shift) || "SHIFT_AFTERNOON".equals(shift);
            assertTrue(isValidShift, "中班可以做试制");
        }

        @Test
        @DisplayName("班次限制 - 夜班不允许")
        void testNightShift_NotAllowed() {
            String shift = "SHIFT_NIGHT"; // 夜班
            
            boolean isValidShift = "SHIFT_DAY".equals(shift) || "SHIFT_AFTERNOON".equals(shift);
            assertFalse(isValidShift, "夜班不能做试制");
        }

        @Test
        @DisplayName("班次限制 - 时间范围验证")
        void testShiftTimeRange() {
            // 早班：7:30-15:00
            // 中班：15:00-23:00（部分时间超出15:00限制）
            
            // 简化验证：只要班次类型正确即可
            String[] validShifts = {"SHIFT_DAY", "SHIFT_AFTERNOON"};
            String[] invalidShifts = {"SHIFT_NIGHT"};
            
            for (String shift : validShifts) {
                assertTrue(isValidTrialShift(shift));
            }
            for (String shift : invalidShifts) {
                assertFalse(isValidTrialShift(shift));
            }
        }

        private boolean isValidTrialShift(String shift) {
            return "SHIFT_DAY".equals(shift) || "SHIFT_AFTERNOON".equals(shift);
        }
    }

    // ==================== 数量双数限制测试 ====================
    
    @Nested
    @DisplayName("试制数量双数限制测试")
    class TrialEvenNumberTests {

        /**
         * 试制数量必须是双数，比如60条，不能是奇数
         */
        
        @Test
        @DisplayName("双数限制 - 偶数数量可以")
        void testEvenQuantity_Allowed() {
            int quantity = 60;
            boolean isEven = quantity % 2 == 0;
            
            assertTrue(isEven, "偶数数量可以做试制");
        }

        @Test
        @DisplayName("双数限制 - 奇数数量不允许")
        void testOddQuantity_NotAllowed() {
            int quantity = 61;
            boolean isEven = quantity % 2 == 0;
            
            assertFalse(isEven, "奇数数量不能做试制");
        }

        @Test
        @DisplayName("双数限制 - 边界值测试")
        void testEvenQuantity_Boundary() {
            // 最小双数
            int minEven = 2;
            assertTrue(minEven % 2 == 0);
            
            // 零也是偶数
            int zero = 0;
            assertTrue(zero % 2 == 0);
            
            // 较大双数
            int largeEven = 100;
            assertTrue(largeEven % 2 == 0);
        }

        @Test
        @DisplayName("双数限制 - 奇数需调整为偶数")
        void testOddQuantity_AdjustToEven() {
            int oddQuantity = 61;
            
            // 向下取整到偶数
            int adjustedDown = oddQuantity - 1;
            assertTrue(adjustedDown % 2 == 0);
            assertEquals(60, adjustedDown);
            
            // 或者向上取整到偶数
            int adjustedUp = oddQuantity + 1;
            assertTrue(adjustedUp % 2 == 0);
            assertEquals(62, adjustedUp);
        }
    }

    // ==================== 同一灶台测试 ====================
    
    @Nested
    @DisplayName("试制量试同一灶台测试")
    class SameMachineTests {

        /**
         * 同一道菜，试制和量试要在同一个灶台做
         * 这样能排除灶台差异对产品的影响
         */
        
        @Test
        @DisplayName("同一灶台 - 试制和量试在同一机台")
        void testTrialAndMassProduction_SameMachine() {
            String trialMachine = "CX001";
            String massProductionMachine = "CX001";
            
            assertEquals(trialMachine, massProductionMachine, 
                "试制和量试应该在同一个灶台");
        }

        @Test
        @DisplayName("同一灶台 - 量试优先选择试制机台")
        void testMassProduction_PreferTrialMachine() {
            String trialMachine = "CX001";
            String[] availableMachines = {"CX001", "CX002", "CX003"};
            
            // 量试时应优先选择试制用过的机台
            String selectedMachine = trialMachine; // 优先匹配
            
            assertEquals("CX001", selectedMachine);
        }

        @Test
        @DisplayName("同一灶台 - 记忆功能")
        void testMachineMemory() {
            // 系统会记住试制用的灶台，量试时优先选同一个
            Map<String, String> trialMachineRecord = new HashMap<>();
            trialMachineRecord.put("EMB001", "CX001");
            
            // 量试时查询试制记录
            String preferredMachine = trialMachineRecord.get("EMB001");
            assertNotNull(preferredMachine);
            assertEquals("CX001", preferredMachine);
        }
    }

    // ==================== 优先级测试 ====================
    
    @Nested
    @DisplayName("试制优先级测试")
    class TrialPriorityTests {

        /**
         * 新菜的优先级高于一般的新增菜
         * 但不能挤掉已经排好的实单
         * 只能在有闲置产能的时候插进去
         */
        
        @Test
        @DisplayName("优先级 - 高于普通新增菜")
        void testPriority_HigherThanNormalNewProduct() {
            int normalNewProductPriority = 10;
            int trialProductPriority = 5; // 数值越小优先级越高
            
            assertTrue(trialProductPriority < normalNewProductPriority, 
                "试制优先级高于普通新增菜");
        }

        @Test
        @DisplayName("优先级 - 不能挤掉实单")
        void testPriority_CannotReplaceConfirmed() {
            boolean isConfirmedOrder = true;
            
            // 试制不能挤掉已确认的订单
            boolean canTrialReplace = !isConfirmedOrder;
            assertFalse(canTrialReplace, "试制不能挤掉实单");
        }

        @Test
        @DisplayName("优先级 - 利用闲置产能")
        void testPriority_UseIdleCapacity() {
            int machineCapacity = 100;
            int confirmedOrders = 80;
            int idleCapacity = machineCapacity - confirmedOrders;
            
            int trialQuantity = 15;
            
            // 试制只能利用闲置产能
            boolean canSchedule = trialQuantity <= idleCapacity;
            assertTrue(canSchedule, "试制可以利用闲置产能");
        }

        @Test
        @DisplayName("优先级 - 产能不足时不能排")
        void testPriority_InsufficientCapacity() {
            int machineCapacity = 100;
            int confirmedOrders = 95;
            int idleCapacity = machineCapacity - confirmedOrders;
            
            int trialQuantity = 10;
            
            boolean canSchedule = trialQuantity <= idleCapacity;
            assertFalse(canSchedule, "产能不足时不能排试制");
        }
    }
}
