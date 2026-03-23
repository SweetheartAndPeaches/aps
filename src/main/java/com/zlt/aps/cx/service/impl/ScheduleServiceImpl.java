package com.zlt.aps.cx.service.impl;

import com.zlt.aps.cx.dto.ScheduleGenerateDTO;
import com.zlt.aps.cx.dto.ScheduleResultDTO;
import com.zlt.aps.cx.entity.CxMaterial;
import com.zlt.aps.cx.entity.CxStock;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.mdm.MdmMoldingMachine;
import com.zlt.aps.cx.mapper.MdmMoldingMachineMapper;
import com.zlt.aps.cx.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 排程管理服务实现类
 *
 * @author APS Team
 */
@Slf4j
@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private CxMaterialService cxMaterialService;

    @Autowired
    private CxStockService cxStockService;

    @Autowired
    private CxScheduleResultService cxScheduleResultService;

    @Autowired
    private CxScheduleDetailService cxScheduleDetailService;

    @Autowired
    private LhScheduleResultService lhScheduleResultService;

    @Autowired
    private AlgorithmService algorithmService;

    @Autowired
    private MdmMoldingMachineMapper mdmMoldingMachineMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CxScheduleResult> generateSchedule(ScheduleGenerateDTO dto) {
        log.info("开始生成排程，日期: {}, 天数: {}", dto.getScheduleDate(), dto.getDays());

        List<CxScheduleResult> allResults = new ArrayList<>();
        LocalDate currentDate = dto.getScheduleDate();

        for (int i = 0; i < dto.getDays(); i++) {
            LocalDate scheduleDate = currentDate.plusDays(i);
            
            // 检查是否需要覆盖
            if (!Boolean.TRUE.equals(dto.getOverwrite())) {
                List<CxScheduleResult> existing = cxScheduleResultService.listByScheduleDate(scheduleDate);
                if (!existing.isEmpty()) {
                    log.info("日期 {} 已存在排程，跳过生成", scheduleDate);
                    allResults.addAll(existing);
                    continue;
                }
            } else {
                // 删除已有排程
                cxScheduleResultService.deleteByScheduleDate(scheduleDate);
            }

            // 生成当日排程
            List<CxScheduleResult> dailyResults = generateDailySchedule(scheduleDate);
            allResults.addAll(dailyResults);
        }

        log.info("排程生成完成，共生成 {} 条记录", allResults.size());
        return allResults;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CxScheduleResult> generateDailySchedule(LocalDate scheduleDate) {
        log.info("生成日排程，日期: {}", scheduleDate);

        // 1. 获取基础数据
        List<MdmMoldingMachine> machines = mdmMoldingMachineMapper.selectList(null);
        List<CxMaterial> materials = cxMaterialService.listActive();
        List<CxStock> stocks = cxStockService.list();

        if (CollectionUtils.isEmpty(machines) || CollectionUtils.isEmpty(materials)) {
            log.warn("缺少基础数据，无法生成排程。机台数: {}, 物料数: {}", 
                    machines != null ? machines.size() : 0, 
                    materials != null ? materials.size() : 0);
            return new ArrayList<>();
        }

        // 2. 执行排程算法
        List<CxScheduleResult> results = algorithmService.executeScheduleAlgorithm(
                scheduleDate, machines, materials, stocks);

        // 3. 保存排程结果
        for (CxScheduleResult result : results) {
            // 生成批次号
            result.setCxBatchNo(cxScheduleResultService.generateBatchNo(scheduleDate));
            result.setScheduleDate(scheduleDate.atStartOfDay());
            result.setProductionStatus("0"); // 未生产
            result.setIsRelease("0"); // 未发布
            result.setDataSource("0"); // 自动排程
            result.setCreateTime(LocalDateTime.now());
            
            // 保存主表
            cxScheduleResultService.save(result);
            
            // 生成明细
            List<CxScheduleDetail> details = algorithmService.generateScheduleDetails(
                    result, getDefaultShiftConfig());
            
            // 设置明细的主表ID
            details.forEach(detail -> {
                detail.setMainId(result.getId());
                detail.setScheduleDate(scheduleDate);
                detail.setCreateTime(LocalDateTime.now());
            });
            
            // 保存明细
            cxScheduleDetailService.batchSave(details);
        }

        log.info("日排程生成完成，日期: {}, 记录数: {}", scheduleDate, results.size());
        return results;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmSchedule(Long id) {
        log.info("确认排程，ID: {}", id);
        return cxScheduleResultService.updateProductionStatus(id, "0");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseSchedule(Long id) {
        log.info("发布排程，ID: {}", id);
        return cxScheduleResultService.releaseSchedule(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchReleaseSchedule(List<Long> ids) {
        log.info("批量发布排程，ID数量: {}", ids.size());
        return cxScheduleResultService.batchReleaseSchedule(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelSchedule(Long id) {
        log.info("取消排程，ID: {}", id);
        // 删除明细
        cxScheduleDetailService.deleteByMainId(id);
        // 删除主表
        return cxScheduleResultService.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSchedule(Long id) {
        log.info("删除排程，ID: {}", id);
        return cancelSchedule(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteScheduleByDate(LocalDate scheduleDate) {
        log.info("删除日期排程，日期: {}", scheduleDate);
        return cxScheduleResultService.deleteByScheduleDate(scheduleDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CxScheduleResult adjustSchedule(Long id, String adjustType, String adjustParam) {
        log.info("调整排程，ID: {}, 类型: {}, 参数: {}", id, adjustType, adjustParam);
        
        CxScheduleResult result = cxScheduleResultService.getById(id);
        if (result == null) {
            return null;
        }

        // 根据调整类型执行不同逻辑
        switch (adjustType) {
            case "INSERT": // 插单
                // TODO: 实现插单逻辑
                break;
            case "SWAP": // 换班
                // TODO: 实现换班逻辑
                break;
            case "MODIFY": // 修改数量
                if (adjustParam != null) {
                    BigDecimal newQty = new BigDecimal(adjustParam);
                    result.setProductNum(newQty);
                    cxScheduleResultService.updateById(result);
                }
                break;
            default:
                log.warn("未知的调整类型: {}", adjustType);
        }

        return result;
    }

    @Override
    public ScheduleResultDTO getScheduleDetail(Long id) {
        return cxScheduleResultService.getDetailById(id);
    }

    @Override
    public ScheduleStatusSummary getTodayScheduleStatus() {
        LocalDate today = LocalDate.now();
        List<CxScheduleResult> results = cxScheduleResultService.listByScheduleDate(today);

        ScheduleStatusSummary summary = new ScheduleStatusSummary();
        summary.setScheduleDate(today);
        summary.setTotalCount(results.size());
        summary.setReleasedCount((int) results.stream()
                .filter(r -> "1".equals(r.getIsRelease()))
                .count());
        summary.setProducingCount((int) results.stream()
                .filter(r -> "1".equals(r.getProductionStatus()))
                .count());
        summary.setCompletedCount((int) results.stream()
                .filter(r -> "2".equals(r.getProductionStatus()))
                .count());
        
        // 计算预警数量
        List<CxStock> alertStocks = cxStockService.listLowStock();
        summary.setAlertCount(alertStocks.size());

        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refreshStockAlertStatus() {
        log.info("刷新库存预警状态");
        cxStockService.refreshAllAlertStatus();
        return true;
    }

    @Override
    public ConstraintCheckResult checkConstraints(CxScheduleResult scheduleResult) {
        ConstraintCheckResult result = new ConstraintCheckResult();
        List<String> violations = new ArrayList<>();

        // 检查库存约束
        CxStock stock = cxStockService.getByMaterialCode(scheduleResult.getEmbryoCode());
        if (stock != null) {
            AlgorithmService.StockConstraintResult stockResult = algorithmService.checkStockConstraint(
                    stock, scheduleResult.getProductNum(), new BigDecimal("4"));
            if (!stockResult.isPassed()) {
                violations.add("库存约束不满足: " + stockResult.getReason());
            }
        }

        // 检查机台约束
        MdmMoldingMachine machine = mdmMoldingMachineMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MdmMoldingMachine>()
                        .eq(MdmMoldingMachine::getCxMachineCode, scheduleResult.getCxMachineCode()));
        if (machine != null) {
            boolean structureOk = algorithmService.checkStructureConstraint(
                    machine, scheduleResult.getStructureName());
            if (!structureOk) {
                violations.add("机台结构约束不满足");
            }
        }

        result.setPassed(violations.isEmpty());
        result.setViolations(violations);
        return result;
    }

    /**
     * 获取默认班次配置（8班次）
     */
    private java.util.Map<String, AlgorithmService.ShiftConfig> getDefaultShiftConfig() {
        java.util.Map<String, AlgorithmService.ShiftConfig> shifts = new java.util.LinkedHashMap<>();
        
        // 8班次配置
        String[] shiftCodes = {"SHIFT1", "SHIFT2", "SHIFT3", "SHIFT4", "SHIFT5", "SHIFT6", "SHIFT7", "SHIFT8"};
        String[] shiftNames = {"一班", "二班", "三班", "四班", "五班", "六班", "七班", "八班"};
        int[] startHours = {0, 3, 6, 9, 12, 15, 18, 21};
        int[] endHours = {3, 6, 9, 12, 15, 18, 21, 24};

        for (int i = 0; i < shiftCodes.length; i++) {
            AlgorithmService.ShiftConfig config = new AlgorithmService.ShiftConfig();
            config.setShiftCode(shiftCodes[i]);
            config.setShiftName(shiftNames[i]);
            config.setStartHour(startHours[i]);
            config.setEndHour(endHours[i]);
            config.setStandardHours(3);
            shifts.put(shiftCodes[i], config);
        }

        return shifts;
    }
}
