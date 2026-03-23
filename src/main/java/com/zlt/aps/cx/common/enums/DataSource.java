package com.zlt.aps.cx.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据来源枚举
 *
 * @author APS Team
 * @since 2.0.0
 */
@Getter
@AllArgsConstructor
public enum DataSource {

    /**
     * 自动排程
     */
    AUTO("0", "自动排程"),

    /**
     * 插单
     */
    INSERT("1", "插单"),

    /**
     * 导入
     */
    IMPORT("2", "导入");

    private final String code;
    private final String desc;

    /**
     * 根据编码获取枚举
     */
    public static DataSource getByCode(String code) {
        for (DataSource source : values()) {
            if (source.getCode().equals(code)) {
                return source;
            }
        }
        return null;
    }
}
