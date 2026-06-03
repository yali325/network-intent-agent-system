package com.yali.mactav.healing.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.healing.request.HealingAgentRequest;
import com.yali.mactav.healing.schema.HealingResponseSchema;
import com.yali.mactav.healing.validator.HealingOutputValidator;
import com.yali.mactav.model.healing.FailureAnalysis;
import com.yali.mactav.model.healing.RepairAction;
import com.yali.mactav.model.healing.RepairPlan;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies Phase 8 repair fixtures through Healing parser, service, and validator boundaries.
 */
class HealingScenarioFixtureTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final HealingOutputValidator validator = new HealingOutputValidator();

    @Test
    void phase8RepairFixturesShouldParseAndValidate() throws Exception {
        for (String scenario : List.of(
                "guest-to-server-unexpected-pass",
                "routing-missing-failure",
                "acl-direction-error")) {
            Path repairPath = scenarioRoot().resolve(scenario).resolve("expected-repair.json");
            RepairPlan fixturePlan = objectMapper.readValue(repairPath.toFile(), RepairPlan.class);

            assertTrue(validator.validate(fixturePlan).isValid(), scenario + " fixture must be valid RepairPlan");

            HealingService service = new HealingServiceImpl();
            RepairPlan parsedPlan = service.parse(toSchema(fixturePlan), request(fixturePlan));

            assertTrue(validator.validate(parsedPlan).isValid(), scenario + " parsed plan must be valid");
            assertFalse(parsedPlan.getFailureAnalysis().isEmpty());
            assertFalse(parsedPlan.getActions().isEmpty());
        }
    }

    private Path scenarioRoot() {
        Path fromModule = Path.of("..", "docs", "test-data", "scenarios").normalize();
        if (Files.exists(fromModule)) {
            return fromModule;
        }
        return Path.of("docs", "test-data", "scenarios");
    }

    private HealingAgentRequest request(RepairPlan plan) {
        return HealingAgentRequest.builder()
                .taskId(plan.getTaskId())
                .validationVersion(plan.getValidationVersion())
                .repairVersion(plan.getRepairVersion())
                .workspaceSnapshot("{}")
                .validationReportJson("{}")
                .traceId("trace-fixture")
                .build();
    }

    private HealingResponseSchema toSchema(RepairPlan plan) {
        return HealingResponseSchema.builder()
                .overallRepairStrategy(plan.getOverallRepairStrategy())
                .requiresUserConfirmation(plan.getRequiresUserConfirmation())
                .failureAnalysis(plan.getFailureAnalysis().stream().map(this::toSchema).toList())
                .actions(plan.getActions().stream().map(this::toSchema).toList())
                .build();
    }

    private HealingResponseSchema.FailureAnalysisSchema toSchema(FailureAnalysis analysis) {
        return HealingResponseSchema.FailureAnalysisSchema.builder()
                .analysisId(analysis.getAnalysisId())
                .failureType(analysis.getFailureType())
                .rootCauseSummary(analysis.getRootCauseSummary())
                .relatedValidationItemIds(analysis.getRelatedValidationItemIds())
                .relatedIntentRelationIds(analysis.getRelatedIntentRelationIds())
                .relatedPlanElementIds(analysis.getRelatedPlanElementIds())
                .relatedConfigBlockIds(analysis.getRelatedConfigBlockIds())
                .confidence(analysis.getConfidence())
                .evidenceIds(analysis.getEvidenceIds())
                .build();
    }

    private HealingResponseSchema.RepairActionSchema toSchema(RepairAction action) {
        return HealingResponseSchema.RepairActionSchema.builder()
                .actionId(action.getActionId())
                .actionType(action.getActionType())
                .targetStage(action.getTargetStage())
                .description(action.getDescription())
                .relatedFailureAnalysisId(action.getRelatedFailureAnalysisId())
                .relatedValidationItemIds(action.getTraceRefs() == null ? List.of()
                        : action.getTraceRefs().getValidationItemIds())
                .relatedIntentRelationIds(action.getTraceRefs() == null ? List.of()
                        : action.getTraceRefs().getIntentRelationIds())
                .relatedPlanElementIds(action.getTraceRefs() == null ? List.of()
                        : action.getTraceRefs().getPlanElementIds())
                .relatedConfigBlockIds(action.getTraceRefs() == null ? List.of()
                        : action.getTraceRefs().getConfigBlockIds())
                .inputArtifactIds(action.getInputArtifactIds())
                .expectedOutputArtifactType(action.getExpectedOutputArtifactType())
                .riskLevel(action.getRiskLevel())
                .riskReason(action.getRiskReason())
                .requiresApproval(action.getRequiresApproval())
                .build();
    }
}
