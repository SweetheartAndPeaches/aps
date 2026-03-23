package com.jinyu.aps.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 班次编码枚举
 *
 * @author APS Team
 * @since 2.0.0
 */
@Getter
@AllArgsConstructor
public enum ShiftCode {

    /**
     * 夜班
     */
    NIGHT("NIGHT", "夜班", 1),

    /**
     * 早班
     */
    DAY("DAY", "早班", 2),

    /**
     * 中班
     */
    AFTERNOON("AFTERNOON", "中班", 3),

    /**
     * 一班
     */
    CLASS1("CLASS1", "一班", 1),

    /**
     * 二班
     */
    CLASS2("CLASS2", "二班", 2),

    /**
     * 三班
     */
    CLASS3("CLASS3", "三班", 3),

    /**
     * 四班
     */
    CLASS4("CLASS4", "四班", 4),

    /**
     * 五班
     */
    CLASS5("CLASS5", "五班", 5),

    /**
     * 六班
     */
    CLASS6("CLASS6", "六班", 6),

    /**
     * 七班
     */
    CLASS7("CLASS7", "七班", 7),

    /**
     * 八班
     */
    CLASS8("CLASS8", "八班", 8);

    private final String code;
    private final String desc;
    private final Integer sortOrder;

    /**
     * 根据编码获取枚举
     */
    public static ShiftCode getByCode(String code) {
        for (ShiftCode shift : values()) {
            if (shift.getCode().equals(code)) {
                return shift;
            }
        }
        return null;
    }
}
