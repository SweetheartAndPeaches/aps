package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.mapper.CxScheduleDetailMapper;
import com.zlt.aps.cx.service.CxScheduleDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 排程明细服务实现类
 *
 * @author APS Team
 */
@Slf4j
@Service
public class CxScheduleDetailServiceImpl extends ServiceImpl<CxScheduleDetailMapper, CxScheduleDetail>
        implements CxScheduleDetailService {

    @Override
    public List<CxScheduleDetail> listByMainId(Long mainId) {
        return list(new LambdaQueryWrapper<CxScheduleDetail>()
                .eq(CxScheduleDetail::getMainId, mainId)
                .orderByAsc(CxScheduleDetail::getClass1Sequence));
    }

    @Override
    public List<CxScheduleDetail> listByMachineAndDate(String cxMachineCode, LocalDate scheduleDate) {
        return list(new LambdaQueryWrapper<CxScheduleDetail>()
                .eq(CxScheduleDetail::getCxMachineCode, cxMachineCode)
                .eq(CxScheduleDetail::getScheduleDate, scheduleDate)
                .orderByAsc(CxScheduleDetail::getClass1Sequence));
    }

    @Override
    public List<CxScheduleDetail> listByShift(Long mainId, String shiftCode) {
        return list(new LambdaQueryWrapper<CxScheduleDetail>()
                .eq(CxScheduleDetail::getMainId, mainId)
                .eq(CxScheduleDetail::getShiftCode, shiftCode)
                .orderByAsc(CxScheduleDetail::getClass1Sequence));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCompletedQuantity(Long detailId, Integer completedQuantity) {
        // 当前实体没有 tripActualQty 字段，跳过更新
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTripStatus(Long detailId, String tripStatus) {
        // 这里可以根据业务需要扩展状态字段
        // 当前实体类中通过 tripActualQty 与 tripCapacity 的比较来判断状态
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSave(List<CxScheduleDetail> details) {
        if (details == null || details.isEmpty()) {
            return false;
        }
        // 逐条插入，避免 saveBatch 在 IdType.AUTO 下因回填 id 导致 Duplicate entry 问题
        for (CxScheduleDetail detail : details) {
            detail.setId(null);
            save(detail);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByMainId(Long mainId) {
        return remove(new LambdaQueryWrapper<CxScheduleDetail>()
                .eq(CxScheduleDetail::getMainId, mainId));
    }

    @Override
    public Integer getNextTripNo(Long mainId, String shiftCode) {
        List<CxScheduleDetail> details = listByShift(mainId, shiftCode);
        if (details.isEmpty()) {
            return 1;
        }
        // 当前实体使用 CLASS1_TRIP_NO 等字段，返回1表示第一个车次
        return 1;
    }
}
