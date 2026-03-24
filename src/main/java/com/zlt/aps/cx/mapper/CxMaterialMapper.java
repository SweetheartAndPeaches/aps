package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.CxMaterial;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物料Mapper接口
 *
 * @author APS Team
 * @deprecated 已弃用，请使用 {@link MdmMaterialInfoMapper}
 */
@Deprecated
@Mapper
public interface CxMaterialMapper extends BaseMapper<CxMaterial> {
    // 已废弃，请使用 MdmMaterialInfoMapper
}
