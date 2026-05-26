package com.yali.mactav.web.controller;

import com.yali.mactav.common.result.ApiResponse;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.orchestrator.service.WorkflowOrchestrator;
import com.yali.mactav.web.dto.CreateTaskRequest;
import com.yali.mactav.web.dto.TaskSummaryResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web API controller for task creation.
 *
 * <p>The controller delegates to Orchestrator only. It does not construct
 * prompts, invoke ChatModel/ReactAgent, or import concrete agent modules.</p>
 */
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final WorkflowOrchestrator workflowOrchestrator;

    public TaskController(WorkflowOrchestrator workflowOrchestrator) {
        this.workflowOrchestrator = workflowOrchestrator;
    }

    @PostMapping
    public ApiResponse<TaskSummaryResponse> createTask(@RequestBody CreateTaskRequest request) {
        NetworkWorkspace workspace = workflowOrchestrator.createTask(
                request == null ? null : request.getRawText(),
                request == null ? null : request.getTargetEnvironmentHint(),
                request == null ? null : request.getCreatedBy());
        return ApiResponse.success(TaskSummaryResponse.from(workspace));
    }
}
