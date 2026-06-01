---
id: huawei-acl-security  
title: Huawei ACL And Security Policy Commands  
vendor: HUAWEI  
platform: VRP  
feature: ACL_SECURITY  
tags: [acl, security-policy, traffic-filter, permit, deny, inbound, outbound]  
knowledgeType: COMMAND_REFERENCE  
generationSourceType: RAG  
riskLevel: MEDIUM  
status: READY  
version: 1.0.0  
updatedAt: 2026-06-02

---
# ACL 与安全控制

## 这个模块解决什么问题

本模块用于说明华为 VRP 设备中的基础 ACL、高级 ACL、ACL 规则顺序、ACL 绑定接口方向和 ACL 回滚方法。

ACL 用于控制不同网段、不同 VLAN、不同业务区域、不同主机之间是否允许通信。常见场景包括：

1. 办公区允许访问服务器区。
    
2. 访客区禁止访问服务器区。
    
3. 办公区和访客区互相隔离。
    
4. 只允许指定协议或端口访问服务器。
    
5. 限制某个源网段访问某个目标网段。
    

ConfigurationAgent 在生成 ACL 配置时，应根据 `NetworkPlan.securityPolicyPlan` 生成结构化配置块，至少包含：

1. ACL 规则配置。
    
2. ACL 应用到接口的配置。
    
3. 验证命令。
    
4. 回滚命令。
    
5. 方向和位置说明。
    
6. `traceRefs`，用于追溯到对应的安全策略规划项和意图关系。
    

## 本模块不负责什么

本模块不负责设备登录认证、AAA、RADIUS、端口安全、802.1X、SSH/Telnet 用户管理等能力。

这些内容属于设备管理安全或接入认证，建议以后单独拆成：

```text
advanced_device_management_security.md
```

或：

```text
port_security_and_aaa.md
```

在当前 MAC-TAV 项目基础场景中，本文件只作为 ACL 访问控制知识库使用。

## ACL 基础概念

|概念|说明|
|---|---|
|基础 ACL|通常使用编号 `2000-2999`，主要匹配源 IP 地址|
|高级 ACL|通常使用编号 `3000-3999`，可匹配源地址、目的地址、协议、端口等|
|二层 ACL|通常使用编号 `4000-4999`，匹配二层 MAC 等信息，基础场景不默认使用|
|rule|ACL 中的规则，按照匹配顺序决定 permit 或 deny|
|permit|允许匹配流量|
|deny|拒绝匹配流量|
|wildcard-mask|通配符掩码，`0` 表示精确匹配，`1` 表示忽略|
|inbound|流量进入接口方向|
|outbound|流量离开接口方向|
|traffic-filter|在接口上应用 ACL 进行报文过滤|

## ACL 编号范围

|类型|编号范围|常见用途|
|---|---|---|
|基础 ACL|`2000-2999`|只按源 IP 过滤|
|高级 ACL|`3000-3999`|按源 IP、目的 IP、协议、端口过滤|
|二层 ACL|`4000-4999`|按二层 MAC 等字段过滤，当前基础场景不默认使用|

## ACL 规则设计原则

1. 先写更具体的规则，再写更宽泛的规则。
    
2. 明确拒绝规则后，通常要根据业务需要补充允许规则。
    
3. 不要把 `permit ip` 随意放在所有 ACL 最后，除非确实希望其他流量全部放行。
    
4. 访客区禁止访问服务器区这类需求，通常应使用高级 ACL。
    
5. ACL 是否生效，不只取决于规则，还取决于应用接口和方向。
    
6. 同一 ACL 被多个接口复用时，修改规则可能影响多个业务路径。
    
7. ACL 负责访问控制，路由负责可达性，NAT 负责地址转换，三者职责不同。
    

## 常用命令

### 创建基础 ACL

```text
acl number <acl-number>
rule <rule-id> deny source <src-ip> <src-wildcard>
rule <rule-id> permit source <src-ip> <src-wildcard>
```

示例：

```text
acl number 2000
rule 5 deny source 192.168.20.0 0.0.0.255
rule 10 permit source 192.168.10.0 0.0.0.255
```

