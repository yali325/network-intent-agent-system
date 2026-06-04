package com.yali.mactav.web.controller;

import com.yali.mactav.orchestrator.service.WorkflowQueryService;
import com.yali.mactav.web.sse.SseEmitterRegistry;
import com.yali.mactav.web.sse.SseProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Web API controller for task-scoped Server-Sent Events streams.
 */
@RestController
@RequestMapping("/api/v1/events")
public class SseController {

    private final WorkflowQueryService workflowQueryService;
    private final SseEmitterRegistry emitterRegistry;
    private final SseProperties sseProperties;

    public SseController(WorkflowQueryService workflowQueryService,
                         SseEmitterRegistry emitterRegistry,
                         SseProperties sseProperties) {
        this.workflowQueryService = workflowQueryService;
        this.emitterRegistry = emitterRegistry;
        this.sseProperties = sseProperties;
    }

    @GetMapping(value = "/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@PathVariable String taskId) {
        workflowQueryService.requireWorkspace(taskId);
        SseEmitter emitter = emitterRegistry.register(taskId, sseProperties.resolvedTimeoutMs());
        emitterRegistry.sendConnected(taskId, emitter);
        return emitter;
    }
}
