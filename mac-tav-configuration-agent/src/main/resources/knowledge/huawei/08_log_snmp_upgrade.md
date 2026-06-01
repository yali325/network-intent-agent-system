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

# 日志与 SNMP 监控

## 这个模块解决什么问题

本模块用于说明华为 VRP 设备中的基础日志管理、远程日志服务器、日志查看、SNMP 监控、SNMP Trap 上报和设备运行状态查看。

本模块主要服务以下场景：

1. 配置设备把日志发送到日志服务器。
    
2. 查看设备本地日志和告警缓冲区。
    
3. 配置 SNMP 监控，让网管系统读取设备状态。
    
4. 配置 SNMP Trap，让设备主动上报告警。
    
5. 为 ConfigurationAgent 生成监控类独立配置块或验证建议。
    

本模块不应和 VLAN、接口、路由、ACL、NAT 等业务转发配置混在一个 `CommandBlock` 中。

## 本模块不负责什么

本模块不负责设备升级、配置恢复、文件删除、清空配置和重启设备。

以下命令属于高风险维护操作，不应作为本模块默认配置生成：

```text
startup system-software
startup saved-configuration
load configuration
reset saved-configuration
reboot
reboot fast
delete /unreserved
reset recycle-bin
```

这些内容应放入维护与备份模块，并且必须要求人工确认。

## 基础概念

|概念|说明|
|---|---|
|info-center|华为 VRP 的信息中心，用于日志、告警、调试信息管理|
|loghost|远程日志服务器，设备可以把日志发送到该服务器|
|logbuffer|本地日志缓冲区|
|trapbuffer|本地告警缓冲区|
|SNMP|Simple Network Management Protocol，用于网络设备监控|
|SNMP community|SNMP v1/v2c 使用的社区字符串，类似明文口令|
|SNMPv3|更安全的 SNMP 版本，支持认证和加密|
|Trap|设备主动向网管系统上报的告警信息|
|NMS|Network Management System，网管系统|
|MIB|管理信息库，定义可被监控的对象|

## 日志级别说明

|级别|名称|说明|
|---|---|---|
|0|Emergency|紧急，系统不可用|
|1|Alert|需要立即处理|
|2|Critical|严重错误|
|3|Error|错误|
|4|Warning|警告|
|5|Notification|通知|
|6|Informational|信息|
|7|Debugging|调试信息|

说明：

- 数值越小，日志越严重。
    
- 普通监控场景不建议长期打开大量 debug 日志。
    
- 日志级别过低可能产生大量日志，日志级别过高可能漏掉有用信息。
    

## 常用命令

### 启用信息中心

```text
info-center enable
```

说明：

- 用于启用日志信息中心。
    
- 如果设备默认已启用，应避免重复生成不必要配置。
    

### 配置远程日志服务器

```text
info-center loghost <loghost-ip>
```

示例：

```text
info-center loghost 192.168.1.100
```

说明：

- `<loghost-ip>` 是日志服务器 IP 地址。
    
- 设备到日志服务器之间需要路由可达。
    
- 如果有 ACL 或安全策略，需要允许日志流量通过。
    

### 指定日志源接口

```text
info-center loghost source <interface-type> <interface-number>
```

示例：

```text
info-center loghost source Vlanif10
```

说明：

- 源接口用于指定设备发送日志时使用的源地址。
    
- 推荐使用稳定的管理接口或管理 VLANIF。
    
- 源接口地址应能被日志服务器访问。
    

### 配置日志输出过滤

```text
info-center source <module> channel <channel> log level <level>
```

示例：

```text
info-center source default channel loghost log level informational
```

说明：

- 用于控制哪些模块、哪些级别的日志输出到指定通道。
    
- 生产环境应根据日志平台能力和排障需求设置合理级别。
    
- 不建议长期把大量 debug 级别日志发送到远程服务器。
    

### 查看日志和告警

```text
display logbuffer
display trapbuffer
display alarm active
display info-center
```

说明：

- `display logbuffer`：查看本地日志缓冲区。
    
- `display trapbuffer`：查看告警缓冲区。
    
- `display alarm active`：查看当前活动告警。
    
- `display info-center`：查看信息中心配置。
    

### 清空日志缓冲区

```text
reset logbuffer
```

说明：

