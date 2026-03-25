package com.zlt.aps.cx.controller;

import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 物料Controller
 *
 * @author APS Team
 * @deprecated 已弃用，物料信息请使用 MdmMaterialInfo 相关接口
 */
@Deprecated
@Api(tags = "物料管理(已废弃)")
@RestController
@RequestMapping("/material")
public class MaterialController {
    // 已废弃，请使用 MdmMaterialInfo 相关接口
}
