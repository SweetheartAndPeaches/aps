package com.zlt.aps.cx.controller;

import com.zlt.aps.cx.common.Result;
import com.zlt.aps.cx.entity.ScheduleDetail;
import com.zlt.aps.cx.service.ScheduleDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 排程明细Controller
 *
 * @author APS Team
 */
@Tag(name = "排程明细管理", description = "排程明细相关接口")
@RestController
@RequestMapping("/schedule/detail")
public class ScheduleDetailController {

    @Autowired
    private ScheduleDetailService scheduleDetailService;

    @Operation(summary = "根据主表ID获取明细", description = "根据排程主表ID查询明细列表")
    @GetMapping("/main/{mainId}")
    public Result<List<ScheduleDetail>> listByMainId(
            @Parameter(description = "主表ID") @PathVariable Long mainId) {
        return Result.success(scheduleDetailService.listByMainId(mainId));
    }

    @Operation(summary = "根据机台和日期获取明细", description = "根据机台编号和日期查询明细")
    @GetMapping("/machine")
    public Result<List<ScheduleDetail>> listByMachineAndDate(
            @Parameter(description = "机台编号") @RequestParam String machineCode,
            @Parameter(description = "计划日期") @RequestParam String scheduleDate) {
        return Result.success(scheduleDetailService.listByMachineAndDate(machineCode, scheduleDate));
    }

    @Operation(summary = "更新完成量", description = "更新排程明细的完成量")
    @PutMapping("/complete/{detailId}")
    public Result<Boolean> updateCompletedQuantity(
            @Parameter(description = "明细ID") @PathVariable Long detailId,
            @Parameter(description = "完成量") @RequestParam Integer completedQuantity) {
        return Result.success(scheduleDetailService.updateCompletedQuantity(detailId, completedQuantity));
    }

    @Operation(summary = "根据ID获取明细", description = "根据明细ID查询详情")
    @GetMapping("/{id}")
    public Result<ScheduleDetail> getById(
            @Parameter(description = "明细ID") @PathVariable Long id) {
        return Result.success(scheduleDetailService.getById(id));
    }

    @Operation(summary = "新增明细", description = "新增排程明细")
    @PostMapping
    public Result<Boolean> save(@RequestBody ScheduleDetail detail) {
        return Result.success(scheduleDetailService.save(detail));
    }

    @Operation(summary = "更新明细", description = "更新排程明细")
    @PutMapping
    public Result<Boolean> update(@RequestBody ScheduleDetail detail) {
        return Result.success(scheduleDetailService.updateById(detail));
    }

    @Operation(summary = "删除明细", description = "删除指定ID的明细")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @Parameter(description = "明细ID") @PathVariable Long id) {
        return Result.success(scheduleDetailService.removeById(id));
    }
}
