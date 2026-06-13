package com.yali.mactav.orchestrator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.common.result.PageResult;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.workflow.job.WorkflowJob;
import com.yali.mactav.model.workflow.job.WorkflowJobStatus;
import com.yali.mactav.model.workflow.job.WorkflowJobType;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.model.workspace.WorkspaceChangeRecord;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import com.yali.mactav.modelcore.event.WorkspaceEventTypes;
import com.yali.mactav.modelcore.query.ArtifactQuery;
import com.yali.mactav.modelcore.query.WorkspaceChangeQuery;
import com.yali.mactav.modelcore.query.WorkspaceEventQuery;
import com.yali.mactav.modelcore.service.InMemoryWorkflowJobService;
import com.yali.mactav.modelcore.service.WorkspaceEventService;
import com.yali.mactav.orchestrator.job.DefaultWorkflowJobRecoveryService;
import com.yali.mactav.orchestrator.job.InMemoryTaskRunLockService;
import com.yali.mactav.orchestrator.job.TaskRunLock;
import com.yali.mactav.orchestrator.job.WorkflowAsyncExecutor;
import com.yali.mactav.orchestrator.job.WorkflowJobSubmitRequest;
import com.yali.mactav.orchestrator.job.WorkflowJobSubmitResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Offline tests for async workflow job submission and duplicate protection.
 */
class DefaultWorkflowAsyncServiceTest {

