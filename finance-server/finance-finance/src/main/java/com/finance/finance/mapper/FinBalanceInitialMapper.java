package com.finance.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.finance.domain.FinBalanceInitial;
import org.apache.ibatis.annotations.Mapper;

/** 科目期初余额 Mapper（F3 账簿期初取数）。 */
@Mapper
public interface FinBalanceInitialMapper extends BaseMapper<FinBalanceInitial> {
}
