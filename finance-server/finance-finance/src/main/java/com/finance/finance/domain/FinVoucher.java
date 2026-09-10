package com.finance.finance.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 记账凭证实体（fin_voucher）。
 */
@TableName("fin_voucher")
public class FinVoucher extends BaseEntity {

    /** 分录列表（非表字段，详情接口装配）。 */
    @TableField(exist = false)
    private List<FinVoucherEntry> entries;

    private String companyCode;
    private Integer periodYear;
    private Integer periodMonth;
    private String voucherNo;
    private LocalDate voucherDate;
    private String voucherStatus;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private String sourceType;
    private Long sourceId;
    private Integer attachCount;
    private String auditor;
    private OffsetDateTime auditedAt;
    private String booker;
    private OffsetDateTime bookedAt;
    private String remark;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public List<FinVoucherEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<FinVoucherEntry> entries) {
        this.entries = entries;
    }

    public Integer getPeriodYear() {
        return periodYear;
    }

    public void setPeriodYear(Integer periodYear) {
        this.periodYear = periodYear;
    }

    public Integer getPeriodMonth() {
        return periodMonth;
    }

    public void setPeriodMonth(Integer periodMonth) {
        this.periodMonth = periodMonth;
    }

    public String getVoucherNo() {
        return voucherNo;
    }

    public void setVoucherNo(String voucherNo) {
        this.voucherNo = voucherNo;
    }

    public LocalDate getVoucherDate() {
        return voucherDate;
    }

    public void setVoucherDate(LocalDate voucherDate) {
        this.voucherDate = voucherDate;
    }

    public String getVoucherStatus() {
        return voucherStatus;
    }

    public void setVoucherStatus(String voucherStatus) {
        this.voucherStatus = voucherStatus;
    }

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    public void setTotalDebit(BigDecimal totalDebit) {
        this.totalDebit = totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public void setTotalCredit(BigDecimal totalCredit) {
        this.totalCredit = totalCredit;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public Integer getAttachCount() {
        return attachCount;
    }

    public void setAttachCount(Integer attachCount) {
        this.attachCount = attachCount;
    }

    public String getAuditor() {
        return auditor;
    }

    public void setAuditor(String auditor) {
        this.auditor = auditor;
    }

    public OffsetDateTime getAuditedAt() {
        return auditedAt;
    }

    public void setAuditedAt(OffsetDateTime auditedAt) {
        this.auditedAt = auditedAt;
    }

    public String getBooker() {
        return booker;
    }

    public void setBooker(String booker) {
        this.booker = booker;
    }

    public OffsetDateTime getBookedAt() {
        return bookedAt;
    }

    public void setBookedAt(OffsetDateTime bookedAt) {
        this.bookedAt = bookedAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
