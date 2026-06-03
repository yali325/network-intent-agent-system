package com.yali.mactav.orchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.execution.config.ExecutionProperties;
import com.yali.mactav.execution.registry.ExecutionAdapterRegistryFactory;
import com.yali.mactav.execution.service.DefaultExecutionService;
import com.yali.mactav.execution.service.ExecutionService;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.a2a.A2aResponse;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.enums.TaskStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.ConfigurationAgentInvokePayload;
import com.yali.mactav.model.config.DeviceConfig;
import com.yali.mactav.model.config.GenerationSource;
import com.yali.mactav.model.execution.ExecutionEnvironmentType;
import com.yali.mactav.model.execution.ExecutionMode;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.execution.ExecutionStatus;
import com.yali.mactav.model.intent.IntentAgentInvokePayload;
import com.yali.mactav.model.intent.IntentNode;
import com.yali.mactav.model.intent.IntentRelation;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.PlanningAgentInvokePayload;
import com.yali.mactav.model.task.NetworkTask;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.model.verification.VerificationAgentInvokePayload;
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

    private static final String CONFIGURATION_AGENT = "ConfigurationAgent";

    private static final String EXECUTION_MODULE = "ExecutionModule";

    private static final String VERIFICATION_AGENT = "VerificationAgent";

    private final NetworkWorkspaceService workspaceService;

    private final AgentExecutionRecordService executionRecordService;

    private final RemoteAgentInvoker remoteAgentInvoker;

    private final ExecutionService executionService;

    private final ExecutionProperties executionProperties;

    private final ObjectMapper objectMapper;

    private final ConcurrentMap<String, String> targetEnvironmentHints = new ConcurrentHashMap<>();

    public MacTavWorkflowOrchestrator(NetworkWorkspaceService workspaceService,
                                      AgentExecutionRecordService executionRecordService,
                                      RemoteAgentInvoker remoteAgentInvoker,
                                      ObjectMapper objectMapper) {
        this(
                workspaceService,
                executionRecordService,
                remoteAgentInvoker,
                objectMapper,
                new DefaultExecutionService(ExecutionAdapterRegistryFactory.structureValidationRegistry()),
                new ExecutionProperties());
    }

    public MacTavWorkflowOrchestrator(NetworkWorkspaceService workspaceService,
                                      AgentExecutionRecordService executionRecordService,
                                      RemoteAgentInvoker remoteAgentInvoker,
                                      ObjectMapper objectMapper,
                                      ExecutionService executionService) {
        this(workspaceService, executionRecordService, remoteAgentInvoker, objectMapper, executionService,
                new ExecutionProperties());
    }

    public MacTavWorkflowOrchestrator(NetworkWorkspaceService workspaceService,
                                      AgentExecutionRecordService executionRecordService,
                                      RemoteAgentInvoker remoteAgentInvoker,
                                      ObjectMapper objectMapper,
                                      ExecutionService executionService,
                                      ExecutionProperties executionProperties) {
        this.workspaceService = workspaceService;
        this.executionRecordService = executionRecordService;
        this.remoteAgentInvoker = remoteAgentInvoker;
        this.objectMapper = objectMapper;
        this.executionService = executionService;
        this.executionProperties = executionProperties == null ? new ExecutionProperties() : executionProperties;
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
            markTaskErrorAndRecord(taskId, traceId, startTime,
                    INTENT_AGENT, WorkflowStage.INTENT, ex.getErrorCode(), ex.getMessage());
            throw ex;
        }
        catch (RuntimeException ex) {
            markTaskErrorAndRecord(taskId, traceId, startTime,
                    INTENT_AGENT, WorkflowStage.INTENT, ErrorCode.AGENT_EXECUTION_FAILED.name(), ex.getMessage());
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
            markTaskErrorAndRecord(taskId, traceId, startTime,
                    PLANNING_AGENT, WorkflowStage.PLANNING, ex.getErrorCode(), ex.getMessage());
            throw ex;
        }
        catch (RuntimeException ex) {
            markTaskErrorAndRecord(taskId, traceId, startTime,
                    PLANNING_AGENT, WorkflowStage.PLANNING, ErrorCode.AGENT_EXECUTION_FAILED.name(), ex.getMessage());
            throw ex;
        }
    }

    @Override
    public NetworkWorkspace runConfigurationStage(String taskId) {
        LocalDateTime startTime = LocalDateTime.now();
        String traceId = "trace-" + UUID.randomUUID();
        try {
            NetworkWorkspace workspace = workspaceService.getWorkspaceOrThrow(taskId);
            NetworkPlan currentPlan = workspace.getCurrentPlan();
            if (currentPlan == null) {
                throw new BusinessException(ErrorCode.STAGE_NOT_READY,
                        "No current NetworkPlan found. Run planning stage first for task: " + taskId);
            }
            if (workspace.getCurrentArtifactRefs() == null
                    || !workspace.getCurrentArtifactRefs().containsKey(ArtifactType.NETWORK_PLAN)) {
                throw new BusinessException(ErrorCode.ARTIFACT_NOT_FOUND,
                        "No NETWORK_PLAN artifact found. Run planning stage first for task: " + taskId);
            }
            int configVersion = nextConfigVersion(workspace);
            workspaceService.updateTaskStage(taskId, WorkflowStage.CONFIGURATION);
            workspaceService.updateTaskStatus(taskId, TaskStatus.RUNNING);

            A2aRequest request = buildConfigurationRequest(workspace, currentPlan, configVersion, traceId);
            A2aResponse response = remoteAgentInvoker.invoke(request);
            if (!Boolean.TRUE.equals(response.getSuccess())) {
                throw new BusinessException(
                        response.getErrorCode(),
                        response.getMessage() == null ? "Remote ConfigurationAgent failed" : response.getMessage());
            }
            ConfigSet configSet = parseConfigSet(response.getPayloadJson());
            normalizeConfigSet(configSet, workspace, currentPlan, configVersion);
            NetworkArtifact artifact = workspaceService.saveStageArtifact(
                    taskId,
                    ArtifactType.CONFIG_SET,
                    WorkflowStage.CONFIGURATION,
                    configSet,
                    configSetSummary(configSet),
                    workspace.getTask().getCreatedBy(),
                    configSet.getTraceRefs());
            appendExecutionRecord(taskId, traceId, startTime, StageStatus.SUCCESS,
                    artifact, null, null, CONFIGURATION_AGENT, WorkflowStage.CONFIGURATION, null, null);
            return workspaceService.getWorkspaceOrThrow(taskId);
        }
        catch (BusinessException ex) {
            markTaskErrorAndRecord(taskId, traceId, startTime,
                    CONFIGURATION_AGENT, WorkflowStage.CONFIGURATION, ex.getErrorCode(), ex.getMessage());
            throw ex;
        }
        catch (RuntimeException ex) {
            markTaskErrorAndRecord(taskId, traceId, startTime,
                    CONFIGURATION_AGENT, WorkflowStage.CONFIGURATION,
                    ErrorCode.AGENT_EXECUTION_FAILED.name(), ex.getMessage());
            throw ex;
        }
    }

    @Override
    public NetworkWorkspace runExecutionStage(String taskId) {
        LocalDateTime startTime = LocalDateTime.now();
        String traceId = "trace-" + UUID.randomUUID();
        try {
            NetworkWorkspace workspace = workspaceService.getWorkspaceOrThrow(taskId);
            NetworkPlan currentPlan = workspace.getCurrentPlan();
            if (currentPlan == null) {
                throw new BusinessException(ErrorCode.STAGE_NOT_READY,
                        "No current NetworkPlan found. Run planning stage first for task: " + taskId);
            }
            ConfigSet currentConfigSet = workspace.getCurrentConfigSet();
            if (currentConfigSet == null) {
                throw new BusinessException(ErrorCode.STAGE_NOT_READY,
                        "No current ConfigSet found. Run configuration stage first for task: " + taskId);
            }
            if (workspace.getCurrentArtifactRefs() == null
                    || !workspace.getCurrentArtifactRefs().containsKey(ArtifactType.NETWORK_PLAN)) {
                throw new BusinessException(ErrorCode.ARTIFACT_NOT_FOUND,
                        "No NETWORK_PLAN artifact found. Run planning stage first for task: " + taskId);
            }
            if (!workspace.getCurrentArtifactRefs().containsKey(ArtifactType.CONFIG_SET)) {
                throw new BusinessException(ErrorCode.ARTIFACT_NOT_FOUND,
                        "No CONFIG_SET artifact found. Run configuration stage first for task: " + taskId);
            }
            int executionVersion = nextExecutionVersion(workspace);
            workspaceService.updateTaskStage(taskId, WorkflowStage.EXECUTION);
            workspaceService.updateTaskStatus(taskId, TaskStatus.RUNNING);

            ExecutionMode executionMode = executionProperties.effectiveMode();
            ExecutionEnvironmentType environmentType = executionEnvironmentType(executionMode);
            ExecutionReport report = executionService.execute(
                    taskId,
                    currentPlan,
                    currentConfigSet,
                    executionVersion,
                    environmentType,
                    executionMode,
                    mergeTraceRefs(currentPlan.getTraceRefs(), currentConfigSet.getTraceRefs()),
                    artifactRefsAsStrings(workspace));
            normalizeExecutionReport(report, workspace, currentPlan, currentConfigSet, executionVersion);
            NetworkArtifact artifact = workspaceService.saveStageArtifact(
                    taskId,
                    ArtifactType.EXECUTION_REPORT,
                    WorkflowStage.EXECUTION,
                    report,
                    executionReportSummary(report),
                    workspace.getTask().getCreatedBy(),
                    report.getTraceRefs());
            StageStatus recordStatus = report.getOverallStatus() == ExecutionStatus.FAILED
                    ? StageStatus.FAILED : StageStatus.SUCCESS;
            appendExecutionRecord(taskId, traceId, startTime, recordStatus,
                    artifact, null, null, EXECUTION_MODULE, WorkflowStage.EXECUTION,
                    "ExecutionService produced ExecutionReport via " + report.getEnvironmentType(),
                    "Execution stage completed");
            workspaceService.updateTaskStatus(taskId, TaskStatus.RUNNING);
            return workspaceService.getWorkspaceOrThrow(taskId);
        }
        catch (BusinessException ex) {
            markTaskErrorAndRecord(taskId, traceId, startTime,
                    EXECUTION_MODULE, WorkflowStage.EXECUTION, ex.getErrorCode(), ex.getMessage());
            throw ex;
        }
        catch (RuntimeException ex) {
            markTaskErrorAndRecord(taskId, traceId, startTime,
                    EXECUTION_MODULE, WorkflowStage.EXECUTION,
                    ErrorCode.EXECUTION_ADAPTER_FAILED.getErrorCode(), ex.getMessage());
            throw ex;
        }
    }

    @Override
    public NetworkWorkspace runVerificationStage(String taskId) {
        LocalDateTime startTime = LocalDateTime.now();
        String traceId = "trace-" + UUID.randomUUID();
        try {
            NetworkWorkspace workspace = workspaceService.getWorkspaceOrThrow(taskId);
            NetworkIntent currentIntent = workspace.getCurrentIntent();
            if (currentIntent == null) {
                throw new BusinessException(ErrorCode.STAGE_NOT_READY,
                        "No current NetworkIntent found. Run intent stage first for task: " + taskId);
            }
            NetworkPlan currentPlan = workspace.getCurrentPlan();
            if (currentPlan == null) {
                throw new BusinessException(ErrorCode.STAGE_NOT_READY,
                        "No current NetworkPlan found. Run planning stage first for task: " + taskId);
            }
            ConfigSet currentConfigSet = workspace.getCurrentConfigSet();
            if (currentConfigSet == null) {
                throw new BusinessException(ErrorCode.STAGE_NOT_READY,
                        "No current ConfigSet found. Run configuration stage first for task: " + taskId);
            }
            ExecutionReport currentExecutionReport = workspace.getCurrentExecutionReport();
            if (currentExecutionReport == null) {
                throw new BusinessException(ErrorCode.STAGE_NOT_READY,
                        "No current ExecutionReport found. Run execution stage first for task: " + taskId);
            }
            requireArtifact(workspace, ArtifactType.NETWORK_INTENT,
                    "No NETWORK_INTENT artifact found. Run intent stage first for task: " + taskId);
            requireArtifact(workspace, ArtifactType.NETWORK_PLAN,
                    "No NETWORK_PLAN artifact found. Run planning stage first for task: " + taskId);
            requireArtifact(workspace, ArtifactType.CONFIG_SET,
                    "No CONFIG_SET artifact found. Run configuration stage first for task: " + taskId);
            requireArtifact(workspace, ArtifactType.EXECUTION_REPORT,
                    "No EXECUTION_REPORT artifact found. Run execution stage first for task: " + taskId);

            int validationVersion = nextValidationVersion(workspace);
            workspaceService.updateTaskStage(taskId, WorkflowStage.VERIFICATION);
            workspaceService.updateTaskStatus(taskId, TaskStatus.RUNNING);

            A2aRequest request = buildVerificationRequest(
                    workspace,
                    currentIntent,
                    currentPlan,
                    currentConfigSet,
                    currentExecutionReport,
                    validationVersion,
                    traceId);
            A2aResponse response = remoteAgentInvoker.invoke(request);
            if (!Boolean.TRUE.equals(response.getSuccess())) {
                throw new BusinessException(
                        response.getErrorCode(),
                        response.getMessage() == null ? "Remote VerificationAgent failed" : response.getMessage());
            }
            ValidationReport report = parseValidationReport(response.getPayloadJson());
            normalizeValidationReport(report, workspace, currentExecutionReport, validationVersion);
            NetworkArtifact artifact = workspaceService.saveStageArtifact(
                    taskId,
                    ArtifactType.VALIDATION_REPORT,
                    WorkflowStage.VERIFICATION,
                    report,
                    validationReportSummary(report),
                    workspace.getTask().getCreatedBy(),
                    report.getTraceRefs());
            appendExecutionRecord(taskId, traceId, startTime, StageStatus.SUCCESS,
                    artifact, null, null, VERIFICATION_AGENT, WorkflowStage.VERIFICATION,
                    "VerificationAgent A2A call succeeded",
                    "Verification stage completed");
            workspaceService.updateTaskStatus(taskId, TaskStatus.RUNNING);
            return workspaceService.getWorkspaceOrThrow(taskId);
        }
        catch (BusinessException ex) {
            markTaskErrorAndRecord(taskId, traceId, startTime,
                    VERIFICATION_AGENT, WorkflowStage.VERIFICATION, ex.getErrorCode(), ex.getMessage());
            throw ex;
        }
        catch (RuntimeException ex) {
            markTaskErrorAndRecord(taskId, traceId, startTime,
                    VERIFICATION_AGENT, WorkflowStage.VERIFICATION,
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

    private A2aRequest buildConfigurationRequest(NetworkWorkspace workspace, NetworkPlan currentPlan,
                                                 int configVersion, String traceId) {
        String planJson = serialize(currentPlan,
                ErrorCode.AGENT_PARSE_FAILED,
                "Failed to serialize current NetworkPlan for configuration request");
        ConfigurationAgentInvokePayload payload = ConfigurationAgentInvokePayload.builder()
                .taskId(workspace.getTask().getTaskId())
                .rawText(workspace.getTask().getRawText())
                .intentVersion(workspace.getCurrentIntentVersion())
                .planVersion(workspace.getCurrentPlanVersion())
                .planJson(planJson)
                .configVersion(configVersion)
                .traceId(traceId)
                .userContext(targetEnvironmentHints.get(workspace.getTask().getTaskId()))
                .workspaceSnapshot(workspaceSnapshot(workspace))
                .targetEnvironmentHint(targetEnvironmentHints.get(workspace.getTask().getTaskId()))
                .createdBy(workspace.getTask().getCreatedBy())
                .build();
        return A2aRequest.builder()
                .taskId(workspace.getTask().getTaskId())
                .sourceAgent(ORCHESTRATOR_AGENT)
                .targetAgent(CONFIGURATION_AGENT)
                .stage(WorkflowStage.CONFIGURATION)
                .artifactVersion(configVersion)
                .payloadJson(serialize(payload, ErrorCode.A2A_CALL_FAILED,
                        "Configuration A2A payload serialization failed"))
                .traceId(traceId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private A2aRequest buildVerificationRequest(NetworkWorkspace workspace,
                                                NetworkIntent currentIntent,
                                                NetworkPlan currentPlan,
                                                ConfigSet currentConfigSet,
                                                ExecutionReport currentExecutionReport,
                                                int validationVersion,
                                                String traceId) {
        VerificationAgentInvokePayload payload = VerificationAgentInvokePayload.builder()
                .taskId(workspace.getTask().getTaskId())
                .rawText(workspace.getTask().getRawText())
                .intentVersion(workspace.getCurrentIntentVersion())
                .planVersion(workspace.getCurrentPlanVersion())
                .configVersion(workspace.getCurrentConfigVersion())
                .executionVersion(workspace.getCurrentExecutionVersion())
                .validationVersion(validationVersion)
                .intentJson(serialize(currentIntent, ErrorCode.AGENT_PARSE_FAILED,
                        "Failed to serialize current NetworkIntent for verification request"))
                .planJson(serialize(currentPlan, ErrorCode.AGENT_PARSE_FAILED,
                        "Failed to serialize current NetworkPlan for verification request"))
                .configSetJson(serialize(currentConfigSet, ErrorCode.AGENT_PARSE_FAILED,
                        "Failed to serialize current ConfigSet for verification request"))
                .executionReportJson(serialize(currentExecutionReport, ErrorCode.AGENT_PARSE_FAILED,
                        "Failed to serialize current ExecutionReport for verification request"))
                .traceId(traceId)
                .userContext(targetEnvironmentHints.get(workspace.getTask().getTaskId()))
                .workspaceSnapshot(workspaceSnapshot(workspace))
                .createdBy(workspace.getTask().getCreatedBy())
                .build();
        return A2aRequest.builder()
                .taskId(workspace.getTask().getTaskId())
                .sourceAgent(ORCHESTRATOR_AGENT)
                .targetAgent(VERIFICATION_AGENT)
                .stage(WorkflowStage.VERIFICATION)
                .artifactVersion(validationVersion)
                .payloadJson(serialize(payload, ErrorCode.A2A_CALL_FAILED,
                        "Verification A2A payload serialization failed"))
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

    private ConfigSet parseConfigSet(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, ConfigSet.class);
        }
        catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.A2A_RESPONSE_INVALID, "ConfigurationAgent payload JSON is invalid", ex);
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

    private ValidationReport parseValidationReport(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, ValidationReport.class);
        }
        catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.A2A_RESPONSE_INVALID, "VerificationAgent payload JSON is invalid", ex);
        }
    }

    private int nextConfigVersion(NetworkWorkspace workspace) {
        Integer current = workspace.getCurrentConfigVersion();
        return current == null ? 1 : current + 1;
    }

    private int nextExecutionVersion(NetworkWorkspace workspace) {
        Integer current = workspace.getCurrentExecutionVersion();
        return current == null ? 1 : current + 1;
    }

    private int nextValidationVersion(NetworkWorkspace workspace) {
        Integer current = workspace.getCurrentValidationVersion();
        return current == null ? 1 : current + 1;
    }

    private ExecutionEnvironmentType executionEnvironmentType(ExecutionMode executionMode) {
        return executionMode == ExecutionMode.MININET_RYU
                ? ExecutionEnvironmentType.MININET_RYU
                : ExecutionEnvironmentType.STRUCTURE_VALIDATION;
    }

    private void normalizeConfigSet(ConfigSet configSet, NetworkWorkspace workspace,
                                    NetworkPlan currentPlan, int configVersion) {
        if (configSet.getTaskId() == null || configSet.getTaskId().isBlank()) {
            configSet.setTaskId(workspace.getTask().getTaskId());
        }
        if (configSet.getPlanVersion() == null) {
            configSet.setPlanVersion(currentPlan.getPlanVersion());
        }
        if (configSet.getConfigVersion() == null) {
            configSet.setConfigVersion(configVersion);
        }
        if (configSet.getTraceRefs() == null) {
            configSet.setTraceRefs(currentPlan.getTraceRefs());
        }
    }

    private String configSetSummary(ConfigSet configSet) {
        int deviceCount = configSet.getDeviceConfigs() == null ? 0 : configSet.getDeviceConfigs().size();
        int commandBlockCount = configSet.getDeviceConfigs() == null ? 0 : configSet.getDeviceConfigs().stream()
                .map(DeviceConfig::getCommandBlocks)
                .filter(blocks -> blocks != null)
                .mapToInt(List::size)
                .sum();
        String sourceTypes = configSet.getGenerationSources() == null ? "" : configSet.getGenerationSources().stream()
                .map(GenerationSource::getSourceType)
                .filter(sourceType -> sourceType != null)
                .map(Enum::name)
                .distinct()
                .toList()
                .toString();
        return "ConfigSet version " + configSet.getConfigVersion()
                + ", devices=" + deviceCount
                + ", commandBlocks=" + commandBlockCount
                + ", generationSources=" + sourceTypes;
    }

    private void normalizeExecutionReport(ExecutionReport report,
                                          NetworkWorkspace workspace,
                                          NetworkPlan currentPlan,
                                          ConfigSet currentConfigSet,
                                          int executionVersion) {
        if (report.getTaskId() == null || report.getTaskId().isBlank()) {
            report.setTaskId(workspace.getTask().getTaskId());
        }
        if (report.getPlanId() == null || report.getPlanId().isBlank()) {
            report.setPlanId(currentPlan.getPlanId());
        }
        if (report.getConfigSetId() == null || report.getConfigSetId().isBlank()) {
            report.setConfigSetId(currentConfigSet.getConfigSetId());
        }
        if (report.getPlanVersion() == null) {
            report.setPlanVersion(workspace.getCurrentPlanVersion());
        }
        if (report.getConfigVersion() == null) {
            report.setConfigVersion(workspace.getCurrentConfigVersion());
        }
        if (report.getExecutionVersion() == null) {
            report.setExecutionVersion(executionVersion);
        }
        if (report.getTraceRefs() == null) {
            report.setTraceRefs(mergeTraceRefs(currentPlan.getTraceRefs(), currentConfigSet.getTraceRefs()));
        }
    }

    private String executionReportSummary(ExecutionReport report) {
        int testCount = report.getTestResults() == null ? 0 : report.getTestResults().size();
        int errorCount = report.getErrors() == null ? 0 : report.getErrors().size();
        return "ExecutionReport version " + report.getExecutionVersion()
                + ", status=" + report.getOverallStatus()
                + ", environment=" + report.getEnvironmentType()
                + ", tests=" + testCount
                + ", errors=" + errorCount;
    }

    private void normalizeValidationReport(ValidationReport report,
                                           NetworkWorkspace workspace,
                                           ExecutionReport currentExecutionReport,
                                           int validationVersion) {
        if (report.getTaskId() == null || report.getTaskId().isBlank()) {
            report.setTaskId(workspace.getTask().getTaskId());
        }
        if (report.getExecutionId() == null || report.getExecutionId().isBlank()) {
            report.setExecutionId(currentExecutionReport.getExecutionId());
        }
        if (report.getIntentVersion() == null) {
            report.setIntentVersion(workspace.getCurrentIntentVersion());
        }
        if (report.getPlanVersion() == null) {
            report.setPlanVersion(workspace.getCurrentPlanVersion());
        }
        if (report.getConfigVersion() == null) {
            report.setConfigVersion(workspace.getCurrentConfigVersion());
        }
        if (report.getExecutionVersion() == null) {
            report.setExecutionVersion(workspace.getCurrentExecutionVersion());
        }
        if (report.getValidationVersion() == null) {
            report.setValidationVersion(validationVersion);
        }
        if (report.getValidationId() == null || report.getValidationId().isBlank()) {
            report.setValidationId("validation-" + workspace.getTask().getTaskId() + "-v" + report.getValidationVersion());
        }
        if (report.getTraceRefs() == null) {
            report.setTraceRefs(validationTraceRefs(report, currentExecutionReport));
        }
    }

    private String validationReportSummary(ValidationReport report) {
        int itemCount = report.getItems() == null ? 0 : report.getItems().size();
        int evidenceCount = report.getEvidences() == null ? 0 : report.getEvidences().size();
        return "ValidationReport version " + report.getValidationVersion()
                + ", status=" + report.getOverallStatus()
                + ", items=" + itemCount
                + ", evidences=" + evidenceCount;
    }

    private TraceRefs validationTraceRefs(ValidationReport report, ExecutionReport executionReport) {
        TraceRefs refs = TraceRefs.builder().build();
        mergeInto(refs, executionReport == null ? null : executionReport.getTraceRefs());
        if (report.getItems() != null) {
            report.getItems().forEach(item -> {
                if (item != null) {
                    addOne(refs.getValidationItemIds(), item.getItemId());
                    addOne(refs.getIntentRelationIds(), item.getRelatedIntentRelationId());
                    addAll(refs.getPlanElementIds(), item.getRelatedPlanElementIds());
                    addAll(refs.getConfigBlockIds(), item.getRelatedConfigBlockIds());
                    addOne(refs.getTestIds(), item.getRelatedTestId());
                }
            });
        }
        return refs;
    }

    private void requireArtifact(NetworkWorkspace workspace, ArtifactType artifactType, String message) {
        if (workspace.getCurrentArtifactRefs() == null
                || !workspace.getCurrentArtifactRefs().containsKey(artifactType)) {
            throw new BusinessException(ErrorCode.ARTIFACT_NOT_FOUND, message);
        }
    }

    private java.util.Map<String, String> artifactRefsAsStrings(NetworkWorkspace workspace) {
        java.util.Map<String, String> refs = new java.util.LinkedHashMap<>();
        if (workspace.getCurrentArtifactRefs() == null) {
            return refs;
        }
        workspace.getCurrentArtifactRefs().forEach((type, id) -> {
            if (type != null && id != null && !id.isBlank()) {
                refs.put(type.name(), id);
            }
        });
        return refs;
    }

    private TraceRefs mergeTraceRefs(TraceRefs first, TraceRefs second) {
        TraceRefs merged = TraceRefs.builder().build();
        mergeInto(merged, first);
        mergeInto(merged, second);
        return merged;
    }

    private void mergeInto(TraceRefs target, TraceRefs source) {
        if (source == null) {
            return;
        }
        addAll(target.getIntentNodeIds(), source.getIntentNodeIds());
        addAll(target.getIntentRelationIds(), source.getIntentRelationIds());
        addAll(target.getPlanElementIds(), source.getPlanElementIds());
        addAll(target.getConfigBlockIds(), source.getConfigBlockIds());
        addAll(target.getTestIds(), source.getTestIds());
        addAll(target.getValidationItemIds(), source.getValidationItemIds());
        addAll(target.getRepairActionIds(), source.getRepairActionIds());
    }

    private void addAll(List<String> target, List<String> source) {
        if (source == null) {
            return;
        }
        for (String value : source) {
            addOne(target, value);
        }
    }

    private void addOne(List<String> target, String value) {
        if (target != null && value != null && !value.isBlank() && !target.contains(value)) {
            target.add(value);
        }
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
                                        String targetAgentName,
                                        WorkflowStage stage,
                                        String errorCode,
                                        String message) {
        try {
            workspaceService.updateTaskStatus(taskId, TaskStatus.ERROR);
            appendExecutionRecord(taskId, traceId, startTime, StageStatus.FAILED, null,
                    errorCode, message, targetAgentName, stage, null, null);
        }
        catch (RuntimeException ignored) {
            // Preserve the original failure while avoiding secondary error noise.
        }
    }
}
