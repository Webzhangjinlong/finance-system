package com.finance.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.finance.domain.FinExpenseClaim;
import com.finance.finance.domain.FinExpenseItem;
import com.finance.finance.domain.FinSubject;
import com.finance.finance.domain.FinVoucherEntry;
import com.finance.finance.dto.ExpenseClaimDTO;
import com.finance.finance.dto.ExpenseItemDTO;
import com.finance.finance.dto.ExpenseSubmitDTO;
import com.finance.finance.dto.VoucherDTO;
import com.finance.finance.mapper.FinExpenseClaimMapper;
import com.finance.finance.mapper.FinExpenseItemMapper;
import com.finance.framework.security.SecurityUtils;
import com.finance.workflow.domain.WfProcessInstance;
import com.finance.workflow.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 费用报销（F6，Gate 13）。
 *
 * <p>状态机：DRAFT 草稿 → SUBMITTED 审批中（W1 单人顺序审批）→ APPROVED 已通过
 * → PAID 已打款（打款自动生成凭证）；SUBMITTED 驳回 → REJECTED（可重新提交）。
 * 硬约束落地：3（明细仅末级科目 + 金额>0，服务校验 + DB CHECK 双兜底）、
 * 7（打款生成凭证 source_type=EXPENSE + source_id 幂等，重复生成由 VoucherService 拦截）、
 * 12（写操作事务）、16（状态/类型用常量禁魔法值）、17（审计日志、敏感信息不落日志）。</p>
 */
