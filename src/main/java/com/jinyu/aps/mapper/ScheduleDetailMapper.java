package com.jinyu.aps.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinyu.aps.entity.ScheduleDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 排程明细Mapper接口
 *
 * @author APS Team
 */
@Mapper
public interface ScheduleDetailMapper extends BaseMapper<ScheduleDetail> {

    /**
     * 根据主表ID查询明细
     */
    @Select("SELECT * FROM t_cx_schedule_detail WHERE main_id = #{mainId} ORDER BY machine_code, sequence, id")
    List<ScheduleDetail> selectByMainId(@Param("mainId") Long mainId);

    /**
     * 根据机台和日期查询明细
     */
    @Select("SELECT * FROM t_cx_schedule_detail WHERE machine_code = #{machineCode} AND schedule_date = #{scheduleDate} ORDER BY sequence, id")
    List<ScheduleDetail> selectByMachineAndDate(@Param("machineCode") String machineCode, @Param("scheduleDate") String scheduleDate);

    /**
     * 根据班次查询明细
     */
    @Select("SELECT * FROM t_cx_schedule_detail WHERE main_id = #{mainId} AND shift_code = #{shiftCode} ORDER BY sequence, id")
    List<ScheduleDetail> selectByShift(@Param("mainId") Long mainId, @Param("shiftCode") String shiftCode);
}
