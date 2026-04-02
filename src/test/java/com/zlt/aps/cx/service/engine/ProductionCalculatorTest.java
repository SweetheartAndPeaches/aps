package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.cx.entity.CxStock;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProductionCalculator 单元测试
 * 
 * 测试覆盖：
 * 1. 计算车次 (calculateTrips)
 * 2. 波浪分配到班次 (waveAllocation)
 * 3. 获取成型余量 (getFormingRemainder)
 * 4. 获取硫化需求 (getVulcanizeDemand)
 */
@DisplayName("生产计算器测试")
class ProductionCalculatorTest {

    private ProductionCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ProductionCalculator();
    }

    // ==================== calculateTrips 测试 ====================

    @Test
    @DisplayName("计算车次 - 正常情况")
    void testCalculateTrips_Normal() {
        // 24条，每车12条 → 2车
        assertEquals(2, calculator.calculateTrips(24, 12));
        
        // 25条，每车12条 → 3车（向上取整）
        assertEquals(3, calculator.calculateTrips(25, 12));
        
        // 36条，每车12条 → 3车
        assertEquals(3, calculator.calculateTrips(36, 12));
    }

    @Test
    @DisplayName("计算车次 - 边界情况")
    void testCalculateTrips_Boundary() {
        // 零条
        assertEquals(0, calculator.calculateTrips(0, 12));
        
        // 负数
        assertEquals(0, calculator.calculateTrips(-10, 12));
        
        // 每车容量为0
        assertEquals(0, calculator.calculateTrips(24, 0));
        
        // 每车容量为负
        assertEquals(0, calculator.calculateTrips(24, -12));
        
        // 1条，每车12条 → 1车
        assertEquals(1, calculator.calculateTrips(1, 12));
    }

    @Test
    @DisplayName("计算车次 - 不同整车容量")
    void testCalculateTrips_DifferentTripCapacity() {
        // 每车18条
        assertEquals(2, calculator.calculateTrips(36, 18));
        assertEquals(3, calculator.calculateTrips(37, 18));
        
        // 每车10条
        assertEquals(3, calculator.calculateTrips(25, 10));
    }

    // ==================== waveAllocation 测试 ====================

    @Test
    @DisplayName("波浪分配 - 正常分配")
    void testWaveAllocation_Normal() {
        // 5车分配到3班，每车12条
        Map<String, Integer> result = calculator.waveAllocation(5, 3, 12);
        
        assertEquals(3, result.size());
        
        // 验证总量
        int total = result.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(60, total); // 5车 * 12条 = 60条
    }

    @Test
    @DisplayName("波浪分配 - 平均分配")
    void testWaveAllocation_EvenDistribution() {
        // 3车分配到3班，每班1车
        Map<String, Integer> result = calculator.waveAllocation(3, 3, 12);
        
        int total = result.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(36, total); // 3车 * 12条 = 36条
    }

    @Test
    @DisplayName("波浪分配 - 边界情况")
    void testWaveAllocation_Boundary() {
        // 0车
        Map<String, Integer> result1 = calculator.waveAllocation(0, 3, 12);
        assertEquals(3, result1.size());
        result1.values().forEach(v -> assertEquals(0, v));
        
        // 0班次
        Map<String, Integer> result2 = calculator.waveAllocation(5, 0, 12);
        assertEquals(0, result2.size());
    }

    @Test
    @DisplayName("波浪分配 - 波浪形态验证")
    void testWaveAllocation_WavePattern() {
        // 5车分配到3班 → 应该形成 2-1-2 的波浪形态
        Map<String, Integer> result = calculator.waveAllocation(5, 3, 12);
        
        List<Integer> values = new ArrayList<>(result.values());
        
        // 验证波浪形态：两端多，中间少
        // 第一个班次 >= 中间班次
        assertTrue(values.get(0) >= values.get(1));
        // 最后一个班次 >= 中间班次
        assertTrue(values.get(2) >= values.get(1));
    }

    // ==================== getFormingRemainder 测试 ====================

    @Test
    @DisplayName("获取成型余量 - 正常情况")
    void testGetFormingRemainder_Normal() {
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
    }

    @Test
    @DisplayName("获取成型余量 - 不存在的胎胚")
    void testGetFormingRemainder_NotFound() {
        ScheduleContextVo context = new ScheduleContextVo();
        List<CxStock> stocks = new ArrayList<>();
        
        CxStock stock = new CxStock();
        stock.setEmbryoCode("EMB001");
        stock.setStockNum(100);
        stocks.add(stock);
        
        context.setStocks(stocks);
        
        assertEquals(0, calculator.getFormingRemainder("EMB999", context));
    }

    @Test
    @DisplayName("获取成型余量 - 空库存")
    void testGetFormingRemainder_EmptyStock() {
        ScheduleContextVo context = new ScheduleContextVo();
        context.setStocks(new ArrayList<>());
        
        assertEquals(0, calculator.getFormingRemainder("EMB001", context));
    }

    @Test
    @DisplayName("获取成型余量 - null库存")
    void testGetFormingRemainder_NullStock() {
        ScheduleContextVo context = new ScheduleContextVo();
        context.setStocks(null);
        
        assertEquals(0, calculator.getFormingRemainder("EMB001", context));
    }

    @Test
    @DisplayName("获取成型余量 - 负数库存处理")
    void testGetFormingRemainder_NegativeStock() {
        ScheduleContextVo context = new ScheduleContextVo();
        List<CxStock> stocks = new ArrayList<>();
        
        CxStock stock = new CxStock();
        stock.setEmbryoCode("EMB001");
        stock.setStockNum(-50);  // 负数
        stocks.add(stock);
        
        context.setStocks(stocks);
        
        // 负数应该返回0
        assertEquals(0, calculator.getFormingRemainder("EMB001", context));
    }

    @Test
    @DisplayName("获取成型余量 - null库存数量")
    void testGetFormingRemainder_NullQuantity() {
        ScheduleContextVo context = new ScheduleContextVo();
        List<CxStock> stocks = new ArrayList<>();
        
        CxStock stock = new CxStock();
        stock.setEmbryoCode("EMB001");
        stock.setStockNum(null);  // null
        stocks.add(stock);
        
        context.setStocks(stocks);
        
        assertEquals(0, calculator.getFormingRemainder("EMB001", context));
    }

    // ==================== getVulcanizeDemand 测试 ====================

    @Test
    @DisplayName("获取硫化需求 - 正常情况")
    void testGetVulcanizeDemand_Normal() {
        ScheduleContextVo context = new ScheduleContextVo();
        List<LhScheduleResult> results = new ArrayList<>();
        
        LhScheduleResult result1 = new LhScheduleResult();
        result1.setEmbryoCode("EMB001");
        result1.setDailyPlanQty(50);
        results.add(result1);
        
        LhScheduleResult result2 = new LhScheduleResult();
        result2.setEmbryoCode("EMB001");
        result2.setDailyPlanQty(30);
        results.add(result2);
        
        LhScheduleResult result3 = new LhScheduleResult();
        result3.setEmbryoCode("EMB002");
        result3.setDailyPlanQty(40);
        results.add(result3);
        
        context.setLhScheduleResults(results);
        
        LocalDate scheduleDate = LocalDate.of(2024, 7, 7);
        
        // EMB001: 50 + 30 = 80
        assertEquals(80, calculator.getVulcanizeDemand("EMB001", context, scheduleDate));
        // EMB002: 40
        assertEquals(40, calculator.getVulcanizeDemand("EMB002", context, scheduleDate));
    }

    @Test
    @DisplayName("获取硫化需求 - 空数据")
    void testGetVulcanizeDemand_EmptyData() {
        ScheduleContextVo context = new ScheduleContextVo();
        context.setLhScheduleResults(new ArrayList<>());
        
        LocalDate scheduleDate = LocalDate.of(2024, 7, 7);
        assertEquals(0, calculator.getVulcanizeDemand("EMB001", context, scheduleDate));
    }

    @Test
    @DisplayName("获取硫化需求 - null数据")
    void testGetVulcanizeDemand_NullData() {
        ScheduleContextVo context = new ScheduleContextVo();
        context.setLhScheduleResults(null);
        
        LocalDate scheduleDate = LocalDate.of(2024, 7, 7);
        assertEquals(0, calculator.getVulcanizeDemand("EMB001", context, scheduleDate));
    }

    // ==================== 常量验证测试 ====================

    @Test
    @DisplayName("验证默认常量值")
    void testDefaultConstants() {
        assertEquals(12, ProductionCalculator.DEFAULT_TRIP_CAPACITY);
        assertEquals(4, ProductionCalculator.DEFAULT_MAX_TYPES_PER_MACHINE);
        assertEquals(new java.math.BigDecimal("0.02"), ProductionCalculator.DEFAULT_LOSS_RATE);
        assertEquals(50, ProductionCalculator.DEFAULT_HOURLY_CAPACITY);
        assertEquals(1200, ProductionCalculator.DEFAULT_DAILY_CAPACITY);
        assertEquals(6, ProductionCalculator.OPENING_SHIFT_HOURS);
    }
}
