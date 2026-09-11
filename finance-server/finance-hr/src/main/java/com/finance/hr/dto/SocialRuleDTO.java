package com.finance.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * 社保公积金规则入参（批次 A：政策配置化）。
 */
public class SocialRuleDTO {

    @NotBlank(message = "险种类型不能为空")
    @Pattern(regexp = "PENSION|MEDICAL|UNEMPLOYMENT|INJURY|MATERNITY|HOUSING_FUND",
            message = "险种类型不合法")
    private String socialType;

    @NotNull(message = "基数下限不能为空")
    @PositiveOrZero(message = "基数下限不能为负")
    private BigDecimal baseFloor;

    @NotNull(message = "基数上限不能为空")
    @PositiveOrZero(message = "基数上限不能为负")
    private BigDecimal baseCeiling;

    @NotNull(message = "个人比例不能为空")
    @PositiveOrZero(message = "个人比例不能为负")
    private BigDecimal personalRate;

    @NotNull(message = "单位比例不能为空")
    @PositiveOrZero(message = "单位比例不能为负")
    private BigDecimal companyRate;

    @NotBlank(message = "生效月份不能为空")
    @Pattern(regexp = "^[0-9]{4}-[0-9]{2}$", message = "生效月份格式 YYYY-MM")
    private String effectiveMonth;

    public String getSocialType() {
        return socialType;
    }

    public void setSocialType(String socialType) {
        this.socialType = socialType;
    }

    public BigDecimal getBaseFloor() {
        return baseFloor;
    }

    public void setBaseFloor(BigDecimal baseFloor) {
        this.baseFloor = baseFloor;
    }

    public BigDecimal getBaseCeiling() {
        return baseCeiling;
    }

    public void setBaseCeiling(BigDecimal baseCeiling) {
        this.baseCeiling = baseCeiling;
    }

    public BigDecimal getPersonalRate() {
        return personalRate;
    }

    public void setPersonalRate(BigDecimal personalRate) {
        this.personalRate = personalRate;
    }

    public BigDecimal getCompanyRate() {
        return companyRate;
    }

    public void setCompanyRate(BigDecimal companyRate) {
        this.companyRate = companyRate;
    }

    public String getEffectiveMonth() {
        return effectiveMonth;
    }

    public void setEffectiveMonth(String effectiveMonth) {
        this.effectiveMonth = effectiveMonth;
    }
}
