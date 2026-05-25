package com.yali.mactav.model.enums;

/**
 * Deterministic MAC-TAV workflow stages from intent to closed-loop completion.
 *
 * <p>The enum is shared stage identity, not a state machine implementation.</p>
 */
public enum WorkflowStage {
    INTENT,
    PLANNING,
    CONFIGURATION,
    EXECUTION,
    VERIFICATION,
    HEALING,
    FINISHED
}
