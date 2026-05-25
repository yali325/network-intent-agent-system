package com.yali.mactav.model.enums;

/**
 * Status of a proposed repair action inside a RepairPlan.
 *
 * <p>Approval and application workflows are orchestrator/model-core concerns,
 * not responsibilities of the DTO enum itself.</p>
 */
public enum RepairStatus {
    PROPOSED,
    APPROVED,
    APPLIED,
    REJECTED,
    FAILED
}
