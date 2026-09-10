# 教训库（Lessons Learned）

> 按流程规范 2.5 收敛反馈：任何失败 → failure-review 复盘 → 此处登记 → 固化为可执行规则。
> 目标：**结构幂等**——所有"待固化"为 0，同一错误不再犯第二次。
> 状态：`待固化` / `已固化`（已落到 AGENTS.md / 工具脚本 / 测试 / DB 约束 / CI 门禁）。

## 教训清单

| # | 错误现象 | 根因 | 避免方法（固化载体） | 状态 |
|---|----------|------|----------------------|------|
| L01 | winget 安装 Git 失败（0x80072efd） | GitHub 直连（443）不通，winget 源走 GitHub | 改用 npmmirror 镜像下载安装包静默安装 | ✅ 已固化（工具链） |
| L02 | git push / ls-remote 间歇性超时 | github.com:443 不通；SSH 22 间歇性丢包 | SSH 通道 + 失败直接重试（非代理问题）；备用 ssh.github.com:443 | ✅ 已固化（流程） |
| L03 | POST /user/keys 返回 404 | PAT 仅 repo 权限，缺 admin:public_key | 改仓库部署密钥（Deploy Key），read_only=false | ✅ 已固化（配置） |
| L04 | 私有仓库设置分支保护 403 | 免费版私有仓库不支持分支保护 | 仓库转 public 后设置 | ✅ 已固化（决策记录） |
| L05 | 作者 approve 自己 PR 报 422 | GitHub 禁止作者审批自己的 PR | 合入统一走 `tools/merge-pr.ps1`（关保护→合入→恢复→验证） | ✅ 已固化（工具） |
| L06 | git 内置 ssh 找不到 known_hosts | git 自带 ssh 与系统 OpenSSH 不共享配置 | `git config --global core.sshCommand "C:/Windows/System32/OpenSSH/ssh.exe"`（正斜杠） | ✅ 已固化（配置） |
| L07 | SSH config 写入中文路径后损坏 | ASCII 编码写入中文绝对路径损坏 | config 用 `IdentityFile ~/.ssh/id_ed25519` 相对路径 | ✅ 已固化（配置） |
| L08 | PowerShell 调原生程序时剥离参数内双引号（psql `CREATE DATABASE "x-y"`、curl JSON body、mvn `-Dflyway.url=...` 被拆坏） | PowerShell 5.1 原生命令参数引号传递规则 | 一律用「SQL/JSON body 临时文件 + `-f`/`-d @file`」，或 `cmd /c` 包裹；**禁止**在 `& exe` 里传含双引号的单引号字符串 | ✅ 已固化（工具/流程） |
| L09 | `Set-Content -Encoding utf8` 写入带 BOM，GitHub API 报 "Problems parsing JSON" | PS 5.1 的 utf8 = UTF-8 with BOM | body 文件用 Write 工具（无 BOM）或 `-Encoding ascii` | ✅ 已固化（流程） |
| L10 | Flowable 引擎先于应用 Flyway 建 70 张 ACT 表 → Flyway 误判非空走 baseline（版本 1）→ 跳过 V1 → V2 失败 | Flowable auto-config 初始化早于 FlywayInitializer；baseline-on-migrate=true | 测试 profile 排除 Flowable 引擎；**Gate 6 引入流程模块时重审 schema 方案**（独立 schema 或预置 ACT 迁移） | ✅ 已固化（测试配置）；⚠️ Gate 6 需重审 |
| L11 | ArchUnit 空规则报 "failed to check any classes" | failOnEmptyShould 默认 true | 命名/类型类规则显式 `.allowEmptyShould(true)` | ✅ 已固化（测试） |
| L12 | ArchUnit 把 `@RestController` 注解误判为"以 Controller 结尾的类" | 注解类名也以 Controller 结尾 | 谓词排除 `doNot(simpleName("RestController"))` | ✅ 已固化（测试） |
| L13 | ArchUnit `SetN.contains(null)` 抛 NPE | 不可变集合不允许 null | predicate 内先判空再 contains | ✅ 已固化（测试） |
| L14 | `mvn flyway:migrate -Dflyway.locations=filesystem:src/...` 报 location not found | filesystem 路径相对**执行目录**（reactor 根）而非模块 | 用 `filesystem:finance-admin/src/main/resources/db/migration` | ✅ 已固化（命令） |
| L15 | `-pl finance-admin flyway:migrate` 报内部模块 SNAPSHOT 缺失 | 依赖模块未 install 到本地仓库 | 先 `mvn -pl finance-admin -am install -DskipTests` 再跑插件目标 | ✅ 已固化（工具 db-migrate.ps1） |
| L16 | Edit 工具报 "Native execution failed" | 工具原生执行故障（环境相关） | 改用 Write 全量重写小文件 | ✅ 已固化（流程） |
| L17 | PATCH /branches/main/protection 返回 404 | GitHub 分支保护更新端点是 **PUT**（PATCH 不存在） | 用 PUT 且 body 携带完整保护配置 | ✅ 已固化（流程） |
| L18 | 迁移脚本引用不存在的列（idx 含 period_year 但分录表无此列） | 复制索引定义时未核对目标表列 | 迁移脚本必须本地先跑通再合入；CI 上下文测试兜底（Flyway 落库） | ✅ 已固化（CI 门禁） |
| L19 | 测试库被 Flowable 表污染后残留 baseline 记录 | 污染库无法自愈 | 删库重建（开发期）；CI 每次全新 services 无此问题 | ✅ 已固化（流程） |
| L20 | `$ErrorActionPreference='Stop'` 下 PowerShell 把原生命令 stderr 警告（JVM/npm chunk 警告）误判为失败 | PS 5.1 Stop 策略把 native stderr 当错误 | 工具脚本不设 Stop，显式检查 `$LASTEXITCODE`（verify-local.ps1 注释） | ✅ 已固化（工具） |
| L21 | ArchUnit 扫描测试类导致分层/命名规则误报（测试类访问 Service 被拒） | @AnalyzeClasses 默认包含 test-classes | `importOptions = ImportOption.DoNotIncludeTests.class`（架构规则只管生产代码） | ✅ 已固化（测试） |
| L22 | PowerShell `Set-Content -Encoding UTF8` 写 Java 源文件带 BOM → javac 报 `非法字符: '\ufeff'` | PS 5.1 的 utf8 = UTF-8 with BOM | Java 源文件一律用 Write 工具（无 BOM）写入；不要用 PS 改源码 | ✅ 已固化（流程） |
| L23 | 冒烟发现：种子菜单仅含 list 权限码，按钮级权限（add/edit/audit/book/reverse）缺失 → 登录后调用写接口 403 | @PreAuthorize 权限码未在 sys_menu 种子数据落地 | V3__permissions.sql 补全 BUTTON 权限点并授权 admin；新增权限点必须同步种子菜单 | ✅ 已固化（V3 迁移） |
| L24 | dev 环境启动失败：Flowable 引擎初始化查 act_ge_property 表不存在（database-schema-update=false 且库无 ACT 表） | Flowable 自管理 schema，与 Flyway 业务迁移冲突（L10 重审点实际爆发） | 主配置统一排除 Flowable 16 类；Gate 7 引入 W1 时改用独立 schema 方案 | ✅ 已固化（application.yml） |
| L25 | WorkflowServiceTest 幂等误伤：测试库已有 CONTRACT 10001-10004 流程记录，start 报"已发起流程" | Flowable ACT_* 表独立事务、Spring 测试回滚不覆盖；wf_process_instance 也因前次运行残留 | @BeforeEach 物理清理：`DELETE FROM wf_process_instance` + ACT 运行表；CI 全新库天然干净 | ✅ 已固化（测试） |
| L26 | 幂等兜底误伤：@BeforeEach 用 MP `delete(null)` 逻辑删后，start 仍报"唯一约束兜底" | `uq_wf_business(business_type, business_id)` 不含 deleted 列，逻辑删除不释放唯一索引 | 测试清理必须**物理删除** wf_process_instance，不能逻辑删 | ✅ 已固化（测试） |
| L27 | PG 报"无法推断参数 $6 的数据类型"（BookQueryMapper @Select） | `#{subjectId}` 传 null 且无上下文类型推断 | 参数显式 `#{subjectId, jdbcType=BIGINT}`（三个查询全改） | ✅ 已固化（代码） |
| L28 | 结账"试算不平衡 260000"：种子凭证借 1002 10万/贷 4001 10万 | 算法写成"归一净额合计==0"；正确应为**借方余额合计 == 贷方余额合计**（净额正=借余、负=贷余，与科目方向无关） | assertTrialBalance 按借/贷余分列合计比较 | ✅ 已固化（PeriodService） |
| L29 | 账簿断言 expected 100000 but was 0（4001 实收资本期末贷余为 0） | calculateEnding 把正向余额统一放 endingDebit，CREDIT 方向科目余额应落 endingCredit | assignByDirection：正向余额按科目方向落列，反向余额落另一列取绝对值 | ✅ 已固化（BookService） |
| L30 | @BeforeEach 清 ACT 表报外键违规 act_fk_idl_procinst | act_ru_identitylink/variable/task 引用 act_ru_execution | 删除顺序：identitylink → variable → task → execution（子表先删） | ✅ 已固化（测试） |
| L31 | V6 迁移报"重复键违反 sys_menu_pkey(1300)"：V2 种子已建合同菜单 | 新迁移重复建已存在的菜单/ID，未先核对 V2 种子 | 新迁移前先查 V2 已建菜单 ID/权限码；已存在的只 UPDATE/补按钮 | ✅ 已固化（流程） |
| L32 | 逻辑删除后重建同编号合同报"唯一约束冲突"，重试无效 | 编号查询（MP selectList）自动过滤 deleted=0，看不到逻辑删记录，误复用编号撞 uq_ctr_contract_no（唯一约束含逻辑删行） | 编号分配用原生 SQL 查 max（**不**加 deleted 过滤）：CtrContractMapper.selectLatestContractNo | ✅ 已固化（代码） |
| L33 | 审批驳回后 process_instance_id 未清空（断言 null 失败） | MP `updateById` 默认忽略 null 字段，`setProcessInstanceId(null)` 不生效 | 显式置 NULL 用 `LambdaUpdateWrapper.set(col, null)` | ✅ 已固化（代码） |

