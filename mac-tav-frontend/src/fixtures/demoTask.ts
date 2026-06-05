import type { DemoTask, StageSummary, WorkflowStage } from '@/api/types';

export const stageOrder: WorkflowStage[] = ['INTENT', 'PLANNING', 'CONFIGURATION', 'EXECUTION', 'VERIFICATION', 'HEALING'];

export const stageLabels: Record<WorkflowStage, { title: string; caption: string }> = {
  INTENT: { title: '意图', caption: 'IntentAgent' },
  PLANNING: { title: '规划', caption: 'PlanningAgent' },
  CONFIGURATION: { title: '配置', caption: 'ConfigurationAgent' },
  EXECUTION: { title: '执行', caption: 'ExecutionAdapter' },
  VERIFICATION: { title: '验证', caption: 'VerificationAgent' },
  HEALING: { title: '修复', caption: 'HealingAgent' }
};

export const sceneTemplates = [
  {
    title: '办公 / 访客隔离',
    subtitle: '生产安全场景',
    prompt: '办公区与访客区隔离，访客区不能访问服务器区；办公区允许访问服务器区，并生成配置、执行验证和失败修复建议。'
  },
  {
    title: '访客误通服务器',
    subtitle: '安全合规校验',
    prompt: '访客区错误地可以访问服务器区，请规划隔离策略，生成华为 VRP 配置，并在验证失败时给出修复建议。'
  },
  {
    title: '核心路由缺失',
    subtitle: '连通性自愈场景',
    prompt: '核心路由缺失导致办公区无法访问服务器区，请生成规划、配置候选和验证步骤，并给出修复路径。'
  },
  {
    title: 'ACL 方向错误',
    subtitle: '策略修复场景',
    prompt: 'ACL 应用方向错误导致隔离策略未生效，请定位方向问题并生成修复后的华为 VRP 命令块。'
  }
];

const stageSummaries: StageSummary[] = [
  {
    stage: 'INTENT',
    agentName: 'IntentAgent',
    artifactName: 'NetworkIntent',
    version: 1,
    summary: '识别出办公区、访客区、服务器区三类安全域，目标为隔离访客访问服务器并保留办公访问。',
    commandDigest: ['security-zone OFFICE GUEST SERVER', 'policy intent isolation']
  },
  {
    stage: 'PLANNING',
    agentName: 'PlanningAgent',
    artifactName: 'NetworkPlan',
    version: 2,
    summary: '生成核心路由、双汇聚交换、访客 VLAN 与生产 VLAN 的拓扑规划，并建立验证追溯点。',
    commandDigest: ['Core-Router-01 -> SW-Agg-01', 'VLAN 10 OFFICE / VLAN 20 GUEST']
  },
  {
    stage: 'CONFIGURATION',
    agentName: 'ConfigurationAgent',
    artifactName: 'ConfigSet',
    version: 2,
    summary: '已生成 Core-Switch-01 的 VRP 命令块，包含 VLAN、Eth-Trunk、ACL 与 OSPF 宣告。',
    commandDigest: ['sysname Core-Switch-01', 'acl number 3001', 'deny ip source 10.20.0.0 0.0.255.255']
  },
  {
    stage: 'EXECUTION',
    agentName: 'ExecutionAdapter',
    artifactName: 'ExecutionReport',
    version: 1,
    summary: '受控适配器完成结构化执行模拟，配置已应用到 Core-Switch-01 与汇聚节点。',
    commandDigest: ['adapter: structured-apply', 'device state: Config Applied']
  },
  {
    stage: 'VERIFICATION',
    agentName: 'VerificationAgent',
    artifactName: 'ValidationReport',
    version: 1,
    summary: '验证发现访客到服务器流量被拒绝，办公到服务器流量通过，隔离意图满足。',
    commandDigest: ['permit OFFICE -> SERVER', 'deny GUEST -> SERVER']
  },
  {
    stage: 'HEALING',
    agentName: 'HealingAgent',
    artifactName: 'RepairPlan',
    version: 1,
    summary: '预留失败诊断闭环，若验证失败将生成修复动作；RepairAction 不作为 workflow stage 展示。',
    commandDigest: ['rollback requires explicit artifact switch', 'repair action requires approval']
  }
];

