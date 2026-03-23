package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.Stock;
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
public interface StockMapper extends BaseMapper<Stock> {

    /**
     * 查询所有库存
     */
    @Select("SELECT * FROM t_cx_stock ORDER BY material_code")
    List<Stock> selectAll();

    /**
     * 根据物料编码查询库存
     */
    @Select("SELECT * FROM t_cx_stock WHERE material_code = #{materialCode}")
    Stock selectByMaterialCode(@Param("materialCode") String materialCode);

    /**
     * 更新库存数量
     */
    @Update("UPDATE t_cx_stock SET current_stock = #{currentStock}, update_time = NOW() WHERE material_code = #{materialCode}")
    int updateStock(@Param("materialCode") String materialCode, @Param("currentStock") Integer currentStock);
}
