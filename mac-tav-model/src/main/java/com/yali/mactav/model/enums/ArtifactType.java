package com.yali.mactav.model.enums;

/**
 * Stable stage artifact categories persisted in NetworkWorkspace history.
 *
 * <p>These values define cross-module artifact identity and should not encode
 * concrete agent implementation names.</p>
 */
public enum ArtifactType {
    NETWORK_INTENT,
    NETWORK_PLAN,
    CONFIG_SET,
    EXECUTION_REPORT,
    VALIDATION_REPORT,
    REPAIR_PLAN
}
