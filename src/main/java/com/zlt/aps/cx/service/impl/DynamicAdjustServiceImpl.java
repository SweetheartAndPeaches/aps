package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zlt.aps.cx.entity.*;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.mapper.*;
import com.zlt.aps.cx.service.DynamicAdjustService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 动态调整服务实现类
 *
 * @author APS Team
 */
@Slf4j
@Service
public class DynamicAdjustServiceImpl implements DynamicAdjustService {

    @Autowired
    private CxStockMapper stockMapper;

    @Autowired
    private MdmMaterialInfoMapper materialInfoMapper;

    @Autowired
    private CxScheduleResultMapper scheduleResultMapper;

    @Autowired
    private CxScheduleDetailMapper scheduleDetailMapper;

    @Autowired
    private CxMaterialExceptionMapper materialExceptionMapper;

    @Autowired
    private CxParamConfigMapper paramConfigMapper;

    @Autowired
    private LhScheduleResultMapper lhScheduleResultMapper;

    @Override
    public ShiftAdjustResult checkAndAdjustBeforeShiftEnd(LocalDateTime scheduleDate, String shiftCode) {
        ShiftAdjustResult result = new ShiftAdjustResult();
        result.setAdjusted(false);
        result.setAdjustedMaterials(new ArrayList<>());
        result.setWarningMaterials(new ArrayList<>());

        // 获取需要调整的物料列表
        AdjustmentMaterialList adjustmentList = getAdjustmentMaterials(scheduleDate);

        List<StockInfo> lowStockMaterials = adjustmentList.getLowStockMaterials();
        List<StockInfo> highStockMaterials = adjustmentList.getHighStockMaterials();

        // 添加预警物料
        for (StockInfo stock : lowStockMaterials) {
            result.getWarningMaterials().add(String.format("%s库存低，可供时长%.2f小时",
                    stock.getMaterialName(), stock.getAvailableHours()));
        }

        // 尝试调整
        if (!CollectionUtils.isEmpty(lowStockMaterials) && !CollectionUtils.isEmpty(highStockMaterials)) {
            String nextShiftCode = getNextShiftCode(shiftCode);

            // 找一对可以交换的物料
            for (StockInfo lowStock : lowStockMaterials) {
                for (StockInfo highStock : highStockMaterials) {
                    boolean adjusted = adjustPlanQuantity(
                            lowStock.getMaterialCode(),
                            highStock.getMaterialCode(),
                            scheduleDate,
                            nextShiftCode);

                    if (adjusted) {
                        result.setAdjusted(true);
                        result.getAdjustedMaterials().add(lowStock.getMaterialCode());
                        result.getAdjustedMaterials().add(highStock.getMaterialCode());
                        result.setMessage(String.format("已将%s加量，%s减量",
                                lowStock.getMaterialName(), highStock.getMaterialName()));
                        return result;
                    }
                }
            }
        }

        if (!result.getAdjusted()) {
            result.setMessage("无需调整或无法找到合适的调整方案");
        }

        return result;
    }

    @Override
    public Integer calculateExpectedShiftEndStock(String materialCode, LocalDateTime scheduleDate, String shiftCode) {
        // 获取当前库存
        CxStock stock = stockMapper.selectOne(
                new LambdaQueryWrapper<CxStock>()
                        .eq(CxStock::getEmbryoCode, materialCode));

        if (stock == null) {
            return 0;
        }

        Integer currentStock = stock.getStockNum() != null ? stock.getStockNum() : 0;

        // 获取本班已完成的数量
        List<CxScheduleDetail> completedDetails = scheduleDetailMapper.selectList(
                new LambdaQueryWrapper<CxScheduleDetail>()
                        .eq(CxScheduleDetail::getMaterialCode, materialCode)
                        .eq(CxScheduleDetail::getScheduleDate, scheduleDate.toLocalDate())
                        .eq(CxScheduleDetail::getShiftCode, shiftCode)
                        .eq(CxScheduleDetail::getStatus, "COMPLETED"));

        Integer completedQty = completedDetails.stream()
                .mapToInt(d -> d.getActualQty() != null ? d.getActualQty() : 0)
                .sum();

        // 获取本班剩余计划数量
        List<CxScheduleDetail> remainingDetails = scheduleDetailMapper.selectList(
                new LambdaQueryWrapper<CxScheduleDetail>()
                        .eq(CxScheduleDetail::getMaterialCode, materialCode)
                        .eq(CxScheduleDetail::getScheduleDate, scheduleDate.toLocalDate())
                        .eq(CxScheduleDetail::getShiftCode, shiftCode)
                        .eq(CxScheduleDetail::getStatus, "PLANNED"));

        Integer remainingQty = remainingDetails.stream()
                .mapToInt(d -> d.getPlanQty() != null ? d.getPlanQty() : 0)
                .sum();

        // 获取本班硫化消耗（简化处理，实际需要根据硫化计划计算）
        Integer lhConsumption = calculateLhConsumption(materialCode, scheduleDate, shiftCode);

        // 预计交班库存 = 当前库存 + 本班已做 + 本班剩余计划 - 本班剩余消耗
        return currentStock + completedQty + remainingQty - lhConsumption;
    }

