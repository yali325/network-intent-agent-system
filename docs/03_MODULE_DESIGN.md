# 系统模块设计

## 1. 文档目标

本文档定义 MAC-TAV 各业务模块的定位、输入输出、核心职责和约束。本文档不展开 DTO 字段、API 路径、Maven 依赖、Agent 初始化代码——对应内容见：

- DTO 字段：`docs/04_DATA_MODELS.md`
- API 契约：`docs/05_API_DESIGN.md`
- Maven 边界：`docs/02_MAVEN_MODULES.md`
- Agent 构建：`docs/09_AGENT_BUILD_GUIDE.md`

## 2. 总体业务链路与标准调用链

```text
用户输入
  -> IntentAgent -> NetworkIntent
  -> PlanningAgent -> NetworkPlan
  -> ConfigurationAgent -> ConfigSet
  -> ExecutionAdapter -> ExecutionReport
  -> VerificationAgent -> ValidationReport
  -> HealingAgent -> RepairPlan -> 重新规划/配置/执行/验证
```

长期标准调用链（Orchestrator 主编排，通过官方 SAA A2A starter 发现并调用远程 Agent）：

```text
Controller / API -> Orchestrator
  -> A2aRemoteAgent / AgentCardProvider（官方 spring-ai-alibaba-starter-a2a-nacos）
  -> Nacos Discovery -> Agent Card -> 专业 Agent A2A Service
  -> XxxAgent -> ResponseSchema -> Parser -> DTO -> Validator
  -> Orchestrator -> Model Core / NetworkWorkspace / Artifact
```

参考：<https://java2ai.com/docs/frameworks/agent-framework/advanced/a2a>

- **Orchestrator**：唯一主编排入口，确定性流程控制，不构造 Prompt，不直接调用大模型。
- **A2aRemoteAgent / AgentCardProvider**：通过官方 SAA A2A starter 完成 Nacos 发现与 A2A 远程调用，不手写 HTTP Client 或 Nacos 注册代码。
- **专业 Agent**：独立 Spring Boot 服务，通过 `XxxAgentConfiguration` 注册 `ReactAgent` Bean，通过 `application.yml` + starter 自动完成 A2A Server / Agent Card 发布 / Nacos 注册。
- **Model Core**：状态中心，管理 Workspace、版本、日志、追溯关系，不调用大模型。

---

## 3. Intent Module

**定位**：将自然语言需求解析为 `NetworkIntent`（业务对象、访问关系、约束、假设），不表达具体网络实现。

**核心职责**：
- 识别业务区域（办公区、访客区、服务器区等）和访问关系（允许、禁止、隔离）
- 识别用户约束和偏好，保留无法确认的假设供后续澄清
- 调用 IntentAgent：`IntentResponseSchema` → Parser → `NetworkIntent` → Validator → 交回 Orchestrator

**不做什么**：不生成设备、接口、VLAN、IP、路由协议、ACL、CLI。

---

## 4. Planning Module

**定位**：将 `NetworkIntent` 转换为 `NetworkPlan`（拓扑、区域、地址规划、VLAN、路由、安全策略、NAT、目标环境）。

**核心职责**：
- 设计拓扑节点和链路，规划区域和安全边界
- 分配地址段和 VLAN，生成路由、安全策略、NAT 等规划元素
- 维护规划元素到意图关系的追溯
- 调用 PlanningAgent 和规划工具，执行结构化输出解析与校验

**不做什么**：不生成 CLI、设备配置文本、执行脚本、验证结论。

---

## 5. Configuration Module

**定位**：将 `NetworkPlan` 转换为结构化 `ConfigSet`（按设备分块，每块含命令、解释、追溯、回滚）。

**核心职责**：
- 将规划元素映射为设备配置块
- 为每个 commandBlock 生成 `blockId`、`commands`、`explanation`、`traceRefs`、`rollbackCommands`
- 可在对应阶段明确要求时接入 RAG、模板和命令知识库增强生成；未被当前任务要求时，不提前生成 RAG / VectorDB / Template / MCP 空壳。

**不做什么**：不返回一整段命令文本，不绕过 ExecutionAdapter 执行配置。

---

## 6. Execution Module

**定位**：以 `ExecutionAdapter` 为核心，将 `NetworkPlan + ConfigSet` 转为受控可执行内容，输出 `ExecutionReport`。不是纯 LLM Agent。

