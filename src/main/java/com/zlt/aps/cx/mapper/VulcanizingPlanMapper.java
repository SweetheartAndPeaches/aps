package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.VulcanizingPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 硫化计划Mapper
 *
 * @author APS Team
 */
@Mapper
public interface VulcanizingPlanMapper extends BaseMapper<VulcanizingPlan> {

    /**
     * 根据日期查询硫化计划
     */
    @Select("SELECT * FROM t_cx_vulcanizing_plan WHERE plan_date = #{planDate} AND status != 'CANCELLED' ORDER BY priority, id")
    List<VulcanizingPlan> selectByDate(@Param("planDate") LocalDate planDate);

    /**
     * 查询所有硫化计划
     */
    @Select("SELECT * FROM t_cx_vulcanizing_plan ORDER BY plan_date DESC, priority, id")
    List<VulcanizingPlan> selectAll();
}
