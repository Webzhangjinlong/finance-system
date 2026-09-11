package com.finance.finance;

import com.finance.finance.domain.FinVoucherEntry;
import com.finance.finance.domain.vo.ReportRow;
import com.finance.finance.domain.vo.TrialBalanceRow;
import com.finance.finance.domain.vo.TrialBalanceVO;
import com.finance.finance.dto.VoucherDTO;
import com.finance.finance.service.AnalysisService;
import com.finance.finance.service.VoucherService;
import org.junit.jupiter.api.AfterEach;
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
 * 财务分析测试（F9）：科目余额表（试算平衡：期初/本期/期末 + 平衡断言 + 无发生科目）
 * + 费用月度趋势（科目×月借方汇总）。
 *
 * <p>依赖 V2 种子凭证 1001（2026-09：借 1002 10万 / 贷 4001 10万）。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class AnalysisServiceTest {

    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        // 只清理非种子凭证（保留 V2 种子 1001，VoucherServiceTest 依赖）
        jdbcTemplate.execute("DELETE FROM fin_voucher_entry WHERE voucher_id NOT IN (1001)");
        jdbcTemplate.execute("DELETE FROM fin_voucher WHERE id NOT IN (1001)");
        // 补齐 2026-08 期间（test 库仅种子 2026-09），供"上期发生"场景使用
        jdbcTemplate.execute("INSERT INTO fin_period (id, company_code, period_year, period_month, period_status, start_date, end_date, create_by) "
                + "SELECT 1002, 'DEMO', 2026, 8, 'OPEN', '2026-08-01', '2026-08-31', 'seed' "
                + "WHERE NOT EXISTS (SELECT 1 FROM fin_period WHERE company_code = 'DEMO' AND period_year = 2026 AND period_month = 8)");
    }

    @AfterEach
    void cleanAfter() {
        clean();
    }

    private void bookVoucher(int year, int month, String remark, List<FinVoucherEntry> entries) {
        VoucherDTO dto = new VoucherDTO();
        dto.setPeriodYear(year);
        dto.setPeriodMonth(month);
        dto.setVoucherDate(LocalDate.of(year, month, 10));
        dto.setRemark(remark);
        dto.setEntries(entries);
        Long id = voucherService.create("DEMO", dto).getId();
        voucherService.audit("DEMO", id);
        voucherService.book("DEMO", id);
    }

    private FinVoucherEntry entry(String code, String debit, String credit) {
        FinVoucherEntry e = new FinVoucherEntry();
        e.setSubjectCode(code);
        e.setSummary("测试");
        e.setDebitAmount(debit == null ? null : new BigDecimal(debit));
        e.setCreditAmount(credit == null ? null : new BigDecimal(credit));
        return e;
    }

    @Test
    void trialBalance_currentPeriodCalculatesAndBalances() {
        // 本期新增：借 1002 5万 / 贷 6001 5万（种子 1001 同月：借 1002 10万 / 贷 4001 10万）
        bookVoucher(2026, 9, "销售", List.of(
                entry("1002", "50000.00", null),
                entry("6001", null, "50000.00")));

        TrialBalanceVO vo = analysisService.trialBalance("DEMO", 2026, 9);

        assertThat(vo.getBalanced()).isTrue();
        // 期初无数据（种子也在本期）
        assertThat(vo.getOpeningDebitTotal()).isEqualByComparingTo("0");
        assertThat(vo.getOpeningCreditTotal()).isEqualByComparingTo("0");
        // 本期借合计 = 1002 10万+5万 = 15万；贷合计 = 4001 10万 + 6001 5万 = 15万
        assertThat(vo.getPeriodDebitTotal()).isEqualByComparingTo("150000.00");
        assertThat(vo.getPeriodCreditTotal()).isEqualByComparingTo("150000.00");
        assertThat(vo.getClosingDebitTotal()).isEqualByComparingTo("150000.00");
        assertThat(vo.getClosingCreditTotal()).isEqualByComparingTo("150000.00");

        TrialBalanceRow asset = row(vo, "1002");
        assertThat(asset.getClosingDebit()).isEqualByComparingTo("150000.00");
        assertThat(asset.getClosingCredit()).isEqualByComparingTo("0");
        TrialBalanceRow revenue = row(vo, "6001");
        assertThat(revenue.getClosingCredit()).isEqualByComparingTo("50000.00");
    }

    @Test
    void trialBalance_openingExcludesCurrentPeriod() {
        // 2026-08 期初凭证：借 1002 3万 / 贷 4001 3万（8月结账前）
        bookVoucher(2026, 8, "借款", List.of(
                entry("1002", "30000.00", null),
                entry("4001", null, "30000.00")));

        TrialBalanceVO vo = analysisService.trialBalance("DEMO", 2026, 9);

        // 期初：1002 借 3万 / 4001 贷 3万；本期：种子 1002 10万 / 4001 10万
        assertThat(vo.getOpeningDebitTotal()).isEqualByComparingTo("30000.00");
        assertThat(vo.getOpeningCreditTotal()).isEqualByComparingTo("30000.00");
        assertThat(vo.getPeriodDebitTotal()).isEqualByComparingTo("100000.00");
        assertThat(vo.getPeriodCreditTotal()).isEqualByComparingTo("100000.00");
        assertThat(vo.getClosingDebitTotal()).isEqualByComparingTo("130000.00");
        assertThat(vo.getClosingCreditTotal()).isEqualByComparingTo("130000.00");
        assertThat(vo.getBalanced()).isTrue();
    }

    @Test
    void trialBalance_inactiveSubjectsShownWithZero() {
        TrialBalanceVO vo = analysisService.trialBalance("DEMO", 2026, 9);
        // 1601 固定资产无任何发生 → 行存在且 active=false
        TrialBalanceRow fixedAsset = row(vo, "1601");
        assertThat(fixedAsset).isNotNull();
        assertThat(fixedAsset.getActive()).isFalse();
        assertThat(fixedAsset.getClosingDebit()).isEqualByComparingTo("0");
    }

    @Test
    void trialBalance_creditDirectionSubjectClosesOnCredit() {
        // 4001 实收资本（EQUITY/CREDIT）种子 10万 → 期末贷方 10万
        TrialBalanceVO vo = analysisService.trialBalance("DEMO", 2026, 9);
        TrialBalanceRow equity = row(vo, "4001");
        assertThat(equity.getClosingCredit()).isEqualByComparingTo("100000.00");
        assertThat(equity.getClosingDebit()).isEqualByComparingTo("0");
    }

    @Test
    void expenseTrend_groupsBySubjectAndMonth() {
        bookVoucher(2026, 8, "采购成本", List.of(
                entry("6601", "20000.00", null),
                entry("1002", null, "20000.00")));
        bookVoucher(2026, 9, "销售费用", List.of(
                entry("6601", "30000.00", null),
                entry("1002", null, "30000.00")));

        List<ReportRow> trend = analysisService.expenseTrend("DEMO", 2026);

        // 6601 两个月各一行：8月 2万、9月 3万
        assertThat(trend).filteredOn(r -> "6601".equals(r.getSubjectCode())).hasSize(2);
        assertThat(trend).anySatisfy(r -> {
            assertThat(r.getSubjectCode()).isEqualTo("6601");
            assertThat(r.getPeriodMonth()).isEqualTo(8);
            assertThat(r.getPeriodDebit()).isEqualByComparingTo("20000.00");
        });
        assertThat(trend).anySatisfy(r -> {
            assertThat(r.getPeriodMonth()).isEqualTo(9);
            assertThat(r.getPeriodDebit()).isEqualByComparingTo("30000.00");
        });
        // 收入科目 6001（PROFIT/CREDIT）不应出现在费用趋势
        assertThat(trend).noneMatch(r -> "6001".equals(r.getSubjectCode()));
    }

    private TrialBalanceRow row(TrialBalanceVO vo, String code) {
        return vo.getRows().stream().filter(r -> code.equals(r.getSubjectCode())).findFirst().orElse(null);
    }
}
