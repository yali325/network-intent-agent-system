package com.yali.mactav.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yali.mactav.common.result.PageResult;
import com.yali.mactav.model.enums.ArtifactStatus;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.common.result.ApiResponse;
import com.yali.mactav.model.enums.RepairStatus;
import com.yali.mactav.model.enums.TaskStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.execution.ExecutionStatus;
import com.yali.mactav.model.healing.RepairAction;
import com.yali.mactav.model.healing.RepairPlan;
import com.yali.mactav.model.task.NetworkTask;
import com.yali.mactav.model.verification.ValidationItem;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.model.workspace.WorkspaceChangeRecord;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import com.yali.mactav.modelcore.query.ArtifactQuery;
import com.yali.mactav.modelcore.query.WorkspaceChangeQuery;
import com.yali.mactav.modelcore.query.WorkspaceEventQuery;
import com.yali.mactav.modelcore.event.WorkspaceEventSummary;
import com.yali.mactav.orchestrator.service.ArtifactDiffResult;
import com.yali.mactav.orchestrator.service.WorkflowOrchestrator;
import com.yali.mactav.orchestrator.service.WorkflowQueryService;
import com.yali.mactav.web.dto.CreateTaskRequest;
import com.yali.mactav.web.dto.RepairActionDecisionRequest;
import com.yali.mactav.web.dto.TaskSummaryResponse;
import com.yali.mactav.web.vo.ArtifactDiffResponse;
import com.yali.mactav.web.vo.ArtifactPayloadResponse;
import com.yali.mactav.web.vo.ArtifactSummaryResponse;
import com.yali.mactav.web.sse.RedisWorkspaceEventSubscriber;
import com.yali.mactav.web.sse.SseEmitterRegistry;
import com.yali.mactav.web.sse.SseEventMapper;
import com.yali.mactav.web.sse.SseProperties;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Offline controller tests for the minimal Web API layer.
 *
 * <p>The tests use a small Orchestrator boundary fixture and do not create
 * concrete agent beans, call Nacos, or invoke model providers.</p>
 */
class WebControllerTest {

    @Test
    void taskControllerShouldReturnApiResponseEnvelope() {
        TaskController controller = new TaskController(orchestrator());
        CreateTaskRequest request = new CreateTaskRequest();
        request.setRawText("office can access server");
        request.setTargetEnvironmentHint("lab");
        request.setCreatedBy("unit-test");

        ApiResponse<TaskSummaryResponse> response = controller.createTask(request);

        assertTrue(response.isSuccess());
        assertEquals("task-web-test", response.getData().getTaskId());
        assertEquals(WorkflowStage.INTENT, response.getData().getCurrentStage());
    }

    @Test
    void workflowAndWorkspaceControllersShouldDelegateToOrchestrator() {
        TestWorkflowOrchestrator orchestrator = orchestrator();
        WorkflowController workflowController = new WorkflowController(orchestrator);
        WorkspaceController workspaceController = new WorkspaceController(orchestrator);

        ApiResponse<NetworkWorkspace> runResponse = workflowController.runIntentStage("task-web-test");
        ApiResponse<NetworkWorkspace> configResponse = workflowController.runConfigurationStage("task-web-test");
        ApiResponse<NetworkWorkspace> queryResponse = workspaceController.getWorkspace("task-web-test");

        assertTrue(runResponse.isSuccess());
        assertTrue(configResponse.isSuccess());
        assertTrue(queryResponse.isSuccess());
        assertEquals("task-web-test", runResponse.getData().getTask().getTaskId());
        assertEquals("task-web-test", configResponse.getData().getTask().getTaskId());
        assertEquals("task-web-test", queryResponse.getData().getTask().getTaskId());
    }

    @Test
    void executionControllerShouldDelegateRunToOrchestrator() {
        TestWorkflowOrchestrator orchestrator = orchestrator();
        ExecutionController controller = new ExecutionController(orchestrator);

        ApiResponse<ExecutionReport> response = controller.runExecutionStage("task-web-test");

        assertTrue(response.isSuccess());
        assertEquals("execution-web-test", response.getData().getExecutionId());
        assertEquals(ExecutionStatus.SUCCESS, response.getData().getOverallStatus());
        assertEquals(1, orchestrator.runExecutionCalls);
        assertEquals("task-web-test", orchestrator.lastExecutionTaskId);
    }

