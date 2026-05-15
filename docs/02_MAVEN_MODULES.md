# Maven 模块规划

本文档定义当前 Demo 阶段的标准 Maven 多模块结构。详细模块职责以 `docs/03_MODULE_DESIGN.md` 为准。

## 1. 模块总览

当前阶段只做 Maven 多模块单体，不拆真实微服务，不引入服务注册发现。

标准模块结构如下：

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
├── mac-tav-orchestrator
└── mac-tav-web
```

Healing 模块当前阶段不创建正式 Maven 模块。后续扩展时再考虑：

```text
mac-tav-healing-agent
```

## 2. 模块职责

| 模块 | 职责 | 当前阶段实现方式 |
| --- | --- | --- |
| `mac-tav-common` | 公共枚举、异常、工具、常量、统一响应 | 轻量实现 |
| `mac-tav-model` | 所有共享 DTO、值对象、跨模块数据结构 | 必须优先实现 |
| `mac-tav-agent-core` | 通用 Agent 抽象，如 `BaseAgent`、`AgentContext`、`AgentResult` | 只放抽象和基础对象 |
| `mac-tav-model-core` | `NetworkWorkspace` 状态与产物管理 | 内存 Mock 存储 |
| `mac-tav-intent-agent` | 意图解析，输出 `NetworkIntent` | Mock / 规则解析 |
| `mac-tav-planning-agent` | 网络规划，输出 `NetworkPlan` | 固定模板方案 |
| `mac-tav-configuration-agent` | 配置生成，输出 `ConfigSet` | 固定 Huawei 风格命令模板 |
| `mac-tav-execution` | 执行与仿真适配，输出 `ExecutionReport` | DryRun / Mock 结果 |
| `mac-tav-verification-agent` | 意图验证，输出 `ValidationReport` | 规则判断 Mock 测试结果 |
| `mac-tav-orchestrator` | 串联任务流程，协调各阶段调用 | 本地 Service 调用 |
| `mac-tav-web` | Spring Boot 启动模块和 Controller | 唯一启动入口 |

## 3. 依赖方向

依赖关系必须保持单向，避免循环依赖。

推荐依赖方向：

```text
mac-tav-common
    ↑
mac-tav-model
    ↑
mac-tav-agent-core
    ↑
具体 Agent 模块

mac-tav-common
    ↑
mac-tav-model
    ↑
mac-tav-model-core

具体 Agent 模块 + mac-tav-execution + mac-tav-verification-agent + mac-tav-model-core
    ↑
mac-tav-orchestrator
    ↑
mac-tav-web
```

更具体地说：

1. `mac-tav-common` 不依赖任何业务模块。
2. `mac-tav-model` 可以依赖 `mac-tav-common`，但不依赖 Agent、Model Core、Orchestrator、Web。
3. `mac-tav-agent-core` 可以依赖 `mac-tav-common` 和 `mac-tav-model`。
4. `mac-tav-intent-agent`、`mac-tav-planning-agent`、`mac-tav-configuration-agent`、`mac-tav-verification-agent` 可以依赖 `mac-tav-agent-core`、`mac-tav-model`、`mac-tav-common`。
5. `mac-tav-execution` 可以依赖 `mac-tav-model` 和 `mac-tav-common`，如需复用 Agent 抽象，也可以依赖 `mac-tav-agent-core`。
6. `mac-tav-model-core` 只依赖 `mac-tav-model` 和 `mac-tav-common`，不依赖任何 Agent 模块。
7. `mac-tav-orchestrator` 可以依赖 Model Core、各 Agent、Execution、Verification。
8. `mac-tav-web` 可以依赖 `mac-tav-orchestrator`、`mac-tav-model`、`mac-tav-common`。
9. Agent 模块不依赖 `mac-tav-orchestrator` 和 `mac-tav-web`。
10. `mac-tav-web` 是唯一 Spring Boot 启动模块。

## 4. 包名约定

包名统一使用 `com.yali`，不要使用 `com.example`。

建议基础包结构如下：

### 4.1 `mac-tav-common`

```text
com.yali.mactav.common
├── enums
├── exception
├── result
└── util
```

### 4.2 `mac-tav-model`

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

### 4.3 `mac-tav-agent-core`

```text
com.yali.mactav.agent.core
├── agent
├── context
└── result
```

建议放置：

1. `BaseAgent`
2. `AgentContext`
3. `AgentResult`
4. `AgentStep`
5. Agent 通用状态和错误描述

### 4.4 `mac-tav-model-core`

```text
com.yali.mactav.modelcore
├── service
├── repository
└── impl
```

当前阶段可以使用内存 Map 保存任务与产物。后续再替换为数据库。

Model Core 不调用大模型，不生成网络方案，不生成配置命令，不执行仿真。

### 4.5 具体 Agent 模块

```text
com.yali.mactav.intent
├── service
└── impl

com.yali.mactav.planning
├── service
└── impl

com.yali.mactav.configuration
├── service
└── impl

com.yali.mactav.verification
├── service
└── impl
```

当前阶段 Service 实现可以直接返回 Mock 数据。

Agent 模块不写 Controller。

### 4.6 `mac-tav-execution`

```text
com.yali.mactav.execution
├── service
├── adapter
└── impl
```

Execution Module 不能直接执行 Huawei CLI，必须通过 Execution Adapter 转换为 Mininet/Ryu/DryRun 可执行内容。

当前阶段优先实现 DryRun / Mock Adapter。

### 4.7 `mac-tav-orchestrator`

```text
com.yali.mactav.orchestrator
├── service
└── impl
```

负责按顺序调用：

```text
Intent -> Planning -> Configuration -> Execution -> Verification
```

每个阶段完成后，都应把产物写入 Model Core / NetworkWorkspace。

### 4.8 `mac-tav-web`

```text
com.yali.mactav.web
├── MacTavApplication
├── controller
└── dto
```

Controller 只做请求接收、参数校验、调用 Orchestrator、返回结果，不写复杂业务逻辑。

## 5. 第一阶段建议创建顺序

1. `mac-tav-common`
2. `mac-tav-model`
3. `mac-tav-agent-core`
4. `mac-tav-model-core`
5. `mac-tav-intent-agent`
6. `mac-tav-planning-agent`
7. `mac-tav-configuration-agent`
8. `mac-tav-execution`
9. `mac-tav-verification-agent`
10. `mac-tav-orchestrator`
11. `mac-tav-web`

## 6. 当前阶段不创建的模块

`mac-tav-healing-agent` 是后期扩展模块。当前阶段不创建正式 Maven 模块，只在文档和 DTO 中保留 Healing / Repair 的 TODO 或 Mock 说明。

## 7. 禁止事项

1. 不要把所有业务逻辑写进 `mac-tav-web`。
2. 不要让 Agent 模块直接调用 Controller。
3. 不要让 Agent 模块依赖 Orchestrator 或 Web。
4. 不要让 Model Core 依赖任何 Agent 模块。
5. 不要让 `mac-tav-model` 依赖任何业务模块。
6. 不要让 Model Core 生成配置或执行仿真。
7. 不要在当前阶段引入真实微服务拆分。
8. 不要在当前阶段强制接入 MySQL、Redis、Qdrant、真实大模型、真实 RAG、Mininet、Ryu。
