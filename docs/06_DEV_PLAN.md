# 长期实现路线

## 1. 文档目标

本文档定义 MAC-TAV 从当前工程基础走向长期完整系统的实现路线。

本文档的阶段划分用于指导 Codex 和项目成员按顺序开发。每个阶段都应明确：

1. 目标。
2. 做什么。
3. 不做什么。
4. 验收标准。
5. 依赖前置。

本文档不负责详细 Maven 模块拆分、DTO 全字段、API 详细路径或 Agent 初始化代码。这些内容由对应专项文档维护。

## 2. 总体实施原则

长期实施必须遵守：

1. 先稳定架构和 Agent Core，再逐个实现真实 Agent。
2. 不一次性实现所有 Agent、MCP、A2A、Skills。
3. 每个阶段必须保持主调用链稳定：

```text
Controller -> Orchestrator -> Service -> Agent -> ResponseSchema -> Parser -> DTO -> Validator -> Workspace
```

4. 所有真实 Agent 必须遵守 `docs/09_AGENT_BUILD_GUIDE.md`。
5. 所有阶段产物必须进入 `NetworkWorkspace`。
6. Execution 能力必须通过 `ExecutionAdapter` 接入。
7. API Key 不允许硬编码。
8. 单元测试不真实调用外部模型 API。
9. Orchestrator 不构造 Prompt，不直接调用模型。
10. Controller 不直接调用具体 Agent、`ChatModel`、`ReactAgent` 或外部执行命令。

## 3. Phase 0：长期文档体系统一

### 目标

将 `AGENTS.md` 和 `docs/00-09` 文档统一为长期项目文档。

### 做什么

1. 统一项目长期定位。
2. 统一 Maven 模块名称。
3. 统一数据模型。
4. 统一 API 规范。
5. 统一 Agent 构建规范。
6. 统一运行与测试规范。

### 不做什么

1. 不写业务代码。
2. 不改 `pom.xml`。
3. 不接真实外部服务。

### 验收标准

1. 文档中不再把本地替身或降级实现作为长期主流程。
2. 各文档职责不重复。
3. `AGENTS.md` 与 `docs/09_AGENT_BUILD_GUIDE.md` 对齐。
4. 长期模块、DTO、API、Agent 构建规范之间没有明显冲突。

### 依赖前置

无。该阶段是后续长期实现路线的基础。

## 4. Phase 1：Agent Core 基础设施

### 目标

建立所有真实 Spring AI Alibaba Agent 共用的基础能力。

### 做什么

1. 实现 `AgentUtils.reactAgentBuilder`。
2. 实现 `AgentUtils.callSchema`。
3. 实现 `PromptLoader` / `PromptBuilder`。
4. 实现 `AgentRunContext`。
5. 实现公共 hooks，例如 `AgentLogHook`、`TraceHook`、`ErrorHandlingHook`。
6. 定义 Agent 调用异常和统一错误转换。
7. 准备 Stub `ChatModel` / Fake Agent 测试工具。

### 不做什么

1. 不实现所有业务 Agent。
2. 不接 MCP / A2A / Skills 全量空壳。
3. 不让 Orchestrator 直接调用模型。
4. 不在具体 Agent 模块重复写 `ReactAgent.builder` 初始化逻辑。

### 验收标准

1. 真实 Agent 可以复用 `AgentUtils` 初始化。
2. 所有 Agent 不需要重复写 `ReactAgent.builder` 初始化逻辑。
3. Prompt 加载、结构化调用、异常转换具备可测试封装。
4. 单元测试不调用真实外部模型 API。

### 依赖前置

依赖 Phase 0 的长期文档规范，尤其是 `docs/09_AGENT_BUILD_GUIDE.md`。

## 5. Phase 2：真实 IntentAgent

### 目标

实现真实 `IntentAgent`，将自然语言转为 `NetworkIntent`。

### 做什么

