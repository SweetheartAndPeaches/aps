package com.jinyu.aps.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinyu.aps.entity.DailyEmbryoTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 日胎胚任务Mapper
 *
 * @author APS Team
 */
@Mapper
public interface DailyEmbryoTaskMapper extends BaseMapper<DailyEmbryoTask> {

    /**
     * 根据排程主表ID查询任务
     */
    @Select("SELECT * FROM t_cx_daily_embryo_task WHERE schedule_main_id = #{mainId} ORDER BY sort_order, id")
    List<DailyEmbryoTask> selectByMainId(@Param("mainId") Long mainId);
}
