package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 库存Mapper接口
 *
 * @author APS Team
 */
@Mapper
public interface CxStockMapper extends BaseMapper<CxStock> {

    /**
     * 查询所有库存
     */
    @Select("SELECT * FROM T_CX_STOCK ORDER BY MATERIAL_CODE")
    List<CxStock> selectAll();

    /**
     * 根据物料编码查询库存
     */
    @Select("SELECT * FROM T_CX_STOCK WHERE MATERIAL_CODE = #{materialCode}")
    CxStock selectByMaterialCode(@Param("materialCode") String materialCode);

    /**
     * 更新库存数量
     */
    @Update("UPDATE T_CX_STOCK SET CURRENT_STOCK = #{currentStock}, UPDATE_TIME = NOW() WHERE MATERIAL_CODE = #{materialCode}")
    int updateStock(@Param("materialCode") String materialCode, @Param("currentStock") Integer currentStock);
}
