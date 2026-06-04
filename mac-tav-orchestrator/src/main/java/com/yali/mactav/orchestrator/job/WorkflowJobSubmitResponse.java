package com.yali.mactav.orchestrator.job;

import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.workflow.job.WorkflowJobStatus;
import com.yali.mactav.model.workflow.job.WorkflowJobType;

/**
 * Public API response for accepted asynchronous workflow job submissions.
 */
public class WorkflowJobSubmitResponse {

    private final String taskId;
    private final String jobId;
    private final WorkflowJobStatus jobStatus;
    private final WorkflowStage requestedStage;
    private final WorkflowJobType jobType;
    private final String message;

    public WorkflowJobSubmitResponse(String taskId,
                                     String jobId,
                                     WorkflowJobStatus jobStatus,
                                     WorkflowStage requestedStage,
                                     WorkflowJobType jobType,
                                     String message) {
        this.taskId = taskId;
        this.jobId = jobId;
        this.jobStatus = jobStatus;
        this.requestedStage = requestedStage;
        this.jobType = jobType;
        this.message = message;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getJobId() {
        return jobId;
    }

    public WorkflowJobStatus getJobStatus() {
        return jobStatus;
    }

    public WorkflowStage getRequestedStage() {
        return requestedStage;
    }

    public WorkflowJobType getJobType() {
        return jobType;
    }

    public String getMessage() {
        return message;
    }
}