说明：

- 基础 ACL 主要匹配源 IP。
    
- 不适合表达“源区域访问目标服务器区”的完整五元组规则。
    
- 如果需要匹配目标地址、协议或端口，应使用高级 ACL。
    

### 创建高级 ACL

```text
acl number <acl-number>
rule <rule-id> deny ip source <src-ip> <src-wildcard> destination <dst-ip> <dst-wildcard>
rule <rule-id> permit ip source <src-ip> <src-wildcard> destination <dst-ip> <dst-wildcard>
rule <rule-id> deny tcp source <src-ip> <src-wildcard> destination <dst-ip> <dst-wildcard> destination-port eq <port>
rule <rule-id> permit tcp source <src-ip> <src-wildcard> destination <dst-ip> <dst-wildcard> destination-port eq <port>
```

示例：

```text
acl number 3000
rule 5 deny ip source 10.0.20.0 0.0.0.255 destination 10.0.30.0 0.0.0.255
rule 10 permit ip source 10.0.10.0 0.0.0.255 destination 10.0.30.0 0.0.0.255
```

说明：

- 高级 ACL 适合控制区域之间的访问关系。
    
- `source` 后面是源地址和源通配符掩码。
    
- `destination` 后面是目标地址和目标通配符掩码。
    
- `destination-port eq <port>` 用于匹配目标端口，例如 SSH 的 22、HTTP 的 80、HTTPS 的 443。
    

### 将 ACL 应用到接口

```text
interface <interface-name>
traffic-filter inbound acl <acl-number>
traffic-filter outbound acl <acl-number>
```

示例：

```text
interface GigabitEthernet0/0/2
traffic-filter inbound acl 3000
```

说明：

- `inbound` 表示过滤进入该接口的流量。
    
- `outbound` 表示过滤离开该接口的流量。
    
- ACL 方向选择错误是访问控制失效的常见原因。
    
- ACL 应用位置应结合流量路径、源区域、目标区域和设备拓扑判断。
    
- 对于访客区访问服务器区的 deny 策略，常见做法是在靠近访客区入口或策略执行点的接口入方向应用 ACL，但最终应以 `NetworkPlan.securityPolicyPlan.enforcementPoints` 为准。
    

### 查看 ACL

```text
display acl all
display acl <acl-number>
display traffic-filter applied-record
display current-configuration interface <interface-name>
```

说明：

- `display acl all`：查看所有 ACL。
    
- `display acl <acl-number>`：查看指定 ACL。
    
- `display traffic-filter applied-record`：查看 ACL 应用记录。
    
- `display current-configuration interface`：查看接口上是否应用了 ACL。
    

### 取消接口上的 ACL

```text
interface <interface-name>
undo traffic-filter inbound
undo traffic-filter outbound
```

说明：

- 取消 ACL 绑定可能导致原本被阻断的流量恢复通过。
    
- 回滚时应明确取消的是 inbound 还是 outbound。
    

### 删除 ACL 规则或 ACL

```text
acl number <acl-number>
undo rule <rule-id>
quit
undo acl number <acl-number>
```

说明：

- `undo rule <rule-id>` 只删除单条规则。
    
- `undo acl number <acl-number>` 删除整个 ACL。
    
- 删除整个 ACL 前必须确认它是否仍被接口引用。
    

## 参数解释

|参数|说明|
|---|---|
|acl-number|ACL 编号，例如基础 ACL `2000`，高级 ACL `3000`|
|rule-id|规则编号，数值越小通常越靠前匹配，例如 `5`、`10`、`20`|
|src-ip|源网络或源主机地址，例如 `10.0.20.0`|
|src-wildcard|源通配符掩码，例如 `/24` 对应 `0.0.0.255`|
|dst-ip|目标网络或目标主机地址，例如 `10.0.30.0`|
|dst-wildcard|目标通配符掩码，例如单主机对应 `0.0.0.0`|
|protocol|协议，例如 `ip`、`tcp`、`udp`、`icmp`|
|port|目标端口，例如 SSH `22`、HTTP `80`、HTTPS `443`|
|interface-name|应用 ACL 的接口，例如 `GigabitEthernet0/0/2`|
|inbound|入方向，流量进入接口|
|outbound|出方向，流量离开接口|

