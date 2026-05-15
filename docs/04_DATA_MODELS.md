# 数据模型设计

## 1. 文档目标

本文档定义 MAC-TAV 长期核心 DTO 和跨模块数据契约。

约定：

1. 所有共享 DTO 应放在 `mac-tav-model`。
2. 公共枚举、异常、统一响应应放在 `mac-tav-common`。
3. 本文档只描述字段和数据关系，不描述 Agent 代码实现细节。
4. 每个阶段产物必须可序列化、可追踪、可校验、可进入 `NetworkWorkspace`。
5. 模型字段应服务于长期闭环：意图解析、网络规划、配置生成、执行适配、验证评估、异常诊断与修复。

本文档不负责 Maven 模块拆分、API 路径、前端页面、数据库表结构或 Spring AI Alibaba Agent 初始化方式。

## 2. 通用数据建模约定

### 2.1 ID 约定

所有核心对象必须有稳定 `id`。

ID 约束：

1. `id` 不应依赖前端展示名称。
2. `id` 应便于 `traceRefs`、`ValidationItem`、`RepairAction` 引用。
3. 同一任务内同类对象的 `id` 应唯一。
4. 可被其他阶段引用的元素不得只依赖数组下标。

至少包括：

| ID | 说明 |
| --- | --- |
| `taskId` | 一次完整网络意图任务 |
| `intentRelationId` | 业务意图关系 |
| `planElementId` | 可被配置、验证、修复引用的规划元素 |
| `topologyNodeId` | 拓扑节点 |
| `topologyLinkId` | 拓扑链路 |
| `commandBlockId` | 配置块 |
| `testId` | 执行测试 |
| `validationItemId` | 验证项 |
| `repairActionId` | 修复动作 |
| `artifactId` | 阶段产物记录 |

### 2.2 版本约定

阶段产物保留独立版本字段：

| 阶段产物 | 版本字段 |
| --- | --- |
| `NetworkIntent` | `intentVersion` |
| `NetworkPlan` | `planVersion` |
| `ConfigSet` | `configVersion` |
| `ExecutionReport` | `executionVersion` |
| `ValidationReport` | `validationVersion` |
| `RepairPlan` | `repairVersion` |

版本约束：

1. 当前版本用于当前主流程。
2. 历史版本用于重试、自愈、回滚和过程回放。
3. 新版本不应覆盖旧版本，应通过 `NetworkArtifact` 或等价机制保留引用。
4. `NetworkWorkspace` 必须记录每类产物的当前版本。

### 2.3 时间字段约定

长期模型建议统一使用以下时间字段：

| 字段 | 说明 |
| --- | --- |
| `createdAt` | 对象创建时间 |
| `updatedAt` | 对象最后更新时间 |
| `startedAt` | 阶段或执行开始时间 |
| `finishedAt` | 阶段或执行结束时间 |

字段类型可以使用 `String` 或 `LocalDateTime`，最终以代码实现统一。

### 2.4 状态枚举约定

状态必须拆分，不要混用任务状态、流程阶段、阶段执行状态、产物状态、验证结论和修复状态。

#### `TaskStatus`

表示整个任务生命周期：

```text
CREATED
RUNNING
WAITING_USER
COMPLETED
FAILED
ERROR
CANCELLED
```

#### `WorkflowStage`

表示当前流程阶段：

```text
INTENT
PLANNING
CONFIGURATION
EXECUTION
VERIFICATION
HEALING
FINISHED
```

#### `StageStatus`

表示某个阶段执行状态：

```text
PENDING
RUNNING
SUCCESS
FAILED
SKIPPED
```

#### `ArtifactStatus`

表示阶段产物状态：

```text
DRAFT
GENERATED
VALIDATED
APPLIED
SUPERSEDED
ROLLED_BACK
```

#### `ValidationStatus`

表示验证结论：

```text
PASSED
FAILED
PARTIAL
UNKNOWN
```

#### `RepairStatus`

表示修复动作状态：

```text
PROPOSED
APPROVED
APPLIED
REJECTED
FAILED
```

### 2.5 追溯关系约定

`TraceRefs` 用于连接用户意图、规划、配置、执行、验证和修复之间的关系。

`TraceRefs` 连接：

1. 用户意图关系。
2. 网络规划元素。
3. 配置块。
4. 执行测试。
5. 验证项。
6. 修复动作。

长期字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `intentNodeIds` | `List<String>` | 关联的意图节点 |
| `intentRelationIds` | `List<String>` | 关联的意图关系 |
| `planElementIds` | `List<String>` | 关联的规划元素 |
| `configBlockIds` | `List<String>` | 关联的配置块 |
| `testIds` | `List<String>` | 关联的执行测试 |
| `validationItemIds` | `List<String>` | 关联的验证项 |
| `repairActionIds` | `List<String>` | 关联的修复动作 |

