# API 设计

## 1. 文档目标

本文档定义 MAC-TAV 长期 HTTP API 契约、资源边界和 Controller 边界。

API 面向：

- 前端。
- 外部调用方。
- 后续集成系统。

MAC-TAV 的长期标准架构只有最终 A2A 多 Agent 服务化架构。HTTP API 只暴露 `mac-tav-web` 面向前端和外部系统的业务入口，不暴露内部专业 Agent A2A 调用细节。

Controller 只接收请求、校验参数、调用 Orchestrator 或查询服务、返回统一响应。业务流程由 Orchestrator 编排。Orchestrator 是唯一主编排入口，负责任务状态推进、Workspace 写入、Artifact 版本管理、异常收敛、阶段重跑和修复闭环。

本文档不展开 DTO 全字段，DTO 字段以 `docs/04_DATA_MODELS.md` 为准。本文档不展开 Agent 代码实现，Agent 规范以 `docs/09_AGENT_BUILD_GUIDE.md` 为准。

## 2. API 基本约定

### 2.1 路径前缀

长期 API 前缀为：

```text
/api/v1
```

路径命名 SHOULD 使用资源域，而不是把所有接口挂在 `/tasks/{taskId}/...` 下。

推荐资源域：

- `/api/v1/tasks`
- `/api/v1/workflows`
- `/api/v1/workspaces`
- `/api/v1/artifacts`
- `/api/v1/executions`
- `/api/v1/validations`
- `/api/v1/repairs`
- `/api/v1/views`
- `/api/v1/events`

### 2.2 统一响应格式

所有 JSON API 使用统一响应体。响应体中的 `code` 是数字业务码，`errorCode` 是稳定的字符串错误标识。HTTP Status 与响应体中的 `code` MUST 分开表达：

- HTTP Status 表示 HTTP 层语义，例如 `200`、`400`、`404`、`500`、`502`。
- `body.code` 表示 MAC-TAV 内部业务码，例如 `0`、`40401`、`50031`。
- `body.errorCode` 表示稳定字符串枚举，例如 `OK`、`TASK_NOT_FOUND`、`A2A_CALL_FAILED`。
- 前端可以优先使用 `errorCode` 做分支判断，使用 `code` 做日志检索、指标聚合和兼容映射。

成功响应：

```json
{
  "success": true,
  "code": 0,
  "errorCode": "OK",
  "message": "success",
  "data": {},
  "traceId": "trace-xxx",
  "timestamp": "2026-05-24T10:00:00+08:00"
}
```

失败响应示例。HTTP Status 可以是 `404`，但响应体中的 `code` 是业务码 `40401`：

```json
{
  "success": false,
  "code": 40401,
  "errorCode": "TASK_NOT_FOUND",
  "message": "任务不存在",
  "data": null,
  "traceId": "trace-xxx",
  "timestamp": "2026-05-24T10:00:00+08:00"
}
```

约束：

- 所有 JSON API 使用统一响应。
- 文件下载和 SSE 可以不包一层统一响应，但必须保留 `traceId`、审计日志或等价追踪能力。
- `success` 表示业务处理是否成功。
- `code` MUST 是数字业务码，不使用字符串错误名。
- `errorCode` MUST 是稳定字符串枚举，命名使用大写下划线风格。
- `message` 是面向调用方的简短说明，不承载完整异常堆栈。
- `data` 类型由具体接口决定。
- `traceId` 用于排查问题。
- Web 响应不返回完整异常堆栈。
- Web 响应不返回完整 API Key、请求头、外部凭据等敏感信息。
### 2.3 分页约定

分页请求参数统一使用：

- `page`：当前页码，从 1 开始。
- `size`：每页条数。
- `sortBy`：排序字段，可选。
- `sortDirection`：`ASC` / `DESC`，可选。

分页响应统一放在 `data` 中：

```json
{
  "success": true,
  "code": 0,
  "errorCode": "OK",
  "message": "success",
  "data": {
    "records": [],
    "page": 1,
    "size": 20,
    "total": 100,
    "hasNext": true
  },
  "traceId": "trace-xxx",
  "timestamp": "2026-05-24T10:00:00+08:00"
}
```

必须支持分页或范围查询的接口包括：

- 任务列表。
- Artifact 列表。
- Workspace 时间线。
- Workspace change history。
- Event history。
- 执行日志。

