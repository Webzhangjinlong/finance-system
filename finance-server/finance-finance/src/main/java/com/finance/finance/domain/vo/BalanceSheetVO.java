package com.finance.finance.domain.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 资产负债表（F5）：科目余额分资产/负债/权益，损益净额计入未分配利润。
 */
public class BalanceSheetVO {

    private List<Item> items = new ArrayList<>();
    private BigDecimal totalAssets = BigDecimal.ZERO;
    private BigDecimal totalLiabilities = BigDecimal.ZERO;
    private BigDecimal totalEquity = BigDecimal.ZERO;
    private BigDecimal netProfit = BigDecimal.ZERO;
    private BigDecimal totalLiabEquity = BigDecimal.ZERO;
    private boolean balanced;

    public static class Item {
        private String subjectCode;
        private String subjectName;
        private String section;
        private BigDecimal balance;

        public Item() {
        }

        public Item(String subjectCode, String subjectName, String section, BigDecimal balance) {
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.section = section;
            this.balance = balance;
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

        public String getSection() {
            return section;
        }

        public void setSection(String section) {
            this.section = section;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public void setBalance(BigDecimal balance) {
            this.balance = balance;
        }
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public BigDecimal getTotalAssets() {
        return totalAssets;
    }

    public void setTotalAssets(BigDecimal totalAssets) {
        this.totalAssets = totalAssets;
    }

    public BigDecimal getTotalLiabilities() {
        return totalLiabilities;
    }

    public void setTotalLiabilities(BigDecimal totalLiabilities) {
        this.totalLiabilities = totalLiabilities;
    }

    public BigDecimal getTotalEquity() {
        return totalEquity;
    }

    public void setTotalEquity(BigDecimal totalEquity) {
        this.totalEquity = totalEquity;
    }

    public BigDecimal getNetProfit() {
        return netProfit;
    }

    public void setNetProfit(BigDecimal netProfit) {
        this.netProfit = netProfit;
    }

    public BigDecimal getTotalLiabEquity() {
        return totalLiabEquity;
    }

    public void setTotalLiabEquity(BigDecimal totalLiabEquity) {
        this.totalLiabEquity = totalLiabEquity;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public void setBalanced(boolean balanced) {
        this.balanced = balanced;
    }
}
