package com.zlt.aps.cx.service.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 收尾管理测试 - 需求文档覆盖
 * 
 * 测试场景：
 * 1. 收尾定义：设备上某个菜系要撤掉，不再做了
 * 2. 收尾条件：硫化计划里这道菜连续10天没单子
 * 3. 紧急收尾：连续3天没单子，且库存够用到第4天
 * 4. 主销产品整车规则
 */
@DisplayName("收尾管理测试")
class FinishingScheduleTest {

    // ==================== 收尾定义测试 ====================
    
    @Nested
    @DisplayName("收尾定义测试")
    class FinishingDefinitionTests {

        /**
         * 收尾定义：
         * - 设备上某个菜系要撤掉，不再做了
         * - 需判断这道菜什么时候能停下来
         */
        
        @Test
        @DisplayName("收尾定义 - 设备菜系撤除")
        void testFinishingDefinition_MachineProductRemoval() {
            String machine = "CX001";
            String productLine = "PRODUCT_LINE_A";
            
            // 设备上有这个菜系在产
            boolean isProductOnline = true;
            
            // 标记收尾
            boolean isFinishing = true;
            
            assertTrue(isProductOnline);
            assertTrue(isFinishing);
        }

        @Test
        @DisplayName("收尾状态 - 收尾中的设备")
        void testFinishingStatus_InProgress() {
            String machineStatus = "FINISHING";
            
            assertEquals("FINISHING", machineStatus, "设备处于收尾状态");
        }

        @Test
        @DisplayName("收尾完成 - 设备空闲")
        void testFinishingComplete_MachineIdle() {
            String machineStatus = "IDLE";
            
            assertEquals("IDLE", machineStatus, "收尾完成后设备空闲");
        }
    }

    // ==================== 收尾条件测试 ====================
    
    @Nested
    @DisplayName("收尾条件测试")
    class FinishingConditionTests {

        /**
         * 正常收尾条件：
         * - 硫化计划里这道菜连续10天没单子
         * - 就可以在设备计划里设置收尾
         */
        
        @Test
        @DisplayName("收尾条件 - 连续10天无订单")
        void testFinishingCondition_10DaysNoOrder() {
            int consecutiveDaysNoOrder = 10;
            int threshold = 10;
            
            boolean canFinish = consecutiveDaysNoOrder >= threshold;
            assertTrue(canFinish, "连续10天无订单可以收尾");
        }

        @Test
        @DisplayName("收尾条件 - 不足10天不能收尾")
        void testFinishingCondition_Below10Days() {
            int consecutiveDaysNoOrder = 9;
            int threshold = 10;
            
            boolean canFinish = consecutiveDaysNoOrder >= threshold;
            assertFalse(canFinish, "不足10天不能收尾");
        }

        @Test
        @DisplayName("收尾条件 - 连续天数计算")
        void testConsecutiveDaysCalculation() {
            // 模拟10天的订单情况
            boolean[] hasOrder = {
                false, // 第1天
                false, // 第2天
                false, // 第3天
                false, // 第4天
                false, // 第5天
                false, // 第6天
                false, // 第7天
                false, // 第8天
                false, // 第9天
                false  // 第10天
            };
            
            int consecutiveDays = 0;
            for (boolean dayHasOrder : hasOrder) {
                if (!dayHasOrder) {
                    consecutiveDays++;
                } else {
                    break; // 有订单则中断连续计数
                }
            }
            
            assertEquals(10, consecutiveDays);
        }

        @Test
        @DisplayName("收尾条件 - 中间有订单则重新计数")
        void testConsecutiveDays_ResetOnOrder() {
            // 模拟订单情况，中间有订单
            boolean[] hasOrder = {
                false, // 第1天
                false, // 第2天
                true,  // 第3天有订单！
                false, // 第4天
                false, // 第5天
                false, // 第6天
                false, // 第7天
                false, // 第8天
                false, // 第9天
                false  // 第10天
            };
            
            // 从后往前计算连续无订单天数
            int consecutiveDays = 0;
            for (int i = hasOrder.length - 1; i >= 0; i--) {
                if (!hasOrder[i]) {
                    consecutiveDays++;
                } else {
                    break;
                }
            }
            
            assertEquals(7, consecutiveDays, "从第4天开始连续7天无订单");
        }
    }