要求：

1. `ConfigSet.commandBlocks` 必须能追溯到 `NetworkPlan` / `NetworkIntent`。
2. `ValidationItem` 必须能追溯到 intent / plan / config / test。
3. `RepairAction` 必须能追溯到 `validationItem` 和相关 plan / config 元素。
4. 不要求 `TraceRefs` 的所有列表都必填，但关键阶段产物必须至少具备一条可解释来源。

## 3. Task 与 Workspace

### 3.1 `NetworkTask`

`NetworkTask` 表示一次用户提交的网络意图任务。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | `String` | 任务 ID |
| `rawText` | `String` | 用户原始输入 |
| `taskStatus` | `TaskStatus` | 任务总状态 |
| `currentStage` | `WorkflowStage` | 当前流程阶段 |
| `createdAt` | `String / LocalDateTime` | 创建时间 |
| `updatedAt` | `String / LocalDateTime` | 更新时间 |
| `createdBy` | `String` | 创建人，可选 |
| `source` | `String` | 任务来源，可选，例如 `WEB` / `API` / `SCHEDULED` |
| `description` | `String` | 任务描述，可选 |

### 3.2 `NetworkWorkspace`

`NetworkWorkspace` 是任务状态中心，应支持“当前产物 + 版本历史 / Artifact 引用”。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `task` | `NetworkTask` | 任务基本信息 |
| `currentIntentVersion` | `Integer` | 当前意图版本 |
| `currentPlanVersion` | `Integer` | 当前规划版本 |
| `currentConfigVersion` | `Integer` | 当前配置版本 |
| `currentExecutionVersion` | `Integer` | 当前执行版本 |
| `currentValidationVersion` | `Integer` | 当前验证版本 |
| `currentRepairVersion` | `Integer` | 当前修复版本 |
| `currentIntent` | `NetworkIntent` | 当前意图产物 |
| `currentPlan` | `NetworkPlan` | 当前规划产物 |
| `currentConfigSet` | `ConfigSet` | 当前配置产物 |
| `currentExecutionReport` | `ExecutionReport` | 当前执行产物 |
| `currentValidationReport` | `ValidationReport` | 当前验证产物 |
| `currentRepairPlan` | `RepairPlan` | 当前修复计划 |
| `artifacts` | `List<NetworkArtifact>` | 全部阶段产物引用或快照 |
| `agentExecutionRecords` | `List<AgentExecutionRecord>` | Agent / 模块执行记录 |
| `changeHistory` | `List<WorkspaceChangeRecord>` | 重试、修复、人工确认等变化记录 |
| `workspaceStatus` | `TaskStatus / String` | Workspace 当前状态 |

说明：

1. `currentXxx` 用于前端快速展示当前结果。
2. `artifacts` 用于保存所有版本产物。
3. `agentExecutionRecords` 用于记录每个 Agent 的执行过程。
4. `changeHistory` 用于记录自愈、重试、人工确认等变化。

### 3.3 `NetworkArtifact`

`NetworkArtifact` 用于保存阶段产物引用或快照。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `artifactId` | `String` | 产物 ID |
| `taskId` | `String` | 任务 ID |
| `artifactType` | `ArtifactType` | 产物类型 |
| `version` | `Integer` | 产物版本 |
| `stage` | `WorkflowStage` | 所属阶段 |
| `status` | `ArtifactStatus` | 产物状态 |
| `payloadType` | `String` | 产物载荷类型，例如 DTO 类名或逻辑类型 |
| `payload` | `Object / JsonNode / String` | 产物快照或引用 |
| `summary` | `String` | 产物摘要 |
| `createdAt` | `String / LocalDateTime` | 创建时间 |
| `createdBy` | `String` | 创建来源，例如 Agent、用户或系统 |
| `traceRefs` | `TraceRefs` | 追溯关系 |

`ArtifactType` 至少包括：

```text
NETWORK_INTENT
NETWORK_PLAN
CONFIG_SET
EXECUTION_REPORT
VALIDATION_REPORT
REPAIR_PLAN
```

### 3.4 `AgentExecutionRecord`

`AgentExecutionRecord` 用于记录 Agent 或关键模块的执行过程。`AgentStepLog` 可以作为前端展示版本，但长期状态记录应以结构化执行记录为准。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `recordId` | `String` | 执行记录 ID |
| `taskId` | `String` | 任务 ID |
| `agentName` | `String` | Agent 或模块名称 |
| `stage` | `WorkflowStage` | 所属阶段 |
| `stageStatus` | `StageStatus` | 阶段执行状态 |
| `inputArtifactIds` | `List<String>` | 输入产物 ID |
| `outputArtifactIds` | `List<String>` | 输出产物 ID |
| `toolCallSummaries` | `List<String>` | Tool 调用摘要 |
| `mcpCallSummaries` | `List<String>` | MCP 调用摘要 |
| `modelCallCount` | `Integer` | 模型调用次数 |
| `startedAt` | `String / LocalDateTime` | 开始时间 |
| `finishedAt` | `String / LocalDateTime` | 结束时间 |
| `errorCode` | `String` | 错误码，可选 |
| `errorMessage` | `String` | 错误信息，可选 |
| `message` | `String` | 展示文本或摘要 |

