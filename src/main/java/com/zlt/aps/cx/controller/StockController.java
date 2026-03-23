package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zlt.aps.cx.common.Result;
import com.zlt.aps.cx.entity.Stock;
import com.zlt.aps.cx.service.StockService;
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
    private StockService stockService;

    @Operation(summary = "根据物料编码获取库存", description = "根据物料编码查询库存信息")
    @GetMapping("/code/{materialCode}")
    public Result<Stock> getByMaterialCode(
            @Parameter(description = "物料编码") @PathVariable String materialCode) {
        return Result.success(stockService.getByMaterialCode(materialCode));
    }

    @Operation(summary = "获取低库存预警列表", description = "获取所有低库存预警的物料")
    @GetMapping("/low")
    public Result<List<Stock>> listLowStock() {
        return Result.success(stockService.listLowStock());
    }

    @Operation(summary = "获取高库存预警列表", description = "获取所有高库存预警的物料")
    @GetMapping("/high")
    public Result<List<Stock>> listHighStock() {
        return Result.success(stockService.listHighStock());
    }

    @Operation(summary = "计算库存可供硫化时长", description = "计算指定物料的库存可供硫化时长")
    @GetMapping("/hours/{materialCode}")
    public Result<BigDecimal> calculateStockHours(
            @Parameter(description = "物料编码") @PathVariable String materialCode) {
        return Result.success(stockService.calculateStockHours(materialCode));
    }

    @Operation(summary = "分页查询库存", description = "分页查询所有库存")
    @GetMapping("/page")
    public Result<Page<Stock>> pageList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Stock> page = new Page<>(pageNum, pageSize);
        return Result.success(stockService.page(page));
    }

    @Operation(summary = "根据ID获取库存", description = "根据库存ID查询库存详情")
    @GetMapping("/{id}")
    public Result<Stock> getById(
            @Parameter(description = "库存ID") @PathVariable Long id) {
        return Result.success(stockService.getById(id));
    }

    @Operation(summary = "更新库存", description = "更新库存信息")
    @PutMapping
    public Result<Boolean> update(@RequestBody Stock stock) {
        return Result.success(stockService.updateById(stock));
    }
}
