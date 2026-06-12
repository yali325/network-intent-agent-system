package com.yali.mactav.web.dto.view;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Read-only execution log projection derived from ExecutionReport and events.
 */
@Data
@Builder
public class ExecutionLogsView {

    private String taskId;

    private String status;

    private boolean ready;

    private String source;

    private List<ExecutionLogLine> lines;

    private List<WorkflowTraceView.TraceEvent> events;

    private String reasonCode;

    private String message;

    /**
     * Structured log line synthesized from stored execution facts.
     */
    @Data
    @Builder
    public static class ExecutionLogLine {

        private String time;

        private String level;

        private String stage;

        private String source;

        private String message;

        private String relatedRecordId;

        private String traceId;
    }
}
