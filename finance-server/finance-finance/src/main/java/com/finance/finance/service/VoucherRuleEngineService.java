package com.finance.finance.service;

import com.finance.finance.domain.FinVoucherEntry;
import com.finance.finance.domain.FinVoucherRule;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 凭证映射引擎（F8）：按 来源类型+事件 匹配启用规则 → 生成借贷分录（草稿素材）。
 *
 * <p>规则语义：direction=CREDIT → 生成贷方分录（科目=subject_code，金额=业务金额×ratio，
 * 摘要=模板渲染）；direction=DEBIT 同理。摘要模板支持 {key} 占位符（如 {claimNo}）。
 * 幂等由 VoucherService.create 的 source_type+source_id 唯一约束兜底（硬约束 7）。</p>
 */
@Service
public class VoucherRuleEngineService {

    private final FinVoucherRuleService ruleService;

    public VoucherRuleEngineService(FinVoucherRuleService ruleService) {
        this.ruleService = ruleService;
    }

    /**
     * 解析规则分录。
     *
     * @param companyCode 公司编码
     * @param sourceType  业务来源类型（EXPENSE...）
     * @param eventType   触发事件（PAID...，可空）
     * @param amount      业务金额
     * @param ctx         摘要模板占位值（claimNo/contractNo 等）
     * @return 规则分录列表；无匹配规则返回空列表（调用方回退默认映射）
     */
    public List<FinVoucherEntry> resolve(String companyCode, String sourceType, String eventType,
                                         BigDecimal amount, Map<String, Object> ctx) {
        List<FinVoucherRule> rules = ruleService.listActive(companyCode, sourceType);
        List<FinVoucherEntry> entries = new ArrayList<>();
        if (rules == null) {
            return entries;
        }
        for (FinVoucherRule rule : rules) {
            if (rule.getEventType() != null && !rule.getEventType().isBlank()
                    && !rule.getEventType().equals(eventType)) {
                continue;
            }
            BigDecimal ratio = rule.getAmountRatio() == null ? BigDecimal.ONE : rule.getAmountRatio();
            BigDecimal amt = amount.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            FinVoucherEntry e = new FinVoucherEntry();
            e.setSubjectCode(rule.getSubjectCode());
            e.setSummary(render(rule.getSummaryTemplate(), ctx));
            if (FinVoucherRule.DIR_DEBIT.equals(rule.getDirection())) {
                e.setDebitAmount(amt);
            } else {
                e.setCreditAmount(amt);
            }
            entries.add(e);
        }
        return entries;
    }

    /** 渲染摘要模板：{key} 占位替换为 ctx 值；无模板返回 null（由调用方兜底摘要）。 */
    private String render(String template, Map<String, Object> ctx) {
        if (template == null || template.isBlank()) {
            return null;
        }
        String out = template;
        if (ctx != null) {
            for (Map.Entry<String, Object> e : ctx.entrySet()) {
                out = out.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
            }
        }
        return out;
    }
}
