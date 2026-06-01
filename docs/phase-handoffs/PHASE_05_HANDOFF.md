# PHASE_05_HANDOFF

## 1. 交接目标

本文档用于新 Codex 窗口进入 Phase 6 前，快速理解 Phase 5 的真实代码状态、自动化测试结果、剩余手动验证和接手边界。

结论：Phase 5 已完成 ConfigurationAgent 真实链路、RAG / Template 最小能力、Orchestrator Configuration stage 接入、`CONFIG_SET` Artifact 写入和 `/config` 过渡调试入口。真实 A2A / Nacos / DashScope / Qdrant 端到端联调仍待用户准备环境后手动验证。

## 2. Phase 5 目标

Phase 5 的目标是实现 `ConfigurationAgent`，根据 `NetworkPlan` 生成结构化 `ConfigSet`，并由 Orchestrator 写入 `NetworkWorkspace` / `NetworkArtifact`。

Phase 5 不做：

- 不执行配置命令。
- 不接 Mininet / Ryu。
- 不实现 Execution / Verification / Healing。
- 不让 ConfigurationAgent 判断业务意图是否验证通过。
- 不让 Tool / RAG 写 Workspace。
- 不返回一整段不可校验的命令文本。
- 不新增 fake/offline Agent 主链路。

## 3. 已完成能力

### 3.1 DTO / 契约

- `ConfigurationAgentInvokePayload` 已加入 `mac-tav-model`。
- `ConfigSet` 已支持结构化配置输出所需字段：`taskId`、`planVersion`、`configVersion`、`targetEnvironment`、`generationSources`、`deviceConfigs`、`endpointConfigs`、`rollbackPlan`、`warnings`、`stageStatus`、`traceRefs`、`createTime`、`updateTime`、`createdBy`。
- `DeviceConfig` 支持 `deviceName`、`deviceType`、`commandBlocks`、`endpointConfig`、`traceRefs`。
- `CommandBlock` 支持 `blockId`、`commands`、`explanation`、`traceRefs`、`rollbackCommands`、`riskLevel`、`isIdempotent`。
- `GenerationSourceType` 支持 `LLM`、`RAG`、`TEMPLATE`、`RULE`、`TOOL`、`MCP`、`MANUAL_OVERRIDE`。

### 3.2 Schema / Parser / Validator

- `ConfigurationResponseSchema` 覆盖 `ConfigSet` 所需结构。
- `ConfigurationResponseParser` 将模型结构化输出转换为 `ConfigSet`，并由系统补齐状态和时间字段。
- `ConfigurationOutputValidator` 校验 task/version、deviceConfigs、commandBlocks、commands、rollbackCommands、traceRefs、generationSources 和越界内容。
- 固定样例 JSON 包含至少两个 deviceConfigs、多个 commandBlocks、generationSources、rollbackCommands、traceRefs。

### 3.3 Tool / RAG / Knowledge

- `ConfigTemplateTool` 已提供模板化配置建议。
- 华为 Markdown 知识库主 resources 文档为 `status=DRAFT` 空壳，等待用户填充真实命令知识。
- `HuaweiKnowledgeMarkdownParser` 和 `HuaweiKnowledgeIngestionService` 已实现最小 ingestion 链路。
- `RagCommandSearchTool` 已实现 VectorStore 检索封装。
- ingestion 不自动启动，需要显式调用；自动化测试使用离线 fixture，不连接真实 Qdrant。

### 3.4 ConfigurationAgent 主链路

- `configuration-agent-prompt.md` 已加入，明确结构化输出、追溯、回滚和越界禁止。
- `ConfigurationAgentApplication` 支持模块独立启动。
- `ConfigurationAgentConfiguration` 注册 `configurationReactAgent`、`ConfigurationAgent`、`ConfigurationService`、Parser、Validator、Tools。
- `ConfigurationAgent` 注入已装配的 `ReactAgent`，执行 `ResponseSchema -> Parser -> DTO -> Validator`。
- `ConfigurationService` 承接 Agent 内部 Parser / Validator 链路。
- ConfigurationAgent 不写 Workspace，不推进任务状态，不管理 Artifact。

### 3.5 Orchestrator / Web 接入

