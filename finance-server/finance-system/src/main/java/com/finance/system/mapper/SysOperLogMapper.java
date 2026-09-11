package com.finance.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.system.domain.SysOperLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper（S4）：只增不删，无删除接口。
 */
@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {
}
