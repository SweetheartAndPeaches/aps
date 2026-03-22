package com.jinyu.aps.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinyu.aps.entity.Material;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 物料Mapper接口
 *
 * @author APS Team
 */
@Mapper
public interface MaterialMapper extends BaseMapper<Material> {

    /**
     * 查询所有物料
     */
    @Select("SELECT * FROM t_cx_material WHERE is_active = 1 ORDER BY material_code")
    List<Material> selectAll();

    /**
     * 根据物料编码查询
     */
    @Select("SELECT * FROM t_cx_material WHERE material_code = #{materialCode}")
    Material selectByCode(String materialCode);
}