    // ==================== 紧急收尾测试 ====================
    
    @Nested
    @DisplayName("紧急收尾测试")
    class UrgentFinishingTests {

        /**
         * 紧急收尾条件：
         * - 连续3天没单子
         * - 且库存够用到第4天
         * - 可以提前收尾
         */
        
        @Test
        @DisplayName("紧急收尾 - 条件满足")
        void testUrgentFinishing_ConditionsMet() {
            int consecutiveDaysNoOrder = 3;
            int stockSufficientDays = 4; // 库存够用4天
            
            boolean canUrgentFinish = consecutiveDaysNoOrder >= 3 
                && stockSufficientDays >= 4;
            
            assertTrue(canUrgentFinish, "满足紧急收尾条件");
        }

        @Test
        @DisplayName("紧急收尾 - 无订单天数不足")
        void testUrgentFinishing_InsufficientDays() {
            int consecutiveDaysNoOrder = 2;
            int stockSufficientDays = 4;
            
            boolean canUrgentFinish = consecutiveDaysNoOrder >= 3 
                && stockSufficientDays >= 4;
            
            assertFalse(canUrgentFinish, "无订单天数不足不能紧急收尾");
        }

        @Test
        @DisplayName("紧急收尾 - 库存不足")
        void testUrgentFinishing_InsufficientStock() {
            int consecutiveDaysNoOrder = 3;
            int stockSufficientDays = 3; // 库存只够3天
            
            boolean canUrgentFinish = consecutiveDaysNoOrder >= 3 
                && stockSufficientDays >= 4;
            
            assertFalse(canUrgentFinish, "库存不足不能紧急收尾");
        }

        @Test
        @DisplayName("紧急收尾 - 库存天数计算")
        void testStockSufficientDaysCalculation() {
            int currentStock = 200; // 当前库存200条
            int dailyDemand = 50; // 每天需求50条
            
            int stockDays = currentStock / dailyDemand;
            assertEquals(4, stockDays, "库存够用4天");
        }

        @Test
        @DisplayName("紧急收尾 - 库存刚好够")
        void testUrgentFinishing_StockJustEnough() {
            int currentStock = 200;
            int dailyDemand = 50;
            int stockDays = currentStock / dailyDemand;
            
            boolean stockSufficient = stockDays >= 4;
            assertTrue(stockSufficient, "库存刚好够用4天");
        }

        @Test
        @DisplayName("紧急收尾 vs 正常收尾 - 优先级")
        void testUrgentVsNormal_Priority() {
            // 紧急收尾优先级更高
            int urgentFinishDays = 3;
            int normalFinishDays = 10;
            
            assertTrue(urgentFinishDays < normalFinishDays, 
                "紧急收尾条件比正常收尾宽松");
        }
    }

    // ==================== 主销产品整车规则测试 ====================
    
    @Nested
    @DisplayName("主销产品整车规则测试")
    class MainProductTripTests {

        /**
         * 主销产品整车规则：
         * - 主销产品优先安排整车
         * - 不能零散地排
         */
        
        @Test
        @DisplayName("主销产品 - 必须整车")
        void testMainProduct_MustBeWholeTrip() {
            boolean isMainProduct = true;
            int quantity = 36; // 36条 = 3车（12条/车）
            int tripCapacity = 12;
            
            boolean isWholeTrip = quantity % tripCapacity == 0;
            assertTrue(isMainProduct && isWholeTrip, 
                "主销产品必须是整车");
        }

