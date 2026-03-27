package com.zlt.aps.cx.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 日硫化量计算模式枚举
 * 用于计算成型机台的满算力
 *
 * @author APS Team
 */
@Getter
@AllArgsConstructor
public enum DayVulcanizationModeEnum {

    /**
     * 使用MES日硫化量
     */
    MES_CAPACITY("1", "MES日硫化量"),

    /**
     * 使用标准日硫化量
     */
    STANDARD_CAPACITY("2", "标准日硫化量"),

    /**
     * 使用APS日硫化量
     */
    APS_CAPACITY("3", "APS日硫化量");

    /**
     * 模式编码
     */
    private final String code;

    /**
     * 模式描述
     */
    private final String desc;

    /**
     * 根据编码获取枚举
     *
     * @param code 模式编码
     * @return 枚举实例
     */
    public static DayVulcanizationModeEnum getByCode(String code) {
        if (code == null) {
            return STANDARD_CAPACITY;
        }
        for (DayVulcanizationModeEnum mode : values()) {
            if (mode.getCode().equals(code)) {
                return mode;
            }
        }
        return STANDARD_CAPACITY;
    }

    /**
     * 根据编码获取枚举
     *
     * @param code 模式编码（数字）
     * @return 枚举实例
     */
    public static DayVulcanizationModeEnum getByCode(Integer code) {
        if (code == null) {
            return STANDARD_CAPACITY;
        }
        return getByCode(String.valueOf(code));
    }
}
