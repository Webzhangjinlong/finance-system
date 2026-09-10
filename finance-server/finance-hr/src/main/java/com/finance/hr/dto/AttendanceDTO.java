package com.finance.hr.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 考勤录入/更新入参（H2，员工+工作日唯一，upsert）。
 */
public class AttendanceDTO {

    @NotNull(message = "员工不能为空")
    private Long employeeId;

    @NotNull(message = "日期不能为空")
    private LocalDate workDate;

    private String status;
    private OffsetDateTime checkIn;
    private OffsetDateTime checkOut;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}
