# 数据模型设计

本文档定义 Demo 阶段需要优先实现的共享 DTO。所有共享 DTO 应放在 `mac-tav-model`，公共枚举、异常、工具应放在 `mac-tav-common`。

字段以 Demo 可跑通和后续可追溯为优先，不要求一次性覆盖生产级复杂场景。

## 1. 通用约定

### 1.1 版本字段

所有阶段产物都应包含 `taskId` 和对应阶段版本。

| 阶段 | 版本字段 |
| --- | --- |
| Intent | `intentVersion` |
| Plan | `planVersion` |
| Config | `configVersion` |
| Execution | `executionVersion` |
| Validation | `validationVersion` |

时间字段第一阶段可选：

| 字段 | 含义 |
| --- | --- |
| `createdAt` | 创建时间 |
| `updatedAt` | 更新时间 |

### 1.2 状态枚举拆分

不要把任务状态、阶段状态和验证结论混用。

#### `TaskStatus`

表示整个任务生命周期。

```text
CREATED
RUNNING
COMPLETED
FAILED
ERROR
```

#### `StageStatus`

表示某个阶段产物或阶段执行状态。

```text
PENDING
RUNNING
SUCCESS
FAILED
SKIPPED
```

阶段自身也可以保留展示型状态值，例如：

```text
PARSED
PLANNED
GENERATED
EXECUTED
VERIFIED
```

这些展示型状态只用于前端或日志说明，不替代 `TaskStatus`。

#### `ValidationStatus`

表示验证结论。

```text
PASSED
FAILED
PARTIAL
UNKNOWN
```

### 1.3 追溯 ID 约定

凡是可能被配置块、验证项、错误定位引用的规划元素，都应有稳定 `id`。

至少包括：

1. `TopologyNode.id`
2. `TopologyLink.id`
3. `NetworkZone.id`
4. `AddressPlanItem.id`
5. `VlanPlanItem.id`
6. `RoutingPlan.id`
7. `RoutingRouter.id`
8. `SecurityPolicyPlanItem.id`
9. `NatPlan.id`
10. `CommandBlock.blockId`
11. `ValidationItem.itemId`

## 2. Task 与 Workspace

### 2.1 `NetworkTask`

表示一次用户任务。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | String | 任务 ID |
| `rawText` | String | 用户原始输入 |
| `taskStatus` | TaskStatus | 任务总状态 |
| `currentStage` | String | 当前阶段，如 `INTENT`、`PLANNING` |
| `createdAt` | String | 创建时间，可选 |
| `updatedAt` | String | 更新时间，可选 |

### 2.2 `NetworkWorkspace`

表示任务的统一状态中心。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `task` | NetworkTask | 任务基本信息 |
| `currentIntentVersion` | Integer | 当前使用的意图版本 |
| `currentPlanVersion` | Integer | 当前使用的规划版本 |
| `currentConfigVersion` | Integer | 当前使用的配置版本 |
| `currentExecutionVersion` | Integer | 当前使用的执行版本 |
| `currentValidationVersion` | Integer | 当前使用的验证版本 |
| `intent` | NetworkIntent | 当前意图产物 |
| `plan` | NetworkPlan | 当前规划产物 |
| `configSet` | ConfigSet | 当前配置产物 |
| `executionReport` | ExecutionReport | 当前执行产物 |
| `validationReport` | ValidationReport | 当前验证产物 |
| `agentLogs` | List<AgentStepLog> | Agent 执行轨迹 |

第一阶段可以只保留当前版本，不必实现多版本历史列表。

### 2.3 `AgentStepLog`

用于前端展示过程追踪。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `stepId` | String | 步骤 ID |
| `taskId` | String | 任务 ID |
| `agentName` | String | Agent 或模块名称 |
| `stage` | String | 阶段 |
| `stageStatus` | StageStatus | 阶段状态 |
| `message` | String | 展示文本 |
| `startedAt` | String | 开始时间 |
| `finishedAt` | String | 结束时间 |

## 3. NetworkIntent

`NetworkIntent` 是 Intent Agent 输出的业务意图。

重要边界：

```text
NetworkIntent 不包含具体设备、接口、VLAN、IP。
```

建议字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | String | 任务 ID |
| `intentVersion` | Integer | 意图版本 |
| `rawText` | String | 用户原始输入 |
| `semanticIntentGraph` | SemanticIntentGraph | 语义意图图 |
| `assumptions` | List<Assumption> | 缺省假设 |
| `stageStatus` | StageStatus | 阶段状态 |

