package com.finance.contract.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.contract.domain.CtrContract;
import com.finance.contract.domain.CtrPaymentPlan;
import com.finance.contract.dto.ContractDTO;
import com.finance.contract.dto.ContractSubmitDTO;
import com.finance.contract.mapper.CtrContractMapper;
import com.finance.contract.mapper.CtrPaymentPlanMapper;
import com.finance.framework.security.SecurityUtils;
import com.finance.workflow.domain.WfProcessInstance;
import com.finance.workflow.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

/**
 * 合同台账与审批服务（C1/C2，docs 5.1/5.2）。
 *
 * <p>业务规则：
 * <ul>
 *   <li>合同编号自动生成（公司+年份+序列），保存即占号（DB 唯一约束 + 冲突重试防并发重号）；
 *       编号查询绕过逻辑删除过滤（uq_ctr_contract_no 唯一约束作用于全表，含逻辑删记录）；</li>
 *   <li>状态机 DRAFT → APPROVING → APPROVED（生效，自动生成收付款计划）→ ACTIVE/COMPLETED；可作废 TERMINATED；</li>
 *   <li>删除保护：非 DRAFT 禁止删除，只能作废（文档 5.1）；</li>
 *   <li>审批走 W1 WorkflowService（business_type=CONTRACT 幂等），通过回调生效并生成计划（幂等，不重复生成）；</li>
 *   <li>写操作 @Transactional + 公司隔离（硬约束 2/10/12）。</li>
 * </ul></p>
 */