    @Test
    void executionControllerShouldReturnCurrentExecutionReport() {
        TestWorkflowOrchestrator orchestrator = orchestrator();
        ExecutionController controller = new ExecutionController(orchestrator);

        ApiResponse<ExecutionReport> response = controller.getExecutionReport("task-web-test");

        assertTrue(response.isSuccess());
        assertEquals("execution-web-test", response.getData().getExecutionId());
        assertEquals("task-web-test", response.getData().getTaskId());
        assertEquals(1, orchestrator.getWorkspaceCalls);
    }

    @Test
    void validationControllerShouldDelegateRunToOrchestrator() {
        TestWorkflowOrchestrator orchestrator = orchestrator();
        ValidationController controller = new ValidationController(orchestrator);

        ApiResponse<ValidationReport> response = controller.runVerificationStage("task-web-test");

        assertTrue(response.isSuccess());
        assertEquals("validation-web-test", response.getData().getValidationId());
        assertEquals(1, orchestrator.runVerificationCalls);
        assertEquals("task-web-test", orchestrator.lastVerificationTaskId);
    }

    @Test
    void validationControllerShouldReturnCurrentItems() {
        TestWorkflowOrchestrator orchestrator = orchestrator();
        ValidationController controller = new ValidationController(orchestrator);

        ApiResponse<java.util.List<ValidationItem>> response = controller.getValidationItems("task-web-test");

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals("val-item-web-test", response.getData().get(0).getItemId());
    }

    @Test
    void repairControllerShouldDelegateAnalyzeAndGetToOrchestrator() {
        TestWorkflowOrchestrator orchestrator = orchestrator();
        RepairController controller = new RepairController(orchestrator);

        ApiResponse<RepairPlan> analyzeResponse = controller.analyzeRepair("task-web-test");
        ApiResponse<RepairPlan> getResponse = controller.getRepairPlan("task-web-test");

        assertTrue(analyzeResponse.isSuccess());
        assertTrue(getResponse.isSuccess());
        assertEquals("repair-action-web-test", analyzeResponse.getData().getActions().get(0).getActionId());
        assertEquals(1, orchestrator.runHealingCalls);
        assertEquals(1, orchestrator.getWorkspaceCalls);
        assertEquals("task-web-test", orchestrator.lastHealingTaskId);
    }

    @Test
    void repairControllerShouldDelegateApproveRejectAndApplyToOrchestrator() {
        TestWorkflowOrchestrator orchestrator = orchestrator();
        RepairController controller = new RepairController(orchestrator);
        RepairActionDecisionRequest request = new RepairActionDecisionRequest();
        request.setActor("alice");
        request.setComment("approved in web test");

        ApiResponse<RepairPlan> approveResponse = controller.approveRepairAction(
                "task-web-test", "repair-action-web-test", request);
        ApiResponse<RepairPlan> rejectResponse = controller.rejectRepairAction(
                "task-web-test", "repair-action-web-test", request);
        ApiResponse<NetworkWorkspace> applyResponse = controller.applyRepairAction(
                "task-web-test", "repair-action-web-test");

        assertTrue(approveResponse.isSuccess());
        assertTrue(rejectResponse.isSuccess());
        assertTrue(applyResponse.isSuccess());
        assertEquals(1, orchestrator.approveCalls);
        assertEquals(1, orchestrator.rejectCalls);
        assertEquals(1, orchestrator.applyCalls);
        assertEquals("repair-action-web-test", orchestrator.lastRepairActionId);
        assertEquals("alice", orchestrator.lastActor);
        assertEquals("approved in web test", orchestrator.lastComment);
    }

