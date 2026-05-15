# 开发计划

本文档定义 Demo 阶段的开发顺序和验收标准。当前项目优先跑通 Maven 多模块单体 Demo，不接入真实大模型、真实 RAG、真实 Mininet/Ryu。

## 1. Phase 0：文档与边界确认

### 做什么

1. 补齐并统一项目文档。
2. 明确 Demo 范围。
3. 明确标准 Maven 模块名。
4. 明确 DTO 和 API 设计边界。
5. 明确 Mock 数据主场景。

### 不做什么

1. 不创建 Java 类。
2. 不修改 `pom.xml`。
3. 不写业务代码。
4. 不引入真实基础设施依赖。

### 验收标准

1. `docs/00_PROJECT_BRIEF.md` 到 `docs/08_RUN_AND_TEST.md` 内容一致。
2. 模块名统一为 `docs/02_MAVEN_MODULES.md` 中的标准模块名。
3. Demo 边界清楚。
4. 后续开发可以按 Phase 顺序执行。

## 2. Phase 1：Maven 多模块骨架

### 做什么

1. 创建 Maven 父工程。
2. 创建标准子模块：
   - `mac-tav-common`
   - `mac-tav-model`
   - `mac-tav-agent-core`
   - `mac-tav-model-core`
   - `mac-tav-intent-agent`
   - `mac-tav-planning-agent`
   - `mac-tav-configuration-agent`
   - `mac-tav-execution`
   - `mac-tav-verification-agent`
   - `mac-tav-orchestrator`
   - `mac-tav-web`
3. 配置模块依赖方向。
4. 确保 `mac-tav-web` 是唯一 Spring Boot 启动模块。
5. 包名统一使用 `com.yali`。

### 不做什么

1. 不实现复杂业务逻辑。
2. 不创建 `mac-tav-healing-agent` 正式模块。
3. 不接入真实大模型、RAG、Mininet、Ryu。
4. 不接入 MySQL、Redis、Qdrant。
5. 不引入服务注册发现。
6. 不拆真实微服务。

### 验收标准

1. `mvn clean compile` 可以通过。
2. 模块依赖无循环。
3. `mac-tav-model-core` 不依赖任何 Agent 模块。
4. Agent 模块不依赖 `mac-tav-orchestrator` 和 `mac-tav-web`。

## 3. Phase 2：common + model DTO

### 做什么

1. 在 `mac-tav-common` 中实现公共枚举、异常、统一响应、基础工具。
2. 在 `mac-tav-common` 中区分：
   - `TaskStatus`
   - `StageStatus`
   - `ValidationStatus`
3. 在 `mac-tav-model` 中实现核心 DTO：
   - `NetworkTask`
   - `NetworkWorkspace`
   - `NetworkIntent`
   - `NetworkPlan`
   - `ConfigSet`
   - `ExecutionReport`
   - `ValidationReport`
   - `AgentStepLog`
4. 为可追溯规划元素增加 `id` 字段，例如 `AddressPlanItem`、`VlanPlanItem`、`RoutingPlan`、`NatPlan`。
5. 保持 `NetworkIntent` 不包含设备、接口、VLAN、IP。
6. 保持 `ConfigSet` 中的 `commandBlocks`、`traceRefs`、`rollbackCommands`。

### 不做什么

1. 不写 Agent 业务逻辑。
2. 不写 Controller。
3. 不写真实执行适配。
4. 不接数据库。

### 验收标准

1. `mvn clean compile` 可以通过。
2. 涉及 DTO 序列化测试时，`mvn clean test` 可以通过。
3. DTO 不依赖 Web、Orchestrator、Agent 实现模块。
4. DTO 字段与 `docs/04_DATA_MODELS.md` 基本一致。

## 4. Phase 3：Model Core 内存状态中心

### 做什么

1. 在 `mac-tav-model-core` 中实现 `NetworkWorkspaceService`。
2. 使用内存 Map 保存任务与阶段产物。
3. 支持创建任务。
4. 支持保存 Intent / Plan / Config / Execution / Validation。
5. 支持查询完整 `NetworkWorkspace`。
6. 支持追加 `AgentStepLog`。

### 不做什么

1. Model Core 不调用大模型。
2. Model Core 不生成网络方案。
3. Model Core 不生成配置命令。
4. Model Core 不执行仿真。
5. Model Core 不依赖任何 Agent 模块。
6. 不接 MySQL、Redis、Qdrant。

### 验收标准

1. `mvn clean compile` 可以通过。
2. Model Core 单元测试通过时运行 `mvn clean test`。
3. 任意阶段产物可以写入 Workspace。
4. 可以根据 `taskId` 查到完整结果。

## 5. Phase 4：Mock Agent 与 Mock Execution/Verification

### 做什么

1. 在 `mac-tav-agent-core` 中实现 Agent 通用抽象：
   - `BaseAgent`
   - `AgentContext`
   - `AgentResult`
