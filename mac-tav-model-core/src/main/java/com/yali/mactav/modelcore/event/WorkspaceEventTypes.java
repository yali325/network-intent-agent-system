package com.yali.mactav.modelcore.event;

/**
 * Standard event type names for workspace event history and future SSE delivery.
 */
public final class WorkspaceEventTypes {

    public static final String TASK_CREATED = "task.created";
    public static final String WORKFLOW_STARTED = "workflow.started";
    public static final String WORKFLOW_COMPLETED = "workflow.completed";
    public static final String WORKFLOW_FAILED = "workflow.failed";
    public static final String WORKFLOW_INTERRUPTED = "workflow.interrupted";
    public static final String STAGE_STARTED = "stage.started";
    public static final String ARTIFACT_GENERATED = "artifact.generated";
    public static final String STAGE_COMPLETED = "stage.completed";
    public static final String STAGE_FAILED = "stage.failed";
    public static final String EXECUTION_COMPLETED = "execution.completed";
    public static final String VALIDATION_COMPLETED = "validation.completed";
    public static final String REPAIR_PROPOSED = "repair.proposed";
    public static final String REPAIR_APPROVED = "repair.approved";
    public static final String REPAIR_REJECTED = "repair.rejected";
    public static final String REPAIR_APPLIED = "repair.applied";
    public static final String ARTIFACT_VERSION_SWITCHED = "artifact.version_switched";

    private WorkspaceEventTypes() {
    }
}
