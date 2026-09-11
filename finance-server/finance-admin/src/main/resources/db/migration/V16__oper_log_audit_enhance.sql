-- =============================================================
-- V16: sys_oper_log 审计增强（S4 操作日志）
-- 背景：V1 已建 12 列表（无五件套/耗时/结果），实体继承 BaseEntity 需五件套列；
--      补齐硬约束 18 五件套 + cost_time/json_result + CHECK + 查询索引。
-- 说明：全部幂等（ADD COLUMN IF NOT EXISTS / CHECK DO 块 / CREATE INDEX IF NOT EXISTS）。
-- =============================================================
ALTER TABLE sys_oper_log ADD COLUMN IF NOT EXISTS cost_time     BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sys_oper_log ADD COLUMN IF NOT EXISTS json_result   TEXT;
ALTER TABLE sys_oper_log ADD COLUMN IF NOT EXISTS oper_location VARCHAR(128);
ALTER TABLE sys_oper_log ADD COLUMN IF NOT EXISTS create_by     VARCHAR(64);
ALTER TABLE sys_oper_log ADD COLUMN IF NOT EXISTS create_time   TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE sys_oper_log ADD COLUMN IF NOT EXISTS update_by     VARCHAR(64);
ALTER TABLE sys_oper_log ADD COLUMN IF NOT EXISTS update_time   TIMESTAMPTZ;
ALTER TABLE sys_oper_log ADD COLUMN IF NOT EXISTS deleted       SMALLINT NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_sys_oper_log_status') THEN
        ALTER TABLE sys_oper_log ADD CONSTRAINT ck_sys_oper_log_status CHECK (status IN (0, 1));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_sys_oper_log_deleted') THEN
        ALTER TABLE sys_oper_log ADD CONSTRAINT ck_sys_oper_log_deleted CHECK (deleted IN (0, 1));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_sys_oper_log_oper_time ON sys_oper_log (oper_time DESC);
CREATE INDEX IF NOT EXISTS idx_sys_oper_log_oper_name ON sys_oper_log (oper_name);
CREATE INDEX IF NOT EXISTS idx_sys_oper_log_title      ON sys_oper_log (title);

COMMENT ON COLUMN sys_oper_log.cost_time     IS '执行耗时（毫秒）';
COMMENT ON COLUMN sys_oper_log.json_result   IS '返回结果（截断/脱敏）';
COMMENT ON COLUMN sys_oper_log.oper_location IS '操作地点（IP 属地）';
COMMENT ON COLUMN sys_oper_log.create_by     IS '创建人';
COMMENT ON COLUMN sys_oper_log.create_time   IS '创建时间';
COMMENT ON COLUMN sys_oper_log.update_by     IS '更新人';
COMMENT ON COLUMN sys_oper_log.update_time   IS '更新时间';
COMMENT ON COLUMN sys_oper_log.deleted       IS '逻辑删除标记（0=正常，1=删除；操作日志只增不删）';
