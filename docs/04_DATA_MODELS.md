# 数据模型设计

## 1. 文档目标

本文档定义 MAC-TAV 长期核心 DTO 和跨模块数据契约。共享 DTO 和领域模型枚举放在 `mac-tav-model`，公共异常、统一响应、错误码放在 `mac-tav-common`。

每个阶段产物必须可序列化、可追踪、可校验、可进入 `NetworkWorkspace`。本文档不负责 Maven 模块拆分、API 路径、前端页面、数据库表结构或 Agent 初始化方式。

## 2. 通用数据建模约定

### 2.1 ID 约定

所有核心对象必须有稳定 `id`。ID 应便于 `traceRefs`、`ValidationItem`、`RepairAction` 引用，不依赖前端展示名称或数组下标。

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

当前版本用于主流程，历史版本通过 `NetworkArtifact` 保留用于重试、自愈和回放。`NetworkWorkspace` 记录每类产物的当前版本。

### 2.3 时间字段约定

| 字段 | 说明 |
| --- | --- |
| `createTime` | 对象创建时间 |
| `updateTime` | 对象最后更新时间 |
| `startTime` | 阶段或执行开始时间 |
| `finishTime` | 阶段或执行结束时间 |

Java DTO 内部统一使用 `LocalDateTime`。对外 API 序列化格式由 Web 层统一配置。

### 2.4 状态枚举约定

状态按维度拆分，不混用任务状态、流程阶段、执行状态、产物状态、验证结论和修复状态。枚举放在 `mac-tav-model`。

| 枚举 | 维度 | 可选值 |
| --- | --- | --- |
| `TaskStatus` | 任务生命周期 | `CREATED`, `RUNNING`, `WAITING_USER`, `COMPLETED`, `ERROR`, `CANCELLED`, `ARCHIVED` |
| `WorkflowStage` | 当前流程阶段 | `INTENT`, `PLANNING`, `CONFIGURATION`, `EXECUTION`, `VERIFICATION`, `HEALING`, `FINISHED` |
| `StageStatus` | 阶段执行状态 | `PENDING`, `RUNNING`, `SUCCESS`, `FAILED`, `SKIPPED` |
| `ArtifactStatus` | 阶段产物状态 | `DRAFT`, `GENERATED`, `VALIDATED`, `APPLIED`, `SUPERSEDED`, `ROLLED_BACK` |
| `ValidationStatus` | 验证结论 | `PASSED`, `FAILED`, `PARTIAL`, `UNKNOWN` |
| `RepairStatus` | 修复动作状态 | `PROPOSED`, `APPROVED`, `APPLIED`, `REJECTED`, `FAILED` |

### 2.5 追溯关系约定

`TraceRefs` 用于连接意图、规划、配置、执行、验证和修复之间的关系。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `intentNodeIds` | `List<String>` | 关联的意图节点 |
| `intentRelationIds` | `List<String>` | 关联的意图关系 |
| `planElementIds` | `List<String>` | 关联的规划元素 |
| `configBlockIds` | `List<String>` | 关联的配置块 |
| `testIds` | `List<String>` | 关联的执行测试 |
| `validationItemIds` | `List<String>` | 关联的验证项 |
| `repairActionIds` | `List<String>` | 关联的修复动作 |

`ConfigSet.commandBlocks` 须能追溯到 `NetworkPlan` / `NetworkIntent`；`ValidationItem` 须能追溯到 intent / plan / config / test；`RepairAction` 须能追溯到 `validationItem` 和相关 plan / config 元素。

---

## 3. Task 与 Workspace

### 3.1 `NetworkTask`

`NetworkTask` 表示一次用户提交的网络意图任务。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | `String` | 任务 ID |
| `rawText` | `String` | 用户原始输入 |
| `taskStatus` | `TaskStatus` | 任务总状态 |
| `currentStage` | `WorkflowStage` | 当前流程阶段 |
| `createTime` | `LocalDateTime` | 创建时间 |
| `updateTime` | `LocalDateTime` | 更新时间 |
| `createdBy` | `String` | 创建人，可选 |
| `description` | `String` | 任务描述，可选 |

### 3.2 `NetworkWorkspace`

