id: huawei-maintenance-backup  
title: Huawei Maintenance And Backup Commands  
vendor: HUAWEI  
platform: VRP  
feature: MAINTENANCE_BACKUP  
tags: [save, backup, current-configuration, saved-configuration, startup, rollback]  
knowledgeType: COMMAND_REFERENCE  
generationSourceType: RAG  
riskLevel: HIGH  
status: READY  
version: 1.0.0  
updatedAt: 2026-06-02

---
# 维护与备份

## 这个模块解决什么问题

本模块用于说明华为 VRP 设备中的配置查看、配置保存、配置备份、配置文件管理和维护类高风险命令边界。

本模块主要服务以下场景：

1. 在生成配置后提醒用户保存配置。
    
2. 在执行高风险变更前建议备份当前配置。
    
3. 在排障时查看当前配置、已保存配置和启动配置。
    
4. 在人工确认后执行配置恢复、配置清理或系统升级相关操作。
    

ConfigurationAgent 可以引用本模块生成维护建议、验证命令、备份提醒和人工确认提示，但不应默认把高风险维护命令混入普通网络配置块。

## 本模块不负责什么

本模块不负责 VLAN、接口 IP、路由、ACL、NAT 等业务网络配置。这些应由对应模块生成。

本模块中的以下命令不应由 ConfigurationAgent 默认生成并自动执行：

```text
load configuration
reset saved-configuration
startup system-software
reboot
reboot fast
shutdown
delete /unreserved
reset recycle-bin
```

这些命令属于高风险维护操作，应要求人工确认。

## 配置文件基础概念

|配置类型|常用查看命令|说明|
|---|---|---|
|当前运行配置|`display current-configuration`|当前正在设备内存中生效的配置|
|已保存配置|`display saved-configuration`|已保存到设备存储中的配置，重启后通常会加载|
|启动配置|`display startup`|设备下次启动时使用的配置文件和系统文件|
|当前视图配置|`display this`|查看当前视图下的配置片段|

说明：

- 当前运行配置不一定已经保存。
    
- 修改配置后，如果没有执行 `save`，设备重启后配置可能丢失。
    
- 启动配置决定设备重启后加载哪个配置文件。
    
- 在真实设备上变更启动配置、恢复配置和重启设备前必须人工确认。
    

## 常用命令

### 查看配置

```text
display current-configuration
display saved-configuration
display startup
display this
display version
```

说明：

- `display current-configuration`：查看当前运行配置。
    
- `display saved-configuration`：查看已保存配置。
    
- `display startup`：查看启动配置文件和系统软件文件。
    
- `display this`：查看当前视图下的配置。
    
- `display version`：查看设备版本和运行信息。
    

### 保存当前配置

```text
save
save <configuration-file>
```

示例：

```text
<Huawei> save
<Huawei> save backup-2026-06-01.cfg
```

说明：

- `save` 会保存当前运行配置。
    
- `save <configuration-file>` 可以把当前配置保存到指定文件。
    
- 在真实设备环境中，保存配置会影响设备重启后的配置状态，应提示用户确认。
    

### 查看文件系统

```text
dir
dir /all
pwd
cd <directory>
more <filename>
```

说明：

- `dir`：查看当前目录文件。
    
- `dir /all`：查看所有文件，包括回收站文件。
    
- `pwd`：查看当前路径。
    
- `cd`：切换目录。
    
- `more`：查看文件内容。
    

### 复制和移动文件

```text
copy <source> <destination>
move <source> <destination>
```

示例：

```text
<Huawei> copy backup-2026-06-01.cfg flash:/backup-2026-06-01.cfg
```

说明：

- 复制配置文件适合做本地备份。
    
- 移动配置文件前应确认目标路径和文件名，避免覆盖重要文件。
    

### 删除和恢复文件

```text
delete <filename>
undelete <filename>
delete /unreserved <filename>
reset recycle-bin
```

说明：

- `delete <filename>` 通常把文件放入回收站。
    
- `undelete <filename>` 可恢复误删文件。
    
- `delete /unreserved <filename>` 会彻底删除文件。
    
- `reset recycle-bin` 会清空回收站。
    
- 彻底删除文件和清空回收站属于高风险操作，不应默认生成。
    

### 指定启动配置文件

```text
startup saved-configuration <filename>
display startup
```

