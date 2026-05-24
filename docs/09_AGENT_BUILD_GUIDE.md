# MAC-TAV Spring AI Alibaba Agent 构建规范

本文档面向 Codex 后续代码生成，用于长期指导 MAC-TAV 中每一个真实 Spring AI Alibaba Agent 的实现。文档只规定工程结构、初始化模式、Prompt 管理、工具接入、结构化输出、校验和边界约束，不描述临时演示实现。

关键词含义：

| 关键词    | 含义                     |
| ------ | ---------------------- |
| MUST   | Codex 生成代码时必须遵守        |
| SHOULD | 默认推荐做法，除非当前任务有明确理由才可偏离 |
| MAY    | 可选扩展，只有任务明确要求时再实现      |

Mock 只作为测试、降级、本地替身使用。真实业务主流程 MUST 以 Spring AI Alibaba Agent 为目标设计。

---

## 参考来源

本项目后续实现 Spring AI Alibaba 相关代码时，优先参考以下资料：

1. Spring AI Alibaba DashScope 文档：<https://java2ai.com/integration/chatmodels/dashScope>
2. Spring AI Alibaba 版本说明：<https://java2ai.com/docs/versions>
3. Spring AI Alibaba Quick Start：<https://java2ai.com/docs/quick-start>
4. Spring AI `ChatClient` 文档：<https://docs.spring.io/spring-ai/reference/api/chatclient.html>
5. Spring AI structured output 文档：<https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html>
6. Spring AI advisors 文档：<https://docs.spring.io/spring-ai/reference/api/advisors.html>
7. Spring AI Alibaba 官方项目: <https://github.com/alibaba/spring-ai-alibaba>

---

## 1. 参考项目

MAC-TAV 后续实现 Spring AI Alibaba Agent 时，MUST 参考：

```text
https://github.com/yali325/trip-plan-agent-system.git
```

只允许参考以下工程模式：

1. `AgentUtils.reactAgentBuilder(...)` 的公共 builder 初始化模式。
2. 每个 Agent 自己维护 instruction 的模式。
3. `.methodTools(...)` 注册当前 Agent 工具的模式。
4. `.hooks(...)` 配置 `PlanHook`、`ModelCallLimitHook` 等 hook 的模式。
5. `.outputType(...)` 声明结构化输出 schema 的模式。
6. RemoteAgentTool / A2A Client 封装远程 Agent 调用的工程思路。

MUST NOT 复制旅行规划业务代码、旅行规划 prompt、地图业务工具、景点推荐 skills、Nacos 拓扑或旅行领域 DTO。MAC-TAV 的 Agent MUST 围绕网络意图翻译与闭环验证，输出本项目 DTO。

---

## 2. 总体目标

MAC-TAV 中每个智能体 MUST 是对应独立 Maven 模块中的真实 Spring AI Alibaba Agent，并面向长期独立 A2A Agent 服务化架构构建。

标准 Agent 模块包括：

| Agent | Maven 模块 | 输入 | 输出 DTO |
| --- | --- | --- | --- |
| IntentAgent | `mac-tav-intent-agent` | 用户自然语言、任务上下文 | `NetworkIntent` |
| PlanningAgent | `mac-tav-planning-agent` | `NetworkIntent` | `NetworkPlan` |
| ConfigurationAgent | `mac-tav-configuration-agent` | `NetworkPlan` | `ConfigSet` |
| VerificationAgent | `mac-tav-verification-agent` | `NetworkIntent`、`NetworkPlan`、`ConfigSet`、`ExecutionReport` | `ValidationReport` |
| HealingAgent | `mac-tav-healing-agent` | `ValidationReport`、`NetworkWorkspace`、失败上下文 | `RepairPlan` |

`HealingAgent` 属于后期阶段实现，但长期规范中 MUST 保留角色定义。Codex MUST NOT 在未被要求时提前创建 `mac-tav-healing-agent` 模块或空壳代码。

每个 Agent MUST：

1. 注入 Spring 管理的 `ChatModel`。
2. 通过 `mac-tav-agent-core` 的 `AgentUtils.reactAgentBuilder(...)` 初始化 Agent。
3. 从 `src/main/resources/prompts/{agent}-prompt.md` 加载系统提示词。
4. 只注册当前 Agent 需要的 `methodTools` / tools。
5. 按需接入当前 Agent 需要的 MCP 工具。
6. 按需接入当前 Agent 需要的 skills。
7. 配置 hooks，至少包含模型调用轮次限制。
8. 显式声明 `outputType(XxxResponseSchema.class)`。
9. 先得到 `ResponseSchema`，再经 Parser 转 DTO，再经 Validator 校验。
10. 返回 `mac-tav-model` 中定义的 DTO，不返回模型原始字符串给 Orchestrator。

