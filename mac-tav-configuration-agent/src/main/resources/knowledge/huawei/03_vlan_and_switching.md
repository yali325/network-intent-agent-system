---
id: huawei-vlan-switching  
title: Huawei VLAN And Switching Commands  
vendor: HUAWEI  
platform: VRP  
feature: VLAN_SWITCHING  
tags: [vlan, access, trunk, hybrid, vlanif, switching, port]  
knowledgeType: COMMAND_REFERENCE  
generationSourceType: RAG  
riskLevel: LOW  
status: READY  
version: 1.0.0  
updatedAt: 2026-06-02

---
# VLAN 与二层交换配置

## 这个模块解决什么问题

本模块用于说明华为 VRP 设备中的 VLAN、Access 端口、Trunk 端口和 Vlanif 网关接口的基础配置方法。

VLAN 用于把一个物理交换网络划分为多个逻辑广播域。常见场景包括办公区、访客区、服务器区隔离。Access 端口通常连接终端主机，Trunk 端口通常连接交换机、路由器或防火墙。Vlanif 接口用于给某个 VLAN 提供三层网关能力。

ConfigurationAgent 在生成配置时，应区分以下配置块：

1. VLAN 创建配置。
    
2. Access 接入口配置。
    
3. Trunk 上联口配置。
    
4. Vlanif 网关接口配置。
    
5. 验证命令。
    
6. 回滚命令。
    

## 基础概念

|概念|说明|
|---|---|
|VLAN|虚拟局域网，用于划分二层广播域|
|Access 端口|通常连接 PC、服务器、摄像头、AP 等终端，只属于一个业务 VLAN|
|Trunk 端口|通常连接交换机、路由器、防火墙等设备，可以允许多个 VLAN 通过|
|Hybrid 端口|同时支持 tagged / untagged 规则，适合复杂场景，基础配置阶段不建议默认使用|
|Vlanif 接口|VLAN 的三层网关接口，可以配置 IP 地址|
|PVID|端口默认 VLAN，Access 端口的默认 VLAN 通常就是终端所属 VLAN|

## 常用命令

### 创建 VLAN

```text
vlan <vlan-id>
vlan batch <vlan-id-list>
undo vlan <vlan-id>
undo vlan batch <vlan-id-list>
```

示例：

```text
vlan 10
vlan batch 10 20 30
vlan batch 10 to 20
```

说明：

- `vlan <vlan-id>`：创建单个 VLAN 并进入 VLAN 视图。
    
- `vlan batch`：批量创建 VLAN。
    
- `undo vlan`：删除指定 VLAN。
    
- `undo vlan batch`：批量删除 VLAN。
    

### VLAN 描述

```text
description <description-text>
```

示例：

```text
vlan 10
description OFFICE
```

说明：

- VLAN 描述用于标记 VLAN 用途。
    
- 建议描述与业务区域一致，例如 `OFFICE`、`GUEST`、`SERVER`。
    

### 配置 Access 端口

```text
interface GigabitEthernet0/0/1
port link-type access
port default vlan <vlan-id>
```

示例：

```text
interface GigabitEthernet0/0/1
description to_office_pc
port link-type access
port default vlan 10
```

说明：

- Access 端口通常连接终端设备。
    
- Access 端口一般只承载一个 VLAN。
    
- `port default vlan` 用于指定 Access 端口所属 VLAN。
    

### 配置 Trunk 端口

```text
interface GigabitEthernet0/0/24
port link-type trunk
port trunk allow-pass vlan <vlan-id-list>
```

示例：

```text
interface GigabitEthernet0/0/24
description uplink_to_core
port link-type trunk
port trunk allow-pass vlan 10 20 30
```

说明：

- Trunk 端口通常用于交换机之间互联，或交换机连接路由器、防火墙、虚拟化宿主机等。
    
- 只有被 `port trunk allow-pass vlan` 允许的 VLAN 才能通过该 trunk 链路。
    
- 如果跨交换机 VLAN 不通，应优先检查两端 trunk 是否都允许了对应 VLAN。
    

### 配置 Vlanif 网关接口

