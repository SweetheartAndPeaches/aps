package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zlt.aps.cx.common.Result;
import com.zlt.aps.cx.dto.ScheduleGenerateDTO;
import com.zlt.aps.cx.dto.ScheduleQueryDTO;
import com.zlt.aps.cx.dto.ScheduleResultDTO;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.service.CxScheduleResultService;
import com.zlt.aps.cx.service.ScheduleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 排程管理Controller
 *
 * @author APS Team
 */
@Api(tags = "排程管理")
@RestController
@RequestMapping("/schedule")
public class ScheduleMainController {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private CxScheduleResultService cxScheduleResultService;

    @ApiOperation(value = "生成排程", notes = "根据日期和天数生成排程")
    @PostMapping("/generate")
    public Result<List<CxScheduleResult>> generateSchedule(@RequestBody ScheduleGenerateDTO dto) {
        // TODO: 待实现 generateSchedule
        return Result.success(new ArrayList<>());
    }

    @ApiOperation(value = "生成单日排程", notes = "生成指定日期的排程")
    @PostMapping("/generate/{date}")
    public Result<List<CxScheduleResult>> generateDailySchedule(
            @ApiParam(value = "排程日期") @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        // TODO: 待实现 generateDailySchedule
        return Result.success(new ArrayList<>());
    }

    @ApiOperation(value = "确认排程", notes = "确认指定排程")
    @PostMapping("/confirm/{id}")
    public Result<Boolean> confirmSchedule(
            @ApiParam(value = "排程ID") @PathVariable Long id) {
        // TODO: 待实现 confirmSchedule
        return Result.success(true);
    }

    @ApiOperation(value = "发布排程", notes = "发布指定排程")
    @PostMapping("/release/{id}")
    public Result<Boolean> releaseSchedule(
            @ApiParam(value = "排程ID") @PathVariable Long id) {
        // TODO: 待实现 releaseSchedule
        return Result.success(true);
    }

    @ApiOperation(value = "批量发布排程", notes = "批量发布排程")
    @PostMapping("/release/batch")
    public Result<Boolean> batchReleaseSchedule(@RequestBody List<Long> ids) {
        // TODO: 待实现 batchReleaseSchedule
        return Result.success(true);
    }

    @ApiOperation(value = "取消排程", notes = "取消指定排程")
    @PostMapping("/cancel/{id}")
    public Result<Boolean> cancelSchedule(
            @ApiParam(value = "排程ID") @PathVariable Long id) {
        // TODO: 待实现 cancelSchedule
        return Result.success(true);
    }

    @ApiOperation(value = "调整排程", notes = "调整排程（插单、换班等）")
    @PostMapping("/adjust/{id}")
    public Result<CxScheduleResult> adjustSchedule(
            @ApiParam(value = "排程ID") @PathVariable Long id,
            @ApiParam(value = "调整类型") @RequestParam String adjustType,
            @ApiParam(value = "调整参数") @RequestParam(required = false) String adjustParam) {
        // TODO: 待实现 adjustSchedule
        return Result.success(null);
    }

    @ApiOperation(value = "删除排程", notes = "删除指定ID的排程")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @ApiParam(value = "排程ID") @PathVariable Long id) {
        // TODO: 待实现 deleteSchedule
        return Result.success(true);
    }

    @ApiOperation(value = "删除日期排程", notes = "删除指定日期的排程")
    @DeleteMapping("/date/{date}")
    public Result<Boolean> deleteByDate(
            @ApiParam(value = "排程日期") @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        // TODO: 待实现 deleteScheduleByDate
        return Result.success(true);
    }

    @ApiOperation(value = "分页查询排程", notes = "分页查询排程列表")
    @PostMapping("/page")
    public Result<Page<ScheduleResultDTO>> pageList(@RequestBody ScheduleQueryDTO queryDTO) {
        return Result.success(cxScheduleResultService.pageList(queryDTO));
    }

    @ApiOperation(value = "根据日期获取排程", notes = "根据日期查询排程")
    @GetMapping("/date/{scheduleDate}")
    public Result<List<CxScheduleResult>> getByScheduleDate(
            @ApiParam(value = "计划日期") 
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scheduleDate) {
        return Result.success(cxScheduleResultService.listByScheduleDate(scheduleDate));
    }

    @ApiOperation(value = "根据机台和日期获取排程", notes = "根据机台编号和日期查询排程")
    @GetMapping("/machine")
    public Result<List<CxScheduleResult>> getByMachineAndDate(
            @ApiParam(value = "机台编号") @RequestParam String machineCode,
            @ApiParam(value = "计划日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scheduleDate) {
        return Result.success(cxScheduleResultService.listByMachineAndDate(machineCode, scheduleDate));
    }

    @ApiOperation(value = "获取排程详情", notes = "根据排程ID查询排程详情（含明细）")
    @GetMapping("/main/detail/{id}")
    public Result<ScheduleResultDTO> getDetailById(
            @ApiParam(value = "排程ID") @PathVariable Long id) {
        // TODO: 待实现 getScheduleDetail
        return Result.success(null);
    }

    @ApiOperation(value = "根据ID获取排程", notes = "根据排程ID查询排程")
    @GetMapping("/{id}")
    public Result<CxScheduleResult> getById(
            @ApiParam(value = "排程ID") @PathVariable Long id) {
        return Result.success(cxScheduleResultService.getById(id));
    }

    @ApiOperation(value = "获取今日排程状态", notes = "获取今日排程状态摘要")
    @GetMapping("/today/status")
    public Result<Object> getTodayStatus() {
        // TODO: 待实现 getTodayScheduleStatus
        return Result.success(null);
    }

    @ApiOperation(value = "刷新库存预警状态", notes = "刷新所有库存的预警状态")
    @PostMapping("/refresh-alert")
    public Result<Boolean> refreshStockAlert() {
        // TODO: 待实现 refreshStockAlertStatus
        return Result.success(true);
    }

    @ApiOperation(value = "更新生产状态", notes = "更新排程的生产状态")
    @PutMapping("/status/{id}")
    public Result<Boolean> updateProductionStatus(
            @ApiParam(value = "排程ID") @PathVariable Long id,
            @ApiParam(value = "生产状态") @RequestParam String productionStatus) {
        return Result.success(cxScheduleResultService.updateProductionStatus(id, productionStatus));
    }

    @ApiOperation(value = "更新班次计划量", notes = "更新排程的班次计划量")
    @PutMapping("/shift-plan/{id}")
    public Result<Boolean> updateShiftPlanQty(
            @ApiParam(value = "排程ID") @PathVariable Long id,
            @ApiParam(value = "班次编码") @RequestParam String shiftCode,
            @ApiParam(value = "计划量") @RequestParam java.math.BigDecimal planQty) {
        return Result.success(cxScheduleResultService.updateShiftPlanQty(id, shiftCode, planQty));
    }

    @ApiOperation(value = "更新班次完成量", notes = "更新排程的班次完成量")
    @PutMapping("/shift-finish/{id}")
    public Result<Boolean> updateShiftFinishQty(
            @ApiParam(value = "排程ID") @PathVariable Long id,
            @ApiParam(value = "班次编码") @RequestParam String shiftCode,
            @ApiParam(value = "完成量") @RequestParam java.math.BigDecimal finishQty) {
        return Result.success(cxScheduleResultService.updateShiftFinishQty(id, shiftCode, finishQty));
    }
}