`NetworkWorkspace` 是任务状态中心，支持当前产物 + 版本历史 / Artifact 引用。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `task` | `NetworkTask` | 任务基本信息 |
| `currentIntentVersion` | `Integer` | 当前意图版本 |
| `currentPlanVersion` | `Integer` | 当前规划版本 |
| `currentConfigVersion` | `Integer` | 当前配置版本 |
| `currentExecutionVersion` | `Integer` | 当前执行版本 |
| `currentValidationVersion` | `Integer` | 当前验证版本 |
| `currentRepairVersion` | `Integer` | 当前修复版本 |
| `currentArtifactRefs` | `Map<ArtifactType, String>` | 当前各阶段 artifactId 引用 |
| `currentIntent` | `NetworkIntent` | 当前意图产物 |
| `currentPlan` | `NetworkPlan` | 当前规划产物 |
| `currentConfigSet` | `ConfigSet` | 当前配置产物 |
| `currentExecutionReport` | `ExecutionReport` | 当前执行产物 |
| `currentValidationReport` | `ValidationReport` | 当前验证产物 |
| `currentRepairPlan` | `RepairPlan` | 当前修复计划 |
| `artifacts` | `List<NetworkArtifact>` | 全部阶段产物记录或引用 |
| `agentExecutionRecords` | `List<AgentExecutionRecord>` | Agent 执行记录 |
| `events` | `List<WorkspaceEvent>` | 事件历史和前端 timeline 数据来源 |
| `changeHistory` | `List<WorkspaceChangeRecord>` | 重试、修复、人工确认等变化记录 |
| `workspaceStatus` | `TaskStatus` | Workspace 当前状态 |

`currentXxx` 用于当前视图快速展示，完整历史 payload 由 `NetworkArtifact` 管理。`events` 支撑 SSE 和前端 timeline，`changeHistory` 记录自愈、重试、人工确认等变化。

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
| `payloadJson` | `String` | 序列化后的产物 JSON 或引用描述 |
| `payloadSummary` | `String` | 产物摘要 |
| `createTime` | `LocalDateTime` | 创建时间 |
| `createdBy` | `String` | 创建来源，例如 Agent、用户或系统 |
| `traceRefs` | `TraceRefs` | 追溯关系 |

`ArtifactType` 至少包括：`NETWORK_INTENT`、`NETWORK_PLAN`、`CONFIG_SET`、`EXECUTION_REPORT`、`VALIDATION_REPORT`、`REPAIR_PLAN`。`NetworkArtifact` 不使用宽泛的 `payload` 字段作为长期契约，`payloadSummary` 用于列表和审计摘要。

### 3.4 `AgentExecutionRecord`

`AgentExecutionRecord` 用于记录 Agent 或关键模块的执行过程。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `recordId` | `String` | 执行记录 ID |
| `taskId` | `String` | 任务 ID |
| `agentName` | `String` | Agent 或模块名称 |
| `stage` | `WorkflowStage` | 所属阶段 |
| `status` | `StageStatus` | 执行状态 |
| `inputArtifactIds` | `List<String>` | 输入产物 ID |
| `outputArtifactIds` | `List<String>` | 输出产物 ID |
| `toolCallSummaries` | `List<String>` | Tool 调用摘要 |
| `mcpCallSummaries` | `List<String>` | MCP 调用摘要 |
| `a2aCallSummaries` | `List<String>` | A2A / 远程调用摘要 |
| `modelCallCount` | `Integer` | 模型调用次数 |
| `startTime` | `LocalDateTime` | 开始时间 |
| `finishTime` | `LocalDateTime` | 结束时间 |
| `durationMs` | `Long` | 执行耗时毫秒数 |
| `inputSummary` | `String` | 输入摘要 |
| `outputSummary` | `String` | 输出摘要 |
| `errorCode` | `String` | 错误码，可选 |
| `errorMessage` | `String` | 错误信息，可选 |
| `message` | `String` | 展示文本或摘要 |

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
| `createTime` | `LocalDateTime` | 创建时间 |
| `createdBy` | `String` | 创建来源 |

### 3.6 `WorkspaceEvent`

