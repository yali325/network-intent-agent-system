# CODEX_DOC_INDEX

> Phase 5 收尾后进入 Phase 6 时，除默认三件套外优先读取 `docs/phase-handoffs/PHASE_05_HANDOFF.md`。

## 1. 目标

本文档用于帮助 Codex 按任务类型选择最小必要上下文，避免每次开发前全量读取 `docs/00-09`。

## 2. 默认读取顺序

任何开发任务前，优先读取：

1. `AGENTS.md`
2. `docs/CODEX_CURRENT_STATE.md`
3. `docs/CODEX_DOC_INDEX.md`

然后根据任务类型读取专项文档和最小必要源码。

## 3. 任务类型到文档映射

| 任务类型 | 必读文档 |
| --- | --- |
| 项目定位 / 长期目标 | `docs/00_PROJECT_BRIEF.md`、`docs/01_SCOPE_AND_BOUNDARIES.md` |
| Maven 模块 / 依赖边界 | `docs/02_MAVEN_MODULES.md` |
| 模块职责 / 架构边界 | `docs/03_MODULE_DESIGN.md`、必要时 `docs/02_MAVEN_MODULES.md` |
| DTO / Workspace / Artifact | `docs/04_DATA_MODELS.md` |
| API / Controller / Web 响应 | `docs/05_API_DESIGN.md` |
| Phase 计划 / 下一阶段边界 | `docs/06_DEV_PLAN.md` |
| 测试场景 / 样例数据 / 失败用例 | `docs/07_TEST_DATA_AND_SCENARIOS.md` |
| 运行 / 启动 / 手动验证 | `docs/08_RUN_AND_TEST.md` |
| Agent / Tool / MCP / A2A / Parser / Validator | `docs/09_AGENT_BUILD_GUIDE.md` |

## 4. 常见任务读取建议

| 常见任务 | 推荐读取 |
| --- | --- |
| Agent 实现任务 | 默认三件套 + `docs/09_AGENT_BUILD_GUIDE.md` + 当前 Agent 模块 + 相关 DTO + 必要测试数据 |
| API / Controller 任务 | 默认三件套 + `docs/05_API_DESIGN.md` + `mac-tav-web` 相关 Controller/DTO + Orchestrator 调用接口 |
| Maven / 依赖调整任务 | 默认三件套 + `docs/02_MAVEN_MODULES.md` + 根 `pom.xml` + 相关模块 `pom.xml` |
| Workspace / Artifact 任务 | 默认三件套 + `docs/04_DATA_MODELS.md` + `mac-tav-model-core` 相关服务/校验/测试 |
| A2A / Nacos 任务 | 默认三件套 + `docs/09_AGENT_BUILD_GUIDE.md` + `docs/08_RUN_AND_TEST.md` + Agent/Orchestrator A2A 配置与 adapter |
| 测试任务 | 默认三件套 + `docs/07_TEST_DATA_AND_SCENARIOS.md` + 目标模块测试 + 必要样例 JSON |
| 审查任务 | 默认三件套 + `git diff` / `git status` + 与变更文件直接相关的专项文档和源码 |
| 新阶段接手任务 | 默认三件套 + 最新的 `docs/phase-handoffs/PHASE_xx_HANDOFF.md` + 下一阶段直接相关专项文档和源码 |
| Phase 6 Execution 准备 / 实现任务 | 默认三件套 + `docs/phase-handoffs/PHASE_05_HANDOFF.md` + `docs/phase-handoffs/PHASE_06_P0_P4_NOTES.md` + `docs/06_DEV_PLAN.md` Phase 6 + `docs/03_MODULE_DESIGN.md` Execution / Orchestrator / Web + `docs/04_DATA_MODELS.md` ExecutionReport / Workspace / Artifact + `docs/08_RUN_AND_TEST.md` Mininet/Ryu |

## 5. 禁止事项

- 不要默认全量读取 `docs/00-09`。
- 不要为了保险全仓扫描。
- 不要把无关专项文档塞进上下文。
- 不要用旧代码覆盖 `AGENTS.md` 和专项文档中的长期架构规则。
- 不要因为某个模块存在占位文件，就假设该阶段真实 Agent 已实现。
