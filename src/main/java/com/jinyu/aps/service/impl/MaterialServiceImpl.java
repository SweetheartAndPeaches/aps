package com.jinyu.aps.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinyu.aps.entity.Material;
import com.jinyu.aps.mapper.MaterialMapper;
import com.jinyu.aps.service.MaterialService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 物料Service实现类
 *
 * @author APS Team
 */
@Service
public class MaterialServiceImpl extends ServiceImpl<MaterialMapper, Material> implements MaterialService {

    @Override
    public List<Material> listByProductStructure(String productStructure) {
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Material::getIsActive, 1)
                .eq(Material::getProductStructure, productStructure);
        return list(wrapper);
    }

    @Override
    public Material getByMaterialCode(String materialCode) {
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Material::getMaterialCode, materialCode)
                .eq(Material::getIsActive, 1);
        return getOne(wrapper);
    }
}
