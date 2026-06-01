package com.yali.mactav.web.controller;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.common.result.ApiResponse;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.orchestrator.service.WorkflowOrchestrator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web API controller for triggering and querying the execution stage.
 *
 * <p>The controller delegates workflow progression to Orchestrator and only
 * reads the current execution artifact from the workspace view.</p>
 */
@RestController
@RequestMapping("/api/v1/executions")
public class ExecutionController {

    private final WorkflowOrchestrator workflowOrchestrator;

    public ExecutionController(WorkflowOrchestrator workflowOrchestrator) {
        this.workflowOrchestrator = workflowOrchestrator;
    }

    @PostMapping("/{taskId}/run")
    public ApiResponse<ExecutionReport> runExecutionStage(@PathVariable String taskId) {
        NetworkWorkspace workspace = workflowOrchestrator.runExecutionStage(taskId);
        return ApiResponse.success(requireExecutionReport(workspace, taskId));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<ExecutionReport> getExecutionReport(@PathVariable String taskId) {
        NetworkWorkspace workspace = workflowOrchestrator.getWorkspace(taskId);
        return ApiResponse.success(requireExecutionReport(workspace, taskId));
    }

    private ExecutionReport requireExecutionReport(NetworkWorkspace workspace, String taskId) {
        if (workspace == null || workspace.getCurrentExecutionReport() == null) {
            throw new BusinessException(
                    ErrorCode.ARTIFACT_NOT_FOUND,
                    "Current execution report not found for task: " + taskId);
        }
        return workspace.getCurrentExecutionReport();
    }
}
