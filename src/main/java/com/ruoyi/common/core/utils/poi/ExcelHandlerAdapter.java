package com.ruoyi.common.core.utils.poi;

/**
 * Excel数据处理器适配器接口
 * 用于自定义Excel数据的导入导出处理
 *
 * @author ruoyi
 */
public interface ExcelHandlerAdapter {
    
    /**
     * 格式化数据
     *
     * @param value 数据值
     * @param args  处理器参数
     * @return 格式化后的数据
     */
    Object format(Object value, String[] args);
}
