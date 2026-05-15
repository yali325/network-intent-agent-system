# API 设计

本文档定义 Demo 阶段的 Web API。Controller 只放在 `mac-tav-web`，业务流程由 `mac-tav-orchestrator` 编排。

当前阶段 API 面向 Demo 展示，不追求生产级完整性。

## 1. API 基本约定

### 1.1 路径前缀

```text
/api
```

### 1.2 响应格式

建议统一响应结构：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {}
}
```

错误响应示例：

```json
{
  "success": false,
  "code": "TASK_NOT_FOUND",
  "message": "任务不存在",
  "data": null
}
```

### 1.3 当前阶段执行方式

当前 Demo 建议先采用同步执行：

```text
提交任务 -> 后端立即跑完 Mock 全流程 -> 返回完整 NetworkWorkspace
```

后续再扩展为异步任务和 SSE 进度推送。

## 2. 提交并运行 Demo 任务

### 2.1 接口

```http
POST /api/demo/tasks
```

### 2.2 请求体

```json
{
  "rawText": "构建一个办公区和访客区隔离的网络，两个区域都能访问互联网，办公区可以访问服务器，访客区不能访问服务器，采用 OSPF。"
}
```

### 2.3 响应体

返回完整 `NetworkWorkspace`。

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "task": {
      "taskId": "task-10001",
      "rawText": "构建一个办公区和访客区隔离的网络，两个区域都能访问互联网，办公区可以访问服务器，访客区不能访问服务器，采用 OSPF。",
      "taskStatus": "COMPLETED",
      "currentStage": "VERIFICATION"
    },
    "currentIntentVersion": 1,
    "currentPlanVersion": 1,
    "currentConfigVersion": 1,
    "currentExecutionVersion": 1,
    "currentValidationVersion": 1,
    "intent": {},
    "plan": {},
    "configSet": {},
    "executionReport": {},
    "validationReport": {},
    "agentLogs": []
  }
}
```

## 3. 查询任务 Workspace

### 3.1 接口

```http
GET /api/tasks/{taskId}
```

### 3.2 用途

根据任务 ID 返回当前 `NetworkWorkspace`。

当前阶段如果使用内存存储，应用重启后任务可以丢失，这是可接受的。

## 4. 查询阶段产物

这些接口用于前端按 Tab 或步骤视图单独加载阶段结果。

### 4.1 查询意图

```http
GET /api/tasks/{taskId}/intent
```

返回 `NetworkIntent`。

### 4.2 查询规划

```http
GET /api/tasks/{taskId}/plan
```

返回 `NetworkPlan`。

### 4.3 查询配置

```http
GET /api/tasks/{taskId}/config
```

返回 `ConfigSet`。

### 4.4 查询执行结果

```http
GET /api/tasks/{taskId}/execution
```

返回 `ExecutionReport`。

### 4.5 查询验证报告

```http
GET /api/tasks/{taskId}/validation
```

返回 `ValidationReport`。

### 4.6 查询 Agent 日志

```http
GET /api/tasks/{taskId}/logs
```

返回 `List<AgentStepLog>`。

## 5. 预留异步接口

第一阶段可以不实现。后续如果要展示实时进度，可增加：

```http
POST /api/tasks
GET /api/tasks/{taskId}/events
POST /api/tasks/{taskId}/run
```

含义：

| 接口 | 说明 |
| --- | --- |
| `POST /api/tasks` | 创建任务，不立即执行或后台执行 |
| `GET /api/tasks/{taskId}/events` | SSE 推送任务阶段进度 |
| `POST /api/tasks/{taskId}/run` | 手动触发执行 |

## 6. 预留阶段重跑接口

第一阶段可以不实现。后续用于 Debug 和自愈。

```http
POST /api/tasks/{taskId}/stages/intent/rerun
POST /api/tasks/{taskId}/stages/planning/rerun
POST /api/tasks/{taskId}/stages/configuration/rerun
POST /api/tasks/{taskId}/stages/execution/rerun
POST /api/tasks/{taskId}/stages/verification/rerun
```

## 7. Controller 边界

Controller 只做：

1. 接收请求。
2. 基础参数校验。
3. 调用 Orchestrator 或查询服务。
4. 返回统一响应。

Controller 不做：

1. 意图解析。
2. 网络规划。
3. 配置生成。
4. 执行适配。
5. 验证判断。
6. 直接操作复杂状态。

Controller 所在模块必须是 `mac-tav-web`。业务流程由 `mac-tav-orchestrator` 调用 `mac-tav-intent-agent`、`mac-tav-planning-agent`、`mac-tav-configuration-agent`、`mac-tav-execution` 和 `mac-tav-verification-agent` 完成。

## 8. 错误码建议

| 错误码 | 含义 |
| --- | --- |
| `OK` | 成功 |
| `BAD_REQUEST` | 请求参数错误 |
| `TASK_NOT_FOUND` | 任务不存在 |
| `STAGE_NOT_READY` | 阶段产物不存在 |
| `PIPELINE_FAILED` | 流程执行失败 |
| `INTERNAL_ERROR` | 未知错误 |

## 9. curl 示例

运行 Demo 任务：

```bash
curl -X POST http://localhost:8080/api/demo/tasks \
  -H "Content-Type: application/json" \
  -d "{\"rawText\":\"构建一个办公区和访客区隔离的网络，两个区域都能访问互联网，办公区可以访问服务器，访客区不能访问服务器，采用 OSPF。\"}"
```

查询任务：

```bash
curl http://localhost:8080/api/tasks/task-10001
```
