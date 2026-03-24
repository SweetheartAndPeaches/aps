package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cx.entity.CxMaterial;
import com.zlt.aps.cx.mapper.CxMaterialMapper;
import com.zlt.aps.cx.service.CxMaterialService;
import org.springframework.stereotype.Service;

/**
 * 物料Service实现类
 *
 * @author APS Team
 * @deprecated 已弃用，请使用 {@link com.zlt.aps.cx.mapper.MdmMaterialInfoMapper}
 */
@Deprecated
@Service
public class CxMaterialServiceImpl extends ServiceImpl<CxMaterialMapper, CxMaterial> implements CxMaterialService {
    // 已废弃，请使用 MdmMaterialInfoMapper
}