## 通配符掩码说明

|子网|通配符掩码|
|---|---|
|`10.0.20.0/24`|`0.0.0.255`|
|`10.0.20.0/25`|`0.0.0.127`|
|`10.0.20.10/32`|`0.0.0.0`|
|`0.0.0.0/0`|`255.255.255.255`|

说明：

- 通配符掩码不是子网掩码。
    
- `0.0.0.255` 表示前三段精确匹配，最后一段任意。
    
- `0.0.0.0` 表示单个主机精确匹配。
    
- ACL 规则中写错通配符掩码会导致匹配范围错误。
    

## 配置示例

### 示例 1：访客区禁止访问服务器区

场景：

- 访客区网段：`10.0.20.0/24`
    
- 服务器区网段：`10.0.30.0/24`
    
- ACL 编号：`3000`
    
- 执行接口：`GigabitEthernet0/0/2`
    
- 执行方向：`inbound`
    

```text
<Huawei> system-view
[Huawei] acl number 3000
[Huawei-acl-adv-3000] rule 5 deny ip source 10.0.20.0 0.0.0.255 destination 10.0.30.0 0.0.0.255
[Huawei-acl-adv-3000] rule 10 permit ip
[Huawei-acl-adv-3000] quit
[Huawei] interface GigabitEthernet0/0/2
[Huawei-GigabitEthernet0/0/2] traffic-filter inbound acl 3000
[Huawei-GigabitEthernet0/0/2] quit
[Huawei] return
<Huawei> display acl 3000
<Huawei> display traffic-filter applied-record
```

适用场景：

- 访客区不能访问服务器区。
    
- 其他未禁止流量按规划允许通过。
    
- 需要在正确的策略执行点应用 ACL。
    

注意：

- `rule 10 permit ip` 表示允许未被前面 deny 命中的其他 IP 流量。
    
- 如果安全策略要求默认拒绝，则不应简单补充 `permit ip`。
    
- 是否补充 `permit ip` 应以 `NetworkPlan.securityPolicyPlan` 为准。
    

### 示例 2：办公区允许访问服务器 HTTPS，禁止访客区访问服务器

场景：

- 办公区网段：`10.0.10.0/24`
    
- 访客区网段：`10.0.20.0/24`
    
- 服务器地址：`10.0.30.10`
    
- 允许办公区访问 HTTPS：TCP 443
    
- 禁止访客区访问服务器
    
- ACL 编号：`3001`
    

```text
<Huawei> system-view
[Huawei] acl number 3001
[Huawei-acl-adv-3001] rule 5 permit tcp source 10.0.10.0 0.0.0.255 destination 10.0.30.10 0.0.0.0 destination-port eq 443
[Huawei-acl-adv-3001] rule 10 deny ip source 10.0.20.0 0.0.0.255 destination 10.0.30.10 0.0.0.0
[Huawei-acl-adv-3001] rule 20 permit ip
[Huawei-acl-adv-3001] quit
[Huawei] interface GigabitEthernet0/0/2
[Huawei-GigabitEthernet0/0/2] traffic-filter inbound acl 3001
[Huawei-GigabitEthernet0/0/2] quit
[Huawei] return
<Huawei> display acl 3001
<Huawei> display traffic-filter applied-record
```

适用场景：

- 需要按协议和端口控制访问。
    
- 需要允许某类业务，同时拒绝另一类访问。
    

注意：

- 规则顺序很重要。更具体的 permit / deny 应放在更宽泛规则前面。
    
- `rule 20 permit ip` 是否保留取决于整体安全策略。
    

### 示例 3：阻止办公区访问服务器 SSH

场景：

- 办公区网段：`10.0.10.0/24`
    
- 服务器地址：`10.0.30.10`
    
- 禁止访问 SSH：TCP 22
    
- ACL 编号：`3002`
    