    @Override
    public BigDecimal calculateShiftEndAvailableHours(String materialCode, Integer expectedStock, BigDecimal remainingShiftHours) {
        // 获取硫化机数量
        int lhMachineCount = getLhMachineCount(materialCode);

        if (lhMachineCount == 0) {
            return BigDecimal.ZERO;
        }

        // 平均单台模数（假设固定值）
        int avgMoldCount = 60;

        // 交班可供时长 = 预计交班库存 / (硫化机数×单台模数) - 剩余班次时间
        BigDecimal totalConsumptionPerHour = BigDecimal.valueOf(lhMachineCount * avgMoldCount);

        if (totalConsumptionPerHour.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalAvailableHours = BigDecimal.valueOf(expectedStock)
                .divide(totalConsumptionPerHour, 2, RoundingMode.HALF_UP);

        return totalAvailableHours.subtract(remainingShiftHours).max(BigDecimal.ZERO);
    }

    @Override
    public String checkStockAlert(String materialCode, BigDecimal availableHours) {
        if (availableHours.compareTo(new BigDecimal("6")) < 0) {
            return "LOW"; // 断料风险
        } else if (availableHours.compareTo(new BigDecimal("18")) > 0) {
            return "HIGH"; // 库存过高
        }
        return "NORMAL";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adjustPlanQuantity(String lowStockMaterial, String highStockMaterial,
                                      LocalDateTime scheduleDate, String nextShiftCode) {
        try {
            // 获取低库存物料的计划
            List<CxScheduleDetail> lowStockPlans = scheduleDetailMapper.selectList(
                    new LambdaQueryWrapper<CxScheduleDetail>()
                            .eq(CxScheduleDetail::getMaterialCode, lowStockMaterial)
                            .eq(CxScheduleDetail::getScheduleDate, scheduleDate.toLocalDate().plusDays(1))
                            .eq(CxScheduleDetail::getShiftCode, nextShiftCode)
                            .orderByAsc(CxScheduleDetail::getSequence));

            // 获取高库存物料的计划
            List<CxScheduleDetail> highStockPlans = scheduleDetailMapper.selectList(
                    new LambdaQueryWrapper<CxScheduleDetail>()
                            .eq(CxScheduleDetail::getMaterialCode, highStockMaterial)
                            .eq(CxScheduleDetail::getScheduleDate, scheduleDate.toLocalDate().plusDays(1))
                            .eq(CxScheduleDetail::getShiftCode, nextShiftCode)
                            .orderByAsc(CxScheduleDetail::getSequence));

            if (CollectionUtils.isEmpty(highStockPlans)) {
                return false;
            }

            // 找一个高库存的计划减12条
            CxScheduleDetail highStockPlan = highStockPlans.get(0);
            if (highStockPlan.getPlanQty() < 12) {
                return false;
            }

            // 减少高库存物料计划量
            highStockPlan.setPlanQty(highStockPlan.getPlanQty() - 12);
            scheduleDetailMapper.updateById(highStockPlan);

            // 增加低库存物料计划量
            if (!CollectionUtils.isEmpty(lowStockPlans)) {
                CxScheduleDetail lowStockPlan = lowStockPlans.get(0);
                lowStockPlan.setPlanQty(lowStockPlan.getPlanQty() + 12);
                scheduleDetailMapper.updateById(lowStockPlan);
            } else {
                // 如果没有低库存物料的计划，创建一个新的
                // TODO: 创建新计划明细
                log.warn("低库存物料 {} 在下个班次没有计划，需要创建新计划", lowStockMaterial);
            }

            log.info("已调整计划量：{} +12, {} -12", lowStockMaterial, highStockMaterial);
            return true;

        } catch (Exception e) {
            log.error("调整计划量失败", e);
            return false;
        }
    }

    @Override
    public List<CxScheduleDetail> recalculateSequence(LocalDateTime scheduleDate, String shiftCode, String machineCode) {
        // 获取该机台该班次的所有计划明细
        List<CxScheduleDetail> details = scheduleDetailMapper.selectList(
                new LambdaQueryWrapper<CxScheduleDetail>()
                        .eq(CxScheduleDetail::getScheduleDate, scheduleDate.toLocalDate())
                        .eq(CxScheduleDetail::getShiftCode, shiftCode)
                        .eq(CxScheduleDetail::getMachineCode, machineCode)
                        .orderByAsc(CxScheduleDetail::getSequence));

        if (CollectionUtils.isEmpty(details)) {
            return details;
        }

        // 获取各物料的最新库存信息
        List<String> materialCodes = details.stream()
                .map(CxScheduleDetail::getMaterialCode)
                .distinct()
                .collect(Collectors.toList());

        Map<String, CxStock> stockMap = stockMapper.selectList(
                        new LambdaQueryWrapper<CxStock>()
                                .in(CxStock::getEmbryoCode, materialCodes))
                .stream()
                .collect(Collectors.toMap(CxStock::getEmbryoCode, s -> s));

        // 按库存可供时长排序（库存低的优先）
        details.sort((d1, d2) -> {
            CxStock s1 = stockMap.get(d1.getEmbryoCode());
            CxStock s2 = stockMap.get(d2.getEmbryoCode());

            BigDecimal hours1 = s1 != null && s1.getStockHours() != null ? s1.getStockHours() : BigDecimal.ZERO;
            BigDecimal hours2 = s2 != null && s2.getStockHours() != null ? s2.getStockHours() : BigDecimal.ZERO;

            return hours1.compareTo(hours2);
        });

        // 更新顺位
        for (int i = 0; i < details.size(); i++) {
            CxScheduleDetail detail = details.get(i);
            detail.setSequence(i + 1);
            scheduleDetailMapper.updateById(detail);
        }

        return details;
    }

    @Override
    public boolean checkTreadAvailability(CxScheduleDetail detail) {
        if (detail == null || detail.getPlanStartTime() == null) {
            return false;
        }

        // 获取胎面信息（假设胎面编码规则）
        String treadCode = deriveTreadCode(detail.getEmbryoCode());

        // 获取胎面库存
        CxStock treadStock = stockMapper.selectOne(
                new LambdaQueryWrapper<CxStock>()
                        .eq(CxStock::getEmbryoCode, treadCode));

        if (treadStock == null || treadStock.getStockNum() == null || treadStock.getStockNum() <= 0) {
            return false;
        }

        // 检查胎面是否满足停放时间要求（4小时）
        // TODO: 需要获取胎面的生产时间进行计算

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialExceptionResult handleMaterialException(CxMaterialException exception) {
        MaterialExceptionResult result = new MaterialExceptionResult();
        result.setHandled(false);
        result.setAffectedSchedules(new ArrayList<>());

        if (exception == null) {
            result.setMessage("异常信息为空");
            return result;
        }

        String exceptionType = exception.getExceptionType();

        if ("TREAD_LENGTH_SHORTAGE".equals(exceptionType)) {
            // 胎面卷曲米数不够
            boolean handled = handleTreadLengthShortage(
                    exception.getScheduleDetailId(),
                    exception.getPlannedValue(),
                    exception.getActualValue());

            if (handled) {
                result.setHandled(true);
                result.setHandlingMethod("调高完成率并备注原因");
                result.setMessage("已完成胎面米数不够的处理");
            }

        } else if ("CURTAIN_ROLL_DEPLETION".equals(exceptionType)) {
            // 大卷帘布用完
            boolean handled = handleCurtainRollDepletion(exception.getMaterialCode());

            if (handled) {
                result.setHandled(true);
                result.setHandlingMethod("检查主销产品状态并处理");
                result.setMessage("已完成大卷帘布用完的处理");
            }

        } else {
            result.setMessage("未知的异常类型：" + exceptionType);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleTreadLengthShortage(Long scheduleDetailId, BigDecimal plannedLength, BigDecimal actualLength) {
        if (plannedLength == null || actualLength == null ||
                plannedLength.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }

        // 计算完成率
        BigDecimal completionRate = actualLength.divide(plannedLength, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        // 完成率低于80%时，调高完成率并备注原因
        if (completionRate.compareTo(new BigDecimal("80")) < 0) {
            // 更新完成率为80%
            scheduleDetailMapper.update(null,
                    new LambdaUpdateWrapper<CxScheduleDetail>()
                            .eq(CxScheduleDetail::getId, scheduleDetailId)
                            .set(CxScheduleDetail::getCompletionRate, new BigDecimal("80"))
                            .set(CxScheduleDetail::getRemark, "胎面卷曲米数不够，实际完成率" + completionRate + "%"));

            log.info("已处理胎面米数不够异常，明细ID: {}, 实际完成率: {}%",
                    scheduleDetailId, completionRate);
            return true;
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleCurtainRollDepletion(String materialCode) {
        // 获取物料信息
        MdmMaterialInfo material = materialInfoMapper.selectOne(
                new LambdaQueryWrapper<MdmMaterialInfo>()
                        .eq(MdmMaterialInfo::getMaterialCode, materialCode));

        if (material == null) {
            return false;
        }

        // 主销产品可加量生产，按单生产产品不可加量
        // 注意：MdmMaterialInfo 没有直接的主销产品标识
        // 实际应根据主销产品配置表判断
        // 这里暂时不判断主销产品，统一按非主销产品处理

        // 非主销产品：不可加量，记录异常
        log.warn("产品 {} 大卷帘布用完，不可加量生产", materialCode);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateFutureShiftsPlan(String startShift) {
        try {
            // 获取未来8个班次的排程计划
            // TODO: 实现滚动更新逻辑
            log.info("开始滚动更新未来8个班次的计划，起始班次: {}", startShift);
            return true;
        } catch (Exception e) {
            log.error("滚动更新未来班次计划失败", e);
            return false;
        }
    }

    @Override
    public AdjustmentMaterialList getAdjustmentMaterials(LocalDateTime scheduleDate) {
        AdjustmentMaterialList result = new AdjustmentMaterialList();
        result.setLowStockMaterials(new ArrayList<>());
        result.setHighStockMaterials(new ArrayList<>());

        // 获取所有在制物料
        List<CxStock> stocks = stockMapper.selectList(
                new LambdaQueryWrapper<CxStock>()
                        .gt(CxStock::getStockNum, 0));

        for (CxStock stock : stocks) {
            BigDecimal availableHours = stock.getStockHours() != null
                    ? stock.getStockHours()
                    : BigDecimal.ZERO;

            String alertType = checkStockAlert(stock.getEmbryoCode(), availableHours);

            StockInfo stockInfo = new StockInfo();
            stockInfo.setMaterialCode(stock.getEmbryoCode());
            stockInfo.setMaterialName(stock.getMaterialName());
            stockInfo.setCurrentStock(stock.getStockNum());
            stockInfo.setStockHours(stock.getStockHours());
            stockInfo.setAvailableHours(availableHours);

            if ("LOW".equals(alertType)) {
                result.getLowStockMaterials().add(stockInfo);
            } else if ("HIGH".equals(alertType)) {
                result.getHighStockMaterials().add(stockInfo);
            }
        }

        return result;
    }

    /**
     * 获取下一个班次编码
     */
    private String getNextShiftCode(String currentShiftCode) {
        if ("SHIFT_NIGHT".equals(currentShiftCode)) {
            return "SHIFT_MORNING";
        } else if ("SHIFT_MORNING".equals(currentShiftCode)) {
            return "SHIFT_AFTERNOON";
        } else {
            return "SHIFT_NIGHT";
        }
    }

    /**
     * 计算硫化消耗量
     */
    private Integer calculateLhConsumption(String materialCode, LocalDateTime scheduleDate, String shiftCode) {
        // 简化处理：假设每班消耗量与计划量相等
        // 实际需要根据硫化计划详细计算
        List<LhScheduleResult> lhSchedules = lhScheduleResultMapper.selectList(
                new LambdaQueryWrapper<LhScheduleResult>()
                        .eq(LhScheduleResult::getMaterialCode, materialCode)
                        .eq(LhScheduleResult::getScheduleDate, scheduleDate.toLocalDate()));

        return lhSchedules.stream()
                .mapToInt(p -> p.getDailyPlanQty() != null ? p.getDailyPlanQty() : 0)
                .sum() / 3; // 平均到三个班次
    }

    /**
     * 获取硫化机数量
     */
    private int getLhMachineCount(String materialCode) {
        // 简化处理：假设每个结构对应固定数量的硫化机
        // 实际需要根据结构硫化配比表查询
        List<LhScheduleResult> lhSchedules = lhScheduleResultMapper.selectList(
                new LambdaQueryWrapper<LhScheduleResult>()
                        .eq(LhScheduleResult::getMaterialCode, materialCode));

        return (int) lhSchedules.stream()
                .map(LhScheduleResult::getLhMachineCode)
                .distinct()
                .count();
    }

    /**
     * 根据胎胚编码推导胎面编码
     */
    private String deriveTreadCode(String embryoCode) {
        // 简化处理：假设胎面编码 = 胎胚编码 + "-T"
        return embryoCode + "-T";
    }
}
