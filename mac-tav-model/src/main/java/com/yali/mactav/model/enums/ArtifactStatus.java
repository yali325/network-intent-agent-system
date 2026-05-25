package com.yali.mactav.model.enums;

/**
 * Lifecycle state for a versioned NetworkArtifact.
 *
 * <p>This enum is shared DTO state only; repository persistence and version
 * transitions belong to model-core/orchestrator code.</p>
 */
public enum ArtifactStatus {
    DRAFT,
    GENERATED,
    VALIDATED,
    APPLIED,
    SUPERSEDED,
    ROLLED_BACK
}
