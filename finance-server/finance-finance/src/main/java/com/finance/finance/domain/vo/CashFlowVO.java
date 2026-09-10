package com.finance.finance.domain.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 现金流量表（F5，一期简化）：货币资金科目收付汇总。
 */
public class CashFlowVO {

    private List<Item> items = new ArrayList<>();
    private BigDecimal inflow = BigDecimal.ZERO;
    private BigDecimal outflow = BigDecimal.ZERO;
    private BigDecimal netCash = BigDecimal.ZERO;
    private String note;

    public static class Item {
        private String subjectCode;
        private String subjectName;
        private BigDecimal inflow;
        private BigDecimal outflow;

        public Item() {
        }

        public Item(String subjectCode, String subjectName, BigDecimal inflow, BigDecimal outflow) {
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.inflow = inflow;
            this.outflow = outflow;
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

        public BigDecimal getInflow() {
            return inflow;
        }

        public void setInflow(BigDecimal inflow) {
            this.inflow = inflow;
        }

        public BigDecimal getOutflow() {
            return outflow;
        }

        public void setOutflow(BigDecimal outflow) {
            this.outflow = outflow;
        }
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public BigDecimal getInflow() {
        return inflow;
    }

    public void setInflow(BigDecimal inflow) {
        this.inflow = inflow;
    }

    public BigDecimal getOutflow() {
        return outflow;
    }

    public void setOutflow(BigDecimal outflow) {
        this.outflow = outflow;
    }

    public BigDecimal getNetCash() {
        return netCash;
    }

    public void setNetCash(BigDecimal netCash) {
        this.netCash = netCash;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
