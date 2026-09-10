package com.finance.finance;

import com.finance.common.core.exception.BusinessException;
import com.finance.finance.domain.FinPeriod;
import com.finance.finance.domain.FinSubject;
import com.finance.finance.domain.FinVoucher;
import com.finance.finance.domain.FinVoucherEntry;
import com.finance.finance.dto.VoucherDTO;
import com.finance.finance.mapper.FinPeriodMapper;
import com.finance.finance.mapper.FinSubjectMapper;
import com.finance.finance.mapper.FinVoucherEntryMapper;
import com.finance.finance.mapper.FinVoucherMapper;
import com.finance.finance.service.SubjectService;
import com.finance.finance.service.VoucherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 凭证管理集成测试（F2，真实 PG 测试库 + Flyway 种子数据，事务回滚）。
 *
 * <p>验证硬约束 3/4/5/6/7：借贷平衡（服务校验 + DB CHECK）、状态机流转、
 * 凭证号唯一且保存即占号、已结账期间只读、业务生成凭证幂等。</p>
 * <p>种子：DEMO 公司 2026-09 OPEN 期间；凭证 记-0001（BOOKED，借 1002 贷 4001 各 100000）。
 * 科目编码：1001 库存现金 / 1002 银行存款 / 2202 应付账款 / 4001 实收资本。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VoucherServiceTest {

    @Autowired
    private VoucherService voucherService;
    @Autowired
    private SubjectService subjectService;
    @Autowired
    private FinVoucherMapper voucherMapper;
    @Autowired
    private FinVoucherEntryMapper entryMapper;
    @Autowired
    private FinSubjectMapper subjectMapper;
    @Autowired
    private FinPeriodMapper periodMapper;

    // ===== 硬约束 3：借贷平衡 / 金额 / 末级科目 =====

    @Test
    void createUnbalanced_rejected() {
        VoucherDTO dto = newVoucher(
                entry("1001", "借现金", "100.00", null),
                entry("2202", "贷应付", null, "50.00"));
        assertThatThrownBy(() -> voucherService.create("DEMO", dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("借贷不平衡");
    }

    @Test
    void createEmptyEntry_rejected() {
        VoucherDTO dto = newVoucher(
                entry("1001", "空行", null, null));
        assertThatThrownBy(() -> voucherService.create("DEMO", dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("同时为空");
    }

    @Test
    void createEntryBothSides_rejected() {
        VoucherDTO dto = newVoucher(
                entry("1001", "双方向", "100.00", "100.00"),
                entry("2202", "贷应付", null, "100.00"));
        assertThatThrownBy(() -> voucherService.create("DEMO", dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("借方或贷方一方");
    }

    @Test
    void createNonLeafSubject_rejected() {
        // 先加二级科目使 1002 变非末级
        FinSubject child = new FinSubject();
        child.setSubjectCode("100201");
        child.setSubjectName("银行存款-工行");
        child.setSubjectType("ASSET");
        child.setDirection("DEBIT");
        child.setParentId(2002L);
        subjectService.add("DEMO", child);

        VoucherDTO dto = newVoucher(
                entry("1002", "非末级科目记账", "100.00", null),
                entry("2202", "贷应付", null, "100.00"));
        assertThatThrownBy(() -> voucherService.create("DEMO", dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅末级科目");
    }

    @Test
    void createUnknownSubject_rejected() {
        VoucherDTO dto = newVoucher(
                entry("9999", "不存在科目", "100.00", null),
                entry("2202", "贷应付", null, "100.00"));
        assertThatThrownBy(() -> voucherService.create("DEMO", dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在或已停用");
    }

    // ===== 硬约束 6：已结账期间只读 =====

    @Test
    void createInClosedPeriod_rejected() {
        FinPeriod period = periodMapper.selectById(1001L);
        period.setPeriodStatus("CLOSED");
        periodMapper.updateById(period);

        VoucherDTO dto = newVoucher(
                entry("1001", "借现金", "100.00", null),
                entry("2202", "贷应付", null, "100.00"));
        assertThatThrownBy(() -> voucherService.create("DEMO", dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未开启");
    }

    // ===== 硬约束 5：凭证号唯一、保存即占号 =====

    @Test
    void create_allocatesSequentialVoucherNo() {
        VoucherDTO dto = newVoucher(
                entry("1001", "借现金", "100.00", null),
                entry("2202", "贷应付", null, "100.00"));
        FinVoucher v1 = voucherService.create("DEMO", dto);
        FinVoucher v2 = voucherService.create("DEMO", dto);

        assertThat(v1.getVoucherNo()).isEqualTo("记-0002");
        assertThat(v2.getVoucherNo()).isEqualTo("记-0003");
        assertThat(v1.getVoucherStatus()).isEqualTo("DRAFT");
        assertThat(v1.getTotalDebit()).isEqualByComparingTo("100.00");
        assertThat(v1.getTotalCredit()).isEqualByComparingTo("100.00");
    }

    @Test
    void duplicateVoucherNo_rejectedByDbUniqueConstraint() {
        // 绕过服务直接插入两条同号凭证 → DB 唯一约束（uq_fin_voucher_no）必须拒绝
        FinVoucher v1 = newVoucherEntity("记-9999");
        FinVoucher v2 = newVoucherEntity("记-9999");
        voucherMapper.insert(v1);
        assertThatThrownBy(() -> voucherMapper.insert(v2))
                .isInstanceOf(DuplicateKeyException.class);
    }

    // ===== 硬约束 4：状态机流转 =====

    @Test
    void fullLifecycle_auditBookReverse() {
        VoucherDTO dto = newVoucher(
                entry("1001", "借现金", "200.00", null),
                entry("2202", "贷应付", null, "200.00"));
        FinVoucher v = voucherService.create("DEMO", dto);

        voucherService.audit("DEMO", v.getId());
        assertThat(voucherMapper.selectById(v.getId()).getVoucherStatus()).isEqualTo("AUDITED");

        voucherService.book("DEMO", v.getId());
        assertThat(voucherMapper.selectById(v.getId()).getVoucherStatus()).isEqualTo("BOOKED");

        voucherService.reverse("DEMO", v.getId());
        FinVoucher reversed = voucherMapper.selectById(v.getId());
        assertThat(reversed.getVoucherStatus()).isEqualTo("REVERSED");

        // 红字凭证：借贷互换、金额为正
        List<FinVoucher> redList = voucherMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FinVoucher>()
                        .eq(FinVoucher::getSourceType, VoucherService.SOURCE_REVERSE)
                        .eq(FinVoucher::getSourceId, v.getId()));
        assertThat(redList).hasSize(1);
        FinVoucher red = redList.get(0);
        assertThat(red.getVoucherStatus()).isEqualTo("DRAFT");
        List<FinVoucherEntry> redEntries = entryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FinVoucherEntry>()
                        .eq(FinVoucherEntry::getVoucherId, red.getId()));
        assertThat(redEntries).hasSize(2);
        FinVoucherEntry redDebit = redEntries.stream()
                .filter(e -> e.getSubjectCode().equals("2202")).findFirst().orElseThrow();
        assertThat(redDebit.getDebitAmount()).isEqualByComparingTo("200.00");
        assertThat(redDebit.getCreditAmount()).isEqualByComparingTo("0");
    }

    @Test
    void auditNonDraft_rejected() {
        // 种子凭证 1001 已 BOOKED
        assertThatThrownBy(() -> voucherService.audit("DEMO", 1001L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅草稿凭证可审核");
    }

    @Test
    void bookNonAudited_rejected() {
        VoucherDTO dto = newVoucher(
                entry("1001", "借现金", "100.00", null),
                entry("2202", "贷应付", null, "100.00"));
        FinVoucher v = voucherService.create("DEMO", dto);
        assertThatThrownBy(() -> voucherService.book("DEMO", v.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅已审核凭证可过账");
    }

    @Test
    void reverseNonBooked_rejected() {
        VoucherDTO dto = newVoucher(
                entry("1001", "借现金", "100.00", null),
                entry("2202", "贷应付", null, "100.00"));
        FinVoucher v = voucherService.create("DEMO", dto);
        assertThatThrownBy(() -> voucherService.reverse("DEMO", v.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅已过账凭证可冲销");
    }

    // ===== 硬约束 7：业务单据生成凭证幂等 =====

    @Test
    void createWithSameSource_rejectedIdempotent() {
        VoucherDTO dto = newVoucher(
                entry("1001", "借现金", "100.00", null),
                entry("2202", "贷应付", null, "100.00"));
        dto.setSourceType("REIMBURSE");
        dto.setSourceId(88888L);
        voucherService.create("DEMO", dto);

        VoucherDTO again = newVoucher(
                entry("1001", "借现金", "100.00", null),
                entry("2202", "贷应付", null, "100.00"));
        again.setSourceType("REIMBURSE");
        again.setSourceId(88888L);
        assertThatThrownBy(() -> voucherService.create("DEMO", again))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已生成凭证");
    }

    // ===== 查询/删除 =====

    @Test
    void deleteDraft_succeeds_butBookedForbidden() {
        VoucherDTO dto = newVoucher(
                entry("1001", "借现金", "100.00", null),
                entry("2202", "贷应付", null, "100.00"));
        FinVoucher v = voucherService.create("DEMO", dto);
        voucherService.delete("DEMO", v.getId());
        assertThat(voucherMapper.selectById(v.getId())).isNull();

        assertThatThrownBy(() -> voucherService.delete("DEMO", 1001L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅草稿凭证可删除");
    }

    @Test
    void detail_containsEntries() {
        FinVoucher detail = voucherService.detail("DEMO", 1001L);
        assertThat(detail.getEntries()).hasSize(2);
        assertThat(detail.getTotalDebit()).isEqualByComparingTo("100000.00");
    }

    // ==================== 工具方法 ====================

    private VoucherDTO newVoucher(FinVoucherEntry... entries) {
        VoucherDTO dto = new VoucherDTO();
        dto.setPeriodYear(2026);
        dto.setPeriodMonth(9);
        dto.setVoucherDate(LocalDate.of(2026, 9, 10));
        dto.setRemark("测试凭证");
        dto.setEntries(List.of(entries));
        return dto;
    }

    private FinVoucherEntry entry(String subjectCode, String summary, String debit, String credit) {
        FinVoucherEntry e = new FinVoucherEntry();
        e.setSubjectCode(subjectCode);
        e.setSummary(summary);
        e.setDebitAmount(debit == null ? null : new BigDecimal(debit));
        e.setCreditAmount(credit == null ? null : new BigDecimal(credit));
        return e;
    }

    private FinVoucher newVoucherEntity(String voucherNo) {
        FinVoucher v = new FinVoucher();
        v.setCompanyCode("DEMO");
        v.setPeriodYear(2026);
        v.setPeriodMonth(9);
        v.setVoucherNo(voucherNo);
        v.setVoucherDate(LocalDate.of(2026, 9, 10));
        v.setVoucherStatus("DRAFT");
        v.setTotalDebit(BigDecimal.ZERO);
        v.setTotalCredit(BigDecimal.ZERO);
        v.setAttachCount(0);
        return v;
    }
}
