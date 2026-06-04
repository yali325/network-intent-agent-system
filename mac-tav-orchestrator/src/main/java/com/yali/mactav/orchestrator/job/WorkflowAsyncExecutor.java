package com.yali.mactav.orchestrator.job;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.enums.ValidationStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.model.workflow.job.WorkflowJob;
import com.yali.mactav.model.workflow.job.WorkflowJobType;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import com.yali.mactav.modelcore.event.WorkspaceEventTypes;
import com.yali.mactav.modelcore.service.WorkflowJobService;
import com.yali.mactav.modelcore.service.WorkspaceEventService;
import com.yali.mactav.orchestrator.service.WorkflowOrchestrator;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Asynchronous worker wrapper for job status, workflow events, locks, and sync stage calls.
 */
public class WorkflowAsyncExecutor {

    private final Executor executor;
    private final WorkflowOrchestrator workflowOrchestrator;
    private final WorkflowJobService workflowJobService;
    private final WorkspaceEventService eventService;
    private final TaskRunLockService lockService;

    public WorkflowAsyncExecutor(Executor executor,
                                 WorkflowOrchestrator workflowOrchestrator,
                                 WorkflowJobService workflowJobService,
                                 WorkspaceEventService eventService,
                                 TaskRunLockService lockService) {
        this.executor = executor;
        this.workflowOrchestrator = workflowOrchestrator;
        this.workflowJobService = workflowJobService;
        this.eventService = eventService;
        this.lockService = lockService;
    }

    public void start(WorkflowJob job, WorkflowJobSubmitRequest request, TaskRunLock lock) {
        executor.execute(() -> run(job, request, lock));
    }

    private void run(WorkflowJob job, WorkflowJobSubmitRequest request, TaskRunLock lock) {
        try {
            workflowJobService.markRunning(job.getJobId());
            appendWorkflowEvent(job, WorkspaceEventTypes.WORKFLOW_STARTED, "Workflow job started", "Job started");
            execute(request);
            workflowJobService.markSuccess(job.getJobId());
            appendWorkflowEvent(job, WorkspaceEventTypes.WORKFLOW_COMPLETED, "Workflow job completed", "Job completed");
        } catch (RuntimeException exception) {
            workflowJobService.markFailed(job.getJobId(), errorCode(exception), safeMessage(exception));
            appendWorkflowEvent(job, WorkspaceEventTypes.WORKFLOW_FAILED, "Workflow job failed", safeMessage(exception));
        } finally {
            lockService.unlock(lock);
        }
    }

    private void execute(WorkflowJobSubmitRequest request) {
        if (request.jobType() == WorkflowJobType.FULL_WORKFLOW) {
            runFullWorkflow(request.taskId());
            return;
        }
        if (request.jobType() == WorkflowJobType.REPAIR_ANALYZE) {
            workflowOrchestrator.runHealingStage(request.taskId());
            return;
        }
        if (request.jobType() == WorkflowJobType.REPAIR_APPLY) {
            workflowOrchestrator.applyRepairAction(request.taskId(), request.actionId());
            return;
        }
        runStage(request.taskId(), request.requestedStage());
    }

    private void runFullWorkflow(String taskId) {
        workflowOrchestrator.runIntentStage(taskId);
        workflowOrchestrator.runPlanningStage(taskId);
        workflowOrchestrator.runConfigurationStage(taskId);
        workflowOrchestrator.runExecutionStage(taskId);
        NetworkWorkspace workspace = workflowOrchestrator.runVerificationStage(taskId);
        ValidationReport report = workspace == null ? null : workspace.getCurrentValidationReport();
        ValidationStatus status = report == null ? null : report.getOverallStatus();
        if (status == ValidationStatus.FAILED || status == ValidationStatus.PARTIAL || status == ValidationStatus.UNKNOWN) {
            workflowOrchestrator.runHealingStage(taskId);
        } else if (status != ValidationStatus.PASSED) {
            throw new BusinessException(ErrorCode.WORKSPACE_STATE_INVALID,
                    "ValidationReport status is missing after verification");
        }
    }

    private void runStage(String taskId, WorkflowStage stage) {
        if (stage == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "requestedStage must not be null");
        }
        switch (stage) {
            case INTENT -> workflowOrchestrator.runIntentStage(taskId);
            case PLANNING -> workflowOrchestrator.runPlanningStage(taskId);
            case CONFIGURATION -> workflowOrchestrator.runConfigurationStage(taskId);
            case EXECUTION -> workflowOrchestrator.runExecutionStage(taskId);
            case VERIFICATION -> workflowOrchestrator.runVerificationStage(taskId);
            case HEALING -> workflowOrchestrator.runHealingStage(taskId);
            default -> throw new BusinessException(ErrorCode.PARAM_INVALID, "Unsupported stage: " + stage);
        }
    }

    private void appendWorkflowEvent(WorkflowJob job, String eventType, String title, String message) {
        eventService.appendEvent(job.getTaskId(), WorkspaceEvent.builder()
                .eventId("event-" + UUID.randomUUID())
                .taskId(job.getTaskId())
                .eventType(eventType)
                .stage(job.getRequestedStage())
                .eventTime(LocalDateTime.now())
                .severity(WorkspaceEventTypes.WORKFLOW_FAILED.equals(eventType) ? "ERROR" : "INFO")
                .title(title)
                .message(message)
                .relatedRecordId(job.getJobId())
                .traceId(job.getTraceId())
                .payloadSummary(job.getJobType() + " " + job.getJobStatus())
                .build());
    }

    private String errorCode(RuntimeException exception) {
        return exception instanceof BusinessException businessException
                ? businessException.getErrorCode()
                : ErrorCode.INTERNAL_ERROR.getErrorCode();
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 1024 ? message.substring(0, 1024) : message;
    }
}
