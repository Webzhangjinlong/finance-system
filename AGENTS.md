# AGENTS.md — 财务管理系统（Codex 自动加载）

本文件由 Codex 每次启动时自动读取并注入上下文。所有规则直接内联于此，不要求 Agent 额外翻阅文档；详细设计见文末导航。

## 项目

业财一体、多公司（多账套）财务管理系统。核心功能域：财务管理（科目/凭证/账簿/结账/报表/报销/收付款）、合同管理（台账/审批/收付款计划/到期提醒）、人事管理（档案/考勤/工资）、系统管理（认证/用户/角色/菜单/字典/日志）。

## 技术栈（版本锁定，禁止擅自变更）

- 前端：Vue 3 + Vite 5 + Pinia + Vue Router + Element Plus + ECharts（`finance-web/`）
- 后端：Spring Boot 3.3 (JDK 17) + MyBatis-Plus + Spring Security/JWT + Flowable 7，模块化单体（`finance-server/`，模块：common/framework/system/finance/contract/hr/workflow/admin，包根 `com.finance`）
- 存储：PostgreSQL（金额 NUMERIC(18,2)、时间 TIMESTAMPTZ）+ Redis + MinIO
- 所有 Controller 返回统一 `Result<T>{code,msg,data}`，code=200 成功

## 硬约束（违反即必须修复，禁止交付）

### 业务（财务核心）
1. 金额一律 `BigDecimal` / `NUMERIC(18,2)`，**禁止 float/double**。
2. 所有业务表必含 `company_code`，查询必须按公司隔离；禁止跨公司读写（拦截器 + RLS 兜底，勿绕过）。
3. 凭证借贷必平衡（DB CHECK + 服务校验双兜底），分录金额 > 0，仅末级科目可记账。
4. 凭证状态机 `DRAFT → AUDITED → BOOKED → REVERSED`；已审核/已过账禁改，冲销用红冲 + 蓝字。
5. 凭证号 = 公司 + 期间 + 序列，唯一且保存即占号；并发场景不得重号。
6. 已结账期间只读；结账前校验无未审核/未过账凭证且试算平衡。
7. 业务单据生成凭证必须幂等（source_type + source_id 唯一），禁止重复生成。
8. 收付款/核销与合同计划联动，核销后回写状态，禁止超额核销。
9. 工资用累计预扣法计算个税；工资条仅本人可见。

### 架构与分层
10. 依赖方向 `Controller → Service → Mapper`，禁止跨层调用、禁止跨域模块直调他人 Mapper（走对方 Service）。
11. 业务代码落在对应模块包内，禁止把财务逻辑写进 system/framework。
12. 资金/写操作必须 `@Transactional` + 幂等键；禁止无事务多步写。
13. 分页返回统一 `records/total/page/size`，禁止自造分页结构。

### 代码与命名
14. 包/类/方法小驼峰（类大驼峰），常量 `UPPER_SNAKE`；表/字段小写下划线，表前缀见设计文档。
15. 接口路径 RESTful；Controller 方法必须带 `@PreAuthorize` 权限码（白名单除外）。
16. 禁止魔法值：状态/类型一律用枚举或常量。
17. 日志禁止输出密码/token/手机号/身份证等敏感信息；关键写操作（审核/过账/结账/删除）留审计日志。

### 数据库
18. 主键用雪花 ID（ASSIGN_ID）；时间用 `TIMESTAMPTZ`；公共五件套（create_by/create_time/update_by/update_time/deleted）+ 逻辑删除。
19. 结构变更只走 Flyway 迁移脚本（`finance-admin/src/main/resources/db/migration`），禁止手工改库。
20. 核心约束必须落到 DDL：CHECK（借贷平衡、金额>0）、唯一约束（凭证号/公司编码/工号等）、外键、索引按查询路径设计。

### 安全
21. 密码 BCrypt；JWT 无状态（默认 2h）；登录失败 5 次锁定 15 分钟。
22. 入参必须 `@Valid` + JSR-303 校验；SQL 一律参数化，禁止拼接。

## 开发工作流

1. 每次改动从 `main` 拉新分支：`feat/<模块>-<简述>` 或 `fix/<简述>`；**禁止直推 main**。
2. 提交用 Conventional Commits（`feat:` / `fix:` / `chore:` / `docs:` / `refactor:`）。
3. 完成标准：本地验证通过（见命令清单）→ 提交 → `git push` → 创建 PR → 用 `tools/merge-pr.ps1` 合入（自动处理分支保护）。
4. 涉及新增业务规则时，同步更新 `.harness/rule-registry.md`。

## 命令清单（已实测可执行）

```bash
# 前端（工作目录 finance-web/）
npm install        # 安装依赖（registry 已配 npmmirror）
npm run build      # 构建验证

# 后端（工作目录 finance-server/）
mvn -pl finance-admin -am package -DskipTests   # 编译打包（跳过测试）
mvn verify                                       # 全量验证（含测试，Gate 4 后为 CI 同款）

# 合入受保护分支（仓库根目录）
$env:GH_TOKEN = '<PAT>'; .\tools\merge-pr.ps1 -PrNumber <N>
```

## DO NOT

- 不要 `--no-verify` 跳过检查，不要强推/直推 `main`，不要绕过 `merge-pr.ps1` 合入。
- 不要手工改数据库、不要用 float 存金额、不要跳过或篡改测试让构建变绿。
- 不要把 token/密码/密钥写入代码、提交、日志或文档。
- 不要为实现方便破坏上述任何一条硬约束。

## 文档导航（详细设计，按需查阅）

- `docs/财务管理系统开发方案.html` — 架构决策、模块划分、DB 表清单、排期
- `docs/财务管理系统功能开发文档.html` — 各功能点开发级规格（规则/接口/权限/异常）
- `docs/开发约束与流程规范.html` — Harness 流程、CI 门禁、收敛机制
- `.harness/` — 教训库、规则注册表、失败复盘（随开发持续维护）
