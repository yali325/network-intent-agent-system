package com.yali.mactav.web.controller;

import com.yali.mactav.common.result.ApiResponse;
import com.yali.mactav.common.result.PageResult;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.model.workspace.WorkspaceChangeRecord;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import com.yali.mactav.modelcore.query.WorkspaceChangeQuery;
import com.yali.mactav.modelcore.query.WorkspaceEventQuery;
import com.yali.mactav.orchestrator.service.WorkflowOrchestrator;
import com.yali.mactav.orchestrator.service.WorkflowQueryService;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web API controller for querying the current NetworkWorkspace view.
 *
 * <p>The controller is read-only and delegates workspace lookup to Orchestrator,
 * preserving Web/Model Core boundaries.</p>
 */
@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkflowOrchestrator workflowOrchestrator;

    private final WorkflowQueryService workflowQueryService;

    public WorkspaceController(WorkflowOrchestrator workflowOrchestrator) {
        this(workflowOrchestrator, null);
    }

    @Autowired
    public WorkspaceController(WorkflowOrchestrator workflowOrchestrator,
                               WorkflowQueryService workflowQueryService) {
        this.workflowOrchestrator = workflowOrchestrator;
        this.workflowQueryService = workflowQueryService;
    }

    @GetMapping("/{taskId}")
    public ApiResponse<NetworkWorkspace> getWorkspace(@PathVariable String taskId) {
        return ApiResponse.success(workflowOrchestrator.getWorkspace(taskId));
    }

    @GetMapping("/{taskId}/timeline")
    public ApiResponse<PageResult<WorkspaceEvent>> getTimeline(
            @PathVariable String taskId,
            @RequestParam(required = false) WorkflowStage stage,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(workflowQueryService.listTimeline(
                taskId,
                new WorkspaceEventQuery(stage, eventType, from, to, page, size)));
    }

    @GetMapping("/{taskId}/changes")
    public ApiResponse<PageResult<WorkspaceChangeRecord>> getChanges(
            @PathVariable String taskId,
            @RequestParam(required = false) WorkflowStage stage,
            @RequestParam(required = false) String changeType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(workflowQueryService.listChanges(
                taskId,
                new WorkspaceChangeQuery(stage, changeType, from, to, page, size)));
    }
}
