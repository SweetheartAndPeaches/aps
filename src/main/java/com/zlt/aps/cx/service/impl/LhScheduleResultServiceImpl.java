package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.mapper.LhScheduleResultMapper;
import com.zlt.aps.cx.service.LhScheduleResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 硫化排程结果服务实现类
 *
 * @author APS Team
 */
@Slf4j
@Service
public class LhScheduleResultServiceImpl extends ServiceImpl<LhScheduleResultMapper, LhScheduleResult>
        implements LhScheduleResultService {

    @Override
    public List<LhScheduleResult> listByScheduleDate(LocalDate scheduleDate) {
        return list(new LambdaQueryWrapper<LhScheduleResult>()
                .eq(LhScheduleResult::getScheduleDate, scheduleDate)
                .orderByAsc(LhScheduleResult::getLhMachineCode)
                .orderByAsc(LhScheduleResult::getMachineOrder));
    }

    @Override
    public List<LhScheduleResult> listByMachineAndDate(String lhMachineCode, LocalDate scheduleDate) {
        return list(new LambdaQueryWrapper<LhScheduleResult>()
                .eq(LhScheduleResult::getLhMachineCode, lhMachineCode)
                .eq(LhScheduleResult::getScheduleDate, scheduleDate)
                .orderByAsc(LhScheduleResult::getMachineOrder));
    }

    @Override
    public List<LhScheduleResult> listByEmbryoCode(String embryoCode) {
        return list(new LambdaQueryWrapper<LhScheduleResult>()
                .eq(LhScheduleResult::getEmbryoCode, embryoCode)
                .orderByDesc(LhScheduleResult::getScheduleDate));
    }

    @Override
    public Page<LhScheduleResult> pageList(LocalDate scheduleDate, String lhMachineCode, Integer pageNum, Integer pageSize) {
        Page<LhScheduleResult> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<LhScheduleResult> wrapper = new LambdaQueryWrapper<>();
        
        if (scheduleDate != null) {
            wrapper.eq(LhScheduleResult::getScheduleDate, scheduleDate);
        }
        if (StringUtils.hasText(lhMachineCode)) {
            wrapper.eq(LhScheduleResult::getLhMachineCode, lhMachineCode);
        }
        
        wrapper.orderByDesc(LhScheduleResult::getScheduleDate)
               .orderByAsc(LhScheduleResult::getLhMachineCode)
               .orderByAsc(LhScheduleResult::getMachineOrder);
        
        return page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProductionStatus(Long id, String productionStatus) {
        return update(new LambdaUpdateWrapper<LhScheduleResult>()
                .eq(LhScheduleResult::getId, id)
                .set(LhScheduleResult::getProductionStatus, productionStatus));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseSchedule(Long id) {
        return update(new LambdaUpdateWrapper<LhScheduleResult>()
                .eq(LhScheduleResult::getId, id)
                .set(LhScheduleResult::getIsRelease, "1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchReleaseSchedule(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return update(new LambdaUpdateWrapper<LhScheduleResult>()
                .in(LhScheduleResult::getId, ids)
                .set(LhScheduleResult::getIsRelease, "1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateShiftPlanQty(Long id, Integer shiftNo, Integer planQty) {
        LambdaUpdateWrapper<LhScheduleResult> wrapper = new LambdaUpdateWrapper<LhScheduleResult>()
                .eq(LhScheduleResult::getId, id);
        
        switch (shiftNo) {
            case 1:
                wrapper.set(LhScheduleResult::getClass1PlanQty, planQty);
                break;
            case 2:
                wrapper.set(LhScheduleResult::getClass2PlanQty, planQty);
                break;
            case 3:
                wrapper.set(LhScheduleResult::getClass3PlanQty, planQty);
                break;
            case 4:
                wrapper.set(LhScheduleResult::getClass4PlanQty, planQty);
                break;
            default:
                return false;
        }
        
        return update(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateShiftFinishQty(Long id, Integer shiftNo, Integer finishQty) {
        LambdaUpdateWrapper<LhScheduleResult> wrapper = new LambdaUpdateWrapper<LhScheduleResult>()
                .eq(LhScheduleResult::getId, id);
        
        switch (shiftNo) {
            case 1:
                wrapper.set(LhScheduleResult::getClass1FinishQty, finishQty);
                break;
            case 2:
                wrapper.set(LhScheduleResult::getClass2FinishQty, finishQty);
                break;
            case 3:
                wrapper.set(LhScheduleResult::getClass3FinishQty, finishQty);
                break;
            case 4:
                wrapper.set(LhScheduleResult::getClass4FinishQty, finishQty);
                break;
            default:
                return false;
        }
        
        return update(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByScheduleDate(LocalDate scheduleDate) {
        return remove(new LambdaQueryWrapper<LhScheduleResult>()
                .eq(LhScheduleResult::getScheduleDate, scheduleDate));
    }

    @Override
    public List<LhScheduleResult> listByCxScheduleId(Long cxScheduleId) {
        // 通过成型排程的 lhScheduleIds 字段关联
        // 这里需要根据实际业务逻辑实现
        return list(new LambdaQueryWrapper<LhScheduleResult>()
                .isNotNull(LhScheduleResult::getBatchNo)
                .orderByAsc(LhScheduleResult::getLhMachineCode));
    }

    @Override
    public Integer calculateShiftCapacity(String lhMachineCode, Integer mouldQty, Integer lhTime) {
        if (mouldQty == null || mouldQty == 0 || lhTime == null || lhTime == 0) {
            return 0;
        }
        
        // 假设每班工作8小时
        int shiftMinutes = 8 * 60;
        
        // 单班产能 = (班次分钟数 / 硫化时长分钟数) * 模数
        // 硫化时长从秒转换为分钟
        double lhTimeMinutes = lhTime / 60.0;
        
        if (lhTimeMinutes <= 0) {
            return 0;
        }
        
        // 计算每班可以硫化多少次
        int cyclesPerShift = (int) (shiftMinutes / lhTimeMinutes);
        
        // 单班产能 = 硫化次数 * 模数
        return cyclesPerShift * mouldQty;
    }
}
