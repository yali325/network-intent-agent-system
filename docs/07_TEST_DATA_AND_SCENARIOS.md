# 测试场景与样例数据设计

## 1. 文档目标

本文档定义 MAC-TAV 长期测试场景、样例输入、期望输出摘要、失败场景和回归测试数据组织方式。

本文档只负责：

- 测试场景。
- 样例数据。
- 回归用例。
- 失败场景。
- Validator 非法输出场景。

本文档不负责：

- DTO 全字段定义。字段以 `docs/04_DATA_MODELS.md` 为准。
- API 路径。路径以 `docs/05_API_DESIGN.md` 为准。
- Agent 构建规范。规范以 `docs/09_AGENT_BUILD_GUIDE.md` 为准。
- 运行和测试命令。命令以 `docs/08_RUN_AND_TEST.md` 为准。
- Maven 依赖。依赖边界以 `docs/02_MAVEN_MODULES.md` 为准。

## 2. 样例数据使用原则

- 样例数据 MUST 可追溯。
- 样例数据 SHOULD 覆盖成功、失败、异常和修复。
- 样例数据不代表真实生产环境。
- 样例 JSON / 固定测试数据只用于前后端联调、Parser / Validator 离线测试、失败分支验证和回归测试。
- 本文档中的 Mock 仅指 Mock JSON / 固定测试数据。
- 本文档不要求生成 Mock Agent、Mock Tool、Fake Agent、Fake ReactAgent 或本地 Agent Bean 替身。
- 样例数据不得替代真实 Agent + Nacos + A2A 主链路验收。
- 样例数据只用于固定输入、固定期望输出、非法输出和失败场景回归。
- 不要把样例数据当成真实 Agent 输出的唯一模板。
- Markdown 中只保留摘要和关键片段。
- 大段 JSON SHOULD 放入 `docs/test-data/scenarios/`。

## 3. 场景目录设计

建议目录：

```text
docs/test-data/scenarios/{scenarioId}/
├── input.txt
├── expected-intent.json
├── expected-plan.json
├── expected-config.json
├── expected-execution.json
├── expected-validation.json
├── expected-repair.json
├── expected-artifacts.json
├── expected-workspace-events.json
├── expected-agent-execution-records.json
├── expected-trace-refs.json
└── README.md
```

说明：

- Markdown 只保留摘要和解释。
- JSON 用于 Parser / Validator / API / 前端联调回归。
- 不要求所有场景一开始都写完整 JSON，可按阶段逐步补齐。

建议先维护以下场景目录：

- `enterprise-office-guest-success`
- `guest-to-server-unexpected-pass`
- `routing-missing-failure`
- `acl-direction-error`
- `intent-conflict`
- `execution-environment-error`

### 3.1 A2A / Nacos 集成验证样例数据

这些不是 Mock Agent，而是服务化调用链的样例请求 / 响应数据。

建议样例文件：

- `agent-card-intent.sample.json`
- `a2a-intent-request.sample.json`
- `a2a-intent-response.sample.json`
- `agent-execution-record.sample.json`
- `workspace-event-agent-called.sample.json`

检查目标：

- Agent Card 包含 `agentName`、`capabilities`、`inputContract`、`outputContract`、`serviceEndpoint`、`version`、`healthStatus`。
- A2A 请求包含 `taskId`、`sourceAgent`、`targetAgent`、`stage`、`artifactVersion`、`payload`。
- A2A 响应能进入 `ResponseSchema -> Parser -> DTO -> Validator`。
- 远程调用摘要能进入 `AgentExecutionRecord` / `WorkspaceEvent`。
- A2A 失败能映射为 `A2A_CALL_FAILED` 或 `AGENT_CARD_NOT_FOUND` 等错误码。

## 4. 固定主场景：办公区 / 访客区 / 服务器区访问控制

### 4.1 场景标识

- scenarioId：`enterprise-office-guest-success`
- 期望结果：完整闭环通过。
- 期望 `ValidationReport.overallStatus`：`PASSED`

### 4.2 输入摘要

```text
构建一个办公区和访客区隔离的网络，
办公区可以访问服务器，访客区不能访问服务器，办公区与访客区互相隔离，采用 OSPF。
可选扩展：办公区和访客区都能访问互联网。
```

