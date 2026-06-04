package com.yali.mactav.orchestrator.job;

import com.yali.mactav.model.workflow.job.WorkflowJob;
import com.yali.mactav.modelcore.event.WorkspaceEventTypes;
import com.yali.mactav.modelcore.service.WorkflowJobService;
import com.yali.mactav.modelcore.service.WorkspaceEventService;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * Marks stale PENDING/RUNNING jobs interrupted when their runtime task lock is gone.
 */
public class DefaultWorkflowJobRecoveryService implements WorkflowJobRecoveryService, ApplicationRunner {

    private final WorkflowJobService workflowJobService;
    private final TaskRunLockService lockService;
    private final WorkspaceEventService eventService;

    public DefaultWorkflowJobRecoveryService(
            WorkflowJobService workflowJobService,
            TaskRunLockService lockService,
            WorkspaceEventService eventService) {
        this.workflowJobService = workflowJobService;
        this.lockService = lockService;
        this.eventService = eventService;
    }

    @Override
    public void run(ApplicationArguments args) {
        recoverInterruptedJobs();
    }

    @Override
    public int recoverInterruptedJobs() {
        int recovered = 0;
        for (WorkflowJob job : workflowJobService.listActiveJobs()) {
            if (lockService.isLocked(job.getTaskId())) {
                continue;
            }
            workflowJobService.markInterrupted(
                    job.getJobId(),
                    "WORKFLOW_INTERRUPTED",
                    "Workflow job was active during startup but no task lock exists");
            eventService.appendEvent(job.getTaskId(), WorkspaceEvent.builder()
                    .eventId("event-" + UUID.randomUUID())
                    .taskId(job.getTaskId())
                    .eventType(WorkspaceEventTypes.WORKFLOW_INTERRUPTED)
                    .stage(job.getRequestedStage())
                    .eventTime(LocalDateTime.now())
                    .severity("WARN")
                    .title("Workflow job interrupted")
                    .message("Workflow job " + job.getJobId() + " was marked INTERRUPTED during startup recovery")
                    .traceId(job.getTraceId())
                    .payloadSummary("jobId=" + job.getJobId() + ", jobType=" + job.getJobType())
                    .build());
            recovered++;
        }
        return recovered;
    }
}
