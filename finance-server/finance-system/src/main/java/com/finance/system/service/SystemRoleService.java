package com.finance.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.system.domain.SysRole;
import com.finance.system.domain.SysRoleMenu;
import com.finance.system.domain.SysUserRole;
import com.finance.system.dto.RoleDTO;
import com.finance.system.mapper.SysRoleMapper;
import com.finance.system.mapper.SysRoleMenuMapper;
import com.finance.system.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 角色管理（S2）：CRUD + 分配菜单权限。
 *
 * <p>role_key 唯一（DB uq_sys_role_key 兜底 + 服务查重）；admin 角色（id=1001）禁删；
 * 被用户引用的角色禁删；删角色先清 role_menu 关联。</p>
 */
@Service
public class SystemRoleService {

    /** 内置超级管理员角色 ID（禁删）。 */
    public static final long ADMIN_ROLE_ID = 1001L;

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;

    public SystemRoleService(SysRoleMapper roleMapper,
                             SysRoleMenuMapper roleMenuMapper,
                             SysUserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /** 全量列表（角色分配下拉用，不过滤状态可选）。 */
    public List<SysRole> listAll() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, SysRole.STATUS_ACTIVE)
                .orderByAsc(SysRole::getRoleSort));
    }

    /** 分页（关键词匹配名称/标识）。 */
    public PageResult<SysRole> page(String keyword, long page, long size) {
        LambdaQueryWrapper<SysRole> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(SysRole::getRoleName, keyword).or().like(SysRole::getRoleKey, keyword));
        }
        qw.orderByAsc(SysRole::getRoleSort);
        Page<SysRole> p = roleMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** 详情（含已分配菜单 ID）。 */
    public SysRole detail(Long id) {
        SysRole role = requireRole(id);
        List<Long> menuIds = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id))
                .stream().map(SysRoleMenu::getMenuId).toList();
        role.setMenuIds(menuIds);
        return role;
    }

    /** 新建角色（role_key 查重，含逻辑删除）。 */
    @Transactional
    public Long create(RoleDTO dto) {
        if (roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, dto.getRoleKey())) > 0) {
            throw new BusinessException("角色标识已存在");
        }
        SysRole role = new SysRole();
        apply(role, dto);
        roleMapper.insert(role);
        return role.getId();
    }

    /** 编辑角色。 */
    @Transactional
    public void update(Long id, RoleDTO dto) {
        SysRole role = requireRole(id);
        if (roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, dto.getRoleKey())
                .ne(SysRole::getId, id)) > 0) {
            throw new BusinessException("角色标识已存在");
        }
        apply(role, dto);
        roleMapper.updateById(role);
    }

    /** 分配菜单权限（先清后插，物理清旧关联防唯一约束残留，事务内）。 */
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        requireRole(roleId);
        roleMenuMapper.physicallyDeleteByRoleId(roleId);
        for (Long menuId : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }

    /** 删除角色：admin 禁删；被用户引用禁删；先清菜单关联（物理删除）。 */
    @Transactional
    public void delete(Long id) {
        if (id == ADMIN_ROLE_ID) {
            throw new BusinessException("超级管理员角色不可删除");
        }
        requireRole(id);
        if (userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, id)) > 0) {
            throw new BusinessException("该角色已分配给用户，不可删除");
        }
        roleMenuMapper.physicallyDeleteByRoleId(id);
        roleMapper.deleteById(id);
    }

    private void apply(SysRole role, RoleDTO dto) {
        role.setRoleName(dto.getRoleName());
        role.setRoleKey(dto.getRoleKey());
        role.setRoleSort(dto.getRoleSort() == null ? 0 : dto.getRoleSort());
        role.setStatus(StringUtils.hasText(dto.getStatus())
                ? dto.getStatus() : SysRole.STATUS_ACTIVE);
        role.setRemark(dto.getRemark());
    }

    private SysRole requireRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }
}
