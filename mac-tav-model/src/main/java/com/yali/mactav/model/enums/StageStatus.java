package com.yali.mactav.model.enums;

/**
 * Execution state for one workflow stage artifact or agent run.
 *
 * <p>It is separate from task lifecycle state and validation result state.</p>
 */
public enum StageStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    SKIPPED
}
