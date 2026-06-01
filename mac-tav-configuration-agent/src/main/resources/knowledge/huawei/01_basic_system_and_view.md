---
id: huawei-basic-system-view  
title: Huawei Basic System And View Commands  
vendor: HUAWEI  
platform: VRP  
feature: BASIC_SYSTEM_VIEW  
tags: [basic, system-view, view, save, display, undo, vrp]  
knowledgeType: COMMAND_REFERENCE  
generationSourceType: RAG  
riskLevel: LOW  
status: READY  
version: 1.0.0  
updatedAt: 2026-06-02

---
# 系统基础与设备视图

## 这个模块解决什么问题

本模块用于说明华为 VRP 设备的基础命令、常见命令视图、设备名称配置、系统时间配置、配置查看和配置保存方式。

这些命令是后续接口、VLAN、路由、ACL、NAT 等配置的基础。ConfigurationAgent 在生成具体配置块时，应根据命令所属视图组织命令顺序，避免把系统视图命令、接口视图命令和用户视图命令混用。

## 视图说明

华为 VRP 常见视图包括：

|视图|提示符示例|说明|
|---|---|---|
|用户视图|`<Huawei>`|登录设备后的默认视图，常用于查看状态、保存配置、重启设备等|
|系统视图|`[Huawei]`|全局配置视图，进入后可以配置设备名称、进入接口视图、创建 VLAN、配置路由协议等|
|接口视图|`[Huawei-GigabitEthernet0/0/1]`|用于配置具体接口，例如接口描述、IP 地址、access/trunk 类型等|
|协议视图|`[Huawei-ospf-1]`|用于配置 OSPF 等协议|
|ACL 视图|`[Huawei-acl-adv-3000]`|用于配置 ACL 规则|

常见视图切换命令：

```text
system-view
quit
return
```

说明：

- `system-view`：从用户视图进入系统视图。
    
- `quit`：退出当前视图，返回上一级视图。
    
- `return`：从任意配置视图直接返回用户视图。
    

## 常用命令

### 进入和退出视图

```text
system-view
quit
return
```

### 设置设备名称

```text
sysname <hostname>
```

示例：

```text
<Huawei> system-view
[Huawei] sysname MySwitch
[MySwitch]
```

### 查看设备信息

```text
display version
display current-configuration
display saved-configuration
display this
display clock
```

说明：

- `display version`：查看设备版本、启动信息等。
    
- `display current-configuration`：查看当前运行配置。
    
- `display saved-configuration`：查看已保存配置。
    
- `display this`：查看当前视图下的配置。
    
- `display clock`：查看系统时间。
    

### 设置系统时间

```text
clock datetime <HH:MM:SS> <YYYY-MM-DD>
```

示例：

```text
<Huawei> system-view
[Huawei] clock datetime 14:30:00 2024-06-01
```

说明：

- 系统时间配置会影响日志、审计和故障排查。
    
- 真实生产环境中通常应优先使用 NTP，而不是手动长期维护时间。
    

### 保存配置

```text
save
```

推荐示例：

```text
<MySwitch> save
```

完整示例：

```text
<Huawei> system-view
[Huawei] sysname MySwitch
[MySwitch] clock datetime 14:30:00 2024-06-01
[MySwitch] return
<MySwitch> save
```

说明：

- 配置修改后，如果没有执行 `save`，设备重启后配置可能丢失。
    
- `save` 属于高影响操作，真实设备环境中应经过人工确认。
    

## 参数解释

|参数|说明|
|---|---|
|hostname|设备名称，建议使用能表达角色的名称，例如 `R1`、`CoreSwitch`、`Office-GW`|
|HH:MM:SS|时间，格式示例：`14:30:00`|
|YYYY-MM-DD|日期，格式示例：`2024-06-01`|

## 配置示例

### 示例 1：设置设备名称

```text
<Huawei> system-view
[Huawei] sysname Office-GW
[Office-GW] return
<Office-GW> save
```

### 示例 2：查看当前配置和已保存配置

```text
<Office-GW> display current-configuration
<Office-GW> display saved-configuration
```

### 示例 3：查看当前视图配置

```text
<Office-GW> system-view
[Office-GW] display this
```

## 验证命令

```text
display version
display current-configuration
display saved-configuration
display this
display clock
```

验证关注点：

- 设备名称是否已经变更。
    
- 当前运行配置中是否存在目标配置。
    
- 已保存配置中是否存在目标配置。
    
- 当前系统时间是否符合预期。
    
- 当前命令是否在正确视图下执行。
    

## 回滚 / undo 命令

### 回滚设备名称

```text
system-view
undo sysname
return
save
```

说明：

- `undo sysname` 用于恢复默认设备名称。
    
- 修改系统时间没有通用的自动回滚命令，如果需要恢复，只能重新设置为原时间或改用 NTP。
    
- `save` 本身不是配置项，不能用 `undo save` 回滚。
    
- `reboot` 不是回滚命令，不应作为 ConfigurationAgent 默认生成的 rollbackCommands。
    

## 高风险命令说明

### reboot

```text
reboot
```

说明：

- `reboot` 用于重启设备。
    
- 它不是回滚命令。
    
- 在真实设备环境中属于高风险运维动作，必须人工确认。
    
- ConfigurationAgent 不应默认把 `reboot` 放入普通配置块或回滚配置块。
    

## 常见错误

1. 修改配置后忘记执行 `save`，设备重启后配置丢失。
    
2. 不理解 `<Huawei>` 和 `[Huawei]` 的区别，把配置命令写在错误视图下。
    
3. 忘记使用 `quit` 或 `return`，导致后续命令进入了错误视图。
    
4. 把 `reboot` 当成回滚命令。
    
5. 把查询命令 `display ...` 和配置命令混在同一个配置块中。
    
6. 修改系统时间时参数顺序写反，正确顺序应为先时间、后日期。