说明：

- `startup saved-configuration` 用于指定下次启动加载的配置文件。
    
- 这会影响设备重启后的配置状态。
    
- 只能在人工确认后作为维护操作执行。
    

### 配置恢复

```text
load configuration <configuration-file>
```

说明：

- `load configuration` 用于从指定配置文件加载配置。
    
- 该操作可能覆盖或改变当前运行配置。
    
- 不应作为 ConfigurationAgent 默认回滚命令。
    
- 如果需要恢复配置，应先比较配置、备份当前状态，并人工确认。
    

### 系统软件升级

```text
tftp <server-ip> get <remote-file> <local-file>
ftp <ftp-server-ip>
startup system-software <filename>
display startup
reboot
```

说明：

- 系统升级属于设备维护，不属于普通网络配置生成。
    
- `startup system-software` 会指定下次启动使用的系统软件。
    
- `reboot` 会重启设备并中断业务。
    
- ConfigurationAgent 不应默认生成系统升级配置块。
    

## 参数解释

|参数|说明|
|---|---|
|configuration-file|配置文件名称，例如 `vrpcfg.cfg`、`backup-2026-06-01.cfg`|
|filename|文件名，可以是配置文件、系统软件文件或日志文件|
|source|源文件路径|
|destination|目标文件路径|
|server-ip|FTP / TFTP 服务器地址|
|remote-file|远端服务器文件名|
|local-file|本地保存文件名|
|system-software|系统软件文件，例如 `vrp.bin`|

## 推荐维护流程

### 变更前备份流程

```text
display current-configuration
display saved-configuration
display startup
save backup-before-change.cfg
dir
```

说明：

- 变更前应查看当前配置和已保存配置。
    
- 变更前建议保存一个备份配置文件。
    
- 备份文件名建议包含日期和变更目的。
    

### 变更后检查流程

```text
display current-configuration
display saved-configuration
display startup
display this
```

说明：

- 检查当前运行配置是否符合预期。
    
- 检查配置是否已经保存。
    
- 检查下次启动配置是否符合预期。
    

### 配置保存流程

```text
display current-configuration
save
display saved-configuration
```

说明：

- 先查看当前配置，再保存。
    
- 保存后查看已保存配置，确认关键配置已经写入。
    

## 配置示例

### 示例 1：查看当前配置和启动配置

```text
<Huawei> display current-configuration
<Huawei> display saved-configuration
<Huawei> display startup
```

适用场景：

- 变更前检查。
    
- 变更后验证。
    
- 故障排查。
    

### 示例 2：保存当前配置

```text
<Huawei> save
<Huawei> display saved-configuration
```

适用场景：

- 配置已经确认正确。
    
- 用户确认需要保存到设备。
    
- 需要避免重启后配置丢失。
    

### 示例 3：变更前备份当前配置到文件

```text
<Huawei> display current-configuration
<Huawei> save backup-2026-06-01.cfg
<Huawei> dir
```

适用场景：

- 执行 ACL、路由、NAT 等重要变更前。
    
- 需要保留变更前状态。
    
- 后续人工恢复时可参考备份文件。
    

### 示例 4：复制备份文件

```text
<Huawei> dir
<Huawei> copy backup-2026-06-01.cfg flash:/backup-2026-06-01-copy.cfg
<Huawei> dir
```

适用场景：

- 本地复制备份文件。
    
- 防止原备份文件被误覆盖。
    

### 示例 5：人工确认后指定启动配置

```text
<Huawei> display startup
<Huawei> startup saved-configuration backup-2026-06-01.cfg
<Huawei> display startup
```

适用场景：

- 需要指定设备下次启动加载某个配置文件。
    
- 仅限人工确认后的维护操作。
    

注意：

- 该操作不会立即重启设备。
    
- 是否重启必须由人工确认。
    
- ConfigurationAgent 不应自动附带 `reboot`。
    

### 示例 6：人工确认后加载备份配置

```text
<Huawei> display current-configuration
<Huawei> dir
<Huawei> load configuration backup-2026-06-01.cfg
<Huawei> display current-configuration
```

适用场景：

- 明确需要恢复备份配置。
    
- 已经人工确认恢复风险。
    
- 已经确认备份文件正确。
    

注意：

- 加载配置可能改变当前运行配置。
    
