-- ============================================================
-- V17: F8 凭证映射引擎 —— 映射规则表 + 种子规则 + 菜单授权
-- 幂等设计：CREATE TABLE IF NOT EXISTS / INSERT ... WHERE NOT EXISTS
-- （L45 教训：建表前已确认全库无同名表；CHECK 枚举值对齐现有约定）
-- ============================================================

-- 1) 映射规则表
CREATE TABLE IF NOT EXISTS fin_voucher_rule (
    id               BIGSERIAL PRIMARY KEY,
    company_code     VARCHAR(20)  NOT NULL,
    rule_code        VARCHAR(50)  NOT NULL,
    rule_name        VARCHAR(100) NOT NULL,
    source_type      VARCHAR(30)  NOT NULL,
    event_type       VARCHAR(30),
    direction        VARCHAR(10)  NOT NULL,
    subject_code     VARCHAR(20)  NOT NULL,
    summary_template VARCHAR(200),
    amount_ratio     NUMERIC(5,2) NOT NULL DEFAULT 1.00,
    enabled          VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    create_by        VARCHAR(50),
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_by        VARCHAR(50),
    update_time      TIMESTAMPTZ,
    deleted          SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uk_voucher_rule_code UNIQUE (company_code, rule_code),
    CONSTRAINT ck_voucher_rule_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_voucher_rule_enabled CHECK (enabled IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_voucher_rule_ratio CHECK (amount_ratio > 0)
);

COMMENT ON TABLE  fin_voucher_rule IS '凭证映射规则（F8：业务事件→借贷科目映射，引擎按规则生成凭证草稿）';
COMMENT ON COLUMN fin_voucher_rule.source_type IS '业务来源类型（EXPENSE/PAYMENT/RECEIPT/CONTRACT...）';
COMMENT ON COLUMN fin_voucher_rule.event_type  IS '触发事件（PAID/APPROVED/BOOKED...，可空=任意）';
COMMENT ON COLUMN fin_voucher_rule.direction   IS '分录方向 DEBIT/CREDIT';
COMMENT ON COLUMN fin_voucher_rule.subject_code IS '映射科目编码（需存在于科目表，服务层校验）';
COMMENT ON COLUMN fin_voucher_rule.summary_template IS '摘要模板，支持 {claimNo} 等占位符';
COMMENT ON COLUMN fin_voucher_rule.amount_ratio IS '金额比例（1.00=全额，借贷各自计算）';

CREATE INDEX IF NOT EXISTS idx_voucher_rule_source ON fin_voucher_rule (company_code, source_type, event_type, enabled);

-- 2) 种子规则：报销打款 → 贷 1002 银行存款（兼容 F6 现有默认映射，页面可直接看到）
INSERT INTO fin_voucher_rule (company_code, rule_code, rule_name, source_type, event_type, direction,
                              subject_code, summary_template, amount_ratio)
SELECT 'DEMO', 'EXPENSE_PAID_CREDIT', '报销打款贷方（银行存款）', 'EXPENSE', 'PAID', 'CREDIT',
       '1002', '报销打款 {claimNo}', 1.00
WHERE NOT EXISTS (SELECT 1 FROM fin_voucher_rule WHERE company_code = 'DEMO' AND rule_code = 'EXPENSE_PAID_CREDIT');

-- 3) 菜单：1209 凭证规则（parent 1200 财务管理）+ 按钮权限
INSERT INTO sys_menu (id, menu_name, parent_id, sort_order, path, component, menu_type, perms, status)
SELECT 1209, '凭证规则', 1200, 9, 'voucher-rule', 'voucher-rule/index', 'MENU', 'finance:voucher-rule:list', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 1209);

INSERT INTO sys_menu (id, menu_name, parent_id, sort_order, path, component, menu_type, perms, status)
SELECT 120901, '规则新增', 1209, 1, '', '', 'BUTTON', 'finance:voucher-rule:add', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 120901);

INSERT INTO sys_menu (id, menu_name, parent_id, sort_order, path, component, menu_type, perms, status)
SELECT 120902, '规则修改', 1209, 2, '', '', 'BUTTON', 'finance:voucher-rule:edit', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 120902);

INSERT INTO sys_menu (id, menu_name, parent_id, sort_order, path, component, menu_type, perms, status)
SELECT 120903, '规则删除', 1209, 3, '', '', 'BUTTON', 'finance:voucher-rule:del', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 120903);

-- 4) 授权给 admin 角色（1001）：财务类菜单沿用既有授权模式
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 100000 + m.id, 1001, m.id FROM sys_menu m
WHERE m.id IN (1209, 120901, 120902, 120903)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1001 AND rm.menu_id = m.id);
