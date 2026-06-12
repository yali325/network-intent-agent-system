export type WorkflowStage = "INTENT" | "PLANNING" | "CONFIGURATION" | "EXECUTION" | "VERIFICATION" | "HEALING";

export type WorkflowJobStatus = "PENDING" | "RUNNING" | "SUCCESS" | "FAILED" | "CANCELLED" | "INTERRUPTED";

export type WorkflowJobType = "FULL_WORKFLOW" | "RUN_STAGE" | "RERUN_STAGE" | "CONTINUE_FROM_STAGE" | "REPAIR_ANALYZE" | "REPAIR_APPLY";

export interface PageResult<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}

export interface TaskSummaryResponse {
  taskId: string;
  taskStatus: WorkflowJobStatus;
  currentStage: WorkflowStage;
  createTime: string;
  rawText: string;
}

export interface WorkflowJob {
  jobId: string;
  taskId: string;
  requestedStage: WorkflowStage | null;
  jobType: "FULL_WORKFLOW" | "RUN_STAGE" | "RERUN_STAGE" | "CONTINUE_FROM_STAGE";
  jobStatus: WorkflowJobStatus;
  requestedBy: string;
  startTime: string;
  finishTime?: string;
  traceId: string;
}

/** Real API response from POST /api/v1/tasks */
export interface RealTaskCreated {
  taskId: string;
  taskStatus: string;
  currentStage: string;
  createTime: string;
}

/** Real API response from GET /api/v1/workflows/jobs/{jobId} */
export interface RealWorkflowJob {
  jobId: string;
  taskId: string;
  requestedStage: WorkflowStage | null;
  jobType: string;
  jobStatus: string;
  requestedBy: string;
  startTime: string;
  finishTime?: string;
  errorCode?: string;
  errorMessage?: string;
  traceId: string;
  createTime?: string;
  updateTime?: string;
}

/** Real API response from GET /api/v1/workspaces/{taskId} */
export interface RealWorkspace {
  taskId?: string;
  taskStatus?: string;
  currentStage?: string;
  task?: {
    taskId?: string;
    rawText?: string;
    taskStatus?: string;
    currentStage?: string;
    createTime?: string;
    updateTime?: string;
    createdBy?: string;
    [key: string]: unknown;
  };
  currentIntentVersion?: number;
  currentPlanVersion?: number;
  currentConfigVersion?: number;
  currentExecutionVersion?: number;
  currentValidationVersion?: number;
  currentRepairVersion?: number;
  currentArtifactRefs?: Record<string, string>;
  workspaceStatus?: string;
  [key: string]: unknown;
}

export interface RealWorkspaceEvent {
  eventId?: string;
  taskId?: string;
  eventType?: string;
  stage?: WorkflowStage | string;
  eventTime?: string;
  severity?: string;
  title?: string;
  message?: string;
  relatedArtifactId?: string;
  relatedRecordId?: string;
  traceId?: string;
  payloadSummary?: string;
  [key: string]: unknown;
}

export interface RealWorkspaceChange {
  changeId?: string;
  taskId?: string;
  stage?: WorkflowStage | string;
  changeType?: string;
  changeTime?: string;
  title?: string;
  summary?: string;
  traceId?: string;
  [key: string]: unknown;
}

export interface RealArtifactSummary {
  artifactId: string;
  taskId?: string;
  artifactType?: string;
  version?: number;
  stage?: WorkflowStage | string;
  status?: string;
  payloadType?: string;
  payloadSummary?: string;
  createTime?: string;
  createdBy?: string;
  traceRefs?: string[];
  [key: string]: unknown;
}

export interface RealExecutionReport {
  [key: string]: unknown;
}

export interface RealValidationReport {
  items?: RealValidationItem[];
  [key: string]: unknown;
}

export interface RealValidationItem {
  [key: string]: unknown;
}

export interface RealRepairPlan {
  actions?: Array<{ actionId?: string; [key: string]: unknown }>;
  [key: string]: unknown;
}

export type RealViewStatus = "READY" | "PARTIAL" | "NOT_READY" | "FAILED" | string;

export interface RealViewReadiness {
  status?: RealViewStatus;
  ready: boolean;
  reasonCode?: string;
  message?: string;
  missingArtifacts?: string[];
}

export interface RealWorkspaceSummaryView {
  taskId: string;
  taskStatus?: string;
  currentStage?: string;
  workspaceStatus?: string;
  currentArtifactRefs?: Record<string, string>;
  latestJob?: RealWorkflowJob;
  stageCards?: RealStageCard[];
  currentStageSummary?: string;
  readiness?: RealViewReadiness;
  missingArtifacts?: string[];
  errors?: string[];
}