```text
interface Vlanif<vlan-id>
ip address <gateway-ip> <subnet-mask>
```

示例：

```text
interface Vlanif10
description office_gateway
ip address 10.0.10.1 255.255.255.0
```

说明：

- Vlanif 接口是 VLAN 的三层网关接口。
    
- 如果只做二层隔离，不一定需要创建 Vlanif。
    
- 如果需要不同 VLAN 之间路由，或需要该 VLAN 的网关，就需要配置 Vlanif。
    
- Vlanif 对应的 VLAN 应该已经存在。
    

### Hybrid 端口

```text
port link-type hybrid
port hybrid tagged vlan <vlan-id-list>
port hybrid untagged vlan <vlan-id-list>
```

说明：

- Hybrid 端口适合更复杂的 VLAN 标签处理场景。
    
- 基础企业网场景优先使用 Access + Trunk。
    
- 除非 NetworkPlan 明确要求 Hybrid，否则 ConfigurationAgent 不应默认生成 Hybrid 配置。
    

## 参数解释

|参数|说明|
|---|---|
|vlan-id|VLAN 编号，常见范围为 1-4094|
|vlan-id-list|VLAN 列表，例如 `10 20 30` 或 `10 to 20`|
|description-text|描述文本，例如 `OFFICE`、`GUEST`、`SERVER`|
|gateway-ip|VLAN 网关地址，例如 `10.0.10.1`|
|subnet-mask|子网掩码，例如 `255.255.255.0`|
|interface|接口名称，例如 `GigabitEthernet0/0/1`|
|tagged|发送或接收时保留 VLAN 标签|
|untagged|发送时去掉 VLAN 标签|

## 配置示例

### 示例 1：创建办公区、访客区、服务器区 VLAN

```text
<Huawei> system-view
[Huawei] vlan batch 10 20 30
[Huawei] vlan 10
[Huawei-vlan10] description OFFICE
[Huawei-vlan10] quit
[Huawei] vlan 20
[Huawei-vlan20] description GUEST
[Huawei-vlan20] quit
[Huawei] vlan 30
[Huawei-vlan30] description SERVER
[Huawei-vlan30] quit
```

适用场景：

- 只创建 VLAN。
    
- 后续再分别配置 access 端口、trunk 端口和 Vlanif 网关。
    

### 示例 2：配置 Access 端口

```text
<Huawei> system-view
[Huawei] interface GigabitEthernet0/0/1
[Huawei-GigabitEthernet0/0/1] description to_office_pc
[Huawei-GigabitEthernet0/0/1] port link-type access
[Huawei-GigabitEthernet0/0/1] port default vlan 10
[Huawei-GigabitEthernet0/0/1] quit

[Huawei] interface GigabitEthernet0/0/2
[Huawei-GigabitEthernet0/0/2] description to_guest_pc
[Huawei-GigabitEthernet0/0/2] port link-type access
[Huawei-GigabitEthernet0/0/2] port default vlan 20
[Huawei-GigabitEthernet0/0/2] quit

[Huawei] interface GigabitEthernet0/0/3
[Huawei-GigabitEthernet0/0/3] description to_server
[Huawei-GigabitEthernet0/0/3] port link-type access
[Huawei-GigabitEthernet0/0/3] port default vlan 30
[Huawei-GigabitEthernet0/0/3] quit
```

适用场景：

- 主机、服务器、普通终端接入口。
    
- 每个接入口只属于一个 VLAN。
    

### 示例 3：配置 Trunk 上联端口

```text
<Huawei> system-view
[Huawei] interface GigabitEthernet0/0/24
[Huawei-GigabitEthernet0/0/24] description uplink_to_core
[Huawei-GigabitEthernet0/0/24] port link-type trunk
[Huawei-GigabitEthernet0/0/24] port trunk allow-pass vlan 10 20 30
[Huawei-GigabitEthernet0/0/24] quit
```

适用场景：

- 接入交换机上联核心交换机。
    
- 交换机之间传递多个 VLAN。
    
- 交换机连接防火墙或路由器子接口场景。
    

### 示例 4：配置 Vlanif 网关接口

