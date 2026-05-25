# 系统模块设计

## 1. 文档目标

本文档定义 MAC-TAV 长期业务模块设计。

本文档只描述每个业务模块的：

- 模块定位。
- 输入。
- 输出。
- 核心处理内容。
- 不做什么。
- 上下游关系。

本文档不展开 DTO 全字段、API 路径、Maven 依赖方向、Agent 初始化代码。对应内容见：

- DTO 字段：`docs/04_DATA_MODELS.md`
- API 契约：`docs/05_API_DESIGN.md`
- Maven 边界：`docs/02_MAVEN_MODULES.md`
- Agent 构建：`docs/09_AGENT_BUILD_GUIDE.md`

## 2. 总体业务链路

MAC-TAV 的长期业务链路为：

```text
用户输入
  -> Intent Module
  -> Planning Module
  -> Configuration Module
  -> Execution Module
  -> Verification Module
  -> Healing Module
  -> 再次进入规划 / 配置 / 执行 / 验证
```

Orchestrator 是唯一主编排入口和确定性工程流程控制入口。RemoteAgentTool / A2A Client 是 Orchestrator 侧远程 Agent 调用工具或客户端。Model Core 是状态中心。Web 是交互和展示入口。

长期标准调用链为：

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

Orchestrator 负责确定性流程编排，决定当前阶段调用哪个专业 Agent，传递阶段输入和 Workspace 摘要，并接收专业 Agent 返回的阶段 DTO 或标准失败结果。RemoteAgentTool / A2A Client 默认放在 `mac-tav-orchestrator` 中，作为 Orchestrator 调用远程专业 Agent 的客户端适配能力，只负责从 Nacos 查询 Agent Card、通过 A2A 调用远程专业 Agent、处理远程调用异常和协议适配，不承担业务编排职责，不写 Workspace，不管理任务状态。Orchestrator 仍负责 Workspace 写入、Artifact 版本管理、任务状态推进、阶段产物追溯、异常收敛、阶段重跑和修复闭环。

## 3. Intent Module

### 3.1 模块定位

Intent Module 负责把用户自然语言网络需求解析为业务意图模型。

该模块的核心产物是 `NetworkIntent`。它表达业务对象、业务关系、用户偏好、约束和假设，不表达具体网络实现。

### 3.2 输入

- 用户原始输入 `rawText`。
- 任务上下文。
- 可选的历史澄清信息。
- 可选的用户偏好或目标环境提示。

### 3.3 输出

- `NetworkIntent`
- 意图解析阶段执行记录。
- 意图假设和冲突提示。

### 3.4 核心处理内容

- 识别业务区域，例如办公区、访客区、服务器区、互联网。
- 识别业务访问关系，例如允许访问、禁止访问、隔离、互联网访问。
- 识别用户明确提出的约束和偏好。
- 生成稳定的 intent node / relation id。
- 保留无法确认的假设，供后续规划或用户澄清使用。
- 调用 IntentAgent，先获得模型结构化输出 `IntentResponseSchema`，再由 `IntentResponseParser` 转换为 `NetworkIntent`，最后通过 `IntentOutputValidator` 校验后交回 Orchestrator，由 Orchestrator / Model Core 写入 NetworkWorkspace。

### 3.5 不做什么

Intent Module 不生成：

- 设备。
- 接口。
- VLAN。
- IP 地址。
- 路由协议细节。
- ACL。
- CLI 命令。

### 3.6 上下游关系

上游：
- Orchestrator。
- RemoteAgentTool / A2A Client 传入的阶段请求。

下游：

- Planning Module。
- Model Core。
- 前端意图展示。

## 4. Planning Module

### 4.1 模块定位

Planning Module 负责把 `NetworkIntent` 转换为网络设计方案。

该模块的核心产物是 `NetworkPlan`。它可以包含拓扑、区域、地址规划、VLAN 规划、路由、安全策略、NAT 和目标执行环境。

### 4.2 输入

- `NetworkIntent`
- 任务上下文。
- 目标环境偏好。
- 可选的规划规则或模板。

### 4.3 输出

- `NetworkPlan`
- 规划阶段执行记录。
- 规划假设和约束。

### 4.4 核心处理内容

