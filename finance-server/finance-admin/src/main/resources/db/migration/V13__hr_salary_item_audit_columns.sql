-- ============================================================
-- V13__hr_salary_item_audit_columns.sql  Gate 15 补 hr_salary_item 公共审计列
-- V1 建表时 hr_salary_item 缺 update_by/update_time（五件套不完整），
-- MyBatis-Plus 全局自动填充要求实体字段与表列一致，补列修复。
-- ============================================================

ALTER TABLE hr_salary_item
    ADD COLUMN update_by   VARCHAR(64),
    ADD COLUMN update_time TIMESTAMPTZ DEFAULT now();
