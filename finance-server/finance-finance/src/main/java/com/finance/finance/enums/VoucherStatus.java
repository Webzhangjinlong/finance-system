package com.finance.finance.enums;

/**
 * 凭证状态机合法取值（硬约束 4：DRAFT → AUDITED → BOOKED → REVERSED）。
 */
public enum VoucherStatus {

    /** 草稿：可编辑/删除 */
    DRAFT,
    /** 已审核：禁止修改，可过账 */
    AUDITED,
    /** 已过账：只读，可冲销 */
    BOOKED,
    /** 已冲销 */
    REVERSED;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (VoucherStatus s : values()) {
            if (s.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
