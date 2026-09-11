package com.finance.hr.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.hr.domain.HrEmployee;
import com.finance.hr.domain.HrSalary;
import com.finance.hr.domain.HrSocialDetail;
import com.finance.hr.domain.HrSocialRule;
import com.finance.hr.dto.SocialRuleDTO;
import com.finance.hr.mapper.HrEmployeeMapper;
import com.finance.hr.mapper.HrSalaryMapper;
import com.finance.hr.mapper.HrSocialDetailMapper;
import com.finance.hr.mapper.HrSocialRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 五险一金智能核算服务（批次 A）：政策规则配置化 + 基数自动识别 + 个人/单位金额自动计算。
 *
 * <p>规则落表（硬约束 17）：费率/基数上下限存 hr_social_rule，换政策只改表不改代码；
 * 基数 = clamp(员工申报 social_base 或当月基本工资, 下限, 上限)；
 * 个人金额 = 基数 × 个人比例；单位金额 = 基数 × 单位比例（工伤/生育个人不缴）。
 * 金额一律 BigDecimal/NUMERIC(18,2)（硬约束 1），公司隔离（硬约束 2）。</p>
 */
@Service
public class SocialInsuranceService {

    /** 险种显示名映射。 */
    private static final Map<String, String> TYPE_NAMES = new HashMap<>();

    static {
        TYPE_NAMES.put(HrSocialRule.TYPE_PENSION, "养老保险");
        TYPE_NAMES.put(HrSocialRule.TYPE_MEDICAL, "医疗保险");
        TYPE_NAMES.put(HrSocialRule.TYPE_UNEMPLOYMENT, "失业保险");
        TYPE_NAMES.put(HrSocialRule.TYPE_INJURY, "工伤保险");
        TYPE_NAMES.put(HrSocialRule.TYPE_MATERNITY, "生育保险");
        TYPE_NAMES.put(HrSocialRule.TYPE_HOUSING_FUND, "住房公积金");
    }

    private final HrSocialRuleMapper ruleMapper;
    private final HrSocialDetailMapper detailMapper;
    private final HrEmployeeMapper employeeMapper;
    private final HrSalaryMapper salaryMapper;

    public SocialInsuranceService(HrSocialRuleMapper ruleMapper,
                                  HrSocialDetailMapper detailMapper,
                                  HrEmployeeMapper employeeMapper,
                                  HrSalaryMapper salaryMapper) {
        this.ruleMapper = ruleMapper;
        this.detailMapper = detailMapper;
        this.employeeMapper = employeeMapper;
        this.salaryMapper = salaryMapper;
    }

    /** 当期是否存在规则（工资核算宽容语义：无规则不调用核算，避免事务 rollback-only）。 */
    public boolean hasRules(String companyCode, int year, int month) {
        return ruleMapper.selectCount(new LambdaQueryWrapper<HrSocialRule>()
                .eq(HrSocialRule::getCompanyCode, companyCode)
                .eq(HrSocialRule::getEffectiveMonth, String.format("%04d-%02d", year, month))) > 0;
    }

