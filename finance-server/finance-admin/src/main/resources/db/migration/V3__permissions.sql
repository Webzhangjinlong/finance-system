-- ============================================================
-- V3__permissions.sql  按钮级权限点（Gate 6）
-- 背景：V2 菜单仅含 list 级权限，@PreAuthorize 需要 add/edit/audit/book/reverse 等按钮权限码
-- 处理：新增 BUTTON 类型菜单并授权给 admin 角色（role_id=1001）
-- ============================================================

-- ---------- 科目管理按钮权限 ----------
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, status, create_by, create_time) VALUES
(120101, 1201, '科目新增', 'BUTTON', NULL, NULL, 'finance:subject:add',    NULL, 1, 'ACTIVE', 'seed', now()),
(120102, 1201, '科目修改', 'BUTTON', NULL, NULL, 'finance:subject:edit',   NULL, 2, 'ACTIVE', 'seed', now()),
(120103, 1201, '科目删除', 'BUTTON', NULL, NULL, 'finance:subject:del',    NULL, 3, 'ACTIVE', 'seed', now()),
(120104, 1201, '期初导入', 'BUTTON', NULL, NULL, 'finance:subject:import', NULL, 4, 'ACTIVE', 'seed', now());

-- ---------- 凭证管理按钮权限 ----------
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, status, create_by, create_time) VALUES
(120201, 1202, '凭证新增', 'BUTTON', NULL, NULL, 'finance:voucher:add',     NULL, 1, 'ACTIVE', 'seed', now()),
(120202, 1202, '凭证修改', 'BUTTON', NULL, NULL, 'finance:voucher:edit',    NULL, 2, 'ACTIVE', 'seed', now()),
(120203, 1202, '凭证审核', 'BUTTON', NULL, NULL, 'finance:voucher:audit',   NULL, 3, 'ACTIVE', 'seed', now()),
(120204, 1202, '凭证过账', 'BUTTON', NULL, NULL, 'finance:voucher:book',    NULL, 4, 'ACTIVE', 'seed', now()),
(120205, 1202, '凭证冲销', 'BUTTON', NULL, NULL, 'finance:voucher:reverse', NULL, 5, 'ACTIVE', 'seed', now()),
(120206, 1202, '凭证删除', 'BUTTON', NULL, NULL, 'finance:voucher:del',     NULL, 6, 'ACTIVE', 'seed', now());

-- ---------- 授权给 admin 角色（1001） ----------
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 200000 + m.id, 1001, m.id
FROM sys_menu m
WHERE m.perms IN (
    'finance:subject:add', 'finance:subject:edit', 'finance:subject:del', 'finance:subject:import',
    'finance:voucher:add', 'finance:voucher:edit', 'finance:voucher:audit',
    'finance:voucher:book', 'finance:voucher:reverse', 'finance:voucher:del'
);
