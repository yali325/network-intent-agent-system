---
id: huawei-management-monitoring
title: Huawei Management And Monitoring Commands
vendor: HUAWEI  
platform: VRP  
feature: MANAGEMENT_MONITORING
tags: [aaa, ssh, vty, ntp, display, monitoring, management]
knowledgeType: COMMAND_REFERENCE
generationSourceType: RAG  
riskLevel: LOW  
status: DISABLED  
version: 0.1.0  
updatedAt: 2026-06-02

---
# 管理面与基础监控

## 这个模块解决什么问题

本模块用于说明华为 VRP 设备中的基础管理面配置和常用监控查看命令，包括本地用户、AAA、VTY 远程登录、SSH 登录限制、NTP 时间同步、设备状态查看和接口/路由/日志等基础监控命令。

本模块主要服务以下场景：

1. 为设备配置基础管理用户。
    
2. 限制远程登录协议，优先使用 SSH。
    
3. 配置 NTP 时间同步，保证日志和审计时间准确。
    
4. 查看设备运行状态、接口状态、路由状态和基础日志。
    
5. 为 ConfigurationAgent 生成管理面相关的独立配置块或验证建议。
    

## 本模块不负责什么

本模块不负责业务转发配置，例如 VLAN、接口 IP、路由、ACL、NAT。这些配置应由对应模块生成。

本模块不负责配置备份、恢复、系统升级、文件删除和清空配置。以下命令不应放入本模块的普通配置块：

```text
reset saved-configuration
startup saved-configuration
startup system-software
load configuration
delete /unreserved
reset recycle-bin
reboot
```

这些内容应放在维护与备份模块，并且需要人工确认。

## 管理面基础概念

|概念|说明|
|---|---|
|AAA|Authentication、Authorization、Accounting，认证、授权、计费|
|local-user|本地用户，用于设备登录认证|
|privilege level|用户权限级别，级别越高权限越大|
|service-type|用户允许使用的服务类型，例如 SSH|
|VTY|Virtual Teletype，虚拟终端线路，用于远程登录|
|SSH|安全远程登录方式，优先推荐|
|Telnet|明文远程登录方式，不建议默认启用|
|NTP|网络时间协议，用于同步设备时间|
|display|查看类命令，不修改设备配置|

## 常用命令

### 进入 AAA 视图

```text
aaa
```

说明：

- AAA 视图用于配置本地用户、认证方案、授权方案等。
    
- 基础管理场景中，通常使用本地用户 + VTY + SSH 登录。
    

### 创建本地用户

```text
local-user <username> password irreversible-cipher <password>
local-user <username> privilege level <level>
local-user <username> service-type ssh
```

示例：

```text
aaa
local-user netadmin password irreversible-cipher <PASSWORD_PLACEHOLDER>
local-user netadmin privilege level 15
local-user netadmin service-type ssh
quit
```

说明：

- `<PASSWORD_PLACEHOLDER>` 是占位符，真实密码不应写入知识库、日志或代码仓库。
    
- 推荐默认只启用 SSH，不默认启用 Telnet。
    
- 权限级别应按最小权限原则设置，不应无脑使用 15 级。
    
- 如果只是生成业务网络配置，ConfigurationAgent 不应默认创建管理用户，除非用户明确要求配置管理面。
    

### 配置 VTY 登录认证

```text
user-interface vty 0 4
authentication-mode aaa
protocol inbound ssh
```

示例：

```text
user-interface vty 0 4
authentication-mode aaa
protocol inbound ssh
quit
```

说明：

- `authentication-mode aaa` 表示 VTY 登录使用 AAA 认证。
    
- `protocol inbound ssh` 表示只允许 SSH 登录。
    
- 不建议默认配置 `protocol inbound telnet`。
    
- 如果用户明确要求 Telnet，应生成高风险提示。
    

### 设置时区

```text
clock timezone <zone-name> add <HH:MM:SS>
```

示例：

```text
clock timezone BJ add 08:00:00
```

说明：

- 中国常用东八区，可使用 `add 08:00:00`。
    
- 时区错误会导致日志、告警和审计时间不准确。
    

### 手动设置系统时间

```text
clock datetime <HH:MM:SS> <YYYY-MM-DD>
```

示例：

```text
clock datetime 14:30:00 2026-06-01
```

说明：

- 手动设置时间适合临时实验或没有 NTP 的环境。
    
- 生产环境建议使用 NTP 自动同步。
    
- 系统时间配置会影响日志、审计和故障排查。
    

### 配置 NTP 时间同步

```text
ntp-service enable
ntp-service unicast-server <ntp-server-ip>
```

示例：

```text
ntp-service enable
ntp-service unicast-server 192.168.1.100
```

说明：

