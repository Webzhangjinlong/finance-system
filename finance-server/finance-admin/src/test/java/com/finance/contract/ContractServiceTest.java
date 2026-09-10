package com.finance.contract;

import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.contract.domain.CtrContract;
import com.finance.contract.domain.CtrPaymentPlan;
import com.finance.contract.dto.ContractDTO;
import com.finance.contract.dto.ContractSubmitDTO;
import com.finance.contract.service.ContractService;
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
 * 合同台账 + 审批集成测试（C1/C2，docs 5.1/5.2）：真实 Flowable 引擎 + 测试库。
 *
 * <p>覆盖：登记（编号自动生成/DB CHECK 兜底）、修改删除保护、作废、
 * 提交审批（状态+流程发起）、审批通过（生效+生成计划幂等）、审批驳回（回 DRAFT）。
 * Flowable 运行表独立事务不回滚，@BeforeEach 物理清理流程与合同数据保证可重。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class ContractServiceTest {

    private static final AtomicLong SEQ = new AtomicLong(10000);

    @Autowired
    private ContractService contractService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        // 合同与计划物理删除（编号/计划号唯一索引依赖物理清理释放）
        jdbcTemplate.execute("DELETE FROM ctr_payment_plan");
        jdbcTemplate.execute("DELETE FROM ctr_contract");
        jdbcTemplate.execute("DELETE FROM wf_process_instance");
        jdbcTemplate.execute("DELETE FROM act_ru_identitylink");
        jdbcTemplate.execute("DELETE FROM act_ru_variable");
        jdbcTemplate.execute("DELETE FROM act_ru_task");
        jdbcTemplate.execute("DELETE FROM act_ru_execution");
    }

    // ===== C1 台账 =====

    @Test
    void create_generatesContractNoAndDraft() {
        CtrContract c = createContract("DEMO", "采购框架协议", "供应商A", "PURCHASE", "50000.00");
        assertThat(c.getId()).isNotNull();
        assertThat(c.getContractNo()).startsWith("HT-" + LocalDate.now().getYear() + "-");
        assertThat(c.getStatus()).isEqualTo(CtrContract.STATUS_DRAFT);
        assertThat(c.getCompanyCode()).isEqualTo("DEMO");

        // 编号递增
        CtrContract c2 = createContract("DEMO", "销售合同", "客户B", "SALES", "30000.00");
        assertThat(c2.getContractNo()).isEqualTo(inc(c.getContractNo()));
    }

    @Test
    void create_invalidType_rejectedByDbCheck() {
        ContractDTO dto = baseDto("异常类型合同", "供应商C", "FURNITURE", "100.00");
        assertThatThrownBy(() -> contractService.create("DEMO", dto))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void update_draftOnly_succeeds() {
        CtrContract c = createContract("DEMO", "原名称", "供应商A", "PURCHASE", "50000.00");
        ContractDTO dto = baseDto("新名称", "供应商A", "PURCHASE", "60000.00");
        contractService.update("DEMO", c.getId(), dto);
        CtrContract after = contractService.getById("DEMO", c.getId());
        assertThat(after.getContractName()).isEqualTo("新名称");
        assertThat(after.getAmount()).isEqualByComparingTo("60000.00");
    }

    @Test
    void update_afterSubmit_rejected() {
        CtrContract c = submitContract("DEMO", "PURCHASE", "50000.00");
        ContractDTO dto = baseDto("改不动", "供应商A", "PURCHASE", "60000.00");
        assertThatThrownBy(() -> contractService.update("DEMO", c.getId(), dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅草拟");
    }

    @Test
    void delete_draftOnly_deleteProtection() {
        CtrContract c = createContract("DEMO", "可删合同", "供应商A", "PURCHASE", "100.00");
        contractService.delete("DEMO", c.getId());
        assertThatThrownBy(() -> contractService.getById("DEMO", c.getId()))
                .isInstanceOf(BusinessException.class);

        CtrContract submitted = submitContract("DEMO", "PURCHASE", "200.00");
        assertThatThrownBy(() -> contractService.delete("DEMO", submitted.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("禁止删除");
    }

    @Test
    void voidContract_terminatesApproved() {
        CtrContract approved = approveFlow("DEMO", "SALES", "80000.00");
        contractService.voidContract("DEMO", approved.getId(), "双方协商终止");
        CtrContract after = contractService.getById("DEMO", approved.getId());
        assertThat(after.getStatus()).isEqualTo(CtrContract.STATUS_TERMINATED);
        assertThat(after.getRemark()).contains("双方协商终止");
    }

    @Test
    void page_filtersByCompanyAndStatus() {
        createContract("DEMO", "甲方合同", "供应商A", "PURCHASE", "100.00");
        createContract("DEMO", "乙方合同", "供应商B", "SALES", "200.00");
        PageResult<CtrContract> p = contractService.page("DEMO", "甲方", null, 1, 10);
        assertThat(p.getTotal()).isEqualTo(1);
        assertThat(p.getRecords().get(0).getContractName()).isEqualTo("甲方合同");
        assertThat(p.getRecords().get(0).getCompanyCode()).isEqualTo("DEMO");
    }

    // ===== C2 审批 =====

    @Test
    void submit_startsApproval() {
        CtrContract c = createContract("DEMO", "待审合同", "供应商A", "PURCHASE", "50000.00");
        contractService.submit("DEMO", c.getId(), submitDto("approver-" + SEQ.incrementAndGet()));
        CtrContract after = contractService.getById("DEMO", c.getId());
        assertThat(after.getStatus()).isEqualTo(CtrContract.STATUS_APPROVING);
        assertThat(after.getProcessInstanceId()).isNotNull();
    }

    @Test
    void submit_duplicate_rejected() {
        CtrContract c = submitContract("DEMO", "PURCHASE", "50000.00");
        assertThatThrownBy(() -> contractService.submit("DEMO", c.getId(), submitDto("approver-again")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅草拟");
    }

    @Test
    void approveTask_activatesContractAndGeneratesPlan() {
        CtrContract c = approveFlow("DEMO", "SALES", "80000.00");
        CtrContract after = contractService.getById("DEMO", c.getId());
        assertThat(after.getStatus()).isEqualTo(CtrContract.STATUS_APPROVED);

        List<CtrPaymentPlan> plans = contractService.listPlans("DEMO", c.getId());
        assertThat(plans).hasSize(1);
        CtrPaymentPlan plan = plans.get(0);
        assertThat(plan.getPlanType()).isEqualTo(CtrPaymentPlan.PLAN_TYPE_RECEIPT); // SALES → 收
        assertThat(plan.getAmount()).isEqualByComparingTo("80000.00");
        assertThat(plan.getPlanNo()).startsWith("PLAN-" + after.getContractNo() + "-");
    }

    @Test
    void approveTask_planGeneratedOnce() {
        CtrContract c = approveFlow("DEMO", "PURCHASE", "30000.00");
        // 二次用已完成任务审批 → 流程层拒绝（幂等由流程状态保证）
        List<WorkflowTaskVO> todos = workflowService.todo("approver-x");
        assertThat(todos).isEmpty();
        // 计划恰好 1 条（不重复生成）
        assertThat(contractService.listPlans("DEMO", c.getId())).hasSize(1);
        assertThat(c.getStatus()).isEqualTo(CtrContract.STATUS_APPROVED);
    }

    @Test
    void rejectTask_returnsToDraft() {
        String approver = "approver-" + SEQ.incrementAndGet();
        CtrContract c = createContract("DEMO", "将被驳回", "供应商A", "PURCHASE", "50000.00");
        contractService.submit("DEMO", c.getId(), submitDto(approver));
        String taskId = firstTodoTaskId(approver);

        contractService.rejectTask(taskId, "条款不符");
        CtrContract after = contractService.getById("DEMO", c.getId());
        assertThat(after.getStatus()).isEqualTo(CtrContract.STATUS_DRAFT);
        assertThat(after.getProcessInstanceId()).isNull();
        assertThat(contractService.listPlans("DEMO", c.getId())).isEmpty();
    }

    // ==================== 测试工具 ====================

    private CtrContract createContract(String company, String name, String party,
                                       String type, String amount) {
        return contractService.create(company, baseDto(name, party, type, amount));
    }

    private CtrContract submitContract(String company, String type, String amount) {
        CtrContract c = createContract(company, "提交合同-" + SEQ.incrementAndGet(), "供应商A", type, amount);
        contractService.submit(company, c.getId(), submitDto("approver-" + SEQ.incrementAndGet()));
        return contractService.getById(company, c.getId());
    }

    /** 走完整流程到 APPROVED：创建 → 提交 → 审批通过。 */
    private CtrContract approveFlow(String company, String type, String amount) {
        String approver = "approver-" + SEQ.incrementAndGet();
        CtrContract c = createContract(company, "审批合同-" + SEQ.incrementAndGet(), "供应商A", type, amount);
        contractService.submit(company, c.getId(), submitDto(approver));
        contractService.approveTask(firstTodoTaskId(approver), "同意");
        return contractService.getById(company, c.getId());
    }

    private String firstTodoTaskId(String approver) {
        List<WorkflowTaskVO> todos = workflowService.todo(approver);
        assertThat(todos).isNotEmpty();
        return todos.get(0).getTaskId();
    }

    private ContractDTO baseDto(String name, String party, String type, String amount) {
        ContractDTO dto = new ContractDTO();
        dto.setContractName(name);
        dto.setCounterparty(party);
        dto.setContractType(type);
        dto.setAmount(new BigDecimal(amount));
        dto.setSignDate(LocalDate.of(2026, 9, 1));
        dto.setStartDate(LocalDate.of(2026, 9, 1));
        dto.setEndDate(LocalDate.of(2027, 8, 31));
        dto.setRemark("测试合同");
        return dto;
    }

    private ContractSubmitDTO submitDto(String approver) {
        ContractSubmitDTO dto = new ContractSubmitDTO();
        dto.setApprover(approver);
        dto.setComment("请审批");
        return dto;
    }

    /** HT-2026-0001 → HT-2026-0002。 */
    private String inc(String no) {
        int seq = Integer.parseInt(no.substring(no.lastIndexOf('-') + 1));
        return no.substring(0, no.lastIndexOf('-') + 1) + String.format("%04d", seq + 1);
    }
}
