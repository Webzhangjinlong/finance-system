package com.finance.finance.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 应收/应付账龄（F7，docs 4.7）：按到期日分档（未到期/0-30/31-60/61-90/90+）。
 */
public class AgingVO {

    /** 账龄分档常量。 */
    public static final String NOT_DUE = "NOT_DUE";
    public static final String D0_30 = "D0_30";
    public static final String D31_60 = "D31_60";
    public static final String D61_90 = "D61_90";
    public static final String D90_PLUS = "D90_PLUS";

    private String type;
    private LocalDate asOf;
    private List<SummaryItem> summary = new ArrayList<>();
    private List<DetailItem> detail = new ArrayList<>();

    public static class SummaryItem {
        private String bucket;
        private long count;
        private BigDecimal amount = BigDecimal.ZERO;

        public SummaryItem() {
        }

        public SummaryItem(String bucket, long count, BigDecimal amount) {
            this.bucket = bucket;
            this.count = count;
            this.amount = amount;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    public static class DetailItem {
        private String docNo;
        private String counterparty;
        private LocalDate dueDate;
        private BigDecimal amount;
        private BigDecimal settled;
        private BigDecimal balance;
        private long days;
        private String bucket;

        public DetailItem() {
        }

        public DetailItem(String docNo, String counterparty, LocalDate dueDate,
                          BigDecimal amount, BigDecimal settled, BigDecimal balance,
                          long days, String bucket) {
            this.docNo = docNo;
            this.counterparty = counterparty;
            this.dueDate = dueDate;
            this.amount = amount;
            this.settled = settled;
            this.balance = balance;
            this.days = days;
            this.bucket = bucket;
        }

        public String getDocNo() {
            return docNo;
        }

        public void setDocNo(String docNo) {
            this.docNo = docNo;
        }

        public String getCounterparty() {
            return counterparty;
        }

        public void setCounterparty(String counterparty) {
            this.counterparty = counterparty;
        }

        public LocalDate getDueDate() {
            return dueDate;
        }

        public void setDueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getSettled() {
            return settled;
        }

        public void setSettled(BigDecimal settled) {
            this.settled = settled;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public void setBalance(BigDecimal balance) {
            this.balance = balance;
        }

        public long getDays() {
            return days;
        }

        public void setDays(long days) {
            this.days = days;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDate getAsOf() {
        return asOf;
    }

    public void setAsOf(LocalDate asOf) {
        this.asOf = asOf;
    }

    public List<SummaryItem> getSummary() {
        return summary;
    }

    public void setSummary(List<SummaryItem> summary) {
        this.summary = summary;
    }

    public List<DetailItem> getDetail() {
        return detail;
    }

    public void setDetail(List<DetailItem> detail) {
        this.detail = detail;
    }
}