安全约束：

1. 不保存 API Key。
2. 不保存完整敏感请求头。
3. raw model output 如需保存，应脱敏并可配置。

### 3.5 `WorkspaceChangeRecord`

`WorkspaceChangeRecord` 用于记录产物切换、重试、修复、人工确认等状态变化。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `changeId` | `String` | 变化记录 ID |
| `taskId` | `String` | 任务 ID |
| `stage` | `WorkflowStage` | 相关阶段 |
| `changeType` | `String` | 变化类型，例如 `VERSION_SWITCH`、`RETRY`、`REPAIR_APPLIED` |
| `fromArtifactId` | `String` | 原产物 ID，可选 |
| `toArtifactId` | `String` | 新产物 ID，可选 |
| `reason` | `String` | 变化原因 |
| `createdAt` | `String / LocalDateTime` | 创建时间 |
| `createdBy` | `String` | 创建来源 |

## 4. `NetworkIntent`

`NetworkIntent` 是 `IntentAgent` 输出的业务意图模型。

必须强调：

1. `NetworkIntent` 不包含具体设备、接口、VLAN、IP、CLI。
2. `NetworkIntent` 只表达业务对象、业务关系、用户偏好、约束和假设。

字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | `String` | 任务 ID |
| `intentVersion` | `Integer` | 意图版本 |
| `rawText` | `String` | 用户原始输入 |
| `semanticIntentGraph` | `SemanticIntentGraph` | 语义意图图 |
| `assumptions` | `List<Assumption>` | 系统假设 |
| `constraints` | `List<IntentConstraint>` | 用户约束或推导约束 |
| `preferences` | `List<IntentPreference>` | 用户偏好，例如协议、风格、目标环境倾向 |
| `stageStatus` | `StageStatus` | 阶段状态 |
| `traceId` | `String` | 调用链追踪 ID |
| `createdAt` | `String / LocalDateTime` | 创建时间 |

### 4.1 `SemanticIntentGraph`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nodes` | `List<IntentNode>` | 业务对象 |
| `relations` | `List<IntentRelation>` | 业务关系 |

### 4.2 `IntentNode`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `String` | 节点 ID |
| `name` | `String` | 节点名称 |
| `type` | `String` | 节点类型 |
| `description` | `String` | 描述 |
| `attributes` | `Map<String, Object>` | 扩展属性，可选 |

`type` 可包含：

```text
ZONE
USER_GROUP
SERVER_GROUP
EXTERNAL_NETWORK
SERVICE
APPLICATION
```

### 4.3 `IntentRelation`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `String` | 关系 ID |
| `type` | `String` | 关系类型 |
| `source` | `String` | 源节点 ID |
| `target` | `String` | 目标节点 ID |
| `action` | `String` | `ALLOW` / `DENY` / `REQUIRE` 等 |
| `service` | `String` | 服务或协议范围 |
| `priority` | `Integer` | 优先级，可选 |
| `explicit` | `Boolean` | 是否由用户明确提出 |
| `description` | `String` | 描述 |
| `constraints` | `List<IntentConstraint>` | 关系级约束，可选 |

`type` 可包含：

```text
ACCESS
ISOLATION
INTERNET_ACCESS
SERVICE_ACCESS
ROUTING_REQUIREMENT
```

### 4.4 `Assumption`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `String` | 假设 ID |
| `field` | `String` | 假设涉及字段 |
| `value` | `String` | 假设值 |
| `reason` | `String` | 假设原因 |
| `confidence` | `Double` | 置信度，可选 |

### 4.5 `IntentConstraint`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `String` | 约束 ID |
| `type` | `String` | 约束类型，例如 `PROTOCOL`、`SECURITY`、`PERFORMANCE` |
| `value` | `String` | 约束值 |
| `description` | `String` | 约束说明 |

### 4.6 `IntentPreference`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `String` | 偏好 ID |
| `type` | `String` | 偏好类型 |
| `value` | `String` | 偏好值 |
| `priority` | `Integer` | 偏好优先级，可选 |

## 5. `NetworkPlan`

`NetworkPlan` 是 `PlanningAgent` 输出的网络设计方案。

