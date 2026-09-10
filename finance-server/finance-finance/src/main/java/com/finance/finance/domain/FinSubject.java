package com.finance.finance.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

import java.util.List;

/**
 * 会计科目实体（fin_subject）。
 */
@TableName("fin_subject")
public class FinSubject extends BaseEntity {

    private String companyCode;
    private String subjectCode;
    private String subjectName;
    private String subjectType;
    private String direction;
    private Long parentId;
    private Integer subjectLevel;
    private Integer isLeaf;
    private String status;

    /** 树形子节点（非表字段）。 */
    @TableField(exist = false)
    private List<FinSubject> children;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getSubjectLevel() {
        return subjectLevel;
    }

    public void setSubjectLevel(Integer subjectLevel) {
        this.subjectLevel = subjectLevel;
    }

    public Integer getIsLeaf() {
        return isLeaf;
    }

    public void setIsLeaf(Integer isLeaf) {
        this.isLeaf = isLeaf;
    }

    public List<FinSubject> getChildren() {
        return children;
    }

    public void setChildren(List<FinSubject> children) {
        this.children = children;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
