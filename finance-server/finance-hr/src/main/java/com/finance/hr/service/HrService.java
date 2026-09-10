package com.finance.hr.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.hr.domain.HrAttendance;
import com.finance.hr.domain.HrDepartment;
import com.finance.hr.domain.HrEmployee;
import com.finance.hr.dto.AttendanceDTO;
import com.finance.hr.dto.DepartmentDTO;
import com.finance.hr.dto.EmployeeDTO;
import com.finance.hr.mapper.HrAttendanceMapper;
import com.finance.hr.mapper.HrDepartmentMapper;
import com.finance.hr.mapper.HrEmployeeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 人事主数据服务（H1 部门/员工档案 + H2 考勤）。
 *
 * <p>公司隔离（硬约束 2）；工号唯一 uq_hr_employee_no（含逻辑删查重，L37 教训）；
 * 考勤员工+日期唯一（upsert 物理清旧防 uq 残留，L41 教训）；
 * 离职停用账号但保留历史数据。</p>
 */
@Service
public class HrService {

    private final HrDepartmentMapper departmentMapper;
    private final HrEmployeeMapper employeeMapper;
    private final HrAttendanceMapper attendanceMapper;

    public HrService(HrDepartmentMapper departmentMapper,
                     HrEmployeeMapper employeeMapper,
                     HrAttendanceMapper attendanceMapper) {
        this.departmentMapper = departmentMapper;
        this.employeeMapper = employeeMapper;
        this.attendanceMapper = attendanceMapper;
    }

    // ==================== 部门 ====================

