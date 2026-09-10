package com.finance.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 站内消息（sys_message，V1 + V8）。
 *
 * <p>到期提醒（W4 定时任务）与业务通知写入；remind_date + 单据幂等
 * （uq_sys_message_remind 按 company+receiver+type+business+remind_date 去重）；
 * 消息仅接收人本人可见（receiver_id + company_code 隔离）。</p>
 */
@TableName("sys_message")
public class SysMessage extends BaseEntity {

    /** 消息类型：合同到期提醒。 */
    public static final String TYPE_CONTRACT_EXPIRE = "CONTRACT_EXPIRE";
    /** 消息类型：收付款计划到期提醒。 */
    public static final String TYPE_PLAN_DUE = "PLAN_DUE";
    /** 消息类型：应收逾期提醒。 */
    public static final String TYPE_AR_OVERDUE = "AR_OVERDUE";
    /** 消息类型：应付逾期提醒。 */
    public static final String TYPE_AP_OVERDUE = "AP_OVERDUE";

    private String companyCode;
    private Long receiverId;
    private String messageType;
    private String title;
    private String content;
    private Integer isRead;
    private OffsetDateTime readAt;
    private String businessType;
    private Long businessId;
    /** 提醒日期（幂等键组成：同单同日不重复提醒）。 */
    private LocalDate remindDate;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getIsRead() {
        return isRead;
    }

    public void setIsRead(Integer isRead) {
        this.isRead = isRead;
    }

    public OffsetDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(OffsetDateTime readAt) {
        this.readAt = readAt;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public LocalDate getRemindDate() {
        return remindDate;
    }

    public void setRemindDate(LocalDate remindDate) {
        this.remindDate = remindDate;
    }
}
