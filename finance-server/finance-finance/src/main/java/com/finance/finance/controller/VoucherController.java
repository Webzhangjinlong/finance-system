package com.finance.finance.controller;

import com.finance.common.core.Result;
import com.finance.common.core.domain.PageResult;
import com.finance.finance.domain.FinVoucher;
import com.finance.finance.dto.VoucherDTO;
import com.finance.finance.service.VoucherService;
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
 * 凭证管理接口（F2，docs 4.2）：/finance/voucher，权限 finance:voucher:*。
 */
@RestController
@RequestMapping("/finance/voucher")
public class VoucherController {

    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    /** 分页查询凭证。 */
    @GetMapping
    @PreAuthorize("hasAuthority('finance:voucher:list')")
    public Result<PageResult<FinVoucher>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer periodYear,
            @RequestParam(required = false) Integer periodMonth,
            @RequestParam(required = false) String status) {
        return Result.ok(voucherService.page(
                SecurityUtils.getCompanyCode(), periodYear, periodMonth, status, page, size));
    }

    /** 凭证详情（含分录）。 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:voucher:list')")
    public Result<FinVoucher> detail(@PathVariable Long id) {
        return Result.ok(voucherService.detail(SecurityUtils.getCompanyCode(), id));
    }

    /** 录入凭证（保存即占号）。 */
    @PostMapping
    @PreAuthorize("hasAuthority('finance:voucher:add')")
    public Result<FinVoucher> create(@Valid @RequestBody VoucherDTO dto) {
        return Result.ok("凭证已保存", voucherService.create(SecurityUtils.getCompanyCode(), dto));
    }

    /** 修改草稿凭证。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:voucher:edit')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody VoucherDTO dto) {
        voucherService.update(SecurityUtils.getCompanyCode(), id, dto);
        return Result.ok();
    }

    /** 审核（DRAFT → AUDITED）。 */
    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('finance:voucher:audit')")
    public Result<Void> audit(@PathVariable Long id) {
        voucherService.audit(SecurityUtils.getCompanyCode(), id);
        return Result.ok();
    }

    /** 过账（AUDITED → BOOKED，期间必须 OPEN）。 */
    @PutMapping("/{id}/book")
    @PreAuthorize("hasAuthority('finance:voucher:book')")
    public Result<Void> book(@PathVariable Long id) {
        voucherService.book(SecurityUtils.getCompanyCode(), id);
        return Result.ok();
    }

    /** 冲销（BOOKED → REVERSED，生成红字凭证，幂等）。 */
    @PutMapping("/{id}/reverse")
    @PreAuthorize("hasAuthority('finance:voucher:reverse')")
    public Result<Void> reverse(@PathVariable Long id) {
        voucherService.reverse(SecurityUtils.getCompanyCode(), id);
        return Result.ok();
    }

    /** 删除草稿凭证。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:voucher:del')")
    public Result<Void> delete(@PathVariable Long id) {
        voucherService.delete(SecurityUtils.getCompanyCode(), id);
        return Result.ok();
    }
}
