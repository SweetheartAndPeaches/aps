package com.jinyu.aps.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 是否标识枚举
 *
 * @author APS Team
 * @since 2.0.0
 */
@Getter
@AllArgsConstructor
public enum YesNo {

    /**
     * 否
     */
    NO("0", "否"),

    /**
     * 是
     */
    YES("1", "是");

    private final String code;
    private final String desc;

    /**
     * 根据编码获取枚举
     */
    public static YesNo getByCode(String code) {
        for (YesNo yn : values()) {
            if (yn.getCode().equals(code)) {
                return yn;
            }
        }
        return null;
    }

    /**
     * 判断是否为"是"
     */
    public static boolean isYes(String code) {
        return YES.getCode().equals(code);
    }

    /**
     * 判断是否为"否"
     */
    public static boolean isNo(String code) {
        return NO.getCode().equals(code);
    }
}
