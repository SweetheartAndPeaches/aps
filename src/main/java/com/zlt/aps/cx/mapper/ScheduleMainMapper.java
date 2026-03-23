package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.ScheduleMain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 排程主表Mapper接口
 *
 * @author APS Team
 */
@Mapper
public interface ScheduleMainMapper extends BaseMapper<ScheduleMain> {

    /**
     * 查询所有排程
     */
    @Select("SELECT * FROM t_cx_schedule_main ORDER BY schedule_date DESC, id DESC")
    List<ScheduleMain> selectAll();

    /**
     * 根据日期范围查询排程
     */
    @Select("SELECT * FROM t_cx_schedule_main WHERE schedule_date BETWEEN #{startDate} AND #{endDate} ORDER BY schedule_date DESC, id DESC")
    List<ScheduleMain> selectByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 根据日期查询排程
     */
    @Select("SELECT * FROM t_cx_schedule_main WHERE schedule_date = #{scheduleDate} ORDER BY id DESC LIMIT 1")
    ScheduleMain selectByDate(@Param("scheduleDate") LocalDate scheduleDate);

    /**
     * 查询草稿状态的排程
     */
    @Select("SELECT * FROM t_cx_schedule_main WHERE status = 'DRAFT' ORDER BY schedule_date DESC, id DESC")
    List<ScheduleMain> selectDraftSchedules();
}
