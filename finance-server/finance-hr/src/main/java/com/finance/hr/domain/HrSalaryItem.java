package com.finance.hr.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

import java.math.BigDecimal;

/**
 * 工资明细项（hr_salary_item，EARNING/DEDUCTION）。
 */
@TableName("hr_salary_item")
public class HrSalaryItem extends BaseEntity {

    public static final String TYPE_EARNING = "EARNING";
    public static final String TYPE_DEDUCTION = "DEDUCTION";

    private String companyCode;
    private Long salaryId;
    private String itemCode;
    private String itemName;
    private BigDecimal amount;
    private String itemType;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public Long getSalaryId() {
        return salaryId;
    }

    public void setSalaryId(Long salaryId) {
        this.salaryId = salaryId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }
}
