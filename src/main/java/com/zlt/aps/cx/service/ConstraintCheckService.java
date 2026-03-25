package com.zlt.aps.cx.service;

import com.zlt.aps.cx.entity.CxStock;
import com.zlt.aps.cx.entity.CxTreadParkingConfig;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 约束校验服务接口
 * 
 * 实现各类约束校验逻辑：
 * - 结构约束
 * - 库存约束
 * - 胎面停放时间约束
 * - 产能约束
 * - 机台种类上限约束
 *
 * @author APS Team
 */
public interface ConstraintCheckService {

    /**
     * 执行完整约束校验
     *
     * @param scheduleResult 排程结果
     * @return 校验结果
     */
    ConstraintCheckResult checkAllConstraints(CxScheduleResult scheduleResult);

    /**
     * 检查结构约束
     * 包括：固定机台约束、不可作业约束、机型匹配约束
     *
     * @param machine  机台信息
     * @param material 物料信息
     * @return 校验结果
     */
    ConstraintCheckResult checkStructureConstraint(MdmMoldingMachine machine, MdmMaterialInfo material);

    /**
     * 检查库存约束
     * 库存可供硫化时长是否满足需求
     *
     * @param stock          库存信息
     * @param planQty        计划量
     * @param alertThreshold 预警阈值（小时）
     * @return 校验结果
     */
    ConstraintCheckResult checkStockConstraint(CxStock stock, BigDecimal planQty, BigDecimal alertThreshold);

    /**
     * 检查胎面停放时间约束
     * 胎面停放时间必须在合理范围内（4小时以上）
     *
     * @param materialCode  物料编码
     * @param produceTime   胎面生产时间
     * @param scheduleTime  计划使用时间
     * @return 校验结果
     */
    ConstraintCheckResult checkTreadParkingTime(String materialCode, LocalDateTime produceTime, LocalDateTime scheduleTime);

    /**
     * 检查产能约束
     * 机台产能是否满足计划量
     *
     * @param machine   机台信息
     * @param planQty   计划量
     * @param shiftHours 班次时长
     * @return 校验结果
     */
    ConstraintCheckResult checkCapacityConstraint(MdmMoldingMachine machine, BigDecimal planQty, Integer shiftHours);

    /**
     * 检查产能约束（根据机台-结构维度）
     * 从CxMachineStructureCapacity获取该机台-结构的产能
     *
     * @param machine      机台信息
     * @param structureCode 结构编码
     * @param planQty      计划量
     * @param shiftCode    班次编码（SHIFT_DAY/SHIFT_AFTERNOON/SHIFT_NIGHT），为null时计算日产能
     * @return 校验结果
     */
    ConstraintCheckResult checkCapacityConstraint(MdmMoldingMachine machine, String structureCode, 
            BigDecimal planQty, String shiftCode);

    /**
     * 检查机台种类上限约束
     * 每台成型机最多做4种不同的胎胚
     *
     * @param machineCode   机台编码
     * @param currentTypes  当前已分配种类数
     * @param newMaterial   新物料编码
     * @return 校验结果
     */
    ConstraintCheckResult checkTypeLimitConstraint(String machineCode, int currentTypes, String newMaterial);

    /**
     * 检查硫化配比约束
     * 成型机对应的硫化机数量是否满足要求
     *
     * @param structureName  结构名称
     * @param machineType    机台类型
     * @param lhMachineCount 硫化机台数
     * @return 校验结果
     */
    ConstraintCheckResult checkLhRatioConstraint(String structureName, String machineType, int lhMachineCount);

    /**
     * 检查关键产品约束
     * 开产首班不排关键产品
     *
     * @param material     物料信息
     * @param isOpeningDay 是否开产日
     * @param isFirstShift 是否首班
     * @return 校验结果
     */
    ConstraintCheckResult checkKeyProductConstraint(MdmMaterialInfo material, boolean isOpeningDay, boolean isFirstShift);

    /**
     * 检查试制约束
     * 一天最多做2个新胎胚，周日不做
     * 只能安排在早班或中班，数量必须是双数
     *
     * @param scheduleDate    排程日期
     * @param trialTaskCount  当天已安排试制数
     * @param shiftCode       班次编码
     * @param quantity        数量
     * @return 校验结果
     */
    ConstraintCheckResult checkTrialConstraint(LocalDateTime scheduleDate, int trialTaskCount, String shiftCode, int quantity);

    /**
     * 检查精度计划约束
     * 有精度计划的机台在精度期间不可排产
     *
     * @param machineCode   机台编码
     * @param scheduleDate  排程日期
     * @param shiftCode     班次编码
     * @return 校验结果
     */
    ConstraintCheckResult checkPrecisionPlanConstraint(String machineCode, LocalDateTime scheduleDate, String shiftCode);

    /**
     * 检查操作工请假约束
     * 请假期间该机台该班次不可用
     *
     * @param machineCode 机台编码
     * @param shiftCode   班次编码
     * @param scheduleDate 排程日期
     * @return 校验结果
     */
    ConstraintCheckResult checkOperatorLeaveConstraint(String machineCode, String shiftCode, LocalDateTime scheduleDate);

    /**
     * 检查收尾约束
     * 收尾余量处理规则
     *
     * @param material    物料信息
     * @param stock       库存信息
     * @param remainingQty 剩余量
     * @return 校验结果
     */
    ConstraintCheckResult checkEndingConstraint(MdmMaterialInfo material, CxStock stock, BigDecimal remainingQty);

    /**
     * 获取胎面停放配置
     *
     * @param structureCode 结构编码
     * @return 胎面停放配置
     */
    CxTreadParkingConfig getTreadParkingConfig(String structureCode);

    /**
     * 约束校验结果
     */
    @lombok.Data
    class ConstraintCheckResult {
        /**
         * 是否通过
         */
        private boolean passed;

        /**
         * 违规信息列表
         */
        private List<String> violations;

        /**
         * 警告信息列表（不影响通过）
         */
        private List<String> warnings;

        /**
         * 详细信息
         */
        private String details;

        public static ConstraintCheckResult pass() {
            ConstraintCheckResult result = new ConstraintCheckResult();
            result.setPassed(true);
            result.setViolations(new java.util.ArrayList<>());
            result.setWarnings(new java.util.ArrayList<>());
            return result;
        }

        public static ConstraintCheckResult fail(String violation) {
            ConstraintCheckResult result = new ConstraintCheckResult();
            result.setPassed(false);
            result.setViolations(java.util.Collections.singletonList(violation));
            result.setWarnings(new java.util.ArrayList<>());
            return result;
        }

        public static ConstraintCheckResult fail(List<String> violations) {
            ConstraintCheckResult result = new ConstraintCheckResult();
            result.setPassed(false);
            result.setViolations(violations);
            result.setWarnings(new java.util.ArrayList<>());
            return result;
        }
    }
}
