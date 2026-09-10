package com.finance.hr.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.hr.domain.HrAttendance;
import com.finance.hr.domain.HrEmployee;
import com.finance.hr.domain.HrSalary;
import com.finance.hr.domain.HrSalaryItem;
import com.finance.hr.dto.SalaryDTO;
import com.finance.hr.mapper.HrAttendanceMapper;
import com.finance.hr.mapper.HrEmployeeMapper;
import com.finance.hr.mapper.HrSalaryItemMapper;
import com.finance.hr.mapper.HrSalaryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 工资核算服务（H3）：累计预扣个税（硬约束 9）+ 状态机 DRAFT→CONFIRMED→PAID。
 *
 * <p>个税算法：累计预扣法
 * 本期应预扣预缴税额 = 累计应预扣预缴税额 − 累计已预扣预缴税额
 * 累计应预扣预缴税额 = max(累计收入 − 累计减除费用(5000×月) − 累计社保公积金, 0) × 预扣率 − 速算扣除数
 * 应发 = 基本 + 奖金 + 补贴 + 加班；扣款 = 社保 + 公积金 + 缺勤扣款(ABSENT 天数×日薪) + 其他；实发 = 应发 − 扣款 − 个税。</p>
 */
@Service
public class SalaryService {

    /** 月度减除费用（5000 元/月）。 */
    private static final BigDecimal MONTHLY_DEDUCTION = new BigDecimal("5000");
    /** 月计薪天数（21.75）。 */
    private static final BigDecimal MONTHLY_DAYS = new BigDecimal("21.75");

    /** 累计预扣税率表：{上限(含), 预扣率, 速算扣除数}，区间下界由上一行上限决定。 */
    private static final BigDecimal[][] TAX_BRACKETS = {
            {new BigDecimal("36000"), new BigDecimal("0.03"), new BigDecimal("0")},
            {new BigDecimal("144000"), new BigDecimal("0.10"), new BigDecimal("2520")},
            {new BigDecimal("300000"), new BigDecimal("0.20"), new BigDecimal("16920")},
            {new BigDecimal("420000"), new BigDecimal("0.25"), new BigDecimal("31920")},
            {new BigDecimal("660000"), new BigDecimal("0.30"), new BigDecimal("52920")},
            {new BigDecimal("960000"), new BigDecimal("0.35"), new BigDecimal("85920")},
            {new BigDecimal("9999999999"), new BigDecimal("0.45"), new BigDecimal("181920")}
    };

    private final HrSalaryMapper salaryMapper;
    private final HrSalaryItemMapper salaryItemMapper;
    private final HrEmployeeMapper employeeMapper;
    private final HrAttendanceMapper attendanceMapper;

    public SalaryService(HrSalaryMapper salaryMapper,
                         HrSalaryItemMapper salaryItemMapper,
                         HrEmployeeMapper employeeMapper,
                         HrAttendanceMapper attendanceMapper) {
        this.salaryMapper = salaryMapper;
        this.salaryItemMapper = salaryItemMapper;
        this.employeeMapper = employeeMapper;
        this.attendanceMapper = attendanceMapper;
    }

