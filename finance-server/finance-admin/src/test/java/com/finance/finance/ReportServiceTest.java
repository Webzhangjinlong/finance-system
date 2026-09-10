package com.finance.finance;

import com.finance.finance.domain.vo.BalanceSheetVO;
import com.finance.finance.domain.vo.CashFlowVO;
import com.finance.finance.domain.vo.IncomeStatementVO;
import com.finance.finance.dto.VoucherDTO;
import com.finance.finance.domain.FinVoucherEntry;
import com.finance.finance.service.ReportService;
import com.finance.finance.service.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 财务报表集成测试（F5，docs 4.5）：资产负债表平衡、利润表收入费用、
 * 现金流量表简化口径、期间过滤。
 *
 * <p>复用 V2 种子科目（1002 银行存款/2202 应付账款/4001 实收资本/6001 主营业务收入/6601 销售费用），
 * 测试前清空凭证使报表从零累计。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class ReportServiceTest {

    @Autowired
    private ReportService reportService;
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        // 只清理非种子凭证（保留 V2 种子凭证 id=1001/1002，VoucherServiceTest 依赖）
        jdbcTemplate.execute("DELETE FROM fin_voucher_entry WHERE voucher_id NOT IN (1001, 1002)");
        jdbcTemplate.execute("DELETE FROM fin_voucher WHERE id NOT IN (1001, 1002)");
    }

    @org.junit.jupiter.api.AfterEach
    void cleanAfter() {
        clean();
    }

    private void bookVoucher(String remark, List<FinVoucherEntry> entries) {
        VoucherDTO dto = new VoucherDTO();
        dto.setPeriodYear(2026);
        dto.setPeriodMonth(9);
        dto.setVoucherDate(LocalDate.of(2026, 9, 10));
        dto.setRemark(remark);
        dto.setEntries(entries);
        Long id = voucherService.create("DEMO", dto).getId();
        voucherService.audit("DEMO", id);
        voucherService.book("DEMO", id);
    }

    private FinVoucherEntry entry(String code, String summary, String debit, String credit) {
        FinVoucherEntry e = new FinVoucherEntry();
        e.setSubjectCode(code);
        e.setSummary(summary);
        e.setDebitAmount(debit == null ? null : new BigDecimal(debit));
        e.setCreditAmount(credit == null ? null : new BigDecimal(credit));
        return e;
    }

    @Test
    void balanceSheet_assetEqualsEquityAndProfit() {
        bookVoucher("销售收入", List.of(
                entry("1002", "收到货款", "100000.00", null),
                entry("6001", "确认收入", null, "100000.00")));

        BalanceSheetVO vo = reportService.balanceSheet("DEMO", null, null);

        // 种子凭证（借 1002 10万 / 贷 4001 10万，EQUITY）叠加后仍平衡
        assertThat(vo.isBalanced()).isTrue();
        assertThat(vo.getTotalAssets()).isEqualByComparingTo("200000.00");
        assertThat(vo.getNetProfit()).isEqualByComparingTo("100000.00");
        assertThat(vo.getTotalLiabEquity()).isEqualByComparingTo("200000.00");
        assertThat(vo.getItems()).anySatisfy(i -> {
            assertThat(i.getSubjectCode()).isEqualTo("1002");
            assertThat(i.getSection()).isEqualTo("ASSET");
        });
        assertThat(vo.getItems()).anySatisfy(i -> {
            assertThat(i.getSubjectCode()).isEqualTo("6001");
            assertThat(i.getSection()).isEqualTo("PROFIT");
        });
    }

    @Test
    void balanceSheet_liabilityIncluded() {
        bookVoucher("赊购", List.of(
                entry("1002", "收到货款", "50000.00", null),
                entry("6001", "确认收入", null, "50000.00")));
        bookVoucher("采购未付", List.of(
                entry("2202", "应付货款", null, "30000.00"),
                entry("6601", "采购成本", "30000.00", null)));

        BalanceSheetVO vo = reportService.balanceSheet("DEMO", null, null);

        // assets = 种子 10万 + 新增 5万；liab = 3万；equity = 种子 10万；净利 = 5万-3万 = 2万
        assertThat(vo.isBalanced()).isTrue();
        assertThat(vo.getTotalAssets()).isEqualByComparingTo("150000.00");
        assertThat(vo.getTotalLiabilities()).isEqualByComparingTo("30000.00");
        assertThat(vo.getTotalEquity()).isEqualByComparingTo("100000.00");
        assertThat(vo.getNetProfit()).isEqualByComparingTo("20000.00");
    }

    @Test
    void incomeStatement_revenueMinusExpense() {
        bookVoucher("销售收入", List.of(
                entry("1002", "收到货款", "80000.00", null),
                entry("6001", "确认收入", null, "80000.00")));
        bookVoucher("销售费用", List.of(
                entry("6601", "广告费", "5000.00", null),
                entry("1002", "支付广告费", null, "5000.00")));

        IncomeStatementVO vo = reportService.incomeStatement("DEMO", 2026, 9);

        assertThat(vo.getTotalRevenue()).isEqualByComparingTo("80000.00");
        assertThat(vo.getTotalExpense()).isEqualByComparingTo("5000.00");
        assertThat(vo.getNetProfit()).isEqualByComparingTo("75000.00");
        assertThat(vo.getRevenues()).anySatisfy(l -> assertThat(l.getSubjectCode()).isEqualTo("6001"));
        assertThat(vo.getExpenses()).anySatisfy(l -> assertThat(l.getSubjectCode()).isEqualTo("6601"));
    }

    @Test
    void incomeStatement_periodFilterExcludesOtherPeriod() {
        bookVoucher("本期收入", List.of(
                entry("1002", "收款", "30000.00", null),
                entry("6001", "收入", null, "30000.00")));

        // 查其他期间 → 空
        IncomeStatementVO vo = reportService.incomeStatement("DEMO", 2026, 8);
        assertThat(vo.getTotalRevenue()).isEqualByComparingTo("0");
        assertThat(vo.getNetProfit()).isEqualByComparingTo("0");
    }

    @Test
    void cashFlow_simplifiedByCashSubjects() {
        bookVoucher("收款", List.of(
                entry("1002", "收到货款", "120000.00", null),
                entry("6001", "确认收入", null, "120000.00")));
        bookVoucher("付款", List.of(
                entry("6601", "费用", "20000.00", null),
                entry("1002", "支付", null, "20000.00")));

        CashFlowVO vo = reportService.cashFlow("DEMO", 2026, 9);

        // 种子凭证 1002 借方 10万 叠加后：inflow = 10万 + 12万
        assertThat(vo.getInflow()).isEqualByComparingTo("220000.00");
        assertThat(vo.getOutflow()).isEqualByComparingTo("20000.00");
        assertThat(vo.getNetCash()).isEqualByComparingTo("200000.00");
        assertThat(vo.getItems()).hasSize(1); // 仅 1002 银行存款
        assertThat(vo.getNote()).contains("一期简化");
    }
}
