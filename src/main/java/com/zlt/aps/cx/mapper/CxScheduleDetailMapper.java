package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 排程明细Mapper接口
 *
 * @author APS Team
 * @since 2.0.0
 */
@Mapper
public interface CxScheduleDetailMapper extends BaseMapper<CxScheduleDetail> {

    /**
     * 根据主表ID查询主表信息
     */
    @Select("SELECT * FROM t_cx_schedule_result WHERE id = #{id}")
    CxScheduleResult selectMainById(@Param("id") Long id);

    /**
     * 根据机台编号和日期查询主表信息
     */
    @Select("<script>" +
            "SELECT * FROM t_cx_schedule_result " +
            "WHERE cx_machine_code = #{machineCode} " +
            "AND schedule_date = #{scheduleDate} " +
            "ORDER BY embryo_code ASC" +
            "</script>")
    List<CxScheduleResult> selectMainByMachineAndDate(@Param("machineCode") String cxMachineCode,
                                                       @Param("scheduleDate") LocalDate scheduleDate);

    /**
     * 根据机台编号范围和日期范围查询主表信息（按机台降序+胎胚排序）
     */
    @Select("<script>" +
            "SELECT * FROM t_cx_schedule_result " +
            "WHERE 1=1 " +
            "<if test='machineCodeStart != null'> AND cx_machine_code &gt;= #{machineCodeStart} </if>" +
            "<if test='machineCodeEnd != null'> AND cx_machine_code &lt;= #{machineCodeEnd} </if>" +
            "<if test='scheduleDateStart != null'> AND schedule_date &gt;= #{scheduleDateStart} </if>" +
            "<if test='scheduleDateEnd != null'> AND schedule_date &lt;= #{scheduleDateEnd} </if>" +
            "ORDER BY cx_machine_code DESC, embryo_code ASC" +
            "</script>")
    List<CxScheduleResult> selectMainByMachineAndDateRange(@Param("machineCodeStart") String machineCodeStart,
                                                            @Param("machineCodeEnd") String machineCodeEnd,
                                                            @Param("scheduleDateStart") LocalDate scheduleDateStart,
                                                            @Param("scheduleDateEnd") LocalDate scheduleDateEnd);
}
