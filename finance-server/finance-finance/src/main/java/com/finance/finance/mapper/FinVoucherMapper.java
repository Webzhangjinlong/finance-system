package com.finance.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.finance.domain.FinVoucher;
import com.finance.finance.domain.FinVoucherEntry;
import org.apache.ibatis.annotations.Mapper;

/**
 * 凭证 Mapper（fin_voucher）。
 */
@Mapper
public interface FinVoucherMapper extends BaseMapper<FinVoucher> {
}
