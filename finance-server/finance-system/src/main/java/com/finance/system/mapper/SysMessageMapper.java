package com.finance.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finance.system.domain.SysMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站内消息 Mapper（sys_message）。
 */
@Mapper
public interface SysMessageMapper extends BaseMapper<SysMessage> {
}
