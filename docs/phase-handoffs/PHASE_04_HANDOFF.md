# PHASE_04_HANDOFF

## 1. 交接目标

本文档用于新 Codex 窗口进入 Phase 5 前，快速理解 Phase 4 真实代码状态、验收结果、剩余风险和接手边界。

结论：Phase 4 已完成 PlanningAgent 真实链路，准备进入 Phase 5：真实 ConfigurationAgent + RAG / Template Tools。真实 A2A / Nacos / DashScope 端到端联调仍待用户手动验证。

## 2. Phase 1-4 已完成内容摘要

- Phase 1：建立 12 模块 Maven 工程，完成 common、model、agent-core、model-core 基础能力；`AgentUtils` 保留单一 `callSchema(ReactAgent, ...)` 入口。
- Phase 2：Orchestrator 侧建立远程 Agent 发现、A2A 调用、响应校验和异常转换边界；官方 SAA A2A / Nacos 成为主线，legacy fallback 默认关闭。
- Phase 3：完成 IntentAgent 真实链路，输出 `NetworkIntent`，Orchestrator 写入 `NETWORK_INTENT` Artifact，Web 提供最小任务和运行入口。
- Phase 4：完成 PlanningAgent 真实链路，输出 `NetworkPlan`，Orchestrator 写入 `NETWORK_PLAN` Artifact，并补齐 Planning parser / validator / workflow 测试。

## 3. Phase 4 具体完成内容

- PlanningAgent 模块结构：`PlanningAgentApplication`、`PlanningAgentConfiguration`、`PlanningAgentProperties`、`PlanningAgent`、`PlanningServiceImpl`、parser、validator、schema、tools、prompt。
- Prompt：`src/main/resources/prompts/planning-agent-prompt.md` 要求结构化 `PlanningResponseSchema`，禁止 CLI 输出。
- Schema：`PlanningResponseSchema` 覆盖拓扑节点/链路、区域、地址、VLAN、路由、安全策略、NAT、约束和告警。
- Parser：`PlanningResponseParser` 将 schema 转换为 `NetworkPlan`，补齐 `updateTime`、`createdBy`、`targetEnvironment`、`addressPlan`、`vlanPlan`、`routingPlan`、`traceRefs`。
- Validator：`PlanningOutputValidator` 校验规划核心字段，并拒绝 CLI / 具体配置越界内容。
- Tools：`AddressPlanningTool`、`VlanPlanningTool`、`TopologyTemplateTool`、`PlanningPlaybookTool`，当前只给规划建议，不执行配置，不写 Workspace。
- A2A / Nacos：`mac-tav-planning-agent/src/main/resources/application.yml` 注册 `PlanningAgent` card，端口 `18082`，使用 `spring-ai-alibaba-starter-a2a-nacos`。
- Orchestrator：`MacTavWorkflowOrchestrator#runPlanningStage` 构造 `PlanningAgentInvokePayload`，调用 `PlanningAgent`，解析 `NetworkPlan`，写入 `NETWORK_PLAN` Artifact，追加 `AgentExecutionRecord`。
- Web API：`POST /api/v1/workflows/{taskId}/plan` 暴露 Planning stage 过渡调试入口。
- 测试：Planning parser / validator / service / prompt / tools、Orchestrator Intent / Planning stage 写入均已覆盖。

## 4. Phase 4 验收状态

| 项 | 状态 | 说明 |
| --- | --- | --- |
| PlanningAgentConfiguration | DONE | 注册 `planningReactAgent` 和 `PlanningAgent`，不使用 `@ConditionalOnBean(ChatModel.class)`。 |
| PlanningAgent | DONE | 薄封装真实 ReactAgent，返回 `NetworkPlan`，不写 Workspace。 |
| ResponseSchema | DONE | `PlanningResponseSchema` 已覆盖 Phase 4 规划输出。 |
| Parser | DONE | 补齐 `updateTime`、`createdBy` 和核心规划字段映射。 |
| Validator | DONE | 校验 addressPlan / vlanPlan / routingPlan / targetEnvironment / traceRefs 等核心字段。 |
| Prompt | DONE | `planning-agent-prompt.md` 已存在并约束结构化输出。 |
| Tools | DONE | 已有 4 个规划建议工具；当前偏规则化。 |
| A2A / Nacos / Agent Card | RISK | 配置已接入官方 starter，但真实端到端联调待手动验证。 |
| Orchestrator 接入 | DONE | `runPlanningStage` 已写入 Workspace / Artifact / AgentExecutionRecord。 |
| Workspace / Artifact 写入 | DONE | `NETWORK_PLAN` Artifact 已接入。 |
| Web API | RISK | `/plan` 是过渡调试入口，后续应统一接口语义。 |
| 测试 | DONE | `mvn -pl mac-tav-planning-agent,mac-tav-orchestrator -am test` 通过。 |

## 5. 当前核心模块状态

