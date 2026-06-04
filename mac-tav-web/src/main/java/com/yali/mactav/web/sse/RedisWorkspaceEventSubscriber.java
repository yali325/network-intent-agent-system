package com.yali.mactav.web.sse;

import com.yali.mactav.modelcore.event.WorkspaceEventSummary;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

/**
 * Redis pattern subscriber that fans workspace events out to task SSE emitters.
 */
public class RedisWorkspaceEventSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisWorkspaceEventSubscriber.class);

    private final SseEmitterRegistry emitterRegistry;
    private final SseEventMapper eventMapper;

    public RedisWorkspaceEventSubscriber(SseEmitterRegistry emitterRegistry, SseEventMapper eventMapper) {
        this.emitterRegistry = emitterRegistry;
        this.eventMapper = eventMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String json = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            WorkspaceEventSummary summary = eventMapper.fromJson(json);
            if (summary.taskId() == null || summary.taskId().isBlank()) {
                log.warn("Ignored Redis workspace event without taskId");
                return;
            }
            emitterRegistry.emit(summary.taskId(), summary);
        } catch (RuntimeException exception) {
            log.warn("Ignored invalid Redis workspace event message", exception);
        }
    }
}
