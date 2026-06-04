package com.yali.mactav.orchestrator.service;

import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.workflow.job.WorkflowJob;
import com.yali.mactav.orchestrator.job.WorkflowJobSubmitResponse;
import java.util.List;

/**
 * Orchestrator async submission facade consumed by public Web APIs.
 */
public interface WorkflowAsyncService {

    WorkflowJobSubmitResponse submitWorkflowStart(String taskId, String requestedBy);

    WorkflowJobSubmitResponse submitStageRun(String taskId, WorkflowStage stage, String requestedBy);

    WorkflowJobSubmitResponse submitStageRerun(String taskId, WorkflowStage stage, String requestedBy);

    WorkflowJobSubmitResponse submitContinueFrom(String taskId, WorkflowStage stage, String requestedBy);

    WorkflowJobSubmitResponse submitRepairAnalyze(String taskId, String requestedBy);

    WorkflowJobSubmitResponse submitRepairApply(String taskId, String actionId, String requestedBy);

    WorkflowJob findByJobId(String jobId);

    List<WorkflowJob> listByTaskId(String taskId);
}
