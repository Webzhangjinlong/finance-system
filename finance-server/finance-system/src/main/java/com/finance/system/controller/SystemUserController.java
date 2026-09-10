package com.finance.system.controller;

import com.finance.common.core.Result;
import com.finance.common.core.domain.PageResult;
import com.finance.system.domain.SysUser;
import com.finance.system.dto.AssignDTO;
import com.finance.system.dto.ResetPwdDTO;
import com.finance.system.dto.UserDTO;
import com.finance.system.service.SystemUserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口（S2）：/system/user。
 *
 * <p>权限码 system:user:list/add/edit/del/reset-pwd/assign（V11 已授权 admin）；
 * 密码一律 BCrypt；返回结构统一 Result，分页统一 records/total/page/size。</p>
 */
@RestController
@RequestMapping("/system/user")
public class SystemUserController {

    private final SystemUserService userService;

    public SystemUserController(SystemUserService userService) {
        this.userService = userService;
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<PageResult<SysUser>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(userService.page(keyword, status, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<SysUser> detail(@PathVariable Long id) {
        return Result.ok(userService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    public Result<Long> create(@Valid @RequestBody UserDTO dto) {
        return Result.ok(userService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserDTO dto) {
        userService.update(id, dto);
        return Result.ok();
    }

    @PutMapping("/{id}/reset-pwd")
    @PreAuthorize("hasAuthority('system:user:reset-pwd')")
    public Result<Void> resetPwd(@PathVariable Long id, @Valid @RequestBody ResetPwdDTO dto) {
        userService.resetPassword(id, dto.getPassword());
        return Result.ok();
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:assign')")
    public Result<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody AssignDTO dto) {
        userService.assignRoles(id, dto.getIds());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:del')")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }
}
