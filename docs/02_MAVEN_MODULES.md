# Maven 模块与依赖规划

## 1. 文档目标

本文档定义 MAC-TAV 长期 Maven 多模块结构。

本文档只关注：

1. 模块划分。
2. 模块职责。
3. Maven 依赖方向。
4. 包名约定。
5. Spring Boot 启动与服务化规则。
6. `pom.xml` 依赖放置边界。

具体 Agent 初始化、Prompt、Tool、MCP、Skills、A2A 规范见 `docs/09_AGENT_BUILD_GUIDE.md`。

具体业务模块职责见 `docs/03_MODULE_DESIGN.md`。

具体 DTO 字段见 `docs/04_DATA_MODELS.md`。

---

## 2. 长期模块总览

长期标准模块结构如下：

```text
network-intent-agent-system
├── mac-tav-common
├── mac-tav-model
├── mac-tav-agent-core
├── mac-tav-model-core
├── mac-tav-intent-agent
├── mac-tav-planning-agent
├── mac-tav-configuration-agent
├── mac-tav-execution
├── mac-tav-verification-agent
├── mac-tav-healing-agent
├── mac-tav-orchestrator
└── mac-tav-web
```

这些 Maven 模块首先服务于长期标准 A2A 多 Agent 服务化架构。当前代码形态仍然可以临时作为 Maven 多模块单体聚合运行，用于过渡期联调和调试；长期标准形态应支持专业 Agent 模块独立启动、注册 Nacos、发布 Agent Card，并通过 A2A 被 Orchestrator 调用。


---

## 3. 模块职责总表

| 模块                            | 类型                       | 长期职责                                                                                                                          | 不应该做什么                                                                 |
| ----------------------------- | ------------------------ | ----------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| `mac-tav-common`              | 公共基础模块                   | 放公共枚举、异常、统一响应、工具、常量。                                                                                                          | 不放业务流程、不放 Agent 逻辑、不放 Spring AI provider 绑定。                           |
| `mac-tav-model`               | 共享数据模型模块                 | 放 `NetworkIntent`、`NetworkPlan`、`ConfigSet`、`ExecutionReport`、`ValidationReport`、`RepairPlan`、`NetworkWorkspace` 等 DTO / 值对象。 | 不依赖 Web、Orchestrator、Agent 实现、Spring AI Alibaba 类型。                    |
| `mac-tav-agent-core`          | Agent 公共能力模块             | 放通用 Agent 初始化、`AgentUtils`、Prompt 加载、公共 hooks、ResponseSchema 调用封装、通用 Tool / MCP / Skill / A2A 抽象。                             | 不放具体业务 Agent 的 Prompt 和业务逻辑，不放 Orchestrator 侧远程调用实现，不强绑定具体模型 provider。 |
| `mac-tav-model-core`          | 状态中心模块                   | 放 `NetworkWorkspace` 管理、任务状态、版本、阶段产物、执行日志、追溯关系。                                                                               | 不调用大模型，不生成规划，不生成配置，不执行仿真。                                              |
| `mac-tav-intent-agent`        | 真实 Agent 模块              | 输入自然语言和任务上下文，输出 `NetworkIntent`。                                                                                              | 不生成设备、接口、VLAN、IP、CLI。                                                  |
| `mac-tav-planning-agent`      | 真实 Agent 模块              | 输入 `NetworkIntent`，输出 `NetworkPlan`，可调用规划工具。                                                                                  | 不生成 CLI 命令。                                                            |
| `mac-tav-configuration-agent` | 真实 Agent 模块              | 输入 `NetworkPlan`，输出 `ConfigSet`，可调用 RAG、配置模板、命令知识库工具。                                                                         | 不只返回一整段命令文本。                                                           |
| `mac-tav-execution`           | 执行适配模块                   | 输入 `NetworkPlan + ConfigSet`，输出 `ExecutionReport`；以 `ExecutionAdapter` 为核心，对接 Mininet、Ryu、Docker、Shell、MCP 工具。                | 不作为纯 LLM Agent，不执行 LLM 拼出来的任意 shell。                                   |
| `mac-tav-verification-agent`  | 真实 Agent 模块              | 输入 `NetworkIntent`、`NetworkPlan`、`ConfigSet`、`ExecutionReport`，输出 `ValidationReport`，可调用验证规则工具和结果解释工具。                        | 不直接修改配置。                                                               |
| `mac-tav-healing-agent`       | 真实 Agent 模块              | 输入 `ValidationReport`、`NetworkWorkspace`、失败上下文，输出 `RepairPlan`，可调用诊断工具、策略分析工具、修复建议工具。                                         | 不绕过 Orchestrator 直接修改 Workspace，不直接执行修复命令。                             |
| `mac-tav-orchestrator`        | 流程编排模块                   | 串联 Intent、Planning、Configuration、Execution、Verification、Healing；负责阶段推进、异常收敛、产物写入 Model Core。                                  | 不构造 Prompt，不直接调用 `ChatModel` / `ReactAgent`。                           |
| `mac-tav-web`                 | Web / Visualization 启动模块 | 负责对外 HTTP API、前端交互、SSE、鉴权和任务入口；当前过渡开发阶段可临时作为本地聚合启动入口。                                                                         | 不写业务流程，不直接调用 Agent，不直接调用模型；不作为长期 Agent 聚合中心。                           |

