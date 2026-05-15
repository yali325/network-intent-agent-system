package com.yali.mactav.web.controller;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.common.result.ApiResponse;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.model.workspace.AgentStepLog;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.modelcore.service.NetworkWorkspaceService;
import com.yali.mactav.orchestrator.service.TaskOrchestratorService;
import com.yali.mactav.web.dto.CreateDemoTaskRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskOrchestratorService taskOrchestratorService;
    private final NetworkWorkspaceService networkWorkspaceService;

    public TaskController(TaskOrchestratorService taskOrchestratorService,
                          NetworkWorkspaceService networkWorkspaceService) {
        this.taskOrchestratorService = taskOrchestratorService;
        this.networkWorkspaceService = networkWorkspaceService;
    }

    @PostMapping("/demo/tasks")
    public ApiResponse<NetworkWorkspace> runDemoTask(@Valid @RequestBody CreateDemoTaskRequest request) {
        if (request == null || isBlank(request.getRawText())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "rawText must not be blank");
        }
        NetworkWorkspace workspace = taskOrchestratorService.runDemoTask(request.getRawText().trim());
        return ApiResponse.ok(workspace);
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<NetworkWorkspace> getWorkspace(@PathVariable String taskId) {
        return ApiResponse.ok(workspace(taskId));
    }

    @GetMapping("/tasks/{taskId}/intent")
    public ApiResponse<NetworkIntent> getIntent(@PathVariable String taskId) {
        NetworkIntent intent = workspace(taskId).getIntent();
        return ApiResponse.ok(requireStageReady(intent, "Intent artifact is not ready"));
    }

    @GetMapping("/tasks/{taskId}/plan")
    public ApiResponse<NetworkPlan> getPlan(@PathVariable String taskId) {
        NetworkPlan plan = workspace(taskId).getPlan();
        return ApiResponse.ok(requireStageReady(plan, "Plan artifact is not ready"));
    }

    @GetMapping("/tasks/{taskId}/config")
    public ApiResponse<ConfigSet> getConfig(@PathVariable String taskId) {
        ConfigSet configSet = workspace(taskId).getConfigSet();
        return ApiResponse.ok(requireStageReady(configSet, "Config artifact is not ready"));
    }

    @GetMapping("/tasks/{taskId}/execution")
    public ApiResponse<ExecutionReport> getExecution(@PathVariable String taskId) {
        ExecutionReport executionReport = workspace(taskId).getExecutionReport();
        return ApiResponse.ok(requireStageReady(executionReport, "Execution artifact is not ready"));
    }

    @GetMapping("/tasks/{taskId}/validation")
    public ApiResponse<ValidationReport> getValidation(@PathVariable String taskId) {
        ValidationReport validationReport = workspace(taskId).getValidationReport();
        return ApiResponse.ok(requireStageReady(validationReport, "Validation artifact is not ready"));
    }

    @GetMapping("/tasks/{taskId}/logs")
    public ApiResponse<List<AgentStepLog>> getLogs(@PathVariable String taskId) {
        List<AgentStepLog> logs = workspace(taskId).getAgentLogs();
        return ApiResponse.ok(logs == null ? List.of() : logs);
    }

    private NetworkWorkspace workspace(String taskId) {
        if (isBlank(taskId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "taskId must not be blank");
        }
        return networkWorkspaceService.getWorkspace(taskId);
    }

    private <T> T requireStageReady(T artifact, String message) {
        if (artifact == null) {
            throw new BusinessException(ErrorCode.STAGE_NOT_READY, message);
        }
        return artifact;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
