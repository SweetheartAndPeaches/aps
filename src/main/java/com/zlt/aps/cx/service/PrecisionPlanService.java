package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.cx.entity.CxPrecisionPlan;
import com.zlt.aps.mdm.api.domain.entity.MdmMoldingMachine;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 精度计划服务接口
 * 
 * 功能说明：
 * 1. 精度计划生成：根据机台精度周期自动生成计划
 * 2. 班次安排决策：根据胎胚库存决定安排在早班还是中班
 * 3. 硫化减产判断：计算精度期间对硫化产能的影响
 *
 * @author APS Team
 */
public interface PrecisionPlanService extends IService<CxPrecisionPlan> {

    /**
     * 获取指定日期的精度计划列表
     *
     * @param planDate 计划日期
     * @return 精度计划列表
     */
    List<CxPrecisionPlan> getByDate(LocalDate planDate);

    /**
     * 获取指定机台的精度计划
     *
     * @param machineCode 机台编码
     * @param planDate    计划日期
     * @return 精度计划
     */
    CxPrecisionPlan getByMachineAndDate(String machineCode, LocalDate planDate);

    /**
     * 自动生成精度计划
     * 根据机台精度周期（每2个月）和到期日期，提前3天安排精度计划
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 生成的计划数量
     */
    int autoGeneratePlans(LocalDate startDate, LocalDate endDate);

    /**
     * 安排精度计划班次
     * 根据胎胚库存决定安排在早班还是中班
     * 
     * 规则：
     * - 如果胎胚库存够吃超过一个班，安排在早班（7:30-11:30）
     * - 特殊情况可以安排中班（13:00-17:00）
     *
     * @param planId 计划ID
     * @return 是否安排成功
     */
    boolean arrangePlanShift(Long planId);

    /**
     * 计算精度期间对硫化产能的影响
     * 
     * 规则：
     * - 如果胎胚库存够硫化机吃4小时以上，硫化机继续生产
     * - 如果不够，硫化机要减产一半
     *
     * @param machineCode  成型机台编码
     * @param planDate     计划日期
     * @return 硫化减产比例（0表示不减产，0.5表示减半）
     */
    java.math.BigDecimal calculateVulcanizeReduceRatio(String machineCode, LocalDate planDate);

    /**
     * 获取精度期间不可用的机台编码集合
     *
     * @param planDate  计划日期
     * @param shiftCode 班次编码（可选，null表示全天）
     * @return 不可用机台编码集合
     */
    List<String> getUnavailableMachines(LocalDate planDate, String shiftCode);

    /**
     * 获取精度计划的产能扣减信息
     *
     * @param planDate 计划日期
     * @return Map<机台编码, 扣减产能条数>
     */
    Map<String, Integer> getCapacityDeduction(LocalDate planDate);

    /**
     * 判断机台在指定时段是否处于精度期间
     *
     * @param machineCode 机台编码
     * @param planDate    计划日期
     * @param shiftCode   班次编码
     * @return 是否在精度期间
     */
    boolean isInPrecisionPeriod(String machineCode, LocalDate planDate, String shiftCode);

    /**
     * 批量更新计划状态
     *
     * @param planIds 计划ID列表
     * @param status  新状态
     * @return 更新数量
     */
    int batchUpdateStatus(List<Long> planIds, String status);

    /**
     * 获取即将到期的机台列表（需要安排精度计划）
     *
     * @param days 提前天数
     * @return 机台列表
     */
    List<MdmMoldingMachine> getMachinesDueForPrecision(int days);
}
