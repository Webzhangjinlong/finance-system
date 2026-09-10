package com.finance.finance.enums;

/**
 * 科目类别（fin_subject.subject_type 合法取值）。
 */
public enum SubjectType {

    /** 资产 */
    ASSET,
    /** 负债 */
    LIABILITY,
    /** 权益 */
    EQUITY,
    /** 成本 */
    COST,
    /** 损益 */
    PROFIT;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (SubjectType t : values()) {
            if (t.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