### 2.1 Agent 的长期服务化形态与当前开发兼容

#### 2.1.1 长期标准形态：独立 A2A Agent 服务

这是长期标准形态。专业 Agent 模块作为独立 Spring Boot 服务启动，注册到 Nacos，发布 Agent Card，并通过 A2A 被 Orchestrator 调用。

- Agent 模块可以拥有自己的 Spring Boot 启动类和 `application.yml`。
- Agent 模块可以配置 A2A Server / Client、Nacos 注册和 Agent Card。
- Agent Card 描述能力、输入输出契约、服务地址、版本等。
- Nacos 负责服务发现和配置注册。
- A2A 负责 Agent 间远程调用。
- Agent 间不要通过 Maven 直接依赖彼此实现类。
- Agent 模块拥有启动类和配置文件不等于架构污染；真正禁止的是依赖 Web、Orchestrator、其他具体 Agent、直接写 Workspace 或绕过 Parser / Validator。

#### 2.1.2 当前开发兼容：本地 Bean 方式

这是当前过渡开发方式，只用于早期联调、单进程调试和无 Nacos / A2A 环境下验证流程，不是长期架构目标。

- Agent 模块可以临时作为普通 Maven jar 提供 Spring Bean。
- `mac-tav-web` 可以临时扫描本地 Agent Bean。
- Orchestrator 可以临时本地调用 `XxxService` / `XxxAgent`。
- 后续落地应逐步迁移到 `Orchestrator -> RemoteAgentTool / A2A Client -> Nacos -> Agent Card -> 专业 Agent A2A Service` 的标准链路。

## 3. 标准调用链

长期标准 Agent 调用链 MUST 保持：

```text
Controller / API
  -> Orchestrator
  -> RemoteAgentTool / A2A Client
  -> Nacos Agent Discovery
  -> Agent Card
  -> 专业 Agent A2A Service
  -> XxxAgent
  -> Spring AI Alibaba Agent
  -> Tools / MCP / Skills
  -> ResponseSchema
  -> Parser
  -> DTO
  -> Validator
  -> Orchestrator
  -> Model Core / NetworkWorkspace / Artifact
```

当前过渡开发方式下，可以临时使用本地 Bean 调用链：

```text
Controller
  -> TaskOrchestratorService
  -> XxxService
  -> XxxAgent
  -> Spring AI Alibaba Agent
  -> Tools / MCP / Skills
  -> ResponseSchema
  -> Parser
  -> DTO
  -> Validator
  -> Orchestrator / Model Core 写入 NetworkWorkspace
```

该链路只用于早期联调和无 Nacos / A2A 环境下的开发调试，不作为长期验收标准。

MUST 遵守的边界：

1. `Controller` MUST NOT 调用 `ChatModel` / `ChatClient` / `ReactAgent`。
2. `Controller` MUST NOT 构造 Prompt。
3. `TaskOrchestratorService` MUST NOT 构造 Prompt。
4. `TaskOrchestratorService` MUST NOT 直接调用大模型。
5. DTO MUST NOT 依赖 Spring AI Alibaba 类型。
6. `Model Core` MUST NOT 调用大模型。
7. `Tool` MUST NOT 直接写 `NetworkWorkspace`。
8. `Execute Module` MUST NOT 执行 LLM 拼出来的任意 shell。
9. Agent 输出 MUST 进入 `ResponseSchema`。
10. `ResponseSchema` MUST 经 Parser 转为 DTO。
11. DTO MUST 经 Validator 校验后才能进入下一阶段。
12. Parser / Validator 是强制边界，不因 A2A 调用而省略。
13. Agent 不直接写 `NetworkWorkspace`，必要时返回结构化阶段产物，由 Orchestrator / Model Core 保存。
14. Agent 模块不得依赖 `mac-tav-web`、`mac-tav-orchestrator` 或其他具体 Agent 模块。
15. RemoteAgentTool / A2A Client 默认放在 `mac-tav-orchestrator` 中，作为 Orchestrator 调用远程专业 Agent 的客户端适配能力；不承担业务编排职责，不写 Workspace，不管理任务状态。

## 4. `mac-tav-agent-core` 规范

