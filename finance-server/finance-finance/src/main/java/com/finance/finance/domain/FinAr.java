package com.finance.finance.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应收账款（fin_ar，V1）。
 *
 * <p>由合同收付款计划到期生成（plan_id 唯一约束 uq_fin_ar_plan 保证幂等）；
 * 核销回写 received_amount/status（OPEN/PARTIAL/SETTLED/BADDEBT），
 * ck_fin_ar_received 保证不超过应收金额（超额拦截 R10）。</p>
 */
@TableName("fin_ar")
public class FinAr extends BaseEntity {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_SETTLED = "SETTLED";

    private String companyCode;
    private String arNo;
    private Long contractId;
    /** 来源收付款计划（幂等键：company_code + plan_id 唯一）。 */
    private Long planId;
    private String customerName;
    private BigDecimal amount;
    private BigDecimal receivedAmount;
    private String status;
    private LocalDate dueDate;
    private Long voucherId;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getArNo() {
        return arNo;
    }

    public void setArNo(String arNo) {
        this.arNo = arNo;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getReceivedAmount() {
        return receivedAmount;
    }

    public void setReceivedAmount(BigDecimal receivedAmount) {
        this.receivedAmount = receivedAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }
}