export function createDemoTask(rawText: string): DemoTask {
  const stamp = Date.now();
  const taskId = `task-demo-${stamp.toString(36)}`;
  const jobId = `job-demo-${(stamp + 97).toString(36)}`;

  return {
    task: {
      taskId,
      taskStatus: 'RUNNING',
      currentStage: 'CONFIGURATION',
      createTime: new Date(stamp).toISOString(),
      rawText
    },
    latestJob: {
      jobId,
      taskId,
      requestedStage: null,
      jobType: 'FULL_WORKFLOW',
      jobStatus: 'RUNNING',
      requestedBy: 'frontend-mock',
      startTime: new Date(stamp - 12400).toISOString(),
      traceId: `trace-${stamp.toString(16)}`
    },
    elapsedSeconds: 12.4,
    telemetry: [
      {
        eventId: 'evt-001',
        eventType: 'stage.started',
        stage: 'INTENT',
        severity: 'INFO',
        title: '意图翻译启动',
        message: 'IntentAgent 已接收自然语言网络意图。',
        eventTime: new Date(stamp - 11200).toISOString()
      },
      {
        eventId: 'evt-002',
        eventType: 'artifact.generated',
        stage: 'PLANNING',
        severity: 'INFO',
        title: '规划产物生成',
        message: 'NetworkPlan v2 已写入 mock workspace。',
        eventTime: new Date(stamp - 7600).toISOString()
      },
      {
        eventId: 'evt-003',
        eventType: 'config.generated',
        stage: 'CONFIGURATION',
        severity: 'INFO',
        title: '配置候选生成',
        message: 'ConfigSet v2 已生成 Huawei VRP 命令块。',
        eventTime: new Date(stamp - 3200).toISOString()
      }
    ],
    stageSummaries,
    topology: {
      devices: [
        { id: 'internet', name: 'Internet', kind: 'router', role: '出口', model: 'WAN Edge', status: 'UP', x: 50, y: 7 },
        { id: 'core-router', name: 'Core-Router-01', kind: 'router', role: '核心路由', model: 'Huawei NE40E', status: 'UP', x: 50, y: 31 },
        { id: 'core-switch', name: 'Core-Switch-01', kind: 'switch', role: '核心交换', model: 'Huawei S12700', status: 'UP', x: 50, y: 56 },
        { id: 'agg-a', name: 'SW-Agg-01', kind: 'switch', role: '办公汇聚', model: 'Huawei S6730', status: 'UP', x: 25, y: 78 },
        { id: 'agg-b', name: 'SW-Agg-02', kind: 'switch', role: '访客汇聚', model: 'Huawei S6730', status: 'UP', x: 75, y: 78 },
        { id: 'prod', name: 'PROD', kind: 'service', role: '服务器区', model: 'VLAN 10', status: 'UP', x: 25, y: 94 },
        { id: 'guest', name: 'GUEST', kind: 'service', role: '访客区', model: 'VLAN 20', status: 'UP', x: 75, y: 94 }
      ],
      links: [
        { from: 'internet', to: 'core-router', status: 'ACTIVE' },
        { from: 'core-router', to: 'core-switch', status: 'ACTIVE' },
        { from: 'core-switch', to: 'agg-a', status: 'ACTIVE' },
        { from: 'core-switch', to: 'agg-b', status: 'ACTIVE' },
        { from: 'agg-a', to: 'prod', status: 'ACTIVE' },
        { from: 'agg-b', to: 'guest', status: 'ACTIVE' },
        { from: 'guest', to: 'prod', status: 'BLOCKED' }
      ]
    },
    deviceConfigs: [
      {
        deviceId: 'core-switch',
        title: 'Core-Switch-01',
        subtitle: 'Huawei S12700 | 状态: UP',
        commands: [
          '# 华为 VRP 核心配置命令块',
          'sysname Core-Switch-01',
          'vlan batch 10 20',
          'interface Eth-Trunk1',
          'description Uplink_To_Core_Router',
          'port link-type trunk',
          'port trunk allow-pass vlan 10 20',
          'acl number 3001',
          'deny ip source 10.20.0.0 0.0.255.255 destination 10.10.0.0 0.0.255.255',
          'permit ip source 10.10.0.0 0.0.255.255 destination 10.30.0.0 0.0.255.255',
          'return'
        ]
      },
      {
        deviceId: 'core-router',
        title: 'Core-Router-01',
        subtitle: 'Huawei NE40E | 状态: UP',
        commands: [
          '# 核心路由宣告',
          'sysname Core-Router-01',
          'interface GigabitEthernet0/0/1',
          'ip address 10.255.0.1 255.255.255.252',
          'ospf 100',
          'area 0.0.0.0',
          'network 10.10.0.0 0.0.255.255',
          'network 10.20.0.0 0.0.255.255',
          'return'
        ]
      },
      {
        deviceId: 'agg-a',
        title: 'SW-Agg-01',
        subtitle: 'Huawei S6730 | 状态: UP',
        commands: [
          '# 办公区汇聚',
          'sysname SW-Agg-01',
          'vlan batch 10',
          'interface Vlanif10',
          'ip address 10.10.0.1 255.255.0.0',
          'description OFFICE_GATEWAY',
          'permit ip source 10.10.0.0 0.0.255.255',
          'return'
        ]
      },
      {
        deviceId: 'agg-b',
        title: 'SW-Agg-02',
        subtitle: 'Huawei S6730 | 状态: UP',
        commands: [
          '# 访客区汇聚',
          'sysname SW-Agg-02',
          'vlan batch 20',
          'interface Vlanif20',
          'ip address 10.20.0.1 255.255.0.0',
          'description GUEST_GATEWAY',
          'deny ip source 10.20.0.0 0.0.255.255 destination 10.30.0.0 0.0.255.255',
          'return'
        ]
      },
      {
        deviceId: 'prod',
        title: 'PROD',
        subtitle: 'VLAN 10 | 服务器区',
        commands: ['# 服务器区策略摘要', 'permit ip source 10.10.0.0 0.0.255.255', 'deny ip source 10.20.0.0 0.0.255.255', 'return']
      },
      {
        deviceId: 'guest',
        title: 'GUEST',
        subtitle: 'VLAN 20 | 访客区',
        commands: ['# 访客区策略摘要', 'permit ip destination 0.0.0.0 255.255.255.255', 'deny ip destination 10.30.0.0 0.0.255.255', 'return']
      },
      {
        deviceId: 'internet',
        title: 'Internet',
        subtitle: 'WAN Edge | 出口',
        commands: ['# 出口设备摘要', 'interface GigabitEthernet0/0/0', 'description INTERNET_UPLINK', 'return']
      }
    ]
  };
}

export const defaultDemoTask = createDemoTask(sceneTemplates[0].prompt);
