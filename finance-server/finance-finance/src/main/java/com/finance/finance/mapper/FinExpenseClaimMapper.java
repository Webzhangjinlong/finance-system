package com.finance.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.finance.domain.FinExpenseClaim;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 费用报销单 Mapper（F6）。
 */
@Mapper
public interface FinExpenseClaimMapper extends BaseMapper<FinExpenseClaim> {

    /** 取当前公司前缀下最新单号（含逻辑删记录防复用冲突，uk_expense_claim_no 兜底）。 */
    @Select("SELECT claim_no FROM fin_expense_claim WHERE company_code = #{companyCode} "
            + "AND claim_no LIKE #{prefix} ORDER BY claim_no DESC LIMIT 1")
    String selectLatestClaimNo(@Param("companyCode") String companyCode,
                               @Param("prefix") String prefix);
}
