package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zlt.aps.cx.entity.FactoryMonthPlanProductionFinalResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 工厂月生产计划-最终排产计划定稿 Mapper接口
 * 
 * @author APS Team
 */
@Mapper
public interface FactoryMonthPlanProductionFinalResultMapper extends BaseMapper<FactoryMonthPlanProductionFinalResult> {

    /**
     * 根据年月查询月计划
     * @param yearMonth 年月(YYYYMM)
     * @return 月计划列表
     */
    @Select("SELECT * FROM T_FACTORY_MONTH_PLAN_PRODUCTION_FINAL_RESULT WHERE YEAR_MONTH = #{yearMonth} ORDER BY PRODUCTION_SEQUENCE, ID")
    List<FactoryMonthPlanProductionFinalResult> selectByYearMonth(@Param("yearMonth") Integer yearMonth);

    /**
     * 根据年份和月份查询
     * @param year 年份
     * @param month 月份
     * @return 月计划列表
     */
    @Select("SELECT * FROM T_FACTORY_MONTH_PLAN_PRODUCTION_FINAL_RESULT WHERE YEAR = #{year} AND MONTH = #{month} ORDER BY PRODUCTION_SEQUENCE, ID")
    List<FactoryMonthPlanProductionFinalResult> selectByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);

    /**
     * 根据工厂和年月查询
     * @param factoryCode 工厂编码
     * @param yearMonth 年月
     * @return 月计划列表
     */
    @Select("SELECT * FROM T_FACTORY_MONTH_PLAN_PRODUCTION_FINAL_RESULT WHERE FACTORY_CODE = #{factoryCode} AND YEAR_MONTH = #{yearMonth} ORDER BY PRODUCTION_SEQUENCE, ID")
    List<FactoryMonthPlanProductionFinalResult> selectByFactoryAndYearMonth(@Param("factoryCode") String factoryCode, @Param("yearMonth") Integer yearMonth);

    /**
     * 根据物料编码查询
     * @param materialCode 物料编码
     * @param yearMonth 年月
     * @return 月计划
     */
    @Select("SELECT * FROM T_FACTORY_MONTH_PLAN_PRODUCTION_FINAL_RESULT WHERE MATERIAL_CODE = #{materialCode} AND YEAR_MONTH = #{yearMonth} LIMIT 1")
    FactoryMonthPlanProductionFinalResult selectByMaterialAndYearMonth(@Param("materialCode") String materialCode, @Param("yearMonth") Integer yearMonth);

    /**
     * 根据工单号查询
     * @param productionNo 工单号
     * @return 月计划
     */
    @Select("SELECT * FROM T_FACTORY_MONTH_PLAN_PRODUCTION_FINAL_RESULT WHERE PRODUCTION_NO = #{productionNo}")
    FactoryMonthPlanProductionFinalResult selectByProductionNo(@Param("productionNo") String productionNo);

    /**
     * 查询指定年月已发布的计划
     * @param yearMonth 年月
     * @return 已发布的月计划列表
     */
    @Select("SELECT * FROM T_FACTORY_MONTH_PLAN_PRODUCTION_FINAL_RESULT WHERE YEAR_MONTH = #{yearMonth} AND IS_RELEASE = '1' ORDER BY PRODUCTION_SEQUENCE, ID")
    List<FactoryMonthPlanProductionFinalResult> selectReleasedByYearMonth(@Param("yearMonth") Integer yearMonth);

    /**
     * 查询指定日期有排产的计划
     * @param yearMonth 年月
     * @param day 日期(1-31)
     * @return 有排产的月计划列表
     */
    @Select("SELECT * FROM T_FACTORY_MONTH_PLAN_PRODUCTION_FINAL_RESULT WHERE YEAR_MONTH = #{yearMonth} AND DAY_${day} > 0 ORDER BY PRODUCTION_SEQUENCE, ID")
    List<FactoryMonthPlanProductionFinalResult> selectWithPlanOnDay(@Param("yearMonth") Integer yearMonth, @Param("day") Integer day);

    /**
     * 查询指定版本的计划
     * @param yearMonth 年月
     * @param productionVersion 排产版本
     * @return 月计划列表
     */
    @Select("SELECT * FROM T_FACTORY_MONTH_PLAN_PRODUCTION_FINAL_RESULT WHERE YEAR_MONTH = #{yearMonth} AND PRODUCTION_VERSION = #{productionVersion} ORDER BY PRODUCTION_SEQUENCE, ID")
    List<FactoryMonthPlanProductionFinalResult> selectByVersion(@Param("yearMonth") Integer yearMonth, @Param("productionVersion") String productionVersion);

    /**
     * 分页查询月计划
     * @param page 分页参数
     * @param yearMonth 年月
     * @param factoryCode 工厂编码(可选)
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT * FROM T_FACTORY_MONTH_PLAN_PRODUCTION_FINAL_RESULT " +
            "WHERE YEAR_MONTH = #{yearMonth} " +
            "<if test='factoryCode != null and factoryCode != \"\"'>" +
            "AND FACTORY_CODE = #{factoryCode} " +
            "</if>" +
            "ORDER BY PRODUCTION_SEQUENCE, ID" +
            "</script>")
    IPage<FactoryMonthPlanProductionFinalResult> selectPageByYearMonth(Page<FactoryMonthPlanProductionFinalResult> page, 
                                                  @Param("yearMonth") Integer yearMonth, 
                                                  @Param("factoryCode") String factoryCode);

    /**
     * 查询所有物料编码(去重)
     * @param yearMonth 年月
     * @return 物料编码列表
     */
    @Select("SELECT DISTINCT MATERIAL_CODE FROM T_FACTORY_MONTH_PLAN_PRODUCTION_FINAL_RESULT WHERE YEAR_MONTH = #{yearMonth} ORDER BY MATERIAL_CODE")
    List<String> selectMaterialCodesByYearMonth(@Param("yearMonth") Integer yearMonth);

    /**
     * 统计月计划数量
     * @param yearMonth 年月
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM T_FACTORY_MONTH_PLAN_PRODUCTION_FINAL_RESULT WHERE YEAR_MONTH = #{yearMonth}")
    int countByYearMonth(@Param("yearMonth") Integer yearMonth);

    /**
     * 统计月排产总量
     * @param yearMonth 年月
     * @return 排产总量
     */
    @Select("SELECT COALESCE(SUM(TOTAL_QTY), 0) FROM T_FACTORY_MONTH_PLAN_PRODUCTION_FINAL_RESULT WHERE YEAR_MONTH = #{yearMonth}")
    Long sumTotalQtyByYearMonth(@Param("yearMonth") Integer yearMonth);

    /**
     * 查询需要同步到日计划的物料(当天有排产且未同步)
     * @param yearMonth 年月
     * @param day 日期
     * @return 月计划列表
     */
    @Select("SELECT * FROM T_FACTORY_MONTH_PLAN_PRODUCTION_FINAL_RESULT WHERE YEAR_MONTH = #{yearMonth} AND DAY_${day} > 0 AND IS_RELEASE = '1' ORDER BY PRODUCTION_SEQUENCE, ID")
    List<FactoryMonthPlanProductionFinalResult> selectNeedSyncToDaily(@Param("yearMonth") Integer yearMonth, @Param("day") Integer day);
}
