-- ============================================================
-- V4__fin_balance_initial.sql  期初余额表（Gate 7 F3 账簿取数）
-- 背景：总账需展示期初/本期/期末；期初数据由建账导入（fin_subject.import 待 V3 期初导入）
-- 规则：科目 + 期间唯一；期初借/贷分列（与分录口径一致），余额方向由科目 direction 决定
-- ============================================================

CREATE TABLE fin_balance_initial (
    id            BIGINT PRIMARY KEY,
    company_code  VARCHAR(32)  NOT NULL,
    period_year   INT          NOT NULL,
    period_month  INT          NOT NULL,
    subject_id    BIGINT       NOT NULL,
    subject_code  VARCHAR(32)  NOT NULL,
    initial_debit  NUMERIC(18,2) NOT NULL DEFAULT 0,
    initial_credit NUMERIC(18,2) NOT NULL DEFAULT 0,
    create_by     VARCHAR(64),
    create_time   TIMESTAMPTZ  DEFAULT now(),
    update_by     VARCHAR(64),
    update_time   TIMESTAMPTZ  DEFAULT now(),
    deleted       SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uq_fin_balance_initial UNIQUE (company_code, period_year, period_month, subject_id),
    CONSTRAINT ck_fin_balance_amount CHECK (
        initial_debit >= 0 AND initial_credit >= 0
        AND NOT (initial_debit > 0 AND initial_credit > 0)
    ),
    CONSTRAINT ck_fin_balance_deleted CHECK (deleted IN (0, 1)),
    CONSTRAINT fk_fin_balance_subject FOREIGN KEY (subject_id) REFERENCES fin_subject (id)
);

COMMENT ON TABLE fin_balance_initial IS '科目期初余额（公司+期间+科目唯一，借/贷分列）';
