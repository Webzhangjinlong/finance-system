package com.finance.finance;

import com.finance.finance.domain.FinBalanceInitial;
import com.finance.finance.dto.BookRow;
import com.finance.finance.dto.VoucherDTO;
import com.finance.finance.domain.FinVoucher;
import com.finance.finance.domain.FinVoucherEntry;
import com.finance.finance.mapper.FinBalanceInitialMapper;
import com.finance.finance.service.BookService;
import com.finance.finance.service.VoucherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 账簿查询集成测试（F3，docs 4.3）：总账/明细账/日记账，真实 PG + 种子数据，事务回滚。
 *
 * <p>种子：记-0001（BOOKED，借 1002 银行存款 100000 / 贷 4001 实收资本 100000）。
 * 科目：1001 库存现金（DEBIT）/ 1002 银行存款（DEBIT）/ 2202 应付账款（CREDIT）/ 4001 实收资本（CREDIT）。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookServiceTest {

    @Autowired
    private BookService bookService;
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private FinBalanceInitialMapper balanceMapper;

    // ===== 总账 =====

    @Test
    void ledger_aggregatesPeriodMovements() {
        List<BookRow> rows = bookService.ledger("DEMO", 2026, 9, null);
        BookRow bank = row(rows, "1002");
        assertThat(bank.getPeriodDebit()).isEqualByComparingTo("100000.00");
        assertThat(bank.getPeriodCredit()).isEqualByComparingTo("0");
        // 无期初 → 期末 = 本期
        assertThat(bank.getEndingDebit()).isEqualByComparingTo("100000.00");
        assertThat(bank.getEndingCredit()).isEqualByComparingTo("0");

        BookRow equity = row(rows, "4001");
        assertThat(equity.getPeriodCredit()).isEqualByComparingTo("100000.00");
        assertThat(equity.getEndingCredit()).isEqualByComparingTo("100000.00");
    }

    @Test
    void ledger_withInitialBalance_includesInitialInEnding() {
        FinBalanceInitial initial = new FinBalanceInitial();
        initial.setCompanyCode("DEMO");
        initial.setPeriodYear(2026);
        initial.setPeriodMonth(9);
        initial.setSubjectId(2002L); // 1002 银行存款
        initial.setSubjectCode("1002");
        initial.setInitialDebit(new BigDecimal("50000.00"));
        initial.setInitialCredit(BigDecimal.ZERO);
        balanceMapper.insert(initial);

        List<BookRow> rows = bookService.ledger("DEMO", 2026, 9, null);
        BookRow bank = row(rows, "1002");
        assertThat(bank.getInitialDebit()).isEqualByComparingTo("50000.00");
        // 期末 = 期初 50000 + 本期借 100000
        assertThat(bank.getEndingDebit()).isEqualByComparingTo("150000.00");
    }

    @Test
    void ledger_filterBySubjectId() {
        List<BookRow> rows = bookService.ledger("DEMO", 2026, 9, 2002L);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getSubjectCode()).isEqualTo("1002");
    }

    @Test
    void ledger_includesNewlyBookedVoucher() {
        VoucherDTO dto = new VoucherDTO();
        dto.setPeriodYear(2026);
        dto.setPeriodMonth(9);
        dto.setVoucherDate(LocalDate.of(2026, 9, 15));
        dto.setRemark("新增过账");
        FinVoucherEntry d = new FinVoucherEntry();
        d.setSubjectCode("1001");
        d.setSummary("借现金");
        d.setDebitAmount(new BigDecimal("300.00"));
        d.setCreditAmount(BigDecimal.ZERO);
        FinVoucherEntry c = new FinVoucherEntry();
        c.setSubjectCode("2202");
        c.setSummary("贷应付");
        c.setDebitAmount(BigDecimal.ZERO);
        c.setCreditAmount(new BigDecimal("300.00"));
        dto.setEntries(List.of(d, c));
        FinVoucher v = voucherService.create("DEMO", dto);
        voucherService.audit("DEMO", v.getId());
        voucherService.book("DEMO", v.getId());

        List<BookRow> rows = bookService.ledger("DEMO", 2026, 9, null);
        BookRow cash = row(rows, "1001");
        assertThat(cash.getPeriodDebit()).isEqualByComparingTo("300.00");
        BookRow payable = row(rows, "2202");
        assertThat(payable.getPeriodCredit()).isEqualByComparingTo("300.00");
    }

    // ===== 明细账 =====

    @Test
    void detail_listsVoucherEntriesWithRunningBalance() {
        List<BookRow> rows = bookService.detail("DEMO", 2026, 9, 2002L);
        assertThat(rows).hasSize(1);
        BookRow row = rows.get(0);
        assertThat(row.getVoucherNo()).isEqualTo("记-0001");
        assertThat(row.getPeriodDebit()).isEqualByComparingTo("100000.00");
        assertThat(row.getEndingDebit()).isEqualByComparingTo("100000.00");
    }

    // ===== 日记账 =====

    @Test
    void journal_aggregatesByDay() {
        List<BookRow> rows = bookService.journal("DEMO", 2026, 9, 2002L);
        assertThat(rows).hasSize(1);
        BookRow row = rows.get(0);
        assertThat(row.getVoucherDate()).isEqualTo("2026-09-01");
        assertThat(row.getPeriodDebit()).isEqualByComparingTo("100000.00");
    }

    @Test
    void journal_allSubjects() {
        List<BookRow> rows = bookService.journal("DEMO", 2026, 9, null);
        assertThat(rows).hasSize(2); // 1002 与 4001 各一行
    }

    // ==================== 工具方法 ====================

    private BookRow row(List<BookRow> rows, String subjectCode) {
        return rows.stream().filter(r -> r.getSubjectCode().equals(subjectCode))
                .findFirst().orElseThrow(() -> new AssertionError("科目 " + subjectCode + " 不在账簿结果中"));
    }
}
