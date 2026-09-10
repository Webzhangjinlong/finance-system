package com.finance.common.core.exception;

/**
 * 业务异常。
 *
 * <p>业务规则校验失败时抛出，由全局异常处理器转换为统一 Result，禁止异常堆栈直接暴露给前端。</p>
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
