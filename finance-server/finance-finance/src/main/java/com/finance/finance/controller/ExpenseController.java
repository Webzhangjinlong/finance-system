package com.finance.finance.controller;

import com.finance.common.core.Result;
import com.finance.common.core.annotation.OperLog;
import com.finance.common.core.annotation.OperType;
import com.finance.common.core.domain.PageResult;
import com.finance.finance.domain.FinExpenseClaim;
import com.finance.finance.dto.ExpenseClaimDTO;
import com.finance.finance.dto.ExpenseSubmitDTO;
import com.finance.finance.service.ExpenseService;
import com.finance.framework.security.SecurityUtils;
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

/**
 * 费用报销接口（F6，Gate 13）。
 *
 * <p>权限：列表/详情 finance:expense:list；新增 finance:expense:add；编辑 finance:expense:edit；
 * 审批 finance:expense:approve（部门负责人）；打款 finance:expense:pay（财务）。
 * 审批动作走 W1 审批流（/workflow/todo 待办 + 本控制器回调）。</p>
 */
@RestController
@RequestMapping("/expense")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('finance:expense:list')")
    public Result<PageResult<FinExpenseClaim>> page(@RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size) {
        return Result.ok(expenseService.page(SecurityUtils.getCompanyCode(), keyword, status, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:expense:list')")
    public Result<FinExpenseClaim> detail(@PathVariable Long id) {
        return Result.ok(expenseService.detail(SecurityUtils.getCompanyCode(), id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:expense:add')")
    public Result<FinExpenseClaim> create(@Valid @RequestBody ExpenseClaimDTO dto) {
        return Result.ok(expenseService.create(SecurityUtils.getCompanyCode(), dto));
    }

    @OperLog(title = "报销修改", operType = OperType.UPDATE)
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:expense:edit')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ExpenseClaimDTO dto) {
        expenseService.update(SecurityUtils.getCompanyCode(), id, dto);
        return Result.ok();
    }

    @OperLog(title = "报销删除", operType = OperType.DELETE)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:expense:edit')")
    public Result<Void> delete(@PathVariable Long id) {
        expenseService.delete(SecurityUtils.getCompanyCode(), id);
        return Result.ok();
    }

    @OperLog(title = "报销提交", operType = OperType.OTHER)
    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('finance:expense:edit')")
    public Result<Void> submit(@PathVariable Long id, @Valid @RequestBody ExpenseSubmitDTO dto) {
        expenseService.submit(SecurityUtils.getCompanyCode(), id, dto);
        return Result.ok();
    }

    /** 审批通过回调（待办在 /workflow/todo，审批人点击后回调本端点）。 */
    @OperLog(title = "报销审批", operType = OperType.AUDIT)
    @PutMapping("/task/{taskId}/approve")
    @PreAuthorize("hasAuthority('finance:expense:approve')")
    public Result<Void> approveTask(@PathVariable String taskId, @RequestParam(required = false) String comment) {
        expenseService.approveTask(taskId, comment);
        return Result.ok();
    }

    /** 审批驳回回调。 */
    @OperLog(title = "报销驳回", operType = OperType.OTHER)
    @PutMapping("/task/{taskId}/reject")
    @PreAuthorize("hasAuthority('finance:expense:approve')")
    public Result<Void> rejectTask(@PathVariable String taskId, @RequestParam(required = false) String comment) {
        expenseService.rejectTask(taskId, comment);
        return Result.ok();
    }

    /** 财务打款（APPROVED → PAID，自动生成费用凭证，source 幂等）。 */
    @OperLog(title = "报销打款", operType = OperType.PAY)
    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('finance:expense:pay')")
    public Result<Void> pay(@PathVariable Long id) {
        expenseService.pay(SecurityUtils.getCompanyCode(), id);
        return Result.ok();
    }
}
