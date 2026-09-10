package com.finance.finance.service;

import com.finance.finance.domain.vo.BalanceSheetVO;
import com.finance.finance.domain.vo.CashFlowVO;
import com.finance.finance.domain.vo.IncomeStatementVO;
import com.finance.finance.domain.vo.ReportRow;
import com.finance.finance.mapper.ReportQueryMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 财务报表服务（F5，docs 4.5）：资产负债表 / 利润表 / 现金流量表。
 *
 * <p>取数口径：已过账凭证（BOOKED）按科目汇总；资产负债表 = 资产 == 负债 + 权益（含本期净利润）；
 * 利润表 = 损益类科目本期发生额（收入贷方 - 费用借方）；现金流量表一期简化
 * （按货币资金科目收付汇总，未细分经营/投资/筹资）。</p>
 */
@Service
public class ReportService {

    private static final String TYPE_ASSET = "ASSET";
    private static final String TYPE_LIABILITY = "LIABILITY";
    private static final String TYPE_EQUITY = "EQUITY";
    private static final String TYPE_PROFIT = "PROFIT";
    private static final String DIR_CREDIT = "CREDIT";

    /** 货币资金科目编码前缀（1001 库存现金 / 1002 银行存款 / 1012 其他货币资金）。 */
    private static final String[] CASH_PREFIX = {"1001", "1002", "1012"};

    private final ReportQueryMapper reportQueryMapper;

    public ReportService(ReportQueryMapper reportQueryMapper) {
        this.reportQueryMapper = reportQueryMapper;
    }

    /** 资产负债表：科目余额分资产/负债/权益，损益净额计入未分配利润，校验平衡。 */
    public BalanceSheetVO balanceSheet(String companyCode, Integer periodYear, Integer periodMonth) {
        List<ReportRow> rows = reportQueryMapper.selectReportRows(companyCode, periodYear, periodMonth);
        BalanceSheetVO vo = new BalanceSheetVO();
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalEquity = BigDecimal.ZERO;
        BigDecimal netProfit = BigDecimal.ZERO;

        for (ReportRow row : rows) {
            BigDecimal balance = switch (row.getSubjectType()) {
                case TYPE_ASSET -> row.getPeriodDebit().subtract(row.getPeriodCredit());
                case TYPE_LIABILITY, TYPE_EQUITY -> row.getPeriodCredit().subtract(row.getPeriodDebit());
                case TYPE_PROFIT -> row.getPeriodCredit().subtract(row.getPeriodDebit());
                default -> BigDecimal.ZERO;
            };
            if (balance.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            String section = switch (row.getSubjectType()) {
                case TYPE_ASSET -> "ASSET";
                case TYPE_LIABILITY -> "LIABILITY";
                case TYPE_EQUITY -> "EQUITY";
                case TYPE_PROFIT -> "PROFIT";
                default -> "OTHER";
            };
            BalanceSheetVO.Item item = new BalanceSheetVO.Item(row.getSubjectCode(),
                    row.getSubjectName(), section, balance);
            vo.getItems().add(item);
            switch (row.getSubjectType()) {
                case TYPE_ASSET -> totalAssets = totalAssets.add(balance);
                case TYPE_LIABILITY -> totalLiabilities = totalLiabilities.add(balance);
                case TYPE_EQUITY -> totalEquity = totalEquity.add(balance);
                case TYPE_PROFIT -> netProfit = netProfit.add(balance);
                default -> { }
            }
        }
        vo.setTotalAssets(totalAssets);
        vo.setTotalLiabilities(totalLiabilities);
        vo.setTotalEquity(totalEquity);
        vo.setNetProfit(netProfit);
        vo.setTotalLiabEquity(totalLiabilities.add(totalEquity).add(netProfit));
        vo.setBalanced(totalAssets.compareTo(vo.getTotalLiabEquity()) == 0);
        return vo;
    }

    /** 利润表：损益类科目本期发生额，收入（贷方）− 费用（借方）= 净利润。 */
    public IncomeStatementVO incomeStatement(String companyCode, int periodYear, int periodMonth) {
        List<ReportRow> rows = reportQueryMapper.selectReportRows(companyCode, periodYear, periodMonth);
        IncomeStatementVO vo = new IncomeStatementVO();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        for (ReportRow row : rows) {
            if (!TYPE_PROFIT.equals(row.getSubjectType())) {
                continue;
            }
            if (DIR_CREDIT.equalsIgnoreCase(row.getDirection())) {
                totalRevenue = totalRevenue.add(row.getPeriodCredit());
                vo.getRevenues().add(new IncomeStatementVO.Line(
                        row.getSubjectCode(), row.getSubjectName(), row.getPeriodCredit()));
            } else {
                totalExpense = totalExpense.add(row.getPeriodDebit());
                vo.getExpenses().add(new IncomeStatementVO.Line(
                        row.getSubjectCode(), row.getSubjectName(), row.getPeriodDebit()));
            }
        }
        vo.setTotalRevenue(totalRevenue);
        vo.setTotalExpense(totalExpense);
        vo.setNetProfit(totalRevenue.subtract(totalExpense));
        return vo;
    }

    /** 现金流量表（一期简化）：货币资金类科目期间收付汇总。 */
    public CashFlowVO cashFlow(String companyCode, int periodYear, int periodMonth) {
        List<ReportRow> rows = reportQueryMapper.selectReportRows(companyCode, periodYear, periodMonth);
        CashFlowVO vo = new CashFlowVO();
        BigDecimal inflow = BigDecimal.ZERO;
        BigDecimal outflow = BigDecimal.ZERO;
        for (ReportRow row : rows) {
            if (!TYPE_ASSET.equals(row.getSubjectType()) || !isCashSubject(row.getSubjectCode())) {
                continue;
            }
            CashFlowVO.Item item = new CashFlowVO.Item(row.getSubjectCode(), row.getSubjectName(),
                    row.getPeriodDebit(), row.getPeriodCredit());
            vo.getItems().add(item);
            inflow = inflow.add(row.getPeriodDebit());
            outflow = outflow.add(row.getPeriodCredit());
        }
        vo.setInflow(inflow);
        vo.setOutflow(outflow);
        vo.setNetCash(inflow.subtract(outflow));
        vo.setNote("一期简化口径：按货币资金科目（1001 库存现金/1002 银行存款/1012 其他货币资金）收付汇总，" +
                "未细分经营活动/投资活动/筹资活动。");
        return vo;
    }

    private boolean isCashSubject(String subjectCode) {
        if (subjectCode == null) {
            return false;
        }
        String code = subjectCode.trim();
        for (String prefix : CASH_PREFIX) {
            if (code.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
