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

这些 Maven 模块服务于长期标准 A2A 多 Agent 服务化架构。长期标准形态应支持专业 Agent 模块独立启动、注册 Nacos、发布 Agent Card，并通过 A2A 被 Orchestrator 调用。


---

## 3. 模块职责总表

| 模块                            | 类型                       | 长期职责                                                                                                                          | 不应该做什么                                                                 |
| ----------------------------- | ------------------------ | ----------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| `mac-tav-common`              | 公共基础模块                   | 放公共异常、统一响应、通用错误码、工具、常量。                                                                                                      | 不放业务流程、不放 Agent 逻辑、不放 Spring AI provider 绑定。                           |
| `mac-tav-model`               | 共享数据模型模块                 | 放 `NetworkIntent`、`NetworkPlan`、`ConfigSet`、`ExecutionReport`、`ValidationReport`、`RepairPlan`、`NetworkWorkspace` 等 DTO / 值对象和领域模型枚举。 | 不依赖 Web、Orchestrator、Agent 实现、Spring AI Alibaba 类型。                    |
| `mac-tav-agent-core`          | Agent 公共能力模块             | 放通用 Agent 初始化、`AgentUtils`、Prompt 加载、公共 hooks、ResponseSchema 调用封装、通用 Tool / MCP / Skill / A2A 抽象。                             | 不放具体业务 Agent 的 Prompt 和业务逻辑，不放 Orchestrator 侧远程调用实现，不强绑定具体模型 provider。 |
| `mac-tav-model-core`          | 状态中心模块                   | 放 `NetworkWorkspace` 管理、任务状态、版本、阶段产物、执行日志、追溯关系。                                                                               | 不调用大模型，不生成规划，不生成配置，不执行仿真。                                              |
| `mac-tav-intent-agent`        | 真实 Agent 模块              | 输入自然语言和任务上下文，输出 `NetworkIntent`。                                                                                              | 不生成设备、接口、VLAN、IP、CLI。                                                  |
| `mac-tav-planning-agent`      | 真实 Agent 模块              | 输入 `NetworkIntent`，输出 `NetworkPlan`，可调用规划工具。                                                                                  | 不生成 CLI 命令。                                                            |
| `mac-tav-configuration-agent` | 真实 Agent 模块              | 输入 `NetworkPlan`，输出 `ConfigSet`，可调用 RAG、配置模板、命令知识库工具。                                                                         | 不只返回一整段命令文本。                                                           |
| `mac-tav-execution`           | 执行适配模块                   | 输入 `NetworkPlan + ConfigSet`，输出 `ExecutionReport`；以 `ExecutionAdapter` 为核心，对接 Mininet、Ryu、Docker、Shell、MCP 工具。                | 不作为纯 LLM Agent，不执行 LLM 拼出来的任意 shell。                                   |
| `mac-tav-verification-agent`  | 真实 Agent 模块              | 输入 `NetworkIntent`、`NetworkPlan`、`ConfigSet`、`ExecutionReport`，输出 `ValidationReport`，可调用验证规则工具和结果解释工具。                        | 不直接修改配置。                                                               |
| `mac-tav-healing-agent`       | 真实 Agent 模块              | 输入 `ValidationReport`、`NetworkWorkspace`、失败上下文，输出 `RepairPlan`，可调用诊断工具、策略分析工具、修复建议工具。                                         | 不绕过 Orchestrator 直接修改 Workspace，不直接执行修复命令。                             |
| `mac-tav-orchestrator`        | 流程编排模块                   | 串联 Intent、Planning、Configuration、Execution、Verification、Healing；负责阶段推进、异常收敛、产物写入 Model Core。                                  | 不构造 Prompt，不直接调用 `ChatModel` / `ReactAgent`。                           |
| `mac-tav-web`                 | Web / Visualization 启动模块 | 负责对外 HTTP API、前端交互、SSE、鉴权和任务入口。                                                                         | 不写业务流程，不直接调用 Agent，不直接调用模型；不作为 Agent 聚合中心。                           |

---

## 4. Maven 依赖方向

模块依赖必须保持单向、清晰、最小化。

长期标准 A2A 多 Agent 服务化架构下，Orchestrator 不通过 Maven 直接依赖任何具体专业 Agent 模块。Orchestrator 只依赖远程调用适配能力，通过 RemoteAgentTool / A2A Client 查询 Nacos、读取 Agent Card，并通过 A2A 调用远程专业 Agent。

推荐依赖方向如下：

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
  
mac-tav-common + mac-tav-model + mac-tav-model-core + mac-tav-execution  
↑  
mac-tav-orchestrator  
  
mac-tav-common + mac-tav-model + mac-tav-orchestrator  
↑  
mac-tav-web
```

Orchestrator 的远程 Agent 调用链为：

```text
mac-tav-orchestrator
  -> RemoteAgentTool / A2A Client
  -> Nacos Agent Discovery
  -> Agent Card
  -> 专业 Agent A2A Service