    /** 工资分页（年月/员工过滤，联员工姓名工号）。 */
    public PageResult<HrSalary> page(String companyCode, Integer year, Integer month,
                                     Long employeeId, long page, long size) {
        LambdaQueryWrapper<HrSalary> qw = new LambdaQueryWrapper<HrSalary>()
                .eq(HrSalary::getCompanyCode, companyCode);
        if (year != null) {
            qw.eq(HrSalary::getSalaryYear, year);
        }
        if (month != null) {
            qw.eq(HrSalary::getSalaryMonth, month);
        }
        if (employeeId != null) {
            qw.eq(HrSalary::getEmployeeId, employeeId);
        }
        qw.orderByDesc(HrSalary::getSalaryYear).orderByDesc(HrSalary::getSalaryMonth)
                .orderByDesc(HrSalary::getCreateTime);
        Page<HrSalary> p = salaryMapper.selectPage(new Page<>(page, size), qw);
        p.getRecords().forEach(s -> fillEmployee(s, companyCode));
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** 编辑草稿（仅 DRAFT 可改；基础项录入后待 calculate 重算税/实发）。 */
    @Transactional
    public void editDraft(String companyCode, Long id, SalaryDTO dto) {
        HrSalary salary = requireSalary(companyCode, id);
        if (!HrSalary.STATUS_DRAFT.equals(salary.getStatus())) {
            throw new BusinessException("仅草稿状态可编辑");
        }
        salary.setBaseSalary(zero(dto.getBaseSalary()));
        salary.setBonus(zero(dto.getBonus()));
        salary.setAllowance(zero(dto.getAllowance()));
        salary.setOvertimePay(zero(dto.getOvertimePay()));
        salary.setSocialSecurity(zero(dto.getSocialSecurity()));
        salary.setHousingFund(zero(dto.getHousingFund()));
        salary.setOtherDeduct(zero(dto.getOtherDeduct()));
        salaryMapper.updateById(salary);
    }

    /**
     * 批量核算（指定员工或全员在职）：生成/更新草稿并计算个税与实发。
     *
     * <p>已 CONFIRMED/PAID 的行跳过（状态机只进不退）；缺勤扣款从考勤 ABSENT 取数。</p>
     */
    @Transactional
    public int calculate(String companyCode, int year, int month, Long employeeId) {
        LambdaQueryWrapper<HrEmployee> eq = new LambdaQueryWrapper<HrEmployee>()
                .eq(HrEmployee::getCompanyCode, companyCode)
                .eq(HrEmployee::getStatus, HrEmployee.STATUS_ONBOARD);
        if (employeeId != null) {
            eq.eq(HrEmployee::getId, employeeId);
        }
        List<HrEmployee> employees = employeeMapper.selectList(eq);
        int count = 0;
        for (HrEmployee emp : employees) {
            HrSalary salary = salaryMapper.selectOne(new LambdaQueryWrapper<HrSalary>()
                    .eq(HrSalary::getCompanyCode, companyCode)
                    .eq(HrSalary::getEmployeeId, emp.getId())
                    .eq(HrSalary::getSalaryYear, year)
                    .eq(HrSalary::getSalaryMonth, month));
            if (salary != null && !HrSalary.STATUS_DRAFT.equals(salary.getStatus())) {
                continue; // 已复核/发放禁重算
            }
            if (salary == null) {
                salary = new HrSalary();
                salary.setCompanyCode(companyCode);
                salary.setEmployeeId(emp.getId());
                salary.setSalaryYear(year);
                salary.setSalaryMonth(month);
                salary.setBaseSalary(BigDecimal.ZERO);
                salary.setBonus(BigDecimal.ZERO);
                salary.setAllowance(BigDecimal.ZERO);
                salary.setOvertimePay(BigDecimal.ZERO);
                salary.setSocialSecurity(BigDecimal.ZERO);
                salary.setHousingFund(BigDecimal.ZERO);
                salary.setOtherDeduct(BigDecimal.ZERO);
                salary.setStatus(HrSalary.STATUS_DRAFT);
            }
            BigDecimal absentDeduct = absentDeduction(companyCode, emp, year, month);
            salary.setOtherDeduct(salary.getOtherDeduct().add(absentDeduct));
            BigDecimal tax = cumulativeWithholdingTax(companyCode, emp, salary, year, month);
            salary.setTax(tax);
            BigDecimal gross = salary.getBaseSalary().add(salary.getBonus())
                    .add(salary.getAllowance()).add(salary.getOvertimePay());
            BigDecimal net = gross.subtract(salary.getSocialSecurity())
                    .subtract(salary.getHousingFund())
                    .subtract(salary.getOtherDeduct())
                    .subtract(tax);
            salary.setNetPay(net.max(BigDecimal.ZERO));
            if (salary.getId() == null) {
                salaryMapper.insert(salary);
            } else {
                salaryMapper.updateById(salary);
            }
            persistItems(companyCode, salary, emp, absentDeduct, gross);
            count++;
        }
        return count;
    }

    /** 提交复核（DRAFT→CONFIRMED）。 */
    @Transactional
    public void submit(String companyCode, Long id) {
        HrSalary salary = requireSalary(companyCode, id);
        if (!HrSalary.STATUS_DRAFT.equals(salary.getStatus())) {
            throw new BusinessException("仅草稿可提交复核");
        }
        salary.setStatus(HrSalary.STATUS_CONFIRMED);
        salaryMapper.updateById(salary);
    }

    /** 财务复核发放（CONFIRMED→PAID，记录发放时间；发放后禁改）。 */
    @Transactional
    public void approve(String companyCode, Long id) {
        HrSalary salary = requireSalary(companyCode, id);
        if (!HrSalary.STATUS_CONFIRMED.equals(salary.getStatus())) {
            throw new BusinessException("仅已复核可发放");
        }
        salary.setStatus(HrSalary.STATUS_PAID);
        salary.setPaidAt(OffsetDateTime.now());
        salaryMapper.updateById(salary);
    }

    /** 工资条（仅本人可见：员工档案 user_id == 当前用户；admin 演示放行）。 */
    public HrSalary slip(String companyCode, Long salaryId, Long currentUserId, String currentUsername) {
        HrSalary salary = requireSalary(companyCode, salaryId);
        HrEmployee emp = employeeMapper.selectById(salary.getEmployeeId());
        boolean isOwner = emp != null && emp.getUserId() != null && emp.getUserId().equals(currentUserId);
        boolean isAdmin = "admin".equals(currentUsername);
        if (!isOwner && !isAdmin) {
            throw new BusinessException("工资条仅本人可见");
        }
        salary.setItems(salaryItemMapper.selectList(new LambdaQueryWrapper<HrSalaryItem>()
                .eq(HrSalaryItem::getSalaryId, salaryId)
                .orderByAsc(HrSalaryItem::getItemType)));
        fillEmployee(salary, companyCode);
        return salary;
    }

    // ==================== 累计预扣个税 ====================

    /**
     * 累计预扣法：
     * 累计应纳税所得额 = 本年 1..month 累计应税收入 − 5000×month − 累计社保公积金（个人）
     * 累计应预扣预缴税额 = 累计应纳税所得额 × 预扣率 − 速算扣除数（阶梯）
     * 本期个税 = 累计应预扣预缴税额 − 本年 1..month-1 已缴个税。
     */
    BigDecimal cumulativeWithholdingTax(String companyCode, HrEmployee emp,
                                        HrSalary current, int year, int month) {
        BigDecimal cumulativeTaxableIncome = BigDecimal.ZERO;
        BigDecimal cumulativePaidTax = BigDecimal.ZERO;
        BigDecimal cumulativeSocial = BigDecimal.ZERO;
        for (int m = 1; m <= month; m++) {
            HrSalary s;
            if (m == month) {
                s = current; // 当月以当前草稿为准（尚未落库或核算中）
            } else {
                s = salaryMapper.selectOne(new LambdaQueryWrapper<HrSalary>()
                        .eq(HrSalary::getCompanyCode, companyCode)
                        .eq(HrSalary::getEmployeeId, emp.getId())
                        .eq(HrSalary::getSalaryYear, year)
                        .eq(HrSalary::getSalaryMonth, m));
            }
            if (s == null) {
                continue;
            }
            BigDecimal gross = zero(s.getBaseSalary()).add(zero(s.getBonus()))
                    .add(zero(s.getAllowance())).add(zero(s.getOvertimePay()))
                    .subtract(zero(s.getOtherDeduct())); // 缺勤等扣款先减收入额，再算税（收入口径）
            cumulativeTaxableIncome = cumulativeTaxableIncome.add(gross);
            cumulativeSocial = cumulativeSocial
                    .add(zero(s.getSocialSecurity())).add(zero(s.getHousingFund()));
            if (m < month) {
                cumulativePaidTax = cumulativePaidTax.add(zero(s.getTax()));
            }
        }
        BigDecimal deduction = MONTHLY_DEDUCTION.multiply(BigDecimal.valueOf(month)).add(cumulativeSocial);
        BigDecimal taxable = cumulativeTaxableIncome.subtract(deduction).max(BigDecimal.ZERO);
        BigDecimal cumulativeTax = taxByBracket(taxable);
        return cumulativeTax.subtract(cumulativePaidTax).max(BigDecimal.ZERO);
    }

    /** 阶梯税率：累计应纳税所得额 × 预扣率 − 速算扣除数。 */
    BigDecimal taxByBracket(BigDecimal taxable) {
        for (BigDecimal[] bracket : TAX_BRACKETS) {
            if (taxable.compareTo(bracket[0]) <= 0) {
                return taxable.multiply(bracket[1]).subtract(bracket[2])
                        .setScale(2, RoundingMode.HALF_UP).max(BigDecimal.ZERO);
            }
        }
        return BigDecimal.ZERO;
    }

    /** 缺勤扣款：当月 ABSENT 天数 × 日薪（基本/21.75），并入其他扣款。 */
    private BigDecimal absentDeduction(String companyCode, HrEmployee emp, int year, int month) {
        Long absentDays = attendanceMapper.selectCount(new LambdaQueryWrapper<HrAttendance>()
                .eq(HrAttendance::getCompanyCode, companyCode)
                .eq(HrAttendance::getEmployeeId, emp.getId())
                .eq(HrAttendance::getStatus, HrAttendance.STATUS_ABSENT)
                .apply("EXTRACT(YEAR FROM work_date) = {0} AND EXTRACT(MONTH FROM work_date) = {1}", year, month));
        if (absentDays == null || absentDays == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(absentDays)
                .multiply(zero(empBaseSalary(companyCode, emp, year, month)))
                .divide(MONTHLY_DAYS, 2, RoundingMode.HALF_UP);
    }

    /** 缺勤扣款取基本工资：优先当月草稿，无则按本月核算值 0 处理（核算时点取 salary 行）。 */
    private BigDecimal empBaseSalary(String companyCode, HrEmployee emp, int year, int month) {
        HrSalary s = salaryMapper.selectOne(new LambdaQueryWrapper<HrSalary>()
                .eq(HrSalary::getCompanyCode, companyCode)
                .eq(HrSalary::getEmployeeId, emp.getId())
                .eq(HrSalary::getSalaryYear, year)
                .eq(HrSalary::getSalaryMonth, month));
        return s == null ? BigDecimal.ZERO : zero(s.getBaseSalary());
    }

    /** 生成工资明细（考勤缺勤扣款 DEDUCTION + 收入汇总 EARNING）。 */
    private void persistItems(String companyCode, HrSalary salary, HrEmployee emp,
                              BigDecimal absentDeduct, BigDecimal gross) {
        salaryItemMapper.delete(new LambdaQueryWrapper<HrSalaryItem>()
                .eq(HrSalaryItem::getSalaryId, salary.getId()));
        if (absentDeduct.signum() > 0) {
            HrSalaryItem item = new HrSalaryItem();
            item.setCompanyCode(companyCode);
            item.setSalaryId(salary.getId());
            item.setItemCode("ABSENT_DEDUCT");
            item.setItemName("缺勤扣款");
            item.setAmount(absentDeduct);
            item.setItemType(HrSalaryItem.TYPE_DEDUCTION);
            salaryItemMapper.insert(item);
        }
        if (gross.signum() > 0) {
            HrSalaryItem item = new HrSalaryItem();
            item.setCompanyCode(companyCode);
            item.setSalaryId(salary.getId());
            item.setItemCode("GROSS_EARN");
            item.setItemName("应发合计");
            item.setAmount(gross);
            item.setItemType(HrSalaryItem.TYPE_EARNING);
            salaryItemMapper.insert(item);
        }
    }

    private void fillEmployee(HrSalary salary, String companyCode) {
        HrEmployee emp = employeeMapper.selectById(salary.getEmployeeId());
        if (emp != null) {
            salary.setEmpName(emp.getEmpName());
            salary.setEmpNo(emp.getEmpNo());
        }
    }

    private HrSalary requireSalary(String companyCode, Long id) {
        HrSalary salary = salaryMapper.selectById(id);
        if (salary == null || !companyCode.equals(salary.getCompanyCode())) {
            throw new BusinessException("工资单不存在");
        }
        return salary;
    }

    private BigDecimal zero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