- 选择网络架构。
- 设计拓扑节点和链路。
- 规划区域和安全边界。
- 分配地址段和 VLAN。
- 生成路由、安全策略、NAT 等规划元素。
- 维护规划元素和意图关系之间的追溯关系。
- 调用 PlanningAgent 和规划工具，执行结构化输出解析与校验。

### 4.5 不做什么

Planning Module 不生成：

- 具体 CLI 命令。
- 设备配置文本。
- 执行脚本。
- 验证结论。

### 4.6 上下游关系

上游：

- Intent Module。
- Orchestrator。

下游：

- Configuration Module。
- Execution Module。
- Verification Module。
- Healing Module。
- Model Core。

## 5. Configuration Module

### 5.1 模块定位

Configuration Module 负责把 `NetworkPlan` 转换为结构化配置集合。

该模块的核心产物是 `ConfigSet`。配置必须按设备、配置块、命令、解释、回滚和追溯关系组织，不能只是一整段命令文本。

### 5.2 输入

- `NetworkPlan`
- 目标环境。
- 配置生成上下文。
- 可选 RAG 检索结果。
- 可选配置模板和命令知识库结果。

### 5.3 输出

- `ConfigSet`
- 配置生成阶段执行记录。
- 配置风险提示和回滚信息。

### 5.4 核心处理内容

- 按设备生成结构化配置。
- 按功能拆分 commandBlocks。
- 生成命令解释。
- 生成 rollbackCommands 或不可回滚说明。
- 记录 generationSources。
- 保留 traceRefs，连接意图、规划元素和配置块。
- 调用 ConfigurationAgent、RAG 工具和模板工具，执行解析与校验。

### 5.5 不做什么

Configuration Module 不做：

- 直接执行配置。
- 直接判断执行结果。
- 直接修改 NetworkWorkspace。
- 只返回一段无法解析的命令文本。

### 5.6 上下游关系

上游：

- Planning Module。
- Orchestrator。

下游：

- Execution Module。
- Verification Module。
- Healing Module。
- Model Core。
- 配置展示和下载接口。

## 6. Execution Module

### 6.1 模块定位

Execution Module 负责把 `NetworkPlan + ConfigSet` 转换为受控可执行内容，并采集执行结果。

该模块的核心产物是 `ExecutionReport`。它不是纯 LLM Agent，而是以 `ExecutionAdapter` 为核心的执行适配模块。

### 6.2 输入

- `NetworkPlan`
- `ConfigSet`
- 执行模式。
- 执行环境配置。
- 可选人工确认结果。

### 6.3 输出

- `ExecutionReport`
- 执行计划。
- 运行时状态。
- 测试结果。
- 错误和告警。

### 6.4 核心处理内容

- 根据 targetEnvironment 选择 ExecutionAdapter。
- 生成或准备 Mininet / Ryu / Docker / 自定义适配器内容；如执行环境暂不可用，可提供结构校验模式验证转换链路，但不得作为最终执行验收替代。
- 执行白名单内的命令或工具调用。
- 采集节点、链路、控制器、流表和测试结果。
- 将执行结果标准化为 `ExecutionReport`。
- 记录执行日志和错误摘要。

### 6.5 不做什么

Execution Module 不做：

- 直接执行 LLM 拼出来的任意 shell。
- 直接执行 Huawei CLI。
- 让 Controller 传入任意命令。
- 判断业务意图是否满足。
- 直接生成修复方案。

### 6.6 上下游关系

上游：

- Configuration Module。
- Orchestrator。
- 人工确认流程。

下游：

- Verification Module。
- Healing Module。
- Model Core。
- 前端执行状态展示。

## 7. Verification Module

### 7.1 模块定位

Verification Module 负责判断执行结果是否满足原始业务意图。

该模块的核心产物是 `ValidationReport`。规则工具负责提供可验证判断依据，LLM 负责总结、解释和组织结论。

### 7.2 输入

- `NetworkIntent`
- `NetworkPlan`
- `ConfigSet`
- `ExecutionReport`
- 验证规则和测试结果摘要。

### 7.3 输出

- `ValidationReport`
- 验证项。
- 验证证据。
- 失败建议。

### 7.4 核心处理内容

