package com.zlt.aps.cx.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ScheduleRequest DTO 测试
 */
@DisplayName("排程请求DTO测试")
class ScheduleRequestTest {

    @Test
    @DisplayName("ScheduleRequest 属性设置和获取")
    void testScheduleRequestProperties() {
        ScheduleRequest request = new ScheduleRequest();
        
        request.setScheduleDate(LocalDate.of(2024, 7, 7));
        request.setFactoryCode("DEFAULT");
        request.setScheduleMode("NORMAL");
        request.setOverwrite(true);
        request.setScheduleType("NORMAL");

        assertEquals(LocalDate.of(2024, 7, 7), request.getScheduleDate());
        assertEquals("DEFAULT", request.getFactoryCode());
        assertEquals("NORMAL", request.getScheduleMode());
        assertTrue(request.getOverwrite());
        assertEquals("NORMAL", request.getScheduleType());
    }

    @Test
    @DisplayName("ScheduleRequest 不同排程模式")
    void testScheduleRequest_DifferentModes() {
        ScheduleRequest request = new ScheduleRequest();
        
        // 正常排程
        request.setScheduleMode("NORMAL");
        assertEquals("NORMAL", request.getScheduleMode());
        
        // 重排程
        request.setScheduleMode("RE_SCHEDULE");
        assertEquals("RE_SCHEDULE", request.getScheduleMode());
        
        // 结构重排
        request.setScheduleMode("STRUCTURE_RE_SCHEDULE");
        assertEquals("STRUCTURE_RE_SCHEDULE", request.getScheduleMode());
    }

    @Test
    @DisplayName("ScheduleRequest 不同排程类型")
    void testScheduleRequest_DifferentTypes() {
        ScheduleRequest request = new ScheduleRequest();
        
        // 正常排程
        request.setScheduleType("NORMAL");
        assertEquals("NORMAL", request.getScheduleType());
        
        // 精准排程
        request.setScheduleType("PRECISION");
        assertEquals("PRECISION", request.getScheduleType());
    }

    @Test
    @DisplayName("ScheduleRequest 序列化ID")
    void testScheduleRequest_SerialVersionUID() {
        ScheduleRequest request = new ScheduleRequest();
        // 验证对象可以正常创建
        assertNotNull(request);
    }
}
