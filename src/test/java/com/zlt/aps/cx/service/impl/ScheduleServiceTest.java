package com.zlt.aps.cx.service.impl;

import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.dto.ScheduleRequest;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.service.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ScheduleService 单元测试（不依赖Spring上下文）
 * 
 * 测试覆盖：
 * 1. 排程请求参数验证
 * 2. 排程结果验证
 */
@DisplayName("排程服务测试")
class ScheduleServiceTest {

    @Test
    @DisplayName("排程请求 - 参数构建")
    void testScheduleRequest_Builder() {
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
    @DisplayName("排程请求 - 默认值")
    void testScheduleRequest_DefaultValues() {
        ScheduleRequest request = new ScheduleRequest();
        
        // 验证默认值为null
        assertNull(request.getScheduleDate());
        assertNull(request.getFactoryCode());
        assertNull(request.getScheduleMode());
        assertNull(request.getOverwrite());
        assertNull(request.getScheduleType());
    }

    @Test
    @DisplayName("排程结果 - 成功结果构建")
    void testScheduleResult_Success() {
        ScheduleService.ScheduleResult result = new ScheduleService.ScheduleResult();
        result.setSuccess(true);
        result.setMessage("排程成功");
        result.setScheduleDate(LocalDate.of(2024, 7, 7));

        assertTrue(result.isSuccess());
        assertEquals("排程成功", result.getMessage());
        assertEquals(LocalDate.of(2024, 7, 7), result.getScheduleDate());
    }

    @Test
    @DisplayName("排程结果 - 失败结果构建")
    void testScheduleResult_Failure() {
        ScheduleService.ScheduleResult result = new ScheduleService.ScheduleResult();
        result.setSuccess(false);
        result.setMessage("排程失败：缺少必要数据");

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("失败"));
    }
}
