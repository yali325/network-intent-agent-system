package com.yali.mactav.execution.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Structured run or cleanup response returned by the Python executor.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MininetRyuRunResponse(
        String status,
        String executionId,
        MininetRyuRuntimeStateResponse runtimeState,
        List<MininetRyuTestResultResponse> testResults,
        Map<String, Object> flowStats,
        List<MininetRyuErrorResponse> errors,
        String logsSummary,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt) {
}
