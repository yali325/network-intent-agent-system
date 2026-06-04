package com.yali.mactav.modelcore.event;

import com.yali.mactav.model.enums.TaskStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.healing.RepairAction;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Creates standard workspace timeline events used by Model Core services.
 *
 * <p>This factory centralizes event shape only. It should not replace
 * orchestrator decisions or durable event delivery.</p>
 */
public final class WorkspaceEventFactory {

    private WorkspaceEventFactory() {
    }

    public static WorkspaceEvent workspaceCreated(NetworkWorkspace workspace) {
        String taskId = workspace.getTask().getTaskId();
        return base(taskId, WorkspaceEventTypes.TASK_CREATED, workspace.getTask().getCurrentStage())
                .title("Workspace created")
                .message("Workspace created for task " + taskId)
                .payloadSummary(workspace.getTask().getDescription())
                .build();
    }

    public static WorkspaceEvent stageChanged(String taskId, WorkflowStage stage) {
        return base(taskId, WorkspaceEventTypes.STAGE_STARTED, stage)
                .title("Stage changed")
                .message("Task stage changed to " + stage)
                .payloadSummary(stage == null ? null : stage.name())
                .build();
    }

    public static WorkspaceEvent taskStatusChanged(String taskId, WorkflowStage stage, TaskStatus status) {
        String eventType = status == TaskStatus.COMPLETED
                ? WorkspaceEventTypes.WORKFLOW_COMPLETED
                : WorkspaceEventTypes.WORKFLOW_INTERRUPTED;
        return base(taskId, eventType, stage)
                .title("Task status changed")
                .message("Task status changed to " + status)
                .payloadSummary(status == null ? null : status.name())
                .build();
    }

    public static WorkspaceEvent artifactGenerated(NetworkArtifact artifact) {
        return base(artifact.getTaskId(), WorkspaceEventTypes.ARTIFACT_GENERATED, artifact.getStage())
                .title("Artifact generated")
                .message("Generated " + artifact.getArtifactType() + " v" + artifact.getVersion())
                .relatedArtifactId(artifact.getArtifactId())
                .payloadSummary(artifact.getPayloadSummary())
                .build();
    }

    public static WorkspaceEvent repairProposed(NetworkArtifact artifact) {
        return base(artifact.getTaskId(), WorkspaceEventTypes.REPAIR_PROPOSED, artifact.getStage())
                .title("Repair proposed")
                .message("Repair plan proposed from " + artifact.getArtifactType() + " v" + artifact.getVersion())
                .relatedArtifactId(artifact.getArtifactId())
                .payloadSummary(artifact.getPayloadSummary())
                .build();
    }

    public static WorkspaceEvent repairApproved(String taskId, RepairAction action, String comment) {
        return repairActionEvent(taskId, WorkspaceEventTypes.REPAIR_APPROVED, "Repair approved", action, comment);
    }

    public static WorkspaceEvent repairRejected(String taskId, RepairAction action, String comment) {
        return repairActionEvent(taskId, WorkspaceEventTypes.REPAIR_REJECTED, "Repair rejected", action, comment);
    }

    public static WorkspaceEvent repairApplied(String taskId, RepairAction action, String comment) {
        return repairActionEvent(taskId, WorkspaceEventTypes.REPAIR_APPLIED, "Repair applied", action, comment);
    }

    public static WorkspaceEvent repairWaitingUser(String taskId, RepairAction action, String comment) {
        return repairActionEvent(taskId, "repair.waiting_user", "Repair waiting for user", action, comment);
    }

    public static WorkspaceEvent artifactVersionSwitched(
            NetworkArtifact targetArtifact,
            String fromArtifactId,
            String reason,
            String actor) {
        String summary = "artifactType=" + targetArtifact.getArtifactType()
                + ", fromArtifactId=" + fromArtifactId
                + ", toArtifactId=" + targetArtifact.getArtifactId()
                + ", version=" + targetArtifact.getVersion()
                + ", actor=" + actor
                + ", reason=" + reason;
        return base(targetArtifact.getTaskId(), WorkspaceEventTypes.ARTIFACT_VERSION_SWITCHED, targetArtifact.getStage())
                .title("Artifact version switched")
                .message("Current " + targetArtifact.getArtifactType() + " switched to v" + targetArtifact.getVersion())
                .relatedArtifactId(targetArtifact.getArtifactId())
                .payloadSummary(summary)
                .build();
    }

    private static WorkspaceEvent repairActionEvent(
            String taskId,
            String eventType,
            String title,
            RepairAction action,
            String comment) {
        String actionId = action == null ? null : action.getActionId();
        return base(taskId, eventType, WorkflowStage.HEALING)
                .title(title)
                .message(title + (actionId == null ? "" : " for action " + actionId))
                .payloadSummary(comment)
                .build();
    }

    private static WorkspaceEvent.WorkspaceEventBuilder base(String taskId, String eventType, WorkflowStage stage) {
        return WorkspaceEvent.builder()
                .eventId("event-" + UUID.randomUUID())
                .taskId(taskId)
                .eventType(eventType)
                .stage(stage)
                .eventTime(LocalDateTime.now())
                .severity("INFO");
    }
}
