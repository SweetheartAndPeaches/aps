package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.schedule.CxScheduleAlgorithmLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 成型算法日志Mapper
 *
 * @author APS Team
 */
@Mapper
public interface CxScheduleAlgorithmLogMapper extends BaseMapper<CxScheduleAlgorithmLog> {

    /**
     * 根据排程日期删除历史日志（只保留最新一次排程的日志）
     *
     * @param scheduleDate 排程日期
     * @param factoryCode 工厂编码
     * @return 删除数量
     */
    int deleteByScheduleDateAndFactory(@Param("scheduleDate") LocalDate scheduleDate,
                                       @Param("factoryCode") String factoryCode);

    /**
     * 查询指定排程日期的日志
     *
     * @param scheduleDate 排程日期
     * @return 日志列表
     */
    @Select("SELECT * FROM T_CX_SCHEDULE_ALGORITHM_LOG WHERE SCHEDULE_DATE = #{scheduleDate} ORDER BY STRUCTURE_NAME")
    List<CxScheduleAlgorithmLog> selectByScheduleDate(@Param("scheduleDate") LocalDate scheduleDate);

    /**
     * 查询指定排程日期和结构的日志
     *
     * @param scheduleDate 排程日期
     * @param structureName 结构名称
     * @return 日志列表
     */
    @Select("SELECT * FROM T_CX_SCHEDULE_ALGORITHM_LOG WHERE SCHEDULE_DATE = #{scheduleDate} AND STRUCTURE_NAME = #{structureName}")
    List<CxScheduleAlgorithmLog> selectByScheduleDateAndStructure(@Param("scheduleDate") LocalDate scheduleDate,
                                                                    @Param("structureName") String structureName);

    /**
     * 查询最新的排程日志
     *
     * @param limit 返回数量
     * @return 日志列表
     */
    @Select("SELECT * FROM T_CX_SCHEDULE_ALGORITHM_LOG ORDER BY CREATE_TIME DESC LIMIT #{limit}")
    List<CxScheduleAlgorithmLog> selectLatestLogs(@Param("limit") int limit);
}