`mac-tav-agent-core` MUST 放通用 Agent 初始化、Prompt、Hook、Tool/MCP/Skill/A2A 抽象。它 MUST NOT 放具体业务 prompt、具体网络阶段逻辑、Controller、Orchestrator 逻辑，也不放 Orchestrator 侧 `RemoteAgentTool / A2A Client` 实现。

### 4.1 `AgentUtils` 最小 API

`AgentUtils` MUST 位于：

```text
mac-tav-agent-core/src/main/java/com/yali/mactav/agent/core/agent/AgentUtils.java
```

`AgentUtils` MUST 至少提供以下 API：

```java
public static Builder reactAgentBuilder(String name, String description, ChatModel chatModel);

public static <T> T callSchema(ReactAgent agent, String input, Class<T> outputType);

public static String loadInstruction(String path);

public static BusinessException wrapException(String errorCode, String message, Throwable cause);
```

API 约束：

1. `reactAgentBuilder(...)` MUST 是所有 Agent 初始化 `ReactAgent` 的唯一入口。
2. 各 Agent 模块 MUST NOT 重复写一套 `ReactAgent.builder()` 初始化逻辑。
3. `reactAgentBuilder(...)` MUST 统一设置公共配置，例如 `name`、`description`、`model`、日志开关、公共异常策略。
4. `callSchema(...)` MUST 封装 Agent 调用和结构化输出解析。
5. `loadInstruction(path)` MUST 从 classpath 加载 prompt 文件。
6. `wrapException(...)` MUST 把模型、工具、MCP、A2A、解析错误转换为项目统一异常。

最小实现形态 SHOULD 类似：

```java
public static Builder reactAgentBuilder(String name, String description, ChatModel chatModel) {
    return ReactAgent.builder()
            .name(name)
            .description(description)
            .model(chatModel)
            .enableLogging(true);
}
```

### 4.2 `PromptLoader` / `PromptBuilder`

Java 代码中 MUST 只保留 `PromptLoader` 或 `PromptBuilder` 这类加载与组装工具。

MUST NOT 同时维护“大段 Java 常量 prompt”和 Markdown prompt 两套内容。

### 4.3 `AgentRunContext`

`AgentRunContext` SHOULD 位于：

```text
com.yali.mactav.agent.core.context.AgentRunContext
```

字段 SHOULD 包含：

| 字段 | 说明 |
| --- | --- |
| `taskId` | 当前任务 ID |
| `stage` | 当前阶段 |
| `version` | 当前阶段版本 |
| `traceId` | 调用链追踪 ID |
| `userInput` | 用户原始输入或阶段输入摘要 |
| `workspaceSnapshot` | 当前 `NetworkWorkspace` 快照或必要引用 |

### 4.4 通用 hooks

`mac-tav-agent-core` SHOULD 提供或封装：

1. `PlanHook`：记录 Agent 开始、结束和关键计划。
2. `ModelCallLimitHook`：限制模型调用轮数。
3. `AgentLogHook`：记录输入摘要、工具调用摘要和输出摘要。
4. `TraceHook`：写入 `taskId`、`stage`、`traceId`。
5. `ErrorHandlingHook`：统一转换 Agent 执行异常。

每个真实 Agent MUST 配置模型调用轮次限制。

---

## 5. Agent 模块标准结构

每个真实 Agent 模块 MUST 使用以下结构。未被当前任务要求的 `mcp`、`skill`、`a2a` 空壳 MUST NOT 提前生成。

```text
mac-tav-xxx-agent
├── src/main/java/com/yali/mactav/xxx
│   ├── agent
│   │   └── XxxAgent.java
│   ├── config
│   │   └── XxxAgentConfiguration.java
│   ├── schema
│   │   └── XxxResponseSchema.java
│   ├── service
│   │   ├── XxxService.java
│   │   └── XxxServiceImpl.java
│   ├── tool
│   │   └── XxxTool.java
│   ├── parser
│   │   └── XxxResponseParser.java
│   └── validator
│       └── XxxOutputValidator.java
└── src/main/resources
    └── prompts
        └── xxx-agent-prompt.md
```

目录职责：

| 路径 | 职责 |
| --- | --- |
| `agent` | 封装 Spring AI Alibaba `ReactAgent` 调用 |
| `config` | 装配当前 Agent Bean、配置运行参数 |
| `schema` | 定义模型结构化输出 `ResponseSchema` |
| `service` | 当前开发兼容方式下可临时对 Orchestrator 暴露当前模块服务；长期标准 A2A 服务化架构下承接协议入口后的业务调用 |
| `tool` | 定义当前 Agent 的 `methodTools` |
| `parser` | 执行 `ResponseSchema -> DTO` |
| `validator` | 校验 DTO 领域约束 |
| `resources/prompts` | 存放当前 Agent 系统提示词 |

