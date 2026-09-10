package com.finance.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 员工档案入参（H1，工号唯一）。
 */
public class EmployeeDTO {

    @NotBlank(message = "工号不能为空")
    @Size(max = 32, message = "工号过长")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "工号仅允许字母数字下划线中划线")
    private String empNo;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名过长")
    private String empName;

    private String gender;
    private String idCard;
    private String phone;
    private String email;
    private Long deptId;
    private String position;
    private LocalDate hireDate;
    private LocalDate leaveDate;
    /** 劳动合同到期日（V14）。 */
    private LocalDate contractExpireDate;
    private String salaryAccount;
    private String status;
    private Long userId;

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

    public LocalDate getContractExpireDate() {
        return contractExpireDate;
    }

    public void setContractExpireDate(LocalDate contractExpireDate) {
        this.contractExpireDate = contractExpireDate;
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