### 3.1 `SemanticIntentGraph`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nodes` | List<IntentNode> | 业务对象 |
| `relations` | List<IntentRelation> | 业务关系 |

### 3.2 `IntentNode`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | String | 节点 ID，如 `office` |
| `name` | String | 展示名称 |
| `type` | String | `ZONE`、`EXTERNAL_NETWORK` 等 |
| `description` | String | 描述 |

### 3.3 `IntentRelation`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | String | 关系 ID |
| `type` | String | `ACCESS`、`ISOLATION` |
| `source` | String | 源节点 ID |
| `target` | String | 目标节点 ID |
| `action` | String | `ALLOW` 或 `DENY` |
| `service` | String | 服务类型，Demo 可用 `ANY` |
| `description` | String | 展示说明 |
| `explicit` | Boolean | 是否由用户明确提出 |

### 3.4 `Assumption`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `field` | String | 缺省字段 |
| `value` | String | 默认值 |
| `reason` | String | 原因 |

## 4. NetworkPlan

`NetworkPlan` 是 Planning Agent 输出的网络设计方案，可以包含拓扑、区域、地址、VLAN、路由、安全策略和 `targetEnvironment`，但不包含具体 CLI 命令。

建议字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | String | 任务 ID |
| `intentVersion` | Integer | 来源意图版本 |
| `planVersion` | Integer | 规划版本 |
| `planSummary` | String | 规划摘要 |
| `selectedArchitecture` | SelectedArchitecture | 架构选择 |
| `topology` | Topology | 拓扑 |
| `zones` | List<NetworkZone> | 网络区域 |
| `addressPlan` | List<AddressPlanItem> | 地址规划 |
| `vlanPlan` | List<VlanPlanItem> | VLAN 规划 |
| `routingPlan` | RoutingPlan | 路由规划 |
| `securityPolicyPlan` | List<SecurityPolicyPlanItem> | 安全策略 |
| `natPlan` | NatPlan | NAT 规划 |
| `targetEnvironment` | TargetEnvironment | 目标环境 |
| `stageStatus` | StageStatus | 阶段状态 |

### 4.1 `SelectedArchitecture`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `type` | String | `ROUTER_ON_A_STICK`、`L3_SWITCH_CORE` 等 |
| `reason` | String | 选择理由 |

### 4.2 `Topology`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nodes` | List<TopologyNode> | 设备、主机、外部网络 |
| `links` | List<TopologyLink> | 链路 |

### 4.3 `TopologyNode`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | String | 节点 ID |
| `name` | String | 节点名称 |
| `nodeType` | String | `DEVICE`、`HOST`、`EXTERNAL_NETWORK` |
| `deviceType` | String | `ROUTER`、`SWITCH`，主机可为空 |
| `hostType` | String | `PC`、`SERVER`，设备可为空 |
| `role` | String | `GATEWAY`、`ACCESS` 等 |
| `vendor` | String | 厂商 |
| `zoneId` | String | 所属区域 |

### 4.4 `TopologyLink`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | String | 链路 ID |
| `sourceNode` | String | 源节点 |
| `sourceInterface` | String | 源接口 |
| `targetNode` | String | 目标节点 |
| `targetInterface` | String | 目标接口 |
| `linkType` | String | `ACCESS`、`TRUNK`、`WAN` |

### 4.5 `NetworkZone`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | String | 区域 ID |
| `name` | String | 区域名称 |
| `mappedFromIntentNode` | String | 来源意图节点 |
| `zoneType` | String | `USER_ZONE`、`SERVER_ZONE`、`EXTERNAL_NETWORK` |

### 4.6 `AddressPlanItem`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | String | 地址规划元素 ID，如 `address-office` |
| `zoneId` | String | 区域 ID |
| `subnet` | String | 网段 |
| `gateway` | String | 网关 |
| `sampleIp` | String | Demo 主机示例地址，可选 |

### 4.7 `VlanPlanItem`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | String | VLAN 规划元素 ID，如 `vlan-office` |
| `vlanId` | Integer | VLAN ID |
| `name` | String | VLAN 名称 |
| `zoneId` | String | 区域 ID |
| `accessPorts` | List<PortRef> | 接入口 |

### 4.8 `PortRef`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `deviceId` | String | 设备 ID |
| `interfaceName` | String | 接口名 |

