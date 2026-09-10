-- ============================================================
-- V10__expense.sql  Gate 13 费用报销（F6）
-- 表：fin_expense_claim / fin_expense_item
-- 菜单：1208 费用报销（父 1200 财务管理，1206 已被 V7 应收应付占用）+ 4 按钮权限；授权 admin(role_id=1001)
-- 硬约束落地：金额 NUMERIC(18,2) + CHECK(amount > 0)（R1）、company_code 隔离（R2）、
--           唯一 claim_no（R5 风格）、公共五件套 + 逻辑删除（R18）
-- ============================================================

CREATE TABLE fin_expense_claim (
    id BIGINT PRIMARY KEY,
    company_code VARCHAR(32)  NOT NULL,
    claim_no     VARCHAR(64)  NOT NULL,
    expense_type VARCHAR(32)  NOT NULL,
    reason       VARCHAR(255) NOT NULL,
    invoice_no   VARCHAR(64),
    amount       NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    status       VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    approver     VARCHAR(64),
    approved_at  TIMESTAMPTZ,
    payer        VARCHAR(64),
    paid_at      TIMESTAMPTZ,
    create_by    VARCHAR(64),
    create_time  TIMESTAMPTZ DEFAULT now(),
    update_by    VARCHAR(64),
    update_time  TIMESTAMPTZ DEFAULT now(),
    deleted      SMALLINT DEFAULT 0,
    CONSTRAINT uk_expense_claim_no UNIQUE (company_code, claim_no)
);

CREATE TABLE fin_expense_item (
    id BIGINT PRIMARY KEY,
    company_code VARCHAR(32)  NOT NULL,
    claim_id     BIGINT       NOT NULL,
    subject_code VARCHAR(32)  NOT NULL,
    summary      VARCHAR(255),
    amount       NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    create_by    VARCHAR(64),
    create_time  TIMESTAMPTZ DEFAULT now(),
    update_by    VARCHAR(64),
    update_time  TIMESTAMPTZ DEFAULT now(),
    deleted      SMALLINT DEFAULT 0,
    CONSTRAINT fk_expense_item_claim FOREIGN KEY (claim_id) REFERENCES fin_expense_claim(id)
);

CREATE INDEX idx_expense_claim_company_status ON fin_expense_claim (company_code, status);
CREATE INDEX idx_expense_item_claim ON fin_expense_item (claim_id);

-- ---------- 菜单 + 按钮权限 ----------
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, status) VALUES
(1208,   1200, '费用报销', 'MENU',   '/finance/expense', 'finance/expense/index', 'finance:expense:list',    'Tickets', 6, 'ACTIVE'),
(120801, 1208, '新增报销', 'BUTTON', NULL, NULL, 'finance:expense:add',     NULL, 1, 'ACTIVE'),
(120802, 1208, '修改报销', 'BUTTON', NULL, NULL, 'finance:expense:edit',    NULL, 2, 'ACTIVE'),
(120803, 1208, '审批报销', 'BUTTON', NULL, NULL, 'finance:expense:approve', NULL, 3, 'ACTIVE'),
(120804, 1208, '打款报销', 'BUTTON', NULL, NULL, 'finance:expense:pay',     NULL, 4, 'ACTIVE');

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 100000 + m.id, 1001, m.id
FROM sys_menu m
WHERE m.id IN (1208, 120801, 120802, 120803, 120804);
