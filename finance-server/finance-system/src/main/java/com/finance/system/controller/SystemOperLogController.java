package com.finance.system.controller;

import com.finance.common.core.Result;
import com.finance.common.core.domain.PageResult;
import com.finance.system.domain.SysOperLog;
import com.finance.system.service.SysOperLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志查询接口（S4，docs 5.5）：GET /system/oper-log，权限 system:log:list。
 *
 * <p>只增不删：不提供删除/清空接口（审计完整性，硬约束 17）。</p>
 */
@RestController
@RequestMapping("/system/oper-log")
public class SystemOperLogController {

    private final SysOperLogService operLogService;

    public SystemOperLogController(SysOperLogService operLogService) {
        this.operLogService = operLogService;
    }

    /** 分页查询（支持 标题/操作人/状态/时间范围 过滤）。 */
    @GetMapping
    @PreAuthorize("hasAuthority('system:log:list')")
    public Result<PageResult<SysOperLog>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String operName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime) {
        return Result.ok(operLogService.page(title, operName, status, beginTime, endTime, page, size));
    }
}
