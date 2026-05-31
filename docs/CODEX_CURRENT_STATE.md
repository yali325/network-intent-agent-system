# CODEX_CURRENT_STATE

## 1. 当前项目阶段

当前项目已完成 Phase 1、Phase 2、Phase 3、Phase 4 和 Phase 5 的主体落地。

Phase 5 的完成内容是 ConfigurationAgent 真实链路和 Orchestrator 配置阶段闭环：

```text
NetworkPlan
  -> ConfigurationAgent
  -> ConfigurationResponseSchema
  -> ConfigurationResponseParser
  -> ConfigSet
  -> ConfigurationOutputValidator
  -> Orchestrator runConfigurationStage
  -> CONFIG_SET Artifact / NetworkWorkspace
```

当前自动化测试只覆盖固定 JSON、Parser、Validator、Tool、RAG ingestion、Orchestrator 写入和 Web 委托，不调用真实外部模型 API，不连接真实 Nacos / Qdrant。

真实 A2A / Nacos / DashScope / Qdrant 端到端联调仍待手动验证。

## 2. 当前主线架构状态

- `mac-tav-web` 仍只作为 Web/API 入口，通过 `WorkflowOrchestrator` 触发工作流，不扫描、不聚合、不直接调用具体 Agent Bean。
- `mac-tav-orchestrator` 是唯一主编排入口，负责创建任务、调用远程 Agent、解析远程 DTO、写入 Model Core、追加 `AgentExecutionRecord`。
- Orchestrator 通过 `RemoteAgentInvoker` / A2A Client 调用远程专业 Agent，不构造 Prompt，不直接调用 `ChatModel` / `ReactAgent`。
- 专业 Agent 只负责阶段能力，不直接写 `NetworkWorkspace`，不推进任务状态，不管理 Artifact 版本。
- 所有阶段产物由 Orchestrator / Model Core 写入 `NetworkWorkspace` / `NetworkArtifact`。
- `mac-tav-model-core` 当前仍是内存实现，不具备跨进程持久化能力。
- 自动化测试不调用真实模型 API，不使用 fake/offline Agent 主链路替代真实业务链路。

## 3. Phase 5 已完成能力

### 3.1 Model / DTO 契约

- `ConfigurationAgentInvokePayload` 已放在 `mac-tav-model`，用于 Orchestrator 与 ConfigurationAgent 的 A2A 调用契约。
- `ConfigSet` 可表达 `taskId`、`planVersion`、`configVersion`、`targetEnvironment`、`generationSources`、`deviceConfigs`、`endpointConfigs`、`rollbackPlan`、`warnings`、`stageStatus`、`traceRefs`、`createTime`。
- `DeviceConfig`、`CommandBlock`、`GenerationSource`、`GenerationSourceType`、`RollbackPlan`、`TraceRefs` 已满足 Phase 5 结构化配置生成的最小契约。
- `GenerationSourceType` 支持 `LLM`、`RAG`、`TEMPLATE`、`RULE`、`TOOL`、`MCP`、`MANUAL_OVERRIDE`。

### 3.2 Configuration Agent 结构化输出链路

- `ConfigurationResponseSchema` 已实现，覆盖 ConfigSet 所需结构。
- `ConfigurationResponseParser` 已实现，将 schema 转换为 `ConfigSet`，并补齐系统字段默认值。
- `ConfigurationOutputValidator` 已实现，拒绝空设备配置、空 commandBlocks、空 commands、缺 traceRefs、空 generationSources、空 sourceType、非结构化 configText、执行/验证/修复越界声明。
- 固定样例 JSON 已放入 `mac-tav-configuration-agent/src/test/resources`，覆盖多设备、多 commandBlocks、generationSources、rollbackCommands、traceRefs。

### 3.3 Tools / RAG / Knowledge

