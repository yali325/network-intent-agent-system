# Phase 10 Frontend API Contract Notes

本文档是 Phase 10 前端工程的 P0 合同冻结记录。核对范围只包括
`mac-tav-web` 中真实 Controller、DTO、VO、SSE 代码，以及必要的
`ApiResponse`、`PageResult`、`ErrorCode`、`WorkflowJob` 类型。

事实源优先级：真实 Controller + DTO/VO + 实际 Java 类型为准；
`docs/05_API_DESIGN.md` 仅作为对照资料。

## 1. Phase 10 前端可用的真实 API 清单

### 1.1 通用响应合同

- 普通 JSON API 外层为 `ApiResponse<T>`。
- 字段：`success`、`code`、`errorCode`、`message`、`data`、`timestamp`。
- 分页为 `PageResult<T>`。
- 字段：`items`、`page`、`size`、`total`。
- 注意：真实分页字段不是 `records`，也没有 `hasNext`。
- 来源：`ApiResponse.java`、`PageResult.java`、`ErrorCode.java`。

### 1.2 tasks

| API | Request | Response data | Async jobId | 前端场景 | 注意事项 | 来源 |
| --- | --- | --- | --- | --- | --- | --- |
| `POST /api/v1/tasks` | body: `CreateTaskRequest { rawText, targetEnvironmentHint, createdBy }` | `TaskSummaryResponse { taskId, taskStatus, currentStage, createTime }` | 否 | Intent Submit 创建任务 | 只创建 task/workspace，不自动启动完整 workflow；创建后需调用 workflow API | `TaskController.java`、`CreateTaskRequest.java`、`TaskSummaryResponse.java` |
| `GET /api/v1/tasks/{taskId}/jobs` | path: `taskId` | `List<WorkflowJob>` | 否 | 任务详情页展示该 task 的 job 历史 | Controller 会把 `requestPayloadJson` 置空；不要依赖请求 payload | `TaskController.java`、`WorkflowJob.java` |

真实代码未确认 `GET /api/v1/tasks`、`GET /api/v1/tasks/{taskId}`、cancel、archive、delete。
Phase 10 前端不得直接依赖这些路径。

### 1.3 workflows

| API | Request | Response data | Async jobId | 前端场景 | 注意事项 | 来源 |
| --- | --- | --- | --- | --- | --- | --- |
| `POST /api/v1/workflows/{taskId}/start` | path: `taskId` | `WorkflowJobSubmitResponse` | 是 | 启动完整工作流 | 返回 job，不同步返回阶段产物；用 job、workspace、artifact、event history 观察进度 | `WorkflowController.java`、`WorkflowJobSubmitResponse.java` |
| `POST /api/v1/workflows/{taskId}/run` | path: `taskId` | `WorkflowJobSubmitResponse` | 是 | 单独运行 Intent 阶段 | 真实代码请求 `WorkflowStage.INTENT` | `WorkflowController.java` |
| `POST /api/v1/workflows/{taskId}/plan` | path: `taskId` | `WorkflowJobSubmitResponse` | 是 | 单独运行 Planning 阶段 | 前置产物不足时后端可能返回阶段未就绪错误 | `WorkflowController.java` |
| `POST /api/v1/workflows/{taskId}/config` | path: `taskId` | `WorkflowJobSubmitResponse` | 是 | 单独运行 Configuration 阶段 | 前置产物不足时后端可能返回阶段未就绪错误 | `WorkflowController.java` |
| `POST /api/v1/workflows/{taskId}/rerun/{stage}` | path: `taskId`, `stage: WorkflowStage` | `WorkflowJobSubmitResponse` | 是 | 指定阶段重跑 | `stage` 使用 Java 枚举值，如 `PLANNING`、`CONFIGURATION` | `WorkflowController.java` |
| `POST /api/v1/workflows/{taskId}/continue-from/{stage}` | path: `taskId`, `stage: WorkflowStage` | `WorkflowJobSubmitResponse` | 是 | 从指定阶段继续推进 | 真实路径不是 `/continue` | `WorkflowController.java` |

