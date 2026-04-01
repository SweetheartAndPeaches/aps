package com.zlt.aps.cx.entity.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CxShiftConfig 实体测试
 * 
 * 测试覆盖：
 * 1. 班次时间解析方法
 * 2. 辅助方法验证
 */
@DisplayName("班次配置测试")
class CxShiftConfigTest {

    @Test
    @DisplayName("班次时间解析 - 标准时间格式")
    void testShiftTimeParsing_StandardFormat() {
        CxShiftConfig config = new CxShiftConfig();
        config.setStartTime("08:00:00");
        config.setEndTime("16:00:00");

        assertEquals(LocalTime.of(8, 0), config.getShiftStartTime());
        assertEquals(LocalTime.of(16, 0), config.getShiftEndTime());
    }

    @Test
    @DisplayName("班次时间解析 - 跨班次时间")
    void testShiftTimeParsing_CrossDay() {
        CxShiftConfig config = new CxShiftConfig();
        config.setStartTime("20:00:00");
        config.setEndTime("08:00:00");

        assertEquals(LocalTime.of(20, 0), config.getShiftStartTime());
        assertEquals(LocalTime.of(8, 0), config.getShiftEndTime());
    }

    @Test
    @DisplayName("班次时间解析 - 空值默认")
    void testShiftTimeParsing_Default() {
        CxShiftConfig config = new CxShiftConfig();
        config.setStartTime(null);
        config.setEndTime(null);

        // 默认值：8:00 - 20:00
        assertEquals(LocalTime.of(8, 0), config.getShiftStartTime());
        assertEquals(LocalTime.of(20, 0), config.getShiftEndTime());
    }

    @Test
    @DisplayName("班次时间解析 - 空字符串默认")
    void testShiftTimeParsing_EmptyString() {
        CxShiftConfig config = new CxShiftConfig();
        config.setStartTime("");
        config.setEndTime("");

        assertEquals(LocalTime.of(8, 0), config.getShiftStartTime());
        assertEquals(LocalTime.of(20, 0), config.getShiftEndTime());
    }

    @Test
    @DisplayName("班次时间解析 - 无效格式默认")
    void testShiftTimeParsing_InvalidFormat() {
        CxShiftConfig config = new CxShiftConfig();
        config.setStartTime("invalid");
        config.setEndTime("invalid");

        assertEquals(LocalTime.of(8, 0), config.getShiftStartTime());
        assertEquals(LocalTime.of(20, 0), config.getShiftEndTime());
    }

    @Test
    @DisplayName("班次时间 - 获取小时和分钟")
    void testGetHourAndMinute() {
        CxShiftConfig config = new CxShiftConfig();
        config.setStartTime("08:30:00");
        config.setEndTime("17:45:00");

        assertEquals(8, config.getStartHour());
        assertEquals(30, config.getStartMinute());
        assertEquals(17, config.getEndHour());
        assertEquals(45, config.getEndMinute());
    }

    @Test
    @DisplayName("班次配置 - 所有属性")
    void testAllProperties() {
        CxShiftConfig config = new CxShiftConfig();
        config.setId(1L);
        config.setShiftCode("SHIFT_DAY");
        config.setShiftName("早班");
        config.setShiftOrder(1);
        config.setShiftHours(8);
        config.setIsCrossDay(0);
        config.setScheduleDay(1);
        config.setDayShiftOrder(1);
        config.setClassField("CLASS1");
        config.setFactoryCode("DEFAULT");
        config.setIsActive(1);

        assertEquals(1L, config.getId());
        assertEquals("SHIFT_DAY", config.getShiftCode());
        assertEquals("早班", config.getShiftName());
        assertEquals(1, config.getShiftOrder());
        assertEquals(8, config.getShiftHours());
        assertEquals(0, config.getIsCrossDay());
        assertEquals(1, config.getScheduleDay());
        assertEquals(1, config.getDayShiftOrder());
        assertEquals("CLASS1", config.getClassField());
        assertEquals("DEFAULT", config.getFactoryCode());
        assertEquals(1, config.getIsActive());
    }
}
