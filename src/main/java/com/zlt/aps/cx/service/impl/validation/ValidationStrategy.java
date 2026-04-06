package com.zlt.aps.cx.service.impl.validation;

import com.zlt.aps.cx.vo.ScheduleContextVo;

import java.time.LocalDate;

/**
 * 数据校验策略接口
 * 
 * 所有数据项的校验逻辑都实现此接口
 * 新增校验项只需：
 * 1. 创建新的策略类实现此接口
 * 2. 添加 @Component 注解自动注册
 * 
 * @author APS Team
 */
public interface ValidationStrategy {

    /**
     * 获取校验项名称
     */
    String getDataItemName();

    /**
     * 获取校验优先级（数字越小优先级越高）
     */
    default int getOrder() {
        return 100;
    }

    /**
     * 执行校验
     *
     * @param context 排程上下文
     * @param scheduleDate 排程日期
     * @param factoryCode 工厂编码
     * @param result 校验结果收集器
     */
    void validate(ScheduleContextVo context, LocalDate scheduleDate, String factoryCode, 
                  ScheduleDataValidationResult result);

    /**
     * 是否启用此校验策略
     * 可通过配置控制是否执行某些校验
     */
    default boolean isEnabled() {
        return true;
    }
}