---

## 4. Maven 依赖方向

模块依赖必须保持单向。推荐依赖方向如下：

```text
mac-tav-common
  ↑
mac-tav-model
  ↑
mac-tav-agent-core
  ↑
具体 Agent 模块

mac-tav-common + mac-tav-model
  ↑
mac-tav-model-core

mac-tav-common + mac-tav-model
  ↑
mac-tav-execution
```

长期标准 A2A 多 Agent 服务化架构下，Orchestrator 不应通过 Maven 直接依赖专业 Agent 实现类。Orchestrator 使用 RemoteAgentTool / A2A Client 作为远程 Agent 调用工具或客户端，负责发现 Agent Card、执行 A2A 调用和处理远程异常，但不承担业务编排职责，不写 Workspace，不管理任务状态。

```text
mac-tav-orchestrator
  -> RemoteAgentTool / A2A Client
  -> Nacos Agent Discovery
  -> Agent Card
  -> 专业 Agent A2A Service
```

当前过渡开发方式下，可以由 Orchestrator 直接依赖本地 Agent 模块完成单进程联调：

```text
mac-tav-model-core + 各 Agent 模块 + mac-tav-execution
  ↑
mac-tav-orchestrator
  ↑
mac-tav-web
```

该方式只用于早期验证和无 Nacos / A2A 环境下的开发调试，不作为长期标准调用方式。

明确规则：

1. `mac-tav-common` 不依赖任何业务模块。
2. `mac-tav-model` 只能依赖 `mac-tav-common`。
3. `mac-tav-agent-core` 可以依赖 `mac-tav-common` 和 `mac-tav-model`。
4. 具体 Agent 模块可以依赖 `mac-tav-agent-core`、`mac-tav-model`、`mac-tav-common`。
5. 具体 Agent 模块不得依赖 `mac-tav-web`。
6. 具体 Agent 模块不得依赖 `mac-tav-orchestrator`。
7. 具体 Agent 模块不得依赖其他具体 Agent 模块。
8. 具体 Agent 模块不得直接写 Workspace，不得绕过 Parser / Validator。
9. `mac-tav-model-core` 只依赖 `mac-tav-model` 和 `mac-tav-common`。
10. `mac-tav-execution` 可以依赖 `mac-tav-model` 和 `mac-tav-common`；如需复用 Agent 通用抽象，可以依赖 `mac-tav-agent-core`，但不要依赖具体 Agent 模块。
11. `mac-tav-orchestrator` 在长期标准架构下通过 RemoteAgentTool / A2A Client 调用远程专业 Agent，不直接依赖专业 Agent 实现类。
12. 当前过渡开发方式下，`mac-tav-orchestrator` 可以临时依赖 Model Core、各 Agent 模块和 Execution 模块用于本地联调。
13. RemoteAgentTool / A2A Client 默认放在 `mac-tav-orchestrator` 中，作为 Orchestrator 调用远程专业 Agent 的客户端适配能力；不作为新的业务编排模块，不写 Workspace，不管理任务状态。
14. `mac-tav-agent-core` 只保留通用 Agent 初始化、上下文、Prompt、Hook、Tool/MCP/Skill/A2A 抽象，不放置面向 Orchestrator 的远程调用实现。
15. `mac-tav-web` 可以依赖 Orchestrator、Model、Common。
16. Model Core 不得依赖任何 Agent 模块。
17. Web 不得直接依赖具体 Agent 模块，正常应通过 Orchestrator 间接调用。
18. Agent 通信通过 A2A / Agent Card / RemoteAgentTool / A2A Client / 约定协议完成，不通过 Maven 直接依赖彼此实现类。
19. 禁止 Maven 循环依赖。

