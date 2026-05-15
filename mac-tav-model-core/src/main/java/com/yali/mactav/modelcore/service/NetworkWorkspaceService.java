package com.yali.mactav.modelcore.service;

import com.yali.mactav.common.enums.TaskStatus;
import com.yali.mactav.common.enums.WorkflowStage;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.model.workspace.AgentStepLog;
import com.yali.mactav.model.workspace.NetworkWorkspace;

public interface NetworkWorkspaceService {

    NetworkWorkspace createTask(String rawText);

    NetworkWorkspace getWorkspace(String taskId);

    NetworkWorkspace saveIntent(String taskId, NetworkIntent intent);

    NetworkWorkspace savePlan(String taskId, NetworkPlan plan);

    NetworkWorkspace saveConfigSet(String taskId, ConfigSet configSet);

    NetworkWorkspace saveExecutionReport(String taskId, ExecutionReport executionReport);

    NetworkWorkspace saveValidationReport(String taskId, ValidationReport validationReport);

    NetworkWorkspace appendAgentLog(String taskId, AgentStepLog log);

    NetworkWorkspace updateTaskStatus(String taskId, TaskStatus status);

    NetworkWorkspace updateCurrentStage(String taskId, WorkflowStage stage);
}
