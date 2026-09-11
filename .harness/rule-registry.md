# 规则注册表（Rule Registry）

> 本表登记所有已落地为**机器强制**的规则（DB 约束 / CI 门禁 / 代码检查），
> 纯文档类约束不在此列。来源：AGENTS.md 硬约束 + docs/开发约束与流程规范.html。
> 每条规则注明：载体（哪里强制）、验证方式、生效状态。

## 已注册规则

| # | 规则 | 载体 | 验证方式 | 状态 |
|---|------|------|----------|------|
| R01 | 金额一律 `NUMERIC(18,2)`/BigDecimal，禁止 float | DB DDL + ArchUnit | `ck_*_amount` CHECK + `business_fields_must_not_be_float_or_double`（CI 强制） | ✅ 生效 |
| R02 | 业务表必含 `company_code`，公司隔离 | DB DDL（23 表）+ RLS + 应用拦截器（待建） | 表结构校验 + RLS policy 存在性 | ✅ 生效 |
| R03 | 凭证借贷必平衡 | DB CHECK `ck_fin_voucher_balance` + 服务校验（Gate 6 已实现） | 违规 INSERT 被拒 + VoucherServiceTest.createUnbalanced_rejected | ✅ 生效 |
| R04 | 凭证状态机 DRAFT→AUDITED→BOOKED→REVERSED | DB CHECK `ck_fin_voucher_status` + 服务校验（Gate 6 已实现） | 非法状态被拒 + VoucherServiceTest.fullLifecycle | ✅ 生效 |
| R05 | 凭证号（公司+期间+序列）唯一 | DB 唯一约束 `uq_fin_voucher_no` + 服务占号重试（Gate 6） | 重复凭证号被拒（已实测）+ duplicateVoucherNo_rejectedByDbUniqueConstraint | ✅ 生效 |
| R06 | 业务单据→凭证幂等 | DB 部分唯一索引 `uq_fin_voucher_source` + 服务前置校验（Gate 6） | 重复 source_type+source_id 被拒 + createWithSameSource_rejectedIdempotent | ✅ 生效 |
| R07 | 分录金额>0 且单行仅借或仅贷 | DB CHECK `ck_fin_entry_amount` + 服务校验（Gate 6） | 违规 INSERT 被拒 + VoucherServiceTest.createEntryBothSides | ✅ 生效 |
| R08 | 仅末级科目可记账 | 服务层校验（Gate 6 VoucherService 已实现） | VoucherServiceTest.createNonLeafSubject_rejected | ✅ 生效 |
| R09 | 已结账期间只读 | 服务层校验（Gate 6 VoucherService 已实现） | VoucherServiceTest.createInClosedPeriod_rejected | ✅ 生效 |
| R10 | 收付款/核销不超额 | DB CHECK `ck_fin_ar_received` / `ck_fin_ap_paid` / `ck_ctr_plan_paid` + 服务校验（待建） | 超额核销被拒 | ✅ 生效（DB 层） |
| R11 | 凭证号/公司编码/工号唯一 | `uq_fin_voucher_no` / `uq_fin_company_code` / `uq_hr_employee_no` 等 | 重复 INSERT 被拒 | ✅ 生效 |
| R12 | 结构变更只走 Flyway | `db/migration/V*.sql` + flyway_schema_history + CI 上下文测试 | `mvn verify`（测试迁移至最新） | ✅ 生效 |
| R13 | 主键雪花 ID / TIMESTAMPTZ / 公共五件套 / 逻辑删除 | DB DDL 全表 | 表结构校验 | ✅ 生效 |
| R14 | 分页结构统一 records/total/page/size | PageResult（Gate 6 common 已建）+ 服务层落地 | VoucherService.page 返回 PageResult | ✅ 生效 |
| R15 | 依赖方向 Controller→Service→Mapper | **ArchUnit `layered_dependencies` 等 3 条 + CI 强制** | `mvn verify`（CI 门禁） | ✅ 生效 |
| R16 | 跨模块禁止直调他人 Mapper | **ArchUnit `no_cross_module_mapper_dependency` + CI** | `mvn verify` | ✅ 生效 |
| R17 | framework 不得依赖业务模块 | **ArchUnit `framework_must_not_depend_on_business_modules` + CI** | `mvn verify` | ✅ 生效 |
| R18 | 分层类命名规范（Controller/Service/Mapper 后缀） | **ArchUnit 命名 3 条 + CI** | `mvn verify` | ✅ 生效 |
| R19 | CI 门禁：合入 main 前必须通过 backend-ci + frontend-ci | **GitHub 分支保护 required status checks** | PR 状态检查 | ✅ 生效 |
| R20 | 合入受保护 main（评审 + 禁直推/强推/删除） | GitHub 分支保护 + `tools/merge-pr.ps1` | PR 流程（自动 bypass 后恢复并验证） | ✅ 生效 |
| R21 | 测试必须真实落库（Flyway + PG），禁止 mock 掉迁移 | CI services.postgres + 上下文测试 | `mvn verify` | ✅ 生效 |
| R22 | 登录失败 5 次锁定 15 分钟 | RedisLoginFailCounter（Gate 6 已实现） | AuthServiceTest.loginWhenLocked | ✅ 生效 |
| R23 | 密码 BCrypt + JWT 无状态 2h | BCryptPasswordEncoder + JwtUtils（Gate 6 已实现） | AuthServiceTest.loginSuccess | ✅ 生效 |
| R24 | Controller 权限码 @PreAuthorize（白名单除外） | SecurityConfig + @EnableMethodSecurity（Gate 6 已实现） | 接口鉴权（未授权 403） | ✅ 生效 |
| R25 | 科目删除保护（子科目/发生额禁止删） | SubjectService（Gate 6 已实现） | SubjectServiceTest.deleteWithChildren / deleteWithVoucherEntries | ✅ 生效 |
| R26 | 流程实例幂等：同一业务单（business_type+business_id）唯一 | DB 唯一约束 `uq_wf_business`（V1）+ 服务前置校验（Gate 7 W1 已实现） | 重复发起被拒 + WorkflowServiceTest.start_duplicate_rejected | ✅ 生效 |
| R27 | 账簿只读已过账凭证、按公司隔离 | BookQueryMapper 聚合 SQL（Gate 7 F3，仅 BOOKED/REVERSED + company_code 过滤） | BookServiceTest 7 例（总账/明细/日记/Excel/过滤） | ✅ 生效 |
| R28 | 结账门禁：无未过账凭证 + 试算平衡（借余==贷余）+ 损益结转幂等（source=PERIOD_CLOSE）+ 结账期间只读 | PeriodService（Gate 7 F4 已实现） | PeriodServiceTest 10 例（校验/结转/幂等/反结账） | ✅ 生效 |
| R29 | **例外登记**：Flowable ACT_* schema 由引擎自管理（`flowable.database-schema-update=true`），不纳入 Flyway 管理；业务表（fin_/wf_/sys_/ctr_/hr_）仍全走 Flyway V* 迁移 | application.yml / application-test.yml（Gate 7 已统一启用） | CI 上下文测试断言 Flyway 版本 v6 + 启动无 act_ge_property 报错 | ✅ 生效（显式例外） |
| R30 | 合同编号（公司+年份+序列）唯一 | DB 唯一约束 `uq_ctr_contract_no` + 服务重试换号（Gate 8 C1）；编号查询**绕过逻辑删除过滤**（唯一约束作用于全表） | 重复编号被拒 + ContractServiceTest.create_generatesContractNoAndDraft / create_invalidType_rejectedByDbCheck | ✅ 生效 |
| R31 | 合同删除保护：非 DRAFT 禁删，只能作废 | ContractService（Gate 8 C1 已实现） | ContractServiceTest.delete_draftOnly_deleteProtection / voidContract_terminatesApproved | ✅ 生效 |
| R32 | 合同审批联动：DRAFT→APPROVING→APPROVED(生成计划)/REJECTED(回 DRAFT)，流程幂等 | ContractService + WorkflowService 回调（Gate 8 C2 已实现） | ContractServiceTest.submit_startsApproval / approveTask_activatesContractAndGeneratesPlan / rejectTask_returnsToDraft | ✅ 生效 |
| R33 | 收付款计划生成幂等：合同生效仅生成一次 | ContractService.generatePaymentPlans（按 contract_id 前置计数 + plan_no 唯一，Gate 8 已实现） | ContractServiceTest.approveTask_planGeneratedOnce | ✅ 生效 |