```text
<Huawei> system-view
[Huawei] interface Vlanif10
[Huawei-Vlanif10] description office_gateway
[Huawei-Vlanif10] ip address 10.0.10.1 255.255.255.0
[Huawei-Vlanif10] quit

[Huawei] interface Vlanif20
[Huawei-Vlanif20] description guest_gateway
[Huawei-Vlanif20] ip address 10.0.20.1 255.255.255.0
[Huawei-Vlanif20] quit

[Huawei] interface Vlanif30
[Huawei-Vlanif30] description server_gateway
[Huawei-Vlanif30] ip address 10.0.30.1 255.255.255.0
[Huawei-Vlanif30] quit
```

适用场景：

- 三层交换机作为各 VLAN 的默认网关。
    
- 后续通过路由或 ACL 控制不同 VLAN 之间的访问关系。
    

注意：

- 创建 Vlanif 后，不代表一定允许 VLAN 间互访。
    
- 是否允许 VLAN 间互访还需要结合路由和 ACL / 安全策略判断。
    
- 如果用户要求“访客区不能访问服务器”，不能只配置 Vlanif，还必须结合 ACL 或安全策略限制访问。
    

### 示例 5：完整基础场景

场景：

- 办公区：VLAN 10，网关 `10.0.10.1/24`，接入口 `GigabitEthernet0/0/1`
    
- 访客区：VLAN 20，网关 `10.0.20.1/24`，接入口 `GigabitEthernet0/0/2`
    
- 服务器区：VLAN 30，网关 `10.0.30.1/24`，接入口 `GigabitEthernet0/0/3`
    
- 上联口：`GigabitEthernet0/0/24`，允许 VLAN 10、20、30
    

```text
<Huawei> system-view
[Huawei] vlan batch 10 20 30

[Huawei] vlan 10
[Huawei-vlan10] description OFFICE
[Huawei-vlan10] quit
[Huawei] vlan 20
[Huawei-vlan20] description GUEST
[Huawei-vlan20] quit
[Huawei] vlan 30
[Huawei-vlan30] description SERVER
[Huawei-vlan30] quit

[Huawei] interface GigabitEthernet0/0/1
[Huawei-GigabitEthernet0/0/1] description to_office_pc
[Huawei-GigabitEthernet0/0/1] port link-type access
[Huawei-GigabitEthernet0/0/1] port default vlan 10
[Huawei-GigabitEthernet0/0/1] quit

[Huawei] interface GigabitEthernet0/0/2
[Huawei-GigabitEthernet0/0/2] description to_guest_pc
[Huawei-GigabitEthernet0/0/2] port link-type access
[Huawei-GigabitEthernet0/0/2] port default vlan 20
[Huawei-GigabitEthernet0/0/2] quit

[Huawei] interface GigabitEthernet0/0/3
[Huawei-GigabitEthernet0/0/3] description to_server
[Huawei-GigabitEthernet0/0/3] port link-type access
[Huawei-GigabitEthernet0/0/3] port default vlan 30
[Huawei-GigabitEthernet0/0/3] quit

[Huawei] interface GigabitEthernet0/0/24
[Huawei-GigabitEthernet0/0/24] description uplink_to_core
[Huawei-GigabitEthernet0/0/24] port link-type trunk
[Huawei-GigabitEthernet0/0/24] port trunk allow-pass vlan 10 20 30
[Huawei-GigabitEthernet0/0/24] quit

[Huawei] interface Vlanif10
[Huawei-Vlanif10] description office_gateway
[Huawei-Vlanif10] ip address 10.0.10.1 255.255.255.0
[Huawei-Vlanif10] quit

[Huawei] interface Vlanif20
[Huawei-Vlanif20] description guest_gateway
[Huawei-Vlanif20] ip address 10.0.20.1 255.255.255.0
[Huawei-Vlanif20] quit

[Huawei] interface Vlanif30
[Huawei-Vlanif30] description server_gateway
[Huawei-Vlanif30] ip address 10.0.30.1 255.255.255.0
[Huawei-Vlanif30] quit

[Huawei] return
<Huawei> save
```

## 验证命令

