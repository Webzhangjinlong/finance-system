package com.finance.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finance.common.core.exception.BusinessException;
import com.finance.system.domain.SysMenu;
import com.finance.system.domain.SysRoleMenu;
import com.finance.system.dto.MenuDTO;
import com.finance.system.mapper.SysMenuMapper;
import com.finance.system.mapper.SysRoleMenuMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单管理（S2）：树 + CRUD。
 *
 * <p>菜单为全局资源（无 company_code）；父菜单删除前须无子菜单；
 * 删除菜单先清 role_menu 关联；菜单类型 DIR/MENU/BUTTON（常量）。</p>
 */
@Service
public class SystemMenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public SystemMenuService(SysMenuMapper menuMapper,
                             SysRoleMenuMapper roleMenuMapper) {
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    /** 菜单树（按 sort_order 排序；id 升序稳定）。 */
    public List<SysMenu> tree() {
        List<SysMenu> all = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSortOrder)
                .orderByAsc(SysMenu::getId));
        Map<Long, List<SysMenu>> byParent = all.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        List<SysMenu> roots = new ArrayList<>();
        for (SysMenu m : all) {
            if (m.getParentId() == null || m.getParentId() == 0L) {
                roots.add(m);
            }
        }
        for (SysMenu m : all) {
            m.setChildren(byParent.getOrDefault(m.getId(), List.of()));
        }
        return roots;
    }

    /** 详情。 */
    public SysMenu detail(Long id) {
        SysMenu menu = requireMenu(id);
        menu.setChildren(null);
        return menu;
    }

    /** 新建菜单。 */
    @Transactional
    public Long create(MenuDTO dto) {
        if (dto.getParentId() != null && dto.getParentId() != 0L) {
            SysMenu parent = menuMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw new BusinessException("父菜单不存在");
            }
        }
        SysMenu menu = new SysMenu();
        apply(menu, dto);
        menuMapper.insert(menu);
        return menu.getId();
    }

    /** 编辑菜单。 */
    @Transactional
    public void update(Long id, MenuDTO dto) {
        requireMenu(id);
        if (dto.getParentId() != null && dto.getParentId() != 0L) {
            if (dto.getParentId().equals(id)) {
                throw new BusinessException("父菜单不能是自己");
            }
            if (menuMapper.selectById(dto.getParentId()) == null) {
                throw new BusinessException("父菜单不存在");
            }
        }
        SysMenu menu = new SysMenu();
        menu.setId(id);
        apply(menu, dto);
        menuMapper.updateById(menu);
    }

    /** 删除菜单：有子菜单禁删；先清角色菜单关联。 */
    @Transactional
    public void delete(Long id) {
        requireMenu(id);
        if (menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, id)) > 0) {
            throw new BusinessException("存在子菜单，不可删除");
        }
        roleMenuMapper.physicallyDeleteByMenuId(id);
        menuMapper.deleteById(id);
    }

    private void apply(SysMenu menu, MenuDTO dto) {
        menu.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        menu.setMenuName(dto.getMenuName());
        menu.setMenuType(dto.getMenuType());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setPerms(dto.getPerms());
        menu.setIcon(dto.getIcon());
        menu.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        menu.setStatus(StringUtils.hasText(dto.getStatus())
                ? dto.getStatus() : SysMenu.STATUS_ACTIVE);
    }

    private SysMenu requireMenu(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException("菜单不存在");
        }
        return menu;
    }
}