- `ConfigTemplateTool` 已实现，用于提供结构化配置模板建议。
- Markdown 华为知识库 DRAFT 空壳已放入 `mac-tav-configuration-agent/src/main/resources/knowledge/huawei`。
- `HuaweiKnowledgeMarkdownParser`、`HuaweiKnowledgeIngestionService` 已实现 Qdrant / VectorStore ingestion 最小链路。
- `RagCommandSearchTool` 已实现，用于结构化命令知识检索。
- ingestion 不自动启动，需要显式调用。
- 主 resources 下的华为知识库文档当前为 `status=DRAFT` 空壳，具体命令内容待用户填写；测试 resources 内有 READY fixture 用于离线测试。

### 3.4 ConfigurationAgent 主链路

- `configuration-agent-prompt.md` 已实现，约束模型输出结构化 `ConfigurationResponseSchema`，禁止一整段命令文本、执行声明、验证闭环和修复闭环。
- `ConfigurationAgentApplication` 已实现，支持 configuration-agent 独立启动。
- `ConfigurationAgentConfiguration` 已实现，注册 `configurationReactAgent`、ConfigurationAgent、Parser、Validator、Service、Tools，并使用 Spring AI Alibaba 官方 starter 配置路线。
- `ConfigurationAgent` 已实现，封装 `ReactAgent` 调用，执行 `ResponseSchema -> Parser -> DTO -> Validator`，返回 `ConfigSet`。
- `ConfigurationService` / `ConfigurationServiceImpl` 已实现，承接 Agent 内部 Parser / Validator 链路。
- ConfigurationAgent 不写 Workspace，不推进任务状态，不管理 Artifact。

### 3.5 Orchestrator / Web 接入

- `WorkflowOrchestrator#runConfigurationStage(taskId)` 已实现。
- `MacTavWorkflowOrchestrator#runConfigurationStage` 会读取当前 Workspace，确认已有 `currentPlan` 和 `NETWORK_PLAN` Artifact，构造 `ConfigurationAgentInvokePayload`，通过 `RemoteAgentInvoker` 调用 `ConfigurationAgent`。
- ConfigurationAgent 远程返回的 `ConfigSet` 会由 Orchestrator 写入：
  - `currentConfigSet`
  - `currentConfigVersion`
  - `CONFIG_SET` Artifact
  - `currentArtifactRefs`
  - `WorkspaceEvent`
  - `AgentExecutionRecord`
- `POST /api/v1/workflows/{taskId}/config` 已作为 Phase 5 过渡调试入口加入 `mac-tav-web`。
- Web Controller 只调用 `WorkflowOrchestrator.runConfigurationStage(taskId)`，不直接依赖 configuration-agent 模块。

## 4. 当前可用能力总表

| 模块 | 当前可用能力 |
| --- | --- |
| `mac-tav-common` | 统一错误码、`BusinessException`、`ApiResponse` 等公共基础能力。 |
| `mac-tav-model` | 核心枚举、阶段产物 DTO、Workspace DTO、TraceRefs、A2A 契约、Intent / Planning / Configuration 调用 payload。 |
| `mac-tav-agent-core` | `AgentUtils`、Prompt 加载、Parser / Validator 接口、ValidationResult、AgentRunContext、通用 hook。 |
| `mac-tav-model-core` | 内存 Workspace、Artifact 版本、事件、执行记录、变更记录服务。 |
| `mac-tav-intent-agent` | 真实 IntentAgent 链路：命名 ReactAgent Bean、prompt、schema、parser、validator、tool、service、官方 A2A / Nacos 配置。 |
| `mac-tav-planning-agent` | 真实 PlanningAgent 链路：命名 ReactAgent Bean、prompt、schema、parser、validator、planning tools、service、官方 A2A / Nacos 配置。 |
| `mac-tav-configuration-agent` | 真实 ConfigurationAgent 链路：schema、parser、validator、template tool、RAG search tool、Markdown knowledge ingestion、prompt、service、官方 A2A / Nacos 配置。 |
| `mac-tav-orchestrator` | 远程 Agent 调用适配、Intent stage、Planning stage、Configuration stage、Workspace / Artifact 写入闭环、AgentExecutionRecord 记录。 |
| `mac-tav-web` | 创建任务、运行 Intent / Planning / Configuration 过渡调试入口、查询 Workspace。 |
| `mac-tav-execution` | 仍未实现 Phase 6 ExecutionAdapter 主体能力。 |
| `mac-tav-verification-agent` | 尚未实现真实 VerificationAgent。 |
| `mac-tav-healing-agent` | 尚未实现真实 HealingAgent。 |

