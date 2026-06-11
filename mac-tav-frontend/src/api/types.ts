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
