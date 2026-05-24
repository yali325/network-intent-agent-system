# AGENTS.md

## 1. 项目定位

MAC-TAV 是“基于多智能体协同的网络意图翻译与闭环验证系统”。

长期目标是通过 Spring AI Alibaba Agent、A2A、Nacos、受控工具、ExecutionAdapter 和 NetworkWorkspace，完成：

```text
意图 -> 规划 -> 配置 -> 执行 -> 验证 -> 诊断/修复
```

本项目不是简单配置生成器。它必须保留全过程状态、阶段产物、追溯关系、验证证据和修复闭环。

长期最终架构应由 Orchestrator 作为唯一主编排入口，通过 RemoteAgentTool / A2A Client 发现并调用远程专业 Agent。专业 Agent 独立提供阶段能力，Orchestrator 负责确定性流程控制和状态闭环。

## 2. Codex 开发前必须阅读

任何开发任务前 MUST 阅读：

1. `docs/00_PROJECT_BRIEF.md`
2. `docs/01_SCOPE_AND_BOUNDARIES.md`
3. `docs/02_MAVEN_MODULES.md`
4. `docs/03_MODULE_DESIGN.md`
5. `docs/04_DATA_MODELS.md`
6. `docs/06_DEV_PLAN.md`
7. `docs/09_AGENT_BUILD_GUIDE.md`

如果任务涉及 HTTP 接口，MUST 额外阅读：

- `docs/05_API_DESIGN.md`

如果任务涉及测试场景、样例数据、失败用例，MUST 额外阅读：

- `docs/07_TEST_DATA_AND_SCENARIOS.md`

如果任务涉及运行、测试、部署、手动验证，MUST 额外阅读：

- `docs/08_RUN_AND_TEST.md`

Agent 实现细节以 `docs/09_AGENT_BUILD_GUIDE.md` 为准。Maven 模块和依赖边界以 `docs/02_MAVEN_MODULES.md` 为准。DTO 字段以 `docs/04_DATA_MODELS.md` 为准。API 路径以 `docs/05_API_DESIGN.md` 为准。

## 3. Maven 与包名总规则

长期标准模块：

1. `mac-tav-common`
2. `mac-tav-model`
3. `mac-tav-agent-core`
4. `mac-tav-model-core`
5. `mac-tav-intent-agent`
6. `mac-tav-planning-agent`
7. `mac-tav-configuration-agent`
8. `mac-tav-execution`
9. `mac-tav-verification-agent`
10. `mac-tav-healing-agent`
11. `mac-tav-orchestrator`
12. `mac-tav-web`

MUST 遵守：

- 包名统一使用 `com.yali.mactav`。
- Agent Core 根包统一为 `com.yali.mactav.agent.core`。
- `mac-tav-agent-core` 只保留通用 Agent 初始化、Prompt、Hook、Tool/MCP/Skill/A2A 抽象，不放 Orchestrator 侧 `RemoteAgentTool / A2A Client` 实现。
- `mac-tav-web` 是 Web/API 入口，不是长期 Agent 聚合中心；当前过渡开发阶段可以临时作为本地聚合启动入口。
- Controller 只放在 `mac-tav-web`；Agent 服务可暴露 A2A 协议入口，但不得放面向业务 HTTP API 的 Controller。
- Agent 模块不写业务 Controller。
- 长期标准 A2A 多 Agent 服务化架构下，专业 Agent 模块可以拥有自己的 Spring Boot 启动类、`application.yml`、A2A 配置、Nacos 注册配置和 Agent Card 配置；这些启动配置不视为污染 Agent 模块。
- Agent 模块不得依赖 `mac-tav-web`、`mac-tav-orchestrator` 或其他具体 Agent 模块。
- Model Core 不依赖任何 Agent 模块。
- 禁止 Maven 循环依赖。

## 4. 长期标准架构与当前过渡开发方式

### 4.1 长期标准架构

MAC-TAV 的长期标准架构只有最终 A2A 多 Agent 服务化架构。Orchestrator 是唯一主编排入口，负责确定性工程流程控制；专业 Agent 作为独立 Spring Boot 服务启动，注册到 Nacos，发布 Agent Card，并通过 A2A 被 Orchestrator 调用。

长期标准特征：