`NetworkPlan` 可以包含拓扑、区域、地址、VLAN、路由、安全策略、NAT、`targetEnvironment`。

`NetworkPlan` 不包含具体 CLI 命令。

字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | `String` | 任务 ID |
| `intentVersion` | `Integer` | 来源意图版本 |
| `planVersion` | `Integer` | 规划版本 |
| `planSummary` | `String` | 规划摘要 |
| `selectedArchitecture` | `SelectedArchitecture` | 选定架构 |
| `targetEnvironment` | `TargetEnvironment` | 目标环境 |
| `topology` | `Topology` | 网络拓扑 |
| `zones` | `List<NetworkZone>` | 网络区域 |
| `addressPlan` | `List<AddressPlanItem>` | 地址规划 |
| `vlanPlan` | `List<VlanPlanItem>` | VLAN 规划 |
| `routingPlan` | `RoutingPlan` | 路由规划 |
| `securityPolicyPlan` | `List<SecurityPolicyPlanItem>` | 安全策略规划 |
| `natPlan` | `NatPlan` | NAT 规划 |
| `planConstraints` | `List<PlanConstraint>` | 规划约束 |
| `traceRefs` | `TraceRefs` | 追溯关系 |
| `stageStatus` | `StageStatus` | 阶段状态 |
| `createdAt` | `String / LocalDateTime` | 创建时间 |

要求：

1. 所有可被 `ConfigSet` / `ValidationReport` / `RepairPlan` 引用的规划元素都必须有 `id`。
2. 地址规划可提供 `exampleHostAddress` 或 `hostAddressHints`，用于示例或候选主机地址。
3. `NetworkPlan` 不输出配置命令。

### 5.1 `SelectedArchitecture`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `String` | 架构元素 ID，可选 |
| `type` | `String` | 架构类型，例如 `ROUTER_ON_A_STICK`、`L3_SWITCH_CORE`、`SDN_OPENFLOW` |
| `reason` | `String` | 选择理由 |
| `tradeoffs` | `List<String>` | 取舍说明，可选 |

### 5.2 `TargetEnvironment`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `vendor` | `String` | 设备风格，例如 `Huawei`、`GenericLinux` |
| `configStyle` | `String` | 配置风格，例如 `CLI`、`OVS_FLOW`、`RYU_FLOW` |
| `simulationTarget` | `String` | 执行目标 |
| `adapterType` | `String` | 适配器类型，可选 |

`simulationTarget` 应支持：

```text
DRY_RUN
MININET_RYU
REAL_DEVICE
CUSTOM_ADAPTER
```

### 5.3 `Topology`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nodes` | `List<TopologyNode>` | 设备、主机、外部网络 |
| `links` | `List<TopologyLink>` | 链路 |

### 5.4 `TopologyNode`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `String` | 节点 ID |
| `name` | `String` | 节点名称 |
| `nodeType` | `String` | `DEVICE` / `HOST` / `EXTERNAL_NETWORK` |
| `deviceType` | `String` | `ROUTER` / `SWITCH` 等，可选 |
| `hostType` | `String` | `PC` / `SERVER` 等，可选 |
| `role` | `String` | `GATEWAY` / `ACCESS` / `CORE` 等 |
| `vendor` | `String` | 厂商或设备风格，可选 |
| `zoneId` | `String` | 所属区域，可选 |
| `traceRefs` | `TraceRefs` | 追溯关系，可选 |

### 5.5 `TopologyLink`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `String` | 链路 ID |
| `sourceNode` | `String` | 源节点 ID |
| `sourceInterface` | `String` | 源接口 |
| `targetNode` | `String` | 目标节点 ID |
| `targetInterface` | `String` | 目标接口 |
| `linkType` | `String` | `ACCESS` / `TRUNK` / `WAN` / `VIRTUAL` |
| `traceRefs` | `TraceRefs` | 追溯关系，可选 |

### 5.6 `NetworkZone`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `String` | 区域 ID |
| `name` | `String` | 区域名称 |
| `mappedFromIntentNode` | `String` | 来源意图节点 ID |
| `zoneType` | `String` | `USER_ZONE` / `SERVER_ZONE` / `EXTERNAL_NETWORK` |
| `description` | `String` | 描述，可选 |

### 5.7 `AddressPlanItem`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `String` | 地址规划元素 ID |
| `zoneId` | `String` | 区域 ID |
| `subnet` | `String` | 网段 |
| `gateway` | `String` | 网关 |
| `dnsServers` | `List<String>` | DNS，可选 |
| `exampleHostAddress` | `String` | 示例主机地址，可选 |
| `hostAddressHints` | `List<String>` | 主机地址提示，可选 |
| `traceRefs` | `TraceRefs` | 追溯关系，可选 |