@Service
public class ExpenseService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseService.class);

    /** 审批流业务类型（W1 business_type，幂等关联）。 */
    public static final String BUSINESS_TYPE_EXPENSE = "EXPENSE";

    private final FinExpenseClaimMapper claimMapper;
    private final FinExpenseItemMapper itemMapper;
    private final WorkflowService workflowService;
    private final VoucherService voucherService;
    private final SubjectService subjectService;
    private final VoucherRuleEngineService voucherRuleEngineService;

    public ExpenseService(FinExpenseClaimMapper claimMapper,
                          FinExpenseItemMapper itemMapper,
                          WorkflowService workflowService,
                          VoucherService voucherService,
                          SubjectService subjectService,
                          VoucherRuleEngineService voucherRuleEngineService) {
        this.claimMapper = claimMapper;
        this.itemMapper = itemMapper;
        this.workflowService = workflowService;
        this.voucherService = voucherService;
        this.subjectService = subjectService;
        this.voucherRuleEngineService = voucherRuleEngineService;
    }

    /** 新建报销单（DRAFT）。 */
    @Transactional
    public FinExpenseClaim create(String companyCode, ExpenseClaimDTO dto) {
        validateItems(companyCode, dto.getItems());
        BigDecimal total = dto.getItems().stream()
                .map(ExpenseItemDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        FinExpenseClaim claim = new FinExpenseClaim();
        claim.setCompanyCode(companyCode);
        claim.setClaimNo(nextClaimNo(companyCode));
        claim.setExpenseType(dto.getExpenseType());
        claim.setReason(dto.getReason());
        claim.setInvoiceNo(dto.getInvoiceNo());
        claim.setAmount(total);
        claim.setStatus(FinExpenseClaim.STATUS_DRAFT);
        claimMapper.insert(claim);
        insertItems(companyCode, claim.getId(), dto.getItems());
        log.info("新建报销单：id={} no={} amount={}", claim.getId(), claim.getClaimNo(), total);
        return claim;
    }

    /** 编辑报销单（仅 DRAFT，已打款不可改）。 */
    @Transactional
    public void update(String companyCode, Long id, ExpenseClaimDTO dto) {
        FinExpenseClaim exist = getById(companyCode, id);
        requireDraft(exist);
        validateItems(companyCode, dto.getItems());
        BigDecimal total = dto.getItems().stream()
                .map(ExpenseItemDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        FinExpenseClaim update = new FinExpenseClaim();
        update.setId(id);
        update.setExpenseType(dto.getExpenseType());
        update.setReason(dto.getReason());
        update.setInvoiceNo(dto.getInvoiceNo());
        update.setAmount(total);
        claimMapper.updateById(update);
        itemMapper.delete(new LambdaQueryWrapper<FinExpenseItem>()
                .eq(FinExpenseItem::getClaimId, id));
        insertItems(companyCode, id, dto.getItems());
        log.info("编辑报销单：id={} no={} amount={}", id, exist.getClaimNo(), total);
    }

    /** 删除报销单（仅 DRAFT）。 */
    @Transactional
    public void delete(String companyCode, Long id) {
        FinExpenseClaim exist = getById(companyCode, id);
        requireDraft(exist);
        itemMapper.delete(new LambdaQueryWrapper<FinExpenseItem>()
                .eq(FinExpenseItem::getClaimId, id));
        claimMapper.deleteById(id);
        log.info("删除报销单：id={} no={}", id, exist.getClaimNo());
    }

    /** 提交审批：DRAFT/REJECTED → SUBMITTED，启动 W1 审批流。 */
    @Transactional
    public void submit(String companyCode, Long id, ExpenseSubmitDTO dto) {
        FinExpenseClaim exist = getById(companyCode, id);
        if (!FinExpenseClaim.STATUS_DRAFT.equals(exist.getStatus())
                && !FinExpenseClaim.STATUS_REJECTED.equals(exist.getStatus())) {
            throw new BusinessException("仅草稿/已驳回报销单可提交审批（当前：" + exist.getStatus() + "）");
        }
        workflowService.start(companyCode, BUSINESS_TYPE_EXPENSE, id, dto.getApprover(), dto.getComment());
        FinExpenseClaim update = new FinExpenseClaim();
        update.setId(id);
        update.setStatus(FinExpenseClaim.STATUS_SUBMITTED);
        update.setApprover(dto.getApprover());
        claimMapper.updateById(update);
        log.info("报销单提交审批：id={} no={} approver={}", id, exist.getClaimNo(), dto.getApprover());
    }

    /** 审批通过回调：SUBMITTED → APPROVED。 */
    @Transactional
    public void approveTask(String taskId, String comment) {
        WfProcessInstance instance = workflowService.approve(taskId, comment);
        FinExpenseClaim claim = claimMapper.selectOne(new LambdaQueryWrapper<FinExpenseClaim>()
                .eq(FinExpenseClaim::getCompanyCode, instance.getCompanyCode())
                .eq(FinExpenseClaim::getId, instance.getBusinessId())
                .last("LIMIT 1"));
        if (claim == null) {
            throw new BusinessException("报销单不存在，无法完成审批回调");
        }
        if (FinExpenseClaim.STATUS_SUBMITTED.equals(claim.getStatus())) {
            FinExpenseClaim update = new FinExpenseClaim();
            update.setId(claim.getId());
            update.setStatus(FinExpenseClaim.STATUS_APPROVED);
            update.setApprovedAt(OffsetDateTime.now());
            claimMapper.updateById(update);
        }
        log.info("报销单审批通过：id={} no={}", claim.getId(), claim.getClaimNo());
    }

    /** 审批驳回回调：SUBMITTED → REJECTED（可重新提交）。 */
    @Transactional
    public void rejectTask(String taskId, String comment) {
        WfProcessInstance instance = workflowService.reject(taskId, comment);
        FinExpenseClaim claim = claimMapper.selectOne(new LambdaQueryWrapper<FinExpenseClaim>()
                .eq(FinExpenseClaim::getCompanyCode, instance.getCompanyCode())
                .eq(FinExpenseClaim::getId, instance.getBusinessId())
                .last("LIMIT 1"));
        if (claim == null) {
            throw new BusinessException("报销单不存在，无法完成审批回调");
        }
        claimMapper.update(null, new LambdaUpdateWrapper<FinExpenseClaim>()
                .eq(FinExpenseClaim::getId, claim.getId())
                .set(FinExpenseClaim::getStatus, FinExpenseClaim.STATUS_REJECTED));
        log.info("报销单审批驳回：id={} no={}", claim.getId(), claim.getClaimNo());
    }

    /** 财务打款：APPROVED → PAID，并生成费用凭证（借明细科目 / 贷银行存款，source 幂等）。 */
    @Transactional
    public void pay(String companyCode, Long id) {
        FinExpenseClaim exist = getById(companyCode, id);
        if (!FinExpenseClaim.STATUS_APPROVED.equals(exist.getStatus())) {
            throw new BusinessException("仅已通过报销单可打款（当前：" + exist.getStatus() + "）");
        }
        List<FinExpenseItem> items = itemMapper.selectList(new LambdaQueryWrapper<FinExpenseItem>()
                .eq(FinExpenseItem::getClaimId, id));
        VoucherDTO dto = buildVoucher(exist, items);
        voucherService.create(companyCode, dto);
        FinExpenseClaim update = new FinExpenseClaim();
        update.setId(id);
        update.setStatus(FinExpenseClaim.STATUS_PAID);
        update.setPayer(SecurityUtils.getUsername());
        update.setPaidAt(OffsetDateTime.now());
        claimMapper.updateById(update);
        log.info("报销单打款并生成凭证：id={} no={} amount={}", id, exist.getClaimNo(), exist.getAmount());
    }

    /** 分页列表（公司隔离 + 状态/关键词过滤）。 */
    public PageResult<FinExpenseClaim> page(String companyCode, String keyword, String status,
                                            long page, long size) {
        LambdaQueryWrapper<FinExpenseClaim> wrapper = new LambdaQueryWrapper<FinExpenseClaim>()
                .eq(FinExpenseClaim::getCompanyCode, companyCode)
                .eq(status != null && !status.isBlank(), FinExpenseClaim::getStatus, status)
                .orderByDesc(FinExpenseClaim::getCreateTime);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(FinExpenseClaim::getClaimNo, keyword)
                    .or().like(FinExpenseClaim::getReason, keyword));
        }
        Page<FinExpenseClaim> p = claimMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** 报销单详情（含明细行，公司隔离）。 */
    public FinExpenseClaim detail(String companyCode, Long id) {
        FinExpenseClaim claim = getById(companyCode, id);
        claim.setItems(itemMapper.selectList(new LambdaQueryWrapper<FinExpenseItem>()
                .eq(FinExpenseItem::getClaimId, id)
                .orderByAsc(FinExpenseItem::getId)));
        return claim;
    }

    /** 明细行校验：科目存在且仅末级科目可入账（硬约束 3）。 */
    private void validateItems(String companyCode, List<ExpenseItemDTO> items) {
        Map<String, FinSubject> index = subjectService.codeIndex(companyCode);
        for (ExpenseItemDTO item : items) {
            FinSubject subject = index.get(item.getSubjectCode());
            if (subject == null) {
                throw new BusinessException("科目不存在：" + item.getSubjectCode());
            }
            if (subject.getIsLeaf() == null || subject.getIsLeaf() != 1) {
                throw new BusinessException("仅末级科目可记账：" + item.getSubjectCode());
            }
        }
    }

    private void insertItems(String companyCode, Long claimId, List<ExpenseItemDTO> items) {
        for (ExpenseItemDTO item : items) {
            FinExpenseItem e = new FinExpenseItem();
            e.setCompanyCode(companyCode);
            e.setClaimId(claimId);
            e.setSubjectCode(item.getSubjectCode());
            e.setSummary(item.getSummary());
            e.setAmount(item.getAmount());
            itemMapper.insert(e);
        }
    }

    /** 报销单号：BX-{年份}-{4 位序列}（公司内唯一，uk_expense_claim_no 兜底）。 */
    private String nextClaimNo(String companyCode) {
        String prefix = "BX-" + Year.now().getValue() + "-";
        String latest = claimMapper.selectLatestClaimNo(companyCode, prefix + "%");
        int seq = 1;
        if (latest != null) {
            seq = Integer.parseInt(latest.substring(latest.lastIndexOf('-') + 1)) + 1;
        }
        return prefix + String.format("%04d", seq);
    }

    /** 打款凭证：借各明细费用科目 / 贷 1002 银行存款（source_type=EXPENSE 幂等，硬约束 7）。 */
    private VoucherDTO buildVoucher(FinExpenseClaim claim, List<FinExpenseItem> items) {
        LocalDate today = LocalDate.now();
        VoucherDTO dto = new VoucherDTO();
        dto.setPeriodYear(today.getYear());
        dto.setPeriodMonth(today.getMonthValue());
        dto.setVoucherDate(today);
        dto.setRemark("费用报销 " + claim.getClaimNo() + " " + claim.getReason());
        dto.setSourceType(FinExpenseClaim.SOURCE_TYPE);
        dto.setSourceId(claim.getId());
        List<FinVoucherEntry> entries = new ArrayList<>();
        for (FinExpenseItem item : items) {
            String summary = item.getSummary() == null || item.getSummary().isBlank()
                    ? "报销 " + claim.getClaimNo() : item.getSummary();
            entries.add(entry(item.getSubjectCode(), summary, item.getAmount(), null));
        }
        // F8 映射引擎：贷方科目由规则驱动（EXPENSE/PAID → 规则 subject_code + 摘要模板）；
        // 无规则时回退默认 1002 银行存款（兼容历史 F6 硬编码映射）
        java.util.List<FinVoucherEntry> ruleEntries = voucherRuleEngineService.resolve(
                claim.getCompanyCode(), FinExpenseClaim.SOURCE_TYPE, "PAID",
                claim.getAmount(), java.util.Map.of("claimNo", claim.getClaimNo()));
        if (ruleEntries.isEmpty()) {
            entries.add(entry("1002", "报销打款 " + claim.getClaimNo(), null, claim.getAmount()));
        } else {
            entries.addAll(ruleEntries);
        }
        dto.setEntries(entries);
        return dto;
    }

    private FinVoucherEntry entry(String subjectCode, String summary, BigDecimal debit, BigDecimal credit) {
        FinVoucherEntry e = new FinVoucherEntry();
        e.setSubjectCode(subjectCode);
        e.setSummary(summary);
        e.setDebitAmount(debit);
        e.setCreditAmount(credit);
        return e;
    }

    private FinExpenseClaim getById(String companyCode, Long id) {
        FinExpenseClaim claim = claimMapper.selectOne(new LambdaQueryWrapper<FinExpenseClaim>()
                .eq(FinExpenseClaim::getCompanyCode, companyCode)
                .eq(FinExpenseClaim::getId, id)
                .last("LIMIT 1"));
        if (claim == null) {
            throw new BusinessException("报销单不存在：" + id);
        }
        return claim;
    }

    private void requireDraft(FinExpenseClaim claim) {
        if (!FinExpenseClaim.STATUS_DRAFT.equals(claim.getStatus())) {
            throw new BusinessException("仅草稿报销单可操作（当前：" + claim.getStatus() + "）");
        }
    }
}
