package com.yali.mactav.web.sse;

import com.yali.mactav.modelcore.event.WorkspaceEventSummary;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Registry for task-scoped SSE emitters with cleanup on completion and errors.
 */
public class SseEmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterRegistry.class);

    private final ConcurrentHashMap<String, CopyOnWriteArraySet<SseEmitter>> emittersByTaskId =
            new ConcurrentHashMap<>();
    private final SseEventMapper eventMapper;

    public SseEmitterRegistry(SseEventMapper eventMapper) {
        this.eventMapper = eventMapper;
    }

    public SseEmitter register(String taskId, long timeoutMs) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        emittersByTaskId.computeIfAbsent(taskId, ignored -> new CopyOnWriteArraySet<>()).add(emitter);
        emitter.onCompletion(() -> unregister(taskId, emitter));
        emitter.onTimeout(() -> {
            unregister(taskId, emitter);
            emitter.complete();
        });
        emitter.onError(error -> unregister(taskId, emitter));
        return emitter;
    }

    public void unregister(String taskId, SseEmitter emitter) {
        Set<SseEmitter> emitters = emittersByTaskId.get(taskId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByTaskId.remove(taskId, emitters);
        }
    }

    public int emit(String taskId, WorkspaceEventSummary summary) {
        Set<SseEmitter> emitters = emittersByTaskId.get(taskId);
        if (emitters == null || emitters.isEmpty()) {
            return 0;
        }
        int delivered = 0;
        for (SseEmitter emitter : emitters) {
            if (send(taskId, emitter, summary)) {
                delivered++;
            }
        }
        return delivered;
    }

    public void sendConnected(String taskId, SseEmitter emitter) {
        send(taskId, emitter, eventMapper.connected(taskId));
    }

    public int emitterCount(String taskId) {
        Set<SseEmitter> emitters = emittersByTaskId.get(taskId);
        return emitters == null ? 0 : emitters.size();
    }

    private boolean send(String taskId, SseEmitter emitter, WorkspaceEventSummary summary) {
        try {
            emitter.send(SseEmitter.event()
                    .id(summary.eventId())
                    .name(summary.eventType())
                    .data(eventMapper.toJson(summary)));
            return true;
        } catch (IOException | IllegalStateException exception) {
            unregister(taskId, emitter);
            try {
                emitter.completeWithError(exception);
            } catch (IllegalStateException ignored) {
                log.debug("SSE emitter already completed for taskId={}", taskId);
            }
            log.warn(
                    "Failed to send SSE event: taskId={}, eventId={}, eventType={}",
                    taskId,
                    summary.eventId(),
                    summary.eventType(),
                    exception);
            return false;
        }
    }
}
