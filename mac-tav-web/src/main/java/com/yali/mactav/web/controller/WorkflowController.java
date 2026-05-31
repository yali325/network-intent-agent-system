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
 * <p>Each endpoint delegates to Orchestrator and must not call concrete agent
 * beans, construct prompts, or directly invoke model APIs.</p>
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

    @PostMapping("/{taskId}/plan")
    public ApiResponse<NetworkWorkspace> runPlanningStage(@PathVariable String taskId) {
        return ApiResponse.success(workflowOrchestrator.runPlanningStage(taskId));
    }

    @PostMapping("/{taskId}/config")
    public ApiResponse<NetworkWorkspace> runConfigurationStage(@PathVariable String taskId) {
        return ApiResponse.success(workflowOrchestrator.runConfigurationStage(taskId));
    }
}
