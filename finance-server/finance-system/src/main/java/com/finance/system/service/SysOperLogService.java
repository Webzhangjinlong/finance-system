package com.finance.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.common.core.domain.PageResult;
import com.finance.system.domain.SysOperLog;
import com.finance.system.mapper.SysOperLogMapper;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务（S4，docs 5.5）：查询支持 模块/操作人/状态/时间范围 过滤，分页 records/total。
 *
 * <p>只增不删：本服务仅提供分页查询，不提供任何删除/清空接口（审计完整性）。</p>
 */
@Service
public class SysOperLogService {

    private final SysOperLogMapper operLogMapper;

    public SysOperLogService(SysOperLogMapper operLogMapper) {
        this.operLogMapper = operLogMapper;
    }

    /**
     * 操作日志分页查询（可按标题/操作人/状态/时间范围过滤，按操作时间倒序）。
     */
    public PageResult<SysOperLog> page(String title, String operName, Integer status,
                                       String beginTime, String endTime,
                                       long page, long size) {
        LambdaQueryWrapper<SysOperLog> qw = new LambdaQueryWrapper<SysOperLog>()
                .like(title != null && !title.isBlank(), SysOperLog::getTitle, title)
                .like(operName != null && !operName.isBlank(), SysOperLog::getOperName, operName)
                .eq(status != null, SysOperLog::getStatus, status)
                .ge(beginTime != null && !beginTime.isBlank(), SysOperLog::getOperTime, beginTime)
                .le(endTime != null && !endTime.isBlank(), SysOperLog::getOperTime, endTime)
                .orderByDesc(SysOperLog::getOperTime);
        Page<SysOperLog> p = operLogMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }
}