`WorkspaceEvent` 用于支撑 SSE、Event history 和前端 timeline。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `eventId` | `String` | 事件 ID |
| `taskId` | `String` | 任务 ID |
| `eventType` | `String` | 事件类型 |
| `stage` | `WorkflowStage` | 相关阶段 |
| `eventTime` | `LocalDateTime` | 事件时间 |
| `severity` | `String` | 严重程度，例如 `INFO`、`WARN`、`ERROR` |
| `title` | `String` | 事件标题 |
| `message` | `String` | 事件摘要 |
| `relatedArtifactId` | `String` | 相关产物 ID，可选 |
| `relatedRecordId` | `String` | 相关执行记录 ID，可选 |
| `traceId` | `String` | 调用链追踪 ID，可选 |
| `payloadSummary` | `String` | 事件载荷摘要 |

---

## 4. `NetworkIntent`

`NetworkIntent` 是 `IntentAgent` 输出的业务意图模型。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `intentId` | `String` | 意图 ID |
| `taskId` | `String` | 任务 ID |
| `intentVersion` | `Integer` | 意图版本 |
| `rawText` | `String` | 原始用户输入 |
| `semanticIntentGraph` | `SemanticIntentGraph` | 语义意图图 |
| `assumptions` | `List<Assumption>` | 无法确认的假设 |
| `constraints` | `List<IntentConstraint>` | 用户明确约束 |
| `preferences` | `List<IntentPreference>` | 用户偏好 |
| `status` | `StageStatus` | 意图解析阶段状态 |
| `createTime` | `LocalDateTime` | 创建时间 |
| `updateTime` | `LocalDateTime` | 更新时间 |
| `createdBy` | `String` | 创建来源，例如 Agent 名称 |

### 4.1 `SemanticIntentGraph`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nodes` | `List<IntentNode>` | 意图节点 |
| `relations` | `List<IntentRelation>` | 意图关系 |

### 4.2 `IntentNode`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nodeId` | `String` | 节点 ID |
| `label` | `String` | 节点标签 |
| `type` | `String` | 节点类型，例如 `BUSINESS_ZONE`, `SERVICE`, `USER_GROUP` |
| `attributes` | `Map<String, Object>` | 扩展属性 |

### 4.3 `IntentRelation`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `relationId` | `String` | 关系 ID |
| `source` | `String` | 源节点 ID |
| `target` | `String` | 目标节点 ID |
| `relationType` | `String` | 关系类型，例如 `ALLOW`, `DENY`, `ISOLATE`, `INTERNET_ACCESS` |
| `protocol` | `String` | 协议偏好，例如 `TCP`, `UDP`, `ANY` |
| `port` | `String` | 端口偏好，例如 `80`, `443`, `ANY` |
| `description` | `String` | 自然语言描述 |
| `confidence` | `Double` | 置信度 0–1 |
| `attributes` | `Map<String, Object>` | 扩展属性 |

### 4.4 `Assumption`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `assumptionId` | `String` | 假设 ID |
| `description` | `String` | 假设内容 |
| `reason` | `String` | 假设原因 |
| `needsClarification` | `Boolean` | 是否需要用户澄清 |

### 4.5 `IntentConstraint`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `constraintId` | `String` | 约束 ID |
| `description` | `String` | 约束描述 |
| `relatedIntentRelationIds` | `List<String>` | 关联的意图关系 ID |

### 4.6 `IntentPreference`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `preferenceId` | `String` | 偏好 ID |
| `key` | `String` | 偏好键 |
| `value` | `String` | 偏好值 |

---

## 5. `NetworkPlan`

`NetworkPlan` 是 `PlanningAgent` 输出的网络设计方案，包含拓扑、区域、地址、VLAN、路由、安全策略、NAT、`targetEnvironment`。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `planId` | `String` | 规划 ID |
| `taskId` | `String` | 任务 ID |
| `intentId` | `String` | 关联的意图 ID |
| `planVersion` | `Integer` | 规划版本 |
| `selectedArchitecture` | `SelectedArchitecture` | 选定的网络架构 |
| `targetEnvironment` | `TargetEnvironment` | 目标执行环境 |
| `topology` | `Topology` | 网络拓扑 |
| `zones` | `List<NetworkZone>` | 网络区域 |
| `addressPlan` | `List<AddressPlanItem>` | 地址规划 |
| `vlanPlan` | `List<VlanPlanItem>` | VLAN 规划 |
| `routingPlans` | `List<RoutingPlan>` | 路由计划 |
| `defaultRoute` | `DefaultRoute` | 默认路由 |
| `securityPolicyPlan` | `List<SecurityPolicyPlanItem>` | 安全策略规划 |
| `natPlan` | `NatPlan` | NAT 规划 |
| `constraints` | `List<PlanConstraint>` | 规划约束 |
| `status` | `StageStatus` | 规划阶段状态 |
| `createTime` | `LocalDateTime` | 创建时间 |
| `updateTime` | `LocalDateTime` | 更新时间 |
| `createdBy` | `String` | 创建来源 |

