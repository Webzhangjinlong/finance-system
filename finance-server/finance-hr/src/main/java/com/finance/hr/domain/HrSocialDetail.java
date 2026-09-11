package com.finance.hr.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

import java.math.BigDecimal;

/**
 * 社保公积金缴费明细（hr_social_detail，company+员工+年月+险种 唯一 uq_hr_social_detail）。
 *
 * <p>每期每员工按险种记录：缴费基数（经上下限 clamp）+ 个人金额 + 单位金额。
 * 金额 NUMERIC(18,2) 硬约束 1；由 SocialInsuranceService 按规则自动核算（智能计算）。</p>
 */
@TableName("hr_social_detail")
public class HrSocialDetail extends BaseEntity {

    private String companyCode;
    private Long employeeId;
    private Integer salaryYear;
    private Integer salaryMonth;
    private String socialType;
    private BigDecimal baseAmount;
    private BigDecimal personalAmount;
    private BigDecimal companyAmount;

    /** 员工姓名（非表字段，列表展示）。 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String empName;

    /** 员工工号（非表字段）。 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String empNo;

    /** 险种名称（非表字段）。 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String typeName;

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

    public String getSocialType() {
        return socialType;
    }

    public void setSocialType(String socialType) {
        this.socialType = socialType;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(BigDecimal baseAmount) {
        this.baseAmount = baseAmount;
    }

    public BigDecimal getPersonalAmount() {
        return personalAmount;
    }

    public void setPersonalAmount(BigDecimal personalAmount) {
        this.personalAmount = personalAmount;
    }

    public BigDecimal getCompanyAmount() {
        return companyAmount;
    }

    public void setCompanyAmount(BigDecimal companyAmount) {
        this.companyAmount = companyAmount;
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

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
}