这是当前开发兼容方式下的最小结构，只用于早期联调和无 Nacos / A2A 环境下验证流程。

长期标准 A2A 服务化架构下，Agent 模块 MAY 在任务明确要求时增加：

```text
src/main/java/com/yali/mactav/xxx/XxxAgentApplication.java
src/main/resources/application.yml
a2a
agentcard
nacos
mcp
skill
```

这些目录和配置只在需要独立启动、注册 Nacos、发布 Agent Card 或暴露 A2A Service 时增加，不应为了占位一次性生成空壳。

---

## 6. Instruction 管理规范

Agent 系统提示词 MUST 统一放在：

```text
src/main/resources/prompts/{agent}-prompt.md
```

命名 MUST 使用小写中划线：

```text
intent-agent-prompt.md
planning-agent-prompt.md
configuration-agent-prompt.md
verification-agent-prompt.md
healing-agent-prompt.md
```

Java 中 MUST 只保留 prompt 加载或组装逻辑，例如：

```java
String instruction = AgentUtils.loadInstruction("prompts/intent-agent-prompt.md");
```

Prompt 文件 MUST 包含：

1. Agent 身份。
2. 输入是什么。
3. 输出是什么。
4. 禁止做什么。
5. 工具使用规则。
6. 输出结构要求。
7. 当前 Agent 的 `ResponseSchema` 字段要求。

Prompt 文件 MUST NOT：

1. 包含 API Key、环境变量值、私有路径。
2. 要求模型生成多个阶段产物。
3. 要求 IntentAgent 输出设备、接口、VLAN、IP、拓扑或 CLI。
4. 要求 PlanningAgent 输出 CLI。
5. 要求 VerificationAgent 修改配置。
6. 要求 HealingAgent 绕过 Orchestrator 修改 Workspace 或直接执行修复命令。
7. 复制旅行规划业务 prompt。

---

## 7. 标准 Agent 代码模板

下面模板以 `IntentAgent` 为例。Codex 后续实现真实 Agent 时 MUST 按这个形态生成，再按具体 Agent 替换 schema、parser、validator 和 tool。

```java
package com.yali.mactav.intent.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.agent.core.agent.AgentUtils;
import com.yali.mactav.agent.core.hook.AgentLogHook;
import com.yali.mactav.agent.core.hook.PlanHook;
import com.yali.mactav.agent.core.hook.TraceHook;
import com.yali.mactav.intent.parser.IntentResponseParser;
import com.yali.mactav.intent.schema.IntentResponseSchema;
import com.yali.mactav.intent.tool.IntentExtractTool;
import com.yali.mactav.intent.validator.IntentOutputValidator;
import com.yali.mactav.model.intent.NetworkIntent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class IntentAgent {

    private static final String PROMPT_PATH = "prompts/intent-agent-prompt.md";

    private final ObjectMapper objectMapper;
    private final IntentResponseParser parser;
    private final IntentOutputValidator validator;
    private final ReactAgent agent;

    public IntentAgent(ChatModel chatModel,
                       IntentExtractTool intentExtractTool,
                       ObjectMapper objectMapper,
                       IntentResponseParser parser,
                       IntentOutputValidator validator) {
        this.objectMapper = objectMapper;
        this.parser = parser;
        this.validator = validator;

        String instruction = AgentUtils.loadInstruction(PROMPT_PATH);

        this.agent = AgentUtils.reactAgentBuilder(
                        "IntentAgent",
                        "MAC-TAV 业务意图理解智能体",
                        chatModel
                )
                .instruction(instruction)
                .methodTools(intentExtractTool)
                .hooks(
                        new PlanHook(),
                        new AgentLogHook(),
                        new TraceHook(),
                        ModelCallLimitHook.builder().runLimit(6).build()
                )
                .outputType(IntentResponseSchema.class)
                .build();
    }

    public NetworkIntent run(IntentAgentRequest request) {
        try {
            String input = objectMapper.writeValueAsString(request);

            IntentResponseSchema schema = AgentUtils.callSchema(
                    agent,
                    input,
                    IntentResponseSchema.class
            );

            NetworkIntent intent = parser.toDto(schema, request);
            validator.validate(intent);
            return intent;
        }
        catch (Exception ex) {
            throw AgentUtils.wrapException(
                    "INTENT_AGENT_FAILED",
                    "IntentAgent failed to produce a valid NetworkIntent",
                    ex
            );
        }
    }
}
```

