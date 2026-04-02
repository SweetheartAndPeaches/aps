package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cx.vo.ScheduleQueryVo;
import com.zlt.aps.cx.vo.ScheduleResultVo;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.mapper.CxScheduleResultMapper;
import com.zlt.aps.cx.service.CxScheduleDetailService;
import com.zlt.aps.cx.service.CxScheduleResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 成型排程结果服务实现类
 *
 * @author APS Team
 */
@Slf4j
@Service
public class CxScheduleResultServiceImpl extends ServiceImpl<CxScheduleResultMapper, CxScheduleResult>
        implements CxScheduleResultService {

    @Autowired
    private CxScheduleDetailService cxScheduleDetailService;

    @Override
    public List<CxScheduleResult> listByScheduleDate(LocalDate scheduleDate) {
        return list(new LambdaQueryWrapper<CxScheduleResult>()
                .eq(CxScheduleResult::getScheduleDate, scheduleDate.atStartOfDay())
                .orderByAsc(CxScheduleResult::getCxMachineCode));
    }

    @Override
    public List<CxScheduleResult> listByMachineAndDate(String cxMachineCode, LocalDate scheduleDate) {
        return list(new LambdaQueryWrapper<CxScheduleResult>()
                .eq(CxScheduleResult::getCxMachineCode, cxMachineCode)
                .eq(CxScheduleResult::getScheduleDate, scheduleDate.atStartOfDay())
                .orderByAsc(CxScheduleResult::getId));
    }

    @Override
    public List<CxScheduleResult> listByEmbryoCode(String embryoCode) {
        return list(new LambdaQueryWrapper<CxScheduleResult>()
                .eq(CxScheduleResult::getEmbryoCode, embryoCode)
                .orderByDesc(CxScheduleResult::getScheduleDate));
    }

    @Override
    public Page<ScheduleResultVo> pageList(ScheduleQueryVo queryDTO) {
        Page<CxScheduleResult> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        
        LambdaQueryWrapper<CxScheduleResult> wrapper = new LambdaQueryWrapper<>();
        
        // 日期范围
        if (queryDTO.getStartDate() != null) {
            wrapper.ge(CxScheduleResult::getScheduleDate, queryDTO.getStartDate().atStartOfDay());
        }
        if (queryDTO.getEndDate() != null) {
            wrapper.le(CxScheduleResult::getScheduleDate, queryDTO.getEndDate().atStartOfDay());
        }
        
        // 其他条件
        if (StringUtils.hasText(queryDTO.getCxMachineCode())) {
            wrapper.eq(CxScheduleResult::getCxMachineCode, queryDTO.getCxMachineCode());
        }
        if (StringUtils.hasText(queryDTO.getEmbryoCode())) {
            wrapper.eq(CxScheduleResult::getEmbryoCode, queryDTO.getEmbryoCode());
        }
        if (StringUtils.hasText(queryDTO.getStructureName())) {
            wrapper.eq(CxScheduleResult::getStructureName, queryDTO.getStructureName());
        }
        if (StringUtils.hasText(queryDTO.getProductionStatus())) {
            wrapper.eq(CxScheduleResult::getProductionStatus, queryDTO.getProductionStatus());
        }
        if (StringUtils.hasText(queryDTO.getIsRelease())) {
            wrapper.eq(CxScheduleResult::getIsRelease, queryDTO.getIsRelease());
        }
        
        wrapper.orderByDesc(CxScheduleResult::getScheduleDate)
               .orderByAsc(CxScheduleResult::getCxMachineCode);
        
        Page<CxScheduleResult> resultPage = page(page, wrapper);
        
        // 转换为DTO
        Page<ScheduleResultVo> dtoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        dtoPage.setRecords(resultPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));
        
        return dtoPage;
    }

    @Override
    public ScheduleResultVo getDetailById(Long id) {
        CxScheduleResult result = getById(id);
        if (result == null) {
            return null;
        }
        
        ScheduleResultVo dto = convertToDTO(result);
        
        // 查询明细
        List<CxScheduleDetail> details = cxScheduleDetailService.listByMainId(id);
        dto.setDetails(details.stream().map(this::convertDetailToDTO).collect(Collectors.toList()));
        
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProductionStatus(Long id, String productionStatus) {
        return update(new LambdaUpdateWrapper<CxScheduleResult>()
                .eq(CxScheduleResult::getId, id)
                .set(CxScheduleResult::getProductionStatus, productionStatus));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseSchedule(Long id) {
        return update(new LambdaUpdateWrapper<CxScheduleResult>()
                .eq(CxScheduleResult::getId, id)
                .set(CxScheduleResult::getIsRelease, "1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchReleaseSchedule(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return update(new LambdaUpdateWrapper<CxScheduleResult>()
                .in(CxScheduleResult::getId, ids)
                .set(CxScheduleResult::getIsRelease, "1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateShiftPlanQty(Long id, String shiftCode, BigDecimal planQty) {
        LambdaUpdateWrapper<CxScheduleResult> wrapper = new LambdaUpdateWrapper<CxScheduleResult>()
                .eq(CxScheduleResult::getId, id);
        
        switch (shiftCode) {
            case "SHIFT1":
                wrapper.set(CxScheduleResult::getClass1PlanQty, planQty);
                break;
            case "SHIFT2":
                wrapper.set(CxScheduleResult::getClass2PlanQty, planQty);
                break;
            case "SHIFT3":
                wrapper.set(CxScheduleResult::getClass3PlanQty, planQty);
                break;
            case "SHIFT4":
                wrapper.set(CxScheduleResult::getClass4PlanQty, planQty);
                break;
            case "SHIFT5":
                wrapper.set(CxScheduleResult::getClass5PlanQty, planQty);
                break;
            case "SHIFT6":
                wrapper.set(CxScheduleResult::getClass6PlanQty, planQty);
                break;
            case "SHIFT7":
                wrapper.set(CxScheduleResult::getClass7PlanQty, planQty);
                break;
            case "SHIFT8":
                wrapper.set(CxScheduleResult::getClass8PlanQty, planQty);
                break;
            default:
                return false;
        }
        
        return update(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateShiftFinishQty(Long id, String shiftCode, BigDecimal finishQty) {
        LambdaUpdateWrapper<CxScheduleResult> wrapper = new LambdaUpdateWrapper<CxScheduleResult>()
                .eq(CxScheduleResult::getId, id);
        
        switch (shiftCode) {
            case "SHIFT1":
                wrapper.set(CxScheduleResult::getClass1FinishQty, finishQty);
                break;
            case "SHIFT2":
                wrapper.set(CxScheduleResult::getClass2FinishQty, finishQty);
                break;
            case "SHIFT3":
                wrapper.set(CxScheduleResult::getClass3FinishQty, finishQty);
                break;
            case "SHIFT4":
                wrapper.set(CxScheduleResult::getClass4FinishQty, finishQty);
                break;
            case "SHIFT5":
                wrapper.set(CxScheduleResult::getClass5FinishQty, finishQty);
                break;
            case "SHIFT6":
                wrapper.set(CxScheduleResult::getClass6FinishQty, finishQty);
                break;
            case "SHIFT7":
                wrapper.set(CxScheduleResult::getClass7FinishQty, finishQty);
                break;
            case "SHIFT8":
                wrapper.set(CxScheduleResult::getClass8FinishQty, finishQty);
                break;
            default:
                return false;
        }
        
        return update(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByScheduleDate(LocalDate scheduleDate) {
        // 先删除明细
        List<CxScheduleResult> results = listByScheduleDate(scheduleDate);
        for (CxScheduleResult result : results) {
            cxScheduleDetailService.deleteByMainId(result.getId());
        }
        
        // 删除主表
        return remove(new LambdaQueryWrapper<CxScheduleResult>()
                .eq(CxScheduleResult::getScheduleDate, scheduleDate.atStartOfDay()));
    }

    @Override
    public String generateBatchNo(LocalDate scheduleDate) {
        String dateStr = scheduleDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "CX" + dateStr + randomStr;
    }

    /**
     * 转换为DTO
     */
    private ScheduleResultVo convertToDTO(CxScheduleResult entity) {
        ScheduleResultVo dto = new ScheduleResultVo();
        BeanUtils.copyProperties(entity, dto);
        if (entity.getScheduleDate() != null) {
            dto.setScheduleDate(entity.getScheduleDate().toLocalDate());
        }
        return dto;
    }

    /**
     * 转换明细为DTO
     */
    private ScheduleResultVo.ScheduleDetailVo convertDetailToDTO(CxScheduleDetail detail) {
        ScheduleResultVo.ScheduleDetailVo dto = new ScheduleResultVo.ScheduleDetailVo();
        BeanUtils.copyProperties(detail, dto);
        return dto;
    }
}
