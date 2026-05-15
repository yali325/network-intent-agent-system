# Mock 数据设计

本文档定义当前 Demo 阶段使用的默认 Mock 数据。所有 Mock 数据服务于端到端流程演示，不代表真实网络环境执行结果。

## 1. 默认 Demo 输入

```text
构建一个办公区和访客区隔离的网络，两个区域都能访问互联网，办公区可以访问服务器，访客区不能访问服务器，采用 OSPF。
```

固定任务 ID 可使用：

```text
task-10001
```

实际实现时也可以自动生成任务 ID。

## 2. Mock 场景对象

| 对象 ID | 名称 | 类型 | 说明 |
| --- | --- | --- | --- |
| `office` | 办公区 | ZONE | 企业内部办公用户 |
| `guest` | 访客区 | ZONE | 访客用户 |
| `server` | 服务器区 | ZONE | 内部服务器 |
| `internet` | 公网 | EXTERNAL_NETWORK | 外部互联网 |

## 3. Mock 意图关系

| 关系 ID | 类型 | 源 | 目标 | 动作 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `rel-001` | ACCESS | `office` | `server` | ALLOW | 办公区可以访问服务器区 |
| `rel-002` | ACCESS | `guest` | `server` | DENY | 访客区不能访问服务器区 |
| `rel-003` | ACCESS | `office` | `internet` | ALLOW | 办公区可以访问公网 |
| `rel-004` | ACCESS | `guest` | `internet` | ALLOW | 访客区可以访问公网 |
| `rel-005` | ISOLATION | `office` | `guest` | DENY | 办公区与访客区隔离 |

## 4. 完整 Mock NetworkIntent JSON

```json
{
  "taskId": "task-10001",
  "intentVersion": 1,
  "rawText": "构建一个办公区和访客区隔离的网络，两个区域都能访问互联网，办公区可以访问服务器，访客区不能访问服务器，采用 OSPF。",
  "semanticIntentGraph": {
    "nodes": [
      {
        "id": "office",
        "name": "办公区",
        "type": "ZONE",
        "description": "企业内部办公用户所在区域"
      },
      {
        "id": "guest",
        "name": "访客区",
        "type": "ZONE",
        "description": "访客用户所在区域"
      },
      {
        "id": "server",
        "name": "服务器区",
        "type": "ZONE",
        "description": "内部服务器所在区域"
      },
      {
        "id": "internet",
        "name": "公网",
        "type": "EXTERNAL_NETWORK",
        "description": "外部互联网"
      }
    ],
    "relations": [
      {
        "id": "rel-001",
        "type": "ACCESS",
        "source": "office",
        "target": "server",
        "action": "ALLOW",
        "service": "ANY",
        "description": "办公区可以访问服务器区",
        "explicit": true
      },
      {
        "id": "rel-002",
        "type": "ACCESS",
        "source": "guest",
        "target": "server",
        "action": "DENY",
        "service": "ANY",
        "description": "访客区不能访问服务器区",
        "explicit": true
      },
      {
        "id": "rel-003",
        "type": "ACCESS",
        "source": "office",
        "target": "internet",
        "action": "ALLOW",
        "service": "ANY",
        "description": "办公区可以访问公网",
        "explicit": true
      },
      {
        "id": "rel-004",
        "type": "ACCESS",
        "source": "guest",
        "target": "internet",
        "action": "ALLOW",
        "service": "ANY",
        "description": "访客区可以访问公网",
        "explicit": true
      },
      {
        "id": "rel-005",
        "type": "ISOLATION",
        "source": "office",
        "target": "guest",
        "action": "DENY",
        "service": "ANY",
        "description": "办公区与访客区需要相互隔离",
        "explicit": true
      }
    ]
  },
  "assumptions": [
    {
      "field": "deviceTopology",
      "value": "AUTO_PLAN",
      "reason": "用户未指定具体设备数量和连接方式，后续由 Planning Agent 自动规划拓扑"
    },
    {
      "field": "addressPlan",
      "value": "AUTO_PLAN",
      "reason": "用户未指定 IP 地址规划，后续由 Planning Agent 自动分配网段和网关"
    },
    {
      "field": "vendor",
      "value": "Huawei",
      "reason": "用户未指定设备厂商，系统默认使用 Huawei 风格配置命令"
    }
  ],
  "stageStatus": "SUCCESS"
}
```