不要在不同接口中混用 `pageSize`、`limit`、`offset` 等多套分页字段。若需要游标分页，应在对应接口中明确说明，并保持响应结构一致。

### 2.4 资源命名约定

- `taskId` 表示 `NetworkTask` ID。
- `artifactId` 表示 `NetworkArtifact` ID。
- `actionId` 表示 `RepairAction` ID。
- `stage` 取值以 `WorkflowStage` 为准。
- `artifactType` 取值以数据模型中的 `ArtifactType` 为准。
- 核心 DTO 内部时间字段以 `docs/04_DATA_MODELS.md` 的 `LocalDateTime` 为准。
- API 响应层可以通过 Web 序列化配置或 Response VO 转换为带时区的 ISO-8601 字符串。

### 2.5 API 文档化约定

长期 SHOULD 提供 Swagger / OpenAPI 文档。

OpenAPI 文件可以由后端注解生成，但注解生成结果必须符合本文档定义的资源边界。

## 3. Controller 与资源边界

长期 Controller SHOULD 按资源域拆分：

| Controller | 资源域 | 主要职责 |
| --- | --- | --- |
| `TaskController` | Task | 创建、查询、列表、取消、归档、删除任务基础资源。 |
| `WorkspaceController` | Workspace | 查询 Workspace 当前视图、摘要、时间线、变更记录。 |
| `ArtifactController` | Artifact | 查询阶段产物、版本、payload、下载、过滤和对比。 |
| `WorkflowController` | Workflow | 启动、继续、阶段重跑、从指定阶段继续。 |
| `ExecutionController` | Execution | 触发执行阶段、查询执行报告、查询执行日志。 |
| `ValidationController` | Validation | 触发验证阶段、查询验证报告、查询验证项。 |
| `RepairController` | Repair | 触发修复分析、查询修复计划、确认 / 拒绝 / 应用修复动作。 |
| `ViewController` | View | 返回前端展示视图，例如拓扑、配置块、追溯视图。 |
| `EventController` | Event / SSE | SSE 订阅、事件历史查询、断线恢复辅助。 |

Controller MUST 遵守：

1. Controller 只放在 `mac-tav-web`。
2. Controller 不直接调用具体 Agent。
3. Controller 不直接调用 `ChatModel` / `ChatClient` / `ReactAgent`。
4. Controller 不构造 Prompt。
5. Controller 不直接修改 `NetworkWorkspace` 内部复杂状态。
6. Controller 不执行外部命令。
7. Controller 不暴露内部 A2A Agent 调用细节。
8. Controller 不把 TaskController 做成全能入口。
9. Controller 通过 Orchestrator 触发流程控制类动作。
10. Controller 通过查询服务读取 Workspace、Artifact、日志、视图等只读资源。

## 4. Task API

Task API 只负责任务基础资源。

它不负责流程启动、Workspace 查询、Artifact 查询、Execution、Validation、Repair 或 SSE。

### 4.1 创建任务

```text
POST /api/v1/tasks
```

请求体包含：

- `rawText`：必填，用户自然语言需求。
- `targetEnvironment`：可选，目标环境偏好。
- `createdBy`：可选，创建人或调用方标识。
- `metadata`：可选，前端或外部系统附加信息。

响应：

- 返回 `taskId`、`taskStatus`、`currentStage`、`createTime`。
- 创建任务只创建 `NetworkTask` 和初始 `NetworkWorkspace`。
- 不要求立即返回完整 `NetworkWorkspace`。
- 不建议通过 `runImmediately` 把创建任务和流程执行混在一起。需要启动流程时调用 Workflow API。

### 4.2 查询任务摘要

```text
GET /api/v1/tasks/{taskId}
```

返回任务摘要：

- `taskId`。
- `rawText`。
- `taskStatus`。
- `currentStage`。
- `createTime`。
- `updateTime`。
- `currentVersions`。
- `currentArtifactRefs`，可选。

默认不返回完整阶段大对象。完整对象通过 Workspace API 或 Artifact API 查询。

### 4.3 查询任务列表

```text
GET /api/v1/tasks
```

支持参数：

- `page`。
- `size`。
- `taskStatus`。
- `currentStage`。
- `keyword`。
- `createdFrom`。
- `createdTo`。
- `createdBy`。

返回分页任务摘要。

### 4.4 取消任务

```text
POST /api/v1/tasks/{taskId}/cancel
```

用于取消 `RUNNING` 或 `WAITING_USER` 状态任务。

约束：

