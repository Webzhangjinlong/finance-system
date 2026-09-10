package com.finance.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.finance.domain.FinPeriod;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会计期间 Mapper（fin_period）。
 */
@Mapper
public interface FinPeriodMapper extends BaseMapper<FinPeriod> {
}
