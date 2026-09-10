package com.finance.finance.enums;

/**
 * 科目余额方向（fin_subject.direction 合法取值）。
 */
public enum SubjectDirection {

    /** 借方 */
    DEBIT,
    /** 贷方 */
    CREDIT;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (SubjectDirection d : values()) {
            if (d.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