模板要求：

1. MUST 注入 `ChatModel`。
2. MUST 注入当前 Agent Tool。
3. MUST 注入 `ObjectMapper`。
4. MUST 使用 `AgentUtils.reactAgentBuilder(...)`。
5. MUST 使用 `.instruction(AgentUtils.loadInstruction(...))` 加载 Markdown prompt。
6. MUST 使用 `.methodTools(...)` 注册当前 Agent 工具。
7. MUST 使用 `.hooks(...)` 配置 hooks。
8. MUST 使用 `.outputType(...)` 声明结构化输出。
9. MUST 在 `run(...)` 中执行 `ResponseSchema -> DTO -> Validator`。
10. MUST NOT 把模型原始字符串返回给 `XxxService` 或 Orchestrator。

---

## 8. Tool / `methodTools` 规范

每个 Agent MUST 只注入自己需要的工具。

工具 MUST：

1. 是 Spring Bean。
2. 方法参数明确。
3. 返回值明确。
4. 尽量返回结构化 DTO / record。
5. 将异常转换为 Agent 可处理的错误。
6. 不直接写 `NetworkWorkspace`。
7. 不绕过当前阶段职责。

工具 SHOULD NOT 返回无法解析的大段自然语言。

标准注册形态：

```java
// IntentAgent
.methodTools(intentExtractTool)

// PlanningAgent
.methodTools(addressPlanningTool, vlanPlanningTool, topologyTemplateTool)

// ConfigurationAgent
.methodTools(commandTemplateTool, ragCommandSearchTool)

// VerificationAgent
.methodTools(reachabilityRuleTool, isolationRuleTool)

// HealingAgent
.methodTools(diagnosisTool, repairSuggestionTool, policyAnalysisTool)
```

工具方法 SHOULD 类似：

```java
@Tool(description = "为业务区域分配不冲突的实验网段")
public AddressPlanSuggestion allocateSubnets(
        @ToolParam(description = "业务区域列表") List<String> zoneIds,
        @ToolParam(description = "可用地址池") String cidrPool) {
    ...
}
```

---

## 9. MCP 规范

MCP 是 MAY 扩展。除非任务明确要求，Codex MUST NOT 一次性生成所有 MCP 空壳。

MCP 接入 MUST 遵守：

1. MCP client / adapter 放在当前 Agent 模块或 `mac-tav-agent-core` 的 `mcp` 包中。
2. 每个 Agent 只接入自己需要的 MCP server。
3. MCP 工具返回结果必须转换成当前 Agent 的 `ResponseSchema` 或中间 DTO。
4. MCP 调用失败必须抛出明确异常。
5. MCP 不得绕过 Model Core 直接改 Workspace。
6. Execute 相关 MCP 必须通过 `ExecutionAdapter` 调用。
7. LLM 不得自由拼接 Mininet / Ryu / Docker / shell 命令。

任务明确要求时，结构 MAY 为：

```text
mac-tav-configuration-agent
└── mcp
    ├── ConfigKnowledgeMcpClient
    └── CommandSearchMcpTool

mac-tav-execution
└── mcp
    ├── MininetMcpClient
    └── RyuMcpClient
```

---

## 10. Skills 规范

Skills 是 MAY 扩展。除非任务明确要求，Codex MUST NOT 一次性生成所有 Skill 空壳。

每个 skill MUST 有：

1. 名称。
2. 描述。
3. 适用 Agent。
4. 输入。
5. 输出。
6. 约束。
7. 失败行为。

skills MUST NOT 直接修改全局状态。

skills MAY 封装：

1. 地址规划技能。
2. VLAN 规划技能。
3. ACL 生成技能。
4. OSPF 配置技能。
5. 连通性验证技能。
6. 拓扑可视化数据生成技能。

共享 skill 抽象 MAY 位于：

```text
mac-tav-agent-core
└── skill
    ├── AgentSkill
    ├── SkillRegistry
    └── SkillDescriptor
```

---

## 11. A2A / Agent Card / Nacos 规范

A2A 是长期标准 A2A 多 Agent 服务化架构下的远程协作通道。落地时仍应按任务范围渐进实现，Codex MUST NOT 在未被要求时一次性生成 A2A、Agent Card 或 Nacos 空壳。

