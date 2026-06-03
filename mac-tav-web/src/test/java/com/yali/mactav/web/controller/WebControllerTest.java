package com.yali.mactav.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.yali.mactav.orchestrator.service.WorkflowOrchestrator;
import com.yali.mactav.web.dto.CreateTaskRequest;
import com.yali.mactav.web.dto.RepairActionDecisionRequest;
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
}
