package com.yali.mactav.modelcore.event;

import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import java.time.LocalDateTime;

/**
 * Safe event summary used for Redis realtime delivery and Web SSE payloads.
 */
public record WorkspaceEventSummary(
        String eventId,
        String taskId,
        String eventType,
        WorkflowStage stage,
        LocalDateTime eventTime,
        String severity,
        String title,
        String message,
        String relatedArtifactId,
        String relatedRecordId,
        String traceId,
        String payloadSummary) {

    public static WorkspaceEventSummary from(WorkspaceEvent event) {
        return new WorkspaceEventSummary(
                event.getEventId(),
                event.getTaskId(),
                event.getEventType(),
                event.getStage(),
                event.getEventTime(),
                event.getSeverity(),
                event.getTitle(),
                event.getMessage(),
                event.getRelatedArtifactId(),
                event.getRelatedRecordId(),
                event.getTraceId(),
                event.getPayloadSummary());
    }
}