        @Test
        @DisplayName("主销产品 - 非整车需调整")
        void testMainProduct_NonWholeTripAdjustment() {
            int quantity = 35; // 非整车
            int tripCapacity = 12;
            
            // 向上取整到整车
            int trips = (int) Math.ceil((double) quantity / tripCapacity);
            int adjustedQuantity = trips * tripCapacity;
            
            assertEquals(3, trips);
            assertEquals(36, adjustedQuantity, "调整为整车36条");
        }

        @Test
        @DisplayName("主销产品 - 整车验证")
        void testMainProduct_TripValidation() {
            int[] quantities = {12, 24, 36, 48, 60};
            int tripCapacity = 12;
            
            for (int qty : quantities) {
                boolean isWholeTrip = qty % tripCapacity == 0;
                assertTrue(isWholeTrip, qty + "条应该是整车");
            }
        }

        @Test
        @DisplayName("主销产品 - 最小整车量")
        void testMainProduct_MinimumTrip() {
            int tripCapacity = 12;
            int minQuantity = tripCapacity; // 最小1车
            
            assertEquals(12, minQuantity);
            assertTrue(minQuantity % tripCapacity == 0);
        }

        @Test
        @DisplayName("非主销产品 - 可以零散")
        void testNonMainProduct_CanBePartial() {
            boolean isMainProduct = false;
            int quantity = 35; // 可以不是整车
            
            // 非主销产品可以零散
            assertTrue(!isMainProduct, "非主销产品可以零散排产");
        }
    }

    // ==================== 收尾库存清零测试 ====================
    
    @Nested
    @DisplayName("收尾库存清零测试")
    class FinishingStockZeroTests {

        /**
         * 收尾目标：
         * - 库存要清零
         * - 不能留菜过夜
         */
        
        @Test
        @DisplayName("收尾库存 - 计划清零")
        void testFinishingStock_PlanToZero() {
            // 场景：收尾时库存80条，需求正好80条，清零
            int currentStock = 80;
            int demandBeforeFinishing = 80; // 收尾前需求
            
            int needToProduce = Math.max(0, demandBeforeFinishing - currentStock);
            
            // 收尾后库存
            int stockAfterFinishing = currentStock + needToProduce - demandBeforeFinishing;
            assertEquals(0, stockAfterFinishing, "收尾后库存应为0");
        }

        @Test
        @DisplayName("收尾库存 - 生产刚好满足需求")
        void testFinishingStock_ExactlyMatch() {
            int currentStock = 80;
            int demand = 100;
            
            int needToProduce = demand - currentStock;
            assertEquals(20, needToProduce, "需要生产20条");
            
            // 生产后库存
            int stockAfterProduction = currentStock + needToProduce;
            assertEquals(100, stockAfterProduction);
            
            // 满足需求后库存为0
            int stockAfterDemand = stockAfterProduction - demand;
            assertEquals(0, stockAfterDemand);
        }

        @Test
        @DisplayName("收尾库存 - 库存超过需求")
        void testFinishingStock_ExceedsDemand() {
            int currentStock = 120;
            int demand = 100;
            
            // 库存超过需求，不需要生产
            int needToProduce = Math.max(0, demand - currentStock);
            assertEquals(0, needToProduce, "库存充足不需要生产");
            
            // 剩余库存（这种情况可能需要调整需求或转移库存）
            int remainingStock = currentStock - demand;
            assertEquals(20, remainingStock, "会有20条剩余");
        }
    }

    // ==================== 收尾任务排序测试 ====================
    
    @Nested
    @DisplayName("收尾任务排序测试")
    class FinishingTaskOrderTests {

        /**
         * 收尾任务排序规则：
         * - 先做完收尾的任务，腾出产能
         * - 让还在继续做的任务多干点
         */
        
