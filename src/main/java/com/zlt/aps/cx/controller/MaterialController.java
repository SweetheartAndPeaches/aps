package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zlt.aps.cx.common.Result;
import com.zlt.aps.cx.entity.Material;
import com.zlt.aps.cx.service.MaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 物料Controller
 *
 * @author APS Team
 */
@Tag(name = "物料管理", description = "物料相关接口")
@RestController
@RequestMapping("/material")
public class MaterialController {

    @Autowired
    private MaterialService materialService;

    @Operation(summary = "根据产品结构获取物料", description = "根据产品结构查询物料列表")
    @GetMapping("/structure/{productStructure}")
    public Result<List<Material>> listByProductStructure(
            @Parameter(description = "产品结构") @PathVariable String productStructure) {
        return Result.success(materialService.listByProductStructure(productStructure));
    }

    @Operation(summary = "根据物料编码获取物料", description = "根据物料编码查询物料详情")
    @GetMapping("/code/{materialCode}")
    public Result<Material> getByMaterialCode(
            @Parameter(description = "物料编码") @PathVariable String materialCode) {
        return Result.success(materialService.getByMaterialCode(materialCode));
    }

    @Operation(summary = "分页查询物料", description = "分页查询所有物料")
    @GetMapping("/page")
    public Result<Page<Material>> pageList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Material> page = new Page<>(pageNum, pageSize);
        return Result.success(materialService.page(page));
    }

    @Operation(summary = "根据ID获取物料", description = "根据物料ID查询物料详情")
    @GetMapping("/{id}")
    public Result<Material> getById(
            @Parameter(description = "物料ID") @PathVariable Long id) {
        return Result.success(materialService.getById(id));
    }

    @Operation(summary = "新增物料", description = "新增物料信息")
    @PostMapping
    public Result<Boolean> save(@RequestBody Material material) {
        return Result.success(materialService.save(material));
    }

    @Operation(summary = "更新物料", description = "更新物料信息")
    @PutMapping
    public Result<Boolean> update(@RequestBody Material material) {
        return Result.success(materialService.updateById(material));
    }

    @Operation(summary = "删除物料", description = "删除指定ID的物料")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @Parameter(description = "物料ID") @PathVariable Long id) {
        return Result.success(materialService.removeById(id));
    }
}