    /** 规则分页（险种筛选，附显示名）。 */
    public PageResult<HrSocialRule> pageRules(String companyCode, String socialType, long page, long size) {
        LambdaQueryWrapper<HrSocialRule> qw = new LambdaQueryWrapper<HrSocialRule>()
                .eq(HrSocialRule::getCompanyCode, companyCode);
        if (socialType != null && !socialType.isBlank()) {
            qw.eq(HrSocialRule::getSocialType, socialType);
        }
        qw.orderByDesc(HrSocialRule::getEffectiveMonth).orderByAsc(HrSocialRule::getSocialType);
        Page<HrSocialRule> p = ruleMapper.selectPage(new Page<>(page, size), qw);
        p.getRecords().forEach(r -> r.setTypeName(typeName(r.getSocialType())));
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** 保存规则（company+type+生效月 存在则更新，否则新增；幂等语义）。 */
    @Transactional
    public Long saveRule(String companyCode, SocialRuleDTO dto) {
        if (dto.getBaseCeiling().compareTo(dto.getBaseFloor()) < 0) {
            throw new BusinessException("基数上限不能小于下限");
        }
        HrSocialRule rule = ruleMapper.selectOne(new LambdaQueryWrapper<HrSocialRule>()
                .eq(HrSocialRule::getCompanyCode, companyCode)
                .eq(HrSocialRule::getSocialType, dto.getSocialType())
                .eq(HrSocialRule::getEffectiveMonth, dto.getEffectiveMonth()));
        if (rule == null) {
            rule = new HrSocialRule();
            rule.setCompanyCode(companyCode);
            rule.setSocialType(dto.getSocialType());
            rule.setEffectiveMonth(dto.getEffectiveMonth());
        }
        rule.setBaseFloor(dto.getBaseFloor());
        rule.setBaseCeiling(dto.getBaseCeiling());
        rule.setPersonalRate(dto.getPersonalRate());
        rule.setCompanyRate(dto.getCompanyRate());
        if (rule.getId() == null) {
            ruleMapper.insert(rule);
        } else {
            ruleMapper.updateById(rule);
        }
        return rule.getId();
    }

    /** 删除规则（物理删：唯一约束场景，R48 语义——逻辑删会与 uq 冲突）。 */
    @Transactional
    public void deleteRule(String companyCode, Long id) {
        HrSocialRule rule = ruleMapper.selectById(id);
        if (rule == null || !companyCode.equals(rule.getCompanyCode())) {
            throw new BusinessException("规则不存在");
        }
        ruleMapper.deleteById(id);
    }

    /**
     * 批量核算五险一金（指定员工或全员在职）：按当月规则生成/更新缴费明细。
     *
     * <p>基数识别：员工申报 social_base（&gt;0 生效）优先，否则取当月工资基本工资，再 clamp 到 [下限,上限]；
     * 无当月规则（effective_month 精确匹配）的险种跳过。</p>
     *
     * @return 写入明细条数
     */
    @Transactional
    public int calculate(String companyCode, int year, int month, Long employeeId) {
        String monthKey = String.format("%04d-%02d", year, month);
        List<HrSocialRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<HrSocialRule>()
                .eq(HrSocialRule::getCompanyCode, companyCode)
                .eq(HrSocialRule::getEffectiveMonth, monthKey));
        if (rules.isEmpty()) {
            throw new BusinessException("当前期间无社保公积金规则，请先在规则页配置（生效月 " + monthKey + "）");
        }
        LambdaQueryWrapper<HrEmployee> eq = new LambdaQueryWrapper<HrEmployee>()
                .eq(HrEmployee::getCompanyCode, companyCode)
                .eq(HrEmployee::getStatus, HrEmployee.STATUS_ONBOARD);
        if (employeeId != null) {
            eq.eq(HrEmployee::getId, employeeId);
        }
        List<HrEmployee> employees = employeeMapper.selectList(eq);
        int count = 0;
        for (HrEmployee emp : employees) {
            BigDecimal base = resolveBase(companyCode, emp, year, month);
            for (HrSocialRule rule : rules) {
                BigDecimal clamped = clamp(base, rule.getBaseFloor(), rule.getBaseCeiling());
                if (clamped.signum() <= 0) {
                    continue; // 无基数不生成明细
                }
                BigDecimal personal = clamped.multiply(zero(rule.getPersonalRate()))
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal company = clamped.multiply(zero(rule.getCompanyRate()))
                        .setScale(2, RoundingMode.HALF_UP);
                upsertDetail(companyCode, emp.getId(), year, month, rule.getSocialType(),
                        clamped, personal, company);
                count++;
            }
        }
        return count;
    }