```

明确规则：

- `mac-tav-common` 不依赖任何业务模块。
- `mac-tav-model` 只能依赖 `mac-tav-common`。
- `mac-tav-agent-core` 可以依赖 `mac-tav-common`、`mac-tav-model` 和必要的 Spring AI / Spring AI Alibaba Agent Framework 基础 API。
- 具体 Agent 模块可以依赖 `mac-tav-agent-core`、`mac-tav-model`、`mac-tav-common`。
- 具体 Agent 模块不得依赖 `mac-tav-web`。
- 具体 Agent 模块不得依赖 `mac-tav-orchestrator`。
- 具体 Agent 模块不得依赖其他具体 Agent 模块。
- 具体 Agent 模块不得依赖 `mac-tav-model-core`。
- `mac-tav-model-core` 只依赖 `mac-tav-model`、`mac-tav-common` 和后续持久化所需依赖。
- `mac-tav-execution` 可以依赖 `mac-tav-model`、`mac-tav-common`；如需复用 Tool / MCP / Skill 抽象，可以依赖 `mac-tav-agent-core`，但不得依赖具体 Agent 模块。
- `mac-tav-orchestrator` 可以依赖 `mac-tav-model-core` 和 `mac-tav-execution`，但不得依赖具体 Agent 模块。
- `mac-tav-orchestrator` 中的 RemoteAgentTool / A2A Client 只负责远程 Agent 发现、调用和协议适配，不承担业务编排职责。
- `mac-tav-web` 可以依赖 `mac-tav-orchestrator`、`mac-tav-model`、`mac-tav-common`，不得依赖具体 Agent 模块。
- Agent 通信通过 A2A / Agent Card / Nacos / RemoteAgentTool / A2A Client 完成，不通过 Maven 直接依赖彼此实现类。
- 禁止 Maven 循环依赖。

## 5. Spring Boot 启动与服务注册规则

本节只定义哪些 Maven 模块可以作为 Spring Boot 应用启动，以及启动后的服务化职责。模块依赖方向以第 4 节为准，不在本节重复。

### 5.1 可启动模块

长期标准 A2A 多 Agent 服务化架构下，以下模块可以作为独立 Spring Boot 应用启动：

| 模块                            | 是否可启动 | 启动职责                                                         |
| ----------------------------- | ----- | ------------------------------------------------------------ |
| `mac-tav-web`                 | 是     | 提供 Web / API / SSE / 前端交互入口，内嵌 Orchestrator 主流程入口。           |
| `mac-tav-intent-agent`        | 是     | 启动 IntentAgent 服务，注册 Nacos，发布 Agent Card，提供 A2A 调用能力。        |
| `mac-tav-planning-agent`      | 是     | 启动 PlanningAgent 服务，注册 Nacos，发布 Agent Card，提供 A2A 调用能力。      |
| `mac-tav-configuration-agent` | 是     | 启动 ConfigurationAgent 服务，注册 Nacos，发布 Agent Card，提供 A2A 调用能力。 |
| `mac-tav-verification-agent`  | 是     | 启动 VerificationAgent 服务，注册 Nacos，发布 Agent Card，提供 A2A 调用能力。  |
| `mac-tav-healing-agent`       | 是     | 启动 HealingAgent 服务，注册 Nacos，发布 Agent Card，提供 A2A 调用能力。       |

### 5.2 非独立启动模块

以下模块默认不作为独立 Spring Boot 应用启动：

| 模块                     | 原因                                           |
| ---------------------- | -------------------------------------------- |
| `mac-tav-common`       | 公共基础库。                                       |
| `mac-tav-model`        | 共享 DTO / 值对象库。                               |
| `mac-tav-agent-core`   | Agent 公共能力库。                                 |
| `mac-tav-model-core`   | 状态中心库，由 Web / Orchestrator 所在进程使用。           |
| `mac-tav-orchestrator` | 编排库，由 `mac-tav-web` 依赖并调用，不单独暴露服务。           |
| `mac-tav-execution`    | 执行适配库，默认由 Orchestrator 调用；后续如需独立执行服务，必须另行设计。 |

### 5.3 启动边界

- `mac-tav-web` 是唯一面向前端和外部调用方的业务 HTTP API 入口。
- 专业 Agent 服务可以拥有自己的 Spring Boot 启动类、`application.yml`、Nacos 注册配置、Agent Card 配置和 A2A Service 配置。
- 专业 Agent 服务不得提供面向前端的业务 Controller。
- 专业 Agent 服务只暴露 A2A 框架所需的内部远程调用能力，不进入 `/api/v1` 公共业务 API。
- `mac-tav-web` 不扫描、不聚合、不直接装配具体 Agent Bean。
- `mac-tav-web` 通过 Orchestrator 触发流程，Orchestrator 通过 RemoteAgentTool / A2A Client 调用远程专业 Agent。

## 6. POM 与依赖管理规则

本文档只规定 Maven 依赖管理方式和依赖归属，不展开具体 Agent 初始化、Prompt、Tool、MCP、Skills、A2A 代码细节。

### 6.1 总体原则

MAC-TAV 使用父工程统一管理版本，子模块按需引入依赖。

父工程 `pom.xml` 负责：

1. 作为 Maven 聚合工程，使用 `<packaging>pom</packaging>`。
2. 维护 `<modules>`。
3. 统一管理 Java、Spring Boot、Spring Cloud、Spring Cloud Alibaba、Spring AI、Spring AI Alibaba、Nacos、MySQL、Redis、Qdrant、测试框架等版本。
4. 通过 `<dependencyManagement>` 管理三方依赖和项目内部模块依赖版本。
5. 通过 `<pluginManagement>` 管理 Maven 插件版本。

父工程 `pom.xml` 不负责：

1. 不作为 Spring Boot 启动应用。
2. 不放业务代码。
3. 不在父工程 `<dependencies>` 中统一引入 Web、Agent、数据库、Redis、Qdrant、模型 provider starter 等具体业务依赖。
4. 不让所有子模块被动继承自己并不需要的依赖。

子模块 `pom.xml` 负责：

1. 只在 `<dependencies>` 中声明当前模块实际需要的依赖。
2. 依赖版本由父工程 `<dependencyManagement>` 统一管理，子模块一般不写 `<version>`。
3. 不为了省事把所有依赖都加到每个模块。
4. 不为了减少配置而依赖不稳定的传递依赖。
5. 测试依赖使用 `test` scope。
6. 可选能力依赖只在真正需要的模块中声明。

### 6.2 父工程 dependencyManagement 示例

父工程可以统一管理所有常用依赖版本，包括业务相关依赖版本：

```xml
<dependencyManagement>
    <dependencies>
        <!-- Spring Boot / Spring Cloud / Spring AI / Spring AI Alibaba BOM -->

        <!-- Web 相关依赖版本 -->
        <!-- Agent Framework 相关依赖版本 -->
        <!-- Nacos / A2A 相关依赖版本 -->
        <!-- MySQL / Redis / Qdrant 相关依赖版本 -->
        <!-- 测试框架相关依赖版本 -->

        <!-- 项目内部模块版本 -->
        <dependency>
            <groupId>com.yali</groupId>
            <artifactId>mac-tav-model</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.yali</groupId>
            <artifactId>mac-tav-common</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

