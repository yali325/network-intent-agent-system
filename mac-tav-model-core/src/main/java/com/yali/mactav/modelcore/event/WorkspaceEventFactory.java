package com.yali.mactav.modelcore.event;

import com.yali.mactav.model.enums.TaskStatus;
import com.yali.mactav.model.enums.WorkflowStage;
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
        return base(taskId, "workspace.created", workspace.getTask().getCurrentStage())
                .title("Workspace created")
                .message("Workspace created for task " + taskId)
                .payloadSummary(workspace.getTask().getDescription())
                .build();
    }

    public static WorkspaceEvent stageChanged(String taskId, WorkflowStage stage) {
        return base(taskId, "stage.changed", stage)
                .title("Stage changed")
                .message("Task stage changed to " + stage)
                .payloadSummary(stage == null ? null : stage.name())
                .build();
    }

    public static WorkspaceEvent taskStatusChanged(String taskId, WorkflowStage stage, TaskStatus status) {
        return base(taskId, "task.status.changed", stage)
                .title("Task status changed")
                .message("Task status changed to " + status)
                .payloadSummary(status == null ? null : status.name())
                .build();
    }

    public static WorkspaceEvent artifactGenerated(NetworkArtifact artifact) {
        return base(artifact.getTaskId(), "artifact.generated", artifact.getStage())
                .title("Artifact generated")
                .message("Generated " + artifact.getArtifactType() + " v" + artifact.getVersion())
                .relatedArtifactId(artifact.getArtifactId())
                .payloadSummary(artifact.getPayloadSummary())
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
