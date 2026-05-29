# AGENTS.md

## 1. 项目定位

MAC-TAV 是“基于多智能体协同的网络意图翻译与闭环验证系统”。

长期目标是通过 Spring AI Alibaba Agent、A2A、Nacos、受控工具、ExecutionAdapter 和 NetworkWorkspace，完成：

```text  
意图 -> 规划 -> 配置 -> 执行 -> 验证 -> 诊断/修复  
```  

本项目不是简单配置生成器。它必须保留全过程状态、阶段产物、追溯关系、验证证据和修复闭环。

长期最终架构应由 Orchestrator 作为唯一主编排入口，通过 RemoteAgentTool / A2A Client 发现并调用远程专业 Agent。专业 Agent 独立提供阶段能力，Orchestrator 负责确定性流程控制和状态闭环。

## 2. Codex 开发前上下文读取规则

Codex 开发本项目时，MUST 先理解任务范围，再按需读取最小必要上下文。

默认禁止在每个任务开始前全量读取 `docs/00-09`。  Codex SHOULD 优先读取索引、当前状态和当前任务直接相关文档，避免重复加载无关长文档。

### 2.1 默认必须先读

任何开发任务前，MUST 优先读取：

1. `AGENTS.md`
2. `docs/CODEX_CURRENT_STATE.md`，了解当前阶段、已完成能力、最近改动和下一步边界。
3. `docs/CODEX_DOC_INDEX.md`，根据任务类型选择需要读取的专项文档。

如果以上两个 Codex 辅助文档尚不存在，Codex SHOULD 提醒创建；在创建前才临时读取与任务最相关的专项文档。

### 2.2 按任务类型读取专项文档

Codex MUST 根据当前任务类型读取对应文档，不得默认全量读取所有文档。

| 任务类型 | 必读文档 |  
| --- | --- |  
| 项目定位、边界、长期目标调整 | `docs/00_PROJECT_BRIEF.md`、`docs/01_SCOPE_AND_BOUNDARIES.md` |  
| Maven 模块、依赖、包名、启动边界 | `docs/02_MAVEN_MODULES.md` |  
| 业务模块职责、上下游关系、阶段边界 | `docs/03_MODULE_DESIGN.md` |  
| DTO、枚举、字段、TraceRefs、Workspace、Artifact | `docs/04_DATA_MODELS.md` |  
| HTTP API、Controller、统一响应、错误码 | `docs/05_API_DESIGN.md` |  
| 阶段计划、Phase 顺序、验收标准 | `docs/06_DEV_PLAN.md` |  
| 测试场景、样例数据、失败用例、Validator 非法输出 | `docs/07_TEST_DATA_AND_SCENARIOS.md` |  
| 运行、测试、启动、环境变量、手动验证 | `docs/08_RUN_AND_TEST.md` |  
| Spring AI Alibaba Agent、Prompt、Tool、MCP、Skills、A2A、Parser、Validator | `docs/09_AGENT_BUILD_GUIDE.md` |  

### 2.3 代码修改前的读取规则

代码修改任务开始前，Codex MUST：

1. 先使用 `git diff --name-only`、`git status` 或用户指定范围确认本轮改动范围。
2. 优先阅读本轮涉及的文件。
3. 只追踪这些文件直接依赖或直接引用的类。
4. 只读取与当前任务相关的专项文档。
5. 不得为了“保险”而全仓扫描或全量阅读 `docs/00-09`。

如果任务只涉及单个模块，Codex SHOULD 优先读取该模块内的代码和该模块对应的专项文档。

### 2.4 Agent 相关任务读取规则

如果任务涉及真实 Agent 实现、Prompt、Tool、MCP、Skills、A2A、Agent Card、Parser、Validator 或结构化输出，Codex MUST 阅读：

1. `docs/09_AGENT_BUILD_GUIDE.md`
2. 与当前 Agent 输入 / 输出 DTO 相关的 `docs/04_DATA_MODELS.md` 小节
3. 与当前模块边界相关的 `docs/02_MAVEN_MODULES.md` 小节
4. 必要时阅读 `docs/03_MODULE_DESIGN.md` 中对应模块小节

Codex MUST NOT 因为实现某一个 Agent，就一次性创建所有 Agent、MCP、Skill、A2A 空壳。

### 2.5 API 相关任务读取规则

如果任务涉及 Controller、接口路径、统一响应、错误码、SSE、前端调用契约，Codex MUST 阅读：

1. `docs/05_API_DESIGN.md`
2. 涉及 DTO 时阅读 `docs/04_DATA_MODELS.md` 对应小节
3. 涉及流程推进时阅读 `docs/03_MODULE_DESIGN.md` 中 Orchestrator / Web 相关小节

