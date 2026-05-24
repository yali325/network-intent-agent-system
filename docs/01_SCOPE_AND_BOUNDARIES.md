# 项目范围与能力边界

## 1. 文档目标

本文档定义 MAC-TAV 的长期能力范围和边界。

- MAC-TAV 应该覆盖哪些能力。
- 哪些能力不属于本项目。
- 真实执行必须遵守哪些安全边界。
- 哪些动作必须经过人工确认。
- Mock 在长期项目中的位置。

本文档不展开 Maven 模块依赖、DTO 字段、API 细节、Agent 初始化代码。对应内容分别见：

- `docs/02_MAVEN_MODULES.md`
- `docs/04_DATA_MODELS.md`
- `docs/05_API_DESIGN.md`
- `docs/09_AGENT_BUILD_GUIDE.md`

## 2. 项目能力范围

MAC-TAV 的长期能力范围是网络意图到闭环验证的工程系统。

系统 SHOULD 覆盖：

- 用户自然语言网络需求输入。
- IntentAgent 解析业务意图，输出 `NetworkIntent`。
- PlanningAgent 生成网络规划，输出 `NetworkPlan`。
- ConfigurationAgent 生成结构化配置，输出 `ConfigSet`。
- Execution Module 通过 ExecutionAdapter 执行适配，输出 `ExecutionReport`。
- VerificationAgent 判断意图达成情况，输出 `ValidationReport`。
- HealingAgent 根据失败上下文生成诊断和修复计划，输出 `RepairPlan`。
- NetworkWorkspace 保存全流程产物、版本、追溯关系和执行记录。
- Web / Visualization 展示意图、拓扑、配置、执行、验证和修复过程。

## 3. 长期目标范围

长期目标不是一次性生成一段设备命令，而是形成可追踪、可验证、可修复的闭环。

系统 MUST 支持以下主链路：

```text
用户输入
  -> 意图解析
  -> 网络规划
  -> 配置生成
  -> 执行适配
  -> 验证评估
  -> 通过后归档展示
  -> 失败后进入诊断和修复
  -> 修复后再次验证
```

长期目标 MAY 扩展：

- RAG 支撑配置知识检索。
- MCP 对接外部工具或执行环境。
- A2A 是长期远程 Agent 协作通道，由 Orchestrator 通过 RemoteAgentTool / A2A Client 使用，不替代 Orchestrator。
- Skills 封装可复用网络能力。
- MySQL / Redis / Qdrant 支撑持久化、实时进度和向量检索。


长期架构边界 MUST 保持：

- 不引入额外的管理型大模型 Agent 作为主编排者、智能决策者或跨 Agent 协作者。
- Orchestrator 是唯一主编排入口，决定当前阶段调用哪个专业 Agent。
- 专业 Agent 不直接共享内部状态，不直接编排其他 Agent。
- 专业 Agent 不直接写 Workspace，不推进任务状态，不管理 Artifact 版本。
- 专业 Agent 之间不通过 Maven 直接依赖彼此实现类。
- 专业 Agent 必须执行 `ResponseSchema -> Parser -> DTO -> Validator` 后返回阶段 DTO 或标准失败结果。

## 4. 不属于本项目的能力

以下能力不属于 MAC-TAV 的核心范围：

- 通用网络设备运维平台。
- 任意厂商 CLI 的全量覆盖系统。
- 无人工确认的生产网络自动变更系统。
- 任意 shell 命令执行平台。
- 单纯聊天机器人。
- 只生成配置文本、没有验证闭环的配置工具。
- 替代专业网络工程师做所有架构决策。
- 管理型大模型 Agent 取代 Orchestrator 成为主编排入口。
- 专业 Agent 绕过 Orchestrator 直接写 Workspace、推进任务状态或编排其他 Agent。

这些能力 MAY 在未来作为外部系统集成，但不得改变 MAC-TAV 的主职责边界。

## 5. 安全边界

系统 MUST 遵守：

- API Key 不允许硬编码。
- 不在日志、响应体、测试数据中泄露 API Key、请求头、外部凭据。
- Controller 不接收任意 shell 命令。
- Execution Module 不执行 LLM 拼出来的任意 shell。
- Tool 不直接写 `NetworkWorkspace`。
- Agent 不直接修改全局状态。
- Web 响应不返回完整异常堆栈。
- 高风险修复动作必须进入人工确认或明确审批流程。

## 6. 真实执行边界

Execution Module 是受控执行适配模块，不是纯 LLM Agent。

真实执行 MUST 经过：

```text
NetworkPlan + ConfigSet
  -> ExecutionAdapter
  -> 白名单 Tool / MCP / Adapter
  -> ExecutionReport
```

ExecutionAdapter SHOULD 支持：

- DryRun 校验。
- Mininet / Ryu 仿真。
- Docker 或自定义实验环境。
- 后续真实设备适配。

真实执行不得：

- 直接执行模型生成的任意命令。
- 让 Controller 传入任意 shell。
- 绕过 ExecutionAdapter 修改环境。
- 在失败时只打印日志而不生成 `ExecutionReport`。

## 7. 人工确认边界

以下动作 SHOULD 进入人工确认：

- 对生产设备或真实环境有影响的配置应用。
- 高风险 `RepairAction`。
- 回滚动作。
- 用户意图存在冲突时的澄清。
- 无法从现有上下文推导出的关键规划假设。

HealingAgent 只能输出 `RepairPlan`。它不得直接修改 `NetworkWorkspace`，不得直接执行修复命令。Orchestrator 根据 RepairAction 决定重新进入规划、配置、执行、验证或用户澄清阶段。

## 8. Mock 边界

Mock 不是长期主流程。

Mock MAY 用于：

- 单元测试。
- 离线测试。
- 失败分支验证。
- 本地开发替身。
- 外部依赖不可用时的降级。

Mock  不得成为真实业务能力的替代说明。真实 Agent、RAG、ExecutionAdapter、Verification、Healing 的长期实现仍以专题文档为准。

## 9. 与其他文档的分工

- 本文档：项目范围、能力边界、安全边界、执行边界、人工确认边界。
- `docs/00_PROJECT_BRIEF.md`：项目背景、目标、痛点、长期定位和创新点。
- `docs/02_MAVEN_MODULES.md`：Maven 模块、依赖方向、包名和 pom 边界。
- `docs/03_MODULE_DESIGN.md`：业务模块定位、输入输出、上下游关系。
- `docs/04_DATA_MODELS.md`：核心 DTO、状态、版本、TraceRefs、Artifact。
- `docs/05_API_DESIGN.md`：HTTP API 契约、错误码、Controller 边界。
- `docs/06_DEV_PLAN.md`：长期实现阶段和验收标准。
- `docs/07_TEST_DATA_AND_SCENARIOS.md`：测试场景与样例数据。
- `docs/08_RUN_AND_TEST.md`：运行、测试、手动验证和常见问题。
- `docs/09_AGENT_BUILD_GUIDE.md`：真实 Spring AI Alibaba Agent 构建规范。
