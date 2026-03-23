package com.zlt.aps.cx.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 排程类型枚举
 *
 * @author APS Team
 * @since 2.0.0
 */
@Getter
@AllArgsConstructor
public enum ScheduleType {

    /**
     * 续作
     */
    CONTINUE("01", "续作"),

    /**
     * 新增
     */
    NEW("02", "新增");

    private final String code;
    private final String desc;

    /**
     * 根据编码获取枚举
     */
    public static ScheduleType getByCode(String code) {
        for (ScheduleType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
