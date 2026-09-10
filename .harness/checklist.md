# 开发完成检查清单（Checklist）

> 来源：docs/开发约束与流程规范.html 2.2/2.4/2.5 + AGENTS.md 工作流。
> **每次 PR 提交前逐项自查**；任一项不满足禁止合入。

## A. 任务定义

- [ ] 任务拆解 ≤ 1 天，有明确验收标准（对应哪条硬约束 / 哪个接口 / 哪个功能点）
- [ ] 需求与 backlog.md / 功能开发文档对应功能点一致

## B. 上下文与约束

- [ ] 已读 AGENTS.md（Codex 自动加载）与相关 docs/ 章节；**没有凭记忆写代码**
- [ ] 涉及表结构变更：已写 Flyway 迁移（`V{n}__*.sql`），未改已合入脚本（checksum 保护）
- [ ] 涉及新业务规则：已同步 .harness/rule-registry.md

## C. 硬约束自查（违反即禁止交付）

- [ ] 金额一律 BigDecimal / NUMERIC(18,2)，**无 float/double**
- [ ] 业务表操作带 company_code 且按公司隔离（未绕过拦截器/RLS）
- [ ] 凭证借贷平衡（DB CHECK + 服务校验）；状态机 DRAFT→AUDITED→BOOKED→REVERSED
- [ ] 凭证号 公司+期间+序列 唯一；业务单据→凭证幂等（source_type+source_id）
- [ ] 已结账期间只读；收付款/核销不超额
- [ ] 依赖方向 Controller→Service→Mapper；**无跨模块直调他人 Mapper**
- [ ] 代码落在对应模块包（禁止财务逻辑进 system/framework）
- [ ] 写操作 @Transactional + 幂等键；分页返回 records/total/page/size
- [ ] 命名规范（类大驼峰/常量 UPPER_SNAKE/表小写下划线）；无魔法值（枚举/常量）
- [ ] 密码 BCrypt；JWT 无状态；入参 @Valid；SQL 参数化；日志无敏感信息
- [ ] Controller 方法带 @PreAuthorize（白名单除外）；关键写操作留审计日志

## D. 自跑验证（本机）

- [ ] 后端：`tools/verify-local.ps1`（mvn verify 全绿：测试 + ArchUnit + Flyway）
- [ ] 前端：`npm ci && npm run build` 成功
- [ ] 财务正确性用例通过（借贷平衡/凭证号并发/幂等/结账校验/公司隔离）
- [ ] 新增代码有对应测试（业务硬约束必须有测试用例）

## E. 提交与 CI

- [ ] 分支名 `feat/<模块>-<简述>` 或 `fix/<简述>`；**未直推 main**
- [ ] commit message 用 Conventional Commits（feat:/fix:/chore:/docs:/refactor:）
- [ ] PR 描述关联需求与验收标准
- [ ] CI 全绿（backend-ci + frontend-ci）才合入；未用 `--no-verify`/绕过

## F. 收敛（Gate 5 起每次交付后）

- [ ] 新失败已复盘（failure-review.md）并登记 lessons.md
- [ ] 新规则已固化（AGENTS.md / 测试 / 工具 / DB / CI）并更新 rule-registry.md
- [ ] 检查 .harness 待固化条目 = 0（或已明确登记 Gate 6 处理项）
