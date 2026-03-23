package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zlt.aps.cx.entity.MonthPlanFinal;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;

import java.time.LocalDate;
import java.util.List;

/**
 * 月度生产计划服务接口
 * 
 * 功能：
 * 1. 月计划查询与管理
 * 2. 月计划拆分为日计划
 * 3. 与APS排程系统集成
 *
 * @author APS Team
 */
public interface MonthPlanFinalService {

    /**
     * 根据年月查询月计划
     * @param yearMonth 年月(YYYYMM)
     * @return 月计划列表
     */
    List<MonthPlanFinal> getByYearMonth(Integer yearMonth);

    /**
     * 根据年份和月份查询
     * @param year 年份
     * @param month 月份
     * @return 月计划列表
     */
    List<MonthPlanFinal> getByYearAndMonth(Integer year, Integer month);

    /**
     * 根据工厂和年月查询
     * @param factoryCode 工厂编码
     * @param yearMonth 年月
     * @return 月计划列表
     */
    List<MonthPlanFinal> getByFactoryAndYearMonth(String factoryCode, Integer yearMonth);

    /**
     * 根据工单号查询
     * @param productionNo 工单号
     * @return 月计划
     */
    MonthPlanFinal getByProductionNo(String productionNo);

    /**
     * 分页查询月计划
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param yearMonth 年月
     * @param factoryCode 工厂编码(可选)
     * @return 分页结果
     */
    Page<MonthPlanFinal> getPage(int pageNum, int pageSize, Integer yearMonth, String factoryCode);

    /**
     * 保存月计划
     * @param monthPlan 月计划
     * @return 是否成功
     */
    boolean save(MonthPlanFinal monthPlan);

    /**
     * 批量保存月计划
     * @param monthPlans 月计划列表
     * @return 是否成功
     */
    boolean saveBatch(List<MonthPlanFinal> monthPlans);

    /**
     * 更新月计划
     * @param monthPlan 月计划
     * @return 是否成功
     */
    boolean update(MonthPlanFinal monthPlan);

    /**
     * 删除月计划
     * @param id 主键ID
     * @return 是否成功
     */
    boolean delete(Long id);

    /**
     * 发布月计划
     * @param yearMonth 年月
     * @param productionVersion 排产版本
     * @return 是否成功
     */
    boolean release(Integer yearMonth, String productionVersion);

    /**
     * ==================== 核心功能：月计划拆分为日计划 ==================== */

    /**
     * 将月计划拆分为日硫化排程
     * 
     * 流程：
     * 1. 查询月计划中指定日期有排产的物料
     * 2. 根据day_X字段值创建日硫化排程记录
     * 3. 设置优先级(根据productionType)
     * 
     * @param scheduleDate 计划日期
     * @return 日硫化排程列表
     */
    List<LhScheduleResult> splitToDailyPlan(LocalDate scheduleDate);

    /**
     * 将月计划拆分为指定日期的日计划
     * @param yearMonth 年月
     * @param day 日期(1-31)
     * @return 日硫化排程列表
     */
    List<LhScheduleResult> splitToDailyPlan(Integer yearMonth, Integer day);

    /**
     * 根据月计划生成APS排程
     * 
     * 完整流程：
     * 1. 从月计划拆分日计划
     * 2. 调用APS核心算法生成排程
     * 3. 同步排程结果回月计划
     * 
     * @param scheduleDate 计划日期
     * @return 排程生成结果
     */
    ScheduleGenerateResult generateScheduleFromMonthPlan(LocalDate scheduleDate);

    /**
     * 同步排程结果到月计划
     * 
     * 功能：
     * 1. 更新total_qty(实际排产量)
     * 2. 更新difference_qty(差异量)
     * 3. 更新cx_machine_code(分配的机台)
     * 
     * @param scheduleDate 计划日期
     * @return 是否成功
     */
    boolean syncScheduleResult(LocalDate scheduleDate);

    /**
     * ==================== 统计与报表 ==================== */

    /**
     * 统计月计划数量
     * @param yearMonth 年月
     * @return 数量
     */
    int countByYearMonth(Integer yearMonth);

    /**
     * 统计月排产总量
     * @param yearMonth 年月
     * @return 排产总量
     */
    Long sumTotalQtyByYearMonth(Integer yearMonth);

    /**
     * 获取月计划统计概览
     * @param yearMonth 年月
     * @return 统计概览
     */
    MonthPlanOverview getOverview(Integer yearMonth);

    /**
     * ==================== 内部类定义 ==================== */

    /**
     * 月计划统计概览
     */
    class MonthPlanOverview {
        private Integer yearMonth;
        private Integer totalPlanCount;      // 总计划数
        private Long totalPlanQty;            // 总计划量
        private Long totalProductionQty;      // 总排产量
        private Long totalDifferenceQty;      // 总差异量
        private Integer releasedCount;        // 已发布数
        private Integer unreleasedCount;      // 未发布数

        // Getters and Setters
        public Integer getYearMonth() { return yearMonth; }
        public void setYearMonth(Integer yearMonth) { this.yearMonth = yearMonth; }
        public Integer getTotalPlanCount() { return totalPlanCount; }
        public void setTotalPlanCount(Integer totalPlanCount) { this.totalPlanCount = totalPlanCount; }
        public Long getTotalPlanQty() { return totalPlanQty; }
        public void setTotalPlanQty(Long totalPlanQty) { this.totalPlanQty = totalPlanQty; }
        public Long getTotalProductionQty() { return totalProductionQty; }
        public void setTotalProductionQty(Long totalProductionQty) { this.totalProductionQty = totalProductionQty; }
        public Long getTotalDifferenceQty() { return totalDifferenceQty; }
        public void setTotalDifferenceQty(Long totalDifferenceQty) { this.totalDifferenceQty = totalDifferenceQty; }
        public Integer getReleasedCount() { return releasedCount; }
        public void setReleasedCount(Integer releasedCount) { this.releasedCount = releasedCount; }
        public Integer getUnreleasedCount() { return unreleasedCount; }
        public void setUnreleasedCount(Integer unreleasedCount) { this.unreleasedCount = unreleasedCount; }
    }

    /**
     * 排程生成结果
     */
    class ScheduleGenerateResult {
        private boolean success;
        private String message;
        private LocalDate scheduleDate;
        private Long scheduleMainId;
        private Integer totalTasks;
        private Integer totalQuantity;
        private List<String> warnings;

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public LocalDate getScheduleDate() { return scheduleDate; }
        public void setScheduleDate(LocalDate scheduleDate) { this.scheduleDate = scheduleDate; }
        public Long getScheduleMainId() { return scheduleMainId; }
        public void setScheduleMainId(Long scheduleMainId) { this.scheduleMainId = scheduleMainId; }
        public Integer getTotalTasks() { return totalTasks; }
        public void setTotalTasks(Integer totalTasks) { this.totalTasks = totalTasks; }
        public Integer getTotalQuantity() { return totalQuantity; }
        public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    }
}
