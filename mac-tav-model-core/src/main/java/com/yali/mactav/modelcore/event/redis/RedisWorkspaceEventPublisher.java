package com.yali.mactav.modelcore.event.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import com.yali.mactav.modelcore.event.WorkspaceEventPublisher;
import com.yali.mactav.modelcore.event.WorkspaceEventSummary;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis-backed publisher for persisted workspace event summaries.
 */
public class RedisWorkspaceEventPublisher implements WorkspaceEventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String channelPrefix;

    public RedisWorkspaceEventPublisher(StringRedisTemplate redisTemplate,
                                        ObjectMapper objectMapper,
                                        String channelPrefix) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.channelPrefix = channelPrefix;
    }

    @Override
    public void publish(WorkspaceEvent event) {
        if (event == null || event.getTaskId() == null || event.getTaskId().isBlank()) {
            return;
        }
        redisTemplate.convertAndSend(channelPrefix + event.getTaskId(), toJson(event));
    }

    private String toJson(WorkspaceEvent event) {
        try {
            return objectMapper.writeValueAsString(WorkspaceEventSummary.from(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize workspace event summary", exception);
        }
    }
}
