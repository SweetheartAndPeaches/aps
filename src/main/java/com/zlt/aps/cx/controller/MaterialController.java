package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zlt.aps.cx.common.Result;
import com.zlt.aps.cx.entity.CxMaterial;
import com.zlt.aps.cx.service.CxMaterialService;
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
 * @deprecated 已弃用，物料信息请使用 MdmMaterialInfo 相关接口
 */
@Deprecated
@Tag(name = "物料管理", description = "物料相关接口")
@RestController
@RequestMapping("/material")
public class MaterialController {

    @Autowired
    private CxMaterialService cxMaterialService;

    @Operation(summary = "根据产品结构获取物料", description = "根据产品结构查询物料列表")
    @GetMapping("/structure/{productStructure}")
    public Result<List<CxMaterial>> listByProductStructure(
            @Parameter(description = "产品结构") @PathVariable String productStructure) {
        return Result.success(cxMaterialService.listByProductStructure(productStructure));
    }

    @Operation(summary = "根据物料编码获取物料", description = "根据物料编码查询物料详情")
    @GetMapping("/code/{materialCode}")
    public Result<CxMaterial> getByMaterialCode(
            @Parameter(description = "物料编码") @PathVariable String materialCode) {
        return Result.success(cxMaterialService.getByMaterialCode(materialCode));
    }

    @Operation(summary = "获取所有启用物料", description = "获取所有启用的物料列表")
    @GetMapping("/active")
    public Result<List<CxMaterial>> listActive() {
        return Result.success(cxMaterialService.listActive());
    }

    @Operation(summary = "分页查询物料", description = "分页查询所有物料")
    @GetMapping("/page")
    public Result<Page<CxMaterial>> pageList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "物料编码") @RequestParam(required = false) String materialCode,
            @Parameter(description = "产品结构") @RequestParam(required = false) String productStructure) {
        Page<CxMaterial> page = new Page<>(pageNum, pageSize);
        return Result.success(cxMaterialService.pageList(page, materialCode, productStructure));
    }

    @Operation(summary = "根据ID获取物料", description = "根据物料ID查询物料详情")
    @GetMapping("/{id}")
    public Result<CxMaterial> getById(
            @Parameter(description = "物料ID") @PathVariable Long id) {
        return Result.success(cxMaterialService.getById(id));
    }

    @Operation(summary = "新增物料", description = "新增物料信息")
    @PostMapping
    public Result<Boolean> save(@RequestBody CxMaterial material) {
        return Result.success(cxMaterialService.save(material));
    }

    @Operation(summary = "更新物料", description = "更新物料信息")
    @PutMapping
    public Result<Boolean> update(@RequestBody CxMaterial material) {
        return Result.success(cxMaterialService.updateById(material));
    }

    @Operation(summary = "删除物料", description = "删除指定ID的物料")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @Parameter(description = "物料ID") @PathVariable Long id) {
        return Result.success(cxMaterialService.removeById(id));
    }
}