- `ntp-service unicast-server` 应写 IP 地址或设备支持的合法服务器地址，不要写 Markdown 链接。
    
- 如果 NTP 服务器不可达，设备时间无法同步。
    
- 如果网络存在 ACL 或安全策略，需要允许设备访问 NTP 服务器。
    
- `display ntp-service status` 可查看本地系统时钟同步状态；华为文档说明该命令可用于了解本地时钟同步状态和 stratum。
    

### 查看 NTP 状态

```text
display clock
display ntp-service status
display ntp sessions
```

说明：

- `display clock`：查看当前系统时间。
    
- `display ntp-service status`：查看 NTP 服务状态。
    
- `display ntp sessions`：查看 NTP 会话信息。
    

### 基础设备监控命令

```text
display version
display device
display health
display cpu-usage
display memory-usage
display alarm active
display logbuffer
```

说明：

- `display version`：查看设备版本和启动信息。
    
- `display device`：查看设备硬件状态。
    
- `display health`：查看设备健康状态。
    
- `display cpu-usage`：查看 CPU 使用率。
    
- `display memory-usage`：查看内存使用率。
    
- `display alarm active`：查看当前活动告警。
    
- `display logbuffer`：查看日志缓冲区。
    

### 网络状态监控命令

```text
display interface brief
display interface <interface-name>
display ip interface brief
display ip routing-table
display arp
ping <destination-ip>
tracert <destination-ip>
```

说明：

- 这些命令用于排查接口、IP、路由、ARP 和连通性问题。
    
- 查询命令不应混入正式配置块。
    
- 可以作为验证命令或排障建议输出。
    

## 参数解释

|参数|说明|
|---|---|
|username|本地用户名，例如 `netadmin`|
|password|用户密码，应使用占位符，不应写真实密码|
|privilege level|用户权限级别，常见范围 `0-15`，数值越大权限越高|
|service-type|用户允许的服务类型，基础管理面推荐 `ssh`|
|vty 0 4|VTY 线路范围，表示 0 到 4 号远程登录线路|
|zone-name|时区名称，例如 `BJ`|
|HH:MM:SS|时间，例如 `14:30:00` 或时区偏移 `08:00:00`|
|YYYY-MM-DD|日期，例如 `2026-06-01`|
|ntp-server-ip|NTP 服务器 IP 地址，例如 `192.168.1.100`|
|interface-name|接口名称，例如 `GigabitEthernet0/0/1`|
|destination-ip|目标 IP 地址，用于 ping 或 tracert|

## 权限级别说明

|级别|说明|
|---|---|
|0|低权限，通常只能执行有限查看类命令|
|1|监控和诊断类权限|
|2|可执行部分配置命令|
|3-14|可按设备和策略自定义|
|15|最高权限，通常可执行所有命令|

说明：

- 不建议默认给所有用户 15 级权限。
    
- 如果只是普通运维查看账号，应使用较低权限。
    
- 如果用户明确要求管理员账号，才考虑配置较高权限，并提示风险。
    

## 配置示例

### 示例 1：配置 SSH 本地用户登录

场景：

- 创建本地用户 `netadmin`。
    
- 只允许 SSH 登录。
    
- VTY 使用 AAA 认证。
    
- 密码使用占位符，不写真实密码。
    

```text
<Huawei> system-view
[Huawei] aaa
[Huawei-aaa] local-user netadmin password irreversible-cipher <PASSWORD_PLACEHOLDER>
[Huawei-aaa] local-user netadmin privilege level 15
[Huawei-aaa] local-user netadmin service-type ssh
[Huawei-aaa] quit
[Huawei] user-interface vty 0 4
[Huawei-ui-vty0-4] authentication-mode aaa
[Huawei-ui-vty0-4] protocol inbound ssh
[Huawei-ui-vty0-4] quit
[Huawei] return
<Huawei> display local-user
<Huawei> display user-interface
```

适用场景：

- 用户明确要求配置设备远程管理。
    
- 实验环境需要基础 SSH 管理入口。
    

注意：

- 不要把真实密码写入知识库。
    
- 不建议默认启用 Telnet。
    
- 该配置属于管理面配置，不是普通业务转发配置。
    

### 示例 2：配置 NTP 时间同步

场景：

- 时区：东八区。
    
- NTP 服务器：`192.168.1.100`。
    

```text
<Huawei> system-view
[Huawei] clock timezone BJ add 08:00:00
[Huawei] ntp-service enable
[Huawei] ntp-service unicast-server 192.168.1.100
[Huawei] return
<Huawei> display clock
<Huawei> display ntp-service status
<Huawei> display ntp sessions
```

适用场景：

- 需要日志和审计时间准确。
    
- 多设备统一时间。
    
