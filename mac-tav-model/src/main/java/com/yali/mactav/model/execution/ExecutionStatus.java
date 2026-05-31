package com.yali.mactav.model.execution;

/**
 * Overall execution-stage result state.
 */
public enum ExecutionStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    PARTIAL,
    FAILED,
    SKIPPED
}