- 将意图关系映射到测试结果。
- 判断连通性、隔离性、安全策略、路由等是否达成。
- 生成 overallStatus。
- 为每个验证项记录 expected、actual、passed、severity。
- 维护验证项到 intent / plan / config / test 的追溯关系。
- 为失败场景提供 HealingAgent 可使用的证据。

### 7.5 不做什么

Verification Module 不做：

- 直接修改配置。
- 直接执行修复。
- 重新规划网络。
- 重新生成配置。

### 7.6 上下游关系

上游：

- Intent Module。
- Planning Module。
- Configuration Module。
- Execution Module。
- Orchestrator。

下游：

- Healing Module。
- Model Core。
- 前端验证展示。

## 8. Healing Module

### 8.1 模块定位

Healing Module 负责在验证失败时生成诊断和修复计划。

该模块的核心产物是 `RepairPlan`。HealingAgent 是长期标准 Agent 角色，但通常在 Intent、Planning、Configuration、Execution、Verification 稳定后实现。

### 8.2 输入

- `ValidationReport`
- `NetworkWorkspace`
- 失败上下文。
- 相关执行日志和验证证据。
- 可选用户确认信息。

### 8.3 输出

- `RepairPlan`
- `FailureAnalysis`
- `RepairAction`

### 8.4 核心处理内容

- 分析失败类型。
- 定位可能根因。
- 关联 validationItem、intent、plan、config、execution evidence。
- 生成修复动作，例如 REPLAN、REGENERATE_CONFIG、PATCH_CONFIG、REEXECUTE、ASK_USER、ROLLBACK。
- 标注风险等级和是否需要人工确认。
- 将修复建议交给 Orchestrator 决策。

### 8.5 不做什么

Healing Module 不做：

- 不绕过 Orchestrator 修改 `NetworkWorkspace`。
- 不直接执行修复命令。
- 不直接应用配置。
- 不猜测冲突意图的最终选择。

### 8.6 上下游关系

上游：

- Verification Module。
- Execution Module。
- Model Core。
- Orchestrator。

下游：

- Orchestrator。
- Planning Module。
- Configuration Module。
- Execution Module。
- Verification Module。
- Model Core。

## 9. Model Core

### 9.1 模块定位

Model Core 是任务状态和阶段产物中心。

它负责管理 `NetworkWorkspace`、版本、Artifact、执行记录、状态流转和追溯关系。

### 9.2 输入

- Orchestrator 写入的阶段产物。
- Agent 执行记录。
- Execution 执行记录。
- 用户确认或取消操作。

### 9.3 输出

- 当前 Workspace 视图。
- Artifact 历史版本。
- AgentExecutionRecord。
- 任务状态和阶段状态。
- 前端时间线数据。

### 9.4 核心处理内容

- 保存当前阶段产物。
- 保存历史 Artifact。
- 管理任务状态和阶段状态。
- 记录 Agent / Tool / MCP / Execution 摘要。
- 支撑阶段重跑、回放、回滚和自愈流程。

### 9.5 不做什么

Model Core 不做：

- 调用大模型。
- 生成 `NetworkPlan`。
- 生成 `ConfigSet`。
- 执行 Mininet / Ryu / Docker。
- 直接执行修复决策。

### 9.6 上下游关系

上游：

- Orchestrator。
- Web 查询服务。

下游：

- Orchestrator。
- Web / Visualization。
- Artifact 查询。
- Timeline 查询。

## 10. Orchestrator

### 10.1 模块定位

Orchestrator 是确定性工程流程控制模块。

它负责串联各阶段、处理异常、推进状态、写入 Workspace、管理 Artifact 版本和追溯关系，并根据验证和修复结果决定下一步。Orchestrator 不是大模型 Agent，不构造 Prompt，不直接调用大模型。

### 10.2 输入

- 创建任务请求。
- 用户继续、取消、重跑、修复确认请求。
- 当前 Workspace 状态。
- RemoteAgentTool / A2A Client 返回的远程调用结果，以及专业 Agent 返回的阶段 DTO 或标准失败结果。

### 10.3 输出

- 任务状态。
- 阶段推进结果。
- 写入 Model Core 的阶段产物。
- Artifact 版本和追溯关系。
- 面向 Web 的流程摘要。

### 10.4 核心处理内容

