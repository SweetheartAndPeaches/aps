package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 硫化排程结果Mapper接口
 *
 * @author APS Team
 * @since 2.0.0
 */
@Mapper
public interface LhScheduleResultMapper extends BaseMapper<LhScheduleResult> {

    /**
     * 按排程日期查询
     */
    @Select("SELECT * FROM t_lh_schedule_result WHERE SCHEDULE_DATE = #{scheduleDate} AND (PRODUCTION_STATUS IS NULL OR PRODUCTION_STATUS != 'COMPLETED') AND IS_DELETE = '0'")
    List<LhScheduleResult> selectByDate(@Param("scheduleDate") LocalDate scheduleDate);

    /**
     * 查询所有未完成的排程
     */
    @Select("SELECT * FROM t_lh_schedule_result WHERE PRODUCTION_STATUS IS NULL OR PRODUCTION_STATUS != 'COMPLETED' AND IS_DELETE = '0' ORDER BY SCHEDULE_DATE, MACHINE_ORDER")
    List<LhScheduleResult> selectAll();
}