### 1.4 workflow jobs

| API | Request | Response data | Async jobId | 前端场景 | 注意事项 | 来源 |
| --- | --- | --- | --- | --- | --- | --- |
| `GET /api/v1/workflows/jobs/{jobId}` | path: `jobId` | `WorkflowJob` | 否 | job 状态轮询、toast、任务进度条 | Controller 会把 `requestPayloadJson` 置空 | `WorkflowController.java`、`WorkflowJob.java` |
| `GET /api/v1/tasks/{taskId}/jobs` | path: `taskId` | `List<WorkflowJob>` | 否 | task 维度 job 历史 | 同上 | `TaskController.java`、`WorkflowJob.java` |

`WorkflowJob` 字段：`jobId`、`taskId`、`requestedStage`、`jobType`、
`jobStatus`、`requestedBy`、`startTime`、`finishTime`、`errorCode`、
`errorMessage`、`traceId`、`createTime`、`updateTime`。真实 API 不返回
`requestPayloadJson`。

`WorkflowJobStatus`：`PENDING`、`RUNNING`、`SUCCESS`、`FAILED`、
`CANCELLED`、`INTERRUPTED`。

`WorkflowJobType`：`FULL_WORKFLOW`、`RUN_STAGE`、`RERUN_STAGE`、
`CONTINUE_FROM_STAGE`、`REPAIR_ANALYZE`、`REPAIR_APPLY`。

### 1.5 workspaces

| API | Request | Response data | Async jobId | 前端场景 | 注意事项 | 来源 |
| --- | --- | --- | --- | --- | --- | --- |
| `GET /api/v1/workspaces/{taskId}` | path: `taskId` | `NetworkWorkspace` | 否 | Mission Control 初始快照、断线恢复、详情主数据源 | Workspace 是当前视图；SSE 不是唯一数据源 | `WorkspaceController.java`、`NetworkWorkspace.java` |
| `GET /api/v1/workspaces/{taskId}/timeline` | query: `stage?`, `eventType?`, `from?`, `to?`, `page=1`, `size=20` | `PageResult<WorkspaceEvent>` | 否 | 时间线、SSE 断线补偿 | `from/to` 为 ISO date-time；分页字段是 `items` | `WorkspaceController.java`、`WorkspaceEvent.java` |
| `GET /api/v1/workspaces/{taskId}/changes` | query: `stage?`, `changeType?`, `from?`, `to?`, `page=1`, `size=20` | `PageResult<WorkspaceChangeRecord>` | 否 | Artifact switch、重跑、修复动作审计 | 记录 workspace view 的变化，不替代 artifact history | `WorkspaceController.java`、`WorkspaceChangeRecord.java` |

真实代码未实现 `GET /api/v1/workspaces/{taskId}/summary`。

### 1.6 artifacts

| API | Request | Response data | Async jobId | 前端场景 | 注意事项 | 来源 |
| --- | --- | --- | --- | --- | --- | --- |
| `GET /api/v1/artifacts/{taskId}` | query: `artifactType?`, `stage?`, `page=1`, `size=20` | `PageResult<ArtifactSummaryResponse>` | 否 | Artifact Inspector 列表 | `artifactType`、`stage` 必须是 Java 枚举名 | `ArtifactController.java`、`ArtifactSummaryResponse.java` |
| `GET /api/v1/artifacts/{taskId}/{artifactId}` | path: `taskId`, `artifactId` | `ArtifactSummaryResponse` | 否 | Artifact 元数据详情 | 不含完整 payload JSON | `ArtifactController.java` |
| `GET /api/v1/artifacts/{taskId}/{artifactId}/payload` | path: `taskId`, `artifactId` | `ArtifactPayloadResponse { metadata, payloadJson }` | 否 | 查看完整产物 JSON | 只在用户打开详情时拉取，避免列表加载大 payload | `ArtifactController.java`、`ArtifactPayloadResponse.java` |
| `GET /api/v1/artifacts/{taskId}/current/{artifactType}` | path: `taskId`, `artifactType` | `ArtifactSummaryResponse` | 否 | 当前版本 badge、阶段产物入口 | `artifactType` 必填且必须合法 | `ArtifactController.java` |
| `GET /api/v1/artifacts/{taskId}/{artifactId}/versions` | query: `page=1`, `size=20` | `PageResult<ArtifactSummaryResponse>` | 否 | 版本列表 | 以 artifactId 定位同源版本链 | `ArtifactController.java` |
| `GET /api/v1/artifacts/{taskId}/{artifactId}/diff` | query: `fromVersion?`, `toVersion?` | `ArtifactDiffResponse { from, to }` | 否 | Diff 面板 | 当前实现返回两个 payload snapshot，不是结构化 patch | `ArtifactController.java`、`ArtifactDiffResponse.java` |
| `POST /api/v1/artifacts/{taskId}/{artifactId}/switch` | body: `ArtifactSwitchRequest { artifactType, reason, actor }` | `ArtifactVersionSwitchResult` | 否 | 手动切换 workspace 当前 artifact 指针 | 只切换 current pointer，不执行真实网络回滚，不调用 ExecutionAdapter，不执行 shell | `ArtifactController.java`、`ArtifactSwitchRequest.java`、`ArtifactVersionSwitchResult.java` |

