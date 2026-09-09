package com.finance.common.constant;

/**
 * 全局常量。
 */
public final class Constants {

    private Constants() {
    }

    /** 成功标记 */
    public static final int SUCCESS = 200;

    /** 认证请求头 */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** JWT 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 逻辑删除字段名 */
    public static final String DEL_FLAG = "deleted";

    /** 公司编码字段名（多公司隔离贯穿字段） */
    public static final String COMPANY_CODE = "company_code";
}
