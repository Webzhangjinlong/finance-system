package com.finance.finance.controller;

import com.finance.common.core.Result;
import com.finance.common.core.annotation.OperLog;
import com.finance.common.core.annotation.OperType;
import com.finance.common.core.domain.PageResult;
import com.finance.finance.domain.FinVoucherRule;
import com.finance.finance.service.FinVoucherRuleService;
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
 * 凭证映射规则接口（F8，docs 4.11）：/finance/voucher-rule，权限 finance:voucher-rule:*。
 *
 * <p>规则由财务经理维护；凭证生成时引擎按 source_type+event_type 匹配规则驱动分录。
 * 触发为系统内部（ExpenseService 打款接入引擎），非独立外部事件。</p>
 */
@RestController
@RequestMapping("/finance/voucher-rule")
public class VoucherRuleController {

    private final FinVoucherRuleService ruleService;

    public VoucherRuleController(FinVoucherRuleService ruleService) {
        this.ruleService = ruleService;
    }

    /** 分页查询规则。 */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('finance:voucher-rule:list')")
    public Result<PageResult<FinVoucherRule>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String enabled,
            @RequestParam(required = false) String keyword) {
        return Result.ok(ruleService.page(
                SecurityUtils.getCompanyCode(), sourceType, enabled, keyword, page, size));
    }

    /** 新增规则。 */
    @PostMapping
    @PreAuthorize("hasAuthority('finance:voucher-rule:add')")
    @OperLog(title = "映射规则新增", operType = OperType.INSERT)
    public Result<Void> add(@Valid @RequestBody FinVoucherRule rule) {
        ruleService.add(SecurityUtils.getCompanyCode(), rule);
        return Result.ok();
    }

    /** 修改规则（编码禁止修改）。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:voucher-rule:edit')")
    @OperLog(title = "映射规则修改", operType = OperType.UPDATE)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody FinVoucherRule rule) {
        ruleService.update(SecurityUtils.getCompanyCode(), id, rule);
        return Result.ok();
    }

    /** 启停用。 */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('finance:voucher-rule:edit')")
    @OperLog(title = "映射规则启停", operType = OperType.UPDATE)
    public Result<Void> switchStatus(@PathVariable Long id, @RequestParam String enabled) {
        ruleService.switchStatus(SecurityUtils.getCompanyCode(), id, enabled);
        return Result.ok();
    }

    /** 删除规则（逻辑删除）。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:voucher-rule:del')")
    @OperLog(title = "映射规则删除", operType = OperType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        ruleService.delete(SecurityUtils.getCompanyCode(), id);
        return Result.ok();
    }
}
