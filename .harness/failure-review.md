# 失败复盘（Failure Review）

> 模板按流程规范 2.5：Codex 犯错（测试失败 / 评审发现问题 / 线上 bug）→ 在此复盘 → 登记 lessons.md → 固化为可执行规则。
> 规则：复盘必须落到"可执行载体"（AGENTS.md / 工具 / 测试 / DB 约束 / CI / Skill），不允许以"注意点"收尾。

---

## 复盘模板

```markdown
## [日期] [标题]

- **目标**：当时要完成什么
- **失败现象**：实际发生了什么（报错/行为/结果）
- **根因**：为什么失败（一层一层挖到底，禁止停留在表象）
- **避免方法**：下次如何避免（具体、可执行）
- **需新增规则**：要不要变成机器强制？落在哪（AGENTS.md 条款 / 测试用例 / 工具脚本 / CI / DB CHECK）
- **状态**：待固化 → 已固化（引用 lessons.md 编号与载体位置）
```

---

## 复盘记录

### 2026-09-09 Gate 4：测试库迁移失败（V2 报 sys_user 不存在）

- **目标**：让上下文测试在独立测试库跑通 Flyway V1+V2
- **失败现象**：`flywayInitializer` 报 `Migration V2__seed_data.sql failed - 关系 "sys_user" 不存在`；查库发现 70 张 Flowable 表 + 1 条 baseline 记录
- **根因**：Flowable auto-configuration 先于应用 FlywayInitializer 初始化引擎并自动建表（database-schema-update=true）→ Flyway 见 schema 非空且 baseline-on-migrate=true → 以默认版本 1 baseline → **跳过 V1** → V2 执行时业务表不存在。三层叠加：时序错误 + baseline 版本默认值 + 测试库非全新
- **避免方法**：测试环境排除 Flowable 引擎全部 auto-config；测试库删库重建；CI 用全新 services.postgres 天然无污染
- **需新增规则**：`测试不得让第三方引擎先于应用 Flyway 建表` → 固化到 application-test.yml（autoconfigure.exclude）+ lessons L10
- **状态**：✅ 已固化

### 2026-09-09 Gate 4：ArchUnit 批量失败

- **目标**：让架构守护规则在骨架空转期通过、代码期生效
- **失败现象**：空规则 "failed to check any classes"；`SysHealthController` 被误判依赖 Controller
- **根因**：① failOnEmptyShould 默认 true，空转规则必须显式放行；② `@RestController` 注解类名以 Controller 结尾被 `haveSimpleNameEndingWith` 命中；③ 自定义谓词 `SetN.contains(null)` 抛 NPE
- **避免方法**：命名/类型规则加 `.allowEmptyShould(true)`；注解类用 `doNot(simpleName("RestController"))` 排除；谓词内先判空
- **需新增规则**：`ArchUnit 谓词必须 null 安全、注解类不参与类名规则` → 固化到 ArchitectureTest + lessons L11/L12/L13
- **状态**：✅ 已固化

### 2026-09-09 全 Gate：PowerShell 原生参数引号丢失

- **目标**：执行 psql / curl / mvn 带引号参数的命令
- **失败现象**：`CREATE DATABASE "finance-system"` 引号被剥离；curl JSON body 报 "Problems parsing JSON"；mvn `-Dflyway.url=...` 被解析成插件坐标
- **根因**：PowerShell 5.1 调用原生 exe 时的参数引号传递规则（双引号被剥离/拆分）
- **避免方法**：SQL 走临时文件 + `-f`；JSON body 走文件 + `-d @file`；mvn 走 `cmd /c "..."` 包裹
- **需新增规则**：`禁止在 & exe 传参中依赖内嵌双引号` → 固化到 lessons L08/L09 与 tools 脚本
- **状态**：✅ 已固化
