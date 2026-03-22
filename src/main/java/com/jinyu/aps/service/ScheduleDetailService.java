package com.jinyu.aps.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jinyu.aps.entity.ScheduleDetail;

import java.util.List;

/**
 * 排程明细Service接口
 *
 * @author APS Team
 */
public interface ScheduleDetailService extends IService<ScheduleDetail> {

    /**
     * 根据主表ID获取明细
     */
    List<ScheduleDetail> listByMainId(Long mainId);

    /**
     * 根据机台和日期获取明细
     */
    List<ScheduleDetail> listByMachineAndDate(String machineCode, String scheduleDate);

    /**
     * 更新完成量
     */
    boolean updateCompletedQuantity(Long detailId, Integer completedQuantity);
}
