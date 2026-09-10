package com.finance.finance.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

import java.time.LocalDate;

/**
 * 会计期间实体（fin_period）。
 */
@TableName("fin_period")
public class FinPeriod extends BaseEntity {

    private String companyCode;
    private Integer periodYear;
    private Integer periodMonth;
    private String periodStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private String closedBy;
    private java.time.OffsetDateTime closedAt;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
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

    public String getPeriodStatus() {
        return periodStatus;
    }

    public void setPeriodStatus(String periodStatus) {
        this.periodStatus = periodStatus;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(String closedBy) {
        this.closedBy = closedBy;
    }

    public java.time.OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(java.time.OffsetDateTime closedAt) {
        this.closedAt = closedAt;
    }
}