- 取消动作必须记录审计日志。
- 如果任务已经完成，返回明确错误或幂等结果。
- Controller 不直接修改 Workspace，取消状态由 Orchestrator / Model Core 收敛。

### 4.5 归档任务

```text
POST /api/v1/tasks/{taskId}/archive
```

长期建议优先归档，避免丢失审计和追溯信息。

约束：

- 归档必须记录审计日志。
- 归档后是否允许查看 Artifact 由权限策略决定。

### 4.6 删除任务

```text
DELETE /api/v1/tasks/{taskId}
```

是否真实删除由实现决定。若涉及审计、Artifact、执行日志和追溯关系，SHOULD 使用归档替代物理删除。

## 5. Workflow API

Workflow API 负责流程控制资源，包括启动、继续、阶段重跑和从指定阶段继续。

Workflow API 必须调用 Orchestrator。Controller 不直接调用 Agent，不直接调用模型，不构造 Prompt。

### 5.1 启动流程

```text
POST /api/v1/workflows/{taskId}/run
```

说明：

- 用于创建任务后手动启动完整流程。
- Orchestrator 决定当前阶段调用哪个专业 Agent。
- Orchestrator 通过 RemoteAgentTool / A2A Client 调用远程专业 Agent。

### 5.2 继续流程

```text
POST /api/v1/workflows/{taskId}/continue
```

用于用户确认、澄清补充、修复确认后继续流程。

约束：

- 仅适用于可继续的任务状态。
- 不允许 Controller 直接推进 Workspace 内部状态。
- 继续动作必须记录审计或 Workspace 变更。

### 5.3 重跑指定阶段

```text
POST /api/v1/workflows/{taskId}/stages/{stage}/rerun
```

`stage` 以 `WorkflowStage` 为准。

约束：

- 重跑阶段必须由 Orchestrator 控制。
- Controller 不直接调用具体 Agent。
- 重跑必须产生新的 Artifact 版本。
- 重跑必须记录 `WorkspaceChangeRecord`。
- 重跑后后续阶段产物可以被标记为 `SUPERSEDED` 或等待重新生成。

### 5.4 从指定阶段继续

```text
POST /api/v1/workflows/{taskId}/stages/{stage}/continue
```

用于从某个阶段产物继续执行后续阶段。

约束：

- Orchestrator 必须检查阶段依赖是否满足。
- 如果前置 Artifact 不存在或版本不匹配，返回 `STAGE_NOT_READY` 或 `ARTIFACT_VERSION_NOT_FOUND`。
- 阶段继续必须记录 Workspace 变更。

## 6. Workspace API

Workspace API 负责 Workspace 当前视图、摘要、时间线和变更记录。

Workspace API 不负责查询所有历史 payload。历史版本通过 Artifact API 查询。

### 6.1 查询 Workspace 当前视图

```text
GET /api/v1/workspaces/{taskId}
```

返回 `NetworkWorkspace` 当前视图。

说明：

- 可包含 currentIntent、currentPlan、currentConfigSet、currentExecutionReport、currentValidationReport、currentRepairPlan 等当前版本。
- 不强制返回所有历史 Artifact payload。
- Workspace 不存在时返回 `WORKSPACE_NOT_FOUND`。

### 6.2 查询 Workspace 摘要

```text
GET /api/v1/workspaces/{taskId}/summary
```

返回前端工作台摘要：

- taskStatus。
- currentStage。
- 当前阶段产物版本。
- 最近一次执行 / 验证 / 修复摘要。
- 是否等待用户确认。

### 6.3 查询 Workspace 时间线

```text
GET /api/v1/workspaces/{taskId}/timeline
```

返回 AgentExecutionRecord / 前端事件摘要形成的时间线。

支持分页参数：

- `page`。
- `size`。
- `stage`。
- `eventType`。
- `from`。
- `to`。

说明：

- 用于前端展示阶段推进、Agent 调用、Artifact 生成和修复闭环。
- 不返回敏感原始模型请求。
- Tool / MCP / A2A 调用摘要必须脱敏。

### 6.4 查询 Workspace 变更记录

```text
GET /api/v1/workspaces/{taskId}/changes
```

返回 `WorkspaceChangeRecord` 分页列表。

说明：

- 阶段重跑、修复确认、修复应用、人工澄清、取消、归档等动作应进入变更记录。
- 变更记录不替代 Artifact 版本查询。

## 7. Artifact API

