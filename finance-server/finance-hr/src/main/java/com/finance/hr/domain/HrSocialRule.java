package com.finance.hr.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.finance.common.core.domain.BaseEntity;

import java.math.BigDecimal;

/**
 * 社保公积金规则（hr_social_rule，company+type+生效月 唯一 uq_hr_social_rule）。
 *
 * <p>费率/基数上下限配置化落表（硬约束 17 禁魔法值）：换政策只改表不改代码。
 * 险种类型：PENSION 养老 / MEDICAL 医疗 / UNEMPLOYMENT 失业 / INJURY 工伤 / MATERNITY 生育 / HOUSING_FUND 公积金。</p>
 */
@TableName("hr_social_rule")
public class HrSocialRule extends BaseEntity {

    public static final String TYPE_PENSION = "PENSION";
    public static final String TYPE_MEDICAL = "MEDICAL";
    public static final String TYPE_UNEMPLOYMENT = "UNEMPLOYMENT";
    public static final String TYPE_INJURY = "INJURY";
    public static final String TYPE_MATERNITY = "MATERNITY";
    public static final String TYPE_HOUSING_FUND = "HOUSING_FUND";

    private String companyCode;
    private String socialType;
    private BigDecimal baseFloor;
    private BigDecimal baseCeiling;
    private BigDecimal personalRate;
    private BigDecimal companyRate;
    /** 生效月份（'YYYY-MM'）。 */
    private String effectiveMonth;

    /** 险种名称（非表字段，展示用）。 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String typeName;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

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

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
}
