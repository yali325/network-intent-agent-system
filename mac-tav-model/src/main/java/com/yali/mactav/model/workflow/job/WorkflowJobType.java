package com.yali.mactav.model.workflow.job;

/**
 * Type of asynchronous workflow job submitted through the public API.
 */
public enum WorkflowJobType {
    FULL_WORKFLOW,
    RUN_STAGE,
    RERUN_STAGE,
    CONTINUE_FROM_STAGE,
    REPAIR_ANALYZE,
    REPAIR_APPLY
}
