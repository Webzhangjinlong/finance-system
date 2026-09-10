package com.finance.finance.dto;

import java.math.BigDecimal;

/**
 * 账簿查询结果行（F3）。
 *
 * <p>总账 ledger：科目期初 / 本期 / 期末；明细账 detail 与日记账 journal 复用部分字段。</p>
 */
public class BookRow {

    // ---- 科目信息 ----
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private String subjectType;
    /** 科目余额方向：DEBIT / CREDIT。 */
    private String direction;

    // ---- 期初 / 本期 ----
    private BigDecimal initialDebit;
    private BigDecimal initialCredit;
    private BigDecimal periodDebit;
    private BigDecimal periodCredit;

    // ---- 期末（按科目方向归集后） ----
    private BigDecimal endingDebit;
    private BigDecimal endingCredit;

    // ---- 明细/日记账行 ----
    private String voucherNo;
    private String voucherDate;
    private String summary;

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public BigDecimal getInitialDebit() {
        return initialDebit;
    }

    public void setInitialDebit(BigDecimal initialDebit) {
        this.initialDebit = initialDebit;
    }

    public BigDecimal getInitialCredit() {
        return initialCredit;
    }

    public void setInitialCredit(BigDecimal initialCredit) {
        this.initialCredit = initialCredit;
    }

    public BigDecimal getPeriodDebit() {
        return periodDebit;
    }

    public void setPeriodDebit(BigDecimal periodDebit) {
        this.periodDebit = periodDebit;
    }

    public BigDecimal getPeriodCredit() {
        return periodCredit;
    }

    public void setPeriodCredit(BigDecimal periodCredit) {
        this.periodCredit = periodCredit;
    }

    public BigDecimal getEndingDebit() {
        return endingDebit;
    }

    public void setEndingDebit(BigDecimal endingDebit) {
        this.endingDebit = endingDebit;
    }

    public BigDecimal getEndingCredit() {
        return endingCredit;
    }

    public void setEndingCredit(BigDecimal endingCredit) {
        this.endingCredit = endingCredit;
    }

    public String getVoucherNo() {
        return voucherNo;
    }

    public void setVoucherNo(String voucherNo) {
        this.voucherNo = voucherNo;
    }

    public String getVoucherDate() {
        return voucherDate;
    }

    public void setVoucherDate(String voucherDate) {
        this.voucherDate = voucherDate;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
