package com.yali.mactav.web.controller;

import com.yali.mactav.common.result.ApiResponse;
import com.yali.mactav.model.workflow.job.WorkflowJob;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.orchestrator.service.WorkflowAsyncService;
import com.yali.mactav.orchestrator.service.WorkflowOrchestrator;
import com.yali.mactav.web.dto.CreateTaskRequest;
import com.yali.mactav.web.dto.TaskSummaryResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final WorkflowAsyncService workflowAsyncService;

    public TaskController(WorkflowOrchestrator workflowOrchestrator, WorkflowAsyncService workflowAsyncService) {
        this.workflowOrchestrator = workflowOrchestrator;
        this.workflowAsyncService = workflowAsyncService;
    }

    @PostMapping
    public ApiResponse<TaskSummaryResponse> createTask(@RequestBody CreateTaskRequest request) {
        NetworkWorkspace workspace = workflowOrchestrator.createTask(
                request == null ? null : request.getRawText(),
                request == null ? null : request.getTargetEnvironmentHint(),
                request == null ? null : request.getCreatedBy());
        return ApiResponse.success(TaskSummaryResponse.from(workspace));
    }

    @GetMapping("/{taskId}/jobs")
    public ApiResponse<List<WorkflowJob>> listTaskJobs(@PathVariable String taskId) {
        return ApiResponse.success(workflowAsyncService.listByTaskId(taskId).stream()
                .map(this::summarize)
                .toList());
    }

    private WorkflowJob summarize(WorkflowJob job) {
        job.setRequestPayloadJson(null);
        return job;
    }
}
