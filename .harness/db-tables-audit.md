# 数据库表清单对照审计（db-tables-audit）

> 对照 docs/财务管理系统功能开发文档.html 中引用的表名 与 V1__init_schema.sql 实际实现的表名。
> 结论：V1 为**权威实现**（已合入且被 Flyway checksum 锁定），文档表名为早期草案；差异在此显式登记，业务开发一律使用 V1 实际表名。
> 覆盖缺口（文档需要但 V1 未建）列入 V3+ 迁移计划。

## 命名差异（文档名 → V1 实际名）

| 文档引用 | V1 实际表 | 差异说明 | 处理 |
|----------|-----------|----------|------|
| fin_account_subject | fin_subject | 科目表名 | 以 V1 为准 |
| sys_operation_log | sys_oper_log | 操作日志表名 | 以 V1 为准 |
| fin_expense_claim / fin_expense_item | fin_reimburse / fin_reimburse_detail | 报销单/明细 | 以 V1 为准 |
| fin_receivable_payable | fin_ar + fin_ap | 文档合并应收应付，V1 拆两表（各自状态/核销约束更严） | 以 V1 为准 |
| fin_payment_receipt | fin_payment + fin_receipt | 文档合并收付款，V1 拆两表 | 以 V1 为准 |
| ct_contract（前缀 ct_） | ctr_contract（前缀 ctr_） | 合同表前缀统一 ctr_ | 以 V1 为准 |
| ct_contract_payment_plan | ctr_payment_plan | 收付款计划 | 以 V1 为准 |
| ct_contract_attachment | sys_attachment（biz_type='contract'） | 附件统一表 | 以 V1 为准 |
| wf_instance / wf_task | wf_process_instance（+ Flowable ACT_* 表） | 流程实例关联表；任务在 Flowable ACT_RU_TASK | 以 V1 为准 |
| base_department | hr_department | 部门表归属人事域 | 以 V1 为准 |
| fin_balance_initial | （V1 无） | 期初余额表 | **缺口 → V3** |
| fin_bank_account | （V1 无） | 银行账户表（收付款关联） | **缺口 → V3** |
| 社保表（单位/个人金额） | （V1 无，hr_salary 已含个人社保/公积金字段） | 社保明细 | 缺口待定（V1 字段已覆盖个人部分） |
| seq_voucher_no（序列） | （V1 无独立序列对象） | 凭证号占号：建议应用层唯一约束 + 重试，不建序列表（R05 已由唯一约束兜底） | 以应用层实现为准 |

## 覆盖核对（V1 33 表 vs 文档功能域）

| 域 | 文档功能点 | V1 覆盖 | 缺口 |
|----|-----------|---------|------|
| 系统 | 用户/角色/菜单/字典/日志/登录 | sys_user/sys_role/sys_user_role/sys_menu/sys_role_menu/sys_dict_type/sys_dict_data/sys_oper_log/sys_login_log | 无 |
| 主数据 | 公司/科目/期间 | fin_company/fin_subject/fin_period | 期初余额（V3） |
| 财务 | 凭证/账簿/结账/报表/报销/收付款/映射引擎 | fin_voucher/fin_voucher_entry/fin_voucher_rule/fin_reimburse(+detail)/fin_ar/fin_ap/fin_payment/fin_receipt | 银行账户（V3） |
| 合同 | 台账/计划/记录 | ctr_contract/ctr_payment_plan/ctr_payment_record | 无（附件走 sys_attachment） |
| 人事 | 部门/员工/考勤/工资 | hr_department/hr_employee/hr_attendance/hr_salary/hr_salary_item | 社保明细（待定） |
| 流程/公共 | 审批关联/消息/附件/通知 | wf_process_instance/sys_message/sys_attachment/sys_notice | 无 |

## V3+ 迁移计划（Gate 6 按需执行）

1. `V3__fin_balance_initial.sql`：期初余额表（company_code + period + subject 唯一，借/贷方金额）
2. `V4__fin_bank_account.sql`：银行账户表（company_code + 账号唯一，收付款/核销关联）
3. （待定）社保明细表（若 hr_salary_item 无法表达单位/个人拆分）

> 规则：任何新增/调整表结构 → 新增 `V{n}__*.sql`，禁止改已合入脚本；执行前本地跑通（db-migrate.ps1），并同步 rule-registry.md。
