package com.finance.hr.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/**
 * 工资草稿编辑入参（H3，DRAFT 状态可改；金额 BigDecimal）。
 */
public class SalaryDTO {

    @DecimalMin(value = "0.0", message = "基本工资不能为负")
    private BigDecimal baseSalary;

    @DecimalMin(value = "0.0", message = "奖金不能为负")
    private BigDecimal bonus;

    @DecimalMin(value = "0.0", message = "补贴不能为负")
    private BigDecimal allowance;

    @DecimalMin(value = "0.0", message = "加班费不能为负")
    private BigDecimal overtimePay;

    @DecimalMin(value = "0.0", message = "社保不能为负")
    private BigDecimal socialSecurity;

    @DecimalMin(value = "0.0", message = "公积金不能为负")
    private BigDecimal housingFund;

    @DecimalMin(value = "0.0", message = "其他扣款不能为负")
    private BigDecimal otherDeduct;

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(BigDecimal baseSalary) {
        this.baseSalary = baseSalary;
    }

    public BigDecimal getBonus() {
        return bonus;
    }

    public void setBonus(BigDecimal bonus) {
        this.bonus = bonus;
    }

    public BigDecimal getAllowance() {
        return allowance;
    }

    public void setAllowance(BigDecimal allowance) {
        this.allowance = allowance;
    }

    public BigDecimal getOvertimePay() {
        return overtimePay;
    }

    public void setOvertimePay(BigDecimal overtimePay) {
        this.overtimePay = overtimePay;
    }

    public BigDecimal getSocialSecurity() {
        return socialSecurity;
    }

    public void setSocialSecurity(BigDecimal socialSecurity) {
        this.socialSecurity = socialSecurity;
    }

    public BigDecimal getHousingFund() {
        return housingFund;
    }

    public void setHousingFund(BigDecimal housingFund) {
        this.housingFund = housingFund;
    }

    public BigDecimal getOtherDeduct() {
        return otherDeduct;
    }

    public void setOtherDeduct(BigDecimal otherDeduct) {
        this.otherDeduct = otherDeduct;
    }
}
