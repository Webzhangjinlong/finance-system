package com.finance.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.contract.domain.CtrContract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 合同 Mapper（ctr_contract）。
 */
@Mapper
public interface CtrContractMapper extends BaseMapper<CtrContract> {

    /**
     * 查询公司内指定前缀的最大合同编号（含逻辑删除记录）。
     *
     * <p>uq_ctr_contract_no 唯一约束作用于全表（含逻辑删记录），
     * MP 内置查询自动过滤 deleted=0 会导致编号误复用而撞唯一约束，
     * 故编号分配必须绕过逻辑删除过滤。</p>
     */
    @Select("SELECT contract_no FROM ctr_contract WHERE company_code = #{companyCode} "
            + "AND contract_no LIKE #{prefix} ORDER BY contract_no DESC LIMIT 1")
    String selectLatestContractNo(@Param("companyCode") String companyCode,
                                  @Param("prefix") String prefix);
}