- 清空日志缓冲区后，本地历史日志不可恢复。
    
- 该命令不应作为普通回滚命令。
    
- 只有在人工确认后才建议执行。
    

## SNMP 版本建议

|版本|安全性|建议|
|---|---|---|
|SNMPv1|低|不建议默认使用|
|SNMPv2c|中低|可用于实验环境，但 community 类似明文口令|
|SNMPv3|较高|推荐用于真实环境，支持认证和加密|

说明：

- 不要在知识库中使用 `public`、`private` 作为推荐社区名。
    
- 不要在知识库中写真实 community、密码或密钥。
    
- SNMP 监控流量应通过 ACL 或管理网段限制来源。
    

## SNMP 常用命令

### 启用 SNMP Agent

```text
snmp-agent
```

说明：

- 启用设备 SNMP Agent。
    
- 只有用户或规划明确要求网管监控时才生成。
    

### 配置 SNMPv2c 只读社区

```text
snmp-agent sys-info version v2c
snmp-agent community read <READ_COMMUNITY_PLACEHOLDER>
```

说明：

- `<READ_COMMUNITY_PLACEHOLDER>` 是占位符，不应写真实 community。
    
- SNMPv2c 不应使用 `public` / `private` 作为默认值。
    
- 如果是生产环境，优先考虑 SNMPv3。
    

### 配置 SNMPv2c 社区绑定 ACL

```text
snmp-agent community read <READ_COMMUNITY_PLACEHOLDER> acl <acl-number>
```

说明：

- 建议限制允许访问 SNMP 的网管服务器地址。
    
- ACL 编号应来自安全策略或管理面规划。
    
- 如果没有 ACL 限制，SNMP 可能暴露给不应访问的网段。
    

### 配置 SNMP Trap

```text
snmp-agent trap enable
snmp-agent target-host trap address udp-domain <nms-ip> udp-port 162 params securityname <security-name>
```

说明：

- Trap 用于设备主动向网管系统上报告警。
    
- `<nms-ip>` 是网管服务器地址。
    
- `<security-name>` 不应使用默认公开字符串。
    
- 设备到网管服务器之间需要路由可达。
    

### SNMPv3 基础配置

```text
snmp-agent sys-info version v3
snmp-agent group v3 <group-name> privacy
snmp-agent usm-user v3 <user-name> <group-name> authentication-mode sha <AUTH_PASSWORD_PLACEHOLDER> privacy-mode aes128 <PRIV_PASSWORD_PLACEHOLDER>
```

说明：

- SNMPv3 推荐用于真实环境。
    
- `<AUTH_PASSWORD_PLACEHOLDER>` 和 `<PRIV_PASSWORD_PLACEHOLDER>` 是占位符，不能写真实密码。
    
- 不同设备版本对认证算法、加密算法和命令格式可能存在差异，真实落地前应按设备版本确认。
    

### 查看 SNMP 状态

```text
display snmp-agent status
display snmp-agent sys-info
display snmp-agent community
display snmp-agent trap all
```

说明：

- `display snmp-agent status`：查看 SNMP Agent 状态。
    
- `display snmp-agent sys-info`：查看 SNMP 系统信息。
    
- `display snmp-agent community`：查看 SNMP community 配置。
    
- `display snmp-agent trap all`：查看 Trap 配置。
    

## 设备状态监控命令

```text
display version
display device
display power
display fan
display temperature
display cpu-usage
display memory-usage
display interface brief
display ip routing-table
```

说明：

- 这些命令只用于查看设备状态。
    
- 查询命令不应混入正式配置命令块。
    
- 可以作为验证命令、排障建议或运维检查建议。
    

## 参数解释

|参数|说明|
|---|---|
|loghost-ip|日志服务器 IP 地址，例如 `192.168.1.100`|
|module|日志来源模块，例如 `default`|
|channel|日志通道，例如 `loghost`|
|level|日志级别，例如 `informational`、`warning`、`error`|
|interface-type|源接口类型，例如 `Vlanif`|
|interface-number|源接口编号，例如 `10`|
|nms-ip|网管服务器 IP 地址|
|acl-number|用于限制 SNMP 访问来源的 ACL 编号|
|READ_COMMUNITY_PLACEHOLDER|SNMPv2c 只读 community 占位符|
|group-name|SNMPv3 组名|
|user-name|SNMPv3 用户名|
|AUTH_PASSWORD_PLACEHOLDER|SNMPv3 认证密码占位符|
|PRIV_PASSWORD_PLACEHOLDER|SNMPv3 加密密码占位符|

