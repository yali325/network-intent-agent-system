package com.yali.mactav.orchestrator.service;

import com.yali.mactav.model.workspace.NetworkWorkspace;

/**
 * Public Orchestrator workflow boundary consumed by Web/API layers.
 *
 * <p>Implementations coordinate workflow state and remote professional agents,
 * but must not expose concrete agent beans, construct prompts, or call model
 * APIs directly.</p>
 */
public interface WorkflowOrchestrator {

    NetworkWorkspace createTask(String rawText, String targetEnvironmentHint, String createdBy);

    NetworkWorkspace runIntentStage(String taskId);

    NetworkWorkspace runPlanningStage(String taskId);

    NetworkWorkspace runConfigurationStage(String taskId);

    NetworkWorkspace runExecutionStage(String taskId);

    NetworkWorkspace runVerificationStage(String taskId);

    NetworkWorkspace runHealingStage(String taskId);

    NetworkWorkspace approveRepairAction(String taskId, String actionId, String approvedBy, String comment);

    NetworkWorkspace rejectRepairAction(String taskId, String actionId, String rejectedBy, String comment);

    NetworkWorkspace applyRepairAction(String taskId, String actionId);

    NetworkWorkspace getWorkspace(String taskId);
}
