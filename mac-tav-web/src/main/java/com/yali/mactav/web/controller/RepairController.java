package com.yali.mactav.web.controller;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.common.result.ApiResponse;
import com.yali.mactav.model.healing.RepairPlan;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.orchestrator.service.WorkflowOrchestrator;
import com.yali.mactav.web.dto.RepairActionDecisionRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web API controller for repair analysis, approval, rejection, and application.
 *
 * <p>The controller delegates all workflow decisions to Orchestrator and never
 * calls concrete agents, model APIs, execution adapters, or shell commands.</p>
 */
@RestController
@RequestMapping("/api/v1/repairs")
public class RepairController {

    private final WorkflowOrchestrator workflowOrchestrator;

    public RepairController(WorkflowOrchestrator workflowOrchestrator) {
        this.workflowOrchestrator = workflowOrchestrator;
    }

    @PostMapping("/{taskId}/analyze")
    public ApiResponse<RepairPlan> analyzeRepair(@PathVariable String taskId) {
        NetworkWorkspace workspace = workflowOrchestrator.runHealingStage(taskId);
        return ApiResponse.success(requireRepairPlan(workspace, taskId));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<RepairPlan> getRepairPlan(@PathVariable String taskId) {
        NetworkWorkspace workspace = workflowOrchestrator.getWorkspace(taskId);
        return ApiResponse.success(requireRepairPlan(workspace, taskId));
    }

    @PostMapping("/{taskId}/actions/{actionId}/approve")
    public ApiResponse<RepairPlan> approveRepairAction(
            @PathVariable String taskId,
            @PathVariable String actionId,
            @RequestBody(required = false) RepairActionDecisionRequest request) {
        NetworkWorkspace workspace = workflowOrchestrator.approveRepairAction(
                taskId,
                actionId,
                actor(request),
                comment(request));
        return ApiResponse.success(requireRepairPlan(workspace, taskId));
    }

    @PostMapping("/{taskId}/actions/{actionId}/reject")
    public ApiResponse<RepairPlan> rejectRepairAction(
            @PathVariable String taskId,
            @PathVariable String actionId,
            @RequestBody(required = false) RepairActionDecisionRequest request) {
        NetworkWorkspace workspace = workflowOrchestrator.rejectRepairAction(
                taskId,
                actionId,
                actor(request),
                comment(request));
        return ApiResponse.success(requireRepairPlan(workspace, taskId));
    }

    @PostMapping("/{taskId}/actions/{actionId}/apply")
    public ApiResponse<NetworkWorkspace> applyRepairAction(@PathVariable String taskId, @PathVariable String actionId) {
        return ApiResponse.success(workflowOrchestrator.applyRepairAction(taskId, actionId));
    }

    private RepairPlan requireRepairPlan(NetworkWorkspace workspace, String taskId) {
        if (workspace == null || workspace.getCurrentRepairPlan() == null) {
            throw new BusinessException(
                    ErrorCode.REPAIR_PLAN_NOT_FOUND,
                    "Current repair plan not found for task: " + taskId);
        }
        return workspace.getCurrentRepairPlan();
    }

    private String actor(RepairActionDecisionRequest request) {
        return request == null ? "api" : request.getActor();
    }

    private String comment(RepairActionDecisionRequest request) {
        return request == null ? null : request.getComment();
    }
}
