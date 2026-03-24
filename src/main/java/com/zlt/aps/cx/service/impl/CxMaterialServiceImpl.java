package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cx.entity.CxMaterial;
import com.zlt.aps.cx.mapper.CxMaterialMapper;
import com.zlt.aps.cx.service.CxMaterialService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 物料Service实现类
 *
 * @author APS Team
 * @deprecated 已弃用，请使用 {@link com.zlt.aps.cx.mapper.MdmMaterialInfoMapper}
 */
@Deprecated
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

    @Override
    public List<CxMaterial> listActive() {
        LambdaQueryWrapper<CxMaterial> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxMaterial::getIsActive, 1);
        return list(wrapper);
    }

    @Override
    public Page<CxMaterial> pageList(Page<CxMaterial> page, String materialCode, String productStructure) {
        LambdaQueryWrapper<CxMaterial> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxMaterial::getIsActive, 1);
        
        if (StringUtils.hasText(materialCode)) {
            wrapper.like(CxMaterial::getMaterialCode, materialCode);
        }
        if (StringUtils.hasText(productStructure)) {
            wrapper.eq(CxMaterial::getProductStructure, productStructure);
        }
        
        wrapper.orderByAsc(CxMaterial::getMaterialCode);
        return page(page, wrapper);
    }
}
