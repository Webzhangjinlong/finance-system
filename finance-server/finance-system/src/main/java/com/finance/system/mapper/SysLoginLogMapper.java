package com.finance.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.system.domain.SysLoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志 Mapper（sys_login_log）。
 */
@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {
}