## 5. 完整 Mock NetworkPlan JSON

```json
{
  "taskId": "task-10001",
  "intentVersion": 1,
  "planVersion": 1,
  "planSummary": "系统规划一个基于 VLAN 隔离、单路由器三层转发、ACL 访问控制和 OSPF 路由的企业接入网络方案。",
  "selectedArchitecture": {
    "type": "ROUTER_ON_A_STICK",
    "reason": "该方案适合轻量实验场景，可以用较少设备实现多区域隔离、三层互通和访问控制。"
  },
  "topology": {
    "nodes": [
      {
        "id": "R1",
        "name": "出口路由器",
        "nodeType": "DEVICE",
        "deviceType": "ROUTER",
        "role": "GATEWAY",
        "vendor": "Huawei"
      },
      {
        "id": "SW1",
        "name": "接入交换机1",
        "nodeType": "DEVICE",
        "deviceType": "SWITCH",
        "role": "ACCESS",
        "vendor": "Huawei"
      },
      {
        "id": "SW2",
        "name": "接入交换机2",
        "nodeType": "DEVICE",
        "deviceType": "SWITCH",
        "role": "ACCESS",
        "vendor": "Huawei"
      },
      {
        "id": "office-pc-1",
        "name": "办公区主机",
        "nodeType": "HOST",
        "hostType": "PC",
        "zoneId": "office"
      },
      {
        "id": "guest-pc-1",
        "name": "访客区主机",
        "nodeType": "HOST",
        "hostType": "PC",
        "zoneId": "guest"
      },
      {
        "id": "server-1",
        "name": "服务器",
        "nodeType": "HOST",
        "hostType": "SERVER",
        "zoneId": "server"
      },
      {
        "id": "internet",
        "name": "公网",
        "nodeType": "EXTERNAL_NETWORK",
        "zoneId": "internet"
      }
    ],
    "links": [
      {
        "id": "link-001",
        "sourceNode": "R1",
        "sourceInterface": "GE0/0/0",
        "targetNode": "SW1",
        "targetInterface": "GE0/0/1",
        "linkType": "TRUNK"
      },
      {
        "id": "link-002",
        "sourceNode": "SW1",
        "sourceInterface": "GE0/0/2",
        "targetNode": "SW2",
        "targetInterface": "GE0/0/1",
        "linkType": "TRUNK"
      },
      {
        "id": "link-003",
        "sourceNode": "office-pc-1",
        "sourceInterface": "eth0",
        "targetNode": "SW1",
        "targetInterface": "GE0/0/10",
        "linkType": "ACCESS"
      },
      {
        "id": "link-004",
        "sourceNode": "guest-pc-1",
        "sourceInterface": "eth0",
        "targetNode": "SW1",
        "targetInterface": "GE0/0/11",
        "linkType": "ACCESS"
      },
      {
        "id": "link-005",
        "sourceNode": "server-1",
        "sourceInterface": "eth0",
        "targetNode": "SW2",
        "targetInterface": "GE0/0/10",
        "linkType": "ACCESS"
      },
      {
        "id": "link-006",
        "sourceNode": "R1",
        "sourceInterface": "GE0/0/1",
        "targetNode": "internet",
        "targetInterface": "wan0",
        "linkType": "WAN"
      }
    ]
  },
  "zones": [
    {
      "id": "office",
      "name": "办公区",
      "mappedFromIntentNode": "office",
      "zoneType": "USER_ZONE"
    },
    {
      "id": "guest",
      "name": "访客区",
      "mappedFromIntentNode": "guest",
      "zoneType": "USER_ZONE"
    },
    {
      "id": "server",
      "name": "服务器区",
      "mappedFromIntentNode": "server",
      "zoneType": "SERVER_ZONE"
    },
    {
      "id": "internet",
      "name": "公网",
      "mappedFromIntentNode": "internet",
      "zoneType": "EXTERNAL_NETWORK"
    }
  ],
  "addressPlan": [
    {
      "id": "address-office",
      "zoneId": "office",
      "subnet": "192.168.10.0/24",
      "gateway": "192.168.10.1",
      "sampleIp": "192.168.10.10"
    },
    {
      "id": "address-guest",
      "zoneId": "guest",
      "subnet": "192.168.20.0/24",
      "gateway": "192.168.20.1",
      "sampleIp": "192.168.20.10"
    },
    {
      "id": "address-server",
      "zoneId": "server",
      "subnet": "192.168.30.0/24",
      "gateway": "192.168.30.1",
      "sampleIp": "192.168.30.10"
    }
  ],
  "vlanPlan": [
    {
      "id": "vlan-office",
      "vlanId": 10,
      "name": "OFFICE",
      "zoneId": "office",
      "accessPorts": [
        {
          "deviceId": "SW1",
          "interfaceName": "GE0/0/10"
        }
      ]
    },
    {
      "id": "vlan-guest",
      "vlanId": 20,
      "name": "GUEST",
      "zoneId": "guest",
      "accessPorts": [
        {
          "deviceId": "SW1",
          "interfaceName": "GE0/0/11"
        }
      ]
    },
    {
      "id": "vlan-server",
      "vlanId": 30,
      "name": "SERVER",
      "zoneId": "server",
      "accessPorts": [
        {
          "deviceId": "SW2",
          "interfaceName": "GE0/0/10"
        }
      ]
    }
  ],
  "routingPlan": {
    "id": "routing-ospf",
    "protocol": "OSPF",
    "area": "0.0.0.0",
    "routers": [
      {
        "id": "routing-ospf-r1",
        "deviceId": "R1",
        "routerId": "1.1.1.1",
        "advertisedNetworks": [
          "192.168.10.0/24",
          "192.168.20.0/24",
          "192.168.30.0/24"
        ]
      }
    ],
    "defaultRoute": {
      "enabled": true,
      "nextHop": "ISP"
    }
  },
  "securityPolicyPlan": [
    {
      "id": "policy-001",
      "name": "deny_guest_to_server",
      "sourceZone": "guest",
      "targetZone": "server",
      "action": "DENY",
      "service": "ANY",
      "enforcementPoint": {
        "deviceId": "R1",
        "interfaceName": "GE0/0/0.20",
        "direction": "INBOUND"
      },
      "basedOnIntentRelation": "rel-002"
    },
    {
      "id": "policy-002",
      "name": "deny_office_guest_access",
      "sourceZone": "office",
      "targetZone": "guest",
      "action": "DENY",
      "service": "ANY",
      "enforcementPoint": {
        "deviceId": "R1",
        "interfaceName": "GE0/0/0.10",
        "direction": "INBOUND"
      },
      "basedOnIntentRelation": "rel-005"
    }
  ],
  "natPlan": {
    "id": "nat-internet-access",
    "enabled": true,
    "insideZones": ["office", "guest"],
    "outsideInterface": {
      "deviceId": "R1",
      "interfaceName": "GE0/0/1"
    },
    "description": "办公区和访客区通过出口路由器访问公网。"
  },
  "targetEnvironment": {
    "vendor": "Huawei",
    "configStyle": "CLI",
    "simulationTarget": "DRY_RUN"
  },
  "stageStatus": "SUCCESS"
}
```

