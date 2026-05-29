# 运行、测试与集成验证说明

本文档定义 MAC-TAV 的运行、测试、手动集成验证、curl 调试示例和常见问题处理方式。

本文档不重新定义 API 契约、DTO 全字段、Agent 架构或 Maven 模块设计。对应内容见：

- API 设计：`docs/05_API_DESIGN.md`
- 测试场景：`docs/07_TEST_DATA_AND_SCENARIOS.md`
- Agent 构建规范：`docs/09_AGENT_BUILD_GUIDE.md`

API 路径、资源边界、Controller 边界和统一响应格式 MUST 以 `docs/05_API_DESIGN.md` 为准。本文档中的 curl 只作为运行调试入口，不替代 API 契约。

## 1. 文档目标

本文档只负责：

1. 运行命令。
2. 测试命令。
3. 环境变量。
4. 手动验证。
5. 常见问题。

本文档同步长期 A2A 多 Agent 服务化架构。`mac-tav-web` 只承载 Web / API / SSE 入口，Orchestrator 通过 A2aRemoteAgent / AgentCardProvider（官方 SAA A2A starter） 调用注册到 Nacos 的专业 Agent。本文档不描述本地 Agent 聚合启动路线。

## 2. 基础环境要求

| 环境 | 说明 |
| --- | --- |
| JDK | 版本以 `pom.xml` 为准 |
| Maven | 用于后端多模块构建和测试 |
| Node.js / npm | 用于前端安装、开发和构建 |
| Docker | 用于后续 Mininet / Ryu / MySQL / Redis / Qdrant 等依赖 |
| 操作系统 | Windows + Docker Desktop / WSL2 可以使用，但要注意端口、路径和文件锁 |
| Nacos | 长期服务化运行用于专业 Agent 服务发现和 Agent Card 注册 / 发现 |
| A2A 运行环境或协议支持 | 长期服务化运行用于 Orchestrator 调用远程专业 Agent |
| DashScope / OpenAI Compatible Key | 仅用于真实 Agent 手动验证或真实集成验证，不用于自动化测试 |
| Redis / MySQL / Qdrant | 可作为长期持久化、SSE、向量检索等依赖规划；当前开发不要求一次性全部具备 |

运行前 SHOULD 确认：

- 后端端口未被占用。
- Docker Desktop / WSL2 可用。
- 本地环境变量按需配置。
- 不在日志或命令历史中暴露真实 API Key。
- 长期 A2A 服务化验证前，Nacos、Agent Card、A2A Service 和 A2aRemoteAgent / AgentCardProvider（官方 SAA A2A starter） 配置可被发现和连通。

## 3. 后端构建命令

在项目根目录运行：

```bash
mvn compile
```

运行全部测试：

```bash
mvn test
```

单模块测试示例：

```bash
mvn -pl mac-tav-intent-agent -am test
mvn -pl mac-tav-planning-agent -am test
mvn -pl mac-tav-configuration-agent -am test
mvn -pl mac-tav-verification-agent -am test
mvn -pl mac-tav-healing-agent -am test
mvn -pl mac-tav-orchestrator -am test
mvn -pl mac-tav-web -am test
```

说明：

- `mac-tav-orchestrator` 可以运行模块测试，但不作为独立 Spring Boot 服务启动。
- `mac-tav-web` 是 Web / API / SSE 入口，并通过依赖的 Orchestrator 承载主编排流程。
- 专业 Agent 服务化后，可以分别对 `mac-tav-intent-agent`、`mac-tav-planning-agent`、`mac-tav-configuration-agent`、`mac-tav-verification-agent`、`mac-tav-healing-agent` 运行模块测试。
- 自动化测试不得调用真实外部模型 API。Parser / Validator 可使用 `docs/07_TEST_DATA_AND_SCENARIOS.md` 中的固定样例 JSON 做离线回归测试；Tool / MCP / A2A 异常分支可使用测试夹具验证。不得用 Stub ChatModel、Fake ReactAgent、Mock Tool 或测试 Agent Bean 替代真实业务主链路。
- 如果 Windows 文件锁导致 `mvn clean` 失败，不要反复 clean。
- 可以先运行 `mvn compile` 或 `mvn test`。
- 清理 `target` 前 SHOULD 关闭正在运行的 Java 进程和 IDE 占用。

## 4. 后端启动方式

### 4.1 长期 A2A 服务化启动顺序

长期运行方式以 A2A 多 Agent 服务化架构为准：