- Orchestrator 是唯一主编排入口，负责任务状态推进、Workspace 写入、Artifact 版本管理、异常收敛、阶段重跑和修复闭环。
- Orchestrator 负责决定当前阶段应该调用哪个专业 Agent。
- Orchestrator 负责传递阶段输入和 Workspace 摘要。
- Orchestrator 负责接收专业 Agent 返回的阶段 DTO 或标准失败结果。
- Orchestrator 不构造 Prompt，不直接调用 `ChatModel` / `ChatClient` / `ReactAgent`。
- Orchestrator 通过 RemoteAgentTool / A2A Client 调用远程专业 Agent。
- RemoteAgentTool / A2A Client 默认放在 `mac-tav-orchestrator` 中，作为 Orchestrator 调用远程专业 Agent 的客户端适配能力。
- RemoteAgentTool / A2A Client 负责 Nacos 查询、Agent Card 解析、A2A 协议调用、远程异常处理和协议适配。
- RemoteAgentTool / A2A Client 不承担业务编排职责，不写 Workspace，不管理任务状态。
- 专业 Agent 模块可以拥有启动类、`application.yml`、A2A / Nacos / Agent Card 配置。
- 专业 Agent 只负责自己的阶段能力，返回已解析、已校验的阶段 DTO 或标准失败结果。
- 专业 Agent 不直接修改 NetworkWorkspace，不推进任务状态，不管理 Artifact 版本。
- 专业 Agent 必须执行 `ResponseSchema -> Parser -> DTO -> Validator`。
- Model Core 负责 Workspace、任务状态、版本、日志和追溯关系等工程状态管理。

### 4.2 当前过渡开发方式

当前开发阶段，为了降低联调成本，`mac-tav-web` 可以临时作为本地聚合启动入口。这只是过渡开发方式，不是长期架构目标。

过渡说明：

- Controller 仍只放在 `mac-tav-web`。
- `mac-tav-web` 可以临时扫描本地 Agent Bean。
- Orchestrator 可以临时本地调用各 `XxxService` / `XxxAgent`。
- 这种方式只用于单进程联调、早期验证和无 Nacos / A2A 环境下的开发调试。
- 后续落地应逐步迁移到 `Orchestrator -> RemoteAgentTool / A2A Client -> Nacos -> Agent Card -> 专业 Agent A2A Service` 的标准链路。

## 5. 核心调用链

长期标准调用链 MUST 保持：

```text
Controller / API
  -> Orchestrator
  -> RemoteAgentTool / A2A Client
  -> Nacos Agent Discovery
  -> Agent Card
  -> 专业 Agent A2A Service
  -> XxxAgent
  -> Spring AI Alibaba Agent / Tools / MCP / Skills
  -> ResponseSchema
  -> Parser
  -> DTO
  -> Validator
  -> Orchestrator
  -> Model Core / NetworkWorkspace / Artifact
```

当前过渡开发方式下，可以临时使用本地聚合调用链：

```text
Controller
  -> TaskOrchestratorService
  -> XxxService
  -> XxxAgent
  -> Spring AI Alibaba Agent / Tools / MCP / Skills
  -> ResponseSchema
  -> Parser
  -> DTO
  -> Validator
  -> Orchestrator / Model Core 写入 NetworkWorkspace
```

该链路只用于早期联调和无 Nacos / A2A 环境下的开发调试，不作为长期验收标准。

禁止事项：

- Controller 不直接调用 `ChatModel` / `ChatClient` / `ReactAgent`。
- Controller 不构造 Prompt。
- Orchestrator 不构造 Prompt。
- Orchestrator 不直接调用大模型。
- Orchestrator 可以决定当前阶段调用哪个专业 Agent，但远程协议细节通过 RemoteAgentTool / A2A Client 封装。
- DTO 不依赖 Spring AI Alibaba 类型。
- Model Core 不调用大模型。
- Tool 不直接写 `NetworkWorkspace`。
- Agent 模块不绕过 `ResponseSchema -> Parser -> DTO -> Validator`。
- Agent 模块不直接承担任务状态推进、版本追溯、闭环控制等 Orchestrator / Model Core 职责。
- Execution Module 不执行 LLM 拼出来的任意 shell 命令。

## 6. Spring AI Alibaba Agent 总规则

所有真实 Agent MUST 遵守 `docs/09_AGENT_BUILD_GUIDE.md`。

每个真实 Agent MUST：