Artifact 是独立资源，不以 `/tasks/{taskId}/artifacts/...` 作为长期主路径。

Artifact API 支持版本历史、回放、审计、下载、修复和对比。

### 7.1 查询 Artifact 列表

```text
GET /api/v1/artifacts?taskId={taskId}&artifactType={type}&stage={stage}&status={status}&version={version}
```

支持过滤参数：

- `taskId`。
- `artifactType`。
- `stage`。
- `status`。
- `version`。
- `page`。
- `size`。

返回 `NetworkArtifact` 摘要分页列表。

### 7.2 查询指定 Artifact

```text
GET /api/v1/artifacts/{artifactId}
```

以 `artifactId` 为主键查询指定 Artifact 元数据和摘要。

说明：

- 用于审计、追溯、版本详情入口。
- 大 payload 可以通过 payload 接口单独查询。

### 7.3 查询 Artifact payload

```text
GET /api/v1/artifacts/{artifactId}/payload
```

返回指定 Artifact 的完整 payload。

约束：

- payload 必须来自已保存的 `NetworkArtifact`。
- Controller 不临时拼接模型输出。
- 敏感字段必须脱敏或按权限控制。

### 7.4 下载 Artifact

```text
GET /api/v1/artifacts/{artifactId}/download
```

返回 `text/plain`、`application/json`、`application/zip` 或其他合适的下载格式。

约束：

- 下载内容必须来自已保存的 Artifact / ConfigSet。
- 不由 Controller 临时拼接模型输出。
- 下载行为必须记录审计日志。

### 7.5 查询当前 Artifact

```text
GET /api/v1/artifacts/current?taskId={taskId}&artifactType={type}
```

用于查询某任务某类产物当前版本。

### 7.6 查询 Artifact 版本列表

```text
GET /api/v1/artifacts/versions?taskId={taskId}&artifactType={type}
```

返回某任务某类产物的所有版本摘要。

### 7.7 对比 Artifact

```text
GET /api/v1/artifacts/compare?leftArtifactId={id}&rightArtifactId={id}
```

可选接口，用于前端展示版本差异。

约束：

- 对比结果不生成新的阶段产物。
- 对比结果不改变 Workspace 状态。

## 8. Execution API

Execution API 负责执行阶段资源。

执行必须由 Orchestrator 判断阶段依赖、状态和版本后，再通过 ExecutionAdapter 和白名单 Adapter 完成。API 不接收任意 shell 命令。

### 8.1 触发执行阶段

```text
POST /api/v1/executions/{taskId}/run
```

说明：

- Orchestrator 或用户触发执行阶段。
- Controller 必须委托 Orchestrator，不允许直接调用 ExecutionAdapter。
- Orchestrator 必须检查阶段依赖、任务状态和 Artifact 版本。
- Controller 不接收任意 shell 命令。
- 不允许直接执行用户传入的外部命令。

### 8.2 查询执行报告

```text
GET /api/v1/executions/{taskId}
```

返回当前 `ExecutionReport` 或执行阶段摘要。

说明：

- 执行报告以 `ExecutionReport` 为准。
- 历史执行报告通过 Artifact API 查询。

### 8.3 查询执行日志

```text
GET /api/v1/executions/{taskId}/logs
```

支持参数：

- `page`。
- `size`。
- `from`。
- `to`。
- `level`。

约束：

- 执行日志必须脱敏。
- 大日志必须分页或按时间范围过滤。
- 不返回完整 API Key、请求头、外部凭据。
- 不返回可被直接复制执行的危险命令上下文。

## 9. Validation API

Validation API 负责验证阶段资源。

### 9.1 触发验证阶段

```text
POST /api/v1/validations/{taskId}/run
```

说明：

- 使用当前 `NetworkIntent`、`NetworkPlan`、`ConfigSet`、`ExecutionReport` 生成或刷新 `ValidationReport`。
- Verification 不直接修改配置。
- 验证失败为 RepairPlan 提供证据。
- Controller 必须委托 Orchestrator 判断阶段依赖、状态和版本，不直接调用具体 Agent。

### 9.2 查询验证报告

```text
GET /api/v1/validations/{taskId}
```

返回当前 `ValidationReport`。

历史验证报告通过 Artifact API 查询。

### 9.3 查询验证项

```text
GET /api/v1/validations/{taskId}/items
```

返回 `ValidationItem` 列表。

支持参数：

