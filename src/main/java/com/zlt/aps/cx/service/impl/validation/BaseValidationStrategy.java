package com.zlt.aps.cx.service.impl.validation;

import com.zlt.aps.cx.vo.ScheduleContextVo;
import lombok.extern.slf4j.Slf4j;

/**
 * 校验策略基础类
 * 
 * 提供公共的校验方法和工具
 * 所有具体校验策略都应继承此类
 *
 * @author APS Team
 */
@Slf4j
public abstract class BaseValidationStrategy implements ValidationStrategy {

    // 引用内部类简化代码
    protected static final ScheduleDataValidationResult.ValidationLevel ERROR = ScheduleDataValidationResult.ValidationLevel.ERROR;
    protected static final ScheduleDataValidationResult.ValidationLevel WARN = ScheduleDataValidationResult.ValidationLevel.WARN;
    protected static final ScheduleDataValidationResult.ValidationLevel INFO = ScheduleDataValidationResult.ValidationLevel.INFO;

    /**
     * 获取校验项枚举
     */
    public abstract ValidationItem getValidationItem();

    @Override
    public String getDataItemName() {
        return getValidationItem().getName();
    }

    @Override
    public int getOrder() {
        return getValidationItem().getOrder();
    }

    /**
     * 添加错误级校验
     */
    protected void addError(ScheduleDataValidationResult result, String message, String suggestion) {
        addDetail(result, ERROR, message, suggestion);
    }

    /**
     * 添加警告级校验
     */
    protected void addWarn(ScheduleDataValidationResult result, String message, String suggestion) {
        addDetail(result, WARN, message, suggestion);
    }

    /**
     * 添加信息级校验
     */
    protected void addInfo(ScheduleDataValidationResult result, String message, String suggestion) {
        addDetail(result, INFO, message, suggestion);
    }

    /**
     * 添加校验详情
     */
    protected void addDetail(ScheduleDataValidationResult result, ScheduleDataValidationResult.ValidationLevel level, 
                            String message, String suggestion) {
        ScheduleDataValidationResult.ValidationDetail detail = new ScheduleDataValidationResult.ValidationDetail();
        detail.setLevel(level);
        detail.setDataItem(getDataItemName());
        detail.setMessage(message);
        detail.setSuggestion(suggestion);
        result.getDetails().add(detail);

        switch (level) {
            case ERROR:
                result.setErrorCount(result.getErrorCount() + 1);
                result.setPassed(false);
                break;
            case WARN:
                result.setWarnCount(result.getWarnCount() + 1);
                break;
            case INFO:
                result.setInfoCount(result.getInfoCount() + 1);
                break;
        }
    }

    /**
     * 检查集合是否为空
     */
    protected <T> boolean isEmpty(java.util.Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 检查集合是否非空
     */
    protected <T> boolean isNotEmpty(java.util.Collection<T> collection) {
        return !isEmpty(collection);
    }
}
