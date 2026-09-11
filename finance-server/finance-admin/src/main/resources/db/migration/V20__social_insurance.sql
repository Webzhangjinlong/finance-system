-- ============================================================
-- V20: 五险一金规则 + 缴费明细（批次 A：智能核算基座）
-- 1) hr_social_rule 社保公积金规则表（费率/基数上下限配置化，硬约束 17 禁魔法值）
-- 2) hr_social_detail 员工缴费明细表（每期每险种 基数/个人/单位）
-- 3) hr_employee.social_base 员工申报缴费基数
-- 4) 种子规则（示例配置：宁夏/银川常见区间，个人养老 8% 医疗 2% 失业 0.5%、
--    工伤/生育个人 0%、公积金 5%；以参保地/公司实际标准为准，规则页可随时调整）
-- 5) 权限：1404 社保公积金 MENU + 140401-140404 按钮 + role 1001 授权
-- ============================================================

CREATE TABLE hr_social_rule (
    id               BIGINT PRIMARY KEY,
    company_code     VARCHAR(32)  NOT NULL,
    social_type      VARCHAR(20)  NOT NULL,
    base_floor       NUMERIC(18,2) NOT NULL DEFAULT 0,
    base_ceiling     NUMERIC(18,2) NOT NULL DEFAULT 0,
    personal_rate    NUMERIC(9,4) NOT NULL DEFAULT 0,
    company_rate     NUMERIC(9,4) NOT NULL DEFAULT 0,
    effective_month  VARCHAR(7)   NOT NULL,
    create_by        VARCHAR(64),
    create_time      TIMESTAMPTZ  DEFAULT now(),
    update_by        VARCHAR(64),
    update_time      TIMESTAMPTZ  DEFAULT now(),
    deleted          SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uq_hr_social_rule UNIQUE (company_code, social_type, effective_month),
    CONSTRAINT ck_hr_social_rule_base CHECK (base_floor >= 0 AND base_ceiling >= 0 AND base_ceiling >= base_floor),
    CONSTRAINT ck_hr_social_rule_rate CHECK (personal_rate >= 0 AND company_rate >= 0),
    CONSTRAINT ck_hr_social_rule_month CHECK (effective_month ~ '^[0-9]{4}-[0-9]{2}$'),
    CONSTRAINT ck_hr_social_rule_deleted CHECK (deleted IN (0, 1))
);

CREATE TABLE hr_social_detail (
    id               BIGINT PRIMARY KEY,
    company_code     VARCHAR(32)  NOT NULL,
    employee_id      BIGINT       NOT NULL,
    salary_year      INT          NOT NULL,
    salary_month     INT          NOT NULL,
    social_type      VARCHAR(20)  NOT NULL,
    base_amount      NUMERIC(18,2) NOT NULL DEFAULT 0,
    personal_amount  NUMERIC(18,2) NOT NULL DEFAULT 0,
    company_amount   NUMERIC(18,2) NOT NULL DEFAULT 0,
    create_by        VARCHAR(64),
    create_time      TIMESTAMPTZ  DEFAULT now(),
    update_by        VARCHAR(64),
    update_time      TIMESTAMPTZ  DEFAULT now(),
    deleted          SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uq_hr_social_detail UNIQUE (company_code, employee_id, salary_year, salary_month, social_type),
    CONSTRAINT ck_hr_social_detail_month CHECK (salary_month BETWEEN 1 AND 12),
    CONSTRAINT ck_hr_social_detail_amounts CHECK (
        base_amount >= 0 AND personal_amount >= 0 AND company_amount >= 0
    ),
    CONSTRAINT ck_hr_social_detail_deleted CHECK (deleted IN (0, 1))
);

ALTER TABLE hr_employee ADD COLUMN social_base NUMERIC(18,2);

-- ---------- 种子规则（示例配置，2026-09 生效） ----------
INSERT INTO hr_social_rule (id, company_code, social_type, base_floor, base_ceiling, personal_rate, company_rate, effective_month) VALUES
(100001, 'DEMO', 'PENSION',      4800, 24000, 0.0800, 0.1600, '2026-09'),
(100002, 'DEMO', 'MEDICAL',      4800, 24000, 0.0200, 0.0800, '2026-09'),
(100003, 'DEMO', 'UNEMPLOYMENT', 4800, 24000, 0.0050, 0.0050, '2026-09'),
(100004, 'DEMO', 'INJURY',       4800, 24000, 0.0000, 0.0040, '2026-09'),
(100005, 'DEMO', 'MATERNITY',    4800, 24000, 0.0000, 0.0080, '2026-09'),
(100006, 'DEMO', 'HOUSING_FUND', 2000, 30000, 0.0500, 0.0500, '2026-09');

-- ---------- 权限：1404 社保公积金 ----------
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, status) VALUES
(1404, 1400, '社保公积金', 'MENU', '/hr/social', 'hr/social/index', 'hr:social:list', 'Collection', 4, 'ACTIVE');

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, status) VALUES
(140401, 1404, '规则维护',   'BUTTON', NULL, NULL, 'hr:social:rule:save', NULL, 1, 'ACTIVE'),
(140402, 1404, '规则删除',   'BUTTON', NULL, NULL, 'hr:social:rule:del',  NULL, 2, 'ACTIVE'),
(140403, 1404, '缴费核算',   'BUTTON', NULL, NULL, 'hr:social:calculate', NULL, 3, 'ACTIVE'),
(140404, 1404, '明细查看',   'BUTTON', NULL, NULL, 'hr:social:detail:list', NULL, 4, 'ACTIVE');

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 100000 + m.id, 1001, m.id
FROM sys_menu m
WHERE m.id IN (1404, 140401, 140402, 140403, 140404);
