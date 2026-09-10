-- ============================================================
-- V9__message_update_columns.sql  Gate 10 补充
-- V1 建 sys_message 时遗漏 update_by/update_time（公共五件套 R18 一致性）；
-- V8 已应用不可修改，故以 V9 补齐。
-- ============================================================

ALTER TABLE sys_message ADD COLUMN update_by VARCHAR(64);
ALTER TABLE sys_message ADD COLUMN update_time TIMESTAMPTZ DEFAULT now();
