/**
 * Real API client for MAC-TAV backend.
 *
 * <p>All methods communicate directly with the backend at baseUrl (from
 * useApiModeStore). Every failure throws ApiError — there is no mock fallback
 * inside this module.</p>
 */
import { useApiModeStore } from "@/stores/apiModeStore";
import type {
  PageResult,
  RealArtifactSummary,
  RealExecutionReport,
  RealExecutionLogsView,
  RealConfigBlocksView,
  RealRepairPlan,
  RealTaskCreated,
  RealTopologyView,
  RealValidationItem,
  RealValidationReport,
  RealWorkflowTraceView,
  RealWorkspaceEvent,
  RealWorkspaceChange,
  RealWorkspaceSummaryView,
  RealWorkflowJob,
  RealWorkspace,
  WorkflowStage,
} from "@/api/types";

/** Structured error thrown by every failing real API call. */
export class ApiError extends Error {
  method: string;
  path: string;
  status: number | null;
  errorCode?: string;

  constructor(
    method: string,
    path: string,
    status: number | null,
    errorCode?: string,
    message?: string,
  ) {
    const label =
      message ?? errorCode ?? `HTTP ${status ?? "network error"}`;
    super(`[${method} ${path}] ${label}`);
    this.name = "ApiError";
    this.method = method;
    this.path = path;
    this.status = status;
    this.errorCode = errorCode;
  }
}

interface RawApiEnvelope<T> {
  success: boolean;
  code?: number;
  errorCode?: string;
  message?: string;
  data: T;
  timestamp?: string;
}

async function fetchJson<T>(method: string, path: string, body?: unknown): Promise<T> {
  const rawBaseUrl = useApiModeStore().baseUrl.trim();
  if (rawBaseUrl === "" && !import.meta.env.DEV) {
    throw new ApiError(
      method,
      path,
      null,
      'REAL_API_BASE_URL_MISSING',
      'Real API base URL not configured. Set VITE_API_BASE_URL in mac-tav-frontend/.env.local'
    );
  }
  const url = rawBaseUrl === "" ? path : `${rawBaseUrl.replace(/\/$/, '')}${path}`;
  let response: Response;
  try {
    response = await fetch(url, {
      method,
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (err: unknown) {
    const netMsg = err instanceof Error ? err.message : String(err);
    throw new ApiError(method, path, null, undefined, `Network error: ${netMsg}`);
  }

  let envelope: RawApiEnvelope<T>;
  try {
    envelope = (await response.json()) as RawApiEnvelope<T>;
  } catch {
    const contentType = response.headers.get('content-type') ?? 'unknown';
    let detail = `Unparseable response (HTTP ${response.status}). Request URL: ${url}`;
    if (!contentType.includes('application/json')) {
      detail += ` Response is not JSON (got ${contentType}). ${
        rawBaseUrl === "" && import.meta.env.DEV
          ? "Using Vite dev proxy for /api/**. Check vite.config.ts proxy target and backend status."
          : "Request may have hit the frontend dev server instead of the backend. Check VITE_API_BASE_URL."
      }`;
    } else {
      detail += ` content-type=${contentType}`;
    }
    throw new ApiError(method, path, response.status, undefined, detail);
  }

  if (!response.ok) {
    throw new ApiError(
      method,
      path,
      response.status,
      envelope.errorCode,
      envelope.message ?? `HTTP ${response.status}`,
    );
  }

  if (!envelope.success) {
    throw new ApiError(
      method,
      path,
      response.status,
      envelope.errorCode,
      envelope.message ?? "API returned success=false",
    );
  }

  return envelope.data;
}

function withQuery(path: string, params: Record<string, string | number | undefined | null> = {}): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, String(value));
    }
  });
  const query = search.toString();
  return query ? `${path}?${query}` : path;
}

export interface WorkflowJobSubmitResponse {
  jobId: string;
}

/**
 * Raw real API client with four core methods for the minimal real call chain.
 *
 * <p>No fallback to mock data — callers must handle ApiError explicitly.</p>
 */