@Service
public class ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

    /** 业务类型：合同（wf_process_instance.business_type）。 */
    public static final String BUSINESS_TYPE_CONTRACT = "CONTRACT";

    private final CtrContractMapper contractMapper;
    private final CtrPaymentPlanMapper paymentPlanMapper;
    private final WorkflowService workflowService;
    private final TransactionTemplate transactionTemplate;

    public ContractService(CtrContractMapper contractMapper,
                           CtrPaymentPlanMapper paymentPlanMapper,
                           WorkflowService workflowService,
                           TransactionTemplate transactionTemplate) {
        this.contractMapper = contractMapper;
        this.paymentPlanMapper = paymentPlanMapper;
        this.workflowService = workflowService;
        this.transactionTemplate = transactionTemplate;
    }

    /** 合同分页（公司隔离，keyword 匹配名称/编号，status 过滤）。 */
    public PageResult<CtrContract> page(String companyCode, String keyword, String status,
                                        long page, long size) {
        LambdaQueryWrapper<CtrContract> wrapper = new LambdaQueryWrapper<CtrContract>()
                .eq(CtrContract::getCompanyCode, companyCode)
                .eq(status != null && !status.isBlank(), CtrContract::getStatus, status)
                .orderByDesc(CtrContract::getCreateTime);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(CtrContract::getContractName, keyword)
                    .or().like(CtrContract::getContractNo, keyword));
        }
        Page<CtrContract> p = contractMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** 合同详情（公司隔离）。 */
    public CtrContract getById(String companyCode, Long id) {
        CtrContract contract = contractMapper.selectById(id);
        if (contract == null || !companyCode.equals(contract.getCompanyCode())) {
            throw new BusinessException("合同不存在");
        }
        return contract;
    }

    /** 登记合同（DRAFT，编号自动生成，保存即占号；并发冲突自动重试换号）。 */
    public CtrContract create(String companyCode, ContractDTO dto) {
        int maxRetry = 3;
        DuplicateKeyException last = null;
        for (int i = 0; i < maxRetry; i++) {
            try {
                return transactionTemplate.execute(status -> doCreate(companyCode, dto));
            } catch (DuplicateKeyException e) {
                last = e;
                log.warn("合同编号冲突，重试 {}/{}", i + 1, maxRetry);
            }
        }
        throw new BusinessException("合同编号分配冲突，请重试", last);
    }

    /** 修改合同（仅 DRAFT）。 */
    @Transactional
    public void update(String companyCode, Long id, ContractDTO dto) {
        CtrContract exist = getById(companyCode, id);
        if (!CtrContract.STATUS_DRAFT.equals(exist.getStatus())) {
            throw new BusinessException("仅草拟状态合同可修改");
        }
        CtrContract update = new CtrContract();
        update.setId(id);
        update.setContractName(dto.getContractName());
        update.setCounterparty(dto.getCounterparty());
        update.setContractType(dto.getContractType());
        update.setAmount(dto.getAmount());
        update.setSignDate(dto.getSignDate());
        update.setStartDate(dto.getStartDate());
        update.setEndDate(dto.getEndDate());
        update.setRemark(dto.getRemark());
        contractMapper.updateById(update);
    }

    /** 删除合同（删除保护：仅 DRAFT 可删，非 DRAFT 只能作废）。 */
    @Transactional
    public void delete(String companyCode, Long id) {
        CtrContract exist = getById(companyCode, id);
        if (!CtrContract.STATUS_DRAFT.equals(exist.getStatus())) {
            throw new BusinessException("合同已提交审批，禁止删除，可作废处理");
        }
        contractMapper.deleteById(id);
    }

    /** 作废合同（APPROVED/ACTIVE/COMPLETED → TERMINATED）。 */
    @Transactional
    public void voidContract(String companyCode, Long id, String reason) {
        CtrContract exist = getById(companyCode, id);
        if (!isVoidable(exist.getStatus())) {
            throw new BusinessException("仅已通过/履约中/已完成合同可作废");
        }
        CtrContract update = new CtrContract();
        update.setId(id);
        update.setStatus(CtrContract.STATUS_TERMINATED);
        update.setRemark((exist.getRemark() == null ? "" : exist.getRemark() + ";")
                + "作废原因：" + (reason == null ? "未说明" : reason));
        contractMapper.updateById(update);
        log.info("作废合同：id={} no={} reason={}", id, exist.getContractNo(), reason);
    }

    /** 提交审批：DRAFT → APPROVING + 发起 Flowable 流程（幂等，重复提交被拒）。 */
    @Transactional
    public void submit(String companyCode, Long id, ContractSubmitDTO dto) {
        CtrContract exist = getById(companyCode, id);
        if (!CtrContract.STATUS_DRAFT.equals(exist.getStatus())) {
            throw new BusinessException("仅草拟状态合同可提交审批（当前：" + exist.getStatus() + "）");
        }
        WfProcessInstance instance = workflowService.start(companyCode, BUSINESS_TYPE_CONTRACT,
                id, dto.getApprover(), dto.getComment());
        CtrContract update = new CtrContract();
        update.setId(id);
        update.setStatus(CtrContract.STATUS_APPROVING);
        update.setProcessInstanceId(instance.getId());
        contractMapper.updateById(update);
        log.info("合同提交审批：id={} no={} approver={}", id, exist.getContractNo(), dto.getApprover());
    }

    /** 审批通过回调：合同生效（APPROVED）并自动生成收付款计划（幂等）。 */
    @Transactional
    public void approveTask(String taskId, String comment) {
        WfProcessInstance instance = workflowService.approve(taskId, comment);
        CtrContract contract = contractMapper.selectOne(new LambdaQueryWrapper<CtrContract>()
                .eq(CtrContract::getCompanyCode, instance.getCompanyCode())
                .eq(CtrContract::getId, instance.getBusinessId())
                .last("LIMIT 1"));
        if (contract == null) {
            throw new BusinessException("合同不存在，无法完成审批回调");
        }
        if (CtrContract.STATUS_APPROVING.equals(contract.getStatus())) {
            CtrContract update = new CtrContract();
            update.setId(contract.getId());
            update.setStatus(CtrContract.STATUS_APPROVED);
            contractMapper.updateById(update);
            generatePaymentPlans(contract);
        }
        log.info("合同审批通过：id={} no={}", contract.getId(), contract.getContractNo());
    }

    /** 审批驳回回调：合同回 DRAFT 可修改重提（显式清空流程关联）。 */
    @Transactional
    public void rejectTask(String taskId, String comment) {
        WfProcessInstance instance = workflowService.reject(taskId, comment);
        CtrContract contract = contractMapper.selectOne(new LambdaQueryWrapper<CtrContract>()
                .eq(CtrContract::getCompanyCode, instance.getCompanyCode())
                .eq(CtrContract::getId, instance.getBusinessId())
                .last("LIMIT 1"));
        if (contract == null) {
            throw new BusinessException("合同不存在，无法完成审批回调");
        }
        contractMapper.update(null, new LambdaUpdateWrapper<CtrContract>()
                .eq(CtrContract::getId, contract.getId())
                .set(CtrContract::getStatus, CtrContract.STATUS_DRAFT)
                .set(CtrContract::getProcessInstanceId, null));
        log.info("合同审批驳回：id={} no={}", contract.getId(), contract.getContractNo());
    }

    /** 收付款计划列表（5.3 进度视图基础）。 */
    public List<CtrPaymentPlan> listPlans(String companyCode, Long contractId) {
        getById(companyCode, contractId);
        return paymentPlanMapper.selectList(new LambdaQueryWrapper<CtrPaymentPlan>()
                .eq(CtrPaymentPlan::getCompanyCode, companyCode)
                .eq(CtrPaymentPlan::getContractId, contractId)
                .orderByAsc(CtrPaymentPlan::getPlanNo));
    }

    /** 到期窗口内生效合同（W4 到期提醒：end_date 在 [from,to] 且非终止/草拟）。 */
    public List<CtrContract> listExpiringContracts(String companyCode, LocalDate from, LocalDate to) {
        return contractMapper.selectList(new LambdaQueryWrapper<CtrContract>()
                .eq(CtrContract::getCompanyCode, companyCode)
                .in(CtrContract::getStatus, CtrContract.STATUS_APPROVED,
                        CtrContract.STATUS_ACTIVE, CtrContract.STATUS_COMPLETED)
                .between(CtrContract::getEndDate, from, to));
    }

    // ==================== 内部实现 ====================

    private CtrContract doCreate(String companyCode, ContractDTO dto) {
        CtrContract contract = new CtrContract();
        contract.setCompanyCode(companyCode);
        contract.setContractNo(nextContractNo(companyCode));
        contract.setContractName(dto.getContractName());
        contract.setCounterparty(dto.getCounterparty());
        contract.setContractType(dto.getContractType());
        contract.setAmount(dto.getAmount());
        contract.setSignDate(dto.getSignDate());
        contract.setStartDate(dto.getStartDate());
        contract.setEndDate(dto.getEndDate());
        contract.setStatus(CtrContract.STATUS_DRAFT);
        contract.setRemark(dto.getRemark());
        contractMapper.insert(contract);
        return contract;
    }

    /** 合同编号：HT-{年份}-{4 位序列}（公司内唯一，uq_ctr_contract_no 兜底；查询含逻辑删记录防复用冲突）。 */
    private String nextContractNo(String companyCode) {
        String prefix = "HT-" + Year.now().getValue() + "-";
        String latest = contractMapper.selectLatestContractNo(companyCode, prefix + "%");
        int seq = 1;
        if (latest != null) {
            seq = Integer.parseInt(latest.substring(latest.lastIndexOf('-') + 1)) + 1;
        }
        return prefix + String.format("%04d", seq);
    }

    /** 合同生效自动生成收付款计划（一期：全额一期；幂等：已生成过则不重复）。 */
    private void generatePaymentPlans(CtrContract contract) {
        Long exists = paymentPlanMapper.selectCount(new LambdaQueryWrapper<CtrPaymentPlan>()
                .eq(CtrPaymentPlan::getCompanyCode, contract.getCompanyCode())
                .eq(CtrPaymentPlan::getContractId, contract.getId()));
        if (exists != null && exists > 0) {
            log.info("合同计划已存在，跳过生成：contractId={}", contract.getId());
            return;
        }
        CtrPaymentPlan plan = new CtrPaymentPlan();
        plan.setCompanyCode(contract.getCompanyCode());
        plan.setContractId(contract.getId());
        plan.setPlanType(planTypeOf(contract.getContractType()));
        plan.setPlanNo("PLAN-" + contract.getContractNo() + "-01");
        plan.setPlanDate(contract.getStartDate() != null ? contract.getStartDate() : LocalDate.now());
        plan.setAmount(contract.getAmount());
        plan.setPaidAmount(BigDecimal.ZERO);
        plan.setStatus(CtrPaymentPlan.PLAN_STATUS_UNPAID);
        plan.setReminderSent(0);
        paymentPlanMapper.insert(plan);
        log.info("生成合同收付款计划：contractId={} planNo={} amount={}",
                contract.getId(), plan.getPlanNo(), plan.getAmount());
    }

    /** 合同类型 → 计划方向：销售收、采购付、其他付。 */
    private String planTypeOf(String contractType) {
        if (CtrContract.TYPE_SALES.equals(contractType)) {
            return CtrPaymentPlan.PLAN_TYPE_RECEIPT;
        }
        return CtrPaymentPlan.PLAN_TYPE_PAYMENT;
    }

    private boolean isVoidable(String status) {
        return CtrContract.STATUS_APPROVED.equals(status)
                || CtrContract.STATUS_ACTIVE.equals(status)
                || CtrContract.STATUS_COMPLETED.equals(status);
    }
}
