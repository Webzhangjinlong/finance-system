package com.finance.hr.controller;

import com.finance.common.core.Result;
import com.finance.common.core.domain.PageResult;
import com.finance.framework.security.SecurityUtils;
import com.finance.hr.domain.HrAttendance;
import com.finance.hr.domain.HrDepartment;
import com.finance.hr.domain.HrEmployee;
import com.finance.hr.dto.AttendanceDTO;
import com.finance.hr.dto.DepartmentDTO;
import com.finance.hr.dto.EmployeeDTO;
import com.finance.hr.service.HrService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 人事接口（H1 部门/员工档案 + H2 考勤）：/hr/*。
 *
 * <p>公司隔离取当前登录公司；权限码 hr:employee:list/add/edit、hr:attendance:list/edit（V12 授权 admin）。</p>
 */
@RestController
@RequestMapping("/hr")
public class HrController {

    private final HrService hrService;

    public HrController(HrService hrService) {
        this.hrService = hrService;
    }

    // ==================== 部门 ====================

    @GetMapping("/department/tree")
    @PreAuthorize("hasAuthority('hr:employee:list')")
    public Result<List<HrDepartment>> departmentTree() {
        return Result.ok(hrService.departmentTree(SecurityUtils.getCompanyCode()));
    }

    @PostMapping("/department")
    @PreAuthorize("hasAuthority('hr:employee:add')")
    public Result<Long> createDepartment(@Valid @RequestBody DepartmentDTO dto) {
        return Result.ok(hrService.createDepartment(SecurityUtils.getCompanyCode(), dto));
    }

    @PutMapping("/department/{id}")
    @PreAuthorize("hasAuthority('hr:employee:edit')")
    public Result<Void> updateDepartment(@PathVariable Long id, @Valid @RequestBody DepartmentDTO dto) {
        hrService.updateDepartment(SecurityUtils.getCompanyCode(), id, dto);
        return Result.ok();
    }

    @DeleteMapping("/department/{id}")
    @PreAuthorize("hasAuthority('hr:employee:del')")
    public Result<Void> deleteDepartment(@PathVariable Long id) {
        hrService.deleteDepartment(SecurityUtils.getCompanyCode(), id);
        return Result.ok();
    }

    // ==================== 员工 ====================

    @GetMapping("/employee/page")
    @PreAuthorize("hasAuthority('hr:employee:list')")
    public Result<PageResult<HrEmployee>> employeePage(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Long deptId,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "10") long size) {
        return Result.ok(hrService.employeePage(SecurityUtils.getCompanyCode(), keyword, deptId, status, page, size));
    }

    @GetMapping("/employee/{id}")
    @PreAuthorize("hasAuthority('hr:employee:list')")
    public Result<HrEmployee> employeeDetail(@PathVariable Long id) {
        return Result.ok(hrService.employeeDetail(SecurityUtils.getCompanyCode(), id));
    }

    @PostMapping("/employee")
    @PreAuthorize("hasAuthority('hr:employee:add')")
    public Result<Long> createEmployee(@Valid @RequestBody EmployeeDTO dto) {
        return Result.ok(hrService.createEmployee(SecurityUtils.getCompanyCode(), dto));
    }

    @PutMapping("/employee/{id}")
    @PreAuthorize("hasAuthority('hr:employee:edit')")
    public Result<Void> updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeDTO dto) {
        hrService.updateEmployee(SecurityUtils.getCompanyCode(), id, dto);
        return Result.ok();
    }

    @PutMapping("/employee/{id}/status")
    @PreAuthorize("hasAuthority('hr:employee:edit')")
    public Result<Void> updateEmployeeStatus(@PathVariable Long id, @RequestParam String status) {
        hrService.updateEmployeeStatus(SecurityUtils.getCompanyCode(), id, status);
        return Result.ok();
    }

    @DeleteMapping("/employee/{id}")
    @PreAuthorize("hasAuthority('hr:employee:del')")
    public Result<Void> deleteEmployee(@PathVariable Long id) {
        hrService.deleteEmployee(SecurityUtils.getCompanyCode(), id);
        return Result.ok();
    }

    // ==================== 考勤 ====================

    @GetMapping("/attendance/page")
    @PreAuthorize("hasAuthority('hr:attendance:list')")
    public Result<PageResult<HrAttendance>> attendancePage(@RequestParam(required = false) Long employeeId,
                                                           @RequestParam(required = false) Integer year,
                                                           @RequestParam(required = false) Integer month,
                                                           @RequestParam(defaultValue = "1") long page,
                                                           @RequestParam(defaultValue = "10") long size) {
        return Result.ok(hrService.attendancePage(SecurityUtils.getCompanyCode(), employeeId, year, month, page, size));
    }

    @PostMapping("/attendance")
    @PreAuthorize("hasAuthority('hr:attendance:edit')")
    public Result<Void> upsertAttendance(@Valid @RequestBody AttendanceDTO dto) {
        hrService.upsertAttendance(SecurityUtils.getCompanyCode(), dto);
        return Result.ok();
    }
}
