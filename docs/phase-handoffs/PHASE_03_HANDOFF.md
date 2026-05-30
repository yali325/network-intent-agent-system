# PHASE_03_HANDOFF

## 1. 交接目标

本文档用于新 Codex 窗口在接手 Phase 4 前，快速理解 Phase 1~3 的实际代码状态、主线架构、已完成能力和仍需确认的风险。本文档只总结现状，不设计 Phase 4 的实现细节。

**重要前提：Phase 3.5 已完成重构，项目不存在"离线链路"和"真实链路"双路径。所有 Agent 只有一条 Spring AI Alibaba 真实链路。**

## 2. Phase 1~3 已完成内容

### Phase 1

- 建立了 Maven 12 模块工程骨架，父工程统一管理内部模块版本和 Spring AI Alibaba 相关版本。
- mac-tav-common 已提供 ErrorCode、BusinessException、ApiResponse 等公共基础能力。
- mac-tav-model 已沉淀核心枚举、Workspace / Artifact / TraceRefs、阶段产物 DTO，以及 Agent Card / A2A 契约模型。
- mac-tav-agent-core 已提供 PromptLoader、AgentResponseParser、AgentOutputValidator、ValidationResult、AgentUtils 和项目内部 hook。
- AgentUtils 只保留单一 callSchema(ReactAgent, ...) 入口，不再有 SchemaAgentInvoker 双路径。

### Phase 2

- mac-tav-model 增加了 AgentCard、AgentCapability、AgentContract、AgentHealthStatus、A2aRequest、A2aResponse 等共享契约。
- mac-tav-orchestrator 增加了远程 Agent 发现、A2A 调用、响应校验、异常转换和 RemoteAgentInvoker / RemoteAgentTool 边界。
- A2A 主线已完全收敛到 Spring AI Alibaba 官方 A2A / Nacos Registry：依赖 spring-ai-alibaba-starter-a2a-nacos，通过 pplication.yml 和 ReactAgent Bean 自动装配。
- Orchestrator 侧 legacy fallback（HttpA2aClient、NacosAgentCardRegistryClient）已通过 @ConditionalOnProperty(prefix="mactav.legacy", havingValue="true") 默认关闭，不应继续作为主线扩展。

### Phase 3

- mac-tav-intent-agent 已实现 IntentAgent 真实链路：IntentAgentConfiguration 注册 ReactAgent Bean → IntentAgent 注入 → IntentResponseSchema → IntentResponseParser → IntentOutputValidator → NetworkIntent。
- 已实现 IntentAgentRequest、IntentAgentInvokePayload、IntentService / IntentServiceImpl、IntentExtractTool、IntentAgent 薄封装和 intent-agent-prompt.md。
- IntentAgent 边界：严格禁止输出设备、接口、VLAN、IP、拓扑、CLI；允许 OSPF 等协议偏好作为约束保留，不展开为规划或配置。
- IntentAgent 已作为独立 Spring Boot 服务存在，通过 pplication.yml 和命名 ReactAgent Bean 走官方 SAA A2A / Nacos 注册方向。
- Orchestrator 已能构造 IntentAgent A2A 请求、解析 NetworkIntent、写入 Model Core 的 NETWORK_INTENT Artifact，并更新 Workspace 当前版本。
- Web 已提供最小 API：创建任务、运行 Intent stage、查询 Workspace。

## 3. 当前核心模块状态

