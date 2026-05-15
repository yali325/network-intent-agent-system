# 运行、测试与集成验证说明

本文档定义 MAC-TAV 长期运行、测试、手动集成验证和常见问题处理方式。

本文档不写 DTO 全字段，不写 Agent 架构，不写 Maven 模块设计。对应内容见：

- API 设计：`docs/05_API_DESIGN.md`
- 测试场景：`docs/07_TEST_DATA_AND_SCENARIOS.md`
- Agent 构建规范：`docs/09_AGENT_BUILD_GUIDE.md`

## 1. 文档目标

本文档只负责：

1. 运行命令。
2. 测试命令。
3. 环境变量。
4. 手动验证。
5. 常见问题。

## 2. 基础环境要求

| 环境 | 说明 |
| --- | --- |
| JDK | 版本以 `pom.xml` 为准 |
| Maven | 用于后端多模块构建和测试 |
| Node.js / npm | 用于前端安装、开发和构建 |
| Docker | 用于后续 Mininet / Ryu / MySQL / Redis / Qdrant |
| 操作系统 | Windows + Docker Desktop / WSL2 可以使用，但要注意端口、路径和文件锁 |

运行前 SHOULD 确认：

- 后端端口未被占用。
- Docker Desktop / WSL2 可用。
- 本地环境变量按需配置。
- 不在日志或命令历史中暴露真实 API Key。

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
mvn -pl mac-tav-orchestrator -am test
mvn -pl mac-tav-web -am test
```

说明：

- 如果 Windows 文件锁导致 `mvn clean` 失败，不要反复 clean。
- 可以先运行 `mvn compile` 或 `mvn test`。
- 清理 `target` 前 SHOULD 关闭正在运行的 Java 进程和 IDE 占用。

## 4. 后端启动方式

可选启动方式：

1. 通过 IDEA 启动 `MacTavApplication`。
2. 使用 Maven 启动 Web 模块：

```bash
mvn -pl mac-tav-web -am spring-boot:run
```

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
| `AI_DASHSCOPE_API_KEY` | DashScope API Key |
| `ALI_API_KEY` | 兼容可选 Key |
| `DASHSCOPE_CHAT_MODEL` | DashScope ChatModel 名称 |
| `SERVER_PORT` | 后端端口 |
| `VITE_API_BASE_URL` | 前端访问后端的基础地址 |

要求：

- API Key 不写入 `application.yml`。
- API Key 不提交仓库。
- 日志不打印完整 API Key。
- 单元测试不依赖真实 API Key。

## 7. Agent 单元测试规则

Agent 单元测试 MUST：

- 使用 Stub ChatModel / Fake ReactAgent。
- 使用 Mock Tool。
- 测试 Prompt 文件是否存在。
- 测试 `ResponseSchema -> DTO` 转换。
- 测试 Validator。
- 测试 Tool 异常转换。
- 测试 Agent 输出越界字段被拒绝。

Agent 单元测试不得调用真实外部模型 API。

## 8. API 测试方式

长期 API 前缀为 `/api/v1`。

常用接口：

- `POST /api/v1/tasks`
- `GET /api/v1/tasks/{taskId}/workspace`
- `GET /api/v1/tasks/{taskId}/intent`
- `GET /api/v1/tasks/{taskId}/plan`
- `GET /api/v1/tasks/{taskId}/config`
- `GET /api/v1/tasks/{taskId}/execution`
- `GET /api/v1/tasks/{taskId}/validation`
- `GET /api/v1/tasks/{taskId}/repair`
- `GET /api/v1/tasks/{taskId}/events`

具体 API 以 `docs/05_API_DESIGN.md` 为准。

创建任务示例：

```bash
curl -X POST http://localhost:8080/api/v1/tasks ^
  -H "Content-Type: application/json" ^
  -d "{\"rawText\":\"构建办公区和访客区隔离网络，办公区可访问服务器，访客区不可访问服务器。\",\"runImmediately\":true}"
```

查询 Workspace 示例：

```bash
curl http://localhost:8080/api/v1/tasks/{taskId}/workspace
```

## 9. SSE 验证方式

使用 curl：

```bash
curl -N http://localhost:8080/api/v1/tasks/{taskId}/events
```

或使用浏览器 `EventSource`。

SSE 约束：

- 只推送进度事件和摘要。
- 不推送 API Key、完整模型请求、完整异常堆栈。
- 前端断线后通过 workspace / timeline 接口恢复状态。

## 10. 手动真实模型验证

手动验证真实 DashScope 前 MUST 设置：

- `AI_DASHSCOPE_API_KEY`

手动验证 SHOULD 记录：

- 输入。
- 输出摘要。
- 错误码。
- 模型调用耗时。
- Validator 结果。

检查重点：

- IntentAgent 输出的 `NetworkIntent` 不包含设备、接口、VLAN、IP、CLI。
- PlanningAgent 输出的 `NetworkPlan` 不包含 CLI。
- ConfigurationAgent 输出的 `ConfigSet` 包含 commandBlocks、traceRefs、rollbackCommands。
- VerificationAgent 输出的 `ValidationReport` 不直接修改配置。
- HealingAgent 输出的 `RepairPlan` 不直接执行修复。

## 11. Mininet / Ryu 手动验证

ExecutionAdapter 接入后再启用 Mininet / Ryu 验证。

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

- 检查 `AI_DASHSCOPE_API_KEY`。
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

## 13. 长期验收清单

长期验收 SHOULD 包括：

- 真实 IntentAgent 可以生成 `NetworkIntent`。
- PlanningAgent 可以生成 `NetworkPlan`。
- ConfigurationAgent 可以生成结构化 `ConfigSet`。
- ExecutionAdapter 可以输出 `ExecutionReport`。
- VerificationAgent 可以输出 `ValidationReport`。
- HealingAgent 可以输出 `RepairPlan`。
- Workspace 可以保存全流程产物。
- 前端可以展示意图、拓扑、配置、执行、验证、修复。
- 测试不泄漏 API Key。
- 执行命令受 Adapter 控制。

## 14. 本文档与其他文档的分工

- 本文档：运行、测试、手动验证、常见问题。
- `docs/05_API_DESIGN.md`：API 契约。
- `docs/07_TEST_DATA_AND_SCENARIOS.md`：测试场景与样例数据。
- `docs/09_AGENT_BUILD_GUIDE.md`：Agent 构建规范。
