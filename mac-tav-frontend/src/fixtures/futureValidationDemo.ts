import type { EvidenceInspectorDemo, ValidationAssertionDemo } from '@/api/futureContracts';

export const validationAssertionsFixture: ValidationAssertionDemo[] = [
  {
    id: 'assert-office-prod',
    source: '办公区',
    destination: '服务器区',
    expectation: 'PERMIT',
    expectationLabel: 'PERMIT / 可访问',
    actual: 'REACHABLE',
    status: 'PASSED',
    traceRefs: ['trace-plan-office-prod', 'trace-acl-allow-office'],
    configBlockId: 'cfg-core-acl-v2',
    testId: 'probe-office-prod-001',
    message: '办公区到服务器区符合放行策略。'
  },
  {
    id: 'assert-guest-prod',
    source: '访客区',
    destination: '服务器区',
    expectation: 'DENY',
    expectationLabel: 'DENY / 阻断',
    actual: 'REACHABLE',
    status: 'FAILED',
    traceRefs: ['trace-guest-prod-deny', 'trace-acl-priority-conflict'],
    configBlockId: 'cfg-core-acl-v1-shadow',
    testId: 'probe-guest-prod-002',
    message: '期望隔离，但执行探测显示仍可达。建议进入修复分析。'
  },
  {
    id: 'assert-office-guest',
    source: '办公区',
    destination: '访客区',
    expectation: 'DENY',
    expectationLabel: 'DENY / 隔离',
    actual: 'BLOCKED',
    status: 'PASSED',
    traceRefs: ['trace-office-guest-deny'],
    configBlockId: 'cfg-zone-isolation-v2',
    testId: 'probe-office-guest-003',
    message: '办公区与访客区隔离断言通过。'
  }
];

export const evidenceInspectorFixture: EvidenceInspectorDemo = {
  relatedNode: 'Core-Switch-01',
  relatedConfigSummary: 'acl number 3001 包含访客区到服务器区 deny 规则，但历史宽放行规则可能先于新隔离策略生效。',
  versionDiff: [
    { version: 'v1', summary: '历史配置存在 permit ip source 10.1.1.0/24 destination 10.1.3.0/24', tone: 'old' },
    { version: 'v2', summary: '新增 deny ip source GUEST destination PROD，等待 Orchestrator 受控处理优先级。', tone: 'new' }
  ],
  conflictHint: '演示证据：检测到历史宽放行规则可能先于新隔离策略生效。'
};
