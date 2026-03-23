package com.zlt.aps.cx.controller;

import com.zlt.aps.cx.common.Result;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.service.CxScheduleDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    private CxScheduleDetailService cxScheduleDetailService;

    @Operation(summary = "根据主表ID获取明细", description = "根据排程主表ID查询明细列表")
    @GetMapping("/main/{mainId}")
    public Result<List<CxScheduleDetail>> listByMainId(
            @Parameter(description = "主表ID") @PathVariable Long mainId) {
        return Result.success(cxScheduleDetailService.listByMainId(mainId));
    }

    @Operation(summary = "根据机台和日期获取明细", description = "根据机台编号和日期查询明细")
    @GetMapping("/machine")
    public Result<List<CxScheduleDetail>> listByMachineAndDate(
            @Parameter(description = "机台编号") @RequestParam String machineCode,
            @Parameter(description = "计划日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scheduleDate) {
        return Result.success(cxScheduleDetailService.listByMachineAndDate(machineCode, scheduleDate));
    }

    @Operation(summary = "根据班次获取明细", description = "根据主表ID和班次编码查询明细")
    @GetMapping("/shift")
    public Result<List<CxScheduleDetail>> listByShift(
            @Parameter(description = "主表ID") @RequestParam Long mainId,
            @Parameter(description = "班次编码") @RequestParam String shiftCode) {
        return Result.success(cxScheduleDetailService.listByShift(mainId, shiftCode));
    }

    @Operation(summary = "更新完成量", description = "更新排程明细的完成量")
    @PutMapping("/complete/{detailId}")
    public Result<Boolean> updateCompletedQuantity(
            @Parameter(description = "明细ID") @PathVariable Long detailId,
            @Parameter(description = "完成量") @RequestParam Integer completedQuantity) {
        return Result.success(cxScheduleDetailService.updateCompletedQuantity(detailId, completedQuantity));
    }

    @Operation(summary = "更新车次状态", description = "更新排程明细的车次状态")
    @PutMapping("/trip-status/{detailId}")
    public Result<Boolean> updateTripStatus(
            @Parameter(description = "明细ID") @PathVariable Long detailId,
            @Parameter(description = "车次状态") @RequestParam String tripStatus) {
        return Result.success(cxScheduleDetailService.updateTripStatus(detailId, tripStatus));
    }

    @Operation(summary = "根据ID获取明细", description = "根据明细ID查询详情")
    @GetMapping("/{id}")
    public Result<CxScheduleDetail> getById(
            @Parameter(description = "明细ID") @PathVariable Long id) {
        return Result.success(cxScheduleDetailService.getById(id));
    }

    @Operation(summary = "新增明细", description = "新增排程明细")
    @PostMapping
    public Result<Boolean> save(@RequestBody CxScheduleDetail detail) {
        return Result.success(cxScheduleDetailService.save(detail));
    }

    @Operation(summary = "更新明细", description = "更新排程明细")
    @PutMapping
    public Result<Boolean> update(@RequestBody CxScheduleDetail detail) {
        return Result.success(cxScheduleDetailService.updateById(detail));
    }

    @Operation(summary = "删除明细", description = "删除指定ID的明细")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @Parameter(description = "明细ID") @PathVariable Long id) {
        return Result.success(cxScheduleDetailService.removeById(id));
    }

    @Operation(summary = "删除主表下所有明细", description = "删除指定主表ID下的所有明细")
    @DeleteMapping("/main/{mainId}")
    public Result<Boolean> deleteByMainId(
            @Parameter(description = "主表ID") @PathVariable Long mainId) {
        return Result.success(cxScheduleDetailService.deleteByMainId(mainId));
    }

    @Operation(summary = "获取下一个车次号", description = "获取指定主表和班次的下一个车次号")
    @GetMapping("/next-trip-no")
    public Result<Integer> getNextTripNo(
            @Parameter(description = "主表ID") @RequestParam Long mainId,
            @Parameter(description = "班次编码") @RequestParam String shiftCode) {
        return Result.success(cxScheduleDetailService.getNextTripNo(mainId, shiftCode));
    }

    @Operation(summary = "批量保存明细", description = "批量保存排程明细")
    @PostMapping("/batch")
    public Result<Boolean> batchSave(@RequestBody List<CxScheduleDetail> details) {
        return Result.success(cxScheduleDetailService.batchSave(details));
    }
}
