package com.yali.mactav.execution.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Health response returned by the Python Mininet/Ryu executor.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MininetRyuHealthResponse(
        String status,
        String pythonVersion,
        Integer configuredPort,
        List<String> ryuExpectedApps,
        String mininetInstalled) {
}
