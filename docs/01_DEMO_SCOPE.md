# Demo 范围说明

本文档定义当前 Demo 阶段的实现边界。

## 1. 当前目标

当前阶段先实现一个可运行、可展示、可验证流程的 Maven 多模块单体 Demo。

Demo 的核心价值不是一次性接入所有真实能力，而是让系统先跑通下面这条主线：

```text
用户输入自然语言网络需求
  ↓
Mock Intent Module 输出 NetworkIntent
  ↓
Mock Planning Module 输出 NetworkPlan
  ↓
Mock Configuration Module 输出 ConfigSet
  ↓
Mock Execution Module 输出 ExecutionReport
  ↓
Mock Verification Agent 输出 ValidationReport
  ↓
Web API 返回完整任务结果
```

## 2. Demo 必须展示的能力

1. 用户可以提交一段自然语言网络意图。
2. 系统返回结构化意图解析结果 `NetworkIntent`。
3. 系统返回网络方案 `NetworkPlan`，包含拓扑、区域、地址、VLAN、路由和安全策略。
4. 系统返回配置结果 `ConfigSet`，配置必须按设备和 `commandBlocks` 结构化组织。
5. 系统返回执行结果 `ExecutionReport`，当前使用 Mock / DryRun 数据。
6. 系统返回验证报告 `ValidationReport`，说明哪些意图满足、哪些失败。
7. 所有阶段产物都应能放入 `NetworkWorkspace`，方便前端展示和后续追踪。

## 3. 当前阶段明确不做

当前阶段暂不接入以下能力：

1. 真实大模型调用。
2. 真实 RAG 检索。
3. 真实 Mininet / Ryu 环境执行。
4. 真实设备 CLI 下发。
5. 完整 Healing 自愈闭环。
6. 用户登录、权限、租户、多用户隔离。
7. 分布式微服务部署。
8. 生产级数据库建模和迁移脚本。

## 4. 技术形态边界

当前阶段采用 Maven 多模块单体，不拆真实微服务。

后端启动入口只有一个：

```text
mac-tav-web
```

核心业务模块通过普通 Java Service 接口和实现类串联。模块之间用本地方法调用，不使用 RPC、消息队列或服务注册发现。

## 5. Mock 边界

当前 Demo 中，所有智能体和外部环境都可以使用 Mock 实现：

| 模块 | 当前实现方式 | 后续替换方向 |
| --- | --- | --- |
| Intent Module | 规则或固定样例解析 | 大模型意图解析 |
| Planning Module | 固定网络方案模板 | 规则规划 + 模型辅助 |
| Configuration Module | 固定 Huawei 风格命令模板 | 模板 + RAG + 设备知识库 |
| Execution Module | DryRun / Mock 执行结果 | Mininet / Ryu / eNSP 适配 |
| Verification Agent | 基于 Mock TestResult 的规则判断 | 静态规则 + 动态仿真验证 |
| Healing Agent | 当前阶段不创建正式 Maven 模块，只保留 TODO 或 Mock 说明 | 后续扩展为 `mac-tav-healing-agent` |

## 6. Demo 主场景

第一版 Demo 固定使用一个代表性场景：

```text
构建一个办公区和访客区隔离的网络，两个区域都能访问互联网，办公区可以访问服务器，访客区不能访问服务器，采用 OSPF。
```

该场景应覆盖：

1. 办公区、访客区、服务器区、公网四类业务对象。
2. 区域间访问控制。
3. VLAN 隔离。
4. IP 地址规划。
5. OSPF 路由规划。
6. ACL 安全策略。
7. NAT / 公网访问说明。
8. 验证用例，包括允许访问和禁止访问。

## 7. Demo 验收标准

当前阶段完成后，应满足：

1. Maven 多模块结构清晰，模块依赖不循环。
2. 共享 DTO 位于 `mac-tav-model`。
3. 公共枚举、异常、工具位于 `mac-tav-common`。
4. Agent 通用抽象位于 `mac-tav-agent-core`。
5. Controller 只位于 `mac-tav-web`。
6. Agent 模块不包含 Controller。
7. Model Core 只做状态与产物管理，不生成方案、不生成配置、不执行仿真。
8. Execution Module 不直接执行 Huawei CLI，只通过 Execution Adapter 生成 DryRun / 仿真可执行内容。
9. 通过一个 API 可以拿到完整 Demo 流程结果。
10. 返回结果能直接支撑前端展示意图、拓扑、配置、执行、验证过程。

## 8. 后续扩展原则

后续扩展真实能力时，应保持接口和 DTO 结构相对稳定。真实大模型、RAG、Mininet/Ryu 都应作为当前 Mock 实现的替换层，而不是推翻主流程。

Healing Agent 在当前阶段不创建正式 Maven 模块，只在文档和 DTO 中保留 TODO 或 Mock 返回，不展开复杂修复逻辑。