- 在自己的 Maven 模块中实现。
- 注入 `ChatModel`。
- 通过 `AgentUtils.reactAgentBuilder(...)` 初始化。
- 从 `src/main/resources/prompts/{agent}-prompt.md` 加载系统提示词。
- 只注册自己需要的 `methodTools`。
- 显式配置 hooks。
- 显式声明 `outputType(XxxResponseSchema.class)`。
- 执行 `ResponseSchema -> Parser -> DTO -> Validator`。
- 返回项目 DTO，而不是模型原始字符串。

参考项目统一为：

- `https://github.com/yali325/trip-plan-agent-system.git`

该项目只用于参考 Agent 构建模式、`AgentUtils`、`methodTools`、hooks、`outputType` 等工程方式。不得复制旅行规划业务类、Prompt、DTO 或工具。

## 7. Tool / MCP / A2A / Skills 总规则

- Tool MUST 是 Spring Bean。
- Tool 参数和返回值 MUST 结构化。
- Tool 不得绕过当前阶段职责。
- MCP、Skills 是增强能力；A2A 是最终多 Agent 服务模式下的远程协作通道，三者都不得破坏 Service、DTO、Workspace 边界。
- 除非任务明确要求，不要一次性生成大量 MCP / Skill / A2A 空壳。
- A2A 是最终多 Agent 服务模式下的远程协作通道；Orchestrator 仍是确定性工程流程主控。
- RemoteAgentTool / A2A Client 可以封装远程 Agent 调用，但只作为 Orchestrator 使用的调用工具或客户端，不承担业务编排职责，不写 Workspace，不管理任务状态；状态仍通过 NetworkWorkspace 管理。
- 本文档中的 RemoteAgentTool / A2A Client 指 Orchestrator 侧远程 Agent 调用客户端；代码实现时可根据 Spring AI Alibaba A2A 能力选择 RemoteAgentTool 或 A2A Client，不同时强制实现两套。

## 8. Model Core 与数据模型总规则

- 共享 DTO 放在 `mac-tav-model`。
- 公共枚举、异常、统一响应放在 `mac-tav-common`。
- `NetworkIntent`、`NetworkPlan`、`ConfigSet`、`ExecutionReport`、`ValidationReport`、`RepairPlan` 是核心阶段产物。
- 所有阶段产物 MUST 写入 `NetworkWorkspace`。
- Model Core 只负责任务状态、版本、阶段产物、执行日志和追溯关系。
- Model Core 不生成 `NetworkPlan`。
- Model Core 不生成 `ConfigSet`。
- Model Core 不执行仿真。

## 9. Execution Module 总规则

- `mac-tav-execution` 以 `ExecutionAdapter` 为核心。
- Execute Module 不是纯 LLM Agent。
- Execute Module 负责把 `NetworkPlan + ConfigSet` 转换为 Mininet / Ryu / Docker / DryRun / 自定义适配器可执行内容。
- 不允许直接执行 Huawei CLI。
- 不允许执行 LLM 拼出来的任意 shell。
- Mininet、Ryu、Docker、Shell 调用 MUST 通过 Tool / Adapter 白名单封装。

## 10. 安全规则

- API Key 不允许硬编码。
- DashScope / OpenAI Compatible / 其他模型 Key MUST 从环境变量或本地私有配置读取。
- 不要提交真实密钥。
- 不要在日志中打印完整 API Key、请求头、外部凭据。
- 外部命令执行 MUST 白名单化。
- Web 响应不返回完整异常堆栈。
- 测试不调用真实外部模型 API。

## 11. 构建与测试要求

- 修改后至少运行 `mvn compile`。
- 涉及测试时运行 `mvn test` 或指定模块测试。
- 如果 Windows 文件锁导致 `mvn clean` 失败，可以说明原因，不要反复 clean。
- 不要在 Codex 中裸跑长期占用前台的 `spring-boot:run` 或 `npm run dev`。
- Agent 单元测试 MUST 使用 Stub ChatModel / Fake ReactAgent / Mock Tool，不调用真实 API。

## 12. 完成任务后必须汇报

每次完成后 MUST 说明：

- 修改了哪些文件。
- 新增了哪些类或文档。
- 是否遵守 `docs/09_AGENT_BUILD_GUIDE.md`。
- 如何运行。
- 如何测试。
- 测试结果。
- 当前还有哪些 TODO。
- 是否有文档与代码不一致的地方。
