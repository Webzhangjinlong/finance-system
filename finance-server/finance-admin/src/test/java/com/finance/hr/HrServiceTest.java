package com.finance.hr;

import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.hr.domain.HrAttendance;
import com.finance.hr.domain.HrDepartment;
import com.finance.hr.domain.HrEmployee;
import com.finance.hr.domain.HrSalary;
import com.finance.hr.dto.AttendanceDTO;
import com.finance.hr.dto.DepartmentDTO;
import com.finance.hr.dto.EmployeeDTO;
import com.finance.hr.dto.SalaryDTO;
import com.finance.hr.service.HrService;
import com.finance.hr.service.SalaryService;
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
 * 人事域集成测试（H1-H3，Gate 15）：部门/员工档案、考勤、工资核算与累计预扣个税。
 *
 * <p>个税核心场景：单月低档、连续两月累计递增、跨档（10% 速算 2520）、
 * 缺勤扣款取数、已复核禁重算、状态机、工资条本人可见。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class HrServiceTest {

    private static final String COMPANY = "DEMO";

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
        jdbcTemplate.execute("DELETE FROM hr_salary_item");
        jdbcTemplate.execute("DELETE FROM hr_salary");
        jdbcTemplate.execute("DELETE FROM hr_attendance");
        jdbcTemplate.execute("DELETE FROM hr_employee");
        jdbcTemplate.execute("DELETE FROM hr_department");
        deptId = hrService.createDepartment(COMPANY, dept("财务部"));
        empId = hrService.createEmployee(COMPANY, emp("H001", "张三", deptId));
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM hr_salary_item");
        jdbcTemplate.execute("DELETE FROM hr_salary");
        jdbcTemplate.execute("DELETE FROM hr_attendance");
        jdbcTemplate.execute("DELETE FROM hr_employee");
        jdbcTemplate.execute("DELETE FROM hr_department");
    }

    // ==================== H1 部门/员工 ====================

    @Test
    void departmentTree_buildsHierarchy() {
        Long subId = hrService.createDepartment(COMPANY, subDept("财务核算组", deptId));
        var tree = hrService.departmentTree(COMPANY);
        assertThat(tree).extracting(HrDepartment::getId).contains(deptId);
        HrDepartment root = tree.stream().filter(d -> d.getId().equals(deptId)).findFirst().orElseThrow();
        assertThat(root.getChildren()).extracting(HrDepartment::getId).contains(subId);
    }

    @Test
    void deleteDepartment_withEmployees_rejected() {
        assertThatThrownBy(() -> hrService.deleteDepartment(COMPANY, deptId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("存在员工");
    }

    @Test
    void createEmployee_empNoUnique_rejected() {
        assertThatThrownBy(() -> hrService.createEmployee(COMPANY, emp("H001", "李四", deptId)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("工号已存在");
    }

    @Test
    void updateEmployeeStatus_leaveKeepsRecord() {
        hrService.updateEmployeeStatus(COMPANY, empId, HrEmployee.STATUS_LEAVE);
        HrEmployee saved = hrService.employeeDetail(COMPANY, empId);
        assertThat(saved.getStatus()).isEqualTo(HrEmployee.STATUS_LEAVE);
        assertThat(saved.getEmpNo()).isEqualTo("H001"); // 历史数据保留
    }

    @Test
    void employeePage_keywordFilter() {
        PageResult<HrEmployee> page = hrService.employeePage(COMPANY, "张", null, null, 1, 10);
        assertThat(page.getRecords()).extracting(HrEmployee::getId).contains(empId);
    }

    // ==================== H2 考勤 ====================

    @Test
    void upsertAttendance_sameDateReplaces() {
        attendance("2026-09-01", HrAttendance.STATUS_NORMAL);
        attendance("2026-09-01", HrAttendance.STATUS_ABSENT); // 同日覆盖
        PageResult<HrAttendance> page = hrService.attendancePage(COMPANY, empId, 2026, 9, 1, 10);
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getRecords().get(0).getStatus()).isEqualTo(HrAttendance.STATUS_ABSENT);
    }

    // ==================== H3 工资 / 累计预扣个税 ====================

    @Test
    void calculate_singleMonth_lowBracket() {
        seedSalary(2026, 1, "20000.00", "0", "0", "0", "2000.00", "0");
        int n = salaryService.calculate(COMPANY, 2026, 1, empId);
        assertThat(n).isEqualTo(1);
        HrSalary s = salaryOf(2026, 1);
        // 累计应纳税所得额 = 20000 - 5000 - 2000 = 13000 → 3% → 390
        assertThat(s.getTax()).isEqualByComparingTo("390.00");
        // 实发 = 20000 - 2000(社保) - 390 = 17610
        assertThat(s.getNetPay()).isEqualByComparingTo("17610.00");
        assertThat(s.getStatus()).isEqualTo(HrSalary.STATUS_DRAFT);
    }

    @Test
    void calculate_secondMonth_cumulativeIncrease() {
        seedSalary(2026, 1, "20000.00", "0", "0", "0", "2000.00", "0");
        seedSalary(2026, 2, "20000.00", "0", "0", "0", "2000.00", "0");
        salaryService.calculate(COMPANY, 2026, 1, empId);
        salaryService.calculate(COMPANY, 2026, 2, empId);
        // 1 月：13000×3% = 390
        // 2 月累计：40000-10000-4000 = 26000 → 780；本期 = 780-390 = 390
        assertThat(salaryOf(2026, 1).getTax()).isEqualByComparingTo("390.00");
        assertThat(salaryOf(2026, 2).getTax()).isEqualByComparingTo("390.00");
    }

    @Test
    void calculate_taxBracketCrossover_10Percent() {
        seedSalary(2026, 1, "50000.00", "0", "0", "0", "5000.00", "0");
        seedSalary(2026, 2, "50000.00", "0", "0", "0", "5000.00", "0");
        salaryService.calculate(COMPANY, 2026, 1, empId);
        salaryService.calculate(COMPANY, 2026, 2, empId);
        // 1 月：50000-5000-5000 = 40000 → 40000×10%-2520 = 1480
        // 2 月累计：100000-10000-10000 = 80000 → 80000×10%-2520 = 5480；本期 = 5480-1480 = 4000
        assertThat(salaryOf(2026, 1).getTax()).isEqualByComparingTo("1480.00");
        assertThat(salaryOf(2026, 2).getTax()).isEqualByComparingTo("4000.00");
    }

    @Test
    void calculate_absentDays_deducted() {
        seedSalary(2026, 1, "21750.00", "0", "0", "0", "0", "0");
        attendance("2026-01-05", HrAttendance.STATUS_ABSENT);
        attendance("2026-01-06", HrAttendance.STATUS_ABSENT);
        salaryService.calculate(COMPANY, 2026, 1, empId);
        HrSalary s = salaryOf(2026, 1);
        // 缺勤扣款 = 2 × (21750/21.75) = 2 × 1000 = 2000；应税 = 21750-5000-2000 = 14750 → 3% = 442.50
        assertThat(s.getOtherDeduct()).isEqualByComparingTo("2000.00");
        assertThat(s.getTax()).isEqualByComparingTo("442.50");
        // 实发 = 21750 - 2000 - 442.50 = 19307.50
        assertThat(s.getNetPay()).isEqualByComparingTo("19307.50");
    }

    @Test
    void calculate_skipsConfirmed() {
        seedSalary(2026, 1, "20000.00", "0", "0", "0", "2000.00", "0");
        salaryService.calculate(COMPANY, 2026, 1, empId);
        salaryService.submit(COMPANY, salaryOf(2026, 1).getId());
        // 复核后重算：不得覆盖状态/金额
        salaryService.calculate(COMPANY, 2026, 1, empId);
        assertThat(salaryOf(2026, 1).getStatus()).isEqualTo(HrSalary.STATUS_CONFIRMED);
        assertThat(salaryOf(2026, 1).getTax()).isEqualByComparingTo("390.00");
    }

    @Test
    void submitApprove_statusFlow_paidBlocksEdit() {
        seedSalary(2026, 1, "20000.00", "0", "0", "0", "2000.00", "0");
        salaryService.calculate(COMPANY, 2026, 1, empId);
        Long id = salaryOf(2026, 1).getId();
        salaryService.submit(COMPANY, id);
        assertThat(salaryOf(2026, 1).getStatus()).isEqualTo(HrSalary.STATUS_CONFIRMED);
        salaryService.approve(COMPANY, id);
        HrSalary paid = salaryOf(2026, 1);
        assertThat(paid.getStatus()).isEqualTo(HrSalary.STATUS_PAID);
        assertThat(paid.getPaidAt()).isNotNull();

        SalaryDTO dto = new SalaryDTO();
        dto.setEmployeeId(empId);
        dto.setBaseSalary(new BigDecimal("30000.00"));
        assertThatThrownBy(() -> salaryService.editDraft(COMPANY, id, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅草稿");
    }

    @Test
    void slip_ownerVisible_otherRejected_adminAllowed() {
        seedSalary(2026, 1, "20000.00", "0", "0", "0", "2000.00", "0");
        salaryService.calculate(COMPANY, 2026, 1, empId);
        Long id = salaryOf(2026, 1).getId();

        // 员工本人（user_id 关联）
        jdbcTemplate.update("UPDATE hr_employee SET user_id = 777001 WHERE id = ?", empId);
        HrSalary slip = salaryService.slip(COMPANY, id, 777001L, "zhangsan");
        assertThat(slip.getEmpName()).isEqualTo("张三");
        assertThat(slip.getItems()).isNotEmpty();

        // 他人拒绝
        assertThatThrownBy(() -> salaryService.slip(COMPANY, id, 888001L, "lisi"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅本人可见");

        // admin 演示放行
        HrSalary adminSlip = salaryService.slip(COMPANY, id, 1001L, "admin");
        assertThat(adminSlip.getNetPay()).isEqualByComparingTo("17610.00");
    }

    // ==================== 工具 ====================

    private void seedSalary(int year, int month, String base, String bonus, String allowance,
                            String overtime, String social, String housing) {
        jdbcTemplate.update("INSERT INTO hr_salary (id, company_code, employee_id, salary_year, salary_month,"
                + " base_salary, bonus, allowance, overtime_pay, social_security, housing_fund, tax, other_deduct,"
                + " net_pay, status, deleted) VALUES ((SELECT COALESCE(MAX(id), 0) + 1 FROM hr_salary), ?, ?, ?, ?,"
                + " ?, ?, ?, ?, ?, ?, 0, 0, 0, 'DRAFT', 0)",
                COMPANY, empId, year, month, new BigDecimal(base), new BigDecimal(bonus),
                new BigDecimal(allowance), new BigDecimal(overtime), new BigDecimal(social), new BigDecimal(housing));
    }

    private HrSalary salaryOf(int year, int month) {
        PageResult<HrSalary> page = salaryService.page(COMPANY, year, month, empId, 1, 10);
        assertThat(page.getTotal()).isEqualTo(1);
        return page.getRecords().get(0);
    }

    private void attendance(String date, String status) {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(empId);
        dto.setWorkDate(LocalDate.parse(date));
        dto.setStatus(status);
        hrService.upsertAttendance(COMPANY, dto);
    }

    private DepartmentDTO dept(String name) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setDeptName(name);
        dto.setStatus("ACTIVE");
        return dto;
    }

    private DepartmentDTO subDept(String name, Long parentId) {
        DepartmentDTO dto = dept(name);
        dto.setParentId(parentId);
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
