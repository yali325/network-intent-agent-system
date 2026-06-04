package com.yali.mactav.modelcore.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * MySQL row for workspace timeline events.
 */
@Data
public class WorkspaceEventEntity {

    private String eventId;
    private String taskId;
    private String eventType;
    private String stage;
    private LocalDateTime eventTime;
    private String severity;
    private String title;
    private String message;
    private String relatedArtifactId;
    private String relatedRecordId;
    private String traceId;
    private String payloadSummary;
}
