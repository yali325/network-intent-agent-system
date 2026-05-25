package com.yali.mactav.model.enums;

/**
 * Top-level task lifecycle state for NetworkTask and NetworkWorkspace.
 *
 * <p>Only orchestrator/model-core code should advance this state in normal
 * workflow execution.</p>
 */
public enum TaskStatus {
    CREATED,
    RUNNING,
    WAITING_USER,
    COMPLETED,
    ERROR,
    CANCELLED,
    ARCHIVED
}
