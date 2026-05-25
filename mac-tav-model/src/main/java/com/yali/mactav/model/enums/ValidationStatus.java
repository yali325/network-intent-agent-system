package com.yali.mactav.model.enums;

/**
 * Overall verification conclusion carried by ValidationReport.
 *
 * <p>This value reports verification outcome; it does not drive repair actions
 * without orchestrator decisions.</p>
 */
public enum ValidationStatus {
    PASSED,
    FAILED,
    PARTIAL,
    UNKNOWN
}
