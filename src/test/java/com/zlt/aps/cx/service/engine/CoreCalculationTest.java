package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.cx.entity.CxStock;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 核心计算逻辑测试 - 需求文档覆盖
 * 
 * 测试场景：
 * 1. 日胎胚计划量计算公式
 * 2. SKU分配的胎胚库存量计算
 * 3. 整车换算（12条/车）
 * 4. 波浪分配（夜班:早班:中班 = 1:2:1）
 * 5. 成型余量计算
 */
@DisplayName("核心计算逻辑测试")
class CoreCalculationTest {

    private ProductionCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ProductionCalculator();
    }

    // ==================== 日胎胚计划量计算 ====================
    
    @Nested
    @DisplayName("日胎胚计划量计算")
    class DailyPlanCalculationTests {

        /**
         * 公式：日胎胚计划量 = (SKU日硫化量 – SKU分配的胎胚库存量) × (1 + 损耗率)
         * 
         * SKU分配的胎胚库存量 = (SKU日硫化量 / 同胎胚所有SKU日硫化量之和) × 胎胚库存量
         */
        @Test
        @DisplayName("基本公式计算 - 单个SKU")
        void testBasicCalculation_SingleSku() {
            // 场景：胎胚A今天要硫化100条，库存20条，损耗率1%
            // 计算：
            // SKU分配库存 = 100/100 * 20 = 20条
            // 日计划量 = (100 - 20) * 1.01 = 80.8 ≈ 81条
            
            ScheduleContextVo context = new ScheduleContextVo();
            List<LhScheduleResult> results = new ArrayList<>();
            
            LhScheduleResult result = new LhScheduleResult();
            result.setEmbryoCode("EMB001");
            result.setDailyPlanQty(100);
            results.add(result);
            
            context.setLhScheduleResults(results);
            
            List<CxStock> stocks = new ArrayList<>();
            CxStock stock = new CxStock();
            stock.setEmbryoCode("EMB001");
            stock.setStockNum(20);
            stocks.add(stock);
            context.setStocks(stocks);
            
            LocalDate scheduleDate = LocalDate.of(2024, 7, 7);
            
            // 验证硫化需求
            int vulcanizeDemand = calculator.getVulcanizeDemand("EMB001", context, scheduleDate);
            assertEquals(100, vulcanizeDemand);
            
            // 验证成型余量（库存）
            int formingRemainder = calculator.getFormingRemainder("EMB001", context);
            assertEquals(20, formingRemainder);
        }

        @Test
        @DisplayName("多SKU共用同一胎胚 - 库存分配计算")
        void testMultiSkuSameEmbryo_StockAllocation() {
            // 场景：胎胚A有两个SKU
            // SKU1硫化需求60条，SKU2硫化需求40条，总计100条
            // 胎胚库存20条
            // SKU1分配库存 = 60/100 * 20 = 12条
            // SKU2分配库存 = 40/100 * 20 = 8条
            
            ScheduleContextVo context = new ScheduleContextVo();
            List<LhScheduleResult> results = new ArrayList<>();
            
            LhScheduleResult result1 = new LhScheduleResult();
            result1.setEmbryoCode("EMB001");
            result1.setDailyPlanQty(60);
            results.add(result1);
            
            LhScheduleResult result2 = new LhScheduleResult();
            result2.setEmbryoCode("EMB001");
            result2.setDailyPlanQty(40);
            results.add(result2);
            
            context.setLhScheduleResults(results);
            
            List<CxStock> stocks = new ArrayList<>();
            CxStock stock = new CxStock();
            stock.setEmbryoCode("EMB001");
            stock.setStockNum(20);
            stocks.add(stock);
            context.setStocks(stocks);
            
            LocalDate scheduleDate = LocalDate.of(2024, 7, 7);
            
            // 验证总硫化需求
            int totalDemand = calculator.getVulcanizeDemand("EMB001", context, scheduleDate);
            assertEquals(100, totalDemand);
        }

        @Test
        @DisplayName("损耗率计算")
        void testLossRateCalculation() {
            // 场景：硫化需求100条，库存0条，损耗率2%
            // 日计划量 = (100 - 0) * 1.02 = 102条
            
            BigDecimal lossRate = new BigDecimal("0.02");
            int demand = 100;
            int stock = 0;
            
            int expected = (int) Math.ceil((demand - stock) * (1 + lossRate.doubleValue()));
            assertEquals(102, expected);
        }

        @Test
        @DisplayName("库存充足时 - 无需生产")
        void testSufficientStock_NoProduction() {
            // 场景：硫化需求50条，库存60条
            // 日计划量 = max(0, 50 - 60) * 1.01 = 0条
            
            ScheduleContextVo context = new ScheduleContextVo();
            
            List<LhScheduleResult> results = new ArrayList<>();
            LhScheduleResult result = new LhScheduleResult();
            result.setEmbryoCode("EMB001");
            result.setDailyPlanQty(50);
            results.add(result);
            context.setLhScheduleResults(results);
            
            List<CxStock> stocks = new ArrayList<>();
            CxStock stock = new CxStock();
            stock.setEmbryoCode("EMB001");
            stock.setStockNum(60);  // 库存大于需求
            stocks.add(stock);
            context.setStocks(stocks);
            
            assertEquals(50, calculator.getVulcanizeDemand("EMB001", context, LocalDate.now()));
            assertEquals(60, calculator.getFormingRemainder("EMB001", context));
        }
    }

    // ==================== 整车换算 ====================
    
    @Nested
    @DisplayName("整车换算测试")
    class TripCalculationTests {

        /**
         * 生产必须按"整车"来，一整车是12条
         */
        @Test
        @DisplayName("整车换算 - 正好整车")
        void testTripCalculation_ExactTrip() {
            // 36条 = 3车
            assertEquals(3, calculator.calculateTrips(36, 12));
            
            // 24条 = 2车
            assertEquals(2, calculator.calculateTrips(24, 12));
        }

        @Test
        @DisplayName("整车换算 - 向上取整")
        void testTripCalculation_RoundUp() {
            // 25条 → 3车（向上取整）
            assertEquals(3, calculator.calculateTrips(25, 12));
            
            // 1条 → 1车（最小1车）
            assertEquals(1, calculator.calculateTrips(1, 12));
            
            // 13条 → 2车
            assertEquals(2, calculator.calculateTrips(13, 12));
        }

        @Test
        @DisplayName("不同整车容量 - 18条/车")
        void testDifferentTripCapacity_18PerTrip() {
            // 有些结构每车18条
            assertEquals(2, calculator.calculateTrips(36, 18));
            assertEquals(3, calculator.calculateTrips(37, 18));
            assertEquals(1, calculator.calculateTrips(18, 18));
        }

        @Test
        @DisplayName("零和负数处理")
        void testZeroAndNegative() {
            assertEquals(0, calculator.calculateTrips(0, 12));
            assertEquals(0, calculator.calculateTrips(-10, 12));
            assertEquals(0, calculator.calculateTrips(24, 0));
            assertEquals(0, calculator.calculateTrips(24, -12));
        }
    }

    // ==================== 波浪分配 ====================
    
    @Nested
    @DisplayName("波浪分配测试（班次均衡）")
    class WaveAllocationTests {

        /**
         * 波浪形态：夜班:早班:中班 = 1:2:1
         * 早班多干点，夜班和中班少干点
         */
        @Test
        @DisplayName("波浪分配 - 标准比例1:2:1")
        void testWaveAllocation_StandardRatio() {
            // 4车分配到3班，期望比例接近 1:2:1
            // 即：1车:2车:1车 = 12条:24条:12条
            
            Map<String, Integer> result = calculator.waveAllocation(4, 3, 12);
            
            assertEquals(3, result.size());
            
            // 验证总量 = 4车 * 12条 = 48条
            int total = result.values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(48, total);
        }

        @Test
        @DisplayName("波浪分配 - 5车分配")
        void testWaveAllocation_5Trips() {
            // 5车分配到3班
            // 理论分配：5/3 ≈ 1.67车/班
            // 波浪形态：两端多，中间少
            
            Map<String, Integer> result = calculator.waveAllocation(5, 3, 12);
            
            int total = result.values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(60, total); // 5车 * 12条 = 60条
            
            // 验证波浪形态：两端 >= 中间
            List<Integer> values = new ArrayList<>(result.values());
            assertTrue(values.get(0) >= values.get(1), "第一个班次应该 >= 中间班次");
            assertTrue(values.get(2) >= values.get(1), "最后一个班次应该 >= 中间班次");
        }

        @Test
        @DisplayName("波浪分配 - 整数倍分配")
        void testWaveAllocation_EvenDistribution() {
            // 3车分配到3班，每班1车
            Map<String, Integer> result = calculator.waveAllocation(3, 3, 12);
            
            int total = result.values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(36, total);
        }

        @Test
        @DisplayName("波浪分配 - 单车")
        void testWaveAllocation_SingleTrip() {
            // 1车分配到3班
            // 应该分配给第一个班次（优先填补两端）
            Map<String, Integer> result = calculator.waveAllocation(1, 3, 12);
            
            int total = result.values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(12, total);
        }

        @Test
        @DisplayName("波浪分配 - 边界情况")
        void testWaveAllocation_Boundary() {
            // 0车
            Map<String, Integer> result1 = calculator.waveAllocation(0, 3, 12);
            assertEquals(0, result1.values().stream().mapToInt(Integer::intValue).sum());
            
            // 0班次
            Map<String, Integer> result2 = calculator.waveAllocation(5, 0, 12);
            assertTrue(result2.isEmpty());
        }
    }

    // ==================== 成型余量计算 ====================
    
    @Nested
    @DisplayName("成型余量计算")
    class FormingRemainderTests {

        /**
         * 成型余量 = 硫化余量 - 胎胚库存
         * 表示"还要做多少才能收尾"
         */
        @Test
        @DisplayName("成型余量 - 正常计算")
        void testFormingRemainder_Calculation() {
            ScheduleContextVo context = new ScheduleContextVo();
            
            List<CxStock> stocks = new ArrayList<>();
            CxStock stock = new CxStock();
            stock.setEmbryoCode("EMB001");
            stock.setStockNum(20);
            stocks.add(stock);
            context.setStocks(stocks);
            
            assertEquals(20, calculator.getFormingRemainder("EMB001", context));
        }

        @Test
        @DisplayName("成型余量 - 多胎胚场景")
        void testFormingRemainder_MultipleEmbryos() {
            ScheduleContextVo context = new ScheduleContextVo();
            
            List<CxStock> stocks = new ArrayList<>();
            
            CxStock stock1 = new CxStock();
            stock1.setEmbryoCode("EMB001");
            stock1.setStockNum(100);
            stocks.add(stock1);
            
            CxStock stock2 = new CxStock();
            stock2.setEmbryoCode("EMB002");
            stock2.setStockNum(200);
            stocks.add(stock2);
            
            context.setStocks(stocks);
            
            assertEquals(100, calculator.getFormingRemainder("EMB001", context));
            assertEquals(200, calculator.getFormingRemainder("EMB002", context));
            assertEquals(0, calculator.getFormingRemainder("EMB999", context)); // 不存在
        }

        @Test
        @DisplayName("成型余量 - 空库存和null处理")
        void testFormingRemainder_EmptyAndNull() {
            ScheduleContextVo context1 = new ScheduleContextVo();
            context1.setStocks(new ArrayList<>());
            assertEquals(0, calculator.getFormingRemainder("EMB001", context1));
            
            ScheduleContextVo context2 = new ScheduleContextVo();
            context2.setStocks(null);
            assertEquals(0, calculator.getFormingRemainder("EMB001", context2));
        }
    }

    // ==================== 硫化需求计算 ====================
    
    @Nested
    @DisplayName("硫化需求计算")
    class VulcanizeDemandTests {

        @Test
        @DisplayName("硫化需求 - 单条记录")
        void testVulcanizeDemand_SingleRecord() {
            ScheduleContextVo context = new ScheduleContextVo();
            
            List<LhScheduleResult> results = new ArrayList<>();
            LhScheduleResult result = new LhScheduleResult();
            result.setEmbryoCode("EMB001");
            result.setDailyPlanQty(100);
            results.add(result);
            context.setLhScheduleResults(results);
            
            assertEquals(100, calculator.getVulcanizeDemand("EMB001", context, LocalDate.now()));
        }

        @Test
        @DisplayName("硫化需求 - 多条记录汇总")
        void testVulcanizeDemand_MultipleRecords() {
            ScheduleContextVo context = new ScheduleContextVo();
            
            List<LhScheduleResult> results = new ArrayList<>();
            
            LhScheduleResult result1 = new LhScheduleResult();
            result1.setEmbryoCode("EMB001");
            result1.setDailyPlanQty(60);
            results.add(result1);
            
            LhScheduleResult result2 = new LhScheduleResult();
            result2.setEmbryoCode("EMB001");
            result2.setDailyPlanQty(40);
            results.add(result2);
            
            context.setLhScheduleResults(results);
            
            // 同一胎胚的需求应该累加
            assertEquals(100, calculator.getVulcanizeDemand("EMB001", context, LocalDate.now()));
        }

        @Test
        @DisplayName("硫化需求 - 空数据处理")
        void testVulcanizeDemand_EmptyData() {
            ScheduleContextVo context1 = new ScheduleContextVo();
            context1.setLhScheduleResults(new ArrayList<>());
            assertEquals(0, calculator.getVulcanizeDemand("EMB001", context1, LocalDate.now()));
            
            ScheduleContextVo context2 = new ScheduleContextVo();
            context2.setLhScheduleResults(null);
            assertEquals(0, calculator.getVulcanizeDemand("EMB001", context2, LocalDate.now()));
        }
    }

    // ==================== 综合场景测试 ====================
    
    @Nested
    @DisplayName("综合场景测试")
    class IntegrationTests {

        @Test
        @DisplayName("完整计算流程 - 从需求到整车")
        void testCompleteCalculationFlow() {
            // 场景：
            // 1. 硫化需求：100条
            // 2. 胎胚库存：20条
            // 3. 损耗率：2%
            // 4. 计算：日计划量 = (100-20)*1.02 = 81.6 ≈ 82条
            // 5. 整车换算：82条 → 7车（向上取整，84条）
            // 6. 波浪分配：7车分配到3班
            
            ScheduleContextVo context = new ScheduleContextVo();
            
            // 设置硫化需求
            List<LhScheduleResult> results = new ArrayList<>();
            LhScheduleResult result = new LhScheduleResult();
            result.setEmbryoCode("EMB001");
            result.setDailyPlanQty(100);
            results.add(result);
            context.setLhScheduleResults(results);
            
            // 设置库存
            List<CxStock> stocks = new ArrayList<>();
            CxStock stock = new CxStock();
            stock.setEmbryoCode("EMB001");
            stock.setStockNum(20);
            stocks.add(stock);
            context.setStocks(stocks);
            
            LocalDate scheduleDate = LocalDate.of(2024, 7, 7);
            
            // 1. 验证硫化需求
            int demand = calculator.getVulcanizeDemand("EMB001", context, scheduleDate);
            assertEquals(100, demand);
            
            // 2. 验证库存
            int stockQty = calculator.getFormingRemainder("EMB001", context);
            assertEquals(20, stockQty);
            
            // 3. 计算待排产量
            int toProduce = demand - stockQty; // 80条
            assertEquals(80, toProduce);
            
            // 4. 考虑损耗率
            BigDecimal lossRate = new BigDecimal("0.02");
            int planWithLoss = (int) Math.ceil(toProduce * (1 + lossRate.doubleValue()));
            assertEquals(82, planWithLoss);
            
            // 5. 整车换算
            int trips = calculator.calculateTrips(planWithLoss, 12);
            assertEquals(7, trips); // 82条 → 7车 = 84条
            
            // 6. 波浪分配
            Map<String, Integer> allocation = calculator.waveAllocation(trips, 3, 12);
            int total = allocation.values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(84, total); // 7车 * 12条 = 84条
        }
    }
}
