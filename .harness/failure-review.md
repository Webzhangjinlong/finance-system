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
## H1-H3 批次复盘（Gate 15）

| 事件 | 根因 | 处置 | 状态 |
|---|---|---|---|
| V12 迁移 1400/1401/1402 与 V2 种子菜单冲突，Flyway 回滚 | 未核对 V2 已占 id（L39 同源复踩） | V12 重写：复用 V2 菜单，仅新增 1403+按钮；role_menu 用 SELECT 100000+m.id 授权 | ✅ 已固化 L42 |
| 工资核算重复执行缺勤扣款累加 | 缺勤扣款写持久字段 other_deduct 且累加 | 改派生项不进 other_deduct，仅当月应税/实发参与；测试补幂等断言 | ✅ 已固化 L43（PR#23） |
| 工资条弹窗空/GET /salary/{id}/slip 500 | 雪花 ID 超 JS 安全整数，Jackson 数字序列化精度丢失 → 前端取错误 id 请求 | JacksonConfig 全局 Long→String；前端/直连数据对比定位"假象" | ✅ 已固化 L44（PR#24） |
| verify-local FAIL（npm ci exit -4048） | npm ci 偶发权限/网络失败 | npm install 重装（registry npmmirror）后 build 通过 | ✅ |
| 页面 403（工资列表） | 浏览器旧 token 权限快照无 hr 权限 | 重新登录获取含 hr 权限新 token | ✅ |
| V14 迁移 sys_attachment 已存在 / status CHECK 违反 | V1 已建同名表未检索；枚举值未核对 | V14 幂等 ALTER 重写 + 本地重建全新库复现 | ✅ 已固化 L45 |
| push 被 pre-receive hook 拒绝 | git add -A 误提交 107MB MinIO 二进制 + 日志 | .gitignore 补忽略 + 提交前核对 staged | ✅ 已固化 L46 |
| 登录日志不落库（失败分支）| ① V1 缺五件套列 insert 报错被吞 ② 补列后 login() @Transactional + 失败抛异常 → 回滚日志 | V15 补列 + login 去 @Transactional + LoginLogWriteTest 真实落库断言 | ✅ 已固化 L47 |
| S4 跨模块编译失败 | @OperLog 注解首版放 finance-system，workflow/contract 等模块不依赖 system → aspectj/注解找不到 | 注解移 finance-common（com.finance.common.core.annotation），切面留 system；全库替换 import 12 个文件 | ✅ 已固化 L48（PR#30） |
| S4 脱敏漏 POJO 字段 | maskSensitive 只递归 Map/List，ResetPwdDTO.password（POJO 字段）原样入参 | 先 valueToTree 转 JsonNode 再递归脱敏（ObjectNode/ArrayNode） | ✅ 已固化 L48 |
| S4 ArchUnit layered_dependencies 红 | 切面直调 SysOperLogMapper 违反 "Mapper 仅 Service 访问" | 分层规则显式增加 Aspect 层（definedBy com.finance..aspect..），Mapper 允许 Service/Aspect 访问，Aspect 不被访问 | ✅ 已固化（规则+测试，PR#30） |
| 浏览器操作日志页 403 | localStorage 残留 9/10 过期 token（页面自动跳转未真正重登）→ 过期 token 全部接口 403（非 401） | 显式 localStorage.clear() 后重新登录，curl 用新 token 验证 200 后再浏览器实测 | ✅ 经验（L48 关联） |
| F8 CI backend-ci 红（145 errors） | V1 占位表 fin_voucher_rule（旧结构）导致 V17 CREATE IF NOT EXISTS 跳过，INSERT 引用新列 event_type 报错；全新库（CI）首次复现 | V17 改为 DROP IF EXISTS + CREATE 重建（占位表从未使用）；本地 test 库删 V17 flyway 记录重跑模拟 CI 路径 | ✅ 已固化 L49（PR#32） |
