package com.finance.contract.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 合同登记/修改入参（C1，docs 5.1）。
 *
 * <p>入参一律 @Valid + JSR-303（硬约束 22）；金额 BigDecimal（硬约束 1）。</p>
 */
public class ContractDTO {

    @NotBlank(message = "合同名称不能为空")
    @Size(max = 128, message = "合同名称最长 128 字符")
    private String contractName;

    @NotBlank(message = "签约方不能为空")
    @Size(max = 128, message = "签约方最长 128 字符")
    private String counterparty;

    @NotBlank(message = "合同类型不能为空")
    @Size(max = 16, message = "合同类型最长 16 字符")
    private String contractType;

    @NotNull(message = "合同金额不能为空")
    @DecimalMin(value = "0.00", message = "合同金额不能为负")
    private BigDecimal amount;

    private LocalDate signDate;
    private LocalDate startDate;
    private LocalDate endDate;

    @Size(max = 255, message = "备注最长 255 字符")
    private String remark;

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getSignDate() {
        return signDate;
    }

    public void setSignDate(LocalDate signDate) {
        this.signDate = signDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
