import type { WorkflowStage } from '@/api/types';

export type ValidationStatus = 'PASSED' | 'FAILED' | 'PENDING';
export type PolicyExpectation = 'PERMIT' | 'DENY';
export type ProbeResult = 'REACHABLE' | 'BLOCKED';
export type RepairActionStatus = 'PENDING_APPROVAL' | 'APPROVED' | 'APPLIED' | 'REJECTED';
export type RepairActionType = 'PATCH_CONFIG' | 'REGENERATE_CONFIG' | 'REEXECUTE';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type MissionView = 'control' | 'validation' | 'repair';
export type MockRepairPhase = 'idle' | 'analysis_ready' | 'approved' | 'applying' | 'applied';

export interface ValidationAssertionDemo {
  id: string;
  source: string;
  destination: string;
  expectation: PolicyExpectation;
  expectationLabel: string;
  actual: ProbeResult;
  status: ValidationStatus;
  traceRefs: string[];
  configBlockId: string;
  testId: string;
  message: string;
}

export interface EvidenceInspectorDemo {
  relatedNode: string;
  relatedConfigSummary: string;
  versionDiff: Array<{ version: string; summary: string; tone: 'old' | 'new' }>;
  conflictHint: string;
}

export interface RcaReportDemo {
  category: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH';
  scope: string;
  evidenceRefs: string[];
  recommendation: string;
}

export interface RepairActionDemo {
  actionId: string;
  actionType: RepairActionType;
  targetStage: WorkflowStage;
  riskLevel: RiskLevel;
  requiresApproval: boolean;
  status: RepairActionStatus;
  reason: string;
  guidance: string;
  traceRefs: string[];
  candidateSnippet?: string[];
}

export interface RepairPlanDemo {
  rca: RcaReportDemo;
  actions: RepairActionDemo[];
}

export interface WorkflowJobSubmitResponse {
  jobId: string;
}

export const futureBackendApiBacklog = [
  'GET /api/v1/views/{taskId}/validation-matrix',
  'GET /api/v1/views/{taskId}/repair-cockpit',
  'POST /api/v1/repairs/{taskId}/actions/{actionId}/simulate',
  'GET /api/v1/artifacts/{taskId}/trace-graph',
  'GET /api/v1/views/{taskId}/topology-state'
];