```text
<Huawei> system-view
[Huawei] acl number 3002
[Huawei-acl-adv-3002] rule 5 deny tcp source 10.0.10.0 0.0.0.255 destination 10.0.30.10 0.0.0.0 destination-port eq 22
[Huawei-acl-adv-3002] rule 10 permit ip
[Huawei-acl-adv-3002] quit
[Huawei] interface GigabitEthernet0/0/1
[Huawei-GigabitEthernet0/0/1] traffic-filter inbound acl 3002
[Huawei-GigabitEthernet0/0/1] quit
[Huawei] return
<Huawei> display acl 3002
<Huawei> display traffic-filter applied-record
```

适用场景：

- 只禁止某个端口。
    
- 其他业务流量允许通过。
    

## 验证命令

```text
display acl all
display acl 3000
display traffic-filter applied-record
display current-configuration interface GigabitEthernet0/0/2
display ip routing-table
ping 10.0.30.10
tracert 10.0.30.10
```

验证关注点：

- ACL 是否存在。
    
- ACL 规则顺序是否正确。
    
- deny / permit 动作是否符合安全策略。
    
- 源地址、目标地址和通配符掩码是否正确。
    
- ACL 是否应用到正确接口。
    
- ACL 方向是否正确。
    
- 测试流量是否经过该接口和方向。
    
- 路由是否正常，避免把路由问题误判为 ACL 问题。
    

## 回滚 / undo 命令

### 取消接口 inbound ACL

```text
system-view
interface GigabitEthernet0/0/2
undo traffic-filter inbound
quit
return
```

### 取消接口 outbound ACL

```text
system-view
interface GigabitEthernet0/0/2
undo traffic-filter outbound
quit
return
```

### 删除单条 ACL 规则

```text
system-view
acl number 3000
undo rule 5
quit
return
```

### 删除整个 ACL

```text
system-view
undo acl number 3000
return
```

说明：

- 删除整个 ACL 前，应先确认没有接口继续引用该 ACL。
    
- 更安全的回滚方式通常是先取消接口绑定，再删除 ACL。
    
- 如果只修复某条错误规则，优先使用 `undo rule <rule-id>`，不要直接删除整个 ACL。
    

## 高风险与注意事项

1. ACL 方向错误会导致策略不生效或误阻断正常业务。
    
2. ACL 绑定接口错误会导致流量根本没有经过该策略。
    
3. 删除 ACL 或取消接口绑定可能导致原本禁止的流量被放行。
    
4. 错误的 deny 规则可能阻断办公区访问服务器、互联网访问或管理流量。
    
5. 错误的 permit 规则可能放行访客区访问服务器等高风险流量。
    
6. `permit ip` 不应无脑添加，应根据安全策略决定。
    
7. 修改被多个接口复用的 ACL 会影响所有引用该 ACL 的接口。
    
8. 查询命令 `display ...`、`ping`、`tracert` 不应混入正式配置块。
    
9. ACL 只能控制经过设备的流量，不能控制不经过该设备的旁路流量。
    
10. ACL 访问控制不替代路由配置，路由不通时 ACL 无法单独解决可达性问题。
    

## 常见错误

1. ACL 方向写错，把应该配置 `inbound` 的地方配置成 `outbound`。
    
2. ACL 绑定接口写错，导致策略没有命中真实流量路径。
    
3. ACL 规则顺序错误，流量先命中了宽泛 permit，后续 deny 不再生效。
    
4. deny 规则后是否需要 permit 没有根据业务策略判断，导致误放行或误阻断。
    
5. 通配符掩码写成子网掩码，例如把 `0.0.0.255` 写成 `255.255.255.0`。
    
6. 基础 ACL 只能匹配源地址，却被用来表达源到目标服务器的访问控制。
    
7. 高级 ACL 中源地址和目标地址写反。
    
8. ACL 已创建但没有绑定到接口。
    
9. ACL 已绑定到接口，但流量没有经过该接口或方向。
    
10. 只看 ping 不通就认为 ACL 生效，实际可能是路由、网关、ARP、NAT 或目标主机防火墙问题。
    
11. 删除整个 ACL 作为回滚，误删了其他策略共用的规则。
    
12. 把 AAA、RADIUS、端口安全等管理安全配置混入普通访问控制配置块。
