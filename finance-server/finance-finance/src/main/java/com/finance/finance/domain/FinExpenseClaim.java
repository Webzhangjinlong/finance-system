package com.finance.finance.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 费用报销单（fin_expense_claim，V10）。
 *
 * <p>状态机（硬约束：状态用枚举/常量，禁魔法值）：
 * DRAFT 草稿 → SUBMITTED 审批中 → APPROVED 已通过（待财务打款）→ PAID 已打款（自动生成凭证）；
 * SUBMITTED 可驳回 → REJECTED（可重新提交回 SUBMITTED）。
 * 已打款（PAID）不可删改；打款生成凭证走 VoucherService（source_type=EXPENSE + source_id 幂等）。</p>
 */
@TableName("fin_expense_claim")
public class FinExpenseClaim extends BaseEntity {

    /** 报销单状态：草稿。 */
    public static final String STATUS_DRAFT = "DRAFT";
    /** 报销单状态：审批中。 */
    public static final String STATUS_SUBMITTED = "SUBMITTED";
    /** 报销单状态：已通过（待财务打款）。 */
    public static final String STATUS_APPROVED = "APPROVED";
    /** 报销单状态：已打款（凭证已生成）。 */
    public static final String STATUS_PAID = "PAID";
    /** 报销单状态：已驳回（可重新提交）。 */
    public static final String STATUS_REJECTED = "REJECTED";

    /** 费用类型：差旅。 */
    public static final String TYPE_TRAVEL = "TRAVEL";
    /** 费用类型：餐饮。 */
    public static final String TYPE_MEAL = "MEAL";
    /** 费用类型：办公。 */
    public static final String TYPE_OFFICE = "OFFICE";
    /** 费用类型：其他。 */
    public static final String TYPE_OTHER = "OTHER";

    /** 凭证来源标识（source_type，幂等键，硬约束 7）。 */
    public static final String SOURCE_TYPE = "EXPENSE";

    private String companyCode;
    private String claimNo;
    private String expenseType;
    private String reason;
    private String invoiceNo;
    private BigDecimal amount;
    private String status;
    private String approver;
    private OffsetDateTime approvedAt;
    private String payer;
    private OffsetDateTime paidAt;

    /** 明细行（非表字段，详情接口装配）。 */
    @TableField(exist = false)
    private List<FinExpenseItem> items;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getClaimNo() {
        return claimNo;
    }

    public void setClaimNo(String claimNo) {
        this.claimNo = claimNo;
    }

    public String getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(String expenseType) {
        this.expenseType = expenseType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approver) {
        this.approver = approver;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(OffsetDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getPayer() {
        return payer;
    }

    public void setPayer(String payer) {
        this.payer = payer;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(OffsetDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public List<FinExpenseItem> getItems() {
        return items;
    }

    public void setItems(List<FinExpenseItem> items) {
        this.items = items;
    }
}
