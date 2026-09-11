package com.finance.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

import java.time.OffsetDateTime;

/**
 * 操作日志实体（sys_oper_log，S4，docs 5.5）：只增不删（deleted 恒 0），审计关键写操作。
 *
 * <p>V1 建表 + V16 补五件套/cost_time/json_result；字段对齐 V1 命名（business_type/oper_param）。</p>
 */
@TableName("sys_oper_log")
public class SysOperLog extends BaseEntity {

    /** 操作模块标题（凭证审核/合同审批/重置密码…） */
    private String title;

    /** 业务操作类型（INSERT/UPDATE/DELETE/GRANT/AUDIT/BOOK/CLOSE/PAY/IMPORT/UPLOAD/OTHER） */
    private String businessType;

    /** 方法名（类.方法） */
    private String method;

    /** HTTP 方法 */
    private String requestMethod;

    /** 操作人 */
    private String operName;

    /** 请求路径 */
    private String operUrl;

    /** 操作 IP */
    private String operIp;

    /** 操作地点（IP 属地，预留） */
    private String operLocation;

    /** 请求参数（JSON，敏感字段已脱敏，截断 2000） */
    private String operParam;

    /** 返回结果（JSON，脱敏/截断 2000） */
    private String jsonResult;

    /** 状态：0=失败 1=成功 */
    private Integer status;

    /** 错误消息（失败时） */
    private String errorMsg;

    /** 执行耗时（毫秒） */
    private Long costTime;

    /** 操作时间 */
    private OffsetDateTime operTime;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getOperName() {
        return operName;
    }

    public void setOperName(String operName) {
        this.operName = operName;
    }

    public String getOperUrl() {
        return operUrl;
    }

    public void setOperUrl(String operUrl) {
        this.operUrl = operUrl;
    }

    public String getOperIp() {
        return operIp;
    }

    public void setOperIp(String operIp) {
        this.operIp = operIp;
    }

    public String getOperLocation() {
        return operLocation;
    }

    public void setOperLocation(String operLocation) {
        this.operLocation = operLocation;
    }

    public String getOperParam() {
        return operParam;
    }

    public void setOperParam(String operParam) {
        this.operParam = operParam;
    }

    public String getJsonResult() {
        return jsonResult;
    }

    public void setJsonResult(String jsonResult) {
        this.jsonResult = jsonResult;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public Long getCostTime() {
        return costTime;
    }

    public void setCostTime(Long costTime) {
        this.costTime = costTime;
    }

    public OffsetDateTime getOperTime() {
        return operTime;
    }

    public void setOperTime(OffsetDateTime operTime) {
        this.operTime = operTime;
    }
}
