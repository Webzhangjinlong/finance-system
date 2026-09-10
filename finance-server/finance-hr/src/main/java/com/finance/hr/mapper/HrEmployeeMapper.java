package com.finance.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.hr.domain.HrEmployee;
import org.apache.ibatis.annotations.Mapper;

/**
 * HrEmployee Mapper。
 */
@Mapper
public interface HrEmployeeMapper extends BaseMapper<HrEmployee> {
}
