package com.zlt.aps.cx.service;

import com.zlt.aps.cx.entity.CxMaterial;
import com.zlt.aps.cx.entity.CxStock;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.entity.mdm.MdmMoldingMachine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 排程算法服务接口
 * 
 * 负责排程的核心算法逻辑，包括：
 * - 机台分配
 * - 产能计算
 * - 班次分配
 * - 库存计算
 * - 优先级排序
 *
 * @author APS Team
 */
public interface AlgorithmService {

    /**
     * 执行排程算法
     *
     * @param scheduleDate 排程日期
     * @param machines     可用机台列表
     * @param materials    物料列表
     * @param stocks       库存列表
     * @return 排程结果列表
     */
    List<CxScheduleResult> executeScheduleAlgorithm(
            LocalDate scheduleDate,
            List<MdmMoldingMachine> machines,
            List<CxMaterial> materials,
            List<CxStock> stocks);

    /**
     * 分配机台
     *
     * @param material    物料信息
     * @param machines    可用机台列表
     * @param stocks      库存信息
     * @return 分配的机台
     */
    MdmMoldingMachine allocateMachine(CxMaterial material, List<MdmMoldingMachine> machines, CxStock stock);

    /**
     * 计算班次计划量
     *
     * @param totalPlanQty   总计划量
     * @param shifts         班次列表（8个班次）
     * @param machineCapacity 机台产能（每小时）
     * @return 各班次计划量
     */
    Map<String, BigDecimal> calculateShiftPlanQty(
            BigDecimal totalPlanQty,
            List<String> shifts,
            BigDecimal machineCapacity);

    /**
     * 计算库存可供硫化时长
     *
     * @param stock              库存信息
     * @param vulcanizeMachineCount 硫化机台数
     * @param vulcanizeMoldCount    总模数
     * @param vulcanizeTimeMinutes  硫化时间（分钟）
     * @return 可供硫化时长（小时）
     */
    BigDecimal calculateStockHours(
            CxStock stock,
            Integer vulcanizeMachineCount,
            Integer vulcanizeMoldCount,
            BigDecimal vulcanizeTimeMinutes);

    /**
     * 计算机台产能
     *
     * @param machine   机台信息
     * @param material  物料信息
     * @param shiftHours 班次时长（小时）
     * @return 班次产能
     */
    BigDecimal calculateMachineCapacity(MdmMoldingMachine machine, CxMaterial material, Integer shiftHours);

    /**
     * 排序SKU优先级
     *
     * @param materials 物料列表
     * @param stocks    库存列表
     * @return 排序后的物料编码列表
     */
    List<String> sortSkuPriority(List<CxMaterial> materials, List<CxStock> stocks);

    /**
     * 检查结构约束
     *
     * @param machine    机台信息
     * @param structure  产品结构
     * @return 是否通过约束检查
     */
    boolean checkStructureConstraint(MdmMoldingMachine machine, String structure);

    /**
     * 检查库存约束
     *
     * @param stock      库存信息
     * @param planQty    计划量
     * @param alertThreshold 预警阈值（小时）
     * @return 约束检查结果
     */
    StockConstraintResult checkStockConstraint(CxStock stock, BigDecimal planQty, BigDecimal alertThreshold);

    /**
     * 生成排程明细
     *
     * @param scheduleResult 排程主表
     * @param shifts         班次配置
     * @return 排程明细列表
     */
    List<CxScheduleDetail> generateScheduleDetails(
            CxScheduleResult scheduleResult,
            Map<String, ShiftConfig> shifts);

    /**
     * 关联硫化排程
     *
     * @param cxScheduleResult 成型排程结果
     * @param lhSchedules      硫化排程列表
     * @return 关联结果
     */
    boolean linkLhSchedule(CxScheduleResult cxScheduleResult, List<LhScheduleResult> lhSchedules);

    /**
     * 计算收尾提示
     *
     * @param scheduleResult 排程结果
     * @param remainingQty   剩余量
     * @param stock          库存信息
     * @return 是否需要收尾提示
     */
    boolean calculateEndingTip(CxScheduleResult scheduleResult, BigDecimal remainingQty, CxStock stock);

    /**
     * 班次配置
     */
    class ShiftConfig {
        private String shiftCode;
        private String shiftName;
        private Integer startHour;
        private Integer endHour;
        private Integer standardHours;

        public String getShiftCode() {
            return shiftCode;
        }

        public void setShiftCode(String shiftCode) {
            this.shiftCode = shiftCode;
        }

        public String getShiftName() {
            return shiftName;
        }

        public void setShiftName(String shiftName) {
            this.shiftName = shiftName;
        }

        public Integer getStartHour() {
            return startHour;
        }

        public void setStartHour(Integer startHour) {
            this.startHour = startHour;
        }

        public Integer getEndHour() {
            return endHour;
        }

        public void setEndHour(Integer endHour) {
            this.endHour = endHour;
        }

        public Integer getStandardHours() {
            return standardHours;
        }

        public void setStandardHours(Integer standardHours) {
            this.standardHours = standardHours;
        }
    }

    /**
     * 库存约束检查结果
     */
    class StockConstraintResult {
        private boolean passed;
        private String reason;
        private BigDecimal availableHours;
        private BigDecimal requiredHours;

        public boolean isPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public BigDecimal getAvailableHours() {
            return availableHours;
        }

        public void setAvailableHours(BigDecimal availableHours) {
            this.availableHours = availableHours;
        }

        public BigDecimal getRequiredHours() {
            return requiredHours;
        }

        public void setRequiredHours(BigDecimal requiredHours) {
            this.requiredHours = requiredHours;
        }
    }
}
