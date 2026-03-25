package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.entity.mdm.MdmMoldingMachine;
import com.zlt.aps.cx.mapper.MdmMoldingMachineMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型机台Controller
 *
 * @author APS Team
 */
@Api(tags = "成型机台管理")
@RestController
@RequestMapping("/machine")
public class MachineController {

    @Autowired
    private MdmMoldingMachineMapper mdmMoldingMachineMapper;

    @ApiOperation(value = "获取所有可用机台", notes = "获取所有状态为运行中的机台列表")
    @GetMapping("/available")
    public AjaxResult listAvailableMachines() {
        LambdaQueryWrapper<MdmMoldingMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmMoldingMachine::getIsActive, 1)
                .and(w -> w.eq(MdmMoldingMachine::getMaintainStatus, "RUNNING")
                        .or().isNull(MdmMoldingMachine::getMaintainStatus));
        return AjaxResult.success(mdmMoldingMachineMapper.selectList(wrapper));
    }

    @ApiOperation(value = "根据产线获取机台", notes = "根据产线编号获取机台列表")
    @GetMapping("/line/{lineNumber}")
    public AjaxResult listByLineNumber(
            @ApiParam(value = "产线编号") @PathVariable Integer lineNumber) {
        LambdaQueryWrapper<MdmMoldingMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmMoldingMachine::getLineNumber, lineNumber)
                .eq(MdmMoldingMachine::getIsActive, 1);
        return AjaxResult.success(mdmMoldingMachineMapper.selectList(wrapper));
    }

    @ApiOperation(value = "根据机台编码获取机台", notes = "根据机台编码获取机台详情")
    @GetMapping("/code/{machineCode}")
    public AjaxResult getByMachineCode(
            @ApiParam(value = "机台编码") @PathVariable String machineCode) {
        LambdaQueryWrapper<MdmMoldingMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmMoldingMachine::getCxMachineCode, machineCode);
        return AjaxResult.success(mdmMoldingMachineMapper.selectOne(wrapper));
    }

    @ApiOperation(value = "分页查询机台", notes = "分页查询所有机台")
    @GetMapping("/page")
    public AjaxResult pageList(
            @ApiParam(value = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam(value = "机台状态") @RequestParam(required = false) String status) {
        Page<MdmMoldingMachine> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MdmMoldingMachine> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(MdmMoldingMachine::getMaintainStatus, status);
        }
        
        wrapper.orderByAsc(MdmMoldingMachine::getCxMachineCode);
        return AjaxResult.success(mdmMoldingMachineMapper.selectPage(page, wrapper));
    }

    @ApiOperation(value = "根据ID获取机台", notes = "根据机台ID获取机台详情")
    @GetMapping("/{id}")
    public AjaxResult getById(
            @ApiParam(value = "机台ID") @PathVariable Long id) {
        return AjaxResult.success(mdmMoldingMachineMapper.selectById(id));
    }

    @ApiOperation(value = "新增机台", notes = "新增成型机台")
    @PostMapping
    public AjaxResult save(@RequestBody MdmMoldingMachine machine) {
        return AjaxResult.success(mdmMoldingMachineMapper.insert(machine) > 0);
    }

    @ApiOperation(value = "更新机台", notes = "更新成型机台信息")
    @PutMapping
    public AjaxResult update(@RequestBody MdmMoldingMachine machine) {
        return AjaxResult.success(mdmMoldingMachineMapper.updateById(machine) > 0);
    }

    @ApiOperation(value = "删除机台", notes = "删除指定ID的机台")
    @DeleteMapping("/{id}")
    public AjaxResult delete(
            @ApiParam(value = "机台ID") @PathVariable Long id) {
        return AjaxResult.success(mdmMoldingMachineMapper.deleteById(id) > 0);
    }

    @ApiOperation(value = "更新机台状态", notes = "更新机台的维护状态")
    @PutMapping("/status/{id}")
    public AjaxResult updateStatus(
            @ApiParam(value = "机台ID") @PathVariable Long id,
            @ApiParam(value = "状态") @RequestParam String status) {
        MdmMoldingMachine machine = new MdmMoldingMachine();
        machine.setId(id);
        machine.setMaintainStatus(status);
        return AjaxResult.success(mdmMoldingMachineMapper.updateById(machine) > 0);
    }
}