    @Test
    void artifactControllerShouldKeepPayloadOutOfSummaryResponses() {
        ArtifactController controller = new ArtifactController(queryService());

        ApiResponse<PageResult<ArtifactSummaryResponse>> listResponse = controller.listArtifacts(
                "task-web-test", "NETWORK_INTENT", null, 1, 20);
        ApiResponse<ArtifactSummaryResponse> detailResponse = controller.getArtifact(
                "task-web-test", "artifact-web-test-v2");
        ApiResponse<ArtifactPayloadResponse> payloadResponse = controller.getArtifactPayload(
                "task-web-test", "artifact-web-test-v2");

        assertTrue(listResponse.isSuccess());
        assertEquals(1, listResponse.getData().getItems().size());
        assertEquals("payload summary v2", listResponse.getData().getItems().get(0).getPayloadSummary());
        assertEquals("artifact-web-test-v2", detailResponse.getData().getArtifactId());
        assertEquals("{\"version\":2}", payloadResponse.getData().getPayloadJson());
    }

    @Test
    void artifactControllerShouldReturnCurrentVersionsAndDiff() {
        ArtifactController controller = new ArtifactController(queryService());

        ApiResponse<ArtifactSummaryResponse> currentResponse = controller.getCurrentArtifact(
                "task-web-test", "NETWORK_INTENT");
        ApiResponse<PageResult<ArtifactSummaryResponse>> versionsResponse = controller.listArtifactVersions(
                "task-web-test", "artifact-web-test-v2", 1, 20);
        ApiResponse<ArtifactDiffResponse> diffResponse = controller.diffArtifactVersions(
                "task-web-test", "artifact-web-test-v2", 1, 2);

        assertTrue(currentResponse.isSuccess());
        assertEquals(2, currentResponse.getData().getVersion());
        assertEquals(2, versionsResponse.getData().getItems().size());
        assertEquals("{\"version\":1}", diffResponse.getData().getFrom().getPayloadJson());
        assertEquals("{\"version\":2}", diffResponse.getData().getTo().getPayloadJson());
    }

    @Test
    void workspaceAndEventHistoryControllersShouldUseQueryFacade() {
        TestWorkflowQueryService queryService = queryService();
        WorkspaceController workspaceController = new WorkspaceController(orchestrator(), queryService);
        EventController eventController = new EventController(queryService);

        ApiResponse<PageResult<WorkspaceEvent>> timelineResponse = workspaceController.getTimeline(
                "task-web-test", WorkflowStage.INTENT, "task.created", null, null, 1, 20);
        ApiResponse<PageResult<WorkspaceChangeRecord>> changesResponse = workspaceController.getChanges(
                "task-web-test", WorkflowStage.INTENT, "artifact.generated", null, null, 1, 20);
        ApiResponse<PageResult<WorkspaceEvent>> historyResponse = eventController.getEventHistory(
                "task-web-test", null, null, null, null, 1, 20);

        assertTrue(timelineResponse.isSuccess());
        assertTrue(changesResponse.isSuccess());
        assertTrue(historyResponse.isSuccess());
        assertEquals("task.created", timelineResponse.getData().getItems().get(0).getEventType());
        assertEquals("artifact.generated", changesResponse.getData().getItems().get(0).getChangeType());
        assertEquals(2, queryService.eventHistoryCalls);
    }

    @Test
    void sseControllerShouldValidateWorkspaceAndRegisterEmitter() {
        TestWorkflowQueryService queryService = queryService();
        SseEventMapper mapper = new SseEventMapper(objectMapper());
        SseEmitterRegistry registry = new SseEmitterRegistry(mapper);
        SseProperties properties = new SseProperties();
        properties.setTimeoutMs(1000);
        SseController controller = new SseController(queryService, registry, properties);

        SseEmitter emitter = controller.connect("task-web-test");

        assertNotNull(emitter);
        assertEquals(1, queryService.requireWorkspaceCalls);
        assertEquals(1, registry.emitterCount("task-web-test"));
    }

    @Test
    void sseEventMapperShouldOnlySerializeSummaryFields() {
        SseEventMapper mapper = new SseEventMapper(objectMapper());

        String json = mapper.toJson(WorkspaceEventSummary.from(queryService().event()));

        assertTrue(json.contains("\"eventId\""));
        assertTrue(json.contains("\"payloadSummary\""));
        assertTrue(!json.contains("payloadJson"));
        assertTrue(!json.contains("apiKey"));
        assertEquals("task-web-test", mapper.fromJson(json).taskId());
    }

