# PHASE_03_HANDOFF

## 1. 交接目标

本文档用于新 Codex 窗口在接手 Phase 4 前，快速理解 Phase 1~3 的实际代码状态、主线架构、已完成能力和仍需确认的风险。本文档只总结现状，不设计 Phase 4 的实现细节。

## 2. Phase 1~3 已完成内容

### Phase 1

- 建立了 Maven 12 模块工程骨架，父工程统一管理内部模块版本和 Spring AI Alibaba 相关版本。
- `mac-tav-common` 已提供 `ErrorCode`、`BusinessException`、`ApiResponse` 等公共基础能力。
- `mac-tav-model` 已沉淀核心枚举、Workspace / Artifact / TraceRefs、阶段产物 DTO，以及 Agent Card / A2A 契约模型。
- `mac-tav-agent-core` 已提供 `AgentRunContext`、`PromptLoader`、`AgentResponseParser`、`AgentOutputValidator`、`ValidationResult`、`SchemaAgentInvoker`、`AgentUtils` 和项目内部 hook。
- `AgentUtils` 当前已经接入可编译的 Spring AI Alibaba `ReactAgent` / Spring AI `ChatModel` 类型，同时保留 `SchemaAgentInvoker` 作为离线测试边界。

### Phase 2

- `mac-tav-model` 增加了 `AgentCard`、`AgentCapability`、`AgentContract`、`AgentHealthStatus`、`A2aRequest`、`A2aResponse` 等共享契约。
- `mac-tav-orchestrator` 增加了远程 Agent 发现、A2A 调用、响应校验、异常转换和 `RemoteAgentInvoker` / `RemoteAgentTool` 边界。
- 当前 A2A 主线已转向 Spring AI Alibaba 官方 A2A / Nacos Registry：Orchestrator 侧优先使用官方 provider / client 适配。
- IntentAgent 侧手写 HTTP JSON endpoint 和自定义 A2A executor 已删除；Orchestrator 侧手写 HTTP JSON 和 Nacos Config 读取代码仍保留为 legacy fallback，不应继续作为主线扩展。

### Phase 3

- `mac-tav-intent-agent` 已实现 IntentAgent 离线核心链路：`IntentResponseSchema -> IntentResponseParser -> IntentOutputValidator -> NetworkIntent`。
- 已实现 `IntentAgentRequest`、`IntentAgentInvokePayload`、`IntentService` / `IntentServiceImpl`、`IntentExtractTool`、`IntentAgent` 薄封装和 `intent-agent-prompt.md`。
- IntentAgent 当前边界要求严格禁止输出设备、接口、VLAN、IP、拓扑、CLI；允许 OSPF 等协议偏好作为约束或偏好保留，不展开为规划或配置。
- IntentAgent 已作为独立 Spring Boot 服务存在，通过配置项和命名 `ReactAgent` Bean 走官方 SAA A2A / Nacos 注册方向。
- Orchestrator 已能在当前代码层构造 IntentAgent A2A 请求、解析 `NetworkIntent`、写入 Model Core 的 `NETWORK_INTENT` Artifact，并更新 Workspace 当前版本。
- Web 已提供最小 API：创建任务、运行 Intent stage、查询 Workspace。

## 3. 当前核心模块状态

