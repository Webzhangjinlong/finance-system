package com.finance.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.finance.domain.FinAp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 应付账款 Mapper（fin_ap）。
 */
@Mapper
public interface FinApMapper extends BaseMapper<FinAp> {

    /** 查询公司内指定前缀的最大应付单号（含逻辑删除，编号唯一约束作用于全表）。 */
    @Select("SELECT ap_no FROM fin_ap WHERE company_code = #{companyCode} "
            + "AND ap_no LIKE #{prefix} ORDER BY ap_no DESC LIMIT 1")
    String selectLatestApNo(@Param("companyCode") String companyCode, @Param("prefix") String prefix);
}
