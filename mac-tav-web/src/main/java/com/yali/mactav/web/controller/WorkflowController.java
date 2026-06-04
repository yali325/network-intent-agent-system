package com.yali.mactav.web.controller;

import com.yali.mactav.common.result.ApiResponse;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.workflow.job.WorkflowJob;
import com.yali.mactav.orchestrator.job.WorkflowJobSubmitResponse;
import com.yali.mactav.orchestrator.service.WorkflowAsyncService;
import org.springframework.web.bind.annotation.GetMapping;
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

    private final WorkflowAsyncService workflowAsyncService;

    public WorkflowController(WorkflowAsyncService workflowAsyncService) {
        this.workflowAsyncService = workflowAsyncService;
    }

    @PostMapping("/{taskId}/run")
    public ApiResponse<WorkflowJobSubmitResponse> runIntentStage(@PathVariable String taskId) {
        return ApiResponse.success(workflowAsyncService.submitStageRun(taskId, WorkflowStage.INTENT, "api"));
    }

    @PostMapping("/{taskId}/plan")
    public ApiResponse<WorkflowJobSubmitResponse> runPlanningStage(@PathVariable String taskId) {
        return ApiResponse.success(workflowAsyncService.submitStageRun(taskId, WorkflowStage.PLANNING, "api"));
    }

    @PostMapping("/{taskId}/config")
    public ApiResponse<WorkflowJobSubmitResponse> runConfigurationStage(@PathVariable String taskId) {
        return ApiResponse.success(workflowAsyncService.submitStageRun(taskId, WorkflowStage.CONFIGURATION, "api"));
    }

    @PostMapping("/{taskId}/start")
    public ApiResponse<WorkflowJobSubmitResponse> startWorkflow(@PathVariable String taskId) {
        return ApiResponse.success(workflowAsyncService.submitWorkflowStart(taskId, "api"));
    }

    @PostMapping("/{taskId}/rerun/{stage}")
    public ApiResponse<WorkflowJobSubmitResponse> rerunStage(@PathVariable String taskId,
                                                             @PathVariable WorkflowStage stage) {
        return ApiResponse.success(workflowAsyncService.submitStageRerun(taskId, stage, "api"));
    }

    @PostMapping("/{taskId}/continue-from/{stage}")
    public ApiResponse<WorkflowJobSubmitResponse> continueFrom(@PathVariable String taskId,
                                                               @PathVariable WorkflowStage stage) {
        return ApiResponse.success(workflowAsyncService.submitContinueFrom(taskId, stage, "api"));
    }

    @GetMapping("/jobs/{jobId}")
    public ApiResponse<WorkflowJob> getJob(@PathVariable String jobId) {
        return ApiResponse.success(summarize(workflowAsyncService.findByJobId(jobId)));
    }

    private WorkflowJob summarize(WorkflowJob job) {
        job.setRequestPayloadJson(null);
        return job;
    }
}