Controller MUST 只调用 Orchestrator 或查询服务，不得直接调用具体 Agent、ChatModel、ReactAgent 或外部命令。

### 2.6 测试与运行相关任务读取规则

如果任务涉及测试、样例数据、失败用例、Validator 回归，Codex MUST 阅读：

1. `docs/07_TEST_DATA_AND_SCENARIOS.md`
2. 必要时阅读 `docs/04_DATA_MODELS.md` 对应 DTO 小节

如果任务涉及运行、启动、部署、环境变量、手动验证，Codex MUST 阅读：

1. `docs/08_RUN_AND_TEST.md`

Codex 不得在自动化测试中调用真实外部模型 API。  Codex 不得在 Codex 环境中长期挂起 `spring-boot:run`、`npm run dev` 或其他前台常驻命令。

### 2.7 审查任务读取规则

如果用户要求审查代码，Codex MUST 先判断审查范围：

- 如果用户要求“审查本次改动”，只审查 `git diff` 涉及文件及其直接依赖。
- 如果用户要求“审查某个模块”，只审查该模块及其直接依赖模块。
- 如果用户明确要求“全项目架构审查”，才允许扩大到全局文档和跨模块扫描。

Codex 审查输出 SHOULD 包含：

1. 发现的问题。
2. 影响范围。
3. 违反了哪条边界或契约。
4. 建议修改文件。
5. 建议运行的最小测试命令。

### 2.8 权威文档优先级

当文档之间出现冲突时，按以下优先级判断：

1. `AGENTS.md`：项目级硬规则和 Codex 行为约束。
2. `docs/CODEX_CURRENT_STATE.md`：当前阶段状态和最近实际代码情况。
3. 具体专项文档：
   - Agent 实现以 `docs/09_AGENT_BUILD_GUIDE.md` 为准。
   - Maven 模块和依赖边界以 `docs/02_MAVEN_MODULES.md` 为准。
   - DTO 字段以 `docs/04_DATA_MODELS.md` 为准。
   - API 路径以 `docs/05_API_DESIGN.md` 为准。
   - 测试样例以 `docs/07_TEST_DATA_AND_SCENARIOS.md` 为准。
   - 运行测试命令以 `docs/08_RUN_AND_TEST.md` 为准。
4. 旧代码只能作为现状参考，不得覆盖长期架构规则。

如果当前代码与长期文档不一致，Codex MUST 在输出中明确说明这是：
- 当前过渡实现；
- 架构债；
- 还是本轮需要修复的问题。

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
- `mac-tav-web` 只负责 Web / API / SSE / 前端交互入口，不作为 Agent 聚合中心，不扫描或直接装配具体 Agent Bean。
- `mac-tav-web` 通过 `mac-tav-orchestrator` 触发工作流；Orchestrator 通过 RemoteAgentTool / A2A Client 调用注册在 Nacos 中的专业 Agent。
- Controller 只放在 `mac-tav-web`；Agent 服务可暴露 A2A 协议入口，但不得放面向业务 HTTP API 的 Controller。
- Agent 模块不写业务 Controller。
- 长期标准 A2A 多 Agent 服务化架构下，专业 Agent 模块可以拥有自己的 Spring Boot 启动类、`application.yml`、A2A 配置、Nacos 注册配置和 Agent Card 配置。
- Agent 模块不得依赖 `mac-tav-web`、`mac-tav-orchestrator` 或其他具体 Agent 模块。
- Model Core 不依赖任何 Agent 模块。
- 禁止 Maven 循环依赖。

## 4. 长期标准架构

MAC-TAV 的唯一主线架构是最终 A2A 多 Agent 服务化架构。

Orchestrator 是唯一主编排入口，负责确定性工程流程控制、任务状态推进、Workspace 写入、Artifact 版本管理、异常收敛、阶段重跑和修复闭环。

专业 Agent 作为独立 Spring Boot 服务启动，注册到 Nacos，发布 Agent Card，并通过 A2A 被 Orchestrator 调用。

MUST 遵守：

