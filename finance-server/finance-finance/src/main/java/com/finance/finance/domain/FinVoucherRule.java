package com.finance.finance.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

import java.math.BigDecimal;

/**
 * 凭证映射规则实体（fin_voucher_rule，F8）。
 *
 * <p>业务事件（如报销打款）→ 按 source_type + event_type 匹配规则 → 生成借贷分录：
 * direction 决定借贷方向、subject_code 决定映射科目、summary_template 渲染摘要、
 * amount_ratio 决定金额比例（借贷各自按业务金额 × ratio 计算）。</p>
 */
@TableName("fin_voucher_rule")
public class FinVoucherRule extends BaseEntity {

    public static final String DIR_DEBIT = "DEBIT";
    public static final String DIR_CREDIT = "CREDIT";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISABLED = "DISABLED";

    /** 公司编码（多账套隔离，硬约束 2）。 */
    private String companyCode;
    /** 规则编码（公司内唯一，如 EXPENSE_PAID_CREDIT）。 */
    private String ruleCode;
    private String ruleName;
    /** 业务来源类型（EXPENSE/PAYMENT/RECEIPT/CONTRACT...）。 */
    private String sourceType;
    /** 触发事件（PAID/APPROVED/BOOKED...，可空=任意事件）。 */
    private String eventType;
    /** 分录方向 DEBIT/CREDIT。 */
    private String direction;
    /** 映射科目编码（需存在于科目表，服务层校验）。 */
    private String subjectCode;
    /** 摘要模板，支持 {claimNo} 等占位符。 */
    private String summaryTemplate;
    /** 金额比例（1.00=全额）。 */
    private BigDecimal amountRatio;
    /** ACTIVE/DISABLED。 */
    private String enabled;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSummaryTemplate() {
        return summaryTemplate;
    }

    public void setSummaryTemplate(String summaryTemplate) {
        this.summaryTemplate = summaryTemplate;
    }

    public BigDecimal getAmountRatio() {
        return amountRatio;
    }

    public void setAmountRatio(BigDecimal amountRatio) {
        this.amountRatio = amountRatio;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }
}