    /** 缴费明细分页（员工/年月筛选，联员工姓名工号）。 */
    public PageResult<HrSocialDetail> pageDetails(String companyCode, Long employeeId,
                                                  Integer year, Integer month, long page, long size) {
        LambdaQueryWrapper<HrSocialDetail> qw = new LambdaQueryWrapper<HrSocialDetail>()
                .eq(HrSocialDetail::getCompanyCode, companyCode);
        if (employeeId != null) {
            qw.eq(HrSocialDetail::getEmployeeId, employeeId);
        }
        if (year != null) {
            qw.eq(HrSocialDetail::getSalaryYear, year);
        }
        if (month != null) {
            qw.eq(HrSocialDetail::getSalaryMonth, month);
        }
        qw.orderByDesc(HrSocialDetail::getSalaryYear).orderByDesc(HrSocialDetail::getSalaryMonth)
                .orderByAsc(HrSocialDetail::getEmployeeId).orderByAsc(HrSocialDetail::getSocialType);
        Page<HrSocialDetail> p = detailMapper.selectPage(new Page<>(page, size), qw);
        p.getRecords().forEach(d -> {
            HrEmployee emp = employeeMapper.selectById(d.getEmployeeId());
            if (emp != null) {
                d.setEmpName(emp.getEmpName());
                d.setEmpNo(emp.getEmpNo());
            }
            d.setTypeName(typeName(d.getSocialType()));
        });
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /**
     * 个人缴纳合计（工资核算接入）：返回 {社保个人合计, 公积金个人合计}。
     * 社保 = 养老 + 医疗 + 失业 个人；公积金 = 公积金个人。
     */
    public BigDecimal[] personalTotals(String companyCode, Long employeeId, int year, int month) {
        List<HrSocialDetail> details = detailMapper.selectList(new LambdaQueryWrapper<HrSocialDetail>()
                .eq(HrSocialDetail::getCompanyCode, companyCode)
                .eq(HrSocialDetail::getEmployeeId, employeeId)
                .eq(HrSocialDetail::getSalaryYear, year)
                .eq(HrSocialDetail::getSalaryMonth, month));
        BigDecimal social = BigDecimal.ZERO;
        BigDecimal housing = BigDecimal.ZERO;
        for (HrSocialDetail d : details) {
            if (HrSocialRule.TYPE_HOUSING_FUND.equals(d.getSocialType())) {
                housing = housing.add(zero(d.getPersonalAmount()));
            } else {
                social = social.add(zero(d.getPersonalAmount()));
            }
        }
        return new BigDecimal[]{social, housing};
    }

    // ==================== 内部 ====================

    /** 基数识别：员工申报 social_base（>0）优先，否则当月工资基本工资。 */
    private BigDecimal resolveBase(String companyCode, HrEmployee emp, int year, int month) {
        if (emp.getSocialBase() != null && emp.getSocialBase().signum() > 0) {
            return emp.getSocialBase();
        }
        HrSalary salary = salaryMapper.selectOne(new LambdaQueryWrapper<HrSalary>()
                .eq(HrSalary::getCompanyCode, companyCode)
                .eq(HrSalary::getEmployeeId, emp.getId())
                .eq(HrSalary::getSalaryYear, year)
                .eq(HrSalary::getSalaryMonth, month));
        return salary == null ? BigDecimal.ZERO : zero(salary.getBaseSalary());
    }

    private BigDecimal clamp(BigDecimal base, BigDecimal floor, BigDecimal ceiling) {
        BigDecimal v = base;
        if (floor != null && v.compareTo(floor) < 0) {
            v = floor;
        }
        if (ceiling != null && ceiling.signum() > 0 && v.compareTo(ceiling) > 0) {
            v = ceiling;
        }
        return v;
    }

    /** 幂等 upsert：company+员工+年月+险种 已存在则更新，否则新增。 */
    private void upsertDetail(String companyCode, Long employeeId, int year, int month,
                              String socialType, BigDecimal base, BigDecimal personal, BigDecimal company) {
        HrSocialDetail detail = detailMapper.selectOne(new LambdaQueryWrapper<HrSocialDetail>()
                .eq(HrSocialDetail::getCompanyCode, companyCode)
                .eq(HrSocialDetail::getEmployeeId, employeeId)
                .eq(HrSocialDetail::getSalaryYear, year)
                .eq(HrSocialDetail::getSalaryMonth, month)
                .eq(HrSocialDetail::getSocialType, socialType));
        if (detail == null) {
            detail = new HrSocialDetail();
            detail.setCompanyCode(companyCode);
            detail.setEmployeeId(employeeId);
            detail.setSalaryYear(year);
            detail.setSalaryMonth(month);
            detail.setSocialType(socialType);
        }
        detail.setBaseAmount(base);
        detail.setPersonalAmount(personal);
        detail.setCompanyAmount(company);
        if (detail.getId() == null) {
            detailMapper.insert(detail);
        } else {
            detailMapper.updateById(detail);
        }
    }

    private String typeName(String socialType) {
        return TYPE_NAMES.getOrDefault(socialType, socialType);
    }

    private BigDecimal zero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