`ArtifactSummaryResponse` 字段：`artifactId`、`taskId`、`artifactType`、
`version`、`stage`、`status`、`payloadType`、`payloadSummary`、`createTime`、
`createdBy`、`traceRefs`。

### 1.7 events/SSE

| API | Request | Response data | Async jobId | 前端场景 | 注意事项 | 来源 |
| --- | --- | --- | --- | --- | --- | --- |
| `GET /api/v1/events/{taskId}` | path: `taskId`; `Accept: text/event-stream` | SSE event data: `WorkspaceEventSummary` JSON string | 否 | Mission Control 实时增量事件 | 连接前会校验 workspace 存在；先读 workspace/timeline，再接 SSE；断线后用 history/timeline 补偿 | `SseController.java`、`SseEmitterRegistry.java`、`SseEventMapper.java`、`WorkspaceEventSummary.java` |
| `GET /api/v1/events/{taskId}/history` | query: `stage?`, `eventType?`, `from?`, `to?`, `page=1`, `size=20` | `PageResult<WorkspaceEvent>` | 否 | 事件历史、断线恢复、审计 | 与 workspace timeline 都来自事件历史查询；不要把 SSE 当唯一数据源 | `EventController.java`、`WorkspaceEvent.java` |

SSE 连接成功后会先收到 `eventType/name = connected` 的事件。SSE payload
字段：`eventId`、`taskId`、`eventType`、`stage`、`eventTime`、`severity`、
`title`、`message`、`relatedArtifactId`、`relatedRecordId`、`traceId`、
`payloadSummary`。

### 1.8 executions

| API | Request | Response data | Async jobId | 前端场景 | 注意事项 | 来源 |
| --- | --- | --- | --- | --- | --- | --- |
| `POST /api/v1/executions/{taskId}/run` | path: `taskId` | `WorkflowJobSubmitResponse` | 是 | 手动触发 Execution 阶段 | 返回 job，不同步返回 `ExecutionReport` | `ExecutionController.java` |
| `GET /api/v1/executions/{taskId}` | path: `taskId` | `ExecutionReport` | 否 | 执行结果/证据面板 | 当前 workspace 没有 execution report 时返回 `ARTIFACT_NOT_FOUND` | `ExecutionController.java` |

真实代码未实现 `GET /api/v1/executions/{taskId}/logs`。

### 1.9 validations

| API | Request | Response data | Async jobId | 前端场景 | 注意事项 | 来源 |
| --- | --- | --- | --- | --- | --- | --- |
| `POST /api/v1/validations/{taskId}/run` | path: `taskId` | `WorkflowJobSubmitResponse` | 是 | 手动触发 Verification 阶段 | 返回 job，不同步返回 `ValidationReport` | `ValidationController.java` |
| `GET /api/v1/validations/{taskId}` | path: `taskId` | `ValidationReport` | 否 | Validation Evidence 总览 | 当前 workspace 没有 validation report 时返回 `ARTIFACT_NOT_FOUND` | `ValidationController.java` |
| `GET /api/v1/validations/{taskId}/items` | path: `taskId` | `List<ValidationItem>` | 否 | 验证条目列表 | 真实代码不支持 `status`、`severity`、`traceRef` 查询参数过滤 | `ValidationController.java` |

