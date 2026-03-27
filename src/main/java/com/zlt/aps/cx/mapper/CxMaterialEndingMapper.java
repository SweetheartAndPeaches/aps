package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.CxMaterialEnding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 物料收尾管理Mapper接口
 *
 * @author APS Team
 */
@Mapper
public interface CxMaterialEndingMapper extends BaseMapper<CxMaterialEnding> {

    /**
     * 根据统计日期查询物料收尾信息
     *
     * @param statDate 统计日期
     * @return 物料收尾列表
     */
    @Select("SELECT * FROM T_CX_MATERIAL_ENDING WHERE STAT_DATE = #{statDate} ORDER BY IS_URGENT_ENDING DESC, ESTIMATED_ENDING_DAYS ASC")
    List<CxMaterialEnding> selectByStatDate(@Param("statDate") LocalDate statDate);

    /**
     * 根据工厂和统计日期查询
     *
     * @param factoryCode 工厂编码
     * @param statDate    统计日期
     * @return 物料收尾列表
     */
    @Select("SELECT * FROM T_CX_MATERIAL_ENDING WHERE FACTORY_CODE = #{factoryCode} AND STAT_DATE = #{statDate} ORDER BY IS_URGENT_ENDING DESC, ESTIMATED_ENDING_DAYS ASC")
    List<CxMaterialEnding> selectByFactoryAndStatDate(@Param("factoryCode") String factoryCode, @Param("statDate") LocalDate statDate);

    /**
     * 根据物料编码和统计日期查询
     *
     * @param materialCode 物料编码
     * @param statDate     统计日期
     * @return 物料收尾
     */
    @Select("SELECT * FROM T_CX_MATERIAL_ENDING WHERE MATERIAL_CODE = #{materialCode} AND STAT_DATE = #{statDate} LIMIT 1")
    CxMaterialEnding selectByMaterialAndStatDate(@Param("materialCode") String materialCode, @Param("statDate") LocalDate statDate);

    /**
     * 查询需要调整月计划的物料
     *
     * @param statDate 统计日期
     * @return 需要调整月计划的物料列表
     */
    @Select("SELECT * FROM T_CX_MATERIAL_ENDING WHERE STAT_DATE = #{statDate} AND NEED_MONTH_PLAN_ADJUST = 1 ORDER BY DELAY_QUANTITY DESC")
    List<CxMaterialEnding> selectNeedMonthPlanAdjust(@Param("statDate") LocalDate statDate);

    /**
     * 查询紧急收尾的物料
     *
     * @param statDate 统计日期
     * @return 紧急收尾的物料列表
     */
    @Select("SELECT * FROM T_CX_MATERIAL_ENDING WHERE STAT_DATE = #{statDate} AND IS_URGENT_ENDING = 1 ORDER BY DELAY_QUANTITY DESC")
    List<CxMaterialEnding> selectUrgentEndings(@Param("statDate") LocalDate statDate);

    /**
     * 删除指定日期的数据（用于重新计算时清理旧数据）
     *
     * @param statDate 统计日期
     * @return 删除数量
     */
    @Select("DELETE FROM T_CX_MATERIAL_ENDING WHERE STAT_DATE = #{statDate}")
    int deleteByStatDate(@Param("statDate") LocalDate statDate);
}