注意：

- 放在 `<dependencyManagement>` 中只是管理版本。
- 不会自动把这些依赖加入所有子模块。
- 子模块需要使用时，仍然要在自己的 `<dependencies>` 中声明。

### 6.3 传递依赖使用原则

允许合理使用 Maven 传递依赖，但不得滥用。

规则：

1. 如果当前模块源码直接 import 某个第三方类，当前模块 SHOULD 显式声明该依赖。
2. 如果当前模块只使用另一个模块封装后的能力，不直接接触底层三方类，可以依赖该模块提供的传递依赖。
3. 不要为了“少写依赖”而依赖偶然传递进来的三方 jar。
4. 不要把某个模块变成“依赖大礼包”，让其他模块通过它间接拿所有依赖。
5. 模块依赖关系必须服务职责边界，而不是为了省 pom 配置。

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

## 9. A2A 多 Agent 服务化与 Maven 边界

Maven 模块用于定义代码边界和依赖边界，不等于所有模块都必须成为独立微服务。

长期标准运行形态中：  
  
1. `mac-tav-web` 是 Web / API / SSE 入口。  
2. `mac-tav-orchestrator` 是编排库，由 `mac-tav-web` 依赖使用，不单独作为服务启动。  
3. Intent、Planning、Configuration、Verification、Healing 等专业 Agent 模块应支持独立 Spring Boot 服务启动。  
4. 专业 Agent 服务启动后注册到 Nacos，发布 Agent Card，并通过 A2A 被 Orchestrator 调用。  
5. `mac-tav-execution` 默认是执行适配模块，由 Orchestrator 调用；如未来需要拆为独立执行服务，必须单独设计服务边界和安全边界。  
6. `mac-tav-model` 和 `mac-tav-common` 只作为共享库，不作为服务启动。  
7. `mac-tav-model-core` 是状态中心模块，默认由 Web / Orchestrator 所在应用使用；如未来拆为独立状态服务，需要单独设计 API 和事务边界。  
  
A2A / Nacos / Agent Card 不改变 Maven 依赖底线：  
  
- Agent 模块之间不通过 Maven 直接依赖彼此实现类。  
- Orchestrator 不通过 Maven 直接依赖具体 Agent 实现类。  
- Agent 服务不直接写 Workspace。  
- 状态、版本、阶段产物和追溯关系仍由 Orchestrator / Model Core 统一管理。  
- 共享契约通过 `mac-tav-model` 和稳定请求 / 响应 DTO 维护。

---