### 5.1 `SelectedArchitecture`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `architectureType` | `String` | 架构类型，例如 `THREE_TIER`, `SPINE_LEAF` |
| `description` | `String` | 架构描述 |

### 5.2 `TargetEnvironment`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `environmentType` | `String` | 环境类型，例如 `MININET`, `RYU`, `DOCKER`, `CUSTOM` |
| `osType` | `String` | 操作系统类型，例如 `LINUX`, `WINDOWS` |
| `controllerType` | `String` | 控制器类型，例如 `RYU`, `FLOODLIGHT` |
| `topoScriptName` | `String` | 拓扑脚本名称，可选 |
| `customConfig` | `Map<String, Object>` | 自定义配置，可选 |

### 5.3 `Topology`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nodes` | `List<TopologyNode>` | 拓扑节点 |
| `links` | `List<TopologyLink>` | 拓扑链路 |

### 5.4 `TopologyNode`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nodeId` | `String` | 节点 ID |
| `name` | `String` | 节点名称 |
| `type` | `String` | 节点类型，例如 `SWITCH`, `ROUTER`, `HOST` |
| `zoneId` | `String` | 所属区域 ID |
| `attributes` | `Map<String, Object>` | 扩展属性 |

### 5.5 `TopologyLink`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `linkId` | `String` | 链路 ID |
| `source` | `String` | 源节点 ID |
| `target` | `String` | 目标节点 ID |
| `linkType` | `String` | 链路类型，例如 `ETHERNET`, `FIBER` |
| `bandwidth` | `String` | 带宽描述 |

### 5.6 `NetworkZone`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `zoneId` | `String` | 区域 ID |
| `name` | `String` | 区域名称 |
| `type` | `String` | 区域类型，例如 `OFFICE`, `SERVER`, `DMZ` |
| `relatedIntentNodeIds` | `List<String>` | 关联的意图节点 ID |

### 5.7 `AddressPlanItem`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `itemId` | `String` | 地址规划项 ID |
| `zoneId` | `String` | 所属区域 ID |
| `subnet` | `String` | CIDR 子网，例如 `10.0.1.0/24` |
| `gateway` | `String` | 网关地址 |
| `dnsServers` | `List<String>` | DNS 服务器，可选 |
| `ntpServers` | `List<String>` | NTP 服务器，可选 |

### 5.8 `VlanPlanItem`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `itemId` | `String` | VLAN 规划项 ID |
| `zoneId` | `String` | 所属区域 ID |
| `vlanId` | `Integer` | VLAN ID |
| `name` | `String` | VLAN 名称 |
| `subnet` | `String` | 关联子网 CIDR |

### 5.9 `PortRef`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nodeId` | `String` | 所属节点 ID |
| `portName` | `String` | 端口名 |

### 5.10 `RoutingPlan`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `protocol` | `String` | 路由协议，例如 `STATIC`, `OSPF` |
| `ospfArea` | `String` | OSPF 区域，可选 |
| `routers` | `List<RoutingRouter>` | 路由器配置列表 |

### 5.11 `RoutingRouter`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nodeId` | `String` | 节点 ID |
| `routerId` | `String` | Router ID |
| `networks` | `List<String>` | 通告网络 |

### 5.12 `DefaultRoute`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nodeId` | `String` | 节点 ID |
| `nextHop` | `String` | 下一跳 |

### 5.13 `SecurityPolicyPlanItem`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `itemId` | `String` | 安全策略项 ID |
| `basedOnIntentRelationId` | `String` | 基于的意图关系 ID |
| `action` | `String` | 动作，例如 `PERMIT`, `DENY` |
| `sourceZoneId` | `String` | 源区域 ID |
| `targetZoneId` | `String` | 目标区域 ID |
| `protocol` | `String` | 协议 |
| `port` | `String` | 端口 |
| `enforcementPoints` | `List<EnforcementPoint>` | 执行点 |

