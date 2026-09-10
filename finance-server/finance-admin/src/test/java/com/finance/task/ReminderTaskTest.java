package com.finance.task;

import com.finance.common.core.domain.PageResult;
import com.finance.contract.domain.CtrContract;
import com.finance.contract.domain.CtrPaymentPlan;
import com.finance.contract.dto.ContractDTO;
import com.finance.contract.dto.ContractSubmitDTO;
import com.finance.contract.service.ContractPlanService;
import com.finance.contract.service.ContractService;
import com.finance.system.domain.SysMessage;
import com.finance.system.service.MessageService;
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

/**
 * 到期提醒定时任务集成测试（W4，docs 5.4）：合同 30 天 / 计划 7 天提醒（幂等）、
 * 逾期扫描置 OVERDUE + 逾期消息（幂等）、未来单据不受影响。
 *
 * <p>接收人解析：合同 create_by=admin → sys_user 种子用户（V2 admin/admin123）。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class ReminderTaskTest {

    private static final AtomicLong SEQ = new AtomicLong(30000);

    @Autowired
    private ReminderTask reminderTask;
    @Autowired
    private ContractService contractService;
    @Autowired
    private ContractPlanService contractPlanService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long adminUserId;

    @BeforeEach
    void clean() {
        jdbcTemplate.execute("DELETE FROM sys_message");
        jdbcTemplate.execute("DELETE FROM fin_ar");
        jdbcTemplate.execute("DELETE FROM fin_ap");
        jdbcTemplate.execute("DELETE FROM ctr_payment_plan");
        jdbcTemplate.execute("DELETE FROM ctr_contract");
        jdbcTemplate.execute("DELETE FROM wf_process_instance");
        jdbcTemplate.execute("DELETE FROM act_ru_identitylink");
        jdbcTemplate.execute("DELETE FROM act_ru_variable");
        jdbcTemplate.execute("DELETE FROM act_ru_task");
        jdbcTemplate.execute("DELETE FROM act_ru_execution");
        adminUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = 'admin'", Long.class);
    }

    @Test
    void dueReminder_contractWithin30Days_sendsMessageIdempotent() {
        // endDate 在 30 天窗口内 → 合同提醒；startDate 推出 7 天窗口外（today+8）→ 不触发计划提醒
        LocalDate end = LocalDate.now().plusDays(15);
        approveContract("DEMO", "SALES", "80000.00", LocalDate.now().plusDays(8), end);

        reminderTask.dueReminder();
        List<SysMessage> messages = allMessages();
        assertThat(messages).hasSize(1); // 仅合同到期提醒（计划日期=今天，不在 7 天窗口? 见下）
        SysMessage m = messages.get(0);
        assertThat(m.getMessageType()).isEqualTo(SysMessage.TYPE_CONTRACT_EXPIRE);
        assertThat(m.getReceiverId()).isEqualTo(adminUserId);
        assertThat(m.getBusinessType()).isEqualTo("CONTRACT");
        assertThat(m.getRemindDate()).isEqualTo(LocalDate.now());

        // 幂等：再次执行不重复
        reminderTask.dueReminder();
        assertThat(allMessages()).hasSize(1);
    }

    @Test
    void dueReminder_planWithin7Days_sendsPlanMessage() {
        // 计划日期 = startDate = 今天+5 → 计划 7 天窗口提醒（合同 endDate 很远，不触发合同提醒）
        LocalDate planDate = LocalDate.now().plusDays(5);
        approveContract("DEMO", "SALES", "80000.00", planDate, LocalDate.now().plusYears(1));

        reminderTask.dueReminder();

        List<SysMessage> messages = allMessages();
        assertThat(messages).hasSize(1);
        SysMessage m = messages.get(0);
        assertThat(m.getMessageType()).isEqualTo(SysMessage.TYPE_PLAN_DUE);
        assertThat(m.getBusinessType()).isEqualTo("PAYMENT_PLAN");
    }

    @Test
    void dueReminder_futureContractsSkipped() {
        approveContract("DEMO", "SALES", "80000.00",
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(60));
        reminderTask.dueReminder();
        assertThat(allMessages()).isEmpty();
    }

    @Test
    void overdueScan_marksPlanOverdueAndSendsMessage() {
        CtrContract c = approveContract("DEMO", "SALES", "80000.00",
                LocalDate.now().minusDays(5), LocalDate.now().plusYears(1));
        CtrPaymentPlan plan = contractPlanService.listPlans("DEMO", c.getId()).get(0);
        assertThat(plan.getStatus()).isEqualTo(CtrPaymentPlan.PLAN_STATUS_UNPAID);

        reminderTask.overdueScan();

        // 计划置 OVERDUE（承接 C3 归属）+ 应收逾期消息
        assertThat(contractPlanService.listPlans("DEMO", c.getId()).get(0).getStatus())
                .isEqualTo(CtrPaymentPlan.PLAN_STATUS_OVERDUE);
        List<SysMessage> messages = allMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getMessageType()).isEqualTo(SysMessage.TYPE_AR_OVERDUE);
        assertThat(messages.get(0).getReceiverId()).isEqualTo(adminUserId);

        // 幂等：OVERDUE 不再重复标记/发消息
        reminderTask.overdueScan();
        assertThat(allMessages()).hasSize(1);
    }

    @Test
    void overdueScan_paidPlanUntouched() {
        CtrContract c = approveContract("DEMO", "PURCHASE", "30000.00",
                LocalDate.now().minusDays(5), LocalDate.now().plusYears(1));
        CtrPaymentPlan plan = contractPlanService.listPlans("DEMO", c.getId()).get(0);
        contractPlanService.registerPayment("DEMO", plan.getId(),
                new BigDecimal("30000.00"), LocalDate.now(), "已结清");

        reminderTask.overdueScan();

        assertThat(contractPlanService.listPlans("DEMO", c.getId()).get(0).getStatus())
                .isEqualTo(CtrPaymentPlan.PLAN_STATUS_PAID);
        assertThat(allMessages()).isEmpty();
    }

    // ==================== 测试工具 ====================

    private CtrContract approveContract(String company, String type, String amount,
                                        LocalDate startDate, LocalDate endDate) {
        String approver = "approver-" + SEQ.incrementAndGet();
        ContractDTO dto = new ContractDTO();
        dto.setContractName("提醒合同-" + SEQ.incrementAndGet());
        dto.setCounterparty("往来方-" + SEQ.incrementAndGet());
        dto.setContractType(type);
        dto.setAmount(new BigDecimal(amount));
        dto.setSignDate(LocalDate.now());
        dto.setStartDate(startDate);
        dto.setEndDate(endDate);
        dto.setRemark("W4 测试");
        CtrContract c = contractService.create(company, dto);
        ContractSubmitDTO submit = new ContractSubmitDTO();
        submit.setApprover(approver);
        submit.setComment("请审批");
        contractService.submit(company, c.getId(), submit);
        List<WorkflowTaskVO> todos = workflowService.todo(approver);
        assertThat(todos).isNotEmpty();
        contractService.approveTask(todos.get(0).getTaskId(), "同意");
        // 测试无登录上下文，create_by 不自动填充 → 显式补齐（真实场景由 Controller 登录用户填充）
        jdbcTemplate.update("UPDATE ctr_contract SET create_by = 'admin' WHERE id = ?", c.getId());
        return contractService.getById(company, c.getId());
    }

    private List<SysMessage> allMessages() {
        PageResult<SysMessage> p = messageService.listMessages("DEMO", adminUserId, false, 1, 100);
        return p.getRecords();
    }
}