export const realClient = {
  /**
   * Create a new task and workspace.
   * POST /api/v1/tasks
   */
  async createTask(
    rawText: string,
    targetEnvironmentHint?: string,
    createdBy?: string,
  ): Promise<RealTaskCreated> {
    const data = await fetchJson<RealTaskCreated>("POST", "/api/v1/tasks", {
      rawText,
      targetEnvironmentHint,
      createdBy,
    });
    return data;
  },

  /**
   * Start full workflow for an existing task.
   * POST /api/v1/workflows/{taskId}/start
   */
  async startWorkflow(taskId: string): Promise<WorkflowJobSubmitResponse> {
    return fetchJson<WorkflowJobSubmitResponse>(
      "POST",
      `/api/v1/workflows/${taskId}/start`,
    );
  },

  async getTaskJobs(taskId: string): Promise<RealWorkflowJob[]> {
    return fetchJson<RealWorkflowJob[]>("GET", `/api/v1/tasks/${taskId}/jobs`);
  },

  async runIntentStage(taskId: string): Promise<WorkflowJobSubmitResponse> {
    return fetchJson<WorkflowJobSubmitResponse>("POST", `/api/v1/workflows/${taskId}/run`);
  },

  async runPlanningStage(taskId: string): Promise<WorkflowJobSubmitResponse> {
    return fetchJson<WorkflowJobSubmitResponse>("POST", `/api/v1/workflows/${taskId}/plan`);
  },

  async runConfigurationStage(taskId: string): Promise<WorkflowJobSubmitResponse> {
    return fetchJson<WorkflowJobSubmitResponse>("POST", `/api/v1/workflows/${taskId}/config`);
  },

  async rerunStage(taskId: string, stage: WorkflowStage): Promise<WorkflowJobSubmitResponse> {
    return fetchJson<WorkflowJobSubmitResponse>("POST", `/api/v1/workflows/${taskId}/rerun/${stage}`);
  },

  async continueFromStage(taskId: string, stage: WorkflowStage): Promise<WorkflowJobSubmitResponse> {
    return fetchJson<WorkflowJobSubmitResponse>("POST", `/api/v1/workflows/${taskId}/continue-from/${stage}`);
  },

  /**
   * Poll workflow job status.
   * GET /api/v1/workflows/jobs/{jobId}
   */
  async getWorkflowJob(jobId: string): Promise<RealWorkflowJob> {
    return fetchJson<RealWorkflowJob>(
      "GET",
      `/api/v1/workflows/jobs/${jobId}`,
    );
  },

  /**
   * Fetch current workspace view.
   * GET /api/v1/workspaces/{taskId}
   */
  async getWorkspace(taskId: string): Promise<RealWorkspace> {
    return fetchJson<RealWorkspace>(
      "GET",
      `/api/v1/workspaces/${taskId}`,
    );
  },

  async getWorkspaceSummary(taskId: string): Promise<RealWorkspaceSummaryView> {
    return fetchJson<RealWorkspaceSummaryView>("GET", `/api/v1/workspaces/${taskId}/summary`);
  },

  async getWorkflowTrace(taskId: string): Promise<RealWorkflowTraceView> {
    return fetchJson<RealWorkflowTraceView>("GET", `/api/v1/views/${taskId}/trace`);
  },

  async getTopologyView(taskId: string): Promise<RealTopologyView> {
    return fetchJson<RealTopologyView>("GET", `/api/v1/views/${taskId}/topology`);
  },

  async getConfigBlocks(taskId: string): Promise<RealConfigBlocksView> {
    return fetchJson<RealConfigBlocksView>("GET", `/api/v1/views/${taskId}/config-blocks`);
  },

  async getExecutionLogs(taskId: string): Promise<RealExecutionLogsView> {
    return fetchJson<RealExecutionLogsView>("GET", `/api/v1/executions/${taskId}/logs`);
  },

  async getWorkspaceTimeline(taskId: string, page = 1, size = 20): Promise<PageResult<RealWorkspaceEvent>> {
    return fetchJson<PageResult<RealWorkspaceEvent>>(
      "GET",
      withQuery(`/api/v1/workspaces/${taskId}/timeline`, { page, size }),
    );
  },

  async getWorkspaceChanges(taskId: string, page = 1, size = 20): Promise<PageResult<RealWorkspaceChange>> {
    return fetchJson<PageResult<RealWorkspaceChange>>(
      "GET",
      withQuery(`/api/v1/workspaces/${taskId}/changes`, { page, size }),
    );
  },

  async listArtifacts(
    taskId: string,
    params: { artifactType?: string; stage?: WorkflowStage; page?: number; size?: number } = {},
  ): Promise<PageResult<RealArtifactSummary>> {
    return fetchJson<PageResult<RealArtifactSummary>>(
      "GET",
      withQuery(`/api/v1/artifacts/${taskId}`, {
        artifactType: params.artifactType,
        stage: params.stage,
        page: params.page ?? 1,
        size: params.size ?? 20,
      }),
    );
  },

  async getEventHistory(taskId: string, page = 1, size = 20): Promise<PageResult<RealWorkspaceEvent>> {
    return fetchJson<PageResult<RealWorkspaceEvent>>(
      "GET",
      withQuery(`/api/v1/events/${taskId}/history`, { page, size }),
    );
  },

  async runExecution(taskId: string): Promise<WorkflowJobSubmitResponse> {
    return fetchJson<WorkflowJobSubmitResponse>("POST", `/api/v1/executions/${taskId}/run`);
  },

  async getExecution(taskId: string): Promise<RealExecutionReport> {
    return fetchJson<RealExecutionReport>("GET", `/api/v1/executions/${taskId}`);
  },

  async runValidation(taskId: string): Promise<WorkflowJobSubmitResponse> {
    return fetchJson<WorkflowJobSubmitResponse>("POST", `/api/v1/validations/${taskId}/run`);
  },

  async getValidation(taskId: string): Promise<RealValidationReport> {
    return fetchJson<RealValidationReport>("GET", `/api/v1/validations/${taskId}`);
  },

  async getValidationItems(taskId: string): Promise<RealValidationItem[]> {
    return fetchJson<RealValidationItem[]>("GET", `/api/v1/validations/${taskId}/items`);
  },

  async analyzeRepair(taskId: string): Promise<WorkflowJobSubmitResponse> {
    return fetchJson<WorkflowJobSubmitResponse>("POST", `/api/v1/repairs/${taskId}/analyze`);
  },

  async getRepair(taskId: string): Promise<RealRepairPlan> {
    return fetchJson<RealRepairPlan>("GET", `/api/v1/repairs/${taskId}`);
  },

  async approveRepairAction(
    taskId: string,
    actionId: string,
    body?: { actor?: string; comment?: string },
  ): Promise<RealRepairPlan> {
    return fetchJson<RealRepairPlan>("POST", `/api/v1/repairs/${taskId}/actions/${actionId}/approve`, body);
  },

  async rejectRepairAction(
    taskId: string,
    actionId: string,
    body?: { actor?: string; comment?: string },
  ): Promise<RealRepairPlan> {
    return fetchJson<RealRepairPlan>("POST", `/api/v1/repairs/${taskId}/actions/${actionId}/reject`, body);
  },

  async applyRepairAction(taskId: string, actionId: string): Promise<WorkflowJobSubmitResponse> {
    return fetchJson<WorkflowJobSubmitResponse>("POST", `/api/v1/repairs/${taskId}/actions/${actionId}/apply`);
  },
};