## 6. 完整 Mock ConfigSet JSON

```json
{
  "taskId": "task-10001",
  "planVersion": 1,
  "configVersion": 1,
  "targetEnvironment": {
    "vendor": "Huawei",
    "configStyle": "CLI",
    "simulationTarget": "DRY_RUN"
  },
  "generationSummary": "已为 R1、SW1、SW2 生成 VLAN、接口、OSPF、ACL、NAT 以及主机地址相关配置。",
  "generationSources": [
    {
      "sourceType": "MOCK_TEMPLATE",
      "sourceName": "office_guest_server_template",
      "description": "当前 Demo 使用固定模板生成 Huawei 风格配置。"
    }
  ],
  "deviceConfigs": [
    {
      "deviceId": "R1",
      "deviceName": "出口路由器",
      "deviceType": "ROUTER",
      "vendor": "Huawei",
      "configText": "interface GigabitEthernet0/0/0.10\n dot1q termination vid 10\n ip address 192.168.10.1 255.255.255.0\n arp broadcast enable\n#\ninterface GigabitEthernet0/0/0.20\n dot1q termination vid 20\n ip address 192.168.20.1 255.255.255.0\n arp broadcast enable\n#\ninterface GigabitEthernet0/0/0.30\n dot1q termination vid 30\n ip address 192.168.30.1 255.255.255.0\n arp broadcast enable\n#\nacl number 3000\n rule 5 deny ip source 192.168.20.0 0.0.0.255 destination 192.168.30.0 0.0.0.255\n rule 10 permit ip\n#\ninterface GigabitEthernet0/0/0.20\n traffic-filter inbound acl 3000\n#\nospf 1 router-id 1.1.1.1\n area 0.0.0.0\n  network 192.168.10.0 0.0.0.255\n  network 192.168.20.0 0.0.0.255\n  network 192.168.30.0 0.0.0.255",
      "commandBlocks": [
        {
          "blockId": "R1-IF-001",
          "blockType": "INTERFACE",
          "order": 10,
          "title": "配置 VLAN10、VLAN20、VLAN30 的三层网关子接口",
          "commands": [
            "interface GigabitEthernet0/0/0.10",
            "dot1q termination vid 10",
            "ip address 192.168.10.1 255.255.255.0",
            "arp broadcast enable",
            "interface GigabitEthernet0/0/0.20",
            "dot1q termination vid 20",
            "ip address 192.168.20.1 255.255.255.0",
            "arp broadcast enable",
            "interface GigabitEthernet0/0/0.30",
            "dot1q termination vid 30",
            "ip address 192.168.30.1 255.255.255.0",
            "arp broadcast enable"
          ],
          "explanation": "为办公区、访客区和服务器区分别创建三层网关子接口，实现不同 VLAN 之间的三层转发。",
          "rollbackCommands": [
            "undo interface GigabitEthernet0/0/0.10",
            "undo interface GigabitEthernet0/0/0.20",
            "undo interface GigabitEthernet0/0/0.30"
          ],
          "dependsOn": [],
          "traceRefs": {
            "intentRelationIds": ["rel-001", "rel-002", "rel-003", "rel-004"],
            "planElementIds": ["address-office", "address-guest", "address-server", "vlan-office", "vlan-guest", "vlan-server"]
          }
        },
        {
          "blockId": "R1-ACL-001",
          "blockType": "ACL",
          "order": 20,
          "title": "禁止访客区访问服务器区",
          "commands": [
            "acl number 3000",
            "rule 5 deny ip source 192.168.20.0 0.0.0.255 destination 192.168.30.0 0.0.0.255",
            "rule 10 permit ip",
            "interface GigabitEthernet0/0/0.20",
            "traffic-filter inbound acl 3000"
          ],
          "explanation": "该配置根据安全策略 policy-001 生成，用于阻止访客区访问服务器区。",
          "rollbackCommands": [
            "interface GigabitEthernet0/0/0.20",
            "undo traffic-filter inbound",
            "undo acl number 3000"
          ],
          "dependsOn": ["R1-IF-001"],
          "traceRefs": {
            "intentRelationIds": ["rel-002"],
            "planElementIds": ["policy-001"]
          }
        },
        {
          "blockId": "R1-OSPF-001",
          "blockType": "ROUTING",
          "order": 30,
          "title": "配置 OSPF 路由协议",
          "commands": [
            "ospf 1 router-id 1.1.1.1",
            "area 0.0.0.0",
            "network 192.168.10.0 0.0.0.255",
            "network 192.168.20.0 0.0.0.255",
            "network 192.168.30.0 0.0.0.255"
          ],
          "explanation": "根据 routingPlan 生成 OSPF 配置，使各业务网段可以被路由协议识别。",
          "rollbackCommands": ["undo ospf 1"],
          "dependsOn": ["R1-IF-001"],
          "traceRefs": {
            "intentRelationIds": ["rel-001", "rel-003", "rel-004"],
            "planElementIds": ["routing-ospf", "routing-ospf-r1"]
          }
        },
        {
          "blockId": "R1-NAT-001",
          "blockType": "NAT",
          "order": 40,
          "title": "配置办公区和访客区访问公网的 NAT 意图",
          "commands": [
            "acl number 2000",
            "rule 5 permit source 192.168.10.0 0.0.0.255",
            "rule 10 permit source 192.168.20.0 0.0.0.255",
            "interface GigabitEthernet0/0/1",
            "nat outbound 2000"
          ],
          "explanation": "当前为 Demo 风格 NAT 配置，真实出口参数后续按环境调整。",
          "rollbackCommands": [
            "interface GigabitEthernet0/0/1",
            "undo nat outbound 2000",
            "undo acl number 2000"
          ],
          "dependsOn": ["R1-IF-001"],
          "traceRefs": {
            "intentRelationIds": ["rel-003", "rel-004"],
            "planElementIds": ["nat-internet-access"]
          }
        }
      ]
    },
    {
      "deviceId": "SW1",
      "deviceName": "接入交换机1",
      "deviceType": "SWITCH",
      "vendor": "Huawei",
      "configText": "vlan batch 10 20 30\n#\ninterface GigabitEthernet0/0/1\n port link-type trunk\n port trunk allow-pass vlan 10 20 30\n#\ninterface GigabitEthernet0/0/2\n port link-type trunk\n port trunk allow-pass vlan 10 20 30\n#\ninterface GigabitEthernet0/0/10\n port link-type access\n port default vlan 10\n#\ninterface GigabitEthernet0/0/11\n port link-type access\n port default vlan 20",
      "commandBlocks": [
        {
          "blockId": "SW1-VLAN-001",
          "blockType": "VLAN",
          "order": 10,
          "title": "创建办公区、访客区和服务器区 VLAN",
          "commands": ["vlan batch 10 20 30"],
          "explanation": "在 SW1 上创建 VLAN10、VLAN20 和 VLAN30，为不同业务区域提供二层隔离基础。",
          "rollbackCommands": ["undo vlan batch 10 20 30"],
          "dependsOn": [],
          "traceRefs": {
            "intentRelationIds": ["rel-005"],
            "planElementIds": ["vlan-office", "vlan-guest", "vlan-server"]
          }
        },
        {
          "blockId": "SW1-PORT-001",
          "blockType": "INTERFACE",
          "order": 20,
          "title": "配置 SW1 上联和接入口",
          "commands": [
            "interface GigabitEthernet0/0/1",
            "port link-type trunk",
            "port trunk allow-pass vlan 10 20 30",
            "interface GigabitEthernet0/0/2",
            "port link-type trunk",
            "port trunk allow-pass vlan 10 20 30",
            "interface GigabitEthernet0/0/10",
            "port link-type access",
            "port default vlan 10",
            "interface GigabitEthernet0/0/11",
            "port link-type access",
            "port default vlan 20"
          ],
          "explanation": "将上联口配置为 trunk，将办公区主机端口加入 VLAN10，将访客区主机端口加入 VLAN20。",
          "rollbackCommands": [
            "interface GigabitEthernet0/0/1",
            "undo port trunk allow-pass vlan 10 20 30",
            "interface GigabitEthernet0/0/2",
            "undo port trunk allow-pass vlan 10 20 30",
            "interface GigabitEthernet0/0/10",
            "undo port default vlan",
            "interface GigabitEthernet0/0/11",
            "undo port default vlan"
          ],
          "dependsOn": ["SW1-VLAN-001"],
          "traceRefs": {
            "intentRelationIds": ["rel-003", "rel-004", "rel-005"],
            "planElementIds": ["link-001", "link-002", "vlan-office", "vlan-guest"]
          }
        }
      ]
    },
    {
      "deviceId": "SW2",
      "deviceName": "接入交换机2",
      "deviceType": "SWITCH",
      "vendor": "Huawei",
      "configText": "vlan batch 10 20 30\n#\ninterface GigabitEthernet0/0/1\n port link-type trunk\n port trunk allow-pass vlan 10 20 30\n#\ninterface GigabitEthernet0/0/10\n port link-type access\n port default vlan 30",
      "commandBlocks": [
        {
          "blockId": "SW2-VLAN-001",
          "blockType": "VLAN",
          "order": 10,
          "title": "创建服务器区相关 VLAN",
          "commands": ["vlan batch 10 20 30"],
          "explanation": "在 SW2 上创建 VLAN10、VLAN20 和 VLAN30，保证与上联 trunk 允许 VLAN 一致。",
          "rollbackCommands": ["undo vlan batch 10 20 30"],
          "dependsOn": [],
          "traceRefs": {
            "intentRelationIds": ["rel-001", "rel-002"],
            "planElementIds": ["vlan-server"]
          }
        },
        {
          "blockId": "SW2-PORT-001",
          "blockType": "INTERFACE",
          "order": 20,
          "title": "配置 SW2 上联和服务器端口",
          "commands": [
            "interface GigabitEthernet0/0/1",
            "port link-type trunk",
            "port trunk allow-pass vlan 10 20 30",
            "interface GigabitEthernet0/0/10",
            "port link-type access",
            "port default vlan 30"
          ],
          "explanation": "将 SW2 上联口配置为 trunk，将服务器端口加入 VLAN30。",
          "rollbackCommands": [
            "interface GigabitEthernet0/0/1",
            "undo port trunk allow-pass vlan 10 20 30",
            "interface GigabitEthernet0/0/10",
            "undo port default vlan"
          ],
          "dependsOn": ["SW2-VLAN-001"],
          "traceRefs": {
            "intentRelationIds": ["rel-001", "rel-002"],
            "planElementIds": ["link-002", "link-005", "vlan-server"]
          }
        }
      ]
    }
  ],
  "endpointConfigs": [
    {
      "nodeId": "office-pc-1",
      "nodeType": "HOST",
      "zoneId": "office",
      "commands": [
        "ip addr add 192.168.10.10/24 dev eth0",
        "ip route add default via 192.168.10.1"
      ],
      "explanation": "为办公区主机配置 IP 地址和默认网关。"
    },
    {
      "nodeId": "guest-pc-1",
      "nodeType": "HOST",
      "zoneId": "guest",
      "commands": [
        "ip addr add 192.168.20.10/24 dev eth0",
        "ip route add default via 192.168.20.1"
      ],
      "explanation": "为访客区主机配置 IP 地址和默认网关。"
    },
    {
      "nodeId": "server-1",
      "nodeType": "HOST",
      "zoneId": "server",
      "commands": [
        "ip addr add 192.168.30.10/24 dev eth0",
        "ip route add default via 192.168.30.1"
      ],
      "explanation": "为服务器配置 IP 地址和默认网关。"
    }
  ],
  "rollbackPlan": {
    "strategy": "REVERSE_ORDER",
    "blockIds": [
      "R1-NAT-001",
      "R1-OSPF-001",
      "R1-ACL-001",
      "R1-IF-001",
      "SW1-PORT-001",
      "SW1-VLAN-001",
      "SW2-PORT-001",
      "SW2-VLAN-001"
    ],
    "description": "按配置块逆序回滚。"
  },
  "warnings": [
    {
      "level": "LOW",
      "message": "当前配置为实验环境示例，真实设备接口名称和 NAT 出口参数需要根据实际环境调整。"
    }
  ],
  "stageStatus": "SUCCESS"
}
```

