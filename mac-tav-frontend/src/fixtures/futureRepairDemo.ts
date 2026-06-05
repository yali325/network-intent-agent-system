import type { RepairPlanDemo } from '@/api/futureContracts';

export const repairPlanFixture: RepairPlanDemo = {
  rca: {
    category: 'POLICY_PRIORITY_CONFLICT',
    severity: 'HIGH',
    scope: '访客区 -> 服务器区隔离路径',
    evidenceRefs: ['assert-guest-prod', 'cfg-core-acl-v1-shadow', 'probe-guest-prod-002'],
    recommendation:
      '演示诊断：根据验证证据与配置版本对比，访客区到服务器区的隔离断言失败。可能原因是 Core-Switch-01 上存在优先级更高的历史放行规则，导致新生成的阻断策略未能覆盖实际路径。'
  },
  actions: [
    {
      actionId: 'repair-clean-shadow-acl',
      actionType: 'PATCH_CONFIG',
      targetStage: 'CONFIGURATION',
      riskLevel: 'MEDIUM',
      requiresApproval: true,
      status: 'PENDING_APPROVAL',
      reason: '清理历史宽放行配置块，避免其覆盖访客隔离策略。',
      guidance: '候选配置片段，等待 Orchestrator 受控处理。',
      traceRefs: ['cfg-core-acl-v1-shadow', 'trace-acl-priority-conflict'],
      candidateSnippet: ['undo rule 12 permit ip source 10.1.1.0 0.0.0.255 destination 10.1.3.0 0.0.0.255', 'rule 12 deny ip source 10.1.1.0 0.0.0.255 destination 10.1.3.0 0.0.0.255']
    },
    {
      actionId: 'repair-regenerate-isolation',
      actionType: 'REGENERATE_CONFIG',
      targetStage: 'CONFIGURATION',
      riskLevel: 'MEDIUM',
      requiresApproval: true,
      status: 'PENDING_APPROVAL',
      reason: '重新生成访客区到服务器区隔离策略，并显式设置 ACL 优先级。',
      guidance: '生成候选 ConfigSet，不由前端直接执行 CLI。',
      traceRefs: ['NetworkIntent:v1', 'ConfigSet:v2']
    },
    {
      actionId: 'repair-reexecute-validation',
      actionType: 'REEXECUTE',
      targetStage: 'VERIFICATION',
      riskLevel: 'LOW',
      requiresApproval: false,
      status: 'PENDING_APPROVAL',
      reason: '修复动作完成后重新执行验证断言。',
      guidance: '提交验证 job，等待 workspace / validation 刷新。',
      traceRefs: ['ValidationReport:pending']
    }
  ]
};