- `status`。
- `severity`。
- `traceRef`。

## 10. Repair API

Repair API 负责修复分析、修复计划、修复动作确认 / 拒绝 / 应用。

RepairPlan 不直接执行修复。apply 由 Orchestrator 根据 `RepairAction.targetStage` 决定重新规划、重新配置、重新执行、重新验证或请求用户澄清。

### 10.1 触发修复分析

```text
POST /api/v1/repairs/{taskId}/analyze
```

说明：

- 输入当前 `ValidationReport` 和 `NetworkWorkspace`。
- 输出 `RepairPlan`。
- 不直接执行修复动作。
- Controller 必须委托 Orchestrator 判断阶段依赖、状态和版本，不直接调用具体 Agent。

### 10.2 查询修复计划

```text
GET /api/v1/repairs/{taskId}
```

返回当前 `RepairPlan`。

历史修复计划通过 Artifact API 查询。

### 10.3 确认修复动作

```text
POST /api/v1/repairs/{taskId}/actions/{actionId}/approve
```

用于需要人工确认的修复动作。

约束：

- 只改变 RepairAction 审批状态，不直接绕过 Orchestrator 执行。
- 必须记录审计日志。
- 必须记录 Workspace 变更。

### 10.4 拒绝修复动作

```text
POST /api/v1/repairs/{taskId}/actions/{actionId}/reject
```

记录用户拒绝原因，不改变历史 Artifact。

约束：

- 必须记录审计日志。
- 必须记录 Workspace 变更。

### 10.5 应用修复动作

```text
POST /api/v1/repairs/{taskId}/actions/{actionId}/apply
```

说明：

- 由 Orchestrator 根据 `RepairAction.targetStage` 决定重新规划、重新生成配置、重新执行、重新验证或请求用户澄清。
- Controller 必须委托 Orchestrator，不直接调用 ExecutionAdapter 或具体 Agent。
- 高风险动作必须要求人工确认。
- 未确认的高风险动作返回 `REPAIR_ACTION_NOT_APPROVED`。
- apply 必须记录审计日志和 Workspace 变更。
- apply 不允许 Controller 直接修改 Workspace 或直接执行修复命令。

## 11. View API

View API 负责前端展示视图。

View API 返回前端友好的展示模型，不改变 Workspace 状态，不生成新的阶段产物。

### 11.1 查询拓扑视图

```text
GET /api/v1/views/{taskId}/topology
```

返回前端拓扑展示数据。

说明：

- 拓扑视图来自 `NetworkPlan` / `ExecutionReport` / Workspace 当前视图。
- 不替代核心 DTO。
- 不改变 Workspace 状态。

### 11.2 查询配置块视图

```text
GET /api/v1/views/{taskId}/config-blocks
```

返回按设备、端点和 commandBlocks 组织的展示数据。

说明：

- 配置块视图来自 `ConfigSet`。
- 不临时生成新的配置文本。

### 11.3 查询追溯视图

```text
GET /api/v1/views/{taskId}/trace
```

返回前端追溯展示模型。

说明：

- Trace 视图来自 `TraceRefs`、Artifact 和 `WorkspaceChangeRecord`。
- 不生成新的阶段产物。
- 不改变 Workspace 状态。

## 12. Event / SSE API

Event API 负责实时进度、事件订阅和断线恢复。

### 12.1 SSE 事件流

```text
GET /api/v1/events/{taskId}
```

响应：

```text
Content-Type: text/event-stream
```

事件类型至少覆盖：

- `task.created`
- `workflow.started`
- `stage.started`
- `stage.completed`
- `stage.failed`
- `artifact.generated`
- `execution.completed`
- `validation.completed`
- `repair.proposed`
- `repair.approved`
- `repair.applied`

约束：

- SSE 只推送进度和摘要。
- SSE 不推送 API Key、完整模型请求、完整异常堆栈或敏感执行日志。
- SSE 不暴露内部 A2A 调用细节。
- 前端断线后通过 Workspace API、Event history 或 timeline 恢复状态。

### 12.2 查询事件历史

```text
GET /api/v1/events/{taskId}/history
```

支持参数：

- `page`。
- `size`。
- `from`。
- `to`。
- `eventType`。

说明：

- 用于前端断线恢复和审计辅助。
- 不替代 Workspace timeline。
- 不返回敏感日志。

## 13. Internal A2A API 边界

专业 Agent 的 A2A Service 不属于 `mac-tav-web` 的 public `/api/v1` HTTP API。

