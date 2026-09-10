package com.finance.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.finance.domain.FinPeriod;
import com.finance.finance.domain.FinSubject;
import com.finance.finance.domain.FinVoucher;
import com.finance.finance.domain.FinVoucherEntry;
import com.finance.finance.dto.VoucherDTO;
import com.finance.finance.enums.VoucherStatus;
import com.finance.finance.mapper.FinPeriodMapper;
import com.finance.finance.mapper.FinSubjectMapper;
import com.finance.finance.mapper.FinVoucherEntryMapper;
import com.finance.finance.mapper.FinVoucherMapper;
import com.finance.framework.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 凭证管理服务（F2，docs 4.2）：录入/审核/过账/冲销，状态机 DRAFT→AUDITED→BOOKED→REVERSED。
 *
 * <p>硬约束落地：3（借贷平衡+金额&gt;0+仅末级科目，服务校验 + DB CHECK 双兜底）、
 * 4（状态机）、5（凭证号 公司+期间+序列 唯一且保存即占号，DB 唯一约束 + 冲突重试防并发重号）、
 * 6（已结账期间只读）、7（业务单据生成凭证幂等，source 唯一索引兜底）、12（写操作事务）。</p>
 */
@Service
public class VoucherService {

    private static final Logger log = LoggerFactory.getLogger(VoucherService.class);

    /** 冲销来源标识（幂等 source_type）。 */
    public static final String SOURCE_REVERSE = "VOUCHER_REVERSE";

    private static final Pattern VOUCHER_NO_PATTERN = Pattern.compile("^记-(\\d+)$");

    private final FinVoucherMapper voucherMapper;
    private final FinVoucherEntryMapper entryMapper;
    private final FinSubjectMapper subjectMapper;
    private final FinPeriodMapper periodMapper;
    private final TransactionTemplate transactionTemplate;

    public VoucherService(FinVoucherMapper voucherMapper,
                          FinVoucherEntryMapper entryMapper,
                          FinSubjectMapper subjectMapper,
                          FinPeriodMapper periodMapper,
                          TransactionTemplate transactionTemplate) {
        this.voucherMapper = voucherMapper;
        this.entryMapper = entryMapper;
        this.subjectMapper = subjectMapper;
        this.periodMapper = periodMapper;
        this.transactionTemplate = transactionTemplate;
    }

    /** 录入凭证（DRAFT，保存即占号）。 */
    public FinVoucher create(String companyCode, VoucherDTO dto) {
        return doWithVoucherNoRetry(companyCode, dto, false);
    }

    /** 修改草稿凭证（保持原凭证号）。 */
    @Transactional
    public void update(String companyCode, Long id, VoucherDTO dto) {
        FinVoucher exist = getById(companyCode, id);
        if (exist == null) {
            throw new BusinessException("凭证不存在");
        }
        if (!VoucherStatus.DRAFT.name().equals(exist.getVoucherStatus())) {
            throw new BusinessException("仅草稿凭证可修改");
        }
        FinVoucher update = new FinVoucher();
        update.setId(id);
        update.setVoucherDate(dto.getVoucherDate());
        update.setRemark(dto.getRemark());
        update.setTotalDebit(validateAndSumDebit(companyCode, dto));
        update.setTotalCredit(validateAndSumCredit(companyCode, dto));
        voucherMapper.updateById(update);
        entryMapper.delete(new LambdaQueryWrapper<FinVoucherEntry>()
                .eq(FinVoucherEntry::getVoucherId, id));
        for (FinVoucherEntry entry : dto.getEntries()) {
            entry.setId(null);
            entry.setVoucherId(id);
            entry.setCompanyCode(companyCode);
            entryMapper.insert(entry);
        }
    }

