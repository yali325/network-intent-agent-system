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
import com.yali.mactav.model.enums.RepairStatus;
import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.enums.TaskStatus;
import com.yali.mactav.model.enums.ValidationStatus;
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
import com.yali.mactav.model.healing.FailureAnalysis;
import com.yali.mactav.model.healing.HealingAgentInvokePayload;
import com.yali.mactav.model.healing.RepairAction;
import com.yali.mactav.model.healing.RepairPlan;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.PlanningAgentInvokePayload;
import com.yali.mactav.model.task.NetworkTask;
import com.yali.mactav.model.verification.ValidationItem;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.model.verification.VerificationAgentInvokePayload;
import com.yali.mactav.model.workspace.AgentExecutionRecord;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.model.workspace.TraceRefs;
import com.yali.mactav.model.workspace.WorkspaceChangeRecord;
import com.yali.mactav.modelcore.event.WorkspaceEventFactory;
import com.yali.mactav.modelcore.service.AgentExecutionRecordService;
import com.yali.mactav.modelcore.service.NetworkWorkspaceService;
import com.yali.mactav.modelcore.service.WorkspaceChangeRecordService;
import com.yali.mactav.orchestrator.remote.invoker.RemoteAgentInvoker;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deterministic MAC-TAV workflow coordinator for Intent and Planning stage closure.
 *
 * <p>The orchestrator creates workspaces, invokes remote agents through
 * RemoteAgentInvoker, and writes stage artifacts through Model Core. It does not
 * construct prompts, call ChatModel/ReactAgent, or depend on concrete agent
 * modules.</p>
 */
