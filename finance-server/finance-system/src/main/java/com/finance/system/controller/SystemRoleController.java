package com.finance.system.controller;

import com.finance.common.core.Result;
import com.finance.common.core.annotation.OperLog;
import com.finance.common.core.annotation.OperType;
import com.finance.common.core.domain.PageResult;
import com.finance.system.domain.SysRole;
import com.finance.system.dto.AssignDTO;
import com.finance.system.dto.RoleDTO;
import com.finance.system.service.SystemRoleService;
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

import java.util.List;

/**
 * 角色管理接口（S2）：/system/role。
 *
 * <p>权限码 system:role:list/add/edit/del/assign（V11 已授权 admin）；/list 供用户分配角色下拉。</p>
 */
@RestController
@RequestMapping("/system/role")
public class SystemRoleController {

    private final SystemRoleService roleService;

    public SystemRoleController(SystemRoleService roleService) {
        this.roleService = roleService;
    }

    /** 全部启用角色（用户分配下拉）。 */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:user:assign') or hasAuthority('system:role:list')")
    public Result<List<SysRole>> listAll() {
        return Result.ok(roleService.listAll());
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<PageResult<SysRole>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(roleService.page(keyword, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<SysRole> detail(@PathVariable Long id) {
        return Result.ok(roleService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    public Result<Long> create(@Valid @RequestBody RoleDTO dto) {
        return Result.ok(roleService.create(dto));
    }

    @OperLog(title = "角色修改", operType = OperType.UPDATE)
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleDTO dto) {
        roleService.update(id, dto);
        return Result.ok();
    }

    @OperLog(title = "角色分配菜单", operType = OperType.GRANT)
    @PutMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:assign')")
    public Result<Void> assignMenus(@PathVariable Long id, @Valid @RequestBody AssignDTO dto) {
        roleService.assignMenus(id, dto.getIds());
        return Result.ok();
    }

    @OperLog(title = "角色删除", operType = OperType.DELETE)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:del')")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }
}
