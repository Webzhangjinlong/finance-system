-- ============================================================
-- V1__init_schema.sql  财务管理系统初始 Schema
-- 约定（AGENTS.md 硬约束 18-20）：
--   * 主键 BIGINT 雪花 ID（应用 ASSIGN_ID 生成）
--   * 金额 NUMERIC(18,2)，禁止 float
--   * 时间 TIMESTAMPTZ
--   * 公共五件套：create_by / create_time / update_by / update_time / deleted（逻辑删除 0/1）
--   * 业务表必含 company_code VARCHAR(32) NOT NULL（多公司隔离）
--   * 核心规则落到 CHECK / 唯一约束；索引按查询路径
--   * RLS 已启用；注意：RLS 对表属主（postgres）不生效，
--     生产环境需使用专用应用账号并 FORCE ROW LEVEL SECURITY，开发期由应用拦截器兜底
-- ============================================================

-- ============ 一、系统域（全局，无 company_code） ============

CREATE TABLE sys_user (
    id            BIGINT PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL,
    password      VARCHAR(128) NOT NULL,
    nickname      VARCHAR(64),
    email         VARCHAR(128),
    phone         VARCHAR(20),
    avatar        VARCHAR(255),
    status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    login_ip      VARCHAR(64),
    login_date    TIMESTAMPTZ,
    create_by     VARCHAR(64),
    create_time   TIMESTAMPTZ DEFAULT now(),
    update_by     VARCHAR(64),
    update_time   TIMESTAMPTZ DEFAULT now(),
    deleted       SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uq_sys_user_username UNIQUE (username),
    CONSTRAINT ck_sys_user_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_sys_user_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE sys_user IS '用户';

CREATE TABLE sys_role (
    id          BIGINT PRIMARY KEY,
    role_name   VARCHAR(64) NOT NULL,
    role_key    VARCHAR(64) NOT NULL,
    role_sort   INT         NOT NULL DEFAULT 0,
    status      VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    remark      VARCHAR(255),
    create_by   VARCHAR(64),
    create_time TIMESTAMPTZ DEFAULT now(),
    update_by   VARCHAR(64),
    update_time TIMESTAMPTZ DEFAULT now(),
    deleted     SMALLINT    NOT NULL DEFAULT 0,
    CONSTRAINT uq_sys_role_key UNIQUE (role_key),
    CONSTRAINT ck_sys_role_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_sys_role_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE sys_role IS '角色';

CREATE TABLE sys_user_role (
    id         BIGINT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    role_id    BIGINT NOT NULL,
    create_by  VARCHAR(64),
    create_time TIMESTAMPTZ DEFAULT now(),
    deleted    SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_sys_user_role UNIQUE (user_id, role_id),
    CONSTRAINT ck_sys_user_role_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE sys_user_role IS '用户-角色关联';

CREATE TABLE sys_menu (
    id          BIGINT PRIMARY KEY,
    parent_id   BIGINT       NOT NULL DEFAULT 0,
    menu_name   VARCHAR(64)  NOT NULL,
    menu_type   VARCHAR(16)  NOT NULL,
    path        VARCHAR(255),
    component   VARCHAR(255),
    perms       VARCHAR(128),
    icon        VARCHAR(64),
    sort_order  INT          NOT NULL DEFAULT 0,
    visible     SMALLINT     NOT NULL DEFAULT 1,
    status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    create_by   VARCHAR(64),
    create_time TIMESTAMPTZ  DEFAULT now(),
    update_by   VARCHAR(64),
    update_time TIMESTAMPTZ  DEFAULT now(),
    deleted     SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT ck_sys_menu_type CHECK (menu_type IN ('DIR', 'MENU', 'BUTTON')),
    CONSTRAINT ck_sys_menu_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_sys_menu_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE sys_menu IS '菜单/权限';

CREATE TABLE sys_role_menu (
    id          BIGINT PRIMARY KEY,
    role_id     BIGINT NOT NULL,
    menu_id     BIGINT NOT NULL,
    create_by   VARCHAR(64),
    create_time TIMESTAMPTZ DEFAULT now(),
    deleted     SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_sys_role_menu UNIQUE (role_id, menu_id),
    CONSTRAINT ck_sys_role_menu_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE sys_role_menu IS '角色-菜单关联';

CREATE TABLE sys_dict_type (
    id          BIGINT PRIMARY KEY,
    dict_name   VARCHAR(64) NOT NULL,
    dict_type   VARCHAR(64) NOT NULL,
    status      VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    remark      VARCHAR(255),
    create_by   VARCHAR(64),
    create_time TIMESTAMPTZ DEFAULT now(),
    update_by   VARCHAR(64),
    update_time TIMESTAMPTZ DEFAULT now(),
    deleted     SMALLINT    NOT NULL DEFAULT 0,
    CONSTRAINT uq_sys_dict_type UNIQUE (dict_type),
    CONSTRAINT ck_sys_dict_type_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_sys_dict_type_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE sys_dict_type IS '字典类型';

CREATE TABLE sys_dict_data (
    id          BIGINT PRIMARY KEY,
    dict_type   VARCHAR(64) NOT NULL,
    dict_label  VARCHAR(64) NOT NULL,
    dict_value  VARCHAR(64) NOT NULL,
    dict_sort   INT         NOT NULL DEFAULT 0,
    status      VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    create_by   VARCHAR(64),
    create_time TIMESTAMPTZ DEFAULT now(),
    update_by   VARCHAR(64),
    update_time TIMESTAMPTZ DEFAULT now(),
    deleted     SMALLINT    NOT NULL DEFAULT 0,
    CONSTRAINT ck_sys_dict_data_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_sys_dict_data_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE sys_dict_data IS '字典数据';

CREATE TABLE sys_oper_log (
    id            BIGINT PRIMARY KEY,
    title         VARCHAR(64),
    business_type VARCHAR(32),
    method        VARCHAR(255),
    request_method VARCHAR(16),
    oper_name     VARCHAR(64),
    oper_url      VARCHAR(255),
    oper_ip       VARCHAR(64),
    oper_param    TEXT,
    status        SMALLINT NOT NULL DEFAULT 0,
    error_msg     TEXT,
    oper_time     TIMESTAMPTZ DEFAULT now()
);
COMMENT ON TABLE sys_oper_log IS '操作日志';

CREATE TABLE sys_login_log (
    id             BIGINT PRIMARY KEY,
    username       VARCHAR(64),
    login_ip       VARCHAR(64),
    login_location VARCHAR(128),
    browser        VARCHAR(64),
    os             VARCHAR(64),
    status         SMALLINT NOT NULL DEFAULT 0,
    msg            VARCHAR(255),
    login_time     TIMESTAMPTZ DEFAULT now()
);
COMMENT ON TABLE sys_login_log IS '登录日志';

-- ============ 二、主数据（带 company_code） ============

CREATE TABLE fin_company (
    id               BIGINT PRIMARY KEY,
    company_code     VARCHAR(32)  NOT NULL,
    company_name     VARCHAR(128) NOT NULL,
    legal_person     VARCHAR(64),
    tax_no           VARCHAR(64),
    address          VARCHAR(255),
    phone            VARCHAR(32),
    status           VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    fiscal_year_start INT         NOT NULL DEFAULT 1,
    create_by        VARCHAR(64),
    create_time      TIMESTAMPTZ  DEFAULT now(),
    update_by        VARCHAR(64),
    update_time      TIMESTAMPTZ  DEFAULT now(),
    deleted          SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uq_fin_company_code UNIQUE (company_code),
    CONSTRAINT ck_fin_company_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_fin_company_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE fin_company IS '公司（账套）主数据';

CREATE TABLE fin_subject (
    id           BIGINT PRIMARY KEY,
    company_code VARCHAR(32)  NOT NULL,
    subject_code VARCHAR(32)  NOT NULL,
    subject_name VARCHAR(128) NOT NULL,
    subject_type VARCHAR(16)  NOT NULL,
    direction    VARCHAR(8)   NOT NULL,
    parent_id    BIGINT       NOT NULL DEFAULT 0,
    subject_level INT         NOT NULL DEFAULT 1,
    is_leaf      SMALLINT     NOT NULL DEFAULT 1,
    status       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    create_by    VARCHAR(64),
    create_time  TIMESTAMPTZ  DEFAULT now(),
    update_by    VARCHAR(64),
    update_time  TIMESTAMPTZ  DEFAULT now(),
    deleted      SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uq_fin_subject UNIQUE (company_code, subject_code),
    CONSTRAINT ck_fin_subject_type CHECK (subject_type IN ('ASSET', 'LIABILITY', 'EQUITY', 'COST', 'PROFIT')),
    CONSTRAINT ck_fin_subject_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_fin_subject_leaf CHECK (is_leaf IN (0, 1)),
    CONSTRAINT ck_fin_subject_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_fin_subject_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE fin_subject IS '会计科目';

CREATE TABLE fin_period (
    id            BIGINT PRIMARY KEY,
    company_code  VARCHAR(32) NOT NULL,
    period_year   INT         NOT NULL,
    period_month  INT         NOT NULL,
    period_status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    start_date    DATE        NOT NULL,
    end_date      DATE        NOT NULL,
    closed_by     VARCHAR(64),
    closed_at     TIMESTAMPTZ,
    create_by     VARCHAR(64),
    create_time   TIMESTAMPTZ DEFAULT now(),
    update_by     VARCHAR(64),
    update_time   TIMESTAMPTZ DEFAULT now(),
    deleted       SMALLINT    NOT NULL DEFAULT 0,
    CONSTRAINT uq_fin_period UNIQUE (company_code, period_year, period_month),
    CONSTRAINT ck_fin_period_status CHECK (period_status IN ('OPEN', 'CLOSING', 'CLOSED')),
    CONSTRAINT ck_fin_period_month CHECK (period_month BETWEEN 1 AND 12),
    CONSTRAINT ck_fin_period_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE fin_period IS '会计期间';

-- ============ 三、财务域 ============

CREATE TABLE fin_voucher (
    id             BIGINT PRIMARY KEY,
    company_code   VARCHAR(32)  NOT NULL,
    period_year    INT          NOT NULL,
    period_month   INT          NOT NULL,
    voucher_no     VARCHAR(32)  NOT NULL,
    voucher_date   DATE         NOT NULL,
    voucher_status VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    total_debit    NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_credit   NUMERIC(18,2) NOT NULL DEFAULT 0,
    source_type    VARCHAR(32),
    source_id      BIGINT,
    attach_count   INT          NOT NULL DEFAULT 0,
    auditor        VARCHAR(64),
    audited_at     TIMESTAMPTZ,
    booker         VARCHAR(64),
    booked_at      TIMESTAMPTZ,
    remark         VARCHAR(255),
    create_by      VARCHAR(64),
    create_time    TIMESTAMPTZ  DEFAULT now(),
    update_by      VARCHAR(64),
    update_time    TIMESTAMPTZ  DEFAULT now(),
    deleted        SMALLINT     NOT NULL DEFAULT 0,
    -- 硬约束 3：借贷平衡（DB 兜底）
    CONSTRAINT ck_fin_voucher_balance CHECK (total_debit >= 0 AND total_credit >= 0 AND total_debit = total_credit),
    -- 硬约束 4：状态机合法取值
    CONSTRAINT ck_fin_voucher_status CHECK (voucher_status IN ('DRAFT', 'AUDITED', 'BOOKED', 'REVERSED')),
    -- 硬约束 5：凭证号 公司+期间+序列 唯一
    CONSTRAINT uq_fin_voucher_no UNIQUE (company_code, period_year, period_month, voucher_no),
    -- 硬约束 7：业务单据生成凭证幂等（部分唯一索引，仅业务来源生效）
    CONSTRAINT ck_fin_voucher_period_month CHECK (period_month BETWEEN 1 AND 12),
    CONSTRAINT ck_fin_voucher_deleted CHECK (deleted IN (0, 1))
);
CREATE UNIQUE INDEX uq_fin_voucher_source ON fin_voucher (source_type, source_id) WHERE source_type IS NOT NULL;
CREATE INDEX idx_fin_voucher_company_period ON fin_voucher (company_code, period_year, period_month, voucher_status);
COMMENT ON TABLE fin_voucher IS '记账凭证';

CREATE TABLE fin_voucher_entry (
    id            BIGINT PRIMARY KEY,
    company_code  VARCHAR(32)  NOT NULL,
    voucher_id    BIGINT       NOT NULL,
    subject_id    BIGINT       NOT NULL,
    subject_code  VARCHAR(32)  NOT NULL,
    summary       VARCHAR(255),
    debit_amount  NUMERIC(18,2) NOT NULL DEFAULT 0,
    credit_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    create_by     VARCHAR(64),
    create_time   TIMESTAMPTZ  DEFAULT now(),
    update_by     VARCHAR(64),
    update_time   TIMESTAMPTZ  DEFAULT now(),
    deleted       SMALLINT     NOT NULL DEFAULT 0,
    -- 硬约束 3：金额非负、单行只能借方或贷方、不能为空行
    CONSTRAINT ck_fin_entry_amount CHECK (
        debit_amount >= 0 AND credit_amount >= 0
        AND (debit_amount > 0 OR credit_amount > 0)
        AND NOT (debit_amount > 0 AND credit_amount > 0)
    ),
    CONSTRAINT ck_fin_entry_deleted CHECK (deleted IN (0, 1))
);
CREATE INDEX idx_fin_entry_voucher ON fin_voucher_entry (voucher_id);
CREATE INDEX idx_fin_entry_subject ON fin_voucher_entry (company_code, subject_code);
COMMENT ON TABLE fin_voucher_entry IS '凭证分录';

CREATE TABLE fin_voucher_rule (
    id            BIGINT PRIMARY KEY,
    company_code  VARCHAR(32) NOT NULL,
    rule_code     VARCHAR(64) NOT NULL,
    rule_name     VARCHAR(128) NOT NULL,
    source_type   VARCHAR(32) NOT NULL,
    trigger_event VARCHAR(32) NOT NULL,
    entry_rule    JSONB       NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    remark        VARCHAR(255),
    create_by     VARCHAR(64),
    create_time   TIMESTAMPTZ DEFAULT now(),
    update_by     VARCHAR(64),
    update_time   TIMESTAMPTZ DEFAULT now(),
    deleted       SMALLINT    NOT NULL DEFAULT 0,
    CONSTRAINT uq_fin_voucher_rule UNIQUE (company_code, rule_code),
    CONSTRAINT ck_fin_voucher_rule_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_fin_voucher_rule_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE fin_voucher_rule IS '凭证映射规则（业财一体化引擎）';

CREATE TABLE fin_reimburse (
    id            BIGINT PRIMARY KEY,
    company_code  VARCHAR(32)  NOT NULL,
    reimburse_no  VARCHAR(32)  NOT NULL,
    employee_id   BIGINT       NOT NULL,
    dept_name     VARCHAR(128),
    amount        NUMERIC(18,2) NOT NULL,
    expense_date  DATE         NOT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    voucher_id    BIGINT,
    paid_at       TIMESTAMPTZ,
    remark        VARCHAR(255),
    create_by     VARCHAR(64),
    create_time   TIMESTAMPTZ  DEFAULT now(),
    update_by     VARCHAR(64),
    update_time   TIMESTAMPTZ  DEFAULT now(),
    deleted       SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uq_fin_reimburse_no UNIQUE (company_code, reimburse_no),
    CONSTRAINT ck_fin_reimburse_amount CHECK (amount > 0),
    CONSTRAINT ck_fin_reimburse_status CHECK (status IN ('DRAFT', 'APPROVING', 'APPROVED', 'PAID', 'REJECTED')),
    CONSTRAINT ck_fin_reimburse_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE fin_reimburse IS '费用报销单';

CREATE TABLE fin_reimburse_detail (
    id            BIGINT PRIMARY KEY,
    company_code  VARCHAR(32)  NOT NULL,
    reimburse_id  BIGINT       NOT NULL,
    expense_type  VARCHAR(32)  NOT NULL,
    summary       VARCHAR(255),
    amount        NUMERIC(18,2) NOT NULL,
    invoice_no    VARCHAR(64),
    create_by     VARCHAR(64),
    create_time   TIMESTAMPTZ  DEFAULT now(),
    update_by     VARCHAR(64),
    update_time   TIMESTAMPTZ  DEFAULT now(),
    deleted       SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT ck_fin_reimburse_detail_amount CHECK (amount > 0),
    CONSTRAINT ck_fin_reimburse_detail_deleted CHECK (deleted IN (0, 1))
);
CREATE INDEX idx_fin_reimburse_detail_master ON fin_reimburse_detail (reimburse_id);
COMMENT ON TABLE fin_reimburse_detail IS '报销明细';

CREATE TABLE fin_ar (
    id             BIGINT PRIMARY KEY,
    company_code   VARCHAR(32)  NOT NULL,
    ar_no          VARCHAR(32)  NOT NULL,
    contract_id    BIGINT,
    customer_name  VARCHAR(128) NOT NULL,
    amount         NUMERIC(18,2) NOT NULL,
    received_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    status         VARCHAR(16)  NOT NULL DEFAULT 'OPEN',
    due_date       DATE,
    voucher_id     BIGINT,
    create_by      VARCHAR(64),
    create_time    TIMESTAMPTZ  DEFAULT now(),
    update_by      VARCHAR(64),
    update_time    TIMESTAMPTZ  DEFAULT now(),
    deleted        SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uq_fin_ar_no UNIQUE (company_code, ar_no),
    CONSTRAINT ck_fin_ar_amount CHECK (amount > 0),
    CONSTRAINT ck_fin_ar_received CHECK (received_amount >= 0 AND received_amount <= amount),
    CONSTRAINT ck_fin_ar_status CHECK (status IN ('OPEN', 'PARTIAL', 'SETTLED', 'BADDEBT')),
    CONSTRAINT ck_fin_ar_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE fin_ar IS '应收账款';

CREATE TABLE fin_ap (
    id             BIGINT PRIMARY KEY,
    company_code   VARCHAR(32)  NOT NULL,
    ap_no          VARCHAR(32)  NOT NULL,
    contract_id    BIGINT,
    supplier_name  VARCHAR(128) NOT NULL,
    amount         NUMERIC(18,2) NOT NULL,
    paid_amount    NUMERIC(18,2) NOT NULL DEFAULT 0,
    status         VARCHAR(16)  NOT NULL DEFAULT 'OPEN',
    due_date       DATE,
    voucher_id     BIGINT,
    create_by      VARCHAR(64),
    create_time    TIMESTAMPTZ  DEFAULT now(),
    update_by      VARCHAR(64),
    update_time    TIMESTAMPTZ  DEFAULT now(),
    deleted        SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uq_fin_ap_no UNIQUE (company_code, ap_no),
    CONSTRAINT ck_fin_ap_amount CHECK (amount > 0),
    CONSTRAINT ck_fin_ap_paid CHECK (paid_amount >= 0 AND paid_amount <= amount),
    CONSTRAINT ck_fin_ap_status CHECK (status IN ('OPEN', 'PARTIAL', 'SETTLED')),
    CONSTRAINT ck_fin_ap_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE fin_ap IS '应付账款';

CREATE TABLE fin_payment (
    id            BIGINT PRIMARY KEY,
    company_code  VARCHAR(32)  NOT NULL,
    payment_no    VARCHAR(32)  NOT NULL,
    payment_type  VARCHAR(32)  NOT NULL,
    related_type  VARCHAR(32),
    related_id    BIGINT,
    payee_name    VARCHAR(128) NOT NULL,
    amount        NUMERIC(18,2) NOT NULL,
    payment_date  DATE         NOT NULL,
    method        VARCHAR(16)  NOT NULL DEFAULT 'BANK',
    status        VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    voucher_id    BIGINT,
    remark        VARCHAR(255),
    create_by     VARCHAR(64),
    create_time   TIMESTAMPTZ  DEFAULT now(),
    update_by     VARCHAR(64),
    update_time   TIMESTAMPTZ  DEFAULT now(),
    deleted       SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uq_fin_payment_no UNIQUE (company_code, payment_no),
    CONSTRAINT ck_fin_payment_amount CHECK (amount > 0),
    CONSTRAINT ck_fin_payment_method CHECK (method IN ('CASH', 'BANK', 'OTHER')),
    CONSTRAINT ck_fin_payment_status CHECK (status IN ('DRAFT', 'APPROVED', 'PAID', 'REJECTED')),
    CONSTRAINT ck_fin_payment_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE fin_payment IS '付款单';

CREATE TABLE fin_receipt (
    id            BIGINT PRIMARY KEY,
    company_code  VARCHAR(32)  NOT NULL,
    receipt_no    VARCHAR(32)  NOT NULL,
    receipt_type  VARCHAR(32)  NOT NULL,
    related_type  VARCHAR(32),
    related_id    BIGINT,
    payer_name    VARCHAR(128) NOT NULL,
    amount        NUMERIC(18,2) NOT NULL,
    receipt_date  DATE         NOT NULL,
    method        VARCHAR(16)  NOT NULL DEFAULT 'BANK',
    status        VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    voucher_id    BIGINT,
    remark        VARCHAR(255),
    create_by     VARCHAR(64),
    create_time   TIMESTAMPTZ  DEFAULT now(),
    update_by     VARCHAR(64),
    update_time   TIMESTAMPTZ  DEFAULT now(),
    deleted       SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uq_fin_receipt_no UNIQUE (company_code, receipt_no),
    CONSTRAINT ck_fin_receipt_amount CHECK (amount > 0),
    CONSTRAINT ck_fin_receipt_method CHECK (method IN ('CASH', 'BANK', 'OTHER')),
    CONSTRAINT ck_fin_receipt_status CHECK (status IN ('DRAFT', 'APPROVED', 'PAID', 'REJECTED')),
    CONSTRAINT ck_fin_receipt_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE fin_receipt IS '收款单';

-- ============ 四、合同域 ============

CREATE TABLE ctr_contract (
    id                 BIGINT PRIMARY KEY,
    company_code       VARCHAR(32)  NOT NULL,
    contract_no        VARCHAR(32)  NOT NULL,
    contract_name      VARCHAR(128) NOT NULL,
    counterparty       VARCHAR(128) NOT NULL,
    contract_type      VARCHAR(16)  NOT NULL,
    amount             NUMERIC(18,2) NOT NULL,
    sign_date          DATE,
    start_date         DATE,
    end_date           DATE,
    status             VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    process_instance_id BIGINT,
    remark             VARCHAR(255),
    create_by          VARCHAR(64),
    create_time        TIMESTAMPTZ  DEFAULT now(),
    update_by          VARCHAR(64),
    update_time        TIMESTAMPTZ  DEFAULT now(),
    deleted            SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uq_ctr_contract_no UNIQUE (company_code, contract_no),
    CONSTRAINT ck_ctr_contract_type CHECK (contract_type IN ('SALES', 'PURCHASE', 'OTHER')),
    CONSTRAINT ck_ctr_contract_amount CHECK (amount >= 0),
    CONSTRAINT ck_ctr_contract_status CHECK (status IN ('DRAFT', 'APPROVING', 'APPROVED', 'ACTIVE', 'COMPLETED', 'TERMINATED')),
    CONSTRAINT ck_ctr_contract_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE ctr_contract IS '合同';

CREATE TABLE ctr_payment_plan (
    id             BIGINT PRIMARY KEY,
    company_code   VARCHAR(32)  NOT NULL,
    contract_id    BIGINT       NOT NULL,
    plan_type      VARCHAR(16)  NOT NULL,
    plan_no        VARCHAR(32)  NOT NULL,
    plan_date      DATE         NOT NULL,
    amount         NUMERIC(18,2) NOT NULL,
    paid_amount    NUMERIC(18,2) NOT NULL DEFAULT 0,
    status         VARCHAR(16)  NOT NULL DEFAULT 'UNPAID',
    reminder_sent  SMALLINT     NOT NULL DEFAULT 0,
    create_by      VARCHAR(64),
    create_time    TIMESTAMPTZ  DEFAULT now(),
    update_by      VARCHAR(64),
    update_time    TIMESTAMPTZ  DEFAULT now(),
    deleted        SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uq_ctr_plan_no UNIQUE (company_code, plan_no),
    CONSTRAINT ck_ctr_plan_type CHECK (plan_type IN ('PAYMENT', 'RECEIPT')),
    CONSTRAINT ck_ctr_plan_amount CHECK (amount > 0),
    CONSTRAINT ck_ctr_plan_paid CHECK (paid_amount >= 0 AND paid_amount <= amount),
    CONSTRAINT ck_ctr_plan_status CHECK (status IN ('UNPAID', 'PARTIAL', 'PAID', 'OVERDUE')),
    CONSTRAINT ck_ctr_plan_deleted CHECK (deleted IN (0, 1))
);
CREATE INDEX idx_ctr_plan_contract ON ctr_payment_plan (company_code, contract_id);
COMMENT ON TABLE ctr_payment_plan IS '合同收付款计划';

CREATE TABLE ctr_payment_record (
    id            BIGINT PRIMARY KEY,
    company_code  VARCHAR(32)  NOT NULL,
    plan_id       BIGINT       NOT NULL,
    record_no     VARCHAR(32)  NOT NULL,
    payment_type  VARCHAR(16)  NOT NULL,
    amount        NUMERIC(18,2) NOT NULL,
    payment_date  DATE         NOT NULL,
    method        VARCHAR(16)  NOT NULL DEFAULT 'BANK',
    voucher_id    BIGINT,
    remark        VARCHAR(255),
    create_by     VARCHAR(64),
    create_time   TIMESTAMPTZ  DEFAULT now(),
    update_by     VARCHAR(64),
    update_time   TIMESTAMPTZ  DEFAULT now(),
    deleted       SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uq_ctr_record_no UNIQUE (company_code, record_no),
    CONSTRAINT ck_ctr_record_type CHECK (payment_type IN ('PAYMENT', 'RECEIPT')),
    CONSTRAINT ck_ctr_record_amount CHECK (amount > 0),
    CONSTRAINT ck_ctr_record_method CHECK (method IN ('CASH', 'BANK', 'OTHER')),
    CONSTRAINT ck_ctr_record_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE ctr_payment_record IS '合同收付款记录';

-- ============ 五、人事域 ============

CREATE TABLE hr_department (
    id           BIGINT PRIMARY KEY,
    company_code VARCHAR(32)  NOT NULL,
    dept_name    VARCHAR(128) NOT NULL,
    parent_id    BIGINT       NOT NULL DEFAULT 0,
    leader_id    BIGINT,
    sort_order   INT          NOT NULL DEFAULT 0,
    status       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    create_by    VARCHAR(64),
    create_time  TIMESTAMPTZ  DEFAULT now(),
    update_by    VARCHAR(64),
    update_time  TIMESTAMPTZ  DEFAULT now(),
    deleted      SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT ck_hr_dept_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_hr_dept_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE hr_department IS '部门';

CREATE TABLE hr_employee (
    id             BIGINT PRIMARY KEY,
    company_code   VARCHAR(32)  NOT NULL,
    emp_no         VARCHAR(32)  NOT NULL,
    emp_name       VARCHAR(64)  NOT NULL,
    gender         VARCHAR(8),
    id_card        VARCHAR(32),
    phone          VARCHAR(20),
    email          VARCHAR(128),
    dept_id        BIGINT,
    position       VARCHAR(64),
    hire_date      DATE,
    leave_date     DATE,
    salary_account VARCHAR(32),
    status         VARCHAR(16)  NOT NULL DEFAULT 'ONBOARD',
    user_id        BIGINT,
    create_by      VARCHAR(64),
    create_time    TIMESTAMPTZ  DEFAULT now(),
    update_by      VARCHAR(64),
    update_time    TIMESTAMPTZ  DEFAULT now(),
    deleted        SMALLINT     NOT NULL DEFAULT 0,
    -- 硬约束：工号 公司内唯一
    CONSTRAINT uq_hr_employee_no UNIQUE (company_code, emp_no),
    CONSTRAINT ck_hr_employee_gender CHECK (gender IN ('MALE', 'FEMALE') OR gender IS NULL),
    CONSTRAINT ck_hr_employee_status CHECK (status IN ('ONBOARD', 'LEAVE')),
    CONSTRAINT ck_hr_employee_deleted CHECK (deleted IN (0, 1))
);
CREATE INDEX idx_hr_employee_dept ON hr_employee (company_code, dept_id);
COMMENT ON TABLE hr_employee IS '员工';

CREATE TABLE hr_attendance (
    id           BIGINT PRIMARY KEY,
    company_code VARCHAR(32) NOT NULL,
    employee_id  BIGINT      NOT NULL,
    work_date    DATE        NOT NULL,
    check_in     TIMESTAMPTZ,
    check_out    TIMESTAMPTZ,
    status       VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    create_by    VARCHAR(64),
    create_time  TIMESTAMPTZ DEFAULT now(),
    update_by    VARCHAR(64),
    update_time  TIMESTAMPTZ DEFAULT now(),
    deleted      SMALLINT    NOT NULL DEFAULT 0,
    CONSTRAINT uq_hr_attendance UNIQUE (company_code, employee_id, work_date),
    CONSTRAINT ck_hr_attendance_status CHECK (status IN ('NORMAL', 'LATE', 'EARLY', 'ABSENT', 'LEAVE')),
    CONSTRAINT ck_hr_attendance_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE hr_attendance IS '考勤';

CREATE TABLE hr_salary (
    id               BIGINT PRIMARY KEY,
    company_code     VARCHAR(32)  NOT NULL,
    employee_id      BIGINT       NOT NULL,
    salary_year      INT          NOT NULL,
    salary_month     INT          NOT NULL,
    base_salary      NUMERIC(18,2) NOT NULL DEFAULT 0,
    bonus            NUMERIC(18,2) NOT NULL DEFAULT 0,
    allowance        NUMERIC(18,2) NOT NULL DEFAULT 0,
    overtime_pay     NUMERIC(18,2) NOT NULL DEFAULT 0,
    social_security  NUMERIC(18,2) NOT NULL DEFAULT 0,
    housing_fund     NUMERIC(18,2) NOT NULL DEFAULT 0,
    tax              NUMERIC(18,2) NOT NULL DEFAULT 0,
    other_deduct     NUMERIC(18,2) NOT NULL DEFAULT 0,
    net_pay          NUMERIC(18,2) NOT NULL DEFAULT 0,
    status           VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    paid_at          TIMESTAMPTZ,
    create_by        VARCHAR(64),
    create_time      TIMESTAMPTZ  DEFAULT now(),
    update_by        VARCHAR(64),
    update_time      TIMESTAMPTZ  DEFAULT now(),
    deleted          SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uq_hr_salary UNIQUE (company_code, employee_id, salary_year, salary_month),
    CONSTRAINT ck_hr_salary_month CHECK (salary_month BETWEEN 1 AND 12),
    CONSTRAINT ck_hr_salary_amounts CHECK (
        base_salary >= 0 AND bonus >= 0 AND allowance >= 0 AND overtime_pay >= 0
        AND social_security >= 0 AND housing_fund >= 0 AND tax >= 0 AND other_deduct >= 0
    ),
    CONSTRAINT ck_hr_salary_status CHECK (status IN ('DRAFT', 'CONFIRMED', 'PAID')),
    CONSTRAINT ck_hr_salary_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE hr_salary IS '工资';

CREATE TABLE hr_salary_item (
    id           BIGINT PRIMARY KEY,
    company_code VARCHAR(32)  NOT NULL,
    salary_id    BIGINT       NOT NULL,
    item_code    VARCHAR(32)  NOT NULL,
    item_name    VARCHAR(64)  NOT NULL,
    amount       NUMERIC(18,2) NOT NULL,
    item_type    VARCHAR(16)  NOT NULL,
    create_by    VARCHAR(64),
    create_time  TIMESTAMPTZ  DEFAULT now(),
    deleted      SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT ck_hr_salary_item_type CHECK (item_type IN ('EARNING', 'DEDUCTION')),
    CONSTRAINT ck_hr_salary_item_deleted CHECK (deleted IN (0, 1))
);
CREATE INDEX idx_hr_salary_item_master ON hr_salary_item (salary_id);
COMMENT ON TABLE hr_salary_item IS '工资明细项';

-- ============ 六、流程/公共 ============

CREATE TABLE wf_process_instance (
    id                 BIGINT PRIMARY KEY,
    company_code       VARCHAR(32) NOT NULL,
    process_type       VARCHAR(32) NOT NULL,
    business_type      VARCHAR(32) NOT NULL,
    business_id        BIGINT      NOT NULL,
    process_def_key    VARCHAR(64),
    process_instance_id VARCHAR(64),
    status             VARCHAR(16) NOT NULL DEFAULT 'RUNNING',
    current_approver   VARCHAR(64),
    started_by         VARCHAR(64),
    started_at         TIMESTAMPTZ DEFAULT now(),
    finished_at        TIMESTAMPTZ,
    create_by          VARCHAR(64),
    create_time        TIMESTAMPTZ DEFAULT now(),
    update_by          VARCHAR(64),
    update_time        TIMESTAMPTZ DEFAULT now(),
    deleted            SMALLINT    NOT NULL DEFAULT 0,
    CONSTRAINT uq_wf_business UNIQUE (business_type, business_id),
    CONSTRAINT ck_wf_status CHECK (status IN ('RUNNING', 'APPROVED', 'REJECTED', 'CANCELED')),
    CONSTRAINT ck_wf_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE wf_process_instance IS '业务-流程实例关联';

CREATE TABLE sys_message (
    id           BIGINT PRIMARY KEY,
    company_code VARCHAR(32) NOT NULL,
    receiver_id  BIGINT      NOT NULL,
    message_type VARCHAR(32) NOT NULL,
    title        VARCHAR(128),
    content      TEXT,
    is_read      SMALLINT    NOT NULL DEFAULT 0,
    read_at      TIMESTAMPTZ,
    business_type VARCHAR(32),
    business_id  BIGINT,
    create_by    VARCHAR(64),
    create_time  TIMESTAMPTZ DEFAULT now(),
    deleted      SMALLINT    NOT NULL DEFAULT 0,
    CONSTRAINT ck_sys_message_read CHECK (is_read IN (0, 1)),
    CONSTRAINT ck_sys_message_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE sys_message IS '站内消息';

CREATE TABLE sys_attachment (
    id           BIGINT PRIMARY KEY,
    company_code VARCHAR(32)  NOT NULL,
    biz_type     VARCHAR(32)  NOT NULL,
    biz_id       BIGINT       NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    file_url     VARCHAR(512) NOT NULL,
    file_size    BIGINT       NOT NULL DEFAULT 0,
    file_type    VARCHAR(64),
    uploader     VARCHAR(64),
    uploaded_at  TIMESTAMPTZ  DEFAULT now(),
    create_by    VARCHAR(64),
    create_time  TIMESTAMPTZ  DEFAULT now(),
    deleted      SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT ck_sys_attachment_deleted CHECK (deleted IN (0, 1))
);
CREATE INDEX idx_sys_attachment_biz ON sys_attachment (company_code, biz_type, biz_id);
COMMENT ON TABLE sys_attachment IS '附件';

CREATE TABLE sys_notice (
    id             BIGINT PRIMARY KEY,
    company_code   VARCHAR(32) NOT NULL,
    notice_type    VARCHAR(32) NOT NULL,
    title          VARCHAR(128) NOT NULL,
    content        TEXT,
    receiver_scope VARCHAR(16) NOT NULL DEFAULT 'ALL',
    receiver_ids   TEXT,
    remind_at      TIMESTAMPTZ,
    is_sent        SMALLINT    NOT NULL DEFAULT 0,
    sent_at        TIMESTAMPTZ,
    status         VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    create_by      VARCHAR(64),
    create_time    TIMESTAMPTZ DEFAULT now(),
    update_by      VARCHAR(64),
    update_time    TIMESTAMPTZ DEFAULT now(),
    deleted        SMALLINT    NOT NULL DEFAULT 0,
    CONSTRAINT ck_sys_notice_scope CHECK (receiver_scope IN ('ALL', 'ROLE', 'DEPT', 'USER')),
    CONSTRAINT ck_sys_notice_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_sys_notice_sent CHECK (is_sent IN (0, 1)),
    CONSTRAINT ck_sys_notice_deleted CHECK (deleted IN (0, 1))
);
COMMENT ON TABLE sys_notice IS '通知提醒';

-- ============ 七、行级安全（RLS） ============
-- 说明：RLS 对表属主（postgres）不生效。开发期由应用 company 拦截器兜底；
-- 生产环境需为应用创建专用账号并 FORCE ROW LEVEL SECURITY 后生效。

DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'fin_subject','fin_period','fin_voucher','fin_voucher_entry','fin_voucher_rule',
        'fin_reimburse','fin_reimburse_detail','fin_ar','fin_ap','fin_payment','fin_receipt',
        'ctr_contract','ctr_payment_plan','ctr_payment_record',
        'hr_department','hr_employee','hr_attendance','hr_salary','hr_salary_item',
        'wf_process_instance','sys_message','sys_attachment','sys_notice'
    ]
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY;', t);
        EXECUTE format(
            'CREATE POLICY %I ON %I USING (company_code = current_setting(''app.company_code'', true));',
            t || '_company_policy', t);
    END LOOP;
END $$;