### 4.3 涉及对象

- office
- guest
- server
- internet，可选扩展

### 4.4 核心意图关系

- office -> server：allow
- guest -> server：deny
- office <-> guest：deny
- office -> internet：allow，可选
- guest -> internet：allow，可选

### 4.5 期望 NetworkIntent 摘要

- 包含 office、guest、server 等业务对象，可包含 internet 扩展对象。
- 包含允许和禁止访问关系。
- 不包含设备、接口、VLAN、IP、CLI。
- relation id 稳定，可被规划、配置、验证引用。

### 4.6 期望 NetworkPlan 摘要

- 包含办公区、访客区、服务器区，可按扩展需要包含公网出口。
- 包含拓扑、地址规划、VLAN 规划、OSPF、ACL / 安全策略规划。
- 不包含具体 CLI。
- 规划元素可追溯到 intent relation。

### 4.7 期望 ConfigSet 摘要

- 按设备组织配置。
- 包含 commandBlocks。
- 每个 commandBlock 包含 explanation、traceRefs、rollbackCommands。
- generationSources 可包含 LLM、RAG、TEMPLATE、RULE、TOOL。

### 4.8 期望 ExecutionReport 摘要

- 长期主验收 `executionMode` SHOULD 为 `MININET_RYU`。如 Mininet / Ryu 暂不可用，可使用结构校验模式生成样例 `ExecutionReport`，但不得作为最终执行验收替代。
- 包含 executionPlan、runtimeState、testResult。
- 测试结果覆盖 office/server、guest/server、office/guest，可扩展覆盖 office/internet、guest/internet。

### 4.9 期望 ValidationReport 摘要

- overallStatus = `PASSED`
- 每个核心意图关系都有对应 ValidationItem。
- 每个 ValidationItem 关联 intent relation、plan element、config block 和 test。

## 5. 访问控制失败场景

### 5.1 场景标识

- scenarioId：`guest-to-server-unexpected-pass`
- 期望结果：验证失败并进入修复分析。
- 期望 `ValidationReport.overallStatus`：`FAILED`

### 5.2 失败现象

- `guest-pc-1 -> server-1` 实际 `REACHABLE`
- 期望结果为 `BLOCKED`

### 5.3 追溯关系

- 关联意图关系：`rel-002`
- 关联规划元素：`policy-001`
- 关联配置块：`R1-ACL-001`

### 5.4 Healing 期望

HealingAgent SHOULD 生成 `RepairPlan`，建议检查：

- ACL 方向。
- ACL 绑定接口。
- 策略优先级。
- 执行适配是否生效。
- 测试命令是否验证了正确路径。

## 6. 路由失败场景

### 6.1 场景标识

- scenarioId：`routing-missing-failure`
- 期望结果：验证失败并生成修复计划。

### 6.2 失败现象

- office -> server 不可达。
- guest -> internet 或 office -> internet 可能也受影响。

### 6.3 可能原因

- OSPF 宣告缺失。
- 默认路由缺失。
- 网关配置错误。
- 路由进程未启用。

### 6.4 期望结果

- VerificationAgent 输出 FAILED。
- HealingAgent 输出 `REGENERATE_CONFIG` 或 `REPLAN` 动作。
- RepairAction 关联失败 ValidationItem 和相关 plan/config 元素。

## 7. ACL 方向错误场景

### 7.1 场景标识

- scenarioId：`acl-direction-error`
- 期望结果：验证失败，修复动作指向配置补丁。

### 7.2 失败现象

- 隔离策略未生效。
- 不该访问的流量被放行。

### 7.3 可能原因

- ACL 绑定在错误接口。
- ACL 方向配置错误。
- ACL 顺序或优先级错误。
- 策略应用对象与规划区域不一致。

### 7.4 期望结果

- ValidationItem 关联 securityPolicyPlan 和 commandBlock。
- HealingAgent 输出 `RepairAction = PATCH_CONFIG`。
- RepairAction 不直接执行修复，由 Orchestrator / ExecutionAdapter 控制。

## 8. 意图冲突场景

### 8.1 场景标识

