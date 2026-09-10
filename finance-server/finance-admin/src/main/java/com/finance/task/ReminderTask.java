package com.finance.task;

import com.finance.contract.domain.CtrContract;
import com.finance.contract.domain.CtrPaymentPlan;
import com.finance.contract.service.ContractPlanService;
import com.finance.contract.service.ContractService;
import com.finance.system.domain.SysMessage;
import com.finance.system.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 到期提醒定时任务（W4，docs 5.4 / backlog W4）。
 *
 * <p>调度：08:00 到期提醒（合同提前 30 天 / 计划提前 7 天），08:30 逾期扫描
 * （到期未清计划置 OVERDUE + 逾期消息）。消息幂等：按 单据+日期+类型 去重
 * （uq_sys_message_remind）。接收人 = 业务单创建人（sys_user.username 解析）。</p>
 */
@Component
public class ReminderTask {

    private static final Logger log = LoggerFactory.getLogger(ReminderTask.class);

    private final ContractService contractService;
    private final ContractPlanService contractPlanService;
    private final MessageService messageService;
    private final JdbcTemplate jdbcTemplate;

    public ReminderTask(ContractService contractService,
                        ContractPlanService contractPlanService,
                        MessageService messageService,
                        JdbcTemplate jdbcTemplate) {
        this.contractService = contractService;
        this.contractPlanService = contractPlanService;
        this.messageService = messageService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 08:00 到期提醒：合同到期前 30 天 + 收付款计划到期前 7 天。 */
    @Scheduled(cron = "0 0 8 * * ?")
    public void dueReminder() {
        LocalDate today = LocalDate.now();
        log.info("到期提醒任务启动：today={}", today);
        for (String companyCode : listCompanies()) {
            remindExpiringContracts(companyCode, today);
            remindDuePlans(companyCode, today);
        }
        log.info("到期提醒任务完成");
    }

    /** 08:30 逾期扫描：到期未清计划置 OVERDUE + 逾期消息。 */
    @Scheduled(cron = "0 30 8 * * ?")
    public void overdueScan() {
        LocalDate today = LocalDate.now();
        log.info("逾期扫描任务启动：today={}", today);
        int marked = 0;
        for (String companyCode : listCompanies()) {
            List<CtrPaymentPlan> overduePlans =
                    contractPlanService.listOverduePlans(companyCode, today);
            for (CtrPaymentPlan plan : overduePlans) {
                contractPlanService.markOverdue(companyCode, plan.getId());
                sendOverdueMessage(companyCode, plan);
                marked++;
            }
        }
        log.info("逾期扫描任务完成：{} 条计划标记逾期", marked);
    }

    // ==================== 内部实现 ====================

    private void remindExpiringContracts(String companyCode, LocalDate today) {
        List<CtrContract> contracts =
                contractService.listExpiringContracts(companyCode, today, today.plusDays(30));
        for (CtrContract contract : contracts) {
            Long receiverId = resolveUserId(companyCode, contract.getCreateBy());
            if (receiverId == null) {
                continue;
            }
            messageService.send(companyCode, receiverId, SysMessage.TYPE_CONTRACT_EXPIRE,
                    "合同即将到期",
                    "合同 " + contract.getContractNo() + "（" + contract.getContractName()
                            + "）将于 " + contract.getEndDate() + " 到期，请及时处理。",
                    "CONTRACT", contract.getId(), today);
        }
    }

    private void remindDuePlans(String companyCode, LocalDate today) {
        List<CtrPaymentPlan> plans =
                contractPlanService.listDuePlans(companyCode, today, today.plusDays(7));
        for (CtrPaymentPlan plan : plans) {
            Long receiverId = resolvePlanOwner(companyCode, plan);
            if (receiverId == null) {
                continue;
            }
            String direction = CtrPaymentPlan.PLAN_TYPE_RECEIPT.equals(plan.getPlanType())
                    ? "收款" : "付款";
            messageService.send(companyCode, receiverId, SysMessage.TYPE_PLAN_DUE,
                    "收付款计划即将到期",
                    "计划 " + plan.getPlanNo() + "（" + direction + " " + plan.getAmount()
                            + " 元）将于 " + plan.getPlanDate() + " 到期，请及时处理。",
                    "PAYMENT_PLAN", plan.getId(), today);
        }
    }

    private void sendOverdueMessage(String companyCode, CtrPaymentPlan plan) {
        Long receiverId = resolvePlanOwner(companyCode, plan);
        if (receiverId == null) {
            return;
        }
        boolean receipt = CtrPaymentPlan.PLAN_TYPE_RECEIPT.equals(plan.getPlanType());
        messageService.send(companyCode, receiverId,
                receipt ? SysMessage.TYPE_AR_OVERDUE : SysMessage.TYPE_AP_OVERDUE,
                receipt ? "应收逾期提醒" : "应付逾期提醒",
                "计划 " + plan.getPlanNo() + "（" + (receipt ? "应收" : "应付") + " "
                        + plan.getAmount() + " 元，已" + (receipt ? "收" : "付")
                        + plan.getPaidAmount() + " 元）已逾期，请及时催收/安排付款。",
                "PAYMENT_PLAN", plan.getId(), LocalDate.now());
    }

    /** 计划责任人 = 所属合同创建人（sys_user.username 解析）。 */
    private Long resolvePlanOwner(String companyCode, CtrPaymentPlan plan) {
        CtrContract contract = contractService.getById(companyCode, plan.getContractId());
        if (contract == null) {
            return null;
        }
        return resolveUserId(companyCode, contract.getCreateBy());
    }

    /** create_by（用户名）→ sys_user.id；查不到返回 null（日志警告，不中断任务）。 */
    private Long resolveUserId(String companyCode, String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM sys_user WHERE username = ? AND deleted = 0 LIMIT 1",
                Long.class, username);
        if (ids.isEmpty()) {
            log.warn("到期提醒跳过：用户不存在 username={}", username);
            return null;
        }
        return ids.get(0);
    }

    private List<String> listCompanies() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT company_code FROM ctr_contract "
                        + "UNION SELECT DISTINCT company_code FROM ctr_payment_plan",
                String.class);
    }
}
