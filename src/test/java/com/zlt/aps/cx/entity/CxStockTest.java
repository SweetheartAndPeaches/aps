package com.zlt.aps.cx.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CxStock 实体测试
 */
@DisplayName("成型库存信息测试")
class CxStockTest {

    @Test
    @DisplayName("CxStock 属性设置和获取")
    void testCxStockProperties() {
        CxStock stock = new CxStock();
        
        stock.setStockDate(new Date());
        stock.setEmbryoCode("EMB001");
        stock.setStockNum(100);
        stock.setOverTimeStock(10);
        stock.setModifyNum(5);
        stock.setBadNum(3);
        stock.setIsEndingSku(1);

        assertNotNull(stock.getStockDate());
        assertEquals("EMB001", stock.getEmbryoCode());
        assertEquals(100, stock.getStockNum());
        assertEquals(10, stock.getOverTimeStock());
        assertEquals(5, stock.getModifyNum());
        assertEquals(3, stock.getBadNum());
        assertEquals(1, stock.getIsEndingSku());
    }

    @Test
    @DisplayName("CxStock 非数据库字段")
    void testCxStockTransientFields() {
        CxStock stock = new CxStock();
        
        stock.setScheduleUseStock(50L);
        stock.setMaterialName("测试胎胚");
        stock.setAlertStatus("NORMAL");

        assertEquals(50L, stock.getScheduleUseStock());
        assertEquals("测试胎胚", stock.getMaterialName());
        assertEquals("NORMAL", stock.getAlertStatus());
    }

    @Test
    @DisplayName("CxStock 默认值")
    void testCxStockDefaultValues() {
        CxStock stock = new CxStock();
        
        // 验证默认值为null
        assertNull(stock.getStockDate());
        assertNull(stock.getEmbryoCode());
        assertNull(stock.getStockNum());
        assertNull(stock.getOverTimeStock());
        assertNull(stock.getModifyNum());
        assertNull(stock.getBadNum());
    }
}
