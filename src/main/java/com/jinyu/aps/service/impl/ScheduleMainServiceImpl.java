package com.jinyu.aps.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinyu.aps.entity.ScheduleMain;
import com.jinyu.aps.mapper.ScheduleMainMapper;
import com.jinyu.aps.service.ScheduleMainService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 排程主表Service实现类
 *
 * @author APS Team
 */
@Service
public class ScheduleMainServiceImpl extends ServiceImpl<ScheduleMainMapper, ScheduleMain> implements ScheduleMainService {

    @Override
    public ScheduleMain generateSchedule(LocalDateTime scheduleDate, Integer days) {
        ScheduleMain scheduleMain = new ScheduleMain();
        scheduleMain.setScheduleCode("SCH" + DateUtil.format(DateUtil.date(), "yyyyMMdd") + IdUtil.fastSimpleUUID().substring(0, 4).toUpperCase());
        scheduleMain.setScheduleDate(scheduleDate);
        scheduleMain.setScheduleType("NORMAL");
        scheduleMain.setStatus("DRAFT");
        scheduleMain.setTotalMachines(0);
        scheduleMain.setTotalQuantity(0);
        scheduleMain.setTotalVehicles(0);
        scheduleMain.setVersion(1);
        scheduleMain.setCreateTime(LocalDateTime.now());
        scheduleMain.setUpdateTime(LocalDateTime.now());
        save(scheduleMain);
        return scheduleMain;
    }

    @Override
    public boolean confirmSchedule(Long scheduleId) {
        ScheduleMain scheduleMain = getById(scheduleId);
        if (scheduleMain == null) {
            return false;
        }
        scheduleMain.setStatus("CONFIRMED");
        scheduleMain.setConfirmTime(LocalDateTime.now());
        return updateById(scheduleMain);
    }

    @Override
    public Page<ScheduleMain> pageList(Integer pageNum, Integer pageSize, LocalDateTime startDate, LocalDateTime endDate) {
        Page<ScheduleMain> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ScheduleMain> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null) {
            wrapper.ge(ScheduleMain::getScheduleDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(ScheduleMain::getScheduleDate, endDate);
        }
        wrapper.orderByDesc(ScheduleMain::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public ScheduleMain getByScheduleDate(LocalDateTime scheduleDate) {
        LambdaQueryWrapper<ScheduleMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleMain::getScheduleDate, scheduleDate)
                .orderByDesc(ScheduleMain::getVersion)
                .last("LIMIT 1");
        return getOne(wrapper);
    }
}
