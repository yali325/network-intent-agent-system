import type {
  DeviceConfig,
  PageResult,
  RealArtifactSummary,
  RealConfigBlocksView,
  RealExecutionLogsView,
  RealRepairPlan,
  RealTopologyView,
  RealValidationItem,
  RealValidationReport,
  RealViewStatus,
  RealWorkflowJob,
  RealWorkflowTraceView,
  RealWorkspaceEvent,
  RealWorkspaceSummaryView,
  TopologyDevice,
  TopologyLink,
  WorkflowStage,
} from "@/api/types";
import type {
  EvidenceInspectorDemo,
  RcaReportDemo,
  RepairActionDemo,
  RepairPlanDemo,
  ValidationAssertionDemo,
} from "@/api/futureContracts";

export type StageNodeStatus = "done" | "running" | "pending" | "failed" | "not_ready";

export interface StageFlowNodeViewModel {
  key: WorkflowStage;
  title: string;
  caption: string;
  status: StageNodeStatus;
}

export interface TelemetryEventViewModel {
  id: string;
  eventType: string;
  title: string;
  message: string;
}

export interface ArtifactPreviewViewModel {
  agentName: string;
  stage: WorkflowStage;
  artifactName: string;
  versionLabel: string;
  summary: string;
  commandDigest: string[];
  tone: "running" | "ok" | "bad" | "warn" | "pending" | "signal";
}

export interface TopologyBoardViewModel {
  devices: TopologyDevice[];
  links: TopologyLink[];
  configs: DeviceConfig[];
  statusLabel: string;
  emptyMessage: string;
}

const stageMeta: Array<{ key: WorkflowStage; title: string; caption: string; artifactType: string }> = [
  { key: "INTENT", title: "意图", caption: "IntentAgent", artifactType: "NETWORK_INTENT" },
  { key: "PLANNING", title: "规划", caption: "PlanningAgent", artifactType: "NETWORK_PLAN" },
  { key: "CONFIGURATION", title: "配置", caption: "ConfigurationAgent", artifactType: "CONFIG_SET" },
  { key: "EXECUTION", title: "执行", caption: "ExecutionAdapter", artifactType: "EXECUTION_REPORT" },
  { key: "VERIFICATION", title: "验证", caption: "VerificationAgent", artifactType: "VALIDATION_REPORT" },
  { key: "HEALING", title: "修复", caption: "HealingAgent", artifactType: "REPAIR_PLAN" },
];

const stageIndex = new Map(stageMeta.map((item, index) => [item.key, index]));

export function normalizeStage(value?: string | null): WorkflowStage {
  return isWorkflowStage(value) ? value : "INTENT";
}

export function buildStageFlowNodes(
  summary: RealWorkspaceSummaryView | null,
  trace: RealWorkflowTraceView | null,
  latestJob: RealWorkflowJob | null,
): StageFlowNodeViewModel[] {
  const currentStage = normalizeStage(summary?.currentStage ?? trace?.currentStage ?? latestJob?.requestedStage);
  const currentIndex = stageIndex.get(currentStage) ?? 0;
  return stageMeta.map((meta, index) => {
    const traceNode = trace?.nodes?.find((node) => node.stage === meta.key || node.id === meta.key);
    const stageCard = summary?.stageCards?.find((card) => card.stage === meta.key);
    const rawStatus = traceNode?.status ?? stageCard?.status;
    return {
      key: meta.key,
      title: stageCard?.title ?? meta.title,
      caption: traceNode?.agentName ?? stageCard?.agentName ?? meta.caption,
      status: mapStageStatus(rawStatus, index, currentIndex, latestJob?.jobStatus),
    };
  });
}

export function buildTelemetryEvents(
  events: PageResult<RealWorkspaceEvent> | null,
  trace: RealWorkflowTraceView | null,
  executionLogs: RealExecutionLogsView | null,
): TelemetryEventViewModel[] {
  const history = events?.items ?? [];
  const traceEvents = trace?.events ?? [];
  const logEvents = executionLogs?.events ?? [];
  const mapped = [...history, ...traceEvents, ...logEvents].slice(0, 6).map((event, index) => ({
    id: event.eventId ?? event.relatedRecordId ?? `real-event-${index}`,
    eventType: event.eventType ?? event.stage ?? event.severity ?? "real.event",
    title: event.title ?? event.severity ?? "后端事件",
    message: event.message || stringField(event, ["payloadSummary"], "") || event.traceId || "后端暂未提供事件摘要",
  }));
  return mapped.length
    ? mapped
    : [
        {
          id: "event-not-ready",
          eventType: "NOT_READY",
          title: "事件历史未就绪",
          message: "后端还没有返回可展示的事件记录，面板保持原位等待真实数据。",
        },
      ];
}