- `mac-tav-web` 只负责 Web / API / SSE / 前端交互入口。
- `mac-tav-web` 不扫描、不聚合、不直接调用具体 Agent Bean。
- Orchestrator 不构造 Prompt，不直接调用 `ChatModel` / `ChatClient` / `ReactAgent`。
- Orchestrator 通过 RemoteAgentTool / A2A Client 调用远程专业 Agent。
- RemoteAgentTool / A2A Client 默认放在 `mac-tav-orchestrator` 中，只负责 Nacos 查询、Agent Card 解析、A2A 调用、远程异常处理和协议适配。
- RemoteAgentTool / A2A Client 不承担业务编排职责，不写 Workspace，不管理任务状态。
- 专业 Agent 只负责自己的阶段能力，返回已解析、已校验的阶段 DTO 或标准失败结果。
- 专业 Agent 不直接修改 NetworkWorkspace，不推进任务状态，不管理 Artifact 版本。
- 专业 Agent 必须执行 `ResponseSchema -> Parser -> DTO -> Validator`。
- Model Core 负责 Workspace、任务状态、版本、日志和追溯关系等工程状态管理。
- 专业 Agent 模块 MUST 通过 `XxxAgentConfiguration`（`@Configuration`）注册 `ReactAgent` Bean 和 `XxxAgent` Bean，不得在 `XxxAgent` 构造器内部私有化创建 `ReactAgent`（详见 `docs/09_AGENT_BUILD_GUIDE.md` §7）。
- `ReactAgent` Bean 命名 SHOULD 遵循 `{lowercaseAgentName}ReactAgent`（例如 `intentReactAgent`），以便 SAA A2A starter 自动装配与 Nacos 注册（参考官方文档：<https://java2ai.com/docs/frameworks/agent-framework/advanced/a2a>）。

## 5. 核心调用链

长期标准调用链 MUST 保持：

```text  
Controller / API  
  -> Orchestrator  -> RemoteAgentTool / A2A Client  -> Nacos Agent Discovery  -> Agent Card  -> 专业 Agent A2A Service  -> XxxAgent  -> Spring AI Alibaba Agent / Tools / MCP / Skills  -> ResponseSchema  -> Parser  -> DTO  -> Validator  -> Orchestrator  -> Model Core / NetworkWorkspace / Artifact  
```  

禁止事项：

- Controller 不直接调用 `ChatModel` / `ChatClient` / `ReactAgent`。
- Controller 不构造 Prompt。
- Controller 不直接调用具体 Agent。
- Orchestrator 不构造 Prompt。
- Orchestrator 不直接调用大模型。
- Orchestrator 不通过 Maven 直接依赖具体 Agent 实现类。
- DTO 不依赖 Spring AI Alibaba 类型。
- Model Core 不调用大模型。
- Tool 不直接写 `NetworkWorkspace`。
- Agent 模块不绕过 `ResponseSchema -> Parser -> DTO -> Validator`。
- Agent 模块不直接承担任务状态推进、版本追溯、闭环控制等 Orchestrator / Model Core 职责。
- Execution Module 不执行 LLM 拼出来的任意 shell 命令。

## 6. Spring AI Alibaba Agent 总规则

所有真实 Agent MUST 遵守 `docs/09_AGENT_BUILD_GUIDE.md`，并采用两层结构（详见 `docs/09_AGENT_BUILD_GUIDE.md` §7）：

1. `XxxAgentConfiguration`（`@Configuration`）中通过 `@Bean` 方法调用 `AgentUtils.reactAgentBuilder(...)` 注册 `ReactAgent` Bean，并构造 `XxxAgent` Bean。
2. `XxxAgent` 注入 `ReactAgent` Bean，只做项目业务封装，MUST NOT 在构造器内部私有化创建 `ReactAgent`。

每个真实 Agent MUST：

- 在自己的 Maven 模块中实现。
- 在 `XxxAgentConfiguration` 中注入 `ChatModel` 并通过 `AgentUtils.reactAgentBuilder(...)` 创建 `ReactAgent` Bean。
- `XxxAgent` 注入已装配好的 `ReactAgent` Bean，MUST NOT 重复调用 `ReactAgent.builder()` 或 `AgentUtils.reactAgentBuilder(...)`。
- 从 `src/main/resources/prompts/{agent}-prompt.md` 加载系统提示词。
- 只注册自己需要的 `methodTools`。
- 显式配置 hooks。
- 显式声明 `outputType(XxxResponseSchema.class)`。
- 执行 `ResponseSchema -> Parser -> DTO -> Validator`。
- 返回项目 DTO，而不是模型原始字符串。
- `ReactAgent` Bean 命名遵循 `{lowercaseAgentName}ReactAgent`，以便 SAA A2A starter / Agent Card / Nacos 自动装配。

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
- A2A / Nacos / Agent Card MUST 优先使用 Spring AI Alibaba 官方 starter（`spring-ai-alibaba-starter-a2a-nacos`）+ `application.yml` 配置 + 命名 `ReactAgent` Bean 自动装配（参考：<https://java2ai.com/docs/frameworks/agent-framework/advanced/a2a>）。
- 除非官方 starter 版本确实缺少必要能力，否则不得手写 Agent Card 发布、Nacos 注册、A2A HTTP Controller 或 HTTP fallback。
- MUST NOT 为了"保险"同时实现官方 A2A 调用链和 legacy HTTP JSON 调用链两套主链路；legacy fallback 如历史遗留存在，只能标注为过渡架构债，不得作为新 Agent 模板复制。
- RemoteAgentTool / A2A Client 可以封装远程 Agent 调用，但只作为 Orchestrator 使用的调用工具或客户端，不承担业务编排职责，不写 Workspace，不管理任务状态；状态仍通过 NetworkWorkspace 管理。
- 本文档中的 RemoteAgentTool / A2A Client 指 Orchestrator 侧远程 Agent 调用客户端；代码实现时可根据 Spring AI Alibaba A2A 能力选择 RemoteAgentTool 或 A2A Client，不同时强制实现两套。

