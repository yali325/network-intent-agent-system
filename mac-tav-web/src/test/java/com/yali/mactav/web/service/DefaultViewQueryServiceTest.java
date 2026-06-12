package com.yali.mactav.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.common.result.PageResult;
import com.yali.mactav.model.enums.ArtifactStatus;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.TaskStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.intent.IntentNode;
import com.yali.mactav.model.intent.IntentRelation;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.intent.SemanticIntentGraph;
import com.yali.mactav.model.task.NetworkTask;
import com.yali.mactav.model.workflow.job.WorkflowJob;
import com.yali.mactav.model.workflow.job.WorkflowJobStatus;
import com.yali.mactav.model.workflow.job.WorkflowJobType;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.model.workspace.WorkspaceChangeRecord;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import com.yali.mactav.modelcore.query.ArtifactQuery;
import com.yali.mactav.modelcore.query.WorkspaceChangeQuery;
import com.yali.mactav.modelcore.query.WorkspaceEventQuery;
import com.yali.mactav.orchestrator.job.WorkflowJobSubmitResponse;
import com.yali.mactav.orchestrator.service.ArtifactDiffResult;
import com.yali.mactav.orchestrator.service.WorkflowAsyncService;
import com.yali.mactav.orchestrator.service.WorkflowOrchestrator;
import com.yali.mactav.orchestrator.service.WorkflowQueryService;
import com.yali.mactav.web.dto.view.ConfigBlocksView;
import com.yali.mactav.web.dto.view.ExecutionLogsView;
import com.yali.mactav.web.dto.view.TopologyView;
import com.yali.mactav.web.dto.view.WorkspaceSummaryView;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Offline tests for read-only frontend view projections.
 */
class DefaultViewQueryServiceTest {

    @Test
    void workspaceWithoutArtifactsShouldReturnNotReadyViews() {
        DefaultViewQueryService service = service(workspace(null), List.of());

        WorkspaceSummaryView summary = service.getWorkspaceSummary("task-view-test");
        ConfigBlocksView configBlocks = service.getConfigBlocks("task-view-test");
        ExecutionLogsView executionLogs = service.getExecutionLogs("task-view-test");
        TopologyView topology = service.getTopology("task-view-test");

        assertFalse(summary.getReadiness().isReady());
        assertEquals("MISSING_ARTIFACT", summary.getReadiness().getReasonCode());
        assertTrue(summary.getMissingArtifacts().contains("CONFIG_SET"));
        assertEquals("NOT_READY", configBlocks.getStatus());
        assertEquals("CONFIG_SET_NOT_FOUND", configBlocks.getReasonCode());
        assertEquals("NOT_READY", executionLogs.getStatus());
        assertEquals("EXECUTION_REPORT_NOT_FOUND", executionLogs.getReasonCode());
        assertEquals("NOT_READY", topology.getStatus());
        assertEquals("NETWORK_TOPOLOGY_NOT_READY", topology.getReasonCode());
    }

    @Test
    void intentOnlyWorkspaceShouldReturnPartialTopology() {
        NetworkIntent intent = NetworkIntent.builder()
                .taskId("task-view-test")
                .semanticIntentGraph(SemanticIntentGraph.builder()
                        .nodes(List.of(IntentNode.builder()
                                .id("client-zone")
                                .name("client-zone")
                                .type("ZONE")
                                .build()))
                        .relations(List.of(IntentRelation.builder()
                                .id("rel-1")
                                .source("client-zone")
                                .target("server-zone")
                                .action("ALLOW")
                                .service("HTTPS")
                                .build()))
                        .build())
                .build();
        DefaultViewQueryService service = service(
                workspace(intent),
                List.of(artifact(ArtifactType.NETWORK_INTENT, WorkflowStage.INTENT)));

        TopologyView topology = service.getTopology("task-view-test");

        assertEquals("PARTIAL", topology.getStatus());
        assertFalse(topology.isReady());
        assertEquals("NETWORK_PLAN_NOT_FOUND", topology.getReasonCode());
        assertEquals(1, topology.getDevices().size());
        assertEquals(1, topology.getLinks().size());
    }

    private DefaultViewQueryService service(NetworkWorkspace workspace, List<NetworkArtifact> artifacts) {
        return new DefaultViewQueryService(
                new TestWorkflowOrchestrator(workspace),
                new TestWorkflowQueryService(artifacts),
                new TestWorkflowAsyncService());
    }

    private NetworkWorkspace workspace(NetworkIntent intent) {
        return NetworkWorkspace.builder()
                .task(NetworkTask.builder()
                        .taskId("task-view-test")
                        .rawText("client can access server")
                        .taskStatus(TaskStatus.CREATED)
                        .currentStage(WorkflowStage.INTENT)
                        .createTime(LocalDateTime.now())
                        .build())
                .currentIntent(intent)
                .workspaceStatus(TaskStatus.CREATED)
                .build();
    }

    private NetworkArtifact artifact(ArtifactType artifactType, WorkflowStage stage) {
        return NetworkArtifact.builder()
                .artifactId("artifact-" + artifactType.name().toLowerCase())
                .taskId("task-view-test")
                .artifactType(artifactType)
                .stage(stage)
                .version(1)
                .status(ArtifactStatus.GENERATED)
                .payloadSummary("real artifact summary")
                .createTime(LocalDateTime.now())
                .createdBy("unit-test")
                .build();
    }

    /**
     * Minimal orchestrator fixture for view read-side tests.
     */
    private static class TestWorkflowOrchestrator implements WorkflowOrchestrator {