## 7. 完整 Mock ExecutionReport JSON

当前阶段执行模式固定为 `DRY_RUN`。Execution Module 不直接执行 Huawei CLI，只返回经过 Execution Adapter 转换后的 DryRun 内容和 Mock 测试结果。

```json
{
  "taskId": "task-10001",
  "planVersion": 1,
  "configVersion": 1,
  "executionVersion": 1,
  "executionMode": "DRY_RUN",
  "executionPlan": {
    "adapterType": "DRY_RUN",
    "topologyScript": "# DryRun: create R1, SW1, SW2, office-pc-1, guest-pc-1, server-1 and internet links",
    "hostCommands": [
      {
        "commandId": "host-cmd-001",
        "nodeId": "office-pc-1",
        "command": "ip addr add 192.168.10.10/24 dev eth0"
      },
      {
        "commandId": "host-cmd-002",
        "nodeId": "guest-pc-1",
        "command": "ip addr add 192.168.20.10/24 dev eth0"
      },
      {
        "commandId": "host-cmd-003",
        "nodeId": "server-1",
        "command": "ip addr add 192.168.30.10/24 dev eth0"
      }
    ],
    "flowRules": [],
    "testCommands": [
      {
        "testId": "test-001",
        "type": "PING",
        "source": "office-pc-1",
        "target": "server-1",
        "expected": "REACHABLE"
      },
      {
        "testId": "test-002",
        "type": "PING",
        "source": "guest-pc-1",
        "target": "server-1",
        "expected": "BLOCKED"
      },
      {
        "testId": "test-003",
        "type": "PING",
        "source": "office-pc-1",
        "target": "internet",
        "expected": "REACHABLE"
      },
      {
        "testId": "test-004",
        "type": "PING",
        "source": "guest-pc-1",
        "target": "internet",
        "expected": "REACHABLE"
      },
      {
        "testId": "test-005",
        "type": "PING",
        "source": "office-pc-1",
        "target": "guest-pc-1",
        "expected": "BLOCKED"
      }
    ]
  },
  "runtimeState": {
    "environmentStatus": "MOCK_READY",
    "controllerConnected": true,
    "nodes": [
      { "nodeId": "R1", "status": "UP" },
      { "nodeId": "SW1", "status": "UP" },
      { "nodeId": "SW2", "status": "UP" },
      { "nodeId": "office-pc-1", "status": "UP" },
      { "nodeId": "guest-pc-1", "status": "UP" },
      { "nodeId": "server-1", "status": "UP" },
      { "nodeId": "internet", "status": "UP" }
    ],
    "links": [
      { "linkId": "link-001", "status": "UP" },
      { "linkId": "link-002", "status": "UP" },
      { "linkId": "link-003", "status": "UP" },
      { "linkId": "link-004", "status": "UP" },
      { "linkId": "link-005", "status": "UP" },
      { "linkId": "link-006", "status": "UP" }
    ]
  },
  "testResult": {
    "connectivityTests": [
      {
        "testId": "test-001",
        "source": "office-pc-1",
        "target": "server-1",
        "expected": "REACHABLE",
        "actual": "REACHABLE",
        "success": true,
        "rawOutput": "Mock ping office-pc-1 -> server-1 success"
      },
      {
        "testId": "test-003",
        "source": "office-pc-1",
        "target": "internet",
        "expected": "REACHABLE",
        "actual": "REACHABLE",
        "success": true,
        "rawOutput": "Mock ping office-pc-1 -> internet success"
      },
      {
        "testId": "test-004",
        "source": "guest-pc-1",
        "target": "internet",
        "expected": "REACHABLE",
        "actual": "REACHABLE",
        "success": true,
        "rawOutput": "Mock ping guest-pc-1 -> internet success"
      }
    ],
    "policyTests": [
      {
        "testId": "test-002",
        "source": "guest-pc-1",
        "target": "server-1",
        "expected": "BLOCKED",
        "actual": "BLOCKED",
        "success": true,
        "rawOutput": "Mock policy guest-pc-1 -> server-1 blocked"
      },
      {
        "testId": "test-005",
        "source": "office-pc-1",
        "target": "guest-pc-1",
        "expected": "BLOCKED",
        "actual": "BLOCKED",
        "success": true,
        "rawOutput": "Mock policy office-pc-1 -> guest-pc-1 blocked"
      }
    ],
    "rawLogs": [
      "DryRun adapter generated topology and test commands.",
      "All mock nodes are UP.",
      "All mock links are UP.",
      "All mock tests matched expected results."
    ]
  },
  "stageStatus": "SUCCESS"
}
```