    /** 部门树（公司隔离）。 */
    public List<HrDepartment> departmentTree(String companyCode) {
        List<HrDepartment> all = departmentMapper.selectList(new LambdaQueryWrapper<HrDepartment>()
                .eq(HrDepartment::getCompanyCode, companyCode)
                .orderByAsc(HrDepartment::getSortOrder)
                .orderByAsc(HrDepartment::getId));
        Map<Long, List<HrDepartment>> byParent = all.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() == null ? 0L : d.getParentId()));
        List<HrDepartment> roots = new ArrayList<>();
        for (HrDepartment d : all) {
            if (d.getParentId() == null || d.getParentId() == 0L) {
                roots.add(d);
            }
        }
        for (HrDepartment d : all) {
            d.setChildren(byParent.getOrDefault(d.getId(), List.of()));
        }
        return roots;
    }

    @Transactional
    public Long createDepartment(String companyCode, DepartmentDTO dto) {
        if (dto.getParentId() != null && dto.getParentId() != 0L
                && departmentMapper.selectById(dto.getParentId()) == null) {
            throw new BusinessException("父部门不存在");
        }
        HrDepartment dept = new HrDepartment();
        dept.setCompanyCode(companyCode);
        dept.setDeptName(dto.getDeptName());
        dept.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        dept.setLeaderId(dto.getLeaderId());
        dept.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        dept.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : HrDepartment.STATUS_ACTIVE);
        departmentMapper.insert(dept);
        return dept.getId();
    }

    @Transactional
    public void updateDepartment(String companyCode, Long id, DepartmentDTO dto) {
        requireDepartment(companyCode, id);
        HrDepartment dept = new HrDepartment();
        dept.setId(id);
        dept.setDeptName(dto.getDeptName());
        if (dto.getParentId() != null && dto.getParentId() != 0L) {
            if (dto.getParentId().equals(id)) {
                throw new BusinessException("父部门不能是自己");
            }
            dept.setParentId(dto.getParentId());
        }
        dept.setLeaderId(dto.getLeaderId());
        dept.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        dept.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : HrDepartment.STATUS_ACTIVE);
        departmentMapper.updateById(dept);
    }

    @Transactional
    public void deleteDepartment(String companyCode, Long id) {
        requireDepartment(companyCode, id);
        if (departmentMapper.selectCount(new LambdaQueryWrapper<HrDepartment>()
                .eq(HrDepartment::getCompanyCode, companyCode)
                .eq(HrDepartment::getParentId, id)) > 0) {
            throw new BusinessException("存在子部门，不可删除");
        }
        if (employeeMapper.selectCount(new LambdaQueryWrapper<HrEmployee>()
                .eq(HrEmployee::getCompanyCode, companyCode)
                .eq(HrEmployee::getDeptId, id)) > 0) {
            throw new BusinessException("部门下存在员工，不可删除");
        }
        departmentMapper.deleteById(id);
    }

    // ==================== 员工 ====================

    /** 员工分页（关键词匹配工号/姓名，可按部门/状态过滤）。 */
    public PageResult<HrEmployee> employeePage(String companyCode, String keyword, Long deptId,
                                               String status, long page, long size) {
        LambdaQueryWrapper<HrEmployee> qw = new LambdaQueryWrapper<HrEmployee>()
                .eq(HrEmployee::getCompanyCode, companyCode);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(HrEmployee::getEmpNo, keyword).or().like(HrEmployee::getEmpName, keyword));
        }
        if (deptId != null) {
            qw.eq(HrEmployee::getDeptId, deptId);
        }
        if (StringUtils.hasText(status)) {
            qw.eq(HrEmployee::getStatus, status);
        }
        qw.orderByDesc(HrEmployee::getCreateTime);
        Page<HrEmployee> p = employeeMapper.selectPage(new Page<>(page, size), qw);
        p.getRecords().forEach(e -> {
            if (e.getDeptId() != null) {
                HrDepartment dept = departmentMapper.selectById(e.getDeptId());
                if (dept != null) {
                    e.setDeptName(dept.getDeptName());
                }
            }
        });
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    public HrEmployee employeeDetail(String companyCode, Long id) {
        return requireEmployee(companyCode, id);
    }

    /** 新建员工（工号公司内唯一，含逻辑删查重）。 */
    @Transactional
    public Long createEmployee(String companyCode, EmployeeDTO dto) {
        if (employeeMapper.selectCount(new LambdaQueryWrapper<HrEmployee>()
                .eq(HrEmployee::getCompanyCode, companyCode)
                .eq(HrEmployee::getEmpNo, dto.getEmpNo())) > 0) {
            throw new BusinessException("工号已存在");
        }
        HrEmployee emp = new HrEmployee();
        emp.setCompanyCode(companyCode);
        emp.setEmpNo(dto.getEmpNo());
        emp.setEmpName(dto.getEmpName());
        emp.setGender(dto.getGender());
        emp.setIdCard(dto.getIdCard());
        emp.setPhone(dto.getPhone());
        emp.setEmail(dto.getEmail());
        emp.setDeptId(dto.getDeptId());
        emp.setPosition(dto.getPosition());
        emp.setHireDate(dto.getHireDate());
        emp.setLeaveDate(dto.getLeaveDate());
        emp.setSalaryAccount(dto.getSalaryAccount());
        emp.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : HrEmployee.STATUS_ONBOARD);
        emp.setUserId(dto.getUserId());
        employeeMapper.insert(emp);
        return emp.getId();
    }

    /** 编辑档案（工号唯一校验排除自身）。 */
    @Transactional
    public void updateEmployee(String companyCode, Long id, EmployeeDTO dto) {
        requireEmployee(companyCode, id);
        if (employeeMapper.selectCount(new LambdaQueryWrapper<HrEmployee>()
                .eq(HrEmployee::getCompanyCode, companyCode)
                .eq(HrEmployee::getEmpNo, dto.getEmpNo())
                .ne(HrEmployee::getId, id)) > 0) {
            throw new BusinessException("工号已存在");
        }
        HrEmployee emp = new HrEmployee();
        emp.setId(id);
        emp.setEmpNo(dto.getEmpNo());
        emp.setEmpName(dto.getEmpName());
        emp.setGender(dto.getGender());
        emp.setIdCard(dto.getIdCard());
        emp.setPhone(dto.getPhone());
        emp.setEmail(dto.getEmail());
        emp.setDeptId(dto.getDeptId());
        emp.setPosition(dto.getPosition());
        emp.setHireDate(dto.getHireDate());
        emp.setLeaveDate(dto.getLeaveDate());
        emp.setSalaryAccount(dto.getSalaryAccount());
        emp.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : HrEmployee.STATUS_ONBOARD);
        emp.setUserId(dto.getUserId());
        employeeMapper.updateById(emp);
    }

    /** 离职/复职（状态变更，历史数据保留）。 */
    @Transactional
    public void updateEmployeeStatus(String companyCode, Long id, String status) {
        requireEmployee(companyCode, id);
        if (!HrEmployee.STATUS_ONBOARD.equals(status) && !HrEmployee.STATUS_LEAVE.equals(status)) {
            throw new BusinessException("非法的员工状态");
        }
        HrEmployee emp = new HrEmployee();
        emp.setId(id);
        emp.setStatus(status);
        employeeMapper.updateById(emp);
    }

    /** 删除员工（逻辑删；有考勤/工资数据仅可离职不可物理删）。 */
    @Transactional
    public void deleteEmployee(String companyCode, Long id) {
        requireEmployee(companyCode, id);
        employeeMapper.deleteById(id);
    }

    // ==================== 考勤 ====================

    /** 考勤分页（员工/月份过滤）。 */
    public PageResult<HrAttendance> attendancePage(String companyCode, Long employeeId,
                                                   Integer year, Integer month, long page, long size) {
        LambdaQueryWrapper<HrAttendance> qw = new LambdaQueryWrapper<HrAttendance>()
                .eq(HrAttendance::getCompanyCode, companyCode);
        if (employeeId != null) {
            qw.eq(HrAttendance::getEmployeeId, employeeId);
        }
        if (year != null && month != null) {
            qw.apply("EXTRACT(YEAR FROM work_date) = {0} AND EXTRACT(MONTH FROM work_date) = {1}", year, month);
        }
        qw.orderByDesc(HrAttendance::getWorkDate);
        Page<HrAttendance> p = attendanceMapper.selectPage(new Page<>(page, size), qw);
        p.getRecords().forEach(a -> {
            HrEmployee emp = employeeMapper.selectById(a.getEmployeeId());
            if (emp != null) {
                a.setEmpNo(emp.getEmpNo());
                a.setEmpName(emp.getEmpName());
            }
        });
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** 考勤录入/更新（员工+日期唯一，upsert 物理清旧防 uq 残留 L41）。 */
    @Transactional
    public void upsertAttendance(String companyCode, AttendanceDTO dto) {
        requireEmployee(companyCode, dto.getEmployeeId());
        attendanceMapper.physicalDeleteByEmployeeAndDate(companyCode, dto.getEmployeeId(), dto.getWorkDate());
        HrAttendance att = new HrAttendance();
        att.setCompanyCode(companyCode);
        att.setEmployeeId(dto.getEmployeeId());
        att.setWorkDate(dto.getWorkDate());
        att.setCheckIn(dto.getCheckIn());
        att.setCheckOut(dto.getCheckOut());
        att.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : HrAttendance.STATUS_NORMAL);
        attendanceMapper.insert(att);
    }

    // ==================== 内部 ====================

    private HrDepartment requireDepartment(String companyCode, Long id) {
        HrDepartment dept = departmentMapper.selectById(id);
        if (dept == null || !companyCode.equals(dept.getCompanyCode())) {
            throw new BusinessException("部门不存在");
        }
        return dept;
    }

    private HrEmployee requireEmployee(String companyCode, Long id) {
        HrEmployee emp = employeeMapper.selectById(id);
        if (emp == null || !companyCode.equals(emp.getCompanyCode())) {
            throw new BusinessException("员工不存在");
        }
        return emp;
    }
}