## 待固化（Gate 6 需清零）

- 当前无待固化项；Gate 6 起每完成一个功能点，对照 checklist 自查并补录新教训。
| L34 | C3 测试 4 例失败：sync 二次计数=1（存在也计数）、部分核销期望 PARTIAL 实为 OVERDUE、pageAp open=0（c2 未生成 AP）、OVERDUE 语义与核销职责混淆 | 计数口径=新生成而非调用成功；OVERDUE 是到期提醒职责（Gate 10 定时任务），核销路径只置 PAID/PARTIAL；测试数据未搭全（需 sync 生成 OPEN 单）| ArApService 加 existsArByPlan/existsApByPlan 前置判定；updatePlanAfterSettle 移除 OVERDUE 分支；测试补 syncDuePlans；OVERDUE 归属 5.4 到期提醒 | ✅ 已固化（代码+测试） |
| L35 | 合入后分支保护状态检查（backend-ci/frontend-ci）丢失：merge-pr.ps1 用 PUT 全量覆盖 /branches/main/protection，$base 中 required_status_checks 为 $null，首次合入即覆盖掉 CI 门禁（CI 红也能合入，违反"机器强制"核心诉求） | PUT 是全量替换而非合并；脚本只关心 enforce_admins 的临时开关，未考虑其他保护字段 | 修复：$base 常驻 status_checks contexts；合入脚本 [3/3] 恢复后增加 status_checks 实证（enforce_admins + reviews + checks 三查）；lessons 记录本条目 | ✅ 已固化（merge-pr.ps1 + 实证） |