1. 创建 `intent-agent-prompt.md`。
2. 实现 `IntentResponseSchema`。
3. 实现 `IntentExtractTool`。
4. 实现 `IntentResponseParser`。
5. 实现 `IntentOutputValidator`。
6. 实现 `IntentAgent`，注入 `ChatModel`，使用 `AgentUtils.reactAgentBuilder`。
7. `IntentAgent` 输出 `NetworkIntent`。
8. 与 Orchestrator 对接。

### 不做什么

1. 不输出设备、接口、VLAN、IP、拓扑、CLI。
2. 不实现 `PlanningAgent`。
3. 不实现 `ConfigurationAgent`。
4. 不接 Mininet / Ryu。

### 验收标准

1. 输入自然语言可以生成 `NetworkIntent`。
2. `NetworkIntent` 通过 Validator。
3. `IntentAgent` 不越界生成规划或配置。
4. `ResponseSchema -> Parser -> DTO -> Validator -> Workspace` 链路完整。
5. 相关测试通过。

### 依赖前置

依赖 Phase 1 的 Agent Core 基础设施。

## 6. Phase 3：真实 PlanningAgent

### 目标

实现 `PlanningAgent`，将 `NetworkIntent` 转为 `NetworkPlan`。

### 做什么

1. 创建 `planning-agent-prompt.md`。
2. 实现 `PlanningResponseSchema`。
3. 实现地址规划、VLAN 规划、拓扑模板等 `methodTools`。
4. 实现 `PlanningResponseParser`。
5. 实现 `PlanningOutputValidator`。
6. 输出 `NetworkPlan`。
7. 将规划产物写入 `NetworkWorkspace`。

### 不做什么

1. 不生成 CLI 命令。
2. 不执行配置。
3. 不判断执行结果。
4. 不绕过 `IntentAgent` 直接读取用户原始需求做完整方案。

### 验收标准

1. `NetworkPlan` 包含拓扑、区域、地址、VLAN、路由、安全策略、`targetEnvironment`。
2. `NetworkPlan` 不包含 CLI。
3. 规划元素具备可追溯 `id`。
4. 规划结果可被 `ConfigurationAgent` 和 `ExecutionAdapter` 使用。

### 依赖前置

依赖 Phase 2 的 `NetworkIntent` 产物和 Phase 1 的 Agent Core。

## 7. Phase 4：真实 ConfigurationAgent + RAG / Template Tools

### 目标

实现 `ConfigurationAgent`，根据 `NetworkPlan` 生成结构化 `ConfigSet`。

### 做什么

1. 创建 `configuration-agent-prompt.md`。
2. 实现 `ConfigurationResponseSchema`。
3. 实现配置模板工具。
4. 实现命令知识库检索工具。
5. 接入 RAG。
6. 输出 `ConfigSet`。
7. 保留 `commandBlocks`、`traceRefs`、`rollbackCommands`。

### 不做什么

1. 不只返回一整段命令文本。
2. 不直接执行命令。
3. 不绕过 `ExecutionAdapter`。
4. 不让配置生成阶段判断业务意图是否验证通过。

### 验收标准

1. `ConfigSet` 可以按设备和配置块展示。
2. 每个 `commandBlock` 可追溯到 intent / plan。
3. 有回滚命令或不可回滚说明。
4. 配置来源能区分 LLM、RAG、Template、Rule、Tool、MCP、Manual Override。

### 依赖前置

依赖 Phase 3 的 `NetworkPlan`，以及 Phase 1 的 Agent Core。

## 8. Phase 5：ExecutionAdapter + Mininet / Ryu

### 目标

实现受控执行适配，将 `NetworkPlan + ConfigSet` 转换为可执行环境内容。

### 做什么

1. 实现 `ExecutionAdapter`。
2. 实现 `MininetRyuExecutionAdapter`。
3. 生成 Mininet 拓扑脚本。
4. 生成 Ryu 流表或控制器调用。
5. 执行 ping / traceroute / iperf。
6. 输出 `ExecutionReport`。
7. 将执行计划、运行状态和测试结果写入 `NetworkWorkspace`。

