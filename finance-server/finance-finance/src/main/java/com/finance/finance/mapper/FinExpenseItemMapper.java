package com.finance.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.finance.domain.FinExpenseItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 费用报销单明细 Mapper（F6）。
 */
@Mapper
public interface FinExpenseItemMapper extends BaseMapper<FinExpenseItem> {
}