长期标准架构下 MUST 遵守：

1. Orchestrator 仍是唯一主编排入口和确定性工程流程主控。
2. Orchestrator 负责决定当前阶段应该调用哪个专业 Agent，并传递阶段输入和 Workspace 摘要。
3. Orchestrator 负责接收专业 Agent 返回的阶段 DTO 或标准失败结果。
4. RemoteAgentTool / A2A Client 默认放在 `mac-tav-orchestrator` 中，作为 Orchestrator 调用远程专业 Agent 的客户端适配能力。
5. RemoteAgentTool / A2A Client 负责从 Nacos 查询 Agent Card。
6. RemoteAgentTool / A2A Client 负责通过 A2A 调用远程专业 Agent。
7. RemoteAgentTool / A2A Client 负责处理远程调用异常和协议适配。
8. RemoteAgentTool / A2A Client 不承担业务编排职责，不写 Workspace，不管理任务状态。
9. Agent Card 描述能力、输入输出契约、服务地址、版本、协议和健康状态等。
10. Nacos 负责服务发现和配置注册。
11. A2A 负责 Agent 间远程调用。
12. 每个 Agent 对外暴露标准输入输出 DTO 或稳定请求 / 响应契约。
13. Agent 之间不得通过 Maven 直接依赖彼此实现类。
14. Agent 之间不得直接共享内部状态。
15. 状态、版本、阶段产物和追溯关系仍通过 `Model Core / NetworkWorkspace` 维护。
16. Parser / Validator 是强制边界，不因远程调用而省略。

A2A 消息 MUST 至少包含：

| 字段 | 说明 |
| --- | --- |
| `taskId` | 当前任务 ID |
| `sourceAgent` | 来源 Agent |
| `targetAgent` | 目标 Agent |
| `stage` | 当前阶段 |
| `artifactVersion` | 关联产物版本 |
| `payload` | 标准 DTO 或中间请求体 |

Agent Card SHOULD 至少包含：

| 字段 | 说明 |
| --- | --- |
| `agentName` | Agent 名称 |
| `capabilities` | 能力描述 |
| `inputContract` | 输入契约 |
| `outputContract` | 输出契约 |
| `serviceEndpoint` | A2A 服务地址 |
| `version` | Agent 版本 |
| `healthStatus` | 健康状态 |
## 12. ResponseSchema / Parser 规范

每个 Agent MUST 定义自己的 `ResponseSchema`。

规则：

1. `ResponseSchema` MUST 是模型结构化输出。
2. `ResponseSchema` MUST NOT 依赖 Spring AI Alibaba 类型。
3. `ResponseSchema` MAY 不等于最终 DTO。
4. Parser MUST 负责 `ResponseSchema -> DTO`。
5. `.outputType(...)` MUST 显式声明当前 Agent 的 schema。
6. Parser MUST 补齐 `taskId`、版本号、阶段状态、追溯字段。
7. Parser MUST 清理模型不应控制的系统字段。

标准声明：

```java
.outputType(IntentResponseSchema.class)
.outputType(PlanningResponseSchema.class)
.outputType(ConfigurationResponseSchema.class)
.outputType(VerificationResponseSchema.class)
.outputType(HealingResponseSchema.class)
```

---

## 13. Validator 规范

每个 Agent 输出 DTO 后 MUST 校验。校验失败 MUST NOT 进入下一阶段。

### 13.1 `IntentOutputValidator`

MUST 校验：

1. `semanticIntentGraph` 不为空。
2. `nodes` 不为空。
3. `relations` 不为空。
4. `relation.source` / `relation.target` 必须存在。
5. 不允许出现设备、接口、VLAN、IP、拓扑、CLI。

### 13.2 `PlanningOutputValidator`

MUST 校验：

1. `topology` 不为空。
2. `zones` 不为空。
3. `addressPlan` / `vlanPlan` 引用的 `zoneId` 必须存在。
4. 不允许出现 CLI 命令。
5. `securityPolicyPlan.basedOnIntentRelation` 应能追溯到意图关系。

### 13.3 `ConfigurationOutputValidator`

MUST 校验：

1. `deviceConfigs` 不为空。
2. `commandBlocks` 不为空。
3. 每个 `commandBlock` 必须有 `blockId`、`commands`、`explanation`、`traceRefs`。
4. 每个 `commandBlock` 必须有 `rollbackCommands`，或明确说明不可回滚原因。
5. `traceRefs.planElementIds` 应能追溯到规划元素。

