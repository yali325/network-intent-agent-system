package com.yali.mactav.execution.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

/**
 * Component status response returned by the Python executor.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MininetRyuStatusResponse(
        String status,
        Map<String, Object> details) {
}