## 配置示例

### 示例 1：配置日志发送到服务器

场景：

- 日志服务器：`192.168.1.100`
    
- 日志级别：`informational`
    
- 日志源接口：`Vlanif10`
    

```text
<Huawei> system-view
[Huawei] info-center enable
[Huawei] info-center loghost 192.168.1.100
[Huawei] info-center loghost source Vlanif10
[Huawei] info-center source default channel loghost log level informational
[Huawei] return
<Huawei> display info-center
<Huawei> display logbuffer
```

适用场景：

- 需要把设备日志统一发送到日志平台。
    
- 需要集中审计和排障。
    

注意：

- 日志服务器 IP 应路由可达。
    
- 管理 VLAN、ACL 或安全策略应允许日志上报流量。
    

### 示例 2：配置 SNMPv2c 只读监控

场景：

- 网管服务器：`192.168.1.100`
    
- SNMP 版本：`v2c`
    
- 只读 community：使用占位符
    
- 限制来源 ACL：`2001`
    

```text
<Huawei> system-view
[Huawei] snmp-agent
[Huawei] snmp-agent sys-info version v2c
[Huawei] snmp-agent community read <READ_COMMUNITY_PLACEHOLDER> acl 2001
[Huawei] snmp-agent trap enable
[Huawei] snmp-agent target-host trap address udp-domain 192.168.1.100 udp-port 162 params securityname <READ_COMMUNITY_PLACEHOLDER>
[Huawei] return
<Huawei> display snmp-agent status
<Huawei> display snmp-agent community
<Huawei> display snmp-agent trap all
```

适用场景：

- 实验环境或兼容旧网管系统。
    
- 只读监控，不允许写操作。
    

注意：

- 不要使用 `public` 或 `private`。
    
- 不建议默认生成 write community。
    
- 如果是生产环境，优先使用 SNMPv3。
    
- ACL `2001` 应限制只允许网管服务器访问。
    

### 示例 3：配置 SNMPv3 监控

场景：

- SNMP 版本：`v3`
    
- 用户：`nmsuser`
    
- 组：`nmsgroup`
    
- 密码使用占位符
    

```text
<Huawei> system-view
[Huawei] snmp-agent
[Huawei] snmp-agent sys-info version v3
[Huawei] snmp-agent group v3 nmsgroup privacy
[Huawei] snmp-agent usm-user v3 nmsuser nmsgroup authentication-mode sha <AUTH_PASSWORD_PLACEHOLDER> privacy-mode aes128 <PRIV_PASSWORD_PLACEHOLDER>
[Huawei] return
<Huawei> display snmp-agent status
<Huawei> display snmp-agent sys-info
```

适用场景：

- 真实环境或安全要求较高的监控场景。
    
- 需要认证和加密的 SNMP 访问。
    

注意：

- 不同设备版本的 SNMPv3 认证和加密命令可能略有差异。
    
- 真实落地前应按设备版本确认命令格式。
    
- 不要在知识库中写真实密码。
    

### 示例 4：基础监控检查

```text
<Huawei> display version
<Huawei> display device
<Huawei> display power
<Huawei> display fan
<Huawei> display temperature
<Huawei> display cpu-usage
<Huawei> display memory-usage
<Huawei> display alarm active
<Huawei> display logbuffer
<Huawei> display interface brief
<Huawei> display ip routing-table
```

适用场景：

- 配置变更前后检查。
    
- 执行失败后的基础排障。
    
- 设备运行状态监控。
    

## 验证命令

```text
display info-center
display logbuffer
display trapbuffer
display alarm active
display snmp-agent status
display snmp-agent sys-info
display snmp-agent community
display snmp-agent trap all
display version
display device
display power
display fan
display temperature
display cpu-usage
display memory-usage
```

验证关注点：

- 信息中心是否启用。
    
- 日志服务器是否配置正确。
    
- 日志源接口是否符合管理面规划。
    
- 本地日志是否有异常。
    
- SNMP Agent 是否启用。
    
