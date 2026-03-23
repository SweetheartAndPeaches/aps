package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zlt.aps.cx.common.Result;
import com.zlt.aps.cx.entity.mdm.MdmMoldingMachine;
import com.zlt.aps.cx.mapper.MdmMoldingMachineMapper;
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
    private MdmMoldingMachineMapper mdmMoldingMachineMapper;

    @Operation(summary = "获取所有可用机台", description = "获取所有状态为运行中的机台列表")
    @GetMapping("/available")
    public Result<List<MdmMoldingMachine>> listAvailableMachines() {
        LambdaQueryWrapper<MdmMoldingMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmMoldingMachine::getIsActive, 1)
                .and(w -> w.eq(MdmMoldingMachine::getMaintainStatus, "RUNNING")
                        .or().isNull(MdmMoldingMachine::getMaintainStatus));
        return Result.success(mdmMoldingMachineMapper.selectList(wrapper));
    }

    @Operation(summary = "根据产线获取机台", description = "根据产线编号获取机台列表")
    @GetMapping("/line/{lineNumber}")
    public Result<List<MdmMoldingMachine>> listByLineNumber(
            @Parameter(description = "产线编号") @PathVariable Integer lineNumber) {
        LambdaQueryWrapper<MdmMoldingMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmMoldingMachine::getLineNumber, lineNumber)
                .eq(MdmMoldingMachine::getIsActive, 1);
        return Result.success(mdmMoldingMachineMapper.selectList(wrapper));
    }

    @Operation(summary = "根据机台编码获取机台", description = "根据机台编码获取机台详情")
    @GetMapping("/code/{machineCode}")
    public Result<MdmMoldingMachine> getByMachineCode(
            @Parameter(description = "机台编码") @PathVariable String machineCode) {
        LambdaQueryWrapper<MdmMoldingMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmMoldingMachine::getCxMachineCode, machineCode);
        return Result.success(mdmMoldingMachineMapper.selectOne(wrapper));
    }

    @Operation(summary = "分页查询机台", description = "分页查询所有机台")
    @GetMapping("/page")
    public Result<Page<MdmMoldingMachine>> pageList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "机台状态") @RequestParam(required = false) String status) {
        Page<MdmMoldingMachine> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MdmMoldingMachine> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(MdmMoldingMachine::getMaintainStatus, status);
        }
        
        wrapper.orderByAsc(MdmMoldingMachine::getCxMachineCode);
        return Result.success(mdmMoldingMachineMapper.selectPage(page, wrapper));
    }

    @Operation(summary = "根据ID获取机台", description = "根据机台ID获取机台详情")
    @GetMapping("/{id}")
    public Result<MdmMoldingMachine> getById(
            @Parameter(description = "机台ID") @PathVariable Long id) {
        return Result.success(mdmMoldingMachineMapper.selectById(id));
    }

    @Operation(summary = "新增机台", description = "新增成型机台")
    @PostMapping
    public Result<Boolean> save(@RequestBody MdmMoldingMachine machine) {
        return Result.success(mdmMoldingMachineMapper.insert(machine) > 0);
    }

    @Operation(summary = "更新机台", description = "更新成型机台信息")
    @PutMapping
    public Result<Boolean> update(@RequestBody MdmMoldingMachine machine) {
        return Result.success(mdmMoldingMachineMapper.updateById(machine) > 0);
    }

    @Operation(summary = "删除机台", description = "删除指定ID的机台")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @Parameter(description = "机台ID") @PathVariable Long id) {
        return Result.success(mdmMoldingMachineMapper.deleteById(id) > 0);
    }

    @Operation(summary = "更新机台状态", description = "更新机台的维护状态")
    @PutMapping("/status/{id}")
    public Result<Boolean> updateStatus(
            @Parameter(description = "机台ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam String status) {
        MdmMoldingMachine machine = new MdmMoldingMachine();
        machine.setId(id);
        machine.setMaintainStatus(status);
        return Result.success(mdmMoldingMachineMapper.updateById(machine) > 0);
    }
}