### 1.10 repairs

| API | Request | Response data | Async jobId | 前端场景 | 注意事项 | 来源 |
| --- | --- | --- | --- | --- | --- | --- |
| `POST /api/v1/repairs/{taskId}/analyze` | path: `taskId` | `WorkflowJobSubmitResponse` | 是 | 触发修复分析 | job 完成后查询 `GET /repairs/{taskId}` | `RepairController.java` |
| `GET /api/v1/repairs/{taskId}` | path: `taskId` | `RepairPlan` | 否 | Repair Cockpit 读取修复计划 | 当前 workspace 没有 repair plan 时返回 `REPAIR_PLAN_NOT_FOUND` | `RepairController.java` |
| `POST /api/v1/repairs/{taskId}/actions/{actionId}/approve` | body optional: `RepairActionDecisionRequest { actor, comment }` | `RepairPlan` | 否 | 人工批准修复动作 | 只改变审批状态，不直接执行动作 | `RepairController.java`、`RepairActionDecisionRequest.java` |
| `POST /api/v1/repairs/{taskId}/actions/{actionId}/reject` | body optional: `RepairActionDecisionRequest { actor, comment }` | `RepairPlan` | 否 | 人工拒绝修复动作 | 只改变审批状态 | `RepairController.java` |
| `POST /api/v1/repairs/{taskId}/actions/{actionId}/apply` | path: `taskId`, `actionId` | `WorkflowJobSubmitResponse` | 是 | 应用已批准修复动作 | `ROLLBACK` 不猜目标 artifact；需要显式 artifact switch API | `RepairController.java` |

## 2. API 状态分类

### 2.1 真实代码已实现，Phase 10 前端可以直接使用

- `POST /api/v1/tasks`
- `GET /api/v1/tasks/{taskId}/jobs`
- `POST /api/v1/workflows/{taskId}/start`
- `POST /api/v1/workflows/{taskId}/run`
- `POST /api/v1/workflows/{taskId}/plan`
- `POST /api/v1/workflows/{taskId}/config`
- `POST /api/v1/workflows/{taskId}/rerun/{stage}`
- `POST /api/v1/workflows/{taskId}/continue-from/{stage}`
- `GET /api/v1/workflows/jobs/{jobId}`
- `GET /api/v1/workspaces/{taskId}`
- `GET /api/v1/workspaces/{taskId}/timeline`
- `GET /api/v1/workspaces/{taskId}/changes`
- `GET /api/v1/artifacts/{taskId}`
- `GET /api/v1/artifacts/{taskId}/{artifactId}`
- `GET /api/v1/artifacts/{taskId}/{artifactId}/payload`
- `GET /api/v1/artifacts/{taskId}/current/{artifactType}`
- `GET /api/v1/artifacts/{taskId}/{artifactId}/versions`
- `GET /api/v1/artifacts/{taskId}/{artifactId}/diff`
- `POST /api/v1/artifacts/{taskId}/{artifactId}/switch`
- `GET /api/v1/events/{taskId}`
- `GET /api/v1/events/{taskId}/history`
- `POST /api/v1/executions/{taskId}/run`
- `GET /api/v1/executions/{taskId}`
- `POST /api/v1/validations/{taskId}/run`
- `GET /api/v1/validations/{taskId}`
- `GET /api/v1/validations/{taskId}/items`
- `POST /api/v1/repairs/{taskId}/analyze`
- `GET /api/v1/repairs/{taskId}`
- `POST /api/v1/repairs/{taskId}/actions/{actionId}/approve`
- `POST /api/v1/repairs/{taskId}/actions/{actionId}/reject`
- `POST /api/v1/repairs/{taskId}/actions/{actionId}/apply`

