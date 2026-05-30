package com.yali.mactav.orchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.a2a.A2aResponse;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.enums.TaskStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.intent.IntentAgentInvokePayload;
import com.yali.mactav.model.intent.IntentNode;
import com.yali.mactav.model.intent.IntentRelation;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.PlanningAgentInvokePayload;
import com.yali.mactav.model.task.NetworkTask;
import com.yali.mactav.model.workspace.AgentExecutionRecord;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.model.workspace.TraceRefs;
import com.yali.mactav.modelcore.service.AgentExecutionRecordService;
import com.yali.mactav.modelcore.service.NetworkWorkspaceService;
import com.yali.mactav.orchestrator.remote.invoker.RemoteAgentInvoker;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Deterministic MAC-TAV workflow coordinator for Intent and Planning stage closure.
 *
 * <p>The orchestrator creates workspaces, invokes remote agents through
 * RemoteAgentInvoker, and writes stage artifacts through Model Core. It does not
 * construct prompts, call ChatModel/ReactAgent, or depend on concrete agent
 * modules.</p>
 */
public class MacTavWorkflowOrchestrator implements WorkflowOrchestrator {

    private static final String ORCHESTRATOR_AGENT = "MacTavOrchestrator";

    private static final String INTENT_AGENT = "IntentAgent";

    private static final String PLANNING_AGENT = "PlanningAgent";

    private final NetworkWorkspaceService workspaceService;

    private final AgentExecutionRecordService executionRecordService;

    private final RemoteAgentInvoker remoteAgentInvoker;

    private final ObjectMapper objectMapper;

    private final ConcurrentMap<String, String> targetEnvironmentHints = new ConcurrentHashMap<>();

    public MacTavWorkflowOrchestrator(NetworkWorkspaceService workspaceService,
                                      AgentExecutionRecordService executionRecordService,
                                      RemoteAgentInvoker remoteAgentInvoker,
                                      ObjectMapper objectMapper) {
        this.workspaceService = workspaceService;
        this.executionRecordService = executionRecordService;
        this.remoteAgentInvoker = remoteAgentInvoker;
        this.objectMapper = objectMapper;
    }

