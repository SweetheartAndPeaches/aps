package com.jinyu.aps.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jinyu.aps.entity.Material;

import java.util.List;

/**
 * 物料Service接口
 *
 * @author APS Team
 */
public interface MaterialService extends IService<Material> {

    /**
     * 根据产品结构获取物料
     */
    List<Material> listByProductStructure(String productStructure);

    /**
     * 根据物料编码获取物料
     */
    Material getByMaterialCode(String materialCode);
}