## 8. Model Core 与数据模型总规则

- 共享 DTO 和领域模型枚举放在 `mac-tav-model`。
- 公共异常、统一响应、通用错误码、通用工具和常量放在 `mac-tav-common`。
- `NetworkIntent`、`NetworkPlan`、`ConfigSet`、`ExecutionReport`、`ValidationReport`、`RepairPlan` 是核心阶段产物。
- 所有阶段产物 MUST 写入 `NetworkWorkspace`。
- Model Core 只负责任务状态、版本、阶段产物、执行日志和追溯关系。
- Model Core 不生成 `NetworkPlan`。
- Model Core 不生成 `ConfigSet`。
- Model Core 不执行仿真。

## 9. Execution Module 总规则

- `mac-tav-execution` 以 `ExecutionAdapter` 为核心。
- Execute Module 不是纯 LLM Agent。
- ExecutionAdapter 长期主验收面向 Mininet / Ryu 或其他真实受控执行适配器；如 Mininet / Ryu 暂不可用，可提供结构校验模式验证 `NetworkPlan + ConfigSet -> ExecutionReport` 的转换链路，但不得作为最终执行验收替代。
- Execute Module 负责把 `NetworkPlan + ConfigSet` 转换为 Mininet / Ryu / Docker / 自定义适配器可执行内容。
- 不允许直接执行 Huawei CLI。
- 不允许执行 LLM 拼出来的任意 shell。
- Mininet、Ryu、Docker、Shell 调用 MUST 通过 Tool / Adapter 白名单封装。

## 10. 安全规则

- API Key 不允许硬编码。优先使用 Spring Boot / Spring AI Alibaba 官方配置体系读取模型 Key，例如 application.yml 占位符 + 本地私有环境变量。不得为每个 Agent 单独新增 ApiKeyResolver / KeyResolver，除非官方配置体系确实无法满足。
- DashScope / OpenAI Compatible / 其他模型 Key MUST 从环境变量或本地私有配置读取。
- 不要提交真实密钥。
- 不要在日志中打印完整 API Key、请求头、外部凭据。
- 外部命令执行 MUST 白名单化。
- Web 响应不返回完整异常堆栈。
- 测试不调用真实外部模型 API。
- 如果有需要的外部环境需求如 nacos 等，请告知我，不要因为某些环境就阻塞真实代码的开发。

## 11. 构建与测试要求

- 在每个生成的类的上面添加简短 JavaDoc，标识这个类的作用是用来干什么的。
- 修改后至少运行 `mvn compile`。
- 涉及测试时运行 `mvn test` 或指定模块测试。
- 如果 Windows 文件锁导致 `mvn clean` 失败，可以说明原因，不要反复 clean。
- 不要在 Codex 中裸跑长期占用前台的 `spring-boot:run` 或 `npm run dev`。
- 自动化测试不调用真实外部模型 API；Parser / Validator 可使用固定样例 JSON；Tool / MCP / A2A 异常分支可使用测试夹具；不得用 Stub ChatModel、Fake ReactAgent、Mock Tool、Mock Agent 或测试 Agent Bean 替代真实业务主链路。
- SchemaAgentInvoker 只允许存在于 agent-core 的离线测试边界、Parser / Validator 测试或测试夹具中。具体业务 Agent 的生产构造器不得同时支持 ReactAgent 和 SchemaAgentInvoker 双路径。

## 12. 完成任务后必须汇报

每次完成后 MUST 说明：

- 修改了哪些文件。
- 新增了哪些类或文档。
- 如何运行。
- 如何测试。
- 测试结果。
- 当前还有哪些 TODO。
- 是否有文档与代码不一致的地方。