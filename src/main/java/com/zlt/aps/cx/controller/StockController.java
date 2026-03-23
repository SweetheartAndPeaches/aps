package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zlt.aps.cx.common.Result;
import com.zlt.aps.cx.entity.CxStock;
import com.zlt.aps.cx.service.CxStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存Controller
 *
 * @author APS Team
 */
@Tag(name = "库存管理", description = "库存相关接口")
@RestController
@RequestMapping("/stock")
public class StockController {

    @Autowired
    private CxStockService cxStockService;

    @Operation(summary = "根据物料编码获取库存", description = "根据物料编码查询库存信息")
    @GetMapping("/code/{materialCode}")
    public Result<CxStock> getByMaterialCode(
            @Parameter(description = "物料编码") @PathVariable String materialCode) {
        return Result.success(cxStockService.getByMaterialCode(materialCode));
    }

    @Operation(summary = "获取低库存预警列表", description = "获取所有低库存预警的物料")
    @GetMapping("/low")
    public Result<List<CxStock>> listLowStock() {
        return Result.success(cxStockService.listLowStock());
    }

    @Operation(summary = "获取高库存预警列表", description = "获取所有高库存预警的物料")
    @GetMapping("/high")
    public Result<List<CxStock>> listHighStock() {
        return Result.success(cxStockService.listHighStock());
    }

    @Operation(summary = "获取收尾预警列表", description = "获取所有需要收尾预警的物料")
    @GetMapping("/ending")
    public Result<List<CxStock>> listEndingStock() {
        return Result.success(cxStockService.listEndingStock());
    }

    @Operation(summary = "计算库存可供硫化时长", description = "计算指定物料的库存可供硫化时长")
    @GetMapping("/hours/{materialCode}")
    public Result<BigDecimal> calculateStockHours(
            @Parameter(description = "物料编码") @PathVariable String materialCode) {
        return Result.success(cxStockService.calculateStockHours(materialCode));
    }

    @Operation(summary = "刷新库存预警状态", description = "刷新所有库存的预警状态")
    @PostMapping("/refresh-alert")
    public Result<Void> refreshAlertStatus() {
        cxStockService.refreshAllAlertStatus();
        return Result.success();
    }

    @Operation(summary = "分页查询库存", description = "分页查询所有库存")
    @GetMapping("/page")
    public Result<Page<CxStock>> pageList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "预警状态") @RequestParam(required = false) String alertStatus) {
        Page<CxStock> page = new Page<>(pageNum, pageSize);
        return Result.success(cxStockService.pageList(page, alertStatus));
    }

    @Operation(summary = "根据ID获取库存", description = "根据库存ID查询库存详情")
    @GetMapping("/{id}")
    public Result<CxStock> getById(
            @Parameter(description = "库存ID") @PathVariable Long id) {
        return Result.success(cxStockService.getById(id));
    }

    @Operation(summary = "更新库存", description = "更新库存信息")
    @PutMapping
    public Result<Boolean> update(@RequestBody CxStock stock) {
        return Result.success(cxStockService.updateById(stock));
    }

    @Operation(summary = "批量更新库存", description = "批量更新库存数量")
    @PostMapping("/batch-update")
    public Result<Boolean> batchUpdateStock(@RequestBody List<CxStock> stocks) {
        return Result.success(cxStockService.updateBatchById(stocks));
    }
}