- 故障排查需要准确时间线。
    

### 示例 3：基础监控检查

```text
<Huawei> display version
<Huawei> display device
<Huawei> display cpu-usage
<Huawei> display memory-usage
<Huawei> display alarm active
<Huawei> display logbuffer
<Huawei> display interface brief
<Huawei> display ip routing-table
```

适用场景：

- 设备运行状态检查。
    
- 配置变更前后检查。
    
- 执行失败后的基础排障。
    

## 验证命令

```text
display local-user
display user-interface
display clock
display ntp-service status
display ntp sessions
display version
display device
display cpu-usage
display memory-usage
display alarm active
display logbuffer
display interface brief
display ip routing-table
```

验证关注点：

- 本地用户是否存在。
    
- 用户 service-type 是否符合预期。
    
- VTY 是否使用 AAA 认证。
    
- VTY 是否只允许 SSH。
    
- 系统时间和时区是否正确。
    
- NTP 是否同步成功。
    
- CPU、内存、告警、日志是否存在异常。
    
- 接口和路由基础状态是否正常。
    

## 回滚 / undo 命令

### 删除本地用户

```text
system-view
aaa
undo local-user netadmin
quit
return
```

说明：

- 删除管理用户前，应确认还有其他可用管理入口。
    
- 否则可能导致无法远程登录设备。
    

### 取消 VTY SSH 限制或认证配置

```text
system-view
user-interface vty 0 4
undo protocol inbound
undo authentication-mode
quit
return
```

说明：

- 取消认证配置可能降低管理面安全性。
    
- 取消协议限制可能导致不安全协议被允许。
    
- 真实环境应谨慎操作。
    

### 删除 NTP 服务器

```text
system-view
undo ntp-service unicast-server 192.168.1.100
return
```

### 关闭 NTP 服务

```text
system-view
undo ntp-service enable
return
```

说明：

- 关闭 NTP 可能导致设备时间漂移，影响日志和审计。
    
- 如果只需要更换 NTP 服务器，优先删除旧服务器并配置新服务器，不要直接关闭 NTP。
    

## 高风险与注意事项

1. 不要在知识库中写真实密码、真实密钥或真实公网管理地址。
    
2. 不建议默认启用 Telnet，管理面应优先使用 SSH。
    
3. 给用户配置 15 级权限属于高权限操作，应提示风险。
    
4. 删除本地用户前，应确认不会导致设备失去管理入口。
    
5. 取消 VTY 认证可能导致管理面暴露风险。
    
6. `reset saved-configuration`、`delete /unreserved`、`reset recycle-bin`、`reboot` 不属于普通管理面配置，不应放入本模块默认配置块。
    
7. NTP 服务器不可达时，时间同步不会成功，应检查路由、ACL 和服务器状态。
    
8. 查询命令 `display ...`、`ping`、`tracert` 不应混入正式配置块，可以作为验证命令或排障建议。
    
9. 如果用户没有明确提出管理面配置需求，ConfigurationAgent 不应默认创建用户或修改 VTY。
    

## ConfigurationAgent 使用规则

ConfigurationAgent 使用本模块时应遵守：

1. 只有用户或规划明确要求管理面配置时，才生成本地用户、VTY、SSH、NTP 等配置块。
    
2. 生成用户配置时，密码必须使用占位符，例如 `<PASSWORD_PLACEHOLDER>`，不得生成真实密码。
    
3. 默认优先 SSH，不默认生成 Telnet。
    
4. 管理面配置应与业务转发配置分开成独立 `CommandBlock`。
    
5. 监控查看命令应放入验证建议或运维建议，不应混入 `commands` 的正式配置块。
    
6. 涉及删除用户、关闭认证、降低安全性、关闭 NTP 的操作应标记为高风险。
    
7. 文件管理、配置恢复和系统升级应交给维护与备份模块，不应由本模块默认生成。
    

## 常见错误

1. 配置用户后忘记配置 VTY 认证方式，导致无法通过远程登录生效。
    
2. 用户配置了 SSH service-type，但 VTY 没有限制或允许 SSH。
    
3. VTY 只允许 SSH，但设备 SSH 服务或密钥配置没有准备好，导致登录失败。
    
4. 用户权限级别过低，登录后无法执行需要的查看或配置命令。
    
5. 给普通运维用户配置过高权限，增加误操作风险。
    
6. 使用 Telnet 进行远程管理，导致账号密码可能被明文传输。
    
7. NTP 服务器地址错误或路由不通，导致时间无法同步。
    
8. 时区未设置，日志显示时间和本地时间不一致。
    
9. 把文件删除、清空配置、系统升级命令混入管理面配置。
    
10. 把监控查询命令当成正式配置命令下发。
