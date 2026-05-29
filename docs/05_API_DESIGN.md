# API 设计

## 1. 文档目标

本文档定义 MAC-TAV HTTP API 契约。API 只暴露 `mac-tav-web` 面向前端的业务入口，不暴露内部专业 Agent A2A 调用细节。

Controller 只接收请求、校验参数、调用 Orchestrator 或查询服务、返回统一响应。

> **Codex 开发规则**：每个 API 节标注了所属 Phase。Codex 在当前阶段开发时，MUST 只实现当前 Phase 及之前 Phase 的 API，MUST NOT 提前实现未来 Phase 的 API 或 Controller。

本文档不展开 DTO 全字段（以 `docs/04_DATA_MODELS.md` 为准），不展开 Agent 代码（以 `docs/09_AGENT_BUILD_GUIDE.md` 为准）。

## 2. API 基本约定

### 2.1 路径前缀与资源域

路径前缀：`/api/v1`

资源域：

| 资源域 | Phase | 说明 |
| --- | --- | --- |
| `/api/v1/tasks` | Phase 1 | 任务 CRUD |
| `/api/v1/workflows` | Phase 2 | 工作流控制 |
| `/api/v1/workspaces` | Phase 2 | Workspace 查询 |
| `/api/v1/artifacts` | Phase 3 | 阶段产物查询 |
| `/api/v1/executions` | Phase 6 | 执行触发与报告 |
| `/api/v1/validations` | Phase 7 | 验证触发与报告 |
| `/api/v1/repairs` | Phase 8 | 修复分析与动作 |
| `/api/v1/views` | Phase 10 | 前端展示视图 |
| `/api/v1/events` | Phase 9 | SSE 与事件历史 |

### 2.2 统一响应格式

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

- `success`：业务处理是否成功。
- `code`：数字业务码。
- `errorCode`：稳定字符串枚举，大写下划线风格，前端优先使用 `errorCode` 做分支判断。
- `message`：简短说明，不承载完整异常堆栈。
- `traceId`：调用链追踪 ID。
- Web 响应不返回完整异常堆栈、API Key、请求头、外部凭据。

文件下载和 SSE 可以不包统一响应，但必须保留 `traceId` 和审计日志。

### 2.3 分页约定

分页参数：`page`（从 1 开始）、`size`、`sortBy`（可选）、`sortDirection`（`ASC`/`DESC`，可选）。

分页响应：

```json
{
  "data": {
    "records": [],
    "page": 1,
    "size": 20,
    "total": 100,
    "hasNext": true
  }
}
```

分页适用于：任务列表、Artifact 列表、时间线、变更记录、事件历史、执行日志。

### 2.4 资源命名约定

- `taskId` → `NetworkTask` ID
- `artifactId` → `NetworkArtifact` ID
- `actionId` → `RepairAction` ID
- `stage` → `WorkflowStage` 枚举值
- `artifactType` → `ArtifactType` 枚举值
- 核心 DTO 时间字段为 `LocalDateTime`，API 响应层通过 Web 序列化配置转为 ISO-8601 字符串

---

## 3. Task API `[Phase 1]`

### 3.1 创建任务

```text
POST /api/v1/tasks
```

Request: `{ "rawText": "...", "description": "..." }`
Response: `{ "taskId": "...", "taskStatus": "CREATED" }`

### 3.2 查询任务

```text
GET /api/v1/tasks/{taskId}
```
返回任务摘要（含 `currentStage`、`taskStatus`），默认不返回完整阶段对象。完整对象通过 Workspace API 或 Artifact API 查询。

### 3.3 查询任务列表

```text
GET /api/v1/tasks
```
支持分页、`status` 过滤。

### 3.4 取消 / 归档 / 删除

```text
POST /api/v1/tasks/{taskId}/cancel
POST /api/v1/tasks/{taskId}/archive
DELETE /api/v1/tasks/{taskId}
```

取消和归档记录审计日志，删除需确认任务不处于 `RUNNING` 状态。

---

## 4. Workflow API `[Phase 2+]`

### 4.1 启动流程

```text
POST /api/v1/workflows/{taskId}/start
```
从 `Intent` 阶段开始推进。Controller 委托 Orchestrator。Orchestrator 通过 `A2aRemoteAgent / AgentCardProvider`（官方 SAA A2A starter）调用远程专业 Agent。

### 4.2 继续流程

```text
POST /api/v1/workflows/{taskId}/continue
```
从 `WAITING_USER` 恢复推进。

### 4.3 重跑阶段

```text
POST /api/v1/workflows/{taskId}/rerun/{stage}
```
重跑指定阶段，生成新版本 Artifact，记录 `WorkspaceChangeRecord`。

### 4.4 从指定阶段继续

```text
POST /api/v1/workflows/{taskId}/continue-from/{stage}
```
从指定阶段重新推进（如 Healing 后重新进入 Planning）。

