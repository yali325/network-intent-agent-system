package com.yali.mactav.web.service;

import com.yali.mactav.common.result.PageResult;
import com.yali.mactav.model.config.CommandBlock;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.ConfigWarning;
import com.yali.mactav.model.config.DeviceConfig;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.execution.ExecutionError;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.execution.TestResult;
import com.yali.mactav.model.intent.IntentNode;
import com.yali.mactav.model.intent.IntentRelation;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.NetworkZone;
import com.yali.mactav.model.plan.SecurityPolicyPlanItem;
import com.yali.mactav.model.plan.Topology;
import com.yali.mactav.model.plan.TopologyLink;
import com.yali.mactav.model.plan.TopologyNode;
import com.yali.mactav.model.workflow.job.WorkflowJob;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import com.yali.mactav.modelcore.query.ArtifactQuery;
import com.yali.mactav.modelcore.query.WorkspaceEventQuery;
import com.yali.mactav.orchestrator.service.WorkflowAsyncService;
import com.yali.mactav.orchestrator.service.WorkflowOrchestrator;
import com.yali.mactav.orchestrator.service.WorkflowQueryService;
import com.yali.mactav.web.dto.view.ConfigBlocksView;
import com.yali.mactav.web.dto.view.ExecutionLogsView;
import com.yali.mactav.web.dto.view.TopologyView;
import com.yali.mactav.web.dto.view.ViewReadiness;
import com.yali.mactav.web.dto.view.WorkflowTraceView;
import com.yali.mactav.web.dto.view.WorkspaceSummaryView;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Default real mission view projector backed only by persisted workspace state.
 */
@Service
public class DefaultViewQueryService implements ViewQueryService {

    private static final List<WorkflowStage> VIEW_STAGES = List.of(
            WorkflowStage.INTENT,
            WorkflowStage.PLANNING,
            WorkflowStage.CONFIGURATION,
            WorkflowStage.EXECUTION,
            WorkflowStage.VERIFICATION,
            WorkflowStage.HEALING);

    private final WorkflowOrchestrator workflowOrchestrator;
    private final WorkflowQueryService workflowQueryService;
    private final WorkflowAsyncService workflowAsyncService;

    public DefaultViewQueryService(WorkflowOrchestrator workflowOrchestrator,
                                   WorkflowQueryService workflowQueryService,
                                   WorkflowAsyncService workflowAsyncService) {
        this.workflowOrchestrator = workflowOrchestrator;
        this.workflowQueryService = workflowQueryService;
        this.workflowAsyncService = workflowAsyncService;
    }

    @Override
    public WorkspaceSummaryView getWorkspaceSummary(String taskId) {
        NetworkWorkspace workspace = workspace(taskId);
        List<NetworkArtifact> artifacts = artifacts(taskId);
        Map<ArtifactType, NetworkArtifact> byType = latestByType(artifacts);
        List<String> missing = missingArtifacts(byType);
        List<WorkspaceSummaryView.StageCard> stageCards = VIEW_STAGES.stream()
                .map(stage -> stageCard(workspace, byType, stage))
                .toList();
        WorkflowJob latestJob = latestJob(taskId);
        List<String> errors = new ArrayList<>();
        if (latestJob != null && latestJob.getErrorCode() != null) {
            errors.add(latestJob.getErrorCode() + ": " + nullToDash(latestJob.getErrorMessage()));
        }
        return WorkspaceSummaryView.builder()
                .taskId(taskId)
                .taskStatus(workspace.getTask() == null || workspace.getTask().getTaskStatus() == null
                        ? null
                        : workspace.getTask().getTaskStatus().name())
                .currentStage(workspace.getTask() == null || workspace.getTask().getCurrentStage() == null
                        ? null
                        : workspace.getTask().getCurrentStage().name())
                .workspaceStatus(workspace.getWorkspaceStatus() == null ? null : workspace.getWorkspaceStatus().name())
                .currentArtifactRefs(stringRefs(workspace.getCurrentArtifactRefs()))
                .latestJob(latestJob)
                .stageCards(stageCards)
                .currentStageSummary(currentStageSummary(workspace, byType))
                .readiness(readiness(missing.isEmpty(), missing.isEmpty() ? "READY" : "PARTIAL",
                        missing.isEmpty() ? null : "MISSING_ARTIFACT",
                        missing.isEmpty() ? "Workspace summary is ready" : "Some stage artifacts are not generated yet",
                        missing))
                .missingArtifacts(missing)
                .errors(errors)
                .build();
    }