### 4.9 `RoutingPlan`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | String | 路由规划元素 ID，如 `routing-ospf` |
| `protocol` | String | `OSPF`、`STATIC` 等 |
| `area` | String | OSPF 区域，可选 |
| `routers` | List<RoutingRouter> | 路由器配置意图 |
| `defaultRoute` | DefaultRoute | 默认路由 |

### 4.10 `RoutingRouter`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | String | 路由器路由元素 ID，如 `routing-ospf-r1` |
| `deviceId` | String | 设备 ID |
| `routerId` | String | Router ID |
| `advertisedNetworks` | List<String> | 宣告网段 |

### 4.11 `DefaultRoute`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `enabled` | Boolean | 是否启用默认路由 |
| `nextHop` | String | 下一跳或外部网络标识 |

### 4.12 `SecurityPolicyPlanItem`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | String | 安全策略 ID |
| `name` | String | 策略名称 |
| `sourceZone` | String | 源区域 |
| `targetZone` | String | 目标区域 |
| `action` | String | `ALLOW` 或 `DENY` |
| `service` | String | 服务类型 |
| `enforcementPoint` | EnforcementPoint | 执行点 |
| `basedOnIntentRelation` | String | 来源意图关系 ID |

### 4.13 `EnforcementPoint`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `deviceId` | String | 设备 ID |
| `interfaceName` | String | 接口名 |
| `direction` | String | `INBOUND` 或 `OUTBOUND` |

### 4.14 `NatPlan`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | String | NAT 规划元素 ID，如 `nat-internet-access` |
| `enabled` | Boolean | 是否启用 |
| `insideZones` | List<String> | 内部区域 |
| `outsideInterface` | PortRef | 出口接口 |
| `description` | String | 说明 |

### 4.15 `TargetEnvironment`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `vendor` | String | 设备风格，如 `Huawei` |
| `configStyle` | String | 配置风格，如 `CLI` |
| `simulationTarget` | String | `DRY_RUN`、`MININET_RYU` 等 |

## 5. ConfigSet

`ConfigSet` 是 Configuration Agent 输出的配置结果，必须结构化，不要只返回一大段字符串。

建议字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | String | 任务 ID |
| `planVersion` | Integer | 来源规划版本 |
| `configVersion` | Integer | 配置版本 |
| `targetEnvironment` | TargetEnvironment | 目标环境 |
| `generationSummary` | String | 生成摘要 |
| `generationSources` | List<GenerationSource> | 可选，配置生成来源 |
| `deviceConfigs` | List<DeviceConfig> | 设备配置 |
| `endpointConfigs` | List<EndpointConfig> | 主机配置 |
| `rollbackPlan` | RollbackPlan | 可选，整体回滚计划 |
| `warnings` | List<ConfigWarning> | 警告 |
| `stageStatus` | StageStatus | 阶段状态 |

### 5.1 `GenerationSource`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `sourceType` | String | `MOCK_TEMPLATE`、`RULE`、`RAG` 等 |
| `sourceName` | String | 来源名称 |
| `description` | String | 说明 |

当前阶段可固定为 `MOCK_TEMPLATE`。

### 5.2 `DeviceConfig`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `deviceId` | String | 设备 ID |
| `deviceName` | String | 设备名称 |
| `deviceType` | String | 设备类型 |
| `vendor` | String | 厂商 |
| `configText` | String | 完整配置文本 |
| `commandBlocks` | List<CommandBlock> | 结构化配置块 |

### 5.3 `CommandBlock`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `blockId` | String | 配置块 ID |
| `blockType` | String | `INTERFACE`、`VLAN`、`ACL`、`ROUTING`、`NAT` 等 |
| `order` | Integer | 执行顺序 |
| `title` | String | 展示标题 |
| `commands` | List<String> | 命令列表 |
| `explanation` | String | 解释 |
| `rollbackCommands` | List<String> | 回滚命令 |
| `dependsOn` | List<String> | 依赖配置块 |
| `traceRefs` | TraceRefs | 追溯来源 |

### 5.4 `TraceRefs`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `intentRelationIds` | List<String> | 关联的意图关系 |
| `planElementIds` | List<String> | 关联的规划元素 |

### 5.5 `EndpointConfig`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nodeId` | String | 主机节点 ID |
| `nodeType` | String | 节点类型 |
| `zoneId` | String | 所属区域 |
| `commands` | List<String> | Linux / DryRun 主机配置命令 |
| `explanation` | String | 说明 |

