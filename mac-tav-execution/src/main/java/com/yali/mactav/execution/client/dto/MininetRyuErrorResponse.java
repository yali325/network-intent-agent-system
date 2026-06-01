package com.yali.mactav.execution.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yali.mactav.model.workspace.TraceRefs;

/**
 * Structured error returned by the Python executor.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MininetRyuErrorResponse(
        String errorCode,
        String message,
        String stage,
        String actionId,
        String testId,
        Boolean recoverable,
        TraceRefs traceRefs) {
}