```text
display vlan
display vlan 10
display vlan 20
display vlan 30
display port vlan
display port vlan GigabitEthernet0/0/1
display port vlan GigabitEthernet0/0/24
display interface Vlanif10
display interface Vlanif20
display interface Vlanif30
display ip interface brief
display current-configuration interface GigabitEthernet0/0/1
display current-configuration interface GigabitEthernet0/0/24
```

验证关注点：

- VLAN 是否已经创建。
    
- VLAN 描述是否符合业务区域。
    
- Access 端口是否加入正确 VLAN。
    
- Trunk 端口是否允许目标 VLAN 通过。
    
- Vlanif 接口是否存在。
    
- Vlanif IP 地址是否符合地址规划。
    
- Vlanif 接口状态是否 up。
    
- 当前配置中是否存在预期接口配置。
    

## 回滚 / undo 命令

### 回滚 Access 端口 VLAN

```text
system-view
interface GigabitEthernet0/0/1
undo port default vlan
quit
```

说明：

- 该命令会取消 Access 端口的默认 VLAN 配置。
    
- 如果端口仍需要接入其他 VLAN，应重新配置正确的 `port default vlan`。
    

### 回滚 Trunk 允许 VLAN

```text
system-view
interface GigabitEthernet0/0/24
undo port trunk allow-pass vlan 10 20 30
quit
```

说明：

- 该命令会从 trunk 允许列表中移除 VLAN 10、20、30。
    
- 对生产 trunk 链路执行该命令可能导致对应 VLAN 跨设备通信中断。
    

### 回滚 Vlanif 网关 IP

```text
system-view
interface Vlanif10
undo ip address
quit
```

说明：

- 该命令会删除 VLANIF 接口 IP 地址。
    
- 删除网关 IP 会导致该 VLAN 内终端失去默认网关。
    

### 删除 Vlanif 接口

```text
system-view
undo interface Vlanif10
```

说明：

- 删除 Vlanif 接口会删除该 VLAN 的三层网关能力。
    
- 真实环境执行前必须确认没有业务依赖该网关。
    

### 删除 VLAN

```text
system-view
undo vlan 10
```

说明：

- 删除 VLAN 前，应确认相关端口、Vlanif、ACL、路由、NAT 等配置是否仍引用该 VLAN。
    
- 删除 VLAN 可能影响接入终端和跨设备通信。
    

## 高风险与注意事项

1. 删除 VLAN 会影响所有属于该 VLAN 的端口和业务。
    
2. 修改 trunk allow-pass VLAN 可能导致跨交换机 VLAN 不通。
    
3. 删除 Vlanif IP 会导致该 VLAN 内终端无法通过网关访问其他网段。
    
4. 只创建 VLAN 不能实现跨 VLAN 三层通信，跨 VLAN 通信需要 Vlanif、路由和策略配合。
    
5. 只配置 Vlanif 也不等于允许所有业务访问，访问控制仍应由 ACL 或安全策略决定。
    
6. 基础场景优先使用 Access + Trunk；Hybrid 属于复杂场景，不建议默认生成。
    
7. 查询命令 `display ...` 不应混入正式配置块。
    

## 常见错误

1. 只创建 VLAN，忘记把 access 端口加入 VLAN。
    
2. Access 端口配置了 `port trunk allow-pass vlan`，但该命令只适用于 trunk 端口。
    
3. Trunk 端口忘记配置 `port trunk allow-pass vlan`，导致目标 VLAN 不能跨设备通过。
    
4. 跨交换机两端 trunk 允许的 VLAN 不一致。
    
5. 创建了 Vlanif，但忘记创建对应 VLAN。
    
6. 创建了 Vlanif，但对应 VLAN 没有可用二层端口或 trunk 链路，导致 Vlanif 状态异常。
    
7. 以为创建 Vlanif 后 VLAN 间就一定可以互访，实际还可能被 ACL、路由或安全策略限制。
    
8. 删除 VLAN 前没有检查端口、Vlanif、ACL、路由和 NAT 依赖。
    
9. 把用户终端接入口配置成 trunk，导致终端无法正常通信。
    
10. 把交换机上联口配置成 access，导致多个 VLAN 无法通过。
