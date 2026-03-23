package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zlt.aps.cx.common.Result;
import com.zlt.aps.cx.entity.Machine;
import com.zlt.aps.cx.service.MachineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型机台Controller
 *
 * @author APS Team
 */
@Tag(name = "成型机台管理", description = "成型机台相关接口")
@RestController
@RequestMapping("/machine")
public class MachineController {

    @Autowired
    private MachineService machineService;

    @Operation(summary = "获取所有可用机台", description = "获取所有状态为运行中的机台列表")
    @GetMapping("/available")
    public Result<List<Machine>> listAvailableMachines() {
        return Result.success(machineService.listAvailableMachines());
    }

    @Operation(summary = "根据产线获取机台", description = "根据产线编号获取机台列表")
    @GetMapping("/line/{lineNumber}")
    public Result<List<Machine>> listByLineNumber(
            @Parameter(description = "产线编号") @PathVariable Integer lineNumber) {
        return Result.success(machineService.listByLineNumber(lineNumber));
    }

    @Operation(summary = "分页查询机台", description = "分页查询所有机台")
    @GetMapping("/page")
    public Result<Page<Machine>> pageList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Machine> page = new Page<>(pageNum, pageSize);
        return Result.success(machineService.page(page));
    }

    @Operation(summary = "根据ID获取机台", description = "根据机台ID获取机台详情")
    @GetMapping("/{id}")
    public Result<Machine> getById(
            @Parameter(description = "机台ID") @PathVariable Long id) {
        return Result.success(machineService.getById(id));
    }

    @Operation(summary = "新增机台", description = "新增成型机台")
    @PostMapping
    public Result<Boolean> save(@RequestBody Machine machine) {
        return Result.success(machineService.save(machine));
    }

    @Operation(summary = "更新机台", description = "更新成型机台信息")
    @PutMapping
    public Result<Boolean> update(@RequestBody Machine machine) {
        return Result.success(machineService.updateById(machine));
    }

    @Operation(summary = "删除机台", description = "删除指定ID的机台")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @Parameter(description = "机台ID") @PathVariable Long id) {
        return Result.success(machineService.removeById(id));
    }
}