## 规则注册流程

1. 新增/变更业务规则 → 同步更新 AGENTS.md 硬约束 + 本表。
2. 规则若可落到 DB/CI/代码检查，必须落地；纯文档规则标注"文档约束"。
3. 每个 Gate 完成后，将新生效规则从"⏳ 未生效"改为"✅ 生效"。
| R34 | 应收/应付生成幂等：计划到期同步只生成一次（company+plan_id 唯一） | V7 uq_fin_ar_plan/uq_fin_ap_plan 唯一约束 + ArApService.generateAr/ApFromPlan 查存在即返回 + DuplicateKey 兜底（Gate 9 C3） | ContractPlanServiceTest.sync_idempotent_noDuplicateAr | ✅ 生效 |
| R35 | 超额核销双重拦截：服务校验（累计后超应收/应付/计划金额抛错）+ DB CHECK 兜底 | ArApService.applyReceipt/applyPayment + ContractPlanService.registerReceipt/registerPayment（Gate 9 C3）；DB：ck_fin_ar_received / ck_fin_ap_paid / ck_ctr_plan_paid | ContractPlanServiceTest.registerReceipt_overpay_rejected | ✅ 生效 |

| R36 | 站内消息幂等：同 公司+接收人+类型+业务单+日期 只提醒一次 | V8 uq_sys_message_remind 唯一约束 + MessageService.send 捕获 DuplicateKey 吞并（Gate 10 W2/W4） | ReminderTaskTest.dueReminder_contractWithin30Days_sendsMessageIdempotent / overdueScan_marksPlanOverdueAndSendsMessage（二次执行不重复） | ✅ 生效 |
| R37 | 计划状态机 OVERDUE 归属：仅 W4 逾期扫描置 OVERDUE（未收付完），核销路径只置 PAID/PARTIAL，禁止手工置逾期 | ContractPlanService.markOverdue 幂等（PAID 跳过）+ registerReceipt/registerPayment 状态回写（Gate 9 约定，Gate 10 落地） | ReminderTaskTest.overdueScan_marksPlanOverdueAndSendsMessage / overdueScan_paidPlanUntouched | ✅ 生效 |
| R38 | 报表取数口径：仅已过账凭证(BOOKED)按科目汇总；资产负债表 资产==负债+权益（含本期净利润）平衡校验 | ReportQueryMapper.selectReportRows（BOOKED+company_code+期间可空）+ ReportService.balanceSheet 平衡断言（Gate 12 F5） | ReportServiceTest.balanceSheet_assetEqualsEquityAndProfit / liabilityIncluded / incomeStatement_revenueMinusExpense / cashFlow_simplifiedByCashSubjects / periodFilterExcludesOtherPeriod | ✅ 生效 |
| R39 | 账龄分档：未到期/0-30/31-60/61-90/90+（按到期日距基准日天数），排除已结清(SETTLED)，余额=金额-已收付 | ArApService.aging（type AR/AP + asOf 可传参，Gate 12 F7） | AgingTest.arAging_bucketsByDueDate / apAging_partialAndBoundary | ✅ 生效 |
| R40 | 费用报销状态机 + 打款凭证幂等：DRAFT→SUBMITTED→APPROVED→PAID（驳回 REJECTED 可重提）；明细仅末级科目且金额>0；打款自动生成凭证（source_type=EXPENSE + source_id 唯一，硬约束 7）；已打款禁改删 | V10 fin_expense_claim/item（CHECK 金额>0 + uk_expense_claim_no + 外键）+ ExpenseService（状态机/末级校验/幂等凭证）+ VoucherService 幂等兜底（Gate 13 F6） | ExpenseServiceTest.submitApprovePay_fullFlow_generatesVoucher / pay_idempotent_noDuplicateVoucher / create_itemSubjectNotLeaf_rejected / update_afterPaid_rejected | ✅ 生效 |
| R41 | 用户/角色/菜单 CRUD + 分配 + 重置密码：密码 BCrypt；用户名/角色标识唯一（逻辑删行查重）；admin 用户/角色禁删禁停用；分配先清后插（物理删旧关联防 uq 残留）；菜单有子禁删；权限变更需重新登录生效（JWT 快照） | V11 系统管理菜单 1000-100303 + 授权 admin + 关联表补五件套列 + SystemUser/Role/MenuService（Gate 14 S2） | SystemServiceTest 13 例（含 assignMenus_permissionsVisibleViaUserPermsQuery 权限联动） | ✅ 生效 |
| R44 | 登录审计日志必须真实落库（R17 审计）：成功 status=1 / 失败 status=0；sys_login_log 含公共五件套列（V15 补列，实体继承 BaseEntity）；login 方法不加 @Transactional —— 失败分支写日志后抛业务异常，若在同一事务内会被回滚导致日志丢失 | V15 迁移补五件套 + AuthService.login 无事务（updateLoginInfo 单条、日志独立提交）| LoginLogWriteTest 3 例（成功落库 status=1 / 失败落库 status=0 / 五件套列存在）+ 浏览器实测失败登录落库 | ✅ 生效 |
| R43 | 附件上传校验 + 合同到期提醒：类型白名单（jpg/png/gif/webp/pdf/doc/xls/docx/xlsx/txt/csv）+ 魔数校验（防伪造扩展名）+ 10MB 上限 + 公司隔离（company_code）；合同到期前 30 天站内提醒 HR（HR 权限用户，同单同日幂等） | AttachmentService（白名单+魔数+大小，W3）+ ReminderTask.contractExpiryReminder（08:10 定时，30 天窗口，HR 权限用户，V8 uq_sys_message_remind 幂等兜底，H4）| AttachmentServiceTest 5 例（成功/拒绝/超大/魔数/空文件）+ ReminderTaskTest.contractExpiryReminder_within30Days_sendsMessageToHrIdempotent / beyond30Days_noMessage | ✅ 生效 |
| R42 | 人事域：部门树删除需无员工引用；工号/考勤(员工+日期)/工资(员工+年月)唯一；工资状态机 DRAFT→CONFIRMED→PAID 且已复核禁改；核算幂等（派生项不进 other_deduct）；工资条仅本人/admin 可见；雪花 ID 全局 Long→String 序列化 | V12/V13 迁移（1403 考勤菜单+按钮+role_menu 授权；hr_salary_item 补审计列）+ DDL 唯一约束/CHECK + SalaryService 幂等/可见性校验 + JacksonConfig（Gate 15 H1-H3） | HrServiceTest 13 例（幂等/缺勤扣款/状态机/工资条可见性）+ 浏览器实测工资条弹窗 | ✅ 生效 |
| R45 | 操作日志审计（硬约束 17 机器化）：所有关键写操作经 @OperLog 注解 + OperLogAspect 自动落库（成功 status=1 / 失败 status=0+error_msg）；敏感键（password/token/phone/idCard 等）一律脱敏 ******；参数/结果截断 2000；审计日志只增不删（无删除接口）；切面自身异常仅 warn 不阻断业务（审计独立于业务事务） | V16 幂等 ALTER sys_oper_log 补列/CHECK/索引 + @OperLog/@OperType（finance-common）+ OperLogAspect（finally 落库）+ 43 个写方法注入 + GET /system/oper-log（system:log:list）+ ArchUnit 分层扩展 Aspect 层（Gate 16 S4） | OperLogAspectTest 3 例（成功+脱敏/失败/分页）+ 浏览器实测成功/失败均落库可见 | ✅ 生效 |
| R47 | 财务分析取数口径（F9）：科目余额表 期初=截止上期累计 BOOKED、本期=期间发生、期末=期初+本期（仅 BOOKED，红冲并入/REVERSED 排除）；按科目 direction 归位借贷栏（DEBIT 正净额记借方、CREDIT 负净额记贷方）；三对合计（期初/本期/期末借==贷）机器断言 balanced；全量科目展示（active 标志）；费用趋势=PROFIT+DEBIT 科目按 科目×月 借方汇总 | ReportQueryMapper.selectReportRowsUpTo/selectExpenseTrend + AnalysisService.trialBalance 平衡断言（Gate 18 F9） | AnalysisServiceTest 5 例（本期平衡/期初排除本期/无发生科目/CREDIT 归位/费用聚合） | ✅ 生效 |
| R46 | 凭证映射引擎（F8）：业务事件→规则→草稿，幂等。规则按 company_code+source_type(+event_type 可空=任意) 匹配启用(ACTIVE)规则，direction DEBIT/CREDIT 决定借贷、subject_code 映射科目（服务层校验存在）、summary_template 渲染 {占位符} 摘要、amount_ratio 金额比例（借贷各自计算）；引擎 resolve 无规则返回空（调用方回退默认映射）；凭证幂等由 VoucherService source_type+source_id 查重兜底（硬约束 7）。规则 CRUD 公司隔离，rule_code 公司内唯一，方向/比例 CHECK 落 DDL | V17 fin_voucher_rule（DROP V1 占位表重建：uk_voucher_rule_code/ck_direction/ck_enabled/ck_ratio + idx_source）+ FinVoucherRuleService/VoucherRuleEngineService + ExpenseService.pay 贷方接入引擎（Gate 17 F8） | FinVoucherRuleServiceTest 6 例（唯一/方向/科目校验/启停/删除）+ VoucherRuleEngineTest 5 例（种子命中/事件过滤/无规则空/模板渲染）+ 报销闭环实测（记-0006 贷1002 模板摘要） | ✅ 生效 |
