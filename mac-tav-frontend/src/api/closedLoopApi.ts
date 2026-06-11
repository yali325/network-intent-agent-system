import { realClient } from "@/api/realClient";
import { useApiModeStore } from "@/stores/apiModeStore";
import type { WorkflowJobSubmitResponse } from "@/api/futureContracts";

export const closedLoopApi = {
  async runValidation(taskId: string): Promise<WorkflowJobSubmitResponse> {
    const { isMock } = useApiModeStore();
    if (isMock) return { jobId: `job-mock-validation-${Date.now().toString(36)}` };
    return realClient.runValidation(taskId);
  },

  async analyzeRepair(taskId: string): Promise<WorkflowJobSubmitResponse> {
    const { isMock } = useApiModeStore();
    if (isMock) return { jobId: `job-mock-repair-analyze-${Date.now().toString(36)}` };
    return realClient.analyzeRepair(taskId);
  },

  async approveRepairAction(taskId: string, actionId: string): Promise<void> {
    const { isMock } = useApiModeStore();
    if (isMock) return;
    await realClient.approveRepairAction(taskId, actionId);
  },

  async applyRepairAction(taskId: string, actionId: string): Promise<WorkflowJobSubmitResponse> {
    const { isMock } = useApiModeStore();
    if (isMock) return { jobId: `job-mock-repair-apply-${Date.now().toString(36)}` };
    return realClient.applyRepairAction(taskId, actionId);
  },
};
