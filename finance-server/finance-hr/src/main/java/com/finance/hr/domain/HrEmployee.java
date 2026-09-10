package com.finance.hr.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

import java.time.LocalDate;

/**
 * 员工档案（hr_employee，工号公司内唯一 uq_hr_employee_no）。
 */
@TableName("hr_employee")
public class HrEmployee extends BaseEntity {

    /** 在职。 */
    public static final String STATUS_ONBOARD = "ONBOARD";
    /** 离职（离职即停用账号，历史数据保留）。 */
    public static final String STATUS_LEAVE = "LEAVE";

    private String companyCode;
    private String empNo;
    private String empName;
    private String gender;
    private String idCard;
    private String phone;
    private String email;
    private Long deptId;
    private String position;
    private LocalDate hireDate;
    private LocalDate leaveDate;
    private String salaryAccount;
    private String status;
    private Long userId;

    /** 部门名称（非表字段，列表展示用）。 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String deptName;

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getEmpNo() {
        return empNo;
    }

    public void setEmpNo(String empNo) {
        this.empNo = empNo;
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public LocalDate getLeaveDate() {
        return leaveDate;
    }

    public void setLeaveDate(LocalDate leaveDate) {
        this.leaveDate = leaveDate;
    }

    public String getSalaryAccount() {
        return salaryAccount;
    }

    public void setSalaryAccount(String salaryAccount) {
        this.salaryAccount = salaryAccount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
