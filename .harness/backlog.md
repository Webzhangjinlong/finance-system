# 功能开发 Backlog（Gate 6+ 执行清单）

> 提取自 docs/财务管理系统功能开发文档.html；表名以 V1 实际实现为准（差异见 db-tables-audit.md）。
> 每完成一个功能点：状态 → ✅，并在 rule-registry 核对对应规则是否生效。
> 优先级：P0 核心（先做）/ P1 扩展。

## 系统管理（com.finance.system）

| # | 功能点 | 接口（路径） | 权限码 | 状态 |
|---|--------|--------------|--------|------|
| S1 | 登录认证（BCrypt + JWT 2h + 5 次锁定 15min） | POST /auth/login、POST /auth/logout、GET /auth/profile | 公开 | ✅ Gate 6 |
| S2 | 用户/角色/菜单 CRUD + 分配 + 重置密码 | GET/POST/PUT/DELETE /system/user、/role、/menu | system:user:list/add/edit/del 等 | ✅ Gate 14 #20 |
| S3 | 字典管理（两级 + Redis 缓存失效） | GET/POST /system/dict | system:dict:* | ⬜ |
| S4 | 操作日志（只增不删，审计写操作） | GET /system/log | system:log:list | ⬜ |

## 财务管理（com.finance.finance）——核心

| # | 功能点 | 接口（路径） | 权限码 | 状态 |
|---|--------|--------------|--------|------|
| F1 | 科目管理（树形/末级记账/删除保护/期初导入） | GET /subject/tree、POST /subject、PUT/DELETE /subject/{id}、POST /subject/import | finance:subject:add/edit/del/import | ✅ Gate 6（期初导入待 V3） |
| F2 | 凭证管理（录入/审核/过账/冲销，凭证号占号） | GET/POST /voucher、PUT /voucher/{id}/audit、/book、/reverse、DELETE（草稿） | finance:voucher:add/edit；audit；book/reverse | ✅ Gate 6 |
| F3 | 账簿查询（总账/明细账/日记账 + Excel） | GET /book/{type}?period=&subjectId= | finance:book:list | ✅ Gate 7 |
| F4 | 期末结账（校验未过账/试算平衡/损益结转/反结账） | PUT /period/close、PUT /period/reopen、GET /period/list | finance:period:close/reopen | ✅ Gate 7 |
| F5 | 财务报表（资产负债表/利润表/现金流量表） | GET /report/balance-sheet、/income、/cash-flow | finance:report:list | ✅ Gate 12 #17 |
| F6 | 费用报销（提交→审批→财务审核→打款→凭证） | GET/POST /expense、PUT /expense/{id}/submit、/approve、/reject、/pay | 发起(员工)/审批(部门负责人)/审核打款(财务) | ✅ Gate 13 #19 |
| F7 | 收付款与应收应付（核销/账龄预警） | GET/POST /receivable、/payment、PUT /payment/{id}/confirm、GET /receivable/aging | 登记核销(出纳)/查看确认(财务经理) | ✅ Gate 12 #17（账龄 /finance/ar|ap/aging） |
| F8 | 凭证映射引擎（事件监听→规则→草稿，幂等） | GET/POST /voucher-rule | 规则维护(财务经理)；触发系统内部 | ⬜ |

## 合同管理（com.finance.contract）

| # | 功能点 | 接口（路径） | 权限码 | 状态 |
|---|--------|--------------|--------|------|
| C1 | 合同台账（编号/类型/金额/状态流转/删除保护） | GET/POST /contract、PUT /contract/{id}、POST /contract/{id}/submit、/void | 维护(合同管理员/业务) | ✅ Gate 8 |
| C2 | 合同审批（Flowable 按金额分级，一期单人） | POST /contract/{id}/submit；GET /workflow/todo、PUT /contract/task/{id}/approve\|reject | 按流程节点角色 | ✅ Gate 8 |
| C3 | 收付款计划（生效自动生成/到期生成应收应付/进度视图） | GET/POST /contract/{id}/payment-plans | finance:receivable:list / contract:plan:edit | ✅（Gate 9） |
| C4 | 到期提醒（定时：合同 30 天/计划 7 天/应收逾期） | GET /message/list、GET /message/unread-count、PUT /message/{id}/read、/read-all | 登录用户本人（ReminderTask 08:00/08:30 内部） | ✅ Gate 10 |

## 人事管理（com.finance.hr）

| # | 功能点 | 接口（路径） | 权限码 | 状态 |
|---|--------|--------------|--------|------|
| H1 | 部门/员工档案（工号唯一/离职停用/数据权限） | GET/POST /hr/department、/hr/employee、PUT /hr/employee/{id}/status | hr:employee:add/edit（HR） | ⬜ |
| H2 | 考勤（月度/批量导入/供工资取数） | GET/POST /hr/attendance、POST /hr/attendance/import | hr:attendance:edit（HR） | ⬜ |
| H3 | 工资核算与工资条（累计预扣个税/流程/工资条本人可见） | POST /salary/calculate、GET /salary/list、POST /salary/{id}/submit、/approve、/pay；GET /salary/{id}/slip | 核算(HR)/复核(财务)/slip(本人) | ⬜ |

## 公共支撑（com.finance.workflow + framework）

| # | 功能点 | 接口（路径） | 权限码 | 状态 |
|---|--------|--------------|--------|------|
| W1 | 审批流封装（WorkflowService：start/todo/approve/reject；单人顺序审批一期） | 内部 Service + /workflow/* | 按流程节点 | ✅ Gate 7 |
| W2 | 消息通知/待办聚合 | GET /message/list、GET /message/unread-count、PUT /message/{id}/read、/read-all | system:message:list（本人隔离） | ✅ Gate 10 |
| W3 | 附件上传（MinIO + 类型/大小校验） | POST /common/upload | 登录用户 | ⬜ |
| W4 | 定时任务（@Scheduled：到期提醒 08:00、逾期扫描 08:30） | 内部（com.finance.task.ReminderTask） | 系统 | ✅ Gate 10 |

## Gate 6 首批建议（认证 + 科目/凭证）

1. **S1 登录认证**（JWT 签发/校验 + Security 收紧白名单）——所有接口的前置
2. **F1 科目管理**（树形 + 末级记账校验 + 期初导入）
3. **F2 凭证管理**（录入/审核/过账/冲销 + 凭证号占号 + 幂等）

> 每个功能点的验收标准：对应 AGENTS.md 硬约束 + 单测覆盖（借贷平衡/并发占号/幂等/状态机）+ ArchUnit 通过。
