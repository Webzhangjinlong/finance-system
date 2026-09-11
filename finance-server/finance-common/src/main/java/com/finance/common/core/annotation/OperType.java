package com.finance.common.core.annotation;

/**
 * 操作类型（S4 操作日志，docs 5.5）。
 */
public enum OperType {
    /** 新增 */
    INSERT,
    /** 修改 */
    UPDATE,
    /** 删除 */
    DELETE,
    /** 授权/分配（角色、菜单） */
    GRANT,
    /** 审核 */
    AUDIT,
    /** 过账 */
    BOOK,
    /** 结账/反结账 */
    CLOSE,
    /** 打款 */
    PAY,
    /** 导入 */
    IMPORT,
    /** 上传 */
    UPLOAD,
    /** 其他（登录、核算、提交等） */
    OTHER
}
