package com.yali.mactav.healing.tool;

import com.yali.mactav.healing.tool.HealingDiagnosisTool.AffectedScope;
import com.yali.mactav.healing.tool.HealingDiagnosisTool.FailureClassification;
import com.yali.mactav.healing.tool.HealingDiagnosisTool.RepairActionSuggestion;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies structured outputs from the HealingDiagnosisTool.
 */
class HealingDiagnosisToolTest {

    private final HealingDiagnosisTool tool = new HealingDiagnosisTool();

    @Test
    void classifiesPolicyFailureAndSuggestsPatchConfig() {
        FailureClassification classification = tool.classifyValidationFailure(
                "isolation",
                "BLOCKED",
                "REACHABLE",
                "HIGH",
                "ACL did not block guest traffic");

        RepairActionSuggestion suggestion = tool.suggestRepairAction(
                classification.failureType(),
                List.of("policy-001"),
                List.of("R1-ACL-001"),
                classification.riskLevel(),
                false);

        assertEquals("POLICY_ENFORCEMENT_FAILURE", classification.failureType());
        assertEquals("PATCH_CONFIG", suggestion.actionType());
        assertTrue(suggestion.requiresApproval());
    }

    @Test
    void extractsAffectedScope() {
        AffectedScope scope = tool.extractAffectedScope(
                "val-1",
                "rel-002",
                List.of("policy-001"),
                List.of("R1-ACL-001"),
                "test-1",
                List.of("ev-1"));

        assertEquals(List.of("val-1"), scope.validationItemIds());
        assertEquals(List.of("rel-002"), scope.intentRelationIds());
        assertEquals(List.of("policy-001"), scope.planElementIds());
        assertEquals(List.of("R1-ACL-001"), scope.configBlockIds());
        assertEquals(List.of("test-1"), scope.testIds());
        assertEquals(List.of("ev-1"), scope.evidenceIds());
    }
}
