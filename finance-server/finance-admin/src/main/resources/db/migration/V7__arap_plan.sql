-- ============================================================
-- V7__arap_plan.sql  Gate 9 收付款计划联动（C3）
-- 1) fin_ar / fin_ap 增加 plan_id + 唯一约束（到期生成应收/应付幂等：一计划一单）
-- 2) 权限菜单：1302 收付款计划（contract:plan:list/sync）+ 1206 应收应付（finance:receivable:list）+ 1207 收付款登记（finance:receivable:write）
-- 3) 授权 admin（role_id=1001）
-- ============================================================

-- ---------- 应收/应付增加 plan_id（幂等生成依据） ----------
ALTER TABLE fin_ar ADD COLUMN plan_id BIGINT;
ALTER TABLE fin_ar ADD CONSTRAINT uq_fin_ar_plan UNIQUE (company_code, plan_id);
CREATE INDEX idx_fin_ar_contract ON fin_ar (company_code, contract_id);

ALTER TABLE fin_ap ADD COLUMN plan_id BIGINT;
ALTER TABLE fin_ap ADD CONSTRAINT uq_fin_ap_plan UNIQUE (company_code, plan_id);
CREATE INDEX idx_fin_ap_contract ON fin_ap (company_code, contract_id);

-- ---------- 收付款计划菜单（合同域 1302） ----------
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, status) VALUES
(1302, 1300, '收付款计划', 'MENU', '/contract/plans', 'contract/plans/index', 'contract:plan:list', NULL, 2, 'ACTIVE'),
(130201, 1302, '计划同步', 'BUTTON', NULL, NULL, 'contract:plan:sync', NULL, 1, 'ACTIVE');

-- ---------- 应收应付 + 收付款登记（财务域 1206/1207） ----------
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, status) VALUES
(1206, 1200, '应收应付', 'MENU', '/finance/arap', 'finance/arap/index', 'finance:receivable:list', NULL, 6, 'ACTIVE'),
(1207, 1200, '收付款登记', 'MENU', '/finance/arpay', 'finance/arpay/index', 'finance:receivable:write', NULL, 7, 'ACTIVE');

-- ---------- 授权 admin（1001；1302 父节点 1300 已由 V2 授权） ----------
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 100000 + m.id, 1001, m.id
FROM sys_menu m
WHERE m.id IN (1302, 130201, 1206, 1207);
