package com.yali.mactav.web.controller;

import com.yali.mactav.common.result.ApiResponse;
import com.yali.mactav.common.result.PageResult;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import com.yali.mactav.modelcore.query.WorkspaceEventQuery;
import com.yali.mactav.orchestrator.service.WorkflowQueryService;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web API controller for durable workspace event history queries.
 */
@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final WorkflowQueryService workflowQueryService;

    public EventController(WorkflowQueryService workflowQueryService) {
        this.workflowQueryService = workflowQueryService;
    }

    @GetMapping("/{taskId}/history")
    public ApiResponse<PageResult<WorkspaceEvent>> getEventHistory(
            @PathVariable String taskId,
            @RequestParam(required = false) WorkflowStage stage,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(workflowQueryService.listEventHistory(
                taskId,
                new WorkspaceEventQuery(stage, eventType, from, to, page, size)));
    }
}