### 5.14 `EnforcementPoint`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nodeId` | `String` | 执行节点 ID |
| `direction` | `String` | 方向，例如 `INBOUND`, `OUTBOUND` |

### 5.15 `NatPlan`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `type` | `String` | NAT 类型，例如 `STATIC`, `DYNAMIC`, `PAT` |
| `insideZoneId` | `String` | 内部区域 ID |
| `outsideZoneId` | `String` | 外部区域 ID |
| `ruleCount` | `Integer` | 规则数量 |

### 5.16 `PlanConstraint`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `constraintId` | `String` | 约束 ID |
| `description` | `String` | 约束描述 |
| `relatedPlanElementIds` | `List<String>` | 关联的规划元素 ID |

---

## 6. `ConfigSet`

`ConfigSet` 是 `ConfigurationAgent` 输出的结构化配置结果。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `configSetId` | `String` | 配置集 ID |
| `planId` | `String` | 关联的规划 ID |
| `taskId` | `String` | 任务 ID |
| `configVersion` | `Integer` | 配置版本 |
| `generationSource` | `GenerationSource` | 生成来源 |
| `deviceConfigs` | `List<DeviceConfig>` | 设备配置列表 |
| `rollbackPlan` | `RollbackPlan` | 回滚计划 |
| `warnings` | `List<ConfigWarning>` | 警告信息 |
| `status` | `StageStatus` | 配置阶段状态 |
| `createTime` | `LocalDateTime` | 创建时间 |
| `updateTime` | `LocalDateTime` | 更新时间 |
| `createdBy` | `String` | 创建来源 |

### 6.1 `GenerationSource`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `sourceType` | `String` | 来源类型，例如 `RAG`, `TEMPLATE`, `MODEL_GENERATED`, `MCP` |
| `sourceId` | `String` | 来源 ID，例如模板 ID、知识库条目 ID |
| `sourceDescription` | `String` | 来源描述 |
| `confidence` | `Double` | 置信度 0–1 |

### 6.2 `DeviceConfig`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `deviceConfigId` | `String` | 设备配置 ID |
| `deviceName` | `String` | 设备名称 |
| `deviceType` | `String` | 设备类型，例如 `SWITCH`, `ROUTER` |
| `commandBlocks` | `List<CommandBlock>` | 命令块列表 |
| `endpointConfig` | `EndpointConfig` | 端点配置，可选 |
| `traceRefs` | `TraceRefs` | 追溯关系 |

### 6.3 `CommandBlock`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `blockId` | `String` | 命令块 ID |
| `commands` | `List<String>` | 命令列表 |
| `explanation` | `String` | 命令解释 |
| `traceRefs` | `TraceRefs` | 追溯关系 |
| `rollbackCommands` | `List<String>` | 回滚命令 |
| `riskLevel` | `String` | 风险等级，例如 `LOW`, `MEDIUM`, `HIGH` |
| `isIdempotent` | `Boolean` | 是否幂等 |

每个 `commandBlock` 至少应能追溯到 `planElementIds` 或 `intentRelationIds`。

### 6.4 `TraceRefs`

字段定义见 §2.5。配置块不要求填满所有列表，但关键配置块必须具备可解释来源。

### 6.5 `EndpointConfig`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `ipAddress` | `String` | IP 地址 |
| `subnetMask` | `String` | 子网掩码 |
| `gateway` | `String` | 网关 |
| `vlanId` | `Integer` | VLAN ID，可选 |

### 6.6 `RollbackPlan`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `blocks` | `List<RollbackBlock>` | 回滚块列表 |
| `estimatedRollbackTimeSeconds` | `Integer` | 预估回滚时间（秒） |

### 6.7 `RollbackBlock`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `relatedCommandBlockId` | `String` | 关联的命令块 ID |
| `commands` | `List<String>` | 回滚命令 |

### 6.8 `ConfigWarning`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `warningId` | `String` | 警告 ID |
| `severity` | `String` | 严重程度 |
| `message` | `String` | 警告消息 |
| `relatedCommandBlockId` | `String` | 关联命令块 ID，可选 |

