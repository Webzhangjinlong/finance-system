package com.finance.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.system.domain.SysUser;
import com.finance.system.domain.SysUserRole;
import com.finance.system.dto.UserDTO;
import com.finance.system.mapper.SysUserMapper;
import com.finance.system.mapper.SysUserRoleMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户管理（S2，硬约束 21：密码 BCrypt；唯一约束 DB 兜底 uq_sys_user_username）。
 *
 * <p>系统域全局表（无 company_code）；admin（id=1001）内置超级管理员，禁删禁停用。
 * 用户名唯一对逻辑删除行仍生效（L37 教训同源），新建/改名需排除逻辑删除做查重。</p>
 */
@Service
public class SystemUserService {

    /** 内置超级管理员用户 ID（禁删/禁停用）。 */
    public static final long ADMIN_USER_ID = 1001L;

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public SystemUserService(SysUserMapper userMapper,
                             SysUserRoleMapper userRoleMapper,
                             PasswordEncoder passwordEncoder,
                             JdbcTemplate jdbcTemplate) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 分页（关键词匹配用户名/昵称，可按状态过滤）。 */
    public PageResult<SysUser> page(String keyword, String status, long page, long size) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(SysUser::getUsername, keyword).or().like(SysUser::getNickname, keyword));
        }
        if (StringUtils.hasText(status)) {
            qw.eq(SysUser::getStatus, status);
        }
        qw.orderByAsc(SysUser::getId);
        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size), qw);
        p.getRecords().forEach(u -> u.setPassword(null));
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** 详情（含已分配角色 ID，脱敏密码）。 */
    public SysUser detail(Long id) {
        SysUser user = requireUser(id);
        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, id))
                .stream().map(SysUserRole::getRoleId).toList();
        user.setRoleIds(roleIds);
        user.setPassword(null);
        return user;
    }

    /** 新建用户：BCrypt 加密、用户名查重（含逻辑删除）、初始分配角色。 */
    @Transactional
    public Long create(UserDTO dto) {
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername())) > 0) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setStatus(StringUtils.hasText(dto.getStatus())
                ? dto.getStatus() : SysUser.STATUS_ACTIVE);
        userMapper.insert(user);
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            assignRoles(user.getId(), dto.getRoleIds());
        }
        return user.getId();
    }

    /** 编辑：不改用户名/密码；admin 禁停用。 */
    @Transactional
    public void update(Long id, UserDTO dto) {
        SysUser user = requireUser(id);
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        if (StringUtils.hasText(dto.getStatus())) {
            if (id == ADMIN_USER_ID && SysUser.STATUS_DISABLED.equals(dto.getStatus())) {
                throw new BusinessException("超级管理员不可停用");
            }
            user.setStatus(dto.getStatus());
        }
        userMapper.updateById(user);
    }

    /** 重置密码（BCrypt）。 */
    @Transactional
    public void resetPassword(Long id, String rawPassword) {
        requireUser(id);
        SysUser patch = new SysUser();
        patch.setId(id);
        patch.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.updateById(patch);
    }

    /** 分配角色（先清后插，物理清旧关联防唯一约束残留，事务内）。 */
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        requireUser(userId);
        userRoleMapper.physicallyDeleteByUserId(userId);
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }

    /** 删除：admin 禁删；先清用户角色关联（物理删除）。 */
    @Transactional
    public void delete(Long id) {
        if (id == ADMIN_USER_ID) {
            throw new BusinessException("超级管理员不可删除");
        }
        requireUser(id);
        userRoleMapper.physicallyDeleteByUserId(id);
        userMapper.deleteById(id);
    }

    /** 按权限码查拥有该权限的用户 id（H4 劳动合同到期提醒：发给 HR 角色）。 */
    public List<Long> listUserIdsByPermission(String permCode) {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT ur.user_id FROM sys_user_role ur"
                        + " JOIN sys_role_menu rm ON rm.role_id = ur.role_id"
                        + " JOIN sys_menu m ON m.id = rm.menu_id"
                        + " JOIN sys_user u ON u.id = ur.user_id"
                        + " WHERE m.perms = ? AND u.deleted = 0",
                Long.class, permCode);
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }
}
