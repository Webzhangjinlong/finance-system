package com.finance.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.finance.domain.FinVoucherRule;
import com.finance.finance.mapper.FinVoucherRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 凭证映射规则服务（F8）：规则 CRUD（公司隔离）+ 启停用 + 引擎取数。
 *
 * <p>校验：rule_code 公司内唯一、direction 枚举、subject_code 必须存在于科目表、
 * amount_ratio &gt; 0、禁用已有规则可随时启停（不物理删，审计友好）。</p>
 */
@Service
public class FinVoucherRuleService {

    private final FinVoucherRuleMapper ruleMapper;
    private final SubjectService subjectService;

    public FinVoucherRuleService(FinVoucherRuleMapper ruleMapper, SubjectService subjectService) {
        this.ruleMapper = ruleMapper;
        this.subjectService = subjectService;
    }

    /** 分页列表（公司隔离 + 来源类型/启停状态/关键词过滤）。 */
    public PageResult<FinVoucherRule> page(String companyCode, String sourceType, String enabled,
                                           String keyword, long page, long size) {
        LambdaQueryWrapper<FinVoucherRule> qw = new LambdaQueryWrapper<FinVoucherRule>()
                .eq(FinVoucherRule::getCompanyCode, companyCode)
                .eq(sourceType != null && !sourceType.isBlank(), FinVoucherRule::getSourceType, sourceType)
                .eq(enabled != null && !enabled.isBlank(), FinVoucherRule::getEnabled, enabled)
                .like(keyword != null && !keyword.isBlank(), FinVoucherRule::getRuleName, keyword)
                .orderByAsc(FinVoucherRule::getSourceType)
                .orderByAsc(FinVoucherRule::getDirection);
        Page<FinVoucherRule> p = ruleMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** 全部启用规则（引擎取数，不分页）。 */
    public List<FinVoucherRule> listActive(String companyCode, String sourceType) {
        return ruleMapper.selectList(new LambdaQueryWrapper<FinVoucherRule>()
                .eq(FinVoucherRule::getCompanyCode, companyCode)
                .eq(FinVoucherRule::getSourceType, sourceType)
                .eq(FinVoucherRule::getEnabled, FinVoucherRule.STATUS_ACTIVE));
    }

    /** 新增规则。 */
    @Transactional
    public void add(String companyCode, FinVoucherRule rule) {
        rule.setCompanyCode(companyCode);
        validate(rule);
        if (ruleMapper.selectCount(new LambdaQueryWrapper<FinVoucherRule>()
                .eq(FinVoucherRule::getCompanyCode, companyCode)
                .eq(FinVoucherRule::getRuleCode, rule.getRuleCode())) > 0) {
            throw new BusinessException("规则编码在该公司已存在：" + rule.getRuleCode());
        }
        if (rule.getAmountRatio() == null) {
            rule.setAmountRatio(java.math.BigDecimal.ONE);
        }
        if (rule.getEnabled() == null) {
            rule.setEnabled(FinVoucherRule.STATUS_ACTIVE);
        }
        ruleMapper.insert(rule);
    }

    /** 修改规则（编码禁止修改）。 */
    @Transactional
    public void update(String companyCode, Long id, FinVoucherRule rule) {
        FinVoucherRule exist = getById(companyCode, id);
        rule.setId(id);
        rule.setCompanyCode(companyCode);
        rule.setRuleCode(exist.getRuleCode());
        validate(rule);
        ruleMapper.updateById(rule);
    }

    /** 启停用（ACTIVE/DISABLED）。 */
    @Transactional
    public void switchStatus(String companyCode, Long id, String enabled) {
        FinVoucherRule exist = getById(companyCode, id);
        if (!FinVoucherRule.STATUS_ACTIVE.equals(enabled) && !FinVoucherRule.STATUS_DISABLED.equals(enabled)) {
            throw new BusinessException("状态不合法（ACTIVE/DISABLED）");
        }
        FinVoucherRule upd = new FinVoucherRule();
        upd.setId(exist.getId());
        upd.setEnabled(enabled);
        ruleMapper.updateById(upd);
    }

    /** 删除规则（逻辑删除）。 */
    @Transactional
    public void delete(String companyCode, Long id) {
        getById(companyCode, id);
        ruleMapper.deleteById(id);
    }

    private FinVoucherRule getById(String companyCode, Long id) {
        FinVoucherRule rule = ruleMapper.selectOne(new LambdaQueryWrapper<FinVoucherRule>()
                .eq(FinVoucherRule::getCompanyCode, companyCode)
                .eq(FinVoucherRule::getId, id)
                .last("LIMIT 1"));
        if (rule == null) {
            throw new BusinessException("凭证映射规则不存在：" + id);
        }
        return rule;
    }

    private void validate(FinVoucherRule rule) {
        if (rule.getRuleCode() == null || rule.getRuleCode().isBlank()) {
            throw new BusinessException("规则编码不能为空");
        }
        if (rule.getRuleName() == null || rule.getRuleName().isBlank()) {
            throw new BusinessException("规则名称不能为空");
        }
        if (rule.getSourceType() == null || rule.getSourceType().isBlank()) {
            throw new BusinessException("来源类型不能为空");
        }
        if (!FinVoucherRule.DIR_DEBIT.equals(rule.getDirection())
                && !FinVoucherRule.DIR_CREDIT.equals(rule.getDirection())) {
            throw new BusinessException("方向不合法（DEBIT/CREDIT）");
        }
        if (rule.getSubjectCode() == null || rule.getSubjectCode().isBlank()) {
            throw new BusinessException("映射科目不能为空");
        }
        if (!subjectService.codeIndex(rule.getCompanyCode()).containsKey(rule.getSubjectCode())) {
            throw new BusinessException("映射科目不存在：" + rule.getSubjectCode());
        }
        if (rule.getAmountRatio() != null
                && rule.getAmountRatio().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException("金额比例必须大于 0");
        }
    }
}