### 5.8 `VlanPlanItem`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `String` | VLAN 规划元素 ID |
| `vlanId` | `Integer` | VLAN ID |
| `name` | `String` | VLAN 名称 |
| `zoneId` | `String` | 区域 ID |
| `accessPorts` | `List<PortRef>` | 接入口 |
| `trunkPorts` | `List<PortRef>` | Trunk 口，可选 |
| `traceRefs` | `TraceRefs` | 追溯关系，可选 |

### 5.9 `PortRef`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `deviceId` | `String` | 设备 ID |
| `interfaceName` | `String` | 接口名 |
| `description` | `String` | 说明，可选 |

### 5.10 `RoutingPlan`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `String` | 路由规划元素 ID |
| `protocol` | `String` | `OSPF` / `STATIC` / `BGP` 等 |
| `area` | `String` | OSPF 区域，可选 |
| `routers` | `List<RoutingRouter>` | 路由器规划 |
| `defaultRoute` | `DefaultRoute` | 默认路由 |
| `traceRefs` | `TraceRefs` | 追溯关系，可选 |

### 5.11 `RoutingRouter`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `String` | 路由器规划元素 ID |
| `deviceId` | `String` | 设备 ID |
| `routerId` | `String` | Router ID |
| `advertisedNetworks` | `List<String>` | 宣告网段 |
| `traceRefs` | `TraceRefs` | 追溯关系，可选 |

### 5.12 `DefaultRoute`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `enabled` | `Boolean` | 是否启用默认路由 |
| `nextHop` | `String` | 下一跳或外部网络标识 |
| `outInterface` | `PortRef` | 出口接口，可选 |

### 5.13 `SecurityPolicyPlanItem`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `String` | 安全策略 ID |
| `name` | `String` | 策略名称 |
| `sourceZone` | `String` | 源区域 |
| `targetZone` | `String` | 目标区域 |
| `action` | `String` | `ALLOW` / `DENY` |
| `service` | `String` | 服务范围 |
| `enforcementPoint` | `EnforcementPoint` | 执行点 |
| `basedOnIntentRelation` | `String` | 来源意图关系 ID |
| `traceRefs` | `TraceRefs` | 追溯关系，可选 |

### 5.14 `EnforcementPoint`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `deviceId` | `String` | 设备 ID |
| `interfaceName` | `String` | 接口名 |
| `direction` | `String` | `INBOUND` / `OUTBOUND` |

### 5.15 `NatPlan`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `String` | NAT 规划元素 ID |
| `enabled` | `Boolean` | 是否启用 |
| `insideZones` | `List<String>` | 内部区域 |
| `outsideInterface` | `PortRef` | 出口接口 |
| `description` | `String` | 说明 |
| `traceRefs` | `TraceRefs` | 追溯关系，可选 |

### 5.16 `PlanConstraint`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `String` | 约束 ID |
| `type` | `String` | 约束类型 |
| `description` | `String` | 约束描述 |
| `sourceIntentId` | `String` | 来源意图节点或关系 ID，可选 |

## 6. `ConfigSet`

`ConfigSet` 是 `ConfigurationAgent` 输出的结构化配置结果。

`ConfigSet` 不能只是一整段命令文本。

字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | `String` | 任务 ID |
| `planVersion` | `Integer` | 来源规划版本 |
| `configVersion` | `Integer` | 配置版本 |
| `targetEnvironment` | `TargetEnvironment` | 目标环境 |
| `generationSummary` | `String` | 生成摘要 |
| `generationSources` | `List<GenerationSource>` | 配置生成来源 |
| `deviceConfigs` | `List<DeviceConfig>` | 设备配置 |
| `endpointConfigs` | `List<EndpointConfig>` | 终端配置 |
| `rollbackPlan` | `RollbackPlan` | 回滚计划 |
| `warnings` | `List<ConfigWarning>` | 配置警告 |
| `traceRefs` | `TraceRefs` | 整体追溯关系 |
| `stageStatus` | `StageStatus` | 阶段状态 |
| `createdAt` | `String / LocalDateTime` | 创建时间 |

### 6.1 `GenerationSource`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `sourceType` | `String` | 来源类型 |
| `sourceName` | `String` | 来源名称 |
| `description` | `String` | 说明 |
| `artifactRef` | `String` | 知识库、模板或工具结果引用，可选 |

长期 `sourceType` 应包括：

```text
LLM
RAG
TEMPLATE
RULE
TOOL
MCP
MANUAL_OVERRIDE
```

### 6.2 `DeviceConfig`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `deviceId` | `String` | 设备 ID |
| `deviceName` | `String` | 设备名称 |
| `deviceType` | `String` | 设备类型 |
| `vendor` | `String` | 厂商或配置风格 |
| `configText` | `String` | 完整配置文本，用于展示和审查 |
| `commandBlocks` | `List<CommandBlock>` | 结构化配置块 |