    /** 审核：DRAFT → AUDITED。 */
    @Transactional
    public void audit(String companyCode, Long id) {
        FinVoucher exist = getById(companyCode, id);
        if (exist == null) {
            throw new BusinessException("凭证不存在");
        }
        if (!VoucherStatus.DRAFT.name().equals(exist.getVoucherStatus())) {
            throw new BusinessException("仅草稿凭证可审核（当前状态：" + exist.getVoucherStatus() + "）");
        }
        assertBalance(exist.getTotalDebit(), exist.getTotalCredit());
        int updated = voucherMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<FinVoucher>()
                        .eq(FinVoucher::getId, id)
                        .eq(FinVoucher::getVoucherStatus, VoucherStatus.DRAFT.name())
                        .set(FinVoucher::getVoucherStatus, VoucherStatus.AUDITED.name())
                        .set(FinVoucher::getAuditor, SecurityUtils.getUsername())
                        .set(FinVoucher::getAuditedAt, OffsetDateTime.now()));
        if (updated != 1) {
            throw new BusinessException("凭证状态已变化，请刷新后重试");
        }
    }

    /** 过账：AUDITED → BOOKED（期间必须 OPEN，硬约束 6）。 */
    @Transactional
    public void book(String companyCode, Long id) {
        FinVoucher exist = getById(companyCode, id);
        if (exist == null) {
            throw new BusinessException("凭证不存在");
        }
        if (!VoucherStatus.AUDITED.name().equals(exist.getVoucherStatus())) {
            throw new BusinessException("仅已审核凭证可过账（当前状态：" + exist.getVoucherStatus() + "）");
        }
        checkPeriodOpen(companyCode, exist.getPeriodYear(), exist.getPeriodMonth());
        int updated = voucherMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<FinVoucher>()
                        .eq(FinVoucher::getId, id)
                        .eq(FinVoucher::getVoucherStatus, VoucherStatus.AUDITED.name())
                        .set(FinVoucher::getVoucherStatus, VoucherStatus.BOOKED.name())
                        .set(FinVoucher::getBooker, SecurityUtils.getUsername())
                        .set(FinVoucher::getBookedAt, OffsetDateTime.now()));
        if (updated != 1) {
            throw new BusinessException("凭证状态已变化，请刷新后重试");
        }
    }

    /**
     * 冲销：BOOKED → REVERSED，并生成红字凭证（分录借贷互换、金额为正，
     * 满足 DB 金额 CHECK；source_type+source_id 幂等唯一索引防重复冲销，硬约束 7）。
     */
    @Transactional
    public void reverse(String companyCode, Long id) {
        FinVoucher exist = getById(companyCode, id);
        if (exist == null) {
            throw new BusinessException("凭证不存在");
        }
        if (!VoucherStatus.BOOKED.name().equals(exist.getVoucherStatus())) {
            throw new BusinessException("仅已过账凭证可冲销（当前状态：" + exist.getVoucherStatus() + "）");
        }
        List<FinVoucherEntry> entries = entryMapper.selectList(
                new LambdaQueryWrapper<FinVoucherEntry>()
                        .eq(FinVoucherEntry::getVoucherId, id));
        if (entries.isEmpty()) {
            throw new BusinessException("凭证分录为空，无法冲销");
        }

        VoucherDTO reverseDto = new VoucherDTO();
        reverseDto.setPeriodYear(exist.getPeriodYear());
        reverseDto.setPeriodMonth(exist.getPeriodMonth());
        reverseDto.setVoucherDate(exist.getVoucherDate());
        reverseDto.setRemark("冲销凭证：" + exist.getVoucherNo() + "（红字）");
        reverseDto.setSourceType(SOURCE_REVERSE);
        reverseDto.setSourceId(id);
        // 红冲：借贷互换，金额保持正数（满足 ck_fin_entry_amount）
        reverseDto.setEntries(entries.stream().map(e -> {
            FinVoucherEntry red = new FinVoucherEntry();
            red.setSubjectId(e.getSubjectId());
            red.setSubjectCode(e.getSubjectCode());
            red.setSummary("红字冲销：" + e.getSummary());
            red.setDebitAmount(e.getCreditAmount());
            red.setCreditAmount(e.getDebitAmount());
            return red;
        }).toList());

        FinVoucher redVoucher = doWithVoucherNoRetry(companyCode, reverseDto, false);

        int updated = voucherMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<FinVoucher>()
                        .eq(FinVoucher::getId, id)
                        .eq(FinVoucher::getVoucherStatus, VoucherStatus.BOOKED.name())
                        .set(FinVoucher::getVoucherStatus, VoucherStatus.REVERSED.name()));
        if (updated != 1) {
            throw new BusinessException("凭证状态已变化，请刷新后重试");
        }
        log.info("凭证 [{}] 已冲销，生成红字凭证 [{}]", exist.getVoucherNo(), redVoucher.getVoucherNo());
    }

    /** 删除草稿凭证。 */
    @Transactional
    public void delete(String companyCode, Long id) {
        FinVoucher exist = getById(companyCode, id);
        if (exist == null) {
            throw new BusinessException("凭证不存在");
        }
        if (!VoucherStatus.DRAFT.name().equals(exist.getVoucherStatus())) {
            throw new BusinessException("仅草稿凭证可删除");
        }
        entryMapper.delete(new LambdaQueryWrapper<FinVoucherEntry>()
                .eq(FinVoucherEntry::getVoucherId, id));
        voucherMapper.deleteById(id);
    }

    /** 分页查询（按公司隔离，支持期间/状态过滤）。 */
    public PageResult<FinVoucher> page(String companyCode, Integer periodYear, Integer periodMonth,
                                       String status, long page, long size) {
        Page<FinVoucher> p = voucherMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<FinVoucher>()
                        .eq(FinVoucher::getCompanyCode, companyCode)
                        .eq(periodYear != null, FinVoucher::getPeriodYear, periodYear)
                        .eq(periodMonth != null, FinVoucher::getPeriodMonth, periodMonth)
                        .eq(status != null, FinVoucher::getVoucherStatus, status)
                        .orderByDesc(FinVoucher::getVoucherDate)
                        .orderByDesc(FinVoucher::getVoucherNo));
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** 凭证详情（含分录）。 */
    public FinVoucher detail(String companyCode, Long id) {
        FinVoucher voucher = getById(companyCode, id);
        if (voucher == null) {
            throw new BusinessException("凭证不存在");
        }
        voucher.setEntries(entryMapper.selectList(new LambdaQueryWrapper<FinVoucherEntry>()
                .eq(FinVoucherEntry::getVoucherId, id)
                .orderByAsc(FinVoucherEntry::getId)));
        return voucher;
    }

    // ==================== 内部实现 ====================

    /** 凭证号冲突重试（最多 3 次），保证并发下不重号（硬约束 5）。 */
    private FinVoucher doWithVoucherNoRetry(String companyCode, VoucherDTO dto, boolean ignored) {
        int maxRetry = 3;
        DuplicateKeyException last = null;
        for (int i = 0; i < maxRetry; i++) {
            try {
                return transactionTemplate.execute(status -> doCreate(companyCode, dto));
            } catch (DuplicateKeyException e) {
                last = e;
                log.warn("凭证号冲突，重试 {}/{}", i + 1, maxRetry);
            }
        }
        throw new BusinessException("凭证号分配冲突，请重试", last);
    }

    private FinVoucher doCreate(String companyCode, VoucherDTO dto) {
        checkPeriodOpen(companyCode, dto.getPeriodYear(), dto.getPeriodMonth());
        if (dto.getSourceType() != null && dto.getSourceId() != null) {
            Long dup = voucherMapper.selectCount(new LambdaQueryWrapper<FinVoucher>()
                    .eq(FinVoucher::getSourceType, dto.getSourceType())
                    .eq(FinVoucher::getSourceId, dto.getSourceId()));
            if (dup != null && dup > 0) {
                throw new BusinessException("该业务单据已生成凭证，禁止重复生成（幂等约束）");
            }
        }
        BigDecimal totalDebit = validateAndSumDebit(companyCode, dto);
        BigDecimal totalCredit = validateAndSumCredit(companyCode, dto);
        assertBalance(totalDebit, totalCredit);

        FinVoucher voucher = new FinVoucher();
        voucher.setCompanyCode(companyCode);
        voucher.setPeriodYear(dto.getPeriodYear());
        voucher.setPeriodMonth(dto.getPeriodMonth());
        voucher.setVoucherNo(nextVoucherNo(companyCode, dto.getPeriodYear(), dto.getPeriodMonth()));
        voucher.setVoucherDate(dto.getVoucherDate());
        voucher.setVoucherStatus(VoucherStatus.DRAFT.name());
        voucher.setTotalDebit(totalDebit);
        voucher.setTotalCredit(totalCredit);
        voucher.setSourceType(dto.getSourceType());
        voucher.setSourceId(dto.getSourceId());
        voucher.setAttachCount(0);
        voucher.setRemark(dto.getRemark());
        try {
            voucherMapper.insert(voucher);
        } catch (DuplicateKeyException e) {
            throw e; // 交给重试层处理（凭证号唯一约束 / 幂等唯一索引）
        }
        for (FinVoucherEntry entry : dto.getEntries()) {
            entry.setId(null);
            entry.setCompanyCode(companyCode);
            entry.setVoucherId(voucher.getId());
            entryMapper.insert(entry);
        }
        return voucher;
    }

    /** 生成凭证号：记-YYYYMM-XXXX（月内序列），max+1，DB 唯一约束兜底。 */
    private String nextVoucherNo(String companyCode, int year, int month) {
        List<FinVoucher> latest = voucherMapper.selectList(new LambdaQueryWrapper<FinVoucher>()
                .eq(FinVoucher::getCompanyCode, companyCode)
                .eq(FinVoucher::getPeriodYear, year)
                .eq(FinVoucher::getPeriodMonth, month)
                .orderByDesc(FinVoucher::getVoucherNo)
                .last("LIMIT 1"));
        int seq = 1;
        if (!latest.isEmpty()) {
            Matcher m = VOUCHER_NO_PATTERN.matcher(latest.get(0).getVoucherNo());
            if (m.matches()) {
                seq = Integer.parseInt(m.group(1)) + 1;
            }
        }
        return String.format("记-%04d", seq);
    }

    private void checkPeriodOpen(String companyCode, int year, int month) {
        FinPeriod period = periodMapper.selectOne(new LambdaQueryWrapper<FinPeriod>()
                .eq(FinPeriod::getCompanyCode, companyCode)
                .eq(FinPeriod::getPeriodYear, year)
                .eq(FinPeriod::getPeriodMonth, month)
                .last("LIMIT 1"));
        if (period == null) {
            throw new BusinessException("会计期间不存在：" + year + "-" + month);
        }
        if (!"OPEN".equals(period.getPeriodStatus())) {
            throw new BusinessException("会计期间未开启（状态：" + period.getPeriodStatus() + "），禁止记账");
        }
    }

    private BigDecimal validateAndSumDebit(String companyCode, VoucherDTO dto) {
        if (dto.getEntries() == null || dto.getEntries().isEmpty()) {
            throw new BusinessException("凭证分录不能为空");
        }
        Map<String, FinSubject> subjects = subjectIndex(companyCode);
        BigDecimal sum = BigDecimal.ZERO;
        for (FinVoucherEntry e : dto.getEntries()) {
            FinSubject subject = subjects.get(e.getSubjectCode());
            if (subject == null) {
                throw new BusinessException("科目不存在或已停用：" + e.getSubjectCode());
            }
            if (subject.getIsLeaf() == null || subject.getIsLeaf() != 1) {
                throw new BusinessException("仅末级科目可记账：" + e.getSubjectCode());
            }
            e.setSubjectId(subject.getId());
            BigDecimal debit = e.getDebitAmount() == null ? BigDecimal.ZERO : e.getDebitAmount();
            BigDecimal credit = e.getCreditAmount() == null ? BigDecimal.ZERO : e.getCreditAmount();
            if (debit.compareTo(BigDecimal.ZERO) < 0 || credit.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("分录金额不能为负");
            }
            if (debit.signum() == 0 && credit.signum() == 0) {
                throw new BusinessException("分录借贷金额不能同时为空");
            }
            if (debit.signum() > 0 && credit.signum() > 0) {
                throw new BusinessException("分录只能记借方或贷方一方");
            }
            sum = sum.add(debit);
        }
        return sum;
    }

    private BigDecimal validateAndSumCredit(String companyCode, VoucherDTO dto) {
        Map<String, FinSubject> subjects = subjectIndex(companyCode);
        BigDecimal sum = BigDecimal.ZERO;
        for (FinVoucherEntry e : dto.getEntries()) {
            BigDecimal credit = e.getCreditAmount() == null ? BigDecimal.ZERO : e.getCreditAmount();
            sum = sum.add(credit);
        }
        return sum;
    }

    private void assertBalance(BigDecimal totalDebit, BigDecimal totalCredit) {
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessException("凭证借贷不平衡：借方 " + totalDebit + " ≠ 贷方 " + totalCredit);
        }
    }

    private Map<String, FinSubject> subjectIndex(String companyCode) {
        List<FinSubject> all = subjectMapper.selectList(new LambdaQueryWrapper<FinSubject>()
                .eq(FinSubject::getCompanyCode, companyCode)
                .eq(FinSubject::getStatus, "ACTIVE"));
        Map<String, FinSubject> map = new HashMap<>();
        for (FinSubject s : all) {
            map.put(s.getSubjectCode(), s);
        }
        return map;
    }

    private FinVoucher getById(String companyCode, Long id) {
        return voucherMapper.selectOne(new LambdaQueryWrapper<FinVoucher>()
                .eq(FinVoucher::getId, id)
                .eq(FinVoucher::getCompanyCode, companyCode)
                .last("LIMIT 1"));
    }
}