2. 在 `mac-tav-intent-agent` 中实现 Mock Intent Agent。
3. 在 `mac-tav-planning-agent` 中实现 Mock Planning Agent。
4. 在 `mac-tav-configuration-agent` 中实现 Mock Configuration Agent。
5. 在 `mac-tav-execution` 中实现 DryRun / Mock Execution Adapter。
6. 在 `mac-tav-verification-agent` 中实现 Mock Verification Agent。
7. 保留失败场景 `guest_to_server_unexpected_pass`，方便后续验证失败分支。

### 不做什么

1. 不写 Controller。
2. 不依赖 Orchestrator 和 Web。
3. 不创建正式 Healing Maven 模块。
4. 不直接执行 Huawei CLI。
5. 不启动 Mininet/Ryu。
6. 不调用真实大模型或 RAG。

### 验收标准

1. `mvn clean compile` 可以通过。
2. 涉及 Mock 逻辑测试时运行 `mvn clean test`。
3. Intent Agent 只输出业务意图，不输出设备、接口、VLAN、IP。
4. Planning Agent 输出网络方案，不输出 CLI 命令。
5. Configuration Agent 输出 `ConfigSet`，且包含 `deviceConfigs` 和 `commandBlocks`。
6. Execution Module 通过 Execution Adapter 返回 DryRun / Mock 结果。
7. Verification Agent 根据 Mock 结果输出 `ValidationReport`。

## 6. Phase 5：Orchestrator + Web API

### 做什么

1. 在 `mac-tav-orchestrator` 中实现 `TaskOrchestratorService`。
2. 串联流程：
   ```text
   Intent -> Planning -> Configuration -> Execution -> Verification
   ```
3. 每个阶段完成后写入 Model Core。
4. 在 `mac-tav-web` 中实现 REST API：
   - `POST /api/demo/tasks`
   - `GET /api/tasks/{taskId}`
   - 阶段产物查询接口
5. 确保 `mac-tav-web` 是唯一 Spring Boot 启动模块。

### 不做什么

1. Controller 不写复杂业务逻辑。
2. 不接真实大模型、RAG、Mininet、Ryu。
3. 不引入服务注册发现。
4. 不强制接 MySQL、Redis、Qdrant。
5. 不实现完整 Healing 闭环。

### 验收标准

1. `mvn clean compile` 可以通过。
2. 涉及接口测试时运行 `mvn clean test`。
3. 提交一段自然语言后可以得到完整 `NetworkWorkspace`。
4. API 返回包含 Intent、Plan、Config、Execution、Validation。
5. Controller 只调用 Orchestrator 或查询服务。

## 7. Phase 6：展示层或接口展示增强

### 做什么

1. 增强接口返回结构，方便前端展示。
2. 可选实现简单前端页面。
3. 拓扑数据可被图组件直接消费。
4. 配置块可分设备、分段展示。
5. 验证报告可按通过和失败分类展示。

### 不做什么

1. 不改变核心 DTO 主结构。
2. 不把业务逻辑搬到前端。
3. 不引入生产级权限系统。

### 验收标准

1. 演示时能看清“意图到验证”的全过程。
2. 用户能看到每个 Agent 的执行轨迹。
3. 配置与意图、规划之间有追溯关系。

## 8. Phase 7：持久化与异步执行

### 做什么

1. 可选接入 MySQL 保存任务与阶段产物。
2. 可选接入 Redis 保存进度与短期状态。
3. 可选增加 SSE 推送任务进度。
4. 支持阶段重跑。
5. 支持多版本产物。

### 不做什么

1. 不在前置 Phase 中强制引入这些依赖。
2. 不改变“Web -> Orchestrator -> 模块 Service”的主调用结构。

### 验收标准

1. 应用重启后任务结果仍可查询。
2. 前端可以实时看到阶段进度。
3. 某个阶段失败时可以定位和重跑。

## 9. Phase 8：真实能力接入

### 做什么

按替换 Mock 实现的方式逐步接入：

1. 真实大模型，替换 Intent Agent。
2. 规划规则或模板库，增强 Planning Agent。
3. 真实 RAG，增强 Configuration Agent。
4. Docker 化 Mininet。
5. Ryu 控制器。
6. 真实 ping / traceroute / iperf 测试。
7. 后续创建 `mac-tav-healing-agent` 并扩展自愈闭环。

### 不做什么

1. 不推翻已有 DTO 和 API 主结构。
2. 不让 Execution Module 直接执行 Huawei CLI。
3. 不让真实能力绕过 Model Core 的产物管理。

### 验收标准

1. 每个真实能力都能独立替换对应 Mock 实现。
2. 不破坏已有 Demo 主流程。
3. 真实执行仍通过 Execution Adapter 适配目标环境。

## 10. 当前推荐下一步

完成文档后，建议优先实现：

```text
Phase 1：Maven 多模块骨架
```

原因：

1. 这是后续所有 DTO、Mock Agent 和 API 的基础。
2. 可以尽早验证模块依赖是否合理。
3. 能先把标准模块名、包名和构建命令固定下来。