### 2.2 文档中出现，但真实 Controller 未确认或未实现，Phase 10 不得直接依赖

- `GET /api/v1/tasks`
- `GET /api/v1/tasks/{taskId}`
- `POST /api/v1/tasks/{taskId}/cancel`
- `POST /api/v1/tasks/{taskId}/archive`
- `DELETE /api/v1/tasks/{taskId}`
- `POST /api/v1/workflows/{taskId}/continue`
- `GET /api/v1/workspaces/{taskId}/summary`
- `GET /api/v1/executions/{taskId}/logs`
- `GET /api/v1/views/{taskId}/topology`
- `GET /api/v1/views/{taskId}/config-blocks`
- `GET /api/v1/views/{taskId}/trace`
- `GET /api/v1/validations/{taskId}/items` 的 `status`、`severity`、`traceRef` 查询过滤。

### 2.3 真实代码中存在，但 docs/05_API_DESIGN.md 未列出或路径不一致

- `GET /api/v1/tasks/{taskId}/jobs` 真实存在，`docs/05_API_DESIGN.md`
  的 Task API 小节未列出；Phase 9 文档和当前状态文档已提到该路径。
- `POST /api/v1/workflows/{taskId}/continue-from/{stage}` 真实存在；
  `docs/05_API_DESIGN.md` 同时出现旧的 `/continue`，前端必须使用真实路径。
- `PageResult<T>` 真实字段为 `items/page/size/total`；
  `docs/05_API_DESIGN.md` 分页示例使用 `records/hasNext`，前端合同以真实类型为准。
- `ApiResponse<T>` 真实类型没有 `traceId` 字段；
  `docs/05_API_DESIGN.md` 示例包含 `traceId`，前端不得假设外层一定有该字段。
- `GET /api/v1/validations/{taskId}/items` 真实存在但不支持文档中描述的过滤查询参数。

## 3. 旧路径和不确定路径修正表

| 候选结论 | 真实 Controller 结论 | Phase 10 修正 |
| --- | --- | --- |
| SSE 是否应使用 `GET /api/v1/events/{taskId}` | 一致，`SseController.java` 已实现，`text/event-stream` | 使用该路径 |
| Event history 是否应使用 `GET /api/v1/events/{taskId}/history` | 一致，`EventController.java` 已实现 | 使用该路径 |
| Workspace 是否应使用 `GET /api/v1/workspaces/{taskId}` | 一致，`WorkspaceController.java` 已实现 | 使用该路径 |
| Timeline 是否应使用 `GET /api/v1/workspaces/{taskId}/timeline` | 一致，`WorkspaceController.java` 已实现 | 使用该路径 |
| Artifact 列表是否应使用 `GET /api/v1/artifacts/{taskId}` | 一致，`ArtifactController.java` 已实现 | 使用该路径 |
| Workflow 启动是否应使用 `POST /api/v1/workflows/{taskId}/start` | 一致，已实现 | 使用该路径 |
| Workflow run 是否应使用 `POST /api/v1/workflows/{taskId}/run` | 一致，已实现；实际为 Intent 阶段 run | 使用该路径，但 UI 文案建议标注为 Intent/Run stage |
| Workflow planning 是否应使用 `POST /api/v1/workflows/{taskId}/plan` | 一致，已实现 | 使用该路径 |
| Workflow config 是否应使用 `POST /api/v1/workflows/{taskId}/config` | 一致，已实现 | 使用该路径 |
| `GET /api/v1/tasks` 是否存在 | 未实现 | 不要依赖 |
| `GET /api/v1/tasks/{taskId}` 是否存在 | 未实现 | 不要依赖；详情用 workspace API |
| `POST /api/v1/workflows/{taskId}/continue` 是否存在 | 未实现 | 改为 `POST /api/v1/workflows/{taskId}/continue-from/{stage}` |
| `GET /api/v1/workspaces/{taskId}/summary` 是否存在 | 未实现 | Phase 10 暂用 `GET /api/v1/workspaces/{taskId}` 自行取摘要 |
| `GET /api/v1/executions/{taskId}/logs` 是否存在 | 未实现 | 不要依赖；执行证据用 `ExecutionReport` 和 artifacts |
| `/api/v1/views/**` 是否存在 | 未实现 | Phase 10 不依赖 View API，必要时前端从 workspace/artifact 派生视图 |

