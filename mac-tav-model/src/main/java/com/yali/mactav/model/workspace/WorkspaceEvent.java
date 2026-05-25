package com.yali.mactav.model.workspace;

import com.yali.mactav.model.enums.WorkflowStage;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 事件列表 —— 前端时间线 / 进度展示
 */
/**
 * Lightweight process event for workspace history, SSE, and timeline views.
 *
 * <p>Events summarize state transitions and related artifacts; they are not the
 * source of truth for artifact payloads or workflow decisions.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceEvent {

    private String eventId;

    private String taskId;

    private String eventType;

    private WorkflowStage stage;

    private LocalDateTime eventTime;

    private String severity;

    private String title;

    private String message;

    private String relatedArtifactId;

    private String relatedRecordId;

    private String traceId;

    private String payloadSummary;
}
