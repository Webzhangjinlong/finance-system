package com.finance.system.controller;

import com.finance.common.core.Result;
import com.finance.system.domain.LoginBody;
import com.finance.system.domain.LoginUserVO;
import com.finance.system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（S1，docs 3.1）：/auth/login 公开，/auth/logout 与 /auth/profile 需登录。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 登录（公开白名单）。 */
    @PostMapping("/login")
    public Result<LoginUserVO> login(@Valid @RequestBody LoginBody body, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return Result.ok("登录成功", authService.login(body, ip));
    }

    /** 登出（需登录）。 */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    /** 当前用户信息（需登录，返回角色与权限码）。 */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public Result<LoginUserVO> profile() {
        return Result.ok(authService.profile());
    }
}
