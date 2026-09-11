-- ============================================================
-- V18: F9 财务分析 —— 菜单 1210 财务分析 + 授权
-- 幂等设计：INSERT ... WHERE NOT EXISTS（L45/L49）
-- 已核对：1210 未占用（1201-1209 已用）；sys_menu 列 sort_order、
-- 枚举 DIR/MENU/BUTTON；sys_role_menu.id = 100000 + menu_id（无序列）
-- ============================================================

INSERT INTO sys_menu (id, menu_name, parent_id, sort_order, path, component, menu_type, perms, status)
SELECT 1210, '财务分析', 1200, 10, 'analysis', 'analysis/index', 'MENU', 'finance:analysis:list', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 1210);

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 101210, 1001, 1210
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1001 AND menu_id = 1210);
