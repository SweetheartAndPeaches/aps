package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zlt.aps.cx.common.Result;
import com.zlt.aps.cx.entity.CxStock;
import com.zlt.aps.cx.service.CxStockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存Controller
 *
 * @author APS Team
 */
@Api(tags = "库存管理")
@RestController
@RequestMapping("/stock")
public class StockController {

    @Autowired
    private CxStockService cxStockService;

    @ApiOperation(value = "根据物料编码获取库存", notes = "根据物料编码查询库存信息")
    @GetMapping("/code/{materialCode}")
    public Result<CxStock> getByMaterialCode(
            @ApiParam(value = "物料编码") @PathVariable String materialCode) {
        return Result.success(cxStockService.getByMaterialCode(materialCode));
    }

    @ApiOperation(value = "获取低库存预警列表", notes = "获取所有低库存预警的物料")
    @GetMapping("/low")
    public Result<List<CxStock>> listLowStock() {
        return Result.success(cxStockService.listLowStock());
    }

    @ApiOperation(value = "获取高库存预警列表", notes = "获取所有高库存预警的物料")
    @GetMapping("/high")
    public Result<List<CxStock>> listHighStock() {
        return Result.success(cxStockService.listHighStock());
    }

    @ApiOperation(value = "获取收尾预警列表", notes = "获取所有需要收尾预警的物料")
    @GetMapping("/ending")
    public Result<List<CxStock>> listEndingStock() {
        return Result.success(cxStockService.listEndingStock());
    }

    @ApiOperation(value = "计算库存可供硫化时长", notes = "计算指定物料的库存可供硫化时长")
    @GetMapping("/hours/{materialCode}")
    public Result<BigDecimal> calculateStockHours(
            @ApiParam(value = "物料编码") @PathVariable String materialCode) {
        return Result.success(cxStockService.calculateStockHours(materialCode));
    }

    @ApiOperation(value = "刷新库存预警状态", notes = "刷新所有库存的预警状态")
    @PostMapping("/refresh-alert")
    public Result<Void> refreshAlertStatus() {
        cxStockService.refreshAllAlertStatus();
        return Result.success();
    }

    @ApiOperation(value = "分页查询库存", notes = "分页查询所有库存")
    @GetMapping("/page")
    public Result<Page<CxStock>> pageList(
            @ApiParam(value = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam(value = "预警状态") @RequestParam(required = false) String alertStatus) {
        Page<CxStock> page = new Page<>(pageNum, pageSize);
        return Result.success(cxStockService.pageList(page, alertStatus));
    }

    @ApiOperation(value = "根据ID获取库存", notes = "根据库存ID查询库存详情")
    @GetMapping("/{id}")
    public Result<CxStock> getById(
            @ApiParam(value = "库存ID") @PathVariable Long id) {
        return Result.success(cxStockService.getById(id));
    }

    @ApiOperation(value = "更新库存", notes = "更新库存信息")
    @PutMapping
    public Result<Boolean> update(@RequestBody CxStock stock) {
        return Result.success(cxStockService.updateById(stock));
    }

    @ApiOperation(value = "批量更新库存", notes = "批量更新库存数量")
    @PostMapping("/batch-update")
    public Result<Boolean> batchUpdateStock(@RequestBody List<CxStock> stocks) {
        return Result.success(cxStockService.updateBatchById(stocks));
    }
}
