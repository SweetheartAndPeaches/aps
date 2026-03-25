package com.zlt.aps.cx.controller;

import com.zlt.aps.cx.common.Result;
import com.zlt.aps.cx.entity.CxAlertConfig;
import com.zlt.aps.cx.service.CxAlertConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预警配置Controller
 *
 * @author APS Team
 */
@Api(tags = "预警配置管理")
@RestController
@RequestMapping("/config/alert")
public class AlertConfigController {

    @Autowired
    private CxAlertConfigService cxAlertConfigService;

    @ApiOperation(value = "获取所有配置", notes = "获取所有预警配置列表")
    @GetMapping("/list")
    public Result<List<CxAlertConfig>> list() {
        return Result.success(cxAlertConfigService.list());
    }

    @ApiOperation(value = "获取启用的配置", notes = "获取所有启用的预警配置列表")
    @GetMapping("/active")
    public Result<List<CxAlertConfig>> listActive() {
        return Result.success(cxAlertConfigService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CxAlertConfig>()
                        .eq(CxAlertConfig::getIsActive, 1)));
    }

    @ApiOperation(value = "根据配置编码获取配置值", notes = "根据编码获取配置值（字符串）")
    @GetMapping("/value/{configCode}")
    public Result<String> getConfigValue(
            @ApiParam(value = "配置编码") @PathVariable String configCode) {
        return Result.success(cxAlertConfigService.getConfigValue(configCode));
    }

    @ApiOperation(value = "根据配置编码获取配置值(整数)", notes = "根据编码获取配置值（整数）")
    @GetMapping("/int/{configCode}")
    public Result<Integer> getConfigValueAsInt(
            @ApiParam(value = "配置编码") @PathVariable String configCode) {
        return Result.success(cxAlertConfigService.getConfigValueAsInt(configCode));
    }

    @ApiOperation(value = "根据配置编码获取配置值(小数)", notes = "根据编码获取配置值（小数）")
    @GetMapping("/double/{configCode}")
    public Result<Double> getConfigValueAsDouble(
            @ApiParam(value = "配置编码") @PathVariable String configCode) {
        return Result.success(cxAlertConfigService.getConfigValueAsDouble(configCode));
    }

    @ApiOperation(value = "根据ID获取配置", notes = "根据配置ID查询详情")
    @GetMapping("/{id}")
    public Result<CxAlertConfig> getById(
            @ApiParam(value = "配置ID") @PathVariable Long id) {
        return Result.success(cxAlertConfigService.getById(id));
    }

    @ApiOperation(value = "新增配置", notes = "新增预警配置")
    @PostMapping
    public Result<Boolean> save(@RequestBody CxAlertConfig config) {
        return Result.success(cxAlertConfigService.save(config));
    }

    @ApiOperation(value = "更新配置", notes = "更新预警配置")
    @PutMapping
    public Result<Boolean> update(@RequestBody CxAlertConfig config) {
        return Result.success(cxAlertConfigService.updateById(config));
    }

    @ApiOperation(value = "删除配置", notes = "删除指定ID的配置")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @ApiParam(value = "配置ID") @PathVariable Long id) {
        return Result.success(cxAlertConfigService.removeById(id));
    }
}
