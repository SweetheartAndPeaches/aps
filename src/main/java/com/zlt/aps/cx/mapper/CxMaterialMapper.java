package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.CxMaterial;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 物料Mapper接口
 *
 * @author APS Team
 */
@Mapper
public interface CxMaterialMapper extends BaseMapper<CxMaterial> {

    /**
     * 查询所有物料
     */
    @Select("SELECT * FROM T_CX_MATERIAL WHERE IS_ACTIVE = 1 ORDER BY MATERIAL_CODE")
    List<CxMaterial> selectAll();

    /**
     * 根据物料编码查询
     */
    @Select("SELECT * FROM T_CX_MATERIAL WHERE MATERIAL_CODE = #{materialCode}")
    CxMaterial selectByCode(String materialCode);
}
