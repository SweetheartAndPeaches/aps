package com.ruoyi.common.core.annotation;

import com.ruoyi.common.core.utils.poi.ExcelHandlerAdapter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel注解
 * 用于标注需要在Excel中导出/导入的字段
 *
 * @author ruoyi
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target({ElementType.FIELD})
public @interface Excel {
    
    /**
     * 导出排序
     */
    int sort() default Integer.MAX_VALUE;

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
     * 字典类型
     */
    String dictType() default "";

    /**
     * 是否将字典类型转换为Excel值
     */
    boolean dictTypeToExcelEnable() default true;

    /**
     * 分隔符，读取字符串组内容
     */
    String separator() default ",";

    /**
     * 小数点后保留位数
     */
    int scale() default -1;

    /**
     * 舍入模式
     */
    int roundingMode() default 6;

    /**
     * 列类型
     */
    ColumnType cellType() default Excel.ColumnType.STRING;

    /**
     * 列高
     */
    double height() default 14.0;

    /**
     * 列宽
     */
    double width() default 16.0;

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
     * 设置只能选择不能输入的列内容
     */
    String[] combo() default {};

    /**
     * 是否导出
     */
    boolean isExport() default true;

    /**
     * 另一个类中的属性名称
     */
    String targetAttr() default "";

    /**
     * 是否自动统计数据
     */
    boolean isStatistics() default false;

    /**
     * 导入字段名称
     */
    String importName() default "";

    /**
     * 对齐方式
     */
    Align align() default Excel.Align.AUTO;

    /**
     * 自定义数据处理器
     */
    Class<?> handler() default ExcelHandlerAdapter.class;

    /**
     * 处理器参数
     */
    String[] args() default {};

    /**
     * 导出/导入类型
     */
    Type type() default Excel.Type.ALL;

    /**
     * 列类型枚举
     */
    public static enum ColumnType {
        NUMERIC(0),
        STRING(1),
        IMAGE(2);

        private final int value;

        private ColumnType(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    /**
     * 导出/导入类型枚举
     */
    public static enum Type {
        ALL(0),
        EXPORT(1),
        IMPORT(2);

        private final int value;

        private Type(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    /**
     * 对齐方式枚举
     */
    public static enum Align {
        AUTO(0),
        LEFT(1),
        CENTER(2),
        RIGHT(3);

        private final int value;

        private Align(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }
}
