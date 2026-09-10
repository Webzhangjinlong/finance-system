package com.finance.hr.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

import java.util.List;

/**
 * 部门（hr_department，公司隔离）。
 */
@TableName("hr_department")
public class HrDepartment extends BaseEntity {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISABLED = "DISABLED";

    private String companyCode;
    private String deptName;
    private Long parentId;
    private Long leaderId;
    private Integer sortOrder;
    private String status;

    /** 子部门（非表字段，树接口装配）。 */
    @TableField(exist = false)
    private List<HrDepartment> children;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(Long leaderId) {
        this.leaderId = leaderId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<HrDepartment> getChildren() {
        return children;
    }

    public void setChildren(List<HrDepartment> children) {
        this.children = children;
    }
}