- `WorkflowOrchestrator#runConfigurationStage(taskId)` 已加入。
- `MacTavWorkflowOrchestrator#runConfigurationStage` 已实现：
  - 读取当前 `NetworkWorkspace`。
  - 校验已有 `currentPlan` 和 `NETWORK_PLAN` Artifact。
  - 构造 `ConfigurationAgentInvokePayload`。
  - 通过 `RemoteAgentInvoker` 调用 `ConfigurationAgent`。
  - 解析远程 `ConfigSet`。
  - 写入 `CONFIG_SET` Artifact。
  - 更新 `currentConfigSet`、`currentConfigVersion`、`currentArtifactRefs`。
  - 追加 `AgentExecutionRecord`。
- `POST /api/v1/workflows/{taskId}/config` 已加入 `WorkflowController`，仅委托 Orchestrator。

## 4. 关键文件清单

### Model

- `mac-tav-model/src/main/java/com/yali/mactav/model/config/ConfigurationAgentInvokePayload.java`
- `mac-tav-model/src/main/java/com/yali/mactav/model/config/ConfigSet.java`
- `mac-tav-model/src/main/java/com/yali/mactav/model/config/DeviceConfig.java`
- `mac-tav-model/src/main/java/com/yali/mactav/model/config/CommandBlock.java`
- `mac-tav-model/src/main/java/com/yali/mactav/model/config/GenerationSource.java`
- `mac-tav-model/src/main/java/com/yali/mactav/model/config/GenerationSourceType.java`

### Configuration Agent

- `mac-tav-configuration-agent/src/main/java/com/yali/mactav/configuration/ConfigurationAgentApplication.java`
- `mac-tav-configuration-agent/src/main/java/com/yali/mactav/configuration/config/ConfigurationAgentConfiguration.java`
- `mac-tav-configuration-agent/src/main/java/com/yali/mactav/configuration/agent/ConfigurationAgent.java`
- `mac-tav-configuration-agent/src/main/java/com/yali/mactav/configuration/service/ConfigurationService.java`
- `mac-tav-configuration-agent/src/main/java/com/yali/mactav/configuration/service/ConfigurationServiceImpl.java`
- `mac-tav-configuration-agent/src/main/java/com/yali/mactav/configuration/schema/ConfigurationResponseSchema.java`
- `mac-tav-configuration-agent/src/main/java/com/yali/mactav/configuration/parser/ConfigurationResponseParser.java`
- `mac-tav-configuration-agent/src/main/java/com/yali/mactav/configuration/validator/ConfigurationOutputValidator.java`
- `mac-tav-configuration-agent/src/main/java/com/yali/mactav/configuration/tool/ConfigTemplateTool.java`
- `mac-tav-configuration-agent/src/main/java/com/yali/mactav/configuration/tool/RagCommandSearchTool.java`
- `mac-tav-configuration-agent/src/main/java/com/yali/mactav/configuration/knowledge/HuaweiKnowledgeIngestionService.java`
- `mac-tav-configuration-agent/src/main/resources/prompts/configuration-agent-prompt.md`
- `mac-tav-configuration-agent/src/main/resources/knowledge/huawei/*.md`

### Orchestrator / Web

- `mac-tav-orchestrator/src/main/java/com/yali/mactav/orchestrator/service/WorkflowOrchestrator.java`
- `mac-tav-orchestrator/src/main/java/com/yali/mactav/orchestrator/service/MacTavWorkflowOrchestrator.java`
- `mac-tav-web/src/main/java/com/yali/mactav/web/controller/WorkflowController.java`

## 5. 测试覆盖

Phase 5 自动化测试覆盖：

- `ConfigurationResponseParserTest`
- `ConfigurationOutputValidatorTest`
- `ConfigTemplateToolTest`
- `HuaweiKnowledgeIngestionServiceTest`
- `RagCommandSearchToolTest`
- `ConfigurationServiceTest`
- `ConfigurationPromptTest`
- `MacTavWorkflowOrchestratorTest`
- `WebControllerTest`

覆盖点：

- 固定 JSON 可解析为 `ConfigSet`。
- Validator 拒绝非结构化配置文本、缺 traceRefs、空 generationSources、空 commands、空 rollbackCommands、执行/验证/修复越界声明。
- Template Tool 和 RAG Search Tool 的离线行为。
- DRAFT / placeholder Markdown 不会进入 ingestion。
- Intent -> Planning -> Configuration 的 Orchestrator 阶段顺序。
- `CONFIG_SET` Artifact 写入后 `currentConfigSet`、`currentConfigVersion`、`currentArtifactRefs` 一致。
- `/config` 过渡入口只委托 Orchestrator，不直接依赖 configuration-agent 模块。

