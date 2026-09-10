-- ============================================================
-- V12__hr_permissions.sql  Gate 15 人事管理权限按钮（H1-H3）
-- 复用 V2 种子菜单：1400 人事管理 / 1401 员工档案 / 1402 工资管理
-- 新增：1403 考勤管理 + 员工/工资/考勤按钮权限码
-- 授权：admin 角色（role_id=1001）
-- ============================================================

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, status) VALUES
(1403, 1400, '考勤管理', 'MENU', '/hr/attendance', 'hr/attendance/index', 'hr:attendance:list', NULL, 3, 'ACTIVE'),
(140101, 1401, '新增员工',   'BUTTON', NULL, NULL, 'hr:employee:add',        NULL, 1, 'ACTIVE'),
(140102, 1401, '修改员工',   'BUTTON', NULL, NULL, 'hr:employee:edit',       NULL, 2, 'ACTIVE'),
(140103, 1401, '离职/删除',  'BUTTON', NULL, NULL, 'hr:employee:del',        NULL, 3, 'ACTIVE'),
(140201, 1402, '工资计算',   'BUTTON', NULL, NULL, 'hr:salary:calculate',    NULL, 1, 'ACTIVE'),
(140202, 1402, '提交复核',   'BUTTON', NULL, NULL, 'hr:salary:submit',       NULL, 2, 'ACTIVE'),
(140203, 1402, '发放确认',   'BUTTON', NULL, NULL, 'hr:salary:approve',      NULL, 3, 'ACTIVE'),
(140204, 1402, '工资条查看', 'BUTTON', NULL, NULL, 'hr:salary:slip',         NULL, 4, 'ACTIVE'),
(140301, 1403, '考勤录入',   'BUTTON', NULL, NULL, 'hr:attendance:edit',     NULL, 1, 'ACTIVE');

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 100000 + m.id, 1001, m.id
FROM sys_menu m
WHERE m.id IN (1403, 140101, 140102, 140103, 140201, 140202, 140203, 140204, 140301);
