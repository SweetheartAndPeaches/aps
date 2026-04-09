package com.zlt.aps.cx.service;

import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.vo.ScheduleRequestVo;

import java.time.LocalDate;
import java.util.List;

/**
 * 排程管理服务接口
 *
 * 负责排程的整体流程管理，包括：
 * - 排程执行
 * - 重排程
 * - 动态调整
 * - 试制排程
 * - 排程验证
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
    ScheduleResult executeSchedule(ScheduleRequestVo request);

    /**
     * 执行重排程
     *
     * @param request 重排程请求
     * @return 是否成功
     */
    boolean reSchedule(ScheduleRequestVo request);

    /**

     /**
     * 执行动态调整
     *
     * @param shiftCode 班次编码
     * @return 是否成功
     */

    /**
     * 排程执行结果
     */
    class ScheduleResult {
        private boolean success;
        private String message;
        private LocalDate scheduleDate;
        private List<CxScheduleResult> results;
        private List<ValidationDetail> validationErrors;
        private List<ValidationDetail> validationWarnings;

        public ScheduleResult() {}

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

        public List<ValidationDetail> getValidationErrors() {
            return validationErrors;
        }

        public void setValidationErrors(List<ValidationDetail> validationErrors) {
            this.validationErrors = validationErrors;
        }

        public List<ValidationDetail> getValidationWarnings() {
            return validationWarnings;
        }

        public void setValidationWarnings(List<ValidationDetail> validationWarnings) {
            this.validationWarnings = validationWarnings;
        }
    }

    /**
     * 校验明细（用于API返回）
     */
    class ValidationDetail {
        private String dataItem;
        private String message;
        private String suggestion;

        public ValidationDetail() {}

        public ValidationDetail(String dataItem, String message, String suggestion) {
            this.dataItem = dataItem;
            this.message = message;
            this.suggestion = suggestion;
        }

        public String getDataItem() { return dataItem; }
        public void setDataItem(String dataItem) { this.dataItem = dataItem; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    }

    /**
     * 校验摘要（用于前端展示）
     */
    class ValidationSummary {
        private int errorCount;
        private int warningCount;
        private List<ValidationDetail> errors;
        private List<ValidationDetail> warnings;

        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        public int getWarningCount() { return warningCount; }
        public void setWarningCount(int warningCount) { this.warningCount = warningCount; }
        public List<ValidationDetail> getErrors() { return errors; }
        public void setErrors(List<ValidationDetail> errors) { this.errors = errors; }
        public List<ValidationDetail> getWarnings() { return warnings; }
        public void setWarnings(List<ValidationDetail> warnings) { this.warnings = warnings; }
    }

    /**
     * 排程验证结果
     */
    class ScheduleValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public void setWarnings(List<String> warnings) {
            this.warnings = warnings;
        }
    }
}
