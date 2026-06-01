id: huawei-troubleshooting  
title: Huawei Troubleshooting Commands  
vendor: HUAWEI  
platform: VRP  
feature: TROUBLESHOOTING  
tags: [ping, tracert, display, arp, mac-address, troubleshooting]  
knowledgeType: TROUBLESHOOTING_PLAYBOOK  
generationSourceType: RAG  
riskLevel: LOW  
status: DISABLED  
version: 0.1.0  
updatedAt: 2026-06-02

---

# 故障排查命令

## 这个模块解决什么问题

本模块用于整理华为 VRP 设备中的常见故障排查命令和排查思路，包括连通性测试、路由排查、ARP 排查、MAC 地址排查、接口排查、VLAN 排查、STP 排查、日志查看和诊断信息收集。

本模块主要服务后续 VerificationAgent 和 HealingAgent，用于解释验证失败原因、生成排障建议或辅助定位故障点。

本模块不应作为 ConfigurationAgent 的主配置生成知识库。这里的命令大多是查询、诊断和排障命令，不应混入正式网络配置 `CommandBlock.commands`。

## 连通性测试

```text
ping <destination-ip>
ping -a <source-ip> <destination-ip>
ping -c <count> <destination-ip>
ping -s <size> <destination-ip>
tracert <destination-ip>
tracert -a <source-ip> <destination-ip>
```

说明：

- `ping` 用于测试基本连通性。
    
- `ping -a` 可以指定源 IP，适合多接口、多 VLAN、多路由路径排查。
    
- `ping -s` 可用于辅助判断 MTU 或大包问题。
    
- `tracert` 用于查看路径经过哪些三层节点。
    
- `tracert` 中间节点超时不一定代表故障，可能是中间设备禁止回应。
    

## 配置查看

```text
display this
display current-configuration
display current-configuration interface <interface-name>
display saved-configuration
```

说明：

- `display this` 查看当前视图配置。
    
- `display current-configuration interface` 适合检查接口、ACL、VLAN、IP 等配置是否存在。
    
- 查看命令只用于排查，不应作为正式配置命令。
    

## 路由排查

```text
display ip routing-table
display ip routing-table <destination>
display ip routing-table verbose
display fib <destination>
```

排查关注点：

- 是否存在到目标网段的路由。
    
- 下一跳是否正确。
    
- 出接口是否正确。
    
- 是否存在默认路由。
    
- 是否缺少回程路由。
    
- 路由存在不代表访问一定允许，还需要检查 ACL、NAT 和目标主机网关。
    

## ARP 排查

```text
display arp
display arp dynamic
display arp static
display arp interface <interface-name>
```

排查关注点：

- 是否学习到目标网关或目标主机的 ARP。
    
- ARP 是否对应正确接口。
    
- 是否存在异常静态 ARP。
    
- ARP 学不到时，应同时检查 VLAN、接口状态、网关 IP、链路和对端设备。
    

## MAC 地址排查

```text
display mac-address
display mac-address dynamic
display mac-address static
display mac-address blackhole
display mac-address aging-time
```

排查关注点：

- 是否学习到终端 MAC。
    
- MAC 是否出现在正确接口。
    
- MAC 是否在错误 VLAN 或错误端口漂移。
    
- 是否存在黑洞 MAC 或静态 MAC 影响转发。
    

## 接口排查

```text
display interface brief
display interface <interface-name>
display counters interface <interface-name>
display error interface <interface-name>
display interface description
```

排查关注点：

- 物理状态是否 up。
    
- 协议状态是否 up。
    
- 接口是否被 shutdown。
    
- 是否存在错包、丢包、CRC、碰撞等异常。
    
- 接口描述是否符合规划。
    
- 接口配置是否和 NetworkPlan 一致。
    

## VLAN 排查

```text
display vlan
display vlan summary
display port vlan
display current-configuration interface <interface-name>
```

排查关注点：

- VLAN 是否存在。
    
- Access 端口是否加入正确 VLAN。
    
- Trunk 端口是否允许目标 VLAN 通过。
    
- Vlanif 是否存在并配置正确 IP。
    
- 跨交换机两端 trunk VLAN 是否一致。
    

## STP 排查

```text
display stp
display stp brief
display stp abnormal-interface
display stp region-configuration
```

排查关注点：

- 接口是否被 STP 阻塞。
    
- 是否存在环路风险。
    
- STP 区域配置是否一致。
    
- STP 收敛期间接口状态变化可能导致短暂不通。
    

## 日志和告警查看

```text
display logbuffer
display logbuffer | last 50
display logbuffer | include <keyword>
display trapbuffer
display alarm active
display alarm history
display debugging
```

排查关注点：

- 故障时间点附近是否有接口 down、协议 down、认证失败、ACL 命中、硬件告警等信息。
    
