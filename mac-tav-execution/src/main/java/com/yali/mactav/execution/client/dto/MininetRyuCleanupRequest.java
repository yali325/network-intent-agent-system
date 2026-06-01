package com.yali.mactav.execution.client.dto;

import com.yali.mactav.model.execution.ExecutionAction;
import com.yali.mactav.model.workspace.TraceRefs;
import java.util.List;

/**
 * Structured request body sent to the Python executor cleanup endpoint.
 */
public record MininetRyuCleanupRequest(
        String executionId,
        String taskId,
        List<ExecutionAction> cleanupActions,
        TraceRefs traceRefs) {
}