    @Override
    public WorkflowTraceView getWorkflowTrace(String taskId) {
        NetworkWorkspace workspace = workspace(taskId);
        List<NetworkArtifact> artifacts = artifacts(taskId);
        Map<ArtifactType, NetworkArtifact> byType = latestByType(artifacts);
        List<String> missing = missingArtifacts(byType);
        List<WorkflowTraceView.TraceNode> nodes = VIEW_STAGES.stream()
                .map(stage -> traceNode(byType, stage))
                .toList();
        List<WorkflowTraceView.TraceEdge> edges = new ArrayList<>();
        for (int index = 0; index < VIEW_STAGES.size() - 1; index++) {
            WorkflowStage from = VIEW_STAGES.get(index);
            WorkflowStage to = VIEW_STAGES.get(index + 1);
            edges.add(WorkflowTraceView.TraceEdge.builder()
                    .from(from.name())
                    .to(to.name())
                    .status(stageIndex(workspace) > index ? "READY" : "PENDING")
                    .label(from.name() + " -> " + to.name())
                    .build());
        }
        WorkflowJob latestJob = latestJob(taskId);
        List<WorkflowTraceView.TraceEvent> events = events(taskId).stream()
                .map(this::traceEvent)
                .toList();
        return WorkflowTraceView.builder()
                .taskId(taskId)
                .status(missing.isEmpty() && !events.isEmpty() ? "READY" : "NOT_READY")
                .ready(missing.isEmpty() && !events.isEmpty())
                .reasonCode(missing.isEmpty() && !events.isEmpty() ? null : "TRACE_NOT_READY")
                .message(missing.isEmpty() && !events.isEmpty()
                        ? "Workflow trace is ready"
                        : "Workflow trace is derived from available artifacts and events only")
                .currentStage(workspace.getTask() == null || workspace.getTask().getCurrentStage() == null
                        ? null
                        : workspace.getTask().getCurrentStage().name())
                .jobStatus(latestJob == null || latestJob.getJobStatus() == null ? null : latestJob.getJobStatus().name())
                .missingArtifacts(missing)
                .nodes(nodes)
                .edges(edges)
                .events(events)
                .errors(latestJob == null || latestJob.getErrorCode() == null
                        ? List.of()
                        : List.of(latestJob.getErrorCode() + ": " + nullToDash(latestJob.getErrorMessage())))
                .build();
    }

    @Override
    public TopologyView getTopology(String taskId) {
        NetworkWorkspace workspace = workspace(taskId);
        NetworkArtifact planArtifact = latestArtifact(taskId, ArtifactType.NETWORK_PLAN);
        NetworkPlan plan = workspace.getCurrentPlan();
        if (plan != null && plan.getTopology() != null) {
            return topologyFromPlan(taskId, plan, planArtifact);
        }
        NetworkArtifact intentArtifact = latestArtifact(taskId, ArtifactType.NETWORK_INTENT);
        NetworkIntent intent = workspace.getCurrentIntent();
        if (intent != null && intent.getSemanticIntentGraph() != null) {
            return topologyFromIntent(taskId, intent, intentArtifact);
        }
        return TopologyView.builder()
                .taskId(taskId)
                .status("NOT_READY")
                .ready(false)
                .sourceArtifactType(null)
                .sourceArtifactId(null)
                .sourceStage(null)
                .devices(List.of())
                .links(List.of())
                .policies(List.of())
                .annotations(List.of())
                .reasonCode("NETWORK_TOPOLOGY_NOT_READY")
                .message("NetworkPlan and NetworkIntent artifacts are not generated yet")
                .build();
    }

