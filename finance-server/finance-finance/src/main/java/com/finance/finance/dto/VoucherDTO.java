package com.finance.finance.dto;

import com.finance.finance.domain.FinVoucherEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * 凭证录入/修改请求体（docs 4.2）。
 */
public class VoucherDTO {

    @NotNull(message = "会计年度不能为空")
    private Integer periodYear;

    @NotNull(message = "会计月份不能为空")
    private Integer periodMonth;

    @NotNull(message = "凭证日期不能为空")
    private LocalDate voucherDate;

    private String remark;

    private String sourceType;

    private Long sourceId;

    @Valid
    @NotEmpty(message = "凭证分录不能为空")
    private List<FinVoucherEntry> entries;

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

    public LocalDate getVoucherDate() {
        return voucherDate;
    }

    public void setVoucherDate(LocalDate voucherDate) {
        this.voucherDate = voucherDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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

    public List<FinVoucherEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<FinVoucherEntry> entries) {
        this.entries = entries;
    }
}
