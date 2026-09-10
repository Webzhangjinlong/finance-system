package com.finance.finance.controller;

import com.finance.common.core.Result;
import com.finance.finance.domain.FinPeriod;
import com.finance.finance.service.PeriodService;
import com.finance.framework.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 期末结账接口（F4，docs 4.4）：/finance/period，权限 finance:period:*。
 */
@RestController
@RequestMapping("/finance/period")
public class PeriodController {

    private final PeriodService periodService;

    public PeriodController(PeriodService periodService) {
        this.periodService = periodService;
    }

    /** 期间列表。 */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('finance:period:list')")
    public Result<List<FinPeriod>> list() {
        return Result.ok(periodService.list(SecurityUtils.getCompanyCode()));
    }

    /** 期末结账（校验 + 损益结转 + CLOSED）。 */
    @PutMapping("/close")
    @PreAuthorize("hasAuthority('finance:period:close')")
    public Result<Void> close(@RequestParam int periodYear, @RequestParam int periodMonth) {
        periodService.close(SecurityUtils.getCompanyCode(), periodYear, periodMonth);
        return Result.ok();
    }

    /** 反结账（删除结转凭证 + 恢复 OPEN）。 */
    @PutMapping("/reopen")
    @PreAuthorize("hasAuthority('finance:period:reopen')")
    public Result<Void> reopen(@RequestParam int periodYear, @RequestParam int periodMonth) {
        periodService.reopen(SecurityUtils.getCompanyCode(), periodYear, periodMonth);
        return Result.ok();
    }
}
