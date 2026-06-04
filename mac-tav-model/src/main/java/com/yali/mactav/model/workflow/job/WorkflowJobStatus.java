package com.yali.mactav.model.workflow.job;

/**
 * Lifecycle status of a workflow job persisted in workflow_job.
 */
public enum WorkflowJobStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED,
    INTERRUPTED
}
