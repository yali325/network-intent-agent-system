# 运行与测试说明

本文档说明当前 Demo 项目的运行、测试和验收方式。当前仓库处于文档整理阶段时，不要求可以运行后端服务。

## 1. 当前文档阶段如何验证

当前任务只整理文档，不创建 Java 类、不修改 `pom.xml`、不写业务代码。

文档阶段检查项：

```text
docs/00_PROJECT_BRIEF.md      已保留
docs/01_DEMO_SCOPE.md         已补齐
docs/02_MAVEN_MODULES.md      已补齐
docs/03_MODULE_DESIGN.md      已保留
docs/04_DATA_MODELS.md        已补齐
docs/05_API_DESIGN.md         已补齐
docs/06_DEV_PLAN.md           已补齐
docs/07_MOCK_DATA.md          已补齐
docs/08_RUN_AND_TEST.md       已补齐
```

## 2. Phase 1 后端构建命令

完成 Maven 多模块骨架后，在项目根目录运行：

```bash
mvn clean compile
```

预期结果：

1. 所有模块编译通过。
2. 不出现循环依赖。
3. 不需要真实大模型、RAG、Mininet/Ryu 环境。

涉及测试时再运行：

```bash
mvn clean test
```

## 3. Phase 5 启动 Demo 服务

完成 Web API 后，在项目根目录运行：

```bash
mvn -pl mac-tav-web spring-boot:run
```

如果需要同时构建依赖模块，可运行：

```bash
mvn -pl mac-tav-web -am spring-boot:run
```

默认访问地址：

```text
http://localhost:8080
```

## 4. API 测试

### 4.1 运行完整 Demo 流程

```bash
curl -X POST http://localhost:8080/api/demo/tasks \
  -H "Content-Type: application/json" \
  -d "{\"rawText\":\"构建一个办公区和访客区隔离的网络，两个区域都能访问互联网，办公区可以访问服务器，访客区不能访问服务器，采用 OSPF。\"}"
```

预期返回：

1. `NetworkTask`
2. `NetworkIntent`
3. `NetworkPlan`
4. `ConfigSet`
5. `ExecutionReport`
6. `ValidationReport`
7. `AgentStepLog` 列表

### 4.2 查询任务

```bash
curl http://localhost:8080/api/tasks/task-10001
```

预期返回完整 `NetworkWorkspace`。

### 4.3 查询阶段产物

```bash
curl http://localhost:8080/api/tasks/task-10001/intent
curl http://localhost:8080/api/tasks/task-10001/plan
curl http://localhost:8080/api/tasks/task-10001/config
curl http://localhost:8080/api/tasks/task-10001/execution
curl http://localhost:8080/api/tasks/task-10001/validation
curl http://localhost:8080/api/tasks/task-10001/logs
```

## 5. 单元测试建议

### 5.1 DTO 测试

重点检查：

1. DTO 可以正常序列化为 JSON。
2. DTO 可以从 JSON 反序列化。
3. 字段命名符合文档约定。

### 5.2 Model Core 测试

重点检查：

1. 创建任务。
2. 保存每个阶段产物。
3. 查询完整 Workspace。
4. 追加 Agent 日志。
5. 查询不存在的任务时返回明确错误。

### 5.3 Mock Agent 测试

重点检查：

1. Intent Agent 不生成设备、接口、VLAN、IP。
2. Planning Agent 不生成 CLI 命令。
3. Configuration Agent 输出 `deviceConfigs` 和 `commandBlocks`。
4. Execution Module 不直接执行 Huawei CLI，必须通过 Execution Adapter 转换为 DryRun / Mininet / Ryu 可执行内容。
5. Verification Agent 根据 Mock 测试结果生成正确报告。

### 5.4 Orchestrator 测试

重点检查：

1. 阶段调用顺序正确。
2. 每个阶段产物都写入 Model Core。
3. 任一阶段失败时任务状态可标记为 `ERROR`。
4. 成功场景最终状态为 `PASSED`。

### 5.5 Web API 测试

重点检查：

1. `POST /api/demo/tasks` 返回完整 Workspace。
2. `GET /api/tasks/{taskId}` 能查询任务。
3. 阶段产物接口返回正确类型。
4. 错误任务 ID 返回 `TASK_NOT_FOUND`。

## 6. Demo 验收清单

运行完整 Demo 后，检查：

1. 页面或 API 能看到原始输入。
2. 意图图包含办公区、访客区、服务器区、公网。
3. 规划结果包含 R1、SW1、SW2、三台主机和公网。
4. 地址规划包含 `192.168.10.0/24`、`192.168.20.0/24`、`192.168.30.0/24`。
5. VLAN 规划包含 10、20、30。
6. 配置结果按 R1、SW1、SW2 分设备展示。
7. 配置结果包含 ACL、OSPF、VLAN、接口配置块。
8. 执行结果明确标记为 Mock / DryRun。
9. 验证报告显示访客区访问服务器被阻断。
10. 验证报告整体状态为 `PASSED`。

## 7. 当前阶段 TODO

1. 创建 Maven 多模块骨架。
2. 实现共享 DTO。
3. 实现 `mac-tav-agent-core` 通用 Agent 抽象。
4. 实现 Model Core 内存状态中心。
5. 实现 Mock Agent。
6. 实现 Orchestrator。
7. 实现 Web API。
8. 后续再考虑前端展示、持久化、真实大模型、RAG、Mininet/Ryu。
