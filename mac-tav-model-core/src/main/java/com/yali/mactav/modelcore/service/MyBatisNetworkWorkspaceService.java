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
import com.yali.mactav.model.workspace.WorkspaceChangeRecord;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import com.yali.mactav.modelcore.artifact.ArtifactPayloadSerializer;
import com.yali.mactav.modelcore.assembler.MyBatisModelCoreAssembler;
import com.yali.mactav.modelcore.entity.NetworkWorkspaceStateEntity;
import com.yali.mactav.modelcore.event.WorkspaceEventFactory;
import com.yali.mactav.modelcore.repository.MyBatisNetworkArtifactRepository;
import com.yali.mactav.modelcore.repository.MyBatisNetworkTaskRepository;
import com.yali.mactav.modelcore.repository.MyBatisNetworkWorkspaceStateRepository;
import com.yali.mactav.modelcore.validator.ArtifactValidator;
import com.yali.mactav.modelcore.validator.WorkspaceStateValidator;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

/**
 * MyBatis-backed workspace service for durable task state and current artifact pointers.
 */
public class MyBatisNetworkWorkspaceService implements NetworkWorkspaceService {

    private final MyBatisNetworkTaskRepository taskRepository;
    private final MyBatisNetworkWorkspaceStateRepository stateRepository;
    private final MyBatisNetworkArtifactRepository artifactRepository;
    private final NetworkArtifactService artifactService;
    private final WorkspaceEventService eventService;
    private final WorkspaceChangeRecordService changeRecordService;
    private final AgentExecutionRecordService executionRecordService;
    private final WorkspaceStateValidator workspaceStateValidator;
    private final ArtifactValidator artifactValidator;
    private final MyBatisModelCoreAssembler assembler;
    private final ArtifactPayloadSerializer payloadSerializer;

    public MyBatisNetworkWorkspaceService(MyBatisNetworkTaskRepository taskRepository,
                                          MyBatisNetworkWorkspaceStateRepository stateRepository,
                                          MyBatisNetworkArtifactRepository artifactRepository,
                                          NetworkArtifactService artifactService,
                                          WorkspaceEventService eventService,
                                          WorkspaceChangeRecordService changeRecordService,
                                          AgentExecutionRecordService executionRecordService,
                                          WorkspaceStateValidator workspaceStateValidator,
                                          ArtifactValidator artifactValidator,
                                          MyBatisModelCoreAssembler assembler,
                                          ArtifactPayloadSerializer payloadSerializer) {
        this.taskRepository = taskRepository;
        this.stateRepository = stateRepository;
        this.artifactRepository = artifactRepository;
        this.artifactService = artifactService;
        this.eventService = eventService;
        this.changeRecordService = changeRecordService;
        this.executionRecordService = executionRecordService;
        this.workspaceStateValidator = workspaceStateValidator;
        this.artifactValidator = artifactValidator;
        this.assembler = assembler;
        this.payloadSerializer = payloadSerializer;
    }

    @Override
    @Transactional
    public NetworkWorkspace createWorkspace(NetworkTask task) {
        workspaceStateValidator.validateTask(task);
        if (taskRepository.findByTaskId(task.getTaskId()).isPresent()) {
            throw new BusinessException(ErrorCode.WORKSPACE_STATE_INVALID, "Workspace already exists: " + task.getTaskId());
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
        taskRepository.insert(assembler.toEntity(task));

        NetworkWorkspaceStateEntity state = new NetworkWorkspaceStateEntity();
        state.setTaskId(task.getTaskId());
        state.setWorkspaceStatus(task.getTaskStatus().name());
        state.setUpdateTime(now);
        stateRepository.insert(state);

        NetworkWorkspace workspace = NetworkWorkspace.builder()
                .task(task)
                .workspaceStatus(task.getTaskStatus())
                .build();
        eventService.appendEvent(task.getTaskId(), WorkspaceEventFactory.workspaceCreated(workspace));
        return getWorkspaceOrThrow(task.getTaskId());
    }

    @Override
    public Optional<NetworkWorkspace> findWorkspace(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return Optional.empty();
        }
        return taskRepository.findByTaskId(taskId).map(task -> rebuildWorkspace(taskId, task));
    }

