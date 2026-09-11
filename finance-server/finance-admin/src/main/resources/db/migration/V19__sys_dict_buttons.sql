-- ============================================================
-- V19: S3 字典管理 —— 按钮权限 150401-150406 + role 1001 授权
-- 幂等：INSERT ... WHERE NOT EXISTS（L45/L49）
-- 已核对：1504 字典管理 MENU 已存在（1500 系统管理下，system:dict:list 已授权）；
-- 150401-150406 未占用（附件按钮至 150702）；sys_role_menu.id = 100000 + menu_id
-- ============================================================

INSERT INTO sys_menu (id, menu_name, parent_id, sort_order, path, component, menu_type, perms, status)
SELECT 150401, '新增字典类型', 1504, 1, NULL, NULL, 'BUTTON', 'system:dict:add', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 150401);

INSERT INTO sys_menu (id, menu_name, parent_id, sort_order, path, component, menu_type, perms, status)
SELECT 150402, '修改字典类型', 1504, 2, NULL, NULL, 'BUTTON', 'system:dict:edit', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 150402);

INSERT INTO sys_menu (id, menu_name, parent_id, sort_order, path, component, menu_type, perms, status)
SELECT 150403, '删除字典类型', 1504, 3, NULL, NULL, 'BUTTON', 'system:dict:del', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 150403);

INSERT INTO sys_menu (id, menu_name, parent_id, sort_order, path, component, menu_type, perms, status)
SELECT 150404, '新增字典数据', 1504, 4, NULL, NULL, 'BUTTON', 'system:dict:data:add', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 150404);

INSERT INTO sys_menu (id, menu_name, parent_id, sort_order, path, component, menu_type, perms, status)
SELECT 150405, '修改字典数据', 1504, 5, NULL, NULL, 'BUTTON', 'system:dict:data:edit', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 150405);

INSERT INTO sys_menu (id, menu_name, parent_id, sort_order, path, component, menu_type, perms, status)
SELECT 150406, '删除字典数据', 1504, 6, NULL, NULL, 'BUTTON', 'system:dict:data:del', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 150406);

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 10150401, 1001, 150401 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1001 AND menu_id = 150401);
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 10150402, 1001, 150402 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1001 AND menu_id = 150402);
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 10150403, 1001, 150403 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1001 AND menu_id = 150403);
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 10150404, 1001, 150404 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1001 AND menu_id = 150404);
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 10150405, 1001, 150405 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1001 AND menu_id = 150405);
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 10150406, 1001, 150406 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1001 AND menu_id = 150406);