自动化测试不调用真实模型、Nacos、Qdrant 或 embedding API。

## 6. 已运行命令

P13 收尾要求的最小验证命令：

```bash
mvn -pl mac-tav-configuration-agent -am test
mvn -pl mac-tav-orchestrator -am test
mvn -pl mac-tav-web -am test
mvn compile
```

结果：全部通过。

## 7. 手动端到端验证步骤

以下步骤尚未由自动化完成，需要用户准备环境后手动执行：

1. 启动 Nacos。
2. 配置真实模型 Key：`ALI_API_KEY`。
3. 如要验证 RAG 检索，启动 Qdrant，并确认 configuration-agent 的 VectorStore 配置指向正确地址。
4. 启动 `mac-tav-intent-agent`。
5. 启动 `mac-tav-planning-agent`。
6. 启动 `mac-tav-configuration-agent`。
7. 启动 `mac-tav-web`。
8. 调用任务创建接口创建任务。
9. 依次触发 Intent、Planning、Configuration 过渡入口。
10. 查询 Workspace，确认存在 `NETWORK_INTENT`、`NETWORK_PLAN`、`CONFIG_SET` Artifacts，以及 `currentConfigSet`、`currentConfigVersion`、`AgentExecutionRecord`。

注意：

- 不要直接调用专业 Agent 的 A2A Service 作为前端 public API。
- 不要回退成本地 Agent Bean 调用作为长期验收方式。
- 不要把真实 API Key 写入仓库或日志。

## 8. 当前过渡实现 / 架构债

- `/api/v1/workflows/{taskId}/config` 是 Phase 5 过渡调试入口，后续应统一进 workflow progression / rerun / continue-from API。
- 真实 A2A / Nacos / Agent Card / DashScope / Qdrant 端到端联调仍待手动验证。
- 华为主知识库文档仍是 DRAFT 空壳，真实命令内容待用户填充。
- Qdrant ingestion 需要显式调用，不随应用启动自动运行。
- `mac-tav-model-core` 仍是内存实现，重启后状态不持久。
- Orchestrator 侧历史 legacy fallback 相关类和测试仍存在，当前只能视为过渡架构债，不应复制为新 Agent 模板。
- `docs/04_DATA_MODELS.md` 中 ConfigSet 小节部分字段仍偏早期写法；当前代码已按 Phase 5 最小契约扩展为更完整的结构化 `ConfigSet`，后续可在独立文档整理任务中统一修订。

## 9. Phase 6 建议入口

Phase 6 应从 `mac-tav-execution` 入手，实现 `ExecutionAdapter`。

建议顺序：

1. 读取 `AGENTS.md`、`docs/CODEX_CURRENT_STATE.md`、`docs/CODEX_DOC_INDEX.md`。
2. 读取 `docs/06_DEV_PLAN.md` 中 Phase 6。
3. 读取 `docs/03_MODULE_DESIGN.md` 中 Execution Module、Orchestrator、Web Module。
4. 读取 `docs/04_DATA_MODELS.md` 中 `NetworkPlan`、`ConfigSet`、`ExecutionReport`、`NetworkWorkspace`、`NetworkArtifact`。
5. 读取 `docs/08_RUN_AND_TEST.md` 中 Mininet/Ryu 和测试要求。
6. 检查 `mac-tav-execution` 当前骨架和 `mac-tav-orchestrator` 当前阶段写入模式。

Phase 6 必须保持：

- Execution Module 不是纯 LLM Agent。
- 不执行 LLM 拼出来的任意 shell。
- 不直接执行 Huawei CLI。
- Controller 不接收任意 shell 命令。
- 通过 Adapter / Tool 白名单封装 Mininet、Ryu、Docker、Shell 等受控执行能力。
- 如 Mininet / Ryu 暂不可用，可实现结构校验模式验证 `NetworkPlan + ConfigSet -> ExecutionReport` 转换链路，但不得作为最终执行验收替代。
- 不开始 Verification / Healing。
