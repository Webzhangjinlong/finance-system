package com.finance.contract;

import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.contract.domain.CtrContract;
import com.finance.contract.domain.CtrPaymentPlan;
import com.finance.contract.dto.ContractDTO;
import com.finance.contract.dto.ContractSubmitDTO;
import com.finance.contract.service.ContractPlanService;
import com.finance.contract.service.ContractService;
import com.finance.finance.domain.FinAp;
import com.finance.finance.domain.FinAr;
import com.finance.finance.service.ArApService;
import com.finance.workflow.dto.WorkflowTaskVO;
import com.finance.workflow.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 收付款计划联动集成测试（C3，docs 5.3）：计划到期 → 应收/应付幂等生成 + 收付款核销回写。
 *
 * <p>覆盖：RECEIPT→AR / PAYMENT→AP 生成、同步幂等（不重复）、未来计划跳过、
 * 全额/部分核销回写计划与单据状态、超额核销拦截（服务校验）、方向错误拦截、
 * 应收/应付分页过滤。OVERDUE 由到期提醒任务（Gate 10）标记，核销路径不置。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class ContractPlanServiceTest {

    private static final AtomicLong SEQ = new AtomicLong(20000);

    @Autowired
    private ContractService contractService;
    @Autowired
    private ContractPlanService contractPlanService;
    @Autowired
    private ArApService arApService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        // 单据与计划物理删除（fin 表在 ctr 之前，无外键但保持逻辑顺序）
        jdbcTemplate.execute("DELETE FROM fin_ar");
        jdbcTemplate.execute("DELETE FROM fin_ap");
        jdbcTemplate.execute("DELETE FROM ctr_payment_plan");
        jdbcTemplate.execute("DELETE FROM ctr_contract");
        jdbcTemplate.execute("DELETE FROM wf_process_instance");
        jdbcTemplate.execute("DELETE FROM act_ru_identitylink");
        jdbcTemplate.execute("DELETE FROM act_ru_variable");
        jdbcTemplate.execute("DELETE FROM act_ru_task");
        jdbcTemplate.execute("DELETE FROM act_ru_execution");
    }

    // ===== 到期同步生成（幂等） =====

    @Test
    void sync_generatesArForReceiptPlan() {
        CtrContract c = approveFlow("DEMO", "SALES", "80000.00");
        CtrPaymentPlan plan = singlePlan(c.getId());
        assertThat(plan.getPlanType()).isEqualTo(CtrPaymentPlan.PLAN_TYPE_RECEIPT);

        int generated = contractPlanService.syncDuePlans("DEMO", c.getId());
        assertThat(generated).isEqualTo(1);

        PageResult<FinAr> ars = arApService.pageAr("DEMO", null, null, 1, 10);
        assertThat(ars.getTotal()).isEqualTo(1);
        FinAr ar = ars.getRecords().get(0);
        assertThat(ar.getPlanId()).isEqualTo(plan.getId());
        assertThat(ar.getArNo()).startsWith("AR-" + LocalDate.now().getYear());
        assertThat(ar.getAmount()).isEqualByComparingTo("80000.00");
        assertThat(ar.getReceivedAmount()).isEqualByComparingTo("0");
        assertThat(ar.getStatus()).isEqualTo(FinAr.STATUS_OPEN);
        assertThat(ar.getDueDate()).isEqualTo(plan.getPlanDate());
    }

    @Test
    void sync_generatesApForPaymentPlan() {
        CtrContract c = approveFlow("DEMO", "PURCHASE", "30000.00");
        assertThat(singlePlan(c.getId()).getPlanType()).isEqualTo(CtrPaymentPlan.PLAN_TYPE_PAYMENT);

        contractPlanService.syncDuePlans("DEMO", c.getId());

        PageResult<FinAp> aps = arApService.pageAp("DEMO", null, null, 1, 10);
        assertThat(aps.getTotal()).isEqualTo(1);
        FinAp ap = aps.getRecords().get(0);
        assertThat(ap.getApNo()).startsWith("AP-" + LocalDate.now().getYear());
        assertThat(ap.getStatus()).isEqualTo(FinAp.STATUS_OPEN);
    }

    @Test
    void sync_idempotent_noDuplicateAr() {
        CtrContract c = approveFlow("DEMO", "SALES", "50000.00");
        contractPlanService.syncDuePlans("DEMO", c.getId());
        // 重复同步不重复生成（计数 0 + company+plan_id 唯一约束兜底）
        int second = contractPlanService.syncDuePlans("DEMO", c.getId());
        assertThat(second).isEqualTo(0);
        assertThat(arApService.pageAr("DEMO", null, null, 1, 10).getTotal()).isEqualTo(1);
    }

    @Test
    void sync_skipsFuturePlan() {
        CtrContract c = approveFlowWithStartDate("DEMO", "SALES", "90000.00", "2030-01-01");
        int generated = contractPlanService.syncDuePlans("DEMO", c.getId());
        assertThat(generated).isEqualTo(0);
        assertThat(arApService.pageAr("DEMO", null, null, 1, 10).getTotal()).isZero();
    }

    // ===== 收付款核销回写 =====

    @Test
    void registerReceipt_full_settlesPlanAndAr() {
        CtrContract c = approveFlow("DEMO", "SALES", "80000.00");
        CtrPaymentPlan plan = singlePlan(c.getId());
        // 未同步直接收款 → 内部幂等生成 AR 再核销
        CtrPaymentPlan after = contractPlanService.registerReceipt("DEMO", plan.getId(),
                new BigDecimal("80000.00"), LocalDate.now(), "客户回款");

        assertThat(after.getPaidAmount()).isEqualByComparingTo("80000.00");
        assertThat(after.getStatus()).isEqualTo(CtrPaymentPlan.PLAN_STATUS_PAID);
        FinAr ar = arApService.pageAr("DEMO", null, null, 1, 10).getRecords().get(0);
        assertThat(ar.getReceivedAmount()).isEqualByComparingTo("80000.00");
        assertThat(ar.getStatus()).isEqualTo(FinAr.STATUS_SETTLED);
    }

    @Test
    void registerReceipt_partial_updatesPlanAndAr() {
        CtrContract c = approveFlow("DEMO", "SALES", "80000.00");
        CtrPaymentPlan plan = singlePlan(c.getId());

        CtrPaymentPlan after = contractPlanService.registerReceipt("DEMO", plan.getId(),
                new BigDecimal("30000.00"), LocalDate.now(), "首期回款");
        assertThat(after.getPaidAmount()).isEqualByComparingTo("30000.00");
        assertThat(after.getStatus()).isEqualTo(CtrPaymentPlan.PLAN_STATUS_PARTIAL);

        FinAr ar = arApService.pageAr("DEMO", null, null, 1, 10).getRecords().get(0);
        assertThat(ar.getReceivedAmount()).isEqualByComparingTo("30000.00");
        assertThat(ar.getStatus()).isEqualTo(FinAr.STATUS_PARTIAL);
    }

    @Test
    void registerPayment_full_settlesAp() {
        CtrContract c = approveFlow("DEMO", "PURCHASE", "30000.00");
        CtrPaymentPlan plan = singlePlan(c.getId());

        CtrPaymentPlan after = contractPlanService.registerPayment("DEMO", plan.getId(),
                new BigDecimal("30000.00"), LocalDate.now(), "供应商付款");
        assertThat(after.getStatus()).isEqualTo(CtrPaymentPlan.PLAN_STATUS_PAID);

        FinAp ap = arApService.pageAp("DEMO", null, null, 1, 10).getRecords().get(0);
        assertThat(ap.getPaidAmount()).isEqualByComparingTo("30000.00");
        assertThat(ap.getStatus()).isEqualTo(FinAp.STATUS_SETTLED);
    }

    @Test
    void registerReceipt_overpay_rejected() {
        CtrContract c = approveFlow("DEMO", "SALES", "80000.00");
        CtrPaymentPlan plan = singlePlan(c.getId());

        assertThatThrownBy(() -> contractPlanService.registerReceipt("DEMO", plan.getId(),
                new BigDecimal("80001.00"), LocalDate.now(), "超收"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超额");

        // 单据与计划未被污染
        assertThat(arApService.pageAr("DEMO", null, null, 1, 10).getTotal()).isZero();
        assertThat(singlePlan(c.getId()).getStatus()).isEqualTo(CtrPaymentPlan.PLAN_STATUS_UNPAID);
    }

    @Test
    void registerReceipt_wrongPlanType_rejected() {
        CtrContract c = approveFlow("DEMO", "PURCHASE", "30000.00");
        CtrPaymentPlan plan = singlePlan(c.getId());
        assertThatThrownBy(() -> contractPlanService.registerReceipt("DEMO", plan.getId(),
                new BigDecimal("100.00"), LocalDate.now(), "走错方向"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("付款计划");
    }

    // ===== 进度视图 / 分页 =====

    @Test
    void listPlans_exposesProgressFields() {
        CtrContract c = approveFlow("DEMO", "SALES", "80000.00");
        CtrPaymentPlan plan = singlePlan(c.getId());
        contractPlanService.registerReceipt("DEMO", plan.getId(), new BigDecimal("40000.00"),
                LocalDate.now(), "一半");

        List<CtrPaymentPlan> plans = contractPlanService.listPlans("DEMO", c.getId());
        assertThat(plans).hasSize(1);
        // 进度 = paidAmount/amount（40k/80k），部分核销 → PARTIAL
        assertThat(plans.get(0).getPaidAmount()).isEqualByComparingTo("40000.00");
        assertThat(plans.get(0).getStatus()).isEqualTo(CtrPaymentPlan.PLAN_STATUS_PARTIAL);
    }

    @Test
    void pageAr_filtersByStatus() {
        CtrContract c = approveFlow("DEMO", "SALES", "80000.00");
        contractPlanService.registerReceipt("DEMO", singlePlan(c.getId()).getId(),
                new BigDecimal("80000.00"), LocalDate.now(), "结清");

        PageResult<FinAr> settled = arApService.pageAr("DEMO", FinAr.STATUS_SETTLED, null, 1, 10);
        assertThat(settled.getTotal()).isEqualTo(1);
        PageResult<FinAr> open = arApService.pageAr("DEMO", FinAr.STATUS_OPEN, null, 1, 10);
        assertThat(open.getTotal()).isZero();
    }

    @Test
    void pageAp_filtersByStatusAndKeyword() {
        CtrContract c1 = approveFlow("DEMO", "PURCHASE", "30000.00");
        contractPlanService.registerPayment("DEMO", singlePlan(c1.getId()).getId(),
                new BigDecimal("30000.00"), LocalDate.now(), "结清");
        CtrContract c2 = approveFlow("DEMO", "PURCHASE", "50000.00");
        contractPlanService.syncDuePlans("DEMO", c2.getId()); // c2 生成 OPEN 应付单

        PageResult<FinAp> settled = arApService.pageAp("DEMO", FinAp.STATUS_SETTLED, null, 1, 10);
        assertThat(settled.getTotal()).isEqualTo(1);
        PageResult<FinAp> open = arApService.pageAp("DEMO", FinAp.STATUS_OPEN, null, 1, 10);
        assertThat(open.getTotal()).isEqualTo(1);
        PageResult<FinAp> byParty = arApService.pageAp("DEMO", null, "往来方", 1, 10);
        assertThat(byParty.getTotal()).isEqualTo(2);
    }

    // ==================== 测试工具 ====================

    private CtrPaymentPlan singlePlan(Long contractId) {
        List<CtrPaymentPlan> plans = contractPlanService.listPlans("DEMO", contractId);
        assertThat(plans).hasSize(1);
        return plans.get(0);
    }

    private CtrContract approveFlow(String company, String type, String amount) {
        return approveFlowWithStartDate(company, type, amount, "2026-09-01");
    }

    private CtrContract approveFlowWithStartDate(String company, String type, String amount, String startDate) {
        String approver = "approver-" + SEQ.incrementAndGet();
        ContractDTO dto = new ContractDTO();
        dto.setContractName("联动合同-" + SEQ.incrementAndGet());
        dto.setCounterparty("往来方-" + SEQ.incrementAndGet());
        dto.setContractType(type);
        dto.setAmount(new BigDecimal(amount));
        dto.setSignDate(LocalDate.of(2026, 9, 1));
        dto.setStartDate(LocalDate.parse(startDate));
        dto.setEndDate(LocalDate.of(2027, 8, 31));
        dto.setRemark("C3 测试");
        CtrContract c = contractService.create(company, dto);
        ContractSubmitDTO submit = new ContractSubmitDTO();
        submit.setApprover(approver);
        submit.setComment("请审批");
        contractService.submit(company, c.getId(), submit);
        List<WorkflowTaskVO> todos = workflowService.todo(approver);
        assertThat(todos).isNotEmpty();
        contractService.approveTask(todos.get(0).getTaskId(), "同意");
        return contractService.getById(company, c.getId());
    }
}
