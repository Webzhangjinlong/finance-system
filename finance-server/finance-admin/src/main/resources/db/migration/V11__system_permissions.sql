-- ============================================================
-- V11__system_permissions.sql  Gate 14 系统管理权限菜单（S2）
-- 新建：1000 系统管理目录 + 1001 用户管理 + 1002 角色管理 + 1003 菜单管理
--      + 用户/角色/菜单按钮权限（新增/修改/删除/重置密码/分配角色/分配菜单）
-- 授权：admin 角色（role_id=1001）全量
-- 补列：sys_user_role / sys_role_menu 补齐公共五件套（硬约束 18，
--       V1 仅建 create_by/create_time/deleted，缺 update_by/update_time）
-- ============================================================

ALTER TABLE sys_user_role ADD COLUMN update_by   VARCHAR(64);
ALTER TABLE sys_user_role ADD COLUMN update_time TIMESTAMPTZ DEFAULT now();
ALTER TABLE sys_role_menu ADD COLUMN update_by   VARCHAR(64);
ALTER TABLE sys_role_menu ADD COLUMN update_time TIMESTAMPTZ DEFAULT now();

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, status) VALUES
(1000, 0,    '系统管理', 'DIR',  '/system', NULL, NULL, 'Setting', 7, 'ACTIVE'),
(1001, 1000, '用户管理', 'MENU', '/system/user', 'system/user/index', 'system:user:list', NULL, 1, 'ACTIVE'),
(1002, 1000, '角色管理', 'MENU', '/system/role', 'system/role/index', 'system:role:list', NULL, 2, 'ACTIVE'),
(1003, 1000, '菜单管理', 'MENU', '/system/menu', 'system/menu/index', 'system:menu:list', NULL, 3, 'ACTIVE'),
(100101, 1001, '新增用户',   'BUTTON', NULL, NULL, 'system:user:add',        NULL, 1, 'ACTIVE'),
(100102, 1001, '修改用户',   'BUTTON', NULL, NULL, 'system:user:edit',       NULL, 2, 'ACTIVE'),
(100103, 1001, '删除用户',   'BUTTON', NULL, NULL, 'system:user:del',        NULL, 3, 'ACTIVE'),
(100104, 1001, '重置密码',   'BUTTON', NULL, NULL, 'system:user:reset-pwd',  NULL, 4, 'ACTIVE'),
(100105, 1001, '分配角色',   'BUTTON', NULL, NULL, 'system:user:assign',     NULL, 5, 'ACTIVE'),
(100201, 1002, '新增角色',   'BUTTON', NULL, NULL, 'system:role:add',        NULL, 1, 'ACTIVE'),
(100202, 1002, '修改角色',   'BUTTON', NULL, NULL, 'system:role:edit',       NULL, 2, 'ACTIVE'),
(100203, 1002, '删除角色',   'BUTTON', NULL, NULL, 'system:role:del',        NULL, 3, 'ACTIVE'),
(100204, 1002, '分配菜单',   'BUTTON', NULL, NULL, 'system:role:assign',     NULL, 4, 'ACTIVE'),
(100301, 1003, '新增菜单',   'BUTTON', NULL, NULL, 'system:menu:add',        NULL, 1, 'ACTIVE'),
(100302, 1003, '修改菜单',   'BUTTON', NULL, NULL, 'system:menu:edit',       NULL, 2, 'ACTIVE'),
(100303, 1003, '删除菜单',   'BUTTON', NULL, NULL, 'system:menu:del',        NULL, 3, 'ACTIVE');

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 100000 + m.id, 1001, m.id
FROM sys_menu m
WHERE m.id IN (1000, 1001, 1002, 1003, 100101, 100102, 100103, 100104, 100105,
               100201, 100202, 100203, 100204, 100301, 100302, 100303);