export function buildArtifactPreview(
  selectedStage: WorkflowStage,
  summary: RealWorkspaceSummaryView | null,
  artifacts: PageResult<RealArtifactSummary> | null,
  configBlocks: RealConfigBlocksView | null,
): ArtifactPreviewViewModel {
  const meta = stageMeta.find((item) => item.key === selectedStage) ?? stageMeta[0];
  const stageCard = summary?.stageCards?.find((card) => card.stage === selectedStage);
  const artifact = artifacts?.items?.find((item) => item.stage === selectedStage || item.artifactType === meta.artifactType);
  const commands = selectedStage === "CONFIGURATION" ? (configBlocks?.commandBlocks?.[0]?.commands ?? []) : [];
  const status = stageCard?.status ?? artifact?.status ?? "NOT_READY";
  return {
    agentName: stageCard?.agentName ?? meta.caption,
    stage: selectedStage,
    artifactName: artifact?.artifactType ?? stageCard?.artifactType ?? meta.artifactType,
    versionLabel: artifact?.version ? `v${artifact.version}` : stageCard?.artifactVersion ? `v${stageCard.artifactVersion}` : "NOT_READY",
    summary:
      stageCard?.summary ??
      artifact?.payloadSummary ??
      summary?.currentStageSummary ??
      `${meta.artifactType} 尚未生成，等待后端阶段产物。`,
    commandDigest: commands.length ? commands.slice(0, 5) : [status === "READY" ? "后端未返回命令摘要" : `${meta.artifactType}_NOT_READY`],
    tone: toneFromStatus(status),
  };
}

export function buildTopologyBoard(topology: RealTopologyView | null, configBlocks: RealConfigBlocksView | null): TopologyBoardViewModel {
  const devices = (topology?.devices ?? []).map((device, index) => ({
    id: device.id,
    name: device.name ?? device.id,
    kind: mapDeviceKind(device.role ?? device.deviceType),
    role: device.role ?? device.deviceType ?? "UNKNOWN",
    model: device.vendor ?? device.deviceType ?? "unknown",
    status: mapDeviceStatus(device.status),
    x: typeof device.x === "number" ? device.x : 14 + (index % 4) * 22,
    y: typeof device.y === "number" ? device.y : 12 + Math.floor(index / 4) * 18,
  }));
  const links = (topology?.links ?? [])
    .filter((link) => link.from && link.to)
    .map((link) => ({
      from: String(link.from),
      to: String(link.to),
      status: mapLinkStatus(link.status),
    }));
  const configs = buildDeviceConfigs(configBlocks);
  return {
    devices,
    links,
    configs,
    statusLabel: statusLabel(configBlocks?.status ?? topology?.status),
    emptyMessage:
      topology?.message ??
      configBlocks?.message ??
      "NetworkPlan / ConfigSet 尚未生成，拓扑与命令窗口等待真实产物。",
  };
}

export function buildValidationAssertions(
  report: RealValidationReport | null,
  items: RealValidationItem[],
): ValidationAssertionDemo[] {
  const sourceItems = items.length ? items : (report?.items ?? []);
  if (!sourceItems.length) {
    return [
      {
        id: "validation-not-ready",
        source: "待验证源",
        destination: "待验证目标",
        expectation: "DENY",
        expectationLabel: "VALIDATION_REPORT_NOT_READY",
        actual: "BLOCKED",
        status: "PENDING",
        traceRefs: ["VALIDATION_REPORT_NOT_FOUND"],
        configBlockId: "CONFIG_SET_NOT_READY",
        testId: "VALIDATION_NOT_READY",
        message: "后端尚未生成 ValidationReport，验证矩阵保留占位。",
      },
    ];
  }
  return sourceItems.slice(0, 6).map((item, index) => ({
    id: stringField(item, ["id", "itemId", "assertionId"], `validation-${index}`),
    source: stringField(item, ["source", "sourceZone", "sourceNode"], "真实源"),
    destination: stringField(item, ["destination", "destinationZone", "targetNode"], "真实目标"),
    expectation: stringField(item, ["expectation"], "DENY") === "PERMIT" ? "PERMIT" : "DENY",
    expectationLabel: stringField(item, ["expectationLabel", "rule", "policy"], "真实验证断言"),
    actual: stringField(item, ["actual", "probeResult", "result"], "BLOCKED") === "REACHABLE" ? "REACHABLE" : "BLOCKED",
    status: mapValidationStatus(stringField(item, ["status", "validationStatus", "resultStatus"], "PENDING")),
    traceRefs: arrayField(item, ["traceRefs", "evidenceRefs"]),
    configBlockId: stringField(item, ["configBlockId", "relatedConfigId"], "CONFIG_BLOCK_UNKNOWN"),
    testId: stringField(item, ["testId", "caseId"], `REAL-VALIDATION-${index + 1}`),
    message: stringField(item, ["message", "summary", "reason"], "后端返回了验证项，但未提供详细说明。"),
  }));
}

