package com.yali.mactav.execution.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;

/**
 * Runtime state payload returned by the Python executor.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MininetRyuRuntimeStateResponse(
        String executorId,
        String executorEndpoint,
        String ryuControllerStatus,
        String mininetStatus,
        String environmentStatus,
        String logsSummary,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt) {
}
