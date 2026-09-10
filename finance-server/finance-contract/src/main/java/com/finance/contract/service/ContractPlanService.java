package com.finance.contract.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finance.common.core.exception.BusinessException;
import com.finance.contract.domain.CtrContract;
import com.finance.contract.domain.CtrPaymentPlan;
import com.finance.contract.mapper.CtrContractMapper;
import com.finance.contract.mapper.CtrPaymentPlanMapper;
import com.finance.finance.domain.FinAp;
import com.finance.finance.domain.FinAr;
import com.finance.finance.service.ArApService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 收付款计划联动服务（C3/W4，docs 5.3/5.4）。
 *
 * <p>业务规则：
 * <ul>
 *   <li>计划到期 → 生成应收/应付单（幂等：fin_ar/fin_ap 的 company+plan_id 唯一约束兜底）；</li>
 *   <li>收付款核销 → 回写计划已收/已付金额与状态（UNPAID/PARTIAL/PAID）；</li>
 *   <li>超额核销双重拦截：ArApService 服务校验 + DB CHECK（ck_fin_ar_received/ck_fin_ap_paid/ck_ctr_plan_paid）；</li>
 *   <li>逾期扫描（W4 定时任务）→ 未收付完的计划置 OVERDUE（承接 C3 的 OVERDUE 归属）；</li>
 *   <li>跨模块遵守 R16：计划（contract 域）与单据（finance 域）通过 ArApService 交互，不直调他人 Mapper。</li>
 * </ul></p>
 */
@Service
public class ContractPlanService {

    private static final Logger log = LoggerFactory.getLogger(ContractPlanService.class);

    private final CtrPaymentPlanMapper planMapper;
    private final CtrContractMapper contractMapper;
    private final ArApService arApService;

    public ContractPlanService(CtrPaymentPlanMapper planMapper,
                               CtrContractMapper contractMapper,
                               ArApService arApService) {
        this.planMapper = planMapper;
        this.contractMapper = contractMapper;
        this.arApService = arApService;
    }

    /** 计划同步：到期（plan_date &lt;= today）且未生成单据的计划 → 生成应收/应付单（幂等）。 */
    @Transactional
    public int syncDuePlans(String companyCode, Long contractId) {
        CtrContract contract = contractMapper.selectById(contractId);
        if (contract == null || !companyCode.equals(contract.getCompanyCode())) {
            throw new BusinessException("合同不存在");
        }
        List<CtrPaymentPlan> duePlans = planMapper.selectList(new LambdaQueryWrapper<CtrPaymentPlan>()
                .eq(CtrPaymentPlan::getCompanyCode, companyCode)
                .eq(CtrPaymentPlan::getContractId, contractId)
                .in(CtrPaymentPlan::getStatus, CtrPaymentPlan.PLAN_STATUS_UNPAID,
                        CtrPaymentPlan.PLAN_STATUS_OVERDUE, CtrPaymentPlan.PLAN_STATUS_PARTIAL)
                .le(CtrPaymentPlan::getPlanDate, LocalDate.now()));
        int generated = 0;
        for (CtrPaymentPlan plan : duePlans) {
            boolean isReceipt = CtrPaymentPlan.PLAN_TYPE_RECEIPT.equals(plan.getPlanType());
            if (isReceipt) {
                if (arApService.existsArByPlan(companyCode, plan.getId())) {
                    continue;
                }
                arApService.generateArFromPlan(companyCode, plan.getId(), contractId,
                        plan.getAmount(), plan.getPlanDate(), contract.getCounterparty());
                generated++;
            } else {
                if (arApService.existsApByPlan(companyCode, plan.getId())) {
                    continue;
                }
                arApService.generateApFromPlan(companyCode, plan.getId(), contractId,
                        plan.getAmount(), plan.getPlanDate(), contract.getCounterparty());
                generated++;
            }
        }
        log.info("计划同步完成：company={} contractId={} 到期计划={} 已生成单据={}",
                companyCode, contractId, duePlans.size(), generated);
        return generated;
    }

    /** 收款核销：回写应收单 + 计划已收金额与状态（超额拦截）。 */
    @Transactional
    public CtrPaymentPlan registerReceipt(String companyCode, Long planId, BigDecimal amount,
                                          LocalDate receiptDate, String remark) {
        CtrPaymentPlan plan = getPlan(companyCode, planId);
        if (!CtrPaymentPlan.PLAN_TYPE_RECEIPT.equals(plan.getPlanType())) {
            throw new BusinessException("该计划为付款计划，请走付款核销");
        }
        if (plan.getPaidAmount().add(amount).compareTo(plan.getAmount()) > 0) {
            throw new BusinessException("收款金额超过计划余额，禁止超额核销（计划 " + plan.getAmount()
                    + "，已收 " + plan.getPaidAmount() + "，本次 " + amount + "）");
        }
        FinAr ar = arApService.generateArFromPlan(companyCode, plan.getId(), plan.getContractId(),
                plan.getAmount(), plan.getPlanDate(), counterpartyOf(companyCode, plan.getContractId()));
        arApService.applyReceipt(companyCode, ar.getId(), amount);
        return updatePlanAfterSettle(plan, amount, remark);
    }

