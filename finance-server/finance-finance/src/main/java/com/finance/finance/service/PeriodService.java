package com.finance.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finance.common.core.exception.BusinessException;
import com.finance.finance.domain.FinPeriod;
import com.finance.finance.domain.FinVoucher;
import com.finance.finance.domain.FinVoucherEntry;
import com.finance.finance.dto.BookRow;
import com.finance.finance.dto.VoucherDTO;
import com.finance.finance.mapper.BookQueryMapper;
import com.finance.finance.mapper.FinPeriodMapper;
import com.finance.finance.mapper.FinVoucherEntryMapper;
import com.finance.finance.mapper.FinVoucherMapper;
import com.finance.framework.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 期末结账服务（F4，docs 4.4）：结账 / 反结账 / 期间列表。
 *
 * <p>硬约束 6：结账前校验无未审核/未过账凭证且试算平衡；结账后期间只读（VoucherService 已按 OPEN 校验）。
 * 损益结转：自动生成"本年利润"结转凭证（source_type=PERIOD_CLOSE + 期间幂等，防重复结账）。</p>
 */
@Service
public class PeriodService {

    private static final Logger log = LoggerFactory.getLogger(PeriodService.class);

    /** 结账结转凭证来源标识（幂等：同一期间只能生成一次）。 */
    public static final String SOURCE_PERIOD_CLOSE = "PERIOD_CLOSE";

    /** 损益类科目类型（收入/费用）+ 成本，参与损益结转。 */
    private static final List<String> PNL_TYPES = List.of("PROFIT", "COST");

    private final FinPeriodMapper periodMapper;
    private final FinVoucherMapper voucherMapper;
    private final FinVoucherEntryMapper entryMapper;
    private final BookQueryMapper bookQueryMapper;
    private final VoucherService voucherService;

    public PeriodService(FinPeriodMapper periodMapper,
                         FinVoucherMapper voucherMapper,
                         FinVoucherEntryMapper entryMapper,
                         BookQueryMapper bookQueryMapper,
                         VoucherService voucherService) {
        this.periodMapper = periodMapper;
        this.voucherMapper = voucherMapper;
        this.entryMapper = entryMapper;
        this.bookQueryMapper = bookQueryMapper;
        this.voucherService = voucherService;
    }

    /** 期间列表（倒序）。 */
    public List<FinPeriod> list(String companyCode) {
        return periodMapper.selectList(new LambdaQueryWrapper<FinPeriod>()
                .eq(FinPeriod::getCompanyCode, companyCode)
                .orderByDesc(FinPeriod::getPeriodYear)
                .orderByDesc(FinPeriod::getPeriodMonth));
    }

    /**
     * 期末结账：校验（无未过账凭证 + 试算平衡）→ 损益结转（生成本年利润凭证并过账）→ 期间 CLOSED。
     */
    @Transactional
    public void close(String companyCode, int year, int month) {
        FinPeriod period = requirePeriod(companyCode, year, month);
        if (!"OPEN".equals(period.getPeriodStatus())) {
            throw new BusinessException("期间未开启（状态：" + period.getPeriodStatus() + "），无法结账");
        }
        // 1. 无未审核/未过账凭证
        Long pending = voucherMapper.selectCount(new LambdaQueryWrapper<FinVoucher>()
                .eq(FinVoucher::getCompanyCode, companyCode)
                .eq(FinVoucher::getPeriodYear, year)
                .eq(FinVoucher::getPeriodMonth, month)
                .in(FinVoucher::getVoucherStatus, "DRAFT", "AUDITED"));
        if (pending != null && pending > 0) {
            throw new BusinessException("期间存在未审核/未过账凭证（" + pending + " 张），禁止结账");
        }
        // 2. 试算平衡：全部科目净余额合计为 0（含期初）
        List<BookRow> ledger = bookQueryMapper.selectLedger(companyCode, year, month, null);
        assertTrialBalance(ledger);
        // 3. 损益结转（source 幂等）
        FinVoucher carry = buildCarryForward(companyCode, period, ledger);
        if (carry != null) {
            voucherService.audit(companyCode, carry.getId());
            voucherService.book(companyCode, carry.getId());
            log.info("期间 {}-{} 结账：生成并过账损益结转凭证 [{}]",
                    year, month, carry.getVoucherNo());
        }
        // 4. 期间 CLOSED
        FinPeriod update = new FinPeriod();
        update.setId(period.getId());
        update.setPeriodStatus("CLOSED");
        update.setClosedBy(SecurityUtils.getUsername());
        update.setClosedAt(OffsetDateTime.now());
        periodMapper.updateById(update);
        log.info("期间 {}-{} 已结账", year, month);
    }

