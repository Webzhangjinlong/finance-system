package com.finance.system;

import com.finance.common.core.domain.PageResult;
import com.finance.common.core.exception.BusinessException;
import com.finance.system.domain.SysMenu;
import com.finance.system.domain.SysRole;
import com.finance.system.domain.SysUser;
import com.finance.system.dto.MenuDTO;
import com.finance.system.dto.RoleDTO;
import com.finance.system.dto.UserDTO;
import com.finance.system.mapper.SysUserMapper;
import com.finance.system.service.SystemMenuService;
import com.finance.system.service.SystemRoleService;
import com.finance.system.service.SystemUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 系统管理集成测试（S2，Gate 14）：用户/角色/菜单 CRUD + 分配 + 权限联动。
 *
 * <p>覆盖：BCrypt 加密、用户名/角色标识唯一、admin 保护、角色-菜单分配后权限码
 * 经 selectPermsByUserId 生效（与登录加载同源）、菜单树、删除保护。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class SystemServiceTest {

    @Autowired
    private SystemUserService userService;
    @Autowired
    private SystemRoleService roleService;
    @Autowired
    private SystemMenuService menuService;
    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String USERNAME = "s2_test_user";
    private static final String ROLE_KEY = "s2_test_role";
    private static final String MENU_NAME = "S2测试菜单";
    private Long roleId;
    private Long menuId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM sys_user_role WHERE user_id IN "
                + "(SELECT id FROM sys_user WHERE username = '" + USERNAME + "')");
        jdbcTemplate.execute("DELETE FROM sys_role_menu WHERE role_id IN "
                + "(SELECT id FROM sys_role WHERE role_key = '" + ROLE_KEY + "')");
        jdbcTemplate.execute("DELETE FROM sys_user WHERE username = '" + USERNAME + "'");
        jdbcTemplate.execute("DELETE FROM sys_role WHERE role_key LIKE 's2_test_role%'");
        jdbcTemplate.execute("DELETE FROM sys_menu WHERE menu_name = '" + MENU_NAME + "'");
        jdbcTemplate.execute("DELETE FROM sys_menu WHERE perms = 's2:test:perm'");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM sys_user_role WHERE user_id IN "
                + "(SELECT id FROM sys_user WHERE username = '" + USERNAME + "')");
        jdbcTemplate.execute("DELETE FROM sys_role_menu WHERE role_id IN "
                + "(SELECT id FROM sys_role WHERE role_key = '" + ROLE_KEY + "')");
        jdbcTemplate.execute("DELETE FROM sys_user WHERE username = '" + USERNAME + "'");
        jdbcTemplate.execute("DELETE FROM sys_role WHERE role_key LIKE 's2_test_role%'");
        jdbcTemplate.execute("DELETE FROM sys_menu WHERE menu_name = '" + MENU_NAME + "'");
        jdbcTemplate.execute("DELETE FROM sys_menu WHERE perms = 's2:test:perm'");
    }

    // ===== 用户 =====

    @Test
    void createUser_encryptsPasswordAndAssignsRoles() {
        roleId = roleService.create(role("s2 测试角色", ROLE_KEY));
        UserDTO dto = user(USERNAME, "abc123");
        dto.setRoleIds(List.of(roleId));

        Long id = userService.create(dto);

        SysUser saved = userMapper.selectById(id);
        assertThat(saved).isNotNull();
        assertThat(saved.getPassword()).isNotEqualTo("abc123");
        assertThat(passwordEncoder.matches("abc123", saved.getPassword())).isTrue();
        assertThat(userService.detail(id).getRoleIds()).contains(roleId);
    }

    @Test
    void createUser_duplicateUsername_rejected() {
        userService.create(user(USERNAME, "abc123"));
        assertThatThrownBy(() -> userService.create(user(USERNAME, "abc123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已存在");
    }

    @Test
    void updateUser_editsProfile() {
        Long id = userService.create(user(USERNAME, "abc123"));
        UserDTO edit = new UserDTO();
        edit.setUsername(USERNAME);
        edit.setNickname("新昵称");
        edit.setEmail("a@b.com");
        edit.setStatus("ACTIVE");

        userService.update(id, edit);

        SysUser saved = userMapper.selectById(id);
        assertThat(saved.getNickname()).isEqualTo("新昵称");
        assertThat(saved.getEmail()).isEqualTo("a@b.com");
    }

    @Test
    void updateAdmin_cannotDisable() {
        UserDTO edit = new UserDTO();
        edit.setUsername("admin");
        edit.setStatus("DISABLED");
        assertThatThrownBy(() -> userService.update(1001L, edit))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超级管理员不可停用");
    }

    @Test
    void resetPassword_newPasswordTakesEffect() {
        Long id = userService.create(user(USERNAME, "abc123"));
        userService.resetPassword(id, "newpass9");
        SysUser saved = userMapper.selectById(id);
        assertThat(passwordEncoder.matches("newpass9", saved.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("abc123", saved.getPassword())).isFalse();
    }

    @Test
    void assignRoles_replaceOldLinks() {
        Long id = userService.create(user(USERNAME, "abc123"));
        Long roleA = roleService.create(role("角色A", ROLE_KEY + "_a"));
        Long roleB = roleService.create(role("角色B", ROLE_KEY + "_b"));
        userService.assignRoles(id, List.of(roleA, roleB));
        assertThat(userService.detail(id).getRoleIds()).containsExactlyInAnyOrder(roleA, roleB);

        userService.assignRoles(id, List.of(roleB));
        assertThat(userService.detail(id).getRoleIds()).containsExactly(roleB);
    }

    @Test
    void deleteUser_removesRoleLinks_adminProtected() {
        roleId = roleService.create(role("s2 测试角色", ROLE_KEY));
        Long id = userService.create(user(USERNAME, "abc123"));
        userService.assignRoles(id, List.of(roleId));

        userService.delete(id);

        assertThat(userMapper.selectById(id)).isNull();
        assertThat(userService.detail(1001L).getId()).isEqualTo(1001L);
        assertThatThrownBy(() -> userService.delete(1001L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超级管理员不可删除");
    }

    @Test
    void userPage_keywordAndStatusFilter() {
        Long id = userService.create(user(USERNAME, "abc123"));
        PageResult<SysUser> page = userService.page(USERNAME, null, 1, 10);
        assertThat(page.getRecords()).extracting(SysUser::getId).contains(id);
        assertThat(page.getRecords()).allMatch(u -> u.getPassword() == null);
    }

    // ===== 角色 =====

    @Test
    void createRole_duplicateKey_rejected() {
        roleService.create(role("s2 测试角色", ROLE_KEY));
        assertThatThrownBy(() -> roleService.create(role("另一个", ROLE_KEY)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("角色标识已存在");
    }

    @Test
    void assignMenus_permissionsVisibleViaUserPermsQuery() {
        // 核心联动：菜单权限 → 角色 → 用户，登录权限加载同源（selectPermsByUserId）
        roleId = roleService.create(role("s2 测试角色", ROLE_KEY));
        menuId = menuService.create(menu(MENU_NAME, "s2:test:perm"));
        roleService.assignMenus(roleId, List.of(menuId));

        Long userId = userService.create(user(USERNAME, "abc123"));
        userService.assignRoles(userId, List.of(roleId));

        List<String> perms = userMapper.selectPermsByUserId(userId);
        assertThat(perms).contains("s2:test:perm");
    }

    @Test
    void deleteRole_adminProtected_andInUseProtected() {
        roleId = roleService.create(role("s2 测试角色", ROLE_KEY));
        Long id = userService.create(user(USERNAME, "abc123"));
        userService.assignRoles(id, List.of(roleId));

        assertThatThrownBy(() -> roleService.delete(roleId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已分配给用户");
        assertThatThrownBy(() -> roleService.delete(1001L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超级管理员角色不可删除");
    }

    // ===== 菜单 =====

    @Test
    void menuTree_buildsHierarchyByParent() {
        Long parentId = menuService.create(menu(MENU_NAME, null));
        MenuDTO child = new MenuDTO();
        child.setParentId(parentId);
        child.setMenuName(MENU_NAME + "-子");
        child.setMenuType(SysMenu.TYPE_BUTTON);
        child.setPerms("s2:test:perm");
        Long childId = menuService.create(child);

        List<SysMenu> tree = menuService.tree();
        assertThat(tree).isNotEmpty();
        boolean found = tree.stream().anyMatch(m -> m.getId().equals(parentId)
                && m.getChildren().stream().anyMatch(c -> c.getId().equals(childId)));
        assertThat(found).isTrue();
    }

    @Test
    void deleteMenu_withChildren_rejected() {
        Long parentId = menuService.create(menu(MENU_NAME, null));
        MenuDTO child = new MenuDTO();
        child.setParentId(parentId);
        child.setMenuName(MENU_NAME + "-子");
        child.setMenuType(SysMenu.TYPE_BUTTON);
        menuService.create(child);

        assertThatThrownBy(() -> menuService.delete(parentId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("存在子菜单");
    }

    // ===== 构造器 =====

    private UserDTO user(String username, String password) {
        UserDTO dto = new UserDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        dto.setNickname("S2 测试用户");
        return dto;
    }

    private RoleDTO role(String name, String key) {
        RoleDTO dto = new RoleDTO();
        dto.setRoleName(name);
        dto.setRoleKey(key);
        dto.setRoleSort(9);
        dto.setStatus("ACTIVE");
        return dto;
    }

    private MenuDTO menu(String name, String perms) {
        MenuDTO dto = new MenuDTO();
        dto.setMenuName(name);
        dto.setMenuType(SysMenu.TYPE_MENU);
        dto.setPerms(perms);
        dto.setStatus("ACTIVE");
        return dto;
    }
}
