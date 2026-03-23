package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cx.entity.CxMaterial;
import com.zlt.aps.cx.mapper.CxMaterialMapper;
import com.zlt.aps.cx.service.CxMaterialService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 物料Service实现类
 *
 * @author APS Team
 */
@Service
public class CxMaterialServiceImpl extends ServiceImpl<CxMaterialMapper, CxMaterial> implements CxMaterialService {

    @Override
    public List<CxMaterial> listByProductStructure(String productStructure) {
        LambdaQueryWrapper<CxMaterial> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxMaterial::getIsActive, 1)
                .eq(CxMaterial::getProductStructure, productStructure);
        return list(wrapper);
    }

    @Override
    public CxMaterial getByMaterialCode(String materialCode) {
        LambdaQueryWrapper<CxMaterial> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxMaterial::getMaterialCode, materialCode)
                .eq(CxMaterial::getIsActive, 1);
        return getOne(wrapper);
    }
}
