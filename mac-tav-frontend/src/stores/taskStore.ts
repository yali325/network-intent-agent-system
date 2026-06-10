import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { closedLoopApi } from "@/api/closedLoopApi";
import { createMockTask, getMockTask } from "@/api/mockAdapter";
import { realClient, ApiError } from "@/api/realClient";
import { useApiModeStore } from "@/stores/apiModeStore";
import type { MissionView, MockRepairPhase, RepairActionDemo, RepairPlanDemo, ValidationAssertionDemo } from "@/api/futureContracts";
import type { DemoTask, RealTaskCreated, RealWorkflowJob, RealWorkspace, WorkflowStage } from "@/api/types";
import { repairPlanFixture } from "@/fixtures/futureRepairDemo";
import { validationAssertionsFixture } from "@/fixtures/futureValidationDemo";

export const useTaskStore = defineStore("task", () => {
  const activeTask = ref<DemoTask | null>(null);
  const selectedStage = ref<WorkflowStage>("CONFIGURATION");
  const draftIntent = ref("");
  const appendedIntents = ref<Array<{ id: string; text: string; createTime: string }>>([]);
  const activeView = ref<MissionView>("control");
  const validationAssertions = ref<ValidationAssertionDemo[]>(validationAssertionsFixture.map((item) => ({ ...item })));
  const repairPlan = ref<RepairPlanDemo>(cloneRepairPlan());
  const selectedRepairActionId = ref(repairPlan.value.actions[0]?.actionId ?? "");
  const repairPhase = ref<MockRepairPhase>("idle");
  const lastSubmittedJobId = ref<string | null>(null);

  /* ── Real-mode state ── */
  const realTaskId = ref<string | null>(null);
  const realJobId = ref<string | null>(null);
  const realJob = ref<RealWorkflowJob | null>(null);
  const realWorkspace = ref<RealWorkspace | null>(null);
  const realError = ref<string | null>(null);

  const currentSummary = computed(() => activeTask.value?.stageSummaries.find((item) => item.stage === selectedStage.value) ?? null);
  const selectedRepairAction = computed(() => repairPlan.value.actions.find((action) => action.actionId === selectedRepairActionId.value) ?? repairPlan.value.actions[0]);
  const failedAssertion = computed(() => validationAssertions.value.find((item) => item.status === "FAILED") ?? null);
  const topologyPolicyState = computed<"conflict" | "approved" | "repaired">(() => {
    if (repairPhase.value === "applied") return "repaired";
    if (repairPhase.value === "approved" || repairPhase.value === "applying") return "approved";
    return "conflict";
  });
  const topologyHealingState = computed<"normal" | "failed" | "healing">(() => {
    if (repairPhase.value === "applying") return "healing";
    return failedAssertion.value && topologyPolicyState.value !== "repaired" ? "failed" : "normal";
  });

  async function createTask(rawText: string): Promise<DemoTask | RealTaskCreated> {
    draftIntent.value = rawText;
    const { isReal } = useApiModeStore();

    if (isReal) {
      realError.value = null;
      try {
        const created = await realClient.createTask(rawText);
        realTaskId.value = created.taskId;
        const submitResp = await realClient.startWorkflow(created.taskId);
        realJobId.value = submitResp.jobId;
        appendedIntents.value = [];
        resetClosedLoop();
        return created;
      } catch (err: unknown) {
        const msg = err instanceof ApiError ? err.message : String(err);
        realError.value = msg;
        throw err;
      }
    }

    /* mock path (unchanged) */
    activeTask.value = createMockTask(rawText);
    appendedIntents.value = [];
    resetClosedLoop();
    selectedStage.value = activeTask.value.task.currentStage;
    return activeTask.value;
  }

  function loadTask(taskId: string): DemoTask | null {
    const { isReal } = useApiModeStore();
    if (isReal) {
      realTaskId.value = taskId;
      realJob.value = null;
      realWorkspace.value = null;
      realError.value = null;
      activeTask.value = null;
      return null;
    }
    activeTask.value = getMockTask(taskId);
    selectedStage.value = activeTask.value.task.currentStage;
    return activeTask.value;
  }

  async function pollJob(jobId?: string): Promise<RealWorkflowJob> {
    const jid = jobId ?? realJobId.value;
    if (!jid) throw new Error("No jobId available to poll");
    try {
      const job = await realClient.getWorkflowJob(jid);
      realJob.value = job;
      if (job.jobStatus === "FAILED") {
        realError.value = job.errorMessage ?? "Job failed";
      }
      return job;
    } catch (err: unknown) {
      const msg = err instanceof ApiError ? err.message : String(err);
      realError.value = msg;
      throw err;
    }
  }

  async function fetchWorkspace(taskId?: string): Promise<RealWorkspace> {
    const tid = taskId ?? realTaskId.value;
    if (!tid) throw new Error("No taskId available for workspace");
    try {
      const ws = await realClient.getWorkspace(tid);
      realWorkspace.value = ws;
      return ws;
    } catch (err: unknown) {
      const msg = err instanceof ApiError ? err.message : String(err);
      realError.value = msg;
      throw err;
    }
  }

  function prepareNewIntent(): void {
    draftIntent.value = "";
  }

  async function appendIntent(text: string): Promise<void> {
    if (!activeTask.value) return;
    const createTime = new Date().toISOString();
    appendedIntents.value.unshift({
      id: `append-${Date.now().toString(36)}`,
      text,
      createTime,
    });
    activeTask.value.task.rawText = `${activeTask.value.task.rawText}\n追加指令：${text}`;
    activeTask.value.telemetry.unshift({
      eventId: `evt-append-${Date.now().toString(36)}`,
      eventType: "intent.appended",
      stage: "INTENT",
      severity: "INFO",
      title: "追加意图已进入多轮队列",
      message: text,
      eventTime: createTime,
    });
    activeTask.value.latestJob.jobStatus = "RUNNING";
    activeTask.value.task.currentStage = "CONFIGURATION";
    selectedStage.value = "CONFIGURATION";
  }

  function resetClosedLoop(): void {
    activeView.value = "control";
    validationAssertions.value = validationAssertionsFixture.map((item) => ({ ...item }));
    repairPlan.value = cloneRepairPlan();
    selectedRepairActionId.value = repairPlan.value.actions[0]?.actionId ?? "";
    repairPhase.value = "idle";
    lastSubmittedJobId.value = null;
  }

  async function runValidation(): Promise<string> {
    if (!activeTask.value) return "";
    const job = await closedLoopApi.runValidation(activeTask.value.task.taskId);
    lastSubmittedJobId.value = job.jobId;
    activeView.value = "validation";
    return job.jobId;
  }

  async function analyzeRepair(): Promise<string> {
    if (!activeTask.value) return "";
    const job = await closedLoopApi.analyzeRepair(activeTask.value.task.taskId);
    lastSubmittedJobId.value = job.jobId;
    repairPhase.value = "analysis_ready";
    activeView.value = "repair";
    return job.jobId;
  }

  function selectRepairAction(actionId: string): void {
    selectedRepairActionId.value = actionId;
  }

  async function approveSelectedRepairAction(): Promise<void> {
    if (!activeTask.value || !selectedRepairAction.value) return;
    await closedLoopApi.approveRepairAction(activeTask.value.task.taskId, selectedRepairAction.value.actionId);
    updateRepairActionStatus(selectedRepairAction.value.actionId, "APPROVED");
    repairPhase.value = "approved";
  }

  async function applySelectedRepairAction(): Promise<string> {
    if (!activeTask.value || !selectedRepairAction.value) return "";
    repairPhase.value = "applying";
    const actionId = selectedRepairAction.value.actionId;
    const job = await closedLoopApi.applyRepairAction(activeTask.value.task.taskId, actionId);
    lastSubmittedJobId.value = job.jobId;
    const { isReal } = useApiModeStore();
    if (isReal) {
      repairPhase.value = "approved";
      return job.jobId;
    }
    window.setTimeout(() => {
      updateRepairActionStatus(actionId, "APPLIED");
      repairPhase.value = "applied";
      activeView.value = "control";
      const failed = validationAssertions.value.find((item) => item.id === "assert-guest-prod");
      if (failed) {
        failed.actual = "BLOCKED";
        failed.status = "PASSED";
        failed.message = "mock/demo：修复动作完成后，访客区到服务器区已恢复阻断性。";
      }
    }, 1500);
    return job.jobId;
  }

  function updateRepairActionStatus(actionId: string, status: RepairActionDemo["status"]): void {
    repairPlan.value.actions = repairPlan.value.actions.map((action) => (action.actionId === actionId ? { ...action, status } : action));
  }

  return {
    activeTask,
    selectedStage,
    draftIntent,
    appendedIntents,
    activeView,
    validationAssertions,
    repairPlan,
    selectedRepairActionId,
    selectedRepairAction,
    repairPhase,
    lastSubmittedJobId,
    realTaskId,
    realJobId,
    realJob,
    realWorkspace,
    realError,
    topologyPolicyState,
    topologyHealingState,
    currentSummary,
    failedAssertion,
    createTask,
    loadTask,
    pollJob,
    fetchWorkspace,
    prepareNewIntent,
    appendIntent,
    resetClosedLoop,
    runValidation,
    analyzeRepair,
    selectRepairAction,
    approveSelectedRepairAction,
    applySelectedRepairAction,
  };
});

function cloneRepairPlan(): RepairPlanDemo {
  return {
    rca: { ...repairPlanFixture.rca, evidenceRefs: [...repairPlanFixture.rca.evidenceRefs] },
    actions: repairPlanFixture.actions.map((action) => ({
      ...action,
      traceRefs: [...action.traceRefs],
      candidateSnippet: action.candidateSnippet ? [...action.candidateSnippet] : undefined,
    })),
  };
}
