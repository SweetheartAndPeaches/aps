package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.service.CxScheduleDetailService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
@Api(tags = "排程明细管理")
@RestController
@RequestMapping("/schedule/detail")
public class ScheduleDetailController {

    @Autowired
    private CxScheduleDetailService cxScheduleDetailService;

    @ApiOperation(value = "根据主表ID获取明细", notes = "根据排程主表ID查询明细列表")
    @GetMapping("/main/{mainId}")
    public AjaxResult listByMainId(
            @ApiParam(value = "主表ID") @PathVariable Long mainId) {
        return AjaxResult.success(cxScheduleDetailService.listByMainId(mainId));
    }

    @ApiOperation(value = "根据机台和日期获取明细", notes = "根据机台编号和日期查询明细")
    @GetMapping("/machine")
    public AjaxResult listByMachineAndDate(
            @ApiParam(value = "机台编号") @RequestParam String machineCode,
            @ApiParam(value = "计划日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scheduleDate) {
        return AjaxResult.success(cxScheduleDetailService.listByMachineAndDate(machineCode, scheduleDate));
    }

    @ApiOperation(value = "根据班次获取明细", notes = "根据主表ID和班次编码查询明细")
    @GetMapping("/shift")
    public AjaxResult listByShift(
            @ApiParam(value = "主表ID") @RequestParam Long mainId,
            @ApiParam(value = "班次编码") @RequestParam String shiftCode) {
        return AjaxResult.success(cxScheduleDetailService.listByShift(mainId, shiftCode));
    }

    @ApiOperation(value = "更新完成量", notes = "更新排程明细的完成量")
    @PutMapping("/complete/{detailId}")
    public AjaxResult updateCompletedQuantity(
            @ApiParam(value = "明细ID") @PathVariable Long detailId,
            @ApiParam(value = "完成量") @RequestParam Integer completedQuantity) {
        return AjaxResult.success(cxScheduleDetailService.updateCompletedQuantity(detailId, completedQuantity));
    }

    @ApiOperation(value = "更新车次状态", notes = "更新排程明细的车次状态")
    @PutMapping("/trip-status/{detailId}")
    public AjaxResult updateTripStatus(
            @ApiParam(value = "明细ID") @PathVariable Long detailId,
            @ApiParam(value = "车次状态") @RequestParam String tripStatus) {
        return AjaxResult.success(cxScheduleDetailService.updateTripStatus(detailId, tripStatus));
    }

    @ApiOperation(value = "根据ID获取明细", notes = "根据明细ID查询详情")
    @GetMapping("/{id}")
    public AjaxResult getById(
            @ApiParam(value = "明细ID") @PathVariable Long id) {
        return AjaxResult.success(cxScheduleDetailService.getById(id));
    }

    @ApiOperation(value = "新增明细", notes = "新增排程明细")
    @PostMapping
    public AjaxResult save(@RequestBody CxScheduleDetail detail) {
        return AjaxResult.success(cxScheduleDetailService.save(detail));
    }

    @ApiOperation(value = "更新明细", notes = "更新排程明细")
    @PutMapping
    public AjaxResult update(@RequestBody CxScheduleDetail detail) {
        return AjaxResult.success(cxScheduleDetailService.updateById(detail));
    }

    @ApiOperation(value = "删除明细", notes = "删除指定ID的明细")
    @DeleteMapping("/{id}")
    public AjaxResult delete(
            @ApiParam(value = "明细ID") @PathVariable Long id) {
        return AjaxResult.success(cxScheduleDetailService.removeById(id));
    }

    @ApiOperation(value = "删除主表下所有明细", notes = "删除指定主表ID下的所有明细")
    @DeleteMapping("/main/{mainId}")
    public AjaxResult deleteByMainId(
            @ApiParam(value = "主表ID") @PathVariable Long mainId) {
        return AjaxResult.success(cxScheduleDetailService.deleteByMainId(mainId));
    }

    @ApiOperation(value = "获取下一个车次号", notes = "获取指定主表和班次的下一个车次号")
    @GetMapping("/next-trip-no")
    public AjaxResult getNextTripNo(
            @ApiParam(value = "主表ID") @RequestParam Long mainId,
            @ApiParam(value = "班次编码") @RequestParam String shiftCode) {
        return AjaxResult.success(cxScheduleDetailService.getNextTripNo(mainId, shiftCode));
    }

    @ApiOperation(value = "批量保存明细", notes = "批量保存排程明细")
    @PostMapping("/batch")
    public AjaxResult batchSave(@RequestBody List<CxScheduleDetail> details) {
        return AjaxResult.success(cxScheduleDetailService.batchSave(details));
    }
}
