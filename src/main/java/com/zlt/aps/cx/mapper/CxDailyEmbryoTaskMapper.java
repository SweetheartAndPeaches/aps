package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.CxDailyEmbryoTask;
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
public interface CxDailyEmbryoTaskMapper extends BaseMapper<CxDailyEmbryoTask> {

    /**
     * 根据排程主表ID查询任务
     */
    @Select("SELECT * FROM T_CX_DAILY_EMBRYO_TASK WHERE SCHEDULE_MAIN_ID = #{mainId} ORDER BY SORT_ORDER, ID")
    List<CxDailyEmbryoTask> selectByMainId(@Param("mainId") Long mainId);
}