    @Override
    public ConfigBlocksView getConfigBlocks(String taskId) {
        NetworkWorkspace workspace = workspace(taskId);
        ConfigSet configSet = workspace.getCurrentConfigSet();
        NetworkArtifact configArtifact = latestArtifact(taskId, ArtifactType.CONFIG_SET);
        if (configSet == null) {
            return ConfigBlocksView.builder()
                    .taskId(taskId)
                    .status("NOT_READY")
                    .ready(false)
                    .sourceArtifactId(null)
                    .devices(List.of())
                    .commandBlocks(List.of())
                    .warnings(List.of())
                    .reasonCode("CONFIG_SET_NOT_FOUND")
                    .message("Current ConfigSet artifact is not generated yet")
                    .build();
        }
        List<ConfigBlocksView.ConfigDeviceView> devices = configSet.getDeviceConfigs().stream()
                .map(device -> configDevice(device, configArtifact))
                .toList();
        List<ConfigBlocksView.CommandBlockView> commandBlocks = configSet.getDeviceConfigs().stream()
                .flatMap(device -> device.getCommandBlocks().stream().map(block -> commandBlock(device, block, configArtifact)))
                .toList();
        return ConfigBlocksView.builder()
                .taskId(taskId)
                .status("READY")
                .ready(true)
                .sourceArtifactId(configArtifact == null ? null : configArtifact.getArtifactId())
                .configVersion(configSet.getConfigVersion())
                .devices(devices)
                .commandBlocks(commandBlocks)
                .warnings(configSet.getWarnings().stream().map(this::warningText).filter(Objects::nonNull).toList())
                .reasonCode(null)
                .message("Config blocks are ready")
                .build();
    }

    @Override
    public ExecutionLogsView getExecutionLogs(String taskId) {
        NetworkWorkspace workspace = workspace(taskId);
        ExecutionReport report = workspace.getCurrentExecutionReport();
        List<WorkflowTraceView.TraceEvent> executionEvents = events(taskId).stream()
                .filter(event -> event.getStage() == WorkflowStage.EXECUTION)
                .map(this::traceEvent)
                .toList();
        if (report == null) {
            return ExecutionLogsView.builder()
                    .taskId(taskId)
                    .status("NOT_READY")
                    .ready(false)
                    .source("workspace-event")
                    .lines(List.of())
                    .events(executionEvents)
                    .reasonCode("EXECUTION_REPORT_NOT_FOUND")
                    .message("Current ExecutionReport artifact is not generated yet")
                    .build();
        }
        List<ExecutionLogsView.ExecutionLogLine> lines = new ArrayList<>();
        if (report.getRuntimeState() != null && report.getRuntimeState().getLogsSummary() != null) {
            lines.add(logLine(report.getRuntimeState().getStartedAt(), "INFO", "runtime-state",
                    report.getRuntimeState().getLogsSummary(), null, traceId(report)));
        }
        for (TestResult testResult : report.getTestResults()) {
            lines.add(logLine(report.getUpdateTime(), testResult.getStatus() == null ? "INFO" : testResult.getStatus().name(),
                    "test-result",
                    nullToDash(testResult.getTestType()) + ": " + nullToDash(testResult.getActualResult()),
                    testResult.getTestId(),
                    traceId(report)));
        }
        for (ExecutionError error : report.getErrors()) {
            lines.add(logLine(report.getEndTime(), "ERROR", "execution-error",
                    nullToDash(error.getErrorCode()) + ": " + nullToDash(error.getMessage()),
                    error.getErrorId(),
                    traceId(report)));
        }
        return ExecutionLogsView.builder()
                .taskId(taskId)
                .status("READY")
                .ready(true)
                .source("ExecutionReport")
                .lines(lines)
                .events(executionEvents)
                .reasonCode(null)
                .message("Execution logs are derived from ExecutionReport and workspace events")
                .build();
    }

    private NetworkWorkspace workspace(String taskId) {
        return workflowOrchestrator.getWorkspace(taskId);
    }

    private List<NetworkArtifact> artifacts(String taskId) {
        PageResult<NetworkArtifact> result = workflowQueryService.listArtifacts(
                taskId,
                new ArtifactQuery(null, null, 1, 100));
        return result.getItems();
    }

    private List<WorkspaceEvent> events(String taskId) {
        PageResult<WorkspaceEvent> result = workflowQueryService.listEventHistory(
                taskId,
                new WorkspaceEventQuery(null, null, null, null, 1, 50));
        return result.getItems();
    }

