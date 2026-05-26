package com.yali.mactav.web.controller;

import com.yali.mactav.common.result.ApiResponse;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.orchestrator.service.WorkflowOrchestrator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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

    public WorkspaceController(WorkflowOrchestrator workflowOrchestrator) {
        this.workflowOrchestrator = workflowOrchestrator;
    }

    @GetMapping("/{taskId}")
    public ApiResponse<NetworkWorkspace> getWorkspace(@PathVariable String taskId) {
        return ApiResponse.success(workflowOrchestrator.getWorkspace(taskId));
    }
}