    @Override
    public NetworkWorkspace getWorkspaceOrThrow(String taskId) {
        workspaceStateValidator.validateTaskId(taskId);
        return findWorkspace(taskId).orElseThrow(() -> workspaceStateValidator.workspaceNotFound(taskId));
    }

    @Override
    @Transactional
    public NetworkWorkspace updateTaskStage(String taskId, WorkflowStage stage) {
        if (stage == null) {
            throw new BusinessException(ErrorCode.WORKSPACE_STATE_INVALID, "stage must not be null");
        }
        NetworkTask task = taskRepository.findByTaskId(taskId)
                .map(assembler::toTask)
                .orElseThrow(() -> workspaceStateValidator.workspaceNotFound(taskId));
        task.setCurrentStage(stage);
        task.setUpdateTime(LocalDateTime.now());
        taskRepository.update(assembler.toEntity(task));
        eventService.appendEvent(taskId, WorkspaceEventFactory.stageChanged(taskId, stage));
        return getWorkspaceOrThrow(taskId);
    }

    @Override
    @Transactional
    public NetworkWorkspace updateTaskStatus(String taskId, TaskStatus status) {
        if (status == null) {
            throw new BusinessException(ErrorCode.WORKSPACE_STATE_INVALID, "status must not be null");
        }
        NetworkTask task = taskRepository.findByTaskId(taskId)
                .map(assembler::toTask)
                .orElseThrow(() -> workspaceStateValidator.workspaceNotFound(taskId));
        task.setTaskStatus(status);
        task.setUpdateTime(LocalDateTime.now());
        taskRepository.update(assembler.toEntity(task));

        NetworkWorkspaceStateEntity state = stateOrThrow(taskId);
        state.setWorkspaceStatus(status.name());
        state.setUpdateTime(LocalDateTime.now());
        stateRepository.update(state);

        eventService.appendEvent(taskId, WorkspaceEventFactory.taskStatusChanged(taskId, task.getCurrentStage(), status));
        return getWorkspaceOrThrow(taskId);
    }

    @Override
    @Transactional
    public NetworkWorkspace saveArtifact(String taskId, NetworkArtifact artifact) {
        workspaceStateValidator.validateTaskId(taskId);
        artifactValidator.validate(artifact);
        if (!taskId.equals(artifact.getTaskId())) {
            throw new BusinessException(ErrorCode.ARTIFACT_INVALID, "Artifact taskId does not match workspace taskId");
        }
        taskRepository.findByTaskId(taskId).orElseThrow(() -> workspaceStateValidator.workspaceNotFound(taskId));
        artifactRepository.insert(assembler.toEntity(artifact));
        updateStateForArtifact(taskId, artifact);
        return getWorkspaceOrThrow(taskId);
    }

    @Override
    @Transactional
    public NetworkWorkspace saveCurrentRepairPlan(String taskId, RepairPlan repairPlan) {
        if (repairPlan == null) {
            throw new BusinessException(ErrorCode.WORKSPACE_STATE_INVALID, "RepairPlan must not be null");
        }
        if (repairPlan.getTaskId() == null || repairPlan.getTaskId().isBlank()) {
            repairPlan.setTaskId(taskId);
        }
        saveStageArtifact(
                taskId,
                ArtifactType.REPAIR_PLAN,
                WorkflowStage.HEALING,
                repairPlan,
                "RepairPlan for task " + taskId,
                "Orchestrator",
                TraceRefs.builder().build());
        return getWorkspaceOrThrow(taskId);
    }

    @Override
    @Transactional
    public NetworkWorkspace appendWorkspaceEvent(String taskId, WorkspaceEvent event) {
        taskRepository.findByTaskId(taskId).orElseThrow(() -> workspaceStateValidator.workspaceNotFound(taskId));
        eventService.appendEvent(taskId, event);
        return getWorkspaceOrThrow(taskId);
    }

