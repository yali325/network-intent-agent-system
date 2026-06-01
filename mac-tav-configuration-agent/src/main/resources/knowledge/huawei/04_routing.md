id: huawei-routing  
title: Huawei Routing And OSPF Commands  
vendor: HUAWEI  
platform: VRP  
feature: ROUTING  
tags: [routing, static-route, default-route, ospf, router-id, network]  
knowledgeType: COMMAND_REFERENCE  
generationSourceType: RAG  
riskLevel: MEDIUM  
status: READY  
version: 1.0.0  
updatedAt: 2026-06-02

---
# 路由配置

## 这个模块解决什么问题

本模块用于说明华为 VRP 设备中的基础 IPv4 路由配置方法，包括直连路由理解、静态路由、默认路由、浮动静态路由和 OSPF 基础配置。

路由配置用于实现不同网段、不同 VLAN、不同业务区域之间的三层转发。常见场景包括办公区访问服务器区、办公区访问互联网、访客区访问互联网，以及多台三层设备之间通过 OSPF 学习路由。

ConfigurationAgent 在生成路由配置时，应根据 `NetworkPlan.routingPlans`、`NetworkPlan.defaultRoute` 和地址规划生成结构化配置块。路由配置只解决“如何到达目标网段”，不负责判断业务是否允许访问。业务允许或禁止应由 ACL 或安全策略配置控制。

## 路由基础概念

|概念|说明|
|---|---|
|直连路由|接口配置 IP 并处于 up 状态后自动产生的路由|
|静态路由|手工指定目的网段、掩码和下一跳的路由|
|默认路由|目的地址为 `0.0.0.0/0` 的兜底路由|
|浮动静态路由|通过更高 preference 作为备份路径的静态路由|
|OSPF|链路状态动态路由协议，适合多设备自动学习路由|
|preference|路由优先级，数值越小优先级越高|
|wildcard-mask|OSPF network 命令使用的通配符掩码，不是子网掩码|

## 常用命令

### 查看路由

```text
display ip routing-table
display ip routing-table <destination>
display ip routing-table protocol static
display ip routing-table protocol ospf
display ip routing-table verbose
```

说明：

- `display ip routing-table`：查看 IPv4 路由表。
    
- `display ip routing-table protocol static`：只查看静态路由。
    
- `display ip routing-table protocol ospf`：只查看 OSPF 路由。
    
- `display ip routing-table <destination>`：查看到指定目的地址或网段的路由。
    

### 静态路由

```text
ip route-static <destination> <mask> <next-hop>
ip route-static <destination> <mask> <interface> <next-hop>
undo ip route-static <destination> <mask> <next-hop>
```

示例：

```text
ip route-static 10.0.30.0 255.255.255.0 10.0.12.2
```

说明：

- `destination` 是目标网络地址。
    
- `mask` 是目标网络掩码。
    
- `next-hop` 是下一跳 IP 地址。
    
- 如果下一跳不可达，静态路由可能无法正常生效。
    
- 静态路由适合简单、稳定、小规模网络。
    

### 默认路由

```text
ip route-static 0.0.0.0 0.0.0.0 <next-hop>
```

示例：

```text
ip route-static 0.0.0.0 0.0.0.0 203.0.113.1
```

说明：

- 默认路由用于匹配没有更精确路由的流量。
    
- 常用于内网设备访问互联网或上级网络。
    
- 默认路由通常和 NAT、出口接口、安全策略配合使用。
    
- 默认路由不等于互联网一定可达，还需要出口链路、NAT 和安全策略正确。
    

### 浮动静态路由

```text
ip route-static <destination> <mask> <next-hop-primary> preference <primary-preference>
ip route-static <destination> <mask> <next-hop-backup> preference <backup-preference>
```

示例：

```text
ip route-static 10.0.30.0 255.255.255.0 10.0.12.2 preference 60
ip route-static 10.0.30.0 255.255.255.0 10.0.13.2 preference 80
```

说明：

- preference 数值越小，路由优先级越高。
    
- 主链路 preference 应小于备份链路。
    
- 浮动静态路由适合主备出口或主备链路场景。
    
- 不要把 preference 和 OSPF cost 混淆。
    

### OSPF 基础配置

```text
ospf <process-id> router-id <router-id>
area <area-id>
network <network-address> <wildcard-mask>
```

示例：

```text
ospf 1 router-id 1.1.1.1
area 0.0.0.0
network 10.0.10.0 0.0.0.255
network 10.0.20.0 0.0.0.255
```

说明：

- `ospf 1` 创建或进入 OSPF 进程 1。
    