### 6.3 `CommandBlock`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `blockId` | `String` | 配置块 ID |
| `blockType` | `String` | `INTERFACE` / `VLAN` / `ACL` / `ROUTING` / `NAT` 等 |
| `order` | `Integer` | 执行顺序 |
| `title` | `String` | 配置块标题 |
| `commands` | `List<String>` | 命令列表 |
| `explanation` | `String` | 配置解释 |
| `rollbackCommands` | `List<String>` | 回滚命令 |
| `rollbackStrategy` | `String` | 回滚策略，可选 |
| `dependsOn` | `List<String>` | 依赖的配置块 ID |
| `traceRefs` | `TraceRefs` | 追溯关系 |
| `riskLevel` | `String` | 风险等级，可选 |

每个 `commandBlock` 至少应能追溯到 `planElementIds` 或 `intentRelationIds`。

### 6.4 `TraceRefs`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `intentNodeIds` | `List<String>` | 关联的意图节点 |
| `intentRelationIds` | `List<String>` | 关联的意图关系 |
| `planElementIds` | `List<String>` | 关联的规划元素 |
| `configBlockIds` | `List<String>` | 关联的配置块 |
| `testIds` | `List<String>` | 关联的执行测试 |
| `validationItemIds` | `List<String>` | 关联的验证项 |
| `repairActionIds` | `List<String>` | 关联的修复动作 |

不要求所有列表都必填，但关键配置块必须具备可解释来源。

### 6.5 `EndpointConfig`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nodeId` | `String` | 终端节点 ID |
| `nodeType` | `String` | 节点类型 |
| `zoneId` | `String` | 所属区域 |
| `commands` | `List<String>` | 终端配置命令 |
| `explanation` | `String` | 说明 |
| `traceRefs` | `TraceRefs` | 追溯关系 |

### 6.6 `RollbackPlan`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `strategy` | `String` | 回滚策略 |
| `rollbackBlocks` | `List<RollbackBlock>` | 回滚块 |
| `description` | `String` | 说明 |

### 6.7 `RollbackBlock`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `deviceId` | `String` | 设备 ID |
| `blockId` | `String` | 对应配置块 ID |
| `commands` | `List<String>` | 回滚命令 |
| `order` | `Integer` | 回滚顺序 |

### 6.8 `ConfigWarning`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `level` | `String` | 警告等级 |
| `message` | `String` | 警告内容 |
| `relatedBlockId` | `String` | 相关配置块 ID，可选 |

## 7. `ExecutionReport`

`ExecutionReport` 是 Execution Module 输出的执行适配与测试结果。

`ExecutionReport` 支持 DryRun、Mininet/Ryu、真实设备适配等多种执行模式。

字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | `String` | 任务 ID |
| `planVersion` | `Integer` | 来源规划版本 |
| `configVersion` | `Integer` | 来源配置版本 |
| `executionVersion` | `Integer` | 执行版本 |
| `executionMode` | `String` | 执行模式 |
| `executionPlan` | `ExecutionPlan` | 执行计划 |
| `runtimeState` | `RuntimeState` | 运行时状态 |
| `testResult` | `TestResult` | 测试结果 |
| `errors` | `List<ExecutionError>` | 执行错误 |
| `warnings` | `List<String>` | 执行警告 |
| `stageStatus` | `StageStatus` | 阶段状态 |
| `startedAt` | `String / LocalDateTime` | 开始时间 |
| `finishedAt` | `String / LocalDateTime` | 结束时间 |

`executionMode` 可包含：

```text
DRY_RUN
MININET_RYU
REAL_DEVICE
CUSTOM_ADAPTER
```

### 7.1 `ExecutionPlan`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `adapterType` | `String` | 适配器类型 |
| `topologyScript` | `String` | 拓扑脚本或拓扑描述 |
| `hostCommands` | `List<ExecutionCommand>` | 主机命令 |
| `flowRules` | `List<FlowRule>` | 流表规则 |
| `testCommands` | `List<TestCommand>` | 测试命令 |
| `cleanupCommands` | `List<ExecutionCommand>` | 清理命令 |
| `traceRefs` | `TraceRefs` | 追溯关系 |

### 7.2 `ExecutionCommand`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `commandId` | `String` | 命令 ID |
| `targetNodeId` | `String` | 目标节点 ID |
| `commandType` | `String` | 命令类型 |
| `command` | `String` | 受控命令文本 |
| `safeToRun` | `Boolean` | 是否允许执行 |
| `traceRefs` | `TraceRefs` | 追溯关系 |