### 不做什么

1. 不执行 LLM 拼出来的任意 shell。
2. 不直接执行 Huawei CLI。
3. 不让 Controller 接收任意 shell 命令。
4. 不让 Execution Module 判断业务意图是否达成。

### 验收标准

1. `ExecutionReport` 包含 `executionPlan`、`runtimeState`、`testResult`。
2. 所有执行命令经过白名单或安全校验。
3. 测试结果可被 `VerificationAgent` 使用。
4. 执行失败时能输出结构化错误和可追溯信息。

### 依赖前置

依赖 Phase 3 的 `NetworkPlan` 和 Phase 4 的 `ConfigSet`。

## 9. Phase 6：真实 VerificationAgent

### 目标

实现 `VerificationAgent`，根据 `NetworkIntent` 和 `ExecutionReport` 判断意图是否达成。

### 做什么

1. 创建 `verification-agent-prompt.md`。
2. 实现 `VerificationResponseSchema`。
3. 实现连通性、隔离性、安全策略验证工具。
4. 输出 `ValidationReport`。
5. 失败时生成可用于 Healing 的证据和建议。
6. 将验证报告写入 `NetworkWorkspace`。

### 不做什么

1. 不直接修改配置。
2. 不直接执行修复。
3. 不绕过 `HealingAgent`。
4. 不重新执行测试命令。

### 验收标准

1. `ValidationReport` 包含 `overallStatus`、`items`、`evidences`、`suggestions`。
2. 每个验证项可追溯到 intent / plan / config / test。
3. 验证失败能为 `RepairPlan` 提供依据。
4. 验证逻辑能区分通过、失败、部分通过和未知。

### 依赖前置

依赖 Phase 2 的 `NetworkIntent`、Phase 3 的 `NetworkPlan`、Phase 4 的 `ConfigSet` 和 Phase 5 的 `ExecutionReport`。

## 10. Phase 7：HealingAgent + 自愈闭环

### 目标

实现 `HealingAgent`，根据失败验证结果生成 `RepairPlan`。

### 做什么

1. 创建 `mac-tav-healing-agent`。
2. 创建 `healing-agent-prompt.md`。
3. 实现 `FailureAnalysis`。
4. 实现 `RepairPlan` 和 `RepairAction`。
5. 支持 `REPLAN`、`REGENERATE_CONFIG`、`PATCH_CONFIG`、`REEXECUTE`、`ASK_USER`、`ROLLBACK` 等动作。
6. Orchestrator 根据 `RepairAction` 重新进入对应阶段。
7. 修复动作和新产物写入 `NetworkWorkspace`。

### 不做什么

1. 不让 `HealingAgent` 直接改 Workspace。
2. 不让 `HealingAgent` 直接执行命令。
3. 不绕过 Orchestrator。
4. 不在修复计划中包含立即执行的危险 shell。

### 验收标准

1. 验证失败可以生成 `RepairPlan`。
2. `RepairAction` 能触发重新规划、重新配置、重新执行或用户澄清。
3. 修复后可以再次验证。
4. 修复过程保留 artifact 版本和追溯关系。

### 依赖前置

依赖 Phase 6 的 `ValidationReport` 和完整 `NetworkWorkspace`。

## 11. Phase 8：持久化、异步任务和 SSE

### 目标

增强长期运行能力。

### 做什么

1. MySQL 保存任务、产物、版本和日志。
2. Redis 保存实时进度、短期状态和 SSE 消息。
3. 实现异步任务执行。
4. 实现 SSE 进度推送。
5. 支持多版本 Artifact。
6. 支持阶段重跑和回放。

### 不做什么

1. 不改变核心 DTO 契约。
2. 不破坏 Orchestrator 主流程。
3. 不让持久化层生成网络规划或配置。

### 验收标准

1. 应用重启后任务仍可查询。
2. 前端能实时看到阶段进度。
3. 产物支持版本历史。
4. 阶段重跑、产物替换和旧版本归档有明确状态。

