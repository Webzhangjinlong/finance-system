package com.finance.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.finance.domain.FinVoucherEntry;
import org.apache.ibatis.annotations.Mapper;

/**
 * 凭证分录 Mapper（fin_voucher_entry）。
 */
@Mapper
public interface FinVoucherEntryMapper extends BaseMapper<FinVoucherEntry> {
}