- scenarioId：`intent-conflict`
- 期望结果：进入澄清或等待用户确认。

### 8.2 输入示例

```text
访客区不能访问服务器，同时访客区必须访问服务器业务系统。
```

### 8.3 期望结果

- IntentAgent 或 PlanningAgent 识别冲突。
- task 可进入 `WAITING_USER`。
- 系统生成澄清问题或冲突说明。
- Healing / Orchestrator 不直接猜测用户真实意图。

## 9. 执行环境失败场景

### 9.1 场景标识

- scenarioId：`execution-environment-error`
- 期望结果：执行失败被结构化记录。

### 9.2 失败现象

- Mininet 启动失败。
- Ryu 未连接。
- Docker 网络异常。
- 测试命令超时。

### 9.3 期望结果

- `ExecutionReport.stageStatus = FAILED`
- 错误归类为 `EXECUTION_ENV_ERROR`
- RepairPlan 或建议指向 ExecutionAdapter / 环境检查。
- 失败信息写入 Workspace，而不是只打印在控制台。

## 10. Agent 输出非法字段场景

这些场景用于测试 Validator。

### 10.1 IntentAgent 越界输出

非法输出：

- VLAN。
- IP。
- 接口。
- 设备。
- CLI。

期望结果：

- IntentOutputValidator 拒绝。
- 错误归类为 `AGENT_OUTPUT_INVALID`。

### 10.2 PlanningAgent 越界输出

非法输出：

- CLI 命令。
- 设备完整配置文本。

期望结果：

- PlanningOutputValidator 拒绝。

### 10.3 ConfigurationAgent 非结构化输出

非法输出：

- 只输出一整段字符串。
- 没有 commandBlocks。
- 没有 traceRefs。
- 没有 rollbackCommands 或回滚说明。

期望结果：

- ConfigurationOutputValidator 拒绝。

### 10.4 VerificationAgent 越权输出

非法行为：

- 直接修改配置。
- 直接生成修复命令并要求执行。

期望结果：

- VerificationOutputValidator 拒绝。
- 修复建议必须进入 HealingAgent / RepairPlan。

### 10.5 HealingAgent 越权输出

非法行为：

- 直接声明已修改 Workspace。
- 直接声明已执行修复命令。
- 输出未审批的高风险修复动作并要求立即执行。
- 输出任意 shell。

期望结果：

- HealingOutputValidator 拒绝。
- 错误归类为 `AGENT_OUTPUT_INVALID`。
- 修复动作必须交给 Orchestrator / ExecutionAdapter 控制。

### 10.6 A2A 返回结构不合法

非法行为：

- 缺 `taskId`。
- 缺 `stage`。
- `payload` 不是目标 `ResponseSchema`。
- `targetAgentName` 与 Agent Card 不匹配。

期望结果：

- 不写入 Workspace。
- 错误归类为 `AGENT_SCHEMA_INVALID` 或 `A2A_CALL_FAILED`。
- Orchestrator 记录失败摘要。

## 11. 回归测试数据要求

每个场景 SHOULD 包含：

- 稳定 `scenarioId`。
- input。
- expectedArtifacts 摘要。
- expectedStatus。
- expectedTraceRefs。
- expectedVersions。
- expectedCurrentArtifactRefs。
- expectedWorkspaceEvents。
- expectedAgentExecutionRecords。
- expectedA2aCallSummary，适用于 A2A 集成验证。
- expectedApprovalStatus，适用于人工确认和修复场景。
- expectedErrorCode，适用于失败场景。
- expectedRepairAction，适用于修复场景。

不要求每个场景都写完整 JSON。完整 JSON 后续 SHOULD 放在 `docs/test-data/scenarios/` 下。

## 12. 本文档与其他文档的分工

- 本文档：测试场景与样例数据。
- `docs/04_DATA_MODELS.md`：DTO 字段。
- `docs/05_API_DESIGN.md`：API 路径。
- `docs/08_RUN_AND_TEST.md`：运行和测试命令。
- `docs/09_AGENT_BUILD_GUIDE.md`：Agent 构建规范。
- `docs/02_MAVEN_MODULES.md`：Maven 依赖边界。
