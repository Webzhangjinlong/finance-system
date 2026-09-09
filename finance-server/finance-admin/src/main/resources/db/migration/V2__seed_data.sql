-- ============================================================
-- V2__seed_data.sql  种子数据（demo 环境）
-- 包含：admin 账号/角色/菜单/字典、DEMO 公司、标准科目模板、会计期间、示例凭证
-- 说明：固定 ID 从 1001 起（雪花 ID 为时间戳大数，不会冲突）
-- ============================================================

-- ---------- 系统：用户 / 角色 / 关联 ----------
INSERT INTO sys_user (id, username, password, nickname, status, create_by, create_time)
VALUES (1001, 'admin', '$2a$10$pfyc7wdPRIp6djh5YN3U8u8Ed82GZBe37SdS6ySP.Eoq.ko/ZJ3pa', '系统管理员', 'ACTIVE', 'seed', now());

INSERT INTO sys_role (id, role_name, role_key, role_sort, status, create_by, create_time)
VALUES (1001, '超级管理员', 'admin', 1, 'ACTIVE', 'seed', now());

INSERT INTO sys_user_role (id, user_id, role_id, create_by, create_time)
VALUES (1001, 1001, 1001, 'seed', now());

-- ---------- 系统：菜单 ----------
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, status) VALUES
(1100, 0,    '工作台',   'MENU',   '/dashboard',       'dashboard/index',        NULL,                 'Odometer',   1, 'ACTIVE'),
(1200, 0,    '财务管理', 'DIR',    '/finance',          NULL,                     NULL,                 'Money',      2, 'ACTIVE'),
(1201, 1200, '科目管理', 'MENU',   '/finance/subject',  'finance/subject/index',  'finance:subject:list',  NULL, 1, 'ACTIVE'),
(1202, 1200, '凭证管理', 'MENU',   '/finance/voucher',  'finance/voucher/index',  'finance:voucher:list',  NULL, 2, 'ACTIVE'),
(1203, 1200, '期末结账', 'MENU',   '/finance/period',   'finance/period/index',   'finance:period:list',   NULL, 3, 'ACTIVE'),
(1204, 1200, '财务报表', 'MENU',   '/finance/report',   'finance/report/index',   'finance:report:list',   NULL, 4, 'ACTIVE'),
(1300, 0,    '合同管理', 'DIR',    '/contract',          NULL,                     NULL,                 'Document',   3, 'ACTIVE'),
(1301, 1300, '合同台账', 'MENU',   '/contract/list',    'contract/list/index',    'contract:list',       NULL, 1, 'ACTIVE'),
(1400, 0,    '人事管理', 'DIR',    '/hr',               NULL,                     NULL,                 'User',       4, 'ACTIVE'),
(1401, 1400, '员工档案', 'MENU',   '/hr/employee',      'hr/employee/index',      'hr:employee:list',    NULL, 1, 'ACTIVE'),
(1402, 1400, '工资管理', 'MENU',   '/hr/salary',        'hr/salary/index',        'hr:salary:list',      NULL, 2, 'ACTIVE'),
(1500, 0,    '系统管理', 'DIR',    '/system',            NULL,                     NULL,                 'Setting',    5, 'ACTIVE'),
(1501, 1500, '用户管理', 'MENU',   '/system/user',      'system/user/index',      'system:user:list',    NULL, 1, 'ACTIVE'),
(1502, 1500, '角色管理', 'MENU',   '/system/role',      'system/role/index',      'system:role:list',    NULL, 2, 'ACTIVE'),
(1503, 1500, '菜单管理', 'MENU',   '/system/menu',      'system/menu/index',      'system:menu:list',    NULL, 3, 'ACTIVE'),
(1504, 1500, '字典管理', 'MENU',   '/system/dict',      'system/dict/index',      'system:dict:list',    NULL, 4, 'ACTIVE'),
(1505, 1500, '操作日志', 'MENU',   '/system/log',       'system/log/index',       'system:log:list',     NULL, 5, 'ACTIVE');

INSERT INTO sys_role_menu (id, role_id, menu_id)
SELECT 100000 + m.id, 1001, m.id FROM sys_menu m;

-- ---------- 系统：字典 ----------
INSERT INTO sys_dict_type (id, dict_name, dict_type, status, create_by, create_time) VALUES
(1001, '用户性别', 'sys_user_gender', 'ACTIVE', 'seed', now()),
(1002, '系统状态', 'sys_normal_disable', 'ACTIVE', 'seed', now());

INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, dict_sort, status, create_by, create_time) VALUES
(1001, 'sys_user_gender',      '男', 'MALE',   1, 'ACTIVE', 'seed', now()),
(1002, 'sys_user_gender',      '女', 'FEMALE', 2, 'ACTIVE', 'seed', now()),
(1003, 'sys_normal_disable',   '正常', 'ACTIVE',   1, 'ACTIVE', 'seed', now()),
(1004, 'sys_normal_disable',   '停用', 'DISABLED', 2, 'ACTIVE', 'seed', now());

-- ---------- 主数据：DEMO 公司 ----------
INSERT INTO fin_company (id, company_code, company_name, legal_person, fiscal_year_start, status, create_by, create_time)
VALUES (1001, 'DEMO', '示例科技有限公司', '张三', 1, 'ACTIVE', 'seed', now());