- `router-id` 用于唯一标识 OSPF 路由器。
    
- `area 0.0.0.0` 表示骨干区域。
    
- `network` 后面使用的是通配符掩码，不是子网掩码。
    
- OSPF 邻居建立通常要求链路连通、区域一致、认证一致、网络类型和定时器匹配。
    

### OSPF 查看命令

```text
display ospf peer
display ospf peer brief
display ospf routing
display ospf interface
display ospf lsdb
```

说明：

- `display ospf peer`：查看 OSPF 邻居。
    
- `display ospf routing`：查看 OSPF 路由。
    
- `display ospf interface`：查看运行 OSPF 的接口。
    
- `display ospf lsdb`：查看链路状态数据库。
    

## 不建议默认生成的协议

### RIP

RIP 适合教学和非常简单的小型网络，但收敛慢、能力有限。除非用户或 `NetworkPlan.routingPlans` 明确指定 `RIP`，ConfigurationAgent 不应默认生成 RIP 配置。

### BGP

BGP 常用于自治系统之间或大型网络边界路由，配置复杂，风险较高。当前项目基础场景主要是园区网、VLAN、OSPF、ACL、NAT，不建议在 Phase 5 默认生成 BGP 配置。

## 参数解释

|参数|说明|
|---|---|
|destination|目标网络地址，例如 `10.0.30.0`|
|mask|子网掩码，例如 `255.255.255.0`|
|next-hop|下一跳 IP 地址，例如 `10.0.12.2`|
|interface|出接口名称，例如 `GigabitEthernet0/0/1`|
|process-id|OSPF 进程 ID，例如 `1`|
|router-id|OSPF Router ID，例如 `1.1.1.1`|
|area-id|OSPF 区域 ID，例如 `0.0.0.0`|
|network-address|OSPF 宣告的网络地址，例如 `10.0.10.0`|
|wildcard-mask|通配符掩码，例如 `/24` 对应 `0.0.0.255`|
|preference|路由优先级，数值越小优先级越高|

## 路由优先级参考

|路由来源|常见 preference|
|---|--:|
|Direct 直连路由|0|
|OSPF|10|
|Static 静态路由|60|
|RIP|100|
|OSPF ASE|150|
|IBGP / EBGP|不同设备和版本可能存在差异，使用前应查对应设备文档|

说明：

- preference 用于不同路由来源之间的优先级比较。
    
- OSPF 内部路径选择还会使用 cost。
    
- 不要把 preference 和 cost 混为一谈。
    

## 配置示例

### 示例 1：静态路由到服务器区

场景：

- 本设备需要到达服务器区 `10.0.30.0/24`。
    
- 下一跳为 `10.0.12.2`。
    

```text
<Huawei> system-view
[Huawei] ip route-static 10.0.30.0 255.255.255.0 10.0.12.2
[Huawei] return
<Huawei> display ip routing-table 10.0.30.0
```

适用场景：

- 简单拓扑。
    
- 网段数量少。
    
- 下一跳稳定明确。
    

### 示例 2：默认路由到出口网关

场景：

- 本设备需要把未知目的流量发往出口网关。
    
- 出口下一跳为 `203.0.113.1`。
    

```text
<Huawei> system-view
[Huawei] ip route-static 0.0.0.0 0.0.0.0 203.0.113.1
[Huawei] return
<Huawei> display ip routing-table 0.0.0.0
```

适用场景：

- 内网访问互联网。
    
- 汇聚设备把未知流量交给上级出口。
    
- 与 NAT / ACL / 出口安全策略配合。
    

### 示例 3：主备静态路由

场景：

- 主下一跳：`10.0.12.2`
    
- 备下一跳：`10.0.13.2`
    
- 目标网段：`10.0.30.0/24`
    

```text
<Huawei> system-view
[Huawei] ip route-static 10.0.30.0 255.255.255.0 10.0.12.2 preference 60
[Huawei] ip route-static 10.0.30.0 255.255.255.0 10.0.13.2 preference 80
[Huawei] return
<Huawei> display ip routing-table 10.0.30.0
```

说明：

- preference 60 的路由优先于 preference 80 的路由。
    
- 主路由失效后，备路由才可能进入路由表。
    

### 示例 4：OSPF 单区域基础配置

场景：

- OSPF 进程：`1`
    
- Router ID：`1.1.1.1`
    
- 区域：`0.0.0.0`
    
- 宣告办公区网段：`10.0.10.0/24`
    
- 宣告访客区网段：`10.0.20.0/24`
    
- 宣告服务器区网段：`10.0.30.0/24`
    

