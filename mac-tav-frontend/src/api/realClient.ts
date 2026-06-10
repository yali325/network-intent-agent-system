/**
 * Real API client for MAC-TAV backend.
 *
 * <p>All methods communicate directly with the backend at baseUrl (from
 * useApiModeStore). Every failure throws ApiError — there is no mock fallback
 * inside this module.</p>
 */
import { useApiModeStore } from "@/stores/apiModeStore";
import type {
  RealTaskCreated,
  RealWorkflowJob,
  RealWorkspace,
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
  const { baseUrl } = useApiModeStore();
  const url = `${baseUrl}${path}`;
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
    throw new ApiError(
      method,
      path,
      response.status,
      undefined,
      `Unparseable response (HTTP ${response.status})`,
    );
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
  async startWorkflow(taskId: string): Promise<{ jobId: string }> {
    return fetchJson<{ jobId: string }>(
      "POST",
      `/api/v1/workflows/${taskId}/start`,
    );
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
};
