package com.finance.finance.controller;

import com.finance.common.core.Result;
import com.finance.common.core.domain.PageResult;
import com.finance.finance.domain.vo.ReportRow;
import com.finance.finance.domain.vo.TrialBalanceVO;
import com.finance.finance.service.AnalysisService;
import com.finance.framework.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 财务分析接口（F9，backlog 新增）：/finance/analysis，权限 finance:analysis:list。
 *
 * <p>科目余额表（试算平衡）period 必传、费用月度趋势（当年 1-12 月按科目汇总）。</p>
 */
@RestController
@RequestMapping("/finance/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /** 科目余额表（试算平衡）：期初/本期/期末 借贷双栏 + 合计 + 平衡标志。 */
    @GetMapping("/trial-balance")
    @PreAuthorize("hasAuthority('finance:analysis:list')")
    public Result<TrialBalanceVO> trialBalance(@RequestParam int periodYear,
                                               @RequestParam int periodMonth) {
        return Result.ok(analysisService.trialBalance(
                SecurityUtils.getCompanyCode(), periodYear, periodMonth));
    }

    /** 费用月度趋势：费用科目（PROFIT+DEBIT）按 科目×月 借方汇总（当年 1-12 月）。 */
    @GetMapping("/expense-trend")
    @PreAuthorize("hasAuthority('finance:analysis:list')")
    public Result<List<ReportRow>> expenseTrend(@RequestParam int periodYear) {
        return Result.ok(analysisService.expenseTrend(SecurityUtils.getCompanyCode(), periodYear));
    }
}
