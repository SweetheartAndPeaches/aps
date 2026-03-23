package com.zlt.aps.cx.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 生产状态枚举
 *
 * @author APS Team
 * @since 2.0.0
 */
@Getter
@AllArgsConstructor
public enum ProductionStatus {

    /**
     * 未生产
     */
    NOT_STARTED("0", "未生产"),

    /**
     * 生产中
     */
    IN_PROGRESS("1", "生产中"),

    /**
     * 已完成/已收尾
     */
    COMPLETED("2", "已完成");

    private final String code;
    private final String desc;

    /**
     * 根据编码获取枚举
     */
    public static ProductionStatus getByCode(String code) {
        for (ProductionStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