---

## 7. `ExecutionReport`

`ExecutionReport` 是 Execution Module 输出的执行适配与测试结果。支持 Mininet/Ryu、真实设备适配等多种执行模式。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `executionId` | `String` | 执行 ID |
| `planId` | `String` | 关联规划 ID |
| `configSetId` | `String` | 关联配置集 ID |
| `taskId` | `String` | 任务 ID |
| `executionVersion` | `Integer` | 执行版本 |
| `environmentType` | `String` | 执行环境类型 |
| `executionPlan` | `ExecutionPlan` | 执行计划 |
| `runtimeState` | `RuntimeState` | 运行时状态 |
| `testResults` | `List<TestResult>` | 测试结果 |
| `errors` | `List<ExecutionError>` | 执行错误 |
| `overallStatus` | `String` | 整体状态，例如 `SUCCESS`, `PARTIAL`, `FAILED` |
| `createTime` | `LocalDateTime` | 创建时间 |
| `updateTime` | `LocalDateTime` | 更新时间 |
| `createdBy` | `String` | 创建来源 |

### 7.1 `ExecutionPlan`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `commands` | `List<ExecutionCommand>` | 执行命令序列 |
| `flowRules` | `List<FlowRule>` | 流规则 |
| `tests` | `List<TestCommand>` | 测试命令 |

### 7.2 `ExecutionCommand`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `commandId` | `String` | 命令 ID |
| `targetNodeId` | `String` | 目标节点 ID |
| `command` | `String` | 命令文本 |
| `commandType` | `String` | 命令类型 |
| `relatedCommandBlockId` | `String` | 关联配置块 ID，可选 |

### 7.3 `FlowRule`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `flowId` | `String` | 流规则 ID |
| `switchId` | `String` | 交换机 ID |
| `match` | `Map<String, Object>` | 匹配条件 |
| `actions` | `List<String>` | 动作列表 |
| `relatedSecurityPolicyId` | `String` | 关联安全策略 ID，可选 |

### 7.4 `TestCommand`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `testId` | `String` | 测试 ID |
| `testType` | `String` | 测试类型，例如 `PING`, `TRACEROUTE`, `NC` |
| `sourceNode` | `String` | 源节点 ID |
| `targetAddress` | `String` | 目标地址 |
| `relatedIntentRelationId` | `String` | 关联意图关系 ID，可选 |

### 7.5 `RuntimeState`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nodes` | `List<RuntimeNodeState>` | 节点运行时状态 |
| `links` | `List<RuntimeLinkState>` | 链路运行时状态 |
| `timestamp` | `LocalDateTime` | 采集时间 |

### 7.6 `RuntimeNodeState`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nodeId` | `String` | 节点 ID |
| `status` | `String` | 状态，例如 `UP`, `DOWN` |
| `cpuUsage` | `Double` | CPU 使用率，可选 |
| `memoryUsage` | `Double` | 内存使用率，可选 |

### 7.7 `RuntimeLinkState`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `linkId` | `String` | 链路 ID |
| `status` | `String` | 状态，例如 `UP`, `DOWN` |

### 7.8 `TestResult`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `testId` | `String` | 测试 ID |
| `passed` | `Boolean` | 是否通过 |
| `expected` | `String` | 期望结果 |
| `actual` | `String` | 实际结果 |
| `output` | `String` | 原始输出，可选 |
| `errorMessage` | `String` | 错误信息，可选 |
| `durationMs` | `Long` | 耗时毫秒数 |

说明：`TestResult` 是执行模块的原始测试结果；`ValidationItem` 是验证模块基于测试结果的意图达成判断。

### 7.9 `ExecutionError`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `errorId` | `String` | 错误 ID |
| `nodeId` | `String` | 错误节点 ID，可选 |
| `commandId` | `String` | 错误命令 ID，可选 |
| `errorType` | `String` | 错误类型 |
| `errorMessage` | `String` | 错误消息 |
| `timestamp` | `LocalDateTime` | 错误时间 |

---

## 8. `ValidationReport`

