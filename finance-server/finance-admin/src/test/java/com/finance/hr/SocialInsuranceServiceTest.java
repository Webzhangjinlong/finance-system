package com.finance.hr;

import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.hr.domain.HrEmployee;
import com.finance.hr.domain.HrSalary;
import com.finance.hr.domain.HrSocialDetail;
import com.finance.hr.domain.HrSocialRule;
import com.finance.hr.dto.EmployeeDTO;
import com.finance.hr.dto.SalaryDTO;
import com.finance.hr.dto.SocialRuleDTO;
import com.finance.hr.service.HrService;
import com.finance.hr.service.SalaryService;
import com.finance.hr.service.SocialInsuranceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 五险一金智能核算测试（批次 A）：比例计算/基数上下限 clamp/个人单位分项/
 * 规则 upsert 幂等/物理删/工资核算自动接入。
 *
 * <p>@BeforeEach 重建 DEMO 种子规则（与 V20 一致：社保 10.5%、公积金 5%，
 * 养老/医疗/失业/工伤/生育下限 4800 上限 24000，公积金下限 2000 上限 30000），
 * 保证任意测试顺序下规则表状态已知。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class SocialInsuranceServiceTest {

    private static final String COMPANY = "DEMO";

    @Autowired
    private SocialInsuranceService socialInsuranceService;
    @Autowired
    private HrService hrService;
    @Autowired
    private SalaryService salaryService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long deptId;
    private Long empId;

    @BeforeEach
    void clean() {
        jdbcTemplate.execute("DELETE FROM hr_social_detail");
        jdbcTemplate.execute("DELETE FROM hr_salary_item");
        jdbcTemplate.execute("DELETE FROM hr_salary");
        jdbcTemplate.execute("DELETE FROM hr_attendance");
        jdbcTemplate.execute("DELETE FROM hr_employee");
        jdbcTemplate.execute("DELETE FROM hr_department");
        jdbcTemplate.execute("DELETE FROM hr_social_rule");
        seedRules();
        deptId = hrService.createDepartment(COMPANY, dept("财务部"));
        empId = hrService.createEmployee(COMPANY, emp("S001", "社保测试员", deptId));
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM hr_social_detail");
        jdbcTemplate.execute("DELETE FROM hr_salary_item");
        jdbcTemplate.execute("DELETE FROM hr_salary");
        jdbcTemplate.execute("DELETE FROM hr_attendance");
        jdbcTemplate.execute("DELETE FROM hr_employee");
        jdbcTemplate.execute("DELETE FROM hr_department");
        jdbcTemplate.execute("DELETE FROM hr_social_rule");
    }

    // ==================== 核心计算 ====================

    @Test
    void calculate_personalAndCompanyByRate() {
        jdbcTemplate.update("UPDATE hr_employee SET social_base = 10000 WHERE id = ?", empId);
        socialInsuranceService.calculate(COMPANY, 2026, 9, empId);
        HrSocialDetail pension = detail(empId, HrSocialRule.TYPE_PENSION);
        // 养老：10000 × 8% = 800；10000 × 16% = 1600
        assertThat(pension.getBaseAmount()).isEqualByComparingTo("10000.00");
        assertThat(pension.getPersonalAmount()).isEqualByComparingTo("800.00");
        assertThat(pension.getCompanyAmount()).isEqualByComparingTo("1600.00");
    }

    @Test
    void calculate_clampToFloor_whenBelow() {
        // 申报 3000 < 下限 4800 → 按下限 4800 缴
        jdbcTemplate.update("UPDATE hr_employee SET social_base = 3000 WHERE id = ?", empId);
        socialInsuranceService.calculate(COMPANY, 2026, 9, empId);
        HrSocialDetail pension = detail(empId, HrSocialRule.TYPE_PENSION);
        assertThat(pension.getBaseAmount()).isEqualByComparingTo("4800.00");
        assertThat(pension.getPersonalAmount()).isEqualByComparingTo("384.00");
    }

    @Test
    void calculate_clampToCeiling_whenAbove() {
        // 申报 30000 > 上限 24000 → 按上限 24000 缴
        jdbcTemplate.update("UPDATE hr_employee SET social_base = 30000 WHERE id = ?", empId);
        socialInsuranceService.calculate(COMPANY, 2026, 9, empId);
        HrSocialDetail pension = detail(empId, HrSocialRule.TYPE_PENSION);
        assertThat(pension.getBaseAmount()).isEqualByComparingTo("24000.00");
    }

    @Test
    void calculate_injuryPersonalZero_companyCharged() {
        jdbcTemplate.update("UPDATE hr_employee SET social_base = 4800 WHERE id = ?", empId);
        socialInsuranceService.calculate(COMPANY, 2026, 9, empId);
        HrSocialDetail injury = detail(empId, HrSocialRule.TYPE_INJURY);
        // 工伤个人 0、单位 0.4%
        assertThat(injury.getPersonalAmount()).isEqualByComparingTo("0.00");
        assertThat(injury.getCompanyAmount()).isEqualByComparingTo("19.20"); // 4800 × 4‰
    }

    @Test
    void calculate_sixTypes_generated() {
        jdbcTemplate.update("UPDATE hr_employee SET social_base = 10000 WHERE id = ?", empId);
        socialInsuranceService.calculate(COMPANY, 2026, 9, empId);
        PageResult<HrSocialDetail> page = socialInsuranceService.pageDetails(COMPANY, empId, 2026, 9, 1, 20);
        assertThat(page.getTotal()).isEqualTo(6);
    }

    // ==================== 规则维护 ====================

    @Test
    void saveRule_upsertSameTypeMonth() {
        SocialRuleDTO dto = rule(HrSocialRule.TYPE_MEDICAL, "3000", "20000", "0.0300", "0.0900", "2026-10");
        Long id = socialInsuranceService.saveRule(COMPANY, dto);
        Long id2 = socialInsuranceService.saveRule(COMPANY, dto); // 同 company+type+month → 更新
        assertThat(id2).isEqualTo(id);
        PageResult<HrSocialRule> page = socialInsuranceService.pageRules(COMPANY, HrSocialRule.TYPE_MEDICAL, 1, 10);
        assertThat(page.getTotal()).isEqualTo(2); // 种子 2026-09 + 新增 2026-10
    }

    @Test
    void saveRule_ceilingBelowFloor_rejected() {
        SocialRuleDTO dto = rule(HrSocialRule.TYPE_PENSION, "20000", "10000", "0.08", "0.16", "2026-10");
        assertThatThrownBy(() -> socialInsuranceService.saveRule(COMPANY, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("上限不能小于下限");
    }

    @Test
    void deleteRule_physicallyRemoved() {
        PageResult<HrSocialRule> before = socialInsuranceService.pageRules(COMPANY, HrSocialRule.TYPE_PENSION, 1, 10);
        Long id = before.getRecords().get(0).getId();
        socialInsuranceService.deleteRule(COMPANY, id);
        PageResult<HrSocialRule> after = socialInsuranceService.pageRules(COMPANY, HrSocialRule.TYPE_PENSION, 1, 10);
        assertThat(after.getTotal()).isEqualTo(0);
    }

    @Test
    void calculate_missingRules_throws() {
        // 2026-01 无规则（种子仅 2026-09）→ 智能核算应显式失败而非静默
        assertThatThrownBy(() -> socialInsuranceService.calculate(COMPANY, 2026, 1, empId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无社保公积金规则");
    }

    // ==================== 工资核算自动接入 ====================

    @Test
    void salaryCalculate_autoFillsSocialAndHousing() {
        // 编辑草稿填基本工资 20000（未申报基数 → 用基本工资作基数）
        seedSalary(2026, 9, "20000.00");
        SalaryDTO dto = new SalaryDTO();
        dto.setBaseSalary(new BigDecimal("20000.00"));
        salaryService.editDraft(COMPANY, salaryOf(2026, 9).getId(), dto);
        salaryService.calculate(COMPANY, 2026, 9, empId);
        HrSalary s = salaryOf(2026, 9);
        // 社保个人 = 20000×10.5% = 2100；公积金个人 = 20000×5% = 1000
        assertThat(s.getSocialSecurity()).isEqualByComparingTo("2100.00");
        assertThat(s.getHousingFund()).isEqualByComparingTo("1000.00");
        // 明细已生成
        assertThat(detail(empId, HrSocialRule.TYPE_PENSION).getPersonalAmount()).isEqualByComparingTo("1600.00");
        assertThat(detail(empId, HrSocialRule.TYPE_HOUSING_FUND).getPersonalAmount()).isEqualByComparingTo("1000.00");
    }

    // ==================== 工具 ====================

    private void seedRules() {
        jdbcTemplate.execute("INSERT INTO hr_social_rule (id, company_code, social_type, base_floor, base_ceiling,"
                + " personal_rate, company_rate, effective_month, deleted) VALUES "
                + "(900001, 'DEMO', 'PENSION', 4800, 24000, 0.0800, 0.1600, '2026-09', 0),"
                + "(900002, 'DEMO', 'MEDICAL', 4800, 24000, 0.0200, 0.0800, '2026-09', 0),"
                + "(900003, 'DEMO', 'UNEMPLOYMENT', 4800, 24000, 0.0050, 0.0050, '2026-09', 0),"
                + "(900004, 'DEMO', 'INJURY', 4800, 24000, 0.0000, 0.0040, '2026-09', 0),"
                + "(900005, 'DEMO', 'MATERNITY', 4800, 24000, 0.0000, 0.0080, '2026-09', 0),"
                + "(900006, 'DEMO', 'HOUSING_FUND', 2000, 30000, 0.0500, 0.0500, '2026-09', 0)");
    }

    private SocialRuleDTO rule(String type, String floor, String ceiling,
                               String personal, String company, String month) {
        SocialRuleDTO dto = new SocialRuleDTO();
        dto.setSocialType(type);
        dto.setBaseFloor(new BigDecimal(floor));
        dto.setBaseCeiling(new BigDecimal(ceiling));
        dto.setPersonalRate(new BigDecimal(personal));
        dto.setCompanyRate(new BigDecimal(company));
        dto.setEffectiveMonth(month);
        return dto;
    }

    private HrSocialDetail detail(Long employeeId, String socialType) {
        PageResult<HrSocialDetail> page = socialInsuranceService.pageDetails(COMPANY, employeeId, 2026, 9, 1, 20);
        return page.getRecords().stream()
                .filter(d -> socialType.equals(d.getSocialType()))
                .findFirst().orElseThrow(() -> new AssertionError("明细不存在: " + socialType));
    }

    private void seedSalary(int year, int month, String base) {
        jdbcTemplate.update("INSERT INTO hr_salary (id, company_code, employee_id, salary_year, salary_month,"
                + " base_salary, bonus, allowance, overtime_pay, social_security, housing_fund, tax, other_deduct,"
                + " net_pay, status, deleted) VALUES ((SELECT COALESCE(MAX(id), 0) + 1 FROM hr_salary), ?, ?, ?, ?,"
                + " ?, 0, 0, 0, 0, 0, 0, 0, 0, 'DRAFT', 0)",
                COMPANY, empId, year, month, new BigDecimal(base));
    }

    private HrSalary salaryOf(int year, int month) {
        PageResult<HrSalary> page = salaryService.page(COMPANY, year, month, empId, 1, 10);
        assertThat(page.getTotal()).isEqualTo(1);
        return page.getRecords().get(0);
    }

    private com.finance.hr.dto.DepartmentDTO dept(String name) {
        com.finance.hr.dto.DepartmentDTO dto = new com.finance.hr.dto.DepartmentDTO();
        dto.setDeptName(name);
        dto.setStatus("ACTIVE");
        return dto;
    }

    private EmployeeDTO emp(String empNo, String name, Long deptId) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmpNo(empNo);
        dto.setEmpName(name);
        dto.setDeptId(deptId);
        dto.setHireDate(LocalDate.parse("2026-01-01"));
        dto.setStatus(HrEmployee.STATUS_ONBOARD);
        return dto;
    }
}
