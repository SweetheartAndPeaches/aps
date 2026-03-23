package com.zlt.aps.cx.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cx.entity.ScheduleMain;
import com.zlt.aps.cx.mapper.ScheduleMainMapper;
import com.zlt.aps.cx.service.ScheduleMainService;
import com.zlt.aps.cx.service.ScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 排程主表Service实现类
 *
 * @author APS Team
 */
@Service
public class ScheduleMainServiceImpl extends ServiceImpl<ScheduleMainMapper, ScheduleMain> implements ScheduleMainService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleMainServiceImpl.class);

    @Autowired
    private ScheduleService scheduleService;

    @Override
    public ScheduleMain generateSchedule(LocalDateTime scheduleDate, Integer days) {
        logger.info("开始生成排程，日期: {}, 天数: {}", scheduleDate, days);
        
        // 调用 ScheduleService 的完整排程生成逻辑
        LocalDate date = scheduleDate.toLocalDate();
        ScheduleService.ScheduleGenerateResult result = scheduleService.generateSchedule(date);
        
        if (result.isSuccess()) {
            logger.info("排程生成成功，总机台数: {}, 总计划量: {}", 
                result.getTotalMachines(), result.getTotalQuantity());
            return result.getScheduleMain();
        } else {
            logger.error("排程生成失败: {}", result.getMessage());
            throw new RuntimeException("排程生成失败: " + result.getMessage());
        }
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