- 日志过多时应使用 `last` 或 `include` 过滤。
    
- 不应默认执行 `reset logbuffer`，否则会清空排障证据。
    

## 诊断信息收集

```text
display diagnostic-information
display diagnostic-information | save diag.txt
display version
display device
display alarm active
display logbuffer
```

说明：

- `display diagnostic-information` 输出信息量大，适合报障或深度排查。
    
- 保存到文件有助于后续分析。
    
- 该命令通常只读，但输出可能包含设备信息，应注意脱敏。
    

## 常见排查场景

### 场景 1：ping 不通

```text
display interface brief
display ip routing-table <destination-ip>
display arp
display acl all
display traffic-filter applied-record
display logbuffer | last 50
```

排查顺序：

1. 检查接口是否 up。
    
2. 检查源和目标是否有路由。
    
3. 检查是否缺少回程路由。
    
4. 检查 ARP 是否正常。
    
5. 检查 ACL 是否阻断。
    
6. 检查 NAT 或安全策略是否影响流量。
    
7. 检查目标主机网关和主机防火墙。
    

### 场景 2：接口 up 但业务不通

```text
display interface <interface-name>
display counters interface <interface-name>
display error interface <interface-name>
display current-configuration interface <interface-name>
display vlan
display port vlan
display stp brief
```

排查顺序：

1. 检查接口错误包。
    
2. 检查接口配置。
    
3. 检查 VLAN 归属。
    
4. 检查 trunk allow-pass VLAN。
    
5. 检查 STP 是否阻塞。
    
6. 检查对端设备状态。
    

### 场景 3：路由存在但访问失败

```text
display ip routing-table <destination-ip>
display arp
display acl all
display traffic-filter applied-record
tracert <destination-ip>
ping -a <source-ip> <destination-ip>
```

排查顺序：

1. 确认路由下一跳和出接口。
    
2. 检查回程路由。
    
3. 检查 ACL 方向和绑定接口。
    
4. 检查 NAT 是否需要配置。
    
5. 使用指定源地址测试路径。
    

### 场景 4：跨 VLAN 不通

```text
display vlan
display port vlan
display interface Vlanif<vlan-id>
display ip interface brief
display ip routing-table
display acl all
```

排查顺序：

1. 检查 VLAN 是否存在。
    
2. 检查 access 端口是否加入正确 VLAN。
    
3. 检查 trunk 是否允许目标 VLAN。
    
4. 检查 Vlanif 是否配置 IP 并处于正常状态。
    
5. 检查路由和 ACL。
    

## 高风险排障命令

以下命令会改变设备状态或清空排障证据，不应默认生成：

```text
reset arp dynamic
reset counters interface <interface-name>
reset logbuffer
reset recycle-bin
traffic-mirror
```

说明：

- `reset arp dynamic` 会清空动态 ARP 表，可能造成短暂重新学习。
    
- `reset counters interface` 会清空接口计数器，影响后续判断错误包是否新增。
    
- `reset logbuffer` 会清空日志缓冲区，导致历史日志丢失。
    
- `reset recycle-bin` 属于文件维护操作，不属于普通排障。
    
- `traffic-mirror` 是配置类动作，不是只读排障命令，需要单独设计和人工确认。
    

## ConfigurationAgent 使用规则

ConfigurationAgent 使用本模块时应遵守：

1. 本模块默认不进入 Phase 5 配置生成 RAG。
    
2. 如果未来启用，应只作为排障建议或验证辅助知识使用。
    
3. 不要把 `display ...`、`ping`、`tracert` 混入正式配置 `CommandBlock.commands`。
    
4. 不要默认生成 `reset ...`、`traffic-mirror` 等会改变设备状态的命令。
    
5. 本模块更适合 VerificationAgent 和 HealingAgent 使用。
    
6. 故障排查建议不能替代 ExecutionReport 或 ValidationReport 的验证结果。
    

## 常见错误

1. ping 不通时只看路由表，忽略接口、ARP、ACL、NAT 和回程路由。
    
2. tracert 中间节点超时就判断链路故障，实际可能是中间设备禁止回应。
    
3. 路由存在就认为访问一定可达，忽略 ACL 或安全策略阻断。
    
4. 跨 VLAN 不通时只检查 Vlanif，忘记检查 access / trunk / VLAN 是否正确。
    
5. 接口 up 但业务不通时，忽略错误包、光模块、速率双工和对端设备。
    
6. 执行 `reset counters interface` 前没有记录当前计数器，导致排障证据丢失。
    
7. 执行 `reset logbuffer` 清空日志，导致故障时间点证据丢失。
    
8. 把 `traffic-mirror` 当作普通排障命令，实际它会修改设备配置。
    
9. 把排障命令混入正式配置块，导致配置产物不符合项目结构化配置要求。