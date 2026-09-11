package com.finance.finance.domain.vo;

import java.math.BigDecimal;

/**
 * 报表取数行（F5）：单科目期间/累计借发生额与贷发生额。
 */
public class ReportRow {

    private String subjectCode;
    private String subjectName;
    private String subjectType;
    private String direction;
    private BigDecimal periodDebit;
    private BigDecimal periodCredit;
    /** 月份（费用趋势按 科目×月 汇总时使用，其余查询为 null）。 */
    private Integer periodMonth;

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

    public Integer getPeriodMonth() {
        return periodMonth;
    }

    public void setPeriodMonth(Integer periodMonth) {
        this.periodMonth = periodMonth;
    }
}
