import { apiBaseUrl, apiMode } from '@/api/config';
import type { WorkflowJobSubmitResponse } from '@/api/futureContracts';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  errorCode?: string;
}

async function postJob(path: string): Promise<WorkflowJobSubmitResponse> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    method: 'POST',
    headers: { Accept: 'application/json' }
  });
  if (!response.ok) {
    throw new Error(`API request failed: ${response.status}`);
  }
  const body = (await response.json()) as ApiResponse<WorkflowJobSubmitResponse>;
  if (!body.success) {
    throw new Error(body.message ?? body.errorCode ?? 'API request failed');
  }
  return body.data;
}

export const closedLoopApi = {
  async runValidation(taskId: string): Promise<WorkflowJobSubmitResponse> {
    if (apiMode === 'mock') return { jobId: `job-mock-validation-${Date.now().toString(36)}` };
    return postJob(`/api/v1/validations/${taskId}/run`);
  },

  async analyzeRepair(taskId: string): Promise<WorkflowJobSubmitResponse> {
    if (apiMode === 'mock') return { jobId: `job-mock-repair-analyze-${Date.now().toString(36)}` };
    return postJob(`/api/v1/repairs/${taskId}/analyze`);
  },

  async approveRepairAction(taskId: string, actionId: string): Promise<void> {
    if (apiMode === 'mock') return;
    await postJob(`/api/v1/repairs/${taskId}/actions/${actionId}/approve`);
  },

  async applyRepairAction(taskId: string, actionId: string): Promise<WorkflowJobSubmitResponse> {
    if (apiMode === 'mock') return { jobId: `job-mock-repair-apply-${Date.now().toString(36)}` };
    return postJob(`/api/v1/repairs/${taskId}/actions/${actionId}/apply`);
  }
};
