package com.yali.mactav.web.controller;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.common.result.ApiResponse;
import com.yali.mactav.model.verification.ValidationItem;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.orchestrator.service.WorkflowOrchestrator;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web API controller for triggering and querying the verification stage.
 */
@RestController
@RequestMapping("/api/v1/validations")
public class ValidationController {

    private final WorkflowOrchestrator workflowOrchestrator;

    public ValidationController(WorkflowOrchestrator workflowOrchestrator) {
        this.workflowOrchestrator = workflowOrchestrator;
    }

    @PostMapping("/{taskId}/run")
    public ApiResponse<ValidationReport> runVerificationStage(@PathVariable String taskId) {
        NetworkWorkspace workspace = workflowOrchestrator.runVerificationStage(taskId);
        return ApiResponse.success(requireValidationReport(workspace, taskId));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<ValidationReport> getValidationReport(@PathVariable String taskId) {
        NetworkWorkspace workspace = workflowOrchestrator.getWorkspace(taskId);
        return ApiResponse.success(requireValidationReport(workspace, taskId));
    }

    @GetMapping("/{taskId}/items")
    public ApiResponse<List<ValidationItem>> getValidationItems(@PathVariable String taskId) {
        ValidationReport report = requireValidationReport(workflowOrchestrator.getWorkspace(taskId), taskId);
        return ApiResponse.success(report.getItems() == null ? List.of() : report.getItems());
    }

    private ValidationReport requireValidationReport(NetworkWorkspace workspace, String taskId) {
        if (workspace == null || workspace.getCurrentValidationReport() == null) {
            throw new BusinessException(
                    ErrorCode.ARTIFACT_NOT_FOUND,
                    "Current validation report not found for task: " + taskId);
        }
        return workspace.getCurrentValidationReport();
    }
}
