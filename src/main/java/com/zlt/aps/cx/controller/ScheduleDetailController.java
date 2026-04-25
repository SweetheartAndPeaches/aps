package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.service.CxScheduleDetailService;
import com.zlt.aps.cx.vo.CxScheduleDetailVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
@Slf4j
public class ScheduleDetailController {

    @Autowired
    private CxScheduleDetailService cxScheduleDetailService;

    // ==================== 查询列表 ====================

    @ApiOperation(value = "根据主表ID获取明细列表（带主表信息）", notes = "根据排程主表ID查询明细列表，并关联主表信息")
    @GetMapping("/main/{mainId}")
    public AjaxResult listByMainId(
            @ApiParam(value = "主表ID") @PathVariable Long mainId) {
        List<CxScheduleDetailVo> list = cxScheduleDetailService.listVoByMainId(mainId);
        return AjaxResult.success(list);
    }

    @ApiOperation(value = "根据机台和日期获取明细列表（带主表信息）", notes = "根据机台编号和日期查询明细列表，并关联主表信息")
    @GetMapping("/machine")
    public AjaxResult listByMachineAndDate(
            @ApiParam(value = "机台编号") @RequestParam String machineCode,
            @ApiParam(value = "计划日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scheduleDate) {
        List<CxScheduleDetailVo> list = cxScheduleDetailService.listVoByMachineAndDate(machineCode, scheduleDate);
        return AjaxResult.success(list);
    }

    @ApiOperation(value = "根据机台和日期范围获取明细列表（按机台降序+胎胚排序）", notes = "查询指定机台范围和日期范围的明细列表，按机台降序+胎胚放一起排序")
    @GetMapping("/machine/range")
    public AjaxResult listByMachineAndDateRange(
            @ApiParam(value = "机台编号（起始）") @RequestParam(required = false) String machineCodeStart,
            @ApiParam(value = "机台编号（结束）") @RequestParam(required = false) String machineCodeEnd,
            @ApiParam(value = "计划日期（起始）") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scheduleDateStart,
            @ApiParam(value = "计划日期（结束）") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scheduleDateEnd) {
        List<CxScheduleDetailVo> list = cxScheduleDetailService.listVoByMachineAndDateRange(
                machineCodeStart, machineCodeEnd, scheduleDateStart, scheduleDateEnd);
        return AjaxResult.success(list);
    }

    @ApiOperation(value = "根据班次获取明细列表", notes = "根据主表ID和班次编码查询明细列表")
    @GetMapping("/shift")
    public AjaxResult listByShift(
            @ApiParam(value = "主表ID") @RequestParam Long mainId,
            @ApiParam(value = "班次编码") @RequestParam String shiftCode) {
        return AjaxResult.success(cxScheduleDetailService.listByShift(mainId, shiftCode));
    }

    @ApiOperation(value = "根据ID获取明细", notes = "根据明细ID查询详情")
    @GetMapping("/{id}")
    public AjaxResult getById(
            @ApiParam(value = "明细ID") @PathVariable Long id) {
        return AjaxResult.success(cxScheduleDetailService.getById(id));
    }

    // ==================== 新增 ====================

    @ApiOperation(value = "新增明细", notes = "新增排程明细")
    @PostMapping
    public AjaxResult save(@RequestBody CxScheduleDetail detail) {
        boolean result = cxScheduleDetailService.save(detail);
        if (result) {
            return AjaxResult.success("新增成功", detail);
        }
        return AjaxResult.error("新增失败");
    }

    @ApiOperation(value = "批量新增明细", notes = "批量新增排程明细")
    @PostMapping("/batch")
    public AjaxResult batchSave(@RequestBody List<CxScheduleDetail> details) {
        boolean result = cxScheduleDetailService.batchSave(details);
        if (result) {
            return AjaxResult.success("批量新增成功", details.size() + "条记录");
        }
        return AjaxResult.error("批量新增失败");
    }

    // ==================== 编辑/修改 ====================

    @ApiOperation(value = "编辑明细", notes = "根据ID修改排程明细")
    @PutMapping("/{id}")
    public AjaxResult update(
            @ApiParam(value = "明细ID") @PathVariable Long id,
            @RequestBody CxScheduleDetail detail) {
        detail.setId(id);
        boolean result = cxScheduleDetailService.updateById(detail);
        if (result) {
            return AjaxResult.success("修改成功", detail);
        }
        return AjaxResult.error("修改失败");
    }

    @ApiOperation(value = "更新完成量", notes = "更新排程明细的完成量")
    @PutMapping("/complete/{detailId}")
    public AjaxResult updateCompletedQuantity(
            @ApiParam(value = "明细ID") @PathVariable Long detailId,
            @ApiParam(value = "完成量") @RequestParam Integer completedQuantity) {
        boolean result = cxScheduleDetailService.updateCompletedQuantity(detailId, completedQuantity);
        if (result) {
            return AjaxResult.success("更新成功");
        }
        return AjaxResult.error("更新失败");
    }

    @ApiOperation(value = "更新车次状态", notes = "更新排程明细的车次状态")
    @PutMapping("/trip-status/{detailId}")
    public AjaxResult updateTripStatus(
            @ApiParam(value = "明细ID") @PathVariable Long detailId,
            @ApiParam(value = "车次状态") @RequestParam String tripStatus) {
        boolean result = cxScheduleDetailService.updateTripStatus(detailId, tripStatus);
        if (result) {
            return AjaxResult.success("更新成功");
        }
        return AjaxResult.error("更新失败");
    }

    // ==================== 删除 ====================

    @ApiOperation(value = "删除明细", notes = "删除指定ID的明细")
    @DeleteMapping("/{id}")
    public AjaxResult delete(
            @ApiParam(value = "明细ID") @PathVariable Long id) {
        boolean result = cxScheduleDetailService.removeById(id);
        if (result) {
            return AjaxResult.success("删除成功");
        }
        return AjaxResult.error("删除失败");
    }

    @ApiOperation(value = "批量删除明细", notes = "批量删除指定ID的明细")
    @DeleteMapping("/batch")
    public AjaxResult batchDelete(
            @ApiParam(value = "明细ID列表") @RequestParam List<Long> ids) {
        boolean result = cxScheduleDetailService.removeByIds(ids);
        if (result) {
            return AjaxResult.success("批量删除成功", ids.size() + "条记录");
        }
        return AjaxResult.error("批量删除失败");
    }

    @ApiOperation(value = "删除主表下所有明细", notes = "删除指定主表ID下的所有明细")
    @DeleteMapping("/main/{mainId}")
    public AjaxResult deleteByMainId(
            @ApiParam(value = "主表ID") @PathVariable Long mainId) {
        boolean result = cxScheduleDetailService.deleteByMainId(mainId);
        if (result) {
            return AjaxResult.success("删除成功");
        }
        return AjaxResult.error("删除失败");
    }

    // ==================== 其他 ====================

    @ApiOperation(value = "获取下一个车次号", notes = "获取指定主表和班次的下一个车次号")
    @GetMapping("/next-trip-no")
    public AjaxResult getNextTripNo(
            @ApiParam(value = "主表ID") @RequestParam Long mainId,
            @ApiParam(value = "班次编码") @RequestParam String shiftCode) {
        Integer nextTripNo = cxScheduleDetailService.getNextTripNo(mainId, shiftCode);
        return AjaxResult.success(nextTripNo);
    }
}