### 7.3 `FlowRule`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `ruleId` | `String` | 流表规则 ID |
| `switchId` | `String` | 交换机 ID |
| `dpid` | `String` | OpenFlow DPID |
| `priority` | `Integer` | 优先级 |
| `match` | `Map<String, Object>` | 匹配条件 |
| `actions` | `List<String>` | 动作 |
| `traceRefs` | `TraceRefs` | 追溯关系 |

### 7.4 `TestCommand`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `testId` | `String` | 测试 ID |
| `type` | `String` | 测试类型 |
| `sourceNode` | `String` | 源节点 |
| `targetNode` | `String` | 目标节点 |
| `command` | `String` | 受控测试命令 |
| `expectedResult` | `String` | 期望结果 |
| `traceRefs` | `TraceRefs` | 追溯关系 |

### 7.5 `RuntimeState`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `environmentStatus` | `String` | 环境状态 |
| `nodes` | `List<RuntimeNodeState>` | 节点状态 |
| `links` | `List<RuntimeLinkState>` | 链路状态 |
| `controllerState` | `Map<String, Object>` | 控制器状态 |
| `flowState` | `Map<String, Object>` | 流表状态 |
| `rawLogs` | `List<String>` | 原始日志 |

### 7.6 `RuntimeNodeState`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nodeId` | `String` | 节点 ID |
| `status` | `String` | 节点状态 |
| `interfaces` | `Map<String, Object>` | 接口状态，可选 |
| `metadata` | `Map<String, Object>` | 扩展信息，可选 |

### 7.7 `RuntimeLinkState`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `linkId` | `String` | 链路 ID |
| `status` | `String` | 链路状态 |
| `sourceNode` | `String` | 源节点 |
| `targetNode` | `String` | 目标节点 |
| `metadata` | `Map<String, Object>` | 扩展信息，可选 |

### 7.8 `TestResult`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `connectivityTests` | `List<ConnectivityTestResult>` | 连通性测试结果 |
| `policyTests` | `List<PolicyTestResult>` | 策略测试结果 |
| `performanceTests` | `List<PerformanceTestResult>` | 性能测试结果 |
| `rawLogs` | `List<String>` | 原始日志 |

说明：

1. `TestResult` 只记录执行结果。
2. 是否满足意图由 `VerificationAgent` 判断。

### 7.9 `ExecutionError`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `errorId` | `String` | 错误 ID |
| `errorCode` | `String` | 错误码 |
| `message` | `String` | 错误摘要 |
| `relatedCommandId` | `String` | 相关命令 ID，可选 |
| `traceRefs` | `TraceRefs` | 追溯关系，可选 |

## 8. `ValidationReport`

`ValidationReport` 是 `VerificationAgent` 输出的意图达成判断。

字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | `String` | 任务 ID |
| `intentVersion` | `Integer` | 来源意图版本 |
| `planVersion` | `Integer` | 来源规划版本 |
| `configVersion` | `Integer` | 来源配置版本 |
| `executionVersion` | `Integer` | 来源执行版本 |
| `validationVersion` | `Integer` | 验证版本 |
| `overallStatus` | `ValidationStatus` | 总体验证状态 |
| `summary` | `String` | 验证摘要 |
| `items` | `List<ValidationItem>` | 验证项 |
| `evidences` | `List<ValidationEvidence>` | 验证证据 |
| `suggestions` | `List<String>` | 建议 |
| `stageStatus` | `StageStatus` | 阶段状态 |
| `createdAt` | `String / LocalDateTime` | 创建时间 |

### 8.1 `ValidationItem`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `itemId` | `String` | 验证项 ID |
| `name` | `String` | 验证项名称 |
| `type` | `String` | `CONNECTIVITY` / `ISOLATION` / `ROUTING` / `SECURITY_POLICY` 等 |
| `expected` | `String` | 期望结果 |
| `actual` | `String` | 实际结果 |
| `passed` | `Boolean` | 是否通过 |
| `severity` | `String` | 严重程度 |
| `relatedIntentRelationId` | `String` | 关联意图关系 ID |
| `relatedPlanElementIds` | `List<String>` | 关联规划元素 ID |
| `relatedConfigBlockIds` | `List<String>` | 关联配置块 ID |
| `relatedTestId` | `String` | 关联测试 ID |
| `evidenceIds` | `List<String>` | 关联证据 ID |
| `message` | `String` | 展示说明 |

### 8.2 `ValidationEvidence`

`ValidationEvidence` 是验证证据模型。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `evidenceId` | `String` | 证据 ID |
| `evidenceType` | `String` | 证据类型 |
| `source` | `String` | 证据来源 |
| `rawValue` | `String` | 原始值 |
| `normalizedValue` | `String / Object` | 归一化值 |
| `relatedTestId` | `String` | 关联测试 ID |
| `relatedRuntimeObjectId` | `String` | 关联运行时对象 ID |

说明：

