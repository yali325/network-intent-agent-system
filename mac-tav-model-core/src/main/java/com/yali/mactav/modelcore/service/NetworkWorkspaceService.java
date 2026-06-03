package com.yali.mactav.modelcore.service;

import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.TaskStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.task.NetworkTask;
import com.yali.mactav.model.healing.RepairPlan;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.model.workspace.TraceRefs;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import java.util.Optional;

/**
 * Model Core boundary for workspace state, stage versions, and current artifact references.
 *
 * <p>Implementations manage state only. They must not call LLMs, generate
 * business artifacts, execute simulations, or own orchestration policy.</p>
 */
public interface NetworkWorkspaceService {

    NetworkWorkspace createWorkspace(NetworkTask task);

    Optional<NetworkWorkspace> findWorkspace(String taskId);

    NetworkWorkspace getWorkspaceOrThrow(String taskId);

    NetworkWorkspace updateTaskStage(String taskId, WorkflowStage stage);

    NetworkWorkspace updateTaskStatus(String taskId, TaskStatus status);

    NetworkWorkspace saveArtifact(String taskId, NetworkArtifact artifact);

    NetworkWorkspace saveCurrentRepairPlan(String taskId, RepairPlan repairPlan);

    NetworkWorkspace appendWorkspaceEvent(String taskId, WorkspaceEvent event);

    NetworkArtifact saveStageArtifact(
            String taskId,
            ArtifactType artifactType,
            WorkflowStage stage,
            Object payloadDto,
            String payloadSummary,
            String createdBy,
            TraceRefs traceRefs);
}