## 4. Phase 10 前端不能做的事情

- 不直接调用 Agent / A2A / Nacos。
- 不新增 A2A fallback。
- 不新增手写 A2A Controller。
- 不新增 Agent Card publisher。
- 不直接执行配置或 shell。
- 不修改后端业务逻辑。
- 不修改具体 Agent 模块。
- 不修改 Agent 的 `ResponseSchema -> Parser -> DTO -> Validator` 链路。
- Artifact switch 只切换 Workspace 当前 artifact 指针，不代表真实网络回滚。
- SSE 只作为增量事件，不作为唯一数据源。
- 普通前端开发和自动化测试不得依赖真实 MySQL / Redis / Nacos / Agent。
- 不假设长任务 POST 同步返回阶段 DTO；Phase 9 后这些接口返回 job。
- 不把 artifact payload 列表化预取；完整 payload 只通过显式 payload API 拉取。

## 5. P1-P7 编码阶段 API 优先级建议

### P1 工程骨架

- 冻结基础类型：`ApiResponse<T>`、`PageResult<T>`、`WorkflowJob`、
  `WorkflowJobSubmitResponse`、`TaskSummaryResponse`、`NetworkWorkspace`。
- 前端类型生成/手写时必须使用真实 `items` 分页字段。
- 先准备 API client、错误处理、枚举兼容层和 taskId/jobId 路由参数。

### P2 mock/real adapter

- fixture 优先级：task create response、job submit response、job status
  `PENDING/RUNNING/SUCCESS/FAILED/INTERRUPTED`、workspace snapshot、
  timeline page、SSE connected event、artifact summary page。
- mock adapter 必须模拟异步：POST 返回 jobId，后续 job 查询和 workspace/artifact
  查询才出现结果。
- mock 不依赖真实 MySQL / Redis / Nacos / Agent。

### P3 Intent Submit + Workflow Job

- 必用 API：`POST /api/v1/tasks`、`POST /api/v1/workflows/{taskId}/start`、
  `GET /api/v1/workflows/jobs/{jobId}`、`GET /api/v1/tasks/{taskId}/jobs`。
- 创建任务后用 workspace route 展示任务，不调用不存在的 task detail API。
- fixture 需要覆盖重复提交/已有运行 job 的错误分支。

### P4 Mission Control + SSE + Timeline

- 必用 API：`GET /api/v1/workspaces/{taskId}`、
  `GET /api/v1/events/{taskId}`、`GET /api/v1/events/{taskId}/history`、
  `GET /api/v1/workspaces/{taskId}/timeline`。
- 页面启动顺序建议：先拉 workspace + timeline/history，再连接 SSE。
- 断线恢复：重新拉 history/timeline，不依赖浏览器 EventSource 缓存。

### P5 Artifact Inspector + Diff + Switch

- 必用 API：artifact list、metadata、payload、current、versions、diff、switch。
- fixture 需要覆盖 `ArtifactSummaryResponse`、`ArtifactPayloadResponse`、
  `ArtifactDiffResponse`、`ArtifactVersionSwitchResult`。
- switch UI 文案必须明确“切换当前指针，不执行真实网络回滚”。

### P6 Validation Evidence + Repair Cockpit

- Validation API：`POST /api/v1/validations/{taskId}/run`、
  `GET /api/v1/validations/{taskId}`、`GET /api/v1/validations/{taskId}/items`。
- Repair API：analyze、get plan、approve、reject、apply。
- Execution API：`POST /api/v1/executions/{taskId}/run`、
  `GET /api/v1/executions/{taskId}`。
- fixture 需要覆盖 validation missing、repair plan missing、repair approve/reject、
  repair apply async job、rollback 被拒绝后改用 artifact switch 的操作引导。

