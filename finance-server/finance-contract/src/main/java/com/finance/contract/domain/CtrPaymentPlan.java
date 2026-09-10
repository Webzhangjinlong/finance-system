package com.finance.contract.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 合同收付款计划（ctr_payment_plan，V1）。
 *
 * <p>合同生效后按条款自动生成（一期：全额一期）；计划状态 UNPAID 未收付 /
 * PARTIAL 部分收付 / PAID 已收付 / OVERDUE 已逾期（到期未清，核销回写）；
 * 到期经 C3 同步生成应收/应付单（幂等），核销后回写已收/已付金额；
 * paid_amount 由 ck_ctr_plan_paid 约束不超过 amount（超额拦截 R10）。</p>
 */
@TableName("ctr_payment_plan")
public class CtrPaymentPlan extends BaseEntity {

    /** 计划方向：付款。 */
    public static final String PLAN_TYPE_PAYMENT = "PAYMENT";
    /** 计划方向：收款。 */
    public static final String PLAN_TYPE_RECEIPT = "RECEIPT";

    /** 计划状态：未收付。 */
    public static final String PLAN_STATUS_UNPAID = "UNPAID";
    /** 计划状态：部分收付。 */
    public static final String PLAN_STATUS_PARTIAL = "PARTIAL";
    /** 计划状态：已收付。 */
    public static final String PLAN_STATUS_PAID = "PAID";
    /** 计划状态：已逾期（到期未清）。 */
    public static final String PLAN_STATUS_OVERDUE = "OVERDUE";

    private String companyCode;
    private Long contractId;
    private String planType;
    private String planNo;
    private LocalDate planDate;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private String status;
    private Integer reminderSent;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public String getPlanNo() {
        return planNo;
    }

    public void setPlanNo(String planNo) {
        this.planNo = planNo;
    }

    public LocalDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(LocalDate planDate) {
        this.planDate = planDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getReminderSent() {
        return reminderSent;
    }

    public void setReminderSent(Integer reminderSent) {
        this.reminderSent = reminderSent;
    }
}
