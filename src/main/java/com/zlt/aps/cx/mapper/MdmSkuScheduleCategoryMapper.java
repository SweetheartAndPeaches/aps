package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.mdm.MdmSkuScheduleCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * SKU排产分类Mapper
 *
 * @author APS Team
 */
@Mapper
public interface MdmSkuScheduleCategoryMapper extends BaseMapper<MdmSkuScheduleCategory> {

    /**
     * 根据物料编码查询排产分类
     */
    @Select("SELECT * FROM t_mdm_sku_schedule_category WHERE MATERIAL_CODE = #{materialCode} AND IS_DELETE = 0 LIMIT 1")
    MdmSkuScheduleCategory selectByMaterialCode(@Param("materialCode") String materialCode);

    /**
     * 查询所有主销产品
     */
    @Select("SELECT * FROM t_mdm_sku_schedule_category WHERE SCHEDULE_TYPE = '01' AND IS_DELETE = 0")
    List<MdmSkuScheduleCategory> selectMainProducts();

    /**
     * 查询所有排产分类
     */
    @Select("SELECT * FROM t_mdm_sku_schedule_category WHERE IS_DELETE = 0")
    List<MdmSkuScheduleCategory> selectAllCategories();
}
