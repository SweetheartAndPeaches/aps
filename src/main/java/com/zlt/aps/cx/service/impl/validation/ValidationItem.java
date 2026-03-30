package com.zlt.aps.cx.service.impl.validation;

import lombok.Getter;

/**
 * 校验项枚举
 * 
 * 定义所有可校验的数据项
 * 新增校验项：
 * 1. 在枚举中添加新的校验项
 * 2. 创建对应的 ValidationStrategy 实现
 * 3. 在策略类中关联对应的枚举值
 *
 * @author APS Team
 */
@Getter
public enum ValidationItem {

    // ==================== 基础配置 ====================
    
    /** 班次配置 */
    SHIFT_CONFIG("班次配置", "T_CX_SHIFT_CONFIG", ValidationLevel.ERROR, 10),
    
    /** 成型机台 */
    MOLDING_MACHINE("成型机台", "T_MDM_MOLDING_MACHINE", ValidationLevel.ERROR, 11),
    
    // ==================== 任务来源 ====================
    
    /** 硫化排程结果 */
    LH_SCHEDULE_RESULT("硫化排程结果", "T_LH_SCHEDULE_RESULT", ValidationLevel.WARN, 20),
    
    /** 物料信息 */
    MATERIAL_INFO("物料信息", "T_MDM_MATERIAL_INFO", ValidationLevel.ERROR, 21),
    
    // ==================== 库存与在制 ====================
    
    /** 胎胚库存 */
    STOCK("胎胚库存", "T_CX_STOCK", ValidationLevel.INFO, 30),
    
    /** 成型在机信息 */
    ONLINE_INFO("成型在机信息", "T_MDM_CX_MACHINE_ONLINE_INFO", ValidationLevel.INFO, 31),
    
    // ==================== 配置参数 ====================
    
    /** 参数配置 */
    PARAM_CONFIG("参数配置", "T_CX_PARAM_CONFIG", ValidationLevel.ERROR, 40),
    
    /** 结构班产配置 */
    STRUCTURE_SHIFT_CAPACITY("结构班产配置", "T_CX_STRUCTURE_SHIFT_CAPACITY", ValidationLevel.WARN, 41),
    
    /** 关键产品配置 */
    KEY_PRODUCT("关键产品配置", "T_CX_KEY_PRODUCT", ValidationLevel.INFO, 42),
    
    // ==================== 产能相关 ====================
    
    /** 结构硫化配比 */
    STRUCTURE_LH_RATIO("结构硫化配比", "T_MDM_STRUCTURE_LH_RATIO", ValidationLevel.WARN, 50),
    
    /** 物料日硫化产能 */
    MATERIAL_LH_CAPACITY("物料日硫化产能", "计算得出", ValidationLevel.WARN, 51),
    
    /** 设备计划停机 */
    DEVICE_PLAN_SHUT("设备计划停机", "T_MDM_DEVICE_PLAN_SHUT", ValidationLevel.INFO, 52),
    
    // ==================== 月计划相关 ====================
    
    /** 月度计划余量 */
    MONTH_SURPLUS("月度计划余量", "T_MDM_MONTH_SURPLUS", ValidationLevel.INFO, 60),
    
    /** 物料收尾数据 */
    MATERIAL_ENDING("物料收尾数据", "T_CX_MATERIAL_ENDING", ValidationLevel.WARN, 61),
    
    // ==================== 其他 ====================
    
    /** SKU排产分类 */
    SKU_SCHEDULE_CATEGORY("SKU排产分类", "T_MDM_SKU_SCHEDULE_CATEGORY", ValidationLevel.INFO, 70),
    
    /** 节假日配置 */
    HOLIDAY_CONFIG("节假日配置", "T_CX_HOLIDAY_CONFIG", ValidationLevel.INFO, 71);

    /**
     * 校验项名称（中文）
     */
    private final String name;

    /**
     * 对应数据表或来源
     */
    private final String source;

    /**
     * 默认校验级别
     */
    private final ValidationLevel defaultLevel;

    /**
     * 排序顺序
     */
    private final int order;

    ValidationItem(String name, String source, ValidationLevel defaultLevel, int order) {
        this.name = name;
        this.source = source;
        this.defaultLevel = defaultLevel;
        this.order = order;
    }

    /**
     * 校验级别
     */
    public enum ValidationLevel {
        /** 阻断级 - 必须修复才能排程 */
        ERROR,
        
        /** 警告级 - 可继续但需关注 */
        WARN,
        
        /** 提示级 - 仅提示 */
        INFO
    }
}
