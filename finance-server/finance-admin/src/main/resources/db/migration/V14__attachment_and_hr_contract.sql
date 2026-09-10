-- =============================================================
-- V14: W3 附件上传（MinIO）+ H4 劳动合同到期提醒
-- 1) sys_attachment 附件元数据表：V1 已建基础表（file_url 等），V14 幂等补齐 MinIO 对象列（object_name/bucket/url/content_type）
-- 2) hr_employee 表 contract_expire_date（docs 6.1：劳动合同到期前 30 天提醒 HR）
-- 3) 菜单 1507 附件管理 + 按钮 150701/150702 + 授权 admin（复用 sys_menu id=1507 空闲区间，V11 已占 1500-1506）
-- 说明：全部 IF NOT EXISTS / ON CONFLICT 幂等；file_url/biz_type/biz_id 放开 NOT NULL（实体以 url/object_name 为准）
-- =============================================================

-- 1) sys_attachment：V1 表已存在，幂等补齐列
ALTER TABLE sys_attachment ADD COLUMN IF NOT EXISTS object_name  VARCHAR(255);
ALTER TABLE sys_attachment ADD COLUMN IF NOT EXISTS bucket       VARCHAR(64);
ALTER TABLE sys_attachment ADD COLUMN IF NOT EXISTS url          VARCHAR(512);
ALTER TABLE sys_attachment ADD COLUMN IF NOT EXISTS content_type VARCHAR(128);
ALTER TABLE sys_attachment ADD COLUMN IF NOT EXISTS update_by    VARCHAR(64);
ALTER TABLE sys_attachment ADD COLUMN IF NOT EXISTS update_time  TIMESTAMPTZ;
ALTER TABLE sys_attachment ALTER COLUMN file_url DROP NOT NULL;
ALTER TABLE sys_attachment ALTER COLUMN biz_type DROP NOT NULL;
ALTER TABLE sys_attachment ALTER COLUMN biz_id   DROP NOT NULL;
COMMENT ON TABLE  sys_attachment IS '附件元数据（MinIO 对象，凭证/报销/合同/员工等复用）';
COMMENT ON COLUMN sys_attachment.object_name IS 'MinIO 对象名：company_code/yyyyMM/uuid.ext';
COMMENT ON COLUMN sys_attachment.biz_type IS '业务类型（VOUCHER/EXPENSE/CONTRACT/EMPLOYEE 等）';
CREATE INDEX IF NOT EXISTS idx_sys_attachment_biz ON sys_attachment (company_code, biz_type, biz_id);

-- 2) hr_employee 合同到期日
ALTER TABLE hr_employee ADD COLUMN IF NOT EXISTS contract_expire_date DATE;
COMMENT ON COLUMN hr_employee.contract_expire_date IS '劳动合同到期日（到期前 30 天站内提醒 HR）';

-- 3) 菜单：1507 附件管理（父=1500 系统管理）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, status)
VALUES (1507, 1500, '附件管理', 'DIR', '/system/attachment', 'system/attachment/index', 'system:attachment:list', 'Folder', 7, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, perms, sort_order, status)
VALUES (150701, 1507, '附件上传', 'BUTTON', 'system:attachment:upload', 1, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, perms, sort_order, status)
VALUES (150702, 1507, '附件下载', 'BUTTON', 'system:attachment:download', 2, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 授权 admin 角色（1001）
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 100000 + m.id, 1001, m.id FROM sys_menu m WHERE m.id IN (1507, 150701, 150702)
ON CONFLICT (id) DO NOTHING;
