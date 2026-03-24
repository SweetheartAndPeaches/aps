package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.mdm.MdmMonthSurplus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 月底计划余量Mapper
 *
 * @author APS Team
 */
@Mapper
public interface MdmMonthSurplusMapper extends BaseMapper<MdmMonthSurplus> {

    /**
     * 根据年月查询计划余量
     */
    @Select("SELECT * FROM t_mdm_month_surplus WHERE YEAR = #{year} AND MONTH = #{month} AND IS_DELETE = 0")
    List<MdmMonthSurplus> selectByYearMonth(@Param("year") Integer year, @Param("month") Integer month);

    /**
     * 根据物料编码查询计划余量
     */
    @Select("SELECT * FROM t_mdm_month_surplus WHERE MATERIAL_CODE = #{materialCode} AND IS_DELETE = 0 ORDER BY YEAR DESC, MONTH DESC LIMIT 1")
    MdmMonthSurplus selectByMaterialCode(@Param("materialCode") String materialCode);

    /**
     * 根据年月和物料编码查询计划余量
     */
    @Select("SELECT * FROM t_mdm_month_surplus WHERE YEAR = #{year} AND MONTH = #{month} AND MATERIAL_CODE = #{materialCode} AND IS_DELETE = 0")
    MdmMonthSurplus selectByYearMonthAndMaterial(@Param("year") Integer year, @Param("month") Integer month, @Param("materialCode") String materialCode);
}
