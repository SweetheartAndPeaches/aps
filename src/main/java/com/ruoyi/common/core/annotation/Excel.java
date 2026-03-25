package com.ruoyi.common.core.annotation;

import java.lang.annotation.*;

/**
 * Excel注解（若依框架兼容）
 * 用于标注需要在Excel中导出的字段
 * 
 * @author APS Team
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Excel {
    /**
     * 导出字段名称
     */
    String name() default "";

    /**
     * 日期格式
     */
    String dateFormat() default "";

    /**
     * 读取内容转表达式
     */
    String readConverterExp() default "";

    /**
     * 分隔符，读取字符串组内容
     */
    String separator() default ",";

    /**
     * 列宽
     */
    int width() default 16;

    /**
     * 列高
     */
    int height() default 16;

    /**
     * 字段类型
     */
    Class<?> cellType() default Class.class;

    /**
     * 导出类型（0数字 1字符串）
     */
    int cellTypeValue() default 0;

    /**
     * 后缀
     */
    String suffix() default "";

    /**
     * 当值为空时,字段的默认值
     */
    String defaultValue() default "";

    /**
     * 提示信息
     */
    String prompt() default "";

    /**
     * 排序
     */
    int sort() default 0;
}
