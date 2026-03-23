package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zlt.aps.cx.entity.MonthPlanFinal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 月度生产计划定稿Mapper接口
 * 
 * @author APS Team
 */
@Mapper
public interface MonthPlanFinalMapper extends BaseMapper<MonthPlanFinal> {

    /**
     * 根据年月查询月计划
     * @param yearMonth 年月(YYYYMM)
     * @return 月计划列表
     */
    @Select("SELECT * FROM t_mp_month_plan_prod_final WHERE year_month = #{yearMonth} ORDER BY production_sequence, id")
    List<MonthPlanFinal> selectByYearMonth(@Param("yearMonth") Integer yearMonth);

    /**
     * 根据年份和月份查询
     * @param year 年份
     * @param month 月份
     * @return 月计划列表
     */
    @Select("SELECT * FROM t_mp_month_plan_prod_final WHERE year = #{year} AND month = #{month} ORDER BY production_sequence, id")
    List<MonthPlanFinal> selectByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);

    /**
     * 根据工厂和年月查询
     * @param factoryCode 工厂编码
     * @param yearMonth 年月
     * @return 月计划列表
     */
    @Select("SELECT * FROM t_mp_month_plan_prod_final WHERE factory_code = #{factoryCode} AND year_month = #{yearMonth} ORDER BY production_sequence, id")
    List<MonthPlanFinal> selectByFactoryAndYearMonth(@Param("factoryCode") String factoryCode, @Param("yearMonth") Integer yearMonth);

    /**
     * 根据物料编码查询
     * @param materialCode 物料编码
     * @param yearMonth 年月
     * @return 月计划
     */
    @Select("SELECT * FROM t_mp_month_plan_prod_final WHERE material_code = #{materialCode} AND year_month = #{yearMonth} LIMIT 1")
    MonthPlanFinal selectByMaterialAndYearMonth(@Param("materialCode") String materialCode, @Param("yearMonth") Integer yearMonth);

    /**
     * 根据工单号查询
     * @param productionNo 工单号
     * @return 月计划
     */
    @Select("SELECT * FROM t_mp_month_plan_prod_final WHERE production_no = #{productionNo}")
    MonthPlanFinal selectByProductionNo(@Param("productionNo") String productionNo);

    /**
     * 查询指定年月已发布的计划
     * @param yearMonth 年月
     * @return 已发布的月计划列表
     */
    @Select("SELECT * FROM t_mp_month_plan_prod_final WHERE year_month = #{yearMonth} AND is_release = '1' ORDER BY production_sequence, id")
    List<MonthPlanFinal> selectReleasedByYearMonth(@Param("yearMonth") Integer yearMonth);

    /**
     * 查询指定日期有排产的计划
     * @param yearMonth 年月
     * @param day 日期(1-31)
     * @return 有排产的月计划列表
     */
    @Select("SELECT * FROM t_mp_month_plan_prod_final WHERE year_month = #{yearMonth} AND day_${day} > 0 ORDER BY production_sequence, id")
    List<MonthPlanFinal> selectWithPlanOnDay(@Param("yearMonth") Integer yearMonth, @Param("day") Integer day);

    /**
     * 查询指定版本的计划
     * @param yearMonth 年月
     * @param productionVersion 排产版本
     * @return 月计划列表
     */
    @Select("SELECT * FROM t_mp_month_plan_prod_final WHERE year_month = #{yearMonth} AND production_version = #{productionVersion} ORDER BY production_sequence, id")
    List<MonthPlanFinal> selectByVersion(@Param("yearMonth") Integer yearMonth, @Param("productionVersion") String productionVersion);

    /**
     * 分页查询月计划
     * @param page 分页参数
     * @param yearMonth 年月
     * @param factoryCode 工厂编码(可选)
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT * FROM t_mp_month_plan_prod_final " +
            "WHERE year_month = #{yearMonth} " +
            "<if test='factoryCode != null and factoryCode != \"\"'>" +
            "AND factory_code = #{factoryCode} " +
            "</if>" +
            "ORDER BY production_sequence, id" +
            "</script>")
    IPage<MonthPlanFinal> selectPageByYearMonth(Page<MonthPlanFinal> page, 
                                                  @Param("yearMonth") Integer yearMonth, 
                                                  @Param("factoryCode") String factoryCode);

    /**
     * 查询所有物料编码(去重)
     * @param yearMonth 年月
     * @return 物料编码列表
     */
    @Select("SELECT DISTINCT material_code FROM t_mp_month_plan_prod_final WHERE year_month = #{yearMonth} ORDER BY material_code")
    List<String> selectMaterialCodesByYearMonth(@Param("yearMonth") Integer yearMonth);

    /**
     * 统计月计划数量
     * @param yearMonth 年月
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM t_mp_month_plan_prod_final WHERE year_month = #{yearMonth}")
    int countByYearMonth(@Param("yearMonth") Integer yearMonth);

    /**
     * 统计月排产总量
     * @param yearMonth 年月
     * @return 排产总量
     */
    @Select("SELECT COALESCE(SUM(total_qty), 0) FROM t_mp_month_plan_prod_final WHERE year_month = #{yearMonth}")
    Long sumTotalQtyByYearMonth(@Param("yearMonth") Integer yearMonth);

    /**
     * 查询需要同步到日计划的物料(当天有排产且未同步)
     * @param yearMonth 年月
     * @param day 日期
     * @return 月计划列表
     */
    @Select("SELECT * FROM t_mp_month_plan_prod_final WHERE year_month = #{yearMonth} AND day_${day} > 0 AND is_release = '1' ORDER BY production_sequence, id")
    List<MonthPlanFinal> selectNeedSyncToDaily(@Param("yearMonth") Integer yearMonth, @Param("day") Integer day);
}