-- ---------- 主数据：标准科目模板（DEMO 公司，一级科目） ----------
INSERT INTO fin_subject (id, company_code, subject_code, subject_name, subject_type, direction, parent_id, subject_level, is_leaf, status, create_by, create_time) VALUES
(2001, 'DEMO', '1001', '库存现金',     'ASSET',    'DEBIT',  0, 1, 1, 'ACTIVE', 'seed', now()),
(2002, 'DEMO', '1002', '银行存款',     'ASSET',    'DEBIT',  0, 1, 1, 'ACTIVE', 'seed', now()),
(2003, 'DEMO', '1012', '其他货币资金', 'ASSET',    'DEBIT',  0, 1, 1, 'ACTIVE', 'seed', now()),
(2004, 'DEMO', '1122', '应收账款',     'ASSET',    'DEBIT',  0, 1, 1, 'ACTIVE', 'seed', now()),
(2005, 'DEMO', '1123', '预付账款',     'ASSET',    'DEBIT',  0, 1, 1, 'ACTIVE', 'seed', now()),
(2006, 'DEMO', '1221', '其他应收款',   'ASSET',    'DEBIT',  0, 1, 1, 'ACTIVE', 'seed', now()),
(2007, 'DEMO', '1403', '原材料',       'ASSET',    'DEBIT',  0, 1, 1, 'ACTIVE', 'seed', now()),
(2008, 'DEMO', '1601', '固定资产',     'ASSET',    'DEBIT',  0, 1, 1, 'ACTIVE', 'seed', now()),
(2009, 'DEMO', '1602', '累计折旧',     'ASSET',    'CREDIT', 0, 1, 1, 'ACTIVE', 'seed', now()),
(2010, 'DEMO', '2001', '短期借款',     'LIABILITY', 'CREDIT', 0, 1, 1, 'ACTIVE', 'seed', now()),
(2011, 'DEMO', '2202', '应付账款',     'LIABILITY', 'CREDIT', 0, 1, 1, 'ACTIVE', 'seed', now()),
(2012, 'DEMO', '2203', '预收账款',     'LIABILITY', 'CREDIT', 0, 1, 1, 'ACTIVE', 'seed', now()),
(2013, 'DEMO', '2211', '应付职工薪酬', 'LIABILITY', 'CREDIT', 0, 1, 1, 'ACTIVE', 'seed', now()),
(2014, 'DEMO', '2221', '应交税费',     'LIABILITY', 'CREDIT', 0, 1, 1, 'ACTIVE', 'seed', now()),
(2015, 'DEMO', '2241', '其他应付款',   'LIABILITY', 'CREDIT', 0, 1, 1, 'ACTIVE', 'seed', now()),
(2016, 'DEMO', '4001', '实收资本',     'EQUITY',    'CREDIT', 0, 1, 1, 'ACTIVE', 'seed', now()),
(2017, 'DEMO', '4002', '资本公积',     'EQUITY',    'CREDIT', 0, 1, 1, 'ACTIVE', 'seed', now()),
(2018, 'DEMO', '4103', '本年利润',     'EQUITY',    'CREDIT', 0, 1, 1, 'ACTIVE', 'seed', now()),
(2019, 'DEMO', '4104', '利润分配',     'EQUITY',    'CREDIT', 0, 1, 1, 'ACTIVE', 'seed', now()),
(2020, 'DEMO', '5001', '生产成本',     'COST',      'DEBIT',  0, 1, 1, 'ACTIVE', 'seed', now()),
(2021, 'DEMO', '6001', '主营业务收入', 'PROFIT',    'CREDIT', 0, 1, 1, 'ACTIVE', 'seed', now()),
(2022, 'DEMO', '6051', '其他业务收入', 'PROFIT',    'CREDIT', 0, 1, 1, 'ACTIVE', 'seed', now()),
(2023, 'DEMO', '6301', '营业外收入',   'PROFIT',    'CREDIT', 0, 1, 1, 'ACTIVE', 'seed', now()),
(2024, 'DEMO', '6401', '主营业务成本', 'PROFIT',    'DEBIT',  0, 1, 1, 'ACTIVE', 'seed', now()),
(2025, 'DEMO', '6402', '其他业务成本', 'PROFIT',    'DEBIT',  0, 1, 1, 'ACTIVE', 'seed', now()),
(2026, 'DEMO', '6601', '销售费用',     'PROFIT',    'DEBIT',  0, 1, 1, 'ACTIVE', 'seed', now()),
(2027, 'DEMO', '6602', '管理费用',     'PROFIT',    'DEBIT',  0, 1, 1, 'ACTIVE', 'seed', now()),
(2028, 'DEMO', '6603', '财务费用',     'PROFIT',    'DEBIT',  0, 1, 1, 'ACTIVE', 'seed', now()),
(2029, 'DEMO', '6711', '营业外支出',   'PROFIT',    'DEBIT',  0, 1, 1, 'ACTIVE', 'seed', now());

-- ---------- 主数据：会计期间 2026-09（开账） ----------
INSERT INTO fin_period (id, company_code, period_year, period_month, period_status, start_date, end_date, create_by, create_time)
VALUES (1001, 'DEMO', 2026, 9, 'OPEN', '2026-09-01', '2026-09-30', 'seed', now());

-- ---------- 示例凭证（已过账演示，借贷平衡） ----------
INSERT INTO fin_voucher (id, company_code, period_year, period_month, voucher_no, voucher_date, voucher_status, total_debit, total_credit, auditor, audited_at, booker, booked_at, remark, create_by, create_time)
VALUES (1001, 'DEMO', 2026, 9, '记-0001', '2026-09-01', 'BOOKED', 100000.00, 100000.00, 'admin', now(), 'admin', now(), '示例：收到股东投资款', 'seed', now());

INSERT INTO fin_voucher_entry (id, company_code, voucher_id, subject_id, subject_code, summary, debit_amount, credit_amount, create_by, create_time) VALUES
(1001, 'DEMO', 1001, 2002, '1002', '收到股东投资款', 100000.00, 0, 'seed', now()),
(1002, 'DEMO', 1001, 2016, '4001', '收到股东投资款', 0, 100000.00, 'seed', now());
