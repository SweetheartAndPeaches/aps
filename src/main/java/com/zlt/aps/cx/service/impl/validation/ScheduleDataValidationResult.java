package com.zlt.aps.cx.service.impl.validation;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 排程数据完整性校验结果
 *
 * @author APS Team
 */
@Data
public class ScheduleDataValidationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否通过校验（无ERROR级问题）
     */
    private boolean passed = true;

    /**
     * 校验摘要信息
     */
    private String summary;

    /**
     * 错误数量（阻断级问题）
     */
    private int errorCount;

    /**
     * 警告数量
     */
    private int warnCount;

    /**
     * 提示数量
     */
    private int infoCount;

    /**
     * 校验明细列表
     */
    private List<ValidationDetail> details = new ArrayList<>();

    public ScheduleDataValidationResult() {
    }

    /**
     * 添加错误级校验
     */
    public void addError(String dataItem, String message, String suggestion) {
        addDetail(ValidationLevel.ERROR, dataItem, message, suggestion);
    }

    /**
     * 添加警告级校验
     */
    public void addWarn(String dataItem, String message, String suggestion) {
        addDetail(ValidationLevel.WARN, dataItem, message, suggestion);
    }

    /**
     * 添加信息级校验
     */
    public void addInfo(String dataItem, String message, String suggestion) {
        addDetail(ValidationLevel.INFO, dataItem, message, suggestion);
    }

    /**
     * 添加校验详情
     */
    public void addDetail(ValidationLevel level, String dataItem, String message, String suggestion) {
        ValidationDetail detail = new ValidationDetail();
        detail.setLevel(level);
        detail.setDataItem(dataItem);
        detail.setMessage(message);
        detail.setSuggestion(suggestion);
        this.details.add(detail);

        switch (level) {
            case ERROR:
                this.errorCount++;
                this.passed = false;
                break;
            case WARN:
                this.warnCount++;
                break;
            case INFO:
                this.infoCount++;
                break;
        }
    }

    /**
     * 生成摘要信息
     */
    public String generateSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("校验");
        if (passed) {
            sb.append("通过");
        } else {
            sb.append("不通过");
        }
        if (errorCount > 0) {
            sb.append(String.format("，错误 %d 项", errorCount));
        }
        if (warnCount > 0) {
            sb.append(String.format("，警告 %d 项", warnCount));
        }
        if (infoCount > 0) {
            sb.append(String.format("，提示 %d 项", infoCount));
        }
        this.summary = sb.toString();
        return this.summary;
    }

    /**
     * 校验明细
     */
    @Data
    public static class ValidationDetail implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 校验级别
         */
        private ValidationLevel level;

        /**
         * 数据项名称
         */
        private String dataItem;

        /**
         * 校验消息
         */
        private String message;

        /**
         * 建议处理方式
         */
        private String suggestion;
    }

    /**
     * 校验级别枚举
     */
    public enum ValidationLevel {
        /**
         * 阻断级 - 缺少此数据无法进行排程
         */
        ERROR,

        /**
         * 警告级 - 数据缺失但不影响排程
         */
        WARN,

        /**
         * 提示级 - 数据完整性提示
         */
        INFO
    }
}
