export type WorkflowStage = "INTENT" | "PLANNING" | "CONFIGURATION" | "EXECUTION" | "VERIFICATION" | "HEALING";

export type WorkflowJobStatus = "PENDING" | "RUNNING" | "SUCCESS" | "FAILED" | "CANCELLED" | "INTERRUPTED";

export type WorkflowJobType = "FULL_WORKFLOW" | "RUN_STAGE" | "RERUN_STAGE" | "CONTINUE_FROM_STAGE" | "REPAIR_ANALYZE" | "REPAIR_APPLY";

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
  taskId: string;
  taskStatus: string;
  currentStage: string;
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