- 是否保存加载后的配置应再次人工确认。
    
- 不建议自动执行 `save` 和 `reboot`。
    

## 验证命令

```text
display current-configuration
display saved-configuration
display startup
display this
display version
dir
dir /all
more <filename>
```

验证关注点：

- 当前运行配置是否符合预期。
    
- 关键配置是否已经保存。
    
- 下次启动配置文件是否正确。
    
- 备份文件是否存在。
    
- 文件大小和时间是否合理。
    
- 当前设备版本是否符合预期。
    
- 当前路径是否正确。
    

## 回滚 / 恢复命令

### 恢复误删文件

```text
undelete <filename>
```

说明：

- 仅适用于可恢复的删除场景。
    
- 如果使用了 `delete /unreserved`，通常无法通过该命令恢复。
    

### 人工确认后加载备份配置

```text
load configuration <configuration-file>
```

说明：

- 这是恢复配置操作，不是普通配置块回滚命令。
    
- 执行前应确认备份文件正确。
    
- 执行后应重新检查当前配置。
    
- 是否保存和重启必须单独人工确认。
    

### 人工确认后指定下次启动配置

```text
startup saved-configuration <configuration-file>
```

说明：

- 该命令影响设备下次启动加载的配置文件。
    
- 执行后应通过 `display startup` 验证。
    
- 不应自动附带 `reboot`。
    

## 高风险命令说明

### reset saved-configuration

```text
reset saved-configuration
```

说明：

- 清空已保存配置，可能导致设备重启后配置丢失。
    
- 不应作为普通回滚命令。
    
- 必须人工确认。
    

### startup system-software

```text
startup system-software <filename>
```

说明：

- 指定下次启动使用的系统软件。
    
- 属于系统升级操作，不属于普通网络配置。
    
- 必须人工确认。
    

### reboot

```text
reboot
reboot fast
```

说明：

- 重启设备会中断业务。
    
- `reboot fast` 更应谨慎使用。
    
- 不应由 ConfigurationAgent 默认生成。
    
- 如确需重启，应交给人工审批流程。
    

### shutdown

```text
shutdown
```

说明：

- 在系统维护语境下，`shutdown` 可能表示关闭系统或接口。
    
- 会造成服务中断。
    
- 不应自动生成。
    

### delete /unreserved 与 reset recycle-bin

```text
delete /unreserved <filename>
reset recycle-bin
```

说明：

- 彻底删除文件或清空回收站后，恢复难度很高。
    
- 不应默认生成。
    
- 必须人工确认。
    

## ConfigurationAgent 使用规则

ConfigurationAgent 使用本模块时应遵守：

1. 可以生成 `display current-configuration`、`display saved-configuration`、`display startup`、`dir` 等验证和检查建议。
    
2. 可以在配置完成后提示用户执行 `save`，但应标记为需要人工确认。
    
3. 可以在高风险配置前建议备份当前配置。
    
4. 不应默认生成 `reboot`、`reset saved-configuration`、`startup system-software`、`delete /unreserved`、`reset recycle-bin`。
    
5. 不应把系统升级命令和业务网络配置混在一个 `CommandBlock` 中。
    
6. 不应把配置恢复命令当成普通 `rollbackCommands`。
    
7. 如果用户明确要求恢复或升级，应生成高风险警告，并要求人工确认。
    

## 常见错误

1. 修改配置后忘记 `save`，设备重启后配置丢失。
    
2. 执行 `load configuration` 后没有检查当前配置是否符合预期。
    
3. 把 `load configuration`、`save`、`reboot` 连在一起执行，缺少人工确认。
    
4. 升级前没有备份配置和系统文件信息。
    
5. 升级时使用错误型号或错误版本的系统文件。
    
6. `reboot` 前没有确认业务影响。
    
7. 删除文件时误用 `delete /unreserved`，导致无法恢复。
    
8. 清空回收站前没有确认文件是否仍需要保留。
    
9. 存储空间不足时仍尝试上传系统文件或备份文件。
    
10. FTP / TFTP 地址错误或网络不可达，导致文件传输失败。
    
11. 指定启动配置后没有执行 `display startup` 验证。
    
12. 恢复配置后没有确认路由、ACL、VLAN、NAT 等关键业务配置是否仍符合预期。