package com.zlt.aps.cx.service;

import com.zlt.aps.cx.dto.ReScheduleRequest;
import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.dto.ScheduleGenerateDTO;
import com.zlt.aps.cx.dto.ScheduleRequest;
import com.zlt.aps.cx.dto.ScheduleResultDTO;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;

import java.time.LocalDate;
import java.util.List;

/**
 * 排程管理服务接口
 * 
 * 负责排程的整体流程管理，包括：
 * - 排程生成
 * - 排程发布
 * - 排程调整
 * - 状态管理
 *
 * @author APS Team
 */
public interface ScheduleService {

    /**
     * 执行排程（核心方法）
     *
     * @param request 排程请求
     * @return 排程结果
     */
    ScheduleResult executeSchedule(ScheduleRequest request);

    /**
     * 执行重排程
     *
     * @param request 重排程请求
     * @return 是否成功
     */
    boolean executeReSchedule(ReScheduleRequest request);

    /**
     * 构建排程上下文
     *
     * @param request 排程请求
     * @return 排程上下文
     */
    ScheduleContextDTO buildScheduleContext(ScheduleRequest request);

    /**
     * 生成排程
     *
     * @param dto 排程生成参数
     * @return 生成的排程结果列表
     */
    List<CxScheduleResult> generateSchedule(ScheduleGenerateDTO dto);

    /**
     * 生成单日排程
     *
     * @param scheduleDate 排程日期
     * @return 生成的排程结果列表
     */
    List<CxScheduleResult> generateDailySchedule(LocalDate scheduleDate);

    /**
     * 确认排程
     *
     * @param id 排程ID
     * @return 是否成功
     */
    boolean confirmSchedule(Long id);

    /**
     * 发布排程
     *
     * @param id 排程ID
     * @return 是否成功
     */
    boolean releaseSchedule(Long id);

    /**
     * 批量发布排程
     *
     * @param ids 排程ID列表
     * @return 是否成功
     */
    boolean batchReleaseSchedule(List<Long> ids);

    /**
     * 取消排程
     *
     * @param id 排程ID
     * @return 是否成功
     */
    boolean cancelSchedule(Long id);

    /**
     * 删除排程
     *
     * @param id 排程ID
     * @return 是否成功
     */
    boolean deleteSchedule(Long id);

    /**
     * 删除指定日期的排程
     *
     * @param scheduleDate 排程日期
     * @return 是否成功
     */
    boolean deleteScheduleByDate(LocalDate scheduleDate);

    /**
     * 调整排程（插单、换班等）
     *
     * @param id          排程ID
     * @param adjustType  调整类型
     * @param adjustParam 调整参数
     * @return 调整后的排程结果
     */
    CxScheduleResult adjustSchedule(Long id, String adjustType, String adjustParam);

    /**
     * 获取排程详情
     *
     * @param id 排程ID
     * @return 排程详情
     */
    ScheduleResultDTO getScheduleDetail(Long id);

    /**
     * 获取当日排程状态
     *
     * @return 排程状态摘要
     */
    ScheduleStatusSummary getTodayScheduleStatus();

    /**
     * 刷新库存预警状态
     *
     * @return 是否成功
     */
    boolean refreshStockAlertStatus();

    /**
     * 检查排程约束
     *
     * @param scheduleResult 排程结果
     * @return 约束检查结果
     */
    ConstraintCheckResult checkConstraints(CxScheduleResult scheduleResult);

    /**
     * 排程状态摘要
     */
    class ScheduleStatusSummary {
        private LocalDate scheduleDate;
        private Integer totalCount;
        private Integer releasedCount;
        private Integer producingCount;
        private Integer completedCount;
        private Integer alertCount;

        public LocalDate getScheduleDate() {
            return scheduleDate;
        }

        public void setScheduleDate(LocalDate scheduleDate) {
            this.scheduleDate = scheduleDate;
        }

        public Integer getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(Integer totalCount) {
            this.totalCount = totalCount;
        }

        public Integer getReleasedCount() {
            return releasedCount;
        }

        public void setReleasedCount(Integer releasedCount) {
            this.releasedCount = releasedCount;
        }

        public Integer getProducingCount() {
            return producingCount;
        }

        public void setProducingCount(Integer producingCount) {
            this.producingCount = producingCount;
        }

        public Integer getCompletedCount() {
            return completedCount;
        }

        public void setCompletedCount(Integer completedCount) {
            this.completedCount = completedCount;
        }

        public Integer getAlertCount() {
            return alertCount;
        }

        public void setAlertCount(Integer alertCount) {
            this.alertCount = alertCount;
        }
    }

    /**
     * 约束检查结果
     */
    class ConstraintCheckResult {
        private boolean passed;
        private List<String> violations;

        public boolean isPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public List<String> getViolations() {
            return violations;
        }

        public void setViolations(List<String> violations) {
            this.violations = violations;
        }
    }

    /**
     * 排程执行结果
     */
    class ScheduleResult {
        private boolean success;
        private String message;
        private LocalDate scheduleDate;
        private List<CxScheduleResult> results;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public LocalDate getScheduleDate() {
            return scheduleDate;
        }

        public void setScheduleDate(LocalDate scheduleDate) {
            this.scheduleDate = scheduleDate;
        }

        public List<CxScheduleResult> getResults() {
            return results;
        }

        public void setResults(List<CxScheduleResult> results) {
            this.results = results;
        }
    }
}