`ValidationReport` 是 `VerificationAgent` 输出的意图达成判断。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `validationId` | `String` | 验证 ID |
| `taskId` | `String` | 任务 ID |
| `executionId` | `String` | 关联执行 ID |
| `validationVersion` | `Integer` | 验证版本 |
| `overallStatus` | `ValidationStatus` | 总体验证结论 |
| `items` | `List<ValidationItem>` | 验证项列表 |
| `summary` | `String` | 验证摘要 |
| `createTime` | `LocalDateTime` | 创建时间 |
| `updateTime` | `LocalDateTime` | 更新时间 |
| `createdBy` | `String` | 创建来源 |

### 8.1 `ValidationItem`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `itemId` | `String` | 验证项 ID |
| `status` | `ValidationStatus` | 验证结论 |
| `expected` | `String` | 期望行为 |
| `actual` | `String` | 实际行为 |
| `passed` | `Boolean` | 是否通过 |
| `severity` | `String` | 严重程度，例如 `CRITICAL`, `HIGH`, `MEDIUM`, `LOW` |
| `description` | `String` | 验证描述 |
| `traceRefs` | `TraceRefs` | 追溯关系 |
| `evidences` | `List<ValidationEvidence>` | 验证证据 |
| `suggestion` | `String` | 修复建议，可选 |

### 8.2 `ValidationEvidence`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `evidenceId` | `String` | 证据 ID |
| `type` | `String` | 证据类型，例如 `LOG`, `METRIC`, `PACKET_CAPTURE` |
| `source` | `String` | 证据来源 |
| `description` | `String` | 证据描述 |
| `relatedTestId` | `String` | 关联测试 ID，可选 |

---

## 9. `RepairPlan`

`RepairPlan` 是 `HealingAgent` 输出的诊断与修复计划。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `repairId` | `String` | 修复计划 ID |
| `taskId` | `String` | 任务 ID |
| `validationId` | `String` | 关联验证 ID |
| `repairVersion` | `Integer` | 修复版本 |
| `failureAnalysis` | `FailureAnalysis` | 失败分析 |
| `repairActions` | `List<RepairAction>` | 修复动作列表 |
| `status` | `RepairStatus` | 修复状态 |
| `createTime` | `LocalDateTime` | 创建时间 |
| `createdBy` | `String` | 创建来源 |

### 9.1 `FailureAnalysis`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `summary` | `String` | 失败摘要 |
| `rootCause` | `String` | 根因分析 |
| `failedValidationItemIds` | `List<String>` | 失败的验证项 ID |
| `affectedIntentIds` | `List<String>` | 受影响的意图 ID，可选 |
| `affectedPlanElementIds` | `List<String>` | 受影响的规划元素 ID，可选 |
| `affectedConfigBlockIds` | `List<String>` | 受影响的配置块 ID，可选 |
| `confidence` | `Double` | 置信度 |

### 9.2 `RepairAction`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `actionId` | `String` | 动作 ID |
| `actionType` | `String` | 动作类型：`REPLAN`, `REGENERATE_CONFIG`, `PATCH_CONFIG`, `REEXECUTE`, `ASK_USER`, `ROLLBACK` |
| `target` | `String` | 目标描述，例如阶段名、配置块 ID |
| `reason` | `String` | 选择理由 |
| `riskLevel` | `String` | 风险等级：`LOW`, `MEDIUM`, `HIGH` |
| `relatedValidationItemIds` | `List<String>` | 关联验证项 ID |
| `rolledBackArtifactId` | `String` | 被回滚产物 ID，仅 `ROLLBACK` 类型时使用 |
| `status` | `RepairStatus` | 动作状态 |

---

## 10. 前端展示辅助模型

可保留轻量展示模型放在 `mac-tav-web` 的 `vo` 包，不污染核心阶段 DTO。可选模型：`TopologyViewModel`、`AgentTimelineItem`、`ConfigBlockView`、`ValidationSummaryView`。

## 11. 数据模型边界

核心禁止事项（仅限模型层面）：

1. `NetworkIntent` 不包含设备、接口、VLAN、IP、CLI。
2. `NetworkPlan` 不包含具体 CLI。
3. `ConfigSet` 不只是一整段命令文本。
4. DTO 不依赖 Spring AI Alibaba 类型。
5. DTO 不包含 API Key、请求头、模型 provider 私密参数。