## 5. Spring Boot 启动模块规则

### 5.1 长期标准服务化架构

长期标准架构是最终 A2A 多 Agent 服务化架构。专业 Agent 模块可以作为独立 Spring Boot 服务启动，并注册到 Nacos。Agent 模块拥有启动类和配置文件不等于架构污染。

Agent 模块可以包含：

1. Spring Boot 启动类。
2. `application.yml`。
3. A2A Server / Client 配置。
4. Nacos 注册配置。
5. Agent Card 配置。

边界规则：

1. Agent 模块仍然不得依赖 `mac-tav-web`。
2. Agent 模块仍然不得依赖 `mac-tav-orchestrator`。
3. Agent 模块仍然不得依赖其他具体 Agent 模块。
4. Agent 模块不得直接写 Workspace。
5. Agent 模块不得绕过 Parser / Validator。
6. 共享 DTO 仍然通过 `mac-tav-model`。
7. 公共异常、枚举、统一响应仍然通过 `mac-tav-common`。
8. Agent 通信通过 A2A / Agent Card / RemoteAgentTool / A2A Client / 约定协议，而不是 Maven 直接依赖。
9. Orchestrator 通过 RemoteAgentTool / A2A Client 查询 Nacos、读取 Agent Card，并通过 A2A 调用专业 Agent；RemoteAgentTool / A2A Client 不承担业务编排职责。
10. 状态、阶段产物、版本和追溯关系仍由 Orchestrator / Model Core / NetworkWorkspace 统一管理。

### 5.2 当前过渡启动方式

当前开发阶段，为了降低联调成本，`mac-tav-web` 可以临时作为 Web 聚合启动模块。这只是过渡开发方式，不是长期架构目标。

过渡规则：

1. `mac-tav-web` 放启动类、Controller、Web 配置和接口 VO。
2. Controller 只放在 `mac-tav-web`。
3. 其他 Agent 模块可以临时作为普通 Maven jar 模块提供 Spring Bean。
4. Agent Bean 可以由 `mac-tav-web` 启动后扫描、装配和调用。
5. Orchestrator 可以临时本地调用各 `XxxService` / `XxxAgent`。
6. Controller 不直接调用 Agent，仍然通过 Orchestrator 间接调用。
7. 后续落地应逐步迁移到 `Orchestrator -> RemoteAgentTool / A2A Client -> Nacos -> Agent Card -> 专业 Agent A2A Service` 的标准链路。

## 6. Spring AI Alibaba 依赖放置边界

本文档只定义 Maven 依赖边界，不展开 Agent 代码细节。

规则：

1. Spring AI Alibaba Agent Framework 相关基础依赖可以由 `mac-tav-agent-core` 或具体 Agent 模块使用，具体以代码实际需要为准。
2. 具体模型 provider starter，例如 DashScope provider，不建议放在 `mac-tav-common` 或 `mac-tav-model`。
3. 具体 Agent 模块可以依赖 Spring AI Alibaba Agent Framework。
4. 当前过渡开发方式下，具体 provider 的自动装配可以临时由 `mac-tav-web` 承载；长期标准 A2A 服务化架构下，应由对应的专业 Agent 服务自身承载。
5. 具体 Agent 初始化、Prompt、methodTools、hooks、outputType 规范不写在本文档，统一引用 `docs/09_AGENT_BUILD_GUIDE.md`。

---

## 7. 数据库、缓存、向量库依赖边界

长期依赖归属如下：

1. MySQL 相关依赖和 Mapper / Repository 主要放在 `mac-tav-model-core`。
2. Redis 相关能力主要用于任务进度、短期状态、SSE 消息，可以放在 `mac-tav-model-core` 或独立基础设施包中，但不要放进 DTO 模块。
3. Qdrant / Vector DB 相关能力主要服务 RAG，优先放在 `mac-tav-configuration-agent` 或独立知识检索模块中。
4. `mac-tav-model` 不引入数据库、Redis、Qdrant 依赖。
5. `mac-tav-common` 不直接绑定具体数据库实现。

这里只写依赖归属，不写详细数据库表结构。数据库表结构交给数据模型或持久化设计文档。

---

## 8. 包名约定

包名统一使用：

```text
com.yali.mactav
```

