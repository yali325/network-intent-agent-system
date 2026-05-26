package com.yali.mactav.web.controller;

import com.yali.mactav.common.result.ApiResponse;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.orchestrator.service.WorkflowOrchestrator;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web API controller for triggering currently implemented workflow stages.
 *
 * <p>For this phase the run endpoint invokes only the INTENT stage through
 * Orchestrator. It must not start Phase 4 planning or call concrete agent beans.</p>
 */
@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowOrchestrator workflowOrchestrator;

    public WorkflowController(WorkflowOrchestrator workflowOrchestrator) {
        this.workflowOrchestrator = workflowOrchestrator;
    }

    @PostMapping("/{taskId}/run")
    public ApiResponse<NetworkWorkspace> runIntentStage(@PathVariable String taskId) {
        return ApiResponse.success(workflowOrchestrator.runIntentStage(taskId));
    }
}
