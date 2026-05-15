# API 设计

## 1. 文档目标

本文档定义 MAC-TAV 长期 HTTP API 契约。

API 面向：

- 前端。
- 外部调用方。
- 后续集成系统。

Controller 只接收请求、校验参数、调用 Orchestrator 或查询服务、返回统一响应。业务流程由 Orchestrator 编排。

本文档不展开 DTO 全字段，DTO 字段以 `docs/04_DATA_MODELS.md` 为准。本文档不展开 Agent 代码实现，Agent 规范以 `docs/09_AGENT_BUILD_GUIDE.md` 为准。

## 2. API 基本约定

### 2.1 路径前缀

长期 API 前缀为：

```text
/api/v1
```

如果当前代码仍使用 `/api`，后续应迁移到 `/api/v1`。

### 2.2 统一响应格式

成功响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "trace-xxx",
  "timestamp": "2026-xx-xxTxx:xx:xx"
}
```

失败响应：

```json
{
  "success": false,
  "code": "TASK_NOT_FOUND",
  "message": "任务不存在",
  "data": null,
  "traceId": "trace-xxx",
  "timestamp": "2026-xx-xxTxx:xx:xx"
}
```

约束：

- `data` 类型由具体接口决定。
- `traceId` 用于排查问题。
- 不返回完整异常堆栈。
- 不返回 API Key、请求头、外部凭据等敏感信息。

### 2.3 分页约定

列表接口使用：

- `page`
- `size`
- `sort`
- `order`

分页响应建议：

```json
{
  "items": [],
  "page": 1,
  "size": 20,
  "total": 100
}
```

### 2.4 API 文档化约定

长期 SHOULD 提供 OpenAPI / Swagger 文档。

OpenAPI 文件可以由后端注解生成，也可以在 `docs/openapi.yaml` 中维护。本文档只描述接口设计，不要求本次生成 OpenAPI 文件。

## 3. 任务 API

### 3.1 创建任务

```text
POST /api/v1/tasks
```

请求体包含：

- `rawText`：必填，用户自然语言需求。
- `runImmediately`：可选，创建后是否立即执行。
- `targetEnvironment`：可选，目标环境偏好。

响应：

- 异步执行时返回 `taskId`、`taskStatus`、`currentStage`。
- 不要求立即返回完整 `NetworkWorkspace`。

### 3.2 查询任务摘要

```text
GET /api/v1/tasks/{taskId}
```

返回任务摘要：

- `taskId`
- `rawText`
- `taskStatus`
- `currentStage`
- `createdAt`
- `updatedAt`
- `latestVersions`

默认不返回所有阶段大对象。完整对象通过 Workspace 或阶段产物接口查询。

### 3.3 查询任务列表

```text
GET /api/v1/tasks
```

支持参数：

- `page`
- `size`
- `taskStatus`
- `currentStage`
- `keyword`
- `createdFrom`
- `createdTo`

返回分页任务摘要。

### 3.4 取消任务

```text
POST /api/v1/tasks/{taskId}/cancel
```

用于取消 `RUNNING` 或 `WAITING_USER` 状态任务。如果任务已经完成，返回明确错误或幂等结果。

### 3.5 删除或归档任务

```text
DELETE /api/v1/tasks/{taskId}
POST /api/v1/tasks/{taskId}/archive
```

是否真实删除由实现决定。长期建议优先归档，避免丢失审计和追溯信息。

## 4. Workspace API

### 4.1 查询完整 Workspace

```text
GET /api/v1/tasks/{taskId}/workspace
```

返回 `NetworkWorkspace` 当前视图。

说明：

- 包含 currentIntent、currentPlan、currentConfigSet、currentExecutionReport、currentValidationReport、currentRepairPlan 等当前版本。
- 历史版本通过 Artifact API 查询。
- 如果 workspace 不存在，返回 `TASK_NOT_FOUND`。

### 4.2 查询 Workspace 时间线

```text
GET /api/v1/tasks/{taskId}/timeline
```

返回 AgentExecutionRecord / AgentStepLog 的前端展示列表。

说明：

- 用于前端展示 Agent 执行轨迹。
- 不返回敏感原始模型请求。
- Tool / MCP 调用摘要必须脱敏。

## 5. 阶段产物 API

这些接口用于前端按步骤加载当前阶段结果。

| 接口 | 返回 |
| --- | --- |
| `GET /api/v1/tasks/{taskId}/intent` | 当前 `NetworkIntent` |
| `GET /api/v1/tasks/{taskId}/plan` | 当前 `NetworkPlan` |
| `GET /api/v1/tasks/{taskId}/config` | 当前 `ConfigSet` |
| `GET /api/v1/tasks/{taskId}/execution` | 当前 `ExecutionReport` |
| `GET /api/v1/tasks/{taskId}/validation` | 当前 `ValidationReport` |
| `GET /api/v1/tasks/{taskId}/repair` | 当前 `RepairPlan` |

约束：

- 对应阶段尚未生成时返回 `STAGE_NOT_READY`。
- 任务不存在时返回 `TASK_NOT_FOUND`。
- DTO 字段以 `docs/04_DATA_MODELS.md` 为准。

## 6. Artifact API

长期系统需要支持版本历史、回放、审计和修复。

### 6.1 查询任务 Artifact 列表

```text
GET /api/v1/tasks/{taskId}/artifacts
```

支持参数：

- `artifactType`
- `stage`
- `version`

返回 `NetworkArtifact` 摘要列表。

### 6.2 查询指定 Artifact

```text
GET /api/v1/tasks/{taskId}/artifacts/{artifactId}
```

返回指定 artifact 的 payload 和元数据。

### 6.3 查询某阶段指定版本产物

```text
GET /api/v1/tasks/{taskId}/artifacts/{artifactType}/versions/{version}
```

用于自愈、回滚、对比、审计。Artifact 数据结构以 `docs/04_DATA_MODELS.md` 为准。

## 7. 流程控制 API

### 7.1 启动或继续任务流程

```text
POST /api/v1/tasks/{taskId}/run
```

用于创建任务后手动启动，也可用于 `WAITING_USER` 后继续执行。

### 7.2 重跑指定阶段

```text
POST /api/v1/tasks/{taskId}/stages/{stage}/rerun
```

`stage` 可选：

- `intent`
- `planning`
- `configuration`
- `execution`
- `verification`
- `healing`

约束：

- 重跑阶段必须由 Orchestrator 控制。
- Controller 不直接调用具体 Agent。
- 重跑会生成新的 artifact 版本。
- 重跑后后续阶段产物可能被标记为 `SUPERSEDED`。

### 7.3 从某阶段继续执行

```text
POST /api/v1/tasks/{taskId}/stages/{stage}/continue
```

用于从某个阶段产物继续执行后续阶段。

## 8. 进度推送 API

### 8.1 SSE 事件流

```text
GET /api/v1/tasks/{taskId}/events
```

响应：

```text
Content-Type: text/event-stream
```

事件类型建议：

- `task.created`
- `stage.started`
- `stage.completed`
- `stage.failed`
- `agent.tool-called`
- `agent.model-called`
- `artifact.generated`
- `validation.completed`
- `repair.proposed`
- `task.completed`
- `task.failed`

SSE 只推送进度和摘要，不推送敏感模型请求、API Key 或完整异常堆栈。前端断线后通过 workspace / timeline 接口恢复状态。

## 9. Execution API

### 9.1 触发执行阶段

```text
POST /api/v1/tasks/{taskId}/execution/run
```

说明：

- Orchestrator 或用户触发执行阶段。
- 具体执行由 ExecutionAdapter 控制。
- API 不接收任意 shell 命令。

### 9.2 查询执行报告

```text
GET /api/v1/tasks/{taskId}/execution
```

该接口已在阶段产物 API 中定义。

### 9.3 下载执行日志

```text
GET /api/v1/tasks/{taskId}/execution/logs
```

返回脱敏后的执行日志。日志量大时应支持分页或时间范围过滤。

## 10. Verification API

### 10.1 触发重新验证

```text
POST /api/v1/tasks/{taskId}/validation/run
```

使用当前 `NetworkIntent`、`NetworkPlan`、`ConfigSet`、`ExecutionReport` 重新生成 `ValidationReport`。不直接修改配置。

### 10.2 查询验证报告

```text
GET /api/v1/tasks/{taskId}/validation
```

该接口已在阶段产物 API 中定义。

## 11. Healing / Repair API

### 11.1 触发修复分析

```text
POST /api/v1/tasks/{taskId}/repair/analyze
```

输入当前 `ValidationReport` 和 Workspace，输出 `RepairPlan`。不直接执行修复动作。

### 11.2 查询修复方案

```text
GET /api/v1/tasks/{taskId}/repair
```

返回当前 `RepairPlan`。

### 11.3 确认修复动作

```text
POST /api/v1/tasks/{taskId}/repair/actions/{actionId}/approve
```

用于需要人工确认的修复动作。只改变 RepairAction 状态，不直接绕过 Orchestrator 执行。

### 11.4 应用修复动作

```text
POST /api/v1/tasks/{taskId}/repair/actions/{actionId}/apply
```

由 Orchestrator 根据 `RepairAction.targetStage` 决定重新规划、重新生成配置、重新执行或询问用户。

### 11.5 拒绝修复动作

```text
POST /api/v1/tasks/{taskId}/repair/actions/{actionId}/reject
```

记录用户拒绝原因，不改变历史 artifact。

## 12. 展示与下载 API

### 12.1 查询拓扑视图

```text
GET /api/v1/tasks/{taskId}/views/topology
```

返回前端拓扑展示数据，可由 `NetworkPlan` 转换生成，不替代核心 DTO。

### 12.2 查询配置块视图

```text
GET /api/v1/tasks/{taskId}/views/config-blocks
```

返回按设备和 commandBlocks 组织的展示数据。

### 12.3 下载配置文本

```text
GET /api/v1/tasks/{taskId}/config/download
```

返回 `text/plain` 或 `application/zip`。下载内容必须来自 `ConfigSet`，不由 Controller 临时拼模型输出。

## 13. 错误码规范

通用：

- `OK`
- `BAD_REQUEST`
- `UNAUTHORIZED`
- `FORBIDDEN`
- `NOT_FOUND`
- `INTERNAL_ERROR`

任务：

- `TASK_NOT_FOUND`
- `TASK_ALREADY_RUNNING`
- `TASK_NOT_RUNNABLE`
- `TASK_CANCELLED`

阶段：

- `STAGE_NOT_READY`
- `STAGE_RUN_FAILED`
- `STAGE_RERUN_NOT_ALLOWED`

Agent：

- `AGENT_CALL_FAILED`
- `AGENT_SCHEMA_INVALID`
- `AGENT_OUTPUT_INVALID`

模型：

- `MODEL_CALL_FAILED`
- `MODEL_RATE_LIMITED`
- `MODEL_TIMEOUT`

工具：

- `TOOL_CALL_FAILED`
- `MCP_CALL_FAILED`
- `A2A_CALL_FAILED`

执行：

- `EXECUTION_ADAPTER_FAILED`
- `UNSAFE_COMMAND_REJECTED`
- `EXECUTION_TIMEOUT`

验证与修复：

- `VALIDATION_FAILED`
- `REPAIR_PLAN_NOT_FOUND`
- `REPAIR_ACTION_NOT_FOUND`
- `REPAIR_ACTION_NOT_APPROVED`

## 14. Controller 边界

Controller 只做：

1. 接收请求。
2. 参数校验。
3. 调用 Orchestrator 或查询服务。
4. 返回统一响应。

Controller 不做：

1. 意图解析。
2. 网络规划。
3. 配置生成。
4. 执行适配。
5. 验证判断。
6. 修复决策。
7. 构造 Prompt。
8. 直接调用 `ChatModel` / `ReactAgent`。
9. 直接执行 shell。
10. 直接修改 `NetworkWorkspace` 内部复杂状态。

## 15. 安全与审计约定

- API Key 不允许出现在请求体、响应体、日志中。
- Web 响应不返回完整异常堆栈。
- 外部命令执行只能通过 ExecutionAdapter 白名单。
- 执行日志、模型日志、工具日志应脱敏。
- 重要操作如 cancel、rerun、repair apply 应记录审计日志。
- 鉴权和权限设计可以后续独立成文档，本文只保留接口安全边界。

## 16. curl 示例归属

本文档只保留 API 契约，不展开详细 curl 示例。

常用手动调试命令见 `docs/08_RUN_AND_TEST.md`。

## 17. 本文档与其他文档的分工

- 本文档：HTTP API 路径、请求响应、进度推送、错误码、Controller 边界。
- `docs/02_MAVEN_MODULES.md`：Maven 模块和依赖边界。
- `docs/03_MODULE_DESIGN.md`：业务模块职责和流程。
- `docs/04_DATA_MODELS.md`：核心 DTO 字段和数据结构。
- `docs/06_DEV_PLAN.md`：长期实现路线。
- `docs/08_RUN_AND_TEST.md`：运行、测试和 curl 调试示例。
- `docs/09_AGENT_BUILD_GUIDE.md`：真实 Spring AI Alibaba Agent 构建规范。