| 模块 | 当前职责 | 关键类/文件 | 已可用能力 | 不应改的边界 |
| --- | --- | --- | --- | --- |
| `mac-tav-common` | 公共错误、异常、响应模型 | `ErrorCode`、`BusinessException`、`ApiResponse` | 公共基础能力 | 不放业务 DTO / Agent / Workspace 逻辑 |
| `mac-tav-model` | 跨模块共享 DTO / 枚举 / 契约 | `NetworkIntent`、`NetworkPlan`、`ConfigSet`、`TraceRefs`、A2A DTO | 阶段产物和契约模型 | 不依赖 Spring AI、Web、Orchestrator 或具体 Agent |
| `mac-tav-agent-core` | Agent 通用基础设施 | `AgentUtils`、`PromptLoader`、Parser / Validator、hooks、`AgentRunContext` | Prompt 加载、ReactAgent 构建、结构化输出调用 | 不写 Workspace，不放 Orchestrator 远程调用实现 |
| `mac-tav-model-core` | Workspace / Artifact 状态中心 | `InMemoryNetworkWorkspaceService`、Artifact services、record services | 内存状态、Artifact 版本、执行记录 | 不调用模型，不生成 Plan / Config |
| `mac-tav-intent-agent` | Intent 阶段专业 Agent | `IntentAgentConfiguration`、`IntentAgent`、schema/parser/validator/tool | `NetworkIntent` 真实链路 | 不写 Workspace，不依赖 Orchestrator / Web |
| `mac-tav-planning-agent` | Planning 阶段专业 Agent | `PlanningAgentConfiguration`、`PlanningAgent`、schema/parser/validator/tools | `NetworkPlan` 真实链路 | 不生成 CLI，不写 Workspace |
| `mac-tav-orchestrator` | 主编排和远程 Agent 调用适配 | `MacTavWorkflowOrchestrator`、`RemoteAgentInvoker`、official A2A adapter | Intent / Planning stage 闭环 | 不直接依赖具体 Agent，不构造 Prompt |
| `mac-tav-web` | Web/API 入口 | `TaskController`、`WorkflowController` | 创建任务、运行 Intent / Planning、查 Workspace | 不直接调用 Agent / ChatModel / ReactAgent |
| `mac-tav-configuration-agent` | Phase 5 待实现 | `package-info.java`、`application.yml`、Qdrant Maven 依赖 | 仅骨架和配置 | 不应被视为已实现 ConfigurationAgent |

## 6. 当前架构债和风险

- 真实 A2A / Nacos / DashScope 端到端联调待手动验证。
- `OfficialAgentCardRegistryClient#findByAgentName` 已支持按名称查 Agent；`listAvailableAgents()` 仍偏 `IntentAgent`，多 Agent 列表能力待扩展。
- `/api/v1/workflows/{taskId}/plan` 是过渡 API，后续应并入统一 workflow progression / rerun 契约。
- Model Core 仍是内存实现，进程重启后状态丢失，Phase 9 再持久化。
- Qdrant / RAG 尚未接入业务链路；`mac-tav-configuration-agent` 现有 Qdrant 配置只是 Phase 5 前置痕迹。
- Planning tools 当前偏简化规则和关键字建议，Phase 5 需要更结构化地服务 ConfigSet 生成。
- Legacy fallback 类仍存在但默认关闭，不应恢复或复制为新 Agent 模板。

## 7. Phase 5 接手建议

1. 先实现 `ConfigurationResponseSchema`。
2. 再实现 `ConfigurationResponseParser` 和 `ConfigurationOutputValidator`。
3. 再实现配置模板工具 `ConfigTemplateTool`。
4. 再实现 RAG / Qdrant 最小检索工具和 Markdown 知识库 ingestion。
5. 再实现 `ConfigurationAgentConfiguration` 和 `ConfigurationAgent`。
6. 再接 Orchestrator Configuration stage。
7. 再补 Workspace / Artifact / AgentExecutionRecord 测试。

## 8. Phase 5 MUST NOT

- 不接 `ExecutionAdapter` / Mininet / Ryu。
- 不实现 `VerificationAgent` / `HealingAgent`。
- 不让 `ConfigurationAgent` 直接执行命令。
- 不让 Tool / RAG 写 Workspace。
- 不返回一整段不可校验的命令文本。
- 不引入 fake/offline Agent 主链路。
- 不让 Web 直接调用 Agent。
- 不让 Orchestrator 直接调用 `ChatModel` / `ReactAgent`。
- 不破坏已有 IntentAgent / PlanningAgent 主线。

## 9. 新 Codex 窗口推荐读取顺序

1. `AGENTS.md`
2. `docs/CODEX_CURRENT_STATE.md`
3. `docs/CODEX_DOC_INDEX.md`
4. `docs/phase-handoffs/PHASE_04_HANDOFF.md`
5. `docs/06_DEV_PLAN.md` 中 Phase 5 相关内容
6. `docs/09_AGENT_BUILD_GUIDE.md` 中 ConfigurationAgent、Tool、RAG、Parser、Validator、A2A 相关内容
7. `docs/04_DATA_MODELS.md` 中 NetworkPlan、ConfigSet、CommandBlock、TraceRefs、NetworkWorkspace、NetworkArtifact 相关内容
8. `docs/02_MAVEN_MODULES.md` 中 mac-tav-configuration-agent 和 Qdrant / Vector DB 依赖边界
9. `docs/07_TEST_DATA_AND_SCENARIOS.md` 中 ConfigSet 和 ConfigurationAgent 非法输出场景
10. `docs/08_RUN_AND_TEST.md` 中 Nacos、真实 Agent、Qdrant、环境变量和测试命令
11. 直接相关源码：`mac-tav-configuration-agent`、`mac-tav-planning-agent`、`mac-tav-orchestrator`、`mac-tav-model`、`mac-tav-model-core`、`mac-tav-agent-core`，必要时读取 `mac-tav-web`

## 10. 建议运行命令

- `mvn compile`
- `mvn -pl mac-tav-configuration-agent -am test`
- `mvn -pl mac-tav-orchestrator -am test`
- `mvn -pl mac-tav-web -am test`
- 如果改公共 DTO，再运行 `mvn test`

说明：

- 自动化测试不调用真实模型 API。
- 不长期挂起 `spring-boot:run`。
- 真实 A2A / Nacos / DashScope / Qdrant 联调由用户准备环境后手动验证。
