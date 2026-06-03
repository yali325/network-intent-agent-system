package com.yali.mactav.modelcore.service;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.TaskStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.healing.RepairPlan;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.task.NetworkTask;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.model.workspace.TraceRefs;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import com.yali.mactav.modelcore.event.WorkspaceEventFactory;
import com.yali.mactav.modelcore.repository.InMemoryNetworkWorkspaceRepository;
import com.yali.mactav.modelcore.validator.ArtifactValidator;
import com.yali.mactav.modelcore.validator.WorkspaceStateValidator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory NetworkWorkspaceService implementation for the current skeleton phase.
 *
 * <p>It manages workspace state, current artifact references, versions, and
 * events only. It does not generate stage DTOs or call agents. TODO Phase 9:
 * replace storage coordination with transactional MySQL/Redis persistence.</p>
 */
public class InMemoryNetworkWorkspaceService implements NetworkWorkspaceService {

    private final InMemoryNetworkWorkspaceRepository workspaceRepository;

    private final NetworkArtifactService artifactService;

    private final WorkspaceEventService eventService;

    private final WorkspaceStateValidator workspaceStateValidator;

    private final ArtifactValidator artifactValidator;

    public InMemoryNetworkWorkspaceService(
            InMemoryNetworkWorkspaceRepository workspaceRepository,
            NetworkArtifactService artifactService,
            WorkspaceEventService eventService,
            WorkspaceStateValidator workspaceStateValidator,
            ArtifactValidator artifactValidator) {
        this.workspaceRepository = workspaceRepository;
        this.artifactService = artifactService;
        this.eventService = eventService;
        this.workspaceStateValidator = workspaceStateValidator;
        this.artifactValidator = artifactValidator;
    }

    @Override
    public NetworkWorkspace createWorkspace(NetworkTask task) {
        workspaceStateValidator.validateTask(task);
        if (workspaceRepository.existsByTaskId(task.getTaskId())) {
            throw new BusinessException(
                    ErrorCode.WORKSPACE_STATE_INVALID,
                    "Workspace already exists: " + task.getTaskId());
        }
        LocalDateTime now = LocalDateTime.now();
        if (task.getTaskStatus() == null) {
            task.setTaskStatus(TaskStatus.CREATED);
        }
        if (task.getCurrentStage() == null) {
            task.setCurrentStage(WorkflowStage.INTENT);
        }
        if (task.getCreateTime() == null) {
            task.setCreateTime(now);
        }
        task.setUpdateTime(now);

        NetworkWorkspace workspace = NetworkWorkspace.builder()
                .task(task)
                .workspaceStatus(task.getTaskStatus())
                .build();
        workspaceRepository.save(task.getTaskId(), workspace);
        appendWorkspaceEvent(workspace, WorkspaceEventFactory.workspaceCreated(workspace));
        return workspaceRepository.save(task.getTaskId(), workspace);
    }

