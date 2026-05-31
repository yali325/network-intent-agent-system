# CODEX_CURRENT_STATE

## 1. 当前项目阶段

当前项目已完成 Phase 1、Phase 2、Phase 3、Phase 4 的主体落地。

Phase 4 的完成内容是 PlanningAgent 真实链路：`PlanningAgentConfiguration -> planningReactAgent -> PlanningAgent -> PlanningResponseSchema -> PlanningResponseParser -> NetworkPlan -> PlanningOutputValidator`，并由 Orchestrator 接入 Planning stage，把 `NETWORK_PLAN` 写入 `NetworkWorkspace` / `NetworkArtifact`。

下一阶段是 Phase 5：真实 `ConfigurationAgent + RAG / Template Tools`。Phase 5 之前不得把 `mac-tav-configuration-agent` 的现有骨架、Qdrant 配置或 Maven 依赖误认为真实业务能力。

真实 A2A / Nacos / DashScope 端到端联调仍为待手动验证项；当前自动化测试只覆盖固定 JSON、Parser、Validator、Orchestrator 写入和 Web 委托，不调用真实外部模型 API。

## 2. 当前主线架构状态

- `mac-tav-web` 仍只作为 Web/API 入口，通过 `WorkflowOrchestrator` 触发工作流，不扫描、不聚合、不直接调用具体 Agent Bean。
- `mac-tav-orchestrator` 仍是唯一主编排入口，负责创建任务、调用远程 Agent、解析返回 DTO、写入 Model Core、追加 `AgentExecutionRecord`。
- Orchestrator 通过 `RemoteAgentInvoker` / A2A Client 调用远程专业 Agent，不构造 Prompt，不直接调用 `ChatModel` / `ReactAgent`。
- 当前已实现真实 Agent：`IntentAgent`、`PlanningAgent`。
- 所有阶段产物仍由 Orchestrator / Model Core 写入 `NetworkWorkspace` / `NetworkArtifact`；专业 Agent 不写 Workspace、不推进任务状态。
- `mac-tav-model-core` 当前仍是内存实现，不具备跨进程持久化能力。
- 自动化测试不调用真实模型 API，不使用 fake/offline Agent 主链路替代真实业务链路。

## 3. Phase 4 已完成能力

- `mac-tav-planning-agent` 已有独立 Spring Boot 启动类、`application.yml`、官方 SAA A2A / Nacos 注册配置，服务端口为 `18082`。
- `PlanningAgentConfiguration` 注册 `planningReactAgent` 命名 Bean 和 `PlanningAgent` Bean；ReactAgent 通过 `AgentUtils.reactAgentBuilder(...)` 创建，注入 `ChatModel`，声明 hooks、methodTools、`outputType(PlanningResponseSchema.class)`。
- `PlanningAgent` 是薄封装，注入 `ReactAgent`、`ObjectMapper`、`PlanningService`，执行模型调用后交给 `PlanningService` 做 `ResponseSchema -> Parser -> DTO -> Validator`。
- `planning-agent-prompt.md` 已要求输出结构化 `PlanningResponseSchema`，禁止输出 CLI，工具输出只作为提示。
- `PlanningResponseSchema` 覆盖拓扑、区域、地址、VLAN、路由、安全策略、NAT、约束、告警等规划字段。
- `PlanningResponseParser` 已补齐 `updateTime`、`createdBy`，并把 schema 转为 `NetworkPlan`，包含 `targetEnvironment`、`addressPlan`、`vlanPlan`、`routingPlan` 和 `traceRefs`。
- `PlanningOutputValidator` 已校验 `taskId`、`intentVersion`、拓扑、区域、`targetEnvironment`、`addressPlan`、`vlanPlan`、`routingPlan`、`traceRefs`、安全策略动作和 CLI 越界内容。
- Planning tools 已存在：`AddressPlanningTool`、`VlanPlanningTool`、`TopologyTemplateTool`、`PlanningPlaybookTool`；当前偏规则和关键字建议，不执行配置，不写 Workspace。
- `MacTavWorkflowOrchestrator#runPlanningStage` 已接入 Planning stage，读取当前 `NetworkIntent`，构造 `PlanningAgentInvokePayload`，调用远程 `PlanningAgent`，解析 `NetworkPlan`，写入 `NETWORK_PLAN` Artifact，并追加 `AgentExecutionRecord`。
- `WorkflowController` 暴露 `/api/v1/workflows/{taskId}/plan` 过渡调试入口；后续更完整工作流应统一到 rerun / continue-from 等流程接口。
- 测试已覆盖 Planning parser、validator、service、prompt、tool，以及 Orchestrator 的 Intent / Planning artifact 写入和 execution record 记录。

## 4. 当前可用能力总表

| 模块 | 当前可用能力 |
| --- | --- |
| `mac-tav-common` | 统一错误码、`BusinessException`、`ApiResponse` 等公共基础能力。 |
| `mac-tav-model` | 核心枚举、阶段产物 DTO、Workspace DTO、TraceRefs、Agent Card / A2A 契约、Intent / Planning 调用 payload。 |
| `mac-tav-agent-core` | `PromptLoader`、Parser / Validator 接口、`ValidationResult`、`AgentUtils.callSchema(ReactAgent, ...)`、通用 hook、`AgentRunContext`。 |
| `mac-tav-model-core` | 内存 Workspace、Artifact 版本、事件、执行记录、变更记录服务。 |
| `mac-tav-intent-agent` | 真实 IntentAgent 链路：命名 ReactAgent Bean、prompt、schema、parser、validator、tool、service、官方 A2A / Nacos 注册配置。 |
| `mac-tav-planning-agent` | 真实 PlanningAgent 链路：命名 ReactAgent Bean、prompt、schema、parser、validator、planning tools、service、官方 A2A / Nacos 注册配置。 |
| `mac-tav-orchestrator` | 远程 Agent 调用适配、Intent stage、Planning stage、Workspace / Artifact 写入闭环、AgentExecutionRecord 记录。 |
| `mac-tav-web` | 创建任务、运行 Intent stage、运行 Planning stage 过渡调试入口、查询 Workspace。 |
| `mac-tav-configuration-agent` | 下一阶段待实现；当前仅有最小包骨架、Qdrant 依赖和 `application.yml` 配置，不具备真实 ConfigurationAgent / RAG 能力。 |
| 其他后续模块 | Verification / Healing / Execution 仍是骨架或占位；Execution 已有资源配置但未实现真实受控执行适配器。 |

