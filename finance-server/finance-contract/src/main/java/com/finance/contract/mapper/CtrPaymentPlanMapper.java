package com.finance.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.contract.domain.CtrPaymentPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合同收付款计划 Mapper（ctr_payment_plan）。
 */
@Mapper
public interface CtrPaymentPlanMapper extends BaseMapper<CtrPaymentPlan> {
}
