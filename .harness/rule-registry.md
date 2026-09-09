# 规则注册表（Rule Registry）

> 本表登记所有已落地为**机器强制**的规则（DB 约束 / CI 门禁 / 代码检查），
> 纯文档类约束不在此列。来源：AGENTS.md 硬约束 + docs/开发约束与流程规范.html。
> 每条规则注明：载体（哪里强制）、验证方式、生效状态。

## 已注册规则

| # | 规则 | 载体 | 验证方式 | 状态 |
|---|------|------|----------|------|
| R01 | 金额一律 `NUMERIC(18,2)`/BigDecimal，禁止 float | DB DDL + AGENTS.md #1 | `ck_*_amount` CHECK + 代码评审 | ✅ 生效 |
| R02 | 业务表必含 `company_code`，公司隔离 | DB DDL（23 表）+ RLS + 应用拦截器（待建） | 表结构校验 + RLS policy 存在性 | ✅ 生效 |
| R03 | 凭证借贷必平衡 | DB CHECK `ck_fin_voucher_balance` + 服务校验（待建） | 违规 INSERT 被拒（已实测） | ✅ 生效 |
| R04 | 凭证状态机 DRAFT→AUDITED→BOOKED→REVERSED | DB CHECK `ck_fin_voucher_status` | 非法状态被拒 | ✅ 生效 |
| R05 | 凭证号（公司+期间+序列）唯一 | DB 唯一约束 `uq_fin_voucher_no` | 重复凭证号被拒（已实测） | ✅ 生效 |
| R06 | 业务单据→凭证幂等 | DB 部分唯一索引 `uq_fin_voucher_source` | 重复 source_type+source_id 被拒 | ✅ 生效 |
| R07 | 分录金额>0 且单行仅借或仅贷 | DB CHECK `ck_fin_entry_amount` | 违规 INSERT 被拒 | ✅ 生效 |
| R08 | 仅末级科目可记账 | 服务层校验（待建，Gate 6） | 待实现 | ⏳ 未生效 |
| R09 | 已结账期间只读 | 服务层校验（待建） | 待实现 | ⏳ 未生效 |
| R10 | 收付款/核销不超额 | DB CHECK `ck_fin_ar_received` / `ck_fin_ap_paid` / `ck_ctr_plan_paid` + 服务校验（待建） | 超额核销被拒 | ✅ 生效（DB 层） |
| R11 | 凭证号/公司编码/工号唯一 | `uq_fin_voucher_no` / `uq_fin_company_code` / `uq_hr_employee_no` 等 | 重复 INSERT 被拒 | ✅ 生效 |
| R12 | 结构变更只走 Flyway | `db/migration/V*.sql` + flyway_schema_history | `flyway migrate` 校验 | ✅ 生效 |
| R13 | 主键雪花 ID / TIMESTAMPTZ / 公共五件套 / 逻辑删除 | DB DDL 全表 | 表结构校验 | ✅ 生效 |
| R14 | 分页结构统一 records/total/page/size | 代码规范（Gate 4 起 ArchUnit 校验） | 待实现 | ⏳ 未生效 |
| R15 | 依赖方向 Controller→Service→Mapper | ArchUnit（Gate 4 起） | 待实现 | ⏳ 未生效 |
| R16 | 合入受保护 main（评审+禁直推） | GitHub 分支保护 + `tools/merge-pr.ps1` | PR 流程 | ✅ 生效 |

## 规则注册流程

1. 新增/变更业务规则 → 同步更新 AGENTS.md 硬约束 + 本表。
2. 规则若可落到 DB/CI/代码检查，必须落地；纯文档规则标注"文档约束"。
3. 每个 Gate 完成后，将新生效规则从"⏳ 未生效"改为"✅ 生效"。