    @Test
    void redisWorkspaceEventSubscriberShouldDispatchByTaskId() {
        SseEventMapper mapper = new SseEventMapper(objectMapper());
        SseEmitterRegistry registry = new SseEmitterRegistry(mapper);
        registry.register("task-web-test", 1000);
        RedisWorkspaceEventSubscriber subscriber = new RedisWorkspaceEventSubscriber(registry, mapper);
        String json = mapper.toJson(WorkspaceEventSummary.from(queryService().event()));

        subscriber.onMessage(
                new DefaultMessage(
                        "mactav:events:task-web-test".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        json.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                "mactav:events:*".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertEquals(1, registry.emitterCount("task-web-test"));
    }

    private TestWorkflowOrchestrator orchestrator() {
        NetworkWorkspace workspace = workspace();
        return new TestWorkflowOrchestrator(workspace);
    }

    private TestWorkflowQueryService queryService() {
        return new TestWorkflowQueryService();
    }

    private ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    private NetworkWorkspace workspace() {
        return NetworkWorkspace.builder()
                .task(NetworkTask.builder()
                        .taskId("task-web-test")
                        .rawText("office can access server")
                        .taskStatus(TaskStatus.CREATED)
                        .currentStage(WorkflowStage.INTENT)
                        .createTime(LocalDateTime.now())
                        .build())
                .currentExecutionReport(ExecutionReport.builder()
                        .executionId("execution-web-test")
                        .taskId("task-web-test")
                        .executionVersion(1)
                        .overallStatus(ExecutionStatus.SUCCESS)
                        .createTime(LocalDateTime.now())
                        .startTime(LocalDateTime.now())
                        .endTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .build())
                .currentValidationReport(ValidationReport.builder()
                        .validationId("validation-web-test")
                        .taskId("task-web-test")
                        .validationVersion(1)
                        .overallStatus(com.yali.mactav.model.enums.ValidationStatus.PASSED)
                        .items(java.util.List.of(ValidationItem.builder()
                                .itemId("val-item-web-test")
                                .expected("REACHABLE")
                                .actual("REACHABLE")
                                .passed(true)
                                .relatedTestId("test-web-test")
                                .build()))
                        .build())
                .currentRepairPlan(RepairPlan.builder()
                        .taskId("task-web-test")
                        .repairVersion(1)
                        .validationVersion(1)
                        .overallRepairStrategy("Web test repair plan")
                        .actions(java.util.List.of(RepairAction.builder()
                                .actionId("repair-action-web-test")
                                .actionType("PATCH_CONFIG")
                                .targetStage(WorkflowStage.CONFIGURATION)
                                .description("Patch config in web test")
                                .riskLevel("LOW")
                                .requiresApproval(false)
                                .status(RepairStatus.PROPOSED)
                                .build()))
                        .build())
                .workspaceStatus(TaskStatus.CREATED)
                .build();
    }

    /**
     * Minimal Orchestrator fixture for Web controller boundary tests.
     */
    private static class TestWorkflowOrchestrator implements WorkflowOrchestrator {

        private final NetworkWorkspace workspace;
        private int runExecutionCalls;
        private int runVerificationCalls;
        private int runHealingCalls;
        private int approveCalls;
        private int rejectCalls;
        private int applyCalls;
        private int getWorkspaceCalls;
        private String lastExecutionTaskId;
        private String lastVerificationTaskId;
        private String lastHealingTaskId;
        private String lastRepairActionId;
        private String lastActor;
        private String lastComment;

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
            runExecutionCalls++;
            lastExecutionTaskId = taskId;
            return workspace;
        }

        @Override
        public NetworkWorkspace runVerificationStage(String taskId) {
            runVerificationCalls++;
            lastVerificationTaskId = taskId;
            return workspace;
        }

        @Override
        public NetworkWorkspace runHealingStage(String taskId) {
            runHealingCalls++;
            lastHealingTaskId = taskId;
            return workspace;
        }

        @Override
        public NetworkWorkspace approveRepairAction(String taskId, String actionId, String approvedBy, String comment) {
            approveCalls++;
            lastRepairActionId = actionId;
            lastActor = approvedBy;
            lastComment = comment;
            return workspace;
        }

        @Override
        public NetworkWorkspace rejectRepairAction(String taskId, String actionId, String rejectedBy, String comment) {
            rejectCalls++;
            lastRepairActionId = actionId;
            lastActor = rejectedBy;
            lastComment = comment;
            return workspace;
        }

        @Override
        public NetworkWorkspace applyRepairAction(String taskId, String actionId) {
            applyCalls++;
            lastRepairActionId = actionId;
            return workspace;
        }

        @Override
        public NetworkWorkspace getWorkspace(String taskId) {
            getWorkspaceCalls++;
            return workspace;
        }
    }

    /**
     * Minimal query facade fixture for Web read-side controller tests.
     */
    private static class TestWorkflowQueryService implements WorkflowQueryService {

        private int requireWorkspaceCalls;
        private int eventHistoryCalls;

        @Override
        public void requireWorkspace(String taskId) {
            requireWorkspaceCalls++;
        }

        @Override
        public PageResult<NetworkArtifact> listArtifacts(String taskId, ArtifactQuery query) {
            return page(java.util.List.of(artifact(2)), query.page(), query.size());
        }

        @Override
        public NetworkArtifact getArtifact(String taskId, String artifactId) {
            return artifact(2);
        }

        @Override
        public NetworkArtifact getCurrentArtifact(String taskId, ArtifactType artifactType) {
            return artifact(2);
        }

        @Override
        public PageResult<NetworkArtifact> listArtifactVersions(String taskId, String artifactId, int page, int size) {
            return page(java.util.List.of(artifact(1), artifact(2)), page, size);
        }

        @Override
        public ArtifactDiffResult diffArtifactVersions(String taskId,
                                                       String artifactId,
                                                       Integer fromVersion,
                                                       Integer toVersion) {
            return new ArtifactDiffResult(artifact(fromVersion), artifact(toVersion));
        }

        @Override
        public PageResult<WorkspaceEvent> listTimeline(String taskId, WorkspaceEventQuery query) {
            eventHistoryCalls++;
            return page(java.util.List.of(event()), query.page(), query.size());
        }

        @Override
        public PageResult<WorkspaceChangeRecord> listChanges(String taskId, WorkspaceChangeQuery query) {
            return page(java.util.List.of(change()), query.page(), query.size());
        }

        @Override
        public PageResult<WorkspaceEvent> listEventHistory(String taskId, WorkspaceEventQuery query) {
            eventHistoryCalls++;
            return page(java.util.List.of(event()), query.page(), query.size());
        }

        private NetworkArtifact artifact(Integer version) {
            return NetworkArtifact.builder()
                    .artifactId("artifact-web-test-v" + version)
                    .taskId("task-web-test")
                    .artifactType(ArtifactType.NETWORK_INTENT)
                    .version(version)
                    .stage(WorkflowStage.INTENT)
                    .status(ArtifactStatus.GENERATED)
                    .payloadType("NetworkIntent")
                    .payloadJson("{\"version\":" + version + "}")
                    .payloadSummary("payload summary v" + version)
                    .createTime(LocalDateTime.now())
                    .createdBy("unit-test")
                    .build();
        }

        private WorkspaceEvent event() {
            return WorkspaceEvent.builder()
                    .eventId("event-web-test")
                    .taskId("task-web-test")
                    .eventType("task.created")
                    .stage(WorkflowStage.INTENT)
                    .eventTime(LocalDateTime.now())
                    .title("Task created")
                    .message("Task created")
                    .payloadSummary("safe event summary")
                    .build();
        }

        private WorkspaceChangeRecord change() {
            return WorkspaceChangeRecord.builder()
                    .changeId("change-web-test")
                    .taskId("task-web-test")
                    .stage(WorkflowStage.INTENT)
                    .changeType("artifact.generated")
                    .fromArtifactId("artifact-web-test-v1")
                    .toArtifactId("artifact-web-test-v2")
                    .reason("unit test change")
                    .createTime(LocalDateTime.now())
                    .createdBy("unit-test")
                    .build();
        }

        private <T> PageResult<T> page(java.util.List<T> items, int page, int size) {
            return PageResult.<T>builder()
                    .items(items)
                    .page(page)
                    .size(size)
                    .total(items.size())
                    .build();
        }
    }
}