| 模块 | 当前职责 | 已有关键类 | 当前可用能力 | 不应误改的边界 |
| --- | --- | --- | --- | --- |
| `mac-tav-common` | 公共错误、异常、响应模型 | `ErrorCode`、`BusinessException`、`ApiResponse` | 统一错误码和 API 响应包装 | 不放业务 DTO、Agent、Workspace 逻辑 |
| `mac-tav-model` | 跨模块共享 DTO / 枚举 / 契约 | `NetworkIntent`、`NetworkPlan`、`NetworkWorkspace`、`NetworkArtifact`、`A2aRequest`、`A2aResponse`、`AgentCard`、`IntentAgentInvokePayload` | 阶段产物、Workspace 快照、Agent Card / A2A 契约 | 不依赖 Spring AI、Web、Orchestrator 或具体 Agent |
| `mac-tav-agent-core` | Agent 通用基础设施 | `AgentUtils`、`PromptLoader`、`AgentRunContext`、`SchemaAgentInvoker`、Parser / Validator 接口、hook 类 | Prompt 加载、ReactAgent 构造、结构化输出调用、异常转换、离线测试边界 | 不放具体业务 prompt，不写 Workspace，不放 Orchestrator 远程调用实现 |
| `mac-tav-model-core` | Workspace / Artifact 状态中心 | `InMemoryNetworkWorkspaceService`、`InMemoryNetworkArtifactService`、`ArtifactPayloadSerializer`、`NetworkArtifactFactory`、各类 repository / validator | 内存 Workspace 创建、Artifact 保存、版本递增、事件/记录/变更追加 | 不调用模型，不生成 Plan/Config，不依赖 Agent/Web/Orchestrator |
| `mac-tav-intent-agent` | IntentAgent 独立阶段能力 | `IntentAgentApplication`、`IntentAgent`、`IntentResponseSchema`、`IntentResponseParser`、`IntentOutputValidator`、`IntentExtractTool`、命名 `ReactAgent` Bean | 自然语言需求到 `NetworkIntent` 的离线链路，以及官方 A2A server/registry 所需的 ReactAgent 注册 | 不写 Workspace，不推进任务状态，不依赖 Web/Orchestrator/Model Core |
| `mac-tav-orchestrator` | 主编排和远程 Agent 调用适配 | `MacTavWorkflowOrchestrator`、`RemoteAgentInvoker`、`RemoteAgentTool`、`OfficialA2aClient`、`OfficialAgentCardRegistryClient` | 创建任务、运行 Intent stage、远程调用 IntentAgent、写入 Workspace | 不直接依赖 `mac-tav-intent-agent`，不构造 Prompt，不调用 ChatModel/ReactAgent |
| `mac-tav-web` | Web/API 入口 | `MacTavApplication`、`TaskController`、`WorkflowController`、`WorkspaceController`、`GlobalExceptionHandler` | 最小任务 API 和统一响应 | 不扫描具体 Agent Bean，不直接调用 Agent/ChatModel/Prompt |
| 其他已创建模块 | 后续阶段占位 | `mac-tav-planning-agent`、`mac-tav-configuration-agent`、`mac-tav-execution`、`mac-tav-verification-agent`、`mac-tav-healing-agent` | Maven 模块存在，部分只有最小包结构或基础依赖 | 不应被新窗口误认为已有真实业务能力 |

## 4. 当前 A2A / Nacos / Agent Card 状态

- 当前 A2A 方向：使用官方 Spring AI Alibaba A2A / Nacos 能力，是主线方向。
- IntentAgent 暴露与注册：`mac-tav-intent-agent` 通过 `application.yml` 中 `spring.ai.alibaba.a2a.server.*` 和 `spring.ai.alibaba.a2a.nacos.*` 配置描述 Agent Card / Nacos 注册；通过 `@Bean(name = "IntentAgent")` 注册本地 `ReactAgent`，由 SAA starter 负责 `GraphAgentExecutor`、协议入口和注册。
- Orchestrator 调用方式：`mac-tav-web` 运行时扫描 Orchestrator 与 Model Core，Orchestrator 通过 `RemoteAgentInvoker` 进入远程调用边界；在官方 provider 可用时使用 `OfficialAgentCardRegistryClient` / `OfficialA2aClient`，并由 `A2aRemoteAgent` 走官方 A2A 调用。
- legacy fallback：IntentAgent 侧 legacy HTTP endpoint 已删除；Orchestrator 侧仍存在 `HttpA2aClient`、`NacosAgentCardRegistryClient`，用于过渡兼容。
- 当前主线类：`IntentAgentConfiguration` 中的命名 `ReactAgent` Bean、`OfficialAgentCardRegistryClient`、`OfficialA2aClient`、`RemoteAgentInvoker`、`MacTavWorkflowOrchestrator`。
- 过渡或兼容类：`HttpA2aClient`、`NacosAgentCardRegistryClient`。
- 真实 Nacos + 官方 A2A 端到端联通仍需手动验证，不能仅凭离线测试视为生产可用。

## 5. 当前 Model Core / Workspace 状态

- Workspace 创建：`MacTavWorkflowOrchestrator#createTask` 创建 `NetworkTask`，再调用 `NetworkWorkspaceService#createWorkspace` 创建初始 Workspace。
- 阶段产物保存：`NetworkWorkspaceService#saveStageArtifact` 自动生成 Artifact、序列化 payload、计算版本、保存 Artifact、更新 `currentArtifactRefs` 和当前阶段版本。
- Artifact 版本机制：同一 `taskId + ArtifactType` 下版本递增；旧 Artifact 保留，可通过 Artifact service 查询；Workspace 当前视图指向最新版本。
- 当前状态流转：已覆盖任务创建、Intent stage 运行中、Intent 成功写入、失败时标记 ERROR 和记录简要错误。
- 新阶段接入时优先复用：`NetworkWorkspaceService`、`NetworkArtifactService`、`WorkspaceEventService`、`AgentExecutionRecordService`、`WorkspaceChangeRecordService`、`ArtifactPayloadSerializer`、`NetworkArtifactFactory`。
- 当前实现全部是内存实现，进程重启后状态不会保留，后续 Phase 9 才应替换为 MySQL / Redis / 持久化实现。

