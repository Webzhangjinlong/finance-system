package com.finance.finance.controller;

import com.finance.common.core.Result;
import com.finance.common.core.domain.PageResult;
import com.finance.finance.domain.FinAp;
import com.finance.finance.domain.FinAr;
import com.finance.finance.service.ArApService;
import com.finance.framework.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 应收/应付查询接口（C3，docs 5.3）：/finance。
 *
 * <p>分页查询支持公司隔离与状态/往来方关键字过滤；
 * 生成与核销由 contract 模块计划联动编排（走 ArApService）。</p>
 */
@RestController
@RequestMapping("/finance")
public class FinArApController {

    private final ArApService arApService;

    public FinArApController(ArApService arApService) {
        this.arApService = arApService;
    }

    /** 应收分页（finance:receivable:list）。 */
    @GetMapping("/ar/page")
    @PreAuthorize("hasAuthority('finance:receivable:list')")
    public Result<PageResult<FinAr>> arPage(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(arApService.pageAr(SecurityUtils.getCompanyCode(), status, keyword, page, size));
    }

    /** 应付分页（finance:receivable:list）。 */
    @GetMapping("/ap/page")
    @PreAuthorize("hasAuthority('finance:receivable:list')")
    public Result<PageResult<FinAp>> apPage(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(arApService.pageAp(SecurityUtils.getCompanyCode(), status, keyword, page, size));
    }
}
