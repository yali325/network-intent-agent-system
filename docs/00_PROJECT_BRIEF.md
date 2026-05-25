# MAC-TAV 项目背景、目标与长期定位

## 1. 项目名称

MAC-TAV：Multi-Agent Collaborative Translation and Validation for Network Intent。

中文名称：基于多智能体协同的网络意图翻译与闭环验证系统。

## 2. 项目一句话简介

MAC-TAV 是一个基于多智能体协同的网络意图翻译与闭环验证系统，
能够将用户的自然语言网络需求逐步转化为可规划、可配置、可执行、可验证、可修复的网络方案。

## 3. 项目长期定位
MAC-TAV 是一个面向网络意图翻译、配置生成、执行适配、验证评估和自愈修复的长期工程系统。

它不是单纯的配置生成器，也不是只回答网络问题的聊天机器人。系统必须形成完整闭环：

```text
意图 -> 规划 -> 配置 -> 执行 -> 验证 -> 诊断/修复 -> 再验证
```

长期实现应以最终 A2A 多 Agent 服务化架构为标准形态。
Intent、Planning、Configuration、Verification、Healing 等专业 Agent 作为独立 Spring Boot 服务启动，注册到 Nacos，发布 Agent Card，并通过 A2A 被 Orchestrator 调用。

系统通过结构化阶段产物、受控工具调用、RAG / MCP / Skills 增强能力、ExecutionAdapter 执行适配和 NetworkWorkspace 状态管理，完成意图解析、网络规划、配置生成、受控执行、验证评估和失败修复闭环。

## 4. 赛题背景与技术趋势

网络配置和验证长期依赖专家经验，需求表达、方案设计、设备命令和验证结果之间缺少统一语义链路。

大模型和 Agent Framework 带来了新的工程机会：

- 使用自然语言表达网络意图。
- 将意图结构化为可验证的数据模型。
- 通过工具和知识库增强配置生成。
- 通过仿真或真实执行环境验证配置效果。
- 在验证失败后生成可追踪的修复计划。

MAC-TAV 关注的是大模型在网络工程中的可控落地，而不是让模型自由生成和执行命令。

## 5. 项目背景

传统网络变更通常经历：

1. 业务方提出需求。
2. 网络工程师理解意图。
3. 工程师设计拓扑、地址、路由和安全策略。
4. 工程师编写设备配置。
5. 在实验或生产环境中验证。
6. 出错后人工定位、回滚或修复。

这个流程存在理解成本高、配置易错、验证滞后、追溯困难等问题。

MAC-TAV 希望把这些阶段拆成清晰的智能体与工程模块，每个阶段都有明确输入、输出、校验和追溯关系。

## 6. 目标场景

典型目标场景包括：

- 办公区、访客区、服务器区之间的访问控制。
- 多区域网络的地址和 VLAN 规划。
- OSPF / 静态路由 / 默认路由等连通性方案。
- ACL / 安全策略 / NAT 的结构化生成。
- Mininet / Ryu 或自定义执行环境中的验证。
- 验证失败后的诊断和修复建议。
- 教学、实验、方案推演和轻量网络设计验证。

## 7. 核心痛点

MAC-TAV 主要解决以下痛点：

1. 自然语言需求难以直接映射到网络模型。
2. 意图、规划、配置、执行和验证之间缺少追溯关系。
3. 配置生成容易退化为一整段不可校验的命令文本。
4. 执行结果与用户意图之间缺少自动判断。
5. 验证失败后缺少结构化诊断和修复计划。
6. 工具调用和外部执行如果不受控，存在安全风险。
7. 前端难以向用户解释“为什么生成这些配置”和“为什么验证失败”。

## 8. 长期项目目标

长期目标包括：

- Intent Agent 输出 `NetworkIntent`。
- Planning Agent 输出 `NetworkPlan`。
- Configuration Agent 输出 `ConfigSet`。
- Execution Module 输出 `ExecutionReport`。
- Verification Agent 输出 `ValidationReport`。
- Healing Agent 输出 `RepairPlan`。
- NetworkWorkspace 保存全过程状态、版本、阶段产物和追溯关系。
- 前端展示意图、拓扑、配置、执行、验证和修复过程。

每个阶段产物都必须可序列化、可追踪、可校验，并可写入 Workspace。

## 9. 系统核心闭环流程

核心流程为：

```text
用户输入
  -> 意图解析
  -> 网络规划
  -> 配置生成
  -> 执行适配
  -> 验证评估
  -> 验证通过则归档展示
  -> 验证失败则进入 Healing
  -> 修复后再次验证
```

这个流程由 Orchestrator 编排，由 Model Core 管理状态，由 Web / Visualization 展示过程和结果。