export function buildEvidenceInspector(
  artifacts: PageResult<RealArtifactSummary> | null,
  executionLogs: RealExecutionLogsView | null,
): EvidenceInspectorDemo {
  const latest = artifacts?.items?.[0];
  const lines = executionLogs?.lines ?? [];
  return {
    relatedNode: latest?.artifactType ?? "Evidence NOT_READY",
    relatedConfigSummary: latest?.payloadSummary ?? executionLogs?.message ?? "后端尚未生成可检查的证据产物。",
    versionDiff: latest
      ? [
          {
            version: `v${latest.version ?? "-"}`,
            summary: latest.payloadSummary ?? latest.payloadType ?? latest.artifactId,
            tone: "new",
          },
        ]
      : [
          {
            version: "NOT_READY",
            summary: "缺少 artifact / execution evidence，Inspector 保留占位。",
            tone: "old",
          },
        ],
    conflictHint: lines[0]?.message ?? "未发现可展示的真实冲突证据；等待 ValidationReport / ExecutionReport。",
  };
}

export function buildRepairPlan(plan: RealRepairPlan | null): RepairPlanDemo {
  const actions = Array.isArray(plan?.actions) ? plan.actions : [];
  return {
    rca: buildRca(plan),
    actions: actions.map((action, index) => ({
      actionId: stringField(action, ["actionId", "id"], `real-action-${index + 1}`),
      actionType: mapRepairActionType(stringField(action, ["actionType", "type"], "PATCH_CONFIG")),
      targetStage: normalizeStage(stringField(action, ["targetStage", "stage"], "CONFIGURATION")),
      riskLevel: mapRiskLevel(stringField(action, ["riskLevel", "risk"], "MEDIUM")),
      requiresApproval: booleanField(action, ["requiresApproval"], true),
      status: mapRepairStatus(stringField(action, ["status", "actionStatus"], "PENDING_APPROVAL")),
      reason: stringField(action, ["reason", "summary"], "后端返回了修复动作，但未提供原因说明。"),
      guidance: stringField(action, ["guidance", "description"], "等待人工确认后由后端执行受控修复。"),
      traceRefs: arrayField(action, ["traceRefs", "evidenceRefs"]),
      candidateSnippet: arrayField(action, ["candidateSnippet", "commands"]),
    })),
  };
}

function buildRca(plan: RealRepairPlan | null): RcaReportDemo {
  if (!plan) {
    return {
      category: "REPAIR_PLAN_NOT_READY",
      severity: "MEDIUM",
      scope: "等待真实 RepairPlan",
      evidenceRefs: ["REPAIR_PLAN_NOT_FOUND"],
      recommendation: "后端尚未生成修复计划，修复驾驶舱保持占位，不展示推测动作。",
    };
  }
  const rca = objectField(plan, "rca");
  return {
    category: stringField(rca ?? plan, ["category", "reasonCode"], "真实修复分析"),
    severity: mapRiskLevel(stringField(rca ?? plan, ["severity", "riskLevel"], "MEDIUM")),
    scope: stringField(rca ?? plan, ["scope"], "真实任务范围"),
    evidenceRefs: arrayField(rca ?? plan, ["evidenceRefs", "traceRefs"]),
    recommendation: stringField(rca ?? plan, ["recommendation", "message"], "后端未返回修复建议详情。"),
  };
}