    @Test
    void submitShouldCreatePendingJobAndRejectDuplicateActiveJob() {
        InMemoryWorkflowJobService jobService = new InMemoryWorkflowJobService();
        DefaultWorkflowAsyncService service = new DefaultWorkflowAsyncService(
                new TestWorkflowQueryService(),
                jobService,
                new InMemoryTaskRunLockService(),
                new NonStartingWorkflowAsyncExecutor());

        WorkflowJobSubmitResponse response = service.submitWorkflowStart("task-async-test", "unit-test");

        assertEquals("task-async-test", response.getTaskId());
        assertEquals(WorkflowJobStatus.PENDING, response.getJobStatus());
        assertTrue(jobService.findActiveByTaskId("task-async-test").isPresent());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.submitWorkflowStart("task-async-test", "unit-test"));
        assertEquals(ErrorCode.TASK_ALREADY_RUNNING.getErrorCode(), exception.getErrorCode());
    }

    @Test
    void recoveryShouldInterruptActiveJobWhenTaskLockIsMissing() {
        InMemoryWorkflowJobService jobService = new InMemoryWorkflowJobService();
        TestWorkspaceEventService eventService = new TestWorkspaceEventService();
        jobService.createPending(WorkflowJob.builder()
                .jobId("job-recovery-test")
                .taskId("task-recovery-test")
                .jobType(WorkflowJobType.FULL_WORKFLOW)
                .requestedStage(WorkflowStage.INTENT)
                .requestedBy("unit-test")
                .build());
        DefaultWorkflowJobRecoveryService recoveryService = new DefaultWorkflowJobRecoveryService(
                jobService,
                new InMemoryTaskRunLockService(),
                eventService);

        int recovered = recoveryService.recoverInterruptedJobs();

        assertEquals(1, recovered);
        assertEquals(WorkflowJobStatus.INTERRUPTED, jobService.findByJobId("job-recovery-test")
                .orElseThrow().getJobStatus());
        assertEquals(WorkspaceEventTypes.WORKFLOW_INTERRUPTED, eventService.events.get(0).getEventType());
    }

    @Test
    void remoteTimeoutShouldMarkJobFailedAndReleaseActiveJob() {
        InMemoryWorkflowJobService jobService = new InMemoryWorkflowJobService();
        TestWorkspaceEventService eventService = new TestWorkspaceEventService();
        InMemoryTaskRunLockService lockService = new InMemoryTaskRunLockService();
        WorkflowAsyncExecutor asyncExecutor = new WorkflowAsyncExecutor(
                Runnable::run,
                new TimeoutWorkflowOrchestrator(),
                jobService,
                eventService,
                lockService);
        DefaultWorkflowAsyncService service = new DefaultWorkflowAsyncService(
                new TestWorkflowQueryService(),
                jobService,
                lockService,
                asyncExecutor);

        WorkflowJobSubmitResponse response = service.submitStageRun("task-timeout-test", WorkflowStage.INTENT, "unit-test");

        WorkflowJob job = jobService.findByJobId(response.getJobId()).orElseThrow();
        assertEquals(WorkflowJobStatus.FAILED, job.getJobStatus());
        assertEquals(ErrorCode.REMOTE_AGENT_TIMEOUT.getErrorCode(), job.getErrorCode());
        assertTrue(job.getErrorMessage().contains("A2A timeout"));
        assertTrue(jobService.findActiveByTaskId("task-timeout-test").isEmpty());
    }

    /**
     * Executor fixture that accepts startup but leaves the job PENDING for duplicate-submit testing.
     */
    private static class NonStartingWorkflowAsyncExecutor extends WorkflowAsyncExecutor {

        private NonStartingWorkflowAsyncExecutor() {
            super(Runnable::run, null, null, null, null);
        }

        @Override
        public void start(com.yali.mactav.model.workflow.job.WorkflowJob job,
                          WorkflowJobSubmitRequest request,
                          TaskRunLock lock) {
            // Keep job PENDING and lock held to simulate a submitted worker.
        }
    }

    /**
     * Query facade fixture that validates task existence without persistence.
     */
    private static class TestWorkflowQueryService implements WorkflowQueryService {

        @Override
        public void requireWorkspace(String taskId) {
            // Workspace exists in this fixture.
        }

        @Override
        public PageResult<NetworkArtifact> listArtifacts(String taskId, ArtifactQuery query) {
            return emptyPage(query.page(), query.size());
        }

        @Override
        public NetworkArtifact getArtifact(String taskId, String artifactId) {
            return null;
        }

        @Override
        public NetworkArtifact getCurrentArtifact(String taskId, ArtifactType artifactType) {
            return null;
        }

        @Override
        public PageResult<NetworkArtifact> listArtifactVersions(String taskId, String artifactId, int page, int size) {
            return emptyPage(page, size);
        }

        @Override
        public ArtifactDiffResult diffArtifactVersions(String taskId,
                                                       String artifactId,
                                                       Integer fromVersion,
                                                       Integer toVersion) {
            return null;
        }

        @Override
        public PageResult<WorkspaceEvent> listTimeline(String taskId, WorkspaceEventQuery query) {
            return emptyPage(query.page(), query.size());
        }

        @Override
        public PageResult<WorkspaceChangeRecord> listChanges(String taskId, WorkspaceChangeQuery query) {
            return emptyPage(query.page(), query.size());
        }

        @Override
        public PageResult<WorkspaceEvent> listEventHistory(String taskId, WorkspaceEventQuery query) {
            return emptyPage(query.page(), query.size());
        }

        private <T> PageResult<T> emptyPage(int page, int size) {
            return PageResult.<T>builder().items(List.of()).page(page).size(size).total(0).build();
        }
    }

    /**
     * Orchestrator fixture that simulates an A2A timeout without real remote services.
     */
    private static class TimeoutWorkflowOrchestrator implements WorkflowOrchestrator {

        @Override
        public NetworkWorkspace createTask(String rawText, String targetEnvironmentHint, String createdBy) {
            return null;
        }

        @Override
        public NetworkWorkspace runIntentStage(String taskId) {
            throw new BusinessException(ErrorCode.REMOTE_AGENT_TIMEOUT, "A2A timeout while invoking IntentAgent");
        }

        @Override
        public NetworkWorkspace runPlanningStage(String taskId) {
            return null;
        }

        @Override
        public NetworkWorkspace runConfigurationStage(String taskId) {
            return null;
        }

        @Override
        public NetworkWorkspace runExecutionStage(String taskId) {
            return null;
        }

        @Override
        public NetworkWorkspace runVerificationStage(String taskId) {
            return null;
        }

        @Override
        public NetworkWorkspace runHealingStage(String taskId) {
            return null;
        }

        @Override
        public NetworkWorkspace approveRepairAction(String taskId, String actionId, String approvedBy, String comment) {
            return null;
        }

        @Override
        public NetworkWorkspace rejectRepairAction(String taskId, String actionId, String rejectedBy, String comment) {
            return null;
        }

        @Override
        public NetworkWorkspace applyRepairAction(String taskId, String actionId) {
            return null;
        }

        @Override
        public NetworkWorkspace getWorkspace(String taskId) {
            return null;
        }
    }

    /**
     * Event service fixture that records recovery events without durable storage.
     */
    private static class TestWorkspaceEventService implements WorkspaceEventService {

        private final List<WorkspaceEvent> events = new java.util.ArrayList<>();

        @Override
        public WorkspaceEvent appendEvent(String taskId, WorkspaceEvent event) {
            events.add(event);
            return event;
        }

        @Override
        public List<WorkspaceEvent> listEvents(String taskId) {
            return events;
        }

        @Override
        public PageResult<WorkspaceEvent> listEvents(String taskId, WorkspaceEventQuery query) {
            return PageResult.<WorkspaceEvent>builder()
                    .items(events)
                    .page(query.page())
                    .size(query.size())
                    .total(events.size())
                    .build();
        }
    }
}
