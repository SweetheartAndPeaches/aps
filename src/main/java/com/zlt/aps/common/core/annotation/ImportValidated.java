package com.zlt.aps.common.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 导入数据验证注解
 * 用于标注需要验证的导入字段
 *
 * @author APS Team
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface ImportValidated {
    
    /**
     * 是否必填
     */
    boolean required() default false;

    /**
     * 最大长度
     */
    int maxLength() default Integer.MAX_VALUE;

    /**
     * 最小长度
     */
    int minLength() default Integer.MIN_VALUE;

    /**
     * 最大值
     */
    double max() default Double.MAX_VALUE;

    /**
     * 最小值
     */
    double min() default Double.MIN_VALUE;

    /**
     * 正则表达式
     */
    String pattern() default "";

    /**
     * 错误提示信息
     */
    String message() default "";
}
