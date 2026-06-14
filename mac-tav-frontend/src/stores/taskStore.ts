import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { closedLoopApi } from "@/api/closedLoopApi";
import { createMockTask, getMockTask } from "@/api/mockAdapter";
import { realClient, ApiError } from "@/api/realClient";
import { useApiModeStore } from "@/stores/apiModeStore";
import type { MissionView, MockRepairPhase, RepairActionDemo, RepairPlanDemo, ValidationAssertionDemo } from "@/api/futureContracts";
import type {
  DemoTask,
  PageResult,
  RealArtifactSummary,
  RealConfigBlocksView,
  RealExecutionReport,
  RealExecutionLogsView,
  RealRepairPlan,
  RealTaskCreated,
  RealTopologyView,
  RealValidationItem,
  RealValidationReport,
  RealWorkflowTraceView,
  RealWorkspace,
  RealWorkspaceEvent,
  RealWorkspaceSummaryView,
  RealWorkflowJob,
  WorkflowStage,
} from "@/api/types";
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
  const realTimeline = ref<PageResult<RealWorkspaceEvent> | null>(null);
  const realEvents = ref<PageResult<RealWorkspaceEvent> | null>(null);
  const realArtifacts = ref<PageResult<RealArtifactSummary> | null>(null);
  const realTaskJobs = ref<RealWorkflowJob[]>([]);
  const realExecution = ref<RealExecutionReport | null>(null);
  const realValidation = ref<RealValidationReport | null>(null);
  const realValidationItems = ref<RealValidationItem[]>([]);
  const realRepairPlan = ref<RealRepairPlan | null>(null);
  const realWorkspaceSummary = ref<RealWorkspaceSummaryView | null>(null);
  const realWorkflowTrace = ref<RealWorkflowTraceView | null>(null);
  const realTopology = ref<RealTopologyView | null>(null);
  const realConfigBlocks = ref<RealConfigBlocksView | null>(null);
  const realExecutionLogs = ref<RealExecutionLogsView | null>(null);
  const isRealRefreshing = ref(false);
  const lastRealRefreshAt = ref<string | null>(null);
  const realRefreshError = ref<string | null>(null);
  const observedRealTaskId = ref<string | null>(null);
  const observedRealJobId = ref<string | null>(null);
  const realRefreshIntervalMs = ref(1800);
  const realViewPanels = ref<Record<string, { loading: boolean; status: string; error: string | null }>>({
    summary: { loading: false, status: "idle", error: null },
    trace: { loading: false, status: "idle", error: null },
    topology: { loading: false, status: "idle", error: null },
    configBlocks: { loading: false, status: "idle", error: null },
    executionLogs: { loading: false, status: "idle", error: null },
  });
  const realNotImplemented = ref([
    "GET /api/v1/views/{taskId}/config-blocks/{blockId}/trace",
    "GET /api/v1/views/{taskId}/validation-evidence-matrix",
    "GET /api/v1/views/{taskId}/repair-simulation",
  ]);

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

  let realRefreshTimer: number | null = null;
  let realRefreshInFlight = false;

  async function createTask(rawText: string): Promise<DemoTask | RealTaskCreated> {
    draftIntent.value = rawText;
    const { isReal } = useApiModeStore();

    if (isReal) {
      activeTask.value = null;
      stopRealMissionRefresh();
      resetRealData();
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

  async function loadTask(taskId: string): Promise<DemoTask | null> {
    const { isReal } = useApiModeStore();
    if (isReal) {
      realTaskId.value = taskId;
      realJob.value = null;
      realWorkspace.value = null;
      realError.value = null;
      resetRealData();
      activeTask.value = null;
      await refreshRealMissionOnce(taskId);
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

  async function fetchTaskJobs(taskId?: string): Promise<RealWorkflowJob[]> {
    const tid = taskId ?? realTaskId.value;
    if (!tid) throw new Error("No taskId available for job history");
    try {
      const jobs = await realClient.getTaskJobs(tid);
      realTaskJobs.value = jobs;
      const latestJob = pickLatestJob(jobs);
      if (!realJob.value && latestJob) {
        realJob.value = latestJob;
        realJobId.value = latestJob.jobId;
      }
      return jobs;
    } catch (err: unknown) {
      const msg = err instanceof ApiError ? err.message : String(err);
      realError.value = msg;
      throw err;
    }
  }

  async function refreshRealMissionOnce(
    taskId?: string,
    options: { jobId?: string } = {},
  ): Promise<void> {
    const tid = taskId ?? realTaskId.value;
    if (!tid) throw new Error("No taskId available for real mission refresh");
    if (realRefreshInFlight) return;

    realRefreshInFlight = true;
    isRealRefreshing.value = true;
    realRefreshError.value = null;

    try {
      const requestedJobId = options.jobId ?? realJobId.value ?? observedRealJobId.value ?? undefined;
      const refreshTasks: Array<Promise<unknown>> = [];
      if (requestedJobId) {
        refreshTasks.push(pollJob(requestedJobId));
      }
      refreshTasks.push(fetchWorkspace(tid));
      refreshTasks.push(fetchTaskJobs(tid));
      refreshTasks.push(fetchEventHistory(tid));
      refreshTasks.push(listArtifacts(tid));
      refreshTasks.push(refreshRealMissionView(tid));

      const results = await Promise.allSettled(refreshTasks);
      const firstFailure = results.find((result) => result.status === "rejected");
      if (firstFailure && firstFailure.status === "rejected") {
        const err = firstFailure.reason as unknown;
        realRefreshError.value = err instanceof ApiError ? err.message : err instanceof Error ? err.message : String(err);
      }

      const latestJob = pickLatestJob(realTaskJobs.value);
      if (!realJobId.value && latestJob) {
        realJobId.value = latestJob.jobId;
      }
      if (!realJob.value && latestJob) {
        realJob.value = latestJob;
      }
      lastRealRefreshAt.value = new Date().toISOString();
    } finally {
      isRealRefreshing.value = false;
      realRefreshInFlight = false;
    }
  }

  function startRealMissionRefresh(taskId: string, jobId?: string): void {
    if (!taskId) return;
    const normalizedJobId = jobId ?? realJobId.value ?? undefined;
    if (realRefreshTimer && observedRealTaskId.value === taskId && observedRealJobId.value === (normalizedJobId ?? null)) {
      return;
    }
    stopRealMissionRefresh();
    realTaskId.value = taskId;
    if (normalizedJobId) {
      realJobId.value = normalizedJobId;
    }
    observedRealTaskId.value = taskId;
    observedRealJobId.value = normalizedJobId ?? null;

    void runObservedRefresh(taskId, normalizedJobId);
    realRefreshTimer = window.setInterval(() => {
      void runObservedRefresh(taskId, observedRealJobId.value ?? realJobId.value ?? undefined);
    }, realRefreshIntervalMs.value);
  }

  function stopRealMissionRefresh(): void {
    if (realRefreshTimer) {
      window.clearInterval(realRefreshTimer);
      realRefreshTimer = null;
    }
    observedRealTaskId.value = null;
    observedRealJobId.value = null;
  }

  async function runObservedRefresh(taskId: string, jobId?: string): Promise<void> {
    try {
      await refreshRealMissionOnce(taskId, { jobId });
      const latestJob = realJob.value ?? pickLatestJob(realTaskJobs.value);
      if (latestJob) {
        observedRealJobId.value = latestJob.jobId;
      }
      if (latestJob && isTerminalJobStatus(latestJob.jobStatus)) {
        await refreshRealMissionOnce(taskId, { jobId: latestJob.jobId });
        stopRealMissionRefresh();
      }
    } catch (err: unknown) {
      realRefreshError.value = err instanceof ApiError ? err.message : err instanceof Error ? err.message : String(err);
    }
  }

  async function fetchEventHistory(taskId?: string): Promise<PageResult<RealWorkspaceEvent>> {
    const tid = taskId ?? realTaskId.value;
    if (!tid) throw new Error("No taskId available for event history");
    try {
      const events = await realClient.getEventHistory(tid);
      realEvents.value = events;
      return events;
    } catch (err: unknown) {
      const msg = err instanceof ApiError ? err.message : String(err);
      realError.value = msg;
      throw err;
    }
  }

  async function fetchTimeline(taskId?: string): Promise<PageResult<RealWorkspaceEvent>> {
    const tid = taskId ?? realTaskId.value;
    if (!tid) throw new Error("No taskId available for workspace timeline");
    try {
      const timeline = await realClient.getWorkspaceTimeline(tid);
      realTimeline.value = timeline;
      return timeline;
    } catch (err: unknown) {
      const msg = err instanceof ApiError ? err.message : String(err);
      realError.value = msg;
      throw err;
    }
  }

  async function listArtifacts(taskId?: string): Promise<PageResult<RealArtifactSummary>> {
    const tid = taskId ?? realTaskId.value;
    if (!tid) throw new Error("No taskId available for artifacts");
    try {
      const artifacts = await realClient.listArtifacts(tid);
      realArtifacts.value = artifacts;
      return artifacts;
    } catch (err: unknown) {
      const msg = err instanceof ApiError ? err.message : String(err);
      realError.value = msg;
      throw err;
    }
  }

  async function refreshRealMissionView(taskId?: string): Promise<void> {
    const tid = taskId ?? realTaskId.value;
    if (!tid) throw new Error("No taskId available for real mission view");
    await Promise.allSettled([
      loadRealPanel("summary", () => realClient.getWorkspaceSummary(tid), (data) => {
        realWorkspaceSummary.value = data;
      }),
      loadRealPanel("trace", () => realClient.getWorkflowTrace(tid), (data) => {
        realWorkflowTrace.value = data;
      }),
      loadRealPanel("topology", () => realClient.getTopologyView(tid), (data) => {
        realTopology.value = data;
      }),
      loadRealPanel("configBlocks", () => realClient.getConfigBlocks(tid), (data) => {
        realConfigBlocks.value = data;
      }),
      loadRealPanel("executionLogs", () => realClient.getExecutionLogs(tid), (data) => {
        realExecutionLogs.value = data;
      }),
    ]);
  }

  async function loadRealPanel<T>(key: string, loader: () => Promise<T>, assign: (data: T) => void): Promise<void> {
    realViewPanels.value[key] = { loading: true, status: "loading", error: null };
    try {
      const data = await loader();
      assign(data);
      const maybeStatus = typeof data === "object" && data !== null && "status" in data ? String((data as { status?: unknown }).status ?? "ready") : "ready";
      realViewPanels.value[key] = { loading: false, status: maybeStatus, error: null };
    } catch (err: unknown) {
      const msg = err instanceof ApiError ? err.message : err instanceof Error ? err.message : String(err);
      realViewPanels.value[key] = { loading: false, status: "error", error: msg };
    }
  }

  function resetRealData(): void {
    realJob.value = null;
    realWorkspace.value = null;
    realTimeline.value = null;
    realEvents.value = null;
    realArtifacts.value = null;
    realTaskJobs.value = [];
    realExecution.value = null;
    realValidation.value = null;
    realValidationItems.value = [];
    realRepairPlan.value = null;
    realWorkspaceSummary.value = null;
    realWorkflowTrace.value = null;
    realTopology.value = null;
    realConfigBlocks.value = null;
    realExecutionLogs.value = null;
    isRealRefreshing.value = false;
    lastRealRefreshAt.value = null;
    realRefreshError.value = null;
    Object.keys(realViewPanels.value).forEach((key) => {
      realViewPanels.value[key] = { loading: false, status: "idle", error: null };
    });
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
    realTimeline,
    realEvents,
    realArtifacts,
    realTaskJobs,
    realExecution,
    realValidation,
    realValidationItems,
    realRepairPlan,
    realWorkspaceSummary,
    realWorkflowTrace,
    realTopology,
    realConfigBlocks,
    realExecutionLogs,
    isRealRefreshing,
    lastRealRefreshAt,
    realRefreshError,
    observedRealTaskId,
    observedRealJobId,
    realRefreshIntervalMs,
    realViewPanels,
    realNotImplemented,
    topologyPolicyState,
    topologyHealingState,
    currentSummary,
    failedAssertion,
    createTask,
    loadTask,
    pollJob,
    fetchWorkspace,
    fetchTaskJobs,
    fetchEventHistory,
    fetchTimeline,
    listArtifacts,
    refreshRealMissionView,
    refreshRealMissionOnce,
    startRealMissionRefresh,
    stopRealMissionRefresh,
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

function isTerminalJobStatus(status?: string | null): boolean {
  return ["SUCCESS", "FAILED", "CANCELLED", "INTERRUPTED", "ERROR"].includes(String(status ?? "").toUpperCase());
}

function pickLatestJob(jobs: RealWorkflowJob[]): RealWorkflowJob | null {
  if (!jobs.length) return null;
  return [...jobs].sort((left, right) => jobTime(right) - jobTime(left))[0] ?? null;
}

function jobTime(job: RealWorkflowJob): number {
  const value = job.updateTime ?? job.createTime ?? job.startTime ?? job.finishTime;
  const time = value ? Date.parse(value) : Number.NaN;
  return Number.isNaN(time) ? 0 : time;
}