### 5.6 `RollbackPlan`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `strategy` | String | 回滚策略，如 `REVERSE_ORDER` |
| `blockIds` | List<String> | 回滚涉及的配置块 |
| `description` | String | 说明 |

## 6. ExecutionReport

`ExecutionReport` 是 Execution Module 输出的执行适配结果。当前阶段为 Mock / DryRun。

Execution Module 不能直接执行 Huawei CLI，必须通过 Execution Adapter 转换为 Mininet/Ryu/DryRun 可执行内容。

建议字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | String | 任务 ID |
| `planVersion` | Integer | 来源规划版本 |
| `configVersion` | Integer | 来源配置版本 |
| `executionVersion` | Integer | 执行版本 |
| `executionMode` | String | `MOCK`、`DRY_RUN`、`MININET_RYU` |
| `executionPlan` | ExecutionPlan | 执行计划 |
| `runtimeState` | RuntimeState | 运行时状态 |
| `testResult` | TestResult | 测试结果 |
| `stageStatus` | StageStatus | 阶段状态 |

### 6.1 `ExecutionPlan`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `adapterType` | String | `DRY_RUN`、`MININET_RYU` |
| `topologyScript` | String | Mininet 拓扑脚本或 Mock 文本 |
| `hostCommands` | List<ExecutionCommand> | 主机命令 |
| `flowRules` | List<FlowRule> | 流表规则，第一阶段可为空 |
| `testCommands` | List<TestCommand> | 测试命令 |

### 6.2 `RuntimeState`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `environmentStatus` | String | `READY`、`MOCK_READY`、`ERROR` |
| `nodes` | List<RuntimeNodeState> | 节点状态 |
| `links` | List<RuntimeLinkState> | 链路状态 |
| `controllerConnected` | Boolean | Ryu 是否连接，Mock 可固定 |

### 6.3 `TestResult`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `connectivityTests` | List<ConnectivityTestResult> | 连通性测试 |
| `policyTests` | List<PolicyTestResult> | 策略测试 |
| `rawLogs` | List<String> | 原始日志 |

`TestResult` 只记录执行结果，不直接判断业务意图是否达成。最终判断交给 Verification Agent。

## 7. ValidationReport

`ValidationReport` 是 Verification Agent 输出的意图验证结果。

建议字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | String | 任务 ID |
| `intentVersion` | Integer | 来源意图版本 |
| `planVersion` | Integer | 来源规划版本 |
| `configVersion` | Integer | 来源配置版本 |
| `executionVersion` | Integer | 来源执行版本 |
| `validationVersion` | Integer | 验证版本 |
| `overallStatus` | ValidationStatus | 验证结论 |
| `summary` | String | 验证摘要 |
| `items` | List<ValidationItem> | 验证项 |
| `suggestions` | List<String> | 建议 |
| `stageStatus` | StageStatus | 阶段状态 |

### 7.1 `ValidationItem`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `itemId` | String | 验证项 ID |
| `name` | String | 验证项名称 |
| `type` | String | `CONNECTIVITY`、`ISOLATION`、`ROUTING`、`SECURITY_POLICY` |
| `expected` | String | 期望 |
| `actual` | String | 实际 |
| `passed` | Boolean | 是否通过 |
| `relatedIntentRelationId` | String | 关联意图关系 |
| `relatedPlanElementIds` | List<String> | 关联规划元素 |
| `relatedConfigBlockIds` | List<String> | 关联配置块 |
| `relatedTestId` | String | 关联测试 |
| `message` | String | 展示说明 |

## 8. Healing 占位模型

当前阶段不创建正式 `mac-tav-healing-agent` Maven 模块。可以在 `mac-tav-model` 中保留以下占位 DTO，供后续扩展：

| 对象 | 说明 |
| --- | --- |
| `RepairPlan` | 修复方案，占位 |
| `RepairAction` | 修复动作，占位 |
| `FailureAnalysis` | 失败分析，占位 |

当前阶段仅要求能表达：

```text
Healing Agent 当前未启用，验证失败时只返回 Mock 建议或 TODO 信息。
```

## 9. 第一阶段 DTO 优先级

建议按下面顺序实现：

1. `NetworkTask`
2. `NetworkWorkspace`
3. `NetworkIntent`
4. `NetworkPlan`
5. `ConfigSet`
6. `ExecutionReport`
7. `ValidationReport`
8. `AgentStepLog`
9. Healing 占位 DTO
