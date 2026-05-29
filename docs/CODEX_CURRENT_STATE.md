# CODEX_CURRENT_STATE

## 1. 当前项目阶段

当前项目已经完成 Phase 1、Phase 2、Phase 3 的主体落地，下一阶段准备进入 Phase 4。本文档只记录当前代码状态和接手边界，不设计 Phase 4 的具体实现。

当前代码以 Maven 12 模块工程为基础，Phase 1~3 已覆盖通用模型、Agent Core、Model Core、IntentAgent、A2A / Nacos / Agent Card 主线以及最小 Web / Orchestrator 闭环。最近一次精确构建状态需以新窗口重新运行 Maven 为准；本轮只做文档更新，未修改 Java、Maven 或配置代码。

## 2. 当前主线架构状态

- `mac-tav-web` 只作为 Web/API 入口，通过 Orchestrator 触发流程，不扫描具体 Agent 模块。
- `mac-tav-orchestrator` 是当前主编排入口，负责创建任务、调用远程 Agent、解析返回结果并把阶段产物写入 Model Core。
- `mac-tav-model-core` 提供内存版 Workspace / Artifact / Event / AgentExecutionRecord / WorkspaceChangeRecord 状态中心，后续 Phase 9 可替换为持久化实现。
- `mac-tav-intent-agent` 已作为独立 Spring Boot Agent 服务存在，当前主线方向是 Spring AI Alibaba 官方 A2A / Nacos Registry：通过命名 `ReactAgent` Bean 和配置项注册到 Nacos 并暴露 A2A 能力。
- A2A 当前已从手写 HTTP JSON 方案收敛到官方 SAA A2A 方向；IntentAgent 侧自定义 A2A controller / executor 已删除，Orchestrator 侧旧的 `HttpA2aClient`、`NacosAgentCardRegistryClient` 仍保留为 legacy fallback。

## 3. 最近关键改动

- 补齐并收敛了 `AgentUtils` 对 Spring AI Alibaba `ReactAgent` / `ChatModel` 的工程适配。
- 实现了 IntentAgent 薄封装、Intent prompt、schema、parser、validator、tool、service 和离线测试。
- 增加了 IntentAgent 独立服务启动类、命名 `ReactAgent` Bean，以及基于 SAA A2A / Nacos 的注册方向。
- Orchestrator 增加了官方 A2A discovery/client 适配路径，并保留 legacy HTTP JSON / Nacos Config fallback。
- Web 增加了最小任务创建、运行 Intent stage、查询 Workspace 的 API 入口。
- 当前工作区存在此前留下的未提交配置/文档改动；新窗口接手前应先查看 `git status`，不要误回滚用户或前序工具生成的改动。

## 4. 当前可用能力

| 模块 | 当前可用能力 |
| --- | --- |
| `mac-tav-common` | 统一错误码、`BusinessException`、`ApiResponse` 等公共基础能力。 |
| `mac-tav-model` | 核心枚举、阶段产物 DTO、Workspace DTO、TraceRefs、Agent Card / A2A 契约、IntentAgent 共享调用 payload。 |
| `mac-tav-agent-core` | `AgentRunContext`、`PromptLoader`、Parser / Validator 接口、`ValidationResult`、`SchemaAgentInvoker`、`AgentUtils`、项目内部 hook。 |
| `mac-tav-model-core` | 内存 Workspace、Artifact 版本、事件、执行记录、变更记录服务。 |
| `mac-tav-intent-agent` | IntentAgent 离线核心链路和真实 Spring AI Alibaba Agent 薄封装；官方 A2A executor 方向已接入。 |
| `mac-tav-orchestrator` | 远程 Agent 调用适配、Intent stage 最小工作流、Workspace 写入闭环。 |
| `mac-tav-web` | 最小 Web API：创建任务、运行当前 Intent stage、查询 Workspace。 |
| 其他 Agent 模块 | Maven 模块已存在，但仍是最小骨架或占位，不应误认为已实现业务能力。 |

## 5. 当前架构债和待确认事项

- 官方 Spring AI Alibaba A2A / Nacos 端到端联调仍需手动验证：需要真实 Nacos、真实 DashScope Key 和 Agent/Web 两个服务启动。
- IntentAgent 当前按官方 A2A 直接暴露命名 `ReactAgent`，真实联调时需确认 Orchestrator 收到的 `output` 是最终 `NetworkIntent` JSON 还是 `IntentResponseSchema` JSON。
- legacy fallback 仍存在于 Orchestrator 侧：`HttpA2aClient`、`NacosAgentCardRegistryClient` 应避免继续扩展；IntentAgent 侧 legacy HTTP JSON endpoint 已删除。
- `OfficialAgentCardRegistryClient#listAvailableAgents` 当前更偏 IntentAgent 场景，后续多 Agent 时需要按官方 discovery 能力扩展。
- `OrchestratorConfiguration` 中官方 provider Bean 的条件装配需要在真实 Spring Boot + Nacos 环境下验证。
- `docs/08_RUN_AND_TEST.md` 仍可能提到 `AI_DASHSCOPE_API_KEY` / `ALI_API_KEY`，但当前项目安全规则要求使用 `aliApi-key`。
- Model Core 仍是内存实现，不具备跨进程持久化能力。
- Planning / Configuration / Verification / Healing 等真实 Agent 尚未实现。

## 6. 下一阶段建议

下一阶段建议进入 Phase 4，但只应从 PlanningAgent 离线核心链路开始：`PlanningResponseSchema -> Parser -> DTO(NetworkPlan) -> Validator`、prompt 和离线测试。不要提前实现 ConfigurationAgent、VerificationAgent、HealingAgent，也不要接 Mininet / Ryu。

Phase 4 不应破坏既有 IntentAgent / A2A 主线；Orchestrator 和 Web 仍不得直接依赖具体 Agent 模块。

## 7. 下一轮 Codex 推荐读取范围

默认读取：

1. `AGENTS.md`
2. `docs/CODEX_CURRENT_STATE.md`
3. `docs/CODEX_DOC_INDEX.md`
4. `docs/phase-handoffs/PHASE_03_HANDOFF.md`

如果用户要求进入 Phase 4，再读取：

1. `docs/06_DEV_PLAN.md` 中 Phase 4 相关内容
2. `docs/09_AGENT_BUILD_GUIDE.md` 中 Agent 模块结构、Prompt、Parser、Validator、A2A 规范
3. `docs/04_DATA_MODELS.md` 中 `NetworkIntent`、`NetworkPlan`、`TraceRefs` 相关内容
4. `docs/07_TEST_DATA_AND_SCENARIOS.md` 中 PlanningAgent 相关输入输出场景
5. 直接涉及的模块源码：`mac-tav-planning-agent`、`mac-tav-model`、`mac-tav-agent-core`，以及必要的 Orchestrator / Model Core 边界类
