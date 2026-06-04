package com.yali.mactav.web.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.modelcore.event.WorkspaceEventSummary;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Maps Redis event messages into safe SSE payload summaries.
 */
public class SseEventMapper {

    private final ObjectMapper objectMapper;

    public SseEventMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public WorkspaceEventSummary fromJson(String json) {
        try {
            return objectMapper.readValue(json, WorkspaceEventSummary.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid workspace event summary JSON", exception);
        }
    }

    public String toJson(WorkspaceEventSummary summary) {
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize workspace event summary", exception);
        }
    }

    public WorkspaceEventSummary connected(String taskId) {
        return new WorkspaceEventSummary(
                "connected-" + UUID.randomUUID(),
                taskId,
                "connected",
                null,
                LocalDateTime.now(),
                "INFO",
                "SSE connected",
                "SSE connection established",
                null,
                null,
                null,
                "connected");
    }
}
