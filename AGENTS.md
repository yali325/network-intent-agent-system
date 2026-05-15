# AGENTS.md

## 1. 项目定位

MAC-TAV 是“基于多智能体协同的网络意图翻译与闭环验证系统”。

长期目标是通过 Spring AI Alibaba Agent、受控工具、ExecutionAdapter 和 NetworkWorkspace，完成：

```text
意图 -> 规划 -> 配置 -> 执行 -> 验证 -> 诊断/修复
```

本项目不是简单配置生成器。它必须保留全过程状态、阶段产物、追溯关系、验证证据和修复闭环。

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
- `mac-tav-web` 是当前唯一 Spring Boot Web 启动模块。
- Controller 只放在 `mac-tav-web`。
- Agent 模块不写 Controller。
- Agent 模块不得依赖 `mac-tav-orchestrator` 或 `mac-tav-web`。
- Model Core 不依赖任何 Agent 模块。
- 禁止 Maven 循环依赖。

## 4. 核心调用链

长期主调用链 MUST 保持：

```text
Controller
  -> TaskOrchestratorService
  -> XxxService
  -> XxxAgent
  -> Spring AI Alibaba Agent / Tools / MCP / Skills / A2A
  -> ResponseSchema
  -> Parser
  -> DTO
  -> Validator
  -> Model Core / NetworkWorkspace
```

禁止事项：

- Controller 不直接调用 `ChatModel` / `ChatClient` / `ReactAgent`。
- Controller 不构造 Prompt。
- Orchestrator 不构造 Prompt。
- Orchestrator 不直接调用大模型。
- DTO 不依赖 Spring AI Alibaba 类型。
- Model Core 不调用大模型。
- Tool 不直接写 `NetworkWorkspace`。
- Execution Module 不执行 LLM 拼出来的任意 shell 命令。

## 5. Spring AI Alibaba Agent 总规则

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

## 6. Tool / MCP / A2A / Skills 总规则

- Tool MUST 是 Spring Bean。
- Tool 参数和返回值 MUST 结构化。
- Tool 不得绕过当前阶段职责。
- MCP、A2A、Skills 是增强能力，不得破坏 Service、DTO、Workspace 边界。
- 除非任务明确要求，不要一次性生成大量 MCP / Skill / A2A 空壳。
- A2A 只作为 Agent 间通信增强，Orchestrator 仍是主编排入口。
- RemoteAgentTool 可以封装远程 Agent 调用，但状态仍通过 NetworkWorkspace 管理。

## 7. Model Core 与数据模型总规则

- 共享 DTO 放在 `mac-tav-model`。
- 公共枚举、异常、统一响应放在 `mac-tav-common`。
- `NetworkIntent`、`NetworkPlan`、`ConfigSet`、`ExecutionReport`、`ValidationReport`、`RepairPlan` 是核心阶段产物。
- 所有阶段产物 MUST 写入 `NetworkWorkspace`。
- Model Core 只负责任务状态、版本、阶段产物、执行日志和追溯关系。
- Model Core 不生成 `NetworkPlan`。
- Model Core 不生成 `ConfigSet`。
- Model Core 不执行仿真。

## 8. Execution Module 总规则

- `mac-tav-execution` 以 `ExecutionAdapter` 为核心。
- Execute Module 不是纯 LLM Agent。
- Execute Module 负责把 `NetworkPlan + ConfigSet` 转换为 Mininet / Ryu / Docker / DryRun / 自定义适配器可执行内容。
- 不允许直接执行 Huawei CLI。
- 不允许执行 LLM 拼出来的任意 shell。
- Mininet、Ryu、Docker、Shell 调用 MUST 通过 Tool / Adapter 白名单封装。

## 9. 安全规则

- API Key 不允许硬编码。
- DashScope / OpenAI Compatible / 其他模型 Key MUST 从环境变量或本地私有配置读取。
- 不要提交真实密钥。
- 不要在日志中打印完整 API Key、请求头、外部凭据。
- 外部命令执行 MUST 白名单化。
- Web 响应不返回完整异常堆栈。
- 测试不调用真实外部模型 API。

## 10. 构建与测试要求

- 修改后至少运行 `mvn compile`。
- 涉及测试时运行 `mvn test` 或指定模块测试。
- 如果 Windows 文件锁导致 `mvn clean` 失败，可以说明原因，不要反复 clean。
- 不要在 Codex 中裸跑长期占用前台的 `spring-boot:run` 或 `npm run dev`。
- Agent 单元测试 MUST 使用 Stub ChatModel / Fake ReactAgent / Mock Tool，不调用真实 API。

## 11. 完成任务后必须汇报

每次完成后 MUST 说明：

- 修改了哪些文件。
- 新增了哪些类或文档。
- 是否遵守 `docs/09_AGENT_BUILD_GUIDE.md`。
- 如何运行。
- 如何测试。
- 测试结果。
- 当前还有哪些 TODO。
- 是否有文档与代码不一致的地方。
