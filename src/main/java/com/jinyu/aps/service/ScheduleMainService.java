package com.jinyu.aps.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jinyu.aps.entity.ScheduleMain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 排程主表Service接口
 *
 * @author APS Team
 */
public interface ScheduleMainService extends IService<ScheduleMain> {

    /**
     * 生成交互排程
     */
    ScheduleMain generateSchedule(LocalDateTime scheduleDate, Integer days);

    /**
     * 确认排程
     */
    boolean confirmSchedule(Long scheduleId);

    /**
     * 分页查询排程
     */
    Page<ScheduleMain> pageList(Integer pageNum, Integer pageSize, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 根据日期获取排程
     */
    ScheduleMain getByScheduleDate(LocalDateTime scheduleDate);
}
