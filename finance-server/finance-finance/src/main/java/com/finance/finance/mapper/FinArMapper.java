package com.finance.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.finance.domain.FinAr;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 应收账款 Mapper（fin_ar）。
 */
@Mapper
public interface FinArMapper extends BaseMapper<FinAr> {

    /** 查询公司内指定前缀的最大应收单号（含逻辑删除，编号唯一约束作用于全表）。 */
    @Select("SELECT ar_no FROM fin_ar WHERE company_code = #{companyCode} "
            + "AND ar_no LIKE #{prefix} ORDER BY ar_no DESC LIMIT 1")
    String selectLatestArNo(@Param("companyCode") String companyCode, @Param("prefix") String prefix);
}
