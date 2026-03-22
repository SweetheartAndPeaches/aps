package com.jinyu.aps.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinyu.aps.entity.ScheduleDetail;
import com.jinyu.aps.mapper.ScheduleDetailMapper;
import com.jinyu.aps.service.ScheduleDetailService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 排程明细Service实现类
 *
 * @author APS Team
 */
@Service
public class ScheduleDetailServiceImpl extends ServiceImpl<ScheduleDetailMapper, ScheduleDetail> implements ScheduleDetailService {

    @Override
    public List<ScheduleDetail> listByMainId(Long mainId) {
        LambdaQueryWrapper<ScheduleDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleDetail::getMainId, mainId)
                .orderByAsc(ScheduleDetail::getSequence)
                .orderByAsc(ScheduleDetail::getMachineCode)
                .orderByAsc(ScheduleDetail::getShiftCode);
        return list(wrapper);
    }

    @Override
    public List<ScheduleDetail> listByMachineAndDate(String machineCode, String scheduleDate) {
        LambdaQueryWrapper<ScheduleDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleDetail::getMachineCode, machineCode)
                .eq(ScheduleDetail::getScheduleDate, scheduleDate)
                .orderByAsc(ScheduleDetail::getSequence);
        return list(wrapper);
    }

    @Override
    public boolean updateCompletedQuantity(Long detailId, Integer completedQuantity) {
        ScheduleDetail detail = getById(detailId);
        if (detail == null) {
            return false;
        }
        detail.setCompletedQuantity(completedQuantity);
        if (completedQuantity >= detail.getPlanQuantity()) {
            detail.setStatus("COMPLETED");
        }
        return updateById(detail);
    }
}