---

## 5. Workspace API `[Phase 2+]`

### 5.1 查询当前视图

```text
GET /api/v1/workspaces/{taskId}
```
返回 `NetworkWorkspace` 当前视图（含各阶段产物引用和状态）。

### 5.2 查询摘要

```text
GET /api/v1/workspaces/{taskId}/summary
```
返回轻量摘要，用于列表和概览。

### 5.3 查询时间线 / 变更记录

```text
GET /api/v1/workspaces/{taskId}/timeline
GET /api/v1/workspaces/{taskId}/changes
```
分页查询，支持 `stage`、`changeType` 过滤。

---

## 6. Artifact API `[Phase 3+]`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/artifacts/{taskId}` | 查询 Artifact 列表（分页，按 `artifactType`、`stage` 过滤） |
| `GET` | `/api/v1/artifacts/{taskId}/{artifactId}` | 查询指定 Artifact（含 payload 摘要和追溯） |
| `GET` | `/api/v1/artifacts/{taskId}/{artifactId}/payload` | 查询 Artifact 完整 payload JSON |
| `GET` | `/api/v1/artifacts/{taskId}/current/{artifactType}` | 查询当前阶段最新 Artifact |
| `GET` | `/api/v1/artifacts/{taskId}/{artifactId}/versions` | 查询版本列表 |
| `GET` | `/api/v1/artifacts/{taskId}/{artifactId}/diff` | 对比两个版本（`?fromVersion=&toVersion=`） |

---

## 7. Execution API `[Phase 6+]`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/executions/{taskId}/run` | 触发执行阶段（Controller 委托 Orchestrator） |
| `GET` | `/api/v1/executions/{taskId}` | 查询当前 `ExecutionReport` |
| `GET` | `/api/v1/executions/{taskId}/logs` | 分页查询执行日志（脱敏） |

---

## 8. Validation API `[Phase 7+]`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/validations/{taskId}/run` | 触发验证阶段（使用当前意图、规划、配置、执行报告） |
| `GET` | `/api/v1/validations/{taskId}` | 查询当前 `ValidationReport` |
| `GET` | `/api/v1/validations/{taskId}/items` | 查询验证项列表（支持 `status`、`severity`、`traceRef` 过滤） |

验证不直接修改配置，失败时为 RepairPlan 提供证据。

---

## 9. Repair API `[Phase 8+]`

### 9.1 触发修复分析

```text
POST /api/v1/repairs/{taskId}/analyze
```

说明：
- 由 Orchestrator 从当前 Workspace 读取 `ValidationReport` 和失败上下文，不要求前端提交完整 Workspace。
- 输出 `RepairPlan`，不直接执行修复动作。
- Controller 委托 Orchestrator，Orchestrator 通过 `A2aRemoteAgent / AgentCardProvider` 调用 HealingAgent。

### 9.2 查询修复计划

```text
GET /api/v1/repairs/{taskId}
```

### 9.3 确认 / 拒绝修复动作

```text
POST /api/v1/repairs/{taskId}/actions/{actionId}/approve
POST /api/v1/repairs/{taskId}/actions/{actionId}/reject
```

只改变审批状态，不直接绕过 Orchestrator 执行。必须记录审计日志。

### 9.4 应用修复动作

```text
POST /api/v1/repairs/{taskId}/actions/{actionId}/apply
```

由 Orchestrator 根据 `RepairAction.targetStage` 决定重新规划/配置/执行/验证。高风险动作必须要求人工确认。必须记录审计日志和 Workspace 变更。

---

## 10. View API `[Phase 10+]`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/views/{taskId}/topology` | 查询拓扑视图数据 |
| `GET` | `/api/v1/views/{taskId}/config-blocks` | 查询配置块展示视图 |
| `GET` | `/api/v1/views/{taskId}/trace` | 查询追溯关系视图（`?artifactId=`） |

展示模型放在 `mac-tav-web` vo 包，不污染核心 DTO。

---

## 11. Event / SSE API `[Phase 9+]`

### 11.1 SSE 事件流

```text
GET /api/v1/events/{taskId}
Content-Type: text/event-stream
```

事件类型：`task.created`、`workflow.started`、`stage.started`、`stage.completed`、`stage.failed`、`artifact.generated`、`execution.completed`、`validation.completed`、`repair.proposed`、`repair.approved`、`repair.applied`。

SSE 只推送进度和摘要，不推送 API Key、完整模型请求、异常堆栈或内部 A2A 调用细节。前端断线后通过 Workspace API 或 Event history 恢复。

### 11.2 查询事件历史

```text
GET /api/v1/events/{taskId}/history
```
分页查询，支持 `eventType`、时间范围过滤。用于断线恢复和审计辅助。

---