    @Override
    @Transactional
    public NetworkWorkspace switchCurrentArtifact(
            String taskId,
            ArtifactType artifactType,
            String targetArtifactId,
            String reason,
            String actor) {
        workspaceStateValidator.validateTaskId(taskId);
        if (artifactType == null) {
            throw new BusinessException(ErrorCode.ARTIFACT_INVALID, "artifactType must not be null");
        }
        if (targetArtifactId == null || targetArtifactId.isBlank()) {
            throw new BusinessException(ErrorCode.ARTIFACT_NOT_FOUND, "targetArtifactId must not be blank");
        }
        taskRepository.findByTaskId(taskId).orElseThrow(() -> workspaceStateValidator.workspaceNotFound(taskId));
        NetworkArtifact targetArtifact = artifactRepository.findByArtifactId(targetArtifactId)
                .map(assembler::toArtifact)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ARTIFACT_NOT_FOUND,
                        "Artifact not found: " + targetArtifactId));
        if (!taskId.equals(targetArtifact.getTaskId())) {
            throw new BusinessException(ErrorCode.ARTIFACT_NOT_FOUND, "Artifact not found: " + targetArtifactId);
        }
        if (targetArtifact.getArtifactType() != artifactType) {
            throw new BusinessException(
                    ErrorCode.ARTIFACT_INVALID,
                    "Artifact type mismatch: expected " + artifactType + ", actual " + targetArtifact.getArtifactType());
        }

        NetworkWorkspaceStateEntity state = stateOrThrow(taskId);
        Map<ArtifactType, String> refs = assembler.artifactRefs(state.getCurrentArtifactRefsJson());
        if (refs == null) {
            refs = new EnumMap<>(ArtifactType.class);
        }
        String fromArtifactId = refs.get(artifactType);
        refs.put(artifactType, targetArtifactId);
        state.setCurrentArtifactRefsJson(assembler.artifactRefsJson(refs));
        updateStateVersion(state, targetArtifact);
        state.setUpdateTime(LocalDateTime.now());
        stateRepository.update(state);

        changeRecordService.appendChange(taskId, WorkspaceChangeRecord.builder()
                .changeId("change-" + UUID.randomUUID())
                .taskId(taskId)
                .stage(targetArtifact.getStage())
                .changeType("VERSION_SWITCH")
                .fromArtifactId(fromArtifactId)
                .toArtifactId(targetArtifactId)
                .reason(reason)
                .createdBy(actor == null || actor.isBlank() ? "api" : actor)
                .createTime(LocalDateTime.now())
                .build());
        eventService.appendEvent(taskId, WorkspaceEventFactory.artifactVersionSwitched(
                targetArtifact,
                fromArtifactId,
                reason,
                actor));
        return getWorkspaceOrThrow(taskId);
    }

    @Override
    @Transactional
    public NetworkArtifact saveStageArtifact(String taskId,
                                             ArtifactType artifactType,
                                             WorkflowStage stage,
                                             Object payloadDto,
                                             String payloadSummary,
                                             String createdBy,
                                             TraceRefs traceRefs) {
        taskRepository.findByTaskId(taskId).orElseThrow(() -> workspaceStateValidator.workspaceNotFound(taskId));
        int version = artifactRepository.nextVersion(taskId, artifactType);
        NetworkArtifact artifact = artifactService.createArtifact(
                taskId,
                artifactType,
                stage,
                version,
                payloadDto,
                payloadSummary,
                createdBy,
                traceRefs);
        try {
            artifactRepository.insert(assembler.toEntity(artifact));
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(
                    ErrorCode.WORKSPACE_STATE_INVALID,
                    "Artifact version conflict for task " + taskId + ", type " + artifactType + ", version " + version,
                    ex);
        }
        artifactRepository.markSupersededExcept(taskId, artifactType, artifact.getArtifactId());
        updateStateForArtifact(taskId, artifact);
        eventService.appendEvent(taskId, WorkspaceEventFactory.artifactGenerated(artifact));
        if (artifactType == ArtifactType.REPAIR_PLAN) {
            eventService.appendEvent(taskId, WorkspaceEventFactory.repairProposed(artifact));
        }
        return artifact;
    }

    private NetworkWorkspace rebuildWorkspace(String taskId, com.yali.mactav.modelcore.entity.NetworkTaskEntity taskEntity) {
        NetworkTask task = assembler.toTask(taskEntity);
        NetworkWorkspaceStateEntity state = stateRepository.findByTaskId(taskId)
                .orElseThrow(() -> workspaceStateValidator.workspaceNotFound(taskId));
        Map<ArtifactType, String> refs = assembler.artifactRefs(state.getCurrentArtifactRefsJson());
        NetworkWorkspace workspace = NetworkWorkspace.builder()
                .task(task)
                .workspaceStatus(TaskStatus.valueOf(state.getWorkspaceStatus()))
                .currentIntentVersion(state.getCurrentIntentVersion())
                .currentPlanVersion(state.getCurrentPlanVersion())
                .currentConfigVersion(state.getCurrentConfigVersion())
                .currentExecutionVersion(state.getCurrentExecutionVersion())
                .currentValidationVersion(state.getCurrentValidationVersion())
                .currentRepairVersion(state.getCurrentRepairVersion())
                .currentArtifactRefs(refs)
                .artifacts(artifactService.listByTaskId(taskId))
                .events(eventService.listEvents(taskId))
                .changeHistory(changeRecordService.listChanges(taskId))
                .agentExecutionRecords(executionRecordService.listRecords(taskId))
                .build();
        hydrateCurrentPayloads(workspace, refs);
        return workspace;
    }

    private void hydrateCurrentPayloads(NetworkWorkspace workspace, Map<ArtifactType, String> refs) {
        refs.forEach((artifactType, artifactId) -> artifactRepository.findByArtifactId(artifactId)
                .map(assembler::toArtifact)
                .ifPresent(artifact -> hydrateCurrentPayload(workspace, artifactType, artifact)));
    }

    private void hydrateCurrentPayload(NetworkWorkspace workspace, ArtifactType artifactType, NetworkArtifact artifact) {
        switch (artifactType) {
            case NETWORK_INTENT -> workspace.setCurrentIntent(
                    payloadSerializer.deserialize(artifact.getPayloadJson(), NetworkIntent.class));
            case NETWORK_PLAN -> workspace.setCurrentPlan(
                    payloadSerializer.deserialize(artifact.getPayloadJson(), NetworkPlan.class));
            case CONFIG_SET -> workspace.setCurrentConfigSet(
                    payloadSerializer.deserialize(artifact.getPayloadJson(), ConfigSet.class));
            case EXECUTION_REPORT -> workspace.setCurrentExecutionReport(
                    payloadSerializer.deserialize(artifact.getPayloadJson(), ExecutionReport.class));
            case VALIDATION_REPORT -> workspace.setCurrentValidationReport(
                    payloadSerializer.deserialize(artifact.getPayloadJson(), ValidationReport.class));
            case REPAIR_PLAN -> workspace.setCurrentRepairPlan(
                    payloadSerializer.deserialize(artifact.getPayloadJson(), RepairPlan.class));
        }
    }

    private void updateStateForArtifact(String taskId, NetworkArtifact artifact) {
        NetworkWorkspaceStateEntity state = stateOrThrow(taskId);
        Map<ArtifactType, String> refs = assembler.artifactRefs(state.getCurrentArtifactRefsJson());
        if (refs == null) {
            refs = new EnumMap<>(ArtifactType.class);
        }
        refs.put(artifact.getArtifactType(), artifact.getArtifactId());
        state.setCurrentArtifactRefsJson(assembler.artifactRefsJson(refs));
        updateStateVersion(state, artifact);
        state.setUpdateTime(LocalDateTime.now());
        stateRepository.update(state);
    }

    private void updateStateVersion(NetworkWorkspaceStateEntity state, NetworkArtifact artifact) {
        switch (artifact.getArtifactType()) {
            case NETWORK_INTENT -> state.setCurrentIntentVersion(artifact.getVersion());
            case NETWORK_PLAN -> state.setCurrentPlanVersion(artifact.getVersion());
            case CONFIG_SET -> state.setCurrentConfigVersion(artifact.getVersion());
            case EXECUTION_REPORT -> state.setCurrentExecutionVersion(artifact.getVersion());
            case VALIDATION_REPORT -> state.setCurrentValidationVersion(artifact.getVersion());
            case REPAIR_PLAN -> state.setCurrentRepairVersion(artifact.getVersion());
        }
    }

    private NetworkWorkspaceStateEntity stateOrThrow(String taskId) {
        return stateRepository.findByTaskId(taskId).orElseThrow(() -> workspaceStateValidator.workspaceNotFound(taskId));
    }
}
