package com.finance.finance.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应付账款（fin_ap，V1）。
 *
 * <p>由合同收付款计划到期生成（plan_id 唯一约束 uq_fin_ap_plan 保证幂等）；
 * 核销回写 paid_amount/status（OPEN/PARTIAL/SETTLED），
 * ck_fin_ap_paid 保证不超过应付金额（超额拦截 R10）。</p>
 */
@TableName("fin_ap")
public class FinAp extends BaseEntity {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_SETTLED = "SETTLED";

    private String companyCode;
    private String apNo;
    private Long contractId;
    /** 来源收付款计划（幂等键：company_code + plan_id 唯一）。 */
    private Long planId;
    private String supplierName;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private String status;
    private LocalDate dueDate;
    private Long voucherId;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getApNo() {
        return apNo;
    }

    public void setApNo(String apNo) {
        this.apNo = apNo;
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

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
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