## 6. 当前测试和运行状态

- 自动化测试覆盖了 Agent Core prompt / validator / AgentUtils、Intent parser / validator / service / prompt / tool、Model Core Workspace / Artifact、Orchestrator remote / workflow、Web Controller 等离线路径。
- 自动化测试不应调用真实模型 API，不应依赖真实 Nacos。
- 当前代码存在手动验证入口和官方 A2A 配置，但真实 Nacos + DashScope + 两服务启动的端到端验证需要人工运行。
- 本轮文档交接任务未重新运行 Maven；新窗口接手前建议至少运行 `mvn compile`，涉及 Phase 4 前再运行相关模块测试。
- 最近精确构建结果以新窗口本地重新运行为准，若失败应先确认是否由未提交配置、依赖下载或环境变量差异导致。

## 7. 当前架构债和风险

| 风险 | 影响 | 建议 |
| --- | --- | --- |
| Orchestrator 侧 legacy A2A fallback 仍存在 | 新窗口可能误以为手写 HTTP JSON 是主线 | 继续使用官方 SAA A2A 主线，不扩展 legacy 类 |
| 官方 A2A 直接暴露 ReactAgent 后的 output 形态待联调确认 | Orchestrator 当前保存 `NetworkIntent`，若远端返回 `IntentResponseSchema` 需补充明确转换边界 | 先做真实 Nacos + A2A 手动验证，再决定是否调整远程响应契约 |
| 官方 A2A / Nacos 未在真实环境完整联调 | 运行时装配或协议细节可能暴露问题 | Phase 4 前或过程中安排一次手动集成验证 |
| `docs/08_RUN_AND_TEST.md` 可能仍提旧 API Key 变量 | 与当前 `aliApi-key` 安全规则冲突 | 后续文档修订时统一为 `aliApi-key` |
| `OfficialAgentCardRegistryClient#listAvailableAgents` 偏 IntentAgent | 多 Agent 后列表能力不足 | 后续多 Agent 接入时按官方 discovery 扩展 |
| Model Core 仅内存实现 | 无持久化、无跨进程恢复 | Phase 9 再替换，Phase 4 不要提前接数据库 |
| 其他 Agent 模块已存在但未实现 | 容易误判为可用能力 | Phase 4 只实现 PlanningAgent，不批量生成其他 Agent |
| Orchestrator 远程调用装配依赖官方 provider Bean | 真实 Boot 环境下仍需确认条件装配 | 手动验证时重点检查 provider Bean 和 client bean 是否生效 |

## 8. Phase 4 接手建议

Phase 4 应只进入 PlanningAgent 离线核心链路：基于已有 `NetworkIntent` 构造 `PlanningResponseSchema -> Parser -> NetworkPlan -> Validator`，配套 prompt 和离线测试。

Phase 4 不应该做：

- 不提前实现 ConfigurationAgent、VerificationAgent、HealingAgent。
- 不提前接 Mininet / Ryu。
- 不生成 CLI、设备命令或执行适配逻辑。
- 不破坏现有 IntentAgent / A2A 主线。
- 不让 Orchestrator 或 Web 直接依赖具体 Agent 实现类。
- 不让 PlanningAgent 写 Workspace 或推进任务状态。

如果用户要求接入 PlanningAgent 服务化，应沿用当前官方 SAA A2A 方向；如果用户只要求离线核心链路，应先保持 Parser / Validator / DTO / prompt 的最小闭环。

## 9. 新 Codex 窗口推荐读取顺序

1. `AGENTS.md`
2. `docs/CODEX_CURRENT_STATE.md`
3. `docs/CODEX_DOC_INDEX.md`
4. `docs/phase-handoffs/PHASE_03_HANDOFF.md`
5. `docs/06_DEV_PLAN.md` 中 Phase 4 相关部分
6. `docs/09_AGENT_BUILD_GUIDE.md` 中 Agent 模块结构 / Parser / Validator / Prompt 规范
7. `docs/04_DATA_MODELS.md` 中 `NetworkIntent` / `NetworkPlan` / `TraceRefs` 相关部分
8. Phase 4 直接涉及的代码文件：`mac-tav-planning-agent`、`mac-tav-agent-core`、`mac-tav-model`，必要时再读 Orchestrator / Model Core 边界类
