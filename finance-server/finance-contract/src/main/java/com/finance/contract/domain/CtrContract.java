package com.finance.contract.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 合同（ctr_contract，V1）。
 *
 * <p>状态机（硬约束：状态用枚举/常量，禁魔法值）：DRAFT 草拟 → APPROVING 审批中
 * → APPROVED 已通过（生效，自动生成收付款计划）→ ACTIVE 履约中 / COMPLETED 已完成；
 * APPROVED/ACTIVE/COMPLETED 可作废 → TERMINATED 已终止。
 * 删除保护：非 DRAFT 状态禁止物理/逻辑删除，只能作废。</p>
 */
@TableName("ctr_contract")
public class CtrContract extends BaseEntity {

    /** 合同状态：草拟。 */
    public static final String STATUS_DRAFT = "DRAFT";
    /** 合同状态：审批中。 */
    public static final String STATUS_APPROVING = "APPROVING";
    /** 合同状态：已通过（生效）。 */
    public static final String STATUS_APPROVED = "APPROVED";
    /** 合同状态：履约中。 */
    public static final String STATUS_ACTIVE = "ACTIVE";
    /** 合同状态：已完成。 */
    public static final String STATUS_COMPLETED = "COMPLETED";
    /** 合同状态：已终止（作废）。 */
    public static final String STATUS_TERMINATED = "TERMINATED";

    /** 合同类型：销售（收）。 */
    public static final String TYPE_SALES = "SALES";
    /** 合同类型：采购（付）。 */
    public static final String TYPE_PURCHASE = "PURCHASE";
    /** 合同类型：其他。 */
    public static final String TYPE_OTHER = "OTHER";

    private String companyCode;
    private String contractNo;
    private String contractName;
    private String counterparty;
    private String contractType;
    private BigDecimal amount;
    private LocalDate signDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    /** Flowable 流程实例 ID（wf_process_instance.id，预留关联）。 */
    private Long processInstanceId;
    private String remark;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getSignDate() {
        return signDate;
    }

    public void setSignDate(LocalDate signDate) {
        this.signDate = signDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