## 5. 当前技术债和待确认事项

- 真实 A2A / Nacos / DashScope 端到端联调仍待手动验证，需要用户启动 Nacos、配置 `aliApi-key`，分别启动 Agent / Web 服务后确认真实返回形态。
- `OfficialAgentCardRegistryClient#findByAgentName` 已可按名称查 Agent；但 `listAvailableAgents()` 当前仍只返回 `IntentAgent` 场景，后续多 Agent 列表发现需要扩展。
- `/api/v1/workflows/{taskId}/plan` 是 Phase 4 过渡调试接口；后续应统一到更明确的 rerun / continue-from / workflow progression 契约。
- Planning tools 当前偏关键字 / 规则化建议；Phase 5 需要更结构化地服务配置生成、模板选择和 RAG 检索。
- `mac-tav-configuration-agent` 已出现 Qdrant 依赖和配置，但尚未实现 schema、parser、validator、tools、agent configuration 或知识库 ingestion。
- Model Core 仍是内存实现，Phase 9 再做持久化。
- `ConfigurationAgent` / RAG / Template Tools 尚未实现。
- `ExecutionAdapter` / `VerificationAgent` / `HealingAgent` 尚未实现。
- Qdrant / Vector DB 尚未真实接入业务链路。
- Legacy fallback 类仍存在但默认关闭，不应恢复或扩展为新 Agent 模板。

## 6. Phase 5 进入前置条件

Phase 5 可以依赖以下已完成能力：

- 当前 `NetworkPlan` 已可由真实 `PlanningAgent` 输出，并可由 Orchestrator 作为 `NETWORK_PLAN` Artifact 写入 Workspace。
- `NetworkPlan` 当前包含 `topology`、`zones`、`addressPlan`、`vlanPlan`、`routingPlan`、`securityPolicyPlan`、`targetEnvironment`、`traceRefs` 等 Phase 5 所需输入。
- Planning 输出已有稳定 `taskId`、`intentVersion`、`planVersion`、`traceRefs`、`createdBy`、`updateTime`，可供 `ConfigSet` 做追溯。
- Orchestrator 已具备 Intent -> Planning 的阶段推进和 `AgentExecutionRecord` 记录方式，可作为 Configuration stage 接入模板。

## 7. Phase 5 下一步建议

Phase 5 只做：

- `ConfigurationAgent` 真实链路。
- `configuration-agent-prompt.md`。
- `ConfigurationResponseSchema`。
- `ConfigurationResponseParser`。
- `ConfigurationOutputValidator`。
- `ConfigTemplateTool`。
- `RagCommandSearchTool` / 命令知识库检索工具。
- Qdrant / VectorStore 最小接入。
- Markdown 配置知识库 ingestion。
- 输出结构化 `ConfigSet`。
- Orchestrator 接入 Configuration stage。
- Workspace / Artifact 写入 `CONFIG_SET`。
- Parser / Validator 固定 JSON 测试。
- 必要的 Orchestrator Configuration stage 测试。

Phase 5 不做：

- 不执行配置。
- 不接 Mininet / Ryu。
- 不实现 VerificationAgent。
- 不实现 HealingAgent。
- 不让 ConfigurationAgent 判断验证是否通过。
- 不让 RAG Tool 写 Workspace。
- 不让 RAG Tool 执行命令。
- 不返回一整段不可校验的命令文本。
- 不新增 fake/offline Agent 主链路。

## 8. 新 Codex 窗口推荐读取范围

进入 Phase 5 时优先读取：

1. `AGENTS.md`
2. `docs/CODEX_CURRENT_STATE.md`
3. `docs/CODEX_DOC_INDEX.md`
4. `docs/phase-handoffs/PHASE_04_HANDOFF.md`
5. `docs/06_DEV_PLAN.md` 中 Phase 5 相关部分
6. `docs/09_AGENT_BUILD_GUIDE.md` 中 ConfigurationAgent、Tool、RAG、Parser、Validator、A2A 相关部分
7. `docs/04_DATA_MODELS.md` 中 NetworkPlan、ConfigSet、CommandBlock、TraceRefs、NetworkWorkspace、NetworkArtifact 相关部分
8. `docs/02_MAVEN_MODULES.md` 中 mac-tav-configuration-agent 和 Vector DB / Qdrant 依赖边界
9. `docs/07_TEST_DATA_AND_SCENARIOS.md` 中 ConfigSet 和 ConfigurationAgent 非法输出场景
10. `docs/08_RUN_AND_TEST.md` 中 Nacos、真实 Agent、Qdrant / 环境变量 / 测试命令相关内容
11. 直接相关源码：`mac-tav-configuration-agent`、`mac-tav-planning-agent`、`mac-tav-orchestrator`、`mac-tav-model`、`mac-tav-model-core`、`mac-tav-agent-core`，必要时读取 `mac-tav-web`
