package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mp.api.domain.entity.MdmSkuConstructionRef;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * SKU与施工（示方书）关系Mapper
 *
 * @author APS Team
 */
@Mapper
public interface MdmSkuConstructionRefMapper extends BaseMapper<MdmSkuConstructionRef> {

    /**
     * 根据物料编码和硫化示方书号查询记录
     */
    @Select("SELECT * FROM T_MDM_SKU_CONSTRUCTION_REF WHERE MATERIAL_CODE = #{materialCode} AND LH_NO = #{lhNo} AND IS_DELETE = '0' LIMIT 1")
    MdmSkuConstructionRef selectByMaterialCodeAndLhNo(@Param("materialCode") String materialCode, @Param("lhNo") String lhNo);
}