## 10. 系统总体架构

系统长期由以下层次组成：

- Web / Visualization：用户输入、任务控制、结果展示。
- Orchestrator：唯一主编排入口，负责确定性流程编排、状态推进、Workspace 写入、Artifact 版本管理、异常收敛、阶段重跑和修复闭环。
- RemoteAgentTool / A2A Client：Orchestrator 侧远程调用适配能力，负责 Nacos 查询、Agent Card 解析、A2A 调用、远程异常处理和协议适配。
- Agent Modules：Intent、Planning、Configuration、Verification、Healing 等专业 Agent 负责阶段能力，作为独立 Spring Boot 服务启动，注册到 Nacos，发布 Agent Card，并通过 A2A 被 Orchestrator 调用。
- Execution Module：ExecutionAdapter 和受控执行环境适配。
- Agent Core：AgentUtils、上下文、Prompt、Hooks、Tool / MCP / Skill / A2A 抽象。
- Model Core：NetworkWorkspace、Artifact、版本、日志、追溯关系。
- Model：跨模块 DTO。
- Common：公共异常、枚举、结果、常量和工具。

长期标准调用链为：

```text
Controller / API
  -> Orchestrator
  -> RemoteAgentTool / A2A Client
  -> Nacos Agent Discovery
  -> Agent Card
  -> 专业 Agent A2A Service
  -> XxxAgent
  -> ResponseSchema
  -> Parser
  -> DTO
  -> Validator
  -> Orchestrator
  -> Model Core / NetworkWorkspace / Artifact
```

详细 Maven 模块和依赖边界见 `docs/02_MAVEN_MODULES.md`。

## 11. 核心设计思想

MAC-TAV 的设计重点是阶段清晰、产物结构化、执行受控、验证闭环。

关键思想：

- 让 Agent 负责智能推理，不让 Agent 绕过工程边界。
- 让 DTO 表达阶段产物，不让模型原始字符串进入主流程。
- 让 Parser 和 Validator 把模型输出变成可检查的数据。
- 让 ExecutionAdapter 控制执行，不让 LLM 直接执行 shell。
- 让 NetworkWorkspace 保存版本和追溯关系。
- 让失败进入 Healing，而不是只返回错误文本。

## 12. 核心创新点

核心创新点包括：

1. 多智能体分工：Intent、Planning、Configuration、Verification、Healing 各自维护独立职责。
2. 结构化阶段产物：每个阶段都有明确 DTO、版本、状态和校验。
3. 配置可追溯：配置块可回溯到规划元素和用户意图关系。
4. 执行受控：通过 ExecutionAdapter 接入 Mininet、Ryu、Docker 或自定义环境。
5. 验证闭环：执行结果由 VerificationAgent 解释并形成 ValidationReport。
6. 修复闭环：验证失败后由 HealingAgent 输出 RepairPlan。
7. 服务化协作：A2A、Nacos、Agent Card 是长期标准协作链路的一部分；RAG、MCP、Skills 在边界内增强 Agent 能力。

## 13. 长期技术路线

长期技术路线包括：

- Spring AI Alibaba Agent Framework。
- ChatModel / DashScope。
- `AgentUtils.reactAgentBuilder`。
- instruction / methodTools / hooks / outputType。
- RAG。
- MCP。
- A2A。
- Skills。
- MySQL。
- Redis。
- Qdrant。
- Mininet。
- Ryu。
- ExecutionAdapter。
- Vue 3 + TypeScript 前端展示。

这些能力应按阶段落地，不要求一次性生成所有空壳。

## 14. 核心模块定位

### 14.1 Intent Agent

负责理解业务意图，输出 `NetworkIntent`。它不输出设备、接口、VLAN、IP 或 CLI。

### 14.2 Planning Agent

负责把 `NetworkIntent` 转成 `NetworkPlan`。它规划拓扑、区域、地址、VLAN、路由和安全策略，但不生成 CLI。

### 14.3 Configuration Agent

负责把 `NetworkPlan` 转成结构化 `ConfigSet`。它可以使用 RAG、模板和命令知识库，但不能只返回一段命令文本。

### 14.4 Execution Module

负责通过 `ExecutionAdapter` 把 `NetworkPlan + ConfigSet` 转成可执行内容并输出 `ExecutionReport`。它不是纯 LLM Agent。

### 14.5 Verification Agent

负责根据意图、规划、配置和执行报告判断目标是否达成，输出 `ValidationReport`。

### 14.6 Healing Agent

负责在验证失败后读取失败上下文和 Workspace，输出 `RepairPlan`。它不直接修改 Workspace，不直接执行修复命令。

### 14.7 Model Core / NetworkWorkspace

