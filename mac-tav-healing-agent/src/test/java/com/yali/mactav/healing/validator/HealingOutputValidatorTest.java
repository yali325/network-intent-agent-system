package com.yali.mactav.healing.validator;

import com.yali.mactav.model.enums.RepairStatus;
import com.yali.mactav.model.healing.FailureAnalysis;
import com.yali.mactav.model.healing.RepairAction;
import com.yali.mactav.model.healing.RepairPlan;
import com.yali.mactav.model.workspace.TraceRefs;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies HealingOutputValidator legal and illegal repair-plan cases.
 */
class HealingOutputValidatorTest {

    private final HealingOutputValidator validator = new HealingOutputValidator();

    @Test
    void acceptsValidProposedRepairPlan() {
        assertTrue(validator.validate(validPlan()).isValid());
    }

    @Test
    void rejectsDangerousShellAndWorkspaceClaims() {
        RepairPlan plan = validPlan();
        plan.setOverallRepairStrategy("I modified workspace and ran command: bash repair.sh");

        assertFalse(validator.validate(plan).isValid());
    }

    @Test
    void rejectsHighRiskActionWithoutApproval() {
        RepairPlan plan = validPlan();
        plan.getActions().get(0).setRequiresApproval(false);

        assertFalse(validator.validate(plan).isValid());
    }

    @Test
    void rejectsAlreadyAppliedAction() {
        RepairPlan plan = validPlan();
        plan.getActions().get(0).setStatus(RepairStatus.APPLIED);

        assertFalse(validator.validate(plan).isValid());
    }

    private RepairPlan validPlan() {
        TraceRefs refs = TraceRefs.builder()
                .validationItemIds(List.of("val-1"))
                .repairActionIds(List.of("repair-action-1"))
                .build();
        return RepairPlan.builder()
                .taskId("task-1")
                .validationVersion(1)
                .repairVersion(1)
                .overallRepairStrategy("Propose an Orchestrator-controlled policy repair.")
                .failureAnalysis(List.of(FailureAnalysis.builder()
                        .analysisId("analysis-val-1")
                        .failureType("POLICY_ENFORCEMENT_FAILURE")
                        .rootCauseSummary("Policy enforcement diverged from expected validation outcome.")
                        .relatedValidationItemIds(List.of("val-1"))
                        .confidence(0.8)
                        .build()))
                .actions(List.of(RepairAction.builder()
                        .actionId("repair-action-1")
                        .actionType("PATCH_CONFIG")
                        .description("Propose a traceable config patch for Orchestrator approval.")
                        .relatedFailureAnalysisId("analysis-val-1")
                        .riskLevel("HIGH")
                        .riskReason("Policy patch can affect isolation.")
                        .requiresApproval(true)
                        .traceRefs(refs)
                        .status(RepairStatus.PROPOSED)
                        .build()))
                .requiresUserConfirmation(true)
                .build();
    }
}
