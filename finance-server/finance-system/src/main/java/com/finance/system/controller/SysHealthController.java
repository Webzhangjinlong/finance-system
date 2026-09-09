package com.finance.system.controller;

import com.finance.common.core.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统健康检查（骨架探活接口）。
 */
@RestController
@RequestMapping("/system")
public class SysHealthController {

    @GetMapping("/health")
    public Result<String> health() {
        return Result.ok("pong");
    }
}
