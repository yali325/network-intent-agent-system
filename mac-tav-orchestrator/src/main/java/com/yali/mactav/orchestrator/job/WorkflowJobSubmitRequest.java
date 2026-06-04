package com.yali.mactav.orchestrator.job;

import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.workflow.job.WorkflowJobType;

/**
 * Internal request for submitting an asynchronous workflow job.
 */
public record WorkflowJobSubmitRequest(
        String taskId,
        WorkflowJobType jobType,
        WorkflowStage requestedStage,
        String requestedBy,
        String actionId,
        String requestPayloadJson) {
}
