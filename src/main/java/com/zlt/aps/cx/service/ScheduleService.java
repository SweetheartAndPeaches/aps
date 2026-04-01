package com.zlt.aps.cx.service;

import com.zlt.aps.cx.dto.ScheduleContextDTO;
import com.zlt.aps.cx.dto.ScheduleRequest;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;

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
    ScheduleResult executeSchedule(ScheduleRequest request);

    /**
     * 执行重排程
     *
     * @param request 重排程请求
     * @return 是否成功
     */
    boolean reSchedule(ScheduleRequest request);

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