- 推进 Intent、Planning、Configuration、Execution、Verification、Healing 阶段。
- 通过 RemoteAgentTool / A2A Client 调用远程专业 Agent。
- 调用 Execution Module 完成受控执行适配。
- 在验证失败时进入 Healing 流程。
- 根据 RepairAction 重新进入指定阶段。
- 捕获异常并更新任务状态。
- 写入 NetworkWorkspace、AgentExecutionRecord 和 Artifact。
- 管理阶段产物版本、追溯关系、阶段重跑和修复闭环。

### 10.5 不做什么

Orchestrator 不做：

- 构造 Prompt。
- 直接调用 ChatModel。
- 直接调用 ReactAgent。
- 直接执行 shell。
- 直接修改 DTO 内部复杂字段来代替 Parser / Validator。
- 绕过 RemoteAgentTool / A2A Client 直接处理远程协议细节。

### 10.6 上下游关系

上游：

- Web Controller。
- 定时任务或外部集成入口。

下游：

- RemoteAgentTool / A2A Client。
- Execution Module。
- Model Core。
- SSE / Event 推送。****

## 11. Web

### 11.1 模块定位

Web / Visualization 是用户交互、任务控制和结果展示入口。

后端 Controller 负责 HTTP API。前端负责展示意图、拓扑、配置、执行、验证、修复和时间线。

### 11.2 输入

- 用户自然语言需求。
- 任务控制请求。
- 阶段产物查询请求。
- 修复动作确认请求。

### 11.3 输出

- 统一 API 响应。
- Workspace 当前视图。
- 阶段产物视图。
- SSE 进度事件。
- 前端展示数据。

### 11.4 核心处理内容

- Controller 接收请求和校验参数。
- 调用 Orchestrator 或查询服务。
- 返回统一响应。
- 前端展示完整闭环过程。
- 支持用户查看版本、追溯关系和修复建议。

### 11.5 不做什么

Web / Visualization 不做：

- 业务流程编排。
- 意图解析。
- 网络规划。
- 配置生成。
- 执行适配。
- 验证判断。
- 修复决策。
- 构造 Prompt。
- 直接调用模型。
- 直接执行 shell。

### 11.6 上下游关系

上游：

- 用户。
- 外部系统。

下游：

- Orchestrator。
- Model Core 查询服务。
- SSE / Event 服务。

## 12. 模块协作边界

长期协作规则：

- Controller 只调用 Orchestrator 或查询服务，不直接调用专业 Agent。
- Orchestrator 是唯一主编排入口。
- Orchestrator 通过 RemoteAgentTool / A2A Client 查询 Nacos、读取 Agent Card，并通过 A2A 调用专业 Agent。
- RemoteAgentTool / A2A Client 只负责远程发现、调用、异常转换和协议适配，不承担业务编排职责。
- 专业 Agent 只返回已解析、已校验的阶段 DTO 或标准失败结果。
- 专业 Agent 不直接写 NetworkWorkspace，不推进任务状态，不管理 Artifact 版本。
- Execution Module 通过 ExecutionAdapter 输出 ExecutionReport，不作为纯 LLM Agent。
- 所有阶段产物由 Orchestrator / Model Core 写入 NetworkWorkspace。
- 样例 JSON / 固定测试数据只用于前后端联调、Parser / Validator 离线测试、失败分支验证和回归测试，不作为真实业务主链路替身。

## 13. 模块间数据流

核心数据流：

```text
rawText
  -> NetworkIntent
  -> NetworkPlan
  -> ConfigSet
  -> ExecutionReport
  -> ValidationReport
  -> RepairPlan
```

每个产物都应具备：

- taskId。
- 版本号。
- 阶段状态。
- 可追溯 id。
- 可校验结构。
- 可写入 Artifact。

## 14. 本文档与其他文档的分工

- 本文档：业务模块定位、输入输出、核心处理内容、上下游关系。
- `docs/02_MAVEN_MODULES.md`：Maven 模块和依赖边界。
- `docs/04_DATA_MODELS.md`：DTO 字段、状态、版本、TraceRefs、Artifact。
- `docs/05_API_DESIGN.md`：HTTP API 契约和 Controller 边界。
- `docs/06_DEV_PLAN.md`：长期实现阶段和验收标准。
- `docs/09_AGENT_BUILD_GUIDE.md`：真实 Spring AI Alibaba Agent 构建规范。
