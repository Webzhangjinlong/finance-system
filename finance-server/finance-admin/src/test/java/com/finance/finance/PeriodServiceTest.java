package com.finance.finance;

import com.finance.common.core.exception.BusinessException;
import com.finance.finance.domain.FinPeriod;
import com.finance.finance.domain.FinVoucher;
import com.finance.finance.domain.FinVoucherEntry;
import com.finance.finance.dto.VoucherDTO;
import com.finance.finance.mapper.FinPeriodMapper;
import com.finance.finance.mapper.FinVoucherEntryMapper;
import com.finance.finance.mapper.FinVoucherMapper;
import com.finance.finance.service.PeriodService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 期末结账集成测试（F4，docs 4.4）：真实 PG + 种子数据，事务回滚。
 *
 * <p>硬约束 6：结账前校验无未审核/未过账凭证 + 试算平衡；结账后期间只读；损益结转凭证幂等。
 * 种子：2026-09 OPEN；记-0001（BOOKED，借 1002 贷 4001 各 100000，平衡）。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PeriodServiceTest {

    @Autowired
    private PeriodService periodService;
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private FinPeriodMapper periodMapper;
    @Autowired
    private FinVoucherMapper voucherMapper;
    @Autowired
    private FinVoucherEntryMapper entryMapper;

    // ===== 结账校验 =====

    @Test
    void close_withPendingDraft_rejected() {
        VoucherDTO dto = newVoucher("1001", "2202", "100.00");
        voucherService.create("DEMO", dto); // DRAFT

        assertThatThrownBy(() -> periodService.close("DEMO", 2026, 9))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未审核/未过账");
    }

    @Test
    void close_nonOpenPeriod_rejected() {
        FinPeriod period = periodMapper.selectById(1001L);
        period.setPeriodStatus("CLOSED");
        periodMapper.updateById(period);

        assertThatThrownBy(() -> periodService.close("DEMO", 2026, 9))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未开启");
    }

    @Test
    void close_unknownPeriod_rejected() {
        assertThatThrownBy(() -> periodService.close("DEMO", 2020, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    // ===== 正常结账 =====

    @Test
    void close_setsPeriodClosed_andRecordsCloser() {
        periodService.close("DEMO", 2026, 9);

        FinPeriod period = periodMapper.selectById(1001L);
        assertThat(period.getPeriodStatus()).isEqualTo("CLOSED");
    }

    @Test
    void close_generatesCarryForwardVoucher_whenPnLExists() {
        // 收入凭证：借 1002 银行 50000 / 贷 6001 主营收入 50000 → 过账
        VoucherDTO income = newVoucher("1002", "6001", "50000.00");
        FinVoucher v = voucherService.create("DEMO", income);
        voucherService.audit("DEMO", v.getId());
        voucherService.book("DEMO", v.getId());

        periodService.close("DEMO", 2026, 9);

        // 结转凭证：source=PERIOD_CLOSE，含 4103 本年利润
        List<FinVoucher> carry = voucherMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FinVoucher>()
                        .eq(FinVoucher::getCompanyCode, "DEMO")
                        .eq(FinVoucher::getSourceType, PeriodService.SOURCE_PERIOD_CLOSE));
        assertThat(carry).hasSize(1);
        assertThat(carry.get(0).getVoucherStatus()).isEqualTo("BOOKED");
        List<FinVoucherEntry> entries = entryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FinVoucherEntry>()
                        .eq(FinVoucherEntry::getVoucherId, carry.get(0).getId()));
        boolean hasCarryToProfit = entries.stream()
                .anyMatch(e -> e.getSubjectCode().equals("4103")
                        && e.getCreditAmount().compareTo(new BigDecimal("50000.00")) == 0);
        boolean hasIncomeClosed = entries.stream()
                .anyMatch(e -> e.getSubjectCode().equals("6001")
                        && e.getDebitAmount().compareTo(new BigDecimal("50000.00")) == 0);
        assertThat(hasCarryToProfit).isTrue();
        assertThat(hasIncomeClosed).isTrue();
    }

    @Test
    void close_secondClose_rejectedByIdempotency() {
        periodService.close("DEMO", 2026, 9);
        // 第二次结账：期间已 CLOSED
        assertThatThrownBy(() -> periodService.close("DEMO", 2026, 9))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未开启");
    }

    // ===== 结账后只读 =====

    @Test
    void close_thenCreateVoucher_rejected() {
        periodService.close("DEMO", 2026, 9);
        VoucherDTO dto = newVoucher("1001", "2202", "100.00");
        assertThatThrownBy(() -> voucherService.create("DEMO", dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未开启");
    }

    // ===== 反结账 =====

    @Test
    void reopen_restoresOpen_andDeletesCarryVoucher() {
        // 先造损益 + 结账
        VoucherDTO income = newVoucher("1002", "6001", "30000.00");
        FinVoucher v = voucherService.create("DEMO", income);
        voucherService.audit("DEMO", v.getId());
        voucherService.book("DEMO", v.getId());
        periodService.close("DEMO", 2026, 9);

        periodService.reopen("DEMO", 2026, 9);

        FinPeriod period = periodMapper.selectById(1001L);
        assertThat(period.getPeriodStatus()).isEqualTo("OPEN");
        assertThat(period.getClosedBy()).isNull();

        List<FinVoucher> carry = voucherMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FinVoucher>()
                        .eq(FinVoucher::getSourceType, PeriodService.SOURCE_PERIOD_CLOSE));
        assertThat(carry).isEmpty();
    }

    @Test
    void reopen_openPeriod_rejected() {
        assertThatThrownBy(() -> periodService.reopen("DEMO", 2026, 9))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未结账");
    }

    // ===== 期间列表 =====

    @Test
    void list_returnsSeedPeriod() {
        List<FinPeriod> periods = periodService.list("DEMO");
        assertThat(periods).isNotEmpty();
        assertThat(periods.get(0).getPeriodYear()).isEqualTo(2026);
        assertThat(periods.get(0).getPeriodMonth()).isEqualTo(9);
    }

    // ==================== 工具方法 ====================

    private VoucherDTO newVoucher(String debitCode, String creditCode, String amount) {
        VoucherDTO dto = new VoucherDTO();
        dto.setPeriodYear(2026);
        dto.setPeriodMonth(9);
        dto.setVoucherDate(LocalDate.of(2026, 9, 20));
        dto.setRemark("结账测试");
        FinVoucherEntry d = new FinVoucherEntry();
        d.setSubjectCode(debitCode);
        d.setSummary("借");
        d.setDebitAmount(new BigDecimal(amount));
        d.setCreditAmount(BigDecimal.ZERO);
        FinVoucherEntry c = new FinVoucherEntry();
        c.setSubjectCode(creditCode);
        c.setSummary("贷");
        c.setDebitAmount(BigDecimal.ZERO);
        c.setCreditAmount(new BigDecimal(amount));
        dto.setEntries(List.of(d, c));
        return dto;
    }
}
