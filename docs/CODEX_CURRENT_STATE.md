# CODEX_CURRENT_STATE

## 1. 当前项目阶段

当前项目已完成 Phase 1、Phase 2、Phase 3 的主体落地，经 Phase 3.5 重构后准备进入 Phase 4。本文档只记录当前代码状态和接手边界，不设计 Phase 4 的具体实现。

Phase 3.5 已完成以下清理：
- 删除 SchemaAgentInvoker 及其在 AgentUtils 中的重载，消除双链路架构
- Orchestrator 侧 legacy fallback（HttpA2aClient、NacosAgentCardRegistryClient）改为 @ConditionalOnProperty 默认关闭
- 移除 Orchestrator 对 mac-tav-execution 的 Maven 依赖
- IntentAgentConfiguration 中移除 @ConditionalOnBean(ChatModel.class)，改为直接参数注入

## 2. 当前主线架构状态

- mac-tav-web 只作为 Web/API 入口，通过 Orchestrator 触发流程，不扫描具体 Agent 模块。
- mac-tav-orchestrator 是唯一主编排入口，负责创建任务、调用远程 Agent、解析返回结果并把阶段产物写入 Model Core。
- mac-tav-model-core 提供内存版 Workspace / Artifact / Event / AgentExecutionRecord / WorkspaceChangeRecord 状态中心，后续 Phase 9 可替换为持久化实现。
- mac-tav-intent-agent 已作为独立 Spring Boot Agent 服务存在，通过 IntentAgentConfiguration 注册命名 ReactAgent Bean，走 Spring AI Alibaba 官方 A2A / Nacos Registry 链路。
- A2A 主线已完全收敛到官方 SAA A2A 方向：依赖 spring-ai-alibaba-starter-a2a-nacos，通过 pplication.yml 和 ReactAgent Bean 自动装配。不存在手写 A2A HTTP Controller 或自定义 executor。
- ReactAgent Bean 必须通过 XxxAgentConfiguration 注册，XxxAgent 注入使用，不通过 @ConditionalOnBean(ChatModel.class) 做条件装配。

## 3. 最近关键改动

- Phase 3.5 重构：删除 SchemaAgentInvoker，关闭 legacy fallback，移除 @ConditionalOnBean(ChatModel.class)。
- AgentUtils 只保留 callSchema(ReactAgent, ...) 单一入口，不再有双路径。
- IntentAgent 实现：薄封装、prompt、schema、parser、validator、tool、service。
- IntentAgent 独立服务启动类 + 命名 ReactAgent Bean + SAA A2A / Nacos 注册配置。
- Orchestrator 官方 A2A discovery/client 适配路径，legacy fallback 默认关闭。
- Web 最小任务创建、运行 Intent stage、查询 Workspace API 入口。
- Nacos 可用（127.0.0.1:8848），用于 Agent 服务注册。

## 4. 当前可用能力

| 模块 | 当前可用能力 |
| --- | --- |
| mac-tav-common | 统一错误码、BusinessException、ApiResponse 等公共基础能力。 |
| mac-tav-model | 核心枚举、阶段产物 DTO、Workspace DTO、TraceRefs、Agent Card / A2A 契约、IntentAgent 共享调用 payload。 |
| mac-tav-agent-core | PromptLoader、Parser / Validator 接口、ValidationResult、AgentUtils（单一 callSchema(ReactAgent, ...) 入口）、项目内部 hook。 |
| mac-tav-model-core | 内存 Workspace、Artifact 版本、事件、执行记录、变更记录服务。 |
| mac-tav-intent-agent | IntentAgent 真实 Spring AI Alibaba Agent 链路：IntentAgentConfiguration 注册 ReactAgent Bean → IntentAgent 注入 → ResponseSchema → Parser → DTO → Validator；A2A / Nacos 注册方向已接入。 |
| mac-tav-orchestrator | 远程 Agent 调用适配、Intent stage 最小工作流、Workspace 写入闭环。Legacy fallback（HttpA2aClient、NacosAgentCardRegistryClient）默认关闭。 |
| mac-tav-web | 最小 Web API：创建任务、运行当前 Intent stage、查询 Workspace。 |
| 其他 Agent 模块 | Maven 模块已存在，但仍是最小骨架或占位，不应误认为已实现业务能力。 |