function buildDeviceConfigs(configBlocks: RealConfigBlocksView | null): DeviceConfig[] {
  const blocks = configBlocks?.commandBlocks ?? [];
  if (!blocks.length) {
    return [
      {
        deviceId: "not-ready",
        title: "配置未就绪",
        subtitle: configBlocks?.message ?? "CONFIG_SET_NOT_FOUND",
        commands: [],
      },
    ];
  }
  return blocks.map((block, index) => ({
    deviceId: block.deviceId ?? `device-${index + 1}`,
    title: block.deviceName ?? block.deviceId ?? `设备 ${index + 1}`,
    subtitle: block.summary ?? block.blockType ?? "后端配置块",
    commands: block.commands ?? [],
  }));
}

function mapStageStatus(status: RealViewStatus | undefined, index: number, currentIndex: number, jobStatus?: string): StageNodeStatus {
  if (status === "FAILED" || jobStatus === "FAILED") return "failed";
  if (status === "NOT_READY") return "not_ready";
  if (status === "READY" || status === "SUCCESS") return "done";
  if (index === currentIndex) return jobStatus === "SUCCESS" ? "done" : "running";
  if (index < currentIndex) return "done";
  return "not_ready";
}

function toneFromStatus(status?: string): "running" | "ok" | "bad" | "warn" | "pending" | "signal" {
  if (status === "READY" || status === "SUCCESS" || status === "PASSED") return "ok";
  if (status === "PARTIAL") return "warn";
  if (status === "FAILED" || status === "ERROR") return "bad";
  if (status === "RUNNING") return "running";
  if (status === "NOT_READY" || status === "PENDING") return "pending";
  return "signal";
}

function statusLabel(status?: string): string {
  if (!status) return "NOT_READY";
  if (status === "READY" || status === "SUCCESS") return "Config Applied";
  return status;
}

function mapDeviceKind(value?: string): TopologyDevice["kind"] {
  const text = (value ?? "").toLowerCase();
  if (text.includes("router")) return "router";
  if (text.includes("switch")) return "switch";
  return "service";
}

function mapDeviceStatus(value?: string): TopologyDevice["status"] {
  if (value === "DOWN") return "DOWN";
  if (value === "ANALYZING") return "ANALYZING";
  return "UP";
}

function mapLinkStatus(value?: string): TopologyLink["status"] {
  if (value === "BLOCKED") return "BLOCKED";
  if (value === "WARN") return "WARN";
  return "ACTIVE";
}

function mapValidationStatus(value: string): ValidationAssertionDemo["status"] {
  if (value === "PASSED" || value === "SUCCESS") return "PASSED";
  if (value === "FAILED" || value === "ERROR") return "FAILED";
  return "PENDING";
}

function mapRepairActionType(value: string): RepairActionDemo["actionType"] {
  if (value === "REGENERATE_CONFIG" || value === "REEXECUTE") return value;
  return "PATCH_CONFIG";
}

function mapRiskLevel(value: string): "LOW" | "MEDIUM" | "HIGH" {
  if (value === "LOW" || value === "HIGH") return value;
  return "MEDIUM";
}

function mapRepairStatus(value: string): RepairActionDemo["status"] {
  if (value === "APPROVED" || value === "APPLIED" || value === "REJECTED") return value;
  return "PENDING_APPROVAL";
}

function isWorkflowStage(value?: string | null): value is WorkflowStage {
  return value === "INTENT" || value === "PLANNING" || value === "CONFIGURATION" || value === "EXECUTION" || value === "VERIFICATION" || value === "HEALING";
}

function stringField(source: unknown, keys: string[], fallback: string): string {
  if (!source || typeof source !== "object") return fallback;
  const record = source as Record<string, unknown>;
  for (const key of keys) {
    const value = record[key];
    if (typeof value === "string" && value.trim()) return value;
    if (typeof value === "number" || typeof value === "boolean") return String(value);
  }
  return fallback;
}

function arrayField(source: unknown, keys: string[]): string[] {
  if (!source || typeof source !== "object") return [];
  const record = source as Record<string, unknown>;
  for (const key of keys) {
    const value = record[key];
    if (Array.isArray(value)) return value.map((item) => String(item));
  }
  return [];
}

function booleanField(source: unknown, keys: string[], fallback: boolean): boolean {
  if (!source || typeof source !== "object") return fallback;
  const record = source as Record<string, unknown>;
  for (const key of keys) {
    const value = record[key];
    if (typeof value === "boolean") return value;
  }
  return fallback;
}

function objectField(source: unknown, key: string): Record<string, unknown> | null {
  if (!source || typeof source !== "object") return null;
  const value = (source as Record<string, unknown>)[key];
  return value && typeof value === "object" ? (value as Record<string, unknown>) : null;
}
