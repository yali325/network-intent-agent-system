package com.yali.mactav.modelcore.impl;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.enums.StageStatus;
import com.yali.mactav.common.enums.TaskStatus;
import com.yali.mactav.common.enums.ValidationStatus;
import com.yali.mactav.common.enums.WorkflowStage;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.common.util.IdGenerator;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.task.NetworkTask;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.model.workspace.AgentStepLog;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.modelcore.repository.InMemoryWorkspaceRepository;
import com.yali.mactav.modelcore.service.NetworkWorkspaceService;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class NetworkWorkspaceServiceImpl implements NetworkWorkspaceService {

    private static final int INITIAL_VERSION = 0;
    private static final int DEFAULT_ARTIFACT_VERSION = 1;
    private static final String MODEL_CORE_AGENT = "ModelCore";

    private final InMemoryWorkspaceRepository repository;

    public NetworkWorkspaceServiceImpl() {
        this(new InMemoryWorkspaceRepository());
    }

    public NetworkWorkspaceServiceImpl(InMemoryWorkspaceRepository repository) {
        this.repository = repository;
    }

    @Override
    public NetworkWorkspace createTask(String rawText) {
        String taskId = "task-" + IdGenerator.uuid();
        String now = now();
        NetworkTask task = NetworkTask.builder()
                .taskId(taskId)
                .rawText(rawText)
                .taskStatus(TaskStatus.CREATED)
                .currentStage(WorkflowStage.CREATED.name())
                .createdAt(now)
                .updatedAt(now)
                .build();
        NetworkWorkspace workspace = NetworkWorkspace.builder()
                .task(task)
                .currentIntentVersion(INITIAL_VERSION)
                .currentPlanVersion(INITIAL_VERSION)
                .currentConfigVersion(INITIAL_VERSION)
                .currentExecutionVersion(INITIAL_VERSION)
                .currentValidationVersion(INITIAL_VERSION)
                .agentLogs(new CopyOnWriteArrayList<>())
                .build();
        return repository.save(workspace);
    }

    @Override
    public NetworkWorkspace getWorkspace(String taskId) {
        return repository.findByTaskId(taskId);
    }

    @Override
    public NetworkWorkspace saveIntent(String taskId, NetworkIntent intent) {
        requireNotNull(intent, "Intent must not be null");
        NetworkWorkspace workspace = repository.findByTaskId(taskId);
        ensureArtifactTaskId(taskId, intent.getTaskId(), intent::setTaskId);
        int version = versionOrDefault(intent.getIntentVersion());
        intent.setIntentVersion(version);
        intent.setStageStatus(StageStatus.SUCCESS);
        workspace.setIntent(intent);
        workspace.setCurrentIntentVersion(version);
        updateTask(workspace, TaskStatus.PARSED, WorkflowStage.INTENT);
        appendSystemLog(workspace, WorkflowStage.INTENT, "Intent artifact saved");
        return repository.save(workspace);
    }

    @Override
    public NetworkWorkspace savePlan(String taskId, NetworkPlan plan) {
        requireNotNull(plan, "Plan must not be null");
        NetworkWorkspace workspace = repository.findByTaskId(taskId);
        ensureArtifactTaskId(taskId, plan.getTaskId(), plan::setTaskId);
        int version = versionOrDefault(plan.getPlanVersion());
        plan.setPlanVersion(version);
        plan.setStageStatus(StageStatus.SUCCESS);
        workspace.setPlan(plan);
        workspace.setCurrentPlanVersion(version);
        updateTask(workspace, TaskStatus.PLANNED, WorkflowStage.PLANNING);
        appendSystemLog(workspace, WorkflowStage.PLANNING, "Plan artifact saved");
        return repository.save(workspace);
    }

    @Override
    public NetworkWorkspace saveConfigSet(String taskId, ConfigSet configSet) {
        requireNotNull(configSet, "ConfigSet must not be null");
        NetworkWorkspace workspace = repository.findByTaskId(taskId);
        ensureArtifactTaskId(taskId, configSet.getTaskId(), configSet::setTaskId);
        int version = versionOrDefault(configSet.getConfigVersion());
        configSet.setConfigVersion(version);
        configSet.setStageStatus(StageStatus.SUCCESS);
        workspace.setConfigSet(configSet);
        workspace.setCurrentConfigVersion(version);
        updateTask(workspace, TaskStatus.GENERATED, WorkflowStage.CONFIGURATION);
        appendSystemLog(workspace, WorkflowStage.CONFIGURATION, "Config artifact saved");
        return repository.save(workspace);
    }

    @Override
    public NetworkWorkspace saveExecutionReport(String taskId, ExecutionReport executionReport) {
        requireNotNull(executionReport, "ExecutionReport must not be null");
        NetworkWorkspace workspace = repository.findByTaskId(taskId);
        ensureArtifactTaskId(taskId, executionReport.getTaskId(), executionReport::setTaskId);
        int version = versionOrDefault(executionReport.getExecutionVersion());
        executionReport.setExecutionVersion(version);
        executionReport.setStageStatus(StageStatus.SUCCESS);
        workspace.setExecutionReport(executionReport);
        workspace.setCurrentExecutionVersion(version);
        updateTask(workspace, TaskStatus.EXECUTED, WorkflowStage.EXECUTION);
        appendSystemLog(workspace, WorkflowStage.EXECUTION, "Execution artifact saved");
        return repository.save(workspace);
    }

    @Override
    public NetworkWorkspace saveValidationReport(String taskId, ValidationReport validationReport) {
        requireNotNull(validationReport, "ValidationReport must not be null");
        NetworkWorkspace workspace = repository.findByTaskId(taskId);
        ensureArtifactTaskId(taskId, validationReport.getTaskId(), validationReport::setTaskId);
        int version = versionOrDefault(validationReport.getValidationVersion());
        validationReport.setValidationVersion(version);
        if (validationReport.getOverallStatus() == null) {
            validationReport.setOverallStatus(ValidationStatus.UNKNOWN);
        }
        validationReport.setStageStatus(StageStatus.SUCCESS);
        workspace.setValidationReport(validationReport);
        workspace.setCurrentValidationVersion(version);
        TaskStatus taskStatus = validationReport.getOverallStatus() == ValidationStatus.PASSED
                ? TaskStatus.PASSED
                : TaskStatus.FAILED;
        updateTask(workspace, taskStatus, WorkflowStage.VERIFICATION);
        appendSystemLog(workspace, WorkflowStage.VERIFICATION, "Validation artifact saved");
        return repository.save(workspace);
    }

    @Override
    public NetworkWorkspace appendAgentLog(String taskId, AgentStepLog log) {
        requireNotNull(log, "AgentStepLog must not be null");
        NetworkWorkspace workspace = repository.findByTaskId(taskId);
        ensureArtifactTaskId(taskId, log.getTaskId(), log::setTaskId);
        if (isBlank(log.getStepId())) {
            log.setStepId("step-" + IdGenerator.uuid());
        }
        workspaceLogs(workspace).add(log);
        touch(workspace);
        return repository.save(workspace);
    }

    @Override
    public NetworkWorkspace updateTaskStatus(String taskId, TaskStatus status) {
        requireNotNull(status, "TaskStatus must not be null");
        NetworkWorkspace workspace = repository.findByTaskId(taskId);
        workspace.getTask().setTaskStatus(status);
        touch(workspace);
        return repository.save(workspace);
    }

    @Override
    public NetworkWorkspace updateCurrentStage(String taskId, WorkflowStage stage) {
        requireNotNull(stage, "WorkflowStage must not be null");
        NetworkWorkspace workspace = repository.findByTaskId(taskId);
        workspace.getTask().setCurrentStage(stage.name());
        touch(workspace);
        return repository.save(workspace);
    }

    private void appendSystemLog(NetworkWorkspace workspace, WorkflowStage stage, String message) {
        String now = now();
        AgentStepLog log = AgentStepLog.builder()
                .stepId("step-" + IdGenerator.uuid())
                .taskId(workspace.getTask().getTaskId())
                .agentName(MODEL_CORE_AGENT)
                .stage(stage.name())
                .stageStatus(StageStatus.SUCCESS)
                .message(message)
                .startedAt(now)
                .finishedAt(now)
                .build();
        workspaceLogs(workspace).add(log);
    }

    private List<AgentStepLog> workspaceLogs(NetworkWorkspace workspace) {
        if (workspace.getAgentLogs() == null) {
            workspace.setAgentLogs(new CopyOnWriteArrayList<>());
        }
        return workspace.getAgentLogs();
    }

    private void updateTask(NetworkWorkspace workspace, TaskStatus status, WorkflowStage stage) {
        workspace.getTask().setTaskStatus(status);
        workspace.getTask().setCurrentStage(stage.name());
        touch(workspace);
    }

    private void touch(NetworkWorkspace workspace) {
        workspace.getTask().setUpdatedAt(now());
    }

    private int versionOrDefault(Integer version) {
        return version == null ? DEFAULT_ARTIFACT_VERSION : version;
    }

    private void ensureArtifactTaskId(String expectedTaskId, String actualTaskId, TaskIdSetter setter) {
        if (isBlank(actualTaskId)) {
            setter.setTaskId(expectedTaskId);
            return;
        }
        if (!expectedTaskId.equals(actualTaskId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Artifact taskId does not match workspace taskId");
        }
    }

    private void requireNotNull(Object value, String message) {
        if (value == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String now() {
        return Instant.now().toString();
    }

    @FunctionalInterface
    private interface TaskIdSetter {
        void setTaskId(String taskId);
    }
}