长期边界：

1. 不在本文档中设计面向前端的 `/api/v1/a2a/**`。
2. 不把 Agent Card、Nacos 查询和 A2A 协议细节暴露给前端业务 API。
3. Orchestrator 通过 RemoteAgentTool / A2A Client 查询 Nacos、解析 Agent Card、调用专业 Agent。
4. RemoteAgentTool / A2A Client 属于 Orchestrator 侧远程调用适配能力，不承担业务编排职责，不写 Workspace，不管理任务状态。
5. A2A 调用失败应转换为统一错误码，例如 `A2A_CALL_FAILED`，并由 Orchestrator 收敛、记录到任务状态和执行日志中。
6. 如后续需要内部调试、健康检查或管理接口，应单独标注为 internal / admin / actuator 类接口，不进入前端业务 API 契约。

## 14. 错误码规范

错误码 SHOULD 按资源域整理。MAC-TAV 对外响应使用统一的 `ApiCode` / `ErrorCode` 口径，每个错误项至少包含：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | `int` | 数字业务码，写入响应体 `body.code`。 |
| `httpStatus` | `int` | HTTP 响应状态码，不等同于 `body.code`。 |
| `errorCode` | `String` | 稳定字符串错误标识，写入响应体 `body.errorCode`。 |
| `message` | `String` | 默认错误说明，可在不泄露内部细节的前提下覆盖。 |

示例口径：

```java
public enum ApiCode {
    OK(0, 200, "OK", "success"),

    BAD_REQUEST(40000, 400, "BAD_REQUEST", "请求参数错误"),
    TASK_NOT_FOUND(40401, 404, "TASK_NOT_FOUND", "任务不存在"),
    WORKSPACE_NOT_FOUND(40402, 404, "WORKSPACE_NOT_FOUND", "Workspace 不存在"),
    ARTIFACT_NOT_FOUND(40403, 404, "ARTIFACT_NOT_FOUND", "Artifact 不存在"),

    AGENT_OUTPUT_INVALID(50021, 500, "AGENT_OUTPUT_INVALID", "Agent 输出校验失败"),
    A2A_CALL_FAILED(50031, 502, "A2A_CALL_FAILED", "远程 Agent 调用失败"),
    EXECUTION_ADAPTER_FAILED(50041, 500, "EXECUTION_ADAPTER_FAILED", "执行适配器失败"),
    INTERNAL_ERROR(50000, 500, "INTERNAL_ERROR", "系统内部错误");

    private final int code;
    private final int httpStatus;
    private final String errorCode;
    private final String message;
}
```

### 14.1 通用错误码

| errorCode | code | httpStatus | 默认说明 |
| --- | ---: | ---: | --- |
| `OK` | `0` | `200` | success |
| `BAD_REQUEST` | `40000` | `400` | 请求参数错误 |
| `INVALID_ARGUMENT` | `40001` | `400` | 请求参数不合法 |
| `UNAUTHORIZED` | `40100` | `401` | 未认证或认证失效 |
| `FORBIDDEN` | `40300` | `403` | 无权限访问该资源 |
| `NOT_FOUND` | `40400` | `404` | 资源不存在 |
| `CONFLICT` | `40900` | `409` | 资源状态冲突 |
| `INTERNAL_ERROR` | `50000` | `500` | 系统内部错误 |

### 14.2 任务 / 工作流错误码

| errorCode | code | httpStatus | 默认说明 |
| --- | ---: | ---: | --- |
| `TASK_NOT_FOUND` | `40401` | `404` | 任务不存在 |
| `TASK_ALREADY_RUNNING` | `40901` | `409` | 任务正在运行 |
| `TASK_NOT_RUNNABLE` | `40902` | `409` | 当前任务状态不可运行 |
| `TASK_CANCELLED` | `40903` | `409` | 任务已取消 |
| `STAGE_NOT_FOUND` | `40411` | `404` | 阶段不存在 |
| `STAGE_NOT_READY` | `40911` | `409` | 阶段尚未就绪 |
| `STAGE_RERUN_NOT_ALLOWED` | `40912` | `409` | 当前阶段不允许重跑 |
| `WORKFLOW_STATE_CONFLICT` | `40913` | `409` | 工作流状态冲突 |

### 14.3 Workspace / Artifact 错误码

