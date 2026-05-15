# AGENTS.md

## 项目说明

本项目是基于多智能体协同的网络意图翻译与闭环验证系统。

当前阶段目标是实现一个可运行的 Maven 多模块单体 Demo，不要求一开始接入真实大模型、真实 RAG、真实 Mininet/Ryu。

## 每次开发前必须阅读

请在任何开发任务开始前阅读：

1. `docs/00_PROJECT_BRIEF.md`
2. `docs/01_DEMO_SCOPE.md`
3. `docs/02_MAVEN_MODULES.md`
4. `docs/03_MODULE_DESIGN.md`
5. `docs/04_DATA_MODELS.md`
6. `docs/06_DEV_PLAN.md`

如果任务涉及接口，请阅读：

- `docs/05_API_DESIGN.md`

如果任务涉及 Mock 数据，请阅读：

- `docs/07_MOCK_DATA.md`

## 标准 Maven 模块名

当前阶段标准 Maven 模块如下：

1. `mac-tav-common`
2. `mac-tav-model`
3. `mac-tav-agent-core`
4. `mac-tav-model-core`
5. `mac-tav-intent-agent`
6. `mac-tav-planning-agent`
7. `mac-tav-configuration-agent`
8. `mac-tav-execution`
9. `mac-tav-verification-agent`
10. `mac-tav-orchestrator`
11. `mac-tav-web`

Healing 模块当前阶段不创建正式 Maven 模块。后续扩展时再考虑创建：

```text
mac-tav-healing-agent
```

## 开发规则

1. 当前先做 Maven 多模块单体 Demo。
2. 当前阶段不要创建真实微服务，不引入服务注册发现。
3. 当前阶段不要强制接入 MySQL、Redis、Qdrant、Mininet、Ryu。
4. 不要一开始接入真实大模型、真实 RAG、真实 Mininet/Ryu。
5. 只有 `mac-tav-web` 是 Spring Boot 启动模块。
6. 所有共享 DTO 放在 `mac-tav-model`。
7. 所有公共工具、枚举、异常放在 `mac-tav-common`。
8. Agent 通用抽象放在 `mac-tav-agent-core`，例如 `BaseAgent`、`AgentContext`、`AgentResult`。
9. Model Core 不调用大模型，不生成网络方案，不生成配置命令，不执行仿真，只负责状态和产物管理。
10. Model Core 不依赖任何 Agent 模块。
11. Agent 模块不依赖 `mac-tav-orchestrator` 和 `mac-tav-web`。
12. Agent 模块不写 Controller。
13. Controller 只放在 `mac-tav-web`。
14. Execute Module 不能直接执行 Huawei CLI，必须通过 Execution Adapter 转换为 Mininet/Ryu/DryRun 可执行内容。
15. 不要让 Maven 模块出现循环依赖。
16. 包名统一使用 `com.yali`，不要使用 `com.example`。
17. 完成开发任务后至少运行 `mvn clean compile`。
18. 涉及测试时运行 `mvn clean test`。

## 完成任务后必须说明

每次完成后请说明：

1. 修改了哪些文件
2. 新增了哪些类或文档
3. 如何运行
4. 如何测试
5. 当前还有哪些 TODO
