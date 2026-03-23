package com.jinyu.aps.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 硫化方式/成型法枚举
 *
 * @author APS Team
 * @since 2.0.0
 */
@Getter
@AllArgsConstructor
public enum MouldMethod {

    /**
     * 机械硫化
     */
    MECHANICAL("1", "机械硫化"),

    /**
     * 液压硫化
     */
    HYDRAULIC("2", "液压硫化");

    private final String code;
    private final String desc;

    /**
     * 根据编码获取枚举
     */
    public static MouldMethod getByCode(String code) {
        for (MouldMethod method : values()) {
            if (method.getCode().equals(code)) {
                return method;
            }
        }
        return null;
    }
}
