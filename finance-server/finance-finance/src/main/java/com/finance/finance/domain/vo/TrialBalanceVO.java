package com.finance.finance.domain.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 试算平衡表（F9）：行明细 + 借/贷合计 + 平衡标志。
 */
public class TrialBalanceVO {

    private Integer periodYear;
    private Integer periodMonth;
    private List<TrialBalanceRow> rows;
    private BigDecimal openingDebitTotal;
    private BigDecimal openingCreditTotal;
    private BigDecimal periodDebitTotal;
    private BigDecimal periodCreditTotal;
    private BigDecimal closingDebitTotal;
    private BigDecimal closingCreditTotal;
    /** 借贷是否平衡（期初/本期/期末三对均相等才算平衡）。 */
    private Boolean balanced;

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

    public List<TrialBalanceRow> getRows() {
        return rows;
    }

    public void setRows(List<TrialBalanceRow> rows) {
        this.rows = rows;
    }

    public BigDecimal getOpeningDebitTotal() {
        return openingDebitTotal;
    }

    public void setOpeningDebitTotal(BigDecimal openingDebitTotal) {
        this.openingDebitTotal = openingDebitTotal;
    }

    public BigDecimal getOpeningCreditTotal() {
        return openingCreditTotal;
    }

    public void setOpeningCreditTotal(BigDecimal openingCreditTotal) {
        this.openingCreditTotal = openingCreditTotal;
    }

    public BigDecimal getPeriodDebitTotal() {
        return periodDebitTotal;
    }

    public void setPeriodDebitTotal(BigDecimal periodDebitTotal) {
        this.periodDebitTotal = periodDebitTotal;
    }

    public BigDecimal getPeriodCreditTotal() {
        return periodCreditTotal;
    }

    public void setPeriodCreditTotal(BigDecimal periodCreditTotal) {
        this.periodCreditTotal = periodCreditTotal;
    }

    public BigDecimal getClosingDebitTotal() {
        return closingDebitTotal;
    }

    public void setClosingDebitTotal(BigDecimal closingDebitTotal) {
        this.closingDebitTotal = closingDebitTotal;
    }

    public BigDecimal getClosingCreditTotal() {
        return closingCreditTotal;
    }

    public void setClosingCreditTotal(BigDecimal closingCreditTotal) {
        this.closingCreditTotal = closingCreditTotal;
    }

    public Boolean getBalanced() {
        return balanced;
    }

    public void setBalanced(Boolean balanced) {
        this.balanced = balanced;
    }
}
