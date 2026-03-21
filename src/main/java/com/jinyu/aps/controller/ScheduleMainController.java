package com.jinyu.aps.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinyu.aps.common.Result;
import com.jinyu.aps.dto.ScheduleGenerateDTO;
import com.jinyu.aps.entity.ScheduleMain;
import com.jinyu.aps.service.ScheduleMainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 排程主表Controller
 *
 * @author APS Team
 */
@Tag(name = "排程管理", description = "排程相关接口")
@RestController
@RequestMapping("/schedule")
public class ScheduleMainController {

    @Autowired
    private ScheduleMainService scheduleMainService;

    @Operation(summary = "生成排程", description = "根据日期和天数生成排程")
    @PostMapping("/generate")
    public Result<ScheduleMain> generateSchedule(@RequestBody ScheduleGenerateDTO dto) {
        return Result.success(scheduleMainService.generateSchedule(dto.getScheduleDate(), dto.getDays()));
    }

    @Operation(summary = "确认排程", description = "确认指定排程")
    @PostMapping("/confirm/{id}")
    public Result<Boolean> confirmSchedule(
            @Parameter(description = "排程ID") @PathVariable Long id) {
        return Result.success(scheduleMainService.confirmSchedule(id));
    }

    @Operation(summary = "分页查询排程", description = "分页查询排程列表")
    @GetMapping("/page")
    public Result<Page<ScheduleMain>> pageList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "开始日期") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        return Result.success(scheduleMainService.pageList(pageNum, pageSize, startDate, endDate));
    }

    @Operation(summary = "根据日期获取排程", description = "根据日期查询排程")
    @GetMapping("/date/{scheduleDate}")
    public Result<ScheduleMain> getByScheduleDate(
            @Parameter(description = "计划日期") 
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime scheduleDate) {
        return Result.success(scheduleMainService.getByScheduleDate(scheduleDate));
    }

    @Operation(summary = "根据ID获取排程", description = "根据排程ID查询排程详情")
    @GetMapping("/{id}")
    public Result<ScheduleMain> getById(
            @Parameter(description = "排程ID") @PathVariable Long id) {
        return Result.success(scheduleMainService.getById(id));
    }

    @Operation(summary = "删除排程", description = "删除指定ID的排程")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @Parameter(description = "排程ID") @PathVariable Long id) {
        return Result.success(scheduleMainService.removeById(id));
    }
}