负责管理任务状态、版本、阶段产物、执行日志、追溯关系和修复历史。它不调用大模型，不生成规划，不生成配置，不执行仿真。

### 14.8 Frontend

负责展示意图、拓扑、配置块、执行状态、验证报告、失败原因、修复建议和 Agent 执行轨迹。

## 15. 数据与状态管理思路

MAC-TAV 的数据底座是 `mac-tav-model` 和 `mac-tav-model-core`。系统通过统一的工作空间模型 NetworkWorkspace 保存任务状态、阶段产物、版本历史、执行记录和追溯关系。

核心阶段产物包括：

- `NetworkIntent`
- `NetworkPlan`
- `ConfigSet`
- `ExecutionReport`
- `ValidationReport`
- `RepairPlan`
- `NetworkWorkspace`
- `NetworkArtifact`
- `AgentExecutionRecord`

所有阶段产物都必须写入 `NetworkWorkspace`。任何 Agent、Tool、MCP 或 A2A 调用都不能绕过 Model Core 直接修改全局状态。

详细字段和数据契约见 `docs/04_DATA_MODELS.md`。

## 16. 与普通配置生成工具的区别

普通配置生成工具通常只解决：

```text
输入需求 -> 输出配置命令
```

MAC-TAV 解决的是：

```text
输入业务意图
  -> 结构化理解
  -> 网络方案规划
  -> 结构化配置生成
  -> 受控执行适配
  -> 意图验证
  -> 失败诊断
  -> 修复计划
```

关键差异：

- MAC-TAV 输出多个阶段产物，而不是一段文本。
- MAC-TAV 保存追溯关系，能解释配置来自哪条意图和哪个规划元素。
- MAC-TAV 通过执行和验证判断意图是否达成。
- MAC-TAV 在失败时进入 Healing 流程，输出 `RepairPlan`。
- MAC-TAV 通过 `ExecutionAdapter` 控制外部环境，而不是让模型自由执行命令。

## 17. 工程落地路线概览

工程落地以 `docs/06_DEV_PLAN.md` 为准。

本文档只保留高层顺序：

1. 先稳定 Agent Core，包括 AgentUtils、Prompt 加载、hooks、结构化调用、异常封装和 Parser / Validator 离线测试样例组织。
2. 搭建长期 A2A 服务化基础，包括专业 Agent 独立启动、Nacos 注册、Agent Card 发布、RemoteAgentTool / A2A Client 调用链。
3. 按阶段逐个实现真实 Agent：Intent、Planning、Configuration、Verification、Healing。
4. 按执行阶段接入 ExecutionAdapter，并逐步支持结构校验模式、Mininet / Ryu 等受控执行环境；结构校验模式不得替代最终执行验收。
5. 持久化、异步执行、SSE、前端体验、RAG、MCP、Skills 按真实需求逐步增强。

## 18. 面向 Codex 的长期开发理解要求

Codex 开发本项目时必须理解：

- 所有 Agent 代码必须遵守 `docs/09_AGENT_BUILD_GUIDE.md`。
- Controller 不直接调用 `ChatModel` / `ChatClient`。
- Orchestrator 不直接构造 Prompt。
- 每个 Agent 在自己的 Maven 模块中实现。
- 每个真实 Agent 必须遵守 docs/09_AGENT_BUILD_GUIDE.md 中定义的 Prompt、工具调用、结构化输出、Parser 和 Validator 规范。
- DTO 不依赖 Spring AI Alibaba 类型。
- Model Core 只负责状态、版本、产物和追溯关系。
- Execution Module 不能执行 LLM 拼出来的任意 shell。
- 所有阶段产物必须写入 NetworkWorkspace。
- API Key 不允许硬编码。

详细规则以 `AGENTS.md` 和 `docs/09_AGENT_BUILD_GUIDE.md` 为准。

## 19. 项目价值总结

MAC-TAV 的价值在于把传统网络配置流程从人工命令驱动提升为自然语言意图驱动，并通过多智能体协同、统一网络模型、结构化配置生成、受控执行适配、闭环验证和失败修复机制，让网络设计、配置和验证过程更加自动化、可解释、可审查和可追溯。

对网络教学，它可以帮助学生理解业务目标如何逐步转化为拓扑、地址、VLAN、路由、安全策略和验证结果。

对轻量网络方案验证，它可以帮助工程师快速生成候选方案、审查配置依据、验证策略是否达成，并在失败时获得可追溯的修复建议。

对智能体互联网方向，它展示了 Spring AI Alibaba Agent、RAG、MCP、A2A、Skills 和网络仿真工具如何结合成一个面向真实工程问题的多智能体闭环系统。
