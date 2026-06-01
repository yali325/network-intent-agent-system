id: huawei-interface-ip  
title: Huawei Interface And IP Configuration Commands  
vendor: HUAWEI  
platform: VRP  
feature: INTERFACE_IP  
tags: [interface, ip-address, gateway, vlanif, loopback, shutdown]  
knowledgeType: COMMAND_REFERENCE  
generationSourceType: RAG  
riskLevel: LOW  
status: READY  
version: 1.0.0  
updatedAt: 2026-06-02

---
# 接口与 IP 地址配置

## 这个模块解决什么问题

本模块用于说明华为 VRP 设备中接口视图的基础配置方法，包括进入接口视图、配置接口描述、启用或关闭接口、配置三层接口 IP 地址、查看接口状态和回滚接口基础配置。

接口配置是 VLAN、路由、ACL、NAT 等网络配置的基础。ConfigurationAgent 在生成配置时，应先判断接口的角色：二层交换接口、三层路由接口，还是 Vlanif 网关接口，避免把 IP 地址配置到不合适的接口上。

## 接口类型说明

|接口类型|示例|常见用途|
|---|---|---|
|物理以太网接口|`GigabitEthernet0/0/1`|连接主机、交换机、路由器、防火墙等设备|
|三层物理接口|`GigabitEthernet0/0/1`|作为路由口配置 IP 地址，用于三层转发|
|二层交换接口|`GigabitEthernet0/0/1`|配置 access / trunk，通常不直接配置 IP 地址|
|Vlanif 接口|`Vlanif10`|作为 VLAN 的三层网关接口|
|LoopBack 接口|`LoopBack0`|常用于 Router ID、管理地址或测试地址|

说明：

- 如果接口用于二层接入，例如办公区主机接入口，通常配置 VLAN，不直接配置 IP 地址。
    
- 如果接口用于三层互联，例如路由器之间点到点连接，可以直接配置 IP 地址。
    
- 如果交换机通过 VLAN 提供网关，通常在 `Vlanif` 接口上配置 IP 地址。
    

## 常用命令

### 查看接口状态

```text
display interface
display interface brief
display interface GigabitEthernet0/0/1
display ip interface brief
display ip interface GigabitEthernet0/0/1
display counters interface GigabitEthernet0/0/1
```

说明：

- `display interface`：查看接口详细状态。
    
- `display interface brief`：查看所有接口的简要状态。
    
- `display ip interface brief`：查看接口 IP 地址摘要。
    
- `display ip interface GigabitEthernet0/0/1`：查看指定接口的 IP 配置和统计信息。
    
- `display counters interface`：查看接口收发包统计。
    

### 进入接口视图

```text
interface GigabitEthernet0/0/1
interface Vlanif10
interface LoopBack0
```

推荐统一写法：

```text
interface GigabitEthernet0/0/1
```

说明：

- 华为 VRP 中接口类型和接口编号可以紧挨着写，也可以用空格分隔。
    
- 为了让知识库生成风格统一，建议统一写成 `GigabitEthernet0/0/1`。
    

### 接口描述

```text
description <description-text>
```

示例：

```text
description to_server_1
description to_office_switch
description uplink_to_core
```

说明：

- 接口描述用于标记接口连接对象，方便排障和审计。
    
- 建议描述中包含连接方向或对端设备，例如 `to_server_1`、`to_guest_switch`。
    

### 启用和关闭接口

```text
undo shutdown
shutdown
```

说明：

- `undo shutdown`：启用接口。
    
- `shutdown`：关闭接口。
    
- 在真实设备环境中，`shutdown` 可能中断业务，应视为高风险操作。
    
- 如果只是生成配置建议，ConfigurationAgent 不应随意关闭已有生产接口。
    

### 配置接口 IP 地址

```text
ip address <ip-address> <subnet-mask>
```

示例：

```text
ip address 192.168.1.1 255.255.255.0
```

说明：

- 该命令应配置在三层接口或 Vlanif 接口上。
    
- 如果接口是二层 access / trunk 口，通常不应直接配置 IP 地址。
    
- IP 地址应来自 `NetworkPlan.addressPlan` 中的网关地址或链路地址。
    

### 删除接口 IP 地址

```text
undo ip address
```

说明：

- `undo ip address` 用于删除接口上的 IP 地址。
    
- 如果接口有多个 IP 地址或存在特殊地址配置，删除前应先通过 `display ip interface` 确认。
    

### 速率和双工模式

```text
speed auto
speed 1000
duplex auto
duplex full
```

说明：

- 默认建议使用自动协商。
    
- 只有在明确知道两端设备速率和双工要求时，才手动配置 `speed` 和 `duplex`。
    
- 两端速率或双工不一致可能导致丢包、错包或链路不稳定。
    

### MTU 配置

```text
mtu <size>
```

示例：

```text
mtu 1500
```

说明：

- MTU 表示最大传输单元。
    
- 普通场景下保持默认值即可。
    
- 修改 MTU 可能影响大包转发、隧道、存储网络或特殊业务流量，应谨慎使用。
    

## 参数解释