    /** 付款核销：回写应付单 + 计划已付金额与状态（超额拦截）。 */
    @Transactional
    public CtrPaymentPlan registerPayment(String companyCode, Long planId, BigDecimal amount,
                                          LocalDate paymentDate, String remark) {
        CtrPaymentPlan plan = getPlan(companyCode, planId);
        if (!CtrPaymentPlan.PLAN_TYPE_PAYMENT.equals(plan.getPlanType())) {
            throw new BusinessException("该计划为收款计划，请走收款核销");
        }
        if (plan.getPaidAmount().add(amount).compareTo(plan.getAmount()) > 0) {
            throw new BusinessException("付款金额超过计划余额，禁止超额核销（计划 " + plan.getAmount()
                    + "，已付 " + plan.getPaidAmount() + "，本次 " + amount + "）");
        }
        FinAp ap = arApService.generateApFromPlan(companyCode, plan.getId(), plan.getContractId(),
                plan.getAmount(), plan.getPlanDate(), counterpartyOf(companyCode, plan.getContractId()));
        arApService.applyPayment(companyCode, ap.getId(), amount);
        return updatePlanAfterSettle(plan, amount, remark);
    }

    /** 计划列表（进度视图数据：amount/paid_amount/status 供计算已收付比例）。 */
    public List<CtrPaymentPlan> listPlans(String companyCode, Long contractId) {
        return planMapper.selectList(new LambdaQueryWrapper<CtrPaymentPlan>()
                .eq(CtrPaymentPlan::getCompanyCode, companyCode)
                .eq(CtrPaymentPlan::getContractId, contractId)
                .orderByAsc(CtrPaymentPlan::getPlanNo));
    }

    /** 到期窗口内未清计划（W4 到期提醒：plan_date 在 [from,to] 且未收付完）。 */
    public List<CtrPaymentPlan> listDuePlans(String companyCode, LocalDate from, LocalDate to) {
        return planMapper.selectList(new LambdaQueryWrapper<CtrPaymentPlan>()
                .eq(CtrPaymentPlan::getCompanyCode, companyCode)
                .in(CtrPaymentPlan::getStatus, CtrPaymentPlan.PLAN_STATUS_UNPAID,
                        CtrPaymentPlan.PLAN_STATUS_PARTIAL, CtrPaymentPlan.PLAN_STATUS_OVERDUE)
                .between(CtrPaymentPlan::getPlanDate, from, to));
    }

    /** 已逾期未清计划（W4 逾期扫描：plan_date &lt; before 且未收付完）。 */
    public List<CtrPaymentPlan> listOverduePlans(String companyCode, LocalDate before) {
        return planMapper.selectList(new LambdaQueryWrapper<CtrPaymentPlan>()
                .eq(CtrPaymentPlan::getCompanyCode, companyCode)
                .in(CtrPaymentPlan::getStatus, CtrPaymentPlan.PLAN_STATUS_UNPAID,
                        CtrPaymentPlan.PLAN_STATUS_PARTIAL)
                .lt(CtrPaymentPlan::getPlanDate, before));
    }

    /** 标记计划逾期（OVERDUE，仅未收付完时生效，幂等）。 */
    @Transactional
    public void markOverdue(String companyCode, Long planId) {
        CtrPaymentPlan plan = getPlan(companyCode, planId);
        if (CtrPaymentPlan.PLAN_STATUS_PAID.equals(plan.getStatus())) {
            return;
        }
        if (!CtrPaymentPlan.PLAN_STATUS_OVERDUE.equals(plan.getStatus())) {
            CtrPaymentPlan update = new CtrPaymentPlan();
            update.setId(planId);
            update.setStatus(CtrPaymentPlan.PLAN_STATUS_OVERDUE);
            planMapper.updateById(update);
            log.info("计划标记逾期：planId={} planNo={}", planId, plan.getPlanNo());
        }
    }

    // ==================== 内部实现 ====================

    private CtrPaymentPlan getPlan(String companyCode, Long planId) {
        CtrPaymentPlan plan = planMapper.selectById(planId);
        if (plan == null || !companyCode.equals(plan.getCompanyCode())) {
            throw new BusinessException("收付款计划不存在");
        }
        return plan;
    }

    private String counterpartyOf(String companyCode, Long contractId) {
        CtrContract contract = contractMapper.selectById(contractId);
        if (contract == null || !companyCode.equals(contract.getCompanyCode())) {
            throw new BusinessException("合同不存在");
        }
        return contract.getCounterparty();
    }

    /** 核销后回写计划：已收/已付金额 + 状态（全额 PAID / 部分 PARTIAL；OVERDUE 由到期提醒任务标记）。 */
    private CtrPaymentPlan updatePlanAfterSettle(CtrPaymentPlan plan, BigDecimal amount, String remark) {
        BigDecimal paid = plan.getPaidAmount().add(amount);
        String status = paid.compareTo(plan.getAmount()) >= 0
                ? CtrPaymentPlan.PLAN_STATUS_PAID
                : CtrPaymentPlan.PLAN_STATUS_PARTIAL;
        CtrPaymentPlan update = new CtrPaymentPlan();
        update.setId(plan.getId());
        update.setPaidAmount(paid);
        update.setStatus(status);
        planMapper.updateById(update);
        log.info("计划核销回写：planId={} paidAmount={} status={} remark={}",
                plan.getId(), paid, status, remark);
        plan.setPaidAmount(paid);
        plan.setStatus(status);
        return plan;
    }
}