| 模块 | 当前职责 | 已有关键类 | 当前可用能力 | 不应误改的边界 |
| --- | --- | --- | --- | --- |
| mac-tav-common | 公共错误、异常、响应模型 | ErrorCode、BusinessException、ApiResponse | 统一错误码和 API 响应包装 | 不放业务 DTO、Agent、Workspace 逻辑 |
| mac-tav-model | 跨模块共享 DTO / 枚举 / 契约 | NetworkIntent、NetworkPlan、NetworkWorkspace、NetworkArtifact、A2aRequest、A2aResponse、AgentCard、IntentAgentInvokePayload | 阶段产物、Workspace 快照、Agent Card / A2A 契约 | 不依赖 Spring AI、Web、Orchestrator 或具体 Agent |
| mac-tav-agent-core | Agent 通用基础设施 | AgentUtils、PromptLoader、AgentRunContext、Parser / Validator 接口、hook 类 | Prompt 加载、ReactAgent 构造、结构化输出调用、异常转换 | 不放具体业务 prompt，不写 Workspace，不放 Orchestrator 远程调用实现 |
| mac-tav-model-core | Workspace / Artifact 状态中心 | InMemoryNetworkWorkspaceService、InMemoryNetworkArtifactService、ArtifactPayloadSerializer、NetworkArtifactFactory、各类 repository / validator | 内存 Workspace 创建、Artifact 保存、版本递增、事件/记录/变更追加 | 不调用模型，不生成 Plan/Config，不依赖 Agent/Web/Orchestrator |
| mac-tav-intent-agent | IntentAgent 独立阶段能力 | IntentAgentApplication、IntentAgentConfiguration、IntentAgent、IntentResponseSchema、IntentResponseParser、IntentOutputValidator、IntentExtractTool、命名 ReactAgent Bean | 自然语言需求到 NetworkIntent 的完整链路，以及官方 A2A server/registry 所需的 ReactAgent 注册 | 不写 Workspace，不推进任务状态，不依赖 Web/Orchestrator/Model Core |
| mac-tav-orchestrator | 主编排和远程 Agent 调用适配 | MacTavWorkflowOrchestrator、RemoteAgentInvoker、RemoteAgentTool、OfficialA2aClient、OfficialAgentCardRegistryClient | 创建任务、运行 Intent stage、远程调用 IntentAgent、写入 Workspace | 不直接依赖 mac-tav-intent-agent，不构造 Prompt，不调用 ChatModel/ReactAgent |
| mac-tav-web | Web/API 入口 | MacTavApplication、TaskController、WorkflowController | 创建任务、触发工作流当前阶段、查询 Workspace/Task/进度 | 不直接调用 Agent/ChatModel/ReactAgent，不扫描/装配具体 Agent Bean |

## 4. 当前 A2A / Agent Card / Nacos Registry 状态

- IntentAgent 侧已通过 IntentAgentConfiguration 注册命名 ReactAgent Bean，配合 pplication.yml 中的 spring.ai.alibaba.a2a.* 配置走官方 SAA A2A server + Nacos Registry 链路。
- Nacos 可用（127.0.0.1:8848），Agent 服务注册方向已确认。
- Orchestrator 侧主线类：OfficialAgentCardRegistryClient、OfficialA2aClient、RemoteAgentInvoker、MacTavWorkflowOrchestrator。
- Legacy 类（HttpA2aClient、NacosAgentCardRegistryClient）仍存在但默认关闭，不应继续作为主线扩展或新 Agent 模板复制。
- 真实 Nacos + 官方 A2A 端到端联通需手动验证，Nacos 已就绪。

## 5. Spring AI Alibaba 官方 Agent 构建规范（Codex MUST 遵守）

Phase 4 及后续实现 Agent 时：

1. ReactAgent Bean 通过 XxxAgentConfiguration（@Configuration 类）的 @Bean 方法注册，不在 XxxAgent 构造器内部 uild()。
2. ReactAgent Bean 方法直接通过参数注入 ChatModel，**禁止使用 @ConditionalOnBean(ChatModel.class)**：后者可能导致 Spring 解析阶段条件判断时序问题。
3. XxxAgent 注入 ReactAgent Bean，只做项目业务封装，执行 ResponseSchema → Parser → DTO → Validator。
4. 项目不存在"离线链路"和"真实链路"双路径。所有 Agent 只有一条 Spring AI Alibaba 真实链路。
5. 自动化测试使用固定样例 JSON 验证 Parser / Validator / Service，不调用真实模型 API。
6. Agent 模块编码时，需要的外部组件（Nacos 等）由用户开启或询问用户开启，不引入离线测试替身替代真实组件。

## 6. 当前 Model Core / Workspace 状态

- Workspace 创建：MacTavWorkflowOrchestrator#createTask 创建 NetworkTask，再调用 NetworkWorkspaceService#createWorkspace 创建初始 Workspace。
- 阶段产物保存：NetworkWorkspaceService#saveStageArtifact 自动生成 Artifact、序列化 payload、计算版本、保存 Artifact、更新 currentArtifactRefs 和当前阶段版本。
- Artifact 版本机制：同一 	askId + ArtifactType 下版本递增；旧 Artifact 保留，可通过 Artifact service 查询；Workspace 当前视图指向最新版本。
- 当前状态流转：已覆盖任务创建、Intent stage 运行中、Intent 成功写入、失败时标记 ERROR 和记录简要错误。
- 新阶段接入时优先复用：NetworkWorkspaceService、NetworkArtifactService、WorkspaceEventService、AgentExecutionRecordService、WorkspaceChangeRecordService、ArtifactPayloadSerializer、NetworkArtifactFactory。
- 当前实现全部是内存实现，进程重启后状态不会保留，后续 Phase 9 才应替换为 MySQL / Redis / 持久化实现。

