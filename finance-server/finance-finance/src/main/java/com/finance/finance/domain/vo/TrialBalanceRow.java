package com.finance.finance.domain.vo;

import java.math.BigDecimal;

/**
 * 试算平衡表行（F9）：科目期初/本期发生/期末余额（借/贷双栏）。
 *
 * <p>口径：期初 = 截止上期累计 BOOKED 凭证净额；本期 = 本期间发生；
 * 期末 = 期初 + 本期。所有金额按借贷分栏存储，期末余额归属方向由
 * 科目 direction 决定（资产/成本/费用 DEBIT 记借方余额，负债/权益/收入 CREDIT 记贷方余额）。</p>
 */
public class TrialBalanceRow {

    private String subjectCode;
    private String subjectName;
    private String subjectType;
    private String direction;
    private BigDecimal openingDebit;
    private BigDecimal openingCredit;
    private BigDecimal periodDebit;
    private BigDecimal periodCredit;
    private BigDecimal closingDebit;
    private BigDecimal closingCredit;
    /** 有余额或本期有发生（前端可筛选是否只看非零科目）。 */
    private Boolean active;

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

    public BigDecimal getOpeningDebit() {
        return openingDebit;
    }

    public void setOpeningDebit(BigDecimal openingDebit) {
        this.openingDebit = openingDebit;
    }

    public BigDecimal getOpeningCredit() {
        return openingCredit;
    }

    public void setOpeningCredit(BigDecimal openingCredit) {
        this.openingCredit = openingCredit;
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

    public BigDecimal getClosingDebit() {
        return closingDebit;
    }

    public void setClosingDebit(BigDecimal closingDebit) {
        this.closingDebit = closingDebit;
    }

    public BigDecimal getClosingCredit() {
        return closingCredit;
    }

    public void setClosingCredit(BigDecimal closingCredit) {
        this.closingCredit = closingCredit;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