1. 启动 Nacos。
2. 启动当前阶段需要验证的专业 Agent 服务，例如 `mac-tav-intent-agent`、`mac-tav-planning-agent`、`mac-tav-configuration-agent`、`mac-tav-verification-agent`、`mac-tav-healing-agent`。
3. 确认专业 Agent 已注册到 Nacos。
4. 确认 Agent Card 可被查询。
5. 启动 `mac-tav-web`。
6. `mac-tav-web` 通过依赖的 Orchestrator 承载主编排流程。
7. Orchestrator 通过 A2aRemoteAgent / AgentCardProvider（官方 SAA A2A starter） 查询 Nacos、读取 Agent Card，并通过 A2A 调用专业 Agent。
8. `mac-tav-orchestrator` 不单独作为服务启动。

启动 Web / API / SSE 入口可使用：

```bash
mvn -pl mac-tav-web -am spring-boot:run
```

该命令只启动 Web / API / SSE 入口，不是本地 Agent 聚合入口。

Codex 执行要求：

- 不要在 Codex 中裸跑长期占用前台的 `spring-boot:run`。
- 如果 Codex 需要验证启动，只允许短时间观察日志。
- 看到 `Started MacTavApplication` 后应停止进程。
- 不要让启动进程长期卡住。

## 5. 前端运行方式

进入前端目录：

```bash
cd mac-tav-frontend
```

安装依赖：

```bash
npm install
```

开发运行：

```bash
npm run dev
```

构建：

```bash
npm run build
```

说明：

- 不要在 Codex 中长期挂起 `npm run dev`。
- Vite proxy 或 `VITE_API_BASE_URL` SHOULD 指向后端。
- 前端构建失败时先检查 Node.js 版本和依赖安装日志。

## 6. 环境变量配置

常用环境变量：

| 环境变量 | 说明 |
| --- | --- |
| `aliApi-key` | DashScope API Key |
| `DASHSCOPE_CHAT_MODEL` | DashScope ChatModel 名称 |
| `SERVER_PORT` | 后端端口 |
| `VITE_API_BASE_URL` | 前端访问后端的基础地址 |
| `NACOS_SERVER_ADDR` | 长期 A2A 服务化运行时的 Nacos 地址 |
| `A2A_BASE_URL` / `A2A_ENDPOINT` | 专业 Agent A2A Service 地址或协议入口，名称以实际实现为准 |
| `REDIS_URL` / `MYSQL_URL` / `QDRANT_URL` | 长期持久化、SSE、向量检索等依赖配置，按实际落地启用 |

要求：

- API Key 不写入 `application.yml`，使用环境变量 `aliApi-key`。
- API Key 不提交仓库。
- 日志不打印完整 API Key。
- 不在命令历史中暴露真实 Key。
- `aliApi-key` 只用于真实 Agent 手动验证或真实集成验证。
- 自动化测试不依赖真实 API Key。
- A2A / Nacos 配置通过 `spring.ai.alibaba.a2a` 前缀的 `application.yml` + SAA starter 自动装配，不手写注册代码（参考：<https://java2ai.com/docs/frameworks/agent-framework/advanced/a2a>）。

## 7. Agent 测试规则

### 7.1 Parser / Validator 离线测试

Parser / Validator 离线测试 MUST：

- 使用 `docs/07_TEST_DATA_AND_SCENARIOS.md` 中的固定样例 JSON。
- 验证 `ResponseSchema -> DTO` 转换。
- 验证 Validator 合法输出和非法输出。
- 验证 Agent 输出跨阶段字段时被拒绝。
- 不调用真实模型。
- 不构造 Fake Agent 主链路。

### 7.2 真实 Agent 手动验证

真实 Agent 手动验证 MUST：

- 使用真实 `ChatModel`。
- API Key 通过环境变量注入。
- 验证真实 Agent 输出 `ResponseSchema`。
- 验证 Parser / Validator 后得到项目 DTO。
- 检查 Agent 是否越界输出。

### 7.3 A2A 集成验证

A2A 集成验证 MUST：

- 启动 Nacos。
- 启动目标专业 Agent 服务。
- 确认 Agent Card 可发现。
- 启动 `mac-tav-web`。
- 通过 Workflow API 触发 Orchestrator。
- 验证 Orchestrator 通过 A2A 调用专业 Agent。
- 验证结果写入 `NetworkWorkspace` / `NetworkArtifact` / `WorkspaceEvent`。

明确禁止：

- 不用 Mock Agent / Mock Tool 验证业务主链路。
- 不用测试 Agent Bean 替代真实 Agent 验证 Orchestrator 到专业 Agent 的长期调用链。
- 不让 Controller 直接调用具体 Agent。
- 自动化测试不得调用真实外部模型 API。

