package com.finance.finance.controller;

import com.finance.common.core.Result;
import com.finance.finance.domain.vo.BalanceSheetVO;
import com.finance.finance.domain.vo.CashFlowVO;
import com.finance.finance.domain.vo.IncomeStatementVO;
import com.finance.finance.service.ReportService;
import com.finance.framework.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 财务报表接口（F5，docs 4.5）：/finance/report 资产负债表/利润表/现金流量表。
 */
@RestController
@RequestMapping("/finance/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** 资产负债表：可选期间过滤（不传=全量累计），校验 资产 == 负债 + 权益。 */
    @GetMapping("/balance-sheet")
    @PreAuthorize("hasAuthority('finance:report:list')")
    public Result<BalanceSheetVO> balanceSheet(@RequestParam(required = false) Integer periodYear,
                                               @RequestParam(required = false) Integer periodMonth) {
        return Result.ok(reportService.balanceSheet(SecurityUtils.getCompanyCode(), periodYear, periodMonth));
    }

    /** 利润表：按期间取损益类科目发生额。 */
    @GetMapping("/income")
    @PreAuthorize("hasAuthority('finance:report:list')")
    public Result<IncomeStatementVO> income(@RequestParam int periodYear,
                                            @RequestParam int periodMonth) {
        return Result.ok(reportService.incomeStatement(SecurityUtils.getCompanyCode(), periodYear, periodMonth));
    }

    /** 现金流量表（一期简化）：货币资金科目期间收付汇总。 */
    @GetMapping("/cash-flow")
    @PreAuthorize("hasAuthority('finance:report:list')")
    public Result<CashFlowVO> cashFlow(@RequestParam int periodYear,
                                       @RequestParam int periodMonth) {
        return Result.ok(reportService.cashFlow(SecurityUtils.getCompanyCode(), periodYear, periodMonth));
    }
}