        private final NetworkWorkspace workspace;

        private TestWorkflowOrchestrator(NetworkWorkspace workspace) {
            this.workspace = workspace;
        }

        @Override
        public NetworkWorkspace createTask(String rawText, String targetEnvironmentHint, String createdBy) {
            return workspace;
        }

        @Override
        public NetworkWorkspace runIntentStage(String taskId) {
            return workspace;
        }

        @Override
        public NetworkWorkspace runPlanningStage(String taskId) {
            return workspace;
        }

        @Override
        public NetworkWorkspace runConfigurationStage(String taskId) {
            return workspace;
        }

        @Override
        public NetworkWorkspace runExecutionStage(String taskId) {
            return workspace;
        }

        @Override
        public NetworkWorkspace runVerificationStage(String taskId) {
            return workspace;
        }

        @Override
        public NetworkWorkspace runHealingStage(String taskId) {
            return workspace;
        }

        @Override
        public NetworkWorkspace approveRepairAction(String taskId, String actionId, String approvedBy, String comment) {
            return workspace;
        }

        @Override
        public NetworkWorkspace rejectRepairAction(String taskId, String actionId, String rejectedBy, String comment) {
            return workspace;
        }

        @Override
        public NetworkWorkspace applyRepairAction(String taskId, String actionId) {
            return workspace;
        }

        @Override
        public NetworkWorkspace getWorkspace(String taskId) {
            return workspace;
        }
    }

    /**
     * Minimal query fixture returning caller-provided artifacts and no events.
     */
    private static class TestWorkflowQueryService implements WorkflowQueryService {

        private final List<NetworkArtifact> artifacts;

        private TestWorkflowQueryService(List<NetworkArtifact> artifacts) {
            this.artifacts = artifacts;
        }

        @Override
        public void requireWorkspace(String taskId) {
        }

        @Override
        public PageResult<NetworkArtifact> listArtifacts(String taskId, ArtifactQuery query) {
            List<NetworkArtifact> filtered = artifacts.stream()
                    .filter(artifact -> query.artifactType() == null
                            || query.artifactType() == artifact.getArtifactType())
                    .toList();
            return PageResult.<NetworkArtifact>builder()
                    .items(filtered)
                    .page(query.page())
                    .size(query.size())
                    .total(filtered.size())
                    .build();
        }

        @Override
        public NetworkArtifact getArtifact(String taskId, String artifactId) {
            return artifacts.get(0);
        }

        @Override
        public NetworkArtifact getCurrentArtifact(String taskId, ArtifactType artifactType) {
            return artifacts.stream()
                    .filter(artifact -> artifact.getArtifactType() == artifactType)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public PageResult<NetworkArtifact> listArtifactVersions(String taskId, String artifactId, int page, int size) {
            return PageResult.<NetworkArtifact>builder().items(artifacts).page(page).size(size).total(artifacts.size()).build();
        }

        @Override
        public ArtifactDiffResult diffArtifactVersions(String taskId, String artifactId, Integer fromVersion, Integer toVersion) {
            return new ArtifactDiffResult(null, null);
        }

        @Override
        public PageResult<WorkspaceEvent> listTimeline(String taskId, WorkspaceEventQuery query) {
            return emptyEvents(query.page(), query.size());
        }

        @Override
        public PageResult<WorkspaceChangeRecord> listChanges(String taskId, WorkspaceChangeQuery query) {
            return PageResult.<WorkspaceChangeRecord>builder().items(List.of()).page(query.page()).size(query.size()).total(0).build();
        }

        @Override
        public PageResult<WorkspaceEvent> listEventHistory(String taskId, WorkspaceEventQuery query) {
            return emptyEvents(query.page(), query.size());
        }

        private PageResult<WorkspaceEvent> emptyEvents(int page, int size) {
            return PageResult.<WorkspaceEvent>builder().items(List.of()).page(page).size(size).total(0).build();
        }
    }

    /**
     * Minimal async fixture for latest-job projection.
     */
    private static class TestWorkflowAsyncService implements WorkflowAsyncService {

        @Override
        public WorkflowJobSubmitResponse submitWorkflowStart(String taskId, String requestedBy) {
            return null;
        }

        @Override
        public WorkflowJobSubmitResponse submitStageRun(String taskId, WorkflowStage stage, String requestedBy) {
            return null;
        }

        @Override
        public WorkflowJobSubmitResponse submitStageRerun(String taskId, WorkflowStage stage, String requestedBy) {
            return null;
        }

        @Override
        public WorkflowJobSubmitResponse submitContinueFrom(String taskId, WorkflowStage stage, String requestedBy) {
            return null;
        }

        @Override
        public WorkflowJobSubmitResponse submitRepairAnalyze(String taskId, String requestedBy) {
            return null;
        }

        @Override
        public WorkflowJobSubmitResponse submitRepairApply(String taskId, String actionId, String requestedBy) {
            return null;
        }

        @Override
        public WorkflowJob findByJobId(String jobId) {
            return null;
        }

        @Override
        public List<WorkflowJob> listByTaskId(String taskId) {
            return List.of(WorkflowJob.builder()
                    .jobId("job-view-test")
                    .taskId(taskId)
                    .jobType(WorkflowJobType.FULL_WORKFLOW)
                    .jobStatus(WorkflowJobStatus.SUCCESS)
                    .requestedStage(WorkflowStage.INTENT)
                    .updateTime(LocalDateTime.now())
                    .build());
        }
    }
}