    @Override
    public Optional<NetworkWorkspace> findWorkspace(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return Optional.empty();
        }
        return workspaceRepository.findByTaskId(taskId);
    }

    @Override
    public NetworkWorkspace getWorkspaceOrThrow(String taskId) {
        workspaceStateValidator.validateTaskId(taskId);
        return workspaceRepository
                .findByTaskId(taskId)
                .orElseThrow(() -> workspaceStateValidator.workspaceNotFound(taskId));
    }

    @Override
    public NetworkWorkspace updateTaskStage(String taskId, WorkflowStage stage) {
        if (stage == null) {
            throw new BusinessException(ErrorCode.WORKSPACE_STATE_INVALID, "stage must not be null");
        }
        NetworkWorkspace workspace = getWorkspaceOrThrow(taskId);
        workspace.getTask().setCurrentStage(stage);
        workspace.getTask().setUpdateTime(LocalDateTime.now());
        appendWorkspaceEvent(workspace, WorkspaceEventFactory.stageChanged(taskId, stage));
        return workspaceRepository.save(taskId, workspace);
    }

    @Override
    public NetworkWorkspace updateTaskStatus(String taskId, TaskStatus status) {
        if (status == null) {
            throw new BusinessException(ErrorCode.WORKSPACE_STATE_INVALID, "status must not be null");
        }
        NetworkWorkspace workspace = getWorkspaceOrThrow(taskId);
        workspace.getTask().setTaskStatus(status);
        workspace.getTask().setUpdateTime(LocalDateTime.now());
        workspace.setWorkspaceStatus(status);
        appendWorkspaceEvent(workspace, WorkspaceEventFactory.taskStatusChanged(
                taskId,
                workspace.getTask().getCurrentStage(),
                status));
        return workspaceRepository.save(taskId, workspace);
    }

    @Override
    public NetworkWorkspace saveArtifact(String taskId, NetworkArtifact artifact) {
        workspaceStateValidator.validateTaskId(taskId);
        artifactValidator.validate(artifact);
        if (!taskId.equals(artifact.getTaskId())) {
            throw new BusinessException(
                    ErrorCode.ARTIFACT_INVALID,
                    "Artifact taskId does not match workspace taskId");
        }
        NetworkWorkspace workspace = getWorkspaceOrThrow(taskId);
        NetworkArtifact saved = artifactService.saveArtifact(artifact);
        if (workspace.getArtifacts() == null) {
            workspace.setArtifacts(new ArrayList<>());
        }
        workspace.getArtifacts().add(saved);
        ensureArtifactRefs(workspace).put(saved.getArtifactType(), saved.getArtifactId());
        updateCurrentVersion(workspace, saved);
        // TODO Phase 9: direct saveArtifact only updates refs/versions; stage DTO hydration needs typed payload handling.
        return workspaceRepository.save(taskId, workspace);
    }

    @Override
    public NetworkArtifact saveStageArtifact(
            String taskId,
            ArtifactType artifactType,
            WorkflowStage stage,
            Object payloadDto,
            String payloadSummary,
            String createdBy,
            TraceRefs traceRefs) {
        getWorkspaceOrThrow(taskId);
        int version = artifactService.nextVersion(taskId, artifactType);
        NetworkArtifact artifact = artifactService.createArtifact(
                taskId,
                artifactType,
                stage,
                version,
                payloadDto,
                payloadSummary,
                createdBy,
                traceRefs);
        saveArtifact(taskId, artifact);

        NetworkWorkspace refreshedWorkspace = getWorkspaceOrThrow(taskId);
        syncCurrentPayload(refreshedWorkspace, artifactType, payloadDto);
        appendWorkspaceEvent(refreshedWorkspace, WorkspaceEventFactory.artifactGenerated(artifact));
        if (artifactType == ArtifactType.REPAIR_PLAN) {
            appendWorkspaceEvent(refreshedWorkspace, WorkspaceEventFactory.repairProposed(artifact));
        }
        workspaceRepository.save(taskId, refreshedWorkspace);
        return artifact;
    }

    private void updateCurrentVersion(NetworkWorkspace workspace, NetworkArtifact artifact) {
        Integer version = artifact.getVersion();
        switch (artifact.getArtifactType()) {
            case NETWORK_INTENT -> workspace.setCurrentIntentVersion(version);
            case NETWORK_PLAN -> workspace.setCurrentPlanVersion(version);
            case CONFIG_SET -> workspace.setCurrentConfigVersion(version);
            case EXECUTION_REPORT -> workspace.setCurrentExecutionVersion(version);
            case VALIDATION_REPORT -> workspace.setCurrentValidationVersion(version);
            case REPAIR_PLAN -> workspace.setCurrentRepairVersion(version);
        }
    }

    private void syncCurrentPayload(NetworkWorkspace workspace, ArtifactType artifactType, Object payloadDto) {
        switch (artifactType) {
            case NETWORK_INTENT -> {
                if (payloadDto instanceof NetworkIntent currentIntent) {
                    workspace.setCurrentIntent(currentIntent);
                }
            }
            case NETWORK_PLAN -> {
                if (payloadDto instanceof NetworkPlan currentPlan) {
                    workspace.setCurrentPlan(currentPlan);
                }
            }
            case CONFIG_SET -> {
                if (payloadDto instanceof ConfigSet currentConfigSet) {
                    workspace.setCurrentConfigSet(currentConfigSet);
                }
            }
            case EXECUTION_REPORT -> {
                if (payloadDto instanceof ExecutionReport currentExecutionReport) {
                    workspace.setCurrentExecutionReport(currentExecutionReport);
                }
            }
            case VALIDATION_REPORT -> {
                if (payloadDto instanceof ValidationReport currentValidationReport) {
                    workspace.setCurrentValidationReport(currentValidationReport);
                }
            }
            case REPAIR_PLAN -> {
                if (payloadDto instanceof RepairPlan currentRepairPlan) {
                    workspace.setCurrentRepairPlan(currentRepairPlan);
                }
            }
        }
        workspaceRepository.save(workspace.getTask().getTaskId(), workspace);
    }

    private void appendWorkspaceEvent(NetworkWorkspace workspace, WorkspaceEvent event) {
        WorkspaceEvent savedEvent = eventService.appendEvent(workspace.getTask().getTaskId(), event);
        if (workspace.getEvents() == null) {
            workspace.setEvents(new ArrayList<>());
        }
        workspace.getEvents().add(savedEvent);
    }

    private Map<ArtifactType, String> ensureArtifactRefs(NetworkWorkspace workspace) {
        if (workspace.getCurrentArtifactRefs() == null) {
            workspace.setCurrentArtifactRefs(new EnumMap<>(ArtifactType.class));
        }
        return workspace.getCurrentArtifactRefs();
    }
}