- SNMP 版本是否符合安全要求。
    
- SNMP community 是否为占位符或由用户提供，不应为 `public/private`。
    
- SNMP Trap 目标服务器是否正确。
    
- 设备电源、风扇、温度、CPU、内存是否异常。
    

## 回滚 / undo 命令

### 删除日志服务器

```text
system-view
undo info-center loghost 192.168.1.100
return
```

### 关闭信息中心

```text
system-view
undo info-center enable
return
```

说明：

- 关闭信息中心可能导致日志和告警不再记录或上报。
    
- 如果只是更换日志服务器，优先删除旧 loghost 并配置新 loghost，不要直接关闭信息中心。
    

### 删除 SNMP community

```text
system-view
undo snmp-agent community read <READ_COMMUNITY_PLACEHOLDER>
return
```

### 关闭 SNMP Agent

```text
system-view
undo snmp-agent
return
```

说明：

- 关闭 SNMP 会导致网管系统无法继续监控设备。
    
- 如果只是替换 community 或 NMS，应优先修改对应配置，不要直接关闭 SNMP。
    

### 清空日志缓冲区

```text
reset logbuffer
```

说明：

- `reset logbuffer` 会清空本地日志缓冲区。
    
- 清空后本地历史日志不可恢复。
    
- 不应作为普通回滚命令。
    
- 必须人工确认后才能执行。
    

## 高风险与注意事项

1. 不要在知识库中写真实 SNMP community、认证密码、加密密码或公网管理地址。
    
2. 不要使用 `public`、`private` 作为推荐 community。
    
3. 不建议默认生成 SNMP write community。
    
4. 真实环境优先使用 SNMPv3。
    
5. SNMPv1 / SNMPv2c 安全性较弱，使用时应限制来源 ACL。
    
6. 配置日志服务器或 SNMP 服务器前，应确认设备到服务器路由可达。
    
7. ACL 或安全策略可能阻断日志、SNMP 或 Trap 流量。
    
8. `reset logbuffer` 会清空本地日志，不可作为普通回滚。
    
9. 设备升级、配置恢复、重启和清空配置不属于本模块默认配置范围。
    
10. 查询命令 `display ...` 不应混入正式配置块，可以作为验证命令或排障建议。
    
11. 修改 SNMP 配置可能影响网管系统监控，应在变更窗口内操作。
    

## ConfigurationAgent 使用规则

ConfigurationAgent 使用本模块时应遵守：

1. 只有用户或规划明确要求日志、SNMP、监控能力时，才生成本模块配置块。
    
2. 日志配置、SNMP 配置和普通业务配置应分成独立 `CommandBlock`。
    
3. SNMP community、认证密码、加密密码必须使用占位符，不能生成真实敏感值。
    
4. 默认只生成 SNMP read 能力，不默认生成 write community。
    
5. 真实环境优先建议 SNMPv3；兼容性场景可使用 SNMPv2c，但必须提示安全风险。
    
6. `display ...`、`ping`、`tracert` 等查询命令应作为验证建议，不应混入正式配置命令。
    
7. `startup system-software`、`load configuration`、`reset saved-configuration`、`reboot` 不应由本模块生成。
    
8. 清空日志、关闭 SNMP、关闭 info-center 等操作应标记为高风险。
    
9. 如果 SNMP 需要绑定 ACL，应引用 ACL 模块或安全策略规划结果。
    

## 常见错误

1. 日志服务器地址错误或路由不可达，导致远程日志无法接收。
    
2. 日志级别设置过高，导致重要日志没有上报。
    
3. 长期开启大量 debug 级别日志，导致日志量过大。
    
4. SNMP community 使用 `public/private`，存在安全风险。
    
5. 配置 SNMP write community，导致设备被非预期修改的风险增加。
    
6. SNMPv2c 没有限制来源 ACL，导致监控面暴露。
    
7. SNMP Trap 服务器地址或端口错误，导致告警无法上报。
    
8. 设备到 NMS 的路由、ACL 或安全策略不通，导致 SNMP 查询失败。
    
9. 将系统升级命令和 SNMP / 日志配置混在一个配置块中。
    
10. 把 `reset logbuffer` 当作普通回滚命令。
    
11. 把查询命令混入正式配置命令。