## 8. API 测试方式

长期 API 前缀为 `/api/v1`。API 契约、资源边界、请求响应结构以 `docs/05_API_DESIGN.md` 为准。本文档只提供手动调试示例。

curl 只访问 `mac-tav-web` 暴露的 `/api/v1` 业务 API，不直接调用专业 Agent A2A Service，不把 A2A Service 暴露为前端 public API。

常用调试入口：

- 任务基础资源：`POST /api/v1/tasks`、`GET /api/v1/tasks/{taskId}`、`GET /api/v1/tasks`
- 流程控制：`POST /api/v1/workflows/{taskId}/start`
- Workspace 当前视图：`GET /api/v1/workspaces/{taskId}`
- Artifact 查询：`GET /api/v1/artifacts/{taskId}`
- 执行报告：`GET /api/v1/executions/{taskId}`
- 验证报告：`GET /api/v1/validations/{taskId}`
- 修复计划：`GET /api/v1/repairs/{taskId}`
- 前端视图：`GET /api/v1/views/{taskId}/topology`
- 事件流：`GET /api/v1/events/{taskId}`

Orchestrator 通过 A2aRemoteAgent / AgentCardProvider（官方 SAA A2A starter） 调用远程专业 Agent。

创建任务示例：

```bash
curl -X POST http://localhost:8080/api/v1/tasks ^
  -H "Content-Type: application/json" ^
  -d "{\"rawText\":\"构建办公区和访客区隔离网络，办公区可访问服务器，访客区不可访问服务器。\"}"
```

启动流程示例：

```bash
curl -X POST http://localhost:8080/api/v1/workflows/{taskId}/start ^
  -H "Content-Type: application/json" ^

```

查询 Workspace 示例：

```bash
curl http://localhost:8080/api/v1/workspaces/{taskId}
```

查询 Artifact 示例：

```bash
curl "http://localhost:8080/api/v1/artifacts/{taskId}?artifactType=CONFIG_SET"
```

查询执行、验证和修复结果示例：

```bash
curl http://localhost:8080/api/v1/executions/{taskId}
curl http://localhost:8080/api/v1/validations/{taskId}
curl http://localhost:8080/api/v1/repairs/{taskId}
```

## 9. SSE 验证方式

使用 curl：

```bash
curl -N http://localhost:8080/api/v1/events/{taskId}
```

或使用浏览器 `EventSource`。

SSE 约束：

- 只推送进度事件和摘要。
- 不推送 API Key、完整模型请求、完整异常堆栈。
- 不暴露内部 A2A 调用细节。
- 前端断线后通过 Workspace API、Event history 或 timeline 恢复状态。

事件历史查询示例：

```bash
curl http://localhost:8080/api/v1/events/{taskId}/history
```

## 10. 手动真实模型验证

手动验证真实 DashScope 前 MUST 设置：

- `aliApi-key`

真实模型验证属于手动集成验证，不是自动化单元测试。真实模型验证必须经过 `ResponseSchema -> Parser -> DTO -> Validator`。

手动验证 SHOULD 记录：

- 输入。
- 输出摘要。
- 错误码。
- 模型调用耗时。
- Validator 结果。
- `AgentExecutionRecord` / `WorkspaceEvent` 摘要。

检查重点：

- IntentAgent 输出的 `NetworkIntent` 不包含设备、接口、VLAN、IP、CLI。
- PlanningAgent 输出的 `NetworkPlan` 不包含 CLI。
- ConfigurationAgent 输出的 `ConfigSet` 包含 commandBlocks、traceRefs、rollbackCommands。
- VerificationAgent 输出的 `ValidationReport` 不直接修改配置。
- HealingAgent 输出的 `RepairPlan` 不直接执行修复。

## 11. Mininet / Ryu 手动验证

ExecutionAdapter 接入后再启用 Mininet / Ryu 验证。

说明：

- Mininet / Ryu 不是 Agent 打通前置条件。
- Mininet / Ryu 验证不替代 A2A / Nacos / Agent Card 验证。
- 失败结果必须进入 `ExecutionReport`。

验证前 SHOULD 检查：

- Docker / WSL2 环境。
- Mininet 是否可启动。
- Ryu controller 是否监听正确端口。
- 拓扑脚本是否来自 ExecutionAdapter。
- 命令是否经过白名单或安全校验。

验证内容：

- Ryu 连接状态。
- Mininet 拓扑状态。
- ping / traceroute / iperf 结果。
- 流表状态。
- cleanup 是否执行。