    private WorkflowJob latestJob(String taskId) {
        return workflowAsyncService.listByTaskId(taskId).stream()
                .max(Comparator.comparing(this::jobSortTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private LocalDateTime jobSortTime(WorkflowJob job) {
        if (job.getUpdateTime() != null) {
            return job.getUpdateTime();
        }
        if (job.getCreateTime() != null) {
            return job.getCreateTime();
        }
        return job.getStartTime();
    }

    private Map<ArtifactType, NetworkArtifact> latestByType(List<NetworkArtifact> artifacts) {
        Map<ArtifactType, NetworkArtifact> result = new EnumMap<>(ArtifactType.class);
        for (NetworkArtifact artifact : artifacts) {
            NetworkArtifact existing = result.get(artifact.getArtifactType());
            if (existing == null || safeVersion(artifact) > safeVersion(existing)) {
                result.put(artifact.getArtifactType(), artifact);
            }
        }
        return result;
    }

    private int safeVersion(NetworkArtifact artifact) {
        return artifact == null || artifact.getVersion() == null ? 0 : artifact.getVersion();
    }

    private NetworkArtifact latestArtifact(String taskId, ArtifactType artifactType) {
        return workflowQueryService.listArtifacts(taskId, new ArtifactQuery(artifactType, null, 1, 1))
                .getItems()
                .stream()
                .findFirst()
                .orElse(null);
    }

    private List<String> missingArtifacts(Map<ArtifactType, NetworkArtifact> artifacts) {
        List<String> missing = new ArrayList<>();
        for (ArtifactType artifactType : ArtifactType.values()) {
            if (!artifacts.containsKey(artifactType)) {
                missing.add(artifactType.name());
            }
        }
        return missing;
    }

    private WorkspaceSummaryView.StageCard stageCard(
            NetworkWorkspace workspace,
            Map<ArtifactType, NetworkArtifact> artifacts,
            WorkflowStage stage) {
        ArtifactType artifactType = artifactType(stage);
        NetworkArtifact artifact = artifacts.get(artifactType);
        boolean ready = artifact != null;
        return WorkspaceSummaryView.StageCard.builder()
                .stage(stage.name())
                .title(stageTitle(stage))
                .agentName(agentName(stage))
                .status(ready ? "SUCCESS" : stage == currentStage(workspace) ? "RUNNING" : "NOT_READY")
                .artifactType(artifactType.name())
                .artifactId(artifact == null ? null : artifact.getArtifactId())
                .artifactVersion(artifact == null ? null : artifact.getVersion())
                .summary(artifact == null ? null : artifact.getPayloadSummary())
                .updateTime(artifact == null || artifact.getCreateTime() == null ? null : artifact.getCreateTime().toString())
                .build();
    }

    private WorkflowTraceView.TraceNode traceNode(Map<ArtifactType, NetworkArtifact> artifacts, WorkflowStage stage) {
        ArtifactType artifactType = artifactType(stage);
        NetworkArtifact artifact = artifacts.get(artifactType);
        return WorkflowTraceView.TraceNode.builder()
                .id(stage.name())
                .stage(stage.name())
                .label(stageTitle(stage))
                .agentName(agentName(stage))
                .status(artifact == null ? "NOT_READY" : "SUCCESS")
                .artifactType(artifactType.name())
                .artifactId(artifact == null ? null : artifact.getArtifactId())
                .build();
    }

    private WorkflowTraceView.TraceEvent traceEvent(WorkspaceEvent event) {
        return WorkflowTraceView.TraceEvent.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .stage(event.getStage() == null ? null : event.getStage().name())
                .severity(event.getSeverity())
                .title(event.getTitle())
                .message(event.getMessage())
                .eventTime(event.getEventTime() == null ? null : event.getEventTime().toString())
                .relatedArtifactId(event.getRelatedArtifactId())
                .relatedRecordId(event.getRelatedRecordId())
                .traceId(event.getTraceId())
                .build();
    }

    private TopologyView topologyFromPlan(String taskId, NetworkPlan plan, NetworkArtifact artifact) {
        Topology topology = plan.getTopology();
        List<TopologyView.TopologyDevice> devices = topology.getNodes().stream()
                .map(node -> topologyDevice(node, plan))
                .toList();
        List<TopologyView.TopologyLink> links = topology.getLinks().stream()
                .map(this::topologyLink)
                .toList();
        return TopologyView.builder()
                .taskId(taskId)
                .status("READY")
                .ready(true)
                .sourceArtifactType(ArtifactType.NETWORK_PLAN.name())
                .sourceArtifactId(artifact == null ? null : artifact.getArtifactId())
                .sourceStage(WorkflowStage.PLANNING.name())
                .devices(devices)
                .links(links)
                .policies(plan.getSecurityPolicyPlan().stream().map(this::policyText).filter(Objects::nonNull).toList())
                .annotations(List.of(nullToDash(plan.getPlanSummary())))
                .reasonCode(null)
                .message("Topology is derived from NetworkPlan")
                .build();
    }

    private TopologyView topologyFromIntent(String taskId, NetworkIntent intent, NetworkArtifact artifact) {
        List<TopologyView.TopologyDevice> devices = intent.getSemanticIntentGraph().getNodes().stream()
                .map(this::intentDevice)
                .toList();
        List<TopologyView.TopologyLink> links = intent.getSemanticIntentGraph().getRelations().stream()
                .map(this::intentLink)
                .toList();
        return TopologyView.builder()
                .taskId(taskId)
                .status("PARTIAL")
                .ready(false)
                .sourceArtifactType(ArtifactType.NETWORK_INTENT.name())
                .sourceArtifactId(artifact == null ? null : artifact.getArtifactId())
                .sourceStage(WorkflowStage.INTENT.name())
                .devices(devices)
                .links(links)
                .policies(List.of())
                .annotations(List.of("Only semantic intent topology is available; NetworkPlan is not generated yet"))
                .reasonCode("NETWORK_PLAN_NOT_FOUND")
                .message("Topology is partially derived from NetworkIntent")
                .build();
    }

    private TopologyView.TopologyDevice topologyDevice(TopologyNode node, NetworkPlan plan) {
        return TopologyView.TopologyDevice.builder()
                .id(node.getId())
                .name(firstNonBlank(node.getName(), node.getId()))
                .role(firstNonBlank(node.getRole(), node.getNodeType()))
                .zone(zoneName(plan.getZones(), node.getZoneId()))
                .ip(firstNonBlank(node.getIp(), node.getIpAddress()))
                .status("UNKNOWN")
                .vendor(node.getVendor())
                .deviceType(firstNonBlank(node.getDeviceType(), node.getHostType()))
                .x(null)
                .y(null)
                .build();
    }

    private TopologyView.TopologyLink topologyLink(TopologyLink link) {
        return TopologyView.TopologyLink.builder()
                .id(link.getId())
                .from(link.getSourceNode())
                .to(link.getTargetNode())
                .label(link.getLinkType())
                .status("UNKNOWN")
                .sourcePort(link.getSourceInterface())
                .targetPort(link.getTargetInterface())
                .build();
    }

    private TopologyView.TopologyDevice intentDevice(IntentNode node) {
        return TopologyView.TopologyDevice.builder()
                .id(node.getId())
                .name(firstNonBlank(node.getName(), node.getId()))
                .role(node.getType())
                .zone(stringAttr(node.getAttributes(), "zone"))
                .ip(stringAttr(node.getAttributes(), "ip"))
                .status("UNKNOWN")
                .vendor(stringAttr(node.getAttributes(), "vendor"))
                .deviceType(node.getType())
                .build();
    }

    private TopologyView.TopologyLink intentLink(IntentRelation relation) {
        return TopologyView.TopologyLink.builder()
                .id(relation.getId())
                .from(relation.getSource())
                .to(relation.getTarget())
                .label(firstNonBlank(relation.getAction(), relation.getType()))
                .status("UNKNOWN")
                .policy(relation.getService())
                .evidence(relation.getDescription())
                .build();
    }

    private ConfigBlocksView.ConfigDeviceView configDevice(DeviceConfig device, NetworkArtifact artifact) {
        List<String> commands = device.getCommandBlocks().stream()
                .flatMap(block -> block.getCommands().stream())
                .toList();
        List<String> rollbackCommands = device.getCommandBlocks().stream()
                .flatMap(block -> block.getRollbackCommands().stream())
                .toList();
        return ConfigBlocksView.ConfigDeviceView.builder()
                .deviceId(device.getDeviceId())
                .deviceName(device.getDeviceName())
                .vendor(device.getVendor())
                .model(device.getDeviceType())
                .status("READY")
                .commands(commands)
                .rollbackCommands(rollbackCommands)
                .summary(commands.isEmpty() ? "No commands in current ConfigSet" : commands.size() + " commands")
                .artifactId(artifact == null ? null : artifact.getArtifactId())
                .traceRefs(device.getTraceRefs())
                .build();
    }

    private ConfigBlocksView.CommandBlockView commandBlock(
            DeviceConfig device,
            CommandBlock block,
            NetworkArtifact artifact) {
        return ConfigBlocksView.CommandBlockView.builder()
                .deviceId(device.getDeviceId())
                .deviceName(device.getDeviceName())
                .blockId(block.getBlockId())
                .title(block.getTitle())
                .blockType(block.getBlockType())
                .order(block.getOrder())
                .commands(block.getCommands())
                .rollbackCommands(block.getRollbackCommands())
                .summary(block.getExplanation())
                .artifactId(artifact == null ? null : artifact.getArtifactId())
                .traceRefs(block.getTraceRefs())
                .build();
    }

    private ExecutionLogsView.ExecutionLogLine logLine(
            LocalDateTime time,
            String level,
            String source,
            String message,
            String relatedRecordId,
            String traceId) {
        return ExecutionLogsView.ExecutionLogLine.builder()
                .time(time == null ? null : time.toString())
                .level(level)
                .stage(WorkflowStage.EXECUTION.name())
                .source(source)
                .message(message)
                .relatedRecordId(relatedRecordId)
                .traceId(traceId)
                .build();
    }

    private ViewReadiness readiness(
            boolean ready,
            String status,
            String reasonCode,
            String message,
            List<String> missingArtifacts) {
        return ViewReadiness.builder()
                .ready(ready)
                .status(status)
                .reasonCode(reasonCode)
                .message(message)
                .missingArtifacts(missingArtifacts)
                .build();
    }

    private Map<String, String> stringRefs(Map<ArtifactType, String> refs) {
        Map<String, String> result = new java.util.LinkedHashMap<>();
        if (refs == null) {
            return result;
        }
        refs.forEach((key, value) -> result.put(key.name(), value));
        return result;
    }

    private ArtifactType artifactType(WorkflowStage stage) {
        return switch (stage) {
            case INTENT -> ArtifactType.NETWORK_INTENT;
            case PLANNING -> ArtifactType.NETWORK_PLAN;
            case CONFIGURATION -> ArtifactType.CONFIG_SET;
            case EXECUTION -> ArtifactType.EXECUTION_REPORT;
            case VERIFICATION -> ArtifactType.VALIDATION_REPORT;
            case HEALING -> ArtifactType.REPAIR_PLAN;
            case FINISHED -> ArtifactType.REPAIR_PLAN;
        };
    }

    private WorkflowStage currentStage(NetworkWorkspace workspace) {
        return workspace.getTask() == null ? null : workspace.getTask().getCurrentStage();
    }

    private int stageIndex(NetworkWorkspace workspace) {
        WorkflowStage current = currentStage(workspace);
        int index = VIEW_STAGES.indexOf(current);
        return Math.max(index, 0);
    }

    private String currentStageSummary(NetworkWorkspace workspace, Map<ArtifactType, NetworkArtifact> byType) {
        WorkflowStage current = currentStage(workspace);
        if (current == null) {
            return null;
        }
        NetworkArtifact artifact = byType.get(artifactType(current));
        return artifact == null ? null : artifact.getPayloadSummary();
    }

    private String stageTitle(WorkflowStage stage) {
        return switch (stage) {
            case INTENT -> "Intent";
            case PLANNING -> "Planning";
            case CONFIGURATION -> "Configuration";
            case EXECUTION -> "Execution";
            case VERIFICATION -> "Verification";
            case HEALING -> "Healing";
            case FINISHED -> "Finished";
        };
    }

    private String agentName(WorkflowStage stage) {
        return switch (stage) {
            case INTENT -> "IntentAgent";
            case PLANNING -> "PlanningAgent";
            case CONFIGURATION -> "ConfigurationAgent";
            case EXECUTION -> "ExecutionAdapter";
            case VERIFICATION -> "VerificationAgent";
            case HEALING -> "HealingAgent";
            case FINISHED -> "Orchestrator";
        };
    }

    private String zoneName(List<NetworkZone> zones, String zoneId) {
        if (zoneId == null || zones == null) {
            return zoneId;
        }
        return zones.stream()
                .filter(zone -> zoneId.equals(zone.getId()))
                .map(NetworkZone::getName)
                .findFirst()
                .orElse(zoneId);
    }

    private String policyText(SecurityPolicyPlanItem policy) {
        if (policy == null) {
            return null;
        }
        return firstNonBlank(policy.getName(), policy.getId()) + ": "
                + nullToDash(policy.getSourceZone()) + " -> "
                + nullToDash(policy.getTargetZone()) + " "
                + nullToDash(policy.getAction());
    }

    private String warningText(ConfigWarning warning) {
        if (warning == null) {
            return null;
        }
        return firstNonBlank(warning.getMessage(), warning.getLevel(), warning.getRelatedBlockId());
    }

    private String traceId(ExecutionReport report) {
        return report.getExecutionId();
    }

    private String stringAttr(Map<String, Object> attributes, String key) {
        if (attributes == null || attributes.get(key) == null) {
            return null;
        }
        return String.valueOf(attributes.get(key));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String nullToDash(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
