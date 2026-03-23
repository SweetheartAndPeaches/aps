package com.zlt.aps.cx.controller;

import com.zlt.aps.cx.common.Result;
import com.zlt.aps.cx.entity.CxAlertConfig;
import com.zlt.aps.cx.service.CxAlertConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预警配置Controller
 *
 * @author APS Team
 */
@Tag(name = "预警配置管理", description = "预警配置相关接口")
@RestController
@RequestMapping("/config/alert")
public class AlertConfigController {

    @Autowired
    private CxAlertConfigService cxAlertConfigService;

    @Operation(summary = "获取所有配置", description = "获取所有预警配置列表")
    @GetMapping("/list")
    public Result<List<CxAlertConfig>> list() {
        return Result.success(cxAlertConfigService.list());
    }

    @Operation(summary = "获取启用的配置", description = "获取所有启用的预警配置列表")
    @GetMapping("/active")
    public Result<List<CxAlertConfig>> listActive() {
        return Result.success(cxAlertConfigService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CxAlertConfig>()
                        .eq(CxAlertConfig::getIsActive, 1)));
    }

    @Operation(summary = "根据配置编码获取配置值", description = "根据编码获取配置值（字符串）")
    @GetMapping("/value/{configCode}")
    public Result<String> getConfigValue(
            @Parameter(description = "配置编码") @PathVariable String configCode) {
        return Result.success(cxAlertConfigService.getConfigValue(configCode));
    }

    @Operation(summary = "根据配置编码获取配置值(整数)", description = "根据编码获取配置值（整数）")
    @GetMapping("/int/{configCode}")
    public Result<Integer> getConfigValueAsInt(
            @Parameter(description = "配置编码") @PathVariable String configCode) {
        return Result.success(cxAlertConfigService.getConfigValueAsInt(configCode));
    }

    @Operation(summary = "根据配置编码获取配置值(小数)", description = "根据编码获取配置值（小数）")
    @GetMapping("/double/{configCode}")
    public Result<Double> getConfigValueAsDouble(
            @Parameter(description = "配置编码") @PathVariable String configCode) {
        return Result.success(cxAlertConfigService.getConfigValueAsDouble(configCode));
    }

    @Operation(summary = "根据ID获取配置", description = "根据配置ID查询详情")
    @GetMapping("/{id}")
    public Result<CxAlertConfig> getById(
            @Parameter(description = "配置ID") @PathVariable Long id) {
        return Result.success(cxAlertConfigService.getById(id));
    }

    @Operation(summary = "新增配置", description = "新增预警配置")
    @PostMapping
    public Result<Boolean> save(@RequestBody CxAlertConfig config) {
        return Result.success(cxAlertConfigService.save(config));
    }

    @Operation(summary = "更新配置", description = "更新预警配置")
    @PutMapping
    public Result<Boolean> update(@RequestBody CxAlertConfig config) {
        return Result.success(cxAlertConfigService.updateById(config));
    }

    @Operation(summary = "删除配置", description = "删除指定ID的配置")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @Parameter(description = "配置ID") @PathVariable Long id) {
        return Result.success(cxAlertConfigService.removeById(id));
    }
}
