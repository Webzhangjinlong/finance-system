package com.finance.system.controller;

import com.finance.common.core.Result;
import com.finance.system.domain.SysMenu;
import com.finance.system.dto.MenuDTO;
import com.finance.system.service.SystemMenuService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单管理接口（S2）：/system/menu。
 *
 * <p>权限码 system:menu:list/add/edit/del（V11 已授权 admin）；tree 供角色分配菜单与前端动态菜单。</p>
 */
@RestController
@RequestMapping("/system/menu")
public class SystemMenuController {

    private final SystemMenuService menuService;

    public SystemMenuController(SystemMenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:menu:list') or hasAuthority('system:role:assign')")
    public Result<List<SysMenu>> tree() {
        return Result.ok(menuService.tree());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public Result<SysMenu> detail(@PathVariable Long id) {
        return Result.ok(menuService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:add')")
    public Result<Long> create(@Valid @RequestBody MenuDTO dto) {
        return Result.ok(menuService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:edit')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody MenuDTO dto) {
        menuService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:del')")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok();
    }
}
