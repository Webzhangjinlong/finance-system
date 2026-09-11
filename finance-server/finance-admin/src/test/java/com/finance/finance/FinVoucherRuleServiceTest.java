package com.finance.finance;

import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.finance.domain.FinVoucherRule;
import com.finance.finance.service.FinVoucherRuleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 凭证映射规则 CRUD 测试（F8）：公司隔离、rule_code 唯一、方向/科目/比例校验、
 * 启停用、逻辑删除。
 */
@SpringBootTest
@ActiveProfiles("test")
class FinVoucherRuleServiceTest {

    @Autowired
    private FinVoucherRuleService ruleService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        // 只清本测试自建规则（TEST_ 前缀），保留 V17 种子规则（EXPENSE_PAID_CREDIT）
        jdbcTemplate.execute("DELETE FROM fin_voucher_rule WHERE rule_code LIKE 'TEST_%'");
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM fin_voucher_rule WHERE rule_code LIKE 'TEST_%'");
    }

    private FinVoucherRule build(String code, String direction) {
        FinVoucherRule r = new FinVoucherRule();
        r.setRuleCode(code);
        r.setRuleName("测试规则 " + code);
        r.setSourceType("EXPENSE");
        r.setEventType("PAID");
        r.setDirection(direction);
        r.setSubjectCode("1002");
        r.setSummaryTemplate("打款 {claimNo}");
        r.setAmountRatio(BigDecimal.ONE);
        r.setEnabled(FinVoucherRule.STATUS_ACTIVE);
        return r;
    }

    @Test
    void add_success_thenPage() {
        ruleService.add("DEMO", build("TEST_ADD", FinVoucherRule.DIR_CREDIT));
        PageResult<FinVoucherRule> page = ruleService.page("DEMO", null, null, "测试规则 TEST_ADD", 1, 10);
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getRecords().get(0).getRuleCode()).isEqualTo("TEST_ADD");
        assertThat(page.getRecords().get(0).getCompanyCode()).isEqualTo("DEMO");
    }

    @Test
    void add_duplicateRuleCode_rejected() {
        ruleService.add("DEMO", build("TEST_DUP", FinVoucherRule.DIR_CREDIT));
        assertThatThrownBy(() -> ruleService.add("DEMO", build("TEST_DUP", FinVoucherRule.DIR_DEBIT)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    void add_invalidDirection_rejected() {
        FinVoucherRule r = build("TEST_DIR", "BOTH");
        assertThatThrownBy(() -> ruleService.add("DEMO", r))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("方向不合法");
    }

    @Test
    void add_subjectNotExist_rejected() {
        FinVoucherRule r = build("TEST_SUB", FinVoucherRule.DIR_CREDIT);
        r.setSubjectCode("9999");
        assertThatThrownBy(() -> ruleService.add("DEMO", r))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("映射科目不存在");
    }

    @Test
    void switchStatus_and_delete() {
        ruleService.add("DEMO", build("TEST_ST", FinVoucherRule.DIR_CREDIT));
        PageResult<FinVoucherRule> page = ruleService.page("DEMO", null, null, null, 1, 10);
        FinVoucherRule created = page.getRecords().stream()
                .filter(r -> "TEST_ST".equals(r.getRuleCode())).findFirst().orElseThrow();

        ruleService.switchStatus("DEMO", created.getId(), FinVoucherRule.STATUS_DISABLED);
        PageResult<FinVoucherRule> afterDisable =
                ruleService.page("DEMO", null, FinVoucherRule.STATUS_DISABLED, null, 1, 10);
        assertThat(afterDisable.getRecords()).anyMatch(r -> "TEST_ST".equals(r.getRuleCode()));

        ruleService.delete("DEMO", created.getId());
        PageResult<FinVoucherRule> afterDelete = ruleService.page("DEMO", null, null, "TEST_ST", 1, 10);
        assertThat(afterDelete.getTotal()).isZero();
    }

    @Test
    void update_changesName_keepsCode() {
        ruleService.add("DEMO", build("TEST_UP", FinVoucherRule.DIR_CREDIT));
        PageResult<FinVoucherRule> page = ruleService.page("DEMO", null, null, "TEST_UP", 1, 10);
        FinVoucherRule created = page.getRecords().get(0);

        FinVoucherRule upd = build("SHOULD_IGNORE", FinVoucherRule.DIR_CREDIT);
        upd.setRuleName("更新后的规则名");
        ruleService.update("DEMO", created.getId(), upd);

        FinVoucherRule after = ruleService.page("DEMO", null, null, "更新后的规则名", 1, 10)
                .getRecords().get(0);
        assertThat(after.getRuleCode()).isEqualTo("TEST_UP");
        assertThat(after.getRuleName()).isEqualTo("更新后的规则名");
    }
}
