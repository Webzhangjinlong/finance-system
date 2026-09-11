-- =============================================================
-- V15: sys_login_log 补公共五件套（硬约束 18：create_by/create_time/update_by/update_time/deleted + 逻辑删除）
-- 背景：V1 建表仅 id/username/.../login_time，实体继承 BaseEntity（INSERT 写五件套列）→ 缺列导致登录日志写入被吞
-- 说明：全部 IF NOT EXISTS 幂等；create_by 可空（登录时刻无登录上下文）
-- =============================================================
ALTER TABLE sys_login_log ADD COLUMN IF NOT EXISTS create_by   VARCHAR(64);
ALTER TABLE sys_login_log ADD COLUMN IF NOT EXISTS create_time TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE sys_login_log ADD COLUMN IF NOT EXISTS update_by   VARCHAR(64);
ALTER TABLE sys_login_log ADD COLUMN IF NOT EXISTS update_time TIMESTAMPTZ;
ALTER TABLE sys_login_log ADD COLUMN IF NOT EXISTS deleted     SMALLINT NOT NULL DEFAULT 0;
COMMENT ON COLUMN sys_login_log.create_by    IS '创建人';
COMMENT ON COLUMN sys_login_log.create_time  IS '创建时间';
COMMENT ON COLUMN sys_login_log.update_by    IS '更新人';
COMMENT ON COLUMN sys_login_log.update_time  IS '更新时间';
COMMENT ON COLUMN sys_login_log.deleted      IS '逻辑删除标记（0=正常，1=删除）';