## 8. 完整 Mock ValidationReport JSON

```json
{
  "taskId": "task-10001",
  "intentVersion": 1,
  "planVersion": 1,
  "configVersion": 1,
  "executionVersion": 1,
  "validationVersion": 1,
  "overallStatus": "PASSED",
  "summary": "办公区访问服务器、公网访问、访客区公网访问和隔离策略均符合用户意图。",
  "items": [
    {
      "itemId": "val-001",
      "name": "办公区访问服务器",
      "type": "CONNECTIVITY",
      "expected": "REACHABLE",
      "actual": "REACHABLE",
      "passed": true,
      "relatedIntentRelationId": "rel-001",
      "relatedPlanElementIds": ["address-office", "address-server", "routing-ospf", "routing-ospf-r1"],
      "relatedConfigBlockIds": ["R1-IF-001", "R1-OSPF-001"],
      "relatedTestId": "test-001",
      "message": "办公区主机可以访问服务器，符合用户意图。"
    },
    {
      "itemId": "val-002",
      "name": "访客区禁止访问服务器",
      "type": "ISOLATION",
      "expected": "BLOCKED",
      "actual": "BLOCKED",
      "passed": true,
      "relatedIntentRelationId": "rel-002",
      "relatedPlanElementIds": ["policy-001"],
      "relatedConfigBlockIds": ["R1-ACL-001"],
      "relatedTestId": "test-002",
      "message": "访客区访问服务器被阻断，符合用户意图。"
    },
    {
      "itemId": "val-003",
      "name": "办公区访问公网",
      "type": "CONNECTIVITY",
      "expected": "REACHABLE",
      "actual": "REACHABLE",
      "passed": true,
      "relatedIntentRelationId": "rel-003",
      "relatedPlanElementIds": ["nat-internet-access", "routing-ospf"],
      "relatedConfigBlockIds": ["R1-NAT-001", "R1-OSPF-001"],
      "relatedTestId": "test-003",
      "message": "办公区可以访问公网，符合用户意图。"
    },
    {
      "itemId": "val-004",
      "name": "访客区访问公网",
      "type": "CONNECTIVITY",
      "expected": "REACHABLE",
      "actual": "REACHABLE",
      "passed": true,
      "relatedIntentRelationId": "rel-004",
      "relatedPlanElementIds": ["nat-internet-access", "routing-ospf"],
      "relatedConfigBlockIds": ["R1-NAT-001", "R1-OSPF-001"],
      "relatedTestId": "test-004",
      "message": "访客区可以访问公网，符合用户意图。"
    },
    {
      "itemId": "val-005",
      "name": "办公区与访客区隔离",
      "type": "ISOLATION",
      "expected": "BLOCKED",
      "actual": "BLOCKED",
      "passed": true,
      "relatedIntentRelationId": "rel-005",
      "relatedPlanElementIds": ["policy-002", "vlan-office", "vlan-guest"],
      "relatedConfigBlockIds": ["R1-ACL-001", "SW1-VLAN-001", "SW1-PORT-001"],
      "relatedTestId": "test-005",
      "message": "办公区与访客区互访被阻断，符合隔离意图。"
    }
  ],
  "suggestions": [
    "当前为 DryRun / Mock 验证结果，后续接入 Mininet/Ryu 后需要用真实测试结果替换。"
  ],
  "stageStatus": "SUCCESS"
}
```