|参数|说明|
|---|---|
|interface-type|接口类型，例如 `GigabitEthernet`、`Vlanif`、`LoopBack`|
|interface-number|接口编号，例如 `0/0/1`、`10`、`0`|
|description-text|接口描述文本，建议使用能表达对端或用途的名称|
|ip-address|接口 IP 地址，例如 `192.168.1.1`|
|subnet-mask|子网掩码，例如 `255.255.255.0`|
|speed|接口速率，例如 `100`、`1000`、`auto`|
|duplex|双工模式，例如 `full`、`half`、`auto`|
|mtu|最大传输单元，常见默认值为 `1500`|

## 配置示例

### 示例 1：配置三层物理接口 IP

```text
<Huawei> system-view
[Huawei] interface GigabitEthernet0/0/1
[Huawei-GigabitEthernet0/0/1] description to_server_1
[Huawei-GigabitEthernet0/0/1] ip address 192.168.1.1 255.255.255.0
[Huawei-GigabitEthernet0/0/1] undo shutdown
[Huawei-GigabitEthernet0/0/1] quit
[Huawei] return
<Huawei> display ip interface GigabitEthernet0/0/1
<Huawei> display interface GigabitEthernet0/0/1
```

适用场景：

- 路由器三层接口。
    
- 三层交换机路由口。
    
- 点到点三层链路。
    

### 示例 2：配置 Vlanif 网关接口 IP

```text
<Huawei> system-view
[Huawei] interface Vlanif10
[Huawei-Vlanif10] description office_gateway
[Huawei-Vlanif10] ip address 10.0.10.1 255.255.255.0
[Huawei-Vlanif10] undo shutdown
[Huawei-Vlanif10] quit
[Huawei] return
<Huawei> display ip interface brief
```

适用场景：

- 办公区 VLAN 网关。
    
- 访客区 VLAN 网关。
    
- 服务器区 VLAN 网关。
    

说明：

- Vlanif 接口通常需要对应 VLAN 已经存在。
    
- 如果 Vlanif 对应 VLAN 没有创建，网关接口可能无法正常工作。
    

### 示例 3：只配置二层接口描述和启用状态

```text
<Huawei> system-view
[Huawei] interface GigabitEthernet0/0/2
[Huawei-GigabitEthernet0/0/2] description to_office_pc
[Huawei-GigabitEthernet0/0/2] undo shutdown
[Huawei-GigabitEthernet0/0/2] quit
[Huawei] return
<Huawei> display interface brief
```

适用场景：

- 普通主机接入口。
    
- 后续将由 VLAN 模块配置 access / trunk。
    

说明：

- 二层接口通常不直接配置 `ip address`。
    
- 二层接口的 VLAN 配置应放在 VLAN 与二层交换模块中。
    

## 验证命令

```text
display interface brief
display interface GigabitEthernet0/0/1
display ip interface brief
display ip interface GigabitEthernet0/0/1
display counters interface GigabitEthernet0/0/1
display this
```

验证关注点：

- 接口物理状态是否为 up。
    
- 接口协议状态是否为 up。
    
- 接口 IP 地址是否符合规划。
    
- 接口描述是否符合预期。
    
- 接口是否被 shutdown。
    
- 接口收发包计数是否增长。
    
- 当前视图下配置是否符合预期。
    

## 回滚 / undo 命令

### 回滚接口描述

```text
system-view
interface GigabitEthernet0/0/1
undo description
quit
```

### 删除接口 IP 地址

```text
system-view
interface GigabitEthernet0/0/1
undo ip address
quit
```

### 关闭接口

```text
system-view
interface GigabitEthernet0/0/1
shutdown
quit
```

说明：

- `shutdown` 会关闭接口，可能中断业务。
    
- 如果只是回滚新启用的实验接口，可以使用。
    
- 对真实生产接口执行 `shutdown` 前必须人工确认。
    

### 恢复速率和双工为自动协商

```text
system-view
interface GigabitEthernet0/0/1
undo speed
undo duplex
quit
```

### 回滚 Vlanif 接口 IP

```text
system-view
interface Vlanif10
undo ip address
quit
```

## 高风险与注意事项

1. `shutdown` 会关闭接口，可能造成业务中断。
    
2. 手动修改 `speed` 和 `duplex` 可能导致链路协商异常。
    
3. 修改 MTU 可能导致大包不通或路径 MTU 问题。
    
4. 在二层物理接口上配置 IP 地址通常不是办公区 / 访客区 / 服务器区接入场景的正确做法。
    
5. Vlanif 接口需要对应 VLAN 存在，且 VLAN 内至少有相关二层链路或端口配置。
    

## 常见错误

1. 配置 IP 后忘记执行 `undo shutdown`，接口仍处于关闭状态。
    
2. 在二层接口上配置 IP，导致规划和配置不一致。
    
3. 本应在 `Vlanif` 上配置网关，却错误配置到了物理 access 接口。
    
4. 接口编号写错，例如把 `GigabitEthernet0/0/1` 写成 `GigabitEthernet0/1/0`。
    
5. 接口速率或双工模式两端不一致，导致丢包或链路不稳定。
    
6. MTU 不一致，导致小包可通、大包不通。
    
7. 只看 `display interface brief` 的 down 状态就判断配置错误，实际可能是线缆、对端设备或协商问题。
    
8. 把 `display ...` 查询命令混入正式配置块。
    
9. 回滚时直接执行 `shutdown`，没有意识到它会中断接口流量。
    
10. 忘记保存配置，设备重启后接口配置丢失。