## 5. Spring AI Alibaba 官方 Agent 构建规范（关键）

Codex 在 Phase 4 及后续实现 Agent 时 MUST 遵守：

1. ReactAgent Bean 通过 XxxAgentConfiguration（@Configuration 类）的 @Bean 方法注册，不在 XxxAgent 构造器内部 uild()。
2. ReactAgent Bean 方法直接通过参数注入 ChatModel，**禁止使用 @ConditionalOnBean(ChatModel.class)**：后者在 Spring 解析阶段判断条件，可能因时序问题导致 ReactAgent Bean 被跳过。
3. XxxAgent 注入 ReactAgent Bean，只做项目业务封装，执行 ResponseSchema → Parser → DTO → Validator。
4. 项目不存在"离线链路"和"真实链路"双路径。所有 Agent 只有一条真实 Spring AI Alibaba 链路。
5. 自动化测试使用固定样例 JSON 验证 Parser / Validator / Service，不调用真实模型 API。
6. Agent 模块编码时，需要的外部组件（Nacos 等）由用户开启或询问用户开启，API 资源充足，**不引入离线测试替身替代真实组件**。

## 6. 当前架构债和待确认事项

- 官方 Spring AI Alibaba A2A / Nacos 端到端联调仍需手动验证：需要 Nacos、DashScope Key 和 Agent/Web 两个服务启动。
- IntentAgent 按官方 A2A 暴露命名 ReactAgent，真实联调时需确认 Orchestrator 收到的 output 形态。
- Legacy fallback 类（HttpA2aClient、NacosAgentCardRegistryClient）仍存在但默认关闭，不应继续扩展或作为新 Agent 模板复制。
- OfficialAgentCardRegistryClient#listAvailableAgents 偏 IntentAgent 场景，后续多 Agent 时需按官方 discovery 能力扩展。
- docs/08_RUN_AND_TEST.md 环境变量已统一为 liApi-key。
- Model Core 仍是内存实现，不具备跨进程持久化能力。
- Planning / Configuration / Verification / Healing 等真实 Agent 尚未实现。

## 7. 下一阶段建议

Phase 4 进入 PlanningAgent 真实链路：PlanningResponseSchema → Parser → DTO(NetworkPlan) → Validator、prompt 和自动化测试。**不创建"离线链路"或测试替身替代真实 Agent 路径。**

Phase 4 MUST NOT：
- 创建 ConfigurationAgent、VerificationAgent、HealingAgent
- 接 Mininet / Ryu
- 引入双链路（真实 + 离线/测试替身）
- 破坏既有 IntentAgent / A2A 主线
- 让 Orchestrator 或 Web 直接依赖具体 Agent 实现类
- 让 PlanningAgent 写 Workspace 或推进任务状态

## 8. 下一轮 Codex 推荐读取范围

默认读取：

1. AGENTS.md
2. docs/CODEX_CURRENT_STATE.md
3. docs/CODEX_DOC_INDEX.md
4. docs/phase-handoffs/PHASE_03_HANDOFF.md

如果用户要求进入 Phase 4，再读取：

1. docs/06_DEV_PLAN.md 中 Phase 4 相关内容
2. docs/09_AGENT_BUILD_GUIDE.md 中 Agent 模块结构、Prompt、Parser、Validator、A2A 规范
3. docs/04_DATA_MODELS.md 中 NetworkIntent、NetworkPlan、TraceRefs 相关内容
4. docs/07_TEST_DATA_AND_SCENARIOS.md 中 PlanningAgent 相关输入输出场景
5. 直接涉及的模块源码：mac-tav-planning-agent、mac-tav-model、mac-tav-agent-core，以及必要的 Orchestrator / Model Core 边界类