1. `ValidationReport` 不直接修改配置。
2. 失败修复交给 `HealingAgent` / `RepairPlan`。

## 9. `RepairPlan`

`RepairPlan` 是 `HealingAgent` 输出的诊断与修复计划。

字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | `String` | 任务 ID |
| `validationVersion` | `Integer` | 来源验证版本 |
| `repairVersion` | `Integer` | 修复版本 |
| `overallRepairStrategy` | `String` | 总体修复策略 |
| `failureAnalysis` | `List<FailureAnalysis>` | 失败分析 |
| `actions` | `List<RepairAction>` | 修复动作 |
| `requiresUserConfirmation` | `Boolean` | 是否需要用户确认 |
| `stageStatus` | `StageStatus` | 阶段状态 |
| `createdAt` | `String / LocalDateTime` | 创建时间 |

### 9.1 `FailureAnalysis`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `analysisId` | `String` | 分析 ID |
| `failureType` | `String` | 失败类型 |
| `rootCauseSummary` | `String` | 根因摘要 |
| `relatedValidationItemIds` | `List<String>` | 相关验证项 ID |
| `relatedIntentRelationIds` | `List<String>` | 相关意图关系 ID |
| `relatedPlanElementIds` | `List<String>` | 相关规划元素 ID |
| `relatedConfigBlockIds` | `List<String>` | 相关配置块 ID |
| `confidence` | `Double` | 置信度 |
| `evidenceIds` | `List<String>` | 证据 ID |

`failureType` 可包含：

```text
INTENT_CONFLICT
PLANNING_ERROR
CONFIG_ERROR
EXECUTION_ENV_ERROR
VERIFICATION_ERROR
INSUFFICIENT_INFORMATION
```

### 9.2 `RepairAction`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `actionId` | `String` | 修复动作 ID |
| `actionType` | `String` | 修复动作类型 |
| `targetStage` | `WorkflowStage` | 目标回退或重跑阶段 |
| `description` | `String` | 动作描述 |
| `relatedFailureAnalysisId` | `String` | 关联失败分析 ID |
| `inputArtifactIds` | `List<String>` | 输入产物 ID |
| `expectedOutputArtifactType` | `ArtifactType` | 预期输出产物类型 |
| `riskLevel` | `String` | 风险等级 |
| `requiresApproval` | `Boolean` | 是否需要审批 |
| `status` | `RepairStatus` | 修复动作状态 |

`actionType` 可包含：

```text
REPLAN
REGENERATE_CONFIG
PATCH_CONFIG
REEXECUTE
ASK_USER
ROLLBACK
```

说明：

1. `RepairPlan` 不直接执行修复。
2. Orchestrator 根据 `RepairAction` 决定重新进入哪个阶段。
3. `ExecutionAdapter` 负责实际执行相关动作。

## 10. 前端展示辅助模型

可以保留轻量展示模型，但不要和核心 DTO 混淆。

可选模型：

1. `TopologyViewModel`
2. `AgentTimelineItem`
3. `ConfigBlockView`
4. `ValidationSummaryView`

说明：

1. 前端展示模型可以由后端 VO 或前端转换生成。
2. 不要让前端展示模型污染核心阶段 DTO。
3. 前端页面布局和交互设计不在本文档定义。

## 11. 数据模型边界

禁止事项：

1. `NetworkIntent` 不包含设备、接口、VLAN、IP、CLI。
2. `NetworkPlan` 不包含具体 CLI。
3. `ConfigSet` 不只是一整段命令文本。
4. `ExecutionReport` 不判断业务意图是否满足。
5. `ValidationReport` 不直接修改配置。
6. `RepairPlan` 不直接执行修复。
7. DTO 不依赖 Spring AI Alibaba 类型。
8. DTO 不依赖 Web、Controller、Orchestrator、具体 Agent 实现。
9. DTO 不包含 API Key、请求头、模型 provider 私密参数。
10. DTO 中的系统状态字段不应由模型直接控制。

## 12. 本文档与其他文档的分工

文档分工：

1. 本文档：核心 DTO、字段、版本、状态、追溯关系。
2. `docs/02_MAVEN_MODULES.md`：Maven 模块与依赖边界。
3. `docs/03_MODULE_DESIGN.md`：业务模块职责与流程。
4. `docs/05_API_DESIGN.md`：API 路径和请求响应。
5. `docs/06_DEV_PLAN.md`：长期实现路线。
6. `docs/09_AGENT_BUILD_GUIDE.md`：真实 Spring AI Alibaba Agent 构建规范。

实现数据模型时，应保持本文档与 `docs/09_AGENT_BUILD_GUIDE.md` 的核心链路一致：

```text
ResponseSchema -> Parser -> DTO -> Validator -> NetworkWorkspace
```