    @Override
    public NetworkWorkspace createTask(String rawText, String targetEnvironmentHint, String createdBy) {
        if (rawText == null || rawText.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "rawText must not be blank");
        }
        String taskId = "task-" + UUID.randomUUID();
        NetworkTask task = NetworkTask.builder()
                .taskId(taskId)
                .rawText(rawText)
                .taskStatus(TaskStatus.CREATED)
                .currentStage(WorkflowStage.INTENT)
                .createTime(LocalDateTime.now())
                .createdBy(createdBy == null || createdBy.isBlank() ? "api" : createdBy)
                .description("MAC-TAV workflow task")
                .build();
        if (targetEnvironmentHint != null && !targetEnvironmentHint.isBlank()) {
            targetEnvironmentHints.put(taskId, targetEnvironmentHint);
        }
        return workspaceService.createWorkspace(task);
    }

    @Override
    public NetworkWorkspace runIntentStage(String taskId) {
        LocalDateTime startTime = LocalDateTime.now();
        String traceId = "trace-" + UUID.randomUUID();
        try {
            NetworkWorkspace workspace = workspaceService.getWorkspaceOrThrow(taskId);
            int intentVersion = nextIntentVersion(workspace);
            workspaceService.updateTaskStage(taskId, WorkflowStage.INTENT);
            workspaceService.updateTaskStatus(taskId, TaskStatus.RUNNING);

            A2aRequest request = buildIntentRequest(workspace, intentVersion, traceId);
            A2aResponse response = remoteAgentInvoker.invoke(request);
            if (!Boolean.TRUE.equals(response.getSuccess())) {
                throw new BusinessException(
                        response.getErrorCode(),
                        response.getMessage() == null ? "Remote IntentAgent failed" : response.getMessage());
            }
            NetworkIntent intent = parseIntent(response.getPayloadJson());
            NetworkArtifact artifact = workspaceService.saveStageArtifact(
                    taskId,
                    ArtifactType.NETWORK_INTENT,
                    WorkflowStage.INTENT,
                    intent,
                    "NetworkIntent for task " + taskId,
                    workspace.getTask().getCreatedBy(),
                    traceRefs(intent));
            appendExecutionRecord(taskId, traceId, startTime, StageStatus.SUCCESS, artifact, null, null);
            return workspaceService.getWorkspaceOrThrow(taskId);
        }
        catch (BusinessException ex) {
            markTaskErrorAndRecord(taskId, traceId, startTime, ex.getErrorCode(), ex.getMessage());
            throw ex;
        }
        catch (RuntimeException ex) {
            markTaskErrorAndRecord(taskId, traceId, startTime,
                    ErrorCode.AGENT_EXECUTION_FAILED.name(), ex.getMessage());
            throw ex;
        }
    }

    @Override
    public NetworkWorkspace runPlanningStage(String taskId) {
        LocalDateTime startTime = LocalDateTime.now();
        String traceId = "trace-" + UUID.randomUUID();
        try {
            NetworkWorkspace workspace = workspaceService.getWorkspaceOrThrow(taskId);
            NetworkIntent currentIntent = workspace.getCurrentIntent();
            if (currentIntent == null) {
                throw new BusinessException(ErrorCode.STAGE_NOT_READY,
                        "No current Intent found. Run intent stage first for task: " + taskId);
            }
            int planVersion = nextPlanVersion(workspace);
            workspaceService.updateTaskStage(taskId, WorkflowStage.PLANNING);
            workspaceService.updateTaskStatus(taskId, TaskStatus.RUNNING);

            A2aRequest request = buildPlanningRequest(workspace, currentIntent, planVersion, traceId);
            A2aResponse response = remoteAgentInvoker.invoke(request);
            if (!Boolean.TRUE.equals(response.getSuccess())) {
                throw new BusinessException(
                        response.getErrorCode(),
                        response.getMessage() == null ? "Remote PlanningAgent failed" : response.getMessage());
            }
            NetworkPlan plan = parsePlan(response.getPayloadJson());
            NetworkArtifact artifact = workspaceService.saveStageArtifact(
                    taskId,
                    ArtifactType.NETWORK_PLAN,
                    WorkflowStage.PLANNING,
                    plan,
                    plan.getPlanSummary() != null ? plan.getPlanSummary() : "NetworkPlan for task " + taskId,
                    workspace.getTask().getCreatedBy(),
                    plan.getTraceRefs());
            appendExecutionRecord(taskId, traceId, startTime, StageStatus.SUCCESS,
                    artifact, null, null, PLANNING_AGENT, WorkflowStage.PLANNING, null, null);
            return workspaceService.getWorkspaceOrThrow(taskId);
        }
        catch (BusinessException ex) {
            markTaskErrorAndRecord(taskId, traceId, startTime, ex.getErrorCode(), ex.getMessage());
            throw ex;
        }
        catch (RuntimeException ex) {
            markTaskErrorAndRecord(taskId, traceId, startTime,
                    ErrorCode.AGENT_EXECUTION_FAILED.name(), ex.getMessage());
            throw ex;
        }
    }

    @Override
    public NetworkWorkspace getWorkspace(String taskId) {
        return workspaceService.getWorkspaceOrThrow(taskId);
    }

    private A2aRequest buildIntentRequest(NetworkWorkspace workspace, int intentVersion, String traceId) {
        IntentAgentInvokePayload payload = IntentAgentInvokePayload.builder()
                .taskId(workspace.getTask().getTaskId())
                .rawText(workspace.getTask().getRawText())
                .intentVersion(intentVersion)
                .traceId(traceId)
                .userContext(targetEnvironmentHints.get(workspace.getTask().getTaskId()))
                .workspaceSnapshot(workspaceSnapshot(workspace))
                .targetEnvironmentHint(targetEnvironmentHints.get(workspace.getTask().getTaskId()))
                .createdBy(workspace.getTask().getCreatedBy())
                .build();
        return A2aRequest.builder()
                .taskId(workspace.getTask().getTaskId())
                .sourceAgent(ORCHESTRATOR_AGENT)
                .targetAgent(INTENT_AGENT)
                .stage(WorkflowStage.INTENT)
                .artifactVersion(intentVersion)
                .payloadJson(serialize(payload, ErrorCode.A2A_CALL_FAILED, "Intent A2A payload serialization failed"))
                .traceId(traceId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private A2aRequest buildPlanningRequest(NetworkWorkspace workspace, NetworkIntent currentIntent,
                                            int planVersion, String traceId) {
        String intentJson = serialize(currentIntent,
                ErrorCode.AGENT_PARSE_FAILED,
                "Failed to serialize current Intent for planning request");
        PlanningAgentInvokePayload payload = PlanningAgentInvokePayload.builder()
                .taskId(workspace.getTask().getTaskId())
                .rawText(workspace.getTask().getRawText())
                .intentVersion(workspace.getCurrentIntentVersion())
                .intentJson(intentJson)
                .planVersion(planVersion)
                .traceId(traceId)
                .userContext(targetEnvironmentHints.get(workspace.getTask().getTaskId()))
                .workspaceSnapshot(workspaceSnapshot(workspace))
                .targetEnvironmentHint(targetEnvironmentHints.get(workspace.getTask().getTaskId()))
                .createdBy(workspace.getTask().getCreatedBy())
                .build();
        return A2aRequest.builder()
                .taskId(workspace.getTask().getTaskId())
                .sourceAgent(ORCHESTRATOR_AGENT)
                .targetAgent(PLANNING_AGENT)
                .stage(WorkflowStage.PLANNING)
                .artifactVersion(planVersion)
                .payloadJson(serialize(payload, ErrorCode.A2A_CALL_FAILED,
                        "Planning A2A payload serialization failed"))
                .traceId(traceId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private NetworkIntent parseIntent(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, NetworkIntent.class);
        }
        catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.A2A_RESPONSE_INVALID, "IntentAgent payload JSON is invalid", ex);
        }
    }

    private NetworkPlan parsePlan(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, NetworkPlan.class);
        }
        catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.A2A_RESPONSE_INVALID, "PlanningAgent payload JSON is invalid", ex);
        }
    }

    private String workspaceSnapshot(NetworkWorkspace workspace) {
        try {
            return objectMapper.writeValueAsString(workspace);
        }
        catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private String serialize(Object value, ErrorCode errorCode, String message) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException ex) {
            throw new BusinessException(errorCode, message, ex);
        }
    }

    private int nextIntentVersion(NetworkWorkspace workspace) {
        Integer current = workspace.getCurrentIntentVersion();
        return current == null ? 1 : current + 1;
    }

    private int nextPlanVersion(NetworkWorkspace workspace) {
        Integer current = workspace.getCurrentPlanVersion();
        return current == null ? 1 : current + 1;
    }

    private TraceRefs traceRefs(NetworkIntent intent) {
        if (intent == null || intent.getSemanticIntentGraph() == null) {
            return TraceRefs.builder().build();
        }
        List<String> nodeIds = intent.getSemanticIntentGraph().getNodes().stream()
                .map(IntentNode::getId)
                .filter(id -> id != null && !id.isBlank())
                .toList();
        List<String> relationIds = intent.getSemanticIntentGraph().getRelations().stream()
                .map(IntentRelation::getId)
                .filter(id -> id != null && !id.isBlank())
                .toList();
        return TraceRefs.builder()
                .intentNodeIds(nodeIds)
                .intentRelationIds(relationIds)
                .build();
    }

    private void appendExecutionRecord(String taskId,
                                       String traceId,
                                       LocalDateTime startTime,
                                       StageStatus status,
                                       NetworkArtifact artifact,
                                       String errorCode,
                                       String errorMessage) {
        appendExecutionRecord(taskId, traceId, startTime, status, artifact, errorCode, errorMessage,
                INTENT_AGENT, WorkflowStage.INTENT, status == StageStatus.SUCCESS
                        ? "IntentAgent A2A call succeeded" : "IntentAgent A2A call failed",
                status == StageStatus.SUCCESS ? "Intent stage completed" : "Intent stage failed");
    }

    private void appendExecutionRecord(String taskId,
                                       String traceId,
                                       LocalDateTime startTime,
                                       StageStatus status,
                                       NetworkArtifact artifact,
                                       String errorCode,
                                       String errorMessage,
                                       String targetAgentName,
                                       WorkflowStage stage,
                                       String a2aSummary,
                                       String message) {
        LocalDateTime finishTime = LocalDateTime.now();
        String effectiveSummary = a2aSummary != null ? a2aSummary
                : (status == StageStatus.SUCCESS
                        ? targetAgentName + " A2A call succeeded"
                        : targetAgentName + " A2A call failed");
        String effectiveMessage = message != null ? message
                : (status == StageStatus.SUCCESS ? stage.name() + " stage completed" : stage.name() + " stage failed");
        AgentExecutionRecord record = AgentExecutionRecord.builder()
                .recordId("agent-record-" + UUID.randomUUID())
                .taskId(taskId)
                .traceId(traceId)
                .agentName(ORCHESTRATOR_AGENT)
                .targetAgentName(targetAgentName)
                .remoteCallType("SAA_A2A")
                .stage(stage)
                .stageStatus(status)
                .outputArtifactIds(artifact == null ? List.of() : List.of(artifact.getArtifactId()))
                .a2aCallSummaries(List.of(effectiveSummary))
                .startTime(startTime)
                .finishTime(finishTime)
                .durationMs(Duration.between(startTime, finishTime).toMillis())
                .inputSummary("Run " + stage.name() + " stage")
                .outputSummary(artifact == null ? null : artifact.getPayloadSummary())
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .message(effectiveMessage)
                .build();
        executionRecordService.appendRecord(taskId, record);
    }

    private void markTaskErrorAndRecord(String taskId,
                                        String traceId,
                                        LocalDateTime startTime,
                                        String errorCode,
                                        String message) {
        try {
            workspaceService.updateTaskStatus(taskId, TaskStatus.ERROR);
            appendExecutionRecord(taskId, traceId, startTime, StageStatus.FAILED, null,
                    errorCode, message);
        }
        catch (RuntimeException ignored) {
            // Preserve the original failure while avoiding secondary error noise.
        }
    }
}