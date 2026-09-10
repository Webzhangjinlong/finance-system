package com.finance.finance;

import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.finance.domain.FinExpenseClaim;
import com.finance.finance.domain.FinExpenseItem;
import com.finance.finance.domain.FinVoucher;
import com.finance.finance.domain.FinVoucherEntry;
import com.finance.finance.dto.ExpenseClaimDTO;
import com.finance.finance.dto.ExpenseItemDTO;
import com.finance.finance.dto.ExpenseSubmitDTO;
import com.finance.finance.dto.VoucherDTO;
import com.finance.finance.service.ExpenseService;
import com.finance.finance.service.VoucherService;
import com.finance.workflow.dto.WorkflowTaskVO;
import com.finance.workflow.service.WorkflowService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 费用报销集成测试（F6，Gate 13）：真实 Flowable 引擎 + 测试库。
 *
 * <p>覆盖：新建（单号/金额合计/明细落库）、末级科目校验、仅草稿可改删、
 * 提交审批（状态 + W1 流程发起）、审批通过/驳回回调、打款（APPROVED→PAID +
 * 凭证生成：借费用科目/贷银行存款 + source 幂等）、打款后禁改删。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class ExpenseServiceTest {

    @Autowired
    private ExpenseService expenseService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        // L37 教训：只清本域测试数据，保留 V2 种子凭证（source_type IS NULL）
        jdbcTemplate.execute("DELETE FROM fin_expense_item");
        jdbcTemplate.execute("DELETE FROM fin_expense_claim");
        jdbcTemplate.execute("DELETE FROM fin_voucher_entry WHERE voucher_id IN "
                + "(SELECT id FROM fin_voucher WHERE source_type = 'EXPENSE')");
        jdbcTemplate.execute("DELETE FROM fin_voucher WHERE source_type = 'EXPENSE'");
        jdbcTemplate.execute("DELETE FROM wf_process_instance WHERE business_type = 'EXPENSE'");
        jdbcTemplate.execute("DELETE FROM act_ru_identitylink");
        jdbcTemplate.execute("DELETE FROM act_ru_variable");
        jdbcTemplate.execute("DELETE FROM act_ru_task");
        jdbcTemplate.execute("DELETE FROM act_ru_execution");
        jdbcTemplate.execute("DELETE FROM sys_message WHERE content LIKE '%报销%'");
    }

    @AfterEach
    void cleanupSelfCreatedSubjects() {
        // 恢复被测试临时标记为非末级的科目
        jdbcTemplate.execute("UPDATE fin_subject SET is_leaf = 1 "
                + "WHERE company_code = 'DEMO' AND subject_code = '6601'");
    }

    // ===== F6 新建 =====

    @Test
    void create_generatesClaimNoAndItems() {
        FinExpenseClaim claim = createClaim("DEMO", "TRAVEL", "差旅报销", item("6601", "交通费", "800.00"),
                item("6601", "住宿费", "1200.00"));

        assertThat(claim.getId()).isNotNull();
        assertThat(claim.getClaimNo()).startsWith("BX-" + java.time.LocalDate.now().getYear() + "-");
        assertThat(claim.getStatus()).isEqualTo(FinExpenseClaim.STATUS_DRAFT);
        assertThat(claim.getAmount()).isEqualByComparingTo("2000.00");
        assertThat(claim.getCompanyCode()).isEqualTo("DEMO");

        FinExpenseClaim detail = expenseService.detail("DEMO", claim.getId());
        assertThat(detail.getItems()).hasSize(2);
        assertThat(detail.getItems().get(0).getSubjectCode()).isEqualTo("6601");
        assertThat(detail.getItems().get(0).getAmount()).isEqualByComparingTo("800.00");
    }

    @Test
    void create_itemSubjectNotLeaf_rejected() {
        // 临时把 6601 标记为非末级（@AfterEach 恢复）——父/非末级科目不可直接入账（硬约束 3）
        jdbcTemplate.execute("UPDATE fin_subject SET is_leaf = 0 "
                + "WHERE company_code = 'DEMO' AND subject_code = '6601'");

        ExpenseClaimDTO dto = claimDto("TRAVEL", "违规入账", item("6601", "非末级科目", "100.00"));
        assertThatThrownBy(() -> expenseService.create("DEMO", dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅末级科目");
    }

    @Test
    void create_itemSubjectNotExist_rejected() {
        ExpenseClaimDTO dto = claimDto("TRAVEL", "科目不存在", item("9999", "幽灵科目", "100.00"));
        assertThatThrownBy(() -> expenseService.create("DEMO", dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("科目不存在");
    }

    @Test
    void update_delete_onlyDraftAllowed() {
        FinExpenseClaim claim = createClaim("DEMO", "OFFICE", "办公用品", item("6601", "文具", "300.00"));

        // 仅草稿可改
        expenseService.update("DEMO", claim.getId(), claimDto("OFFICE", "办公用品(改)", item("6601", "文具", "500.00")));
        assertThat(expenseService.detail("DEMO", claim.getId()).getAmount()).isEqualByComparingTo("500.00");

        // 仅草稿可删
        expenseService.delete("DEMO", claim.getId());
        assertThatThrownBy(() -> expenseService.detail("DEMO", claim.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("报销单不存在");
    }

    @Test
    void update_afterPaid_rejected() {
        FinExpenseClaim claim = createClaim("DEMO", "MEAL", "团建餐费", item("6601", "餐费", "2000.00"));
        fullApproveAndPay(claim);

        assertThatThrownBy(() -> expenseService.update("DEMO", claim.getId(),
                claimDto("MEAL", "试图修改", item("6601", "餐费", "1.00"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅草稿");
    }

    // ===== F6 状态机 + W1 审批流 =====

    @Test
    void submitApprovePay_fullFlow_generatesVoucher() {
        FinExpenseClaim claim = createClaim("DEMO", "TRAVEL", "差旅报销", item("6601", "交通费", "1500.00"));

        // 提交 → SUBMITTED + 流程发起
        expenseService.submit("DEMO", claim.getId(), submitDto("wangwu", "请审批"));
        assertThat(expenseService.detail("DEMO", claim.getId()).getStatus())
                .isEqualTo(FinExpenseClaim.STATUS_SUBMITTED);
        List<WorkflowTaskVO> todo = workflowService.todo("wangwu");
        assertThat(todo).anyMatch(t -> "EXPENSE".equals(t.getBusinessType())
                && claim.getId().equals(t.getBusinessId()));

        // 审批通过 → APPROVED
        String taskId = todo.stream().filter(t -> claim.getId().equals(t.getBusinessId()))
                .findFirst().orElseThrow().getTaskId();
        expenseService.approveTask(taskId, "同意");
        assertThat(expenseService.detail("DEMO", claim.getId()).getStatus())
                .isEqualTo(FinExpenseClaim.STATUS_APPROVED);

        // 打款 → PAID + 凭证（借 6601 / 贷 1002，source 幂等键）
        expenseService.pay("DEMO", claim.getId());
        FinExpenseClaim paid = expenseService.detail("DEMO", claim.getId());
        assertThat(paid.getStatus()).isEqualTo(FinExpenseClaim.STATUS_PAID);
        assertThat(paid.getPaidAt()).isNotNull();

        FinVoucher voucher = findExpenseVoucher(claim.getId());
        assertThat(voucher).isNotNull();
        assertThat(voucher.getSourceType()).isEqualTo(FinExpenseClaim.SOURCE_TYPE);
        assertThat(voucher.getSourceId()).isEqualTo(claim.getId());
        assertThat(voucher.getTotalDebit()).isEqualByComparingTo("1500.00");
        assertThat(voucher.getTotalCredit()).isEqualByComparingTo("1500.00");
        List<FinVoucherEntry> entries = voucherService.detail("DEMO", voucher.getId()).getEntries();
        assertThat(entries).anyMatch(e -> "6601".equals(e.getSubjectCode())
                && e.getDebitAmount() != null && e.getDebitAmount().compareTo(new BigDecimal("1500.00")) == 0);
        assertThat(entries).anyMatch(e -> "1002".equals(e.getSubjectCode())
                && e.getCreditAmount() != null && e.getCreditAmount().compareTo(new BigDecimal("1500.00")) == 0);
    }

    @Test
    void pay_idempotent_noDuplicateVoucher() {
        FinExpenseClaim claim = createClaim("DEMO", "OFFICE", "办公耗材", item("6601", "耗材", "600.00"));
        fullApproveAndPay(claim);

        // 已打款重复打款 → 状态机拒绝
        assertThatThrownBy(() -> expenseService.pay("DEMO", claim.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅已通过");

        // 同 source 再生成凭证 → 幂等约束拒绝（硬约束 7）
        assertThatThrownBy(() -> voucherService.create("DEMO", expenseVoucher(claim)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("禁止重复生成");
        assertThat(findExpenseVoucher(claim.getId())).isNotNull();
    }

    @Test
    void reject_backToRejected_thenResubmit() {
        FinExpenseClaim claim = createClaim("DEMO", "MEAL", "餐费", item("6601", "餐费", "800.00"));
        expenseService.submit("DEMO", claim.getId(), submitDto("zhaoliu", "请审批"));

        String taskId = workflowService.todo("zhaoliu").stream()
                .filter(t -> claim.getId().equals(t.getBusinessId()))
                .findFirst().orElseThrow().getTaskId();
        expenseService.rejectTask(taskId, "发票不全");

        FinExpenseClaim rejected = expenseService.detail("DEMO", claim.getId());
        assertThat(rejected.getStatus()).isEqualTo(FinExpenseClaim.STATUS_REJECTED);

        // 驳回可重新提交 → SUBMITTED（新流程实例）
        expenseService.submit("DEMO", claim.getId(), submitDto("zhaoliu", "补充发票后重提"));
        assertThat(expenseService.detail("DEMO", claim.getId()).getStatus())
                .isEqualTo(FinExpenseClaim.STATUS_SUBMITTED);
    }

    @Test
    void submit_nonDraftNonRejected_rejected() {
        FinExpenseClaim claim = createClaim("DEMO", "OTHER", "其他", item("6601", "杂项", "100.00"));
        expenseService.submit("DEMO", claim.getId(), submitDto("wangwu", "请审批"));
        // 已提交重复提交 → 拒绝
        assertThatThrownBy(() -> expenseService.submit("DEMO", claim.getId(), submitDto("wangwu", "再提")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅草稿/已驳回");
    }

    @Test
    void page_filtersByStatusAndKeyword() {
        FinExpenseClaim a = createClaim("DEMO", "TRAVEL", "差旅报销A", item("6601", "交通", "100.00"));
        FinExpenseClaim b = createClaim("DEMO", "MEAL", "团建餐费B", item("6601", "餐费", "200.00"));
        expenseService.submit("DEMO", a.getId(), submitDto("wangwu", "请审批"));

        PageResult<FinExpenseClaim> submitted = expenseService.page("DEMO", null, "SUBMITTED", 1, 10);
        assertThat(submitted.getTotal()).isEqualTo(1);
        assertThat(submitted.getRecords().get(0).getId()).isEqualTo(a.getId());

        PageResult<FinExpenseClaim> byKeyword = expenseService.page("DEMO", "餐费B", null, 1, 10);
        assertThat(byKeyword.getTotal()).isEqualTo(1);
        assertThat(byKeyword.getRecords().get(0).getId()).isEqualTo(b.getId());
    }

    // ===== helpers =====

    private FinExpenseClaim createClaim(String companyCode, String type, String reason,
                                        ExpenseItemDTO... items) {
        ExpenseClaimDTO dto = claimDto(type, reason, items);
        return expenseService.create(companyCode, dto);
    }

    private ExpenseClaimDTO claimDto(String type, String reason, ExpenseItemDTO... items) {
        ExpenseClaimDTO dto = new ExpenseClaimDTO();
        dto.setExpenseType(type);
        dto.setReason(reason);
        dto.setInvoiceNo("INV-2026-001");
        dto.setItems(new ArrayList<>(List.of(items)));
        return dto;
    }

    private ExpenseItemDTO item(String subjectCode, String summary, String amount) {
        ExpenseItemDTO dto = new ExpenseItemDTO();
        dto.setSubjectCode(subjectCode);
        dto.setSummary(summary);
        dto.setAmount(new BigDecimal(amount));
        return dto;
    }

    private ExpenseSubmitDTO submitDto(String approver, String comment) {
        ExpenseSubmitDTO dto = new ExpenseSubmitDTO();
        dto.setApprover(approver);
        dto.setComment(comment);
        return dto;
    }

    /** 快捷全流程：提交 → 审批通过 → 打款。 */
    private void fullApproveAndPay(FinExpenseClaim claim) {
        expenseService.submit("DEMO", claim.getId(), submitDto("wangwu", "请审批"));
        String taskId = workflowService.todo("wangwu").stream()
                .filter(t -> claim.getId().equals(t.getBusinessId()))
                .findFirst().orElseThrow().getTaskId();
        expenseService.approveTask(taskId, "同意");
        expenseService.pay("DEMO", claim.getId());
    }

    private FinVoucher findExpenseVoucher(Long claimId) {
        return jdbcTemplate.query("SELECT * FROM fin_voucher WHERE source_type = 'EXPENSE' "
                        + "AND source_id = ?", (rs, i) -> {
                    FinVoucher v = new FinVoucher();
                    v.setId(rs.getLong("id"));
                    v.setCompanyCode(rs.getString("company_code"));
                    v.setPeriodYear(rs.getInt("period_year"));
                    v.setPeriodMonth(rs.getInt("period_month"));
                    v.setVoucherNo(rs.getString("voucher_no"));
                    v.setVoucherStatus(rs.getString("voucher_status"));
                    v.setTotalDebit(rs.getBigDecimal("total_debit"));
                    v.setTotalCredit(rs.getBigDecimal("total_credit"));
                    v.setSourceType(rs.getString("source_type"));
                    v.setSourceId(rs.getLong("source_id"));
                    return v;
                }, claimId).stream().findFirst().orElse(null);
    }

    private VoucherDTO expenseVoucher(FinExpenseClaim claim) {
        List<FinExpenseItem> items = expenseService.detail("DEMO", claim.getId()).getItems();
        VoucherDTO dto = new VoucherDTO();
        dto.setPeriodYear(java.time.LocalDate.now().getYear());
        dto.setPeriodMonth(java.time.LocalDate.now().getMonthValue());
        dto.setVoucherDate(java.time.LocalDate.now());
        dto.setRemark("费用报销 " + claim.getClaimNo());
        dto.setSourceType(FinExpenseClaim.SOURCE_TYPE);
        dto.setSourceId(claim.getId());
        List<FinVoucherEntry> entries = new ArrayList<>();
        for (FinExpenseItem item : items) {
            FinVoucherEntry e = new FinVoucherEntry();
            e.setSubjectCode(item.getSubjectCode());
            e.setSummary(item.getSummary());
            e.setDebitAmount(item.getAmount());
            entries.add(e);
        }
        FinVoucherEntry credit = new FinVoucherEntry();
        credit.setSubjectCode("1002");
        credit.setSummary("报销打款");
        credit.setCreditAmount(claim.getAmount());
        entries.add(credit);
        dto.setEntries(entries);
        return dto;
    }
}
