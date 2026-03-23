package com.jinyu.aps.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 机台状态枚举
 *
 * @author APS Team
 * @since 2.0.0
 */
@Getter
@AllArgsConstructor
public enum MachineStatus {

    /**
     * 正常
     */
    NORMAL("0", "正常"),

    /**
     * 维护中
     */
    MAINTAINING("1", "维护中"),

    /**
     * 故障
     */
    FAULT("2", "故障"),

    /**
     * 停用
     */
    DISABLED("3", "停用");

    private final String code;
    private final String desc;

    /**
     * 根据编码获取枚举
     */
    public static MachineStatus getByCode(String code) {
        for (MachineStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