| errorCode | code | httpStatus | 默认说明 |
| --- | ---: | ---: | --- |
| `WORKSPACE_NOT_FOUND` | `40402` | `404` | Workspace 不存在 |
| `ARTIFACT_NOT_FOUND` | `40403` | `404` | Artifact 不存在 |
| `ARTIFACT_VERSION_NOT_FOUND` | `40404` | `404` | Artifact 版本不存在 |
| `ARTIFACT_TYPE_INVALID` | `40011` | `400` | Artifact 类型不合法 |

### 14.4 Agent / A2A 错误码

| errorCode | code | httpStatus | 默认说明 |
| --- | ---: | ---: | --- |
| `AGENT_CARD_NOT_FOUND` | `40421` | `404` | Agent Card 不存在 |
| `REMOTE_AGENT_UNAVAILABLE` | `50321` | `503` | 远程 Agent 不可用 |
| `A2A_CALL_FAILED` | `50031` | `502` | 远程 Agent 调用失败 |
| `AGENT_SCHEMA_INVALID` | `50022` | `500` | Agent 输出 Schema 不合法 |
| `AGENT_PARSE_FAILED` | `50023` | `500` | Agent 输出解析失败 |
| `AGENT_OUTPUT_INVALID` | `50021` | `500` | Agent 输出校验失败 |

### 14.5 Execution 错误码

| errorCode | code | httpStatus | 默认说明 |
| --- | ---: | ---: | --- |
| `EXECUTION_ADAPTER_NOT_FOUND` | `40441` | `404` | 执行适配器不存在 |
| `EXECUTION_ADAPTER_FAILED` | `50041` | `500` | 执行适配器失败 |
| `EXECUTION_TIMEOUT` | `50441` | `504` | 执行超时 |
| `EXECUTION_FORBIDDEN_COMMAND` | `40341` | `403` | 命令不在白名单内 |

### 14.6 Validation / Repair 错误码

| errorCode | code | httpStatus | 默认说明 |
| --- | ---: | ---: | --- |
| `VALIDATION_REPORT_NOT_FOUND` | `40451` | `404` | 验证报告不存在 |
| `VALIDATION_FAILED` | `50051` | `500` | 验证执行失败 |
| `REPAIR_PLAN_NOT_FOUND` | `40461` | `404` | 修复计划不存在 |
| `REPAIR_ACTION_NOT_FOUND` | `40462` | `404` | 修复动作不存在 |
| `REPAIR_ACTION_NOT_APPROVED` | `40961` | `409` | 修复动作尚未确认 |
| `REPAIR_ACTION_REJECTED` | `40962` | `409` | 修复动作已拒绝 |
## 15. 安全与审计约定

安全要求：

- API Key 不允许硬编码。
- API Key 不允许出现在请求体、响应体、日志、SSE 中。
- 不在日志、响应、SSE 中泄露完整请求头、外部凭据或敏感环境变量。
- Web 响应不返回完整异常堆栈。
- SSE 不推送敏感内容。
- Controller 不接收任意 shell 命令。
- Controller 不直接执行外部命令。
- 外部命令执行只能通过 ExecutionAdapter 白名单。
- 执行日志、模型日志、工具日志、A2A 调用摘要应脱敏。

审计要求：

- `repair apply` 必须记录审计日志。
- `repair approve` / `repair reject` 必须记录审计日志。
- `rerun` 必须记录审计日志和 `WorkspaceChangeRecord`。
- `cancel` 必须记录审计日志。
- `archive` / `delete` 必须记录审计日志。
- Artifact download SHOULD 记录审计日志。
- 高风险 `RepairAction` 必须支持人工确认。

## 16. curl 示例归属

本文档只保留 API 契约，不展开详细 curl 示例。

常用手动调试命令见 `docs/08_RUN_AND_TEST.md`。

## 17. 本文档与其他文档的分工

- 本文档：HTTP API 路径、请求响应、资源边界、进度推送、错误码、Controller 边界。
- `docs/02_MAVEN_MODULES.md`：Maven 模块和依赖边界。
- `docs/03_MODULE_DESIGN.md`：业务模块职责和流程。
- `docs/04_DATA_MODELS.md`：核心 DTO 字段和数据结构。
- `docs/06_DEV_PLAN.md`：长期实现路线。
- `docs/08_RUN_AND_TEST.md`：运行、测试和 curl 调试示例。
- `docs/09_AGENT_BUILD_GUIDE.md`：真实 Spring AI Alibaba Agent 构建规范。