    /**
     * 反结账：删除该期间损益结转凭证（PERIOD_CLOSE），期间恢复 OPEN。
     */
    @Transactional
    public void reopen(String companyCode, int year, int month) {
        FinPeriod period = requirePeriod(companyCode, year, month);
        if (!"CLOSED".equals(period.getPeriodStatus())) {
            throw new BusinessException("期间未结账（状态：" + period.getPeriodStatus() + "），无法反结账");
        }
        List<FinVoucher> closeVouchers = voucherMapper.selectList(new LambdaQueryWrapper<FinVoucher>()
                .eq(FinVoucher::getCompanyCode, companyCode)
                .eq(FinVoucher::getPeriodYear, year)
                .eq(FinVoucher::getPeriodMonth, month)
                .eq(FinVoucher::getSourceType, SOURCE_PERIOD_CLOSE));
        for (FinVoucher v : closeVouchers) {
            entryMapper.delete(new LambdaQueryWrapper<FinVoucherEntry>()
                    .eq(FinVoucherEntry::getVoucherId, v.getId()));
            voucherMapper.deleteById(v.getId());
        }
        FinPeriod update = new FinPeriod();
        update.setId(period.getId());
        update.setPeriodStatus("OPEN");
        update.setClosedBy(null);
        update.setClosedAt(null);
        periodMapper.updateById(update);
        log.info("期间 {}-{} 已反结账，删除结转凭证 {} 张", year, month, closeVouchers.size());
    }

    // ==================== 内部实现 ====================

    private FinPeriod requirePeriod(String companyCode, int year, int month) {
        FinPeriod period = periodMapper.selectOne(new LambdaQueryWrapper<FinPeriod>()
                .eq(FinPeriod::getCompanyCode, companyCode)
                .eq(FinPeriod::getPeriodYear, year)
                .eq(FinPeriod::getPeriodMonth, month)
                .last("LIMIT 1"));
        if (period == null) {
            throw new BusinessException("会计期间不存在：" + year + "-" + month);
        }
        return period;
    }

    /** 试算平衡：借方余额合计 == 贷方余额合计（含期初；净额正=借余、负=贷余）。 */
    private void assertTrialBalance(List<BookRow> ledger) {
        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        for (BookRow row : ledger) {
            BigDecimal initial = nz(row.getInitialDebit()).subtract(nz(row.getInitialCredit()));
            BigDecimal period = nz(row.getPeriodDebit()).subtract(nz(row.getPeriodCredit()));
            BigDecimal net = initial.add(period);
            if (net.signum() >= 0) {
                debitTotal = debitTotal.add(net);
            } else {
                creditTotal = creditTotal.add(net.negate());
            }
        }
        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new BusinessException("试算不平衡：借方余额合计 " + debitTotal
                    + " ≠ 贷方余额合计 " + creditTotal + "，请检查凭证/期初余额");
        }
    }

    /** 损益结转凭证（收入结转入本年利润贷方，费用结转入借方；无损益发生返回 null）。 */
    private FinVoucher buildCarryForward(String companyCode, FinPeriod period, List<BookRow> ledger) {
        List<FinVoucherEntry> entries = new ArrayList<>();
        for (BookRow row : ledger) {
            if (!PNL_TYPES.contains(row.getSubjectType())) {
                continue;
            }
            // 收入类（PROFIT + CREDIT 方向）：本期贷方发生 → 结转至本年利润（4103）贷方
            if ("PROFIT".equals(row.getSubjectType()) && "CREDIT".equals(row.getDirection())) {
                BigDecimal amount = nz(row.getPeriodCredit());
                if (amount.signum() > 0) {
                    entries.add(entry(row.getSubjectCode(), "结转收入：" + row.getSubjectName(), amount, null));
                    entries.add(entry("4103", "结转收入至本年利润", null, amount));
                }
            }
            // 费用/成本类（DEBIT 方向）：本期借方发生 → 结转至本年利润（4103）借方
            if ("DEBIT".equals(row.getDirection())) {
                BigDecimal amount = nz(row.getPeriodDebit());
                if (amount.signum() > 0) {
                    entries.add(entry("4103", "结转费用至本年利润", amount, null));
                    entries.add(entry(row.getSubjectCode(), "结转费用：" + row.getSubjectName(), null, amount));
                }
            }
        }
        if (entries.isEmpty()) {
            return null;
        }
        VoucherDTO dto = new VoucherDTO();
        dto.setPeriodYear(period.getPeriodYear());
        dto.setPeriodMonth(period.getPeriodMonth());
        dto.setVoucherDate(period.getEndDate());
        dto.setRemark("损益结转（结账自动生成）");
        dto.setSourceType(SOURCE_PERIOD_CLOSE);
        dto.setSourceId(period.getId());
        dto.setEntries(entries);
        return voucherService.create(companyCode, dto);
    }

    private FinVoucherEntry entry(String subjectCode, String summary, BigDecimal debit, BigDecimal credit) {
        FinVoucherEntry e = new FinVoucherEntry();
        e.setSubjectCode(subjectCode);
        e.setSummary(summary);
        e.setDebitAmount(debit == null ? BigDecimal.ZERO : debit);
        e.setCreditAmount(credit == null ? BigDecimal.ZERO : credit);
        return e;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