## 5. 当前测试覆盖

- Configuration parser：固定 JSON -> `ConfigSet`。
- Configuration validator：合法输出、空 deviceConfigs、空 commandBlocks、空 commands、缺 traceRefs、空 generationSources、sourceType 为 null、非结构化 configText、执行/验证越界内容。
- Template tool：配置模板建议。
- Huawei knowledge ingestion：Markdown front matter 解析、仅 READY 且非 placeholder 文档入库，DRAFT 文档跳过。
- RAG search tool：离线 VectorStore fixture 搜索。
- Configuration service：Parser / Validator 组合链路。
- Configuration prompt：prompt 存在性和关键边界词。
- Orchestrator：Intent -> Planning -> Configuration 阶段顺序、CONFIG_SET Artifact 写入、currentConfigSet/currentConfigVersion/currentArtifactRefs、AgentExecutionRecord。
- Web：`/config` 过渡入口只委托 Orchestrator。

## 6. 当前过渡实现和待验证事项

- 真实 A2A / Nacos / DashScope / Qdrant 端到端联调仍需手动验证。
- `/api/v1/workflows/{taskId}/config` 是 Phase 5 过渡调试入口，后续应统一进 workflow progression / rerun / continue-from API。
- 主 resources 下华为知识库文档仍是 `status=DRAFT` 空壳，具体命令知识待用户填写。
- Qdrant ingestion 不自动启动，需要显式调用，避免应用启动时误写向量库。
- `mac-tav-model-core` 仍是内存实现，持久化和异步任务属于后续 Phase 9。
- Orchestrator 侧仍存在历史 legacy fallback 相关类和测试，当前应视为过渡架构债，不应作为新 Agent 模板复制或扩展。
- Execution / Verification / Healing 尚未实现。

## 7. Phase 6 下一步边界

Phase 6 只进入 ExecutionAdapter + Mininet/Ryu 或结构校验模式，不实现 Verification / Healing。

Phase 6 应做：

- 实现 `ExecutionAdapter`。
- 按文档边界实现 Mininet/Ryu 或结构校验模式。
- 将 `NetworkPlan + ConfigSet` 转换为受控执行计划和 `ExecutionReport`。
- 将执行结果写入 `NetworkWorkspace` / `NetworkArtifact`。

Phase 6 必须遵守：

- Execution Module 不是纯 LLM Agent。
- 不执行 LLM 拼出来的任意 shell。
- 不直接执行 Huawei CLI。
- Controller 不接收任意 shell 命令。
- 通过 Adapter / Tool 白名单封装 Mininet、Ryu、Docker、Shell 等受控执行能力。
- 不让 Execution Module 判断业务意图是否达成；该职责属于后续 VerificationAgent。

## 8. 新 Codex 窗口推荐读取范围

进入 Phase 6 时优先读取：

1. `AGENTS.md`
2. `docs/CODEX_CURRENT_STATE.md`
3. `docs/CODEX_DOC_INDEX.md`
4. `docs/phase-handoffs/PHASE_05_HANDOFF.md`
5. `docs/06_DEV_PLAN.md` 中 Phase 6 相关内容
6. `docs/03_MODULE_DESIGN.md` 中 Execution Module、Orchestrator、Web Module 相关内容
7. `docs/04_DATA_MODELS.md` 中 NetworkPlan、ConfigSet、ExecutionReport、NetworkWorkspace、NetworkArtifact 相关小节
8. `docs/08_RUN_AND_TEST.md` 中测试命令、Mininet/Ryu、手动验证要求
9. 直接相关源码：`mac-tav-execution`、`mac-tav-orchestrator`、`mac-tav-model`、`mac-tav-model-core`，必要时读取 `mac-tav-web`