## 12. 错误码规范

### 12.1 格式约定

每个错误码包含：`code`（数字业务码）、`httpStatus`、`errorCode`（稳定字符串枚举）、`message`（简短说明）。

### 12.2 通用错误码 `[Phase 1+]`

| errorCode | code | httpStatus | 说明 |
| --- | ---: | ---: | --- |
| `OK` | `0` | `200` | success |
| `BAD_REQUEST` | `40000` | `400` | 请求参数错误 |
| `UNAUTHORIZED` | `40100` | `401` | 未认证 |
| `FORBIDDEN` | `40300` | `403` | 无权限 |
| `NOT_FOUND` | `40400` | `404` | 资源不存在 |
| `CONFLICT` | `40900` | `409` | 状态冲突 |
| `INTERNAL_ERROR` | `50000` | `500` | 系统内部错误 |

### 12.3 任务 / 工作流 `[Phase 1-2+]`

| errorCode | code | 说明 |
| --- | ---: | --- |
| `TASK_NOT_FOUND` | `40401` | 任务不存在 |
| `TASK_ALREADY_RUNNING` | `40901` | 任务正在运行 |
| `TASK_NOT_RUNNABLE` | `40902` | 当前状态不可运行 |
| `TASK_CANCELLED` | `40903` | 任务已取消 |
| `STAGE_NOT_READY` | `40911` | 阶段尚未就绪 |
| `STAGE_RERUN_NOT_ALLOWED` | `40912` | 不允许重跑 |
| `WORKFLOW_STATE_CONFLICT` | `40913` | 工作流状态冲突 |

### 12.4 Workspace / Artifact `[Phase 2-3+]`

| errorCode | code | 说明 |
| --- | ---: | --- |
| `WORKSPACE_NOT_FOUND` | `40402` | Workspace 不存在 |
| `ARTIFACT_NOT_FOUND` | `40403` | Artifact 不存在 |
| `ARTIFACT_VERSION_NOT_FOUND` | `40404` | 版本不存在 |
| `ARTIFACT_TYPE_INVALID` | `40011` | 类型不合法 |

### 12.5 Agent / A2A `[Phase 2+]`

| errorCode | code | 说明 |
| --- | ---: | --- |
| `AGENT_CARD_NOT_FOUND` | `40421` | Agent Card 不存在 |
| `REMOTE_AGENT_UNAVAILABLE` | `50321` | 远程 Agent 不可用 |
| `A2A_CALL_FAILED` | `50031` | 远程 Agent 调用失败 |
| `AGENT_SCHEMA_INVALID` | `50022` | Agent 输出 Schema 不合法 |
| `AGENT_PARSE_FAILED` | `50023` | Agent 输出解析失败 |
| `AGENT_OUTPUT_INVALID` | `50021` | Agent 输出校验失败 |

### 12.6 Execution / Validation / Repair `[Phase 6-8+]`

| errorCode | code | 说明 |
| --- | ---: | --- |
| `EXECUTION_ADAPTER_FAILED` | `50041` | 执行适配器失败 |
| `EXECUTION_TIMEOUT` | `50441` | 执行超时 |
| `EXECUTION_FORBIDDEN_COMMAND` | `40341` | 命令不在白名单 |
| `VALIDATION_FAILED` | `50051` | 验证执行失败 |
| `REPAIR_PLAN_NOT_FOUND` | `40461` | 修复计划不存在 |
| `REPAIR_ACTION_NOT_APPROVED` | `40961` | 修复动作未确认 |

---

## 13. Internal A2A 边界

专业 Agent 的 A2A Service 不属于 `mac-tav-web` 的 public `/api/v1` HTTP API。

1. 不在本文档设计面向前端的 `/api/v1/a2a/**`。
2. 不把 Agent Card、Nacos 查询和 A2A 协议细节暴露给前端业务 API。
3. Orchestrator 通过官方 SAA A2A starter（`spring-ai-alibaba-starter-a2a-nacos`，参考：<https://java2ai.com/docs/frameworks/agent-framework/advanced/a2a>）的 `A2aRemoteAgent / AgentCardProvider` 发现并调用远程专业 Agent。
4. A2A 调用失败转换为统一错误码，由 Orchestrator 收敛。
5. 如需内部调试/健康检查接口，单独标注为 internal/admin，不进入前端业务 API 契约。

---

## 14. 安全与审计约定

- API Key 不允许硬编码，不出现于请求体、响应体、日志、SSE。
- Web 响应不返回完整异常堆栈。
- Controller 不接收任意 shell 命令，外部执行只能通过 `ExecutionAdapter` 白名单。
- `repair approve / reject / apply`、`rerun`、`cancel`、`archive`、`delete` 必须记录审计日志和 `WorkspaceChangeRecord`。
- 高风险 `RepairAction` 必须支持人工确认。