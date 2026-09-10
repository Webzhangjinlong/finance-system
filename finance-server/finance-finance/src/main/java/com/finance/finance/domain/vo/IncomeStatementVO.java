package com.finance.finance.domain.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 利润表（F5）：损益类科目本期发生额，收入 − 费用 = 净利润。
 */
public class IncomeStatementVO {

    private List<Line> revenues = new ArrayList<>();
    private List<Line> expenses = new ArrayList<>();
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private BigDecimal totalExpense = BigDecimal.ZERO;
    private BigDecimal netProfit = BigDecimal.ZERO;

    public static class Line {
        private String subjectCode;
        private String subjectName;
        private BigDecimal amount;

        public Line() {
        }

        public Line(String subjectCode, String subjectName, BigDecimal amount) {
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.amount = amount;
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

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    public List<Line> getRevenues() {
        return revenues;
    }

    public void setRevenues(List<Line> revenues) {
        this.revenues = revenues;
    }

    public List<Line> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<Line> expenses) {
        this.expenses = expenses;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public BigDecimal getNetProfit() {
        return netProfit;
    }

    public void setNetProfit(BigDecimal netProfit) {
        this.netProfit = netProfit;
    }
}
