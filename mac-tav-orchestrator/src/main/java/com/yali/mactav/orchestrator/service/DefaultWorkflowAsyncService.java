package com.yali.mactav.orchestrator.service;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.workflow.job.WorkflowJob;
import com.yali.mactav.model.workflow.job.WorkflowJobStatus;
import com.yali.mactav.model.workflow.job.WorkflowJobType;
import com.yali.mactav.modelcore.service.WorkflowJobService;
import com.yali.mactav.orchestrator.job.TaskRunLock;
import com.yali.mactav.orchestrator.job.TaskRunLockService;
import com.yali.mactav.orchestrator.job.WorkflowAsyncExecutor;
import com.yali.mactav.orchestrator.job.WorkflowJobSubmitRequest;
import com.yali.mactav.orchestrator.job.WorkflowJobSubmitResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Default async submitter enforcing MySQL job checks before and after task lock acquisition.
 */
public class DefaultWorkflowAsyncService implements WorkflowAsyncService {

    private final WorkflowQueryService workflowQueryService;
    private final WorkflowJobService workflowJobService;
    private final TaskRunLockService lockService;
    private final WorkflowAsyncExecutor asyncExecutor;

    public DefaultWorkflowAsyncService(WorkflowQueryService workflowQueryService,
                                       WorkflowJobService workflowJobService,
                                       TaskRunLockService lockService,
                                       WorkflowAsyncExecutor asyncExecutor) {
        this.workflowQueryService = workflowQueryService;
        this.workflowJobService = workflowJobService;
        this.lockService = lockService;
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    public WorkflowJobSubmitResponse submitWorkflowStart(String taskId, String requestedBy) {
        return submit(new WorkflowJobSubmitRequest(
                taskId, WorkflowJobType.FULL_WORKFLOW, WorkflowStage.INTENT, requestedBy, null, null));
    }

    @Override
    public WorkflowJobSubmitResponse submitStageRun(String taskId, WorkflowStage stage, String requestedBy) {
        return submit(new WorkflowJobSubmitRequest(taskId, WorkflowJobType.RUN_STAGE, stage, requestedBy, null, null));
    }

    @Override
    public WorkflowJobSubmitResponse submitStageRerun(String taskId, WorkflowStage stage, String requestedBy) {
        return submit(new WorkflowJobSubmitRequest(taskId, WorkflowJobType.RERUN_STAGE, stage, requestedBy, null, null));
    }

    @Override
    public WorkflowJobSubmitResponse submitContinueFrom(String taskId, WorkflowStage stage, String requestedBy) {
        return submit(new WorkflowJobSubmitRequest(
                taskId, WorkflowJobType.CONTINUE_FROM_STAGE, stage, requestedBy, null, null));
    }

    @Override
    public WorkflowJobSubmitResponse submitRepairAnalyze(String taskId, String requestedBy) {
        return submit(new WorkflowJobSubmitRequest(
                taskId, WorkflowJobType.REPAIR_ANALYZE, WorkflowStage.HEALING, requestedBy, null, null));
    }

    @Override
    public WorkflowJobSubmitResponse submitRepairApply(String taskId, String actionId, String requestedBy) {
        return submit(new WorkflowJobSubmitRequest(
                taskId, WorkflowJobType.REPAIR_APPLY, WorkflowStage.HEALING, requestedBy, actionId,
                "{\"actionId\":\"" + (actionId == null ? "" : actionId) + "\"}"));
    }

    @Override
    public WorkflowJob findByJobId(String jobId) {
        return workflowJobService.findByJobId(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Workflow job not found: " + jobId));
    }

    @Override
    public List<WorkflowJob> listByTaskId(String taskId) {
        workflowQueryService.requireWorkspace(taskId);
        return workflowJobService.listByTaskId(taskId);
    }

    private WorkflowJobSubmitResponse submit(WorkflowJobSubmitRequest request) {
        workflowQueryService.requireWorkspace(request.taskId());
        rejectIfActive(request.taskId());

        String jobId = "job-" + UUID.randomUUID();
        TaskRunLock lock = lockService.tryLock(request.taskId(), jobId)
                .orElseThrow(() -> alreadyRunning(request.taskId()));

        WorkflowJob job = null;
        try {
            rejectIfActive(request.taskId());
            job = workflowJobService.createPending(WorkflowJob.builder()
                    .jobId(jobId)
                    .taskId(request.taskId())
                    .requestedStage(request.requestedStage())
                    .jobType(request.jobType())
                    .jobStatus(WorkflowJobStatus.PENDING)
                    .requestedBy(blankToApi(request.requestedBy()))
                    .requestPayloadJson(request.requestPayloadJson())
                    .traceId("trace-" + UUID.randomUUID())
                    .createTime(LocalDateTime.now())
                    .build());
        } catch (RuntimeException exception) {
            lockService.unlock(lock);
            throw exception;
        }

        try {
            asyncExecutor.start(job, request, lock);
        } catch (RuntimeException exception) {
            workflowJobService.markFailed(job.getJobId(), ErrorCode.INTERNAL_ERROR.getErrorCode(), exception.getMessage());
            lockService.unlock(lock);
            throw exception;
        }
        return response(job, "Workflow job submitted");
    }

    private void rejectIfActive(String taskId) {
        workflowJobService.findActiveByTaskId(taskId).ifPresent(active -> {
            throw alreadyRunning(taskId);
        });
    }

    private BusinessException alreadyRunning(String taskId) {
        return new BusinessException(ErrorCode.TASK_ALREADY_RUNNING, "Task already has a pending or running job: " + taskId);
    }

    private WorkflowJobSubmitResponse response(WorkflowJob job, String message) {
        return new WorkflowJobSubmitResponse(
                job.getTaskId(),
                job.getJobId(),
                job.getJobStatus(),
                job.getRequestedStage(),
                job.getJobType(),
                message);
    }

    private String blankToApi(String requestedBy) {
        return requestedBy == null || requestedBy.isBlank() ? "api" : requestedBy;
    }
}
