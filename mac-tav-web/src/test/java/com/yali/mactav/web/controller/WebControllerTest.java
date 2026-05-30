package com.yali.mactav.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.common.result.ApiResponse;
import com.yali.mactav.model.enums.TaskStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.task.NetworkTask;
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
        WorkflowOrchestrator orchestrator = orchestrator();
        WorkflowController workflowController = new WorkflowController(orchestrator);
        WorkspaceController workspaceController = new WorkspaceController(orchestrator);

        ApiResponse<NetworkWorkspace> runResponse = workflowController.runIntentStage("task-web-test");
        ApiResponse<NetworkWorkspace> queryResponse = workspaceController.getWorkspace("task-web-test");

        assertTrue(runResponse.isSuccess());
        assertTrue(queryResponse.isSuccess());
        assertEquals("task-web-test", runResponse.getData().getTask().getTaskId());
        assertEquals("task-web-test", queryResponse.getData().getTask().getTaskId());
    }

    private WorkflowOrchestrator orchestrator() {
        NetworkWorkspace workspace = workspace();
        return new WorkflowOrchestrator() {
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
            public NetworkWorkspace getWorkspace(String taskId) {
                return workspace;
            }
        };
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
                .workspaceStatus(TaskStatus.CREATED)
                .build();
    }
}
