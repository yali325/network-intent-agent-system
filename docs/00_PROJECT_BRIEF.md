# MAC-TAV 项目背景、目标与长期定位

## 1. 项目名称

MAC-TAV：Multi-Agent Collaborative Translation and Validation for Network Intent。
中文名称：基于多智能体协同的网络意图翻译与闭环验证系统。

## 2. 项目一句话简介

MAC-TAV 将用户的自然语言网络需求逐步转化为可规划、可配置、可执行、可验证、可修复的网络方案。

## 3. 项目背景与定位

传统网络变更依赖专家从需求理解到配置验证的全手工流程，存在理解成本高、配置易错、验证滞后、追溯困难等问题。

MAC-TAV 不是单纯配置生成器或聊天机器人，它面向完整闭环：

```text
意图 -> 规划 -> 配置 -> 执行 -> 验证 -> 诊断/修复 -> 再验证
```

系统以最终 A2A 多 Agent 服务化架构为标准形态：专业 Agent 作为独立 Spring Boot 服务启动，注册到 Nacos，发布 Agent Card，通过 A2A 被 Orchestrator 调用。

## 4. 目标场景与核心痛点

典型场景：多区域网络的访问控制、地址/VLAN 规划、OSPF/路由方案、ACL/安全策略/NAT 的结构化生成、Mininet/Ryu 环境验证、失败诊断与修复。

核心痛点：
- 自然语言需求难以直接映射到网络模型
- 意图→规划→配置→执行→验证之间缺少追溯关系
- 配置退化为一整段不可校验的命令文本
- 验证失败后缺少结构化诊断和修复计划
- 工具调用和外部执行不受控存在安全风险

## 5. 长期核心目标

每个 Agent 负责一个阶段，输出结构化 DTO：

| Agent | 输出 |
| --- | --- |
| Intent Agent | `NetworkIntent` |
| Planning Agent | `NetworkPlan` |
| Configuration Agent | `ConfigSet` |
| Execution Module | `ExecutionReport` |
| Verification Agent | `ValidationReport` |
| Healing Agent | `RepairPlan` |

`NetworkWorkspace` 保存全过程状态、版本、阶段产物和追溯关系。各阶段产物必须可序列化、可追踪、可校验。

## 6. 系统架构概览

长期层次与标准调用链：

```text
Controller / API -> Orchestrator
  -> A2aRemoteAgent / AgentCardProvider（官方 SAA A2A starter）
  -> Nacos Discovery -> Agent Card -> Spring AI Alibaba A2A Server 自动暴露的协议入口
  -> XxxAgent -> ResponseSchema -> Parser -> DTO -> Validator
  -> Orchestrator -> Model Core / NetworkWorkspace / Artifact
```

- **Orchestrator**：唯一主编排入口，负责确定性流程编排、Workspace 写入、Artifact 版本管理、异常收敛、阶段重跑和修复闭环。不构造 Prompt，不直接调用大模型。
- **A2aRemoteAgent / AgentCardProvider**：通过官方 `spring-ai-alibaba-starter-a2a-nacos`（参考：<https://java2ai.com/docs/frameworks/agent-framework/advanced/a2a>）实现 Nacos 服务发现与 A2A 远程调用，不手写 HTTP Client 或 Nacos 注册代码。
- **专业 Agent**：以独立 Spring Boot 服务启动，通过 `XxxAgentConfiguration` 注册 `ReactAgent` Bean 和 `XxxAgent` Bean，通过 `application.yml` + starter 自动完成 A2A Server / Agent Card 发布 / Nacos 注册。
- **Model Core**：负责 Workspace、任务状态、版本、日志和追溯关系，不调用大模型。
- **Execution Module**：以 `ExecutionAdapter` 为核心，不执行 LLM 拼出的任意 shell。

## 7. 核心设计思想与创新点

**设计思想：**
- Agent 负责智能推理，不绕过工程边界
- DTO 表达阶段产物，模型原始字符串不进入主流程
- Parser + Validator 把模型输出变成可检查的数据
- `ExecutionAdapter` 控制执行，LLM 不直接执行 shell
- `NetworkWorkspace` 保存版本和追溯关系
- 失败进入 Healing，不只返回错误文本

**创新点：**
- 多智能体分工，每个 Agent 独立维护职责
- 结构化阶段产物，每个阶段有明确 DTO、版本、校验
- 配置可追溯：配置块可回溯到规划元素和用户意图关系
- 执行受控：通过 `ExecutionAdapter` 接入 Mininet、Ryu、Docker
- 验证闭环 + 修复闭环：`ValidationReport` → `RepairPlan`

## 8. 与普通配置生成工具的关键区别

普通工具：`输入需求 -> 输出配置命令`

MAC-TAV：`输入业务意图 -> 结构化理解 -> 网络方案规划 -> 结构化配置 -> 受控执行 -> 意图验证 -> 失败诊断 -> 修复计划`

关键差异：
- 输出多个阶段产物（不是一段文本），每个产物可追踪、可校验
- 通过执行和验证判断意图是否达成，失败时进入 Healing
- 通过 `ExecutionAdapter` 白名单控制外部环境

## 9. 项目价值

将传统网络配置流程从人工命令驱动提升为自然语言意图驱动，通过多智能体协同、结构化配置、受控执行和闭环验证，让网络设计、配置和验证过程更自动化、可解释、可审查、可追溯。

适用于：网络教学（逐步理解业务目标到网络方案的转化）、轻量方案验证（快速生成候选方案并审查配置依据）、智能体互联网方向研究（Spring AI Alibaba Agent + RAG + MCP + A2A + 网络仿真的多智能体闭环实践）。

---

**详细规范以以下专项文档为准：**

| 文档 | 职责 |
| --- | --- |
| `AGENTS.md` | 项目级硬规则和 Codex 行为约束 |
| `docs/02_MAVEN_MODULES.md` | Maven 模块划分、依赖边界、A2A starter |
| `docs/03_MODULE_DESIGN.md` | 业务模块职责和流程 |
| `docs/04_DATA_MODELS.md` | 核心 DTO 字段和数据契约 |
| `docs/06_DEV_PLAN.md` | 开发阶段路线和验收标准 |
| `docs/09_AGENT_BUILD_GUIDE.md` | Agent 构建规范（Prompt、Tool、Parser、Validator、A2A） |