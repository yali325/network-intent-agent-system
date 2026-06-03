package com.yali.mactav.healing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.healing.request.HealingAgentRequest;
import com.yali.mactav.healing.schema.HealingResponseSchema;
import com.yali.mactav.healing.schema.HealingResponseSchema.FailureAnalysisSchema;
import com.yali.mactav.healing.schema.HealingResponseSchema.RepairActionSchema;
import com.yali.mactav.healing.tool.HealingDiagnosisTool;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.RepairStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.healing.RepairPlan;
import com.yali.mactav.model.verification.ValidationItem;
import com.yali.mactav.model.verification.ValidationReport;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies HealingService parser/tool enrichment and validation flow.
 */
class HealingServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseValidSchemaReturnsRepairPlan() {
        HealingService service = new HealingServiceImpl();
        HealingResponseSchema schema = HealingResponseSchema.builder()
                .overallRepairStrategy("Regenerate policy configuration under Orchestrator control.")
                .requiresUserConfirmation(true)
                .failureAnalysis(List.of(FailureAnalysisSchema.builder()
                        .analysisId("analysis-val-1")
                        .failureType("POLICY_ENFORCEMENT_FAILURE")
                        .rootCauseSummary("ACL policy did not block the denied flow.")
                        .relatedValidationItemIds(List.of("val-1"))
                        .relatedConfigBlockIds(List.of("R1-ACL-001"))
                        .confidence(0.88)
                        .evidenceIds(List.of("ev-1"))
                        .build()))
                .actions(List.of(RepairActionSchema.builder()
                        .actionId("repair-action-1")
                        .actionType("PATCH_CONFIG")
                        .targetStage(WorkflowStage.CONFIGURATION)
                        .description("Propose a traceable policy patch for the affected config block.")
                        .relatedFailureAnalysisId("analysis-val-1")
                        .relatedValidationItemIds(List.of("val-1"))
                        .relatedConfigBlockIds(List.of("R1-ACL-001"))
                        .expectedOutputArtifactType(ArtifactType.CONFIG_SET)
                        .riskLevel("HIGH")
                        .riskReason("Policy changes can affect isolation.")
                        .requiresApproval(true)
                        .build()))
                .build();

        RepairPlan plan = service.parse(schema, requestWithValidationReport());

        assertEquals("task-1", plan.getTaskId());
        assertEquals(1, plan.getValidationVersion());
        assertEquals(1, plan.getRepairVersion());
        assertFalse(plan.getFailureAnalysis().isEmpty());
        assertFalse(plan.getActions().isEmpty());
        assertEquals(RepairStatus.PROPOSED, plan.getActions().get(0).getStatus());
        assertTrue(plan.getActions().get(0).getTraceRefs().getValidationItemIds().contains("val-1"));
    }

    @Test
    void serviceUsesToolToEnrichEmptySchemaFromFailedValidationReport() {
        HealingService service = new HealingServiceImpl();

        RepairPlan plan = service.parse(new HealingResponseSchema(), requestWithValidationReport());

        assertEquals("task-1", plan.getTaskId());
        assertFalse(plan.getFailureAnalysis().isEmpty());
        assertFalse(plan.getActions().isEmpty());
        assertNotNull(plan.getOverallRepairStrategy());
        assertEquals("POLICY_ENFORCEMENT_FAILURE", plan.getFailureAnalysis().get(0).getFailureType());
        assertEquals("PATCH_CONFIG", plan.getActions().get(0).getActionType());
        assertTrue(plan.getActions().get(0).getRequiresApproval());
    }

    private HealingAgentRequest requestWithValidationReport() {
        ValidationReport report = ValidationReport.builder()
                .validationId("validation-task-1-v1")
                .taskId("task-1")
                .validationVersion(1)
                .items(List.of(ValidationItem.builder()
                        .itemId("val-1")
                        .type("isolation")
                        .expected("BLOCKED")
                        .actual("REACHABLE")
                        .passed(false)
                        .severity("HIGH")
                        .relatedIntentRelationId("rel-002")
                        .relatedPlanElementIds(List.of("policy-001"))
                        .relatedConfigBlockIds(List.of("R1-ACL-001"))
                        .relatedTestId("test-guest-server")
                        .evidenceIds(List.of("ev-1"))
                        .message("Guest traffic reached server although it should be blocked.")
                        .build()))
                .build();
        try {
            return HealingAgentRequest.builder()
                    .taskId("task-1")
                    .rawText("Guest zone must not reach server zone.")
                    .validationVersion(1)
                    .repairVersion(1)
                    .validationReportJson(objectMapper.writeValueAsString(report))
                    .failedValidationItemIds(List.of("val-1"))
                    .workspaceSnapshot("{\"taskId\":\"task-1\"}")
                    .traceId("trace-1")
                    .build();
        }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
