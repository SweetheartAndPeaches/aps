package com.zlt.aps.mp.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductLhCapacityVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 工厂月度排产-SKU日硫化产能Mapper接口
 *
 * @author APS Team
 */
@Mapper
public interface MdmMonthPlanProductLhCapacityMapper extends BaseMapper<Object> {

    /**
     * 根据工厂编码查询物料日硫化产能
     *
     * @param factoryCode 工厂编码
     * @param type 类型（01-模具关系 02-新模具到货计划）
     * @return 物料日硫化产能列表
     */
    @Select("SELECT FACTORY_CODE AS factoryCode, MATERIAL_CODE AS materialCode, MATERIAL_DESC AS materialDesc, " +
            "MES_CAPACITY AS mesCapacity, STANDARD_CAPACITY AS standardCapacity, APS_CAPACITY AS apsCapacity, " +
            "VULCANIZATION_TIME AS vulcanizationTime, TYPE AS type " +
            "FROM T_MDM_MONTH_PLAN_PRODUCT_LH WHERE FACTORY_CODE = #{factoryCode} AND TYPE = #{type}")
    java.util.List<MonthPlanProductLhCapacityVo> selectByFactoryCodeAndType(
            @Param("factoryCode") String factoryCode, 
            @Param("type") String type);
}
