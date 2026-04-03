package com.zlt.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel导入字段验证注解
 * 用于标注需要验证的Excel导入字段
 *
 * @author zlt
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface ImportExcelValidated {
    
    /**
     * 字段名称
     */
    String name() default "";

    /**
     * 是否必填
     */
    boolean required() default false;

    /**
     * 是否为代码类型
     */
    boolean isCode() default false;

    /**
     * 是否为数字类型
     */
    boolean number() default false;

    /**
     * 是否为数字类型（支持小数）
     */
    boolean digits() default false;

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
     * 是否为日期类型
     */
    boolean date() default false;

    /**
     * 字典类型
     */
    String dictType() default "";

    /**
     * 是否为颜色代码
     */
    boolean colorCode() default false;
}