失败结果 MUST 进入 `ExecutionReport`，不能只打印在控制台。

## 12. 常见问题

### 12.1 端口 8080 被占用

处理方式：

- 修改 `SERVER_PORT`。
- 或停止占用端口的本地进程。

### 12.2 spring-boot:run 长期运行

`spring-boot:run` 前台长期运行是正常服务状态，不等于卡死。Codex 验证时不应让它长期挂起。

### 12.3 Windows 文件锁导致 mvn clean 失败

处理方式：

- 关闭正在运行的 Java 进程。
- 关闭占用 target 的 IDE 或测试进程。
- 优先运行 `mvn compile` 或 `mvn test`。

### 12.4 API Key 未配置

现象：

- 真实模型调用失败。
- 报鉴权错误或配置缺失。

处理方式：

- 检查 `aliApi-key`。
- 确认未把 Key 写入仓库。

### 12.5 DashScope 调用超时

处理方式：

- 检查网络。
- 检查模型名称。
- 检查调用限流。
- 查看脱敏后的 Agent 日志。

### 12.6 npm 依赖安装失败

处理方式：

- 检查 Node.js / npm 版本。
- 清理本地 npm 缓存。
- 检查代理和 registry 配置。

### 12.7 Docker / WSL2 环境未启动

处理方式：

- 启动 Docker Desktop。
- 检查 WSL2 状态。
- 确认容器网络可用。

### 12.8 Mininet / Ryu 连接失败

处理方式：

- 检查 Ryu 监听端口。
- 检查 Mininet controller 配置。
- 检查防火墙和容器网络。
- 将失败写入 `ExecutionReport`。

### 12.9 Nacos 或 Agent Card 不可发现

现象：

- Orchestrator 无法发现专业 Agent。
- 远程 Agent 调用前即返回 `AGENT_CARD_NOT_FOUND` 或服务不可用错误。

处理方式：

- 检查 Nacos 是否启动。
- 检查专业 Agent 是否已注册到 Nacos。
- 检查 Agent Card 是否包含能力、输入输出契约、服务地址和版本。
- 检查 Agent Card 中的 A2A Endpoint 是否可访问。
- 检查 `mac-tav-web` / Orchestrator 侧 Nacos 地址和 A2aRemoteAgent / AgentCardProvider（官方 SAA A2A starter） 配置。
- 不允许回退成本地 Agent Bean 调用作为长期验证方式。

### 12.10 A2A 调用失败

现象：

- Orchestrator 通过 A2aRemoteAgent / AgentCardProvider（官方 SAA A2A starter） 调用专业 Agent 失败。
- 统一错误码应收敛为 `A2A_CALL_FAILED` 或更具体的远程 Agent 错误。

处理方式：

- 检查专业 Agent A2A Service 是否启动。
- 检查 A2aRemoteAgent / AgentCardProvider（官方 SAA A2A starter） 是否读取到正确 Agent Card。
- 检查协议地址、端口、超时和鉴权配置。
- 检查失败是否被 Orchestrator 记录到任务状态、执行日志或 Workspace 变更记录中。
- 不在 Controller 中直接绕过 Orchestrator 调用专业 Agent。
- 不允许回退成本地 Agent Bean 调用作为长期验证方式。

## 13. 长期验收清单

长期验收 SHOULD 包括：

- Nacos 已启动。
- 专业 Agent 可独立启动。
- 专业 Agent 已注册到 Nacos。
- Agent Card 可被 Orchestrator 查询。
- Orchestrator 可通过 A2A 调用专业 Agent。
- 真实 IntentAgent 可输出 `NetworkIntent`。
- 真实 PlanningAgent 可输出 `NetworkPlan`。
- 真实 ConfigurationAgent 可输出 `ConfigSet`。
- ExecutionAdapter 可输出 `ExecutionReport`。
- VerificationAgent 可输出 `ValidationReport`。
- HealingAgent 可输出 `RepairPlan`。
- A2A 调用失败可转换为统一错误。
- Orchestrator 仍负责写 Workspace、推进任务状态、管理 Artifact 版本和闭环控制。
- Workspace 可以保存全流程产物。
- 前端可以展示意图、拓扑、配置、执行、验证、修复。
- 测试不泄漏 API Key。
- 不存在 `mac-tav-web` 本地扫描具体 Agent Bean 的启动路线。
- 不存在 Controller 直接调用具体 Agent 的路线。
- 不存在 Mock Agent / Mock Tool 替代业务主链路的验收路线。

