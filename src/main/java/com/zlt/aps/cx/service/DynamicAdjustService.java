package com.zlt.aps.cx.service;

import com.zlt.aps.cx.entity.CxMaterialException;
import com.zlt.aps.cx.entity.CxStock;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 动态调整服务接口
 * 
 * 实现排程执行过程中的动态调整：
 * - 交班库存检查
 * - 计划量微调
 * - 顺位重新计算
 * - 材料异常处理
 *
 * @author APS Team
 */
public interface DynamicAdjustService {

    /**
     * 执行交班前检查和调整
     * 每班结束前1小时执行
     *
     * @param scheduleDate 排程日期
     * @param shiftCode    当前班次
     * @return 调整结果
     */
    ShiftAdjustResult checkAndAdjustBeforeShiftEnd(LocalDateTime scheduleDate, String shiftCode);

    /**
     * 计算预计交班库存
     * 公式：预计交班库存 = 当前库存 + 本班已做 + 本班剩余计划 - 本班剩余消耗
     *
     * @param materialCode 物料编码
     * @param scheduleDate 排程日期
     * @param shiftCode    班次编码
     * @return 预计交班库存
     */
    Integer calculateExpectedShiftEndStock(String materialCode, LocalDateTime scheduleDate, String shiftCode);

    /**
     * 计算交班可供时长
     * 公式：交班可供时长 = 预计交班库存 / (硫化机数×单台模数) - 剩余班次时间
     *
     * @param materialCode      物料编码
     * @param expectedStock     预计交班库存
     * @param remainingShiftHours 剩余班次时间（小时）
     * @return 交班可供时长（小时）
     */
    BigDecimal calculateShiftEndAvailableHours(String materialCode, Integer expectedStock, BigDecimal remainingShiftHours);

    /**
     * 检查库存预警
     * 交班可供时长 < 6小时：断料风险，需加量
     * 交班可供时长 > 18小时：库存过高预警
     *
     * @param materialCode 物料编码
     * @param availableHours 可供时长
     * @return 预警类型（LOW/HIGH/NORMAL）
     */
    String checkStockAlert(String materialCode, BigDecimal availableHours);

    /**
     * 执行计划量微调
     * 库存低的加1整车，库存高的减1整车
     *
     * @param lowStockMaterial  低库存物料编码
     * @param highStockMaterial 高库存物料编码
     * @param scheduleDate      排程日期
     * @param nextShiftCode     下个班次编码
     * @return 是否调整成功
     */
    boolean adjustPlanQuantity(String lowStockMaterial, String highStockMaterial, 
                               LocalDateTime scheduleDate, String nextShiftCode);

    /**
     * 重新计算顺位
     * 根据最新的库存情况重新排序
     *
     * @param scheduleDate 排程日期
     * @param shiftCode    班次编码
     * @param machineCode  机台编码
     * @return 新的顺位列表
     */
    List<CxScheduleDetail> recalculateSequence(LocalDateTime scheduleDate, String shiftCode, String machineCode);

    /**
     * 检查胎面到位情况
     * 胎面停放时间4小时，检查每个任务开始时胎面是否到位
     *
     * @param detail 排程明细
     * @return 是否到位
     */
    boolean checkTreadAvailability(CxScheduleDetail detail);

    /**
     * 处理材料异常
     * 包括胎面卷曲米数不够、大卷帘布用完等
     *
     * @param exception 材料异常记录
     * @return 处理结果
     */
    MaterialExceptionResult handleMaterialException(CxMaterialException exception);

    /**
     * 处理胎面卷曲米数不够
     * 完成率低于80%时，调高完成率并备注原因
     *
     * @param scheduleDetailId 排程明细ID
     * @param plannedLength    计划长度
     * @param actualLength     实际长度
     * @return 处理结果
     */
    boolean handleTreadLengthShortage(Long scheduleDetailId, BigDecimal plannedLength, BigDecimal actualLength);

    /**
     * 处理大卷帘布用完
     * 主销产品可加量生产，按单生产产品不可加量
     *
     * @param materialCode 物料编码
     * @return 处理结果
     */
    boolean handleCurtainRollDepletion(String materialCode);

    /**
     * 滚动更新未来8个班次的计划
     *
     * @param startShift 起始班次
     * @return 更新结果
     */
    boolean updateFutureShiftsPlan(String startShift);

    /**
     * 获取需要调整的物料列表
     * 返回库存过低和过高的物料
     *
     * @param scheduleDate 排程日期
     * @return 调整物料列表
     */
    AdjustmentMaterialList getAdjustmentMaterials(LocalDateTime scheduleDate);

    /**
     * 班次调整结果
     */
    class ShiftAdjustResult {
        private boolean adjusted;
        private List<String> adjustedMaterials;
        private List<String> warningMaterials;
        private String message;

        public boolean isAdjusted() {
            return adjusted;
        }

        public boolean getAdjusted() {
            return adjusted;
        }

        public void setAdjusted(boolean adjusted) {
            this.adjusted = adjusted;
        }

        public List<String> getAdjustedMaterials() {
            return adjustedMaterials;
        }

        public void setAdjustedMaterials(List<String> adjustedMaterials) {
            this.adjustedMaterials = adjustedMaterials;
        }

        public List<String> getWarningMaterials() {
            return warningMaterials;
        }

        public void setWarningMaterials(List<String> warningMaterials) {
            this.warningMaterials = warningMaterials;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * 材料异常处理结果
     */
    @lombok.Data
    class MaterialExceptionResult {
        private boolean handled;
        private String handlingMethod;
        private String message;
        private List<String> affectedSchedules;
    }

    /**
     * 调整物料列表
     */
    @lombok.Data
    class AdjustmentMaterialList {
        private List<StockInfo> lowStockMaterials;
        private List<StockInfo> highStockMaterials;
    }

    /**
     * 库存信息
     */
    @lombok.Data
    class StockInfo {
        private String materialCode;
        private String materialName;
        private Integer currentStock;
        private BigDecimal stockHours;
        private BigDecimal availableHours;
    }
}
