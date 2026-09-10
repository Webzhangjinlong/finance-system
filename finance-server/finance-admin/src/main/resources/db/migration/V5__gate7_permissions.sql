-- ============================================================
-- V5__gate7_permissions.sql  Gate 7 权限菜单（账簿/结账按钮/审批流）
-- 新增：1205 账簿查询；1203 下结账按钮（close/reopen）；1600 流程管理目录 + 审批待办
-- 授权：admin 角色（role_id=1001）全量
-- ============================================================

-- ---------- 账簿查询菜单（F3） ----------
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, status) VALUES
(1205, 1200, '账簿查询', 'MENU', '/finance/book', 'finance/book/index', 'finance:book:list', 'Tickets', 5, 'ACTIVE');

-- ---------- 期末结账按钮权限（F4） ----------
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, status) VALUES
(120301, 1203, '期末结账', 'BUTTON', NULL, NULL, 'finance:period:close',   NULL, 1, 'ACTIVE'),
(120302, 1203, '反结账',   'BUTTON', NULL, NULL, 'finance:period:reopen', NULL, 2, 'ACTIVE');

-- ---------- 流程管理目录 + 审批待办（W1） ----------
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, status) VALUES
(1600, 0,    '流程管理', 'DIR',  '/workflow',      NULL,                    NULL,                       'Share',      6, 'ACTIVE'),
(1601, 1600, '审批待办', 'MENU', '/workflow/todo', 'workflow/todo/index',   'workflow:task:approve',    NULL, 1, 'ACTIVE'),
(1602, 1600, '流程发起', 'MENU', '/workflow/start','workflow/start/index',  'workflow:instance:start',  NULL, 2, 'ACTIVE');

-- ---------- 授权给 admin 角色（1001） ----------
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 100000 + m.id, 1001, m.id
FROM sys_menu m
WHERE m.id IN (1205, 120301, 120302, 1600, 1601, 1602);
