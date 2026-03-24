package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.cx.entity.CxMaterial;

import java.util.List;

/**
 * 物料Service接口
 *
 * @author APS Team
 * @deprecated 已弃用，请使用 {@link com.zlt.aps.cx.mapper.MdmMaterialInfoMapper}
 */
@Deprecated
public interface CxMaterialService extends IService<CxMaterial> {

    /**
     * 根据产品结构获取物料
     */
    List<CxMaterial> listByProductStructure(String productStructure);

    /**
     * 根据物料编码获取物料
     */
    CxMaterial getByMaterialCode(String materialCode);

    /**
     * 获取所有启用的物料
     */
    List<CxMaterial> listActive();

    /**
     * 分页查询物料
     */
    Page<CxMaterial> pageList(Page<CxMaterial> page, String materialCode, String productStructure);
}
