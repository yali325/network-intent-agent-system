package com.yali.mactav.web.dto.view;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Read-only workflow trace view for stage topology and telemetry panels.
 */
@Data
@Builder
public class WorkflowTraceView {

    private String taskId;

    private String status;

    private boolean ready;

    private String reasonCode;

    private String message;

    private String currentStage;

    private String jobStatus;

    private List<String> missingArtifacts;

    private List<TraceNode> nodes;

    private List<TraceEdge> edges;

    private List<TraceEvent> events;

    private List<String> errors;

    /**
     * Stage node shown in the frontend trace graph.
     */
    @Data
    @Builder
    public static class TraceNode {

        private String id;

        private String stage;

        private String label;

        private String agentName;

        private String status;

        private String artifactType;

        private String artifactId;

        private String errorCode;

        private String errorMessage;
    }

    /**
     * Directed edge between workflow stages.
     */
    @Data
    @Builder
    public static class TraceEdge {

        private String from;

        private String to;

        private String status;

        private String label;
    }

    /**
     * Safe event projection used by the trace panel.
     */
    @Data
    @Builder
    public static class TraceEvent {

        private String eventId;

        private String eventType;

        private String stage;

        private String severity;

        private String title;

        private String message;

        private String eventTime;

        private String relatedArtifactId;

        private String relatedRecordId;

        private String traceId;
    }
}