### 依赖前置

可与 Phase 2 到 Phase 7 并行推进，但不得改变核心 DTO 契约和主调用链。

## 12. Phase 9：前端可视化和用户体验增强

### 目标

增强前端对完整闭环的展示能力。

### 做什么

1. 展示 `NetworkIntent`。
2. 展示拓扑。
3. 展示 `ConfigSet` 和 `commandBlocks`。
4. 展示 `ExecutionReport`。
5. 展示 `ValidationReport`。
6. 展示 `RepairPlan`。
7. 展示 Agent 时间线。
8. 展示阶段重跑和修复确认流程。

### 不做什么

1. 不把业务决策逻辑搬到前端。
2. 不让前端直接拼接执行命令。
3. 不让前端展示模型污染核心 DTO。

### 验收标准

1. 用户能看清意图到修复闭环全过程。
2. 配置、验证、修复具有追溯关系。
3. 前端可从 Workspace、Artifact、Timeline 等 API 恢复当前状态。

### 依赖前置

依赖阶段产物 API、Workspace API 和核心 DTO 契约。可与 Phase 8 并行推进。

## 13. Phase 10：A2A / MCP / Skills 增强

### 目标

增强多智能体协作与外部工具调用能力。

### 做什么

1. 按需接入 MCP。
2. 按需接入 A2A。
3. 按需引入 Skills。
4. `RemoteAgentTool` 封装远程 Agent 调用。
5. 支持 Agent 间受控协作。

### 不做什么

1. 不一次性生成所有 MCP / Skill / A2A 空壳。
2. 不破坏现有 Service / DTO 边界。
3. 不让 Agent 直接共享内部状态。
4. 不让 MCP 绕过 Model Core 直接修改 Workspace。

### 验收标准

1. A2A / MCP / Skills 不破坏 Orchestrator 主流程。
2. 状态仍由 `NetworkWorkspace` 维护。
3. 工具调用有日志和错误处理。
4. 远程 Agent 调用仍遵守标准输入输出 DTO 契约。

### 依赖前置

依赖 Phase 1 的 Agent Core。推荐在核心 Agent、Execution、Verification、Healing 稳定后再扩展。

## 14. 阶段依赖关系

阶段依赖关系：

1. Phase 1 是所有真实 Agent 的前置。
2. `IntentAgent` 是后续 `PlanningAgent` 的前置。
3. `PlanningAgent` 是 `ConfigurationAgent` 和 `ExecutionAdapter` 的前置。
4. `ConfigurationAgent` 和 `ExecutionAdapter` 是 `VerificationAgent` 的前置。
5. `VerificationAgent` 是 `HealingAgent` 的前置。
6. 持久化、SSE、前端增强可以和 Agent 能力并行推进，但不能改变核心 DTO 契约。
7. A2A / MCP / Skills 是增强能力，不应早于核心 Agent 链路稳定。

推荐主线顺序：

```text
Phase 0 -> Phase 1 -> Phase 2 -> Phase 3 -> Phase 4 -> Phase 5 -> Phase 6 -> Phase 7
```

增强能力可按需并行：

```text
Phase 8 / Phase 9 / Phase 10
```

## 15. 本文档与其他文档的分工

文档分工：

1. 本文档：长期实现阶段、阶段目标、验收标准。
2. `docs/02_MAVEN_MODULES.md`：Maven 模块和依赖边界。
3. `docs/03_MODULE_DESIGN.md`：业务模块职责和流程。
4. `docs/04_DATA_MODELS.md`：核心 DTO 字段。
5. `docs/05_API_DESIGN.md`：HTTP API 契约。
6. `docs/07_TEST_DATA_AND_SCENARIOS.md`：测试场景和样例数据。
7. `docs/08_RUN_AND_TEST.md`：运行、测试、手动验证。
8. `docs/09_AGENT_BUILD_GUIDE.md`：真实 Spring AI Alibaba Agent 构建规范。
