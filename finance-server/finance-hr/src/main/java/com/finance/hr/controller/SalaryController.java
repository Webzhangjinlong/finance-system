package com.finance.hr.controller;

import com.finance.common.core.Result;
import com.finance.common.core.domain.PageResult;
import com.finance.framework.security.SecurityUtils;
import com.finance.hr.domain.HrSalary;
import com.finance.hr.dto.SalaryDTO;
import com.finance.hr.service.SalaryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工资核算接口（H3）：/salary。
 *
 * <p>状态机 DRAFT→CONFIRMED→PAID；个税累计预扣法；工资条仅本人可见（admin 演示放行）。</p>
 */
@RestController
@RequestMapping("/salary")
public class SalaryController {

    private final SalaryService salaryService;

    public SalaryController(SalaryService salaryService) {
        this.salaryService = salaryService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('hr:salary:list')")
    public Result<PageResult<HrSalary>> list(@RequestParam(required = false) Integer year,
                                             @RequestParam(required = false) Integer month,
                                             @RequestParam(required = false) Long employeeId,
                                             @RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size) {
        return Result.ok(salaryService.page(SecurityUtils.getCompanyCode(), year, month, employeeId, page, size));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:salary:calculate')")
    public Result<Void> editDraft(@PathVariable Long id, @Valid @RequestBody SalaryDTO dto) {
        salaryService.editDraft(SecurityUtils.getCompanyCode(), id, dto);
        return Result.ok();
    }

    @PostMapping("/calculate")
    @PreAuthorize("hasAuthority('hr:salary:calculate')")
    public Result<Integer> calculate(@RequestParam int year,
                                     @RequestParam int month,
                                     @RequestParam(required = false) Long employeeId) {
        return Result.ok(salaryService.calculate(SecurityUtils.getCompanyCode(), year, month, employeeId));
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('hr:salary:submit')")
    public Result<Void> submit(@PathVariable Long id) {
        salaryService.submit(SecurityUtils.getCompanyCode(), id);
        return Result.ok();
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('hr:salary:approve')")
    public Result<Void> approve(@PathVariable Long id) {
        salaryService.approve(SecurityUtils.getCompanyCode(), id);
        return Result.ok();
    }

    @GetMapping("/{id}/slip")
    @PreAuthorize("hasAuthority('hr:salary:slip')")
    public Result<HrSalary> slip(@PathVariable Long id) {
        return Result.ok(salaryService.slip(SecurityUtils.getCompanyCode(), id,
                SecurityUtils.getUserId(), SecurityUtils.getUsername()));
    }
}
