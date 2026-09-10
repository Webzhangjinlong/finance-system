-- ============================================================
-- V6__contract_permissions.sql  Gate 8 合同权限菜单（C1/C2）
-- V2 种子已建 1300 合同管理目录 + 1301 合同台账（perms='contract:list'，已授权 admin）
-- 本迁移：① 台账权限码对齐 Controller（contract:list → contract:contract:list）
--         ② 补 5 个按钮权限（add/edit/del/void/submit）并授权 admin
-- ============================================================

-- ---------- 台账权限码对齐（Controller @PreAuthorize 统一 contract:contract:*） ----------
UPDATE sys_menu SET perms = 'contract:contract:list' WHERE id = 1301;

-- ---------- 合同台账按钮权限 ----------
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, status) VALUES
(130101, 1301, '新增合同', 'BUTTON', NULL, NULL, 'contract:contract:add',    NULL, 1, 'ACTIVE'),
(130102, 1301, '修改合同', 'BUTTON', NULL, NULL, 'contract:contract:edit',   NULL, 2, 'ACTIVE'),
(130103, 1301, '删除合同', 'BUTTON', NULL, NULL, 'contract:contract:del',    NULL, 3, 'ACTIVE'),
(130104, 1301, '作废合同', 'BUTTON', NULL, NULL, 'contract:contract:void',   NULL, 4, 'ACTIVE'),
(130105, 1301, '提交审批', 'BUTTON', NULL, NULL, 'contract:contract:submit', NULL, 5, 'ACTIVE');

-- ---------- 授权按钮给 admin 角色（1001；1300/1301 已由 V2 全量授权） ----------
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 100000 + m.id, 1001, m.id
FROM sys_menu m
WHERE m.id IN (130101, 130102, 130103, 130104, 130105);