### 13.4 `VerificationOutputValidator`

MUST 校验：

1. `overallStatus` 不为空。
2. `items` 不为空。
3. 每个 item 必须有 `expected`、`actual`、`passed`。
4. 每个 item 应关联 intent / plan / config / test 中的可追溯 ID。

### 13.5 `HealingOutputValidator`

MUST 校验：

1. `repairPlan` 或等价修复方案主体不为空。
2. 每个修复动作必须有 `actionId`、`actionType`、`reason`、`riskLevel`。
3. 每个修复动作必须关联失败验证项或失败上下文。
4. 不允许包含立即执行的 shell 命令。
5. 不允许声明已直接修改 `NetworkWorkspace`。
6. 不允许声明已直接下发修复配置。

---

## 14. 各 Agent 职责边界

### 14.1 `IntentAgent`

MUST 输出 `NetworkIntent`。

MUST 只理解业务意图：

1. 业务对象。
2. 访问关系。
3. 隔离关系。
4. 协议偏好。
5. 缺省假设。

MUST NOT 输出设备、接口、VLAN、IP、拓扑、CLI。

### 14.2 `PlanningAgent`

MUST 输入 `NetworkIntent`，输出 `NetworkPlan`。

MUST 生成网络规划：

1. 拓扑。
2. 区域。
3. 地址规划。
4. VLAN 规划。
5. 路由规划。
6. 安全策略。
7. NAT 规划。

MUST NOT 生成 CLI 命令。

### 14.3 `ConfigurationAgent`

MUST 输入 `NetworkPlan`，输出 `ConfigSet`。

MUST 输出：

1. `deviceConfigs`。
2. `commandBlocks`。
3. `traceRefs`。
4. `rollbackCommands`。

MUST NOT 只输出一整段命令文本。

### 14.4 `VerificationAgent`

MUST 输入 `NetworkIntent`、`NetworkPlan`、`ConfigSet`、`ExecutionReport`。

MUST 输出 `ValidationReport`。

MUST NOT 直接修改配置。

MUST NOT 直接触发重新执行。

### 14.5 `HealingAgent`

`HealingAgent` 是后期阶段实现的真实 Agent，长期规范中 MUST 保留其角色定义。

MUST 输入验证失败上下文，至少包括：

1. `ValidationReport`。
2. `NetworkWorkspace`。
3. 失败验证项。
4. 关联的 intent / plan / config / execution 追溯信息。

MUST 输出 `RepairPlan`。

MAY 调用：

1. 诊断工具。
2. 配置修复建议工具。
3. 策略分析工具。
4. 失败原因归因工具。

MUST NOT 直接绕过 Orchestrator 修改 `NetworkWorkspace`。

MUST NOT 直接执行修复命令。

修复动作 MUST 仍由 Orchestrator / ExecutionAdapter 控制。

### 14.6 Execute Module

`mac-tav-execution` SHOULD 以 `ExecutionAdapter` 为核心，不作为纯 LLM Agent。

Execute Module MUST：

1. 将 `NetworkPlan + ConfigSet` 转换为 Mininet / Ryu / DryRun 可执行内容。
2. 通过 adapter 白名单控制执行命令。
3. 输出 `ExecutionReport`。

Execute Module MUST NOT：

1. 直接执行 Huawei CLI。
2. 执行 LLM 拼出来的任意 shell。
3. 让 LLM 绕过 adapter 操作 Docker、Mininet、Ryu。


---

## 15. 异常处理规范

以下错误 MUST 转换为 `BusinessException` 或 `AgentResult` 失败：

1. 模型调用失败。
2. Agent 输出结构错误。
3. Parser 转换失败。
4. Validator 校验失败。
5. Tool 调用失败。
6. MCP 调用失败。
7. A2A 调用失败。

错误码 SHOULD 使用：

| 场景 | 错误码 |
| --- | --- |
| 模型调用失败 | `MODEL_CALL_FAILED` |
| 输出结构错误 | `AGENT_SCHEMA_INVALID` |
| Parser 转换失败 | `AGENT_PARSE_FAILED` |
| Validator 校验失败 | `AGENT_OUTPUT_INVALID` |
| Tool 调用失败 | `TOOL_CALL_FAILED` |
| MCP 调用失败 | `MCP_CALL_FAILED` |
| A2A 调用失败 | `A2A_CALL_FAILED` |

Orchestrator 捕获异常后 MUST：