public class MacTavWorkflowOrchestrator implements WorkflowOrchestrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MacTavWorkflowOrchestrator.class);

    private static final String ORCHESTRATOR_AGENT = "MacTavOrchestrator";

    private static final String INTENT_AGENT = "IntentAgent";

    private static final String PLANNING_AGENT = "PlanningAgent";

    private static final String CONFIGURATION_AGENT = "ConfigurationAgent";

    private static final String EXECUTION_MODULE = "ExecutionModule";

    private static final String VERIFICATION_AGENT = "VerificationAgent";

    private static final String HEALING_AGENT = "HealingAgent";

    private final NetworkWorkspaceService workspaceService;

    private final AgentExecutionRecordService executionRecordService;

    private final WorkspaceChangeRecordService changeRecordService;

    private final RemoteAgentInvoker remoteAgentInvoker;

    private final ExecutionService executionService;

    private final ExecutionProperties executionProperties;

    private final ObjectMapper objectMapper;

    private final ConcurrentMap<String, String> targetEnvironmentHints = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, String> repairGuidanceHints = new ConcurrentHashMap<>();

    public MacTavWorkflowOrchestrator(NetworkWorkspaceService workspaceService,
                                      AgentExecutionRecordService executionRecordService,
                                      RemoteAgentInvoker remoteAgentInvoker,
                                      ObjectMapper objectMapper) {
        this(
                workspaceService,
                executionRecordService,
                null,
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
        this(workspaceService, executionRecordService, null, remoteAgentInvoker, objectMapper, executionService,
                new ExecutionProperties());
    }

    public MacTavWorkflowOrchestrator(NetworkWorkspaceService workspaceService,
                                      AgentExecutionRecordService executionRecordService,
                                      RemoteAgentInvoker remoteAgentInvoker,
                                      ObjectMapper objectMapper,
                                      ExecutionService executionService,
                                      ExecutionProperties executionProperties) {
        this(workspaceService, executionRecordService, null, remoteAgentInvoker, objectMapper, executionService,
                executionProperties);
    }

    public MacTavWorkflowOrchestrator(NetworkWorkspaceService workspaceService,
                                      AgentExecutionRecordService executionRecordService,
                                      WorkspaceChangeRecordService changeRecordService,
                                      RemoteAgentInvoker remoteAgentInvoker,
                                      ObjectMapper objectMapper,
                                      ExecutionService executionService,
                                      ExecutionProperties executionProperties) {
        this.workspaceService = workspaceService;
        this.executionRecordService = executionRecordService;
        this.changeRecordService = changeRecordService;
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
    public NetworkWorkspace runHealingStage(String taskId) {
        LocalDateTime startTime = LocalDateTime.now();
        String traceId = "trace-" + UUID.randomUUID();
        try {
            NetworkWorkspace workspace = workspaceService.getWorkspaceOrThrow(taskId);
            ValidationReport validationReport = workspace.getCurrentValidationReport();
            if (validationReport == null) {
                throw new BusinessException(ErrorCode.STAGE_NOT_READY,
                        "No current ValidationReport found. Run verification stage first for task: " + taskId);
            }
            if (!requiresHealing(validationReport.getOverallStatus())) {
                throw new BusinessException(ErrorCode.STAGE_NOT_READY,
                        "ValidationReport status " + validationReport.getOverallStatus()
                                + " does not require healing for task: " + taskId);
            }
            requireArtifact(workspace, ArtifactType.VALIDATION_REPORT,
                    "No VALIDATION_REPORT artifact found. Run verification stage first for task: " + taskId);

            int repairVersion = nextRepairVersion(workspace);
            workspaceService.updateTaskStage(taskId, WorkflowStage.HEALING);
            workspaceService.updateTaskStatus(taskId, TaskStatus.RUNNING);

            A2aRequest request = buildHealingRequest(workspace, validationReport, repairVersion, traceId);
            A2aResponse response = remoteAgentInvoker.invoke(request);
            if (!Boolean.TRUE.equals(response.getSuccess())) {
                throw new BusinessException(
                        response.getErrorCode(),
                        response.getMessage() == null ? "Remote HealingAgent failed" : response.getMessage());
            }
            RepairPlan repairPlan = parseRepairPlan(response.getPayloadJson());
            normalizeRepairPlan(repairPlan, workspace, validationReport, repairVersion);
            NetworkArtifact artifact = workspaceService.saveStageArtifact(
                    taskId,
                    ArtifactType.REPAIR_PLAN,
                    WorkflowStage.HEALING,
                    repairPlan,
                    repairPlanSummary(repairPlan),
                    workspace.getTask().getCreatedBy(),
                    repairTraceRefs(repairPlan, validationReport));
            appendExecutionRecord(taskId, traceId, startTime, StageStatus.SUCCESS,
                    artifact, null, null, HEALING_AGENT, WorkflowStage.HEALING,
                    "HealingAgent A2A call succeeded",
                    "Healing stage completed");
            workspaceService.updateTaskStatus(taskId,
                    Boolean.TRUE.equals(repairPlan.getRequiresUserConfirmation())
                            ? TaskStatus.WAITING_USER
                            : TaskStatus.RUNNING);
            return workspaceService.getWorkspaceOrThrow(taskId);
        }
        catch (BusinessException ex) {
            markTaskErrorAndRecord(taskId, traceId, startTime,
                    HEALING_AGENT, WorkflowStage.HEALING, ex.getErrorCode(), ex.getMessage());
            throw ex;
        }
        catch (RuntimeException ex) {
            markTaskErrorAndRecord(taskId, traceId, startTime,
                    HEALING_AGENT, WorkflowStage.HEALING,
                    ErrorCode.AGENT_EXECUTION_FAILED.name(), ex.getMessage());
            throw ex;
        }
    }

    @Override
    public NetworkWorkspace approveRepairAction(String taskId, String actionId, String approvedBy, String comment) {
        RepairPlan repairPlan = requireCurrentRepairPlan(taskId);
        RepairAction action = requireRepairAction(repairPlan, actionId);
        action.setStatus(RepairStatus.APPROVED);
        action.setApprovalStatus("APPROVED");
        action.setApprovedBy(blankToDefault(approvedBy, "api"));
        action.setApprovedAt(LocalDateTime.now());
        action.setApprovalComment(comment);
        NetworkWorkspace workspace = workspaceService.saveCurrentRepairPlan(taskId, repairPlan);
        appendRepairChange(taskId, action, "REPAIR_APPROVED", comment, action.getApprovedBy());
        workspace = workspaceService.appendWorkspaceEvent(
                taskId,
                WorkspaceEventFactory.repairApproved(taskId, action, comment));
        return workspace;
    }

    @Override
    public NetworkWorkspace rejectRepairAction(String taskId, String actionId, String rejectedBy, String comment) {
        RepairPlan repairPlan = requireCurrentRepairPlan(taskId);
        RepairAction action = requireRepairAction(repairPlan, actionId);
        action.setStatus(RepairStatus.REJECTED);
        action.setApprovalStatus("REJECTED");
        action.setRejectedBy(blankToDefault(rejectedBy, "api"));
        action.setRejectedAt(LocalDateTime.now());
        action.setApprovalComment(comment);
        NetworkWorkspace workspace = workspaceService.saveCurrentRepairPlan(taskId, repairPlan);
        appendRepairChange(taskId, action, "REPAIR_REJECTED", comment, action.getRejectedBy());
        workspace = workspaceService.appendWorkspaceEvent(
                taskId,
                WorkspaceEventFactory.repairRejected(taskId, action, comment));
        return workspace;
    }

    @Override
    public NetworkWorkspace applyRepairAction(String taskId, String actionId) {
        RepairPlan repairPlan = requireCurrentRepairPlan(taskId);
        RepairAction action = requireRepairAction(repairPlan, actionId);
        validateRepairActionCanApply(action);
        if (isRollback(action)) {
            throw new BusinessException(ErrorCode.WORKSPACE_STATE_INVALID,
                    "ROLLBACK repair action is not supported until artifact version switching is implemented");
        }
        WorkflowStage targetStage = repairTargetStage(action);
        String guidance = repairGuidance(action);
        action.setStatus(RepairStatus.APPLIED);
        action.setAppliedAt(LocalDateTime.now());
        workspaceService.saveCurrentRepairPlan(taskId, repairPlan);
        appendRepairChange(taskId, action, "REPAIR_APPLIED", guidance, ORCHESTRATOR_AGENT);
        workspaceService.appendWorkspaceEvent(taskId, WorkspaceEventFactory.repairApplied(taskId, action, guidance));

        if (isAskUser(action)) {
            workspaceService.updateTaskStatus(taskId, TaskStatus.WAITING_USER);
            return workspaceService.appendWorkspaceEvent(
                    taskId,
                    WorkspaceEventFactory.repairWaitingUser(taskId, action, guidance));
        }

        repairGuidanceHints.put(taskId, guidance);
        try {
            if (targetStage == WorkflowStage.PLANNING) {
                return runPlanningStage(taskId);
            }
            if (targetStage == WorkflowStage.CONFIGURATION) {
                return runConfigurationStage(taskId);
            }
            if (targetStage == WorkflowStage.EXECUTION) {
                return runExecutionStage(taskId);
            }
            if (targetStage == WorkflowStage.VERIFICATION) {
                return runVerificationStage(taskId);
            }
            throw new BusinessException(ErrorCode.WORKSPACE_STATE_INVALID,
                    "Unsupported repair target stage for action: " + actionId);
        }
        finally {
            repairGuidanceHints.remove(taskId);
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
        String compactWorkspaceSnapshot = compactPlanningWorkspaceSnapshot(workspace, currentIntent);
        PlanningAgentInvokePayload payload = PlanningAgentInvokePayload.builder()
                .taskId(workspace.getTask().getTaskId())
                .rawText(workspace.getTask().getRawText())
                .intentVersion(workspace.getCurrentIntentVersion())
                .intentJson(intentJson)
                .planVersion(planVersion)
                .traceId(traceId)
                .userContext(userContextWithRepairGuidance(workspace.getTask().getTaskId()))
                .workspaceSnapshot(compactWorkspaceSnapshot)
                .targetEnvironmentHint(targetEnvironmentHints.get(workspace.getTask().getTaskId()))
                .createdBy(workspace.getTask().getCreatedBy())
                .build();
        String payloadJson = serialize(payload, ErrorCode.A2A_CALL_FAILED,
                "Planning A2A payload serialization failed");
        LOGGER.info(
                "Planning A2A payload compacted taskId={}, traceId={}, intentJsonLength={}, compactWorkspaceLength={}, payloadLength={}, networkIntentArtifactRefPresent={}",
                workspace.getTask().getTaskId(),
                traceId,
                intentJson.length(),
                compactWorkspaceSnapshot.length(),
                payloadJson.length(),
                workspace.getCurrentArtifactRefs() != null
                        && workspace.getCurrentArtifactRefs().containsKey(ArtifactType.NETWORK_INTENT));
        return A2aRequest.builder()
                .taskId(workspace.getTask().getTaskId())
                .sourceAgent(ORCHESTRATOR_AGENT)
                .targetAgent(PLANNING_AGENT)
                .stage(WorkflowStage.PLANNING)
                .artifactVersion(planVersion)
                .payloadJson(payloadJson)
                .traceId(traceId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private A2aRequest buildConfigurationRequest(NetworkWorkspace workspace, NetworkPlan currentPlan,
                                                 int configVersion, String traceId) {
        String planJson = serialize(currentPlan,
                ErrorCode.AGENT_PARSE_FAILED,
                "Failed to serialize current NetworkPlan for configuration request");
        String compactWorkspaceSnapshot = compactConfigurationWorkspaceSnapshot(workspace, currentPlan);
        ConfigurationAgentInvokePayload payload = ConfigurationAgentInvokePayload.builder()
                .taskId(workspace.getTask().getTaskId())
                .rawText(workspace.getTask().getRawText())
                .intentVersion(workspace.getCurrentIntentVersion())
                .planVersion(workspace.getCurrentPlanVersion())
                .planJson(planJson)
                .configVersion(configVersion)
                .traceId(traceId)
                .userContext(userContextWithRepairGuidance(workspace.getTask().getTaskId()))
                .workspaceSnapshot(compactWorkspaceSnapshot)
                .targetEnvironmentHint(targetEnvironmentHints.get(workspace.getTask().getTaskId()))
                .createdBy(workspace.getTask().getCreatedBy())
                .build();
        String payloadJson = serialize(payload, ErrorCode.A2A_CALL_FAILED,
                "Configuration A2A payload serialization failed");
        LOGGER.info(
                "Configuration A2A payload compacted taskId={}, traceId={}, planJsonLength={}, compactWorkspaceLength={}, payloadLength={}, networkPlanArtifactRefPresent={}",
                workspace.getTask().getTaskId(),
                traceId,
                planJson.length(),
                compactWorkspaceSnapshot.length(),
                payloadJson.length(),
                workspace.getCurrentArtifactRefs() != null
                        && workspace.getCurrentArtifactRefs().containsKey(ArtifactType.NETWORK_PLAN));
        return A2aRequest.builder()
                .taskId(workspace.getTask().getTaskId())
                .sourceAgent(ORCHESTRATOR_AGENT)
                .targetAgent(CONFIGURATION_AGENT)
                .stage(WorkflowStage.CONFIGURATION)
                .artifactVersion(configVersion)
                .payloadJson(payloadJson)
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

    private A2aRequest buildHealingRequest(NetworkWorkspace workspace,
                                           ValidationReport validationReport,
                                           int repairVersion,
                                           String traceId) {
        List<ValidationItem> failedItems = failedValidationItems(validationReport);
        HealingAgentInvokePayload payload = HealingAgentInvokePayload.builder()
                .taskId(workspace.getTask().getTaskId())
                .rawText(workspace.getTask().getRawText())
                .validationVersion(validationReport.getValidationVersion() == null
                        ? workspace.getCurrentValidationVersion()
                        : validationReport.getValidationVersion())
                .repairVersion(repairVersion)
                .validationReportJson(serialize(validationReport, ErrorCode.AGENT_PARSE_FAILED,
                        "Failed to serialize current ValidationReport for healing request"))
                .workspaceSnapshot(workspaceSnapshot(workspace))
                .failedValidationItemIds(failedItems.stream()
                        .map(ValidationItem::getItemId)
                        .filter(id -> id != null && !id.isBlank())
                        .toList())
                .failedValidationItemsJson(serialize(failedItems, ErrorCode.AGENT_PARSE_FAILED,
                        "Failed to serialize failed validation items for healing request"))
                .evidencesJson(serialize(validationReport.getEvidences(), ErrorCode.AGENT_PARSE_FAILED,
                        "Failed to serialize validation evidences for healing request"))
                .suggestions(validationReport.getSuggestions())
                .traceRefsJson(serialize(validationReport.getTraceRefs(), ErrorCode.AGENT_PARSE_FAILED,
                        "Failed to serialize validation trace refs for healing request"))
                .traceId(traceId)
                .userContext(targetEnvironmentHints.get(workspace.getTask().getTaskId()))
                .createdBy(workspace.getTask().getCreatedBy())
                .build();
        return A2aRequest.builder()
                .taskId(workspace.getTask().getTaskId())
                .sourceAgent(ORCHESTRATOR_AGENT)
                .targetAgent(HEALING_AGENT)
                .stage(WorkflowStage.HEALING)
                .artifactVersion(repairVersion)
                .payloadJson(serialize(payload, ErrorCode.A2A_CALL_FAILED,
                        "Healing A2A payload serialization failed"))
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

    private String compactPlanningWorkspaceSnapshot(NetworkWorkspace workspace, NetworkIntent currentIntent) {
        Map<String, Object> compact = new LinkedHashMap<>();
        compact.put("task", compactTask(workspace == null ? null : workspace.getTask()));
        compact.put("currentIntentVersion", workspace == null ? null : workspace.getCurrentIntentVersion());
        compact.put("currentPlanVersion", workspace == null ? null : workspace.getCurrentPlanVersion());
        compact.put("currentStage", workspace == null || workspace.getTask() == null
                ? null
                : workspace.getTask().getCurrentStage());
        compact.put("workspaceStatus", workspace == null ? null : workspace.getWorkspaceStatus());
        compact.put("currentArtifactRefs", compactArtifactRefs(workspace));
        compact.put("currentIntentSummary", compactIntentSummary(currentIntent));
        return serialize(compact,
                ErrorCode.A2A_CALL_FAILED,
                "Failed to serialize compact planning workspace snapshot");
    }

    private String compactConfigurationWorkspaceSnapshot(NetworkWorkspace workspace, NetworkPlan currentPlan) {
        Map<String, Object> compact = new LinkedHashMap<>();
        compact.put("task", compactTask(workspace == null ? null : workspace.getTask()));
        compact.put("currentIntentVersion", workspace == null ? null : workspace.getCurrentIntentVersion());
        compact.put("currentPlanVersion", workspace == null ? null : workspace.getCurrentPlanVersion());
        compact.put("currentConfigVersion", workspace == null ? null : workspace.getCurrentConfigVersion());
        compact.put("currentStage", workspace == null || workspace.getTask() == null
                ? null
                : workspace.getTask().getCurrentStage());
        compact.put("workspaceStatus", workspace == null ? null : workspace.getWorkspaceStatus());
        compact.put("currentArtifactRefs", compactArtifactRefs(workspace));
        compact.put("currentPlanSummary", compactPlanSummary(currentPlan));
        return serialize(compact,
                ErrorCode.A2A_CALL_FAILED,
                "Failed to serialize compact configuration workspace snapshot");
    }

    private Map<String, Object> compactTask(NetworkTask task) {
        Map<String, Object> compact = new LinkedHashMap<>();
        if (task == null) {
            return compact;
        }
        compact.put("taskId", task.getTaskId());
        compact.put("taskStatus", task.getTaskStatus());
        compact.put("currentStage", task.getCurrentStage());
        compact.put("createTime", task.getCreateTime());
        compact.put("updateTime", task.getUpdateTime());
        compact.put("createdBy", task.getCreatedBy());
        compact.put("description", task.getDescription());
        return compact;
    }

    private Map<String, Object> compactArtifactRefs(NetworkWorkspace workspace) {
        Map<String, Object> compact = new LinkedHashMap<>();
        if (workspace == null || workspace.getCurrentArtifactRefs() == null) {
            return compact;
        }
        String intentArtifactRef = workspace.getCurrentArtifactRefs().get(ArtifactType.NETWORK_INTENT);
        if (intentArtifactRef != null && !intentArtifactRef.isBlank()) {
            compact.put(ArtifactType.NETWORK_INTENT.name(), intentArtifactRef);
        }
        String planArtifactRef = workspace.getCurrentArtifactRefs().get(ArtifactType.NETWORK_PLAN);
        if (planArtifactRef != null && !planArtifactRef.isBlank()) {
            compact.put(ArtifactType.NETWORK_PLAN.name(), planArtifactRef);
        }
        return compact;
    }

    private Map<String, Object> compactIntentSummary(NetworkIntent intent) {
        Map<String, Object> compact = new LinkedHashMap<>();
        if (intent == null) {
            return compact;
        }
        compact.put("taskId", intent.getTaskId());
        compact.put("intentVersion", intent.getIntentVersion());
        compact.put("stageStatus", intent.getStageStatus());
        compact.put("traceId", intent.getTraceId());
        compact.put("constraintCount", intent.getConstraints() == null ? 0 : intent.getConstraints().size());
        compact.put("preferenceCount", intent.getPreferences() == null ? 0 : intent.getPreferences().size());
        compact.put("assumptionCount", intent.getAssumptions() == null ? 0 : intent.getAssumptions().size());
        if (intent.getSemanticIntentGraph() != null) {
            compact.put("semanticNodeCount", intent.getSemanticIntentGraph().getNodes() == null
                    ? 0
                    : intent.getSemanticIntentGraph().getNodes().size());
            compact.put("semanticRelationCount", intent.getSemanticIntentGraph().getRelations() == null
                    ? 0
                    : intent.getSemanticIntentGraph().getRelations().size());
        }
        return compact;
    }

    private Map<String, Object> compactPlanSummary(NetworkPlan plan) {
        Map<String, Object> compact = new LinkedHashMap<>();
        if (plan == null) {
            return compact;
        }
        compact.put("taskId", plan.getTaskId());
        compact.put("intentVersion", plan.getIntentVersion());
        compact.put("planVersion", plan.getPlanVersion());
        compact.put("stageStatus", plan.getStageStatus());
        compact.put("planSummary", plan.getPlanSummary());
        compact.put("zoneCount", plan.getZones() == null ? 0 : plan.getZones().size());
        compact.put("addressPlanCount", plan.getAddressPlan() == null ? 0 : plan.getAddressPlan().size());
        compact.put("vlanPlanCount", plan.getVlanPlan() == null ? 0 : plan.getVlanPlan().size());
        compact.put("securityPolicyCount", plan.getSecurityPolicyPlan() == null ? 0 : plan.getSecurityPolicyPlan().size());
        if (plan.getTopology() != null) {
            compact.put("topologyNodeCount", plan.getTopology().getNodes() == null
                    ? 0
                    : plan.getTopology().getNodes().size());
            compact.put("topologyLinkCount", plan.getTopology().getLinks() == null
                    ? 0
                    : plan.getTopology().getLinks().size());
        }
        return compact;
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

    private RepairPlan parseRepairPlan(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, RepairPlan.class);
        }
        catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.A2A_RESPONSE_INVALID, "HealingAgent payload JSON is invalid", ex);
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

    private int nextRepairVersion(NetworkWorkspace workspace) {
        Integer current = workspace.getCurrentRepairVersion();
        return current == null ? 1 : current + 1;
    }

    private boolean requiresHealing(ValidationStatus status) {
        return status == ValidationStatus.FAILED
                || status == ValidationStatus.PARTIAL
                || status == ValidationStatus.UNKNOWN;
    }

    private RepairPlan requireCurrentRepairPlan(String taskId) {
        NetworkWorkspace workspace = workspaceService.getWorkspaceOrThrow(taskId);
        RepairPlan repairPlan = workspace.getCurrentRepairPlan();
        if (repairPlan == null) {
            throw new BusinessException(ErrorCode.REPAIR_PLAN_NOT_FOUND,
                    "Current repair plan not found for task: " + taskId);
        }
        return repairPlan;
    }

    private RepairAction requireRepairAction(RepairPlan repairPlan, String actionId) {
        if (actionId == null || actionId.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "repair actionId must not be blank");
        }
        if (repairPlan.getActions() == null) {
            throw new BusinessException(ErrorCode.REPAIR_ACTION_NOT_FOUND,
                    "Repair action not found: " + actionId);
        }
        return repairPlan.getActions().stream()
                .filter(action -> action != null && actionId.equals(action.getActionId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.REPAIR_ACTION_NOT_FOUND,
                        "Repair action not found: " + actionId));
    }

    private void validateRepairActionCanApply(RepairAction action) {
        if (action.getStatus() == RepairStatus.REJECTED) {
            throw new BusinessException(ErrorCode.WORKSPACE_STATE_INVALID,
                    "Rejected repair action cannot be applied: " + action.getActionId());
        }
        if (requiresApproval(action) && action.getStatus() != RepairStatus.APPROVED) {
            throw new BusinessException(ErrorCode.REPAIR_ACTION_NOT_APPROVED,
                    "Repair action requires approval before apply: " + action.getActionId());
        }
    }

    private boolean requiresApproval(RepairAction action) {
        return Boolean.TRUE.equals(action.getRequiresApproval())
                || "HIGH".equalsIgnoreCase(action.getRiskLevel());
    }

    private WorkflowStage repairTargetStage(RepairAction action) {
        if (action.getTargetStage() != null) {
            return action.getTargetStage();
        }
        String actionType = normalizedActionType(action);
        if ("REPLAN".equals(actionType)) {
            return WorkflowStage.PLANNING;
        }
        if ("REGENERATE_CONFIG".equals(actionType) || "PATCH_CONFIG".equals(actionType)) {
            return WorkflowStage.CONFIGURATION;
        }
        if ("REEXECUTE".equals(actionType)) {
            return WorkflowStage.EXECUTION;
        }
        if ("ASK_USER".equals(actionType) || "ROLLBACK".equals(actionType)) {
            return WorkflowStage.HEALING;
        }
        throw new BusinessException(ErrorCode.WORKSPACE_STATE_INVALID,
                "Unsupported repair actionType: " + action.getActionType());
    }

    private boolean isAskUser(RepairAction action) {
        return "ASK_USER".equals(normalizedActionType(action));
    }

    private boolean isRollback(RepairAction action) {
        return "ROLLBACK".equals(normalizedActionType(action));
    }

    private String normalizedActionType(RepairAction action) {
        return action.getActionType() == null ? "" : action.getActionType().trim().toUpperCase();
    }

    private String repairGuidance(RepairAction action) {
        return "Repair guidance actionId=" + action.getActionId()
                + ", actionType=" + action.getActionType()
                + ", targetStage=" + action.getTargetStage()
                + ", description=" + action.getDescription()
                + ", riskLevel=" + action.getRiskLevel()
                + ", relatedFailureAnalysisId=" + action.getRelatedFailureAnalysisId();
    }

    private String userContextWithRepairGuidance(String taskId) {
        String base = targetEnvironmentHints.get(taskId);
        String guidance = repairGuidanceHints.get(taskId);
        if (guidance == null || guidance.isBlank()) {
            return base;
        }
        if (base == null || base.isBlank()) {
            return guidance;
        }
        return base + "\n" + guidance;
    }

    private void appendRepairChange(String taskId,
                                    RepairAction action,
                                    String changeType,
                                    String reason,
                                    String createdBy) {
        if (changeRecordService == null) {
            return;
        }
        changeRecordService.appendChange(taskId, WorkspaceChangeRecord.builder()
                .changeId("change-" + UUID.randomUUID())
                .taskId(taskId)
                .stage(WorkflowStage.HEALING)
                .changeType(changeType)
                .reason(reason == null || reason.isBlank() ? repairGuidance(action) : reason)
                .createTime(LocalDateTime.now())
                .createdBy(blankToDefault(createdBy, ORCHESTRATOR_AGENT))
                .build());
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
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

    private void normalizeRepairPlan(RepairPlan repairPlan,
                                     NetworkWorkspace workspace,
                                     ValidationReport validationReport,
                                     int repairVersion) {
        if (repairPlan == null) {
            throw new BusinessException(ErrorCode.A2A_RESPONSE_INVALID, "HealingAgent returned empty RepairPlan");
        }
        if (repairPlan.getTaskId() == null || repairPlan.getTaskId().isBlank()) {
            repairPlan.setTaskId(workspace.getTask().getTaskId());
        }
        if (repairPlan.getValidationVersion() == null) {
            repairPlan.setValidationVersion(validationReport.getValidationVersion() == null
                    ? workspace.getCurrentValidationVersion()
                    : validationReport.getValidationVersion());
        }
        if (repairPlan.getRepairVersion() == null) {
            repairPlan.setRepairVersion(repairVersion);
        }
        if (repairPlan.getStageStatus() == null) {
            repairPlan.setStageStatus(StageStatus.SUCCESS);
        }
        if (repairPlan.getCreateTime() == null) {
            repairPlan.setCreateTime(LocalDateTime.now());
        }
        if (repairPlan.getFailureAnalysis() == null) {
            repairPlan.setFailureAnalysis(new java.util.ArrayList<>());
        }
        if (repairPlan.getActions() == null) {
            repairPlan.setActions(new java.util.ArrayList<>());
        }
        for (int i = 0; i < repairPlan.getActions().size(); i++) {
            normalizeRepairAction(repairPlan.getActions().get(i), repairPlan, validationReport, i + 1);
        }
        if (repairPlan.getRequiresUserConfirmation() == null) {
            repairPlan.setRequiresUserConfirmation(repairPlan.getActions().stream()
                    .anyMatch(action -> Boolean.TRUE.equals(action.getRequiresApproval())));
        }
    }

    private void normalizeRepairAction(RepairAction action,
                                       RepairPlan repairPlan,
                                       ValidationReport validationReport,
                                       int index) {
        if (action == null) {
            return;
        }
        if (action.getActionId() == null || action.getActionId().isBlank()) {
            action.setActionId("repair-action-" + index);
        }
        if (action.getStatus() == null) {
            action.setStatus(RepairStatus.PROPOSED);
        }
        if (action.getExpectedOutputArtifactType() == null) {
            action.setExpectedOutputArtifactType(ArtifactType.REPAIR_PLAN);
        }
        if (action.getTraceRefs() == null) {
            action.setTraceRefs(repairTraceRefs(repairPlan, validationReport));
        }
        addOne(action.getTraceRefs().getRepairActionIds(), action.getActionId());
        if (action.getRelatedFailureAnalysisId() == null || action.getRelatedFailureAnalysisId().isBlank()) {
            repairPlan.getFailureAnalysis().stream()
                    .map(FailureAnalysis::getAnalysisId)
                    .filter(id -> id != null && !id.isBlank())
                    .findFirst()
                    .ifPresent(action::setRelatedFailureAnalysisId);
        }
        if ("HIGH".equalsIgnoreCase(action.getRiskLevel())) {
            action.setRequiresApproval(true);
        }
    }

    private String repairPlanSummary(RepairPlan repairPlan) {
        int analysisCount = repairPlan.getFailureAnalysis() == null ? 0 : repairPlan.getFailureAnalysis().size();
        int actionCount = repairPlan.getActions() == null ? 0 : repairPlan.getActions().size();
        return "RepairPlan version " + repairPlan.getRepairVersion()
                + ", validationVersion=" + repairPlan.getValidationVersion()
                + ", analyses=" + analysisCount
                + ", actions=" + actionCount
                + ", requiresUserConfirmation=" + repairPlan.getRequiresUserConfirmation();
    }

    private TraceRefs repairTraceRefs(RepairPlan repairPlan, ValidationReport validationReport) {
        TraceRefs refs = TraceRefs.builder().build();
        mergeInto(refs, validationReport == null ? null : validationReport.getTraceRefs());
        if (repairPlan == null) {
            return refs;
        }
        if (repairPlan.getActions() != null) {
            repairPlan.getActions().forEach(action -> {
                if (action != null) {
                    mergeInto(refs, action.getTraceRefs());
                    addOne(refs.getRepairActionIds(), action.getActionId());
                }
            });
        }
        return refs;
    }

    private List<ValidationItem> failedValidationItems(ValidationReport validationReport) {
        if (validationReport.getItems() == null) {
            return List.of();
        }
        return validationReport.getItems().stream()
                .filter(item -> item != null && !Boolean.TRUE.equals(item.getPassed()))
                .toList();
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
