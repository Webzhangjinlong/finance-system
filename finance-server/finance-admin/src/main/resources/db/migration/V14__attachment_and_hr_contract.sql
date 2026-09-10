-- =============================================================
-- V14: W3 附件上传（MinIO）+ H4 劳动合同到期提醒
-- 1) sys_attachment 附件元数据表（五件套 + 公司隔离）
-- 2) hr_employee 补 contract_expire_date（docs 6.1：劳动合同到期前 30 天提醒 HR）
-- 3) 菜单 1507 附件管理 + 按钮 150701/150702 + 授权 admin（复用 sys_menu id=1507 空闲区间，V11 已占 1500-1506）
-- =============================================================

CREATE TABLE sys_attachment (
    id           BIGINT PRIMARY KEY,
    company_code VARCHAR(20)  NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    object_name  VARCHAR(255) NOT NULL,
    bucket       VARCHAR(64)  NOT NULL,
    url          VARCHAR(512),
    file_size    BIGINT       NOT NULL,
    content_type VARCHAR(128),
    biz_type     VARCHAR(32),
    biz_id       BIGINT,
    create_by    VARCHAR(64),
    create_time  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    update_by    VARCHAR(64),
    update_time  TIMESTAMPTZ,
    deleted      SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE  sys_attachment IS '附件元数据（MinIO 对象，凭证/报销/合同/员工等复用）';
COMMENT ON COLUMN sys_attachment.object_name IS 'MinIO 对象名：company_code/yyyyMM/uuid.ext';
COMMENT ON COLUMN sys_attachment.biz_type IS '业务类型（VOUCHER/EXPENSE/CONTRACT/EMPLOYEE 等）';
CREATE INDEX idx_sys_attachment_biz ON sys_attachment (company_code, biz_type, biz_id);

ALTER TABLE hr_employee ADD COLUMN contract_expire_date DATE;
COMMENT ON COLUMN hr_employee.contract_expire_date IS '劳动合同到期日（到期前 30 天站内提醒 HR）';

-- 菜单：1507 附件管理（父=1500 系统管理）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, status)
VALUES (1507, 1500, '附件管理', 'DIR', '/system/attachment', 'system/attachment/index', 'system:attachment:list', 'Folder', 7, 'ACTIVE');
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, perms, sort_order, status)
VALUES (150701, 1507, '附件上传', 'BUTTON', 'system:attachment:upload', 1, 'ACTIVE');
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, perms, sort_order, status)
VALUES (150702, 1507, '附件下载', 'BUTTON', 'system:attachment:download', 2, 'ACTIVE');

-- 授权 admin 角色（1001）
INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 100000 + m.id, 1001, m.id FROM sys_menu m WHERE m.id IN (1507, 150701, 150702);
