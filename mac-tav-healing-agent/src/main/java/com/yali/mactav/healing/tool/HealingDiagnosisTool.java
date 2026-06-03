package com.yali.mactav.healing.tool;

import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.WorkflowStage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Spring AI method tool that classifies failures and suggests repair actions.
 */
public class HealingDiagnosisTool {

    @Tool(name = "classifyValidationFailure", description = "Classify a failed validation item without executing repair commands.")
    public FailureClassification classifyValidationFailure(
            @ToolParam(required = true, description = "Validation item type.") String itemType,
            @ToolParam(required = false, description = "Expected validation result.") String expected,
            @ToolParam(required = false, description = "Actual validation result.") String actual,
            @ToolParam(required = false, description = "Validation severity.") String severity,
            @ToolParam(required = false, description = "Validation failure message.") String message) {
        String joined = normalize(itemType) + " " + normalize(expected) + " "
                + normalize(actual) + " " + normalize(message);
        if (joined.contains("BLOCK") || joined.contains("DENY") || joined.contains("ACL")
                || joined.contains("ISOLATION")) {
            return new FailureClassification(
                    "POLICY_ENFORCEMENT_FAILURE",
                    "Validation evidence indicates that an access-control or isolation policy did not match the expected behavior.",
                    confidence(severity, 0.82),
                    highOrMediumRisk(severity));
        }
        if (joined.contains("REACHABLE") || joined.contains("UNREACHABLE") || joined.contains("ROUTE")
                || joined.contains("OSPF") || joined.contains("PING")) {
            return new FailureClassification(
                    "CONNECTIVITY_FAILURE",
                    "Validation evidence indicates that reachability or routing behavior diverged from the expected network plan.",
                    confidence(severity, 0.78),
                    highOrMediumRisk(severity));
        }
        if (joined.contains("TIMEOUT") || joined.contains("ENV") || joined.contains("ADAPTER")) {
            return new FailureClassification(
                    "EXECUTION_EVIDENCE_FAILURE",
                    "Validation evidence may be stale or affected by execution adapter or environment behavior.",
                    confidence(severity, 0.68),
                    "MEDIUM");
        }
        return new FailureClassification(
                "UNKNOWN_FAILURE",
                "The failed validation item does not provide enough deterministic evidence for a single root cause.",
                confidence(severity, 0.45),
                "MEDIUM");
    }

    @Tool(name = "extractAffectedScope", description = "Extract traceable affected intent, plan, config, test, and evidence scope from a failed validation item.")
    public AffectedScope extractAffectedScope(
            @ToolParam(required = true, description = "Validation item id.") String validationItemId,
            @ToolParam(required = false, description = "Related intent relation id.") String relatedIntentRelationId,
            @ToolParam(required = false, description = "Related plan element ids.") List<String> relatedPlanElementIds,
            @ToolParam(required = false, description = "Related config block ids.") List<String> relatedConfigBlockIds,
            @ToolParam(required = false, description = "Related execution test id.") String relatedTestId,
            @ToolParam(required = false, description = "Related evidence ids.") List<String> evidenceIds) {
        List<String> validationIds = new ArrayList<>();
        add(validationIds, validationItemId);
        List<String> intentIds = new ArrayList<>();
        add(intentIds, relatedIntentRelationId);
        List<String> testIds = new ArrayList<>();
        add(testIds, relatedTestId);
        return new AffectedScope(
                validationIds,
                intentIds,
                copy(relatedPlanElementIds),
                copy(relatedConfigBlockIds),
                testIds,
                copy(evidenceIds));
    }

