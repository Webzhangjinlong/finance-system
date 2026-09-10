package com.finance.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

/**
 * 附件元数据（sys_attachment，V14）。
 *
 * <p>W3 附件上传：文件本体存 MinIO（object_name = company_code/yyyyMM/uuid.ext），
 * 本表记录元数据，供凭证/报销/合同/员工等业务复用；按 company_code 隔离。</p>
 */
@TableName("sys_attachment")
public class SysAttachment extends BaseEntity {

    private String companyCode;
    /** 原始文件名（展示用，只存 basename）。 */
    private String fileName;
    /** MinIO 对象名：company_code/yyyyMM/uuid.ext。 */
    private String objectName;
    private String bucket;
    /** 访问路径（MinIO endpoint + bucket + objectName）。 */
    private String url;
    private Long fileSize;
    private String contentType;
    /** 业务类型（VOUCHER/EXPENSE/CONTRACT/EMPLOYEE 等，可为空=通用）。 */
    private String bizType;
    private Long bizId;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public Long getBizId() {
        return bizId;
    }

    public void setBizId(Long bizId) {
        this.bizId = bizId;
    }
}
