-- ============================================================
-- V8__reminder.sql  Gate 10 到期提醒（W4）+ 消息中心（W2）
-- 1) sys_message 增加 remind_date + 幂等唯一约束（按 单据+日期+类型 去重，docs 5.4）
-- 2) 接收人查询索引
-- 3) 菜单 1506 消息中心（system:message:list）+ 授权 admin
-- ============================================================

-- ---------- 消息幂等键：company + receiver + type + business + remind_date ----------
ALTER TABLE sys_message ADD COLUMN remind_date DATE;
ALTER TABLE sys_message ADD CONSTRAINT uq_sys_message_remind
    UNIQUE (company_code, receiver_id, message_type, business_type, business_id, remind_date);
CREATE INDEX idx_sys_message_receiver ON sys_message (company_code, receiver_id, is_read);

-- ---------- 消息中心菜单（1506，1501-1505 已占用） ----------
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, status) VALUES
(1506, 1500, '消息中心', 'MENU', '/system/message', 'system/message/index', 'system:message:list', NULL, 6, 'ACTIVE');

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 100000 + m.id, 1001, m.id FROM sys_menu m WHERE m.id = 1506;