1. 更新任务状态为 `ERROR`。
2. 写入 `AgentStepLog`。
3. 向 Web 层抛出统一错误。
4. 不泄漏 API Key、请求头、完整外部凭据或敏感环境变量。

---

## 16. 配置规范

配置 MUST 遵守：

1. API Key 不得硬编码。
2. `ChatModel` 的 `base-url`、`model`、`api-key` 必须配置化。
3. 每个 Agent 的 `runLimit` 必须配置化或有明确默认值。
4. 每个 Agent 的 `temperature`、`maxTokens` SHOULD 可配置。
5. 每个 Agent 的 `enabledTools`、`enabledMcpServers` MAY 可配置。

统一配置前缀 SHOULD 为：

```yaml
mactav:
  agents:
    intent:
      enabled: true
      run-limit: 6
    planning:
      enabled: true
      run-limit: 8
    configuration:
      enabled: true
      run-limit: 10
    verification:
      enabled: true
      run-limit: 6
    healing:
      enabled: false
      run-limit: 8
```

DashScope 配置 SHOULD 使用环境变量：

```yaml
spring:
  ai:
    model:
      chat: dashscope
    dashscope:
      api-key: ${AI_DASHSCOPE_API_KEY:${ALI_API_KEY:}}
      chat:
        options:
          model: ${DASHSCOPE_CHAT_MODEL:qwen-plus}
          temperature: 0.2
```

长期标准 A2A Agent 服务化架构下，Agent 模块 MAY 增加 A2A、Nacos 和 Agent Card 配置，但不得在配置中硬编码 API Key、私有地址凭据或跨模块实现类绑定。

---

## 17. 测试规范

自动化测试 MUST 不调用真实外部模型 API。

测试 MUST 覆盖：

1. `ResponseSchema -> DTO` 转换。
2. Validator 合法输出。
3. Validator 非法输出。
4. Tool 异常。
5. 模型调用异常。
6. Prompt 文件存在性。
7. Agent 输出跨阶段字段时被拒绝。

测试 SHOULD 使用：

1. Stub `ChatModel`。
2. Fake `ReactAgent` 调用封装。
3. Mock Tool。
4. Mock MCP Server 或 Stub Client。
5. 测试 Agent Bean 替换真实 Agent 做 Orchestrator 集成测试。

真实 DashScope / 外部 API MAY 作为手动集成验证，但 MUST 通过环境变量注入 API Key。

---

## 18. 落地优先级

Codex MUST 按任务范围渐进落地，不得在未被要求时一次性生成所有 MCP / Skill / A2A 空壳。

长期实现顺序：

1. 第一阶段：`Agent Core + IntentAgent`。
2. 第二阶段：`PlanningAgent`。
3. 第三阶段：`ConfigurationAgent + RAG / Template Tools`。
4. 第四阶段：`VerificationAgent`。
5. 第五阶段：`ExecutionAdapter + Mininet/Ryu Tool/MCP`。
6. 第六阶段：`HealingAgent + A2A / Nacos / Agent Card`。

每一阶段 MUST 保持长期标准调用链边界稳定；当前本地 Bean 方式只作为过渡开发兼容。

当前开发兼容链路：

```text
Controller -> Orchestrator -> Service -> Agent -> ResponseSchema -> Parser -> DTO -> Validator -> Workspace
```

长期标准 A2A 服务化链路：

```text
Controller -> Orchestrator -> RemoteAgentTool / A2A Client -> Nacos -> Agent Card -> 专业 Agent A2A Service -> XxxAgent -> ResponseSchema -> Parser -> DTO -> Validator -> Orchestrator -> Model Core / NetworkWorkspace / Artifact
```

阶段落地规则：

1. 第一阶段 MUST 先固定 `AgentUtils` 最小 API。
2. 第一阶段 MUST 先实现 `IntentAgent` 的真实 API 调用、prompt 文件、schema、parser、validator。
3. 第二阶段 MUST 保证 `PlanningAgent` 不输出 CLI。
4. 第三阶段 MUST 优先实现配置模板工具，再接 RAG。
5. 第四阶段 MUST 先结合规则工具判断，再由 LLM 组织解释。
6. 第五阶段 MUST 通过 `ExecutionAdapter` 接入 Mininet / Ryu。
7. 第六阶段 SHOULD 将本地直接调用逐步替换为 Orchestrator 侧 RemoteAgentTool / A2A Client 调用，并完成 `HealingAgent` 的 A2A 服务化验证。