| L36 | 已应用的 Flyway 迁移文件被修改（V8 增列）→ 本地/测试库 checksum 不匹配 → 83 例测试全 Error（上下文加载失败） | 迁移文件一旦被任何库应用，checksum 锁定，不得修改内容 | 未合入的迁移可改，但已应用过的库必须能对上 checksum：已应用则**追加新 V9 迁移**补列，不回改旧文件；新迁移在本地先跑通（mvn test 全绿）再合入 | ✅ 已固化（流程） |
| L37 | 新集成测试 @BeforeEach `DELETE FROM fin_voucher` 全清 → 删掉 V2 种子凭证(1001/1002) → 依赖种子的 VoucherServiceTest/BookServiceTest/PeriodServiceTest 批量失败；且测试跑完未清理，残留凭证占凭证号导致顺序断言失败 | 共享测试库的种子数据是跨测试类的公共依赖，全量删除即破坏；测试方法间无 @AfterEach 兜底 | 清理 SQL 用 `WHERE id NOT IN (种子id)` 保留共享种子；@BeforeEach + @AfterEach 双清理保证跑完不留数据；破坏后从 V2 脚本原样重建种子 | ✅ 已固化（测试规范） |

| L39 | V10 菜单 id=1206 冲突：1206 已被 V7 应收应付占用（sys_menu_pkey 唯一冲突）→ 首次 mvn test 上下文加载失败 100 例全 Error | 新迁移插入固定 id 前未检查历史迁移已占用；Flyway 失败整体回滚但半状态需手工清理 | 写迁移前先 psql 查对应 id 区间占用；失败后 DROP 半建表 + 清理 flyway_schema_history 再重跑 | ✅ 已固化（流程） |
| L40 | W1 审批流"驳回后重新提交"被幂等拦截：start 只要存在历史实例即拒绝，且唯一约束兜底双重拦截（驳回重提 → "唯一约束兜底"） | W1 幂等语义过严：已结束（APPROVED/REJECTED）实例应允许重新发起，仅 RUNNING 需拦截 | WorkflowService.start 改为仅 RUNNING 拒绝；已结束实例复用记录行 updateById（驳回可重提，兼容合同/报销，Flowable 历史仍在 act_hi_*）；测试补驳回重提用例 | ✅ 已固化（代码+测试） |
| L41 | S2 分配角色/菜单"先清后插"踩唯一约束：MyBatis-Plus delete() 是逻辑删（UPDATE deleted=1），残留行仍占用 uq_sys_user_role/uq_sys_role_menu → 重新插入同组合撞唯一约束 | 对唯一联合约束表使用逻辑删除做"清空"语义错误；逻辑删除行对唯一约束仍可见 | 关联表（sys_user_role/sys_role_menu）清空改用物理删除 @Delete 自定义 SQL；业务主表（sys_user/sys_role）保留逻辑删除；教训：唯一约束表"先清后插"必须物理清 | ✅ 已固化（代码+测试） |
| L42 | V12 迁移写 1400/1401/1402 与 V2 种子菜单 pkey 冲突（sys_menu_pkey 唯一约束）→ Flyway 失败回滚，与 L39（V10 写 1206）同源二次踩坑 | 写迁移前只查了目标区间，未核对 V2 种子菜单（1400-1402 已占用）；flyway 失败整体回滚 | 写迁移前先 psql 查 sys_menu id 占用（含种子 V2）；V12 重写为只补 1403 + 按钮，复用 V2 菜单；教训在 L39 已固化为流程仍复踩 → 流程升级为 checklist 必查项 | ✅ 已固化（V12 重写 + 流程） |
| L43 | 工资核算 calculate 重复执行缺勤扣款无限累加（other_deduct 越加越大）→ 幂等性破坏 | 缺勤扣款直接 set 到持久字段 other_deduct，每次重算在旧值上加 | 派生项（ABSENT×日薪）只参与当月应税收入与实发计算，不落 other_deduct（仅手工扣款落库）；cumulativeWithholdingTax 带 absentDeduct 仅当月扣；测试加重复核算幂等断言 | ✅ 已固化（PR#23） |
| L44 | 雪花 ID(Long≈2.09e18) 超出 JS Number 安全整数(2^53)，Jackson 默认数字序列化 → 前端 id 精度丢失（2098020816046051329→2098020816046051300），按错误 id 请求 /salary/{id}/slip 返回 500 | Long 序列化为 JSON 数字，JSON.parse 精度丢失；DB 行真实 id 与前端请求 id 不一致，排查时表现为"页面与直连数据不一致"假象 | JacksonConfig 全局 ToStringSerializer(Long/Long.TYPE)，id 以字符串传递（URL 拼接/@PathVariable 兼容）；教训：雪花 ID 场景必须 Long→String，属前后端分离标配 | ✅ 已固化（PR#24） |
| L45 | V14 迁移"sys_attachment 已存在"：V1__init_schema.sql 已建同名表，V14 重复 CREATE TABLE → 全新库（CI）首次执行即 42P07；status 写 '0' 违反 ck_sys_menu_status（ACTIVE/DISABLED 枚举）→ 本地/CI 双双失败，本地曾误判为"test 库脏残留"删表掩盖根因 | 写迁移前未检索历史迁移是否已建同名表（只查了菜单 id）；枚举 CHECK 约束值未核对 | V14 重写为幂等 ALTER（ADD COLUMN IF NOT EXISTS + DROP NOT NULL + ON CONFLICT DO NOTHING）；写迁移前搜全库同名表 + 查目标表 CHECK 约束枚举值；本地 test 库重建为全新库（DROP SCHEMA public CASCADE）复现 CI 路径 | ✅ 已固化（V14 重写 + 流程：新建表先查历史迁移 + 查 CHECK 枚举）|
| L46 | git add -A 把运行日志/MinIO 二进制（107MB）提交 → GitHub pre-receive hook 拒绝推送；误覆盖 .gitignore（target/ 丢失）→ 13822 个构建产物进暂存区 | add -A 无差别全量；.gitignore 用 Set-Content 覆盖原内容 | .gitignore 追加忽略（*.log/tools/minio/ 等）并保留原条目；提交前 git status 检查 staged 文件清单；大文件不进仓库（MinIO 二进制本地运行即可） | ✅ 已固化（.gitignore + 提交流程）|
| L47 | 登录失败日志丢失：V1 缺五件套列 → insert 报 "create_by 字段不存在" 被 writeLoginLog 的 try-catch 吞掉；补列后 insert 成功（Updates:1）但表内仍无记录 → 根因是 login() 上 @Transactional，失败分支 writeLoginLog 后抛 BusinessException 触发事务回滚，日志随回滚消失 | 审计日志写入放在业务事务内 + 后续抛业务异常 → 日志被回滚；writeLoginLog 的 try-catch 吞异常掩盖了第一层问题 | V15 幂等补五件套列；去掉 login() 的 @Transactional（登录无多步事务需求，updateLoginInfo 单条 update，日志独立提交）；测试补真实落库断言（LoginLogWriteTest）；教训：审计日志必须独立于业务事务提交（REQUIRES_NEW 或拆分事务），且 try-catch 吞异常后要用日志排查而非只看表 | ✅ 已固化（V15 + AuthService + LoginLogWriteTest）|