## 7. 当前测试和运行状态

- 自动化测试覆盖了 Agent Core prompt / validator / AgentUtils、Intent parser / validator / service / prompt / tool、Model Core Workspace / Artifact、Orchestrator remote / workflow、Web Controller 等路径。
- 自动化测试使用固定 JSON 数据，不调用真实模型 API，不依赖真实 Nacos。
- 当前代码存在手动验证入口和官方 A2A 配置，真实 Nacos + DashScope + 两服务启动的端到端验证需要人工运行。
- 新窗口接手前建议至少运行 mvn compile，涉及 Phase 4 前再运行相关模块测试。
- 最近精确构建结果以新窗口本地重新运行为准，若失败应先确认是否由未提交配置、依赖下载或环境变量差异导致。

## 8. 当前架构债和风险

| 风险 | 影响 | 建议 |
| --- | --- | --- |
| Orchestrator 侧 legacy A2A fallback 仍存在（默认关闭） | 新窗口可能误以为手写 HTTP JSON 是主线 | 继续使用官方 SAA A2A 主线，不扩展 legacy 类 |
| 官方 A2A 直接暴露 ReactAgent 后的 output 形态待联调确认 | Orchestrator 当前保存 NetworkIntent，若远端返回 IntentResponseSchema 需补充明确转换边界 | 先做真实 Nacos + A2A 手动验证，再决定是否调整远程响应契约 |
| 官方 A2A / Nacos 端到端联调需手动验证 | 运行时装配或协议细节可能暴露问题 | Phase 4 前或过程中安排一次手动集成验证 |
| 环境变量已统一为 liApi-key | 文档一致性已修复 | 无需额外操作 |
| OfficialAgentCardRegistryClient#listAvailableAgents 偏 IntentAgent | 多 Agent 后列表能力不足 | 后续多 Agent 接入时按官方 discovery 扩展 |
| Model Core 仅内存实现 | 无持久化、无跨进程恢复 | Phase 9 再替换，Phase 4 不要提前接数据库 |
| 其他 Agent 模块已存在但未实现 | 容易误判为可用能力 | Phase 4 只实现 PlanningAgent，不批量生成其他 Agent |
| Orchestrator 远程调用装配依赖官方 provider Bean | 真实 Boot 环境下仍需确认条件装配 | 手动验证时重点检查 provider Bean 和 client bean 是否生效 |

## 9. Phase 4 接手建议

Phase 4 进入 PlanningAgent 真实链路：基于已有 NetworkIntent 构造 PlanningResponseSchema → Parser → NetworkPlan → Validator，配套 prompt 和自动化测试（固定 JSON 验证 Parser / Validator，不调用真实模型 API）。

**不创建"离线链路"、测试替身或 SchemaAgentInvoker。**

Phase 4 MUST NOT：

- 创建 ConfigurationAgent、VerificationAgent、HealingAgent
- 接 Mininet / Ryu
- 引入双链路（真实 + 离线/测试替身）
- 破坏既有 IntentAgent / A2A 主线
- 让 Orchestrator 或 Web 直接依赖具体 Agent 实现类
- 让 PlanningAgent 写 Workspace 或推进任务状态

如果用户要求接入 PlanningAgent 服务化，应沿用当前官方 SAA A2A 方向；如果用户只要求核心链路，应先保持 Parser / Validator / DTO / prompt 的最小闭环。

## 10. 新 Codex 窗口推荐读取顺序

1. AGENTS.md
2. docs/CODEX_CURRENT_STATE.md
3. docs/CODEX_DOC_INDEX.md
4. docs/phase-handoffs/PHASE_03_HANDOFF.md（本文档）
5. docs/06_DEV_PLAN.md 中 Phase 4 相关部分
6. docs/09_AGENT_BUILD_GUIDE.md 中 Agent 模块结构 / Parser / Validator / Prompt 规范
7. docs/04_DATA_MODELS.md 中 NetworkIntent / NetworkPlan / TraceRefs 相关部分
8. Phase 4 直接涉及的代码文件：mac-tav-planning-agent、mac-tav-agent-core、mac-tav-model，必要时再读 Orchestrator / Model Core 边界类