    @Tool(name = "suggestRepairAction", description = "Suggest an Orchestrator-controlled repair action for a classified validation failure.")
    public RepairActionSuggestion suggestRepairAction(
            @ToolParam(required = true, description = "Failure type from classifyValidationFailure.") String failureType,
            @ToolParam(required = false, description = "Affected plan element ids.") List<String> relatedPlanElementIds,
            @ToolParam(required = false, description = "Affected config block ids.") List<String> relatedConfigBlockIds,
            @ToolParam(required = false, description = "Risk level from failure classification.") String riskLevel,
            @ToolParam(required = false, description = "Whether the evidence is ambiguous.") Boolean ambiguous) {
        String type = normalize(failureType);
        boolean uncertain = Boolean.TRUE.equals(ambiguous) || "UNKNOWN_FAILURE".equals(type);
        if (uncertain) {
            return new RepairActionSuggestion(
                    "ASK_USER",
                    WorkflowStage.HEALING,
                    "Ask for clarification or operator review before choosing a repair path.",
                    "MEDIUM",
                    true,
                    null);
        }
        if ("POLICY_ENFORCEMENT_FAILURE".equals(type) && !isEmpty(relatedConfigBlockIds)) {
            return new RepairActionSuggestion(
                    "PATCH_CONFIG",
                    WorkflowStage.CONFIGURATION,
                    "Propose a traceable configuration patch for the affected policy block; Orchestrator must approve and apply it through controlled stages.",
                    normalizeRisk(riskLevel),
                    requiresApproval(riskLevel),
                    ArtifactType.CONFIG_SET);
        }
        if ("CONNECTIVITY_FAILURE".equals(type) && isEmpty(relatedConfigBlockIds) && !isEmpty(relatedPlanElementIds)) {
            return new RepairActionSuggestion(
                    "REPLAN",
                    WorkflowStage.PLANNING,
                    "Re-enter planning for the affected network element because the evidence points beyond a narrow config patch.",
                    normalizeRisk(riskLevel),
                    true,
                    ArtifactType.NETWORK_PLAN);
        }
        if ("EXECUTION_EVIDENCE_FAILURE".equals(type)) {
            return new RepairActionSuggestion(
                    "REEXECUTE",
                    WorkflowStage.EXECUTION,
                    "Re-run the controlled execution/verification path after Orchestrator confirms the previous evidence may be stale.",
                    normalizeRisk(riskLevel),
                    true,
                    ArtifactType.EXECUTION_REPORT);
        }
        return new RepairActionSuggestion(
                "REGENERATE_CONFIG",
                WorkflowStage.CONFIGURATION,
                "Regenerate configuration for the affected plan/config scope and let Orchestrator manage artifact versions.",
                normalizeRisk(riskLevel),
                requiresApproval(riskLevel),
                ArtifactType.CONFIG_SET);
    }

    private double confidence(String severity, double base) {
        String normalized = normalize(severity);
        if ("CRITICAL".equals(normalized) || "HIGH".equals(normalized)) {
            return Math.min(0.95, base + 0.08);
        }
        if ("LOW".equals(normalized)) {
            return Math.max(0.35, base - 0.08);
        }
        return base;
    }

    private String highOrMediumRisk(String severity) {
        String normalized = normalize(severity);
        return "CRITICAL".equals(normalized) || "HIGH".equals(normalized) ? "HIGH" : "MEDIUM";
    }

    private boolean requiresApproval(String riskLevel) {
        return "HIGH".equals(normalizeRisk(riskLevel));
    }

    private String normalizeRisk(String riskLevel) {
        String normalized = normalize(riskLevel);
        return normalized.isBlank() ? "MEDIUM" : normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private List<String> copy(List<String> values) {
        return values == null ? List.of() : new ArrayList<>(values);
    }

    private void add(List<String> values, String value) {
        if (value != null && !value.isBlank() && !values.contains(value)) {
            values.add(value);
        }
    }

    private boolean isEmpty(List<String> values) {
        return values == null || values.isEmpty();
    }

    /**
     * Tool result describing the likely validation failure category.
     */
    public record FailureClassification(
            String failureType,
            String rootCauseSummary,
            Double confidence,
            String riskLevel) {
    }

    /**
     * Tool result describing traceable affected validation scope.
     */
    public record AffectedScope(
            List<String> validationItemIds,
            List<String> intentRelationIds,
            List<String> planElementIds,
            List<String> configBlockIds,
            List<String> testIds,
            List<String> evidenceIds) {
    }

    /**
     * Tool result describing a proposed Orchestrator-controlled repair action.
     */
    public record RepairActionSuggestion(
            String actionType,
            WorkflowStage targetStage,
            String description,
            String riskLevel,
            Boolean requiresApproval,
            ArtifactType expectedOutputArtifactType) {
    }
}