```text
<Huawei> system-view
[Huawei] ospf 1 router-id 1.1.1.1
[Huawei-ospf-1] area 0.0.0.0
[Huawei-ospf-1-area-0.0.0.0] network 10.0.10.0 0.0.0.255
[Huawei-ospf-1-area-0.0.0.0] network 10.0.20.0 0.0.0.255
[Huawei-ospf-1-area-0.0.0.0] network 10.0.30.0 0.0.0.255
[Huawei-ospf-1-area-0.0.0.0] quit
[Huawei-ospf-1] quit
[Huawei] return
<Huawei> display ospf peer
<Huawei> display ospf routing
```

适用场景：

- 多台三层设备之间自动学习路由。
    
- 办公区、访客区、服务器区等多个网段需要被动态路由协议学习。
    
- 项目主场景中用户明确要求“采用 OSPF”。
    

注意：

- OSPF 的 `network` 命令不会直接创建 IP 地址，它只宣告已存在的接口网段。
    
- 如果接口没有对应 IP 或接口未 up，OSPF 无法正常工作。
    
- OSPF 只负责路由可达，不负责访问控制。
    

## 验证命令

```text
display ip routing-table
display ip routing-table 10.0.30.0
display ip routing-table protocol static
display ip routing-table protocol ospf
display ospf peer
display ospf peer brief
display ospf routing
display ospf interface
ping 10.0.30.10
tracert 10.0.30.10
```

验证关注点：

- 是否存在到目标网段的路由。
    
- 路由下一跳是否正确。
    
- 默认路由是否存在。
    
- 静态路由是否进入路由表。
    
- OSPF 邻居是否建立。
    
- OSPF 是否学习到预期路由。
    
- ping / tracert 是否符合预期路径。
    
- 如果 ping 不通，应同时检查回程路由、ACL、安全策略、接口状态和目标主机网关。
    

## 回滚 / undo 命令

### 删除静态路由

```text
system-view
undo ip route-static 10.0.30.0 255.255.255.0 10.0.12.2
return
```

### 删除默认路由

```text
system-view
undo ip route-static 0.0.0.0 0.0.0.0 203.0.113.1
return
```

### 删除主备静态路由

```text
system-view
undo ip route-static 10.0.30.0 255.255.255.0 10.0.12.2
undo ip route-static 10.0.30.0 255.255.255.0 10.0.13.2
return
```

### 删除 OSPF 进程

```text
system-view
undo ospf 1
return
```

说明：

- `undo ospf 1` 会删除整个 OSPF 进程，影响所有通过该进程学习或发布的路由。
    
- 如果只想移除某个网段宣告，应进入对应 OSPF area 视图删除对应 `network` 命令，而不是直接删除整个 OSPF 进程。
    

### 删除 OSPF 单个 network 宣告

```text
system-view
ospf 1
area 0.0.0.0
undo network 10.0.30.0 0.0.0.255
quit
quit
return
```

## 高风险与注意事项

1. 删除默认路由可能导致设备无法访问互联网或上级网络。
    
2. 删除 OSPF 进程会影响多个网段的动态路由学习。
    
3. 路由可达不等于业务允许访问，访问控制仍应由 ACL 或安全策略决定。
    
4. 配置默认路由前，应确认是否已有更合适的出口路径。
    
5. 静态路由需要考虑回程路由，否则可能出现单向通信。
    
6. OSPF network 使用通配符掩码，不是子网掩码。
    
7. OSPF 邻居不建立时，应检查接口状态、IP 网段、区域 ID、认证、网络类型和防火墙/ACL。
    
8. 查询命令 `display ...`、`ping`、`tracert` 不应混入正式配置块。
    
9. RIP 和 BGP 不建议作为基础场景默认生成配置。
    

## 常见错误

1. 静态路由下一跳不可达，导致路由无法正常生效。
    
2. 配置静态路由时忘记回程路由，导致请求能到达但响应回不来。
    
3. 默认路由下一跳写错，导致所有未知目的流量走错路径。
    
4. OSPF 区域 ID 不一致，邻居无法建立。
    
5. OSPF `network` 命令把通配符掩码误写成子网掩码。
    
6. 以为 OSPF 会自动创建接口 IP，实际 OSPF 只宣告已存在接口网段。
    
7. 以为路由通了就等于访问策略允许，实际 ACL 仍可能阻断流量。
    
8. preference 理解错误，导致主备路由选路不符合预期。
    
9. 删除 OSPF 进程进行回滚，误伤其他业务网段。
    
10. 只看路由表存在就判断业务一定可达，没有检查 ACL、NAT、接口和主机网关。