### P7 真实联调

- 环境：MySQL 执行 `deploy/mysql/phase9_schema.sql`，Redis 本地可用，
  Web 手动启动；真实 A2A/Nacos/DashScope 只在明确联调时启用。
- smoke test 入口：
  `POST /api/v1/tasks` -> `POST /api/v1/workflows/{taskId}/start` ->
  `GET /api/v1/workflows/jobs/{jobId}` ->
  `GET /api/v1/workspaces/{taskId}` ->
  `GET /api/v1/events/{taskId}/history` ->
  `GET /api/v1/artifacts/{taskId}`。
- SSE smoke：连接 `GET /api/v1/events/{taskId}`，确认收到 `connected`
  事件，并在后台事件持久化后收到增量 summary。
- 不在 Codex 中长期运行 `spring-boot:run`、`npm run dev` 或其他前台服务。

## 6. 文档与代码不一致点

### 6.1 docs/05_API_DESIGN.md 与真实 Controller

- `docs/05_API_DESIGN.md` 当前文件在 PowerShell 读取时中文呈现乱码；
  可辨认路径和代码片段仍可作为对照，但 Phase 10 合同不以其文字描述为事实源。
- Task list/detail/cancel/archive/delete 在文档中出现，但真实 Controller 未实现。
- Workflow `/continue` 在文档中出现，但真实 Controller 未实现；真实路径为
  `/continue-from/{stage}`。
- Workspace summary 在文档中出现，但真实 Controller 未实现。
- Execution logs 在文档中出现，但真实 Controller 未实现。
- View API `/api/v1/views/**` 在文档中出现，但真实 Controller 未实现。
- Validation items 文档描述支持过滤，但真实 Controller 不接收过滤 query。
- 文档分页示例使用 `records`、`hasNext`；真实 `PageResult` 使用
  `items`、`page`、`size`、`total`。
- 文档统一响应示例包含 `traceId`；真实 `ApiResponse` 无 `traceId` 字段。
- ErrorCode 文档中的部分 code 值与真实 `ErrorCode.java` 不一致，前端应优先使用
  `errorCode` 字符串，不依赖数字 code。

### 6.2 docs/08_RUN_AND_TEST.md 前端联调注意

- `docs/08_RUN_AND_TEST.md` 的 API debugging 小节列出的是 common current
  endpoints，不是完整前端合同；完整合同以本文档和真实 Controller 为准。
- 联调默认生产路径需要 MySQL + Redis；普通前端开发和自动化测试应使用 mock/fixture，
  不依赖真实 MySQL / Redis。
- `VITE_API_BASE_URL` 已列为前端 API base URL 环境变量，P1 工程骨架应接入。
- Codex 不得长期挂起 `spring-boot:run` 或 `npm run dev`；真实联调由人工启动服务。
- A2A/Nacos/DashScope 是真实全链路联调需求，不是普通前端开发前置条件。

### 6.3 PHASE_09_HANDOFF.md 对 Phase 10 的影响

- Phase 9 后所有公共长任务 POST 返回 `WorkflowJobSubmitResponse.jobId`，
  前端必须使用 job polling + workspace/artifact/event history 组合观察结果。
- MySQL 是权威历史；Redis 只用于实时事件、SSE fan-out 和 task lock。
- Artifact switch 只更新 workspace 当前 artifact 引用和 current version 字段，
  不执行 shell、不调用 ExecutionAdapter、不推送配置。
- Startup recovery 会把失锁的 active job 标记为 `INTERRUPTED`；前端需要展示
  interrupted job，并为后续 rerun/continue-from 预留操作入口。
- `ROLLBACK` repair apply 不猜目标 artifact；需要用户显式调用 artifact switch。

### 6.4 真实代码存在但文档缺失的前端可用接口

- `GET /api/v1/tasks/{taskId}/jobs`
- `GET /api/v1/workspaces/{taskId}/changes`
- `POST /api/v1/workflows/{taskId}/continue-from/{stage}` 作为真实 continue
  入口，需要替换旧 `/continue` 认知。
