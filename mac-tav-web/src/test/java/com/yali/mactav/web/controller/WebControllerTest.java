package com.yali.mactav.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.common.result.ApiResponse;
import com.yali.mactav.model.enums.TaskStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.execution.ExecutionStatus;
import com.yali.mactav.model.task.NetworkTask;
import com.yali.mactav.model.verification.ValidationItem;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.orchestrator.service.WorkflowOrchestrator;
import com.yali.mactav.web.dto.CreateTaskRequest;
import com.yali.mactav.web.dto.TaskSummaryResponse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

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

    private TestWorkflowOrchestrator orchestrator() {
        NetworkWorkspace workspace = workspace();
        return new TestWorkflowOrchestrator(workspace);
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
        private int getWorkspaceCalls;
        private String lastExecutionTaskId;
        private String lastVerificationTaskId;

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
        public NetworkWorkspace getWorkspace(String taskId) {
            getWorkspaceCalls++;
            return workspace;
        }
    }
}