## 9. Mock 失败场景预留

为了后续测试 Verification 和 Healing，可预留一个失败模式：

```text
guest_to_server_unexpected_pass
```

含义：

1. `guest-pc-1 -> server-1` 实际结果为 `REACHABLE`。
2. 期望结果为 `BLOCKED`。
3. Verification Agent 输出 `FAILED`。
4. 后续 `mac-tav-healing-agent` 可根据该失败定位到 `R1-ACL-001` 或 `policy-001`。
5. 当前阶段不创建正式 Healing Maven 模块，只返回 TODO 或 Mock 建议，例如“请检查 R1-ACL-001 是否正确生成或执行适配”。

失败场景的关键覆盖数据：

```json
{
  "scenario": "guest_to_server_unexpected_pass",
  "failedTest": {
    "testId": "test-002",
    "source": "guest-pc-1",
    "target": "server-1",
    "expected": "BLOCKED",
    "actual": "REACHABLE",
    "success": false
  },
  "expectedValidation": {
    "overallStatus": "FAILED",
    "failedValidationItem": "val-002",
    "relatedIntentRelationId": "rel-002",
    "relatedPlanElementIds": ["policy-001"],
    "relatedConfigBlockIds": ["R1-ACL-001"],
    "mockSuggestion": "访客区访问服务器未被阻断，请检查策略 policy-001 与配置块 R1-ACL-001。"
  }
}
```

第一阶段可以不通过 API 暴露失败模式，但 Mock 服务内部应容易扩展。
