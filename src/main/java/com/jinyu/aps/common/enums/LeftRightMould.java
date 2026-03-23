package com.jinyu.aps.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 左右模枚举
 *
 * @author APS Team
 * @since 2.0.0
 */
@Getter
@AllArgsConstructor
public enum LeftRightMould {

    /**
     * 左模
     */
    LEFT("L", "左模"),

    /**
     * 右模
     */
    RIGHT("R", "右模"),

    /**
     * 左右模
     */
    BOTH("LR", "左右模");

    private final String code;
    private final String desc;

    /**
     * 根据编码获取枚举
     */
    public static LeftRightMould getByCode(String code) {
        for (LeftRightMould mould : values()) {
            if (mould.getCode().equals(code)) {
                return mould;
            }
        }
        return null;
    }
}
