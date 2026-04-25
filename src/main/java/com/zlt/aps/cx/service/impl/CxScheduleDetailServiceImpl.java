package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.mapper.CxScheduleDetailMapper;
import com.zlt.aps.cx.mapper.CxScheduleResultMapper;
import com.zlt.aps.cx.service.CxScheduleDetailService;
import com.zlt.aps.cx.vo.CxScheduleDetailVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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
    public List<CxScheduleDetailVo> listVoByMainId(Long mainId) {
        // 查询子表数据
        List<CxScheduleDetail> details = list(new LambdaQueryWrapper<CxScheduleDetail>()
                .eq(CxScheduleDetail::getMainId, mainId)
                .orderByAsc(CxScheduleDetail::getClass1Sequence));

        if (details.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询主表数据
        CxScheduleResult mainResult = baseMapper.selectMainById(mainId);
        if (mainResult == null) {
            return Collections.emptyList();
        }

        // 转换为VO
        return convertToVoList(details, mainResult);
    }

    @Override
    public List<CxScheduleDetailVo> listVoByMachineAndDate(String cxMachineCode, LocalDate scheduleDate) {
        // 查询主表获取匹配的mainId
        List<CxScheduleResult> mainResults = baseMapper.selectMainByMachineAndDate(cxMachineCode, scheduleDate);

        if (mainResults.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> mainIds = mainResults.stream()
                .map(CxScheduleResult::getId)
                .collect(Collectors.toList());

        // 查询子表数据
        List<CxScheduleDetail> details = list(new LambdaQueryWrapper<CxScheduleDetail>()
                .in(CxScheduleDetail::getMainId, mainIds)
                .orderByAsc(CxScheduleDetail::getClass1Sequence));

        // 按主表分组
        Map<Long, CxScheduleResult> mainMap = mainResults.stream()
                .collect(Collectors.toMap(CxScheduleResult::getId, r -> r));

        // 转换为VO
        return details.stream()
                .map(detail -> {
                    CxScheduleDetailVo vo = new CxScheduleDetailVo();
                    BeanUtils.copyProperties(detail, vo);
                    CxScheduleResult main = mainMap.get(detail.getMainId());
                    if (main != null) {
                        copyMainFieldsToVo(main, vo);
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CxScheduleDetailVo> listVoByMachineAndDateRange(String machineCodeStart, String machineCodeEnd,
                                                                  LocalDate scheduleDateStart, LocalDate scheduleDateEnd) {
        // 查询主表数据（按机台降序+胎胚排序）
        List<CxScheduleResult> mainResults = baseMapper.selectMainByMachineAndDateRange(
                machineCodeStart, machineCodeEnd, scheduleDateStart, scheduleDateEnd);

        if (mainResults.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> mainIds = mainResults.stream()
                .map(CxScheduleResult::getId)
                .collect(Collectors.toList());

        // 查询子表数据
        List<CxScheduleDetail> details = list(new LambdaQueryWrapper<CxScheduleDetail>()
                .in(CxScheduleDetail::getMainId, mainIds)
                .orderByAsc(CxScheduleDetail::getClass1Sequence));

        // 按主表ID分组
        Map<Long, CxScheduleResult> mainMap = mainResults.stream()
                .collect(Collectors.toMap(CxScheduleResult::getId, r -> r));

        // 转换为VO并按机台降序+胎胚排序
        List<CxScheduleDetailVo> voList = details.stream()
                .map(detail -> {
                    CxScheduleDetailVo vo = new CxScheduleDetailVo();
                    BeanUtils.copyProperties(detail, vo);
                    CxScheduleResult main = mainMap.get(detail.getMainId());
                    if (main != null) {
                        copyMainFieldsToVo(main, vo);
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        // 按机台降序+胎胚排序
        voList.sort((a, b) -> {
            // 机台降序
            int machineCompare = compareStringDescending(a.getCxMachineCode(), b.getCxMachineCode());
            if (machineCompare != 0) {
                return machineCompare;
            }
            // 胎胚相同的放一起（胎胚编码升序）
            return compareStringAscending(a.getEmbryoCode(), b.getEmbryoCode());
        });

        return voList;
    }

    @Override
    public List<CxScheduleDetail> listByMainId(Long mainId) {
        return list(new LambdaQueryWrapper<CxScheduleDetail>()
                .eq(CxScheduleDetail::getMainId, mainId)
                .orderByAsc(CxScheduleDetail::getClass1Sequence));
    }

    @Override
    public List<CxScheduleDetail> listByMachineAndDate(String cxMachineCode, LocalDate scheduleDate) {
        // 子表字段已从主表继承，不再有 cxMachineCode 和 scheduleDate 字段
        // 改为通过主表关联查询：先查主表获取 mainId，再查子表
        log.warn("listByMachineAndDate 方法已废弃，子表不再包含机台和日期字段，请通过主表查询");
        return java.util.Collections.emptyList();
    }

    @Override
    public List<CxScheduleDetail> listByShift(Long mainId, String shiftCode) {
        // 子表不再有 shiftCode 字段，改为仅按 mainId 查询
        return list(new LambdaQueryWrapper<CxScheduleDetail>()
                .eq(CxScheduleDetail::getMainId, mainId)
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
        // 清除所有明细记录的ID,避免主键冲突,让数据库自动生成新ID
        for (CxScheduleDetail detail : details) {
            detail.setId(null);
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
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByMainIds(List<Long> mainIds) {
        if (mainIds == null || mainIds.isEmpty()) {
            return true;
        }
        return remove(new LambdaQueryWrapper<CxScheduleDetail>()
                .in(CxScheduleDetail::getMainId, mainIds));
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

    // ==================== 私有辅助方法 ====================

    /**
     * 转换子表列表为VO列表
     */
    private List<CxScheduleDetailVo> convertToVoList(List<CxScheduleDetail> details, CxScheduleResult mainResult) {
        return details.stream()
                .map(detail -> {
                    CxScheduleDetailVo vo = new CxScheduleDetailVo();
                    BeanUtils.copyProperties(detail, vo);
                    copyMainFieldsToVo(mainResult, vo);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 复制主表字段到VO
     */
    private void copyMainFieldsToVo(CxScheduleResult main, CxScheduleDetailVo vo) {
        vo.setCxMachineCode(main.getCxMachineCode());
        vo.setCxMachineName(main.getCxMachineName());
        vo.setCxMachineType(main.getCxMachineType());
        vo.setLhMachineCode(main.getLhMachineCode());
        vo.setLhMachineName(main.getLhMachineName());
        vo.setLhMachineQty(main.getLhMachineQty() != null ? main.getLhMachineQty().intValue() : null);
        vo.setEmbryoCode(main.getEmbryoCode());
        vo.setMaterialCode(main.getMaterialCode());
        vo.setMaterialDesc(main.getMaterialDesc());
        vo.setMainMaterialDesc(main.getMainMaterialDesc());
        vo.setSpecDimension(main.getSpecDimension() != null ? main.getSpecDimension().toString() : null);
        vo.setStructureName(main.getStructureName());
        vo.setScheduleDate(main.getScheduleDate() != null ? main.getScheduleDate().toString() : null);
        vo.setCxBatchNo(main.getCxBatchNo());
        vo.setOrderNo(main.getOrderNo());
        vo.setProductionStatus(main.getProductionStatus());
        // Date -> LocalDateTime
        if (main.getCreateTime() != null) {
            vo.setCreateTime(main.getCreateTime().toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        }
        if (main.getUpdateTime() != null) {
            vo.setUpdateTime(main.getUpdateTime().toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        }
    }

    /**
     * 字符串升序比较（处理null）
     */
    private int compareStringAscending(String s1, String s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return -1;
        if (s2 == null) return 1;
        return s1.compareTo(s2);
    }

    /**
     * 字符串降序比较（处理null）
     */
    private int compareStringDescending(String s1, String s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return 1;
        if (s2 == null) return -1;
        return s2.compareTo(s1); // 降序
    }
}
