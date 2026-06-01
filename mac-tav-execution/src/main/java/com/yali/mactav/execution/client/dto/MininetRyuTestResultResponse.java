package com.yali.mactav.execution.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yali.mactav.model.workspace.TraceRefs;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * One test result returned by the Python executor.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MininetRyuTestResultResponse(
        String testId,
        String testType,
        String status,
        String sourceNodeId,
        String targetNodeId,
        String sourceNode,
        String targetNode,
        String expectedResult,
        String actualResult,
        String resultSummary,
        String stdoutSummary,
        String stderrSummary,
        Map<String, Object> metrics,
        String logsSummary,
        Map<String, String> evidenceRefs,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        Long durationMs,
        TraceRefs traceRefs) {
}
