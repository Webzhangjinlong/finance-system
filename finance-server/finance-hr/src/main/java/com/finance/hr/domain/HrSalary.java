package com.finance.hr.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 工资单（hr_salary，员工+年月唯一 uq_hr_salary；金额 NUMERIC(18,2) 硬约束 1）。
 *
 * <p>状态机 DRAFT→CONFIRMED→PAID；个税按累计预扣法（硬约束 9）。</p>
 */
@TableName("hr_salary")
public class HrSalary extends BaseEntity {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_PAID = "PAID";

    private String companyCode;
    private Long employeeId;
    private Integer salaryYear;
    private Integer salaryMonth;
    private BigDecimal baseSalary;
    private BigDecimal bonus;
    private BigDecimal allowance;
    private BigDecimal overtimePay;
    private BigDecimal socialSecurity;
    private BigDecimal housingFund;
    private BigDecimal tax;
    private BigDecimal otherDeduct;
    private BigDecimal netPay;
    private String status;
    private OffsetDateTime paidAt;

    /** 工资明细（非表字段，工资条装配）。 */
    @TableField(exist = false)
    private List<HrSalaryItem> items;

    /** 员工姓名（非表字段，列表展示）。 */
    @TableField(exist = false)
    private String empName;

    /** 员工工号（非表字段）。 */
    @TableField(exist = false)
    private String empNo;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Integer getSalaryYear() {
        return salaryYear;
    }

    public void setSalaryYear(Integer salaryYear) {
        this.salaryYear = salaryYear;
    }

    public Integer getSalaryMonth() {
        return salaryMonth;
    }

    public void setSalaryMonth(Integer salaryMonth) {
        this.salaryMonth = salaryMonth;
    }

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

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getOtherDeduct() {
        return otherDeduct;
    }

    public void setOtherDeduct(BigDecimal otherDeduct) {
        this.otherDeduct = otherDeduct;
    }

    public BigDecimal getNetPay() {
        return netPay;
    }

    public void setNetPay(BigDecimal netPay) {
        this.netPay = netPay;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(OffsetDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public List<HrSalaryItem> getItems() {
        return items;
    }

    public void setItems(List<HrSalaryItem> items) {
        this.items = items;
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
    }

    public String getEmpNo() {
        return empNo;
    }

    public void setEmpNo(String empNo) {
        this.empNo = empNo;
    }
}
