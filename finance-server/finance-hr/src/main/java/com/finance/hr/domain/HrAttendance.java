package com.finance.hr.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 考勤记录（hr_attendance，员工+工作日唯一 uq_hr_attendance）。
 */
@TableName("hr_attendance")
public class HrAttendance extends BaseEntity {

    public static final String STATUS_NORMAL = "NORMAL";
    public static final String STATUS_LATE = "LATE";
    public static final String STATUS_EARLY = "EARLY";
    public static final String STATUS_ABSENT = "ABSENT";
    public static final String STATUS_LEAVE = "LEAVE";

    private String companyCode;
    private Long employeeId;
    private LocalDate workDate;
    private OffsetDateTime checkIn;
    private OffsetDateTime checkOut;
    private String status;

    /** 工号（非表字段，列表展示用）。 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String empNo;
    /** 姓名（非表字段，列表展示用）。 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String empName;

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

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public OffsetDateTime getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(OffsetDateTime checkIn) {
        this.checkIn = checkIn;
    }

    public OffsetDateTime getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(OffsetDateTime checkOut) {
        this.checkOut = checkOut;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