export interface RealStageCard {
  stage: WorkflowStage | string;
  title?: string;
  agentName?: string;
  status?: RealViewStatus;
  artifactType?: string;
  artifactId?: string;
  artifactVersion?: number;
  summary?: string;
  errorCode?: string;
  errorMessage?: string;
  updateTime?: string;
}

export interface RealWorkflowTraceView {
  taskId: string;
  status?: RealViewStatus;
  ready: boolean;
  reasonCode?: string;
  message?: string;
  currentStage?: WorkflowStage | string;
  jobStatus?: string;
  missingArtifacts?: string[];
  nodes?: RealTraceNode[];
  edges?: RealTraceEdge[];
  events?: RealTraceEvent[];
  errors?: string[];
}

export interface RealTraceNode {
  id: string;
  stage?: WorkflowStage | string;
  label?: string;
  agentName?: string;
  status?: RealViewStatus;
  artifactType?: string;
  artifactId?: string;
  errorCode?: string;
  errorMessage?: string;
}

export interface RealTraceEdge {
  from: string;
  to: string;
  status?: RealViewStatus;
  label?: string;
}

export interface RealTraceEvent {
  eventId?: string;
  eventType?: string;
  stage?: WorkflowStage | string;
  severity?: string;
  title?: string;
  message?: string;
  eventTime?: string;
  relatedArtifactId?: string;
  relatedRecordId?: string;
  traceId?: string;
}

export interface RealTopologyView {
  taskId: string;
  status?: RealViewStatus;
  ready: boolean;
  sourceArtifactType?: string;
  sourceArtifactId?: string;
  sourceStage?: WorkflowStage | string;
  devices?: RealTopologyDevice[];
  links?: RealTopologyLink[];
  policies?: string[];
  annotations?: string[];
  reasonCode?: string;
  message?: string;
}

export interface RealTopologyDevice {
  id: string;
  name?: string;
  role?: string;
  zone?: string;
  ip?: string;
  status?: string;
  vendor?: string;
  deviceType?: string;
  x?: number;
  y?: number;
}

export interface RealTopologyLink {
  id?: string;
  from?: string;
  to?: string;
  label?: string;
  status?: string;
  policy?: string;
  evidence?: string;
  sourcePort?: string;
  targetPort?: string;
}

export interface RealConfigBlocksView {
  taskId: string;
  status?: RealViewStatus;
  ready: boolean;
  sourceArtifactId?: string;
  configVersion?: number;
  devices?: RealConfigDeviceView[];
  commandBlocks?: RealCommandBlockView[];
  warnings?: string[];
  reasonCode?: string;
  message?: string;
}

export interface RealConfigDeviceView {
  deviceId?: string;
  deviceName?: string;
  vendor?: string;
  model?: string;
  status?: string;
  commands?: string[];
  rollbackCommands?: string[];
  summary?: string;
  artifactId?: string;
  traceRefs?: unknown;
}

export interface RealCommandBlockView {
  deviceId?: string;
  deviceName?: string;
  blockId?: string;
  title?: string;
  blockType?: string;
  order?: number;
  commands?: string[];
  rollbackCommands?: string[];
  summary?: string;
  artifactId?: string;
  traceRefs?: unknown;
}

export interface RealExecutionLogsView {
  taskId: string;
  status?: RealViewStatus;
  ready: boolean;
  source?: string;
  lines?: RealExecutionLogLine[];
  events?: RealTraceEvent[];
  reasonCode?: string;
  message?: string;
}

export interface RealExecutionLogLine {
  time?: string;
  level?: string;
  stage?: string;
  source?: string;
  message?: string;
  relatedRecordId?: string;
  traceId?: string;
}

export interface TelemetryEvent {
  eventId: string;
  eventType: string;
  stage: WorkflowStage;
  severity: "INFO" | "WARN" | "ERROR";
  title: string;
  message: string;
  eventTime: string;
}

export interface StageSummary {
  stage: WorkflowStage;
  agentName: string;
  artifactName: string;
  version: number;
  summary: string;
  commandDigest: string[];
}

export interface TopologyDevice {
  id: string;
  name: string;
  kind: "router" | "switch" | "service";
  role: string;
  model: string;
  status: "UP" | "DOWN" | "ANALYZING";
  x: number;
  y: number;
}

export interface TopologyLink {
  from: string;
  to: string;
  status: "ACTIVE" | "BLOCKED" | "WARN";
}

export interface DeviceConfig {
  deviceId: string;
  title: string;
  subtitle: string;
  commands: string[];
}

export interface DemoTask {
  task: TaskSummaryResponse;
  latestJob: WorkflowJob;
  elapsedSeconds: number;
  telemetry: TelemetryEvent[];
  stageSummaries: StageSummary[];
  topology: {
    devices: TopologyDevice[];
    links: TopologyLink[];
  };
  deviceConfigs: DeviceConfig[];
}