不要使用 `com.example`，也不要使用 `com.yali` 作为顶层业务包后直接混杂所有代码。
以下模块的包设计设置作为参考，需根据实际业务进行动态调整

### `mac-tav-common`

```text
com.yali.mactav.common
├── enums
├── exception
├── result
├── constants
└── util
```

### `mac-tav-model`

```text
com.yali.mactav.model
├── task
├── intent
├── plan
├── config
├── execution
├── verification
├── healing
└── workspace
```

### `mac-tav-agent-core`

```text
com.yali.mactav.agent.core
├── agent
├── context
├── hook
├── prompt
├── tool
├── mcp
├── skill
└── a2a
```

### 具体 Agent 模块

```text
com.yali.mactav.intent
com.yali.mactav.planning
com.yali.mactav.configuration
com.yali.mactav.verification
com.yali.mactav.healing
```

### `mac-tav-execution`

```text
com.yali.mactav.execution
├── service
├── adapter
├── converter
├── tool
├── mcp
├── client
└── parser
```

### `mac-tav-model-core`

```text
com.yali.mactav.modelcore
├── service
├── repository
├── entity
├── mapper
├── assembler
├── validator
└── statemachine
```

### `mac-tav-orchestrator`

```text
com.yali.mactav.orchestrator
├── service
├── workflow
├── event
└── sse
```

### `mac-tav-web`

```text
com.yali.mactav.web
├── MacTavApplication
├── controller
├── vo
├── config
└── handler
```

---

## 9. 父 pom 与子模块 pom 规则

### 父 `pom.xml`

父 `pom.xml` 负责：

1. 管理 Java 版本。
2. 管理 Spring Boot 版本。
3. 管理 Spring AI / Spring AI Alibaba 版本。
4. 管理公共 `dependencyManagement`。
5. 管理 `modules`。

父 `pom.xml` 不写业务依赖，不作为可启动应用。

### 子模块 `pom.xml`

子模块 `pom.xml` 规则：

1. 只声明自己需要的依赖。
2. 不为了省事把所有依赖都加到每个模块。
3. Agent 模块才引入 Agent 相关依赖。
4. Web 模块引入 Spring Boot Web 启动依赖。
5. Model 模块保持轻量。
6. Common 模块保持轻量。
7. Execution 模块引入执行适配需要的依赖。
8. Model Core 模块引入持久化相关依赖。

---

## 10. 最终 A2A 多 Agent 服务演进边界

当前 Maven 模块首先用于代码边界和依赖边界，不天然等于微服务。

当前过渡开发方式可以继续用于快速联调和单进程调试，但不作为长期标准架构。

长期标准 A2A 多 Agent 服务化架构应优先以模块边界为拆分依据，使 Intent、Planning、Configuration、Verification、Healing 等专业 Agent 能够独立启动、注册 Nacos、发布 Agent Card，并通过 A2A 被 Orchestrator 调用。

拆分后仍应保持 DTO 契约和 `NetworkWorkspace` 状态中心一致。

A2A / Nacos / `RemoteAgentTool` / Agent Card 是最终协作架构的一部分，但不改变 Maven 依赖底线：Agent 模块之间不要通过 Maven 直接依赖彼此实现类，状态也不要由 Agent 服务直接写入 Workspace。

## 11. 禁止事项

以下禁止事项只覆盖 Maven 模块和依赖边界：

1. 不要把所有业务逻辑写进 `mac-tav-web`。
2. 不要让 Agent 模块依赖 `mac-tav-web`。
3. 不要让 Agent 模块依赖 `mac-tav-orchestrator`。
4. 不要让 `mac-tav-model-core` 依赖任何 Agent 模块。
5. 不要让 `mac-tav-model` 依赖 Spring AI Alibaba、Web、Orchestrator、Model Core 或具体 Agent。
6. 不要把 provider starter、数据库、Redis、Qdrant 依赖放进 `mac-tav-model`。
7. 不要因为 Agent 模块拥有启动类或 `application.yml` 就引入 Web、Orchestrator 或其他具体 Agent 依赖；长期标准 A2A 服务化架构下这些启动配置是允许的。
8. 不要让 Execution Module 绕过 adapter 依赖具体 Agent。
9. 不要出现 Maven 循环依赖。
10. 不要在本文档重复大段 Agent 实现细节，Agent 细节归 `docs/09_AGENT_BUILD_GUIDE.md`。

---