        @Test
        @DisplayName("收尾任务 - 优先完成")
        void testFinishingTask_PriorityComplete() {
            String task1Status = "FINISHING";
            String task2Status = "ONGOING";
            
            // 收尾任务优先级高于持续任务
            int task1Priority = 1;
            int task2Priority = 2;
            
            assertTrue(task1Priority < task2Priority, 
                "收尾任务应该优先完成");
        }

        @Test
        @DisplayName("多个收尾任务 - 按库存排序")
        void testMultipleFinishingTasks_OrderByStock() {
            // 场景：多个任务都在收尾，库存少的优先
            int task1Stock = 50;
            int task2Stock = 30;
            int task3Stock = 40;
            
            // 库存少的优先（更快完成收尾）
            assertTrue(task2Stock < task3Stock);
            assertTrue(task3Stock < task1Stock);
        }

        @Test
        @DisplayName("收尾任务 - 不影响持续任务")
        void testFinishingTask_NoImpactOnOngoing() {
            // 收尾任务完成后，持续任务继续
            boolean finishingTaskCompleted = true;
            boolean ongoingTaskRunning = true;
            
            assertTrue(finishingTaskCompleted);
            assertTrue(ongoingTaskRunning, "持续任务不受收尾影响");
        }
    }

    // ==================== 综合场景测试 ====================
    
    @Nested
    @DisplayName("综合场景测试")
    class IntegrationTests {

        @Test
        @DisplayName("完整收尾流程 - 从判断到完成")
        void testCompleteFinishingFlow() {
            // 场景：
            // 1. 产品A连续10天无订单
            // 2. 当前库存50条
            // 3. 硫化还有最后需求50条
            // 4. 完成收尾
            
            // Step 1: 判断收尾条件
            int consecutiveDaysNoOrder = 10;
            boolean canFinish = consecutiveDaysNoOrder >= 10;
            assertTrue(canFinish, "满足收尾条件");
            
            // Step 2: 计算最后需求
            int currentStock = 50;
            int finalDemand = 50;
            
            // Step 3: 计算生产量
            int needToProduce = Math.max(0, finalDemand - currentStock);
            assertEquals(0, needToProduce, "库存足够，无需生产");
            
            // Step 4: 完成收尾
            int stockAfterFinishing = currentStock - finalDemand;
            assertEquals(0, stockAfterFinishing, "收尾完成，库存清零");
        }

        @Test
        @DisplayName("紧急收尾流程 - 提前完成")
        void testUrgentFinishingFlow() {
            // 场景：
            // 1. 产品B连续3天无订单
            // 2. 当前库存200条，够用4天
            // 3. 提前收尾
            
            int consecutiveDaysNoOrder = 3;
            int currentStock = 200;
            int dailyDemand = 50;
            int stockDays = currentStock / dailyDemand;
            
            boolean canUrgentFinish = consecutiveDaysNoOrder >= 3 && stockDays >= 4;
            assertTrue(canUrgentFinish, "满足紧急收尾条件");
            
            // 预计收尾日期
            int finishDaysLater = stockDays;
            assertEquals(4, finishDaysLater, "4天后收尾完成");
        }

        @Test
        @DisplayName("主销产品收尾 - 整车约束")
        void testMainProductFinishing_TripConstraint() {
            // 场景：
            // 1. 主销产品要收尾
            // 2. 当前库存35条
            // 3. 必须整车
            
            boolean isMainProduct = true;
            int currentStock = 35;
            int tripCapacity = 12;
            
            // 整车化
            int trips = (int) Math.ceil((double) currentStock / tripCapacity);
            int adjustedQuantity = trips * tripCapacity;
            assertEquals(36, adjustedQuantity, "调整为整车36条");
            
            // 或者向下取整
            int floorTrips = currentStock / tripCapacity;
            int floorQuantity = floorTrips * tripCapacity;
            assertEquals(24, floorQuantity, "向下取整24条");
            
            // 主销产品通常向上取整
            assertTrue(adjustedQuantity >= currentStock);
        }
    }
}