**核心职责**：
- 根据 `targetEnvironment` 选择 ExecutionAdapter（Mininet / Ryu / Docker / 自定义）
- 通过 adapter 白名单执行命令或工具调用，采集执行结果
- 将结果标准化为 `ExecutionReport`，记录执行日志和错误摘要
- 如 Mininet / Ryu 暂不可用，可提供结构校验模式验证转换链路，但不得作为最终执行验收替代

**不做什么**：不执行 LLM 拼出的任意 shell，不执行 Huawei CLI，不让 Controller 传入任意命令。

---

## 7. Verification Module

**定位**：判断执行结果是否满足原始业务意图，输出 `ValidationReport`。

**核心职责**：
- 将意图关系映射到测试结果，判断连通性、隔离性、安全策略、路由等是否达成
- 为每个验证项记录 `expected`、`actual`、`passed`、`severity`
- 维护验证项到 intent / plan / config / test 的追溯
- 为失败场景提供 HealingAgent 可使用的证据

**不做什么**：不修改配置，不执行修复，不重新规划或生成配置。

---

## 8. Healing Module

**定位**：验证失败时生成 `RepairPlan`（诊断 + 修复动作）。通常在 Intent、Planning、Configuration、Execution、Verification 稳定后实现。

**核心职责**：
- 读取 `ValidationReport`、失败上下文、Workspace 和追溯信息
- 生成 `RepairAction`（`REPLAN`、`REGENERATE_CONFIG`、`PATCH_CONFIG`、`REEXECUTE`、`ASK_USER`、`ROLLBACK`）
- 每个修复动作含 `actionId`、`actionType`、`reason`、`riskLevel`

**不做什么**：不直接修改 Workspace，不直接执行修复命令，不绕过 Orchestrator。

---

## 9. Model Core

**定位**：状态中心，管理 NetworkWorkspace、Artifact 版本、任务状态、执行日志、追溯关系和修复历史。

**核心职责**：
- 保存任务状态和所有阶段产物版本
- 记录 Agent 执行摘要和 Workspace 变更
- 提供阶段产物查询和追溯能力

**不做什么**：不调用大模型，不生成 `NetworkPlan`、`ConfigSet`，不执行 Mininet / Ryu / Docker。

---

## 10. Orchestrator

**定位**：确定性工程流程控制模块。不是大模型 Agent，不构造 Prompt，不直接调用大模型。

**核心职责**：
- 推进 Intent → Planning → Configuration → Execution → Verification → Healing 阶段
- 通过 `A2aRemoteAgent / AgentCardProvider`（官方 SAA A2A starter）调用远程专业 Agent
- 调用 Execution Module 完成受控执行适配
- 验证失败时进入 Healing 流程，根据 `RepairAction` 重新进入指定阶段
- 写入 NetworkWorkspace、AgentExecutionRecord 和 Artifact，管理版本和追溯

**不做什么**：不构造 Prompt，不直接调用 `ChatModel` / `ReactAgent`，不直接执行 shell，不绕过 Parser/Validator 修改 DTO。

---

## 11. Web

**定位**：用户交互、任务控制和结果展示入口。后端 Controller 接收 HTTP 请求，前端展示完整闭环过程。

**核心职责**：
- Controller 接收请求、校验参数、调用 Orchestrator 或查询服务
- 返回统一 API 响应、Workspace 视图、阶段产物、SSE 进度
- 前端展示意图、拓扑、配置块、执行状态、验证报告、失败原因、修复建议和 Agent 执行轨迹

**不做什么**：不做业务流程编排、意图解析、网络规划、配置生成、执行适配、验证判断、修复决策。不构造 Prompt，不直接调用模型。

---

## 12. 模块协作边界（核心规则）

1. Controller 只调用 Orchestrator 或查询服务，不直接调用专业 Agent。
2. Orchestrator 是唯一主编排入口，通过 `A2aRemoteAgent / AgentCardProvider`（官方 SAA A2A starter）调用远程 Agent。
3. 专业 Agent 只返回已解析、已校验的阶段 DTO，不直接写 NetworkWorkspace，不推进任务状态。
4. Execution Module 通过 ExecutionAdapter 输出 ExecutionReport，不作为纯 LLM Agent。
5. 所有阶段产物由 Orchestrator / Model Core 写入 NetworkWorkspace。
6. 样例 JSON / 固定测试数据只用于 Parser/Validator 离线测试和回归测试，不作为业务主链路替身。

## 13. 模块间数据流

```text
rawText -> NetworkIntent -> NetworkPlan -> ConfigSet -> ExecutionReport -> ValidationReport -> RepairPlan
```

每个产物都应具备：`taskId`、版本号、阶段状态、可追溯 `id`、可校验结构、可写入 Artifact。